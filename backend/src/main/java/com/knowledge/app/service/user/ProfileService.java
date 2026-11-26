package com.knowledge.app.service.user;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.model.dto.request.user.UpdateProfileRequest;
import com.knowledge.app.model.dto.response.user.OrganizationInfo;
import com.knowledge.app.model.entity.User;
import com.knowledge.app.model.entity.UserProfile;
import com.knowledge.app.repository.UserProfileRepository;
import com.knowledge.app.repository.UserRepository;
import com.knowledge.app.service.organization.OrganizationService;

/**
 * Quản lý hồ sơ cá nhân: tên hiển thị, giới thiệu, ảnh đại diện, trường/khoa.
 */
@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final AvatarStorage avatarStorage;

    public ProfileService(UserProfileRepository userProfileRepository,
            UserRepository userRepository,
            OrganizationService organizationService,
            AvatarStorage avatarStorage) {
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
        this.organizationService = organizationService;
        this.avatarStorage = avatarStorage;
    }

    /** Thông tin cơ bản của hồ sơ. */
    public record ProfileDetails(String displayName, String bio, String avatarUrl) {
    }

    /** Lấy thông tin hồ sơ. Nếu chưa có hồ sơ thì trả về các giá trị null. */
    public ProfileDetails details(Long userId) {
        Optional<UserProfile> profile = userProfileRepository.findByUserId(userId);
        if (profile.isEmpty()) {
            return new ProfileDetails(null, null, null);
        }
        UserProfile p = profile.get();
        return new ProfileDetails(p.getDisplayName(), p.getBio(), p.getAvatarUrl());
    }

    /** Lấy thông tin trường/khoa trong hồ sơ. Chưa có hồ sơ thì trả về null. */
    public OrganizationInfo organization(Long userId) {
        Optional<UserProfile> profile = userProfileRepository.findByUserId(userId);
        if (profile.isEmpty()) {
            return null;
        }
        return organizationService.info(profile.get().getInstitution(), profile.get().getFaculty());
    }

    /** Kiểm tra trường/khoa có hợp lệ không (ném lỗi nếu sai). */
    public void validateOrganization(Long institutionId, Long facultyId) {
        organizationService.resolve(institutionId, facultyId);
    }

    /**
     * Cập nhật ảnh đại diện.
     * Ảnh cũ chỉ bị xoá nếu không còn hồ sơ nào dùng nó.
     */
    @Transactional
    public void avatar(Long userId, MultipartFile file) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(userRepository.getReferenceById(userId));
        }

        String oldAvatarUrl = profile.getAvatarUrl();

        String newAvatarUrl = avatarStorage.save(file);
        profile.setAvatarUrl(newAvatarUrl);
        userProfileRepository.saveAndFlush(profile);

        if (oldAvatarUrl != null && userProfileRepository.countByAvatarUrl(oldAvatarUrl) == 0) {
            avatarStorage.deleteAfterCommit(oldAvatarUrl);
        }
    }

    /**
     * Cập nhật hồ sơ. Trường nào null (displayName, bio) thì giữ nguyên giá trị cũ.
     */
    @Transactional
    public void update(Long userId, UpdateProfileRequest request) {
        OrganizationService.Selection organization =
                organizationService.resolve(request.institutionId(), request.facultyId());

        // Lấy hồ sơ hiện có, chưa có thì tạo mới
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
            profile = new UserProfile();
            profile.setUser(user);
        }

        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        profile.setInstitution(organization.institution());
        profile.setFaculty(organization.faculty());

        userProfileRepository.saveAndFlush(profile);
    }
}
