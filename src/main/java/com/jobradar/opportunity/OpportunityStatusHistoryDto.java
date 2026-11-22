package com.jobradar.opportunity;
import java.time.Instant;
public record OpportunityStatusHistoryDto(OpportunityStatus previousStatus, OpportunityStatus newStatus,
        Instant changedAt, String note) {
    static OpportunityStatusHistoryDto from(OpportunityStatusHistoryEntity entity, boolean includeNote) {
        return new OpportunityStatusHistoryDto(entity.getPreviousStatus(), entity.getNewStatus(), entity.getChangedAt(),
                includeNote ? entity.getReviewNote() : null);
    }
}
