package com.jobradar.notification;

import com.jobradar.common.api.ApiResponse;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.pagination.PaginationParameters;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service; public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public ApiResponse<PaginatedResponse<NotificationDto>> list(@Valid @ModelAttribute PaginationParameters page, Authentication a) { return ApiResponse.success("Notifications retrieved successfully", service.list(page, a)); }
    @GetMapping("/unread-count") public ApiResponse<Long> unread(Authentication a) { return ApiResponse.success("Unread notification count retrieved successfully", service.unread(a)); }
    @PatchMapping("/{id}/read") public ApiResponse<NotificationDto> read(@PathVariable Long id, Authentication a) { return ApiResponse.success("Notification marked as read", service.read(id, a)); }
    @PatchMapping("/read-all") public ApiResponse<Void> readAll(Authentication a) { service.readAll(a); return ApiResponse.success("Notifications marked as read", null); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id, Authentication a) { service.delete(id, a); return ApiResponse.success("Notification deleted", null); }
}
