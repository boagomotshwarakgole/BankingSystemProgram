import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public class AccountOpeningService
{
    private final AccountDAO accountDAO;
    private final CustomerDAO customerDAO;

    public AccountOpeningService()
    {
        this(new AccountDaoImpl(), new CustomerDaoImpl());
    }

    AccountOpeningService(AccountDAO accountDAO, CustomerDAO customerDAO)
    {
        this.accountDAO = accountDAO;
        this.customerDAO = customerDAO;
    }

    public Account openAccount(
            String customerId,
            String accountType,
            double openingDeposit,
            String companyName,
            String companyAddress,
            String occupation)
    {
        if (!Double.isFinite(openingDeposit) || openingDeposit < 0)
        {
            throw new IllegalArgumentException("Opening deposit must be zero or more.");
        }

        Customer customer = customerDAO.findById(customerId);
        if (!(customer instanceof IndividualCustomer))
        {
            throw new IllegalStateException("Only an individual customer can open these accounts.");
        }

        String accountNumber = "ACC-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        String dateOpened = LocalDate.now().toString();
        Account account;
        String type = accountType == null ? "" : accountType.trim().toUpperCase(Locale.ROOT);
        switch (type)
        {
            case "SAVINGS":
                account = new SavingsAccount(
                        openingDeposit, dateOpened, 0, "SAVINGS", customer, accountNumber);
                break;
            case "INVESTMENT":
                if (openingDeposit < 500)
                {
                    throw new IllegalArgumentException(
                            "An investment account requires a minimum opening deposit of BWP 500.");
                }
                account = new InvestmentAccount(
                        accountNumber, openingDeposit, dateOpened, 500, "INVESTMENT", customer);
                break;
            case "CHEQUE":
                requireText(companyName, "Employer name");
                requireText(companyAddress, "Employer address");
                requireText(occupation, "Occupation");
                account = new ChequeAccount(
                        companyName.trim(),
                        companyAddress.trim(),
                        accountNumber,
                        openingDeposit,
                        dateOpened,
                        0,
                        customer,
                        "CHEQUE");
                break;
            default:
                throw new IllegalArgumentException("Select Savings, Investment, or Cheque as the account type.");
        }

        accountDAO.openAccount(account, occupation);
        return account;
    }

    private void requireText(String value, String label)
    {
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException(label + " is required.");
        }
    }
}
