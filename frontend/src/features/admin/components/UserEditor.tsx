import { useState } from "react";
import type { FormEvent } from "react";
import { mutate, errorMessage } from "../../../shared/api/http-client";
import type { Account } from "../../auth/types";
import { roleNames, statusNames } from "../../auth/types";
import PasswordInput from "../../../shared/components/ui/PasswordInput";
import Icon from "../../../shared/components/ui/Icon";
import { AdminDialog, Notice } from "./AdminUI";

/* Hộp thoại tạo tài khoản, sửa tài khoản hoặc đặt lại mật khẩu. */

/** Hộp thoại đang mở ở chế độ nào và cho tài khoản nào. */
export interface EditorState {
  mode: "create" | "edit" | "password";
  user?: Account;
}
const roleHints: Record<string, string> = {
  ADMIN: "Quản lý tài khoản và phân quyền",
  MODERATOR: "Kiểm duyệt nội dung",
  USER: "Tài khoản thành viên",
};

export default function UserEditor({
  state,
  onClose,
  onSaved,
}: {
  state: EditorState;
  onClose: () => void;
  onSaved: (message: string) => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const title =
    state.mode === "create"
      ? "Tạo tài khoản mới"
      : state.mode === "edit"
        ? "Chỉnh sửa tài khoản"
        : "Đặt lại mật khẩu";
  /** Gửi form tương ứng với chế độ đang mở (tạo / sửa / đổi mật khẩu). */
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const roles = form.getAll("roles");
    if (state.mode !== "password" && !roles.length) {
      setError("Hãy chọn ít nhất một vai trò.");
      return;
    }
    if (state.mode !== "edit" && form.get("password") !== form.get("confirm")) {
      setError("Mật khẩu xác nhận chưa khớp.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      if (state.mode === "create")
        await mutate("/admin/users", "POST", {
          username: form.get("username"),
          email: form.get("email"),
          password: form.get("password"),
          roles,
        });
      else if (state.mode === "edit")
        await mutate(`/admin/users/${state.user!.id}`, "PUT", {
          email: form.get("email"),
          status: form.get("status"),
          roles,
        });
      else
        await mutate(`/admin/users/${state.user!.id}/password`, "POST", {
          password: form.get("password"),
        });
      await onSaved(
        state.mode === "password"
          ? "Đã đặt lại mật khẩu. Các phiên đăng nhập cũ đã bị thu hồi."
          : state.mode === "create"
            ? "Đã tạo tài khoản mới."
            : "Đã lưu tài khoản.",
      );
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  const footer = (
    <>
      <button
        className="button secondary"
        type="button"
        disabled={busy}
        onClick={onClose}
      >
        Hủy
      </button>
      <button
        className="button primary"
        type="submit"
        form="adm-user-form"
        disabled={busy}
      >
        <Icon name="check" size={15} />
        {busy
          ? "Đang lưu…"
          : state.mode === "create"
            ? "Tạo tài khoản"
            : "Lưu thay đổi"}
      </button>
    </>
  );
  return (
    <AdminDialog
      title={title}
      subtitle={
        state.user
          ? `@${state.user.username}`
          : "Tài khoản mới có thể đăng nhập ngay sau khi tạo."
      }
      onClose={onClose}
      busy={busy}
      footer={footer}
    >
      {error && <Notice tone="error">{error}</Notice>}
      <form id="adm-user-form" className="adm-form" onSubmit={submit}>
        <fieldset disabled={busy}>
          {state.mode === "create" && (
            <label className="field">
              Tên đăng nhập
              <input
                autoFocus
                name="username"
                autoComplete="off"
                required
                minLength={3}
                maxLength={50}
                pattern="[a-zA-Z0-9_.-]{3,50}"
                placeholder="nguyenvan.a"
              />
              <small>
                3–50 ký tự: chữ, số, dấu chấm, gạch dưới, gạch ngang.
              </small>
            </label>
          )}
          {state.mode !== "password" && (
            <label className="field">
              Địa chỉ email
              <input
                name="email"
                type="email"
                required
                maxLength={254}
                defaultValue={state.user?.email}
                placeholder="ban@example.com"
              />
            </label>
          )}
          {state.mode === "edit" && (
            <label className="field">
              Trạng thái
              <select name="status" defaultValue={state.user?.status}>
                {Object.entries(statusNames).map(([key, value]) => (
                  <option key={key} value={key}>
                    {value}
                  </option>
                ))}
              </select>
              <small>
                Đổi trạng thái hoặc vai trò sẽ yêu cầu người dùng đăng nhập lại.
              </small>
            </label>
          )}
          {state.mode !== "edit" && (
            <div className="adm-form-row">
              <PasswordInput
                label="Mật khẩu mới"
                name="password"
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                hint="Tối thiểu 8 ký tự."
              />
              <PasswordInput
                label="Xác nhận mật khẩu"
                name="confirm"
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
              />
            </div>
          )}
          {state.mode !== "password" && (
            <fieldset className="adm-roles">
              <legend>Vai trò</legend>
              {Object.entries(roleNames).map(([key, label]) => (
                <label className="adm-check" key={key}>
                  <input
                    type="checkbox"
                    name="roles"
                    value={key}
                    defaultChecked={
                      state.user
                        ? state.user.roles.includes(key)
                        : key === "USER"
                    }
                  />
                  <span>
                    <strong>{label}</strong>
                    <small>{roleHints[key]}</small>
                  </span>
                </label>
              ))}
            </fieldset>
          )}
        </fieldset>
      </form>
    </AdminDialog>
  );
}
