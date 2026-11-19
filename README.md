# Banking System Program

This JavaFX banking system implements the OOAD assignment requirements for a multi-account banking application.

## Covered assignment areas

- Core domain model: `Account`, `Customer`, `SavingsAccount`, `InvestmentAccount`, `ChequeAccount`, `Transaction`
- Object-oriented design: inheritance, abstraction, interfaces, method overriding, and encapsulation
- GUI boundary classes: `LoginView`, `AccountView`
- Controller layer: `LoginController`, `AccountController`
- Persistence layer: `AccountDaoImpl`, `CustomerDaoImpl`, `TransactionDaoImpl`, `DatabaseConnection`
- Authentication and account opening: `PasswordAuthentication`, `AccountOpeningService`
- Integration flow testing: `BankingSystemIntegrationTest`

## Example transaction flow

1. Register a customer with a username and password.
2. Authenticate the customer.
3. Open one or more accounts.
4. Deposit, withdraw, transfer funds, and apply interest.
5. Persist the records in the SQLite database.

Cheque accounts can be opened by providing employer and occupation details; no document upload is required. Transfers are made by selecting one of your other accounts from the destination list.

## Sample database records

The project includes `sample-data.sql` with representative customer, account, and transaction records for demonstration and testing.

## Validation

Run the project checks with Maven:

```bash
./mvnw test
```
