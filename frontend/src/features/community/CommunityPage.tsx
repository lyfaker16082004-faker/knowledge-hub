import { useState } from 'react';
import Icon from '../../shared/components/ui/Icon';
import { useRemote } from '../../shared/hooks/useRemote';
import { mutate, errorMessage } from '../../shared/api/http-client';
import type { Account } from '../auth/types';
import CommunityActivity from './CommunityActivity';
import type { AuthorStats } from './CommunityActivity';
import FollowPanel from '../user/components/FollowPanel';
import type { FollowMode } from '../user/components/FollowPanel';
import { AdminDialog } from '../admin/components/AdminUI';

/*
 * Trang "Hồ sơ thành viên" (#person/{id}).
 * - Ảnh bìa, ảnh đại diện, giới thiệu, nút Theo dõi
 * - Số người theo dõi / đang theo dõi: bấm để xem danh sách (giống TikTok)
 * - Thống kê và bài viết cộng đồng của thành viên
 * Không có trang liệt kê toàn bộ thành viên; hồ sơ được mở từ bài viết, bình luận.
 */

type Member = {
  id: number;
  username: string;
  displayName: string | null;
  bio: string | null;
  avatarUrl: string | null;
  institutionName: string | null;
  facultyName: string | null;
  followerCount: number;
  followingCount: number;
  following: boolean; // Người xem đang theo dõi thành viên này không
};

/** Lấy tên hiển thị, nếu chưa đặt thì dùng tên đăng nhập. */
function memberName(member: Member) {
  return member.displayName || member.username;
}

/** Nút Theo dõi / Bỏ theo dõi. */
function FollowButton({ member, user, onChange }: { member: Member; user: Account | null; onChange: () => void }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function toggle() {
    setBusy(true);
    setError('');
    try {
      await mutate(`/community/users/${member.id}/follow`, 'POST', { following: !member.following });
      onChange();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  // Không hiện nút trên hồ sơ của chính mình
  if (user?.id === member.id) {
    return null;
  }

  // Chưa đăng nhập -> mời đăng nhập
  if (!user) {
    return (
      <a className="button primary" href="#login">
        Đăng nhập để theo dõi
      </a>
    );
  }

  let label = member.following ? 'Bỏ theo dõi' : 'Theo dõi';
  if (busy) {
    label = 'Đang lưu…';
  }

  return (
    <>
      <button className={member.following ? 'button secondary' : 'button primary'} disabled={busy} onClick={toggle}>
        {label}
      </button>
      {error && (
        <p role="alert" className="inline-error">
          {error}
        </p>
      )}
    </>
  );
}

/** Phần đầu trang hồ sơ: ảnh bìa, ảnh đại diện, giới thiệu, số theo dõi và nút theo dõi. */
function MemberProfile({
  member,
  user,
  posts,
  onChange,
}: {
  member: Member;
  user: Account | null;
  posts: number | undefined;
  onChange: () => void;
}) {
  const organization = [member.institutionName, member.facultyName].filter(Boolean).join(' · ');
  const isMe = user?.id === member.id;
  // Danh sách theo dõi đang mở (null = đóng)
  const [followList, setFollowList] = useState<FollowMode | null>(null);

  /** Bấm vào số theo dõi: chưa đăng nhập thì mời đăng nhập, ngược lại mở danh sách. */
  function openFollowList(mode: FollowMode) {
    if (!user) {
      location.hash = 'login';
      return;
    }
    setFollowList(mode);
  }

  return (
    <article className="mp-card">
      <div className="mp-cover" aria-hidden="true" />
      <div className="mp-card-body">
        <span className="mp-avatar">
          {member.avatarUrl ? <img src={member.avatarUrl} alt="" /> : memberName(member).slice(0, 2).toUpperCase()}
        </span>

        <div className="mp-identity">
          <h2>{memberName(member)}</h2>
          <p className="mp-username">@{member.username}</p>
          {organization && (
            <p className="mp-org">
              <Icon name="institution" size={15} />
              {organization}
            </p>
          )}
          <p className="mp-bio preserve-lines">{member.bio || 'Thành viên chưa thêm giới thiệu.'}</p>
        </div>

        <div className="mp-actions">
          {isMe ? (
            <>
              <a href="#account" className="button secondary">
                <Icon name="edit" size={15} />
                Chỉnh sửa hồ sơ
              </a>
              <a href="#mine?tab=following" className="button secondary">
                <Icon name="users" size={15} />
                Quản lý theo dõi
              </a>
            </>
          ) : (
            <FollowButton member={member} user={user} onChange={onChange} />
          )}
        </div>
      </div>

      {/* Ba con số chính */}
      <dl className="mp-counts">
        <div>
          <dt>Bài viết</dt>
          <dd>{posts ?? '—'}</dd>
        </div>
        {/* Bấm vào để xem danh sách (giống TikTok) */}
        <div>
          <button type="button" className="mp-count-button" onClick={() => openFollowList('followers')}>
            <span className="mp-count-label">Người theo dõi</span>
            <strong className="mp-count-value">{member.followerCount}</strong>
          </button>
        </div>
        <div>
          <button type="button" className="mp-count-button" onClick={() => openFollowList('following')}>
            <span className="mp-count-label">Đang theo dõi</span>
            <strong className="mp-count-value">{member.followingCount}</strong>
          </button>
        </div>
      </dl>

      {followList && (
        <AdminDialog title={memberName(member)} subtitle={`@${member.username}`} wide onClose={() => setFollowList(null)}>
          <FollowPanel
            mode={followList}
            onModeChange={setFollowList}
            onChanged={onChange}
            ownerId={member.id}
            ownerName={memberName(member)}
            viewerId={user?.id}
          />
        </AdminDialog>
      )}
    </article>
  );
}

// =========================================================
// TRANG CHÍNH
// =========================================================

export default function CommunityPage({ user, id }: { user: Account | null; id: number }) {
  const member = useRemote<Member>(`/community/users/${id}`);
  // Số bài viết hiện ở đầu trang hồ sơ
  const stats = useRemote<AuthorStats>(`/posts/authors/${id}/stats`);

  let content = null;
  if (member.loading) {
    content = <p role="status">Đang tải hồ sơ…</p>;
  } else if (member.error) {
    content = (
      <div className="alert" role="alert">
        {member.error}
        <button className="text-button" onClick={member.refresh}>
          <Icon name="refresh" size={16} />
          Thử lại
        </button>
      </div>
    );
  } else if (member.data) {
    content = (
      <>
        <MemberProfile member={member.data} user={user} posts={stats.data?.posts} onChange={member.refresh} />
        {/* Thống kê và bài viết cộng đồng của thành viên */}
        <CommunityActivity authorId={member.data.id} user={user} isMe={user?.id === member.data.id} />
      </>
    );
  }

  return (
    <section className="community-page">
      <div className="page-heading">
        <div>
          <span className="section-kicker">KẾT NỐI TRI THỨC</span>
          <h1>Hồ sơ thành viên</h1>
          <p className="muted">Theo dõi để đón đọc những chia sẻ mới của thành viên.</p>
        </div>
        <a className="button secondary" href="#community">
          ← Cộng đồng
        </a>
      </div>

      {content}
    </section>
  );
}
