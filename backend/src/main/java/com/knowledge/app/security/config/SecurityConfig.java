package com.knowledge.app.security.config;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import com.knowledge.app.security.user.AccountDetailsService;
import com.knowledge.app.security.jwt.JwtService;
import com.knowledge.app.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean AuthenticationManager authenticationManager(AccountDetailsService details,PasswordEncoder encoder){var provider=new DaoAuthenticationProvider(details);provider.setPasswordEncoder(encoder);return new ProviderManager(provider);}
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,AccountDetailsService details,JwtService tokens)throws Exception{
  return http.csrf(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).securityContext(c->c.securityContextRepository(new NullSecurityContextRepository())).requestCache(c->c.disable())
   .authorizeHttpRequests(r->r.requestMatchers("/api/health","/error").permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/auth/register","/api/auth/login").permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/institutions","/api/institutions/*/faculties","/api/categories","/api/subjects","/api/documents","/api/documents/*","/api/documents/*/files/*/sample","/api/avatars/*","/api/community/users","/api/community/users/*","/api/community/users/*/posts").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/auth/logout","/api/users/me","/api/users/me/**","/api/documents/*/files/*/preview","/api/documents/*/files/*/preview-text","/api/community/**").authenticated()
    .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/documents","/api/documents/*/files/*/download").authenticated().anyRequest().denyAll())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->json(res,401,"Vui lòng đăng nhập bằng JWT hợp lệ")) .accessDeniedHandler((req,res,ex)->json(res,403,"Bạn không có quyền thực hiện thao tác này")))
   .addFilterBefore(new JwtAuthenticationFilter(tokens,details),UsernamePasswordAuthenticationFilter.class).build();
 }
 private static void json(HttpServletResponse response,int status,String message)throws IOException{response.setStatus(status);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");response.getWriter().write("{\"success\":false,\"message\":\""+message+"\",\"data\":null}");}
}

