import java.util.List;


public interface AccountDAO extends GenericDao<Account> {

    List<Account> getAllAccountsByCustomerId(String customerId);
    
}
