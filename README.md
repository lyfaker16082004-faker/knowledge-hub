# Knowledge Hub

Ứng dụng chia sẻ tri thức và tài liệu học tập, xây dựng bằng **Spring Boot, React, TypeScript và MySQL**. Giao diện tiếng Việt, hỗ trợ desktop và mobile.

## Chức năng

- **Tài khoản:** đăng ký bằng email, đăng nhập bằng email hoặc username, xác thực JWT, đổi mật khẩu và phân vai trò.
- **Hồ sơ:** tên hiển thị, giới thiệu bản thân, ảnh đại diện, trường/tổ chức và khoa/chuyên ngành.
- **Thư viện:** tìm kiếm full-text, lọc theo môn học/trường/khoa/chủ đề/tags, upload tài liệu, tải file, lưu tài liệu và xem lịch sử đã tải.
- **Cộng đồng:** hồ sơ công khai, theo dõi/bỏ theo dõi, danh sách người theo dõi và bảng tin bài viết đã xuất bản.
- **Quản trị riêng với sidebar:** quản lý người dùng, xem chi tiết, phân quyền, đặt lại mật khẩu, xóa mềm CRUD trường/khoa và quản trị/duyệt kho tài liệu.

Sprint 1 và Sprint 2 đã hoàn thiện. Luồng tạo/sửa bài hỏi đáp, bình luận, bình chọn, thanh toán trực tuyến, xác minh/khôi phục mật khẩu qua email và mô hình AI chưa được triển khai. Tài liệu có phí hiện dùng quyền truy cập do quản trị viên cấp, chưa có cổng thanh toán.

## Công nghệ và yêu cầu

| Thành phần | Công nghệ / yêu cầu |
|---|---|
| Backend | Java 21+, Spring Boot 4, Spring Security, Spring Data JPA |
| Frontend | React 19, TypeScript, Vite; Node.js 22.12+ và npm |
| Database | MySQL 8.0.16+, Flyway migrations |
| Xác thực | JWT HS256, BCrypt, vai trò ADMIN / MODERATOR / USER |
| Kiểm thử | Kiểm thử thủ công trên giao diện (không dùng test tự động) |

Dự án có Maven Wrapper, không cần cài Maven riêng. Lần chạy đầu cần Internet để tải dependencies. Chạy local không cần Docker hoặc Redis.

## Cấu trúc repository

```text
knowledge-hub/
├── backend/                  # Spring Boot API và Maven Wrapper
│   ├── config/               # (không commit) cấu hình local: mật khẩu DB, khóa JWT
│   ├── storage/              # (không commit) dữ liệu: documents/, avatars/, backups/
│   ├── src/main/java/        # Controller, service, repository, entity, security
│   ├── src/main/resources/
│   │   └── db/migration/     # Flyway V1–V10
├── frontend/                 # React + TypeScript + Vite
│   └── src/                  # Giao diện người dùng và quản trị
├── docs/                     # API, kiến trúc, schema và nghiệm thu
├── scripts/                  # Khởi tạo admin, đặt lại tài khoản test
├── .env.example              # Danh sách biến môi trường tham khảo
└── README.md
```

## Cài đặt và chạy local

Các lệnh dưới đây bắt đầu từ thư mục gốc repository. Repository được clone vào thư mục `knowledge-hub` trong ví dụ dưới đây.

```sh
git clone "https://github.com/lyfaker16082004-faker/knowledge-hub.git" knowledge-hub
cd knowledge-hub
```

### 1. Tạo database MySQL

Khởi động MySQL, mở MySQL Workbench hoặc MySQL CLI và chạy:

```sql
CREATE DATABASE IF NOT EXISTS knowledge_hub
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Dùng tài khoản MySQL của bạn có quyền tạo/sửa bảng trong database này. Backend tự chạy Flyway để tạo schema và dữ liệu mẫu khi khởi động lần đầu; Hibernate kiểm tra schema bằng `ddl-auto=validate`.

### 2. Cấu hình và chạy backend

**Windows — PowerShell:**

```powershell
cd backend
$env:DB_USERNAME = "your_mysql_user"
$env:DB_PASSWORD = "your_mysql_password"
.\mvnw.cmd spring-boot:run
```

**macOS / Linux — Bash:**

```bash
cd backend
export DB_USERNAME="your_mysql_user"
export DB_PASSWORD="your_mysql_password"
# Chỉ cần nếu file wrapper chưa có quyền thực thi:
chmod +x mvnw
./mvnw spring-boot:run
```

Backend mặc định chạy tại `http://localhost:8080`, profile `dev`. Các biến phải được đặt trong terminal chạy backend. Nếu MySQL dùng host/port/database khác, đặt thêm `DB_URL`:

```text
jdbc:mysql://localhost:3306/knowledge_hub?serverTimezone=Asia/Bangkok&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci
```

`.env.example` chỉ là file tham khảo; **Spring Boot không tự đọc `.env`**. Có thể dùng file cấu hình local `backend/config/application-dev.properties` thay cho biến môi trường; thư mục `backend/config/` đã được `.gitignore` bỏ qua. Nếu file này tồn tại từ trước, kiểm tra các giá trị cấu hình trong đó khi đổi kết nối.

### 3. Chạy frontend

Mở terminal thứ hai tại thư mục gốc repository:

```sh
cd frontend
npm ci
npm run dev
```

Mở [http://localhost:5173](http://localhost:5173). Vite chuyển tiếp `/api` sang backend tại `http://localhost:8080`.

Trong Windows PowerShell, nếu chính sách thực thi chặn `npm.ps1`, dùng `npm.cmd` và `npx.cmd` thay cho `npm` và `npx`.

### 4. Khởi tạo tài khoản quản trị

Không có tài khoản admin/mật khẩu mặc định dùng chung. Đăng ký trên giao diện luôn tạo tài khoản **USER**.

Trên Windows, dừng backend đang chạy, giữ cấu hình MySQL trong terminal rồi chạy từ thư mục gốc:

```powershell
.\scripts\create-admin.ps1
```

Script hỏi username, email và mật khẩu bằng input ẩn, sau đó khởi động backend để tạo admin. Chỉ tạo khi chưa có ADMIN đang hoạt động; không ghi đè mật khẩu của admin hiện có. Sau khi tạo thành công, dừng bằng `Ctrl+C` và chạy backend bình thường theo bước 2.

Có thể bootstrap thủ công trên hệ điều hành khác bằng các biến sau khi chạy backend với profile `dev`:

| Biến | Giá trị |
|---|---|
| `APP_BOOTSTRAP_ENABLED` | `true` |
| `APP_ADMIN_USERNAME` | Username admin muốn tạo |
| `APP_ADMIN_EMAIL` | Email admin |
| `APP_ADMIN_PASSWORD` | Mật khẩu riêng của bạn |

Sau khi khởi tạo, bỏ các biến bootstrap khỏi môi trường chạy. Mật khẩu tài khoản tối thiểu 8 ký tự, tối đa 72 byte UTF-8.

## Các màn hình

Thêm các đường dẫn sau vào địa chỉ frontend, ví dụ `http://localhost:5173/#documents`:

| Đường dẫn | Chức năng |
|---|---|
| `/#home` | Trang chủ |
| `/#documents` | Thư viện tài liệu |
| `/#about` | Về chúng tôi |
| `/#register`, `/#login` | Đăng ký / Đăng nhập |
| `/#mine` | Tài liệu đã tải và đã lưu |
| `/#account` | Hồ sơ cá nhân và đổi mật khẩu |
| `/#upload` | Đóng góp tài liệu (thành viên gửi chờ duyệt) |
| `/#my-documents` | Tài liệu đã đăng, sửa và xóa của tác giả |
| `/#people` | Cộng đồng và theo dõi |
| `/#admin/documents` | Quản trị, duyệt/từ chối/ẩn tài liệu — ADMIN |
| `/#admin/documents/new` | Thêm giáo trình chuẩn — ADMIN |
| `/#admin/users` | Quản trị người dùng — ADMIN |
| `/#admin/organizations` | Quản trị trường/khoa — ADMIN |

Trang chủ, thư viện, giới thiệu và hồ sơ cộng đồng mở công khai. Tải/lưu tài liệu, theo dõi và thao tác cá nhân cần đăng nhập. Khu quản trị có sidebar riêng; quyền ADMIN được kiểm tra tại backend.

Xóa mềm chuyển tài khoản sang `DELETED`, giữ dữ liệu liên quan và thu hồi đăng nhập. Admin có thể khôi phục bằng cách sửa trạng thái thành Hoạt động. Hệ thống giữ ít nhất một ADMIN hoạt động và chặn xóa trường/khoa đang được sử dụng.

## Cấu hình và lưu trữ

| Biến | Mặc định / mục đích |
|---|---|
| `DB_URL` | MySQL local, database `knowledge_hub`; xem URL mẫu ở trên |
| `DB_USERNAME`, `DB_PASSWORD` | Thông tin tài khoản MySQL |
| `PORT` | `8080` — cổng backend |
| `APP_JWT_SECRET` | Khóa Base64 ít nhất 32 byte; bắt buộc với profile `prod` |
| `APP_STORAGE_DIR` | `backend/storage` — thư mục gốc chứa `documents/` (file upload riêng tư) và `avatars/` (ảnh đại diện) |
| `APP_DOCUMENTS_STORAGE_DIR`, `APP_AVATARS_STORAGE_DIR` | Tùy chọn: đặt riêng từng thư mục, ghi đè `APP_STORAGE_DIR` |
| `VITE_PROXY_TARGET` | `http://localhost:8080` — backend đích của proxy Vite khi phát triển |
| `VITE_API_BASE_URL` | `/api` — địa chỉ API dùng khi build frontend |

Backend luôn dùng thư mục `backend/` làm gốc, dù chạy từ `backend/` (mvnw, task VS Code) hay từ thư mục gốc repository (nút Run của IDE); đường dẫn tương đối trong các biến trên cũng tính từ `backend/`. Khi khởi động, log in ra đường dẫn thực tế (`Document files: …`). Thư mục được tự tạo nếu chưa có; dữ liệu ở vị trí cũ `backend/config/document-storage` và `avatar-storage` được tự chuyển sang `backend/storage/`. Avatar hỗ trợ PNG/JPEG nhỏ hơn 2 MiB. Tài liệu upload hỗ trợ PDF/PPT/PPTX/DOCX/MD/TXT/ZIP/RAR tối đa 20 MB.

JWT có thời hạn 24 giờ, frontend lưu trong `sessionStorage` của tab. Đổi mật khẩu, thay đổi tài khoản hoặc đăng xuất thu hồi token cũ. Trong `dev`, khóa JWT tự tạo tại `backend/config/jwt-secret.key`; giữ khóa này để token còn hiệu lực sau khởi động lại. Không đưa mật khẩu, khóa ký hoặc dữ liệu upload lên GitHub; cấu hình local nằm trong thư mục đã được bỏ qua.

## Database và tài liệu

Schema hiện có **26 bảng**, quản lý bằng **10 migration Flyway**. MySQL `utf8mb4` hỗ trợ tiếng Việt và emoji.

- [Schema và ERD](docs/database/schema.md)
- [SQL cấu trúc và dữ liệu mẫu](docs/database/knowledge_hub.sql)
- [Migration Flyway](backend/src/main/resources/db/migration)
- [Kiến trúc dự án](docs/architecture.md)
- [API tài khoản, hồ sơ và cộng đồng](docs/accounts.md)
- [Thư viện và quyền truy cập tài liệu](docs/library.md)
- [Trường/tổ chức và khoa](docs/organizations.md)
- [Báo cáo nghiệm thu Sprint 2](docs/sprint2-review.md)
- [Báo cáo nghiệm thu Sprint 1](docs/sprint1-review.md)

Ưu tiên khởi tạo bằng Flyway khi chạy ứng dụng. File SQL độc lập phục vụ tham khảo/minh chứng schema; không import rồi chạy migration trên cùng database mới nếu chưa thiết lập baseline phù hợp. Không sửa các migration đã áp dụng.

## Dữ liệu dùng chung cho team

Database và file upload **không nằm trên GitHub** (repo công khai, dữ liệu có thông tin tài khoản). Có 2 cách để có dữ liệu giống nhau:

**Cách 1 — Nạp gói dữ liệu (giống hệt máy người gửi).** Người có dữ liệu chạy:

```powershell
.\scripts\export-team-data.ps1
```

rồi gửi file `team-data/knowledge-hub-data-<ngày>.zip` qua Drive / Zalo. Người nhận **tắt backend**, chạy:

```powershell
.\scriptsestore-team-data.ps1 -Package "D:\Downloads\knowledge-hub-data-<ngày>.zip"
```

Script tự tạo database `knowledge_hub` nếu chưa có, sao lưu dữ liệu cũ vào `backend/storage/backups/`, nạp database và giải nén tài liệu / ảnh vào `backend/storage/`. Sau đó chạy backend như bình thường.

**Cách 2 — Tự tạo dữ liệu mẫu bằng script** (không có các tài liệu PDF đã upload tay):

```powershell
# 1. Tạo tài khoản quản trị quantri / 12345678
& "C:\Program Files\MySQL\MySQL Server 8.0in\mysql.exe" -uroot -p --default-character-set=utf8mb4 knowledge_hub -e "source scripts/create-demo-admin.sql"
# 2. Backend đang chạy, tạo tài khoản, bài viết, bình luận, tài liệu mẫu
python scripts/seed_demo_data.py
# 3. (Tuỳ chọn) Thêm tài liệu PPTX / DOCX / ZIP / PDF lấy từ internet
python scripts/import_web_documents.py --folder web-docs
```

Danh sách tài khoản để đăng nhập: [docs/demo-accounts.md](docs/demo-accounts.md).

## Kiểm thử

Dự án kiểm thử **thủ công** trên giao diện, không dùng test tự động (JUnit / Playwright đã được gỡ bỏ).
Trước khi nộp hoặc demo, nên đi qua các luồng chính:

1. **Tài khoản:** đăng ký, đăng nhập, đổi ảnh đại diện, sửa hồ sơ, đổi mật khẩu.
2. **Thư viện:** tìm kiếm + bộ lọc, mở chi tiết, xem thử (PDF, DOCX, PPTX, ZIP), lưu, tải về.
3. **Đóng góp tài liệu:** thành viên đăng tài liệu → admin duyệt / từ chối → tài liệu hiện trong thư viện.
4. **Cộng đồng:** đặt câu hỏi, trả lời lồng nhau, bình chọn, thích, lưu, theo dõi, chuông thông báo.
5. **Quản trị:** người dùng, trường / khoa, kho tài liệu, cộng đồng & hỏi đáp (ẩn / khôi phục).

Dữ liệu và tài khoản mẫu để thử: xem [docs/demo-accounts.md](docs/demo-accounts.md).

### Tài khoản test

Trên Windows, từ thư mục gốc:

```powershell
.\scripts\reset-test-users.ps1
```

Script sao lưu database local vào `backend/storage/backups/`. Sau đó nó xóa các tài khoản `e2e_*`, `codex_*`, `user1`, `user2`, `mod1` cùng tài liệu họ đã tải lên (cả dữ liệu lẫn file), rồi tạo lại ba tài khoản sau, mật khẩu chung `12345678`:

| Tài khoản | Vai trò |
|---|---|
| `user1`, `user2` | Thành viên |
| `mod1` | Thành viên + Kiểm duyệt viên |

Tài khoản `admin` và mọi tài khoản khác giữ nguyên. Toàn bộ thay đổi chạy trong một transaction; nếu lỗi, database không bị đổi. Chỉ dùng cho môi trường phát triển.

### Frontend

Từ thư mục `frontend`:

```sh
npm run build
```

Lệnh build chạy kiểm tra kiểu TypeScript (`tsc --noEmit`) trước khi đóng gói bằng Vite.

## Làm việc với VS Code

Mở thư mục gốc repository trong VS Code. Nên cài Extension Pack for Java và Spring Boot Extension Pack. Có thể chạy bằng hai terminal như hướng dẫn trên hoặc dùng **Terminal → Run Task → Backend: dev / Frontend: dev** trên Windows. Khi dùng task, cấu hình database qua file local hoặc môi trường mà VS Code kế thừa.

## Build và triển khai

Backend: chạy `mvnw package` bằng wrapper tương ứng hệ điều hành trong thư mục `backend`. Frontend: chạy `npm run build` trong `frontend`, kết quả tại `frontend/dist`.

Khi triển khai, chọn profile `prod`, cấu hình MySQL, cung cấp `APP_JWT_SECRET` riêng và dùng HTTPS. Phục vụ frontend cùng origin với API, cấu hình reverse proxy `/api` tới backend và fallback về `index.html`. Build frontend không tự được đóng gói vào JAR. Giữ bền vững thư mục file upload/avatar và khóa JWT qua các lần triển khai.

## Làm việc với IDE khác và Git

Mã nguồn không phụ thuộc VS Code. IntelliJ IDEA hoặc Eclipse có thể import `backend/pom.xml` dưới dạng Maven project, chọn JDK 21 trở lên và cấu hình các biến MySQL như hướng dẫn. Frontend mở tại thư mục `frontend`, dùng Node/npm để chạy. Thư mục `.vscode` chỉ cung cấp task tiện dụng, không bắt buộc.

`.editorconfig` thống nhất encoding và định dạng giữa các editor; `.gitattributes` giữ line ending phù hợp Windows/Linux, đặc biệt với Maven Wrapper. `package-lock.json` được commit để `npm ci` cài đúng phiên bản dependency. Các thư mục build, dependency tải về, file local và khóa ký được bỏ qua trong `.gitignore`.

Nên tạo nhánh riêng cho mỗi chức năng rồi mở pull request về `main`:

```sh
git switch -c feature/ten-chuc-nang
# Chỉnh sửa, chạy kiểm thử và chọn các file cần commit
git add <file-hoac-thu-muc>
git commit -m "feat: mo ta thay doi"
git push -u origin feature/ten-chuc-nang
```
