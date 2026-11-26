package com.knowledge.app.model.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu viết / sửa bình luận.
 * parentId: id bình luận được trả lời (null nếu là bình luận gốc).
 */
public record CommentRequest(
        @NotBlank @Size(max = 5000, message = "Bình luận tối đa 5.000 ký tự") String content,
        @Positive Long parentId) {
}
