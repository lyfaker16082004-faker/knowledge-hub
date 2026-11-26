import { useEffect, useState, type FormEvent } from "react";
import Icon from "../../../shared/components/ui/Icon";
import { errorMessage, get, mutate } from "../../../shared/api/http-client";
import { useRemote } from "../../../shared/hooks/useRemote";
import type { Account } from "../../auth/types";
import type { DocumentItem, PageData } from "../../documents/types";
import type {
  FeedSidebar,
  PostDetail,
  PostPage,
  PostSummary,
  PostType,
} from "../types";
import {
  PostCard,
  PostEditor,
  UserAvatar,
  authorName,
  timeAgo,
} from "../components/PostUI";
import NotificationBell from "../components/NotificationBell";

/*
 * Trang Bảng tin cộng đồng & Hỏi đáp (#community) - Sprint 3.
 * - Các tab: Mới nhất, Đang theo dõi, Phổ biến, Chưa trả lời...
 * - Lọc theo loại bài, chủ đề, thẻ và từ khoá
 * - Đăng câu hỏi / bài chia sẻ, sửa và xoá bài của mình
 * - Cột bên: chủ đề, thẻ thịnh hành, thành viên tích cực
 */

// Số bài mỗi trang
const PAGE_SIZE = 10;
// Các tab chính của bảng tin (auth: true = phải đăng nhập mới thấy)
const tabs = [
  { key: "newest", label: "Mới nhất", icon: "clock" },
  { key: "following", label: "Đang theo dõi", icon: "users", auth: true },
  { key: "popular", label: "Phổ biến", icon: "trending" },
  { key: "unanswered", label: "Chưa trả lời", icon: "question" },
];
// Lọc theo loại bài: câu hỏi cần giải đáp hay bài chia sẻ kiến thức
const typeOptions = [
  { key: "", label: "Tất cả" },
  { key: "QUESTION", label: "Câu hỏi", icon: "question" },
  { key: "ARTICLE", label: "Bài chia sẻ", icon: "book" },
];
// Các tab cá nhân: bài của tôi, bài đã lưu
const personalTabs = [
  { key: "mine", label: "Bài viết của tôi", icon: "edit" },
  { key: "saved", label: "Bài viết đã lưu", icon: "bookmark" },
];

// Bộ lọc hiện tại (lưu trên URL để có thể chia sẻ link)
type Filters = {
  tab: string;
  q: string;
  category: string;
  tag: string;
  type: string;
};
/** Đọc bộ lọc từ phần ?... của URL. */
function readFilters(search: string): Filters {
  const params = new URLSearchParams(search);
  const type = params.get("type") || "";
  return {
    tab: params.get("tab") || "newest",
    q: params.get("q") || "",
    category: params.get("category") || "",
    tag: params.get("tag") || "",
    type: type === "QUESTION" || type === "ARTICLE" ? type : "",
  };
}
/** Ghi bộ lọc lên URL -> App sẽ tạo lại trang với bộ lọc mới. */
function go(filters: Filters) {
  const params = new URLSearchParams();
  if (filters.tab !== "newest") params.set("tab", filters.tab);
  for (const key of ["q", "category", "tag", "type"] as const)
    if (filters[key]) params.set(key, filters[key]);
  const query = params.toString();
  location.hash = `community${query ? `?${query}` : ""}`;
}

export default function CommunityFeedPage({
  user,
  search,
}: {
  user: Account | null;
  search: string;
}) {
  const filters = readFilters(search);
  const tab =
    !user && ["following", "mine", "saved"].includes(filters.tab)
      ? "newest"
      : filters.tab;
  const sidebar = useRemote<FeedSidebar>("/posts/sidebar");
  const [items, setItems] = useState<PostSummary[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [editor, setEditor] = useState<{
    open: boolean;
    post?: PostDetail;
    type?: PostType;
  }>({ open: false });
  const [showAllCategories, setShowAllCategories] = useState(false);
  const [query, setQuery] = useState(filters.q);
  const [notice, setNotice] = useState("");
  const set = (patch: Partial<Filters>) => go({ ...filters, tab, ...patch });
  // Tab "Chưa trả lời" chỉ gồm câu hỏi nên bỏ qua bộ lọc loại bài
  const type = tab === "unanswered" ? "" : filters.type;

  const queryString = `tab=${tab}&q=${encodeURIComponent(filters.q)}&tag=${encodeURIComponent(filters.tag)}${filters.category ? `&category=${filters.category}` : ""}${type ? `&type=${type}` : ""}&size=${PAGE_SIZE}`;
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    get<PostPage>(`/posts?${queryString}&page=${page}`, controller.signal)
      .then((data) => {
        setItems((list) =>
          page === 0
            ? data.content
            : [
                ...list,
                ...data.content.filter(
                  (p) => !list.some((old) => old.id === p.id),
                ),
              ],
        );
        setTotal(data.totalElements);
        setTotalPages(data.totalPages);
      })
      .catch((e) => {
        if (!controller.signal.aborted) setError(errorMessage(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [queryString, page, reload]);
  useEffect(() => {
    if (!notice) return;
    const timer = setTimeout(() => setNotice(""), 3500);
    return () => clearTimeout(timer);
  }, [notice]);

  /** Tải lại bảng tin và cột bên. */
  function refreshAll() {
    setPage(0);
    setReload((n) => n + 1);
    sidebar.refresh();
  }
  /** Mở hộp thoại đăng bài mới. */
  function openEditor(postType: PostType) {
    if (!user) {
      location.hash = "login";
      return;
    }
    setEditor({ open: true, type: postType });
  }
  /** Mở hộp thoại sửa bài (cần tải nội dung đầy đủ trước). */
  async function edit(post: PostSummary) {
    try {
      setEditor({
        open: true,
        post: await get<PostDetail>(`/posts/${post.id}`),
      });
    } catch (e) {
      setNotice(errorMessage(e));
    }
  }
  /** Xoá bài viết của mình. */
  async function remove(post: PostSummary) {
    if (
      !confirm(
        `Xóa bài viết "${post.title}"? Bài viết sẽ không còn hiển thị với mọi người.`,
      )
    )
      return;
    try {
      await mutate(`/posts/${post.id}`, "DELETE");
      setItems((list) => list.filter((p) => p.id !== post.id));
      setTotal((n) => n - 1);
      setNotice("Đã xóa bài viết");
      sidebar.refresh();
    } catch (e) {
      setNotice(errorMessage(e));
    }
  }
  /** Bấm tìm kiếm. */
  function submitSearch(event: FormEvent) {
    event.preventDefault();
    set({ q: query.trim() });
  }

  const categories = sidebar.data?.categories || [];
  const activeCategory = categories.find(
    (c) => String(c.id) === filters.category,
  );
  const userName = user ? user.displayName || user.username : "";
  const hasFilter = Boolean(
    filters.q || filters.category || filters.tag || type,
  );
  // Cột trái chỉ hiện chủ đề đã có bài (và chủ đề đang chọn); các chủ đề trống được gom lại
  const busyCategories = categories.filter(
    (c) => c.count > 0 || String(c.id) === filters.category,
  );
  const quietCount = categories.length - busyCategories.length;
  const visibleCategories = showAllCategories ? categories : busyCategories;
  const allTabs = [...tabs, ...personalTabs];

  return (
    <div className="cf-page">
      <aside className="cf-left" aria-label="Danh mục cộng đồng">
        <section className="cf-panel">
          <h2 className="cf-panel-title">Danh mục</h2>
          <nav
            className={`cf-cat-list${showAllCategories ? " is-expanded" : ""}`}
          >
            <button
              type="button"
              className={!filters.category ? "on" : ""}
              aria-current={!filters.category ? "true" : undefined}
              onClick={() => set({ category: "" })}
            >
              <Icon name="grid" size={18} />
              <span>Tất cả</span>
              <small>{sidebar.data?.totalPosts ?? ""}</small>
            </button>
            {visibleCategories.map((c) => (
              <button
                type="button"
                key={c.id}
                className={String(c.id) === filters.category ? "on" : ""}
                aria-current={
                  String(c.id) === filters.category ? "true" : undefined
                }
                onClick={() => set({ category: String(c.id) })}
              >
                <Icon name="hash" size={18} />
                <span>{c.name}</span>
                <small>{c.count}</small>
              </button>
            ))}
          </nav>
          {quietCount > 0 && (
            <button
              type="button"
              className="cf-cat-more"
              aria-expanded={showAllCategories}
              onClick={() => setShowAllCategories((v) => !v)}
            >
              {showAllCategories
                ? "Thu gọn"
                : `${quietCount} chủ đề chưa có bài`}
              <Icon name={showAllCategories ? "up" : "down"} size={14} />
            </button>
          )}
        </section>
        {user && (
          <section className="cf-panel">
            <h2 className="cf-panel-title">Của bạn</h2>
            <nav className="cf-cat-list">
              {personalTabs.map((t) => (
                <button
                  type="button"
                  key={t.key}
                  className={tab === t.key ? "on" : ""}
                  onClick={() => set({ tab: t.key })}
                >
                  <Icon name={t.icon} size={18} />
                  <span>{t.label}</span>
                </button>
              ))}
              <a href="#mine?tab=following">
                <Icon name="users" size={18} />
                <span>Người tôi theo dõi</span>
              </a>
            </nav>
          </section>
        )}
        {!!sidebar.data?.activeMembers.length && (
          <section className="cf-panel">
            <h2 className="cf-panel-title">Thành viên tích cực</h2>
            <ul className="cf-members">
              {sidebar.data.activeMembers.map((m) => (
                <li key={m.id}>
                  <a href={`#person/${m.id}`}>
                    <UserAvatar
                      name={m.name}
                      url={m.avatarUrl}
                      id={m.id}
                      size="sm"
                    />
                    <span>{m.name}</span>
                    <small>{m.posts} hoạt động</small>
                  </a>
                </li>
              ))}
            </ul>
          </section>
        )}
      </aside>

      <main className="cf-main">
        <section className="cf-panel cf-ask">
          {user ? (
            <UserAvatar name={userName} url={user.avatarUrl} id={user.id} />
          ) : (
            <span className="cf-avatar md t2" aria-hidden="true">
              <Icon name="user" size={18} />
            </span>
          )}
          <button
            type="button"
            className="cf-ask-input"
            onClick={() => openEditor("QUESTION")}
          >
            {user
              ? "Bạn đang vướng ở đâu? Hỏi ngay, có thể dán ảnh lỗi…"
              : "Đăng nhập để đặt câu hỏi hoặc chia sẻ kiến thức…"}
          </button>
          <div className="cf-ask-actions">
            <button
              type="button"
              className="button primary cf-ask-btn"
              onClick={() => openEditor("QUESTION")}
              aria-label="Đặt câu hỏi"
            >
              <Icon name="question" size={17} />
              <span>Đặt câu hỏi</span>
            </button>
            <button
              type="button"
              className="button secondary cf-ask-btn"
              onClick={() => openEditor("ARTICLE")}
              aria-label="Chia sẻ kiến thức"
            >
              <Icon name="book" size={17} />
              <span>Chia sẻ</span>
            </button>
            {/* Chuông thông báo bình luận (chỉ khi đã đăng nhập) */}
            {user && <NotificationBell />}
          </div>
        </section>

        <div className="cf-toolbar">
          <div className="cf-tabs" role="tablist" aria-label="Bảng tin">
            {tabs
              .filter((t) => !t.auth || user)
              .map((t) => (
                <button
                  type="button"
                  role="tab"
                  key={t.key}
                  aria-selected={tab === t.key}
                  className={tab === t.key ? "on" : ""}
                  onClick={() => set({ tab: t.key })}
                >
                  <Icon name={t.icon} size={16} />
                  {t.label}
                </button>
              ))}
          </div>
          <form className="cf-search" role="search" onSubmit={submitSearch}>
            <Icon name="search" size={17} />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              maxLength={150}
              placeholder="Tìm câu hỏi, bài viết…"
              aria-label="Tìm trong cộng đồng"
            />
            {query && (
              <button
                type="button"
                aria-label="Xóa từ khóa"
                onClick={() => {
                  setQuery("");
                  set({ q: "" });
                }}
              >
                <Icon name="close" size={15} />
              </button>
            )}
          </form>
        </div>

        <div className="cf-subbar">
          {tab !== "unanswered" && (
            <div
              className="cf-type-filter"
              role="radiogroup"
              aria-label="Loại bài viết"
            >
              {typeOptions.map((option) => (
                <button
                  type="button"
                  role="radio"
                  key={option.key}
                  aria-checked={type === option.key}
                  className={type === option.key ? "on" : ""}
                  onClick={() => set({ type: option.key })}
                >
                  {option.icon && <Icon name={option.icon} size={14} />}
                  {option.label}
                </button>
              ))}
            </div>
          )}
          {(hasFilter || personalTabs.some((t) => t.key === tab)) && (
            <div className="cf-active-filters">
              <span>
                {loading && page === 0 ? "Đang lọc…" : `${total} bài viết`}
              </span>
              {personalTabs.some((t) => t.key === tab) && (
                <button type="button" onClick={() => set({ tab: "newest" })}>
                  {allTabs.find((t) => t.key === tab)?.label}
                  <Icon name="close" size={13} />
                </button>
              )}
              {activeCategory && (
                <button type="button" onClick={() => set({ category: "" })}>
                  {activeCategory.name}
                  <Icon name="close" size={13} />
                </button>
              )}
              {filters.tag && (
                <button type="button" onClick={() => set({ tag: "" })}>
                  #{filters.tag}
                  <Icon name="close" size={13} />
                </button>
              )}
              {filters.q && (
                <button
                  type="button"
                  onClick={() => {
                    setQuery("");
                    set({ q: "" });
                  }}
                >
                  “{filters.q}”<Icon name="close" size={13} />
                </button>
              )}
            </div>
          )}
        </div>
        {notice && (
          <div className="cf-toast" role="status">
            {notice}
          </div>
        )}

        {error && (
          <div className="cf-panel cf-error" role="alert">
            <p>{error}</p>
            <button
              type="button"
              className="button secondary compact"
              onClick={() => setReload((n) => n + 1)}
            >
              <Icon name="refresh" size={15} />
              Thử lại
            </button>
          </div>
        )}
        {items.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            signedIn={!!user}
            onTag={(tag) => set({ tag })}
            onEdit={edit}
            onDelete={remove}
          />
        ))}
        {loading && (
          <div
            className="cf-skeletons"
            role="status"
            aria-label="Đang tải bài viết"
          >
            {[0, 1, 2].slice(0, page ? 1 : 3).map((i) => (
              <div key={i} className="cf-card cf-skeleton">
                <span />
                <span />
                <span />
              </div>
            ))}
          </div>
        )}
        {!loading && !error && !items.length && (
          <div className="cf-panel cf-empty">
            <Icon name={tab === "saved" ? "bookmark" : "message"} size={32} />
            <h2>
              {tab === "saved"
                ? "Bạn chưa lưu bài viết nào"
                : tab === "mine"
                  ? "Bạn chưa đăng bài nào"
                  : tab === "following"
                    ? "Chưa có bài viết từ người bạn theo dõi"
                    : hasFilter
                      ? "Không tìm thấy bài viết phù hợp"
                      : "Cộng đồng đang chờ câu hỏi đầu tiên"}
            </h2>
            <p>
              {tab === "following"
                ? "Bấm vào tên tác giả của bài viết hay để theo dõi, bảng tin sẽ phong phú hơn."
                : hasFilter
                  ? "Thử bỏ bớt bộ lọc hoặc dùng từ khóa khác."
                  : "Đặt câu hỏi hoặc chia sẻ điều bạn vừa học được."}
            </p>
            {tab === "following" ? (
              <a className="button secondary" href="#mine?tab=following">
                Người tôi theo dõi
              </a>
            ) : (
              !hasFilter && (
                <button
                  type="button"
                  className="button primary"
                  onClick={() => openEditor("QUESTION")}
                >
                  <Icon name="plus" size={16} />
                  Đăng bài đầu tiên
                </button>
              )
            )}
          </div>
        )}
        {!loading && page + 1 < totalPages && (
          <button
            type="button"
            className="button secondary cf-more"
            onClick={() => setPage((p) => p + 1)}
          >
            Xem thêm bài viết
            <Icon name="down" size={16} />
          </button>
        )}
      </main>

      <aside className="cf-right" aria-label="Gợi ý">
        {tab !== "unanswered" && (
          <WaitingQuestions
            category={filters.category}
            onSeeAll={() => set({ tab: "unanswered", type: "" })}
          />
        )}
        <RelatedDocuments
          category={filters.category}
          categoryName={activeCategory?.name}
        />
        <section className="cf-panel">
          <h2 className="cf-panel-title">
            <Icon name="trending" size={16} />
            Đang thịnh hành
          </h2>
          {sidebar.data?.trendingTags.length ? (
            <ul className="cf-trending">
              {sidebar.data.trendingTags.map((t, index) => (
                <li key={t.id}>
                  <button
                    type="button"
                    className={filters.tag === t.name ? "on" : ""}
                    onClick={() => set({ tag: t.name })}
                  >
                    <b>{index + 1}</b>
                    <span>#{t.name}</span>
                    <small>{t.count} bài</small>
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="cf-muted">Chưa có thẻ nào trong 30 ngày qua.</p>
          )}
        </section>
      </aside>

      {editor.open && (
        <PostEditor
          initial={editor.post}
          defaultType={editor.type}
          defaultCategory={filters.category ? Number(filters.category) : null}
          onClose={() => setEditor({ open: false })}
          onSaved={(saved) => {
            setEditor({ open: false });
            if (editor.post) {
              setNotice("Đã lưu thay đổi");
              refreshAll();
            } else location.hash = `post/${saved.post.id}`;
          }}
        />
      )}
    </div>
  );
}

/** Tài liệu trong thư viện liên quan tới chủ đề đang xem. */
function RelatedDocuments({
  category,
  categoryName,
}: {
  category: string;
  categoryName?: string;
}) {
  const docs = useRemote<PageData<DocumentItem>>(
    `/documents?size=4&sort=popular&global=true${category ? `&category=${category}` : ""}`,
  );
  return (
    <section className="cf-panel cf-related">
      <span className="cf-kicker">
        <Icon name="spark" size={13} />
        Gợi ý cho bạn
      </span>
      <h2 className="cf-panel-title plain">
        {categoryName
          ? `Tài liệu về ${categoryName}`
          : "Tài liệu được tải nhiều"}
      </h2>
      {docs.loading ? (
        <p className="cf-muted">Đang tải…</p>
      ) : docs.data?.content.length ? (
        <ul>
          {docs.data.content.map((doc) => (
            <li key={doc.id}>
              <a href={`#document/${doc.id}`}>
                <span className="cf-doc-icon">
                  <Icon name="book" size={17} />
                </span>
                <span>
                  <strong>{doc.title}</strong>
                  <small>
                    {doc.fileFormat || "Tài liệu"} · {doc.downloads} lượt tải
                  </small>
                </span>
              </a>
            </li>
          ))}
        </ul>
      ) : (
        <p className="cf-muted">Chưa có tài liệu cho chủ đề này.</p>
      )}
      <a
        className="cf-see-all"
        href={`#documents${category ? `?category=${category}&global=true` : ""}`}
      >
        Xem trong thư viện
        <Icon name="chevronRight" size={15} />
      </a>
    </section>
  );
}

/** Câu hỏi chưa ai trả lời — mời mọi người vào giúp, điểm khác biệt của cộng đồng hỏi đáp. */
function WaitingQuestions({
  category,
  onSeeAll,
}: {
  category: string;
  onSeeAll: () => void;
}) {
  const questions = useRemote<PostPage>(
    `/posts?tab=unanswered&size=4${category ? `&category=${category}` : ""}`,
  );
  const list = questions.data?.content || [];
  const total = questions.data?.totalElements || 0;
  return (
    <section className="cf-panel cf-waiting">
      <h2 className="cf-panel-title">
        <Icon name="question" size={16} />
        Đang chờ câu trả lời
        {total > 0 && <span className="cf-count">{total}</span>}
      </h2>
      {questions.loading ? (
        <p className="cf-muted">Đang tải…</p>
      ) : list.length ? (
        <ul>
          {list.map((post) => (
            <li key={post.id}>
              <a href={`#post/${post.id}`}>
                <strong>{post.title}</strong>
                <small>
                  {authorName(post.author)} · {timeAgo(post.publishedAt)}
                </small>
              </a>
            </li>
          ))}
        </ul>
      ) : (
        <p className="cf-muted">Mọi câu hỏi đều đã có người trả lời.</p>
      )}
      {total > list.length && (
        <button type="button" className="cf-see-all" onClick={onSeeAll}>
          Xem tất cả {total} câu hỏi
          <Icon name="chevronRight" size={15} />
        </button>
      )}
    </section>
  );
}
