package com.bank.model;

public class SavingsAccount extends BankAccount {
    private double withdrawalLimit;
    private int withdrawalsThisMonth;
    private static final double DEFAULT_INTEREST_RATE = 0.035; // 3.5%
    private static final int MAX_WITHDRAWALS_PER_MONTH = 6;

    public SavingsAccount(Client owner, double initialDeposit) {
        super(owner, initialDeposit);
        this.interestRate = DEFAULT_INTEREST_RATE;
        this.withdrawalLimit = 10000.0;
        this.withdrawalsThisMonth = 0;
    }

    @Override
    protected boolean canWithdraw(double amount) {
        if (withdrawalsThisMonth >= MAX_WITHDRAWALS_PER_MONTH) return false;
        if (amount > withdrawalLimit) return false;
        return balance >= amount;
    }

    @Override
    public boolean withdraw(double amount) {
        boolean success = super.withdraw(amount);
        if (success) withdrawalsThisMonth++;
        return success;
    }

    @Override
    public void calculateInterest() {
        double interest = balance * interestRate / 12;
        balance += interest;
        transactions.add(new Transaction(Transaction.Type.INTEREST, interest, "Monthly interest (3.5% APY)", balance));
        withdrawalsThisMonth = 0; // reset monthly
    }

    @Override
    public String getAccountType() { return "Savings Account"; }

    @Override
    public String getAccountSummary() {
        return String.format("Savings Account [%s] | Balance: €%.2f | APY: %.1f%% | Withdrawals: %d/%d | Owner: %s",
                accountNumber, balance, interestRate * 100, withdrawalsThisMonth, MAX_WITHDRAWALS_PER_MONTH, owner.getFullName());
    }

    public double getWithdrawalLimit() { return withdrawalLimit; }
    public int getWithdrawalsThisMonth() { return withdrawalsThisMonth; }
    public void setWithdrawalsThisMonth(int count) { this.withdrawalsThisMonth = count; }
    public int getMaxWithdrawalsPerMonth() { return MAX_WITHDRAWALS_PER_MONTH; }
}
