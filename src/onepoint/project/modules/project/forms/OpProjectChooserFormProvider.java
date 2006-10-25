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

import java.util.HashMap;

public class OpProjectChooserFormProvider implements XFormProvider {

   // Form parameters and component IDs
   public final static String CALLING_FRAME_ID = "CallingFrameID";
   public final static String ACTION_HANDLER = "ActionHandler";
   public final static String PROJECT_LOCATOR_FIELD_ID = "ProjectLocatorFieldID";
   public final static String PROJECT_NAME_FIELD_ID = "ProjectNameFieldID";
   public final static String PROJECT_SET = "ProjectSet";
   public final static String SHOW_PROJECT = "ShowProject";
   public final static String SHOW_PORTFOLIO = "ShowPortfolio";
   public final static String SHOW_TEMPLATE = "ShowTemplate";

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

      // *** Put all project names into project data-set (values are IDs)
      XComponent dataSet = form.findComponent(PROJECT_SET);
      OpBroker broker = session.newBroker();

      boolean showPortfolio = ((Boolean) parameters.get(SHOW_PORTFOLIO)).booleanValue();
      boolean showTemplate = ((Boolean) parameters.get(SHOW_TEMPLATE)).booleanValue();
      boolean showProject = ((Boolean) parameters.get(SHOW_PROJECT)).booleanValue();
      int types = 0;
      if (showProject) {
         types += OpProjectDataSetFactory.PROJECTS;
      }
      if (showPortfolio) {
         types += OpProjectDataSetFactory.PORTFOLIOS;
      }
      if (showTemplate) {
         types += OpProjectDataSetFactory.TEMPLATES;
      }
      if (showPortfolio) {
         OpProjectDataSetFactory.retrieveProjectDataSet(session, broker, dataSet, types, false);
      }
      else {
         OpProjectDataSetFactory.retrieveProjectDataSetFlatStructure(session, broker, dataSet, types, false);
      }
      broker.close();
   }
}
