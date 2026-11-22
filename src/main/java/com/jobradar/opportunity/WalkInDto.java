package com.jobradar.opportunity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

public record WalkInDto(Integer opportunityId, String title, Integer companyId, String companyName,
        String companyLogo, String location, List<OpportunityReferenceDto> roles,
        List<OpportunityReferenceDto> skills, int minExperience, int maxExperience,
        BigDecimal minSalary, BigDecimal maxSalary, String currencyCode,
        LocalDate driveDate, LocalTime startTime, LocalTime endTime, String timeZoneId,
        Instant driveStartAtUtc, Instant driveEndAtUtc, String venue, String venueAddress,
        String contactName, String contactPhone, String contactEmail, String instructions,
        String sourceUrl, OpportunityStatus status, boolean verified) {
    public static WalkInDto from(OpportunityEntity opportunity) {
        return from(opportunity, true);
    }

    static WalkInDto publicPreview(OpportunityEntity opportunity) {
        return from(opportunity, false);
    }

    private static WalkInDto from(OpportunityEntity opportunity, boolean includeContact) {
        WalkInDetailsEntity drive = opportunity.getWalkInDetails();
        return new WalkInDto(opportunity.getId(), opportunity.getTitle(), opportunity.getCompany().getId(),
                opportunity.getCompany().getCompanyName(), opportunity.getCompany().getLogoUrl(),
                opportunity.getLocation().getCity() + (opportunity.getLocation().getState() == null ? "" : ", " + opportunity.getLocation().getState())
                        + ", " + opportunity.getLocation().getCountry(),
                opportunity.getRoles().stream().map(role -> new OpportunityReferenceDto(role.getId(), role.getRoleName(), role.getSlug()))
                        .sorted(Comparator.comparing(OpportunityReferenceDto::name)).toList(),
                opportunity.getSkills().stream().map(skill -> new OpportunityReferenceDto(skill.getId(), skill.getName(), skill.getSlug()))
                        .sorted(Comparator.comparing(OpportunityReferenceDto::name)).toList(),
                opportunity.getMinExperience(), opportunity.getMaxExperience(), opportunity.getMinSalary(),
                opportunity.getMaxSalary(), opportunity.getCurrencyCode(), drive.getDriveDate(), drive.getStartTime(),
                drive.getEndTime(), drive.getTimeZoneId(), drive.getDriveStartAtUtc(), drive.getDriveEndAtUtc(),
                drive.getVenue(), drive.getVenueAddress(), includeContact ? drive.getContactName() : null,
                includeContact ? drive.getContactPhone() : null, includeContact ? drive.getContactEmail() : null,
                includeContact ? drive.getInstructions() : null, drive.getSourceUrl(), opportunity.getStatus(),
                opportunity.isVerified());
    }
}
