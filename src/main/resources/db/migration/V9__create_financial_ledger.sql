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
  KEY `idx_fl_fault_party` (`FaultParty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS EnsureFinancialLedgerSchema;

DELIMITER //

CREATE PROCEDURE EnsureFinancialLedgerSchema()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'financial_ledger'
      AND column_name = 'CompletedRevenueActiveKey'
  ) THEN
    ALTER TABLE `financial_ledger`
      ADD COLUMN `CompletedRevenueActiveKey` int GENERATED ALWAYS AS (
        CASE
          WHEN `LedgerType` = 'COMPLETED_ORDER_REVENUE' AND `LedgerStatus` = 'RECORDED'
          THEN `OrderID`
          ELSE NULL
        END
      ) STORED;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'financial_ledger'
      AND index_name = 'uk_fl_completed_revenue_active'
  ) THEN
    ALTER TABLE `financial_ledger`
      ADD UNIQUE KEY `uk_fl_completed_revenue_active` (`CompletedRevenueActiveKey`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'financial_ledger' AND index_name = 'idx_fl_order') THEN
    ALTER TABLE `financial_ledger` ADD KEY `idx_fl_order` (`OrderID`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'financial_ledger' AND index_name = 'idx_fl_payment') THEN
    ALTER TABLE `financial_ledger` ADD KEY `idx_fl_payment` (`RelatedPaymentID`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'financial_ledger' AND index_name = 'idx_fl_recorded_by') THEN
    ALTER TABLE `financial_ledger` ADD KEY `idx_fl_recorded_by` (`RecordedByID`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'financial_ledger' AND index_name = 'idx_fl_type') THEN
    ALTER TABLE `financial_ledger` ADD KEY `idx_fl_type` (`LedgerType`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'financial_ledger' AND index_name = 'idx_fl_status') THEN
    ALTER TABLE `financial_ledger` ADD KEY `idx_fl_status` (`LedgerStatus`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'financial_ledger' AND index_name = 'idx_fl_recognized_at') THEN
    ALTER TABLE `financial_ledger` ADD KEY `idx_fl_recognized_at` (`RecognizedAt`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'financial_ledger' AND index_name = 'idx_fl_fault_party') THEN
    ALTER TABLE `financial_ledger` ADD KEY `idx_fl_fault_party` (`FaultParty`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'financial_ledger'
      AND constraint_name = 'fk_fl_order'
      AND constraint_type = 'FOREIGN KEY'
  ) THEN
    ALTER TABLE `financial_ledger`
      ADD CONSTRAINT `fk_fl_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'financial_ledger'
      AND constraint_name = 'fk_fl_payment'
      AND constraint_type = 'FOREIGN KEY'
  ) THEN
    ALTER TABLE `financial_ledger`
      ADD CONSTRAINT `fk_fl_payment` FOREIGN KEY (`RelatedPaymentID`) REFERENCES `payment` (`PaymentID`) ON DELETE SET NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'financial_ledger'
      AND constraint_name = 'fk_fl_user'
      AND constraint_type = 'FOREIGN KEY'
  ) THEN
    ALTER TABLE `financial_ledger`
      ADD CONSTRAINT `fk_fl_user` FOREIGN KEY (`RecordedByID`) REFERENCES `user` (`UserID`);
  END IF;
END //

DELIMITER ;

CALL EnsureFinancialLedgerSchema();
DROP PROCEDURE EnsureFinancialLedgerSchema;
