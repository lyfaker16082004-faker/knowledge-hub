package com.knowledge.app.service.impl.post;

import com.knowledge.app.common.util.JdbcUtils;
import com.knowledge.app.common.util.TimeUtils;
import com.knowledge.app.model.dto.response.post.NotificationItem;
import com.knowledge.app.model.entity.NotificationRead;
import com.knowledge.app.model.mapper.AuthorMapper;
import com.knowledge.app.repository.NotificationReadRepository;
import com.knowledge.app.repository.UserRepository;
import com.knowledge.app.service.post.NotificationService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thông báo bình luận.
 *
 * Không cần bảng thông báo riêng: thông báo được tính trực tiếp từ bảng comments.
 * Một bình luận là thông báo của tôi khi:
 * - Nó nằm trong bài viết do tôi đăng, HOẶC nó trả lời một bình luận của tôi
 * - Người viết không phải là tôi, bình luận và bài viết vẫn đang hiển thị
 * Bình luận mới hơn thời điểm tôi xem thông báo lần cuối (notification_reads) là "chưa đọc".
 */
@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    /** Điều kiện chung (3 tham số: id của tôi, id của tôi, id của tôi). */
    private static final String FROM_WHERE = """
             FROM comments c
            JOIN posts p ON p.id = c.post_id
            JOIN users u ON u.id = c.author_id
            LEFT JOIN user_profiles pr ON pr.user_id = u.id
            LEFT JOIN comments parent ON parent.id = c.parent_id
            WHERE c.status = 'VISIBLE' AND p.status = 'PUBLISHED' AND u.status = 'ACTIVE'
              AND c.author_id <> ?
              AND (p.author_id = ? OR parent.author_id = ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NotificationReadRepository readRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(JdbcTemplate jdbcTemplate, NotificationReadRepository readRepository,
            UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.readRepository = readRepository;
        this.userRepository = userRepository;
    }

    @Override
    public NotificationItem.Page list(Long userId, int size) {
        // Lần đầu chưa xem bao giờ -> mọi bình luận đều là chưa đọc
        LocalDateTime seenAt = readRepository.findByUserId(userId)
                .map(NotificationRead::getSeenAt)
                .orElse(LocalDateTime.of(2000, 1, 1, 0, 0));
        Timestamp seen = Timestamp.valueOf(seenAt);

        long unread = JdbcUtils.queryCount(jdbcTemplate,
                "SELECT COUNT(*)" + FROM_WHERE + " AND c.created_at > ?", userId, userId, userId, seen);

        List<NotificationItem> items = jdbcTemplate.query("""
                SELECT c.id, c.post_id, p.title, SUBSTRING(c.content, 1, 160) AS excerpt, c.created_at,
                       u.id AS author_id, u.username, pr.display_name, pr.avatar_url,
                       CASE WHEN parent.author_id = ? THEN 'REPLY' ELSE 'COMMENT' END AS kind
                """ + FROM_WHERE + " ORDER BY c.created_at DESC, c.id DESC LIMIT ?",
                (rs, i) -> {
                    LocalDateTime createdAt = JdbcUtils.readTime(rs, "created_at");
                    return new NotificationItem(rs.getLong("id"), rs.getLong("post_id"), rs.getString("title"),
                            rs.getString("kind"), AuthorMapper.fromResultSet(rs), rs.getString("excerpt"),
                            createdAt, createdAt != null && createdAt.isAfter(seenAt));
                },
                userId, userId, userId, userId, size);

        return new NotificationItem.Page(unread, items);
    }

    @Override
    @Transactional
    public void markAllSeen(Long userId) {
        NotificationRead read = readRepository.findByUserId(userId).orElseGet(() -> {
            NotificationRead created = new NotificationRead();
            created.setUser(userRepository.getReferenceById(userId));
            return created;
        });
        read.setSeenAt(TimeUtils.now());
        readRepository.saveAndFlush(read);
    }
}
