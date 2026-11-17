import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class AccountView
{
    private final Parent root;

    public AccountView(AuthenticationService authentication, Runnable logoutAction) throws IOException
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/BankingView.fxml"));
        loader.setControllerFactory(type -> {
            if (type == AccountController.class)
            {
                return new AccountController(authentication, logoutAction);
            }
            throw new IllegalArgumentException("Unexpected FXML controller: " + type.getName());
        });
        root = loader.load();
    }

    public Parent getRoot()
    {
        return root;
    }
}
