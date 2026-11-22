package com.jobradar.opportunity;

import com.jobradar.common.api.ApiResponse;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.pagination.PaginationParameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1")
public class OpportunityController {
    private final OpportunityService opportunities;
    private final OpportunityReportService reports;
    public OpportunityController(OpportunityService opportunities, OpportunityReportService reports) {
        this.opportunities = opportunities; this.reports = reports;
    }

    @GetMapping("/opportunities")
    public ApiResponse<PaginatedResponse<OpportunityDto>> search(
            @RequestParam(required = false) @Size(max = 200) String keyword,
            @RequestParam(required = false) @Size(max = 120) String role,
            @RequestParam(required = false) @Min(1) Integer categoryId,
            @RequestParam(required = false) @Size(max = 20) List<@Size(max = 120) String> skills,
            @RequestParam(required = false) @Size(max = 160) String company,
            @RequestParam(required = false) @Size(max = 200) String location,
            @RequestParam(required = false) @Min(0) @Max(60) Integer minExperience,
            @RequestParam(required = false) @Min(0) @Max(60) Integer maxExperience,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) OpportunityType opportunityType,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minSalary,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxSalary,
            @RequestParam(required = false) @Size(max = 20) String postedWithin,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) @Pattern(regexp = "(?i)ASC|DESC") String direction) {
        PaginationParameters pagination = new PaginationParameters();
        pagination.setPage(page); pagination.setSize(size);
        if (sort != null && !sort.isBlank()) pagination.setSort(sort);
        if (direction != null && !direction.isBlank()) pagination.setDirection(direction);
        return ApiResponse.success("Opportunities retrieved successfully", opportunities.search(keyword, role, categoryId, skills,
                company, location, minExperience, maxExperience, workMode, employmentType, opportunityType,
                minSalary, maxSalary, postedWithin, pagination));
    }

    @GetMapping("/opportunities/latest")
    public ApiResponse<List<OpportunityDto>> latest() {
        return ApiResponse.success("Latest opportunities retrieved successfully", opportunities.latest());
    }

    @GetMapping("/opportunities/{id}")
    public ApiResponse<OpportunityDto> get(@PathVariable Integer id, Authentication authentication) {
        return ApiResponse.success("Opportunity retrieved successfully", opportunities.get(id, authentication));
    }

    @GetMapping("/moderation/opportunities")
    public ApiResponse<PaginatedResponse<OpportunityDto>> reviewQueue(@Valid @ModelAttribute PaginationParameters pagination) {
        return ApiResponse.success("Opportunities awaiting review retrieved successfully",
                opportunities.reviewQueue(pagination, false));
    }

    @GetMapping("/admin/opportunities")
    public ApiResponse<PaginatedResponse<OpportunityDto>> adminList(@Valid @ModelAttribute PaginationParameters pagination) {
        return ApiResponse.success("Opportunities retrieved successfully", opportunities.reviewQueue(pagination, true));
    }

    @GetMapping("/admin/opportunities/review-queue")
    public ApiResponse<PaginatedResponse<OpportunityDto>> adminReviewQueue(
            @Valid @ModelAttribute PaginationParameters pagination,
            @RequestParam(defaultValue = "PENDING") OpportunityStatus status) {
        return ApiResponse.success("Review queue retrieved successfully", opportunities.adminReviewQueue(pagination, status));
    }

    @GetMapping("/admin/opportunities/{id}")
    public ApiResponse<OpportunityService.ReviewDetails> reviewDetails(@PathVariable @Min(1) Integer id,
            Authentication administrator) {
        return ApiResponse.success("Submission retrieved successfully", opportunities.getReview(id, administrator));
    }

    @PatchMapping("/admin/opportunities/{id}/review")
    public ApiResponse<OpportunityDto> reviewSubmission(@PathVariable @Min(1) Integer id,
            @Valid @RequestBody OpportunityReviewRequest request, Authentication administrator) {
        return ApiResponse.success("Review recorded", opportunities.review(id, request.decision(), request.note(), administrator));
    }

    @PostMapping("/opportunities/{id}/review-response")
    public ApiResponse<OpportunityDto> respondToReview(@PathVariable @Min(1) Integer id,
            @Valid @RequestBody OpportunityReviewResponse request, Authentication authentication) {
        return ApiResponse.success("Details sent for review", opportunities.respondToReview(id, request.message(), authentication));
    }

    @PostMapping("/opportunities")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpportunityDto> create(@Valid @RequestBody OpportunityRequest request, Authentication authentication) {
        return ApiResponse.success("Opportunity submitted for review", opportunities.create(request, authentication.getName()));
    }

    @PutMapping("/opportunities/{id}")
    public ApiResponse<OpportunityDto> update(@PathVariable Integer id, @Valid @RequestBody OpportunityRequest request,
            Authentication authentication) {
        return ApiResponse.success("Opportunity updated successfully", opportunities.update(id, request, authentication));
    }

    @PatchMapping("/opportunities/{id}/status")
    public ApiResponse<OpportunityDto> status(@PathVariable Integer id, @Valid @RequestBody StatusRequest request,
            Authentication authentication) {
        return ApiResponse.success("Opportunity status updated successfully",
                opportunities.changeStatus(id, request.status(), authentication));
    }

    @PatchMapping("/opportunities/{id}/verification")
    public ApiResponse<OpportunityDto> verify(@PathVariable Integer id, @Valid @RequestBody VerificationRequest request,
            Authentication authentication) {
        return ApiResponse.success("Opportunity verification updated successfully",
                opportunities.verify(id, request.verified(), authentication));
    }

    @PostMapping("/opportunities/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpportunityReportDto> report(@PathVariable Integer id,
            @Valid @RequestBody OpportunityReportRequest request, Authentication authentication) {
        return ApiResponse.success("Opportunity report submitted successfully",
                reports.report(id, request, authentication.getName()));
    }

    @GetMapping("/moderation/reports")
    public ApiResponse<PaginatedResponse<OpportunityReportDto>> reports(
            @Valid @ModelAttribute PaginationParameters pagination, Authentication authentication) {
        return ApiResponse.success("Open opportunity reports retrieved successfully", reports.openReports(pagination, authentication));
    }

    @GetMapping("/moderation/reports/{reportId}")
    public ApiResponse<OpportunityReportDto> getReport(@PathVariable Integer reportId, Authentication authentication) {
        return ApiResponse.success("Opportunity report retrieved successfully", reports.get(reportId, authentication));
    }

    @PatchMapping("/moderation/reports/{reportId}")
    public ApiResponse<OpportunityReportDto> reviewReport(@PathVariable Integer reportId,
            @Valid @RequestBody ReportReviewRequest request, Authentication authentication) {
        return ApiResponse.success("Opportunity report reviewed successfully",
                reports.review(reportId, request.status(), authentication));
    }

    @PatchMapping("/moderation/opportunities/{id}/status")
    public ApiResponse<OpportunityDto> moderateOpportunityStatus(@PathVariable Integer id,
            @Valid @RequestBody StatusRequest request, Authentication authentication) {
        return ApiResponse.success("Opportunity status updated by moderation", 
                opportunities.changeStatus(id, request.status(), authentication));
    }

    @DeleteMapping("/opportunities/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id, Authentication authentication) {
        opportunities.remove(id, authentication);
        return ApiResponse.success("Opportunity removed successfully", null);
    }

    public record StatusRequest(@NotNull OpportunityStatus status) {}
    public record OpportunityReviewRequest(@NotNull OpportunityReviewDecision decision, @Size(max = 1000) String note) {}
    public record OpportunityReviewResponse(@NotBlank @Size(max = 1000) String message) {}
    public record VerificationRequest(@NotNull Boolean verified) {}
    public record ReportReviewRequest(@NotNull OpportunityReportStatus status) {}
}
