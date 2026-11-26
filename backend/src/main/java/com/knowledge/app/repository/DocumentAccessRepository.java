package com.knowledge.app.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.knowledge.app.model.entity.DocumentAccess;

public interface DocumentAccessRepository extends JpaRepository<DocumentAccess, Long> {

    Optional<DocumentAccess> findByUserIdAndDocumentId(Long userId, Long documentId);

    /**
     * Đếm số quyền truy cập còn hiệu lực (chưa bị thu hồi và chưa hết hạn).
     * Kết quả > 0 nghĩa là người dùng được phép đọc tài liệu.
     */
    @Query("""
            select count(a) from DocumentAccess a
            where a.user.id = :user
              and a.document.id = :doc
              and a.revokedAt is null
              and (a.expiresAt is null or a.expiresAt > :now)
            """)
    long validGrant(@Param("user") Long user, @Param("doc") Long document, @Param("now") LocalDateTime now);
}
