import Icon from '../../../shared/components/ui/Icon';
import type { DocumentItem, PageData } from '../types';
import { levelNames, priceLabel, formatName } from '../types';

/*
 * Các thành phần giao diện dùng chung cho Thư viện tài liệu:
 * ảnh bìa, thẻ tài liệu, lưới tài liệu, trạng thái tải và phân trang.
 */

// =========================================================
// PHẦN 1: CHỌN ẢNH BÌA CHO TÀI LIỆU
// =========================================================

// Tài liệu mẫu có sẵn ảnh bìa riêng (theo slug)
const slugCovers: Record<string, string> = {
  'ke-hoach-tu-hoc': 'ke-hoach-tu-hoc.webp',
  'rest-api-checklist': 'rest-api-checklist.webp',
  'git-lam-viec-nhom': 'git-lam-viec-nhom.webp',
  'java-can-ban': 'java-can-ban.webp',
  'mysql-tieng-viet': 'mysql-tieng-viet.webp',
  'react-typescript': 'react-typescript.webp',
  'spring-boot-chuyen-sau': 'spring-boot-chuyen-sau.webp',
};

// Tài liệu người dùng đăng: chọn ảnh theo từ khoá trong tên / chủ đề / tags.
// Thứ tự quan trọng: dòng nào khớp trước thì dùng dòng đó
// (ví dụ phải kiểm tra JavaScript trước Java).
const topicCovers: { pattern: RegExp; image: string }[] = [
  { pattern: /\bspring\b/, image: 'spring-boot-chuyen-sau.webp' },
  { pattern: /\b(react|typescript|javascript|frontend|html|css)\b|giao diện/, image: 'react-typescript.webp' },
  { pattern: /\bpython\b|django|pandas/, image: 'topic-python.webp' },
  { pattern: /\bjava\b|\boop\b|hướng đối tượng/, image: 'java-can-ban.webp' },
  { pattern: /\b(sql|mysql|database)\b|cơ sở dữ liệu/, image: 'mysql-tieng-viet.webp' },
  { pattern: /\b(git|github)\b|làm việc nhóm/, image: 'git-lam-viec-nhom.webp' },
  { pattern: /\b(api|rest|backend)\b|web service/, image: 'rest-api-checklist.webp' },
  { pattern: /cấu trúc dữ liệu|giải thuật|thuật toán|\balgorithm/, image: 'topic-algorithm.webp' },
  { pattern: /mạng máy tính|giao thức|\b(tcp|network)\b/, image: 'topic-network.webp' },
  { pattern: /an toàn thông tin|bảo mật|an ninh mạng|mật mã|\bsecurity\b/, image: 'topic-security.webp' },
  { pattern: /học máy|machine learning|deep learning/, image: 'topic-ai.webp' },
  { pattern: /trí tuệ nhân tạo|\bai\b/, image: 'topic-robot.webp' },
  { pattern: /hệ điều hành|\b(linux|unix|operating system)\b/, image: 'topic-os.webp' },
  { pattern: /điện toán đám mây|\bcloud\b/, image: 'topic-cloud.webp' },
  { pattern: /thương mại điện tử|\be-?commerce\b|kinh doanh/, image: 'topic-ecommerce.webp' },
  { pattern: /toán|\bmath/, image: 'topic-math.webp' },
  { pattern: /phần mềm|\b(software|agile|scrum|testing)\b|kiểm thử/, image: 'topic-software.webp' },
  { pattern: /kế hoạch|tự học|kỹ năng|học tập|tư duy/, image: 'ke-hoach-tu-hoc.webp' },
];

type CoverInput = Pick<DocumentItem, 'slug' | 'title' | 'category' | 'subjectName' | 'tags'>;

/** Trả về tên file ảnh bìa (trong thư mục public/covers) phù hợp với tài liệu. */
export function coverFor(doc: CoverInput) {
  // Bước 1: Tài liệu mẫu -> dùng ảnh riêng
  if (slugCovers[doc.slug]) {
    return slugCovers[doc.slug];
  }

  // Bước 2: Ghép tên, chủ đề, môn học, tags thành một chuỗi chữ thường để dò từ khoá
  const words: string[] = [doc.title, doc.category];
  if (doc.subjectName) {
    words.push(doc.subjectName);
  }
  if (doc.tags) {
    words.push(...doc.tags);
  }
  const text = words.filter(Boolean).join(' ').toLocaleLowerCase('vi');

  // Bước 3: Tìm từ khoá đầu tiên khớp
  for (const topic of topicCovers) {
    if (topic.pattern.test(text)) {
      return topic.image;
    }
  }

  // Bước 4: Không khớp gì -> ảnh mặc định
  return 'topic-generic.webp';
}

const coverThemes = ['violet', 'peach', 'mint', 'sky', 'rose', 'sand'];

/** Ảnh bìa của tài liệu (kèm tên và định dạng file). */
export function DocumentCover({ doc, large = false }: { doc: DocumentItem; large?: boolean }) {
  const theme = coverThemes.includes(doc.coverStyle) ? doc.coverStyle : 'violet';
  const image = coverFor(doc);
  let className = `document-cover cover-${theme}`;
  if (large) {
    className += ' cover-large';
  }

  return (
    <div className={className} aria-hidden="true">
      <img className="document-cover-image" src={`/covers/${image}`} alt="" loading="lazy" decoding="async" />
      <span className="document-cover-title">{doc.title}</span>
      <span className="cover-format">{doc.fileFormat || 'MD'}</span>
    </div>
  );
}

// =========================================================
// PHẦN 2: THẺ TÀI LIỆU VÀ LƯỚI TÀI LIỆU
// =========================================================

/** Thẻ hiển thị một tài liệu trong danh sách. Bấm vào để mở trang chi tiết. */
export function DocumentCard({ doc }: { doc: DocumentItem }) {
  const isFree = doc.accessType === 'FREE';
  const level = levelNames[doc.level] || doc.level;

  return (
    <article className="document-card">
      <a className="document-card-link" href={`#document/${doc.id}`} aria-label={`Xem tài liệu: ${doc.title}`}>
        <DocumentCover doc={doc} />

        <div className="document-card-body">
          <div className="card-topline">
            <span className="category-label">{doc.category}</span>
            <span className={isFree ? 'access-badge free' : 'access-badge paid'}>
              {isFree ? 'Miễn phí' : 'Có phí'}
            </span>
          </div>

          <h3>{doc.title}</h3>
          <p>{doc.summary}</p>

          {doc.institutionName && (
            <div className="document-organization">
              {doc.institutionName}
              {doc.facultyName && <span> · {doc.facultyName}</span>}
            </div>
          )}

          <div className="card-meta">
            <span>
              {formatName(doc.fileFormat || 'MD')} · {level}
            </span>
            <span>
              <Icon name="download" size={13} />
              {doc.downloads}
            </span>
          </div>

          <div className="card-bottom">
            <strong className={isFree ? 'free-price' : ''}>{priceLabel(doc)}</strong>
            <span>
              Xem tài liệu <Icon name="arrow" size={14} />
            </span>
          </div>
        </div>
      </a>
    </article>
  );
}

/** Lưới nhiều thẻ tài liệu. */
export function DocumentGrid({ documents }: { documents: DocumentItem[] }) {
  return (
    <div className="document-grid">
      {documents.map((doc) => (
        <DocumentCard key={doc.id} doc={doc} />
      ))}
    </div>
  );
}

// =========================================================
// PHẦN 3: TRẠNG THÁI ĐANG TẢI / LỖI VÀ PHÂN TRANG
// =========================================================

type RemoteStateProps = {
  loading: boolean;
  error: string;
  onRetry: () => void;
};

/** Hiện vòng xoay khi đang tải, hoặc thông báo lỗi kèm nút "Thử lại". */
export function RemoteState({ loading, error, onRetry }: RemoteStateProps) {
  if (loading) {
    return (
      <div className="library-loading" role="status">
        <span className="spinner" />
        Đang tìm những tài liệu dành cho bạn…
      </div>
    );
  }

  if (error) {
    return (
      <div className="library-empty" role="alert">
        <Icon name="book" size={32} />
        <h3>Chưa tải được nội dung</h3>
        <p>{error}</p>
        <button className="button secondary" onClick={onRetry}>
          <Icon name="refresh" size={16} />
          Thử lại
        </button>
      </div>
    );
  }

  return null;
}

type PaginationProps = {
  data: Pick<PageData<unknown>, 'totalElements' | 'totalPages'>;
  page: number; // Trang hiện tại, bắt đầu từ 0
  onPage: (page: number) => void;
};

/** Thanh phân trang: Trước / Trang x / y / Sau. */
export function Pagination({ data, page, onPage }: PaginationProps) {
  const totalPages = Math.max(1, data.totalPages);
  const isFirstPage = page === 0;
  const isLastPage = page + 1 >= data.totalPages;

  return (
    <div className="library-pagination">
      <span>{data.totalElements} kết quả</span>
      <div>
        <button className="button secondary compact" disabled={isFirstPage} onClick={() => onPage(page - 1)}>
          <Icon name="chevronLeft" size={16} />
          Trước
        </button>
        <span>
          Trang {page + 1} / {totalPages}
        </span>
        <button className="button secondary compact" disabled={isLastPage} onClick={() => onPage(page + 1)}>
          Sau
          <Icon name="chevronRight" size={16} />
        </button>
      </div>
    </div>
  );
}
