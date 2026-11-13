import type { Account } from '../../auth/types';
import Icon from '../../../shared/components/ui/Icon';
import { useRemote } from '../../../shared/hooks/useRemote';
import type { CatalogueStats, ManagedDocument, PageData } from '../../documents/types';
import { documentStatuses } from '../../documents/types';

const areas = [
  { href: '#admin/users', icon: 'users', title: 'Quản lý người dùng', description: 'Xem hồ sơ, cập nhật vai trò và xử lý tài khoản bằng thao tác xóa mềm.', tone: 'violet' },
  { href: '#admin/organizations', icon: 'institution', title: 'Trường và khoa', description: 'Tạo, chỉnh sửa và quản lý danh mục trường, tổ chức và chuyên ngành.', tone: 'blue' },
  { href: '#admin/documents', icon: 'book', title: 'Kho tài liệu', description: 'Duyệt tài liệu chờ xét duyệt, quản lý nội dung và quyền truy cập.', tone: 'green' },
];

export default function AdminDashboard({ user }: { user: Account }) {
  const users = useRemote<{totalElements:number}>('/admin/users?size=1');
  const documents = useRemote<PageData<ManagedDocument>>('/admin/documents?size=1');
  const pending = useRemote<PageData<ManagedDocument>>('/admin/documents?status=PENDING&size=5');
  const stats = useRemote<CatalogueStats>('/documents/stats');
  const failed = [users,documents,pending,stats].some(resource => resource.error);
  const metrics = [
    {label:'Người dùng',value:users.data?.totalElements,icon:'users',tone:'violet'},
    {label:'Tài liệu',value:documents.data?.totalElements,icon:'book',tone:'green'},
    {label:'Lượt tải',value:stats.data?.downloads,icon:'download',tone:'blue'},
    {label:'Chờ xét duyệt',value:pending.data?.totalElements,icon:'clock',tone:'amber'},
  ];
  return <div className="admin-dashboard">
    <div className="page-heading">
      <div><span className="section-kicker">KHÔNG GIAN QUẢN TRỊ</span><h1>Tổng quan</h1><p className="muted">Xin chào {user.username}, chọn khu vực bạn muốn quản lý.</p></div>
      <span className="admin-dashboard-secure"><Icon name="shield" size={17}/>Quản trị viên</span>
    </div>
    <section className="admin-dashboard-welcome">
      <span className="admin-dashboard-welcome-icon"><Icon name="spark" size={25}/></span>
      <div><h2>Chào mừng đến với Knowledge Hub Admin</h2><p>Các công cụ quản trị được tập hợp tại đây để bạn theo dõi và cập nhật hệ thống.</p></div>
      <Icon name="grid" size={58}/>
    </section>
    {failed&&<div className="alert" role="alert">Một số thống kê chưa tải được.<button className="text-button" onClick={()=>{users.refresh();documents.refresh();pending.refresh();stats.refresh();}}><Icon name="refresh" size={16}/>Thử lại</button></div>}
    <div className="admin-stat-grid" aria-label="Thống kê hệ thống">
      {metrics.map(metric=><article className="admin-stat-card" key={metric.label}><span className={`admin-dashboard-card-icon ${metric.tone}`}><Icon name={metric.icon} size={23}/></span><div><strong>{metric.value===undefined?'—':metric.value.toLocaleString('vi-VN')}</strong><small>{metric.label}</small></div></article>)}
    </div>
    <div className="admin-dashboard-grid">
      {areas.map(area => <a className="admin-dashboard-card" href={area.href} key={area.href}>
        <span className={`admin-dashboard-card-icon ${area.tone}`}><Icon name={area.icon} size={23}/></span>
        <span className="admin-dashboard-card-copy"><strong>{area.title}</strong><span>{area.description}</span></span>
        <span className="admin-dashboard-card-arrow"><Icon name="arrow" size={18}/></span>
      </a>)}
    </div>
    <section className="panel user-panel admin-recent">
      <div className="table-toolbar"><h2>Tài liệu chờ xét duyệt</h2><a className="button secondary compact" href="#admin/documents"><Icon name="eye" size={16}/>Quản lý tài liệu</a></div>
      {pending.loading?<p role="status">Đang tải tài liệu…</p>:pending.error?<p role="alert">{pending.error}</p>:!pending.data?.content.length?<p className="muted">Hiện không có tài liệu chờ xét duyệt.</p>:<div className="table-scroll"><table><caption className="sr-only">Tài liệu đang chờ quản trị viên duyệt</caption><thead><tr><th>TÊN TÀI LIỆU</th><th>TÁC GIẢ</th><th>CHỦ ĐỀ</th><th>TRẠNG THÁI</th></tr></thead><tbody>{pending.data.content.map(item=><tr key={item.document.id}><td>{item.document.title}</td><td>{item.author}</td><td>{item.document.category}</td><td><span className="document-status pending">{documentStatuses[item.status]}</span></td></tr>)}</tbody></table></div>}
    </section>
    <p className="admin-dashboard-footnote"><Icon name="lock" size={15}/>Khu vực này chỉ dành cho tài khoản có quyền quản trị viên.</p>
  </div>;
}
