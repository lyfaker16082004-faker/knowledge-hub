package com.knowledge.app.service.auth;
import com.knowledge.app.model.dto.request.auth.RegisterRequest;
import com.knowledge.app.model.dto.response.user.UserResponse;
public interface AuthService {
 UserResponse register(RegisterRequest request);
 com.knowledge.app.model.dto.response.auth.LoginResponse login(String identity,String password);
 void logout(Long id);
 void recordLogin(Long id);
}
