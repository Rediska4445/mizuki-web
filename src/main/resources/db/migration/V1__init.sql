-- Init version of database

-- users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- authors
CREATE TABLE authors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(64) NOT NULL UNIQUE,
    CONSTRAINT fk_authors_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

-- tracks
CREATE TABLE tracks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(128),
    name VARCHAR(255) NOT NULL,
    file_path VARCHAR(256) UNIQUE,
    picture_path VARCHAR(256),
    color VARCHAR(7),
    is_explicit BOOLEAN DEFAULT FALSE,
    likes_count BIGINT NOT NULL DEFAULT 0 CHECK (likes_count >= 0),
    plays_count BIGINT NOT NULL DEFAULT 0 CHECK (plays_count >= 0),
    description TEXT,
    duration BIGINT,
    user_id BIGINT,
    CONSTRAINT fk_tracks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

-- tracks.authors
CREATE TABLE tracks_authors (
    tracks_id BIGINT NOT NULL,
    authors_id BIGINT NOT NULL,
    PRIMARY KEY (tracks_id, authors_id),
    CONSTRAINT fk_tracks_authors_track FOREIGN KEY (tracks_id) REFERENCES tracks (id) ON DELETE CASCADE,
    CONSTRAINT fk_tracks_authors_author FOREIGN KEY (authors_id) REFERENCES authors (id) ON DELETE CASCADE
);
