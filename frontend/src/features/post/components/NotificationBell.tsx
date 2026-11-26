import { useEffect, useRef, useState } from 'react';
import Icon from '../../../shared/components/ui/Icon';
import { useRemote } from '../../../shared/hooks/useRemote';
import { mutate } from '../../../shared/api/http-client';
import { UserAvatar, authorName, timeAgo } from './PostUI';
import type { PostAuthor } from '../types';

/*
 * Chuông thông báo trên trang Cộng đồng.
 * Báo khi có người bình luận bài viết của mình hoặc trả lời bình luận của mình.
 * - Số màu đỏ: số thông báo chưa đọc
 * - Bấm chuông: mở danh sách, đồng thời đánh dấu đã xem
 * - Tự kiểm tra thông báo mới mỗi 60 giây
 */

type Notification = {
  commentId: number;
  postId: number;
  postTitle: string;
  kind: 'COMMENT' | 'REPLY';
  actor: PostAuthor;
  excerpt: string;
  createdAt: string;
  unread: boolean;
};

type NotificationPage = { unread: number; items: Notification[] };

const REFRESH_MS = 60_000;

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [tick, setTick] = useState(0);
  const box = useRef<HTMLDivElement>(null);
  const data = useRemote<NotificationPage>(`/users/me/notifications?size=20&_=${tick}`);
  const unread = data.data?.unread || 0;

  // Kiểm tra thông báo mới định kỳ (khi danh sách đang đóng)
  useEffect(() => {
    if (open) {
      return;
    }
    const timer = setInterval(() => setTick((n) => n + 1), REFRESH_MS);
    return () => clearInterval(timer);
  }, [open]);

  // Bấm ra ngoài hoặc nhấn Esc thì đóng
  useEffect(() => {
    if (!open) {
      return;
    }
    function onClick(event: MouseEvent) {
      if (!box.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  async function toggle() {
    const next = !open;
    setOpen(next);
    // Mở danh sách -> báo server là đã xem (vẫn giữ chấm "mới" trong lần mở này)
    if (next && unread > 0) {
      try {
        await mutate('/users/me/notifications/seen', 'POST');
      } catch {
        // Lỗi đánh dấu đã xem không ảnh hưởng việc đọc thông báo
      }
    }
  }

  /** Đóng danh sách thì tải lại để số chưa đọc về 0. */
  function close() {
    setOpen(false);
    setTick((n) => n + 1);
  }

  const items = data.data?.items || [];

  return (
    <div className="nb" ref={box}>
      <button
        type="button"
        className={open ? 'nb-button on' : 'nb-button'}
        aria-label={unread > 0 ? `Thông báo, ${unread} chưa đọc` : 'Thông báo'}
        aria-expanded={open}
        onClick={toggle}
      >
        <Icon name="bell" size={20} />
        {unread > 0 && <span className="nb-count">{unread > 9 ? '9+' : unread}</span>}
      </button>

      {open && (
        <div className="nb-panel" role="dialog" aria-label="Thông báo">
          <div className="nb-head">
            <strong>Thông báo</strong>
            <button type="button" className="icon-button" aria-label="Đóng" onClick={close}>
              <Icon name="close" size={16} />
            </button>
          </div>

          {data.loading && !data.data && <p className="nb-empty">Đang tải…</p>}
          {data.error && <p className="nb-empty">{data.error}</p>}
          {data.data && items.length === 0 && (
            <p className="nb-empty">
              <Icon name="message" size={22} />
              Chưa có ai bình luận bài viết của bạn.
            </p>
          )}

          <ul className="nb-list">
            {items.map((item) => {
              const name = authorName(item.actor);
              return (
                <li key={item.commentId} className={item.unread ? 'is-unread' : ''}>
                  <a href={`#post/${item.postId}`} onClick={close}>
                    <UserAvatar name={name} url={item.actor.avatarUrl} id={item.actor.id} size="sm" />
                    <span className="nb-text">
                      <span>
                        <strong>{name}</strong>
                        {item.kind === 'REPLY' ? ' đã trả lời bình luận của bạn trong ' : ' đã bình luận bài viết '}
                        <strong>{item.postTitle}</strong>
                      </span>
                      <q>{item.excerpt}</q>
                      <small>{timeAgo(item.createdAt)}</small>
                    </span>
                    {item.unread && <span className="nb-dot" aria-label="Mới" />}
                  </a>
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
}
