DROP PROCEDURE IF EXISTS add_order_type_col;

DELIMITER //
CREATE PROCEDURE add_order_type_col()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = DATABASE() AND table_name = 'order' AND column_name = 'OrderType'
    ) THEN
        ALTER TABLE `order` ADD COLUMN `OrderType` VARCHAR(50) NOT NULL DEFAULT 'ONLINE';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics 
        WHERE table_schema = DATABASE() AND table_name = 'order' AND index_name = 'idx_order_order_type'
    ) THEN
        CREATE INDEX `idx_order_order_type` ON `order` (`OrderType`);
    END IF;
END //
DELIMITER ;

CALL add_order_type_col();
DROP PROCEDURE IF EXISTS add_order_type_col;

UPDATE `order` SET `OrderType` = 'ONLINE' WHERE `OrderType` IS NULL OR `OrderType` = '';
