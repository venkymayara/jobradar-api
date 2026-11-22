package com.jobradar.opportunity;

import com.jobradar.auth.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "OpportunityReports", schema = "dbo")
public class OpportunityReportEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "opportunity_id", nullable = false)
    private OpportunityEntity opportunity;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reported_by_user_id", nullable = false)
    private UserEntity reporter;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private OpportunityReportReason reason;
    @Column(length = 500, columnDefinition = "nvarchar(500)")
    private String details;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private OpportunityReportStatus status;
    @Column(name = "reviewed_by", length = 100, columnDefinition = "nvarchar(100)")
    private String reviewedBy;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Version @Column(nullable = false)
    private long version;

    protected OpportunityReportEntity() {}
    public OpportunityReportEntity(OpportunityEntity opportunity, UserEntity reporter,
            OpportunityReportReason reason, String details) {
        this.opportunity = opportunity; this.reporter = reporter; this.reason = reason;
        this.details = details == null || details.isBlank() ? null : details.trim();
        this.status = OpportunityReportStatus.OPEN; this.createdAt = Instant.now();
    }
    public Integer getId() { return id; }
    public OpportunityEntity getOpportunity() { return opportunity; }
    public UserEntity getReporter() { return reporter; }
    public OpportunityReportReason getReason() { return reason; }
    public String getDetails() { return details; }
    public OpportunityReportStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public void review(OpportunityReportStatus status, String reviewer) {
        this.status = status; this.reviewedBy = reviewer; this.reviewedAt = Instant.now();
    }
}
