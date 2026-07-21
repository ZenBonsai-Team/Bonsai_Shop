ALTER TABLE `order`
    ADD COLUMN `OrderType` varchar(50) NOT NULL DEFAULT 'ONLINE' AFTER `OrderStatus`;

UPDATE `order`
SET `OrderType` = 'ONLINE'
WHERE `OrderType` IS NULL OR `OrderType` = '';

CREATE INDEX `idx_order_order_type` ON `order` (`OrderType`);
