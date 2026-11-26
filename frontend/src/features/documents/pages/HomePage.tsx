import { useState } from 'react';
import type { FormEvent } from 'react';
import type { Account } from '../../auth/types';
import { useRemote } from '../../../shared/hooks/useRemote';
import type { CatalogueStats, CategoryInfo, DocumentItem, PageData } from '../types';
import { DocumentCover, RemoteState } from '../components/LibraryUI';
import Icon from '../../../shared/components/ui/Icon';
import '../../../styles/home-dashboard.css';

/*
 * Trang chủ dạng dashboard:
 * - Lời chào + ô tìm kiếm
 * - 4 thẻ thống kê
 * - Khám phá theo chủ đề
 * - Các hàng tài liệu: mới, tải nhiều, gợi ý
 * - Hướng dẫn 3 bước và câu hỏi thường gặp
 */

// =========================================================
// PHẦN 1: HÀM HỖ TRỢ
// =========================================================

/** Đổi thời gian thành dạng "5 phút trước", "2 ngày trước"... */
function timeAgo(value: string) {
  // Backend trả giờ UTC không có chữ Z ở cuối -> thêm Z để trình duyệt hiểu đúng
  const hasTimezone = /[Z+]/.test(value);
  const date = new Date(hasTimezone ? value : value + 'Z');

  const minutes = Math.max(0, Math.round((Date.now() - date.getTime()) / 60000));
  if (minutes < 1) {
    return 'Vừa xong';
  }
  if (minutes < 60) {
    return `${minutes} phút trước`;
  }

  const hours = Math.round(minutes / 60);
  if (hours < 24) {
    return `${hours} giờ trước`;
  }

  const days = Math.round(hours / 24);
  if (days < 30) {
    return `${days} ngày trước`;
  }
  return date.toLocaleDateString('vi-VN');
}

// Icon cho từng chủ đề, dòng nào khớp trước thì dùng dòng đó
const TOPIC_ICONS: { pattern: RegExp; icon: string }[] = [
  { pattern: /python/i, icon: 'python' },
  { pattern: /java(?!script)/i, icon: 'coffee' }, // Java nhưng không phải JavaScript
  { pattern: /frontend|giao diện|web|javascript/i, icon: 'layout' },
  { pattern: /cơ sở dữ liệu|database|sql/i, icon: 'database' },
  { pattern: /cấu trúc dữ liệu|giải thuật|thuật toán/i, icon: 'branch' },
  { pattern: /mạng máy tính|network/i, icon: 'network' },
  { pattern: /an toàn|bảo mật|an ninh|mật mã/i, icon: 'shield' },
  { pattern: /trí tuệ nhân tạo|học máy|\bai\b/i, icon: 'spark' },
  { pattern: /hệ điều hành|linux/i, icon: 'terminal' },
  { pattern: /phần mềm|software/i, icon: 'settings' },
  { pattern: /đám mây|cloud/i, icon: 'cloud' }, // Đặt trước "toán" vì "Điện toán đám mây" cũng có chữ "toán"
  { pattern: /toán/i, icon: 'sigma' },
  { pattern: /thương mại|kinh doanh|commerce/i, icon: 'cart' },
  { pattern: /kỹ năng|học tập|tư duy/i, icon: 'bulb' },
];

function topicIcon(name: string) {
  for (const rule of TOPIC_ICONS) {
    if (rule.pattern.test(name)) {
      return rule.icon;
    }
  }
  return 'book';
}

// Thứ tự đổi cách sắp xếp khi bấm nút sắp xếp
const SORT_CYCLE: Record<string, { next: string; label: string }> = {
  newest: { next: 'popular', label: 'Mới nhất' },
  popular: { next: 'title', label: 'Tải nhiều' },
  title: { next: 'featured', label: 'Tên A–Z' },
  featured: { next: 'newest', label: 'Nổi bật' },
};

const STEPS = [
  { icon: 'search', title: 'Tìm đúng tài liệu', text: 'Lọc theo chủ đề, môn học, trường hoặc từ khóa chỉ trong vài giây.' },
  { icon: 'eye', title: 'Xem trước nội dung', text: 'Đọc phần mô tả và bản xem trước để chắc tài liệu phù hợp với bạn.' },
  { icon: 'download', title: 'Tải về & lưu lại', text: 'Tải tài liệu để học, hoặc lưu vào thư viện cá nhân để đọc sau.' },
];

const FAQS = [
  {
    q: 'Tài liệu trên Knowledge Hub có miễn phí không?',
    a: 'Phần lớn tài liệu là miễn phí. Bạn chỉ cần đăng nhập để tải về; tài liệu có phí được ghi rõ giá trên thẻ tài liệu.',
  },
  {
    q: 'Tôi có thể đóng góp tài liệu của mình không?',
    a: 'Có. Sau khi đăng nhập, chọn “Đóng góp tài liệu”. Tài liệu sẽ được kiểm duyệt trước khi hiển thị công khai.',
  },
  {
    q: 'Làm sao tìm tài liệu của trường tôi?',
    a: 'Trong thư viện, dùng bộ lọc Trường/Tổ chức và Khoa để chỉ xem tài liệu thuộc đơn vị của bạn.',
  },
  {
    q: 'Tài liệu đã lưu nằm ở đâu?',
    a: 'Mọi tài liệu đã lưu và đã tải nằm trong mục “Thư viện của tôi” để bạn mở lại bất cứ lúc nào.',
  },
];

// =========================================================
// PHẦN 2: CÁC THÀNH PHẦN NHỎ
// =========================================================

type StatCardProps = {
  icon: string;
  label: string;
  value: number | string | undefined;
  text: string;
};

/** Một thẻ thống kê. */
function StatCard({ icon, label, value, text }: StatCardProps) {
  return (
    <div className="home-stat-card">
      <span className="home-stat-icon" aria-hidden="true">
        <Icon name={icon} size={20} />
      </span>
      <div className="home-stat-body">
        <strong>{value ?? '—'}</strong>
        <span>{label}</span>
        <p>{text}</p>
      </div>
    </div>
  );
}

/** Thẻ tài liệu nhỏ: ảnh bìa vuông + tên + thời gian. */
function MaterialCard({ doc }: { doc: DocumentItem }) {
  return (
    <a className="material-card" href={`#document/${doc.id}`} aria-label={`Xem tài liệu: ${doc.title}`}>
      <DocumentCover doc={doc} />
      <h3>{doc.title}</h3>
      <p>
        {timeAgo(doc.publishedAt)} · {doc.downloads} lượt tải
      </p>
    </a>
  );
}

/** Một hàng 5 tài liệu, có nút đổi cách sắp xếp. */
function MaterialRow({ title, initialSort }: { title: string; initialSort: string }) {
  const [sort, setSort] = useState(initialSort);
  const docs = useRemote<PageData<DocumentItem>>(`/documents?size=5&sort=${sort}&global=true`);
  const sortLabel = SORT_CYCLE[sort].label;

  return (
    <section className="home-row">
      <div className="home-row-head">
        <h2>{title}</h2>
        <div className="home-row-actions">
          <button
            className="home-sort-button"
            onClick={() => setSort(SORT_CYCLE[sort].next)}
            title={`Sắp xếp: ${sortLabel}`}
            aria-label={`Đổi cách sắp xếp, hiện tại: ${sortLabel}`}
          >
            {/* Icon sắp xếp */}
            <svg
              width="17"
              height="17"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.7"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d="M7 4v16 M3 16l4 4 4-4 M15 4h4l-4 6h4 M15 14h4v6h-4z M15 17h4" />
            </svg>
            <span>{sortLabel}</span>
          </button>
          <a className="home-view-all" href="#documents">
            Xem tất cả
            <Icon name="chevronRight" size={16} />
          </a>
        </div>
      </div>

      <RemoteState loading={docs.loading} error={docs.error} onRetry={docs.refresh} />

      {docs.data && docs.data.content.length > 0 && (
        <div className="material-row">
          {docs.data.content.map((doc) => (
            <MaterialCard key={doc.id} doc={doc} />
          ))}
        </div>
      )}

      {docs.data && docs.data.content.length === 0 && (
        <div className="library-empty">
          <Icon name="book" size={30} />
          <h3>Chưa có tài liệu nào</h3>
        </div>
      )}
    </section>
  );
}

/** Danh sách chủ đề dạng lưới. */
function TopicSection() {
  const categories = useRemote<CategoryInfo[]>('/documents/categories');
  if (!categories.data || categories.data.length === 0) {
    return null;
  }

  return (
    <section className="home-section">
      <div className="home-row-head">
        <h2>Khám phá theo chủ đề</h2>
        <a className="home-view-all" href="#documents">
          Tất cả chủ đề
          <Icon name="chevronRight" size={16} />
        </a>
      </div>

      <div className="home-topics">
        {categories.data.map((item, index) => (
          <a
            key={item.id}
            className={`home-topic topic-tone-${index % 6}`}
            href={`#documents?category=${item.id}&global=true`}
          >
            <span className="home-topic-icon" aria-hidden="true">
              <Icon name={topicIcon(item.name)} size={20} />
            </span>
            <span>
              <strong>{item.name}</strong>
              <small>{item.count} tài liệu</small>
            </span>
            <Icon name="chevronRight" size={16} />
          </a>
        ))}
      </div>
    </section>
  );
}

/** Hướng dẫn 3 bước. */
function HowItWorks() {
  return (
    <section className="home-section home-steps" aria-labelledby="home-steps-title">
      <div className="home-section-intro">
        <span className="section-kicker">BẮT ĐẦU DỄ DÀNG</span>
        <h2 id="home-steps-title">Học hiệu quả hơn chỉ với 3 bước</h2>
      </div>
      <ol>
        {STEPS.map((step, index) => (
          <li key={step.title}>
            <span className="home-step-number">{index + 1}</span>
            <span className="home-step-icon" aria-hidden="true">
              <Icon name={step.icon} size={20} />
            </span>
            <h3>{step.title}</h3>
            <p>{step.text}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}

/** Câu hỏi thường gặp (bấm để mở câu trả lời). */
function Faq() {
  return (
    <section className="home-section home-faq" aria-labelledby="home-faq-title">
      <div className="home-section-intro">
        <span className="section-kicker">CÂU HỎI THƯỜNG GẶP</span>
        <h2 id="home-faq-title">Bạn cần hỗ trợ?</h2>
      </div>
      <div className="home-faq-list">
        {FAQS.map((item) => (
          <details key={item.q}>
            <summary>
              {item.q}
              <Icon name="plus" size={16} />
            </summary>
            <p>{item.a}</p>
          </details>
        ))}
      </div>
    </section>
  );
}

// =========================================================
// PHẦN 3: TRANG CHỦ
// =========================================================

export default function HomePage({ user }: { user: Account | null }) {
  const catalogue = useRemote<CatalogueStats>('/documents/stats');
  const name = user ? user.displayName || user.username : null;

  /** Bấm tìm -> chuyển sang trang Thư viện với từ khoá. */
  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const keyword = String(new FormData(event.currentTarget).get('q') || '');
    location.hash = `documents?q=${encodeURIComponent(keyword)}`;
  }

  return (
    <div className="home-dashboard">
      {/* ===== Lời chào và ô tìm kiếm ===== */}
      <section className="home-welcome">
        <h1>
          {name ? `Chào mừng trở lại, ${name}!` : 'Chào mừng đến Knowledge Hub!'} <span aria-hidden="true">👋</span>
        </h1>
        <p>Sẵn sàng khám phá hôm nay chứ?</p>
        <form className="home-search" role="search" onSubmit={search}>
          <label>
            <Icon name="search" size={18} />
            <input name="q" aria-label="Tìm tài liệu" placeholder="Nhập tên môn học hoặc tên tài liệu" maxLength={150} />
          </label>
          <button type="submit" aria-label="Tìm kiếm">
            <Icon name="arrow" size={18} />
          </button>
        </form>
      </section>

      {/* ===== 4 thẻ thống kê ===== */}
      <section className="home-stats" aria-label="Thống kê">
        <StatCard
          icon="book"
          label="Tài liệu"
          value={catalogue.data?.documents}
          text="Tài liệu đang có trong thư viện, sẵn sàng để bạn khám phá."
        />
        <StatCard
          icon="gift"
          label="Miễn phí"
          value={catalogue.data?.freeDocuments}
          text={user ? 'Tài liệu bạn có thể tải về ngay bây giờ.' : 'Tài liệu bạn có thể tải về ngay sau khi đăng nhập.'}
        />
        <StatCard
          icon="grid"
          label="Chủ đề"
          value={catalogue.data?.categories}
          text="Chủ đề đa dạng để bạn bắt đầu từ điều mình thích."
        />
        <StatCard
          icon="download"
          label="Lượt tải"
          value={catalogue.data?.downloads}
          text="Cộng đồng đã tải tài liệu để học tập mỗi ngày."
        />
      </section>

      <TopicSection />
      <MaterialRow title="Tài liệu mới" initialSort="newest" />
      <MaterialRow title="Tải nhiều nhất" initialSort="popular" />
      <HowItWorks />
      <MaterialRow title="Gợi ý cho bạn" initialSort="featured" />

      {/* Đã đăng nhập: mời đóng góp tài liệu */}
      {user && (
        <section className="home-contribute">
          <span className="home-step-icon" aria-hidden="true">
            <Icon name="upload" size={22} />
          </span>
          <div>
            <h2>Chia sẻ tài liệu bạn đang có</h2>
            <p>Bài giảng, đề cương hay ghi chú hay của bạn có thể giúp hàng trăm bạn học khác.</p>
          </div>
          <a className="button primary" href="#upload">
            <Icon name="plus" size={16} />
            Đóng góp tài liệu
          </a>
        </section>
      )}

      <Faq />

      {/* Chưa đăng nhập: mời tạo tài khoản */}
      {!user && (
        <section className="public-cta">
          <span className="tile-icon purple">
            <Icon name="bookmark" size={22} />
          </span>
          <div>
            <h2>Biến tài liệu hay thành góc học tập của bạn.</h2>
            <p>Lưu lại để đọc sau, tải về để học tiếp và tìm lại bất cứ lúc nào.</p>
          </div>
          <a className="button primary" href="#register">
            Tạo tài khoản miễn phí
            <Icon name="arrow" size={16} />
          </a>
        </section>
      )}
    </div>
  );
}
