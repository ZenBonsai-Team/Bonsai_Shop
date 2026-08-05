DROP PROCEDURE IF EXISTS AddProductVisibility;

DELIMITER //

CREATE PROCEDURE AddProductVisibility()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'product'
          AND column_name = 'IsVisible'
    ) THEN
        ALTER TABLE `PRODUCT`
          ADD COLUMN `IsVisible` boolean DEFAULT true;
    END IF;
END //

DELIMITER ;

CALL AddProductVisibility();
DROP PROCEDURE AddProductVisibility;

UPDATE `PRODUCT`
SET `IsVisible` = false
WHERE `ProductStatus` = 'HIDDEN';
