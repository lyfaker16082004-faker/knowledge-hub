package com.knowledge.app.repository;

import com.knowledge.app.model.entity.DocumentFile;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy vấn bảng file tài liệu trong database (Spring Data JPA tự sinh câu SQL).
 */
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {
    List<DocumentFile> findByDocumentIdAndActiveTrueOrderByIdAsc(Long id);

    Optional<DocumentFile> findByIdAndDocumentIdAndActiveTrue(Long id, Long documentId);
}
