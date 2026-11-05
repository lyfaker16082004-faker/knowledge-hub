package com.knowledge.app.security.filter;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import com.knowledge.app.security.jwt.JwtService;
import com.knowledge.app.security.user.AccountDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtService tokens;private final AccountDetailsService details;
 public JwtAuthenticationFilter(JwtService tokens,AccountDetailsService details){this.tokens=tokens;this.details=details;}
 @Override protected boolean shouldNotFilter(HttpServletRequest request){String path=request.getRequestURI().substring(request.getContextPath().length());return "POST".equals(request.getMethod())&&("/api/auth/login".equals(path)||"/api/auth/register".equals(path));}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String header=request.getHeader("Authorization");
  if(header!=null){
   try{
    if(!header.startsWith("Bearer ")||header.length()<8)throw new JwtException("Invalid authorization header");
    var jwt=tokens.verify(header.substring(7));String username=jwt.getClaimAsString("username");if(username==null)throw new JwtException("Missing identity");
    var principal=details.loadUserByUsername(username);Object version=jwt.getClaim("av");
    if(!principal.isEnabled()||!principal.getId().toString().equals(jwt.getSubject())||!(version instanceof Number number)||number.longValue()!=principal.getAuthVersion())throw new JwtException("Token revoked");
    principal.eraseCredentials();SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));
   }catch(JwtException|UsernameNotFoundException|IllegalArgumentException e){response.setStatus(401);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");response.getWriter().write("{\"success\":false,\"message\":\"Phiên đăng nhập hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.\",\"data\":null}");return;}
  }
  try{chain.doFilter(request,response);}finally{SecurityContextHolder.clearContext();}
 }
}
