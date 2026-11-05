> Bản kiểm tra trước khi triển khai hoàn thiện Sprint 1. Trạng thái hiện tại xem sprint1-review.md.

# Đối chiếu Sprint 1 và User Story

Ngày đánh giá: 05/11/2026.
Project: Knowledge Hub.
Nguồn yêu cầu: Tài liệu xác định yêu cầu hệ thống (DOCX nội bộ).

## Kết luận

Sprint 1 chưa đáp ứng đầy đủ các User Story và tiêu chí nghiệm thu trong tài liệu. Các luồng tài khoản, phân vai trò, khóa tài khoản, cập nhật trường/khoa và kiểm tra khoa đúng trường đã được triển khai. Những phần còn thiếu chính là JWT, avatar dưới 2 MB và giao diện sửa tiểu sử, Follow, cùng chức năng admin thêm/sửa/xóa Trường/Khoa.

Có khác biệt cần thống nhất: tài liệu quy định mật khẩu tối thiểu 8 ký tự, còn code và giao diện đang đặt tối thiểu 12 ký tự. Không tự coi việc tăng ngưỡng là đã khớp nghiệm thu; cần chốt chính sách hoặc cập nhật AC. Tài liệu mô tả đăng ký bằng email, trong khi giao diện hiện yêu cầu cả username và email; đăng nhập hiện bằng username.

Việc có bảng/entity/repository chỉ là nền tảng dữ liệu, chưa đủ để đánh dấu một User Story hoàn thành. Kết quả test xanh cũng chỉ chứng minh các trường hợp test đã viết, không thay thế việc kiểm tra toàn bộ AC.

## Sprint 1 đối chiếu chi tiết

| User Story | Yêu cầu trong tài liệu | Hiện có | Phần còn thiếu hoặc khác AC | Đánh giá |
|---|---|---|---|---|
| US1.1 Đăng ký | Email, mật khẩu ít nhất 8 ký tự, báo trùng email, mặc định USER | Form/API đăng ký; kiểm tra email/username trùng; BCrypt; luôn USER; dropdown trường/khoa | Min password hiện là 12, nên mật khẩu 8–11 ký tự bị từ chối. Cần chốt username có bắt buộc không và bổ sung test email trùng độc lập với username | Gần hoàn thành, cần thống nhất AC |
| US1.2 Đăng nhập và xác thực | Đăng nhập trả JWT; tài khoản LOCKED bị từ chối | Spring Security, session cookie, CSRF, phân vai trò; khóa tài khoản thu hồi phiên và từ chối đăng nhập | Login trả JSON với data=null, không có JWT access token, ký/xác minh token hoặc Bearer authentication | Một phần |
| US1.3 Profile và Trường/Khoa | Cập nhật avatar, tiểu sử; chọn dropdown trường/khoa; avatar dưới 2 MB | Có dropdown và API cập nhật tổ chức; dữ liệu faculty/institution được kiểm tra. API profile có nhận bio/displayName | Chưa có upload avatar hoặc kiểm tra ảnh dưới 2 MB; UI chưa có sửa/hiển thị bio. UserResponse chưa trả bio/avatarUrl; avatar hiện là chữ viết tắt username | Một phần |
| US1.4 Follow | Theo dõi chuyên gia/giảng viên để xem bài mới | Có bảng user_follows, entity và repository | Chưa có service/controller Follow/Unfollow, UI theo dõi, danh sách theo dõi hoặc cách xem bài từ người đang theo dõi. User Story chưa có AC chi tiết | Chưa triển khai luồng |
| US1.5 Admin quản lý tổ chức | Admin thêm/sửa/xóa Trường và Khoa | Có schema, dữ liệu mẫu, GET institutions/faculties và dropdown | Chưa có API ghi ADMIN hay trang admin CRUD Trường/Khoa. Dữ liệu mẫu trong migration không thay thế chức năng quản trị | Một phần, mới có đọc danh mục |

### Minh chứng trong source

- [RegisterRequest.java](../backend/src/main/java/com/knowledge/app/model/dto/request/auth/RegisterRequest.java:6): @Size(min=12,max=72).
- [AuthServiceImpl.java](../backend/src/main/java/com/knowledge/app/service/impl/auth/AuthServiceImpl.java:17): đăng ký gán USER, xác thực tổ chức trước khi tạo tài khoản.
- [UserServiceImpl.java](../backend/src/main/java/com/knowledge/app/service/impl/user/UserServiceImpl.java:29): kiểm tra trùng username/email; các phương thức cập nhật và khóa tài khoản đã có.
- [SecurityConfig.java](../backend/src/main/java/com/knowledge/app/security/config/SecurityConfig.java:30): formLogin, logout hủy HttpSession/JSESSIONID; không phải JWT.
- [UpdateProfileRequest.java](../backend/src/main/java/com/knowledge/app/model/dto/request/user/UpdateProfileRequest.java:3): có bio, displayName và tổ chức; không có file avatar.
- [UserResponse.java](../backend/src/main/java/com/knowledge/app/model/dto/response/user/UserResponse.java:4): chưa có bio hoặc avatarUrl trong response.
- [AccountPage.tsx](../frontend/src/features/user/pages/AccountPage.tsx:19): form hiện gửi institutionId/facultyId; chưa có editor tiểu sử/ảnh.
- [UserFollowRepository.java](../backend/src/main/java/com/knowledge/app/repository/UserFollowRepository.java:6): chỉ có JpaRepository cơ bản; không có API/UI Follow trong danh sách controller/pages.
- [OrganizationController.java](../backend/src/main/java/com/knowledge/app/controller/organization/OrganizationController.java:13): chỉ có hai GET public.
- [AdminUserController.java](../backend/src/main/java/com/knowledge/app/controller/admin/AdminUserController.java:11): quản trị tài khoản đã có, nhưng không quản trị danh mục Trường/Khoa.

## Các hạng mục Sprint 1 đã làm tốt

- Quản lý tài khoản cơ bản: tìm kiếm/phân trang, tạo, sửa email/trạng thái/vai trò, reset mật khẩu.
- USER không được truy cập API quản trị; đăng ký không thể tự cấp ADMIN.
- Giữ ít nhất một admin ACTIVE; đổi mật khẩu hoặc khóa tài khoản làm phiên cũ hết hiệu lực.
- Chọn trường/khoa; chọn OTHER ẩn khoa và xem toàn cầu; khóa ngoại kép bảo vệ quan hệ khoa–trường.
- Thư viện mặc định theo tổ chức và upload điền sẵn trường/khoa đã có; đây cũng là phần hỗ trợ các Story của Sprint 2.

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

## Những việc cần chốt để nghiệm thu Sprint 1

1. Chốt chính sách mật khẩu min 8 hay min 12, và yêu cầu username/email. Nếu giữ code hiện tại, cập nhật AC bằng quyết định của nhóm.
2. Triển khai JWT như AC, hoặc chính thức đổi US1.2 sang session cookie; sau đó kiểm thử theo đúng cơ chế đã chọn.
3. Hoàn thiện Profile: API đọc dữ liệu đầy đủ, UI tiểu sử, upload/đổi avatar với giới hạn dưới 2 MB và kiểm tra file ảnh/ownership.
4. Hoàn thiện Follow/Unfollow và UI; bổ sung AC về tự follow, trùng follow, unfollow và cách xem người/bài đang theo dõi. Cần tách rõ phần Feed thuộc Sprint 3.
5. Thêm API và UI admin CRUD institutions/faculties; kiểm tra ADMIN, dữ liệu trùng, khoa thuộc trường và xóa khi đang được tham chiếu.
6. Bổ sung các test AC còn thiếu: email trùng với username khác, ngưỡng mật khẩu 8–11, JWT response/expiry, avatar, follow và CRUD tổ chức.

Các yêu cầu Report/Moderation trong tài liệu cần được thống nhất với quyết định tinh gọn gần đây. Không tự đánh dấu các Story này hoàn thành chỉ vì đã bỏ chúng khỏi schema.

## Kiểm chứng trong lần đánh giá này

- Đã đọc toàn bộ nội dung DOCX, gồm danh sách Sprint và 23 User Story; tài liệu gốc được giữ nguyên.
- Đã kiểm tra source backend/frontend, DTO, routes, migrations và các test hiện có. Không chỉnh sửa triển khai chức năng.
- Chạy lại AccountSecurityIntegrationTest (9) và OrganizationIntegrationTest (7): 16 test PASS trên profile test/H2, 0 failure, 0 error.
- npm.cmd run build: PASS.
- Không chạy lại MySQL hoặc browser E2E trong lượt đánh giá này. Các kết quả MySQL/E2E ở lượt triển khai trước không được dùng để coi JWT/avatar/follow/admin CRUD đã đạt AC.
- Log lượt kiểm tra: backend/sprint1-audit-tests.log; báo cáo test: backend/target/surefire-reports.

Kết luận nghiệm thu: Sprint 1 chưa đóng; nên hoàn thiện các khoảng trống trên hoặc chốt thay đổi yêu cầu trước khi đánh dấu Done.