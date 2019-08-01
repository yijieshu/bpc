package brs.at;

import brs.Account;
import brs.Burst;
import brs.crypto.Crypto;
import brs.fluxcapacitor.FluxValues;
import brs.props.Props;
import brs.util.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.*;

public abstract class AtController {
    private AtController() {
    }

    private static final Logger logger = LoggerFactory.getLogger(AtController.class);

    private static final Logger debugLogger = Burst.getPropertyService().getBoolean(Props.ENABLE_AT_DEBUG_LOG) ? logger : NOPLogger.NOP_LOGGER;

    private static int runSteps(AtMachineState state) {
        state.getMachineState().running = true;
        state.getMachineState().stopped = false;
        state.getMachineState().finished = false;
        state.getMachineState().dead = false;
        state.getMachineState().steps = 0;

        AtMachineProcessor processor = new AtMachineProcessor(state, Burst.getPropertyService().getBoolean(Props.ENABLE_AT_DEBUG_LOG));

        state.setFreeze(false);

        long stepFee = AtConstants.getInstance().stepFee(state.getCreationBlockHeight());

        int numSteps = 0;

        while (state.getMachineState().steps +
                (numSteps = getNumSteps(state.getApCode().get(state.getMachineState().pc), state.getCreationBlockHeight()))
                <= AtConstants.getInstance().maxSteps(state.getHeight())) {

            if ((state.getgBalance() < stepFee * numSteps)) {
                debugLogger.debug("stopped - not enough balance");
                state.setFreeze(true);
                return 3;
            }

            state.setgBalance(state.getgBalance() - (stepFee * numSteps));
            state.getMachineState().steps += numSteps;
            int rc = processor.processOp(false, false);

            if (rc >= 0) {
                if (state.getMachineState().stopped) {
                    debugLogger.debug("stopped");
                    state.getMachineState().running = false;
                    return 2;
                } else if (state.getMachineState().finished) {
                    debugLogger.debug("finished");
                    state.getMachineState().running = false;
                    return 1;
                }
            } else {
                if (rc == -1)
                    debugLogger.debug("error: overflow");
                else if (rc == -2)
                    debugLogger.debug("error: invalid code");
                else
                    debugLogger.debug("unexpected error");

                if (state.getMachineState().jumps.contains(state.getMachineState().err)) {
                    state.getMachineState().pc = state.getMachineState().err;
                } else {
                    state.getMachineState().dead = true;
                    state.getMachineState().running = false;
                    return 0;
                }
            }
        }

        return 5;
    }

    private static int getNumSteps(byte op, int height) {
        if (op >= 0x32 && op < 0x38)
            return (int) AtConstants.getInstance().apiStepMultiplier(height);

        return 1;
    }

    public static void resetMachine(AtMachineState state) {
        state.getMachineState().reset();
        listCode(state, true, true);
    }

    private static void listCode(AtMachineState state, boolean disassembly, boolean determineJumps) {

        AtMachineProcessor machineProcessor = new AtMachineProcessor(state, Burst.getPropertyService().getBoolean(Props.ENABLE_AT_DEBUG_LOG));

        int opc = state.getMachineState().pc;
        int osteps = state.getMachineState().steps;

        state.getApCode().order(ByteOrder.LITTLE_ENDIAN);
        state.getApData().order(ByteOrder.LITTLE_ENDIAN);

        state.getMachineState().pc = 0;
        state.getMachineState().opc = opc;

        while (true) {

            int rc = machineProcessor.processOp(disassembly, determineJumps);
            if (rc <= 0) break;

            state.getMachineState().pc += rc;
        }

        state.getMachineState().steps = osteps;
        state.getMachineState().pc = opc;
    }

    public static int checkCreationBytes(byte[] creation, int height) throws AtException {
        if (creation == null)
            throw new AtException("Creation bytes cannot be null");

        int totalPages;
        try {
            ByteBuffer b = ByteBuffer.allocate(creation.length);
            b.order(ByteOrder.LITTLE_ENDIAN);

            b.put(creation);
            b.clear();

            AtConstants instance = AtConstants.getInstance();

            short version = b.getShort();
            if (version != instance.atVersion(height)) {
                throw new AtException(AtError.INCORRECT_VERSION.getDescription());
            }

            // Ignore reserved bytes
            b.getShort(); //future: reserved for future needs

            short codePages = b.getShort();
            if (codePages > instance.maxMachineCodePages(height) || codePages < 1) {
                throw new AtException(AtError.INCORRECT_CODE_PAGES.getDescription());
            }

            short dataPages = b.getShort();
            if (dataPages > instance.maxMachineDataPages(height) || dataPages < 0) {
                throw new AtException(AtError.INCORRECT_DATA_PAGES.getDescription());
            }

            short callStackPages = b.getShort();
            if (callStackPages > instance.maxMachineCallStackPages(height) || callStackPages < 0) {
                throw new AtException(AtError.INCORRECT_CALL_PAGES.getDescription());
            }

            short userStackPages = b.getShort();
            if (userStackPages > instance.maxMachineUserStackPages(height) || userStackPages < 0) {
                throw new AtException(AtError.INCORRECT_USER_PAGES.getDescription());
            }

            // Ignore the minimum activation amount
            b.getLong();

            int codeLen = getLength(codePages, b);
            if (codeLen < 1 || codeLen > codePages * 256) {
                throw new AtException(AtError.INCORRECT_CODE_LENGTH.getDescription());
            }
            byte[] code = new byte[codeLen];
            b.get(code, 0, codeLen);

            int dataLen = getLength(dataPages, b);
            if (dataLen < 0 || dataLen > dataPages * 256) {
                throw new AtException(AtError.INCORRECT_DATA_LENGTH.getDescription());
            }
            byte[] data = new byte[dataLen];
            b.get(data, 0, dataLen);

            totalPages = codePages + dataPages + userStackPages + callStackPages;

            if (b.position() != b.capacity()) {
                throw new AtException(AtError.INCORRECT_CREATION_TX.getDescription());
            }

            //TODO note: run code in demo mode for checking if is valid

        } catch (BufferUnderflowException e) {
            throw new AtException(AtError.INCORRECT_CREATION_TX.getDescription());
        }

        return totalPages;
    }

    private static int getLength(int nPages, ByteBuffer buffer) throws AtException {
        int codeLen;
        if (nPages * 256 < 257) {
            codeLen = buffer.get();
            if (codeLen < 0)
                codeLen += (Byte.MAX_VALUE + 1) * 2;
        } else if (nPages * 256 < Short.MAX_VALUE + 1) {
            codeLen = buffer.getShort();
            if (codeLen < 0)
                codeLen += (Short.MAX_VALUE + 1) * 2;
        } else if (nPages * 256 <= Integer.MAX_VALUE) {
            codeLen = buffer.getInt();
        } else {
            throw new AtException(AtError.INCORRECT_CODE_LENGTH.getDescription());
        }
        return codeLen;
    }

    public static AtBlock getCurrentBlockATs(int freePayload, int blockHeight) {
        List<Long> orderedATs = AT.getOrderedATs();
        Iterator<Long> keys = orderedATs.iterator();

        List<AT> processedATs = new ArrayList<>();

        int costOfOneAT = getCostOfOneAT();
        int payload = 0;
        long totalFee = 0;
        long totalAmount = 0;

        while (payload <= freePayload - costOfOneAT && keys.hasNext()) {
            Long id = keys.next();
            AT at = AT.getAT(id);

            long atAccountBalance = getATAccountBalance(id);
            long atStateBalance = at.getgBalance();

            if (at.freezeOnSameBalance() && (atAccountBalance - atStateBalance < at.minActivationAmount())) {
                continue;
            }

            if (atAccountBalance >= AtConstants.getInstance().stepFee(at.getCreationBlockHeight())
                    * AtConstants.getInstance().apiStepMultiplier(at.getCreationBlockHeight())) {
                try {
                    at.setgBalance(atAccountBalance);
                    at.setHeight(blockHeight);
                    at.clearTransactions();
                    at.setWaitForNumberOfBlocks(at.getSleepBetween());
                    listCode(at, true, true);
                    runSteps(at);

                    long fee = at.getMachineState().steps * AtConstants.getInstance().stepFee(at.getCreationBlockHeight());
                    if (at.getMachineState().dead) {
                        fee += at.getgBalance();
                        at.setgBalance(0L);
                    }
                    at.setpBalance(at.getgBalance());

                    long amount = makeTransactions(at);
                    if (!Burst.getFluxCapacitor().getValue(FluxValues.AT_FIX_BLOCK_4, blockHeight)) {
                        totalAmount = amount;
                    } else {
                        totalAmount += amount;
                    }

                    totalFee += fee;
                    AT.addPendingFee(id, fee);

                    payload += costOfOneAT;

                    processedATs.add(at);
                } catch (Exception e) {
                    debugLogger.debug("Error handling AT", e);
                }
            }
        }

        byte[] bytesForBlock;

        bytesForBlock = getBlockATBytes(processedATs, payload);

        return new AtBlock(totalFee, totalAmount, bytesForBlock);
    }

    public static AtBlock validateATs(byte[] blockATs, int blockHeight) throws AtException {
        if (blockATs == null) {
            return new AtBlock(0, 0, null);
        }

        LinkedHashMap<ByteBuffer, byte[]> ats = getATsFromBlock(blockATs);

        List<AT> processedATs = new ArrayList<>();

        long totalFee = 0;
        MessageDigest digest = Crypto.md5();
        byte[] md5;
        long totalAmount = 0;

        for (Map.Entry<ByteBuffer, byte[]> entry : ats.entrySet()) {
            ByteBuffer atIdBuffer = entry.getKey();
            byte[] receivedMd5 = entry.getValue();
            byte[] atId = atIdBuffer.array();
            AT at = AT.getAT(atId);
            try {
                at.clearTransactions();
                at.setHeight(blockHeight);
                at.setWaitForNumberOfBlocks(at.getSleepBetween());

                long atAccountBalance = getATAccountBalance(AtApiHelper.getLong(atId));
                if (atAccountBalance < AtConstants.getInstance().stepFee(at.getCreationBlockHeight())
                        * AtConstants.getInstance().apiStepMultiplier(at.getCreationBlockHeight())) {
                    throw new AtException("AT has insufficient balance to run");
                }

                if (at.freezeOnSameBalance() && (atAccountBalance - at.getgBalance() < at.minActivationAmount())) {
                    throw new AtException("AT should be frozen due to unchanged balance");
                }

                if (at.nextHeight() > blockHeight) {
                    throw new AtException("AT not allowed to run again yet");
                }

                at.setgBalance(atAccountBalance);

                listCode(at, true, true);

                runSteps(at);

                long fee = at.getMachineState().steps * AtConstants.getInstance().stepFee(at.getCreationBlockHeight());
                if (at.getMachineState().dead) {
                    fee += at.getgBalance();
                    at.setgBalance(0L);
                }
                at.setpBalance(at.getgBalance());

                if (!Burst.getFluxCapacitor().getValue(FluxValues.AT_FIX_BLOCK_4, blockHeight)) {
                    totalAmount = makeTransactions(at);
                } else {
                    totalAmount += makeTransactions(at);
                }

                totalFee += fee;
                AT.addPendingFee(atId, fee);

                processedATs.add(at);

                md5 = digest.digest(at.getBytes());
                if (!Arrays.equals(md5, receivedMd5)) {
                    throw new AtException("Calculated md5 and received md5 are not matching");
                }
            } catch (Exception e) {
                debugLogger.debug("ATs error", e);
                throw new AtException("ATs error. Block rejected", e);
            }
        }

        for (AT at : processedATs) {
            at.saveState();
        }

        return new AtBlock(totalFee, totalAmount, new byte[1]);
    }

    private static LinkedHashMap<ByteBuffer, byte[]> getATsFromBlock(byte[] blockATs) throws AtException {
        if (blockATs.length > 0 && blockATs.length % (getCostOfOneAT()) != 0) {
            throw new AtException("blockATs must be a multiple of cost of one AT ( " + getCostOfOneAT() + " )");
        }

        ByteBuffer b = ByteBuffer.wrap(blockATs);
        b.order(ByteOrder.LITTLE_ENDIAN);

        byte[] temp = new byte[AtConstants.AT_ID_SIZE];

        LinkedHashMap<ByteBuffer, byte[]> ats = new LinkedHashMap<>();

        while (b.position() < b.capacity()) {
            b.get(temp, 0, temp.length);
            byte[] md5 = new byte[16];
            b.get(md5, 0, md5.length);
            ByteBuffer atId = ByteBuffer.allocate(AtConstants.AT_ID_SIZE);
            atId.put(temp);
            atId.clear();
            if (ats.containsKey(atId)) {
                throw new AtException("AT included in block multiple times");
            }
            ats.put(atId, md5);
        }

        if (b.position() != b.capacity()) {
            throw new AtException("bytebuffer not matching");
        }

        return ats;
    }

    private static byte[] getBlockATBytes(List<AT> processedATs, int payload) {
        if (payload <= 0) {
            return null;
        }

        ByteBuffer b = ByteBuffer.allocate(payload);
        b.order(ByteOrder.LITTLE_ENDIAN);

        MessageDigest digest = Crypto.md5();
        for (AT at : processedATs) {
            b.put(at.getId());
            digest.update(at.getBytes());
            b.put(digest.digest());
        }

        return b.array();
    }

    private static int getCostOfOneAT() {
        return AtConstants.AT_ID_SIZE + 16;
    }

    //platform based implementations
    //platform based
    private static long makeTransactions(AT at) throws AtException {
        long totalAmount = 0;
        if (!Burst.getFluxCapacitor().getValue(FluxValues.AT_FIX_BLOCK_4, at.getHeight())) {
            for (AtTransaction tx : at.getTransactions()) {
                if (AT.findPendingTransaction(tx.getRecipientId())) {
                    throw new AtException("Conflicting transaction found");
                }
            }
        }
        for (AtTransaction tx : at.getTransactions()) {
            totalAmount += tx.getAmount();
            AT.addPendingTransaction(tx);
            if (logger.isDebugEnabled()) {
                logger.debug("Transaction to {}, amount {}", Convert.toUnsignedLong(AtApiHelper.getLong(tx.getRecipientId())), tx.getAmount());
            }
        }
        return totalAmount;
    }

    //platform based
    private static long getATAccountBalance(Long id) {
        Account atAccount = Account.getAccount(id);

        if (atAccount != null) {
            return atAccount.getBalanceNQT();
        }

        return 0;
    }
}
