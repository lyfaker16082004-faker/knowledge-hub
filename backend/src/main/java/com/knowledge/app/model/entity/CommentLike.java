package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bảng comment_likes: bình chọn của người dùng cho bình luận (+1 hoặc -1).
 */
@Entity
@Table(name = "comment_likes", uniqueConstraints = {@UniqueConstraint(columnNames = {"comment_id", "user_id"})})
public class CommentLike extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "vote_value", nullable = false)
    private Short voteValue = 1;

    public Short getVoteValue() { return voteValue; }
    public void setVoteValue(Short voteValue) { this.voteValue = voteValue; }
    public Comment getComment() { return comment; }
    public void setComment(Comment comment) { this.comment = comment; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
