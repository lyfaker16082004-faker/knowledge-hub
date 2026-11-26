package com.knowledge.app.common.constant;

/**
 * Các hằng số liên quan đến bảo mật và JWT.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Chỉ chứa hằng số, không tạo đối tượng
    }

    /** Spring Security quy ước vai trò có tiền tố "ROLE_". VD: "ADMIN" -> "ROLE_ADMIN". */
    public static final String ROLE_PREFIX = "ROLE_";

    /** Header gửi lên có dạng: "Authorization: Bearer <token>". */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Ai phát hành token. */
    public static final String JWT_ISSUER = "knowledge-hub";

    /** Token dành cho ai. */
    public static final String JWT_AUDIENCE = "knowledge-hub-web";

    /** Thời gian sống của token: 86400 giây = 1 ngày. */
    public static final long JWT_LIFETIME_SECONDS = 86400;
}
