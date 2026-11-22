package com.jobradar.opportunity;

import com.jobradar.auth.user.UserEntity;
import com.jobradar.auth.user.UserRepository;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.exception.ForbiddenException;
import com.jobradar.common.exception.ResourceNotFoundException;
import com.jobradar.common.pagination.PaginationParameters;
import java.util.Set;
import java.time.Clock;
import java.time.Duration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.jobradar.notification.NotificationService;
import com.jobradar.notification.NotificationType;

@Service
@Transactional(readOnly = true)
public class OpportunityReportService {
    private final OpportunityReportRepository reports;
    private final OpportunityRepository opportunities;
    private final UserRepository users;
    private final Clock clock;
    private final NotificationService notifications;

    @Autowired
    public OpportunityReportService(OpportunityReportRepository reports, OpportunityRepository opportunities,
            UserRepository users, Clock clock, NotificationService notifications) {
        this.reports = reports; this.opportunities = opportunities; this.users = users; this.clock = clock; this.notifications = notifications;
    }
    public OpportunityReportService(OpportunityReportRepository reports, OpportunityRepository opportunities,
            UserRepository users, Clock clock) {
        this(reports, opportunities, users, clock, null);
    }

    @Transactional
    public OpportunityReportDto report(Integer opportunityId, OpportunityReportRequest request, String reporterEmail) {
        OpportunityEntity opportunity = opportunities.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
        if (opportunity.getStatus() != OpportunityStatus.ACTIVE)
            throw new ResourceNotFoundException("Opportunity not found");
        UserEntity reporter = users.findByEmailIgnoreCase(reporterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
        if (reports.existsByOpportunityIdAndReporterIdAndStatus(opportunityId, reporter.getId(), OpportunityReportStatus.OPEN))
            throw new ConflictException("You already have an open report for this opportunity");
        if (reports.countByReporter_IdAndCreatedAtAfter(reporter.getId(), clock.instant().minus(Duration.ofHours(1))) >= 10)
            throw new ConflictException("You have submitted too many reports recently");
        try {
            return OpportunityReportDto.from(reports.saveAndFlush(new OpportunityReportEntity(opportunity, reporter,
                    request.parsedReason(), request.details())));
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new ConflictException("You already have an open report for this opportunity");
        }
    }

    public PaginatedResponse<OpportunityReportDto> openReports(PaginationParameters pagination, Authentication moderator) {
        requireModerator(moderator);
        Pageable pageable = PageRequest.of(pagination.getPage(), pagination.getSize(),
                Sort.by(Sort.Direction.fromString(pagination.getDirection()), "createdAt"));
        return PaginatedResponse.from(reports.findAllByStatus(OpportunityReportStatus.OPEN, pageable)
                .map(OpportunityReportDto::from));
    }

    public OpportunityReportDto get(Integer reportId, Authentication moderator) {
        requireModerator(moderator);
        OpportunityReportEntity report = reports.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return OpportunityReportDto.from(report);
    }

    @Transactional
    public OpportunityReportDto review(Integer reportId, OpportunityReportStatus next, Authentication reviewer) {
        requireModerator(reviewer);
        if (next == OpportunityReportStatus.OPEN) throw new ConflictException("A report cannot be reopened");
        OpportunityReportEntity report = reports.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        if (report.getStatus() != OpportunityReportStatus.OPEN)
            throw new ConflictException("This report has already been reviewed");
        report.review(next, reviewer.getName());
        if (notifications != null) notifications.create(report.getReporter(), "Report reviewed", "Your opportunity report was marked " + next.name().toLowerCase(java.util.Locale.ROOT) + ".", NotificationType.REPORT_UPDATED, report.getOpportunity().getId());
        return OpportunityReportDto.from(report);
    }

    private void requireModerator(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).noneMatch(Set.of("ROLE_MODERATOR", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")::contains))
            throw new ForbiddenException("Only moderators or administrators can access moderation reports");
    }
}
