package com.jobradar.auth.user;

import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @Query("select distinct u from UserEntity u left join fetch u.roles where lower(u.email) = lower(:email)")
    Optional<UserEntity> findWithRolesByEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findForRoleUpdate(@Param("id") Integer id);

    @Query("select distinct u from UserEntity u join u.roles r where r.role in ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    List<UserEntity> findAdministrators();

    String ADMIN_DIRECTORY_FILTER = """
            FROM dbo.admin_user_directory
            WHERE :search = '' OR CHARINDEX(LOWER(:search), LOWER(display_name)) > 0
                OR CHARINDEX(LOWER(:search), LOWER(username)) > 0
            """;

    @Query(value = """
            SELECT user_id AS userId, display_name AS displayName, username, administrator, moderator,
                   super_administrator AS superAdministrator
            """ + ADMIN_DIRECTORY_FILTER + " ORDER BY display_name, user_id",
            countQuery = "SELECT COUNT(*) " + ADMIN_DIRECTORY_FILTER, nativeQuery = true)
    Page<AdminDirectoryUser> findAdminDirectoryUsers(@Param("search") String search, Pageable pageable);

    interface AdminDirectoryUser {
        int getUserId();
        String getDisplayName();
        String getUsername();
        boolean getAdministrator();
        boolean getModerator();
        boolean getSuperAdministrator();
    }
}
