# Architecture

Backend: Java 21+, Spring Boot 4, MySQL 8, Maven Wrapper. Controller -> service interface -> service implementation -> Spring Data JPA repository. DTO tách API khỏi entity. Flyway V1–V9 và Hibernate validate quản lý 24 bảng; V5 thêm users.auth_version.

Authentication: JWT HS256 (24h) + Spring Security Nimbus JOSE, BCrypt và phân vai trò ADMIN/MODERATOR/USER. JwtAuthenticationFilter kiểm chữ ký/issuer/audience/expiry, account status/auth_version và roles hiện tại. Stateless Bearer; không dùng cookie xác thực. Xem accounts.md.

Frontend: React + TypeScript + Vite. features/auth, features/user và features/admin chứa API/types/components/pages. shared/api xử lý lỗi và JWT trong sessionStorage; shared/components/ui chứa các thành phần dùng chung. App quản lý phiên và điều hướng hash cho home, documents, document detail, about, mine, account, users, organizations, people và person/{id}. features/documents và features/about chứa các trang công khai; MyLibraryPage đọc lịch sử/bookmark từ API. Vite proxy /api tới localhost:8080.

Dev và prod dùng MySQL; test mặc định dùng H2. Test tích hợp MySQL tùy chọn kiểm tra ràng buộc và bảo mật trên MySQL thật. Playwright kiểm tra UI desktop/mobile và các luồng tài khoản.

Unicode: Java String, UTF-8 source/JDBC và MySQL utf8mb4_unicode_ci. Không dùng @Nationalized/NVARCHAR vì MySQL NVARCHAR ngụ ý utf8mb3. SQL snapshot trong database/knowledge_hub.sql.

Thư viện và trang cá nhân có API thực: catalogue, protected download, bookmark và download history. File mẫu riêng tư nằm trong classpath:library; quyền tải tài liệu có phí được kiểm tra ở backend. Upload PDF/DOCX/MD/TXT có API riêng tư và form tự điền tổ chức. Có API/UI theo dõi, danh sách cộng đồng và đọc feed bài đã xuất bản. Tạo/sửa bài hỏi đáp, bình luận, thanh toán và mô hình AI chưa có luồng nghiệp vụ. Redis chưa bắt buộc; Docker chưa dùng.
Organizations: institutions -> faculties, tham chiếu nullable trong profiles/documents/posts. Khóa ngoại kép giữ khoa đúng trường. OrganizationService xác thực selection; ProfileService xử lý hồ sơ. Thư viện lọc tổ chức trước, sort=popular dùng lượt tải 7 ngày. PrivateDocumentStorage lưu UUID ngoài static, kiểm tra định dạng và dọn khi rollback. Xem organizations.md.
