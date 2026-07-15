-- V5: Create post_like table to track which users have liked which posts
CREATE TABLE IF NOT EXISTS community_post_like (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_post_user_like (post_id, user_id),
    CONSTRAINT fk_like_post FOREIGN KEY (post_id) REFERENCES community_post(PostID) ON DELETE CASCADE,
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES USER(UserID) ON DELETE CASCADE
);