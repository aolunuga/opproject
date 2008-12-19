/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_status.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.project.modules.project_status.OpProjectStatusService;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for edit project status dialog.
 *
 * @author mihai.costin
 */
public class OpEditProjectStatusFormProvider implements XFormProvider {

   private final static String PROJECT_STATUS_ID = "ProjectStatusID";
   private final static String EDIT_MODE = "EditMode";
   private final static String EDIT_MODE_PARAM = "edit_mode";
   private final static String CANCEL_BUTTON_ID = "Cancel";
   private final static String INFO_RESOURCE_MAP = "project_status.Info";
   private final static String INFO_RESOURCE_TITLE = "InfoProjectStatus";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Locate project status in database
      String id_string = (String) (parameters.get(OpProjectStatusService.PROJECT_STATUS_ID));
      Boolean edit_mode = (Boolean) parameters.get(EDIT_MODE_PARAM);

      OpBroker broker = ((OpProjectSession) session).newBroker();
      try {
         OpProjectStatus projectStatus = (OpProjectStatus) (broker.getObject(id_string));

         // Downgrade edit mode to view mode if user is not the administrator
         if (edit_mode.booleanValue() && !session.userIsAdministrator()) {
            edit_mode = Boolean.FALSE;
         }

         // Fill form with data
         form.findComponent(PROJECT_STATUS_ID).setStringValue(id_string);
         form.findComponent(EDIT_MODE).setBooleanValue(edit_mode.booleanValue());

         XComponent name = form.findComponent(OpProjectStatus.NAME);
         name.setStringValue(projectStatus.getName());
         XComponent description = form.findComponent(OpProjectStatus.DESCRIPTION);
         if (projectStatus.getDescription() != null)
        	 description.setStringValue(projectStatus.getDescription());
         else
        	 description.setStringValue("");
         XComponent color = form.findComponent(OpProjectStatus.COLOR);
         color.setIntValue(projectStatus.getColor());

         if (!edit_mode.booleanValue()) {
            name.setEnabled(false);
            description.setEnabled(false);
            color.setEnabled(false);
            form.findComponent(CANCEL_BUTTON_ID).setVisible(false);
            String title = session.getLocale().getResourceMap(INFO_RESOURCE_MAP).getResource(INFO_RESOURCE_TITLE)
            .getText();
            form.setText(title);
         }
      }
      finally {
         broker.close();
      }
   }

}
