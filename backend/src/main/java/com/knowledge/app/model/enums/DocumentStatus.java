package com.knowledge.app.model.enums;

/**
 * Trạng thái của tài liệu.
 *
 * Database vẫn lưu dạng chuỗi (VD: "DRAFT"). Dùng enum để không gõ sai chuỗi trong code.
 */
public enum DocumentStatus {

    DRAFT, // Bản nháp
    PENDING, // Đang chờ duyệt
    PUBLISHED, // Đã công bố
    REJECTED, // Bị từ chối
    ARCHIVED, // Bị ẩn
    DELETED; // Đã xoá (xoá mềm)

    /** So sánh với giá trị chuỗi lấy từ database. VD: DocumentStatus.DRAFT.is(entity.getStatus()) */
    public boolean is(String value) {
        return name().equals(value);
    }
}
