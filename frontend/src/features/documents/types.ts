/*
 * Các kiểu dữ liệu (interface) của phần Thư viện tài liệu.
 * Tên trường giống với JSON mà backend trả về.
 */

/** Thông tin tóm tắt của một tài liệu (dùng cho thẻ tài liệu, danh sách). */
export interface DocumentItem {
  id: number;
  slug: string;
  title: string;
  summary: string;
  categoryId: number;
  category: string;
  accessType: 'FREE' | 'PAID'; // Miễn phí hoặc có phí
  price: number;
  currency: string;
  level: string; // BEGINNER | INTERMEDIATE | ADVANCED
  language: string;
  coverStyle: string;
  featured: boolean;
  publishedAt: string;
  downloads: number;
  institutionId: number | null;
  institutionName: string | null;
  facultyId: number | null;
  facultyName: string | null;
  fileFormat: string; // PDF, DOCX, MD...
  subjectId: number | null;
  subjectName: string | null;
  tags: string[];
}

/** Dữ liệu có phân trang. */
export interface PageData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

/** Một chủ đề (danh mục) và số tài liệu trong đó. */
export interface CategoryInfo {
  id: number;
  name: string;
  slug: string;
  count: number;
}

/** Số liệu chung của thư viện (hiện ở trang chủ). */
export interface CatalogueStats {
  documents: number;
  freeDocuments: number;
  categories: number;
  downloads: number;
  institutionId: number | null;
  institutionName: string | null;
  facultyId: number | null;
  facultyName: string | null;
  fileFormat: string;
}

/** File đính kèm của tài liệu. */
export interface DocumentFileInfo {
  id: number;
  name: string;
  contentType: string;
  sizeBytes: number;
}

/** Thông tin đầy đủ của tài liệu (trang chi tiết). */
export interface DocumentDetail {
  document: DocumentItem;
  description: string;
  author: string;
  saved: boolean; // Người xem đã lưu tài liệu này chưa
  canDownload: boolean;
  status: string;
  reviewNote: string | null; // Lý do từ chối (nếu bị từ chối)
  version: number | null;
  canManage: boolean; // Người xem có được sửa / xoá không
  canPreview: boolean;
  files: DocumentFileInfo[];
}

/** Một lần tải tài liệu (lịch sử tải). */
export interface DownloadInfo {
  id: number;
  documentId: number;
  title: string;
  category: string;
  fileName: string;
  downloadedAt: string;
  available: boolean; // Tài liệu còn được công bố không
}

/** Số liệu cá nhân trong "Trang của tôi". */
export interface PersonalStats {
  downloadedDocuments: number;
  downloads: number;
  savedDocuments: number;
}

/** Tài liệu trong màn hình quản lý (của tác giả hoặc admin). */
export interface ManagedDocument {
  document: DocumentItem;
  description: string;
  authorId: number | null;
  author: string;
  status: string;
  reviewNote: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

// =========================================================
// CÁC BẢNG DỊCH SANG TIẾNG VIỆT
// =========================================================

export const levelNames: Record<string, string> = {
  BEGINNER: 'Cơ bản',
  INTERMEDIATE: 'Trung cấp',
  ADVANCED: 'Nâng cao',
};

export const documentStatuses: Record<string, string> = {
  PENDING: 'Chờ duyệt',
  PUBLISHED: 'Đã công bố',
  REJECTED: 'Bị từ chối',
  ARCHIVED: 'Đã ẩn',
  DRAFT: 'Bản nháp',
  DELETED: 'Đã xóa',
};

const formatNames: Record<string, string> = {
  MD: 'Markdown',
  TXT: 'Văn bản',
  PDF: 'PDF',
  DOCX: 'Word',
  ZIP: 'Tệp nén ZIP',
  RAR: 'Tệp nén RAR',
};

// =========================================================
// CÁC HÀM HIỂN THỊ
// =========================================================

/** Hiện giá tài liệu: "Miễn phí" hoặc số tiền theo định dạng Việt Nam. */
export function priceLabel(doc: DocumentItem) {
  if (doc.accessType === 'FREE') {
    return 'Miễn phí';
  }
  const formatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: doc.currency });
  return formatter.format(doc.price);
}

/** Đổi số byte sang chuỗi dễ đọc: B, KB hoặc MB. */
export function fileSize(bytes: number) {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Tên định dạng file bằng tiếng Việt. */
export function formatName(format: string) {
  return formatNames[format] || format;
}
