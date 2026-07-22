DROP TABLE IF EXISTS `cart_item`;
DROP TABLE IF EXISTS `cart`;

CREATE TABLE IF NOT EXISTS `cart` (
    `CartID` INT AUTO_INCREMENT PRIMARY KEY,
    `CustomerID` INT NOT NULL UNIQUE,
    `CreatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_cart_customer` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cart_item` (
    `CartItemID` INT AUTO_INCREMENT PRIMARY KEY,
    `CartID` INT NOT NULL,
    `ProductID` INT NOT NULL,
    `CreatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_cart_item_cart` FOREIGN KEY (`CartID`) REFERENCES `cart` (`CartID`) ON DELETE CASCADE,
    CONSTRAINT `fk_cart_item_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE,
    UNIQUE KEY `uk_cart_product` (`CartID`, `ProductID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS add_order_detail_quantity_col;

DELIMITER //
CREATE PROCEDURE add_order_detail_quantity_col()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = DATABASE() AND table_name = 'order_detail' AND column_name = 'quantity'
    ) THEN
        ALTER TABLE `order_detail` ADD COLUMN `quantity` INT NOT NULL DEFAULT 1;
    END IF;
END //
DELIMITER ;

CALL add_order_detail_quantity_col();
DROP PROCEDURE IF EXISTS add_order_detail_quantity_col;
