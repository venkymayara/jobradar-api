package com.jobradar.saved;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SavedOpportunityId implements Serializable {
    @Column(name = "user_id")
    private Integer userId;
    @Column(name = "opportunity_id")
    private Integer opportunityId;

    public SavedOpportunityId() {}

    public SavedOpportunityId(Integer userId, Integer opportunityId) {
        this.userId = userId;
        this.opportunityId = opportunityId;
    }

    public Integer getUserId() { return userId; }
    public Integer getOpportunityId() { return opportunityId; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SavedOpportunityId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(opportunityId, that.opportunityId);
    }

    @Override public int hashCode() { return Objects.hash(userId, opportunityId); }
}
