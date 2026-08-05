CREATE TABLE IF NOT EXISTS `live_session` (
  `SessionID` int NOT NULL AUTO_INCREMENT,
  `Title` varchar(255) NOT NULL,
  `StreamURL` varchar(500) DEFAULT NULL,
  `Status` varchar(50) NOT NULL DEFAULT 'ONGOING',
  `StartTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `EndTime` datetime DEFAULT NULL,
  PRIMARY KEY (`SessionID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `live_lead` (
  `LeadID` int NOT NULL AUTO_INCREMENT,
  `SessionID` int NOT NULL,
  `ProductID` int DEFAULT NULL,
  `ViewerName` varchar(150) NOT NULL,
  `PhoneNumber` varchar(20) DEFAULT NULL,
  `RawComment` text NOT NULL,
  `IntentType` varchar(50) NOT NULL,
  `LeadStatus` varchar(50) NOT NULL DEFAULT 'PENDING',
  `Notes` varchar(500) DEFAULT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `UpdatedAt` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`LeadID`),
  CONSTRAINT `fk_ll_session` FOREIGN KEY (`SessionID`) REFERENCES `live_session` (`SessionID`) ON DELETE CASCADE,
  CONSTRAINT `fk_ll_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
