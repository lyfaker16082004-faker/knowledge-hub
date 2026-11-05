package com.knowledge.app.controller.user;
import com.knowledge.app.service.user.AvatarStorage;
import java.time.Duration;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
public class AvatarController {
 private final AvatarStorage avatars;
 public AvatarController(AvatarStorage avatars){this.avatars=avatars;}
 @GetMapping("/api/avatars/{key}") public ResponseEntity<byte[]> avatar(@PathVariable String key){return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic()).body(avatars.read(key));}
}
