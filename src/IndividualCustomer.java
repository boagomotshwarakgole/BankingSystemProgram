public class IndividualCustomer extends Customer

{
    private String firstName;
    private String lastName;
    private String nationalID;
    private int age;

    public IndividualCustomer( String firstName, String lastName, String nationalID,int age, String phoneNumber, String customerID, String address )
    {
        super(address, customerID, phoneNumber);
        this.firstName = firstName;
        this.lastName = lastName;
        this.nationalID = nationalID;


    }

    public String getFirstName()
    {
        return firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public String getNationalD()
    {
        return nationalID;
    }
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    public void setNationalD(String nationalD)
    {
        this.nationalID = nationalD;
    }
 public int getAge()
     {
     return age;
     }
     public void setAge(int age)
     {
         this.age = age;
     }



}
