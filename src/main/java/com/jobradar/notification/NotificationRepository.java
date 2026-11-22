package com.jobradar.notification;

import java.util.Optional;
import java.time.Instant;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findByUserId(Integer userId, Pageable pageable);
    long countByUserIdAndReadFalse(Integer userId);
    Optional<NotificationEntity> findByIdAndUserId(Long id, Integer userId);
    boolean existsByUserIdAndTypeAndReferenceId(Integer userId, NotificationType type, Integer referenceId);

    @Modifying
    @Query("update NotificationEntity n set n.read = true, n.readAt = :readAt where n.user.id = :userId and n.read = false")
    void markAllUnreadAsRead(@Param("userId") Integer userId, @Param("readAt") Instant readAt);
}
