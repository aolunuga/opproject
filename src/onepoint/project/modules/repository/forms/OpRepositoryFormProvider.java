/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.repository.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.repository.OpRepositoryErrorMap;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;
import onepoint.util.XEnvironmentManager;

import java.io.File;
import java.util.HashMap;

/**
 * Form provider for the repository main form.
 *
 * @author horia.chiorean
 */
public class OpRepositoryFormProvider implements XFormProvider {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpRepositoryFormProvider.class, true);

   /**
    * Error id for creating the backup directory
    */
   private static final String BACKUP_DIR_ERROR = "{$BackupDirectoryError}";

   /**
    * Form component ids.
    */
   private static final String USER_GROUPS_COUNT_ID = "UserGroupsCount";
   private static final String USER_GROUPS_COUNT_LABEL_ID = "UserGroupsCountLabel";
   private static final String USERS_COUNT_LABEL_ID = "UsersCountLabel";
   private static final String USERS_COUNT_ID = "UsersCount";
   private static final String PORTFOLIOS_COUNT_ID = "PortfoliosCount";
   private static final String TEMPLATES_COUNT_ID = "TemplatesCount";
   private static final String PROJECTS_COUNT_ID = "ProjectsCount";
   private static final String POOLS_COUNT_ID = "PoolsCount";
   private static final String RESOURCES_COUNT_ID = "ResourcesCount";
   private static final String REPORTS_COUNT_ID = "ReportsCount";

   private static final String BACKUP_BUTTON_ID = "Backup";
   private static final String RESTORE_BUTTON_ID = "Restore";
   private static final String RESET_BUTTON_ID = "Reset";

   private static final String ERROR_LABEL_ID = "ErrorLabel";
   private static final String BACKUP_ROOT_DIR_FIELD_ID = "BackupDirRootPath";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      OpProjectSession projectSession = (OpProjectSession) session;

      // hide multi user fields
      if (!OpInitializer.isMultiUser()) {
         form.findComponent(USER_GROUPS_COUNT_ID).setVisible(false);
         form.findComponent(USER_GROUPS_COUNT_LABEL_ID).setVisible(false);
         form.findComponent(USERS_COUNT_ID).setVisible(false);
         form.findComponent(USERS_COUNT_LABEL_ID).setVisible(false);
      }

      //set the form's fields
      setFormFields(form, projectSession);

      //check permissions
      boolean hasPermissions = hasRepositoryPermissions(projectSession, form);
      if (!hasPermissions) {
         return;
      }

      //create (if necessary) the backup directory
      String rootBackupDirectory = createRootBackupPath();
      if (rootBackupDirectory == null) {
         form.findComponent(BACKUP_BUTTON_ID).setEnabled(false);
         form.findComponent(RESTORE_BUTTON_ID).setEnabled(false);
         
         XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
         XLocalizer localizer = XLocaleManager.createLocalizer(projectSession.getLocale().getID(), OpRepositoryErrorMap.RESOURCE_MAP_ID);
         errorLabel.setText(localizer.localize(BACKUP_DIR_ERROR));
         errorLabel.setVisible(true);
         return;
      }

      //cache the root directory on the form
      form.findComponent(BACKUP_ROOT_DIR_FIELD_ID).setStringValue(rootBackupDirectory);
   }

   /**
    * Checks if the user on the current session has enough permissions to perform repository operations.
    * At the moment, only the administrator can perform repository operations.
    * @param projectSession a <code>OpProjectSession</code> representing the user session.
    * @param form a <code>XComponent(FORM)</code> representing the main repository form.
    * @return <code>true</code> if the user has enough permissions.
    */
   private boolean hasRepositoryPermissions(OpProjectSession projectSession, XComponent form) {
      if (!projectSession.userIsAdministrator()) {
         form.findComponent(BACKUP_BUTTON_ID).setEnabled(false);
         form.findComponent(RESTORE_BUTTON_ID).setEnabled(false);
         form.findComponent(RESET_BUTTON_ID).setEnabled(false);
         return false;
      }
      return true;
   }

   /**
    * Calculates the values for the fields of the form.
    * @param form a <code>XComponent(FORM)</code> representing the repository form.
    * @param projectSession a <code>OpProjectSession</code> representing the server session.
    */
   private void setFormFields(XComponent form, OpProjectSession projectSession) {
      OpBroker broker = projectSession.newBroker();

      form.findComponent(USER_GROUPS_COUNT_ID).setIntValue(projectSession.countEntity(OpGroup.GROUP, broker));
      form.findComponent(USERS_COUNT_ID).setIntValue(projectSession.countEntity(OpUser.USER, broker));
      form.findComponent(PORTFOLIOS_COUNT_ID).setIntValue(OpProjectDataSetFactory.countProjectNode(OpProjectNode.PORTFOLIO, broker));
      form.findComponent(TEMPLATES_COUNT_ID).setIntValue(OpProjectDataSetFactory.countProjectNode(OpProjectNode.TEMPLATE, broker));
      form.findComponent(PROJECTS_COUNT_ID).setIntValue(OpProjectDataSetFactory.countProjectNode(OpProjectNode.PROJECT, broker));
      form.findComponent(POOLS_COUNT_ID).setIntValue(projectSession.countEntity(OpResourcePool.RESOURCE_POOL, broker));
      form.findComponent(RESOURCES_COUNT_ID).setIntValue(projectSession.countEntity(OpResource.RESOURCE, broker));
      form.findComponent(REPORTS_COUNT_ID).setIntValue(projectSession.countEntity(OpReport.REPORT, broker));

      broker.close();
   }

   /**
    * Creates (or retrieves) the root directory where backupp files will be stored.
    * @return  a <code>String</code> representing the path to the root directory, or null if an error occurred.
    */
   private String createRootBackupPath() {
      String fullBackupRootPath;
      try {
         String backupDirectoryName = XEnvironmentManager.convertPathToSlash(OpInitializer.getConfiguration().getBackupPath());
         File absoluteDirectory = new File(backupDirectoryName);
         if (absoluteDirectory.exists() && absoluteDirectory.isDirectory() && absoluteDirectory.isAbsolute()) {
            fullBackupRootPath = absoluteDirectory.getCanonicalPath();
         }
         else {
            String parentDir = OpEnvironmentManager.getOnePointHome();
            File backupDir = new File(parentDir, backupDirectoryName);
            if (!backupDir.exists() || !backupDir.isDirectory()) {
               boolean dirCreated = backupDir.mkdir();
               if (!dirCreated) {
                  logger.error("Cannot create directory " + backupDir.getAbsolutePath() + " to store backup files");
                  return null;
               }
            }
            fullBackupRootPath = backupDir.getCanonicalPath();
         }
      }
      catch (Exception e) {
         logger.error("Cannot perform backup because:" + e.getMessage(), e);
         return null;
      }
      return fullBackupRootPath;
   }
}
