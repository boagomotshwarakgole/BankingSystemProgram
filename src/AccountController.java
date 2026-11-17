import java.time.LocalDateTime;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class AccountController
{
    @FXML private Label welcomeLabel;
    @FXML private Label accountNumberLabel;
    @FXML private Label balanceLabel;
    @FXML private Label incomeLabel;
    @FXML private Label expenseLabel;
    @FXML private ComboBox<Account> accountComboBox;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField descriptionField;
    @FXML private TextField amountField;
    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, String> colDescription;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colAmount;

    private final AuthenticationService authentication;
    private final Runnable logoutAction;
    private final AccountDAO accountDAO = new AccountDaoImpl();
    private final TransactionDAO transactionDAO = new TransactionDaoImpl();
    private final AccountOpeningService accountOpeningService = new AccountOpeningService();

    public AccountController(AuthenticationService authentication, Runnable logoutAction)
    {
        this.authentication = authentication;
        this.logoutAction = logoutAction;
    }

    @FXML
    private void initialize()
    {
        welcomeLabel.setText("Account Overview - " + authentication.getAuthenticatedCustomerId());
        typeComboBox.setItems(FXCollections.observableArrayList(
                "DEPOSIT", "WITHDRAWAL", "TRANSFER", "INTEREST"));

        accountComboBox.setCellFactory(list -> accountCell());
        accountComboBox.setButtonCell(accountCell());
        accountComboBox.valueProperty().addListener(
                (observable, oldAccount, newAccount) -> showSelectedAccount());

        colDate.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getTimestamp()));
        colDescription.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(describe(cell.getValue())));
        colType.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getClass().getSimpleName().replace("Transaction", "")));
        colAmount.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(formatMoney(cell.getValue().getAmount())));

        loadAccounts();
    }

    @FXML
    private void handleTransaction()
    {
        Account account = accountComboBox.getValue();
        if (account == null)
        {
            showError("This customer has no account selected.");
            return;
        }

        String type = typeComboBox.getValue();
        if (type == null)
        {
            showError("Select a transaction type.");
            return;
        }

        double amount;
        try
        {
            amount = Double.parseDouble(amountField.getText().trim());
        }
        catch (NumberFormatException exception)
        {
            showError("Enter a valid amount.");
            return;
        }
        if (!Double.isFinite(amount) || amount <= 0)
        {
            showError("Amount must be greater than zero.");
            return;
        }

        String transactionId = "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String timestamp = LocalDateTime.now().withNano(0).toString();

        try
        {
            Transaction transaction;
            Account destination = null;
            if ("DEPOSIT".equals(type))
            {
                transaction = new DepositTransaction(transactionId, account, amount, timestamp, account);
            }
            else if ("WITHDRAWAL".equals(type))
            {
                transaction = new WithdrawalTransaction(transactionId, account, amount, timestamp);
            }
            else if ("TRANSFER".equals(type))
            {
                String destinationNumber = descriptionField.getText().trim();
                if (destinationNumber.isEmpty())
                {
                    showError("Enter the destination account number in the reference field.");
                    return;
                }
                destination = accountDAO.findById(destinationNumber);
                if (destination == null)
                {
                    showError("Destination account was not found.");
                    return;
                }
                transaction = new TransferTransaction(
                        transactionId, account, amount, timestamp, account, destination);
            }
            else
            {
                if (!(account instanceof InterestBearing))
                {
                    showError("This account does not earn interest.");
                    return;
                }
                amount = ((InterestBearing) account).calculateInterest();
                if (amount <= 0)
                {
                    showError("There is no interest to apply.");
                    return;
                }
                transaction = new InterestTransaction(transactionId, account, amount, timestamp);
            }

            transaction.execute();
            if (!accountDAO.update(account))
            {
                throw new IllegalStateException("The account balance was not updated.");
            }
            if (destination != null && !accountDAO.update(destination))
            {
                throw new IllegalStateException("The destination account balance was not updated.");
            }
            transactionDAO.save(transaction);
            amountField.clear();
            descriptionField.clear();
            loadAccounts();
        }
        catch (IllegalArgumentException | IllegalStateException exception)
        {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleClearForm()
    {
        typeComboBox.getSelectionModel().clearSelection();
        descriptionField.clear();
        amountField.clear();
    }

    @FXML
    private void handleOpenAccount()
    {
        ComboBox<String> accountType = new ComboBox<>(
                FXCollections.observableArrayList("SAVINGS", "INVESTMENT", "CHEQUE"));
        accountType.setPromptText("Select account type");
        accountType.getSelectionModel().select("SAVINGS");

        TextField openingDeposit = new TextField("0");
        openingDeposit.setPromptText("Opening deposit (BWP)");
        TextField employerName = new TextField();
        employerName.setPromptText("Employer name");
        TextField employerAddress = new TextField();
        employerAddress.setPromptText("Employer address");
        TextField occupation = new TextField();
        occupation.setPromptText("Occupation");
        Label proofName = new Label("No proof selected");
        File[] selectedProof = new File[1];
        Button chooseProof = new Button("Choose proof of employment");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.addRow(0, new Label("Account type"), accountType);
        form.addRow(1, new Label("Opening deposit (BWP)"), openingDeposit);
        Label requirement = new Label(
                "Investment accounts require at least BWP 500. Cheque accounts require employment details and proof.");
        requirement.setWrapText(true);
        form.add(requirement, 0, 2, 2, 1);
        Label employerNameLabel = new Label("Employer name");
        Label employerAddressLabel = new Label("Employer address");
        Label occupationLabel = new Label("Occupation");
        Label proofLabel = new Label("Employment proof");
        HBox proofSelection = new HBox(8, chooseProof, proofName);
        form.addRow(3, employerNameLabel, employerName);
        form.addRow(4, employerAddressLabel, employerAddress);
        form.addRow(5, occupationLabel, occupation);
        form.addRow(6, proofLabel, proofSelection);

        Runnable updateEmploymentFields = () -> {
            boolean cheque = "CHEQUE".equals(accountType.getValue());
            for (javafx.scene.Node node : List.of(
                    employerNameLabel, employerName,
                    employerAddressLabel, employerAddress,
                    occupationLabel, occupation,
                    proofLabel, proofSelection))
            {
                node.setVisible(cheque);
                node.setManaged(cheque);
            }
        };
        accountType.valueProperty().addListener((observable, oldType, newType) -> updateEmploymentFields.run());
        updateEmploymentFields.run();

        chooseProof.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select proof of employment");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Employment proof (PDF, PNG, JPEG)", "*.pdf", "*.png", "*.jpg", "*.jpeg"));
            Window window = chooseProof.getScene() == null ? null : chooseProof.getScene().getWindow();
            File file = chooser.showOpenDialog(window);
            if (file != null)
            {
                selectedProof[0] = file;
                proofName.setText(file.getName());
            }
        });

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Open an account");
        dialog.setHeaderText("Add another account to your customer profile");
        dialog.getDialogPane().setContent(form);
        ButtonType openButton = new ButtonType("Open account", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(openButton, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != openButton)
        {
            return;
        }

        double deposit;
        try
        {
            deposit = Double.parseDouble(openingDeposit.getText().trim());
        }
        catch (NumberFormatException exception)
        {
            showError("Enter a valid opening deposit.");
            return;
        }

        byte[] proofData = null;
        String proofFileName = null;
        if ("CHEQUE".equals(accountType.getValue()))
        {
            if (selectedProof[0] == null)
            {
                showError("Upload proof of employment to open a cheque account.");
                return;
            }
            try
            {
                if (Files.size(selectedProof[0].toPath()) > 5 * 1024 * 1024)
                {
                    showError("Employment proof must be 5 MB or smaller.");
                    return;
                }
                proofData = Files.readAllBytes(selectedProof[0].toPath());
                proofFileName = selectedProof[0].getName();
            }
            catch (IOException exception)
            {
                showError("Could not read the selected employment proof: " + exception.getMessage());
                return;
            }
        }

        try
        {
            Account opened = accountOpeningService.openAccount(
                    authentication.getAuthenticatedCustomerId(),
                    accountType.getValue(),
                    deposit,
                    employerName.getText(),
                    employerAddress.getText(),
                    occupation.getText(),
                    proofFileName,
                    proofData);
            loadAccounts();
            accountComboBox.getItems().stream()
                    .filter(account -> account.getAccountNumber().equals(opened.getAccountNumber()))
                    .findFirst()
                    .ifPresent(account -> accountComboBox.getSelectionModel().select(account));
            showInformation("Account opened", opened.getAccountType() + " account "
                    + opened.getAccountNumber() + " opened successfully.");
        }
        catch (IllegalArgumentException | IllegalStateException exception)
        {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleLogout()
    {
        authentication.logout();
        logoutAction.run();
    }

    private void loadAccounts()
    {
        try
        {
            List<Account> accounts =
                    accountDAO.getAllAccountsByCustomerId(authentication.getAuthenticatedCustomerId());
            accountComboBox.setItems(FXCollections.observableArrayList(accounts));
            if (!accounts.isEmpty())
            {
                accountComboBox.getSelectionModel().selectFirst();
            }
            else
            {
                accountNumberLabel.setText("No accounts found.");
                balanceLabel.setText(formatMoney(0));
                incomeLabel.setText(formatMoney(0));
                expenseLabel.setText(formatMoney(0));
                transactionTable.getItems().clear();
            }
        }
        catch (IllegalStateException exception)
        {
            showError(exception.getMessage());
        }
    }

    private void showSelectedAccount()
    {
        Account selected = accountComboBox.getValue();
        if (selected == null)
        {
            return;
        }

        accountNumberLabel.setText(selected.getAccountType() + ": " + selected.getAccountNumber());
        double totalBalance = accountComboBox.getItems().stream()
                .mapToDouble(Account::getBalance)
                .sum();
        balanceLabel.setText(formatMoney(totalBalance));

        try
        {
            List<Transaction> transactions =
                    transactionDAO.getAllTransactionsByAccountId(selected.getAccountNumber());
            transactionTable.setItems(FXCollections.observableArrayList(transactions));

            double inflow = 0;
            double outflow = 0;
            for (Transaction transaction : transactions)
            {
                if (transaction instanceof TransferTransaction)
                {
                    TransferTransaction transfer = (TransferTransaction) transaction;
                    if (transfer.getAccount().getAccountNumber().equals(selected.getAccountNumber()))
                    {
                        outflow += transfer.getAmount();
                    }
                    else
                    {
                        inflow += transfer.getAmount();
                    }
                }
                else if (transaction instanceof WithdrawalTransaction)
                {
                    outflow += transaction.getAmount();
                }
                else
                {
                    inflow += transaction.getAmount();
                }
            }
            incomeLabel.setText(formatMoney(inflow));
            expenseLabel.setText(formatMoney(outflow));
        }
        catch (IllegalStateException exception)
        {
            showError(exception.getMessage());
        }
    }

    private ListCell<Account> accountCell()
    {
        return new ListCell<>()
        {
            @Override
            protected void updateItem(Account account, boolean empty)
            {
                super.updateItem(account, empty);
                setText(empty || account == null ? null
                        : account.getAccountNumber() + " (" + account.getAccountType() + ")");
            }
        };
    }

    private String describe(Transaction transaction)
    {
        if (transaction instanceof TransferTransaction)
        {
            TransferTransaction transfer = (TransferTransaction) transaction;
            Account selected = accountComboBox.getValue();
            if (selected != null
                    && transfer.getDestinationAccount().getAccountNumber().equals(selected.getAccountNumber()))
            {
                return "Transfer from " + transfer.getAccount().getAccountNumber();
            }
            return "Transfer to " + transfer.getDestinationAccount().getAccountNumber();
        }
        return transaction.getTransactionID();
    }

    private String formatMoney(double amount)
    {
        return String.format(Locale.US, "BWP %,.2f", amount);
    }

    private void showError(String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Banking System");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInformation(String header, String message)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
