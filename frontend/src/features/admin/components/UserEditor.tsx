import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { mutate, errorMessage } from '../../../shared/api/http-client';
import type { Account } from '../../auth/types';
import { roleNames, statusNames } from '../../auth/types';
import PasswordInput from '../../../shared/components/ui/PasswordInput';
import Icon from '../../../shared/components/ui/Icon';
export interface EditorState { mode: 'create' | 'edit' | 'password'; user?: Account }
export default function UserEditor({ state, onClose, onSaved }: { state: EditorState; onClose: () => void; onSaved: (message: string) => Promise<void> }) {
 const ref = useRef<HTMLDialogElement>(null); const [busy, setBusy] = useState(false); const [error, setError] = useState('');
 useEffect(() => { ref.current?.showModal(); }, []);
 const title = state.mode === 'create' ? 'Tạo tài khoản mới' : state.mode === 'edit' ? 'Chỉnh sửa tài khoản' : 'Đặt lại mật khẩu';
 async function submit(event: FormEvent<HTMLFormElement>) {
  event.preventDefault(); const form = new FormData(event.currentTarget); const roles = form.getAll('roles');
  if (state.mode !== 'password' && !roles.length) { setError('Hãy chọn ít nhất một vai trò.'); return; }
  if (state.mode !== 'edit' && form.get('password') !== form.get('confirm')) { setError('Mật khẩu xác nhận chưa khớp.'); return; }
  setBusy(true); setError('');
  try {
   if (state.mode === 'create') await mutate('/admin/users', 'POST', { username: form.get('username'), email: form.get('email'), password: form.get('password'), roles });
   else if (state.mode === 'edit') await mutate(`/admin/users/${state.user!.id}`, 'PUT', { email: form.get('email'), status: form.get('status'), roles });
   else await mutate(`/admin/users/${state.user!.id}/password`, 'POST', { password: form.get('password') });
   await onSaved(state.mode === 'password' ? 'Đã đặt lại mật khẩu. Các phiên đăng nhập cũ đã bị thu hồi.' : 'Đã lưu tài khoản thành công.');
  } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
 }
 return <dialog ref={ref} className="editor-dialog" aria-labelledby="editor-title" onCancel={e => { e.preventDefault(); if (!busy) onClose(); }}><div className="dialog-header"><div><span className="section-kicker">QUẢN LÝ THÀNH VIÊN</span><h2 id="editor-title">{title}</h2>{state.user && <p className="muted">@{state.user.username}</p>}</div><button className="icon-button" type="button" aria-label="Đóng" disabled={busy} onClick={onClose}><Icon name="close"/></button></div>{error && <div className="alert" role="alert">{error}</div>}<form onSubmit={submit}><fieldset disabled={busy}>{state.mode === 'create' && <label className="field">Tên đăng nhập<input autoFocus name="username" autoComplete="off" required minLength={3} maxLength={50} pattern="[a-zA-Z0-9_.-]{3,50}" placeholder="nguyenvan.a"/></label>}{state.mode !== 'password' && <label className="field">Địa chỉ email<input name="email" type="email" required maxLength={254} defaultValue={state.user?.email} placeholder="ban@example.com"/></label>}{state.mode === 'edit' && <label className="field">Trạng thái<select name="status" defaultValue={state.user?.status}>{Object.entries(statusNames).map(([key, value]) => <option key={key} value={key}>{value}</option>)}</select><small>Đổi trạng thái hoặc vai trò sẽ yêu cầu người dùng đăng nhập lại.</small></label>}{state.mode !== 'edit' && <><PasswordInput label="Mật khẩu mới" name="password" required minLength={8} maxLength={72} autoComplete="new-password" hint="Tối thiểu 8 ký tự, tối đa 72 byte UTF-8."/><PasswordInput label="Xác nhận mật khẩu" name="confirm" required minLength={8} maxLength={72} autoComplete="new-password"/></>}{state.mode !== 'password' && <div className="field"><span className="field-label">Vai trò</span><div className="role-options">{Object.entries(roleNames).map(([key, label]) => <label className="check-option" key={key}><input type="checkbox" name="roles" value={key} defaultChecked={state.user ? state.user.roles.includes(key) : key === 'USER'}/><span>{label}<small>{key === 'ADMIN' ? 'Quản lý tài khoản và phân quyền' : key === 'MODERATOR' ? 'Vai trò kiểm duyệt nội dung' : 'Tài khoản thành viên'}</small></span></label>)}</div></div>}<div className="dialog-actions"><button className="button secondary" type="button" onClick={onClose}><Icon name="close" size={16}/>Hủy</button><button className="button primary" type="submit">{busy ? 'Đang lưu…' : state.mode === 'create' ? 'Tạo tài khoản' : 'Lưu thay đổi'}</button></div></fieldset></form></dialog>;
}
