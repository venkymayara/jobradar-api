package com.jobradar.externaljobs;

import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ExternalJobService {
    private static final Logger log = LoggerFactory.getLogger(ExternalJobService.class);
    private final List<ExternalJobProvider> providers;
    private final ExternalJobRepository repository;
    private final ExternalJobProviderStatusRepository statusRepository;
    private final Environment environment;

    public ExternalJobService(List<ExternalJobProvider> providers, ExternalJobRepository repository,
            ExternalJobProviderStatusRepository statusRepository, Environment environment) {
        this.providers = providers; this.repository = repository; this.statusRepository = statusRepository; this.environment = environment;
    }

    @Scheduled(initialDelayString = "${app.external-jobs.initial-delay-ms:60000}", fixedDelayString = "${app.external-jobs.refresh-interval-ms:21600000}")
    public void scheduledRefresh() { refresh(); }

    public synchronized void refresh() {
        List<ExternalJobDto> jobs = new ArrayList<>();
        for (ExternalJobProvider provider : providers) {
            ExternalJobProviderStatusEntity status = status(provider.name(), true);
            status.attempted(Instant.now()); statusRepository.save(status);
            try { jobs.addAll(provider.fetchJobs()); status.succeeded(Instant.now(), 0); }
            catch (RuntimeException exception) { status.failed(sanitize(exception)); log.warn("External job provider {} failed: {}", provider.name(), exception.getMessage()); }
            statusRepository.save(status);
        }
        List<ExternalJobEntity> valid = jobs.stream().filter(job -> job.externalId() != null && job.applicationUrl() != null).map(ExternalJobEntity::new).toList();
        valid.forEach(repository::save);
        for (ExternalJobProvider provider : providers) {
            statusRepository.findById(provider.name()).ifPresent(status -> {
                status.succeeded(Instant.now(), repository.countByProvider(provider.name()));
                statusRepository.save(status);
            });
        }
    }

    public PaginatedResponse<ExternalJobDto> search(String keyword, String company, String location, String workMode, int page, int size) {
        return PaginatedResponse.from(repository.search(clean(keyword), clean(company), clean(location),
                clean(workMode), PageRequest.of(page, size)).map(ExternalJobEntity::toDto));
    }

    public List<ExternalJobProviderStatusDto> providers() {
        Map<String, ExternalJobProviderStatusDto> statuses = new HashMap<>();
        statusRepository.findAll().forEach(status -> statuses.put(status.toDto().provider(), status.toDto()));
        boolean remotiveEnabled = environment.getProperty("app.external-jobs.remotive.enabled", Boolean.class, false);
        statuses.putIfAbsent("REMOTIVE", status("REMOTIVE", remotiveEnabled).toDto());
        return statuses.values().stream().sorted(Comparator.comparing(ExternalJobProviderStatusDto::provider)).toList();
    }
    public ExternalJobDto get(String externalId) { return repository.findById(externalId).map(ExternalJobEntity::toDto).orElseThrow(() -> new ResourceNotFoundException("External job not found")); }
    private String clean(String value) { return value == null || value.isBlank() ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private ExternalJobProviderStatusEntity status(String provider, boolean enabled) { return statusRepository.findById(provider).orElseGet(() -> new ExternalJobProviderStatusEntity(provider, enabled)); }
    private String sanitize(RuntimeException exception) { String message = exception.getMessage(); return message == null ? "Provider request failed" : message.replaceAll("(?i)(token|secret|password|authorization)[^,; ]*", "$1=[redacted]").substring(0, Math.min(1000, message.length())); }
}
