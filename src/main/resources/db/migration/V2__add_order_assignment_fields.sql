-- Thêm các cột phân bổ và quản lý phiên bản cho bảng ORDER
ALTER TABLE `order` ADD COLUMN `assigned_to` INT NULL;
ALTER TABLE `order` ADD COLUMN `assigned_at` DATETIME NULL;
ALTER TABLE `order` ADD COLUMN `version` INT NOT NULL DEFAULT 0;

-- Tạo khóa ngoại liên kết cột assigned_to với bảng USER
ALTER TABLE `order` 
ADD CONSTRAINT `fk_order_assigned_moderator` 
FOREIGN KEY (`assigned_to`) REFERENCES `user` (`UserID`) 
ON DELETE SET NULL;
