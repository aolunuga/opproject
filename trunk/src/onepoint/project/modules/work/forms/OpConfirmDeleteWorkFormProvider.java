/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */
package onepoint.project.modules.work.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Form provider for delete work slips confirm dialog.
 *
 * @author mihai.costin
 */
public class OpConfirmDeleteWorkFormProvider implements XFormProvider {

   private final static String RESOURCE_MAP = "work.delete";
   private final static String SELECTED_ROWS = "WorkIds";
   private final static String SELECTED_IDS_KEY = "work_slips";
   private final static String NO_SELECTION = "NoSelectionMessage";
   private final static String DELETE_ONE = "ConfirmDeleteOneMessage";
   private final static String DELETE_MULTIPLE = "ConfirmDeleteMultipleMessage";
   private final static String CANCEL_BUTTON = "CancelButton";
   private final static String CONFIRM_LABEL = "ConfirmDeleteLabel";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Retrieve work IDs
      ArrayList rows = (ArrayList) (parameters.get(SELECTED_IDS_KEY));
      form.findComponent(SELECTED_ROWS).setValue(rows);

      // get the form's confirm message label
      XComponent confirmMessageLabel = form.findComponent(CONFIRM_LABEL);
      // the i18n confirm message
      String confirmMessage;

      // The number of work rows determines the confirm message
      if ((rows == null) || (rows.size() == 0)) {
         confirmMessage = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(NO_SELECTION).getText();
         form.findComponent(CANCEL_BUTTON).setVisible(false);
      }
      else if (rows.size() == 1)
         confirmMessage = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_ONE).getText();
      else
         confirmMessage = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_MULTIPLE).getText();

      confirmMessageLabel.setText(confirmMessage);

   }
}
