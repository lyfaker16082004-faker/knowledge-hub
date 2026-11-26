package com.knowledge.app.model.enums;

/**
 * Trạng thái của bài viết.
 *
 * Database vẫn lưu dạng chuỗi (VD: "PUBLISHED"). Dùng enum để không gõ sai chuỗi trong code.
 */
public enum PostStatus {

    PUBLISHED, // Đã đăng
    REJECTED, // Bị quản trị viên ẩn (có thể khôi phục)
    ARCHIVED; // Tác giả đã xoá

    /** So sánh với giá trị chuỗi lấy từ database. VD: PostStatus.PUBLISHED.is(entity.getStatus()) */
    public boolean is(String value) {
        return name().equals(value);
    }
}
