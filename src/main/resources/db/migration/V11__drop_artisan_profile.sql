-- Remove artisan_profile after product ownership moved to PRODUCT.CreatedByID.
DROP PROCEDURE IF EXISTS drop_artisan_profile_if_exists;

DELIMITER //
CREATE PROCEDURE drop_artisan_profile_if_exists()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'product'
          AND constraint_name = 'fk_p_artisan'
    ) THEN
        ALTER TABLE `product` DROP FOREIGN KEY `fk_p_artisan`;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'product'
          AND column_name = 'ArtisanID'
    ) THEN
        ALTER TABLE `product` DROP COLUMN `ArtisanID`;
    END IF;

    DROP TABLE IF EXISTS `artisan_profile`;
END //
DELIMITER ;

CALL drop_artisan_profile_if_exists();
DROP PROCEDURE IF EXISTS drop_artisan_profile_if_exists;
