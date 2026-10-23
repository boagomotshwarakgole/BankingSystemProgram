public class SavingsAccount extends Account
{
  private final double INTEREST_RATE;

  public SavingsAccount(double balance, double interestRate,String dateOpened,double minimalDeposit,String accountType,Customer customer, String accountNumber)
  {
    super(accountNumber , balance, dateOpened, minimalDeposit, accountType,customer);
    this.INTEREST_RATE = interestRate;
  }

  public  double getInterestRate()
  {
    return INTEREST_RATE;
  }

  public double calculateInterest()
  {
    return getBalance() * INTEREST_RATE/100;
  }

  public void applyMonthlyInterest()
  {
    double interest = calculateInterest();
    deposit(interest);
  }

}
