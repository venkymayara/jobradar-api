package com.jobradar.opportunity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OpportunityReportRequest(
        @NotBlank @Pattern(regexp = "FAKE_JOB|EXPIRED|WRONG_INFORMATION|DUPLICATE|SUSPICIOUS_PAYMENT|MISLEADING_SALARY|BROKEN_URL|INAPPROPRIATE_CONTENT") String reason,
        @Size(max = 500) String details) {
    public OpportunityReportReason parsedReason() { return OpportunityReportReason.valueOf(reason); }
}
