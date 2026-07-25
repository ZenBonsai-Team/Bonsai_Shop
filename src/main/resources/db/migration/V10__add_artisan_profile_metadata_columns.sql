-- Add lightweight metadata for artisan_profile.
DROP PROCEDURE IF EXISTS add_artisan_profile_metadata_columns;

DELIMITER //
CREATE PROCEDURE add_artisan_profile_metadata_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'artisan_profile'
          AND column_name = 'CoverImageUrl'
    ) THEN
        ALTER TABLE `artisan_profile`
            ADD COLUMN `CoverImageUrl` VARCHAR(500) NULL AFTER `Specialty`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'artisan_profile'
          AND column_name = 'CreatedAt'
    ) THEN
        ALTER TABLE `artisan_profile`
            ADD COLUMN `CreatedAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `CoverImageUrl`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'artisan_profile'
          AND column_name = 'UpdatedAt'
    ) THEN
        ALTER TABLE `artisan_profile`
            ADD COLUMN `UpdatedAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `CreatedAt`;
    END IF;
END //
DELIMITER ;

CALL add_artisan_profile_metadata_columns();
DROP PROCEDURE IF EXISTS add_artisan_profile_metadata_columns;
