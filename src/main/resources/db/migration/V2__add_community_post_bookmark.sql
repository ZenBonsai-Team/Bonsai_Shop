-- Flyway migration script V2: Add community_post_bookmark table
CREATE TABLE IF NOT EXISTS `community_post_bookmark` (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_post_user_bookmark` (`post_id`,`user_id`),
  CONSTRAINT `fk_bookmark_post` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`PostID`) ON DELETE CASCADE,
  CONSTRAINT `fk_bookmark_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
