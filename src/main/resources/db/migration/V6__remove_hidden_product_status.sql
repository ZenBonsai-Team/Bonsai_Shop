UPDATE `PRODUCT`
SET `ProductStatus` = 'AVAILABLE',
    `IsVisible` = false
WHERE `ProductStatus` = 'HIDDEN';
