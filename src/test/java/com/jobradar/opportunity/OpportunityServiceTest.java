package com.jobradar.opportunity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jobradar.auth.user.UserEntity;
import com.jobradar.auth.user.UserRepository;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.exception.ForbiddenException;
import com.jobradar.common.pagination.PaginationParameters;
import com.jobradar.company.CompanyEntity;
import com.jobradar.company.CompanyRepository;
import com.jobradar.taxonomy.category.CategoryEntity;
import com.jobradar.taxonomy.location.LocationEntity;
import com.jobradar.taxonomy.location.LocationRepository;
import com.jobradar.taxonomy.role.RoleEntity;
import com.jobradar.taxonomy.role.RoleRepository;
import com.jobradar.taxonomy.skill.SkillRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class OpportunityServiceTest {
    private final OpportunityRepository opportunities = mock(OpportunityRepository.class);
    private final OpportunityStatusHistoryRepository history = mock(OpportunityStatusHistoryRepository.class);
    private final WalkInDetailsRepository walkInDetails = mock(WalkInDetailsRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final LocationRepository locations = mock(LocationRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final SkillRepository skills = mock(SkillRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OpportunityService service = new OpportunityService(opportunities, history, walkInDetails, companies,
            locations, roles, skills, users);

    @Test void searchAcceptsAllFiltersAndUsesPageableSort() {
        PaginationParameters page = new PaginationParameters();
        page.setPage(2); page.setSize(15); page.setSort("title"); page.setDirection("ASC");
        Pageable pageable = PageRequest.of(2, 15, Sort.by("title"));
        when(opportunities.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));
        var result = service.search("java", "developer", List.of("java", "spring-boot"), "Acme",
                "Bengaluru", 2, 8, WorkMode.REMOTE, EmploymentType.FULL_TIME,
                OpportunityType.JOB, new BigDecimal("50000"), new BigDecimal("200000"), "7d", page);
        assertEquals(2, result.page());
        assertEquals(15, result.size());
        verify(opportunities).findAll(any(Specification.class), eq(pageable));
    }

    @Test void searchRejectsReversedExperienceAndSalaryRanges() {
        PaginationParameters page = new PaginationParameters();
        assertThrows(com.jobradar.common.exception.BadRequestException.class, () -> service.search(null, null,
                null, null, null, 8, 2, null, null, null, null, null, null, page));
        assertThrows(com.jobradar.common.exception.BadRequestException.class, () -> service.search(null, null,
                null, null, null, null, null, null, null, null, new BigDecimal("20"), new BigDecimal("10"), null, page));
    }

    @Test void latestUsesOnlyActiveOpportunitiesAndNoUnboundedPage() {
        var now = java.time.Instant.parse("2026-11-22T12:00:00Z");
        var today = java.time.LocalDate.of(2026, 11, 22);
        var limit = PageRequest.of(0, 10);
        when(opportunities.findLatestPublic(OpportunityStatus.ACTIVE, today, now, limit)).thenReturn(List.of());

        assertTrue(service.latest(today, now).isEmpty());

        verify(opportunities).findLatestPublic(OpportunityStatus.ACTIVE, today, now, limit);
    }

    @Test void communitySubmissionStartsPendingAndUnverifiedAndRecordsHistory() {
        OpportunityRequest request = request();
        UserEntity user = mock(UserEntity.class);
        CompanyEntity company = mock(CompanyEntity.class);
        LocationEntity location = mock(LocationEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        CategoryEntity category = mock(CategoryEntity.class);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(companies.findByIdAndActiveTrue(10)).thenReturn(Optional.of(company));
        when(locations.findByIdAndActiveTrue(20)).thenReturn(Optional.of(location));
        when(roles.findAllById(Set.of(30))).thenReturn(List.of(role));
        when(role.isActive()).thenReturn(true); when(role.getCategory()).thenReturn(category); when(category.isActive()).thenReturn(true);
        when(role.getId()).thenReturn(30); when(role.getRoleName()).thenReturn("Java Developer"); when(role.getSlug()).thenReturn("java-developer");
        when(company.getId()).thenReturn(10); when(company.getCompanyName()).thenReturn("Acme");
        when(company.getLogoUrl()).thenReturn(null);
        when(location.getId()).thenReturn(20); when(location.getCity()).thenReturn("Bengaluru");
        when(location.getState()).thenReturn("Karnataka"); when(location.getCountry()).thenReturn("India");
        when(opportunities.save(any(OpportunityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(history.findByOpportunityIdOrderByChangedAtAscIdAsc(null)).thenReturn(List.of());
        var response = service.create(request, "owner@example.com");
        assertEquals(OpportunityStatus.PENDING, response.status());
        assertFalse(response.verified());
        verify(history).save(argThat(entry -> entry.getPreviousStatus() == null
                && entry.getNewStatus() == OpportunityStatus.PENDING));
    }

    @Test void ownerCanPauseOwnActiveOpportunityButCannotApproveIt() {
        UserEntity owner = mock(UserEntity.class);
        UserEntity publisher = mock(UserEntity.class);
        when(owner.getId()).thenReturn(4); when(publisher.getId()).thenReturn(4);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        OpportunityEntity active = new OpportunityEntity(request(), mock(CompanyEntity.class), mock(LocationEntity.class),
                publisher, Set.of(), Set.of(), OpportunityStatus.ACTIVE);
        when(opportunities.findById(9)).thenReturn(Optional.of(active));
        when(history.findByOpportunityIdOrderByChangedAtAscIdAsc(null)).thenReturn(List.of());
        var user = new UsernamePasswordAuthenticationToken("owner@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var paused = service.changeStatus(9, OpportunityStatus.PAUSED, user);
        assertEquals(OpportunityStatus.PAUSED, paused.status());
        assertFalse(paused.verified());
        assertThrows(ConflictException.class, () -> service.changeStatus(9, OpportunityStatus.ACTIVE, user));
    }

    @Test void userCannotEditAnotherUsersOpportunity() {
        UserEntity other = mock(UserEntity.class); UserEntity publisher = mock(UserEntity.class);
        when(other.getId()).thenReturn(8); when(publisher.getId()).thenReturn(4);
        when(users.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(other));
        OpportunityEntity entity = new OpportunityEntity(request(), mock(CompanyEntity.class), mock(LocationEntity.class),
                publisher, Set.of(), Set.of(), OpportunityStatus.PENDING);
        when(opportunities.findById(7)).thenReturn(Optional.of(entity));
        var auth = new UsernamePasswordAuthenticationToken("other@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertThrows(ForbiddenException.class, () -> service.update(7, request(), auth));
    }

    @Test void moderatorCanApprovePendingOpportunityButApprovalDoesNotVerifyIt() {
        UserEntity publisher = mock(UserEntity.class);
        CompanyEntity company = mock(CompanyEntity.class);
        LocationEntity location = mock(LocationEntity.class);
        when(company.getCompanyName()).thenReturn("Acme");
        when(location.getCity()).thenReturn("Bengaluru"); when(location.getState()).thenReturn(null);
        when(location.getCountry()).thenReturn("India");
        OpportunityEntity pending = new OpportunityEntity(request(), company, location, publisher,
                Set.of(), Set.of(), OpportunityStatus.PENDING);
        when(opportunities.findById(15)).thenReturn(Optional.of(pending));
        when(users.findByEmailIgnoreCase("moderator@example.com")).thenReturn(Optional.empty());
        when(history.findByOpportunityIdOrderByChangedAtAscIdAsc(null)).thenReturn(List.of());
        var moderator = new UsernamePasswordAuthenticationToken("moderator@example.com", "",
                List.of(new SimpleGrantedAuthority("ROLE_MODERATOR")));

        OpportunityDto result = service.changeStatus(15, OpportunityStatus.ACTIVE, moderator);

        assertEquals(OpportunityStatus.ACTIVE, result.status());
        assertNotNull(result.publishedAt());
        assertFalse(result.verified());
        verify(history).save(argThat(entry -> entry.getPreviousStatus() == OpportunityStatus.PENDING
                && entry.getNewStatus() == OpportunityStatus.ACTIVE));
    }

    @Test void moderatorTransitionPreservesStatusHistoryAndAdminCanModerate() {
        UserEntity publisher = mock(UserEntity.class);
        OpportunityEntity active = new OpportunityEntity(request(), mock(CompanyEntity.class), mock(LocationEntity.class),
                publisher, Set.of(), Set.of(), OpportunityStatus.ACTIVE);
        when(opportunities.findById(18)).thenReturn(Optional.of(active));
        when(history.findByOpportunityIdOrderByChangedAtAscIdAsc(null)).thenReturn(List.of());
        var admin = new UsernamePasswordAuthenticationToken("admin@example.com", "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var result = service.changeStatus(18, OpportunityStatus.PAUSED, admin);

        assertEquals(OpportunityStatus.PAUSED, result.status());
        verify(history).save(argThat(entry -> entry.getPreviousStatus() == OpportunityStatus.ACTIVE
                && entry.getNewStatus() == OpportunityStatus.PAUSED
                && entry.getChangedBy().equals("admin@example.com")));
    }

    @Test void onlyModeratorCanVerifyAnActiveOpportunity() {
        UserEntity publisher = mock(UserEntity.class);
        OpportunityEntity active = new OpportunityEntity(request(), mock(CompanyEntity.class), mock(LocationEntity.class),
                publisher, Set.of(), Set.of(), OpportunityStatus.ACTIVE);
        when(opportunities.findById(16)).thenReturn(Optional.of(active));
        when(history.findByOpportunityIdOrderByChangedAtAscIdAsc(null)).thenReturn(List.of());
        var moderator = new UsernamePasswordAuthenticationToken("moderator@example.com", "",
                List.of(new SimpleGrantedAuthority("ROLE_MODERATOR")));
        var user = new UsernamePasswordAuthenticationToken("user@example.com", "",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(service.verify(16, true, moderator).verified());
        assertThrows(ForbiddenException.class, () -> service.verify(16, false, user));
    }

    private OpportunityRequest request() {
        return new OpportunityRequest("Java Engineer", 10, 20, WorkMode.REMOTE, EmploymentType.FULL_TIME,
                2, 8, new BigDecimal("50000"), new BigDecimal("150000"), "INR", OpportunityType.JOB,
                "Build backend services", "https://example.com/apply", "https://example.com/job", null,
                null, null, null, null, null, null, null, null, null, null, Set.of(30), Set.of());
    }
}
