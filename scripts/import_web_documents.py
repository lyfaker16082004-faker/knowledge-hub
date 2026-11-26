"""
Đưa một số tài liệu có giấy phép mở (lấy từ internet) vào thư viện Knowledge Hub,
để thư viện có đủ các loại file: PPTX, DOCX, ZIP, PDF.

Script sẽ:
  1. Tải file về thư mục --folder (file đã có sẵn thì không tải lại)
  2. Đăng tài liệu bằng tài khoản thành viên mẫu (ghi rõ nguồn và giấy phép trong mô tả)
  3. Đăng nhập quản trị viên quantri và duyệt tài liệu

Cần chạy scripts/seed_demo_data.py và scripts/create-demo-admin.sql trước.

Cách chạy (backend phải đang chạy ở cổng 8080):
    python scripts/import_web_documents.py --folder web-docs
"""
import argparse
import json
import os
import sys
import urllib.request

# Dùng lại các hàm gọi API của script dữ liệu mẫu
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from seed_demo_data import (ADMIN_PASSWORD, ADMIN_USERNAME, DEMO_PASSWORD, Api, category_ids,  # noqa: E402
                            multipart, my_document_titles)

# =========================================================
# DANH SÁCH TÀI LIỆU LẤY TỪ INTERNET
# =========================================================

WEB_DOCUMENTS = [
    {
        'author': 'doquangminh',
        'file': 'KITE-OpenCL-course.pptx',
        'url': 'https://github.com/HandsOnOpenCL/Lecture-Slides/releases/download/v1.2/KITE-OpenCL-course.pptx',
        'type': 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'title': 'Slide khóa học Lập trình song song với OpenCL (Đại học Bristol)',
        'summary': 'Bộ slide bài giảng tiếng Anh về lập trình song song trên GPU/CPU với OpenCL: '
                   'mô hình nền tảng, bộ nhớ, kernel và tối ưu hiệu năng.',
        'category': 'Công nghệ phần mềm',
        'level': 'ADVANCED',
        'tags': ['opencl', 'lập trình song song', 'slide'],
        'source': 'Simon McIntosh-Smith, Tom Deakin (University of Bristol) — HandsOnOpenCL Lecture Slides v1.2',
        'license': 'Creative Commons Attribution 3.0 (CC BY 3.0)',
        'page': 'https://github.com/HandsOnOpenCL/Lecture-Slides',
    },
    {
        'author': 'vuthiha',
        'file': 'POGIL.docx',
        'url': 'https://cs.calvin.edu/courses/cs/108/24fa/POGIL.docx',
        'type': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'title': 'Phiếu bài tập nhóm POGIL môn Nhập môn lập trình Python (Calvin University)',
        'summary': 'Phiếu hoạt động học nhóm tiếng Anh theo phương pháp POGIL, dùng cho môn CS108 '
                   'nhập môn lập trình Python.',
        'category': 'Lập trình Python',
        'level': 'BEGINNER',
        'tags': ['python', 'bài tập nhóm', 'pogil'],
        'source': 'Calvin University — CS 108, Fall 2024',
        'license': 'Creative Commons BY-NC-SA 4.0',
        'page': 'https://cs.calvin.edu/courses/cs/108/24fa/',
    },
    {
        'author': 'phamthidung',
        'file': 'python-3.14-docs-text.zip',
        'url': 'https://docs.python.org/3.14/archives/python-3.14-docs-text.zip',
        'type': 'application/zip',
        'title': 'Tài liệu chính thức Python 3.14 (bản văn bản, nén ZIP)',
        'summary': 'Toàn bộ tài liệu chính thức của Python 3.14 dạng văn bản thuần: hướng dẫn nhập môn, '
                   'thư viện chuẩn, tham chiếu ngôn ngữ. Đọc được không cần mạng.',
        'category': 'Lập trình Python',
        'level': 'INTERMEDIATE',
        'tags': ['python', 'tài liệu chính thức', 'tham khảo'],
        'source': 'Python Software Foundation — docs.python.org',
        'license': 'PSF Documentation License (Python Software Foundation License)',
        'page': 'https://docs.python.org/3.14/download.html',
    },
    {
        'author': 'nguyenvanan',
        'file': 'MIT6_0001F16_Lec1.pdf',
        'url': 'https://ocw.mit.edu/courses/6-0001-introduction-to-computer-science-and-programming-in-python-fall-2016/'
               'e921a690079369751bcce3e34da6c6ee_MIT6_0001F16_Lec1.pdf',
        'type': 'application/pdf',
        'title': 'Bài giảng 1 — Nhập môn khoa học máy tính và lập trình Python (MIT 6.0001)',
        'summary': 'Bài giảng tiếng Anh đầu tiên của môn MIT 6.0001: máy tính làm việc thế nào, '
                   'kiểu dữ liệu, biểu thức và biến trong Python.',
        'category': 'Lập trình Python',
        'level': 'BEGINNER',
        'tags': ['python', 'mit', 'bài giảng'],
        'source': 'Ana Bell, Eric Grimson, John Guttag — MIT OpenCourseWare 6.0001 (Fall 2016)',
        'license': 'Creative Commons BY-NC-SA 4.0',
        'page': 'https://ocw.mit.edu/courses/6-0001-introduction-to-computer-science-and-programming-in-python-fall-2016/',
    },
]


def download(doc, folder):
    """Tải file về thư mục (đã có thì bỏ qua)."""
    path = os.path.join(folder, doc['file'])
    if not os.path.exists(path):
        request = urllib.request.Request(doc['url'], headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(request) as response, open(path, 'wb') as out:
            out.write(response.read())
    return path


def description(doc):
    """Mô tả có ghi nguồn và giấy phép (giấy phép CC bắt buộc ghi nguồn)."""
    return (f"<p>{doc['summary']}</p>"
            f"<p><strong>Nguồn:</strong> {doc['source']}<br>"
            f"<strong>Giấy phép:</strong> {doc['license']}<br>"
            f"<strong>Trang gốc:</strong> <a href=\"{doc['page']}\">{doc['page']}</a></p>"
            "<p>Knowledge Hub chia sẻ lại nguyên bản, không chỉnh sửa nội dung.</p>")


def main():
    parser = argparse.ArgumentParser(description='Đưa tài liệu mở từ internet vào Knowledge Hub')
    parser.add_argument('--folder', default='web-docs', help='Thư mục chứa file tải về')
    parser.add_argument('--api', default='http://localhost:8080/api')
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding='utf-8')
    os.makedirs(args.folder, exist_ok=True)

    api = Api(args.api)
    categories = category_ids(api)
    admin = api.post('/auth/login', {'username': ADMIN_USERNAME, 'password': ADMIN_PASSWORD})['accessToken']

    titles = set()
    for doc in WEB_DOCUMENTS:
        path = download(doc, args.folder)
        token = api.post('/auth/login', {'username': doc['author'], 'password': DEMO_PASSWORD})['accessToken']
        titles.add(doc['title'])
        if doc['title'] in my_document_titles(api, token):
            print(f"  (bỏ qua, đã có) {doc['file']}")
            continue

        metadata = {
            'title': doc['title'],
            'summary': doc['summary'],
            'description': description(doc),
            'categoryId': categories[doc['category']],
            'level': doc['level'],
            'accessType': 'FREE',
            'price': 0,
            'tags': doc['tags'],
        }
        with open(path, 'rb') as f:
            body, content_type = multipart(
                {'metadata': (json.dumps(metadata).encode('utf-8'), 'application/json')},
                'file', doc['file'], f.read(), doc['type'])
        api.call('POST', '/documents', token=token, raw=body, content_type=content_type)
        print(f"  đã đăng {doc['file']} ({os.path.getsize(path) // 1024} KB)")

    # Quản trị viên duyệt
    pending = api.get('/admin/documents?status=PENDING&size=30', admin)
    approved = 0
    for item in pending['content']:
        if item['document']['title'] in titles:
            api.post(f"/admin/documents/{item['document']['id']}/review",
                     {'version': item['version'], 'action': 'APPROVE', 'note': None}, admin)
            approved += 1
    print(f'  {approved} tài liệu được duyệt')


if __name__ == '__main__':
    main()
