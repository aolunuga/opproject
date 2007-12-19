/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.repository;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpSourceManager;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleManager;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceManager;
import onepoint.util.XEnvironmentManager;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Service class for the repository module.
 *
 * @author horia.chiorean
 */
public class OpRepositoryService extends OpProjectService {

   /**
    * Repository service name
    */
   private static final String REPOSITORY_SERVICE_NAME = "RepositoryService";

   /**
    * The name of the request parameter which contains the value of the back dir
    */
   public static final String BACKUP_DIR_ROOT_PATH_PARAM = "backupDirRootPath";

   /**
    * The default prefix for back-up files.
    */
   public static final String BACKUP_FILE_PREFIX = "backup-";

   /**
    * Default extension for the backup file.
    */
   public static final String BACKUP_FILE_EXTENSION = ".xml";

   /**
    * The name of the response parameter representing the name of the backup file.
    */
   public static final String BACKUP_FILENAME_PARAMETER = "backupFilename";

   /**
    * The error map.
    */
   private static final OpRepositoryErrorMap ERROR_MAP = new OpRepositoryErrorMap();

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpRepositoryService.class);

   /**
    * The date format object used for formatting the name of the backup files.
    */
   private static final DateFormat BACKUP_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd-HH'h'mm'm'ss's'");

   /**
    * The name of the request parameter representing the file to restore.
    */
   private static final String RESTORE_FILE_PARAMENTER = "restoreFile";

   /**
    * The name of the request parameter representing the
    */
   public static final String ADMIN_PASSWORD_PARAMETER = "adminPassword";

   /**
    * Gets the registered instance of this service.
    *
    * @return The registered instance of this service.
    */
   public static OpRepositoryService getService() {
      return (OpRepositoryService) XServiceManager.getService(REPOSITORY_SERVICE_NAME);
   }

   /**
    * Backs up a repository.
    *
    * @param projectSession a <code>OpProjectSession</code> representing the server session.
    * @param request        a <code>XMessage</code> representing the client request.
    * @return a <code>XMessage</code> representing the server reponse.
    */
   public XMessage backup(OpProjectSession projectSession, XMessage request) {
      File backupFile = getBackupFile(projectSession);
      if (backupFile == null) {
         return createErrorMessage(projectSession, OpRepositoryError.BACKUP_ERROR_CODE);
      }

      //get the all the ids from the sessions belonging to the current site.
      List<Integer> idsList = projectSession.getIdsOfSessionsFromSameSite();

      //invalidate all server sessions
      projectSession.getServer().invalidateAllSessions(projectSession.getID(), idsList);
      //invalidate the entire site
      projectSession.getServer().invalidateSite(projectSession.getSourceName());

      try {
         boolean fileCreated = backupFile.createNewFile();
         if (!fileCreated) {
            logger.error("Cannot create backup file");
            return createErrorMessage(projectSession, OpRepositoryError.BACKUP_ERROR_CODE);
         }
         OpBackupManager.getBackupManager().backupRepository(projectSession, backupFile.getCanonicalPath());
         XMessage response = new XMessage();
         response.setArgument(BACKUP_FILENAME_PARAMETER, backupFile.getName());
         return response;
      }
      catch (IOException e) {
         logger.error("Cannot backup repository to: '" + backupFile.getAbsolutePath() + "', because:" + e.getMessage(), e);
         return createErrorMessage(projectSession, OpRepositoryError.BACKUP_ERROR_CODE);
      }
      finally {
         //revalidate the site
         projectSession.getServer().validateSite(projectSession.getSourceName());
      }
   }

   /**
    * Gets the file where the backup will be stored.
    *
    * @param session a <code>OpProjectSession</code> representing the current session.
    * @return a <code>File</code> objects or null if the backup folder can't be created.
    */
   private File getBackupFile(OpProjectSession session) {
      File backupDir = getBackupFolder(session);
      //make sure it exists
      if (backupDir == null) {
         return null;
      }
      return new File(backupDir, createBackupFilename());
   }

   /**
    * Creates a new name for the backup file which will be created.
    *
    * @return a <code>String</code> representing the name of the backup file.
    */
   private String createBackupFilename() {
      Date currentDate = new Date(System.currentTimeMillis());
      StringBuffer fileNameBuffer = new StringBuffer(BACKUP_FILE_PREFIX);
      fileNameBuffer.append(BACKUP_DATEFORMAT.format(currentDate));
      fileNameBuffer.append(BACKUP_FILE_EXTENSION);
      return fileNameBuffer.toString();
   }


   /**
    * Creates an error message with the given code.
    *
    * @param session a <code>OpProjectSession</code> session representing the server session.
    * @param code    a <code>int</code> representing the error code.
    * @return a <code>XMessage</code> that contains an error with the given code.
    */
   private XMessage createErrorMessage(OpProjectSession session, int code) {
      XMessage result = new XMessage();
      XError error = session.newError(ERROR_MAP, code);
      result.setError(error);
      return result;
   }

   /**
    * Restores a repository from an existing backup file.
    *
    * @param projectSession a <code>OpProjectSession</code> representing the server session.
    * @param request        a <code>XMessage</code> representing the client request.
    * @return a <code>XMessage</code> representing the server reponse.
    */
   public XMessage restore(OpProjectSession projectSession, XMessage request) {
      File restoreFile = new File((String) request.getArgument(RESTORE_FILE_PARAMENTER));
      if (!restoreFile.exists() || restoreFile.isDirectory()) {
         return createErrorMessage(projectSession, OpRepositoryError.RESTORE_ERROR_CODE);
      }

      try {
         //get the all the ids from the sessions belonging to the current site.
         List<Integer> idsList = projectSession.getIdsOfSessionsFromSameSite();

         //invalidate all server sessions belonging to this site except the current one
         projectSession.getServer().invalidateAllSessions(projectSession.getID(), idsList);
         projectSession.getServer().invalidateSite(projectSession.getSourceName());

         OpInitializer initializer = OpInitializerFactory.getInstance().getInitializer();
         initializer.restoreSchemaFromFile(restoreFile.getCanonicalPath(), projectSession);

         //apply settings
         OpSettingsService.getService().loadSettings(projectSession);

         //clear everything from the current session
         projectSession.clearSession();
      }
      catch (Exception e) {
         logger.error("Cannot restore repository because:" + e.getMessage(), e);
         return createErrorMessage(projectSession, OpRepositoryError.RESTORE_ERROR_CODE);
      }
      finally {
         //revalidate the site
         projectSession.getServer().validateSite(projectSession.getSourceName());
      }
      return null;
   }

   /**
    * Resets a repository by removing an existing db schema and creating a new one.
    *
    * @param projectSession a <code>OpProjectSession</code> representing the server session.
    * @param request        a <code>XMessage</code> representing the client request.
    * @return a <code>XMessage</code> representing the server reponse.
    */
   public XMessage reset(OpProjectSession projectSession, XMessage request) {
      XMessage reply = validateAdminPassword(projectSession, request);
      if (reply != null) {
         return reply;
      }

      try {
         //get the all the ids from the sessions belonging to the current site.
         List<Integer> idsList = projectSession.getIdsOfSessionsFromSameSite();

         //invalidate all server sessions
         projectSession.getServer().invalidateAllSessions(projectSession.getID(), idsList);
         //invalidate the entire site
         projectSession.getServer().invalidateSite(projectSession.getSourceName());

         logger.info("Stopping modules");
         OpModuleManager.stop();

         //remove all objects and clear sources
         OpBackupManager.getBackupManager().removeAllObjects(projectSession);
         OpSourceManager.clearAllSources();

         logger.info("Starting modules");
         OpModuleManager.start();

         //clear this session
         projectSession.clearSession();
      }
      catch (Exception e) {
         logger.error("An error occured during reset:" + e.getMessage(), e);
         return createErrorMessage(projectSession, OpRepositoryError.RESET_ERROR_CODE);
      }
      finally {
         //revalidate the site
         projectSession.getServer().validateSite(projectSession.getSourceName());
      }
      return null;
   }

   /**
    * Checks whether the user entered a valid administrator password or not.
    *
    * @param projectSession a <code>OpProjectSession</code> representing the server session.
    * @param request        a <code>XMessage</code> representing the server request.
    * @return a <code>XMessage</code> which is the response.
    */
   public XMessage validateAdminPassword(OpProjectSession projectSession, XMessage request) {
      //check whether the user entered a valid admin password
      XMessage reply = null;
      OpBroker broker = projectSession.newBroker();
      if (OpEnvironmentManager.isMultiUser()) {
         String adminPassword = projectSession.user(broker).getPassword();
         String enteredAdminPassword = (String) request.getArgument(ADMIN_PASSWORD_PARAMETER);
         if (!adminPassword.equals(enteredAdminPassword)) {
            reply = createErrorMessage(projectSession, OpRepositoryError.INVALID_ADMIN_PASSWORD);
         }
      }
      broker.close();
      return reply;
   }

   /**
    * Returns the directory where backup files will be stored. If it doesn't exist it will be created.
    *
    * @param session the session attached to the current user.
    * @return a <code>File</code> representing the back-up directory, or <code>null</code> if an error occurred.
    */
   public File getBackupFolder(OpProjectSession session) {
      File backupDir;
      try {
         OpInitializer initializer = OpInitializerFactory.getInstance().getInitializer();
         String backupDirectoryName = XEnvironmentManager.convertPathToSlash(initializer.getConfiguration().getBackupPath());
         backupDir = new File(backupDirectoryName);
         if (backupDir.exists() && backupDir.isDirectory() && backupDir.isAbsolute()) {
            backupDir = backupDir.getCanonicalFile();
         }
         else {
            String parentDir = OpEnvironmentManager.getDataFolderPath();
            backupDir = new File(parentDir, backupDirectoryName);
            if (!backupDir.exists() || !backupDir.isDirectory()) {
               boolean dirCreated = backupDir.mkdirs();
               if (!dirCreated) {
                  logger.error("Cannot create directory " + backupDir.getAbsolutePath() + " to store backup files");
                  return null;
               }
            }
            backupDir = backupDir.getCanonicalFile();
         }
      }
      catch (Exception e) {
         logger.error("Cannot perform backup because:" + e.getMessage(), e);
         return null;
      }
      return backupDir;
   }

}
