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
