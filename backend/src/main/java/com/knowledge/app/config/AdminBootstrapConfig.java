package com.knowledge.app.config;
import com.knowledge.app.service.user.UserService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
@Configuration @Profile("dev")
@ConditionalOnProperty(name="app.bootstrap.enabled",havingValue="true")
public class AdminBootstrapConfig {
 @Bean ApplicationRunner createInitialAdmin(UserService users,Environment env) {
  return args->{
   String username=env.getRequiredProperty("APP_ADMIN_USERNAME"),email=env.getRequiredProperty("APP_ADMIN_EMAIL"),password=env.getRequiredProperty("APP_ADMIN_PASSWORD");
   if(!username.matches("[a-zA-Z0-9_.-]{3,50}") || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new IllegalArgumentException("Invalid bootstrap username/email");
   users.bootstrapAdmin(username,email,password);
  };
 }
}
