import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MonthlyInterestService
{
    private final AccountDAO accountDAO;

    public MonthlyInterestService()
    {
        this(new AccountDaoImpl());
    }

    MonthlyInterestService(AccountDAO accountDAO)
    {
        this.accountDAO = accountDAO;
    }

    public void applyDueInterest()
    {
        String yearMonth = YearMonth.now().toString();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement check = connection.prepareStatement(
                     "SELECT 1 FROM MonthlyInterestRuns WHERE year_month = ?"))
        {
            check.setString(1, yearMonth);
            try (ResultSet result = check.executeQuery())
            {
                if (result.next())
                {
                    return;
                }
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not check monthly interest.", exception);
        }

        List<Account> accounts = accountDAO.getAll();
        for (Account account : accounts)
        {
            if (!(account instanceof InterestBearing))
            {
                continue;
            }

            double interest = ((InterestBearing) account).calculateInterest();
            if (interest <= 0)
            {
                continue;
            }

            String transactionId = "TRX-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
            InterestTransaction transaction = new InterestTransaction(
                    transactionId,
                    account,
                    interest,
                    LocalDateTime.now().withNano(0).toString());
            accountDAO.processTransaction(transaction, null);
        }

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO MonthlyInterestRuns (year_month) VALUES (?)"))
        {
            insert.setString(1, yearMonth);
            insert.executeUpdate();
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not record monthly interest.", exception);
        }
    }
}
