package com.knowledge.app.model.dto.response.document;
import java.time.LocalDateTime;
public record ManagedDocument(DocumentSummary document,String description,Long authorId,String author,String status,String reviewNote,LocalDateTime createdAt,LocalDateTime updatedAt,Long version){}
