package com.jobradar.externaljobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class IndiaJobSourcesTest {
    private static final Instant NOW = Instant.parse("2026-11-22T13:00:00Z");
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer upstream = MockRestServiceServer.bindTo(builder).build();
    private final IndiaJobSources sources = new IndiaJobSources(builder.build());

    @Test
    void readsEmployerBoardsAndKeepsOnlyIndianLocations() {
        upstream.expect(requestTo("https://boards-api.greenhouse.io/v1/boards/test/jobs"))
                .andRespond(withSuccess("""
                        {"jobs":[
                          {"title":"Backend Engineer","location":{"name":"Hyderabad, India; New York, USA"},
                           "absolute_url":"https://job-boards.greenhouse.io/test/jobs/1?utm_source=test"},
                          {"title":"Engineer","location":{"name":"Indianapolis, Indiana"},
                           "absolute_url":"https://job-boards.greenhouse.io/test/jobs/2"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(sources.greenhouse("test", "Test Employer", Map.of("backend-development", 2), NOW))
                .singleElement().satisfies(job -> {
                    assertThat(job.location()).isEqualTo("Hyderabad, India");
                    assertThat(job.categoryId()).isEqualTo(2);
                    assertThat(job.companyName()).isEqualTo("Test Employer");
                    assertThat(job.externalId()).isEqualTo("job-boards.greenhouse.io/test/jobs/1");
                });
        upstream.verify();
    }

    @Test
    void readsEveryPageOfIndianSmartRecruitersOpenings() {
        String url = "https://api.smartrecruiters.com/v1/companies/Test/postings?country=in&limit=100&offset=";
        upstream.expect(requestTo(url + "0")).andRespond(withSuccess("""
                {"totalFound":2,"content":[{"id":"123","name":"Backend Engineer",
                  "location":{"city":"Hyderabad","country":"in","hybrid":true}}]}
                """, MediaType.APPLICATION_JSON));
        upstream.expect(requestTo(url + "1")).andRespond(withSuccess("""
                {"totalFound":2,"content":[{"id":"456","name":"QA Engineer",
                  "location":{"city":"Bengaluru","country":"IN"}}]}
                """, MediaType.APPLICATION_JSON));

        var jobs = sources.smartRecruiters("Test", "Test Employer", Map.of("backend-development", 2), NOW);
        assertThat(jobs).extracting(ExternalJobDto::location).containsExactly("Hyderabad, India", "Bengaluru, India");
        assertThat(jobs.getFirst().applicationUrl()).isEqualTo("https://jobs.smartrecruiters.com/Test/123");
        assertThat(jobs.getFirst().categoryId()).isEqualTo(2);
        assertThat(jobs.getFirst().workMode()).isEqualTo("HYBRID");
        upstream.verify();
    }

    @Test
    void readsRssWithOriginalAttributionAndExcludesRestrictedRoles() {
        upstream.expect(requestTo("https://weworkremotely.com/remote-jobs.rss"))
                .andRespond(withSuccess("""
                        <rss version="2.0"><channel>
                          <item><title>Example: QA Engineer</title><region>Anywhere in the World</region>
                            <link>https://weworkremotely.com/remote-jobs/example-qa</link>
                            <skills>Selenium, Java</skills><pubDate>Sat, 21 Nov 2026 11:00:00 GMT</pubDate></item>
                          <item><title>Other: Support (US-based)</title><region>Anywhere in the World</region>
                            <link>https://weworkremotely.com/remote-jobs/other-support</link></item>
                          <item><title>Other: Developer</title><region>Canada</region>
                            <link>https://weworkremotely.com/remote-jobs/other-dev</link></item>
                        </channel></rss>
                        """, MediaType.APPLICATION_XML));

        assertThat(sources.remoteRss(Map.of("testing", 7), NOW)).singleElement().satisfies(job -> {
            assertThat(job.provider()).isEqualTo("We Work Remotely");
            assertThat(job.categoryId()).isEqualTo(7);
            assertThat(job.applicationUrl()).isEqualTo("https://weworkremotely.com/remote-jobs/example-qa");
            assertThat(job.skills()).containsExactly("Selenium", "Java");
        });
        upstream.verify();
    }
}
