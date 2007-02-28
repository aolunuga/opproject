/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Form Provider class for confirmation dialog used in delete users and groups .
 * 
 * @author ovidiu.lupas
 */
public class OpConfirmDeleteUserFormProvider implements XFormProvider {

   private final static String RESOURCE_MAP = "user.delete";
   private final static String SELECTED_ROWS = "SubjectIds";
   private final static String SUPER_ROWS = "SuperIds";
   private final static String SUB_ROWS = "SubIds";
   private final static String NO_SELECTION = "NoSelectionMessage";
   private final static String DELETE_ONE = "ConfirmDeleteOneMessage";
   private final static String DELETE_MULTIPLE = "ConfirmDeleteMultipleMessage";
   private final static String DATA_SET = "user_data_set";
   private final static String SELECTED_DATA_SET_ROWS = "selected_rows";
   private final static String CANCEL_BUTTON = "CancelButton";
   private final static String CONFIRM_LABEL = "ConfirmDeleteLabel";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;


      List selected_rows = (List) parameters.get(SELECTED_DATA_SET_ROWS);
      XComponent  user_data_set = (XComponent) parameters.get(DATA_SET);
      ArrayList subjectLocators = new ArrayList();
      ArrayList superLocators = new ArrayList();
      ArrayList subLocators = new ArrayList();


      if (selected_rows.size() > 0) {
         int i = 0;
         int index;
         int outline_level;
         while (i < selected_rows.size()) {

            //search for super group on the ui
            XComponent selectedComponent = (XComponent) selected_rows.get(i);
            index = selectedComponent.getIndex();
            index--;
            outline_level = selectedComponent.getOutlineLevel();
            XComponent previous;
            XComponent found = null;
            while (index >= 0 && found == null) {
               previous = (XComponent) user_data_set.getChild(index);
               if (previous.getOutlineLevel() < outline_level) {
                  found = previous;
               }
               index--;
            }

            if (found != null) {
               //super was found for this, so remove user/group assignment
               superLocators.add(found.getStringValue());
               subLocators.add(selectedComponent.getStringValue());
            }
            else {
               //no super was found, user mut be deleted
               subjectLocators.add(selectedComponent.getStringValue());
            }
            i++;
         }
      }

      //FIXME : UI shows more relations (assignments) that there are in the DB.
      //extra relations are deleted when a group is selected because of "children-selection" 
      form.findComponent(SELECTED_ROWS).setValue(subjectLocators);
      form.findComponent(SUPER_ROWS).setValue(superLocators);
      form.findComponent(SUB_ROWS).setValue(subLocators);

      // get the form's confirm message label
      XComponent confirmMessageLabel = form.findComponent(CONFIRM_LABEL);
      // the i18n confirm message
      String confirmMessage;

      // The number of subjectIds determines the confirm message
      if ( subjectLocators.size() == 0&& subLocators.size() == 0 ) {
         confirmMessage = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(NO_SELECTION).getText();
         form.findComponent(CANCEL_BUTTON).setVisible(false);
      }
      else if (subjectLocators.size() == 1 || subLocators.size() == 1)
         confirmMessage = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_ONE).getText();
      else
         confirmMessage = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_MULTIPLE).getText();

      confirmMessageLabel.setText(confirmMessage);
   }

}
