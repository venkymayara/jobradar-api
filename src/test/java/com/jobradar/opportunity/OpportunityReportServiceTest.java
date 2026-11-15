package com.jobradar.opportunity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jobradar.auth.user.UserEntity;
import com.jobradar.auth.user.UserRepository;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.exception.ForbiddenException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

class OpportunityReportServiceTest {
    private final OpportunityReportRepository reports = mock(OpportunityReportRepository.class);
    private final OpportunityRepository opportunities = mock(OpportunityRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OpportunityReportService service = new OpportunityReportService(reports, opportunities, users,
            Clock.fixed(Instant.parse("2026-11-15T00:00:00Z"), ZoneOffset.UTC));

    @Test void authenticatedUserCanReportActiveOpportunity() {
        OpportunityEntity opportunity = mock(OpportunityEntity.class);
        UserEntity user = mock(UserEntity.class);
        when(opportunities.findById(5)).thenReturn(Optional.of(opportunity));
        when(opportunity.getStatus()).thenReturn(OpportunityStatus.ACTIVE);
        when(opportunity.getId()).thenReturn(5); when(opportunity.getTitle()).thenReturn("Java role");
        when(user.getId()).thenReturn(8);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(reports.save(any(OpportunityReportEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(reports.saveAndFlush(any(OpportunityReportEntity.class))).thenAnswer(call -> call.getArgument(0));
        var result = service.report(5, new OpportunityReportRequest("FAKE_JOB", "Repeated listing"), "user@example.com");
        assertEquals(5, result.opportunityId());
        assertEquals(OpportunityReportReason.FAKE_JOB, result.reason());
        assertEquals(OpportunityReportStatus.OPEN, result.status());
        assertEquals(OpportunityStatus.ACTIVE, opportunity.getStatus());
        verify(reports).saveAndFlush(any(OpportunityReportEntity.class));
    }

    @Test void duplicateOpenReportIsRejected() {
        OpportunityEntity opportunity = mock(OpportunityEntity.class);
        UserEntity user = mock(UserEntity.class);
        when(opportunities.findById(5)).thenReturn(Optional.of(opportunity));
        when(opportunity.getStatus()).thenReturn(OpportunityStatus.ACTIVE);
        when(user.getId()).thenReturn(8);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(reports.existsByOpportunityIdAndReporterIdAndStatus(5, 8, OpportunityReportStatus.OPEN)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.report(5,
                new OpportunityReportRequest("EXPIRED", null), "user@example.com"));
        verify(reports, never()).saveAndFlush(any());
    }

    @Test void repeatedReportsAreRateLimitedAfterTenPerHour() {
        OpportunityEntity opportunity = mock(OpportunityEntity.class);
        UserEntity user = mock(UserEntity.class);
        when(opportunities.findById(5)).thenReturn(Optional.of(opportunity));
        when(opportunity.getStatus()).thenReturn(OpportunityStatus.ACTIVE);
        when(user.getId()).thenReturn(8);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(reports.countByReporter_IdAndCreatedAtAfter(eq(8), any())).thenReturn(10L);

        assertThrows(ConflictException.class, () -> service.report(5,
                new OpportunityReportRequest("WRONG_INFORMATION", null), "user@example.com"));
        verify(reports, never()).saveAndFlush(any());
    }

    @Test void reviewIsRestrictedToModeratorAndClosesOpenReport() {
        OpportunityEntity opportunity = mock(OpportunityEntity.class);
        OpportunityReportEntity report = new OpportunityReportEntity(opportunity, mock(UserEntity.class),
                OpportunityReportReason.EXPIRED, null);
        when(opportunity.getId()).thenReturn(5); when(opportunity.getTitle()).thenReturn("Java role");
        when(reports.findById(3)).thenReturn(Optional.of(report));
        var member = new UsernamePasswordAuthenticationToken("member", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var moderator = new UsernamePasswordAuthenticationToken("moderator", "", List.of(new SimpleGrantedAuthority("ROLE_MODERATOR")));
        assertThrows(ForbiddenException.class, () -> service.review(3, OpportunityReportStatus.REVIEWED, member));
        assertEquals(OpportunityReportStatus.REVIEWED, service.review(3, OpportunityReportStatus.REVIEWED, moderator).status());
        assertEquals("moderator", report.getReviewedBy());
        assertEquals("moderator", service.get(3, moderator).reviewedBy());
        assertThrows(ConflictException.class, () -> service.review(3, OpportunityReportStatus.DISMISSED, moderator));
    }

    @Test void reportQueueAndDetailRequireModeratorOrAdmin() {
        var member = new UsernamePasswordAuthenticationToken("member", "",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var admin = new UsernamePasswordAuthenticationToken("admin", "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertThrows(ForbiddenException.class, () -> service.openReports(new com.jobradar.common.pagination.PaginationParameters(), member));
        assertThrows(ForbiddenException.class, () -> service.get(1, member));
        when(reports.findAllByStatus(eq(OpportunityReportStatus.OPEN), any())).thenReturn(org.springframework.data.domain.Page.empty());
        assertEquals(0, service.openReports(new com.jobradar.common.pagination.PaginationParameters(), admin).totalElements());
    }
}
