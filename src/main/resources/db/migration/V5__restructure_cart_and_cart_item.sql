-- Restructure CART and CART_ITEM tables to enterprise standard:
-- 1. User/Customer (1) <---> (1) CART
-- 2. CART (1) <---> (N) CART_ITEM (No quantity column for unique bonsai items)

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
