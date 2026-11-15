package com.jobradar.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModeratorPermissionAuditRepository extends JpaRepository<ModeratorPermissionAuditEntity, Long> {}
