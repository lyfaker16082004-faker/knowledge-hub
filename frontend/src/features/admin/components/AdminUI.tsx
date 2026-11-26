import { useEffect, useRef } from "react";
import type { ReactNode } from "react";
import Icon from "../../../shared/components/ui/Icon";
import "../../../styles/admin.css";

/*
 * Các thành phần giao diện dùng chung cho khu quản trị:
 * tiêu đề trang, thẻ, nút icon, trạng thái trống, thông báo, phân trang, hộp thoại, ảnh đại diện.
 */

/** Tiêu đề trang (kèm dòng mô tả và các nút bên phải). */
export function PageHeader({
  kicker,
  title,
  description,
  actions,
}: {
  kicker?: string;
  title: string;
  description?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <header className="adm-page-header">
      <div>
        {kicker && <span className="adm-kicker">{kicker}</span>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {actions && <div className="adm-page-actions">{actions}</div>}
    </header>
  );
}

/** Khung trắng bo góc chứa nội dung. */
export function Card({
  title,
  count,
  actions,
  children,
  className = "",
}: {
  title?: ReactNode;
  count?: number | string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={`adm-card ${className}`}>
      {(title || actions) && (
        <div className="adm-card-head">
          {title && (
            <h2>
              {title}
              {count !== undefined && <span className="adm-pill">{count}</span>}
            </h2>
          )}
          {actions && <div className="adm-card-actions">{actions}</div>}
        </div>
      )}
      {children}
    </section>
  );
}

/** Nút chỉ có icon, rê chuột vào sẽ hiện chú thích. Có href thì là link. */
export function IconAction({
  icon,
  label,
  onClick,
  tone,
  disabled,
  href,
}: {
  icon: string;
  label: string;
  onClick?: () => void;
  tone?: "danger" | "primary" | "success";
  disabled?: boolean;
  href?: string;
}) {
  const className = `adm-icon-action${tone ? ` ${tone}` : ""}`;
  if (href)
    return (
      <a className={className} href={href} aria-label={label} data-tip={label}>
        <Icon name={icon} size={17} />
      </a>
    );
  return (
    <button
      type="button"
      className={className}
      aria-label={label}
      data-tip={label}
      onClick={onClick}
      disabled={disabled}
    >
      <Icon name={icon} size={17} />
    </button>
  );
}

/** Thông báo khi danh sách trống hoặc có lỗi. */
export function EmptyState({
  icon = "search",
  title,
  text,
  action,
}: {
  icon?: string;
  title: string;
  text?: string;
  action?: ReactNode;
}) {
  return (
    <div className="adm-empty">
      <span aria-hidden="true">
        <Icon name={icon} size={24} />
      </span>
      <h3>{title}</h3>
      {text && <p>{text}</p>}
      {action}
    </div>
  );
}

/** Các dòng xám nhấp nháy trong lúc đang tải. */
export function LoadingRows({ rows = 5 }: { rows?: number }) {
  return (
    <div className="adm-skeleton" role="status" aria-label="Đang tải">
      {Array.from({ length: rows }, (_, i) => (
        <span key={i} />
      ))}
    </div>
  );
}

/** Thông báo thành công (xanh) hoặc lỗi (đỏ). */
export function Notice({
  tone = "success",
  children,
  onClose,
}: {
  tone?: "success" | "error";
  children: ReactNode;
  onClose?: () => void;
}) {
  return (
    <div
      className={`adm-notice ${tone}`}
      role={tone === "error" ? "alert" : "status"}
    >
      <Icon name={tone === "error" ? "close" : "check"} size={16} />
      <span>{children}</span>
      {onClose && (
        <button type="button" aria-label="Đóng thông báo" onClick={onClose}>
          <Icon name="close" size={14} />
        </button>
      )}
    </div>
  );
}

/** Auto-hides a success message after a few seconds. */
export function useAutoClear(value: string, clear: () => void, ms = 4000) {
  useEffect(() => {
    if (!value) return;
    const timer = setTimeout(clear, ms);
    return () => clearTimeout(timer);
  }, [value]); // eslint-disable-line react-hooks/exhaustive-deps
}

/** Thanh phân trang: tổng số, trang trước / trang sau. */
export function Pager({
  page,
  totalPages,
  total,
  unit,
  onPage,
  disabled,
}: {
  page: number;
  totalPages: number;
  total?: number;
  unit: string;
  onPage: (page: number) => void;
  disabled?: boolean;
}) {
  const pages = Math.max(1, totalPages);
  return (
    <div className="adm-pager">
      <span>
        {total !== undefined ? (
          <>
            <strong>{total.toLocaleString("vi-VN")}</strong> {unit}
          </>
        ) : (
          "—"
        )}
      </span>
      <div>
        <button
          type="button"
          className="adm-icon-action"
          aria-label="Trang trước"
          disabled={disabled || page === 0}
          onClick={() => onPage(page - 1)}
        >
          <Icon name="chevronLeft" size={16} />
        </button>
        <span>
          Trang <strong>{page + 1}</strong> / {pages}
        </span>
        <button
          type="button"
          className="adm-icon-action"
          aria-label="Trang sau"
          disabled={disabled || page + 1 >= pages}
          onClick={() => onPage(page + 1)}
        >
          <Icon name="chevronRight" size={16} />
        </button>
      </div>
    </div>
  );
}

/** Modal dialog with consistent header / body / footer. */
export function AdminDialog({
  title,
  subtitle,
  onClose,
  busy,
  wide,
  footer,
  children,
}: {
  title: string;
  subtitle?: ReactNode;
  onClose: () => void;
  busy?: boolean;
  wide?: boolean;
  footer?: ReactNode;
  children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    ref.current?.showModal();
  }, []);
  return (
    <dialog
      ref={ref}
      className={`adm-dialog${wide ? " wide" : ""}`}
      aria-labelledby="adm-dialog-title"
      onCancel={(e) => {
        e.preventDefault();
        if (!busy) onClose();
      }}
      onClick={(e) => {
        if (e.target === ref.current && !busy) onClose();
      }}
    >
      <div className="adm-dialog-head">
        <div>
          <h2 id="adm-dialog-title">{title}</h2>
          {subtitle && <p>{subtitle}</p>}
        </div>
        <button
          type="button"
          className="adm-icon-action"
          aria-label="Đóng"
          disabled={busy}
          onClick={onClose}
        >
          <Icon name="close" size={18} />
        </button>
      </div>
      <div className="adm-dialog-body">{children}</div>
      {footer && <div className="adm-dialog-foot">{footer}</div>}
    </dialog>
  );
}

/** Lấy 2 chữ cái đầu của tên, viết hoa. */
export function initials(name: string) {
  return name.slice(0, 2).toUpperCase();
}
/** Ảnh đại diện nhỏ, chưa có ảnh thì hiện chữ cái đầu. */
export function Avatar({
  name,
  url,
  size = "md",
}: {
  name: string;
  url?: string | null;
  size?: "md" | "lg";
}) {
  return (
    <span className={`adm-avatar ${size}`} aria-hidden="true">
      {url ? <img src={url} alt="" /> : initials(name)}
    </span>
  );
}
/** Ngày dạng dd/mm/yyyy. */
export function shortDate(value: string | null) {
  if (!value) return "—";
  return new Date(value + (/[Z+]/.test(value) ? "" : "Z")).toLocaleDateString(
    "vi-VN",
    { day: "2-digit", month: "2-digit", year: "numeric" },
  );
}
