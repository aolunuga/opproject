/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for reverting project plan confirm dialog
 *
 * @author ovidiu.lupas
 */
public class OpConfirmImportProjectFormProvider implements XFormProvider {

   private final static String RESOURCE_MAP = "project_planning.ImportProjectPlan";
   private final static String CONFIRM_LABEL = "ConfirmImportLabel";
   private final static String PROJECT_ID = "ProjectId";
   private final static String ACTIVITY_SET = "ActivitySet";
   private final static String EDIT_MODE = "EditMode";
   private final static String CONFIRM_IMPORT = "ConfirmImportProjectPlan";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      OpProjectSession session = (OpProjectSession) s;
      //the project id to revert
      String projectId = (String) parameters.get(OpConfirmImportProjectFormProvider.PROJECT_ID);
      form.findComponent(OpConfirmImportProjectFormProvider.PROJECT_ID).setValue(projectId);
      Boolean editMode = (Boolean) parameters.get(EDIT_MODE);
      form.findComponent(EDIT_MODE).setValue(editMode);        
      XComponent activitySet = (XComponent) parameters.get(ACTIVITY_SET);
      form.findComponent(ACTIVITY_SET).setValue(activitySet);

      //set confirm message
      XComponent label = form.findComponent(OpConfirmImportProjectFormProvider.CONFIRM_LABEL);
      String text = session.getLocale().getResourceMap(OpConfirmImportProjectFormProvider.RESOURCE_MAP).getResource(OpConfirmImportProjectFormProvider.CONFIRM_IMPORT).getText(parameters);
      label.setText(text);

   }
}
