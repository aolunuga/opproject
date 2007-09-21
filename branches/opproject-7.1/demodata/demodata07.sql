-- MySQL dump 10.10
--
-- Host: localhost    Database: opproject
-- ------------------------------------------------------
-- Server version	5.0.19-nt

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `hibernate_unique_key`
--

DROP TABLE IF EXISTS `hibernate_unique_key`;
CREATE TABLE `hibernate_unique_key` (
  `next_hi` int(11) default NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `hibernate_unique_key`
--


/*!40000 ALTER TABLE `hibernate_unique_key` DISABLE KEYS */;
LOCK TABLES `hibernate_unique_key` WRITE;
INSERT INTO `hibernate_unique_key` VALUES (12);
UNLOCK TABLES;
/*!40000 ALTER TABLE `hibernate_unique_key` ENABLE KEYS */;

--
-- Table structure for table `op_activity`
--

DROP TABLE IF EXISTS `op_activity`;
CREATE TABLE `op_activity` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) default NULL,
  `op_description` varchar(255) default NULL,
  `op_type` tinyint(4) default NULL,
  `op_attributes` int(11) default NULL,
  `op_sequence` int(11) default NULL,
  `op_outlinelevel` tinyint(4) default NULL,
  `op_start` date default NULL,
  `op_finish` date default NULL,
  `op_duration` double default NULL,
  `op_complete` double default NULL,
  `op_priority` tinyint(4) default NULL,
  `op_baseeffort` double default NULL,
  `op_basetravelcosts` double default NULL,
  `op_basepersonnelcosts` double default NULL,
  `op_basematerialcosts` double default NULL,
  `op_baseexternalcosts` double default NULL,
  `op_basemiscellaneouscosts` double default NULL,
  `op_actualeffort` double default NULL,
  `op_actualtravelcosts` double default NULL,
  `op_actualpersonnelcosts` double default NULL,
  `op_actualmaterialcosts` double default NULL,
  `op_actualexternalcosts` double default NULL,
  `op_actualmiscellaneouscosts` double default NULL,
  `op_remainingeffort` double default NULL,
  `op_deleted` bit(1) default NULL,
  `op_expanded` bit(1) default NULL,
  `op_template` bit(1) default NULL,
  `op_projectplan` bigint(20) default NULL,
  `op_category` bigint(20) default NULL,
  `op_superactivity` bigint(20) default NULL,
  `op_responsibleresource` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `op_activity_start_i` (`op_start`),
  KEY `op_activity_finish_i` (`op_finish`),
  KEY `FK11FC23AD2A50EF1F` (`op_category`),
  KEY `FK11FC23AD570B4F11` (`op_superactivity`),
  KEY `FK11FC23ADD958CFCC` (`op_id`),
  KEY `FK11FC23ADB46C1030` (`op_projectplan`),
  KEY `FK11FC23AD53755975` (`op_responsibleresource`),
  CONSTRAINT `FK11FC23AD53755975` FOREIGN KEY (`op_responsibleresource`) REFERENCES `op_resource` (`op_id`),
  CONSTRAINT `FK11FC23AD2A50EF1F` FOREIGN KEY (`op_category`) REFERENCES `op_activitycategory` (`op_id`),
  CONSTRAINT `FK11FC23AD570B4F11` FOREIGN KEY (`op_superactivity`) REFERENCES `op_activity` (`op_id`),
  CONSTRAINT `FK11FC23ADB46C1030` FOREIGN KEY (`op_projectplan`) REFERENCES `op_projectplan` (`op_id`),
  CONSTRAINT `FK11FC23ADD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_activity`
--


/*!40000 ALTER TABLE `op_activity` DISABLE KEYS */;
LOCK TABLES `op_activity` WRITE;
INSERT INTO `op_activity` VALUES (65537,'Project Management','',1,0,0,0,'2007-01-01','2007-08-17',1320,7.5187969924812,0,532,0,69160,0,0,0,40,0,5200,0,0,0,492,'\0','','\0',32787,262149,NULL,NULL),(65538,'Project start','',0,0,1,1,'2007-01-01','2007-01-12',80,50,0,80,0,10400,0,0,0,40,0,5200,0,0,0,40,'\0','\0','\0',32787,262149,65537,NULL),(65539,'Project controlling','',0,0,2,1,'2007-01-15','2007-08-17',1240,0,0,372,0,48360,0,0,0,0,0,0,0,0,0,372,'\0','\0','\0',32787,262149,65537,NULL),(65540,'Project documentation','',0,0,3,1,'2007-07-23','2007-08-17',160,0,0,80,0,10400,0,0,0,0,0,0,0,0,0,80,'\0','\0','\0',32787,262149,65537,NULL),(65541,'Project completed','',2,0,4,1,'2007-08-17','2007-08-17',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,'\0','\0','\0',32787,262149,65537,NULL),(65542,'Design','',1,0,5,0,'2007-01-15','2007-02-23',240,0,0,240,0,31200,0,0,0,0,0,0,0,0,0,240,'\0','','\0',32787,262150,NULL,NULL),(65543,'Design UI','',0,0,6,1,'2007-01-15','2007-02-02',120,0,0,120,0,15600,0,0,0,0,0,0,0,0,0,120,'\0','\0','\0',32787,262150,65542,NULL),(65544,'Design database','',0,0,7,1,'2007-02-05','2007-02-23',120,0,0,120,0,15600,0,0,0,0,0,0,0,0,0,120,'\0','\0','\0',32787,262150,65542,NULL),(65545,'Design completed','',2,0,8,1,'2007-02-23','2007-02-23',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,'\0','\0','\0',32787,262150,65542,NULL),(65546,'Prototype','',1,0,9,0,'2007-02-05','2007-03-30',320,0,0,480,0,48000,0,0,0,0,0,0,0,0,0,480,'\0','','\0',32787,262151,NULL,NULL),(65547,'Prototype UI','',0,0,10,1,'2007-02-05','2007-03-09',200,0,0,200,0,20000,0,0,0,0,0,0,0,0,0,200,'\0','\0','\0',32787,262151,65546,NULL),(65548,'Prototype database','',0,0,11,1,'2007-02-26','2007-03-16',120,0,0,120,0,12000,0,0,0,0,0,0,0,0,0,120,'\0','\0','\0',32787,262151,65546,NULL),(65549,'Integration prototype','',0,0,12,1,'2007-03-19','2007-03-30',80,0,0,160,0,16000,0,0,0,0,0,0,0,0,0,160,'\0','\0','\0',32787,262151,65546,NULL),(65550,'Prototype completed','',2,0,13,1,'2007-03-30','2007-03-30',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,'\0','\0','\0',32787,262151,65546,NULL),(65551,'Implementation','',1,0,14,0,'2007-04-02','2007-07-06',560,0,0,2040,0,195600,0,0,0,0,0,0,0,0,0,2040,'\0','','\0',32787,262151,NULL,NULL),(65552,'Implementation UI','',0,0,15,1,'2007-04-02','2007-06-08',400,0,0,800,0,80000,0,0,0,0,0,0,0,0,0,800,'\0','\0','\0',32787,262151,65551,NULL),(65553,'Implementation database','',0,0,16,1,'2007-04-02','2007-05-25',320,0,0,640,0,64000,0,0,0,0,0,0,0,0,0,640,'\0','\0','\0',32787,262151,65551,NULL),(65554,'Integration','',0,0,17,1,'2007-06-11','2007-07-06',160,0,0,320,0,32000,0,0,0,0,0,0,0,0,0,320,'\0','\0','\0',32787,262151,65551,NULL),(65555,'System manual','',0,0,18,1,'2007-04-02','2007-05-18',280,0,0,280,0,19600,0,0,0,0,0,0,0,0,0,280,'\0','\0','\0',32787,262151,65551,NULL),(65556,'Implementation completed','',2,0,19,1,'2007-07-06','2007-07-06',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,'\0','\0','\0',32787,262151,65551,NULL),(65557,'QA','',1,0,20,0,'2007-06-11','2007-08-03',320,0,0,640,0,61600,0,0,0,0,0,0,0,0,0,640,'\0','','\0',32787,262152,NULL,NULL),(65558,'Manual tests (UI)','',0,0,21,1,'2007-06-11','2007-07-13',200,0,0,400,0,40000,0,0,0,0,0,0,0,0,0,400,'\0','\0','\0',32787,262152,65557,NULL),(65559,'Automated tests','',0,0,22,1,'2007-07-09','2007-08-03',160,0,0,160,0,11200,0,0,0,0,0,0,0,0,0,160,'\0','\0','\0',32787,262152,65557,NULL),(65560,'QA system manual','',0,0,23,1,'2007-07-09','2007-07-20',80,0,0,80,0,10400,0,0,0,0,0,0,0,0,0,80,'\0','\0','\0',32787,262152,65557,NULL),(65561,'QA completed','',2,0,24,1,'2007-08-03','2007-08-03',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,'\0','\0','\0',32787,262152,65557,NULL),(98363,'Training ALS 1','',0,0,0,0,'2007-02-19','2007-02-21',24,100,0,24,0,2760,0,0,0,24,0,2760,0,0,0,0,'\0','\0','\0',98305,NULL,NULL,NULL),(98364,'Workshop requirements','',0,0,1,0,'2007-02-22','2007-02-26',24,100,0,48,0,5880,0,0,0,48,0,5880,0,0,0,0,'\0','\0','\0',98305,NULL,NULL,NULL),(98365,'Documentation requirements','',0,0,2,0,'2007-02-27','2007-03-05',40,66.6666666666667,0,40,0,5200,0,0,0,32,0,4160,0,0,0,16,'\0','\0','\0',98305,NULL,NULL,NULL),(98366,'Training ALS 2','',0,0,3,0,'2007-02-27','2007-03-05',40,80,0,40,0,4600,0,0,0,32,0,3680,0,0,0,8,'\0','\0','\0',98305,NULL,NULL,NULL),(98367,'Review requirements','',0,0,4,0,'2007-03-06','2007-03-19',80,0,0,40,0,10400,0,0,0,0,0,0,0,0,0,40,'\0','\0','\0',98305,NULL,NULL,NULL),(98368,'Implementation customizing','',0,0,5,0,'2007-03-20','2007-04-16',160,0,0,160,0,18400,0,0,0,0,0,0,0,0,0,160,'\0','\0','\0',98305,NULL,NULL,NULL),(98369,'Installation ALS','',0,0,6,0,'2007-04-17','2007-04-30',80,0,0,80,0,9200,0,0,0,0,0,0,0,0,0,80,'\0','\0','\0',98305,NULL,NULL,NULL),(98370,'Acceptance','',2,0,7,0,'2007-04-30','2007-04-30',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,'\0','\0','\0',98305,NULL,NULL,NULL);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_activity` ENABLE KEYS */;

--
-- Table structure for table `op_activitycategory`
--

DROP TABLE IF EXISTS `op_activitycategory`;
CREATE TABLE `op_activitycategory` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) NOT NULL,
  `op_description` varchar(255) default NULL,
  `op_color` int(11) default NULL,
  `op_active` bit(1) default NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_name` (`op_name`),
  KEY `FKE66AE3ABD958CFCC` (`op_id`),
  CONSTRAINT `FKE66AE3ABD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_activitycategory`
--


/*!40000 ALTER TABLE `op_activitycategory` DISABLE KEYS */;
LOCK TABLES `op_activitycategory` WRITE;
INSERT INTO `op_activitycategory` VALUES (262149,'Administration','',15,''),(262150,'Design & Conception',NULL,14,''),(262151,'Development',NULL,17,''),(262152,'Quality Assurance',NULL,12,'');
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_activitycategory` ENABLE KEYS */;

--
-- Table structure for table `op_activitycomment`
--

DROP TABLE IF EXISTS `op_activitycomment`;
CREATE TABLE `op_activitycomment` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) default NULL,
  `op_text` varchar(255) default NULL,
  `op_sequence` int(11) default NULL,
  `op_activity` bigint(20) default NULL,
  `op_creator` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK2F77BE12D958CFCC` (`op_id`),
  KEY `FK2F77BE12FBED317D` (`op_creator`),
  KEY `FK2F77BE12A7A23912` (`op_activity`),
  CONSTRAINT `FK2F77BE12A7A23912` FOREIGN KEY (`op_activity`) REFERENCES `op_activity` (`op_id`),
  CONSTRAINT `FK2F77BE12D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FK2F77BE12FBED317D` FOREIGN KEY (`op_creator`) REFERENCES `op_user` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_activitycomment`
--


/*!40000 ALTER TABLE `op_activitycomment` DISABLE KEYS */;
LOCK TABLES `op_activitycomment` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_activitycomment` ENABLE KEYS */;

--
-- Table structure for table `op_activityversion`
--

DROP TABLE IF EXISTS `op_activityversion`;
CREATE TABLE `op_activityversion` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) default NULL,
  `op_description` varchar(255) default NULL,
  `op_type` tinyint(4) default NULL,
  `op_attributes` int(11) default NULL,
  `op_sequence` int(11) default NULL,
  `op_outlinelevel` tinyint(4) default NULL,
  `op_start` date default NULL,
  `op_finish` date default NULL,
  `op_duration` double default NULL,
  `op_complete` double default NULL,
  `op_priority` tinyint(4) default NULL,
  `op_baseeffort` double default NULL,
  `op_basetravelcosts` double default NULL,
  `op_basepersonnelcosts` double default NULL,
  `op_basematerialcosts` double default NULL,
  `op_baseexternalcosts` double default NULL,
  `op_basemiscellaneouscosts` double default NULL,
  `op_expanded` bit(1) default NULL,
  `op_template` bit(1) default NULL,
  `op_category` bigint(20) default NULL,
  `op_activity` bigint(20) default NULL,
  `op_superactivityversion` bigint(20) default NULL,
  `op_planversion` bigint(20) default NULL,
  `op_responsibleresource` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `op_activityversion_start_i` (`op_start`),
  KEY `op_activityversion_finish_i` (`op_finish`),
  KEY `FKBC6EC8B2A50EF1F` (`op_category`),
  KEY `FKBC6EC8BD958CFCC` (`op_id`),
  KEY `FKBC6EC8BA6849D5F` (`op_superactivityversion`),
  KEY `FKBC6EC8BA7A23912` (`op_activity`),
  KEY `FKBC6EC8B14104F9D` (`op_planversion`),
  KEY `FKBC6EC8B53755975` (`op_responsibleresource`),
  CONSTRAINT `FKBC6EC8B53755975` FOREIGN KEY (`op_responsibleresource`) REFERENCES `op_resource` (`op_id`),
  CONSTRAINT `FKBC6EC8B14104F9D` FOREIGN KEY (`op_planversion`) REFERENCES `op_projectplanversion` (`op_id`),
  CONSTRAINT `FKBC6EC8B2A50EF1F` FOREIGN KEY (`op_category`) REFERENCES `op_activitycategory` (`op_id`),
  CONSTRAINT `FKBC6EC8BA6849D5F` FOREIGN KEY (`op_superactivityversion`) REFERENCES `op_activityversion` (`op_id`),
  CONSTRAINT `FKBC6EC8BA7A23912` FOREIGN KEY (`op_activity`) REFERENCES `op_activity` (`op_id`),
  CONSTRAINT `FKBC6EC8BD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_activityversion`
--


/*!40000 ALTER TABLE `op_activityversion` DISABLE KEYS */;
LOCK TABLES `op_activityversion` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_activityversion` ENABLE KEYS */;

--
-- Table structure for table `op_assignment`
--

DROP TABLE IF EXISTS `op_assignment`;
CREATE TABLE `op_assignment` (
  `op_id` bigint(20) NOT NULL,
  `op_assigned` double default NULL,
  `op_complete` double default NULL,
  `op_baseeffort` double default NULL,
  `op_actualeffort` double default NULL,
  `op_remainingeffort` double default NULL,
  `op_basecosts` double default NULL,
  `op_actualcosts` double default NULL,
  `op_projectplan` bigint(20) default NULL,
  `op_resource` bigint(20) default NULL,
  `op_activity` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK469207EBD958CFCC` (`op_id`),
  KEY `FK469207EBB46C1030` (`op_projectplan`),
  KEY `FK469207EBA7A23912` (`op_activity`),
  KEY `FK469207EBEF1B87B` (`op_resource`),
  CONSTRAINT `FK469207EBEF1B87B` FOREIGN KEY (`op_resource`) REFERENCES `op_resource` (`op_id`),
  CONSTRAINT `FK469207EBA7A23912` FOREIGN KEY (`op_activity`) REFERENCES `op_activity` (`op_id`),
  CONSTRAINT `FK469207EBB46C1030` FOREIGN KEY (`op_projectplan`) REFERENCES `op_projectplan` (`op_id`),
  CONSTRAINT `FK469207EBD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_assignment`
--


/*!40000 ALTER TABLE `op_assignment` DISABLE KEYS */;
LOCK TABLES `op_assignment` WRITE;
INSERT INTO `op_assignment` VALUES (65562,100,50,80,40,40,10400,5200,32787,62,65538),(65565,30,0,372,0,372,48360,0,32787,62,65539),(65598,50,0,80,0,80,10400,0,32787,62,65540),(65603,100,0,0,0,0,0,0,32787,62,65541),(65606,100,0,120,0,120,15600,0,32787,68,65543),(65610,100,0,120,0,120,15600,0,32787,68,65544),(65615,100,0,0,0,0,0,0,32787,62,65545),(65617,100,0,200,0,200,20000,0,32787,80,65547),(65624,100,0,120,0,120,12000,0,32787,83,65548),(65629,100,0,80,0,80,8000,0,32787,80,65549),(65630,100,0,80,0,80,8000,0,32787,83,65549),(65635,100,0,0,0,0,0,0,32787,62,65550),(65638,100,0,400,0,400,40000,0,32787,89,65552),(65639,100,0,400,0,400,40000,0,32787,80,65552),(65650,100,0,320,0,320,32000,0,32787,83,65553),(65651,100,0,320,0,320,32000,0,32787,92,65553),(65660,100,0,160,0,160,16000,0,32787,83,65554),(65661,100,0,160,0,160,16000,0,32787,80,65554),(65668,100,0,280,0,280,19600,0,32787,56,65555),(65676,100,0,0,0,0,0,0,32787,62,65556),(65679,100,0,200,0,200,20000,0,32787,89,65558),(65680,100,0,200,0,200,20000,0,32787,92,65558),(65687,100,0,160,0,160,11200,0,32787,71,65559),(65693,100,0,80,0,80,10400,0,32787,68,65560),(65698,100,0,0,0,0,0,0,32787,62,65561),(98371,100,100,24,24,0,2760,2760,98305,65707,98363),(98373,100,100,24,24,0,3120,3120,98305,65,98364),(98374,100,100,24,24,0,2760,2760,98305,65707,98364),(98378,100,66.6666666666667,40,32,16,5200,4160,98305,65,98365),(98382,100,80,40,32,8,4600,3680,98305,65707,98366),(98386,100,0,80,0,80,10400,0,98305,68,98367),(98391,100,0,160,0,160,18400,0,98305,86,98368),(98398,100,0,80,0,80,9200,0,98305,86,98369),(98403,100,0,0,0,0,0,0,98305,65,98370);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_assignment` ENABLE KEYS */;

--
-- Table structure for table `op_assignmentversion`
--

DROP TABLE IF EXISTS `op_assignmentversion`;
CREATE TABLE `op_assignmentversion` (
  `op_id` bigint(20) NOT NULL,
  `op_assigned` double default NULL,
  `op_complete` double default NULL,
  `op_baseeffort` double default NULL,
  `op_basecosts` double default NULL,
  `op_planversion` bigint(20) default NULL,
  `op_resource` bigint(20) default NULL,
  `op_activityversion` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK2A11668DD958CFCC` (`op_id`),
  KEY `FK2A11668D14104F9D` (`op_planversion`),
  KEY `FK2A11668DEF1B87B` (`op_resource`),
  KEY `FK2A11668DDC13A03E` (`op_activityversion`),
  CONSTRAINT `FK2A11668DDC13A03E` FOREIGN KEY (`op_activityversion`) REFERENCES `op_activityversion` (`op_id`),
  CONSTRAINT `FK2A11668D14104F9D` FOREIGN KEY (`op_planversion`) REFERENCES `op_projectplanversion` (`op_id`),
  CONSTRAINT `FK2A11668DD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FK2A11668DEF1B87B` FOREIGN KEY (`op_resource`) REFERENCES `op_resource` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_assignmentversion`
--


/*!40000 ALTER TABLE `op_assignmentversion` DISABLE KEYS */;
LOCK TABLES `op_assignmentversion` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_assignmentversion` ENABLE KEYS */;

--
-- Table structure for table `op_attachment`
--

DROP TABLE IF EXISTS `op_attachment`;
CREATE TABLE `op_attachment` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) default NULL,
  `op_linked` bit(1) default NULL,
  `op_location` varchar(255) default NULL,
  `op_content` bigint(20) default NULL,
  `op_projectplan` bigint(20) default NULL,
  `op_activity` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK945DDA41FD07045D` (`op_content`),
  KEY `FK945DDA41D958CFCC` (`op_id`),
  KEY `FK945DDA41B46C1030` (`op_projectplan`),
  KEY `FK945DDA41A7A23912` (`op_activity`),
  CONSTRAINT `FK945DDA41A7A23912` FOREIGN KEY (`op_activity`) REFERENCES `op_activity` (`op_id`),
  CONSTRAINT `FK945DDA41B46C1030` FOREIGN KEY (`op_projectplan`) REFERENCES `op_projectplan` (`op_id`),
  CONSTRAINT `FK945DDA41D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FK945DDA41FD07045D` FOREIGN KEY (`op_content`) REFERENCES `op_content` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_attachment`
--


/*!40000 ALTER TABLE `op_attachment` DISABLE KEYS */;
LOCK TABLES `op_attachment` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_attachment` ENABLE KEYS */;

--
-- Table structure for table `op_attachmentversion`
--

DROP TABLE IF EXISTS `op_attachmentversion`;
CREATE TABLE `op_attachmentversion` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) default NULL,
  `op_linked` bit(1) default NULL,
  `op_location` varchar(255) default NULL,
  `op_content` bigint(20) default NULL,
  `op_planversion` bigint(20) default NULL,
  `op_activityversion` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK6E5A6777FD07045D` (`op_content`),
  KEY `FK6E5A6777D958CFCC` (`op_id`),
  KEY `FK6E5A677714104F9D` (`op_planversion`),
  KEY `FK6E5A6777DC13A03E` (`op_activityversion`),
  CONSTRAINT `FK6E5A6777DC13A03E` FOREIGN KEY (`op_activityversion`) REFERENCES `op_activityversion` (`op_id`),
  CONSTRAINT `FK6E5A677714104F9D` FOREIGN KEY (`op_planversion`) REFERENCES `op_projectplanversion` (`op_id`),
  CONSTRAINT `FK6E5A6777D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FK6E5A6777FD07045D` FOREIGN KEY (`op_content`) REFERENCES `op_content` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_attachmentversion`
--


/*!40000 ALTER TABLE `op_attachmentversion` DISABLE KEYS */;
LOCK TABLES `op_attachmentversion` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_attachmentversion` ENABLE KEYS */;

--
-- Table structure for table `op_contact`
--

DROP TABLE IF EXISTS `op_contact`;
CREATE TABLE `op_contact` (
  `op_id` bigint(20) NOT NULL,
  `op_firstname` varchar(255) default NULL,
  `op_lastname` varchar(255) default NULL,
  `op_email` varchar(255) default NULL,
  `op_phone` varchar(255) default NULL,
  `op_mobile` varchar(255) default NULL,
  `op_fax` varchar(255) default NULL,
  `op_user` bigint(20) NOT NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_user` (`op_user`),
  KEY `FK8F0F67C21CAA8918` (`op_user`),
  KEY `FK8F0F67C2D958CFCC` (`op_id`),
  CONSTRAINT `FK8F0F67C2D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FK8F0F67C21CAA8918` FOREIGN KEY (`op_user`) REFERENCES `op_user` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_contact`
--


/*!40000 ALTER TABLE `op_contact` DISABLE KEYS */;
LOCK TABLES `op_contact` WRITE;
INSERT INTO `op_contact` VALUES (2,NULL,NULL,NULL,NULL,NULL,NULL,1),(13,'Claudia','Schulz','','','','',12),(17,'Thomas','Winter','','','','',16),(21,'Hiromi','Sato','','','','',20),(25,'Fredrik','Nieminen','','','','',24),(29,'Mihir','Singh','','','','',28),(33,'Jody','Wang','','','','',32),(37,'Sabine','Hausberg','','','','',36),(41,'Duncan','MacKay','','','','',40),(65711,'Josef','Muster','','','','',65710);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_contact` ENABLE KEYS */;

--
-- Table structure for table `op_content`
--

DROP TABLE IF EXISTS `op_content`;
CREATE TABLE `op_content` (
  `op_id` bigint(20) NOT NULL,
  `op_refcount` int(11) default NULL,
  `op_mediatype` varchar(255) default NULL,
  `op_size` bigint(20) default NULL,
  `op_bytes` mediumblob,
  PRIMARY KEY  (`op_id`),
  KEY `FK8F0F781BD958CFCC` (`op_id`),
  CONSTRAINT `FK8F0F781BD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_content`
--


/*!40000 ALTER TABLE `op_content` DISABLE KEYS */;
LOCK TABLES `op_content` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_content` ENABLE KEYS */;

--
-- Table structure for table `op_dependency`
--

DROP TABLE IF EXISTS `op_dependency`;
CREATE TABLE `op_dependency` (
  `op_id` bigint(20) NOT NULL,
  `op_projectplan` bigint(20) default NULL,
  `op_predecessoractivity` bigint(20) default NULL,
  `op_successoractivity` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK7D55469D958CFCC` (`op_id`),
  KEY `FK7D55469A24B271F` (`op_predecessoractivity`),
  KEY `FK7D554695EF07CBC` (`op_successoractivity`),
  KEY `FK7D55469B46C1030` (`op_projectplan`),
  CONSTRAINT `FK7D55469B46C1030` FOREIGN KEY (`op_projectplan`) REFERENCES `op_projectplan` (`op_id`),
  CONSTRAINT `FK7D554695EF07CBC` FOREIGN KEY (`op_successoractivity`) REFERENCES `op_activity` (`op_id`),
  CONSTRAINT `FK7D55469A24B271F` FOREIGN KEY (`op_predecessoractivity`) REFERENCES `op_activity` (`op_id`),
  CONSTRAINT `FK7D55469D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_dependency`
--


/*!40000 ALTER TABLE `op_dependency` DISABLE KEYS */;
LOCK TABLES `op_dependency` WRITE;
INSERT INTO `op_dependency` VALUES (65597,32787,65538,65539),(65604,32787,65540,65541),(65605,32787,65538,65542),(65614,32787,65543,65544),(65616,32787,65544,65545),(65623,32787,65543,65547),(65628,32787,65544,65548),(65633,32787,65548,65549),(65634,32787,65547,65549),(65636,32787,65549,65550),(65637,32787,65550,65551),(65666,32787,65553,65554),(65667,32787,65552,65554),(65677,32787,65554,65556),(65678,32787,65555,65556),(65686,32787,65552,65558),(65692,32787,65554,65559),(65696,32787,65555,65560),(65697,32787,65556,65560),(65699,32787,65560,65561),(65700,32787,65559,65561),(65701,32787,65558,65561),(98377,98305,98363,98364),(98381,98305,98364,98365),(98385,98305,98364,98366),(98390,98305,98365,98367),(98397,98305,98367,98368),(98402,98305,98368,98369),(98404,98305,98369,98370);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_dependency` ENABLE KEYS */;

--
-- Table structure for table `op_dependencyversion`
--

DROP TABLE IF EXISTS `op_dependencyversion`;
CREATE TABLE `op_dependencyversion` (
  `op_id` bigint(20) NOT NULL,
  `op_planversion` bigint(20) default NULL,
  `op_predecessorversion` bigint(20) default NULL,
  `op_successorversion` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK194B04FD958CFCC` (`op_id`),
  KEY `FK194B04F9644B080` (`op_predecessorversion`),
  KEY `FK194B04F30FFB343` (`op_successorversion`),
  KEY `FK194B04F14104F9D` (`op_planversion`),
  CONSTRAINT `FK194B04F14104F9D` FOREIGN KEY (`op_planversion`) REFERENCES `op_projectplanversion` (`op_id`),
  CONSTRAINT `FK194B04F30FFB343` FOREIGN KEY (`op_successorversion`) REFERENCES `op_activityversion` (`op_id`),
  CONSTRAINT `FK194B04F9644B080` FOREIGN KEY (`op_predecessorversion`) REFERENCES `op_activityversion` (`op_id`),
  CONSTRAINT `FK194B04FD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_dependencyversion`
--


/*!40000 ALTER TABLE `op_dependencyversion` DISABLE KEYS */;
LOCK TABLES `op_dependencyversion` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_dependencyversion` ENABLE KEYS */;

--
-- Table structure for table `op_document`
--

DROP TABLE IF EXISTS `op_document`;
CREATE TABLE `op_document` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) NOT NULL,
  `op_content` bigint(20) default NULL,
  `op_creator` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_name` (`op_name`),
  KEY `FKA80D01B9FD07045D` (`op_content`),
  KEY `FKA80D01B9D958CFCC` (`op_id`),
  KEY `FKA80D01B9FBED317D` (`op_creator`),
  CONSTRAINT `FKA80D01B9FBED317D` FOREIGN KEY (`op_creator`) REFERENCES `op_user` (`op_id`),
  CONSTRAINT `FKA80D01B9D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FKA80D01B9FD07045D` FOREIGN KEY (`op_content`) REFERENCES `op_content` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_document`
--


/*!40000 ALTER TABLE `op_document` DISABLE KEYS */;
LOCK TABLES `op_document` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_document` ENABLE KEYS */;

--
-- Table structure for table `op_dynamicresource`
--

DROP TABLE IF EXISTS `op_dynamicresource`;
CREATE TABLE `op_dynamicresource` (
  `op_id` bigint(20) NOT NULL,
  `op_locale` varchar(255) default NULL,
  `op_name` varchar(255) default NULL,
  `op_value` varchar(255) default NULL,
  `op_object` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `op_dynamicresource_name_i` (`op_name`),
  KEY `FK567BF6EFD958CFCC` (`op_id`),
  KEY `FK567BF6EFF3A8A6F0` (`op_object`),
  CONSTRAINT `FK567BF6EFF3A8A6F0` FOREIGN KEY (`op_object`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FK567BF6EFD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_dynamicresource`
--


/*!40000 ALTER TABLE `op_dynamicresource` DISABLE KEYS */;
LOCK TABLES `op_dynamicresource` WRITE;
INSERT INTO `op_dynamicresource` VALUES (327715,'de',NULL,'Kategorien-Bericht',327717),(327716,'en',NULL,'Category Report',327717);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_dynamicresource` ENABLE KEYS */;

--
-- Table structure for table `op_goal`
--

DROP TABLE IF EXISTS `op_goal`;
CREATE TABLE `op_goal` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) default NULL,
  `op_priority` tinyint(4) default NULL,
  `op_completed` bit(1) default NULL,
  `op_projectnode` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FKB45DAEB1B46A55E2` (`op_projectnode`),
  KEY `FKB45DAEB1D958CFCC` (`op_id`),
  CONSTRAINT `FKB45DAEB1D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FKB45DAEB1B46A55E2` FOREIGN KEY (`op_projectnode`) REFERENCES `op_projectnode` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_goal`
--


/*!40000 ALTER TABLE `op_goal` DISABLE KEYS */;
LOCK TABLES `op_goal` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_goal` ENABLE KEYS */;

--
-- Table structure for table `op_group`
--

DROP TABLE IF EXISTS `op_group`;
CREATE TABLE `op_group` (
  `op_id` bigint(20) NOT NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FKD759BAA13B976501` (`op_id`),
  CONSTRAINT `FKD759BAA13B976501` FOREIGN KEY (`op_id`) REFERENCES `op_subject` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_group`
--


/*!40000 ALTER TABLE `op_group` DISABLE KEYS */;
LOCK TABLES `op_group` WRITE;
INSERT INTO `op_group` VALUES (3);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_group` ENABLE KEYS */;

--
-- Table structure for table `op_groupassignment`
--

DROP TABLE IF EXISTS `op_groupassignment`;
CREATE TABLE `op_groupassignment` (
  `op_id` bigint(20) NOT NULL,
  `op_supergroup` bigint(20) default NULL,
  `op_subgroup` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK46F92C2E98EE01F8` (`op_subgroup`),
  KEY `FK46F92C2ED958CFCC` (`op_id`),
  KEY `FK46F92C2E432F2E1D` (`op_supergroup`),
  CONSTRAINT `FK46F92C2E432F2E1D` FOREIGN KEY (`op_supergroup`) REFERENCES `op_group` (`op_id`),
  CONSTRAINT `FK46F92C2E98EE01F8` FOREIGN KEY (`op_subgroup`) REFERENCES `op_group` (`op_id`),
  CONSTRAINT `FK46F92C2ED958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_groupassignment`
--


/*!40000 ALTER TABLE `op_groupassignment` DISABLE KEYS */;
LOCK TABLES `op_groupassignment` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_groupassignment` ENABLE KEYS */;

--
-- Table structure for table `op_lock`
--

DROP TABLE IF EXISTS `op_lock`;
CREATE TABLE `op_lock` (
  `op_id` bigint(20) NOT NULL,
  `op_owner` bigint(20) default NULL,
  `op_target` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FKB45FF4C940132484` (`op_owner`),
  KEY `FKB45FF4C9D958CFCC` (`op_id`),
  KEY `FKB45FF4C9FC267542` (`op_target`),
  CONSTRAINT `FKB45FF4C9FC267542` FOREIGN KEY (`op_target`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FKB45FF4C940132484` FOREIGN KEY (`op_owner`) REFERENCES `op_user` (`op_id`),
  CONSTRAINT `FKB45FF4C9D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_lock`
--


/*!40000 ALTER TABLE `op_lock` DISABLE KEYS */;
LOCK TABLES `op_lock` WRITE;
INSERT INTO `op_lock` VALUES (327897,1,32781);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_lock` ENABLE KEYS */;

--
-- Table structure for table `op_object`
--

DROP TABLE IF EXISTS `op_object`;
CREATE TABLE `op_object` (
  `op_id` bigint(20) NOT NULL,
  `Created` datetime NOT NULL,
  `Modified` datetime default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `op_object_created_i` (`Created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_object`
--


/*!40000 ALTER TABLE `op_object` DISABLE KEYS */;
LOCK TABLES `op_object` WRITE;
INSERT INTO `op_object` VALUES (1,'2006-05-07 14:29:51','2007-02-06 09:27:22'),(2,'2006-05-07 14:29:51',NULL),(3,'2006-05-07 14:29:51',NULL),(4,'2006-05-07 14:29:51',NULL),(5,'2006-05-07 14:29:52','2007-02-12 15:23:37'),(6,'2006-05-07 14:29:52',NULL),(7,'2006-05-07 14:29:52',NULL),(8,'2006-05-07 14:29:52','2007-02-12 14:52:16'),(9,'2006-05-07 14:29:52',NULL),(10,'2006-05-07 14:29:52',NULL),(11,'2006-05-07 14:32:13',NULL),(12,'2006-05-07 14:32:31','2007-02-06 09:27:22'),(13,'2006-05-07 14:32:31',NULL),(14,'2006-05-07 14:32:31',NULL),(15,'2006-05-07 14:32:31',NULL),(16,'2006-05-07 14:32:47','2007-02-06 09:27:22'),(17,'2006-05-07 14:32:47',NULL),(18,'2006-05-07 14:32:47',NULL),(19,'2006-05-07 14:32:47',NULL),(20,'2006-05-07 14:33:10','2007-02-06 09:27:22'),(21,'2006-05-07 14:33:10',NULL),(22,'2006-05-07 14:33:10',NULL),(23,'2006-05-07 14:33:10',NULL),(24,'2006-05-07 14:34:05','2007-02-06 09:27:22'),(25,'2006-05-07 14:34:05',NULL),(26,'2006-05-07 14:34:05',NULL),(27,'2006-05-07 14:34:05',NULL),(28,'2006-05-07 14:34:25','2007-02-06 09:27:22'),(29,'2006-05-07 14:34:25',NULL),(30,'2006-05-07 14:34:25',NULL),(31,'2006-05-07 14:34:25',NULL),(32,'2006-05-07 14:34:50','2007-02-06 09:27:22'),(33,'2006-05-07 14:34:50',NULL),(34,'2006-05-07 14:34:50',NULL),(35,'2006-05-07 14:34:50',NULL),(36,'2006-05-07 14:35:14','2007-02-06 09:27:22'),(37,'2006-05-07 14:35:14',NULL),(38,'2006-05-07 14:35:14',NULL),(39,'2006-05-07 14:35:14',NULL),(40,'2006-05-07 14:35:44','2007-02-06 09:27:22'),(41,'2006-05-07 14:35:44',NULL),(42,'2006-05-07 14:35:44','2006-05-07 16:41:02'),(43,'2006-05-07 14:35:44',NULL),(44,'2006-05-07 14:38:49','2007-02-12 15:20:16'),(45,'2006-05-07 14:38:49',NULL),(46,'2006-05-07 14:38:49',NULL),(47,'2006-05-07 14:39:02','2007-02-12 15:23:43'),(48,'2006-05-07 14:39:02',NULL),(49,'2006-05-07 14:39:02',NULL),(50,'2006-05-07 14:39:58','2007-02-12 15:31:17'),(51,'2006-05-07 14:39:58',NULL),(52,'2006-05-07 14:39:58',NULL),(53,'2006-05-07 14:40:18','2007-02-12 15:23:30'),(54,'2006-05-07 14:40:18',NULL),(55,'2006-05-07 14:40:18',NULL),(56,'2006-05-07 14:42:12','2007-02-12 15:23:03'),(57,'2006-05-07 14:42:12',NULL),(58,'2006-05-07 14:42:12',NULL),(62,'2006-05-07 14:43:10','2006-05-07 17:20:18'),(63,'2006-05-07 14:43:10',NULL),(64,'2006-05-07 14:43:10',NULL),(65,'2006-05-07 14:43:38','2006-05-07 17:20:09'),(66,'2006-05-07 14:43:38',NULL),(67,'2006-05-07 14:43:38',NULL),(68,'2006-05-07 14:44:14','2006-05-07 14:57:22'),(69,'2006-05-07 14:44:14',NULL),(70,'2006-05-07 14:44:14',NULL),(71,'2006-05-07 14:45:05','2007-02-12 15:22:50'),(72,'2006-05-07 14:45:05',NULL),(73,'2006-05-07 14:45:05',NULL),(80,'2006-05-07 14:49:22','2006-05-07 14:55:17'),(81,'2006-05-07 14:49:22',NULL),(82,'2006-05-07 14:49:22',NULL),(83,'2006-05-07 14:51:13','2007-02-12 15:21:57'),(84,'2006-05-07 14:51:13',NULL),(85,'2006-05-07 14:51:13',NULL),(86,'2006-05-07 14:51:36','2006-05-07 14:59:38'),(87,'2006-05-07 14:51:36',NULL),(88,'2006-05-07 14:51:36',NULL),(89,'2006-05-07 14:52:26','2007-02-12 15:21:22'),(90,'2006-05-07 14:52:26',NULL),(91,'2006-05-07 14:52:26',NULL),(92,'2006-05-07 14:52:36','2007-02-12 15:21:44'),(93,'2006-05-07 14:52:36',NULL),(94,'2006-05-07 14:52:36',NULL),(32768,'2006-05-07 15:23:29','2007-02-12 14:52:51'),(32769,'2006-05-07 15:23:29',NULL),(32770,'2006-05-07 15:23:29',NULL),(32771,'2006-05-07 15:23:52',NULL),(32772,'2006-05-07 15:24:24','2007-02-12 14:52:25'),(32773,'2006-05-07 15:24:24',NULL),(32774,'2006-05-07 15:24:24',NULL),(32775,'2006-05-07 15:24:24',NULL),(32776,'2006-05-07 15:24:24',NULL),(32777,'2006-05-07 15:24:59','2007-02-12 14:57:17'),(32778,'2006-05-07 15:24:59',NULL),(32779,'2006-05-07 15:24:59',NULL),(32780,'2006-05-07 15:24:59',NULL),(32781,'2006-05-07 15:26:37','2007-02-12 14:56:56'),(32782,'2006-05-07 15:26:37','2007-02-12 14:56:56'),(32783,'2006-05-07 15:26:37',NULL),(32784,'2006-05-07 15:26:37',NULL),(32785,'2006-05-07 15:26:37',NULL),(32786,'2006-05-07 15:27:22','2007-02-23 10:25:03'),(32787,'2006-05-07 15:27:22','2007-02-23 10:25:03'),(32788,'2006-05-07 15:27:22',NULL),(32789,'2006-05-07 15:27:22','2006-05-07 16:41:57'),(32790,'2006-05-07 15:27:22',NULL),(32791,'2006-05-07 15:28:20','2007-02-12 14:56:47'),(32792,'2006-05-07 15:28:20','2007-02-12 14:56:47'),(32793,'2006-05-07 15:28:20',NULL),(32794,'2006-05-07 15:28:20',NULL),(32795,'2006-05-07 15:28:20',NULL),(32796,'2006-05-07 15:31:30','2007-02-12 14:57:51'),(32797,'2006-05-07 15:31:30','2007-02-12 14:57:51'),(32798,'2006-05-07 15:31:30',NULL),(32799,'2006-05-07 15:31:30',NULL),(32800,'2006-05-07 15:31:30',NULL),(32801,'2006-05-07 15:32:33','2007-02-12 14:57:42'),(32802,'2006-05-07 15:32:33','2007-02-12 14:57:42'),(32803,'2006-05-07 15:32:33',NULL),(32804,'2006-05-07 15:32:33',NULL),(32805,'2006-05-07 15:32:33',NULL),(32850,'2006-05-07 16:03:11',NULL),(32851,'2006-05-07 16:03:11',NULL),(32852,'2006-05-07 16:03:11',NULL),(32853,'2006-05-07 16:03:11',NULL),(32854,'2006-05-07 16:03:11',NULL),(32855,'2006-05-07 16:03:11',NULL),(32856,'2006-05-07 16:03:11',NULL),(32857,'2006-05-07 16:03:11',NULL),(32858,'2006-05-07 16:03:11',NULL),(32859,'2006-05-07 16:03:11',NULL),(32860,'2006-05-07 16:03:11',NULL),(32861,'2006-05-07 16:03:11',NULL),(32862,'2006-05-07 16:03:11',NULL),(32863,'2006-05-07 16:03:11',NULL),(32864,'2006-05-07 16:03:11',NULL),(32865,'2006-05-07 16:03:11',NULL),(65537,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65538,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65539,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65540,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65541,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65542,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65543,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65544,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65545,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65546,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65547,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65548,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65549,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65550,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65551,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65552,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65553,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65554,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65555,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65556,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65557,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65558,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65559,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65560,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65561,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65562,'2006-05-07 16:40:01','2007-02-06 09:40:39'),(65563,'2006-05-07 16:40:01',NULL),(65564,'2006-05-07 16:40:01',NULL),(65565,'2006-05-07 16:40:01','2006-10-21 12:23:41'),(65566,'2006-05-07 16:40:01',NULL),(65567,'2006-05-07 16:40:01',NULL),(65568,'2006-05-07 16:40:01',NULL),(65569,'2006-05-07 16:40:01',NULL),(65570,'2006-05-07 16:40:01',NULL),(65571,'2006-05-07 16:40:01',NULL),(65572,'2006-05-07 16:40:01',NULL),(65573,'2006-05-07 16:40:01',NULL),(65574,'2006-05-07 16:40:01',NULL),(65575,'2006-05-07 16:40:01',NULL),(65576,'2006-05-07 16:40:01',NULL),(65577,'2006-05-07 16:40:01',NULL),(65578,'2006-05-07 16:40:01',NULL),(65579,'2006-05-07 16:40:01',NULL),(65580,'2006-05-07 16:40:01',NULL),(65581,'2006-05-07 16:40:01',NULL),(65582,'2006-05-07 16:40:01',NULL),(65583,'2006-05-07 16:40:01',NULL),(65584,'2006-05-07 16:40:01',NULL),(65585,'2006-05-07 16:40:01',NULL),(65586,'2006-05-07 16:40:01',NULL),(65587,'2006-05-07 16:40:01',NULL),(65588,'2006-05-07 16:40:01',NULL),(65589,'2006-05-07 16:40:01',NULL),(65590,'2006-05-07 16:40:01',NULL),(65591,'2006-05-07 16:40:01',NULL),(65592,'2006-05-07 16:40:01',NULL),(65593,'2006-05-07 16:40:01',NULL),(65594,'2006-05-07 16:40:01',NULL),(65595,'2006-05-07 16:40:01',NULL),(65596,'2006-05-07 16:40:01',NULL),(65597,'2006-05-07 16:40:01',NULL),(65598,'2006-05-07 16:40:01',NULL),(65599,'2006-05-07 16:40:01',NULL),(65600,'2006-05-07 16:40:01',NULL),(65601,'2006-05-07 16:40:01',NULL),(65602,'2006-05-07 16:40:01',NULL),(65603,'2006-05-07 16:40:01',NULL),(65604,'2006-05-07 16:40:01',NULL),(65605,'2006-05-07 16:40:01',NULL),(65606,'2006-05-07 16:40:01',NULL),(65607,'2006-05-07 16:40:01',NULL),(65608,'2006-05-07 16:40:01',NULL),(65609,'2006-05-07 16:40:01',NULL),(65610,'2006-05-07 16:40:01',NULL),(65611,'2006-05-07 16:40:01',NULL),(65612,'2006-05-07 16:40:01',NULL),(65613,'2006-05-07 16:40:01',NULL),(65614,'2006-05-07 16:40:01',NULL),(65615,'2006-05-07 16:40:01',NULL),(65616,'2006-05-07 16:40:01',NULL),(65617,'2006-05-07 16:40:01',NULL),(65618,'2006-05-07 16:40:01',NULL),(65619,'2006-05-07 16:40:01',NULL),(65620,'2006-05-07 16:40:01',NULL),(65621,'2006-05-07 16:40:01',NULL),(65622,'2006-05-07 16:40:01',NULL),(65623,'2006-05-07 16:40:01',NULL),(65624,'2006-05-07 16:40:01',NULL),(65625,'2006-05-07 16:40:01',NULL),(65626,'2006-05-07 16:40:01',NULL),(65627,'2006-05-07 16:40:01',NULL),(65628,'2006-05-07 16:40:01',NULL),(65629,'2006-05-07 16:40:01',NULL),(65630,'2006-05-07 16:40:01',NULL),(65631,'2006-05-07 16:40:01',NULL),(65632,'2006-05-07 16:40:01',NULL),(65633,'2006-05-07 16:40:01',NULL),(65634,'2006-05-07 16:40:01',NULL),(65635,'2006-05-07 16:40:01',NULL),(65636,'2006-05-07 16:40:01',NULL),(65637,'2006-05-07 16:40:01',NULL),(65638,'2006-05-07 16:40:01',NULL),(65639,'2006-05-07 16:40:01',NULL),(65640,'2006-05-07 16:40:01',NULL),(65641,'2006-05-07 16:40:01',NULL),(65642,'2006-05-07 16:40:01',NULL),(65643,'2006-05-07 16:40:01',NULL),(65644,'2006-05-07 16:40:01',NULL),(65645,'2006-05-07 16:40:01',NULL),(65646,'2006-05-07 16:40:01',NULL),(65647,'2006-05-07 16:40:01',NULL),(65648,'2006-05-07 16:40:01',NULL),(65649,'2006-05-07 16:40:01',NULL),(65650,'2006-05-07 16:40:01',NULL),(65651,'2006-05-07 16:40:01',NULL),(65652,'2006-05-07 16:40:01',NULL),(65653,'2006-05-07 16:40:01',NULL),(65654,'2006-05-07 16:40:01',NULL),(65655,'2006-05-07 16:40:01',NULL),(65656,'2006-05-07 16:40:01',NULL),(65657,'2006-05-07 16:40:01',NULL),(65658,'2006-05-07 16:40:01',NULL),(65659,'2006-05-07 16:40:01',NULL),(65660,'2006-05-07 16:40:01',NULL),(65661,'2006-05-07 16:40:01',NULL),(65662,'2006-05-07 16:40:01',NULL),(65663,'2006-05-07 16:40:01',NULL),(65664,'2006-05-07 16:40:01',NULL),(65665,'2006-05-07 16:40:01',NULL),(65666,'2006-05-07 16:40:01',NULL),(65667,'2006-05-07 16:40:01',NULL),(65668,'2006-05-07 16:40:01',NULL),(65669,'2006-05-07 16:40:01',NULL),(65670,'2006-05-07 16:40:01',NULL),(65671,'2006-05-07 16:40:01',NULL),(65672,'2006-05-07 16:40:01',NULL),(65673,'2006-05-07 16:40:01',NULL),(65674,'2006-05-07 16:40:01',NULL),(65675,'2006-05-07 16:40:01',NULL),(65676,'2006-05-07 16:40:01',NULL),(65677,'2006-05-07 16:40:01',NULL),(65678,'2006-05-07 16:40:01',NULL),(65679,'2006-05-07 16:40:01',NULL),(65680,'2006-05-07 16:40:01',NULL),(65681,'2006-05-07 16:40:01',NULL),(65682,'2006-05-07 16:40:01',NULL),(65683,'2006-05-07 16:40:01',NULL),(65684,'2006-05-07 16:40:01',NULL),(65685,'2006-05-07 16:40:01',NULL),(65686,'2006-05-07 16:40:01',NULL),(65687,'2006-05-07 16:40:01',NULL),(65688,'2006-05-07 16:40:01',NULL),(65689,'2006-05-07 16:40:01',NULL),(65690,'2006-05-07 16:40:01',NULL),(65691,'2006-05-07 16:40:01',NULL),(65692,'2006-05-07 16:40:01',NULL),(65693,'2006-05-07 16:40:01',NULL),(65694,'2006-05-07 16:40:01',NULL),(65695,'2006-05-07 16:40:01',NULL),(65696,'2006-05-07 16:40:01',NULL),(65697,'2006-05-07 16:40:01',NULL),(65698,'2006-05-07 16:40:01',NULL),(65699,'2006-05-07 16:40:01',NULL),(65700,'2006-05-07 16:40:01',NULL),(65701,'2006-05-07 16:40:01',NULL),(65702,'2006-05-07 16:41:57',NULL),(65703,'2006-05-07 16:41:57',NULL),(65704,'2006-05-07 16:41:57',NULL),(65705,'2006-05-07 16:41:57',NULL),(65706,'2006-05-07 16:41:57',NULL),(65707,'2006-05-07 17:21:29','2006-05-07 17:22:03'),(65708,'2006-05-07 17:21:29',NULL),(65709,'2006-05-07 17:21:29',NULL),(65710,'2006-05-07 17:21:48','2007-02-06 09:27:22'),(65711,'2006-05-07 17:21:48',NULL),(65712,'2006-05-07 17:21:48',NULL),(65713,'2006-05-07 17:21:48',NULL),(98304,'2006-05-07 17:38:48','2007-02-23 10:24:50'),(98305,'2006-05-07 17:38:48','2007-02-23 10:24:50'),(98306,'2006-05-07 17:38:48',NULL),(98308,'2006-05-07 17:38:48',NULL),(98309,'2006-05-07 17:38:48',NULL),(98332,'2006-05-07 17:44:00',NULL),(98333,'2006-05-07 17:44:00',NULL),(98334,'2006-05-07 17:44:00',NULL),(98335,'2006-05-07 17:44:00',NULL),(98336,'2006-05-07 17:44:00',NULL),(98337,'2006-05-07 17:44:00',NULL),(98338,'2006-05-07 17:44:00',NULL),(98339,'2006-05-07 17:44:00',NULL),(98363,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98364,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98365,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98366,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98367,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98368,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98369,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98370,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98371,'2006-05-07 17:48:40','2006-05-07 17:55:23'),(98372,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98373,'2006-05-07 17:48:40','2006-05-07 17:59:48'),(98374,'2006-05-07 17:48:40','2006-05-07 17:57:01'),(98375,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98376,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98377,'2006-05-07 17:48:40',NULL),(98378,'2006-05-07 17:48:40','2006-05-07 17:59:48'),(98379,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98380,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98381,'2006-05-07 17:48:40',NULL),(98382,'2006-05-07 17:48:40','2006-05-07 17:57:01'),(98383,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98384,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98385,'2006-05-07 17:48:40',NULL),(98386,'2006-05-07 17:48:40','2006-10-21 12:23:42'),(98387,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98388,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98389,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98390,'2006-05-07 17:48:40',NULL),(98391,'2006-05-07 17:48:40',NULL),(98392,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98393,'2006-05-07 17:48:40',NULL),(98394,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98395,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98396,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98397,'2006-05-07 17:48:40',NULL),(98398,'2006-05-07 17:48:40',NULL),(98399,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98400,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98401,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98402,'2006-05-07 17:48:40',NULL),(98403,'2006-05-07 17:48:40',NULL),(98404,'2006-05-07 17:48:40',NULL),(131072,'2006-05-07 17:55:23',NULL),(131073,'2006-05-07 17:55:24',NULL),(131074,'2006-05-07 17:55:24',NULL),(131075,'2006-05-07 17:57:01',NULL),(131076,'2006-05-07 17:57:01',NULL),(131077,'2006-05-07 17:57:01',NULL),(131080,'2006-05-07 17:59:10',NULL),(131081,'2006-05-07 17:59:10',NULL),(131082,'2006-05-07 17:59:48',NULL),(131083,'2006-05-07 17:59:48',NULL),(131084,'2006-05-07 17:59:48',NULL),(196608,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196609,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196611,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196612,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196613,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196614,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196615,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196616,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196617,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196618,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196619,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196620,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196621,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196622,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196623,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196624,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196625,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196626,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196627,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196628,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196629,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196630,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196631,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196632,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196633,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196634,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196635,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196636,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196637,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196638,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196639,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196640,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196641,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196642,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196643,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196646,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196647,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196648,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196649,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196650,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196651,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196652,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(229376,'2007-02-06 09:40:39',NULL),(229377,'2007-02-06 09:40:39',NULL),(262144,'2007-02-06 09:53:33',NULL),(262145,'2007-02-06 09:53:33',NULL),(262147,'2007-02-06 09:53:38','2007-02-12 14:00:14'),(262148,'2007-02-06 09:53:38','2007-02-12 14:00:14'),(262149,'2007-02-06 09:59:48',NULL),(262150,'2007-02-06 10:00:03','2007-02-12 14:31:32'),(262151,'2007-02-06 10:00:22','2007-02-12 14:25:20'),(262152,'2007-02-06 10:00:39','2007-02-12 14:26:45'),(294912,'2007-02-06 10:22:42','2007-02-12 14:35:36'),(294913,'2007-02-06 10:23:23','2007-02-12 14:36:21'),(294914,'2007-02-06 10:23:33','2007-02-12 14:37:49'),(294915,'2007-02-06 10:23:43','2007-02-12 14:40:14'),(327715,'2007-02-12 15:25:50',NULL),(327716,'2007-02-12 15:25:50',NULL),(327717,'2007-02-12 15:25:50',NULL),(327897,'2007-02-12 15:37:42',NULL),(327898,'2007-02-12 15:37:44','2007-02-12 16:07:29');
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_object` ENABLE KEYS */;

--
-- Table structure for table `op_permission`
--

DROP TABLE IF EXISTS `op_permission`;
CREATE TABLE `op_permission` (
  `op_id` bigint(20) NOT NULL,
  `op_object` bigint(20) default NULL,
  `op_subject` bigint(20) default NULL,
  `op_accesslevel` tinyint(4) default NULL,
  `op_systemmanaged` bit(1) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FKEA8C46EDD958CFCC` (`op_id`),
  KEY `FKEA8C46EDF3A8A6F0` (`op_object`),
  KEY `FKEA8C46ED1C497196` (`op_subject`),
  CONSTRAINT `FKEA8C46ED1C497196` FOREIGN KEY (`op_subject`) REFERENCES `op_subject` (`op_id`),
  CONSTRAINT `FKEA8C46EDD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FKEA8C46EDF3A8A6F0` FOREIGN KEY (`op_object`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_permission`
--


/*!40000 ALTER TABLE `op_permission` DISABLE KEYS */;
LOCK TABLES `op_permission` WRITE;
INSERT INTO `op_permission` VALUES (6,5,3,2,'\0'),(7,5,1,64,'\0'),(9,8,3,2,'\0'),(10,8,1,64,'\0'),(45,44,1,64,'\0'),(46,44,3,2,'\0'),(48,47,1,64,'\0'),(49,47,3,2,'\0'),(51,50,1,64,'\0'),(52,50,3,2,'\0'),(54,53,1,64,'\0'),(55,53,3,2,'\0'),(57,56,1,64,'\0'),(58,56,3,2,'\0'),(63,62,1,64,'\0'),(64,62,3,2,'\0'),(66,65,1,64,'\0'),(67,65,3,2,'\0'),(69,68,1,64,'\0'),(70,68,3,2,'\0'),(72,71,1,64,'\0'),(73,71,3,2,'\0'),(81,80,1,64,'\0'),(82,80,3,2,'\0'),(84,83,1,64,'\0'),(85,83,3,2,'\0'),(87,86,1,64,'\0'),(88,86,3,2,'\0'),(90,89,1,64,'\0'),(91,89,3,2,'\0'),(93,92,1,64,'\0'),(94,92,3,2,'\0'),(32769,32768,1,64,'\0'),(32770,32768,3,2,'\0'),(32771,32768,40,16,'\0'),(32773,32772,1,64,'\0'),(32774,32772,16,16,'\0'),(32775,32772,12,16,'\0'),(32776,32772,3,2,'\0'),(32778,32777,1,64,'\0'),(32779,32777,40,16,'\0'),(32780,32777,3,2,'\0'),(32783,32781,1,64,'\0'),(32784,32781,40,16,'\0'),(32785,32781,3,2,'\0'),(32788,32786,1,64,'\0'),(32790,32786,3,2,'\0'),(32793,32791,1,64,'\0'),(32794,32791,40,16,'\0'),(32795,32791,3,2,'\0'),(32798,32796,1,64,'\0'),(32799,32796,40,16,'\0'),(32800,32796,3,2,'\0'),(32803,32801,1,64,'\0'),(32804,32801,40,16,'\0'),(32805,32801,3,2,'\0'),(32851,32786,36,4,''),(32853,32786,12,4,''),(32855,32786,40,4,''),(32857,32786,20,4,''),(32859,32786,32,4,''),(32861,32786,28,4,''),(65702,32786,12,16,'\0'),(65708,65707,1,64,'\0'),(65709,65707,3,2,'\0'),(98306,98304,1,64,'\0'),(98308,98304,16,16,'\0'),(98309,98304,3,2,'\0'),(98333,98304,16,4,''),(98335,98304,40,4,''),(98337,98304,24,4,''),(98339,98304,65710,4,'');
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_permission` ENABLE KEYS */;

--
-- Table structure for table `op_preference`
--

DROP TABLE IF EXISTS `op_preference`;
CREATE TABLE `op_preference` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) default NULL,
  `op_value` varchar(255) default NULL,
  `op_user` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FKB2633DF91CAA8918` (`op_user`),
  KEY `FKB2633DF9D958CFCC` (`op_id`),
  CONSTRAINT `FKB2633DF9D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FKB2633DF91CAA8918` FOREIGN KEY (`op_user`) REFERENCES `op_user` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_preference`
--


/*!40000 ALTER TABLE `op_preference` DISABLE KEYS */;
LOCK TABLES `op_preference` WRITE;
INSERT INTO `op_preference` VALUES (14,'Locale','de',12),(18,'Locale','en',16),(22,'Locale','de',20),(26,'Locale','de',24),(30,'Locale','en',28),(34,'Locale','en',32),(38,'Locale','de',36),(42,'Locale','de',40),(65712,'Locale','de',65710),(262147,'Locale','en',1),(262148,'ShowHours','false',1);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_preference` ENABLE KEYS */;

--
-- Table structure for table `op_projectnode`
--

DROP TABLE IF EXISTS `op_projectnode`;
CREATE TABLE `op_projectnode` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) NOT NULL,
  `op_type` tinyint(4) NOT NULL,
  `op_description` varchar(255) default NULL,
  `op_start` date default NULL,
  `op_finish` date default NULL,
  `op_budget` double default NULL,
  `op_supernode` bigint(20) default NULL,
  `op_templatenode` bigint(20) default NULL,
  `op_status` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_name` (`op_name`),
  KEY `FKEB70673DD684B9C4` (`op_supernode`),
  KEY `FKEB70673DA37403E5` (`op_status`),
  KEY `FKEB70673DD958CFCC` (`op_id`),
  KEY `FKEB70673DD8A44DBF` (`op_templatenode`),
  CONSTRAINT `FKEB70673DD8A44DBF` FOREIGN KEY (`op_templatenode`) REFERENCES `op_projectnode` (`op_id`),
  CONSTRAINT `FKEB70673DA37403E5` FOREIGN KEY (`op_status`) REFERENCES `op_projectstatus` (`op_id`),
  CONSTRAINT `FKEB70673DD684B9C4` FOREIGN KEY (`op_supernode`) REFERENCES `op_projectnode` (`op_id`),
  CONSTRAINT `FKEB70673DD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_projectnode`
--


/*!40000 ALTER TABLE `op_projectnode` DISABLE KEYS */;
LOCK TABLES `op_projectnode` WRITE;
INSERT INTO `op_projectnode` VALUES (8,'${RootProjectPortfolioName}',1,'${RootProjectPortfolioDescription}',NULL,NULL,0,NULL,NULL,NULL),(32768,'Development',1,'',NULL,NULL,0,8,NULL,NULL),(32772,'Consulting',1,'',NULL,NULL,0,8,NULL,NULL),(32777,'Organization',1,'',NULL,NULL,0,8,NULL,NULL),(32781,'SuperWiz',3,'','2006-06-05','2006-11-24',0,32768,NULL,294913),(32786,'Virtual Cockpit v2',3,'','2007-01-01','2007-08-31',0,32768,NULL,294914),(32791,'ALS v5',3,'Automatic Landing System','2006-07-03','2006-12-29',0,32768,NULL,294912),(32796,'Relocation (new HQ)',3,'','2006-08-14','2006-08-25',0,32777,NULL,294912),(32801,'Marketing Plan',3,'','2006-01-02','2006-12-29',0,32777,NULL,294912),(98304,'Implementation ALS/Xplore',3,'','2007-02-05','2007-03-31',0,32772,NULL,294912);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_projectnode` ENABLE KEYS */;

--
-- Table structure for table `op_projectnodeassignment`
--

DROP TABLE IF EXISTS `op_projectnodeassignment`;
CREATE TABLE `op_projectnodeassignment` (
  `op_id` bigint(20) NOT NULL,
  `op_resource` bigint(20) default NULL,
  `op_projectnode` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK1B05C5CAB46A55E2` (`op_projectnode`),
  KEY `FK1B05C5CAD958CFCC` (`op_id`),
  KEY `FK1B05C5CAEF1B87B` (`op_resource`),
  CONSTRAINT `FK1B05C5CAEF1B87B` FOREIGN KEY (`op_resource`) REFERENCES `op_resource` (`op_id`),
  CONSTRAINT `FK1B05C5CAB46A55E2` FOREIGN KEY (`op_projectnode`) REFERENCES `op_projectnode` (`op_id`),
  CONSTRAINT `FK1B05C5CAD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_projectnodeassignment`
--


/*!40000 ALTER TABLE `op_projectnodeassignment` DISABLE KEYS */;
LOCK TABLES `op_projectnodeassignment` WRITE;
INSERT INTO `op_projectnodeassignment` VALUES (32850,56,32786),(32852,62,32786),(32854,68,32786),(32856,71,32786),(32858,80,32786),(32860,83,32786),(32862,89,32786),(32864,92,32786),(98332,65,98304),(98334,68,98304),(98336,86,98304),(98338,65707,98304);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_projectnodeassignment` ENABLE KEYS */;

--
-- Table structure for table `op_projectplan`
--

DROP TABLE IF EXISTS `op_projectplan`;
CREATE TABLE `op_projectplan` (
  `op_id` bigint(20) NOT NULL,
  `op_start` date NOT NULL,
  `op_finish` date NOT NULL,
  `op_calculationmode` tinyint(4) default NULL,
  `op_progresstracked` bit(1) default NULL,
  `op_template` bit(1) default NULL,
  `op_projectnode` bigint(20) NOT NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_projectnode` (`op_projectnode`),
  KEY `op_projectplan_finish_i` (`op_finish`),
  KEY `op_projectplan_start_i` (`op_start`),
  KEY `FKEB714464B46A55E2` (`op_projectnode`),
  KEY `FKEB714464D958CFCC` (`op_id`),
  CONSTRAINT `FKEB714464D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FKEB714464B46A55E2` FOREIGN KEY (`op_projectnode`) REFERENCES `op_projectnode` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_projectplan`
--


/*!40000 ALTER TABLE `op_projectplan` DISABLE KEYS */;
LOCK TABLES `op_projectplan` WRITE;
INSERT INTO `op_projectplan` VALUES (32782,'2006-02-06','2006-11-24',1,'','\0',32781),(32787,'2006-05-01','2006-05-12',1,'','\0',32786),(32792,'2006-01-09','2006-12-29',1,'','\0',32791),(32797,'2006-08-14','2006-08-25',2,'\0','\0',32796),(32802,'2006-01-02','2006-12-29',2,'\0','\0',32801),(98305,'2006-04-10','2006-04-12',2,'','\0',98304);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_projectplan` ENABLE KEYS */;

--
-- Table structure for table `op_projectplanversion`
--

DROP TABLE IF EXISTS `op_projectplanversion`;
CREATE TABLE `op_projectplanversion` (
  `op_id` bigint(20) NOT NULL,
  `op_versionnumber` int(11) default NULL,
  `op_comment` varchar(255) default NULL,
  `op_start` date NOT NULL,
  `op_finish` date NOT NULL,
  `op_template` bit(1) default NULL,
  `op_projectplan` bigint(20) default NULL,
  `op_creator` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `op_projectplanversion_finish_i` (`op_finish`),
  KEY `op_projectplanversion_start_i` (`op_start`),
  KEY `FKFC44DFF4D958CFCC` (`op_id`),
  KEY `FKFC44DFF4FBED317D` (`op_creator`),
  KEY `FKFC44DFF4B46C1030` (`op_projectplan`),
  CONSTRAINT `FKFC44DFF4B46C1030` FOREIGN KEY (`op_projectplan`) REFERENCES `op_projectplan` (`op_id`),
  CONSTRAINT `FKFC44DFF4D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FKFC44DFF4FBED317D` FOREIGN KEY (`op_creator`) REFERENCES `op_user` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_projectplanversion`
--


/*!40000 ALTER TABLE `op_projectplanversion` DISABLE KEYS */;
LOCK TABLES `op_projectplanversion` WRITE;
INSERT INTO `op_projectplanversion` VALUES (327898,-1,NULL,'2006-02-06','2006-11-24','\0',32782,1);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_projectplanversion` ENABLE KEYS */;

--
-- Table structure for table `op_projectstatus`
--

DROP TABLE IF EXISTS `op_projectstatus`;
CREATE TABLE `op_projectstatus` (
  `op_id` bigint(20) NOT NULL,
  `op_sequence` int(11) NOT NULL,
  `op_name` varchar(255) NOT NULL,
  `op_description` text,
  `op_color` int(11) default NULL,
  `op_active` bit(1) default NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_name` (`op_name`),
  KEY `FKD9C1266DD958CFCC` (`op_id`),
  CONSTRAINT `FKD9C1266DD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_projectstatus`
--


/*!40000 ALTER TABLE `op_projectstatus` DISABLE KEYS */;
LOCK TABLES `op_projectstatus` WRITE;
INSERT INTO `op_projectstatus` VALUES (294912,0,'	Acquisition ',NULL,15,''),(294913,1,'Assignment',NULL,14,''),(294914,2,'Realization',NULL,16,''),(294915,3,'Acceptance',NULL,10,'');
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_projectstatus` ENABLE KEYS */;

--
-- Table structure for table `op_report`
--

DROP TABLE IF EXISTS `op_report`;
CREATE TABLE `op_report` (
  `op_id` bigint(20) NOT NULL,
  `op_type` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK25EC1552AC77FEAB` (`op_id`),
  KEY `FK25EC155239869D81` (`op_type`),
  CONSTRAINT `FK25EC155239869D81` FOREIGN KEY (`op_type`) REFERENCES `op_reporttype` (`op_id`),
  CONSTRAINT `FK25EC1552AC77FEAB` FOREIGN KEY (`op_id`) REFERENCES `op_document` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_report`
--


/*!40000 ALTER TABLE `op_report` DISABLE KEYS */;
LOCK TABLES `op_report` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_report` ENABLE KEYS */;

--
-- Table structure for table `op_reporttype`
--

DROP TABLE IF EXISTS `op_reporttype`;
CREATE TABLE `op_reporttype` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) NOT NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_name` (`op_name`),
  KEY `FK7C93AB8CD958CFCC` (`op_id`),
  CONSTRAINT `FK7C93AB8CD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_reporttype`
--


/*!40000 ALTER TABLE `op_reporttype` DISABLE KEYS */;
LOCK TABLES `op_reporttype` WRITE;
INSERT INTO `op_reporttype` VALUES (327717,'categoryreport');
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_reporttype` ENABLE KEYS */;

--
-- Table structure for table `op_resource`
--

DROP TABLE IF EXISTS `op_resource`;
CREATE TABLE `op_resource` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) NOT NULL,
  `op_description` varchar(255) default NULL,
  `op_available` double default NULL,
  `op_inheritpoolrate` bit(1) default NULL,
  `op_hourlyrate` double default NULL,
  `op_user` bigint(20) default NULL,
  `op_pool` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_name` (`op_name`),
  KEY `FK605BF4AC1CAA8918` (`op_user`),
  KEY `FK605BF4ACD958CFCC` (`op_id`),
  KEY `FK605BF4ACD7E91845` (`op_pool`),
  CONSTRAINT `FK605BF4ACD7E91845` FOREIGN KEY (`op_pool`) REFERENCES `op_resourcepool` (`op_id`),
  CONSTRAINT `FK605BF4AC1CAA8918` FOREIGN KEY (`op_user`) REFERENCES `op_user` (`op_id`),
  CONSTRAINT `FK605BF4ACD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_resource`
--


/*!40000 ALTER TABLE `op_resource` DISABLE KEYS */;
LOCK TABLES `op_resource` WRITE;
INSERT INTO `op_resource` VALUES (56,'sh','Sabine Hausberg',100,'',70,36,50),(62,'cs','Claudia Schulz',100,'',130,12,53),(65,'tw','Thomas Winter',100,'',130,16,53),(68,'dmk','Duncan MacKay (CA)',100,'\0',130,40,44),(71,'hs','Hiromi Sato',100,'',70,20,50),(80,'jw','Jody Wang',100,'',100,32,44),(83,'ms','Mihir Singh',100,'',100,28,44),(86,'fn','Fredrik Nieminen',100,'',115,24,47),(89,'ee1','External Engineer 1',100,'',100,40,44),(92,'ee2','External Engineer 2',100,'',100,40,44),(65707,'jm','Josef Muster',100,'',115,65710,47);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_resource` ENABLE KEYS */;

--
-- Table structure for table `op_resourcepool`
--

DROP TABLE IF EXISTS `op_resourcepool`;
CREATE TABLE `op_resourcepool` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) NOT NULL,
  `op_description` varchar(255) default NULL,
  `op_hourlyrate` double default NULL,
  `op_superpool` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_name` (`op_name`),
  KEY `FK32EE56C8311305C4` (`op_superpool`),
  KEY `FK32EE56C8D958CFCC` (`op_id`),
  CONSTRAINT `FK32EE56C8D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FK32EE56C8311305C4` FOREIGN KEY (`op_superpool`) REFERENCES `op_resourcepool` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_resourcepool`
--


/*!40000 ALTER TABLE `op_resourcepool` DISABLE KEYS */;
LOCK TABLES `op_resourcepool` WRITE;
INSERT INTO `op_resourcepool` VALUES (5,'${RootResourcePoolName}','${RootResourcePoolDescription}',0,NULL),(44,'Engineer','',100,5),(47,'Consultants','',115,5),(50,'QA & Documentation','',70,5),(53,'Project Manager','',130,5);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_resourcepool` ENABLE KEYS */;

--
-- Table structure for table `op_schedule`
--

DROP TABLE IF EXISTS `op_schedule`;
CREATE TABLE `op_schedule` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) NOT NULL,
  `op_description` varchar(255) default NULL,
  `op_start` date default NULL,
  `op_unit` int(11) default NULL,
  `op_interval` int(11) default NULL,
  `op_mask` int(11) default NULL,
  `op_lastexecuted` date default NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_name` (`op_name`),
  KEY `FK4B16C335D958CFCC` (`op_id`),
  CONSTRAINT `FK4B16C335D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_schedule`
--


/*!40000 ALTER TABLE `op_schedule` DISABLE KEYS */;
LOCK TABLES `op_schedule` WRITE;
INSERT INTO `op_schedule` VALUES (4,'report-archive-schedule',NULL,'2006-05-07',2,8,0,'2007-02-11');
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_schedule` ENABLE KEYS */;

--
-- Table structure for table `op_schema`
--

DROP TABLE IF EXISTS `op_schema`;
CREATE TABLE `op_schema` (
  `op_version` int(11) default NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_schema`
--


/*!40000 ALTER TABLE `op_schema` DISABLE KEYS */;
LOCK TABLES `op_schema` WRITE;
INSERT INTO `op_schema` VALUES (4);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_schema` ENABLE KEYS */;

--
-- Table structure for table `op_setting`
--

DROP TABLE IF EXISTS `op_setting`;
CREATE TABLE `op_setting` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) NOT NULL,
  `op_value` varchar(255) default NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_name` (`op_name`),
  KEY `FKCCB751F2D958CFCC` (`op_id`),
  CONSTRAINT `FKCCB751F2D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_setting`
--


/*!40000 ALTER TABLE `op_setting` DISABLE KEYS */;
LOCK TABLES `op_setting` WRITE;
INSERT INTO `op_setting` VALUES (11,'Allow_EmptyPassword','true'),(262144,'Calendar_DayWorkTime','8.0'),(262145,'Calendar_WeekWorkTime','40.0');
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_setting` ENABLE KEYS */;

--
-- Table structure for table `op_site`
--

DROP TABLE IF EXISTS `op_site`;
CREATE TABLE `op_site` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FKB4630EE5D958CFCC` (`op_id`),
  CONSTRAINT `FKB4630EE5D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_site`
--


/*!40000 ALTER TABLE `op_site` DISABLE KEYS */;
LOCK TABLES `op_site` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_site` ENABLE KEYS */;

--
-- Table structure for table `op_subject`
--

DROP TABLE IF EXISTS `op_subject`;
CREATE TABLE `op_subject` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) NOT NULL,
  `op_displayname` varchar(255) default NULL,
  `op_description` varchar(255) default NULL,
  PRIMARY KEY  (`op_id`),
  UNIQUE KEY `op_name` (`op_name`),
  KEY `FKE70298CED958CFCC` (`op_id`),
  CONSTRAINT `FKE70298CED958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_subject`
--


/*!40000 ALTER TABLE `op_subject` DISABLE KEYS */;
LOCK TABLES `op_subject` WRITE;
INSERT INTO `op_subject` VALUES (1,'Administrator','${AdministratorDisplayName}','${AdministratorDescription}'),(3,'Everyone','${EveryoneDisplayName}','${EveryoneDescription}'),(12,'cs','Claudia Schulz',''),(16,'tw','Thomas Winter',''),(20,'hs','Hiromi Sato',''),(24,'fn','Fredrik Nieminen',''),(28,'ms','Mihir Singh',''),(32,'jw','Jody Wang',''),(36,'sh','Sabine Hausberg',''),(40,'dmk','Duncan MacKay',''),(65710,'jm','Josef Muster','');
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_subject` ENABLE KEYS */;

--
-- Table structure for table `op_todo`
--

DROP TABLE IF EXISTS `op_todo`;
CREATE TABLE `op_todo` (
  `op_id` bigint(20) NOT NULL,
  `op_name` varchar(255) default NULL,
  `op_priority` tinyint(4) default NULL,
  `op_completed` bit(1) default NULL,
  `op_due` date default NULL,
  `op_projectnode` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FKB46397E4B46A55E2` (`op_projectnode`),
  KEY `FKB46397E4D958CFCC` (`op_id`),
  CONSTRAINT `FKB46397E4D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FKB46397E4B46A55E2` FOREIGN KEY (`op_projectnode`) REFERENCES `op_projectnode` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_todo`
--


/*!40000 ALTER TABLE `op_todo` DISABLE KEYS */;
LOCK TABLES `op_todo` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_todo` ENABLE KEYS */;

--
-- Table structure for table `op_user`
--

DROP TABLE IF EXISTS `op_user`;
CREATE TABLE `op_user` (
  `op_id` bigint(20) NOT NULL,
  `op_password` varchar(255) default NULL,
  `op_authenticationtype` tinyint(4) default NULL,
  `op_level` tinyint(4) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FKB4641B693B976501` (`op_id`),
  CONSTRAINT `FKB4641B693B976501` FOREIGN KEY (`op_id`) REFERENCES `op_subject` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_user`
--


/*!40000 ALTER TABLE `op_user` DISABLE KEYS */;
LOCK TABLES `op_user` WRITE;
INSERT INTO `op_user` VALUES (1,'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709',0,2),(12,'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709',0,2),(16,'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709',0,2),(20,'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709',0,2),(24,'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709',0,2),(28,'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709',0,2),(32,'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709',0,2),(36,'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709',0,2),(40,'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709',0,2),(65710,'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709',0,2);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_user` ENABLE KEYS */;

--
-- Table structure for table `op_userassignment`
--

DROP TABLE IF EXISTS `op_userassignment`;
CREATE TABLE `op_userassignment` (
  `op_id` bigint(20) NOT NULL,
  `op_user` bigint(20) default NULL,
  `op_group` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FKF53732F61CAA8918` (`op_user`),
  KEY `FKF53732F6771B6BBC` (`op_group`),
  KEY `FKF53732F6D958CFCC` (`op_id`),
  CONSTRAINT `FKF53732F6D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`),
  CONSTRAINT `FKF53732F61CAA8918` FOREIGN KEY (`op_user`) REFERENCES `op_user` (`op_id`),
  CONSTRAINT `FKF53732F6771B6BBC` FOREIGN KEY (`op_group`) REFERENCES `op_group` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_userassignment`
--


/*!40000 ALTER TABLE `op_userassignment` DISABLE KEYS */;
LOCK TABLES `op_userassignment` WRITE;
INSERT INTO `op_userassignment` VALUES (15,12,3),(19,16,3),(23,20,3),(27,24,3),(31,28,3),(35,32,3),(39,36,3),(43,40,3),(65713,65710,3);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_userassignment` ENABLE KEYS */;

--
-- Table structure for table `op_workperiod`
--

DROP TABLE IF EXISTS `op_workperiod`;
CREATE TABLE `op_workperiod` (
  `op_id` bigint(20) NOT NULL,
  `op_start` date default NULL,
  `op_baseeffort` double default NULL,
  `op_workingdays` bigint(20) default NULL,
  `op_projectplan` bigint(20) default NULL,
  `op_activity` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK49A6D30D958CFCC` (`op_id`),
  KEY `FK49A6D30B46C1030` (`op_projectplan`),
  KEY `FK49A6D30A7A23912` (`op_activity`),
  CONSTRAINT `FK49A6D30A7A23912` FOREIGN KEY (`op_activity`) REFERENCES `op_activity` (`op_id`),
  CONSTRAINT `FK49A6D30B46C1030` FOREIGN KEY (`op_projectplan`) REFERENCES `op_projectplan` (`op_id`),
  CONSTRAINT `FK49A6D30D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_workperiod`
--


/*!40000 ALTER TABLE `op_workperiod` DISABLE KEYS */;
LOCK TABLES `op_workperiod` WRITE;
INSERT INTO `op_workperiod` VALUES (196608,'2007-07-08',2.4,1741659966,32787,65539),(196609,'2007-04-06',16,524188921,32787,65552),(196611,'2007-08-08',4,999,32787,65540),(196612,'2007-05-07',2.4,1944571807,32787,65539),(196613,'2006-12-03',8,1610612736,32787,65538),(196614,'2007-02-03',8,1335836284,32787,65547),(196615,'2007-04-06',8,524188921,32787,65555),(196616,'2007-06-07',16,3,32787,65552),(196617,'2007-03-06',16,2013265920,32787,65553),(196618,'2007-05-07',8,3999,32787,65555),(196619,'2007-03-06',8,15,32787,65547),(196620,'2007-01-03',8,999,32787,65538),(196621,'2007-04-06',2.4,524188921,32787,65539),(196622,'2007-07-08',4,1741651968,32787,65540),(196623,'2007-07-08',8,7998,32787,65560),(196624,'2007-03-06',16,2013265920,32787,65552),(196625,'2007-05-07',16,511903,32787,65553),(196626,'2007-05-07',16,1944571807,32787,65552),(196627,'2007-06-07',16,1048377840,32787,65558),(196628,'2007-08-08',2.4,999,32787,65539),(196629,'2007-03-06',2.4,2046027727,32787,65539),(196630,'2007-01-03',2.4,2096754688,32787,65539),(196631,'2007-02-03',8,1333788672,32787,65548),(196632,'2007-03-06',8,2013265920,32787,65555),(196633,'2007-03-06',8,1999,32787,65548),(196634,'2007-04-06',16,524188921,32787,65553),(196635,'2007-06-07',16,1048377840,32787,65554),(196636,'2007-02-03',8,2047612,32787,65544),(196637,'2007-02-03',2.4,1335836284,32787,65539),(196638,'2007-06-07',2.4,1048377843,32787,65539),(196639,'2007-01-03',8,2096754688,32787,65543),(196640,'2007-07-08',16,62,32787,65558),(196641,'2007-03-06',16,32759808,32787,65549),(196642,'2007-07-08',8,131047230,32787,65559),(196643,'2007-02-03',8,1325400064,98305,98366),(196646,'2007-03-06',4,10191,98305,98367),(196647,'2007-02-03',8,1325400064,98305,98365),(196648,'2007-04-06',8,20871168,98305,98369),(196649,'2007-03-06',8,2046017536,98305,98368),(196650,'2007-02-03',16,9961472,98305,98364),(196651,'2007-04-06',8,1273,98305,98368),(196652,'2007-02-03',8,458752,98305,98363);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_workperiod` ENABLE KEYS */;

--
-- Table structure for table `op_workperiodversion`
--

DROP TABLE IF EXISTS `op_workperiodversion`;
CREATE TABLE `op_workperiodversion` (
  `op_id` bigint(20) NOT NULL,
  `op_start` date default NULL,
  `op_baseeffort` double default NULL,
  `op_workingdays` bigint(20) default NULL,
  `op_planversion` bigint(20) default NULL,
  `op_activityversion` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FKE96E79A8D958CFCC` (`op_id`),
  KEY `FKE96E79A814104F9D` (`op_planversion`),
  KEY `FKE96E79A8DC13A03E` (`op_activityversion`),
  CONSTRAINT `FKE96E79A8DC13A03E` FOREIGN KEY (`op_activityversion`) REFERENCES `op_activityversion` (`op_id`),
  CONSTRAINT `FKE96E79A814104F9D` FOREIGN KEY (`op_planversion`) REFERENCES `op_projectplanversion` (`op_id`),
  CONSTRAINT `FKE96E79A8D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_workperiodversion`
--


/*!40000 ALTER TABLE `op_workperiodversion` DISABLE KEYS */;
LOCK TABLES `op_workperiodversion` WRITE;
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_workperiodversion` ENABLE KEYS */;

--
-- Table structure for table `op_workrecord`
--

DROP TABLE IF EXISTS `op_workrecord`;
CREATE TABLE `op_workrecord` (
  `op_id` bigint(20) NOT NULL,
  `op_actualeffort` double default NULL,
  `op_remainingeffort` double default NULL,
  `op_remainingeffortchange` double default NULL,
  `op_personnelcosts` double default NULL,
  `op_travelcosts` double default NULL,
  `op_materialcosts` double default NULL,
  `op_externalcosts` double default NULL,
  `op_miscellaneouscosts` double default NULL,
  `op_comment` varchar(255) default NULL,
  `op_completed` bit(1) default NULL,
  `op_assignment` bigint(20) default NULL,
  `op_workslip` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `FK7FD63C0D958CFCC` (`op_id`),
  KEY `FK7FD63C07AE4913E` (`op_workslip`),
  KEY `FK7FD63C0CE18738E` (`op_assignment`),
  CONSTRAINT `FK7FD63C0CE18738E` FOREIGN KEY (`op_assignment`) REFERENCES `op_assignment` (`op_id`),
  CONSTRAINT `FK7FD63C07AE4913E` FOREIGN KEY (`op_workslip`) REFERENCES `op_workslip` (`op_id`),
  CONSTRAINT `FK7FD63C0D958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_workrecord`
--


/*!40000 ALTER TABLE `op_workrecord` DISABLE KEYS */;
LOCK TABLES `op_workrecord` WRITE;
INSERT INTO `op_workrecord` VALUES (131072,24,0,-24,2760,0,0,0,0,NULL,'',98371,131074),(131073,16,8,-16,1840,0,0,0,0,NULL,'\0',98374,131074),(131075,8,0,-8,2760,0,0,0,0,NULL,'',98374,131077),(131076,32,8,-32,3680,0,0,0,0,NULL,'\0',98382,131077),(131080,16,8,-16,2080,0,0,0,0,NULL,'\0',98373,131081),(131082,8,0,-8,3120,0,0,0,0,NULL,'',98373,131084),(131083,32,16,-24,4160,0,0,0,0,NULL,'\0',98378,131084),(229376,40,40,-40,5200,0,0,0,0,NULL,'\0',65562,229377);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_workrecord` ENABLE KEYS */;

--
-- Table structure for table `op_workslip`
--

DROP TABLE IF EXISTS `op_workslip`;
CREATE TABLE `op_workslip` (
  `op_id` bigint(20) NOT NULL,
  `op_number` int(11) default NULL,
  `op_date` date default NULL,
  `op_creator` bigint(20) default NULL,
  PRIMARY KEY  (`op_id`),
  KEY `op_workslip_date_i` (`op_date`),
  KEY `FK76D1F06FD958CFCC` (`op_id`),
  KEY `FK76D1F06FFBED317D` (`op_creator`),
  CONSTRAINT `FK76D1F06FFBED317D` FOREIGN KEY (`op_creator`) REFERENCES `op_user` (`op_id`),
  CONSTRAINT `FK76D1F06FD958CFCC` FOREIGN KEY (`op_id`) REFERENCES `op_object` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `op_workslip`
--


/*!40000 ALTER TABLE `op_workslip` DISABLE KEYS */;
LOCK TABLES `op_workslip` WRITE;
INSERT INTO `op_workslip` VALUES (131074,1,'2006-04-28',65710),(131077,2,'2006-05-05',65710),(131081,1,'2006-04-28',16),(131084,2,'2006-05-05',16),(229377,1,'2007-01-08',12);
UNLOCK TABLES;
/*!40000 ALTER TABLE `op_workslip` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


