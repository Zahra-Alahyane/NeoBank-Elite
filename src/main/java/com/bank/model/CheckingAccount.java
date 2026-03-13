package com.bank.model;

public class CheckingAccount extends BankAccount {
    private double overdraftLimit;
    private static final double DEFAULT_INTEREST_RATE = 0.001; // 0.1%

    public CheckingAccount(Client owner, double initialDeposit, double overdraftLimit) {
        super(owner, initialDeposit);
        this.overdraftLimit = overdraftLimit;
        this.interestRate = DEFAULT_INTEREST_RATE;
    }

    public CheckingAccount(Client owner, double initialDeposit) {
        this(owner, initialDeposit, 500.0);
    }

    @Override
    protected boolean canWithdraw(double amount) {
        return (balance - amount) >= -overdraftLimit;
    }

    @Override
    public void calculateInterest() {
        if (balance > 0) {
            double interest = balance * interestRate;
            balance += interest;
            transactions.add(new Transaction(Transaction.Type.INTEREST, interest, "Monthly interest", balance));
        }
    }

    @Override
    public String getAccountType() { return "Checking Account"; }

    @Override
    public String getAccountSummary() {
        return String.format("Checking Account [%s] | Balance: €%.2f | Overdraft: €%.2f | Owner: %s",
                accountNumber, balance, overdraftLimit, owner.getFullName());
    }

    public double getOverdraftLimit() { return overdraftLimit; }
    public void setOverdraftLimit(double overdraftLimit) { this.overdraftLimit = overdraftLimit; }
}
