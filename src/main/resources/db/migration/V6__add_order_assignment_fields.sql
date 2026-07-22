DROP PROCEDURE IF EXISTS add_order_assignment_cols;

DELIMITER //
CREATE PROCEDURE add_order_assignment_cols()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = DATABASE() AND table_name = 'order' AND column_name = 'assigned_to'
    ) THEN
        ALTER TABLE `order` ADD COLUMN `assigned_to` INT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = DATABASE() AND table_name = 'order' AND column_name = 'assigned_at'
    ) THEN
        ALTER TABLE `order` ADD COLUMN `assigned_at` DATETIME NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = DATABASE() AND table_name = 'order' AND column_name = 'version'
    ) THEN
        ALTER TABLE `order` ADD COLUMN `version` INT NOT NULL DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_schema = DATABASE() AND constraint_name = 'fk_order_assigned_moderator'
    ) THEN
        ALTER TABLE `order` ADD CONSTRAINT `fk_order_assigned_moderator` FOREIGN KEY (`assigned_to`) REFERENCES `user` (`UserID`) ON DELETE SET NULL;
    END IF;
END //
DELIMITER ;

CALL add_order_assignment_cols();
DROP PROCEDURE IF EXISTS add_order_assignment_cols;
