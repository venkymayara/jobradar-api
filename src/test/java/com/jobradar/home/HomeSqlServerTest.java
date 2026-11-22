package com.jobradar.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "app.external-jobs.remotive.enabled=false",
        "app.opportunities.expiry-enabled=false",
        "app.walk-ins.expiry-initial-delay-ms=86400000",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureMockMvc
@Import(HomeSqlServerTest.FixedClock.class)
@EnabledIfEnvironmentVariable(named = "JOBRADAR_SQL_TESTS", matches = "true")
@Transactional
class HomeSqlServerTest {
    private static final int COMPANY_ID = -20261122;
    private static final Instant NOW = Instant.parse("2026-11-22T12:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void publicHomepageFiltersBeforeLimitingAndCountsTheSameEligibilityWindow() throws Exception {
        int roleId = jdbc.queryForObject("""
                SELECT TOP (1) r.id FROM dbo.job_roles r JOIN dbo.categories c ON c.id = r.category_id
                WHERE r.is_active = 1 AND c.is_active = 1 ORDER BY r.id
                """, Integer.class);
        int categoryId = jdbc.queryForObject("SELECT category_id FROM dbo.job_roles WHERE id = ?", Integer.class, roleId);
        int skillId = jdbc.queryForObject("SELECT MIN(id) FROM dbo.skills WHERE is_active = 1", Integer.class);
        int userId = jdbc.queryForObject("SELECT MIN(id) FROM dbo.users", Integer.class);
        int locationId = jdbc.queryForObject("SELECT MIN(id) FROM dbo.locations", Integer.class);
        JsonNode baseline = homepage();

        jdbc.execute("SET IDENTITY_INSERT dbo.companies ON");
        try {
            jdbc.update("INSERT dbo.companies (id, company_name, is_active) VALUES (?, 'Homepage SQL test', 1)", COMPANY_ID);
        } finally {
            jdbc.execute("SET IDENTITY_INSERT dbo.companies OFF");
        }
        jdbc.execute("SET IDENTITY_INSERT dbo.Opportunities ON");
        try {
            for (int sequence = 1; sequence <= 11; sequence++) {
                insertOpportunity(sequence, "JOB", "ACTIVE", "2026-11-22", userId, locationId);
            }
            for (int sequence = 12; sequence <= 19; sequence++) {
                insertOpportunity(sequence, "WALK_IN", "ACTIVE", null, userId, locationId);
                insertDrive(sequence, "2026-11-23T12:00:00+00:00", "2026-11-23T13:00:00+00:00");
            }
            insertOpportunity(20, "JOB", "ACTIVE", "2026-11-21", userId, locationId);
            insertOpportunity(21, "JOB", "PENDING", null, userId, locationId);
            insertOpportunity(22, "WALK_IN", "ACTIVE", null, userId, locationId);
            insertDrive(22, "2026-11-22T11:00:00+00:00", "2026-11-22T12:00:00+00:00");
            insertOpportunity(23, "WALK_IN", "ACTIVE", null, userId, locationId);
            insertOpportunity(24, "JOB", "REMOVED", null, userId, locationId);
            insertOpportunity(25, "WALK_IN", "ACTIVE", "2026-11-21", userId, locationId);
            insertDrive(25, "2026-11-23T12:00:00+00:00", "2026-11-23T13:00:00+00:00");
        } finally {
            jdbc.execute("SET IDENTITY_INSERT dbo.Opportunities OFF");
        }
        for (int sequence = 1; sequence <= 25; sequence++) {
            jdbc.update("INSERT dbo.OpportunityRoles (opportunity_id, job_role_id) VALUES (?, ?)", id(sequence), roleId);
            jdbc.update("INSERT dbo.OpportunitySkills (opportunity_id, skill_id) VALUES (?, ?)", id(sequence), skillId);
        }

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        JsonNode result = homepage();

        assertEquals(4, result.size());
        assertEquals(10, result.path("categories").size());
        assertEquals(10, result.path("popularSkills").size());
        assertEquals(10, result.path("latestOpportunities").size());
        assertEquals(6, result.path("upcomingWalkIns").size());
        assertEquals(IntStream.iterate(11, n -> n - 1).limit(10).map(HomeSqlServerTest::id).boxed().toList(),
                result.path("latestOpportunities").valueStream().map(row -> row.get("opportunityId").intValue()).toList());
        assertEquals(IntStream.rangeClosed(12, 17).map(HomeSqlServerTest::id).boxed().toList(),
                result.path("upcomingWalkIns").valueStream().map(row -> row.get("opportunityId").intValue()).toList());
        assertEquals(count(baseline, "categories", "categoryId", categoryId) + 19,
                count(result, "categories", "categoryId", categoryId));
        assertEquals(count(baseline, "popularSkills", "skillId", skillId) + 19,
                count(result, "popularSkills", "skillId", skillId));

        for (String section : new String[] {"latestOpportunities", "upcomingWalkIns"}) {
            for (JsonNode row : result.path(section)) {
                assertEquals("ACTIVE", row.path("status").asText());
                assertEquals(skillId, row.path("skills").get(0).path("id").asInt());
                assertTrue(row.path("contactEmail").isNull());
                assertTrue(row.path("contactPhone").isNull());
                assertTrue(row.path("contactName").isNull());
                assertTrue(row.path("instructions").isNull());
                assertTrue(row.path("publisher").isMissingNode());
            }
        }
        long queryCount = statistics.getPrepareStatementCount();
        assertTrue(queryCount <= 8, () -> "Expected batched homepage reads but observed " + queryCount + " statements");
    }

    private JsonNode homepage() throws Exception {
        return objectMapper.readTree(mvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
    }

    private static int count(JsonNode payload, String section, String idField, int id) {
        return payload.path(section).valueStream().filter(row -> row.path(idField).asInt() == id)
                .findFirst().map(row -> row.path("opportunityCount").asInt()).orElse(0);
    }

    private static int id(int sequence) {
        return COMPANY_ID + sequence;
    }

    private void insertOpportunity(int sequence, String type, String status, String deadline, int userId, int locationId) {
        jdbc.update("""
                INSERT dbo.Opportunities (id, title, company_id, location_id, work_mode, employment_type,
                    min_experience, max_experience, opportunity_type, description, application_deadline,
                    status, published_at, created_by_user_id)
                VALUES (?, ?, ?, ?, 'ON_SITE', 'FULL_TIME', 0, 2, ?, 'Homepage integration fixture',
                    CONVERT(date, ?), ?, CONVERT(datetimeoffset, ?), ?)
                """, id(sequence), "Homepage SQL test " + sequence, COMPANY_ID, locationId, type, deadline, status,
                sequence >= 20 ? "2026-11-22T11:00:00+00:00"
                        : sequence <= 11 ? "2026-11-22T10:00:00+00:00" : "2026-11-22T09:00:00+00:00",
                userId);
    }

    private void insertDrive(int sequence, String start, String end) {
        jdbc.update("""
                INSERT dbo.WalkInDetails (opportunity_id, drive_date, start_time, end_time, time_zone_id,
                    drive_start_at_utc, drive_end_at_utc, venue, contact_name, contact_phone, contact_email, instructions)
                VALUES (?, CONVERT(date, ?), '12:00', '13:00', 'UTC',
                    CONVERT(datetimeoffset, ?), CONVERT(datetimeoffset, ?), 'Test venue',
                    'Private contact', '5550100', 'private@example.test', 'Private instructions')
                """, id(sequence), start.substring(0, 10), start, end);
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock homepageTestClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
