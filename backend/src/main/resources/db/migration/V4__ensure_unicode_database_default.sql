-- MySQL Unicode support: VARCHAR/TEXT + utf8mb4, not NVARCHAR (utf8mb3).
-- V1 already created every application table using utf8mb4_unicode_ci.
-- Set the current database default for future tables as well.
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;