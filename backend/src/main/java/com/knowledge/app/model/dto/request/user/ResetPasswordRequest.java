package com.knowledge.app.model.dto.request.user;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(@NotBlank @Size(min=8,max=72) String password) {}
