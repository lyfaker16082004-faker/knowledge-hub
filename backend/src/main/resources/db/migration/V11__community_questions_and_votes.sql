-- Sprint 3: community Q&A feed.
-- post_type separates questions from knowledge articles; vote_value turns comment likes into up/down votes.
ALTER TABLE posts
 ADD COLUMN post_type VARCHAR(20) NOT NULL DEFAULT 'QUESTION',
 ADD CONSTRAINT ck_posts_type CHECK (post_type IN ('QUESTION','ARTICLE'));

ALTER TABLE comment_likes
 ADD COLUMN vote_value SMALLINT NOT NULL DEFAULT 1,
 ADD CONSTRAINT ck_comment_likes_vote CHECK (vote_value IN (-1,1));

CREATE INDEX ix_post_likes_post ON post_likes(post_id);
CREATE INDEX ix_bookmarks_post ON bookmarks(post_id);
