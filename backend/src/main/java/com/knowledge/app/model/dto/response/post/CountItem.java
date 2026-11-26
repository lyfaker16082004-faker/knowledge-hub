package com.knowledge.app.model.dto.response.post;

/**
 * Một mục kèm số lượng (VD: chủ đề và số bài viết, nhãn và số lần dùng).
 */
public record CountItem(Long id, String name, long count) {
}
