package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name="document_access")
public class DocumentAccess extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="document_id",nullable=false)
    private Document document;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id",nullable=false)
    private User user;

    @Column(name="grant_type",nullable=false, length=20)
    private String grantType;

    @Column(name="expires_at",nullable=true)
    private LocalDateTime expiresAt;

    @Column(name="revoked_at",nullable=true)
    private LocalDateTime revokedAt;

    public Document getDocument() { return document; }
    public void setDocument(Document value) { document=value; }
    public User getUser() { return user; }
    public void setUser(User value) { user=value; }
    public String getGrantType() { return grantType; }
    public void setGrantType(String value) { grantType=value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { expiresAt=value; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime value) { revokedAt=value; }
}
