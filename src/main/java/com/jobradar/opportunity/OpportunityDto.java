package com.jobradar.opportunity;

import com.jobradar.taxonomy.role.RoleEntity;
import com.jobradar.taxonomy.skill.SkillEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record OpportunityDto(Integer opportunityId, String title, Integer companyId, String companyName,
        String companyLogo, Integer locationId, String location, WorkMode workMode,
        EmploymentType employmentType, int minExperience, int maxExperience, BigDecimal minSalary,
        BigDecimal maxSalary, String currencyCode, OpportunityType opportunityType, String description,
        String applicationUrl, String sourceUrl, LocalDate applicationDeadline, LocalDate driveDate,
        LocalTime driveStartTime, LocalTime driveEndTime, String venue, String venueAddress,
        String timeZoneId, Instant driveStartAtUtc, Instant driveEndAtUtc, String contactName,
        String contactPhone, String contactEmail, String instructions,
        Instant publishedAt, OpportunityStatus status, boolean verified,
        List<OpportunityReferenceDto> roles, List<OpportunityReferenceDto> skills,
        List<OpportunityStatusHistoryDto> statusHistory) {

    public static OpportunityDto from(OpportunityEntity entity, List<OpportunityStatusHistoryDto> history) {
        List<OpportunityReferenceDto> roles = entity.getRoles().stream()
                .map(role -> new OpportunityReferenceDto(role.getId(), role.getRoleName(), role.getSlug()))
                .sorted(java.util.Comparator.comparing(OpportunityReferenceDto::name)).toList();
        List<OpportunityReferenceDto> skills = entity.getSkills().stream()
                .map(skill -> new OpportunityReferenceDto(skill.getId(), skill.getName(), skill.getSlug()))
                .sorted(java.util.Comparator.comparing(OpportunityReferenceDto::name)).toList();
        String location = entity.getLocation().getCity()
                + (entity.getLocation().getState() == null ? "" : ", " + entity.getLocation().getState())
                + ", " + entity.getLocation().getCountry();
        WalkInDetailsEntity walkIn = entity.getWalkInDetails();
        return new OpportunityDto(entity.getId(), entity.getTitle(), entity.getCompany().getId(),
                entity.getCompany().getCompanyName(), entity.getCompany().getLogoUrl(),
                entity.getLocation().getId(), location, entity.getWorkMode(), entity.getEmploymentType(),
                entity.getMinExperience(), entity.getMaxExperience(), entity.getMinSalary(), entity.getMaxSalary(),
                entity.getCurrencyCode(), entity.getOpportunityType(), entity.getDescription(),
                entity.getApplicationUrl(), walkIn == null ? entity.getSourceUrl() : walkIn.getSourceUrl(), entity.getApplicationDeadline(),
                walkIn == null ? null : walkIn.getDriveDate(), walkIn == null ? null : walkIn.getStartTime(),
                walkIn == null ? null : walkIn.getEndTime(), walkIn == null ? null : walkIn.getVenue(),
                walkIn == null ? null : walkIn.getVenueAddress(), walkIn == null ? null : walkIn.getTimeZoneId(),
                walkIn == null ? null : walkIn.getDriveStartAtUtc(), walkIn == null ? null : walkIn.getDriveEndAtUtc(),
                walkIn == null ? null : walkIn.getContactName(), walkIn == null ? null : walkIn.getContactPhone(),
                walkIn == null ? null : walkIn.getContactEmail(), walkIn == null ? null : walkIn.getInstructions(),
                entity.getPublishedAt(), entity.getStatus(), entity.isVerified(),
                roles, skills, history);
    }
}
