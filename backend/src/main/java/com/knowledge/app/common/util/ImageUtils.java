package com.knowledge.app.common.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.http.HttpStatus;

import com.knowledge.app.common.exception.ApiException;

/**
 * Các hàm xử lý ảnh dùng chung (ảnh đại diện, ảnh trong bài viết).
 *
 * Ý tưởng an toàn: không bao giờ lưu nguyên file người dùng gửi lên.
 * Ta đọc ảnh ra thành điểm ảnh rồi VẼ LẠI thành file mới, nhờ vậy mọi dữ liệu ẩn
 * (mã độc, thông tin GPS trong ảnh chụp...) đều bị loại bỏ.
 */
public final class ImageUtils {

    private static final int MAX_DIMENSION = 4096;     // Tối đa 4096 px mỗi chiều
    private static final long MAX_PIXELS = 16_000_000; // Tối đa 16 triệu điểm ảnh

    private ImageUtils() {
        // Class tiện ích, chỉ có hàm static
    }

    /** Ảnh đã đọc được, kèm định dạng gốc: "png" hoặc "jpeg". */
    public record DecodedImage(BufferedImage image, String format) {
    }

    /**
     * Đọc ảnh PNG / JPEG từ mảng byte.
     * Kiểm tra kích thước TRƯỚC khi đọc toàn bộ ảnh để tránh tốn bộ nhớ (ảnh "bom").
     */
    public static DecodedImage decode(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalid("Nội dung không phải ảnh PNG/JPEG hợp lệ");
            }

            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg")) {
                    throw invalid("Chỉ hỗ trợ ảnh PNG và JPEG");
                }

                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                boolean invalidSize = width < 1 || height < 1
                        || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS;
                if (invalidSize) {
                    throw invalid("Ảnh quá lớn; tối đa 4096 px và 16 triệu điểm ảnh");
                }

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw invalid("Không đọc được ảnh");
                }
                return new DecodedImage(image, format);
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Vẽ lại ảnh, thu nhỏ để cạnh dài nhất tối đa "maxSide" px (ảnh nhỏ hơn thì giữ nguyên kích thước).
     *
     * @param keepTransparency true = giữ nền trong suốt (dùng cho PNG); false = nền trắng (dùng cho JPEG)
     */
    public static BufferedImage redraw(BufferedImage image, int maxSide, boolean keepTransparency) {
        int longestSide = Math.max(image.getWidth(), image.getHeight());
        double scale = Math.min(1.0, (double) maxSide / longestSide);

        int newWidth = Math.max(1, (int) (image.getWidth() * scale));
        int newHeight = Math.max(1, (int) (image.getHeight() * scale));
        int type = keepTransparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

        BufferedImage result = new BufferedImage(newWidth, newHeight, type);
        Graphics2D graphics = result.createGraphics();
        try {
            if (!keepTransparency) {
                // JPEG không có nền trong suốt -> tô nền trắng trước
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fillRect(0, 0, newWidth, newHeight);
            }
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(image, 0, 0, newWidth, newHeight, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
