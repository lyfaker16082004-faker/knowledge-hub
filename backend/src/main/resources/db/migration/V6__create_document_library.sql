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
