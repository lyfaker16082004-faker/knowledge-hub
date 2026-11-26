package com.knowledge.app.repository;

import com.knowledge.app.model.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

/**
 * Truy vấn bảng hồ sơ người dùng trong database (Spring Data JPA tự sinh câu SQL).
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    @EntityGraph(attributePaths = { "institution", "faculty" })
    Optional<UserProfile> findByUserId(Long userId);

    long countByAvatarUrl(String avatarUrl);

    boolean existsByInstitutionId(Long institutionId);

    boolean existsByFacultyId(Long facultyId);
}
