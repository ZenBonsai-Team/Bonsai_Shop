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
ALTER TABLE viewing_appointment
DROP FOREIGN KEY fk_va_product;
END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'viewing_appointment'
          AND column_name = 'ProductID'
    ) THEN
ALTER TABLE viewing_appointment
DROP COLUMN ProductID;
END IF;
END //
DELIMITER ;

CALL drop_viewing_appointment_product_id();
DROP PROCEDURE IF EXISTS drop_viewing_appointment_product_id;


CREATE TABLE IF NOT EXISTS appointment_setting
(
    SettingID           INT NOT NULL AUTO_INCREMENT,

    AutoApprove         TINYINT(1) NOT NULL DEFAULT 1,
    AutoApproveAfter    INT NOT NULL DEFAULT 5,

    AutoComplete        TINYINT(1) NOT NULL DEFAULT 1,
    AutoCompleteAfter   INT NOT NULL DEFAULT 60,

    PauseFrom           DATETIME DEFAULT NULL,
    PauseTo             DATETIME DEFAULT NULL,
    PauseReason         TEXT NULL,

    UpdatedBy           INT DEFAULT NULL,
    UpdatedAt           DATETIME DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (SettingID),

    KEY fk_as_user (UpdatedBy),

    CONSTRAINT fk_as_user
    FOREIGN KEY (UpdatedBy)
    REFERENCES user(UserID)
    ON DELETE SET NULL
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;


DROP PROCEDURE IF EXISTS update_appointment_setting_columns;

DELIMITER //
CREATE PROCEDURE update_appointment_setting_columns()
BEGIN

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'appointment_setting'
          AND column_name = 'AutoApprove'
    ) THEN
ALTER TABLE appointment_setting
    ADD COLUMN AutoApprove TINYINT(1) NOT NULL DEFAULT 1
            AFTER SettingID;
END IF;


    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'appointment_setting'
          AND column_name = 'AutoApproveAfter'
    ) THEN
ALTER TABLE appointment_setting
    ADD COLUMN AutoApproveAfter INT NOT NULL DEFAULT 5
    AFTER AutoApprove;
END IF;


    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'appointment_setting'
          AND column_name = 'AutoComplete'
    ) THEN
ALTER TABLE appointment_setting
    ADD COLUMN AutoComplete TINYINT(1) NOT NULL DEFAULT 1
            AFTER PauseReason;
END IF;


    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'appointment_setting'
          AND column_name = 'AutoCompleteAfter'
    ) THEN
ALTER TABLE appointment_setting
    ADD COLUMN AutoCompleteAfter INT NOT NULL DEFAULT 60
    AFTER AutoComplete;
END IF;


    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'appointment_setting'
          AND column_name = 'PauseReason'
    ) THEN
ALTER TABLE appointment_setting
    ADD COLUMN PauseReason TEXT NULL
            AFTER PauseTo;
END IF;

END //
DELIMITER ;

CALL update_appointment_setting_columns();
DROP PROCEDURE IF EXISTS update_appointment_setting_columns;


UPDATE appointment_setting
SET
    AutoApprove       = COALESCE(AutoApprove,1),
    AutoApproveAfter  = COALESCE(AutoApproveAfter,5),
    AutoComplete      = COALESCE(AutoComplete,1),
    AutoCompleteAfter = COALESCE(AutoCompleteAfter,60);


INSERT INTO appointment_setting
(
    AutoApprove,
    AutoApproveAfter,
    AutoComplete,
    AutoCompleteAfter
)
SELECT
    1,
    5,
    1,
    60
    WHERE NOT EXISTS
(
    SELECT 1
    FROM appointment_setting
);