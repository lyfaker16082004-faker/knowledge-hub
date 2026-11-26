package com.knowledge.app.controller.post;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.model.dto.response.post.UploadedImage;
import com.knowledge.app.service.post.PostImageStorage;

/**
 * API ảnh trong bài viết:
 * - POST /api/posts/images        : tải ảnh lên (phải đăng nhập)
 * - GET  /api/post-images/{key}   : xem ảnh (ai cũng xem được)
 */
@RestController
public class PostImageController {

    private final PostImageStorage postImageStorage;

    public PostImageController(PostImageStorage postImageStorage) {
        this.postImageStorage = postImageStorage;
    }

    @PostMapping(value = "/api/posts/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadedImage> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(new UploadedImage(postImageStorage.save(file)));
    }

    @GetMapping("/api/post-images/{key}")
    public ResponseEntity<byte[]> image(@PathVariable String key) {
        PostImageStorage.StoredImage image = postImageStorage.read(key);

        // Tên file là UUID và ảnh không bao giờ bị sửa -> cho trình duyệt cache lâu 
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.bytes());
    }
}
