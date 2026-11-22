package com.jobradar.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredEmailNotificationSender implements EmailNotificationSender {
    private static final Logger log = LoggerFactory.getLogger(ConfiguredEmailNotificationSender.class);
    private final boolean enabled;
    public ConfiguredEmailNotificationSender(@Value("${app.notifications.email-enabled:false}") boolean enabled) { this.enabled = enabled; }
    @Override public void send(String recipient, String subject, String message) {
        if (!enabled) return;
        log.info("Email notification delivery is enabled but no provider is configured; recipient={}, subject={}", recipient, subject);
    }
}
