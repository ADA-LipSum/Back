-- MySQL schema changes for community/blog/notice/poll support.
-- Apply once before running with spring.jpa.hibernate.ddl-auto=validate.

ALTER TABLE posts
    ADD COLUMN board_type VARCHAR(20) NULL,
    ADD COLUMN community_category VARCHAR(30) NULL,
    ADD COLUMN tech_sub_tag VARCHAR(20) NULL,
    ADD COLUMN thumbnail_image VARCHAR(455) NULL;

UPDATE posts
   SET board_type = 'COMMUNITY'
 WHERE board_type IS NULL;

CREATE TABLE IF NOT EXISTS polls (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_uuid VARCHAR(36) NOT NULL,
    question VARCHAR(100) NOT NULL,
    anonymous BIT NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    ended BIT NOT NULL,
    ended_notified BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_polls_post_uuid (post_uuid),
    CONSTRAINT fk_polls_post
        FOREIGN KEY (post_uuid) REFERENCES posts (post_uuid)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS poll_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    poll_id BIGINT NOT NULL,
    option_text VARCHAR(80) NOT NULL,
    vote_count INT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_poll_options_poll_id (poll_id),
    CONSTRAINT fk_poll_options_poll
        FOREIGN KEY (poll_id) REFERENCES polls (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS poll_votes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    poll_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    voter_uuid VARCHAR(36) NOT NULL,
    voter_name VARCHAR(20) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_poll_vote_user (poll_id, voter_uuid),
    KEY idx_poll_votes_option_id (option_id),
    CONSTRAINT fk_poll_votes_poll
        FOREIGN KEY (poll_id) REFERENCES polls (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_option
        FOREIGN KEY (option_id) REFERENCES poll_options (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_uuid VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(255) NOT NULL,
    post_uuid VARCHAR(36) NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_recipient_created (recipient_uuid, created_at),
    KEY idx_notifications_post_uuid (post_uuid)
);
