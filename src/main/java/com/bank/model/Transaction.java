package com.bank.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {
    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER, INTEREST, FEE }

    private String transactionId;
    private Type type;
    private double amount;
    private String description;
    private double balanceAfter;
    private LocalDateTime timestamp;

    public Transaction(Type type, double amount, String description, double balanceAfter) {
        this.transactionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.balanceAfter = balanceAfter;
        this.timestamp = LocalDateTime.now();
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionRef(String ref) { this.transactionId = ref; }
    public Type getType() { return type; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getBalanceAfter() { return balanceAfter; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public boolean isCredit() {
        return type == Type.DEPOSIT || type == Type.INTEREST;
    }
}
