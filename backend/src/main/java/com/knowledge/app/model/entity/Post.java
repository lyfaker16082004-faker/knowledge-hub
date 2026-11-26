package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import com.knowledge.app.model.enums.PostType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bảng posts: bài viết trên bảng tin (câu hỏi hoặc chia sẻ kiến thức).
 */
@Entity
@Table(name = "posts")
public class Post extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "summary", nullable = true, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "visibility", nullable = false, length = 20)
    private String visibility;

    @Column(name = "post_type", nullable = false, length = 20)
    private String postType = PostType.QUESTION.name();

    @Column(name = "published_at", nullable = true)
    private LocalDateTime publishedAt;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="institution_id")
    private Institution institution;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="faculty_id")
    private Faculty faculty;
    public Institution getInstitution(){return institution;}
    public void setInstitution(Institution value){institution=value;}
    public Faculty getFaculty(){return faculty;}
    public void setFaculty(Faculty value){faculty=value;}

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
