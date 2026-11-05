# Trang người dùng và thư viện tài liệu

## Các trang
| Đường dẫn hash | Nội dung | Truy cập |
|---|---|---|
| #home | Trang chủ, tìm nhanh, chủ đề, số liệu và tài liệu gợi ý | Công khai |
| #documents | Tìm theo tên/nội dung, lọc chủ đề, miễn phí/có phí, sắp xếp, phân trang | Công khai |
| #document/{id} | Giới thiệu, tác giả, danh sách tệp, lưu và tải | Xem công khai; tải/lưu cần đăng nhập |
| #about | Về chúng tôi, mục tiêu và giá trị của Knowledge Hub | Công khai |
| #mine | Thống kê cá nhân, lịch sử tải, tài liệu đã lưu | Đã đăng nhập |
| #account | Thông tin tài khoản và đổi mật khẩu | Đã đăng nhập |
| #users | Quản lý tài khoản và vai trò | ADMIN |

Đăng nhập từ một tài liệu sẽ quay lại đúng tài liệu đó. Trang của tôi dùng dữ liệu MySQL, không lưu lịch sử giả trong localStorage. Tải lại trang hoặc đăng nhập lại vẫn xem được lịch sử.

## API mới
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | /api/documents | q, category, access=FREE/PAID, sort=newest/title/featured, page, size (tối đa 30) |
| GET | /api/documents/categories | Danh mục có tài liệu đã công bố và số lượng |
| GET | /api/documents/stats | Số tài liệu, miễn phí, danh mục, lượt tải thực tế |
| GET | /api/documents/{id} | Chi tiết; file không lộ storageKey hoặc đường dẫn nội bộ |
| POST | /api/documents/{id}/files/{fileId}/download | JWT Bearer; trả file attachment; kiểm tra file thuộc tài liệu |
| GET | /api/users/me/library/stats | Số tài liệu khác nhau đã tải, tổng lượt tải, số đã lưu |
| GET | /api/users/me/downloads | Lịch sử của chính người dùng trong phiên, phân trang |
| GET | /api/users/me/bookmarks | Tài liệu đã lưu, phân trang |
| POST | /api/users/me/bookmarks/{id} | JSON saved: true/false; idempotent |
| POST | /api/admin/documents/{id}/access | ADMIN + JWT Bearer; JSON userId; cấp quyền thủ công cho tài liệu có phí |

## Database
V6 thêm năm bảng: documents, document_files, document_access, document_downloads, document_bookmarks. Tổng cộng 24 bảng nghiệp vụ; chưa tính flyway_schema_history. Khóa ngoại kép bảo đảm file trong lịch sử thuộc đúng document_id. Giá DECIMAL(12,2), ràng buộc FREE=0 / PAID>0.

V7 thêm bốn chủ đề và bảy tài liệu mẫu do Knowledge Hub biên soạn: sáu miễn phí và một tài liệu mẫu có phí. Các file Markdown UTF-8 thực nằm trong backend/src/main/resources/library, không nằm dưới static/public. Mỗi file khớp metadata dung lượng trong database.

File có phí chỉ tải được khi có document_access chưa bị thu hồi và chưa hết hạn. Thanh toán trực tuyến, quản trị nội dung tài liệu chưa triển khai. Upload PDF/DOCX/MD/TXT tối đa 10 MB đã có ở #upload; xem organizations.md. Các tệp mẫu khởi đầu là Markdown, mở bằng VS Code hoặc trình đọc Markdown.

Lịch sử ghi nhận khi backend đọc được tệp và cấp dữ liệu cho trình duyệt; không khẳng định người dùng đã lưu xong vào ổ đĩa. Tải lại tạo một lượt mới; số tài liệu đã tải đếm riêng theo document_id. Request bị từ chối hoặc tệp không tồn tại không tạo lịch sử.

## Kiểm tra
- Kiểm thử backend H2/MySQL pass, gồm các luồng H2/MySQL về phân quyền, file có phí, file sai document, JWT, cách ly lịch sử và Unicode.
- 5 test Playwright pass: đăng nhập/quản trị, đăng ký/đổi mật khẩu mobile, trang công khai/bộ lọc, tải file thật/lưu/lịch sử sau reload.
- Frontend build TypeScript + Vite pass.

Chạy cả backend/frontend trước `npm.cmd run test:e2e`. Tài khoản test được ghi trong manifest local backend/config/e2e-*.json. Dọn chính xác fixture bằng `./scripts/cleanup-e2e.ps1` ở thư mục gốc; script kiểm tra cả tên và email trước khi xóa trong transaction. Tài khoản người dùng thật không nằm trong manifest sẽ được giữ nguyên.
V8 removes optional moderation/logging, advanced token, notification, collection and post-revision tables. These features are outside the project scope; document_bookmarks and document_downloads remain functional.

V9 bổ sung trường/khoa và scope theo profile. GET /api/documents hỗ trợ institutionId, facultyId, global=true và sort=popular. POST /api/documents nhận multipart metadata/file. Thông tin đầy đủ tại organizations.md.
