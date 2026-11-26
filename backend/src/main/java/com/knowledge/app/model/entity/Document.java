package com.knowledge.app.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.knowledge.app.model.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tài liệu trong thư viện (bảng "documents").
 *
 * Vòng đời trạng thái (status):
 *   PENDING (chờ duyệt) -> PUBLISHED (đã công bố) hoặc REJECTED (bị từ chối)
 *   PUBLISHED -> ARCHIVED (bị ẩn) -> PUBLISHED (khôi phục)
 *   Bất kỳ -> DELETED (đã xoá mềm)
 */
@Entity
@Table(name = "documents")
public class Document extends BaseEntity {

    // ===================== Thông tin cơ bản =====================

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "description", nullable = false, columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "level", nullable = false, length = 20)
    private String level; // BEGINNER, INTERMEDIATE, ADVANCED

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "cover_style", nullable = false, length = 20)
    private String coverStyle;

    @Column(name = "featured", nullable = false)
    private Boolean featured; // Tài liệu nổi bật

    // ===================== Giá =====================

    @Column(name = "access_type", nullable = false, length = 10)
    private String accessType; // FREE hoặc PAID

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    // ===================== Trạng thái và kiểm duyệt =====================

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "published_at", nullable = true)
    private LocalDateTime publishedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote; // Ghi chú của admin khi duyệt

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy; // Admin đã duyệt

    // ===================== Quan hệ =====================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "author_id", nullable = true)
    private User author; // null = tài liệu do ban biên tập đăng

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    /** Nhiều tài liệu - nhiều nhãn, lưu qua bảng trung gian "document_tags". */
    @ManyToMany
    @JoinTable(name = "document_tags",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    // ===================== Getter / Setter =====================

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCoverStyle() {
        return coverStyle;
    }

    public void setCoverStyle(String coverStyle) {
        this.coverStyle = coverStyle;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }
}
