import { useMemo, useState } from "react";
import type { FormEvent } from "react";
import { useRemote } from "../../../shared/hooks/useRemote";
import { mutate, errorMessage } from "../../../shared/api/http-client";
import Icon from "../../../shared/components/ui/Icon";
import {
  AdminDialog,
  Card,
  EmptyState,
  IconAction,
  LoadingRows,
  Notice,
  PageHeader,
  useAutoClear,
} from "../components/AdminUI";

/*
 * Trang quản lý Trường / Khoa (#admin/organizations) - US1.5.
 * Thêm, sửa, xoá trường và các khoa trực thuộc.
 */

// Một trường kèm số khoa
type School = { id: number; name: string; type: string; facultyCount: number };
// Một khoa thuộc trường
type Faculty = { id: number; institutionId: number; name: string };
// Thông tin của hộp thoại thêm / sửa đang mở
type Editor = {
  kind: "school" | "faculty";
  id?: number;
  name: string;
  type: string;
};
const types: Record<string, string> = {
  UNIVERSITY: "Trường đại học",
  COMPANY: "Công ty / Tổ chức",
  OTHER: "Khác",
};
const shortTypes: Record<string, string> = {
  UNIVERSITY: "Trường",
  COMPANY: "Tổ chức",
  OTHER: "Khác",
};
const typeIcons: Record<string, string> = {
  UNIVERSITY: "institution",
  COMPANY: "grid",
  OTHER: "users",
};

export default function OrganizationsPage() {
  const schools = useRemote<School[]>("/admin/institutions");
  const [selected, setSelected] = useState<number | null>(null);
  const faculties = useRemote<Faculty[]>(
    selected ? `/admin/institutions/${selected}/faculties` : null,
  );
  const [editor, setEditor] = useState<Editor | null>(null);
  const [deleting, setDeleting] = useState<{
    kind: string;
    id: number;
    name: string;
  } | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [filter, setFilter] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  useAutoClear(notice, () => setNotice(""));
  const school = schools.data?.find((s) => s.id === selected);
  const visible = useMemo(() => {
    const needle = filter.trim().toLocaleLowerCase("vi");
    return (schools.data || []).filter(
      (s) =>
        (!typeFilter || s.type === typeFilter) &&
        (!needle || s.name.toLocaleLowerCase("vi").includes(needle)),
    );
  }, [schools.data, filter, typeFilter]);

  /** Mở hộp thoại thêm / sửa. */
  function edit(value: Editor) {
    setError("");
    setEditor(value);
  }
  /** Mở hộp thoại xác nhận xoá. */
  function remove(kind: string, id: number, name: string) {
    setError("");
    setDeleting({ kind, id, name });
  }
  /** Đóng hộp thoại. */
  function close() {
    if (busy) return;
    setEditor(null);
    setDeleting(null);
    setError("");
  }
  /** Lưu trường / khoa (thêm mới hoặc cập nhật). */
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editor) return;
    setBusy(true);
    setError("");
    const form = new FormData(event.currentTarget);
    try {
      const path = editor.kind === "school" ? "institutions" : "faculties";
      await mutate(
        `/admin/${path}${editor.id ? "/" + editor.id : ""}`,
        editor.id ? "PUT" : "POST",
        editor.kind === "school"
          ? { name: String(form.get("name")).trim(), type: form.get("type") }
          : {
              name: String(form.get("name")).trim(),
              institutionId: Number(form.get("institutionId")),
            },
      );
      setEditor(null);
      schools.refresh();
      faculties.refresh();
      setNotice(editor.id ? "Đã lưu thay đổi." : "Đã thêm mới.");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  /** Xoá trường / khoa sau khi đã xác nhận. */
  async function confirmDelete() {
    if (!deleting) return;
    setBusy(true);
    setError("");
    try {
      await mutate(
        `/admin/${deleting.kind === "school" ? "institutions" : "faculties"}/${deleting.id}`,
        "DELETE",
      );
      if (deleting.kind === "school" && deleting.id === selected)
        setSelected(null);
      setDeleting(null);
      schools.refresh();
      faculties.refresh();
      setNotice("Đã xóa thành công.");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  const typeCounts = (schools.data || []).reduce<Record<string, number>>(
    (acc, s) => {
      acc[s.type] = (acc[s.type] || 0) + 1;
      return acc;
    },
    {},
  );

  return (
    <section>
      <PageHeader
        kicker="QUẢN LÝ"
        title="Trường và chuyên ngành"
        description="Danh mục tổ chức dùng trong hồ sơ thành viên và bộ lọc thư viện."
        actions={
          <button
            className="button primary"
            onClick={() =>
              edit({ kind: "school", name: "", type: "UNIVERSITY" })
            }
          >
            <Icon name="plus" size={16} />
            Thêm trường / tổ chức
          </button>
        }
      />
      {notice && <Notice onClose={() => setNotice("")}>{notice}</Notice>}
      {schools.error && (
        <Notice tone="error">
          {schools.error}{" "}
          <button className="text-button" onClick={schools.refresh}>
            Thử lại
          </button>
        </Notice>
      )}

      <div className="adm-master-detail">
        <Card
          title="Trường / Tổ chức"
          count={schools.data?.length ?? "…"}
          className="adm-master"
        >
          <div className="adm-master-tools">
            <label className="adm-search">
              <Icon name="search" size={16} />
              <input
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                placeholder="Lọc theo tên…"
                aria-label="Lọc trường"
              />
            </label>
            <div
              className="adm-tabs small"
              role="tablist"
              aria-label="Loại tổ chức"
            >
              <button
                role="tab"
                aria-selected={!typeFilter}
                onClick={() => setTypeFilter("")}
              >
                Tất cả
              </button>
              {Object.entries(shortTypes).map(([key, label]) => (
                <button
                  key={key}
                  role="tab"
                  aria-selected={typeFilter === key}
                  onClick={() => setTypeFilter(key)}
                >
                  {label}
                  <span>{typeCounts[key] || 0}</span>
                </button>
              ))}
            </div>
          </div>
          {schools.loading ? (
            <LoadingRows />
          ) : !visible.length ? (
            <EmptyState
              icon="institution"
              title={
                schools.data?.length ? "Không có kết quả" : "Chưa có trường nào"
              }
              text={
                schools.data?.length
                  ? "Thử từ khóa hoặc loại khác."
                  : "Thêm trường / tổ chức đầu tiên."
              }
            />
          ) : (
            <ul className="adm-list" role="listbox" aria-label="Chọn trường">
              {visible.map((s) => (
                <li key={s.id}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={selected === s.id}
                    className="adm-list-item"
                    onClick={() => setSelected(s.id)}
                  >
                    <span className={`adm-list-icon t-${s.type.toLowerCase()}`}>
                      <Icon
                        name={typeIcons[s.type] || "institution"}
                        size={17}
                      />
                    </span>
                    <span>
                      <strong>{s.name}</strong>
                      <small>
                        {types[s.type]}
                        {s.type !== "OTHER" && ` · ${s.facultyCount} khoa`}
                      </small>
                    </span>
                    <Icon name="chevronRight" size={15} />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card
          className="adm-detail"
          title={school ? school.name : "Khoa / Chuyên ngành"}
          actions={
            school && (
              <>
                <IconAction
                  icon="edit"
                  label="Sửa trường"
                  onClick={() =>
                    edit({
                      kind: "school",
                      id: school.id,
                      name: school.name,
                      type: school.type,
                    })
                  }
                />
                <IconAction
                  icon="trash"
                  label="Xóa trường"
                  tone="danger"
                  onClick={() => remove("school", school.id, school.name)}
                />
              </>
            )
          }
        >
          {!school ? (
            <EmptyState
              icon="chevronLeft"
              title="Chọn một trường"
              text="Chọn trường / tổ chức ở danh sách bên trái để quản lý các khoa trực thuộc."
            />
          ) : (
            <>
              <div className="adm-detail-meta">
                <span className={`adm-chip t-${school.type.toLowerCase()}`}>
                  {types[school.type]}
                </span>
                {school.type !== "OTHER" && (
                  <span className="adm-muted">
                    {school.facultyCount} khoa / chuyên ngành
                  </span>
                )}
              </div>
              {school.type === "OTHER" ? (
                <EmptyState
                  icon="users"
                  title="Không cần khoa"
                  text="Tổ chức loại Khác (người đi làm / tự do) không có khoa trực thuộc."
                />
              ) : (
                <>
                  <div className="adm-detail-head">
                    <h3>Khoa / Chuyên ngành</h3>
                    <button
                      className="button secondary compact"
                      onClick={() =>
                        edit({ kind: "faculty", name: "", type: "" })
                      }
                    >
                      <Icon name="plus" size={14} />
                      Thêm khoa
                    </button>
                  </div>
                  {faculties.loading ? (
                    <LoadingRows rows={3} />
                  ) : faculties.error ? (
                    <Notice tone="error">{faculties.error}</Notice>
                  ) : faculties.data?.length ? (
                    <ul className="adm-rows">
                      {faculties.data.map((f) => (
                        <li key={f.id}>
                          <span>
                            <Icon name="book" size={15} />
                            {f.name}
                          </span>
                          <div className="adm-row-actions">
                            <IconAction
                              icon="edit"
                              label={`Sửa ${f.name}`}
                              onClick={() =>
                                edit({
                                  kind: "faculty",
                                  id: f.id,
                                  name: f.name,
                                  type: "",
                                })
                              }
                            />
                            <IconAction
                              icon="trash"
                              label={`Xóa ${f.name}`}
                              tone="danger"
                              onClick={() => remove("faculty", f.id, f.name)}
                            />
                          </div>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <EmptyState
                      icon="plus"
                      title="Chưa có khoa"
                      text="Thêm khoa đầu tiên cho trường này."
                    />
                  )}
                </>
              )}
            </>
          )}
        </Card>
      </div>
      <p className="adm-footnote">
        <Icon name="lock" size={14} />
        Danh mục đang được hồ sơ, tài liệu hoặc bài viết sử dụng sẽ được giữ lại
        để bảo toàn dữ liệu.
      </p>

      {deleting && (
        <AdminDialog
          title="Xóa danh mục"
          onClose={close}
          busy={busy}
          footer={
            <>
              <button
                className="button secondary"
                disabled={busy}
                onClick={close}
              >
                Hủy
              </button>
              <button
                className="button danger"
                disabled={busy}
                onClick={confirmDelete}
              >
                <Icon name="trash" size={15} />
                {busy ? "Đang xóa…" : "Xác nhận xóa"}
              </button>
            </>
          }
        >
          {error && <Notice tone="error">{error}</Notice>}
          <div className="adm-warning">
            <Icon name="trash" size={18} />
            <p>
              Bạn muốn xóa <strong>“{deleting.name}”</strong>? Thao tác này
              không thể hoàn tác.
            </p>
          </div>
        </AdminDialog>
      )}
      {editor && (
        <AdminDialog
          title={`${editor.id ? "Chỉnh sửa" : "Thêm"} ${editor.kind === "school" ? "trường / tổ chức" : "khoa"}`}
          onClose={close}
          busy={busy}
          footer={
            <>
              <button
                className="button secondary"
                type="button"
                disabled={busy}
                onClick={close}
              >
                Hủy
              </button>
              <button
                className="button primary"
                type="submit"
                form="adm-org-form"
                disabled={busy}
              >
                <Icon name="check" size={15} />
                {busy ? "Đang lưu…" : "Lưu"}
              </button>
            </>
          }
        >
          {error && <Notice tone="error">{error}</Notice>}
          <form id="adm-org-form" className="adm-form" onSubmit={save}>
            <fieldset disabled={busy}>
              <label className="field">
                {editor.kind === "school" ? "Tên trường / tổ chức" : "Tên khoa"}
                <input
                  name="name"
                  required
                  maxLength={255}
                  defaultValue={editor.name}
                  autoFocus
                />
              </label>
              {editor.kind === "school" ? (
                <label className="field">
                  Loại tổ chức
                  <select name="type" defaultValue={editor.type}>
                    {Object.entries(types).map(([key, label]) => (
                      <option key={key} value={key}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
              ) : (
                <label className="field">
                  Trường trực thuộc
                  <select
                    name="institutionId"
                    required
                    defaultValue={selected || ""}
                  >
                    {schools.data
                      ?.filter((s) => s.type !== "OTHER")
                      .map((s) => (
                        <option value={s.id} key={s.id}>
                          {s.name}
                        </option>
                      ))}
                  </select>
                </label>
              )}
            </fieldset>
          </form>
        </AdminDialog>
      )}
    </section>
  );
}
