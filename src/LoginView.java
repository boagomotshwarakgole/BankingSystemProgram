import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class LoginView extends VBox
{
    private final TextField loginUsername = new TextField();
    private final PasswordField loginPassword = new PasswordField();
    private final TextField registerFirstName = new TextField();
    private final TextField registerLastName = new TextField();
    private final TextField registerNationalId = new TextField();
    private final TextField registerAge = new TextField();
    private final TextField registerAddress = new TextField();
    private final TextField registerPhoneNumber = new TextField();
    private final TextField registerUsername = new TextField();
    private final TextField registerEmail = new TextField();
    private final PasswordField registerPassword = new PasswordField();
    private final TextField resetEmail = new TextField();
    private final TextField resetToken = new TextField();
    private final PasswordField resetPassword = new PasswordField();
    private final Label message = new Label();
    private Runnable loginAction = () -> { };
    private Runnable registerAction = () -> { };
    private Runnable requestResetAction = () -> { };
    private Runnable resetAction = () -> { };

    public LoginView()
    {
        setSpacing(14);
        setPadding(new Insets(24));
        setAlignment(Pos.CENTER);
        setPrefSize(620, 480);
        setStyle("-fx-background-color: #F4F1EA;");

        Label title = new Label("BANKING SYSTEM");
        title.setStyle("-fx-text-fill: #D4AF37; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label subtitle = new Label("Secure access to your personal and business accounts");
        subtitle.setStyle("-fx-text-fill: #A3B5AA; -fx-font-size: 12px;");
        VBox header = new VBox(5, title, subtitle);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color: #1A2F24; -fx-background-radius: 5;");

        TabPane tabs = new TabPane();
        tabs.getTabs().add(createLoginTab());
        tabs.getTabs().add(createRegistrationTab());
        tabs.getTabs().add(createResetTab());
        tabs.setStyle("-fx-background-color: white;");
        VBox.setVgrow(tabs, javafx.scene.layout.Priority.ALWAYS);

        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #1A2F24;");
        getChildren().addAll(header, tabs, message);
    }

    public void setLoginAction(Runnable action)
    {
        loginAction = action;
    }

    public void setRegisterAction(Runnable action)
    {
        registerAction = action;
    }

    public void setRequestResetAction(Runnable action)
    {
        requestResetAction = action;
    }

    public void setResetAction(Runnable action)
    {
        resetAction = action;
    }

    public String getLoginUsername()
    {
        return loginUsername.getText().trim();
    }

    public String getLoginPassword()
    {
        return loginPassword.getText();
    }

    public String getRegisterFirstName()
    {
        return registerFirstName.getText().trim();
    }

    public String getRegisterLastName()
    {
        return registerLastName.getText().trim();
    }

    public String getRegisterNationalId()
    {
        return registerNationalId.getText().trim();
    }

    public String getRegisterAge()
    {
        return registerAge.getText().trim();
    }

    public String getRegisterAddress()
    {
        return registerAddress.getText().trim();
    }

    public String getRegisterPhoneNumber()
    {
        return registerPhoneNumber.getText().trim();
    }

    public String getRegisterUsername()
    {
        return registerUsername.getText().trim();
    }

    public String getRegisterEmail()
    {
        return registerEmail.getText().trim();
    }

    public String getRegisterPassword()
    {
        return registerPassword.getText();
    }

    public String getResetEmail()
    {
        return resetEmail.getText().trim();
    }

    public String getResetToken()
    {
        return resetToken.getText().trim();
    }

    public String getNewPassword()
    {
        return resetPassword.getText();
    }

    public void setMessage(String text)
    {
        message.setText(text);
    }

    private Tab createLoginTab()
    {
        GridPane form = createForm();
        addRow(form, 0, "Username", loginUsername);
        addRow(form, 1, "Password", loginPassword);
        Button button = new Button("Sign in");
        stylePrimaryButton(button);
        button.setOnAction(event -> loginAction.run());
        form.add(button, 1, 2);
        return new Tab("Login", form);
    }

    private Tab createRegistrationTab()
    {
        GridPane form = createForm();
        Label registrationNote = new Label("Create a new customer profile. No existing customer ID is needed.");
        registrationNote.setWrapText(true);
        registrationNote.setStyle("-fx-text-fill: #5C6661;");
        form.add(registrationNote, 0, 0, 2, 1);
        addRow(form, 1, "First name", registerFirstName);
        addRow(form, 2, "Last name", registerLastName);
        addRow(form, 3, "National ID", registerNationalId);
        addRow(form, 4, "Age", registerAge);
        addRow(form, 5, "Address", registerAddress);
        addRow(form, 6, "Phone number", registerPhoneNumber);
        addRow(form, 7, "Username", registerUsername);
        addRow(form, 8, "Email", registerEmail);
        addRow(form, 9, "Password (8+ characters)", registerPassword);
        Button button = new Button("Register");
        stylePrimaryButton(button);
        button.setOnAction(event -> registerAction.run());
        form.add(button, 1, 10);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white;");
        Tab tab = new Tab("Register", scrollPane);
        tab.setClosable(false);
        return tab;
    }

    private Tab createResetTab()
    {
        GridPane form = createForm();
        addRow(form, 0, "Email", resetEmail);
        Button requestButton = new Button("Create reset token");
        stylePrimaryButton(requestButton);
        requestButton.setOnAction(event -> requestResetAction.run());
        form.add(requestButton, 1, 1);
        addRow(form, 2, "Reset token", resetToken);
        addRow(form, 3, "New password", resetPassword);
        Button resetButton = new Button("Reset password");
        stylePrimaryButton(resetButton);
        resetButton.setOnAction(event -> resetAction.run());
        form.add(resetButton, 1, 4);
        return new Tab("Forgot password", form);
    }

    private GridPane createForm()
    {
        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(12));
        return form;
    }

    private void addRow(GridPane form, int row, String label, javafx.scene.control.Control input)
    {
        form.add(new Label(label), 0, row);
        input.setPrefWidth(260);
        form.add(input, 1, row);
    }

    private void stylePrimaryButton(Button button)
    {
        button.setStyle(
                "-fx-background-color: #1A2F24; -fx-text-fill: white; "
                        + "-fx-font-weight: bold; -fx-background-radius: 4; "
                        + "-fx-padding: 9 16 9 16;");
    }
}
