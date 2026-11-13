package com.knowledge.app.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.knowledge.app.common.response.*;
import com.knowledge.app.model.dto.request.user.*;
import com.knowledge.app.model.dto.response.user.UserResponse;
import com.knowledge.app.service.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService users;

    public AdminUserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(@RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(users.list(q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(users.get(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<UserResponse> delete(@PathVariable Long id) {
        return new ApiResponse<>(true, "Đã xóa mềm tài khoản và thu hồi đăng nhập", users.softDelete(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ApiResponse.ok(users.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.ok(users.update(id, request));
    }

    @PostMapping("/{id}/password")
    public ApiResponse<Void> password(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        users.resetPassword(id, request);
        return new ApiResponse<>(true, "Đã đặt lại mật khẩu và thu hồi các phiên đăng nhập", null);
    }
}
