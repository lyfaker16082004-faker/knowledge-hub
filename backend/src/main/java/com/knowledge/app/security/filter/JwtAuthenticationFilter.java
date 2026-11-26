package com.knowledge.app.security.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import com.knowledge.app.common.constant.SecurityConstants;
import com.knowledge.app.security.handler.JsonErrorWriter;
import com.knowledge.app.security.jwt.JwtService;
import com.knowledge.app.security.user.AccountDetailsService;
import com.knowledge.app.security.user.AccountPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter chạy trước mỗi request: đọc JWT trong header "Authorization: Bearer <token>".
 *
 * - Không có header -> cho đi tiếp như khách (chưa đăng nhập)
 * - Có header và token hợp lệ -> đánh dấu người dùng đã đăng nhập
 * - Có header nhưng token sai / hết hạn / đã bị thu hồi -> trả lỗi 401
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccountDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, AccountDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /** Bỏ qua filter với API đăng nhập và đăng ký. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean isPost = "POST".equals(request.getMethod());
        boolean isLoginOrRegister = "/api/auth/login".equals(path) || "/api/auth/register".equals(path);
        return isPost && isLoginOrRegister;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null) {
            try {
                authenticate(header);
            } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
                writeUnauthorized(response);
                return; // Dừng lại, không xử lý request nữa
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Xoá thông tin đăng nhập sau khi xử lý xong request
            SecurityContextHolder.clearContext();
        }
    }

    /** Kiểm tra token và lưu người dùng vào SecurityContext. */
    private void authenticate(String header) {
        // Bước 1: Header phải có dạng "Bearer <token>"
        if (!header.startsWith(SecurityConstants.BEARER_PREFIX) || header.length() < 8) {
            throw new JwtException("Invalid authorization header");
        }
        String token = header.substring(SecurityConstants.BEARER_PREFIX.length());

        // Bước 2: Giải mã token
        Jwt jwt = jwtService.verify(token);
        String username = jwt.getClaimAsString("username");
        if (username == null) {
            throw new JwtException("Missing identity");
        }

        // Bước 3: Tìm người dùng trong database
        AccountPrincipal principal = userDetailsService.loadUserByUsername(username);

        // Bước 4: Kiểm tra token còn hiệu lực không
        Object authVersion = jwt.getClaim("av");
        boolean sameUser = principal.getId().toString().equals(jwt.getSubject());
        boolean sameAuthVersion = authVersion instanceof Number number
                && number.longValue() == principal.getAuthVersion();
        if (!principal.isEnabled() || !sameUser || !sameAuthVersion) {
            // authVersion khác nghĩa là người dùng đã đăng xuất / đổi mật khẩu sau khi token được tạo
            throw new JwtException("Token revoked");
        }

        // Bước 5: Đánh dấu đã đăng nhập
        principal.eraseCredentials(); // Không giữ mật khẩu trong bộ nhớ
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        JsonErrorWriter.write(response, 401, "Phiên đăng nhập hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.");
    }
}
