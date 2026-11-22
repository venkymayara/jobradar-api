package com.jobradar.externaljobs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "external_jobs")
public class ExternalJobEntity {
    @Id
    @Column(name = "external_id", length = 200)
    private String externalId;
    @Column(nullable = false, length = 300) private String title;
    @Column(name = "company_name", nullable = false, length = 200) private String companyName;
    @Column(name = "company_logo_url", length = 2048) private String companyLogoUrl;
    @Column(columnDefinition = "nvarchar(max)") private String description;
    @Column(length = 300) private String location;
    @Column(length = 120) private String city;
    @Column(length = 120) private String country;
    @Column(name = "employment_type", length = 80) private String employmentType;
    @Column(name = "experience_level", length = 80) private String experienceLevel;
    @Column(length = 200) private String salary;
    @Column(columnDefinition = "nvarchar(2000)") private String skills;
    @Column(name = "work_mode", length = 40) private String workMode;
    @Column(name = "job_url", length = 2048) private String jobUrl;
    @Column(name = "application_url", nullable = false, length = 2048) private String applicationUrl;
    @Column(nullable = false, length = 80) private String provider;
    @Column(name = "posted_at") private Instant postedAt;
    @Column(name = "fetched_at", nullable = false) private Instant fetchedAt;
    @Column(name = "expires_at") private LocalDate expiresAt;

    protected ExternalJobEntity() { }

    public ExternalJobEntity(ExternalJobDto dto) {
        externalId = dto.externalId(); title = dto.title(); companyName = dto.companyName(); companyLogoUrl = dto.companyLogoUrl();
        description = dto.description(); location = dto.location(); city = dto.city(); country = dto.country(); employmentType = dto.employmentType();
        experienceLevel = dto.experienceLevel(); salary = dto.salary(); skills = dto.skills() == null ? null : String.join("|", dto.skills());
        workMode = dto.workMode(); jobUrl = dto.jobUrl(); applicationUrl = dto.applicationUrl(); provider = dto.provider(); postedAt = dto.postedAt();
        fetchedAt = dto.fetchedAt() == null ? Instant.now() : dto.fetchedAt(); expiresAt = dto.expiresAt();
    }

    public ExternalJobDto toDto() {
        List<String> skillList = skills == null || skills.isBlank() ? List.of() : Arrays.stream(skills.split("\\|", -1)).filter(value -> !value.isBlank()).toList();
        return new ExternalJobDto(externalId, title, companyName, companyLogoUrl, description, location, city, country, employmentType,
                experienceLevel, salary, skillList, workMode, jobUrl, applicationUrl, provider, postedAt, fetchedAt, expiresAt, true);
    }
}
