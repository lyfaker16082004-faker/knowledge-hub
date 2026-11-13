import {useEffect,useState} from 'react';
import {currentAccount,logout} from '../features/auth/api';
import type {Account} from '../features/auth/types';
import AuthPage from '../features/auth/pages/AuthPage';
import AccountPage from '../features/user/pages/AccountPage';
import MyLibraryPage from '../features/user/pages/MyLibraryPage';
import AdminLayout from './layouts/AdminLayout';
import OrganizationsPage from '../features/admin/pages/OrganizationsPage';
import CommunityPage from '../features/community/CommunityPage';
import QuestionsPage from '../features/community/QuestionsPage';
import UsersPage from '../features/admin/pages/UsersPage';
import HomePage from '../features/documents/pages/HomePage';
import DocumentsPage from '../features/documents/pages/DocumentsPage';
import DocumentPage from '../features/documents/pages/DocumentPage';
import ManageDocumentsPage from '../features/documents/pages/ManageDocumentsPage';
import UploadPage from '../features/documents/pages/UploadPage';
import AboutPage from '../features/about/AboutPage';
import AdminDashboard from '../features/admin/pages/AdminDashboard';
import Icon from '../shared/components/ui/Icon';
import {ApiError,errorMessage,setAccessToken} from '../shared/api/http-client';
import '../styles/public.css';
import '../styles/sprint2.css';
 function readRoute(){const [raw,search='']=location.hash.slice(1).split('?');const path=raw==='overview'?'home':raw==='admin/users'?'users':raw==='admin/organizations'?'organizations':raw||'home';const known=['admin','admin/documents','admin/documents/new','my-documents','home','documents','about','mine','account','users','organizations','people','questions','upload','login','register'].includes(path)||/^(document|person)\/[1-9][0-9]*$/.test(path);return {path:known?path:'home',search,full:known?path+(search?'?'+search:''):'home'};}
export default function App(){
 const [user,setUser]=useState<Account|null>(null);const [loading,setLoading]=useState(true);const [route,setRoute]=useState(readRoute);const [returnTo,setReturnTo]=useState('home');const [notice,setNotice]=useState('');const [error,setError]=useState('');const [busy,setBusy]=useState(false);const [menu,setMenu]=useState(false);
 function navigate(path:string){location.hash=path;setRoute(readRoute());setMenu(false);setError('');window.scrollTo({top:0,behavior:'instant'});}
 async function recheck(){try{setUser(await currentAccount());}catch(e){if(e instanceof ApiError&&e.status===401)setUser(null);else setError(errorMessage(e));}}
 useEffect(()=>{let active=true;currentAccount().then(account=>{if(active)setUser(account);}).catch(e=>{if(active&&!(e instanceof ApiError&&e.status===401))setError(errorMessage(e));}).finally(()=>{if(active)setLoading(false);});return()=>{active=false;};},[]);
 useEffect(()=>{const hash=()=>{setRoute(readRoute());setMenu(false);window.scrollTo({top:0,behavior:'instant'});};window.addEventListener('hashchange',hash);return()=>window.removeEventListener('hashchange',hash);},[]);
 useEffect(()=>{const expire=()=>{if(user){setUser(null);setNotice('Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại.');}};window.addEventListener('auth-expired',expire);return()=>window.removeEventListener('auth-expired',expire);},[user]);
 const adminPage=route.path==='admin'||['users','organizations'].includes(route.path)||route.path.startsWith('admin/');
 const protectedPage=adminPage||['my-documents','mine','account','users','organizations','upload'].includes(route.path);
 useEffect(()=>{if(!loading&&!user&&protectedPage){setReturnTo(route.full);location.hash='login';setRoute(readRoute());}},[loading,user,protectedPage,route.full]);
 function requireLogin(){setReturnTo(route.full);setNotice('Đăng nhập để tải và lưu tài liệu vào góc học tập của bạn.');navigate('login');}
 async function signOut(){setBusy(true);try{await logout();setUser(null);setNotice('Bạn đã đăng xuất an toàn.');navigate('home');}catch(e){setError(errorMessage(e));}finally{setBusy(false);}}
 if(route.path==='login'||route.path==='register')return <div className="auth-route"><a href="#home" className="auth-home-link">← Về trang chủ</a><AuthPage key={route.path} initialMode={route.path==='register'?'register':'login'} notice={error||notice} onLogin={account=>{setUser(account);setNotice('');setError('');navigate(account.roles.includes('ADMIN')?'admin':returnTo);setReturnTo('home');}}/></div>;
 let content;
 if(protectedPage&&(!user||loading))content=<div className="library-loading" role="status"><span className="spinner"/>Đang mở không gian của bạn…</div>;
 else if(route.path==='admin'&&user)content=<AdminDashboard user={user}/>;
 else if(route.path==='documents')content=loading?<div className="library-loading">Đang mở thư viện…</div>:<DocumentsPage key={route.search+String(user?.id||0)+String(user?.organization?.institutionId||0)} search={route.search} user={user}/>;
 else if(route.path==='questions')content=<QuestionsPage user={user}/>;
 else if(route.path==='my-documents'&&user)content=<ManageDocumentsPage user={user}/>;
 else if(route.path==='admin/documents'&&user)content=user.roles.includes('ADMIN')?<ManageDocumentsPage user={user} adminMode/>:<h1>Bạn chưa có quyền quản trị</h1>;
 else if(route.path==='admin/documents/new'&&user)content=user.roles.includes('ADMIN')?<UploadPage user={user} adminMode onUploaded={()=>navigate('admin/documents')}/>:<h1>Bạn chưa có quyền quản trị</h1>;
 else if(route.path==='upload'&&user)content=<UploadPage user={user} onUploaded={id=>navigate(`document/${id}`)}/>;
 else if(route.path.startsWith('document/'))content=<DocumentPage key={route.path} id={Number(route.path.split('/')[1])} user={user} requireLogin={requireLogin}/>;
 else if(route.path==='people'||route.path.startsWith('person/'))content=<CommunityPage key={route.path+String(user?.id||0)} user={user} id={route.path.startsWith('person/')?Number(route.path.split('/')[1]):undefined}/>;
 else if(route.path==='organizations'&&user)content=user.roles.includes('ADMIN')?<OrganizationsPage/>:<div className="library-empty"><h1>Bạn chưa có quyền quản trị</h1></div>;
 else if(route.path==='about')content=<AboutPage/>;
 else if(route.path==='mine'&&user)content=<MyLibraryPage user={user}/>;
 else if(route.path==='account'&&user)content=<AccountPage user={user} onProfileSaved={setUser} onChanged={()=>{setAccessToken(null);setUser(null);setNotice('Đổi mật khẩu thành công. Vui lòng đăng nhập lại.');setReturnTo('account');navigate('login');}}/>;
 else if(route.path==='users'&&user)content=user.roles.includes('ADMIN')?<UsersPage recheck={recheck}/>:<div className="library-empty"><Icon name="shield" size={38}/><h1>Bạn chưa có quyền quản trị</h1><p>Trang này dành cho quản trị viên hệ thống.</p><a className="button primary" href="#home">Về trang chủ</a></div>;
 else content=<HomePage user={user}/>;
 if(adminPage&&user&&!user.roles.includes('ADMIN'))return <main className="admin-access-denied">{content}<a className="button secondary" href="#home">Về trang người dùng</a></main>;
 if(adminPage&&user)return <AdminLayout user={user} active={route.path} onLogout={signOut} busy={busy}>{error&&<div className="alert" role="alert">{error}</div>}{content}</AdminLayout>;
 const libraryActive=route.path==='documents'||route.path.startsWith('document/');
 return <div className="public-app"><a className="skip-link" href="#main-content" onClick={event=>{event.preventDefault();document.getElementById('main-content')?.focus();}}>Bỏ qua điều hướng</a><header className="site-header"><div className="site-header-inner"><a href="#home" className="brand"><span className="brand-symbol"><Icon name="book" size={22}/></span>knowledge<span className="brand-light">hub</span><span className="brand-dot">.</span></a><button className="mobile-menu-button icon-button" aria-label={menu?'Đóng menu':'Mở menu'} aria-expanded={menu} aria-controls="site-navigation" onClick={()=>setMenu(!menu)}><Icon name={menu?'close':'menu'} size={23}/></button><nav id="site-navigation" aria-label="Điều hướng chính" className={menu?'site-nav is-open':'site-nav'}><a href="#home" aria-current={route.path==='home'?'page':undefined}>Trang chủ</a><a href="#documents" aria-current={libraryActive?'page':undefined}>Tài liệu</a><a href="#questions" aria-current={route.path==='questions'?'page':undefined}>Hỏi đáp</a><a href="#people" aria-current={route.path==='people'||route.path.startsWith('person/')?'page':undefined}>Cộng đồng</a><a href="#about" aria-current={route.path==='about'?'page':undefined}>Về chúng tôi</a><a href="#mine" aria-current={['mine','account'].includes(route.path)?'page':undefined}>Trang của tôi</a>{user&&<a href="#upload" aria-current={route.path==='upload'?'page':undefined}>Đóng góp</a>}</nav><div className="site-auth-actions">{loading?<span className="muted small">Đang kết nối…</span>:user?<><a href="#mine" className="header-account" aria-label={`Trang của ${user.username}`}><span className="avatar">{user.avatarUrl?<img src={user.avatarUrl} alt=""/>:user.username.slice(0,2).toUpperCase()}</span><span>{user.username}</span></a><button className="icon-button" aria-label="Đăng xuất" onClick={signOut} disabled={busy}><Icon name="logout" size={19}/></button></>:<><a href="#login" className="header-login">Đăng nhập</a><a href="#register" className="button primary">Đăng ký<Icon name="arrow" size={15}/></a></>}</div></div></header><main id="main-content" tabIndex={-1} className={`public-main${route.path==='users'?' admin-public-main':''}`}>{error&&<div className="alert" role="alert">{error}</div>}{notice&&route.path==='home'&&<div className="notice" role="status">{notice}</div>}{content}</main><footer className="site-footer"><div className="footer-top"><div><a href="#home" className="brand"><span className="brand-symbol"><Icon name="book" size={20}/></span>knowledge<span className="brand-light">hub</span><span className="brand-dot">.</span></a><p>Một nơi để học hỏi.<br/>Một góc nhỏ để giữ lại tri thức.</p></div><div><h2>Khám phá</h2><a href="#documents">Thư viện tài liệu</a><a href="#about">Về chúng tôi</a></div><div><h2>Không gian của bạn</h2><a href="#mine">Trang của tôi</a><a href={user?'#account':'#login'}>{user?'Bảo mật tài khoản':'Đăng nhập / Đăng ký'}</a></div><div className="footer-quote"><Icon name="spark" size={25}/><p>Học một chút mỗi ngày.<br/>Hiểu thêm một điều mới.</p></div></div><div className="footer-bottom"><span>© {new Date().getFullYear()} Knowledge Hub</span><span>Học hỏi. Chia sẻ. Kết nối.</span></div></footer></div>;
}
