package com.jobradar.externaljobs;

import com.jobradar.taxonomy.category.CategoryEntity;
import com.jobradar.taxonomy.category.CategoryRepository;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
class IndiaJobsService {
    private static final Logger LOG = LoggerFactory.getLogger(IndiaJobsService.class);
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    // Three pages per city leaves room for the national and Hyderabad feeds and IT job details.
    private static final List<String> CITY_FEEDS = List.of("Bengaluru", "Chennai", "Mumbai", "Pune", "Delhi",
            "Gurugram", "Noida", "Kolkata", "Ahmedabad", "Kochi", "Bhubaneswar", "Jaipur", "Chandigarh",
            "Nagpur", "Visakhapatnam");
    private static final Map<String, String> LEVER_BOARDS = Map.of(
            "modeln", "Model N", "zeta", "Zeta", "outreach", "Outreach", "cprime", "Cprime", "cred", "CRED");
    private static final Map<String, String> GREENHOUSE_BOARDS = Map.of(
            "gitlab", "GitLab", "okta", "Okta", "twilio", "Twilio", "mongodb", "MongoDB",
            "databricks", "Databricks", "coinbase", "Coinbase");
    private static final Map<String, String> SMART_RECRUITERS_BOARDS = Map.of(
            "BoschGroup", "Bosch Group", "ServiceNow", "ServiceNow",
            "Experian", "Experian", "WesternDigital", "Western Digital");

    private final JobsForceIndiaService jobsForce;
    private final IndiaJobSources sources;
    private final CategoryRepository categories;
    private final Clock clock;
    private Instant refreshAt = Instant.EPOCH;
    private Optional<List<ExternalJobDto>> cached = Optional.empty();
    private Instant himalayasRefreshAt = Instant.EPOCH;
    private List<ExternalJobDto> cachedHimalayas = List.of();

    IndiaJobsService(JobsForceIndiaService jobsForce, RestClient.Builder builder, CategoryRepository categories, Clock clock) {
        this.jobsForce = jobsForce;
        this.categories = categories;
        this.clock = clock;
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.sources = new IndiaJobSources(builder.requestFactory(factory).build());
    }

    synchronized Optional<List<ExternalJobDto>> jobs() {
        if (!clock.instant().isBefore(refreshAt)) {
            cached = refresh(clock.instant());
            refreshAt = clock.instant().plus(cached.isPresent() ? Duration.ofMinutes(5) : Duration.ofSeconds(30));
        }
        LocalDate today = LocalDate.ofInstant(clock.instant(), INDIA);
        return cached.map(jobs -> jobs.stream()
                .filter(job -> job.expiresAt() == null || !job.expiresAt().isBefore(today))
                .toList());
    }

    Optional<Map<Integer, Long>> categoryCounts() {
        return jobs().map(jobs -> jobs.stream().filter(job -> job.categoryId() != null)
                .collect(Collectors.groupingBy(ExternalJobDto::categoryId, Collectors.counting())));
    }

    private Optional<List<ExternalJobDto>> refresh(Instant now) {
        Map<String, Integer> categoryIds = categories.findAllByActiveTrue().stream()
                .collect(Collectors.toMap(CategoryEntity::getSlug, CategoryEntity::getId));
        var feeds = new LinkedHashMap<String, Callable<List<ExternalJobDto>>>();
        feeds.put("JobsForce India (Hyderabad IT)", () -> jobsForce.hyderabadJobs()
                .orElseThrow(() -> new RestClientException("Hyderabad IT feed unavailable")));
        feeds.put("JobsForce India (Hyderabad)", () -> jobsForce.discoverJobs("Hyderabad", 10, categoryIds));
        feeds.put("JobsForce India (India)", () -> jobsForce.discoverJobs(null, 10, categoryIds));
        for (String city : CITY_FEEDS) {
            feeds.put("JobsForce India (" + city + ")", () -> jobsForce.discoverJobs(city, 3, categoryIds));
        }
        LEVER_BOARDS.forEach((slug, company) ->
                feeds.put(company, () -> sources.lever(slug, company, categoryIds, now)));
        GREENHOUSE_BOARDS.forEach((slug, company) ->
                feeds.put(company, () -> sources.greenhouse(slug, company, categoryIds, now)));
        SMART_RECRUITERS_BOARDS.forEach((slug, company) ->
                feeds.put(company, () -> sources.smartRecruiters(slug, company, categoryIds, now)));
        feeds.put("Atlan", () -> sources.ashby("atlan", "Atlan", categoryIds, now));
        feeds.put("Himalayas", () -> himalayas(categoryIds, now));
        feeds.put("We Work Remotely", () -> sources.remoteRss(categoryIds, now));

        var unique = new LinkedHashMap<String, ExternalJobDto>();
        boolean anyAvailable = false;
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var names = new ArrayList<>(feeds.keySet());
            var results = workers.invokeAll(feeds.values(), 22, TimeUnit.SECONDS);
            for (int i = 0; i < results.size(); i++) {
                var result = results.get(i);
                if (result.isCancelled()) {
                    LOG.warn("India jobs feed timed out: {}", names.get(i));
                    continue;
                }
                try {
                    for (ExternalJobDto job : result.get()) {
                        unique.putIfAbsent(IndiaJobSources.canonicalUrl(job.jobUrl()), job);
                    }
                    anyAvailable = true;
                } catch (ExecutionException ex) {
                    LOG.warn("India jobs feed unavailable: {} ({})", names.get(i), ex.getCause().getClass().getSimpleName());
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
        return anyAvailable ? Optional.of(unique.values().stream()
                .sorted(Comparator.comparing(ExternalJobDto::postedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExternalJobDto::title))
                .toList()) : Optional.empty();
    }

    private List<ExternalJobDto> himalayas(Map<String, Integer> categoryIds, Instant now) {
        if (!now.isBefore(himalayasRefreshAt)) {
            cachedHimalayas = sources.himalayas(categoryIds, now);
            himalayasRefreshAt = now.plus(Duration.ofHours(24));
        }
        return cachedHimalayas;
    }
}
