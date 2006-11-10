OnePoint Project 06.1 Open Edition (BETA)
=========================================

Welcome to the most current release of the Open Edition of OnePoint Project.
This package contains the Web application of OnePoint Project 06.1.


System Requirements
-------------------

* Microsoft Windows 2000/XP, Linux or Mac OS X

* Java Runtime Environment (JRE) version 1.4.2 or higher

* MySQL version 5.0.19 (see Appendix C) or PostgreSQL 8.1

* MySQL or PostgreSQL JDBC driver

* Apache Tomcat 5.5

Commercial application servers (Websphere) and databases (IBM DB/2) are supported
by the Team Edition.


Installation
------------

If you are upgrading from an earlier release, please move your existing "opproject"
web application from the "webapps" folder to a different location in the folder
hierarchy.

These short instructions assume that OnePoint Project is installed as a Tomcat Web
application:

1. Install Tomcat 5.5 (if an installation does not yet exist)

2. Copy the "opproject.war" file from the ZIP package to the "wepapps" folder inside
the Tomcat installation. If you used an English language installer it is typically:

   C:\Program Files\Apache Software Foundation\Tomcat 5.5\webapps

Slightly afterwards or after you restart Tomcat, a folder "opproject" should appear
inside the webapps folder.

3. Copy the MySQL JDBC driver (JAR file) into the opproject\WEB-INF\lib folder

4. Install the MySQL database software and create an instance
   - Use UTF-8/Unicode for the charset/encoding of the instance

5. Create the MySQL database user ("opproject") and database ("opproject")
   - See Appendix A if you do not know how to do this in MySQL

6. Adjust the configuration file C:\opproject\configuration.oxc.xml or copy your
existing configuration file from the previously moved old installation
   - Database URL (default is "opproject")
   - Database user (default is "opproject")
   - Database password (default is "opproject")

ATTENTION: Do not execute the following step if you are upgrading from an existing
installation!

7. (OPTIONAL) Load the demo data by executing the folling SQL file (see Appendix B):
   - demodata061.sql

8. Stop Tomcat and restart it in order to make sure that the changes in the configuration
files are applied to your installation (in case of a new installation the repository
structure is now created in the database which can last a few minutes).


Starting the Application
------------------------

If you installed Tomcat using the default settings then OnePoint Project should
be accessible by using the following URL:

   http://localhost:8080/opproject/service

Please note that for a totally new installation it can take a couple of minutes
until the repository structure is set up completely.

You can now log in as "administrator", there is no password set as default.
Please note that the first thing you should do now is to specify a password
for the administrator user: You can do this under "My" -> "Preferences".


Using the Demo Data
-------------------

Log in as test user "cs" for a German user interface, or as "tw" for English.
Neither one of the test users has a password set. In order to gain
administrative access, you have to log in as user "administrator"; note that
also there no password is set.

Please note that only two projects really contain actual data: "Virtual
Cockpit v2" and "Einbau ALS/Xplore". The users tw and cs have only manager
permissions on these two projects, otherwise you have to use the
administrator user.


Known Limitations and Problems
------------------------------

* The tool dock is not yet collapsible

This limitation will be fixed in the final product version (OnePoint Project
06.1).


Appendix A: Creating a New Database and a New Database User in MySQL
--------------------------------------------------------------------

In order to create a new database with the default values you can simply
execute the following steps after installing MySQL:

   (1) Open a command line window (DOS shell) and change into the "demodata"
       directory
   (2) Type "mysql -u root -p" and enter the root password which you specified
       when installing MySQL
   (3) mysql> \. createdb.sql;
   (4) mysql> quit

Alternatively, you can create a new database and a new user in MySQL also
manually:

   (1) Open a command line window (DOS shell) and type "mysql –u root -p" and
       enter the root password which you specified when installing MySQL
   (2) mysql> create database opproject;
   (3) mysql> grant all privileges on opproject.* to 'opproject'@'localhost'
       identified by 'opproject' with grant option;
   (4) mysql> quit


Appendix B: Executing a SQL file in MySQL
-----------------------------------------

In order to execute a SQL file in MySQL you have to do the following:

   (1) Open a command line window (DOS shell) and type
       "mysql -u opproject -p opproject" and enter the password you specified
       when creating the database user
   (2) mysql> \. demodata061.sql;
   (3) mysql> quit

Please note that this example assumes that the user and the database you
created in MySQL are called "opproject" and the SQL file "demodata061.sql".
If you used a different user name, then you have to specify this user name in
(1) after the option "-u" and a different database name after "-p".


Appendix C: MySQL-Versions
--------------------------

In order to ensure a high level of software quality we were forced to limit
the release to a single MySQL version. The major reason for this is that the
different MySQL versions are of very different quality levels and that there
are really versions where even base database functionality does not work.
We had very good results using MySQL 5.0.19 and this is why we used this
version to do all our final testing. Please in any case do not use MySQL
5.0.21 -- here we know that there are a lot of problems.
