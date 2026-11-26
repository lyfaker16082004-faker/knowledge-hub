import { useState } from 'react';
import type { FormEvent, KeyboardEvent } from 'react';
import Icon from '../../../shared/components/ui/Icon';
import { errorMessage, mutate } from '../../../shared/api/http-client';
import { useRemote } from '../../../shared/hooks/useRemote';
import { AdminDialog } from '../../admin/components/AdminUI';
import type { Counter, PostAuthor, PostDetail, PostSummary, PostType } from '../types';
import MarkdownContent from '../../../shared/components/markdown/MarkdownContent';
import CategoryPicker from './CategoryPicker';
import type { CategoryChoice } from './CategoryPicker';
import MarkdownEditor from './MarkdownEditor';
import '../../../styles/community-feed.css';
import '../../../styles/post-editor.css';

/*
 * Các thành phần dùng chung cho Bảng tin cộng đồng & Hỏi đáp:
 * ảnh đại diện, nhãn loại bài, thẻ bài viết, menu sửa/xoá, ô nhập thẻ, hộp soạn bài.
 */

const MAX_TAGS = 5;

// =========================================================
// PHẦN 1: HÀM HỖ TRỢ
// =========================================================

/** Đổi thời gian thành dạng "5 phút trước", "2 ngày trước"... */
export function timeAgo(value: string) {
  const hasTimezone = /[Z+]/.test(value);
  const date = new Date(hasTimezone ? value : value + 'Z');

  const minutes = Math.max(0, Math.round((Date.now() - date.getTime()) / 60000));
  if (minutes < 1) return 'Vừa xong';
  if (minutes < 60) return `${minutes} phút trước`;

  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;

  const days = Math.round(hours / 24);
  if (days < 30) return `${days} ngày trước`;

  return date.toLocaleDateString('vi-VN');
}

/** Tên hiển thị của tác giả (chưa đặt thì dùng tên đăng nhập). */
export function authorName(author: Pick<PostAuthor, 'displayName' | 'username'>) {
  return author.displayName || author.username;
}

/** Chuẩn hoá thẻ: bỏ dấu #, gộp khoảng trắng, chữ thường, tối đa 30 ký tự. */
function normalizeTag(raw: string) {
  return raw.trim().replace(/^#+/, '').replace(/\s+/g, ' ').toLowerCase().slice(0, 30);
}

function goLogin() {
  location.hash = 'login';
}

// =========================================================
// PHẦN 2: ẢNH ĐẠI DIỆN VÀ NHÃN
// =========================================================

// Các màu nền cho ảnh đại diện chưa có ảnh
const AVATAR_TONES = ['t0', 't1', 't2', 't3', 't4', 't5'];

type UserAvatarProps = {
  name: string;
  url?: string | null;
  id?: number;
  size?: 'sm' | 'md' | 'lg';
};

/** Ảnh đại diện. Chưa có ảnh thì hiện chữ cái đầu của 2 từ cuối trong tên. */
export function UserAvatar({ name, url, id, size = 'md' }: UserAvatarProps) {
  const words = name.split(/\s+/).filter((w) => w !== '');
  const lastTwo = words.slice(-2);
  const initials = lastTwo.map((w) => w[0]).join('').toUpperCase() || '?';
  const tone = AVATAR_TONES[(id || name.length) % AVATAR_TONES.length];

  return (
    <span className={`cf-avatar ${size} ${tone}`} aria-hidden="true">
      {url ? <img src={url} alt="" /> : initials}
    </span>
  );
}

/** Nhãn loại bài: Hỏi đáp hoặc Chia sẻ kiến thức. */
export function TypeBadge({ type }: { type: PostType }) {
  if (type === 'QUESTION') {
    return <span className="cf-badge question">Hỏi đáp</span>;
  }
  return <span className="cf-badge article">Chia sẻ kiến thức</span>;
}

/** Nội dung bài viết dạng Markdown (đậm, nghiêng, code, danh sách, ảnh...). Bài cũ dạng chữ thường vẫn hiển thị đúng. */
export function PostContent({ text }: { text: string }) {
  return <MarkdownContent text={text} className="cf-content md-content" />;
}

// =========================================================
// PHẦN 3: THÍCH / LƯU / CHIA SẺ
// =========================================================

/**
 * Hook xử lý nút Thích và Lưu.
 * Đổi giao diện ngay khi bấm (không chờ server), nếu server báo lỗi thì trả lại như cũ.
 */
export function usePostActions(post: PostSummary, signedIn: boolean) {
  const [liked, setLiked] = useState(post.liked);
  const [likes, setLikes] = useState(post.likes);
  const [saved, setSaved] = useState(post.saved);
  const [error, setError] = useState('');

  async function toggleLike() {
    if (!signedIn) {
      goLogin();
      return;
    }
    setError('');
    const next = !liked;

    // Đổi ngay trên giao diện
    setLiked(next);
    setLikes((n) => (next ? n + 1 : n - 1));

    try {
      const result = await mutate<Counter>(`/posts/${post.id}/like`, 'POST', { active: next });
      setLiked(result.active);
      setLikes(result.count);
    } catch (e) {
      // Lỗi -> trả lại trạng thái cũ
      setLiked(!next);
      setLikes((n) => (next ? n - 1 : n + 1));
      setError(errorMessage(e));
    }
  }

  async function toggleBookmark() {
    if (!signedIn) {
      goLogin();
      return;
    }
    setError('');
    const next = !saved;
    setSaved(next);

    try {
      const result = await mutate<Counter>(`/posts/${post.id}/bookmark`, 'POST', { active: next });
      setSaved(result.active);
    } catch (e) {
      setSaved(!next);
      setError(errorMessage(e));
    }
  }

  return { liked, likes, saved, error, like: toggleLike, bookmark: toggleBookmark };
}

/**
 * Chia sẻ bài viết: dùng hộp chia sẻ của điện thoại nếu có,
 * nếu không thì sao chép liên kết. Trả về câu thông báo (hoặc chuỗi rỗng).
 */
export async function sharePost(id: number, title: string) {
  const url = `${location.origin}${location.pathname}#post/${id}`;
  try {
    if (navigator.share) {
      await navigator.share({ title, url });
    } else {
      await navigator.clipboard.writeText(url);
      return 'Đã sao chép liên kết bài viết';
    }
  } catch {
    // Người dùng đóng hộp chia sẻ -> bỏ qua
  }
  return '';
}

// =========================================================
// PHẦN 4: THẺ BÀI VIẾT TRÊN BẢNG TIN
// =========================================================

type PostCardProps = {
  post: PostSummary;
  signedIn: boolean;
  onTag: (tag: string) => void; // Bấm vào thẻ -> lọc theo thẻ
  onEdit?: (post: PostSummary) => void;
  onDelete?: (post: PostSummary) => void;
};

export function PostCard({ post, signedIn, onTag, onEdit, onDelete }: PostCardProps) {
  const actions = usePostActions(post, signedIn);
  const [hint, setHint] = useState('');
  const name = authorName(post.author);

  // Câu hỏi chưa ai trả lời: làm nổi bật để mọi người vào giúp
  const waiting = post.type === 'QUESTION' && post.comments === 0;

  async function share() {
    setHint(await sharePost(post.id, post.title));
    setTimeout(() => setHint(''), 2500);
  }

  return (
    <article className={waiting ? 'cf-card is-waiting' : 'cf-card'}>
      <header className="cf-card-head">
        <a href={`#person/${post.author.id}`} className="cf-author">
          <UserAvatar name={name} url={post.author.avatarUrl} id={post.author.id} />
          <span>
            <strong>{name}</strong>
            <small>
              {timeAgo(post.publishedAt)}
              {post.category && <> · {post.category}</>}
            </small>
          </span>
        </a>

        {waiting ? <span className="cf-badge waiting">Chưa có trả lời</span> : <TypeBadge type={post.type} />}

        {post.mine && onEdit && onDelete && <OwnerMenu onEdit={() => onEdit(post)} onDelete={() => onDelete(post)} />}
      </header>

      <a className="cf-card-title" href={`#post/${post.id}`}>
        <h2>{post.title}</h2>
      </a>

      {post.excerpt && <p className="cf-excerpt">{post.excerpt}</p>}

      {post.tags.length > 0 && (
        <div className="cf-tags">
          {post.tags.map((tag) => (
            <button key={tag} type="button" onClick={() => onTag(tag)}>
              #{tag}
            </button>
          ))}
        </div>
      )}

      <footer className="cf-actions">
        <button
          type="button"
          className={actions.liked ? 'on like' : 'like'}
          aria-pressed={actions.liked}
          onClick={actions.like}
          aria-label={actions.liked ? 'Bỏ thích' : 'Thích'}
        >
          <Icon name="heart" size={17} />
          {actions.likes}
        </button>

        {waiting ? (
          <a className="cf-answer-cta" href={`#post/${post.id}`}>
            <Icon name="reply" size={16} />
            Trả lời đầu tiên
          </a>
        ) : (
          <a href={`#post/${post.id}`} aria-label={`${post.comments} bình luận`}>
            <Icon name="message" size={17} />
            {post.comments}
            <span className="cf-hide-sm">{post.type === 'QUESTION' ? ' trả lời' : ' bình luận'}</span>
          </a>
        )}

        <span className="cf-stat" title="Lượt xem">
          <Icon name="eye" size={17} />
          {post.views}
        </span>

        <button
          type="button"
          className={actions.saved ? 'on save' : 'save'}
          aria-pressed={actions.saved}
          onClick={actions.bookmark}
        >
          <Icon name="bookmark" size={17} />
          {actions.saved ? 'Đã lưu' : 'Lưu'}
        </button>

        <button type="button" aria-label="Chia sẻ" title="Chia sẻ liên kết" onClick={share}>
          <Icon name="share" size={17} />
        </button>

        {(hint || actions.error) && (
          <span className={actions.error ? 'cf-inline-error' : 'cf-hint'} role="status">
            {actions.error || hint}
          </span>
        )}
      </footer>
    </article>
  );
}

// =========================================================
// PHẦN 5: MENU SỬA / XOÁ
// =========================================================

type OwnerMenuProps = {
  onEdit?: () => void;
  onDelete: () => void;
  label?: string; // "bài viết", "bình luận"...
};

/** Nút ba chấm, bấm vào hiện menu Chỉnh sửa / Xóa. */
export function OwnerMenu({ onEdit, onDelete, label = 'bài viết' }: OwnerMenuProps) {
  const [open, setOpen] = useState(false);

  return (
    <div
      className="cf-menu"
      onBlur={(e) => {
        // Bấm ra ngoài menu -> đóng menu
        if (!e.currentTarget.contains(e.relatedTarget)) {
          setOpen(false);
        }
      }}
    >
      <button
        type="button"
        className="cf-icon-btn"
        aria-label={`Tùy chọn ${label}`}
        aria-expanded={open}
        onClick={() => setOpen(!open)}
      >
        <Icon name="more" size={20} />
      </button>

      {open && (
        <div className="cf-menu-list" role="menu">
          {onEdit && (
            <button
              type="button"
              role="menuitem"
              onClick={() => {
                setOpen(false);
                onEdit();
              }}
            >
              <Icon name="edit" size={16} />
              Chỉnh sửa
            </button>
          )}
          <button
            type="button"
            role="menuitem"
            className="danger"
            onClick={() => {
              setOpen(false);
              onDelete();
            }}
          >
            <Icon name="trash" size={16} />
            Xóa {label}
          </button>
        </div>
      )}
    </div>
  );
}

// =========================================================
// PHẦN 6: Ô NHẬP THẺ (TAGS)
// =========================================================

function TagInput({ value, onChange }: { value: string[]; onChange: (tags: string[]) => void }) {
  const [draft, setDraft] = useState('');

  function add(raw: string) {
    const tag = normalizeTag(raw);
    if (tag && !value.includes(tag) && value.length < MAX_TAGS) {
      onChange([...value, tag]);
    }
    setDraft('');
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    // Enter hoặc dấu phẩy -> thêm thẻ
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      add(draft);
    } else if (event.key === 'Backspace' && !draft && value.length > 0) {
      // Ô trống mà bấm xoá -> bỏ thẻ cuối cùng
      onChange(value.slice(0, -1));
    }
  }

  return (
    <div className="cf-tag-input">
      {value.map((tag) => (
        <span key={tag}>
          #{tag}
          <button type="button" aria-label={`Bỏ thẻ ${tag}`} onClick={() => onChange(value.filter((t) => t !== tag))}>
            <Icon name="close" size={13} />
          </button>
        </span>
      ))}

      {value.length < MAX_TAGS && (
        <input
          id="post-tags"
          value={draft}
          maxLength={30}
          onChange={(e) => setDraft(e.target.value.replace(',', ''))}
          onKeyDown={onKeyDown}
          onBlur={() => {
            if (draft) add(draft);
          }}
          placeholder={value.length > 0 ? 'Thêm thẻ…' : 'VD: java, spring boot (Enter để thêm)'}
        />
      )}
    </div>
  );
}

// =========================================================
// PHẦN 7: HỘP SOẠN BÀI VIẾT
// =========================================================

// Hai loại bài: [giá trị, icon, tên, mô tả]
const POST_TYPES: [PostType, string, string, string][] = [
  ['QUESTION', 'question', 'Đặt câu hỏi', 'Nhờ cộng đồng giải đáp'],
  ['ARTICLE', 'book', 'Chia sẻ kiến thức', 'Bài viết, kinh nghiệm, hướng dẫn'],
];

type PostEditorProps = {
  initial?: PostDetail; // Có giá trị -> đang sửa bài cũ
  defaultType?: PostType;
  defaultCategory?: number | null;
  onClose: () => void;
  onSaved: (post: PostDetail) => void;
};

export function PostEditor({ initial, defaultType, defaultCategory, onClose, onSaved }: PostEditorProps) {
  // Lấy chủ đề kèm số bài viết (từ sidebar) để ô chọn chủ đề xếp được mục "Phổ biến"
  const sidebar = useRemote<{ categories: CategoryChoice[] }>('/posts/sidebar');

  const [type, setType] = useState<PostType>(initial?.post.type || defaultType || 'QUESTION');
  const [title, setTitle] = useState(initial?.post.title || '');
  const [content, setContent] = useState(initial?.content || '');
  const [categoryId, setCategoryId] = useState(String(initial?.post.categoryId || defaultCategory || ''));
  const [tags, setTags] = useState<string[]>(initial?.post.tags || []);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function submit(event: FormEvent) {
    event.preventDefault();

    // Kiểm tra dữ liệu trước khi gửi
    if (title.trim().length < 10) {
      setError('Tiêu đề cần ít nhất 10 ký tự.');
      return;
    }
    if (content.trim().length < 20) {
      setError('Nội dung cần ít nhất 20 ký tự để mọi người hiểu rõ.');
      return;
    }
    if (!categoryId) {
      setError('Hãy chọn chủ đề cho bài viết.');
      return;
    }

    setBusy(true);
    setError('');
    try {
      const body = { type, title, content, categoryId: Number(categoryId), tags, version: initial?.version };
      let saved: PostDetail;
      if (initial) {
        saved = await mutate<PostDetail>(`/posts/${initial.post.id}`, 'PUT', body);
      } else {
        saved = await mutate<PostDetail>('/posts', 'POST', body);
      }
      onSaved(saved);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  /** Thêm từ khoá vào danh sách thẻ (dùng khi không tìm thấy chủ đề phù hợp). */
  function addTag(raw: string) {
    const tag = normalizeTag(raw);
    if (tag && !tags.includes(tag) && tags.length < MAX_TAGS) {
      setTags([...tags, tag]);
    }
  }

  let dialogTitle = 'Chia sẻ kiến thức';
  if (initial) dialogTitle = 'Chỉnh sửa bài viết';
  else if (type === 'QUESTION') dialogTitle = 'Đặt câu hỏi';

  let submitLabel = 'Đăng bài';
  if (busy) submitLabel = 'Đang lưu…';
  else if (initial) submitLabel = 'Lưu thay đổi';

  const footer = (
    <>
      <button type="button" className="button secondary" onClick={onClose} disabled={busy}>
        Hủy
      </button>
      <button className="button primary" form="post-editor" disabled={busy}>
        {submitLabel}
      </button>
    </>
  );

  return (
    <AdminDialog
      wide
      title={dialogTitle}
      subtitle="Bài viết hiển thị ngay trên Cộng đồng sau khi đăng."
      onClose={onClose}
      busy={busy}
      footer={footer}
    >
      <form id="post-editor" className="cf-editor" onSubmit={submit} noValidate>
        {/* Chọn loại bài */}
        <div className="cf-type-switch" role="radiogroup" aria-label="Loại bài viết">
          {POST_TYPES.map(([value, icon, label, hint]) => (
            <button
              type="button"
              key={value}
              role="radio"
              aria-checked={type === value}
              className={type === value ? 'on' : ''}
              onClick={() => setType(value)}
            >
              <Icon name={icon} size={18} />
              <span>
                <strong>{label}</strong>
                <small>{hint}</small>
              </span>
            </button>
          ))}
        </div>

        <label htmlFor="post-title">Tiêu đề</label>
        <input
          id="post-title"
          value={title}
          maxLength={200}
          onChange={(e) => setTitle(e.target.value)}
          placeholder={
            type === 'QUESTION'
              ? 'VD: Khi nào nên dùng ArrayList thay vì LinkedList?'
              : 'VD: Hướng dẫn xây dựng REST API với Spring Boot'
          }
          autoFocus
        />

        <label htmlFor="post-category">Chủ đề</label>
        <CategoryPicker
          categories={sidebar.data?.categories || []}
          value={categoryId}
          onChange={setCategoryId}
          onUseAsTag={addTag}
        />

        <label htmlFor="post-content">Nội dung</label>
        <MarkdownEditor
          id="post-content"
          value={content}
          onChange={setContent}
          maxLength={20000}
          placeholder={
            type === 'QUESTION'
              ? 'Mô tả vấn đề: bạn đang làm gì, lỗi hiện ra thế nào, bạn đã thử những gì.\n\nDán ảnh chụp màn hình lỗi bằng Ctrl+V, dán code giữa hai dòng ```'
              : 'Nội dung chia sẻ của bạn. Có thể dán ảnh (Ctrl+V) và code giữa hai dòng ```'
          }
        />

        <label htmlFor="post-tags">
          Thẻ <span className="cf-optional">(tối đa 5)</span>
        </label>
        <TagInput value={tags} onChange={setTags} />

        {error && (
          <p className="cf-form-error" role="alert">
            {error}
          </p>
        )}
      </form>
    </AdminDialog>
  );
}
