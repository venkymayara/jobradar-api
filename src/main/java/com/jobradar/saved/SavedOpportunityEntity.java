package com.jobradar.saved;

import com.jobradar.auth.user.UserEntity;
import com.jobradar.opportunity.OpportunityEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "\"SavedOpportunities\"")
public class SavedOpportunityEntity {
    @EmbeddedId
    private SavedOpportunityId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("opportunityId")
    @JoinColumn(name = "opportunity_id", nullable = false)
    private OpportunityEntity opportunity;

    @CreationTimestamp
    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;

    protected SavedOpportunityEntity() {}

    public SavedOpportunityEntity(UserEntity user, OpportunityEntity opportunity) {
        this.id = new SavedOpportunityId(user.getId(), opportunity.getId());
        this.user = user;
        this.opportunity = opportunity;
    }

    public SavedOpportunityId getId() { return id; }
    public UserEntity getUser() { return user; }
    public OpportunityEntity getOpportunity() { return opportunity; }
    public Instant getSavedAt() { return savedAt; }
}
