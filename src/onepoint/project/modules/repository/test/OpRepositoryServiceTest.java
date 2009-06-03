/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.repository.test;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.documents.OpDocumentNode;
import onepoint.project.modules.documents.OpFolder;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpAttachmentIfc;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.project.modules.project.OpToDo;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.repository.OpRepositoryError;
import onepoint.project.modules.repository.OpRepositoryService;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserError;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.modules.work.OpCostRecord;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceException;
import onepoint.service.server.XSession;

/**
 * Test-classe for repository services.
 *
 * @author lucian.furtos
 */
public class OpRepositoryServiceTest extends OpBaseOpenTestCase {

   private static final String DEFAULT_USER = "tester";
   private static final String DEFAULT_PASSWORD = "pass";

   private static final String BACKUP_DIRECTORY = "backup";
   private static final String OLDER_VERSION_DEMODATA_FILE_NAME = "older_version_demodata.opx";
   private static final String RESTORE_FILE_NAME = "test_restore.xml";
   private static final String DOCUMENTS_BACKUP = "documents_backup";
   private static final String ATTACHMENTS_BACKUP = "attachments_backup";
   private static final String ATTACHMENT_VERSION_BACKUP = "attachmentVersion_backup";
   private static final String REPORTS_BACKUP = "reports_backup";
   private static final String ALL_OBJECTS_BACKUP = "allObjects_backup";
   private File backupFolder;
   private List<String> backupFileNames = null;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   @Override
   protected void setUp()
        throws Exception {
      super.setUp();
      backupFolder = OpTestDataFactory.getRepositoryService().getBackupFolder(session);
      backupFileNames = new ArrayList<String>();
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   @Override
   protected void tearDown()
        throws Exception {
      logOut();
      logIn();

      reset();
      clean();
      super.tearDown();
   }

   /**
    * Delete all back-up files from file system.
    */
   @Override
   protected void clean()
        throws Exception {
      super.clean();
      File[] backupFiles = backupFolder.listFiles(new FileFilter() {

         /**
          * Tests whether or not the specified abstract pathname should be
          * included in a pathname list.
          *
          * @param pathname The abstract pathname to be tested
          * @return <code>true</code> if and only if <code>pathname</code>
          *         should be included
          */
         public boolean accept(File pathname) {
            String fileName = pathname.getName();
            return Collections.binarySearch(backupFileNames, fileName, new Comparator<String>() {
               public int compare(String s1, String s2) {
                  if (s1 == null || s2 == null) {
                     return -1;
                  }
                  if (s1.startsWith(s2) || s2.startsWith(s1)) {
                     return 0;
                  }
                  return s1.compareTo(s2);
               }
            }) > -1;
         }
      });

      for (File backupFile : backupFiles) {
         if (backupFile.isFile()) {
            backupFile.delete();
         }
         else {
            deleteFolder(backupFile);
         }
      }
   }

   /**
    * Delete a folder recursively.
    *
    * @param folder folder to delete.
    */
   private void deleteFolder(File folder) {
      File[] children = folder.listFiles();
      for (File child : children) {
         if (child.isFile()) {
            child.delete();
         }
         else {
            deleteFolder(child);
         }
      }

      folder.delete();
   }

   public void testBackUp()
        throws Exception {
      XMessage response = doBackup();
      assertNoError(response);

      String fileName = (String) response.getArgument(OpRepositoryService.BACKUP_FILENAME_PARAMETER);
      assertTrue(new File(backupFolder, fileName).exists());
      //todo: test data from XML
   }

   /**
    * Restores the database from a backup file made from the current state of the database
    *
    * @throws Exception if anything fails.
    */
   public void testRestoreFromCurrentState()
        throws Exception {
      XMessage response = doBackup();
      assertNoError(response);
      String fileName = (String) response.getArgument(OpRepositoryService.BACKUP_FILENAME_PARAMETER);
      File file = new File(backupFolder, fileName);
      assertTrue(file.exists());

      XSession newSession = server.newSession();
      assertTrue(newSession.isValid());

      XMessage request = new XMessage();
      request.setArgument("restoreFile", file.getCanonicalPath());
      response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertNoError(response);
      assertFalse(newSession.isValid());
      newSession.close();
   }

//   /**
//    * Restores the database from a backup present file
//    *
//    * @throws Exception if anything fails.
//    */
//   public void testRestoreDemoData()
//        throws Exception {
//      File file = new File(backupFolder, RESTORE_FILE_NAME);
//      assertTrue(file.exists());
//
//      XSession newSession = server.newSession();
//      assertTrue(newSession.isValid());
//
//      XMessage request = new XMessage();
//      request.setArgument("restoreFile", file.getCanonicalPath());
//      XMessage response = OpTestDataFactory.getRepositoryService().restore(session, request);
//      assertNoError(response);
//      assertFalse(newSession.isValid());
//      newSession.close();
//   }

   public void testRestoreError()
        throws Exception {
      XMessage request = new XMessage();
      request.setArgument("restoreFile", backupFolder.getAbsolutePath());
      XMessage response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertError(response, OpRepositoryError.RESTORE_ERROR_CODE);

      request = new XMessage();
      request.setArgument("restoreFile", "nosuchfile");
      response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertError(response, OpRepositoryError.RESTORE_ERROR_CODE);
   }

   public void testReset()
        throws Exception {
      assertNoError(reset());
   }

   /**
    * Resets the schema.
    *
    * @return an <code>XMessage</code>.
    */
   private XMessage reset() {
      XMessage request = new XMessage();
      request.setArgument(OpRepositoryService.ADMIN_PASSWORD_PARAMETER, OpUser.BLANK_PASSWORD);

      return OpTestDataFactory.getRepositoryService().reset(session, request);
   }

   public void testPermission()
        throws Exception {
      Map userData = OpUserTestDataFactory.createUserData(DEFAULT_USER, DEFAULT_PASSWORD, OpUser.CONTRIBUTOR_USER_LEVEL);
      userData.put(OpUserService.LANGUAGE, "en");
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = OpTestDataFactory.getUserService().insertUser(session, request);
      assertNoError(response);
      logIn(DEFAULT_USER, DEFAULT_PASSWORD);

      boolean exceptionThrown = false;
      try {
         doBackup();
      }
      catch (XServiceException e) {
         exceptionThrown = true;
         assertEquals("OpPermissionCheckServiceInterceptor failed for method in OpRepositoryService", OpUserError.INSUFFICIENT_PRIVILEGES, e.getError().getCode());
      }
      assertTrue("OpPermissionCheckServiceInterceptor failed for method in OpRepositoryService, exception should have been thrown", exceptionThrown);

      exceptionThrown = false;
      try {
         OpTestDataFactory.getRepositoryService().restore(session, new XMessage());
      }
      catch (XServiceException e) {
         exceptionThrown = true;
         assertEquals("OpPermissionCheckServiceInterceptor failed for method in OpRepositoryService", OpUserError.INSUFFICIENT_PRIVILEGES, e.getError().getCode());
      }
      assertTrue("OpPermissionCheckServiceInterceptor failed for method in OpRepositoryService, exception should have been thrown", exceptionThrown);

      exceptionThrown = false;
      try {
         OpTestDataFactory.getRepositoryService().reset(session, new XMessage());
      }
      catch (XServiceException e) {
         exceptionThrown = true;
         assertEquals("OpPermissionCheckServiceInterceptor failed for method in OpRepositoryService", OpUserError.INSUFFICIENT_PRIVILEGES, e.getError().getCode());
      }
      assertTrue("OpPermissionCheckServiceInterceptor failed for method in OpRepositoryService, exception should have been thrown", exceptionThrown);
   }

   /**
    * Restores the database from a backup file made from an older version of the application
    *
    * @throws Exception if anything fails.
    */
   public void testRestoreFromOlderVersion()
        throws Exception {
      assertNoError(this.reset());
      logIn();
      File file = new File(OpEnvironmentManager.getOnePointHome() + File.separator + BACKUP_DIRECTORY,
           OLDER_VERSION_DEMODATA_FILE_NAME + OpRepositoryService.BACKUP_FILE_EXTENSION);
      assertTrue(file.exists());

      XSession newSession = server.newSession();
      assertTrue(newSession.isValid());

      XMessage request = new XMessage();
      request.setArgument("restoreFile", file.getCanonicalPath());
      XMessage response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertNoError(response);
      assertFalse(newSession.isValid());
      newSession.close();
   }

   public void testResetWithOlderVersion()
        throws Exception {
      assertNoError(this.reset());
      logIn();
      File file = new File(OpEnvironmentManager.getOnePointHome() + File.separator + BACKUP_DIRECTORY,
           OLDER_VERSION_DEMODATA_FILE_NAME + OpRepositoryService.BACKUP_FILE_EXTENSION);
      assertTrue(file.exists());

      XSession newSession = server.newSession();
      assertTrue(newSession.isValid());

      XMessage request = new XMessage();
      request.setArgument("restoreFile", file.getCanonicalPath());
      XMessage response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertNoError(response);
      assertFalse(newSession.isValid());

      //reset the imported schema
      newSession.close();
      newSession = server.newSession();
      logIn();
      request = new XMessage();
      request.setArgument(OpRepositoryService.ADMIN_PASSWORD_PARAMETER, OpUser.BLANK_PASSWORD);
      response = OpTestDataFactory.getRepositoryService().reset(session, request);
      assertNoError(response);
      newSession.close();
   }

   /**
    * Restores the database from a backup file in order to test the restoring of document contents.
    * The backup structure should be: OpProjectNode (project) -> OpFolder (rootFolder) -> OpDocumentNode (with content)
    *                                                                                  -> OpDocumentNode (linked)
    * @throws Exception if the test fails
    */
   public void testRestoreDocumentContents()
        throws Exception {
      assertNoError(this.reset());
      logIn();
      File file = new File(OpEnvironmentManager.getOnePointHome() + File.separator + BACKUP_DIRECTORY,
           DOCUMENTS_BACKUP + OpRepositoryService.BACKUP_FILE_EXTENSION);
      assertTrue(file.exists());

      XSession newSession = server.newSession();
      assertTrue(newSession.isValid());

      XMessage request = new XMessage();
      request.setArgument("restoreFile", file.getCanonicalPath());
      XMessage response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertNoError(response);
      assertFalse(newSession.isValid());
      newSession.close();

      OpBroker broker = session.newBroker();
      try {
         OpQuery projectQuery = broker.newQuery("select project from OpProjectNode as project where project.Type = ?");
         projectQuery.setLong(0, OpProjectNode.PROJECT);
         List projects = broker.list(projectQuery);
         assertEquals(1, projects.size());
         OpProjectNode project = (OpProjectNode) projects.get(0);
         assertEquals("project", project.getName());
         assertEquals(1, project.getFolders().size());
         OpFolder rootFolder = null;
         for (OpFolder folder : project.getFolders()) {
            rootFolder = folder;
         }
         assertNotNull(rootFolder);
         assertEquals(2, rootFolder.getDocumentNodes().size());
         for (OpDocumentNode documentNode : rootFolder.getDocumentNodes()) {
            assertTrue(documentNode.getName().equals("documentPicture") || documentNode.getName().equals("documentLink"));
            assertNull(documentNode.getDescription());
            assertEquals(rootFolder.getId(), documentNode.getFolder().getId());
            if (documentNode.getName().equals("documentPicture")) {
               assertFalse(documentNode.isLinked());
               assertNotNull(documentNode.getContent());
            }
            else {
               assertTrue(documentNode.isLinked());
               assertEquals("http://www.google.com", documentNode.getLocation());
            }
         }
      }
      finally{
         broker.close();
      }
   }

   /**
    * Restores the database from a backup file in order to test the restoring of attachment contents.
    * The backup structure should be: OpProjectNode (project) -> OpActivity (activity) -> OpAttachment (with content)
    *                                                                                  -> OpAttachment (linked)
    *                                                         -> OpActivity (task)     -> OpAttachment (with content)
    *                                                                                  -> OpAttachment (linked)
    *                                                         -> OpCostRecord          -> OpAttachment (with content)
    *                                                                                  -> OpAttachment (linked)
    * @throws Exception if the test fails
    */
   public void testRestoreAttachmentContents()
        throws Exception {
      assertNoError(this.reset());
      logIn();
      File file = new File(OpEnvironmentManager.getOnePointHome() + File.separator + BACKUP_DIRECTORY,
           ATTACHMENTS_BACKUP + OpRepositoryService.BACKUP_FILE_EXTENSION);
      assertTrue(file.exists());

      XSession newSession = server.newSession();
      assertTrue(newSession.isValid());

      XMessage request = new XMessage();
      request.setArgument("restoreFile", file.getCanonicalPath());
      XMessage response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertNoError(response);
      assertFalse(newSession.isValid());
      newSession.close();

      OpBroker broker = session.newBroker();
      try {
         OpQuery projectQuery = broker.newQuery("select project from OpProjectNode as project where project.Type = ?");
         projectQuery.setLong(0, OpProjectNode.PROJECT);
         List projects = broker.list(projectQuery);
         assertEquals(1, projects.size());
         OpProjectNode project = (OpProjectNode) projects.get(0);
         assertEquals("project", project.getName());
         assertEquals(2, project.getPlan().getActivities().size());
         OpActivity projectActivity = null;
         OpActivity adhocTaskActivity = null;
         for (OpActivity activity : project.getPlan().getActivities()) {
            if (activity.getType() == OpActivity.STANDARD) {
               projectActivity = activity;
            }
            else {
               adhocTaskActivity = activity;
            }
         }

         //test the project activity attachments
         assertNotNull(projectActivity);
         assertEquals(2, projectActivity.getAttachments().size());
         for (OpAttachmentIfc attachment : projectActivity.getAttachments()) {
            assertTrue(attachment.getName().equals("activityPicture") || attachment.getName().equals("activityLink"));
            assertNotNull(attachment.getActivityIfc());
            assertEquals(projectActivity.getId(), attachment.getActivityIfc().getId());
            if (attachment.getName().equals("activityPicture")) {
               assertFalse(attachment.getLinked());
               assertNotNull(attachment.getContent());
            }
            else {
               assertTrue(attachment.getLinked());
               assertEquals("http://www.yahoo.com", attachment.getLocation());
            }
         }

         //test the adhoc task attachments
         assertNotNull(adhocTaskActivity);
         assertEquals(2, adhocTaskActivity.getAttachments().size());
         for (OpAttachmentIfc attachment : adhocTaskActivity.getAttachments()) {
            assertTrue(attachment.getName().equals("taskPicture") || attachment.getName().equals("taskLink"));
            assertNotNull(attachment.getActivityIfc());
            assertEquals(adhocTaskActivity.getId(), attachment.getActivityIfc().getId());
            if (attachment.getName().equals("taskPicture")) {
               assertFalse(attachment.getLinked());
               assertNotNull(attachment.getContent());
            }
            else {
               assertTrue(attachment.getLinked());
               assertEquals("http://www.google.com", attachment.getLocation());
            }
         }

         //test the cost record attachments
         assertEquals(1, projectActivity.getAssignments().size());
         OpAssignment activityAssignment = null;
         for (OpAssignment assignment : projectActivity.getAssignments()) {
            activityAssignment = assignment;
         }

         assertNotNull(activityAssignment);
         assertEquals(1, activityAssignment.getWorkRecords().size());
         OpWorkRecord activityWR = null;
         for (OpWorkRecord workRecord : activityAssignment.getWorkRecords()) {
            activityWR = workRecord;
         }
         assertNotNull(activityWR);

         assertEquals(1, activityWR.getCostRecords().size());
         OpCostRecord costRecord = null;
         for (OpCostRecord cRecord : activityWR.getCostRecords()) {
            costRecord = cRecord;
         }

         assertNotNull(costRecord);
         assertEquals(2, costRecord.getAttachments().size());
         for (OpAttachment attachment : costRecord.getAttachments()) {
            assertTrue(attachment.getName().equals("costPicture") || attachment.getName().equals("costLink"));
            assertNull(attachment.getActivity());
            assertNotNull(attachment.getCostRecord());
            assertEquals(costRecord.getId(), attachment.getCostRecord().getId());
            if (attachment.getName().equals("costPicture")) {
               assertFalse(attachment.getLinked());
               assertNotNull(attachment.getContent());
            }
            else {
               assertTrue(attachment.getLinked());
               assertEquals("http://www.amazon.com", attachment.getLocation());
            }
         }
      }
      finally {
         broker.close();
      }
   }

   /**
    * Restores the database from a backup file in order to test the restoring of attachment version contents.
    * The backup structure should be: OpProjectNode (project) -> OpActivity (activity) -> OpAttachment (with content)
    *                                                                                  -> OpAttachment (linked)
    *                                                         -> OpActivityVersion (activity) -> OpAttachmentVersion (with content)
    *                                                                                        -> OpAttachmentVersion (linked)
    *
    * @throws Exception if the test fails
    */
   public void testRestoreAttachmentVersionContents()
        throws Exception {
      assertNoError(this.reset());
      logIn();
      File file = new File(OpEnvironmentManager.getOnePointHome() + File.separator + BACKUP_DIRECTORY,
           ATTACHMENT_VERSION_BACKUP + OpRepositoryService.BACKUP_FILE_EXTENSION);
      assertTrue(file.exists());

      XSession newSession = server.newSession();
      assertTrue(newSession.isValid());

      XMessage request = new XMessage();
      request.setArgument("restoreFile", file.getCanonicalPath());
      XMessage response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertNoError(response);
      assertFalse(newSession.isValid());
      newSession.close();

      OpBroker broker = session.newBroker();
      try {
         OpQuery projectQuery = broker.newQuery("select project from OpProjectNode as project where project.Type = ?");
         projectQuery.setLong(0, OpProjectNode.PROJECT);
         List projects = broker.list(projectQuery);
         assertEquals(1, projects.size());
         OpProjectNode project = (OpProjectNode) projects.get(0);
         assertEquals("project", project.getName());
         assertEquals(1, project.getPlan().getActivities().size());
         OpActivity projectActivity = null;
         for (OpActivity activity : project.getPlan().getActivities()) {
            projectActivity = activity;
         }

         assertNotNull(projectActivity);
         assertEquals(1, projectActivity.getVersions().size());
         OpActivityVersion activityVersion = null;
         for (OpActivityVersion version : projectActivity.getVersions()) {
            activityVersion = version;
         }

         assertNotNull(activityVersion);
         assertEquals(2, activityVersion.getAttachmentVersions().size());
         for (OpAttachmentIfc attachmentVersion : activityVersion.getAttachmentVersions()) {
            assertTrue(attachmentVersion.getName().equals("activityPicture") || attachmentVersion.getName().equals("activityLink"));
            assertNotNull(attachmentVersion.getActivityIfc());
            assertEquals(activityVersion.getId(), attachmentVersion.getActivityIfc().getId());
            if (attachmentVersion.getName().equals("activityPicture")) {
               assertFalse(attachmentVersion.getLinked());
               assertNotNull(attachmentVersion.getContent());
            }
            else {
               assertTrue(attachmentVersion.getLinked());
               assertEquals("http://www.google.com", attachmentVersion.getLocation());
            }
         }
      }
      finally {
         broker.close();
      }
   }

   /**
    * Restores the database from a backup file in order to test the restoring of report contents.
    * The backup structure should be: OpProjectNode (project) -> OpReport (with content, associated with the project)
    *                                                         -> OpReport (with content, associated with the project)
    *                                 OpReport (with content, not associated with the project)
    *
    * @throws Exception if the test fails
    */
   public void testRestoreReportContents()
        throws Exception {
      assertNoError(this.reset());
      logIn();
      File file = new File(OpEnvironmentManager.getOnePointHome() + File.separator + BACKUP_DIRECTORY,
           REPORTS_BACKUP + OpRepositoryService.BACKUP_FILE_EXTENSION);
      assertTrue(file.exists());

      XSession newSession = server.newSession();
      assertTrue(newSession.isValid());

      XMessage request = new XMessage();
      request.setArgument("restoreFile", file.getCanonicalPath());
      XMessage response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertNoError(response);
      assertFalse(newSession.isValid());
      newSession.close();

      OpBroker broker = session.newBroker();
      try {
         OpQuery projectQuery = broker.newQuery("select project from OpProjectNode as project where project.Type = ?");
         projectQuery.setLong(0, OpProjectNode.PROJECT);
         List projects = broker.list(projectQuery);
         assertEquals(1, projects.size());
         OpProjectNode project = (OpProjectNode) projects.get(0);
         assertEquals("project", project.getName());

         //test the existence of 2 reports for the project
         assertEquals(2, project.getReports().size());
         for (OpReport report : project.getReports()) {
            assertNotNull(report.getContent());
            assertTrue(report.getType().getName().equals("projectprogress") || report.getType().getName().equals("resourceallocation"));
         }

         //test the existance of a third report, which is not associated with the project
         OpQuery reportQuery = broker.newQuery("select report from OpReport as report where report.Project = null");
         List reports = broker.list(reportQuery);
         assertEquals(1, reports.size());
         OpReport report = (OpReport) reports.get(0);
         assertNotNull(report.getContent());
         assertEquals("workreport", report.getType().getName());
      }
      finally {
         broker.close();
      }
   }

   /**
    * Restores the database from a backup file in order to test the restoring of a certain hierarchy of objects.
    *
    * @throws Exception if the test fails
    */
   public void testRestoreAllObjects()
        throws Exception {
      assertNoError(this.reset());
      logIn();
      File file = new File(OpEnvironmentManager.getOnePointHome() + File.separator + BACKUP_DIRECTORY,
           ALL_OBJECTS_BACKUP + OpRepositoryService.BACKUP_FILE_EXTENSION);
      assertTrue(file.exists());

      XSession newSession = server.newSession();
      assertTrue(newSession.isValid());

      XMessage request = new XMessage();
      request.setArgument("restoreFile", file.getCanonicalPath());
      XMessage response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertNoError(response);
      assertFalse(newSession.isValid());
      newSession.close();

      OpBroker broker = session.newBroker();
      try {
         //test the existence of 2 projects (project1 and project2) and a portfolio (portfolio1 which contains project1)
         OpQuery projectQuery = broker.newQuery("select project from OpProjectNode as project where project.SuperNode = null");
         List projects = broker.list(projectQuery);
         assertEquals(1, projects.size());
         OpProjectNode rootPortfolio = (OpProjectNode) projects.get(0);

         assertEquals(2, rootPortfolio.getSubNodes().size());
         OpProjectNode porfolio1 = null;
         OpProjectNode project1 = null;
         OpProjectNode project2 = null;
         Iterator it = rootPortfolio.getSubNodes().iterator();
         while (it.hasNext()) {
            OpProjectNode project = (OpProjectNode) it.next();
            if (project.getName().equals("portfolio1")) {
               porfolio1 = project;
            }
            else {
               project2 = project;
            }
         }
         assertNotNull(porfolio1);
         assertNotNull(project2);
         assertEquals(1, porfolio1.getSubNodes().size());
         it = porfolio1.getSubNodes().iterator();
         while (it.hasNext()) {
            project1 = (OpProjectNode) it.next();
         }
         assertNotNull(project1);

         //test the fields of the two projects and the portfolio
         assertEquals("portfolio1", porfolio1.getName());
         assertEquals(OpProjectNode.PORTFOLIO, porfolio1.getType().byteValue());
         assertNull(porfolio1.getDescription());
         assertNull(porfolio1.getStart());
         assertNull(porfolio1.getFinish());
         assertEquals(5, porfolio1.getPriority().intValue());
         assertEquals(100, porfolio1.getProbability().intValue());
         assertEquals(rootPortfolio.getId(), porfolio1.getSuperNode().getId());
         assertNull(porfolio1.getStatus());
         assertFalse(porfolio1.getArchived());

         //project1
         assertEquals("project1", project1.getName());
         assertEquals(OpProjectNode.PROJECT, project1.getType().byteValue());
         assertNull(project2.getDescription());
         assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 4).getTimeInMillis()), project1.getStart());
         assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 28).getTimeInMillis()), project1.getFinish());
         assertEquals(5, project1.getPriority().intValue());
         assertEquals(100, project1.getProbability().intValue());
         assertEquals(porfolio1.getId(), project1.getSuperNode().getId());
         assertNotNull(project1.getStatus());
         assertFalse(project1.getArchived());

         //project2
         assertEquals("project2", project2.getName());
         assertEquals(OpProjectNode.PROJECT, project2.getType().byteValue());
         assertNull(project2.getDescription());
         assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 4).getTimeInMillis()), project2.getStart());
         assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 29).getTimeInMillis()), project2.getFinish());
         assertEquals(5, project2.getPriority().intValue());
         assertEquals(100, project2.getProbability().intValue());
         assertEquals(rootPortfolio.getId(), project2.getSuperNode().getId());
         assertNotNull(project2.getStatus());
         assertFalse(project2.getArchived());

         //test that project1 has a document node
         assertEquals(1, project1.getFolders().size());
         OpFolder rootFolder = null;
         for (OpFolder folder : project1.getFolders()) {
            rootFolder = folder;
         }
         assertNotNull(rootFolder);
         assertEquals(1, rootFolder.getDocumentNodes().size());
         for (OpDocumentNode documentNode : rootFolder.getDocumentNodes()) {
            assertTrue(documentNode.getName().equals("project1Link"));
            assertNull(documentNode.getDescription());
            assertEquals(rootFolder.getId(), documentNode.getFolder().getId());
            assertTrue(documentNode.isLinked());
            assertEquals("http://www.google.com", documentNode.getLocation());
         }

         //test that project1 has a todo
         assertEquals(1, project1.getToDos().size());
         for (OpToDo todo : project1.getToDos()) {
            assertEquals("goal1", todo.getName());
            assertEquals(2, todo.getPriority());
            assertFalse(todo.getCompleted());
            assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 21).getTimeInMillis()), todo.getDue());
            assertEquals(project1.getId(), todo.getProjectNode().getId());
         }

         //test that project1 has a status
         assertNotNull(project1.getStatus());
         OpProjectStatus status1 = project1.getStatus();
         assertEquals("initiated", status1.getName());
         assertNull(status1.getDescription());
         assertEquals(1, status1.getProjects().size());
         for (OpProjectNode project : status1.getProjects()) {
            assertEquals(project1.getId(), project.getId());
         }

         //test the activities of project1
         OpActivity proj1Activity1 = null;
         assertEquals(2, project1.getPlan().getActivities().size());
         for (OpActivity activity : project1.getPlan().getActivities()) {
            assertTrue(activity.getName().equals("proj1Activity1") || activity.getName().equals("proj1Activity2"));
            assertNull(activity.getDescription());
            assertEquals(0d, activity.getBaseTravelCosts());
            assertEquals(0d, activity.getBaseMaterialCosts());
            assertEquals(0d, activity.getBaseExternalCosts());
            assertEquals(0d, activity.getBaseMiscellaneousCosts());
            assertEquals(0d, activity.getActualTravelCosts());
            assertEquals(0d, activity.getRemainingTravelCosts());
            assertEquals(0d, activity.getActualMaterialCosts());
            assertEquals(0d, activity.getRemainingMaterialCosts());
            assertEquals(0d, activity.getActualExternalCosts());
            assertEquals(0d, activity.getRemainingExternalCosts());
            assertEquals(0d, activity.getActualMiscellaneousCosts());
            assertEquals(0d, activity.getRemainingMiscellaneousCosts());
            assertEquals(project1.getPlan().getId(), activity.getProjectPlan().getId());
            assertNull(activity.getSuperActivity());
            if (activity.getName().equals("proj1Activity1")) {
               proj1Activity1 = activity;
               assertEquals((Byte) OpActivity.STANDARD, new Byte(activity.getType()));
               assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 11).getTimeInMillis()), activity.getStart());
               assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 15).getTimeInMillis()), activity.getFinish());
               assertEquals(40d, activity.getDuration());
               assertEquals(25d, activity.getComplete());
               assertEquals(5, activity.getPriority());
               assertEquals(20d, activity.getBaseEffort());
               assertEquals(40d, activity.getBasePersonnelCosts());
               assertEquals(5d, activity.getActualEffort());
               assertEquals(10d, activity.getActualPersonnelCosts());
               assertEquals(30d, activity.getRemainingPersonnelCosts());
               assertEquals(15d, activity.getOpenEffort());
               assertEquals(100d, activity.getBaseProceeds());
               assertEquals(25d, activity.getActualProceeds());
               assertEquals(75d, activity.getRemainingProceeds());
            }
            if (activity.getName().equals("proj1Activity2")) {
               assertEquals((Byte) OpActivity.MILESTONE, new Byte(activity.getType()));
               assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 21).getTimeInMillis()), activity.getStart());
               assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 21).getTimeInMillis()), activity.getFinish());
               assertEquals(0d, activity.getDuration());
               assertEquals(0d, activity.getComplete());
               assertEquals(0, activity.getPriority());
               assertEquals(0d, activity.getBaseEffort());
               assertEquals(0d, activity.getBasePersonnelCosts());
               assertEquals(0d, activity.getActualEffort());
               assertEquals(0d, activity.getActualPersonnelCosts());
               assertEquals(0d, activity.getRemainingPersonnelCosts());
               assertEquals(0d, activity.getOpenEffort());
               assertEquals(0d, activity.getBaseProceeds());
               assertEquals(0d, activity.getActualProceeds());
               assertEquals(0d, activity.getRemainingProceeds());
            }
         }

         //test the existence of two resources one assigned to each project
         OpResource resource1 = null;
         OpResource resource2 = null;
         OpQuery resourceQuery = broker.newQuery("from OpProjectNodeAssignment");
         List<OpProjectNodeAssignment> projectAssignments = broker.list(resourceQuery);
         assertEquals(2, projectAssignments.size());
         for (OpProjectNodeAssignment projectAssignment : projectAssignments) {
            assertNotNull(projectAssignment.getResource());
            assertNotNull(projectAssignment.getProjectNode());
            assertNull(projectAssignment.getHourlyRate());
            assertNull(projectAssignment.getExternalRate());
            if (projectAssignment.getProjectNode().getId() == project1.getId()) {
               resource1 = projectAssignment.getResource();
            }
            else {
               resource2 = projectAssignment.getResource();
            }
         }

         assertNotNull(resource1);
         assertEquals("resource1", resource1.getName());
         assertNull(resource1.getDescription());
         assertEquals(50d, resource1.getAvailable());
         assertFalse(resource1.getInheritPoolRate());
         assertEquals(2d, resource1.getHourlyRate());
         assertEquals(5d, resource1.getExternalRate());

         assertNotNull(resource2);
         assertEquals("resource2", resource2.getName());
         assertNull(resource2.getDescription());
         assertEquals(100d, resource2.getAvailable());
         assertTrue(resource2.getInheritPoolRate());
         assertEquals(0d, resource2.getHourlyRate());
         assertEquals(0d, resource2.getExternalRate());

         //test the existence of a work record on activity1
         assertNotNull(proj1Activity1);
         assertEquals(1, proj1Activity1.getAssignments().size());
         OpWorkRecord workRecord = null;
         for (OpAssignment assignment : proj1Activity1.getAssignments()) {
            assertEquals(1, assignment.getWorkRecords().size());
            for (OpWorkRecord wr : assignment.getWorkRecords()) {
               workRecord = wr;
            }
         }
         assertEquals(5d, workRecord.getActualEffort());
         assertEquals(15d, workRecord.getRemainingEffort());
         assertEquals(10d, workRecord.getPersonnelCosts());
         assertEquals(0d, workRecord.getTravelCosts());
         assertEquals(0d, workRecord.getRemTravelCosts());
         assertEquals(0d, workRecord.getMaterialCosts());
         assertEquals(0d, workRecord.getRemMaterialCosts());
         assertEquals(0d, workRecord.getExternalCosts());
         assertEquals(0d, workRecord.getRemExternalCosts());
         assertEquals(0d, workRecord.getMiscellaneousCosts());
         assertEquals(0d, workRecord.getRemMiscCosts());
         assertEquals(25d, workRecord.getActualProceeds());
         assertFalse(workRecord.getCompleted());
         assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 21).getTimeInMillis()), workRecord.getWorkSlip().getDate());

         //test that project2 has a document node
         assertEquals(1, project2.getFolders().size());
         rootFolder = null;
         for (OpFolder folder : project2.getFolders()) {
            rootFolder = folder;
         }
         assertNotNull(rootFolder);
         assertEquals(1, rootFolder.getDocumentNodes().size());
         for (OpDocumentNode documentNode : rootFolder.getDocumentNodes()) {
            assertTrue(documentNode.getName().equals("proj2Document"));
            assertEquals("a document", documentNode.getDescription());
            assertEquals(rootFolder.getId(), documentNode.getFolder().getId());
            assertFalse(documentNode.isLinked());
            assertNotNull(documentNode.getContent());
         }

         //test that project2 has a status
         assertNotNull(project2.getStatus());
         OpProjectStatus status2 = project2.getStatus();
         assertEquals("terminated", status2.getName());
         assertNull(status2.getDescription());
         assertEquals(1, status2.getProjects().size());
         for (OpProjectNode project : status2.getProjects()) {
            assertEquals(project2.getId(), project.getId());
         }

         //test the activity of project2
         OpActivity proj2Activity1 = null;
         assertEquals(1, project2.getPlan().getActivities().size());
         for (OpActivity activity : project2.getPlan().getActivities()) {
            proj2Activity1 = activity;
            assertTrue(activity.getName().equals("proj2Activity1"));
            assertNull(activity.getDescription());
            assertEquals((Byte) OpActivity.STANDARD, new Byte(activity.getType()));
            assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 5).getTimeInMillis()), activity.getStart());
            assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 7).getTimeInMillis()), activity.getFinish());
            assertEquals(24d, activity.getDuration());
            assertEquals(0d, activity.getComplete());
            assertEquals(1, activity.getPriority());
            assertEquals(24d, activity.getBaseEffort());
            assertEquals(10d, activity.getBaseTravelCosts());
            assertEquals(0d, activity.getBasePersonnelCosts());
            assertEquals(15d, activity.getBaseMaterialCosts());
            assertEquals(20d, activity.getBaseExternalCosts());
            assertEquals(0d, activity.getBaseMiscellaneousCosts());
            assertEquals(0d, activity.getActualEffort());
            assertEquals(0d, activity.getActualTravelCosts());
            assertEquals(10d, activity.getRemainingTravelCosts());
            assertEquals(0d, activity.getActualPersonnelCosts());
            assertEquals(0d, activity.getRemainingPersonnelCosts());
            assertEquals(3d, activity.getActualMaterialCosts());
            assertEquals(12d, activity.getRemainingMaterialCosts());
            assertEquals(0d, activity.getActualExternalCosts());
            assertEquals(20d, activity.getRemainingExternalCosts());
            assertEquals(0d, activity.getActualMiscellaneousCosts());
            assertEquals(0d, activity.getRemainingMiscellaneousCosts());
            assertEquals(24d, activity.getOpenEffort());
            assertEquals(0d, activity.getBaseProceeds());
            assertEquals(0d, activity.getActualProceeds());
            assertEquals(0d, activity.getRemainingProceeds());
            assertEquals(project2.getPlan().getId(), activity.getProjectPlan().getId());
            assertNull(activity.getSuperActivity());
         }

         //test the existence of a cost record on activity1 of project2
         assertNotNull(proj2Activity1);
         assertEquals(1, proj2Activity1.getAssignments().size());
         OpCostRecord costRecord = null;
         for (OpAssignment assignment : proj2Activity1.getAssignments()) {
            assertEquals(1, assignment.getWorkRecords().size());
            for (OpWorkRecord wr : assignment.getWorkRecords()) {
               assertEquals(1, wr.getCostRecords().size());
               for (OpCostRecord cr : wr.getCostRecords()) {
                  costRecord = cr;
               }
            }
         }
         assertEquals((Byte) OpCostRecord.MATERIAL_COST, costRecord.getType());
         assertEquals(3d, costRecord.getActualCosts());
         assertEquals(12d, costRecord.getRemainingCosts());
         assertEquals(new Date(getCalendarWithExactDaySet(2008, 1, 22).getTimeInMillis()), costRecord.getWorkRecord().getWorkSlip().getDate());
      }
      finally {
         broker.close();
      }
   }

   /**
    * Backs up a repository and saves the name of the backup file for clean-up.
    *
    * @return a <code>XMessage</code> the service response.
    */
   private XMessage doBackup() {
      XMessage response = OpTestDataFactory.getRepositoryService().backup(session, new XMessage());
      String fileName = (String) response.getArgument(OpRepositoryService.BACKUP_FILENAME_PARAMETER);
      backupFileNames.add(fileName);
      return response;
   }
}