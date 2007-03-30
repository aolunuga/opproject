/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.repository.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.repository.OpRepositoryService;
import onepoint.service.server.XSession;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Form provider for the restore file list form.
 *
 * @author horia.chiorean
 */
public class OpRestoreFileListFormProvider implements XFormProvider {

   /**
    * This class logger
    */
   private final static XLog logger = XLogFactory.getLogger(OpRestoreFileListFormProvider.class, true);

   /**
    * Form component buttons
    */
   private static final String OK_BUTTON_ID = "okButton" ;
   private static final String RESTORE_FILELIST_DATASET_ID = "RestoreFileListSet" ;
   private static final String RESTORE_FILELIST_LISTBOX = "RestoreFileList" ;

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      File backupRootDir = new File((String) parameters.get(OpRepositoryService.BACKUP_DIR_ROOT_PATH_PARAM));
      if (!backupRootDir.exists() || !backupRootDir.isDirectory()) {
         form.findComponent(OK_BUTTON_ID).setEnabled(false);
         return;
      }

      File[] backupFiles = getBackupFiles(backupRootDir);

      if (backupFiles == null || backupFiles.length == 0) {
         form.findComponent(OK_BUTTON_ID).setEnabled(false);
         return;         
      }

      try {
         populateBackupFileList(form, backupFiles, backupRootDir);
         form.findComponent(RESTORE_FILELIST_LISTBOX).setSelectionModel(XComponent.SINGLE_ROW_SELECTION);
      }
      catch (IOException e) {
         logger.error("Cannot get file list to backup", e);
         form.findComponent(OK_BUTTON_ID).setEnabled(false);
      }
   }

   /**
    * Gets all the available backup files from a directory.
    * @param backupRootDir a <code>File</code> representing a directory with backup files.
    * @return a <code>File[]</code> representing backup files.
    */
   private File[] getBackupFiles(File backupRootDir) {
      File[] backupFiles = backupRootDir.listFiles(new FileFilter() {
         public boolean accept(File pathname) {
            return !pathname.isDirectory() && !pathname.isHidden() && pathname.getName().endsWith(OpRepositoryService.BACKUP_FILE_EXTENSION);
         }
      });
      Arrays.sort(backupFiles, new Comparator() {
         public int compare(Object o1, Object o2) {
            File file1 = (File) o1;
            File file2 = (File) o2;
            long modifiedDiff = file1.lastModified() - file2.lastModified();
            return (int) -modifiedDiff;
         }
      });
      return backupFiles;
   }

   /**
    * Populates the filelist dataset with the list of files to backup.
    * @param form a <code>XComponent(FORM)</code> representing the file list form.
    * @param backupFiles a <code>File[]</code> representing backup files.
    * @param backupRootDir a <code>File</code> representing the parent dir where the backup files are located.
    * @throws IOException if the path to one of the backup files can't be retrieved.
    */
   private void populateBackupFileList(XComponent form, File[] backupFiles, File backupRootDir)
        throws IOException {
      XComponent fileListDataSet = form.findComponent(RESTORE_FILELIST_DATASET_ID);
      for (int i = 0; i < backupFiles.length; i++) {
         String backupFileName = backupFiles[i].getName();
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(backupFiles[i].getCanonicalPath(), backupFileName));
         fileListDataSet.addChild(dataRow);
      }
   }
}
