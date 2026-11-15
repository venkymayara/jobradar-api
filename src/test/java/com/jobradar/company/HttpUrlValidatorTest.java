package com.jobradar.company;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;

class HttpUrlValidatorTest {
    private final HttpUrlValidator validator = new HttpUrlValidator();

    @Test void allowsAbsoluteHttpAndHttpsUrls() {
        assertTrue(validator.isValid("https://jobs.example.com/path", null));
        assertTrue(validator.isValid("http://example.com", null));
        assertTrue(validator.isValid(null, null));
    }

    @Test void rejectsNonHttpUrlsCredentialsAndRelativePaths() {
        assertFalse(validator.isValid("javascript:alert(1)", null));
        assertFalse(validator.isValid("https://user:secret@example.com", null));
        assertFalse(validator.isValid("/company/logo.png", null));
        assertFalse(validator.isValid("https://", null));
    }

    @Test void requestValidationRejectsInvalidWebsiteScheme() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new CompanyRequest("Example Co", "javascript:alert(1)", null, null, null, null);
            assertTrue(factory.getValidator().validate(request).stream()
                    .anyMatch(error -> error.getPropertyPath().toString().equals("website")));
        }
    }
}
