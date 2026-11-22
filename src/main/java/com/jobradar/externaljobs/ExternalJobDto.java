package com.jobradar.externaljobs;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExternalJobDto(String externalId, String title, String companyName, String companyLogoUrl,
        String description, String location, String city, String country, String employmentType,
        String experienceLevel, String salary, List<String> skills, String workMode,
        String jobUrl, String applicationUrl, String provider, Instant postedAt, Instant fetchedAt,
        LocalDate expiresAt, boolean external, Integer categoryId, Integer minExperience,
        Integer maxExperience, Long minMonthlySalary, Long maxMonthlySalary) {

    public ExternalJobDto(String externalId, String title, String companyName, String companyLogoUrl,
            String description, String location, String city, String country, String employmentType,
            String experienceLevel, String salary, List<String> skills, String workMode,
            String jobUrl, String applicationUrl, String provider, Instant postedAt, Instant fetchedAt,
            LocalDate expiresAt, boolean external) {
        this(externalId, title, companyName, companyLogoUrl, description, location, city, country,
                employmentType, experienceLevel, salary, skills, workMode, jobUrl, applicationUrl,
                provider, postedAt, fetchedAt, expiresAt, external, null, null, null, null, null);
    }
}
