-- Thông báo bình luận: lưu thời điểm mỗi người dùng xem thông báo lần cuối.
-- Bình luận mới hơn thời điểm này được tính là "chưa đọc".
CREATE TABLE IF NOT EXISTS notification_reads (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  seen_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_notification_reads_user UNIQUE (user_id),
  CONSTRAINT fk_notification_reads_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Giúp tìm nhanh bình luận theo bài viết / bình luận cha
CREATE INDEX ix_comments_parent_created ON comments (parent_id, created_at);
