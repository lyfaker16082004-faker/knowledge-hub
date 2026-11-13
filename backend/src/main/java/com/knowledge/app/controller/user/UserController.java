package com.knowledge.app.controller.user;

import jakarta.validation.Valid;
import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.model.dto.request.user.ChangePasswordRequest;
import com.knowledge.app.model.dto.response.user.UserResponse;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserService users;
    private final com.knowledge.app.service.user.ProfileService profiles;

    public UserController(UserService users, com.knowledge.app.service.user.ProfileService profiles) {
        this.profiles = profiles;
        this.users = users;
    }

    @PostMapping(value = "/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> avatar(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        profiles.avatar(principal.getId(), file);
        return ApiResponse.ok(users.get(principal.getId()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserResponse> profile(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody com.knowledge.app.model.dto.request.user.UpdateProfileRequest request) {
        profiles.update(principal.getId(), request);
        return ApiResponse.ok(users.get(principal.getId()));
    }

    @GetMapping
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.ok(users.get(principal.getId()));
    }

    @PostMapping("/password")
    public ApiResponse<Void> password(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        users.changePassword(principal.getId(), request);
        return new ApiResponse<>(true, "Đã đổi mật khẩu. Vui lòng đăng nhập lại.", null);
    }
}
