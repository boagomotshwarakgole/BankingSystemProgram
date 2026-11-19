import java.time.LocalDateTime;
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
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

public class AccountController
{
    @FXML private Label welcomeLabel;
    @FXML private Label accountNumberLabel;
    @FXML private Label balanceLabel;
    @FXML private Label incomeLabel;
    @FXML private Label expenseLabel;
    @FXML private ComboBox<Account> accountComboBox;
    @FXML private ComboBox<Account> destinationAccountComboBox;
    @FXML private ComboBox<String> typeComboBox;
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
        destinationAccountComboBox.setCellFactory(list -> accountCell());
        destinationAccountComboBox.setButtonCell(accountCell());
        accountComboBox.valueProperty().addListener((observable, oldAccount, newAccount) -> {
            showSelectedAccount();
            refreshDestinationAccounts();
        });
        typeComboBox.valueProperty().addListener(
                (observable, oldType, newType) -> updateDestinationVisibility());
        updateDestinationVisibility();

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

        try
        {
            Transaction transaction = createTransaction(type, account, amount);
            Account destination = transaction instanceof TransferTransaction
                    ? ((TransferTransaction) transaction).getDestinationAccount()
                    : null;

            if (!accountDAO.processTransaction(transaction, destination))
            {
                throw new IllegalStateException("The transaction was not completed.");
            }
            amountField.clear();
            destinationAccountComboBox.getSelectionModel().clearSelection();
            loadAccounts();
            showInformation(
                    "Transaction successful",
                    type + " of " + formatMoney(transaction.getAmount()) + " was completed.");
        }
        catch (IllegalArgumentException | IllegalStateException exception)
        {
            showError(exception.getMessage());
        }
    }

    private Transaction createTransaction(String type, Account account, double amount)
    {
        String transactionId = "TRX-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String timestamp = LocalDateTime.now().withNano(0).toString();

        if ("DEPOSIT".equals(type))
        {
            return new DepositTransaction(transactionId, account, amount, timestamp, account);
        }
        if ("WITHDRAWAL".equals(type))
        {
            return new WithdrawalTransaction(transactionId, account, amount, timestamp);
        }
        if ("TRANSFER".equals(type))
        {
            Account destination = destinationAccountComboBox.getValue();
            if (destination == null)
            {
                throw new IllegalArgumentException("Select a destination account.");
            }
            if (!authentication.getAuthenticatedCustomerId()
                    .equals(destination.getCustomer().getCustomerID()))
            {
                throw new IllegalArgumentException("Transfers are only allowed to accounts that you own.");
            }
            return new TransferTransaction(
                    transactionId, account, amount, timestamp, account, destination);
        }

        if (!(account instanceof InterestBearing))
        {
            throw new IllegalArgumentException("This account does not earn interest.");
        }
        double interest = ((InterestBearing) account).calculateInterest();
        if (interest <= 0)
        {
            throw new IllegalArgumentException("There is no interest to apply.");
        }
        return new InterestTransaction(transactionId, account, interest, timestamp);
    }

    @FXML
    private void handleClearForm()
    {
        typeComboBox.getSelectionModel().clearSelection();
        destinationAccountComboBox.getSelectionModel().clearSelection();
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

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.addRow(0, new Label("Account type"), accountType);
        form.addRow(1, new Label("Opening deposit (BWP)"), openingDeposit);
        Label requirement = new Label(
                "Investment accounts require at least BWP 500. Cheque accounts require employer and occupation details.");
        requirement.setWrapText(true);
        form.add(requirement, 0, 2, 2, 1);
        Label employerNameLabel = new Label("Employer name");
        Label employerAddressLabel = new Label("Employer address");
        Label occupationLabel = new Label("Occupation");
        form.addRow(3, employerNameLabel, employerName);
        form.addRow(4, employerAddressLabel, employerAddress);
        form.addRow(5, occupationLabel, occupation);

        Runnable updateEmploymentFields = () -> {
            boolean cheque = "CHEQUE".equals(accountType.getValue());
            for (javafx.scene.Node node : List.of(
                    employerNameLabel, employerName,
                    employerAddressLabel, employerAddress))
            {
                node.setVisible(cheque);
                node.setManaged(cheque);
            }
        };
        accountType.valueProperty().addListener((observable, oldType, newType) -> updateEmploymentFields.run());
        updateEmploymentFields.run();

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

        try
        {
            Account opened = accountOpeningService.openAccount(
                    authentication.getAuthenticatedCustomerId(),
                    accountType.getValue(),
                    deposit,
                    employerName.getText(),
                    employerAddress.getText(),
                    occupation.getText());
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
            refreshDestinationAccounts();
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
        balanceLabel.setText(formatMoney(selected.getBalance()));

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
                        : account.getAccountType() + " - "
                                + account.getAccountNumber().substring(
                                        Math.max(0, account.getAccountNumber().length() - 6)));
            }
        };
    }

    private void refreshDestinationAccounts()
    {
        List<Account> accounts = accountComboBox.getItems();
        Account source = accountComboBox.getValue();
        destinationAccountComboBox.setItems(FXCollections.observableArrayList(
                accounts.stream()
                        .filter(account -> source == null
                                || !account.getAccountNumber().equals(source.getAccountNumber()))
                        .toList()));
        destinationAccountComboBox.getSelectionModel().clearSelection();
    }

    private void updateDestinationVisibility()
    {
        boolean transfer = "TRANSFER".equals(typeComboBox.getValue());
        destinationAccountComboBox.setVisible(transfer);
        destinationAccountComboBox.setManaged(transfer);
        if (!transfer)
        {
            destinationAccountComboBox.getSelectionModel().clearSelection();
        }
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
