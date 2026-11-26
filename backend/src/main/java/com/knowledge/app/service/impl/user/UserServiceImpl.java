package com.knowledge.app.service.impl.user;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.dto.request.user.AdminCreateUserRequest;
import com.knowledge.app.model.dto.request.user.AdminUpdateUserRequest;
import com.knowledge.app.model.dto.request.user.ChangePasswordRequest;
import com.knowledge.app.model.dto.request.user.ResetPasswordRequest;
import com.knowledge.app.model.dto.response.user.UserResponse;
import com.knowledge.app.model.entity.Role;
import com.knowledge.app.model.entity.User;
import com.knowledge.app.model.entity.UserRole;
import com.knowledge.app.model.enums.RoleName;
import com.knowledge.app.model.enums.UserStatus;
import com.knowledge.app.model.mapper.UserMapper;
import com.knowledge.app.repository.RoleRepository;
import com.knowledge.app.repository.UserRepository;
import com.knowledge.app.repository.UserRoleRepository;
import com.knowledge.app.service.user.UserService;

/**
 * Quản lý tài khoản người dùng: tạo, sửa, xoá, đổi mật khẩu.
 *
 * Quy tắc quan trọng: hệ thống luôn phải còn ít nhất 1 admin đang hoạt động.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "MODERATOR", "USER");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    // =========================================================
    // XEM TÀI KHOẢN
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return toResponse(findUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String keyword, int page, int size) {
        Page<User> result = userRepository.search(keyword.trim(),
                PageRequest.of(page, size, Sort.by("id").descending()));

        List<UserResponse> items = new ArrayList<>();
        for (User user : result.getContent()) {
            items.add(toResponse(user));
        }
        return new PageResponse<>(items, result.getTotalElements(), result.getTotalPages(), page, size);
    }

    // =========================================================
    // TẠO / SỬA / XOÁ TÀI KHOẢN
    // =========================================================

    /** Admin tạo tài khoản. */
    @Override
    public UserResponse create(AdminCreateUserRequest request) {
        lockAdminRole();
        return createAccount(request);
    }

    /** Tạo tài khoản (dùng chung cho admin tạo và người dùng tự đăng ký). */
    public UserResponse createAccount(AdminCreateUserRequest request) {
        // Username và email luôn lưu dạng chữ thường
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsernameIgnoreCase(username) || userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Tên đăng nhập hoặc email đã tồn tại");
        }

        validatePassword(request.password());
        List<Role> roles = findRoles(request.roles());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password())); // Mã hoá mật khẩu bằng BCrypt
        user.setStatus(UserStatus.ACTIVE.name());
        user.setEmailVerified(false);
        userRepository.saveAndFlush(user);

        assignRoles(user, roles);
        return toResponse(user);
    }

    /** Admin cập nhật email, trạng thái, vai trò của tài khoản. */
    @Override
    public UserResponse update(Long id, AdminUpdateUserRequest request) {
        lockAdminRole();
        User user = findUser(id);
        List<Role> newRoles = findRoles(request.roles());

        // Không được khoá hoặc gỡ quyền admin cuối cùng
        boolean wasActiveAdmin = isAdmin(id) && UserStatus.ACTIVE.is(user.getStatus());
        boolean willLoseAdmin = !UserStatus.ACTIVE.is(request.status()) || !request.roles().contains(RoleName.ADMIN.name());
        if (wasActiveAdmin && willLoseAdmin && userRoleRepository.countActiveAdmins() <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Phải giữ ít nhất một admin đang hoạt động");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        // Đổi email thì phải xác thực lại
        if (!email.equals(user.getEmail())) {
            user.setEmailVerified(false);
        }

        user.setEmail(email);
        user.setStatus(request.status());
        user.setAuthVersion(user.getAuthVersion() + 1); // Bắt người dùng đăng nhập lại

        // Xoá vai trò cũ rồi gán vai trò mới
        userRoleRepository.removeForUser(id);
        userRoleRepository.flush();
        assignRoles(user, newRoles);

        userRepository.flush();
        return toResponse(user);
    }

    /** Xoá mềm: chỉ đổi trạng thái sang DELETED. */
    @Override
    public UserResponse softDelete(Long id) {
        lockAdminRole();
        User user = findUser(id);

        if (UserStatus.DELETED.is(user.getStatus())) {
            return toResponse(user);
        }

        boolean isActiveAdmin = isAdmin(id) && UserStatus.ACTIVE.is(user.getStatus());
        if (isActiveAdmin && userRoleRepository.countActiveAdmins() <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Phải giữ ít nhất một admin đang hoạt động");
        }

        user.setStatus(UserStatus.DELETED.name());
        user.setAuthVersion(user.getAuthVersion() + 1);
        userRepository.flush();
        return toResponse(user);
    }

    /** Tạo admin đầu tiên khi khởi động (nếu chưa có admin nào). */
    @Override
    public void bootstrapAdmin(String username, String email, String password) {
        lockAdminRole();
        if (userRoleRepository.countActiveAdmins() > 0) {
            return;
        }
        createAccount(new AdminCreateUserRequest(username, email, password, Set.of(RoleName.ADMIN.name(), RoleName.USER.name())));
    }

    // =========================================================
    // MẬT KHẨU
    // =========================================================

    /** Người dùng tự đổi mật khẩu (phải nhập đúng mật khẩu hiện tại). */
    @Override
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = findUser(id);
        String currentPassword = request.currentPassword();

        boolean tooLong = currentPassword.getBytes(StandardCharsets.UTF_8).length > 72;
        if (tooLong || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }

        replacePassword(user, request.newPassword());
    }

    /** Admin đặt lại mật khẩu cho người dùng. */
    @Override
    public void resetPassword(Long id, ResetPasswordRequest request) {
        replacePassword(findUser(id), request.password());
    }

    private void replacePassword(User user, String newPassword) {
        validatePassword(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setAuthVersion(user.getAuthVersion() + 1); // Đăng xuất khỏi mọi thiết bị
    }

    /** Mật khẩu: ít nhất 8 ký tự, tối đa 72 byte (giới hạn của BCrypt). */
    private void validatePassword(String password) {
        boolean invalid = password == null
                || password.isBlank()
                || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72;
        if (invalid) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte UTF-8");
        }
    }

    // =========================================================
    // VAI TRÒ
    // =========================================================

    private boolean isAdmin(Long userId) {
        for (UserRole userRole : userRoleRepository.findWithRoles(userId)) {
            if (RoleName.ADMIN.is(userRole.getRole().getName())) {
                return true;
            }
        }
        return false;
    }

    /** Chuyển danh sách tên vai trò thành danh sách Role trong database. */
    private List<Role> findRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty() || !ALLOWED_ROLES.containsAll(roleNames)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vai trò không hợp lệ");
        }

        List<String> sortedNames = new ArrayList<>(roleNames);
        sortedNames.sort(null);

        List<Role> roles = new ArrayList<>();
        for (String name : sortedNames) {
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Chưa có dữ liệu vai trò. Hãy chạy migration."));
            roles.add(role);
        }
        return roles;
    }

    private void assignRoles(User user, List<Role> roles) {
        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }
        userRoleRepository.flush();
    }

    /**
     * Khoá dòng ADMIN trong bảng roles (SELECT ... FOR UPDATE).
     * Giúp 2 thao tác quản lý admin chạy cùng lúc không làm mất admin cuối cùng.
     */
    private void lockAdminRole() {
        roleRepository.lockByName(RoleName.ADMIN.name())
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Thiếu vai trò ADMIN"));
    }

    // =========================================================
    // HÀM HỖ TRỢ
    // =========================================================

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    private UserResponse toResponse(User user) {
        return userMapper.toResponse(user);
    }
}
