package com.knowledge.app.model.dto.response.auth;
import com.knowledge.app.model.dto.response.user.UserResponse;
public record LoginResponse(String accessToken,String tokenType,long expiresIn,UserResponse user) {}
