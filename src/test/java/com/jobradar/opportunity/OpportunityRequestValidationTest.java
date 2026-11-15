package com.jobradar.opportunity;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpportunityRequestValidationTest {
    @Test void rejectsInvalidRangesAndWalkInDetails() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var reversed = new OpportunityRequest("Role", 1, 1, WorkMode.REMOTE, EmploymentType.FULL_TIME,
                    8, 2, null, null, "INR", OpportunityType.JOB, "Description", null, null,
                    null, null, null, null, null, null, null, null, null, null, null, Set.of(1), Set.of());
            assertTrue(validator.validate(reversed).stream().anyMatch(error -> error.getPropertyPath().toString().equals("rangesValid")));

            var missingDrive = new OpportunityRequest("Drive", 1, 1, WorkMode.ON_SITE, EmploymentType.FULL_TIME,
                    0, 10, null, null, "INR", OpportunityType.WALK_IN, "Description", null, "https://example.com/drive",
                    null, null, null, null, null, null, null, null, null, null, null, Set.of(1), Set.of());
            assertTrue(validator.validate(missingDrive).stream().anyMatch(error -> error.getPropertyPath().toString().equals("rangesValid")));
        }
    }

    @Test void acceptsWellFormedWalkInAndRanges() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new OpportunityRequest("Drive", 1, 1, WorkMode.ON_SITE, EmploymentType.FULL_TIME,
                    0, 10, null, null, "INR", OpportunityType.WALK_IN, "Description", null, "https://example.com/drive",
                    null, java.time.LocalDate.now().plusDays(2), java.time.LocalTime.of(9, 0),
                    java.time.LocalTime.of(17, 0), "Convention Hall", "Building 2", "Asia/Kolkata",
                    "Hiring Desk", "+91 98765 43210", "jobs@example.com", "Bring ID proof",
                    Set.of(1), Set.of());
            var violations = factory.getValidator().validate(request);
            assertTrue(violations.isEmpty(), violations.toString());
        }
    }
}
