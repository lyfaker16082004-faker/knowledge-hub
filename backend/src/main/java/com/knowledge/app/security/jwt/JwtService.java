package com.knowledge.app.security.jwt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.knowledge.app.common.constant.SecurityConstants;
import com.knowledge.app.security.user.AccountPrincipal;

/**
 * Tạo và kiểm tra JWT (JSON Web Token).
 */
@Service
public class JwtService {

    /** Thời gian sống của token (giây). Giữ lại tên này để các class khác dùng. */
    public static final long LIFETIME_SECONDS = SecurityConstants.JWT_LIFETIME_SECONDS;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    /**
     * Tạo token cho người dùng vừa đăng nhập.
     */
    public String issue(AccountPrincipal principal) {
        Instant now = Instant.now();

        // Danh sách vai trò, bỏ tiền tố "ROLE_". VD: "ROLE_ADMIN" -> "ADMIN"
        List<String> roles = new ArrayList<>();
        for (GrantedAuthority authority : principal.getAuthorities()) {
            roles.add(authority.getAuthority().replace(SecurityConstants.ROLE_PREFIX, ""));
        }

        // Nội dung (claims) của token
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SecurityConstants.JWT_ISSUER)
                .audience(List.of(SecurityConstants.JWT_AUDIENCE))
                .subject(principal.getId().toString())   // "sub" = id người dùng
                .issuedAt(now)
                .expiresAt(now.plusSeconds(LIFETIME_SECONDS))
                .claim("username", principal.getUsername())
                .claim("av", principal.getAuthVersion()) // "av" = authVersion, dùng để vô hiệu token khi đăng xuất
                .claim("roles", roles)
                .build();

        // Ký token bằng thuật toán HS256
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Giải mã và kiểm tra token. Ném JwtException nếu token sai.
     */
    public Jwt verify(String token) {
        Jwt jwt = decoder.decode(token);
        if (!jwt.getAudience().contains(SecurityConstants.JWT_AUDIENCE)) {
            throw new JwtException("Invalid token audience");
        }
        return jwt;
    }
}
