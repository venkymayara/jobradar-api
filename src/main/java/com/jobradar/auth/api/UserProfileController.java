package com.jobradar.auth.api;

import com.jobradar.common.api.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.validation.Valid;
import com.jobradar.auth.user.UserRepository;
import com.jobradar.auth.user.UserEntity;
import com.jobradar.common.exception.ResourceNotFoundException;
import com.jobradar.opportunity.OpportunityRepository;
import com.jobradar.opportunity.OpportunityStatus;
import com.jobradar.opportunity.OpportunityReportRepository;
import com.jobradar.opportunity.OpportunityReportStatus;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.pagination.PaginationParameters;
import com.jobradar.opportunity.OpportunityDto;
import com.jobradar.opportunity.OpportunityReportDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.jobradar.saved.SavedOpportunityRepository;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {
    private final UserRepository users; private final OpportunityRepository opportunities; private final SavedOpportunityRepository saved; private final OpportunityReportRepository reports;
    public UserProfileController(UserRepository users, OpportunityRepository opportunities, SavedOpportunityRepository saved, OpportunityReportRepository reports) { this.users = users; this.opportunities = opportunities; this.saved = saved; this.reports = reports; }
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Authentication authentication) { return ApiResponse.success("Profile retrieved successfully", UserResponse.from(user(authentication))); }
    @PutMapping("/me")
    public ApiResponse<UserResponse> update(@Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
        UserEntity user = user(authentication); user.updateProfile(request.fullName(), request.phoneNumber(), request.location(), request.professionalHeadline(), request.skills(), request.experienceYears(), request.linkedinUrl(), request.githubUrl());
        return ApiResponse.success("Profile updated successfully", UserResponse.from(users.save(user)));
    }
    @GetMapping("/me/dashboard")
    public ApiResponse<DashboardSummary> dashboard(Authentication authentication) {
        Integer id = user(authentication).getId();
        return ApiResponse.success("Dashboard retrieved successfully", new DashboardSummary(opportunities.countByPublisherId(id), opportunities.countByPublisherIdAndStatus(id, OpportunityStatus.ACTIVE), opportunities.countByPublisherIdAndStatus(id, OpportunityStatus.PENDING), saved.countByIdUserId(id), reports.countByReporter_IdAndStatus(id, OpportunityReportStatus.OPEN), 0L));
    }
    @GetMapping("/me/preferences")
    public ApiResponse<UserPreferencesResponse> preferences(Authentication authentication) { return ApiResponse.success("Preferences retrieved successfully", UserPreferencesResponse.from(user(authentication))); }
    @PutMapping("/me/preferences")
    public ApiResponse<UserPreferencesResponse> updatePreferences(@Valid @RequestBody PreferencesUpdateRequest request, Authentication authentication) {
        UserEntity current = user(authentication); current.updatePreferences(request.emailNotificationsEnabled(), request.jobAlertsEnabled());
        return ApiResponse.success("Preferences updated successfully", UserPreferencesResponse.from(users.save(current)));
    }
    @GetMapping("/me/opportunities")
    public ApiResponse<PaginatedResponse<OpportunityDto>> opportunities(@Valid @ModelAttribute PaginationParameters page, Authentication authentication) {
        return ApiResponse.success("Your opportunities retrieved successfully", PaginatedResponse.from(opportunities.findByPublisherId(user(authentication).getId(), PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"))).map(item -> OpportunityDto.from(item, java.util.List.of()))));
    }
    @GetMapping("/me/reports")
    public ApiResponse<PaginatedResponse<OpportunityReportDto>> reports(@Valid @ModelAttribute PaginationParameters page, Authentication authentication) {
        return ApiResponse.success("Your reports retrieved successfully", PaginatedResponse.from(reports.findByReporter_Id(user(authentication).getId(), PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"))).map(OpportunityReportDto::from)));
    }
    private UserEntity user(Authentication a) { return users.findWithRolesByEmail(a.getName()).orElseThrow(() -> new ResourceNotFoundException("User account not found")); }
    public record DashboardSummary(long publishedOpportunityCount, long activeOpportunityCount, long pendingOpportunityCount, long savedOpportunityCount, long pendingReportCount, long totalViews) {}
    public record PreferencesUpdateRequest(boolean emailNotificationsEnabled, boolean jobAlertsEnabled) {}
}
