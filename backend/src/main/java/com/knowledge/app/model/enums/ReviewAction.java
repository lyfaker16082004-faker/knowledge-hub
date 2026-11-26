package com.knowledge.app.model.enums;

/**
 * Thao tác admin khi duyệt tài liệu.
 *
 * Database vẫn lưu dạng chuỗi (VD: "APPROVE"). Dùng enum để không gõ sai chuỗi trong code.
 */
public enum ReviewAction {

    APPROVE, // Duyệt: PENDING -> PUBLISHED
    REJECT, // Từ chối: PENDING -> REJECTED
    HIDE, // Ẩn: PUBLISHED -> ARCHIVED
    RESTORE; // Khôi phục: ARCHIVED -> PUBLISHED

    /** So sánh với giá trị chuỗi lấy từ database. VD: ReviewAction.APPROVE.is(entity.getStatus()) */
    public boolean is(String value) {
        return name().equals(value);
    }
}
