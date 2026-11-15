package com.jobradar.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonAlias;

public record RegisterRequest(
        @JsonAlias("displayName")
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])[\\x21-\\x7E]{12,72}",
                message = "Password must be 12–72 printable ASCII characters and include upper, lower, number, and symbol")
        String password) {
}
