CREATE TABLE IF NOT EXISTS `financial_ledger` (
  `FinancialLedgerID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL,
  `RelatedPaymentID` int DEFAULT NULL,
  `RecordedByID` int NOT NULL,
  `LedgerType` varchar(50) NOT NULL,
  `Amount` decimal(15,2) NOT NULL,
  `Direction` varchar(20) NOT NULL,
  `FaultParty` varchar(30) DEFAULT NULL,
  `Reason` varchar(1000) DEFAULT NULL,
  `EvidenceNote` varchar(1000) DEFAULT NULL,
  `ExternalReference` varchar(255) DEFAULT NULL,
  `LedgerStatus` varchar(30) NOT NULL DEFAULT 'RECORDED',
  `RecognizedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `CreatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `CompletedRevenueActiveKey` int GENERATED ALWAYS AS (
    CASE
      WHEN `LedgerType` = 'COMPLETED_ORDER_REVENUE' AND `LedgerStatus` = 'RECORDED'
      THEN `OrderID`
      ELSE NULL
    END
  ) STORED,
  PRIMARY KEY (`FinancialLedgerID`),
  UNIQUE KEY `uk_fl_completed_revenue_active` (`CompletedRevenueActiveKey`),
  KEY `idx_fl_order` (`OrderID`),
  KEY `idx_fl_payment` (`RelatedPaymentID`),
  KEY `idx_fl_recorded_by` (`RecordedByID`),
  KEY `idx_fl_type` (`LedgerType`),
  KEY `idx_fl_status` (`LedgerStatus`),
  KEY `idx_fl_recognized_at` (`RecognizedAt`),
  KEY `idx_fl_fault_party` (`FaultParty`),
  CONSTRAINT `fk_fl_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE,
  CONSTRAINT `fk_fl_payment` FOREIGN KEY (`RelatedPaymentID`) REFERENCES `payment` (`PaymentID`) ON DELETE SET NULL,
  CONSTRAINT `fk_fl_user` FOREIGN KEY (`RecordedByID`) REFERENCES `user` (`UserID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
