package com.knowledge.app.service.post;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.knowledge.app.common.constant.AppConstants;
import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.util.ImageUtils;
import com.knowledge.app.config.StoragePaths;

/**
 * Lưu và đọc ảnh chèn trong bài viết (VD: ảnh chụp màn hình lỗi khi đặt câu hỏi).
 *
 * - Chỉ nhận PNG / JPEG, tối đa 5 MB.
 * - Ảnh được vẽ lại (loại bỏ dữ liệu ẩn) và thu nhỏ còn tối đa 1600 px.
 * - PNG giữ là PNG (ảnh chụp màn hình rõ nét), JPEG giữ là JPEG (ảnh chụp nhẹ hơn).
 * - Tên file là UUID ngẫu nhiên nên không đoán được và không trùng.
 */
@Service
public class PostImageStorage {

    private static final int MAX_SIDE = 1600;
    private static final String URL_PREFIX = "/api/post-images/";
    private static final String KEY_PATTERN = "[a-f0-9-]{36}[.](png|jpg)";

    private final Path directory;

    public PostImageStorage(StoragePaths paths) {
        this.directory = paths.postImages();
    }

    /** Ảnh đọc ra để trả về trình duyệt. */
    public record StoredImage(byte[] bytes, String contentType) {
    }

    /**
     * Lưu ảnh mới.
     *
     * @return URL của ảnh, VD: "/api/post-images/xxxx.png"
     */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("Chọn ảnh PNG hoặc JPEG");
        }
        if (file.getSize() > AppConstants.MAX_POST_IMAGE_SIZE) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE, "Ảnh tối đa 5 MB");
        }

        Path target = null;
        try {
            // Bước 1: Đọc ảnh gốc
            ImageUtils.DecodedImage original = ImageUtils.decode(file.getBytes());
            boolean isPng = original.format().equals("png");

            // Bước 2: Vẽ lại và thu nhỏ
            BufferedImage redrawn = ImageUtils.redraw(original.image(), MAX_SIDE, isPng);

            // Bước 3: Ghi xuống ổ đĩa với tên ngẫu nhiên
            String extension = isPng ? "png" : "jpg";
            String key = UUID.randomUUID() + "." + extension;
            target = directory.resolve(key).normalize();

            Files.createDirectories(directory);
            try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                if (!ImageIO.write(redrawn, isPng ? "png" : "jpeg", output)) {
                    throw invalid("Không chuyển đổi được ảnh");
                }
            }
            return URL_PREFIX + key;

        } catch (ApiException e) {
            deleteQuietly(target);
            throw e;
        } catch (IOException | RuntimeException e) {
            deleteQuietly(target);
            throw invalid("Không thể xử lý ảnh PNG/JPEG. Hãy thử một ảnh khác.");
        }
    }

    /** Đọc ảnh theo tên file. Tên không hợp lệ hoặc không tồn tại -> 404. */
    public StoredImage read(String key) {
        if (!key.matches(KEY_PATTERN)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh");
        }

        Path file = directory.resolve(key).normalize();
        try {
            if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
                throw new IOException("Missing image");
            }
            String contentType = key.endsWith(".png") ? "image/png" : "image/jpeg";
            return new StoredImage(Files.readAllBytes(file), contentType);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh");
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Bỏ qua lỗi khi dọn dẹp
        }
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
