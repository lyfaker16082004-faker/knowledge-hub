package com.knowledge.app.service.post;

import java.util.List;

import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.dto.response.post.Counter;
import com.knowledge.app.model.dto.response.post.PostDetail;
import com.knowledge.app.model.dto.response.post.PostSummary;
import com.knowledge.app.model.dto.response.post.Sidebar;
import com.knowledge.app.model.entity.Post;

/**
 * Chức năng bài viết của trang hỏi đáp cộng đồng.
 * Code xử lý nằm trong service/impl/post/PostServiceImpl.
 */
public interface PostService {

    /**
     * Bảng tin bài viết.
     *
     * @param tab "following", "saved", "mine", "unanswered", "popular", còn lại = mới nhất
     */
    /** Thống kê hoạt động cộng đồng của một thành viên (bài viết, lượt thích, bình luận nhận được...). */
    com.knowledge.app.model.dto.response.post.AuthorStats authorStats(Long authorId);

    /** authorId khác null: chỉ lấy bài của tác giả đó (trang hồ sơ thành viên). */
    PageResponse<PostSummary> feed(Long viewerId, String tab, String keyword, Long categoryId, String tag,
            String type, Long authorId, int page, int size);

    /** Chi tiết bài viết (đồng thời ghi nhận 1 lượt xem). */
    PostDetail detail(Long postId, Long viewerId, boolean moderator, String visitorKey);

    /** Dữ liệu cột bên: chủ đề, nhãn thịnh hành, thành viên tích cực, tổng số bài. */
    Sidebar sidebar();

    PostDetail create(Long userId, String type, String title, String content, Long categoryId, List<String> tags);

    PostDetail update(Long userId, Long postId, String type, String title, String content, Long categoryId,
            List<String> tags, Long version);

    /** Xoá bài. Moderator được xoá bài của người khác. */
    void delete(Long userId, boolean moderator, Long postId);

    Counter like(Long userId, Long postId, boolean liked);

    Counter bookmark(Long userId, Long postId, boolean saved);

    /**
     * Tìm bài viết mà người dùng được phép xem; không có thì ném lỗi 404.
     * CommentService dùng hàm này trước khi đọc / viết bình luận.
     */
    Post requireVisiblePost(Long postId, Long viewerId);
}
