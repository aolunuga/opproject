/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_status.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Form provider for new project status dialog.
 *
 * @author mihai.costin
 */
public class OpProjectStatusFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getServerLogger(OpProjectStatusFormProvider.class);

   //form's components
   private final static String PROJECT_STATUS_DATA_SET = "ProjectStatusDataSet";
   private final static String IS_ADMIN_ROLE_DATA_FIELD = "AdminRoleDataField";
   private final static String NEW_PROJECT_STATUS_BUTTON = "NewProjectStatus";
   private final static String INFO_BUTTON = "Info";
   private final static String DELETE_BUTTON = "Delete";
   private final static String UP_BUTTON = "Up";
   private final static String DOWN_BUTTON = "Down";


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      try {
         //disable buttons that require selection
         form.findComponent(INFO_BUTTON).setEnabled(false);
         form.findComponent(DELETE_BUTTON).setEnabled(false);
         form.findComponent(UP_BUTTON).setEnabled(false);
         form.findComponent(DOWN_BUTTON).setEnabled(false);

         boolean isUserAdministrator = session.userIsAdministrator();
         //only admin can create projectStatus
         form.findComponent(NEW_PROJECT_STATUS_BUTTON).setEnabled(isUserAdministrator);
         form.findComponent(IS_ADMIN_ROLE_DATA_FIELD).setBooleanValue(isUserAdministrator);

         Iterator projectStatusItr = OpProjectDataSetFactory.getProjectStatusIterator(broker);

         //fill project status data set
         XComponent dataSet = form.findComponent(PROJECT_STATUS_DATA_SET);

         XComponent dataRow = null;
         XComponent dataCell = null;
         OpProjectStatus projectStatus = null;
         while (projectStatusItr.hasNext()) {
            projectStatus = (OpProjectStatus) projectStatusItr.next();

            //do not show inactive projectStatus
            if (!projectStatus.getActive()) {
               continue;
            }
            dataRow = new XComponent(XComponent.DATA_ROW);
            dataRow.setStringValue(projectStatus.locator());
            dataSet.addChild(dataRow);

            // Sequence (0)
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setIntValue(projectStatus.getSequence());
            dataRow.addChild(dataCell);
            // Name (1)
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setStringValue(projectStatus.getName());
            dataRow.addChild(dataCell);
            // Description (2)
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setStringValue(projectStatus.getDescription());
            dataRow.addChild(dataCell);
            // Color (3)
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setIntValue(projectStatus.getColor());
            dataRow.addChild(dataCell);
         }
      }
      finally {
         broker.close();
      }
   }

}
