# Checklist xây REST API dễ sử dụng

Tài liệu mẫu do Knowledge Hub biên soạn để minh họa chức năng thư viện.

## Hợp đồng API
- Đặt tên endpoint theo tài nguyên.
- Tách DTO khỏi entity; không trả password hash hoặc đường dẫn file nội bộ.
- Định nghĩa kiểu lỗi nhất quán và HTTP status đúng ngữ nghĩa.

## Dữ liệu
- Giới hạn kích thước chuỗi và phân trang.
- Khóa ngoại và unique ở database, không chỉ kiểm tra bằng code.
- Kiểm tra quyền ở backend cho từng hành động.

## Cookie và tải file
- Cookie session cần CSRF cho thao tác ghi.
- File riêng tư chỉ được tải sau khi kiểm tra quyền.
- Lấy userId từ phiên đăng nhập, không tin userId client gửi.

## Kiểm thử
Bao gồm thành công, đầu vào sai, chưa đăng nhập, không có quyền và dữ liệu trùng.
