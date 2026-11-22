package com.jobradar.auth.user;

import com.jobradar.auth.role.AppRoleEntity;
import com.jobradar.auth.role.AppRoleRepository;
import com.jobradar.common.exception.ForbiddenException;
import com.jobradar.common.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRoleService {
    private static final AppRoleEntity SUPER_ADMIN = new AppRoleEntity("ROLE_SUPER_ADMIN");
    private final UserRepository users;
    private final AppRoleRepository roles;
    private final ModeratorPermissionAuditRepository audit;

    public AdminRoleService(UserRepository users, AppRoleRepository roles, ModeratorPermissionAuditRepository audit) {
        this.users = users;
        this.roles = roles;
        this.audit = audit;
    }

    @Transactional
    public void setAdministrator(int userId, boolean enabled, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Only Super Admins can manage Admin access");
        }
        var actor = users.findWithRolesByEmail(authentication.getName())
                .filter(user -> user.getRoles().contains(SUPER_ADMIN))
                .orElseThrow(() -> new ForbiddenException("Only Super Admins can manage Admin access"));
        if (actor.getId() == userId) {
            throw new ForbiddenException("You cannot change your own Admin access");
        }
        var target = users.findForRoleUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
        if (target.getRoles().contains(SUPER_ADMIN)) {
            throw new ForbiddenException("Super Admin accounts are managed separately");
        }
        var admin = roles.findById("ROLE_ADMIN")
                .orElseThrow(() -> new ResourceNotFoundException("Admin role is not configured"));
        boolean changed = enabled ? target.getRoles().add(admin) : target.getRoles().remove(admin);
        if (changed) {
            audit.save(new ModeratorPermissionAuditEntity(target, actor, enabled, admin.getRole()));
        }
    }
}
