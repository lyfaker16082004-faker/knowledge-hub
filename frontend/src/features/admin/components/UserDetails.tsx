import { useEffect, useState } from "react";
import { get, mutate, errorMessage } from "../../../shared/api/http-client";
import type { Account } from "../../auth/types";
import { dateLabel, roleNames, statusNames } from "../../auth/types";
import Icon from "../../../shared/components/ui/Icon";
import { AdminDialog, Avatar, LoadingRows, Notice } from "./AdminUI";

/* Hộp thoại xem chi tiết tài khoản, cũng dùng để xác nhận xoá mềm. */

export default function UserDetails({
  id,
  mode,
  onClose,
  onSaved,
}: {
  id: number;
  mode: "detail" | "delete";
  onClose: () => void;
  onSaved: (message: string) => Promise<void>;
}) {
  const [user, setUser] = useState<Account | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    get<Account>(`/admin/users/${id}`, controller.signal)
      .then(setUser)
      .catch((e) => {
        if (!controller.signal.aborted) setError(errorMessage(e));
      });
    return () => controller.abort();
  }, [id]);
  /** Xoá mềm tài khoản (giữ dữ liệu, chỉ đổi trạng thái thành DELETED). */
  async function remove() {
    setBusy(true);
    setError("");
    try {
      await mutate(`/admin/users/${id}`, "DELETE");
      await onSaved(
        "Đã xóa mềm tài khoản. Người dùng không thể đăng nhập; dữ liệu được giữ lại.",
      );
      onClose();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  const footer = (
    <>
      <button className="button secondary" disabled={busy} onClick={onClose}>
        {mode === "delete" ? "Hủy" : "Đóng"}
      </button>
      {mode === "delete" && (
        <button
          className="button danger"
          disabled={busy || !user || user.status === "DELETED"}
          onClick={remove}
        >
          <Icon name="trash" size={15} />
          {busy ? "Đang xử lý…" : "Xác nhận xóa mềm"}
        </button>
      )}
    </>
  );
  return (
    <AdminDialog
      title={mode === "delete" ? "Xóa mềm tài khoản" : "Chi tiết người dùng"}
      onClose={onClose}
      busy={busy}
      footer={footer}
    >
      {error && <Notice tone="error">{error}</Notice>}
      {!user ? (
        !error && <LoadingRows rows={4} />
      ) : (
        <>
          <div className="adm-identity">
            <Avatar name={user.username} url={user.avatarUrl} size="lg" />
            <div>
              <h3>{user.displayName || user.username}</h3>
              <p>
                @{user.username} · ID #{user.id}
              </p>
              <div className="adm-badges">
                <span className={`adm-status ${user.status.toLowerCase()}`}>
                  {statusNames[user.status]}
                </span>
                {user.roles.map((r) => (
                  <span key={r} className={`adm-role ${r.toLowerCase()}`}>
                    {roleNames[r] || r}
                  </span>
                ))}
              </div>
            </div>
          </div>
          {mode === "detail" ? (
            <>
              <dl className="adm-dl">
                <div>
                  <dt>Email</dt>
                  <dd>{user.email}</dd>
                </div>
                <div>
                  <dt>Trường / Tổ chức</dt>
                  <dd>{user.organization?.institutionName || "Chưa chọn"}</dd>
                </div>
                <div>
                  <dt>Khoa / Chuyên ngành</dt>
                  <dd>{user.organization?.facultyName || "Chưa chọn"}</dd>
                </div>
                <div>
                  <dt>Ngày tham gia</dt>
                  <dd>{dateLabel(user.createdAt)}</dd>
                </div>
                <div>
                  <dt>Đăng nhập gần nhất</dt>
                  <dd>{dateLabel(user.lastLoginAt)}</dd>
                </div>
              </dl>
              <h4 className="adm-subtitle">Giới thiệu</h4>
              <p className="adm-bio preserve-lines">
                {user.bio || "Chưa có giới thiệu."}
              </p>
            </>
          ) : (
            <div className="adm-warning">
              <Icon name="shield" size={18} />
              <p>
                Tài khoản <strong>{user.username}</strong> sẽ chuyển sang{" "}
                <strong>Đã xóa mềm</strong> và bị thu hồi đăng nhập. Hồ sơ, tài
                liệu và lịch sử được giữ lại — có thể khôi phục bằng cách sửa
                trạng thái thành Hoạt động.
              </p>
            </div>
          )}
        </>
      )}
    </AdminDialog>
  );
}
