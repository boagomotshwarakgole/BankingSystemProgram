import java.util.List;


public interface AccountDAO extends GenericDao<Account> {

    List<Account> getAllAccountsByCustomerId(String customerId);

    boolean processTransaction(Transaction transaction, Account destinationAccount);

    void openAccount(
            Account account,
            String occupation);
}
