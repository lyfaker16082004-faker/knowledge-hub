import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { mutate, errorMessage } from '../../../shared/api/http-client';
import { useRemote } from '../../../shared/hooks/useRemote';
import type { PageData } from '../../documents/types';
import Icon from '../../../shared/components/ui/Icon';
import {
  AdminDialog,
  Avatar,
  Card,
  EmptyState,
  IconAction,
  LoadingRows,
  Notice,
  PageHeader,
  Pager,
  shortDate,
  useAutoClear,
} from '../components/AdminUI';
import '../../../styles/community-admin.css';

/*
 * Trang quản trị "Cộng đồng & Hỏi đáp" (#admin/community) - Sprint 3.
 * - 4 ô thống kê và các thẻ phổ biến
 * - Tab Bài viết: lọc, tìm, xem nhanh, ẩn / khôi phục
 * - Tab Bình luận: lọc, tìm, ẩn / khôi phục
 *
 * Quy ước trạng thái (giống backend):
 * - Bài viết:  PUBLISHED (đang hiện) | REJECTED (admin ẩn) | ARCHIVED (tác giả đã xoá)
 * - Bình luận: VISIBLE (đang hiện)   | HIDDEN (admin ẩn)   | DELETED (tác giả đã xoá)
 */

// =========================================================
// PHẦN 1: KIỂU DỮ LIỆU VÀ HẰNG SỐ
// =========================================================

interface Author {
  id: number;
  username: string;
  displayName: string | null;
  avatarUrl: string | null;
}

interface AdminPost {
  id: number;
  type: 'QUESTION' | 'ARTICLE';
  title: string;
  excerpt: string;
  status: string;
  visibility: string;
  author: Author;
  category: string | null;
  tags: string[];
  createdAt: string;
  publishedAt: string | null;
  likes: number;
  comments: number;
  views: number;
}

interface AdminComment {
  id: number;
  postId: number;
  postTitle: string;
  postStatus: string;
  parentId: number | null; // null = trả lời trực tiếp bài viết
  author: Author;
  content: string;
  status: string;
  createdAt: string;
  editedAt: string | null;
  score: number;
  replies: number;
}

interface CommunityStats {
  posts: number;
  questions: number;
  articles: number;
  unanswered: number;
  hiddenPosts: number;
  comments: number;
  hiddenComments: number;
  postsLast7Days: number;
  commentsLast7Days: number;
  topTags: { name: string; posts: number }[];
}

type Tab = 'posts' | 'comments';

// Một thao tác đang chờ xác nhận (ẩn hoặc khôi phục)
type Confirm =
  | { kind: 'post'; item: AdminPost; next: 'PUBLISHED' | 'REJECTED' }
  | { kind: 'comment'; item: AdminComment; next: 'VISIBLE' | 'HIDDEN' };

const POST_STATUSES: Record<string, string> = {
  PUBLISHED: 'Đang hiển thị',
  REJECTED: 'Đã ẩn',
  ARCHIVED: 'Tác giả đã xóa',
};

const COMMENT_STATUSES: Record<string, string> = {
  VISIBLE: 'Đang hiển thị',
  HIDDEN: 'Đã ẩn',
  DELETED: 'Tác giả đã xóa',
};

// Màu của nhãn trạng thái (dùng lại class của trang Kho tài liệu)
const STATUS_CLASS: Record<string, string> = {
  PUBLISHED: 's-published',
  VISIBLE: 's-published',
  REJECTED: 's-rejected',
  HIDDEN: 's-rejected',
  ARCHIVED: 's-archived',
  DELETED: 's-archived',
};

const TYPE_NAMES = { QUESTION: 'Hỏi đáp', ARTICLE: 'Chia sẻ' };

// =========================================================
// PHẦN 2: HÀM HỖ TRỢ
// =========================================================

/** Tên hiển thị của tác giả. */
function authorName(author: Author) {
  return author.displayName || author.username;
}

/** Đọc tab và bài viết đang lọc từ URL, ví dụ #admin/community?tab=comments&postId=5 */
function readUrl() {
  const params = new URLSearchParams(location.hash.split('?')[1] || '');
  const tab: Tab = params.get('tab') === 'comments' ? 'comments' : 'posts';
  const postId = params.get('postId') || '';
  return { tab, postId };
}

/** Trả về giá trị sau khi người dùng ngừng gõ một lúc (tránh gọi API mỗi lần gõ). */
function useDebounced(value: string, ms = 350) {
  const [current, setCurrent] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setCurrent(value.trim()), ms);
    return () => clearTimeout(timer);
  }, [value, ms]);
  return current;
}

/** Ô tác giả: ảnh đại diện + tên + @username */
function AuthorCell({ author }: { author: Author }) {
  return (
    <span className="adm-person static">
      <Avatar name={author.username} url={author.avatarUrl} />
      <span>
        <strong>{authorName(author)}</strong>
        <small>@{author.username}</small>
      </span>
    </span>
  );
}

// =========================================================
// PHẦN 3: BẢNG BÀI VIẾT VÀ BẢNG BÌNH LUẬN
// =========================================================

type PostTableProps = {
  posts: AdminPost[];
  loading: boolean;
  onPreview: (post: AdminPost) => void;
  onShowComments: (post: AdminPost) => void;
  onConfirm: (confirm: Confirm) => void;
};

function PostTable({ posts, loading, onPreview, onShowComments, onConfirm }: PostTableProps) {
  return (
    <div className={loading ? 'adm-table-wrap is-loading' : 'adm-table-wrap'}>
      <table className="adm-table adm-community-table">
        <caption className="sr-only">Danh sách bài viết cộng đồng</caption>
        <thead>
          <tr>
            <th>Bài viết</th>
            <th>Tác giả</th>
            <th>Tương tác</th>
            <th>Trạng thái</th>
            <th>Ngày đăng</th>
            <th className="end">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {posts.map((p) => (
            <tr key={p.id}>
              <td data-label="Bài viết">
                <button type="button" className="adm-post-title" onClick={() => onPreview(p)}>
                  <span className="adm-post-meta">
                    <span className={`adm-chip t-${p.type.toLowerCase()}`}>{TYPE_NAMES[p.type]}</span>
                    {p.category && <span>{p.category}</span>}
                  </span>
                  <strong>{p.title}</strong>
                  {p.tags.length > 0 && (
                    <span className="adm-post-tags">
                      {p.tags.map((t) => (
                        <i key={t}>#{t}</i>
                      ))}
                    </span>
                  )}
                </button>
              </td>

              <td data-label="Tác giả">
                <AuthorCell author={p.author} />
              </td>

              <td data-label="Tương tác">
                <span
                  className="adm-counters"
                  aria-label={`${p.likes} thích, ${p.comments} bình luận, ${p.views} lượt xem`}
                >
                  <span>
                    <Icon name="heart" size={13} />
                    {p.likes}
                  </span>
                  <span>
                    <Icon name="message" size={13} />
                    {p.comments}
                  </span>
                  <span>
                    <Icon name="eye" size={13} />
                    {p.views}
                  </span>
                </span>
              </td>

              <td data-label="Trạng thái">
                <span className={`adm-doc-status ${STATUS_CLASS[p.status]}`}>{POST_STATUSES[p.status]}</span>
              </td>

              <td data-label="Ngày đăng" className="adm-muted">
                {shortDate(p.publishedAt || p.createdAt)}
              </td>

              <td className="end">
                <div className="adm-row-actions">
                  <IconAction icon="eye" label="Xem nhanh" onClick={() => onPreview(p)} />
                  <IconAction icon="message" label="Xem bình luận" onClick={() => onShowComments(p)} />
                  {p.status === 'PUBLISHED' && (
                    <IconAction
                      icon="eyeOff"
                      label="Ẩn bài viết"
                      tone="danger"
                      onClick={() => onConfirm({ kind: 'post', item: p, next: 'REJECTED' })}
                    />
                  )}
                  {p.status === 'REJECTED' && (
                    <IconAction
                      icon="refresh"
                      label="Khôi phục"
                      tone="success"
                      onClick={() => onConfirm({ kind: 'post', item: p, next: 'PUBLISHED' })}
                    />
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

type CommentTableProps = {
  comments: AdminComment[];
  loading: boolean;
  onConfirm: (confirm: Confirm) => void;
};

function CommentTable({ comments, loading, onConfirm }: CommentTableProps) {
  return (
    <div className={loading ? 'adm-table-wrap is-loading' : 'adm-table-wrap'}>
      <table className="adm-table adm-community-table">
        <caption className="sr-only">Danh sách bình luận cộng đồng</caption>
        <thead>
          <tr>
            <th>Bình luận</th>
            <th>Tác giả</th>
            <th>Điểm</th>
            <th>Trạng thái</th>
            <th>Ngày</th>
            <th className="end">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {comments.map((c) => {
            let scoreClass = 'adm-score';
            if (c.score > 0) scoreClass += ' pos';
            if (c.score < 0) scoreClass += ' neg';

            return (
              <tr key={c.id}>
                <td data-label="Bình luận">
                  <div className="adm-comment-cell">
                    <p>{c.content}</p>
                    <small>
                      {c.parentId ? '↳ Trả lời bình luận trong' : 'Trả lời bài'}{' '}
                      <a href={`#post/${c.postId}`} target="_blank" rel="noreferrer">
                        {c.postTitle}
                      </a>
                      {c.replies > 0 && <> · {c.replies} phản hồi</>}
                    </small>
                  </div>
                </td>

                <td data-label="Tác giả">
                  <AuthorCell author={c.author} />
                </td>

                <td data-label="Điểm">
                  <span className={scoreClass}>{c.score > 0 ? `+${c.score}` : c.score}</span>
                </td>

                <td data-label="Trạng thái">
                  <span className={`adm-doc-status ${STATUS_CLASS[c.status]}`}>{COMMENT_STATUSES[c.status]}</span>
                </td>

                <td data-label="Ngày" className="adm-muted">
                  {shortDate(c.createdAt)}
                </td>

                <td className="end">
                  <div className="adm-row-actions">
                    <IconAction icon="arrow" label="Mở bài viết" href={`#post/${c.postId}`} />
                    {c.status === 'VISIBLE' && (
                      <IconAction
                        icon="eyeOff"
                        label="Ẩn bình luận"
                        tone="danger"
                        onClick={() => onConfirm({ kind: 'comment', item: c, next: 'HIDDEN' })}
                      />
                    )}
                    {c.status === 'HIDDEN' && (
                      <IconAction
                        icon="refresh"
                        label="Khôi phục"
                        tone="success"
                        onClick={() => onConfirm({ kind: 'comment', item: c, next: 'VISIBLE' })}
                      />
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

// =========================================================
// PHẦN 4: TRANG CHÍNH
// =========================================================

export default function CommunityAdminPage() {
  const start = readUrl();

  const [tab, setTab] = useState<Tab>(start.tab);
  // Đang xem bình luận của một bài viết cụ thể
  const [postFilter, setPostFilter] = useState<{ id: string; title: string } | null>(
    start.postId ? { id: start.postId, title: `#${start.postId}` } : null,
  );
  const [status, setStatus] = useState('');
  const [type, setType] = useState('');
  const [draft, setDraft] = useState(''); // Chữ đang gõ trong ô tìm
  const q = useDebounced(draft);
  const [page, setPage] = useState(0);
  const [reload, setReload] = useState(0); // Tăng lên để tải lại dữ liệu
  const [notice, setNotice] = useState('');
  const [actionError, setActionError] = useState('');
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [preview, setPreview] = useState<AdminPost | null>(null);
  const [busy, setBusy] = useState(false);
  useAutoClear(notice, () => setNotice(''));

  // Đổi bộ lọc thì quay về trang đầu
  useEffect(() => {
    setPage(0);
  }, [q, status, type, tab, postFilter]);

  // ----- Tải dữ liệu -----
  const stats = useRemote<CommunityStats>(`/admin/community/stats?_=${reload}`);

  let postsUrl: string | null = null;
  let commentsUrl: string | null = null;
  if (tab === 'posts') {
    postsUrl = `/admin/community/posts?q=${encodeURIComponent(q)}&status=${status}&type=${type}&page=${page}&size=10&_=${reload}`;
  } else {
    commentsUrl = `/admin/community/comments?q=${encodeURIComponent(q)}&status=${status}&page=${page}&size=10&_=${reload}`;
    if (postFilter) {
      commentsUrl += `&postId=${postFilter.id}`;
    }
  }
  const posts = useRemote<PageData<AdminPost>>(postsUrl);
  const comments = useRemote<PageData<AdminComment>>(commentsUrl);

  const current = tab === 'posts' ? posts : comments;
  const statuses = tab === 'posts' ? POST_STATUSES : COMMENT_STATUSES;

  /** Đổi tab, xoá bộ lọc và cập nhật URL. */
  function switchTab(next: Tab, filter: { id: string; title: string } | null = null) {
    setTab(next);
    setStatus('');
    setType('');
    setDraft('');
    setPostFilter(filter);

    let url = '#admin/community';
    if (next === 'comments') {
      url += '?tab=comments';
      if (filter) url += `&postId=${filter.id}`;
    }
    history.replaceState(null, '', url);
  }

  function reloadAll() {
    setReload((n) => n + 1);
  }

  /** Gửi thao tác ẩn / khôi phục đang chờ xác nhận. */
  async function applyConfirm() {
    if (!confirm) {
      return;
    }
    setBusy(true);
    setActionError('');
    try {
      let path = `/admin/community/comments/${confirm.item.id}/status`;
      if (confirm.kind === 'post') {
        path = `/admin/community/posts/${confirm.item.id}/status`;
      }
      await mutate(path, 'PUT', { status: confirm.next });

      const isHiding = confirm.next === 'REJECTED' || confirm.next === 'HIDDEN';
      const what = confirm.kind === 'post' ? 'bài viết' : 'bình luận';
      setNotice(`${isHiding ? 'Đã ẩn' : 'Đã khôi phục'} ${what}.`);

      setConfirm(null);
      setPreview(null);
      reloadAll();
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  function closeConfirm() {
    setConfirm(null);
    setActionError('');
  }

  // ----- 4 ô thống kê -----
  const s = stats.data;
  const kpis = [
    {
      label: 'Bài viết đang hiển thị',
      value: s?.posts,
      hint: s ? `${s.questions} hỏi đáp · ${s.articles} chia sẻ` : '…',
      icon: 'book',
      tone: 'violet',
      onClick: () => {
        switchTab('posts');
        setStatus('PUBLISHED');
      },
    },
    {
      label: 'Câu hỏi chưa có trả lời',
      value: s?.unanswered,
      hint: 'Cần cộng đồng hỗ trợ',
      icon: 'question',
      tone: 'amber',
      onClick: () => {
        switchTab('posts');
        setType('QUESTION');
      },
    },
    {
      label: 'Bình luận',
      value: s?.comments,
      hint: s ? `+${s.commentsLast7Days} trong 7 ngày` : '…',
      icon: 'message',
      tone: 'blue',
      onClick: () => switchTab('comments'),
    },
    {
      label: 'Nội dung đã ẩn',
      value: s ? s.hiddenPosts + s.hiddenComments : undefined,
      hint: s ? `${s.hiddenPosts} bài · ${s.hiddenComments} bình luận` : '…',
      icon: 'eyeOff',
      tone: 'green',
      onClick: () => {
        switchTab('posts');
        setStatus('REJECTED');
      },
    },
  ];

  // ----- Nội dung bảng -----
  let tableContent: ReactNode;
  if (current.error) {
    tableContent = (
      <EmptyState
        icon="refresh"
        title="Chưa tải được dữ liệu"
        text={current.error}
        action={
          <button className="button secondary compact" onClick={reloadAll}>
            Thử lại
          </button>
        }
      />
    );
  } else if (current.loading && !current.data) {
    tableContent = <LoadingRows />;
  } else if (!current.data || current.data.content.length === 0) {
    let emptyTitle = tab === 'posts' ? 'Chưa có bài viết' : 'Chưa có bình luận';
    let emptyText = 'Thay đổi bộ lọc để xem thêm.';
    if (q) {
      emptyTitle = 'Không tìm thấy kết quả';
      emptyText = `Không có kết quả cho “${q}”.`;
    }
    tableContent = <EmptyState icon={q ? 'search' : 'message'} title={emptyTitle} text={emptyText} />;
  } else if (tab === 'posts') {
    tableContent = (
      <PostTable
        posts={posts.data?.content || []}
        loading={posts.loading}
        onPreview={setPreview}
        onShowComments={(p) => switchTab('comments', { id: String(p.id), title: p.title })}
        onConfirm={setConfirm}
      />
    );
  } else {
    tableContent = (
      <CommentTable comments={comments.data?.content || []} loading={comments.loading} onConfirm={setConfirm} />
    );
  }

  // ----- Hộp thoại xác nhận -----
  let confirmTitle = '';
  let confirmText = '';
  let confirmQuote = '';
  let isHiding = false;
  if (confirm) {
    isHiding = confirm.next === 'REJECTED' || confirm.next === 'HIDDEN';
    const what = confirm.kind === 'post' ? 'bài viết' : 'bình luận';
    confirmTitle = (isHiding ? 'Ẩn ' : 'Khôi phục ') + what + '?';
    confirmQuote = confirm.kind === 'post' ? confirm.item.title : confirm.item.content;

    if (!isHiding) {
      confirmText = 'Nội dung sẽ hiển thị lại cho thành viên như trước khi bị ẩn.';
    } else if (confirm.kind === 'post') {
      confirmText =
        'Bài viết sẽ biến mất khỏi bảng tin và trang cá nhân của tác giả. Bình luận và lượt thích vẫn được giữ lại.';
    } else {
      confirmText =
        'Thành viên sẽ thấy dòng “bình luận đã bị xóa” thay cho nội dung này. Các phản hồi bên dưới vẫn hiển thị.';
    }
  }

  return (
    <section className="adm-community">
      <PageHeader
        kicker="NỘI DUNG"
        title="Cộng đồng & Hỏi đáp"
        description="Theo dõi bài viết, câu hỏi và bình luận của thành viên. Ẩn nội dung vi phạm và khôi phục khi cần."
        actions={
          <a className="button secondary" href="#community" target="_blank" rel="noreferrer">
            <Icon name="eye" size={16} />
            Xem bảng tin
          </a>
        }
      />

      {notice && <Notice onClose={() => setNotice('')}>{notice}</Notice>}

      {/* ===== 4 ô thống kê (bấm để lọc) ===== */}
      <div className="adm-kpis" aria-label="Thống kê cộng đồng">
        {kpis.map((k) => (
          <button type="button" className={`adm-kpi k-${k.tone}`} key={k.label} onClick={k.onClick}>
            <span className="adm-kpi-icon">
              <Icon name={k.icon} size={20} />
            </span>
            <span className="adm-kpi-body">
              <small>{k.label}</small>
              <strong>{k.value === undefined ? '—' : k.value.toLocaleString('vi-VN')}</strong>
              <em>{k.hint}</em>
            </span>
            <Icon name="chevronRight" size={16} />
          </button>
        ))}
      </div>

      {/* ===== Thẻ phổ biến ===== */}
      {s && s.topTags.length > 0 && (
        <div className="adm-top-tags">
          <span>
            <Icon name="spark" size={14} />
            Thẻ phổ biến
          </span>
          {s.topTags.map((t) => (
            <button
              type="button"
              key={t.name}
              onClick={() => {
                switchTab('posts');
                setDraft(t.name);
              }}
            >
              #{t.name}
              <em>{t.posts}</em>
            </button>
          ))}
        </div>
      )}

      <Card>
        {/* Tab Bài viết / Bình luận */}
        <div className="adm-community-switch" role="tablist" aria-label="Loại nội dung">
          <button role="tab" aria-selected={tab === 'posts'} onClick={() => switchTab('posts')}>
            <Icon name="book" size={16} />
            Bài viết
          </button>
          <button role="tab" aria-selected={tab === 'comments'} onClick={() => switchTab('comments')}>
            <Icon name="message" size={16} />
            Bình luận
          </button>
        </div>

        {/* Bộ lọc trạng thái, loại bài và ô tìm */}
        <div className="adm-docs-toolbar">
          <div className="adm-tabs small" role="tablist" aria-label="Lọc theo trạng thái">
            <button role="tab" aria-selected={!status} onClick={() => setStatus('')}>
              Tất cả
            </button>
            {Object.keys(statuses).map((key) => (
              <button role="tab" key={key} aria-selected={status === key} onClick={() => setStatus(key)}>
                {statuses[key]}
              </button>
            ))}
          </div>

          <div className="adm-docs-search">
            {tab === 'posts' && (
              <select
                className="adm-select"
                value={type}
                onChange={(e) => setType(e.target.value)}
                aria-label="Loại bài viết"
              >
                <option value="">Mọi loại bài</option>
                <option value="QUESTION">Hỏi đáp</option>
                <option value="ARTICLE">Chia sẻ</option>
              </select>
            )}
            <label className="adm-search">
              <Icon name="search" size={16} />
              <input
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                maxLength={150}
                placeholder={tab === 'posts' ? 'Tìm tiêu đề, nội dung, tác giả…' : 'Tìm nội dung, bài viết, tác giả…'}
                aria-label="Tìm kiếm"
              />
              {draft && (
                <button type="button" aria-label="Xóa tìm kiếm" onClick={() => setDraft('')}>
                  <Icon name="close" size={14} />
                </button>
              )}
            </label>
            <IconAction icon="refresh" label="Làm mới" onClick={reloadAll} />
          </div>
        </div>

        {tab === 'comments' && postFilter && (
          <div className="adm-filter-pill">
            Bình luận của bài: <strong>{postFilter.title}</strong>
            <button type="button" aria-label="Bỏ lọc theo bài viết" onClick={() => switchTab('comments')}>
              <Icon name="close" size={13} />
            </button>
          </div>
        )}

        {tableContent}

        {current.data && current.data.totalElements > 0 && (
          <Pager
            page={page}
            totalPages={current.data.totalPages}
            total={current.data.totalElements}
            unit={tab === 'posts' ? 'bài viết' : 'bình luận'}
            disabled={current.loading}
            onPage={setPage}
          />
        )}
      </Card>

      <p className="adm-footnote">
        <Icon name="shield" size={14} />
        Nội dung bị ẩn không còn xuất hiện với thành viên nhưng vẫn được lưu lại và có thể khôi phục. Nội dung do tác
        giả tự xóa chỉ được xem.
      </p>

      {/* ===== Hộp thoại xem nhanh bài viết ===== */}
      {preview && (
        <AdminDialog
          wide
          title={preview.title}
          subtitle={
            <>
              {TYPE_NAMES[preview.type]} · {authorName(preview.author)} ·{' '}
              {shortDate(preview.publishedAt || preview.createdAt)}
            </>
          }
          onClose={() => setPreview(null)}
          footer={
            <>
              <a className="button secondary" href={`#post/${preview.id}`} target="_blank" rel="noreferrer">
                <Icon name="eye" size={16} />
                Mở trên bảng tin
              </a>
              {preview.status === 'PUBLISHED' && (
                <button
                  className="button danger"
                  onClick={() => setConfirm({ kind: 'post', item: preview, next: 'REJECTED' })}
                >
                  <Icon name="eyeOff" size={16} />
                  Ẩn bài viết
                </button>
              )}
              {preview.status === 'REJECTED' && (
                <button
                  className="button primary"
                  onClick={() => setConfirm({ kind: 'post', item: preview, next: 'PUBLISHED' })}
                >
                  <Icon name="refresh" size={16} />
                  Khôi phục
                </button>
              )}
            </>
          }
        >
          <div className="adm-post-preview">
            <div className="adm-post-meta">
              <span className={`adm-doc-status ${STATUS_CLASS[preview.status]}`}>{POST_STATUSES[preview.status]}</span>
              {preview.category && <span className="adm-chip">{preview.category}</span>}
              <span className="adm-muted">{preview.visibility === 'FOLLOWERS' ? 'Chỉ người theo dõi' : 'Công khai'}</span>
            </div>
            <p>
              {preview.excerpt}
              {preview.excerpt.length >= 240 && '…'}
            </p>
            {preview.tags.length > 0 && (
              <div className="adm-post-tags">
                {preview.tags.map((t) => (
                  <i key={t}>#{t}</i>
                ))}
              </div>
            )}
            <dl>
              <div>
                <dt>Lượt thích</dt>
                <dd>{preview.likes}</dd>
              </div>
              <div>
                <dt>Bình luận</dt>
                <dd>{preview.comments}</dd>
              </div>
              <div>
                <dt>Lượt xem</dt>
                <dd>{preview.views}</dd>
              </div>
            </dl>
          </div>
        </AdminDialog>
      )}

      {/* ===== Hộp thoại xác nhận ẩn / khôi phục ===== */}
      {confirm && (
        <AdminDialog
          title={confirmTitle}
          busy={busy}
          onClose={closeConfirm}
          footer={
            <>
              <button className="button secondary" disabled={busy} onClick={closeConfirm}>
                Hủy
              </button>
              <button className={isHiding ? 'button danger' : 'button primary'} disabled={busy} onClick={applyConfirm}>
                {busy ? 'Đang lưu…' : isHiding ? 'Ẩn nội dung' : 'Khôi phục'}
              </button>
            </>
          }
        >
          {actionError && <Notice tone="error">{actionError}</Notice>}
          <blockquote className="adm-confirm-quote">{confirmQuote}</blockquote>
          <p className="adm-muted">{confirmText}</p>
        </AdminDialog>
      )}
    </section>
  );
}
