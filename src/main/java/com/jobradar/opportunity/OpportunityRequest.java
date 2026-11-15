package com.jobradar.opportunity;

import com.jobradar.company.HttpUrl;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Email;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record OpportunityRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull Integer companyId,
        @NotNull Integer locationId,
        @NotNull WorkMode workMode,
        @NotNull EmploymentType employmentType,
        @Min(0) @Max(60) int minExperience,
        @Min(0) @Max(60) int maxExperience,
        @DecimalMin("0.00") BigDecimal minSalary,
        @DecimalMin("0.00") BigDecimal maxSalary,
        @Pattern(regexp = "(?i)[A-Z]{3}") String currencyCode,
        @NotNull OpportunityType opportunityType,
        @NotBlank @Size(max = 10000) String description,
        @HttpUrl @Size(max = 2048) String applicationUrl,
        @HttpUrl @Size(max = 2048) String sourceUrl,
        LocalDate applicationDeadline,
        LocalDate driveDate,
        LocalTime driveStartTime,
        LocalTime driveEndTime,
        @Size(max = 180) String venue,
        @Size(max = 500) String venueAddress,
        @Size(max = 64) String timeZoneId,
        @Size(max = 120) String contactName,
        @Pattern(regexp = "[+()0-9 .-]{7,32}") String contactPhone,
        @Email @Size(max = 254) String contactEmail,
        @Size(max = 2000) String instructions,
        @NotEmpty Set<@NotNull Integer> roleIds,
        Set<@NotNull Integer> skillIds) {

    @AssertTrue(message = "Experience and salary ranges must be ordered; walk-in drives require valid date, time, and venue")
    public boolean isRangesValid() {
        boolean experience = maxExperience >= minExperience;
        boolean salary = minSalary == null || maxSalary == null || maxSalary.compareTo(minSalary) >= 0;
        boolean walkIn = opportunityType != null && opportunityType != OpportunityType.WALK_IN
                || driveDate != null && driveStartTime != null && driveEndTime != null
                && driveEndTime.isAfter(driveStartTime) && venue != null && !venue.isBlank()
                && timeZoneId != null && !timeZoneId.isBlank() && sourceUrl != null && !sourceUrl.isBlank()
                && (contactPhone != null && !contactPhone.isBlank() || contactEmail != null && !contactEmail.isBlank())
                && validZone(timeZoneId);
        return opportunityType != null && experience && salary && walkIn;
    }

    private boolean validZone(String value) {
        try {
            var zone = java.time.ZoneId.of(value);
            var local = java.time.LocalDateTime.of(driveDate, driveStartTime.withNano(0));
            return !zone.getRules().getValidOffsets(local).isEmpty();
        } catch (RuntimeException exception) { return false; }
    }
}
