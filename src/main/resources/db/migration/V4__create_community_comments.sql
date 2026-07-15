USE `bonsai_shop`;

-- Thêm cột Status cho bài đăng (APPROVED: được duyệt/hiển thị, HIDDEN: ẩn bài)
ALTER TABLE `community_post` ADD COLUMN `Status` varchar(50) DEFAULT 'APPROVED';

-- Tạo bảng lưu trữ bình luận bài viết
CREATE TABLE IF NOT EXISTS `community_comment` (
  `CommentID` int NOT NULL AUTO_INCREMENT,
  `PostID` int NOT NULL,
  `AuthorName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `AuthorAvatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`CommentID`),
  CONSTRAINT `fk_comment_post` FOREIGN KEY (`PostID`) REFERENCES `community_post` (`PostID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm bình luận mẫu cho bài viết 1
INSERT INTO `community_comment` (`PostID`, `AuthorName`, `AuthorAvatar`, `Content`, `CreatedAt`) VALUES
(1, 'Hoàng Long', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=100', 'Bài viết rất ý nghĩa và sâu sắc ạ! Triết lý chơi cây của người Việt mình đúng thật là rèn chữ Nhẫn và giữ lòng thanh thản.', NOW() - INTERVAL 2 HOUR),
(1, 'Khánh Huyền', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=100', 'Bác Thành viết bài chia sẻ kỹ thuật đi kẽm uốn cây Tùng La Hán kỹ hơn được không ạ? Em rất thích dáng cây nhà bác.', NOW() - INTERVAL 5 HOUR),
(1, 'Trần Hải', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=100', 'Gốc cây trong ảnh bìa uốn đẹp quá, nét thân chi tiết nhìn rất có chiều sâu thời gian.', NOW() - INTERVAL 1 DAY);
