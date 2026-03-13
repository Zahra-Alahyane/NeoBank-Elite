package com.bank.model;

public interface Bankable {
    void deposit(double amount);
    boolean withdraw(double amount);
    double getBalance();
    void calculateInterest();
    String getAccountSummary();
}
