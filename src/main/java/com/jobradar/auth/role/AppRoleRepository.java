package com.jobradar.auth.role;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRoleRepository extends JpaRepository<AppRoleEntity, String> {
}
