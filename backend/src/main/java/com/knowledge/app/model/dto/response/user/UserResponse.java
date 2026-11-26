package com.knowledge.app.model.dto.response.user;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin tài khoản trả về cho frontend (không bao gồm mật khẩu).
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String status,                  // ACTIVE, LOCKED, DELETED...
        boolean emailVerified,
        List<String> roles,             // VD: ["ADMIN", "USER"]
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        OrganizationInfo organization,  // Trường / khoa
        String displayName,
        String bio,
        String avatarUrl) {
}
