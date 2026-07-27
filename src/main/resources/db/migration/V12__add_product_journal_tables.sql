CREATE TABLE IF NOT EXISTS `product_journal_event` (
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

CREATE TABLE IF NOT EXISTS `product_journal_media` (
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
