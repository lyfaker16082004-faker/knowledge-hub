package com.knowledge.app.model.dto.response.user;

/**
 * Thông tin trường / khoa của người dùng.
 */
public record OrganizationInfo(
        Long institutionId,
        String institutionName,
        String institutionType, // UNIVERSITY, COMPANY, OTHER
        Long facultyId,
        String facultyName) {
}
