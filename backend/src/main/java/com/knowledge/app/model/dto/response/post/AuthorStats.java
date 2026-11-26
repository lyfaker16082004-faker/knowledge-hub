package com.knowledge.app.model.dto.response.post;

/**
 * Thống kê hoạt động cộng đồng của một thành viên (hiện trên trang hồ sơ).
 */
public record AuthorStats(
        long posts,             // Tổng số bài đang hiển thị
        long questions,         // Số câu hỏi
        long articles,          // Số bài chia sẻ kiến thức
        long answers,           // Số bình luận / câu trả lời đã viết
        long likesReceived,     // Tổng lượt thích các bài của mình nhận được
        long commentsReceived,  // Tổng bình luận người khác viết trên bài của mình
        long views,             // Tổng lượt xem các bài của mình
        long helpfulVotes,      // Tổng điểm bình chọn các câu trả lời của mình
        long documents) {       // Số tài liệu đã công bố trong thư viện
}
