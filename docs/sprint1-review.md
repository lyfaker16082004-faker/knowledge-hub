# Nghiệm thu Sprint 1

Project: Knowledge Hub. Ngày cập nhật: 05/11/2026.

Nguồn AC: “Hoàn Thành xác định yêu cầu hệ thống.docx”. Tài liệu được dùng để đối chiếu yêu cầu; không sửa file DOCX gốc. Yêu cầu mới của người dùng về khu quản trị riêng, sidebar, chi tiết và xóa mềm cũng được triển khai. Bản đánh giá trước triển khai: sprint1-audit-before.md.

Kết luận: Sprint 1 (US1.1–US1.5) đã hoàn thành theo phạm vi đối chiếu, kèm khu quản trị riêng và quản lý người dùng bổ sung.

## Kết quả triển khai

| Story | Kết quả / tiêu chí đã có | Màn hình |
|---|---|---|
| US1.1 Đăng ký | Email + mật khẩu tối thiểu 8 ký tự, username tùy chọn/tự sinh; email trùng bị từ chối; mặc định USER; chọn trường/khoa đúng cặp | #register |
| US1.2 Đăng nhập | Email hoặc username; JWT HS256 24 giờ; kiểm chữ ký/expiry/issuer/audience; LOCKED không đăng nhập; đổi mật khẩu/khóa/xóa mềm/logout thu hồi token cũ | #login |
| US1.3 Hồ sơ | Tên hiển thị, bio tiếng Việt, avatar PNG/JPEG nhỏ hơn 2 MiB; dropdown trường/khoa, OTHER ẩn khoa; API trả đủ hồ sơ, chỉ sửa chính mình | #account |
| US1.4 Follow | Khám phá/tìm thành viên, hồ sơ công khai, follow/unfollow, danh sách đang theo dõi/người theo dõi; feed chỉ bài đã xuất bản PUBLIC/FOLLOWERS của người theo dõi | #people, #person/{id} |
| US1.5 Admin Trường/Khoa | List/create/update/delete; ADMIN; tên trùng, khoa sai trường, OTHER và xóa dữ liệu đang dùng được kiểm soát | #admin/organizations |
| Bổ sung quản trị người dùng | Layout riêng có sidebar; tìm kiếm/phân trang, tạo/sửa/phân vai trò/reset, xem chi tiết và xóa mềm có xác nhận; giữ dữ liệu và admin ACTIVE cuối cùng | #admin/users |

Xóa mềm dùng status=DELETED, giữ user/profile/tài liệu/lịch sử, vô hiệu hóa JWT. Có thể khôi phục bằng Sửa → Hoạt động. Không thêm bảng permission hoặc các bảng tính năng phụ đã bỏ. Schema vẫn 24 bảng, migration V1–V9 giữ nguyên.

Feed ở Sprint 1 là danh sách đọc từ posts đã xuất bản; chưa có bài thì UI hiện trạng thái trống. Tạo/sửa câu hỏi, bình luận và vote thuộc Sprint 3, không được đánh dấu hoàn thành trong Sprint 1.

## Kiểm chứng

- Backend: 74 test PASS, 0 failure/error/skipped với .\mvnw.cmd -Dmysql.it=true test. Bao gồm H2, MySQL 8.0.45 và kiểm chứng Flyway/schema. MySQL dùng schema knowledge_hub_account_test riêng.
- Browser E2E: 9 test PASS (33.8s), kiểm tra tài khoản, thư viện, trường/khoa, hồ sơ/ảnh, follow, quản trị riêng/sidebar, chi tiết và xóa mềm; desktop 1440px và mobile 390px. QA dùng cổng 18080/15173.
- Frontend TypeScript + Vite production build: PASS.
- Đã xem ảnh chụp desktop/mobile. Dữ liệu tài khoản, avatar, tài liệu upload kiểm thử được dọn theo manifest chính xác bằng scripts/cleanup-e2e.ps1; danh mục thử được xóa qua UI.
- Log: backend/sprint1-final-tests.log, backend/sprint1-browser-tests.log, backend/sprint1-frontend-build.log (ignored); báo cáo máy: backend/target/surefire-reports.
- Minh chứng: [Quản lý người dùng](sprint1-evidence/admin-users.png), [Trường/Khoa](sprint1-evidence/organizations.png), [Hồ sơ](sprint1-evidence/profile.png).

## Cách kiểm tra trong VS Code

1. Khởi động lại backend: từ thư mục backend: .\mvnw.cmd spring-boot:run.
2. Chạy frontend: từ thư mục frontend: npm.cmd run dev. Mở http://127.0.0.1:5173.
3. Đăng ký email/mật khẩu từ 8 ký tự; vào Trang của tôi → Tài khoản, sửa tên/bio/ảnh và trường/khoa.
4. Vào Cộng đồng, mở hồ sơ thành viên khác, theo dõi rồi kiểm tra Đang theo dõi/Bài viết mới.
5. Đăng nhập ADMIN → Quản trị. Sidebar Người dùng có Chi tiết, Sửa, Mật khẩu, Xóa mềm. Sidebar Trường / Khoa cho phép CRUD trường và khoa trực thuộc.

Backend test: .\mvnw.cmd -Dmysql.it=true test. Frontend: npm.cmd run build; npm.cmd run test:e2e. E2E đọc admin khởi tạo từ config/initial-admin.json hoặc E2E_ADMIN_USERNAME/E2E_ADMIN_PASSWORD; nếu đã đổi mật khẩu cần đặt biến môi trường tương ứng. File khóa/ảnh/tài khoản local nằm backend/config và được Git bỏ qua.

## Toàn bộ User Story còn lại trong tài liệu

| Story | Đánh giá | Đối chiếu với code hiện tại |
|---|---|---|
| US2.1 Upload tài liệu | Một phần | Có PDF/DOCX/MD/TXT, tiêu đề/tóm tắt/chủ đề, trường/khoa điền sẵn. Giới hạn hiện 10 MB, AC là 20 MB; chưa có Slide/PPT/PPTX và tags cho document |
| US2.2 CRUD tài liệu cá nhân | Chưa đầy đủ | Có tạo/upload. Chưa có danh sách tài liệu mình đăng, API/UI sửa mô tả hoặc xóa của tác giả; lịch sử đã tải không phải danh sách đã đăng |
| US2.3 Chi tiết và Download | Đã có theo AC nêu trong file | Có trang chi tiết, download trả file và tạo document_downloads sau khi đọc được tệp; test về file thật và lịch sử đã có |
| US2.4 Search và Filter | Một phần | Có LIKE trên title/summary và lọc category/trường/khoa. Chưa có MySQL FULLTEXT/MATCH AGAINST hay thực thể môn học riêng; cần thống nhất mức full-text/môn học mà Story yêu cầu |
| US2.5 Bookmark | Đã có luồng chính | Có lưu/bỏ lưu, API theo user và tab Đã lưu trong Trang của tôi |
| US2.6 Duyệt tài liệu | Chưa có | Upload hiện công bố PUBLISHED ngay. Chưa có PENDING/REJECTED và UI duyệt/từ chối |
| US2.7 Admin CRUD kho tài liệu | Chưa có đầy đủ | Admin có thể dùng upload chung và cấp quyền file có phí; chưa có quản trị sửa/xóa/ẩn tài liệu bất kỳ |
| US3.1 Câu hỏi Markdown và ảnh, tối đa 5 tags | Chưa triển khai luồng | Mới có nền tảng posts/tags/attachments; chưa có API/UI đăng hỏi đáp và ảnh |
| US3.2 Bình luận đa cấp | Chưa triển khai luồng | Có cấu trúc comments.parent, chưa có API/UI bình luận 2 cấp |
| US3.3 Upvote/Downvote | Chưa triển khai luồng | Có bảng likes nhưng chưa có API/UI vote và cơ chế điểm âm/dương |
| US3.4 Accepted Answer | Chưa triển khai | Chưa có trường/luồng chọn câu trả lời đúng |
| US4.1 Tracking View trên 5 giây | Chưa triển khai | Ghi download đã có; chưa có timer/view event trên 5 giây cho bài viết |
| US4.2 TF-IDF/Cosine, Top 5 liên quan | Chưa triển khai | Chưa có Python service/NLP và widget liên quan theo ngữ nghĩa |
| US4.3 Gợi ý tag tự động, tùy chọn | Chưa triển khai | Chưa có trích xuất/đề xuất tags bằng AI |
| US5.1 Collaborative Filtering | Chưa triển khai mô hình | sort=popular hiện đếm lượt tải 7 ngày sau khi lọc tổ chức; chưa phân tích người dùng tương tự/cross-recommendation bằng Python |
| US5.2 Report Abuse | Khác phạm vi đã thống nhất trước đó | Chưa có luồng report; reports đã bị bỏ theo yêu cầu tinh gọn trước. Tài liệu vẫn liệt kê nên cần cập nhật backlog hoặc khôi phục phạm vi khi được chốt |
| US5.3 Dashboard hệ thống | Chưa đạt Story | Có một số số đếm công khai và tổng số tài khoản trong danh sách; chưa có dashboard biểu đồ user/bài đăng/Top 10 tháng |
| US5.4 Moderation | Một phần và khác phạm vi | Khóa tài khoản đã có. Chưa có danh sách bài bị report, ẩn bài; moderation_actions/reports đã bị bỏ theo yêu cầu trước |

Nguồn đối chiếu Sprint 2–5: [DocumentController](../backend/src/main/java/com/knowledge/app/controller/document/DocumentController.java), [DocumentServiceImpl](../backend/src/main/java/com/knowledge/app/service/impl/document/DocumentServiceImpl.java), [PrivateDocumentStorage](../backend/src/main/java/com/knowledge/app/service/document/PrivateDocumentStorage.java), [DocumentRepository](../backend/src/main/java/com/knowledge/app/repository/DocumentRepository.java), [MyLibraryPage](../frontend/src/features/user/pages/MyLibraryPage.tsx), cùng danh sách controller, frontend pages và migrations hiện có.
