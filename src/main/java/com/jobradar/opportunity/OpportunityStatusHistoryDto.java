package com.jobradar.opportunity;
import java.time.Instant;
public record OpportunityStatusHistoryDto(OpportunityStatus previousStatus, OpportunityStatus newStatus,
        Instant changedAt) {
    static OpportunityStatusHistoryDto from(OpportunityStatusHistoryEntity entity) {
        return new OpportunityStatusHistoryDto(entity.getPreviousStatus(), entity.getNewStatus(), entity.getChangedAt());
    }
}
