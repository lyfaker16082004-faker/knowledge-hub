package com.knowledge.app.controller.user;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.model.dto.request.user.ChangePasswordRequest;
import com.knowledge.app.model.dto.request.user.UpdateProfileRequest;
import com.knowledge.app.model.dto.response.user.UserResponse;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.user.ProfileService;
import com.knowledge.app.service.user.UserService;

import jakarta.validation.Valid;

/**
 * API tài khoản của người dùng đang đăng nhập (/api/users/me).
 */
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;

    public UserController(UserService userService, ProfileService profileService) {
        this.userService = userService;
        this.profileService = profileService;
    }

    /** Xem thông tin tài khoản của tôi. */
    @GetMapping
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.ok(userService.get(principal.getId()));
    }

    /** Đổi ảnh đại diện. */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> avatar(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestPart("file") MultipartFile file) {
        profileService.avatar(principal.getId(), file);
        return ApiResponse.ok(userService.get(principal.getId()));
    }

    /** Cập nhật hồ sơ (tên hiển thị, giới thiệu, trường/khoa). */
    @PutMapping("/profile")
    public ApiResponse<UserResponse> profile(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        profileService.update(principal.getId(), request);
        return ApiResponse.ok(userService.get(principal.getId()));
    }

    /** Đổi mật khẩu. Sau khi đổi, người dùng phải đăng nhập lại. */
    @PostMapping("/password")
    public ApiResponse<Void> password(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return new ApiResponse<>(true, "Đã đổi mật khẩu. Vui lòng đăng nhập lại.", null);
    }
}
