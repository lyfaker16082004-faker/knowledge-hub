package com.knowledge.app.model.dto.request.comment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Bình chọn bình luận: 1 = upvote, -1 = downvote, 0 = bỏ bình chọn.
 */
public record VoteRequest(@NotNull @Min(-1) @Max(1) Integer value) {
}
