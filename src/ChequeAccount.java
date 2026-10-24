public class ChequeAccount extends Account implements WithdrawalAccount
{
    private String companyName;
    private String companyAddress;

    public  ChequeAccount(String companyName, String companyAddress, String accountNumber, double balance,String dateOpened, double minimalDeposit,Customer customer,String accountType)
    {
        super(accountNumber, balance, dateOpened, minimalDeposit, accountType, customer);
        this.companyName = companyName;
        this.companyAddress = companyAddress;

    }

    public String getCompanyAddress() {
        return companyAddress;
    }
    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }
    public String getCompanyName() {
        return companyName;

    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;

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

