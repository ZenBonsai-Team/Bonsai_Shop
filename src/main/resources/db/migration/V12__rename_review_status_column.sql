-- V12: Rename review status column to match entity mapping
-- The V11 migration added column 'Status'. The entity maps to 'ReviewStatus'.
-- This migration renames the column to align with the entity.
ALTER TABLE `review` 
  CHANGE COLUMN `Status` `ReviewStatus` VARCHAR(20) NOT NULL DEFAULT 'PENDING';
