package com.jobradar.externaljobs;

import java.time.Instant;

public record ExternalJobProviderStatusDto(String provider, boolean enabled, Instant lastSuccessfulSync,
        Instant lastAttemptedSync, String lastError, long activeJobs, String connectionStatus) { }
