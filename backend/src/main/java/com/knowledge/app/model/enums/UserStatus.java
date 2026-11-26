package com.knowledge.app.model.enums;

/**
 * Trạng thái tài khoản người dùng.
 *
 * Database vẫn lưu dạng chuỗi (VD: "ACTIVE"). Dùng enum để không gõ sai chuỗi trong code.
 */
public enum UserStatus {

    ACTIVE, // Đang hoạt động
    LOCKED, // Bị khoá
    DELETED; // Đã xoá (xoá mềm)

    /** So sánh với giá trị chuỗi lấy từ database. VD: UserStatus.ACTIVE.is(entity.getStatus()) */
    public boolean is(String value) {
        return name().equals(value);
    }
}
