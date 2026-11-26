package com.knowledge.app.model.dto.request.user;
import jakarta.validation.constraints.*;
/**
 * Mật khẩu mới do admin đặt lại cho người dùng.
 */
public record ResetPasswordRequest(@NotBlank @Size(min=8,max=72) String password) {}
