-- Chủ đề "Khác": dùng khi bài viết / tài liệu chưa có chủ đề phù hợp.
-- Người dùng mô tả chủ đề cụ thể bằng thẻ (tag); admin có thể tạo chủ đề mới sau khi thấy nhiều bài cùng thẻ.
-- sort_order lớn để "Khác" luôn nằm cuối danh sách.
INSERT IGNORE INTO categories (name, slug, sort_order, active, created_at, updated_at, version)
VALUES ('Khác', 'khac', 999, true, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0);
