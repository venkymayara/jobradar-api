package com.jobradar.opportunity;

import com.jobradar.auth.user.UserEntity;
import com.jobradar.common.audit.AuditedEntity;
import com.jobradar.company.CompanyEntity;
import com.jobradar.taxonomy.location.LocationEntity;
import com.jobradar.taxonomy.role.RoleEntity;
import com.jobradar.taxonomy.skill.SkillEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "Opportunities", schema = "dbo")
@BatchSize(size = 32)
public class OpportunityEntity extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, length = 200, columnDefinition = "nvarchar(200)")
    private String title;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "location_id", nullable = false)
    private LocationEntity location;
    @Enumerated(EnumType.STRING) @Column(name = "work_mode", nullable = false, length = 20)
    private WorkMode workMode;
    @Enumerated(EnumType.STRING) @Column(name = "employment_type", nullable = false, length = 30)
    private EmploymentType employmentType;
    @Column(name = "min_experience", nullable = false)
    private int minExperience;
    @Column(name = "max_experience", nullable = false)
    private int maxExperience;
    @Column(name = "min_salary", precision = 15, scale = 2)
    private BigDecimal minSalary;
    @Column(name = "max_salary", precision = 15, scale = 2)
    private BigDecimal maxSalary;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Enumerated(EnumType.STRING) @Column(name = "opportunity_type", nullable = false, length = 20)
    private OpportunityType opportunityType;
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String description;
    @Column(name = "application_url", length = 2048)
    private String applicationUrl;
    @Column(name = "source_url", length = 2048)
    private String sourceUrl;
    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private OpportunityStatus status;
    @Column(name = "is_verified", nullable = false)
    private boolean verified;
    @Column(name = "published_at")
    private Instant publishedAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity publisher;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "OpportunityRoles", schema = "dbo", joinColumns = @JoinColumn(name = "opportunity_id"), inverseJoinColumns = @JoinColumn(name = "job_role_id"))
    @BatchSize(size = 32)
    private Set<RoleEntity> roles = new LinkedHashSet<>();
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "OpportunitySkills", schema = "dbo", joinColumns = @JoinColumn(name = "opportunity_id"), inverseJoinColumns = @JoinColumn(name = "skill_id"))
    @BatchSize(size = 32)
    private Set<SkillEntity> skills = new LinkedHashSet<>();
    @OneToOne(mappedBy = "opportunity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private WalkInDetailsEntity walkInDetails;
    @Version @Column(nullable = false)
    private long version;

    protected OpportunityEntity() {}

    public OpportunityEntity(OpportunityRequest request, CompanyEntity company, LocationEntity location,
            UserEntity publisher, Set<RoleEntity> roles, Set<SkillEntity> skills, OpportunityStatus status) {
        this.title = request.title().trim(); this.company = company; this.location = location;
        this.workMode = request.workMode(); this.employmentType = request.employmentType();
        this.minExperience = request.minExperience(); this.maxExperience = request.maxExperience();
        this.minSalary = request.minSalary(); this.maxSalary = request.maxSalary();
        this.currencyCode = request.currencyCode() == null ? "INR" : request.currencyCode().toUpperCase();
        this.opportunityType = request.opportunityType(); this.description = request.description().trim();
        this.applicationUrl = clean(request.applicationUrl()); this.sourceUrl = clean(request.sourceUrl());
        this.applicationDeadline = request.applicationDeadline();
        this.publisher = publisher; this.roles.addAll(roles); this.skills.addAll(skills);
        this.status = status;
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public CompanyEntity getCompany() { return company; }
    public LocationEntity getLocation() { return location; }
    public WorkMode getWorkMode() { return workMode; }
    public EmploymentType getEmploymentType() { return employmentType; }
    public int getMinExperience() { return minExperience; }
    public int getMaxExperience() { return maxExperience; }
    public BigDecimal getMinSalary() { return minSalary; }
    public BigDecimal getMaxSalary() { return maxSalary; }
    public String getCurrencyCode() { return currencyCode; }
    public OpportunityType getOpportunityType() { return opportunityType; }
    public String getDescription() { return description; }
    public String getApplicationUrl() { return applicationUrl; }
    public String getSourceUrl() { return sourceUrl; }
    public LocalDate getApplicationDeadline() { return applicationDeadline; }
    public OpportunityStatus getStatus() { return status; }
    public boolean isVerified() { return verified; }
    public Instant getPublishedAt() { return publishedAt; }
    public UserEntity getPublisher() { return publisher; }
    public Set<RoleEntity> getRoles() { return roles; }
    public Set<SkillEntity> getSkills() { return skills; }
    public WalkInDetailsEntity getWalkInDetails() { return walkInDetails; }
    public void setWalkInDetails(WalkInDetailsEntity details) { this.walkInDetails = details; }

    public void update(OpportunityRequest request, CompanyEntity nextCompany, LocationEntity nextLocation,
            Set<RoleEntity> nextRoles, Set<SkillEntity> nextSkills) {
        title = request.title().trim(); company = nextCompany; location = nextLocation;
        workMode = request.workMode(); employmentType = request.employmentType();
        minExperience = request.minExperience(); maxExperience = request.maxExperience();
        minSalary = request.minSalary(); maxSalary = request.maxSalary();
        currencyCode = request.currencyCode() == null ? "INR" : request.currencyCode().toUpperCase();
        opportunityType = request.opportunityType(); description = request.description().trim();
        applicationUrl = clean(request.applicationUrl()); sourceUrl = clean(request.sourceUrl());
        applicationDeadline = request.applicationDeadline();
        roles.clear(); roles.addAll(nextRoles); skills.clear(); skills.addAll(nextSkills);
    }

    public void transition(OpportunityStatus next) {
        status = next;
        if (next == OpportunityStatus.ACTIVE && publishedAt == null) publishedAt = Instant.now();
        if (next != OpportunityStatus.ACTIVE) verified = false;
    }
    public void setVerified(boolean verified) { this.verified = verified; }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
