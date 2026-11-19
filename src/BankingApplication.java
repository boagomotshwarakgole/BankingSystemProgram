import javafx.application.Application;
import javafx.stage.Stage;

public class BankingApplication extends Application
{
    @Override
    public void start(Stage primaryStage)
    {
        try
        {
            new MonthlyInterestService().applyDueInterest();
        }
        catch (IllegalStateException exception)
        {
            System.err.println("Could not apply monthly interest: " + exception.getMessage());
        }
        new LoginController(primaryStage, new PasswordAuthentication());
    }
}
