package com.knowledge.app.model.dto.request.user;
import jakarta.validation.constraints.*;
/**
 * Dữ liệu người dùng gửi lên khi tự đổi mật khẩu.
 */
public record ChangePasswordRequest(@NotBlank @Size(max=72) String currentPassword,
 @NotBlank @Size(min=8,max=72) String newPassword) {}
