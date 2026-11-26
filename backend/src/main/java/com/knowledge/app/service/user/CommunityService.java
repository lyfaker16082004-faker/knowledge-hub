package com.knowledge.app.service.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.entity.User;
import com.knowledge.app.model.entity.UserFollow;
import com.knowledge.app.model.entity.UserProfile;
import com.knowledge.app.model.enums.UserStatus;
import com.knowledge.app.repository.UserFollowRepository;
import com.knowledge.app.repository.UserProfileRepository;
import com.knowledge.app.repository.UserRepository;

/**
 * Chức năng cộng đồng: danh sách thành viên, theo dõi (follow), bảng tin bài viết.
 */
@Service
@Transactional(readOnly = true)
public class CommunityService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserFollowRepository userFollowRepository;

    public CommunityService(UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserFollowRepository userFollowRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userFollowRepository = userFollowRepository;
    }

    /** Thông tin một thành viên. */
    public record Member(Long id, String username, String displayName, String bio, String avatarUrl,
            String institutionName, String facultyName, long followerCount, long followingCount,
            boolean following) {
    }


    // =========================================================
    // THÀNH VIÊN
    // =========================================================

    /** Xem thông tin một thành viên. */
    public Member get(Long userId, Long viewerId) {
        return toMember(findActiveUser(userId), viewerId);
    }

    /**
     * Danh sách người mà "ownerId" đang theo dõi.
     * viewerId là người đang xem (để biết mình đã theo dõi từng người trong danh sách chưa).
     */
    public PageResponse<Member> following(Long ownerId, Long viewerId, int page, int size) {
        findActiveUser(ownerId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<UserFollow> result = userFollowRepository.followingPage(ownerId, pageable);

        List<Member> items = new ArrayList<>();
        for (UserFollow follow : result.getContent()) {
            items.add(toMember(follow.getFollowing(), viewerId));
        }
        return new PageResponse<>(items, result.getTotalElements(), result.getTotalPages(), page, size);
    }

    /** Danh sách người đang theo dõi "ownerId". */
    public PageResponse<Member> followers(Long ownerId, Long viewerId, int page, int size) {
        findActiveUser(ownerId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<UserFollow> result = userFollowRepository.followersPage(ownerId, pageable);

        List<Member> items = new ArrayList<>();
        for (UserFollow follow : result.getContent()) {
            items.add(toMember(follow.getFollower(), viewerId));
        }
        return new PageResponse<>(items, result.getTotalElements(), result.getTotalPages(), page, size);
    }

    /**
     * Theo dõi hoặc bỏ theo dõi một thành viên.
     *
     * @param enabled true = theo dõi, false = bỏ theo dõi
     */
    @Transactional
    public Member follow(Long viewerId, Long targetId, boolean enabled) {
        if (viewerId.equals(targetId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bạn không thể theo dõi chính mình");
        }

        User target = findActiveUser(targetId);
        Optional<UserFollow> existing = userFollowRepository.findByFollowerIdAndFollowingId(viewerId, targetId);

        if (enabled) {
            // Chỉ tạo mới nếu chưa theo dõi
            if (existing.isEmpty()) {
                UserFollow follow = new UserFollow();
                follow.setFollower(findActiveUser(viewerId));
                follow.setFollowing(target);
                userFollowRepository.saveAndFlush(follow);
            }
        } else {
            if (existing.isPresent()) {
                userFollowRepository.delete(existing.get());
            }
            userFollowRepository.flush();
        }

        return toMember(target, viewerId);
    }

    /** Chuyển User -> Member (kèm thông tin hồ sơ và số người theo dõi). */
    private Member toMember(User user, Long viewerId) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);

        String displayName = null;
        String bio = null;
        String avatarUrl = null;
        String institutionName = null;
        String facultyName = null;

        if (profile != null) {
            displayName = profile.getDisplayName();
            bio = profile.getBio();
            avatarUrl = profile.getAvatarUrl();
            if (profile.getInstitution() != null) {
                institutionName = profile.getInstitution().getName();
            }
            if (profile.getFaculty() != null) {
                facultyName = profile.getFaculty().getName();
            }
        }

        long followerCount = userFollowRepository.followers(user.getId());
        long followingCount = userFollowRepository.following(user.getId());

        // Người đang xem có theo dõi thành viên này không?
        boolean isFollowing = viewerId != null
                && userFollowRepository.existsByFollowerIdAndFollowingId(viewerId, user.getId());

        return new Member(user.getId(), user.getUsername(), displayName, bio, avatarUrl,
                institutionName, facultyName, followerCount, followingCount, isFollowing);
    }

    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> UserStatus.ACTIVE.is(user.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy thành viên"));
    }
}
