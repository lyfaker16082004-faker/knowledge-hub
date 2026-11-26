package com.knowledge.app.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Nơi duy nhất quyết định backend lưu file ở đâu.
 * Kết quả luôn giống nhau dù chạy app từ thư mục backend/ (mvnw, VS Code task)
 * hay từ thư mục gốc của repo (nút Run trong IDE).
 *
 * <pre>
 * backend/
 *   config/       cấu hình và khoá bí mật (application-dev.properties, jwt-secret.key)
 *   storage/      dữ liệu do người dùng tạo
 *     documents/  file tài liệu tải lên
 *     avatars/    ảnh đại diện đã xử lý
 *     post-images/ ảnh chèn trong bài viết cộng đồng
 *     backups/    bản sao lưu database
 * </pre>
 *
 * Có thể đổi bằng biến môi trường APP_STORAGE_DIR (cả thư mục storage)
 * hoặc APP_DOCUMENTS_STORAGE_DIR / APP_AVATARS_STORAGE_DIR.
 */
@Component
public class StoragePaths {

    private static final Logger log = LoggerFactory.getLogger(StoragePaths.class);

    private final Path home;
    private final Path documents;
    private final Path avatars;
    private final Path postImages;

    public StoragePaths(@Value("${app.storage.root:}") String storageRoot,
            @Value("${app.documents.storage-dir:}") String documentsDir,
            @Value("${app.avatars.storage-dir:}") String avatarsDir) {
        this.home = backendHome();

        // Thư mục storage gốc
        Path root;
        if (storageRoot.isBlank()) {
            root = home.resolve("storage");
        } else {
            root = resolve(storageRoot);
        }

        // Thư mục tài liệu và ảnh đại diện
        this.documents = documentsDir.isBlank() ? root.resolve("documents") : resolve(documentsDir);
        this.avatars = avatarsDir.isBlank() ? root.resolve("avatars") : resolve(avatarsDir);
        this.postImages = root.resolve("post-images");

        try {
            Files.createDirectories(documents);
            Files.createDirectories(avatars);
            Files.createDirectories(postImages);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage folders under " + root, e);
        }

        // Chuyển file từ cấu trúc thư mục cũ (nếu có)
        moveLegacyFiles(home.resolve("config/document-storage"), documents);
        moveLegacyFiles(home.resolve("config/avatar-storage"), avatars);

        log.info("Document files: {}", documents);
        log.info("Avatar files:   {}", avatars);
    }

    /**
     * Thư mục backend/: là thư mục đang chạy,
     * hoặc ./backend nếu app được chạy từ thư mục gốc của repo.
     */
    public static Path backendHome() {
        Path currentDir = Path.of("").toAbsolutePath().normalize();
        boolean runFromRepoRoot = !Files.exists(currentDir.resolve("pom.xml"))
                && Files.exists(currentDir.resolve("backend/pom.xml"));
        if (runFromRepoRoot) {
            return currentDir.resolve("backend");
        }
        return currentDir;
    }

    public Path home() {
        return home;
    }

    public Path documents() {
        return documents;
    }

    public Path avatars() {
        return avatars;
    }

    public Path postImages() {
        return postImages;
    }

    /** Đường dẫn tương đối thì tính từ thư mục backend/. */
    private Path resolve(String configured) {
        Path path = Path.of(configured);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return home.resolve(path).normalize();
    }

    /**
     * Chuyển file một lần từ cấu trúc cũ (backend/config/*-storage) sang thư mục mới,
     * để các file đã tải lên trước đây vẫn dùng được sau khi cập nhật.
     */
    private static void moveLegacyFiles(Path legacyDir, Path targetDir) {
        if (!Files.isDirectory(legacyDir) || legacyDir.equals(targetDir)) {
            return;
        }

        // Bước 1: Chuyển từng file
        int movedCount = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(legacyDir)) {
            for (Path file : files) {
                Path destination = targetDir.resolve(file.getFileName());
                if (Files.isRegularFile(file) && !Files.exists(destination)) {
                    Files.move(file, destination, StandardCopyOption.ATOMIC_MOVE);
                    movedCount++;
                }
            }
        } catch (IOException e) {
            log.warn("Could not move files from {} to {}: {}", legacyDir, targetDir, e.getMessage());
            return;
        }

        // Bước 2: Xoá thư mục cũ nếu đã rỗng
        try (DirectoryStream<Path> remaining = Files.newDirectoryStream(legacyDir)) {
            if (!remaining.iterator().hasNext()) {
                Files.delete(legacyDir);
            }
        } catch (IOException ignored) {
            // Thư mục cũ còn file thì giữ nguyên
        }

        if (movedCount > 0) {
            log.info("Moved {} file(s) from {} to {}", movedCount, legacyDir, targetDir);
        }
    }
}
