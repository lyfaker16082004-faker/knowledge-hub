package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name="document_downloads")
public class DocumentDownload extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="document_id",nullable=false)
    private Document document;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="file_id",nullable=false)
    private DocumentFile file;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id",nullable=false)
    private User user;

    @Column(name="document_title",nullable=false, length=255)
    private String documentTitle;

    @Column(name="file_name",nullable=false, length=255)
    private String fileName;

    public Document getDocument() { return document; }
    public void setDocument(Document value) { document=value; }
    public DocumentFile getFile() { return file; }
    public void setFile(DocumentFile value) { file=value; }
    public User getUser() { return user; }
    public void setUser(User value) { user=value; }
    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String value) { documentTitle=value; }
    public String getFileName() { return fileName; }
    public void setFileName(String value) { fileName=value; }
}
