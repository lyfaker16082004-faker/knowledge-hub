import { useState } from 'react';
import { useRemote } from '../../../shared/hooks/useRemote';
import { mutate, errorMessage } from '../../../shared/api/http-client';
import Icon from '../../../shared/components/ui/Icon';
import { Pagination, RemoteState } from '../../documents/components/LibraryUI';

/*
 * Tab "Theo dõi" trong trang Quản lý cá nhân:
 * - Đang theo dõi: những người mình theo dõi (có nút Bỏ theo dõi)
 * - Người theo dõi: những người đang theo dõi mình
 */

export type FollowMember = {
  id: number;
  username: string;
  displayName: string | null;
  bio: string | null;
  avatarUrl: string | null;
  institutionName: string | null;
  followerCount: number;
  followingCount: number;
  following: boolean;
};

type MemberPage = { content: FollowMember[]; totalElements: number; totalPages: number };

export type FollowMode = 'following' | 'followers';

type Props = {
  mode: FollowMode;
  onModeChange: (mode: FollowMode) => void;
  onChanged: () => void; // Gọi sau khi theo dõi / bỏ theo dõi để trang cha cập nhật số đếm
  ownerId?: number; // Xem danh sách của người khác (trang hồ sơ). Bỏ trống = của chính mình
  ownerName?: string;
  viewerId?: number; // Người đang xem: ẩn nút Theo dõi trên dòng của chính mình
};

export default function FollowPanel({ mode, onModeChange, onChanged, ownerId, ownerName, viewerId }: Props) {
  const [page, setPage] = useState(0);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const isMine = ownerId === undefined;

  // Của mình: /community/following ; của người khác: /community/users/{id}/following
  const base = isMine ? `/community/${mode}` : `/community/users/${ownerId}/${mode}`;
  const list = useRemote<MemberPage>(`${base}?page=${page}&size=12`);

  function changeMode(next: FollowMode) {
    onModeChange(next);
    setPage(0);
  }

  /** Theo dõi / bỏ theo dõi một người. */
  async function toggle(member: FollowMember) {
    setBusyId(member.id);
    setError('');
    try {
      await mutate(`/community/users/${member.id}/follow`, 'POST', { following: !member.following });
      list.refresh();
      onChanged();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  const members = list.data?.content || [];

  return (
    <div className="follow-panel">
      <div className="follow-switch" role="group" aria-label="Danh sách theo dõi">
        <button type="button" aria-pressed={mode === 'following'} onClick={() => changeMode('following')}>
          Đang theo dõi
        </button>
        <button type="button" aria-pressed={mode === 'followers'} onClick={() => changeMode('followers')}>
          Người theo dõi
        </button>
      </div>

      <RemoteState loading={list.loading} error={list.error} onRetry={list.refresh} />
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}

      {list.data && members.length === 0 && !isMine && (
        <div className="library-empty">
          <span className="tile-icon purple">
            <Icon name="users" size={30} />
          </span>
          <h2>{mode === 'following' ? `${ownerName} chưa theo dõi ai` : `Chưa có ai theo dõi ${ownerName}`}</h2>
        </div>
      )}

      {list.data && members.length === 0 && isMine && (
        <div className="library-empty">
          <span className="tile-icon purple">
            <Icon name="users" size={30} />
          </span>
          <h2>{mode === 'following' ? 'Bạn chưa theo dõi ai' : 'Chưa có ai theo dõi bạn'}</h2>
          <p>
            {mode === 'following'
              ? 'Mở Bảng tin, bấm vào tên tác giả của bài viết hay để xem hồ sơ và theo dõi.'
              : 'Hãy chia sẻ câu hỏi và kiến thức trên Cộng đồng để mọi người biết đến bạn.'}
          </p>
          <a className="button primary" href="#community">
            Đến Cộng đồng
            <Icon name="arrow" size={16} />
          </a>
        </div>
      )}

      {members.length > 0 && (
        <ul className="follow-list">
          {members.map((m) => {
            const name = m.displayName || m.username;
            return (
              <li key={m.id} className="follow-item">
                <a href={`#person/${m.id}`} className="follow-person">
                  <span className="avatar">
                    {m.avatarUrl ? <img src={m.avatarUrl} alt="" /> : name.slice(0, 2).toUpperCase()}
                  </span>
                  <span>
                    <strong>{name}</strong>
                    <small>
                      @{m.username}
                      {m.institutionName && <> · {m.institutionName}</>}
                    </small>
                    <small>{m.followerCount} người theo dõi</small>
                  </span>
                </a>
                {/* Không hiện nút trên dòng của chính mình */}
                {m.id !== viewerId && (
                  <button
                    type="button"
                    className={m.following ? 'button secondary compact' : 'button primary compact'}
                    disabled={busyId === m.id}
                    onClick={() => toggle(m)}
                  >
                    {busyId === m.id ? 'Đang lưu…' : m.following ? 'Bỏ theo dõi' : 'Theo dõi'}
                  </button>
                )}
              </li>
            );
          })}
        </ul>
      )}

      {list.data && list.data.totalPages > 1 && <Pagination data={list.data} page={page} onPage={setPage} />}
    </div>
  );
}
