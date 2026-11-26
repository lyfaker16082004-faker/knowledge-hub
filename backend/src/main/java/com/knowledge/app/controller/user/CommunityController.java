package com.knowledge.app.controller.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.user.CommunityService;
import com.knowledge.app.service.user.CommunityService.Member;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * API cộng đồng: thành viên, theo dõi, bảng tin.
 */
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    /** following = true để theo dõi, false để bỏ theo dõi. */
    public record FollowRequest(@NotNull Boolean following) {
    }

    // Không có API liệt kê toàn bộ thành viên (giống TikTok):
    // người dùng chỉ xem được danh sách mình theo dõi / người theo dõi mình,
    // còn hồ sơ người khác thì mở từ bài viết, bình luận.

    /** Xem thông tin một thành viên. */
    @GetMapping("/users/{id}")
    public ApiResponse<Member> get(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal viewer) {
        return ApiResponse.ok(communityService.get(id, AccountPrincipal.idOf(viewer)));
    }

    /** Theo dõi / bỏ theo dõi. */
    @PostMapping("/users/{id}/follow")
    public ApiResponse<Member> follow(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal viewer,
            @Valid @RequestBody FollowRequest request) {
        return ApiResponse.ok(communityService.follow(viewer.getId(), id, request.following()));
    }

    /** Những người tôi đang theo dõi. */
    @GetMapping("/following")
    public ApiResponse<PageResponse<Member>> following(@AuthenticationPrincipal AccountPrincipal viewer,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return ApiResponse.ok(communityService.following(viewer.getId(), viewer.getId(), page, size));
    }

    /** Những người đang theo dõi tôi. */
    @GetMapping("/followers")
    public ApiResponse<PageResponse<Member>> followers(@AuthenticationPrincipal AccountPrincipal viewer,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return ApiResponse.ok(communityService.followers(viewer.getId(), viewer.getId(), page, size));
    }

    /** Những người mà một thành viên đang theo dõi (xem trên trang hồ sơ, cần đăng nhập). */
    @GetMapping("/users/{id}/following")
    public ApiResponse<PageResponse<Member>> memberFollowing(@PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal viewer,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return ApiResponse.ok(communityService.following(id, viewer.getId(), page, size));
    }

    /** Những người đang theo dõi một thành viên (xem trên trang hồ sơ, cần đăng nhập). */
    @GetMapping("/users/{id}/followers")
    public ApiResponse<PageResponse<Member>> memberFollowers(@PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal viewer,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return ApiResponse.ok(communityService.followers(id, viewer.getId(), page, size));
    }
}
