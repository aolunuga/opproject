/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_resources.forms;

import java.util.HashMap;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.project_resources.OpProjectResourceDataSetFactory;
import onepoint.service.server.XSession;

public class OpProjectResourcesFormProvider implements XFormProvider {

   private final static String RESOURCE_SET = "ResourceSet";

   protected final static String PROJECT_ID = "project_id";

   private final static String PRINT_TITLE = "PrintTitle";
   private final static String PRINT_BUTTON = "PrintButton";
   private final static String RESOURCE_FOOTER_DATA_SET = "ResourceFooterSet";
   private final static String TOTAL_CAPTION = "Total";
   // NL: Remaining is estimation of resource (effortToComplete); predicted = actual + remaining
   // (Therefore, deviation = predicted - base)

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      try {
         // Decide on project-ID and retrieve project
         String project_id_string = (String) (parameters.get(PROJECT_ID));
         if (project_id_string == null) {
            String selectLocator = (String)parameters.get(XFormProvider.SELECT);
            if (selectLocator != null) {
               project_id_string = selectLocator;
            }
         }
         if (project_id_string != null) {
            // Get open project-ID from parameters and set project-ID session variable
            session.setVariable(PROJECT_ID, project_id_string);
         }
         else {
            project_id_string = (String) (session.getVariable(PROJECT_ID));
         }

         if (project_id_string != null) {

            OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string));
            //print title
            form.findComponent(PRINT_TITLE).setStringValue(project.getName());
            form.findComponent(PRINT_BUTTON).setEnabled(true);

            // Locate data set in form
            XComponent data_set = form.findComponent(RESOURCE_SET);
            int max_outline_level = getMaxOutlineLevel(form, session);
            OpProjectPlanVersion usedVersion = getProjectVersion(form, session, project);
            // Create dynamic resource summaries for collection-activities
            // (Note: Value of collection-activities have been set on check-in/work-calculator)
            OpProjectResourceDataSetFactory.fillEffortDataSet(session, broker, usedVersion, max_outline_level, data_set, true);
         }
      }
      finally {
         broker.close();
      }
   }

   protected int getMaxOutlineLevel(XComponent form, OpProjectSession session) {
      return 0;
   }

   /**
    * Template method for getProjectVersion(XComponent, OpProjectSession, OpProjectNode) in the CLOSED version
    *
    * @return the project plan's base version
    */
   protected OpProjectPlanVersion getProjectVersion(XComponent form, OpProjectSession session, OpProjectNode project) {
      return project.getPlan().getBaseVersion();
   }
}