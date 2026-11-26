package com.knowledge.app.service.admin;

import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.dto.response.admin.AdminCommentItem;
import com.knowledge.app.model.dto.response.admin.AdminPostItem;
import com.knowledge.app.model.dto.response.admin.CommunityStats;

/** Quản trị Bảng tin cộng đồng & Hỏi đáp (Sprint 3). */
public interface CommunityModerationService {

    CommunityStats stats();

    PageResponse<AdminPostItem> posts(String keyword, String status, String type, int page, int size);

    /** Ẩn (REJECTED) hoặc khôi phục (PUBLISHED) một bài viết. */
    AdminPostItem setPostStatus(Long postId, String status);

    PageResponse<AdminCommentItem> comments(String keyword, String status, Long postId, int page, int size);

    /** Ẩn (HIDDEN) hoặc khôi phục (VISIBLE) một bình luận. */
    AdminCommentItem setCommentStatus(Long commentId, String status);
}
