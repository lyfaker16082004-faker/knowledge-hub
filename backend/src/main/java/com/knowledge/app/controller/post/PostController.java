package com.knowledge.app.controller.post;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.model.dto.response.post.AuthorStats;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.dto.request.post.PostRequest;
import com.knowledge.app.model.dto.request.post.ToggleRequest;
import com.knowledge.app.model.dto.response.post.Counter;
import com.knowledge.app.model.dto.response.post.PostDetail;
import com.knowledge.app.model.dto.response.post.PostSummary;
import com.knowledge.app.model.dto.response.post.Sidebar;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.post.PostService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * API bài viết của trang hỏi đáp cộng đồng.
 * API bình luận nằm ở controller/comment/CommentController.
 */
@RestController
@Validated
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /** Bảng tin bài viết. */
    @GetMapping("/api/posts")
    public ApiResponse<PageResponse<PostSummary>> feed(@AuthenticationPrincipal AccountPrincipal viewer,
            @RequestParam(defaultValue = "newest")
            @Pattern(regexp = "newest|popular|following|unanswered|saved|mine") String tab,
            @RequestParam(defaultValue = "") @Size(max = 150) String q,
            @RequestParam(required = false) @Positive Long category,
            @RequestParam(defaultValue = "") @Size(max = 40) String tag,
            @RequestParam(defaultValue = "") @Pattern(regexp = "|QUESTION|ARTICLE") String type,
            @RequestParam(required = false) @Positive Long author,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int size) {
        Long viewerId = AccountPrincipal.idOf(viewer);
        return ApiResponse.ok(postService.feed(viewerId, tab, q, category, tag, type, author, page, size));
    }

    /** Thống kê hoạt động cộng đồng của một thành viên. */
    @GetMapping("/api/posts/authors/{id}/stats")
    public ApiResponse<AuthorStats> authorStats(@PathVariable Long id) {
        return ApiResponse.ok(postService.authorStats(id));
    }

    /** Dữ liệu cột bên (chủ đề, nhãn thịnh hành, thành viên tích cực). */
    @GetMapping("/api/posts/sidebar")
    public ApiResponse<Sidebar> sidebar() {
        return ApiResponse.ok(postService.sidebar());
    }

    /** Chi tiết bài viết. */
    @GetMapping("/api/posts/{id}")
    public ApiResponse<PostDetail> detail(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal viewer,
            HttpServletRequest request) {
        // Mã nhận diện khách (IP + trình duyệt) dùng để đếm lượt xem
        String visitorKey = request.getRemoteAddr() + "|" + String.valueOf(request.getHeader("User-Agent"));
        PostDetail detail = postService.detail(id, AccountPrincipal.idOf(viewer),
                AccountPrincipal.isModerator(viewer), visitorKey);
        return ApiResponse.ok(detail);
    }

    @PostMapping("/api/posts")
    public ApiResponse<PostDetail> create(@AuthenticationPrincipal AccountPrincipal viewer,
            @Valid @RequestBody PostRequest body) {
        PostDetail created = postService.create(viewer.getId(), body.type(), body.title(), body.content(),
                body.categoryId(), body.tags());
        return ApiResponse.ok(created);
    }

    @PutMapping("/api/posts/{id}")
    public ApiResponse<PostDetail> update(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal viewer,
            @Valid @RequestBody PostRequest body) {
        PostDetail updated = postService.update(viewer.getId(), id, body.type(), body.title(), body.content(),
                body.categoryId(), body.tags(), body.version());
        return ApiResponse.ok(updated);
    }

    @DeleteMapping("/api/posts/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal viewer) {
        postService.delete(viewer.getId(), viewer.isModerator(), id);
        return ApiResponse.ok(null);
    }

    /** Thích / bỏ thích. */
    @PostMapping("/api/posts/{id}/like")
    public ApiResponse<Counter> like(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal viewer,
            @Valid @RequestBody ToggleRequest body) {
        return ApiResponse.ok(postService.like(viewer.getId(), id, body.active()));
    }

    /** Lưu / bỏ lưu. */
    @PostMapping("/api/posts/{id}/bookmark")
    public ApiResponse<Counter> bookmark(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal viewer,
            @Valid @RequestBody ToggleRequest body) {
        return ApiResponse.ok(postService.bookmark(viewer.getId(), id, body.active()));
    }
}
