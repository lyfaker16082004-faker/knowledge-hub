package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name="document_files")
public class DocumentFile extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="document_id",nullable=false)
    private Document document;

    @Column(name="original_name",nullable=false, length=255)
    private String originalName;

    @Column(name="storage_key",nullable=false, length=255)
    private String storageKey;

    @Column(name="content_type",nullable=false, length=100)
    private String contentType;

    @Column(name="size_bytes",nullable=false)
    private Long sizeBytes;

    @Column(name="active",nullable=false)
    private Boolean active;

    public Document getDocument() { return document; }
    public void setDocument(Document value) { document=value; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String value) { originalName=value; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String value) { storageKey=value; }
    public String getContentType() { return contentType; }
    public void setContentType(String value) { contentType=value; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long value) { sizeBytes=value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { active=value; }
}
