package com.knowledge.app.model.dto.response.auth;
import com.knowledge.app.model.dto.response.user.UserResponse;
/**
 * Kết quả đăng nhập: token JWT, thời gian hết hạn và thông tin tài khoản.
 */
public record LoginResponse(String accessToken,String tokenType,long expiresIn,UserResponse user) {}
