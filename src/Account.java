public class Account
{
    private String accountNumber;
    private double balance;
    private String dateOpened;
    private double minimalDeposit;
    private String accountType;
    private Customer customer;

    public  Account( String accountNumber, double balance, String dateOpened, double minimalDeposit, String accountType , Customer customer )
    {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.dateOpened = dateOpened;
        this.minimalDeposit = minimalDeposit;
        this.accountType = accountType;
        this.customer = customer;

    }

    public String getAccountNumber()
    {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber)
    {
        this.accountNumber = accountNumber;
    }

    public double getBalance()
    {
        return balance;
    }
    public void setBalance(double balance)
    {
        this.balance = balance;
    }

    public String getDateOpened()
    {
        return dateOpened;
    }

    public void setDateOpened(String dateOpened)
    {
        this.dateOpened = dateOpened;
    }

    public double getMinimalDeposit()
    {
        return minimalDeposit;
    }

    public void setMinimalDeposit(double minimalDeposit) {
        this.minimalDeposit = minimalDeposit;
    }

    public String getAccountType()
    {
        return accountType;
    }

    public void setAccountType(String accountType)
    {
        this.accountType = accountType;
    }

    public Customer getCustomer()
    {
        return customer;
    }

    public void setCustomer(Customer customer)
    {
        this.customer = customer;
    }

    public void deposit(double amount)
    {
        if (amount > 0)
            {
            this.balance += amount;
            }
    }

    public void updatebalance()
    {
        this.balance += this.minimalDeposit;
    }

}
