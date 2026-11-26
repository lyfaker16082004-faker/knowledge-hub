package com.knowledge.app.model.dto.response.post;

/**
 * Kết quả sau khi tải ảnh lên. Frontend chèn URL này vào nội dung: ![mô tả](url)
 */
public record UploadedImage(String url) {
}
