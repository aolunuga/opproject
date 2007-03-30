/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.forms;

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
public class OpConfirmDeleteProjectFormProvider implements XFormProvider {
   private final static String SELECTED_ROWS = "ProjectIds";
   private final static String RESOURCE_MAP = "project.delete";

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

      //no projects selected
      if (projects == null || projects.size() == 0) {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(NO_SELECTION).getText();
      }
      //one project/portfolio
      else if (projects.size() == 1) {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_ONE).getText();
      }
      //more projects/portfolios
      else {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_MULTIPLE).getText();
      }
      label.setText(text);
   }
}
