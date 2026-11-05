-- Vietnamese and emoji: VARCHAR/TEXT CHARACTER SET utf8mb4, not MySQL NVARCHAR (utf8mb3).
-- KNOWLEDGE HUB - COMPLETE MYSQL DATABASE SCRIPT
-- Current schema: 26 application tables; role-based authorization.
-- MySQL 8.0.16+; no Docker required.
-- Standalone snapshot for documentation and creating a NEW database.
-- Existing application databases must use Flyway migrations instead.
-- Tables are not dropped. Existing tables are not upgraded by this script.

CREATE DATABASE IF NOT EXISTS knowledge_hub
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE knowledge_hub;
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- MySQL 8.0.16+; utf8mb4; timestamps are UTC by application convention.
-- All deletes are restricted: explicit lifecycle operations must handle dependencies.

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT uk_roles_name UNIQUE (name),
  description VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
  auth_version BIGINT NOT NULL DEFAULT 0,
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT uk_users_username UNIQUE (username),
  email VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT uk_users_email UNIQUE (email),
  password_hash VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
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

CREATE TABLE IF NOT EXISTS user_profiles (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT uk_user_profiles_user_id UNIQUE (user_id),
  display_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  bio TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  avatar_url VARCHAR(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  website_url VARCHAR(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  location VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
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
  name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  slug VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT uk_categories_slug UNIQUE (slug),
  description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
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
  name VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT uk_tags_name UNIQUE (name),
  slug VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT uk_tags_slug UNIQUE (slug),
  description VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS posts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  author_id BIGINT NOT NULL,
  CONSTRAINT fk_posts_author_id FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE RESTRICT,
  title VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  slug VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT uk_posts_slug UNIQUE (slug),
  summary TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  content LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  visibility VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
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
  content TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
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

CREATE TABLE IF NOT EXISTS attachments (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  uploader_id BIGINT NOT NULL,
  CONSTRAINT fk_attachments_uploader_id FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE RESTRICT,
  post_id BIGINT,
  CONSTRAINT fk_attachments_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
  original_name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  storage_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT uk_attachments_storage_key UNIQUE (storage_key),
  content_type VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  size_bytes BIGINT NOT NULL,
  status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
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
  visitor_hash VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  viewed_at DATETIME(6) NOT NULL,
  CONSTRAINT ck_post_views_0 CHECK (viewer_id IS NOT NULL OR visitor_hash IS NOT NULL),
  INDEX ix_post_views_0 (post_id, viewed_at)
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
  operation VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  provider VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  model VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  input_tokens INT,
  output_tokens INT,
  latency_ms BIGINT,
  error_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  completed_at DATETIME(6),
  CONSTRAINT ck_ai_requests_0 CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED')),
  CONSTRAINT ck_ai_requests_1 CHECK (input_tokens IS NULL OR input_tokens >= 0),
  CONSTRAINT ck_ai_requests_2 CHECK (output_tokens IS NULL OR output_tokens >= 0),
  CONSTRAINT ck_ai_requests_3 CHECK (latency_ms IS NULL OR latency_ms >= 0),
  INDEX ix_ai_requests_0 (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initial role data (safe if these names already exist).
INSERT IGNORE INTO roles (name, description, created_at, updated_at, version) VALUES
('ADMIN', 'System administrator', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
('MODERATOR', 'Content moderator', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
('USER', 'Registered member', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0);


-- Document catalogue, protected files and per-user library. MySQL 8.0.16+.

CREATE TABLE documents (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  category_id BIGINT NOT NULL,
  CONSTRAINT fk_documents_category_id FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
  author_id BIGINT,
  CONSTRAINT fk_documents_author_id FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE RESTRICT,
  title VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  slug VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT uk_documents_slug UNIQUE (slug),
  summary TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  description LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  access_type VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  level VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  language VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  featured BOOLEAN NOT NULL,
  published_at DATETIME(6),
  cover_style VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT ck_documents_0 CHECK (access_type IN ('FREE','PAID')),
  CONSTRAINT ck_documents_1 CHECK ((access_type='FREE' AND price=0) OR (access_type='PAID' AND price>0)),
  CONSTRAINT ck_documents_2 CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
  CONSTRAINT ck_documents_3 CHECK (level IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
  CONSTRAINT ck_documents_4 CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL),
  INDEX ix_documents_0 (status, published_at),
  INDEX ix_documents_1 (category_id, status),
  INDEX ix_documents_2 (access_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document_files (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  document_id BIGINT NOT NULL,
  CONSTRAINT fk_document_files_document_id FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE RESTRICT,
  original_name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  storage_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  CONSTRAINT uk_document_files_storage_key UNIQUE (storage_key),
  content_type VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  size_bytes BIGINT NOT NULL,
  active BOOLEAN NOT NULL,
  CONSTRAINT uk_document_files_0 UNIQUE (id, document_id),
  CONSTRAINT ck_document_files_0 CHECK (size_bytes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document_access (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  document_id BIGINT NOT NULL,
  CONSTRAINT fk_document_access_document_id FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE RESTRICT,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_document_access_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  grant_type VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  expires_at DATETIME(6),
  revoked_at DATETIME(6),
  CONSTRAINT uk_document_access_0 UNIQUE (user_id, document_id),
  CONSTRAINT ck_document_access_0 CHECK (grant_type IN ('MANUAL','PURCHASE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document_downloads (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  document_id BIGINT NOT NULL,
  CONSTRAINT fk_document_downloads_document_id FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE RESTRICT,
  file_id BIGINT NOT NULL,
  CONSTRAINT fk_document_downloads_file_id FOREIGN KEY (file_id) REFERENCES document_files(id) ON DELETE RESTRICT,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_document_downloads_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  document_title VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  file_name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  INDEX ix_document_downloads_0 (user_id, created_at),
  INDEX ix_document_downloads_1 (document_id, created_at),
  CONSTRAINT fk_download_file_document FOREIGN KEY (file_id, document_id) REFERENCES document_files(id, document_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document_bookmarks (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  document_id BIGINT NOT NULL,
  CONSTRAINT fk_document_bookmarks_document_id FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE RESTRICT,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_document_bookmarks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT uk_document_bookmarks_0 UNIQUE (user_id, document_id),
  INDEX ix_document_bookmarks_0 (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Original starter materials, not external publications. Paid access is manual; no checkout.

INSERT IGNORE INTO categories (name,slug,sort_order,active,created_at,updated_at,version) VALUES ('Lập trình Java','java-spring',0,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0);

INSERT IGNORE INTO categories (name,slug,sort_order,active,created_at,updated_at,version) VALUES ('Cơ sở dữ liệu','database',0,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0);

INSERT IGNORE INTO categories (name,slug,sort_order,active,created_at,updated_at,version) VALUES ('Frontend','frontend',0,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0);

INSERT IGNORE INTO categories (name,slug,sort_order,active,created_at,updated_at,version) VALUES ('Kỹ năng học tập','study',0,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0);

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Java căn bản: từ biến đến lập trình hướng đối tượng','java-can-ban','Làm quen với biến, điều kiện, vòng lặp và cách tổ chức một chương trình Java dễ đọc.','Làm quen với biến, điều kiện, vòng lặp và cách tổ chức một chương trình Java dễ đọc.

Nội dung chính:
1. Chương trình đầu tiên
2. Biến và điều kiện
3. Tách trách nhiệm
4. Bài tập

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','BEGINNER','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'violet',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='java-spring';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'java-can-ban.md','java-can-ban.md','text/markdown; charset=UTF-8',948,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='java-can-ban';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Thiết kế MySQL và lưu tiếng Việt đúng cách','mysql-tieng-viet','Khóa chính, khóa ngoại, utf8mb4 và những kiểm tra đầu tiên khi làm việc với MySQL.','Khóa chính, khóa ngoại, utf8mb4 và những kiểm tra đầu tiên khi làm việc với MySQL.

Nội dung chính:
1. Tạo database Unicode
2. Ràng buộc
3. Kiểm thử tiếng Việt
4. Bài tập

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','BEGINNER','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'peach',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='database';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'mysql-tieng-viet.md','mysql-tieng-viet.md','text/markdown; charset=UTF-8',931,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='mysql-tieng-viet';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'React & TypeScript: xây giao diện đầu tiên','react-typescript','Component, props, state và cách xử lý các trạng thái tải, lỗi, rỗng trong giao diện.','Component, props, state và cách xử lý các trạng thái tải, lỗi, rỗng trong giao diện.

Nội dung chính:
1. Component
2. State
3. Gọi API
4. Bài tập

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','BEGINNER','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'mint',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='frontend';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'react-typescript.md','react-typescript.md','text/markdown; charset=UTF-8',765,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='react-typescript';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Git thực hành cho một nhóm nhỏ','git-lam-viec-nhom','Quy trình branch, commit, pull request và cách xử lý xung đột một cách bình tĩnh.','Quy trình branch, commit, pull request và cách xử lý xung đột một cách bình tĩnh.

Nội dung chính:
1. Kiểm tra trước khi sửa
2. Một nhánh cho một mục tiêu
3. Review
4. Xung đột

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','BEGINNER','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'sky',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='study';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'git-lam-viec-nhom.md','git-lam-viec-nhom.md','text/markdown; charset=UTF-8',766,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='git-lam-viec-nhom';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Checklist xây REST API dễ sử dụng','rest-api-checklist','Một checklist ngắn về DTO, validation, phân trang, xác thực và lỗi API.','Một checklist ngắn về DTO, validation, phân trang, xác thực và lỗi API.

Nội dung chính:
Hợp đồng API
Dữ liệu
Cookie và tải file
Kiểm thử

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','INTERMEDIATE','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'rose',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='java-spring';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'rest-api-checklist.md','rest-api-checklist.md','text/markdown; charset=UTF-8',917,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='rest-api-checklist';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Lập kế hoạch tự học trong 4 tuần','ke-hoach-tu-hoc','Chia mục tiêu lớn thành các buổi học nhỏ, ghi tiến độ và ôn tập có chủ đích.','Chia mục tiêu lớn thành các buổi học nhỏ, ghi tiến độ và ôn tập có chủ đích.

Nội dung chính:
Tuần 1: Mục tiêu
Tuần 2: Sản phẩm nhỏ
Tuần 3: Củng cố
Tuần 4: Hoàn thiện
Mẫu ghi chép

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','FREE',0,'VND','BEGINNER','vi','PUBLISHED',true,UTC_TIMESTAMP(6),'sand',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='study';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'ke-hoach-tu-hoc.md','ke-hoach-tu-hoc.md','text/markdown; charset=UTF-8',852,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='ke-hoach-tu-hoc';

INSERT INTO documents (category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style,created_at,updated_at,version) SELECT id,'Thiết kế module Spring Boot có kiểm thử','spring-boot-chuyen-sau','Tài liệu mẫu có phí: transaction, service và kiểm thử tích hợp cho module tài khoản.','Tài liệu mẫu có phí: transaction, service và kiểm thử tích hợp cho module tài khoản.

Nội dung chính:
1. Ranh giới module
2. Transaction
3. Thu hồi phiên
4. Kiểm thử
Bài tập

Định dạng Markdown (.md), mở bằng VS Code hoặc trình đọc Markdown. Đây là tài liệu mẫu nội bộ để trải nghiệm thư viện.','PAID',49000,'VND','INTERMEDIATE','vi','PUBLISHED',false,UTC_TIMESTAMP(6),'violet',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM categories WHERE slug='java-spring';

INSERT INTO document_files (document_id,original_name,storage_key,content_type,size_bytes,active,created_at,updated_at,version) SELECT id,'spring-boot-chuyen-sau.md','spring-boot-chuyen-sau.md','text/markdown; charset=UTF-8',873,true,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM documents WHERE slug='spring-boot-chuyen-sau';


-- Organization hierarchy. Faculty/institution pairs are protected by composite foreign keys.
CREATE TABLE institutions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 type VARCHAR(20) NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_institutions_name UNIQUE(name),
 CONSTRAINT ck_institutions_type CHECK(type IN ('UNIVERSITY','COMPANY','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE faculties (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 institution_id BIGINT NOT NULL,
 name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_faculties_institution FOREIGN KEY(institution_id) REFERENCES institutions(id) ON DELETE RESTRICT,
 CONSTRAINT uk_faculties_institution_name UNIQUE(institution_id,name),
 CONSTRAINT uk_faculties_id_institution UNIQUE(id,institution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE user_profiles
 ADD COLUMN institution_id BIGINT NULL,
 ADD COLUMN faculty_id BIGINT NULL,
 ADD CONSTRAINT fk_user_profiles_institution FOREIGN KEY(institution_id) REFERENCES institutions(id) ON DELETE RESTRICT,
 ADD CONSTRAINT fk_user_profiles_faculty FOREIGN KEY(faculty_id,institution_id) REFERENCES faculties(id,institution_id) ON DELETE RESTRICT,
 ADD CONSTRAINT ck_user_profiles_faculty_school CHECK(faculty_id IS NULL OR institution_id IS NOT NULL),
 ADD INDEX ix_user_profiles_organization(institution_id,faculty_id);
ALTER TABLE documents
 ADD COLUMN institution_id BIGINT NULL,
 ADD COLUMN faculty_id BIGINT NULL,
 ADD CONSTRAINT fk_documents_institution FOREIGN KEY(institution_id) REFERENCES institutions(id) ON DELETE RESTRICT,
 ADD CONSTRAINT fk_documents_faculty FOREIGN KEY(faculty_id,institution_id) REFERENCES faculties(id,institution_id) ON DELETE RESTRICT,
 ADD CONSTRAINT ck_documents_faculty_school CHECK(faculty_id IS NULL OR institution_id IS NOT NULL),
 ADD INDEX ix_documents_organization(institution_id,faculty_id);
ALTER TABLE posts
 ADD COLUMN institution_id BIGINT NULL,
 ADD COLUMN faculty_id BIGINT NULL,
 ADD CONSTRAINT fk_posts_institution FOREIGN KEY(institution_id) REFERENCES institutions(id) ON DELETE RESTRICT,
 ADD CONSTRAINT fk_posts_faculty FOREIGN KEY(faculty_id,institution_id) REFERENCES faculties(id,institution_id) ON DELETE RESTRICT,
 ADD CONSTRAINT ck_posts_faculty_school CHECK(faculty_id IS NULL OR institution_id IS NOT NULL),
 ADD INDEX ix_posts_organization(institution_id,faculty_id);

-- Starter directory using the institution names supplied in the project requirements.
INSERT INTO institutions(name,type,created_at,updated_at,version) VALUES
('Đại học Bách Khoa Hà Nội','UNIVERSITY',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
('Đại học FPT','UNIVERSITY',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
('Khác','OTHER',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0);
INSERT INTO faculties(institution_id,name,created_at,updated_at,version)
SELECT id,'Khoa CNTT',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM institutions WHERE type='UNIVERSITY';
INSERT INTO faculties(institution_id,name,created_at,updated_at,version)
SELECT id,'Khoa Kinh tế',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0 FROM institutions WHERE type='UNIVERSITY';


-- Sprint 2 additions (V10); standalone snapshot only.
-- Sprint 2: review workflow, subjects, tags and full-text search.
CREATE TABLE subjects (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 slug VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 CONSTRAINT uk_subjects_name UNIQUE(name), CONSTRAINT uk_subjects_slug UNIQUE(slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE documents DROP CHECK ck_documents_2;
ALTER TABLE documents ADD CONSTRAINT ck_documents_2 CHECK (status IN ('DRAFT','PENDING','PUBLISHED','REJECTED','ARCHIVED','DELETED'));
ALTER TABLE documents ADD COLUMN subject_id BIGINT NULL,
 ADD COLUMN review_note TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
 ADD COLUMN reviewed_at DATETIME(6) NULL,
 ADD COLUMN reviewed_by_id BIGINT NULL,
 ADD CONSTRAINT fk_documents_subject FOREIGN KEY(subject_id) REFERENCES subjects(id) ON DELETE RESTRICT,
 ADD CONSTRAINT fk_documents_reviewer FOREIGN KEY(reviewed_by_id) REFERENCES users(id) ON DELETE RESTRICT,
 ADD INDEX ix_documents_author_status(author_id,status),
 ADD INDEX ix_documents_subject(subject_id,status),
 ADD FULLTEXT INDEX ft_documents_content(title,summary,description);
CREATE TABLE document_tags (
 document_id BIGINT NOT NULL, tag_id BIGINT NOT NULL,
 PRIMARY KEY(document_id,tag_id),
 CONSTRAINT fk_document_tags_document FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE,
 CONSTRAINT fk_document_tags_tag FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO subjects(name,slug,created_at,updated_at,version) VALUES
 ('Lập trình Java','lap-trinh-java',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
 ('Cơ sở dữ liệu','co-so-du-lieu',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
 ('Học máy','hoc-may',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
 ('Công nghệ phần mềm','cong-nghe-phan-mem',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0);
