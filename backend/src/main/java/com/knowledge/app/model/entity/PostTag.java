package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_tags", uniqueConstraints = {@UniqueConstraint(columnNames = {"post_id", "tag_id"})})
public class PostTag extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public Tag getTag() { return tag; }
    public void setTag(Tag tag) { this.tag = tag; }
}
