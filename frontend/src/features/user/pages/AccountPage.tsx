import { useState } from 'react';
import type { FormEvent } from 'react';
import { mutate, errorMessage } from '../../../shared/api/http-client';
import type { Account } from '../../auth/types';
import { dateLabel, roleNames } from '../../auth/types';
import PasswordInput from '../../../shared/components/ui/PasswordInput';
import Icon from '../../../shared/components/ui/Icon';
import InstitutionPicker from '../../organizations/InstitutionPicker';
import AvatarPicker from '../components/AvatarPicker';

/*
 * Trang "Tài khoản của tôi" (#account):
 * - Cột trái: thông tin tài khoản, đổi ảnh đại diện, sửa hồ sơ (tên, giới thiệu, trường/khoa)
 * - Cột phải: đổi mật khẩu
 */

type Props = {
  user: Account;
  onChanged: () => void; // Gọi sau khi đổi mật khẩu thành công (App sẽ bắt đăng nhập lại)
  onProfileSaved: (user: Account) => void; // Gọi sau khi lưu hồ sơ / ảnh để App cập nhật thông tin
};

export default function AccountPage({ user, onChanged, onProfileSaved }: Props) {
  // ----- Hồ sơ -----
  const [institution, setInstitution] = useState(user.organization ? String(user.organization.institutionId) : '');
  const [faculty, setFaculty] = useState(user.organization?.facultyId ? String(user.organization.facultyId) : '');
  const [profileBusy, setProfileBusy] = useState(false);
  const [profileMessage, setProfileMessage] = useState('');
  const [profileError, setProfileError] = useState('');

  // ----- Đổi mật khẩu -----
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  /** Lưu tên hiển thị, giới thiệu và trường/khoa. */
  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);

    setProfileBusy(true);
    setProfileError('');
    setProfileMessage('');
    try {
      const updated = await mutate<Account>('/users/me/profile', 'PUT', {
        displayName: String(form.get('displayName') || ''),
        bio: String(form.get('bio') || ''),
        institutionId: institution ? Number(institution) : null,
        facultyId: faculty ? Number(faculty) : null,
      });
      onProfileSaved(updated);
      setProfileMessage('Đã lưu hồ sơ cá nhân.');
    } catch (e) {
      setProfileError(errorMessage(e));
    } finally {
      setProfileBusy(false);
    }
  }

  /** Đổi mật khẩu. */
  async function changePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const newPassword = String(form.get('newPassword'));

    // Mật khẩu mới và mật khẩu nhập lại phải giống nhau
    if (newPassword !== form.get('confirm')) {
      setError('Mật khẩu xác nhận chưa khớp.');
      return;
    }

    setBusy(true);
    setError('');
    try {
      await mutate('/users/me/password', 'POST', {
        currentPassword: form.get('currentPassword'),
        newPassword: newPassword,
      });
      onChanged();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <span className="section-kicker">KHÔNG GIAN CÁ NHÂN</span>
          <h1>Tài khoản của tôi</h1>
          <p className="muted">Thông tin cá nhân và bảo mật tài khoản của bạn.</p>
        </div>
      </div>

      <div className="account-grid">
        {/* ===== Cột trái: thông tin và hồ sơ ===== */}
        <section className="panel">
          <div className="panel-title">
            <Icon name="user" />
            <h2>Thông tin tài khoản</h2>
          </div>

          {/* Ảnh đại diện: bấm vào ảnh để chọn, xem trước rồi mới lưu */}
          <AvatarPicker user={user} onSaved={onProfileSaved} />

          <dl className="detail-list">
            <div>
              <dt>Email</dt>
              <dd>{user.email}</dd>
            </div>
            <div>
              <dt>Vai trò</dt>
              <dd>{user.roles.map((r) => roleNames[r]).join(', ')}</dd>
            </div>
            <div>
              <dt>Trường / Tổ chức</dt>
              <dd>{user.organization?.institutionName || 'Chưa chọn'}</dd>
            </div>
            <div>
              <dt>Khoa / Chuyên ngành</dt>
              <dd>{user.organization?.facultyName || 'Không chọn khoa'}</dd>
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

          <form onSubmit={saveProfile} className="profile-organization-form">
            <h3>Chỉnh sửa hồ sơ</h3>
            <p className="muted small">Lựa chọn này giúp lọc tài liệu và điền sẵn thông tin khi bạn đóng góp.</p>

            {profileError && (
              <div className="alert" role="alert">
                {profileError}
              </div>
            )}
            {profileMessage && (
              <div className="notice" role="status">
                {profileMessage}
              </div>
            )}

            <fieldset disabled={profileBusy}>
              <div className="field">
                <label htmlFor="profile-name">Tên hiển thị</label>
                <input id="profile-name" name="displayName" maxLength={100} defaultValue={user.displayName || ''} />
              </div>
              <div className="field">
                <label htmlFor="profile-bio">Giới thiệu bản thân</label>
                <textarea
                  id="profile-bio"
                  name="bio"
                  rows={4}
                  maxLength={2000}
                  defaultValue={user.bio || ''}
                  placeholder="Chia sẻ một chút về bạn và lĩnh vực yêu thích…"
                />
              </div>
              <InstitutionPicker
                institutionId={institution}
                facultyId={faculty}
                onChange={(i, f) => {
                  setInstitution(i);
                  setFaculty(f);
                }}
              />
              <button className="button primary" type="submit">
                {profileBusy ? 'Đang lưu…' : 'Lưu hồ sơ'}
              </button>
            </fieldset>
          </form>
        </section>

        {/* ===== Cột phải: đổi mật khẩu ===== */}
        <section className="panel">
          <div className="panel-title">
            <Icon name="lock" />
            <h2>Đổi mật khẩu</h2>
          </div>
          <p className="muted small">
            Sau khi đổi mật khẩu, tất cả phiên hiện tại sẽ hết hiệu lực. Bạn cần đăng nhập lại.
          </p>

          {error && (
            <div className="alert" role="alert">
              {error}
            </div>
          )}

          <form onSubmit={changePassword}>
            <fieldset disabled={busy}>
              <PasswordInput
                label="Mật khẩu hiện tại"
                name="currentPassword"
                required
                maxLength={72}
                autoComplete="current-password"
              />
              <PasswordInput
                label="Mật khẩu mới"
                name="newPassword"
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                hint="Tối thiểu 8 ký tự, tối đa 72 byte UTF-8."
              />
              <PasswordInput
                label="Xác nhận mật khẩu mới"
                name="confirm"
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
              />
              <button className="button primary" type="submit">
                {busy ? 'Đang cập nhật…' : 'Cập nhật mật khẩu'}
              </button>
            </fieldset>
          </form>
        </section>
      </div>
    </>
  );
}
