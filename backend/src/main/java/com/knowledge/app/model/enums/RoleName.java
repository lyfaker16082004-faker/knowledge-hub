package com.knowledge.app.model.enums;

/**
 * Vai trò của người dùng.
 *
 * Database vẫn lưu dạng chuỗi (VD: "ADMIN"). Dùng enum để không gõ sai chuỗi trong code.
 */
public enum RoleName {

    ADMIN, // Quản trị viên
    MODERATOR, // Người kiểm duyệt
    USER; // Người dùng thường

    /** So sánh với giá trị chuỗi lấy từ database. VD: RoleName.ADMIN.is(entity.getStatus()) */
    public boolean is(String value) {
        return name().equals(value);
    }
}
