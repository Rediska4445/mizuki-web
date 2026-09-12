CREATE TABLE user_liked_tracks (
    user_id BIGINT NOT NULL,
    track_id BIGINT NOT NULL,

    CONSTRAINT pk_user_liked_tracks PRIMARY KEY (user_id, track_id),

    CONSTRAINT fk_user_id FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_track_id FOREIGN KEY (track_id)
        REFERENCES tracks(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_liked_tracks_user ON user_liked_tracks(user_id);
CREATE INDEX idx_user_liked_tracks_track ON user_liked_tracks(track_id);
