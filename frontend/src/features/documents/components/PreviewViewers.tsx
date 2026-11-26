import type { ReactNode } from 'react';
import Icon from '../../../shared/components/ui/Icon';
import MarkdownContent from '../../../shared/components/markdown/MarkdownContent';

/*
 * Các khung xem thử riêng cho từng loại file:
 *  - PDF  : trình xem PDF của trình duyệt
 *  - MD   : hiển thị như bài viết (tiêu đề, danh sách, code...)
 *  - TXT  : văn bản thuần, giữ xuống dòng
 *  - DOCX : trang giấy với các đoạn văn
 *  - PPTX : lưới các slide (mỗi slide một thẻ)
 *  - ZIP  : cây thư mục, có số tệp và dung lượng
 *  - RAR  : thông báo (chưa đọc được bên trong)
 */

export type FileKind = 'pdf' | 'docx' | 'pptx' | 'md' | 'txt' | 'zip' | 'rar' | 'other';

/** Đoán loại file theo đuôi tên file. */
export function fileKind(fileName: string): FileKind {
  const extension = fileName.split('.').pop()?.toLowerCase() || '';
  if (extension === 'ppt') return 'pptx';
  if (['pdf', 'docx', 'pptx', 'md', 'txt', 'zip', 'rar'].includes(extension)) {
    return extension as FileKind;
  }
  return 'other';
}

// Thông tin hiển thị cho từng loại: tên, icon, mô tả bản xem thử
export const KIND_INFO: Record<FileKind, { label: string; icon: string; sample: string }> = {
  pdf: { label: 'PDF', icon: 'book', sample: '2 trang đầu của tài liệu' },
  docx: { label: 'Word', icon: 'edit', sample: 'Phần chữ đầu tiên của văn bản' },
  pptx: { label: 'PowerPoint', icon: 'layout', sample: 'Nội dung chữ của các slide đầu' },
  md: { label: 'Markdown', icon: 'code', sample: 'Phần đầu của bài viết' },
  txt: { label: 'Văn bản', icon: 'book', sample: 'Phần đầu của văn bản' },
  zip: { label: 'Tệp nén ZIP', icon: 'grid', sample: 'Danh sách tệp bên trong' },
  rar: { label: 'Tệp nén RAR', icon: 'grid', sample: 'Thông tin tệp nén' },
  other: { label: 'Tệp', icon: 'book', sample: 'Phần đầu của tệp' },
};

// Dòng backend thêm vào cuối khi cắt bớt bản xem thử
const CUT_MARKERS = ['… Bản xem thử đã dừng', '… (bản xem trước đã rút gọn)'];

/** Tách dòng "đã cắt bớt" ra khỏi nội dung để hiện thành nhãn riêng. */
function splitCutMarker(text: string) {
  for (const marker of CUT_MARKERS) {
    const index = text.indexOf(marker);
    if (index >= 0) {
      return { body: text.slice(0, index).trimEnd(), cut: true };
    }
  }
  return { body: text, cut: false };
}

/** Nhãn báo bản xem thử đã dừng giữa chừng. */
function CutNotice({ show }: { show: boolean }) {
  if (!show) {
    return null;
  }
  return (
    <p className="pv-cut">
      <Icon name="download" size={14} />
      Bản xem thử dừng ở đây. Tải tài liệu để đọc đầy đủ.
    </p>
  );
}

// =========================================================
// PDF
// =========================================================

export function PdfViewer({ url, title }: { url: string; title: string }) {
  return (
    <div className="pv-pdf">
      <iframe title={title} src={`${url}#toolbar=1&navpanes=0&view=FitH`} />
    </div>
  );
}

// =========================================================
// MARKDOWN VÀ VĂN BẢN THƯỜNG
// =========================================================

export function MarkdownViewer({ text }: { text: string }) {
  const { body, cut } = splitCutMarker(text);
  return (
    <div className="pv-paper">
      <MarkdownContent text={body} className="md-content pv-markdown" />
      <CutNotice show={cut} />
    </div>
  );
}

export function TextViewer({ text }: { text: string }) {
  const { body, cut } = splitCutMarker(text);
  return (
    <div className="pv-paper">
      <pre className="pv-plain">{body}</pre>
      <CutNotice show={cut} />
    </div>
  );
}

// =========================================================
// WORD (DOCX)
// =========================================================

export function WordViewer({ text }: { text: string }) {
  const { body, cut } = splitCutMarker(text);
  // Bỏ dòng tiêu đề "── Nội dung tài liệu ──" do backend thêm vào
  const content = body.replace(/^\s*── .* ──\s*/, '');
  const paragraphs = content.split(/\n+/).filter((line) => line.trim() !== '');

  return (
    <div className="pv-word">
      <div className="pv-word-page">
        {paragraphs.length === 0 && <p className="muted">Văn bản không có phần chữ để xem thử.</p>}
        {paragraphs.map((line, i) => (
          // Dòng đầu tiên thường là tiêu đề văn bản
          <p key={i} className={i === 0 ? 'pv-word-title' : undefined}>
            {line}
          </p>
        ))}
        <CutNotice show={cut} />
      </div>
    </div>
  );
}

// =========================================================
// POWERPOINT (PPTX)
// =========================================================

type Slide = { number: string; lines: string[] };

/** Tách văn bản thành từng slide theo dòng "── Slide N ──". */
function parseSlides(text: string): Slide[] {
  const slides: Slide[] = [];
  let current: Slide | null = null;

  for (const rawLine of text.split('\n')) {
    const line = rawLine.trim();
    const header = line.match(/^── Slide (\d+) ──$/);
    if (header) {
      current = { number: header[1], lines: [] };
      slides.push(current);
    } else if (current && line !== '') {
      current.lines.push(line);
    }
  }
  return slides;
}

export function SlideViewer({ text }: { text: string }) {
  const { body, cut } = splitCutMarker(text);
  const slides = parseSlides(body);

  if (slides.length === 0) {
    return <TextViewer text={text} />;
  }

  return (
    <div className="pv-slides">
      <p className="pv-summary">
        <Icon name="layout" size={15} />
        Đang xem phần chữ của {slides.length} slide đầu. Hình ảnh và bố cục chỉ có trong bản gốc.
      </p>
      <ol className="pv-slide-grid">
        {slides.map((slide) => (
          <li key={slide.number} className="pv-slide">
            <span className="pv-slide-number">{slide.number}</span>
            {slide.lines.length === 0 ? (
              <p className="pv-slide-empty">Slide chỉ có hình ảnh</p>
            ) : (
              <>
                <h4>{slide.lines[0]}</h4>
                <ul>
                  {slide.lines.slice(1, 7).map((line, i) => (
                    <li key={i}>{line}</li>
                  ))}
                </ul>
                {slide.lines.length > 7 && <small>+ {slide.lines.length - 7} dòng nữa</small>}
              </>
            )}
          </li>
        ))}
      </ol>
      <CutNotice show={cut} />
    </div>
  );
}

// =========================================================
// TỆP NÉN (ZIP / RAR)
// =========================================================

type ArchiveItem = { path: string; folder: boolean; size: string };

/** Đọc danh sách tệp do backend trả về. */
function parseArchive(text: string) {
  const lines = text.split('\n');
  const summary = lines[0] || '';
  const items: ArchiveItem[] = [];
  let more = '';

  for (const line of lines.slice(1)) {
    if (line.startsWith('[thư mục] ')) {
      items.push({ path: line.slice('[thư mục] '.length), folder: true, size: '' });
    } else if (line.startsWith('  ')) {
      // "  thu-muc/ten-file.txt (12.3 KB)"
      const match = line.trim().match(/^(.*?)(?: \(([^()]+)\))?$/);
      if (match) {
        items.push({ path: match[1], folder: false, size: match[2] || '' });
      }
    } else if (line.startsWith('… và')) {
      more = line.trim();
    }
  }
  return { summary, items, more };
}

export function ArchiveViewer({ text, kind }: { text: string; kind: FileKind }) {
  const { body, cut } = splitCutMarker(text);

  // RAR: backend chỉ trả về một đoạn thông báo
  if (kind === 'rar') {
    return (
      <div className="pv-archive">
        <p className="pv-summary">
          <Icon name="grid" size={15} />
          {body}
        </p>
      </div>
    );
  }

  const { summary, items, more } = parseArchive(body);

  return (
    <div className="pv-archive">
      <p className="pv-summary">
        <Icon name="grid" size={15} />
        {summary}
      </p>
      <ul className="pv-tree">
        {items.map((item) => {
          // Độ sâu thư mục = số dấu "/" (thư mục kết thúc bằng "/" nên trừ 1)
          const parts = item.path.replace(/\/$/, '').split('/');
          const depth = parts.length - 1;
          const name = parts[parts.length - 1];
          return (
            <li
              key={item.path}
              className={item.folder ? 'pv-tree-folder' : 'pv-tree-file'}
              style={{ paddingLeft: 10 + depth * 18 }}
              title={item.path}
            >
              <Icon name={item.folder ? 'grid' : 'book'} size={14} />
              <span>{name}</span>
              {item.size && <small>{item.size}</small>}
            </li>
          );
        })}
      </ul>
      {(more || cut) && <p className="pv-cut">{more || 'Danh sách dài, chỉ hiện phần đầu.'} Tải tệp nén để xem đầy đủ.</p>}
    </div>
  );
}

/** Chọn khung xem phù hợp cho nội dung chữ. */
export function TextPreview({ text, kind }: { text: string; kind: FileKind }): ReactNode {
  if (kind === 'md') return <MarkdownViewer text={text} />;
  if (kind === 'docx') return <WordViewer text={text} />;
  if (kind === 'pptx') return <SlideViewer text={text} />;
  if (kind === 'zip' || kind === 'rar') return <ArchiveViewer text={text} kind={kind} />;
  return <TextViewer text={text} />;
}
