package com.knowledge.app.security.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowledge.app.model.entity.User;
import com.knowledge.app.model.entity.UserRole;
import com.knowledge.app.repository.UserRepository;
import com.knowledge.app.repository.UserRoleRepository;

/**
 * Spring Security gọi class này để tìm người dùng khi đăng nhập.
 */
@Service
public class AccountDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public AccountDetailsService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Tìm người dùng theo email (nếu có "@") hoặc theo username.
     */
    @Override
    @Transactional(readOnly = true)
    public AccountPrincipal loadUserByUsername(String identity) {
        String value = identity.trim();

        User user;
        if (identity.contains("@")) {
            user = userRepository.findByEmailIgnoreCase(value)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        } else {
            user = userRepository.findByUsernameIgnoreCase(value)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        }

        List<String> roleNames = new ArrayList<>();
        for (UserRole userRole : userRoleRepository.findWithRoles(user.getId())) {
            roleNames.add(userRole.getRole().getName());
        }

        return new AccountPrincipal(user, roleNames);
    }
}
