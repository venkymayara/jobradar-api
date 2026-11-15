package com.jobradar.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank @Size(max = 160) String companyName,
        @HttpUrl @Size(max = 2048) String website,
        @HttpUrl @Size(max = 2048) String companyLogo,
        @Size(max = 180) String location,
        @Size(max = 120) String industry,
        @Size(max = 2000) String description) {}
