# Tài khoản và xác thực Sprint 1

Đăng ký bằng email, mật khẩu 8–72 ký tự và tối đa 72 byte UTF-8. Username tùy chọn; nếu bỏ trống sẽ sinh user_<12 ký tự>. Email/username chuẩn hóa chữ thường, không được trùng. Tài khoản mới luôn USER kể cả khi client gửi roles. BCrypt cost 12.

## API

Response bọc ApiResponse; không lộ passwordHash/authVersion.

| Method | Endpoint | Nội dung / quyền |
|---|---|---|
| POST | /api/auth/register | Public; JSON email, password, username?, institutionId?, facultyId? |
| POST | /api/auth/login | Public; JSON username (email hoặc username), password; trả accessToken, tokenType=Bearer, expiresIn=86400, user |
| POST | /api/auth/logout | Bearer; tăng auth_version, thu hồi tất cả token hiện tại của tài khoản |
| GET | /api/users/me | Bearer; trả hồ sơ đầy đủ, organization và roles |
| PUT | /api/users/me/profile | Bearer; displayName (≤100), bio (≤2000), institutionId, facultyId; gửi cả lựa chọn tổ chức hiện tại, null để xóa lựa chọn |
| POST | /api/users/me/avatar | Bearer; multipart file PNG/JPEG, nhỏ hơn 2 MiB |
| GET | /api/avatars/{key} | Public; PNG đã xử lý, UUID, cache 7 ngày |
| POST | /api/users/me/password | Bearer; currentPassword, newPassword; thu hồi token cũ |
| GET | /api/admin/users?q=&page=0&size=10 | ADMIN; tìm kiếm, phân trang |
| GET | /api/admin/users/{id} | ADMIN; chi tiết hồ sơ đầy đủ |
| DELETE | /api/admin/users/{id} | ADMIN; xóa mềm (status=DELETED), giữ dữ liệu, thu hồi token; lặp lại an toàn |
| POST | /api/admin/users | ADMIN; username, email, password, roles[] |
| PUT | /api/admin/users/{id} | ADMIN; email, status, roles[] |
| POST | /api/admin/users/{id}/password | ADMIN; đặt lại mật khẩu |

JWT ký HS256 bằng Spring Security Nimbus JOSE, kiểm chữ ký, exp/nbf, issuer knowledge-hub và audience knowledge-hub-web. Mỗi request đọc lại trạng thái ACTIVE, auth_version và roles từ DB. Khóa tài khoản, thay đổi tài khoản/mật khẩu hoặc logout làm token cũ hết hiệu lực. Giữ ít nhất một ADMIN ACTIVE với khóa hàng ở service. Form login cũ (form-urlencoded) vẫn được hỗ trợ, trả cùng JSON JWT.

Backend stateless, không chấp nhận cookie làm chứng thực; CSRF tắt cho API Bearer. Frontend gửi token qua Authorization, lưu sessionStorage riêng từng tab; đăng nhập/register không gửi token cũ. Token không nằm trong URL. Cần HTTPS khi triển khai. Dev giữ khóa trong config/jwt-secret.key (ignored); prod bắt buộc đặt APP_JWT_SECRET Base64 tối thiểu 32 byte. Không commit khóa.

Avatar được kiểm tra nội dung ImageIO, chỉ PNG/JPEG, chiều mỗi cạnh tối đa 4096 và tối đa 16 triệu pixel, chuyển PNG tối đa 512px và loại metadata. File lưu config/avatar-storage ngoài static; thay ảnh dọn ảnh cũ sau commit; rollback dọn ảnh mới. API chỉ cập nhật chính người đang đăng nhập.

## Cộng đồng và theo dõi

| Method | Endpoint | Quyền / chức năng |
|---|---|---|
| GET | /api/community/users?q=&page=0&size=12 | Public; thành viên ACTIVE, tìm username/tên hiển thị |
| GET | /api/community/users/{id} | Public; hồ sơ không có email/trạng thái/vai trò bí mật |
| POST | /api/community/users/{id}/follow | Bearer; JSON following=true/false |
| GET | /api/community/following | Bearer; danh sách đang theo dõi |
| GET | /api/community/followers | Bearer; danh sách người theo dõi |
| GET | /api/community/feed | Bearer; bài PUBLISHED PUBLIC/FOLLOWERS của người đang theo dõi |
| GET | /api/community/users/{id}/posts | Public chỉ thấy PUBLIC; follower được thấy thêm FOLLOWERS |

Các danh sách hỗ trợ page≥0, size 1–50. Không tự follow; tài khoản không ACTIVE không hiện. Follow/unfollow lặp tuần tự không tạo bản ghi trùng. Feed xếp publishedAt mới nhất, không lộ DRAFT/PRIVATE. UI tại #people và #person/{id}; nếu chưa có bài thì hiện trạng thái trống. Luồng viết/sửa hỏi đáp, bình luận và vote vẫn thuộc Sprint 3.

Email verification, reset qua email, refresh token, MFA và kiểm duyệt nội dung chưa thuộc Sprint 1. Không bổ sung lại các bảng đã bỏ theo yêu cầu tinh gọn.

Khu quản trị có layout riêng tại #admin/users và #admin/organizations, sidebar riêng, không dùng header/footer trang người dùng. Chi tiết đọc API riêng, xóa mềm có hộp thoại xác nhận; tài khoản Đã xóa mềm vẫn hiện trong danh sách quản trị và có thể khôi phục qua Sửa → trạng thái Hoạt động. Chặn xóa mềm admin ACTIVE cuối cùng. Người dùng thường không được xem chi tiết quản trị hoặc gọi DELETE.
