package com.jobradar.auth.api;
import jakarta.validation.constraints.*;
public record ProfileUpdateRequest(@NotBlank @Size(max=150) String fullName, @Size(max=30) String phoneNumber, @Size(max=160) String location, @Size(max=180) String professionalHeadline, @Size(max=1000) String skills, @Min(0) @Max(60) Integer experienceYears, @Size(max=2048) @Pattern(regexp="^$|https?://.*") String linkedinUrl, @Size(max=2048) @Pattern(regexp="^$|https?://.*") String githubUrl) {}
