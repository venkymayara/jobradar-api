package com.jobradar.config;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobradar.auth.api.RegisterRequest;
import com.jobradar.auth.service.AuthService;
import com.jobradar.auth.service.JwtTokenService;
import com.jobradar.auth.user.UserRepository;
import jakarta.persistence.EntityManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "app.external-jobs.remotive.enabled=false",
        "app.notifications.email-enabled=false",
        "app.opportunities.expiry-enabled=false",
        "app.walk-ins.expiry-initial-delay-ms=86400000"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "JOBRADAR_SQL_TESTS", matches = "true")
@Transactional
class DashboardSqlServerTest {
    private static final String TEST_EMAIL = "dashboard-sql-fixture@example.test";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthService auth;
    @Autowired ObjectMapper json;
    @Autowired JwtTokenService jwt;
    @Autowired UserRepository users;
    @Autowired EntityManager entityManager;

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void superAdminCanGrantAndRevokeWithImmediateEffectOnExistingTokens() throws Exception {
        String actorEmail = "superadmin-fixture@example.test";
        String targetEmail = "admin-target-fixture@example.test";
        int actorId = auth.register(new RegisterRequest("Super Admin fixture", actorEmail, "Dashboard-Fixture-42")).userId();
        int targetId = auth.register(new RegisterRequest("Admin target fixture", targetEmail, "Dashboard-Fixture-42")).userId();
        entityManager.flush();
        jdbc.update("INSERT dbo.user_roles (username, role) VALUES (?, 'ROLE_SUPER_ADMIN')", actorEmail);
        entityManager.clear();
        String endpoint = "/api/v1/admin/users/" + targetId + "/admin-permission";
        String actorToken = "Bearer " + jwt.createAccessToken(users.findWithRolesByEmail(actorEmail).orElseThrow());
        var csrfResponse = mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk()).andReturn().getResponse();
        var csrfCookie = csrfResponse.getCookie("XSRF-TOKEN");
        String csrfHeader = json.readTree(csrfResponse.getContentAsString()).path("data").path("token").asText();

        mvc.perform(put(endpoint).header("Authorization", actorToken).cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfHeader).contentType("application/json")
                .content("{\"enabled\":true}")).andExpect(status().isOk());
        entityManager.flush();
        String token = jwt.createAccessToken(users.findWithRolesByEmail(targetEmail).orElseThrow());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token).param("search", targetEmail))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].administrator").value(true));

        mvc.perform(put(endpoint).header("Authorization", actorToken).cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfHeader).contentType("application/json")
                .content("{\"enabled\":false}")).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.moderator_permission_history
                WHERE target_user_id = ? AND changed_by_user_id = ? AND changed_role = 'ROLE_ADMIN'
                """, Integer.class, targetId, actorId));

        mvc.perform(put("/api/v1/admin/users/" + actorId + "/admin-permission")
                .header("Authorization", actorToken).cookie(csrfCookie).header("X-XSRF-TOKEN", csrfHeader)
                .contentType("application/json").content("{\"enabled\":false}")).andExpect(status().isForbidden());
    }

    @Test
    void externalJobsFilterAndPageInSql() throws Exception {
        int existing = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.external_jobs", Integer.class);
        jdbc.update("""
                INSERT dbo.external_jobs (external_id, title, company_name, description, location, work_mode,
                    application_url, provider, posted_at, fetched_at)
                VALUES ('review-sql-ext-1', 'Paging fixture 100% engineer', 'Paging fixture company', 'SQL fixture',
                    'Hyderabad', 'REMOTE', 'https://example.test/job/1', 'FIXTURE', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
                       ('review-sql-ext-2', 'Paging fixture 100% engineer', 'Paging fixture company', 'SQL fixture',
                    'Hyderabad', 'REMOTE', 'https://example.test/job/2', 'FIXTURE', NULL, SYSDATETIMEOFFSET()),
                       ('review-sql-ext-3', 'Paging fixture 1000 engineer', 'Paging fixture company', 'SQL fixture',
                    'Hyderabad', 'REMOTE', 'https://example.test/job/3', 'FIXTURE', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())
                """);

        mvc.perform(get("/api/v1/external-jobs").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(existing + 3))
                .andExpect(jsonPath("$.data.content.length()").value(2));

        for (int page = 0; page < 2; page++) {
            mvc.perform(get("/api/v1/external-jobs").param("keyword", "  PAGING FIXTURE 100% ")
                    .param("company", "paging fixture").param("location", "hyderabad")
                    .param("workMode", "remote").param("page", String.valueOf(page)).param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.content[0].externalId").value("review-sql-ext-" + (page + 1)));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void directoryPagesThroughTheSqlView() throws Exception {
        mvc.perform(get("/api/v1/admin/users").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].userId").isNumber())
                .andExpect(jsonPath("$.data.content[0].administrator").isBoolean())
                .andExpect(jsonPath("$.data.content[0].moderator").isBoolean())
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/admin/users").param("search", "jobradar-nonexistent@example.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "USER")
    void preferencesNotificationsAndWalkInUseTheExistingAccountWorkflow() throws Exception {
        int userId = auth.register(new RegisterRequest("Dashboard fixture", TEST_EMAIL, "Dashboard-Fixture-42")).userId();

        mvc.perform(put("/api/v1/users/me/preferences").with(csrf()).contentType("application/json")
                .content("""
                        {"emailNotificationsEnabled":false,"jobAlertsEnabled":true}
                        """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailNotificationsEnabled").value(false))
                .andExpect(jsonPath("$.data.jobAlertsEnabled").value(true));

        jdbc.update("""
                INSERT dbo.notifications (user_id, title, message, notification_type)
                SELECT TOP (1001) ?, 'Notification fixture', 'Test message', 'ACCOUNT'
                FROM sys.all_objects a CROSS JOIN sys.all_objects b
                """, userId);
        mvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(1001));
        mvc.perform(patch("/api/v1/notifications/read-all").with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(0));

        int roleId = jdbc.queryForObject("""
                SELECT MIN(r.id) FROM dbo.job_roles r JOIN dbo.categories c ON c.id = r.category_id
                WHERE r.is_active = 1 AND c.is_active = 1
                """, Integer.class);
        int locationId = jdbc.queryForObject("SELECT MIN(id) FROM dbo.locations WHERE is_active = 1", Integer.class);
        int skillId = jdbc.queryForObject("SELECT MIN(id) FROM dbo.skills WHERE is_active = 1", Integer.class);
        LocalDate driveDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(2);
        String companyName = "Walk-in fixture " + UUID.randomUUID();

        String walkIn = """
                        {"title":"Walk-in fixture","companyName":"%s","locationId":%d,"roleIds":[%d],"skillIds":[%d],
                         "workMode":"ON_SITE","employmentType":"FULL_TIME","currencyCode":"INR",
                         "opportunityType":"WALK_IN","description":"A fixture for the walk-in submission form",
                         "sourceUrl":"https://example.test/event","driveDate":"%s","driveStartTime":"09:00",
                         "driveEndTime":"10:00","venue":"Test venue","timeZoneId":"Asia/Kolkata",
                         "contactEmail":"hiring@example.test","minExperience":0,"maxExperience":2}
                        """.formatted(companyName, locationId, roleId, skillId, driveDate);
        var submitted = mvc.perform(post("/api/v1/opportunities").with(csrf()).contentType("application/json").content(walkIn))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.timeZoneId").value("Asia/Kolkata"))
                .andExpect(jsonPath("$.data.skills[0].id").value(skillId))
                .andReturn();
        mvc.perform(get("/api/v1/users/me/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Walk-in fixture"));

        int id = json.readTree(submitted.getResponse().getContentAsString()).path("data").path("opportunityId").asInt();
        var administrator = user("review-test@example.test").roles("ADMIN");
        String publicPath = "/api/v1/opportunities/" + id;
        String adminPath = "/api/v1/admin/opportunities/" + id;

        mvc.perform(get(adminPath)).andExpect(status().isForbidden());
        mvc.perform(get(publicPath).with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(get(adminPath).with(administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedBy").value("Dashboard fixture"))
                .andExpect(jsonPath("$.data.opportunity.status").value("PENDING"));
        mvc.perform(get("/api/v1/admin/opportunities/review-queue").with(administrator))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Walk-in fixture")));
        assertAdministratorNotifications(id, 1);

        mvc.perform(patch(adminPath + "/review").with(administrator).with(csrf()).contentType("application/json")
                .content("""
                        {"decision":"REQUEST_DETAILS","note":"Please explain the registration process."}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NEEDS_DETAILS"));
        mvc.perform(get(publicPath)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canRespond").value(true))
                .andExpect(jsonPath("$.data.statusHistory[1].note").value("Please explain the registration process."));
        mvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("OPPORTUNITY_DETAILS_REQUESTED"));

        String reply = """
                {"message":"Candidates must pre-register using the event link."}
                """;
        mvc.perform(post(publicPath + "/review-response").with(administrator).with(csrf())
                .contentType("application/json").content(reply)).andExpect(status().isNotFound());
        mvc.perform(post(publicPath + "/review-response").with(csrf()).contentType("application/json").content(reply))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        assertAdministratorNotifications(id, 2);
        mvc.perform(get(adminPath).with(administrator)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opportunity.statusHistory[2].note")
                        .value("Candidates must pre-register using the event link."));

        mvc.perform(patch(adminPath + "/review").with(administrator).with(csrf()).contentType("application/json")
                .content("""
                        {"decision":"APPROVE","note":"Checked the event listing."}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        mvc.perform(get(publicPath).with(anonymous())).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("pre-register"))))
                .andExpect(content().string(not(containsString("Checked the event"))));

        var second = mvc.perform(post("/api/v1/opportunities").with(csrf()).contentType("application/json")
                .content(walkIn.replace("Walk-in fixture", "Rejected fixture")))
                .andExpect(status().isCreated()).andReturn();
        int secondId = json.readTree(second.getResponse().getContentAsString()).path("data").path("opportunityId").asInt();
        String rejectionPath = "/api/v1/admin/opportunities/" + secondId + "/review";
        mvc.perform(patch(rejectionPath).with(administrator).with(csrf()).contentType("application/json")
                .content("""
                        {"decision":"REJECT","note":" "}
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(rejectionPath).with(administrator).with(csrf()).contentType("application/json")
                .content("""
                        {"decision":"REJECT","note":"The source did not confirm this event."}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
        mvc.perform(get("/api/v1/opportunities/" + secondId).with(anonymous())).andExpect(status().isNotFound());
    }

    private void assertAdministratorNotifications(int opportunityId, int expectedPerAdministrator) {
        int administrators = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.admin_user_directory WHERE administrator = 1 OR super_administrator = 1
                """, Integer.class);
        int notifications = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.notifications
                WHERE reference_id = ? AND reference_type = 'ADMIN_REVIEW'
                """, Integer.class, opportunityId);
        assertEquals(administrators * expectedPerAdministrator, notifications);
    }
}
