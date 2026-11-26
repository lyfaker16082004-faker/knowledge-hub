import { useEffect, useMemo, useRef, useState } from 'react';
import { useRemote } from '../../../shared/hooks/useRemote';
import type { CategoryInfo, DocumentItem, PageData } from '../types';
import { DocumentGrid, RemoteState, Pagination } from '../components/LibraryUI';
import Icon from '../../../shared/components/ui/Icon';
import InstitutionPicker from '../../organizations/InstitutionPicker';
import type { Account } from '../../auth/types';
import '../../../styles/library-page.css';

/*
 * Trang Thư viện tài liệu (#documents).
 * - Ô tìm kiếm
 * - Thanh chủ đề (cuộn ngang) + bảng "Tất cả chủ đề"
 * - Bộ lọc: trường/khoa, môn học, nhãn, miễn phí/có phí
 * - Sắp xếp, danh sách kết quả và phân trang
 */

const PAGE_SIZE = 12;

const SORT_OPTIONS: [string, string][] = [
  ['newest', 'Mới nhất'],
  ['featured', 'Nổi bật'],
  ['popular', 'Tải nhiều 7 ngày'],
  ['title', 'Tên A–Z'],
];

const ACCESS_OPTIONS: [string, string][] = [
  ['', 'Tất cả'],
  ['FREE', 'Miễn phí'],
  ['PAID', 'Có phí'],
];

type ActiveFilter = { key: string; label: string; clear: () => void };

// =========================================================
// PHẦN 1: THANH CHỦ ĐỀ
// =========================================================

type TopicBarProps = {
  categories: CategoryInfo[];
  total: number | undefined; // Tổng số tài liệu (hiện ở nút "Tất cả")
  value: string; // id chủ đề đang chọn ('' = tất cả)
  onChange: (id: string) => void;
};

/**
 * Hàng nút chủ đề cuộn ngang, kèm nút "Tất cả chủ đề" mở bảng có ô tìm.
 * Giúp trang vẫn gọn khi có rất nhiều chủ đề.
 */
function TopicBar({ categories, total, value, onChange }: TopicBarProps) {
  const rowRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false); // Bảng "Tất cả chủ đề" đang mở
  const [filter, setFilter] = useState(''); // Chữ tìm trong bảng
  const [atStart, setAtStart] = useState(true); // Hàng chủ đề đang ở đầu (ẩn mũi tên trái)
  const [atEnd, setAtEnd] = useState(true); // Hàng chủ đề đang ở cuối (ẩn mũi tên phải)

  // Hàng ngang: chủ đề nhiều tài liệu đứng trước
  const sortedByCount = useMemo(() => {
    const list = [...categories];
    list.sort((a, b) => {
      if (b.count !== a.count) {
        return b.count - a.count;
      }
      return a.name.localeCompare(b.name, 'vi');
    });
    return list;
  }, [categories]);

  // Bảng tất cả chủ đề: sắp theo tên A-Z và lọc theo chữ đang gõ
  const matches = useMemo(() => {
    const keyword = filter.trim().toLocaleLowerCase('vi');
    const list = [...categories];
    list.sort((a, b) => a.name.localeCompare(b.name, 'vi'));
    if (!keyword) {
      return list;
    }
    return list.filter((item) => item.name.toLocaleLowerCase('vi').includes(keyword));
  }, [categories, filter]);

  /** Kiểm tra hàng đã cuộn tới đầu / cuối chưa để ẩn hiện mũi tên. */
  function updateEdges() {
    const row = rowRef.current;
    if (!row) {
      return;
    }
    setAtStart(row.scrollLeft < 4);
    setAtEnd(row.scrollLeft + row.clientWidth >= row.scrollWidth - 4);
  }

  // Kiểm tra lại khi danh sách đổi hoặc khi đổi kích thước cửa sổ
  useEffect(() => {
    updateEdges();
    window.addEventListener('resize', updateEdges);
    return () => window.removeEventListener('resize', updateEdges);
  }, [sortedByCount]);

  // Cuộn tới chủ đề đang chọn
  useEffect(() => {
    const selected = rowRef.current?.querySelector<HTMLElement>('[aria-pressed=true]');
    selected?.scrollIntoView({ block: 'nearest', inline: 'center' });
  }, [value]);

  // Đóng bảng khi bấm ra ngoài hoặc nhấn Esc
  useEffect(() => {
    if (!open) {
      return;
    }
    function closeOnClickOutside(event: MouseEvent) {
      if (!panelRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', closeOnClickOutside);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('mousedown', closeOnClickOutside);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [open]);

  function pick(id: string) {
    onChange(id);
    setOpen(false);
    setFilter('');
  }

  /** Cuộn hàng chủ đề sang trái (-1) hoặc phải (1). */
  function scrollRow(direction: number) {
    const row = rowRef.current;
    if (row) {
      row.scrollBy({ left: direction * row.clientWidth * 0.7, behavior: 'smooth' });
    }
  }

  let trackClass = 'lib-topic-track';
  if (!atStart) trackClass += ' fade-start';
  if (!atEnd) trackClass += ' fade-end';

  return (
    <div className="lib-topics" ref={panelRef}>
      <div className={trackClass}>
        {!atStart && (
          <button type="button" className="lib-topic-arrow start" aria-label="Cuộn chủ đề sang trái" onClick={() => scrollRow(-1)}>
            <Icon name="chevronLeft" size={18} />
          </button>
        )}

        <div className="lib-topic-row" ref={rowRef} onScroll={updateEdges} role="group" aria-label="Chủ đề">
          <button type="button" className="lib-chip" aria-pressed={!value} onClick={() => pick('')}>
            Tất cả
            {total !== undefined && <small>{total}</small>}
          </button>
          {sortedByCount.map((item) => (
            <button
              type="button"
              key={item.id}
              className="lib-chip"
              aria-pressed={value === String(item.id)}
              onClick={() => pick(String(item.id))}
            >
              {item.name}
              <small>{item.count}</small>
            </button>
          ))}
        </div>

        {!atEnd && (
          <button type="button" className="lib-topic-arrow end" aria-label="Cuộn chủ đề sang phải" onClick={() => scrollRow(1)}>
            <Icon name="chevronRight" size={18} />
          </button>
        )}
      </div>

      <button
        type="button"
        className="lib-all-topics"
        aria-expanded={open}
        aria-controls="lib-topic-panel"
        onClick={() => setOpen(!open)}
      >
        <Icon name="grid" size={16} />
        Tất cả chủ đề
        <span className="lib-count">{categories.length}</span>
      </button>

      {open && (
        <div className="lib-topic-panel" id="lib-topic-panel" role="dialog" aria-label="Chọn chủ đề">
          <div className="lib-topic-panel-head">
            <label className="lib-input-icon">
              <Icon name="search" size={16} />
              <input
                autoFocus
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                placeholder={`Tìm trong ${categories.length} chủ đề…`}
                aria-label="Tìm chủ đề"
              />
            </label>
            <button type="button" className="icon-button" aria-label="Đóng" onClick={() => setOpen(false)}>
              <Icon name="close" size={18} />
            </button>
          </div>

          <div className="lib-topic-grid">
            {matches.map((item) => (
              <button
                type="button"
                key={item.id}
                aria-pressed={value === String(item.id)}
                onClick={() => pick(String(item.id))}
              >
                <span>{item.name}</span>
                <small>{item.count}</small>
              </button>
            ))}
            {matches.length === 0 && <p className="lib-muted">Không có chủ đề nào khớp “{filter}”.</p>}
          </div>
        </div>
      )}
    </div>
  );
}

// =========================================================
// PHẦN 2: TRANG THƯ VIỆN
// =========================================================

/** Trường mặc định: lấy từ URL, nếu không có thì lấy trường trong hồ sơ người dùng. */
function defaultInstitution(params: URLSearchParams, user: Account | null) {
  if (params.get('global') === 'true') {
    return '';
  }
  const fromUrl = params.get('institutionId');
  if (fromUrl !== null) {
    return fromUrl;
  }
  if (user?.organization && user.organization.institutionType !== 'OTHER') {
    return String(user.organization.institutionId);
  }
  return '';
}

/** Ghép các bộ lọc thành đường dẫn gọi API. */
function buildSearchUrl(f: {
  q: string;
  category: string;
  access: string;
  sort: string;
  institution: string;
  faculty: string;
  subject: string;
  tag: string;
  page: number;
}) {
  let url = `/documents?q=${encodeURIComponent(f.q)}&access=${f.access}&sort=${f.sort}`;
  if (f.category) url += `&category=${f.category}`;
  if (f.institution) url += `&institutionId=${f.institution}`;
  else url += '&global=true'; // Không chọn trường -> tìm trong toàn bộ thư viện
  if (f.faculty) url += `&facultyId=${f.faculty}`;
  if (f.subject) url += `&subjectId=${f.subject}`;
  if (f.tag) url += `&tag=${encodeURIComponent(f.tag)}`;
  url += `&page=${f.page}&size=${PAGE_SIZE}`;
  return url;
}

export default function DocumentsPage({ search, user }: { search: string; user: Account | null }) {
  // Đọc bộ lọc ban đầu từ URL, ví dụ #documents?q=java&tag=oop
  const params = new URLSearchParams(search);

  const [institution, setInstitution] = useState(defaultInstitution(params, user));
  const [faculty, setFaculty] = useState(params.get('facultyId') || '');
  const [q, setQ] = useState(params.get('q') || '');
  const [category, setCategory] = useState(params.get('category') || '');
  const [access, setAccess] = useState('');
  const [sort, setSort] = useState('newest');
  const [page, setPage] = useState(0);
  const [subject, setSubject] = useState('');
  const [tag, setTag] = useState(params.get('tag') || ''); // Nhãn đang lọc
  const [tagInput, setTagInput] = useState(params.get('tag') || ''); // Chữ đang gõ trong ô nhãn
  const [filtersOpen, setFiltersOpen] = useState(false); // Bộ lọc trên điện thoại
  const resultsRef = useRef<HTMLDivElement>(null);

  // Gõ nhãn xong 350ms mới lọc, tránh gọi API mỗi lần gõ một chữ
  useEffect(() => {
    const timer = setTimeout(() => {
      if (tagInput.trim() !== tag) {
        setTag(tagInput.trim());
        setPage(0);
      }
    }, 350);
    return () => clearTimeout(timer);
  }, [tagInput, tag]);

  const subjects = useRemote<{ id: number; name: string }[]>('/subjects');
  const categories = useRemote<CategoryInfo[]>('/documents/categories');
  const docs = useRemote<PageData<DocumentItem>>(
    buildSearchUrl({ q, category, access, sort, institution, faculty, subject, tag, page }),
  );

  // Tổng số tài liệu = cộng số tài liệu của từng chủ đề
  let totalInLibrary: number | undefined = undefined;
  if (categories.data) {
    totalInLibrary = 0;
    for (const item of categories.data) {
      totalInLibrary += item.count;
    }
  }

  // ----- Các hàm đổi bộ lọc (đổi bộ lọc thì quay về trang 1) -----

  function changeQuery(value: string) {
    setQ(value);
    setPage(0);
  }
  function changeCategory(value: string) {
    setCategory(value);
    setPage(0);
  }
  function changeSubject(value: string) {
    setSubject(value);
    setPage(0);
  }
  function changeAccess(value: string) {
    setAccess(value);
    setPage(0);
  }
  function changeSort(value: string) {
    setSort(value);
    setPage(0);
  }
  function clearTag() {
    setTagInput('');
    setTag('');
    setPage(0);
  }
  function clearOrganization() {
    setInstitution('');
    setFaculty('');
    setPage(0);
  }

  /** Bỏ các bộ lọc ở cột bên trái. */
  function clearSidebarFilters() {
    clearOrganization();
    setSubject('');
    clearTag();
    setAccess('');
  }

  /** Bỏ tất cả bộ lọc. */
  function resetAll() {
    clearSidebarFilters();
    setQ('');
    setCategory('');
    setSort('newest');
  }

  /** Chuyển trang và cuộn lên đầu danh sách kết quả. */
  function goToPage(next: number) {
    setPage(next);
    resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  const categoryName = categories.data?.find((c) => String(c.id) === category)?.name;
  const subjectName = subjects.data?.find((s) => String(s.id) === subject)?.name;

  // Danh sách các bộ lọc đang bật (hiện thành các nút có dấu x)
  const activeFilters: ActiveFilter[] = [];
  if (q) {
    activeFilters.push({ key: 'q', label: `“${q}”`, clear: () => changeQuery('') });
  }
  if (category) {
    activeFilters.push({ key: 'category', label: categoryName || 'Chủ đề', clear: () => changeCategory('') });
  }
  if (subject) {
    activeFilters.push({ key: 'subject', label: subjectName || 'Môn học', clear: () => changeSubject('') });
  }
  if (tag) {
    activeFilters.push({ key: 'tag', label: `#${tag}`, clear: clearTag });
  }
  if (access) {
    activeFilters.push({ key: 'access', label: access === 'FREE' ? 'Miễn phí' : 'Có phí', clear: () => changeAccess('') });
  }
  if (institution) {
    const label = faculty ? 'Trường & khoa đã chọn' : 'Trường đã chọn';
    activeFilters.push({ key: 'institution', label, clear: clearOrganization });
  }

  // Số bộ lọc đang bật ở cột trái
  const sidebarCount = [institution, subject, tag, access].filter((v) => v !== '').length;

  // ----- Phần kết quả -----
  let results = null;
  if (docs.data && docs.data.content.length > 0) {
    results = (
      <>
        <DocumentGrid documents={docs.data.content} />
        {docs.data.totalPages > 1 && <Pagination data={docs.data} page={page} onPage={goToPage} />}
      </>
    );
  } else if (docs.data) {
    results = (
      <div className="lib-empty">
        <span aria-hidden="true">
          <Icon name="search" size={26} />
        </span>
        <h3>Chưa tìm thấy tài liệu phù hợp</h3>
        <p>Thử một từ khóa khác, chọn chủ đề khác hoặc bỏ bớt bộ lọc nhé.</p>
        <button className="button secondary" onClick={resetAll}>
          Xem toàn bộ thư viện
        </button>
      </div>
    );
  }

  return (
    <div className="lib-page">
      {/* ===== Đầu trang và ô tìm kiếm ===== */}
      <section className="lib-hero">
        <div className="lib-hero-copy">
          <span className="section-kicker">GÓC TRI THỨC DÀNH CHO BẠN</span>
          <h1>Thư viện tài liệu</h1>
          <p>
            {totalInLibrary !== undefined ? (
              <>
                <strong>{totalInLibrary}</strong> tài liệu trong <strong>{categories.data?.length}</strong> chủ đề — tìm
                đúng thứ bạn cần học.
              </>
            ) : (
              'Tìm đúng tài liệu. Bắt đầu điều bạn muốn học.'
            )}
          </p>
        </div>

        <a href={user ? '#upload' : '#login'} className="button primary lib-hero-cta">
          <Icon name="plus" size={16} />
          Đóng góp tài liệu
        </a>

        <form
          className="lib-search"
          role="search"
          onSubmit={(event) => {
            event.preventDefault();
            const value = String(new FormData(event.currentTarget).get('q') || '');
            changeQuery(value.trim());
          }}
        >
          <label className="lib-input-icon">
            <Icon name="search" size={18} />
            <input
              key={q}
              name="q"
              defaultValue={q}
              maxLength={150}
              placeholder="Tìm theo tên hoặc nội dung tài liệu…"
              aria-label="Tìm tài liệu"
            />
          </label>
          <button className="button primary" type="submit">
            Tìm kiếm
          </button>
        </form>
      </section>

      {categories.data && categories.data.length > 0 && (
        <TopicBar categories={categories.data} total={totalInLibrary} value={category} onChange={changeCategory} />
      )}

      <div className="lib-layout">
        {/* Nút mở bộ lọc (chỉ hiện trên điện thoại) */}
        <button
          type="button"
          className="button secondary lib-filter-toggle"
          aria-expanded={filtersOpen}
          aria-controls="lib-filters"
          onClick={() => setFiltersOpen(!filtersOpen)}
        >
          <Icon name="settings" size={16} />
          Bộ lọc
          {sidebarCount > 0 && <span className="lib-count solid">{sidebarCount}</span>}
          <Icon name={filtersOpen ? 'close' : 'chevronRight'} size={15} />
        </button>

        {/* ===== Cột bộ lọc ===== */}
        <aside id="lib-filters" className={filtersOpen ? 'lib-filters is-open' : 'lib-filters'} aria-label="Bộ lọc">
          <div className="lib-filters-head">
            <h2>Bộ lọc</h2>
            {sidebarCount > 0 && (
              <button type="button" className="text-button" onClick={clearSidebarFilters}>
                Xóa lọc
              </button>
            )}
          </div>

          <div className="lib-filter-block">
            <InstitutionPicker
              emptyLabel="Toàn bộ thư viện"
              institutionId={institution}
              facultyId={faculty}
              onChange={(i, f) => {
                setInstitution(i);
                setFaculty(f);
                setPage(0);
              }}
            />
          </div>

          <div className="lib-filter-block">
            <label htmlFor="lib-subject">Môn học</label>
            <select id="lib-subject" value={subject} onChange={(e) => changeSubject(e.target.value)}>
              <option value="">Tất cả môn học</option>
              {subjects.data?.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>

          <div className="lib-filter-block">
            <label htmlFor="lib-tag">Nhãn</label>
            <input
              id="lib-tag"
              value={tagInput}
              maxLength={80}
              onChange={(e) => setTagInput(e.target.value)}
              placeholder="Ví dụ: Java"
            />
          </div>

          <div className="lib-filter-block">
            <span className="lib-filter-label" id="lib-access-label">
              Loại tài liệu
            </span>
            <div className="lib-segment" role="radiogroup" aria-labelledby="lib-access-label">
              {ACCESS_OPTIONS.map(([value, label]) => (
                <button
                  type="button"
                  key={value}
                  role="radio"
                  aria-checked={access === value}
                  onClick={() => changeAccess(value)}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <div className="lib-filter-tip">
            <Icon name="spark" size={18} />
            <p>
              <strong>Mẹo:</strong> chọn trường của bạn để thấy tài liệu sát chương trình học nhất.
            </p>
          </div>
        </aside>

        {/* ===== Kết quả ===== */}
        <div className="lib-results" ref={resultsRef}>
          <div className="lib-toolbar">
            <p aria-live="polite">
              {docs.data ? (
                <>
                  <strong>{docs.data.totalElements}</strong> tài liệu
                  {categoryName && (
                    <>
                      {' '}
                      trong <strong>{categoryName}</strong>
                    </>
                  )}
                </>
              ) : (
                'Đang tải thư viện…'
              )}
            </p>
            <label>
              Sắp xếp
              <select value={sort} onChange={(e) => changeSort(e.target.value)} aria-label="Sắp xếp tài liệu">
                {SORT_OPTIONS.map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {activeFilters.length > 0 && (
            <div className="lib-active" aria-label="Bộ lọc đang áp dụng">
              {activeFilters.map((item) => (
                <button
                  type="button"
                  key={item.key}
                  className="lib-active-chip"
                  onClick={item.clear}
                  aria-label={`Bỏ lọc ${item.label}`}
                >
                  {item.label}
                  <Icon name="close" size={13} />
                </button>
              ))}
              {activeFilters.length > 1 && (
                <button type="button" className="text-button" onClick={resetAll}>
                  Xóa tất cả
                </button>
              )}
            </div>
          )}

          <RemoteState loading={docs.loading} error={docs.error} onRetry={docs.refresh} />
          {results}
        </div>
      </div>
    </div>
  );
}
