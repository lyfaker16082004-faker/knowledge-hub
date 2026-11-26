import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import type { Account } from '../../auth/types';
import type { DocumentItem, ManagedDocument } from '../types';
import { fileSize } from '../types';
import { useRemote } from '../../../shared/hooks/useRemote';
import { uploadDocument, mutate, errorMessage } from '../../../shared/api/http-client';
import InstitutionPicker from '../../organizations/InstitutionPicker';
import Icon from '../../../shared/components/ui/Icon';
import SummernoteEditor from '../components/SummernoteEditor';

/*
 * Trang đăng tài liệu mới / chỉnh sửa tài liệu.
 * Dùng chung cho 3 trường hợp:
 *  - Thành viên đóng góp tài liệu (#upload) -> cần quản trị viên duyệt
 *  - Quản trị viên thêm giáo trình (#admin/documents/new) -> công bố ngay
 *  - Sửa tài liệu đã có (truyền vào initial)
 */

const ALLOWED_FILES = /\.(pdf|ppt|pptx|docx|md|txt|zip|rar)$/i;
const MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB

type Option = { id: number; name: string };

type Props = {
  user: Account;
  onUploaded: (id: number) => void; // Gọi sau khi lưu thành công
  initial?: ManagedDocument; // Có giá trị -> đang sửa tài liệu cũ
  adminMode?: boolean;
};

export default function UploadPage({ user, onUploaded, initial, adminMode = false }: Props) {
  const existing = initial?.document;
  const isAdmin = user.roles.includes('ADMIN');

  const categories = useRemote<Option[]>('/categories');
  const subjects = useRemote<Option[]>('/subjects');

  // Giá trị ban đầu của trường/khoa: lấy từ tài liệu cũ, nếu không có thì lấy từ hồ sơ người dùng
  let startInstitution = '';
  let startFaculty = '';
  if (existing?.institutionId) {
    startInstitution = String(existing.institutionId);
  } else if (user.organization) {
    startInstitution = String(user.organization.institutionId);
  }
  if (existing?.facultyId) {
    startFaculty = String(existing.facultyId);
  } else if (user.organization?.facultyId) {
    startFaculty = String(user.organization.facultyId);
  }

  const [categoryId, setCategoryId] = useState(existing?.categoryId ? String(existing.categoryId) : '');
  const [subjectId, setSubjectId] = useState(existing?.subjectId ? String(existing.subjectId) : '');
  const [institution, setInstitution] = useState(startInstitution);
  const [faculty, setFaculty] = useState(startFaculty);
  const [access, setAccess] = useState<string>(existing?.accessType || 'FREE');
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [drag, setDrag] = useState(false); // Đang kéo file vào vùng thả
  const fileInput = useRef<HTMLInputElement>(null);

  /** Kiểm tra file người dùng chọn. */
  function chooseFile(next: File | undefined) {
    setError('');
    if (!next) {
      return;
    }
    if (!ALLOWED_FILES.test(next.name)) {
      setError('Chỉ hỗ trợ PDF, PPT/PPTX, DOCX, Markdown, TXT, ZIP và RAR.');
      return;
    }
    if (!next.size || next.size > MAX_FILE_SIZE) {
      setError('Tệp cần có nội dung và dung lượng không quá 20 MB.');
      return;
    }
    setFile(next);
  }

  /** Gửi form. */
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    // Đăng mới thì bắt buộc phải có file
    if (!file && !initial) {
      setError('Chọn một tệp tài liệu để chia sẻ.');
      return;
    }

    const fields = new FormData(event.currentTarget);

    // Nhãn: "Java, Ôn tập" -> ["Java", "Ôn tập"]
    const tags = String(fields.get('tags') || '')
      .split(',')
      .map((t) => t.trim())
      .filter((t) => t !== '');

    const metadata = {
      title: fields.get('title'),
      summary: fields.get('summary'),
      description: fields.get('description'),
      categoryId: Number(fields.get('categoryId')),
      level: fields.get('level'),
      accessType: access,
      price: access === 'FREE' ? 0 : Number(fields.get('price')),
      institutionId: institution ? Number(institution) : null,
      facultyId: faculty ? Number(faculty) : null,
      subjectId: fields.get('subjectId') ? Number(fields.get('subjectId')) : null,
      tags: tags,
    };

    setBusy(true);
    setError('');
    try {
      if (initial) {
        // Sửa tài liệu cũ
        const base = adminMode ? 'admin' : 'users/me';
        await mutate(`/${base}/documents/${initial.document.id}`, 'PUT', { version: initial.version, metadata });
        onUploaded(initial.document.id);
      } else {
        // Đăng tài liệu mới
        const doc = await uploadDocument<DocumentItem>(metadata, file!);
        onUploaded(doc.id);
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  // ----- Các dòng chữ thay đổi theo chế độ -----
  let title = 'Đóng góp tài liệu';
  if (initial) {
    title = 'Chỉnh sửa tài liệu';
  } else if (adminMode) {
    title = 'Thêm giáo trình';
  }

  let submitLabel = 'Chia sẻ tài liệu';
  if (busy) {
    submitLabel = 'Đang lưu…';
  } else if (initial) {
    submitLabel = 'Lưu thay đổi';
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <span className="section-kicker">{adminMode ? 'QUẢN LÝ NỘI DUNG' : 'CHIA SẺ ĐỂ CÙNG PHÁT TRIỂN'}</span>
          <h1>{title}</h1>
          <p className="muted">
            {adminMode
              ? 'Giáo trình do quản trị viên đăng được công bố ngay, không cần chờ duyệt.'
              : 'Một tài liệu hữu ích có thể giúp thêm nhiều người bắt đầu.'}
          </p>
        </div>
        <a className="subtle-link" href={adminMode ? '#admin/documents' : '#documents'}>
          {adminMode ? '← Về kho tài liệu' : '← Về thư viện'}
        </a>
      </div>

      <form onSubmit={submit} className="upload-form">
        <fieldset disabled={busy}>
          <div className="upload-grid">
            {/* ===== Cột trái: nội dung tài liệu ===== */}
            <section className="panel">
              <div className="panel-title">
                <Icon name="book" />
                <h2>Nội dung tài liệu</h2>
              </div>

              <label className="field">
                Tên tài liệu
                <input
                  aria-label="Tên tài liệu"
                  name="title"
                  defaultValue={existing?.title}
                  required
                  maxLength={255}
                  placeholder="Ví dụ: Đề ôn tập Lập trình Java cuối kỳ"
                />
              </label>

              <label className="field">
                Giới thiệu ngắn
                <textarea
                  aria-label="Giới thiệu ngắn"
                  name="summary"
                  defaultValue={existing?.summary}
                  required
                  maxLength={1500}
                  rows={3}
                  placeholder="Tài liệu này giúp người đọc học được điều gì?"
                />
              </label>

              <div className="field">
                <span className="summernote-editor-label">Mô tả chi tiết</span>
                <SummernoteEditor initialValue={initial?.description} />
              </div>

              <div className="upload-two-columns">
                <label className="field">
                  Chủ đề
                  <select
                    aria-label="Chủ đề"
                    name="categoryId"
                    required
                    value={categoryId}
                    onChange={(e) => setCategoryId(e.target.value)}
                    disabled={categories.loading || !!categories.error}
                  >
                    <option value="" disabled>
                      Chọn chủ đề
                    </option>
                    {categories.data?.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="field">
                  Mức độ
                  <select name="level" defaultValue={existing?.level || 'BEGINNER'}>
                    <option value="BEGINNER">Cơ bản</option>
                    <option value="INTERMEDIATE">Trung cấp</option>
                    <option value="ADVANCED">Nâng cao</option>
                  </select>
                </label>
              </div>

              {categories.error && (
                <div className="alert" role="alert">
                  {categories.error}
                  <button type="button" className="text-button" onClick={categories.refresh}>
                    <Icon name="refresh" size={16} />
                    Thử lại
                  </button>
                </div>
              )}

              <label className="field">
                Môn học
                <select
                  aria-label="Môn học"
                  name="subjectId"
                  value={subjectId}
                  onChange={(e) => setSubjectId(e.target.value)}
                >
                  <option value="">Không chọn môn học</option>
                  {subjects.data?.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                Nhãn (Tags)
                <input
                  aria-label="Nhãn (Tags)"
                  name="tags"
                  maxLength={404}
                  defaultValue={existing?.tags?.join(', ')}
                  placeholder="Ví dụ: Java, Ôn tập, Cuối kỳ"
                />
                <small>Tối đa 5 nhãn, cách nhau bằng dấu phẩy; mỗi nhãn tối đa 80 ký tự.</small>
              </label>

              {/* Sửa tài liệu thì giữ file cũ, đăng mới thì hiện vùng chọn file */}
              {initial ? (
                <p className="notice">Tệp gốc được giữ lại. Chỉnh sửa của tác giả sẽ được gửi duyệt lại.</p>
              ) : (
                <div
                  className={drag ? 'upload-dropzone drag-active' : 'upload-dropzone'}
                  onDragOver={(event) => {
                    event.preventDefault();
                    setDrag(true);
                  }}
                  onDragLeave={() => setDrag(false)}
                  onDrop={(event) => {
                    event.preventDefault();
                    setDrag(false);
                    if (!busy) {
                      chooseFile(event.dataTransfer.files[0]);
                    }
                  }}
                >
                  <span className="tile-icon purple">
                    <Icon name="upload" size={30} />
                  </span>
                  <h3>{file ? file.name : 'Chọn hoặc kéo thả tài liệu vào đây'}</h3>
                  <p>{file ? fileSize(file.size) : 'PDF, PPT/PPTX, DOCX, Markdown, TXT, ZIP, RAR · Tối đa 20 MB'}</p>
                  <input
                    ref={fileInput}
                    type="file"
                    accept=".pdf,.ppt,.pptx,.docx,.md,.txt,.zip,.rar"
                    aria-label="Tệp tài liệu"
                    hidden
                    onChange={(event) => chooseFile(event.target.files?.[0])}
                  />
                  <button type="button" className="button secondary" onClick={() => fileInput.current?.click()}>
                    {file ? 'Đổi tệp' : 'Chọn tệp'}
                  </button>
                </div>
              )}
            </section>

            {/* ===== Cột phải: trường/khoa và hình thức chia sẻ ===== */}
            <aside className="upload-aside">
              <section className="panel">
                <div className="panel-title">
                  <Icon name="institution" />
                  <h2>Trường và chuyên ngành</h2>
                </div>
                <p className="muted small">
                  Tài liệu của thành viên tự động dùng trường/khoa trong hồ sơ. Quản trị viên có thể chọn tổ chức cho
                  giáo trình chuẩn.
                </p>

                {/* Admin được chọn trường; thành viên chỉ xem trường trong hồ sơ */}
                {isAdmin ? (
                  <InstitutionPicker
                    emptyLabel="Không gắn trường — tài liệu toàn cầu"
                    institutionId={institution}
                    facultyId={faculty}
                    onChange={(i, f) => {
                      setInstitution(i);
                      setFaculty(f);
                    }}
                  />
                ) : (
                  <div className="document-profile-scope">
                    <strong>
                      {existing?.institutionName || user.organization?.institutionName || 'Tài liệu toàn cầu'}
                    </strong>
                    <p>{existing?.facultyName || user.organization?.facultyName || 'Không gắn khoa'}</p>
                    <a className="text-button" href="#account">
                      Cập nhật hồ sơ
                    </a>
                  </div>
                )}
              </section>

              <section className="panel">
                <div className="panel-title">
                  <Icon name="gift" />
                  <h2>Hình thức chia sẻ</h2>
                </div>
                <label className="field">
                  Loại tài liệu
                  <select value={access} onChange={(event) => setAccess(event.target.value)}>
                    <option value="FREE">Miễn phí</option>
                    <option value="PAID">Có phí</option>
                  </select>
                </label>

                {access === 'PAID' ? (
                  <>
                    <label className="field">
                      Giá (VNĐ)
                      <input
                        type="number"
                        name="price"
                        defaultValue={existing?.price}
                        required
                        min={1}
                        max={9999999999}
                        step={1}
                        placeholder="Nhập giá tài liệu"
                      />
                    </label>
                    <p className="paid-note">
                      Chưa có thanh toán trực tuyến. Người đọc cần được quản trị viên cấp quyền trước khi tải tài liệu
                      có phí.
                    </p>
                  </>
                ) : (
                  <p className="muted small">Thành viên đăng nhập có thể tải tài liệu miễn phí của bạn.</p>
                )}
              </section>

              <div className="upload-publish-note">
                <Icon name="shield" size={18} />
                <p>
                  Chia sẻ nội dung bạn có quyền phân phối. Tài liệu của thành viên cần được quản trị viên duyệt trước
                  khi xuất hiện trong thư viện.
                </p>
              </div>
            </aside>
          </div>

          {error && (
            <div className="alert" role="alert">
              {error}
            </div>
          )}

          <div className="upload-actions">
            <a
              href="#documents"
              className="button secondary"
              aria-disabled={busy}
              onClick={(event) => {
                if (busy) {
                  event.preventDefault();
                }
              }}
            >
              Hủy
            </a>
            <button type="submit" className="button primary" disabled={busy || !categories.data?.length}>
              <Icon name="upload" size={18} />
              {submitLabel}
            </button>
          </div>
        </fieldset>
      </form>
    </>
  );
}
