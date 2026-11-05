package com.knowledge.app.security.jwt;
import java.time.*;
import java.util.List;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import com.knowledge.app.security.user.AccountPrincipal;
@Service
public class JwtService {
 public static final long LIFETIME_SECONDS=86400;
 private final JwtEncoder encoder;private final JwtDecoder decoder;
 public JwtService(JwtEncoder encoder,JwtDecoder decoder){this.encoder=encoder;this.decoder=decoder;}
 public String issue(AccountPrincipal principal){var now=Instant.now();var claims=JwtClaimsSet.builder().issuer("knowledge-hub").audience(List.of("knowledge-hub-web")).subject(principal.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(LIFETIME_SECONDS)).claim("username",principal.getUsername()).claim("av",principal.getAuthVersion()).claim("roles",principal.getAuthorities().stream().map(a->a.getAuthority().replace("ROLE_","")).toList()).build();return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),claims)).getTokenValue();}
 public Jwt verify(String token){var jwt=decoder.decode(token);if(!jwt.getAudience().contains("knowledge-hub-web"))throw new JwtException("Invalid token audience");return jwt;}
}
