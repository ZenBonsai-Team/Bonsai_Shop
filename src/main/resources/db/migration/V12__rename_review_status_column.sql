-- V12: Ensure ReviewStatus column exists with correct name.
-- Kept idempotent for environments that may have run older review migrations.

DROP PROCEDURE IF EXISTS EnsureReviewStatusColumn;

DELIMITER //

CREATE PROCEDURE EnsureReviewStatusColumn()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review'
      AND column_name = 'ReviewStatus'
  ) THEN
    ALTER TABLE `review`
      ADD COLUMN `ReviewStatus` VARCHAR(20) NOT NULL DEFAULT 'PENDING';
  END IF;
END //

DELIMITER ;

CALL EnsureReviewStatusColumn();
DROP PROCEDURE EnsureReviewStatusColumn;
