/** Trường / khoa của người dùng. */
export interface OrganizationInfo {
  institutionId: number;
  institutionName: string;
  institutionType: string;
  facultyId: number | null;
  facultyName: string | null;
}
/** Thông tin tài khoản đang đăng nhập. */
export interface Account {
  id: number;
  username: string;
  email: string;
  status: string;
  emailVerified: boolean;
  roles: string[];
  createdAt: string;
  lastLoginAt: string | null;
  organization: OrganizationInfo | null;
  displayName: string | null;
  bio: string | null;
  avatarUrl: string | null;
}
/** Một trang danh sách tài khoản (màn hình quản trị). */
export interface AccountPage {
  content: Account[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
/** Tên vai trò bằng tiếng Việt. */
export const roleNames: Record<string, string> = {
  ADMIN: "Quản trị viên",
  MODERATOR: "Kiểm duyệt viên",
  USER: "Thành viên",
};
/** Tên trạng thái tài khoản bằng tiếng Việt. */
export const statusNames: Record<string, string> = {
  ACTIVE: "Hoạt động",
  INACTIVE: "Tạm ngưng",
  LOCKED: "Đã khóa",
  DELETED: "Đã xóa mềm",
};
/** Hiện ngày giờ kiểu Việt Nam. Backend trả giờ UTC không có Z nên phải thêm vào. */
export function dateLabel(value: string | null) {
  return value
    ? new Date(value + (/[Z+]/.test(value) ? "" : "Z")).toLocaleString("vi-VN")
    : "Chưa có";
}
