-- Remove out-of-scope features. Child tables are dropped before their parents.
-- Account sessions, document bookmarks and download history use separate retained tables.
DROP TABLE IF EXISTS moderation_actions;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS email_verification_tokens;
DROP TABLE IF EXISTS notification_preferences;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS collection_posts;
DROP TABLE IF EXISTS collections;
DROP TABLE IF EXISTS post_revisions;
