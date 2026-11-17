public class InterestTransaction extends Transaction
{
    public InterestTransaction(String transactionID, Account account, double amount, String timestamp)
    {
        super(transactionID, account, amount, timestamp, account);
    }

    @Override
    public void execute()
    {
        if (getAccount() == null || getAmount() <= 0)
        {
            throw new IllegalArgumentException("Interest transaction details are invalid.");
        }
        getAccount().deposit(getAmount());
    }
}
