package com.knowledge.app.service.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.knowledge.app.common.exception.ApiException;

@Service
public class DocumentSamplePreview {
    private static final int MAX_PREVIEW_CHARACTERS = 2400;
    private static final int MAX_PDF_PAGES = 2;
    private final OfficeTextPreview officeTextPreview;

    public DocumentSamplePreview(OfficeTextPreview officeTextPreview) {
        this.officeTextPreview = officeTextPreview;
    }

    public Sample create(byte[] bytes, String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".pdf")) return new Sample(firstPdfPages(bytes), fileName, "application/pdf");
        String text;
        if (lowerName.endsWith(".docx") || lowerName.endsWith(".pptx")) {
            text = officeTextPreview.extract(bytes);
        } else if (lowerName.endsWith(".md") || lowerName.endsWith(".txt")) {
            text = new String(bytes, StandardCharsets.UTF_8);
        } else {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng này chưa hỗ trợ xem thử");
        }
        if (text.length() > MAX_PREVIEW_CHARACTERS) {
            text = text.substring(0, MAX_PREVIEW_CHARACTERS)
                    + "\n\n… Bản xem thử đã dừng. Tải bản đầy đủ để đọc tiếp.";
        }
        return new Sample(text.getBytes(StandardCharsets.UTF_8), fileName, "text/plain");
    }

    private byte[] firstPdfPages(byte[] bytes) {
        try (PDDocument original = Loader.loadPDF(bytes);
             PDDocument excerpt = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int pageCount = Math.min(MAX_PDF_PAGES, original.getNumberOfPages());
            if (pageCount == 0) throw new IOException("PDF không có trang");
            for (int page = 0; page < pageCount; page++) excerpt.importPage(original.getPage(page));
            excerpt.save(output);
            return output.toByteArray();
        } catch (IOException | RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể tạo bản xem thử PDF. Hãy tải tệp đầy đủ để kiểm tra");
        }
    }

    public record Sample(byte[] bytes, String fileName, String contentType) {}
}
