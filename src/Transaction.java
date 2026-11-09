public abstract class Transaction {

    private String TransactionID;
    private Account accountNumber;
    private double amount;
    private String timestamp;
    private Account account;

    public Transaction(String TransactionID,Account accountNumber,double amount,String timestamp,Account account)
    {
        this.TransactionID=TransactionID;
        this.accountNumber=accountNumber;
        this.amount=amount;
        this.timestamp=timestamp;
        this.account=account;
    }

    public abstract void execute();

    public String getTransactionID()
    {
        return TransactionID;
    }

    public Account getAccountNumber()
    {
        return accountNumber;
    }

    public double getAmount()
    {
        return amount;
    }

    public String getTimestamp()
    {
        return timestamp;

    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        this.account = account;
    }

    // Check to make sure nobody sets a negative or zero amount//

    public void setAmount(double amount)
    {
        if (amount > 0)
        {
            this.amount = amount;
        }
        else
        {
            System.out.println("Amount must be greater than zero");
        }
    }

    public void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp;
    }

    public void setTransactionID(String TransactionID)
    {
        this.TransactionID = TransactionID;
    }

    public void setAccountNumber(Account accountNumber)
    {
        this.accountNumber = accountNumber;
    }




}


