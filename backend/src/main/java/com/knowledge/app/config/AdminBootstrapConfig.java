package com.knowledge.app.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.knowledge.app.service.user.UserService;

/**
 * Tự tạo tài khoản admin đầu tiên khi khởi động app.
 *
 * Chỉ chạy khi: profile = dev VÀ app.bootstrap.enabled = true.
 * Thông tin admin lấy từ biến môi trường APP_ADMIN_USERNAME, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD.
 */
@Configuration
@Profile("dev")
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class AdminBootstrapConfig {

    private static final String USERNAME_PATTERN = "[a-zA-Z0-9_.-]{3,50}";
    private static final String EMAIL_PATTERN = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+";

    @Bean
    ApplicationRunner createInitialAdmin(UserService userService, Environment environment) {
        return args -> {
            String username = environment.getRequiredProperty("APP_ADMIN_USERNAME");
            String email = environment.getRequiredProperty("APP_ADMIN_EMAIL");
            String password = environment.getRequiredProperty("APP_ADMIN_PASSWORD");

            if (!username.matches(USERNAME_PATTERN) || !email.matches(EMAIL_PATTERN)) {
                throw new IllegalArgumentException("Invalid bootstrap username/email");
            }

            // Nếu đã có admin thì hàm này không làm gì cả
            userService.bootstrapAdmin(username, email, password);
        };
    }
}
