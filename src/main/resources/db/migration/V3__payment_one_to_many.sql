-- V3: Refactor Payment table — chuyển từ quan hệ 1-1 sang 1-N với Order
-- Lý do: Business rule mới yêu cầu 1 Order có thể có nhiều Payment records
--   (Payment #1: DEPOSIT qua VNPay / Payment #2: REMAINING_PAYMENT bằng tiền mặt)
-- An toàn: không xóa dữ liệu, không thay đổi kiểu dữ liệu. Các thao tác drop/add
-- được bọc điều kiện để chạy được trên clean DB và schema dev đã bị Hibernate/Flyway
-- đồng bộ một phần.

DROP PROCEDURE IF EXISTS RefactorPaymentOneToMany;

DELIMITER //

CREATE PROCEDURE RefactorPaymentOneToMany()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'payment'
          AND constraint_name = 'fk_pay_order'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE `payment` DROP FOREIGN KEY `fk_pay_order`;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'payment'
          AND index_name = 'OrderID'
    ) THEN
        ALTER TABLE `payment` DROP INDEX `OrderID`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'payment'
          AND constraint_name = 'fk_pay_order'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE `payment`
            ADD CONSTRAINT `fk_pay_order`
            FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'payment'
          AND index_name = 'idx_payment_order_id'
    ) THEN
        ALTER TABLE `payment` ADD INDEX `idx_payment_order_id` (`OrderID`);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'payment'
          AND column_name = 'Notes'
    ) THEN
        ALTER TABLE `payment` ADD COLUMN `Notes` VARCHAR(500) DEFAULT NULL AFTER `PaymentDate`;
    END IF;
END //

DELIMITER ;

CALL RefactorPaymentOneToMany();
DROP PROCEDURE RefactorPaymentOneToMany;
