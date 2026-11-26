import type { ApiResponse } from './types';

/*
 * Các hàm gọi API tới backend Spring Boot.
 * Mọi request đều đi qua hàm read() để:
 *  - tự gắn token đăng nhập (JWT) vào header
 *  - đọc JSON trả về và lấy phần "data"
 *  - báo lỗi bằng tiếng Việt nếu có sự cố
 */

// Địa chỉ API. Khi chạy dev, Vite sẽ chuyển tiếp /api sang http://localhost:8080
const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api';

// Tên khoá dùng để lưu token trong sessionStorage
const tokenKey = 'knowledge-hub.access-token';

/** Lỗi do API trả về, có kèm mã HTTP (401, 403, 404...). */
export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

// =========================================================
// PHẦN 1: LƯU / ĐỌC TOKEN ĐĂNG NHẬP
// =========================================================

/** Lưu token sau khi đăng nhập. Truyền null để xoá token (đăng xuất). */
export function setAccessToken(token: string | null) {
  try {
    if (token) {
      sessionStorage.setItem(tokenKey, token);
    } else {
      sessionStorage.removeItem(tokenKey);
    }
  } catch {
    // Một số trình duyệt chặn sessionStorage -> người dùng sẽ phải đăng nhập lại
  }
}

/** Đọc token đang lưu. Nếu token đã hết hạn thì xoá đi và trả về null. */
export function getAccessToken() {
  try {
    const token = sessionStorage.getItem(tokenKey);
    if (!token) {
      return null;
    }

    // JWT có dạng: header.payload.signature -> lấy phần payload (ở giữa)
    const payload = token.split('.')[1];
    if (!payload) {
      throw new Error('Invalid JWT');
    }

    // Payload được mã hoá base64url -> đổi về base64 thường rồi giải mã
    let base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    base64 = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const claims = JSON.parse(atob(base64));

    // exp là thời điểm hết hạn (tính bằng giây)
    const isExpired = !claims.exp || claims.exp * 1000 <= Date.now();
    if (isExpired) {
      setAccessToken(null);
      window.dispatchEvent(new Event('auth-expired'));
      return null;
    }
    return token;
  } catch {
    setAccessToken(null);
    return null;
  }
}

/** Tạo header Authorization nếu đang đăng nhập. */
function authorizationHeader(): Record<string, string> {
  const token = getAccessToken();
  if (token) {
    return { Authorization: `Bearer ${token}` };
  }
  return {};
}

/** Gọi khi backend trả về 401: xoá token và báo cho App biết phiên đã hết hạn. */
function handleExpiredSession() {
  setAccessToken(null);
  window.dispatchEvent(new Event('auth-expired'));
}

/** Đọc JSON trả về, nếu body không phải JSON thì trả về null. */
async function readJson(response: Response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

// =========================================================
// PHẦN 2: HÀM GỌI API CHUNG
// =========================================================

async function read<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;

  try {
    const headers = new Headers(init.headers);

    // Trang đăng nhập / đăng ký không cần gửi token
    const isAuthPage = path === '/auth/login' || path === '/auth/register';
    if (!isAuthPage) {
      const auth = authorizationHeader();
      for (const name of Object.keys(auth)) {
        headers.set(name, auth[name]);
      }
    }

    response = await fetch(`${baseUrl}${path}`, { ...init, credentials: 'omit', headers });
  } catch (error) {
    // Request bị huỷ (người dùng chuyển trang) -> ném lại lỗi gốc
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    throw new ApiError(0, 'Không thể kết nối máy chủ. Hãy kiểm tra backend và thử lại.');
  }

  const result = (await readJson(response)) as ApiResponse<T> | null;

  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/auth/login')) {
      handleExpiredSession();
    }
    const message = result?.message || `Yêu cầu thất bại (${response.status}). Vui lòng thử lại.`;
    throw new ApiError(response.status, message);
  }

  return result?.data as T;
}

/** Gọi API dạng GET. */
export function get<T>(path: string, signal?: AbortSignal) {
  return read<T>(path, { signal });
}

/**
 * Gọi API dạng POST / PUT / DELETE.
 * form = true: gửi dữ liệu dạng form (dùng cho đăng nhập), ngược lại gửi JSON.
 */
export function mutate<T>(path: string, method: 'POST' | 'PUT' | 'DELETE', data?: unknown, form = false) {
  let body: string | undefined = undefined;
  let contentType = 'application/json';

  if (form) {
    contentType = 'application/x-www-form-urlencoded';
    body = new URLSearchParams(data as Record<string, string>).toString();
  } else if (data !== undefined) {
    body = JSON.stringify(data);
  }

  return read<T>(path, { method, headers: { 'Content-Type': contentType }, body });
}

/** Lấy câu thông báo lỗi để hiển thị cho người dùng. */
export function errorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
}

// =========================================================
// PHẦN 3: TẢI FILE LÊN / TẢI FILE VỀ
// =========================================================

/** Tải file tài liệu về máy. */
export async function downloadFile(documentId: number, fileId: number, fileName: string) {
  const response = await fetch(`${baseUrl}/documents/${documentId}/files/${fileId}/download`, {
    method: 'POST',
    credentials: 'omit',
    headers: authorizationHeader(),
  });

  if (!response.ok) {
    const result = await readJson(response);
    if (response.status === 401) {
      handleExpiredSession();
    }
    throw new ApiError(response.status, result?.message || 'Không thể tải tài liệu. Vui lòng thử lại.');
  }

  // Tạo một thẻ <a> tạm thời rồi bấm vào để trình duyệt tải file
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** Gửi file lên server (multipart/form-data). */
export async function uploadMultipart<T>(path: string, form: FormData): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      method: 'POST',
      credentials: 'omit',
      headers: authorizationHeader(),
      body: form,
    });
  } catch {
    throw new ApiError(0, 'Không thể kết nối máy chủ để tải tệp.');
  }

  const result = await readJson(response);
  if (!response.ok) {
    if (response.status === 401) {
      handleExpiredSession();
    }
    throw new ApiError(response.status, result?.message || 'Không thể tải tệp. Vui lòng thử lại.');
  }
  return result.data as T;
}

/** Đăng tài liệu mới: gửi thông tin (metadata) dạng JSON kèm file. */
export function uploadDocument<T>(metadata: unknown, file: File) {
  const form = new FormData();
  form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
  form.append('file', file);
  return uploadMultipart<T>('/documents', form);
}

/** Lấy file để xem trước. sample = true: chỉ lấy bản xem thử vài trang đầu. */
export async function previewFile(documentId: number, fileId: number, sample = false) {
  const route = sample ? 'sample' : 'preview';
  const response = await fetch(`${baseUrl}/documents/${documentId}/files/${fileId}/${route}`, {
    credentials: 'omit',
    headers: authorizationHeader(),
  });

  if (!response.ok) {
    const result = await readJson(response);
    if (response.status === 401) {
      handleExpiredSession();
    }
    throw new ApiError(response.status, result?.message || 'Không thể mở tệp xem trước.');
  }
  return response.blob();
}
