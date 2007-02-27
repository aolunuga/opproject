/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_resources.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project_resources.OpProjectResourceDataSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpProjectResourcesFormProvider implements XFormProvider {

   private final static String RESOURCE_SET = "ResourceSet";

   protected final static String PROJECT_ID = "project_id";

   private final static String PRINT_TITLE = "PrintTitle";
   private final static String PRINT_BUTTON = "PrintButton";
   // NL: Remaining is estimation of resource (effortToComplete); predicted = actual + remaining
   // (Therefore, deviation = predicted - base)

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {


      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // Decide on project-ID and retrieve project
      String project_locator = (String) (parameters.get(PROJECT_ID));
      if (project_locator != null) {
         // Get open project-ID from parameters and set project-ID session variable
         session.setVariable(PROJECT_ID, project_locator);
      }
      else {
         project_locator = (String) (session.getVariable(PROJECT_ID));
      }

      if (project_locator != null) {

         OpProjectNode project = (OpProjectNode) (broker.getObject(project_locator));
         //print title
         form.findComponent(PRINT_TITLE).setStringValue(project.getName());
         form.findComponent(PRINT_BUTTON).setEnabled(true);

         // Locate data set in form
         XComponent data_set = form.findComponent(RESOURCE_SET);
         int max_outline_level = getMaxOutlineLevel(form, session);
         // Create dynamic resource summaries for collection-activities
         // (Note: Value of collection-activities have been set on check-in/work-calculator)
         OpProjectResourceDataSetFactory.fillEffortDataSet(broker, project, max_outline_level, data_set);
      }
      broker.close();
   }

   protected int getMaxOutlineLevel(XComponent form, OpProjectSession session) {
      return 0;
   }

}