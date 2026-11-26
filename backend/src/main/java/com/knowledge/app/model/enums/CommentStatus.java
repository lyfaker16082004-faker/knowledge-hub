package com.knowledge.app.model.enums;

/**
 * Trạng thái của bình luận.
 *
 * Database vẫn lưu dạng chuỗi (VD: "VISIBLE"). Dùng enum để không gõ sai chuỗi trong code.
 */
public enum CommentStatus {

    VISIBLE, // Đang hiển thị
    HIDDEN, // Bị quản trị viên ẩn (có thể khôi phục)
    DELETED; // Đã xoá

    /** So sánh với giá trị chuỗi lấy từ database. VD: CommentStatus.VISIBLE.is(entity.getStatus()) */
    public boolean is(String value) {
        return name().equals(value);
    }
}
