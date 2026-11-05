package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_categories", uniqueConstraints = {@UniqueConstraint(columnNames = {"post_id", "category_id"})})
public class PostCategory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}
