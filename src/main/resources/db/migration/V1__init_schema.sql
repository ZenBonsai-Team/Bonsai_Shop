SET FOREIGN_KEY_CHECKS = 0;

-- 1. Table structure for table `role`
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `RoleID` int NOT NULL AUTO_INCREMENT,
  `RoleName` varchar(100) NOT NULL UNIQUE,
  `Description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`RoleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Table structure for table `bussiness_action`
DROP TABLE IF EXISTS `bussiness_action`;
CREATE TABLE `bussiness_action` (
  `ActionID` int NOT NULL AUTO_INCREMENT,
  `ActionCode` varchar(100) NOT NULL UNIQUE,
  `ActionName` varchar(255) NOT NULL,
  `Description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`ActionID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Table structure for table `role_action`
DROP TABLE IF EXISTS `role_action`;
CREATE TABLE `role_action` (
  `RoleID` int NOT NULL,
  `ActionID` int NOT NULL,
  `IsEnabled` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`RoleID`,`ActionID`),
  CONSTRAINT `fk_roleaction_action` FOREIGN KEY (`ActionID`) REFERENCES `bussiness_action` (`ActionID`) ON DELETE CASCADE,
  CONSTRAINT `fk_roleaction_role` FOREIGN KEY (`RoleID`) REFERENCES `role` (`RoleID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Table structure for table `user`
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `UserID` int NOT NULL AUTO_INCREMENT,
  `RoleID` int NOT NULL,
  `FullName` varchar(255) NOT NULL,
  `Username` varchar(255) DEFAULT NULL UNIQUE,
  `Email` varchar(255) NOT NULL UNIQUE,
  `Password` varchar(255) NOT NULL,
  `Phone` varchar(20) DEFAULT NULL,
  `Avatar` varchar(500) DEFAULT NULL,
  `avatar_public_id` varchar(255) DEFAULT NULL,
  `Address` varchar(500) DEFAULT NULL,
  `Status` varchar(50) DEFAULT 'ACTIVE',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`UserID`),
  CONSTRAINT `fk_u_role` FOREIGN KEY (`RoleID`) REFERENCES `role` (`RoleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Table structure for table `password_reset_otp`
DROP TABLE IF EXISTS `password_reset_otp`;
CREATE TABLE `password_reset_otp` (
  `OtpID` int NOT NULL AUTO_INCREMENT,
  `Email` varchar(255) NOT NULL,
  `OtpCode` varchar(6) NOT NULL,
  `ExpiredAt` datetime NOT NULL,
  `IsUsed` tinyint(1) DEFAULT '0',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`OtpID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Table structure for table `category`
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `CategoryID` int NOT NULL AUTO_INCREMENT,
  `CategoryName` varchar(255) NOT NULL,
  `Description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`CategoryID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Table structure for table `variety`
DROP TABLE IF EXISTS `variety`;
CREATE TABLE `variety` (
  `VarietyID` int NOT NULL AUTO_INCREMENT,
  `CategoryID` int NOT NULL,
  `VarietyName` varchar(255) NOT NULL,
  `Description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`VarietyID`),
  CONSTRAINT `fk_v_cat` FOREIGN KEY (`CategoryID`) REFERENCES `category` (`CategoryID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Table structure for table `product_segment`
DROP TABLE IF EXISTS `product_segment`;
CREATE TABLE `product_segment` (
  `SegmentID` int NOT NULL AUTO_INCREMENT,
  `SegmentName` varchar(100) NOT NULL,
  PRIMARY KEY (`SegmentID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Table structure for table `product`
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `ProductID` int NOT NULL AUTO_INCREMENT,
  `CreatedByID` int DEFAULT NULL,
  `VarietyID` int NOT NULL,
  `SegmentID` int NOT NULL,
  `ProductCode` varchar(100) NOT NULL UNIQUE,
  `ProductName` varchar(255) NOT NULL,
  `Price` decimal(15,2) NOT NULL,
  `Age` int DEFAULT NULL,
  `Style` varchar(100) DEFAULT NULL,
  `IsPublicPrice` tinyint(1) DEFAULT '1',
  `ProductStatus` varchar(50) DEFAULT 'AVAILABLE',
  `Description` text DEFAULT NULL,
  `Height` float DEFAULT NULL,
  `TrunkDiameter` float DEFAULT NULL,
  `ViewCount` int DEFAULT '0',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `TreeStory` text DEFAULT NULL,
  PRIMARY KEY (`ProductID`),
  CONSTRAINT `fk_p_createdby` FOREIGN KEY (`CreatedByID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL,
  CONSTRAINT `fk_p_variety` FOREIGN KEY (`VarietyID`) REFERENCES `variety` (`VarietyID`),
  CONSTRAINT `fk_p_segment` FOREIGN KEY (`SegmentID`) REFERENCES `product_segment` (`SegmentID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Table structure for table `product_media`
DROP TABLE IF EXISTS `product_media`;
CREATE TABLE `product_media` (
  `MediaID` int NOT NULL AUTO_INCREMENT,
  `ProductID` int NOT NULL,
  `MediaURL` varchar(500) NOT NULL,
  `MediaType` varchar(50) DEFAULT 'IMAGE',
  `SlotType` varchar(50) DEFAULT 'FRONT',
  `Caption` varchar(255) DEFAULT NULL,
  `IsThumbnail` tinyint(1) DEFAULT '1',
  `DisplayOrder` int DEFAULT '0',
  PRIMARY KEY (`MediaID`),
  CONSTRAINT `fk_pm_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Table structure for table `tag`
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
  `TagID` int NOT NULL AUTO_INCREMENT,
  `TagName` varchar(255) NOT NULL,
  PRIMARY KEY (`TagID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Table structure for table `product_tag`
DROP TABLE IF EXISTS `product_tag`;
CREATE TABLE `product_tag` (
  `ProductID` int NOT NULL,
  `TagID` int NOT NULL,
  PRIMARY KEY (`ProductID`,`TagID`),
  CONSTRAINT `fk_pt_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE,
  CONSTRAINT `fk_pt_tag` FOREIGN KEY (`TagID`) REFERENCES `tag` (`TagID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Table structure for table `cart`
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `CartID` INT AUTO_INCREMENT PRIMARY KEY,
  `CustomerID` INT NOT NULL UNIQUE,
  `CreatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_cart_customer` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Table structure for table `cart_item`
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item` (
  `CartItemID` INT AUTO_INCREMENT PRIMARY KEY,
  `CartID` INT NOT NULL,
  `ProductID` INT NOT NULL,
  `CreatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_cart_item_cart` FOREIGN KEY (`CartID`) REFERENCES `cart` (`CartID`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_item_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE,
  UNIQUE KEY `uk_cart_product` (`CartID`, `ProductID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Table structure for table `order`
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `OrderID` int NOT NULL AUTO_INCREMENT,
  `CustomerID` int DEFAULT NULL,
  `OrderCode` varchar(50) DEFAULT NULL UNIQUE,
  `CustomerName` varchar(255) DEFAULT NULL,
  `CustomerPhone` varchar(20) DEFAULT NULL,
  `CustomerEmail` varchar(255) DEFAULT NULL,
  `ShippingAddress` varchar(500) DEFAULT NULL,
  `TotalAmount` decimal(15,2) DEFAULT NULL,
  `DepositAmount` decimal(15,2) DEFAULT '0.00',
  `CraneFee` decimal(15,2) DEFAULT '0.00',
  `ShippingFee` decimal(15,2) DEFAULT '0.00',
  `OrderStatus` varchar(50) DEFAULT 'PENDING',
  `Notes` varchar(500) DEFAULT NULL,
  `OrderDate` datetime DEFAULT CURRENT_TIMESTAMP,
  `OrderType` varchar(50) NOT NULL DEFAULT 'ONLINE',
  `assigned_to` int DEFAULT NULL,
  `assigned_at` datetime DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`OrderID`),
  KEY `idx_order_order_type` (`OrderType`),
  CONSTRAINT `fk_o_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL,
  CONSTRAINT `fk_order_assigned_moderator` FOREIGN KEY (`assigned_to`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. Table structure for table `order_detail`
DROP TABLE IF EXISTS `order_detail`;
CREATE TABLE `order_detail` (
  `OrderDetailID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL,
  `ProductID` int NOT NULL,
  `PriceAtPurchase` decimal(15,2) NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`OrderDetailID`),
  CONSTRAINT `fk_od_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE,
  CONSTRAINT `fk_od_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Table structure for table `order_handling`
DROP TABLE IF EXISTS `order_handling`;
CREATE TABLE `order_handling` (
  `OrderHandlingID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL,
  `ModeratorOrderID` int DEFAULT NULL,
  `HandledAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `ReleasedAt` datetime DEFAULT NULL,
  `IsActive` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`OrderHandlingID`),
  CONSTRAINT `fk_oh_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE,
  CONSTRAINT `fk_oh_moderator` FOREIGN KEY (`ModeratorOrderID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. Table structure for table `order_log`
DROP TABLE IF EXISTS `order_log`;
CREATE TABLE `order_log` (
  `OrderLogID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL,
  `ActionByID` int NOT NULL,
  `ActionType` varchar(100) DEFAULT NULL,
  `FromStatus` varchar(50) DEFAULT NULL,
  `ToStatus` varchar(50) DEFAULT NULL,
  `ActionAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`OrderLogID`),
  CONSTRAINT `fk_ol_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE,
  CONSTRAINT `fk_ol_user` FOREIGN KEY (`ActionByID`) REFERENCES `user` (`UserID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. Table structure for table `payment`
DROP TABLE IF EXISTS `payment`;
CREATE TABLE `payment` (
  `PaymentID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL UNIQUE,
  `PaymentMethod` varchar(100) DEFAULT NULL,
  `PaymentStatus` varchar(50) DEFAULT 'PENDING',
  `PaymentType` varchar(100) DEFAULT NULL,
  `Amount` decimal(15,2) DEFAULT NULL,
  `PaymentDate` datetime DEFAULT NULL,
  PRIMARY KEY (`PaymentID`),
  CONSTRAINT `fk_pay_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. Table structure for table `viewing_appointment`
DROP TABLE IF EXISTS `viewing_appointment`;
CREATE TABLE `viewing_appointment` (
  `AppointmentID` int NOT NULL AUTO_INCREMENT,
  `CustomerID` int NOT NULL,
  `ProductID` int NOT NULL,
  `AppointmentDate` datetime NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `UpdatedAt` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `Note` varchar(500) DEFAULT NULL,
  `Status` varchar(50) DEFAULT 'PENDING',
  PRIMARY KEY (`AppointmentID`),
  CONSTRAINT `fk_va_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE,
  CONSTRAINT `fk_va_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 21. Table structure for table `review`
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
  `ReviewID` int NOT NULL AUTO_INCREMENT,
  `ProductID` int NOT NULL,
  `CustomerID` int NOT NULL,
  `Rating` int DEFAULT NULL,
  `Comment` varchar(1000) DEFAULT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ReviewID`),
  CONSTRAINT `fk_rev_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE,
  CONSTRAINT `fk_rev_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE,
  CONSTRAINT `review_chk_1` CHECK ((`Rating` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. Table structure for table `wishlist`
DROP TABLE IF EXISTS `wishlist`;
CREATE TABLE `wishlist` (
  `CustomerID` int NOT NULL,
  `ProductID` int NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`CustomerID`,`ProductID`),
  CONSTRAINT `fk_wl_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE,
  CONSTRAINT `fk_wl_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 23. Table structure for table `community_post`
DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post` (
  `PostID` int NOT NULL AUTO_INCREMENT,
  `AuthorID` int DEFAULT NULL,
  `Title` varchar(255) NOT NULL,
  `Content` text NOT NULL,
  `Summary` varchar(1000) DEFAULT NULL,
  `AuthorName` varchar(255) DEFAULT NULL,
  `AuthorAvatar` varchar(500) DEFAULT NULL,
  `Category` varchar(100) DEFAULT NULL,
  `ImageUrl` varchar(1000) DEFAULT NULL,
  `ReadTime` int DEFAULT '5',
  `LikesCount` int DEFAULT '0',
  `CommentsCount` int DEFAULT '0',
  `Status` varchar(50) DEFAULT 'APPROVED',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`PostID`),
  CONSTRAINT `fk_cp_author` FOREIGN KEY (`AuthorID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 24. Table structure for table `community_comment`
DROP TABLE IF EXISTS `community_comment`;
CREATE TABLE `community_comment` (
  `CommentID` int NOT NULL AUTO_INCREMENT,
  `PostID` int NOT NULL,
  `UserID` int DEFAULT NULL,
  `AuthorName` varchar(255) NOT NULL,
  `AuthorAvatar` varchar(500) DEFAULT NULL,
  `Content` text NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `ModerationReason` varchar(500) DEFAULT NULL,
  `Status` varchar(50) DEFAULT 'APPROVED',
  PRIMARY KEY (`CommentID`),
  CONSTRAINT `fk_cc_post` FOREIGN KEY (`PostID`) REFERENCES `community_post` (`PostID`) ON DELETE CASCADE,
  CONSTRAINT `fk_cc_user` FOREIGN KEY (`UserID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 25. Table structure for table `community_post_like`
DROP TABLE IF EXISTS `community_post_like`;
CREATE TABLE `community_post_like` (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_post_user_like` (`post_id`,`user_id`),
  CONSTRAINT `fk_like_post` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`PostID`) ON DELETE CASCADE,
  CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 26. Table structure for table `community_post_bookmark`
DROP TABLE IF EXISTS `community_post_bookmark`;
CREATE TABLE `community_post_bookmark` (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_post_user_bookmark` (`post_id`,`user_id`),
  CONSTRAINT `fk_bookmark_post` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`PostID`) ON DELETE CASCADE,
  CONSTRAINT `fk_bookmark_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 27. Table structure for table `moderation_notification`
DROP TABLE IF EXISTS `moderation_notification`;
CREATE TABLE `moderation_notification` (
  `NotificationID` int NOT NULL AUTO_INCREMENT,
  `TargetUsername` varchar(255) NOT NULL,
  `Message` varchar(1000) NOT NULL,
  `IsRead` boolean DEFAULT FALSE,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`NotificationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 28. Table structure for table `product_journal_event`
DROP TABLE IF EXISTS `product_journal_event`;
CREATE TABLE `product_journal_event` (
  `EventID` int NOT NULL AUTO_INCREMENT,
  `ProductID` int NOT NULL,
  `CreatedByID` int DEFAULT NULL,
  `EventDate` date NOT NULL,
  `EventType` varchar(50) NOT NULL DEFAULT 'PHOTO_UPDATE',
  `Title` varchar(255) NOT NULL,
  `Description` text DEFAULT NULL,
  `IsPublic` tinyint(1) DEFAULT '1',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `UpdatedAt` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`EventID`),
  KEY `idx_pje_product_date` (`ProductID`, `EventDate`),
  CONSTRAINT `fk_pje_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE,
  CONSTRAINT `fk_pje_created_by` FOREIGN KEY (`CreatedByID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 29. Table structure for table `product_journal_media`
DROP TABLE IF EXISTS `product_journal_media`;
CREATE TABLE `product_journal_media` (
  `MediaID` int NOT NULL AUTO_INCREMENT,
  `EventID` int NOT NULL,
  `MediaURL` varchar(500) NOT NULL,
  `MediaType` varchar(50) NOT NULL DEFAULT 'IMAGE',
  `Caption` varchar(255) DEFAULT NULL,
  `DisplayOrder` int DEFAULT '0',
  PRIMARY KEY (`MediaID`),
  KEY `idx_pjm_event_order` (`EventID`, `DisplayOrder`),
  CONSTRAINT `fk_pjm_event` FOREIGN KEY (`EventID`) REFERENCES `product_journal_event` (`EventID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------
-- Seed baseline data
-- --------------------------------------------------

-- Roles
INSERT INTO `role` (RoleID, RoleName, Description) VALUES 
(1,'ROLE_CUSTOMER','Khách'), 
(2,'ROLE_OWNER','Chủ'), 
(3,'ROLE_ARTISAN','Nghệ nhân'), 
(4,'ROLE_MODERATOR','Kiểm duyệt đơn'), 
(5,'ROLE_CONTENT_MODERATOR','Kiểm duyệt');

-- Business Actions
INSERT INTO `bussiness_action` (ActionID, ActionCode, ActionName, Description) VALUES
(1, 'ACTION_PRODUCT_MANAGE', 'Quản lý sản phẩm', 'Cho phép thêm, sửa, xóa sản phẩm'),
(2, 'ACTION_ORDER_VIEW_ALL', 'Xem tất cả đơn hàng', 'Cho phép xem tất cả đơn hàng hệ thống'),
(3, 'ACTION_ORDER_HANDLE_CLAIM', 'Xử lý khiếu nại đơn hàng', 'Cho phép xử lý khiếu nại của khách hàng'),
(4, 'ACTION_USER_MANAGE', 'Quản lý người dùng', 'Cho phép quản trị tài khoản người dùng');

-- Role Actions
INSERT INTO `role_action` (RoleID, ActionID, IsEnabled) VALUES
(2, 1, 1), (2, 2, 1), (2, 3, 1), (2, 4, 1), -- OWNER có toàn quyền
(3, 1, 1),                                   -- ARTISAN uốn cây kiêm bán có quyền quản lý sản phẩm
(4, 2, 1), (4, 3, 1);                         -- MODERATOR xem đơn và xử lý vận chuyển/khiếu nại

-- Users (Default administrative and staff accounts with password '123')
INSERT INTO `user` (UserID, RoleID, FullName, Username, Email, Password, Phone, Address, Status) VALUES 
(1, 2, 'Nguyễn Văn Chủ', 'owner', 'owner@bonsai.com', '$2a$10$BO1HYsR0U/pg9PpemLO7z.cX3jbPvH48cnJwCyH/VFts.BYLCpQtK', '0912345678', 'Hà Nội', 'ACTIVE'),
(2, 3, 'Thành Nghệ Nhân', 'artisan_thanh', 'thanh@art.com', '$2a$10$BO1HYsR0U/pg9PpemLO7z.cX3jbPvH48cnJwCyH/VFts.BYLCpQtK', '0988111222', 'Nam Định', 'ACTIVE'),
(3, 3, 'Duy Nghệ Nhân', 'artisan_duy', 'duy@art.com', '$2a$10$BO1HYsR0U/pg9PpemLO7z.cX3jbPvH48cnJwCyH/VFts.BYLCpQtK', '0988333444', 'Hải Phòng', 'ACTIVE'),
(4, 4, 'Trần Kiểm Đơn', 'moderator_order', 'order@bonsai.com', '$2a$10$BO1HYsR0U/pg9PpemLO7z.cX3jbPvH48cnJwCyH/VFts.BYLCpQtK', '0333222111', 'Hà Nội', 'ACTIVE'),
(5, 5, 'Lê Kiểm Duyệt', 'moderator_content', 'content@bonsai.com', '$2a$10$BO1HYsR0U/pg9PpemLO7z.cX3jbPvH48cnJwCyH/VFts.BYLCpQtK', '0555666777', 'Hà Nội', 'ACTIVE');

-- Categories
INSERT INTO `category` (CategoryID, CategoryName, Description) VALUES 
(1, 'Dòng Sanh', 'Cây Sanh cảnh nghệ thuật'), 
(2, 'Dòng Tùng', 'Cây Tùng cảnh truyền thống'), 
(3, 'Dòng Mai', 'Hoa Mai cảnh bonsai'), 
(4, 'Cây Mini', 'Bonsai kích thước nhỏ để bàn');

-- Varieties
INSERT INTO `variety` (VarietyID, CategoryID, VarietyName, Description) VALUES 
(1, 1, 'Sanh Nam Điền', 'Sanh Nam Điền lá bóng đẹp cổ kính'), 
(2, 1, 'Sanh Quê', 'Sanh Quê khỏe dễ uốn'), 
(3, 2, 'Tùng La Hán', 'Tùng Vạn Niên thanh lịch phong thủy'), 
(4, 2, 'Tùng Kim Cương', 'Dòng Tùng quý hiếm lá nhọn xanh mướt'), 
(5, 3, 'Mai Chiếu Thủy', 'Hoa thơm trắng tinh khiết phát lộc'), 
(6, 4, 'Si Nhật', 'Si Nhật Bonsai nhỏ nhắn xinh xắn');

-- Segments
INSERT INTO `product_segment` (SegmentID, SegmentName) VALUES 
(1, 'Budget'), 
(2, 'Mid'), 
(3, 'Elite');

SET FOREIGN_KEY_CHECKS = 1;
