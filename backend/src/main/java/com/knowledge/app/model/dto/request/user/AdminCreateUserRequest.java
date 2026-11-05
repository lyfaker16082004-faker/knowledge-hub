package com.knowledge.app.model.dto.request.user;
import java.util.Set;
import jakarta.validation.constraints.*;
public record AdminCreateUserRequest(
 @NotBlank @Pattern(regexp="[a-zA-Z0-9_.-]{3,50}") String username,
 @NotBlank @Email @Size(max=254) String email,
 @NotBlank @Size(min=8,max=72) String password,
 @NotEmpty Set<@Pattern(regexp="USER|MODERATOR|ADMIN") String> roles) {}
