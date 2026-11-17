import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerDaoImpl implements CustomerDAO
{
    @Override
    public void save(Customer customer)
    {
        String customerType = getCustomerType(customer);
        String sql = "INSERT INTO Customers "
                + "(customer_id, customer_type, address, phone_number) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                try (PreparedStatement statement = connection.prepareStatement(sql))
                {
                    statement.setString(1, customer.getCustomerID());
                    statement.setString(2, customerType);
                    statement.setString(3, customer.getAddress());
                    statement.setString(4, customer.getPhoneNumber());
                    statement.executeUpdate();
                }

                saveSubtype(connection, customer, customerType);
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
            throw new IllegalStateException("Could not save customer.", exception);
        }
    }

    @Override
    public Customer findById(String customerId)
    {
        Customer customer = findByIdWithoutAccounts(customerId);
        if (customer != null)
        {
            for (Account account : new AccountDaoImpl().getAllAccountsByCustomerId(customerId))
            {
                customer.addAccount(account);
            }
        }
        return customer;
    }

    Customer findByIdWithoutAccounts(String customerId)
    {
        String sql = "SELECT c.customer_type, c.address, c.phone_number, "
                + "i.first_name, i.last_name, i.national_id, i.age, "
                + "e.company_name AS employer_name, e.company_address, e.occupation, "
                + "cc.company_name AS corporate_name, cc.tax_id, cc.registration_number "
                + "FROM Customers c "
                + "LEFT JOIN IndividualCustomers i ON i.customer_id = c.customer_id "
                + "LEFT JOIN EmployedCustomers e ON e.customer_id = c.customer_id "
                + "LEFT JOIN CorporateCustomers cc ON cc.customer_id = c.customer_id "
                + "WHERE c.customer_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, customerId);
            try (ResultSet result = statement.executeQuery())
            {
                if (!result.next())
                {
                    return null;
                }

                String type = result.getString("customer_type");
                String address = result.getString("address");
                String phone = result.getString("phone_number");

                if ("EMPLOYED".equals(type))
                {
                    return new EmployedCustomer(
                            result.getString("employer_name"),
                            result.getString("company_address"),
                            result.getString("occupation"),
                            result.getString("first_name"),
                            result.getString("last_name"),
                            result.getString("national_id"),
                            result.getInt("age"),
                            address,
                            phone,
                            customerId);
                }
                if ("INDIVIDUAL".equals(type))
                {
                    return new IndividualCustomer(
                            result.getString("first_name"),
                            result.getString("last_name"),
                            result.getString("national_id"),
                            result.getInt("age"),
                            phone,
                            customerId,
                            address);
                }
                if ("CORPORATE".equals(type))
                {
                    return new CorporateCustomer(
                            address,
                            customerId,
                            phone,
                            result.getString("corporate_name"),
                            result.getString("tax_id"),
                            result.getString("registration_number"));
                }
                throw new SQLException("Unknown customer type: " + type);
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not find customer.", exception);
        }
    }

    @Override
    public boolean update(Customer customer)
    {
        String customerType = getCustomerType(customer);
        String sql = "UPDATE Customers SET address = ?, phone_number = ? "
                + "WHERE customer_id = ? AND customer_type = ?";

        try (Connection connection = DatabaseConnection.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                int rows;
                try (PreparedStatement statement = connection.prepareStatement(sql))
                {
                    statement.setString(1, customer.getAddress());
                    statement.setString(2, customer.getPhoneNumber());
                    statement.setString(3, customer.getCustomerID());
                    statement.setString(4, customerType);
                    rows = statement.executeUpdate();
                }

                if (rows == 0)
                {
                    connection.rollback();
                    return false;
                }

                updateSubtype(connection, customer);
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
            throw new IllegalStateException("Could not update customer.", exception);
        }
    }

    @Override
    public boolean delete(Customer customer)
    {
        String customerId = customer.getCustomerID();

        try (Connection connection = DatabaseConnection.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                String type;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT customer_type FROM Customers WHERE customer_id = ?"))
                {
                    statement.setString(1, customerId);
                    try (ResultSet result = statement.executeQuery())
                    {
                        if (!result.next())
                        {
                            connection.rollback();
                            return false;
                        }
                        type = result.getString("customer_type");
                    }
                }

                if ("EMPLOYED".equals(type))
                {
                    deleteSubtype(connection, "EmployedCustomers", customerId);
                    deleteSubtype(connection, "IndividualCustomers", customerId);
                }
                else if ("INDIVIDUAL".equals(type))
                {
                    deleteSubtype(connection, "IndividualCustomers", customerId);
                }
                else if ("CORPORATE".equals(type))
                {
                    deleteSubtype(connection, "CorporateCustomers", customerId);
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM Customers WHERE customer_id = ?"))
                {
                    statement.setString(1, customerId);
                    boolean deleted = statement.executeUpdate() == 1;
                    connection.commit();
                    return deleted;
                }
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
                    "Could not delete customer. Check whether accounts still reference it.", exception);
        }
    }

    @Override
    public List<Customer> getAll()
    {
        List<String> customerIds = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("SELECT customer_id FROM Customers ORDER BY customer_id");
             ResultSet result = statement.executeQuery())
        {
            while (result.next())
            {
                customerIds.add(result.getString("customer_id"));
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not get customers.", exception);
        }

        List<Customer> customers = new ArrayList<>();
        for (String customerId : customerIds)
        {
            customers.add(findById(customerId));
        }
        return customers;
    }

    private String getCustomerType(Customer customer)
    {
        if (customer instanceof EmployedCustomer)
        {
            return "EMPLOYED";
        }
        if (customer instanceof CorporateCustomer)
        {
            return "CORPORATE";
        }
        if (customer instanceof IndividualCustomer)
        {
            return "INDIVIDUAL";
        }
        throw new IllegalArgumentException("Unsupported customer type.");
    }

    private void saveSubtype(Connection connection, Customer customer, String type) throws SQLException
    {
        if (customer instanceof IndividualCustomer)
        {
            IndividualCustomer individual = (IndividualCustomer) customer;
            String sql = "INSERT INTO IndividualCustomers "
                    + "(customer_id, customer_type, first_name, last_name, national_id, age) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql))
            {
                statement.setString(1, individual.getCustomerID());
                statement.setString(2, type);
                statement.setString(3, individual.getFirstName());
                statement.setString(4, individual.getLastName());
                statement.setString(5, individual.getNationalD());
                statement.setInt(6, individual.getAge());
                statement.executeUpdate();
            }
        }

        if (customer instanceof EmployedCustomer)
        {
            EmployedCustomer employed = (EmployedCustomer) customer;
            String sql = "INSERT INTO EmployedCustomers "
                    + "(customer_id, company_name, company_address, occupation) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql))
            {
                statement.setString(1, employed.getCustomerID());
                statement.setString(2, employed.getCompanyName());
                statement.setString(3, employed.getCompanyAddress());
                statement.setString(4, employed.getOccupation());
                statement.executeUpdate();
            }
        }
        else if (customer instanceof CorporateCustomer)
        {
            CorporateCustomer corporate = (CorporateCustomer) customer;
            String sql = "INSERT INTO CorporateCustomers "
                    + "(customer_id, company_name, tax_id, registration_number) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql))
            {
                statement.setString(1, corporate.getCustomerID());
                statement.setString(2, corporate.getCompanyName());
                statement.setString(3, corporate.getTaxID());
                statement.setString(4, corporate.getRegistrationNumber());
                statement.executeUpdate();
            }
        }
    }

    private void updateSubtype(Connection connection, Customer customer) throws SQLException
    {
        if (customer instanceof IndividualCustomer)
        {
            IndividualCustomer individual = (IndividualCustomer) customer;
            String sql = "UPDATE IndividualCustomers SET first_name = ?, last_name = ?, "
                    + "national_id = ?, age = ? WHERE customer_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql))
            {
                statement.setString(1, individual.getFirstName());
                statement.setString(2, individual.getLastName());
                statement.setString(3, individual.getNationalD());
                statement.setInt(4, individual.getAge());
                statement.setString(5, individual.getCustomerID());
                statement.executeUpdate();
            }
        }

        if (customer instanceof EmployedCustomer)
        {
            EmployedCustomer employed = (EmployedCustomer) customer;
            String sql = "UPDATE EmployedCustomers SET company_name = ?, company_address = ?, "
                    + "occupation = ? WHERE customer_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql))
            {
                statement.setString(1, employed.getCompanyName());
                statement.setString(2, employed.getCompanyAddress());
                statement.setString(3, employed.getOccupation());
                statement.setString(4, employed.getCustomerID());
                statement.executeUpdate();
            }
        }
        else if (customer instanceof CorporateCustomer)
        {
            CorporateCustomer corporate = (CorporateCustomer) customer;
            String sql = "UPDATE CorporateCustomers SET company_name = ?, tax_id = ?, "
                    + "registration_number = ? WHERE customer_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql))
            {
                statement.setString(1, corporate.getCompanyName());
                statement.setString(2, corporate.getTaxID());
                statement.setString(3, corporate.getRegistrationNumber());
                statement.setString(4, corporate.getCustomerID());
                statement.executeUpdate();
            }
        }
    }

    private void deleteSubtype(Connection connection, String table, String customerId) throws SQLException
    {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM " + table + " WHERE customer_id = ?"))
        {
            statement.setString(1, customerId);
            statement.executeUpdate();
        }
    }
}
