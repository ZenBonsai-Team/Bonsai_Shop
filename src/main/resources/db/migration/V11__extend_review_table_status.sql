DROP PROCEDURE IF EXISTS EnsureReviewModerationColumns;

DELIMITER //

CREATE PROCEDURE EnsureReviewModerationColumns()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review'
      AND column_name = 'ReviewStatus'
  ) THEN
    ALTER TABLE `review`
      ADD COLUMN `ReviewStatus` VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER `Comment`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review'
      AND column_name = 'Response'
  ) THEN
    ALTER TABLE `review`
      ADD COLUMN `Response` VARCHAR(1000) DEFAULT NULL AFTER `ReviewStatus`;
  END IF;
END //

DELIMITER ;

CALL EnsureReviewModerationColumns();
DROP PROCEDURE EnsureReviewModerationColumns;
