"""
Tạo dữ liệu mẫu cho Knowledge Hub bằng cách gọi API thật của backend.

Script sẽ:
  1. Đăng ký 12 tài khoản tên tiếng Việt (đã có thì chỉ đăng nhập)
  2. Cập nhật hồ sơ: tên hiển thị, giới thiệu, trường / khoa
  3. Cho các thành viên theo dõi nhau
  4. Đăng câu hỏi và bài chia sẻ trên Bảng tin cộng đồng
  5. Viết câu trả lời, trả lời lồng nhau, bình chọn, thích và lưu bài
  6. Đóng góp khoảng 30 tài liệu ở nhiều lĩnh vực
  7. Quản trị viên (quantri) duyệt để tài liệu hiện trong Thư viện

Chạy lại nhiều lần không bị trùng: bài viết / tài liệu đã có (cùng tiêu đề) sẽ được bỏ qua.

Cách chạy (backend phải đang chạy ở cổng 8080):
    python scripts/seed_demo_data.py
    python scripts/seed_demo_data.py --api http://localhost:8080/api

Danh sách tài khoản và mật khẩu: docs/demo-accounts.md
"""
import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid

# Mật khẩu chung của các tài khoản mẫu (chỉ dùng cho môi trường chạy thử)
DEMO_PASSWORD = 'Hoctap@2026'
EMAIL_DOMAIN = 'knowledgehub.test'

# =========================================================
# PHẦN 1: DỮ LIỆU MẪU
# =========================================================

# (tên đăng nhập, tên hiển thị, giới thiệu, tên trường, tên khoa)
USERS = [
    ('nguyenvanan', 'Nguyễn Văn An', 'Sinh viên năm 3 ngành Kỹ thuật phần mềm. Thích Java và Spring Boot.',
     'Đại học Bách Khoa Hà Nội', 'Khoa CNTT'),
    ('tranthibinh', 'Trần Thị Bình', 'Mê cơ sở dữ liệu, đang ôn thi chứng chỉ SQL.',
     'Đại học FPT', 'Khoa CNTT'),
    ('lequangcuong', 'Lê Quang Cường', 'Frontend developer tập sự, hay viết React và TypeScript.',
     'Đại học FPT', 'Khoa CNTT'),
    ('phamthidung', 'Phạm Thị Dung', 'Sinh viên kinh tế đang học thêm phân tích dữ liệu với Python.',
     'Đại học Bách Khoa Hà Nội', 'Khoa Kinh tế'),
    ('hoangminhduc', 'Hoàng Minh Đức', 'Trợ giảng môn Cấu trúc dữ liệu và giải thuật.',
     'Đại học Bách Khoa Hà Nội', 'Khoa CNTT'),
    ('vuthiha', 'Vũ Thị Hà', 'Năm nhất, đang tập làm quen với lập trình. Hỏi hơi nhiều, mong mọi người thông cảm.',
     'Đại học FPT', 'Khoa CNTT'),
    ('dangvanhung', 'Đặng Văn Hùng', 'Quản trị hệ thống Linux, thích mạng máy tính và bảo mật.',
     'Khác', None),
    ('buithilan', 'Bùi Thị Lan', 'Đang làm đồ án tốt nghiệp về học máy.',
     'Đại học Bách Khoa Hà Nội', 'Khoa CNTT'),
    ('doquangminh', 'Đỗ Quang Minh', 'Backend developer 2 năm kinh nghiệm, chia sẻ những gì mình học được.',
     'Khác', None),
    ('ngothinga', 'Ngô Thị Nga', 'Sinh viên thương mại điện tử, quan tâm tới marketing số.',
     'Đại học FPT', 'Khoa Kinh tế'),
    ('lyvanphong', 'Lý Văn Phong', 'Thích toán rời rạc và các bài thuật toán thi đấu.',
     'Đại học Bách Khoa Hà Nội', 'Khoa CNTT'),
    ('trinhthuytrang', 'Trịnh Thùy Trang', 'Tester, đang học kiểm thử tự động.',
     'Đại học FPT', 'Khoa CNTT'),
]

# Ai theo dõi ai: (người theo dõi, danh sách người được theo dõi)
FOLLOWS = [
    ('nguyenvanan', ['doquangminh', 'hoangminhduc', 'tranthibinh']),
    ('tranthibinh', ['nguyenvanan', 'doquangminh']),
    ('lequangcuong', ['doquangminh', 'nguyenvanan', 'trinhthuytrang']),
    ('phamthidung', ['buithilan', 'tranthibinh']),
    ('hoangminhduc', ['lyvanphong', 'nguyenvanan']),
    ('vuthiha', ['nguyenvanan', 'hoangminhduc', 'doquangminh', 'lequangcuong', 'tranthibinh']),
    ('dangvanhung', ['doquangminh']),
    ('buithilan', ['phamthidung', 'hoangminhduc']),
    ('doquangminh', ['nguyenvanan', 'dangvanhung']),
    ('ngothinga', ['phamthidung', 'lequangcuong']),
    ('lyvanphong', ['hoangminhduc']),
    ('trinhthuytrang', ['lequangcuong', 'doquangminh']),
]

# Bài viết: tác giả, loại, chủ đề, tiêu đề, nội dung, thẻ, các câu trả lời
# Mỗi câu trả lời: (người viết, nội dung, [các phản hồi (người viết, nội dung)])
POSTS = [
    {
        'author': 'vuthiha', 'type': 'QUESTION', 'category': 'Lập trình Java',
        'title': 'Vì sao so sánh hai String bằng == lại ra false?',
        'content': 'Mình viết đoạn code sau:\n\n```java\nString a = new String("hello");\nString b = "hello";\n'
                   'System.out.println(a == b);\n```\n\nKết quả in ra `false` dù hai chuỗi giống nhau. '
                   'Mình nên so sánh chuỗi thế nào cho đúng ạ?',
        'tags': ['java', 'string', 'người mới'],
        'answers': [
            ('nguyenvanan', '`==` so sánh **địa chỉ** của hai đối tượng, còn `equals()` mới so sánh **nội dung**. '
                            'Bạn dùng `a.equals(b)` là ra `true` nhé.',
             [('vuthiha', 'Cảm ơn bạn, mình thử lại được rồi!'),
              ('hoangminhduc', 'Bổ sung: nếu `a` có thể null thì dùng `Objects.equals(a, b)` cho an toàn.')]),
            ('doquangminh', 'Chuỗi viết trực tiếp như `"hello"` được Java gom vào String pool, '
                            'còn `new String(...)` luôn tạo đối tượng mới nên địa chỉ khác nhau.', []),
        ],
        'likes': ['nguyenvanan', 'tranthibinh', 'lequangcuong'],
        'saves': ['vuthiha', 'lyvanphong'],
    },
    {
        'author': 'doquangminh', 'type': 'ARTICLE', 'category': 'Lập trình Java',
        'title': '5 lỗi hay gặp khi mới làm REST API với Spring Boot',
        'content': 'Tổng hợp từ kinh nghiệm review code cho các bạn thực tập:\n\n'
                   '1. **Trả về entity trực tiếp** thay vì DTO -> lộ mật khẩu, vòng lặp JSON.\n'
                   '2. **Không validate** dữ liệu đầu vào, thiếu `@Valid`.\n'
                   '3. **Bắt Exception chung chung** rồi trả 200 kèm thông báo lỗi.\n'
                   '4. **Quên phân trang** khi trả danh sách lớn.\n'
                   '5. **Để logic trong controller** thay vì service.\n\n'
                   'Mọi người có lỗi nào hay gặp nữa thì bổ sung bên dưới nhé.',
        'tags': ['spring boot', 'rest api', 'kinh nghiệm'],
        'answers': [
            ('nguyenvanan', 'Em hay quên thêm `@Transactional` khi sửa nhiều bảng cùng lúc, dữ liệu bị lưu dở dang.',
             [('doquangminh', 'Đúng rồi, cái này cũng rất hay gặp. Cảm ơn em đã bổ sung.')]),
            ('lequangcuong', 'Bên frontend thì khổ nhất là API trả lỗi không thống nhất format, mỗi chỗ một kiểu.', []),
        ],
        'likes': ['nguyenvanan', 'tranthibinh', 'lequangcuong', 'trinhthuytrang', 'dangvanhung'],
        'saves': ['nguyenvanan', 'vuthiha', 'lequangcuong'],
    },
    {
        'author': 'tranthibinh', 'type': 'QUESTION', 'category': 'Cơ sở dữ liệu',
        'title': 'Lưu tiếng Việt vào MySQL bị lỗi dấu hỏi chấm, sửa thế nào?',
        'content': 'Mình tạo bảng rồi insert chữ "Nguyễn Văn An" nhưng khi SELECT ra lại thành "Nguy?n V?n An". '
                   'Mình dùng MySQL 8, kết nối từ Spring Boot. Có phải do charset không ạ?',
        'tags': ['mysql', 'utf8mb4', 'tiếng việt'],
        'answers': [
            ('doquangminh', 'Bạn kiểm tra 3 chỗ:\n1. Database và bảng dùng `utf8mb4` + `utf8mb4_unicode_ci`\n'
                            '2. Chuỗi kết nối có `characterEncoding=UTF-8`\n3. File SQL được lưu ở UTF-8',
             [('tranthibinh', 'Đúng là bảng của mình đang để latin1, đổi sang utf8mb4 là hết lỗi. Cảm ơn anh!')]),
        ],
        'likes': ['phamthidung', 'nguyenvanan'],
        'saves': ['tranthibinh', 'phamthidung', 'ngothinga'],
    },
    {
        'author': 'hoangminhduc', 'type': 'ARTICLE', 'category': 'Cấu trúc dữ liệu & giải thuật',
        'title': 'Cách nhớ nhanh độ phức tạp của các thuật toán sắp xếp',
        'content': 'Mẹo nhỏ cho các bạn ôn thi giữa kỳ:\n\n'
                   '- **Bubble, Selection, Insertion**: O(n²) — đều là "so sánh từng cặp".\n'
                   '- **Merge sort**: luôn O(n log n), tốn thêm O(n) bộ nhớ.\n'
                   '- **Quick sort**: trung bình O(n log n), tệ nhất O(n²) khi chọn chốt kém.\n'
                   '- **Heap sort**: O(n log n), không tốn thêm bộ nhớ.\n\n'
                   'Nhớ "chia đôi thì có log n" là được một nửa rồi.',
        'tags': ['sắp xếp', 'độ phức tạp', 'ôn thi'],
        'answers': [
            ('lyvanphong', 'Anh có thể nói thêm vì sao Quick sort thực tế lại hay nhanh hơn Merge sort không ạ?',
             [('hoangminhduc', 'Do Quick sort sắp xếp tại chỗ, truy cập bộ nhớ liên tục nên tận dụng cache tốt hơn.'),
              ('lyvanphong', 'À ra vậy, em hiểu rồi ạ.')]),
            ('vuthiha', 'Bài này dễ hiểu quá, em lưu lại để ôn thi.', []),
        ],
        'likes': ['lyvanphong', 'vuthiha', 'nguyenvanan', 'buithilan'],
        'saves': ['vuthiha', 'lyvanphong', 'nguyenvanan'],
    },
    {
        'author': 'lyvanphong', 'type': 'QUESTION', 'category': 'Cấu trúc dữ liệu & giải thuật',
        'title': 'Khi nào nên dùng BFS thay vì DFS khi duyệt đồ thị?',
        'content': 'Hai cách duyệt này đều đi hết các đỉnh, vậy khi làm bài thì dựa vào đâu để chọn ạ? '
                   'Ví dụ bài tìm đường đi ngắn nhất trong mê cung thì nên dùng cách nào?',
        'tags': ['đồ thị', 'bfs', 'dfs'],
        'answers': [
            ('hoangminhduc', 'Đồ thị **không có trọng số** mà cần đường đi ngắn nhất thì dùng **BFS**, '
                             'vì BFS đi theo từng lớp nên gặp đích lần đầu chính là ngắn nhất. '
                             'DFS hợp với các bài cần duyệt hết, tìm thành phần liên thông, phát hiện chu trình.',
             [('lyvanphong', 'Vậy mê cung là BFS rồi. Cảm ơn anh!')]),
        ],
        'likes': ['hoangminhduc', 'nguyenvanan'],
        'saves': ['lyvanphong'],
    },
    {
        'author': 'phamthidung', 'type': 'QUESTION', 'category': 'Lập trình Python',
        'title': 'Đọc file Excel bằng pandas bị lỗi font tiếng Việt',
        'content': 'Mình dùng `pd.read_csv("baocao.csv")` thì cột tên khách hàng bị lỗi chữ. '
                   'File được xuất từ Excel. Mình cần thêm tham số gì không ạ?',
        'tags': ['python', 'pandas', 'excel'],
        'answers': [
            ('buithilan', 'File CSV xuất từ Excel thường là `utf-8-sig`. Bạn thử '
                          '`pd.read_csv("baocao.csv", encoding="utf-8-sig")` nhé.',
             [('phamthidung', 'Được rồi bạn ơi, cảm ơn nhiều!')]),
        ],
        'likes': ['buithilan', 'ngothinga'],
        'saves': ['phamthidung'],
    },
    {
        'author': 'buithilan', 'type': 'ARTICLE', 'category': 'Trí tuệ nhân tạo',
        'title': 'Lộ trình tự học Machine Learning cho sinh viên năm 3',
        'content': 'Mình chia sẻ lộ trình mình đã đi trong 6 tháng:\n\n'
                   '1. Ôn lại **đại số tuyến tính** và **xác suất thống kê** (1 tháng)\n'
                   '2. Học **Python, numpy, pandas** (1 tháng)\n'
                   '3. Các thuật toán cơ bản: hồi quy tuyến tính, cây quyết định, k-means (2 tháng)\n'
                   '4. Làm 2 dự án nhỏ trên dữ liệu thật (2 tháng)\n\n'
                   'Quan trọng nhất là làm dự án, đọc lý thuyết mãi sẽ rất nhanh quên.',
        'tags': ['machine learning', 'lộ trình', 'tự học'],
        'answers': [
            ('phamthidung', 'Chị ơi, người học kinh tế như em có theo lộ trình này được không ạ?',
             [('buithilan', 'Được em, em có lợi thế hiểu dữ liệu kinh doanh. Cứ bắt đầu từ pandas là ổn.')]),
        ],
        'likes': ['phamthidung', 'hoangminhduc', 'nguyenvanan', 'lyvanphong'],
        'saves': ['phamthidung', 'nguyenvanan'],
    },
    {
        'author': 'lequangcuong', 'type': 'QUESTION', 'category': 'Frontend',
        'title': 'useEffect bị gọi hai lần khi chạy React ở chế độ dev?',
        'content': 'Mình để `console.log` trong `useEffect(() => {...}, [])` thì thấy in ra 2 lần, '
                   'API cũng bị gọi 2 lần. Có phải code mình sai không ạ?',
        'tags': ['react', 'useeffect'],
        'answers': [
            ('doquangminh', 'Không sai đâu, đó là do `React.StrictMode` cố tình chạy effect 2 lần ở chế độ dev '
                            'để phát hiện effect thiếu hàm dọn dẹp. Khi build production sẽ chỉ chạy 1 lần.',
             [('lequangcuong', 'À vậy là mình nên thêm AbortController để huỷ request cũ đúng không anh?'),
              ('doquangminh', 'Chuẩn rồi, trả về `() => controller.abort()` trong effect là đẹp.')]),
        ],
        'likes': ['trinhthuytrang', 'ngothinga'],
        'saves': ['lequangcuong', 'trinhthuytrang'],
    },
    {
        'author': 'dangvanhung', 'type': 'ARTICLE', 'category': 'Mạng máy tính',
        'title': 'Giải thích dễ hiểu: điều gì xảy ra khi bạn gõ một địa chỉ web?',
        'content': 'Tóm tắt 5 bước:\n\n1. **DNS** đổi tên miền thành địa chỉ IP\n'
                   '2. Trình duyệt mở **kết nối TCP** (bắt tay 3 bước)\n'
                   '3. Nếu là HTTPS thì thêm bước **bắt tay TLS** để mã hoá\n'
                   '4. Gửi **HTTP request**, máy chủ trả về HTML\n'
                   '5. Trình duyệt tải thêm CSS, JS, ảnh rồi **vẽ trang**\n\n'
                   'Câu này rất hay gặp khi phỏng vấn thực tập.',
        'tags': ['dns', 'tcp', 'phỏng vấn'],
        'answers': [
            ('trinhthuytrang', 'Bài hay quá anh, em sắp đi phỏng vấn nên đúng lúc luôn.', []),
        ],
        'likes': ['trinhthuytrang', 'doquangminh', 'nguyenvanan'],
        'saves': ['trinhthuytrang', 'vuthiha'],
    },
    {
        'author': 'trinhthuytrang', 'type': 'QUESTION', 'category': 'Công nghệ phần mềm',
        'title': 'Kiểm thử đơn vị và kiểm thử tích hợp khác nhau thế nào?',
        'content': 'Mình đang viết test cho đồ án Spring Boot. Thầy yêu cầu có cả unit test và integration test '
                   'nhưng mình chưa phân biệt rõ hai loại này. Mọi người giải thích giúp mình với.',
        'tags': ['testing', 'junit', 'đồ án'],
        'answers': [
            ('nguyenvanan', '**Unit test** kiểm tra một hàm / một class riêng lẻ, các phần phụ thuộc được giả lập (mock). '
                            '**Integration test** chạy nhiều phần thật cùng nhau, ví dụ gọi API -> service -> database.',
             [('trinhthuytrang', 'Vậy dùng `@SpringBootTest` với MockMvc là integration test đúng không?'),
              ('nguyenvanan', 'Đúng rồi bạn.')]),
        ],
        'likes': ['nguyenvanan'],
        'saves': ['trinhthuytrang'],
    },
    {
        'author': 'ngothinga', 'type': 'QUESTION', 'category': 'Thương mại điện tử',
        'title': 'Nên chọn Shopify hay WooCommerce cho shop nhỏ mới mở?',
        'content': 'Mình muốn mở một shop bán đồ handmade, ngân sách ít và không biết nhiều về lập trình. '
                   'Nên dùng nền tảng nào cho dễ quản lý ạ?',
        'tags': ['shopify', 'woocommerce', 'kinh doanh'],
        'answers': [],
        'likes': ['phamthidung'],
        'saves': [],
    },
    {
        'author': 'nguyenvanan', 'type': 'QUESTION', 'category': 'Hệ điều hành',
        'title': 'Lệnh nào để xem cổng 8080 đang bị chương trình nào chiếm?',
        'content': 'Mình chạy Spring Boot thì báo lỗi "Port 8080 was already in use". '
                   'Làm sao biết chương trình nào đang dùng cổng này trên Windows và Linux ạ?',
        'tags': ['linux', 'windows', 'port'],
        'answers': [
            ('dangvanhung', 'Linux: `sudo lsof -i :8080` hoặc `ss -ltnp | grep 8080`.\n'
                            'Windows: `netstat -ano | findstr :8080` rồi xem cột PID, mở Task Manager để tắt.',
             [('nguyenvanan', 'Tìm ra rồi, là một bản backend cũ mình quên tắt. Cảm ơn anh!')]),
        ],
        'likes': ['vuthiha', 'lequangcuong'],
        'saves': ['nguyenvanan', 'vuthiha'],
    },
    {
        'author': 'vuthiha', 'type': 'QUESTION', 'category': 'Kỹ năng học tập',
        'title': 'Làm sao để không bị nản khi mới học lập trình?',
        'content': 'Em học năm nhất, nhiều bài tập làm mãi không ra nên rất nản. '
                   'Các anh chị có cách nào để duy trì động lực không ạ?',
        'tags': ['động lực', 'người mới', 'tự học'],
        'answers': [
            ('doquangminh', 'Chia nhỏ mục tiêu: mỗi ngày giải 1 bài, làm xong thì ghi lại. '
                            'Sau 1 tháng nhìn lại sẽ thấy mình tiến bộ nhiều.', []),
            ('hoangminhduc', 'Đừng ngại hỏi. Bí quá 30 phút thì lên đây hỏi, mọi người sẵn sàng giúp.',
             [('vuthiha', 'Dạ em cảm ơn anh chị nhiều ạ!')]),
        ],
        'likes': ['nguyenvanan', 'tranthibinh', 'buithilan', 'doquangminh', 'lyvanphong', 'ngothinga'],
        'saves': ['vuthiha'],
    },
    {
        'author': 'tranthibinh', 'type': 'ARTICLE', 'category': 'Cơ sở dữ liệu',
        'title': 'Ghi chú nhanh: khi nào cần tạo index trong MySQL',
        'content': 'Nên tạo index cho:\n\n- Cột hay dùng trong `WHERE`, `JOIN`, `ORDER BY`\n'
                   '- Khoá ngoại\n- Cột cần tìm kiếm toàn văn (FULLTEXT)\n\n'
                   'Không nên tạo index cho:\n\n- Bảng rất nhỏ\n- Cột ít giá trị khác nhau (ví dụ giới tính)\n'
                   '- Cột thay đổi liên tục\n\nDùng `EXPLAIN` trước câu SELECT để xem có dùng index hay không.',
        'tags': ['mysql', 'index', 'hiệu năng'],
        'answers': [
            ('doquangminh', 'Thêm một ý: index nhiều cột thì thứ tự cột quan trọng, cột lọc nhiều nhất nên đứng đầu.',
             [('tranthibinh', 'Cảm ơn anh, em bổ sung vào ghi chú.')]),
        ],
        'likes': ['doquangminh', 'nguyenvanan', 'phamthidung'],
        'saves': ['nguyenvanan', 'lequangcuong'],
    },
]

# Bình chọn cho câu trả lời: (người bình chọn, tác giả câu trả lời, đoạn đầu nội dung, +1 hoặc -1)
VOTES = [
    ('vuthiha', 'nguyenvanan', '`==` so sánh', 1),
    ('lyvanphong', 'nguyenvanan', '`==` so sánh', 1),
    ('tranthibinh', 'doquangminh', 'Chuỗi viết trực tiếp', 1),
    ('tranthibinh', 'doquangminh', 'Bạn kiểm tra 3 chỗ', 1),
    ('phamthidung', 'doquangminh', 'Bạn kiểm tra 3 chỗ', 1),
    ('lyvanphong', 'hoangminhduc', 'Đồ thị **không có trọng số**', 1),
    ('nguyenvanan', 'hoangminhduc', 'Đồ thị **không có trọng số**', 1),
    ('phamthidung', 'buithilan', 'File CSV xuất từ Excel', 1),
    ('lequangcuong', 'doquangminh', 'Không sai đâu', 1),
    ('trinhthuytrang', 'doquangminh', 'Không sai đâu', 1),
    ('nguyenvanan', 'dangvanhung', 'Linux: `sudo lsof', 1),
    ('trinhthuytrang', 'nguyenvanan', '**Unit test**', 1),
    ('vuthiha', 'hoangminhduc', 'Đừng ngại hỏi', 1),
    ('vuthiha', 'doquangminh', 'Chia nhỏ mục tiêu', 1),
    ('ngothinga', 'lequangcuong', 'Bên frontend thì khổ', -1),
]

# Tài liệu đóng góp (sẽ ở trạng thái Chờ duyệt): tác giả, tên file, tiêu đề, tóm tắt, chủ đề, mức độ, thẻ, nội dung
DOCUMENTS = [
    ('nguyenvanan', 'de-cuong-oop-java.md', 'Đề cương ôn tập Lập trình hướng đối tượng với Java',
     'Tóm tắt 4 tính chất OOP, interface, abstract class và các câu hỏi thường gặp khi thi cuối kỳ.',
     'Lập trình Java', 'BEGINNER', ['java', 'oop', 'ôn thi'],
     '# Đề cương ôn tập OOP với Java\n\n## 1. Bốn tính chất\n- Đóng gói\n- Kế thừa\n- Đa hình\n- Trừu tượng\n\n'
     '## 2. Interface và abstract class\nInterface chỉ khai báo hành vi, abstract class có thể có sẵn code.\n\n'
     '## 3. Câu hỏi ôn tập\n1. Phân biệt overloading và overriding.\n2. Khi nào dùng interface?\n'),
    ('tranthibinh', 'bai-tap-sql-co-loi-giai.md', '30 bài tập SQL có lời giải cho người mới',
     'Các bài tập SELECT, JOIN, GROUP BY trên cơ sở dữ liệu bán hàng, có đáp án chi tiết.',
     'Cơ sở dữ liệu', 'BEGINNER', ['sql', 'bài tập', 'mysql'],
     '# 30 bài tập SQL\n\n## Bài 1\nLiệt kê tất cả khách hàng ở Hà Nội.\n\n```sql\nSELECT * FROM customers WHERE city = '
     "'Hà Nội';\n```\n\n## Bài 2\nĐếm số đơn hàng của mỗi khách hàng.\n\n```sql\nSELECT customer_id, COUNT(*)\n"
     'FROM orders GROUP BY customer_id;\n```\n'),
    ('hoangminhduc', 'tom-tat-cau-truc-du-lieu.md', 'Tóm tắt Cấu trúc dữ liệu và giải thuật trong 10 trang',
     'Mảng, danh sách liên kết, ngăn xếp, hàng đợi, cây, đồ thị và các thuật toán sắp xếp, tìm kiếm cơ bản.',
     'Cấu trúc dữ liệu & giải thuật', 'INTERMEDIATE', ['ctdl', 'giải thuật', 'tóm tắt'],
     '# Tóm tắt CTDL & GT\n\n## Ngăn xếp (Stack)\nVào sau ra trước (LIFO).\n\n## Hàng đợi (Queue)\n'
     'Vào trước ra trước (FIFO).\n\n## Cây nhị phân tìm kiếm\nTrái nhỏ hơn gốc, phải lớn hơn gốc. Tìm kiếm O(log n) '
     'khi cây cân bằng.\n'),
    ('buithilan', 'ghi-chu-pandas.txt', 'Ghi chú pandas cho người mới phân tích dữ liệu',
     'Các lệnh pandas hay dùng nhất: đọc file, lọc dòng, nhóm dữ liệu và vẽ biểu đồ nhanh.',
     'Lập trình Python', 'BEGINNER', ['python', 'pandas', 'phân tích dữ liệu'],
     'GHI CHÚ PANDAS\n\n1. Đọc file: pd.read_csv("file.csv", encoding="utf-8-sig")\n'
     '2. Xem nhanh: df.head(), df.info(), df.describe()\n3. Lọc dòng: df[df["tuoi"] > 18]\n'
     '4. Nhóm: df.groupby("thanh_pho")["doanh_thu"].sum()\n5. Vẽ biểu đồ: df["doanh_thu"].plot(kind="bar")\n'),
]


# Thêm tài liệu ở nhiều lĩnh vực (cùng dạng với DOCUMENTS ở trên).
# Tác giả 'quantri' là quản trị viên -> tài liệu được công bố ngay như "giáo trình chuẩn".
DOCUMENTS += [
    ('quantri', 'giao-trinh-java-co-ban.md', 'Giáo trình Lập trình Java cơ bản (chuẩn khoa CNTT)',
     'Giáo trình 8 chương: cú pháp, mảng, chuỗi, lớp và đối tượng, kế thừa, ngoại lệ, collection, đọc ghi file.',
     'Lập trình Java', 'BEGINNER', ['java', 'giáo trình', 'cơ bản'],
     '# Giáo trình Lập trình Java cơ bản\n\n## Chương 1. Làm quen với Java\n- Cài JDK, chạy chương trình Hello World\n'
     '- Biến, kiểu dữ liệu nguyên thuỷ\n\n## Chương 2. Mảng và chuỗi\n- Mảng một chiều, hai chiều\n'
     '- String, StringBuilder\n\n## Chương 3. Lớp và đối tượng\n- Thuộc tính, phương thức, hàm khởi tạo\n\n'
     '## Chương 4. Kế thừa và đa hình\n\n## Chương 5. Xử lý ngoại lệ\n- try / catch / finally\n\n'
     '## Chương 6. Collection\n- ArrayList, HashMap, HashSet\n\n## Chương 7. Đọc ghi file\n\n## Chương 8. Bài tập tổng hợp\n'),
    ('doquangminh', 'spring-boot-rest-api-tung-buoc.md', 'Xây dựng REST API với Spring Boot từng bước',
     'Hướng dẫn tạo dự án, viết controller, service, repository, kết nối MySQL và kiểm thử API bằng Postman.',
     'Lập trình Java', 'INTERMEDIATE', ['spring boot', 'rest api', 'mysql'],
     '# REST API với Spring Boot\n\n## Bước 1. Tạo dự án\nDùng start.spring.io, chọn Web, JPA, MySQL Driver.\n\n'
     '## Bước 2. Tạo entity\n```java\n@Entity\npublic class Book {\n    @Id @GeneratedValue\n    private Long id;\n'
     '    private String title;\n}\n```\n\n## Bước 3. Repository và Service\n\n## Bước 4. Controller\n'
     '```java\n@GetMapping("/api/books")\npublic List<Book> list() { return service.findAll(); }\n```\n\n'
     '## Bước 5. Kiểm thử bằng Postman\n'),
    ('tranthibinh', 'thiet-ke-csdl-quan-ly-thu-vien.md', 'Thiết kế cơ sở dữ liệu quản lý thư viện (có sơ đồ ERD)',
     'Bài mẫu phân tích yêu cầu, vẽ ERD, chuẩn hoá 3NF và viết câu lệnh tạo bảng cho hệ thống thư viện.',
     'Cơ sở dữ liệu', 'INTERMEDIATE', ['erd', 'chuẩn hoá', 'đồ án'],
     '# Thiết kế CSDL quản lý thư viện\n\n## 1. Thực thể\n- Sách, Tác giả, Độc giả, Phiếu mượn\n\n'
     '## 2. Quan hệ\n- Một độc giả có nhiều phiếu mượn\n- Một sách có nhiều tác giả (nhiều - nhiều)\n\n'
     '## 3. Chuẩn hoá\nĐưa về 3NF: tách bảng sach_tac_gia cho quan hệ nhiều - nhiều.\n\n'
     '## 4. Tạo bảng\n```sql\nCREATE TABLE sach (\n  id INT PRIMARY KEY AUTO_INCREMENT,\n  ten VARCHAR(255) NOT NULL\n);\n```\n'),
    ('lyvanphong', 'quy-hoach-dong-co-ban.md', 'Quy hoạch động cơ bản qua 10 bài toán kinh điển',
     'Fibonacci, cái túi, dãy con tăng dài nhất, xâu con chung dài nhất... mỗi bài có ý tưởng và code mẫu.',
     'Cấu trúc dữ liệu & giải thuật', 'ADVANCED', ['quy hoạch động', 'thuật toán', 'thi đấu'],
     '# Quy hoạch động cơ bản\n\n## Ý tưởng\nChia bài toán lớn thành bài toán con, lưu kết quả để không tính lại.\n\n'
     '## Bài 1. Fibonacci\n```python\nf = [0, 1]\nfor i in range(2, n + 1):\n    f.append(f[i-1] + f[i-2])\n```\n\n'
     '## Bài 2. Cái túi 0/1\ndp[i][w] = max(dp[i-1][w], dp[i-1][w - c[i]] + v[i])\n\n'
     '## Bài 3. Dãy con tăng dài nhất (LIS)\n'),
    ('phamthidung', 'python-cho-nguoi-lam-kinh-te.md', 'Python cho người làm kinh tế: tự động hoá báo cáo Excel',
     'Dùng pandas và openpyxl để gộp nhiều file Excel, tính tổng doanh thu và xuất báo cáo tự động.',
     'Lập trình Python', 'BEGINNER', ['python', 'excel', 'tự động hoá'],
     '# Tự động hoá báo cáo Excel bằng Python\n\n## 1. Gộp nhiều file\n```python\nimport glob, pandas as pd\n'
     'files = glob.glob("bao_cao/*.xlsx")\ndf = pd.concat(pd.read_excel(f) for f in files)\n```\n\n'
     '## 2. Tính tổng theo tháng\n```python\ndf.groupby("thang")["doanh_thu"].sum()\n```\n\n'
     '## 3. Xuất file\n```python\ndf.to_excel("tong_hop.xlsx", index=False)\n```\n'),
    ('lequangcuong', 'react-hooks-thuc-hanh.md', 'Thực hành React Hooks: useState, useEffect, useMemo',
     'Giải thích từng hook qua ví dụ nhỏ: bộ đếm, gọi API, lọc danh sách, và các lỗi hay gặp.',
     'Frontend', 'INTERMEDIATE', ['react', 'hooks', 'typescript'],
     '# Thực hành React Hooks\n\n## useState\n```tsx\nconst [count, setCount] = useState(0);\n```\n\n'
     '## useEffect\nChạy sau khi vẽ giao diện. Nhớ trả về hàm dọn dẹp khi gọi API.\n\n'
     '## useMemo\nGhi nhớ kết quả tính toán nặng, chỉ tính lại khi phụ thuộc thay đổi.\n\n'
     '## Lỗi hay gặp\n- Quên mảng phụ thuộc -> effect chạy mãi\n- Sửa trực tiếp state -> giao diện không cập nhật\n'),
    ('lequangcuong', 'html-css-flexbox-grid.md', 'Dàn trang với Flexbox và CSS Grid cho người mới',
     'So sánh Flexbox và Grid, kèm 6 mẫu bố cục hay dùng: thanh menu, thẻ sản phẩm, trang quản trị...',
     'Frontend', 'BEGINNER', ['css', 'flexbox', 'grid'],
     '# Flexbox và CSS Grid\n\n## Khi nào dùng Flexbox?\nSắp xếp theo **một chiều** (một hàng hoặc một cột).\n\n'
     '## Khi nào dùng Grid?\nBố cục **hai chiều** (hàng và cột cùng lúc).\n\n## Mẫu: lưới thẻ sản phẩm\n'
     '```css\n.grid {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));\n  gap: 16px;\n}\n```\n'),
    ('hoangminhduc', 'ky-nang-ghi-chu-cornell.md', 'Phương pháp ghi chép Cornell giúp nhớ bài lâu hơn',
     'Cách chia trang vở thành 3 phần, cách ôn tập sau buổi học và mẫu ghi chép áp dụng cho môn chuyên ngành.',
     'Kỹ năng học tập', 'BEGINNER', ['ghi chép', 'kỹ năng', 'ôn thi'],
     '# Phương pháp Cornell\n\n## Chia trang\n- Cột trái (1/3): từ khoá, câu hỏi\n- Cột phải (2/3): ghi bài\n'
     '- Phần dưới: tóm tắt 2-3 câu\n\n## Sau buổi học\n1. Đọc lại trong 24 giờ\n2. Che cột phải, tự trả lời câu hỏi bên trái\n\n'
     '## Mẹo\nViết bằng lời của mình, không chép nguyên văn slide.\n'),
    ('vuthiha', 'ke-hoach-hoc-tap-nam-nhat.txt', 'Kế hoạch học tập năm nhất ngành CNTT',
     'Lịch học theo tuần, danh sách môn cần ưu tiên và các kỹ năng nên rèn từ năm nhất.',
     'Kỹ năng học tập', 'BEGINNER', ['năm nhất', 'kế hoạch', 'cntt'],
     'KẾ HOẠCH HỌC TẬP NĂM NHẤT\n\n1. Ưu tiên: Nhập môn lập trình, Toán rời rạc, Tiếng Anh\n'
     '2. Mỗi tuần: 5 bài tập lập trình, đọc 1 chương sách\n3. Kỹ năng: dùng Git, viết báo cáo, làm việc nhóm\n'
     '4. Cuối mỗi tháng: tự đánh giá và điều chỉnh kế hoạch\n'),
    ('dangvanhung', 'mo-hinh-osi-tcpip.md', 'Mô hình OSI và TCP/IP: tóm tắt để ôn thi Mạng máy tính',
     'Chức năng 7 tầng OSI, 4 tầng TCP/IP, các giao thức tiêu biểu ở mỗi tầng và câu hỏi trắc nghiệm.',
     'Mạng máy tính', 'BEGINNER', ['osi', 'tcp/ip', 'ôn thi'],
     '# Mô hình OSI và TCP/IP\n\n| Tầng | Tên | Giao thức |\n|---|---|---|\n| 7 | Ứng dụng | HTTP, DNS |\n'
     '| 4 | Giao vận | TCP, UDP |\n| 3 | Mạng | IP, ICMP |\n| 2 | Liên kết dữ liệu | Ethernet |\n\n'
     '## Mẹo nhớ\n"All People Seem To Need Data Processing" (từ tầng 7 xuống tầng 1).\n'),
    ('dangvanhung', 'chia-subnet-de-hieu.md', 'Chia mạng con (subnetting) dễ hiểu kèm 15 bài tập',
     'Cách tính địa chỉ mạng, broadcast, số host theo CIDR và bài tập có lời giải.',
     'Mạng máy tính', 'INTERMEDIATE', ['subnet', 'ip', 'bài tập'],
     '# Chia mạng con\n\n## Ví dụ\n192.168.1.0/26\n- Số host mỗi mạng: 2^6 - 2 = 62\n- Các mạng con: .0, .64, .128, .192\n\n'
     '## Bài tập 1\nChia 172.16.0.0/16 thành 8 mạng con bằng nhau.\n'),
    ('dangvanhung', 'bao-mat-web-owasp.md', 'Top 10 lỗ hổng bảo mật web (OWASP) cho lập trình viên',
     'Giải thích SQL Injection, XSS, CSRF, lộ dữ liệu nhạy cảm... và cách phòng tránh trong code.',
     'An toàn thông tin', 'INTERMEDIATE', ['owasp', 'xss', 'sql injection'],
     '# OWASP Top 10\n\n## SQL Injection\nKhông nối chuỗi vào câu SQL, luôn dùng tham số (PreparedStatement).\n\n'
     '## XSS\nKhông chèn HTML người dùng nhập vào trang khi chưa lọc.\n\n## CSRF\nDùng token chống giả mạo yêu cầu.\n\n'
     '## Lưu mật khẩu\nBăm bằng BCrypt, không bao giờ lưu mật khẩu gốc.\n'),
    ('buithilan', 'hoi-quy-tuyen-tinh.md', 'Hồi quy tuyến tính từ con số 0 với Python',
     'Ý tưởng, công thức, cài đặt bằng numpy và so sánh kết quả với scikit-learn trên dữ liệu giá nhà.',
     'Trí tuệ nhân tạo', 'INTERMEDIATE', ['machine learning', 'hồi quy', 'numpy'],
     '# Hồi quy tuyến tính\n\n## Ý tưởng\nTìm đường thẳng y = ax + b gần các điểm dữ liệu nhất.\n\n'
     '## Hàm mất mát\nTrung bình bình phương sai số (MSE).\n\n## Cài đặt\n```python\nimport numpy as np\n'
     'a, b = np.polyfit(x, y, 1)\n```\n\n## Với scikit-learn\n```python\nfrom sklearn.linear_model import LinearRegression\n'
     'model = LinearRegression().fit(X, y)\n```\n'),
    ('buithilan', 'gioi-thieu-mang-no-ron.md', 'Giới thiệu mạng nơ-ron và học sâu cho sinh viên',
     'Nơ-ron nhân tạo, hàm kích hoạt, lan truyền ngược và ví dụ nhận dạng chữ số viết tay MNIST.',
     'Trí tuệ nhân tạo', 'ADVANCED', ['deep learning', 'mạng nơ-ron', 'mnist'],
     '# Mạng nơ-ron\n\n## Một nơ-ron\nĐầu ra = hàm kích hoạt(tổng có trọng số + độ lệch)\n\n'
     '## Hàm kích hoạt\n- ReLU, Sigmoid, Softmax\n\n## Lan truyền ngược\nTính đạo hàm của hàm mất mát theo từng trọng số rồi cập nhật.\n\n'
     '## Bài thực hành\nNhận dạng chữ số MNIST với 2 lớp ẩn, độ chính xác khoảng 97%.\n'),
    ('dangvanhung', 'lenh-linux-hay-dung.txt', '50 lệnh Linux hay dùng nhất cho sinh viên',
     'Các lệnh quản lý file, phân quyền, tiến trình, mạng và nén file, có ví dụ đi kèm.',
     'Hệ điều hành', 'BEGINNER', ['linux', 'terminal', 'lệnh'],
     '50 LỆNH LINUX HAY DÙNG\n\nFile: ls, cd, pwd, cp, mv, rm, mkdir, cat, less\n'
     'Phân quyền: chmod 755 file, chown user file\nTiến trình: ps aux, top, kill -9 PID\n'
     'Mạng: ping, curl, ss -ltnp\nNén: tar -czf a.tar.gz thu_muc\n'),
    ('lyvanphong', 'lap-lich-cpu.md', 'Các thuật toán lập lịch CPU: FCFS, SJF, Round Robin',
     'Cách tính thời gian chờ, thời gian hoàn thành và vẽ biểu đồ Gantt cho từng thuật toán.',
     'Hệ điều hành', 'INTERMEDIATE', ['lập lịch', 'cpu', 'hệ điều hành'],
     '# Lập lịch CPU\n\n## FCFS\nAi đến trước chạy trước. Dễ cài nhưng tiến trình ngắn phải chờ lâu.\n\n'
     '## SJF\nTiến trình ngắn nhất chạy trước, thời gian chờ trung bình nhỏ nhất.\n\n'
     '## Round Robin\nMỗi tiến trình chạy một lượng thời gian (quantum) rồi nhường lượt.\n'),
    ('trinhthuytrang', 'viet-test-case-chuan.md', 'Cách viết test case chuẩn cho người mới làm tester',
     'Cấu trúc một test case, kỹ thuật phân vùng tương đương, giá trị biên và mẫu file Excel.',
     'Công nghệ phần mềm', 'BEGINNER', ['testing', 'test case', 'qa'],
     '# Viết test case\n\n## Cấu trúc\n| Mã | Mô tả | Bước | Dữ liệu | Kết quả mong đợi |\n|---|---|---|---|---|\n'
     '| TC01 | Đăng nhập đúng | Nhập email, mật khẩu | an@x.vn / 12345678 | Vào trang chủ |\n\n'
     '## Phân vùng tương đương\nChia dữ liệu thành nhóm hợp lệ / không hợp lệ, mỗi nhóm thử một giá trị.\n\n'
     '## Giá trị biên\nMật khẩu 8 ký tự: thử 7, 8, 9 ký tự.\n'),
    ('nguyenvanan', 'scrum-cho-do-an-nhom.md', 'Áp dụng Scrum cho đồ án nhóm sinh viên',
     'Chia sprint, viết user story, họp daily ngắn và dùng bảng Trello để theo dõi tiến độ.',
     'Công nghệ phần mềm', 'BEGINNER', ['scrum', 'agile', 'làm việc nhóm'],
     '# Scrum cho đồ án nhóm\n\n## Sprint\nMỗi sprint 2 tuần, cuối sprint có sản phẩm chạy được.\n\n'
     '## User story\n"Là sinh viên, tôi muốn tải tài liệu để ôn thi."\n\n## Daily\n15 phút: hôm qua làm gì, hôm nay làm gì, vướng gì.\n'),
    ('lyvanphong', 'toan-roi-rac-logic-menh-de.md', 'Toán rời rạc: Logic mệnh đề và bảng chân trị',
     'Các phép toán logic, bảng chân trị, tương đương logic và bài tập chứng minh có lời giải.',
     'Toán rời rạc', 'BEGINNER', ['logic', 'mệnh đề', 'toán rời rạc'],
     '# Logic mệnh đề\n\n## Phép toán\n- Phủ định ¬p\n- Hội p ∧ q\n- Tuyển p ∨ q\n- Kéo theo p → q\n\n'
     '## Bảng chân trị của p → q\n| p | q | p → q |\n|---|---|---|\n| Đ | Đ | Đ |\n| Đ | S | S |\n| S | Đ | Đ |\n| S | S | Đ |\n'),
    ('lyvanphong', 'do-thi-va-cay.md', 'Toán rời rạc: Đồ thị, cây và các bài toán đường đi',
     'Định nghĩa đồ thị, bậc của đỉnh, đường đi Euler, Hamilton, cây khung nhỏ nhất.',
     'Toán rời rạc', 'INTERMEDIATE', ['đồ thị', 'cây', 'euler'],
     '# Đồ thị và cây\n\n## Định lý bắt tay\nTổng bậc các đỉnh = 2 x số cạnh.\n\n'
     '## Đường đi Euler\nĐi qua mỗi cạnh đúng một lần. Có khi số đỉnh bậc lẻ là 0 hoặc 2.\n\n'
     '## Cây khung nhỏ nhất\nThuật toán Kruskal và Prim.\n'),
    ('doquangminh', 'docker-cho-nguoi-moi.md', 'Docker cho người mới: đóng gói ứng dụng Spring Boot',
     'Khái niệm image, container, viết Dockerfile và chạy ứng dụng kèm MySQL bằng docker compose.',
     'Điện toán đám mây', 'INTERMEDIATE', ['docker', 'container', 'devops'],
     '# Docker cho người mới\n\n## Image và container\nImage là bản mẫu, container là bản đang chạy.\n\n'
     '## Dockerfile\n```dockerfile\nFROM eclipse-temurin:21-jre\nCOPY target/app.jar app.jar\n'
     'ENTRYPOINT ["java", "-jar", "app.jar"]\n```\n\n## Chạy\n```bash\ndocker build -t my-app .\n'
     'docker run -p 8080:8080 my-app\n```\n'),
    ('doquangminh', 'tong-quan-dien-toan-dam-may.md', 'Tổng quan điện toán đám mây: IaaS, PaaS, SaaS',
     'Phân biệt các mô hình dịch vụ đám mây, ví dụ thực tế và cách chọn nhà cung cấp cho dự án nhỏ.',
     'Điện toán đám mây', 'BEGINNER', ['cloud', 'iaas', 'saas'],
     '# Điện toán đám mây\n\n- **IaaS**: thuê máy chủ ảo (ví dụ máy ảo EC2)\n- **PaaS**: chỉ cần đưa code lên, nền tảng lo phần chạy\n'
     '- **SaaS**: dùng phần mềm qua trình duyệt (Gmail, Google Docs)\n\n## Ưu điểm\nTrả tiền theo mức dùng, mở rộng nhanh.\n'),
    ('ngothinga', 'marketing-so-cho-shop-nho.md', 'Marketing số cho shop online nhỏ: từ 0 đến đơn hàng đầu tiên',
     'Xác định khách hàng mục tiêu, chụp ảnh sản phẩm, viết nội dung và chạy quảng cáo với ngân sách thấp.',
     'Thương mại điện tử', 'BEGINNER', ['marketing', 'bán hàng online', 'quảng cáo'],
     '# Marketing số cho shop nhỏ\n\n## 1. Khách hàng mục tiêu\nTuổi, sở thích, họ hay dùng mạng xã hội nào?\n\n'
     '## 2. Ảnh sản phẩm\nÁnh sáng tự nhiên, nền đơn giản.\n\n## 3. Nội dung\nKể câu chuyện về sản phẩm thay vì chỉ ghi giá.\n\n'
     '## 4. Quảng cáo\nBắt đầu với 50.000đ/ngày, theo dõi tỉ lệ nhấp rồi điều chỉnh.\n'),
    ('ngothinga', 'mo-hinh-kinh-doanh-tmdt.md', 'Các mô hình kinh doanh thương mại điện tử B2B, B2C, C2C',
     'Đặc điểm, ví dụ doanh nghiệp tiêu biểu tại Việt Nam và ưu nhược điểm của từng mô hình.',
     'Thương mại điện tử', 'INTERMEDIATE', ['b2c', 'b2b', 'kinh doanh'],
     '# Mô hình thương mại điện tử\n\n- **B2C**: doanh nghiệp bán cho người tiêu dùng\n- **B2B**: doanh nghiệp bán cho doanh nghiệp\n'
     '- **C2C**: người dùng bán cho nhau trên sàn\n\n## Câu hỏi thảo luận\nShop handmade nhỏ nên bắt đầu từ mô hình nào?\n'),
    ('vuthiha', 'tieng-anh-chuyen-nganh-cntt.txt', '300 từ vựng tiếng Anh chuyên ngành CNTT thường gặp',
     'Từ vựng chia theo chủ đề: lập trình, mạng, cơ sở dữ liệu, bảo mật, kèm nghĩa tiếng Việt và ví dụ.',
     'Khác', 'BEGINNER', ['tiếng anh', 'từ vựng', 'cntt'],
     'TỪ VỰNG TIẾNG ANH CNTT\n\nLập trình: variable (biến), function (hàm), loop (vòng lặp), bug (lỗi)\n'
     'Mạng: router (bộ định tuyến), bandwidth (băng thông), packet (gói tin)\n'
     'CSDL: table (bảng), query (truy vấn), index (chỉ mục)\nBảo mật: encryption (mã hoá), firewall (tường lửa)\n'),
]

# Tài liệu có phí (tiêu đề -> giá VNĐ). Người đọc cần được admin cấp quyền mới tải được.
PAID_DOCUMENTS = {
    'Quy hoạch động cơ bản qua 10 bài toán kinh điển': 30000,
    'Giới thiệu mạng nơ-ron và học sâu cho sinh viên': 50000,
}

# Tài khoản quản trị dễ nhớ (tạo bằng scripts/create-demo-admin.sql)
ADMIN_USERNAME = 'quantri'
ADMIN_PASSWORD = '12345678'


# =========================================================
# PHẦN 2: HÀM GỌI API
# =========================================================

class Api:
    def __init__(self, base):
        self.base = base.rstrip('/')

    def call(self, method, path, body=None, token=None, raw=None, content_type=None):
        """Gọi API, trả về phần data. Lỗi thì ném RuntimeError kèm mã HTTP."""
        headers = {}
        data = None
        if body is not None:
            data = json.dumps(body).encode('utf-8')
            headers['Content-Type'] = 'application/json'
        if raw is not None:
            data = raw
            headers['Content-Type'] = content_type
        if token:
            headers['Authorization'] = 'Bearer ' + token
        request = urllib.request.Request(self.base + path, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request) as response:
                text = response.read().decode('utf-8')
        except urllib.error.HTTPError as error:
            text = error.read().decode('utf-8', errors='replace')
            raise RuntimeError(f'{error.code} {method} {path}: {text[:200]}') from None
        return json.loads(text)['data'] if text else None

    def get(self, path, token=None):
        return self.call('GET', path, token=token)

    def post(self, path, body, token=None):
        return self.call('POST', path, body, token)

    def put(self, path, body, token=None):
        return self.call('PUT', path, body, token)


def multipart(fields, file_field, file_name, file_bytes, file_type):
    """Tự đóng gói dữ liệu multipart/form-data (không cần thư viện requests)."""
    boundary = uuid.uuid4().hex
    parts = []
    for name, (value, value_type) in fields.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n'
                     f'Content-Type: {value_type}\r\n\r\n'.encode('utf-8') + value + b'\r\n')
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{file_field}"; filename="{file_name}"\r\n'
                 f'Content-Type: {file_type}\r\n\r\n'.encode('utf-8') + file_bytes + b'\r\n')
    parts.append(f'--{boundary}--\r\n'.encode('utf-8'))
    return b''.join(parts), f'multipart/form-data; boundary={boundary}'


# =========================================================
# PHẦN 3: CÁC BƯỚC TẠO DỮ LIỆU
# =========================================================

def find_organization(api, school_name, faculty_name):
    """Tìm id trường và khoa theo tên. Không thấy thì trả về (None, None)."""
    for school in api.get('/institutions'):
        if school['name'] != school_name:
            continue
        if not faculty_name:
            return school['id'], None
        for faculty in api.get(f"/institutions/{school['id']}/faculties"):
            if faculty['name'] == faculty_name:
                return school['id'], faculty['id']
        return school['id'], None
    return None, None


def create_users(api):
    """Bước 1 + 2: đăng ký (hoặc đăng nhập) và cập nhật hồ sơ. Trả về {username: (token, id)}."""
    accounts = {}
    for username, display_name, bio, school, faculty in USERS:
        school_id, faculty_id = find_organization(api, school, faculty)
        try:
            api.post('/auth/register', {
                'username': username,
                'email': f'{username}@{EMAIL_DOMAIN}',
                'password': DEMO_PASSWORD,
                'institutionId': school_id,
                'facultyId': faculty_id,
            })
            status = 'đã tạo'
        except RuntimeError as error:
            if '409' not in str(error) and 'tồn tại' not in str(error) and 'đã được' not in str(error):
                raise
            status = 'đã có sẵn'

        login = api.post('/auth/login', {'username': username, 'password': DEMO_PASSWORD})
        token = login['accessToken']
        api.put('/users/me/profile', {
            'displayName': display_name,
            'bio': bio,
            'institutionId': school_id,
            'facultyId': faculty_id,
        }, token)
        accounts[username] = (token, login['user']['id'])
        print(f'  {username:16} {status}')
    return accounts


def create_follows(api, accounts):
    """Bước 3: theo dõi nhau."""
    total = 0
    for follower, targets in FOLLOWS:
        token = accounts[follower][0]
        for target in targets:
            api.post(f'/community/users/{accounts[target][1]}/follow', {'following': True}, token)
            total += 1
    print(f'  {total} lượt theo dõi')


def category_ids(api):
    """Đọc danh sách chủ đề: {tên: id}."""
    return {c['name']: c['id'] for c in api.get('/categories')}


def existing_post_titles(api):
    """Tiêu đề các bài đã có, để không đăng trùng khi chạy lại."""
    titles = set()
    page = 0
    while True:
        data = api.get(f'/posts?size=30&page={page}')
        titles.update(p['title'] for p in data['content'])
        page += 1
        if page >= data['totalPages']:
            return titles


def create_posts(api, accounts):
    """Bước 4 + 5: đăng bài, trả lời, thích, lưu và bình chọn."""
    categories = category_ids(api)
    existing = existing_post_titles(api)
    comment_ids = {}  # (tác giả, nội dung) -> id bình luận
    created = 0

    for post in POSTS:
        if post['title'] in existing:
            print(f'  (bỏ qua, đã có) {post["title"]}')
            continue

        token = accounts[post['author']][0]
        detail = api.post('/posts', {
            'type': post['type'],
            'title': post['title'],
            'content': post['content'],
            'categoryId': categories[post['category']],
            'tags': post['tags'],
        }, token)
        post_id = detail['post']['id']
        created += 1

        # Câu trả lời và phản hồi lồng nhau
        for author, text, replies in post['answers']:
            answer = api.post(f'/posts/{post_id}/comments', {'content': text, 'parentId': None}, accounts[author][0])
            comment_ids[(author, text)] = answer['id']
            for reply_author, reply_text in replies:
                api.post(f'/posts/{post_id}/comments', {'content': reply_text, 'parentId': answer['id']},
                         accounts[reply_author][0])

        for username in post['likes']:
            api.post(f'/posts/{post_id}/like', {'active': True}, accounts[username][0])
        for username in post['saves']:
            api.post(f'/posts/{post_id}/bookmark', {'active': True}, accounts[username][0])

    # Bình chọn câu trả lời (chỉ cho các bài vừa tạo trong lần chạy này)
    votes = 0
    for voter, author, start, value in VOTES:
        # Tìm câu trả lời của đúng tác giả, có nội dung bắt đầu bằng đoạn chữ đã cho
        for (comment_author, comment_text), comment_id in comment_ids.items():
            if comment_author == author and comment_text.startswith(start):
                api.post(f'/comments/{comment_id}/vote', {'value': value}, accounts[voter][0])
                votes += 1
                break
    print(f'  {created} bài viết mới, {votes} lượt bình chọn')


def my_document_titles(api, token):
    """Tiêu đề tất cả tài liệu mà một tài khoản đã đăng (mọi trạng thái)."""
    titles = set()
    page = 0
    while True:
        data = api.get(f'/users/me/documents?size=30&page={page}', token)
        titles.update(item['document']['title'] for item in data['content'])
        page += 1
        if page >= data['totalPages']:
            return titles


def login_admin(api):
    """Đăng nhập tài khoản quản trị. Chưa có thì trả về None."""
    try:
        return api.post('/auth/login', {'username': ADMIN_USERNAME, 'password': ADMIN_PASSWORD})['accessToken']
    except RuntimeError:
        return None


def create_documents(api, accounts, admin_token):
    """Bước 6: đóng góp tài liệu. Của thành viên thì chờ duyệt, của quản trị viên thì công bố ngay."""
    categories = category_ids(api)
    created = 0
    titles_by_author = {}

    for author, file_name, title, summary, category, level, tags, content in DOCUMENTS:
        if author == ADMIN_USERNAME:
            token = admin_token
            if not token:
                print(f'  (bỏ qua, chưa có tài khoản {ADMIN_USERNAME}) {title}')
                continue
        else:
            token = accounts[author][0]

        if author not in titles_by_author:
            titles_by_author[author] = my_document_titles(api, token)
        if title in titles_by_author[author]:
            continue

        price = PAID_DOCUMENTS.get(title, 0)
        metadata = {
            'title': title,
            'summary': summary,
            'description': summary,
            'categoryId': categories[category],
            'level': level,
            'accessType': 'PAID' if price > 0 else 'FREE',
            'price': price,
            'tags': tags,
        }
        file_type = 'text/markdown' if file_name.endswith('.md') else 'text/plain'
        body, content_type = multipart(
            {'metadata': (json.dumps(metadata).encode('utf-8'), 'application/json')},
            'file', file_name, content.encode('utf-8'), file_type)
        api.call('POST', '/documents', token=token, raw=body, content_type=content_type)
        created += 1
    print(f'  {created} tài liệu mới')


def approve_documents(api, admin_token):
    """Bước 7: quản trị viên duyệt các tài liệu mẫu đang chờ duyệt."""
    if not admin_token:
        print(f'  Chưa có tài khoản {ADMIN_USERNAME}: chạy scripts/create-demo-admin.sql rồi chạy lại script này.')
        return

    demo_titles = {doc[2] for doc in DOCUMENTS}
    approved = 0
    while True:
        # Luôn đọc trang đầu vì tài liệu đã duyệt sẽ rời khỏi danh sách chờ
        pending = api.get('/admin/documents?status=PENDING&size=30', admin_token)
        targets = [item for item in pending['content'] if item['document']['title'] in demo_titles]
        if not targets:
            break
        for item in targets:
            api.post(f"/admin/documents/{item['document']['id']}/review", {
                'version': item['version'],
                'action': 'APPROVE',
                'note': 'Nội dung phù hợp, đã duyệt.',
            }, admin_token)
            approved += 1
    print(f'  {approved} tài liệu được duyệt và công bố')


def main():
    parser = argparse.ArgumentParser(description='Tạo dữ liệu mẫu cho Knowledge Hub')
    parser.add_argument('--api', default='http://localhost:8080/api', help='Địa chỉ API của backend')
    args = parser.parse_args()
    api = Api(args.api)

    # Cửa sổ dòng lệnh Windows mặc định không in được tiếng Việt -> chuyển sang UTF-8
    sys.stdout.reconfigure(encoding='utf-8')

    try:
        api.get('/health')
    except Exception:
        print('Không kết nối được backend. Hãy chạy backend trước (cổng 8080).')
        sys.exit(1)

    print('1. Tài khoản:')
    accounts = create_users(api)
    print('2. Theo dõi:')
    create_follows(api, accounts)
    print('3. Bài viết và bình luận:')
    create_posts(api, accounts)
    admin_token = login_admin(api)
    print('4. Tài liệu đóng góp:')
    create_documents(api, accounts, admin_token)
    print('5. Quản trị viên duyệt tài liệu:')
    approve_documents(api, admin_token)
    print('Xong! Danh sách tài khoản: docs/demo-accounts.md')


if __name__ == '__main__':
    main()
