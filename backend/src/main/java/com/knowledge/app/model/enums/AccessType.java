package com.knowledge.app.model.enums;

/**
 * Loại truy cập của tài liệu.
 *
 * Database vẫn lưu dạng chuỗi (VD: "FREE"). Dùng enum để không gõ sai chuỗi trong code.
 */
public enum AccessType {

    FREE, // Miễn phí
    PAID; // Có phí

    /** So sánh với giá trị chuỗi lấy từ database. VD: AccessType.FREE.is(entity.getStatus()) */
    public boolean is(String value) {
        return name().equals(value);
    }
}
