USE `bonsai_shop`;

ALTER TABLE `order`
ADD COLUMN `CraneFee` decimal(15,2) DEFAULT '0.00' AFTER `OrderStatus`,
ADD COLUMN `ShippingFee` decimal(15,2) DEFAULT '0.00' AFTER `CraneFee`;
