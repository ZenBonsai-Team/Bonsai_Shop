-- V12: Ensure ReviewStatus column exists with correct name
-- V11 already added ReviewStatus directly on some environments.
-- This migration is idempotent: adds ReviewStatus only if it does not exist yet.
ALTER TABLE `review`
  ADD COLUMN IF NOT EXISTS `ReviewStatus` VARCHAR(20) NOT NULL DEFAULT 'PENDING';
