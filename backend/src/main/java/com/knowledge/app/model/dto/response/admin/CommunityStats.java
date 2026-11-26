package com.knowledge.app.model.dto.response.admin;

import java.util.List;

/** Số liệu tổng quan của Bảng tin cộng đồng cho quản trị viên. */
public record CommunityStats(
        long posts,
        long questions,
        long articles,
        long unanswered,
        long hiddenPosts,
        long comments,
        long hiddenComments,
        long postsLast7Days,
        long commentsLast7Days,
        List<TopTag> topTags) {

    public record TopTag(String name, long posts) {
    }
}
