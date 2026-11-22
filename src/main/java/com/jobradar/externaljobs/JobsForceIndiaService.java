package com.jobradar.externaljobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobradar.taxonomy.category.CategoryEntity;
import com.jobradar.taxonomy.category.CategoryRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
class JobsForceIndiaService {
    private static final Logger LOG = LoggerFactory.getLogger(JobsForceIndiaService.class);
    private static final String PROVIDER = "JobsForce India";
    private static final String ENDPOINT = "https://jobsforceindia.com/api/public/v1/jobs";
    private static final int MAX_PAGES = 10;
    // The India refresh also requests national and city search pages under the same rate limit.
    private static final int MAX_DETAIL_REQUESTS = 40;
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(30);

    private final RestClient client;
    private final Clock clock;
    private final CategoryRepository categories;
    private Instant refreshAt = Instant.EPOCH;
    private Optional<List<ExternalJobDto>> cachedJobs = Optional.empty();

    @Autowired
    JobsForceIndiaService(RestClient.Builder builder, Clock clock, CategoryRepository categories) {
        this(configureClient(builder), clock, categories);
    }

    JobsForceIndiaService(RestClient client, Clock clock, CategoryRepository categories) {
        this.client = client;
        this.clock = clock;
        this.categories = categories;
    }

    synchronized Optional<List<ExternalJobDto>> hyderabadJobs() {
        Instant now = clock.instant();
        if (!now.isBefore(refreshAt)) {
            try {
                Map<String, Integer> categoryIds = categories.findAllByActiveTrue().stream()
                        .collect(Collectors.toMap(CategoryEntity::getSlug, CategoryEntity::getId));
                cachedJobs = Optional.of(fetchJobs(now, "Hyderabad", true, MAX_PAGES, categoryIds));
                refreshAt = clock.instant().plus(CACHE_TTL);
            } catch (RestClientException ex) {
                LOG.warn("JobsForce India listings unavailable ({})", ex.getClass().getSimpleName());
                cachedJobs = Optional.empty();
                refreshAt = clock.instant().plus(RETRY_DELAY);
            }
        }
        LocalDate today = LocalDate.ofInstant(clock.instant(), INDIA);
        return cachedJobs.map(jobs -> jobs.stream()
                .filter(job -> job.expiresAt() == null || !job.expiresAt().isBefore(today))
                .toList());
    }

    List<ExternalJobDto> discoverJobs(String city, int pageLimit, Map<String, Integer> categoryIds) {
        return fetchJobs(clock.instant(), city, false, pageLimit, categoryIds);
    }

    private List<ExternalJobDto> fetchJobs(Instant fetchedAt, String city, boolean enrichItJobs,
            int pageLimit, Map<String, Integer> categoryIds) {
        Map<String, JsonNode> listings = new LinkedHashMap<>();
        LocalDate today = LocalDate.ofInstant(fetchedAt, INDIA);
        String filters = (city == null ? "" : "city={city}&") + (enrichItJobs ? "category=IT&" : "");
        for (int page = 1; page <= pageLimit; page++) {
            Map<String, Object> parameters = city == null ? Map.of("page", page) : Map.of("page", page, "city", city);
            JsonNode response = client.get()
                    .uri(ENDPOINT + "?" + filters + "per_page=20&page={page}", parameters)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("results").isArray()) {
                throw new RestClientException("JobsForce India response is missing results");
            }
            for (JsonNode item : response.path("results")) {
                String id = item.path("id").asText("");
                LocalDate expiration = parseDate(item.path("valid_through"));
                if (isIndiaJob(item, id, city) && (expiration == null || !expiration.isBefore(today))) {
                    listings.putIfAbsent(id, item);
                }
            }
            if (!response.path("has_more").asBoolean()) break;
        }
        if (listings.isEmpty()) return List.of();

        Map<String, List<String>> details = enrichItJobs
                ? fetchSkills(listings.keySet().stream().limit(MAX_DETAIL_REQUESTS).toList()) : Map.of();
        List<ExternalJobDto> jobs = new ArrayList<>();
        for (var listing : listings.entrySet()) {
            String id = listing.getKey();
            jobs.add(toJob(id, listing.getValue(), details.getOrDefault(id, List.of()), categoryIds, fetchedAt, enrichItJobs));
        }
        return List.copyOf(jobs);
    }

    private Map<String, List<String>> fetchSkills(List<String> ids) {
        List<Callable<List<String>>> tasks = ids.stream()
                .<Callable<List<String>>>map(id -> () -> fetchJobSkills(id)).toList();
        Map<String, List<String>> byId = new LinkedHashMap<>();
        try (var workers = Executors.newFixedThreadPool(16)) {
            var results = workers.invokeAll(tasks, 8, TimeUnit.SECONDS);
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).isCancelled()) byId.put(ids.get(i), results.get(i).get());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RestClientException("JobsForce India refresh interrupted", ex);
        } catch (ExecutionException ex) {
            LOG.debug("Some JobsForce India listing details were unavailable");
        }
        return byId;
    }

    private List<String> fetchJobSkills(String id) {
        try {
            JsonNode details = client.get().uri(ENDPOINT + "/{id}", id)
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(JsonNode.class);
            if (details == null) return List.of();
            var skills = new LinkedHashSet<String>();
            for (String field : List.of("skills", "skills_preferred")) {
                for (JsonNode skill : details.path(field)) {
                    if (skill.isTextual() && !skill.asText().isBlank()) skills.add(skill.asText().trim());
                }
            }
            return List.copyOf(skills);
        } catch (RestClientException ex) {
            return List.of();
        }
    }

    private static boolean isIndiaJob(JsonNode result, String id, String city) {
        JsonNode location = result.path("location");
        return id.matches("[A-Za-z0-9_-]+") && !result.path("title").asText("").isBlank()
                && providerUrl(result.path("url")) != null
                && (city == null || city.equalsIgnoreCase(location.path("city").asText()))
                && "India".equalsIgnoreCase(location.path("country").asText());
    }

    private ExternalJobDto toJob(String id, JsonNode result, List<String> skills,
            Map<String, Integer> categoryIds, Instant fetchedAt, boolean enrichItJobs) {
        String title = result.path("title").asText();
        String summary = result.path("summary").asText(null);
        boolean technology = enrichItJobs || "IT".equalsIgnoreCase(result.path("category").asText());
        String category = ExternalJobRoleMatcher.category(title, skills, summary, technology);
        Integer categoryId = category == null ? null : categoryIds.get(category);
        String jobUrl = providerUrl(result.path("url"));
        String applicationUrl = providerUrl(result.path("apply_url"));
        JsonNode location = result.path("location");
        String city = location.path("city").asText("");
        String country = location.path("country").asText("");
        String displayLocation = java.util.stream.Stream.of(city, location.path("state").asText(""), country)
                .filter(value -> !value.isBlank()).collect(Collectors.joining(", "));
        JsonNode hiring = result.path("hiring");
        String company = "agency".equals(hiring.path("type").asText())
                ? PROVIDER + " (recruiting for a confidential client)"
                : hiring.path("display").asText(PROVIDER);
        String workMode = switch (result.path("work_mode").asText("").toLowerCase(Locale.ROOT)) {
            case "onsite" -> "ON_SITE";
            case "hybrid" -> "HYBRID";
            case "remote" -> "REMOTE";
            default -> null;
        };
        LocalDate postedDate = parseDate(result.path("posted_date"));
        JsonNode experience = result.path("experience");
        JsonNode salary = result.path("salary");
        boolean monthlyInr = "monthly".equals(salary.path("period").asText())
                && "INR".equals(salary.path("currency").asText());
        return new ExternalJobDto("JOBSFORCE_INDIA:" + id, title, company, null,
                summary, displayLocation, city, country,
                result.path("job_type").asText(null), experience.path("label").asText(null),
                salary.path("label").asText(null), skills, workMode,
                jobUrl, applicationUrl == null ? jobUrl : applicationUrl, PROVIDER,
                postedDate == null ? null : postedDate.atStartOfDay(INDIA).toInstant(),
                fetchedAt, parseDate(result.path("valid_through")), true, categoryId,
                experience.path("min_years").isIntegralNumber() ? experience.path("min_years").intValue() : null,
                experience.path("max_years").isIntegralNumber() ? experience.path("max_years").intValue() : null,
                monthlyInr && salary.path("min").isIntegralNumber() ? salary.path("min").longValue() : null,
                monthlyInr && salary.path("max").isIntegralNumber() ? salary.path("max").longValue() : null);
    }

    private static String providerUrl(JsonNode value) {
        try {
            URI uri = URI.create(value.asText(""));
            if ("https".equalsIgnoreCase(uri.getScheme()) && "jobsforceindia.com".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)) {
                return uri.toString();
            }
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed listing links.
        }
        return null;
    }

    private static LocalDate parseDate(JsonNode value) {
        try {
            return value.isTextual() ? LocalDate.parse(value.asText()) : null;
        } catch (java.time.format.DateTimeParseException ignored) {
            return null;
        }
    }

    private static RestClient configureClient(RestClient.Builder builder) {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var requestFactory = new JdkClientHttpRequestFactory(http);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder.requestFactory(requestFactory).build();
    }
}
