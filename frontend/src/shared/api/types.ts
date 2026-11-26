/** Dạng dữ liệu chung mà mọi API của backend trả về. */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

/** Dữ liệu có phân trang. */
export interface PageResponse<T> {
  content: T[]; // Danh sách phần tử của trang hiện tại
  totalElements: number; // Tổng số phần tử
  totalPages: number; // Tổng số trang
  page: number; // Trang hiện tại (bắt đầu từ 0)
  size: number; // Số phần tử mỗi trang
}
