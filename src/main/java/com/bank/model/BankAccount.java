package com.bank.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class BankAccount implements Bankable, Notifiable {
    protected String accountId;
    protected String accountNumber;
    protected double balance;
    protected double interestRate;
    protected Client owner;
    protected List<Transaction> transactions;
    protected boolean notificationsEnabled;
    protected LocalDateTime createdAt;
    protected AccountStatus status;

    public enum AccountStatus { ACTIVE, FROZEN, CLOSED }

    public BankAccount(Client owner, double initialDeposit) {
        this.accountId = UUID.randomUUID().toString();
        this.accountNumber = generateAccountNumber();
        this.balance = initialDeposit;
        this.owner = owner;
        this.transactions = new ArrayList<>();
        this.notificationsEnabled = true;
        this.createdAt = LocalDateTime.now();
        this.status = AccountStatus.ACTIVE;

        if (initialDeposit > 0) {
            transactions.add(new Transaction(Transaction.Type.DEPOSIT, initialDeposit, "Initial deposit", balance));
        }
    }

    private String generateAccountNumber() {
        return "FR" + String.format("%016d", (long)(Math.random() * 1_000_000_000_0000_00L));
    }

    @Override
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
        if (status != AccountStatus.ACTIVE) throw new IllegalStateException("Account is not active");
        balance += amount;
        transactions.add(new Transaction(Transaction.Type.DEPOSIT, amount, "Deposit", balance));
        if (notificationsEnabled) sendNotification("Deposit of €" + String.format("%.2f", amount) + " received.");
    }

    @Override
    public boolean withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive");
        if (status != AccountStatus.ACTIVE) throw new IllegalStateException("Account is not active");
        if (!canWithdraw(amount)) return false;
        balance -= amount;
        transactions.add(new Transaction(Transaction.Type.WITHDRAWAL, amount, "Withdrawal", balance));
        if (notificationsEnabled) sendNotification("Withdrawal of €" + String.format("%.2f", amount) + " processed.");
        return true;
    }

    protected abstract boolean canWithdraw(double amount);

    public abstract String getAccountType();

    @Override
    public double getBalance() { return balance; }

    @Override
    public void sendNotification(String message) {
        if (notificationsEnabled) {
            System.out.println("[NOTIF] " + owner.getFullName() + ": " + message);
        }
    }

    @Override
    public void enableNotifications(boolean enabled) { this.notificationsEnabled = enabled; }

    @Override
    public boolean isNotificationsEnabled() { return notificationsEnabled; }

    // Getters & Setters
    public String getAccountId() { return accountId; }
    public void setAccountId(String id) { this.accountId = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public double getInterestRate() { return interestRate; }
    public Client getOwner() { return owner; }
    public List<Transaction> getTransactions() { return transactions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setNotificationsEnabled(boolean enabled) { this.notificationsEnabled = enabled; }

    public boolean transfer(BankAccount target, double amount) {
        if (withdraw(amount)) {
            target.deposit(amount);
            transactions.get(transactions.size()-1).setDescription("Transfer to " + target.getAccountNumber());
            return true;
        }
        return false;
    }
}
