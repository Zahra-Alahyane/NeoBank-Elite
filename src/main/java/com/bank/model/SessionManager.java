package com.bank.model;

public class SessionManager {
    private static AppUser currentUser;

    public static void setCurrentUser(AppUser user) { currentUser = user; }
    public static AppUser getCurrentUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }
    public static boolean isAdmin() { return currentUser != null && "ADMIN".equals(currentUser.getRole()); }
    public static void logout() { currentUser = null; }
}
