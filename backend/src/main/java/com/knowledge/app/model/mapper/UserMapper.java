package com.knowledge.app.model.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.knowledge.app.model.dto.response.user.UserResponse;
import com.knowledge.app.model.entity.User;
import com.knowledge.app.model.entity.UserRole;
import com.knowledge.app.repository.UserRoleRepository;
import com.knowledge.app.service.user.ProfileService;

/**
 * Chuyển entity User sang UserResponse (kèm vai trò và thông tin hồ sơ).
 */
@Component
public class UserMapper {

    private final UserRoleRepository userRoleRepository;
    private final ProfileService profileService;

    public UserMapper(UserRoleRepository userRoleRepository, ProfileService profileService) {
        this.userRoleRepository = userRoleRepository;
        this.profileService = profileService;
    }

    public UserResponse toResponse(User user) {
        ProfileService.ProfileDetails profile = profileService.details(user.getId());

        // Danh sách tên vai trò, sắp xếp theo bảng chữ cái
        List<String> roleNames = new ArrayList<>();
        for (UserRole userRole : userRoleRepository.findWithRoles(user.getId())) {
            roleNames.add(userRole.getRole().getName());
        }
        roleNames.sort(null);

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                roleNames,
                user.getCreatedAt(),
                user.getLastLoginAt(),
                profileService.organization(user.getId()),
                profile.displayName(),
                profile.bio(),
                profile.avatarUrl());
    }
}
