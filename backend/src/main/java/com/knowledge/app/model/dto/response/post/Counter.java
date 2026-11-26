package com.knowledge.app.model.dto.response.post;

/**
 * Kết quả sau khi thích / lưu bài: trạng thái hiện tại và tổng số lượt.
 */
public record Counter(boolean active, long count) {
}
