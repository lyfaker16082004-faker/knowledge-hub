package com.knowledge.app.security.jwt;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.knowledge.app.common.constant.SecurityConstants;
import com.knowledge.app.config.StoragePaths;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Cấu hình khoá bí mật để ký và kiểm tra JWT.
 */
@Configuration
public class JwtConfiguration {

    private static final int MIN_KEY_BYTES = 32; // HS256 cần khoá tối thiểu 32 byte

    /**
     * Lấy khoá bí mật:
     * - Nếu có cấu hình app.jwt.secret (biến môi trường APP_JWT_SECRET) thì dùng nó
     * - Nếu không (khi chạy dev): tự sinh khoá ngẫu nhiên và lưu vào file config/jwt-secret.key
     * - Môi trường production bắt buộc phải cấu hình khoá
     */
    @Bean
    SecretKey jwtKey(@Value("${app.jwt.secret:}") String configuredSecret, Environment environment)
            throws Exception {
        String secret = configuredSecret;

        if (secret.isBlank()) {
            boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
            if (isProduction) {
                throw new IllegalStateException(
                        "Set APP_JWT_SECRET to a Base64 key of at least 32 bytes in production");
            }
            secret = readOrCreateLocalSecret();
        }

        byte[] keyBytes = Base64.getDecoder().decode(secret);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT signing key must contain at least 32 bytes");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /** Đọc khoá từ file; nếu file chưa tồn tại thì sinh khoá ngẫu nhiên và ghi vào file. */
    private String readOrCreateLocalSecret() throws Exception {
        Path path = StoragePaths.backendHome().resolve("config/jwt-secret.key");
        Files.createDirectories(path.getParent());

        if (!Files.exists(path)) {
            byte[] randomBytes = new byte[MIN_KEY_BYTES];
            new SecureRandom().nextBytes(randomBytes);
            String encoded = Base64.getEncoder().encodeToString(randomBytes);
            try {
                Files.writeString(path, encoded, StandardOpenOption.CREATE_NEW);
            } catch (FileAlreadyExistsException ignored) {
                // Có tiến trình khác vừa tạo file -> dùng file đó
            }
        }

        return Files.readString(path).trim();
    }

    /** Bộ tạo JWT. */
    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    /** Bộ giải mã JWT: chỉ chấp nhận HS256 và issuer "knowledge-hub", tự kiểm tra hạn dùng. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(SecurityConstants.JWT_ISSUER));
        return decoder;
    }
}
