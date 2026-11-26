package com.knowledge.app.service.user;

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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.constant.AppConstants;
import com.knowledge.app.common.util.ImageUtils;
import com.knowledge.app.config.StoragePaths;

/**
 * Lưu và đọc ảnh đại diện.
 *
 * Ảnh tải lên sẽ được kiểm tra, thu nhỏ tối đa 512px và luôn lưu lại dưới dạng PNG
 * (việc vẽ lại ảnh cũng giúp loại bỏ dữ liệu ẩn/độc hại trong file gốc).
 */
@Service
public class AvatarStorage {

    private static final long MAX_FILE_SIZE = AppConstants.MAX_AVATAR_SIZE; // 2 MB
    private static final int TARGET_SIZE = 512;                    // Thu nhỏ về tối đa 512 px

    private static final String URL_PREFIX = "/api/avatars/";
    private static final String KEY_PATTERN = "[a-f0-9-]{36}[.]png";

    private final Path directory;

    public AvatarStorage(StoragePaths paths) {
        this.directory = paths.avatars();
    }

    // =========================================================
    // LƯU ẢNH
    // =========================================================

    /**
     * Lưu ảnh đại diện mới.
     *
     * @return URL của ảnh, VD: "/api/avatars/xxxx.png"
     */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("Chọn ảnh đại diện PNG hoặc JPEG");
        }
        if (file.getSize() >= MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh đại diện phải nhỏ hơn 2 MB");
        }

        String key = UUID.randomUUID() + ".png";
        Path target = directory.resolve(key).normalize();

        try {
            // Bước 1: Đọc ảnh gốc (chỉ chấp nhận PNG / JPEG)
            ImageUtils.DecodedImage original = ImageUtils.decode(file.getBytes());

            // Bước 2: Vẽ lại và thu nhỏ ảnh (giữ nền trong suốt vì luôn lưu dạng PNG)
            BufferedImage resized = ImageUtils.redraw(original.image(), TARGET_SIZE, true);

            // Bước 3: Ghi ảnh PNG xuống ổ đĩa
            Files.createDirectories(directory);
            try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                if (!ImageIO.write(resized, "png", output)) {
                    throw invalid("Không chuyển đổi được ảnh");
                }
            }

            // Bước 4: Nếu lưu database thất bại thì xoá ảnh vừa ghi
            deleteFileIfTransactionFails(target);

            return URL_PREFIX + key;

        } catch (ApiException e) {
            remove(target);
            throw e;
        } catch (IOException | RuntimeException e) {
            remove(target);
            throw invalid("Không thể xử lý ảnh PNG/JPEG. Hãy thử một ảnh khác.");
        }
    }

    private void deleteFileIfTransactionFails(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Avatar writes require transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    remove(target);
                }
            }
        });
    }

    // =========================================================
    // ĐỌC / XOÁ ẢNH
    // =========================================================

    public byte[] read(String key) {
        if (!key.matches(KEY_PATTERN)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh");
        }

        Path file = directory.resolve(key).normalize();
        try {
            if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
                throw new IOException();
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh");
        }
    }

    /**
     * Xoá ảnh cũ, nhưng chỉ xoá SAU KHI transaction commit thành công
     * (nếu lưu database lỗi thì vẫn giữ ảnh cũ).
     */
    public void deleteAfterCommit(String url) {
        if (url == null || !url.matches(URL_PREFIX + KEY_PATTERN)) {
            return;
        }

        String key = url.substring(url.lastIndexOf('/') + 1);
        Path oldFile = directory.resolve(key).normalize();
        if (!oldFile.startsWith(directory)) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                remove(oldFile);
            }
        });
    }

    // =========================================================
    // HÀM HỖ TRỢ
    // =========================================================

    private void remove(Path file) {
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
