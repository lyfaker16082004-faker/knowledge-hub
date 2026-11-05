package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name="documents")
public class Document extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="category_id",nullable=false)
    private Category category;

    @ManyToOne(fetch=FetchType.LAZY, optional=true)
    @JoinColumn(name="author_id",nullable=true)
    private User author;

    @Column(name="title",nullable=false, length=255)
    private String title;

    @Column(name="slug",nullable=false, length=255)
    private String slug;

    @Column(name="summary",nullable=false, columnDefinition="TEXT")
    private String summary;

    @Column(name="description",nullable=false, columnDefinition="LONGTEXT")
    private String description;

    @Column(name="access_type",nullable=false, length=10)
    private String accessType;

    @Column(name="price",nullable=false, precision=12, scale=2)
    private BigDecimal price;

    @Column(name="currency",nullable=false, length=3)
    private String currency;

    @Column(name="level",nullable=false, length=20)
    private String level;

    @Column(name="language",nullable=false, length=10)
    private String language;

    @Column(name="status",nullable=false, length=20)
    private String status;

    @Column(name="featured",nullable=false)
    private Boolean featured;

    @Column(name="published_at",nullable=true)
    private LocalDateTime publishedAt;

    @Column(name="cover_style",nullable=false, length=20)
    private String coverStyle;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="institution_id")
    private Institution institution;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="faculty_id")
    private Faculty faculty;
    public Institution getInstitution(){return institution;}
    public void setInstitution(Institution value){institution=value;}
    public Faculty getFaculty(){return faculty;}
    public void setFaculty(Faculty value){faculty=value;}

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="subject_id") private Subject subject;
    @Column(name="review_note",columnDefinition="TEXT") private String reviewNote;
    @Column(name="reviewed_at") private LocalDateTime reviewedAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by_id") private User reviewedBy;
    @ManyToMany @JoinTable(name="document_tags",joinColumns=@JoinColumn(name="document_id"),inverseJoinColumns=@JoinColumn(name="tag_id")) private java.util.Set<Tag> tags=new java.util.LinkedHashSet<>();
    public Subject getSubject(){return subject;}public void setSubject(Subject value){subject=value;}
    public String getReviewNote(){return reviewNote;}public void setReviewNote(String value){reviewNote=value;}
    public LocalDateTime getReviewedAt(){return reviewedAt;}public void setReviewedAt(LocalDateTime value){reviewedAt=value;}
    public User getReviewedBy(){return reviewedBy;}public void setReviewedBy(User value){reviewedBy=value;}
    public java.util.Set<Tag> getTags(){return tags;}public void setTags(java.util.Set<Tag> value){tags=value;}
    public Category getCategory() { return category; }
    public void setCategory(Category value) { category=value; }
    public User getAuthor() { return author; }
    public void setAuthor(User value) { author=value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title=value; }
    public String getSlug() { return slug; }
    public void setSlug(String value) { slug=value; }
    public String getSummary() { return summary; }
    public void setSummary(String value) { summary=value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description=value; }
    public String getAccessType() { return accessType; }
    public void setAccessType(String value) { accessType=value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { price=value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { currency=value; }
    public String getLevel() { return level; }
    public void setLevel(String value) { level=value; }
    public String getLanguage() { return language; }
    public void setLanguage(String value) { language=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status=value; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean value) { featured=value; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime value) { publishedAt=value; }
    public String getCoverStyle() { return coverStyle; }
    public void setCoverStyle(String value) { coverStyle=value; }
}
