package com.jobradar.opportunity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jobradar.common.pagination.PaginationParameters;
import com.jobradar.company.CompanyEntity;
import com.jobradar.taxonomy.location.LocationEntity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class WalkInServiceTest {
    private final OpportunityRepository opportunities = mock(OpportunityRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T20:00:00Z"), ZoneId.of("UTC"));
    private final WalkInService service = new WalkInService(opportunities, clock);

    @Test void todayUsesEachDrivesLocalCalendarDateAcrossUtcBoundary() {
        OpportunityEntity india = opportunity("2026-06-02", "Asia/Kolkata", "2026-06-02T04:30:00Z");
        OpportunityEntity losAngeles = opportunity("2026-06-02", "America/Los_Angeles", "2026-06-02T10:00:00Z");
        when(opportunities.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(call ->
                new PageImpl<>(List.of(india, losAngeles), call.getArgument(1), 2));

        var result = service.today(emptyFilters(), new PaginationParameters());

        assertEquals(1, result.totalElements());
        assertEquals("Asia/Kolkata", result.content().getFirst().timeZoneId());
    }

    @Test void todayExcludesCompletedDrivesButDateSpecificFiltersKeepLocalWeekSemantics() {
        OpportunityEntity endedToday = opportunity("2026-06-02", "Asia/Kolkata", "2026-06-01T19:00:00Z");
        when(opportunities.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(call ->
                new PageImpl<>(List.of(endedToday), call.getArgument(1), 1));
        assertEquals(0, service.today(emptyFilters(), new PaginationParameters()).totalElements());
        assertThrows(com.jobradar.common.exception.BadRequestException.class, () -> service.list(
                new WalkInFilters(null, null, null, null, null, null, null, null, null, "not-a-date"),
                new PaginationParameters()));
    }

    @Test void tomorrowAndThisWeekUseEachDriveLocalCalendar() {
        OpportunityEntity tomorrow = opportunity("2026-06-03", "Asia/Kolkata", "2026-06-03T05:00:00Z");
        OpportunityEntity thisWeek = opportunity("2026-06-06", "Asia/Kolkata", "2026-06-06T05:00:00Z");
        OpportunityEntity nextWeek = opportunity("2026-06-08", "Asia/Kolkata", "2026-06-08T05:00:00Z");
        when(opportunities.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(call ->
                new PageImpl<>(List.of(tomorrow, thisWeek, nextWeek), call.getArgument(1), 3));

        var tomorrowResult = service.list(withDate("TOMORROW"), new PaginationParameters());
        var weekResult = service.list(withDate("THIS_WEEK"), new PaginationParameters());

        assertEquals(List.of("2026-06-03"), tomorrowResult.content().stream().map(d -> d.driveDate().toString()).toList());
        assertEquals(List.of("2026-06-03", "2026-06-06"), weekResult.content().stream().map(d -> d.driveDate().toString()).toList());
    }

    @Test void upcomingAppliesFiltersAndPageable() {
        OpportunityEntity event = opportunity("2026-06-02", "Asia/Kolkata", "2026-06-02T04:30:00Z");
        when(opportunities.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(call ->
                new PageImpl<>(List.of(event), call.getArgument(1), 1));
        PaginationParameters pagination = new PaginationParameters();
        pagination.setPage(1); pagination.setSize(5);
        var filters = new WalkInFilters("Java", "Bengaluru", "Developer", List.of("java"),
                "Acme", 1, 10, new BigDecimal("10000"), new BigDecimal("100000"), null);
        var result = service.upcoming(filters, pagination);
        assertEquals(6, result.totalElements());
        var capturedPage = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(opportunities).findAll(any(Specification.class), capturedPage.capture());
        assertEquals(1, capturedPage.getValue().getPageNumber());
        assertEquals(5, capturedPage.getValue().getPageSize());
        assertTrue(capturedPage.getValue().getSort().isSorted());
    }

    @Test void homepageUpcomingWalkInsAreBoundedAndRequireFutureActiveDrives() {
        when(opportunities.findUpcomingWalkInsForHomepage(eq(OpportunityStatus.ACTIVE), eq(OpportunityType.WALK_IN),
                eq(clock.instant()), any(Pageable.class))).thenReturn(List.of());

        assertTrue(service.upcomingForHomepage().isEmpty());

        var page = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(opportunities).findUpcomingWalkInsForHomepage(eq(OpportunityStatus.ACTIVE), eq(OpportunityType.WALK_IN),
                eq(clock.instant()), page.capture());
        assertEquals(6, page.getValue().getPageSize());
    }

    private WalkInFilters emptyFilters() { return new WalkInFilters(null, null, null, null, null, null, null, null, null, null); }
    private WalkInFilters withDate(String date) { return new WalkInFilters(null, null, null, null, null, null, null, null, null, date); }

    private OpportunityEntity opportunity(String localDate, String zone, String endUtc) {
        OpportunityEntity opportunity = mock(OpportunityEntity.class);
        WalkInDetailsEntity drive = mock(WalkInDetailsEntity.class);
        CompanyEntity company = mock(CompanyEntity.class);
        LocationEntity location = mock(LocationEntity.class);
        when(opportunity.getId()).thenReturn(4); when(opportunity.getTitle()).thenReturn("Java Engineer");
        when(opportunity.getCompany()).thenReturn(company); when(company.getId()).thenReturn(2);
        when(company.getCompanyName()).thenReturn("Acme"); when(company.getLogoUrl()).thenReturn(null);
        when(opportunity.getLocation()).thenReturn(location); when(location.getCity()).thenReturn("Bengaluru");
        when(location.getState()).thenReturn("Karnataka"); when(location.getCountry()).thenReturn("India");
        when(opportunity.getRoles()).thenReturn(Set.of()); when(opportunity.getSkills()).thenReturn(Set.of());
        when(opportunity.getMinExperience()).thenReturn(1); when(opportunity.getMaxExperience()).thenReturn(10);
        when(opportunity.getMinSalary()).thenReturn(null); when(opportunity.getMaxSalary()).thenReturn(null);
        when(opportunity.getCurrencyCode()).thenReturn("INR"); when(opportunity.getStatus()).thenReturn(OpportunityStatus.ACTIVE);
        when(opportunity.isVerified()).thenReturn(false); when(opportunity.getWalkInDetails()).thenReturn(drive);
        when(drive.getDriveDate()).thenReturn(LocalDate.parse(localDate));
        when(drive.getStartTime()).thenReturn(LocalTime.of(9, 0)); when(drive.getEndTime()).thenReturn(LocalTime.of(10, 0));
        when(drive.getTimeZoneId()).thenReturn(zone); when(drive.getDriveStartAtUtc()).thenReturn(Instant.parse("2026-06-02T03:30:00Z"));
        when(drive.getDriveEndAtUtc()).thenReturn(Instant.parse(endUtc)); when(drive.getVenue()).thenReturn("Hall");
        when(drive.getVenueAddress()).thenReturn("Address"); when(drive.getSourceUrl()).thenReturn("https://example.com");
        return opportunity;
    }
}
