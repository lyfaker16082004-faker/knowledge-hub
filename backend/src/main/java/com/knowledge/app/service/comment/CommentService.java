package com.knowledge.app.service.comment;

import java.util.List;

import com.knowledge.app.model.dto.response.comment.CommentItem;
import com.knowledge.app.model.dto.response.comment.VoteResult;

/**
 * Chức năng bình luận (nhiều cấp) của bài viết.
 * Code xử lý nằm trong service/impl/comment/CommentServiceImpl.
 */
public interface CommentService {

    /** Danh sách bình luận của một bài viết. */
    List<CommentItem> comments(Long postId, Long viewerId, boolean moderator);

    /**
     * Viết bình luận mới.
     *
     * @param parentId id bình luận được trả lời (null nếu là bình luận gốc)
     */
    CommentItem comment(Long userId, Long postId, Long parentId, String content);

    CommentItem editComment(Long userId, Long commentId, String content);

    /** Xoá bình luận. Moderator được xoá bình luận của người khác. */
    void deleteComment(Long userId, boolean moderator, Long commentId);

    /**
     * Bình chọn bình luận.
     *
     * @param value 1 = upvote, -1 = downvote, 0 = bỏ bình chọn
     */
    VoteResult vote(Long userId, Long commentId, int value);
}
