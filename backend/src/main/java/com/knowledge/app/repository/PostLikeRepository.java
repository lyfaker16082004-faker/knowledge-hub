package com.knowledge.app.repository;

import com.knowledge.app.model.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {}
