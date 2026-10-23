public class EmployedCustomer extends IndividualCustomer implements Employable
{
    private String companyName;
    private String companyAddress;
    private String occupation;


    public EmployedCustomer(String CompanyName,String CompanyAddress, String Occupation,String firstName, String lastName, String nationalID, int age, String address, String phoneNumber,String customerID)
    {
        super(firstName,lastName,nationalID,age,address,phoneNumber,customerID);
        this.companyName = CompanyName;
        this.companyAddress = CompanyAddress;
        this.occupation = Occupation;

    }

    @Override
    public String getCompanyName()
    {
        return companyName;
    }

    @Override
    public  String getCompanyAddress()
    {
        return companyAddress;
    }

    @Override
    public String getOccupation()
    {
        return occupation;

    }

    public void setCompanyName(String CompanyName)
    {
        this.companyName = CompanyName;
    }

    public void setCompanyAddress(String CompanyAddress)
    {
        this.companyAddress = CompanyAddress;
    }

    public void setOccupation(String Occupation)
    {
        this.occupation = Occupation;
    }

}
