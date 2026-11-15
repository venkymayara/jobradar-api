package com.jobradar.company;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class CompanyServiceTest {
    private final CompanyRepository repository = mock(CompanyRepository.class);
    private final CompanyService service = new CompanyService(repository);
    private final CompanyRequest request = new CompanyRequest("Example Co", "https://example.com", null,
            "Bengaluru", "Technology", "Company description");

    @Test void userSubmissionIsPendingAndNeverVerified() {
        when(repository.save(any(CompanyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CompanyDto submitted = service.submit(request, "member@example.com");
        assertFalse(submitted.active());
        assertFalse(submitted.verified());
        verify(repository).save(argThat(entity -> !entity.isActive() && !entity.isVerified()
                && "member@example.com".equals(entity.getSubmittedBy())));
    }

    @Test void duplicateNameIsRejectedOnSubmission() {
        when(repository.existsByCompanyNameIgnoreCase("Example Co")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.submit(request, "member@example.com"));
        verify(repository, never()).save(any());
    }

    @Test void adminCreationDoesNotAutoVerifyAndIsActive() {
        when(repository.save(any(CompanyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CompanyDto created = service.create(request);
        assertTrue(created.active());
        assertFalse(created.verified());
    }

    @Test void onlyExplicitVerificationActionSetsVerificationMetadata() {
        CompanyEntity company = new CompanyEntity("Example Co", null, null, null, null, null, true, null);
        when(repository.findById(12)).thenReturn(Optional.of(company));
        CompanyDto verified = service.setVerification(12, true, "moderator@example.com");
        assertTrue(verified.verified());
        assertNotNull(verified.verifiedAt());
        assertEquals("moderator@example.com", company.getVerifiedBy());
        CompanyDto revoked = service.setVerification(12, false, "moderator@example.com");
        assertFalse(revoked.verified());
        assertNull(company.getVerifiedBy());
        assertNull(company.getVerifiedAt());
    }

    @Test void duplicateNameIsRejectedOnUpdate() {
        CompanyEntity company = new CompanyEntity("Example Co", null, null, null, null, null, true, null);
        when(repository.findById(12)).thenReturn(Optional.of(company));
        when(repository.existsByCompanyNameIgnoreCaseAndIdNot("Example Co 2", 12)).thenReturn(true);
        CompanyRequest duplicate = new CompanyRequest("Example Co 2", null, null, null, null, null);
        assertThrows(ConflictException.class, () -> service.update(12, duplicate));
    }

    @Test void activateAndDeactivateChangePublicVisibilityState() {
        CompanyEntity company = new CompanyEntity("Example Co", null, null, null, null, null, false, null);
        when(repository.findById(12)).thenReturn(Optional.of(company));
        service.setActive(12, true);
        assertTrue(company.isActive());
        service.setActive(12, false);
        assertFalse(company.isActive());
    }

    @Test void publicCompanyLookupHidesInactiveCompanies() {
        when(repository.findByIdAndActiveTrue(12)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getPublic(12));
    }

    @Test void searchAppliesPaginationAndReturnsPageMetadata() {
        var paging = new com.jobradar.common.pagination.PaginationParameters();
        paging.setPage(1); paging.setSize(10); paging.setSort("companyName"); paging.setDirection("ASC");
        var pageable = PageRequest.of(1, 10, Sort.by("companyName"));
        when(repository.searchPublic("engineer", pageable)).thenReturn(new PageImpl<>(java.util.List.of(), pageable, 14));
        var result = service.search("engineer", paging, false);
        assertEquals(1, result.page());
        assertEquals(10, result.size());
        assertEquals(14, result.totalElements());
        verify(repository).searchPublic("engineer", pageable);
    }
}
