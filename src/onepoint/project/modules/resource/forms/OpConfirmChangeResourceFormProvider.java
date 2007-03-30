/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for change resource confirm dialog.
 *
 * @author mihai.costin
 */
public class OpConfirmChangeResourceFormProvider implements XFormProvider {

   private final static String RESOURCE_ID = "resource_id";
   private final static String RESOURCE_DATA = "resource_data";
   private final static String CHANGE_LABEL = "ConfirmChangeLabel";
   private final static String HOURLY_CHANGED = "hourlyChanged";
   private final static String CONFIRM_HR = "ConfirmChangeHR";
   private final static String CONFIRM_AVAILABLE = "ConfirmChangeAvailable";
   private final static String RESOURCE_MAP = "resource.change";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Retrieve the resources
      String id= (String) (parameters.get(RESOURCE_ID));
      form.findComponent(RESOURCE_ID).setValue(id);

      HashMap data = (HashMap) (parameters.get(RESOURCE_DATA));
      form.findComponent(RESOURCE_DATA).setValue(data);

      //set confirm message (available/hr)
      XComponent label = form.findComponent(CHANGE_LABEL);
      boolean hrChanged = ((Boolean) (parameters.get(HOURLY_CHANGED))).booleanValue();
      XLanguageResourceMap map = session.getLocale().getResourceMap(RESOURCE_MAP);

      String labelText;
      if (hrChanged){
         labelText = map.getResource(CONFIRM_HR).getText();
      }
      else {
        labelText = map.getResource(CONFIRM_AVAILABLE).getText();
      }
      label.setText(labelText);
   }
}
