-- Tạo tài khoản quản trị dễ nhớ cho môi trường chạy thử (LOCAL).
--   Tên đăng nhập: quantri
--   Mật khẩu:      12345678
--   Vai trò:       USER + ADMIN
-- Chạy lại nhiều lần không bị trùng. Tài khoản admin cũ được giữ nguyên.
-- Mã băm BCrypt giống scripts/reset-test-users.sql (cost 12, cùng kiểu với backend).

START TRANSACTION;

SET @now = UTC_TIMESTAMP(6);
SET @hash = '$2a$12$4JbtEnaa4Ib3KQXJtQ0aPuRm/K5zuFwcXDgtwicnCECcSC2Z34aby';

-- Bước 1: tạo tài khoản nếu chưa có
INSERT INTO users (created_at, updated_at, version, username, email, password_hash, status, email_verified, auth_version)
SELECT @now, @now, 0, 'quantri', 'quantri@knowledgehub.test', @hash, 'ACTIVE', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'quantri');

SET @admin_id = (SELECT id FROM users WHERE username = 'quantri');

-- Bước 2: đặt lại mật khẩu và trạng thái (phòng khi đã bị đổi / bị khoá)
UPDATE users SET password_hash = @hash, status = 'ACTIVE', auth_version = auth_version + 1, updated_at = @now
WHERE id = @admin_id;

-- Bước 3: hồ sơ
INSERT INTO user_profiles (created_at, updated_at, version, user_id, display_name, bio)
SELECT @now, @now, 0, @admin_id, 'Quản trị viên', 'Tài khoản quản trị dùng để chạy thử hệ thống.'
WHERE NOT EXISTS (SELECT 1 FROM user_profiles WHERE user_id = @admin_id);

-- Bước 4: vai trò USER và ADMIN
INSERT INTO user_roles (created_at, updated_at, version, user_id, role_id)
SELECT @now, @now, 0, @admin_id, r.id FROM roles r
WHERE r.name IN ('USER', 'ADMIN')
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = @admin_id AND ur.role_id = r.id);

COMMIT;

SELECT u.username, u.status, GROUP_CONCAT(r.name ORDER BY r.name) AS roles
FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
WHERE u.username = 'quantri'
GROUP BY u.id;
