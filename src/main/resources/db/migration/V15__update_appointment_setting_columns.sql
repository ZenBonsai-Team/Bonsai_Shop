DROP PROCEDURE IF EXISTS update_appointment_setting_v15;

DELIMITER //

CREATE PROCEDURE update_appointment_setting_v15()
BEGIN

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'appointment_setting'
          AND column_name = 'AutoApprove'
    ) THEN
        ALTER TABLE appointment_setting
            ADD COLUMN AutoApprove TINYINT(1) NOT NULL DEFAULT 1;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'appointment_setting'
          AND column_name = 'AutoApproveAfter'
    ) THEN
        ALTER TABLE appointment_setting
            ADD COLUMN AutoApproveAfter INT NOT NULL DEFAULT 5;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'appointment_setting'
          AND column_name = 'AutoCompleteAfter'
    ) THEN
        ALTER TABLE appointment_setting
            ADD COLUMN AutoCompleteAfter INT NOT NULL DEFAULT 60;
    END IF;

END //

DELIMITER ;

CALL update_appointment_setting_v15();
DROP PROCEDURE IF EXISTS update_appointment_setting_v15;

UPDATE appointment_setting
SET
    AutoApprove = COALESCE(AutoApprove, 1),
    AutoApproveAfter = COALESCE(AutoApproveAfter, 5),
    AutoCompleteAfter = COALESCE(AutoCompleteAfter, 60);
