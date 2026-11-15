package com.jobradar.company;

import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.exception.ResourceNotFoundException;
import com.jobradar.common.pagination.PaginationParameters;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {
    private static final Set<String> SORTS = Set.of("companyName", "industry", "headquarters", "opportunityCount", "createdAt");
    private final CompanyRepository companies;

    public CompanyService(CompanyRepository companies) { this.companies = companies; }

    public PaginatedResponse<CompanyDto> search(String search, PaginationParameters pagination, boolean admin) {
        String sort = pagination.getSort().equals("createdAt") ? "companyName" : pagination.getSort();
        if (!SORTS.contains(sort)) throw new com.jobradar.common.exception.BadRequestException("Unsupported sort field: " + sort);
        Pageable pageable = PageRequest.of(pagination.getPage(), pagination.getSize(),
                Sort.by(Sort.Direction.fromString(pagination.getDirection()), sort));
        String term = search == null || search.isBlank() ? null : search.trim();
        Page<CompanyDto> result = (admin ? companies.searchAdmin(term, pageable) : companies.searchPublic(term, pageable))
                .map(CompanyDto::from);
        return PaginatedResponse.from(result);
    }

    public CompanyDto getPublic(Integer id) {
        return companies.findByIdAndActiveTrue(id).map(CompanyDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    @Transactional
    public CompanyDto submit(CompanyRequest request, String submitter) {
        ensureUnique(request.companyName(), null);
        // User submissions remain inactive and unverified until reviewed.
        return CompanyDto.from(companies.save(entity(request, false, submitter)));
    }

    @Transactional
    public CompanyDto create(CompanyRequest request) {
        ensureUnique(request.companyName(), null);
        return CompanyDto.from(companies.save(entity(request, true, null)));
    }

    @Transactional
    public CompanyDto update(Integer id, CompanyRequest request) {
        CompanyEntity company = find(id);
        ensureUnique(request.companyName(), id);
        company.update(clean(request.companyName()), clean(request.website()), clean(request.companyLogo()),
                clean(request.location()), clean(request.industry()), clean(request.description()));
        return CompanyDto.from(company);
    }

    @Transactional
    public void setActive(Integer id, boolean active) { find(id).setActive(active); }

    @Transactional
    public CompanyDto setVerification(Integer id, boolean verified, String moderator) {
        CompanyEntity company = find(id);
        company.setVerification(verified, moderator);
        return CompanyDto.from(company);
    }

    private CompanyEntity entity(CompanyRequest request, boolean active, String submitter) {
        return new CompanyEntity(clean(request.companyName()), clean(request.website()),
                clean(request.companyLogo()), clean(request.location()), clean(request.industry()),
                clean(request.description()), active, submitter);
    }

    private CompanyEntity find(Integer id) {
        return companies.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    private void ensureUnique(String name, Integer id) {
        String normalized = clean(name);
        boolean duplicate = id == null ? companies.existsByCompanyNameIgnoreCase(normalized)
                : companies.existsByCompanyNameIgnoreCaseAndIdNot(normalized, id);
        if (duplicate) throw new ConflictException("A company with this name already exists");
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
