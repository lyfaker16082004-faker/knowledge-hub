package com.knowledge.app.model.entity.base;

import java.time.LocalDateTime;

import com.knowledge.app.common.util.TimeUtils;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

/**
 * Lớp cha của mọi entity. Chứa các cột chung: id, created_at, updated_at, version.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Database tự tăng id
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Dùng cho optimistic locking: mỗi lần cập nhật, version tăng lên 1.
     * Nếu 2 người cùng sửa một bản ghi, người lưu sau sẽ bị lỗi thay vì ghi đè âm thầm.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /** Tự chạy trước khi INSERT. */
    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtils.now();
        updatedAt = createdAt;
    }

    /** Tự chạy trước khi UPDATE. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = TimeUtils.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
