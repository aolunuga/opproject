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
INSERT INTO `op_content` VALUES (327714,1,'application/pdf',73392,'%PDF-1.4\n%âãÏÓ\n3 0 obj <</Filter/FlateDecode/Type/XObject/Length 14832/BitsPerComponent 8/Height 229/ColorSpace/DeviceGray/Subtype/Image/Width 1318>>stream\nxœí]m¡ë „I¨„I@Â$TÂ$à`*a0	•0	÷İ­k!¡\'®İ»œ?ï¾µ	!À)ŸápXÆX{s®ı™Ã9gmmªÕÓ/(((Ø)æÒ¸ÇÏî7{®¶¶µ   à³0—›ßyŒâáìù¸µÑÁÉ:	ANz•×Â”ÿ9u³<ÆÁ]ª­óPPPP°õ-‰!{´×ÓÖ9)(((ÈLÙeéQü_0‰í®Ş:O™p¬ÛÌùÂãZm³‚‚‚‚tT×ÜİÈ7nfëÜ¤áÔ¬F‘/Üë­sXPPP ‡qëräm½u.\n\n\nt¨>À‘_Î“Æ˜Ú>á&xıPÿ>ÚÚº‚‚œ.»ÿ<ÇİÊé8ªæ3ùÄİl[æl¯Î{<9³ĞeÁ.qòw®ÜëB”í\ZlÈÃU[çá#@ël]öÒì	ätÚÃ„Q¯·®Íáº÷â1Vá#ÄıZW[ç¢ à	v:íqŞÚ´/Á‰óàªxÔ[ç›‡ÑFøsy+ç\n6Ç9Ò\rjöŞ_‘£ÊNıŸl¿±ÏawuÉx6ó…¶)ßë‚-QGëçıÿ¢Éêrÿq™uš63\'ğ¸dÎL2^%§·2Q^°Îµ37§lˆÓµÍŸ£ãuR€±«îä±^‡\";<J²`T‹«vkóàÜQäOf–<­IöÓöIäGk«­sYğ÷Ğ,×ÌjkÓq¶ßœ,yY¸íb(zvÊ­Ù:§P-›­LÃ±¾Í»8ùXò˜{•B‰vû…ëDAb²[oÛ‚?duö±µ‘	¨ˆP¸ÙXòÔf\'\0-6u’#Ÿ(<YğA8¤Nš­­T‚YnÍÅ’ñÍÆ–[¶>âÃGáÉ‚¡EjäW®,,·®’‰%›l->6Û²Um4íğeGÙ¾P}´[[)Æi\\Ğ&…%.KSÏˆÇ6““öóG3ÜªMr\\ğ×Ğ\"µÑlm¥ç…\r)9X²Ú|Pˆ-,n»êa?Ÿã‚¿‡TÆíWPa—(ò\'K¶ë@ÅP§çL†Íf¸Qİ,øV ûı¾iÛ\0ùIgI³O’üô¦­]t¨íG³\\ğì—´[)ÀGXrW‹Ûs|’&cQR>ˆ{õÁ<üI,ŸB~ìâdˆO°äIò“4¹ñùõ7J|¿‚•ql—*án	#ø\0Kîš$?F“»Zã·ŸÉsÁŸÅÒBÄwO\\Ÿ%wN’*°íƒ|Ìğ†A-Øâ4ù]$¹>Kî$?RdIkü­k€W€=ÿ¼Ø«K!Şÿ,jÁîëØ­bm–ü’ü\0Mª×mÜ5zå×él57‰=Qh²`]°d[³µiR¬Ì’ˆú`ešÔ}*îÖ@ÚOºû ÊÎÉ‚•A^%ıÖeÉn&±êŠ›†$…×1\0g´¥7Y°2üø9Ælm’«²är\\÷İ ÎèS\'±1íEN`ÇÚI“)ƒî‚õq4öê¸}ï…ñk²äq_ëºq¬V€âş´3Ú”\ZaJ…&\n\0¬É’NØh7ÅZ‚B’TsääDP·\\¹,(ø±\"K\n[ìÖX§c%ìO\'Ç€¬œ(=›!‹ÿ9ÖcÉ¯Ø4Å*«FbA–Ûe»ÊaÅ‚‚%¬Æ’ò5‹Ía3ûö üTdº)üØÒüª ›`-–\\>î¾C˜ÜŞ­ÜäÛ$éNf½l½ àÄZ,¹“+eeÈŞ±LJf]=’œ\Zÿªà,`%–D‚ï™;V/d^;,\Z­=æ~:7gkÏfrıx%g.İ	xcvµUïeQİ›VmbÂÑS›\rÍX\r§W¦†Ü%ÖñuXò\'%;Ø4oÎ!ØTŸ}ıˆwæW:ùË7êùã¹½Ø¬“æáÙ:›˜éœ:wı%ëÕ’^FelãZÊ0{ImÉ~İó[,„	]Ñlí tTgÚÅI>^…%¿j;ù9»\rœê\Z»ğ20ºN÷¤ª›ÅÔïMÍÊ+qª¯Éoëìy5Jª¸\Zd“´·ËŠ=Ş£±7°N<\\ê1 Ò©®ØmÇórùc>öMm\0§µÒ|î&*·ù\n¹ü#{¢oàŸ*Í<ÃójbC&{nZ4á¶É¶©ªo²íûm#;)Yq¹“Ã‘#lİc\r³Ÿ-¸PFKm…’€FûÓÁtu=]áúş¸-} ¥¾ÒäóKÑ¸*Ê<æ},”™ø©#Ô\\u··bµxÕî–(-d†»ÍÙwë#;Xÿ÷£Ô)·:£U¿ÉËrÀ2Ÿ0@”yN^$Xò$ör†J;JÏ¯ÜôF®†ƒw%i7¦şZ‰:“ï0/¾İòºúÂãZ¥äÒ;#C›–8a…?P9åÑä±JÎĞ!î…-ˆb£Ì‰_[¶Ue,ò}Véã@çs­;§Î½û·¤Hİ1ÜE…Ç£A´™Ï|TãŒ’æRò˜Doè¯›ô-g®Ö&>`ÆEvfÚ)Içù	³RàÄ³\0ˆVAg9™³¤8ÄËl¨K½Jd>×XßvöLtñ*comşÄ,^zÑär±2Ô;ØáL¿×f&Ïj4)NÕ…¯ó|*[›0ès‘}?:·)V%éWöìiH;·ˆN8{çf&7eIıW¨Ã“iJ=ùtY“øuJ|QªªsÖ†\'òLÂ{%I\'f<æ®U“·å©ùÄUÌT9RíñPò$á;<KmÁIıÉsrâ>nc…˜¾0¾ô›%O2INW;•Ï¼A.°İšHİ±D¡â[@ûÉÄX8Ğ\nWCİLÿ4^heİIÓfIu€†\'I¯ô®¦gq«ÄF½ks$îC@Úˆ:@Ù°–Ìãa2ÄLÅˆ|JÃ)ÆpL‡œs‰Êo	ğ¼C•Vhg²è`§·L÷8ß¾¯\ZÏ˜0B‚V¶ÚÎ}šm÷4S;Ğ„‰JèÇ\0ó$¢lI×éJû°gÉK6¦	UäÒü‘Ï|K7Ò2*›Ím1Î\0RÇÚ×7:Ğ¶çM\nMaÏ³véj4[ùÊz\n<(SdôbÙº9B£:ä†ò\0/rGT™h.ºÍf^,™õ+éüLeÔMåßş²\0ÍVˆc¶¶SË÷€.+¬}¼øÚAoOe˜¬Éj(S’`\"< ôã^±¹íÃŒê‘»@Bk Î-¢É°ÒŠüéX²Îû•ôudUæ³É£X»],O&G€@¿k.pwhÕ†,Îöšèª²”o¬â¶TòfÉ+ö`rÛ‡82OÖ’@®(FôZ´ZÚşè²vÕ;xÎª;Èg®¤nWH‡LßR«·àt\r«NLg¨?*_pyJÓäš|›b±ìó7‘–8`Q]!H?z„õCGƒ—»“ˆC‹º%9W­0”°bóaùÌ±„\"\Z`È2/“Ú™lÀtÖƒ.#Õ¾à2K®Cté–ğ5šÈø\"Î¢ü*öAëÿ«»fD0‘\'öÒ%ÛU†Fj¾Nó!ÏîÅ!Ï2rŒÈlš	-–Ê\'…ƒ¦£º­NãÇÒ‹ïíU °~ò$€ ÍÙú	æ :-êV6ÁìëœU³ŸÏôÎÅÂ‡D†jZg=xó‰8á\rfJ01¹YÌ’XüIÆiòéSX¤ÉßÆóÑ7²ä¬_”U±—ÏôYÉ{µPP¤OĞØÅ4\"@æ´ÑUnm²#ÒÁşIF)à3X É_Y\Zíó#\n-êV¶›Ã´_”U±—Ï&U_¾¨eé»z“:“à®ÑG¶ìF€~»Œ\'§gÉöñz¸QA~Ô¾â²\"$ÉûÍ^z3jÛ8‰èZábîî…V%«‘Üë[yGäı\n×Ã©\rrÍp;Å³¼ÅãI›«=Ìó™Ü•¬#nãÔnhMƒ%ñ™ûAj©=1K¶×0x%ŒMÃ},7düÜ¯&7ÔÊvÜD9™ÀÍ¯J8š‹8ìc¤ºhHrc«QEØŠĞ$\"ĞNaÉ3xqXbgæğƒÉY	,æùLåf½Dğ~­DÊ‡%asåu\rc,Ù^¹2mde<(±> Å.µãÙ˜ØÃÄd—†Æô‹h„”$·‹	Œ9‹có4‰HtpB#x÷„áüŞ•×y@óğ…ÌÚDâ	îÜ$)º ‹DÂBR‹¥PgËkà×Ë»!fIu˜„\'éªĞ€Òq+ÀH\\8#¢~©DZ­DVTü·L‘’¥É„Ì8‘‹aï‡Ú-«$ßí`i3#Â€0š$“ÆÃ`\n&[Nc\0\'Iıì\nYr9\Z\"~P‡\\úGë×blŸ#æ†ÛĞ<XšóİÃ•ÌD,†G8^Z6n“\"ËâÉ#&ñS|ƒÏÇ’IÜ¥½ÄJ4)6éÃõ Ô	ˆ\0Ò?Î±$tºîÜSı8\nãs_3Ëw`z´0G¹ÁRò‚–ı•Ä]Kø–0®qxâ`ğ\r¸ÿÆjÈÆ’I!Êó·{4)V©—ÜQ‚É™ÓìÖHXİÁå@u„(Æ°à™¨A\'±ô@#ãç ªP_€Ãáö0!«QC§íàìÀ»«QšdY(K6xæÂÜ®·o0¥‡«Ş¨ó×X§…_§Í¢´‘µËÀ›ÏF„9„ËIrbŸÓ§É‘d¼ãĞ”³t\r¦±(K&­İ¬yW{\nMj×o”cÜµ\0æV—‰QŒLPiX!ÚÌ† \r‡¬÷X€ÿàzZù‚-(¸xözøFr®4tÂMXp‹=è$ËÉçbÉ”µ›\ZÎ­¢©mÚÛ@.øÌvI¸a{z{j±>ŠñÅ ê%ó§S˜q°¤ ?Ò¤ÿ%X-Ü3º…Ÿs#‚†N×\r@1z²œx.–LØu“ñÄ6‡”kJÊUåµ\0fÖÌ¥P¿É)aç%ƒN|›İdôC1dÿb	¥Éj&…å¨…ÖğY\0­—¬s$èA”Ï6ÈL,	ß«\ZBˆödÇ~qµS¡RğØ!DÛ6ÅŸbÉZfT]­\'q«Ğ¤Q…Rˆ%O¡5pçmî‡Y#õÍè0Œø bŒ‡À	Çy\r¢sm–Ô¸Ñ{]OÖù’·ø´#|AÊ!÷_bIi´uhĞaBòYŞVlÇv&!¹ìs>Ë\'XHÊÛ•@Ş&Z	\"fèD–¥J–•´)›L,©pCgö\"ŒÖVËÒ	+Kº!÷_bÉZhä;—†•RC–C0/bŠvÿ/6Äv\r°v§¾7ìüT \"eè4”¤”÷¡Šì‡6KêÜHo-~¸M\r¢Ìñ‡XR¼[\n*;—A:YŠN@­\r+ˆùÚzƒ¹|âlV®ÀXXwU«¡“tP’âí\'VN8KªÜ@İ^>	°|K°º««rƒ¬ü‘Àiø~Ok²bAÖxÍéciJj¹Z„+‹ˆùê=^PgrÒ3Ä¸­Òš:+2\"eè$”\'qN ×rÂyX²rF ^ÒŒİbs«âZÔcn‘ı¡]åòhëˆÖyƒF*š°ïË}Ôî[Ô1è±ŞÆ»gUë¤Ó¿Ø°¨öÅ!C§èYù0RË	çaI-	-jF#,#V¯s›Å¼øC,iÄæ ZçõêšË\r²XiÌW_ú†}ÍÇ¬bc–¤kC0‹‚!·(s8DÖŠ3Ò j9á,,©>Ã½°F-¹¿fáo«´Pµ™å¥O\\è œeBnR¦ó\Z+UÄ´¬6 Äíié\0lÀH¦A’†\\ˆ¡tˆ¬üTÔ\n8á,,©í©-,TÉneˆŸêÔî-W-¢‹YF£\\Œ5#§)ÜãÄ¶ º~¹bc9b‹¾÷\r¹Gn‘·•»€`= ¿í!2†NĞiÒ[Æö,	eŒ@Õ*]ŠĞÚ¸Rå«Ñ-˜sK*–ésæj!óWrM01‰ØbTÆ<-ZŸ&ìcœz+i‹$âC§çYy6 \'œ…%Ä¿rŠuó:¢NÛ™TU²Óø™ÏkŒõÄ ‡)&$ vVã°ŞO?\n`°â›Ø\"wÊhøÔ¿‹MK¦FnmDüšŒÈ:=ˆ*U¡êœ\"¼Ä’ÚiÉ*¦Tµ¹¨(t:#íBæSÒRïö•@{78T­¬Üd2mVãRâ§Ãxæ\"«ß˜Ô4ˆL®cP\'Ì§DÆè= ÂlÎ’ÊiÉhOJ·3b@¹§Sµõ\rÔ²N„ö9Ğ-õÆ“ƒªU-·iÛN*°ü\")§\ZÄ&#x7y¼¢ZÅäÂ‡D¿‘%•{¶/áy\"£ÉV§r!ó$PFN3B el¨ŒÜ/cIë™ˆ¤”YÓ½ë4öË¤òa–TLôlÎ’-b@€ØP@2ÕOÙÚ4ÛuĞ9ˆOLL‚»©‚aânX²Á2°ü]=€ˆ•;EäK¦ˆ‘TüŠ €äÊÊs±5K*q×•	ÆyâÁ½Šìdêş@´r4×Á¿İ°¤s°3nVî”’y¨•˜côÉ$Xæ\0Q+ÏÅÖ,©Gæä’ÆXüÇS73 ÚWî@åF£\\tÒ8Øãk%7èËXÒò\0\"Vî”’53È~“`L§H&Á2$9+ÏÅÖ,©[¼‰œOPvúzğÇVuë7ª¹x”ç×r· %ÁÄBRµŠàËXR1ãf^ aIÙqF5œ\"™Ëä¬<[³$~Óå5¯Ğ©`]¨›\ZP¼@\'&ñ¾d@;úáØ¿°dÏ|@Â*¼2@Â’y×$£O&Á2$9ÅdëÖ,‰d+OÉûã*N³nÈ­\"2tùÉj”Ğ€v„ÚÂ’<ó‰”‚d^Ò!ïšcôÉ$X†$§ÈÔÖ,©Zèl5ú¦`k©nnÀ,–\0T¾rg¼¿Î…%;xæ)û%¡Y!#pŒI0¦C«H&Á2$WŠLmÍ’Hòø€\0ZTŒjİ„g½\\!àğèV£FZAL+–ìà™H¤°ä·îªä2†NÉ#\ZÃÆ,©c5~¢Sé›]–n5Ú,P!ĞöªIøì(á±İ°dƒfbxæ+Dr{g 	è]›`ÌØl¾\'$È…§áY’_I[à^ĞŞh´é¦™à5­5—¹jñÑÚ\rKn{öFÓş>|ĞÔ¹ä÷gN(ú‹ˆŒ¡ÓszÑäl¤ÇYRU‰y•ºsÜ0C51©@áloTúÀÙ¥²XX²ƒg>\"bnéá&A%”írÿ‡£]0¢1|#KZV]«Qçƒû€ªú½Êz/¨¯6æÆÏyÖ„ônXr­C@: ÆØuÕû§Ö.ÕêˆŒ¡ÓszÑ6fÉIŞ;§¬¾bvÈ­Ò•‚Ê³Rfûc€Ïy’%wÃ’P•HºÜEÄ}ÿ\rÊëèè+X\'fJÄoÏˆŒ¡ÓszÑ6fI$WØÅõ5^spnT‡J!Å1ëLM6pú5%¾–\\»û&bŒ¾ÿU;¼í·õ—ğ¼ õ‹È:A$SŒhßÈ’¬6õÍÙsØœÆV@!(]Ûc>BÇÜØK:@\"‘p ^Ñ÷ß ê?vÜ°\"şÀí`Aç2t‚N/\ZÃ²$§Uh˜¬šD5H)¤y¦V&ÁCphN|?,‰,è} ºRÄ+ê!7¶ë¦\Z^Ç¦§lJv±¹Ï 	DÈĞ):½h_È’¬Fe¶\0\\£ù(KJ65åîM\nH’)Œı°$4è«¦h€Ø¢®2P©Mêv‹¼Ÿ´:Ø@Ù­|1DÈĞ):½h_È’ìø(ÃÁ›9õ¨H89ïÜd#H™™#ŞKBŸÎO-ß ¶¨ä¢{r Œ±úÜbú°ëŒH½Ñ¾%-§LĞŠƒñãgYòØ\nRqù6 ]Ë(ÙKB³u¹ií\0ï’0\ZİXíœ¬\'ƒ;B4áö;8HÈ!RFŸ$#\ZÃÿÄ’Ùv3~T±¤¾›\"J.~Ÿ¸$ÕV*Ë.;bIIˆµ˜ò£$m‡hírk×3æ	lÇ	±ï3z\'0¢1–$ÀlÈTÍ{²Ö.Cv’èš¥;)ó!ÛÏØK‘={âAîÑY{6•P\nñÊVjºéf65½”Ó9`W•È*\"fèD^4†ÕY2‡É•Ë)ËÆ’\\\n9u!mĞlÓ#¸ˆ:’É±™*X›“úndç®¶6à°±Deö-ğ>k ŒîZ’¦œº\0‘3tªN/\ZÃê,oH®|XNÙÇ’ÒÛ ~é•œp¿i¤“±\'–„:5Âµ\\‚`ºD,éÌ‘–¤ƒÔz£g´ÁX¡1˜$ÉdˆœÑ»¡°¤ …œº ˆ\\6•:­ª¦¹—wW,‰U2Ù˜;fÇİ5öb%†ÒAH“\r¦µKÁ„âA·I’ô:\"hèt^4šÄ¤U„á\\ù°œ²ÿ%ËöQ%$æÈÈEj‡±$¶&aìë¥ÛãÒãQìi0ÁF`¸zİ+5‚–H~AC\'ìô¢1¬Ï’Ñ¡’+–S¶ö\Z÷,©Ùİt¯Åë8µ§ïììŠ%Á“«8M‚}0Bbàé@xïVˆ¢’?Á^\nÖJ+E$\r4’2#\ZÃú,5\nÉ•Ë)[›%U!‡XkA4šD›³€(Ïâò¡…á¾X<ÇtvÄVÕˆ©NÌwlÔ\r/¹Ã[ÁÌ·3 w,¬“9‚ˆ2Æ8½hë³dKÉ•Ë)ƒ¯‹YBÊÉÏ.·ÒÆªú¦ºŸmiÚl_,‰~?[ƒØ\0’$Uò˜àÀî®#¾_Œ*3I-hÒ»na}ÜÄ6\"khQ§!©:C£ÓâH®|°#‘ÿë„âuùáì¹bÕVÆŞZ­æ%ŞKÂ{ª\0Zª@]TG	´â‡$è¸1í¦‘˜ã¾ù•À¾!Â†uzÑÒª3\"ÌÍ÷Ñ\\ùàiW¡ŒwÚ@uÒ %Á#íäåİ]ŸK®æµúøü£¶Ö¹•íb7uo,‰nLY¤¥ÃÔDn-˜éixgWW)1=7´g<˜se‰²’Ínó_ZDÚĞ¢ˆŒh`ÉWybÕùzï$H®u¬9ª0¹!¸i|U§ÎÈÊƒÄY5*^\r±-@=öÆ’‚/ÍÃV‘’€«˜¡Äa#fh¯”²ÓETİYRB÷–¿álpèÈÔ´ññİRDštïNYõÌ­®:Ê\\®®+‹æ\n7§ÑhÁ-ç©Ô+Ê$Ä©Í“³,h€Eİ±¤¨ğÆê:Guia–4B`ƒw}oÁ<ıœ”ÛjÖ§p?{†»»Ù®NlKÜÿ%£<\\;ûÿMª`ÛJ3â¾º*cåEBA¨g]@[BöÇ’Â@öms1Ózfê«¤³ÄGD&dEl[‘&“ØFZÔéEcH«Î)7»ŞÑ\\`³™\'/¹ZU™äEB#Û6§4,OI¾°C–NÀ½ğp¤rÜ¤Ü‚Lˆïı8MÆWÿ\r†uzÑÒªsRçí©@ÕöùŞL–‹o¸Ï.¸ënx°	v1êF·î%U4©;qû)|,m˜G×í3aabQahQ§!­:«ˆcj­Š%ù2Ï2äæ¾sªí˜/‚ìŒ[	xóçYòc4É“ÀgÒ°|ªè£3:KÛˆC‹:½h‰Õ¹EÄÔ%­ñ—9É×ëÚUŒ‘%ë³Ÿ|È²Í`)¢Oá$–<›4`ˆpÒ\'’$ól,PG´ZÔéEcH¬Î)İ‹&€o­M‚==jN·Óh³ò\"‰İ­·îF`ç>Y2­¾‚ˆ­“¬Ÿ:\Zs­0¶¨Á¶@—-êô¢Qß &ñâ)Cn§–ç~IS\0/ğUê¬¼H¢øLw(„(DÍ~Yrõ­§q?!\ZšÌÔ·Ÿ˜Ñ‚f Š˜\\9½h©Õ1‹A‹z$@dÑèíé`9ÍºSâŠ\"Y€4Vn,Hñ±[–¬Ñ`!Â¢\"/‘‹®[½;	t$A/ZÔéE£ALJ•¨UÍ´En™OíLòQu_Ú´èá4Ì‡yRÌ‘{fÉUg-ìBÒˆßN]¶ò•Ä;[Û7ø•#ˆ.C‹:½hÉÕ9¡DZ<Â=‰Ã†šUÜªôÉK<f®\nÜ7K®Ö_¾PÑò|ïÜæ1¨;X>Cœ±m†uzÑ’«³èÚè9êƒvi-2C`ÏO¬…©‚K®wÑ³(úJîµÊ¼]³ä¯y.Í+¢çõâ1Ã’³p*yÀ:<)ùÖ\"ú-êô¢1¤WgıÆ}{ĞÎöE†ÜIs\0ŠU«ë£2qF³@`\\ˆ‡òzˆİ³d~lk$UDSÿjj‡W5èQçŞqÖ^$Æ \Z\r-êô¢1d¨Îjš|Vg]-šÓ„1wÍkmU\n­¸@$„_ÃÉo†±{–Ì;»‹q¤ˆ%Óˆ<…#Ÿ8iâ×sp w$^2´¨Ó‹Æ£:Ÿ”Ÿı\"w|£¬úKÑªŒƒn¤å!ÅéÚjsÅıR¥˜õ,ùÏØ¦9©ÇÍ )\"Ú&¯ky2•#_P]ô¢½VÒ”µ†uzÑ²TgíN+4_bÕ@{\'6‰¨ìxd¨­‹¨.Ng‡Ç-¡Ùá+Xòƒ\nDŸ±WNÜ\"i×-æVüFs¡	¢ÙĞ¢N/\ZC¦ê¬[t}Ú«œ¦.˜)#æE¨A¹Á(6šç«ÓYÀY“ÁoaÉÃÓu­ŞYÂ·Â+GY°]E×-Š“ş|¿*·Á!Ê\r-ŠËˆÆ­:‹¢ËwxÅOReãü£¡Éh ’Fg¥ø¶÷$ÜcÓ¡½]L.[ôzW`ÉÃó.q¤í\'	H.ªì óÊÉ‚Ö9èF81~+–´=iœ3IÀĞ¢N/\Zõ\0b¨K4|ú,Ú˜uÔ9MFIr#WÀÑØF:úñ…svàu¿Îz+@­)^İå\ZåçQN[·Äãßö´æôNu¶7Œ«ÕÎÔ&¯\'½h™«ó	5ÿÛ$ëIMn!ßXÌJ£1ÆCŞiw¯­òi_ÆÑœ±@ûç®öl¶±r8šúé¹–v—s7{	î|\0©3¼ôó\"¼ÿ¼ı-Å&	NæbofKçš4çü-<?;T}5É: ò©:ê¸²ˆyñ¡±àn÷ò…àÕâ÷wî®\'q®¯ÿÖ¢ß_„×-©rèD*Í¢’cn«48åwÎßØÍUNLFN\\÷À;€K§\Z¥‰–,Ø?J³µ{„ú²\Z»¤İÇ¹tÜU¹ó}ƒiÉ‚}©4[ÛX°Kh7G@¤.Ÿ¿uJ–\r,øS@*ÍÖ6ìêƒıÀ!éÅ£û­]Ô¡¾LgµP_\n¤ÖlmcÁ.¡ĞFb–¿qŒuF\"ŞèïÒ±‰)øß€Ôš­m,Ø\'\"4ûÂ†>hÕbgôA6ÚT°[ µfkö	}80Ùé2ß•ëšºÂ$õ—×~ìxbÁ·\0©6[ÛX°Oè‡ÜñãÜ>*cL}–í¾×GÏŒ^¦Wğ\'T›­m,Ø)Z5­=ªM¹è¾¸< ÕfkvŠ„¸¹àÔ¤	‘ZË€»ÀRo¶¶±`§H¹ø0~\0;)·H¯¯+øÿÔ›­m,Ø+RîÙXñ ~åæ§l)/Ô›­m,Ø+’èh5šL²jÍ{Á\n¾HÅÙÚÆ‚½B¿sû‰•h2ayûÛñ^ğÇ€Tœ­m,Ø-šBZ‡&ÓH²¬İ„@jÎÖ6ì)ë7?«Ğdvm”ÍoQÁ×©9[ÛX°_¸$NÊO“Is’?Ÿ¹<±àÛ€Ôœ­m,Ø/´±xdŞ”²è‰·€\0Ru¶¶±`Çhyéñ¨Ë±I4¦l* €T­m,Ø1R‡¸?l«ÊhŒsòËNşº³µ{F›JM?×<£îÄu›\'LC\nş7ä½°´àï!¹3™gÔ}L8º= t%\n\nÖ@›NO?6µ;yNïH–®dAAÁ:ÈĞ™DîúŠ¡r9l(]É‚‚‚uĞæ ¨§v\"¸Ma2ú¤   à,É_4•&õÅëáä3»¥   `€ËCS¿De¤IW¹8²ì•,((X&Qı»kQÂM¾”í:¾)(((8¤†šãÑ€”UìÆny²åwAAÁzH‹3 mW¼O6ù ÍõÜTPPğwqÉKY¿p–½XÖ\\nyYù§ì*((X™{vZwµÆTC\ZGc.ö¶JJeé¦  `eœVá®Ání¿‚‚‚ÿ©‘7Å}kï|ª³µî‰«½°3bZÏöú«ÚşªÎ¬y?Xg$üäŠqéhõßZõ‰’ï¾T{bí¿¥fk;hìÈıæ:ğíÏxõÜxË÷kÆà°Õ4\\M>µ;Ã¹m.¸X*ğ\'5>ßÊö!z¿°$ˆİ±$sÄí^gÒ?;Àçq6S*ÛãkÇÜùÆÛn!¥fİM™Ÿnå¯–#’(,	bo,ÉÇÜÒÇ_˜éŸé¼N÷_­,qÄNñ¨>çÇªİÉÏ¶ò¾w!’),	bg,Õğ¨3$ĞÎTN4šW›²’Ø	ªì»?‚:ŸÜbb«Òäg[¹¬!w(,	b_,ù&ÉöfmıÜ‘wÔõô;Y†®dS?Ã¾×ÕûI—ŒMNa?H½Pqää:•Ş¯GcŞ\'…2_\Z9CaÉ4–¤1,9<®Õä×cİ¿WŒ Œ¾^\\Â\']“²©	ì	™=~YYËu:©G¦í´ÓÛ…%W„Æ¹«¡¯Ö÷Ê0¹KvYçù–MÛ¦&°+¸u¨l=dœ”<DYòpì»“ÿMXÂ’+bO,Ùw%©şÄ07‰I8Öóİ›¨_8¶kÑÙJ0Y³ï:¥ôÃaÜRgMr;–\\{bÉX¡õ£ÇÔi«®áXÑ“/Æé»Vpˆ‰¸N+ó´éş/1ÑK®ˆ=±dW«©áğ\rL¶k–KÂŠ|3¾j\'7a¹N-ó´¯ù|+4µ­ùs_Ï•Åßç)‹äÏu$kÏàÙ²şåŠ{Í,\r-KŠònÌÙ>¹±–Opz*.Ó^dÉã‚K<İ|q~‹ÚÚˆ…sç¾’MºòûøôÉEWy\\Ì-Mgh¤F½òz‰[ß%aEOÒQ½üú[[~kJmø§²Ö™ëœ {¯Áuz™§G2ÕëëXìï¶íßêPÔ\\\'@İ…©“µtQ87í¨b1‚çi’à#´èitÿÂ+MÔ“\Z–¬¦¡Dou”áõ$“/Ëc¯×ï|·óa‰%Íµ*fŞ\Z‹sˆœN©.njá¥\"kÒ±~¿~§_>teà·ĞëP2ÇúíAÖp]úLwñ¹€óÓî1Ë+} ñedWm—ÁÂ“ÿd4ıöú¯õ,¡jâK‡»…É¦[™øÃ–ïÖùàıç5¨Òü|	òoÊqbîq÷Ô+G×ıvtoÃü.î¸åâıFEéµò@G[óù^~xSÎ{ö(å,iü¤W¶Øª†(eö¼“—É»‰úï¸Ä’ï¹N[œ„Ta,é$sO{Õõ*<ÓcO¢·X>;æõ~œwŒÇà¢˜áAo‘ÇÛ}.çlw\rKàğ<ÑúºÍYxùĞK¯¯ğ^	k¨Eˆ%ğÍ>Ñæ_lvfæ)=âvİonb™™½pš>\Za‰øV~¢b‘¸Š¶“LĞÍ¼¼Ád™¶gIòáÓfö¡Ñù‰+Ü/¼ÿáÛ¿Mì\Zu*¦¡¦¹­©y{âôJ_Wª°ÜÂı8,ºÅÉj~?:äÜÎ¾\0:¨tá+¶Â³dÿdfĞ°8:\'±Îİ^AZÊ²ÿcqN‹a®LTCg¸‚»Ã\Z§`\\§šyÚï.ó>l/7=?Ÿ\'\'ÛÕYÕÙVÎœÂ¥]À,ÀÍ^vşÓ˜Wa–<RäşCVçÈÇ™Ú{EfòÁù[“¬ÃW_a‹“›\n™©EsC î¡LùÜrF¯?âºßöb¢+N¹²óÓß°ı%{‹f-i`«Yë9‡?ÅæÃîòË‡®™¼3¯d÷ƒÔ;hOXå¨ ët3^i»§5!3k\r³Æ\"½_ßá«ÂÖÂ±ä¨ãV?uTçføpÂÈ÷W°·jœ›¦÷\\{è•Øb^	,DYr¬Gmwî=3H,»]ßf¿=Õc.Ã·ÇÃ´îu\ZÎ\\\'úot‰óË Şç¬ë–ŞŠ‚m»S†¸Û×±½1;á7‚öª{«h»É²Ó C¶i÷½\"Û^Ï äXvë«<Ìx—Ÿ×ò^Õ§{æºº4ä£òŸ¼3ØölÈ=¨ŸÕé.¿sÊ\ZJóq=W¯dŞÕ*tKXœgJWßXoUVà¢İbóÔ®SN?lú”¤ÌO÷©>Õ·Ù—oG´füiìãsÒKE9ìûî~¸©~¸èm’àplÈkë¯qûğë÷O—Ş¼Úwè|MU!™Ê?Íäñİûü7pÛı­{,ƒ \ZE‹³	l†òÁd×Ûœñíaøf‡öêÛ”7M}ÙÂeó3Á½Ab¥0™£;sŸù\Zwß,ÎÁ/³Ì•y^ï–Ù|õP­‚.àÛ‡Wó,N7kÏµC†Šº÷N“+pvÒ#Ã4„¥e“Úîá|	âÌPÃ’}YÌ{T}ÿ2x¹é~ŸuTæ¬fo®Î’–0e ü ¥õn<‘¿úéÕ„æ±Ã¸ÄEüDİ‹ç#´¦o)ş\nòÈ’vúk_’A¤s9š2}0´Ìš0Gq‰*-å…#KÑ.ÌküÉİ/’÷ôà¤HºBš÷&ÎTi¿reOÏA‰Ò5ètï›&×ŠÌã:õáƒ÷.X3êeèxé—\r\'OÍ’½ØIVÕıêæûô,õ#e6	KV´ÃúJØ/Ï-{~¡¥Ü7Ğ$ãl¿çŞ¿^{?G‹“ò˜!ßXrŞÇdr¹À’sûO½IœÅ†`\ng¹v4l6÷³íè|ñOº9şÉ·eb_5şx#R»Qe?Ğ.Ç’ôAfº±•Ê=ÓäjáË\\§ßÎps÷XÒ½½Á½í„˜H¿¶Ñ,Ùé[E÷»×øû¯£!œwœÖfIÆ”AÇÜ\'êÇÃÀvşã™ôŞ°èèûÏ1eàH¿ÆŠ³oU•—¬#6ú÷Îõ?L[\ZGïÒpgáv´—³0A#úâ°Ã‘pÿwGSìIåe¢S|§ÚmPöºéŒe–z÷İ¢	ËÆqWĞ¤¦ÆDtìëÅxt‹I×œ½ÜÜ=³Áƒş[íÕ6’%{aÂ}k™—eK©xñİÍ¿´im–dL6çûõüyø¡^¾Slº²ğßí=å¥Èõß®¥ÇøtëQ{y|ôÎõ§+¯´Oh¯ö¦©u•Ÿ;;M¶ÕO@m÷ìÜNôWÚ=ñ1ÿä6ÏÄË1mW5ÆÏÂ…Ìhõ<†ØléÚÌùğrTs«LM\nùh(Ÿn#<º…¤[¢ñt2ôÉ†¾èªğIÓ=9¯ûg{¨7Ÿ ÊRR¾+³$Ëî½ÇˆóºwíüÇöõcX»ÉÚ…-ƒJw¬8Çå¨çJ@†v3óL¿Ü™ŞWB,£¨ê&#úGÆjDlØb¦@ÈbŠ>é¾ñ#·]z÷Y]ïş‡õ‰¢,IOOP»6§	ƒß¡`ÎwX3®‹¦Ü’›¨:zÍ±û`ReÔwdÌìG²\rñúÛ×“YUîû——Á)VfI˜ÁQ€êlû±óß»ë@€ì©İØÔ:å7êGf	yäÀÙèiúŞ¹ş+\n–´œU$ù(Ìevô\'ä†ãŸxPNS°äq¶_üÔ§g§%[±­Ç×e.ÃYU:yú‹LNı¼Ğ¼ ÷iñû77ÃªyŸ.;ÓİÉĞ‡¯»oUF}KŸó.Ù†úyt‚ØDÍ|QI¬Ì’}˜°»]HøÄ`:L/;…FÔ”ÿœÄñâô‡W÷fzwe£ÿ³‚%ÃlğB`q4ÃˆÊo¤\Z92(¦…\'Í4áWeyY»M­ˆíï6¦¶Í{³=a2oØ{[^C=ì.šü0ª®KÄzˆF§éd,ùì‡F=\"ÛĞ’Gf¯3ó^$VfI·d8ñÕ9şö[âU;{ëÌn4şóô8\"Á‰/İ6ä>í|,Y…Ê»×kÆFïÓ	ä †¬\Z]\r£×¼,ñ>ûdv¬æõI|v€^¥ßwü[.ÿ¿8]nn¹0ãÅI*ˆ)cADİÒ£¨R¸.…Œ%ŸığÏ¨Gé,é(\r¶fÉ@‰i¸W-e—£5Y’^*&NşåcIÂÊ):ûö=ÚcÌ|?ò­â½Æ?éùúVv#«gã¾Íû•3Á.h‡ÇŠÓE«ízá×s;Ğ[C3Âué(d,ùì‡F=úÓ,IU4g©5îMYòÙXAà¢/aÉ÷¤Ú¬i}„%;B|\r¹_sØ¯d×ÃlŞÏëPˆjĞ67Úá±ât²šÇn&\'ÙZÙàº„2–|öÃ?£EX²ñgFÔÓ—o”aÉ–µÛÛÌá…hİõµq‰òî…3<Â’·ƒ2Ûû1Hh6©÷„¿¦ø–¬ã6¨gö?%jbd}­{ä/lğ^ãŸt}ÅW\'vp÷Óğù_s4sÏ»Æ>\'Ä¢kÜTòã´f,€TôYd¹\\}®KJ!cÉgíëµ@n\ZŒ°$­?\0º|üÄGX#ìé!†û/=VãïTî{ÃÃÉ’‰ÿ@–|ât™mªñh(KrÙDWo–ô¨]?ŸX½™lôy¸<Íz•T~^W‹ŸÍ’É±â”ÕĞEìaÔ½úhû	×¥¥±‘gT95=²Æ-Û»½»2KJLyãõ şŞ\rC%ôˆ¬qÃm!Vœç÷Fíjö$K†Ù”í²œ·æ‰Ïlê‡T“ëò›¾è0Œ	®Ã]ÉŸ‡Ú„Ôƒ˜»\n–ìR\0v\Z¡`~ÜÖŒ¼p]b\nK>cw„OªÙ‘ı’`Y²İ×º›7È]™%#--DßµŞî·ó_L• Wø¶HÄŠ“ÄĞ4æÃ‚|,Éemİ½%üg‚p1\" \"DÇ’ã~ÈW§r ŞşÇn?e˜É¾8ƒ^CC;<Vœü^-Æ8›`lU¸.9…Œ%Ÿ1Ìál²\rõeiB·ğ:£U¸†3CVfÉ¾=àK\\6Õ;Êú¯Ò¿s›õ[Ê5[Í•¸*Vœ‡Ós#gåıh(ó±$—Mxãğ#^fäi\Zr+éLà½ógW§ã¬€úkÂª·eáë–vx,y>ª…½.]‡ÆaÃî$SJft>ÍÇ’lü–¾é’›y½ºÙ³MXcILÄ”şİjúÛÊ,9ç«üß+Â)œŞæ‡ò.C|5é©şÃ–AŸòì\rŞ>ğg~¨uø|,É…ófÑû»\0åFSÃ¤ÑG¢f‚JXB}¬yt9¹vÿg?^ntå3ıŸ{‹X’EÓ4zöf†­f\'?2#ÙÁu)*d,ı°w™ÿtøèåŒnC½Qşg•\ZYÓ	¶„æµY’&­±YÎLdôußz¿wÙñÚıKŞO+ƒ–L3Vœ”ôúPF–ô\Z«8rÚ0“G·£é´!öO ıDQ@R¼×bşìGÕ¯\n1~¬º¯\Z{j1`XH“$?Ôº •ŞñÊ,fƒ-æáQüÑ»G!cé‡C˜ìzöë° ëİ†ªÀt´ÔËWêÇµYrŒ@<ÿµo–ó/xÿ£¿:üògz7â¦Ù÷ßÀõüç†~;Vœ}î½\ZÙ3_ÉÈ’så.8_1¬\nwi~ÚÃé=2„g­‘÷š#°GW¹S\'s{u(Ò=ğ¿CÙËXrèhx5´¡j¨—O»×ß#9…£]\rÈXæißügÕdØ\ZT6¦\rë¿“ß*NÇÀ©Ó¦<©&ß}…D,9˜2›É»ö}—yL¼we\\ÒÔ$†Ùû–Äàå%¦u‰¥‡Xq­jf»!ûv9Yòçö6|(wÑş•QÑ£™9øtå>Cï[0ª÷oã`2¬2¼×º\'Ìp=äN=şÆ‡(ñnÿ3,ÒÅŠóşíú;—	çü&÷‹|\0Üeêk¡óiN–¿rípúxÃŸ6h\0“K¶zÕX½ë Á¡€îÃ£áLO¨Ãºr#Ø@»Pú^†OÄÏĞ8ãù>z+ÉtSlí&õÀ·cà«G¿lœ\\{ÈàñáíC5¾d<ZœC†Üx³ıXš^›ÊÊ’c/°\Zü/»ey\ZQûáÏ`·vü‘X	\Z	l(»·Ïˆ·y¯õ·×Ùí`F&Ó\nCßŸ^ \Z8ôMlïÅú2ì…á›ı¸5^#A^-¿\nÈkÈWEçÓ¬,99*pwÓ\0&Ä™t¦\rMkùİ]¯“€u4Aw³ï˜\0aìâ·\ZtÃ%\'—™<\\3\róLx™ñÅî”Ñ`v\'v*ŞaX~½z:ÖœÿŞƒ¸vî¿°)t	[&ÿoßş„ÜıÊÜFûOÌ’ÏônÿIãP/,)„0éÒ¹æ:);êmŞkõ$pšq¬ Óîé˜™ËñiwœjàúGW5lX‡·cÚy\rM#ÉÃ§x²MèñjÑù4/K2ÌBÍ¥s,ÉÕræ8Ò…z— <5ŒÖæe„ñßg.#\'ª`M½÷¨û¡xh<†åÑeœ\ZŒ2v³\rîõÀrhhEÁûùXòºPb•»ş‡!C¹£×	x¯\'	‡+NãÍ‰ÕäÇûúK!¹8Òô3.~Íâ\rëÁ8&ÇşìõyÒmÀ‘+±äáä‚ì=È÷Y–¤owšÎ¦,%Hn¦j&ÏcYô-daª¦Ğß¿p§ÙkœÙå›ú²[OÀMÄ¡Kèùœî=Ëz€š™Ÿ\\a;¦×=	l¦-¤½×›2™uëÜ¢ZÏäâèK:o‘ğuêú‡CÔkÓà‹aR=åÍÖñ‡!7“ObëösÔIßœ´Tœ“°HäÚŸ]]ùS:>?ÖîĞù47K>c‚Í¼u«éšaÉ_™—¦7MğA_5\r®ˆYòYSÚÙ;·šÑíµã{7	ÛW~*³ÕT ­™sÜ¼2xáÎX,ÎãeŞ™yÜ(EYò·˜&)sÓrœıå¿¶×Q	¯H˜«Äq¯M>”&xØïü#Íí´0ÿäõ-áHå],ÎQC3öĞßç™ğ`¾Wß\rs¹¾Â 7–ŒÚúB”%\':œ½,OLÛ½|¾{zFŒ±—X˜átü&rël‰_ƒp8Nòš×û€?^oÛÁRÏà8ø¶\\´&-¼Ïç•âø\\µéL¿Ù\ZqZ5”ö:›ªµÓØ©8Ó­šâ·†º¡†æĞ7Å™è¬¦âÎô³ş–X²àOaÂ’ßŒêBÎ¥j1.ÈÿM–,˜ °äÿæ–_E6›¬ØìdL›‚¿ŠÂ’ÿN×äe¡ÈÃÂi®‚¿†Â’ÿªšØ×…‚½ÅõAV»àÿFaÉÿ§‹‚)]şE¥o…ètÁÂ’ÿ+ª³u(UºkeüïGÕŞì0k]ZSğÿ£°ä£yn²âÉòµa­ÚÚÊaz<¯LKt(,ù\'p|Å™âl\n;hŞ$©8£[ğ¢°dAÁ“°_…$z–,(qt=˜³Å¯Ã£eqs;üµzç¸\nendstream\nendobj\n4 0 obj <</Filter/FlateDecode/Type/XObject/Length 56065/SMask 3 0 R/BitsPerComponent 8/Height 229/ColorSpace[/CalRGB<</WhitePoint[0.95043 1 1.09]/Matrix[0.41237 0.21264 0.01933 0.35759 0.71518 0.1192 0.18047 0.07218 0.95049]/Gamma[2.2 2.2 2.2]>>]/Subtype/Image/Width 1318>>stream\nxœì½İlËU\'ø§øş	•oôÊ|xix˜#Q%Ù#x˜–Çâ¡Í`8²¤iÁ-[#İM·ñ´EëZ\\Ì-É¸Çª£4rı\'ç¾9§ªvÄú}¬È¯Ø¹3Ï¥T)kçşˆX±¾~±VÄ~ófĞvº{õ³O?åËÓ÷øï\'÷ŸW?û‹şáÃoÿõ‡/?®Ÿ>å{<øí¿^ºsƒ\r\Z4hĞ Aƒ\r\Z4hĞ)è	5×¿÷hß?~Á#ñ´§¿?úéÿ÷ïüÚö3ızN[šƒ\r\Z4hĞ Aƒ\r\Z4hPzFĞ÷˜°Ætvır?AïˆµÃ%oúoÿÓ_>ƒèw)Ó?Óñ·§=}Ê%åß¥Ù3hĞ Aƒ\r\Z4hĞ AƒíAŸ¼|}/Xû^àvã„{È‰ÿù_ı}„Ò­4· réáøÒl4hĞ Aƒ\r\Z4hĞ Aƒq8}´\\Ü‚ñ,Á—üğ¿ÿË7ÿòo\0Doİ[OkÃ—æå Aƒ\r\Z4hĞ Aƒ\r\ZôÆàëpä®nƒö`—lÃ2íP:N(ûoøğ_ÿî\'Së—-+»§”·Í}/ÍİAƒ\r\Z4hĞ Aƒ\r\Zô™#^so3nòÚXO³Û|·{9-|şü¯şŞ$¯íæßá„i5·Gâï~….ÍïAƒ\r\Z4hĞ Aƒ\r\Zôş¿±ë12ıµ/ù²?İékÙ+Ãïów?ùçoşåÚÛ•+$·¹ï®k¦Û­_š÷ƒ\r\Z4hĞ Aƒ\r\Z4è=$*7øš$ú·\'ù]HgÃUÏ§=Pš»œğ=¬-WôM‰ïXsÜŒß–ìvÏô4hĞ Aƒ\r\Z4hĞ A]ˆSØ·ï†kòú•Ãà¶†œ—~\'ûª!À·où‹¸ãÂ¸|‡—zÇUŞåûÒ#3hĞ Aƒ\r\Z4hĞ Aƒ.•î&ä{GË®c=¹ÛßŒ—iÛİÕìÊî¬¤\\Œ—&ıİOşùßùBÖí:ó§D6ÁçŠ»ãOµÔœáyùuéQ\Z4hĞ Aƒ\r\Z4hĞ A—D¼é™VgX[Î)•SÍ9V‰?ğmm¥z¨9¾äñ`¶o9!h-&‡K^†ƒ¶:½|bâ{úié4hĞ Aƒ\r\Z4hĞ AçNún/Èkg<{w¶gû/¨câ›ÓBïÇ#ïjË©¼‘õ~ù×P\'+Á·Ÿ²ŞKŞ ‹¡«ÍÍÕæúj}ıîËúÚÜO«xĞİaé\r\Z4hĞ Aƒf¤g¿¿Ñ°ã‡¥[:hĞ ¦«ï(ïŒk±\rò—l‚inŞ½Ü–‘;ˆ]^Æ­\rûáOşå¹¶<Ù*­±\rÚ–×œ‡·ŒákåïÒ#9è,¨z½ˆ‹ãßâË¿ö›«ÇËWå*{¦‚tBâÃó\Z4hĞ ANf*ÿuQÄ\0\r:²ë¬=èvKª-^¾C°¼­ªÜä»áä°“¹/>¿W[÷7Ó%ÛR%.K¹³·¹§/>¸û3Ki\ZÚÀáDÜ7«§#áøJ€y…Şt¹¢ï	ã›fĞm77K³mĞ Aƒ\r\Z´…¢ï^•0`ƒ~?‰=Êù«áô\rZğÆuıWÖY+æÕ„xZ=şPoKÛ¦¹«î°q7ügõÿ4€sö¦0@Öx0M‹?É«ÂãÉc?óÏ\0µ\\Ûæzú$¥_MH^Áò&8SM”—GLÎ4u»Šñz?_š£ƒ\r\Z4hĞ J0»¾¹&¯=MÚË	qz?Îöo†£4èÔd\nÂK°#4N€öEÖ.‘mwZcà‹Ç9.%îŸĞ¾åˆ=v~9ı•<—Û*œW¾ô8êIìì¶ hƒv£ã{:gÿŞ K•ûlä>¼¸;@~×$Ûê‘oÆLø Aƒ\r\Z´,QõÚ*T1ƒ.^S\0>p÷ “Óg³´’0õ”;NàvR\rşœ¡–$øàeØ«\\!ö=ƒk@Ùm¤/ïé®û–ç:+#{ã’íí[«Ñk»c>ıv$»/œÒd±;®á.‹]±-£ì5üäï–n™rã%DO·-üÑ›k-úÒì4hĞ Aƒ>s„Im^Æ^Áõ¶ğ`€îA\'!Ñ¥›sRòµß7Ş£oìâï[ËÎ\'Œl2×îä´tÃÇ/ußrHd×Ô³ÂçP—„õíTFş!ı›”¬kâ{€î‹#Ê¯Ê‚kM@Çuy2ßÄEÜ¡’<ù‚ğ|ŠÃ)Ç]ş]‘Îæ²3c­ÚX6hĞ Aƒ„üœ|œ«\'M¹o‚Ø\Z¢|Æ°Ï S¶£ÑÒí:İpú”¡Ö\rÊ¦_êŞ{˜¬élÔœªNVjG}sî‚îé¹w.½şÃÿş/ßüËøúğ­©jÌqînd·ámİõ>¦@}i)´…Âm`a7ÉŞÎ\\mâ³8Gû…nõ¨Ù”±oÅØˆÀÓ¥GÆ¡×¾,=Dƒ\r\Z4hĞ{Kà£/.ïÅı«{´~K|ÍMµJ¡æÒ\r<¥yíø&/É;Ç-ÔÌBlÊtßË¯\nÃëøÑˆ¾wÓ´@h˜}a÷»Úr¬î¾oõr±Û5ç´úãÒow·5zİŞ|Ú;}iYä)æ|/ËæEÓkŞB\\O(N“ò*ÖxÓèx¦­4#ÄûªÑiu¯•h›‚viçÒ#6hĞ Aƒ½o”Î½C sìY:@Àx†Ô$ø”\r>3)F²íıÆïëO²IšÕ\\m~sÓ-ÊíŞé¦Ú<ßÆííq®-7èX’àîåİ\0–óÓâædw<3¿´PªÄ’ŞT¬ª\09º0N(ë¶fI¥÷F¥ºÎìæŒ c¹x2{aêöùrdéÑ4hĞ AƒŞÂx\0§Á;Ó¤ú†bYq&³ôKwwĞ¥Ò•,½4SC“Ü~ŞÏ(Õnı­À6[OMeŞXé]6\'çwy@›¡=àÓİë¿]{lİû]¼ÉıÏ~ø“)û–´kò×ŠbRö»®é{Çv{ËØÒÒ1kÈÙiÒY*±µj+\0v®!×ÚïMp¬k<MÿİÔÈÓt}JrOšÇ>ºª³æìAèûÒ#9hĞ Aƒ]<÷j¦÷¥2­„+\nÖ!Æˆ7¤aøîA{’kq†ÇKï{¸µ®›±6½c+I13¾¶_¤¶¼‘›¶è>Š×st37È•sÍy¼ö{eßr³ìZv&ÏVg‡Zq^n‹Õ“×Áòğ‘é>bÿE¯¹Œ\0K»¯tŸ±‚…†õ;¡Eçˆë³üîgfÛ¾a3ÁÈt	ùn€ØPcŞ=~tßáª¥GuĞ Aƒ\rº`ò³ÜúE?T&GYo	b–|8îAÛÉD¶6³á‰\nn—îÇ,ô	XÂ§vq÷àbEèÙNhõ¡ùO9¨Gˆ­ ;åß§“Mmy]ˆmv/÷Û¬ÑrlIpßJ[a¸V•d÷Rä“×\nZN2%·H[†F¸JçdOœ>°/ºó³f©xlá†z!Óİ™§Æ–˜×|£]^Ÿş~ZÔAƒ\r\Z4hnJæÏu2?æ$  ™uŸ®ZºÇƒÎ—`Õ¿ÎçÀÄÎŠ«~–îPJóÚÉá°E9Unç0™¶\\3)l‚äú:0M…\'«ÈmcÚxÿí%÷“~ª-÷)fİWµÂä¤4^Ö|S˜/>ùñÒ\"ó™ FÓr[ÈZ~¥6¿;,ŞÖ\'¾k5{Ö·Ù´zO;1Tû€ñŒµë%ß½«zJXz´\r\Z4hĞ #ãšc}]	6ÒødšsìÃYrteŠ91ÉbÃÈZˆHß/acL\Zi†vï ëi6mo^Ÿè–{3@nïáFÏÊox—üûöËÿõİ¿ı°ız/İâŒ·J“‚óæ¶ç·6£ İœ3ö0Ÿ™Œˆ.L«¸-¶µ9bgyÂ\'µèö†IN¹1-[nİ®‡äxm#\0¼Yá6\\ù Aƒ\r\Zty˜ŒñÀÊ»oW\\§Ã†±\rË B&z”ü|v;Æ½\Z\"†…ŞKw±e{wG(İX[ájXUı`³Õ5\rTªÛzuå+²ÏŠ¯ÿv‹Ä\\?h¾ûƒÃãzß\"X¦sd7òÖfhşdº0\\Áû(/Ÿ“<æM]Ò”ÒUÛbĞå\rßÊ\"w=½j*œşN^².©¶mĞfL?e›WhÛxÛ´È\nœyà|wHpÇõ;Åö.=şƒ\r\Z4hĞePÜ ‘u}»åŞá40$XºÇƒ–¤|zÇíG”¥W ã£ñs½çÒİíC¨>XtÜ^%feŞ-ÔLOlßD²Õ¦1˜(W,oî†ùÛü\'É/ÿµGÄn[rZ‚íÅ±\"²ár!<:$Ê©}i9zßÌBæt2hŒFŠÖéq„¯	‰+\"Nb+;›;›%ÙöædB»&-zÿØ¯ò“ÖÏOŸ¥aĞ Aƒ\r:w*n·¸QXS÷u®~µ‘àÄ¤\ZÃ±éä¥»;ha‚Sw*Ğ[Ğ7çY|hzC—/İİÔ(É¦ı½}¸«Çö°ú±pr+H=¬\\,+µïåoò¹÷×LúÛ#ú½Z¤Ü:¨¸Ûf«%W¾{ÕzãÅa$ »1®7gT¯2¼œç‘2ÅW~»AÊZŞ³óh\'•ÔßÊ\rwÀ×¦§õ}‹uZ^rıïËÜæ Aƒ\r\Z4¥•½ë\"‡\0¿1ß£*ßA‡„*uº­®\rA5¶L@÷Òİ=–>¹wH9€å\'`ë!m¶ï™T†ãjîæ»³ıbğ‡OüÏ^¦5êø\nïz®ÿî~Içæ+Àn§÷YôÍùñz!W›ßfÆm™º{b9gi™ºx€!—‹3ü¤EÊ`mÌµÆ&™_f!¹ÆbÜèøZÌ£?’§Ñ=`OjÔwİEÓîaáñøÒ¢1hĞ Aƒ/9O*±G8\'})TÜy7½t_-L‚ ñ;¦™²¨Ï¼Ğ6FÔáKw÷(ŠØ9nnö\'w‹ ãÎi»àYBå€Íu5wüI =¾íëöp³MºÃ¿öõgoÿşàÇÿê·©Ò[nçùè<yofn|nÛ›»4úÒ’uÁTk]¢Ï¢mF,şú¬­®\rÄï×–©f…üÚ<ÂÅà›yìúw{ÉºÅİòYiƒ¡\râÓ·Ìs^¶É4hĞ Aƒæ£âaysòlŠ/î#‘CpĞKwtĞòäÃ6Åù¸nKş!ùÒİ=œî…Œ°lÛ¾1ß}§ˆXVLO¨öAsÙ±1åäöÎiüí–fkàóÉË£ßÕ“7v?Ë‹É#d6™ñ²j;»ù¶mÌ	ãßfğ|ú,-_—GMË\0Ìé8oè[A¥.V\ZîfM™iš,±¡YG‚üÓñ•o¿4rëGòÔ2]\0>=îŸÆ\'Og.-)ƒ\r\Z4hĞ™xvĞì7à\ZÍpğ0¶(ôDIrHì%-Lé à§wsÁ«çra¹„[ğ›o®pØ,ß¾ÿW˜ãNã&Ûn›Š;¼Ü­o	·(û)Eştü¹<ÀáÖ¢lØšƒæ—n; }È²nÍn#Z¿©íIÌš9^“uóJ\"7a¦y7¡`ry\nØíîdúDNiÚF×Íô‰)ÆçÉbi]¯á ï[^7 ìƒ\r\Z4hP‹üŞ)8w±Œ1¬‹ÎwĞDn¹¥Ë¹4$’ÒxòR£>\0¼T¿Ig»(²Ë#ûÄ÷”=g ÏĞ¸&Ùóõ×”˜æœ».<Orñ1\'şƒÿSŠ¬¿\rK§o_š\\vún/.G0¾#ĞÎ*Ø“§—UáKËÚeàSpL‚CM`Lê÷şÂ¤§süvj“+„÷je•<Ó|7mì´€í£;¼¢şú4v(V§†…/dŞO,\'ƒ\r\Z4hĞEPË­‡h!É\\gñÆÒİ\ZtF”äh0Öu“6)BQ¼è½Ê¡„R½­¢k*;—’òçİÕÌ›¸5åÖŒg	ô;û,»[så÷¦åÛ›÷êgú_ÿî6¾±‹`5¾±+fVUÛíÔlV: ãô}ß[+Õ¿-K¼\'€?6RÛJÍ]QÂ·¸Zì‰ï¥zÀ˜A]ğ}¥%öÂy,Ï®È×/ºIliøßQ²íYö§:@ehè4hĞ Aƒyü’½†Û8t\0–îĞ ó¢*$¾@Â×HP”Èo²C‘‹ÑïÒİİÒ¬±,åÖ¬qL|ÛõÑfçp¼Ğ¬İ0î3Ô€úâÉ´Óš®\r7=‚¹‚çïïêÉ#z•W]gXÖS—–\'…èˆÓı{½Í6æÓsu5÷íK~Ù\0İ\r\"‹ë’:jo=(?îYX\'v&HË{‡.áéÄ6¦Æg,1İ¯|Ğ2$åpÜã‘FBĞ Aƒ\r\Z×Ìª·m¼‘$Ö­-İ‰AçH\\®©^2·S×	&âg§†–îî¤K¡M¾8X»L[‹Ìë}²G0´OjÑMc\Z`_ªÄ>±)r‹ñ_…zrÔ.kÜx?—É2ïR.{ ÄMp^’İ·”[O½´ô#Ù94A||SÏäÅh…ğ:%èğ{yÊÒLzGÅxNF’óÎü¯NSÀD„ÇÔÅJ<^Æ\"Aø¾4‡\r\Z4hĞ ó%ˆj T,Ô¹m]—nø ó¥•Ùí–6Ÿ@h/ßÈOÓùKwwW2ÕnP&ùå;W¡íÓßm,LÏ\r°ú	ªhosâ÷ØB·ß¸o†ıéñòo}ï‡»BãÆiT%¾í4½¹İJÙ]¦{×G,-ƒçEvvWJÊoâ¹ xn\'Š-`Ÿ¾Ã·Ã™K³ÇLK¶ºÉ³è¥›n:” yV€”]RúÒì4hĞ AƒÎ¼—0féfº\02ğÙƒk—o¢ìUŒñ(ç5E€Kww\'Šë¬?A&¸_ëä&W®[Vˆ·–„k3ìZoÚ™<}øƒ¹Š×ƒ?ÄıÉ\rÎ5Ùœv‹IêwçØ7¹û·66/wkNdyóÛQ^ÈZje¸d`*/q^tmêì.ÃÇ¥3	6y­Õ\\$èqû8L‘›Ù	™©Xš1ƒ\r\Z4hĞ AŸ	J`µÎv±¤djl¼¦W–îîNäwÅ8tûëÀBéøBZ›wÎ?¼.»uNŠô9Mo‘»9üÛü\'À¼VkÒùÖaØ’KòZSäş\r_ö]õ²•j7öQ^H«e»Ì}7\ncÚI[ç“ËKseo*-ç=ß¶Í6ø´¾á˜YûãÙ{QÖxĞ Aƒ\r\Z4èÒI ó¶’Å¸–Á\'¾CÎ‹£ëˆñ4yİHO—·w•ã¼±¹‚t©åÎŞÌõ‰»sÄÔş–ßwt·{Ó.÷wSÇßÕ“G¸­‰ém\06M1k\n{Ê}ë¯·áY	Š÷¯öŞútíÎgt§{ |[­ÀhA¥€Ávé5BÔ¥Yr	ë0ûOıÍvkà=Ãã\nÉù×‹)4hĞ Aƒ\rºhÊ\"´ºç^–„Ò\r”4À6(Ë—îîÊÊ°„\n.V ÌÈ½¼ÎÛÁp€êÍEßu£3ÍPÓk¿ÈÓ’p½´êzúİüØ€Óøv0°}Ë¼\Z`¹¾±«±Ê[+ÕhšêÏ	ËûåŸíÚò¤ ¥ÎÈ™—bo®y\n®\Z“5*ùÅáéK3£y‹ªIj¬÷¶„ÀşJP}úwif\Z4hĞ Aƒ½çäÖ\\Û¬ŠÍ:UlÎ¡ÀÏ:ºãÒäUY²IÚaŞx>m\\v\'G\"B÷+Ä_a{è>¸r\\³ÕtM‹ëoÂ?Ÿÿƒÿš×N*½íà·Ésô¿eû%ß–—~ËCáÑÍ„{º¡zÀòKËæ2´C‚æßV”uØY}Ë*cÜTÀãÒÌèLvZÑ4C­snh°şß¼§¼4hĞ Aƒ\r:+²øšÂÃ‡÷GÒ-€(,Ü@ˆ¸twS²9k“,†ñC|µÖİ+Æ°xÍ›¼è‰à–o|lzİäµ±ı¡SQıŸ>Õ“¿¤7b7SÕˆymª\ZÒÊÉÄôå_\\ÓŞ\nĞ›Ë®æ±µH_ZBOM%…mqw«f#–¬²&…ou¹¯Ü—tîB¶ DÖEv1W¥\ZÁ-ö	ü¯Æyi6\Z4hĞ Aƒ½Ï„™)—1qW×Å/€Ö9{µtwSâ²jû-[õ-Pı9÷í²ÒÔßË%zïmàÚ\"kÂì´í¹7BşÿûoÿÑ\0Şfbº }®9¢ælaµ=Ò,&ç\'RÅ»&Ä“òõ§_—Ò“REÜL[Å½²	bâ3“r	N”ªrú»4f\'@Öºå{°¨æ5èíK…—û“Í_š\rƒ\r\Z4hĞ Aï-™ ¹±/n£ÊT—s\nB?ÛdJ„·\\+~Ï™\\Û—y ®iîmË´ë%÷x9Öº›7yÑ!ŸÂ]™Îeç÷S=yØ‹,ÃÚ\no9Ç•ät¾İ?ÓÜOïŸ®Ïg\0üşlŸÕ4·ÉJ¹8V,·_-e\'Sy4ƒ·4NDØk-¶Ï\r¯{5y¸Ø÷[“ºıs5Îç@‰7¼Qá_º¥—MŞ\\d>¸}	ÄöJ\rÔæú\"6ù¹t2îX^òï2èÌ‰a¦Ï;\\¼Xr€GúÃc-t¤²Òv(x®ŒJk¶i[3­Ó–´5¤Œ5WşÊ]›u+š¶uï„ıUñÙ_™ÛÆ¿ïêÉ“ŠñšË~™¤•Ë‡mñi¥·Àp]p\nÔ›élÓŒv³ãå/?+º\r:®¬–“YÈß£MV§Ëj”¥ÙpR\nàú†YM‰ib5¢f‚Àv)øş|Ã¥y°<Á$R”#ÉMOG5`Mrò|ƒ¢E:ÙÜ€Æ(\\¾\ZSK k‰“iìì|à…gK&í¥:„g{İ±3ñãK÷uĞE˜n¬¢Ì+¬\\æ¸\\˜­ğaÃÔ/^T(¡ D[ê—î.SZ\".‹qiwİU¬µy8ÙÄÎ„Çõ*ûIX{(}Ï÷Ê¯ßıÁ³*ë\nW5uÚ¸Úºñ¢í-ïóÊ7LKqôÔ’Æ°,O§--³³“Gº+\ZÀ6p¢X+œãbƒúwi,C©ÚMÒ6×ätVø“>\ZGµÕÂö§–w»ÛYÍïÚ ‰\n—ÃTóFKàsGjI£\Z\r0/Û#\nŠĞhKdşÒl8êÈ4ä#U\"óGYKEˆá¶]šıŞñ-}HôaN»C5YQËx¹U\nŞ(1èµ?Ûò—cüŸúÕóndÒaÏgV…Õv!\nMOÅçf3DZ¦Æ›÷º6#·Kä¹@ÇYñk@‚OÑà$Lnpcº°‹÷1sgXå©´R:ÛŠ-)ó~ xÎYø|\0î¹Ãfk±^¸ñ®«Ä	h<sÖ´¯øKË\nÌ	PÇšv½¼Ÿímm¦Û6F·S{ßt[gêİŠÅ†ÎÂøIuëÚú©ö%’±~Õ+\r–ÀÒòÒla;WüïY´WıĞ“H¸³ÆTAgs.Üõ®íıuo+ô»Z’l…nAP½y-\rÜÒìY€(p=ü>E#&³ã\'ZA•¨Æ¦9ju‚+¦YéVŸ]7L©¦8¯M%@j(Ó¿#lx¬‹I\\šO{Ğ\n[\\Şsƒå7Êº”3`‚–înJ$=î–,a[«h‰«İ àY‹¡€^›5]Ò…?¾›V6v™Ùê¶ŒV¢à5@ºû ÉÂdà§ÑıãyeÁõöªo—›wx(@Ø¼Æ+.ÁnàôF]º\"îf’š¸ÙKÍİêé\'SOAÚPwmÀ2!hÍ5—l¸îynªwˆÚ&Ğ}Âî–İ³çÇËŞÙG>äÁ	’ĞÛ¢Íì7«¼nrPı’<õdìg–tˆlÜ8£ªÑ‚\Zÿé×óŒ”Äßã@½ã¶Ó öâÄ·ºÑãó4p2¼oE7ê¯öDå³i€ÓÇkÉ¤ø‡R ÄÏZša³3ğĞ£¦$a†möÖ&‹YªúÂæICç`Ú¹‘	›­¦€I7œçLY`£M;ó>rĞ¤)Ï]Ë8¨ŞV™û/³œ]º»L(Ç¢TÄÎ!MMÂ=3Û“ÃI;FêÇé¶óˆb›R³¦±™²\Z¢8ıÎÎç­”\ZëšäÓÍUÕJ$É¨€1¨Zä«¿jŠ1²İ3æ¸MÙtÚ¤¹=r÷™îx[³¹N øyƒŸ}÷oÿ‘Ş‚Í/¹Şg/²ôÓ(\'Ø‹[¢¥«¶ùş˜o¶-t-mÒÓ¬ÂûšæV,JÇ¥Ëj\nÀ¶°-U«‹5êÏ–yiœ±i];†“OÉßÒèİM•t¹tïßÃ±¸Äi½÷´ÀUi— ³Í¨ê²%â²ÎËŞBÖÆ÷Í¥BïIå“Uc\06(>‘pÅFS\r!§§5Ó‰¾‹e{ƒŒÆÏ^·RÙKS¦©™\"8³‹™j¿ç-ä|Ì\\Š8¢˜\0<A7º¥T(ÂTìh~1ÜÓN„a:[ÿ¦˜˜Ãßqö•“­NÎEÔ—îî3…<&ZÚ©µûİMy¥&]£M®Ÿ\"„fY„²}—S×!°*Æ†.rXEBEU¯‘±	–óNğ@¼ãäĞ‘¶bÏÆ\ZgÍ7Ï!äëeÓÉ÷|IÜ“œ“ÑTg¾u‡4l‰é‚V­O\'H:XòÅ(¦íÇo“Tu«Ì[ÜNØ6ƒê[~J¶Jã=Ïµ=OOSäõ§.Úznô¤k+ÒJRL´\0Æ\r©µ‰>]ıòtÂÒ½?#’¹ÙÄrcËËÕğ\'sØ°X\\ÄI4ºï1-pE“ÉUäŠà÷g¶óZ³´¯67|¦Ê6ß-°}%˜•Ï	x¯tıBöñ€=ÜŠ¢µŒ)X•ú‰N…Ó–fç±ÌO7»Ş1×Ã¸#škS™Œƒ®Æ\'Jût@fFLÏK/EF>m™#8ZİD>×AêCƒ®ª‡C`V3YOÌûÒ<òëßÑªsêAûkåV³ŸK÷=SË{İĞÔrQ¹a^‰2¥,ÅÎ¨!\"cbmN•[\'ö¢<ÂV³§!™Qaù—.D; ^N€©0ŒvÈb³ˆÊ‹Ï:†KZøí³½÷5Î×&¸)í\'Uå÷Û_˜ü`‰ã¿­küaÜk§­M½wV¡İÎzÛ÷…M7lln+Ìu½vÜ)ıV€¶Ïq#6?FÏ24½óÆŠ,*\0¯­¦Ü6”z\n7Ğ0:ÛŞ*°İÔ#…¿§îé\Zûo”À2]°ı\r7Cİq»³Æ·\\;g#7Ò tû—Ú¼0ÿ`©cã€ˆL£ã%¬²£fl‘x‰äèÍ}€4}–æî~dä\r¤Ú\0ä-7Ü1‹„Iy4{—EãçÇb˜ùxÕf×Ş-µ\"sÈ@5Ü«a×	x\0r|\nÇí~×x²hSªÔÓ9Kóû™<c­¥ªıJlËÔå•^2¼X7ƒ/ãu‚ÎHn¿¡T=Ü3G¼;ğR*d#Éò¥”ÿÁ v¨»äHÃÖ]û¾(g€K‰¢ÅÛf¶×ğğ\Z´›-€LVa>†KbO°4¾’»µƒY·ï\r:Nñx²Ü;4Föa£y6] 7<b·É¡Tÿ¶¯[Y]è{·5åí³Ø!Ï»£Kâ2òuñxı²eS›çàçR@@ÕŸ´5û_©à½¿Ê¶çâ|Ï‡|ìjÔ¦^Ì&¡ìDôIb!îÑ$!ÿn%pËêí¯¢TwÃƒË©9^¥¡‹½Ã&y{Ş³‹K±Vn„\"\n4ÆÅ§ÀŠĞ4Z»’™GÜ§`šqÈ©´àjÊ¤°ã½Ö=5~‹\\•±zd¾gÃ§¼ìÄWpB5ü>åXIÔAÈ7©©±Ú¸´¢),»Í™7lÓqo\'#º—û˜9ÉÇÏÒ¼—éÙéé\n§ÕòØI!eûéKëS)\"f°şÛºgfHuu[¾¿¨‘ŞÈjÁ£A–öÆïÀCvœŠµ4œt}7#²¥LÇ‹µR›\\€ê²o’„‹³ˆsÇÉ«²ä|ÖÓ´Ì[_™\rh}[–ü“·Ãı[o\"Ã~•ÆÌ«}©\"î,÷½¢Ëíº–¾ÛÂêæË¿<T×œxøÎåñnq÷É{jiú&f\0!™A1ù¼˜ˆ¥»~¦äLt}E-³0_ªã¼+œ½Sv¾w°‹fl\"ŒÚÂ.K)Ó¿ºŒ?q,‰.&nóŒ	ì\0áe ç½È¨¿ŠhĞu`{…„Õö_d/Çúc»¤ImÎËgi®ºÒ!(’¿ˆY(C`îik“t°²s¢fÙX:h‘•ùdhV ¿7l@B×ˆ3§½ˆåœw¢k#:µ(ƒ²é	Å×ëÄlVqAÂ°gFuÆ! 5I÷rOPyø¬6¤hÆGŸ¨S[µr\rC©	M^İÀlŠe]ğÅîP¨OÆPB+à;$3$ü·şz(ŸoâM\"gÔQrôÅ<I4q£_Ì4‚±ùvDˆQ¬›¬GFé\\\\­›éJçŠ|Z±“mÀùÅ^‚Öïğü¸^;æ¸µt<¾ÜN#Æ¢.¤uİŒ ,KúÛ~÷)õô\'¿x¼Q‹Ş8Íş» “;«y´Ò\Zw5Â×è³¶&D¦§,İû3%g1¤A»MÓËô¹(ÈEw3õ¥Y*±7œ¡S)£ğVĞİ0Ê9ÓO¾\0 0Ÿ3kŞ[ÅÒ ¶l£Ùs(ÉôWR]Ì=3ÇîNã™–x&2¡<+K³\nXI’ò˜Æ—e~!oxm÷I­H2Eœœ…ßÍzg±¢¯À¹áfk;M]3ÔU‘À‹±È`m%hy´\Zá7Œb¸g˜ÃÑ»Ÿ=CcË\nh‡i“´¤*8h®Ì|^/8RÆnDæd>KuMÏaV¿û2o_¬SL£àºi¥É÷gæ°l´µØ9µwP?(rÁâ¹1bCtÃuwG’ÊÄcO]£‰²8125Ş(¯)ëĞ,ƒ[ôÖş0Œöî YÚëŒÊÓÉ\\ò‡ÓºlNRcK>‰ğYkÚ©myôaœ™ƒ|š{;‚ş˜³Ø»¬w©j†ùÙp)÷ÕïºÍÚ{ƒ¸AûDÁ[63	¨Ö×lüÑçM_ºëgMl‡Ã¸9Ş`«½‹‘¹PÄé>³tAEBã1ñ2ñ \"n†T¥;Ô»p+_ê,ò¹ÒŠ¸¢Oº×h„|¼÷×Öö\Z‘ÃG0¦\ZiˆæáÁz¸K#\rSâÈ÷ñ¹?sĞ	¿Q‡|\\è>ËòŸÕÄÙUP7G™¬.£ªğ¡Æ~&BnñŸ”…ï, -µ›E+¢Hš(·=ÒÙ‹¢‚XGQ1\r+éZ¸‰áË†‹u—al£¶\r15\\\r—\Zö<ô,zÕpÜÀYz§³ë<å«‚”É©ıE\rÊ…-4õ:0°²z#\"Ñˆ?Kµ;F¼‹Qß4h/Î‡6 &Ù–V‘lHùVƒáÒÁÈ=N1ì6jz¹0ÿ\0±t—wíÖRmÿ’îWÉU÷ÏËÀÍ#\\\Zš±9“»9•‘ÇàÉ	H‘²…½\r<½}{ë	Ù²nÿPiŒÛ†ııİWÑõg¡XHsÜØCŒ¥Áö.Ç^Q\0¥•è˜õñÇ¨ê¼ÄáÔrö7Ú6ı•´©q\"Ï˜NkLÌ:AWÎ\"Êm.OÏ !+El±ºFS’á1ŞÑµç RO-¨Q?nc0;@Äy{m‰‘ìiaÉw3¸w™Dóı¤üçŞr/c‡‰ø<ÉŞóøêĞL×úJƒ0\\Ô™Û£ÁsDa4^¨\Z©\najbLØè=}N9|–¸/hú\\Osƒ–É¿àLSü7-	IKUUfä;llÈó9\'”ÒZaQ˜‚¬Ğé˜d±ÍÎ¯@uÚ—Xf\'rf¶-ğŸâ\røÊ]˜Z!u\'ÀÔ\'„ÉİÂH‘L††¥ÓDø ı†@8ö’ÑÜºÄ™Ì6¾ËVúÉLPt%Ç î¤hœalAåXæÍoEïÚ®%èÙzm}-×´5º&Á9£SwçŠµ#%p¸Y+î6[3§Ypí°6­Ô.gÆ}ËnóîmélKs÷(ò¡r#Ì³\'„p-¼–Ö—îúPƒù­H©a¨Ë(„XC©­Ív³\0ªãè„G±1©í)ÒÃ0)\\Ç©ÇU¶ÁJMrMs˜É\r×¨áxØÓ¿]…kÛPj< †Â2v«Xb±ŸÏÇq÷ÛcÄR¾ôæ1ì¤sjÌcz1;Û3Ym(µ¼=.\ZdûS[;NI;58©ã\"‚xî;h¥1~¨µ¨G³İ–‘ÖFÖ\nôÎ\r¨\r×•áÓ\Z&Ü»*Š&#\0B}DÑü5ıÄ¢K”c‘ı7—I.cÙîó¤ŒÍš„mh¡Î0@oh1Q„á*³@€ß<Õ¶ÍH ÑM<Ç—?™~%Ã‡wŞc,P;x0Ó29!NR‘İóë62QìWíµY\'<×–Ogî+¥vtº¯¸l	nŞ- Z_¿õIÙüëÆÓuÙÉ~höøİ…`íH9pök½%yfº‹Äé ¼‰A:lïV ¶nã&ÓKóõ(bUÍ¦È(ª±ë^É´f¦fºÏÒ]¿\0¢ì›‰‚Ä`ôk  $¢(Îkß¦òã\\´f<‚J‘u—õü­A…Ä\0®wõnú¯´ÍÔf­µMÔÊÏ–`{¸Òl:>‡˜Á8‚ü˜ È%Ÿ‡Û;}råf\0¯s/äƒ‰È§5Ò87L32z„×ßŒW\rÃ‹R½½Ø˜dÛvÎ%kR9Á_e}\\gÍhò¸Ï7|[‡Õ0c‚êf%’‹Šy®›»ŠAª•¼Öûë\0±‰öúnl&\0“§;ÅĞhšÀˆ™´DÀttÈ$öh°÷×ç¡ı<\'Ú_¦M¸;ª¤ÔYc…ˆ‡îşFb] }AÓ·S%6qÀÅ«0Ù¸Ú§áŠÖS›Y5å˜D \Z5ÁHY‡Û4³SKìÔºİm^àP¹m¥¶¬Vä7«ªuWğ»\0¨\rHwèşNÑ·&Ü-–¿4¬‰ß³ÒÉJêŠµØ[‹Ã[;Ÿ#N—Wƒ™©\0ªxgtÿò²A7ÀÌÆêZÌÃ¤·†ÓÓÁç—îúÍ×Ÿ!P¾NO½ŞMÍ¢‘è•$äJ+àØI=÷ekŠ¥yçÆ\\î\ZÙKß×	¯Ú\Z<TÔ/yhqÍÓmç´çAıuğMÇÅuÍ/¾ÊIcËñ Æí:ØVFºĞÒŒÕGÌÇå‰åv•mœØI\'Bc)Åù:j\0‰¢º>+ÜßKÔ.ŸúÂ ÚB;¸Ø7õ„,v0wú5Ib\\\rÖIì?LÕ¸ÊI£ìlëè\'!NÇéq*\0<RyD1ó`EÉ™LMĞzÒ\Zkô_‚„a z58mé£¿d¤6x‚7ì~P£0FòéMKíW†«9²vÀˆ±åÓÄ ,;6CÔ\'Ìt]ÃC±U‰Ãµs\ZĞµ<pj¹¡ßÛ^ø³—ˆR9[—}—ÿËß§\nğLğüÈZòçÍæiZˆ~Ép»ËIÍöm–n¶9î©\"RØv§µ—îr|V»%ª/ÍÎÃ)*rk²,sê€$¬nlÑÒı¾ŠV:­´ÔÉRëÎÔ=mÈò×“÷n§õŞMh(\nÈ$öEğ¯Ü\0™ìm¼J·7tZ|j,ŠÃ±ÂÌróÎ!foè\rkÔB3ÿ€aRèZ«pQEß ¬rbƒ+m‰Pw¾ÇõëùHà¤qŠ!@³u5ƒåCß”N}ÇmØŞşU£d±•QCV1öğtÇ®é&sŸ¦OÇÖÎ:Ë™YïMà’20«ñV<•:”Ôe$ïVx’¾(âˆ»)‚YÇ(ğS,vC‰èBÖ:ŞMêÜ`³ÊTøXĞ¾ÇÎ;\r\"\n˜,š,’3MaO´!ø«*Ë¿0;ä¸ŸÃVí7&Hğ<¹ò!±v<™pÈ|\\´\'VËê§ËªüË-ÜKD9ãŒ+¦Ó·t½’Ó’×g#|~ˆ\0ù¹-å¸a6 ñ²éV‡ªé9’))/0Ü®³®§Ií·.ÓÖ]ÎößQ­õšoÿK³óâyË`pÌ¼·Sá˜}`{uŸéòûØ\\\n¥†Æâ:Ò<NĞHO]ÃtæŞíTãŸ!)²íîB›si„9d•Õ\"úÄ-¼j ¾U]?`àãòˆÇHâ¨Ç#s‰™â£´×V=[BB–F-N<ÿ5)\0»K•6W«0„{\Z=r¡»ç<£p5í*ğÍ!¬Ò~•½l4Ëˆx%¶ˆšªc±L¢k+¼ùJ¹±‘–™1kTed×p·îÃCE7ò-›—‹Ü#ÌbÅOt$ªƒ—»ç&pÒK”Qswg:\"Ğ€D´%ñÑpæ\\ÃDòIaËL2§v¿/ZZL\0*Ù[ÖQ`Şç|Y*’éèËŞP³ÁÖÆ#™Ö¨€%»u,pc¤<AşXÑ‚_Í7=’ œíŒx2x\rÚÓTz€¬IäûŸqîßê¥5áTømÖz\')rsÎ=#ër0–¬¡¦çK\\Ñh7®­¾ì‹Ãeó4Z¸\rhİàë|a8í©nàüã—¥yå]UTr\rv<&[BÌ?Š¿.Š¿™V°KwıbHjùÚ~„ÂËÔW–ƒ0½Œö¾íl98!jƒßôÃÏêˆñ>:E)5Ø¾¬6ìÈ¸Áäë“qá2]CÀ§3¾ şöÆz±~ú½êû\r3Ğ„åEp(.<g;£áÅ-Ğ¸œÃ†)ş¤YñØpOatí<¡…‹´©iÃÊãˆ+{¯tÜò­Te÷T¹DsS‘\r`Èc•=W¦>áÌ¾ÃWÇ±9:¢rrô˜x<lÜ$7N„˜ã¯u²ÌWƒ†ëv±€\\°<ÕdÔf© œ­JÕi¤«¾¢øø¥Cƒ£ã¨ük#ÊR«àN\r0ìó¡Œ¹›ö·szâw´À2Z0Jdö°æğ%1\\q°ÀxocÍ[¹ƒ©]É†#Í¤7\'%ô’õ~ªGK°9}¬oËrÍSã&it>!z›³¶\0?fØ?y_ÊÈd¹k2º]n·ËÎånšv¼Ä¬õ¾è4w¦_œb/Ÿ®LåËÒı¾$bgG&½NîV(æ¡âêx°üû…&\\¸\0<‘\rØ¨ƒØ©öR¡<I§šÉ\nñÅÎ¦òŸEhÉØ§\\?Ş1My€Ü.`.ó#\rWÚÀ6›öÉmE&\Z÷ÛÑ£m§\r<˜nÍétm¯€±°“™¼‰.¸\0•{Ú*xnÒÁI¦P–”+Sp£¼v¶R:¨±ôO©Paj¿)”\r<¬É†¡)ñUñœñ&\Zú›Ç;\'¶×«9Íä,°ÈÀLe	­f@ÇÑ•í¢)Úï[åÍ`Ó†Ô}u§7	úU}VÖ÷8²ö u„î<u„C‚Ú‹|²HÕÊëˆ“ç­ˆ˜à+ÑƒØÓ;‡¬°‡ \"hoÜ“)kXEï^KvÎ€myÙuÌ#›ô·yÑvAÜxŸ‡Æşf†k2İbÿ÷b“´İiòu09«¿Å×m¬•‹Sİxº:^¸4÷&¶«5°ÉC/\náÔÏú®ZºßFÆ‘‘…¬FØ»ÉqH\\&zwc\Z	®3‰©t¾= )ÈTG©2F.	ì=q¥KÀò0OcL¤û>)OğÈ¾]HûåÂpŒ^»V}wØ1/•uñG÷TÀµ\r&+²—ªG\râvŠ¼i<Óqöƒ°I]é‹\'3†ã82c*§994fG+Ab…ÄW¥Š¤±*Y)ŠBhÏ±mè^¥ÀŠ“ŒğÜZÎ¨pn4\rb#ÀÊÂ4}b™*±JD–Æ—\"·öƒM\0–‰çt/ÛòÈ+íy(–+±áx‡“+œÚÀ^j	CkvüdB>Üœ†@\'­à9=Â9ÛÄ¦‘Ì?>qKkéqÖ”eS7Å’ÇÎÆ1òRçmxšFå²^ÌÜeiÇáSˆ]Pó´OøÁaÈVËVfZvkÅaIxÄ×åÛÙgÂİí…’Ô\\İ,ÊN7aã’r·£\Z­ßh_î¼4÷&RLø.¢y$]N]Ò@Üûgõ}\rJ1*ùÔgí;±ùÜHß¶Ìe0DŠ.•¢Gƒ _xCëø:…jâ¡\\$f˜cúHc§7ûnå}Ñˆ%ªí¦À«˜•#s}€Ñ¤^lºÍ¿QãeÆàÆ„íjœëî½Zça²¨’llSZS©U•à4ÅF\n´À”pûCc`ÎA]\rKe[›Ş&2\"âû„çYÎZº ê©ƒ‚tè¾:I•7ÒÁ„knŒ¯S·ÌV„;÷\Z¯7âñ÷ú¥©†®GÒÍ\r&nçvÛ\nCê³ÔĞí.™]7$d7´mæ³åFUïğßrŒj¹w#<Ï7äºa ßuÄ9¯ø \"½ÅqĞ RÇÍ—ï2dÀ¶ßÁ\rëµµÎ<jİñìß<¾YŒ÷F»çÛÚ…ä„÷bÈ•pl³ÌúŠ±ìßD÷[£\nömÕæÔš\"Xš…û‘zE4šÛ2ÚÊ†Â9£pòÒı¾0òQ–a¯†¯ˆ50xğ3-á³g#Åß­1¯·Š©<ja;‚å“g6	ÃĞI™¨ìš}¢í¿ßÓeXÂ…Çö$\n‹c_jãw`~#¶	79 ˆbNAäÀO‡BßöDb‘%[Ò<Ã,\"Û2”Â›Ö2õ¯7±dƒù´­Á.$âGá5káßz\ZO¶°<øáîÜzjlşf\'â(Ø‘e½ÃË7=‡Ld,xÓNËîMGX£§½[÷îZxÈXQlÃ²äW:K•K\Z,O‰‡İ‡\nù”Ì†£ó5—›ß5ßY[¿¡#N*™¥/mlŞoÈ ;ÓÇFeQfĞz\'N**‘°¨cËw!…ÕP¹ìø…÷ISDœàtªN÷yx~´>Trßåû‰Ùu>Ô(\Z\\,¯ôJÈ³_3ÌŞÚ¶yå>K3o?™Y$õƒ²\rZZÏl=æXÇ½\'ñ¹‰­ÎH©‹”´ªy¿FŠ[Ø/Ñ²øP|®Ö3·zD!.ri¦áÀ.ß@SCÚ³ÙÕ]®¯aD6’¡›î1Èñ¹¹–ğp#Ï¦Ú™†ƒgØê‘‘ç]Ë`©ƒÆÊA\\º²øÏìĞòÆ.‚™üi¡dÍ•îÒßFåÆô÷ø~µ»\\›ª-$´=ÍMÜõ\Z>3LÑ®fcÚ4_ĞY4Nªë9½†FiK_\nö4LÎGĞG2‚:èİªJ®ùY:i¬ş7SÌÌyMŸ\r÷·Õ&³†ËY°\0ßøÎ\\…2İíøíÔ_×Z“å§Ö6ršózl6×ÕÃöí Ç6Û¥\Z”\re¼ç	‡†ˆA®Bc‡s·n†Æ@;äµã+¿Íë¼ÃÂxÁ¸d½OÌ«s#ÿz/-5GÄU·^Ï\r…ëe§Yõ’/Í¼])\r•5\"e­Ç0€-öMj(¦#KwıÂh;dˆFX³®n~HÊ#îå‡&ŸmG¤aèù>8›=_òÔ÷zû·M¯j=lœ²¶¯Cİm_—f³] eã´ò¸Ğ<WĞXgã%6#Ğ‘ù[úhµÀt\\d	ƒğd:Ñ	íñU6†ÍãŸÀ Ø|ıj£j\"½K˜Óe vë¸4†¬íõºf÷¶Ù\"wÏ£ÚŸ=ë±1Œ‘S[‘hcT·($§T®D÷¹ñ“ôX6Œ¦£|wú×YtèH¼í&KPD¤ã…Z™Náñ>\rQßeÍÉ¼YB‡2Í,Ï]?–ö:Óšƒèz½İ\Zà¨õì…äßİŒ(ÈXk‹­ŸĞ±å[‰’×”Y¦—yA}ø½àp9SV»]Ğ“…ŞÌ?ópû‰ÊK·ıÎivÕ6ë§w*¯{‰ =pY^Ñ·Í¤´¾4çv%‰¡R›´£|Y‘éˆF>1K÷ûò(­ªJXÍ‹wôÇ\ZóĞ%ûû\Z~ºæ¦Åw/éòz>b1Aİú\ZÂ¤%æslï,Àa¯j0§rbD·:¼Á§P#a	…ÖË¦ÜÉçÉ‡&©zU°\02iS\'[Ø2ı{dƒ™ÂXÈÆàMcoÍãdê£B»¡NíiS;—\Z‡µòÊ»$*¶9¬ÙìI~&ckÜ¨Õ/ì&™>‚w™î€İn¢’dBo[øï±]\0†ãôš@×g9KßED;ğ<Ï^<¢F ?ÓÈMD²Zba`í…¶<4’µ,N#èñ8^°€¨ó¨=weÓñ7Wkqı4p©ƒsb›`ÒÙ’³Îö.»Ë²/ù¢§Ğ»g\ZæÊ*<p;¯ô6C‹ùåÛ)ãÎiğvo<s—½ÍéZ0¾4Ûv%c“1ÎgP£e<Ê:ƒ {é~_‰ÇaâÒÉ×û¸:‹Èí½[P¼¾D¡Ş³ë{@(z‘W\'ÏnsßÍ(äÎQ=\rÍ¶;SU^ŸEú›\r™ñ2@aúŞ—á{‘Wè ÆÁØ»é±üÑöô—Õ6JÅ&ÈĞ!WyS³p„\\u¡+²i\r1³âê~ò3ÆsÎ\ræ{:]VkŒ(úsºÁİOÄI£ˆ8ÂèäÖè—V:uhê¤ôˆK»«ÅP”×o°ö°ÃNµ[WÍEñø^Úw/ET‚Aåq|3×†2Ù¯ı2«Òæ(ºÑ-#‹÷éİì­ ˜02„·v×Bt½!–¬—õÚqç4\0ãºa\ZV˜ŸŒEAXò-EİeŸğ¬À;ì%^{Z‘®ùñ)y};İ¡`|³ßZ˜\n¸”4·wı\"Æy#ÚmÍ{bĞ–î÷ERôuÔiyÊœ†iÛê‰(oBÑNt^‡¬ãæVHÙ(EÚã¥Şa· Ö†î¯ónÏA§\\÷4ÅAÔĞî 7jå¡ÈGex#l¡Í¨<X-±TZ7Ş\nxLPƒ·Nˆ{XMªİˆ®5¼Ì”Œy¯^<?œ	V•4l6q2[08hÙ\"R±wSQ1U5VÔ$íML\rÆ2rêÔsÒ|¦!Ø—@–”Ã™­ˆÖÏ.ßˆäé|N÷©qÜ*§GêÂâ\0­m/ØI ]–Li‡•®V¡»óS‰:€.mŒâKÊ¾5ÑíbŒ8^&múwåG\ZšªeŠ`^TƒÈ¤t¶‰Ù-L-Û—i¶º‘W0~÷Ê¤¿íAEî\'ãÏÑ¶uÓœeæW€¹„µÏncÎZ÷9OëÉ	­_ÎRîÔE‚Õâ&®¢·H!œ·-g~_\"‘9mU‚ÑL)¹`kñAÑÃîÑÂÉ©Q¡fú \r$¸JÁóq~wŠ€Ô0Á\01‹ÚÄJDd±#)Ğ’Ğ+_ñcƒ©©V„ÎHßsÄíºĞşh°Š|D#¨B•‡ÉğÄè1ïB×—ûIjµÂÜDh¿”L¹*àÈ~íä\0¸L’€Pµ–¯Ä/ªşá’™80ÅÁbşW]3af®W[gÿÍ\rù/0ñâ;9?Ş¹Ÿ]ÆïV´è‰\"GkõL3rR—Ø:u—ê1ùª2’®¹›NîÖxx÷¬¤\0ÉØ¯V¡½š½•çÆUÛ%Ñ¬Û—=¿}»Q=ŞØc|‡²sŞN-œs2æ\\ÚÍVm·×wã®æºÇøí´¦Û¢ïË\nî¤şÜ¾¿li¶m\'\0Ø¼${O\r¿ãÜ`Ìºšòà¥û}‘äğBğ)™o¥ïö&\Z…svo!O2Ãœ]¤”†ˆJx ‰¤ù8¿;§Oı…ˆ÷,ŒŠ£ÀC£C\ZÙ·¼š³qàÕ>Hx¾É¬>€D\\TlTCĞ¨w >p«Ã©c-S¾nÄêû®¶âùoGnO~¡õoT+‰B…!)<ßyøZR„´¯	Q%ÒÈÙİü|&FˆØÖU‡$tß­øpà1gò‘àÂ€5?RjÀó.²§º1™UC¼||áfÖsçÚşãß…RÙPÁ3Éb7¾ÒkšÓèÙò’ŸâÉO×’,`‹‚jb¤ıìØ‘+£9[ıí[N{š4&ğn÷=pı`µK©/İşôÅŸşÁçêçÅçŞy÷y÷ïëéóöûR-dìÒÍº4;ş{k“ÑõBèRp¾ã\'<e)^íN[ÜYjÖ’¹k>ñ|´`K÷û\"‰}A,e\\|yÅ®\ZöÜì5Xp+rv\"-ÜÂ­\r&)Å;ÏÇö}‰APË32sÆj zc²oJn«¦C›ádÍÜ™«`LÏkhŞX‹ç`Õ\'˜E¹õ+\'ÛÂFL˜¨9¥Øâü§+¹d­ìËê.”¦~‚|j±“ä8ÄÈ\"äö^«¹L7A¯\"5_\'<ÏQ+}fæN‰æWÓ”7şºÊ ß\ZÙè…á(/ ÷Q¡6P¿@ØVÙØMÍ>ÃÆ2PU•¨vKö,ĞSÅ9\'©ƒnrÄ2ÛÔ¿îÌ$>tkyh\'.t¸4§\n!ßºÄf:ßÈz×«Ùm²ë£m\Z^Ï­H¼“]¦[n\\HÉñWË,ßştÑõïÊøzàzZN{ñÁ‰lPmş®.=¾ek5¼yãícÊ>i6í~û§±/ãÉŞ,ÄƒÖ	ªYŸ¥û}‘Ä~Kô­¼XXİh4òä¦Ñàï×Â–œ$q…d:!`_Ôî9Ï â¤ù\rª‘çt‰™¢G&ï×<	â\'=Ef\ZTª‰ÉÇPk|Èª±aµzæQÍ+Ø-5¿:vSûÎê%mí	È°,›ö×ÛŠvzlæ.\r³ik•ã2^!¿Ş‡LÙFÑÇP•O\n*¬CÔ`ÕM ËìÊ¡3ùà“é\Z^÷¤*IÂ6}Ñú®>ìUS\0æKX\'¬ÆJ~÷¥n¯H`n2¬ì6/æÒNÁ„X4¶ªÈU{¶¼q˜,ÃF†O…*:,\'½šİ¦Šµİ&iæÛùË¸õ=ÚšòæÄ·Ãéºóùİ”?q‚û-R~×¯ŸrÙÏmİõ~ß_^OW=]rš–ÛTò­¼Û«zNö\'G0şokß¦›¨Ó—K(,E&ŸH&}ÒtšÚ\rÕãŠa	¦`é~_$eÓæâÓIJ(@HOáPpU»·Pq±™„aï b£ÙÖ»ùx~\0%ñ|‚Z±ƒ½k>ç¶m‡[Ï…“)ç+fÏohÈ7ã+´c¹(£0½rpó8hTe¡d‡ê¨³„åÚş³©Bj‹8]Õ0’äé”É;‹.knö\\wsk	MÎ‹¥ñŒPO›q¯â—Ğ®aÃ\nÿÚÙZ<²:t9¶ŠáûŠ74ØL“ÅHÆ½oÕFF— \\µÅ’,˜pøøfw$cÉ,°õÇg†­\n2a––Ó›\0¡šJ»*Wbîz5»AÏµÜ¢ï+‚6ˆû¢ïp‰É•ÓÉ˜ò&„î>¢òğä‰0gıüóÚ¬.çÄTxMyã¯§)8\'<ËØYëÃ·î†ë»#$Ï·@‡,ùTâ.» _ÂkÂÀJ“)¦š–LµÑ ¸UÀø™YïK!—9õQx#	5·Üs²ïÿU‡_¬gÙ&ró1ü02L^³üOJA“M¤\0Ê#{´Í\nL[¦æÁ¼ºvÊüº_ÛNFd¦ ¶Ú/˜WÜÆ®ˆ—i^#\ZŒí‰ü§–OíIç»ÄPtgr_ræB½Œã[¦}ö;Ü£IYğ,‚ÁEã“¤­@ÆZ}9\rÃ»P\ZÉ,Š“Ø%½\nÀ¡’|‘5;GƒëKû1}Ü°ÛĞÔŒ\rBÅJ\ZCåFi“Ôt¯åòó›êIú\Z´²B¥ê™™œÜ¹Ù:-ãƒ%šL‰¸è¢LŞªW³”¬Ñ~ˆĞ˜Wjë+³“\"ğ=>˜C/éìØ¼§¿\'à	¬ÑùuHaS%yÍƒ‡L÷k¼Ïsî»\\ûˆÁçî§ª·í:ŞHsS9ğøB±u¸MyKEúÜÜ8’À_°ÓmæÜÔ×¨U3x–Öûü‰¹İ¶Ï†óbáÑ2gaÆ~4nİDÆ¿»\05½IÎÓ¹Êé~ØHõv‰9ñ§ıÚfù}÷:WæêoøÔ‘ù˜|$>eÍRm² ONÆë¶¹yK©-ÄgÁØ±=o+`—…¥\' °cS¯mM‚	³5Lå†o[°J!.%Ù})¯~)ö!şƒ…vû4¬îE > œ./¬Úä,#w¸ÆU|ŠvÏIQ]&`MD»yŞæV™)BÈõÆÖ€#Ï}©9¾Íİ	ZkõZ•Ñš\Zh¶º9€Z01«ÎÈ#ºÏ‹/N…¸µ8Ü$¦›9îç×j‡İÏ<ˆkkêœ—ãjñ’yŸ›„+Ä~‹²_Äğjîp…ä¯Ã…óõHSÕvÁ5çEÜv¯òÆRn{·°É¹Iˆ¿ÛÛüb÷\n¦m)Ë¢tğ³ó‰˜Ç«–î÷åÑn½¼¸QÍ$é°ïH9©P¼éÎ±ç“s!O´>¤…§¡Vò×ã œ$Ñô#{³—W¥«Zº¬ã@2®qùYÆiOä9;Ç…ú«S^”Q\n¬8¸m%*¶ù8«éyÖõé§îìƒHÀÄ¹8d—K&sL~Zm³FÇ	(ÛÚ]Ú@æÑÄÉ¸İ‹8\0ˆÆmE¿êğ…ˆ¥NG„!;€Kö)Œ^íğÅNq@7‡†¢WÛMZ‹’kÇrjmÎ²Æt3åÿjŠ‚±Åäşs(Û³Ùk×lÖ‹8p‰—a€×jàt¶ßÁËÂpöymy1÷K‘›—nkJ=Àğ¹YÁ{Éöh”øuã’ïç×şôòë|ıòpsĞâ¾•äu;?âîòÊ0IvÓ\rçãÃñ”ÆiŞ;«¬Y*Š‚1_ºß—GÅüòZ¿M`~uñˆÍÍ@X?«˜ë ÄmDE&™1W}­æ‹´Ó×31ü\"D3u0ª‰Ä<4dføìÈî1Fxg§Â¤İÔüMÖ;Ä3³2ùRÉGŒƒrKAu\r„dÈœ9=¤mq¸)’—!(QY°2¬a4Wrdw§öqÉg™­QRÛ8=+mŒšÿh+!;T³\\ñÌÏ9\Zº­JQµé\Zø¦Ş~¸Á«õ­½ÚŸQÆ]Š1teäbÈ8 ı,~­W£g!- ùm#é÷=¾Íİ©´m¥ù‚Èó8¬ª‰ÑrfÊ¸é9Ùe¤·ÆÚH=¹\n¨U¢½š/¾Ö/ùöãPyNğ\\ö3×c¾[«ĞC*¼¾ï{>>øE4]ÙŸ†jp>(WÁ—˜İ¦#˜ø©ƒ>»­Ë´¿-Ë«İ9ÔwËİ_ämñø î$¢¶ñ@Õt†9³TüïÛğs&ãP4äÀÀçièÍ¾‰o¸_#Å”@Ÿè‹ßÒŒU\r0(	2·$‘yò¡×<4Ô}ˆÒpb_¯\Z[bb×1hÅ¥×0^G4lòh«g¦bè†×†Êß˜itVm©ÈåÄ®G8ó1\"j:‹Âr M<L®M[’^²Á	)Û¼ BW´	!n¼|JVw$P1õM4.jpÒÓÜ4Ÿv@^ÌägTŠNƒ™( ­ïYUÒŸ¥<Œ]±ˆ¢9 z|›»“	i(ju)`sò.†´{›â´àsW`¢Ø×µ8ÄDà\'¸{˜Ğ¾¼\\KÄ“*tDîu3sóîïéüù˜Pv#‡]Çã†ä¸;ş—uÃk´]¹¼Yìµ)—ÏÑÇZ¹]*ÉÃËµ(Æ”4€èXåâé\nnZnqútÛ9ºß‹¼ïãÀ.³Ş“l—zÏÍ,ì3B1Ş°sÑfgô)q Ù1µ<Î~ÌÄ Š$“\0xĞWe$ğ†@„gqc<‹‚)BH4tïVµc”ŸŒ+±HçŒ‡¦›2Õá•)N@<…E>7‡ñÁ«CcÈƒĞ%ªìØø9x;AË£ÊØuĞ¬¤˜ö|Ÿí-1l—\'5ab³MOÉç¾D*ÍàJ9PuJ%!š)Ğ‚½ÙÅ!M„< ã7ü\\\0G2µ•Lt÷d©Ìõ_‡±_Z9¦öçø6w§Ô6j5ÚQ\ZÉn8fvk¶zùM2vn°ÒÓ|x0ûÀákµ|2\ZÑ´Âp»°<yˆ@û‘´ÏÄ€ØR\nåßX[•P^VÁ{8h†[À>İaB~9[Áaµd±J×İØÒêt¬}‡ö|xŞ;–Gõä¬²·âÿÌ\Zk¶t¿/ŒĞ9‚÷ç€-‡f:ÛÕ‰”\ZÉ#Mé8xâÎáÒÅ«fâö‘Ä1„¡ïè|ë\n-˜ÂppgÄÍ-\n3C›ä\'6aPB´v¶å…¢Œ™YAsŞ6ª¬;0æi¡şF^ÃØm9MÎ~˜\"]Å™†ÉØ:áƒ°¬—äùç¬a “–l®¯Èë5††ôz-·½¨aR2ßà¸DïD3¢ıwÂ\\®İ»yRÜŸœ­–¼~\\û»T•ƒ½•ù4g%VÚf1ÚÄü¶,NÖò’™(wOjfõƒ¸ömöŠŞM˜3K2–èB¸¤W³3j-Ù®ë¯ââëß×3\rå2àö\rb®4}¦îÓJm_>­¿&<®©½F@]¼ˆß?ˆÈÚâú™öRÛ¾ÔZÊË\rv–wéK¾¹¿ÌÑ·mÏ™#nkmŠ¿ˆ®0*ì\\ÔPœSxglÆÏ–À¹øhMœ¹\rù¢wÖW ­÷GÜBM	·|¨=8C\"«“‡.»€ÕA¬	Ó–;.—Ô$¬ÖØìº~j«êùªçãhrXî]·E˜ä78´™\'Ã¨›’YcŞÊLK¦]äƒÍ±NO9\\xÜ=M3\0æ—±sˆ@fD÷í…	ŠêíTB]Dl^D€\'^Ù6˜;pyƒj«kª@MÚT³yb>÷%§¿ÎV>8!Ç¡a½F–î×<@.Á4-6ÃŠ	ÂFóBd‘:ğ“ËRÓõ«)3\\Òñ\rƒš&Ñ\r•ºá}øÌïãv¶¨1^\"`6Ä\n0øî™gê<‚nf·Sl~oÀ²V3ÏkÈ?‰ëÄçAÜXCşAÌ8gûAéø‹d™6\"nY)ïçaY÷ãßî]n­¿¦W€Ålxò±Ë½áå_Í½Úâqºª{Ç;’QX0YÍˆW¨ıI]Ã…OÚŸ˜\\ŒQ#=Í¥b¶Ô-®\ZÓ{ç½Ú©~ªéñÙİ»œ¼ÍÍ;Å–+(óÌşcVÂêæÍî¸Iº¨¼øô46pã¢#U?éf+ $	×5†FN€iÒ†ğïv‰%ó5ı»e!@hí¾é~3c<˜éÌ¯ŠYi°L¯$İKR\"ouº&-øi#ñ|èM •îL&×y=5¹fÚYÁÏ™ZjHŠå3·	lµĞÂì¯qÁîM£™Ø™Ì„J!VŞÎü4¦LfÔõo\r3İ|Ô¹ŠŸW^šœŒŠéí†³Şèş“HôjöŠ†FÅ»Œ	~&©‹½SÛÒUØ\Zd±3¼Ş+¼;­9ÏòÔ0Oğ{¼?=b¾ók¼^j&°LeáqıuÜu\\vE+µè¯íµò	À¼ÿÛºou‡pİ\'m—4tóäx¼±zü[wDyî›§ñt®†âkRäkÖ÷Ì,¨µÇs–îúÅ:t\nŸlÉñ½4~ökg@IFH0HX‘	Ç§)C\\].Ÿ‰áGGD¡£ÑèpT n´ºİç#;5I¸\r‰\Z£8|Y$cTá*SDAO·wÆÈçJRHo—ãdÍx\Z*‰¢ùYtÔ8§B°CBÖ„	IĞ¨ÛFÅì é^’0á˜ô”%·– ´-Xùl]a#¼7’…B,ó}öÚZ–tD<¬‘™´¸Œš3¡8ÙrĞ:n²]\rÈ¯µ7æd˜nº`7•öUh<óGÿZÎ\'=[+!	ëøW¤½°8¾ÜÒN—÷jv|4Ï6Ó<j\'Ôw›1}Nµ,»¾«°× ã˜›¦7|éÛÁîBÑ¸¹›,÷û°!ŠïŞqÄË!13İ/(I] 7¤¼áZÙìF›¢óÊñ¸l|†5İiz:\"qZîàë´ì¼	º-~Ÿàöóñ¾]îKÑpµ`27lÁÔPsÁÑÈ{HœŒª÷±Šç0üê®\\Ñ¡1¹	ƒƒSƒh™‘Ø@±ê$31üHÂN•`2‰6Ëàb\0É¼¢ù– z;5iíÄ#ğ9Älé>uŞËoXf\Zšü¾ˆ\\Î¢F{Ü¯¤­´Bøé¬BEz½CªGÓSdı$¯UŠt˜²Ñ<×XºA¬/ØÓ˜£4J¤\"¡ÿ²r9î„™år£>n\\LvÛ‹èå\r“Æü†Dƒ¦‰].¨HRÌûÏq‘Ê-XjÒ¥¢Õ™(oé¿Ğ*6#lı’z9«ç@¸À¤í;‚¶ŠÆ<<Ô†7(AôÍ¶Ùs6IëØÍnáMÆùŞaİ¢\\×hgÅáöş²»šn^{÷^GØ+oìª€^ŸıÂå£_è(P§ÕßaG5³ü5İ0ì½Ö7Ó]/½¥ËíOw3#Ä½#NA½Ãûñü]îK©i\njö|£füšb®ÈBûP¬úÒ]¿bî\rr€lœ]ˆD˜ñµãõkµœ#\nL\Zšb\n÷`j¹xc\\K_ÇPCÍICwj’ğY&Ø]¬«q¥-w\"Ê‰<„K[DB{Í÷,-A)’–ø]}a‘vs4÷(qì~Â™ÌS»5;¢³p=ƒÿ*ˆó5ÑhÔ/Ñt˜!fá©üOw0Dğl‰òé^3A\r‹Í›äêô|îN^1Ùè¡…ÏLåäãß½Ú–ÖõYù!½ã	á&0ï·WyiúùIåÓäîÏÉSRÊá n,-í÷j©>sÚ­ÙÑƒ°$HÜ‹şZã¢:Ü½š­ÄåÜ÷17ä¸ãƒq—Ñ¾ÓŸegImüµ{ÇaIµ.²¾¼vµßX:.•áa¶Okúƒ\n·°Ã®æ´¯Z¨Wï‹¸c]w{_qZˆsĞpNÍ†û„5â÷æÚğK@ÜoL4.±½·lvR8ê}VO×óY «ÊÔDgÑ~8a¥i)	(BğH-»K|‰ø”\r}óç`øñÔ\nc\"ÿÉiZŒ ş¸·+¢ğ˜¢ıvğV´Û==µíã\0.\"~Çx‰D:“XS¡—ÀyÑU¼V#Òô‰ÒlËÆı£µFÌ“3Êg®!)³%I÷’äMÊ@¤Ö2‘vg—Lì>EQ*²™,“ê\rZVş==Ÿ»t9Û­\nşkÔ™£Is÷h›µ®ÖÊ¡*¥–S_T~íÃÌÈF5t-G,:’Yé~\rƒZò@qNŠ`?XAg¤®g³Õ\"EQ¡=I_4@’õSÑzôj¶’E¸Z@Îoï’7M§=”sBú`j—È6/øÆĞûöšK»Ëh˜×FÈ«Í17ÉqNvÓ÷Z.Î+¾Ş÷½{§¹Íû²MMxŠ…m¶Ú®oÁjz£wÌ’_Àvå`u£9â¼:#gt¥ÏüO—îú	Ä¿{ø&—˜,¡ş\Z¬÷¾Mõˆ@ÃÑÌÅ[ø ®3äÁç`øñõ]ºV4(>İ\\JÔ¯•(åsqñ ‘á.ŒiFh¥ãºàqK-ÍLQ*8»äøfj°ÃW»4/;È³Iéí/<\rÆbx™·à›Ñ£s¥tİI‹™Y<7	%d•XÒ¢7„“DRd\Zyz>w\'ÒJË+‡>pŒÔú4ßõAˆ»é•â Ó³h³’LigfV¨MjY--\Zß–ó½µC©‰³jÎ~D€ÍJ÷Y¯ø¬ú—¼­‚èL,ÉÃ¢§5À¦rnÿRìûˆ©\r\0¿‹ùëdW´;İ¦ì\\}Ï“à¯ÌÖlµ/Ç·z½ÆAß%£+µq“sÁãq-y8Ú;.è†úmÙ\'<+\Zodº=¬ÎwvóˆÓ{uvÊãÒØp”Ï)ÖÃY†³7ægEÂUÅ™š€A®ÕñÈŞM\r¤)w3I;Ti:\0ü‘Ë9¸İ…¨;5j•¥¡zxšËMï:LW4sîË_c;s57­\rÃ”EÅ\"l«ìA|a#ÆĞ¿ì[‰ZÃùÀ^\nªInCV&9)×f® û–u¤P	ŸÍ|ZÀ¡}K%OF<(\"3\\MêŠ·Óš6l€É<¥9lÕšÔ¶ÉëCìğÒv‘&;³	ç«1ÜÈT†L@í×¶†‡%İ§§Û©T²$¨†˜i¦&’É·€Sƒ+Û¸ßà9È7[»Ãê–Œµ2hçfçÊnæ@¦“í|×9 z5[)©X›«Í5=íÅii¶Ç·©ZÇö<ÜuGÜ¡\\ßÕõzú7,ßÆ‡áë¶?E$nÒßTjoå÷w—`q{ß7tóêìF>Úî.n÷O›ŞÓ}ïl\nË\r¸¶¡÷êì\në= G’hërß¤Nîy¾öüLÈEæS\"Ãi8²ş¨9¤µ¶h0óş º aã$*Ü¹;·{Qh<æ¿¼.pHÆÌ\"Š}JŒ‹—‡ú\'†ñJ§ÜÑË›R‡0²­å‡Ù­H6é’N8>]è“øjëÜ\0­â%î‘/_ßßÄQM±(Ë†‹õwğ é^’R±±ã¨¥lÌî3Iˆi•OÊ[5ŸkÇ»sz>w§«5¨¡ 54²7Ê–½{å¸C“Â.#HRœ{awĞÖ.ÌôòlIZ¢»jL´õÚã<mQsí2ı¬*pµg³½Ù	ÚZrQ‡–ß™ì\0k7ö¿Lèøéªïÿè§ßùèzßi‚û^’éò>²™rÜ\\jËáØ/ĞÆ„5ïÖ_—\rĞb­¸TªÇbuŞê<ìpN˜½3è¡´İñLkÑ¸ÎvHkä²a‘¸>úüË×b¢z¢*ÖfÈí×xíü@ÜÛ¨²ô‘ó+rêhÔhCl@>ÈWœÿâ¤$øqñò6Ud¼áKwn÷¢_ùšgêúÖ5enw†šCÀšî‰ò£ÙŞ0¦+S~c†¸p6\"Y°­R»\0ç\Z¾Xñ³Ñ¾A\rilR)#…¸áûfµlß*/$²Ñµ\ZŠóÖ ŒV°èŞåmzh¬ĞgÀRï¬m0)ÅxmIˆöA_X¥IÓ9§çswJ¦[q\"‹»®¡*£³œ…]û{¶34jÙ‹3€g½­î2š™„»	W,mÅ~]]?\rÓñ\rƒ¼¾XUB‰Úéü#evà¨fï²_¨Pé\")Ö…`[f¶ğ¦ğû>ÍhÓ¢ïŠ‘ïöµ?şÏ?ÿK¿ñ¶µ_úÊÂ+Ã\Z@ş•y\nW§÷^Á\r‹£_ğ.g±x;,ÍŞömÚr-.÷°Ñ÷ô/±Ûï}WsøõĞøq£3ÍYĞym»:{:gk]öêìäc¶,2ÙˆAPW•z‰ÏÕ¤/Nu;p¸áCa]¥wxÖá\rFi)ö_*ñ‚+±5Ù{Óm»3¼·aİáŸô7šñHq¾;5ÉF *9y3êMŠ±²;!4òl¸WŞ¦İh­‘šÕ‰19¯©yĞ‹(ºV°ƒ›i‡Í®Ô»\Zİï°±=ô\"júô9Hº—¤+hÖ˜D‰R¥‹†‘üšpØ±HúÙà­¢<ài*‡({ï‰2œøÏĞ…0ÙBu2ÿªrVçj7\"~jB³û)İ%>¼uVWm&±wÏAÁB³u]	ÎÒ¨¾;Îˆ>vkv¸-:\ZjX3„®Fô¯íÕl¥‡#ş5x9.µk®?úÑOå‹¿u<õ/ÿêo–dw#uî×Œ×¼ùC÷÷‚}*87-ä¦¤3áhºœRÛö)•s^Aºä»C\n¾C´><}·Wø^.N©íÆ}^ú\rä?Äİ°ÌíØxì€uta´uçmÒ\'ï(lV7ÜvñdD©¶z>x`ƒÑ¦Á	wÇA¿èòJ „h¢/·;3ÙØ‘–™øÜ…R¬’;ñ#´| ‚ü”·\rè5\"„Úàdl\\Ñ°<±/4ÏßàÊÚø¨A‹†r6e¨O7Ù[xÖSl«Ë!½[ZØef¨â%$`g<g•‘Q¸`¦ùiLH>1Ü´ÁœŒR*ª$ºÖÔØRé¬\rİîÄ\nbY .‰1Ù‡ì†Óg¶¹˜$u¬2¦Ş´VEC”Ôu¯ò«8‘ŸÎÄµv²\'<çîğtÂñ\rƒÌ ƒ Ss.&!sJùø½s³­/kÿëTnIæ4XæıÿµYï;ÁÎ¿ûõ?ı¹_øX¼ÇüÜ/|ñ«_ÿS¬-`dK(>Ó±¿áÆ´5•Ó*o¨§/!o¯ğÜàó¯)»ËË·µªOlRûV°ğ‡6İ„ê¦+ÉbKfüé„^=‰ªÉUûÌÓõEG¢OlÚ´báƒõˆöméŞŸ±—¯ÿ*3MÔÁ	åÌh\'®çÀ6û´o?\n›Ö³ÅøáÚö¥/Ã;R…9ÈóÃXõªÈ;:2—µ[“BHC‹×ÑA|İh’FQµÍBì¸•ôHx4V\\C;ôÅ—»ÛÆ;“U#Ïéä˜ï6ÅŞ¡:£èÆ=®(VÔˆ1FãdÒ/Ğê6œ‹J–”ŠŒ“Aö\\<òJXU²{B².\"?gHQÂJ©‰ó¦FÎ¡áÄáV²\'mQdŒæµÈt„	ºãùÙ›à#V†±9&\rŒ]!Koğxîü×ÀÁÍ©ªcê×l;g¾K“ôK˜0yXô‚×ÉWŒc¢Y3ÔOÿ~ç£øåëß³	Lø•/~åû?ú©¯!§}ÒæO¿cg3T7(³çğ\"k:®»c\rùkI¦ë‘páğqSÒ¼[,dV@¥¡ù½Ş¶øü%\'¾c¾[‘;íÒÖ«§3Qk‚—ÜŠÚ%²Qƒ\\4\"Fï|ÑÓR­+”ÔŠ/ğ‰8®¶j\ZüÛ\\Æš§-ÏÚY|=îv¦‘À;\rbAÂÊVKC\rrHaÏ÷q¯ùA¨å˜x¢ó!(u£&cíØñ%!û¸ËÔÃü4Üƒ|\"Eï[ƒpÏâÌ~ÂSÚĞVm§8õé)8T´ƒ|1*1ùP\Z5o¯Ğ\"\Z\nºYÙØÌqëd¦jq[ÈÃ2•¸#¬ñùŒ”¸İ€-í½fMÊÄÀ½›§m«‚Ä 0Ğ~/´?ØÚ>üÌlšu»E#tÿ:bchğ™×’U³Á¾¨#ˆÚºQ©ƒ†•wd]«Ê­Y&1SaÓ]U£ƒc&ÌkŞ=Üø÷)1]ß—ı\\ìığï¾ú\'5µíç»ğö´¯ıñ†=ÖÜ[Àø•ÜS:vÖ.”¨™_‡MpIˆ—Âo†Ïqk5ª<wõê!“>5CW‚ëµ/>èÂƒ‚Pç{£5RÛÙ2íô=àßfôıôéÒÍùğNcG8¨‘êõZ€]šgDy¼c¡3ó’Øx†Âƒpğğf§€(&%]ĞöZtÀ÷9c1>”Â\0‹\'oxş®M¢,¶×høâ\rM­¾ÛğNh\ZŠÆÂµ0zšÆö<.°ñ¤Vü²Í”e÷¼|oáá¸ı†E¨Ås8EG´mqb•iÈéo•o³0FqHlŒœÓl@’œ˜Éİ‰‡ƒ8&“x¢°­Qæ¹—ÍA\ZgF³İœ#µ®9is~¶½ü9<}xZÃxÔéÇ7xÂ6çşQ]mÆı.ïÜì(E\ZYÓm—Ÿ™Çèb3c&+Û¾ÓÜaõıÏ¾õòã_ø7ÿùjbRB››ßøò‹şşÿ…gíğÎî¾9n·U¸ÃÚ/\0Ç<u>+è–špÎĞ	´â[ŞÓ\r{›wa¯­N\0²}[·n¤vksÙIÕº®·™î.İœ•86`K~ƒÚ¶Bâ«VsÚ‡Ë\"bûjƒ4ØáV.˜’5Rlüo3ÎÜ£åÿV$œºH(z†.3à8Š8BCpG| `Iy²3x\Zƒ,~§?1™lŞÜL~_‰Mèès\"œˆON¾Ä7ø#Ï\nÁqÖ¶-ƒò‰0û—µÁd¦à†×¬ş¥=4¬A»qÈ®©\'æs_²ÆœFªÍ)ùE±„ÃÂGV•W­iÈUª\rvRkigp‚}øé-y±Z›3¹úSèÎñ\rƒŒqkh¥#kL’ÉÕÎÍ±Ï¡Ö¿à­}›3å«»yµ‡òò¯/ıÖ©¤ù®±/»ùùıëßúÎÇœLÏĞıôéÕÓ×¶Æ›v×JoW\rn2Ñ´”[05l®ï“\rÒ!i®iîéH/æ‚ö Aİ8&©cÚZ_Õmè²›AßÓ÷^İœ®ÌÜ¬æÂœÓ4âï³f3kMbTÃü¹õ~jTƒÀê‹q|!B8¶åR—µŠ_Bl)Sš^ÆD\ZKùå™FoÂ{ÉEæe²šGÍÇ{X¹ÍÑÅMò¡`æßAxÜ›¸iX)|Æ£sædÅÆA@â„cTÀÅÒİİƒÄ8¢aãFÎ`…ÑµÀÆ6Îe«†`‹R\'ÊÂ&ZŒó)ùÜxŒ€0\n¦¾.³KŞA<ÿİ¯y³(ü´cª3cÒêE7~‘n5ÒZ{üÈDœ|ƒç 8EÚGÒ¢pg\rö•¨íÍ6:î\Zş­«NÔvYİµÙD¶ºû.@ïRéıÇßúŞ»—q—£ú£=W3øøÓ—¾òG´j;>èî<´/âõØŒ©	¿àZj¼= 6Û …œuÙ{1u@ÓĞİÉœŞ	>ìÂ›Úö›ÓªmÍ_»æÇw¿ö\"rÜo 2w6júîí›sˆzO—¸ôĞâxbSÌÙf°ç†ËXiwÃã·wÂÚ -¥%Õ£ë±©„ó¸j„éÈÖdWtƒŞ•àãHOÍ xEn•y£:ÏÊá÷˜PC³)©ÖôgÉ¡Cv1#UÛŸou•†ĞÖ©À`NUØ6â&G‚IUùüµC–Ú)ùÜ—ŒÓÏd˜ìÏt«¼ŒÛâË÷l!ó_RÀâOM”	ík–‡.9nÛ\r²©xù	«,88¾Ás·V„âXD”gC£ÄÁõk6\rD2Qª\0Îç$V9¡W³‰²7sE8üııô7¾üÂàÛµÛ2\"ÿÓÿü¿eÉnx•X×]Êy\'4~q¶Kmóşän5÷#šÆ]Í?¨ÕéŒÇõ×\n·ËA·®<?§ca9ìf¾ø÷%Ë½Ë÷lóP9#ıÇguéæ¬dŒvşY5>–9)2/’š›ŒQL¶·`Oçd µÜªKU¿y4uAÇ½=\0í¼Ù;×Hà\r¯Î€ kEsĞI‰`\röhUğàvÙT*l™(Ú××îô*dTXÑŠîƒa¼a„ÃÁ¥{¼+]i.‚g”pg3½]²•?\rÍbì¬¦87ÑÛ¶& mª·:%«;’é2?°k+yÈbby¾áŞ-´süú:Î–<XB\ZĞë¸íœ•¥*x|‚…rå´s^Ñ»fæ²ˆÿ¡_aÖšbQñ³á¶=›­¦Úœ8Ü  ~¸ñn³]º^{úûõ?ù/ÿê¿ÈÌÏ&ÂGë[b òs¿øÅ÷Õ?ÑÓûwì)×“ÓòêAÇTµ\0d¸œ*À_T°Ìk½ãmK¦ßÆë¸§=ÏCÂ]×¡wÎq·2İ\0Ÿ€¹ìx12®òŞ;N+»/q¿ÑujVµ£}@_c_Ó/Õ¸Aömi,@œ…!øÃÁ[tµõ#uşy\r¿sµÇvÁxzÙé›lED”¦û±;]Ø>Uß¡V¡›„yıâĞ³îQ\nfVYöF9}ô!™•@<mV¿Ç”ë¾š…\Z¥l)“ÈòåŒ”H¦›@p½‹±´_a5nâ†ó}M/*ZªİfòD,áåQÎãÉŒp¼]‹^è ñCø÷ØF‚İ3òv[eÉ™î¾êNß²”-¹F2İ‘Î™Nõ$r‚!+‡C7Àî5Ø\n¾Uåğh3_-×\\	¯àŞğ˜Æ;ôj6Qú^°ÇUÛÿËKU›ì¡™Âj8,Ë_úÕßüèG?ml¤Ö«›œÚˆ;î+P6×u¿ø\\Ìe›rù·`çÒ)K.À<Y9™÷˜[ïÂŸÚmæ©uërıË´¿\rKÅM‘y²c[—nÎMF‹³¹Ğhñ@Sœ¾È»¼DK³á¤TÜb\Z¼±÷¤ -‚²Ü{‚âguèxÀ¨k“Ş	âãÖF	9çHàMDÜÔ÷(á‡›h\rÒ=t·0¬{µj·ápëÑĞPIXffv÷ï7UuØÈG‰œ\0çid)¹éßùŠ;R;×äÀ¥ã¥¦iWµjHÔˆé»®÷‘”8|Í?\r«;’š¾LÉYÃX”¢Ú(wÚŞVË#Pk6«tq\0nZÒy¸j`Kªî» •jAãñ\rƒ _uf†bNæ3uÜ;ÚùU	l[\n¢“á£°0„½šMDUÜe÷W_ş•*ªLFU]ãåé’õ‹_üúŸü—¬¼¼W7!•<-©†¬÷‹§Ş{k\"ûE½ĞìaëÉ§Òh¸¹3½×Ç™*÷î9n\\¬}+êFğé>)§õà/Íùğªîé×.İ<±j ¹®«~&3eê¯#Æ)×éæà‹§—fÃ‰È;ëÔËãÄfËûp.W´iÇwÄBS)á˜_ê²İm5Øêã<¥sì¤#›kÒ,ï[ÁéÜ¨\0ìÜ*†ÏfÊkcFêŠ2t\n|øw¾tÎÄO\nBºF!ë‡ïR¦7(&Î…ìdğ2W¶T˜ø<Ü©=Æ9MQ¤ºlMA©l¹@Äİ‚3\Zo¯åœ8.Ö0º“ig/¾s\ruTÀb/ì|)ìæz§«¾ÇÈMY¤²J™Ó ¥“ài†].UÒD×’íÄ¼\0ïÙlz„ÆU‰à‹³x§w,iZù;ıÃ/ÿêoªAÖªË ]7,ªÚšbW7×¿òÅßúş~ªo\nëÕMZ©[“Ì«	ëx¼~cV:…ÏˆÜ›Cò|iák½Ãôåxş˜÷bÛA˜d·%cîÓâ/Ãökí\Zõ——‘ã~#ĞI\"]5IWë[³\0r]cÈ¥™1#±×hÆ[ü)w‘	¥{6pUïî$¡ÑöA/f„3»´yj`õDÉ4+ò“#Ø½½ªJQ¬T‡hÄ*ì4¬¾†™zñ^kî|„q\Z²Æ\Zœ R›“>3kì€¶Q¤­‘‰¦Ïie<sK“(Œ§9´ğ«T)äúEæ:\0½‚»OÃğ.ìİ&„Êgd$‹\'çØNÎµ±0 ¹NçHi¡P‘Ì(ŸÇ36ÎˆºÕ@EÉ8ğ<Ê°…3çIª!NäsÃhºLĞ,¾Œ­STöÜ2$kR¢q@\0ÛOØ,=½ü«€Ü/}åî¿–p„°şkT~µ†BE4|~ş—~ı¿õ½jè‰¸_0°5ùå°°Úì`¦k®#Ü.)o»¼^®-ïƒã«õê/>w<¼5%å\\émwE{‰in„Ø·š§]Ú¡O0üvZ<~|OF‰öÖ,ıP8Äï9J=`ösµùG’–\nd\'/é5šßh©ôZ™6ìÕ£+ŠCÚNŒ\ZÇA.I8Ó«Ùİ‰;;‰±Ìç¨ÅuRÁ@Á=\ZÆÏÊ£\\õà6\n%‡ÈëùÑÙ’Qp’Ÿ\r‰J\ZŒñ«r-[ºß-\"üÓV6c¥}´b,sD{qÃ{±Màsèönháœı•}Y4q}ÁU˜ëƒÉ:cgÌA5¤Çxö,.Ã¨Uîqì°¾½Î“¨#ë2‚,TñA±ÁÔ*QSıëæôoğÄnˆ¸AÖ@ßÅ	Ö¯©Œ]Í£Z¿8ÂZøz$·íiôj6QÙ™ü[ßùøşÍ¿İŞf‰[ÌG+‚RÅdMüõ/ÿÁs²û¾kUù„ÌXø\r•‡òoDÙ˜°ÆK°SU9•¿ìw ;¿ \0ÜäüñËñü!ÌW^û÷yÙœxV®7ßº\\>Ç÷ñ”d|)8i™}ç)cwF1	èãÒüèI\rÖiM/sÒ‡j>tqCĞ¹S›èÅˆSqSZ/öüÌ³r-ÿ¢^iƒá\\Ì—ÙXg†÷DÜ%ô2™>ÎÊ…9LÙ¥‘ØÓù˜|™xàÌ\Z	CcÇ(‰C0b” .ÅpÉÒ]÷”õ÷Š„3hkYd‘Ë³p‰ãn\"a”Z­·\rãFn›`4™y»?¨†ÆÄ‘5Â³ë¦ñ¶¶Yó?‰é“Áò9úNë¸WešlB¦¬;â¦uïé†Ç7x²ØÓ@¶8fµ’F\\˜¹êªt¥IÕ1,B6ÌS{‚}éÛl¢éå_`<bc,óh‹ë:éKöëæ&Şü-ğÿÖËŸöNïÕM³y¸Ûl²Ş%+MoÁÆ·ic…yHdc¡8\'¾ñ=_q/t€Ş´H<zÑ…Eş-`I¦òË¼>NóàÍšs^7ïÒÇSÚ±ÜD¤’Ì5\Z™¥ùÑ|Leç6[¾ÛX¡«¥¬ˆ«á{×®™©~’HdoØ³ ”V¹0XÇİt%0/M>%ªÒñ…”!åT2M!) ‘	gj´y>ğ«ÿÎÄäÃÈ¡©±VkÉV¶sĞˆ8ñYxL)QfI\\èòôåôımSlêª5†¬d‘-,½!Ì–]Û›!,ö¡š:ŸhYläÜl?†H2YŞ6×Q¿@ÎyÚÁ:,g®(şà×q,LÖ‡o•Î1Ú±Û_´vh0	¹Kï€jI3µÜØÙó¤GË˜G7‡^	CS§ªÈÆnÍŞEr(&}Yc—7ÚµşÍ&úƒÿó?şü/ı†±{ê_b/b°±¾f5Ï>l.œÅXñ¥¯üa×·”y\'K­MÙy}WWL^Ã¯º¶ºæ£ñæY¿Æs\0¶»æÑİJÖ»‹Ú)f³Ù¸àîR¾#fW$¾’û\"·S\n6S¢ïùˆÂêÁø3ş:o°• ¤Ç8ÍÌí«Õs4®&8¯¶öïàz\n†eèùKæ\rÓøêš¾ôm|G2ÜÖÎŠg¡phûøA|ğs;1\\qOQ`’¤M@8›äé•­[ËºãT2¬}˜«S~–Òè¾\Z}‘úĞéÀÕ!›©_‡Q{8¼v n(ÉíjğÁ¦f\r°’fş­&â\\­_fI\Ze0†‰dŠ$×Ë©Ö¿lùÖ\\äƒ;aØ>L&®{£öC\"{üëõ¹„B;iV44Ê8ÙåjÉÂÜKÇw4ÑùUÓS\'ëÕÄ˜¸^Íæ^XIÓ–8°œL™¦sA™kÆ²™*Ø?ÿ¯½W7?%àüB ·]¦ó×!7måÉ:qz—7cp·u[ä?Õs¦;taÑï}ó/ã‚k¿ï™ûÉ¤°wÛ‡w;·{ùñı‡ÿö›ÿÇèÒÇ+¾õ’’6Jğà–W—R³ı ¥³±ñQçXmWb|ÀYàŒ‡\Z\"Ë7¶ÉıQƒ7•:±L¯-$HÅYBƒH©š0gnØ%eÕÆ„Ó¿‡4ÏˆV\"T*~î³Ò›„ûÌÁä(F;¾È¾7l\Z±c©ñğ†ÁÊ¥FRÕÇË/X8\'ÍJ1\ZÍkÈ—\r¨ˆ´õG»·Pd®¢¿0dş4?½0ç£o±´ÉM?êL \n\'şöi¶8+Ôx·00İ4|ıŞÇ­íÙHĞ&³ÊÇ_éäN¼L¯[Ã\"+Õ)E’ÛºæC Šúğ\r¿uQF6£Õ«ÙÜ‹6ÃYsİÎoÖùÂ`¥¦Û—n‘îÕÍ˜¿ö;›UØ””´ÀjØ\0Í¤¡_ĞSø®O~ÕÓ,úîÂ¢/üÎí—¿ögÿş?}Äø7{çWhË2ğx<M—¿d0ş»·ßùµ¯~ãmÛºôñôTÅ;³T™\'Šö!„¸ÕOùW7íÎ9¿¨ƒ(rƒ²–Ó4½£¹;÷¯cˆ±lÆõÏW·¢Ê†Ø$qoLè°=?ï¡7eiÄ‡’Ö¨+I\"Šƒ«ş˜Éúİ†+uP¸Ù+{$\Z3ÛŠ,­h\ròV5(œé‹RâX‡Ï!í,Ö/Ë¦¹±<Çyd!º![İÿ‡ğ•Â›2Â|v‹LM.ê·ÓÏ`Xn‡1EøI~SeìœÌ`ÊÒ\\;šJ§†Q\\@¸çQ--ÔvªBÅÓHw`«|/fY]¢#;y«óüé»\\Ÿ»ÀSFg`\n,qg¡l\0Yr¸ïŞ\\D[Ñ³Ù°ÚÅ)8ùSU\n@”RAß½Z®½ fãâ,5İN­²3ÎtoÒ«›S)x@ÊXøÍë©5÷-™hÎzB	î˜\r§7nÎVç^\0è5ízŸ.,z‹jß~ş×¯~ó÷¿ùİ]Öa34ÿªn\\Më»}~|ºí¿ÿ‹ïùëş¶IŸÿ/q¿ÙZİ_ê¤Ê¨6MFsæˆì\'+cŸ;i]Î7pRŸ6€ÉbÎ–½ò.x–.ÛŒ›mŒySí\r÷Â‰=s.t¡àq„ùR ¹ì¯_m:ı{HYüyOE‘¢ÒÊğ÷ÈÌï‘Ô6FI‰b²V$À›kVÌCã´(<Ã( «q¾11§ÍÄœª}‹é—1z*üV>£¬Ã\Z§‘Ã™‡48†Ä\né)~çiíĞMpóÕ‡tßòß*Wi(ê	+½<Êp\'GÆN¦@C/`^ññ;‡+ÓMjI0éi§Á2âŒ0‡d:(»Éíñ\rƒ®j¡N£_ÙäUöBõ	äuœ®ìÖì-ö*ª<Z;§ÊZc\"¥^-‡^ŠQ“¥*²P”\ZÏc·2³Üq(Ã­zuÓ¢f»hÚ.—®»‹kF›îğ‚3Ôüh,S‡Õb“pK·x!oïÖq?Ûwˆûó¿ıŞ¾…ºï’İ5\'í4Ù·=7(;ÉqÿŞ7şò-ö‚Û—¸ßdg¬Ó\\£}°áS;e«6ğükw“³?|~“İbnrŞZk¦&à“e2È3õ½˜_3\0ª½ãUTœ\"c}œ©Ç“õ8-OAŒ_¸	Üë»ƒµPœRl<³NPÄÂ\'/¦\"x×¦§íšåáï\rC„¶î¨Öšı™Q$Â,İqåv-»Ã¡€ìÂ1¤]ã²ŠÔA84‡;“ŞpÃÛÌzzÍlDƒëV3Eó_úÄNW1³&ËĞZúîFD“+ìĞ+TU®JwÂô¯6^Õ5±\'âf¾±Ù©üuÃç“Ç!›~:¾ÁsÈ››-I£&‹İÄ’¦õm6M†¬$1m\Z¯‰\ZY](Z¯–C/&Õöáºê{fÙÔh5”yQ+ğ3tYçïæ&xË{ )4vK­CÙ9ìj>İÄ\0ÿ˜Ë†,ùôhÍkÇÏŸ\' A÷¯}õoa¯K@cÁùôâì¼Ôs\\Î—ÚşÚŸ}ajÉÜ~û÷ø>.KN¹¬•¾fƒ€1m\'Ú¬İD¢6qº²pJ¶d+J¼%iØ\"²Z…!Û<‚½[D¯=~ƒÅİGËÏelä;¬œ(—¤³óuäHjº¢J7¸Æ=	üá$­å’©’m`§ùSQÕ¾ÜŞŞMhOè]CMGŒf5ëğÙ9Ybcß°`l1Ê\nr’\\%Í6}	Íè;(¾ã69ZÂkÌBshÀ¶w;¼ñ•óñ\"c \nİ\nMÀzÙ8†;Èö.‰bã¯£#¾¡d’O~œ–AG‚\0H©^j\0±Ä…çî¸VaÓs€´1ªfæßi½ÿ½Ìñ\rƒ¢<”±5#®ËÄ;›-—tk¶6íÆŠ“øİ*WĞ/éøIªÊ£F¨4â™ÆĞp8³&CF±\"ØÕ^İŒ9æ£%‹¿³Û—”sÆn¢û¢Çõã„Çã=\'À^ëá…b\0Ò;mTşŒµÿ>}yD»+»ÿâû¦ê;Kyo{/Xzäñïï}ó»Ï©í0ğ~ î7öËEß)ØH§³fÏ§0¦¨¶çÕ›[Ô™	dœ3­†%²+6r\rGŠË†„£3_ÕtIoÇÑb‹;Ç¢aIè]^DôìdgíË1z‡âJQ½§u7p	ùßÃ÷E£Y¬f£œM.‰pvdx«ƒ¥Ô;v¶ë/&\Z:)Z‘\n©`ÎC1¤µ\n¬\rqVå*±±‡º,MŒFkO‘c¦á¢€NPÈ®ğ”V¨œnd‡Ö’4W´.ãâ»	ê“\"k	‰oüÁúÉÏÉ,I\'b´‡RFµ¦°™<Z\'ËPlçŞAlĞgÖ,4OfÅ++oğd†	Ä˜C<7îaĞoº+”>Ú×Æ‡yˆjp-ï2+…@¾‘ÀDÙÖ6ß©\\÷a !”Š×ßßB±8<ä»eÿp)8Ïv?£Ÿlú»ÜŸ2ï´FÅç¼ˆ;”µãs{m›öù\0´È}úòk¿û·@˜ßÿµVÛ¥ÜáZÚıiÕöçÃ£êjF—n.NW´û®¼6Z\r8×ÍZÿ%.FÖXiÌ™µÍ¸B¾Òıc›1¦2–ÜX•d=‹<«ØŸ\rŞ–ıcfÁvÍ.mß˜’€ğ%Å0{îîLqŒL‘¼z\r]aÖ¦R{â®àN=EÅ:X\"ä&Co«ª1+4P•·Í[_“\\Ùc9d#Æ(§>5ËÂã¬G¢tÆÂ ½M“haz65ÚO™r4>…g#m<fm£3AÇ÷%‰ü­7T§0ó?~@Ñ¡ˆ$w¦7O«¥Ô\'&êÀ|¦â€²ª–…¨~úiÕµk>6ˆŒ\0©#V„› 7|>¡Gƒº±²A\"‘}\"ŸÉ¶œ«Ÿ5] ñ\nC`Ò±³\r¶L|îßrŠñT5ô×Dê¤øÇØÌ}3Q„V\"·¾©>\nu@‡ * @ò2¢å{WÖañuİ‡¼–sÛd·®Ş²IšdÏÍ	R»NÏŠéoZ^®êÂœšÚş‚¸K]÷»ƒÿû»d÷GOUâ²1\ZìgşøIwE³‹¸ŸRÛìÇÏ{†¸ßĞ.‚ä[‰K2³\Z ùbrç¯UCéÑƒkh\ZîiÂNäÜªîsĞ…–!±\'dCÌÍ‘?1HV/ï˜\n©@¾¹ÂN C‚Y]ñt=:Œ°µUÚmõ;I\rtØc›ªÏ\"áq\0­àğnw#=e90ØµSÜ#«M!jÕ‰;ÛleœÖ¡ñQ\0ßªŒáOf!‰mÒ’Ä\nÕg=ßáğ>Z	Ï U\"ëb¦†ùª’ò¤³1)A¥éÌ\0ÖXR<MlY…ipÿ¸ÍŞp™œÈøøbr£ø:Ô`1)¢•]Ækêfb±§<«¦¢îûÓ:•Ác;¡ˆŒXbxåxR€q¬™\\§d’-ÊUG;áßT·§ıèèl@ü²¡_ƒñsÂ#L“€{W{Ä½Uxp |­&Å´Q•B÷Å£_+ÿÎÖ´5&ae7®éNsß1	vN³çk8,ô¦íÓÃp.kÇÂò¾Uår?|şL¥İO°÷×¾úº¹ì‡Æ	nMsO»šÃªíÿôQ]µı”Ú¸»&»ß#Äı&ZÍÔÀjy^±¢d`Ñr’İğ4³Él¾œÿ…H 	©I©!×†~SìöTo«QVoÔ‰Kkµ-0²qU\"I\ZÇsİCõÙ	<ãU“i¨ğ‰³Uê\nk­Jû®¢‹j+ºiÜ¥?ó¨X°O‘°êlø—‹±£}ë6Ì19Rå¡Ùec(Ä@MöGJÙkÏs,AÇË…É¸¨Õ½¾ÒĞ\rÏÊ’=Œæİ¾rËÚLš¨œÁ<z>[;`0Zé‘²éÌX/òl\\C˜•­,BQwÒEÎWu0ÌüPÑ˜gäYb7ñ>Çë]{BZfºo:•ö\ZªCƒ­šÈ¤„aµÎuè Ç><¾ÁsÈ|Ü·Üx1é©Í&Ÿq‡²%4†º?œÆ¾@Œ~Ç¾¯h¬UTÄ)?}§.Ä	(,$ ‘KÜuÖ»Ì:†şÆ¹Jr^½+¾,fã~ã›ÍĞtG¸_2¨@ŞbÿøˆºÓ\ZŞs†Wƒ•ÔöxA÷‡µàüñËóÊîRa²Õ\\Lş’ÁxXîı¼!ùÓ»¶ËöhŸmøíùß‡EÜD¬¹Æ”ícèKp23˜\Z-lzOµTn;—èt0ö“iºM¼„p‡‡À­˜°Ñeê o_¬–³“³ËƒíBt1ÀË¬±N­ÄÛN·êíHFæEŞ@x ËÂ®0ÜpŸ~Cœ\"k+f¤İ*ä8Q³8®nèòf§Éà3ˆ·B’ÕE§8œ¦<¨;È™cüobb8Œˆ²dOfµiñ³¢*Ëí\"9±= ç‰\r¬¼u.@%Í\r´±óxZáÀñCÆ#u¥@™e>jŠ)ºÈ4(®M3îÃmñq0F’X¯Ú-”c¥Íæ=c×ã˜Wôè ¨©)üÇ!3Ø¡ÁUÈÙ>T­¡¾€jx\rÊ,Şñ\rƒ¼v³O-kŒÌ^ò	=[Î–0‰™é`£§öª¾J.L=——„ Îy²6×ËÍd‚=¿4[-CayôJ¥ı½+Ô’Å\\È-À¶nwŞ×VËrlyË6­\n8ooåÎıÖz1‡ÒÊZÚ·V{·y|g÷”Ñ¾Íá6§¶ë»¶á¡eáöÜ~ÿJÊ‰H1seA§¶ßèÅÌœöDæc@Zœ\0pÑqU˜ĞârlêZšíŒ\ZÃFDº^ÀW\Zî9“Î|RéQ	{lŞçÄ½Ût b0	(ûk½êŠ&½…iİ\Z¬²ª¢•Ä™¬t*\Zf¨ºuÓ†iDaÕbfĞz4ÓèÄø„1#Ygd>oÜŒbfjü<C2Wbâ˜9Î>o¶ƒ_ş¤‚a¥nGQ´3ìª\nÕeÈü º0ŠÜÛÂLµí±kóvãd#\n9+Ìª«–%£ošê†fÒPNÊ‡ÏŒƒøÌ%,?VŒ³~±^ôi°±f\r?\"Z¿‘Á\rÚÏ<Û™mw±\'¦ÃÉUì8ÏÊ>şíØr¯ÔFÌšêŒéóËù\ZCsD-ÚêBÔ÷4>áG$½ €ˆ†Gl\\;QsË‘ãEoæŠ{‘…uÈ,ú6åèG¿Æ[e·y	¹«c7kÉ_\0\0İ/Áí vº¤ú©ÚüóÓ;»ohìSÛá½`¿ÿÍï>¥¶¿€móÄçUäï-â~ƒWÛA…Ÿ[Û1Z RM+\'ğÉ[Xš—&ã\Z0†s•!àm³ÉUí¾X˜àD¯…tC±½Kˆ…Êù®€$Ğ€Fó›ç\Z	¼Á=”šeQ7:¾ê:Ù£I Ñ§Í8^ÆÁÉ4Î‡Ü4æv ×&@tŞÛxğkĞ²fĞëEN›Á‚—D;ø¸>C@rcúhB)Õ)ÖÄí°HWyó01ßV>XuaXø¹òÊX2qû4^p§Ë\'v¯ŒhòğÊŒZC3‘sFÕİ@«~±L³Ã­Â N‚Úº	+™6Nµ?~Ÿi¤ ñv¼ÂD„‹1ò½_Ü vhp>Lñ¡ÅP¤#•Ì2µ2»šLnİl‘±­“AŒ*Ü¹åÄy£d÷Œå4V˜/4…%ø\\–máXè‚±$Pø¤lwöGª/Äæ X%Ï]ë]\0IÙê©`p–\\6IğŠ¯C¡ø–Îj¾ûEøB“\0¥aÒŒ×ñı7®–’ò©º;®ò~Lvó÷Ÿ¶1ymÅàåß’ÚÖg}!Àù÷xÛ4Kh«¥@”ü¯š5£Aã¹8P£‚,6ã\0ŒuÃßÃm¹Ş¬\Z¹h3\"U¤ïíDXí,ÛbPTº‰ˆ¥•PÙ˜îğÓRİÜJ*I	\"–|ÇÚ”¡ŒU×á&Ål	°Înµ?V£edïf«â¼Má°{“iD[“±ÊZ˜ˆnlRÏQÈxËÖ£‰Ú¸›»\r=ˆ¨WÃ+xÙ–\0®D~Ü­D¶^BWÅA©#(7\\ŸÈzpãUÙö4æ\"Êƒº×»Œ9:\"U¹ÀDç£9’å–í›_ağ¯-ƒÅà]>ã0©Ç™8ÀÎ‡`ğhNaQ¯·Ò—Ôfj¸\'íxlùñ\rƒÂ4ÈMN)ŸaÙCí¬X…;÷o|‹ùÉ¿t	´ÓL,JIá*#ejÕ$Ïµı NÕG›ÆóÁ<\0ûsãû®–ÆwúÛq°¸ö[ò×J˜Ì©p|ƒv¬—Wl”2Ğş\0ş\r­H¼>ès½ØŞàö”_›–Ó;»¿PWv¤;Ã÷—?oHN»¢ıvDñ¼aZùÛ«§çLl¯P}nB5”[E‹²¡\0,£z\Zİ}VCÙyá¹Ø¨z°ŞÜL#¬±¿›pa4G•ÇŞáùK~Œ´Áæk4.6²3¤kæÏ‚=mS*·Y§¢¾$\"üÄï=[^T)ÓS™97ıBÙ6ğÁòA~2YBnŒ\rdÚ¼D_Å2¤i`Xks é38Ü¬~öFz	ø®ƒv‚Ì‹3¿\r.eèo-C³C#‘›ò\\Ã6ˆúÔk8?Ò[e¶¨•´Ğ<n‘ÿ*ízÇXÈq×¿®L…ÎÇÍ5ùÁÒ \"înhœ¸®š}€¼û6¦L¿@f•‘%y\ZÓ\rÎ¤ÂPÕµ¤\0)ª?ôıøÏA»h‰‘Ôª¯_Ó½ñ0×”µF\'j¥ı†¡Pûã€°˜Ye¾wî|Û¼\rÆ_ø ÷1}‘z¼5·öxúaµyÿWxáWÀÔp«ÁDH^¾S!:-*§Wá=_‡szñä	í~> àŠÁ%ã<­° ıCØÆ\\¶P{—ÚşÚŸ}^2ÚñŸ^ïÿÛïáÎi¥v\0u|[q Ã³Œeœ©Ü1V;FTŸ(eŠ3l™XXãÍÉ’¨‹¯Q½pEm{<¾ô°\'«¹;Øwu1áR¿“»twSÂQ¾†ñJzE‚GÒ®VáäùÚoâí­š.Ü¸`»”(×;UÛ¢‰k‘(\r!¶}°C¼İp¼{îãM|ññ»}eE} ÚÆúš^ZOş55¹F`×œ\0¨8ièNv²­ä™\"—!Yi*9P£#‹l	<`«ÕÁqçÒ2h!A×zóhåî2tí£y„nšt|è¸/Kp#Ş¡Á¬S7Ì@ÒYU#±¶¢õñ\rƒØµeI­™Pcî€!}_`+\'Ğ)ÚAÛfÖ8‘g±ÛÙ8ıZ™wÓZVÔ»7©åäşäİÉ3 î×ˆšëÚm÷R0ª ZÖtÓ«´ÍÛ´¹bü…@{[Óî^CÖ‹\'²R»Béÿ¿½¯k‘­¹Îû)öO˜pˆ‰1dÚ²Š Šû½sdP°06±ƒ\rŒ‚Cô¢‹,_æ:ğÕíD:3»j=µ»«jwï9ëa8Ìéé]{Õ×Zë©µªªÆ e›ƒÍ_wv/ÁîBºË®mO·a³ÙCıó{·ªé›\0;ifÆò»TOZCCÜùNcÊKBiÕèŠ;ÛT©âŒ/*œd°ÚÏêaTM—Ğ-30µÚ‰Æœˆ‰Û»ºM˜!½~Ï;}¾2Ü˜¹x¼\'ƒ’Ó1¹8Mh½ÈÔ®~¡1ÚaR»ïÀ—W<¨à3;•Â+fRß¸Zx©–×²h¶fÌ±¹xhÜ~ŠªĞìøÔÉ•¤áV˜OÅĞ¿âXŸ¥v#m¼üé]ÖÑ­Ö‚¨­ŒV)Z¦Nñ¯¨<uvÜcƒÄ†ÒßÏQXªk!²‰ækWí\Zµ1|ş­}‰íë/Û¬J/¤v××‰V§îàç¾¬¢0JÃö=Õò³¹ü‡#ÌÜ;çyÓ”°(ÆP™Ú•\nZò-yÔz‘*Ğü(j9­`1mQš;T5|{lºyÅ\Z†şæ—È‚…q3ç¥[³q³vëÌóVN;pópƒ¢N¬|«ÖxÀ¤nˆ/?>Ñf\Z§.{º?Éİ/»¶ğkpİØãë{cÊz<¢üÙÇmáñ¢ó`5¡›êâiUG±İ?ü±¨Óâ4o©#µhQ]8k«¥²OMdµexjïN®\0É[İªÕ×´5\rÿî«:fpZ+CVOÇª}ír1ã«X«ÇâÙÅ±öT¥¿úıÚÌ,økF¼†w!3·”ã]çç\\tŞy]hFfÊ©XF¬İR§ŸOS	j©\r¨İd5\0õB|¼1´BQøRÛ×bG.ÔeKu)+Ã•ÆªYR.ëÔRmœt`n\Z3T?Î‹ÖxÓ	WEâH^¾IÓùªbF8Îµe·9¹Ê¤€ÆÁG›™­æÄ\\étû‰2´.t³Æ¢@ö+Y:¦¾\rÇ§Ãöò£3yE„®ÄQWÿådšË²¾Çß´]õ˜uiÈ™‡âõ{(Í$tY	éœmû‹CÉxIwøEwdÿÄ¦ë¿¿Ü3ÀaG\ZLü›øj¬yßx·D·]$\ZR¾Kø;l\0‡ÿş÷_ıßÿùoÿï·ì›BØ‘}+—·,şå—­*ûæpZ¡U#¥\\õ#«P\\VuĞ4ºj•V\nõpşh|œ’`›éLñÙ¦psé^NÇ¶ëKjÖjşX =x{Õ/\0Ëù“~\nHsö&Ç5®±ò€İá¾›S‘Èœ¢©L§™˜-ı ù\'şj`gÙ­.r=e+{¥§Şw2åc(4i°×gëI¿q»ÙÑ¸êâÚY§ƒ„°VÉ1 @\rXŸhLü¢]Öá±yW]nG—L|Gå\ZÓ(a»%óQÌV(§9ZHÂ…¨–™»[wĞøƒíÔ4„|ªû68vœŒØ;ğKœG±/ÂV8÷µy/5ü¯üzĞ8KU)ª.¥@üô,b´Ô~„Z4,uœÚqòŞS­Tw	Úßë%Ò|¨&äÂ¥ôê4<uä7(ÀçwmÛ_ºš6Y×üğ¯!âù2Ç©<ZsÔM€Ûç±#ß¿À5ÜÏKJy¹ó+¤”ó]–/›£Õu/v;#ıÑN?Ù°²o¤O…¨XÃ8—7µ‰b‰~Úì¼=#J)*áûğÒcã½ê±¨Æ`Íæ¼V‘dïÎôpí­êRô§Xq;BöÛy&ØI³\rbÛÄ2t¶YìÊ^¼:jİ´wÔ7hyİÖ³mOF$Ñ8İ¢„£÷Uô-ÜXußÅ\\/níÖŒ ‰`§	)ÏûØDü8¼Wëçr{æ\Z‘èÚÎq„Ğ[BÇµ–Œ¢\'cJÀ¥»l\0ÜnÚeú\'iŸØ&Y¨4¯ÔÉÀnıˆ!öÙ#,acĞr—U^†Án}Q[¬ú˜ÅÑh.Kà¯õ÷â«l pK-Ä.€¾k÷o«ğù¼À—€Ñ9a-ñDí¬Ú´xù¹Tâ¤>~€£Å+Ò.¹ğüPÉûÚL·­T‚ïuPUái·Œx˜à±åOû{äl¯X\\(²mÛ_¸ùZwYÜõ2/ŒASz9<õ5Æ¸C<ı7ÂÄùöíÀëé8µ\rÛ¡píx‚Y9*œ7VÇmİö±%o¼(·ã¹ól_lJ¹…è\r7Ë–ÿú”cq®dÉ‹	,eáâ×V‘(³\Z ’¯ƒDoØ]1®ãíOÂiÂĞt¾Ëâ¿äKó#Áå»QOàyí $·†Ğj–`õŒ…*£âZÑ¹•²^ğˆ|ÎØV\ZâÜúYïæusÅÃµ9NğØìWlgÓjXU8ØÎì¹f7¯æ¦hKš¤ÖMµn˜­w¢\'<´Fw_§Ë†q°ù“K}›É$ÒÂœì!íà²4¥k\nír™ë¸´„eD]‹Ñœ‰fS„ÑÈƒV[•G¾ŒÕ¥Ì\rnOCÙÓkGê¿Yì[Dª\r4åiùÅ+\"­~cÆ]Ğõ*/µéÖ9*»Œ¾R«j¬ş.£¨JîşÊ1èöêë·å¯Æõ…IôúaYhøE/¤U\"±åxô×5<^~1Ç]ğºt:ƒãğ·ËÛÄ¸7l„wöíÈ‘Mğš>“¿BQ‘¡—Ò „Ç˜¸şúß\rëû>ĞÔ«Ÿ«\nj[=¤½ø‰×rËlå˜—jª¨Lìfd>¿Qû¶RÒÚİ0»Œˆà=Iû{Ë8ªùÃVÚ»ºM¬Ùwµt÷26Äp4§Æç»vÕŒ`\rÊÃÕdùy€ï×j:ôÛÕS*#ŠÆ•–C¯¶_»úì£%ÁƒNŠ \'§gc³ÜÅ”]Ô~+Y\"ÔS–kÄ†8u>8WA•É`Â1Ÿ¡EN\rª›_ŸŒàŠ¨ŸlÚ_\\b7³|r—µMË‡”¶ú²A$Îy™Ë{·ısáãØ*K}}R\n5õ†û¸[j6¶öÒ¹òRüÌÂàøò…y/•òl.|Õ¦ä8h¯QRòÜ•²¤ĞzªjQªVQPİ›–7ü)ŒŸø!®ã—U`ÅµgÑ pÔ¿S/M‘h¤Ş\'2Ã5;O&7<ú÷¿ÑİÂ¶mØ[¶C†¹ü	rˆkÃ	çñsş7Ü&ùê1Î^ÿºm•ß\rtŠ5iÛşp0Í8(%ˆ“òüµ¨‘3*ò7(UËíô•º]{g™jç•¤íkĞØŞÙ»õ¬r5jKu`Ø\rN0bRÅCØ½‚\\£åCŞô±>ÚK[±â`CO{Ğ—ŠxØìnJÊD~ùÓm»4—O&´¦µ°NVGN«×*…¥Å£*V5–lŒCÉHu¤&Ú©×6Î\Z×¡q\"PH#@‹á÷xñœL¾Œ;Y¿<öŞ]Ñ\\æeÒì£X¨MJ[m~r\Z¹I÷‹•1\Z8¦ñ”dØ,ŸÌ|	¸z!¨M¡õµc~e.\\úì–¢¦™è*NÔ=ÖzùZ™éœ~©µÖÒâ»t#ÒÊİî\\ íÔÀ[;PiN‰ÌÎáçyq‰şŠ×^Û4o9f\rY¶òâpÑXı=°FyæáæKdüóÏ¶u×À´îİIã6?sËãnÖÖ@vû 6\nvo[ë÷‡8ÑšÉäúaËDÚ3rÃç’øİVAMÕäàÖZ¢:$ªâPÛìİ!#X¬¹jHı¹bPØQ1½PxÜŞÕmB¬FõgPåÕAkì › ûù®Á¬³ËWjGö´YA*TkƒDh6Q9Gœòm²¶È¼÷5R±Ú€®\rëªBcĞŒªúNsÄ$÷•¼2\nÚYO}Ÿæ…-Ö1[>¹B]­.­İ04µsã\"O·~¯¸ëâoÇ¿êï4}´›Bi{·.ã®µê\ZÌÇ5í¡íF®ÈGR¯ó%ê\rÚ”\'rU\ZU]ØøğUöÓÁÙ]çW#Šºxu´ïÖ‡¢Ö”î½Œ\r5”a`ğRêòıg£Z™µIÁ\rÛN#q‚µ*KıëWÚ/Eº…J›ë°éĞòÖmİ&í¼æŸKùJğé°µxÙç/o^wŞ£-YâúI¯Üïï&r-Ãå´´JÃıÌ}Ü}0Ëkºhlµ¢.ª—Å=ò=pn²‚‰ÜõV¥œø¦[=\0áoÔº	4­¸¤ul¤©Á%ğ…ì]İ&ê¨­ÑZlá5Şğï¡ñ{m±ÛğˆŒÕ:B×‹Y4ŸP|Í-¾µ¦˜8™J?yàn¥1I­M×,ÖÕA½×)½U¨@§R\ZY£64/ÀWÏÊ*ç‹öÈ°Î¿Œízq¹Í€oX¥Öärî·ïn~êõ—½Û²‰°óB<\n‡ìŸ4¬X t¤ˆæ¦ySR»Ïz6l\ZEøò¼À—@m†]Ğ…#ÎÊ¶ãöèÛóJ•2ë\0q™~üº÷1¸¾º¼©•:!4ÑAø¼kÕÌTlœG”ßçM5ì)+²,òÈ%z\nÏ%ö›¸!ô¼<GÃMÜD·1t^#Ú1]ØzùÚ¶µÖÌğ@aÃµ¦‚ÇŒñOÄ¾—7gK«õ½\ZOß¶Ö_Ö34êì¡™…£T­˜	¨VWkk5›ú6°¶|£m\0Ò8Îòx_¨¹¸/–’÷®n<$X~±\Z<hÍÃŠfeïêV@×ë`ÙÑ\\Çf—#Øn$d|Áì6Š=ûæXK[+(Mj4#YZàÒ°}‚¡Ê=1UÕó‘¹pBO:í¡‹xê…[C£AÜÜa¥áˆ•İı¤¦–:‘ú¥9œŒÈön¹Ó(•å--dÓiØë\n^ü-}µUVù»Xß»20Ö¸Óo´M5ÅzİRWNğË…ÄëÙ¦†Ôš¶¾ƒ+Ø‡£Lm˜°Á…¦éÌ\rMá›7¶Í‚–Î©£Æ^Ë!äu	ö4>R-.ÔM…ëáfñ6m\n‡Èëáä…kÓMÜô¯ÏcÇb¿¹@>ù3™†iá›~p¬~Ç[ÀlŞx<3MJĞ8xıå·Ú¼â_ È“ä\0«‘5·\ræ¦ş•=[¯O¬f3ú°”€ú|ïæ¼¸5–vXÉ%–,&l[1‚ñgïê6áëØªi‹®ëÕÑí]]ƒ0ü‚˜c>,›ŒrıWbYéÚ¿–°wS1š^´şˆkA¾Maq¹&6é „ÔÈ-rAœeECbgA§«{v¼ õ&À\nVU.õ‹(P¥û¢+ky4MÃ[¹UšfyÌ ®ï\nÁYõÿCËœŸX²&°Õv?8KÎæò\'R…›´/Ï|ë\rªJmšø ´Û±\ZÜøùnu$İN´ášÂÚ¨h´’ÔPõ¿Ş\'Y/–O6Hß(…“?€ÅŠ}AWJ2¬L¿_¨ƒÊkŒYÃÑgDã^l³§;<û#Ñğ!ìéæÒ6¯ìƒÄ¦]‚7\\FÄ™(—ttÚå7t/%z^v|—Wo^÷/ÀY¼O.œ×ê§UÌ¬óÚh9Wš¤\nß®åÚ\r#.ªFQ·3ÛÏŞp{¾ÔÈ¬3Øõÿ_çüĞØ^ş»wuO ÊIÑ˜€6pSİƒ5Ñzá“½Ûã¼‹bıùÖ”Y™MèOŠ\neZ©8ŠVO®®§\'óÍôİõ÷áú°µU°±…a²Õ½néÄ¶îİã6i¤‹Ñ)fÌ©²m¸[é#¿%V¬øH­ä\"ÖÏ	|	4”	-….–vƒmì ²¸=÷­¬	LXs©CÔ|Ò\Z\'8–×Âl•‰5dº„Î¶ÃÍ5cPÔö]\\;ı&$›Ôîå43Î÷Ö£ÏğqÂÖ\0:¿VrÎ7>œü+—bë&nÃš]N¸¥ğ+…—;ÅJQ—¨~\"b-DÚ8übv´´Í÷¨Ïİ_÷nDâv-)ºı`‘ŠÏßân{W·Pñ¨©ôóÅÁw9Œ\ZÜºe ›ì½TŞKt\'è.ÕÜVÂ/8f>TfAA>°%nÜàÆï]ÑDâK„á§\ZJæû%ÊòÓ~£ÀúÁ¯ì}¨?¨}5dwü…æjssuÑ¬\Z]t\r¤›88Ÿr†·lkbyÜåİ\"İ¯¡ğ¯n¿|íBuáfsB¸pp¾êkSËÍ_Âó	iúör1Ù…Z q>¼Ã3‘ŠsBS^<èï]­Dâ}ÂFkÊß §^‡áG†¥\n²*K§#®Sô»ª‚­\'V½¯¼exM£kH{›H$ÎEƒÉJ†Ã¦ó:2eÍo,¥6²°L8»Á£Í ¬9»WlXqE¥½_7˜ò×!<­©ãq÷/P#¾ãæ—8™¼@×ÛÁè³x½—¥¦ùän÷7rêGkó9ç™RH$‰3ÁÎF\\áşÆÁßç‚ëŠúß{pö®k\"‘H$Ş*b<ÚìFQZMkæ¯­ı/š1ÕÊ«‹W>õ7MÓOb^¾!N¿4ãæxå÷åêÉuLê¹â1o¼ìÚ§«rMÁqŠ_Ó–pºûS»ğËµC\"‘H$ŞÜ}+.9\'„ª}¤»õ~ÑŞuM$‰Ä†š›ùêšúnÉµ9ÙØ¯¤jL[ó0“+4HˆV›³Ñ¾Áßø5Şü…[¼)ğŸ\\ºFHo•}›£Ò^ùxˆAk®¸Û‚?‘ xˆq‡kÅ¸ÀK·F\"‘H$Ş0‰¼>¹é3¬ü«¿A›ãîs_L\"‘H$¦`|8¹–­è|!Ù¸æÁ Âè51ì~“§[³=ƒ^BŞq7İß­T=†¼¯PMÛ„×$7vdÓµİÌĞ!&wg×™Gá\nï<3-‘H$çC|•¶;a)öÊ¡»ñˆãk-ø\'‰Dâ½R¸-Ñ¶¡jà.æÃâöèc³†nmhe­_Ñêq 7q×ˆv	jã%_p¤yıÚ•âÚ/àû¼äà2û§B™K¼›6_ÛB”S7i{8-\r#ãÉ¸‰D\"q.$6ıÁ¸G<†úÜòp\"¿hïº&‰DâmÃ²×>„à5-›İÙf›6çk™ y[˜+·…¹á´4ÊG¢•“Óˆ¶_Sr³ùZˆp<“<^Ñ¥ô™¾£»°Å†\rİ–òG®Ù8‰D\"‘xÓ€…ı£PæFÉ¸îw;ì’q\'‰Dbn•X¯€Ó—ù²Kù/íİ¶ƒƒ½k˜È½\Z*^ãUÖ_ËÑí¸\rüúÒ†üm<™÷k+·)í\\ã×ÊÜ•J#[7ì%n~ı&J$‰ÄÛÆ²%œî0µ¹s´¹»Eº“q\'‰Dbœõí~â½ºõÇD½5#]ïó{¦ä—ã‹ön³ß®÷¢ãÇ÷•­Æ—…k#ó57y™[³ñnîğ‰9M#éö°zDÛ’j¾o‹%‰Dâm¡x\Z°çZï^¡KWyÍ_NWæõ÷½ëšH$‰7æ¹&ãE–By¹¸’åz¦\\pÙØâmS¿˜›ïİf·><\\Îw‡¤q~øƒ|Ieš³Ñlìû•ÎÃ‰ïİl‰D\"‘xKhl@[uTâ¶8ërØİpÉ¸‰D\"1\rceìR0%_1oåkáÒ1™HØı­I_hòp;- ‡>/dù)ÒgYËÍİ‘V75¸…÷¯w×—îİf‰D\"‘xc€·!£®…İ1g÷µUæŒ;‘H$Û\0t3·œ	2-)»gc–WİWuDsÖ¢êT¬£ê{·ÜÍÂÊ%ºÈá(r—oørü‰\nÿ„ßçp9üÉ§¬ïİl‰D\"‘xc0çÃg†6¦-¬œòÏãçİaL|ïº&‰Dâ=€í‹¦WE;¥«Á+¬YSÇõNÌµ|r¹,,üìİl·wrøSaÜ!‹ÛÅ²1BİÊ?×¨·œs¥éÆpKØ÷n¹D\"‘H¼1ànëÆÖl# -Ş«nO:‰D\"‘Ø\nSr}ğöÈEŸ‰•]LÜrv›OÎ1îõüó°X½wËİ\n0{ÜDº[Ñjz&Şmo£/Ômİ”mŞ¾Dìå›{·\\\"‘H$ŞZKñ¼ÂSãBŞ>u8b|<<¾w]‰D\"ñ\0jhÖ½§ÆÍsK4~\rnÃÓ+[WFÏ”:ß»ñö‡¾òŒøñ©d}›”ï°çúÙúrúYŒ\\7_ça[bß{7^\"‘H$Ş<×>â/\Z\Zğ‚˜[[Ø‰D\"q)pĞ¹×.¶‰rX=îÀ„º‘:ßBqğæ‡5ïkïÆÛ˜§]™¯=ñÓrOw¸PLCÒæóºé›#é˜yy=|mïÆK$‰Ä›DƒJKr°oÙ=G~1ôÌ¦K$‰Ä¦0$—‚İ\Z˜¾g¢\r÷`–ÇqW8»2ñxª‰ÍÛ»ıölš~„,ñÈs)ıû6m³Ğ¤¨x7w^·2É)«|ïöK$‰Ä›D3Z­ix­c]í²¿’w£$‰Db;8&[~ğ »J,¦Ê‡Èq˜µ²Í\"€’ı/4½Ü\'Nƒéæ÷ã“?HòÀköxüW™><BéèVŒ½›0‘H$oBœÉÁ“W1ïD4/\ZÛ»®‰D\"‘x?(ö…ÓÅ-•6‰[§¬ØÑ=XC“·é¼›°¾¬`w	(?8¢íøõÆ ál`Ğ¸)˜Îwp×ãÇŸÊéè!\Zn¤Ú»	‰D\"ñVcĞ‡£,ì·œş¦Ü¹7Ï¬òD\"‘Hl!Ñ#7?´¡j4‚~7·–@”—\Zk¯vsïV¼,Zeæ×Âµ)è¼şCe\r†«½U*Ï÷Cä}ï†L$‰Ä[E3 Yp°ÇÍ™fƒáó½ëšH$‰w…šémMcÊeïö¸°gÓœ^.w|¯>b·~ÇóÙ–·ïİ—åuŸØ…\rD»RİÆ¡âOXò“¾(²#Ë®÷Ç§eÿ8_ÿîD\"‘Hl<FÆ{ÿ{Dê}”¿‚ËgÑ¼ûeüD\"‘H\\m›ubA8^.yà´˜l¶VÕ…hŸÜóÇ £ŒW°÷nËÁ‘b^‚\\Ååœ4N‡qüúÊ¬ü:übÎg{¬²AœFß»-‰D\"ñ†!\\Û…¼ñ÷x«!×e(„½ëšH$‰÷°8Ê—[vM¿¯œİ0ôú}:œÜ”¦añxwYÒ÷uÄh /Aj¦Ì†Ör»™\Zv[?R\Z¹gèúbèæOxºÚo‹Ú»9‰D\"‘H$‰Db7øÛ*íï%ë[·Jéâóñ0å…Ãbş˜úÕxÑg¯ç°•í7¸6SÚxŠxaÜ/<š.×¦£ËÃ—-G~ŠåK\\›±g˜+O/_Ø»E‰D\"‘H$‰Db7#fÚm\rgÛÖ„¿Ç·—òYæÃÉm\nz”®‰WönÔ­±cŞI±f{ïöúMÙOoíònÊù-vç\0zæ“\'‰D\"‘H$‰‘íšSÊGÎ.ÌÚ>µ²ë*|\"„Z\Z­qv¬‡ÃÜŞïæÜïG¾Û+Ş¦7Mã†k>9-f¡k†ù§_Pú·y¤ÿåOôßx„š®ìİ´‰D\"‘H$‰D\"±3jŒ’·Mtûp¤0t8¤D®)G=2b8Àde‡¸+áŸ‚à»l!Ù»u×\0§‚óØ;¶´—Î\'*ı`8ÍËÚ–’+ëÿ¾ß\n£—?íİº‰D\"‘H$‰D\"qĞ³ÈÌAj¸‡ÚîÔ¦oú$sÍ?d\\H}ƒM¾O¿(Iÿ,ğŞ\rÌğ©àË¦lPVV+ Å‹º8ıÛRi€#¹6­&¾Xß»‰D\"‘H$‰Dâ&\0¼õ¾A¥Oñ_¼Éëdx\Z\"§Ø·~\"×\\Î]9–m5t¾w3ÿ‘ØZV«t;Æ²õËšgŞÚı€Œ»n„‘7˜;ä½ËçàN$‰D\"‘H$‰€Vw;Q\\X­¥Ãz ›	vëQç«ÇÌ¶îºVÀÇ©Á÷÷9Ïœq=gÌİ‘\r|ö±ìãş7:“|=\0MwgëçA8Ÿè|Ø¦]~$J^¿\\^tıN$‰D\"‘H$‰›…9--^¿es¶•€SœšÔdrüo<÷Ì|!d‰û@¶%õºzRß/İª\Z>y\Z¹)ë×,û¶Ìİ…Åù‚°µB\Z!ø•¤÷K·j\"‘H$‰D\"‘H¼9p´ZoíÎ¶b*8]v \0t‹A#ï†àµ‘ªL·ßtïÚ¤¡ÆX0ÑX³#;„¶a»´&œÃ1h–)×½Õ€ÛåJùñ´4søyùæ&m˜H$‰D\"‘H$ïøõG¡«.N-_3àÌåeß7iŞÆ„°›Ì½Fêõt8[_\\7hµU#ôÌ[°[{«‰tWö¨t9‹,²ïõf²²Êm4œ¶‡ó5d.„ıPK~úÄ4üõók×D\"‘H$‰D\"‘xCx=\0M	¬n”ZönÃÙã­py8ôÌt†{±[œ]ù8ÜkÖØ‡Ç„s©şòÿø/60més`²\Zû¸=«¥Dô@i#í5¯6±iGœY’O«\"©xš—şòùŞã7‘H$‰D\"‘H$n\ZLZ)L¼\ZÆ uã04ËÖ\rã®¥q\n:Ùiw¶¥çx½x¤á‡¸7œ	>TğÏ¾ú\'J·.á`Ç²M’¶e¾°9ú5®İ|P.órÿñ‰x4şÉ¬ØİåXÓp–Z½Ü{ä&‰D\"‘H|)0şêñC‰gE·yoI¯ŠRëC¤!Ê¶·€‰Ä+V‚Â2zë6mŸCc^®Û¶„_D›¯+)&6³Çqíš¯Ş<Âî¡ü?üŞOô³Â:ë™Ş†ó._Ç}¯V\"à†¶[^léóÒäOA&àî´XLz71ñ—ß÷¶®|]ŠŸKÇŞÕz(³5ê½…Jl¿0›ıöA«å/NÂŞB%*ÀİÚUÁÚôÅ]$y+|QçN[7ş6îÉ½(¾f²p?·ÃŞb&¯ˆœÒ­\rM¦ÃĞÜ.ìÈsïÛm‡ÎE™X†¾°ìÊ€$h¾v°y”3&À»|òşı¯\nE54¹}pw ®>6­Ò%bŸRlO;1hıè9ŠFW}=4*R/+›Ê¥À½Çì¥à÷;ĞĞ•¥¤Ãqm=\'ş¾wın\Z¬srizÜšCËóhQÈ{Ë•˜…ñüsşŞ¬_t’oHAİÀ\Z*›Vß›b»{wô¥áı|G^:Ê|×-–¸íuWf¯\rlŒPÚb8n¯\\Øbµ\r.§ÛÌp¥Ò,À‘«ğÛ_òÉñì2fĞ­ıÔ!KœwsûxtcKµæuÇh{+bN_{@ñl¸œä´Ï’Ì{ÖBÉuÈ¦ˆ)2¶éÃ•)“ä¢»%.æc¶Õ8Êh¼©hcÉ,IJ‡\\Œz´·dÜ·ğ»v]ÁôÅ¹8cÁ`İ¶JÊ3¿™–8‹íc·ÓE-viù_2ÂTåÛ¸\\àÛpCÉ6\'†+¤ÃœofcÓöÕñsKü•ÎÇ™¨i\'±üãÇ?øÎ…u6#ÚmzË‡{c™÷VÛûÂÊ/áø²\'ò&&~Rà•èyëûñÕ{Õâ(\0\rË2ıèÕD/ŞÁbî]Ñ[„jƒÛá‰ovøí-Ô+ª~F¿ho¹³°F|o¡\0İÕEVX<Æ]$¹Y4WIËäOoºäó½k¶qhğ…“ã\\©Ç{j¨Ä\rB†.dÇALTZÖ0¹Å\\Ì½N“)‡œ\n´GF,ÂÇë¼›ÙæÇKÿùÁüíÿ^tÍ©¦{² íÒÂ?éj)IÁt-a——×y,×»Îã¥ŞUæ÷~>yƒ Û¥!Ã¬İ9ü<şË`Û»®7MƒÉ¥‰1Ô\Zso¡^a¦R.­¼Ğ¦›Cæ¨ÜŒ¶oŒÍè.’Ü&¼?lˆvøšu¼õÀ¥÷ÒÔu0S­-Ë8Uå—g‰k\\§\"‰/eĞÂ~j¥Ãõ,›§€’hãq¡¢õb¾ÖÎr·œhÙ3Hœè€µû“¿ü‡ælj)nûşåø#ËbìÛœ7Şº˜ŒY|+½Ü\\.vFœy÷ûn¿€|ÿš°±h%qñ*U: &´äºÄœ2¼JË† #\\ïaN.oÖ_ÚG\nâ¼¯Øë$ŠM—ìœ?§ø4$=—éfÌÁ0 ¾¶—nPÚYÌÄ;Æ’ÛSF7Ğ“ %ì×âã.œM%4)¼d†ã_ñÆó4‰(ŸùƒïüøãÏş5hÃuÏµİƒãD·#“¥¨·¥Õ\ZÅ¦Œô_†üv:±¼ËÆ§¦ãş<¸÷ğ¼86çÍy”t2q1„1|sƒ­±:šîÍ›ÜÅÕß[¨D…u·ö*ÁX¹	¨·(öÃCï_BòkVæ§mHqåÑ\"‰«!$oÃ$•Ó	jæ˜Nj9u¼‘L¾|n\nG:ºCds¿’Á®uYòÉJ&yÉô~’¼qé†ÇÛ\'œGB­ÌÚÛ¦ç|€›^ÖˆwSu¾@ºı\'§ÕÕ¡Ù2)§S86;‘ˆ€>7æbYÆYåï\0b‚oè¼¾Ä3¨…úËŞB%\0±wÈ+,Ğ¸Ö¯Î¶’_¥¡bùLiš‹¾¡´‰D\rZı1LU‰\Zkb…á_D#S6Yş¤û¸éíA¥C/+/ùäÊ”õÜ³•t$Ëë‘hCş‹ahºÉk=Ë½Q¬†/2[î¯üıñİŞ¾­À‰Í¢\0Ä/2ë2qQ¸ÕÈ[ñ¯ØväÒÓ{A\\«¼µuÄ³?¥$çİmÁxÑÓó(¬öOEÌo\nì§ÍyS†}¼ñöI¼!«Ç$7ÉF½yvõvßGV’E°lqÕ\\™øjáİ,ç%ŸÜNw{º#Ú°å¹#½Æ]`ºD®m$â\'1ßJ•×Cí^¾¿÷x¼XñnGŠ[“e“Â‰«9÷êFWçÒÓ»€µõ{•¨¸3éÊÙA·TÚÛP?ãŠ¿ı¹iiÅ\\i¼uCi‰uÔ·Fl«×dÎ3§¸3PxãšıİœXë\0ú\'Kóİ‚\0}óû?ıçØ¥]Õ!‡N×lˆÛøø/è-­÷Êh\ZïF‚¼FØHÍ¥€r#ØŞ#ñª0#m#ÅÓÔ/ä‘’ÕØ0¥SCWoÔdkçîå~¥º,Z÷†:KT÷Ç›Z(ğ«Ä{ìÊ­\\ñseX2Á¢şk[“~E,·­mû¢³$ÙôĞïeRÿdŸ{NNƒôÂ7KĞ¼gxÛ‹qú”tĞíŠ¥­F7Å5÷ŒG;R•›)í n(í[D8î²«J‚©Î—¬„t\'f(6x›Ç­5]InxX3.¨!×\Zd?~¸;šM²¾Lüğğ7”wíÿòÌ•S‘ârÜ÷’eyİS	ak]#Ú%êİ’ÙÆÜ!cBöPÇO_$İ~–4îæÔ8”\\g¸}% ÅlˆÍÇN	,Ö3z†óoi¾qÓĞ§‘Ÿ´ÄUË`Âêşå0*¦ÜlßYGAs^¢§å<†£?‚T»CË;´y/#¼Å]8(&u;5…’|„WĞ¡¦Â©Á¶nê˜Å.€SRkU½ÍÙ‹¨Åâ\"˜hıC|øı´óFqIv‡pØ`½xzÒÑ3b<kóöWMgÄÊŸxĞnÇd7ÎëmZûY8Ë†bl²Õ¶F…?u¸™sï[?Á6K\rïc‹tşóZ#È\0ë¨šÈÜïÂ˜?³]U}C!Dò5ö®øÒzÑ›ÎeòÉC#¾hãGîıËä¸-i­ĞŞ	¡ÙçúªÉnƒhÈ¶‡V¥x§¹Ÿƒõ÷o}û«—óÉ•_kÈ8CnƒÎ†ğ†3ĞZ\'?4>!FOßÔÈ¸%òô}Şµı‹§È²9öı%å“¿\05ö6úÄŞÒıø±Î gĞebòz]¯Ò0³f}N­Õ×\"ô^Ø r¤‰”¬Ó@q]„F\'Š“yØ9¦îª©/°­·­ã¸œŸõö.Â©Bsù{7dRl&ƒµ}òc|¡íæË«$~²·œÛEá<Ú†š5Z¾\\CfgÍï-MÎÏ¤A¹ïÚ#+¢H²d‚c3å!,JëF¬«ÜqƒO5²ÑQ\Z@Zi©\\Å†P3Ãµ¸¢JŒ=²·\\Ô‰ÿ’ZÌOO²ğ®àiœ½Yšx/*mtVhì¹^]Æ#¶³•^Z€N~;ÿíÏ%ÄÓÄ™¡v‡Î¤Ho.Ö$öBÃ4÷½U×0ŒéÚèkN@#éâ_[Y%ĞY-¿\"ö²ÎbYn\nÿR±òê{‘|¢ßÿé?×ør`Ö•xRØ×œöo@º5á<°õø_=£L£äøv%Ú‘GóhqA Íú[+\0Ÿ¾<ºı\\Ã:0¼·)ù^Fi¯ZFã®8œğ9îÌ˜ieÊô*·<;Ò:¯o7e—†‚Á¹1 7jwpàu«à‚]™½\'¿£6;÷E¶ï¢îİ¨mgp—^K•÷‹q£u0É<ø‰ôm9NÂ¹»Å\Z†¶:ĞĞ5³o¶[qÌ|¼ÃdKÖô‰oS\rBÂ„úV.*‹—î‡ßnıöP8jì…ÓU³ÊS‡™e7hHAøR*ÑE¼¶01m¥¾İEé$}V×=´ŸD}Œ4m›)¼	Ø„nÚ[´\nnü¨ÌR*öwJ9˜’é-ebâ¸]+ÍZÆªaøÃ³d\rÒW¯¸ªÖ3Ç£3Ğ¶†‘©¢gÙÜ$‚?¾.¿Ã<õoïi\"ªÌ/ôÉÛU«İ\0Z“ÈwDÔcşVî`8â¨¦[ÆØ¡q@–.Æ…_wÅ˜µôìÿùß~\n±lH ¡j¹,²‘ª»ÔË!f5»ğ´E!i\"ætº\Z±ìæÁk\ZFÿ2îSÔ‘ïV%›`koŒ›f_ëÔ$ç–oMOœD 4˜ŞMM£œğ™¾\"UĞÖáTµ1|ÔTÙUJUgÂ¦N-\0Úu/Æ\râm¸WqDšSî¦Ë–¥Şäî(†7jF#Í2ãNò ¥1nÅ¦ÚMVä–Všhö¥I\'öQªûÇõª’ğÈyúª©|œB0íÄè­T¤o´wä­´³è±æ5©?*\'3ÂĞİ½\"_®-0P‘Kà®e&Æó–â­ñ\n`€Á/½ƒ§¥«qç5º×ï¬Ô‚·¾\Z%tşñBÂíYŞÂo?ëq\n¸tN¥–¶å<LĞ½ãvË—şUÏ¹sğ@-°»%W\nŞXÿíÍÀL%«Á¢] OìL¼çqÒR2Ğ¹:hêñ4±‹ÙúwßşêG?ÿuàª|±ÚÂš)mwÛ?)Ïõo©ÜY7Y›Pûƒ>ûh6€Ã‰ååäs<¢¼|ó:ãê!+á\'Ôì™0»ß€J6i°e2›zp\"˜™‰õË–˜°ìö\rb›1zyÆİ‘svëàßÒŒs9œZ²KUZW†çDµXãÕ*[x=óaä,£·.ÏîàIb/{VßÒÆ>-vyLŒ°ÙŠM^Œ¿Ïª`–‹=‡xˆ³§vh\n?º4Ú2\"	nı¯Ù<w–±³1Ş\ZfÍŸÔù>ùsä¼ªcÅîùÙŞJ±&üü»¦Pr­İÔXÚ|¸‹¹zëÂÍ¢Î§6©­WğTê²9|G¸ådÓÑsSÍ«#æ8œÎ}u¢B0MÀ=èFEPÇªñu†ûtË”ù%co­^-]wÎ\n³\\ç¼wyõ‰£Bšï‚¦îpT\\œ–óºèÜî`™ÙNÇÁ“è½ŞÚA˜,ˆdvpŒ*ºt43ŞüT¥—ÒßÔ°ÚeŸùŞO)–Hô3_a¦ù*GšÜ<*M¶i?ò/1Nãìøa}û\'¡äá@¶’Ö~â,ôëŒ¨ÛÎEoT¬ŠÃk¼4Ñ¿íÕêĞÆ%’ÓŠçW­”Æ¹ßıúÍ8\'çÙ/´ İ†Ï”vÚr†!>8ê´Î¡Â^ÄFÃxğ¸½®!cIî+qˆ}túA˜€ÓÎÆ1²¹sİ	}hj%QWçœ\\J˜øg=»V&,‚Aùç<H†Îò­ê”ÉùÃæ9¨RP\'Bÿpx1Ğ8±H½×úZª×X\Z‘¤¨´ìé9M\ZëK’ôVäB\"×šõÕÖ\nÚüå„ğeÒŸ“i¡ØÑj•Ìµa{›”\'ÅyUãyMÌè”º®ËkªLJ(Õùõ¥®ÔùQÁFçgù%\n?9WÈ~ßvp‘§·‚càÚ’wT!aÑÛ¨Y™¹n­éèÆ¤iÆĞSQ´tôı¿ÖŒn½>›ò®ˆÃÊîéOôµGF?JlšSÄŸˆG‡¨zaâôŞ¹…ÄbÍZAˆtGš±t³ÀùşqÌwåïeÄ.Ÿôµ‚§¨c~VHR›‹3vŞãŞè¶Y4aÇ´wôº]Pû´N\rˆá2Ï³eäWŸmãÛ¹;nÆ™ôÍrıÛì”<{*‘ºÅP¯©o˜ãşö„I‡Dì¬gÑˆ&âş²¶ÿúÓS‘¦ÃÖ!FpÌî(ˆ6w	ÑdtŸÛ×¤Cî†xl&&])»¦1¤¥?ò¸0f\ráéÇÕœîÜmQ0ù´ªvĞnn&0\"±{P›½§Ff…dDñ7¦&&h°Q££ñ,Åu‚õœ³ö+YçJ‹‹½šwwF	@ØÏŸ}¦Ä×&<@.Y¡ó-X¢ğìõ¸âpj¹ËŸ|î½}0NdW(ù fm,1Åg—/ëÛ_ıÅ?şŸÔv©İ‘À.§œµÏôş„ü·EÃ9–­l÷Wˆ˜ıÉ½]‚ã|s™ãà¯2_gİ2@ƒ.Ù½µŒÀx\"úîq\n(ISlœƒ£F*¢¯XÁşÕo1Á£+A»NU$G4@ %;Zôd§!¾4¢Ùº;‡É6!vqÿT‚D‚B£zou1–n€,s>ÃàÄvyîã ï_ézAÜ=7¡‹Ä‘kU˜ƒ}®ÚZ±Å¿í4Ñû:„Q×]Ú >ÅÒğ÷Î—ÊA³ØYB3‚Ó]Â\\VÆå c&×·[£wa`Y)@_¢~8N¡H¦kŠí6â{<s¬ö¬BhJŠsBEpï÷úz^âô4K(Š2lµoš\rk˜A…?Ds¶íœ:K\0l[˜}hñq5>wæwµwBg‰Â8ûwüs=+,Òç‡5¦£Õàn/‰VÛ„m‚Ã)åÍMßD®ëlIJ‡`:Fº1nRÜ3ºı‚â˜u«Ñj4¾ crÈ²cãå¬æİH9±Å†(L8ªb¤vÚÔc«‘VËÿ8Ÿ°4ï°/ır~]ÊROÈˆ¸¥Ø›&ûÈFËF¹Ç]QogÅèœ¿w4\\Ø-ÃÙWó´€™ä“ŞøT`\Z< ”z%‘2?ŞM¨¸ÉÖ1?$ƒÙ6{şãEênUO\nd±;}…Ìqöë€í¾êÏ2¢BƒpÀk>[o™à°‡t&†¡9f	šÛ	>*’8„+ßùü¨ùx|°ßÊ\'vlél‚Ï¾²<›ôço†¥‹6†Í+>~±3 uvgÿuá«RïÅ1\Z%Ô´‘<@yÅü/mÓq·u¼7Vëqå#n^{mÙ·M>×ÛÇˆkÛğwÜQ®áu	£‡Ó–_®9xn:´hT›Ì{7JõOÓ6İšİÉü––„#EÅÅÀÀ|;KÏPzqbõH‡şª¸d­ºhù½[Êêé±§¬ŠGÛá\0­¬Àue¬g4‡ğÙzı}\rÂ›5ÖÙİ:ZÆ³Á…yèo\nĞ·K¿¯˜7~ß·`Â ï1 	FÃşš«aàj†0àÇ\'/;rcb!#n’~v;‹“Ú\\şw±a{œ½\0Ãç8‘˜×‘fWC?cãHíôª‹ZNğJC­}Ùô ÇnN¿kùep_ˆTv=ş\'r1aıíC·‘	ö–³*Ûx*›n;œ²XcÃiG{œáp\Zêqí–!–ıáˆy,ùøñ[úğÿø/6.ŒAç\'%­Êv-ÿeşn¼ÍÁh‘üÆãÓiW¸pg“@Ş&øpZ,óÊ#ç–aTŸ[UöKI‹º\0«äìã l°ˆ´¡RÂŠLen®ÎÃluU­K­-~ÅL“×IÛgjA«4Wf”ìeM­ÿo»*rMñJ³Ğ€-\r£l×­Ë«älwv·XíA\rÛ¡âÇ5\\2¡‹8\\Ş3nÌ&GhJç@ÿÌÛÅ‰\ZLf öé–¤ÚÍJ¾úJ¨¼¦j¹^1P’Ú5å\\\rìQp_wEæÔÀzZdvSËƒÏ´·\"XÉB6Yx~vK@cÃ)8Qe\\­½Ttİİ’Ÿ2ïaF«X…lJù¬`:ògGì¡œI=â–?f]ÜÎ¨\r_¶A®RÍ˜1n}6(<¶óòµïşøç6U›?tû¬×CÌ‘k›3ÉÃ‹ô0såæ±ØV¸œ’Æc²:‡ï]È»0úë›[oV¢1†ÃÛ[ºÖ÷§\'©YPšgÜfng¶:í õÎ;/tÌÑLP)KËw<ˆMzeı\\‡èıù3AŒìúâİé8éçµ´8Á—¬Ëmn¾îş@Òİ»å?®vGíÊDŒş&ó\nÜK–ŠŒˆæPMÈ“oŸMçæ¸ÿã†ZwŠİÈqbÔõ-iË“ +ã]f™Ï(èœPkuY·É^«êtqŸçÆ˜Q÷Â¸«IZ“„*>0ƒ†aôü\\/Üéf™iígøĞj@)-Æ‹wTĞøH‡)QÜoÁ&EA™~ák²-Å­ì‘ıBÒô‡ßû‰9‹Ì]•¥§“E&Î¡gùš”cø²\r G–Mÿ¢<Í7Œqç¤Y”Ñ\\YŒ,/ëH¼BÈ5ÛdÛxIÓ\n?^\ZÁc_wUoŒ1¯ÿN!À_12~´…tt¢QÎW¿úÊVÿÊ2´\0¶ èçë	à\\µ:<`ó—õ3y)CÌ–Ò.£Ôïg±¹ZæÂÒFD¥æ\Z+ƒ\nu×€ÆŞ‰3Wò(ËXzyKe»”²ûÎĞ9]½yâÕÔ C^:5éXÃ3Ç\\L¬j¶´Á@Q»Ã­%~YYÇ^_É2=§D\rCqÌGªy#8s“.ä\Z­Íßú:½VÏëãBQ/’GGÎ.s	éÖ„MJÎ´4ŠMZF‘çF!ßïàÔÕª‰]»S>BSŞšj„ú_=©Cün‰ªû?üû_­‹ÃÅ[¦;»cxºY £Ér®8?B÷İ÷‚Å#×Btû&ŸÛhxyï^£åÆÍ-ò˜…#?\Z·_r<ZØâ.=~Ú’ÊŒîxV4|Ç«¥qbiu—·êa£I¼v:¿hœœ_‹MğR‹}ehAŒõks]O\0íÎ²Ä¼1Ä\rc„;c²…ƒwÄŒâ˜<¨\"R_kD»<™z7ÊRµ\r¿f4µlßş8V/b¦†Ö¬†t„]Ôµú“ƒ	Æh\rcÜLÿ»Å›Û+F-\n×m®¿Ióxi–¨CJõ’Ü8[L’g€/Wí·E¢¯}ãEÁÎ¢~£ö¤g=a—•‡1Àú¬i\'›ÿYgtï{ï(ÎrÜ€ÏÊ¬uóÌª¬ş^6AßC¬!š¹ò¡$­ù®\\1²äxĞw„Ââ¨øüûwÿêçk¦…¶†e¯Ü)Ö¢ºœşø„\"Ñn®xM,›Åú¿ãP¹qØÙ··P¯°šp²L“:2§·:yÏ2 1@ÿQK¸øÑ¨b²Aq¡KßßÚ FmĞ$Ôdß©†lŸwí †å•\'ÓIò:ŠQª¡`áE¸­Ïñqq-Î}¯3¯½Â/U ËÛG#E´â	Y3- Ó»|GêbÓ ôfébU;\\…©>²-6QÈ¸ª„=3\"F¿•qÂ°¶¼¾Æ¾š³©KK”ş\nVr+©ŠÆè+Á% MÉ#Êg²4r®üëœğÔGº¾aæà|†ÿ‘ßÈ^)Ş¶µ\Z ªÜMbÜpƒ^s=ÁYºå.›”pº‚m#½…v7A³œìÖØPÔ&öO¯×pÿ%ZS:·gÇ%Æ½lÓ®Áe$énÆ-Şî¿­ xäÈ´[¶ÃºgÜİ>è‡¿¥½…z…Ñ›ìvC<-ä ?f³ú^-nXl+\\‚%Ã*Ë(œ1p\rğüZlÓ{ˆ‚V³_O\0ëKˆ%jÒpúñ£şêÈˆµ–}¡Ö¸\r_îkPÍáÅnÀNå©í©éıuÃšs	è%^‡ï½†UÊïo’SoYZ8À€w÷òR©%êTt¯ÌvBİ=ãî•Š‚aó®÷¼Ò;»¦V=ÎÊ3gØÉŸ[¶5«.Î•YÈÅæÂä%!ÔË4l¿ \ZcşL°çŠ*È~Gøİ Ñ÷Şb¿¤YÃ”È¬×ÀjıÃ¨è]Ó¾(ŠÀ&[•½‚ÆêÁšã!åÜKb0ÖÛÿèûmƒ¿+?xÌ8PlÈ÷n”©ÿ•Äuz‘ä¥ë/t\rŸ½Iı1ı½È­£ÎĞ0Øöêax„,ø\0«i]â\\İ¾ñºœdöK%TD	;Úê!VçŠ!?ç×bœ¹lÀn8$—@†„ã&#áŒŠtG{İÙÚèi‘w%É©Fc9oÖÂv·©)ªÛ-§*VZÀ:kİ×=ÆLÿ^r™ñ¶¼:zİ’4T©9m_{\'”3VÉÓ]N™^°(İÃ¥M‰±©-&´Ö©ºd‹–eFKß:K0³~NÍ,¦Í>‚×İóëXª|\ZŒ{‹qâõäıÛ\ZôµE’øÔh~×½¼k«“Ó‘ªE õ4ÁÁpÀ¿7–#î°Ÿ_Ä§TùB¢eWÏ3¯ûî_Áqåzr\Z]İ¥™á­\\qÃ¯%†®Qì@ùdrM_W¾öêO®}6fã¥a†ô&çKÌy>^Â!Ëõ2si{fßÛY±ìä8E³‡ÿfôäM´ã/ÓízˆÑÙ«qÀŠ¡åâ}m«šu€1Æ=7bƒÎqÁ;švz\'!àõ\\¬Ğ€”™C.qÿ2‚óÁºÅÓò\'ÏJ\Z*„Çíèh\'3x˜EM‚×!·6¦ÅÎ^±1Zbze^iig	rÀõ&®Kp!†K{Öİ:A¼8æ[Ópeª‚Ã•&Â™¦ç™×˜68ı6ª—¤ÌZÉ2o¼ôD£K‡¢.¤Dw—9z®ƒ$>_÷ùÃo}û«ß¡f¨±?­†’\'„Ÿ(7m`Ö5)İqg£ub›FÀË‹öow|î-­e¡O¾‰!ã;k¶ta­+ÁÕ-¨v¿&şŒ;.Ÿ^×W¯2ìÇNb&Ô²	^\Z„2Í®övÓiá½£œ0…Ë‚ÕÙ2À„-›ëGª££nàäCdblö+FÛd³‚G¬+¤W$²AcËeÔË\n*Vø\0·¥¹=aÇ7	¸é¨#ftşàÑ¬¤ÉÃÓhf\r´¹!³yLF†K«â9+Cmxç…IºØz|¦â±¾ñ(›Şn%Ú5f;D§m°Ï¿Ÿ•¿\ZtÀD%Éº…–Çq8İ…ÕB™úFQìñš0{{uÍÇ^ÎIÓº=†Üo¢\Z„°—½áçsveî1˜Nfh»jvq´,Ä¡İb%0¨¸açJcç°CHqä:•?Mö½§yóÍêîìrlGp	Äf)½|Õ³ÊÕjìÑ>¥w`T¿Ç-Uj\0c¸‡r£{¿×‰¥)â£]$‘xA£ÏB_=Ì¸Á\0\rVÔış0GH7´ı‹]BûæãŸªyşàá©=aÔLicŒÛÍ…1yÅñÛà0ö 7È“,”ç°ÚÅ4Ç_ä‰ccjØ‡šÆ5ğÑj‚Mï.„µÁÊšnBİ½-pKÚ_È«ÔÜ†è Ñ0ÖrgtˆşÙWÿä.ÔæØq¡Û˜Î‡™Û¼t¸ÿÚÅ¦\ZÏ>Ä§9é)‡¹q!I·Gx\"ëÉİA.ñ&\nDƒæ“–”#˜İI¼ª0ò8¶R=^Jä-E4.C‰]]»n9 À% .ßµ×FŒë8á­¡‘ê±e`LFÛÁö\r¹=<bG»¬v9Ş˜8ÆÄ½s@p‹!\nM}ìå€cÓßÈìSùØ¬ßFúˆ\n 8NÍNœß>!É†°3h«Å@_…«eŸ²­Î9ï,d3¿åÙ¶ü‰ÍşÌ®óæ/M1²VÕp\r(vBÅaWÎ-âáªoo!\"˜„ofFl,çºîÊ\0ÊÌâ‹xì¿â(¶´U•u é[B9¿ıòÿşWÈ|›vqjK{í c<:îbÙ”¬NûÊ\"ë÷§¨5cî{wû›„³\rò[¶hÚ&~µzÓ–KKr¹\'L€Q ÃT×j’‘#àÀoï•aøX`£EoŠqÓ0¾º—Ë£tÔÊû#×Îì#]aôy8a WkápñHuºç¯=îo,ÆgÇßêŸº\"ó1/3Tf\\Üî&ºÓ	HMèXöâúOÈ/\\TNg!<û†‡Ü&`süyĞnµ@†µWñnå*ÄW·Îâî-gx6qiıj|­Ì†B‹Ÿ¬tn4L‘¹ å\Z”³Ù‡’É«„±‚S‹xÌãz1Å\Zg~´šÛ-7]³\ZnFƒË>¤mÂHs3İhËºhãÃ­ºÿğşóOâIht¸ävÆ¸¹¿›8u<?Í„Ë+y§,ñp‰˜É*×¢^ş´wo¿aÄ¡UFéŞB½ÂNümŠsê8¢Æù›Höh°.¸u¥¯÷ï	¦„Q\n3s\05ÒÙõöËA›­,l—ó÷=—££œ;`(£•V„¢)ì®˜Ñ¾ñ&ËİÕjwVGV×Çty§ıìÀ”Ş%†ŒÔã˜w\Z½šeßı¹ÏºE¼\"Û™8Ø×#%LØ‹¸<çQgU¶´	y°³gƒ{Ôkit°ãˆEé9õkz÷¶GæúE6ùk\"IİÉyq£²ñáœ#Ùàè`ëI“Ø¼\rãF9&x•ÃG\\œ¨É¿€—Èg›E¿hÅÚ7.…¼ä–[ú¬)ßñd3\r1/û¸Û‡µI4ß®£#Í[…—ÿîİÃoQ{Oê´ÍaôöVKß÷ Iz•^uÀâJÅ€¿Ít’8€-è©‹$Æläöˆ!:­§=U©î¸!rYfœŸN¯Cü>QœC™>ı>pç¼Øu~9Ï+	ç×ÅZönÆ-‹äKãœ_ˆ;ïet	5IW	k‚¯f ÇÃƒ½ñ tCAö¸1â|òìSõa˜õÖˆ×vÂ/½uÙ¬öÉC;#ÖIŒšdôJ…ãt×@½hIyÆˆl²\Zv¢Ø{?äÖg=MÆ”RŠR…•‰ûæº².|mçpƒ‡ÿv\"æuejqbkèı µC[,ÛşpSë~÷órny‰/Çë°€ÿÂ¥]!ßƒàxê8ñëHº%îóÌé‡Öˆãïİ±ï2„nhfaBÈÇá„\"…\Zg®<“Ò¾+gqåQ³r[w_®ƒíò2ªÙn1Ä1>SQû û@~~<÷i»Ñ2‰–¶ßAáwëGÙÀãâçw¯íaê$İdİæ[æ±z^×¨Rú÷ó³nTÈ§ŸÅmÃL\nH¿ŸÖO2+Î–Íôu¿ZPÿª»0q¦Õ\0Ö‘>•rúíf´Ü„­§:œg˜ƒ»?kØ¢=#3âtrnêŒ3L¦¨M÷¦	ªÂœò‘À!‰FjZ<Ã§Îî¬HpùJëm ›ÓK§E­Cå¬W°yE}µ‰xWƒ5©œÇ…+`4ğè8SjI)G†úRà~ï¿}üÙ¿ºHôS$×²{ú©$Ÿ¦v[S²:}Á±lŞH^Ù÷ãHvvïİŸïj•n„§<7’”6+ÜN–àv:y>TÀŒ»a”î;R™6YmkWÿ#È07è¥‡¨¯ZÇ“Š9.>IWSè0–\Zí@r_e+WPE­ÒÊ¢‡ÕĞ¢5éDİ!pºì˜ÿÈÆwb>šÁyÉ…®$eU[¯K’çVÄ¼NF.[€ùËëPïc© ;Ä =wŒŞŞÑHkw±‘²m­æ­¿úBº©ãíÎ\Z©ÂdIdÌtŠÑP 4Âë ¿Âİ½Y@‹ÿ%º„»2M\Zƒ\ro\rÊ¦¹¨ÄÜ#ÆÇüíqIÎ›>g•Ç’â[LcàBÉ§¥¢D_çCu]Jf=zøŠPÖÍ@¨EGûv^·XÖö{„ÎMi‰xg{0¸Á¶¯_ıŸ\ZşÃ¾£,Y«Äşä/ÿ#×@œ¹IànWµÛ^nú¦ÌõAŠ¤k’¹=-mï>|oà¹6Ê¶.:¤Ãµz[–O	®ÁÙn™KİKu~”Ğ\np?],ÂÓlıÂ=ÕT¼¸¹`U«;§”Ø¢\r9Æf%„éc›\rñ5jÿ:ŞÜò¬ûéÆùŞ4°Õˆ¨ÍêoOÿÆF†ƒaÁ:Š&Ì3ùÆ²èÁİ¡ßÔÎš=Õ§Æ.IüA\0~ÓÑ®s`=®rš%s¦\"~²2àP»®WSíFt*eö¬ò)ÆÍİ„ş?5`kT“º6PŸ¨©¬#¨ãi¢w¾lM½ä•dıe,“™Å {½ÉÉiÈ>hex®äØg\r¹ğ ÑÌò`føi…Ã©1JÇï„rfªÆå³™k&‹Ç	krSˆC=šQÂ øWË\n£\"Z7Ş\0~~ğw¿)åOñ(³Ô^9lnñ.è%Ì½~ÉW¼•;LnÂÜ¿¨,~ï~{Ÿ±ñÜŸ{Ú˜ş¬ÒÑœ±Y§y\Z(Ş6o:Íãµ¨uÍ év´Ê¾9&\0WÊø\'ÒÎK•ãúÿp/£ù°nÖõ\ZƒqPÏ¯­„,P÷Í\"µk4Í°aæöÓm&Âû63Æïİ\Z(ºl‘ë»ø;í<–iµ¿N(Z\'œÖŞ–Î×ÅTÍ´\'ùR)\n÷K&ÃDdyB·À<uÍ É9Lñ*Bcô£“åÒpT:*ŠÆt8â’ôøí—n¢ON+V¼™Õ–AŞ2^š®Qİ2ãväŒÇQ† +„ñ‚EıC£…¾À¦[)ü©SãŒMÖÎ¼„5¢©´ü—öoŞ˜èpu3|€;ø.q†Lgè¢šşıwşêG?û5ŞÍÍ›¯?B­Ü¹PlÙ‚Í{·ëİÜ&·<dŒë_—Oöî«÷ŒªÜ‚S½·P¯0Ãû1JcâÑ…cñÊÙWGÅ>­âD4£öf©vÎ‚‹Ğh†Ê	™:gCâ‰÷â0¶.Ğ&‘ÊŒ%ç­•ş&E­:“õ!–ÅÉ&S/>2Ó ´Â°ÏvGô|éA6ys­í\ZÇû(¾lœÿ™–w>¼n¯£©4ñêËNifl7Â\né\\Á\05‘ÆI›‹×ûÉ¢‰îk½Â_¦j° üçÈJklZYÑQ!]Ñ¢Ó:ó¹pdìÙmjÄöXÆxÉÒ g=kxîà2W£plFè#[új\'uìƒßx¨eí-Ë+p1ªQñ‹yƒ7\ZQn¨8üœ`|ƒ `×ò%>ÿüÉ_şOMÇtñ¡ÆŸb o±iŸÁ¾dÇäs¹,ãÚ‡õ÷êbãÆ#kç¾(ªeı|~Ñu©TŒGlP—¢fïE!Xì2\r«kÔ\rW[xƒ—Ò\rŒò3ÿŠn‘˜S„áDã-4KÙï·éœi4îëHıÙ¦MÀ¨ÑÊ3› ” #ùl§Î‹œÌ*UãÔ¨Åâ2æ’|h´†ÆV6SÚş˜ÉùW¨sbIÊ=ôÂø\ZìÛÌ‘±BpB_!åÙCğ÷jánñÇªú^É÷‚ÕŞ‹V,4j]+~\r©ÜBh\\/¢nóêKx ¤ô\\íFŠÅê÷Jë„Ù²OãX:Põ—¾£µMxœ–Súó@\r^zÄ\n»·3ëBM}›ˆcŞ\'\'Ç†\n|í°\ZAøÁßşïx.Y./Qìåv0wÜ™’t\rO;\Znî{Göî“Ä\nYıcçpÃÕÈXşd‚z»|qkƒçvÕj\Zc¸y£*½÷•%µ!8rDÖdé¯‹¼º¤]Bo¯\0‡îµ%‰\\ı¥«ÍßÍºÈ±K¿ß8XK™•áğı½ÄGµYõ§±BxÅjF:fGïjsS‘¥:{‹vAĞTROï© ­]˜8_ÑV€2±ë–Ä²yÌ„ŸûğKPq]å¾óãıì×qoµİµ­•]Ş¯êpÄmâÖË¸e?8²ïÇŒk\'¾€Æû2V‰D\"Ñ‹¸D™‹‰D\"±	L›Ù¬{] »‘¦…?ÿé‡§±i9Q\\Î9&÷w—(¹òk“p¾Ğö½[=‘¸6ê_2RŞ÷ªr\"‘H$ÀGaäòl\"‘Hl“N&Yåáßpº»UäR&^¾óıŸşs$ÑaÏµ»Ì¯Û¶×{}BVÎü\Z©÷ŞmœHìÌÚ5;†‰D\"‘(›îãN$‰ÄV°{\rJø~ıÀĞhw‰l~ùÖŸ>üèç¿–·!-¼u8e†+CgÂ¾0ô½[4‘Ø6eo¡‰D\"q[¨^ß¼ï2‘H$®†p ¨;è@¢Ş°;Ş¥£¿üüñŸÿMó4—.{ºé>{­œ™¶wû%7š¼o÷(¤D\"‘H\\xxr.Ï&‰Äõp(7ÈØíÛp»Cú‹&ÿ¯?ı_–tÛ{»ÜÖì’@şT.æÎ¼ñD¢…ƒ[şºÄµŒ‰D\"‘xÓ0ÓX$‰ÄÕ¡9ç¼İû(Ñ4tø¿õí¯~ô³åÒÂ¥ÛxîÙS‰eÓÆí‡<o<‘ø\\Y\"Û[ÌD\"‘HÜlĞdo¡‰DâKxòzùñ¾ä–·%÷g˜º½w‰›¤”ÈY\n™%˜H$‰ìFÂ½…J$‰„G¼öüÿåßüí/õR°×Èu¦ˆ\'£ ygÏ0Ì,ÁD\"‘H(ĞX¼ş»·P‰D\"‘H$7„&Ñ‡*ì-c\"‘H$n|nÆ¸‰D\"‘H$dï6d’\'×N$‰Ä\nÈˆäÅ‰Dâšøÿ)\'zÜ\nendstream\nendobj\n5 0 obj <</Filter/FlateDecode/Length 877>>stream\nxœµ˜İo›0Àßù+ü4uqı‰í½õsÓ¤J]›·iŒˆ‰„Ğnıïw†@]Í€%w±ïüã|Çá0ô5¸œxÙO-š¯ƒó[Š(HËàìãüWpãN!(^»F\\ e¦­!ãÖ†±0š˜T¬‚³«¨LVyñŠ’§¼(­[‚V×°2¼À¤>Sô\'øşÆ K¤)‡O\ZR4ã09	.ƒß•j0…BõD¥%–ü¢ót½¢è:Gß\\zAX5‡p‹_{˜/‚Y+ Á<Ô!·$	á[\n¹#ó\0<y#î6Ÿ1{)Ì^Ê:FµZöNë’ãf–`®M¥uÉñnÑ‰£øÅØ…Ì\\Å/Æ°ı¹¢8æ¦M²Ğ—+°sö­!ŸA3Ä)äWûCwîJ€È:	c×Ï`Ø+eÁ£?ç*»6ùZL)¤ ©9P]\'›sMLLa1XĞöºˆ½®îô8û’?Û¾\"8œ“šO\n/>”ï±Œ–Ë‘ølÜOh?Ÿ6˜±¡|ó\"zÁãğ	Jëøù÷WØ:Cùî¢r,¼Õá3~<#°\Zœ~7ÇÂ“ıÑ“\\ãp0Ş]ºGâ³åxŠÄ_CÙšF6õîRî¯Şj|àu²‹ô©LóÍÁŒnD$\n\rÁºíç¦Z¯\Zµî/ët“nË\":v¦)f¢Mpß¾ßpè%ıÎõ-Ät¾9´O¡ú}S\"ÈÑìÆE—®¶N°áŠIÈvc³¾$3â¿ç-ï$0çö	%T´~Ş\nß´ìÖ%ÇÍ,º6•Ö%Ç»Ew&âc2s¿¸çyËNİ›·ŠtµAĞU¾‰“£oLs¬DÿR§Öó¾›z„{BßbÂ˜È±¹5©û’õ%Éò§u²é==îKÒ¾%NMÒ)|7I:	÷„¾Å„1‘csOÚp¸qN¥eï´.¹m8R¸6•Ö%7\rdgâ(~1v!3Wñ‹ûœ—:7åÛs”¥å+ºØnŸ‹zÎ)•Ü·Ğ©•<…ï¦’\'áĞ·˜0&rlî}jÿQYTÙ“˜ò9Á!|P<àÏÃ£2ô32!±Ñ9ï‹tS.\0v$B#-¡1~B†GÙì6ù9c„¨‘şPáºÚg¸cû%…T\ny­°Í—ˆvCş`“§{\nendstream\nendobj\n1 0 obj<</Type/Page/Contents 5 0 R/Parent 6 0 R/Resources<</XObject<</img0 3 0 R/img1 4 0 R>>/ProcSet [/PDF /Text /ImageB /ImageC /ImageI]/Font<</F1 2 0 R>>>>/MediaBox[0 0 595 842]>>\nendobj\n7 0 obj[1 0 R/XYZ 0 854 0]\nendobj\n2 0 obj<</Type/Font/BaseFont/Helvetica/Subtype/Type1/Encoding/WinAnsiEncoding>>\nendobj\n6 0 obj<</Count 1/Type/Pages/Kids[1 0 R]>>\nendobj\n8 0 obj<</Names[(JR_PAGE_ANCHOR_0_1) 7 0 R]>>\nendobj\n9 0 obj<</Dests 8 0 R>>\nendobj\n10 0 obj<</Type/Catalog/Pages 6 0 R/Names 9 0 R>>\nendobj\n11 0 obj<</CreationDate(D:20070212152549Z)/Producer(iText1.3.1 by lowagie.com \\(based on itext-paulo-154\\))/Creator(JasperReports \\(CategoryReport\\))/ModDate(D:20070212152549Z)>>\nendobj\nxref\n0 12\n0000000000 65535 f \n0000072313 00000 n \n0000072537 00000 n \n0000000015 00000 n \n0000015006 00000 n \n0000071369 00000 n \n0000072624 00000 n \n0000072503 00000 n \n0000072674 00000 n \n0000072727 00000 n \n0000072758 00000 n \n0000072815 00000 n \ntrailer\n<</ID [<c0fb0ca84741ac745e809807b221dcf5><c0fb0ca84741ac745e809807b221dcf5>]/Root 10 0 R/Size 12/Info 11 0 R>>\nstartxref\n73001\n%%EOF\n');
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

