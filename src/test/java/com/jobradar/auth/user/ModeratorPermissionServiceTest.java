package com.jobradar.auth.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jobradar.auth.role.AppRoleEntity;
import com.jobradar.auth.role.AppRoleRepository;
import com.jobradar.common.exception.ForbiddenException;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

class ModeratorPermissionServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AppRoleRepository roles = mock(AppRoleRepository.class);
    private final ModeratorPermissionAuditRepository audit = mock(ModeratorPermissionAuditRepository.class);
    private final ModeratorPermissionService service = new ModeratorPermissionService(users, roles, audit);

    @Test void administratorCanGrantAndRevokeModeratorRole() {
        UserEntity target = new UserEntity("Mod", "mod@example.com", "hash");
        ReflectionTestUtils.setField(target, "id", 12);
        UserEntity actor = new UserEntity("Admin", "admin@example.com", "hash");
        ReflectionTestUtils.setField(actor, "id", 1);
        var moderatorRole = new AppRoleEntity("ROLE_MODERATOR");
        when(users.findById(12)).thenReturn(Optional.of(target));
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(actor));
        when(roles.findById("ROLE_MODERATOR")).thenReturn(Optional.of(moderatorRole));
        var admin = new UsernamePasswordAuthenticationToken("admin@example.com", "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var granted = service.setPermission(12, true, admin);
        var revoked = service.setPermission(12, false, admin);

        assertTrue(granted.moderator());
        assertFalse(revoked.moderator());
        assertEquals("admin@example.com", granted.updatedBy());
        verify(users, times(2)).save(target);
        verify(audit).save(argThat(entry -> entry.getTarget() == target && entry.getChangedBy() == actor && entry.isEnabled()));
        verify(audit).save(argThat(entry -> entry.getTarget() == target && entry.getChangedBy() == actor && !entry.isEnabled()));
    }

    @Test void regularUsersCannotManageModeratorPermissions() {
        var user = new UsernamePasswordAuthenticationToken("user@example.com", "",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertThrows(ForbiddenException.class, () -> service.setPermission(12, true, user));
        verifyNoInteractions(users, roles, audit);
    }
}
