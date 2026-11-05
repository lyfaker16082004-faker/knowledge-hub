package com.knowledge.app.security.user;
import java.util.*;
import com.knowledge.app.model.entity.User;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
public final class AccountPrincipal implements UserDetails, CredentialsContainer {
 private static final long serialVersionUID=1L;
 private final Long id, authVersion;
 private final String username, status;
 private String password;
 private final List<SimpleGrantedAuthority> authorities;
 public AccountPrincipal(User user, List<String> roles) {
  id=user.getId(); authVersion=user.getAuthVersion(); username=user.getUsername(); status=user.getStatus(); password=user.getPasswordHash();
  authorities=roles.stream().map(r->new SimpleGrantedAuthority("ROLE_"+r)).toList();
 }
 public Long getId() { return id; }
 public Long getAuthVersion() { return authVersion; }
 @Override public String getUsername() { return username; }
 @Override public String getPassword() { return password; }
 @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
 @Override public boolean isEnabled() { return "ACTIVE".equals(status); }
 @Override public boolean isAccountNonLocked() { return !"LOCKED".equals(status); }
 @Override public boolean isAccountNonExpired() { return !"DELETED".equals(status); }
 @Override public boolean isCredentialsNonExpired() { return true; }
 @Override public void eraseCredentials() { password=null; }
}
