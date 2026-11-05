package com.knowledge.app.model.dto.request.user;
import java.util.Set;
import jakarta.validation.constraints.*;
public record AdminUpdateUserRequest(
 @NotBlank @Email @Size(max=254) String email,
 @NotBlank @Pattern(regexp="ACTIVE|INACTIVE|LOCKED|DELETED") String status,
 @NotEmpty Set<@Pattern(regexp="USER|MODERATOR|ADMIN") String> roles) {}
