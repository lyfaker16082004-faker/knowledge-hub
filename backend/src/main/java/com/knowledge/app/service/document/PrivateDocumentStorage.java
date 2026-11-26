package com.knowledge.app.service.document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.constant.AppConstants;
import com.knowledge.app.config.StoragePaths;

/**
 * Lưu và đọc file tài liệu trên ổ đĩa (thư mục riêng, người ngoài không truy cập trực tiếp được).
 *
 * Khi lưu file, class này kiểm tra rất kỹ để chống file độc hại:
 * - Giới hạn dung lượng, chỉ cho phép một số đuôi file
 * - Kiểm tra nội dung có đúng định dạng không (PDF, ZIP, Office...)
 * - Chặn file Office có chứa macro
 */
@Service
public class PrivateDocumentStorage {

    private static final long MAX_FILE_SIZE = AppConstants.MAX_DOCUMENT_SIZE; // 20 MB
    private static final long MAX_UNZIPPED_SIZE = 100L * 1024 * 1024;     // 100 MB sau khi giải nén
    private static final int MAX_ZIP_ENTRIES = 5000;

    // Tên file đã lưu có dạng: upload-<uuid>.<đuôi>
    private static final String UPLOADED_KEY_PATTERN = "upload-[a-f0-9-]{36}[.](pdf|ppt|pptx|docx|md|txt|zip|rar)";
    // File mẫu có sẵn trong thư mục resources/library
    private static final String SAMPLE_KEY_PATTERN = "[a-z0-9-]+[.]md";

    private final Path directory;

    public PrivateDocumentStorage(StoragePaths paths) {
        this.directory = paths.documents();
    }

    /** Thông tin file sau khi lưu thành công. */
    public record StoredFile(String key, String name, String contentType, long size) {
    }

    // =========================================================
    // LƯU FILE
    // =========================================================

    public StoredFile store(MultipartFile file) {
        // Bước 1: Kiểm tra file có rỗng hoặc quá lớn không
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw invalid("Chọn tệp không rỗng, dung lượng tối đa 20 MB");
        }

        // Bước 2: Làm sạch tên file
        String name = cleanFileName(file.getOriginalFilename());
        if (name.isBlank() || name.length() > 255) {
            throw invalid("Tên tệp không hợp lệ");
        }

        // Bước 3: Lấy đuôi file và xác định content type
        String extension = "";
        if (name.contains(".")) {
            extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        String contentType = getContentType(extension);

        // Bước 4: Tạo tên lưu trữ ngẫu nhiên (tránh trùng và tránh tấn công đường dẫn)
        String key = "upload-" + UUID.randomUUID() + "." + extension;
        Path target = directory.resolve(key).normalize();
        if (!target.startsWith(directory)) {
            throw invalid("Đường dẫn tệp không hợp lệ");
        }

        try {
            byte[] bytes = file.getBytes();

            // Bước 5: Kiểm tra nội dung trước khi ghi
            validateContentBeforeSaving(extension, bytes);

            // Bước 6: Ghi file xuống ổ đĩa
            Files.createDirectories(directory);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);

            // Bước 7: Kiểm tra file Office (cần file đã ghi xuống đĩa để đọc)
            if (extension.equals("docx") || extension.equals("pptx")) {
                validateOffice(target, extension);
            }
            if (extension.equals("ppt")) {
                validatePpt(target);
            }

            // Bước 8: Nếu transaction lưu database thất bại -> tự xoá file đã ghi
            deleteFileIfTransactionFails(target);

            return new StoredFile(key, name, contentType, bytes.length);

        } catch (IOException e) {
            remove(target);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Chưa thể lưu tệp. Vui lòng thử lại sau.");
        } catch (RuntimeException e) {
            // Bao gồm cả ApiException: xoá file rồi ném lỗi ra ngoài
            remove(target);
            throw e;
        }
    }

    /**
     * Làm sạch tên file:
     * - Bỏ phần đường dẫn (VD: "C:\folder\a.pdf" -> "a.pdf")
     * - Bỏ các ký tự điều khiển (ký tự ẩn)
     */
    private String cleanFileName(String originalName) {
        if (originalName == null) {
            originalName = "";
        }
        String path = originalName.replace('\\', '/');
        String baseName = path.substring(path.lastIndexOf('/') + 1);

        StringBuilder cleaned = new StringBuilder();
        baseName.codePoints().forEach(codePoint -> {
            if (!Character.isISOControl(codePoint)) {
                cleaned.appendCodePoint(codePoint);
            }
        });
        return cleaned.toString();
    }

    /** Chỉ cho phép một số loại file. */
    private String getContentType(String extension) {
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "md":
                return "text/markdown; charset=UTF-8";
            case "txt":
                return "text/plain; charset=UTF-8";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/vnd.rar";
            default:
                throw invalid("Chỉ hỗ trợ PDF, PPT/PPTX, DOCX, Markdown, TXT, ZIP và RAR");
        }
    }

    /** Kiểm tra nội dung file có đúng với đuôi file không. */
    private void validateContentBeforeSaving(String extension, byte[] bytes) {
        // File PDF luôn bắt đầu bằng "%PDF-"
        if (extension.equals("pdf")) {
            boolean isPdf = bytes.length >= 5
                    && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
            if (!isPdf) {
                throw invalid("Nội dung không phải tệp PDF");
            }
        }

        if (extension.equals("zip")) {
            ArchivePreview.validateZip(bytes);
        }
        if (extension.equals("rar")) {
            ArchivePreview.validateRar(bytes);
        }

        // File văn bản phải dùng mã hoá UTF-8 để hiển thị đúng tiếng Việt
        if (extension.equals("md") || extension.equals("txt")) {
            checkUtf8(bytes);
        }
    }

    private void checkUtf8(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException e) {
            throw invalid("Tệp văn bản cần sử dụng UTF-8 để giữ tiếng Việt");
        }
    }

    /**
     * Đăng ký: khi transaction kết thúc mà KHÔNG commit thành công thì xoá file.
     * Nhờ vậy không bị "file rác" trên ổ đĩa khi lưu database lỗi.
     */
    private void deleteFileIfTransactionFails(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Uploads require a transaction");
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
    // ĐỌC FILE
    // =========================================================

    public byte[] read(String key) {
        try {
            // Trường hợp 1: file người dùng đã tải lên
            if (key.matches(UPLOADED_KEY_PATTERN)) {
                Path target = directory.resolve(key).normalize();
                boolean unavailable = !target.startsWith(directory)
                        || !Files.isRegularFile(target)
                        || Files.size(target) > MAX_FILE_SIZE;
                if (unavailable) {
                    throw invalid("Tệp không khả dụng");
                }
                return Files.readAllBytes(target);
            }

            // Trường hợp 2: file mẫu có sẵn trong resources
            if (key.matches(SAMPLE_KEY_PATTERN)) {
                try (InputStream stream = new ClassPathResource("library/" + key).getInputStream()) {
                    return stream.readAllBytes();
                }
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Tệp tài liệu tạm thời không khả dụng");
        }

        throw new ApiException(HttpStatus.NOT_FOUND, "Tệp không khả dụng");
    }

    // =========================================================
    // KIỂM TRA FILE OFFICE
    // =========================================================

    /**
     * File DOCX/PPTX thực chất là file ZIP. Kiểm tra:
     * - Có đủ các file bắt buộc bên trong
     * - Không có macro (vbaProject)
     * - Không có đường dẫn nguy hiểm, không quá nhiều file, không quá lớn khi giải nén (chống "zip bomb")
     */
    private void validateOffice(Path file, String extension) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            String requiredEntry = extension.equals("pptx") ? "ppt/presentation.xml" : "word/document.xml";
            if (zip.getEntry("[Content_Types].xml") == null || zip.getEntry(requiredEntry) == null) {
                throw invalid("Tệp Office không hợp lệ");
            }

            long totalSize = 0;
            int entryCount = 0;
            Set<String> seenNames = new HashSet<>();

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                entryCount++;
                totalSize += entry.getSize();

                boolean isDuplicate = !seenNames.add(entryName);
                boolean isDangerousPath = entryName.contains("..") || entryName.startsWith("/");
                boolean hasMacro = entryName.toLowerCase(Locale.ROOT).contains("vbaproject");
                boolean tooManyEntries = entryCount > MAX_ZIP_ENTRIES;
                boolean unknownSize = entry.getSize() < 0;
                boolean tooLarge = totalSize > MAX_UNZIPPED_SIZE;

                if (isDuplicate || isDangerousPath || hasMacro || tooManyEntries || unknownSize || tooLarge) {
                    throw invalid("Tệp Office có macro, cấu trúc sai hoặc vượt giới hạn giải nén");
                }

                checkRealEntrySize(zip, entry);
            }
        } catch (IOException e) {
            throw invalid("Không đọc được cấu trúc Office");
        }
    }

    /** Đọc thử nội dung entry để chắc chắn kích thước thật không lớn hơn kích thước khai báo. */
    private void checkRealEntrySize(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream input = zip.getInputStream(entry)) {
            long actualSize = 0;
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                actualSize += bytesRead;
                if (actualSize > MAX_UNZIPPED_SIZE || actualSize > entry.getSize()) {
                    throw invalid("Nội dung Office vượt giới hạn");
                }
            }
        }
    }

    /** File PPT (định dạng cũ): kiểm tra cấu trúc và chặn macro. */
    private void validatePpt(Path file) {
        try (InputStream input = Files.newInputStream(file);
                POIFSFileSystem fileSystem = new POIFSFileSystem(input)) {
            DirectoryNode root = fileSystem.getRoot();

            boolean validStructure = root.hasEntry("PowerPoint Document") && root.hasEntry("Current User");
            boolean hasMacro = root.hasEntry("VBA") || root.hasEntry("_VBA_PROJECT_CUR");

            if (!validStructure || hasMacro) {
                throw invalid("Tệp PPT không hợp lệ hoặc có macro");
            }
        } catch (IOException | IllegalArgumentException e) {
            throw invalid("Không đọc được tệp PPT");
        }
    }

    // =========================================================
    // HÀM HỖ TRỢ
    // =========================================================

    /** Xoá file, nếu lỗi thì bỏ qua (chỉ là dọn dẹp). */
    private void remove(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Bỏ qua lỗi khi dọn dẹp
        }
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
