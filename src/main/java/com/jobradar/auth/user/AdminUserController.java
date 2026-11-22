package com.jobradar.auth.user;

import com.jobradar.common.api.ApiResponse;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.pagination.PaginationParameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final UserRepository users;
    private final AdminRoleService roles;

    public AdminUserController(UserRepository users, AdminRoleService roles) {
        this.users = users;
        this.roles = roles;
    }

    @GetMapping
    public ApiResponse<PaginatedResponse<AdminUser>> list(
            @RequestParam(defaultValue = "") @Size(max = 100) String search,
            @Valid @ModelAttribute PaginationParameters pagination) {
        String literalSearch = search.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_").replace("[", "![");
        var page = users.findAdminDirectoryUsers(literalSearch, PageRequest.of(pagination.getPage(), pagination.getSize()))
                .map(user -> new AdminUser(user.getUserId(), user.getDisplayName(), user.getUsername(),
                        user.getAdministrator(), user.getModerator(), user.getSuperAdministrator()));
        return ApiResponse.success("Users retrieved successfully", PaginatedResponse.from(page));
    }

    @PutMapping("/{userId}/admin-permission")
    public ApiResponse<Void> setAdministrator(@PathVariable int userId,
            @Valid @RequestBody AdminPermission request, Authentication actor) {
        roles.setAdministrator(userId, request.enabled(), actor);
        return ApiResponse.success(request.enabled() ? "Admin access granted" : "Admin access revoked", null);
    }

    public record AdminPermission(@NotNull Boolean enabled) {}

    public record AdminUser(int userId, String displayName, String username, boolean administrator,
            boolean moderator, boolean superAdministrator) {}
}
