package com.jobradar.company;

import java.time.Instant;

public record CompanyDto(Integer companyId, String companyName, String companyLogo, String website,
        String location, String industry, String description, int opportunityCount,
        boolean verified, boolean active, Instant verifiedAt, Instant createdAt, Instant updatedAt) {
    static CompanyDto from(CompanyEntity company) {
        return new CompanyDto(company.getId(), company.getCompanyName(), company.getLogoUrl(),
                company.getWebsiteUrl(), company.getHeadquarters(), company.getIndustry(),
                company.getDescription(), company.getOpportunityCount(), company.isVerified(),
                company.isActive(), company.getVerifiedAt(), company.getCreatedAt(), company.getUpdatedAt());
    }
}
