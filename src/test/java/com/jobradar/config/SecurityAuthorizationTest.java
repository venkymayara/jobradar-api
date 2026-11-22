package com.jobradar.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobradar.security.ApiAccessDeniedHandler;
import com.jobradar.security.ApiAuthenticationEntryPoint;
import com.jobradar.auth.user.AdminUserController;
import com.jobradar.auth.user.AdminRoleService;
import com.jobradar.auth.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = {SecurityAuthorizationTest.ProbeController.class, AdminUserController.class})
@Import({SecurityConfig.class, CorsConfig.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        SecurityAuthorizationTest.ProbeController.class})
class SecurityAuthorizationTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserRepository users;
    @MockitoBean private AdminRoleService adminRoles;

    @Test
    @WithMockUser(roles = "ADMIN")
    void regularAdminCannotChangeAdminAccess() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/12/admin-permission").with(csrf())
                .contentType("application/json").content("{\"enabled\":true}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminRoles);
    }

    @Test
    void adminDirectoryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void moderatorCannotReadAdminDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden());
        verifyNoInteractions(users);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorCanSearchTheDirectory() throws Exception {
        when(users.findAdminDirectoryUsers(eq("Example"), any(Pageable.class))).thenReturn(Page.empty());
        mockMvc.perform(get("/api/v1/admin/users").param("search", " Example "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void protectedProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {"success":false,"message":"Authentication is required","data":null}
                        """, false));
    }

    @Test
    void savedOpportunityEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/saved")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/opportunities/42/save").with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/opportunities/42/save").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void opportunityReportingRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/opportunities/42/report").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void authenticatedUsersCanReachReportSubmission() throws Exception {
        mockMvc.perform(post("/api/v1/opportunities/42/report").with(csrf())).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotManageModeratorPermissions() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/12/moderator-permission").with(csrf())
                .contentType("application/json").content("{\"enabled\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicOpportunityBrowsingAllowsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/v1/opportunities/probe")).andExpect(status().isOk());
    }

    @Test
    void opportunitySubmissionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/opportunities/probe").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void authenticatedUserCanSubmitOpportunity() throws Exception {
        mockMvc.perform(post("/api/v1/opportunities/probe").with(csrf())).andExpect(status().isOk());
    }

    @Test
    void refreshCookieEndpointsRequireCsrfProtection() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isForbidden());
    }

    @Test
    void corsAllowsConfiguredAngularOriginWithCredentials() throws Exception {
        mockMvc.perform(options("/api/v1/auth/refresh")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotAccessModeration() throws Exception {
        mockMvc.perform(get("/api/v1/moderation/probe")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/categories/probe")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/moderation/companies/probe")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/moderation/opportunities/probe")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/moderation/reports/probe")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/moderation/reports/3")).andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/moderation/reports/3").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/moderation/opportunities/42/status").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void moderatorCanAccessModerationButNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/moderation/probe")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/moderation/companies/probe")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/moderation/opportunities/probe")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/moderation/reports/probe")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/moderation/reports/3")).andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/moderation/reports/3").with(csrf())).andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/moderation/opportunities/42/status").with(csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/probe")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdministrativeRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/probe")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/categories/probe")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/moderation/probe")).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/admin/users/12/moderator-permission").with(csrf())
                .contentType("application/json").content("{\"enabled\":true}")).andExpect(status().isOk());
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/api/v1/users/me/probe", "/api/v1/opportunities/probe",
                "/api/v1/users/me/saved",
                "/api/v1/moderation/probe", "/api/v1/admin/probe", "/api/v1/admin/categories/probe",
                "/api/v1/moderation/companies/probe", "/api/v1/moderation/opportunities/probe",
                "/api/v1/moderation/reports/probe", "/api/v1/moderation/reports/3"})
        String probe() {
            return "ok";
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/opportunities/probe")
        String submitProbe() { return "ok"; }

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/opportunities/42/save")
        String saveProbe() { return "ok"; }

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/opportunities/42/report")
        String reportProbe() { return "ok"; }

        @org.springframework.web.bind.annotation.DeleteMapping("/api/v1/opportunities/42/save")
        String removeProbe() { return "ok"; }

        @org.springframework.web.bind.annotation.PutMapping("/api/v1/admin/users/12/moderator-permission")
        String moderatorPermissionProbe() { return "ok"; }

        @org.springframework.web.bind.annotation.PatchMapping("/api/v1/moderation/reports/3")
        String reviewReportProbe() { return "ok"; }

        @org.springframework.web.bind.annotation.PatchMapping("/api/v1/moderation/opportunities/42/status")
        String moderateOpportunityProbe() { return "ok"; }
    }
}
