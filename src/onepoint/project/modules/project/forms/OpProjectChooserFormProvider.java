/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.service.server.XSession;

import java.util.*;

public class OpProjectChooserFormProvider implements XFormProvider {

   // Form parameters and component IDs
   private final static String CALLING_FRAME_ID = "CallingFrameID";
   private final static String ACTION_HANDLER = "ActionHandler";
   private final static String PROJECT_LOCATOR_FIELD_ID = "ProjectLocatorFieldID";
   private final static String PROJECT_NAME_FIELD_ID = "ProjectNameFieldID";
   private final static String PROJECT_SET = "ProjectSet";
   private final static String PROJECTS_LIST_BOX = "ProjectList";
   private final static String ENABLE_PROJECTS = "EnableProjects";
   private final static String ENABLE_PORTFOLIOS = "EnablePortfolios";
   private final static String ENABLE_TEMPLATES = "EnableTemplates";
   private final static String FILTERED_OUT_IDS = "FilteredOutIds";
   private final static String MULTIPLE_SELECTION = "MultipleSelection";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Set calling frame and field IDs from parameters
      String callingFrameID = (String) parameters.get(CALLING_FRAME_ID);
      form.findComponent(CALLING_FRAME_ID).setStringValue(callingFrameID);

      String actionHandler = (String) parameters.get(ACTION_HANDLER);
      if (actionHandler != null) {
         form.findComponent(ACTION_HANDLER).setStringValue(actionHandler);
      }

      String projectLocatorFieldID = (String) parameters.get(PROJECT_LOCATOR_FIELD_ID);
      if (projectLocatorFieldID != null) {
         form.findComponent(PROJECT_LOCATOR_FIELD_ID).setStringValue(projectLocatorFieldID);
      }

      String projectNameFieldID = (String) parameters.get(PROJECT_NAME_FIELD_ID);
      if (projectNameFieldID != null) {
         form.findComponent(PROJECT_NAME_FIELD_ID).setStringValue(projectNameFieldID);
      }

      Boolean multipleSelection = (Boolean) parameters.get(MULTIPLE_SELECTION);
      XComponent listBox = form.findComponent(PROJECTS_LIST_BOX);
      if (multipleSelection != null && multipleSelection.booleanValue()) {
         listBox.setSelectionModel(XComponent.MULTIPLE_ROWS_SELECTION);
      }
      else {
         listBox.setSelectionModel(XComponent.SINGLE_ROW_SELECTION);
      }
      form.findComponent(MULTIPLE_SELECTION).setBooleanValue(multipleSelection != null && multipleSelection.booleanValue());

      // *** Put all project names into project data-set (values are IDs)
      XComponent dataSet = form.findComponent(PROJECT_SET);

      OpBroker broker = session.newBroker();
      int types = OpProjectDataSetFactory.PROJECTS + OpProjectDataSetFactory.PORTFOLIOS + OpProjectDataSetFactory.TEMPLATES;
      OpProjectDataSetFactory.retrieveProjectDataSet(session, broker, dataSet, types, false);
      broker.close();

      //filter out any nodes if necessary
      List filteredIds = (List) parameters.get(FILTERED_OUT_IDS);
      filterOutIds(dataSet, filteredIds);

      //enable or disable any nodes
      enableNodes(parameters, dataSet);
   }

   /**
    * Performs enabling or disabling (selection wise) of various project nodes, based on the request parameters.
    * @param parameters a <code>Map</code> of String,Object pairs representing the request parameters.
    * @param dataSet a <code>XComponent(DATA_SET)</code> representing the project node structure.
    */
   private void enableNodes(Map parameters, XComponent dataSet) {
      boolean enablePortfolios = ((Boolean) parameters.get(ENABLE_PORTFOLIOS)).booleanValue();
      boolean enableTemplates = ((Boolean) parameters.get(ENABLE_TEMPLATES)).booleanValue();
      boolean enableProjects = ((Boolean) parameters.get(ENABLE_PROJECTS)).booleanValue();
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         String choice = dataRow.getStringValue();
         //<FIXME author="Horia Chiorean" description="Using the icon index as a denominator is not the best choice">
         int iconIndex = XValidator.choiceIconIndex(choice);
         //<FIXME>
         switch(iconIndex) {
            case OpProjectDataSetFactory.PROJECT_ICON_INDEX: {
               if (!enableProjects) {
                  dataRow.setSelectable(false);
               }
               break;
            }
            case OpProjectDataSetFactory.PORTFOLIO_ICON_INDEX: {
               if (!enablePortfolios) {
                  dataRow.setSelectable(false);
               }
               break;
            }
            case OpProjectDataSetFactory.TEMPLATE_ICON_INDEX: {
               if (!enableTemplates) {
                  dataRow.setSelectable(false);
               }
               break;
            }
         }
      }
   }

   /**
    * Removes the data rows that have the string value among the list of given ids.
    * @param dataSet a <code>XComponent(DATA_SET)</code> representing a set of data rows.
    * @param ids a <code>List</code> of <code>String</code> representing ids to filter out.
    */
   private void filterOutIds(XComponent dataSet, List ids) {
      if (ids != null) {
         List childrenToRemove = new ArrayList();
         for (int i = 0; i < dataSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) dataSet.getChild(i);
            String choice = dataRow.getStringValue();
            String projectNodeChoiceId = XValidator.choiceID(choice);
            if (ids.contains(projectNodeChoiceId)) {
               childrenToRemove.add(dataRow);
            }
         }

         for (Iterator it = childrenToRemove.iterator(); it.hasNext(); ) {
            XComponent child = (XComponent) it.next();
            dataSet.removeChild(child);
         }
      }
   }
}
