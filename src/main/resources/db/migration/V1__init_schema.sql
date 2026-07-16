CREATE DATABASE  IF NOT EXISTS `bonsai_shop` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `bonsai_shop`;
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: bonsai_shop
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `artisan_profile`
--

DROP TABLE IF EXISTS `artisan_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `artisan_profile` (
  `ArtisanID` int NOT NULL AUTO_INCREMENT,
  `UserID` int DEFAULT NULL,
  `FullName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Bio` text COLLATE utf8mb4_unicode_ci,
  `YearsOfExperience` int DEFAULT NULL,
  `Specialty` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ArtisanID`),
  KEY `fk_artisan_user` (`UserID`),
  CONSTRAINT `fk_artisan_user` FOREIGN KEY (`UserID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `artisan_profile`
--

LOCK TABLES `artisan_profile` WRITE;
/*!40000 ALTER TABLE `artisan_profile` DISABLE KEYS */;
INSERT INTO `artisan_profile` VALUES (1,2,'Nguyễn Thành','Chuyên gia Sanh cổ Nam Định',40,'Sanh Nam Điền'),(2,3,'Trần Duy','Bậc thầy Tùng La Hán',25,'Tùng La Hán'),(3,NULL,'Lê Hoàn','Nghệ nhân tạo dáng Văn Nhân',15,'Bonsai Mini'),(4,NULL,'Phạm Quang','Chuyên gia Bonsai ôm đá',20,'Cây ký đá'),(5,NULL,'Vũ Tiệp','Lão nông yêu cây',50,'Đa, Đề'),(6,NULL,'Hoàng Thắng','Nghệ nhân trẻ triển vọng',10,'Linh Sam'),(7,NULL,'Đỗ Hùng','Chuyên Mai Chiếu Thủy',30,'Mai Chiếu Thủy'),(8,NULL,'Bùi Phái','Nghệ nhân uốn kẽm nghệ thuật',22,'Sanh Quê'),(9,NULL,'Lê Minh','Sáng tạo dáng Thác Đổ',18,'Cây dáng Huyền'),(10,NULL,'Ngô Vân','Chuyên gia tiểu cảnh',12,'Tiểu cảnh Bonsai');
/*!40000 ALTER TABLE `artisan_profile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bussiness_action`
--

DROP TABLE IF EXISTS `bussiness_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bussiness_action` (
  `ActionID` int NOT NULL AUTO_INCREMENT,
  `ActionCode` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ActionName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ActionID`),
  UNIQUE KEY `ActionCode` (`ActionCode`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bussiness_action`
--

LOCK TABLES `bussiness_action` WRITE;
/*!40000 ALTER TABLE `bussiness_action` DISABLE KEYS */;
INSERT INTO `bussiness_action` VALUES (1,'ACTION_PRODUCT_MANAGE','Quản lý sản phẩm','Cho phép thêm, sửa, xóa sản phẩm'),(2,'ACTION_ORDER_VIEW_ALL','Xem tất cả đơn hàng','Cho phép xem tất cả đơn hàng hệ thống'),(3,'ACTION_ORDER_HANDLE_CLAIM','Xử lý khiếu nại đơn hàng','Cho phép xử lý khiếu nại của khách hàng'),(4,'ACTION_USER_MANAGE','Quản lý người dùng','Cho phép quản trị tài khoản người dùng');
/*!40000 ALTER TABLE `bussiness_action` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cart`
--

DROP TABLE IF EXISTS `cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart` (
  `CartID` int NOT NULL AUTO_INCREMENT,
  `CustomerID` int NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`CartID`),
  KEY `fk_cart_user` (`CustomerID`),
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart`
--

LOCK TABLES `cart` WRITE;
/*!40000 ALTER TABLE `cart` DISABLE KEYS */;
/*!40000 ALTER TABLE `cart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cart_item`
--

DROP TABLE IF EXISTS `cart_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_item` (
  `CartItemID` int NOT NULL AUTO_INCREMENT,
  `CartID` int NOT NULL,
  `ProductID` int NOT NULL,
  PRIMARY KEY (`CartItemID`),
  KEY `fk_ci_cart` (`CartID`),
  KEY `fk_ci_product` (`ProductID`),
  CONSTRAINT `fk_ci_cart` FOREIGN KEY (`CartID`) REFERENCES `cart` (`CartID`) ON DELETE CASCADE,
  CONSTRAINT `fk_ci_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_item`
--

LOCK TABLES `cart_item` WRITE;
/*!40000 ALTER TABLE `cart_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `cart_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `CategoryID` int NOT NULL AUTO_INCREMENT,
  `CategoryName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`CategoryID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (1,'Dòng Sanh','Cây Sanh cảnh nghệ thuật'),(2,'Dòng Tùng','Cây Tùng cảnh truyền thống'),(3,'Dòng Mai','Hoa Mai cảnh bonsai'),(4,'Cây Mini','Bonsai kích thước nhỏ để bàn');
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `community_comment`
--

DROP TABLE IF EXISTS `community_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_comment` (
  `CommentID` int NOT NULL AUTO_INCREMENT,
  `PostID` int NOT NULL,
  `UserID` int DEFAULT NULL,
  `AuthorName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `AuthorAvatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`CommentID`),
  KEY `fk_cc_post` (`PostID`),
  KEY `fk_cc_user` (`UserID`),
  CONSTRAINT `fk_cc_post` FOREIGN KEY (`PostID`) REFERENCES `community_post` (`PostID`) ON DELETE CASCADE,
  CONSTRAINT `fk_cc_user` FOREIGN KEY (`UserID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `community_comment`
--

LOCK TABLES `community_comment` WRITE;
/*!40000 ALTER TABLE `community_comment` DISABLE KEYS */;
INSERT INTO `community_comment` VALUES (1,1,2,'Thành Nghệ Nhân','https://api.dicebear.com/7.x/adventurer/svg?seed=thanh','Bài viết rất có chiều sâu chiêm nghiệm!','2026-07-16 13:33:20'),(2,1,6,'Khách 01',NULL,'Cảm ơn chú đã chia sẻ tri thức bổ ích.','2026-07-16 13:33:20'),(3,2,6,'Khách 01',NULL,'Nhà em bị chết mất một cây tùng do gió bấc rồi, tiếc quá.','2026-07-16 13:33:20'),(4,5,7,'Khách 02',NULL,'Cách làm rễ ôm đá rất chi tiết, cảm ơn nhà vườn!','2026-07-16 13:33:20');
/*!40000 ALTER TABLE `community_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `community_post`
--

DROP TABLE IF EXISTS `community_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_post` (
  `PostID` int NOT NULL AUTO_INCREMENT,
  `AuthorID` int DEFAULT NULL,
  `Title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `Summary` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `AuthorName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `AuthorAvatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Category` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ImageUrl` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ReadTime` int DEFAULT '5',
  `LikesCount` int DEFAULT '0',
  `CommentsCount` int DEFAULT '0',
  `Status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'APPROVED',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`PostID`),
  KEY `fk_cp_author` (`AuthorID`),
  CONSTRAINT `fk_cp_author` FOREIGN KEY (`AuthorID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `community_post`
--

LOCK TABLES `community_post` WRITE;
/*!40000 ALTER TABLE `community_post` DISABLE KEYS */;
INSERT INTO `community_post` VALUES (1,1,'Triết lý Wabi-Sabi trong Bonsai','Vẻ đẹp của sự không hoàn hảo và dấu vết thời gian...','Vẻ đẹp của sự không hoàn hảo trong bonsai.','Nguyễn Văn Chủ','https://api.dicebear.com/7.x/adventurer/svg?seed=owner','Triết lý','/images/bonsai-1.png',5,25,2,'APPROVED','2026-07-16 13:33:20'),(2,2,'Cách chăm Tùng mùa đông','Đừng để cây bị gió mùa Đông Bắc tạt trực tiếp...','Kinh nghiệm chăm sóc tùng mùa gió rét.','Thành Nghệ Nhân','https://api.dicebear.com/7.x/adventurer/svg?seed=thanh','Kỹ thuật','/images/bonsai-2.png',6,12,1,'APPROVED','2026-07-16 13:33:20'),(3,1,'Sanh Nam Điền và Sanh Quê','Cách phân biệt hai dòng sanh phổ biến nhất...','Phân biệt lá và da sanh nam điền.','Nguyễn Văn Chủ','https://api.dicebear.com/7.x/adventurer/svg?seed=owner','Kinh nghiệm','/images/default-bonsai.png',4,30,0,'APPROVED','2026-07-16 13:33:20'),(4,6,'Tôi đã mua cây đầu tiên thế nào','Chia sẻ của một người mới tập chơi...','Hành trình chọn mua bonsai ban đầu.','Khách 01',NULL,'Góc chia sẻ',NULL,3,8,0,'APPROVED','2026-07-16 13:33:20'),(5,1,'Kỹ thuật ký đá cho cây Sanh','Hướng dẫn chọn đá thấm thủy và cách ép rễ...','Hướng dẫn chi tiết quy trình ký đá.','Nguyễn Văn Chủ','https://api.dicebear.com/7.x/adventurer/svg?seed=owner','Kỹ thuật','/images/bonsai-3.png',8,45,1,'APPROVED','2026-07-16 13:33:20'),(6,2,'Tạo dáng Văn Nhân - Khó hay Dễ','Nét vẽ mảnh mai nhưng đầy khí chất...','Giới thiệu về dáng văn nhân cốt cách.','Thành Nghệ Nhân','https://api.dicebear.com/7.x/adventurer/svg?seed=thanh','Tạo dáng',NULL,5,18,0,'APPROVED','2026-07-16 13:33:20'),(7,1,'Phân bón hữu cơ cho Bonsai','Tại sao nên dùng bánh dầu thay vì hóa học...','Ưu điểm của phân hữu cơ chậm tan.','Nguyễn Văn Chủ','https://api.dicebear.com/7.x/adventurer/svg?seed=owner','Chăm sóc',NULL,4,22,0,'APPROVED','2026-07-16 13:33:20'),(8,5,'Bảo vệ tác quyền nghệ nhân','Cần minh bạch phả hệ cây để tránh hàng giả...','Minh bạch nguồn gốc nghệ nhân.','Lê Kiểm Duyệt',NULL,'Nhận định',NULL,7,15,0,'APPROVED','2026-07-16 13:33:20'),(9,1,'Lịch sử Bonsai Việt Nam','Từ thú chơi cung đình đến phong trào bình dân...','Lịch sử phát triển nền nghệ thuật nước nhà.','Nguyễn Văn Chủ','https://api.dicebear.com/7.x/adventurer/svg?seed=owner','Lịch sử',NULL,10,50,0,'APPROVED','2026-07-16 13:33:20'),(10,2,'Cắt giật hay uốn kẽm?','Ưu và nhược điểm của từng phương pháp tạo tác...','So sánh uốn kẽm và cắt giật xương cây.','Thành Nghệ Nhân','https://api.dicebear.com/7.x/adventurer/svg?seed=thanh','Kỹ thuật',NULL,6,29,0,'APPROVED','2026-07-16 13:33:20'),(11,6,'Cảm ơn nhà vườn vì cây Si đẹp','Review trải nghiệm mua hàng tại BSMS...','Trải nghiệm dịch vụ tuyệt vời.','Khách 01',NULL,'Góc chia sẻ',NULL,3,11,0,'APPROVED','2026-07-16 13:33:20'),(12,1,'Trưng bày Bonsai trong nhà cổ','Cách phối hợp không gian kiến trúc và cây cảnh...','Nghệ thuật trưng bày sảnh phòng.','Nguyễn Văn Chủ','https://api.dicebear.com/7.x/adventurer/svg?seed=owner','Không gian',NULL,5,16,0,'APPROVED','2026-07-16 13:33:20'),(13,2,'Xử lý cây bị úng rễ','Các bước cấp cứu khẩn cấp cho cây Bonsai...','Hướng dẫn xử lý bầu đất nghẹt nước.','Thành Nghệ Nhân','https://api.dicebear.com/7.x/adventurer/svg?seed=thanh','Cấp cứu cây',NULL,6,33,0,'APPROVED','2026-07-16 13:33:20'),(14,1,'Tâm thế người chơi cây','Chơi cây là rèn lòng thanh thản...','Sự tĩnh tại trong tâm hồn người chăm cây.','Nguyễn Văn Chủ','https://api.dicebear.com/7.x/adventurer/svg?seed=owner','Cảm nhận',NULL,4,40,0,'APPROVED','2026-07-16 13:33:20'),(15,7,'Hỏi về kỹ thuật tỉa lá Mai','Em mới chơi xin các bác chỉ giáo...','Hỏi đáp kỹ thuật nhặt lá mai Tết.','Khách 02',NULL,'Hỏi đáp',NULL,3,5,0,'APPROVED','2026-07-16 13:33:20');
/*!40000 ALTER TABLE `community_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `community_post_like`
--

DROP TABLE IF EXISTS `community_post_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_post_like` (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_post_user_like` (`post_id`,`user_id`),
  KEY `fk_like_user` (`user_id`),
  CONSTRAINT `fk_like_post` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`PostID`) ON DELETE CASCADE,
  CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `community_post_like`
--

LOCK TABLES `community_post_like` WRITE;
/*!40000 ALTER TABLE `community_post_like` DISABLE KEYS */;
INSERT INTO `community_post_like` VALUES (1,1,6,'2026-07-16 13:33:20'),(2,1,7,'2026-07-16 13:33:20'),(3,2,6,'2026-07-16 13:33:20');
/*!40000 ALTER TABLE `community_post_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','init schema','SQL','V1__init_schema.sql',1519090135,'root','2026-07-16 06:33:20',1204,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `moderation_notification`
--

DROP TABLE IF EXISTS `moderation_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `moderation_notification` (
  `NotificationID` int NOT NULL AUTO_INCREMENT,
  `TargetUsername` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Message` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `IsRead` tinyint(1) DEFAULT '0',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`NotificationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `moderation_notification`
--

LOCK TABLES `moderation_notification` WRITE;
/*!40000 ALTER TABLE `moderation_notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `moderation_notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order`
--

DROP TABLE IF EXISTS `order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order` (
  `OrderID` int NOT NULL AUTO_INCREMENT,
  `CustomerID` int DEFAULT NULL,
  `OrderCode` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CustomerName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CustomerPhone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CustomerEmail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ShippingAddress` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TotalAmount` decimal(15,2) DEFAULT NULL,
  `DepositAmount` decimal(15,2) DEFAULT '0.00',
  `CraneFee` decimal(15,2) DEFAULT '0.00',
  `ShippingFee` decimal(15,2) DEFAULT '0.00',
  `OrderStatus` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `Notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `OrderDate` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`OrderID`),
  UNIQUE KEY `OrderCode` (`OrderCode`),
  KEY `fk_o_user` (`CustomerID`),
  CONSTRAINT `fk_o_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order`
--

LOCK TABLES `order` WRITE;
/*!40000 ALTER TABLE `order` DISABLE KEYS */;
INSERT INTO `order` VALUES (1,6,'ORD-101','Khách 01','0901000001','cust01@gmail.com','Hồ Chí Minh',850000.00,0.00,0.00,0.00,'COMPLETED','Giao giờ hành chính','2026-07-16 13:33:20'),(2,7,'ORD-102','Khách 02','0901000002','cust02@gmail.com','Đà Nẵng',15000000.00,0.00,500000.00,200000.00,'SHIPPING','Cần bao bọc kỹ bầu đất','2026-07-16 13:33:20'),(3,8,'ORD-103','Khách 03','0901000003','cust03@gmail.com','Cần Thơ',2500000.00,0.00,0.00,500000.00,'PENDING','Giao cuối tuần','2026-07-16 13:33:20'),(4,9,'ORD-104','Khách 04','0901000004','cust04@gmail.com','Hải Phòng',3200000.00,1000000.00,0.00,150000.00,'APPROVED','Chuyển khoản cọc trước','2026-07-16 13:33:20'),(5,10,'ORD-105','Khách 05','0901000005','cust05@gmail.com','Bình Dương',1500000.00,0.00,0.00,50000.00,'COMPLETED','','2026-07-16 13:33:20'),(6,11,'ORD-106','Khách 06','0901000006','cust06@gmail.com','Đồng Nai',18000000.00,5000000.00,800000.00,300000.00,'SHIPPING','Giao xe cẩu lớn','2026-07-16 13:33:20'),(7,12,'ORD-107','Khách 07','0901000007','cust07@gmail.com','Quảng Ninh',5500000.00,0.00,0.00,250000.00,'PENDING','','2026-07-16 13:33:20'),(8,13,'ORD-108','Khách 08','0901000008','cust08@gmail.com','Thanh Hóa',4500000.00,0.00,0.00,100000.00,'APPROVED','Giao giờ tối','2026-07-16 13:33:20'),(9,14,'ORD-109','Khách 09','0901000009','cust09@gmail.com','Nghệ An',22000000.00,10000000.00,1000000.00,400000.00,'COMPLETED','Cọc trước 10tr','2026-07-16 13:33:20'),(10,15,'ORD-110','Khách 10','0901000010','cust10@gmail.com','Huế',8000000.00,0.00,200000.00,200000.00,'PENDING','Gọi trước khi giao','2026-07-16 13:33:20');
/*!40000 ALTER TABLE `order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_detail`
--

DROP TABLE IF EXISTS `order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_detail` (
  `OrderDetailID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL,
  `ProductID` int NOT NULL,
  `PriceAtPurchase` decimal(15,2) NOT NULL,
  PRIMARY KEY (`OrderDetailID`),
  KEY `fk_od_order` (`OrderID`),
  KEY `fk_od_product` (`ProductID`),
  CONSTRAINT `fk_od_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE,
  CONSTRAINT `fk_od_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_detail`
--

LOCK TABLES `order_detail` WRITE;
/*!40000 ALTER TABLE `order_detail` DISABLE KEYS */;
INSERT INTO `order_detail` VALUES (1,1,4,850000.00),(2,2,7,15000000.00),(3,3,9,2500000.00),(4,4,12,3200000.00);
/*!40000 ALTER TABLE `order_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_handling`
--

DROP TABLE IF EXISTS `order_handling`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_handling` (
  `OrderHandlingID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL,
  `ModeratorOrderID` int DEFAULT NULL,
  `HandledAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `ReleasedAt` datetime DEFAULT NULL,
  `IsActive` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`OrderHandlingID`),
  KEY `fk_oh_order` (`OrderID`),
  KEY `fk_oh_moderator` (`ModeratorOrderID`),
  CONSTRAINT `fk_oh_moderator` FOREIGN KEY (`ModeratorOrderID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL,
  CONSTRAINT `fk_oh_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_handling`
--

LOCK TABLES `order_handling` WRITE;
/*!40000 ALTER TABLE `order_handling` DISABLE KEYS */;
INSERT INTO `order_handling` VALUES (1,2,4,'2026-07-16 13:33:20',NULL,1),(2,6,4,'2026-07-16 13:33:20',NULL,1);
/*!40000 ALTER TABLE `order_handling` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_log`
--

DROP TABLE IF EXISTS `order_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_log` (
  `OrderLogID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL,
  `ActionByID` int NOT NULL,
  `ActionType` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FromStatus` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ToStatus` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ActionAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`OrderLogID`),
  KEY `fk_ol_order` (`OrderID`),
  KEY `fk_ol_user` (`ActionByID`),
  CONSTRAINT `fk_ol_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE,
  CONSTRAINT `fk_ol_user` FOREIGN KEY (`ActionByID`) REFERENCES `user` (`UserID`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_log`
--

LOCK TABLES `order_log` WRITE;
/*!40000 ALTER TABLE `order_log` DISABLE KEYS */;
INSERT INTO `order_log` VALUES (1,1,4,'DELIVER','APPROVED','COMPLETED','2026-07-16 13:33:20'),(2,5,4,'DELIVER','APPROVED','COMPLETED','2026-07-16 13:33:20');
/*!40000 ALTER TABLE `order_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_otp`
--

DROP TABLE IF EXISTS `password_reset_otp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_otp` (
  `OtpID` int NOT NULL AUTO_INCREMENT,
  `Email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `OtpCode` varchar(6) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ExpiredAt` datetime NOT NULL,
  `IsUsed` tinyint(1) DEFAULT '0',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`OtpID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_otp`
--

LOCK TABLES `password_reset_otp` WRITE;
/*!40000 ALTER TABLE `password_reset_otp` DISABLE KEYS */;
/*!40000 ALTER TABLE `password_reset_otp` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `PaymentID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL,
  `PaymentMethod` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PaymentStatus` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `PaymentType` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Amount` decimal(15,2) DEFAULT NULL,
  `PaymentDate` datetime DEFAULT NULL,
  PRIMARY KEY (`PaymentID`),
  UNIQUE KEY `OrderID` (`OrderID`),
  CONSTRAINT `fk_pay_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
INSERT INTO `payment` VALUES (1,1,'VNPAY','SUCCESS','ONLINE',850000.00,'2026-07-16 13:33:20'),(2,5,'COD','SUCCESS','CASH',1500000.00,'2026-07-16 13:33:20');
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `ProductID` int NOT NULL AUTO_INCREMENT,
  `CreatedByID` int DEFAULT NULL,
  `ArtisanID` int DEFAULT NULL,
  `VarietyID` int NOT NULL,
  `SegmentID` int NOT NULL,
  `ProductCode` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ProductName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Price` decimal(15,2) NOT NULL,
  `Age` int DEFAULT NULL,
  `Style` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IsPublicPrice` tinyint(1) DEFAULT '1',
  `ProductStatus` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'AVAILABLE',
  `Description` text COLLATE utf8mb4_unicode_ci,
  `Height` float DEFAULT NULL,
  `TrunkDiameter` float DEFAULT NULL,
  `ViewCount` int DEFAULT '0',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `TreeStory` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`ProductID`),
  UNIQUE KEY `ProductCode` (`ProductCode`),
  KEY `fk_p_createdby` (`CreatedByID`),
  KEY `fk_p_artisan` (`ArtisanID`),
  KEY `fk_p_variety` (`VarietyID`),
  KEY `fk_p_segment` (`SegmentID`),
  CONSTRAINT `fk_p_artisan` FOREIGN KEY (`ArtisanID`) REFERENCES `artisan_profile` (`ArtisanID`) ON DELETE SET NULL,
  CONSTRAINT `fk_p_createdby` FOREIGN KEY (`CreatedByID`) REFERENCES `user` (`UserID`) ON DELETE SET NULL,
  CONSTRAINT `fk_p_segment` FOREIGN KEY (`SegmentID`) REFERENCES `product_segment` (`SegmentID`),
  CONSTRAINT `fk_p_variety` FOREIGN KEY (`VarietyID`) REFERENCES `variety` (`VarietyID`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (1,1,1,1,3,'E-001','Sanh Nam Điền Long Đằng',250000000.00,50,'Dáng Long',0,'AVAILABLE','Cây dáng Long oai vệ bệ rễ vững chãi.',120,15,105,'2026-07-16 13:33:20','Cây báu vật của dòng họ Nguyễn...'),(2,1,2,3,3,'E-002','Tùng La Hán Tuyết Sơn',450000000.00,60,'Dáng Trực',0,'AVAILABLE','Tùng lá nhỏ uốn nắn tinh xảo.',160,22,94,'2026-07-16 13:33:20','Khai thác từ vùng núi cao phía Bắc...'),(3,2,3,5,2,'M-001','Mai Chiếu Thủy Bonsai',12000000.00,15,'Dáng Bay',1,'AVAILABLE','Thân xù xì, hoa nở quanh năm rất thơm.',45,8,120,'2026-07-16 13:33:20','Hoa nở trắng xóa thơm ngát...'),(4,2,6,6,1,'B-001','Si Nhật Mini Đặt Bàn',850000.00,5,'Dáng Trực',1,'AVAILABLE','Nhỏ gọn thích hợp decor bàn làm việc.',25,3,340,'2026-07-16 13:33:20','Phù hợp văn phòng hiện đại.'),(5,1,4,1,3,'E-003','Sanh Cổ Ôm Đá',180000000.00,45,'Ký đá',0,'AVAILABLE','Bệ rễ ăn bám chặt vào đá san hô.',110,18,55,'2026-07-16 13:33:20','Sự kết hợp giữa đá và rễ cây...'),(6,1,1,1,3,'E-004','Sanh Nam Điền Phu Thê',320000000.00,55,'Phu Thê',0,'AVAILABLE','Hai thân quấn quýt hài hòa cân đối.',130,20,78,'2026-07-16 13:33:20','Hai thân quấn quýt hài hòa...'),(7,2,5,2,2,'M-002','Sanh Quê Dáng Lão',15000000.00,30,'Dáng Lão',1,'AVAILABLE','Gốc to bậm mốc thời gian.',85,12,64,'2026-07-16 13:33:20','Gốc già nua gân guốc.'),(8,1,2,4,3,'E-005','Tùng Kim Cương Đại',950000000.00,100,'Dáng Trực',0,'AVAILABLE','Tuyệt phẩm quý hiếm dành cho đại gia.',210,35,150,'2026-07-16 13:33:20','Siêu phẩm trăm năm tuổi.'),(9,2,3,5,1,'B-002','Mai Chiếu Thủy Lá Trung',2500000.00,8,'Văn Nhân',1,'AVAILABLE','Uốn nắn theo phong cách tối giản thanh tao.',50,4,88,'2026-07-16 13:33:20','Nét vẽ thanh mảnh nghệ thuật.'),(10,2,7,1,2,'M-003','Sanh Nam Điền Tán Tròn',45000000.00,20,'Dáng Trực',1,'AVAILABLE','Bông tán đều đặn đối xứng.',95,10,45,'2026-07-16 13:33:20','Tán bông đều tản vân.'),(11,1,8,3,3,'E-006','Tùng La Hán Nghệ Thuật',155000000.00,35,'Thác Đổ',0,'AVAILABLE','Đường thân bay lượn sà xuống chậu.',70,9,72,'2026-07-16 13:33:20','Mềm mại như dòng suối.'),(12,2,9,6,1,'B-003','Si Nhật Ngũ Phúc',3200000.00,10,'Ngũ Phúc',1,'AVAILABLE','Năm tán bông đẹp sum suê.',35,5,110,'2026-07-16 13:33:20','Tượng trưng cho may mắn.'),(13,1,10,1,3,'E-007','Sanh Nam Điền Đại Thụ',210000000.00,48,'Dáng Trực',0,'AVAILABLE','Gốc vững chãi bệ rễ móng cọp.',150,25,60,'2026-07-16 13:33:20','Bệ rễ móng cọp trải rộng.'),(14,2,3,5,2,'M-004','Mai Chiếu Thủy Cổ',22000000.00,25,'Dáng Xiên',1,'AVAILABLE','Lũa thép tự nhiên cực đẹp.',65,11,49,'2026-07-16 13:33:20','Gốc bặm trợn rất có lực.'),(15,1,4,3,3,'E-008','Tùng Kim Cương Mini VIP',85000000.00,12,'Dáng Bay',0,'AVAILABLE','Lá kim dày xanh mướt mịn màng.',30,4,185,'2026-07-16 13:33:20','Hàng tuyển chọn showroom.'),(16,2,6,6,1,'B-004','Si Nhật Đậu Chậu Gốm',1500000.00,6,'Dáng Trực',1,'AVAILABLE','Đã thuần chậu lâu năm cực khỏe.',28,3,132,'2026-07-16 13:33:20','Lá nhỏ mướt xanh quanh năm.'),(17,1,1,1,3,'E-009','Sanh Cổ Ngũ Hành',550000000.00,75,'Ngũ Hành',0,'AVAILABLE','Năm thân tượng trưng Kim Mộc Thủy Hỏa Thổ.',180,30,99,'2026-07-16 13:33:20','Năm thân quần tụ uy nghi.'),(18,2,8,2,2,'M-005','Sanh Quê Ký Đá Thấm Thủy',18000000.00,18,'Thác Đổ',1,'AVAILABLE','Rễ ăn bám bọc kín đá thấm thủy.',60,6,80,'2026-07-16 13:33:20','Nét đá tự nhiên mọc rêu.'),(19,1,2,4,3,'E-010','Tùng Kim Cương Dáng Huyền',135000000.00,22,'Dáng Huyền',0,'AVAILABLE','Khúc uốn ngặt nghèo đầy nghệ thuật.',80,7,142,'2026-07-16 13:33:20','Đường thân uốn lượn như rồng.'),(20,2,9,5,1,'B-005','Mai Chiếu Thủy Độc Bản',5500000.00,14,'Dáng Kỳ',1,'AVAILABLE','Dáng kỳ quái độc bản có 1 không 2.',40,5,230,'2026-07-16 13:33:20','Dáng lạ cho người sưu tầm.');
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_media`
--

DROP TABLE IF EXISTS `product_media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_media` (
  `MediaID` int NOT NULL AUTO_INCREMENT,
  `ProductID` int NOT NULL,
  `MediaURL` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `MediaType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'IMAGE',
  `SlotType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'FRONT',
  `Caption` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IsThumbnail` tinyint(1) DEFAULT '1',
  `DisplayOrder` int DEFAULT '0',
  PRIMARY KEY (`MediaID`),
  KEY `fk_pm_product` (`ProductID`),
  CONSTRAINT `fk_pm_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_media`
--

LOCK TABLES `product_media` WRITE;
/*!40000 ALTER TABLE `product_media` DISABLE KEYS */;
INSERT INTO `product_media` VALUES (1,1,'/images/bonsai-1.png','IMAGE','FRONT','Mặt trước Sanh Nam Điền',1,0),(2,2,'/images/bonsai-2.png','IMAGE','FRONT','Mặt trước Tùng Tuyết Sơn',1,0),(3,3,'/images/bonsai-3.png','IMAGE','FRONT','Mai Chiếu Thủy Bonsai',1,0),(4,4,'/images/default-bonsai.png','IMAGE','FRONT','Si Nhật Mini',1,0);
/*!40000 ALTER TABLE `product_media` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_segment`
--

DROP TABLE IF EXISTS `product_segment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_segment` (
  `SegmentID` int NOT NULL AUTO_INCREMENT,
  `SegmentName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`SegmentID`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_segment`
--

LOCK TABLES `product_segment` WRITE;
/*!40000 ALTER TABLE `product_segment` DISABLE KEYS */;
INSERT INTO `product_segment` VALUES (1,'Budget'),(2,'Mid'),(3,'Elite');
/*!40000 ALTER TABLE `product_segment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_tag`
--

DROP TABLE IF EXISTS `product_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_tag` (
  `ProductID` int NOT NULL,
  `TagID` int NOT NULL,
  PRIMARY KEY (`ProductID`,`TagID`),
  KEY `fk_pt_tag` (`TagID`),
  CONSTRAINT `fk_pt_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE,
  CONSTRAINT `fk_pt_tag` FOREIGN KEY (`TagID`) REFERENCES `tag` (`TagID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_tag`
--

LOCK TABLES `product_tag` WRITE;
/*!40000 ALTER TABLE `product_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `product_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `ReviewID` int NOT NULL AUTO_INCREMENT,
  `ProductID` int NOT NULL,
  `CustomerID` int NOT NULL,
  `Rating` int DEFAULT NULL,
  `Comment` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ReviewID`),
  KEY `fk_rev_product` (`ProductID`),
  KEY `fk_rev_user` (`CustomerID`),
  CONSTRAINT `fk_rev_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE,
  CONSTRAINT `fk_rev_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE,
  CONSTRAINT `review_chk_1` CHECK ((`Rating` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
INSERT INTO `review` VALUES (1,4,6,5,'Cây để bàn rất đẹp và tươi tốt, đóng gói cẩn thận lắm ạ!','2026-07-16 13:33:20'),(2,7,7,4,'Cây sanh uốn khá già dặn, giao hàng hơi lâu chút nhưng chấp nhận được.','2026-07-16 13:33:20');
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `RoleID` int NOT NULL AUTO_INCREMENT,
  `RoleName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`RoleID`),
  UNIQUE KEY `RoleName` (`RoleName`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role`
VALUES (1,'ROLE_CUSTOMER','Khách'),
		(2,'ROLE_OWNER','Chủ'),
        (3,'ROLE_ARTISAN','Nghệ nhân'),
        (4,'ROLE_MODERATOR','Kiểm duyệt đơn'),
        (5,'ROLE_CONTENT_MODERATOR','Kiểm duyệt');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_action`
--

DROP TABLE IF EXISTS `role_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_action` (
  `RoleID` int NOT NULL,
  `ActionID` int NOT NULL,
  `IsEnabled` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`RoleID`,`ActionID`),
  KEY `fk_roleaction_action` (`ActionID`),
  CONSTRAINT `fk_roleaction_action` FOREIGN KEY (`ActionID`) REFERENCES `bussiness_action` (`ActionID`) ON DELETE CASCADE,
  CONSTRAINT `fk_roleaction_role` FOREIGN KEY (`RoleID`) REFERENCES `role` (`RoleID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_action`
--

LOCK TABLES `role_action` WRITE;
/*!40000 ALTER TABLE `role_action` DISABLE KEYS */;
INSERT INTO `role_action` VALUES (2,1,1),(2,2,1),(2,3,1),(2,4,1),(3,1,1),(4,2,1),(4,3,1);
/*!40000 ALTER TABLE `role_action` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tag`
--

DROP TABLE IF EXISTS `tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tag` (
  `TagID` int NOT NULL AUTO_INCREMENT,
  `TagName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`TagID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tag`
--

LOCK TABLES `tag` WRITE;
/*!40000 ALTER TABLE `tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `UserID` int NOT NULL AUTO_INCREMENT,
  `RoleID` int NOT NULL,
  `FullName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar_public_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`UserID`),
  UNIQUE KEY `Email` (`Email`),
  UNIQUE KEY `Username` (`Username`),
  KEY `fk_u_role` (`RoleID`),
  CONSTRAINT `fk_u_role` FOREIGN KEY (`RoleID`) REFERENCES `role` (`RoleID`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,2,'Nguyễn Văn Chủ','owner','owner@bonsai.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0912345678',NULL,NULL,'Hà Nội','ACTIVE','2026-07-16 13:33:20'),(2,3,'Thành Nghệ Nhân','artisan_thanh','thanh@art.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0988111222',NULL,NULL,'Nam Định','ACTIVE','2026-07-16 13:33:20'),(3,3,'Duy Nghệ Nhân','artisan_duy','duy@art.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0988333444',NULL,NULL,'Hải Phòng','ACTIVE','2026-07-16 13:33:20'),(4,4,'Trần Kiểm Đơn','moderator_ship','ship@bonsai.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0333222111',NULL,NULL,'Hà Nội','ACTIVE','2026-07-16 13:33:20'),(5,5,'Lê Kiểm Duyệt','moderator_content','content@bonsai.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0555666777',NULL,NULL,'Hà Nội','ACTIVE','2026-07-16 13:33:20'),(6,1,'Khách 01','cust01','cust01@gmail.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0901000001',NULL,NULL,'Hồ Chí Minh','ACTIVE','2026-07-16 13:33:20'),(7,1,'Khách 02','cust02','cust02@gmail.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0901000002',NULL,NULL,'Đà Nẵng','ACTIVE','2026-07-16 13:33:20'),(8,1,'Khách 03','cust03','cust03@gmail.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0901000003',NULL,NULL,'Cần Thơ','ACTIVE','2026-07-16 13:33:20'),(9,1,'Khách 04','cust04','cust04@gmail.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0901000004',NULL,NULL,'Hải Phòng','ACTIVE','2026-07-16 13:33:20'),(10,1,'Khách 05','cust05','cust05@gmail.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0901000005',NULL,NULL,'Bình Dương','ACTIVE','2026-07-16 13:33:20'),(11,1,'Khách 06','cust06','cust06@gmail.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0901000006',NULL,NULL,'Đồng Nai','ACTIVE','2026-07-16 13:33:20'),(12,1,'Khách 07','cust07','cust07@gmail.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0901000007',NULL,NULL,'Quảng Ninh','ACTIVE','2026-07-16 13:33:20'),(13,1,'Khách 08','cust08','cust08@gmail.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0901000008',NULL,NULL,'Thanh Hóa','ACTIVE','2026-07-16 13:33:20'),(14,1,'Khách 09','cust09','cust09@gmail.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0901000009',NULL,NULL,'Nghệ An','ACTIVE','2026-07-16 13:33:20'),(15,1,'Khách 10','cust10','cust10@gmail.com','$2a$10$MD2agZTBLPkYRlh.KT983.UUbRI/W9GrFu4VvwOAB05bB6pCchSsa','0901000010',NULL,NULL,'Huế','ACTIVE','2026-07-16 13:33:20');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `variety`
--

DROP TABLE IF EXISTS `variety`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `variety` (
  `VarietyID` int NOT NULL AUTO_INCREMENT,
  `CategoryID` int NOT NULL,
  `VarietyName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`VarietyID`),
  KEY `fk_v_cat` (`CategoryID`),
  CONSTRAINT `fk_v_cat` FOREIGN KEY (`CategoryID`) REFERENCES `category` (`CategoryID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `variety`
--

LOCK TABLES `variety` WRITE;
/*!40000 ALTER TABLE `variety` DISABLE KEYS */;
INSERT INTO `variety` VALUES (1,1,'Sanh Nam Điền','Sanh Nam Điền lá bóng đẹp cổ kính'),(2,1,'Sanh Quê','Sanh Quê khỏe dễ uốn'),(3,2,'Tùng La Hán','Tùng Vạn Niên thanh lịch phong thủy'),(4,2,'Tùng Kim Cương','Dòng Tùng quý hiếm lá nhọn xanh mướt'),(5,3,'Mai Chiếu Thủy','Hoa thơm trắng tinh khiết phát lộc'),(6,4,'Si Nhật','Si Nhật Bonsai nhỏ nhắn xinh xắn');
/*!40000 ALTER TABLE `variety` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `viewing_appointment`
--

DROP TABLE IF EXISTS `viewing_appointment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `viewing_appointment` (
  `AppointmentID` int NOT NULL AUTO_INCREMENT,
  `CustomerID` int NOT NULL,
  `ProductID` int NOT NULL,
  `AppointmentDate` datetime NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `UpdatedAt` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `Note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  PRIMARY KEY (`AppointmentID`),
  KEY `fk_va_user` (`CustomerID`),
  KEY `fk_va_product` (`ProductID`),
  CONSTRAINT `fk_va_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE,
  CONSTRAINT `fk_va_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `viewing_appointment`
--

LOCK TABLES `viewing_appointment` WRITE;
/*!40000 ALTER TABLE `viewing_appointment` DISABLE KEYS */;
INSERT INTO `viewing_appointment` VALUES (1,6,1,'2026-07-20 09:00:00','2026-07-16 13:33:20','2026-07-16 13:33:20','Muốn xem thực tế gốc cây Sanh Long Đằng','PENDING'),(2,7,2,'2026-07-22 14:00:00','2026-07-16 13:33:20','2026-07-16 13:33:20','Xem tùng tuyết sơn cành bay','PENDING');
/*!40000 ALTER TABLE `viewing_appointment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wishlist`
--

DROP TABLE IF EXISTS `wishlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wishlist` (
  `CustomerID` int NOT NULL,
  `ProductID` int NOT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`CustomerID`,`ProductID`),
  KEY `fk_wl_product` (`ProductID`),
  CONSTRAINT `fk_wl_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`) ON DELETE CASCADE,
  CONSTRAINT `fk_wl_user` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wishlist`
--

LOCK TABLES `wishlist` WRITE;
/*!40000 ALTER TABLE `wishlist` DISABLE KEYS */;
/*!40000 ALTER TABLE `wishlist` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-16 13:39:00
