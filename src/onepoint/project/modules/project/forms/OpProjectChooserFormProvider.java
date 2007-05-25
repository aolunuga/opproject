/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;

public class OpProjectChooserFormProvider implements XFormProvider {

   // Form parameters and component IDs
   private final static String CALLING_FRAME_ID = "CallingFrameID";
   private final static String ACTION_HANDLER = "ActionHandler";
   private final static String PROJECT_LOCATOR_FIELD_ID = "ProjectLocatorFieldID";
   private final static String PROJECT_NAME_FIELD_ID = "ProjectNameFieldID";
   private final static String PROJECT_SET = "ProjectSet";
   private final static String PROJECTS_LIST_BOX = "ProjectList";
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

      Boolean enableProjects = (Boolean) parameters.get(OpProjectDataSetFactory.ENABLE_PROJECTS);
      form.findComponent(OpProjectDataSetFactory.ENABLE_PROJECTS).setBooleanValue(enableProjects.booleanValue());

      Boolean enablePortfolios = (Boolean) parameters.get(OpProjectDataSetFactory.ENABLE_PORTFOLIOS);
      form.findComponent(OpProjectDataSetFactory.ENABLE_PORTFOLIOS).setBooleanValue(enablePortfolios.booleanValue());

      Boolean enableTemplates = (Boolean) parameters.get(OpProjectDataSetFactory.ENABLE_TEMPLATES);
      form.findComponent(OpProjectDataSetFactory.ENABLE_TEMPLATES).setBooleanValue(enableTemplates.booleanValue());      

      //filter out any nodes if necessary
      ArrayList filteredIds = (ArrayList) parameters.get(OpProjectDataSetFactory.FILTERED_OUT_IDS);
      form.findComponent(OpProjectDataSetFactory.FILTERED_OUT_IDS).setListValue(filteredIds);

      // *** Put all project names into project data-set (values are IDs)
      XComponent dataSet = form.findComponent(PROJECT_SET);

      OpBroker broker = session.newBroker();
      int types = OpProjectDataSetFactory.PROJECTS + OpProjectDataSetFactory.PORTFOLIOS + OpProjectDataSetFactory.TEMPLATES;
      OpProjectDataSetFactory.retrieveProjectDataSetRootHierarchy(session, dataSet, types, false, filteredIds);
      broker.close();

      //enable or disable any nodes
      OpProjectDataSetFactory.enableNodes(parameters, dataSet);
   }
}
