public class WithdrawalTransaction extends Transaction
{
    public WithdrawalTransaction(
            String transactionID, Account account, double amount, String timestamp)
    {
        super(transactionID, account, amount, timestamp, account);
    }

    @Override
    public void execute()
    {
        if (getAccount() == null || !(getAccount() instanceof WithdrawalAccount))
        {
            throw new IllegalStateException("This account does not allow withdrawals.");
        }
        if (getAmount() <= 0 || getAmount() > getAccount().getBalance())
        {
            throw new IllegalArgumentException("Withdrawal amount is invalid or exceeds the balance.");
        }

        ((WithdrawalAccount) getAccount()).withdraw(getAmount());
    }
}
