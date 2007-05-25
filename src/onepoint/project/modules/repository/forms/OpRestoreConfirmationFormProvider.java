/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.repository.forms;

import onepoint.express.server.XFormProvider;
import onepoint.express.XComponent;
import onepoint.service.server.XSession;
import onepoint.project.OpInitializer;

import java.util.HashMap;

/**
 * Form provider for the confirmation message of the reset function.
 *
 * @author horia.chiorean
 */
public class OpRestoreConfirmationFormProvider extends OpResetConfirmationFormProvider {

   /**
    * Form component ids.
    */
   private static final String BACKUP_DIR_ROOT_PATH_FIELD_ID = "BackupDirRootPath";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      super.prepareForm(session, form, parameters);
      String backupDirPath = (String) parameters.get("backupDirRootPath");
      form.findComponent(BACKUP_DIR_ROOT_PATH_FIELD_ID).setStringValue(backupDirPath);
   }
}
