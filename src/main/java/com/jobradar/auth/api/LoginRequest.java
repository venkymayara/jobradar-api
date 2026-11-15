package com.jobradar.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password is too long")
        String password) {
}
