import Icon from '../../../shared/components/ui/Icon';
import type { DocumentItem, PageData } from '../types';
import { levelNames,priceLabel,formatName } from '../types';
const generatedCovers:Record<string,string>={
 'ke-hoach-tu-hoc':'ke-hoach-tu-hoc.png',
 'rest-api-checklist':'rest-api-checklist.png',
 'git-lam-viec-nhom':'git-lam-viec-nhom.png',
 'java-can-ban':'java-can-ban.png',
 'mysql-tieng-viet':'mysql-tieng-viet.png',
 'react-typescript':'react-typescript.png',
 'spring-boot-chuyen-sau':'spring-boot-chuyen-sau.png',
};
function coverIcon(slug:string,title:string,category:string,size:number){
 const value=`${slug} ${title} ${category}`.toLocaleLowerCase('vi');
 const path=value.includes('git')?'M7 4a2 2 0 1 0 0 4 2 2 0 0 0 0-4Zm10 12a2 2 0 1 0 0 4 2 2 0 0 0 0-4ZM7 8v8a4 4 0 0 0 4 4h4 M17 8a2 2 0 1 0 0-4 2 2 0 0 0 0 4Zm0 0v8':value.includes('rest')||value.includes('api')?'M8 6 3 12l5 6 M16 6l5 6-5 6 M14 3l-4 18':value.includes('ke-hoach')||value.includes('tu-hoc')||value.includes('study')?'M4 5h16v16H4z M8 3v4 M16 3v4 M4 10h16 M8 14h2 M14 14h2 M8 18h2 M14 18h2':value.includes('database')||value.includes('mysql')?'M12 3c-4 0-7 1.3-7 3s3 3 7 3 7-1.3 7-3-3-3-7-3ZM5 6v12c0 1.7 3 3 7 3s7-1.3 7-3V6 M5 12c0 1.7 3 3 7 3s7-1.3 7-3':'M6 4h12v16H6z M9 8h6 M9 12h6 M9 16h4';
 return <svg className="cover-book-icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={path}/></svg>;
}
export function DocumentCover({ doc, large=false }: {doc:DocumentItem;large?:boolean}) {
 const theme=['violet','peach','mint','sky','rose','sand'].includes(doc.coverStyle)?doc.coverStyle:'violet';
 const image=generatedCovers[doc.slug];
 return <div className={`document-cover cover-${theme}${large?' cover-large':''}`} aria-hidden="true">{image?<><img className="document-cover-image" src={`/covers/${image}`} alt=""/><span className="document-cover-title">{doc.title}</span></>:<><span className="cover-label">KNOWLEDGE HUB / COLLECTION</span><div className="cover-book">{coverIcon(doc.slug,doc.title,doc.category,large?65:42)}<span>{doc.category}</span><strong>{doc.title.split(':')[0]}</strong><i/></div><span className="cover-orbit"/></>}<span className="cover-format">{doc.fileFormat||'MD'}</span></div>;
}export function DocumentCard({ doc }: {doc:DocumentItem}) {
 return <article className="document-card"><a className="document-card-link" href={`#document/${doc.id}`} aria-label={`Xem tài liệu: ${doc.title}`}><DocumentCover doc={doc}/><div className="document-card-body"><div className="card-topline"><span className="category-label">{doc.category}</span><span className={`access-badge ${doc.accessType==='FREE'?'free':'paid'}`}>{doc.accessType==='FREE'?'Miễn phí':'Có phí'}</span></div><h3>{doc.title}</h3><p>{doc.summary}</p>{doc.institutionName&&<div className="document-organization">{doc.institutionName}{doc.facultyName&&<span> · {doc.facultyName}</span>}</div>}<div className="card-meta"><span>{formatName(doc.fileFormat||'MD')} · {levelNames[doc.level]||doc.level}</span><span><Icon name="download" size={13}/>{doc.downloads}</span></div><div className="card-bottom"><strong className={doc.accessType==='FREE'?'free-price':''}>{priceLabel(doc)}</strong><span>Xem tài liệu <Icon name="arrow" size={14}/></span></div></div></a></article>;
}
export function DocumentGrid({documents}:{documents:DocumentItem[]}){return <div className="document-grid">{documents.map(doc=><DocumentCard key={doc.id} doc={doc}/>)}</div>;}
export function RemoteState({loading,error,onRetry}:{loading:boolean;error:string;onRetry:()=>void}){if(loading)return <div className="library-loading" role="status"><span className="spinner"/>Đang tìm những tài liệu dành cho bạn…</div>;if(error)return <div className="library-empty" role="alert"><Icon name="book" size={32}/><h3>Chưa tải được nội dung</h3><p>{error}</p><button className="button secondary" onClick={onRetry}><Icon name="refresh" size={16}/>Thử lại</button></div>;return null;}
export function Pagination({data,page,onPage}:{data:Pick<PageData<unknown>,'totalElements'|'totalPages'>;page:number;onPage:(page:number)=>void}){return <div className="library-pagination"><span>{data.totalElements} kết quả</span><div><button className="button secondary compact" disabled={page===0} onClick={()=>onPage(page-1)}><Icon name="chevronLeft" size={16}/>Trước</button><span>Trang {page+1} / {Math.max(1,data.totalPages)}</span><button className="button secondary compact" disabled={page+1>=data.totalPages} onClick={()=>onPage(page+1)}><Icon name="chevronRight" size={16}/>Sau</button></div></div>;}