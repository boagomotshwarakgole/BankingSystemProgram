import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AccountDaoImpl implements AccountDAO
{
    @Override
    public void save(Account account)
    {
        String sql = "INSERT INTO Accounts "
                + "(account_number, customer_id, account_type, balance, date_opened, minimum_deposit) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, account.getAccountNumber());
            statement.setString(2, account.getCustomer().getCustomerID());
            statement.setString(3, getAccountType(account));
            statement.setDouble(4, account.getBalance());
            statement.setString(5, account.getDateOpened());
            statement.setDouble(6, account.getMinimalDeposit());
            statement.executeUpdate();
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not save account.", exception);
        }
    }

    @Override
    public void openAccount(
            Account account,
            String occupation)
    {
        if (!Double.isFinite(account.getBalance()) || account.getBalance() < 0)
        {
            throw new IllegalArgumentException("Opening deposit must be a valid amount of zero or more.");
        }

        boolean chequeAccount = account instanceof ChequeAccount;
        String accountType = getAccountType(account);
        if (!"SAVINGS".equals(accountType)
                && !"INVESTMENT".equals(accountType)
                && !"CHEQUE".equals(accountType))
        {
            throw new IllegalArgumentException("Unsupported account type.");
        }
        if (chequeAccount != "CHEQUE".equals(accountType))
        {
            throw new IllegalArgumentException("Account type does not match the account details.");
        }
        if (chequeAccount)
        {
            ChequeAccount cheque = (ChequeAccount) account;
            validateEmploymentDetails(
                    cheque.getCompanyName(),
                    cheque.getCompanyAddress(),
                    occupation);
        }
        else if ("INVESTMENT".equals(accountType) && account.getBalance() < 500)
        {
            throw new IllegalArgumentException("An investment account requires a minimum opening deposit of BWP 500.");
        }

        String customerTypeSql = "UPDATE Customers SET customer_type = 'EMPLOYED' "
                + "WHERE customer_id = ? AND customer_type IN ('INDIVIDUAL', 'EMPLOYED')";
        String employmentSql = "INSERT INTO EmployedCustomers "
                + "(customer_id, company_name, company_address, occupation) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT(customer_id) DO UPDATE SET company_name = excluded.company_name, "
                + "company_address = excluded.company_address, occupation = excluded.occupation";
        String accountSql = "INSERT INTO Accounts "
                + "(account_number, customer_id, account_type, balance, date_opened, minimum_deposit) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String chequeDetailsSql = "INSERT INTO ChequeAccountDetails "
                + "(account_number, company_name, company_address) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                if (chequeAccount)
                {
                    try (PreparedStatement statement = connection.prepareStatement(customerTypeSql))
                    {
                        statement.setString(1, account.getCustomer().getCustomerID());
                        if (statement.executeUpdate() != 1)
                        {
                            throw new IllegalArgumentException(
                                    "Only an individual customer can open a cheque account.");
                        }
                    }

                    try (PreparedStatement statement = connection.prepareStatement(employmentSql))
                    {
                        ChequeAccount cheque = (ChequeAccount) account;
                        statement.setString(1, account.getCustomer().getCustomerID());
                        statement.setString(2, cheque.getCompanyName());
                        statement.setString(3, cheque.getCompanyAddress());
                        statement.setString(4, occupation.trim());
                        statement.executeUpdate();
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(accountSql))
                {
                    statement.setString(1, account.getAccountNumber());
                    statement.setString(2, account.getCustomer().getCustomerID());
                    statement.setString(3, getAccountType(account));
                    statement.setDouble(4, account.getBalance());
                    statement.setString(5, account.getDateOpened());
                    statement.setDouble(6, account.getMinimalDeposit());
                    statement.executeUpdate();
                }

                if (chequeAccount)
                {
                    ChequeAccount cheque = (ChequeAccount) account;
                    try (PreparedStatement details = connection.prepareStatement(chequeDetailsSql))
                    {
                        details.setString(1, account.getAccountNumber());
                        details.setString(2, cheque.getCompanyName().trim());
                        details.setString(3, cheque.getCompanyAddress().trim());
                        details.executeUpdate();
                    }
                }

                if (account.getBalance() > 0)
                {
                    String transactionSql = "INSERT INTO Transactions "
                            + "(transaction_id, transaction_type, account_number, amount, transaction_time) "
                            + "VALUES (?, 'DEPOSIT', ?, ?, CURRENT_TIMESTAMP)";
                    try (PreparedStatement statement = connection.prepareStatement(transactionSql))
                    {
                        statement.setString(1, "TRX-" + java.util.UUID.randomUUID()
                                .toString().replace("-", "").substring(0, 12).toUpperCase(java.util.Locale.ROOT));
                        statement.setString(2, account.getAccountNumber());
                        statement.setDouble(3, account.getBalance());
                        statement.executeUpdate();
                    }
                }

                connection.commit();
            }
            catch (SQLException | RuntimeException exception)
            {
                connection.rollback();
                throw exception;
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not open account: " + exception.getMessage(), exception);
        }
    }

    @Override
    public Account findById(String accountNumber)
    {
        String sql = "SELECT a.account_number, a.account_type, a.balance, a.date_opened, "
                + "a.minimum_deposit, c.customer_id, "
                + "COALESCE(q.company_name, e.company_name) AS company_name, "
                + "COALESCE(q.company_address, e.company_address) AS company_address "
                + "FROM Accounts a JOIN Customers c ON c.customer_id = a.customer_id "
                + "LEFT JOIN EmployedCustomers e ON e.customer_id = c.customer_id "
                + "LEFT JOIN ChequeAccountDetails q ON q.account_number = a.account_number "
                + "WHERE a.account_number = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, accountNumber);
            try (ResultSet result = statement.executeQuery())
            {
                if (!result.next())
                {
                    return null;
                }

                Customer customer = new CustomerDaoImpl()
                        .findByIdWithoutAccounts(result.getString("customer_id"));
                if (customer == null)
                {
                    throw new SQLException("Account references a missing customer.");
                }
                String type = result.getString("account_type");
                double balance = result.getDouble("balance");
                String dateOpened = result.getString("date_opened");
                double minimumDeposit = result.getDouble("minimum_deposit");

                if ("SAVINGS".equals(type))
                {
                    return new SavingsAccount(
                            balance, dateOpened, minimumDeposit, type, customer, accountNumber);
                }
                if ("INVESTMENT".equals(type))
                {
                    return new InvestmentAccount(
                            accountNumber, balance, dateOpened, minimumDeposit, type, customer);
                }
                if ("CHEQUE".equals(type))
                {
                    return new ChequeAccount(
                            result.getString("company_name"),
                            result.getString("company_address"),
                            accountNumber,
                            balance,
                            dateOpened,
                            minimumDeposit,
                            customer,
                            type);
                }
                throw new SQLException("Unknown account type: " + type);
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not find account.", exception);
        }
    }

    @Override
    public boolean update(Account account)
    {
        String sql = "UPDATE Accounts SET customer_id = ?, account_type = ?, balance = ?, "
                + "date_opened = ?, minimum_deposit = ? WHERE account_number = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, account.getCustomer().getCustomerID());
            statement.setString(2, getAccountType(account));
            statement.setDouble(3, account.getBalance());
            statement.setString(4, account.getDateOpened());
            statement.setDouble(5, account.getMinimalDeposit());
            statement.setString(6, account.getAccountNumber());
            return statement.executeUpdate() == 1;
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not update account.", exception);
        }
    }

    @Override
    public boolean processTransaction(Transaction transaction, Account destinationAccount)
    {
        if (transaction == null)
        {
            throw new IllegalArgumentException("Transaction is required.");
        }

        try (Connection connection = DatabaseConnection.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                transaction.execute();
                updateBalance(connection, transaction.getAccount());

                if (destinationAccount != null)
                {
                    updateBalance(connection, destinationAccount);
                }
                else if (transaction instanceof TransferTransaction)
                {
                    updateBalance(
                            connection,
                            ((TransferTransaction) transaction).getDestinationAccount());
                }

                String sql = "INSERT INTO Transactions "
                        + "(transaction_id, transaction_type, account_number, "
                        + "destination_account_number, amount, transaction_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql))
                {
                    statement.setString(1, transaction.getTransactionID());
                    statement.setString(2, getTransactionType(transaction));
                    statement.setString(3, transaction.getAccount().getAccountNumber());
                    if (transaction instanceof TransferTransaction)
                    {
                        statement.setString(
                                4,
                                ((TransferTransaction) transaction)
                                        .getDestinationAccount().getAccountNumber());
                    }
                    else
                    {
                        statement.setNull(4, java.sql.Types.VARCHAR);
                    }
                    statement.setDouble(5, transaction.getAmount());
                    statement.setString(
                            6,
                            LocalDateTime.parse(transaction.getTimestamp()).toString());
                    statement.executeUpdate();
                }

                connection.commit();
                return true;
            }
            catch (SQLException | RuntimeException exception)
            {
                connection.rollback();
                throw exception;
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException(
                    "Could not save the transaction consistently.",
                    exception);
        }
    }

    @Override
    public boolean delete(Account account)
    {
        String sql = "DELETE FROM Accounts WHERE account_number = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, account.getAccountNumber());
            return statement.executeUpdate() == 1;
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException(
                    "Could not delete account. Check whether transactions still reference it.", exception);
        }
    }

    @Override
    public List<Account> getAll()
    {
        return getAccounts("SELECT account_number FROM Accounts ORDER BY account_number", null);
    }

    @Override
    public List<Account> getAllAccountsByCustomerId(String customerId)
    {
        return getAccounts(
                "SELECT account_number FROM Accounts WHERE customer_id = ? ORDER BY account_number",
                customerId);
    }

    private List<Account> getAccounts(String sql, String customerId)
    {
        List<String> accountNumbers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            if (customerId != null)
            {
                statement.setString(1, customerId);
            }
            try (ResultSet result = statement.executeQuery())
            {
                while (result.next())
                {
                    accountNumbers.add(result.getString("account_number"));
                }
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not get accounts.", exception);
        }

        List<Account> accounts = new ArrayList<>();
        for (String accountNumber : accountNumbers)
        {
            accounts.add(findById(accountNumber));
        }
        return accounts;
    }

    private void updateBalance(Connection connection, Account account) throws SQLException
    {
        String sql = "UPDATE Accounts SET balance = ? WHERE account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setDouble(1, account.getBalance());
            statement.setString(2, account.getAccountNumber());
            if (statement.executeUpdate() != 1)
            {
                throw new SQLException("Account balance was not updated: "
                        + account.getAccountNumber());
            }
        }
    }

    private String getTransactionType(Transaction transaction)
    {
        if (transaction instanceof TransferTransaction)
        {
            return "TRANSFER";
        }
        if (transaction instanceof DepositTransaction)
        {
            return "DEPOSIT";
        }
        if (transaction instanceof WithdrawalTransaction)
        {
            return "WITHDRAWAL";
        }
        if (transaction instanceof InterestTransaction)
        {
            return "INTEREST";
        }
        throw new IllegalArgumentException("Unsupported transaction type.");
    }

    private String getAccountType(Account account)
    {
        if (account instanceof SavingsAccount)
        {
            return "SAVINGS";
        }
        if (account instanceof InvestmentAccount)
        {
            return "INVESTMENT";
        }
        if (account instanceof ChequeAccount)
        {
            return "CHEQUE";
        }
        return account.getAccountType();
    }

    private void validateEmploymentDetails(
            String companyName,
            String companyAddress,
            String occupation)
    {
        if (companyName == null || companyName.isBlank()
                || companyAddress == null || companyAddress.isBlank()
                || occupation == null || occupation.isBlank())
        {
            throw new IllegalArgumentException(
                    "Employer name, employer address, and occupation are required for a cheque account.");
        }
    }
}
