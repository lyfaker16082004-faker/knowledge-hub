package com.knowledge.app.repository;
import com.knowledge.app.model.entity.DocumentDownload;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface DocumentDownloadRepository extends JpaRepository<DocumentDownload,Long> {
 long countByDocumentId(Long id);long countByUserId(Long id);
 @Query("select count(distinct d.document.id) from DocumentDownload d where d.user.id=:user") long distinctDocuments(@Param("user") Long user);
 @EntityGraph(attributePaths={"document","document.category","file"}) Page<DocumentDownload> findByUserId(Long id,Pageable page);
}
