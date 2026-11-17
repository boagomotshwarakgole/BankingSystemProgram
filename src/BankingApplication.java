import javafx.application.Application;
import javafx.stage.Stage;

public class BankingApplication extends Application
{
    @Override
    public void start(Stage primaryStage)
    {
        new LoginController(primaryStage, new PasswordAuthentication());
    }
}
