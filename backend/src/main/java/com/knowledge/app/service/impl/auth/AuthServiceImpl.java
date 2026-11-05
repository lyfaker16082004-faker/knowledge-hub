package com.knowledge.app.service.impl.auth;
import java.time.*;
import java.util.Set;
import com.knowledge.app.model.dto.request.auth.RegisterRequest;
import com.knowledge.app.model.dto.request.user.AdminCreateUserRequest;
import com.knowledge.app.model.dto.response.user.UserResponse;
import com.knowledge.app.repository.UserRepository;
import com.knowledge.app.service.auth.AuthService;
import com.knowledge.app.service.impl.user.UserServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class AuthServiceImpl implements AuthService {
 private final UserServiceImpl users; private final org.springframework.security.authentication.AuthenticationManager manager;private final com.knowledge.app.security.jwt.JwtService tokens; private final UserRepository repository; private final com.knowledge.app.service.user.ProfileService profiles;
 public AuthServiceImpl(UserServiceImpl users,UserRepository repository,com.knowledge.app.service.user.ProfileService profiles,org.springframework.security.authentication.AuthenticationManager manager,com.knowledge.app.security.jwt.JwtService tokens) { this.manager=manager;this.tokens=tokens; this.profiles=profiles; this.users=users; this.repository=repository; }
 @Override public UserResponse register(RegisterRequest request) { profiles.validateOrganization(request.institutionId(),request.facultyId()); String username=request.username()==null||request.username().isBlank()?"user_"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,12):request.username(); var created=users.createAccount(new AdminCreateUserRequest(username,request.email(),request.password(),Set.of("USER"))); profiles.update(created.id(),new com.knowledge.app.model.dto.request.user.UpdateProfileRequest(username,null,request.institutionId(),request.facultyId())); return users.get(created.id()); }
 @Override public com.knowledge.app.model.dto.response.auth.LoginResponse login(String identity,String password){
  if(identity==null||identity.isBlank()||identity.length()>254||(!identity.contains("@")&&identity.trim().length()>50)||password==null||password.isEmpty()||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new com.knowledge.app.common.exception.ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,"Thông tin đăng nhập không hợp lệ");
  try{var authenticated=manager.authenticate(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.unauthenticated(identity.trim(),password));var principal=(com.knowledge.app.security.user.AccountPrincipal)authenticated.getPrincipal();recordLogin(principal.getId());return new com.knowledge.app.model.dto.response.auth.LoginResponse(tokens.issue(principal),"Bearer",com.knowledge.app.security.jwt.JwtService.LIFETIME_SECONDS,users.get(principal.getId()));}
  catch(org.springframework.security.core.AuthenticationException e){throw new com.knowledge.app.common.exception.ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,"Email/tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản bị khóa / không hoạt động");}
 }
 @Override public void logout(Long id){repository.findById(id).ifPresent(user->user.setAuthVersion(user.getAuthVersion()+1));}
 @Override public void recordLogin(Long id) { repository.findById(id).ifPresent(user->user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC))); }
}
