package com.knowledge.app.model.dto.response.comment;

/**
 * Kết quả sau khi bình chọn: bình chọn của mình và tổng điểm mới.
 */
public record VoteResult(int myVote, long score) {
}
