import { useEffect, useState } from 'react';
import { previewFile, get, errorMessage } from '../../../shared/api/http-client';
import Icon from '../../../shared/components/ui/Icon';
import type { DocumentDetail } from '../types';
import { fileSize } from '../types';
import { KIND_INFO, PdfViewer, TextPreview, fileKind } from './PreviewViewers';
import type { FileKind } from './PreviewViewers';
import '../../../styles/document-preview.css';

/*
 * Khu vực "Đọc thử tài liệu" trong trang chi tiết.
 * - Tài liệu đã công bố: ai cũng xem được bản thử (2 trang đầu PDF hoặc 2.400 ký tự đầu).
 *   Bản thử của tệp đầu tiên được mở sẵn khi vào trang.
 * - Tác giả và quản trị viên: được mở bản xem trước đầy đủ.
 * - Mỗi loại file có khung xem riêng (xem PreviewViewers.tsx).
 */

// Các loại cần backend trích chữ khi xem bản đầy đủ
const NEEDS_TEXT_EXTRACT: FileKind[] = ['docx', 'pptx', 'zip', 'rar'];

type Opened = {
  fileId: number;
  name: string;
  kind: FileKind;
  sample: boolean;
  url: string; // Đường dẫn tạm tới file (để xem PDF hoặc tải bản xem trước)
  text: string | null; // Nội dung chữ (với các loại không phải PDF)
};

export default function DocumentPreview({ detail }: { detail: DocumentDetail }) {
  const [opened, setOpened] = useState<Opened | null>(null);
  const [loadingId, setLoadingId] = useState<number | null>(null);
  const [error, setError] = useState('');

  const published = detail.status === 'PUBLISHED';
  const firstFile = detail.files[0];

  // Giải phóng đường dẫn tạm khi đổi file hoặc rời trang
  useEffect(() => {
    return () => {
      if (opened?.url) {
        URL.revokeObjectURL(opened.url);
      }
    };
  }, [opened]);

  // Vào trang là mở sẵn bản thử của tệp đầu tiên
  useEffect(() => {
    if (published && firstFile) {
      void open(firstFile.id, firstFile.name, true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [detail.document.id]);

  /** Mở một file. sample = true: chỉ mở bản xem thử. */
  async function open(fileId: number, name: string, sample: boolean) {
    setLoadingId(fileId);
    setError('');
    try {
      const kind = fileKind(name);
      const blob = await previewFile(detail.document.id, fileId, sample);

      let text: string | null = null;
      if (blob.type.startsWith('text/')) {
        text = await blob.text();
      }
      // Bản đầy đủ của Word / PowerPoint / tệp nén: nhờ backend trích chữ
      if (!sample && NEEDS_TEXT_EXTRACT.includes(kind)) {
        text = await get<string>(`/documents/${detail.document.id}/files/${fileId}/preview-text`);
      }

      setOpened({ fileId, name, kind, sample, url: URL.createObjectURL(blob), text });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoadingId(null);
    }
  }

  const isPaidLocked =
    published && !detail.canDownload && !detail.canPreview && detail.document.accessType === 'PAID';

  // ----- Khung xem của file đang mở -----
  let viewer = null;
  if (opened) {
    if (opened.kind === 'pdf') {
      viewer = <PdfViewer url={opened.url} title={(opened.sample ? 'Đọc thử ' : 'Xem trước ') + opened.name} />;
    } else if (opened.text !== null) {
      viewer = <TextPreview text={opened.text} kind={opened.kind} />;
    } else {
      viewer = <p className="notice">Không có bản trích cho định dạng này. Tải tài liệu để xem.</p>;
    }
  }

  return (
    <section className="document-preview pv">
      <div className="pv-head">
        <div>
          <h3>{published ? 'Đọc thử tài liệu' : 'Xem trước tệp'}</h3>
          <p className="muted small">
            {published
              ? 'Xem nhanh phần đầu tài liệu trước khi tải. Tệp gốc vẫn cần quyền tải riêng.'
              : 'Bản xem trước đầy đủ dành cho tác giả và quản trị viên.'}
          </p>
        </div>
      </div>

      {/* Danh sách tệp: mỗi tệp một dòng, có loại file và nút xem */}
      <ul className="pv-files">
        {detail.files.map((f) => {
          const kind = fileKind(f.name);
          const info = KIND_INFO[kind];
          const isOpen = opened?.fileId === f.id;
          const isLoading = loadingId === f.id;
          return (
            <li key={f.id} className={isOpen ? 'pv-file is-open' : 'pv-file'}>
              <span className={`pv-badge k-${kind}`}>
                <Icon name={info.icon} size={16} />
                {f.name.split('.').pop()?.toUpperCase()}
              </span>
              <span className="pv-file-name">
                <strong>{f.name}</strong>
                <small>
                  {info.label} · {fileSize(f.sizeBytes)} · Xem thử: {info.sample}
                </small>
              </span>
              <span className="pv-file-actions">
                {published && (
                  <button
                    type="button"
                    className={isOpen && opened?.sample ? 'button primary compact' : 'button secondary compact'}
                    disabled={loadingId !== null}
                    onClick={() => open(f.id, f.name, true)}
                  >
                    <Icon name="eye" size={14} />
                    {isLoading ? 'Đang mở…' : 'Xem thử'}
                  </button>
                )}
                {detail.canPreview && (
                  <button
                    type="button"
                    className={isOpen && !opened?.sample ? 'button primary compact' : 'button secondary compact'}
                    disabled={loadingId !== null}
                    onClick={() => open(f.id, f.name, false)}
                  >
                    {isLoading ? 'Đang mở…' : 'Bản đầy đủ'}
                  </button>
                )}
              </span>
            </li>
          );
        })}
      </ul>

      {isPaidLocked && (
        <p className="paid-note">Bản xem thử chỉ gồm phần trích. Cần đăng nhập và có quyền để tải đầy đủ.</p>
      )}

      {error && (
        <p role="alert" className="alert">
          {error}
        </p>
      )}

      {/* Khung xem */}
      {opened && (
        <div className="pv-stage">
          <div className="pv-stage-bar">
            <span>
              <strong>{opened.name}</strong>
              <em>{opened.sample ? 'Bản xem thử' : 'Bản xem trước đầy đủ'}</em>
            </span>
            {!opened.sample && (
              <a className="button secondary compact" href={opened.url} download={opened.name}>
                <Icon name="download" size={14} />
                Tải bản xem trước
              </a>
            )}
          </div>
          {loadingId === opened.fileId ? (
            <div className="library-loading" role="status">
              <span className="spinner" />
              Đang mở tệp…
            </div>
          ) : (
            viewer
          )}
        </div>
      )}

      {!opened && loadingId !== null && (
        <div className="library-loading" role="status">
          <span className="spinner" />
          Đang mở bản xem thử…
        </div>
      )}
    </section>
  );
}
