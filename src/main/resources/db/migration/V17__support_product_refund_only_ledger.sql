-- V17: Ensure financial_ledger table supports PRODUCT_REFUND_ONLY ledger type and related indexes
-- Note: LedgerType is VARCHAR(50) from V9, which already accommodates 'PRODUCT_REFUND_ONLY' (19 chars).

DROP PROCEDURE IF EXISTS EnsureV17ProductRefundOnlyLedger;

DELIMITER //

CREATE PROCEDURE EnsureV17ProductRefundOnlyLedger()
BEGIN
  -- Verify index on LedgerType exists
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'financial_ledger'
      AND index_name = 'idx_fl_type'
  ) THEN
    ALTER TABLE `financial_ledger` ADD KEY `idx_fl_type` (`LedgerType`);
  END IF;
END //

DELIMITER ;

CALL EnsureV17ProductRefundOnlyLedger();
DROP PROCEDURE EnsureV17ProductRefundOnlyLedger;
