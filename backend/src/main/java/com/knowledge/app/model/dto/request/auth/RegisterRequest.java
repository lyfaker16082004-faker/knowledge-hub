package com.knowledge.app.model.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đăng ký tài khoản.
 * username có thể bỏ trống (hệ thống sẽ tự sinh).
 */
public record RegisterRequest(
        @Pattern(regexp = "[a-zA-Z0-9_.-]{3,50}", message = "Tên đăng nhập gồm 3–50 chữ, số hoặc _ . -")
        String username,

        @NotBlank @Email @Size(max = 254)
        String email,

        @NotBlank @Size(min = 8, max = 72)
        String password,

        @Positive Long institutionId,
        @Positive Long facultyId) {
}
