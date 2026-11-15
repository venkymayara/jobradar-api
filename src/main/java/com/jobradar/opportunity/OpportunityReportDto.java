package com.jobradar.opportunity;
import java.time.Instant;
public record OpportunityReportDto(Integer reportId, Integer opportunityId, String opportunityTitle,
        OpportunityReportReason reason, String details, OpportunityReportStatus status,
        Instant createdAt, String reviewedBy, Instant reviewedAt) {
    public static OpportunityReportDto from(OpportunityReportEntity report) {
        return new OpportunityReportDto(report.getId(), report.getOpportunity().getId(),
                report.getOpportunity().getTitle(), report.getReason(), report.getDetails(),
                report.getStatus(), report.getCreatedAt(), report.getReviewedBy(), report.getReviewedAt());
    }
}
