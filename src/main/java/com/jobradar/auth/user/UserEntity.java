package com.jobradar.auth.user;

import java.util.HashSet;
import java.util.Set;

import com.jobradar.auth.role.AppRoleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", length = 150, columnDefinition = "nvarchar(150)")
    private String fullName;
    @Column(name = "phone_number", length = 30, columnDefinition = "nvarchar(30)") private String phoneNumber;
    @Column(name = "location", length = 160, columnDefinition = "nvarchar(160)") private String location;
    @Column(name = "professional_headline", length = 180, columnDefinition = "nvarchar(180)") private String professionalHeadline;
    @Column(name = "skills", length = 1000, columnDefinition = "nvarchar(1000)") private String skills;
    @Column(name = "experience_years") private Integer experienceYears;
    @Column(name = "linkedin_url", length = 2048) private String linkedinUrl;
    @Column(name = "github_url", length = 2048) private String githubUrl;
    @Column(name = "avatar_url", length = 2048) private String avatarUrl;
    @Column(name = "email_notifications_enabled", nullable = false) private boolean emailNotificationsEnabled = true;
    @Column(name = "job_alerts_enabled", nullable = false) private boolean jobAlertsEnabled = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "username", referencedColumnName = "username"),
            inverseJoinColumns = @JoinColumn(name = "role", referencedColumnName = "role"))
    private Set<AppRoleEntity> roles = new HashSet<>();

    protected UserEntity() {
    }

    public UserEntity(String fullName, String email, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Integer getId() {
        return id;
    }

    void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }
    public String getPhoneNumber() { return phoneNumber; } public String getLocation() { return location; }
    public String getProfessionalHeadline() { return professionalHeadline; } public String getSkills() { return skills; }
    public Integer getExperienceYears() { return experienceYears; } public String getLinkedinUrl() { return linkedinUrl; }
    public String getGithubUrl() { return githubUrl; } public String getAvatarUrl() { return avatarUrl; }
    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public boolean isJobAlertsEnabled() { return jobAlertsEnabled; }
    public void updatePreferences(boolean emailNotificationsEnabled, boolean jobAlertsEnabled) { this.emailNotificationsEnabled = emailNotificationsEnabled; this.jobAlertsEnabled = jobAlertsEnabled; }
    public void updateProfile(String fullName, String phoneNumber, String location, String headline, String skills, Integer experienceYears, String linkedinUrl, String githubUrl) {
        this.fullName = fullName.strip(); this.phoneNumber = clean(phoneNumber); this.location = clean(location); this.professionalHeadline = clean(headline); this.skills = clean(skills); this.experienceYears = experienceYears; this.linkedinUrl = clean(linkedinUrl); this.githubUrl = clean(githubUrl);
    }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.strip(); }

    public Set<AppRoleEntity> getRoles() {
        return roles;
    }
}
