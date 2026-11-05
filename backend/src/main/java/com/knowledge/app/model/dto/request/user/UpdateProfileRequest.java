package com.knowledge.app.model.dto.request.user;
import jakarta.validation.constraints.*;
public record UpdateProfileRequest(@Size(max=100) String displayName,@Size(max=2000) String bio,@Positive Long institutionId,@Positive Long facultyId) {}
