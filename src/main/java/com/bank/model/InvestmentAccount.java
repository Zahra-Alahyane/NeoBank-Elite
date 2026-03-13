package com.bank.model;

public class InvestmentAccount extends BankAccount {
    private double riskLevel; // 1-10
    private double portfolioValue;
    private static final double BASE_INTEREST_RATE = 0.07; // 7%

    public InvestmentAccount(Client owner, double initialDeposit, double riskLevel) {
        super(owner, initialDeposit);
        this.riskLevel = Math.min(10, Math.max(1, riskLevel));
        this.interestRate = BASE_INTEREST_RATE + (riskLevel - 5) * 0.01;
        this.portfolioValue = initialDeposit;
    }

    @Override
    protected boolean canWithdraw(double amount) {
        return balance >= amount && balance - amount >= 1000; // minimum €1000
    }

    @Override
    public void calculateInterest() {
        // Simulate market volatility
        double volatility = (Math.random() - 0.45) * riskLevel * 0.02;
        double effectiveRate = interestRate + volatility;
        double gain = balance * effectiveRate / 12;
        balance += gain;
        portfolioValue = balance;
        String desc = gain >= 0 ?
            String.format("Portfolio gain (+%.2f%%)", effectiveRate * 100) :
            String.format("Portfolio loss (%.2f%%)", effectiveRate * 100);
        transactions.add(new Transaction(
            gain >= 0 ? Transaction.Type.INTEREST : Transaction.Type.FEE,
            Math.abs(gain), desc, balance));
    }

    @Override
    public String getAccountType() { return "Investment Account"; }

    @Override
    public String getAccountSummary() {
        return String.format("Investment Account [%s] | Portfolio: €%.2f | Risk: %.0f/10 | Base Return: %.1f%% | Owner: %s",
                accountNumber, portfolioValue, riskLevel, interestRate * 100, owner.getFullName());
    }

    public double getRiskLevel() { return riskLevel; }
    public double getPortfolioValue() { return portfolioValue; }
}
