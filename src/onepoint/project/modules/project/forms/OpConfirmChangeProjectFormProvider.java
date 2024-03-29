/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for possible change of project assignment rates confirm dialog.
 *
 * @author florin.haizea
 */
public class OpConfirmChangeProjectFormProvider implements XFormProvider {

   private final static String PROJECT_ID = "project_id";
   private final static String PROJECT_DATA = "project_data";
   private final static String GOALS_SET = "goals_set";
   private final static String RESOURCE_SET = "resource_set";
   private final static String VERSIONS_SET = "versions_set";
   protected final static String CHANGE_LABEL = "ConfirmChangeLabel";
   protected final static String CONFIRM_HR = "ConfirmChangeHR";
   protected final static String RESOURCE_MAP = "project.change";
   protected final static String MODIFIED_RATES = "ModifiedRates";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      form.findComponent(MODIFIED_RATES).setValue(true);
      // Retrieve the parameters
      String id = (String) (parameters.get(PROJECT_ID));
      form.findComponent(PROJECT_ID).setValue(id);

      HashMap data = (HashMap) (parameters.get(PROJECT_DATA));
      form.findComponent(PROJECT_DATA).setValue(data);

      XComponent goalsSet = (XComponent) (parameters.get(GOALS_SET));
      XComponent formGoalsSet = form.findComponent(GOALS_SET);
      XComponent dataRow;
      for (int i = 0; i < goalsSet.getChildCount(); i++) {
         dataRow = (XComponent) goalsSet.getChild(i);
         formGoalsSet.addChild(dataRow.copyData());
      }

      XComponent resourceSet = (XComponent) (parameters.get(RESOURCE_SET));
      XComponent formResourceSet = form.findComponent(RESOURCE_SET);
      for (int i = 0; i < resourceSet.getChildCount(); i++) {
         dataRow = (XComponent) resourceSet.getChild(i);
         formResourceSet.addChild(dataRow.copyData());
         dataRow.clearDataSelection();
      }

      XComponent versionSet = (XComponent) (parameters.get(VERSIONS_SET));
      XComponent formVersionsSet = form.findComponent(VERSIONS_SET);
      for (int i = 0; i < versionSet.getChildCount(); i++) {
         dataRow = (XComponent) versionSet.getChild(i);
         formVersionsSet.addChild(dataRow.copyData());
      }

      //set confirm message (available/hr)
      XComponent label = form.findComponent(CHANGE_LABEL);
      XLanguageResourceMap map = session.getLocale().getResourceMap(RESOURCE_MAP);

      String labelText;
      labelText = map.getResource(CONFIRM_HR).getText();
      label.setText(labelText);
   }
}