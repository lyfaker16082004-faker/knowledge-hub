# Thiết kế module Spring Boot có kiểm thử

Tài liệu mẫu do Knowledge Hub biên soạn để minh họa chức năng thư viện.

## 1. Ranh giới module
Controller xử lý HTTP/DTO. Service chứa nghiệp vụ. Repository truy cập dữ liệu.

## 2. Transaction
Tạo tài khoản và gán vai trò cần cùng thành công hoặc cùng rollback.

## 3. Thu hồi phiên
Khi đổi mật khẩu hoặc quyền, tăng auth_version để phiên cũ hết hiệu lực. Không dựa vào việc giao diện ẩn nút để phân quyền.

## 4. Kiểm thử
Dùng database test riêng và rollback mỗi test. Kiểm tra khóa ngoại, unique, truy cập sai quyền và admin cuối.

## Bài tập
Xây nghiệp vụ khóa tài khoản không khóa admin cuối. Chạy hai yêu cầu đồng thời và xác nhận điều kiện vẫn được giữ.
