INSERT INTO roles (name, description, created_at, updated_at, version) VALUES
('ADMIN', 'System administrator', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
('MODERATOR', 'Content moderator', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
('USER', 'Registered member', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0);
INSERT INTO permissions (code, description, created_at, updated_at, version) VALUES
('POST_CREATE', 'Create own posts', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
('POST_EDIT_OWN', 'Edit own posts', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
('COMMENT_CREATE', 'Create comments', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
('CONTENT_MODERATE', 'Moderate content and reports', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
('USER_MANAGE', 'Manage users and roles', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
('CATEGORY_MANAGE', 'Manage categories and tags', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
('AUDIT_READ', 'Read audit logs', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0);
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at, version)
SELECT r.id, p.id, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0 FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
OR (r.name = 'MODERATOR' AND p.code IN ('POST_CREATE','POST_EDIT_OWN','COMMENT_CREATE','CONTENT_MODERATE','CATEGORY_MANAGE'))
OR (r.name = 'USER' AND p.code IN ('POST_CREATE','POST_EDIT_OWN','COMMENT_CREATE'));
