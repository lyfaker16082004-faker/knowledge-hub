import { useRef, useState, type ClipboardEvent, type DragEvent, type KeyboardEvent } from 'react';
import Icon from '../../../shared/components/ui/Icon';
import { errorMessage, uploadMultipart } from '../../../shared/api/http-client';
import MarkdownContent from '../../../shared/components/markdown/MarkdownContent';

/**
 * Trình soạn thảo nội dung bài viết (định dạng Markdown).
 *
 * - Thanh công cụ: đậm, nghiêng, code, khối code, link, danh sách, trích dẫn, chèn ảnh
 * - Ảnh: dán trực tiếp (Ctrl+V ảnh chụp màn hình), kéo thả vào ô soạn, hoặc bấm nút chọn file
 * - Tab "Xem trước" hiển thị đúng như khi bài được đăng
 */

const MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB, khớp với giới hạn ở backend
const IMAGE_TYPES = ['image/png', 'image/jpeg'];

type Props = {
  id: string;
  value: string;
  onChange: (value: string) => void;
  maxLength: number;
  placeholder?: string;
};

export default function MarkdownEditor({ id, value, onChange, maxLength, placeholder }: Props) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [tab, setTab] = useState<'write' | 'preview'>('write');
  const [uploading, setUploading] = useState(0);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState('');

  // Ảnh tải lên mất vài giây; lúc tải xong người dùng có thể đã gõ thêm chữ.
  // Vì vậy luôn đọc giá trị MỚI NHẤT qua ref thay vì biến "value" cũ.
  const valueRef = useRef(value);
  valueRef.current = value;

  // ===================== Chèn / bọc văn bản =====================

  /** Thay đoạn [start, end) bằng "text" rồi đặt con trỏ vào vị trí mong muốn. */
  function replaceRange(start: number, end: number, text: string, selectFrom: number, selectTo: number) {
    const current = valueRef.current;
    onChange(current.slice(0, start) + text + current.slice(end));
    requestAnimationFrame(() => {
      const area = textareaRef.current;
      if (!area) return;
      area.focus();
      area.setSelectionRange(start + selectFrom, start + selectTo);
    });
  }

  /** Bọc chữ đang chọn. VD: wrap('**', '**', 'chữ đậm') -> **chữ đậm** */
  function wrap(before: string, after: string, placeholderText: string) {
    const area = textareaRef.current;
    if (!area) return;
    const { selectionStart: start, selectionEnd: end } = area;
    const selected = valueRef.current.slice(start, end) || placeholderText;
    replaceRange(start, end, before + selected + after, before.length, before.length + selected.length);
  }

  /** Thêm tiền tố vào đầu mỗi dòng đang chọn. VD: "- " cho danh sách, "> " cho trích dẫn */
  function prefixLines(prefix: string | ((index: number) => string)) {
    const area = textareaRef.current;
    if (!area) return;
    const current = valueRef.current;
    const lineStart = current.lastIndexOf('\n', area.selectionStart - 1) + 1;
    const end = area.selectionEnd;
    const lines = current.slice(lineStart, end).split('\n');
    const result = lines.map((line, index) => (typeof prefix === 'string' ? prefix : prefix(index)) + line).join('\n');
    replaceRange(lineStart, end, result, result.length, result.length);
  }

  function insertCodeBlock() {
    const area = textareaRef.current;
    if (!area) return;
    const { selectionStart: start, selectionEnd: end } = area;
    const selected = valueRef.current.slice(start, end) || 'dán code vào đây';
    const before = (start > 0 && valueRef.current[start - 1] !== '\n' ? '\n' : '') + '```\n';
    replaceRange(start, end, before + selected + '\n```\n', before.length, before.length + selected.length);
  }

  function insertLink() {
    const url = window.prompt('Nhập địa chỉ liên kết (https://...)');
    if (!url) return;
    wrap('[', `](${url.trim()})`, 'mô tả liên kết');
  }

  /** Phím tắt: Ctrl+B in đậm, Ctrl+I in nghiêng, Tab thụt lề 2 dấu cách. */
  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    const ctrl = event.ctrlKey || event.metaKey;
    if (ctrl && event.key.toLowerCase() === 'b') { event.preventDefault(); wrap('**', '**', 'chữ đậm'); }
    else if (ctrl && event.key.toLowerCase() === 'i') { event.preventDefault(); wrap('*', '*', 'chữ nghiêng'); }
    else if (event.key === 'Tab' && !event.shiftKey) {
      event.preventDefault();
      const area = event.currentTarget;
      replaceRange(area.selectionStart, area.selectionEnd, '  ', 2, 2);
    }
  }

  // ===================== Tải ảnh lên =====================

  async function uploadImages(files: File[]) {
    setError('');
    for (const file of files) {
      if (!IMAGE_TYPES.includes(file.type)) { setError(`"${file.name}" không phải ảnh PNG hoặc JPEG.`); continue; }
      if (file.size > MAX_IMAGE_SIZE) { setError(`"${file.name}" lớn hơn 5 MB.`); continue; }

      // Bước 1: Chèn chữ tạm tại vị trí con trỏ để người dùng biết ảnh đang tải
      const marker = `![Đang tải ảnh ${file.name}…]()`;
      const area = textareaRef.current;
      const position = area ? area.selectionStart : valueRef.current.length;
      const needsNewLine = position > 0 && valueRef.current[position - 1] !== '\n';
      const insertion = (needsNewLine ? '\n' : '') + marker + '\n';
      replaceRange(position, area ? area.selectionEnd : position, insertion, insertion.length, insertion.length);

      // Bước 2: Gửi ảnh lên server
      setUploading(n => n + 1);
      try {
        const form = new FormData();
        form.append('file', file);
        const result = await uploadMultipart<{ url: string }>('/posts/images', form);

        // Bước 3: Thay chữ tạm bằng cú pháp ảnh thật
        const altText = file.name.replace(/\.[^.]+$/, '').replace(/[[\]]/g, '') || 'ảnh';
        onChange(valueRef.current.replace(marker, `![${altText}](${result.url})`));
      } catch (e) {
        onChange(valueRef.current.replace(marker + '\n', '').replace(marker, ''));
        setError(errorMessage(e));
      } finally {
        setUploading(n => n - 1);
      }
    }
  }

  function imagesFrom(list: FileList | null | undefined) {
    return Array.from(list || []).filter(file => file.type.startsWith('image/'));
  }

  function handlePaste(event: ClipboardEvent<HTMLTextAreaElement>) {
    const images = imagesFrom(event.clipboardData.files);
    if (images.length) {
      event.preventDefault(); // Không dán tên file dạng chữ
      uploadImages(images);
    }
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    setDragging(false);
    const images = imagesFrom(event.dataTransfer.files);
    if (images.length) {
      event.preventDefault();
      uploadImages(images);
    }
  }

  // ===================== Giao diện =====================

  const tools: { icon: string; label: string; title: string; action: () => void }[] = [
    { icon: 'B', label: 'In đậm', title: 'In đậm (Ctrl+B)', action: () => wrap('**', '**', 'chữ đậm') },
    { icon: 'I', label: 'In nghiêng', title: 'In nghiêng (Ctrl+I)', action: () => wrap('*', '*', 'chữ nghiêng') },
    { icon: '</>', label: 'Code trong dòng', title: 'Code trong dòng', action: () => wrap('`', '`', 'code') },
    { icon: '{ }', label: 'Khối code', title: 'Khối code nhiều dòng', action: insertCodeBlock },
    { icon: '🔗', label: 'Liên kết', title: 'Chèn liên kết', action: insertLink },
    { icon: '•', label: 'Danh sách', title: 'Danh sách chấm', action: () => prefixLines('- ') },
    { icon: '1.', label: 'Danh sách số', title: 'Danh sách đánh số', action: () => prefixLines(index => `${index + 1}. `) },
    { icon: '❝', label: 'Trích dẫn', title: 'Trích dẫn', action: () => prefixLines('> ') },
  ];

  return (
    <div className={`md-editor${dragging ? ' is-dragging' : ''}`}>
      <div className="md-editor-bar">
        <div className="md-tabs" role="tablist">
          <button type="button" role="tab" aria-selected={tab === 'write'} onClick={() => setTab('write')}>Viết</button>
          <button type="button" role="tab" aria-selected={tab === 'preview'} onClick={() => setTab('preview')}>Xem trước</button>
        </div>

        {tab === 'write' && (
          <div className="md-tools" role="toolbar" aria-label="Định dạng nội dung">
            {tools.map(tool => (
              <button key={tool.label} type="button" title={tool.title} aria-label={tool.label}
                onMouseDown={event => event.preventDefault()} onClick={tool.action}>
                <span aria-hidden="true">{tool.icon}</span>
              </button>
            ))}
            <button type="button" className="md-image-button" title="Chèn ảnh từ máy" onClick={() => fileInputRef.current?.click()}>
              <Icon name="upload" size={15} /><span>Ảnh</span>
            </button>
            <input ref={fileInputRef} type="file" accept="image/png,image/jpeg" multiple hidden
              onChange={event => { uploadImages(imagesFrom(event.target.files)); event.target.value = ''; }} />
          </div>
        )}
      </div>

      {tab === 'write' ? (
        <div className="md-write"
          onDragOver={event => { if (event.dataTransfer.types.includes('Files')) { event.preventDefault(); setDragging(true); } }}
          onDragLeave={() => setDragging(false)}
          onDrop={handleDrop}>
          <textarea ref={textareaRef} id={id} value={value} maxLength={maxLength} placeholder={placeholder}
            onChange={event => onChange(event.target.value)} onPaste={handlePaste} onKeyDown={handleKeyDown} />
          {dragging && <div className="md-drop-hint"><Icon name="upload" size={22} />Thả ảnh vào đây để tải lên</div>}
        </div>
      ) : (
        <div className="md-preview">
          {value.trim() ? <MarkdownContent text={value} /> : <p className="md-empty">Chưa có nội dung để xem trước.</p>}
        </div>
      )}

      <div className="md-footer">
        <span>
          {uploading > 0
            ? <><span className="spinner small" />Đang tải {uploading} ảnh…</>
            : 'Dán ảnh (Ctrl+V) hoặc kéo thả ảnh vào ô soạn · Đặt code giữa hai dòng ```'}
        </span>
        <span className={value.length > maxLength * 0.9 ? 'is-near-limit' : ''}>
          {value.length.toLocaleString('vi-VN')} / {maxLength.toLocaleString('vi-VN')}
        </span>
      </div>
      {error && <p className="cf-form-error" role="alert">{error}</p>}
    </div>
  );
}
