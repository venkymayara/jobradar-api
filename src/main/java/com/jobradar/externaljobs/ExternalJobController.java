package com.jobradar.externaljobs;

import com.jobradar.common.api.ApiResponse;
import com.jobradar.common.api.PaginatedResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/external-jobs")
public class ExternalJobController {
    private final ExternalJobService jobs;
    private final JobsForceIndiaService jobsForceIndia;
    private final IndiaJobsService india;

    public ExternalJobController(ExternalJobService jobs, JobsForceIndiaService jobsForceIndia, IndiaJobsService india) {
        this.jobs = jobs;
        this.jobsForceIndia = jobsForceIndia;
        this.india = india;
    }

    @GetMapping("/india")
    public ResponseEntity<ApiResponse<List<ExternalJobDto>>> indiaJobs() {
        return indiaResponse(india.jobs());
    }

    @GetMapping("/india/category-counts")
    public ResponseEntity<ApiResponse<Map<Integer, Long>>> indiaJobCategoryCounts() {
        return indiaResponse(india.categoryCounts());
    }

    private static <T> ResponseEntity<ApiResponse<T>> indiaResponse(Optional<T> data) {
        return data.map(value -> ResponseEntity.ok(ApiResponse.success("India jobs retrieved", value)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.failure("India jobs are temporarily unavailable", null)));
    }

    @GetMapping("/hyderabad")
    public ResponseEntity<ApiResponse<List<ExternalJobDto>>> hyderabadJobs() {
        return jobsForceIndia.hyderabadJobs()
                .map(listings -> ResponseEntity.ok(ApiResponse.success("Hyderabad IT jobs retrieved", listings)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.failure("Hyderabad IT jobs are temporarily unavailable", null)));
    }
    @GetMapping public ApiResponse<PaginatedResponse<ExternalJobDto>> search(@RequestParam(required = false) @Size(max = 200) String keyword,
            @RequestParam(required = false) @Size(max = 160) String company, @RequestParam(required = false) @Size(max = 200) String location,
            @RequestParam(required = false) @Size(max = 20) String workMode, @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return ApiResponse.success("External jobs retrieved successfully", jobs.search(keyword, company, location, workMode, page, size)); }
    @GetMapping("/providers") public ApiResponse<List<ExternalJobProviderStatusDto>> providers() { return ApiResponse.success("External job providers retrieved successfully", jobs.providers()); }
    @GetMapping("/{externalId}") public ApiResponse<ExternalJobDto> get(@PathVariable String externalId) { return ApiResponse.success("External job retrieved successfully", jobs.get(externalId)); }
}
