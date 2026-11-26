package com.knowledge.app.repository;

import com.knowledge.app.model.entity.Category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy vấn bảng chủ đề trong database (Spring Data JPA tự sinh câu SQL).
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderByNameAsc();
}
