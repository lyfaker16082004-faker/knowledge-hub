package com.knowledge.app.repository;

import com.knowledge.app.model.entity.PostView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostViewRepository extends JpaRepository<PostView, Long> {
}
