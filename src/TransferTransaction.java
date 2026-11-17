public class TransferTransaction extends Transaction
{
  private Account destinationAccount ;

  public TransferTransaction(String transactionID, Account accountNumber, double amount,
                             String timestamp, Account account, Account destinationAccount )
  {
      super(transactionID, accountNumber, amount, timestamp, account);
      this.destinationAccount =  destinationAccount;
  }

  public Account getDestinationAccount()
      {
      return destinationAccount;
      }

      public  void setDestinationAccount(Account destinationAccount)
      {
        this.destinationAccount = destinationAccount;
      }


      @Override
      public void execute() {

        Account sourceAccount = getAccount();

        if (sourceAccount == null || destinationAccount == null)
        {
            throw new IllegalArgumentException("Source and destination accounts are required.");
        }
        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber()))
        {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }
        if (getAmount() <= 0)
        {
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        }
        if (!(sourceAccount instanceof WithdrawalAccount))
        {
            throw new IllegalArgumentException("Source account does not allow withdrawals.");
        }
        if (sourceAccount.getBalance() < getAmount())
        {
            throw new IllegalArgumentException("Insufficient balance for this transfer.");
        }

        ((WithdrawalAccount) sourceAccount).withdraw(getAmount());
        destinationAccount.deposit(getAmount());
      }
}

