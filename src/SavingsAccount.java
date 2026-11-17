public class SavingsAccount extends Account implements InterestBearing
{
  private final double INTEREST_RATE = 0.05;

  public SavingsAccount(double balance,String dateOpened,double minimalDeposit,String accountType,Customer customer, String accountNumber)
  {
    super(accountNumber , balance, dateOpened, minimalDeposit, accountType,customer);

  }

  public  double getInterestRate()
  {
    return INTEREST_RATE;
  }

  @Override
  public double calculateInterest()
  {
    return getBalance() * INTEREST_RATE/100;
  }

  @Override
  public void applyMonthlyInterest()
  {
    double interest = calculateInterest();
    deposit(interest);
  }

}
