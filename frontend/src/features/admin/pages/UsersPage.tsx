import { useEffect, useState } from "react";
import { get, errorMessage } from "../../../shared/api/http-client";
import type { AccountPage } from "../../auth/types";
import { roleNames, statusNames } from "../../auth/types";
import Icon from "../../../shared/components/ui/Icon";
import UserDetails from "../components/UserDetails";
import UserEditor from "../components/UserEditor";
import type { EditorState } from "../components/UserEditor";
import {
  Avatar,
  Card,
  EmptyState,
  IconAction,
  LoadingRows,
  Notice,
  PageHeader,
  Pager,
  shortDate,
  useAutoClear,
} from "../components/AdminUI";

/*
 * Trang quản lý Người dùng (#admin/users).
 * Tìm kiếm, tạo, sửa, đổi vai trò, đặt lại mật khẩu và xoá mềm tài khoản.
 */

export default function UsersPage({
  recheck,
}: {
  recheck: () => Promise<void>;
}) {
  const [inspection, setInspection] = useState<{
    id: number;
    mode: "detail" | "delete";
  } | null>(null);
  const [data, setData] = useState<AccountPage | null>(null);
  const [query, setQuery] = useState("");
  const [draft, setDraft] = useState("");
  const [page, setPage] = useState(0);
  const [reload, setReload] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [editor, setEditor] = useState<EditorState | null>(null);
  useAutoClear(notice, () => setNotice(""));

  // search as you type (debounced)
  useEffect(() => {
    const timer = setTimeout(() => {
      if (draft.trim() !== query) {
        setQuery(draft.trim());
        setPage(0);
      }
    }, 350);
    return () => clearTimeout(timer);
  }, [draft, query]);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    get<AccountPage>(
      `/admin/users?q=${encodeURIComponent(query)}&page=${page}&size=10`,
      controller.signal,
    )
      .then((result) => {
        setData(result);
        if (result.totalPages && page >= result.totalPages)
          setPage(result.totalPages - 1);
      })
      .catch((e) => {
        if (!controller.signal.aborted) setError(errorMessage(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [query, page, reload]);

  /** Gọi sau khi lưu xong: đóng hộp thoại, hiện thông báo và tải lại danh sách. */
  async function saved(message: string) {
    setEditor(null);
    setNotice(message);
    setReload((n) => n + 1);
    await recheck();
  }

  return (
    <>
      <PageHeader
        kicker="QUẢN LÝ"
        title="Người dùng"
        description="Quản lý thành viên, trạng thái và vai trò. Tài khoản tự đăng ký mặc định là Thành viên."
        actions={
          <button
            className="button primary"
            onClick={() => setEditor({ mode: "create" })}
          >
            <Icon name="plus" size={16} />
            Tạo tài khoản
          </button>
        }
      />
      {notice && <Notice onClose={() => setNotice("")}>{notice}</Notice>}
      <Card
        title="Danh sách tài khoản"
        count={data?.totalElements ?? "…"}
        actions={
          <label className="adm-search">
            <Icon name="search" size={16} />
            <input
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              maxLength={100}
              placeholder="Tìm tên đăng nhập, email…"
              aria-label="Tìm tài khoản"
            />
            {draft && (
              <button
                type="button"
                aria-label="Xóa tìm kiếm"
                onClick={() => setDraft("")}
              >
                <Icon name="close" size={14} />
              </button>
            )}
          </label>
        }
      >
        {error ? (
          <EmptyState
            icon="refresh"
            title="Chưa tải được danh sách"
            text={error}
            action={
              <button
                className="button secondary compact"
                onClick={() => setReload((n) => n + 1)}
              >
                Thử lại
              </button>
            }
          />
        ) : loading && !data ? (
          <LoadingRows />
        ) : !data?.content.length ? (
          <EmptyState
            icon={query ? "search" : "users"}
            title={query ? "Không tìm thấy tài khoản" : "Chưa có tài khoản"}
            text={
              query
                ? `Không có kết quả cho “${query}”. Thử tên đăng nhập hoặc email khác.`
                : "Tạo tài khoản đầu tiên để bắt đầu."
            }
          />
        ) : (
          <div className={`adm-table-wrap${loading ? " is-loading" : ""}`}>
            <table className="adm-table">
              <caption className="sr-only">
                Danh sách tài khoản người dùng
              </caption>
              <thead>
                <tr>
                  <th>Thành viên</th>
                  <th>Vai trò</th>
                  <th>Trạng thái</th>
                  <th>Ngày tham gia</th>
                  <th className="end">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((user) => (
                  <tr key={user.id}>
                    <td data-label="Thành viên">
                      <button
                        type="button"
                        className="adm-person"
                        onClick={() =>
                          setInspection({ id: user.id, mode: "detail" })
                        }
                      >
                        <Avatar name={user.username} url={user.avatarUrl} />
                        <span>
                          <strong>{user.displayName || user.username}</strong>
                          <small>{user.email}</small>
                        </span>
                      </button>
                    </td>
                    <td data-label="Vai trò">
                      <div className="adm-badges">
                        {user.roles.map((role) => (
                          <span
                            className={`adm-role ${role.toLowerCase()}`}
                            key={role}
                          >
                            {roleNames[role] || role}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td data-label="Trạng thái">
                      <span
                        className={`adm-status ${user.status.toLowerCase()}`}
                      >
                        {statusNames[user.status]}
                      </span>
                    </td>
                    <td data-label="Ngày tham gia" className="adm-muted">
                      {shortDate(user.createdAt)}
                    </td>
                    <td className="end">
                      <div className="adm-row-actions">
                        <IconAction
                          icon="eye"
                          label="Xem chi tiết"
                          onClick={() =>
                            setInspection({ id: user.id, mode: "detail" })
                          }
                        />
                        <IconAction
                          icon="edit"
                          label="Sửa tài khoản"
                          onClick={() => setEditor({ mode: "edit", user })}
                        />
                        <IconAction
                          icon="lock"
                          label="Đặt lại mật khẩu"
                          onClick={() => setEditor({ mode: "password", user })}
                        />
                        <IconAction
                          icon="trash"
                          label={
                            user.status === "DELETED" ? "Đã xóa mềm" : "Xóa mềm"
                          }
                          tone="danger"
                          disabled={user.status === "DELETED"}
                          onClick={() =>
                            setInspection({ id: user.id, mode: "delete" })
                          }
                        />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {data && data.totalElements > 0 && (
          <Pager
            page={page}
            totalPages={data.totalPages}
            total={data.totalElements}
            unit={query ? `tài khoản khớp “${query}”` : "tài khoản"}
            disabled={loading}
            onPage={setPage}
          />
        )}
      </Card>
      <p className="adm-footnote">
        <Icon name="shield" size={14} />
        Hệ thống luôn giữ ít nhất một quản trị viên hoạt động.
      </p>
      {inspection && (
        <UserDetails
          id={inspection.id}
          mode={inspection.mode}
          onClose={() => setInspection(null)}
          onSaved={saved}
        />
      )}
      {editor && (
        <UserEditor
          state={editor}
          onClose={() => setEditor(null)}
          onSaved={saved}
        />
      )}
    </>
  );
}
