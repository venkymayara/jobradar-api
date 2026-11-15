package com.jobradar.opportunity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "opportunity_status_history")
public class OpportunityStatusHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "opportunity_id", nullable = false)
    private OpportunityEntity opportunity;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 20)
    private OpportunityStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 20)
    private OpportunityStatus newStatus;
    @Column(name = "changed_by", nullable = false, length = 100, columnDefinition = "nvarchar(100)")
    private String changedBy;
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected OpportunityStatusHistoryEntity() {}
    public OpportunityStatusHistoryEntity(OpportunityEntity opportunity, OpportunityStatus previousStatus,
            OpportunityStatus newStatus, String changedBy) {
        this.opportunity = opportunity; this.previousStatus = previousStatus;
        this.newStatus = newStatus; this.changedBy = changedBy; this.changedAt = Instant.now();
    }
    public OpportunityStatus getPreviousStatus() { return previousStatus; }
    public OpportunityStatus getNewStatus() { return newStatus; }
    public String getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
