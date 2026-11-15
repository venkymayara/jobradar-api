package com.jobradar.company;

import com.jobradar.common.api.ApiResponse;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.pagination.PaginationParameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1")
public class CompanyController {
    private final CompanyService companies;

    public CompanyController(CompanyService companies) { this.companies = companies; }

    @GetMapping("/companies")
    public ApiResponse<PaginatedResponse<CompanyDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String keyword,
            @Valid @ModelAttribute PaginationParameters pagination) {
        return ApiResponse.success("Companies retrieved successfully",
                companies.search(search == null ? keyword : search, pagination, false));
    }

    @GetMapping("/companies/{id}")
    public ApiResponse<CompanyDto> get(@PathVariable Integer id) {
        return ApiResponse.success("Company retrieved successfully", companies.getPublic(id));
    }

    @PostMapping("/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyDto> submit(@Valid @RequestBody CompanyRequest request, Authentication authentication) {
        return ApiResponse.success("Company submitted for review", companies.submit(request, authentication.getName()));
    }

    @GetMapping("/admin/companies")
    public ApiResponse<PaginatedResponse<CompanyDto>> adminList(
            @RequestParam(required = false) String search,
            @Valid @ModelAttribute PaginationParameters pagination) {
        return ApiResponse.success("Companies retrieved successfully", companies.search(search, pagination, true));
    }

    @PostMapping("/admin/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyDto> create(@Valid @RequestBody CompanyRequest request) {
        return ApiResponse.success("Company created successfully", companies.create(request));
    }

    @PutMapping("/admin/companies/{id}")
    public ApiResponse<CompanyDto> update(@PathVariable Integer id, @Valid @RequestBody CompanyRequest request) {
        return ApiResponse.success("Company updated successfully", companies.update(id, request));
    }

    @PatchMapping("/admin/companies/{id}/activate")
    public ApiResponse<Void> activate(@PathVariable Integer id) {
        companies.setActive(id, true);
        return ApiResponse.success("Company activated successfully", null);
    }

    @PatchMapping("/admin/companies/{id}/deactivate")
    public ApiResponse<Void> deactivate(@PathVariable Integer id) {
        companies.setActive(id, false);
        return ApiResponse.success("Company deactivated successfully", null);
    }

    @PutMapping({"/admin/companies/{id}/verification", "/moderation/companies/{id}/verification"})
    public ApiResponse<CompanyDto> verify(@PathVariable Integer id, @Valid @RequestBody VerificationRequest request,
            Authentication authentication) {
        return ApiResponse.success("Company verification updated successfully",
                companies.setVerification(id, request.verified(), authentication.getName()));
    }

    public record VerificationRequest(@NotNull Boolean verified) {}
}
