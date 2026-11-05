package com.knowledge.app.security.user;
import com.knowledge.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.*;
@Service
public class AccountDetailsService implements UserDetailsService {
 private final UserRepository users; private final UserRoleRepository grants;
 public AccountDetailsService(UserRepository users, UserRoleRepository grants) { this.users=users; this.grants=grants; }
 @Override @Transactional(readOnly=true)
 public AccountPrincipal loadUserByUsername(String name) {
  var user=(name.contains("@")?users.findByEmailIgnoreCase(name.trim()):users.findByUsernameIgnoreCase(name.trim())).orElseThrow(()->new UsernameNotFoundException("Không tìm thấy tài khoản"));
  return new AccountPrincipal(user,grants.findWithRoles(user.getId()).stream().map(g->g.getRole().getName()).toList());
 }
}
