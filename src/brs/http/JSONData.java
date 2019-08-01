package brs.http;

import brs.*;
import brs.Alias.Offer;
import brs.at.AT;
import brs.at.AtApiHelper;
import brs.crypto.Crypto;
import brs.crypto.EncryptedData;
import brs.peer.Peer;
import brs.services.AccountService;
import brs.util.Convert;
import brs.util.JSON;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static brs.http.common.ResultFields.*;

public final class JSONData {

  static JsonObject alias(Alias alias, Offer offer) {
    JsonObject json = new JsonObject();
    putAccount(json, ACCOUNT_RESPONSE, alias.getAccountId());
    json.addProperty(ALIAS_NAME_RESPONSE, alias.getAliasName());
    json.addProperty(ALIAS_URI_RESPONSE, alias.getAliasURI());
    json.addProperty(TIMESTAMP_RESPONSE, alias.getTimestamp());
    json.addProperty(ALIAS_RESPONSE, Convert.toUnsignedLong(alias.getId()));

    if (offer != null) {
      json.addProperty(PRICE_NQT_RESPONSE, String.valueOf(offer.getPriceNQT()));
      if (offer.getBuyerId() != 0) {
        json.addProperty(BUYER_RESPONSE, Convert.toUnsignedLong(offer.getBuyerId()));
      }
    }
    return json;
  }

  static JsonObject accountBalance(Account account) {
    JsonObject json = new JsonObject();
    if (account == null) {
      json.addProperty(BALANCE_NQT_RESPONSE,             "0");
      json.addProperty(UNCONFIRMED_BALANCE_NQT_RESPONSE, "0");
      json.addProperty(EFFECTIVE_BALANCE_NQT_RESPONSE,   "0");
      json.addProperty(FORGED_BALANCE_NQT_RESPONSE,      "0");
      json.addProperty(GUARANTEED_BALANCE_NQT_RESPONSE,  "0");
    }
    else {
      json.addProperty(BALANCE_NQT_RESPONSE, String.valueOf(account.getBalanceNQT()));
      json.addProperty(UNCONFIRMED_BALANCE_NQT_RESPONSE, String.valueOf(account.getUnconfirmedBalanceNQT()));
      json.addProperty(EFFECTIVE_BALANCE_NQT_RESPONSE, String.valueOf(account.getBalanceNQT()));
      json.addProperty(FORGED_BALANCE_NQT_RESPONSE, String.valueOf(account.getForgedBalanceNQT()));
      json.addProperty(GUARANTEED_BALANCE_NQT_RESPONSE, String.valueOf(account.getBalanceNQT()));
    }
    return json;
  }

  static JsonObject asset(Asset asset, int tradeCount, int transferCount, int assetAccountsCount) {
    JsonObject json = new JsonObject();
    putAccount(json, ACCOUNT_RESPONSE, asset.getAccountId());
    json.addProperty(NAME_RESPONSE, asset.getName());
    json.addProperty(DESCRIPTION_RESPONSE, asset.getDescription());
    json.addProperty(DECIMALS_RESPONSE, asset.getDecimals());
    json.addProperty(QUANTITY_QNT_RESPONSE, String.valueOf(asset.getQuantityQNT()));
    json.addProperty(ASSET_RESPONSE, Convert.toUnsignedLong(asset.getId()));
    json.addProperty(NUMBER_OF_TRADES_RESPONSE, tradeCount);
    json.addProperty(NUMBER_OF_TRANSFERS_RESPONSE, transferCount);
    json.addProperty(NUMBER_OF_ACCOUNTS_RESPONSE, assetAccountsCount);
    return json;
  }

  static JsonObject accountAsset(Account.AccountAsset accountAsset) {
    JsonObject json = new JsonObject();
    putAccount(json, ACCOUNT_RESPONSE, accountAsset.getAccountId());
    json.addProperty(ASSET_RESPONSE, Convert.toUnsignedLong(accountAsset.getAssetId()));
    json.addProperty(QUANTITY_QNT_RESPONSE, String.valueOf(accountAsset.getQuantityQNT()));
    json.addProperty(UNCONFIRMED_QUANTITY_QNT_RESPONSE, String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
    return json;
  }

  static JsonObject askOrder(Order.Ask order) {
    JsonObject json = order(order);
    json.addProperty(TYPE_RESPONSE, "ask");
    return json;
  }

  static JsonObject bidOrder(Order.Bid order) {
    JsonObject json = order(order);
    json.addProperty(TYPE_RESPONSE, "bid");
    return json;
  }

  private static JsonObject order(Order order) {
    JsonObject json = new JsonObject();
    json.addProperty(ORDER_RESPONSE, Convert.toUnsignedLong(order.getId()));
    json.addProperty(ASSET_RESPONSE, Convert.toUnsignedLong(order.getAssetId()));
    putAccount(json, ACCOUNT_RESPONSE, order.getAccountId());
    json.addProperty(QUANTITY_QNT_RESPONSE, String.valueOf(order.getQuantityQNT()));
    json.addProperty(PRICE_NQT_RESPONSE, String.valueOf(order.getPriceNQT()));
    json.addProperty(HEIGHT_RESPONSE, order.getHeight());
    return json;
  }

  static JsonObject block(Block block, boolean includeTransactions, int currentBlockchainHeight, long blockReward, int scoopNum) {
    JsonObject json = new JsonObject();
    json.addProperty(BLOCK_RESPONSE, block.getStringId());
    json.addProperty(HEIGHT_RESPONSE, block.getHeight());
    putAccount(json, GENERATOR_RESPONSE, block.getGeneratorId());
    json.addProperty(GENERATOR_PUBLIC_KEY_RESPONSE, Convert.toHexString(block.getGeneratorPublicKey()));
    json.addProperty(NONCE_RESPONSE, Convert.toUnsignedLong(block.getNonce()));
    json.addProperty(SCOOP_NUM_RESPONSE, scoopNum);
    json.addProperty(TIMESTAMP_RESPONSE, block.getTimestamp());
    json.addProperty(NUMBER_OF_TRANSACTIONS_RESPONSE, block.getTransactions().size());
    json.addProperty(TOTAL_AMOUNT_NQT_RESPONSE, String.valueOf(block.getTotalAmountNQT()));
    json.addProperty(TOTAL_FEE_NQT_RESPONSE, String.valueOf(block.getTotalFeeNQT()));
    json.addProperty(BLOCK_REWARD_RESPONSE, Convert.toUnsignedLong(blockReward / Constants.ONE_BURST));
    json.addProperty(PAYLOAD_LENGTH_RESPONSE, block.getPayloadLength());
    json.addProperty(VERSION_RESPONSE, block.getVersion());
    json.addProperty(BASE_TARGET_RESPONSE, Convert.toUnsignedLong(block.getBaseTarget()));

    if (block.getPreviousBlockId() != 0) {
      json.addProperty(PREVIOUS_BLOCK_RESPONSE, Convert.toUnsignedLong(block.getPreviousBlockId()));
    }

    if (block.getNextBlockId() != 0) {
      json.addProperty(NEXT_BLOCK_RESPONSE, Convert.toUnsignedLong(block.getNextBlockId()));
    }

    json.addProperty(PAYLOAD_HASH_RESPONSE, Convert.toHexString(block.getPayloadHash()));
    json.addProperty(GENERATION_SIGNATURE_RESPONSE, Convert.toHexString(block.getGenerationSignature()));

    if (block.getVersion() > 1) {
      json.addProperty(PREVIOUS_BLOCK_HASH_RESPONSE, Convert.toHexString(block.getPreviousBlockHash()));
    }

    json.addProperty(BLOCK_SIGNATURE_RESPONSE, Convert.toHexString(block.getBlockSignature()));

    JsonArray transactions = new JsonArray();
    for (Transaction transaction : block.getTransactions()) {
      if (includeTransactions) {
        transactions.add(transaction(transaction, currentBlockchainHeight));
      } else {
        transactions.add(Convert.toUnsignedLong(transaction.getId()));
      }
    }
    json.add(TRANSACTIONS_RESPONSE, transactions);
    return json;
  }

  static JsonObject encryptedData(EncryptedData encryptedData) {
    JsonObject json = new JsonObject();
    json.addProperty(DATA_RESPONSE, Convert.toHexString(encryptedData.getData()));
    json.addProperty(NONCE_RESPONSE, Convert.toHexString(encryptedData.getNonce()));
    return json;
  }

  static JsonObject escrowTransaction(Escrow escrow) {
    JsonObject json = new JsonObject();
    json.addProperty(ID_RESPONSE, Convert.toUnsignedLong(escrow.getId()));
    json.addProperty(SENDER_RESPONSE, Convert.toUnsignedLong(escrow.getSenderId()));
    json.addProperty(SENDER_RS_RESPONSE, Convert.rsAccount(escrow.getSenderId()));
    json.addProperty(RECIPIENT_RESPONSE, Convert.toUnsignedLong(escrow.getRecipientId()));
    json.addProperty(RECIPIENT_RS_RESPONSE, Convert.rsAccount(escrow.getRecipientId()));
    json.addProperty(AMOUNT_NQT_RESPONSE, Convert.toUnsignedLong(escrow.getAmountNQT()));
    json.addProperty(REQUIRED_SIGNERS_RESPONSE, escrow.getRequiredSigners());
    json.addProperty(DEADLINE_RESPONSE, escrow.getDeadline());
    json.addProperty(DEADLINE_ACTION_RESPONSE, Escrow.decisionToString(escrow.getDeadlineAction()));

    JsonArray signers = new JsonArray();
    for (Escrow.Decision decision : escrow.getDecisions()) {
      if(decision.getAccountId().equals(escrow.getSenderId()) ||
              decision.getAccountId().equals(escrow.getRecipientId())) {
        continue;
      }
      JsonObject signerDetails = new JsonObject();
      signerDetails.addProperty(ID_RESPONSE, Convert.toUnsignedLong(decision.getAccountId()));
      signerDetails.addProperty(ID_RS_RESPONSE, Convert.rsAccount(decision.getAccountId()));
      signerDetails.addProperty(DECISION_RESPONSE, Escrow.decisionToString(decision.getDecision()));
      signers.add(signerDetails);
    }
    json.add(SIGNERS_RESPONSE, signers);
    return json;
  }

  static JsonObject goods(DigitalGoodsStore.Goods goods) {
    JsonObject json = new JsonObject();
    json.addProperty(GOODS_RESPONSE, Convert.toUnsignedLong(goods.getId()));
    json.addProperty(NAME_RESPONSE, goods.getName());
    json.addProperty(DESCRIPTION_RESPONSE, goods.getDescription());
    json.addProperty(QUANTITY_RESPONSE, goods.getQuantity());
    json.addProperty(PRICE_NQT_RESPONSE, String.valueOf(goods.getPriceNQT()));
    putAccount(json, SELLER_RESPONSE, goods.getSellerId());
    json.addProperty(TAGS_RESPONSE, goods.getTags());
    json.addProperty(DELISTED_RESPONSE, goods.isDelisted());
    json.addProperty(TIMESTAMP_RESPONSE, goods.getTimestamp());
    return json;
  }

  static JsonObject token(Token token) {
    JsonObject json = new JsonObject();
    putAccount(json, "account", Account.getId(token.getPublicKey()));
    json.addProperty("timestamp", token.getTimestamp());
    json.addProperty("valid", token.isValid());
    return json;
  }

  static JsonObject peer(Peer peer) {
    JsonObject json = new JsonObject();
    json.addProperty("state", peer.getState().ordinal());
    json.addProperty("announcedAddress", peer.getAnnouncedAddress());
    json.addProperty("shareAddress", peer.shareAddress());
    json.addProperty("downloadedVolume", peer.getDownloadedVolume());
    json.addProperty("uploadedVolume", peer.getUploadedVolume());
    json.addProperty("application", peer.getApplication());
    json.addProperty("version", peer.getVersion().toStringIfNotEmpty());
    json.addProperty("platform", peer.getPlatform());
    json.addProperty("blacklisted", peer.isBlacklisted());
    json.addProperty("lastUpdated", peer.getLastUpdated());
    return json;
  }

  static JsonObject purchase(DigitalGoodsStore.Purchase purchase) {
    JsonObject json = new JsonObject();
    json.addProperty(PURCHASE_RESPONSE, Convert.toUnsignedLong(purchase.getId()));
    json.addProperty(GOODS_RESPONSE, Convert.toUnsignedLong(purchase.getGoodsId()));
    json.addProperty(NAME_RESPONSE, purchase.getName());
    putAccount(json, SELLER_RESPONSE, purchase.getSellerId());
    json.addProperty(PRICE_NQT_RESPONSE, String.valueOf(purchase.getPriceNQT()));
    json.addProperty(QUANTITY_RESPONSE, purchase.getQuantity());
    putAccount(json, BUYER_RESPONSE, purchase.getBuyerId());
    json.addProperty(TIMESTAMP_RESPONSE, purchase.getTimestamp());
    json.addProperty(DELIVERY_DEADLINE_TIMESTAMP_RESPONSE, purchase.getDeliveryDeadlineTimestamp());
    if (purchase.getNote() != null) {
      json.add(NOTE_RESPONSE, encryptedData(purchase.getNote()));
    }
    json.addProperty(PENDING_RESPONSE, purchase.isPending());
    if (purchase.getEncryptedGoods() != null) {
      json.add(GOODS_DATA_RESPONSE, encryptedData(purchase.getEncryptedGoods()));
      json.addProperty(GOODS_IS_TEXT_RESPONSE, purchase.goodsIsText());
    }
    if (purchase.getFeedbackNotes() != null) {
      JsonArray feedbacks = new JsonArray();
      for (EncryptedData encryptedData : purchase.getFeedbackNotes()) {
        feedbacks.add(encryptedData(encryptedData));
      }
      json.add(FEEDBACK_NOTES_RESPONSE, feedbacks);
    }
    if (!purchase.getPublicFeedback().isEmpty()) {
      JsonArray publicFeedbacks = new JsonArray();
      for (String string : purchase.getPublicFeedback()) {
        publicFeedbacks.add(string);
      }
      json.add(PUBLIC_FEEDBACKS_RESPONSE, publicFeedbacks);
    }
    if (purchase.getRefundNote() != null) {
      json.add(REFUND_NOTE_RESPONSE, encryptedData(purchase.getRefundNote()));
    }
    if (purchase.getDiscountNQT() > 0) {
      json.addProperty(DISCOUNT_NQT_RESPONSE, String.valueOf(purchase.getDiscountNQT()));
    }
    if (purchase.getRefundNQT() > 0) {
      json.addProperty(REFUND_NQT_RESPONSE, String.valueOf(purchase.getRefundNQT()));
    }
    return json;
  }

  static JsonObject subscription(Subscription subscription) {
    JsonObject json = new JsonObject();
    json.addProperty(ID_RESPONSE, Convert.toUnsignedLong(subscription.getId()));
    putAccount(json, SENDER_RESPONSE, subscription.getSenderId());
    putAccount(json, RECIPIENT_RESPONSE, subscription.getRecipientId());
    json.addProperty(AMOUNT_NQT_RESPONSE, Convert.toUnsignedLong(subscription.getAmountNQT()));
    json.addProperty(FREQUENCY_RESPONSE, subscription.getFrequency());
    json.addProperty(TIME_NEXT_RESPONSE, subscription.getTimeNext());
    return json;
  }

  static JsonObject trade(Trade trade, Asset asset) {
    JsonObject json = new JsonObject();
    json.addProperty(TIMESTAMP_RESPONSE, trade.getTimestamp());
    json.addProperty(QUANTITY_QNT_RESPONSE, String.valueOf(trade.getQuantityQNT()));
    json.addProperty(PRICE_NQT_RESPONSE, String.valueOf(trade.getPriceNQT()));
    json.addProperty(ASSET_RESPONSE, Convert.toUnsignedLong(trade.getAssetId()));
    json.addProperty(ASK_ORDER_RESPONSE, Convert.toUnsignedLong(trade.getAskOrderId()));
    json.addProperty(BID_ORDER_RESPONSE, Convert.toUnsignedLong(trade.getBidOrderId()));
    json.addProperty(ASK_ORDER_HEIGHT_RESPONSE, trade.getAskOrderHeight());
    json.addProperty(BID_ORDER_HEIGHT_RESPONSE, trade.getBidOrderHeight());
    putAccount(json, SELLER_RESPONSE, trade.getSellerId());
    putAccount(json, BUYER_RESPONSE, trade.getBuyerId());
    json.addProperty(BLOCK_RESPONSE, Convert.toUnsignedLong(trade.getBlockId()));
    json.addProperty(HEIGHT_RESPONSE, trade.getHeight());
    json.addProperty(TRADE_TYPE_RESPONSE, trade.isBuy() ? "buy" : "sell");
    if (asset != null) {
      json.addProperty(NAME_RESPONSE, asset.getName());
      json.addProperty(DECIMALS_RESPONSE, asset.getDecimals());
    }
    return json;
  }

  static JsonObject assetTransfer(AssetTransfer assetTransfer, Asset asset) {
    JsonObject json = new JsonObject();
    json.addProperty(ASSET_TRANSFER_RESPONSE, Convert.toUnsignedLong(assetTransfer.getId()));
    json.addProperty(ASSET_RESPONSE, Convert.toUnsignedLong(assetTransfer.getAssetId()));
    putAccount(json, SENDER_RESPONSE, assetTransfer.getSenderId());
    putAccount(json, RECIPIENT_RESPONSE, assetTransfer.getRecipientId());
    json.addProperty(QUANTITY_QNT_RESPONSE, String.valueOf(assetTransfer.getQuantityQNT()));
    json.addProperty(HEIGHT_RESPONSE, assetTransfer.getHeight());
    json.addProperty(TIMESTAMP_RESPONSE, assetTransfer.getTimestamp());
    if (asset != null) {
      json.addProperty(NAME_RESPONSE, asset.getName());
      json.addProperty(DECIMALS_RESPONSE, asset.getDecimals());
    }

    return json;
  }

  static JsonObject unconfirmedTransaction(Transaction transaction) {
    JsonObject json = new JsonObject();
    json.addProperty(TYPE_RESPONSE, transaction.getType().getType());
    json.addProperty(SUBTYPE_RESPONSE, transaction.getType().getSubtype());
    json.addProperty(TIMESTAMP_RESPONSE, transaction.getTimestamp());
    json.addProperty(DEADLINE_RESPONSE, transaction.getDeadline());
    json.addProperty(SENDER_PUBLIC_KEY_RESPONSE, Convert.toHexString(transaction.getSenderPublicKey()));
    if (transaction.getRecipientId() != 0) {
      putAccount(json, RECIPIENT_RESPONSE, transaction.getRecipientId());
    }
    json.addProperty(AMOUNT_NQT_RESPONSE, String.valueOf(transaction.getAmountNQT()));
    json.addProperty(FEE_NQT_RESPONSE, String.valueOf(transaction.getFeeNQT()));
    if (transaction.getReferencedTransactionFullHash() != null) {
      json.addProperty(REFERENCED_TRANSACTION_FULL_HASH_RESPONSE, transaction.getReferencedTransactionFullHash());
    }
    byte[] signature = Convert.emptyToNull(transaction.getSignature());
    if (signature != null) {
      json.addProperty(SIGNATURE_RESPONSE, Convert.toHexString(signature));
      json.addProperty(SIGNATURE_HASH_RESPONSE, Convert.toHexString(Crypto.sha256().digest(signature)));
      json.addProperty(FULL_HASH_RESPONSE, transaction.getFullHash());
      json.addProperty(TRANSACTION_RESPONSE, transaction.getStringId());
    }
    else if (!transaction.getType().isSigned()) {
      json.addProperty(FULL_HASH_RESPONSE, transaction.getFullHash());
      json.addProperty(TRANSACTION_RESPONSE, transaction.getStringId());
    }
    JsonObject attachmentJSON = new JsonObject();
    for (Appendix appendage : transaction.getAppendages()) {
      JSON.addAll(attachmentJSON, appendage.getJsonObject());
    }
    if (attachmentJSON.size() > 0) {
      modifyAttachmentJSON(attachmentJSON);
      json.add(ATTACHMENT_RESPONSE, attachmentJSON);
    }
    putAccount(json, SENDER_RESPONSE, transaction.getSenderId());
    json.addProperty(HEIGHT_RESPONSE, transaction.getHeight());
    json.addProperty(VERSION_RESPONSE, transaction.getVersion());
    if (transaction.getVersion() > 0) {
      json.addProperty(EC_BLOCK_ID_RESPONSE, Convert.toUnsignedLong(transaction.getECBlockId()));
      json.addProperty(EC_BLOCK_HEIGHT_RESPONSE, transaction.getECBlockHeight());
    }

    return json;
  }

  public static JsonObject transaction(Transaction transaction, int currentBlockchainHeight) {
    JsonObject json = unconfirmedTransaction(transaction);
    json.addProperty(BLOCK_RESPONSE, Convert.toUnsignedLong(transaction.getBlockId()));
    json.addProperty(CONFIRMATIONS_RESPONSE, currentBlockchainHeight - transaction.getHeight());
    json.addProperty(BLOCK_TIMESTAMP_RESPONSE, transaction.getBlockTimestamp());
    return json;
  }

  // ugly, hopefully temporary
  private static void modifyAttachmentJSON(JsonObject json) {
    JsonElement quantityQNT = json.remove(QUANTITY_QNT_RESPONSE);
    if (quantityQNT != null && quantityQNT.isJsonPrimitive()) {
      json.addProperty(QUANTITY_QNT_RESPONSE, quantityQNT.getAsString());
    }
    JsonElement priceNQT = json.remove(PRICE_NQT_RESPONSE);
    if (priceNQT != null && priceNQT.isJsonPrimitive()) {
      json.addProperty(PRICE_NQT_RESPONSE, priceNQT.getAsString());
    }
    JsonElement discountNQT = json.remove(DISCOUNT_NQT_RESPONSE);
    if (discountNQT != null && discountNQT.isJsonPrimitive()) {
      json.addProperty(DISCOUNT_NQT_RESPONSE, discountNQT.getAsString());
    }
    JsonElement refundNQT = json.remove(REFUND_NQT_RESPONSE);
    if (refundNQT != null && refundNQT.isJsonPrimitive()) {
      json.addProperty(REFUND_NQT_RESPONSE, refundNQT.getAsString());
    }
  }

  static void putAccount(JsonObject json, String name, long accountId) {
    json.addProperty(name, Convert.toUnsignedLong(accountId));
    json.addProperty(name + "RS", Convert.rsAccount(accountId));
  }

  //TODO refactor the accountservice out of this :-)
  static JsonObject at(AT at, AccountService accountService) {
    JsonObject json = new JsonObject();
    ByteBuffer bf = ByteBuffer.allocate( 8 );
    bf.order( ByteOrder.LITTLE_ENDIAN );

    bf.put( at.getCreator() );
    bf.clear();
    putAccount(json, "creator", bf.getLong() ); // TODO is this redundant or does this bring LE byte order?
    bf.clear();
    bf.put( at.getId() , 0 , 8 );
    long id = bf.getLong(0);
    json.addProperty("at", Convert.toUnsignedLong( id ));
    json.addProperty("atRS", Convert.rsAccount(id));
    json.addProperty("atVersion", at.getVersion());
    json.addProperty("name", at.getName());
    json.addProperty("description", at.getDescription());
    json.addProperty("creator", Convert.toUnsignedLong(AtApiHelper.getLong(at.getCreator())));
    json.addProperty("creatorRS", Convert.rsAccount(AtApiHelper.getLong(at.getCreator())));
    json.addProperty("machineCode", Convert.toHexString(at.getApCodeBytes()));
    json.addProperty("machineData", Convert.toHexString(at.getApDataBytes()));
    json.addProperty("balanceNQT", Convert.toUnsignedLong(accountService.getAccount(id).getBalanceNQT()));
    json.addProperty("prevBalanceNQT", Convert.toUnsignedLong(at.getpBalance()));
    json.addProperty("nextBlock", at.nextHeight());
    json.addProperty("frozen", at.freezeOnSameBalance());
    json.addProperty("running", at.getMachineState().isRunning());
    json.addProperty("stopped", at.getMachineState().isStopped());
    json.addProperty("finished", at.getMachineState().isFinished());
    json.addProperty("dead", at.getMachineState().isDead());
    json.addProperty("minActivation", Convert.toUnsignedLong(at.minActivationAmount()));
    json.addProperty("creationBlock", at.getCreationBlockHeight());
    return json;
  }

  static JsonObject hex2long(String longString){
    JsonObject json = new JsonObject();
    json.addProperty("hex2long", longString);
    return json;
  }

  private JSONData() {} // never

}
