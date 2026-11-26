package com.knowledge.app.repository;

import com.knowledge.app.model.entity.NotificationRead;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy vấn bảng notification_reads (thời điểm xem thông báo lần cuối).
 */
public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {
    Optional<NotificationRead> findByUserId(Long userId);
}
