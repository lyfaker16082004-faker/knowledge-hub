package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bảng notification_reads: thời điểm người dùng xem thông báo lần cuối.
 * Bình luận mới hơn seen_at được tính là chưa đọc.
 */
@Entity
@Table(name = "notification_reads", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id"})})
public class NotificationRead extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "seen_at", nullable = false)
    private LocalDateTime seenAt;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getSeenAt() { return seenAt; }
    public void setSeenAt(LocalDateTime seenAt) { this.seenAt = seenAt; }
}
