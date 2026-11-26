package com.knowledge.app.model.dto.response.post;

import java.util.List;

/**
 * Dữ liệu cột bên của trang hỏi đáp.
 */
public record Sidebar(
        List<CountItem> categories,
        List<CountItem> trendingTags,
        List<ActiveMember> activeMembers,
        long totalPosts) {
}
