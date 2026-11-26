package com.knowledge.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.knowledge.app.model.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByInstitutionId(Long institutionId);

    boolean existsByFacultyId(Long facultyId);
}
