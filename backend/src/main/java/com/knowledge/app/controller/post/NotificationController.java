package com.knowledge.app.controller.post;

import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.model.dto.response.post.NotificationItem;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.post.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API thông báo bình luận của người đang đăng nhập.
 */
@RestController
@RequestMapping("/api/users/me/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    /** Danh sách thông báo (mới nhất trước) và số chưa đọc. */
    @GetMapping
    public ApiResponse<NotificationItem.Page> list(@AuthenticationPrincipal AccountPrincipal user,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.ok(notifications.list(user.getId(), size));
    }

    /** Đánh dấu đã xem hết. */
    @PostMapping("/seen")
    public ApiResponse<Void> seen(@AuthenticationPrincipal AccountPrincipal user) {
        notifications.markAllSeen(user.getId());
        return new ApiResponse<>(true, "Đã xem thông báo", null);
    }
}
