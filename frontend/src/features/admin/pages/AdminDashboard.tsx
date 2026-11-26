import type { Account } from "../../auth/types";
import Icon from "../../../shared/components/ui/Icon";
import { useRemote } from "../../../shared/hooks/useRemote";
import type {
  CatalogueStats,
  ManagedDocument,
  PageData,
} from "../../documents/types";
import { coverFor } from "../../documents/components/LibraryUI";
import {
  Card,
  EmptyState,
  LoadingRows,
  Notice,
  PageHeader,
  shortDate,
} from "../components/AdminUI";

/*
 * Trang Tổng quan của quản trị viên (#admin):
 * số liệu hệ thống, tài liệu chờ duyệt và tình trạng kho tài liệu.
 */

// Các thanh trạng thái của kho tài liệu
const statusBars = [
  { key: "PUBLISHED", label: "Đã công bố", tone: "green" },
  { key: "PENDING", label: "Chờ duyệt", tone: "amber" },
  { key: "REJECTED", label: "Bị từ chối", tone: "red" },
  { key: "ARCHIVED", label: "Đã ẩn", tone: "slate" },
];

/** Đếm số tài liệu theo trạng thái (chỉ lấy 1 phần tử để đọc tổng số). */
function useStatusCount(status: string) {
  return useRemote<PageData<ManagedDocument>>(
    `/admin/documents?status=${status}&size=1`,
  );
}

export default function AdminDashboard({ user }: { user: Account }) {
  const users = useRemote<{ totalElements: number }>("/admin/users?size=1");
  const documents = useRemote<PageData<ManagedDocument>>(
    "/admin/documents?size=1",
  );
  const pending = useRemote<PageData<ManagedDocument>>(
    "/admin/documents?status=PENDING&size=5",
  );
  const stats = useRemote<CatalogueStats>("/documents/stats");
  const counts = {
    PUBLISHED: useStatusCount("PUBLISHED"),
    PENDING: pending,
    REJECTED: useStatusCount("REJECTED"),
    ARCHIVED: useStatusCount("ARCHIVED"),
  } as Record<string, { data: PageData<ManagedDocument> | null }>;
  const failed = [users, documents, pending, stats].some(
    (resource) => resource.error,
  );
  const totalForBars = statusBars.reduce(
    (sum, s) => sum + (counts[s.key].data?.totalElements || 0),
    0,
  );
  const pendingTotal = pending.data?.totalElements || 0;

  const metrics = [
    {
      label: "Người dùng",
      value: users.data?.totalElements,
      icon: "users",
      tone: "violet",
      href: "#admin/users",
      hint: "Tài khoản trong hệ thống",
    },
    {
      label: "Tài liệu",
      value: documents.data?.totalElements,
      icon: "book",
      tone: "blue",
      href: "#admin/documents",
      hint: "Mọi trạng thái",
    },
    {
      label: "Lượt tải",
      value: stats.data?.downloads,
      icon: "download",
      tone: "green",
      href: "#admin/documents",
      hint: "Tổng lượt tải về",
    },
    {
      label: "Chờ duyệt",
      value: pending.data?.totalElements,
      icon: "clock",
      tone: "amber",
      href: "#admin/documents?status=PENDING",
      hint: pendingTotal ? "Cần bạn xem xét" : "Đã xử lý hết",
    },
  ];
  const today = new Date().toLocaleDateString("vi-VN", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });

  return (
    <div className="adm-dashboard">
      <PageHeader
        kicker={today}
        title={`Xin chào, ${user.displayName || user.username}`}
        description="Tình hình hệ thống hôm nay và những việc cần xử lý."
        actions={
          <>
            <a className="button secondary" href="#admin/users">
              <Icon name="users" size={16} />
              Người dùng
            </a>
            <a className="button primary" href="#admin/documents/new">
              <Icon name="plus" size={16} />
              Thêm giáo trình
            </a>
          </>
        }
      />

      {failed && (
        <Notice tone="error">
          Một số thống kê chưa tải được.{" "}
          <button
            type="button"
            className="text-button"
            onClick={() => {
              users.refresh();
              documents.refresh();
              pending.refresh();
              stats.refresh();
            }}
          >
            Thử lại
          </button>
        </Notice>
      )}

      <div className="adm-kpis" aria-label="Thống kê hệ thống">
        {metrics.map((m) => (
          <a className={`adm-kpi k-${m.tone}`} href={m.href} key={m.label}>
            <span className="adm-kpi-icon">
              <Icon name={m.icon} size={20} />
            </span>
            <span className="adm-kpi-body">
              <small>{m.label}</small>
              <strong>
                {m.value === undefined ? "—" : m.value.toLocaleString("vi-VN")}
              </strong>
              <em>{m.hint}</em>
            </span>
            <Icon name="chevronRight" size={16} />
          </a>
        ))}
      </div>

      <div className="adm-dash-grid">
        <Card
          title="Tài liệu chờ duyệt"
          count={pendingTotal}
          className="adm-pending"
          actions={
            <a
              className="button secondary compact"
              href="#admin/documents?status=PENDING"
            >
              Xem tất cả
              <Icon name="chevronRight" size={14} />
            </a>
          }
        >
          {pending.loading ? (
            <LoadingRows rows={4} />
          ) : pending.error ? (
            <EmptyState
              icon="refresh"
              title="Chưa tải được"
              text={pending.error}
            />
          ) : !pending.data?.content.length ? (
            <EmptyState
              icon="check"
              title="Không còn tài liệu chờ duyệt"
              text="Bạn đã xử lý hết. Tài liệu mới gửi lên sẽ hiện ở đây."
            />
          ) : (
            <ul className="adm-doc-mini">
              {pending.data.content.map((item) => (
                <li key={item.document.id}>
                  <img
                    src={`/covers/${coverFor(item.document)}`}
                    alt=""
                    loading="lazy"
                  />
                  <div>
                    <strong>{item.document.title}</strong>
                    <small>
                      {item.author} · {item.document.category} ·{" "}
                      {shortDate(item.createdAt)}
                    </small>
                  </div>
                  <a
                    className="button secondary compact"
                    href="#admin/documents?status=PENDING"
                  >
                    Xét duyệt
                  </a>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <div className="adm-dash-side">
          <Card title="Tình trạng kho tài liệu">
            <div
              className="adm-stack"
              role="img"
              aria-label={statusBars
                .map(
                  (s) =>
                    `${s.label}: ${counts[s.key].data?.totalElements ?? 0}`,
                )
                .join(", ")}
            >
              {statusBars.map((s) => {
                const n = counts[s.key].data?.totalElements || 0;
                return n ? (
                  <span
                    key={s.key}
                    className={`k-${s.tone}`}
                    style={{ flexGrow: n }}
                  />
                ) : null;
              })}
              {!totalForBars && <span className="empty" />}
            </div>
            <ul className="adm-legend">
              {statusBars.map((s) => (
                <li key={s.key}>
                  <a href={`#admin/documents?status=${s.key}`}>
                    <i className={`k-${s.tone}`} />
                    {s.label}
                    <b>{counts[s.key].data?.totalElements ?? "—"}</b>
                  </a>
                </li>
              ))}
            </ul>
          </Card>
          <Card title="Lối tắt">
            <div className="adm-shortcuts">
              <a href="#admin/users">
                <Icon name="users" size={17} />
                <span>
                  <strong>Quản lý tài khoản</strong>
                  <small>Tạo, sửa và phân quyền thành viên</small>
                </span>
              </a>
              <a href="#admin/organizations">
                <Icon name="institution" size={17} />
                <span>
                  <strong>Trường / Khoa</strong>
                  <small>Cập nhật danh mục tổ chức</small>
                </span>
              </a>
              <a href="#admin/documents/new">
                <Icon name="upload" size={17} />
                <span>
                  <strong>Thêm giáo trình</strong>
                  <small>Đăng tài liệu chính thức</small>
                </span>
              </a>
            </div>
          </Card>
        </div>
      </div>
      <p className="adm-footnote">
        <Icon name="lock" size={14} />
        Khu vực này chỉ dành cho tài khoản có quyền quản trị viên.
      </p>
    </div>
  );
}
