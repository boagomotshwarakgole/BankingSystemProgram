/* The first Subclass that presents the Coporate Customer.*/

public class CorporateCustomer extends  Customer
{
    private String CompanyName;
    private String CompanyAddress;
    private String taxID;

    public CorporateCustomer(String CompanyName, String CompanyAddress, String taxID)
    {
        this.CompanyName = CompanyName;
        this.CompanyAddress = CompanyAddress;
        this.taxID = taxID;
    }

    public String getCompanyName()
    {
        return CompanyName;
    }
    public String getCompanyAddress()
    {
        return CompanyAddress;

    }

    public String getTaxID()
    {
        return taxID;
    }
}
