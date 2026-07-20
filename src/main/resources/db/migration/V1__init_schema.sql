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

-- 6. Table structure for table `artisan_profile`
DROP TABLE IF EXISTS `artisan_profile`;
CREATE TABLE `artisan_profile` (
  `ArtisanID` int NOT NULL AUTO_INCREMENT,
  `UserID` int DEFAULT NULL,
  `FullName` varchar(255) NOT NULL,
  `Bio` text DEFAULT NULL,
  `YearsOfExperience` int DEFAULT NULL,
  `Specialty` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ArtisanID`),
  CONSTRAINT `fk_artisan_user` FOREIGN KEY (`UserID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Table structure for table `category`
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `CategoryID` int NOT NULL AUTO_INCREMENT,
  `CategoryName` varchar(255) NOT NULL,
  `Description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`CategoryID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Table structure for table `variety`
DROP TABLE IF EXISTS `variety`;
CREATE TABLE `variety` (
  `VarietyID` int NOT NULL AUTO_INCREMENT,
  `CategoryID` int NOT NULL,
  `VarietyName` varchar(255) NOT NULL,
  `Description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`VarietyID`),
  CONSTRAINT `fk_v_cat` FOREIGN KEY (`CategoryID`) REFERENCES `category` (`CategoryID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Table structure for table `product_segment`
DROP TABLE IF EXISTS `product_segment`;
CREATE TABLE `product_segment` (
  `SegmentID` int NOT NULL AUTO_INCREMENT,
  `SegmentName` varchar(100) NOT NULL,
  PRIMARY KEY (`SegmentID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Table structure for table `product`
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `ProductID` int NOT NULL AUTO_INCREMENT,
  `CreatedByID` int DEFAULT NULL,
  `ArtisanID` int DEFAULT NULL,
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
  CONSTRAINT `fk_p_artisan` FOREIGN KEY (`ArtisanID`) REFERENCES `artisan_profile` (`ArtisanID`) ON DELETE SET NULL,
  CONSTRAINT `fk_p_variety` FOREIGN KEY (`VarietyID`) REFERENCES `variety` (`VarietyID`),
  CONSTRAINT `fk_p_segment` FOREIGN KEY (`SegmentID`) REFERENCES `product_segment` (`SegmentID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Table structure for table `product_media`
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

-- 12. Table structure for table `tag`
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
  `TagID` int NOT NULL AUTO_INCREMENT,
  `TagName` varchar(255) NOT NULL,
  PRIMARY KEY (`TagID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Table structure for table `product_tag`
DROP TABLE IF EXISTS `product_tag`;
CREATE TABLE `product_tag` (
  `ProductID` int NOT NULL,
  `TagID` int NOT NULL,
  PRIMARY KEY (`ProductID`,`TagID`),
  CONSTRAINT `fk_pt_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE,
  CONSTRAINT `fk_pt_tag` FOREIGN KEY (`TagID`) REFERENCES `tag` (`TagID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Table structure for table `cart`
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `CartID` int NOT NULL AUTO_INCREMENT,
  `CustomerID` int NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`CartID`),
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Table structure for table `cart_item`
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item` (
  `CartItemID` int NOT NULL AUTO_INCREMENT,
  `CartID` int NOT NULL,
  `ProductID` int NOT NULL,
  PRIMARY KEY (`CartItemID`),
  CONSTRAINT `fk_ci_cart` FOREIGN KEY (`CartID`) REFERENCES `cart` (`CartID`) ON DELETE CASCADE,
  CONSTRAINT `fk_ci_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. Table structure for table `order`
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
  PRIMARY KEY (`OrderID`),
  CONSTRAINT `fk_o_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Table structure for table `order_detail`
DROP TABLE IF EXISTS `order_detail`;
CREATE TABLE `order_detail` (
  `OrderDetailID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL,
  `ProductID` int NOT NULL,
  `PriceAtPurchase` decimal(15,2) NOT NULL,
  PRIMARY KEY (`OrderDetailID`),
  CONSTRAINT `fk_od_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE,
  CONSTRAINT `fk_od_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. Table structure for table `order_handling`
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

-- 19. Table structure for table `order_log`
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

-- 20. Table structure for table `payment`
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

-- 21. Table structure for table `viewing_appointment`
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

-- 22. Table structure for table `review`
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

-- 23. Table structure for table `wishlist`
DROP TABLE IF EXISTS `wishlist`;
CREATE TABLE `wishlist` (
  `CustomerID` int NOT NULL,
  `ProductID` int NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`CustomerID`,`ProductID`),
  CONSTRAINT `fk_wl_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE,
  CONSTRAINT `fk_wl_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 24. Table structure for table `community_post`
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

-- 25. Table structure for table `community_comment`
DROP TABLE IF EXISTS `community_comment`;
CREATE TABLE `community_comment` (
  `CommentID` int NOT NULL AUTO_INCREMENT,
  `PostID` int NOT NULL,
  `UserID` int DEFAULT NULL,
  `AuthorName` varchar(255) NOT NULL,
  `AuthorAvatar` varchar(500) DEFAULT NULL,
  `Content` text NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`CommentID`),
  CONSTRAINT `fk_cc_post` FOREIGN KEY (`PostID`) REFERENCES `community_post` (`PostID`) ON DELETE CASCADE,
  CONSTRAINT `fk_cc_user` FOREIGN KEY (`UserID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 26. Table structure for table `community_post_like`
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

-- 26b. Table structure for table `community_post_bookmark`
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

-- Users (15 entries)
INSERT INTO `user` (UserID, RoleID, FullName, Username, Email, Password, Phone, Address, Status) VALUES 
(1, 2, 'Nguyễn Văn Chủ', 'owner', 'owner@bonsai.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0912345678', 'Hà Nội', 'ACTIVE'),
(2, 3, 'Thành Nghệ Nhân', 'artisan_thanh', 'thanh@art.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0988111222', 'Nam Định', 'ACTIVE'),
(3, 3, 'Duy Nghệ Nhân', 'artisan_duy', 'duy@art.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0988333444', 'Hải Phòng', 'ACTIVE'),
(4, 4, 'Trần Kiểm Đơn', 'moderator_ship', 'ship@bonsai.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0333222111', 'Hà Nội', 'ACTIVE'),
(5, 5, 'Lê Kiểm Duyệt', 'moderator_content', 'content@bonsai.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0555666777', 'Hà Nội', 'ACTIVE'),
(6, 1, 'Khách 01', 'cust01', 'cust01@gmail.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0901000001', 'Hồ Chí Minh', 'ACTIVE'),
(7, 1, 'Khách 02', 'cust02', 'cust02@gmail.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0901000002', 'Đà Nẵng', 'ACTIVE'),
(8, 1, 'Khách 03', 'cust03', 'cust03@gmail.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0901000003', 'Cần Thơ', 'ACTIVE'),
(9, 1, 'Khách 04', 'cust04', 'cust04@gmail.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0901000004', 'Hải Phòng', 'ACTIVE'),
(10, 1, 'Khách 05', 'cust05', 'cust05@gmail.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0901000005', 'Bình Dương', 'ACTIVE'),
(11, 1, 'Khách 06', 'cust06', 'cust06@gmail.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0901000006', 'Đồng Nai', 'ACTIVE'),
(12, 1, 'Khách 07', 'cust07', 'cust07@gmail.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0901000007', 'Quảng Ninh', 'ACTIVE'),
(13, 1, 'Khách 08', 'cust08', 'cust08@gmail.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0901000008', 'Thanh Hóa', 'ACTIVE'),
(14, 1, 'Khách 09', 'cust09', 'cust09@gmail.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0901000009', 'Nghệ An', 'ACTIVE'),
(15, 1, 'Khách 10', 'cust10', 'cust10@gmail.com', '$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa', '0901000010', 'Huế', 'ACTIVE');

-- Artisan Profiles (10 entries)
INSERT INTO `artisan_profile` (ArtisanID, UserID, FullName, Bio, YearsOfExperience, Specialty) VALUES 
(1, 2, 'Nguyễn Thành', 'Chuyên gia Sanh cổ Nam Định', 40, 'Sanh Nam Điền'),
(2, 3, 'Trần Duy', 'Bậc thầy Tùng La Hán', 25, 'Tùng La Hán'),
(3, NULL, 'Lê Hoàn', 'Nghệ nhân tạo dáng Văn Nhân', 15, 'Bonsai Mini'),
(4, NULL, 'Phạm Quang', 'Chuyên gia Bonsai ôm đá', 20, 'Cây ký đá'),
(5, NULL, 'Vũ Tiệp', 'Lão nông yêu cây', 50, 'Đa, Đề'),
(6, NULL, 'Hoàng Thắng', 'Nghệ nhân trẻ triển vọng', 10, 'Linh Sam'),
(7, NULL, 'Đỗ Hùng', 'Chuyên Mai Chiếu Thủy', 30, 'Mai Chiếu Thủy'),
(8, NULL, 'Bùi Phái', 'Nghệ nhân uốn kẽm nghệ thuật', 22, 'Sanh Quê'),
(9, NULL, 'Lê Minh', 'Sáng tạo dáng Thác Đổ', 18, 'Cây dáng Huyền'),
(10, NULL, 'Ngô Vân', 'Chuyên gia tiểu cảnh', 12, 'Tiểu cảnh Bonsai');

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

-- Products (20 entries)
INSERT INTO `product` (ProductID, CreatedByID, ArtisanID, VarietyID, SegmentID, ProductCode, ProductName, Price, Age, Style, IsPublicPrice, ProductStatus, Description, Height, TrunkDiameter, ViewCount, TreeStory) VALUES 
(1, 1, 1, 1, 3, 'E-001', 'Sanh Nam Điền Long Đằng', 250000000.00, 50, 'Dáng Long', 0, 'AVAILABLE', 'Cây dáng Long oai vệ bệ rễ vững chãi.', 120, 15, 105, 'Cây báu vật của dòng họ Nguyễn...'),
(2, 1, 2, 3, 3, 'E-002', 'Tùng La Hán Tuyết Sơn', 450000000.00, 60, 'Dáng Trực', 0, 'AVAILABLE', 'Tùng lá nhỏ uốn nắn tinh xảo.', 160, 22, 94, 'Khai thác từ vùng núi cao phía Bắc...'),
(3, 2, 3, 5, 2, 'M-001', 'Mai Chiếu Thủy Bonsai', 12000000.00, 15, 'Dáng Bay', 1, 'AVAILABLE', 'Thân xù xì, hoa nở quanh năm rất thơm.', 45, 8, 120, 'Hoa nở trắng xóa thơm ngát...'),
(4, 2, 6, 6, 1, 'B-001', 'Si Nhật Mini Đặt Bàn', 850000.00, 5, 'Dáng Trực', 1, 'AVAILABLE', 'Nhỏ gọn thích hợp decor bàn làm việc.', 25, 3, 340, 'Phù hợp văn phòng hiện đại.'),
(5, 1, 4, 1, 3, 'E-003', 'Sanh Cổ Ôm Đá', 180000000.00, 45, 'Ký đá', 0, 'AVAILABLE', 'Bệ rễ ăn bám chặt vào đá san hô.', 110, 18, 55, 'Sự kết hợp giữa đá và rễ cây...'),
(6, 1, 1, 1, 3, 'E-004', 'Sanh Nam Điền Phu Thê', 320000000.00, 55, 'Phu Thê', 0, 'AVAILABLE', 'Hai thân quấn quýt hài hòa cân đối.', 130, 20, 78, 'Hai thân quấn quýt hài hòa...'),
(7, 2, 5, 2, 2, 'M-002', 'Sanh Quê Dáng Lão', 15000000.00, 30, 'Dáng Lão', 1, 'AVAILABLE', 'Gốc to bậm mốc thời gian.', 85, 12, 64, 'Gốc già nua gân guốc.'),
(8, 1, 2, 4, 3, 'E-005', 'Tùng Kim Cương Đại', 950000000.00, 100, 'Dáng Trực', 0, 'AVAILABLE', 'Tuyệt phẩm quý hiếm dành cho đại gia.', 210, 35, 150, 'Siêu phẩm trăm năm tuổi.'),
(9, 2, 3, 5, 1, 'B-002', 'Mai Chiếu Thủy Lá Trung', 2500000.00, 8, 'Văn Nhân', 1, 'AVAILABLE', 'Uốn nắn theo phong cách tối giản thanh tao.', 50, 4, 88, 'Nét vẽ thanh mảnh nghệ thuật.'),
(10, 2, 7, 1, 2, 'M-003', 'Sanh Nam Điền Tán Tròn', 45000000.00, 20, 'Dáng Trực', 1, 'AVAILABLE', 'Bông tán đều đặn đối xứng.', 95, 10, 45, 'Tán bông đều tản vân.'),
(11, 1, 8, 3, 3, 'E-006', 'Tùng La Hán Nghệ Thuật', 155000000.00, 35, 'Thác Đổ', 0, 'AVAILABLE', 'Đường thân bay lượn sà xuống chậu.', 70, 9, 72, 'Mềm mại như dòng suối.'),
(12, 2, 9, 6, 1, 'B-003', 'Si Nhật Ngũ Phúc', 3200000.00, 10, 'Ngũ Phúc', 1, 'AVAILABLE', 'Năm tán bông đẹp sum suê.', 35, 5, 110, 'Tượng trưng cho may mắn.'),
(13, 1, 10, 1, 3, 'E-007', 'Sanh Nam Điền Đại Thụ', 210000000.00, 48, 'Dáng Trực', 0, 'AVAILABLE', 'Gốc vững chãi bệ rễ móng cọp.', 150, 25, 60, 'Bệ rễ móng cọp trải rộng.'),
(14, 2, 3, 5, 2, 'M-004', 'Mai Chiếu Thủy Cổ', 22000000.00, 25, 'Dáng Xiên', 1, 'AVAILABLE', 'Lũa thép tự nhiên cực đẹp.', 65, 11, 49, 'Gốc bặm trợn rất có lực.'),
(15, 1, 4, 3, 3, 'E-008', 'Tùng Kim Cương Mini VIP', 85000000.00, 12, 'Dáng Bay', 0, 'AVAILABLE', 'Lá kim dày xanh mướt mịn màng.', 30, 4, 185, 'Hàng tuyển chọn showroom.'),
(16, 2, 6, 6, 1, 'B-004', 'Si Nhật Đậu Chậu Gốm', 1500000.00, 6, 'Dáng Trực', 1, 'AVAILABLE', 'Đã thuần chậu lâu năm cực khỏe.', 28, 3, 132, 'Lá nhỏ mướt xanh quanh năm.'),
(17, 1, 1, 1, 3, 'E-009', 'Sanh Cổ Ngũ Hành', 550000000.00, 75, 'Ngũ Hành', 0, 'AVAILABLE', 'Năm thân tượng trưng Kim Mộc Thủy Hỏa Thổ.', 180, 30, 99, 'Năm thân quần tụ uy nghi.'),
(18, 2, 8, 2, 2, 'M-005', 'Sanh Quê Ký Đá Thấm Thủy', 18000000.00, 18, 'Thác Đổ', 1, 'AVAILABLE', 'Rễ ăn bám bọc kín đá thấm thủy.', 60, 6, 80, 'Nét đá tự nhiên mọc rêu.'),
(19, 1, 2, 4, 3, 'E-010', 'Tùng Kim Cương Dáng Huyền', 135000000.00, 22, 'Dáng Huyền', 0, 'AVAILABLE', 'Khúc uốn ngặt nghèo đầy nghệ thuật.', 80, 7, 142, 'Đường thân uốn lượn như rồng.'),
(20, 2, 9, 5, 1, 'B-005', 'Mai Chiếu Thủy Độc Bản', 5500000.00, 14, 'Dáng Kỳ', 1, 'AVAILABLE', 'Dáng kỳ quái độc bản có 1 không 2.', 40, 5, 230, 'Dáng lạ cho người sưu tầm.');

-- Seed Product Media (front images baseline)
INSERT INTO `product_media` (ProductID, MediaURL, MediaType, SlotType, Caption, IsThumbnail, DisplayOrder) VALUES
(1, '/images/bonsai-1.png', 'IMAGE', 'FRONT', 'Mặt trước Sanh Nam Điền', 1, 0),
(2, '/images/bonsai-2.png', 'IMAGE', 'FRONT', 'Mặt trước Tùng Tuyết Sơn', 1, 0),
(3, '/images/bonsai-3.png', 'IMAGE', 'FRONT', 'Mai Chiếu Thủy Bonsai', 1, 0),
(4, '/images/default-bonsai.png', 'IMAGE', 'FRONT', 'Si Nhật Mini', 1, 0);

-- Community Posts (15 entries)
INSERT INTO `community_post` (PostID, AuthorID, Title, Content, Summary, AuthorName, AuthorAvatar, Category, ImageUrl, ReadTime, LikesCount, CommentsCount, Status) VALUES 
(1, 1, 'Triết lý Wabi-Sabi trong Bonsai', 'Vẻ đẹp của sự không hoàn hảo và dấu vết thời gian...', 'Vẻ đẹp của sự không hoàn hảo trong bonsai.', 'Nguyễn Văn Chủ', 'https://api.dicebear.com/7.x/adventurer/svg?seed=owner', 'Triết lý', '/images/bonsai-1.png', 5, 25, 2, 'APPROVED'),
(2, 2, 'Cách chăm Tùng mùa đông', 'Đừng để cây bị gió mùa Đông Bắc tạt trực tiếp...', 'Kinh nghiệm chăm sóc tùng mùa gió rét.', 'Thành Nghệ Nhân', 'https://api.dicebear.com/7.x/adventurer/svg?seed=thanh', 'Kỹ thuật', '/images/bonsai-2.png', 6, 12, 1, 'APPROVED'),
(3, 1, 'Sanh Nam Điền và Sanh Quê', 'Cách phân biệt hai dòng sanh phổ biến nhất...', 'Phân biệt lá và da sanh nam điền.', 'Nguyễn Văn Chủ', 'https://api.dicebear.com/7.x/adventurer/svg?seed=owner', 'Kinh nghiệm', '/images/default-bonsai.png', 4, 30, 0, 'APPROVED'),
(4, 6, 'Tôi đã mua cây đầu tiên thế nào', 'Chia sẻ của một người mới tập chơi...', 'Hành trình chọn mua bonsai ban đầu.', 'Khách 01', NULL, 'Góc chia sẻ', NULL, 3, 8, 0, 'APPROVED'),
(5, 1, 'Kỹ thuật ký đá cho cây Sanh', 'Hướng dẫn chọn đá thấm thủy và cách ép rễ...', 'Hướng dẫn chi tiết quy trình ký đá.', 'Nguyễn Văn Chủ', 'https://api.dicebear.com/7.x/adventurer/svg?seed=owner', 'Kỹ thuật', '/images/bonsai-3.png', 8, 45, 1, 'APPROVED'),
(6, 2, 'Tạo dáng Văn Nhân - Khó hay Dễ', 'Nét vẽ mảnh mai nhưng đầy khí chất...', 'Giới thiệu về dáng văn nhân cốt cách.', 'Thành Nghệ Nhân', 'https://api.dicebear.com/7.x/adventurer/svg?seed=thanh', 'Tạo dáng', NULL, 5, 18, 0, 'APPROVED'),
(7, 1, 'Phân bón hữu cơ cho Bonsai', 'Tại sao nên dùng bánh dầu thay vì hóa học...', 'Ưu điểm của phân hữu cơ chậm tan.', 'Nguyễn Văn Chủ', 'https://api.dicebear.com/7.x/adventurer/svg?seed=owner', 'Chăm sóc', NULL, 4, 22, 0, 'APPROVED'),
(8, 5, 'Bảo vệ tác quyền nghệ nhân', 'Cần minh bạch phả hệ cây để tránh hàng giả...', 'Minh bạch nguồn gốc nghệ nhân.', 'Lê Kiểm Duyệt', NULL, 'Nhận định', NULL, 7, 15, 0, 'APPROVED'),
(9, 1, 'Lịch sử Bonsai Việt Nam', 'Từ thú chơi cung đình đến phong trào bình dân...', 'Lịch sử phát triển nền nghệ thuật nước nhà.', 'Nguyễn Văn Chủ', 'https://api.dicebear.com/7.x/adventurer/svg?seed=owner', 'Lịch sử', NULL, 10, 50, 0, 'APPROVED'),
(10, 2, 'Cắt giật hay uốn kẽm?', 'Ưu và nhược điểm của từng phương pháp tạo tác...', 'So sánh uốn kẽm và cắt giật xương cây.', 'Thành Nghệ Nhân', 'https://api.dicebear.com/7.x/adventurer/svg?seed=thanh', 'Kỹ thuật', NULL, 6, 29, 0, 'APPROVED'),
(11, 6, 'Cảm ơn nhà vườn vì cây Si đẹp', 'Review trải nghiệm mua hàng tại BSMS...', 'Trải nghiệm dịch vụ tuyệt vời.', 'Khách 01', NULL, 'Góc chia sẻ', NULL, 3, 11, 0, 'APPROVED'),
(12, 1, 'Trưng bày Bonsai trong nhà cổ', 'Cách phối hợp không gian kiến trúc và cây cảnh...', 'Nghệ thuật trưng bày sảnh phòng.', 'Nguyễn Văn Chủ', 'https://api.dicebear.com/7.x/adventurer/svg?seed=owner', 'Không gian', NULL, 5, 16, 0, 'APPROVED'),
(13, 2, 'Xử lý cây bị úng rễ', 'Các bước cấp cứu khẩn cấp cho cây Bonsai...', 'Hướng dẫn xử lý bầu đất nghẹt nước.', 'Thành Nghệ Nhân', 'https://api.dicebear.com/7.x/adventurer/svg?seed=thanh', 'Cấp cứu cây', NULL, 6, 33, 0, 'APPROVED'),
(14, 1, 'Tâm thế người chơi cây', 'Chơi cây là rèn lòng thanh thản...', 'Sự tĩnh tại trong tâm hồn người chăm cây.', 'Nguyễn Văn Chủ', 'https://api.dicebear.com/7.x/adventurer/svg?seed=owner', 'Cảm nhận', NULL, 4, 40, 0, 'APPROVED'),
(15, 7, 'Hỏi về kỹ thuật tỉa lá Mai', 'Em mới chơi xin các bác chỉ giáo...', 'Hỏi đáp kỹ thuật nhặt lá mai Tết.', 'Khách 02', NULL, 'Hỏi đáp', NULL, 3, 5, 0, 'APPROVED');

-- Seed Community Comments
INSERT INTO `community_comment` (CommentID, PostID, UserID, AuthorName, AuthorAvatar, Content) VALUES
(1, 1, 2, 'Thành Nghệ Nhân', 'https://api.dicebear.com/7.x/adventurer/svg?seed=thanh', 'Bài viết rất có chiều sâu chiêm nghiệm!'),
(2, 1, 6, 'Khách 01', NULL, 'Cảm ơn chú đã chia sẻ tri thức bổ ích.'),
(3, 2, 6, 'Khách 01', NULL, 'Nhà em bị chết mất một cây tùng do gió bấc rồi, tiếc quá.'),
(4, 5, 7, 'Khách 02', NULL, 'Cách làm rễ ôm đá rất chi tiết, cảm ơn nhà vườn!');

-- Seed Community Post Likes
INSERT INTO `community_post_like` (post_id, user_id) VALUES
(1, 6), (1, 7), (2, 6);

-- Seed Orders (10 entries)
INSERT INTO `order` (OrderID, CustomerID, OrderCode, CustomerName, CustomerPhone, CustomerEmail, ShippingAddress, TotalAmount, DepositAmount, CraneFee, ShippingFee, OrderStatus, Notes) VALUES 
(1, 6, 'ORD-101', 'Khách 01', '0901000001', 'cust01@gmail.com', 'Hồ Chí Minh', 850000.00, 0.00, 0.00, 0.00, 'COMPLETED', 'Giao giờ hành chính'),
(2, 7, 'ORD-102', 'Khách 02', '0901000002', 'cust02@gmail.com', 'Đà Nẵng', 15000000.00, 0.00, 500000.00, 200000.00, 'SHIPPING', 'Cần bao bọc kỹ bầu đất'),
(3, 8, 'ORD-103', 'Khách 03', '0901000003', 'cust03@gmail.com', 'Cần Thơ', 2500000.00, 0.00, 0.00, 500000.00, 'PENDING', 'Giao cuối tuần'),
(4, 9, 'ORD-104', 'Khách 04', '0901000004', 'cust04@gmail.com', 'Hải Phòng', 3200000.00, 1000000.00, 0.00, 150000.00, 'APPROVED', 'Chuyển khoản cọc trước'),
(5, 10, 'ORD-105', 'Khách 05', '0901000005', 'cust05@gmail.com', 'Bình Dương', 1500000.00, 0.00, 0.00, 50000.00, 'COMPLETED', ''),
(6, 11, 'ORD-106', 'Khách 06', '0901000006', 'cust06@gmail.com', 'Đồng Nai', 18000000.00, 5000000.00, 800000.00, 300000.00, 'SHIPPING', 'Giao xe cẩu lớn'),
(7, 12, 'ORD-107', 'Khách 07', '0901000007', 'cust07@gmail.com', 'Quảng Ninh', 5500000.00, 0.00, 0.00, 250000.00, 'PENDING', ''),
(8, 13, 'ORD-108', 'Khách 08', '0901000008', 'cust08@gmail.com', 'Thanh Hóa', 4500000.00, 0.00, 0.00, 100000.00, 'APPROVED', 'Giao giờ tối'),
(9, 14, 'ORD-109', 'Khách 09', '0901000009', 'cust09@gmail.com', 'Nghệ An', 22000000.00, 10000000.00, 1000000.00, 400000.00, 'COMPLETED', 'Cọc trước 10tr'),
(10, 15, 'ORD-110', 'Khách 10', '0901000010', 'cust10@gmail.com', 'Huế', 8000000.00, 0.00, 200000.00, 200000.00, 'PENDING', 'Gọi trước khi giao');

-- Seed Order details
INSERT INTO `order_detail` (OrderID, ProductID, PriceAtPurchase) VALUES
(1, 4, 850000.00),
(2, 7, 15000000.00),
(3, 9, 2500000.00),
(4, 12, 3200000.00);

-- Seed Payments
INSERT INTO `payment` (OrderID, PaymentMethod, PaymentStatus, PaymentType, Amount, PaymentDate) VALUES
(1, 'VNPAY', 'SUCCESS', 'ONLINE', 850000.00, CURRENT_TIMESTAMP),
(5, 'COD', 'SUCCESS', 'CASH', 1500000.00, CURRENT_TIMESTAMP);

-- Seed Order Handling
INSERT INTO `order_handling` (OrderID, ModeratorOrderID, IsActive) VALUES
(2, 4, 1),
(6, 4, 1);

-- Seed Order Logs
INSERT INTO `order_log` (OrderID, ActionByID, ActionType, FromStatus, ToStatus) VALUES
(1, 4, 'DELIVER', 'APPROVED', 'COMPLETED'),
(5, 4, 'DELIVER', 'APPROVED', 'COMPLETED');

-- Seed Viewing Appointments
INSERT INTO `viewing_appointment` (CustomerID, ProductID, AppointmentDate, Note, Status) VALUES
(6, 1, '2026-07-20 09:00:00', 'Muốn xem thực tế gốc cây Sanh Long Đằng', 'PENDING'),
(7, 2, '2026-07-22 14:00:00', 'Xem tùng tuyết sơn cành bay', 'PENDING');

-- Seed Reviews
INSERT INTO `review` (ProductID, CustomerID, Rating, Comment) VALUES
(4, 6, 5, 'Cây để bàn rất đẹp và tươi tốt, đóng gói cẩn thận lắm ạ!'),
(7, 7, 4, 'Cây sanh uốn khá già dặn, giao hàng hơi lâu chút nhưng chấp nhận được.');

SET FOREIGN_KEY_CHECKS = 1;
