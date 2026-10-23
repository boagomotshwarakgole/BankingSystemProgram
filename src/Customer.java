/* This class represents tHE common attributes for the types of customers available
which is  individual customer and corporate customer. it is a superclass tbh. */


import java.util.ArrayList
import java.util.List

public class Customer
{
    private String address;
    private String customerID;
    private String phoneNumber;
    private List<Account> accountList;

    //constructor//

    public Customer(String address, String customerID, String phoneNumber)
        {
        this.address = address;
        this.customerID = customerID;
        this.phoneNumber = phoneNumber;
        }

        // public Methods//

    public void addAccount(Account account)
    {
        this.accountList.add(account);
    }

    public List<Account> getAccountList() {
        return accountList;
    }

    public String getAddress() {
        return address;

    }
    public String getCustomerID() {
        return customerID;

    }
    public String getPhoneNumber() {
        return phoneNumber;

    }

}

