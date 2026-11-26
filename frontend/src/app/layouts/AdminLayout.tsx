import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import type { Account } from '../../features/auth/types';
import type { ManagedDocument, PageData } from '../../features/documents/types';
import { useRemote } from '../../shared/hooks/useRemote';
import Icon from '../../shared/components/ui/Icon';
import { Avatar } from '../../features/admin/components/AdminUI';

/*
 * Khung giao diện của khu quản trị:
 * sidebar bên trái, thanh trên cùng (đường dẫn + số tài liệu chờ duyệt) và vùng nội dung.
 */

// Tên trang hiện trên thanh đường dẫn
const PAGE_TITLES: Record<string, string> = {
  admin: 'Tổng quan',
  users: 'Người dùng',
  organizations: 'Trường / Khoa',
  'admin/documents': 'Kho tài liệu',
  'admin/documents/new': 'Thêm giáo trình',
  'admin/community': 'Cộng đồng & Hỏi đáp',
};

type NavLinkProps = {
  href: string;
  icon: string;
  label: string;
  active: boolean;
  badge?: number; // Số nhỏ màu đỏ bên phải (ví dụ số tài liệu chờ duyệt)
};

/** Một mục trong sidebar. */
function NavLink({ href, icon, label, active, badge }: NavLinkProps) {
  return (
    <a href={href} aria-current={active ? 'page' : undefined}>
      <Icon name={icon} size={19} />
      <span>{label}</span>
      {badge !== undefined && badge > 0 && (
        <em aria-label={`${badge} chờ duyệt`}>{badge > 99 ? '99+' : badge}</em>
      )}
    </a>
  );
}

type Props = {
  children: ReactNode;
  user: Account;
  active: string; // Trang đang mở
  onLogout: () => void;
  busy: boolean;
};

export default function AdminLayout({ children, user, active, onLogout, busy }: Props) {
  const [open, setOpen] = useState(false); // Menu trên điện thoại đang mở

  // Đếm số tài liệu chờ duyệt (tải lại mỗi khi đổi trang)
  const pending = useRemote<PageData<ManagedDocument>>(`/admin/documents?status=PENDING&size=1&_=${active}`);
  const pendingCount = pending.data?.totalElements || 0;

  // Đổi trang thì đóng menu
  useEffect(() => {
    setOpen(false);
  }, [active]);

  // Nhấn Esc để đóng menu
  useEffect(() => {
    if (!open) {
      return;
    }
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        setOpen(false);
      }
    }
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open]);

  return (
    <div className="admin-shell adm-shell">
      <a
        className="skip-link"
        href="#adm-main"
        onClick={(e) => {
          e.preventDefault();
          document.getElementById('adm-main')?.focus();
        }}
      >
        Bỏ qua điều hướng
      </a>

      {open && <button className="adm-backdrop" aria-label="Đóng menu quản trị" onClick={() => setOpen(false)} />}

      {/* ===== Sidebar ===== */}
      <aside className={open ? 'adm-sidebar is-open' : 'adm-sidebar'} id="adm-navigation">
        <a className="adm-brand" href="#admin">
          <span>
            <Icon name="book" size={20} />
          </span>
          <b>
            knowledge<i>hub</i>
          </b>
          <small>Admin</small>
        </a>

        <nav aria-label="Điều hướng quản trị">
          <p>Chung</p>
          <NavLink href="#admin" icon="grid" label="Tổng quan" active={active === 'admin'} />

          <p>Quản lý</p>
          <NavLink href="#admin/users" icon="users" label="Người dùng" active={active === 'users'} />
          <NavLink href="#admin/organizations" icon="institution" label="Trường / Khoa" active={active === 'organizations'} />
          <NavLink
            href="#admin/documents"
            icon="book"
            label="Kho tài liệu"
            active={active === 'admin/documents'}
            badge={pendingCount}
          />
          <NavLink
            href="#admin/community"
            icon="message"
            label="Cộng đồng & Hỏi đáp"
            active={active === 'admin/community'}
          />

          <p>Nội dung</p>
          <NavLink
            href="#admin/documents/new"
            icon="upload"
            label="Thêm giáo trình"
            active={active === 'admin/documents/new'}
          />
          <NavLink href="#home" icon="home" label="Trang người dùng" active={false} />
        </nav>

        <div className="adm-account">
          <Avatar name={user.username} url={user.avatarUrl} />
          <div>
            <strong>{user.displayName || user.username}</strong>
            <small>Quản trị viên</small>
          </div>
          <button
            type="button"
            className="adm-icon-action"
            aria-label="Đăng xuất"
            data-tip="Đăng xuất"
            disabled={busy}
            onClick={onLogout}
          >
            <Icon name="logout" size={17} />
          </button>
        </div>
      </aside>

      {/* ===== Vùng làm việc ===== */}
      <div className="adm-workspace">
        <header className="adm-topbar">
          <button
            type="button"
            className="adm-icon-action adm-menu-toggle"
            aria-label={open ? 'Đóng menu' : 'Mở menu'}
            aria-expanded={open}
            aria-controls="adm-navigation"
            onClick={() => setOpen(!open)}
          >
            <Icon name={open ? 'close' : 'menu'} size={19} />
          </button>

          <nav className="adm-crumbs" aria-label="Vị trí">
            <a href="#admin">Quản trị</a>
            <Icon name="chevronRight" size={14} />
            <span aria-current="page">{PAGE_TITLES[active] || 'Quản trị'}</span>
          </nav>

          <div className="adm-topbar-right">
            {pendingCount > 0 && (
              <a className="adm-topbar-pending" href="#admin/documents?status=PENDING">
                <Icon name="clock" size={15} />
                {pendingCount} chờ duyệt
              </a>
            )}
            <a className="adm-icon-action" href="#home" aria-label="Xem trang người dùng" data-tip="Trang người dùng">
              <Icon name="eye" size={17} />
            </a>
          </div>
        </header>

        <main id="adm-main" tabIndex={-1} className="adm-main">
          {children}
        </main>
      </div>
    </div>
  );
}
