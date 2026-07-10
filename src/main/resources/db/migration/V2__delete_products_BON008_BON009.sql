-- Xóa các liên kết trước (FK constraints)
DELETE FROM order_detail WHERE ProductID IN (SELECT ProductID FROM product WHERE ProductCode IN ('BON008', 'BON009'));
DELETE FROM product_media WHERE ProductID IN (SELECT ProductID FROM product WHERE ProductCode IN ('BON008', 'BON009'));
DELETE FROM review WHERE ProductID IN (SELECT ProductID FROM product WHERE ProductCode IN ('BON008', 'BON009'));
DELETE FROM wishlist WHERE ProductID IN (SELECT ProductID FROM product WHERE ProductCode IN ('BON008', 'BON009'));

-- Xóa 2 sản phẩm BON008 và BON009
DELETE FROM product WHERE ProductCode IN ('BON008', 'BON009');
