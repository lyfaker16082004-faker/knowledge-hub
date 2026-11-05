# Thiết kế MySQL và lưu tiếng Việt đúng cách

Tài liệu mẫu do Knowledge Hub biên soạn để minh họa chức năng thư viện.

## 1. Tạo database Unicode
```sql
CREATE DATABASE learning CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE learning;
CREATE TABLE notes (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 title VARCHAR(255) NOT NULL,
 content TEXT NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 2. Ràng buộc
Khóa ngoại ngăn dữ liệu mồ côi. UNIQUE chống trùng dữ liệu. CHECK kiểm tra điều kiện trên một bản ghi.

## 3. Kiểm thử tiếng Việt
Chèn “Đặng Thị Hồng chia sẻ kiến thức 📚”, đọc lại bằng ứng dụng và so sánh với chuỗi gốc.

## 4. Bài tập
Tạo bảng categories và liên kết notes.category_id. Thử chèn category_id không tồn tại để xác nhận khóa ngoại hoạt động.
