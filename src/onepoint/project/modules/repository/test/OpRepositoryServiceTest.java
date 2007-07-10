/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.repository.test;

import onepoint.project.modules.repository.OpRepositoryError;
import onepoint.project.modules.repository.OpRepositoryService;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO: Document HERE!!!
 *
 * @author lucian.furtos
 */
public class OpRepositoryServiceTest extends OpBaseOpenTestCase {

   private static final String DEFAULT_USER = "tester";
   private static final String DEFAULT_PASSWORD = "pass";
   private static final String BACKUP_ROOT_DIR = ".";
   private static final String BACKUP_CONTENT_PREFIX = "backup-";

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();
      cleanUp();
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      logOut();
      logIn();
      cleanUp();

      OpUserTestDataFactory usrData = new OpUserTestDataFactory(session);
      OpUser user = usrData.getUserByName(DEFAULT_USER);
      if (user != null) {
         List ids = new ArrayList();
         ids.add(user.locator());
         XMessage request = new XMessage();
         request.setArgument(OpUserService.SUBJECT_IDS, ids);
         OpTestDataFactory.getUserService().deleteSubjects(session, request);
      }

      super.tearDown();
   }

   /**
    * Delete all back-up files from file system.
    */
   private void cleanUp() {
      File rootDir = new File(BACKUP_ROOT_DIR);
      File[] backupFiles = rootDir.listFiles(new FileFilter() {

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
            return fileName.startsWith(BACKUP_CONTENT_PREFIX);
         }
      });

      for (int i = 0; i < backupFiles.length; i++) {
         File backupFile = backupFiles[i];
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
      for (int i = 0; i < children.length; i++) {
         File child = children[i];
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
      XMessage request = new XMessage();
      request.setArgument(OpRepositoryService.BACKUP_DIR_ROOT_PATH_PARAM, BACKUP_ROOT_DIR);

      XMessage response = OpTestDataFactory.getRepositoryService().backup(session, request);
      assertNoError(response);

      String fileName = (String) response.getArgument(OpRepositoryService.BACKUP_FILENAME_PARAMETER);
      assertTrue(new File(BACKUP_ROOT_DIR, fileName).exists());
      //todo: test data from XML
   }

   public void testBackUpErrors()
        throws Exception {
      XMessage request = new XMessage();
      request.setArgument(OpRepositoryService.BACKUP_DIR_ROOT_PATH_PARAM, "/");

      XMessage response = OpTestDataFactory.getRepositoryService().backup(session, request);
      assertError(response, OpRepositoryError.BACKUP_ERROR_CODE);
   }

   public void testRestore()
        throws Exception {
      XMessage request = new XMessage();
      request.setArgument(OpRepositoryService.BACKUP_DIR_ROOT_PATH_PARAM, BACKUP_ROOT_DIR);
      XMessage response = OpTestDataFactory.getRepositoryService().backup(session, request);
      assertNoError(response);
      String fileName = (String) response.getArgument(OpRepositoryService.BACKUP_FILENAME_PARAMETER);
      File file = new File(BACKUP_ROOT_DIR, fileName);
      assertTrue(file.exists());

      XSession newSession = server.newSession();
      assertTrue(newSession.isValid());

      request = new XMessage();
      request.setArgument("restoreFile", file.getCanonicalPath());
      response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertNoError(response);
      assertFalse(newSession.isValid());
   }

   public void testRestoreError()
        throws Exception {
      XMessage request = new XMessage();
      request.setArgument("restoreFile", BACKUP_ROOT_DIR);
      XMessage response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertError(response, OpRepositoryError.RESTORE_ERROR_CODE);

      request = new XMessage();
      request.setArgument("restoreFile", "nosuchfile");
      response = OpTestDataFactory.getRepositoryService().restore(session, request);
      assertError(response, OpRepositoryError.RESTORE_ERROR_CODE);
   }

   public void testReset()
        throws Exception {
      XMessage request = new XMessage();
      request.setArgument(OpRepositoryService.ADMIN_PASSWORD_PARAMETER, OpUser.BLANK_PASSWORD);
      XMessage response = OpTestDataFactory.getRepositoryService().reset(session, request);
      assertNoError(response);
   }

   public void testPermission()
        throws Exception {
      Map userData = OpUserTestDataFactory.createUserData(DEFAULT_USER, DEFAULT_PASSWORD, OpUser.STANDARD_USER_LEVEL);
      userData.put(OpUserService.LANGUAGE, "en");
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = OpTestDataFactory.getUserService().insertUser(session, request);
      assertNoError(response);
      logIn(DEFAULT_USER, DEFAULT_PASSWORD);

      response = OpTestDataFactory.getRepositoryService().backup(session, new XMessage());
      assertError(response, OpRepositoryError.INSUFICIENT_PERMISSIONS_ERROR_CODE);

      response = OpTestDataFactory.getRepositoryService().restore(session, new XMessage());
      assertError(response, OpRepositoryError.INSUFICIENT_PERMISSIONS_ERROR_CODE);

      response = OpTestDataFactory.getRepositoryService().reset(session, new XMessage());
      assertError(response, OpRepositoryError.INSUFICIENT_PERMISSIONS_ERROR_CODE);
   }
}
