package com.knowledge.app.repository;

import com.knowledge.app.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy vấn bảng bình luận trong database (Spring Data JPA tự sinh câu SQL).
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {
}
