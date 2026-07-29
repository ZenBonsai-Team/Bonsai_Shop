-- V3: Refactor Payment table — chuyển từ quan hệ 1-1 sang 1-N với Order
-- Lý do: Business rule mới yêu cầu 1 Order có thể có nhiều Payment records
--   (Payment #1: DEPOSIT qua VNPay / Payment #2: REMAINING_PAYMENT bằng tiền mặt)
-- An toàn: không xóa dữ liệu, không thay đổi kiểu cột, data cũ vẫn valid

-- 1. Bỏ UNIQUE constraint trên OrderID trong bảng payment
--    (MySQL đặt tên index trùng với tên cột khi dùng `NOT NULL UNIQUE`. 
--    Ta cần tạm thời drop foreign key trước khi drop index này).
ALTER TABLE `payment` DROP FOREIGN KEY `fk_pay_order`;
ALTER TABLE `payment` DROP INDEX `OrderID`;

-- 2. Thêm lại foreign key và index thường để query theo OrderID vẫn nhanh
ALTER TABLE `payment` ADD CONSTRAINT `fk_pay_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE;
ALTER TABLE `payment` ADD INDEX `idx_payment_order_id` (`OrderID`);

-- 3. Thêm cột Notes để Moderator ghi chú khi xác nhận thanh toán tiền mặt
ALTER TABLE `payment` ADD COLUMN `Notes` VARCHAR(500) DEFAULT NULL AFTER `PaymentDate`;

