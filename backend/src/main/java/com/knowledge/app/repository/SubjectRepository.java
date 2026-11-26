package com.knowledge.app.repository;

import com.knowledge.app.model.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Truy vấn bảng môn học trong database (Spring Data JPA tự sinh câu SQL).
 */
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findAllByOrderByNameAsc();
}
