import java.io.IOException;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class LoginController
{
    private final Stage stage;
    private final AuthenticationService authentication;
    private final LoginView view;

    public LoginController(Stage stage, AuthenticationService authentication)
    {
        this.stage = stage;
        this.authentication = authentication;
        this.view = new LoginView();

        view.setLoginAction(this::login);
        view.setRegisterAction(this::register);
        view.setRequestResetAction(this::requestPasswordReset);
        view.setResetAction(this::resetPassword);

        stage.setTitle("Banking System");
        stage.setMinWidth(560);
        stage.setMinHeight(440);
        stage.setScene(new Scene(view, 620, 480));
        stage.show();
    }

    private void login()
    {
        try
        {
            if (authentication.authenticate(view.getLoginUsername(), view.getLoginPassword()))
            {
                showAccounts();
            }
            else
            {
                view.setMessage("Username or password is incorrect.");
            }
        }
        catch (IllegalStateException exception)
        {
            view.setMessage(exception.getMessage());
        }
    }

    private void register()
    {
        try
        {
            int age;
            try
            {
                age = Integer.parseInt(view.getRegisterAge());
            }
            catch (NumberFormatException exception)
            {
                view.setMessage("Enter your age as a whole number.");
                return;
            }

            authentication.register(
                    view.getRegisterFirstName(),
                    view.getRegisterLastName(),
                    view.getRegisterNationalId(),
                    age,
                    view.getRegisterAddress(),
                    view.getRegisterPhoneNumber(),
                    view.getRegisterUsername(),
                    view.getRegisterEmail(),
                    view.getRegisterPassword());
            view.setMessage("Customer profile and first savings account created. You can now sign in.");
        }
        catch (IllegalArgumentException | IllegalStateException exception)
        {
            view.setMessage(exception.getMessage());
        }
    }

    private void requestPasswordReset()
    {
        try
        {
            String token = authentication.createPasswordResetToken(view.getResetEmail());
            if (token == null)
            {
                view.setMessage("If that email is registered, a reset token has been created.");
                return;
            }
            view.setMessage("Demo token (normally emailed to you): " + token);
        }
        catch (IllegalArgumentException | IllegalStateException exception)
        {
            view.setMessage(exception.getMessage());
        }
    }

    private void resetPassword()
    {
        try
        {
            boolean changed = authentication.resetPassword(
                    view.getResetEmail(), view.getResetToken(), view.getNewPassword());
            view.setMessage(changed
                    ? "Password reset. You can now sign in."
                    : "Reset token is invalid or has already been used.");
        }
        catch (IllegalArgumentException | IllegalStateException exception)
        {
            view.setMessage(exception.getMessage());
        }
    }

    private void showAccounts()
    {
        try
        {
            AccountView accountView = new AccountView(
                    authentication, () -> new LoginController(stage, authentication));
            stage.setScene(new Scene(accountView.getRoot()));
        }
        catch (IOException exception)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Could not load banking dashboard");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }
}
