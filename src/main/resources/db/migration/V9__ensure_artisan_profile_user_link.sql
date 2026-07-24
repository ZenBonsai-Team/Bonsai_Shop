-- Ensure every ARTISAN account has an artisan_profile row.
INSERT INTO `artisan_profile` (`UserID`, `FullName`, `Bio`, `YearsOfExperience`, `Specialty`)
SELECT u.`UserID`, u.`FullName`, '', 0, ''
FROM `user` u
JOIN `role` r ON r.`RoleID` = u.`RoleID`
WHERE UPPER(r.`RoleName`) IN ('ARTISAN', 'ROLE_ARTISAN')
  AND NOT EXISTS (
      SELECT 1
      FROM `artisan_profile` ap
      WHERE ap.`UserID` = u.`UserID`
  );

-- Enforce one artisan_profile per user where UserID is present.
DROP PROCEDURE IF EXISTS add_artisan_profile_user_unique_key;

DELIMITER //
CREATE PROCEDURE add_artisan_profile_user_unique_key()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'artisan_profile'
          AND index_name = 'uq_artisan_profile_user_id'
    ) THEN
        ALTER TABLE `artisan_profile`
            ADD UNIQUE KEY `uq_artisan_profile_user_id` (`UserID`);
    END IF;
END //
DELIMITER ;

CALL add_artisan_profile_user_unique_key();
DROP PROCEDURE IF EXISTS add_artisan_profile_user_unique_key;
