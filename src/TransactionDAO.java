import java.util.List;

public interface TransactionDAO extends GenericDao<Transaction> {
    List<Transaction> getAllTransactionsByAccountId(String accountNumber);
}