/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.repository.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.modules.repository.OpRepositoryService;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for the backup confirmation dialog.
 *
 * @author horia.chiorean
 */
public class OpBackupConfirmationFormProvider implements XFormProvider {

   private static final String CONFIRMATION_LABEL_ID = "BackupConfirmationMessage";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      String backupFileName = (String) parameters.get(OpRepositoryService.BACKUP_FILENAME_PARAMETER);
      XComponent confirmationLabel = form.findComponent(CONFIRMATION_LABEL_ID);
      confirmationLabel.setText(confirmationLabel.getText() + backupFileName);
   }
}
