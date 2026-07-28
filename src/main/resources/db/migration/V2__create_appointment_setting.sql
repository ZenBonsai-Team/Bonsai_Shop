DROP PROCEDURE IF EXISTS drop_viewing_appointment_product_id;

DELIMITER //
CREATE PROCEDURE drop_viewing_appointment_product_id()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'viewing_appointment'
          AND constraint_name = 'fk_va_product'
    ) THEN
        ALTER TABLE `viewing_appointment` DROP FOREIGN KEY `fk_va_product`;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'viewing_appointment'
          AND column_name = 'ProductID'
    ) THEN
        ALTER TABLE `viewing_appointment` DROP COLUMN `ProductID`;
    END IF;
END //
DELIMITER ;

CALL drop_viewing_appointment_product_id();
DROP PROCEDURE IF EXISTS drop_viewing_appointment_product_id;

CREATE TABLE IF NOT EXISTS `appointment_setting` (
  `SettingID` int NOT NULL AUTO_INCREMENT,
  `PauseFrom` datetime DEFAULT NULL,
  `PauseTo` datetime DEFAULT NULL,
  `AutoComplete` tinyint(1) NOT NULL DEFAULT 1,
  `UpdatedBy` int DEFAULT NULL,
  `UpdatedAt` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`SettingID`),
  KEY `fk_as_user` (`UpdatedBy`),
  CONSTRAINT `fk_as_user`
    FOREIGN KEY (`UpdatedBy`) REFERENCES `user` (`UserID`)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO appointment_setting
(
    AutoComplete
)
SELECT
    1
    WHERE NOT EXISTS
(
    SELECT 1
    FROM appointment_setting
);
ALTER TABLE appointment_setting
    ADD COLUMN PauseReason TEXT NULL;
