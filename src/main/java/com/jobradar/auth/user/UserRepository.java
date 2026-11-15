package com.jobradar.auth.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @Query("select distinct u from UserEntity u left join fetch u.roles where lower(u.email) = lower(:email)")
    Optional<UserEntity> findWithRolesByEmail(@Param("email") String email);
}
