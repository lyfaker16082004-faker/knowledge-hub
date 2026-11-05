-- Original starter materials, not external publications. Paid access is manual; no checkout.

INSERT IGNORE INTO categories (name,slug,sort_order,active,created_at,updated_at,version) VALUES ('Lập trình Java','java-spring',0,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0);

INSERT IGNORE INTO categories (name,slug,sort_order,active,created_at,updated_at,version) VALUES ('Cơ sở dữ liệu','database',0,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0);

INSERT IGNORE INTO categories (name,slug,sort_order,active,created_at,updated_at,version) VALUES ('Frontend','frontend',0,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0);

INSERT IGNORE INTO categories (name,slug,sort_order,active,created_at,updated_at,version) VALUES ('Kỹ năng học tập','study',0,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0);

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Java căn bản: từ biến đến lập trình hướng đối tượng','java-can-ban','Làm quen với biến, điều kiện, vòng lặp và cách tổ chức một chương trình Java dễ đọc.','Làm quen với biến, điều kiện, vòng lặp và cách tổ chức một chương trình Java dễ đọc.

Nội dung chính:
1. Chương trình đầu tiên
2. Biến và điều kiện
3. Tách trách nhiệm
4. Bài tập

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','BEGINNER','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'violet',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='java-spring';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'java-can-ban.md','java-can-ban.md','text/markdown; charset=UTF-8',948,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='java-can-ban';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Thiết kế MySQL và lưu tiếng Việt đúng cách','mysql-tieng-viet','Khóa chính, khóa ngoại, utf8mb4 và những kiểm tra đầu tiên khi làm việc với MySQL.','Khóa chính, khóa ngoại, utf8mb4 và những kiểm tra đầu tiên khi làm việc với MySQL.

Nội dung chính:
1. Tạo database Unicode
2. Ràng buộc
3. Kiểm thử tiếng Việt
4. Bài tập

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','BEGINNER','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'peach',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='database';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'mysql-tieng-viet.md','mysql-tieng-viet.md','text/markdown; charset=UTF-8',931,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='mysql-tieng-viet';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'React & TypeScript: xây giao diện đầu tiên','react-typescript','Component, props, state và cách xử lý các trạng thái tải, lỗi, rỗng trong giao diện.','Component, props, state và cách xử lý các trạng thái tải, lỗi, rỗng trong giao diện.

Nội dung chính:
1. Component
2. State
3. Gọi API
4. Bài tập

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','BEGINNER','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'mint',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='frontend';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'react-typescript.md','react-typescript.md','text/markdown; charset=UTF-8',765,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='react-typescript';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Git thực hành cho một nhóm nhỏ','git-lam-viec-nhom','Quy trình branch, commit, pull request và cách xử lý xung đột một cách bình tĩnh.','Quy trình branch, commit, pull request và cách xử lý xung đột một cách bình tĩnh.

Nội dung chính:
1. Kiểm tra trước khi sửa
2. Một nhánh cho một mục tiêu
3. Review
4. Xung đột

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','BEGINNER','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'sky',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='study';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'git-lam-viec-nhom.md','git-lam-viec-nhom.md','text/markdown; charset=UTF-8',766,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='git-lam-viec-nhom';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Checklist xây REST API dễ sử dụng','rest-api-checklist','Một checklist ngắn về DTO, validation, phân trang, xác thực và lỗi API.','Một checklist ngắn về DTO, validation, phân trang, xác thực và lỗi API.

Nội dung chính:
Hợp đồng API
Dữ liệu
Cookie và tải file
Kiểm thử

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','INTERMEDIATE','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'rose',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='java-spring';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'rest-api-checklist.md','rest-api-checklist.md','text/markdown; charset=UTF-8',917,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='rest-api-checklist';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Lập kế hoạch tự học trong 4 tuần','ke-hoach-tu-hoc','Chia mục tiêu lớn thành các buổi học nhỏ, ghi tiến độ và ôn tập có chủ đích.','Chia mục tiêu lớn thành các buổi học nhỏ, ghi tiến độ và ôn tập có chủ đích.

Nội dung chính:
Tuần 1: Mục tiêu
Tuần 2: Sản phẩm nhỏ
Tuần 3: Củng cố
Tuần 4: Hoàn thiện
Mẫu ghi chép

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','BEGINNER','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'sand',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='study';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'ke-hoach-tu-hoc.md','ke-hoach-tu-hoc.md','text/markdown; charset=UTF-8',852,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='ke-hoach-tu-hoc';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Thiết kế module Spring Boot có kiểm thử','spring-boot-chuyen-sau','Tài liệu mẫu có phí: transaction, service và kiểm thử tích hợp cho module tài khoản.','Tài liệu mẫu có phí: transaction, service và kiểm thử tích hợp cho module tài khoản.

Nội dung chính:
1. Ranh giới module
2. Transaction
3. Thu hồi phiên
4. Kiểm thử
Bài tập

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','PAID',49000,'VND','INTERMEDIATE','vi','PUBLISHED',false,UTC_TIMESTAMP(6),'violet',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='java-spring';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'spring-boot-chuyen-sau.md','spring-boot-chuyen-sau.md','text/markdown; charset=UTF-8',873,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='spring-boot-chuyen-sau';
