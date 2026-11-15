package com.jobradar.auth.user;

import com.jobradar.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class ModeratorPermissionController {
    private final ModeratorPermissionService permissions;

    public ModeratorPermissionController(ModeratorPermissionService permissions) { this.permissions = permissions; }

    @PutMapping("/{id}/moderator-permission")
    public ApiResponse<ModeratorPermissionDto> setPermission(@PathVariable Integer id,
            @Valid @RequestBody PermissionRequest request, Authentication administrator) {
        return ApiResponse.success("Moderator permission updated successfully",
                permissions.setPermission(id, request.enabled(), administrator));
    }

    public record PermissionRequest(@NotNull Boolean enabled) {}
}
