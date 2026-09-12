-- 1. Enable pg_trgm extension for optimize search

CREATE EXTENSION IF NOT EXISTS pg_trgm SCHEMA public;

CREATE INDEX IF NOT EXISTS idx_tracks_title_trgm
ON tracks USING gin (lower(title) public.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_authors_name_trgm
ON authors USING gin (lower(name) public.gin_trgm_ops);
