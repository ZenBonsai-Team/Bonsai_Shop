DROP PROCEDURE IF EXISTS EnsureOrderCompletedAtColumn;

DELIMITER //

CREATE PROCEDURE EnsureOrderCompletedAtColumn()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'order'
      AND column_name = 'CompletedAt'
  ) THEN
    ALTER TABLE `order`
      ADD COLUMN `CompletedAt` DATETIME NULL;
  END IF;
END //

DELIMITER ;

CALL EnsureOrderCompletedAtColumn();
DROP PROCEDURE EnsureOrderCompletedAtColumn;
