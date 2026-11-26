import { get, mutate, setAccessToken } from "../../../shared/api/http-client";
import type { Account } from "../types";

/* Các API về tài khoản: lấy thông tin, đăng nhập, đăng ký, đăng xuất. */

/** Kết quả trả về khi đăng nhập thành công. */
export interface LoginResult {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: Account;
}
/** Lấy thông tin người đang đăng nhập (401 nếu chưa đăng nhập). */
export const currentAccount = () => get<Account>("/users/me");
/** Đăng nhập bằng email hoặc tên đăng nhập, sau đó lưu token. */
export async function login(username: string, password: string) {
  const result = await mutate<LoginResult>("/auth/login", "POST", {
    username,
    password,
  });
  setAccessToken(result.accessToken);
  return result.user;
}
/** Đăng ký tài khoản mới. */
export async function register(
  username: string,
  email: string,
  password: string,
  institutionId: number | null = null,
  facultyId: number | null = null,
) {
  return mutate<Account>("/auth/register", "POST", {
    username: username.trim() || undefined,
    email,
    password,
    institutionId,
    facultyId,
  });
}
/** Đăng xuất: báo backend thu hồi token rồi xoá token trên trình duyệt. */
export async function logout() {
  await mutate("/auth/logout", "POST");
  setAccessToken(null);
}
