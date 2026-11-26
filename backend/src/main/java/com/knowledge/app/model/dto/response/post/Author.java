package com.knowledge.app.model.dto.response.post;

/**
 * Thông tin ngắn gọn của tác giả bài viết / bình luận.
 */
public record Author(Long id, String username, String displayName, String avatarUrl) {
}
