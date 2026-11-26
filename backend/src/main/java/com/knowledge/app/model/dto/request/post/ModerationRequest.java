package com.knowledge.app.model.dto.request.post;

import jakarta.validation.constraints.NotBlank;

/** Quản trị viên đổi trạng thái hiển thị của bài viết / bình luận. */
public record ModerationRequest(@NotBlank(message = "Vui lòng chọn trạng thái") String status) {
}
