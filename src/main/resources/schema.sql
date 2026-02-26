CREATE TABLE posts
(
    id          BIGINT PRIMARY KEY,
    title       TEXT NOT NULL,
    text        TEXT NOT NULL,
    likes_count INT DEFAULT 0
);

CREATE TABLE comments
(
    id      BIGINT PRIMARY KEY,
    text    TEXT NOT NULL,
    post_id BIGINT REFERENCES posts (id) ON DELETE CASCADE
);

CREATE TABLE tags
(
    id   BIGINT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE post_tags
(
    post_id BIGINT REFERENCES posts (id) ON DELETE CASCADE,
    tag_id  BIGINT REFERENCES tags (id) ON DELETE CASCADE
);