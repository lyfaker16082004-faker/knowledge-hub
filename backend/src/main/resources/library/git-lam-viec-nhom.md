# Git thực hành cho một nhóm nhỏ

Tài liệu mẫu do Knowledge Hub biên soạn để minh họa chức năng thư viện.

## 1. Kiểm tra trước khi sửa
Chạy git status. Hiểu các thay đổi có sẵn của đồng đội trước khi làm việc.

## 2. Một nhánh cho một mục tiêu
```sh
git switch -c feature/document-library
git add src/
git commit -m "Add document library filters"
```
Không commit mật khẩu, file .env hoặc thư mục build.

## 3. Review
Mô tả vấn đề, hành vi thay đổi và cách kiểm thử. Commit nhỏ giúp người review theo dõi.

## 4. Xung đột
Đọc cả hai thay đổi để hiểu ý định, sửa kết quả cuối, chạy kiểm tra rồi mới đánh dấu giải quyết.
