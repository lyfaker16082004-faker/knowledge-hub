package com.knowledge.app.repository;

import com.knowledge.app.model.entity.Institution;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy vấn bảng trường / tổ chức trong database (Spring Data JPA tự sinh câu SQL).
 */
public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    List<Institution> findAllByOrderByNameAsc();

    Optional<Institution> findByType(String type);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByType(String type);
}
