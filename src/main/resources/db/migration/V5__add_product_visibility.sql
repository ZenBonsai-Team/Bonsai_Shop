ALTER TABLE `PRODUCT`
  ADD COLUMN `IsVisible` boolean DEFAULT true;

UPDATE `PRODUCT`
SET `IsVisible` = false
WHERE `ProductStatus` = 'HIDDEN';
