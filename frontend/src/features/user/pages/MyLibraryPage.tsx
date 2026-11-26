import { useState } from 'react';
import type { Account } from '../../auth/types';
import { dateLabel, roleNames } from '../../auth/types';
import { useRemote } from '../../../shared/hooks/useRemote';
import type { PersonalStats, DownloadInfo, DocumentItem, ManagedDocument, PageData } from '../../documents/types';
import { DocumentGrid, RemoteState, Pagination } from '../../documents/components/LibraryUI';
import ManageDocumentsPage from '../../documents/pages/ManageDocumentsPage';
import FollowPanel from '../components/FollowPanel';
import CommunityActivity from '../../community/CommunityActivity';
import type { FollowMember, FollowMode } from '../components/FollowPanel';
import Icon from '../../../shared/components/ui/Icon';

/*
 * Trang "Quản lý cá nhân" (#mine), gộp "Đã lưu" và "Đã đăng" vào một chỗ:
 * - Lời chào và vai trò của người dùng
 * - 4 số liệu: tài liệu đã đăng, đã tải, tổng lượt tải, đã lưu
 * - Số người theo dõi / đang theo dõi bên Cộng đồng
 * - 5 tab: Đã lưu, Đã đăng (sửa / xoá tài liệu của mình), Lịch sử tải,
 *   Bài viết cộng đồng (thống kê + bài đã đăng), Theo dõi
 */

export type Tab = 'bookmarks' | 'uploads' | 'downloads' | 'posts' | 'following';

/** Đọc tab từ URL, ví dụ #mine?tab=uploads */
export function tabFromSearch(search: string): Tab {
  const value = new URLSearchParams(search).get('tab');
  if (value === 'uploads' || value === 'downloads' || value === 'posts' || value === 'following') {
    return value;
  }
  return 'bookmarks';
}

/** Lịch sử các lần tải tài liệu. */
function DownloadHistory({ items }: { items: DownloadInfo[] }) {
  if (items.length === 0) {
    return (
      <div className="library-empty">
        <span className="tile-icon purple">
          <Icon name="download" size={30} />
        </span>
        <h2>Thư viện của bạn đang chờ trang đầu tiên</h2>
        <p>Bạn chưa tải tài liệu nào. Khám phá thư viện và chọn một điều muốn học nhé.</p>
        <a className="button primary" href="#documents">
          Khám phá tài liệu
          <Icon name="arrow" size={16} />
        </a>
      </div>
    );
  }

  return (
    <div className="download-history">
      {items.map((item) => (
        <article className="history-row" key={item.id}>
          <span className="history-icon">
            <Icon name="book" size={25} />
          </span>
          <div>
            <span className="category-label">{item.category}</span>
            <h2>{item.title}</h2>
            <p>
              {item.fileName}
              <span>·</span>
              {dateLabel(item.downloadedAt)}
            </p>
          </div>

          {/* Tài liệu đã bị gỡ thì không mở lại được */}
          {item.available ? (
            <a href={`#document/${item.documentId}`} className="button secondary compact">
              Xem / tải lại
              <Icon name="arrow" size={15} />
            </a>
          ) : (
            <span className="muted small">Không còn công bố</span>
          )}
        </article>
      ))}
    </div>
  );
}

/** Thông báo khi chưa lưu tài liệu nào. */
function EmptySaved() {
  return (
    <div className="library-empty">
      <span className="tile-icon peach">
        <Icon name="bookmark" size={30} />
      </span>
      <h2>Giữ lại những điều bạn muốn học</h2>
      <p>Nhấn “Lưu để đọc sau” ở trang chi tiết để thêm tài liệu vào đây.</p>
      <a className="button primary" href="#documents">
        Tìm tài liệu để lưu
        <Icon name="arrow" size={16} />
      </a>
    </div>
  );
}

// Các tab: [mã tab, icon, tên hiển thị]
const TABS: [Tab, string, string][] = [
  ['bookmarks', 'bookmark', 'Đã lưu'],
  ['uploads', 'upload', 'Đã đăng'],
  ['downloads', 'download', 'Lịch sử tải'],
  ['posts', 'message', 'Bài viết cộng đồng'],
  ['following', 'users', 'Theo dõi'],
];

type Props = {
  user: Account;
  initialTab?: Tab; // Tab mở sẵn, ví dụ #my-documents mở thẳng tab "Đã đăng"
};

export default function MyLibraryPage({ user, initialTab = 'bookmarks' }: Props) {
  const [tab, setTab] = useState<Tab>(initialTab);
  const [page, setPage] = useState(0);

  // Số trang chỉ áp dụng cho tab đang mở, tab còn lại luôn ở trang 0
  const downloadPage = tab === 'downloads' ? page : 0;
  const savedPage = tab === 'bookmarks' ? page : 0;

  const [followMode, setFollowMode] = useState<FollowMode>('following');

  const stats = useRemote<PersonalStats>('/users/me/library/stats');
  const uploads = useRemote<PageData<ManagedDocument>>('/users/me/documents?size=1');
  // Số người theo dõi / đang theo dõi lấy từ hồ sơ cộng đồng của chính mình
  const me = useRemote<FollowMember>(`/community/users/${user.id}`);

  /** Bấm vào số theo dõi -> mở tab Theo dõi ở đúng danh sách. */
  function showFollow(mode: FollowMode) {
    setFollowMode(mode);
    selectTab('following');
  }
  const history = useRemote<PageData<DownloadInfo>>(`/users/me/downloads?page=${downloadPage}&size=10`);
  const saved = useRemote<PageData<DocumentItem>>(`/users/me/bookmarks?page=${savedPage}&size=9`);
  const current = tab === 'downloads' ? history : saved;

  function selectTab(next: Tab) {
    setTab(next);
    setPage(0);
    // Ghi tab lên URL để tải lại trang vẫn mở đúng tab
    window.history.replaceState(null, '', `#mine?tab=${next}`);
  }

  const roles = user.roles.map((r) => roleNames[r]).join(' · ');

  return (
    <>
      {/* Lời chào */}
      <section className="my-library-banner">
        <span className="avatar large">
          {user.avatarUrl ? <img src={user.avatarUrl} alt="" /> : user.username.slice(0, 2).toUpperCase()}
        </span>
        <div>
          <span className="section-kicker">GÓC TRI THỨC CỦA RIÊNG BẠN</span>
          <h1>Quản lý cá nhân</h1>
          <p>Chào {user.displayName || user.username}, tiếp tục từ nơi bạn đã bắt đầu.</p>
          <span className="member-role">{roles}</span>
          {/* Số theo dõi bên Cộng đồng: bấm để xem danh sách */}
          <div className="follow-counts">
            <button type="button" onClick={() => showFollow('followers')}>
              <strong>{me.data?.followerCount ?? '—'}</strong> người theo dõi
            </button>
            <button type="button" onClick={() => showFollow('following')}>
              <strong>{me.data?.followingCount ?? '—'}</strong> đang theo dõi
            </button>
            <a href={`#person/${user.id}`}>Xem hồ sơ cộng đồng</a>
          </div>
        </div>
        <a href="#account" className="button secondary">
          <Icon name="user" size={17} />
          Quản lý tài khoản
        </a>
      </section>

      {/* 4 số liệu cá nhân */}
      <div className="personal-stats four">
        <div>
          <span className="tile-icon purple">
            <Icon name="upload" size={24} />
          </span>
          <div>
            <strong>{uploads.data?.totalElements ?? '—'}</strong>
            <span>Tài liệu đã đăng</span>
          </div>
        </div>
        <div>
          <span className="tile-icon purple">
            <Icon name="book" size={24} />
          </span>
          <div>
            <strong>{stats.data?.downloadedDocuments ?? '—'}</strong>
            <span>Tài liệu đã tải</span>
          </div>
        </div>
        <div>
          <span className="tile-icon green">
            <Icon name="download" size={24} />
          </span>
          <div>
            <strong>{stats.data?.downloads ?? '—'}</strong>
            <span>Tổng lượt tải</span>
          </div>
        </div>
        <div>
          <span className="tile-icon peach">
            <Icon name="bookmark" size={24} />
          </span>
          <div>
            <strong>{stats.data?.savedDocuments ?? '—'}</strong>
            <span>Tài liệu đã lưu</span>
          </div>
        </div>
      </div>

      {/* Ba tab: Đã lưu / Đã đăng / Lịch sử tải */}
      <section className="personal-library">
        <div className="library-tabs" role="tablist" aria-label="Quản lý cá nhân">
          {TABS.map(([key, icon, label]) => (
            <button
              key={key}
              role="tab"
              id={`${key}-tab`}
              aria-controls="library-content"
              aria-selected={tab === key}
              onClick={() => selectTab(key)}
            >
              <Icon name={icon} size={18} />
              {label}
            </button>
          ))}
          <a href="#documents" className="my-contributions-link">
            Khám phá thêm
            <Icon name="arrow" size={15} />
          </a>
        </div>

        <div id="library-content" role="tabpanel" aria-labelledby={`${tab}-tab`}>
          {/* Tab Đã đăng: dùng lại màn hình quản lý tài liệu của tác giả */}
          {tab === 'uploads' && <ManageDocumentsPage user={user} embedded />}

          {/* Tab Bài viết cộng đồng: thống kê và các bài mình đã đăng */}
          {tab === 'posts' && (
            <div className="personal-community">
              <CommunityActivity authorId={user.id} user={user} isMe />
            </div>
          )}

          {/* Tab Theo dõi: danh sách đang theo dõi / người theo dõi */}
          {tab === 'following' && <FollowPanel mode={followMode} onModeChange={setFollowMode} onChanged={me.refresh} />}

          {(tab === 'bookmarks' || tab === 'downloads') && (
            <RemoteState loading={current.loading} error={current.error} onRetry={current.refresh} />
          )}

          {tab === 'downloads' && history.data && (
            <>
              <DownloadHistory items={history.data.content} />
              {history.data.content.length > 0 && <Pagination data={history.data} page={page} onPage={setPage} />}
            </>
          )}

          {tab === 'bookmarks' && saved.data && saved.data.content.length > 0 && (
            <div className="personal-saved">
              <DocumentGrid documents={saved.data.content} />
              <Pagination data={saved.data} page={page} onPage={setPage} />
            </div>
          )}

          {tab === 'bookmarks' && saved.data && saved.data.content.length === 0 && <EmptySaved />}
        </div>
      </section>
    </>
  );
}
