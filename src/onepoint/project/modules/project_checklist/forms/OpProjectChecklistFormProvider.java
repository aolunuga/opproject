/**
 * Copyright(c) OnePoint Software GmbH 2008. All Rights Reserved.
 */
package onepoint.project.modules.project_checklist.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpToDo;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider class for the project checklist main form.
 *
 * @author mihai.costin
 */
public class OpProjectChecklistFormProvider implements XFormProvider {

   private static final String SAVE_BUTTON = "SaveButton";
   private static final String REVERT_BUTTON = "RevertButton";
   private static final String TO_DOS_TABLE_BOX = "ToDosTableBox";
   private static final String PLUS_BUTTON = "PlusButton";
   private static final String MINUS_BUTTON = "MinusButton";
   private static final String TO_DOS_SET = "ToDosSet";
   private static final String PROJECT_ID = "ProjectId";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // Decide on project-ID and retrieve project
      String project_locator = (String) (parameters.get(OpProjectConstants.PROJECT_ID));
      if (project_locator != null) {
         // Get open project-ID from parameters and set project-ID session variable
         session.setVariable(OpProjectConstants.PROJECT_ID, project_locator);
      }
      else {
         project_locator = (String) (session.getVariable(OpProjectConstants.PROJECT_ID));
      }

      if (project_locator != null) {
         OpProjectNode project = (OpProjectNode) (broker.getObject(project_locator));
         form.findComponent(PROJECT_ID).setStringValue(project_locator);

         if (project.getType() == 3) {
            activateContent(form, true);
            fillToDos(form, project);
         } else {
            activateContent(form, false);
         }
      } else {
         activateContent(form, false);
      }
   }



    /**
    * Fills the goals for the edited project.
    *
    * @param form     a <code>XComponent(FORM)</code> representing the edit project form.
    * @param project  a <code>OpProjectNode</code> representing the project being edited.
    */
   private void fillToDos(XComponent form, OpProjectNode project) {
      XComponent dataSet = form.findComponent(TO_DOS_SET);
      XComponent dataRow;
      XComponent dataCell;

      for (OpToDo toDo : project.getToDos()) {
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(toDo.locator());
         dataSet.addChild(dataRow);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setBooleanValue(toDo.getCompleted());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(toDo.getName());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setIntValue(toDo.getPriority());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDateValue(toDo.getDue());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
      }
      //sort to dos data set based on to do's name (data cell with index 1)
      dataSet.sort(1);
   }

   private void activateContent(XComponent form, boolean value) {
      form.findComponent(SAVE_BUTTON).setEnabled(value);
      form.findComponent(REVERT_BUTTON).setEnabled(value);
      form.findComponent(TO_DOS_TABLE_BOX).setEnabled(value);
      form.findComponent(TO_DOS_TABLE_BOX).setEditMode(value);
      form.findComponent(PLUS_BUTTON).setEnabled(value);
      form.findComponent(MINUS_BUTTON).setEnabled(value);
   }
}
