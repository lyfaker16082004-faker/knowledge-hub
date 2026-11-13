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
| Backend | Java 17+, Spring Boot 4, Spring Security, Spring Data JPA |
| Frontend | React 19, TypeScript, Vite; Node.js 22.12+ và npm |
| Database | MySQL 8.0.16+, Flyway migrations |
| Xác thực | JWT HS256, BCrypt, vai trò ADMIN / MODERATOR / USER |
| Kiểm thử | JUnit, Spring Boot Test, H2, MySQL integration test, Playwright |

Dự án có Maven Wrapper, không cần cài Maven riêng. Lần chạy đầu cần Internet để tải dependencies. Chạy local không cần Docker hoặc Redis.

## Cấu trúc repository

```text
knowledge-hub/
├── backend/                  # Spring Boot API và Maven Wrapper
│   ├── src/main/java/        # Controller, service, repository, entity, security
│   ├── src/main/resources/
│   │   └── db/migration/     # Flyway V1–V10
│   └── src/test/             # Kiểm thử backend
├── frontend/                 # React + TypeScript + Vite
│   ├── src/                  # Giao diện người dùng và quản trị
│   └── e2e/                  # Kiểm thử Playwright
├── docs/                     # API, kiến trúc, schema và nghiệm thu
├── scripts/                  # Khởi tạo admin và dọn dữ liệu thử
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
| `APP_DOCUMENTS_STORAGE_DIR` | `./config/document-storage` — file upload riêng tư |
| `APP_AVATARS_STORAGE_DIR` | `./config/avatar-storage` — ảnh đại diện đã xử lý |
| `VITE_PROXY_TARGET` | `http://localhost:8080` — backend đích của proxy Vite khi phát triển |
| `VITE_API_BASE_URL` | `/api` — địa chỉ API dùng khi build frontend |

Đường dẫn lưu trữ mặc định được tính từ thư mục chạy backend. Avatar hỗ trợ PNG/JPEG nhỏ hơn 2 MiB. Tài liệu upload hỗ trợ PDF/PPT/PPTX/DOCX/MD/TXT tối đa 20 MB.

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

## Kiểm thử

### Backend

Từ thư mục `backend`:

```powershell
# Windows: kiểm thử mặc định bằng H2
.\mvnw.cmd test

# Tùy chọn: kiểm thử thêm trên MySQL
.\mvnw.cmd "-Dmysql.it=true" test
```

Trên macOS/Linux, thay `.\mvnw.cmd` bằng `./mvnw`. Test mặc định không cần MySQL. Test MySQL yêu cầu kết nối đã cấu hình; test tài khoản/nghiệp vụ dùng database `knowledge_hub_account_test` riêng và cần quyền tạo database. Test schema sử dụng database `dev` đang cấu hình, các thay đổi thử được rollback; nên dùng môi trường phát triển dành riêng cho kiểm thử.

### Frontend và trình duyệt

Từ thư mục `frontend`:

```sh
npm run build
npx playwright install chromium
npm run test:e2e
```

E2E cần backend/frontend đang chạy và tài khoản ADMIN thử nghiệm. Các test hiện đọc file local `backend/config/initial-admin.json` dạng `{"username":"...","password":"..."}`; tự tạo file này với tài khoản của môi trường kiểm thử, không commit. Có thể để nội dung `{}` và đặt `E2E_ADMIN_USERNAME` / `E2E_ADMIN_PASSWORD` trong terminal chạy test. Script bootstrap không tự sinh file thông tin này.

`E2E_BASE_URL` mặc định là `http://127.0.0.1:5173`. Test tạo tài khoản `e2e_*` với email `@example.test`; trên Windows có thể dọn các fixture theo manifest bằng `scripts/cleanup-e2e.ps1` từ thư mục gốc. Script dọn hiện kết nối MySQL local và đọc `backend/config/application-dev.properties`; kiểm tra cấu hình trước khi dùng với môi trường khác.

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
