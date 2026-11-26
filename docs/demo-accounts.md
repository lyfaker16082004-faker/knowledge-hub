# Tài khoản dữ liệu mẫu

Các tài khoản dưới đây được tạo bởi `scripts/seed_demo_data.py`, chỉ dùng để chạy thử trên máy local.

## Quản trị viên

| Tên đăng nhập | Mật khẩu | Vai trò |
|---|---|---|
| `quantri` | `12345678` | Quản trị viên (ADMIN) |

Tạo bằng `scripts/create-demo-admin.sql` (chạy lại sẽ đặt lại mật khẩu về `12345678`). Tài khoản `admin` cũ vẫn giữ nguyên.

```powershell
# Từ thư mục gốc, thay <mật khẩu MySQL> bằng mật khẩu root của bạn
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 knowledge_hub -e "source scripts/create-demo-admin.sql"
```

## Thành viên

**Mật khẩu chung:** `Hoctap@2026`
Đăng nhập bằng **tên đăng nhập** hoặc email `tên-đăng-nhập@knowledgehub.test`.

| Tên đăng nhập | Tên hiển thị | Trường / Khoa | Vai trò trong dữ liệu mẫu |
|---|---|---|---|
| `nguyenvanan` | Nguyễn Văn An | ĐH Bách Khoa Hà Nội · Khoa CNTT | Hay trả lời câu hỏi Java |
| `tranthibinh` | Trần Thị Bình | ĐH FPT · Khoa CNTT | Chuyên cơ sở dữ liệu |
| `lequangcuong` | Lê Quang Cường | ĐH FPT · Khoa CNTT | Frontend, React |
| `phamthidung` | Phạm Thị Dung | ĐH Bách Khoa Hà Nội · Khoa Kinh tế | Học Python phân tích dữ liệu |
| `hoangminhduc` | Hoàng Minh Đức | ĐH Bách Khoa Hà Nội · Khoa CNTT | Trợ giảng CTDL & GT |
| `vuthiha` | Vũ Thị Hà | ĐH FPT · Khoa CNTT | Sinh viên năm nhất, hay đặt câu hỏi |
| `dangvanhung` | Đặng Văn Hùng | Khác | Linux, mạng máy tính |
| `buithilan` | Bùi Thị Lan | ĐH Bách Khoa Hà Nội · Khoa CNTT | Học máy |
| `doquangminh` | Đỗ Quang Minh | Khác | Backend developer, nhiều người theo dõi |
| `ngothinga` | Ngô Thị Nga | ĐH FPT · Khoa Kinh tế | Thương mại điện tử |
| `lyvanphong` | Lý Văn Phong | ĐH Bách Khoa Hà Nội · Khoa CNTT | Thuật toán |
| `trinhthuytrang` | Trịnh Thùy Trang | ĐH FPT · Khoa CNTT | Kiểm thử phần mềm |

## Dữ liệu được tạo

- Các thành viên theo dõi lẫn nhau
- 14 bài viết (câu hỏi và bài chia sẻ) ở nhiều chủ đề, có thẻ
- Câu trả lời, trả lời lồng nhau, bình chọn Hữu ích / Không hữu ích, lượt thích và lượt lưu
- 29 tài liệu ở đủ các lĩnh vực: Java, CSDL, CTDL & GT, Python, Frontend, Mạng máy tính, An toàn thông tin,
  Trí tuệ nhân tạo, Hệ điều hành, Công nghệ phần mềm, Toán rời rạc, Điện toán đám mây, Thương mại điện tử,
  Kỹ năng học tập, Tiếng Anh chuyên ngành. 2 tài liệu có phí.
- Tài liệu của thành viên được `quantri` duyệt tự động (cần tạo tài khoản `quantri` trước khi chạy script)

## Chạy lại

```bash
python scripts/seed_demo_data.py
```

Backend phải đang chạy ở cổng 8080. Chạy lại nhiều lần không bị trùng: bài viết và tài liệu đã có sẽ được bỏ qua.
