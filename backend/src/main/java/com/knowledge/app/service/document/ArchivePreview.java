package com.knowledge.app.service.document;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.springframework.http.HttpStatus;

import com.knowledge.app.common.exception.ApiException;

/**
 * Kiểm tra và xem trước file nén (ZIP, RAR).
 *
 * Server KHÔNG BAO GIỜ giải nén file:
 * - ZIP: chỉ đọc danh sách file bên trong
 * - RAR: chỉ kiểm tra chữ ký (vài byte đầu tiên của file)
 */
public final class ArchivePreview {

    static final int MAX_ENTRIES = 10_000;   // ZIP tối đa 10.000 mục
    private static final int MAX_LISTED = 200; // Xem trước tối đa 200 mục

    /** Các loại file thực thi không được phép có trong thư viện học tập. */
    private static final Set<String> BLOCKED_EXTENSIONS =
            Set.of("exe", "msi", "scr", "com", "pif", "dll", "vbs", "lnk", "cpl");

    // "Chữ ký" (magic bytes) ở đầu mỗi loại file
    private static final byte[] ZIP_SIGNATURE = { 'P', 'K', 3, 4 };
    private static final byte[] RAR4_SIGNATURE = { 'R', 'a', 'r', '!', 0x1A, 0x07, 0x00 };
    private static final byte[] RAR5_SIGNATURE = { 'R', 'a', 'r', '!', 0x1A, 0x07, 0x01, 0x00 };

    private ArchivePreview() {
        // Class tiện ích, chỉ có hàm static
    }

    /** Một mục trong file ZIP. */
    private record Entry(String name, boolean directory, long size) {
    }

    // =========================================================
    // KIỂM TRA
    // =========================================================

    public static void validateZip(byte[] bytes) {
        if (!startsWith(bytes, ZIP_SIGNATURE)) {
            throw invalid("Nội dung không phải tệp ZIP hợp lệ");
        }

        List<Entry> entries = readZip(bytes);
        if (entries.isEmpty()) {
            throw invalid("Tệp ZIP không có nội dung");
        }

        // Không cho phép file thực thi bên trong
        for (Entry entry : entries) {
            if (entry.directory()) {
                continue;
            }
            String name = entry.name().toLowerCase(Locale.ROOT);
            String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
            if (BLOCKED_EXTENSIONS.contains(extension)) {
                throw invalid("Tệp ZIP chứa tệp thực thi (" + entry.name() + "). Hãy bỏ tệp này rồi nén lại");
            }
        }
    }

    public static void validateRar(byte[] bytes) {
        if (!startsWith(bytes, RAR4_SIGNATURE) && !startsWith(bytes, RAR5_SIGNATURE)) {
            throw invalid("Nội dung không phải tệp RAR hợp lệ");
        }
    }

    // =========================================================
    // XEM TRƯỚC
    // =========================================================

    /** Tạo nội dung xem trước dạng văn bản. Với RAR không bao giờ ném lỗi. */
    public static String describe(byte[] bytes, String fileName) {
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".rar")) {
            return "Tệp nén RAR (" + formatSize(bytes.length) + ").\n\n"
                    + "Chưa hỗ trợ xem danh sách tệp bên trong RAR. "
                    + "Tải về và giải nén bằng WinRAR hoặc 7-Zip để xem nội dung.";
        }

        // Sắp xếp theo đường dẫn để các file trong cùng thư mục đứng cạnh nhau
        List<Entry> entries = new ArrayList<>(readZip(bytes));
        entries.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        // Đếm số file và tổng dung lượng
        long fileCount = 0;
        long totalSize = 0;
        for (Entry entry : entries) {
            if (!entry.directory()) {
                fileCount++;
            }
            if (entry.size() > 0) {
                totalSize += entry.size();
            }
        }

        // Dòng tiêu đề
        StringBuilder text = new StringBuilder();
        text.append("Tệp nén ZIP gồm ").append(fileCount).append(" tệp");
        if (totalSize > 0) {
            text.append(", khoảng ").append(formatSize(totalSize)).append(" sau khi giải nén");
        }
        text.append(".\n\n");

        // Liệt kê từng mục (tối đa MAX_LISTED mục)
        int limit = Math.min(entries.size(), MAX_LISTED);
        for (int i = 0; i < limit; i++) {
            Entry entry = entries.get(i);
            if (entry.directory()) {
                text.append("[thư mục] ").append(entry.name());
            } else {
                text.append("  ").append(entry.name());
                if (entry.size() >= 0) {
                    text.append(" (").append(formatSize(entry.size())).append(")");
                }
            }
            text.append('\n');
        }

        if (entries.size() > MAX_LISTED) {
            text.append("\n… và ").append(entries.size() - MAX_LISTED).append(" mục khác.");
        }
        return text.toString();
    }

    // =========================================================
    // ĐỌC FILE ZIP
    // =========================================================

    private static List<Entry> readZip(byte[] bytes) {
        Path tempFile = null;
        try {
            // ZipFile cần đọc từ file trên đĩa -> ghi ra file tạm
            tempFile = Files.createTempFile("kh-archive-", ".zip");
            Files.write(tempFile, bytes);

            try {
                return listEntries(tempFile, StandardCharsets.UTF_8);
            } catch (ZipException | IllegalArgumentException utf8Failed) {
                // Các công cụ cũ trên Windows lưu tên file bằng bảng mã CP437 thay vì UTF-8
                return listEntries(tempFile, Charset.forName("IBM437"));
            }
        } catch (ApiException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw invalid("Không đọc được cấu trúc tệp ZIP");
        } finally {
            deleteQuietly(tempFile);
        }
    }

    /** Chỉ đọc "mục lục" của file ZIP, không giải nén dữ liệu. */
    private static List<Entry> listEntries(Path file, Charset charset) throws IOException {
        List<Entry> entries = new ArrayList<>();
        try (ZipFile zip = new ZipFile(file.toFile(), charset)) {
            if (zip.size() > MAX_ENTRIES) {
                throw invalid("Tệp ZIP có quá nhiều tệp (tối đa " + MAX_ENTRIES + ")");
            }
            Enumeration<? extends ZipEntry> all = zip.entries();
            while (all.hasMoreElements()) {
                ZipEntry entry = all.nextElement();
                entries.add(new Entry(entry.getName(), entry.isDirectory(), entry.getSize()));
            }
        }
        return entries;
    }

    // =========================================================
    // HÀM HỖ TRỢ
    // =========================================================

    /** Kiểm tra mảng byte có bắt đầu bằng "prefix" không. */
    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /** Định dạng dung lượng dễ đọc. VD: 2048 -> "2.0 KB". */
    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024));
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Bỏ qua lỗi khi xoá file tạm
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
