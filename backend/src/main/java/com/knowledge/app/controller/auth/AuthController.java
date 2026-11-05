package com.knowledge.app.controller.auth;
import jakarta.validation.Valid;
import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.model.dto.request.auth.*;
import com.knowledge.app.model.dto.response.auth.LoginResponse;
import com.knowledge.app.model.dto.response.user.UserResponse;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.auth.AuthService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final AuthService auth;
 public AuthController(AuthService auth){this.auth=auth;}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request){return ApiResponse.ok(auth.register(request));}
 @PostMapping(value="/login",consumes=MediaType.APPLICATION_JSON_VALUE) public ApiResponse<LoginResponse> loginJson(@Valid @RequestBody LoginRequest request){return ApiResponse.ok(auth.login(request.username(),request.password()));}
 @PostMapping("/login") public ApiResponse<LoginResponse> loginForm(@RequestParam(required=false) String username,@RequestParam(required=false) String password){return ApiResponse.ok(auth.login(username,password));}
 @PostMapping("/logout") public ApiResponse<Void> logout(@AuthenticationPrincipal AccountPrincipal principal){auth.logout(principal.getId());return new ApiResponse<>(true,"Đã đăng xuất và thu hồi token cũ",null);}
}
