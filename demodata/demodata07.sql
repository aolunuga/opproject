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
INSERT INTO `op_content` VALUES (327714,1,'application/pdf',73392,'%PDF-1.4\n%����\n3 0 obj <</Filter/FlateDecode/Type/XObject/Length 14832/BitsPerComponent 8/Height 229/ColorSpace/DeviceGray/Subtype/Image/Width 1318>>stream\nx��]m�� ��I��I@�$T�$�`*a�0	�0	�ݭk!�\'��ݻ�?ﾵ	!�)��pX�X{s����9gmm���/(((�)��Ҹ���7{��������0���y����������:	ANz���9�u�<Ǝ�]���PPPP���-�!{����9)(((ȎL�e�Q�_0����:O�p������Zm������tT����7nf����ԬF�/��sXPPP��q�r�m�u.\n\n\nt�>��_ΓƘ�>�&x�P�>�ں���.���<����8��3���l�[��l��{<9��e�.q�w���B��\Zl��U[��#@�l]���	�t����Q�������1V�#��ZW[碠�	v:�q�ڴ/����x�[盇�F�sy+�\n6�9�\rj��_���N��l���awu�x6�)��-QG�������r�q�u�63\'�d�L2�^%��2Q^���37�l�ӵ͟��uR�����^�\";<J��`T��vk���Q�Of�<�I�ӝ�I�Gk��sY���,��jk�q��ߜ,yY���b(zvʭ�:�P-���Lñ�ͻ8�X�{�B�v����DAb�[o�ۂ?du����	��P��X��f\'\0-6u�#�(<Y�A8�N���T�Yn�Œ��Ɩ[�>��G�ɂ��Ej�W�,�,����%�l->6۲Um4��eG��P}�[[)�i\\�&��%�.KSψ�6����G3ܪMr\\���\"��lm��\r)9X��|P�-,n��a?�タ�T��WPa�(�\'K���@�P��L�͎f�Q�,�V ���i��\0�IgI�O�����]t��G�\\��열[)�GXrW��s|�&cQR>�{��<�I,�B~��d�O��I�4����7J|���ql�*�n�	#�\0K�$?F��Z㷟�s����B�w�O\\�%wN�*��|���A-��4�]$�>K�$?RdIk��k���W�=��ثK!��,j���ح�bm����\0M��m�5z���l57�=Qh�`]��d[��iR�̒��`e��}*��@�O�� ��ɂ�A^%���eɝn&�ꊛ�$��1\0g��7Y�2��9��lm����r\\�ݠ��S\'�1�EN`��I�)���q4�Ꞹ}��k��q_�q�V����3ڔ\ZaJ�&\n\0�ɒN�h7�Z��B�Ts��DP�\\�,(���\"K\n[��X�c%�O\'ǀ��(=�!��9�cɯ�4�*�FbA��e���ał�%�ƒ�5��a3�� �Td�)�������`-�\\>�C������ۍ$�Nf�l����Z,��+ee�ޱLJf]=��\Z���,`%�D���;V/d^;,\Z�=�~�:7gk�fr�x%g.�	xcv�U�eQݛVmb��S�\r�X\r�W���%��uX�\'%;�4o�!�T�}���w�W:���7��㹽ج����:����:w�%�Ւ^Fel�Z�0{Im�~��[,�	]�l�tTg��I>^�%�j;�9�\r��\Z���20�N�������M��+q���o��y5J��\Zd���ˊ=ޣ�7�N<\\�1�ҩ��m��r�c>�Mm\0���|�&*��\n��#{�o��*�<��jbC&{nZ4�ɶ��o���m#;)�Yq��Ñ#l�c\r���-�PFKm���F���tu=]����-}������KѸ*ʎ<�},����#�\\u��b�x�(-d����w�#;X����)�:�U���r�2�0@�yN^�$X�$�r��J;J�ϯ��F���w%i7��Z�:��0/������Z���;#C��8a��?P9���J��!��-�b�̉_[��Ue,�}V��@�s�;�ν���H�1�E�ǣA���|T㌒�R�Do����-g��&>`�Evf��)I��	�R�ĳ\0�VAg9���8��l�K�Jd>�X�v�Lt�*com��,^z��r�2�;��L��f&�j4)NՅ��|*[�0�s�}?:�)V%��W��iH;��N8{�f&7eI�W�ÓiJ=��tY��uJ|Q��sֆ\'�L�{%I\'f<�U�����U�T9R��P�$�;<Km�I��sr�>nc���0���%O2INW;�ϼA.��ݚHݱD��[@���X8�\nWC��L�4�^he�I�fIu��\'I�����gq��F�ks$�C@ڈ:@ٰ���a2�L��|J�)�pL��s��o	�C��Vhg���`��L�8߾�\ZϘ0B�V���}�m�4S;Є�J��\0�$�lI��J��g�K6�	�U�����|K7�2*��m1�\0�R���7:���M\nMa�ϳv�j4[��z\n<(Sd�bٺ9B�:���\0/rGT�h.��f^,��+��Le�M����\0�V�c��S���.+�}���Ao�Oe���j(S�`\"<���^���Ìꑻ@Bk��-�ɰ����X�����udU�ɣX�],O&G�@�k.pwhՆ,������o��T�f�+�`rۇ82O֒@�(F�Z�Z���v�;xΪ;�g���nWH�L�R���t\r�NLg�?*_pyJ��|�b���7��8`Q�]!H?z��CG�����C��%9W�0��b�a�̱�\"\Z`�2/�ڙl�t֏�.#վ�2K�Ct��5����\"΢�*�A����fD0�\'�ҏ�%�U�Fj�N�!���!�2r��l�	-��\'������N��ҋ��U���~�$� ���	� :-�V6����U������D��jZg=x�8�\rfJ01�Y̒X�I�i��SX������7��_�U����Y�{�PP�O���4\"@���Unm�#���IF)�3X�ɏ_Y\Z��#\n-�V��ô_�U���&U_��e�z�:���G��F�~��\'�g���z�QA~Ծ�\"$���^z3j�8��Z�b��V%����[yG��\n�é\rr�p;ų���I���=��ܕ�#n��nhM�%��Aj�=1K��0x%�M�},7d�ܯ&�7��v�D9��ͯJ8��8�c��hHrc�QE؊�$\"�Na�3xqXbg����Y�	,��L�f�D�~�D��%as�u\rc,�^�2mde<�(�>��.��٘���d����h��$��	�9�c�4�HtpB#x����ޕ�y@�����D�	��$)� �D�BR��Pg�k����!fIu��\'�Ѐ�q+�H\\8#�~�D�Z�DVT��L���Ʉ�8��a���-�$��`i3#0�$���`\n&[Nc\0\'I��\nYr9\Z\"~P�\\�G��bl�#����<X���Õ�D,�G8^Z�6n�\"���#&�S|��ǒIܥ��J4)6������	�\0ҝ?��$t���S�8�\n��s_3�w`z�0G��R����]K��0�qx�`�\r���j�ƒI!���{4)V���Q�ə���HX���@u�(ư���A\'��@#�� �P_����0!�QC�������Q�dY(K6x��ܮ�o0���ި��X��_����������F�9��Irb�ӧ��d��Д�t\r���(K&�ݬyW{\nMj�o�cܵ\0�V��Q�LPiX!�̆ \r���X����zZ��-(�x�z�Fr�4t�MXp�=�$���bɔ��\Zέ��m��@.��vI�a{�z�{j�>��Š�%�S�q���?�Ҥ�%X-�3���s#��Nׁ\r@1z��x.�L�u��č6��kJ�U�\0f�̥P��)a�%�N|��d�C1d��b	��j&�娅��Y\0����s$�A��6�L,	߫\ZB��d��~q�S�R��!D��6şb�ZfT]�\'q���Q�R�%O�5p�m��Y#���0�� b���	�y\r�sm����{]O������#|A�!�_bIi�uh�aB�Y�Vl�v&!��s>�\'XH�ە@�&Z	\"f�D��J���)�L,�pCg�\"��V��	+K�!�_b�Zh�;����RC��C0/b�v�/6ĝv\r�v��7��T� \"e�4��������6K��Ho-~�M\r���XR�[\n*;�A:Y�N@�\r+���z��|�lV��XXwU���tP���\'�VN8K��@�^>	�|K����r�����i�~Ok�bA�x��ciJj�Z�+����=^Pgr�3���Қ:+2\"e�$�\'qN �r�yX��rF�^Ҍ�bs��Z�cn���]��h��y��F*����}Ԑ�[�1��ƻgU�ӿذ���!C��Y�0R�	�aI-	-jF#,�#V�s�ż�C,i�� Z����\r��Xi�W_��}�Ǭbc��kC0��!�(s8D֊3� j9�,,�>ý�F-��f�o��P���O\\���eBnR��\Z�+UĴ�6���i�\0l��H�A��\\���t���T�\n8�,,��-,T�ne�����-W-��YF�\\�5#�)��Ķ �~�bc9b���\r�Gn�����`= ��!2�N�i�[��,	e�@�*]��ڸR���-�sK*��s�j!�WrM01��bT�<-Z�&�c�z+i�$�C��Yy6��\'��%Ŀr�u�:�NۙTU�ӝ���k��� �)&$�vV��O?\n`�⛏�\"w�h�Կ�MK�Fnm�D����:=�*U��\"�Ē�i�*�T����(t:#�B�S�R���@{78T���d2mV�R��x�\"�ߘ�4�L�cP\'̧D��=��lΒ�i�hOJ�3b@��S��\rԲN��9�-�Ɠ��U-�i�N*��\")�\Z�&#x7y��Z��D��%�{�/�y\"��V�r!�$PFN�3B�el��ܞ/cI����Yӽ�4�ˁ��a�TL�lΒ-b@��P@2�O��4�u�9�OLL����a�nX��2��]=���;E�K���T�������s�5K*q��	�y�����d��@�r4���ݰ�s�3nV��y���c��$X�\0Q+���,�G���X��S73��W�@�F�\\t�8��k%7��X��\0\"V��53�~�`L�H&�2$9+���,�[���OPv�z��Vu�7��x���r��%��BR����XR1�f^ aI�qF5�\"�ː�<[�$~��5�Щ�`]��\ZP�@\'&�d@;��ؿ�d�|@�*�2@y�$�O&�2$9�d��,�d+O���*N�nȭ\"2t��j�Ѐv���<����d^�!�c��$X�$����,�Z���l5��`k�nn�,�\0�T�rg���΅%;x�)�%�Y!#p�I0�C�H&�2$W�Lm͒H���\0�ZT�j݄g�\\!���V�FZAL+����H������2�N�#\Z��,�c5~�S雁]�n5�,P!����I��(�ݰd�fbx�+Dr{g 	�]�`��l�\'$ȅ���Y�_I[�^��h�香�5�5��j���\rKn{�F��>|�Թ��gN(������sz��l��YRU�y��s�0C51�@�loT��٥�XX��g>\"bn��&A%��r���]0�1|#KZV]�Q�������z/��6���yք�nXr�C@: ��u����.�ꈌ��sz�6f�I�;���bvȭ���ʳRf�c��y�%wÒP�H��E�}�\r����+X\'fJ�oψ���sz�6fI$W���5^spnT�J!�1�LM6p�5%��\\��&b���U;�퐷������:A$S�h�Ȓ�6���s؜�V@!(]�c��>B���K:@\"�p ^��ߠ�?vܰ\"���`A�2t�N/\Z��$�Uh���D5H)�y�V&�Cph�N|?,�,�} �R�+�!7��\Z^Ǧ�lJv��� 	D��):�h_Ȓ�Fe�\0\\��(KJ65��M\nH�)���$4��h�آ�2P�M�v����:�@٭|1D��):�h_Ȓ��(���9��H89��d#H��#�KB��O-� ����{r������b���H����%-�L�����gY��\nRq�6� ]�(�KB�u�i�\0�0\Z�X휬\'�;B4��;8H�!RF�$#\Z��Ē�v3~T����\"J.~��$�V�*�.;bII�����$m�h��rk�3�	l�	��3z\'0�1�$�l�T�{��.Cv�蚥;)�!���K�={�A��Y{6�P\n��Vj��f65����9`W��*\"f�D�^4��Y2�ɕ�)�ƒ\\\n9u!�m�l�#��:�ɱ�*X���nd箶6��De�-�>k���Z����\0�3t�N/\Z��,oH�|XN�ǒ�� �~镜p�i���\'��:5µ\\�`�D,�̑����z�g��X�1�$ɍd��ѻ����� ��� ��\\6�:�����wW,�U2٘;f��5�b%��AH�\r���K���A�I��:\"h�t�^4�ĤU��\\������%���Q%$���Ej���$�&a������Q�i0��F`�z�+�5��H~AC\'���1�ϒѡ�+�S��\Z�,���t���8�����%���8M�}0Bb��@x�V���?�^�\n�J+E$\r�4�2#\Z��,5\nɕ�)[�%U!�XkA4�D���(ύ���X<�t�v�VՈ�N̎wl�\r/��[�̷3�w,��9��2�8�h�dKɕ�)���YBʁ�ύ.��ƪ����mi�l_,�~?[��\0�$U����#�_�*3I-hһna}��6\"khQ��!�:C���H�|�#����u���b�V��Z��%�K�{�\0Z�@]TG	�⍇�$�1��������!uz�Ҫ3\"����\\��iW��w�@uҠ%�#����]�K�������ֹ��b7uo,�nLY����Dn-���ixgWW)1=7�g<�se����n�_ZD�Т��h`�Wyb��z�$H�u�9�0�!�i|U���ʃ�Y5*^\r�-@=�ƒ�/��V�������a#fh����ET�YRB����lp��ԍ����RD�t�NY�̭�:��\\��+��\n7��h�-��+�$ĩ͓�,h�E�ݱ�����:Guia�4B`�w}o�<�����j֧p?{�����NlKܞ��%�<\\;��M�`�J3⾺*c�EBA�g]@[B�ǒ�@�ms1�zfꫤ��GD&dEl[��&���FZ��EcH��)7���\\`��\'/�ZU��EB#�6�4,OI��C�N���p�rܤ�܂L���8M�W�\r�uz�ҪsR��@����L��o��.��n�x�	v1�F��%U4�;q�)|,m�G��3aabQahQ��!�:��cj��%�2�2��s��/��[	x��Y�c4ɓ�g��|��3:KۈC�:�h�չE��%��9����U���%볟|Ȳ�`)�O�$�<�4`�p�\'�$�l,PG�Z��EcH��)��&�o�M�==jN��h��\"�ݭ��F`�>Y2��������:�\Zs�0����@��-���Q� &��)Cn���~IS\0/��UꬼH��Lw(�(D�~Yr���q?!\Z��Է��с�f ��\\9�h��1�A�z$@d����`9ͺS�\"Y�4Vn,�H�[���`!��\"/����[�;	t$A/Z��E��ALJ���UʹEn�O�L�Qu_ڴ��4̇yȒ{f�Ug-�B҈��N]���;[�7��#�.C�:�h��9�D�Z<�=�Æ�Uܪ��K�<f�\n��7K��_�P��|���1�;X>C��m�uz�������9�vi-2C�`�O����K�wѳ(�J�ʼ]��y.�+������1Ò�p*y�:<)��\"�-���1�Wg��}{���E��Is\0��U��2qF�@`\\���z�ݳd~�lk$UDS�jj�W5�Q��q�^$� \Z\r-���1d��j�|Vg]-�ӄ1w�kmU\n��@$��_���o��{��;��q��%ӈ<�#�8i��sp�w$^2��ӋƐ�:����\"w|���KѪ��n��!���js��R���,��ئ9��͠)\"�&�ky2�#_P]���VҔ��uz��Tg�N+4_b�@{\'6���xd����.Ng��-���+X�\nD��WN��\"i�-�V���Fs�	��ТN/\ZC��[t}ګ���.�)#�E�A��(6���Y�Y���oa���u��Y���+GY�]E�-���|�*��!�\r-�ˈƐ�:���wx�ORe�����h��Fg����$�cӡ�]L.[��zW`���.q��\'	H.����ɂ�9�F81~+��=i�3I�ТN/\Z�\0b�K4|�,ژ�u�9MFIr#W���F:����sv�u��z+@��)^���\Z��QN[��������Nu�7������&�\'�h���	5��$�IMn!�X�J�1�C�iw���i_�ќ�@���l��r�8��鹖v�s7{	�|\0�3���\"����-�&	N�bo�fK�4��-<?;T}5�:���:���y��n������w�\'q���֢��_��-�r�D*͢�cn�48�w����UNLFN\\��;�K�\Z����,�?�J���{���\Z���ǹt�U��}�iɂ}�4[�X�Kh7�G�@��.��uJ�\r,�S@*��6����!�ţ��]ԡ�Lg�P_\n��lmc�.��Fb��q��uF\"���ұ��)�߀Ԛ�m,�\'\"4���>h�bg�A6�T�[ �fk�	}80��2ߕ뚺�$���~�xb��\0�6[�X�O�����>*cL}���Gό^�W�\'�T��m,�)Z5�=�M���< �fkv�����Ԥ	�Zˀ��Ro���`�H��0~\0;)��H��+���ԛ�m,�+R��X��~��l)/�ԛ�m,�+��h5�L�j�{�\n�H���Ƃ�B�s���h2ay���^�ǀT��m,�-�BZ�&�H����@j��6�)�7?��d�vm��oQ���9[�X�_�$N�O�Is�?��<��ۀԜ�m,�/��xd������\0Ru���`�hy��˱I4�l*��T��m,�1R��?�l��h�s��N������{F�JM?�<���u�\'LC\n�7佰���!�3�g�}L8�=�t%\n\n�@�NO?6�;yN�H��dAA�:�ЙD����r9l(]ɂ��u�栨�v\"�Ma2�������,��_4�&�����3�����`��CS�De�IW�8��,((X&Q���kQ�M���:�)(((8����р�U��ny��wAA�zH�3�mW�O6�����TPP�wq�KY�p��X�\\nyY���*((X�{vZw��TC\ZGc.��JJe馠�`e�Vᮏ�n������7�}k�|�����3bZ�������άy?Xg$��q�h���Z����T{�b�fk;h����:���x��x��k���4\\M>�;���m.�X*�\'5>�ʏ�!z��$�ݱ$s��^g�?;���q6S*��k�����n!�f�M��n寖#�(,	bo,�����_���N�_�,q�N�>�Ǫ��϶�w!�),	bg,��3$��TN4�W����	��?�:��bb���g[��!w(,	b_,�&��fm�ܑw����;Y��dS?þ���I��MNa?H�Pq��:�ޯGc�\'�2_\Z9Ca�4��1,9<����c��W� ��^\\�\']���	�	�=~YY�u:�G������%W�ƹ������0�KvY���Mۦ&�+�u�l=d��<DY�p컓�MX�+bO,�w%���07�I8�����_8�k��J0Y��:���a�RgMr;�\\{b�X�����i���Xѓ/��Vp����N+���/1�K��=�dW����\rL�k�K�|3�j\'7a�N-��|+4���s_ϕ���)���u$k��ٲ��{�,\r-K��n��>����Opz*.�^d��K<�|q�~��ڈ�s羒M������EWy\\�-Mgh�F��z�[�%aEO�Q���[��[~kJm���֙�� {��uz��G2���X������P�\\\'@݅���tQ87�b1��i��#��it��+Mԓ\Z����Dou���$�/�c���|��a�%͵�*f�\Z�s��N�.nj�\"kұ~�~�_>te���P2���A�p]�Lw����1�+}��edWm����d4�����,�j�K���ɞ�[��Ö������5���|	�o�q�b�q��+G��vto��.����FE鏵�@G[��^~xS�{��(�,i��W�ت�(e����ɻ����Ē�N[���Ta�,�$sO{��*<�cO��X>;��~�w��࢘�Ao���}.�lw\rK��<����Yx�ЍK���^	k�E�%��>��_lv�f�)=�v�onb���p�>\Za��V~�b�����L�ͼ��d��gI���f�����+�/����ۿM��\Zu*������y{��J_W�����8,���j~?:��΁�\0:�t�+�³d�dfа8:\'���^A�Zʲ��cqN�a�LTCg����\Z�`\\��y��.�>l/7=?�\'\'��Y��VΜ¥]�,��^v�ӘWa�<R��CV��Ǚ�{Ef����[���W_�a����\n��EsC�L��rF�?���b�+N����߰��%{�f-i`�Y�9�?�����ˇ���3�d���;hOX娠�t3^i��5!3k\r��\"�_�����±��V?uT�f��p���W��j����\\{��b^	,DYr�Gmw�=3H,�]�f�=�c.÷�ô�u\Z�\\\'�ot��ˠލ琬�ފ�m�S���ױ�1;�7���{�h�ɲӠC�i��\"�^Ϡ�Xv��<�x����^է{溺4��3��l�=����.�s�\ZJ�q=W�d��*tKX�gJW�X�oUV��b��ԮSN?l�����O��>շٗoG�f�i��s�KE9���~��~��m��pl�k��q����O�޼�w�|MU!��?������7p���{,��\ZE��	l���d�ۜ��a�f���۔7M}��e�3��Ab�0��;s��\Zw�,��/���y�^��|�P��.�ۇW�,N7k�ϵC����N�+�p�v�#�4��e����|	��PÒ}Y�{T}�2x��~�uT�fo�Β�0e�����n<����Մ����E�D݋�#��o)�\n�Ȓv�k_�A�s9�2}0�̚0�Gq�*-�#K�.�k���/����H�B��&�Ti�reO�A��5�t�&׊��:����.X3�e�x��\r\'O͒��IV������,�#e6	KV���Jؐ/�-{~���7�$�l��޿^{?G���!�Xr��dr���s�O�I���`\ng�v4l6�����|�O�9�ɷeb_5�x#R�Qe?�.ǒ�Af����ʍ�=��j��\\���ps�Xҽ���턘H���,��[E�������!��w��fIƔA��\'����v���ް����1e�H�Ɗ�oU���#6����?L[�\ZG��pg�v����0A#���Ñp�wGS�I�e�S|���mP���e�z�ݢ	��qWФ��Dt���xt�Iל���=����[��6�%{a�}k��eK�x���Ϳ�im�dL6����y��^�Sl�����=��������t�Q{y|����+��Oh����u���;;M��O@m���N�W�=��1��6���1mW5���h�<��l�����rTs�LM\n�h(�n�#<���[��t2�Ɇ���I�=9��g{�7���RR�+�$��ǈ��w�����cX�ɞڅ-�Jw�8���J@�v3�L�ܙ�WB,���&#�G�jDl�b�@�b�>��#�]z�Y]������,IOOP�6�	�ߡ`�wX3���ܒ��:zͱ�`Re�wd��G�\r���דYU�����)VfI����Q��l���߻�@�����:�7�Gf	y����i�޹�+\n���U$�(�ev�\'���xPNS��q�_�ԧg�%[����e.�YU:y��LN��м��i��77êy�.;���Ї��oUF}K��.ن�yt��D�|QI�̒}���]H��`:L/;�FԔ������W�fzwe����%�l��B`q4���o�\Z92�(��\'�4�WeyY�M����6���{�=a2o�{[^C=�.���0��K�z�F��d,��F=\"�ВGf�3�^$VfI�d8��9��[�U;{��n4���8\"����/�6�>�|,Y�ʻ�k�F��	䠆�\Z]\r�׼,�>�dv���I|v�^��w�[.��8]nn�0��I*�)cAD�ң�R�.��%���ϨG�,�(\r�f�@�i�W-e��5Y�^*&N��cI��):��=�c�|?���?���Vv#�g㾎���3��.h�Ǌ�E��z��s;�[C3�u�(d,��F=��,IU4g�5�MY��XA�/a���ڬi}�%;B|\r�_sدd��l���P��j�67���t���n&\'�Z�ຄ2�|��?�EX��gF�ӗo�aɖ�����h���q���3<���2��1Hh6���������6��g�?�%jbd}�{�/l�^�t}�W\'vp�����_s4sϻ�>\'Ģk�T���f,�T�Yd�\\}�KJ!c�g���@n\Z��$�?\0�|��GX#��!��/=V��T�{�Áɒ��@�|�t�m��h(Kr�DWo���]?�X��l�y�<�z�T~^W��͒ɱ���E�aԽ�h�	ץ����gT9�5=��-ۻ��2KJLy�����\rC%�q�m!V����F�j�$K�ٔ������lꇏT����0�	��]ɟ�ڄ�ԃ��\n��R\0v\Z�`~�֌�p]b\nK>cw�O�ُ���`Y��׺�7�]�%#--Dߵ����_L� W��HĊ���4�Â|,�emݽ%�g�p1\"�\"Dǒ�~�W�r ���n?e�ɾ8�^CC;<V���^-Ɛ8�`lU�.9��%�1���l�\r�eiB��:��U��3CVfɾ=��K\\6�;���ҿs��[�5[͕�*V���s#g��h(�$�Mx��#^f�i\Zr+�L��gW��㬀�kª�e��vx,y>���.]��a��$SJft>�ǒl���钛y��ٳMXcILĔ��j���,9���+�)����.C|5����A���\r�>�g~�u�|,Ʌ��f����\0�FSä�G�f�JXB}�yt9�v�g?^nt�3��{�X��E�4z�f��f\'?2#��u)*d,��w��t���nC�Q�g�\ZY�	���Y�&��Y�Ld�u�z�w����K�O�+��L3V����PF��\Z�8r�0�G���!��O �DQ@R��b��Gկ\n1~���\Z{j�1`XH�$?Ժ ����,f�-��Q�ѻG!c�C��z�밠�݆��t���W�ǵYr�@<��o��/x����:���gz�7��������~;V�}�\Z�3_�Ȓs�.8_1�\nw�i~���=2�g����#�GW��S\'s{u(�=�C��Xr�hx5��j��O���#9��]\r�X�i��g�d�\ZT6�\r�뿓�*N���Ӧ<�&�}�D,9�2�ɻ��}�yL�we\\��$�������%�u���Xq�jf�!�v9Y���6|(w���Qѣ�9�t�>C�[0��o�`2�2�׺\'�p=�N=�Ƈ(�n�3,�Ŋ����;�	��&��|\0�e�k��iN��r�p�x��6h\0�K�z�X�� ����ã�L�O�úr#�@�P�^��O���8���>z+�tSl�&���c�G�l�\\{�����C5�d<Z�C��x��X�^��ʒc/�\Z�/�ey\ZQ���`��v��X	\Z	l(��ψ�y������`F&�\nCߟ^�\Z8�Ml����2����5^#A^-�\n�k�WE�Ӭ,99*pw�\0&ęt�\rMk��]���u4Aw��\0a��\ZtÎ�%\'��<\\3\r�Lx�����`v\'v*�aX~�z:֜�ރ�v)t	[&�o��������F�O̒��n�I�P/,)��0�ҹ�:);�m�k�$�p�q� ������iw�j��GW5lX��c�y\rM#�çx�M��j��4/K2�Bͥs,��r�8҅z� <5���e���g.#\'�`M�����xh<���e�\Z�2v�\r���rhhE���X��Pb����!C���	x�\'	�+N�͉�����K!�8��3.~��\r��8&����y�m��+�����=��Y��ow�Φ,%Hn�j&�cY�-da���߿p��k�����[O�M��K����=�z����\\a;��=	l�-��כ2�u�ܢZ����K:o��u���C�k���aR=����!7�Ob��s�Iߜ�T���H�ڟ]]��S:>?����47K>c�ͼu��a�_���7M�A_5\r��Y�YS��;�����{7	�W~*��T���s��2x�ΞX,��eޙy�(EY�&)�s�r��忶�Q	�H���q�M>�&x���#��0���-�H�],�QC3�Ў���`�W�\rs��� 7����B�%\':��,OL۽|��{zF���X��t�&r�l�_�p8N�����?^o��R��8��\\�&-��畍���\\��L��\ZqZ5��:������8�ӭ�ⷆ�����7ř謦������X��Oaߌ�BΥj1.��M�,������_E6����dL�����N��e����i��������ׅ�����AV���Fa�����)]�E�o��t���+��u(U�k�e��G���0k�]ZS�������yn����a����az<�LKt(,�\'p|���l\n;h�$�8�[���dA���_�$z�,(qt=����ãeqs;��z�\nendstream\nendobj\n4 0 obj <</Filter/FlateDecode/Type/XObject/Length 56065/SMask 3 0 R/BitsPerComponent 8/Height 229/ColorSpace[/CalRGB<</WhitePoint[0.95043 1 1.09]/Matrix[0.41237 0.21264 0.01933 0.35759 0.71518 0.1192 0.18047 0.07218 0.95049]/Gamma[2.2 2.2 2.2]>>]/Subtype/Image/Width 1318>>stream\nx��ݏl�U\'����	�o��|xix�#Q%�#x�����`�8��i�-[#�M���E�Z\\�-ɸǪ�4r�\'羞9��v��}�ȯع3ύ�T)k���X��~�V�~�f�v�{��O?������\'���W?������o���/?��>�{<��^�s�\r\Z4hРA�\r\Z4h�)�	5׿�h�?~�#񴧿?����������3�zN[��\r\Z4hРA�\r\Z4hPzF�����tv�r?A�%o�o��_>��w)�?��=}�%�ߥ�3hРA�\r\Z4hРA��A��|}/X�^�v�{ȉ��_�}�ҭ4��r����l4hРA�\r\Z4hРA�q8}�\\܂�,�������7��o\0Do�[Ok×��A�\r\Z4hРA�\r\Z����p�n��`�l�2�P:N(��o��_��\'S�-+�����}/��A�\r\Z4hРA�\r\Z��#^s�o3n��XO��|�{9-|�����$�����i5�G��~�.��A�\r\Z4hРA�\r\Z�����12��/��?��k�+���w?��o���ە+$����k�ۭ_���\r\Z4hРA�\r\Z4�=$*7���$��\'�]Hg�Uϧ=P����=�-W�M��Xs��ߖ�v��4hРA�\r\Z4hРA]�S����k����ආ��~\'��!��o����¸�|��z�U����#3hРA�\r\Z4hРA�.��&�{Gˮc=��ߌ�i�����\\��&��O����B��:�D6�犻�O�Ԝ�y�u�Q\Z4hРA�\r\Z4hРA�D��V�gX[�)�S�9V�?�mm�z�9���`�o9!h-&�K^���:�|b�{�i�4hРA�\r\Z4hРA�N�n/�kg<{w�g�/�c���B��#�j˩���~��P\'+�����K�ޠ��������j}������O�x��a�\r\Z4hРA�f�g��Ѱ�㇥[:h� ���(�k�\r��l�in޽ܖ�;�]^ƭ\r��O�并<�*��\rږל����k���#9�,�z������˿�����W�*{��tB���\Z4hРANf*��uQ�\0\r:��=�vK�-^�C�������䰓�/>�W[�7�%�R%.K�����/>��3Ki\Z���D�7��#��J�y��t���	�f�m77K�mРA�\r\Z����^�0`�~?�=�����\rZ���u�W�Y+�ՄxZ=�PoKۦ���q7��g��4�s��0@�x0M�?�ɫ���c?��\0�\\��z�$�_MH^��&8SM��GL�4u���z?_���\r\Z4hРJ0���&�=M��	qz?��o��4��d\n�K�#4N���E�.�mwZc����9.%�о�=v~9��<��*�W��8�I�� h�v��{:g�ޠK��l�>��;@~�$���o�L��A�\r\Z�,Q��*�T1�.^S\0>p����g���0��;N�vR\r����$���eث\\!�=�k@�m�/�����:+#�{����[��k�c>�v$�/��d�;��.�]�-��5���n�r�%DO�-�ћk-���4hРA�>s�Im^��^�����`��A\'!ѥ�sR��7ޣ�o���[��\'�l2���t��/u�rHd�Գ��P����TF�!����k�{��#��ʂkM@Ǐuy2��Eܡ�<���|��)�]�]�����3c��X6hРA�����|��\'M�o��\Z�|ưϠS�����:�p����\rʦ_��{���lԜ�NVjG}s���w.�����/������j�q�nd��m��>�@}i)���m`a7���\\m��8G��n��ٔ�o����ӥGơ׾,=D�\r\Z4h�{K���/.����{�~K|�M�J���\r<�y��&/�;�-��Bl�t�˯\n���ш�wӴ@h�}a���r�o�r��5����ow�5z��|�;}iY�)�|/��E�k�B\\O(N���*�x��x��4#č���iu��h��vi��#6hРA��o�νC�s�Y:@�x��ԁ$��\r>3)F������O�I���\\m~s��-�����<����q�-7�X�����\0�����dw<3���P����T��\09�0N(�fI��F����挠c�x2{�a���rd��4hРA���x\0��;Ӥ��bYq&��KwwХҕ,�4SC��~��(�n���6[OMe�X�]6\'�wy@���=����]{l��]����~��)���k�׊�bR����{�v{����1k��i�Y*��j+\0v�!���Mp�k<M�����t}JrO��>�����A���#9hРA�]<�j���2��+\n�!ƈ7�a��A{�kq��K�{������6�c+I13��_�������>��st37ȕs�y��{e�r��Zv&�Vg�Zq^n�Փא����>b�E���\0K��t������;�E����gf۾a3��t	�n��Pc�=~t�᪥GuРA�\r�`���E?T&GYo	b�|8�A��D�6��\nn���,�	X§v�q��bE��Nh���O9�G�� �;�ߧ�Mmy]�mv/�۬�rlIp�J�[a�V��d�R��\nZN2�%�H[�F�J�dO�>�/��f�xl�z!�ݙ�Ɩ��|�]^��~Z�A�\r\Z4hnJ��u2?�$ �� �u��Z�ǃΗ`տ����΍��~��PJ����E9Un�0��\\3)l���:0M�\'��mc�x��%��~�-�)f�W�����4^�|S�/>���\"� F�r[�Z~�6�;,��\'�k5{�ַٴzO;1�T����%߽�zJXz�\r\Z4hР#�c}]	6���d�s��Yrte�91�b�ȍZ�H�/acL\Z�i�v� �i6�mo^��{3@n��F��ox�������ݿ���z/�⌷J�����6�� ݜ3�0����.L��-��9bgy�\'����IN�1-[nݮ��xm#\0�Y�6\\��A�\r\Zty����ʻoW\\�Æ�\rˠB&z��|v;ƽ\Z\"���Kw�e{wG(�X[�jXU�`��5\r�T��zu��+�ϊ��v��\\?h�����z�\"X�sd7��fh�d�0\\��(/��<�M]Ҕ�U�b��\r��\"w=�j*��N^�.��m�fL?e�Wh�x۴�\n�y�|wHp��;��.=��\r\Z4h�eP� �u}����4�0$X�ǃ��|z��G��W ��s�����C�>Xt�^%fe�-�LOl�D�զ1�(W,o����\'�/��G�n[rZ��ű\"��r!<:$ʩ}i9z��B�t2h�F���q��	�+\"Nb+;��;�%���dB�&-z�د���O��aРA�\r:w*n��QX�S�u�~���Ĥ\Z���䥻;ha�Sw*�[�7�Y|hzC�/���(ɦ��}�������pr+H=�\\,+���o���L��#��Z��:���f�%W�{�z��a$��1�7g�T��2���2�W~��A�Z޳�h\'����\rw�צ��}�uZ^r�����A�\r\Z4����\"�\0�1ߣ*�A��*u���\rA5�L@���=�>�wH9��\'`�!m��T��j�滳�b��O��^�5��\n�z���~I��+�n��Y����z!W��f�m��{b9gi��x�!��3��E�`m̵�&��_f!���b���Ẓ?���=`Oj�wݏE��a����Ң1hРA��/9O*�G8\'})T�y7�t_-L���;����ϼ�6F��Kw�(��9nn�\'w����i��YB��u5w�I�=����p�M�ÿ��go�����귩�[n���<y�ofn|nۛ�4�Ғu�Tk]�ϢmF,������\r��ז�f���<����y��w{ɺ���Yi��\r�ӷ�s^��4hРA���ays�l��/�#�Cp�Kwt����6���nK�!���=������lۍ�1�}��XVLO��Asٱ1����i��fk���ˣ�Փ7v?ˋ�#d6��j;���m�	��f�|�,-_�GMː\0��8o�[A��.V\Z�fM��i�,��YG����o�4r�G��2]\0>=��\'Og.-)�\r\Z4hЙxv��7�\Z�p�0�(�DIrH�%-L���ws���ra��[�o�p�,߾�W��N�&�n��;�ܭo	�(�)E�t���<��֢l�ؚ��n;�}Ȳn�n#Z���I̚9^�u�J\"7a�y7�`ry\n���d�DNi�F���)��Ɂbi]�� �[^7��\r\Z4hP���)8w���1���w�Dn��˹4�$��x�R�>\0�T��Ig�(���#����=g �и&���ה�朻.<Or�1\'���S���\rK�o_�\\v�n/.G0�#��*ؓ��U�K��e�SpL�C�M`L�������s�vj�+��je�<�|7m촀�;�����4v(V���/d�O,\'�\r\Z4h�EP˭�h!�\\g����\ZtF��h0�u�6)BQ��ʡ�R���k*;������̛�5�֌g	�;�,��[s����ۛ��g�_��6���`5��+�fVU���lV:���}�[+տ-K�\'�?6R�J�]Q·�Z����z��A]�}�%��y,Ϯ��/�Ili��Q��Y��:@eh�4hРA�y�����8t\0��Р�*$�@��HP��o�C������ݏҬ�,�֬qL|���f�p�Ь�0�3Ԁ��ɴӚ�\r7=��������#z�W]gX�S���\'����{��6��su5��K~�\0�\r\"��랒:jo=(?�YX\'v&H�{�.���6�Ɛg,1ݯ|�2$�p���FBРA�\r\Z�̪�m��$֭-݉A�H\\��^2�S�	&�g������K�M�8X�L[���}�G0�Oj�Mc\Z`_��>�)r��_�zr�.k�x?��2�R.�{��Mp^�ݷ�[O����#�94A||�S���h���:%��{y��LzG�xNF�����NS�D����J<^�\"A��4�\r\Z4hР�%�j�T,Ԟ�m]�n�����6�@�h/��O��KwwW2�nP&��;W����m,L�\r��	�hos���B�߸o�����o}B��iT%��4����J�]�{�G,-��EvvWJ�o⁹ xn\'�-`����ÙK��LK��ɳ襛n:� yV��]R���4hРA�Ν��0f�f�\02�كk�o��U��(�5E�Kww\'��?A�&�_��&W�[V����k3�Zoڙ<}����׃?���\r�5�ٜv�I�w��7���66/wkNdy��Q^���Zje�d`*/q^tm��.�ǥ3	6y��\\$�q�8L���	��X�1�\r\Z4hРA�	J`��v��djl��W���N�w�8t���B���BZ�w�?�.�uN��9Mo���9���\'��Vk���a��K�ZS��\r_�]���j7�Q^H��e��}7\nc�I[��Kseo*-�=߶�6����Y���{Q�xРA�\r\Z4��I �Ÿ��\'�C΋����4y�HO��w�㼱��t��������s�Ԑ���wt�{��.��wS��ՓG����m\06M1k\n{�}믷�Y	������t��gt�{�|[��hA���v�5BԥYr	�0�O��vk��=��\n��׋)4hРA�\r�h�\"���^���\r�4��6(�����ʰ�\n.V �Ƚ����p���E�u�3�P�k��Ӓp���z���؀��v0�}��\Z`������[+�h���	�����򤠥�ș�bo�y\n�\Z�5*����K3�y��Ij������JP}�wif\Z4hРA�����\\۬��:Ul�����:����UY�I�a�x>m\\v\'G\"B�+�_a{�>�r\\��tM��o�?�������N*����s���e�%ߖ�~�C��̈́{��z��K��2�C���V�u��Y}�*c�T�����LvZ�4C�snh������4hРA�\r:+����Ç�G�-�(,�@��twS�9k�,��C|���+ưx͛����o|lz�䵱��SQ��>Փ��7b7SՈym�\Z������_\\�ޞ\nЛˮ汵H_ZBOM%�mqw�f#���&�ou��ܗt�B��D�Ev1W�\Z�-�	���yi6\Z4hРA��τ�)�1qW��/��9{�twS�j�-[�-P�9������%�z�m��\"k���7B���o��\0�fb���}�9��la�=�,&�\'RŻ&ē���_�ғRE�L[Ž�	b�3�r	N��r��4f\'@ֺ�{���5��K�����_�\r�\r\Z4hРA�-� ��/n��T�s\nB?�dJ��\\+~ϐ�\\ۗy��i�m˴�%�x9ֺ�7yѝ!��]��e��S=y؋,��\no9Ǎ��t��?��ܐO�g\0��l��4�ɐJ�8V,�_-e\'Sy4��4ND�k-��\r�{5y����[���s5��@�7�Q�_���M�\\d>�}	��J\r���\"6��t2�X^��2�̉a��;\\�Xr�G��c-t���v(x��Jk�i[3�Ӗ�5��5W��]�u+��u��U�ٝ_��ƿ��ɓ���~�����m�i���p]p\nԛ�lӌv���/?+�\r:����Y�ߣMV��j���pR\n���YM�ib5�f��v)��|åy�<�$R�#�MOG5`Mr�|��E:�܀�(\\�\ZSK� k��i��|���gK&�:�g{ݱ3��K�u�E�n���+�\\��\\���a��/^T(��D[���.SZ\".�qiw�U��y8���΄��*�IX{(}ύ�ʯ�����*�\nW�5uڸں��-���7LKq�ԒƝ�,O�--���G�+\Z�6p�X+��b��wi,C��M�6��tV��>\ZG������w��Y��ڠ�\n��T�F�K�sGjI�\Z\r0/�#\n��hKd��l8�ȍ4�#U\"�GYKE��]����-}H�aN�C5YQ�x�U\n�(1��?���c�����ndҝa�gV��v!\nMO��f3DZ�ƛ��6#�K��@�Y�k@�O��$Lnpc����1sgX���R:ۊ-)�~ x�Y�|\0��fk�^�����	h<sִ��K�\n�	Pǚv����mm��6F�S{�t[g�݊ņ���Iu�����%��~��+\r�����la;W��Y�W�ГH���TAgs.�����uo+��Z�l�nAP�y-\r���Y�(p=�>E#&��\'ZA��Ʀ9ju�+�Y�V�]7L���8�M�%�@j(ӿ#lx��I\\�O{�\n[\\��s��7ʺ�3`���nJ$=�,a[�h��ݠ�Y����^�5]҅?��V6v���권V��5@�� ��d����ye����o��wx(@ؼ�+.�n��F]�\"�f�����K����\'SO�A�Pwm�2!h�5�l��yn�w��&�}��������G>��	��ۢ��7��nr�P��<�d�g�t�l�8��т\Z�������@��� ��ķ�э��4p2�oE7��D�i���kɤ��R ��Z�a�3�Ў��$a��m��&�Y����IC�`ڹ�	����I7��LY`�M;�>rО�)�]�8��V���/���]��L(ǢT��!MM��=3ۓ�I;F����b��R�����\Z�8��Ύ���\Z����U�J�$ɨ�1�Z䫿j�1���3�M�t���=r���x[��N �y��}�o��ނ�/��g/���(\'؋[�������o�-t-m�Ӭ����V,Jǥ�j\n���-U��5�ϖyi��i];��O�����M�t�t��ñ��i����U�i� �ͨ�%���ސB���ͥB�I�Uc\06(>�p�FS\r!���5Ӊ��e{����^�R�KS���\"8���j��-�|�\\�8���\0<A7���T(�T�h~�1��N�a:[������q����N�Eԗ��3�<&Zک���My�&]�M��\"�fY��}��S�!�*Ɔ.rXEBE�U����	��N�@���Б�b��\Zg�7�!��e���|Iܓ���Tg�u�4l��V�O\'H:X��(���o�Tu��[��N�6��[~J�J�=ϵ=O�OS���.�zn��k+�JRL�\0�\r���>]��t�ҽ?#����rc�ː���\'sذX\\�I4��1-pE��U���g��Z���67|��6�-��}%����	x�t�B��=܊����)X���N�Ӗf��O7���1�ø#�kS�����\'J�t@fFL��K/EF>m�#8Z�D>�A�C����C`V3YO���<���Ѫs�A�k�V��K�=S�{���rQ�a^�2�,�Ψ!\"cbmN�[\'���<�V��!�Qa��.D;�^N��0�v�b���ʋ�:�KZ���5��&�)�\'U���_��`���k�a�k��M�wV���z���M7ll�n+�u�v�)�V���q#6?Fϐ24����,*\0����6�z�\n7�0:��*���#�����\Z�o��2]���\r7C��q��Ʒ\\;g#7Ҡt��ڼ0�`�c〈L��%���fl�x����}�4}���~d�\r��\0�-7�1��Iy4{�E���b��x�f�ޝ-�\"s�@5ܫa�	x\0r|\n��~�x�hS���9K���<c����Jl���^2��X7�/�u��Hn��T�=��3G�;�R*d#����� v����H��]��(g�K����f����\Z��-�LVa>�KbO�4�����Y��\r:N�x��;4F�a�y6]�7<b�ɡT���[Y]��{�5���!ϻ�K�2�u�x��eS����R@@՟�5�_�ཿʶ���|χ|�jԦ��^�&��D�Ib!��$!�n%p��펯�Tw��˩9^�����&y{޳�K��Vn�\"\n4�ŧ���4�Z���Gܧ`�qȩ��jʤ���=5~�\\��zd�g�����WpB5�>�XI�A�7�������),��͙7l��qo\'#����9���Ҽ��ٍ��\n����I!e��K�S�)\"f��ۺgfHuu[������j���A�����Cv���4�t}7#��Lǋ�R��\\��o�����s�ɫ��|�Ӵ�[_�\rh}[������[o\"�~��̫}�\"�,������������˿<Tלx����nq��{ji�&f\0!�A1�������~��Lt}E�-�0_��+��Sv�w��fl\"���.�K)����?q,�.&n�	�\0�e����Ȩ��h�u`{����_d/��c��Im��gi���!(���Y(C`�ik�t��s�f�X:h���dhV��7l@B׈3�����w�k�#:�(���	�����lVqA°gFu�! 5I�rOPy��6�h�G��S[�r\rC�	M^��l�e]���P��O�PB�+�;$3$���z(�o�M\"g�Qr��<I4q�_�4���vD�Q���GF�\\\\����J�|Z��m���^������^;渵t<��N#Ƣ.�u݌�,K��~�)��\'�x�Q��8��� �;�y��\Zw5��賶&D��,��3%g1�A�M����(�Ew3��Y*�7��S)��V��0�9�O�\0 0�3k�[�� ��l��s(��WR]�=3��N㙖x&2�<+K�\nXI����e~!oxm�I�H2E�����zg������fk;M]3�U�����`m%hy�\Z��7�b�g��ѻ�=Cc�\nh�i���*8h��|^/8R�nD�d>KuM�aV��2o_�SL��i���g�l���9��wP?(r��1bCt�uwG����cO]���8125�(�)��,�[���0����� Y�����\\��ӺlNRcK>��Yk��my�a���|�{;����ػ�w�j���p)�����{��A�D�[63	���l���M_��gMl���9�`�����P��>�tAEB�1�2�\"n�T�;Իp+_�,�Ҋ��O��h��|�����\Z��G0�\Zi����z�K#\rS����?s�	�Q�|\\�>�����UP7G��.�����~&Bn񟔅�, -��E+�H�(�=�ً��XGQ1�\r+�Z�������u�al��\r15�\\\r�\Z��<�,z�p��Yz���<嫂�ɩ��E\rʅ-4�:0��z#\"ш?K�;F��Q�4h/·6�&ٖV�lH�V�����=N1�6jz�0�\0�t�w��Rm���W�U�����#\\\Z��9��9�����	H����\r<��}{�	ٲn�Pi�ۆ���W��g��XHs��C����.�^Q\0������Ǩ����r�7�6����q\"Ϗ�NkL�:AW�\"�m.O� !+El��FS��1���� RO-�Q?nc0;@�y{m���ia�w3�w�D������r/�c���<������L��J��0\\ԙۣ�sDa4^�\Z�\najbL��=}N9|��/h�\\Os��ɿ��LS�7-	IKUUf�;ll��9\'��ZaQ�������d��ί@uڗXf\'rf�-���\r��]�Z!u\'��\'����H�L����D����@8�����ę��6��V��LPt%� �h�alA�X��oE�ڮ%��zm}-״5�&�9��Sw犵#%p�Y+�6[3�Yp�6��.g�}�n��m�lKs�(�r#̳\'�p-�������P���H�a��(��XC���v�\0����G�1��)��0)\\�ǩ�U���JMrMs��\r���x�ӿ]�k�Pj<���2v�Xb����q��c�R����1�sj�cz1;�3Ym(��=.�\Zd��S[�;NI;58���\"�x�;h�1~���G��ݖ��֏F�\n��\r�\rו��\Z&ܻ*�&#\0B}D��5�Ģ�K��c��7�I.c���͚�mh��0@oh1Q���*�@��<ն�H��M<Ǘ?�~%Çw�c,P;x0�29!NR����62Q�W�Y\'<זOg�+�vt���l	n�- Z_��I�����u��~h��݅`�H9p�k�%y�f���� ��A:l�V �n�&�K��(bUͦ�(���^ɴf�f���]�\0�웉��`�k �$�(�kߦ��\\�f<�J�u����A��\0�w�n�����f��M��ϖ`{��l:>���8�����%���;}r�f\0�s/䃉ȧ5�87L32z��ߌW\rËR��ؘd�v�%kR9�_e�}\\g�h��7|[��0c��f%���y����A������\0����nl&\0��;��h�����D�tt�$�h��מ��<\'�_�M�;���Yc�����Fb]�}AӷS%6q���0ٸڧ��S�Y5�D \Z5�HY��4�SK����m^�P�m���V�7���uW�\0�\rHw��Nѷ&�-��4������J����[��[;�#N�W���\0�xgt��A7����Z�ä����������ן!P�NO�ށM͢��$�J+��I=�ek��y��\\�\Z�K��	��\Z�<T�/yhq��m琴�A�u�M��u͎/���Ic�� ��:�VF��Ҍ�G����v�m��I\'Bc)���:j\0���>+��K�.�� �B;��7��,v0w�5Ib\\\r�I�?Lո�I��l��\'!N��q*\0<RyD1�`EəLM�z�\Zk��_��a z58m���d�6x�7�~P�0F��MK�W��9�v�����Ġ,;�6C�\'�t]�C�U�õs\Zе<pj����^����R9[�}���ߧ\n�L����Z����iZ�~�p���I��m�n�9�\"R�v����r|V�%�/���)*rk�,s�$�n�l�����V:����R���=m��ד�n���Mh(�\n�$�E���\0��m��J�7tZ|j,�ñ��r��!fo�\rk�B3��aR�Z�pQE� �rb�+m�Pw�����H�q�!@�u5��CߔN}�m���U�d��QCV1���tǮ�&s���O���:˙Y�M��20��V<�:��e$�Vx��(∻)�Y�(�S,vC��B�:�M��`����T�Xо�Ύ;\r\"\n�,�,�3MaO�!���*˿0;�����V�7&H�<��!�v<�p�|\\�\'V�꧎˪��-�KD9�+�ӷt��Ӓ�g#|~�\0��-�a6���V���9�))/0ܮ���I�.��]���Q���o��K���y�`p̼�S�}`{u������\\\n����:�<N�HO]�t���T�!)���B�si�9d��\"���-�j���U]?`����H��#s��⣴�V=[BB�F-N�<�5)\0�K���6W�0�{\Z=r����<�p�5��*��!��~��l4ˈx%����c�L�k+��J�����1kTed�p���CE7�-����#�b�Ot$������&p�K�Qswg:\"ЀD�%��p�\\�D�Ia�L2�v�/ZZL\0*��[�Q`ލ�|Y*������P����#�֨�%�u,p�c�<A�Xт_��7=� ���x2x\r���Tz��I���q���5�T�m�z\')rs�=#�r0�����K\\эh7������e�4Z�\rh���|a8�n��㗥y�]UTr\rv<&[B�?��.���V�Kw�bHj��~����W��0�����l98!j�������>:E)5؍���6�ȸ���q�2]�C��3�����z��~����\r3Є�Ep(.<g;���-���Æ)��Y��pOat�<�����i���+{�t��Te�T�DsS�\r`�c�=�W�>�̾�WǱ9:�rr��x�<l�$7N���u��W���v��\\�<�d�f� ��J�i�������C����k#�R��N\r�0�󡌹����sz�w��2Z0Jd����%1\\q��xoc�[���]Ɇ#ͤ7\'%���~�GK�9}�o�r�S�&it>!z���\0?f�?y_��d�k2�]n�����n��v�Ĭ���4w�_��b/��L�����$bgG&��N�V(���x����&\\��\0�<�\rب�ة�R��<�I���\n��΁��Eh�ا\\?�1My��.`.�#\rW��6���mE&\Z��ѣm�\r<�n��tm���������.�\0�{�*xn��I�P��+Sp��v�R:���O��Paj�)�\r<�Ɇ��)�U���&\Z���;\'���9��,���Le	�f@�ѕ��)���[��`ӆ�}u�7	�U}V��8�� u��<u�C�ڋ|�H��눓筈��+у��;���� \"ho��)k�XE�^K�v΀my�u�#���y�vA�x����f�k2�b��b���i�u09����m����S�x�:^�4�&��5��C/\n�����Z��FƑ���Fػ�qH\\�&zwc\Z	�3��t�= )�TG�2F.	��=q�K��0OcL��>)O�Ⱦ]H���p�^�V}w��1�/�u�G�T��\r&+���G\r�v��i<�q���I]鋍\'3��82�c*�994fG+Ab��W����*Y)�Bhϱm�^�������ZΨpn4\rb#���4}b�*�JD�Ɨ\"���M\0���t/���+�y(�+��x��+���^j	Ckv�dB>�ܜ�@\'��9=�9�Ħ��?>qKk�q֔eS7Œ���1�R�mx�F�^��ei��S�]P�O��a�V�VfZvk�aIx�׍���g��텒�\\ݝ,�N7a�r��\Z���h_�4�&RL�.��y$]N]�@��g�}\rJ1*��g�;���H߶�e0D�.��G���_xC��:�j�\\$f�c�Hc��7�n�}ш%������#s�}�Ѥ^l�ͿQ�e��Ƅ�j���Z�a���llSZS�U��4ŝF\n���p�Cc`�A]\rKe[��&2\"����Y�Z��꩝���t�:I�7���kn��S��V�;�\Z�7��������G��\r&n�v�\nC����.�]7$d7�m��FU���r�j�w#<�7�a ߐu�9���\"��qР�R�͗�2d����\r뵵�<j����<�Y��F���څ䝄�bȕpl�������D�[�\n�m����\"X����zE4��2�ʆ�9�p����0�Q�a����50x�3-�g#�߭1����<ja;��g6	��I���}�����eX��$\n�c_j�w`~#�	79��b�NA��O�B��Db�%[�<�,\"�2��2��7�d�����.$��G�5k��z\ZO��<�����zjl�f\'�(ؑe���7=�Ld,x�Nˁ�MGX���[��Zx�XQlò�W:K�K\Z,O��݇\n��̆��5���5�Y[��#N*��/ml�oȠ;��FeQf�z\'N**���c�w!��P������ISD��t�N�yx~�>Tr�����u>�(\Z\\,��Jȳ_3��ڶy�>K3o?�Y$���\rZZ�l=�Xǽ\'�����H������y�F�[�/Ѳ�P|��3�zD!.ri���.�@SCڳ��]��aD6����1�񹹖�p#�Ϧڙ��g�ꑎ��]�`����A\\��������.���i�d͕��ߐF�����~��\\��-$��=�M���\Z>3LѮfc�4_�Y4N��9��FiK_\n�4L�G�G2�:�ݪJ��Y:i��7S��yM�\r���&���Y�\0���\\�2�����_�Z�姝�6r��zl6����� �6ۥ\Z�\re��	���A�Bc�s�n��@;��+�����x��d�O̫s#�z/-5GĝU��^�\r��e�Y��/ͼ])\r�5\"e��0�-�Mj(�#Kw��h;d�FX��n~H�#��&�mG�a��>8�=_���z��M�j=l����C�m_�f�]�e���<W�Xg�%6#Б�[�h��t\\d	��d:�	��U6����� �|�j�j\"�K��e v�4�������f���\"wϣڟ=�1��S[�h�cT�($�T�D����X6���|w��Yt�H��&KPD��Z�N��>\rQ�e�ɼYB�2�,�]?��:�����z��\Z�����݌(�Xk����б�[��הY��yA}���p9SV�]Г���?�p���K���iv�6��w�*��{� =pY^ѷͤ��4�v%��R���|Y��F>1K���(��JX͋w��\Z��%��\Z~���w/��z>b1A��\Z¤%�sl�,�a�j0�rbD�:���P#a	�֐˦���ɇ&�zU�\02iS\'[�2�{d����X���Mco��d�B���N�iS;�\Z���ʻ$*�9���I~&ckܨ�/�&�>�w���n��dBo[��]\0����@�g9K�ED;�<��^<�F ?��MD��Zba`텶<4��,N#��8^����=we��7Wkq�4p���sb�`�ْ���.���/�����g\Z��*<p;����6C�����)��i�vo<s����Z0�4�v%c�1�gP�e<�:� {�~_���a������:���[P���D�޳�{@(z�W\'�ns��(��Q=\rͶ;SU^�E��\r���2@a�ޗ�{�W蠝������������6J�&��!WyS�p�\\u�+�i\r1���~�3�s΁\r�{:]Vk�(�s����O�I��8�����V:uh��K���P��o����N�[W�E��^�w/E�T�A�q|3׆2ٯ�2�Ҏ�(��-#�������02��v�Bt�!�����q�4\0�a\ZV���EAX�-E�e���;�%^�{Z����)y};ݡ`|��Z�\n��4�w�\"�y#�m�{bЖ��ER�u�iyʜ�i��(oB�Nt^����VH�(E����a� ֆ��n�A�\\�4�A���7j��Gex#l���<X-�TZ7�\nxLP��N�{XM�݈�5�̔��y�^<?�	V�4l6q2[08h�\"R�wSQ1U5V�$�M�L\r�2r��s�|�!ؗ@��Ù����.߈��|N��q�*�G���\0�m/�I ]�Li���V���S�:�.m��Kʾ5��b�8^&m�w�G\Z��e�`^T�Ȥt���-L-ۗi���W0~�ʤ��AE�\'��ѶuӜe�W�����nc�Z�9O��	�_�R��E���&���H!��-g~_\"�9mU��L)�`k�A�����ɩQ�f� \r$�J��q~w���0�\01���JDd�#)В�+_�c���V��H�s����h��|D#��B��ɍ���1�Bח�Ij��ܝDh��L�*��~��\0�L��P�����/��ᒙ80��b�W]3af��W[g��\r�/0��;9?޹�]��V���\"Gk�L3rR��:u��1��2����N��xx���\0���V�������U�%Ѭۗ=�}�Q=��c|��s�N-�s2�\\��Vm��w�����ۢ��\n��ܾ�li�m\'\0ؼ${O\r���`̺����}���B�)�o���&\Z�svo!O2��]����Jx����8�;�O�����,����C�C\Z�����q�Վ>Hx���>�D\\TlTCШw�>p���c-S�n�������oGnO~��oT+�B�!)<�y�ZR����	Q%�����|&F���U�$t߭�p�1g��5?Rj��.���1�UC�||�f�s����߅R�P�3�b7��k�������Oג,`��jb���ؑ+�9[���[N{�4&�n�=p�`��K�/��������������y�y�������R-d��ͺ4;�{k���B�Rp��\'<e)^�N[�Yj֒�k>�|�`K��\"�}A,e\\|y�Ů\Z���5Xp+rv\"-�­\r&)�;���}�AP�32s�j zc�oJn��C��d�ܙ�`L�kh�X��`�\'�E��+\'��FL��9�����+�d����.��~�|j����8��\"��^��L�7A�\"5_\'<�Q+}f�N��WӔ7��ʠ�\Z���(/��Q�6P�@�V��M�>���2PU��vK�,�S�9\'��nr�2�Կ���$>tkyh\'.t�4�\n!ߺ�f:��z׫�m��m\Z^ϭH��]�[n�\\H��W�,��t�����z�zZN{���lPm��.=�ek5�y��c�>i6�~���/���,ă�	�Y���}��~�K���XX�h4�������$q�d:!`_��9� ���\r���t���G&��<	�\'=Ef\ZT������Pk|Ȫ��a�z�Q�+�-5�:vS����%�m�	Ȱ�,���ۊvzl�.\r�ik��2^!�އLِF��P�O\n*�C�`�M ��ʡ3����\Z^��*I�6}���>�US\0�KX\'��J~��n�H`n2���6/��N��X4���U{��q�,�F�O�*:,\'���ݦ���&i���˸�=ښ��ķ����ݔ?q��-R~ׯ�r��mݐ�~ߏ_^OW=]r���T�۫�zN�\'G0�ok����ӗK(,E&�H&}�t��\r���a	�`�~_$e����IJ(@HO�PpU��Pq���a� b��ֻ�x~\0%�|�Z����k>琶m�[υ�)�+f�oh��7�+�c�(�0�rp�8hTe�d����������Bj�8]�0����;�.kn�\\wsk	M΋��PO�q����a�\n���Z<�:t9�����74�L��Hƽo�F�F��\\�Œ,�p��fw$c�,���g��\n2a����\0��J�*Wb�z5�Aϵ���+�6����p�ɕ�ɘ�&��>����0g�����.��TxMy㯧)8\'<��Y�÷�#$Ϸ@�,�T�.��_�k��J�)���L�� ��U���Y�K!�9�Qx#	5��s����U�_�g�&r�1�02L^��OJA�M�\0�#{��\nL[������v���_�NFd����/�W�Ʈ��i^#\Z�����O�I��Ptgr_r�B���[�}�;ܣIY�,��E㓤�@�Z}9\rûP\Z�,���%��\n���|�5;G��K�1}ܰ����\rB�J\ZC�Fi��t�����I�\Z��B�����ܹ�:-�%�L���螢LުW����~�ИWj�+��\"�=>�C/��ؼ��\'�	���uHaS%y̓�L�k��s�\\���������:�HsS�9��B�u�MyKE���8��_��m���רU3x������ݶφ�b��2ga�~�4n�Dƿ�\05�I��ӹ��~�H�v�9���f�}�:W��o�ԑ��|$>e�Rm��ON�됶�yK�-�g�ر=o+`���\' �cS�mM�	�5L��o[�J!.%�})�~)�!����v�4��E�> �./���,#�w��U|�v�IQ]&`MD�y���V�)B���ր#�}�9���	Zk�Z�њ\Zh���9�Z01���#�ϋ/N���8�$��9���j���<��kkꜗ��j�y����+�~��_Ğ��j�p��Å��HS�v�5�E�v���Rn{��ɹI�����b�\n�m�)ˢt�����ǫ�����n����Q�$��H9�P��α�s!O�>����V��� �$��#{��W��Z����@2�q�Y�iO�9;ǅ��S^�Q\n�8�m%*���8��y����읃H�Ĺ8d�K&sL~Zm��F�	(��]�@���ɸ݋8\0��mE������NG�!;�K�)�^���N�q@7���W�MZ��k�rjmβ�t3��j������s(۳�k�l֋8p��a�מj�t������p�ymy1��K���nkJ=��Y�{���h��u���������|��ps�⾕�u;?�����0Iv�\r�����i�;��Y*��1_�ߗG���Z�M`~u���@X?��� �mDE&�1W}�拍���31�\"D3u0���<4df����1Fxg�¤���M�;�3�2�R�G��rKAu\r�dȜ9=�mq�)��!(QY�2�a4Wrdw��q��g��QR�8=+m���h+!;T�\\���9\Z��JQ��\Z��ށ~�������ڟQ�]�1te�b�8��,~�W�g!-��m#���=��ݩ�m�����8����rfʸ�9�e����H=�\n�U����/��/���PyN�\\�3�c�[��C*���{>>�E4]ٟ�jp>(W���ݦ#�����>��˴�-˫�9���w�ݞ_�m�� �$���@�t�9�T����s&�P4����i�;�o�_#Ŕ@���ҌU\r0(	2��$�y��<4�}��pb_�\Z[bbא�1h���0^G4l�h�g�b��׆�ߘitVm���ĮG8�1\"j:��r�M<L�M[�^��	)ۼ BW�	!n�|JVw$P1�M4.jp���4�v@^��gT�N��( ��YU����<�]���9�z|���	i(ju)`s�.��{����sW`����8�D�\'�{�о�\\Kē*tD�u3s�������Pv#�]���;��u�k�]��Y�)����Z�]*��˵(Ɣ4��X���\nnZnq�t�9�ߋ����.�ޓl�z��,�3B1ްs�fg�)q��1�<�~��Ġ�$�\0x�W�e$��@�gqc<��)BH4t�V�c���+�H猇��2��)N@<�E>7����Cc���%����9x;Aˣ��uЬ����|��-1l�\'5a�b�MO��D*��J9PuJ%!�)Ђ���!M�<��7�\\\0G2��Lt�d�̐�_��_Z9����6w��6j��5�Q\Z�n8fvk�z�M2vn���|x0���k�|2\ZѴ�p��<y�@��������R\n��X[��P^V�{8h�[�>�a��B~9[�a�d�J�����t��}��|x�;�G��������\Zk�t�/��9���-�f:��Չ�\Z�#�M�8x����ūf����1����|�\n-��ppg��-\n3C��\'6aPB�v������YAs�6��;0�i��F^��m�9M�~�\"]�����:�������a���l����5���z-���aR2��D�D�3��w�\\�ݻy�Rܟ����~\\��T�����4g%V�f1����,N���(wOjf����m���M��3K2��B��W�3j-ٮ������3\r�2��\rb�4}���Jm_>��&<���F@]����?�������R۾�Z��\rv�w��K����ѷmϙ#nkm����0*�\\�P�Sxgl�ϖ���hM��\r��w�W ��G܍BM	�|�=8�C\"���.���A�	Ӗ;.��$����~j������hrX�]�E��78��\'è��Yc��LK��]�ͱNO9\\x�=M3\0旱s�@fD��	���T�B]Dl^D�\'^�6�;py�j�k��@M�T�yb>�%���V>8!ǡa�F���<@.�4-6��	�F�Bd�:��R���)3\\��\r���&�\r����}�̞��v��1^\"`6�\n0��g�<�nf�Sl~o��V�3�k�?����A�XC�A�8g��A���d�6\"nY)��aY����]n���W��lx�˽��_ͽ��q��{�;�QX0Y͈W��I]ÅOڟ�\\�Q#=ͥb��-�\Z�{�ک~����ݻ��́�;Ŗ+(���cV�����I�����46p�#U?�f+ $	�5�FN�i҆��v�%�5��e!@h��~3c<��̯�Yi�L�$�KR\"ou�&-�i#�|�M ��L&�y=5�f�Y�ϙZjH��3�	l����q��M��ؙ̄J!V���4�Lf���o\r3�|Թ��W^�����톳����H�j���FŻ�	~&���S��U�\Zd�3��+�;�9���0O�{�?=b���k�^j&�Le�q�u�u\\vE+����	���ۺou�p�\'m�4t��x��z�[wDy�t���kR�k���,���s����:t\n�l����4�~�kg@IFH0HX�	ǧ)C\\].���GGD�����pT n����#;5I�\r�\Z�8|Y$c�T�*SDAO�w���JRHo��d�x\Z*���Yt�8�B�CBք	IШ�F�� �^�0���%�� �-X�l]a#�7��B,�}��Z�tD<�������3�8�r�:n�]\rȯ�7�d��n�`7��Uh<�G�Z�\'�=[+!	��W���8���N��jv|4�6�<j�\'�w�1�}N�,����נ㘛�7|����BѸ��,���!���q��!13�/(I]�7���Zٝ�F�����l|�5�iz:\"qZ����	�-~�����]�K�p�`27l��Ps���{H�������0��\\�ѡ1�	��S�h���@��$�31�H�N�`2�6��b\0ɼ����z;5i��#�9�l�>u��oXf\Z����\\΢F{ܯ���B���BEz�C�G�S�d�$�U�t���<�X�A�/�Ә�4J�\"����r9�r�>n\\Lvۋ��\r����D���].�HR���q��-Xj�ҥ�Ձ�(o��*6#l��z9��@����;����<<Ԇ7(A�Ͷ�s6I���n�M���aݢ\\�hg�������n�^�{�^G�+o쪀^����_�(P���aG5��5�0��7�]/����Ow3#Ľ#�NA������]�K�i\nj�|�f��b��B�P���]�b�\rr�l�]��D�����k��#\nL\Z��b\n�`j�x�c\\K_�PC�ICwj��Y&�]��q�-w\"ʉ<�K[DB{��,-A)���]�}a�vs4�(q�~���S�5;��p=��*��5�h�/�t�!f��Ow0D�l���^3A\r�͛���|�N^1�衅�L���߽ږ��Y�!��	�&0�Wyi��I������SR��n,-��j�>sڭ�у�$H܎��Z�:ܽ������17����q�ѾӟegIm��{�aI�.����v��X:.��a�Ok���\n��î洯Z�Wc]w{_qZ�s�pN͆��5�����K@�oL4.���lvR8�}VO��Y����Dg�~8a�i)	(B�H-��K|���\r}���`���\nc\"��iZ�����+���v�V��==���\0.\"~�x�D:�XS���yѝU�V#���l�����F̓3�g�!)��%I���M�@��2�vg�L�>EQ*��,��\rZV�==��t9ۍ��\n�kԙ�Is�h����ʡ*��S_T~����F5t-G,:�Y�~\r��Z�@qN��`�?XAg��g��\"EQ��=I_4@��S�z�j��E�Z@�o�7M�=�sB��`j��6/�ƍ����K��h��F���17��qNv��Z.�+����{�����MMx��m�ڮo�jz�w̒_�v�`u�9⼏:#g�t���O����	Ŀ{�&��,��\Z���M��@����[���3���`���]�V4(>�\\Jԯ�(�sq� ��.�iFh���qK-�LQ*8���fj��W�4/;ȳI��/<\r�bx�����ѣs��t�I���Y<7	%d�X��7��DRd\Zyz>w\'�J�+�>p���4��A����ӳh��LigfV�MjY--\Zߖ��C���j�~D���J�Y���������L,�â��5��rn�R����\r\0����dW�;ݦ�\\}ϓ�����l�/Ƿz�ƏA�%��+�q�s��q-y8�;.��m�\'<+\Zod�=��wv��{uv����p��)��Y��7�gE�U����A�����M\r��)w3I;Ti:\0���9�݅�;5j���zx��M�:LW4s��_c;s57�\rÔE�\"l��A|a#�п�[�Z���^\n�InCV&9)�f����u�P	��|Z��}K%OF<(\"3\\M���Ӛ6l��<�9lՁ�Զ��C���v�&;�	�1��T�L@�׶��%ݧ�۩T�$���i�&�ɷ�S�+ۍ���9�7[��ꖌ��2h�f��n�@���|�9 z5[)�X���5=��ii�Ƿ�Z��<�uGܡ\\���z�7,�����?E$n��Tj�o��w�`q{�7t���F>��.n�O���}�l\n�\r������\n�= G�h�rߤN�y���LȏE�S\"�i8���9���h0���� a�$*ܹ;�{Qh<濼.pH��\"�}J�����\'��J���˛R�0���٭H6�N8>]��j��\0��%�/_���QM�(ˆ��w� �^�R����l��3I�i��O�[5�k��sz>w��5�� 54�7ʖ�{�C��.#HR��{aw��.���lIZ��jL�����<mQs�2���*�p�g���	�ZrQ��ߙ�\0k7��L���������z�i��^���>��r�\\j���/�Ƅ5���_�\r�b��T��bu��<�pN��3�����Lk���vHk�a��>����b�z�*�f���x��@�ۨ����+r�h�hCl@>�W���$�q��6Ud��Kwn��_��g���5enw���C������0�+S�~c��p6\"Y��R�\0�\Z�X�ѾA\rilR)�#����f�l�*/$�ѵ\Z��֠�V����mzh���g�R�m0)�xmI��A_X�I�9��swJ�[q\"�����*����]�{�34j��3�g���2����	W,m�~]]?\r��\r����XUB����#ev�f�_�P�\")օ`[f����>�hӢ���?��?�K��_���+�\Z@��y\nW��^�\r��_�.g�x;,���m�r-.�������/���}Ws����q�3�YЎym�:{:gk]����c�,2وAPW�z���դ/N�u;p��Ca]�wx��\rFi)�_*�+�5ٞ{�m�3��a���7��Hq�;5�F *9y3�M���;!4�l�Wަ�h���Չ19��yЋ(�V���i�ͮ��\Z���=�\"j��9H���+h֘D�R������p؎��H��ୢ<�i*�({�2���Ѕ0�Bu2��rV�j7\"~jB��)�%>�uVWm&�w��A�B�u]	�Ҩ�;Έ>vkv�-:\ZjX3��F����l��#�5x9.�k�?��O勿u<�/��o�dw#u�׌׼�C���}*87-䦤3�h��R��)�s��^A��C\n�C�><}�W�^.N���}^�\r�?�ݰ���x�u�ta�u�m�\'�(lV7�v�dD��z>x`����	w�A���J �h�/�;3�ؑ���܅R��;�#�| ����\r�5\"���dl\\Ѱ<�/4�������A��r6e�O7�[x�Sl���!�[Z�ef��%$`g<g����Q�`��iLH>1ܴ���R*�$����R�\r���\nbY�.�1ه��g����$u�2�޴VEC��u��8���ĵv�\'<���t��\r��̠��Ss.&!sJ���s��/k��TnI�4X����Y�;�ο��?��_�X�ǎ��/|�_�S�-`d�K(>�ӱ��ƴ5���*o��/!o������)��˷��OlR�V���6݄�+�bKf��^=����U����EG�Olڴb����m�ޟ����*3M��	��h\'���6���o?\n�ֳ������/�;R�9���X���;:2��[�BHC����A|�h�FQ��B�����Hx4V\\C�;�ŗ���;�U#����6���:���=�(VԈ1F�d�/��6��J�����A�\\<�J�XU�{B�.\"?gHQ�J����FΡ���V�\'mQd���t�	������#V��9&\r�]!K�o�x�����ͩ�c��l;g�K��K�0yX���W�c�Y3�O�~������	L��/~��?���!�}��O�cg3T7(���\"k:���c\r�kI��p��qSҼ[,dV@�����޶��%\'�c�[�;��֫�3Qk��܊�%�Q�\\4\"F�|��R�+�Ԋ/��8��j\Z��\\Ɲ��-��Y|�=�v���;�\rbA��VK�C\rrHa��q��A��x��!(u�&c���%!������4܃|\"E�[�p���~�S��Vm�8��)8T��|1*1�P\Z5o��\"\Z\n�Y���q�d�jq[��2���#������݀-�fM������m��� 0�~/�?��>��l�u�E#t�:bch�גU�����#�ںQ����wd]�ʭY&1Sa�]U��c&�k�=���)1]ߗ�\\�����\'5������������=��[����S:v�.����_�MpI���o��qk5�<w��!�>5CW��/>����P�{�5R��2��=��f��������NcG8������Z��]�gDy�c�3���x�p��f��(&%]��Zt��9c�1>��\0�\'ox��M�,��h��\rM����Nh\Z����0z���<.��V��͔e��|o����E��s8EG�mqb�i��o��o�0FqHl���l@������݉��8&�x���Q湗�A\ZgF���#��9is~���9<}xZ�xԎ��7x�6��Q]m��.���(E\ZY�m�����b3c&+۾��a���Ͼ���_�7��jbRB���������g����9n�U���/\0�<u>+薚p��	��[��\r{�wa��N\0�}[�n�vks�Iպ����.ݜ�86`K~�ڍ����B�Vsڇ�\"b�j��4��V.��5Rl�o3�ܣ���V$���H�(z�.3�8�8BCpG|�`Iy�3x\Z���,~�?1�l��L~_�M��s\"��ON��7�#�\n�qֶ-��0����d���׬��=4�A�qȮ�\'�s_�ƜF��)�E����GV�W�i�U�\rv�R�kigp�}��-y�Z�3��S���\r���qkh�#kL��������ֿ��}�3�嫻y����/�������/��������ǜL�������׶ƛv�JoW\rn2Ѵ�[05l����\r�!i�i��H/����A�8&�c�Z_�m��A���^ݜ���ܬ��4���f3kMbT����~jT���q|!B8��R���_Bl)S�^�D\ZK��Fo�{�E�e��G��{X���ŎM�`��Axܛ�iX)|ƣs�d��A@�cT����݃�8�a�F�`�ѵ��6�e��`�R\'��&Z��)�ܝx��0\n��.�K�A<�ݯy�(��c�3c��E7~�n5�Z{��D�|�� 8E�GҢpg\r�����6:�\Z���N�vYݵ�D���.@�R�����޻�q����=W3��ӗ��G�j;>��<�/��،�	��Zj�=�6۠��u�{�1u@���ɜ�	>�������Ӫm�_����w��\"r�o 2w6j����s�zO�����xbS��f�獆�Xiw���w�� -�%��뱩���j����dWt�ޕ��HO͠xEn�y�:�����PC�)���gɝ�Cv1#U۟ou���֩��`NU�6�&G�IU����C���)�ܗ���d���t��������l!�_R��OM�	�k��.9n�\r��x�	�,8�8��s���V��XD�gC����k6\rD2Q��\0��$�V9�W���7sE8����7�������2\"�����e�nx�X�]�y\'4~q�Km���n5�#��]�?�������\n��A��<?�ca9�f���%˽��l�P9#��gu��d�v��Y5>�9)2�/����QL��`O�d��ܪKU�y4uAǽ=�\0��;�H�\r�΀�kEsАI�`\r��hU��v�T*l�(�����*dTXъ�a�a����{�+]i.��g�pg3�]��?\r�b쬦87�۶& m��:%�;��2?�k+y�bby���-�s���:Ζ<XB\ZЎ�휏��*x|��r�s^��f沈��_a֚bQ��=���ڜ8ܠ ~��n��]�^{���?�/������&�G�[b��s�����?ѝ��w�)ד���A�T�\0d��*�_T��k��mK���븧=�C�]סw�q�2�\0����x12���;N+�/q��ujV��}@_c_�/ոA�mi,@��!���[t��#u�y\r�s��v�xz��lED����;]�>UߡV���y�����Q\nf�VY�F9}�!��@<mV�ǔ뾚�\Z�l)���匔H��@p����_a5n��}M/*Z��f�D,��Q��Ɍp�]�^��C���F��3�v[eə��N߲�-�F2ݑ�ΙN�$r�!+�C7��5�\n��U��h3_�-�\\	�����;�j6Q�^��U���KU�졙�j8,�_�����G?ml�֫��ڎ�;�+P6�u��\\�e�r��`��)K.�<Y9����[��m�u�r�˴�\rK�M�y�c[�n�MF����h�@S�����D�K��T�b\Z���� -���{��gu�x���k��	���F	9�H�MD���(���h\r�=t�0�{�j��p���PIXffv��7Uu��G��\0�id)�����;R;א���㥦iW�j�HԈ黮���8|͍?\r�;���L�Y�X���(w�ލV�#Pk6�tq\0nZ�y�j`K�� ��jA��\r���_uf�bN�3u�;���U	l[\n��ᣰ0���MDU�e�W_��*�LFU]�����_��������W7!�<-��������{k\"�E���a�ɧ�h���3��Ǚ*��9n\\�}+��F��>)���/������.�<�j����~&3e�#�)�������fÉ�;�����f��p.�W�i�w�BS)��_��m5���<�s�#�k�,�[��ܨ\0��*��f�kcF�2t\n|�w�t��O\nB�F!����R�7�(&΅�d�2W�T��<ܩ=Ɲ9MQ��lMA�l�@�݂3\Zo��8.�0��ig/�s\ruT�b/�|)��z�����MY��J�� ����i�].U�Dג�ļ\0���lz���U����x��w,iZ�;��/��o�A֪� ]7,����bW7׿������~�o\n��MZ��[�̫	�x�~cV:�ψ��C��|i�k����x���bۏA�d�%c���/��k�\Z�����~#�I\"]�5�IW�[�\0r]cȥ�1#��h�[�)w�	�{6pU��$���A/f�3��yj`�D�4+�#ؽ��JQ�T�h�*�4����z�^k�|�q\Z��\Z��R���>3k��Q������ie<sK�(��9��T)��E�:\0���O��.��&��gd$�\'��Nε�0��N�Hi�P��(��36Έ��@E�8�<ʰ�3��I�!N�s�h�L�,���ST��2$kR�q@\0�O�,=�����/}����p���kT~��BE4|~��~������j艸_0�5�尰��`�k�#�.)o���^�-������/>w<�5%�\\�mwE{�in�ط��]��O0�vZ<~|OF���,�P8��9J=`�s��G��\nd\'/��5��h��Z�6�գ+��C�N�\Z�A.I8ӫ�݉;;�����uR�@�=\Z��ʣ\\��6\n%�ȁ����ْQp��\r�J\Z���r-[��-\"��V6c�}�b,sD{q�{�M�s��nh���}Y4q}�U���:cg�A5��x�,.èU�q찾�Γ�#�2�,T�A���*QS����o��n��A�@��	֯��]ͣZ�8�Z�z$��i�j6Qٙ�[����Ϳ��f�[�G+�R�dM��/��s���kU���X�\r���oD٘��K�SU9����w�;��\0�������!�W^��yٜxV�7ߺ\\>���d|)8i�}�)cwF1	�����I\r�iM/�s҇j>tqCйS��ňSqSZ/��̳r-��^i��\\̗�X�g��D�%�2�>�ʅ9�L٥�����|�x��\Z	Cc�(�C0b��.�p��]������3hkYd�˳p��n\"a�Z��\r�Fn�`4�y�?���đ5�������Y�?����9�N�We��lB��;�u���7x���@�8f��F\\���t�I��1,B6�S{�}��l���_`<bc,�h��:�K���&��-���ˏ��N��M�y��l��%+Mo�Ʒic�yHdc�8\'��=_q/t�޴H<�zхE�-`I��˼>N��͚s^�7���Sڱ�D���5\Z���с|Le�6[�ێX�������{׮��~�Hdo����V��0X��t%0/M>%���!�T2M!) �	gj�y>������ȡ��Vk�V�sЈ8�YxL)QfI\\������mSl��5��d�-,��!̖]���!,���:�hYl��l?�H2Y�6�Q�@�y��:,�g�(���q,Lևo��1ڱ�_�vh0	�K�jI3��؍��G��G7�^	CS����n��Er(&}Yc�7ڵ��&����?��/���{�_b/b���f5�>l.�ŞX��a���y\'K�M�y}WWL^ï������Y��s\0�����Jֻ��)f�ٸ��R�#fW$����\"�S\n6S��������3�:o�� ��8����s4�&8�����z\n�e��K�\r��ꚾ�m|G2��Ίg�ph��A|�s;1\\qOQ`��M@8��镭[˺�T2�}��S~����\Z}������!��_�Q{8�v n(��j���f\r��f���&�\\�_fI\Ze0��d�$�ˏ���l����\\�;a�>L&�{��C\"{�����B;iV44�8��j���K�w4ў�U�S\'��Ę�^��^XIӖ8��L��sA�kƲ�*�?���W7?%��B��]���!7m��:qz�7cp�u[�?�s�;ta��}�/�k���ɤ�wۇ�w;��{������������+����6J���W�R�������Q�XmWb|�Y���\Z\"�7���Q�7�:�L�-$�H�YB�H��0gn�%e��Ƅӿ�4ψV\"T*~�қ�����(F;�ȍ��7l\Z�c�����ʥFR���/X8\'�J1\Z�kȗ\r����G��Pd���0d�4?�0��o���M?�L�\n\'���i�8+��x�0�0�4|��ǭ��H�&���_��N���L�[�\"+�)E�۞��C ���\r�uQF6�ի�܋6�Ys��o���`��ۗn���͘��;��U�����j�\0ͤ�_�S��O~��,��¢/��헿�g��?}��7{�Wh�2�x<M��d0�������~�mۺ���T�;�T�\'��!����O�W7��9���(r�����4���;��c��l���W��ʆ�$qoL�=?�7eić�֨+I\"�������݆+uP��+{$\Z�3ۊ,�h\r��V5(��R�X��!�,�/˦��<�yd�!�![ݝ����2�|v�LM.����`Xn�1E�I~Se��`��\\;�J��Q\\@��Q-�-�v�B��Hw`�|/f�Y]�#;y����\\���SFg`\n,qg��l\0Yr���\\D[ѳٰ��)8�SU\n�@�RA߽Z���f��,5�N��3�toҫ�S)x@�X���5�-�h�zB�	�\r�7n�V�^\0�5�z�.,z�j�~�ׯ~�����]�a34��n\\�M�}~|���������I���/q��Z�_�ʨ6MFs��\'+c�;i]�7p�R�6��bΖ��.x�.ی�m�yS�\r�=s�.t��q��R���_m:�{HY�yOE����������6FI�b�V$��kV�C�(<�(��q�11��Ĝ�}��1z*�V>���\Z��Ù�48��\n�)~�i��Mp�Շt���*Wi�(�	+�<�p\'G�N�@C/`^��;�+�MjI0�i��2�0�d:(����\r���j�N�_��U�B�	�u�����-�*�<Z;��Zc\"�^-�^�Q���*��P�\Z�c�2��q(ízuӢf�h�.����kF����3��h,S��b�pK�x!o��q?�w����޾����5\'�4��=7(;�q��7��-��ۗ���dg��\\�}��S;e�6��kw��?|~��bnr�Zk�&��e2�3���_3�\0����UT�\"c}��Ǔ�8-OA�_�	�뻃�P��Rl<�N�P��\'/��\"xצ������\rC���֚��Q$�,�q�v-�á���1�]㲊�A84�;��p���zz�lD��V3E�_��NW1�&��Z��FD��+��+TU�Jw���6^�5�\'�f��٩�u���!�~:��s�ț�-I�&��Ē��m6M��$1m\Z���\ZY](Z��C/&����{f��h5�yQ+�3tY���&x�{�)4vK�C�9�j>��\0��ˆ,��h�k�ϟ\'�A��}�oa�K@c�������s\\����ڟ}aj��~���>.KN����f��1m\'ڬ��D�6q���pJ�d+J�%i�\"�Z�!�<��[D�=~���G��el�;��(����u�Hj����J7�Ɲ=	��$�咩�m`��SQվ���MhO�]CMG�f5����9Ybc߰`l1�\nr�\\%�6}	��;(��69Z�k�Bsh��w;����\"c \n��\nM�z�8�;���.�b㯣#��d�O~��AG�\0H�^j\0�ą��Va�s��1�f��i�����\r���<���5#���;�-�tk�6�Ɗ���*W�/��I�ʣF�4���p8�&CF�\"��^݌9�%����ۗ�s�n��������=\'�^��b\0�;mT����>}yD��+������;Kyo{/Xz����}�ϩ�0�~ �7���E�)�H��fϧ0�����՛[ԙ	d�3��%�+6r\rG�ˆ��3_��tIo��b�;ǢaI�]^D��dg��1z��J�Q��u7p	����E�Y�f��M.�pvdx�����;v��/&\Z:)Z�\n�`�C1��\n�\rqV�*����,M�FkO�c�ᢀNP���V��nd�֒4W�.��	�\"k	�o������,I\'b���R�F����<Z\'�Pl��AlОg�,4Of�++�o�d�	ď��C<7�a�o�+�>��Ƈy�jp-�2+�@���D��6ߩ\\�a�!�����B�8<�e�p)8�v?��l��ܟ�2�F�缈;���s{m���\0��}��k����@����Vۥ��Z��i���ã�jF�n.NW�����6Z\r�8��Z�%.F�Xi̙�͸B���c�1�2��X�d=�<�؟\rޖ�cf��v�.m������%��0{��Lq�L��z\r]a֦R{��N=E�:X\"�&Co��1+4P���[_�\\�c9d#�(��>5���G�t� �M�haz�65�O�r4>�g#m<fm�3A��%���7T�0�?~@ѡ�$w�7O���\'&��|�′����~�iյk>6��\0�#V���7|>�G����A\"�}\"�ɶ���5]��\nC`���\r�L|��r��T5��D�����}3Q�V\"���>\nu@��*�@�2���{W�a�u݇��s�d���޲I�d��	R�Nϊ�oZ^��������K]������d�GOU�1\Z�g��IwE����R����{����.��[��K2�\Z �br�UC�уkh\Z�i�N����sЅ�!�\'dC�͑?1HV/�\n�@���N C�Y]�t=:���U�m��;I\rt��c���\"�q\0���nw#=e90صS�#�M!jՉ;�le��֡�Q\0ߪ��Of�!�m���\n�g=���>Z	��U\"�b�������1)A���\0�XR<MlY��ip����p�����br��:�`1)��]�k�fb���<�������:��c;���Xbx�xR�q���\\�d�-�UG;��T�����l@���_��s�#L��{W{ĽUxp |�&ŴQ�B�Ş�_+��ִ5&�ae7��Ns�1	vN��k8,�����p.k���U�r?|�L��O��׾�������	nMsO��ê���Q]������&��#��&Z���jy^��d`�r���4��l����H 	�I�!׆~S��To�QVo��Kk�-0�qU\"I\Z�s�C���	<�U�i����U�\n�k�J����j+�iܥ?�X�O���l�����}�6�19R��ec(�@M�GJ�k�s,A�˅ɸ�ս���\r�ʒ=��ݾr��L����<z>[;`0Z����X/�l\\C���,BQw�E�W�u0��P�јg�Yb7�>��]{BZf�o:��\Z�C���Ȥ�a��u��><��s�|ܷ�x1��&�q��%4��?�ƾ@��~Ǿ�h�UT�)?}�.�	(,$ �Kܐuֻ�:��ƹJr^�+�,f��~����tG��_2�@�b�����\Z�s�W����xA����������Ra��\\L���xX���!�ӻ���h��m���߇E�D��Ɣ�c�Kp23�\Z-lzO�Tn;��t0��i�M��p�������e� o_�����˃�Bt1�ˬ�N���N���HF�E�@x��®0�p�~C�\"k+f��*�8�Q�8�n��f���3���B��E�8��<�;șc�obb8���dOf�i����*��\"9�= �\r��u.@%�\r���xZ���C�#u�@�e>j�)��4(�M3���m�q0F�X��-�c���=c���W�� ��)��!3ء�U��>T����jx\r�,��\r���v�O-k��^�	=[Ζ0���`�����J.L=�����y�6���d�=�4[-Cay�J���+Ԑ���\\�-��nw��V�rly�6�\n8oo�΁��z1���Z��V{��y|g��Ѿ��6��뻶�e���~�JʉH1seA���������D�c@Z�\0p�qU���rl�Z��\Z�FD�^�W\Z�9��|R�Q	{l��Ľ۝t b0	(�k��&��i�\Z�����ę�t*�\Zf���uӆiDaՁbf�z4�����1#Yg�d>o܌bfj�<C2�Wb�9�>o��_���a�nGQ��3�\n�e�� ��0�ܐ��L��k�v�d#\n9+̪��%�o��f�PNʇό���%,?V��~�^�i��f\r?\"Z���\r��<ۙmw��\'���U�8��>���r��F̚�����\ZCsD-��B��4>�G$�����Gl\\;Qsˑ��Eo�{��u�,�6��G��[e�y	��c7k�_\0\0�/�� v��������;�oh�S��`����>����m���U��-�~�W�A��[�1Z RM+\'��[X��&�\Z0�s�!�m��U�X��D��tC��K������$ЀF��\Z	��=��eQ7:��:٣I ѧ�8^���4·ܐ4�v �&�@t��x�kвf��EN����D;��>C@rc�hB)�)���HWy�01�V>XuaX����X2q�4^p��\'v��h��ʌZC3�sF��@�~�L�í� N�ں	+�6N�?~�i���v��D��1�_� vhp>L��P�#��2�2��Ln�l����A�*ܹ��y�d���4V�/4�%�\\�m�X肱$P��lw�G�/��X%�]�]\0I��`�p�\\6I���C�����j��E�B�\0�aҌ���7�����;��~Lv����1ym���ߒ��g}!���x�4Kh��@����5�A�8�P��,6�\0��u���m�ެ\Z�h3\"�U���DX�,�bPT������P٘���R��J*I	\"�|��ڔ��U��&�l	��n�?V�ed��f��M�{�iD[����Z��nlR�Q�x�֣�ڸ���\r=��W�+xٖ\0�D~ܭD�^BW�A�#(7\\��zp�U��4�\"ʃ�׻�9:\"U��D�9���_a��-���]�>�0�Ǚ8���`��hNaQ��җ�fj��\'�xl��\r���4�MN)��a�C��X�;�o|��ɿt	��L,JI�*#ej�$ϵ� N�G����<\0�s������w��q���[��J�̩p|�v��Wl��2��\0�\r�H�>�s������_����;��PWv�;����?oHN���vD�aZ�۫��Ll�P}nB5�[E���\0,�z\Zݐ}VC�y�بz���L#����pa4G�����K�~����k4.6�3�k�ς=mS*�Y���$\"���=[^T)�S�97�B�6���A~2YBn�\rdڼD_�2��i`Xks �38ܬ~�Fz	����v�̋3�\r.e�o-C��C#���\\�6���k8?�[e�����<n��*�z�X�q׿�L��Ǐ�5��� \"�nh�����}���6�L�@f��%y\ZӞ\rΤ�Pյ�\0)�?����A�h���Ԫ�_ӽ�0ה��F\'j����P��〰�Ye�w�|ۏ�\r�_���1}�z�5��x�a�y�Wx�W��p��DH^�S!:-*�W��=_�sz��	�~>����%�<��� �C��\\�P{���ڟ}^2���^������i�v\0u|[q�ó�e���1V;F�T�(e�3l�XX��ɒ���Q�pEm{<���\'��;�wu1�R���twS�Q���JzE�GҮV����o���.ܸ`��(�;Uۢ�k�(\r!�}�C��p�{��M|���}eE} ����^�ZO�55�F`ל\0�8i��Nv���\"�!Yi*9P�#�l	<`���q��2h!A�z��h��2t��y�n�t|�/Kp#ޡ��S7�@�Y�U#������\r����eI��Pc�!}_`+\'�)��A�f�8�g���8�Z�w�ZVԝ��7�������3 �׈���m�R0�� Z�tӫ��۴�b��@{[��^C֋\'�R�B�����k�����)�O��p��1dڲ� ���sdP�06��\r��C���,_�:���D:3�j=���jw�9�a8���]{��Z멵��Ơ�e���_wv/��B�ˮmO�a��C���{���\0;ifƝ�TOZCC��Nc�KBi��;�T��/*�d����aTM��-30��ډ����ۻ�M�!�~�;}�2ܘ�x�\'���1�8Mh��Ԯ~�1�aR����W<��3;��+fR߸Zx����h�f̱�xh�~�����������V�O�п�X��v#m���]�ѭւ���V)Z�N�<uv�c�Ć���QX�k!���kW�\Z�1|��}����/��J/�v�׉V�������0J���=����#��;�y���(�P�ڕ\nZ�-y��z�*��(�j9�`1mQ�;T5|{l�y�\Z���Ȃ�q3�[�q�v���VN;p�p��N�|��x��n�/?>сf\Z�.{�?ɝ�/���kp����{c�z<����m���`5����iUG���?�����4o�#�hQ]8k���OMd�exj�N�\0�[ݪ���5\r���:fpZ+CVOǪ}�r1�X����ű�T������,�kF��w!3���]��\\t�y�]hFfʩXF��R��OS	j�\r��d5\0�B|�1�BQ�R��bG.�e�Ku�)+ÕƪYR.��Rm�t`n\Z3T?΋�x�	WE�H^�I����bF8εe��9�ʤ���G������\\�t���2�.t�Ƣ�@�+Y:��\r�����3yE���QW��d�˲��ߴ]��uiș�����{(�$tY�	�m��C�xIw�Ewd�Ħ뿿�3�aG\Z�L���j�y�x�D�]$\ZR�K�;l\0����_����o���Bؑ}+��,�嗭*��pZ�U#�\\�#�P�\\Vu�4�j�V\n���p��h|��`��L�٦ps�^NǶ�Kj�j�X =x{�/\0����~\nHs�&�5����፾�S�Ȝ��L���-���\'�j`g٭.r=e+{���w2�c(4i��g�I�q��Ѹ���Y�����V�1� @\rX�hL��]��yW]nG�L|G�\Z�(a�%�Q�V(�9ZH����[w������4�|��68v���;�K�G�/�V8��y�/5���z�8KU)��.�@��,b���~�Z4,u��q��S�Tw	���%�|�&����4<u�7(��wm�_���6Y���!��2ǩ<Zs�M���#߿�5��KJy��+���]�/���u/v;#��N?ٰ�o�O��X�8�7��b�~��=#J)*����c�걨�`��V�d���p���R��Xq;B�ۏy&�I�\rb��2t�Y��^�:jݴw�7hy�ֳmOF$�8ݢ����U�-�Xu��\\/n�֌��`�	)���D�8�W��r{�\Z����q��[Bǵ����\'cJ���l\0�n�e�\'i��&Y�4����n��!��#,ac�r�U^��n}Q[�����h.K����l pK-�.��k�o���������9a-�D�ڴx��T�>~����+�.���P���L��T��uPU�i��x���O�{�l��X\\(�m�_��Z�wY��2/�ASz9<�5ƸC<�7��������8�\rۡp�x�Y9*�7V�m���%o�(�べ�l_lJ���\r7˖���cq�dɋ	,e���V�(�\Z ���Do�]1���O�i�t����K�#��QO�y�$���j�`���*��Zѹ���^��|��V\Z���Y��us�õ9N���Wlg�jXU8����f7��hK���M�n��w�\'<�Fw_�ˆq���K}��$��!��4�k\n�r�����eD]�ќ�fS��ȃV[�G��ե�\rnO�C���kG�Y�[�D�\r4�i��+\"�~c�]��*/���9*����R�j��.��J���1��������I��aYh�E/�U\"��x��5�<^~1�]�t:�������ĸ7l�w��ȑM�>��BQ���Ҡ�ǘ����\r��>�ԁ���\nj[=�����r�l嘗j��L�fd>�Q��R���0�����=I�{�8���Vڻ�M��w�t�26�p4����vՌ`\r���d�y����j:���S*#�ƕ�C��_���%��N� \'�gc��Ŕ]�~+Y\"�S�k��8u>8WA��`��1��EN\r��_������l�_\\b7�|r��Mˇ������A$�y��{��s���*K}}R\n5����[j6��ҹ�R������y�/���l.|�զ�8h�QR�ܕ���z�jQ�VQPݛ�7�)���!��U`ŵgѠp�ԿS/M��h��\'2�5;O&7<����ݏ¶m�[�C���	r�k�	��s�7�&��1�^��m��\rt�5i��p0�8(%��������3*�7(U�����]{g�j畤�k���ٻ��r5jKu`�\rN0bR�Cؽ�\\��C���>�K[��`C�O{З�x��nJ�D~��m�4�O&�����NVGN��*�����*V5�l�C�Hu�&ک�6�\Zסq\"PH#@���x�L��;Y�<�ލ]�\\�e���X�MJ[m~r\Z�I���1\Z8��d�,��|	�z�!�M���c~e.\\�쎖����*N�=�z�Z��~�����t#����\\ ���[;PiN�����yq����^�4o9f\rY���p�X�=�Fy����Kd��϶u����ݎI�6?s��n��@v��6\nvo[���8њ���a�D�3r����VAM����Z�:$��P���!#X��jH��bP�Q1�Px���mB�F�gP��Ak젛 �������WjG��YA*Tk��Dh6Q9G��m��ȼ�5R��ڀ�\r�BcЌ��Ns�$���2\n��YO}��-�1[>�B]�.���04�s�\"O�~����oǿ��4}��Bi{�.㮵�\Z��5��F��GR��%�\rڔ\'rU\ZU]����U����]�W#��xu��և�֔\r5�a`�R���g�Z��I�\r�N#q��*K��W�/E��J������m�&��K�J�鰵x��/o^wޣ-Y��I����&r-�崴J���}�}0�k�hl��.���=�=pn����ܞ�V����[=\0�oԺ�	4���ul���%���]�&���Zl�5����{m�������:B׋Y4�P|�-����8�J?y�n�1I�M�,��A��)�U�@��R\ZY�64/�W��*��Ȏ�ο��zq�̀oX���r��n~����۲���B<\n��4�X t����ySR��z6l\ZE����@m��]Ѕ#�ʶ�����J�2�\0q�~���1�������:!4�A���k��Tl�G���M5�)+�,��%z\n�%����!��<�G�M�D�1t^#�1]�z�ڶ����@�aõ��ǌ�Oľ�7gK���\ZO߶�_�34�졙��T��	�VWkk5��6��|�m\0�8��x�_���/����n<$X~�\Z<h�Êfe��V@��`���\\�f�#�n$d|��6�=��XK[+(Mj4�#YZ�Ұ}���=1U��pBO:�x�[C�A��a�ሕ�����:���9����n��(��--d�i��\n^�-}�UV��X߻20���o�M5Şz�RWN�ˍ���٦�Ԛ���+؇�Lm�������\rM�7�͂�Ω��^�!�u	�4>R-.�M���f�6m\n�����k�M����c�b��@>�3���i��~p�~�[�l�x<3MJ�8x��ڼ�_ ȓ�\0��5�\r���=[�O�f3�����|���5�vX�%�,&l[1��g��6��تi������]]�0���c>,���r�Wb�Y�ڿ��wS1�^���kA�Maq�&6頄��-rA�eECbgA��{v���&�\nVU.��(�P���+ky4M�[�U�fy� ��\n�Y��C˜�X�&��v�?8K���\'R���/�|�\r�Jm�� �۱\Z���nu$�N�����h���P���\'Y/�O6H�(��?�Ŋ}AWJ2�L�_��ʞk�Y��gD��^l��;<��#��!����6��Ħ]�7\\Fę�(�tt��7t/%z^v|�Wo^�/�Y�O.����U̬��h9W��\n߮��\r#.�FQ�3���p{��Ȭ3���_����^��wuO ��Iј�6pS݃5�zᓽ����b��֔Y�M�O�\neZ�8�VO���\'����������U���a�սn�Ķ���6i���)f���m�[�#�%V���H��\"��	|	4�	-�.�v�m젲�=���	LXs�C�|�\Z\'8���l���5d��ζ��5cP��]\\;�&$�����43��֣��q��\0:�Vr�7>��+�b�&nÚ]N���+��;�JQ��~\"b-D�8�bv��������_�n�D�v�-)��`�������n{W�P���ō��w9�\Zܺe���T�Kt\'�.��V�/8f>TfAA>�%n����]�D�K��\ZJ��%���~������}�?�}5dw���jssuѬ\Z]�t\r��88�r��lkby���\"ݯ��n�|�Bu�fsB�pp��k�S��_���	i��r1مZ q>��3��sBS^<���]�D�}�Fk�� �^��G��\n��*K�#�S�����\'V���exM�kH{�H$�E��J�æ�:2e�o,�6��L8������9�WlXqE��_7���!<���q�/�P#���8��@�����x�������n�7r�G�k�9�R�H$�3��F\\���������{p���k\"�H$�*b<��FQZMk毭�/�1����W>�7�M�Ob^�!N��4��x�����uL���1o��ڎ��rM�q�_Ӗp���S��˵C\"�H$��}+.9\'��}���~��uM$��������nɵ9�د��jL[�0�+4H�V��Ѿ���5���[�)���\\�FHo�}���^�x�Ak��ۂ?� x�q�kŸ�K�F\"�H$�0��>��3����A���s_L\"�H$�`|8����|�!ٸ�� ��51�~��[�=�^B�q7�߭T=���PMێ��$�7vdӵ���!&wg��G�\n�<3-�H$�C|��;a)�ʡ���k-�\'�D�R�-Ѷ�j�.�����c��nmhe�_��q 7q׈v	j�%_p�y�ڕ��/�����2��B�K��6_�B�S7i{8-\r#�ɸ�D\"q.$6���G<����p\"�h�&�D�m���>��5-���f�6�k��y[�+����4�G���ӈ�_Sr��Z�p<�<^ѥ������ņ\rݖ�G��8�D\"�xӀ���P�Fɸ�w;�q\'�Dbn�X���ӗ��K�/�ݶ���k�Ƚ\Z*^�U�_ˎ��\r��҆�m<��k+��)�\\���ܕJ#[7�%n~�&J$���Ʋ%��0��s���E��q\'�Db���~⽺��D�5#]��{�����n�߁��������Ɨ�k#�57y�[��n���9M#���zDےj�o�%�D�m�x\Z��Z�^�KWy�_NW�����H$�7湎&�E�B�y����z�\\p���mS�����f�><\\�w��q~��|Ie���l���������l�D\"�xKhl@[uT�8�r��pɸ�D\"1\rce�R0%_1o�k��1�H���I_h�p;-���>/d�)�g�Y��ݑV75����wח��f�D\"�xc���!����1g��U枌;�H$�\0t3��	2-)�gc�W�WuDs֢�T���{��́��%���(r�o�r��\n����p9�ɧ���l�D\"�xc0��g�6�-�������aL|�&�D�=�틦WE;���+�YS��N̵|r�,,���l�wr�Sa�!��Ų1B��?ר��s���pK��n�D\"�H�1�n���l#�-ޫnO:�D\"��\nSr}���E���]L�rv�O�1����X�w��\n0{�D�[�jz&�mo�/�mݔm޾D��{�\\\"�H$�ZK��S�Bޝ>u8b|<<�w]�D\"�\0jh�ֽ���sK4~\rn��+[WF���:߻��������d}��������r�Y�\\7_�a[b�{7^\"�H$�<�>�/\Z\Z���[�[��D\"q)pй�.��rX=������:�Bq��5�k�����]��=��rOw�PLC����#�yy=|m��K$�ěD�JKr�o�=G~1�̦K$�Ħ0$���\Z��g�\r�`���qW8�2�x���ۻ��l�~�,��s)��6m����x7w^�2�)�|��K$�ěD3Z�ix�c]��w�$�Db;8&[~� �J,�ʇ�q�����\"���/4��\'N�����?H���k�x�W�><B��V���0�H$oB�����W1�D4/\Zۻ��D\"�x?(����-�6�[����=XC���������`w	(?8����Ơ�l`Ѝ�)��wp��ǟ���!\Zn�ڻ	�D\"�VcЇ�,췜��ܹ7Ϭ�D\"�Hl!ў#7?��j4�~7��@��\Zk�vs�V�,Ze��µ)��Ce\r���U*��C�}�L$��[E3�Yp��͝�f����H$�w���mM�c�e�����gӜ^.w|�>b�~��ٖ��ݐ��u�؅\rD�R�ơ�OX�(�#ˮ�ǧe�8_���D\"�Hl<FƝ{�{D�}����gѼ�e�D\"�H\\m�ubA8^.yഘl�VՅh���� ��W��nˍ��b^�\\��4N���q��ʬ�:�b�g{��A��F߻-�D\"�!\\ۅ���x�!�e(���H$���8ʗ[vM����0��}:�ܔ�a�xwY��u�h� /Aj����r��\Zv[?R\Z�g��b��Ox��o�ڻ9�D\"�H$�Db7��*��%�[�J����0�Ýb�����x�g�簕�7��6S�x�xa�/<�.צ��×-G~��K\\��g�+O/_ػE�D\"�H$�Db7#f�m\rg�ք�����Y���m\nz����W�n���c�I��f{���M�Oo��n��-v�\0z�\'�D\"�H$�����S�G�.��>���*|\"�Z\Z�qv���������G��+ަ7M�k>9-f�k���_P��y���O��x����ݴ�D\"�H$�D\"�3j���Mt�p�0t8��D�)G=2b8�de��+����l!ٻu�\0����;����\'*�`8�˝ږ�+����\n��?�ݺ�D\"�H$�D\"qг��Aj����Ԧo�$s�?d\\H}�M�O�(I�,��\r���˦lPVV+�ŋ�8��Ri��#�6�&��X߻��D\"�H$�D�&\0���A�O�_���dx\Z�\"�ط~\"�\\�]9�m5t�w3���ZV�t;Ʋ�˚g�ڝ����n���7�;����N$�D\"�H$��Vw;Q\\X���z �	v�Q獫�̶V�ǩ���9Ϝq=g�ݑ\r|�����7:�|=\0Mwg��A8��|ئ]~$J^�\\^t�N$�D\"�H$���9--^�es���S���dr�o<��|!d��@�%��zR�/ݪ\Z>y\Z��)��,���݅�����B\Z!����K�j\"�H$�D\"�H�9p�Zo�ζb*8]v�\0t�A#�൑�L��t�ڤ��X0�X�#;��a��&��1h�)׽Վ���J��4s�y��&m�H$�D\"�H$����G��.N-_3���e�7i�Ƅ��̽F��t8[_\\7h�U#��[�[{��tW��t9�,���f���m4����5d.��PK~��4���k�D\"�H$�D\"�xCx=\0M	�n��Z�n���py8��t�{�[�]�8�k�؇�Ǆs�����/60m�s`�\Z���=��D�@i#�5�6�iG�Y�O�\"�x�������7�H$�D\"�H$n\ZLZ)L�\Z� u�04��\r㮥q\n:�iw���x�x�ᇸ7�	>T�Ͼ�\'J�.�`ǲM��e��9�5��|P.�r��x4�ɬ���X�p�Z��{�&�D\"�H|)0���C�gE�yoI��R�C�!ʶ����+V��2z�6m�C�c^�۶�_D��+)&6��q횯�<���?��O���:�ކ�._�}�V\"���[^l����OA&���XLz71������|�]���K���z(�5ꍽ�Jl�0���A��/N��B%*���U����]$y+�|Q�N[7�6�ɽ(�f�p?���b&���ҭ\rM����.��s��m��E�X����ʀ$h�v�y�3&��|����\nE54�}pw��>6��%b�RlO;�1h���9��FW}=4*R/+�ʥ������;�Е���qm=\'��w�n\Z�srizܚC��hQ�{˕����s���_t�oHA��\Z*�Vߛb�{w����|G^:�|�-���uWf�\rl�P�b8n�\\�b�\r.���p��,�����_����2fЭ��!K�ws�xtcK��u�h{+bN_{@�l���ϒ�{��B�uȦ�)2��Õ)�䢁�%.�c��8�h��hcɁ,IJ�\\�z���dܷ�v]��ŏ�8c��`ݶJ�3���8��c��E-vi�_2�T�۸\\��p�C�6\'�+�Üofc����sK���Ǚ�i\'����?�Ώ�u6#�mzˇ{c��V����/���\'�&&~R���y����{��(\0\r�2���D/��b�]�[�j���ov��-�+�~F�ho���F|o�\0��EVX<�]$�Y4WI��O�o���k�qh����\\��{j��\rB�.d�ALTZ�0��\\̽N�)��\n�GF,��뼛��ǐK������^tͩ�{� ���?�j�)I��t-a����y,׎����U��~>y� ۥ!ì�9�<��`ۻ�7M�ɥ�1ԁ\Zso�^a�R.��Ц�C����o���.��&�?l�v��u�������u0S�-�8U�g�k\\�\"�/e��~j����,����h�q���b���r��h�3H�耵������lj)�n����#�b�ۜ7޺��Y|+��\\.vF�y���n��|�����h%q�*U: &����2�Jˆ #\\�aN.o�_�G\n⼯��$�M��?��4$=��f��0 ���nPڍY��;ƒێSF7Г�%����.�M%4)�d��_���4�(���������5�hÎuϵ���D�#������\ZŦ��_��v:���Ƨ���<���86��y�t2q1�1|s���:��͛����[�D�u��*�X�	��(��C�_B�kV�mHq��\"��!$o�$��	j�Nj9u��L�|n\nG:�Cds����uY�Ɂ�J&y��~��q���\'�GB���ۦ�|��^ֈwSu�@��\'��ա�2)�S86;����>7�bYƝY��\0b�o輾�3�����B%\0�w�+,и֯ζ�_��b�Li������D\rZ�1LU�\Zkb��_�D#S6Y������A�C/+/��ʔ�ܳ�t$��hC��ah��k=˽Q��/2[������޾���͢\0�/2�2qQ���[��v���{A\\���u�ĳ?�$��m�x���(��OE�o\n��yS�}���I�!���$7�F�yv�vߏGV�E��lq�\\��j��,�%��Nw{�#ڰ�#�Ǝ]`�D�m$��\'1�J��C�^���x�X�nG�[�e����9��FW��ӻ���{���3���A�T��P?㊿��ii�\\i��uCi�uԁ�Fl��d�3��3Px���ݜX��\0�\'K�݂\0}��?��إ]�!�N�l����/��-���h\Z�F��F؝Hͥ�r#��#�0#m#���/䑒��0�SCWo�dk���~���,Z��:KT�ǛZ(��{��ʭ\\�seX2���k[�~E,��m���$����eR�d��{NN���7Kмgxۋq��t�튥�F7�5��G;R���)� n(�[D�8�J������t\'f(6x�ǭ5]InxX3.�!�\Zd?~�;�M��L����7�w���̕�S���r���ey�S	ak]#�%�ݒ���!cB�P�O_$�~�4���8�\\g�}%��l���N	,�3z��oi�q�Ч����U�`����0*��l�YGAs^����<��?�T�C�;��y/#��]8(&u;5��|�WС�©��n��.�SRkU���ً���\"�h�C|����FqIv�p�`�xz��3b<k��WMg�ʟx�n�d7��mZ�Y8ˆbl�նF�?u��s�[?��6K\r�c�t��Z#�\0먚���?�]U}C!D�5����zћ�e��C#�h�G���丞-i���	������n�hȶ�V�x������o}����ɕ_k�8�Cn�Ά��3�Z\'�?4>!FO��ȸ%��}޵���Ȳ9��%哿\05�6�������Πg�eb�z]��0�f}N���\"�^�� r�����@q]��F\'��y�9��/����㸜���.��Bs�{7dRl&��}�c|���˫$~����E�<چ�5Z�\\Cfg��-M�ϤA���#+�H�d��c3�!,J�F���q�O5��Q�\Z@Zi�\\ņP3õ��J�=��\\ԉ��Z�OO���i��Y�x/*mtVh��^�]�#���^Z�N~;���%��ę�v�ΤHo.�$�B�4��U�0����kN@#��_[Y%�Y-�\"���bYn\n�R���{�|����?��r`֕xR�ל��o@�5�<���_=�L���v%ڑG�hqA���[+\0��<��\\�:0��)�^Fi�ZF�8���9�̘ie��*�<;�:�o7e������1�7jwp�u���]��\'��6;�E���ݨmgp�^K���q�u0�<���m9N¹��\Z��:��5�o�[q�|��dK֍�oS\rB�V.*����n��P8j��U��S��e7hHA�R*�E��01m���E�$}V�=��D}�4m�)�	��n�[�\nn��̍R*�wJ9���-eb�]+�Zƪa�ód\r�W����3ǣ3ж�����g��$�?�.��<�o�i\"��/���U��\0Z��wD�c�V�`8⨦[Ǝءq@�.ƅ_wŘ������~\n�lH �j�,�������!f�5��E!i\"�t�\Z����k\ZF�2�Sԑ�V%�`ko��f_��$�oMO�D��4��MM��𙍾\"U���T�1|��T�UJU�g¦N�-\0�u/�\r�m�WqD�S�˖����(�7jF#�2�N� �1nŦ�MV�V�h���I\'��Q��������y���|�B0���T�o��w䭴�豁�5�?*\'3��ݽ\"_�-0P�K�e&��⭍�\n`��/�����q�5���Ԃ��\Z%t��B��Y���o?�q\n�tN����<Lн�v˗�UϹs�@-��%W\n�X�����L%���]�O�L��q�R2й:h��4����w���G?�u�|��)mw�?)��o��Y7Y�P��>�h6�É���s<��|�:��!+�\'��0�߀J6i�e2�zp\"����˖����\rb�1zy�ݑsv���Ҍs9�Z�KUZW��D�X��*[x=�a�,��.���Ib/{V���>-vyL��يM^��Ϫ`��=�x���vh\n?�4�2\"	n���<w���1�\Zf͟��>�s伪c�����J�&����Pr���X�|���z��͢Χ6��W�T�9|G��d��sSͫ#�8��}u�B0M�=�FEPǪ�u��t˔�%co�^-]w�\n��\\�wy���B��pT\\������`��N������ڍA�,�dvp��*�t�43��T����԰�e���O)�H�3_a���*G��<*M�i?�/1N���a}�\'���@���~�,�댨��EoT���k�4ѿ������%�ӊ�W��ƹ����8\'��/� ݆ϔv�r�!>8�Ρ�^�F�x𸽮!cI�+q�}t�A�����1��s�	�}hj%QW�\\J��g=�V&,�A��<H��������9�RP\'�B�px1�8�H���Z��X\Z������9M\Z�K��V�B��\"ך����\n����eҟ�i���j��̵a{��\'�yU�yM�����k�LJ(�������Q�F�g�%\n?9W�~�vp����c���ڒwT!a�ۨY��n���Ƥi��SQ�t���֌n�>�������O��GF?Jl�Sğ�G��za�������b�ZA�tG���t����q�w��e�.������c~VHR��3v����Y4aǴw��]P��N\r��2ϳe�W�m�۹;nƙ��r�۝�<{*����P��o������I�D�gш&�������S����!Fp��(�6w	�dt��פC�xl&&�])��1��?�0f\r����Ձ���mQ0���v�nn&0\"�{P���Ff�dD�7�&&h�Q���,�u�����+Y��J����wwF	@�ϟ}���&<@.Y��-X������pj�˟|�}0NdW(��fm,1�g�/��_��?���v�ݑ�.���������E�9���l�W���ɽ]��|s���2_g�2@��.ٽ���x\"��q\n(ISl���F�*���X���o1��+A�NU$�G4@�%;Z�d�!�4�ٺ;���6�!vq�T�D�B�zou1�n�,s>���vy����_�zA�=7��đkU��}��Z�ſ�4��:�Q�]ڠ>����Η�A��YB3��]�\\V�� c&���[��wa`Y)@_��~8N�H�k��6�{<s���BhJ�sBEp���z^��4K(�2l�o�\rk�A�?Ds��:K\0l[�}h�q5>w��w�w�Bg��8�w�s=+,��5����n/�Vۄm��)��M�D��lIJ�`:F�1nR�3�����u��j4� crȲc�����H9�ņ(L8�b�v��c��V˝�8��4�/�r~]�ROȈ��؍�&��F�F��]Qog�蜿w4\\�-��W󴀙���T`\Z<���z%�2?�M����1?$��6{��E�nUO\nd�;}��q�����2�B�p�k>[o�ఇt&��9f	��	>*�8�+�����x|���\'vl�l�Ͼ�<���o���6��+>~�3 uvg�u��R��1\Z%Դ�<�@y��/m�q�u��7V�q�#n^{mٷM>��ǈk��w�Q��u	��Ӗ_�9xn:�hT�̍{7J�O�6ݚ������#E����|;K�Pzqb�H����d��h��[��鱧��G��\0���ue�g4���z��}\r5���:ZƳ��y�o\nзK����7~߷` �1 	F����a��j�0��\'/;rcb�!#n�~v;���\\�w�a{��\0��8��בfWC?c�H����ZN�JC�}�� �nN�k�ep_�Tv=�\'r1a��C��	���*�x*��n;��X�c�iG{��p\Z�q�!����y,���[����/6.�A�\'%��v-�e�n���h�����iW�pg�@�&�pZ,��#�aT�[U�KI��\0����l����RLen���luU�K�-~�L��I�gjA�4Wf��eM��o�*rM�J�Ѐ-\r�l׭˫�lwv�X�A\rۡ��5\\2��8\\��3n�&GhJ�@���ŉ\ZLf��閤��J��J���j�^1P��5�\\\r�Qp_wE���zZdvS˃ϴ�\"XɁB6Yx~vK@c�)8Qe\\��Tt�ݒ���2�aF�X�lJ��`:�gG���I�=�?f]�Ψ\r_�A�R͘1n}6(<�������6U�?t���C̑k�3�Ë�0s���V����c�:��]Ȼ0���[oV�1���[����\'�YP�g�fng�:���;/t��LP)K�w<�Mze�\\����3A������8����8����mn���@�ݻ�?��vG��D��&�\n�K�����PMȓo�M����Zw���qb��-i˓ +�]f��(�PkuY��^��t�q��ƘQ�¸�IZ��*>0��a��\\/��f�i�g��j@)-ƋwT��H�)Q�o�&EA�~�k�-ŭ��B�����9��]����E&Ρg���c��\r�G�M��<�7��q��Y��\\Y�,/�H�B�5�ۍd�xI�\n?^\Z�c_�wUo�1���N!�_12~��tt�Q�W���V��2�\0� ���	�\\�:<`��3y)C̖�.���g��Z���FD��\Z+�\nu׀�މ3W�(�XzyKe������9]�y��� C^:5�X��3�\\L�j���@Q�í%~�YY�^_�2=�D\rCq�G�y#8s�.�\Z����:�V���BQ/�GG�.s	�քMJδ4�MZF��F!����ժ�]�S>BSޚj��_=�C�n����?��_����[�;�cx�Y���r�8?B��ݎ���#�Bt�&��hxy�^����-�#?\Z�_r<�Z��.=~ڒʌ�xV4|ǫ�qbiu���a�I�v:�h��_�M�R�}ehA��ks]O\0�β��1�\rc�;c���wČ�<��\"R_kD�<�z7�R��\r�f4��l��8V/b���֬�t�]�����	�h\rc�L�����+F-\n�m��I�xi��CJ���8[L�g�/W�E���}�E��΢~���g=a���1����i\'��Ygt�{�(�r܀���u�̪��^6AߎC�!���$���\\1��x�w������w���k������e��)֢�����\"�n�xM,�����P�q�ٷ�P���p�L�:2��:y�2�1@�QK��Ѩb�Aq�K��ڠFm�$�dߎ��l�w��\'�I�:�Q���`�E����qq-�}�3���/U ��G#E��	Y3-��ӻ|G�bӠ�f�bU;\\��>�-6Qȸ��=3\"F��q°���ƾ���KK��\nVr+����+�% M�#�g�4r�����G��a��|�����^)޶�\Z ��Mb�p�^s=�Y��.��p��m#��v7A�����P�&�O��p�%ZS:�g�%ƽlӮ�e$�n�-� x�ȴ[��ú�g��>臿����z�с��vC<-�?f��^-nXl+\\�%�*�(�1p\r��Zl�{��V�_O\0�K�%j�p����Ȉ��}�ָ\r_�k�P���n�N����uÚs	�%^�ｆU��o�SoYZ8���w��R�%�Tt��v�B�=����a����;��V=��3g�ɟ[�5�.ΕY�����%!��4�l��\Zc�L��*�~G�ݠ���b��YÔȬ��j�è�]Ӿ(��&[��������!��Kb0������m��+?x�8Pl��n�����uz���/t\r��I�1��ȭ���0���ax���,�\0�i]�\\ݾ����d�K%TD	;��!V�!?��b��l�n8$�@���&#���tG{����i�w%ɩFc9o��v��)��-�*VZ�:k��=�L�^r��:zݒ4T�9m_{\'�3V��]N�^�(��åM���-&�֩�d��eFK�:K0�~N��,��>�����X��|\Z�{�q�����\Z��E���h~׽�k��ӎ��E��4��p��7�#���_ħT�B�e�W�3����_�q�zr\Z]ݥ��\\qï%��Q�@��drM_W�����O�}6f�a��&�K�y>^�!��2si{f��Y���8E����f��M��/��z��٫q�����}m��u�1�=7b��q�;�vz\'!��\\�Ѐ��C.q�2�������\'�J\Z*����h\'3x�EM��!�6���^�1Zbze^iig	r��&�Kp!�K{��:A�8�[�pe���Õ�&���ט68�6���̏Z��2o��D�K��.�Dw�9z��$>_���o}����f��?���\'��(7m`�5)�qg�ub�F�ˋ�ow|�-�e�O��!�;k�ta�+��-�v�&��;.�^�W�2��Nb&Բ	^\Z�2ͮ�v��iὣ�0�˂��2��-��G���n��Cdbl�+F�d���G�+�W$�Ac�e��\n*V�\0���=a�7	��#ft��Ѭ����hf\r��!�yLF�K��9+Cmx��I��z|�Ȿ�(��n%�5f;D�m�ύ����\Zt�D%ɺ���q8݅�B��FQ��0{{u��^�I��=��o�\Z������sve�1�Nfh�jvq�,ġ�b%0��a�Jc�CHq�:�?M���y�����rlGp	�f)�|ճ��j��>�w`T��-U�j\0c��r��{�׉�)��]$�xA��B�_≠�\0\r�V���0GH7���]B���㟪y���=a�Lic��ͅ1y�����0��7ȓ,����4�_�ccj؇��5��j�M�.�����nBݽ-pK�_ȫ�܆� �0�rg�t���W��.���q�ۘ·�ۼt���Ŧ\Z�>ħ9�)��q!�I�Gx\"���A.�&\nD�擖�#��I���0�8�R=^J�-E4.C�]�]�n9 �% .ߵ�F��8᭡���e`LF���\r�=<bG��v9��8ƍĽ�s@p�!\nM}��c����S��ج�F��\n 8N�N��>!Ɇ�3h��@�_���e����9�,d3��ٶ����̮��/M1�V�p\r(vB�aW�-��oo!\"��ofFl,���\0���x��(���U�u �[B9�����W�|�vqjK{� c<�:�bٔ�N��\"����5c�{w����\r�[�hڍ&~�zӖKKr�\'L�Q �T�j��#��o�a�X`�Eo�q�0���ˣt���#���#]a�y8a�Wk�p�Hu��=�o,�g��ꟺ\"�1/3Tf\\��&���	HM�X���O�/\\TNg!<����&`s�y�n�@��W�n�*�W����-gx6qi�j|�̆B���tn4L����\Z��ه�ɫ���S�x��z1�\Zg~���-7]��\ZnF��>��m�Hs3�h˺h�í�����O�Iht��vƸ���8u<?̈́�+y�,�p���*ע^��wo�aġUF��B��N�m��s�8�Ɲ��H�h�.�u����	��Q\n3s\05������A��,l���=����;`(��V��)��Ѿ�&���jwVGV��ty�������%����w\Z��e���ϺE�\"۝�8��#%L؋�<�QgU��	y��g�{�k�i�t��E�9�kz��G��E6�k\"I��yq����#���`�I�ؼ\r�F9&�x��G�\\��ɿ���g��E�h��7.���[��)��d3\r1/��ۇ��I4���#�[������oQ{O��a��VK���Iz�^u��Jŀ��t�8�-詋$�l���!:��=U���!r�Yf��N�C��>�Q��C�>�>p��u~9�+	����Z�n�-��K�_�;�et	5IW	k��f��Ã�� �tCA���1�|��S�a��ֈ�v�/�u����C�;#�I��d�J��t�@�hIyƈl�\Zv��{?��g=MƔR�R�����溲.|m�p���v\"�u�ejqbk�� �C[,��pS�~��rny�/����¥]!���x�8��H�%��������ݱ�2�nhfaB���\"��\Zg�<�Ҿ+gq�Q�r[w_����2��n1�1>SQ� �@~~<�i��2����A�w�G�����w��a�$�d��[�z^רR���nTȧ��m�L\nH���O2+Ζ��u�ZP���0q��\0֑>�r��f�܄��:��g���?kآ=#3�trnꌞ3L��M��	����!�FjZ<ç���Hp�J�m���K�E�C�W�yE}��xW�5��ǅ+`4��8SjI)G��R�~�}�ٿ�H�S$ײ{��$��v[S�:}��l�H^���Hvv�ݟ�j�n��<7��6+�N��v:y>T����a��;R�6YmkW�#�07襇��ZǓ�9.>IWS�0�\Z�@r_e+WPE��ʢ����5�D�!p�����wb>��yɅ�$eU[�K��VļNF.�[����P�c��;Ġ=w����Hk�w���m�歿�B�����\Z��dId�t��P 4�� ���ݽY@��%���2M\Z�\ro\rʦ����#����qIΛ>g�ǒ�[Lc�Bɧ��D_�Cu]Jf=z��P��@�EG�v^�X��{��Mi�xg{0����_��\Z�þ�,Y����/�#�@���I�nW��^n����A��k��=-m�>|o�6ʶ.�:�õz[�O	���n�K�Ku~��\np�?]�,��l��=�T���`U�;��آ\r9�f%��c�\r�5j�:��������4�Ո���oO��F��a�:�&�3�Ʋ��ݡ��Κ=է�.I�A�\0~�Ѯs`=��r�%s�\"~�2��P��WS��Ft*e���)��݄�?5`kT������6P����#���i�w�lM��d�e,��� {���i�>hex���g\r�����`f��i�é�1�J��rf��峙k&���	krS�C=�Q �W�\n�\"Z7�\0~~�w�)�O�(��^9ln�.�%̽~�W��;�Ln�ܿ�,~�~{���ܟ{�����ќ�Y�y\Z(�6o:�㵨u͠�v���9&\0W��\'��K����p/���n��\Z�qPϯ��,�P��\"�k4�Ͱa��Ӂm&��63���\Z�(�l���;�<���i��N(Z\'��ޖ���Tʹ\'��R)\n�K&�DdyB��<u�� �9L�*Bc�����pT:*��t8�����n�ON+V����A�2^��Q�2�v��Q��+��E�C�����[)��S�M�μ�5������o���pu3|�;�.q�Lg袚��w��G?�5��͛�?B�ܹPlق�{����&�<d��_�O�����܂S��P�0��1Jc�хc���WG�>��D�4��f�v΂��h��	�:gC���0�.�&�ʌ%筕�&E�:��!���&S/>2� �°��vG�|�A6ys��\Z��(�l�����w>�n���4���Nifl7�\n�\\�\05��I����ɢ��k��_�j� ����JklZY�Q!]Ѣ�:�pd��mj��X�x�� g=kx��2W�plF�#[�j\'u���x�e�-�+p1�Q�y�7\ZQn�8��`|��`��%>���_��O�M�t��Ɵb �o�i���d���s��,������b��#k�(�e�|~�u�T�GlP��f�E!X�2\r�k�\rW[x���\r��3��n��S��D�-4K�����i4��H�٦M����3� ��#�l�΋��*U�����2��|h���V6S�����W�sbI�=����\Z��̑�BpB_!��C��j�n�Ǫ�^����ދV,4�j]+~\r��Bh\\�/�n��Kx ��\\�F����J�ٲO�X:P�����Mx��S��@\r^z�\n��3�BM}��c�\'\'��\n|�\ZA�����x.Y./Q��v0wܙ�t\rO;\Zn�{�G���\nY�c�p���X�d�z�|qk��v�j\Zc�y�*���%�!8rD�d鯋���]Bo�\0��%�\\����������K��8X�K������ĞG�Y���Bx�jF:fG�jsS��:{�vA�TRO �]�8_�V�2��Ĳȳ���KPq]�������qo�ݵ���]ޯ�p�m��˸e?8��ǌk\'����2V�D\"ы�D���D\"�	L�٬{] ����?����i9Q\\�9&�w�(��k�p����[=��6�_2R���r\"�H$�Ga��l\"�Hl�N&Y���p��U�R&^�����s$�aϵ���۶�{}BV��\Z���m�H���5;��D\"�(����N$��V�{\rJ��~���hw�l~�֟>��翖�!-�u�8e�+Cg¾0��[4��6eo��D\"q[�^���2�H$��p��;�@�ް;ޥ������M�4�.{��>{����w�%7��o�(�D\"�H\\xxr.�&���p(7����p��C���&��?�_�t�{����@�T.�μ�D���[��ĵ��D\"�x�0�X$��ա9���(�4t����~����¥�x��S�e���<o<��\\Y\"�[�D\"�H�l�do��D�Kx�z�����䖷�%�g���w������Y\n�%�H$��F½�J$��G��������/�R���u��\'��yg�0�,�D\"�H(�X����P�D\"�H$7�&��*�-c\"�H$n|nƸ�D\"�H$d�6d�\'�N$��\nȈ���D���)\'z�\nendstream\nendobj\n5 0 obj <</Filter/FlateDecode/Length 877>>stream\nx����o�0���+�4uq����sӤJ]��i�������n��w�@]͐��%w����|��0�5��x�O-����[�(H�����Wp�N!(^�F\\ e��!����0��T�����LVy����(�[�Vװ2���>S�\'���� K�)�O\ZR4�09	.�ߕj�0��B�D�%����t���:G�\\zAX5�p�_{�/�Y+ �<�!�$	�[\n�#�\0<y#�6�1{)�^�:�F�Z�N��f�`�M�u��nѝ����؅�\\�/ư���8�M�З+�s��!�A3�)�W�Cw�J��:	c��`�+e��?�*�6�ZL)� �9�P]\'�sMLLa1X��������8��?۾\"8����O\n/>�ﱌ�ˑ�l܁Oh?�6���|�\"z���	J����W�:�C��r,����3~<#�\Z�~7��ѓ\\�p0�]��G��x��_CٚF6��R��j|�u�����L����nD$\n\r����Z�\Z��/�t�n�\":v�)f�Mpߍ��p�%�Ώ�-�t�9�O��}S\"����E����N��I�vc��$3��-�$0��	%T�~�\nߴ��%��,�6��%ǻEw&��c2s���y�Nݛ��t�A�U����oLs�D�R�����z��{B�bȱ�5����%��u��==�KҾ%NM�)|7I:	���ń1�csO�p�qN�e�.�m8R�6��%7\rdg�(~1v!3W����:7��s���+��n��z�)�ܷЩ�<�漣\'�з�0&rl�}j�Q�YTٓ��9�!|P<����2�32!��9�tS.\0v$B#-�1~B�G��6�9c����P��g�c�%�T\ny��͗�vC�`��{\nendstream\nendobj\n1 0 obj<</Type/Page/Contents 5 0 R/Parent 6 0 R/Resources<</XObject<</img0 3 0 R/img1 4 0 R>>/ProcSet [/PDF /Text /ImageB /ImageC /ImageI]/Font<</F1 2 0 R>>>>/MediaBox[0 0 595 842]>>\nendobj\n7 0 obj[1 0 R/XYZ 0 854 0]\nendobj\n2 0 obj<</Type/Font/BaseFont/Helvetica/Subtype/Type1/Encoding/WinAnsiEncoding>>\nendobj\n6 0 obj<</Count 1/Type/Pages/Kids[1 0 R]>>\nendobj\n8 0 obj<</Names[(JR_PAGE_ANCHOR_0_1) 7 0 R]>>\nendobj\n9 0 obj<</Dests 8 0 R>>\nendobj\n10 0 obj<</Type/Catalog/Pages 6 0 R/Names 9 0 R>>\nendobj\n11 0 obj<</CreationDate(D:20070212152549Z)/Producer(iText1.3.1 by lowagie.com \\(based on itext-paulo-154\\))/Creator(JasperReports \\(CategoryReport\\))/ModDate(D:20070212152549Z)>>\nendobj\nxref\n0 12\n0000000000 65535 f \n0000072313 00000 n \n0000072537 00000 n \n0000000015 00000 n \n0000015006 00000 n \n0000071369 00000 n \n0000072624 00000 n \n0000072503 00000 n \n0000072674 00000 n \n0000072727 00000 n \n0000072758 00000 n \n0000072815 00000 n \ntrailer\n<</ID [<c0fb0ca84741ac745e809807b221dcf5><c0fb0ca84741ac745e809807b221dcf5>]/Root 10 0 R/Size 12/Info 11 0 R>>\nstartxref\n73001\n%%EOF\n');
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
INSERT INTO `op_document` VALUES (327718,'Category Report 12.02.07 15:25:50 GMT',327714,1);
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
INSERT INTO `op_object` VALUES (1,'2006-05-07 14:29:51','2007-02-06 09:27:22'),(2,'2006-05-07 14:29:51',NULL),(3,'2006-05-07 14:29:51',NULL),(4,'2006-05-07 14:29:51',NULL),(5,'2006-05-07 14:29:52','2007-02-12 15:23:37'),(6,'2006-05-07 14:29:52',NULL),(7,'2006-05-07 14:29:52',NULL),(8,'2006-05-07 14:29:52','2007-02-12 14:52:16'),(9,'2006-05-07 14:29:52',NULL),(10,'2006-05-07 14:29:52',NULL),(11,'2006-05-07 14:32:13',NULL),(12,'2006-05-07 14:32:31','2007-02-06 09:27:22'),(13,'2006-05-07 14:32:31',NULL),(14,'2006-05-07 14:32:31',NULL),(15,'2006-05-07 14:32:31',NULL),(16,'2006-05-07 14:32:47','2007-02-06 09:27:22'),(17,'2006-05-07 14:32:47',NULL),(18,'2006-05-07 14:32:47',NULL),(19,'2006-05-07 14:32:47',NULL),(20,'2006-05-07 14:33:10','2007-02-06 09:27:22'),(21,'2006-05-07 14:33:10',NULL),(22,'2006-05-07 14:33:10',NULL),(23,'2006-05-07 14:33:10',NULL),(24,'2006-05-07 14:34:05','2007-02-06 09:27:22'),(25,'2006-05-07 14:34:05',NULL),(26,'2006-05-07 14:34:05',NULL),(27,'2006-05-07 14:34:05',NULL),(28,'2006-05-07 14:34:25','2007-02-06 09:27:22'),(29,'2006-05-07 14:34:25',NULL),(30,'2006-05-07 14:34:25',NULL),(31,'2006-05-07 14:34:25',NULL),(32,'2006-05-07 14:34:50','2007-02-06 09:27:22'),(33,'2006-05-07 14:34:50',NULL),(34,'2006-05-07 14:34:50',NULL),(35,'2006-05-07 14:34:50',NULL),(36,'2006-05-07 14:35:14','2007-02-06 09:27:22'),(37,'2006-05-07 14:35:14',NULL),(38,'2006-05-07 14:35:14',NULL),(39,'2006-05-07 14:35:14',NULL),(40,'2006-05-07 14:35:44','2007-02-06 09:27:22'),(41,'2006-05-07 14:35:44',NULL),(42,'2006-05-07 14:35:44','2006-05-07 16:41:02'),(43,'2006-05-07 14:35:44',NULL),(44,'2006-05-07 14:38:49','2007-02-12 15:20:16'),(45,'2006-05-07 14:38:49',NULL),(46,'2006-05-07 14:38:49',NULL),(47,'2006-05-07 14:39:02','2007-02-12 15:23:43'),(48,'2006-05-07 14:39:02',NULL),(49,'2006-05-07 14:39:02',NULL),(50,'2006-05-07 14:39:58','2007-02-12 15:31:17'),(51,'2006-05-07 14:39:58',NULL),(52,'2006-05-07 14:39:58',NULL),(53,'2006-05-07 14:40:18','2007-02-12 15:23:30'),(54,'2006-05-07 14:40:18',NULL),(55,'2006-05-07 14:40:18',NULL),(56,'2006-05-07 14:42:12','2007-02-12 15:23:03'),(57,'2006-05-07 14:42:12',NULL),(58,'2006-05-07 14:42:12',NULL),(62,'2006-05-07 14:43:10','2006-05-07 17:20:18'),(63,'2006-05-07 14:43:10',NULL),(64,'2006-05-07 14:43:10',NULL),(65,'2006-05-07 14:43:38','2006-05-07 17:20:09'),(66,'2006-05-07 14:43:38',NULL),(67,'2006-05-07 14:43:38',NULL),(68,'2006-05-07 14:44:14','2006-05-07 14:57:22'),(69,'2006-05-07 14:44:14',NULL),(70,'2006-05-07 14:44:14',NULL),(71,'2006-05-07 14:45:05','2007-02-12 15:22:50'),(72,'2006-05-07 14:45:05',NULL),(73,'2006-05-07 14:45:05',NULL),(80,'2006-05-07 14:49:22','2006-05-07 14:55:17'),(81,'2006-05-07 14:49:22',NULL),(82,'2006-05-07 14:49:22',NULL),(83,'2006-05-07 14:51:13','2007-02-12 15:21:57'),(84,'2006-05-07 14:51:13',NULL),(85,'2006-05-07 14:51:13',NULL),(86,'2006-05-07 14:51:36','2006-05-07 14:59:38'),(87,'2006-05-07 14:51:36',NULL),(88,'2006-05-07 14:51:36',NULL),(89,'2006-05-07 14:52:26','2007-02-12 15:21:22'),(90,'2006-05-07 14:52:26',NULL),(91,'2006-05-07 14:52:26',NULL),(92,'2006-05-07 14:52:36','2007-02-12 15:21:44'),(93,'2006-05-07 14:52:36',NULL),(94,'2006-05-07 14:52:36',NULL),(32768,'2006-05-07 15:23:29','2007-02-12 14:52:51'),(32769,'2006-05-07 15:23:29',NULL),(32770,'2006-05-07 15:23:29',NULL),(32771,'2006-05-07 15:23:52',NULL),(32772,'2006-05-07 15:24:24','2007-02-12 14:52:25'),(32773,'2006-05-07 15:24:24',NULL),(32774,'2006-05-07 15:24:24',NULL),(32775,'2006-05-07 15:24:24',NULL),(32776,'2006-05-07 15:24:24',NULL),(32777,'2006-05-07 15:24:59','2007-02-12 14:57:17'),(32778,'2006-05-07 15:24:59',NULL),(32779,'2006-05-07 15:24:59',NULL),(32780,'2006-05-07 15:24:59',NULL),(32781,'2006-05-07 15:26:37','2007-02-12 14:56:56'),(32782,'2006-05-07 15:26:37','2007-02-12 14:56:56'),(32783,'2006-05-07 15:26:37',NULL),(32784,'2006-05-07 15:26:37',NULL),(32785,'2006-05-07 15:26:37',NULL),(32786,'2006-05-07 15:27:22','2007-02-23 10:25:03'),(32787,'2006-05-07 15:27:22','2007-02-23 10:25:03'),(32788,'2006-05-07 15:27:22',NULL),(32789,'2006-05-07 15:27:22','2006-05-07 16:41:57'),(32790,'2006-05-07 15:27:22',NULL),(32791,'2006-05-07 15:28:20','2007-02-12 14:56:47'),(32792,'2006-05-07 15:28:20','2007-02-12 14:56:47'),(32793,'2006-05-07 15:28:20',NULL),(32794,'2006-05-07 15:28:20',NULL),(32795,'2006-05-07 15:28:20',NULL),(32796,'2006-05-07 15:31:30','2007-02-12 14:57:51'),(32797,'2006-05-07 15:31:30','2007-02-12 14:57:51'),(32798,'2006-05-07 15:31:30',NULL),(32799,'2006-05-07 15:31:30',NULL),(32800,'2006-05-07 15:31:30',NULL),(32801,'2006-05-07 15:32:33','2007-02-12 14:57:42'),(32802,'2006-05-07 15:32:33','2007-02-12 14:57:42'),(32803,'2006-05-07 15:32:33',NULL),(32804,'2006-05-07 15:32:33',NULL),(32805,'2006-05-07 15:32:33',NULL),(32850,'2006-05-07 16:03:11',NULL),(32851,'2006-05-07 16:03:11',NULL),(32852,'2006-05-07 16:03:11',NULL),(32853,'2006-05-07 16:03:11',NULL),(32854,'2006-05-07 16:03:11',NULL),(32855,'2006-05-07 16:03:11',NULL),(32856,'2006-05-07 16:03:11',NULL),(32857,'2006-05-07 16:03:11',NULL),(32858,'2006-05-07 16:03:11',NULL),(32859,'2006-05-07 16:03:11',NULL),(32860,'2006-05-07 16:03:11',NULL),(32861,'2006-05-07 16:03:11',NULL),(32862,'2006-05-07 16:03:11',NULL),(32863,'2006-05-07 16:03:11',NULL),(32864,'2006-05-07 16:03:11',NULL),(32865,'2006-05-07 16:03:11',NULL),(65537,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65538,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65539,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65540,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65541,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65542,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65543,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65544,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65545,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65546,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65547,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65548,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65549,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65550,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65551,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65552,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65553,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65554,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65555,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65556,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65557,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65558,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65559,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65560,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65561,'2006-05-07 16:40:01','2007-02-23 10:23:50'),(65562,'2006-05-07 16:40:01','2007-02-06 09:40:39'),(65563,'2006-05-07 16:40:01',NULL),(65564,'2006-05-07 16:40:01',NULL),(65565,'2006-05-07 16:40:01','2006-10-21 12:23:41'),(65566,'2006-05-07 16:40:01',NULL),(65567,'2006-05-07 16:40:01',NULL),(65568,'2006-05-07 16:40:01',NULL),(65569,'2006-05-07 16:40:01',NULL),(65570,'2006-05-07 16:40:01',NULL),(65571,'2006-05-07 16:40:01',NULL),(65572,'2006-05-07 16:40:01',NULL),(65573,'2006-05-07 16:40:01',NULL),(65574,'2006-05-07 16:40:01',NULL),(65575,'2006-05-07 16:40:01',NULL),(65576,'2006-05-07 16:40:01',NULL),(65577,'2006-05-07 16:40:01',NULL),(65578,'2006-05-07 16:40:01',NULL),(65579,'2006-05-07 16:40:01',NULL),(65580,'2006-05-07 16:40:01',NULL),(65581,'2006-05-07 16:40:01',NULL),(65582,'2006-05-07 16:40:01',NULL),(65583,'2006-05-07 16:40:01',NULL),(65584,'2006-05-07 16:40:01',NULL),(65585,'2006-05-07 16:40:01',NULL),(65586,'2006-05-07 16:40:01',NULL),(65587,'2006-05-07 16:40:01',NULL),(65588,'2006-05-07 16:40:01',NULL),(65589,'2006-05-07 16:40:01',NULL),(65590,'2006-05-07 16:40:01',NULL),(65591,'2006-05-07 16:40:01',NULL),(65592,'2006-05-07 16:40:01',NULL),(65593,'2006-05-07 16:40:01',NULL),(65594,'2006-05-07 16:40:01',NULL),(65595,'2006-05-07 16:40:01',NULL),(65596,'2006-05-07 16:40:01',NULL),(65597,'2006-05-07 16:40:01',NULL),(65598,'2006-05-07 16:40:01',NULL),(65599,'2006-05-07 16:40:01',NULL),(65600,'2006-05-07 16:40:01',NULL),(65601,'2006-05-07 16:40:01',NULL),(65602,'2006-05-07 16:40:01',NULL),(65603,'2006-05-07 16:40:01',NULL),(65604,'2006-05-07 16:40:01',NULL),(65605,'2006-05-07 16:40:01',NULL),(65606,'2006-05-07 16:40:01',NULL),(65607,'2006-05-07 16:40:01',NULL),(65608,'2006-05-07 16:40:01',NULL),(65609,'2006-05-07 16:40:01',NULL),(65610,'2006-05-07 16:40:01',NULL),(65611,'2006-05-07 16:40:01',NULL),(65612,'2006-05-07 16:40:01',NULL),(65613,'2006-05-07 16:40:01',NULL),(65614,'2006-05-07 16:40:01',NULL),(65615,'2006-05-07 16:40:01',NULL),(65616,'2006-05-07 16:40:01',NULL),(65617,'2006-05-07 16:40:01',NULL),(65618,'2006-05-07 16:40:01',NULL),(65619,'2006-05-07 16:40:01',NULL),(65620,'2006-05-07 16:40:01',NULL),(65621,'2006-05-07 16:40:01',NULL),(65622,'2006-05-07 16:40:01',NULL),(65623,'2006-05-07 16:40:01',NULL),(65624,'2006-05-07 16:40:01',NULL),(65625,'2006-05-07 16:40:01',NULL),(65626,'2006-05-07 16:40:01',NULL),(65627,'2006-05-07 16:40:01',NULL),(65628,'2006-05-07 16:40:01',NULL),(65629,'2006-05-07 16:40:01',NULL),(65630,'2006-05-07 16:40:01',NULL),(65631,'2006-05-07 16:40:01',NULL),(65632,'2006-05-07 16:40:01',NULL),(65633,'2006-05-07 16:40:01',NULL),(65634,'2006-05-07 16:40:01',NULL),(65635,'2006-05-07 16:40:01',NULL),(65636,'2006-05-07 16:40:01',NULL),(65637,'2006-05-07 16:40:01',NULL),(65638,'2006-05-07 16:40:01',NULL),(65639,'2006-05-07 16:40:01',NULL),(65640,'2006-05-07 16:40:01',NULL),(65641,'2006-05-07 16:40:01',NULL),(65642,'2006-05-07 16:40:01',NULL),(65643,'2006-05-07 16:40:01',NULL),(65644,'2006-05-07 16:40:01',NULL),(65645,'2006-05-07 16:40:01',NULL),(65646,'2006-05-07 16:40:01',NULL),(65647,'2006-05-07 16:40:01',NULL),(65648,'2006-05-07 16:40:01',NULL),(65649,'2006-05-07 16:40:01',NULL),(65650,'2006-05-07 16:40:01',NULL),(65651,'2006-05-07 16:40:01',NULL),(65652,'2006-05-07 16:40:01',NULL),(65653,'2006-05-07 16:40:01',NULL),(65654,'2006-05-07 16:40:01',NULL),(65655,'2006-05-07 16:40:01',NULL),(65656,'2006-05-07 16:40:01',NULL),(65657,'2006-05-07 16:40:01',NULL),(65658,'2006-05-07 16:40:01',NULL),(65659,'2006-05-07 16:40:01',NULL),(65660,'2006-05-07 16:40:01',NULL),(65661,'2006-05-07 16:40:01',NULL),(65662,'2006-05-07 16:40:01',NULL),(65663,'2006-05-07 16:40:01',NULL),(65664,'2006-05-07 16:40:01',NULL),(65665,'2006-05-07 16:40:01',NULL),(65666,'2006-05-07 16:40:01',NULL),(65667,'2006-05-07 16:40:01',NULL),(65668,'2006-05-07 16:40:01',NULL),(65669,'2006-05-07 16:40:01',NULL),(65670,'2006-05-07 16:40:01',NULL),(65671,'2006-05-07 16:40:01',NULL),(65672,'2006-05-07 16:40:01',NULL),(65673,'2006-05-07 16:40:01',NULL),(65674,'2006-05-07 16:40:01',NULL),(65675,'2006-05-07 16:40:01',NULL),(65676,'2006-05-07 16:40:01',NULL),(65677,'2006-05-07 16:40:01',NULL),(65678,'2006-05-07 16:40:01',NULL),(65679,'2006-05-07 16:40:01',NULL),(65680,'2006-05-07 16:40:01',NULL),(65681,'2006-05-07 16:40:01',NULL),(65682,'2006-05-07 16:40:01',NULL),(65683,'2006-05-07 16:40:01',NULL),(65684,'2006-05-07 16:40:01',NULL),(65685,'2006-05-07 16:40:01',NULL),(65686,'2006-05-07 16:40:01',NULL),(65687,'2006-05-07 16:40:01',NULL),(65688,'2006-05-07 16:40:01',NULL),(65689,'2006-05-07 16:40:01',NULL),(65690,'2006-05-07 16:40:01',NULL),(65691,'2006-05-07 16:40:01',NULL),(65692,'2006-05-07 16:40:01',NULL),(65693,'2006-05-07 16:40:01',NULL),(65694,'2006-05-07 16:40:01',NULL),(65695,'2006-05-07 16:40:01',NULL),(65696,'2006-05-07 16:40:01',NULL),(65697,'2006-05-07 16:40:01',NULL),(65698,'2006-05-07 16:40:01',NULL),(65699,'2006-05-07 16:40:01',NULL),(65700,'2006-05-07 16:40:01',NULL),(65701,'2006-05-07 16:40:01',NULL),(65702,'2006-05-07 16:41:57',NULL),(65703,'2006-05-07 16:41:57',NULL),(65704,'2006-05-07 16:41:57',NULL),(65705,'2006-05-07 16:41:57',NULL),(65706,'2006-05-07 16:41:57',NULL),(65707,'2006-05-07 17:21:29','2006-05-07 17:22:03'),(65708,'2006-05-07 17:21:29',NULL),(65709,'2006-05-07 17:21:29',NULL),(65710,'2006-05-07 17:21:48','2007-02-06 09:27:22'),(65711,'2006-05-07 17:21:48',NULL),(65712,'2006-05-07 17:21:48',NULL),(65713,'2006-05-07 17:21:48',NULL),(98304,'2006-05-07 17:38:48','2007-02-23 10:24:50'),(98305,'2006-05-07 17:38:48','2007-02-23 10:24:50'),(98306,'2006-05-07 17:38:48',NULL),(98308,'2006-05-07 17:38:48',NULL),(98309,'2006-05-07 17:38:48',NULL),(98332,'2006-05-07 17:44:00',NULL),(98333,'2006-05-07 17:44:00',NULL),(98334,'2006-05-07 17:44:00',NULL),(98335,'2006-05-07 17:44:00',NULL),(98336,'2006-05-07 17:44:00',NULL),(98337,'2006-05-07 17:44:00',NULL),(98338,'2006-05-07 17:44:00',NULL),(98339,'2006-05-07 17:44:00',NULL),(98363,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98364,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98365,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98366,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98367,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98368,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98369,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98370,'2006-05-07 17:48:40','2007-02-12 16:07:13'),(98371,'2006-05-07 17:48:40','2006-05-07 17:55:23'),(98372,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98373,'2006-05-07 17:48:40','2006-05-07 17:59:48'),(98374,'2006-05-07 17:48:40','2006-05-07 17:57:01'),(98375,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98376,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98377,'2006-05-07 17:48:40',NULL),(98378,'2006-05-07 17:48:40','2006-05-07 17:59:48'),(98379,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98380,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98381,'2006-05-07 17:48:40',NULL),(98382,'2006-05-07 17:48:40','2006-05-07 17:57:01'),(98383,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98384,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98385,'2006-05-07 17:48:40',NULL),(98386,'2006-05-07 17:48:40','2006-10-21 12:23:42'),(98387,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98388,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98389,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98390,'2006-05-07 17:48:40',NULL),(98391,'2006-05-07 17:48:40',NULL),(98392,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98393,'2006-05-07 17:48:40',NULL),(98394,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98395,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98396,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98397,'2006-05-07 17:48:40',NULL),(98398,'2006-05-07 17:48:40',NULL),(98399,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98400,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98401,'2006-05-07 17:48:40','2006-05-07 17:49:25'),(98402,'2006-05-07 17:48:40',NULL),(98403,'2006-05-07 17:48:40',NULL),(98404,'2006-05-07 17:48:40',NULL),(131072,'2006-05-07 17:55:23',NULL),(131073,'2006-05-07 17:55:24',NULL),(131074,'2006-05-07 17:55:24',NULL),(131075,'2006-05-07 17:57:01',NULL),(131076,'2006-05-07 17:57:01',NULL),(131077,'2006-05-07 17:57:01',NULL),(131080,'2006-05-07 17:59:10',NULL),(131081,'2006-05-07 17:59:10',NULL),(131082,'2006-05-07 17:59:48',NULL),(131083,'2006-05-07 17:59:48',NULL),(131084,'2006-05-07 17:59:48',NULL),(196608,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196609,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196611,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196612,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196613,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196614,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196615,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196616,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196617,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196618,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196619,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196620,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196621,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196622,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196623,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196624,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196625,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196626,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196627,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196628,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196629,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196630,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196631,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196632,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196633,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196634,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196635,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196636,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196637,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196638,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196639,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196640,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196641,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196642,'2006-10-21 12:23:41','2007-02-06 09:32:46'),(196643,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196646,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196647,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196648,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196649,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196650,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196651,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(196652,'2006-10-21 12:23:42','2007-02-06 09:44:23'),(229376,'2007-02-06 09:40:39',NULL),(229377,'2007-02-06 09:40:39',NULL),(262144,'2007-02-06 09:53:33',NULL),(262145,'2007-02-06 09:53:33',NULL),(262147,'2007-02-06 09:53:38','2007-02-12 14:00:14'),(262148,'2007-02-06 09:53:38','2007-02-12 14:00:14'),(262149,'2007-02-06 09:59:48',NULL),(262150,'2007-02-06 10:00:03','2007-02-12 14:31:32'),(262151,'2007-02-06 10:00:22','2007-02-12 14:25:20'),(262152,'2007-02-06 10:00:39','2007-02-12 14:26:45'),(294912,'2007-02-06 10:22:42','2007-02-12 14:35:36'),(294913,'2007-02-06 10:23:23','2007-02-12 14:36:21'),(294914,'2007-02-06 10:23:33','2007-02-12 14:37:49'),(294915,'2007-02-06 10:23:43','2007-02-12 14:40:14'),(327714,'2007-02-12 15:25:50','2007-02-12 15:25:50'),(327715,'2007-02-12 15:25:50',NULL),(327716,'2007-02-12 15:25:50',NULL),(327717,'2007-02-12 15:25:50',NULL),(327718,'2007-02-12 15:25:50',NULL),(327719,'2007-02-12 15:25:50',NULL),(327720,'2007-02-12 15:25:50',NULL),(327721,'2007-02-12 15:25:50',NULL),(327897,'2007-02-12 15:37:42',NULL),(327898,'2007-02-12 15:37:44','2007-02-12 16:07:29');
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
INSERT INTO `op_permission` VALUES (6,5,3,2,'\0'),(7,5,1,64,'\0'),(9,8,3,2,'\0'),(10,8,1,64,'\0'),(45,44,1,64,'\0'),(46,44,3,2,'\0'),(48,47,1,64,'\0'),(49,47,3,2,'\0'),(51,50,1,64,'\0'),(52,50,3,2,'\0'),(54,53,1,64,'\0'),(55,53,3,2,'\0'),(57,56,1,64,'\0'),(58,56,3,2,'\0'),(63,62,1,64,'\0'),(64,62,3,2,'\0'),(66,65,1,64,'\0'),(67,65,3,2,'\0'),(69,68,1,64,'\0'),(70,68,3,2,'\0'),(72,71,1,64,'\0'),(73,71,3,2,'\0'),(81,80,1,64,'\0'),(82,80,3,2,'\0'),(84,83,1,64,'\0'),(85,83,3,2,'\0'),(87,86,1,64,'\0'),(88,86,3,2,'\0'),(90,89,1,64,'\0'),(91,89,3,2,'\0'),(93,92,1,64,'\0'),(94,92,3,2,'\0'),(32769,32768,1,64,'\0'),(32770,32768,3,2,'\0'),(32771,32768,40,16,'\0'),(32773,32772,1,64,'\0'),(32774,32772,16,16,'\0'),(32775,32772,12,16,'\0'),(32776,32772,3,2,'\0'),(32778,32777,1,64,'\0'),(32779,32777,40,16,'\0'),(32780,32777,3,2,'\0'),(32783,32781,1,64,'\0'),(32784,32781,40,16,'\0'),(32785,32781,3,2,'\0'),(32788,32786,1,64,'\0'),(32790,32786,3,2,'\0'),(32793,32791,1,64,'\0'),(32794,32791,40,16,'\0'),(32795,32791,3,2,'\0'),(32798,32796,1,64,'\0'),(32799,32796,40,16,'\0'),(32800,32796,3,2,'\0'),(32803,32801,1,64,'\0'),(32804,32801,40,16,'\0'),(32805,32801,3,2,'\0'),(32851,32786,36,4,''),(32853,32786,12,4,''),(32855,32786,40,4,''),(32857,32786,20,4,''),(32859,32786,32,4,''),(32861,32786,28,4,''),(65702,32786,12,16,'\0'),(65708,65707,1,64,'\0'),(65709,65707,3,2,'\0'),(98306,98304,1,64,'\0'),(98308,98304,16,16,'\0'),(98309,98304,3,2,'\0'),(98333,98304,16,4,''),(98335,98304,40,4,''),(98337,98304,24,4,''),(98339,98304,65710,4,''),(327719,327718,3,2,'\0'),(327720,327718,1,64,'\0'),(327721,327718,1,16,'\0');
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
INSERT INTO `op_projectnode` VALUES (8,'{$RootProjectPortfolioName}',1,'{$RootProjectPortfolioDescription}',NULL,NULL,0,NULL,NULL,NULL),(32768,'Development',1,'',NULL,NULL,0,8,NULL,NULL),(32772,'Consulting',1,'',NULL,NULL,0,8,NULL,NULL),(32777,'Organization',1,'',NULL,NULL,0,8,NULL,NULL),(32781,'SuperWiz',3,'','2006-06-05','2006-11-24',0,32768,NULL,294913),(32786,'Virtual Cockpit v2',3,'','2007-01-01','2007-08-31',0,32768,NULL,294914),(32791,'ALS v5',3,'Automatic Landing System','2006-07-03','2006-12-29',0,32768,NULL,294912),(32796,'Relocation (new HQ)',3,'','2006-08-14','2006-08-25',0,32777,NULL,294912),(32801,'Marketing Plan',3,'','2006-01-02','2006-12-29',0,32777,NULL,294912),(98304,'Implementation ALS/Xplore',3,'','2007-02-05','2007-03-31',0,32772,NULL,294912);
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
INSERT INTO `op_report` VALUES (327718,327717);
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
INSERT INTO `op_resourcepool` VALUES (5,'{$RootResourcePoolName}','{$RootResourcePoolDescription}',0,NULL),(44,'Engineer','',100,5),(47,'Consultants','',115,5),(50,'QA & Documentation','',70,5),(53,'Project Manager','',130,5);
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
INSERT INTO `op_subject` VALUES (1,'Administrator','{$AdministratorDisplayName}','{$AdministratorDescription}'),(3,'Everyone','{$EveryoneDisplayName}','{$EveryoneDescription}'),(12,'cs','Claudia Schulz',''),(16,'tw','Thomas Winter',''),(20,'hs','Hiromi Sato',''),(24,'fn','Fredrik Nieminen',''),(28,'ms','Mihir Singh',''),(32,'jw','Jody Wang',''),(36,'sh','Sabine Hausberg',''),(40,'dmk','Duncan MacKay',''),(65710,'jm','Josef Muster','');
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

