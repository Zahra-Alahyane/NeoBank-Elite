package com.bank.model;

public class AppUser {
    private String id;
    private String username;
    private String passwordHash;
    private String role; // ADMIN or CLIENT
    private String clientId;
    private String fullName;

    public AppUser(String id, String username, String passwordHash, String role, String clientId, String fullName) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.clientId = clientId;
        this.fullName = fullName;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getClientId() { return clientId; }
    public String getFullName() { return fullName; }
}
