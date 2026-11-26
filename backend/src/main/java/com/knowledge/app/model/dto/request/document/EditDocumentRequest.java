package com.knowledge.app.model.dto.request.document;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Dữ liệu gửi lên khi sửa thông tin tài liệu (kèm version để tránh ghi đè).
 */
public record EditDocumentRequest(@NotNull @PositiveOrZero Long version,
        @NotNull @Valid UploadDocumentRequest metadata) {
}
