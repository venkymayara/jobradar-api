package com.jobradar.company;

import com.jobradar.common.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "companies")
public class CompanyEntity extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "company_name", nullable = false, length = 160, columnDefinition = "nvarchar(160)")
    private String companyName;
    @Column(name = "website_url", length = 2048)
    private String websiteUrl;
    @Column(name = "logo_url", length = 2048)
    private String logoUrl;
    @Column(length = 180, columnDefinition = "nvarchar(180)")
    private String headquarters;
    @Column(length = 120, columnDefinition = "nvarchar(120)")
    private String industry;
    @Column(length = 2000, columnDefinition = "nvarchar(2000)")
    private String description;
    @Column(name = "opportunity_count", nullable = false)
    private int opportunityCount;
    @Column(name = "is_active", nullable = false)
    private boolean active;
    @Column(name = "is_verified", nullable = false)
    private boolean verified;
    @Column(name = "submitted_by", length = 100, columnDefinition = "nvarchar(100)")
    private String submittedBy;
    @Column(name = "verified_by", length = 100, columnDefinition = "nvarchar(100)")
    private String verifiedBy;
    @Column(name = "verified_at")
    private Instant verifiedAt;
    @Version @Column(nullable = false)
    private long version;

    protected CompanyEntity() {}

    public CompanyEntity(String companyName, String websiteUrl, String logoUrl, String headquarters,
            String industry, String description, boolean active, String submittedBy) {
        this.companyName = companyName;
        this.websiteUrl = websiteUrl;
        this.logoUrl = logoUrl;
        this.headquarters = headquarters;
        this.industry = industry;
        this.description = description;
        this.active = active;
        this.submittedBy = submittedBy;
    }

    public Integer getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getWebsiteUrl() { return websiteUrl; }
    public String getLogoUrl() { return logoUrl; }
    public String getHeadquarters() { return headquarters; }
    public String getIndustry() { return industry; }
    public String getDescription() { return description; }
    public int getOpportunityCount() { return opportunityCount; }
    public boolean isActive() { return active; }
    public boolean isVerified() { return verified; }
    public String getSubmittedBy() { return submittedBy; }
    public String getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }

    public void update(String name, String website, String logo, String location, String industry, String description) {
        companyName = name; websiteUrl = website; logoUrl = logo; headquarters = location;
        this.industry = industry; this.description = description;
    }
    public void setActive(boolean active) { this.active = active; }
    public void setVerification(boolean verified, String administrator) {
        this.verified = verified;
        this.verifiedBy = verified ? administrator : null;
        this.verifiedAt = verified ? Instant.now() : null;
    }
}
