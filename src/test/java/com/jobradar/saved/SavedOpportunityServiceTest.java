package com.jobradar.saved;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jobradar.auth.user.UserEntity;
import com.jobradar.auth.user.UserRepository;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.exception.ForbiddenException;
import com.jobradar.common.exception.ResourceNotFoundException;
import com.jobradar.common.pagination.PaginationParameters;
import com.jobradar.company.CompanyEntity;
import com.jobradar.opportunity.OpportunityEntity;
import com.jobradar.opportunity.OpportunityRepository;
import com.jobradar.opportunity.OpportunityStatus;
import com.jobradar.taxonomy.location.LocationEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class SavedOpportunityServiceTest {
    private final SavedOpportunityRepository saved = mock(SavedOpportunityRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OpportunityRepository opportunities = mock(OpportunityRepository.class);
    private final SavedOpportunityService service = new SavedOpportunityService(saved, users, opportunities);
    private final Authentication authentication = new UsernamePasswordAuthenticationToken("person@example.com", "ignored", List.of());
    private final UserEntity user = user(7);

    @Test void savesAnAvailableOpportunityUsingTheCompositeOwnerKey() {
        OpportunityEntity opportunity = opportunity(42);
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(opportunities.findById(42)).thenReturn(Optional.of(opportunity));
        when(saved.existsById(new SavedOpportunityId(7, 42))).thenReturn(false);

        service.save(42, authentication);

        ArgumentCaptor<SavedOpportunityEntity> entry = ArgumentCaptor.forClass(SavedOpportunityEntity.class);
        verify(saved).saveAndFlush(entry.capture());
        assertEquals(new SavedOpportunityId(7, 42), entry.getValue().getId());
        assertSame(user, entry.getValue().getUser());
        assertSame(opportunity, entry.getValue().getOpportunity());
    }

    @Test void rejectsDuplicateAndConcurrentDuplicateSaves() {
        OpportunityEntity opportunity = opportunity(42);
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(opportunities.findById(42)).thenReturn(Optional.of(opportunity));
        when(saved.existsById(new SavedOpportunityId(7, 42))).thenReturn(true, false);
        doThrow(new DataIntegrityViolationException("duplicate composite key")).when(saved)
                .saveAndFlush(any(SavedOpportunityEntity.class));

        assertThrows(ConflictException.class, () -> service.save(42, authentication));
        assertThrows(ConflictException.class, () -> service.save(42, authentication));
        verify(saved, times(1)).saveAndFlush(any(SavedOpportunityEntity.class));
    }

    @Test void rejectsMissingOrUnavailableOpportunities() {
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(opportunities.findById(42)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.save(42, authentication));

        OpportunityEntity pending = opportunity(42);
        when(opportunities.findById(42)).thenReturn(Optional.of(pending));
        when(pending.getStatus()).thenReturn(OpportunityStatus.PENDING);
        assertThrows(ResourceNotFoundException.class, () -> service.save(42, authentication));
        verify(saved, never()).saveAndFlush(any());
    }

    @Test void removesOnlyTheCurrentUsersBookmarkAndIsIdempotent() {
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        SavedOpportunityId ownerKey = new SavedOpportunityId(7, 42);
        SavedOpportunityEntity entry = mock(SavedOpportunityEntity.class);
        when(saved.findById(ownerKey)).thenReturn(Optional.of(entry), Optional.empty());

        service.remove(42, authentication);
        service.remove(42, authentication);

        verify(saved, times(2)).findById(ownerKey);
        verify(saved).delete(entry);
        verify(saved, times(2)).findById(any(SavedOpportunityId.class));
    }

    @Test void savedPageIsPaginatedAndContainsOpportunityDtos() {
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        OpportunityEntity opportunity = opportunity(42);
        SavedOpportunityEntity entry = mock(SavedOpportunityEntity.class);
        when(entry.getOpportunity()).thenReturn(opportunity);
        when(saved.findAvailableOpportunities(eq(7), eq(OpportunityStatus.ACTIVE), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(entry), call.getArgument(2), 12));
        PaginationParameters pagination = new PaginationParameters();
        pagination.setPage(1);
        pagination.setSize(5);

        var result = service.list(pagination, authentication);

        assertEquals(42, result.content().getFirst().opportunityId());
        assertEquals(12, result.totalElements());
        assertEquals(3, result.totalPages());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(saved).findAvailableOpportunities(eq(7), eq(OpportunityStatus.ACTIVE), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        assertEquals("savedAt", pageable.getValue().getSort().getOrderFor("savedAt").getProperty());
    }

    @Test void serviceDoesNotTrustUnauthenticatedCallers() {
        assertThrows(ForbiddenException.class, () -> service.list(new PaginationParameters(), null));
        verifyNoInteractions(saved, users, opportunities);
    }

    private UserEntity user(int id) {
        UserEntity value = new UserEntity("Person", "person@example.com", "hashed");
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private OpportunityEntity opportunity(int id) {
        OpportunityEntity value = mock(OpportunityEntity.class);
        CompanyEntity company = mock(CompanyEntity.class);
        LocationEntity location = mock(LocationEntity.class);
        when(value.getId()).thenReturn(id);
        when(value.getTitle()).thenReturn("Engineer");
        when(value.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(3);
        when(company.getCompanyName()).thenReturn("Acme");
        when(value.getLocation()).thenReturn(location);
        when(location.getId()).thenReturn(9);
        when(location.getCity()).thenReturn("Pune");
        when(location.getCountry()).thenReturn("India");
        when(value.getRoles()).thenReturn(java.util.Set.of());
        when(value.getSkills()).thenReturn(java.util.Set.of());
        when(value.getStatus()).thenReturn(OpportunityStatus.ACTIVE);
        return value;
    }
}
