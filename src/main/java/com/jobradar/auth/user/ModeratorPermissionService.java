package com.jobradar.auth.user;

import com.jobradar.auth.role.AppRoleEntity;
import com.jobradar.auth.role.AppRoleRepository;
import com.jobradar.common.exception.ForbiddenException;
import com.jobradar.common.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModeratorPermissionService {
    private static final String MODERATOR_ROLE = "ROLE_MODERATOR";
    private final UserRepository users;
    private final AppRoleRepository roles;
    private final ModeratorPermissionAuditRepository audit;

    public ModeratorPermissionService(UserRepository users, AppRoleRepository roles,
            ModeratorPermissionAuditRepository audit) {
        this.users = users;
        this.roles = roles;
        this.audit = audit;
    }

    @Transactional
    public ModeratorPermissionDto setPermission(Integer userId, boolean enabled, Authentication administrator) {
        if (administrator == null || !administrator.isAuthenticated() || administrator.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).noneMatch("ROLE_ADMIN"::equals))
            throw new ForbiddenException("Only administrators can manage moderator permissions");

        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
        UserEntity actor = users.findByEmailIgnoreCase(administrator.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Administrator account not found"));
        AppRoleEntity moderator = roles.findById(MODERATOR_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("Moderator role is not configured"));
        boolean changed = enabled ? user.getRoles().add(moderator) : user.getRoles().remove(moderator);
        if (changed) {
            users.save(user);
            audit.save(new ModeratorPermissionAuditEntity(user, actor, enabled));
        }
        return new ModeratorPermissionDto(user.getId(), user.getEmail(), user.getRoles().contains(moderator), administrator.getName());
    }
}
