package com.knowledge.app.common.constant;

/**
 * Các hằng số dùng chung trong toàn ứng dụng.
 */
public final class AppConstants {

    private AppConstants() {
        // Chỉ chứa hằng số, không tạo đối tượng
    }

    /** Khi người xem chưa đăng nhập, dùng id = -1 trong câu SQL (không trùng với người dùng nào). */
    public static final long GUEST_USER_ID = -1;

    /** Mỗi bài viết tối đa 5 nhãn. */
    public static final int MAX_POST_TAGS = 5;

    /** File tài liệu tối đa 20 MB. */
    public static final long MAX_DOCUMENT_SIZE = 20L * 1024 * 1024;

    /** Ảnh đại diện phải nhỏ hơn 2 MB. */
    public static final long MAX_AVATAR_SIZE = 2L * 1024 * 1024;

    /** Ảnh chèn trong bài viết tối đa 5 MB. */
    public static final long MAX_POST_IMAGE_SIZE = 5L * 1024 * 1024;
}
