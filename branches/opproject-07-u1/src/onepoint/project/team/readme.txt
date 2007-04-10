Onepoint Project 07 Team Edition (Update 1)
===========================================

Welcome to the most current release of Onepoint Project Team Edition. This
package contains the Web application archive (WAR) of Onepoint Project together
with a short user tutorial.

Please note that all installation steps in this document assume that you are
using Tomcat and MySQL under Windows 2000/XP/Vista/2003 Server. If you are using
Linux or Mac OS X, or a different application server or database you will have to
adjust the installation steps according to your environment.


System Requirements
-------------------

* Microsoft Windows 2000/XP/Vista/2003 Server, Linux or Mac OS X

* Java Runtime Environment (JRE) version 1.4.2 or higher

* MySQL 5.0.27, PostgreSQL 8.1, IBM DB2 9, Oracle 10g, Microsoft SQL Server 2000

* Suitable JDBC driver for your chosen database

* Apache Tomcat 5.5

Other application servers can be supported on request.


Documentation
-------------

The ZIP package contains a tutorial about Onepoint Project which explains
the most important functions step by step. The tutorial is provided in the
form of an Acrobat/PDF file -- you can use Adobe's Acrobat Reader or a
Web browser with the Acrobat Reader plug-in (typically, your Web browser
already contains such a plug-in). In case you do not have Acrobot Reader
installed, you can download it for free from the Adobe website:

   hhttp://www.adobe.com ("Get Adobe Reader")


Installation
------------

If you are upgrading from Onepoint Project 06.1 Team Edition, please move your
existing "opproject" web application from the "webapps" folder to a different
location in the folder hierarchy.

These short instructions assume that Onepoint Project is installed as a Tomcat Web
application:

1. Install Tomcat 5.5 (if an installation does not yet exist)

2. Copy the "opproject.war" file from the ZIP package to the "wepapps" folder inside
the Tomcat installation. If you used an English language installer it is typically:

   C:\Program Files\Apache Software Foundation\Tomcat 5.5\webapps

Slightly afterwards or after you restart Tomcat, a folder "opproject" should appear
inside the webapps folder.

3. Copy the MySQL JDBC driver (JAR file) into the opproject\WEB-INF\lib folder

4. Install the MySQL database software and create an instance
   - Use UTF-8/Unicode as the default charset ("best support for multilingual...")

5. Create the MySQL database user ("opproject") and database ("opproject")
   - See Appendix A if you do not know how to do this in MySQL

6. If you upgraded from a previous version of Onepoint Project copy your
configuration file ("configuration.oxc.xml") to the new installation path

ATTENTION: Do not execute the following step if you are upgrading from an existing
installation!

7. (OPTIONAL) If you want to start with some demo data load the demo data by
executing the folling SQL file (see Appendix B):
   - demodata07.sql

Note: This step is optional. If you do not upload the demo data a new, plaim
repository will be created at the first startup of the web application.

8. Copy a valid license file ("license.oxl.xml") into the opproject folder

9. Stop Tomcat and restart it in order to make sure that the JDBC driver gets loaded
and your previous configuration file is loaded (in case you provided one).


Starting the Application
------------------------

If you installed Tomcat using the default settings then Onepoint Project should
be accessible by using the following URL:

   http://localhost:8080/opproject/service

If you did not upgrade from a previous installation you will now be presented with
the configuration wizard where you have to choose your database type (e.g., MySQL),
provide a JDBC connect string for your database instance, a database user and a
password. If you are not familiar with JDBC connect strings please take a look at
Appendix C.

Pleae note that for a totally new installation it can take a couple of minutes
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


Appendix A: Creating a New Database and a New Database User in MySQL
--------------------------------------------------------------------

In order to create a new database with the default values you can simply
execute the following steps after installing MySQL:

   (1) Open a command line window (DOS shell) und change into the "demodata"
       directory
   (2) Type "mysql –u root -p" and enter the root password which you specified
       when installing MySQL
   (3) mysql> source createdb.sql;
   (4) mysql> quit

Alternatively, you can create a new database and a new user in MySQL also
manually:

   (1) Open a command line window (DOS shell) und type "mysql –u root -p" and
       enter the root password which you specified when installing MySQL
   (2) mysql> create database opproject;
   (3) mysql> grant all privileges on opproject.* to 'opproject'@'localhost'
       identified by 'opproject' with grant option;
   (4) mysql> quit


Appendix B: Executing a SQL file in MySQL
-----------------------------------------

In order to execute a SQL file in MySQL you have to do the following:

   (1) Open a command line window (DOS shell) und type
       "mysql –u opproject -p opproject" and enter the password you specified
       when creating the database user
   (2) mysql> source demodata07.sql;
   (3) mysql> quit

Please note that this example assumes that the user and the database you
created in MySQL are called "opproject" and the SQL file "demodata07.sql".
If you used a different user name, then you have to specify this user name in
(1) after the option "-u" and a different database name after "-p".


Appendix C: Example Connect Strings for MySQL and PostgreSQL
------------------------------------------------------------

A complete discussion about JDBC connect strings is way beyond the scope of
this document, but we would like to provide you with example connect strings
for the most simple case (Tomcat and database are installed on the same
machine using their respective default port numbers).

For MySQL, the most simple connect string is:

	jdbc:mysql:///opproject
	
For PostgreSQL, the connect string is:

	jdbc:postgresql:opproject

In both cases "opproject" is the name of the database instance to connect to.
You will find more information about connect strings on the MySQL and
PostgreSQL web sites (http://www.mysql.com and http://www.postgresql.org)


Appendix D: Troubleshooting
---------------------------

* If the applet does not get loaded, but the Java Virtual Machine of the
browser is starting, please try to clear your Java cache on the client
computer. Under Windows, this can be done by going to the control panel,
selecting "Java" and then pressing the clear cache button

* If you get an error in the login screen saying that the JDBC driver was
not found then you probably forgot to copy the JDBC driver JAR file into
the WEB-INF/lib directory of the web application; please do it and restart
Tomcat afterwards in order to be sure that the JDBC driver JAR file gets
loaded correctly
