package com.knowledge.app.security.jwt;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
@Configuration
public class JwtConfiguration {
 @Bean SecretKey jwtKey(@Value("${app.jwt.secret:}") String configured,Environment env)throws Exception{
  String value=configured;
  if(value.isBlank()){
   if(Arrays.asList(env.getActiveProfiles()).contains("prod"))throw new IllegalStateException("Set APP_JWT_SECRET to a Base64 key of at least 32 bytes in production");
   Path path=Path.of("config/jwt-secret.key");Files.createDirectories(path.getParent());
   if(!Files.exists(path)){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);try{Files.writeString(path,Base64.getEncoder().encodeToString(bytes),StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}}
   value=Files.readString(path).trim();
  }
  byte[] bytes=Base64.getDecoder().decode(value);if(bytes.length<32)throw new IllegalStateException("JWT signing key must contain at least 32 bytes");return new SecretKeySpec(bytes,"HmacSHA256");
 }
 @Bean JwtEncoder jwtEncoder(SecretKey jwtKey){return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));}
 @Bean JwtDecoder jwtDecoder(SecretKey jwtKey){var decoder=NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("knowledge-hub"));return decoder;}
}
