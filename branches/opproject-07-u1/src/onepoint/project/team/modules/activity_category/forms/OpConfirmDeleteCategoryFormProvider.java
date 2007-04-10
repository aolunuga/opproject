/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */
package onepoint.project.team.modules.activity_category.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.List;

/**
 * Form provider for delete category confirm dialog.
 *
 * @author gmesaric
 */
public class OpConfirmDeleteCategoryFormProvider implements XFormProvider {

   private final static String SELECTED_ROWS = "CategoryIds";
   private final static String RESOURCE_IDS_KEY = "category_ids";
   
   private final static String RESOURCE_MAP = "activity_category.delete";

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

      //no category selected
      if (categories == null || categories.size() == 0) {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(NO_SELECTION).getText();
         form.findComponent(CANCEL_BUTTON).setVisible(false);
      }
      //one category
      else if (categories.size() == 1) {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_CATEGORY).getText();
      }
      //more than one category selected
      else {
         text = session.getLocale().getResourceMap(RESOURCE_MAP).getResource(DELETE_CATEGORIES).getText();
      }
      label.setText(text);

   }
}
