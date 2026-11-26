import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import Icon from '../../../shared/components/ui/Icon';
import { DocumentDescription } from '../components/DocumentDescription';
import type { Account } from '../../auth/types';
import type { ManagedDocument, PageData, DocumentDetail } from '../types';
import { documentStatuses, levelNames } from '../types';
import { useRemote } from '../../../shared/hooks/useRemote';
import { mutate, errorMessage } from '../../../shared/api/http-client';
import { RemoteState, coverFor } from '../components/LibraryUI';
import DocumentPreview from '../components/DocumentPreview';
import UploadPage from './UploadPage';
import {
  AdminDialog,
  Card,
  EmptyState,
  IconAction,
  LoadingRows,
  Notice,
  PageHeader,
  Pager,
  shortDate,
  useAutoClear,
} from '../../admin/components/AdminUI';

/*
 * Màn hình quản lý tài liệu. Dùng chung cho:
 *  - Thành viên: "Tài liệu của tôi" (#my-documents) -> xem, sửa, xoá tài liệu mình đăng
 *  - Quản trị viên: "Kho tài liệu" (#admin/documents) -> thêm duyệt, từ chối, ẩn, công bố lại, cấp quyền
 */

// Các thao tác có thể làm với một tài liệu
type Kind = 'edit' | 'view' | 'DELETE' | 'APPROVE' | 'REJECT' | 'HIDE' | 'RESTORE' | 'GRANT';
type Action = { item: ManagedDocument; kind: Kind };

const STATUS_TABS = ['PENDING', 'PUBLISHED', 'REJECTED', 'ARCHIVED', 'DRAFT'];

// Tiêu đề hộp thoại
const DIALOG_TITLES: Record<Kind, string> = {
  edit: 'Chỉnh sửa tài liệu',
  view: 'Chi tiết và xem trước',
  DELETE: 'Xóa tài liệu',
  APPROVE: 'Duyệt tài liệu',
  REJECT: 'Từ chối tài liệu',
  HIDE: 'Ẩn tài liệu',
  RESTORE: 'Công bố lại tài liệu',
  GRANT: 'Cấp quyền tài liệu có phí',
};

// Chữ trên nút xác nhận
const CONFIRM_LABELS: Partial<Record<Kind, string>> = {
  DELETE: 'Xóa tài liệu',
  APPROVE: 'Duyệt & công bố',
  REJECT: 'Từ chối',
  HIDE: 'Ẩn tài liệu',
  RESTORE: 'Công bố lại',
  GRANT: 'Cấp quyền',
};

// Giải thích cho từng thao tác
const EXPLANATIONS: Partial<Record<Kind, string>> = {
  DELETE: 'Tài liệu sẽ được xóa mềm khỏi thư viện. Lịch sử tải được giữ lại.',
  HIDE: 'Tài liệu sẽ tạm ngừng xuất hiện và không thể tải công khai.',
  APPROVE: 'Xác nhận đã kiểm tra nội dung và cho phép công bố tài liệu này.',
  RESTORE: 'Tài liệu đã ẩn sẽ được công bố trở lại.',
  GRANT: 'Nhập ID thành viên cần cấp quyền tải. Tra ID tại mục Người dùng → Chi tiết.',
  REJECT: 'Nhập lý do để tác giả biết cần chỉnh sửa nội dung nào.',
};

// Thông báo sau khi làm xong
function successMessage(kind: Kind) {
  if (kind === 'APPROVE') return 'Đã duyệt và công bố tài liệu.';
  if (kind === 'REJECT') return 'Đã từ chối tài liệu và gửi lý do cho tác giả.';
  if (kind === 'DELETE') return 'Đã xóa tài liệu.';
  return 'Đã cập nhật tài liệu.';
}

// =========================================================
// PHẦN 1: HỘP THOẠI THAO TÁC
// =========================================================

type DocumentActionProps = {
  action: Action;
  user: Account;
  adminMode: boolean;
  onClose: () => void;
  onSaved: (message: string) => void;
  onSwitch: (kind: Kind) => void; // Chuyển sang thao tác khác (ví dụ từ Xem -> Duyệt)
};

function DocumentAction({ action, user, adminMode, onClose, onSaved, onSwitch }: DocumentActionProps) {
  const { item, kind } = action;
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  // Chỉ tải chi tiết khi mở hộp thoại "Xem"
  const detail = useRemote<DocumentDetail>(kind === 'view' ? `/documents/${item.document.id}` : null);

  /** Gửi thao tác xoá / cấp quyền / duyệt / từ chối / ẩn / công bố lại. */
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const id = item.document.id;

    setBusy(true);
    setError('');
    try {
      if (kind === 'DELETE') {
        const base = adminMode ? 'admin' : 'users/me';
        await mutate(`/${base}/documents/${id}?version=${item.version}`, 'DELETE');
      } else if (kind === 'GRANT') {
        await mutate(`/admin/documents/${id}/access`, 'POST', { userId: Number(form.get('userId')) });
      } else {
        // APPROVE, REJECT, HIDE, RESTORE đều gọi chung API review
        await mutate(`/admin/documents/${id}/review`, 'POST', {
          version: item.version,
          action: kind,
          note: form.get('note') || null,
        });
      }
      onSaved(successMessage(kind));
      onClose();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  // ----- Hộp thoại sửa tài liệu -----
  if (kind === 'edit') {
    return (
      <AdminDialog title={DIALOG_TITLES.edit} subtitle={item.document.title} wide onClose={onClose} busy={busy}>
        <UploadPage
          user={user}
          initial={item}
          adminMode={adminMode}
          onUploaded={() => {
            onSaved(adminMode ? 'Đã lưu thay đổi.' : 'Đã lưu và gửi tài liệu duyệt lại.');
            onClose();
          }}
        />
      </AdminDialog>
    );
  }

  // ----- Hộp thoại xem chi tiết -----
  if (kind === 'view') {
    let footer = undefined;
    if (adminMode && item.status === 'PENDING') {
      footer = (
        <>
          <span className="adm-muted adm-foot-hint">Đã kiểm tra nội dung?</span>
          <button className="button secondary adm-reject" onClick={() => onSwitch('REJECT')}>
            <Icon name="close" size={15} />
            Từ chối
          </button>
          <button className="button primary" onClick={() => onSwitch('APPROVE')}>
            <Icon name="check" size={15} />
            Duyệt
          </button>
        </>
      );
    }

    return (
      <AdminDialog
        title={item.document.title}
        subtitle={
          <>
            {item.author} · {documentStatuses[item.status]} · {shortDate(item.createdAt)}
          </>
        }
        wide
        onClose={onClose}
        footer={footer}
      >
        {item.reviewNote && <Notice tone="error">Ghi chú duyệt: {item.reviewNote}</Notice>}
        <DocumentDescription source={item.description} />
        <RemoteState loading={detail.loading} error={detail.error} onRetry={detail.refresh} />
        {detail.data && <DocumentPreview detail={detail.data} />}
      </AdminDialog>
    );
  }

  // ----- Hộp thoại xác nhận (xoá, duyệt, từ chối...) -----
  const isDanger = kind === 'DELETE' || kind === 'REJECT';
  let confirmIcon = 'check';
  if (kind === 'DELETE') confirmIcon = 'trash';
  else if (kind === 'REJECT') confirmIcon = 'close';

  const footer = (
    <>
      <button className="button secondary" type="button" disabled={busy} onClick={onClose}>
        Hủy
      </button>
      <button className={isDanger ? 'button danger' : 'button primary'} type="submit" form="adm-doc-action" disabled={busy}>
        <Icon name={confirmIcon} size={15} />
        {busy ? 'Đang lưu…' : CONFIRM_LABELS[kind]}
      </button>
    </>
  );

  return (
    <AdminDialog title={DIALOG_TITLES[kind]} subtitle={item.document.title} onClose={onClose} busy={busy} footer={footer}>
      {error && <Notice tone="error">{error}</Notice>}

      <form id="adm-doc-action" className="adm-form" onSubmit={submit}>
        <div className={isDanger ? 'adm-warning' : 'adm-warning info'}>
          <Icon name={isDanger ? 'shield' : 'check'} size={18} />
          <p>{EXPLANATIONS[kind]}</p>
        </div>

        {/* Cấp quyền: nhập ID người dùng */}
        {kind === 'GRANT' && (
          <label className="field">
            ID người dùng
            <input type="number" name="userId" min={1} required autoFocus />
          </label>
        )}

        {/* Duyệt / từ chối / ẩn / công bố lại: nhập ghi chú (từ chối thì bắt buộc) */}
        {kind !== 'GRANT' && kind !== 'DELETE' && (
          <label className="field">
            {kind === 'REJECT' ? 'Lý do từ chối' : 'Ghi chú (không bắt buộc)'}
            <textarea
              name="note"
              rows={3}
              maxLength={2000}
              required={kind === 'REJECT'}
              autoFocus={kind === 'REJECT'}
              placeholder={kind === 'REJECT' ? 'Ví dụ: Thiếu nguồn tham khảo, tệp bị lỗi trang 3…' : ''}
            />
          </label>
        )}
      </form>
    </AdminDialog>
  );
}

// =========================================================
// PHẦN 2: MỘT DÒNG TÀI LIỆU TRONG DANH SÁCH
// =========================================================

type RowProps = {
  item: ManagedDocument;
  adminMode: boolean;
  onOpen: (item: ManagedDocument, kind: Kind) => void;
};

function DocumentRow({ item, adminMode, onOpen }: RowProps) {
  const d = item.document;
  const statusClass = 's-' + item.status.toLowerCase();
  const isPublished = item.status === 'PUBLISHED';

  return (
    <li className={`adm-doc ${statusClass}`}>
      <button type="button" className="adm-doc-cover" aria-label={`Xem trước ${d.title}`} onClick={() => onOpen(item, 'view')}>
        <img src={`/covers/${coverFor(d)}`} alt="" loading="lazy" />
        <span>{d.fileFormat || 'MD'}</span>
      </button>

      <div className="adm-doc-body">
        <div className="adm-doc-top">
          <span className={`adm-doc-status ${statusClass}`}>{documentStatuses[item.status]}</span>
          {d.accessType === 'PAID' && <span className="adm-chip">Có phí</span>}
          <span className="adm-muted">{shortDate(item.updatedAt || item.createdAt)}</span>
        </div>

        <h3>
          <button type="button" onClick={() => onOpen(item, 'view')}>
            {d.title}
          </button>
        </h3>

        <p className="adm-doc-meta">
          {adminMode && (
            <>
              <Icon name="user" size={13} />
              {item.author}
              <i />
            </>
          )}
          {d.subjectName || d.category}
          <i />
          {levelNames[d.level] || d.level}
          {d.institutionName && (
            <>
              <i />
              {d.institutionName}
            </>
          )}
        </p>

        {/* Chỉ hiện lý do khi bị từ chối (ghi chú lúc duyệt không phải là lỗi) */}
        {item.reviewNote && item.status === 'REJECTED' && (
          <p className="adm-doc-note">
            <Icon name="close" size={13} />
            {item.reviewNote}
          </p>
        )}

        {d.tags && d.tags.length > 0 && (
          <div className="adm-tags">
            {d.tags.slice(0, 5).map((t) => (
              <span key={t}>#{t}</span>
            ))}
          </div>
        )}
      </div>

      <div className="adm-doc-actions">
        {/* Nút Duyệt / Từ chối cho tài liệu đang chờ */}
        {adminMode && item.status === 'PENDING' && (
          <div className="adm-review">
            <button className="button primary compact" onClick={() => onOpen(item, 'APPROVE')}>
              <Icon name="check" size={14} />
              Duyệt
            </button>
            <button className="button secondary compact adm-reject" onClick={() => onOpen(item, 'REJECT')}>
              <Icon name="close" size={14} />
              Từ chối
            </button>
          </div>
        )}

        <div className="adm-row-actions">
          <IconAction icon="eye" label="Xem trước" onClick={() => onOpen(item, 'view')} />
          <IconAction icon="edit" label="Sửa" onClick={() => onOpen(item, 'edit')} />
          {adminMode && isPublished && <IconAction icon="eyeOff" label="Ẩn tài liệu" onClick={() => onOpen(item, 'HIDE')} />}
          {adminMode && item.status === 'ARCHIVED' && (
            <IconAction icon="refresh" label="Công bố lại" tone="success" onClick={() => onOpen(item, 'RESTORE')} />
          )}
          {adminMode && isPublished && d.accessType === 'PAID' && (
            <IconAction icon="lock" label="Cấp quyền tải" onClick={() => onOpen(item, 'GRANT')} />
          )}
          <IconAction icon="trash" label="Xóa" tone="danger" onClick={() => onOpen(item, 'DELETE')} />
        </div>
      </div>
    </li>
  );
}

// =========================================================
// PHẦN 3: TRANG CHÍNH
// =========================================================

/** Đếm số tài liệu theo trạng thái (chỉ lấy 1 phần tử để đọc totalElements). */
function useCount(base: string, status: string, tick: number) {
  return useRemote<PageData<ManagedDocument>>(`${base}?status=${status}&size=1&_=${tick}`);
}

type Props = {
  user: Account;
  adminMode?: boolean;
  initialStatus?: string;
  embedded?: boolean; // Nằm trong tab của trang Quản lý cá nhân -> không hiện tiêu đề trang
};

export default function ManageDocumentsPage({ user, adminMode = false, initialStatus = '', embedded = false }: Props) {
  const base = adminMode ? '/admin/documents' : '/users/me/documents';

  const [status, setStatus] = useState(initialStatus);
  const [q, setQ] = useState(''); // Từ khoá đang tìm
  const [draft, setDraft] = useState(''); // Chữ đang gõ trong ô tìm
  const [page, setPage] = useState(0);
  const [tick, setTick] = useState(0); // Tăng lên để tải lại dữ liệu
  const [action, setAction] = useState<Action | null>(null);
  const [notice, setNotice] = useState('');
  useAutoClear(notice, () => setNotice(''));

  // Gõ xong 350ms mới tìm
  useEffect(() => {
    const timer = setTimeout(() => {
      if (draft.trim() !== q) {
        setQ(draft.trim());
        setPage(0);
      }
    }, 350);
    return () => clearTimeout(timer);
  }, [draft, q]);

  const resource = useRemote<PageData<ManagedDocument>>(
    `${base}?status=${status}&q=${encodeURIComponent(q)}&page=${page}&size=10&_=${tick}`,
  );

  // Số lượng trên mỗi tab
  const all = useCount(base, '', tick);
  const pending = useCount(base, 'PENDING', tick);
  const published = useCount(base, 'PUBLISHED', tick);
  const rejected = useCount(base, 'REJECTED', tick);
  const archived = useCount(base, 'ARCHIVED', tick);
  const draftCount = useCount(base, 'DRAFT', tick);
  const counts: Record<string, PageData<ManagedDocument> | null> = {
    PENDING: pending.data,
    PUBLISHED: published.data,
    REJECTED: rejected.data,
    ARCHIVED: archived.data,
    DRAFT: draftCount.data,
  };

  // Tab "Bản nháp" chỉ hiện khi có bản nháp
  const tabs = STATUS_TABS.filter((key) => {
    if (key !== 'DRAFT') return true;
    return (counts.DRAFT?.totalElements || 0) > 0 || status === 'DRAFT';
  });

  function refresh() {
    setTick((t) => t + 1);
  }

  function openAction(item: ManagedDocument, kind: Kind) {
    setAction({ item, kind });
  }

  function selectStatus(next: string) {
    setStatus(next);
    setPage(0);
  }

  // ----- Nội dung danh sách -----
  let listContent;
  if (resource.loading && !resource.data) {
    listContent = <LoadingRows />;
  } else if (resource.error) {
    listContent = (
      <EmptyState
        icon="refresh"
        title="Chưa tải được tài liệu"
        text={resource.error}
        action={
          <button className="button secondary compact" onClick={refresh}>
            Thử lại
          </button>
        }
      />
    );
  } else if (!resource.data || resource.data.content.length === 0) {
    let emptyTitle = 'Chưa có tài liệu';
    let emptyText = adminMode ? 'Thay đổi bộ lọc hoặc thêm giáo trình mới.' : 'Tài liệu bạn đóng góp sẽ được quản lý tại đây.';
    if (q) {
      emptyTitle = 'Không tìm thấy tài liệu';
      emptyText = `Không có kết quả cho “${q}”.`;
    } else if (status) {
      emptyTitle = `Không có tài liệu “${documentStatuses[status]}”`;
    }
    listContent = <EmptyState icon={q ? 'search' : 'book'} title={emptyTitle} text={emptyText} />;
  } else {
    listContent = (
      <ul className={resource.loading ? 'adm-doc-list is-loading' : 'adm-doc-list'}>
        {resource.data.content.map((item) => (
          <DocumentRow key={item.document.id} item={item} adminMode={adminMode} onOpen={openAction} />
        ))}
      </ul>
    );
  }

  return (
    <section className={embedded ? 'adm-docs adm-docs-embedded' : 'adm-docs'}>
      {embedded ? (
        <div className="adm-docs-embedded-head">
          <p className="muted small">Tài liệu mới và tài liệu bạn chỉnh sửa cần được duyệt trước khi công bố.</p>
          <a className="button primary compact" href="#upload">
            <Icon name="plus" size={15} />
            Đăng tài liệu
          </a>
        </div>
      ) : (
        <PageHeader
          kicker={adminMode ? 'QUẢN LÝ' : 'ĐÓNG GÓP CỦA BẠN'}
          title={adminMode ? 'Kho tài liệu' : 'Tài liệu của tôi'}
          description={
            adminMode
              ? 'Kiểm tra nội dung, duyệt và quản lý toàn bộ tài liệu trong thư viện.'
              : 'Tài liệu mới và tài liệu bạn chỉnh sửa cần được duyệt trước khi công bố.'
          }
          actions={
            <a className="button primary" href={adminMode ? '#admin/documents/new' : '#upload'}>
              <Icon name="plus" size={16} />
              {adminMode ? 'Thêm giáo trình' : 'Đăng tài liệu'}
            </a>
          }
        />
      )}

      {notice && <Notice onClose={() => setNotice('')}>{notice}</Notice>}

      <Card>
        <div className="adm-docs-toolbar">
          {/* Các tab trạng thái */}
          <div className="adm-tabs" role="tablist" aria-label="Lọc theo trạng thái">
            <button role="tab" aria-selected={!status} onClick={() => selectStatus('')}>
              Tất cả<span>{all.data?.totalElements ?? '…'}</span>
            </button>
            {tabs.map((key) => (
              <button
                key={key}
                role="tab"
                aria-selected={status === key}
                className={`s-${key.toLowerCase()}`}
                onClick={() => selectStatus(key)}
              >
                {documentStatuses[key]}
                <span>{counts[key]?.totalElements ?? '…'}</span>
              </button>
            ))}
          </div>

          {/* Ô tìm và nút làm mới */}
          <div className="adm-docs-search">
            <label className="adm-search">
              <Icon name="search" size={16} />
              <input
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                maxLength={150}
                placeholder="Tìm theo tên hoặc tóm tắt…"
                aria-label="Tìm tài liệu"
              />
              {draft && (
                <button type="button" aria-label="Xóa tìm kiếm" onClick={() => setDraft('')}>
                  <Icon name="close" size={14} />
                </button>
              )}
            </label>
            <IconAction icon="refresh" label="Làm mới" onClick={refresh} />
          </div>
        </div>

        {listContent}

        {resource.data && resource.data.totalElements > 0 && (
          <Pager
            page={page}
            totalPages={resource.data.totalPages}
            total={resource.data.totalElements}
            unit="tài liệu"
            disabled={resource.loading}
            onPage={setPage}
          />
        )}
      </Card>

      {action && (
        <DocumentAction
          key={action.item.document.id + action.kind}
          action={action}
          user={user}
          adminMode={adminMode}
          onClose={() => setAction(null)}
          onSaved={(message) => {
            setNotice(message);
            refresh();
          }}
          onSwitch={(kind) => setAction({ item: action.item, kind })}
        />
      )}
    </section>
  );
}
