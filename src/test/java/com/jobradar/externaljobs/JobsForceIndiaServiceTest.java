package com.jobradar.externaljobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jobradar.taxonomy.category.CategoryEntity;
import com.jobradar.taxonomy.category.CategoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class JobsForceIndiaServiceTest {
    private static final String ENDPOINT = "https://jobsforceindia.com/api/public/v1/jobs";
    private static final String PAGE = ENDPOINT + "?city=Hyderabad&category=IT&per_page=20&page=";
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer upstream = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-22T20:00:00Z"), ZoneOffset.UTC);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final JobsForceIndiaService service = new JobsForceIndiaService(builder.build(), clock, categories);

    @Test
    void readsAllPagesAndMapsRolesFromTitlesAndSkills() {
        CategoryEntity backend = category(2, "backend-development");
        CategoryEntity frontend = category(3, "frontend-development");
        when(categories.findAllByActiveTrue()).thenReturn(List.of(backend, frontend));
        upstream.expect(requestTo(PAGE + "1")).andRespond(withSuccess("""
                {"has_more":true,"results":[
                  {"id":"java","title":"Java Developer","hiring":{"type":"agency","display":"Private name"},
                   "location":{"city":"Hyderabad","state":"Telangana","country":"India"},
                   "work_mode":"onsite","valid_through":"2026-11-23",
                   "url":"https://jobsforceindia.com/jobs/java",
                   "apply_url":"https://jobsforceindia.com/apply/java?source=api"},
                  {"id":"closed","title":"Closed job","location":{"city":"Hyderabad","country":"India"},
                   "valid_through":"2026-11-22","url":"https://jobsforceindia.com/jobs/closed"},
                  {"id":"out-of-area","title":"Pune job","location":{"city":"Pune","country":"India"},
                   "url":"https://jobsforceindia.com/jobs/out-of-area"}
                ]}
                """, MediaType.APPLICATION_JSON));
        upstream.expect(requestTo(PAGE + "2")).andRespond(withSuccess("""
                {"has_more":false,"results":[
                  {"id":"software","title":"Software Developer","location":{"city":"Hyderabad","country":"India"},
                   "url":"https://jobsforceindia.com/jobs/software"},
                  {"id":"java","title":"Java Developer","location":{"city":"Hyderabad","country":"India"},
                   "url":"https://jobsforceindia.com/jobs/java"}
                ]}
                """, MediaType.APPLICATION_JSON));
        upstream.expect(requestTo(ENDPOINT + "/java"))
                .andRespond(withSuccess("{\"skills\":[\"React\"]}", MediaType.APPLICATION_JSON));
        upstream.expect(requestTo(ENDPOINT + "/software"))
                .andRespond(withSuccess("{\"skills\":[\"Angular\"],\"skills_preferred\":[\"TypeScript\"]}", MediaType.APPLICATION_JSON));

        var jobs = service.hyderabadJobs().orElseThrow();
        assertThat(jobs).hasSize(2).first().satisfies(job -> {
            assertThat(job.externalId()).isEqualTo("JOBSFORCE_INDIA:java");
            assertThat(job.companyName()).isEqualTo("JobsForce India (recruiting for a confidential client)");
            assertThat(job.location()).isEqualTo("Hyderabad, Telangana, India");
            assertThat(job.workMode()).isEqualTo("ON_SITE");
            assertThat(job.categoryId()).isEqualTo(2);
            assertThat(job.applicationUrl()).isEqualTo("https://jobsforceindia.com/apply/java?source=api");
        });
        assertThat(jobs.get(1).categoryId()).isEqualTo(3);
        assertThat(jobs.get(1).skills()).containsExactly("Angular", "TypeScript");
        assertThat(service.hyderabadJobs()).contains(jobs);
        upstream.verify();
    }

    @Test
    void acceptsAnEmptyBoard() {
        upstream.expect(requestTo(PAGE + "1")).andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        assertThat(service.hyderabadJobs()).contains(List.of());
        upstream.verify();
    }

    @Test
    void backsOffWhenTheProviderIsUnavailable() {
        upstream.expect(requestTo(PAGE + "1")).andRespond(withServiceUnavailable());

        assertThat(service.hyderabadJobs()).isEmpty();
        assertThat(service.hyderabadJobs()).isEmpty();
        upstream.verify();
    }

    private static CategoryEntity category(int id, String slug) {
        var category = mock(CategoryEntity.class);
        when(category.getId()).thenReturn(id);
        when(category.getSlug()).thenReturn(slug);
        return category;
    }
}
