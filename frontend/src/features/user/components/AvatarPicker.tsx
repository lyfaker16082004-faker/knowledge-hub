import { useEffect, useRef, useState } from 'react';
import type { DragEvent } from 'react';
import { uploadMultipart, errorMessage } from '../../../shared/api/http-client';
import type { Account } from '../../auth/types';
import { statusNames } from '../../auth/types';
import Icon from '../../../shared/components/ui/Icon';
import { fileSize } from '../../documents/types';

/*
 * Khu vực đổi ảnh đại diện:
 * - Bấm vào ảnh (hoặc nút "Đổi ảnh") để chọn file, hoặc kéo thả ảnh vào
 * - Kiểm tra định dạng và dung lượng ngay khi chọn
 * - Xem trước ảnh mới, bấm "Lưu ảnh" mới gửi lên server, "Hủy" để bỏ
 */

const MAX_SIZE = 2 * 1024 * 1024; // 2 MB
const ALLOWED_TYPES = ['image/png', 'image/jpeg'];

type Props = {
  user: Account;
  onSaved: (user: Account) => void;
};

export default function AvatarPicker({ user, onSaved }: Props) {
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null); // Ảnh mới đang chọn (chưa lưu)
  const [preview, setPreview] = useState(''); // Đường dẫn tạm để xem trước
  const [busy, setBusy] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  // Giải phóng đường dẫn tạm khi đổi ảnh khác hoặc rời trang
  useEffect(() => {
    return () => {
      if (preview) {
        URL.revokeObjectURL(preview);
      }
    };
  }, [preview]);

  // Thông báo thành công tự ẩn sau 3 giây
  useEffect(() => {
    if (!message) {
      return;
    }
    const timer = setTimeout(() => setMessage(''), 3000);
    return () => clearTimeout(timer);
  }, [message]);

  function openPicker() {
    if (!busy) {
      input.current?.click();
    }
  }

  /** Kiểm tra ảnh vừa chọn rồi hiện bản xem trước. */
  function choose(next: File | undefined) {
    setError('');
    setMessage('');
    if (!next) {
      return;
    }
    if (!ALLOWED_TYPES.includes(next.type)) {
      setError('Chỉ nhận ảnh PNG hoặc JPEG.');
      return;
    }
    if (next.size >= MAX_SIZE) {
      setError(`Ảnh của bạn nặng ${fileSize(next.size)}, cần nhỏ hơn 2 MB.`);
      return;
    }
    setFile(next);
    setPreview(URL.createObjectURL(next));
  }

  function cancel() {
    setFile(null);
    setPreview('');
    setError('');
  }

  /** Gửi ảnh lên server. */
  async function save() {
    if (!file) {
      return;
    }
    setBusy(true);
    setError('');
    try {
      const form = new FormData();
      form.append('file', file);
      const updated = await uploadMultipart<Account>('/users/me/avatar', form);
      onSaved(updated);
      setFile(null);
      setPreview('');
      setMessage('Đã cập nhật ảnh đại diện.');
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  // ----- Kéo thả ảnh -----
  function onDragOver(event: DragEvent) {
    event.preventDefault();
    setDragging(true);
  }
  function onDrop(event: DragEvent) {
    event.preventDefault();
    setDragging(false);
    if (!busy) {
      choose(event.dataTransfer.files[0]);
    }
  }

  // Ảnh đang hiển thị: ảnh xem trước (nếu có) hoặc ảnh hiện tại
  const shownImage = preview || user.avatarUrl;
  const name = user.displayName || user.username;

  let avatarClass = 'avatar-picker-photo';
  if (dragging) avatarClass += ' is-dragging';
  if (preview) avatarClass += ' is-preview';

  return (
    <div className="avatar-picker">
      <div className="avatar-picker-row">
        {/* Ảnh đại diện: bấm hoặc kéo thả ảnh vào để đổi */}
        <button
          type="button"
          className={avatarClass}
          onClick={openPicker}
          onDragOver={onDragOver}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
          disabled={busy}
          aria-label="Chọn ảnh đại diện mới"
          title="Bấm hoặc kéo thả ảnh vào đây"
        >
          {shownImage ? <img src={shownImage} alt="" /> : <span>{user.username.slice(0, 2).toUpperCase()}</span>}
          <span className="avatar-picker-overlay" aria-hidden="true">
            {busy ? <span className="spinner" /> : <Icon name="camera" size={22} />}
          </span>
        </button>

        <div className="avatar-picker-info">
          <h3>{name}</h3>
          <span className={`status ${user.status.toLowerCase()}`}>{statusNames[user.status]}</span>

          {/* Chưa chọn ảnh mới: nút Đổi ảnh. Đã chọn: nút Lưu / Hủy */}
          {file ? (
            <div className="avatar-picker-actions">
              <button type="button" className="button primary compact" onClick={save} disabled={busy}>
                <Icon name="check" size={15} />
                {busy ? 'Đang lưu…' : 'Lưu ảnh'}
              </button>
              <button type="button" className="button secondary compact" onClick={cancel} disabled={busy}>
                Hủy
              </button>
            </div>
          ) : (
            <div className="avatar-picker-actions">
              <button type="button" className="button secondary compact" onClick={openPicker}>
                <Icon name="camera" size={15} />
                {user.avatarUrl ? 'Đổi ảnh' : 'Tải ảnh lên'}
              </button>
            </div>
          )}

          <small className="avatar-picker-hint">
            {file ? `${file.name} · ${fileSize(file.size)}` : 'PNG hoặc JPEG, nhỏ hơn 2 MB. Có thể kéo thả ảnh vào.'}
          </small>
        </div>
      </div>

      {/* Ô chọn file thật được ẩn đi, chỉ mở qua các nút ở trên */}
      <input
        ref={input}
        type="file"
        accept="image/png,image/jpeg"
        hidden
        aria-label="Ảnh đại diện"
        onChange={(e) => {
          choose(e.target.files?.[0]);
          e.target.value = ''; // Cho phép chọn lại cùng một file
        }}
      />

      {error && (
        <p className="avatar-picker-error" role="alert">
          {error}
        </p>
      )}
      {message && (
        <p className="avatar-picker-success" role="status">
          <Icon name="check" size={14} />
          {message}
        </p>
      )}
    </div>
  );
}
