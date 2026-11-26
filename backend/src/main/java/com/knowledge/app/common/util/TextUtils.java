package com.knowledge.app.common.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Các hàm xử lý chuỗi dùng chung.
 */
public final class TextUtils {

    private TextUtils() {
        // Class tiện ích, chỉ có hàm static
    }

    /**
     * Thoát các ký tự đặc biệt của LIKE trong SQL (% và _) để tìm đúng ký tự đó.
     * Dùng "!" làm ký tự thoát, nên câu SQL phải có ESCAPE '!'.
     * VD: "50%" -> "50!%".
     */
    public static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * Chuyển chuỗi tiếng Việt thành slug không dấu, tối đa 180 ký tự.
     * VD: "Học Java cơ bản" -> "hoc-java-co-ban"
     */
    public static String toSlug(String text) {
        // Bước 1: Bỏ dấu tiếng Việt (đ -> d, các dấu thanh bị tách ra rồi xoá đi)
        String noAccent = text.replace('đ', 'd').replace('Đ', 'D');
        noAccent = Normalizer.normalize(noAccent, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");

        // Bước 2: Chữ thường, thay ký tự đặc biệt bằng "-", bỏ "-" ở đầu và cuối
        String slug = noAccent.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        // Bước 3: Giới hạn độ dài
        if (slug.length() > 180) {
            slug = slug.substring(0, 180);
        }
        return slug;
    }

    /**
     * Lấy đuôi file viết hoa. VD: "bai-giang.pdf" -> "PDF".
     */
    public static String fileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
    }
}
