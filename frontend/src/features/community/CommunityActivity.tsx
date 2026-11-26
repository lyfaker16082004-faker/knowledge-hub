import { useState } from 'react';
import { useRemote } from '../../shared/hooks/useRemote';
import Icon from '../../shared/components/ui/Icon';
import type { Account } from '../auth/types';
import type { PostPage } from '../post/types';
import { PostCard } from '../post/components/PostUI';
import { Pagination, RemoteState } from '../documents/components/LibraryUI';
import '../../styles/member-profile.css';

/*
 * Tổng hợp hoạt động cộng đồng của một thành viên:
 * - Các ô thống kê: câu hỏi, bài chia sẻ, câu trả lời, lượt thích / bình luận / lượt xem nhận được...
 * - Danh sách bài viết của thành viên (lọc Tất cả / Hỏi đáp / Chia sẻ)
 * Dùng ở trang Hồ sơ thành viên (#person/{id}) và tab "Bài viết cộng đồng" của trang Quản lý cá nhân.
 */

export type AuthorStats = {
  posts: number;
  questions: number;
  articles: number;
  answers: number;
  likesReceived: number;
  commentsReceived: number;
  views: number;
  helpfulVotes: number;
  documents: number;
};

const PAGE_SIZE = 6;

const TYPE_FILTERS: [string, string][] = [
  ['', 'Tất cả'],
  ['QUESTION', 'Hỏi đáp'],
  ['ARTICLE', 'Chia sẻ'],
];

type Props = {
  authorId: number;
  user: Account | null; // Người đang xem
  isMe: boolean; // Đang xem hoạt động của chính mình
};

/** Một ô thống kê. */
function StatTile({ icon, value, label, tone }: { icon: string; value?: number; label: string; tone: string }) {
  return (
    <div className={`mp-stat t-${tone}`}>
      <span className="mp-stat-icon" aria-hidden="true">
        <Icon name={icon} size={18} />
      </span>
      <span>
        <strong>{value === undefined ? '—' : value.toLocaleString('vi-VN')}</strong>
        <small>{label}</small>
      </span>
    </div>
  );
}

/** Các ô thống kê (dùng riêng được, ví dụ ở đầu trang hồ sơ). */
export function ActivityStats({ stats }: { stats: AuthorStats | null }) {
  const s = stats || undefined;
  return (
    <div className="mp-stats">
      <StatTile icon="question" value={s?.questions} label="Câu hỏi" tone="amber" />
      <StatTile icon="book" value={s?.articles} label="Bài chia sẻ" tone="green" />
      <StatTile icon="reply" value={s?.answers} label="Câu trả lời đã viết" tone="blue" />
      <StatTile icon="up" value={s?.helpfulVotes} label="Điểm hữu ích" tone="violet" />
      <StatTile icon="heart" value={s?.likesReceived} label="Lượt thích nhận được" tone="rose" />
      <StatTile icon="message" value={s?.commentsReceived} label="Bình luận nhận được" tone="blue" />
      <StatTile icon="eye" value={s?.views} label="Lượt xem bài viết" tone="slate" />
      <StatTile icon="upload" value={s?.documents} label="Tài liệu đã công bố" tone="green" />
    </div>
  );
}

export default function CommunityActivity({ authorId, user, isMe }: Props) {
  const [type, setType] = useState('');
  const [page, setPage] = useState(0);

  const stats = useRemote<AuthorStats>(`/posts/authors/${authorId}/stats`);
  const posts = useRemote<PostPage>(`/posts?author=${authorId}&type=${type}&page=${page}&size=${PAGE_SIZE}`);

  function changeType(next: string) {
    setType(next);
    setPage(0);
  }

  const items = posts.data?.content || [];

  return (
    <div className="mp-activity">
      <ActivityStats stats={stats.data} />

      <div className="mp-posts-head">
        <h2>{isMe ? 'Bài viết cộng đồng của bạn' : 'Bài viết cộng đồng'}</h2>
        <div className="mp-filter" role="group" aria-label="Loại bài viết">
          {TYPE_FILTERS.map(([value, label]) => (
            <button key={value} type="button" aria-pressed={type === value} onClick={() => changeType(value)}>
              {label}
            </button>
          ))}
        </div>
      </div>

      <RemoteState loading={posts.loading} error={posts.error} onRetry={posts.refresh} />

      {posts.data && items.length === 0 && (
        <div className="library-empty">
          <span className="tile-icon purple">
            <Icon name="message" size={28} />
          </span>
          <h2>{isMe ? 'Bạn chưa đăng bài nào' : 'Thành viên chưa đăng bài nào'}</h2>
          {isMe && (
            <>
              <p>Đặt câu hỏi hoặc chia sẻ điều bạn vừa học được với cộng đồng.</p>
              <a className="button primary" href="#community">
                <Icon name="plus" size={16} />
                Viết bài trên Cộng đồng
              </a>
            </>
          )}
        </div>
      )}

      {items.length > 0 && (
        <div className="mp-post-list">
          {items.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              signedIn={user !== null}
              onTag={(tag) => {
                location.hash = `community?tag=${encodeURIComponent(tag)}`;
              }}
            />
          ))}
        </div>
      )}

      {posts.data && posts.data.totalPages > 1 && <Pagination data={posts.data} page={page} onPage={setPage} />}
    </div>
  );
}
