package com.knowledge.app.service.impl.post;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowledge.app.common.constant.AppConstants;
import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.common.util.JdbcUtils;
import com.knowledge.app.model.dto.response.post.AuthorStats;
import com.knowledge.app.common.util.TextUtils;
import com.knowledge.app.common.util.TimeUtils;
import com.knowledge.app.model.dto.response.post.ActiveMember;
import com.knowledge.app.model.dto.response.post.Author;
import com.knowledge.app.model.dto.response.post.CountItem;
import com.knowledge.app.model.dto.response.post.Counter;
import com.knowledge.app.model.dto.response.post.PostDetail;
import com.knowledge.app.model.dto.response.post.PostSummary;
import com.knowledge.app.model.dto.response.post.Sidebar;
import com.knowledge.app.model.entity.Category;
import com.knowledge.app.model.entity.Post;
import com.knowledge.app.model.enums.PostStatus;
import com.knowledge.app.model.enums.PostVisibility;
import com.knowledge.app.model.mapper.AuthorMapper;
import com.knowledge.app.repository.CategoryRepository;
import com.knowledge.app.repository.PostRepository;
import com.knowledge.app.repository.UserRepository;
import com.knowledge.app.service.post.PostService;

/**
 * Xử lý bài viết của trang hỏi đáp (bình luận nằm ở CommentServiceImpl).
 *
 * - Phần ĐỌC dùng SQL thuần (JdbcTemplate) để mỗi trang bảng tin chỉ tốn một số câu query cố định.
 * - Phần GHI dùng JPA entity.
 */
@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    /**
     * Điều kiện "bài viết được phép xem" (cần 2 tham số: id người xem, id người xem):
     * - Bài đã đăng và tác giả còn hoạt động, VÀ
     * - Bài công khai, HOẶC mình là tác giả, HOẶC bài dành cho follower và mình đang theo dõi tác giả
     */
    private static final String VISIBLE_CONDITION = """
            p.status = 'PUBLISHED' AND u.status = 'ACTIVE'
            AND (p.visibility = 'PUBLIC'
                 OR p.author_id = ?
                 OR (p.visibility = 'FOLLOWERS'
                     AND EXISTS (SELECT 1 FROM user_follows f
                                 WHERE f.following_id = p.author_id AND f.follower_id = ?)))
            """;

    /** Các cột cần lấy của bài viết (cần 2 tham số: id người xem để tính "liked" và "saved"). */
    private static final String POST_COLUMNS = """
            SELECT p.id, p.post_type, p.title, p.summary,
                   SUBSTRING(p.content, 1, 600) AS body,
                   p.published_at, p.updated_at, p.version,
                   u.id AS author_id, u.username, pr.display_name, pr.avatar_url,
                   (SELECT MIN(pc.category_id) FROM post_categories pc WHERE pc.post_id = p.id) AS category_id,
                   (SELECT COUNT(*) FROM post_likes l WHERE l.post_id = p.id) AS likes,
                   (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id AND c.status = 'VISIBLE') AS comment_count,
                   (SELECT COUNT(*) FROM post_views v WHERE v.post_id = p.id) AS views,
                   (SELECT COUNT(*) FROM post_likes l WHERE l.post_id = p.id AND l.user_id = ?) AS liked,
                   (SELECT COUNT(*) FROM bookmarks b WHERE b.post_id = p.id AND b.user_id = ?) AS saved
            """;

    private static final String POST_FROM = """
             FROM posts p
            JOIN users u ON u.id = p.author_id
            LEFT JOIN user_profiles pr ON pr.user_id = u.id
            WHERE
            """;

    private final JdbcTemplate jdbcTemplate;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public PostServiceImpl(JdbcTemplate jdbcTemplate,
            PostRepository postRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    /** Một dòng dữ liệu bài viết đọc từ database (chỉ dùng trong class này). */
    private record PostRow(Long id, String type, String title, String summary, String body,
            LocalDateTime publishedAt, LocalDateTime updatedAt, Long version, Author author, Long categoryId,
            long likes, long comments, long views, boolean liked, boolean saved) {
    }

    // =========================================================
    // PHẦN 1: ĐỌC DỮ LIỆU
    // =========================================================

    @Override
    public PageResponse<PostSummary> feed(Long viewerId, String tab, String keyword, Long categoryId,
            String tag, String type, Long authorId, int page, int size) {
        long me = viewerId != null ? viewerId : AppConstants.GUEST_USER_ID;

        boolean needsLogin = List.of("following", "saved", "mine").contains(tab);
        if (viewerId == null && needsLogin) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để xem mục này");
        }

        // ---- Bước 1: Ghép điều kiện WHERE ----
        StringBuilder where = new StringBuilder(VISIBLE_CONDITION);
        List<Object> whereParams = new ArrayList<>();
        whereParams.add(me); // 2 tham số cho VISIBLE_CONDITION
        whereParams.add(me);

        switch (tab) {
            case "following":
                where.append(" AND EXISTS (SELECT 1 FROM user_follows f2"
                        + " WHERE f2.following_id = p.author_id AND f2.follower_id = ?)");
                whereParams.add(me);
                break;
            case "saved":
                where.append(" AND EXISTS (SELECT 1 FROM bookmarks b2 WHERE b2.post_id = p.id AND b2.user_id = ?)");
                whereParams.add(me);
                break;
            case "mine":
                where.append(" AND p.author_id = ?");
                whereParams.add(me);
                break;
            case "unanswered":
                where.append(" AND p.post_type = 'QUESTION'"
                        + " AND NOT EXISTS (SELECT 1 FROM comments c2"
                        + " WHERE c2.post_id = p.id AND c2.status = 'VISIBLE')");
                break;
            default:
                break;
        }

        if (categoryId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM post_categories pc2"
                    + " WHERE pc2.post_id = p.id AND pc2.category_id = ?)");
            whereParams.add(categoryId);
        }

        if (tag != null && !tag.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM post_tags pt JOIN tags t ON t.id = pt.tag_id"
                    + " WHERE pt.post_id = p.id AND LOWER(t.name) = ?)");
            whereParams.add(normalizeTag(tag));
        }

        if (type != null && !type.isBlank()) {
            where.append(" AND p.post_type = ?");
            whereParams.add(type);
        }

        // Trang hồ sơ: chỉ lấy bài của một tác giả
        if (authorId != null) {
            where.append(" AND p.author_id = ?");
            whereParams.add(authorId);
        }

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + TextUtils.escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
            where.append(" AND (LOWER(p.title) LIKE ? ESCAPE '!'"
                    + " OR LOWER(p.content) LIKE ? ESCAPE '!'"
                    + " OR EXISTS (SELECT 1 FROM post_tags pt3 JOIN tags t3 ON t3.id = pt3.tag_id"
                    + " WHERE pt3.post_id = p.id AND LOWER(t3.name) LIKE ? ESCAPE '!'))");
            whereParams.add(pattern);
            whereParams.add(pattern);
            whereParams.add(pattern);
        }

        // ---- Bước 2: Đếm tổng số bài ----
        String countSql = "SELECT COUNT(*) FROM posts p JOIN users u ON u.id = p.author_id WHERE " + where;
        long total = JdbcUtils.queryCount(jdbcTemplate, countSql, whereParams.toArray());

        // ---- Bước 3: Lấy danh sách bài của trang hiện tại ----
        String orderBy;
        if ("popular".equals(tab)) {
            // Điểm phổ biến = lượt thích x3 + bình luận x2 + lượt xem / 10
            orderBy = "(x.likes * 3 + x.comment_count * 2 + x.views / 10) DESC, x.published_at DESC, x.id DESC";
        } else {
            orderBy = "x.published_at DESC, x.id DESC";
        }

        // Thứ tự tham số: 2 tham số của POST_COLUMNS, các tham số WHERE, LIMIT, OFFSET
        List<Object> allParams = new ArrayList<>();
        allParams.add(me);
        allParams.add(me);
        allParams.addAll(whereParams);
        allParams.add(size);
        allParams.add((long) page * size);

        String sql = "SELECT * FROM (" + POST_COLUMNS + POST_FROM + where + ") x"
                + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
        List<PostRow> rows = jdbcTemplate.query(sql, this::mapPostRow, allParams.toArray());

        List<PostSummary> items = toSummaries(rows, viewerId);
        int totalPages = (int) Math.ceil(total / (double) size);
        return new PageResponse<>(items, total, totalPages, page, size);
    }

    @Override
    public AuthorStats authorStats(Long authorId) {
        // Chỉ tính bài đang hiển thị và bình luận chưa bị xoá / ẩn
        String mine = "SELECT id FROM posts WHERE author_id = ? AND status = 'PUBLISHED'";
        return new AuthorStats(
                count("SELECT COUNT(*) FROM posts WHERE author_id = ? AND status = 'PUBLISHED'", authorId),
                count("SELECT COUNT(*) FROM posts WHERE author_id = ? AND status = 'PUBLISHED' AND post_type = 'QUESTION'",
                        authorId),
                count("SELECT COUNT(*) FROM posts WHERE author_id = ? AND status = 'PUBLISHED' AND post_type = 'ARTICLE'",
                        authorId),
                count("SELECT COUNT(*) FROM comments WHERE author_id = ? AND status = 'VISIBLE'", authorId),
                count("SELECT COUNT(*) FROM post_likes WHERE post_id IN (" + mine + ")", authorId),
                count("SELECT COUNT(*) FROM comments WHERE status = 'VISIBLE' AND author_id <> ? AND post_id IN ("
                        + mine + ")", authorId, authorId),
                count("SELECT COUNT(*) FROM post_views WHERE post_id IN (" + mine + ")", authorId),
                count("SELECT COALESCE(SUM(cl.vote_value), 0) FROM comment_likes cl JOIN comments c ON c.id = cl.comment_id"
                        + " WHERE c.author_id = ? AND c.status = 'VISIBLE'", authorId),
                count("SELECT COUNT(*) FROM documents WHERE author_id = ? AND status = 'PUBLISHED'", authorId));
    }

    private long count(String sql, Object... params) {
        return JdbcUtils.queryCount(jdbcTemplate, sql, params);
    }

    @Override
    @Transactional
    public PostDetail detail(Long postId, Long viewerId, boolean moderator, String visitorKey) {
        long me = viewerId != null ? viewerId : AppConstants.GUEST_USER_ID;

        String sql = POST_COLUMNS + POST_FROM + "p.id = ? AND " + VISIBLE_CONDITION;
        List<PostRow> rows = jdbcTemplate.query(sql, this::mapPostRow, me, me, postId, me, me);
        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết");
        }

        recordView(postId, viewerId, visitorKey);

        // Lấy toàn bộ nội dung (POST_COLUMNS chỉ lấy 600 ký tự đầu)
        String content = jdbcTemplate.queryForObject("SELECT content FROM posts WHERE id = ?", String.class, postId);

        PostRow row = rows.get(0);
        PostSummary summary = toSummaries(rows, viewerId).get(0);
        return new PostDetail(summary, content, row.updatedAt(), row.version(), moderator);
    }

    @Override
    public Sidebar sidebar() {
        String publicPost = "p.status = 'PUBLISHED' AND p.visibility = 'PUBLIC' AND u.status = 'ACTIVE'";

        // Số bài viết công khai của từng chủ đề
        String categorySql = "SELECT c.id, c.name,"
                + " (SELECT COUNT(*) FROM post_categories pc"
                + "  JOIN posts p ON p.id = pc.post_id"
                + "  JOIN users u ON u.id = p.author_id"
                + "  WHERE pc.category_id = c.id AND " + publicPost + ") AS n"
                + " FROM categories c WHERE c.active = true"
                + " ORDER BY n DESC, c.sort_order, c.name";
        List<CountItem> categories = jdbcTemplate.query(categorySql,
                (rs, rowNumber) -> new CountItem(rs.getLong("id"), rs.getString("name"), rs.getLong("n")));

        Timestamp thirtyDaysAgo = Timestamp.valueOf(TimeUtils.now().minusDays(30));

        // 8 nhãn được dùng nhiều nhất trong 30 ngày
        String tagSql = "SELECT t.id, t.name, COUNT(*) AS n"
                + " FROM post_tags pt"
                + " JOIN tags t ON t.id = pt.tag_id"
                + " JOIN posts p ON p.id = pt.post_id"
                + " JOIN users u ON u.id = p.author_id"
                + " WHERE " + publicPost + " AND p.published_at >= ?"
                + " GROUP BY t.id, t.name ORDER BY n DESC, t.name LIMIT 8";
        List<CountItem> trendingTags = jdbcTemplate.query(tagSql,
                (rs, rowNumber) -> new CountItem(rs.getLong("id"), rs.getString("name"), rs.getLong("n")),
                thirtyDaysAgo);

        // 5 thành viên tích cực nhất (số bài + số bình luận trong 30 ngày)
        String memberSql = "SELECT u.id, u.username, pr.display_name, pr.avatar_url,"
                + " (SELECT COUNT(*) FROM posts p WHERE p.author_id = u.id"
                + "  AND p.status = 'PUBLISHED' AND p.published_at >= ?)"
                + " + (SELECT COUNT(*) FROM comments c WHERE c.author_id = u.id"
                + "  AND c.status = 'VISIBLE' AND c.created_at >= ?) AS n"
                + " FROM users u LEFT JOIN user_profiles pr ON pr.user_id = u.id"
                + " WHERE u.status = 'ACTIVE' ORDER BY n DESC, u.id LIMIT 5";
        List<ActiveMember> allMembers = jdbcTemplate.query(memberSql, (rs, rowNumber) -> {
            String name = rs.getString("display_name");
            if (name == null) {
                name = rs.getString("username");
            }
            return new ActiveMember(rs.getLong("id"), name, rs.getString("avatar_url"), rs.getLong("n"));
        }, thirtyDaysAgo, thirtyDaysAgo);

        // Chỉ giữ người có hoạt động
        List<ActiveMember> activeMembers = new ArrayList<>();
        for (ActiveMember member : allMembers) {
            if (member.posts() > 0) {
                activeMembers.add(member);
            }
        }

        long totalPosts = JdbcUtils.queryCount(jdbcTemplate,
                "SELECT COUNT(*) FROM posts p JOIN users u ON u.id = p.author_id WHERE " + publicPost);

        return new Sidebar(categories, trendingTags, activeMembers, totalPosts);
    }

    // =========================================================
    // PHẦN 2: TẠO / SỬA / XOÁ BÀI VIẾT
    // =========================================================

    @Override
    @Transactional
    public PostDetail create(Long userId, String type, String title, String content, Long categoryId,
            List<String> tags) {
        Category category = findActiveCategory(categoryId);

        Post post = new Post();
        post.setAuthor(userRepository.getReferenceById(userId));
        post.setPostType(type);
        post.setTitle(title.trim());
        // Slug = tiêu đề không dấu + 8 ký tự ngẫu nhiên. VD: "hoc-java-co-ban-1a2b3c4d"
        post.setSlug(TextUtils.toSlug(title) + "-" + UUID.randomUUID().toString().substring(0, 8));
        post.setContent(content.trim());
        post.setSummary(makeExcerpt(content, 300));
        post.setStatus(PostStatus.PUBLISHED.name());
        post.setVisibility(PostVisibility.PUBLIC.name());
        post.setPublishedAt(TimeUtils.now());
        postRepository.saveAndFlush(post);

        insertPostCategory(post.getId(), category.getId());
        saveTags(post.getId(), tags);

        return detail(post.getId(), userId, false, null);
    }

    @Override
    @Transactional
    public PostDetail update(Long userId, Long postId, String type, String title, String content,
            Long categoryId, List<String> tags, Long version) {
        Post post = findOwnPost(postId, userId);

        // Kiểm tra có ai sửa bài này trước đó chưa
        if (version != null && !version.equals(post.getVersion())) {
            throw new ApiException(HttpStatus.CONFLICT, "Bài viết vừa được thay đổi ở nơi khác. Hãy tải lại trang.");
        }

        Category category = findActiveCategory(categoryId);

        post.setPostType(type);
        post.setTitle(title.trim());
        post.setContent(content.trim());
        post.setSummary(makeExcerpt(content, 300));
        postRepository.saveAndFlush(post);

        // Xoá chủ đề và nhãn cũ rồi thêm lại
        jdbcTemplate.update("DELETE FROM post_categories WHERE post_id = ?", postId);
        insertPostCategory(postId, category.getId());

        jdbcTemplate.update("DELETE FROM post_tags WHERE post_id = ?", postId);
        saveTags(postId, tags);

        return detail(postId, userId, false, null);
    }

    /** Xoá bài (thực chất là chuyển sang ARCHIVED). */
    @Override
    @Transactional
    public void delete(Long userId, boolean moderator, Long postId) {
        Post post = findPublishedPost(postId);

        boolean isAuthor = post.getAuthor().getId().equals(userId);
        if (!moderator && !isAuthor) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể xóa bài viết của mình");
        }

        post.setStatus(PostStatus.ARCHIVED.name());
        postRepository.saveAndFlush(post);
    }

    // =========================================================
    // PHẦN 3: THÍCH / LƯU BÀI VIẾT
    // =========================================================

    @Override
    @Transactional
    public Counter like(Long userId, Long postId, boolean liked) {
        requireVisiblePost(postId, userId);

        if (liked) {
            long existing = JdbcUtils.queryCount(jdbcTemplate,
                    "SELECT COUNT(*) FROM post_likes WHERE post_id = ? AND user_id = ?", postId, userId);
            if (existing == 0) {
                jdbcTemplate.update("INSERT INTO post_likes (created_at, updated_at, version, post_id, user_id)"
                        + " VALUES (?, ?, 0, ?, ?)", TimeUtils.nowTimestamp(), TimeUtils.nowTimestamp(), postId, userId);
            }
        } else {
            jdbcTemplate.update("DELETE FROM post_likes WHERE post_id = ? AND user_id = ?", postId, userId);
        }

        long totalLikes = JdbcUtils.queryCount(jdbcTemplate,
                "SELECT COUNT(*) FROM post_likes WHERE post_id = ?", postId);
        return new Counter(liked, totalLikes);
    }

    @Override
    @Transactional
    public Counter bookmark(Long userId, Long postId, boolean saved) {
        requireVisiblePost(postId, userId);

        if (saved) {
            long existing = JdbcUtils.queryCount(jdbcTemplate,
                    "SELECT COUNT(*) FROM bookmarks WHERE post_id = ? AND user_id = ?", postId, userId);
            if (existing == 0) {
                jdbcTemplate.update("INSERT INTO bookmarks (created_at, updated_at, version, user_id, post_id)"
                        + " VALUES (?, ?, 0, ?, ?)", TimeUtils.nowTimestamp(), TimeUtils.nowTimestamp(), userId, postId);
            }
        } else {
            jdbcTemplate.update("DELETE FROM bookmarks WHERE post_id = ? AND user_id = ?", postId, userId);
        }

        long totalSaves = JdbcUtils.queryCount(jdbcTemplate,
                "SELECT COUNT(*) FROM bookmarks WHERE post_id = ?", postId);
        return new Counter(saved, totalSaves);
    }

    // =========================================================
    // PHẦN 4: TÌM / KIỂM TRA DỮ LIỆU
    // =========================================================

    @Override
    public Post requireVisiblePost(Long postId, Long viewerId) {
        long me = viewerId != null ? viewerId : AppConstants.GUEST_USER_ID;
        String sql = "SELECT COUNT(*) FROM posts p JOIN users u ON u.id = p.author_id WHERE p.id = ? AND "
                + VISIBLE_CONDITION;
        if (JdbcUtils.queryCount(jdbcTemplate, sql, postId, me, me) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết");
        }
        return postRepository.getReferenceById(postId);
    }

    private Post findPublishedPost(Long postId) {
        return postRepository.findById(postId)
                .filter(post -> PostStatus.PUBLISHED.is(post.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"));
    }

    /** Tìm bài viết của chính mình (để sửa). */
    private Post findOwnPost(Long postId, Long userId) {
        Post post = findPublishedPost(postId);
        if (!post.getAuthor().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể sửa bài viết của mình");
        }
        return post;
    }

    private Category findActiveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Chủ đề không hợp lệ"));
    }

    // =========================================================
    // PHẦN 5: CHUYỂN DỮ LIỆU
    // =========================================================

    /** Đọc 1 dòng kết quả SQL thành PostRow. */
    private PostRow mapPostRow(ResultSet rs, int rowNumber) throws SQLException {
        return new PostRow(
                rs.getLong("id"),
                rs.getString("post_type"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("body"),
                JdbcUtils.readTime(rs, "published_at"),
                JdbcUtils.readTime(rs, "updated_at"),
                rs.getLong("version"),
                AuthorMapper.fromResultSet(rs),
                JdbcUtils.readNullableLong(rs, "category_id"),
                rs.getLong("likes"),
                rs.getLong("comment_count"),
                rs.getLong("views"),
                rs.getLong("liked") > 0,
                rs.getLong("saved") > 0);
    }

    /** Chuyển danh sách PostRow -> PostSummary (kèm nhãn và tên chủ đề). */
    private List<PostSummary> toSummaries(List<PostRow> rows, Long viewerId) {
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<String>> tagsByPost = loadTags(rows);

        // Map: id chủ đề -> tên chủ đề
        Map<Long, String> categoryNames = new HashMap<>();
        for (Category category : categoryRepository.findAll()) {
            categoryNames.put(category.getId(), category.getName());
        }

        List<PostSummary> result = new ArrayList<>();
        for (PostRow row : rows) {
            // Ưu tiên phần tóm tắt, không có thì dùng nội dung
            String text = (row.summary() == null || row.summary().isBlank()) ? row.body() : row.summary();
            String categoryName = row.categoryId() != null ? categoryNames.get(row.categoryId()) : null;
            List<String> tags = tagsByPost.getOrDefault(row.id(), List.of());
            boolean mine = viewerId != null && viewerId.equals(row.author().id());

            result.add(new PostSummary(row.id(), row.type(), row.title(), makeExcerpt(text, 220),
                    row.publishedAt(), row.author(), row.categoryId(), categoryName, tags,
                    row.likes(), row.comments(), row.views(), row.liked(), row.saved(), mine));
        }
        return result;
    }

    /** Lấy nhãn của nhiều bài viết bằng 1 câu query (tránh N+1 query). */
    private Map<Long, List<String>> loadTags(List<PostRow> rows) {
        List<Object> postIds = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        for (PostRow row : rows) {
            postIds.add(row.id());
            placeholders.add("?");
        }

        String sql = "SELECT pt.post_id, t.name FROM post_tags pt JOIN tags t ON t.id = pt.tag_id"
                + " WHERE pt.post_id IN (" + String.join(",", placeholders) + ") ORDER BY pt.id";

        Map<Long, List<String>> tagsByPost = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            Long postId = rs.getLong(1);
            String tagName = rs.getString(2);
            tagsByPost.computeIfAbsent(postId, key -> new ArrayList<>()).add(tagName);
        }, postIds.toArray());
        return tagsByPost;
    }

    // =========================================================
    // PHẦN 6: GHI DỮ LIỆU PHỤ
    // =========================================================

    private void insertPostCategory(Long postId, Long categoryId) {
        jdbcTemplate.update("INSERT INTO post_categories (created_at, updated_at, version, post_id, category_id)"
                + " VALUES (?, ?, 0, ?, ?)", TimeUtils.nowTimestamp(), TimeUtils.nowTimestamp(), postId, categoryId);
    }

    /**
     * Ghi nhận lượt xem:
     * - Người đã đăng nhập: mỗi người chỉ tính 1 lượt cho mỗi bài
     * - Khách: tính theo mã khách (đã băm SHA-256) + ngày, mỗi ngày tối đa 1 lượt
     */
    private void recordView(Long postId, Long viewerId, String visitorKey) {
        Timestamp now = TimeUtils.nowTimestamp();

        if (viewerId != null) {
            long existing = JdbcUtils.queryCount(jdbcTemplate,
                    "SELECT COUNT(*) FROM post_views WHERE post_id = ? AND viewer_id = ?", postId, viewerId);
            if (existing == 0) {
                jdbcTemplate.update("INSERT INTO post_views"
                        + " (created_at, updated_at, version, post_id, viewer_id, viewed_at)"
                        + " VALUES (?, ?, 0, ?, ?, ?)", now, now, postId, viewerId, now);
            }
            return;
        }

        if (visitorKey == null) {
            return;
        }

        String visitorHash = sha256(visitorKey + "|" + LocalDate.now(ZoneOffset.UTC));
        long existing = JdbcUtils.queryCount(jdbcTemplate,
                "SELECT COUNT(*) FROM post_views WHERE post_id = ? AND visitor_hash = ?", postId, visitorHash);
        if (existing == 0) {
            jdbcTemplate.update("INSERT INTO post_views"
                    + " (created_at, updated_at, version, post_id, visitor_hash, viewed_at)"
                    + " VALUES (?, ?, 0, ?, ?, ?)", now, now, postId, visitorHash, now);
        }
    }

    /**
     * Gắn nhãn cho bài viết. Nhãn chưa có sẽ được tạo mới.
     * Nhãn được chuẩn hoá, bỏ trùng và giới hạn tối đa MAX_POST_TAGS nhãn.
     */
    private void saveTags(Long postId, List<String> rawTags) {
        // Bước 1: Chuẩn hoá danh sách nhãn
        List<String> tagNames = new ArrayList<>();
        if (rawTags != null) {
            for (String raw : rawTags) {
                String name = normalizeTag(raw);
                if (!name.isBlank() && !tagNames.contains(name) && tagNames.size() < AppConstants.MAX_POST_TAGS) {
                    tagNames.add(name);
                }
            }
        }

        // Bước 2: Lưu từng nhãn
        for (String name : tagNames) {
            Long tagId = findOrCreateTag(name);
            jdbcTemplate.update("INSERT INTO post_tags (created_at, updated_at, version, post_id, tag_id)"
                    + " VALUES (?, ?, 0, ?, ?)", TimeUtils.nowTimestamp(), TimeUtils.nowTimestamp(), postId, tagId);
        }
    }

    /** Tìm id nhãn theo tên; chưa có thì tạo nhãn mới với slug không trùng. */
    private Long findOrCreateTag(String name) {
        List<Long> existingIds = jdbcTemplate.queryForList("SELECT id FROM tags WHERE LOWER(name) = ?",
                Long.class, name);
        if (!existingIds.isEmpty()) {
            return existingIds.get(0);
        }

        // Tạo slug không trùng: "java", "java-2", "java-3", ...
        String baseSlug = TextUtils.toSlug(name);
        if (baseSlug.isBlank()) {
            baseSlug = "tag";
        }
        String slug = baseSlug;
        int counter = 2;
        while (JdbcUtils.queryCount(jdbcTemplate, "SELECT COUNT(*) FROM tags WHERE slug = ?", slug) > 0) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        jdbcTemplate.update("INSERT INTO tags (created_at, updated_at, version, name, slug) VALUES (?, ?, 0, ?, ?)",
                TimeUtils.nowTimestamp(), TimeUtils.nowTimestamp(), name, slug);
        return jdbcTemplate.queryForObject("SELECT id FROM tags WHERE slug = ?", Long.class, slug);
    }

    // =========================================================
    // PHẦN 7: HÀM TIỆN ÍCH
    // =========================================================

    /**
     * Chuẩn hoá nhãn: bỏ dấu # ở đầu, gộp nhiều khoảng trắng thành 1, chuyển chữ thường.
     * VD: "  #Spring   Boot " -> "spring boot"
     */
    private static String normalizeTag(String tag) {
        return tag.trim()
                .replaceFirst("^#+", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Tạo đoạn trích ngắn từ nội dung Markdown:
     * bỏ khối code ```...```, thay ảnh bằng chữ "[Ảnh]", giữ chữ của link, bỏ ký hiệu định dạng,
     * gộp khoảng trắng và cắt còn tối đa maxLength ký tự (không cắt giữa từ).
     */
    private static String makeExcerpt(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        String flat = text
                .replaceAll("```[\\s\\S]*?```", " ")                        // Bỏ khối code
                .replaceAll("!\\[[^\\]]*]\\([^)]*\\)", " [Ảnh] ")              // ![mô tả](url) -> [Ảnh]
                .replaceAll("\\[([^\\]]*)]\\([^)]*\\)", "$1")                  // [chữ](url)   -> chữ
                .replaceAll("(?m)^\\s{0,3}(#{1,6}|>|[-*+]|\\d+[.)])\\s+", "") // Bỏ #, >, -, 1. ở đầu dòng
                .replaceAll("\\*\\*|__|`", "")                                // Bỏ **, __, `
                .replaceAll("\\s+", " ")                                      // Gộp khoảng trắng
                .trim();

        if (flat.length() <= maxLength) {
            return flat;
        }
        String cut = flat.substring(0, maxLength).replaceFirst("\\s+\\S*$", ""); // Bỏ từ bị cắt dở
        return cut + "…";
    }

    /** Băm chuỗi bằng SHA-256 (để không lưu trực tiếp thông tin của khách). */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
