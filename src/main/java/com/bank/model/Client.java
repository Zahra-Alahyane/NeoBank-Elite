package com.bank.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Client {
    private String clientId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private List<BankAccount> accounts;
    private ClientTier tier;
    private LocalDate memberSince;

    public enum ClientTier { STANDARD, SILVER, GOLD, PLATINUM }

    public Client(String firstName, String lastName, String email, String phone,
                  LocalDate dateOfBirth, String address) {
        this.clientId = UUID.randomUUID().toString();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.accounts = new ArrayList<>();
        this.tier = ClientTier.STANDARD;
        this.memberSince = LocalDate.now();
    }

    public void addAccount(BankAccount account) {
        accounts.add(account);
        updateTier();
    }

    public double getTotalBalance() {
        return accounts.stream().mapToDouble(BankAccount::getBalance).sum();
    }

    private void updateTier() {
        double total = getTotalBalance();
        if (total >= 100000) tier = ClientTier.PLATINUM;
        else if (total >= 50000) tier = ClientTier.GOLD;
        else if (total >= 10000) tier = ClientTier.SILVER;
        else tier = ClientTier.STANDARD;
    }

    public String getFullName() { return firstName + " " + lastName; }

    // Getters & Setters
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public List<BankAccount> getAccounts() { return accounts; }
    public ClientTier getTier() { updateTier(); return tier; }
    public void setTierFromString(String tier) { this.tier = ClientTier.valueOf(tier); }
    public LocalDate getMemberSince() { return memberSince; }
}
