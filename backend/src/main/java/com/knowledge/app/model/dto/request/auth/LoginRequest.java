package com.knowledge.app.model.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đăng nhập. "username" có thể là username hoặc email.
 */
public record LoginRequest(
        @NotBlank @Size(max = 254) String username,
        @NotBlank @Size(max = 72) String password) {
}
