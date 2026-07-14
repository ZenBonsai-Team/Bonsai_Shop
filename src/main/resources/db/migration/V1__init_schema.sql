CREATE DATABASE  IF NOT EXISTS `bonsai_shop` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `bonsai_shop`;

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
-- Table structure for table `bussiness_action`
--

DROP TABLE IF EXISTS `bussiness_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bussiness_action` (
  `ActionID` int NOT NULL AUTO_INCREMENT,
  `ActionCode` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ActionName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ActionID`),
  UNIQUE KEY `ActionCode` (`ActionCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  KEY `fk_cart_customer` (`CustomerID`),
  CONSTRAINT `fk_cart_customer` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  KEY `fk_cartitem_cart` (`CartID`),
  KEY `fk_cartitem_product` (`ProductID`),
  CONSTRAINT `fk_cartitem_cart` FOREIGN KEY (`CartID`) REFERENCES `cart` (`CartID`),
  CONSTRAINT `fk_cartitem_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `CategoryID` int NOT NULL AUTO_INCREMENT,
  `CategoryName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`CategoryID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order`
--

DROP TABLE IF EXISTS `order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order` (
  `OrderID` int NOT NULL AUTO_INCREMENT,
  `CustomerID` int DEFAULT NULL,
  `OrderCode` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `CustomerName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CustomerPhone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CustomerEmail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ShippingAddress` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `OrderDate` datetime DEFAULT CURRENT_TIMESTAMP,
  `TotalAmount` decimal(15,2) DEFAULT NULL,
  `DepositAmount` decimal(15,2) DEFAULT '0.00',
  `OrderStatus` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  PRIMARY KEY (`OrderID`),
  UNIQUE KEY `OrderCode` (`OrderCode`),
  KEY `fk_order_customer` (`CustomerID`),
  CONSTRAINT `fk_order_customer` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  KEY `fk_orderdetail_order` (`OrderID`),
  KEY `fk_orderdetail_product` (`ProductID`),
  CONSTRAINT `fk_orderdetail_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`),
  CONSTRAINT `fk_orderdetail_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  KEY `fk_orderhandling_order` (`OrderID`),
  KEY `fk_orderhandling_moderator` (`ModeratorOrderID`),
  CONSTRAINT `fk_orderhandling_moderator` FOREIGN KEY (`ModeratorOrderID`) REFERENCES `user` (`UserID`),
  CONSTRAINT `fk_orderhandling_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `ActionType` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FromStatus` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ToStatus` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ActionAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`OrderLogID`),
  KEY `fk_orderlog_order` (`OrderID`),
  KEY `fk_orderlog_actionby` (`ActionByID`),
  CONSTRAINT `fk_orderlog_actionby` FOREIGN KEY (`ActionByID`) REFERENCES `user` (`UserID`),
  CONSTRAINT `fk_orderlog_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `password_reset_otp`
--

DROP TABLE IF EXISTS `password_reset_otp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_otp` (
  `OtpID` int NOT NULL AUTO_INCREMENT,
  `Email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `OtpCode` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ExpiredAt` datetime NOT NULL,
  `IsUsed` tinyint(1) DEFAULT '0',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`OtpID`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `PaymentID` int NOT NULL AUTO_INCREMENT,
  `OrderID` int NOT NULL,
  `PaymentMethod` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PaymentStatus` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `PaymentType` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Amount` decimal(15,2) DEFAULT NULL,
  `PaymentDate` datetime DEFAULT NULL,
  PRIMARY KEY (`PaymentID`),
  UNIQUE KEY `OrderID` (`OrderID`),
  CONSTRAINT `fk_payment_order` FOREIGN KEY (`OrderID`) REFERENCES `order` (`OrderID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `ProductID` int NOT NULL AUTO_INCREMENT,
  `SellerID` int DEFAULT NULL,
  `VarietyID` int NOT NULL,
  `SegmentID` int NOT NULL,
  `ProductCode` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ProductName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `Age` int DEFAULT NULL,
  `Height` float DEFAULT NULL,
  `TrunkDiameter` float DEFAULT NULL,
  `Style` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Price` decimal(15,2) NOT NULL,
  `IsPublicPrice` tinyint(1) DEFAULT '1',
  `ProductStatus` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'AVAILABLE',
  `ViewCount` int DEFAULT '0',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ProductID`),
  UNIQUE KEY `ProductCode` (`ProductCode`),
  KEY `fk_product_seller` (`SellerID`),
  KEY `fk_product_variety` (`VarietyID`),
  KEY `fk_product_segment` (`SegmentID`),
  CONSTRAINT `fk_product_segment` FOREIGN KEY (`SegmentID`) REFERENCES `product_segment` (`SegmentID`),
  CONSTRAINT `fk_product_seller` FOREIGN KEY (`SellerID`) REFERENCES `user` (`UserID`),
  CONSTRAINT `fk_product_variety` FOREIGN KEY (`VarietyID`) REFERENCES `variety` (`VarietyID`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_media`
--

DROP TABLE IF EXISTS `product_media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_media` (
  `MediaID` int NOT NULL AUTO_INCREMENT,
  `ProductID` int NOT NULL,
  `MediaURL` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `MediaType` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SlotType` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Caption` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IsThumbnail` tinyint(1) DEFAULT '1',
  `DisplayOrder` int DEFAULT '0',
  PRIMARY KEY (`MediaID`),
  KEY `fk_productmedia_product` (`ProductID`),
  CONSTRAINT `fk_productmedia_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_segment`
--

DROP TABLE IF EXISTS `product_segment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_segment` (
  `SegmentID` int NOT NULL AUTO_INCREMENT,
  `SegmentName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`SegmentID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  KEY `fk_producttag_tag` (`TagID`),
  CONSTRAINT `fk_producttag_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`),
  CONSTRAINT `fk_producttag_tag` FOREIGN KEY (`TagID`) REFERENCES `tag` (`TagID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `ReviewID` int NOT NULL AUTO_INCREMENT,
  `CustomerID` int NOT NULL,
  `ProductID` int NOT NULL,
  `Rating` int DEFAULT NULL,
  `Comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ReviewID`),
  KEY `fk_review_customer` (`CustomerID`),
  KEY `fk_review_product` (`ProductID`),
  CONSTRAINT `fk_review_customer` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`),
  CONSTRAINT `fk_review_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`),
  CONSTRAINT `review_chk_1` CHECK ((`Rating` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `RoleID` int NOT NULL AUTO_INCREMENT,
  `RoleName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`RoleID`),
  UNIQUE KEY `RoleName` (`RoleName`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  CONSTRAINT `fk_roleaction_action` FOREIGN KEY (`ActionID`) REFERENCES `bussiness_action` (`ActionID`),
  CONSTRAINT `fk_roleaction_role` FOREIGN KEY (`RoleID`) REFERENCES `role` (`RoleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tag`
--

DROP TABLE IF EXISTS `tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tag` (
  `TagID` int NOT NULL AUTO_INCREMENT,
  `TagName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`TagID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `UserID` int NOT NULL AUTO_INCREMENT,
  `RoleID` int NOT NULL,
  `FullName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar_public_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`UserID`),
  UNIQUE KEY `Email` (`Email`),
  UNIQUE KEY `Username` (`Username`),
  KEY `fk_user_role` (`RoleID`),
  CONSTRAINT `fk_user_role` FOREIGN KEY (`RoleID`) REFERENCES `role` (`RoleID`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `variety`
--

DROP TABLE IF EXISTS `variety`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `variety` (
  `VarietyID` int NOT NULL AUTO_INCREMENT,
  `CategoryID` int NOT NULL,
  `VarietyName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`VarietyID`),
  KEY `fk_variety_category` (`CategoryID`),
  CONSTRAINT `fk_variety_category` FOREIGN KEY (`CategoryID`) REFERENCES `category` (`CategoryID`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `Note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  PRIMARY KEY (`AppointmentID`),
  KEY `fk_appointment_customer` (`CustomerID`),
  KEY `fk_appointment_product` (`ProductID`),
  CONSTRAINT `fk_appointment_customer` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`),
  CONSTRAINT `fk_appointment_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  KEY `fk_wishlist_product` (`ProductID`),
  CONSTRAINT `fk_wishlist_customer` FOREIGN KEY (`CustomerID`) REFERENCES `user` (`UserID`),
  CONSTRAINT `fk_wishlist_product` FOREIGN KEY (`ProductID`) REFERENCES `product` (`ProductID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Seed baseline data
--

-- Roles
INSERT INTO `role` (RoleID, RoleName, Description) VALUES (1,'ROLE_USER','Khách hàng thông thường');
INSERT INTO `role` (RoleID, RoleName, Description) VALUES (2,'ROLE_ADMIN','Quản trị viên hệ thống');
INSERT INTO `role` (RoleID, RoleName, Description) VALUES (3,'ROLE_MODERATOR','Người kiểm duyệt / xử lý khiếu nại');

-- Categories
INSERT INTO `category` (CategoryID, CategoryName, Description) VALUES (1,'Pine Bonsai','Traditional pine bonsai');
INSERT INTO `category` (CategoryID, CategoryName, Description) VALUES (2,'Maple Bonsai','Japanese maple bonsai');
INSERT INTO `category` (CategoryID, CategoryName, Description) VALUES (3,'Juniper Bonsai','Popular beginner bonsai');
INSERT INTO `category` (CategoryID, CategoryName, Description) VALUES (4,'Flowering Bonsai','Flowering bonsai trees');

-- Segments
INSERT INTO `product_segment` (SegmentID, SegmentName) VALUES (1,'Beginner');
INSERT INTO `product_segment` (SegmentID, SegmentName) VALUES (2,'Intermediate');
INSERT INTO `product_segment` (SegmentID, SegmentName) VALUES (3,'Premium');
INSERT INTO `product_segment` (SegmentID, SegmentName) VALUES (4,'Collector');

-- Varieties
INSERT INTO `variety` (VarietyID, CategoryID, VarietyName, Description) VALUES (1,1,'Japanese Black Pine','Classic Japanese pine');
INSERT INTO `variety` (VarietyID, CategoryID, VarietyName, Description) VALUES (2,1,'White Pine','Elegant white pine');
INSERT INTO `variety` (VarietyID, CategoryID, VarietyName, Description) VALUES (3,2,'Red Maple','Beautiful autumn colors');
INSERT INTO `variety` (VarietyID, CategoryID, VarietyName, Description) VALUES (4,3,'Chinese Juniper','Easy maintenance');
INSERT INTO `variety` (VarietyID, CategoryID, VarietyName, Description) VALUES (5,4,'Azalea Bonsai','Seasonal flowers');

-- Seed initial admin/user
INSERT INTO `user` (UserID, RoleID, FullName, Username, Email, Password, Phone, Address, Status) VALUES (1,2,'Nguyễn Kiều Tùng Dương','DuongNKT','nguyenkieutungduong@gmail.com','$2a$10$STf1tU5lFcq6Zm4xgZRujuA7wsGd.6AK4nRH6FEL.ieVUQE8EWp3e','0984634913','abc','ACTIVE');

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
