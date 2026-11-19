import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class BankingSystemIntegrationTest
{
    private String originalUserHome;

    @AfterEach
    void restoreUserHome()
    {
        if (originalUserHome != null)
        {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    void fullBankingFlowWorksAcrossServiceLayerAndDatabase()
    {
        originalUserHome = System.getProperty("user.home");
        Path tempHome = null;
        try
        {
            tempHome = Files.createTempDirectory("banking-system-test-");
            System.setProperty("user.home", tempHome.toString());
        }
        catch (Exception exception)
        {
            throw new RuntimeException("Could not create a temporary home directory for the test DB.", exception);
        }

        AuthenticationService authentication = new PasswordAuthentication();
        authentication.register(
                "Thabo",
                "Mokoena",
                "NID-2026-101",
                29,
                "34 Main Road, Gaborone",
                "74000101",
                "tmokoena",
                "thabo@example.com",
                "P@ssword123");

        assertTrue(authentication.authenticate("tmokoena", "P@ssword123"));

        AccountOpeningService accountOpeningService = new AccountOpeningService();
        Account savingsAccount = accountOpeningService.openAccount(
                authentication.getAuthenticatedCustomerId(),
                "SAVINGS",
                2500,
                null,
                null,
                null);

        Account investmentAccount = accountOpeningService.openAccount(
                authentication.getAuthenticatedCustomerId(),
                "INVESTMENT",
                1400,
                null,
                null,
                null);

        Account chequeAccount = accountOpeningService.openAccount(
                authentication.getAuthenticatedCustomerId(),
                "CHEQUE",
                1000,
                "Example Employer",
                "1 Main Road",
                "Accountant");

        AccountDAO accountDAO = new AccountDaoImpl();
        List<Account> customerAccounts = accountDAO.getAllAccountsByCustomerId(
                authentication.getAuthenticatedCustomerId());
        assertEquals(4, customerAccounts.size());
        assertTrue(customerAccounts.stream()
                .anyMatch(account -> account.getAccountNumber().equals(chequeAccount.getAccountNumber())));

        savingsAccount.deposit(750);
        assertTrue(accountDAO.update(savingsAccount));

        Account targetAccount = accountDAO.findById(investmentAccount.getAccountNumber());
        assertNotNull(targetAccount);

        TransferTransaction transfer = new TransferTransaction(
                "TRX-INTEGRATION-1",
                chequeAccount,
                300,
                LocalDateTime.now().withNano(0).toString(),
                chequeAccount,
                targetAccount);
        transfer.execute();
        assertTrue(accountDAO.update(chequeAccount));
        assertTrue(accountDAO.update(targetAccount));

        TransactionDAO transactionDAO = new TransactionDaoImpl();
        transactionDAO.save(transfer);
        assertNotNull(transactionDAO.findById("TRX-INTEGRATION-1"));
    }
}
