package com.bank.model;

public interface Notifiable {
    void sendNotification(String message);
    void enableNotifications(boolean enabled);
    boolean isNotificationsEnabled();
}
