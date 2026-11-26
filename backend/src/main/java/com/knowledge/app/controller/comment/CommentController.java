package com.knowledge.app.controller.comment;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.model.dto.request.comment.CommentRequest;
import com.knowledge.app.model.dto.request.comment.VoteRequest;
import com.knowledge.app.model.dto.response.comment.CommentItem;
import com.knowledge.app.model.dto.response.comment.VoteResult;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.comment.CommentService;

import jakarta.validation.Valid;

/**
 * API bình luận của bài viết.
 */
@RestController
@Validated
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** Danh sách bình luận của bài viết. */
    @GetMapping("/api/posts/{postId}/comments")
    public ApiResponse<List<CommentItem>> comments(@PathVariable Long postId,
            @AuthenticationPrincipal AccountPrincipal viewer) {
        List<CommentItem> items = commentService.comments(postId, AccountPrincipal.idOf(viewer),
                AccountPrincipal.isModerator(viewer));
        return ApiResponse.ok(items);
    }

    /** Viết bình luận (hoặc trả lời một bình luận khác nếu có parentId). */
    @PostMapping("/api/posts/{postId}/comments")
    public ApiResponse<CommentItem> comment(@PathVariable Long postId,
            @AuthenticationPrincipal AccountPrincipal viewer, @Valid @RequestBody CommentRequest body) {
        return ApiResponse.ok(commentService.comment(viewer.getId(), postId, body.parentId(), body.content()));
    }

    /** Sửa bình luận của mình. */
    @PutMapping("/api/comments/{id}")
    public ApiResponse<CommentItem> editComment(@PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal viewer, @Valid @RequestBody CommentRequest body) {
        return ApiResponse.ok(commentService.editComment(viewer.getId(), id, body.content()));
    }

    /** Xoá bình luận. */
    @DeleteMapping("/api/comments/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal viewer) {
        commentService.deleteComment(viewer.getId(), viewer.isModerator(), id);
        return ApiResponse.ok(null);
    }

    /** Bình chọn bình luận. */
    @PostMapping("/api/comments/{id}/vote")
    public ApiResponse<VoteResult> vote(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal viewer,
            @Valid @RequestBody VoteRequest body) {
        return ApiResponse.ok(commentService.vote(viewer.getId(), id, body.value()));
    }
}
