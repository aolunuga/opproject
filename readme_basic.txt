Onepoint Project 7.1 (Update 3) Basic Edition

==================================



Welcome to the most current release of Onepoint Project Basic Edition. This

package contains a single-user desktop version of Onepoint Project together

with a short user tutorial.





System Requirements

-------------------



* Microsoft Windows XP/Vista, Mac OS X 10.4, Linux 2.4 or later



* Java Runtime Environment (JRE) version 5 or later





Documentation

-------------



The ZIP package contains a tutorial about Onepoint Project which explains

the most important functions step by step. The tutorial is provided in the

form of an Acrobat/PDF file -- you can use Adobe's Acrobat Reader or a

Web browser with the Acrobat Reader plug-in (typically, your Web browser

already contains such a plug-in). In case you do not have Acrobot Reader

installed, you can download it for free from the Adobe website:



   http://www.adobe.com ("Get Adobe Reader")





IMPORTANT: Upgrading to New Embedded Database System

----------------------------------------------------



For scalability and performance reasons we decided to change the embedded

database from HSQL-DB to Derby starting with the 7.1 release. Please

note that HSQL-DB is still part of the distribution and you can still use

it. However, we strongly recommend to upgrade to the new database, because

it is much faster and we will not support HSQL-DB anymore in the future.



For security reasons, upgrading has to be done manually. In order to upgrade

to the new database you have to do the following. Please make sure that you

execute the steps in the correct order:



(1) Backup your current data to an XML file by using the "Repository" tool



(2) Open your configuration.oxc.xml file which is located in your Onepoint

Project installation folder by using a text editor (e.g., Notepad) and

write down the repository path part of the contents of the

<database-url>...</database-url> element -- you will need this for the new

repository to find your backup data again



(3) Install the new software as described under "Installation" below



(4) Start the new application and choose the same repository path you wrote

down in (2)



(5) You should now have a new version of Onepoint Project up and running

and you can restore your previously backuped data using the "Repository"

tool





Installation

------------



Under Windows extract the ZIP package to a location of your choice, e.g.,

C:\Program Files.



Under Mac OS X double-click the disk image in order to open it and drag the

"Onepoint Project" folder to a location of your choice, e.g., your

Applications folder.



Under Linux extract the TGZ package to a location of your choice, e.g.,

/usr/local.



When the "Onepoint Project" application is started the first time it will ask

you to to choose a location for your data directory. This directory will contain

the Onepoint project repository and all XML backup files that you will create.





Starting the Application

------------------------



Onepoint Project can be started by simply double-clicking the "Onepoint Project"

application (Windows/Mac OS X) or shell script (Linux), or by running it from

the command line.



