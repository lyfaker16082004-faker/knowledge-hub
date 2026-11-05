package com.knowledge.app.model.dto.request.auth;
import jakarta.validation.constraints.*;
public record RegisterRequest(
 @Pattern(regexp="[a-zA-Z0-9_.-]{3,50}", message="Tên đăng nhập gồm 3–50 chữ, số hoặc _ . -") String username,
 @NotBlank @Email @Size(max=254) String email,
 @NotBlank @Size(min=8,max=72) String password, @Positive Long institutionId, @Positive Long facultyId) {}
