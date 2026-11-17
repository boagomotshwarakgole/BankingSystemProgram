-- Optional fictional sample data for the SQLite database.
-- The application creates the schema automatically on first launch.
BEGIN TRANSACTION;

INSERT OR IGNORE INTO Customers (customer_id, customer_type, address, phone_number)
VALUES
    ('CUST-1001', 'EMPLOYED', '12 Market Street', '0712345001'),
    ('CUST-1002', 'INDIVIDUAL', '8 River Road', '0712345002'),
    ('CUST-1003', 'CORPORATE', 'Plot 45, Commerce Park', '0712345003'),
    ('CUST-1004', 'INDIVIDUAL', '22 Highfield Avenue', '0712345004'),
    ('CUST-1005', 'EMPLOYED', '7 Stadium Road', '0712345005');

INSERT OR IGNORE INTO IndividualCustomers
    (customer_id, customer_type, first_name, last_name, national_id, age)
VALUES
    ('CUST-1001', 'EMPLOYED', 'Kago', 'Molefe', 'NID-1001', 32),
    ('CUST-1002', 'INDIVIDUAL', 'Masego', 'Dube', 'NID-1002', 28),
    ('CUST-1004', 'INDIVIDUAL', 'Lerato', 'Kgosi', 'NID-1004', 26),
    ('CUST-1005', 'EMPLOYED', 'Palesa', 'Ntshe', 'NID-1005', 35);

INSERT OR IGNORE INTO EmployedCustomers
    (customer_id, company_name, company_address, occupation)
VALUES
    ('CUST-1001', 'Northside Finance', '10 Main Mall', 'Accountant'),
    ('CUST-1005', 'Botswana Railways', '4 Station Road', 'Engineer');

INSERT OR IGNORE INTO CorporateCustomers
    (customer_id, company_name, tax_id, registration_number)
VALUES
    ('CUST-1003', 'Metsi Office Supplies', 'TAX-1003', 'REG-1003');

INSERT OR IGNORE INTO Accounts
    (account_number, customer_id, account_type, balance, date_opened, minimum_deposit)
VALUES
    ('ACC-2001', 'CUST-1001', 'CHEQUE', 2500.00, '2026-11-09', 0.00),
    ('ACC-2002', 'CUST-1002', 'SAVINGS', 800.00, '2026-11-09', 0.00),
    ('ACC-2003', 'CUST-1003', 'INVESTMENT', 900.00, '2026-11-09', 500.00),
    ('ACC-2004', 'CUST-1004', 'SAVINGS', 1200.50, '2026-11-09', 0.00),
    ('ACC-2005', 'CUST-1004', 'INVESTMENT', 1500.00, '2026-11-09', 500.00),
    ('ACC-2006', 'CUST-1005', 'CHEQUE', 1800.00, '2026-11-09', 0.00);

INSERT OR IGNORE INTO Transactions
    (transaction_id, transaction_type, account_number, destination_account_number, amount, transaction_time)
VALUES
    ('TRX-3001', 'DEPOSIT', 'ACC-2001', NULL, 250.00, '2026-11-09T09:00:00'),
    ('TRX-3002', 'INTEREST', 'ACC-2002', NULL, 0.40, '2026-11-09T09:05:00'),
    ('TRX-3003', 'DEPOSIT', 'ACC-2003', NULL, 500.00, '2026-11-09T09:10:00'),
    ('TRX-3004', 'WITHDRAWAL', 'ACC-2004', NULL, 100.00, '2026-11-09T10:00:00'),
    ('TRX-3005', 'TRANSFER', 'ACC-2001', 'ACC-2006', 200.00, '2026-11-09T10:15:00'),
    ('TRX-3006', 'DEPOSIT', 'ACC-2006', NULL, 350.00, '2026-11-09T10:20:00');

COMMIT;

-- Check the sample customers, accounts, and transaction records.
SELECT c.customer_id, c.customer_type, a.account_number, a.account_type, a.balance
FROM Customers AS c
LEFT JOIN Accounts AS a ON a.customer_id = c.customer_id
ORDER BY c.customer_id, a.account_number;

SELECT COUNT(*) AS CustomerCount FROM Customers;
SELECT COUNT(*) AS AccountCount FROM Accounts;
SELECT COUNT(*) AS TransactionCount FROM Transactions;
