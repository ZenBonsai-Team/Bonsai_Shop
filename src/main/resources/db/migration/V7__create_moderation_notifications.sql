USE `bonsai_shop`;

CREATE TABLE IF NOT EXISTS `moderation_notification` (
  `NotificationID` int NOT NULL AUTO_INCREMENT,
  `TargetUsername` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `IsRead` boolean DEFAULT FALSE,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`NotificationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
