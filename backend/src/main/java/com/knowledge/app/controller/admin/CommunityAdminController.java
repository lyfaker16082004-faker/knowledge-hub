package com.knowledge.app.controller.admin;

import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.dto.request.post.ModerationRequest;
import com.knowledge.app.model.dto.response.admin.AdminCommentItem;
import com.knowledge.app.model.dto.response.admin.AdminPostItem;
import com.knowledge.app.model.dto.response.admin.CommunityStats;
import com.knowledge.app.service.admin.CommunityModerationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API quản trị Bảng tin cộng đồng & Hỏi đáp (Sprint 3). Chỉ ADMIN mới gọi được.
 */
@RestController
@Validated
@RequestMapping("/api/admin/community")
@PreAuthorize("hasRole('ADMIN')")
public class CommunityAdminController {
    private final CommunityModerationService moderation;

    public CommunityAdminController(CommunityModerationService moderation) {
        this.moderation = moderation;
    }

    @GetMapping("/stats")
    public ApiResponse<CommunityStats> stats() {
        return ApiResponse.ok(moderation.stats());
    }

    @GetMapping("/posts")
    public ApiResponse<PageResponse<AdminPostItem>> posts(
            @RequestParam(defaultValue = "") @Size(max = 150) String q,
            @RequestParam(defaultValue = "") @Pattern(regexp = "|PUBLISHED|REJECTED|ARCHIVED") String status,
            @RequestParam(defaultValue = "") @Pattern(regexp = "|QUESTION|ARTICLE") String type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.ok(moderation.posts(q, status, type, page, size));
    }

    @PutMapping("/posts/{id}/status")
    public ApiResponse<AdminPostItem> postStatus(@PathVariable Long id, @Valid @RequestBody ModerationRequest request) {
        AdminPostItem item = moderation.setPostStatus(id, request.status());
        return new ApiResponse<>(true, "PUBLISHED".equals(item.status()) ? "Đã khôi phục bài viết" : "Đã ẩn bài viết", item);
    }

    @GetMapping("/comments")
    public ApiResponse<PageResponse<AdminCommentItem>> comments(
            @RequestParam(defaultValue = "") @Size(max = 150) String q,
            @RequestParam(defaultValue = "") @Pattern(regexp = "|VISIBLE|HIDDEN|DELETED") String status,
            @RequestParam(required = false) @Positive Long postId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.ok(moderation.comments(q, status, postId, page, size));
    }

    @PutMapping("/comments/{id}/status")
    public ApiResponse<AdminCommentItem> commentStatus(@PathVariable Long id, @Valid @RequestBody ModerationRequest request) {
        AdminCommentItem item = moderation.setCommentStatus(id, request.status());
        return new ApiResponse<>(true, "VISIBLE".equals(item.status()) ? "Đã khôi phục bình luận" : "Đã ẩn bình luận", item);
    }
}
