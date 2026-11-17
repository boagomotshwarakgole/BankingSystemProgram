import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordAuthentication implements AuthenticationService
{
    private static final int HASH_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private String authenticatedCustomerId;

    @Override
    public boolean authenticate(String username, String password)
    {
        String sql = "SELECT customer_id, password_hash "
                + "FROM UserCredentials WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery())
            {
                if (!result.next() || !verifyPassword(password, result.getString("password_hash")))
                {
                    authenticatedCustomerId = null;
                    return false;
                }

                authenticatedCustomerId = result.getString("customer_id");
                return true;
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException(
                    "Could not reach the banking database: " + exception.getMessage(),
                    exception);
        }
    }

    @Override
    public boolean isAuthenticated()
    {
        return authenticatedCustomerId != null;
    }

    @Override
    public String getAuthenticatedCustomerId()
    {
        return authenticatedCustomerId;
    }

    @Override
    public void register(
            String firstName,
            String lastName,
            String nationalId,
            int age,
            String address,
            String phoneNumber,
            String username,
            String email,
            String password)
    {
        requireText(firstName, "First name");
        requireText(lastName, "Last name");
        requireText(nationalId, "National ID");
        requireText(address, "Address");
        requireText(phoneNumber, "Phone number");
        requireText(username, "Username");
        requireText(email, "Email");
        if (age <= 0)
        {
            throw new IllegalArgumentException("Age must be a positive whole number.");
        }
        validatePassword(password);

        String customerId = "CUST-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        String customerSql = "INSERT INTO Customers "
                + "(customer_id, customer_type, address, phone_number) "
                + "VALUES (?, 'INDIVIDUAL', ?, ?)";
        String individualSql = "INSERT INTO IndividualCustomers "
                + "(customer_id, customer_type, first_name, last_name, national_id, age) "
                + "VALUES (?, 'INDIVIDUAL', ?, ?, ?, ?)";
        String credentialsSql = "INSERT INTO UserCredentials "
                + "(customer_id, username, email, password_hash) VALUES (?, ?, ?, ?)";
        String firstAccountSql = "INSERT INTO Accounts "
                + "(account_number, customer_id, account_type, balance, date_opened, minimum_deposit) "
                + "VALUES (?, ?, 'SAVINGS', 0, ?, 0)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement insertCustomer = connection.prepareStatement(customerSql);
             PreparedStatement insertIndividual = connection.prepareStatement(individualSql);
             PreparedStatement insertCredentials = connection.prepareStatement(credentialsSql);
             PreparedStatement insertFirstAccount = connection.prepareStatement(firstAccountSql))
        {
            connection.setAutoCommit(false);
            try
            {
                insertCustomer.setString(1, customerId);
                insertCustomer.setString(2, address);
                insertCustomer.setString(3, phoneNumber);
                insertCustomer.executeUpdate();

                insertIndividual.setString(1, customerId);
                insertIndividual.setString(2, firstName);
                insertIndividual.setString(3, lastName);
                insertIndividual.setString(4, nationalId);
                insertIndividual.setInt(5, age);
                insertIndividual.executeUpdate();

                insertCredentials.setString(1, customerId);
                insertCredentials.setString(2, username);
                insertCredentials.setString(3, email);
                insertCredentials.setString(4, hashPassword(password));
                insertCredentials.executeUpdate();

                insertFirstAccount.setString(1, "ACC-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
                insertFirstAccount.setString(2, customerId);
                insertFirstAccount.setString(3, LocalDate.now().toString());
                insertFirstAccount.executeUpdate();
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
            throw new IllegalStateException(
                    "Could not create the customer account: " + exception.getMessage(),
                    exception);
        }
    }

    @Override
    public String createPasswordResetToken(String email)
    {
        requireText(email, "Email");
        String token = createRandomToken();
        String sql = "SELECT user_id FROM UserCredentials WHERE email = ?";

        try (Connection connection = DatabaseConnection.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                int userId;
                try (PreparedStatement statement = connection.prepareStatement(sql))
                {
                    statement.setString(1, email);
                    try (ResultSet result = statement.executeQuery())
                    {
                        if (!result.next())
                        {
                            connection.rollback();
                            return null;
                        }
                        userId = result.getInt("user_id");
                    }
                }

                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM PasswordResetTokens WHERE user_id = ?");
                     PreparedStatement insert = connection.prepareStatement(
                             "INSERT INTO PasswordResetTokens (user_id, token_hash) VALUES (?, ?)"))
                {
                    delete.setInt(1, userId);
                    delete.executeUpdate();
                    insert.setInt(1, userId);
                    insert.setString(2, hashToken(token));
                    insert.executeUpdate();
                }

                connection.commit();
                return token;
            }
            catch (SQLException | RuntimeException exception)
            {
                connection.rollback();
                throw exception;
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not create password reset token.", exception);
        }
    }

    @Override
    public boolean resetPassword(String email, String token, String newPassword)
    {
        requireText(email, "Email");
        requireText(token, "Reset token");
        validatePassword(newPassword);

        String findSql = "SELECT u.user_id, t.token_hash "
                + "FROM UserCredentials u "
                + "JOIN PasswordResetTokens t ON t.user_id = u.user_id "
                + "WHERE u.email = ?";
        try (Connection connection = DatabaseConnection.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                int userId;
                String savedTokenHash;
                try (PreparedStatement statement = connection.prepareStatement(findSql))
                {
                    statement.setString(1, email);
                    try (ResultSet result = statement.executeQuery())
                    {
                        if (!result.next())
                        {
                            connection.rollback();
                            return false;
                        }
                        userId = result.getInt("user_id");
                        savedTokenHash = result.getString("token_hash");
                    }
                }

                if (!MessageDigest.isEqual(
                        hashToken(token).getBytes(StandardCharsets.US_ASCII),
                        savedTokenHash.getBytes(StandardCharsets.US_ASCII)))
                {
                    connection.rollback();
                    return false;
                }

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE UserCredentials SET password_hash = ? WHERE user_id = ?");
                     PreparedStatement delete = connection.prepareStatement(
                             "DELETE FROM PasswordResetTokens WHERE user_id = ?"))
                {
                    update.setString(1, hashPassword(newPassword));
                    update.setInt(2, userId);
                    update.executeUpdate();
                    delete.setInt(1, userId);
                    delete.executeUpdate();
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
            throw new IllegalStateException("Could not reset password.", exception);
        }
    }

    @Override
    public void logout()
    {
        authenticatedCustomerId = null;
    }

    private static String hashPassword(String password)
    {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derivePasswordHash(password, salt);
        return "pbkdf2$" + HASH_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verifyPassword(String password, String savedHash)
    {
        if (password == null || savedHash == null)
        {
            return false;
        }

        String[] parts = savedHash.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0]))
        {
            return false;
        }

        try
        {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = derivePasswordHash(password, salt, iterations);
            return MessageDigest.isEqual(expectedHash, actualHash);
        }
        catch (IllegalArgumentException exception)
        {
            return false;
        }
    }

    private static byte[] derivePasswordHash(String password, byte[] salt)
    {
        return derivePasswordHash(password, salt, HASH_ITERATIONS);
    }

    private static byte[] derivePasswordHash(String password, byte[] salt, int iterations)
    {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
        try
        {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        }
        catch (Exception exception)
        {
            throw new IllegalStateException("Could not hash password.", exception);
        }
        finally
        {
            spec.clearPassword();
        }
    }

    private static String createRandomToken()
    {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest)
            {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        }
        catch (Exception exception)
        {
            throw new IllegalStateException("Could not hash reset token.", exception);
        }
    }

    private static void validatePassword(String password)
    {
        if (password == null || password.length() < 8)
        {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
    }

    private static void requireText(String value, String field)
    {
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException(field + " is required.");
        }
    }
}
