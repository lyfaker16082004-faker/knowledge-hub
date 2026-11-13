package com.knowledge.app.repository;

import com.knowledge.app.model.entity.Faculty;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    List<Faculty> findByInstitutionIdOrderByNameAsc(Long institutionId);

    Optional<Faculty> findByIdAndInstitutionId(Long id, Long institutionId);

    boolean existsByInstitutionIdAndNameIgnoreCase(Long institutionId, String name);

    boolean existsByInstitutionIdAndNameIgnoreCaseAndIdNot(Long institutionId, String name, Long id);

    long countByInstitutionId(Long institutionId);

}
