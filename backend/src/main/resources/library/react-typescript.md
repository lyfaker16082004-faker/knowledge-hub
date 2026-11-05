# React & TypeScript: xây giao diện đầu tiên

Tài liệu mẫu do Knowledge Hub biên soạn để minh họa chức năng thư viện.

## 1. Component
```tsx
type Props = { name: string };
export function Greeting({ name }: Props) {
    return <h1>Xin chào, {name}!</h1>;
}
```

## 2. State
State lưu trạng thái theo tương tác. Không sửa trực tiếp một mảng trong state; tạo mảng mới để React nhận biết thay đổi.

## 3. Gọi API
Hiển thị loading, error, empty và success. Hủy request bằng AbortController khi component unmount hoặc bộ lọc đổi.

## 4. Bài tập
Tạo danh sách tài liệu có ô tìm kiếm. Khi không có kết quả, hiển thị lời gợi ý thay vì để trống màn hình.
