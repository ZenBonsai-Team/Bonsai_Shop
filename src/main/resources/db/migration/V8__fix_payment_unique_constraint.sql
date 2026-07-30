-- V8: Fix unique constraint and duplicate foreign key on payment table

DROP PROCEDURE IF EXISTS DropPaymentConstraints;

DELIMITER //

CREATE PROCEDURE DropPaymentConstraints()
BEGIN
    -- Drop foreign key FKlbegd2tftjus4wx4lehalq911 if exists
    IF EXISTS (
        SELECT * FROM information_schema.table_constraints 
        WHERE constraint_schema = DATABASE() 
          AND table_name = 'payment' 
          AND constraint_name = 'FKlbegd2tftjus4wx4lehalq911'
    ) THEN
        ALTER TABLE `payment` DROP FOREIGN KEY `FKlbegd2tftjus4wx4lehalq911`;
    END IF;

    -- Drop foreign key fk_pay_order if exists (to avoid duplicate cascade recreation)
    IF EXISTS (
        SELECT * FROM information_schema.table_constraints 
        WHERE constraint_schema = DATABASE() 
          AND table_name = 'payment' 
          AND constraint_name = 'fk_pay_order'
    ) THEN
        ALTER TABLE `payment` DROP FOREIGN KEY `fk_pay_order`;
    END IF;

    -- Drop unique index UK8ck2u498kfrj5wemsw02bik6a if exists
    IF EXISTS (
        SELECT * FROM information_schema.statistics 
        WHERE table_schema = DATABASE() 
          AND table_name = 'payment' 
          AND index_name = 'UK8ck2u498kfrj5wemsw02bik6a'
    ) THEN
        ALTER TABLE `payment` DROP INDEX `UK8ck2u498kfrj5wemsw02bik6a`;
    END IF;
    
    -- Drop any other index named OrderID if it somehow exists as unique
    IF EXISTS (
        SELECT * FROM information_schema.statistics 
        WHERE table_schema = DATABASE() 
          AND table_name = 'payment' 
          AND index_name = 'OrderID'
    ) THEN
        ALTER TABLE `payment` DROP INDEX `OrderID`;
    END IF;

    -- Re-add correct foreign key
    ALTER TABLE `payment` ADD CONSTRAINT `fk_pay_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE;
END //

DELIMITER ;

CALL DropPaymentConstraints();
DROP PROCEDURE DropPaymentConstraints;
