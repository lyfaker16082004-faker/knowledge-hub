package com.knowledge.app.model.dto.request.post;

import jakarta.validation.constraints.NotNull;

/**
 * Bật / tắt một tương tác (thích, lưu bài viết).
 * active = true để bật, false để tắt.
 */
public record ToggleRequest(@NotNull Boolean active) {
}
