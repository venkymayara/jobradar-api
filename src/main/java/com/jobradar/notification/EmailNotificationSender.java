package com.jobradar.notification;

public interface EmailNotificationSender {
    void send(String recipient, String subject, String message);
}
