package com.knowledge.app.service.impl.auth;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.constant.SecurityConstants;
import com.knowledge.app.common.util.TimeUtils;
import com.knowledge.app.model.dto.request.auth.RegisterRequest;
import com.knowledge.app.model.dto.request.user.AdminCreateUserRequest;
import com.knowledge.app.model.dto.request.user.UpdateProfileRequest;
import com.knowledge.app.model.dto.response.auth.LoginResponse;
import com.knowledge.app.model.dto.response.user.UserResponse;
import com.knowledge.app.model.enums.RoleName;
import com.knowledge.app.repository.UserRepository;
import com.knowledge.app.security.jwt.JwtService;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.auth.AuthService;
import com.knowledge.app.service.impl.user.UserServiceImpl;
import com.knowledge.app.service.user.ProfileService;

/**
 * Xử lý đăng ký, đăng nhập, đăng xuất.
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserServiceImpl userService;
    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserServiceImpl userService,
            UserRepository userRepository,
            ProfileService profileService,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Đăng ký tài khoản mới (vai trò USER).
     */
    @Override
    public UserResponse register(RegisterRequest request) {
        // Bước 1: Kiểm tra trường/khoa có hợp lệ không
        profileService.validateOrganization(request.institutionId(), request.facultyId());

        // Bước 2: Nếu không nhập username thì tự sinh. VD: "user_3f9a1b2c4d5e"
        String username = request.username();
        if (username == null || username.isBlank()) {
            String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            username = "user_" + randomPart;
        }

        // Bước 3: Tạo tài khoản
        AdminCreateUserRequest createRequest =
                new AdminCreateUserRequest(username, request.email(), request.password(), Set.of(RoleName.USER.name()));
        UserResponse created = userService.createAccount(createRequest);

        // Bước 4: Tạo hồ sơ (tên hiển thị mặc định = username)
        UpdateProfileRequest profileRequest =
                new UpdateProfileRequest(username, null, request.institutionId(), request.facultyId());
        profileService.update(created.id(), profileRequest);

        return userService.get(created.id());
    }

    /**
     * Đăng nhập bằng email hoặc username.
     *
     * @param identity email hoặc username
     */
    @Override
    public LoginResponse login(String identity, String password) {
        // Bước 1: Kiểm tra sơ bộ dữ liệu (chặn dữ liệu rác trước khi truy vấn database)
        if (!isValidLoginInput(identity, password)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không hợp lệ");
        }

        try {
            // Bước 2: Nhờ Spring Security kiểm tra username/mật khẩu
            UsernamePasswordAuthenticationToken token =
                    UsernamePasswordAuthenticationToken.unauthenticated(identity.trim(), password);
            Authentication authenticated = authenticationManager.authenticate(token);
            AccountPrincipal principal = (AccountPrincipal) authenticated.getPrincipal();

            // Bước 3: Ghi lại thời điểm đăng nhập
            recordLogin(principal.getId());

            // Bước 4: Tạo JWT và trả về cho client
            String accessToken = jwtService.issue(principal);
            UserResponse user = userService.get(principal.getId());
            return new LoginResponse(accessToken, "Bearer", SecurityConstants.JWT_LIFETIME_SECONDS, user);

        } catch (AuthenticationException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Email/tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản bị khóa / không hoạt động");
        }
    }

    /**
     * Kiểm tra dữ liệu đăng nhập:
     * - identity: không rỗng, tối đa 254 ký tự; nếu là username (không có @) thì tối đa 50 ký tự
     * - password: không rỗng, tối đa 72 byte (giới hạn của thuật toán BCrypt)
     */
    private boolean isValidLoginInput(String identity, String password) {
        if (identity == null || identity.isBlank() || identity.length() > 254) {
            return false;
        }
        boolean isUsername = !identity.contains("@");
        if (isUsername && identity.trim().length() > 50) {
            return false;
        }
        if (password == null || password.isEmpty()) {
            return false;
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        return true;
    }

    /**
     * Đăng xuất: tăng authVersion lên 1.
     * Mọi JWT cũ chứa authVersion cũ sẽ không còn hợp lệ nữa.
     */
    @Override
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(user -> user.setAuthVersion(user.getAuthVersion() + 1));
    }

    @Override
    public void recordLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> user.setLastLoginAt(TimeUtils.now()));
    }
}
