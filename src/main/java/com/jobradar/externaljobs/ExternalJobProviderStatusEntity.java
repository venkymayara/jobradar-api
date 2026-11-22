package com.jobradar.externaljobs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "external_job_provider_status")
public class ExternalJobProviderStatusEntity {
    @Id @Column(length = 80) private String provider;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "last_successful_sync") private Instant lastSuccessfulSync;
    @Column(name = "last_attempted_sync") private Instant lastAttemptedSync;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Column(name = "active_jobs", nullable = false) private long activeJobs;
    @Column(name = "connection_status", nullable = false, length = 20) private String connectionStatus;

    protected ExternalJobProviderStatusEntity() { }
    public ExternalJobProviderStatusEntity(String provider, boolean enabled) {
        this.provider = provider; this.enabled = enabled; this.connectionStatus = "UNKNOWN";
    }
    public ExternalJobProviderStatusDto toDto() { return new ExternalJobProviderStatusDto(provider, enabled, lastSuccessfulSync, lastAttemptedSync, lastError, activeJobs, connectionStatus); }
    public void attempted(Instant value) { lastAttemptedSync = value; }
    public void succeeded(Instant value, long count) { lastSuccessfulSync = value; activeJobs = count; lastError = null; connectionStatus = "UP"; }
    public void failed(String error) { lastError = error; connectionStatus = "DOWN"; }
}
