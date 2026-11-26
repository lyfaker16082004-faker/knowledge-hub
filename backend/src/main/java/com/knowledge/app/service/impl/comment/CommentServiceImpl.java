package com.knowledge.app.service.impl.comment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowledge.app.common.constant.AppConstants;
import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.util.JdbcUtils;
import com.knowledge.app.common.util.TimeUtils;
import com.knowledge.app.model.dto.response.comment.CommentItem;
import com.knowledge.app.model.dto.response.comment.VoteResult;
import com.knowledge.app.model.dto.response.post.Author;
import com.knowledge.app.model.entity.Comment;
import com.knowledge.app.model.entity.Post;
import com.knowledge.app.model.enums.CommentStatus;
import com.knowledge.app.model.mapper.AuthorMapper;
import com.knowledge.app.repository.CommentRepository;
import com.knowledge.app.repository.UserRepository;
import com.knowledge.app.service.comment.CommentService;
import com.knowledge.app.service.post.PostService;

/**
 * Xử lý bình luận nhiều cấp và bình chọn bình luận.
 */
@Service
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    /** Lấy bình luận kèm tác giả, tổng điểm và bình chọn của người xem (tham số: id người xem, id bài). */
    private static final String COMMENT_QUERY = """
            SELECT c.id, c.parent_id, c.content, c.status, c.created_at, c.edited_at,
                   u.id AS author_id, u.username, pr.display_name, pr.avatar_url,
                   (SELECT COALESCE(SUM(cl.vote_value), 0) FROM comment_likes cl
                    WHERE cl.comment_id = c.id) AS score,
                   (SELECT COALESCE(MAX(cl.vote_value), 0) FROM comment_likes cl
                    WHERE cl.comment_id = c.id AND cl.user_id = ?) AS my_vote
            FROM comments c
            JOIN users u ON u.id = c.author_id
            LEFT JOIN user_profiles pr ON pr.user_id = u.id
            WHERE c.post_id = ? AND c.status IN ('VISIBLE', 'HIDDEN', 'DELETED')
            ORDER BY c.created_at, c.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostService postService;

    public CommentServiceImpl(JdbcTemplate jdbcTemplate,
            CommentRepository commentRepository,
            UserRepository userRepository,
            PostService postService) {
        this.jdbcTemplate = jdbcTemplate;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postService = postService;
    }

    // =========================================================
    // ĐỌC BÌNH LUẬN
    // =========================================================

    @Override
    public List<CommentItem> comments(Long postId, Long viewerId, boolean moderator) {
        postService.requireVisiblePost(postId, viewerId);
        long me = viewerId != null ? viewerId : AppConstants.GUEST_USER_ID;

        List<CommentItem> items = jdbcTemplate.query(COMMENT_QUERY, (rs, rowNumber) -> {
            // Bình luận đã xoá hoặc bị quản trị viên ẩn đều hiện dạng "chỗ trống"
            boolean deleted = !CommentStatus.VISIBLE.is(rs.getString("status"));
            long authorId = rs.getLong("author_id");

            // parent_id = null (hoặc 0) nghĩa là bình luận gốc
            Long parentId = JdbcUtils.readNullableLong(rs, "parent_id");
            if (parentId != null && parentId == 0) {
                parentId = null;
            }

            // Bình luận đã xoá thì ẩn tác giả và nội dung
            Author author = deleted ? null : AuthorMapper.fromResultSet(rs);
            String content = deleted ? null : rs.getString("content");

            boolean mine = !deleted && authorId == me;
            boolean canDelete = !deleted && (authorId == me || moderator);

            return new CommentItem(rs.getLong("id"), parentId, author, content, deleted,
                    JdbcUtils.readTime(rs, "created_at"), JdbcUtils.readTime(rs, "edited_at"),
                    rs.getLong("score"), rs.getInt("my_vote"), mine, canDelete);
        }, me, postId);

        return removeDeletedLeaves(items);
    }

    // =========================================================
    // VIẾT / SỬA / XOÁ BÌNH LUẬN
    // =========================================================

    @Override
    @Transactional
    public CommentItem comment(Long userId, Long postId, Long parentId, String content) {
        Post post = postService.requireVisiblePost(postId, userId);
        Comment comment = new Comment();

        if (parentId != null) {
            // Bình luận cha phải thuộc cùng bài viết và chưa bị xoá
            Comment parent = commentRepository.findById(parentId)
                    .filter(c -> c.getPost().getId().equals(postId) && CommentStatus.VISIBLE.is(c.getStatus()))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "Bình luận bạn trả lời không còn tồn tại"));
            comment.setParent(parent);
        }

        comment.setPost(post);
        comment.setAuthor(userRepository.getReferenceById(userId));
        comment.setContent(content.trim());
        comment.setStatus(CommentStatus.VISIBLE.name());
        commentRepository.saveAndFlush(comment);

        return findCommentItem(comment.getId(), postId, userId);
    }

    @Override
    @Transactional
    public CommentItem editComment(Long userId, Long commentId, String content) {
        Comment comment = findVisibleComment(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể sửa bình luận của mình");
        }

        comment.setContent(content.trim());
        comment.setEditedAt(TimeUtils.now());
        commentRepository.saveAndFlush(comment);

        return findCommentItem(commentId, comment.getPost().getId(), userId);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, boolean moderator, Long commentId) {
        Comment comment = findVisibleComment(commentId);

        boolean isAuthor = comment.getAuthor().getId().equals(userId);
        if (!moderator && !isAuthor) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể xóa bình luận của mình");
        }

        comment.setStatus(CommentStatus.DELETED.name());
        commentRepository.saveAndFlush(comment);
    }

    // =========================================================
    // BÌNH CHỌN
    // =========================================================

    @Override
    @Transactional
    public VoteResult vote(Long userId, Long commentId, int value) {
        Comment comment = findVisibleComment(commentId);

        if (comment.getAuthor().getId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bạn không thể bình chọn cho bình luận của mình");
        }

        // Xoá bình chọn cũ (nếu có) rồi thêm bình chọn mới
        jdbcTemplate.update("DELETE FROM comment_likes WHERE comment_id = ? AND user_id = ?", commentId, userId);
        if (value != 0) {
            jdbcTemplate.update("INSERT INTO comment_likes"
                    + " (created_at, updated_at, version, comment_id, user_id, vote_value)"
                    + " VALUES (?, ?, 0, ?, ?, ?)",
                    TimeUtils.nowTimestamp(), TimeUtils.nowTimestamp(), commentId, userId, (short) value);
        }

        long score = JdbcUtils.queryCount(jdbcTemplate,
                "SELECT COALESCE(SUM(vote_value), 0) FROM comment_likes WHERE comment_id = ?", commentId);
        return new VoteResult(value, score);
    }

    // =========================================================
    // HÀM HỖ TRỢ
    // =========================================================

    private Comment findVisibleComment(Long commentId) {
        return commentRepository.findById(commentId)
                .filter(comment -> CommentStatus.VISIBLE.is(comment.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bình luận"));
    }

    /** Đọc lại một bình luận (dạng DTO) sau khi tạo / sửa. */
    private CommentItem findCommentItem(Long commentId, Long postId, Long viewerId) {
        for (CommentItem item : comments(postId, viewerId, false)) {
            if (item.id().equals(commentId)) {
                return item;
            }
        }
        throw new NoSuchElementException("Comment not found");
    }

    /**
     * Bỏ các bình luận đã xoá mà không còn câu trả lời nào.
     * (Nếu bình luận đã xoá vẫn còn trả lời thì giữ lại làm "chỗ trống" để cây bình luận không bị đứt.)
     * Lặp lại nhiều lần vì sau khi xoá một lá, cha của nó có thể trở thành lá mới.
     */
    private static List<CommentItem> removeDeletedLeaves(List<CommentItem> items) {
        List<CommentItem> list = new ArrayList<>(items);
        boolean changed = true;

        while (changed) {
            // Tập id các bình luận đang có con
            Set<Long> parentIds = new HashSet<>();
            for (CommentItem item : list) {
                if (item.parentId() != null) {
                    parentIds.add(item.parentId());
                }
            }
            changed = list.removeIf(item -> item.deleted() && !parentIds.contains(item.id()));
        }
        return list;
    }
}
