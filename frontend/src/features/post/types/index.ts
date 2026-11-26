/*
 * Các kiểu dữ liệu của Bảng tin cộng đồng & Hỏi đáp (Sprint 3).
 * Tên trường giống với JSON mà backend trả về.
 */

/** Loại bài: câu hỏi hoặc bài chia sẻ kiến thức. */
export type PostType = 'QUESTION' | 'ARTICLE';

/** Tác giả của bài viết / bình luận. */
export interface PostAuthor {
  id: number;
  username: string;
  displayName: string | null;
  avatarUrl: string | null;
}

/** Thông tin tóm tắt của bài viết (hiện trên bảng tin). */
export interface PostSummary {
  id: number;
  type: PostType;
  title: string;
  excerpt: string; // Đoạn trích ngắn của nội dung
  publishedAt: string;
  author: PostAuthor;
  categoryId: number | null;
  category: string | null;
  tags: string[];
  likes: number;
  comments: number;
  views: number;
  liked: boolean; // Người xem đã thích chưa
  saved: boolean; // Người xem đã lưu chưa
  mine: boolean; // Bài của chính người xem
}

/** Bài viết đầy đủ (trang chi tiết). */
export interface PostDetail {
  post: PostSummary;
  content: string; // Nội dung Markdown
  updatedAt: string;
  version: number;
  canModerate: boolean; // Người xem được xoá bài (tác giả hoặc kiểm duyệt viên)
}

/** Một bình luận / câu trả lời. Bình luận đã xoá thì author và content là null. */
export interface CommentItem {
  id: number;
  parentId: number | null; // null = trả lời trực tiếp bài viết
  author: PostAuthor | null;
  content: string | null;
  deleted: boolean;
  createdAt: string;
  editedAt: string | null;
  score: number; // Tổng điểm bình chọn
  myVote: number; // Bình chọn của người xem: 1, 0 hoặc -1
  mine: boolean;
  canDelete: boolean;
}

/** Một mục có đếm số lượng (chủ đề, thẻ...). */
export interface CountItem {
  id: number;
  name: string;
  count: number;
}

/** Thành viên tích cực (cột bên phải bảng tin). */
export interface ActiveMember {
  id: number;
  name: string;
  avatarUrl: string | null;
  posts: number;
}

/** Dữ liệu cột bên của bảng tin. */
export interface FeedSidebar {
  categories: CountItem[];
  trendingTags: CountItem[];
  activeMembers: ActiveMember[];
  totalPosts: number;
}

/** Kết quả sau khi bấm Thích / Lưu. */
export interface Counter {
  active: boolean;
  count: number;
}

/** Một trang bài viết. */
export interface PostPage {
  content: PostSummary[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
