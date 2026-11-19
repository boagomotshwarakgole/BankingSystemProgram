import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection
{
    private static final Path DATABASE_PATH = getDatabasePath();
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_PATH;
    private static final String[] SCHEMA = {
        "CREATE TABLE IF NOT EXISTS Customers ("
                + "customer_id VARCHAR(20) PRIMARY KEY, "
                + "customer_type VARCHAR(20) NOT NULL CHECK "
                + "(customer_type IN ('EMPLOYED', 'INDIVIDUAL', 'CORPORATE')), "
                + "address VARCHAR(200) NOT NULL, "
                + "phone_number VARCHAR(30) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS IndividualCustomers ("
                + "customer_id VARCHAR(20) PRIMARY KEY REFERENCES Customers(customer_id) ON DELETE CASCADE, "
                + "customer_type VARCHAR(20) NOT NULL, "
                + "first_name VARCHAR(80) NOT NULL, "
                + "last_name VARCHAR(80) NOT NULL, "
                + "national_id VARCHAR(30) NOT NULL UNIQUE, "
                + "age INTEGER NOT NULL CHECK (age > 0))",
        "CREATE TABLE IF NOT EXISTS EmployedCustomers ("
                + "customer_id VARCHAR(20) PRIMARY KEY REFERENCES IndividualCustomers(customer_id) ON DELETE CASCADE, "
                + "company_name VARCHAR(150) NOT NULL, "
                + "company_address VARCHAR(200) NOT NULL, "
                + "occupation VARCHAR(100) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS CorporateCustomers ("
                + "customer_id VARCHAR(20) PRIMARY KEY REFERENCES Customers(customer_id) ON DELETE CASCADE, "
                + "company_name VARCHAR(150) NOT NULL, "
                + "tax_id VARCHAR(50) NOT NULL, "
                + "registration_number VARCHAR(50) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS Accounts ("
                + "account_number VARCHAR(30) PRIMARY KEY, "
                + "customer_id VARCHAR(20) NOT NULL REFERENCES Customers(customer_id), "
                + "account_type VARCHAR(20) NOT NULL CHECK "
                + "(account_type IN ('CHEQUE', 'SAVINGS', 'INVESTMENT')), "
                + "balance DECIMAL(19, 2) NOT NULL CHECK (balance >= 0), "
                + "date_opened DATE NOT NULL, "
                + "minimum_deposit DECIMAL(19, 2) NOT NULL CHECK (minimum_deposit >= 0))",
        "CREATE TABLE IF NOT EXISTS ChequeAccountDetails ("
                + "account_number VARCHAR(30) PRIMARY KEY REFERENCES Accounts(account_number) ON DELETE CASCADE, "
                + "company_name VARCHAR(150) NOT NULL, "
                + "company_address VARCHAR(200) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS EmploymentProofs ("
                + "account_number VARCHAR(30) PRIMARY KEY REFERENCES Accounts(account_number) ON DELETE CASCADE, "
                + "file_name VARCHAR(255) NOT NULL, "
                + "file_data BLOB NOT NULL, "
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
        "CREATE TABLE IF NOT EXISTS Transactions ("
                + "transaction_id VARCHAR(30) PRIMARY KEY, "
                + "transaction_type VARCHAR(20) NOT NULL CHECK "
                + "(transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'INTEREST')), "
                + "account_number VARCHAR(30) NOT NULL REFERENCES Accounts(account_number), "
                + "destination_account_number VARCHAR(30) REFERENCES Accounts(account_number), "
                + "amount DECIMAL(19, 2) NOT NULL CHECK (amount > 0), "
                + "transaction_time TIMESTAMP NOT NULL)",
        "CREATE TABLE IF NOT EXISTS UserCredentials ("
                + "user_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "customer_id VARCHAR(20) NOT NULL UNIQUE REFERENCES Customers(customer_id) ON DELETE CASCADE, "
                + "username VARCHAR(50) NOT NULL UNIQUE, "
                + "email VARCHAR(254) NOT NULL UNIQUE, "
                + "password_hash VARCHAR(255) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS PasswordResetTokens ("
                + "user_id INTEGER PRIMARY KEY REFERENCES UserCredentials(user_id) ON DELETE CASCADE, "
                + "token_hash CHAR(64) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS MonthlyInterestRuns ("
                + "year_month VARCHAR(7) PRIMARY KEY)",
        "CREATE INDEX IF NOT EXISTS IX_Accounts_CustomerId ON Accounts(customer_id)",
        "CREATE INDEX IF NOT EXISTS IX_Transactions_AccountTime "
                + "ON Transactions(account_number, transaction_time)",
        "CREATE INDEX IF NOT EXISTS IX_Transactions_DestinationTime "
                + "ON Transactions(destination_account_number, transaction_time)"
    };
    private static boolean initialized;

    private DatabaseConnection()
    {
    }

    private static Path getDatabasePath()
    {
        String configuredPath = System.getenv("BANKING_DB_PATH");
        if (configuredPath != null && !configuredPath.isBlank())
        {
            return Path.of(configuredPath).toAbsolutePath();
        }
        return Path.of(System.getProperty("user.home"), ".banking-system", "banking.db");
    }

    public static Connection getConnection() throws SQLException
    {
        try
        {
            Files.createDirectories(DATABASE_PATH.getParent());
        }
        catch (java.io.IOException exception)
        {
            throw new SQLException("Could not create the local banking database directory.", exception);
        }

        Connection connection = DriverManager.getConnection(DATABASE_URL);
        try (Statement statement = connection.createStatement())
        {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            initializeSchema(connection);
            return connection;
        }
        catch (SQLException exception)
        {
            connection.close();
            throw exception;
        }
    }

    private static synchronized void initializeSchema(Connection connection) throws SQLException
    {
        if (initialized)
        {
            return;
        }

        try (Statement statement = connection.createStatement())
        {
            for (String sql : SCHEMA)
            {
                statement.execute(sql);
            }
        }
        initialized = true;
    }
}
