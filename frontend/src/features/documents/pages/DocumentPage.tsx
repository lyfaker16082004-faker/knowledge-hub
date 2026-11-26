import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import type { Account } from '../../auth/types';
import { useRemote } from '../../../shared/hooks/useRemote';
import { downloadFile, mutate, errorMessage } from '../../../shared/api/http-client';
import Icon from '../../../shared/components/ui/Icon';
import type { DocumentDetail } from '../types';
import { priceLabel, fileSize, levelNames, formatName, documentStatuses } from '../types';
import { DocumentCover, RemoteState } from '../components/LibraryUI';
import DocumentPreview from '../components/DocumentPreview';
import { DocumentDescription } from '../components/DocumentDescription';
import RelatedDocuments from '../components/RelatedDocuments';

type Props = { id: number; user: Account | null; requireLogin: () => void };

/** Ngày dạng dd/mm/yyyy. Backend trả thời gian UTC không có "Z" nên phải thêm vào. */
function dateOnly(value: string | null) {
  if (!value) return 'Chưa công bố';
  return new Date(value + (/[Z+]/.test(value) ? '' : 'Z')).toLocaleDateString('vi-VN');
}

const languageNames: Record<string, string> = { vi: 'Tiếng Việt', en: 'Tiếng Anh' };

export default function DocumentPage({ id, user, requireLogin }: Props) {
  const resource = useRemote<DocumentDetail>(`/documents/${id}`, String(user?.id || 0));
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  // Khi bấm sang một tài liệu liên quan, cuộn lên đầu trang
  useEffect(() => { window.scrollTo({ top: 0 }); }, [id]);

  async function download(fileId: number, fileName: string) {
    if (!user) { requireLogin(); return; }
    setBusy(true); setError(''); setNotice('');
    try {
      await downloadFile(id, fileId, fileName);
      setNotice('Tệp đã được gửi đến trình duyệt. Bạn có thể tìm lại trong Trang của tôi.');
      resource.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function toggleSave() {
    if (!user) { requireLogin(); return; }
    const wasSaved = !!resource.data?.saved;
    setBusy(true); setError(''); setNotice('');
    try {
      await mutate(`/users/me/bookmarks/${id}`, 'POST', { saved: !wasSaved });
      setNotice(wasSaved ? 'Đã bỏ lưu tài liệu.' : 'Đã lưu vào góc học tập của bạn.');
      resource.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  const detail = resource.data;

  return (
    <div className="dd-page">
      <a href="#documents" className="back-link dd-back"><Icon name="chevronLeft" size={16} />Trở lại thư viện</a>
      <RemoteState loading={resource.loading} error={resource.error} onRetry={resource.refresh} />

      {detail && <>
        <div className="dd-layout">
          <div className="dd-main">
            <DocumentHeader detail={detail} user={user} />

            <section className="dd-section">
              <h2>Về tài liệu này</h2>
              <CollapsibleDescription source={detail.description} />
            </section>

            <section className="dd-section">
              <h2>Tệp đính kèm <span className="dd-count">{detail.files.length}</span></h2>
              <FileList detail={detail} canDownload={!!user && detail.canDownload} busy={busy} onDownload={download} />
            </section>

            {(detail.canPreview || detail.status === 'PUBLISHED') && <DocumentPreview detail={detail} />}
          </div>

          <DownloadPanel detail={detail} user={user} busy={busy} notice={notice} error={error}
            onDownload={() => {
              if (!user) requireLogin();
              else if (detail.files[0]) download(detail.files[0].id, detail.files[0].name);
            }}
            onToggleSave={toggleSave} />
        </div>

        <RelatedDocuments document={detail.document} />
      </>}
    </div>
  );
}

/* =========================================================
   PHẦN ĐẦU TRANG: breadcrumb, tiêu đề, tác giả, nhãn
   ========================================================= */
function DocumentHeader({ detail, user }: { detail: DocumentDetail; user: Account | null }) {
  const doc = detail.document;
  const isFree = doc.accessType === 'FREE';
  const manageLink = user?.roles.includes('ADMIN') ? '#admin/documents' : '#mine?tab=uploads';

  return (
    <header className="dd-header">
      <nav className="dd-breadcrumb" aria-label="Đường dẫn">
        <a href="#documents">Thư viện</a>
        <Icon name="chevronRight" size={14} />
        <a href={`#documents?category=${doc.categoryId}`}>{doc.category}</a>
      </nav>

      <div className="dd-badges">
        <span className={`dd-badge ${isFree ? 'is-free' : 'is-paid'}`}>{isFree ? 'Miễn phí' : 'Có phí'}</span>
        <span className="dd-badge">{levelNames[doc.level] || doc.level}</span>
        <span className="dd-badge">{formatName(doc.fileFormat || 'MD')}</span>
      </div>

      <h1>{doc.title}</h1>
      <p className="dd-summary">{doc.summary}</p>

      {detail.status !== 'PUBLISHED' && (
        <div className="dd-status" role="status">
          <Icon name="clock" size={16} />
          <span><strong>{documentStatuses[detail.status] || detail.status}</strong>{detail.reviewNote && ` · ${detail.reviewNote}`}</span>
        </div>
      )}

      <div className="dd-meta">
        <div className="dd-author">
          <span className="dd-avatar" aria-hidden="true">{detail.author.charAt(0).toUpperCase()}</span>
          <div>
            <strong>{detail.author}</strong>
            <span>Cập nhật {dateOnly(doc.publishedAt)}</span>
          </div>
        </div>
        <span className="dd-meta-item"><Icon name="download" size={16} />{doc.downloads} lượt tải</span>
        <span className="dd-meta-item"><Icon name="book" size={16} />{detail.files.length} tệp</span>
        {detail.canManage && <a className="button secondary compact dd-manage" href={manageLink}><Icon name="edit" size={15} />Quản lý</a>}
      </div>

      {(doc.institutionName || doc.subjectName) && (
        <div className="dd-chips">
          {doc.institutionName && (
            <span className="dd-chip"><Icon name="institution" size={15} />{doc.institutionName}{doc.facultyName && ` · ${doc.facultyName}`}</span>
          )}
          {doc.subjectName && <span className="dd-chip"><Icon name="bulb" size={15} />{doc.subjectName}</span>}
        </div>
      )}

      {doc.tags?.length > 0 && (
        <div className="dd-tags">
          {doc.tags.map(tag => <a key={tag} href={`#documents?tag=${encodeURIComponent(tag)}`}>#{tag}</a>)}
        </div>
      )}
    </header>
  );
}

/* =========================================================
   MÔ TẢ: tự thu gọn khi quá dài, có nút "Xem thêm"
   ========================================================= */
const COLLAPSED_HEIGHT = 360;

function CollapsibleDescription({ source }: { source: string }) {
  const contentRef = useRef<HTMLDivElement>(null);
  const [isLong, setIsLong] = useState(false);
  const [expanded, setExpanded] = useState(false);

  // Đo chiều cao thật của nội dung sau khi hiển thị
  useLayoutEffect(() => {
    const height = contentRef.current?.scrollHeight || 0;
    setIsLong(height > COLLAPSED_HEIGHT + 80);
  }, [source]);

  const collapsed = isLong && !expanded;

  return (
    <div className={`dd-description${collapsed ? ' is-collapsed' : ''}`}>
      <div ref={contentRef} className="dd-description-body" style={collapsed ? { maxHeight: COLLAPSED_HEIGHT } : undefined}>
        <DocumentDescription source={source} />
      </div>
      {isLong && (
        <button type="button" className="dd-more" onClick={() => setExpanded(!expanded)} aria-expanded={expanded}>
          {expanded ? 'Thu gọn' : 'Xem thêm'}
          <Icon name={expanded ? 'up' : 'down'} size={15} />
        </button>
      )}
    </div>
  );
}

/* =========================================================
   DANH SÁCH TỆP ĐÍNH KÈM
   ========================================================= */
type FileListProps = {
  detail: DocumentDetail;
  canDownload: boolean;
  busy: boolean;
  onDownload: (fileId: number, fileName: string) => void;
};

function FileList({ detail, canDownload, busy, onDownload }: FileListProps) {
  if (!detail.files.length) return <p className="dd-muted">Tài liệu chưa có tệp đính kèm.</p>;

  return (
    <ul className="dd-files">
      {detail.files.map(file => {
        const extension = file.name.includes('.') ? file.name.split('.').pop()!.toUpperCase() : 'FILE';
        return (
          <li key={file.id} className="dd-file">
            <span className="dd-file-icon">{extension}</span>
            <div className="dd-file-info">
              <strong title={file.name}>{file.name}</strong>
              <small>{fileSize(file.sizeBytes)} · {formatName(extension)}</small>
            </div>
            {canDownload && (
              <button className="button secondary compact" disabled={busy} onClick={() => onDownload(file.id, file.name)}
                aria-label={`Tải file ${file.name}`}>
                <Icon name="download" size={16} />Tải
              </button>
            )}
          </li>
        );
      })}
    </ul>
  );
}

/* =========================================================
   CỘT BÊN PHẢI: giá, nút tải, lưu, thông tin nhanh
   ========================================================= */
type DownloadPanelProps = {
  detail: DocumentDetail;
  user: Account | null;
  busy: boolean;
  notice: string;
  error: string;
  onDownload: () => void;
  onToggleSave: () => void;
};

function DownloadPanel({ detail, user, busy, notice, error, onDownload, onToggleSave }: DownloadPanelProps) {
  const doc = detail.document;
  const isFree = doc.accessType === 'FREE';
  const totalSize = detail.files.reduce((sum, file) => sum + file.sizeBytes, 0);

  // Chữ trên nút tải thay đổi theo từng trường hợp
  let downloadLabel = 'Tải tài liệu';
  if (busy) downloadLabel = 'Đang xử lý…';
  else if (detail.status !== 'PUBLISHED') downloadLabel = 'Chưa công bố';
  else if (!user) downloadLabel = 'Đăng nhập để tải';
  else if (!detail.canDownload) downloadLabel = 'Cần quyền truy cập';

  const downloadDisabled = busy || (!!user && !detail.canDownload) || !detail.files.length;

  return (
    <aside className="dd-aside">
      <div className="dd-panel">
        <DocumentCover doc={doc} large />

        <div className="dd-panel-body">
          <span className="dd-panel-label">Giá tài liệu</span>
          <strong className={`dd-price${isFree ? ' is-free' : ''}`}>{priceLabel(doc)}</strong>
          <p className="dd-muted">{isFree ? 'Tải miễn phí sau khi đăng nhập.' : 'Tài liệu có phí, cần được cấp quyền trước khi tải.'}</p>

          <button className="button primary full" disabled={downloadDisabled} onClick={onDownload}>
            <Icon name={detail.canDownload || !user ? 'download' : 'lock'} size={18} />{downloadLabel}
          </button>
          <button className="button secondary full" disabled={busy} onClick={onToggleSave} aria-pressed={detail.saved}>
            <Icon name={detail.saved ? 'check' : 'bookmark'} size={18} />{detail.saved ? 'Đã lưu' : 'Lưu để đọc sau'}
          </button>

          {/* Thông báo hiện ngay dưới nút bấm để người dùng thấy kết quả */}
          {error && <div className="dd-alert is-error" role="alert">{error}</div>}
          {notice && <div className="dd-alert" role="status">{notice}</div>}

          {!isFree && !detail.canDownload && (
            <div className="paid-note">Thanh toán trực tuyến chưa được mở. Liên hệ quản trị viên để được cấp quyền truy cập.</div>
          )}

          <dl className="dd-facts">
            <div><dt>Định dạng</dt><dd>{formatName(doc.fileFormat || 'MD')}</dd></div>
            <div><dt>Dung lượng</dt><dd>{totalSize ? fileSize(totalSize) : '—'}</dd></div>
            <div><dt>Trình độ</dt><dd>{levelNames[doc.level] || doc.level}</dd></div>
            <div><dt>Ngôn ngữ</dt><dd>{languageNames[doc.language] || doc.language}</dd></div>
            <div><dt>Lượt tải</dt><dd>{doc.downloads}</dd></div>
            <div><dt>Ngày đăng</dt><dd>{dateOnly(doc.publishedAt)}</dd></div>
          </dl>

          <div className="dd-assurance"><Icon name="shield" size={15} /><span>Tệp được kiểm tra quyền truy cập khi tải.</span></div>
        </div>
      </div>
    </aside>
  );
}
