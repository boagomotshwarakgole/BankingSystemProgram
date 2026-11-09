public class DepositTransaction extends Transaction

{
     public DepositTransaction( String transactionID, Account accountNumber, double amount, String timestamp, Account account)
     {
         super(transactionID,accountNumber,amount,timestamp,account);
     }

     @Override
    public void execute()
     {
       if (getAmount() > 0 && getAccount() != null)
       {
           getAccount().deposit(getAmount());
       }
     }


}
