package com.knowledge.app.repository;

import com.knowledge.app.model.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
    java.util.Optional<Tag> findByNameIgnoreCase(String name);
}
