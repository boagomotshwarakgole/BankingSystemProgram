import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDaoImpl implements TransactionDAO
{
    @Override
    public void save(Transaction transaction)
    {
        String type = getTransactionType(transaction);
        String sourceAccount = transaction.getAccount().getAccountNumber();
        String destinationAccount = transaction instanceof TransferTransaction
                ? ((TransferTransaction) transaction).getDestinationAccount().getAccountNumber()
                : null;
        String sql = "INSERT INTO Transactions "
                + "(transaction_id, transaction_type, account_number, destination_account_number, amount, transaction_time) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, transaction.getTransactionID());
            statement.setString(2, type);
            statement.setString(3, sourceAccount);
            statement.setString(4, destinationAccount);
            statement.setDouble(5, transaction.getAmount());
            statement.setString(6, LocalDateTime.parse(transaction.getTimestamp()).toString());
            statement.executeUpdate();
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not save transaction.", exception);
        }
    }

    @Override
    public Transaction findById(String transactionId)
    {
        String sql = "SELECT transaction_id, transaction_type, account_number, "
                + "destination_account_number, amount, transaction_time "
                + "FROM Transactions WHERE transaction_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, transactionId);
            try (ResultSet result = statement.executeQuery())
            {
                return result.next() ? createTransaction(result) : null;
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not find transaction.", exception);
        }
    }

    @Override
    public boolean update(Transaction transaction)
    {
        String type = getTransactionType(transaction);
        String destinationAccount = transaction instanceof TransferTransaction
                ? ((TransferTransaction) transaction).getDestinationAccount().getAccountNumber()
                : null;
        String sql = "UPDATE Transactions SET transaction_type = ?, account_number = ?, "
                + "destination_account_number = ?, amount = ?, transaction_time = ? "
                + "WHERE transaction_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, type);
            statement.setString(2, transaction.getAccount().getAccountNumber());
            statement.setString(3, destinationAccount);
            statement.setDouble(4, transaction.getAmount());
            statement.setString(5, LocalDateTime.parse(transaction.getTimestamp()).toString());
            statement.setString(6, transaction.getTransactionID());
            return statement.executeUpdate() == 1;
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not update transaction.", exception);
        }
    }

    @Override
    public boolean delete(Transaction transaction)
    {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM Transactions WHERE transaction_id = ?"))
        {
            statement.setString(1, transaction.getTransactionID());
            return statement.executeUpdate() == 1;
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not delete transaction.", exception);
        }
    }

    @Override
    public List<Transaction> getAll()
    {
        return getTransactions(
                "SELECT transaction_id, transaction_type, account_number, "
                        + "destination_account_number, amount, transaction_time "
                        + "FROM Transactions ORDER BY transaction_time",
                null);
    }

    @Override
    public List<Transaction> getAllTransactionsByAccountId(String accountNumber)
    {
        return getTransactions(
                "SELECT transaction_id, transaction_type, account_number, "
                        + "destination_account_number, amount, transaction_time "
                        + "FROM Transactions "
                        + "WHERE account_number = ? OR destination_account_number = ? "
                        + "ORDER BY transaction_time",
                accountNumber);
    }

    private List<Transaction> getTransactions(String sql, String accountNumber)
    {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            if (accountNumber != null)
            {
                statement.setString(1, accountNumber);
                statement.setString(2, accountNumber);
            }
            try (ResultSet result = statement.executeQuery())
            {
                while (result.next())
                {
                    transactions.add(createTransaction(result));
                }
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not get transactions.", exception);
        }
        return transactions;
    }

    private Transaction createTransaction(ResultSet result) throws SQLException
    {
        String id = result.getString("transaction_id");
        String type = result.getString("transaction_type");
        String accountNumber = result.getString("account_number");
        double amount = result.getDouble("amount");
        String time = LocalDateTime.parse(
                result.getString("transaction_time").replace(' ', 'T')).toString();
        Account account = new AccountDaoImpl().findById(accountNumber);
        if (account == null)
        {
            throw new SQLException("Transaction references a missing account: " + accountNumber);
        }

        if ("DEPOSIT".equals(type))
        {
            return new DepositTransaction(id, account, amount, time, account);
        }
        if ("WITHDRAWAL".equals(type))
        {
            return new WithdrawalTransaction(id, account, amount, time);
        }
        if ("INTEREST".equals(type))
        {
            return new InterestTransaction(id, account, amount, time);
        }
        if ("TRANSFER".equals(type))
        {
            String destinationNumber = result.getString("destination_account_number");
            Account destination = new AccountDaoImpl().findById(destinationNumber);
            if (destination == null)
            {
                throw new SQLException("Transfer references a missing destination account: " + destinationNumber);
            }
            return new TransferTransaction(id, account, amount, time, account, destination);
        }
        throw new SQLException("Unknown transaction type: " + type);
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
        if (transaction instanceof TransferTransaction)
        {
            return "TRANSFER";
        }
        if (transaction instanceof WithdrawalTransaction)
        {
            return "WITHDRAWAL";
        }
        if (transaction instanceof InterestTransaction)
        {
            return "INTEREST";
        }
        throw new IllegalArgumentException("Unsupported transaction type: "
                + transaction.getClass().getSimpleName());
    }
}
