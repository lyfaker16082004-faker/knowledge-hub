# Đối chiếu và nghiệm thu Sprint 2

Ngày: 05/11/2026. Nhánh triển khai: sprint-2.

Kết luận: US2.1–US2.7 hoàn thành trong phạm vi đã đối chiếu.

Nguồn yêu cầu: mục “SPRINT 2: Thư viện Tri thức & Lưu trữ” trong DOCX xác định yêu cầu hệ thống đã cung cấp. DOCX chỉ được đọc làm tiêu chí đối chiếu; không sửa tài liệu gốc. Các quyết định trước về MySQL local, chưa dùng Docker và bỏ các bảng tính năng phụ được giữ nguyên.

## Ma trận User Story

| Story | Tiêu chí / kết quả | Kiểm chứng chính |
|---|---|---|
| US2.1 Đăng tải tài liệu | PDF/Slide PPT/PPTX, bổ sung DOCX/MD/TXT; tối đa 20 MiB; danh mục bắt buộc; title/summary/description, tags; tổ chức gắn từ hồ sơ bởi backend | 20 MiB được chấp nhận, vượt giới hạn bị từ chối; sai loại file/macro Office bị chặn; tags tối đa 5; API tự gắn trường/khoa |
| US2.2 CRUD tài liệu cá nhân | Tạo tại Upload; danh sách đã đăng riêng; xem, sửa metadata, xóa mềm; sửa của tác giả gửi duyệt lại | Cách ly danh sách từng user; cấm sửa/xóa của người khác; version cũ trả 409; UI chỉnh sửa/xóa hoạt động |
| US2.3 Chi tiết và Download | Chi tiết đã công bố, quyền tài liệu miễn phí/có phí; file thật; mỗi lần cấp file thành công tạo một document_downloads | Hai lần tải tạo hai bản ghi; preview/từ chối/file sai không tăng lịch sử; lịch sử vẫn còn khi tài liệu bị xóa |
| US2.4 Full-text Search & Filter | MATCH AGAINST MySQL trên title/summary/description; filter môn học, trường/khoa, category, tag, FREE/PAID; phân trang/sắp xếp | Chỉ mục FULLTEXT thật; từ không liền nhau vẫn tìm được; kết hợp trường/khoa/môn học/tag; ẩn tài liệu chưa duyệt |
| US2.5 Bookmark | Lưu/bỏ lưu và danh sách Đọc sau riêng tư | Idempotent, không thấy bookmark của user khác; tài liệu ngừng công bố không hiện trong bookmark |
| US2.6 Kiểm duyệt | Danh sách PENDING; preview PDF/text và trích văn bản PPTX/DOCX; duyệt PUBLISHED hoặc từ chối REJECTED có lý do | Chỉ ADMIN; owner xem bản của mình; người khác không xem pending; không từ chối thiếu lý do; tác giả đọc lý do và gửi lại |
| US2.7 Admin CRUD kho | Thêm giáo trình chuẩn trực tiếp PUBLISHED; xem/sửa/xóa mềm mọi tài liệu; ẩn ARCHIVED và công bố lại | UI sidebar riêng; sửa metadata, preview Slide, ẩn/khôi phục/xóa; quyền API kiểm cả ở filter và service |

Trạng thái DRAFT có sẵn từ nền tảng vẫn được giữ. Quy trình mới dùng PENDING/PUBLISHED/REJECTED/ARCHIVED/DELETED. Không khôi phục reports, moderation_actions hoặc audit_logs; ghi chú/kết quả duyệt gần nhất nằm trên documents.

## Thay đổi kỹ thuật

- Migration V10: subjects, document_tags; trường subject/review trên documents; status constraint và full-text index. Tổng 26 bảng nghiệp vụ; V1–V9 giữ nguyên.
- API quản lý cá nhân/admin, optimistic version, chủ sở hữu, preview riêng tư và quyền tải độc lập với preview.
- PostgreSQL/Docker không được đưa vào luồng chạy. MySQL utf8mb4 giữ tiếng Việt và emoji.
- SQL minh chứng docs/database/knowledge_hub.sql được cập nhật cho database mới, có đủ phần V10.
- Frontend: #my-documents, #admin/documents, #admin/documents/new; môn học/tags trong upload và thư viện; trạng thái/lý do duyệt hiển thị cho tác giả; nút Làm mới cho hàng chờ.

## Kiểm chứng

- Backend cuối: 97 test PASS, 0 failure/error/skipped với Maven -Dmysql.it=true test, gồm kiểm thử hồi quy Sprint 1, H2 và MySQL 8.0.45. Test MySQL nghiệp vụ dùng knowledge_hub_account_test riêng.
- Kiểm tra SQL snapshot trên database tạm riêng: PASS, 26 bảng và chỉ mục FULLTEXT 3 cột; database thử đã được dọn.
- Kiểm thử Playwright: 11/11 PASS (37.9s), gồm hồi quy Sprint 1 và hai luồng nghiệm thu xuyên suốt Sprint 2.
- Frontend TypeScript + Vite production build: PASS.
- Đã kiểm tra ảnh desktop/mobile và dọn tài khoản/file/tags thử theo manifest chính xác.
- Minh chứng: [Tài liệu cá nhân](sprint2-evidence/author-pending.png), [Kho admin](sprint2-evidence/admin-documents.png), [Xem trước](sprint2-evidence/review-preview.png), [Mobile](sprint2-evidence/admin-mobile.png).
- Log local (ignored): backend/sprint2-final-tests.log, backend/sprint2-snapshot-check.log, backend/sprint2-browser-tests.log, backend/sprint2-frontend-build.log.

## Chạy demo nghiệm thu

1. Khởi động backend/frontend theo README. Database hiện có sẽ nhận V10 tự động bằng Flyway.
2. USER đặt trường/khoa tại Tài khoản → Upload: nhập tên, tóm tắt, chọn danh mục/môn học/tags và file ≤20 MiB. Sau upload, trạng thái Chờ duyệt; xem danh sách tại Trang của tôi → Tài liệu đã đăng.
3. ADMIN mở Quản trị → Kho tài liệu; lọc Chờ duyệt, Xem trước, mở tệp, Từ chối kèm ghi chú hoặc Duyệt.
4. USER sửa tài liệu bị từ chối và gửi lại; ADMIN bấm Làm mới rồi duyệt. Tìm tài liệu trong thư viện bằng môn học/trường/khoa/tag, tải và lưu Đọc sau.
5. ADMIN ẩn/công bố lại tài liệu hoặc thêm giáo trình Slide chuẩn. Tác giả/admin xóa tài liệu; lịch sử tải vẫn hiển thị với nhãn Không còn công bố.

Xem API và giới hạn chi tiết tại [library.md](library.md). Office preview trích văn bản, không tái hiện toàn bộ định dạng/hình ảnh; có thể tải bản gốc để kiểm tra. Thanh toán trực tuyến và AI vẫn thuộc các Sprint sau. Sprint 3 hỏi đáp/bình luận/vote chưa được đánh dấu hoàn thành.
