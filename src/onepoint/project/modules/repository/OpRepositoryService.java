/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.repository;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.util.XEnvironmentManager;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Service class for the repository module.
 *
 * @author horia.chiorean
 */
public class OpRepositoryService extends OpProjectService {

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
   private static final XLog logger = XLogFactory.getLogger(OpRepositoryService.class, true);

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
   private static final String ADMIN_PASSWORD_PARAMETER = "adminPassword";
   
   /**
    * Backs up a repository.
    *
    * @param projectSession       a <code>OpProjectSession</code> representing the server session.
    * @param request a <code>XMessage</code> representing the client request.
    * @return a <code>XMessage</code> representing the server reponse.
    */
   public XMessage backup(OpProjectSession projectSession, XMessage request) {
      //only admin can perform operation
      if (!projectSession.userIsAdministrator()) {
         return createErrorMessage(projectSession, OpRepositoryError.INSUFICIENT_PERMISSIONS_ERROR_CODE);
      }

      File backupFile = getBackupFile(request);
      if (backupFile == null) {
         return createErrorMessage(projectSession, OpRepositoryError.BACKUP_ERROR_CODE);
      }

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
         logger.error("Cannot backup repository because:" + e.getMessage(), e);
         return createErrorMessage(projectSession, OpRepositoryError.BACKUP_ERROR_CODE);
      }
   }

   /**
    * Gets the file where the backup will be stored.
    * @param request a <code>XMessage</code> representing the server request.
    * @return a <code>File</code> objects or null if the backup folder can't be created.
    */
   private File getBackupFile(XMessage request) {
      File fullBackupRootPath = new File((String) request.getArgument(BACKUP_DIR_ROOT_PATH_PARAM));
      //make sure it exists
      if (!fullBackupRootPath.exists()) {
         boolean pathCreated = fullBackupRootPath.mkdir();
         if (!pathCreated) {
            logger.error("Cannot create backup folder");
            return null;
         }
      }
      return new File(fullBackupRootPath + createBackupFilename());
   }

   /**
    * Creates a new name for the backup file which will be created.
    *
    * @return a <code>String</code> representing the name of the backup file.
    */
   private String createBackupFilename() {
      StringBuffer fileNameBuffer = new StringBuffer(XEnvironmentManager.FILE_SEPARATOR);
      fileNameBuffer.append(BACKUP_FILE_PREFIX);

      Date currentDate = new Date(System.currentTimeMillis());
      fileNameBuffer.append(BACKUP_DATEFORMAT.format(currentDate));
      fileNameBuffer.append(BACKUP_FILE_EXTENSION);
      return fileNameBuffer.toString();
   }


   /**
    * Creates an error message with the given code.
    * @param session a <code>OpProjectSession</code> session representing the server session.
    * @param code a <code>int</code> representing the error code.
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
    * @param projectSession       a <code>OpProjectSession</code> representing the server session.
    * @param request a <code>XMessage</code> representing the client request.
    * @return a <code>XMessage</code> representing the server reponse.
    */
   public XMessage restore(OpProjectSession projectSession, XMessage request) {
      //only admin can perform operation
      if (!projectSession.userIsAdministrator()) {
         return createErrorMessage(projectSession, OpRepositoryError.INSUFICIENT_PERMISSIONS_ERROR_CODE);
      }

      File restoreFile = new File((String) request.getArgument(RESTORE_FILE_PARAMENTER));
      if (!restoreFile.exists() || restoreFile.isDirectory()) {
         return createErrorMessage(projectSession, OpRepositoryError.RESTORE_ERROR_CODE);
      }

      try {
         OpInitializer.restoreSchemaFromFile(restoreFile.getCanonicalPath(), projectSession);
         //invalidate all server sessions
         projectSession.getServer().invalidateAllSessions();         
      }
      catch (Exception e) {
         logger.error("Cannot restore repository because:" +  e.getMessage(), e);
         return createErrorMessage(projectSession, OpRepositoryError.RESTORE_ERROR_CODE);
      }
      return null;
   }

   /**
    * Resets a repository by removing an existing db schema and creating a new one.
    *
    * @param projectSession       a <code>OpProjectSession</code> representing the server session.
    * @param request a <code>XMessage</code> representing the client request.
    * @return a <code>XMessage</code> representing the server reponse.
    */
   public XMessage reset(OpProjectSession projectSession, XMessage request) {
      //only admin can perform operation
      if (!projectSession.userIsAdministrator()) {
         return createErrorMessage(projectSession, OpRepositoryError.INSUFICIENT_PERMISSIONS_ERROR_CODE);
      }
      //check whether the user entered a valid admin password
      OpBroker broker = projectSession.newBroker();
      if (OpInitializer.isMultiUser()) {
         String adminPassword = projectSession.user(broker).getPassword();
         String enteredAdminPassword = (String) request.getArgument(ADMIN_PASSWORD_PARAMETER);
         if (!adminPassword.equals(enteredAdminPassword)) {
            return createErrorMessage(projectSession, OpRepositoryError.INVALID_ADMIN_PASSWORD);
         }
      }

      try {
         OpInitializer.resetDbSchema();
         //invalidate all server sessions
         projectSession.getServer().invalidateAllSessions();
      }
      catch (Exception e) {
         logger.error("An error occured during reset:" + e.getMessage(), e);
         return createErrorMessage(projectSession, OpRepositoryError.RESET_ERROR_CODE);
      }
      finally {
         broker.close();
      }
      return null;
   }
}
