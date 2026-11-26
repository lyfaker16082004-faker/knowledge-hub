-- Reset test accounts in the LOCAL dev database. Run through scripts/reset-test-users.ps1.
-- Removes accounts named e2e_*, codex_* and the fixed test accounts (user1, user2, mod1)
-- together with the documents they uploaded, then recreates user1, user2 and mod1.
-- admin and every other real account are left untouched.
-- Password of the recreated accounts: 12345678 (BCrypt cost 12, same as the backend encoder).

START TRANSACTION;

SET @now = UTC_TIMESTAMP(6);
SET @hash = '$2a$12$4JbtEnaa4Ib3KQXJtQ0aPuRm/K5zuFwcXDgtwicnCECcSC2Z34aby';

DROP TEMPORARY TABLE IF EXISTS reset_users;
CREATE TEMPORARY TABLE reset_users (id BIGINT PRIMARY KEY)
  SELECT id FROM users
  WHERE username LIKE 'e2e\_%' OR username LIKE 'codex\_%' OR username IN ('user1', 'user2', 'mod1');

DROP TEMPORARY TABLE IF EXISTS reset_documents;
CREATE TEMPORARY TABLE reset_documents (id BIGINT PRIMARY KEY)
  SELECT id FROM documents WHERE author_id IN (SELECT id FROM reset_users);

-- documents uploaded by the test accounts
DELETE FROM document_tags      WHERE document_id IN (SELECT id FROM reset_documents);
DELETE FROM document_access    WHERE document_id IN (SELECT id FROM reset_documents);
DELETE FROM document_bookmarks WHERE document_id IN (SELECT id FROM reset_documents);
DELETE FROM document_downloads WHERE document_id IN (SELECT id FROM reset_documents);
DELETE FROM document_files     WHERE document_id IN (SELECT id FROM reset_documents);
DELETE FROM documents          WHERE id IN (SELECT id FROM reset_documents);
UPDATE documents SET reviewed_by_id = NULL WHERE reviewed_by_id IN (SELECT id FROM reset_users);

-- activity of the test accounts on other documents / users
DELETE FROM document_access    WHERE user_id IN (SELECT id FROM reset_users);
DELETE FROM document_bookmarks WHERE user_id IN (SELECT id FROM reset_users);
DELETE FROM document_downloads WHERE user_id IN (SELECT id FROM reset_users);
DELETE FROM user_follows       WHERE follower_id IN (SELECT id FROM reset_users);
DELETE FROM user_follows       WHERE following_id IN (SELECT id FROM reset_users);
DELETE FROM post_likes         WHERE user_id IN (SELECT id FROM reset_users);
DELETE FROM post_views         WHERE viewer_id IN (SELECT id FROM reset_users);
DELETE FROM comment_likes      WHERE user_id IN (SELECT id FROM reset_users);
DELETE FROM bookmarks          WHERE user_id IN (SELECT id FROM reset_users);
DELETE FROM ai_requests        WHERE user_id IN (SELECT id FROM reset_users);

-- the accounts themselves
DELETE FROM user_roles    WHERE user_id IN (SELECT id FROM reset_users);
DELETE FROM user_profiles WHERE user_id IN (SELECT id FROM reset_users);
DELETE FROM users         WHERE id IN (SELECT id FROM reset_users);

-- fresh test accounts
INSERT INTO users (created_at, updated_at, version, username, email, password_hash, status, email_verified, auth_version) VALUES
  (@now, @now, 0, 'user1', 'user1@example.com', @hash, 'ACTIVE', 1, 0),
  (@now, @now, 0, 'user2', 'user2@example.com', @hash, 'ACTIVE', 1, 0),
  (@now, @now, 0, 'mod1',  'mod1@example.com',  @hash, 'ACTIVE', 1, 0);

INSERT INTO user_profiles (created_at, updated_at, version, user_id, display_name)
  SELECT @now, @now, 0, id, CASE username WHEN 'user1' THEN 'Người dùng 1' WHEN 'user2' THEN 'Người dùng 2' ELSE 'Kiểm duyệt 1' END
  FROM users WHERE username IN ('user1', 'user2', 'mod1');

INSERT INTO user_roles (created_at, updated_at, version, user_id, role_id)
  SELECT @now, @now, 0, u.id, r.id FROM users u JOIN roles r ON r.name = 'USER'
  WHERE u.username IN ('user1', 'user2', 'mod1');
INSERT INTO user_roles (created_at, updated_at, version, user_id, role_id)
  SELECT @now, @now, 0, u.id, r.id FROM users u JOIN roles r ON r.name = 'MODERATOR'
  WHERE u.username = 'mod1';

COMMIT;

SELECT u.username, u.email, u.status, GROUP_CONCAT(r.name ORDER BY r.name) AS roles
FROM users u LEFT JOIN user_roles ur ON ur.user_id = u.id LEFT JOIN roles r ON r.id = ur.role_id
GROUP BY u.id ORDER BY u.id;
