ALTER TABLE tracks
ADD CONSTRAINT chk_likes_count_positive
CHECK (likes_count >= 0);
