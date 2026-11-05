package com.knowledge.app.repository;
import com.knowledge.app.model.entity.DocumentAccess;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface DocumentAccessRepository extends JpaRepository<DocumentAccess,Long> {
 Optional<DocumentAccess> findByUserIdAndDocumentId(Long userId,Long documentId);
 @Query("select count(a) from DocumentAccess a where a.user.id=:user and a.document.id=:doc and a.revokedAt is null and (a.expiresAt is null or a.expiresAt>:now)")
 long validGrant(@Param("user") Long user,@Param("doc") Long document,@Param("now") LocalDateTime now);
}
