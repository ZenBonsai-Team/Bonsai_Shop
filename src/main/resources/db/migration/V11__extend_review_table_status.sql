ALTER TABLE `review` 
ADD COLUMN `ReviewStatus` varchar(20) NOT NULL DEFAULT 'PENDING',
ADD COLUMN `Response` varchar(1000) DEFAULT NULL;
