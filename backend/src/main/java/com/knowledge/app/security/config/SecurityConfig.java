package com.knowledge.app.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

import com.knowledge.app.security.filter.JwtAuthenticationFilter;
import com.knowledge.app.security.handler.RestAccessDeniedHandler;
import com.knowledge.app.security.handler.RestAuthenticationEntryPoint;
import com.knowledge.app.security.jwt.JwtService;
import com.knowledge.app.security.user.AccountDetailsService;

/**
 * Cấu hình bảo mật của ứng dụng (Spring Security).
 *
 * - Dùng JWT, không dùng session (STATELESS)
 * - Khai báo API nào ai cũng gọi được, API nào phải đăng nhập, API nào chỉ dành cho ADMIN
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Mã hoá mật khẩu bằng BCrypt (độ mạnh 12). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** Bộ kiểm tra username/mật khẩu khi đăng nhập. */
    @Bean
    AuthenticationManager authenticationManager(AccountDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountDetailsService userDetailsService,
            JwtService jwtService) throws Exception {

        // ---- Tắt các tính năng không cần vì đã dùng JWT ----
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.logout(AbstractHttpConfigurer::disable);
        http.requestCache(cache -> cache.disable());

        // ---- Không lưu trạng thái đăng nhập trên server ----
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()));

        // ---- Phân quyền cho từng API ----
        http.authorizeHttpRequests(auth -> auth
                // 1. Ai cũng truy cập được
                .requestMatchers("/api/health", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/institutions",
                        "/api/institutions/*/faculties",
                        "/api/categories",
                        "/api/subjects",
                        "/api/documents",
                        "/api/documents/*",
                        "/api/documents/*/related",
                        "/api/documents/*/files/*/sample",
                        "/api/avatars/*",
                        "/api/post-images/*",
                        "/api/community/users/*",
                        "/api/posts",
                        "/api/posts/sidebar",
                        "/api/posts/authors/*/stats",
                        "/api/posts/*",
                        "/api/posts/*/comments").permitAll()

                // 2. Chỉ ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 3. Phải đăng nhập
                .requestMatchers(
                        "/api/auth/logout",
                        "/api/users/me",
                        "/api/users/me/**",
                        "/api/documents/*/files/*/preview",
                        "/api/documents/*/files/*/preview-text",
                        "/api/community/**").authenticated()
                .requestMatchers(HttpMethod.POST,
                        "/api/documents",
                        "/api/documents/*/files/*/download").authenticated()
                .requestMatchers(
                        "/api/posts",
                        "/api/posts/*",
                        "/api/posts/*/like",
                        "/api/posts/*/bookmark",
                        "/api/posts/*/comments",
                        "/api/comments/*",
                        "/api/comments/*/vote").authenticated()

                // 4. Còn lại: chặn hết
                .anyRequest().denyAll());

        // ---- Trả lỗi dạng JSON khi chưa đăng nhập (401) hoặc không có quyền (403) ----
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .accessDeniedHandler(new RestAccessDeniedHandler()));

        // ---- Thêm filter đọc JWT trong header Authorization ----
        http.addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
