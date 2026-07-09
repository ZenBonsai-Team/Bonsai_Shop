-- Xóa các liên kết trước (FK constraints)
DELETE FROM order_items WHERE product_id IN (SELECT product_id FROM products WHERE product_code IN ('BON008', 'BON009'));
DELETE FROM product_medias WHERE product_id IN (SELECT product_id FROM products WHERE product_code IN ('BON008', 'BON009'));
DELETE FROM reviews WHERE product_id IN (SELECT product_id FROM products WHERE product_code IN ('BON008', 'BON009'));
DELETE FROM wishlists WHERE product_id IN (SELECT product_id FROM products WHERE product_code IN ('BON008', 'BON009'));

-- Xóa 2 sản phẩm BON008 và BON009
DELETE FROM products WHERE product_code IN ('BON008', 'BON009');
