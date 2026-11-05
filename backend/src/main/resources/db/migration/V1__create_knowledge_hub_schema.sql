-- MySQL 8.0.16+; utf8mb4; timestamps are UTC by application convention.
-- All deletes are restricted: explicit lifecycle operations must handle dependencies.

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(40) NOT NULL,
  CONSTRAINT uk_roles_name UNIQUE (name),
  description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permissions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  code VARCHAR(100) NOT NULL,
  CONSTRAINT uk_permissions_code UNIQUE (code),
  description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  username VARCHAR(50) NOT NULL,
  CONSTRAINT uk_users_username UNIQUE (username),
  email VARCHAR(254) NOT NULL,
  CONSTRAINT uk_users_email UNIQUE (email),
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL,
  email_verified BOOLEAN NOT NULL,
  last_login_at DATETIME(6),
  CONSTRAINT ck_users_0 CHECK (status IN ('ACTIVE','INACTIVE','LOCKED','DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  role_id BIGINT NOT NULL,
  CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,
  CONSTRAINT uk_user_roles_0 UNIQUE (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  role_id BIGINT NOT NULL,
  CONSTRAINT fk_role_permissions_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,
  permission_id BIGINT NOT NULL,
  CONSTRAINT fk_role_permissions_permission_id FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE RESTRICT,
  CONSTRAINT uk_role_permissions_0 UNIQUE (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_profiles (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT uk_user_profiles_user_id UNIQUE (user_id),
  display_name VARCHAR(100),
  bio TEXT,
  avatar_url VARCHAR(2048),
  website_url VARCHAR(2048),
  location VARCHAR(150)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_follows (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  follower_id BIGINT NOT NULL,
  CONSTRAINT fk_user_follows_follower_id FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE RESTRICT,
  following_id BIGINT NOT NULL,
  CONSTRAINT fk_user_follows_following_id FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT uk_user_follows_0 UNIQUE (follower_id, following_id),
  CONSTRAINT ck_user_follows_0 CHECK (follower_id <> following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(100) NOT NULL,
  slug VARCHAR(150) NOT NULL,
  CONSTRAINT uk_categories_slug UNIQUE (slug),
  description TEXT,
  parent_id BIGINT,
  CONSTRAINT fk_categories_parent_id FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE RESTRICT,
  sort_order INT NOT NULL,
  active BOOLEAN NOT NULL,
  CONSTRAINT ck_categories_1 CHECK (sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tags (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(80) NOT NULL,
  CONSTRAINT uk_tags_name UNIQUE (name),
  slug VARCHAR(100) NOT NULL,
  CONSTRAINT uk_tags_slug UNIQUE (slug),
  description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS posts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  author_id BIGINT NOT NULL,
  CONSTRAINT fk_posts_author_id FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE RESTRICT,
  title VARCHAR(255) NOT NULL,
  slug VARCHAR(255) NOT NULL,
  CONSTRAINT uk_posts_slug UNIQUE (slug),
  summary TEXT,
  content LONGTEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  visibility VARCHAR(20) NOT NULL,
  published_at DATETIME(6),
  CONSTRAINT ck_posts_0 CHECK (status IN ('DRAFT','PENDING','PUBLISHED','REJECTED','ARCHIVED')),
  CONSTRAINT ck_posts_1 CHECK (visibility IN ('PUBLIC','PRIVATE','FOLLOWERS')),
  CONSTRAINT ck_posts_2 CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL),
  INDEX ix_posts_0 (status, published_at),
  INDEX ix_posts_1 (author_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_categories (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  post_id BIGINT NOT NULL,
  CONSTRAINT fk_post_categories_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  category_id BIGINT NOT NULL,
  CONSTRAINT fk_post_categories_category_id FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
  CONSTRAINT uk_post_categories_0 UNIQUE (post_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_tags (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  post_id BIGINT NOT NULL,
  CONSTRAINT fk_post_tags_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  tag_id BIGINT NOT NULL,
  CONSTRAINT fk_post_tags_tag_id FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE RESTRICT,
  CONSTRAINT uk_post_tags_0 UNIQUE (post_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_revisions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  post_id BIGINT NOT NULL,
  CONSTRAINT fk_post_revisions_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  editor_id BIGINT NOT NULL,
  CONSTRAINT fk_post_revisions_editor_id FOREIGN KEY (editor_id) REFERENCES users(id) ON DELETE RESTRICT,
  revision_number INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  content LONGTEXT NOT NULL,
  change_note VARCHAR(500),
  CONSTRAINT uk_post_revisions_0 UNIQUE (post_id, revision_number),
  CONSTRAINT ck_post_revisions_0 CHECK (revision_number > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comments (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  post_id BIGINT NOT NULL,
  CONSTRAINT fk_comments_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  author_id BIGINT NOT NULL,
  CONSTRAINT fk_comments_author_id FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE RESTRICT,
  parent_id BIGINT,
  CONSTRAINT fk_comments_parent_id FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE RESTRICT,
  content TEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  edited_at DATETIME(6),
  CONSTRAINT uk_comments_0 UNIQUE (id, post_id),
  CONSTRAINT ck_comments_0 CHECK (status IN ('PENDING','VISIBLE','HIDDEN','DELETED')),
  INDEX ix_comments_0 (post_id, status, created_at),
  INDEX ix_comments_1 (parent_id, post_id),
  CONSTRAINT fk_comments_parent_post FOREIGN KEY (parent_id, post_id) REFERENCES comments(id, post_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_likes (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  post_id BIGINT NOT NULL,
  CONSTRAINT fk_post_likes_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_post_likes_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT uk_post_likes_0 UNIQUE (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comment_likes (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  comment_id BIGINT NOT NULL,
  CONSTRAINT fk_comment_likes_comment_id FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE RESTRICT,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_comment_likes_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT uk_comment_likes_0 UNIQUE (comment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bookmarks (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_bookmarks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  post_id BIGINT NOT NULL,
  CONSTRAINT fk_bookmarks_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  CONSTRAINT uk_bookmarks_0 UNIQUE (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS collections (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  owner_id BIGINT NOT NULL,
  CONSTRAINT fk_collections_owner_id FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
  name VARCHAR(150) NOT NULL,
  slug VARCHAR(180) NOT NULL,
  description TEXT,
  visibility VARCHAR(20) NOT NULL,
  CONSTRAINT uk_collections_0 UNIQUE (owner_id, slug),
  CONSTRAINT ck_collections_0 CHECK (visibility IN ('PUBLIC','PRIVATE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS collection_posts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  collection_id BIGINT NOT NULL,
  CONSTRAINT fk_collection_posts_collection_id FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE RESTRICT,
  post_id BIGINT NOT NULL,
  CONSTRAINT fk_collection_posts_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  sort_order INT NOT NULL,
  CONSTRAINT uk_collection_posts_0 UNIQUE (collection_id, post_id),
  CONSTRAINT ck_collection_posts_0 CHECK (sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attachments (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  uploader_id BIGINT NOT NULL,
  CONSTRAINT fk_attachments_uploader_id FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE RESTRICT,
  post_id BIGINT,
  CONSTRAINT fk_attachments_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  original_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(255) NOT NULL,
  CONSTRAINT uk_attachments_storage_key UNIQUE (storage_key),
  content_type VARCHAR(150) NOT NULL,
  size_bytes BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  CONSTRAINT ck_attachments_0 CHECK (size_bytes >= 0),
  CONSTRAINT ck_attachments_1 CHECK (status IN ('PENDING','READY','REJECTED','DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_views (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  post_id BIGINT NOT NULL,
  CONSTRAINT fk_post_views_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  viewer_id BIGINT,
  CONSTRAINT fk_post_views_viewer_id FOREIGN KEY (viewer_id) REFERENCES users(id) ON DELETE RESTRICT,
  visitor_hash VARCHAR(64),
  viewed_at DATETIME(6) NOT NULL,
  CONSTRAINT ck_post_views_0 CHECK (viewer_id IS NOT NULL OR visitor_hash IS NOT NULL),
  INDEX ix_post_views_0 (post_id, viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  token_hash VARCHAR(64) NOT NULL,
  CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
  family_id VARCHAR(36) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6),
  user_agent VARCHAR(512),
  ip_hash VARCHAR(64),
  CONSTRAINT ck_refresh_tokens_0 CHECK (expires_at > created_at),
  INDEX ix_refresh_tokens_0 (family_id),
  INDEX ix_refresh_tokens_1 (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_password_reset_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  token_hash VARCHAR(64) NOT NULL,
  CONSTRAINT uk_password_reset_tokens_token_hash UNIQUE (token_hash),
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6),
  CONSTRAINT ck_password_reset_tokens_0 CHECK (expires_at > created_at),
  INDEX ix_password_reset_tokens_0 (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_verification_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_email_verification_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  token_hash VARCHAR(64) NOT NULL,
  CONSTRAINT uk_email_verification_tokens_token_hash UNIQUE (token_hash),
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6),
  CONSTRAINT ck_email_verification_tokens_0 CHECK (expires_at > created_at),
  INDEX ix_email_verification_tokens_0 (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  recipient_id BIGINT NOT NULL,
  CONSTRAINT fk_notifications_recipient_id FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE RESTRICT,
  actor_id BIGINT,
  CONSTRAINT fk_notifications_actor_id FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE RESTRICT,
  post_id BIGINT,
  CONSTRAINT fk_notifications_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  comment_id BIGINT,
  CONSTRAINT fk_notifications_comment_id FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE RESTRICT,
  type VARCHAR(40) NOT NULL,
  title VARCHAR(255) NOT NULL,
  body TEXT,
  read_at DATETIME(6),
  CONSTRAINT ck_notifications_0 CHECK (type IN ('FOLLOW','POST_PUBLISHED','POST_LIKED','COMMENT_ADDED','COMMENT_REPLIED','MENTION','MODERATION','SYSTEM')),
  INDEX ix_notifications_0 (recipient_id, read_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_preferences (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_notification_preferences_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  type VARCHAR(40) NOT NULL,
  in_app_enabled BOOLEAN NOT NULL,
  email_enabled BOOLEAN NOT NULL,
  CONSTRAINT uk_notification_preferences_0 UNIQUE (user_id, type),
  CONSTRAINT ck_notification_preferences_0 CHECK (type IN ('FOLLOW','POST_PUBLISHED','POST_LIKED','COMMENT_ADDED','COMMENT_REPLIED','MENTION','MODERATION','SYSTEM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reports (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  reporter_id BIGINT NOT NULL,
  CONSTRAINT fk_reports_reporter_id FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE RESTRICT,
  post_id BIGINT,
  CONSTRAINT fk_reports_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  comment_id BIGINT,
  CONSTRAINT fk_reports_comment_id FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE RESTRICT,
  reported_user_id BIGINT,
  CONSTRAINT fk_reports_reported_user_id FOREIGN KEY (reported_user_id) REFERENCES users(id) ON DELETE RESTRICT,
  reason VARCHAR(40) NOT NULL,
  details TEXT,
  status VARCHAR(20) NOT NULL,
  resolved_by_id BIGINT,
  CONSTRAINT fk_reports_resolved_by_id FOREIGN KEY (resolved_by_id) REFERENCES users(id) ON DELETE RESTRICT,
  resolved_at DATETIME(6),
  CONSTRAINT ck_reports_0 CHECK ((post_id IS NOT NULL) + (comment_id IS NOT NULL) + (reported_user_id IS NOT NULL) = 1),
  CONSTRAINT ck_reports_1 CHECK (status IN ('OPEN','IN_REVIEW','RESOLVED','DISMISSED')),
  CONSTRAINT ck_reports_2 CHECK (reason IN ('SPAM','HARASSMENT','HATE','MISINFORMATION','COPYRIGHT','OTHER')),
  INDEX ix_reports_0 (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS moderation_actions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  moderator_id BIGINT NOT NULL,
  CONSTRAINT fk_moderation_actions_moderator_id FOREIGN KEY (moderator_id) REFERENCES users(id) ON DELETE RESTRICT,
  report_id BIGINT,
  CONSTRAINT fk_moderation_actions_report_id FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE RESTRICT,
  post_id BIGINT,
  CONSTRAINT fk_moderation_actions_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  comment_id BIGINT,
  CONSTRAINT fk_moderation_actions_comment_id FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE RESTRICT,
  target_user_id BIGINT,
  CONSTRAINT fk_moderation_actions_target_user_id FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE RESTRICT,
  action VARCHAR(40) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  expires_at DATETIME(6),
  CONSTRAINT ck_moderation_actions_0 CHECK ((post_id IS NOT NULL) + (comment_id IS NOT NULL) + (target_user_id IS NOT NULL) = 1),
  CONSTRAINT ck_moderation_actions_1 CHECK (action IN ('WARN','HIDE','RESTORE','LOCK','UNLOCK','REJECT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  actor_id BIGINT,
  CONSTRAINT fk_audit_logs_actor_id FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE RESTRICT,
  action VARCHAR(100) NOT NULL,
  entity_type VARCHAR(80) NOT NULL,
  entity_id BIGINT,
  details TEXT,
  ip_hash VARCHAR(64),
  INDEX ix_audit_logs_0 (entity_type, entity_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_requests (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_ai_requests_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  post_id BIGINT,
  CONSTRAINT fk_ai_requests_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  operation VARCHAR(40) NOT NULL,
  provider VARCHAR(80) NOT NULL,
  model VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL,
  input_tokens INT,
  output_tokens INT,
  latency_ms BIGINT,
  error_code VARCHAR(100),
  completed_at DATETIME(6),
  CONSTRAINT ck_ai_requests_0 CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED')),
  CONSTRAINT ck_ai_requests_1 CHECK (input_tokens IS NULL OR input_tokens >= 0),
  CONSTRAINT ck_ai_requests_2 CHECK (output_tokens IS NULL OR output_tokens >= 0),
  CONSTRAINT ck_ai_requests_3 CHECK (latency_ms IS NULL OR latency_ms >= 0),
  INDEX ix_ai_requests_0 (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
