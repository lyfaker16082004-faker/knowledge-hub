package com.knowledge.app.model.enums;

/**
 * Loại bài viết.
 *
 * Database vẫn lưu dạng chuỗi (VD: "QUESTION"). Dùng enum để không gõ sai chuỗi trong code.
 */
public enum PostType {

    QUESTION, // Câu hỏi
    ARTICLE; // Bài chia sẻ

    /** So sánh với giá trị chuỗi lấy từ database. VD: PostType.QUESTION.is(entity.getStatus()) */
    public boolean is(String value) {
        return name().equals(value);
    }
}
