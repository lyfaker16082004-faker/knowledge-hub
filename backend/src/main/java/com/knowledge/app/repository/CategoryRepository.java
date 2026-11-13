package com.knowledge.app.repository;

import com.knowledge.app.model.entity.Category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderByNameAsc();
}
