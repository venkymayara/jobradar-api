package com.jobradar.opportunity;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class WalkInExpirationServiceTest {
    @Test void expiresOnlyElapsedActiveWalkInsAndRecordsStatusHistory() {
        var detailsRepository = mock(WalkInDetailsRepository.class);
        var history = mock(OpportunityStatusHistoryRepository.class);
        var clock = Clock.fixed(Instant.parse("2026-06-02T12:00:00Z"), ZoneOffset.UTC);
        var service = new WalkInExpirationService(detailsRepository, history, clock);
        var details = mock(WalkInDetailsEntity.class);
        var opportunity = mock(OpportunityEntity.class);
        when(details.getOpportunity()).thenReturn(opportunity);
        when(opportunity.getStatus()).thenReturn(OpportunityStatus.ACTIVE);
        when(detailsRepository.findExpired(any(), eq(OpportunityStatus.ACTIVE), eq(OpportunityType.WALK_IN), any(Pageable.class)))
                .thenReturn(List.of(details), List.of());

        int expired = service.expireDueDrives();

        org.junit.jupiter.api.Assertions.assertEquals(1, expired);
        verify(opportunity).transition(OpportunityStatus.EXPIRED);
        verify(history).save(argThat(entry -> entry.getNewStatus() == OpportunityStatus.EXPIRED));
        verify(detailsRepository, times(2)).findExpired(any(), eq(OpportunityStatus.ACTIVE),
                eq(OpportunityType.WALK_IN), any(Pageable.class));
    }
}
