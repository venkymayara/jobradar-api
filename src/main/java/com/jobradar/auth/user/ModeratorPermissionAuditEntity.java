package com.jobradar.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "moderator_permission_history")
public class ModeratorPermissionAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    private UserEntity target;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private UserEntity changedBy;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "changed_role", nullable = false, length = 32)
    private String changedRole;
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ModeratorPermissionAuditEntity() {}

    public ModeratorPermissionAuditEntity(UserEntity target, UserEntity changedBy, boolean enabled) {
        this(target, changedBy, enabled, "ROLE_MODERATOR");
    }

    public ModeratorPermissionAuditEntity(UserEntity target, UserEntity changedBy, boolean enabled, String changedRole) {
        this.target = target;
        this.changedBy = changedBy;
        this.enabled = enabled;
        this.changedRole = changedRole;
        this.changedAt = Instant.now();
    }

    public UserEntity getTarget() { return target; }
    public UserEntity getChangedBy() { return changedBy; }
    public boolean isEnabled() { return enabled; }
    public Instant getChangedAt() { return changedAt; }
}
