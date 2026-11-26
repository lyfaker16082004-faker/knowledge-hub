package com.knowledge.app.model.enums;

/**
 * Ai được xem bài viết.
 *
 * Database vẫn lưu dạng chuỗi (VD: "PUBLIC"). Dùng enum để không gõ sai chuỗi trong code.
 */
public enum PostVisibility {

    PUBLIC, // Mọi người
    FOLLOWERS; // Chỉ người theo dõi tác giả

    /** So sánh với giá trị chuỗi lấy từ database. VD: PostVisibility.PUBLIC.is(entity.getStatus()) */
    public boolean is(String value) {
        return name().equals(value);
    }
}
