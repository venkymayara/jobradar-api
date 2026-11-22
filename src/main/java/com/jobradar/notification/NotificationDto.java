package com.jobradar.notification;

import java.time.Instant;

public record NotificationDto(Long id, String title, String message, NotificationType type, String referenceType, Integer referenceId, boolean read, Instant createdAt, Instant readAt) {
    public static NotificationDto from(NotificationEntity n) { return new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.getReferenceType(), n.getReferenceId(), n.isRead(), n.getCreatedAt(), n.getReadAt()); }
}
