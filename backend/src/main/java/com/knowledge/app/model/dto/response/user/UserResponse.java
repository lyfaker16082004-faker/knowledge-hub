package com.knowledge.app.model.dto.response.user;
import java.time.LocalDateTime;
import java.util.List;
public record UserResponse(Long id, String username, String email, String status,
 boolean emailVerified, List<String> roles, LocalDateTime createdAt, LocalDateTime lastLoginAt, OrganizationInfo organization,String displayName,String bio,String avatarUrl) {}
