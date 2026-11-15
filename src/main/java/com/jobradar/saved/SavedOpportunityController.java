package com.jobradar.saved;

import com.jobradar.common.api.ApiResponse;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.pagination.PaginationParameters;
import com.jobradar.opportunity.OpportunityDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1")
public class SavedOpportunityController {
    private final SavedOpportunityService saved;

    public SavedOpportunityController(SavedOpportunityService saved) { this.saved = saved; }

    @PostMapping("/opportunities/{id}/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> save(@PathVariable Integer id, Authentication authentication) {
        saved.save(id, authentication);
        return ApiResponse.success("Opportunity saved successfully", null);
    }

    @DeleteMapping("/opportunities/{id}/save")
    public ApiResponse<Void> remove(@PathVariable Integer id, Authentication authentication) {
        saved.remove(id, authentication);
        return ApiResponse.success("Saved opportunity removed successfully", null);
    }

    @GetMapping("/users/me/saved")
    public ApiResponse<PaginatedResponse<OpportunityDto>> list(
            @Valid @ModelAttribute PaginationParameters pagination, Authentication authentication) {
        return ApiResponse.success("Saved opportunities retrieved successfully", saved.list(pagination, authentication));
    }
}
