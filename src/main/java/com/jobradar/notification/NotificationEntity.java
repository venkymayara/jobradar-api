package com.jobradar.notification;

import com.jobradar.auth.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @Column(nullable = false, length = 180, columnDefinition = "nvarchar(180)") private String title;
    @Column(nullable = false, length = 1000, columnDefinition = "nvarchar(1000)") private String message;
    @Enumerated(EnumType.STRING) @Column(name = "notification_type", nullable = false, length = 40) private NotificationType type;
    @Column(name = "reference_type", length = 40) private String referenceType;
    @Column(name = "reference_id") private Integer referenceId;
    @Column(name = "is_read", nullable = false) private boolean read;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "read_at") private Instant readAt;
    protected NotificationEntity() {}
    public NotificationEntity(UserEntity user, String title, String message, NotificationType type, String referenceType, Integer referenceId) {
        this.user = user; this.title = title; this.message = message; this.type = type; this.referenceType = referenceType; this.referenceId = referenceId; this.createdAt = Instant.now();
    }
    public Long getId() { return id; } public String getTitle() { return title; } public String getMessage() { return message; }
    public NotificationType getType() { return type; } public String getReferenceType() { return referenceType; } public Integer getReferenceId() { return referenceId; }
    public boolean isRead() { return read; } public Instant getCreatedAt() { return createdAt; } public Instant getReadAt() { return readAt; }
    public void markRead() { read = true; readAt = Instant.now(); }
}
