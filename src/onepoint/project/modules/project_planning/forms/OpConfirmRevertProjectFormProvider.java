/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
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
public class OpConfirmRevertProjectFormProvider implements XFormProvider {

   private final static String RESOURCE_MAP = "project_planning.RevertProjectPlan";
   private final static String CONFIRM_LABEL = "ConfirmRevertLabel";
   private final static String PROJECT_ID = "ProjectId";
   private final static String CONFIRM_REVERT = "ConfirmRevertProjectPlan";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      OpProjectSession session = (OpProjectSession) s;
      //the project id to revert
      String projectId = (String) parameters.get(PROJECT_ID);
      form.findComponent(PROJECT_ID).setValue(projectId);
      //set confirm message
      XComponent label = form.findComponent(CONFIRM_LABEL);
      String text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(CONFIRM_REVERT).getText();
      label.setText(text);

   }
}
