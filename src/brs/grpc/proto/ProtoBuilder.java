package brs.grpc.proto;

import brs.*;
import brs.assetexchange.AssetExchange;
import brs.at.AT;
import brs.crypto.EncryptedData;
import brs.services.AccountService;
import brs.services.BlockService;
import brs.util.Convert;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusException;
import io.grpc.protobuf.StatusProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.Collectors;

public final class ProtoBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ProtoBuilder.class);

    private ProtoBuilder() {
    }

    public static StatusException buildError(Throwable t) {
        String message = t.getMessage() == null ? "Unknown Error: " + t.getClass().toString() : t.getMessage();
        if (t.getMessage() == null) {
            logger.debug("Unknown message for gRPC API exception. Exception:", t);
        }
        return StatusProto.toStatusException(Status.newBuilder().setCode(Code.ABORTED_VALUE).setMessage(message).build());
    }
    
    private static ByteString buildByteString(byte[] data) {
        return data == null ? ByteString.EMPTY : ByteString.copyFrom(data);
    }

    public static BrsApi.Account buildAccount(Account account, AccountService accountService) {
        return BrsApi.Account.newBuilder()
                .setId(account.getId())
                .setPublicKey(buildByteString(account.getPublicKey()))
                .setBalance(account.getBalanceNQT())
                .setUnconfirmedBalance(account.getUnconfirmedBalanceNQT())
                .setForgedBalance(account.getForgedBalanceNQT())
                .setName(account.getName())
                .setDescription(account.getDescription())
                .setRewardRecipient(accountService.getRewardRecipientAssignment(account).accountId)
                .addAllAssetBalances(accountService.getAssets(account.id, 0, -1)
                        .stream()
                        .map(ProtoBuilder::buildAssetBalance)
                        .collect(Collectors.toList()))
                .build();
    }

    public static BrsApi.AssetBalance buildAssetBalance(Account.AccountAsset asset) {
        return BrsApi.AssetBalance.newBuilder()
                .setAsset(asset.getAssetId())
                .setAccount(asset.getAccountId())
                .setBalance(asset.getQuantityQNT())
                .setUnconfirmedBalance(asset.getUnconfirmedQuantityQNT())
                .build();
    }

    public static BrsApi.Block buildBlock(Blockchain blockchain, BlockService blockService, Block block, boolean includeTransactions) {
        BrsApi.Block.Builder builder = BrsApi.Block.newBuilder()
                .setId(block.getId())
                .setHeight(block.getHeight())
                .setNumberOfTransactions(block.getTransactions().size())
                .setTotalAmount(block.getTotalAmountNQT())
                .setTotalFee(block.getTotalFeeNQT())
                .setBlockReward(blockService.getBlockReward(block))
                .setPayloadLength(block.getPayloadLength())
                .setVersion(block.getVersion())
                .setBaseTarget(block.getBaseTarget())
                .setTimestamp(block.getTimestamp())
                .addAllTransactionIds(block.getTransactions().stream()
                        .map(Transaction::getId)
                        .collect(Collectors.toList()))
                .setGenerationSignature(buildByteString(block.getGenerationSignature()))
                .setBlockSignature(buildByteString(block.getBlockSignature()))
                .setPayloadHash(buildByteString(block.getPayloadHash()))
                .setGeneratorPublicKey(buildByteString(block.getGeneratorPublicKey()))
                .setNonce(block.getNonce())
                .setScoop(blockService.getScoopNum(block))
                .setPreviousBlockHash(buildByteString(block.getPreviousBlockHash()))
                .setNextBlockId(block.getNextBlockId());

        if (includeTransactions) {
            int currentHeight = blockchain.getHeight();
            builder.addAllTransactions(block.getTransactions().stream()
                    .map(transaction -> buildTransaction(transaction, currentHeight))
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public static BrsApi.BasicTransaction buildBasicTransaction(Transaction transaction) {
        BrsApi.BasicTransaction.Builder builder = BrsApi.BasicTransaction.newBuilder()
                .setSenderPublicKey(buildByteString(transaction.getSenderPublicKey()))
                .setSenderId(transaction.getSenderId())
                .setRecipient(transaction.getRecipientId())
                .setVersion(transaction.getVersion())
                .setType(transaction.getType().getType())
                .setSubtype(transaction.getType().getSubtype())
                .setAmount(transaction.getAmountNQT())
                .setFee(transaction.getFeeNQT())
                .setTimestamp(transaction.getTimestamp())
                .setDeadline(transaction.getDeadline())
                .setReferencedTransactionFullHash(buildByteString(Convert.parseHexString(transaction.getReferencedTransactionFullHash())))
                .addAllAppendages(transaction.getAppendages()
                        .stream()
                        .map(Appendix::getProtobufMessage)
                        .collect(Collectors.toList()))
                .setEcBlockId(transaction.getECBlockId())
                .setEcBlockHeight(transaction.getECBlockHeight())
                .setSignature(buildByteString(transaction.getBytes()));
        if (transaction.getAttachment() != null) {
            builder.setAttachment(transaction.getAttachment().getProtobufMessage());
        }
        return builder.build();
    }

    public static BrsApi.Transaction buildTransaction(Transaction transaction, int currentHeight) {
        return BrsApi.Transaction.newBuilder()
                .setTransaction(buildBasicTransaction(transaction))
                .setId(transaction.getId())
                .setTransactionBytes(buildByteString(transaction.getBytes()))
                .setBlock(transaction.getBlockId())
                .setBlockHeight(transaction.getHeight())
                .setBlockTimestamp(transaction.getBlockTimestamp())
                .setSignature(buildByteString(transaction.getSignature()))
                .setFullHash(buildByteString(Convert.parseHexString(transaction.getFullHash())))
                .setConfirmations(currentHeight - transaction.getHeight())
                .build();
    }

    public static BrsApi.Transaction buildUnconfirmedTransaction(Transaction transaction) {return BrsApi.Transaction.newBuilder()
            .setTransaction(buildBasicTransaction(transaction))
            .setId(transaction.getId())
            .setTransactionBytes(buildByteString(transaction.getBytes()))
            .setBlockHeight(transaction.getHeight())
            .setSignature(buildByteString(transaction.getSignature()))
            .setFullHash(buildByteString(Convert.parseHexString(transaction.getFullHash())))
            .build();
    }

    public static BrsApi.AT buildAT(AccountService accountService, AT at) {
        ByteBuffer bf = ByteBuffer.allocate( 8 );
        bf.order( ByteOrder.LITTLE_ENDIAN );
        bf.put( at.getCreator() );
        bf.clear();
        long creatorId = bf.getLong(); // TODO can this be improved?
        bf.clear();
        bf.put( at.getId() , 0 , 8 );
        long atId = bf.getLong(0);
        return BrsApi.AT.newBuilder()
                .setId(atId)
                .setCreator(creatorId)
                .setVersion(at.getVersion())
                .setName(at.getName())
                .setDescription(at.getDescription())
                .setMachineCode(buildByteString(at.getApCodeBytes()))
                .setMachineData(buildByteString(at.getApDataBytes()))
                .setBalance(accountService.getAccount(atId).getBalanceNQT())
                .setPreviousBalance(at.getpBalance())
                .setNextBlock(at.nextHeight())
                .setFrozen(at.freezeOnSameBalance())
                .setRunning(at.getMachineState().isRunning())
                .setStopped(at.getMachineState().isStopped())
                .setFinished(at.getMachineState().isFinished())
                .setDead(at.getMachineState().isDead())
                .setMinActivation(at.minActivationAmount())
                .setCreationBlock(at.getCreationBlockHeight())
                .build();
    }

    public static BrsApi.Alias buildAlias(Alias alias, Alias.Offer offer) {
        BrsApi.Alias.Builder builder = BrsApi.Alias.newBuilder()
                .setId(alias.getId())
                .setOwner(alias.getAccountId())
                .setName(alias.getAliasName())
                .setUri(alias.getAliasURI())
                .setTimestamp(alias.getTimestamp())
                .setOffered(offer != null);

        if (offer != null) {
            builder.setPrice(offer.getPriceNQT());
            builder.setBuyer(offer.getBuyerId());
        }

        return builder.build();
    }

    public static BrsApi.EncryptedData buildEncryptedData(EncryptedData encryptedData) {
        if (encryptedData == null) return BrsApi.EncryptedData.getDefaultInstance(); // TODO is this needed for all methods?
        return BrsApi.EncryptedData.newBuilder()
                .setData(buildByteString(encryptedData.getData()))
                .setNonce(buildByteString(encryptedData.getNonce()))
                .build();
    }

    public static EncryptedData parseEncryptedData(BrsApi.EncryptedData encryptedData) {
        return new EncryptedData(encryptedData.getData().toByteArray(), encryptedData.getNonce().toByteArray());
    }

    public static BrsApi.IndexRange sanitizeIndexRange(BrsApi.IndexRange indexRange) {
        BrsApi.IndexRange.Builder newIndexRange = indexRange.toBuilder();
        if (newIndexRange.getFirstIndex() == 0 && newIndexRange.getLastIndex() == 0) { // Unset values
            newIndexRange.setLastIndex(Integer.MAX_VALUE); // Signed :(
        }
        if (newIndexRange.getFirstIndex() < 0 || newIndexRange.getLastIndex() < 0) {
            newIndexRange.setFirstIndex(0);
            newIndexRange.setLastIndex(100);
        }
        if (newIndexRange.getFirstIndex() > newIndexRange.getLastIndex()) {
            newIndexRange.setFirstIndex(newIndexRange.getLastIndex());
        }
        return newIndexRange.build();
    }

    public static BrsApi.Asset buildAsset(AssetExchange assetExchange, Asset asset) {
        return BrsApi.Asset.newBuilder()
                .setAsset(asset.getId())
                .setAccount(asset.getAccountId())
                .setName(asset.getName())
                .setDescription(asset.getDescription())
                .setQuantity(asset.getQuantityQNT())
                .setDecimals(asset.getDecimals())
                .setNumberOfTrades(assetExchange.getTradeCount(asset.getId()))
                .setNumberOfTransfers(assetExchange.getTransferCount(asset.getId()))
                .setNumberOfAccounts(assetExchange.getAssetAccountsCount(asset.getId()))
                .build();
    }

    public static BrsApi.Subscription buildSubscription(Subscription subscription) {
        return BrsApi.Subscription.newBuilder()
                .setId(subscription.getId())
                .setSender(subscription.getSenderId())
                .setRecipient(subscription.getRecipientId())
                .setAmount(subscription.getAmountNQT())
                .setFrequency(subscription.getFrequency())
                .setTimeNext(subscription.getTimeNext())
                .build();
    }

    public static BrsApi.Order buildOrder(Order order) {
        return BrsApi.Order.newBuilder()
                .setId(order.getId())
                .setAsset(order.getAssetId())
                .setAccount(order.getAccountId())
                .setQuantity(order.getQuantityQNT())
                .setPrice(order.getPriceNQT())
                .setHeight(order.getHeight())
                .setType(order.getProtobufType())
                .build();
    }

    public static BrsApi.DgsGood buildGoods(DigitalGoodsStore.Goods goods) {
        return BrsApi.DgsGood.newBuilder()
                .setId(goods.getId())
                .setSeller(goods.getSellerId())
                .setPrice(goods.getPriceNQT())
                .setQuantity(goods.getQuantity())
                .setIsDelisted(goods.isDelisted())
                .setTimestamp(goods.getTimestamp())
                .setName(goods.getName())
                .setDescription(goods.getDescription())
                .setTags(goods.getTags())
                .build();
    }

    public static BrsApi.EscrowTransaction buildEscrowTransaction(Escrow escrow) {
        return BrsApi.EscrowTransaction.newBuilder()
                .setEscrowId(escrow.getId())
                .setSender(escrow.getSenderId())
                .setRecipient(escrow.getRecipientId())
                .setAmount(escrow.getAmountNQT())
                .setRequiredSigners(escrow.getRequiredSigners())
                .setDeadline(escrow.getDeadline())
                .setDeadlineAction(Escrow.decisionToProtobuf(escrow.getDeadlineAction()))
                .build();
    }

    public static BrsApi.AssetTrade buildTrade(Trade trade, Asset asset) {
        return BrsApi.AssetTrade.newBuilder()
                .setAsset(trade.getAssetId())
                .setTradeType(trade.isBuy() ? BrsApi.AssetTradeType.BUY : BrsApi.AssetTradeType.SELL)
                .setSeller(trade.getSellerId())
                .setBuyer(trade.getBuyerId())
                .setPrice(trade.getPriceNQT())
                .setQuantity(trade.getQuantityQNT())
                .setAskOrder(trade.getAskOrderId())
                .setBidOrder(trade.getBidOrderId())
                .setAskOrderHeight(trade.getAskOrderHeight())
                .setBidOrderHeight(trade.getBidOrderHeight())
                .setBlock(trade.getBlockId())
                .setHeight(trade.getHeight())
                .setTimestamp(trade.getTimestamp())
                .setAssetName(asset.getName())
                .setAssetDescription(asset.getDescription())
                .build();
    }

    public static BrsApi.AssetTransfer buildTransfer(AssetTransfer assetTransfer, Asset asset) {
        return BrsApi.AssetTransfer.newBuilder()
                .setId(assetTransfer.getId())
                .setAsset(assetTransfer.getAssetId())
                .setSender(assetTransfer.getSenderId())
                .setRecipient(assetTransfer.getRecipientId())
                .setQuantity(assetTransfer.getQuantityQNT())
                .setHeight(assetTransfer.getHeight())
                .setTimestamp(assetTransfer.getTimestamp())
                .setAssetName(asset.getName())
                .setAssetDescription(asset.getDescription())
                .build();
    }

    public static BrsApi.DgsPurchase buildPurchase(DigitalGoodsStore.Purchase purchase, DigitalGoodsStore.Goods goods) {
        return BrsApi.DgsPurchase.newBuilder()
                .setId(purchase.getId())
                .setGood(purchase.getGoodsId())
                .setSeller(purchase.getSellerId())
                .setBuyer(purchase.getBuyerId())
                .setPrice(purchase.getPriceNQT())
                .setQuantity(purchase.getQuantity())
                .setTimestamp(purchase.getTimestamp())
                .setDeliveryDeadlineTimestamp(purchase.getDeliveryDeadlineTimestamp())
                .setGoodName(goods.getName())
                .setGoodDescription(goods.getDescription())
                .setNote(buildEncryptedData(purchase.getNote()))
                .setIsPending(purchase.isPending())
                .setDeliveredData(buildEncryptedData(purchase.getEncryptedGoods()))
                .setDeliveredDataIsText(purchase.goodsIsText())
                .addAllFeedback(purchase.getFeedbackNotes()
                        .stream()
                        .map(ProtoBuilder::buildEncryptedData)
                        .collect(Collectors.toList()))
                .addAllPublicFeedback(purchase.getPublicFeedback())
                .setRefundNote(buildEncryptedData(purchase.getRefundNote()))
                .setDiscount(purchase.getDiscountNQT())
                .setRefund(purchase.getRefundNQT())
                .build();
    }

    public static Transaction parseBasicTransaction(Blockchain blockchain, BrsApi.BasicTransaction basicTransaction) throws ApiException {
        try {
            Transaction.Builder transactionBuilder = new Transaction.Builder(((byte) basicTransaction.getVersion()), basicTransaction.getSenderPublicKey().toByteArray(), basicTransaction.getAmount(), basicTransaction.getFee(), basicTransaction.getTimestamp(), ((short) basicTransaction.getDeadline()), Attachment.AbstractAttachment.parseProtobufMessage(basicTransaction.getAttachment()))
                    .senderId(basicTransaction.getSenderId())
                    .recipientId(basicTransaction.getRecipient());

            if (basicTransaction.getReferencedTransactionFullHash().size() > 0) {
                transactionBuilder.referencedTransactionFullHash(basicTransaction.getReferencedTransactionFullHash().toByteArray());
            }

            int blockchainHeight = blockchain.getHeight();

            for (Any appendix : basicTransaction.getAppendagesList()) {
                try {
                    if (appendix.is(BrsApi.MessageAppendix.class)) {
                        transactionBuilder.message(new Appendix.Message(appendix.unpack(BrsApi.MessageAppendix.class), blockchainHeight));
                    } else if (appendix.is(BrsApi.EncryptedMessageAppendix.class)) {
                        BrsApi.EncryptedMessageAppendix encryptedMessageAppendix = appendix.unpack(BrsApi.EncryptedMessageAppendix.class);
                        switch (encryptedMessageAppendix.getType()) {
                            case TO_RECIPIENT:
                                transactionBuilder.encryptedMessage(new Appendix.EncryptedMessage(encryptedMessageAppendix, blockchainHeight));
                                break;
                            case TO_SELF:
                                transactionBuilder.encryptToSelfMessage(new Appendix.EncryptToSelfMessage(encryptedMessageAppendix, blockchainHeight));
                                break;
                            default:
                                throw new ApiException("Invalid encrypted message type: " + encryptedMessageAppendix.getType().name());
                        }
                    } else if (appendix.is(BrsApi.PublicKeyAnnouncementAppendix.class)) {
                        transactionBuilder.publicKeyAnnouncement(new Appendix.PublicKeyAnnouncement(appendix.unpack(BrsApi.PublicKeyAnnouncementAppendix.class), blockchainHeight));
                    }
                } catch (InvalidProtocolBufferException e) {
                    throw new ApiException("Failed to unpack Any: " + e.getMessage());
                }
            }
            return transactionBuilder.build();
        } catch (BurstException.NotValidException e) {
            throw new ApiException("Transaction not valid: " + e.getMessage());
        } catch (InvalidProtocolBufferException e) {
            throw new ApiException("Could not parse an Any: " + e.getMessage());
        }
    }
}
