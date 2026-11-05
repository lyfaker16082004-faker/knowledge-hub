package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_views")
public class PostView extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "viewer_id", nullable = true)
    private User viewer;

    @Column(name = "visitor_hash", nullable = true, length = 64)
    private String visitorHash;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public User getViewer() { return viewer; }
    public void setViewer(User viewer) { this.viewer = viewer; }
    public String getVisitorHash() { return visitorHash; }
    public void setVisitorHash(String visitorHash) { this.visitorHash = visitorHash; }
    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }
}
