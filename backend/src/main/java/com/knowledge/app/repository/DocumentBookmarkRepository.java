package com.knowledge.app.repository;

import com.knowledge.app.model.entity.DocumentBookmark;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface DocumentBookmarkRepository extends JpaRepository<DocumentBookmark, Long> {
    Optional<DocumentBookmark> findByUserIdAndDocumentId(Long user, Long document);

    boolean existsByUserIdAndDocumentId(Long user, Long document);

    long countByUserIdAndDocumentStatus(Long user, String status);

    @EntityGraph(attributePaths = { "document", "document.category", "document.author" })
    Page<DocumentBookmark> findByUserIdAndDocumentStatus(Long user, String status, Pageable page);
}
