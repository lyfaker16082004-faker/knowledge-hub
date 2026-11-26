import { useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import Icon from '../../../shared/components/ui/Icon';
import { errorMessage, mutate } from '../../../shared/api/http-client';
import type { Account } from '../../auth/types';
import type { CommentItem } from '../types';
import { OwnerMenu, UserAvatar, authorName, timeAgo } from './PostUI';

/*
 * Khu vực bình luận / câu trả lời nhiều cấp (giống Facebook, Reddit).
 * - Trả lời bài viết hoặc trả lời một bình luận khác
 * - Bình chọn Hữu ích / Không hữu ích
 * - Sửa, xoá, thu gọn chuỗi trả lời
 * - Sắp xếp: Hữu ích nhất hoặc Mới nhất
 */

// Từ cấp thứ 3 trở đi không thụt lề thêm nữa, để chuỗi dài vẫn dễ đọc trên điện thoại
const MAX_DEPTH = 3;

// =========================================================
// PHẦN 1: Ô NHẬP BÌNH LUẬN
// =========================================================

type ComposerProps = {
  user: Account;
  placeholder: string;
  initial?: string;
  submitLabel: string;
  autoFocus?: boolean;
  onSubmit: (text: string) => Promise<void>;
  onCancel?: () => void; // Có nút Hủy (khi trả lời hoặc sửa)
};

function Composer({ user, placeholder, initial = '', submitLabel, autoFocus, onSubmit, onCancel }: ComposerProps) {
  const [text, setText] = useState(initial);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function submit(event?: FormEvent) {
    event?.preventDefault();
    if (!text.trim()) {
      return;
    }
    setBusy(true);
    setError('');
    try {
      await onSubmit(text.trim());
      setText('');
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="cf-composer" onSubmit={submit}>
      <UserAvatar name={user.displayName || user.username} url={user.avatarUrl} id={user.id} size="sm" />
      <div>
        <textarea
          value={text}
          rows={onCancel ? 2 : 3}
          maxLength={5000}
          placeholder={placeholder}
          autoFocus={autoFocus}
          aria-label={placeholder}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => {
            // Ctrl + Enter để gửi, Esc để hủy
            if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
              submit();
            }
            if (e.key === 'Escape' && onCancel) {
              onCancel();
            }
          }}
        />
        {error && (
          <p className="cf-inline-error" role="alert">
            {error}
          </p>
        )}
        <div className="cf-composer-actions">
          <small>Ctrl + Enter để gửi</small>
          {onCancel && (
            <button type="button" className="button secondary compact" onClick={onCancel} disabled={busy}>
              Hủy
            </button>
          )}
          <button className="button primary compact" disabled={busy || !text.trim()}>
            {busy ? 'Đang gửi…' : submitLabel}
          </button>
        </div>
      </div>
    </form>
  );
}

// =========================================================
// PHẦN 2: NÚT BÌNH CHỌN
// =========================================================

type VoteProps = {
  item: CommentItem;
  user: Account | null;
  onChange: (patch: Partial<CommentItem>) => void;
};

function Vote({ item, user, onChange }: VoteProps) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function vote(value: number) {
    if (!user) {
      location.hash = 'login';
      return;
    }
    // Bấm lại nút đang chọn -> huỷ bình chọn (gửi 0)
    const next = item.myVote === value ? 0 : value;

    setBusy(true);
    setError('');
    try {
      const result = await mutate<{ myVote: number; score: number }>(`/comments/${item.id}/vote`, 'POST', {
        value: next,
      });
      onChange(result);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  // Không được tự bình chọn cho bình luận của mình
  const disabled = busy || item.mine;

  let scoreClass = '';
  if (item.score > 0) scoreClass = 'pos';
  else if (item.score < 0) scoreClass = 'neg';

  return (
    <div className="cf-vote" title={item.mine ? 'Bạn không thể bình chọn cho bình luận của mình' : undefined}>
      <button
        type="button"
        className={item.myVote === 1 ? 'on up' : 'up'}
        aria-pressed={item.myVote === 1}
        aria-label="Hữu ích"
        disabled={disabled}
        onClick={() => vote(1)}
      >
        <Icon name="up" size={18} />
      </button>
      <span className={scoreClass} aria-label={`Điểm ${item.score}`}>
        {item.score}
      </span>
      <button
        type="button"
        className={item.myVote === -1 ? 'on down' : 'down'}
        aria-pressed={item.myVote === -1}
        aria-label="Không hữu ích"
        disabled={disabled}
        onClick={() => vote(-1)}
      >
        <Icon name="down" size={18} />
      </button>
      {error && (
        <span className="cf-inline-error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}

// =========================================================
// PHẦN 3: DANH SÁCH BÌNH LUẬN
// =========================================================

type Props = {
  postId: number;
  user: Account | null;
  initial: CommentItem[];
  isQuestion: boolean; // Câu hỏi thì gọi là "câu trả lời", bài viết thì gọi là "bình luận"
  onCount: (count: number) => void; // Báo số bình luận mới cho trang cha
};

export default function CommentThread({ postId, user, initial, isQuestion, onCount }: Props) {
  const [items, setItems] = useState(initial);
  const [sort, setSort] = useState<'top' | 'new'>('top');
  const [replyTo, setReplyTo] = useState<number | null>(null); // Đang trả lời bình luận nào
  const [editing, setEditing] = useState<number | null>(null); // Đang sửa bình luận nào
  const [collapsed, setCollapsed] = useState<Set<number>>(new Set()); // Các bình luận đã thu gọn
  const [error, setError] = useState('');

  const noun = isQuestion ? 'câu trả lời' : 'bình luận';
  const nounCapitalized = noun[0].toUpperCase() + noun.slice(1);

  // Gom bình luận theo bình luận cha: parentId -> danh sách con
  const children = useMemo(() => {
    const map = new Map<number | null, CommentItem[]>();
    for (const item of items) {
      const list = map.get(item.parentId) || [];
      list.push(item);
      map.set(item.parentId, list);
    }
    return map;
  }, [items]);

  // Bình luận gốc (trả lời trực tiếp bài viết), đã sắp xếp
  const roots = useMemo(() => {
    const list = [...(children.get(null) || [])];
    list.sort((a, b) => {
      if (sort === 'new') {
        return b.createdAt.localeCompare(a.createdAt);
      }
      // Hữu ích nhất: điểm cao trước, bằng điểm thì cũ trước
      if (b.score !== a.score) {
        return b.score - a.score;
      }
      return a.createdAt.localeCompare(b.createdAt);
    });
    return list;
  }, [children, sort]);

  const visibleCount = items.filter((item) => !item.deleted).length;

  /** Thay cả danh sách và báo số lượng mới cho trang cha. */
  function updateList(next: CommentItem[]) {
    setItems(next);
    onCount(next.filter((item) => !item.deleted).length);
  }

  /** Sửa một vài trường của một bình luận. */
  function patch(id: number, value: Partial<CommentItem>) {
    setItems((list) => list.map((item) => (item.id === id ? { ...item, ...value } : item)));
  }

  function toggleCollapsed(id: number) {
    setCollapsed((old) => {
      const next = new Set(old);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  /** Thêm bình luận mới. parentId = null: trả lời bài viết. */
  async function add(text: string, parentId: number | null) {
    const created = await mutate<CommentItem>(`/posts/${postId}/comments`, 'POST', { content: text, parentId });
    updateList([...items, created]);
    setReplyTo(null);

    // Mở chuỗi trả lời ra để người dùng thấy bình luận vừa gửi
    if (parentId) {
      setCollapsed((old) => {
        const next = new Set(old);
        next.delete(parentId);
        return next;
      });
    }
  }

  async function edit(id: number, text: string) {
    const saved = await mutate<CommentItem>(`/comments/${id}`, 'PUT', { content: text });
    patch(id, saved);
    setEditing(null);
  }

  async function remove(item: CommentItem) {
    if (!confirm(`Xóa ${noun} này?`)) {
      return;
    }
    setError('');
    try {
      await mutate(`/comments/${item.id}`, 'DELETE');

      const hasReplies = (children.get(item.id) || []).length > 0;
      if (hasReplies) {
        // Còn câu trả lời con -> giữ lại một "chỗ trống" để chuỗi không bị đứt
        updateList(
          items.map((c) =>
            c.id === item.id ? { ...c, deleted: true, content: null, author: null, mine: false, canDelete: false } : c,
          ),
        );
      } else {
        updateList(items.filter((c) => c.id !== item.id));
      }
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  /** Đếm tổng số phản hồi bên dưới một bình luận (kể cả các cấp sâu hơn). */
  function countReplies(id: number): number {
    let total = 0;
    for (const child of children.get(id) || []) {
      total += 1 + countReplies(child.id);
    }
    return total;
  }

  /** Vẽ một bình luận và các câu trả lời bên dưới nó (gọi đệ quy). */
  function renderComment(item: CommentItem, depth: number) {
    const replies = children.get(item.id) || [];
    const isCollapsed = collapsed.has(item.id);
    const name = item.author ? authorName(item.author) : '';

    // Phần nội dung chính của bình luận
    let body;
    if (item.deleted || !item.author) {
      body = <p className="cf-deleted">{nounCapitalized} đã bị xóa</p>;
    } else {
      const author = item.author;
      body = (
        <>
          <div className="cf-comment-meta">
            <a href={`#person/${author.id}`}>
              <strong>{name}</strong>
            </a>
            <span>
              {timeAgo(item.createdAt)}
              {item.editedAt && ' · đã sửa'}
            </span>
            {item.canDelete && (
              <OwnerMenu
                label={noun}
                onEdit={item.mine ? () => setEditing(item.id) : undefined}
                onDelete={() => remove(item)}
              />
            )}
          </div>

          {editing === item.id && user ? (
            <Composer
              user={user}
              initial={item.content || ''}
              placeholder={`Sửa ${noun}`}
              submitLabel="Lưu"
              autoFocus
              onSubmit={(text) => edit(item.id, text)}
              onCancel={() => setEditing(null)}
            />
          ) : (
            <p className="cf-comment-text">{item.content}</p>
          )}

          <div className="cf-comment-actions">
            <Vote item={item} user={user} onChange={(value) => patch(item.id, value)} />

            <button
              type="button"
              onClick={() => {
                if (!user) {
                  location.hash = 'login';
                  return;
                }
                setReplyTo(replyTo === item.id ? null : item.id);
              }}
            >
              <Icon name="reply" size={16} />
              Trả lời
            </button>

            {replies.length > 0 && (
              <button type="button" onClick={() => toggleCollapsed(item.id)} aria-expanded={!isCollapsed}>
                <Icon name={isCollapsed ? 'down' : 'up'} size={16} />
                {isCollapsed ? `Xem ${countReplies(item.id)} phản hồi` : 'Thu gọn'}
              </button>
            )}
          </div>
        </>
      );
    }

    // Danh sách câu trả lời con
    let replyList = null;
    if (replies.length > 0 && !isCollapsed) {
      if (depth + 1 < MAX_DEPTH) {
        replyList = <ul className="cf-replies">{replies.map((reply) => renderComment(reply, depth + 1))}</ul>;
      } else {
        // Đã đủ sâu -> không thụt lề thêm
        replyList = <ul className="cf-replies flat">{replies.map((reply) => renderComment(reply, MAX_DEPTH))}</ul>;
      }
    }

    return (
      <li key={item.id} className={depth > 0 ? 'cf-comment reply' : 'cf-comment'}>
        <div className="cf-comment-row">
          {item.deleted || !item.author ? (
            <span className="cf-avatar sm ghost" aria-hidden="true">
              <Icon name="trash" size={14} />
            </span>
          ) : (
            <a href={`#person/${item.author.id}`} aria-label={name}>
              <UserAvatar name={name} url={item.author.avatarUrl} id={item.author.id} size="sm" />
            </a>
          )}

          <div className="cf-comment-body">
            {body}
            {replyTo === item.id && user && (
              <Composer
                user={user}
                autoFocus
                placeholder={`Trả lời ${name}…`}
                submitLabel="Trả lời"
                onSubmit={(text) => add(text, item.id)}
                onCancel={() => setReplyTo(null)}
              />
            )}
          </div>
        </div>
        {replyList}
      </li>
    );
  }

  return (
    <section className="cf-panel cf-comments" aria-labelledby="comments-title">
      <div className="cf-comments-head">
        <h2 id="comments-title">
          {visibleCount} {noun}
        </h2>
        {roots.length > 1 && (
          <div className="cf-segment" role="group" aria-label="Sắp xếp">
            <button
              type="button"
              aria-pressed={sort === 'top'}
              className={sort === 'top' ? 'on' : ''}
              onClick={() => setSort('top')}
            >
              Hữu ích nhất
            </button>
            <button
              type="button"
              aria-pressed={sort === 'new'}
              className={sort === 'new' ? 'on' : ''}
              onClick={() => setSort('new')}
            >
              Mới nhất
            </button>
          </div>
        )}
      </div>

      {user ? (
        <Composer
          user={user}
          placeholder={isQuestion ? 'Viết câu trả lời của bạn…' : 'Viết bình luận…'}
          submitLabel={isQuestion ? 'Gửi trả lời' : 'Bình luận'}
          onSubmit={(text) => add(text, null)}
        />
      ) : (
        <div className="cf-login-hint">
          <Icon name="message" size={18} />
          <span>Đăng nhập để tham gia thảo luận.</span>
          <a className="button primary compact" href="#login">
            Đăng nhập
          </a>
        </div>
      )}

      {error && (
        <p className="cf-inline-error" role="alert">
          {error}
        </p>
      )}

      {roots.length > 0 ? (
        <ul className="cf-thread">{roots.map((item) => renderComment(item, 0))}</ul>
      ) : (
        <div className="cf-empty small">
          <Icon name="message" size={26} />
          <p>{isQuestion ? 'Chưa có câu trả lời. Hãy là người đầu tiên giúp bạn ấy!' : 'Chưa có bình luận nào.'}</p>
        </div>
      )}
    </section>
  );
}
