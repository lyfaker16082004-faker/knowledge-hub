package com.knowledge.app.model.dto.request.auth;

import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank @Size(max = 254) String username, @NotBlank @Size(max = 72) String password) {
}
