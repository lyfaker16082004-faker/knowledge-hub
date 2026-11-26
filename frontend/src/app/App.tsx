import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { currentAccount, logout } from '../features/auth/api';
import type { Account } from '../features/auth/types';
import AuthPage from '../features/auth/pages/AuthPage';
import AccountPage from '../features/user/pages/AccountPage';
import MyLibraryPage, { tabFromSearch } from '../features/user/pages/MyLibraryPage';
import AdminLayout from './layouts/AdminLayout';
import OrganizationsPage from '../features/admin/pages/OrganizationsPage';
import CommunityPage from '../features/community/CommunityPage';
import CommunityFeedPage from '../features/post/pages/CommunityFeedPage';
import PostDetailPage from '../features/post/pages/PostDetailPage';
import UsersPage from '../features/admin/pages/UsersPage';
import HomePage from '../features/documents/pages/HomePage';
import DocumentsPage from '../features/documents/pages/DocumentsPage';
import DocumentPage from '../features/documents/pages/DocumentPage';
import ManageDocumentsPage from '../features/documents/pages/ManageDocumentsPage';
import UploadPage from '../features/documents/pages/UploadPage';
import AboutPage from '../features/about/AboutPage';
import AdminDashboard from '../features/admin/pages/AdminDashboard';
import CommunityAdminPage from '../features/admin/pages/CommunityAdminPage';
import Icon from '../shared/components/ui/Icon';
import { ApiError, errorMessage, setAccessToken } from '../shared/api/http-client';
import '../styles/public.css';
import '../styles/sprint2.css';
import '../styles/side-rail.css';

/*
 * Thành phần gốc của ứng dụng.
 * - Đọc địa chỉ sau dấu # để biết đang ở trang nào (ví dụ #documents, #post/12)
 * - Kiểm tra đăng nhập, chuyển về trang đăng nhập nếu trang cần đăng nhập
 * - Trang quản trị dùng AdminLayout, các trang còn lại dùng sidebar bên trái
 */

// =========================================================
// PHẦN 1: ĐỌC ĐƯỜNG DẪN (ROUTE)
// =========================================================

// Các trang có trong ứng dụng
const KNOWN_PAGES = [
  'admin',
  'admin/documents',
  'admin/documents/new',
  'admin/community',
  'my-documents',
  'home',
  'documents',
  'about',
  'mine',
  'account',
  'users',
  'organizations',
  'community',
  'upload',
  'login',
  'register',
];

// Trang có số id ở cuối, ví dụ document/5, person/3, post/12
const PAGE_WITH_ID = /^(document|person|post)\/[1-9][0-9]*$/;

// Các trang bắt buộc đăng nhập (ngoài các trang quản trị)
const LOGIN_REQUIRED_PAGES = ['my-documents', 'mine', 'account', 'users', 'organizations', 'upload'];

type Route = {
  path: string; // Tên trang, ví dụ "documents"
  search: string; // Phần sau dấu ?, ví dụ "q=java"
  full: string; // path + search, dùng để quay lại sau khi đăng nhập
};

/** Đọc location.hash và trả về trang hiện tại. */
function readRoute(): Route {
  const hash = location.hash.slice(1); // Bỏ dấu #
  const parts = hash.split('?');
  const raw = parts[0];
  const search = parts[1] || '';

  // Một số đường dẫn cũ được đổi sang tên mới
  let path = raw || 'home';
  if (raw === 'overview') path = 'home';
  if (raw === 'admin/users') path = 'users';
  if (raw === 'admin/organizations') path = 'organizations';

  // Đường dẫn lạ -> về trang chủ
  const isKnown = KNOWN_PAGES.includes(path) || PAGE_WITH_ID.test(path);
  if (!isKnown) {
    return { path: 'home', search, full: 'home' };
  }

  const full = search ? path + '?' + search : path;
  return { path, search, full };
}

/** Lấy số id ở cuối đường dẫn, ví dụ "post/12" -> 12. */
function idFromPath(path: string) {
  return Number(path.split('/')[1]);
}

/** Thông báo khi người dùng thường vào trang quản trị. */
function NoPermission() {
  return <h1>Bạn chưa có quyền quản trị</h1>;
}

// =========================================================
// PHẦN 2: THÀNH PHẦN APP
// =========================================================

export default function App() {
  const [user, setUser] = useState<Account | null>(null);
  const [loading, setLoading] = useState(true); // Đang kiểm tra đăng nhập
  const [route, setRoute] = useState(readRoute);
  const [returnTo, setReturnTo] = useState('home'); // Trang quay lại sau khi đăng nhập
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [menu, setMenu] = useState(false); // Menu trên điện thoại đang mở

  /** Chuyển sang trang khác. */
  function navigate(path: string) {
    location.hash = path;
    setRoute(readRoute());
    setMenu(false);
    setError('');
    window.scrollTo({ top: 0, behavior: 'instant' });
  }

  /** Tải lại thông tin tài khoản (sau khi admin sửa chính mình). */
  async function recheck() {
    try {
      setUser(await currentAccount());
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        setUser(null);
      } else {
        setError(errorMessage(e));
      }
    }
  }

  // Lúc mở trang: hỏi backend xem đã đăng nhập chưa
  useEffect(() => {
    let active = true;
    currentAccount()
      .then((account) => {
        if (active) setUser(account);
      })
      .catch((e) => {
        // 401 = chưa đăng nhập, không phải lỗi
        const notLoggedIn = e instanceof ApiError && e.status === 401;
        if (active && !notLoggedIn) setError(errorMessage(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  // Khi địa chỉ # thay đổi (bấm link) -> đổi trang
  useEffect(() => {
    function onHashChange() {
      setRoute(readRoute());
      setMenu(false);
      window.scrollTo({ top: 0, behavior: 'instant' });
    }
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  // Khi token hết hạn (http-client phát sự kiện 'auth-expired') -> đăng xuất
  useEffect(() => {
    function onExpired() {
      if (user) {
        setUser(null);
        setNotice('Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại.');
      }
    }
    window.addEventListener('auth-expired', onExpired);
    return () => window.removeEventListener('auth-expired', onExpired);
  }, [user]);

  // Thông báo tự ẩn sau 4 giây.
  // Riêng trang đăng nhập / đăng ký thì giữ lại vì nó giải thích vì sao người dùng ở đó.
  useEffect(() => {
    if (!notice || route.path === 'login' || route.path === 'register') {
      return;
    }
    const timer = setTimeout(() => setNotice(''), 4000);
    return () => clearTimeout(timer);
  }, [notice, route.path]);

  const isAdminPage =
    route.path === 'admin' ||
    route.path === 'users' ||
    route.path === 'organizations' ||
    route.path.startsWith('admin/');
  const needsLogin = isAdminPage || LOGIN_REQUIRED_PAGES.includes(route.path);
  const isAuthPage = route.path === 'login' || route.path === 'register';
  const isAdmin = user !== null && user.roles.includes('ADMIN');

  // Trang cần đăng nhập mà chưa đăng nhập -> chuyển sang trang đăng nhập
  useEffect(() => {
    if (!loading && !user && needsLogin) {
      setReturnTo(route.full);
      location.hash = 'login';
      setRoute(readRoute());
    }
  }, [loading, user, needsLogin, route.full]);

  /** Gọi khi người chưa đăng nhập bấm Tải / Lưu tài liệu. */
  function requireLogin() {
    setReturnTo(route.full);
    setNotice('Đăng nhập để tải và lưu tài liệu vào góc học tập của bạn.');
    navigate('login');
  }

  async function signOut() {
    setBusy(true);
    try {
      await logout();
      setUser(null);
      setNotice('Bạn đã đăng xuất an toàn.');
      navigate('home');
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  // =========================================================
  // PHẦN 3: CHỌN TRANG ĐỂ HIỂN THỊ
  // =========================================================

  function renderPage(): ReactNode {
    const path = route.path;

    if (isAuthPage) {
      return (
        <div className="auth-route auth-in-rail">
          <AuthPage
            key={path}
            initialMode={path === 'register' ? 'register' : 'login'}
            notice={error || notice}
            onLogin={(account) => {
              setUser(account);
              setNotice('');
              setError('');
              // Admin vào trang quản trị, người dùng thường quay lại trang trước đó
              navigate(account.roles.includes('ADMIN') ? 'admin' : returnTo);
              setReturnTo('home');
            }}
          />
        </div>
      );
    }

    // Trang cần đăng nhập: đợi kiểm tra xong
    if (needsLogin && (!user || loading)) {
      return (
        <div className="library-loading" role="status">
          <span className="spinner" />
          Đang mở không gian của bạn…
        </div>
      );
    }

    // ----- Trang quản trị -----
    if (path === 'admin' && user) {
      return <AdminDashboard user={user} />;
    }
    if (path === 'admin/documents' && user) {
      if (!isAdmin) return <NoPermission />;
      const status = new URLSearchParams(route.search).get('status') || '';
      return <ManageDocumentsPage key={route.search} user={user} adminMode initialStatus={status} />;
    }
    if (path === 'admin/community' && user) {
      if (!isAdmin) return <NoPermission />;
      return <CommunityAdminPage key={route.search} />;
    }
    if (path === 'admin/documents/new' && user) {
      if (!isAdmin) return <NoPermission />;
      return <UploadPage user={user} adminMode onUploaded={() => navigate('admin/documents')} />;
    }
    if (path === 'organizations' && user) {
      if (!isAdmin) {
        return (
          <div className="library-empty">
            <NoPermission />
          </div>
        );
      }
      return <OrganizationsPage />;
    }
    if (path === 'users' && user) {
      if (!isAdmin) {
        return (
          <div className="library-empty">
            <Icon name="shield" size={38} />
            <h1>Bạn chưa có quyền quản trị</h1>
            <p>Trang này dành cho quản trị viên hệ thống.</p>
            <a className="button primary" href="#home">
              Về trang chủ
            </a>
          </div>
        );
      }
      return <UsersPage recheck={recheck} />;
    }

    // ----- Thư viện tài liệu -----
    if (path === 'documents') {
      if (loading) {
        return <div className="library-loading">Đang mở thư viện…</div>;
      }
      // key thay đổi -> tạo lại trang (khi đổi bộ lọc trên URL hoặc đổi người dùng)
      const key = route.search + String(user?.id || 0) + String(user?.organization?.institutionId || 0);
      return <DocumentsPage key={key} search={route.search} user={user} />;
    }
    if (path.startsWith('document/')) {
      return <DocumentPage key={path} id={idFromPath(path)} user={user} requireLogin={requireLogin} />;
    }
    if (path === 'my-documents' && user) {
      // Đường dẫn cũ: mở trang Quản lý cá nhân ở tab "Đã đăng"
      return <MyLibraryPage key="my-documents" user={user} initialTab="uploads" />;
    }
    if (path === 'upload' && user) {
      return <UploadPage user={user} onUploaded={(id) => navigate(`document/${id}`)} />;
    }

    // ----- Cộng đồng -----
    if (path === 'community') {
      return <CommunityFeedPage key={route.search + String(user?.id || 0)} user={user} search={route.search} />;
    }
    if (path.startsWith('post/')) {
      return <PostDetailPage key={path + String(user?.id || 0)} id={idFromPath(path)} user={user} />;
    }
    if (path.startsWith('person/')) {
      return <CommunityPage key={path + String(user?.id || 0)} user={user} id={idFromPath(path)} />;
    }

    // ----- Trang cá nhân -----
    if (path === 'about') {
      return <AboutPage />;
    }
    if (path === 'mine' && user) {
      return <MyLibraryPage key={route.search} user={user} initialTab={tabFromSearch(route.search)} />;
    }
    if (path === 'account' && user) {
      return (
        <AccountPage
          user={user}
          onProfileSaved={setUser}
          onChanged={() => {
            // Đổi mật khẩu xong thì bắt đăng nhập lại
            setAccessToken(null);
            setUser(null);
            setNotice('Đổi mật khẩu thành công. Vui lòng đăng nhập lại.');
            setReturnTo('account');
            navigate('login');
          }}
        />
      );
    }

    return <HomePage user={user} />;
  }

  const content = renderPage();

  // Người dùng thường vào trang quản trị -> chỉ hiện thông báo không có quyền
  if (isAdminPage && user && !isAdmin) {
    return (
      <main className="admin-access-denied">
        {content}
        <a className="button secondary" href="#home">
          Về trang người dùng
        </a>
      </main>
    );
  }

  // Trang quản trị dùng giao diện riêng
  if (isAdminPage && user) {
    return (
      <AdminLayout user={user} active={route.path} onLogout={signOut} busy={busy}>
        {error && (
          <div className="alert" role="alert">
            {error}
          </div>
        )}
        {content}
      </AdminLayout>
    );
  }

  // =========================================================
  // PHẦN 4: GIAO DIỆN NGƯỜI DÙNG (SIDEBAR BÊN TRÁI)
  // =========================================================

  const path = route.path;
  const navItems = [
    { path: 'home', icon: 'home', label: 'Trang chủ', active: path === 'home' },
    { path: 'documents', icon: 'book', label: 'Tài liệu', active: path === 'documents' || path.startsWith('document/') },
    // "Đã lưu" và "Đã đăng" được gộp thành một mục Cá nhân
    {
      path: 'mine',
      icon: 'user',
      label: 'Cá nhân',
      active: path === 'mine' || path === 'my-documents' || path === 'upload',
    },
    {
      path: 'community',
      icon: 'users',
      label: 'Cộng đồng',
      active: path === 'community' || path.startsWith('person/') || path.startsWith('post/'),
    },
    { path: 'account', icon: 'settings', label: 'Cài đặt', active: path === 'account' },
    { path: 'about', icon: 'help', label: 'Giới thiệu', active: path === 'about' },
  ];

  let appClass = 'public-app rail-app';
  if (menu) appClass += ' rail-open';
  if (isAuthPage) appClass += ' rail-auth';

  // Phần tài khoản ở cuối sidebar
  let accountArea;
  if (loading) {
    accountArea = <span className="spinner" />;
  } else if (user) {
    accountArea = (
      <>
        <a href="#mine" className="rail-avatar" aria-label={`Trang của ${user.username}`} title={user.username}>
          {user.avatarUrl ? <img src={user.avatarUrl} alt="" /> : user.username.slice(0, 2).toUpperCase()}
        </a>
        <button className="rail-logout" aria-label="Đăng xuất" title="Đăng xuất" onClick={signOut} disabled={busy}>
          <Icon name="logout" size={19} />
        </button>
      </>
    );
  } else {
    accountArea = (
      <a href="#login" className="rail-login" aria-current={isAuthPage ? 'page' : undefined}>
        <Icon name="user" size={22} />
        <span>Đăng nhập</span>
      </a>
    );
  }

  return (
    <div className={appClass}>
      <a
        className="skip-link"
        href="#main-content"
        onClick={(event) => {
          event.preventDefault();
          document.getElementById('main-content')?.focus();
        }}
      >
        Bỏ qua điều hướng
      </a>

      {/* Thanh trên cùng (chỉ hiện trên điện thoại) */}
      <header className="rail-topbar">
        <a href="#home" className="rail-logo" aria-label="Knowledge Hub - Trang chủ">
          <Icon name="book" size={22} />
        </a>
        <span className="rail-topbar-title">
          knowledge<b>hub</b>
        </span>
        <button
          className="icon-button"
          aria-label={menu ? 'Đóng menu' : 'Mở menu'}
          aria-expanded={menu}
          aria-controls="site-navigation"
          onClick={() => setMenu(!menu)}
        >
          <Icon name={menu ? 'close' : 'menu'} size={23} />
        </button>
      </header>

      {/* Sidebar bên trái */}
      <aside className="side-rail" id="site-navigation">
        <a href="#home" className="rail-logo" aria-label="Knowledge Hub - Trang chủ">
          <Icon name="book" size={24} />
        </a>
        <a href={user ? '#upload' : '#login'} className="rail-plus" aria-label="Đóng góp tài liệu" title="Đóng góp tài liệu">
          <Icon name="plus" size={28} />
        </a>
        <nav aria-label="Điều hướng chính" className="rail-nav">
          {navItems.map((item) => (
            <a key={item.path} href={`#${item.path}`} aria-current={item.active ? 'page' : undefined}>
              <Icon name={item.icon} size={22} />
              <span>{item.label}</span>
            </a>
          ))}
        </nav>
        <div className="rail-account">{accountArea}</div>
      </aside>

      {menu && <button className="rail-backdrop" aria-label="Đóng menu" onClick={() => setMenu(false)} />}

      {/* Nội dung trang */}
      <div className="rail-content">
        <main
          id="main-content"
          tabIndex={-1}
          className={path === 'users' ? 'public-main admin-public-main' : 'public-main'}
        >
          {error && !isAuthPage && (
            <div className="alert" role="alert">
              {error}
            </div>
          )}
          {notice && path === 'home' && (
            <div className="notice notice-toast" role="status">
              <span>{notice}</span>
              <button type="button" className="icon-button" aria-label="Đóng thông báo" onClick={() => setNotice('')}>
                <Icon name="close" size={16} />
              </button>
            </div>
          )}
          {content}
        </main>
        <footer className="rail-footer">
          <span>© {new Date().getFullYear()} Knowledge Hub</span>
          <span>Học hỏi. Chia sẻ. Kết nối.</span>
        </footer>
      </div>
    </div>
  );
}
