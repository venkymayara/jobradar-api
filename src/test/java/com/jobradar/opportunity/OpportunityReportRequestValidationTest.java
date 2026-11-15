package com.jobradar.opportunity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class OpportunityReportRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test void acceptsOnlyTheSupportedCommunityReportingReasons() {
        for (String reason : Set.of("FAKE_JOB", "EXPIRED", "WRONG_INFORMATION", "DUPLICATE",
                "SUSPICIOUS_PAYMENT", "MISLEADING_SALARY", "BROKEN_URL", "INAPPROPRIATE_CONTENT")) {
            assertTrue(validator.validate(new OpportunityReportRequest(reason, null)).isEmpty(), reason);
        }
        assertFalse(validator.validate(new OpportunityReportRequest("SPAM", null)).isEmpty());
        assertFalse(validator.validate(new OpportunityReportRequest(null, null)).isEmpty());
    }
}
