package com.jobradar.company;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;

public class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {
    @Override public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            return uri.isAbsolute() && uri.getHost() != null && uri.getUserInfo() == null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
