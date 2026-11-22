package com.jobradar.opportunity;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.List;
import com.jobradar.auth.user.UserEntity;
import com.jobradar.notification.NotificationService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OpportunityExpirationServiceTest {
    @Test void expiresDueActiveOpportunityAndIsIdempotentAcrossScans() {
        OpportunityRepository opportunities = mock(OpportunityRepository.class);
        OpportunityStatusHistoryRepository history = mock(OpportunityStatusHistoryRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-02T12:00:00Z"), ZoneOffset.UTC);
        OpportunityEntity opportunity = mock(OpportunityEntity.class);
        UserEntity publisher = mock(UserEntity.class);
        when(opportunity.getStatus()).thenReturn(OpportunityStatus.ACTIVE);
        when(opportunity.getPublisher()).thenReturn(publisher);
        when(opportunity.getTitle()).thenReturn("Java Engineer");
        when(opportunity.getId()).thenReturn(7);
        when(opportunities.findByStatusAndApplicationDeadlineBefore(eq(OpportunityStatus.ACTIVE), eq(LocalDate.of(2026, 6, 2))))
                .thenReturn(List.of(opportunity), List.of());
        OpportunityExpirationService service = new OpportunityExpirationService(opportunities, history, notifications, clock, true);

        assertEquals(1, service.expireDueOpportunities());
        assertEquals(0, service.expireDueOpportunities());
        verify(opportunity).transition(OpportunityStatus.EXPIRED);
        verify(history).save(argThat(entry -> entry.getNewStatus() == OpportunityStatus.EXPIRED));
        verify(notifications).create(eq(publisher), eq("Opportunity expired"), anyString(), eq(com.jobradar.notification.NotificationType.OPPORTUNITY_EXPIRED), eq(7));
    }
}
