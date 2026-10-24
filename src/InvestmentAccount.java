public class InvestmentAccount extends Account implements InterestBearing,WithdrawalAccount
{
    private final double INTEREST_RATE = 5.0;

    public InvestmentAccount(String accountNumber, double balance, String dateOpened, double minimalDeposit, String accountType, Customer customer)

    {
        super(accountNumber, balance, dateOpened, minimalDeposit, accountType, customer);


    }

    public double getINTEREST_RATE()
    {
        return INTEREST_RATE;
    }

    @Override
    public double calculateInterest()
    {
        return getBalance()*INTEREST_RATE/100;
    }

    @Override
    public void applyMonthlyInterest()
    {
        double interest = calculateInterest();
        deposit(interest);
    }

    @Override
    public void withdraw(double amount)
    {
        if(amount > 0 && amount <= getBalance() )
        {
            setBalance(getBalance() - amount);
        }
    }

}



