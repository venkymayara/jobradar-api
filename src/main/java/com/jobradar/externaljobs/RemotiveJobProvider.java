package com.jobradar.externaljobs;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.external-jobs.remotive.enabled", havingValue = "true")
public class RemotiveJobProvider implements ExternalJobProvider {
    private static final String DEFAULT_ENDPOINT = "https://remotive.com/api/remote-jobs";
    private final RestClient client;
    private final String endpoint;

    public RemotiveJobProvider(RestClient.Builder builder, Environment environment) {
        this.client = builder.build(); this.endpoint = environment.getProperty("app.external-jobs.remotive.endpoint", DEFAULT_ENDPOINT);
    }

    @Override public String name() { return "REMOTIVE"; }

    @Override
    public List<ExternalJobDto> fetchJobs() {
        JsonNode root = client.get().uri(endpoint).accept(MediaType.APPLICATION_JSON).retrieve().body(JsonNode.class);
        List<ExternalJobDto> result = new ArrayList<>();
        if (root == null || !root.path("jobs").isArray()) return result;
        for (JsonNode job : root.path("jobs")) {
            Instant posted = parseInstant(job.path("publication_date").asText(null));
            result.add(new ExternalJobDto(job.path("id").asText(), job.path("title").asText(),
                    job.path("company_name").asText(), textOrNull(job.path("company_logo")),
                    textOrNull(job.path("description")), job.path("candidate_required_location").asText(),
                    null, null, job.path("job_type").asText(null), null,
                    job.path("salary").asText(null), List.of(), "REMOTE",
                    job.path("url").asText(null), job.path("url").asText(null), name(), posted,
                    Instant.now(), null, true));
        }
        return result;
    }

    private String textOrNull(JsonNode node) { return node.isMissingNode() || node.isNull() || node.asText().isBlank() ? null : node.asText(); }
    private Instant parseInstant(String value) { try { return value == null ? null : Instant.parse(value); } catch (RuntimeException ignored) { return null; } }
}
