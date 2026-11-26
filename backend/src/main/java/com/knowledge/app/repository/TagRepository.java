package com.knowledge.app.repository;

import com.knowledge.app.model.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy vấn bảng thẻ trong database (Spring Data JPA tự sinh câu SQL).
 */
public interface TagRepository extends JpaRepository<Tag, Long> {
    java.util.Optional<Tag> findByNameIgnoreCase(String name);
}
