package com.knowledge.app.service.impl.admin;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.common.util.JdbcUtils;
import com.knowledge.app.common.util.TextUtils;
import com.knowledge.app.common.util.TimeUtils;
import com.knowledge.app.model.dto.response.admin.AdminCommentItem;
import com.knowledge.app.model.dto.response.admin.AdminPostItem;
import com.knowledge.app.model.dto.response.admin.CommunityStats;
import com.knowledge.app.model.entity.Comment;
import com.knowledge.app.model.entity.Post;
import com.knowledge.app.model.enums.CommentStatus;
import com.knowledge.app.model.enums.PostStatus;
import com.knowledge.app.model.mapper.AuthorMapper;
import com.knowledge.app.repository.CommentRepository;
import com.knowledge.app.repository.PostRepository;
import com.knowledge.app.service.admin.CommunityModerationService;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản trị Bảng tin cộng đồng: xem mọi bài viết / bình luận, ẩn và khôi phục.
 *
 * Quy ước trạng thái:
 * - Bài viết: PUBLISHED (đang hiện), REJECTED (quản trị viên ẩn), ARCHIVED (tác giả đã xoá – chỉ xem).
 * - Bình luận: VISIBLE (đang hiện), HIDDEN (quản trị viên ẩn), DELETED (tác giả đã xoá – chỉ xem).
 */
@Service
@Transactional(readOnly = true)
public class CommunityModerationServiceImpl implements CommunityModerationService {

    private static final String POST_SELECT = """
            SELECT p.id, p.post_type, p.title, SUBSTRING(p.content, 1, 240) AS excerpt, p.status, p.visibility,
                   p.created_at, p.published_at,
                   u.id AS author_id, u.username, pr.display_name, pr.avatar_url,
                   (SELECT MIN(cat.name) FROM post_categories pc JOIN categories cat ON cat.id = pc.category_id
                    WHERE pc.post_id = p.id) AS category,
                   (SELECT COUNT(*) FROM post_likes l WHERE l.post_id = p.id) AS likes,
                   (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id AND c.status = 'VISIBLE') AS comment_count,
                   (SELECT COUNT(*) FROM post_views v WHERE v.post_id = p.id) AS views
            FROM posts p
            JOIN users u ON u.id = p.author_id
            LEFT JOIN user_profiles pr ON pr.user_id = u.id
            """;

    private static final String COMMENT_SELECT = """
            SELECT c.id, c.post_id, p.title AS post_title, p.status AS post_status, c.parent_id, c.content, c.status,
                   c.created_at, c.edited_at,
                   u.id AS author_id, u.username, pr.display_name, pr.avatar_url,
                   (SELECT COALESCE(SUM(cl.vote_value), 0) FROM comment_likes cl WHERE cl.comment_id = c.id) AS score,
                   (SELECT COUNT(*) FROM comments r WHERE r.parent_id = c.id) AS replies
            FROM comments c
            JOIN posts p ON p.id = c.post_id
            JOIN users u ON u.id = c.author_id
            LEFT JOIN user_profiles pr ON pr.user_id = u.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public CommunityModerationServiceImpl(JdbcTemplate jdbcTemplate, PostRepository postRepository,
            CommentRepository commentRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    // =========================================================
    // TỔNG QUAN
    // =========================================================

    @Override
    public CommunityStats stats() {
        Timestamp weekAgo = Timestamp.valueOf(TimeUtils.now().minusDays(7));
        List<CommunityStats.TopTag> topTags = jdbcTemplate.query("""
                SELECT t.name, COUNT(*) AS total FROM post_tags pt
                JOIN tags t ON t.id = pt.tag_id
                JOIN posts p ON p.id = pt.post_id
                WHERE p.status = 'PUBLISHED'
                GROUP BY t.id, t.name ORDER BY total DESC, t.name LIMIT 6
                """, (rs, i) -> new CommunityStats.TopTag(rs.getString("name"), rs.getLong("total")));
        return new CommunityStats(
                count("SELECT COUNT(*) FROM posts WHERE status = 'PUBLISHED'"),
                count("SELECT COUNT(*) FROM posts WHERE status = 'PUBLISHED' AND post_type = 'QUESTION'"),
                count("SELECT COUNT(*) FROM posts WHERE status = 'PUBLISHED' AND post_type = 'ARTICLE'"),
                count("""
                        SELECT COUNT(*) FROM posts p WHERE p.status = 'PUBLISHED' AND p.post_type = 'QUESTION'
                        AND NOT EXISTS (SELECT 1 FROM comments c WHERE c.post_id = p.id AND c.status = 'VISIBLE')
                        """),
                count("SELECT COUNT(*) FROM posts WHERE status = 'REJECTED'"),
                count("SELECT COUNT(*) FROM comments WHERE status = 'VISIBLE'"),
                count("SELECT COUNT(*) FROM comments WHERE status = 'HIDDEN'"),
                count("SELECT COUNT(*) FROM posts WHERE status = 'PUBLISHED' AND published_at >= ?", weekAgo),
                count("SELECT COUNT(*) FROM comments WHERE status = 'VISIBLE' AND created_at >= ?", weekAgo),
                topTags);
    }

    // =========================================================
    // BÀI VIẾT
    // =========================================================

    @Override
    public PageResponse<AdminPostItem> posts(String keyword, String status, String type, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE p.status IN ('PUBLISHED', 'REJECTED', 'ARCHIVED')");
        List<Object> params = new ArrayList<>();
        if (!status.isBlank()) {
            where.append(" AND p.status = ?");
            params.add(status);
        }
        if (!type.isBlank()) {
            where.append(" AND p.post_type = ?");
            params.add(type);
        }
        if (!keyword.isBlank()) {
            String pattern = like(keyword);
            where.append(" AND (LOWER(p.title) LIKE ? ESCAPE '!' OR LOWER(p.content) LIKE ? ESCAPE '!'"
                    + " OR LOWER(u.username) LIKE ? ESCAPE '!' OR LOWER(COALESCE(pr.display_name, '')) LIKE ? ESCAPE '!')");
            Collections.addAll(params, pattern, pattern, pattern, pattern);
        }

        long total = count("SELECT COUNT(*) FROM posts p JOIN users u ON u.id = p.author_id"
                + " LEFT JOIN user_profiles pr ON pr.user_id = u.id" + where, params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((long) page * size);
        List<AdminPostItem> rows = jdbcTemplate.query(POST_SELECT + where
                + " ORDER BY p.created_at DESC, p.id DESC LIMIT ? OFFSET ?",
                (rs, i) -> new AdminPostItem(rs.getLong("id"), rs.getString("post_type"), rs.getString("title"),
                        rs.getString("excerpt"), rs.getString("status"), rs.getString("visibility"),
                        AuthorMapper.fromResultSet(rs), rs.getString("category"), List.of(),
                        JdbcUtils.readTime(rs, "created_at"), JdbcUtils.readTime(rs, "published_at"),
                        rs.getLong("likes"), rs.getLong("comment_count"), rs.getLong("views")),
                pageParams.toArray());

        return new PageResponse<>(withTags(rows), total, (int) Math.ceil(total / (double) size), page, size);
    }

    @Override
    @Transactional
    public AdminPostItem setPostStatus(Long postId, String status) {
        if (!PostStatus.PUBLISHED.is(status) && !PostStatus.REJECTED.is(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Trạng thái chỉ có thể là PUBLISHED hoặc REJECTED");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"));
        if (PostStatus.ARCHIVED.is(post.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Bài viết đã bị tác giả xóa, không thể thay đổi");
        }
        if (!status.equals(post.getStatus())) {
            post.setStatus(status);
            postRepository.saveAndFlush(post);
        }
        return withTags(jdbcTemplate.query(POST_SELECT + " WHERE p.id = ?",
                (rs, i) -> new AdminPostItem(rs.getLong("id"), rs.getString("post_type"), rs.getString("title"),
                        rs.getString("excerpt"), rs.getString("status"), rs.getString("visibility"),
                        AuthorMapper.fromResultSet(rs), rs.getString("category"), List.of(),
                        JdbcUtils.readTime(rs, "created_at"), JdbcUtils.readTime(rs, "published_at"),
                        rs.getLong("likes"), rs.getLong("comment_count"), rs.getLong("views")), postId)).get(0);
    }

    // =========================================================
    // BÌNH LUẬN
    // =========================================================

    @Override
    public PageResponse<AdminCommentItem> comments(String keyword, String status, Long postId, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (!status.isBlank()) {
            where.append(" AND c.status = ?");
            params.add(status);
        }
        if (postId != null) {
            where.append(" AND c.post_id = ?");
            params.add(postId);
        }
        if (!keyword.isBlank()) {
            String pattern = like(keyword);
            where.append(" AND (LOWER(c.content) LIKE ? ESCAPE '!' OR LOWER(p.title) LIKE ? ESCAPE '!'"
                    + " OR LOWER(u.username) LIKE ? ESCAPE '!' OR LOWER(COALESCE(pr.display_name, '')) LIKE ? ESCAPE '!')");
            Collections.addAll(params, pattern, pattern, pattern, pattern);
        }

        long total = count("SELECT COUNT(*) FROM comments c JOIN posts p ON p.id = c.post_id"
                + " JOIN users u ON u.id = c.author_id LEFT JOIN user_profiles pr ON pr.user_id = u.id" + where,
                params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((long) page * size);
        List<AdminCommentItem> rows = jdbcTemplate.query(COMMENT_SELECT + where
                + " ORDER BY c.created_at DESC, c.id DESC LIMIT ? OFFSET ?", this::mapComment, pageParams.toArray());
        return new PageResponse<>(rows, total, (int) Math.ceil(total / (double) size), page, size);
    }

    @Override
    @Transactional
    public AdminCommentItem setCommentStatus(Long commentId, String status) {
        if (!CommentStatus.VISIBLE.is(status) && !CommentStatus.HIDDEN.is(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Trạng thái chỉ có thể là VISIBLE hoặc HIDDEN");
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bình luận"));
        if (CommentStatus.DELETED.is(comment.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Bình luận đã bị tác giả xóa, không thể thay đổi");
        }
        if (!status.equals(comment.getStatus())) {
            comment.setStatus(status);
            commentRepository.saveAndFlush(comment);
        }
        return jdbcTemplate.query(COMMENT_SELECT + " WHERE c.id = ?", this::mapComment, commentId).get(0);
    }

    // =========================================================
    // HÀM HỖ TRỢ
    // =========================================================

    private AdminCommentItem mapComment(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        Long parentId = JdbcUtils.readNullableLong(rs, "parent_id");
        return new AdminCommentItem(rs.getLong("id"), rs.getLong("post_id"), rs.getString("post_title"),
                rs.getString("post_status"), parentId != null && parentId == 0 ? null : parentId,
                AuthorMapper.fromResultSet(rs), rs.getString("content"), rs.getString("status"),
                JdbcUtils.readTime(rs, "created_at"), JdbcUtils.readTime(rs, "edited_at"),
                rs.getLong("score"), rs.getLong("replies"));
    }

    /** Gắn danh sách tag cho các bài trong trang bằng một truy vấn. */
    private List<AdminPostItem> withTags(List<AdminPostItem> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        String marks = String.join(",", Collections.nCopies(rows.size(), "?"));
        Map<Long, List<String>> tags = new HashMap<>();
        jdbcTemplate.query("SELECT pt.post_id, t.name FROM post_tags pt JOIN tags t ON t.id = pt.tag_id"
                + " WHERE pt.post_id IN (" + marks + ") ORDER BY pt.id",
                rs -> {
                    tags.computeIfAbsent(rs.getLong("post_id"), k -> new ArrayList<>()).add(rs.getString("name"));
                },
                rows.stream().map(AdminPostItem::id).toArray());
        return rows.stream().map(p -> new AdminPostItem(p.id(), p.type(), p.title(), p.excerpt(), p.status(),
                p.visibility(), p.author(), p.category(), tags.getOrDefault(p.id(), List.of()), p.createdAt(),
                p.publishedAt(), p.likes(), p.comments(), p.views())).toList();
    }

    private long count(String sql, Object... params) {
        return JdbcUtils.queryCount(jdbcTemplate, sql, params);
    }

    private static String like(String keyword) {
        return "%" + TextUtils.escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
    }
}
