package com.knowledge.app.repository;

import com.knowledge.app.model.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRequestRepository extends JpaRepository<AiRequest, Long> {
}
