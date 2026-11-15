package com.jobradar.opportunity;

import com.jobradar.common.api.ApiResponse;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.pagination.PaginationParameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/walk-ins")
public class WalkInController {
    private final WalkInService walkIns;
    public WalkInController(WalkInService walkIns) { this.walkIns = walkIns; }

    @GetMapping
    public ApiResponse<PaginatedResponse<WalkInDto>> list(
            @RequestParam(required = false) @Size(max = 200) String keyword,
            @RequestParam(required = false) @Size(max = 200) String location,
            @RequestParam(required = false) @Size(max = 120) String role,
            @RequestParam(required = false) @Size(max = 20) List<@Size(max = 120) String> skills,
            @RequestParam(required = false) @Size(max = 160) String company,
            @RequestParam(required = false) @Min(0) @Max(60) Integer minExperience,
            @RequestParam(required = false) @Min(0) @Max(60) Integer maxExperience,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minSalary,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxSalary,
            @RequestParam(required = false) @Size(max = 20) String date,
            @Valid @ModelAttribute PaginationParameters pagination) {
        WalkInFilters filters = new WalkInFilters(keyword, location, role, skills, company,
                minExperience, maxExperience, minSalary, maxSalary, date);
        return ApiResponse.success("Walk-in drives retrieved successfully", walkIns.list(filters, pagination));
    }

    @GetMapping("/upcoming")
    public ApiResponse<PaginatedResponse<WalkInDto>> upcoming(
            @RequestParam(required = false) @Size(max = 200) String keyword,
            @RequestParam(required = false) @Size(max = 200) String location,
            @RequestParam(required = false) @Size(max = 120) String role,
            @RequestParam(required = false) @Size(max = 20) List<@Size(max = 120) String> skills,
            @RequestParam(required = false) @Size(max = 160) String company,
            @RequestParam(required = false) @Min(0) @Max(60) Integer minExperience,
            @RequestParam(required = false) @Min(0) @Max(60) Integer maxExperience,
            @Valid @ModelAttribute PaginationParameters pagination) {
        WalkInFilters filters = new WalkInFilters(keyword, location, role, skills, company,
                minExperience, maxExperience, null, null, "UPCOMING");
        return ApiResponse.success("Upcoming walk-in drives retrieved successfully", walkIns.upcoming(filters, pagination));
    }

    @GetMapping("/today")
    public ApiResponse<PaginatedResponse<WalkInDto>> today(
            @RequestParam(required = false) @Size(max = 200) String location,
            @RequestParam(required = false) @Size(max = 120) String role,
            @RequestParam(required = false) @Size(max = 20) List<@Size(max = 120) String> skills,
            @RequestParam(required = false) @Size(max = 160) String company,
            @RequestParam(required = false) @Min(0) @Max(60) Integer minExperience,
            @RequestParam(required = false) @Min(0) @Max(60) Integer maxExperience,
            @Valid @ModelAttribute PaginationParameters pagination) {
        WalkInFilters filters = new WalkInFilters(null, location, role, skills, company,
                minExperience, maxExperience, null, null, "TODAY");
        return ApiResponse.success("Today's walk-in drives retrieved successfully", walkIns.today(filters, pagination));
    }

    @GetMapping("/{id}")
    public ApiResponse<WalkInDto> get(@PathVariable Integer id) {
        return ApiResponse.success("Walk-in drive retrieved successfully", walkIns.get(id));
    }
}
