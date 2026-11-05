-- Incremented when credentials, status or roles change; invalidates existing sessions.
ALTER TABLE users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
