import { useRef, useState } from 'react';
import type { ChangeEvent, MouseEvent, ClipboardEvent, ReactNode } from 'react';
import { sanitizeDescriptionHtml } from './DocumentDescription';

/*
 * Trình soạn thảo "Mô tả chi tiết" kiểu Summernote (soạn trực tiếp, thấy định dạng ngay).
 * - Dùng thẻ <div contentEditable> làm vùng soạn thảo.
 * - Các nút định dạng gọi document.execCommand (in đậm, nghiêng, danh sách...).
 * - Nội dung HTML luôn được lọc qua sanitizeDescriptionHtml để chặn mã độc.
 * - Giá trị cuối cùng được chép vào một <textarea name="description"> ẩn
 *   để form cha đọc được bằng FormData.
 */

const HTML_TAG = /<\/?(?:p|div|h[1-6]|ul|ol|li|blockquote|strong|b|em|i|u|s|a|img|br|span|pre|code|font)\b/i;

/** Liên kết hợp lệ: http, https hoặc mailto. */
function safeLink(value: string) {
  try {
    const url = new URL(value);
    if (url.protocol === 'http:' || url.protocol === 'https:' || url.protocol === 'mailto:') {
      return url.href;
    }
    return null;
  } catch {
    return null;
  }
}

/** Ảnh hợp lệ: chỉ https. */
function safeImage(value: string) {
  try {
    const url = new URL(value);
    return url.protocol === 'https:' ? url.href : null;
  } catch {
    return null;
  }
}

/** Đổi mô tả cũ thành HTML để đưa vào vùng soạn thảo. */
function toEditorHtml(source: string) {
  if (!source) {
    return '';
  }
  // Đã là HTML -> chỉ cần lọc
  if (HTML_TAG.test(source)) {
    return sanitizeDescriptionHtml(source);
  }
  // Văn bản thường -> đổi ký tự đặc biệt và xuống dòng thành <br>
  const escaped = source
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\r?\n/g, '<br>');
  return '<p>' + escaped + '</p>';
}

type Props = {
  initialValue?: string | null;
  maxLength?: number;
};

export default function SummernoteEditor({ initialValue = '', maxLength = 10000 }: Props) {
  const editor = useRef<HTMLDivElement>(null);
  const hiddenField = useRef<HTMLTextAreaElement>(null);
  const savedRange = useRef<Range | null>(null); // Vị trí con trỏ / vùng chữ đang chọn

  // HTML ban đầu chỉ tính một lần, để React không ghi đè khi người dùng đang gõ
  const startHtml = useRef({ __html: toEditorHtml(initialValue || '') });

  const [value, setValue] = useState(initialValue || '');
  const [preview, setPreview] = useState(false);
  const [insertKind, setInsertKind] = useState<'link' | 'image' | null>(null); // Đang mở ô chèn link/ảnh
  const [url, setUrl] = useState('');
  const [label, setLabel] = useState('');
  const [error, setError] = useState('');

  // =========================================================
  // PHẦN 1: ĐỒNG BỘ NỘI DUNG
  // =========================================================

  /** Lấy HTML trong vùng soạn thảo, lọc lại và lưu vào state. */
  function sync() {
    const node = editor.current;
    if (!node) {
      return;
    }
    const raw = node.innerHTML;
    const clean = sanitizeDescriptionHtml(raw);
    if (clean !== raw) {
      node.innerHTML = clean;
    }
    setValue(clean);

    // Báo lỗi cho form nếu mô tả quá dài
    const tooLong = clean.length > maxLength;
    hiddenField.current?.setCustomValidity(tooLong ? 'Mô tả vượt quá ' + maxLength + ' ký tự được phép.' : '');
  }

  // =========================================================
  // PHẦN 2: GHI NHỚ VÙNG CHỌN
  // (Bấm nút trên thanh công cụ sẽ làm mất vùng chọn, nên phải lưu lại trước)
  // =========================================================

  function saveSelection() {
    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      savedRange.current = selection.getRangeAt(0).cloneRange();
    }
  }

  function rememberSelection(event: MouseEvent<HTMLElement>) {
    event.preventDefault();
    saveSelection();
    editor.current?.focus();
  }

  function restoreSelection() {
    const selection = window.getSelection();
    if (savedRange.current && selection) {
      selection.removeAllRanges();
      selection.addRange(savedRange.current);
    }
    editor.current?.focus();
  }

  /** Chạy một lệnh định dạng, ví dụ command('bold'). */
  function command(name: string, arg?: string) {
    restoreSelection();
    document.execCommand(name, false, arg);
    sync();
  }

  // =========================================================
  // PHẦN 3: CHÈN LIÊN KẾT / ẢNH
  // =========================================================

  function openInsert(kind: 'link' | 'image') {
    saveSelection();
    setInsertKind(kind);
    setUrl('');
    setLabel('');
    setError('');
  }

  function insert() {
    const isImage = insertKind === 'image';
    const validUrl = isImage ? safeImage(url) : safeLink(url);

    if (!validUrl) {
      setError(isImage ? 'Dùng URL ảnh công khai bắt đầu bằng https://.' : 'Nhập liên kết http://, https:// hoặc mailto:.');
      return;
    }

    restoreSelection();
    const selection = window.getSelection();
    const range = selection && selection.rangeCount > 0 ? selection.getRangeAt(0) : null;

    if (isImage) {
      // Chèn thẻ <img> vào vị trí con trỏ
      const img = document.createElement('img');
      img.src = validUrl;
      img.alt = label.trim() || 'Ảnh minh họa';
      if (range) {
        range.deleteContents();
        range.insertNode(img);
        range.setStartAfter(img);
        range.collapse(true);
      }
    } else if (range && !range.collapsed) {
      // Đang bôi đen chữ -> biến chữ đó thành liên kết
      document.execCommand('createLink', false, validUrl);
    } else {
      // Không bôi đen -> chèn liên kết mới
      const anchor = document.createElement('a');
      anchor.href = validUrl;
      anchor.textContent = label.trim() || validUrl;
      if (range) {
        range.insertNode(anchor);
        range.setStartAfter(anchor);
        range.collapse(true);
      }
    }

    if (selection && range) {
      selection.removeAllRanges();
      selection.addRange(range);
    }

    sync();
    setInsertKind(null);
    setUrl('');
    setLabel('');
  }

  // =========================================================
  // PHẦN 4: CÁC SỰ KIỆN KHÁC
  // =========================================================

  /** Khi dán, chỉ lấy chữ thuần (bỏ định dạng lạ từ trang web khác). */
  function pastePlainText(event: ClipboardEvent<HTMLDivElement>) {
    event.preventDefault();
    const text = event.clipboardData.getData('text/plain');
    document.execCommand('insertText', false, text);
    sync();
  }

  function changeFontSize(event: ChangeEvent<HTMLSelectElement>) {
    command('fontSize', event.target.value);
  }

  // Nút định dạng đơn giản: [lệnh, chú thích, nội dung nút]
  const styleButtons: [string, string, ReactNode][] = [
    ['bold', 'In đậm', <b>B</b>],
    ['italic', 'In nghiêng', <i>I</i>],
    ['underline', 'Gạch chân', <u>U</u>],
    ['strikeThrough', 'Gạch ngang', <s>S</s>],
  ];
  const listButtons: [string, string, string][] = [
    ['insertUnorderedList', 'Danh sách chấm đầu dòng', '• List'],
    ['insertOrderedList', 'Danh sách đánh số', '1. List'],
    ['justifyLeft', 'Căn trái', '≡'],
    ['justifyCenter', 'Căn giữa', '☰'],
  ];

  return (
    <div className="summernote-editor">
      {/* ===== Thanh công cụ ===== */}
      <div className="summernote-toolbar" role="toolbar" aria-label="Thanh công cụ soạn mô tả">
        <label className="summernote-select">
          <span>Kiểu chữ</span>
          <select
            aria-label="Kiểu đoạn văn"
            defaultValue="p"
            onMouseDown={saveSelection}
            onChange={(e) => command('formatBlock', e.target.value)}
          >
            <option value="p">Đoạn văn</option>
            <option value="h2">Tiêu đề lớn</option>
            <option value="h3">Tiêu đề</option>
            <option value="h4">Tiêu đề nhỏ</option>
            <option value="blockquote">Trích dẫn</option>
          </select>
        </label>

        <span className="summernote-divider" />

        {styleButtons.map(([name, title, content]) => (
          <button
            key={name}
            type="button"
            title={title}
            aria-label={title}
            onMouseDown={rememberSelection}
            onClick={() => command(name)}
          >
            {content}
          </button>
        ))}

        <label className="summernote-select summernote-size">
          <span>Cỡ chữ</span>
          <select aria-label="Cỡ chữ" defaultValue="" onMouseDown={saveSelection} onChange={changeFontSize}>
            <option value="" disabled>
              Cỡ
            </option>
            <option value="2">Nhỏ</option>
            <option value="3">Vừa</option>
            <option value="4">Lớn</option>
            <option value="5">Rất lớn</option>
          </select>
        </label>

        <label className="summernote-color" title="Màu chữ">
          A
          <input
            type="color"
            aria-label="Màu chữ"
            defaultValue="#243247"
            onMouseDown={saveSelection}
            onChange={(e) => command('foreColor', e.target.value)}
          />
        </label>

        <span className="summernote-divider" />

        {listButtons.map(([name, title, text]) => (
          <button
            key={name}
            type="button"
            title={title}
            aria-label={title}
            onMouseDown={rememberSelection}
            onClick={() => command(name)}
          >
            {text}
          </button>
        ))}

        <span className="summernote-divider" />

        <button
          type="button"
          title="Chèn liên kết"
          aria-label="Chèn liên kết"
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => openInsert('link')}
        >
          ↗ Link
        </button>
        <button
          type="button"
          title="Chèn ảnh từ URL"
          aria-label="Chèn ảnh từ URL"
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => openInsert('image')}
        >
          ▧ Ảnh
        </button>
        <button
          type="button"
          title="Xóa định dạng"
          aria-label="Xóa định dạng"
          onMouseDown={rememberSelection}
          onClick={() => command('removeFormat')}
        >
          Tx
        </button>

        <span className="summernote-spacer" />

        <button
          type="button"
          className="summernote-preview-button"
          aria-pressed={preview}
          onClick={() => setPreview(!preview)}
        >
          {preview ? 'Quay lại soạn' : 'Xem trước'}
        </button>
      </div>

      {/* ===== Ô nhập link / ảnh ===== */}
      {insertKind && (
        <div className="summernote-insert">
          <label>
            {insertKind === 'image' ? 'URL ảnh' : 'Địa chỉ liên kết'}
            <input
              autoFocus
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder={insertKind === 'image' ? 'https://.../anh.jpg' : 'https://...'}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  insert();
                }
              }}
            />
          </label>
          <label>
            {insertKind === 'image' ? 'Mô tả ảnh' : 'Nội dung liên kết'}
            <input
              value={label}
              onChange={(e) => setLabel(e.target.value)}
              placeholder={insertKind === 'image' ? 'Ví dụ: Sơ đồ bài học' : 'Chọn văn bản hoặc nhập tên liên kết'}
            />
          </label>
          <button type="button" className="button primary" onClick={insert}>
            Chèn
          </button>
          <button type="button" className="button secondary" onClick={() => setInsertKind(null)}>
            Hủy
          </button>
          {error && <span role="alert">{error}</span>}
        </div>
      )}

      {/* Ô ẩn chứa giá trị để gửi cùng form */}
      <textarea
        ref={hiddenField}
        className="summernote-value"
        name="description"
        aria-label="Mô tả chi tiết"
        maxLength={maxLength}
        value={value}
        onChange={() => {}}
      />

      {/* ===== Vùng soạn thảo ===== */}
      <div
        ref={editor}
        className="summernote-canvas"
        contentEditable={!preview}
        suppressContentEditableWarning
        dangerouslySetInnerHTML={startHtml.current}
        onInput={sync}
        onPaste={pastePlainText}
        hidden={preview}
        role="textbox"
        aria-multiline="true"
        aria-label="Mô tả chi tiết"
      />

      {preview && (
        <div className="summernote-preview" dangerouslySetInnerHTML={{ __html: sanitizeDescriptionHtml(value) }} />
      )}

      <div className="summernote-footer">
        <span>Soạn trực tiếp như trong trình soạn thảo. Ảnh được chèn bằng URL HTTPS.</span>
        <span>
          {value.length.toLocaleString('vi-VN')}/{maxLength.toLocaleString('vi-VN')}
        </span>
      </div>
    </div>
  );
}
