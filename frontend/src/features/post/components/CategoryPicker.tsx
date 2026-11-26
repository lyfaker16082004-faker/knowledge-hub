import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react';
import Icon from '../../../shared/components/ui/Icon';

/**
 * Ô chọn chủ đề có tìm kiếm — dùng tốt cả khi có hàng trăm chủ đề.
 *
 * - Gõ để lọc, không cần gõ dấu ("co so du lieu" vẫn tìm được "Cơ sở dữ liệu")
 * - Khi chưa gõ gì: hiện các chủ đề phổ biến (nhiều bài nhất) rồi đến toàn bộ chủ đề theo A-Z
 * - Không tìm thấy: gợi ý dùng từ khoá làm THẺ và chọn chủ đề "Khác"
 * - Dùng được bằng bàn phím: ↑ ↓ để di chuyển, Enter để chọn, Esc để đóng
 */

export type CategoryChoice = { id: number; name: string; count?: number };

type Props = {
  categories: CategoryChoice[];
  value: string;                       // id chủ đề đang chọn (chuỗi rỗng = chưa chọn)
  onChange: (id: string) => void;
  onUseAsTag: (text: string) => void;  // Thêm từ khoá vào danh sách thẻ
};

const POPULAR_COUNT = 6;
const OTHER_SLUG_NAMES = ['khác', 'khac'];

/** Bỏ dấu tiếng Việt và viết thường để so sánh. VD: "Cơ sở Dữ liệu" -> "co so du lieu" */
function normalize(text: string) {
  return text.normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'd').toLowerCase().trim();
}

export default function CategoryPicker({ categories, value, onChange, onUseAsTag }: Props) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [activeIndex, setActiveIndex] = useState(0);
  const rootRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  const selected = categories.find(c => String(c.id) === value);
  const otherCategory = categories.find(c => OTHER_SLUG_NAMES.includes(normalize(c.name)));

  // Danh sách hiển thị (tính lại khi đổi từ khoá)
  const sections = useMemo(() => {
    const keyword = normalize(query);
    const byName = [...categories].sort((a, b) => a.name.localeCompare(b.name, 'vi'));

    if (!keyword) {
      const popular = [...categories]
        .filter(c => (c.count || 0) > 0)
        .sort((a, b) => (b.count || 0) - (a.count || 0))
        .slice(0, POPULAR_COUNT);
      const sectionList = [];
      if (popular.length) sectionList.push({ title: 'Phổ biến', items: popular });
      sectionList.push({ title: `Tất cả chủ đề (${categories.length})`, items: byName });
      return sectionList;
    }

    // Có từ khoá: tên bắt đầu bằng từ khoá xếp trước, chứa từ khoá xếp sau
    const startsWith = byName.filter(c => normalize(c.name).startsWith(keyword));
    const contains = byName.filter(c => !normalize(c.name).startsWith(keyword) && normalize(c.name).includes(keyword));
    return [{ title: 'Kết quả', items: [...startsWith, ...contains] }];
  }, [categories, query]);

  // Danh sách phẳng để điều khiển bằng phím ↑ ↓
  const flatItems = sections.flatMap(section => section.items);

  // Đóng khi bấm ra ngoài
  useEffect(() => {
    if (!open) return;
    function handleClick(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [open]);

  // Cuộn để mục đang chọn bằng bàn phím luôn nhìn thấy được
  useEffect(() => {
    listRef.current?.querySelector('[data-active="true"]')?.scrollIntoView({ block: 'nearest' });
  }, [activeIndex]);

  function choose(category: CategoryChoice) {
    onChange(String(category.id));
    setQuery('');
    setOpen(false);
  }

  function addKeywordAsTag() {
    onUseAsTag(query.trim());
    if (otherCategory) onChange(String(otherCategory.id));
    setQuery('');
    setOpen(false);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setOpen(true);
      setActiveIndex(i => Math.min(i + 1, flatItems.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setActiveIndex(i => Math.max(i - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault(); // Không gửi form
      if (flatItems[activeIndex]) choose(flatItems[activeIndex]);
      else if (query.trim()) addKeywordAsTag();
    } else if (event.key === 'Escape') {
      setOpen(false);
    }
  }

  let flatPosition = -1; // Đếm vị trí khi vẽ để so với activeIndex

  return (
    <div className={`cat-picker${open ? ' is-open' : ''}`} ref={rootRef}>
      <div className="cat-picker-field">
        <Icon name="search" size={16} />
        <input id="post-category" role="combobox" aria-expanded={open} aria-controls="cat-picker-list" aria-autocomplete="list"
          autoComplete="off"
          value={open ? query : selected?.name || ''}
          placeholder={selected ? selected.name : 'Tìm chủ đề, VD: java, cơ sở dữ liệu…'}
          onFocus={() => { setOpen(true); setActiveIndex(0); }}
          onChange={event => { setQuery(event.target.value); setOpen(true); setActiveIndex(0); }}
          onKeyDown={handleKeyDown} />
        {selected && !open && (
          <button type="button" className="cat-picker-clear" aria-label="Bỏ chọn chủ đề" onClick={() => onChange('')}>
            <Icon name="close" size={14} />
          </button>
        )}
      </div>

      {open && (
        <div className="cat-picker-panel">
          {flatItems.length > 0 ? (
            <ul id="cat-picker-list" role="listbox" ref={listRef}>
              {sections.map(section => (
                <li key={section.title} role="presentation">
                  <span className="cat-picker-group">{section.title}</span>
                  <ul role="presentation">
                    {section.items.map(category => {
                      flatPosition++;
                      const isActive = flatPosition === activeIndex;
                      const position = flatPosition;
                      return (
                        <li key={section.title + category.id} role="option" aria-selected={String(category.id) === value}
                          data-active={isActive} className={isActive ? 'is-active' : ''}
                          onMouseEnter={() => setActiveIndex(position)}
                          onMouseDown={event => event.preventDefault()}
                          onClick={() => choose(category)}>
                          <span>{category.name}</span>
                          {String(category.id) === value
                            ? <Icon name="check" size={15} />
                            : category.count ? <small>{category.count} bài</small> : null}
                        </li>
                      );
                    })}
                  </ul>
                </li>
              ))}
            </ul>
          ) : (
            <div className="cat-picker-empty">
              <p>Không có chủ đề nào khớp “<strong>{query.trim()}</strong>”.</p>
              <button type="button" className="button secondary compact" onClick={addKeywordAsTag}>
                <Icon name="hash" size={15} />
                Dùng “{query.trim()}” làm thẻ{otherCategory ? ' và chọn chủ đề “Khác”' : ''}
              </button>
              <small>Thẻ giúp mọi người tìm đúng bài của bạn. Quản trị viên sẽ tạo chủ đề mới khi có nhiều bài cùng thẻ.</small>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
