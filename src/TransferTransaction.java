public abstract class TransferTransaction extends Transaction
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

             //basic validation//

              if(sourceAccount == null || destinationAccount == null){
                  System.out.println("Transfer Failed: Source or destination account is null.");
                  return;
              }

              if(getAmount() <= 0)
              {
                  System.out.println("Transfer Failed: Amount is greater than zero.");
                  return;
              }

              //check if the account suports Withdrawals//

          if(!(sourceAccount instanceof WithdrawalAccount))
          {
              System.out.println("Transfer Failed: Source account is not of type WithdrawalAccount");

              return;
          }


          //check if the Account has enough Money//
          if(sourceAccount.getBalance() < getAmount())
          {
              System.out.println("Transfer Failed: Amount is Insufficient Balance.Available Balance: " + sourceAccount.getBalance() + "Transfer amount: " + getAmount());
              return;
          }

          //safe to transfer .. withdraw from source to destination//
          WithdrawalAccount withdrawableSource = (WithdrawalAccount) sourceAccount;
          withdrawableSource.withdraw(getAmount());
          destinationAccount.deposit(getAmount());

          System.out.println("Transfer successful! " + getAmount() + " transferred to " + destinationAccount.getAccountNumber());
      }
}




