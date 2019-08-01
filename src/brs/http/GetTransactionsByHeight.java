package brs.http;

import brs.Blockchain;
import brs.Transaction;
import brs.services.BlockService;
import brs.util.Convert;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;

import java.util.Collection;

import static brs.http.common.Parameters.*;

/**
 * 根据高度和收款方地址查出交易记录
 */
public final class GetTransactionsByHeight extends APIServlet.JsonRequestHandler {

  private final Blockchain blockchain;

  GetTransactionsByHeight(Blockchain blockchain) {
    super(new APITag[] {APITag.TRANSACTIONS}, HEIGHT_PARAMETER);
    this.blockchain = blockchain;
  }

  @Override
  JsonElement processRequest(HttpServletRequest req) {
    String heightValue = Convert.emptyToNull(req.getParameter(HEIGHT_PARAMETER));

    Integer height = Integer.parseInt(heightValue);
    Collection<Transaction> recipientTransactions = blockchain.getTransactionsByHeightAndRecipient(height);

    JsonArray transactions = new JsonArray();
    for (Transaction transaction : recipientTransactions) {
      transactions.add(JSONData.transaction(transaction, blockchain.getHeight()));
    }

    JsonObject response = new JsonObject();
    response.add("transactions", transactions);

    return response;
  }
}
