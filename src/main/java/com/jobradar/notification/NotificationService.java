package com.jobradar.notification;

import com.jobradar.auth.user.UserEntity;
import com.jobradar.auth.user.UserRepository;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.exception.ResourceNotFoundException;
import com.jobradar.common.pagination.PaginationParameters;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notifications; private final UserRepository users; private final EmailNotificationSender emailSender;
    public NotificationService(NotificationRepository notifications, UserRepository users, EmailNotificationSender emailSender) { this.notifications = notifications; this.users = users; this.emailSender = emailSender; }
    @Transactional(readOnly = true) public PaginatedResponse<NotificationDto> list(PaginationParameters p, Authentication a) { return PaginatedResponse.from(notifications.findByUserId(user(a).getId(), PageRequest.of(p.getPage(), p.getSize(), Sort.by(Sort.Direction.DESC, "createdAt", "id"))).map(NotificationDto::from)); }
    @Transactional(readOnly = true) public long unread(Authentication a) { return notifications.countByUserIdAndReadFalse(user(a).getId()); }
    @Transactional public NotificationDto read(Long id, Authentication a) { NotificationEntity n = owned(id, a); if (!n.isRead()) n.markRead(); return NotificationDto.from(n); }
    @Transactional public void readAll(Authentication a) { notifications.markAllUnreadAsRead(user(a).getId(), Instant.now()); }
    @Transactional public void delete(Long id, Authentication a) { notifications.delete(owned(id, a)); }
    @Transactional public void create(UserEntity user, String title, String message, NotificationType type, Integer referenceId) {
        if (referenceId != null && notifications.existsByUserIdAndTypeAndReferenceId(user.getId(), type, referenceId)) return;
        notifications.save(new NotificationEntity(user, title, message, type, referenceId == null ? null : "OPPORTUNITY", referenceId));
        try { emailSender.send(user.getEmail(), title, message); } catch (RuntimeException exception) { log.warn("Notification email delivery failed for user {}", user.getId(), exception); }
    }
    @Transactional
    public void createReviewUpdate(UserEntity recipient, String title, String message, NotificationType type,
            Integer opportunityId) {
        String referenceType = type == NotificationType.OPPORTUNITY_REVIEW_REQUESTED
                ? "ADMIN_REVIEW" : "OPPORTUNITY";
        notifications.save(new NotificationEntity(recipient, title, message, type, referenceType, opportunityId));
    }
    private NotificationEntity owned(Long id, Authentication a) { return notifications.findByIdAndUserId(id, user(a).getId()).orElseThrow(() -> new ResourceNotFoundException("Notification not found")); }
    private UserEntity user(Authentication a) { return users.findByEmailIgnoreCase(a.getName()).orElseThrow(() -> new ResourceNotFoundException("User account not found")); }
}
