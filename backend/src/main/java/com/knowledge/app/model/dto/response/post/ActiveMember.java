package com.knowledge.app.model.dto.response.post;

/**
 * Thành viên tích cực. posts = số bài viết + số bình luận trong 30 ngày.
 */
public record ActiveMember(Long id, String name, String avatarUrl, long posts) {
}
