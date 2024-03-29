/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_costs.forms;

import java.util.HashMap;
import java.util.Map;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.project_costs.OpProjectCostsDataSetFactory;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

public class OpProjectCostsFormProvider implements XFormProvider {

   private final static String COST_SET = "CostSet";
   private final static String PRINT_BUTTON = "PrintButton";

   protected final static String PROJECT_ID = "project_id";

   // Project costs resource map
   protected final static String PROJECT_COSTS_PROJECT_COSTS = "project_costs.project_costs";

   // Cost types
   protected final static String PERSONNEL = "${Personnel}";
   protected final static String PROCEEDS = "${Proceeds}";
   protected final static String TRAVEL = "${Travel}";
   protected final static String MATERIAL = "${Material}";
   protected final static String EXTERNAL = "${External}";
   protected final static String MISCELLANEOUS = "${Miscellaneous}";
   private final static String PRINT_TITLE = "PrintTitle";
   private final static String TOTAL = "Total";
   private final static String FOOTER_DATA_SET = "CostFooterSet";

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

            // Locate data set in form
            XComponent data_set = form.findComponent(COST_SET);

            //print title
            form.findComponent(PRINT_TITLE).setStringValue(project.getName());
            form.findComponent(PRINT_BUTTON).setEnabled(true);
            // Create dynamic resource summaries for collection-activities
            // (Note: Value of collection-activities have been set on check-in/work-calculator)

            int max_outline_level = getMaxOutlineLevel(form, session);
            OpProjectPlanVersion usedVersion = getProjectVersion(form, session, project);
            createViewDataSet(session, broker, usedVersion, max_outline_level, data_set);
         }
      }
      finally {
         broker.close();
      }

   }

   protected void createViewDataSet(OpProjectSession session, OpBroker broker, OpProjectPlanVersion planVersion, int max_outline_level, XComponent data_set) {
      // I18ned cost types
      XLocalizer localizer = new XLocalizer();
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(),
           PROJECT_COSTS_PROJECT_COSTS);
      String personnel = PERSONNEL;
      String proceeds = PROCEEDS;
      String travel = TRAVEL;
      String material = MATERIAL;
      String external = EXTERNAL;
      String miscellaneous = MISCELLANEOUS;
      Map costNames = new HashMap();
      if (resourceMap != null) {
         localizer.setResourceMap(resourceMap);
         costNames.put(new Integer(OpProjectCostsDataSetFactory.PERSONNEL_COST_INDEX), localizer.localize(personnel));
         costNames.put(new Integer(OpProjectCostsDataSetFactory.TRAVEL_COST_INDEX), localizer.localize(travel, session.getLocalizerParameters()));
         costNames.put(new Integer(OpProjectCostsDataSetFactory.MATERIAL_COST_INDEX), localizer.localize(material, session.getLocalizerParameters()));
         costNames.put(new Integer(OpProjectCostsDataSetFactory.EXTERNAL_COST_INDEX), localizer.localize(external, session.getLocalizerParameters()));
         costNames.put(new Integer(OpProjectCostsDataSetFactory.MISC_COST_INDEX), localizer.localize(miscellaneous, session.getLocalizerParameters()));
         costNames.put(new Integer(OpProjectCostsDataSetFactory.PROCEEDS_COST_INDEX), localizer.localize(proceeds));
      }
      OpProjectCostsDataSetFactory.fillCostsDataSet(session, broker, planVersion, max_outline_level, data_set, costNames);
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
