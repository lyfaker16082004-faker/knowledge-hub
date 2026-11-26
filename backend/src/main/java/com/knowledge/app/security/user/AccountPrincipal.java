package com.knowledge.app.security.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.knowledge.app.common.constant.SecurityConstants;
import com.knowledge.app.model.entity.User;
import com.knowledge.app.model.enums.RoleName;
import com.knowledge.app.model.enums.UserStatus;

/**
 * Thông tin người dùng đang đăng nhập, dùng bởi Spring Security.
 */
public final class AccountPrincipal implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long authVersion;
    private final String username;
    private final String status;
    private String password; // Không final vì sẽ bị xoá (eraseCredentials) sau khi xác thực
    private final List<SimpleGrantedAuthority> authorities;

    public AccountPrincipal(User user, List<String> roles) {
        this.id = user.getId();
        this.authVersion = user.getAuthVersion();
        this.username = user.getUsername();
        this.status = user.getStatus();
        this.password = user.getPasswordHash();

        // Spring Security quy ước vai trò có tiền tố "ROLE_". VD: "ADMIN" -> "ROLE_ADMIN"
        List<SimpleGrantedAuthority> list = new ArrayList<>();
        for (String role : roles) {
            list.add(new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role));
        }
        this.authorities = List.copyOf(list);
    }

    public Long getId() {
        return id;
    }

    public Long getAuthVersion() {
        return authVersion;
    }

    /** Người dùng có vai trò này không. VD: hasRole(RoleName.ADMIN) */
    public boolean hasRole(RoleName role) {
        String expected = SecurityConstants.ROLE_PREFIX + role.name();
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals(expected)) {
                return true;
            }
        }
        return false;
    }

    /** ADMIN hoặc MODERATOR đều có quyền kiểm duyệt bài viết, bình luận. */
    public boolean isModerator() {
        return hasRole(RoleName.ADMIN) || hasRole(RoleName.MODERATOR);
    }

    /**
     * Lấy id của người dùng đang đăng nhập; chưa đăng nhập (principal = null) thì trả về null.
     * Dùng trong các API cho phép cả khách lẫn người đã đăng nhập.
     */
    public static Long idOf(AccountPrincipal principal) {
        return principal != null ? principal.getId() : null;
    }

    /** Giống isModerator() nhưng an toàn khi principal = null (khách). */
    public static boolean isModerator(AccountPrincipal principal) {
        return principal != null && principal.isModerator();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /** Chỉ tài khoản ACTIVE mới được đăng nhập. */
    @Override
    public boolean isEnabled() {
        return UserStatus.ACTIVE.is(status);
    }

    @Override
    public boolean isAccountNonLocked() {
        return !UserStatus.LOCKED.is(status);
    }

    @Override
    public boolean isAccountNonExpired() {
        return !UserStatus.DELETED.is(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Xoá mật khẩu khỏi bộ nhớ sau khi xác thực xong. */
    @Override
    public void eraseCredentials() {
        password = null;
    }
}
