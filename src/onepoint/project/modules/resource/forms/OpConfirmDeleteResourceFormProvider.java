/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */
package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.List;

/**
 * Form provider for delete resource confirm dialog.
 *
 * @author mihai.costin
 */
public class OpConfirmDeleteResourceFormProvider implements XFormProvider {

   private final static String SELECTED_ROWS = "ResourceIds";
   private final static String RESOURCE_IDS_KEY = "resource_ids";
   private final static String RESOURCE_MAP = "resource.delete";

   private final static String CONFIRM_LABEL = "ConfirmDeleteLabel";
   private final static String NO_SELECTION = "NoSelectionMessage";
   private final static String DELETE_RESOURCE = "ConfirmDeleteOneMessage";
   private final static String DELETE_RESOURCES = "ConfirmDeleteMultipleMessage";
   private final static String CANCEL_BUTTON = "CancelButton";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Retrieve the resources
      List resources = (List) (parameters.get(RESOURCE_IDS_KEY));
      form.findComponent(SELECTED_ROWS).setValue(resources);

      //set confirm message
      XComponent label = form.findComponent(CONFIRM_LABEL);
      String text;

      //no resources selected
      if (resources == null || resources.size() == 0) {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(NO_SELECTION).getText();
         form.findComponent(CANCEL_BUTTON).setVisible(false);
      }
      //one resource/pool
      else if (resources.size() == 1) {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_RESOURCE).getText();
      }
      //more than one resources/pools selected
      else {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_RESOURCES).getText();
      }
      label.setText(text);

   }
}
