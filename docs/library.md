# Thư viện tài liệu — Sprint 2

## User Story và giao diện

| Story | Chức năng | Màn hình |
|---|---|---|
| US2.1 | Upload PDF/PPT/PPTX/DOCX/MD/TXT tối đa 20 MiB; danh mục bắt buộc, tối đa 5 tags, chọn môn học; tự gắn trường/khoa từ hồ sơ | #upload |
| US2.2 | Danh sách tài liệu đã đăng, xem chi tiết, sửa metadata và xóa mềm của chính tác giả | #my-documents |
| US2.3 | Xem chi tiết, kiểm tra quyền FREE/PAID, tải file thật; mỗi lần backend cấp file thành công ghi một document_downloads | #document/{id}, #mine |
| US2.4 | MySQL FULLTEXT trên title/summary/description, lọc môn học, trường/khoa, chủ đề, tag và FREE/PAID; sắp xếp và phân trang | #documents |
| US2.5 | Lưu/bỏ lưu idempotent; danh sách Đọc sau riêng cho từng tài khoản | #document/{id}, tab Đã lưu tại #mine |
| US2.6 | Admin xem PENDING, xem trước PDF/văn bản và trích nội dung Slide/Word; duyệt hoặc từ chối kèm lý do | #admin/documents |
| US2.7 | Admin thêm giáo trình chuẩn, sửa metadata, xóa mềm, ẩn và công bố lại tài liệu | #admin/documents, #admin/documents/new |

Trang của tôi có liên kết Tài liệu đã đăng. Admin dùng Kho tài liệu trong sidebar quản trị riêng.

## Trạng thái và quyền truy cập

Tài liệu mới của USER bắt đầu ở PENDING. Tài liệu do ADMIN thêm được công bố trực tiếp. Admin duyệt PENDING → PUBLISHED hoặc từ chối PENDING → REJECTED (bắt buộc lý do). Tác giả chỉnh sửa tài liệu của mình sẽ gửi lại PENDING và xóa kết quả duyệt cũ. Chỉnh sửa của admin giữ nguyên trạng thái.

Admin ẩn PUBLISHED → ARCHIVED; công bố lại ARCHIVED → PUBLISHED. Xóa mềm chuyển sang DELETED và không cho xem/tải công khai nữa. Lịch sử tải, file và bản ghi liên quan được giữ lại để bảo toàn dữ liệu. Danh sách bookmark chỉ hiện tài liệu đang PUBLISHED. Lịch sử tải vẫn hiện tài liệu đã ngừng công bố, với available=false.

Chỉ PUBLISHED có danh mục active xuất hiện trong tìm kiếm và chi tiết công khai. Chủ sở hữu/admin được xem chi tiết chưa công bố và bản xem trước riêng tư. Người dùng thường không được sửa/xóa tài liệu của người khác; admin được quản lý mọi tài liệu. Mỗi lần cập nhật/xóa/duyệt gửi version hiện tại; yêu cầu với version cũ trả 409, tránh ghi đè thay đổi của người khác.

Trường/khoa lúc người dùng upload lấy từ profile phía backend, không chỉ điền sẵn ở frontend. Nếu client gửi lựa chọn khác profile, API từ chối. Admin có thể chọn tổ chức cho giáo trình chuẩn. Khi tác giả sửa metadata, tổ chức ban đầu được giữ nguyên; việc đổi profile không tự đổi nhãn tài liệu cũ.

## API

Các API ghi/quản lý dùng JWT Bearer. Response JSON bọc ApiResponse; file trả binary, không lộ storage_key.

| Method | Endpoint | Chức năng |
|---|---|---|
| GET | /api/subjects | Danh mục môn học công khai |
| GET | /api/documents | q, category, subjectId, tag, access, institutionId, facultyId, global, sort, page, size≤30 |
| GET | /api/documents/categories | Chủ đề có tài liệu đã công bố và số lượng |
| GET | /api/documents/stats | Thống kê tài liệu đã công bố và lượt tải |
| POST | /api/documents | Multipart metadata JSON + file; trả DocumentSummary |
| GET | /api/documents/{id} | Chi tiết; owner/admin có thêm status, reviewNote, version, canManage/canPreview |
| POST | /api/documents/{id}/files/{fileId}/download | Tải tài liệu đã công bố và ghi lịch sử |
| GET | /api/documents/{id}/files/{fileId}/preview | Owner/admin; file riêng tư phục vụ kiểm duyệt; không ghi lịch sử tải |
| GET | /api/documents/{id}/files/{fileId}/preview-text | Owner/admin; trích văn bản PPTX/DOCX, không ghi lượt tải |
| GET | /api/users/me/documents | Danh sách tài liệu chính mình: q, status, page, size |
| PUT | /api/users/me/documents/{id} | JSON version, metadata; kiểm tra tác giả, gửi duyệt lại |
| DELETE | /api/users/me/documents/{id}?version=... | Tác giả xóa mềm |
| GET | /api/admin/documents | ADMIN; danh sách toàn kho với q/status/page/size |
| PUT | /api/admin/documents/{id} | ADMIN sửa metadata bất kỳ tài liệu |
| DELETE | /api/admin/documents/{id}?version=... | ADMIN xóa mềm |
| POST | /api/admin/documents/{id}/review | ADMIN; JSON version, action, note; action APPROVE/REJECT/HIDE/RESTORE |
| POST | /api/admin/documents/{id}/access | ADMIN; JSON userId; cấp quyền thủ công tài liệu có phí |
| GET | /api/users/me/library/stats | Thống kê tải/lưu của chính mình |
| GET | /api/users/me/downloads | Lịch sử tải riêng theo user, phân trang |
| GET | /api/users/me/bookmarks | Tài liệu đã lưu còn công bố, phân trang |
| POST | /api/users/me/bookmarks/{id} | JSON saved=true/false; idempotent |

Metadata upload/sửa:

```json
{
  "title": "Đề ôn tập Java",
  "summary": "Tóm tắt nội dung",
  "description": "Nội dung chi tiết và nguồn tài liệu",
  "categoryId": 1,
  "subjectId": 1,
  "tags": ["Java", "Ôn tập"],
  "level": "BEGINNER",
  "accessType": "FREE",
  "price": 0
}
```

ID chủ đề/môn học lấy từ API; không giả định ID mẫu luôn bằng 1. subjectId/tags có thể bỏ trống. FREE cần price=0; PAID cần price>0. Tài liệu PAID chỉ tải được khi là tác giả hoặc có document_access còn hiệu lực; thanh toán trực tuyến chưa triển khai.

## Tìm kiếm và lưu trữ

V10 thêm subjects, document_tags, subject_id/review_note/reviewed_at/reviewed_by_id và chỉ mục ft_documents_content. Tổng 26 bảng nghiệp vụ, 10 migration. V1–V9 giữ nguyên checksum. MySQL dùng MATCH AGAINST theo natural language; kết hợp LIKE đã escape cho từ ngắn/cụm tiếng Việt và tìm tên tag. H2 dùng LIKE để kiểm thử; có kiểm thử MySQL riêng chứng minh truy vấn full-text thật.

File lưu ở backend/config/document-storage, tên UUID và đường dẫn private. Kiểm tra đuôi + nội dung: PDF signature, PPT compound document bằng Apache POI, PPTX/DOCX cấu trúc ZIP và giới hạn giải nén, MD/TXT UTF-8. Không chấp nhận macro Office đã nhận diện; giới hạn upload 20 MiB và tổng giải nén 100 MiB. Rollback sẽ dọn file mới. API tải kiểm tra file thuộc đúng document, tài khoản và quyền truy cập trước khi cấp bytes.

PDF xem trong iframe; TXT/MD hiển thị văn bản; PPTX/DOCX có bản trích văn bản để kiểm duyệt. Bản trích không tái hiện đầy đủ định dạng/hình ảnh, admin có thể tải bản gốc để xem bằng PowerPoint/Word hoặc phần mềm tương thích. PPT cũ dùng tải bản xem trước. Xem trước không được tính là lượt tải phục vụ AI. Bộ đọc XML tắt DTD/thực thể ngoài và giới hạn nội dung trích.

Mỗi lần backend đọc/cấp file download thành công mới ghi lịch sử, không khẳng định người dùng đã lưu xong vào ổ đĩa. Lỗi quyền truy cập, sai file hoặc file thiếu không tạo bản ghi.

Dữ liệu mẫu khởi đầu gồm 7 tài liệu Markdown của Knowledge Hub và 4 môn học. E2E tạo dữ liệu có tiền tố riêng; scripts/cleanup-e2e.ps1 dọn đúng manifest và không xóa tài khoản thật. Không bổ sung lại reports, moderation_actions hay audit_logs; ghi chú duyệt nằm trực tiếp trên documents.
