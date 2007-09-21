/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.List;

/**
 * Form provider for delete project confirm dialog.
 *
 * @author mihai.costin
 */
public class OpConfirmDeleteAdhocFormProvider implements XFormProvider {
   private final static String SELECTED_ROWS = "TaskIds";
   private final static String RESOURCE_MAP = "my_tasks.delete";

   private final static String CONFIRM_LABEL = "ConfirmDeleteLabel";
   private final static String NO_SELECTION = "NoSelectionMessage";
   private final static String DELETE_ONE = "ConfirmDeleteOneMessage";
   private final static String DELETE_MULTIPLE = "ConfirmDeleteMultipleMessage";
   private final static String SELECTED_IDS_KEY = "rows";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      OpProjectSession session = (OpProjectSession) s;

      // Retrieve projects
      List projects = (List) (parameters.get(SELECTED_IDS_KEY));
      form.findComponent(SELECTED_ROWS).setValue(projects);

      //set confirm message
      XComponent label = form.findComponent(CONFIRM_LABEL);
      String text;

      //no tasks selected
      if (projects == null || projects.size() == 0) {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(NO_SELECTION).getText();
      }
      //one task
      else if (projects.size() == 1) {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_ONE).getText();
      }
      //more tasks
      else {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_MULTIPLE).getText();
      }
      label.setText(text);
   }
}
