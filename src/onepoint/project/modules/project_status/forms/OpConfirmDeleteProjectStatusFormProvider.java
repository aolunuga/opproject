/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_status.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.List;

/**
 * Form provider for delete project status confirm dialog.
 *
 * @author mihai.costin
 */
public class OpConfirmDeleteProjectStatusFormProvider implements XFormProvider {

   private final static String SELECTED_ROWS = "ProjectStatusIds";
   private final static String RESOURCE_IDS_KEY = "project_status_ids";

   private final static String RESOURCE_MAP = "project_status.delete";

   private final static String CONFIRM_LABEL = "ConfirmDeleteLabel";
   private final static String NO_SELECTION = "NoSelectionMessage";
   private final static String DELETE_CATEGORY = "ConfirmDeleteOneMessage";
   private final static String DELETE_CATEGORIES = "ConfirmDeleteMultipleMessage";
   private final static String CANCEL_BUTTON = "CancelButton";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Retrieve the resources
      List categories = (List) (parameters.get(RESOURCE_IDS_KEY));
      form.findComponent(SELECTED_ROWS).setValue(categories);

      //set confirm message
      XComponent label = form.findComponent(CONFIRM_LABEL);
      String text;

      //no project status selected
      if (categories == null || categories.size() == 0) {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(NO_SELECTION).getText();
         form.findComponent(CANCEL_BUTTON).setVisible(false);
      }
      //one project status
      else if (categories.size() == 1) {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_CATEGORY).getText();
      }
      //more than one project status selected
      else {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_CATEGORIES).getText();
      }
      label.setText(text);

   }
}
