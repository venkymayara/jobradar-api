package com.jobradar.opportunity;

import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.exception.BadRequestException;
import com.jobradar.common.exception.ResourceNotFoundException;
import com.jobradar.common.pagination.PaginationParameters;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WalkInService {
    private static final int CANDIDATE_PAGE_SIZE = 100;
    private static final Set<String> SORTS = Set.of("createdAt", "driveStartAtUtc", "title");
    private final OpportunityRepository opportunities;
    private final Clock clock;

    public WalkInService(OpportunityRepository opportunities, Clock clock) {
        this.opportunities = opportunities; this.clock = clock;
    }

    public PaginatedResponse<WalkInDto> list(WalkInFilters filters, PaginationParameters pagination) {
        return search(filters, pagination);
    }

    public PaginatedResponse<WalkInDto> upcoming(WalkInFilters filters, PaginationParameters pagination) {
        WalkInFilters upcoming = new WalkInFilters(filters.keyword(), filters.location(), filters.role(), filters.skills(),
                filters.company(), filters.minExperience(), filters.maxExperience(), filters.minSalary(),
                filters.maxSalary(), "UPCOMING");
        return search(upcoming, pagination);
    }

    public List<WalkInDto> upcomingForHomepage() {
        return opportunities.findUpcomingWalkInsForHomepage(OpportunityStatus.ACTIVE, OpportunityType.WALK_IN,
                        clock.instant(), PageRequest.of(0, 6))
                .stream().map(WalkInDto::from).toList();
    }

    public PaginatedResponse<WalkInDto> today(WalkInFilters filters, PaginationParameters pagination) {
        WalkInFilters today = new WalkInFilters(filters.keyword(), filters.location(), filters.role(), filters.skills(),
                filters.company(), filters.minExperience(), filters.maxExperience(), filters.minSalary(),
                filters.maxSalary(), "TODAY");
        return search(today, pagination);
    }

    public WalkInDto get(Integer id) {
        OpportunityEntity opportunity = opportunities.findExpandedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Walk-in drive not found"));
        WalkInDetailsEntity drive = opportunity.getWalkInDetails();
        if (opportunity.getOpportunityType() != OpportunityType.WALK_IN || drive == null
                || opportunity.getStatus() != OpportunityStatus.ACTIVE || !drive.getDriveEndAtUtc().isAfter(clock.instant()))
            throw new ResourceNotFoundException("Walk-in drive not found");
        return WalkInDto.from(opportunity);
    }

    private PaginatedResponse<WalkInDto> search(WalkInFilters filters, PaginationParameters pagination) {
        Instant now = clock.instant();
        DateWindow window = dateWindow(filters.date(), now);
        String requestedSort = pagination.getSort();
        String sortProperty = requestedSort.equals("createdAt") ? "walkInDetails.driveStartAtUtc" : requestedSort.equals("driveStartAtUtc")
                ? "walkInDetails.driveStartAtUtc" : requestedSort;
        if (!SORTS.contains(requestedSort)) throw new BadRequestException("Unsupported walk-in sort field");
        Sort.Direction direction = requestedSort.equals("createdAt") ? Sort.Direction.ASC
                : Sort.Direction.fromString(pagination.getDirection());
        Pageable responsePageable = PageRequest.of(pagination.getPage(), pagination.getSize(), Sort.by(direction, sortProperty));

        OpportunitySearchCriteria criteria = new OpportunitySearchCriteria(clean(filters.keyword()), clean(filters.role()),
                filters.skills() == null ? List.of() : filters.skills().stream().filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().toList(),
                clean(filters.company()), clean(filters.location()), filters.minExperience(), filters.maxExperience(),
                null, null, OpportunityType.WALK_IN, filters.minSalary(), filters.maxSalary(), null,
                window == null ? now : window.start().minusNanos(1), window == null ? null : window.end(), now);
        Specification<OpportunityEntity> spec = OpportunitySpecifications.publicSearch(criteria);
        if (window == null) {
            Page<WalkInDto> page = opportunities.findAll(spec, responsePageable).map(WalkInDto::from);
            return PaginatedResponse.from(page);
        }

        List<WalkInDto> filtered = collectDateWindow(spec, window, now, direction);
        Page<WalkInDto> page = slice(filtered, responsePageable);
        return PaginatedResponse.from(page);
    }

    private List<WalkInDto> collectDateWindow(Specification<OpportunityEntity> spec, DateWindow window,
            Instant now, Sort.Direction direction) {
        List<WalkInDto> filtered = new ArrayList<>();
        Pageable candidatePage = PageRequest.of(0, CANDIDATE_PAGE_SIZE,
                Sort.by(direction, "walkInDetails.driveStartAtUtc"));
        Page<OpportunityEntity> candidates;
        do {
            candidates = opportunities.findAll(spec, candidatePage);
            for (OpportunityEntity opportunity : candidates.getContent()) {
                WalkInDetailsEntity drive = opportunity.getWalkInDetails();
                if (drive != null && matchesLocalDate(drive, window) && drive.getDriveEndAtUtc().isAfter(now))
                    filtered.add(WalkInDto.from(opportunity));
            }
            candidatePage = candidatePage.next();
        } while (!candidates.isLast());
        return filtered;
    }

    private Page<WalkInDto> slice(List<WalkInDto> rows, Pageable pageable) {
        int from = Math.min(Math.toIntExact(pageable.getOffset()), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(from, to), pageable, rows.size());
    }

    private boolean matchesLocalDate(WalkInDetailsEntity drive, DateWindow window) {
        LocalDate localToday = LocalDate.now(clock.withZone(ZoneId.of(drive.getTimeZoneId())));
        LocalDate date = drive.getDriveDate();
        return switch (window.mode()) {
            case TODAY -> date.equals(localToday);
            case TOMORROW -> date.equals(localToday.plusDays(1));
            case THIS_WEEK -> !date.isBefore(localToday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                    && !date.isAfter(localToday.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case DATE -> date.equals(window.date());
            case UPCOMING -> true;
        };
    }

    private DateWindow dateWindow(String raw, Instant now) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("UPCOMING")) return null;
        String mode = raw.trim().toUpperCase(Locale.ROOT);
        LocalDate utcDate = now.atZone(ZoneOffset.UTC).toLocalDate();
        if (mode.equals("TODAY")) return broad(DateMode.TODAY, utcDate.minusDays(1), utcDate.plusDays(1), null);
        if (mode.equals("TOMORROW")) return broad(DateMode.TOMORROW, utcDate.minusDays(1), utcDate.plusDays(2), null);
        if (mode.equals("THIS_WEEK")) return broad(DateMode.THIS_WEEK, utcDate.minusDays(8), utcDate.plusDays(8), null);
        try {
            LocalDate date = LocalDate.parse(raw);
            return broad(DateMode.DATE, date.minusDays(1), date.plusDays(1), date);
        } catch (RuntimeException exception) {
            throw new BadRequestException("Walk-in date must be TODAY, TOMORROW, THIS_WEEK, UPCOMING, or yyyy-MM-dd");
        }
    }

    private DateWindow broad(DateMode mode, LocalDate first, LocalDate last, LocalDate date) {
        Instant start = first.atStartOfDay(ZoneOffset.UTC).toInstant().minus(Duration.ofHours(14));
        Instant end = last.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().plus(Duration.ofHours(14));
        return new DateWindow(mode, date, start, end);
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private enum DateMode { TODAY, TOMORROW, THIS_WEEK, DATE, UPCOMING }
    private record DateWindow(DateMode mode, LocalDate date, Instant start, Instant end) {}
}
