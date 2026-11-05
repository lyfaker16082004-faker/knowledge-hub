package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name="document_bookmarks")
public class DocumentBookmark extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="document_id",nullable=false)
    private Document document;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id",nullable=false)
    private User user;

    public Document getDocument() { return document; }
    public void setDocument(Document value) { document=value; }
    public User getUser() { return user; }
    public void setUser(User value) { user=value; }
}
