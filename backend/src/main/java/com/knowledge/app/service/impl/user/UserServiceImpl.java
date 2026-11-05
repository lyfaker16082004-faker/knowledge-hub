package com.knowledge.app.service.impl.user;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.dto.request.user.*;
import com.knowledge.app.model.dto.response.user.UserResponse;
import com.knowledge.app.model.entity.*;
import com.knowledge.app.repository.*;
import com.knowledge.app.service.user.UserService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class UserServiceImpl implements UserService {
 private final UserRepository users; private final RoleRepository roles; private final UserRoleRepository grants; private final PasswordEncoder encoder; private final com.knowledge.app.service.user.ProfileService profiles;
 public UserServiceImpl(UserRepository users,RoleRepository roles,UserRoleRepository grants,PasswordEncoder encoder,com.knowledge.app.service.user.ProfileService profiles) { this.profiles=profiles; this.users=users; this.roles=roles; this.grants=grants; this.encoder=encoder; }
 @Override @Transactional(readOnly=true) public UserResponse get(Long id) { return view(required(id)); }
 @Override @Transactional(readOnly=true) public PageResponse<UserResponse> list(String query,int page,int size) {
  var result=users.search(query.trim(),PageRequest.of(page,size,Sort.by("id").descending()));
  return new PageResponse<>(result.getContent().stream().map(this::view).toList(),result.getTotalElements(),result.getTotalPages(),page,size);
 }
 @Override public UserResponse create(AdminCreateUserRequest request) { lockAdmin(); return createAccount(request); }
 public UserResponse createAccount(AdminCreateUserRequest request) {
  String username=request.username().trim().toLowerCase(Locale.ROOT), email=request.email().trim().toLowerCase(Locale.ROOT);
  if(users.existsByUsernameIgnoreCase(username)||users.existsByEmailIgnoreCase(email)) throw new ApiException(HttpStatus.CONFLICT,"Tên đăng nhập hoặc email đã tồn tại");
  validatePassword(request.password()); var assigned=resolve(request.roles());
  User user=new User(); user.setUsername(username); user.setEmail(email); user.setPasswordHash(encoder.encode(request.password())); user.setStatus("ACTIVE"); user.setEmailVerified(false);
  users.saveAndFlush(user); assign(user,assigned); return view(user);
 }
 @Override public UserResponse update(Long id,AdminUpdateUserRequest request) {
  lockAdmin(); var user=required(id); var assigned=resolve(request.roles());
  boolean wasAdmin=grants.findWithRoles(id).stream().anyMatch(g->"ADMIN".equals(g.getRole().getName()));
  if(wasAdmin && "ACTIVE".equals(user.getStatus()) && (!"ACTIVE".equals(request.status()) || !request.roles().contains("ADMIN")) && grants.countActiveAdmins()<=1) throw new ApiException(HttpStatus.CONFLICT,"Phải giữ ít nhất một admin đang hoạt động");
  String email=request.email().trim().toLowerCase(Locale.ROOT);
  if(users.existsByEmailIgnoreCaseAndIdNot(email,id)) throw new ApiException(HttpStatus.CONFLICT,"Email đã tồn tại");
  if(!email.equals(user.getEmail())) user.setEmailVerified(false);
  user.setEmail(email); user.setStatus(request.status()); user.setAuthVersion(user.getAuthVersion()+1);
  grants.removeForUser(id); grants.flush(); assign(user,assigned); users.flush(); return view(user);
 }
 @Override public UserResponse softDelete(Long id) {
  lockAdmin();var user=required(id);
  if("DELETED".equals(user.getStatus()))return view(user);
  boolean admin=grants.findWithRoles(id).stream().anyMatch(g->"ADMIN".equals(g.getRole().getName()));
  if(admin&&"ACTIVE".equals(user.getStatus())&&grants.countActiveAdmins()<=1)throw new ApiException(HttpStatus.CONFLICT,"Phải giữ ít nhất một admin đang hoạt động");
  user.setStatus("DELETED");user.setAuthVersion(user.getAuthVersion()+1);users.flush();return view(user);
 }
 @Override public void changePassword(Long id,ChangePasswordRequest request) {
  var user=required(id);
  if(request.currentPassword().getBytes(StandardCharsets.UTF_8).length>72 || !encoder.matches(request.currentPassword(),user.getPasswordHash())) throw new ApiException(HttpStatus.BAD_REQUEST,"Mật khẩu hiện tại không đúng");
  replacePassword(user,request.newPassword());
 }
 @Override public void resetPassword(Long id,ResetPasswordRequest request) { replacePassword(required(id),request.password()); }
 @Override public void bootstrapAdmin(String username,String email,String password) {
  lockAdmin(); if(grants.countActiveAdmins()>0) return;
  createAccount(new AdminCreateUserRequest(username,email,password,Set.of("ADMIN","USER")));
 }
 private void replacePassword(User user,String password) { validatePassword(password); user.setPasswordHash(encoder.encode(password)); user.setAuthVersion(user.getAuthVersion()+1); }
 private void validatePassword(String password) {
  if(password==null || password.isBlank() || password.length()<8 || password.getBytes(StandardCharsets.UTF_8).length>72) throw new ApiException(HttpStatus.BAD_REQUEST,"Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte UTF-8");
 }
 private List<Role> resolve(Set<String> values) {
  if(values==null || values.isEmpty() || !Set.of("ADMIN","MODERATOR","USER").containsAll(values)) throw new ApiException(HttpStatus.BAD_REQUEST,"Vai trò không hợp lệ");
  return values.stream().sorted().map(name->roles.findByName(name).orElseThrow(()->new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa có dữ liệu vai trò. Hãy chạy migration."))).toList();
 }
 private void assign(User user,List<Role> values) { for(Role role:values) { var grant=new UserRole(); grant.setUser(user); grant.setRole(role); grants.save(grant); } grants.flush(); }
 private void lockAdmin() { roles.lockByName("ADMIN").orElseThrow(()->new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Thiếu vai trò ADMIN")); }
 private User required(Long id) { return users.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản")); }
 private UserResponse view(User user) { var profile=profiles.details(user.getId()); return new UserResponse(user.getId(),user.getUsername(),user.getEmail(),user.getStatus(),Boolean.TRUE.equals(user.getEmailVerified()),grants.findWithRoles(user.getId()).stream().map(g->g.getRole().getName()).sorted().toList(),user.getCreatedAt(),user.getLastLoginAt(),profiles.organization(user.getId()),profile.displayName(),profile.bio(),profile.avatarUrl()); }
}
