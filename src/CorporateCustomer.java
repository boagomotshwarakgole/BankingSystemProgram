/* The first Subclass that presents the Coporate Customer.*/

public class CorporateCustomer extends  Customer implements Corporate
{
    private String CompanyName;
    private String taxID;
    private String RegistrationNumber;

    public CorporateCustomer(String address, String customerID, String phoneNumber,
                             String companyName, String taxID, String registrationNumber)
    {
        super(address, customerID, phoneNumber);
        this.CompanyName = companyName;
        this.taxID = taxID;
        this.RegistrationNumber = registrationNumber;
    }

    public String getCompanyName()
    {
        return CompanyName;
    }

    @Override
    public String getTaxID()
    {
        return taxID;
    }

    @Override
    public String getRegistrationNumber()
    {
        return RegistrationNumber;
    }
}
