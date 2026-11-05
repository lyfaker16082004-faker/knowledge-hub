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
