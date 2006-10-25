/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_costs.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class OpProjectCostsFormProvider implements XFormProvider {

   public final static String COST_SET = "CostSet";
   public final static String PRINT_BUTTON = "PrintButton";

   public final static String PROJECT_ID = "project_id";

   public final static int ACTIVITY_COST_TYPE_COLUMN_INDEX = 0;
   public final static int BASE_COLUMN_INDEX = 1;
   public final static int ACTUAL_COLUMN_INDEX = 2;
   public final static int REMAINING_COLUMN_INDEX = 3;
   public final static int PREDICTED_COLUMN_INDEX = 4;
   public final static int DEVIATION_COLUMN_INDEX = 5;
   public final static int DEVIATION100_COLUMN_INDEX = 6;

   // Project costs resource map
   public final static String PROJECT_COSTS_PROJECT_COSTS = "project_costs.project_costs";

   // Cost types
   public final static String PERSONNEL = "{$Personnel}";
   public final static String TRAVEL = "{$Travel}";
   public final static String MATERIAL = "{$Material}";
   public final static String EXTERNAL = "{$External}";
   public final static String MISCELLANEOUS = "{$Miscellaneous}";
   public final static String PRINT_TITLE = "PrintTitle";

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
      else
         project_locator = (String) (session.getVariable(PROJECT_ID));

      if (project_locator != null) {

         OpProjectNode project = (OpProjectNode) (broker.getObject(project_locator));

         // Locate data set in form
         XComponent data_set = form.findComponent(COST_SET);

         //print title
         form.findComponent(PRINT_TITLE).setStringValue(project.getName());
         form.findComponent(PRINT_BUTTON).setEnabled(true);
         // Create dynamic resource summaries for collection-activities
         // (Note: Value of collection-activities have been set on check-in/work-calculator)

         int max_outline_level = getMaxOutlineLevel(form, session);
         createViewDataSet(session, broker, project, max_outline_level, data_set);
      }
      broker.close();
   }

   protected void createViewDataSet(OpProjectSession session, OpBroker broker, OpProjectNode project, int max_outline_level, XComponent data_set) {
      // I18ned cost types
      XLocalizer localizer = new XLocalizer();
      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(),
            PROJECT_COSTS_PROJECT_COSTS);
      String personnel = PERSONNEL;
      String travel = TRAVEL;
      String material = MATERIAL;
      String external = EXTERNAL;
      String miscellaneous = MISCELLANEOUS;
      if (resourceMap != null) {
         localizer.setResourceMap(resourceMap);
         personnel = localizer.localize(personnel);
         travel = localizer.localize(travel);
         material = localizer.localize(material);
         external = localizer.localize(external);
         miscellaneous = localizer.localize(miscellaneous);
      }

      OpQuery query = broker
            .newQuery("from OpActivity as activity where activity.ProjectPlan.ProjectNode.ID = ? and activity.Deleted = false order by activity.Sequence");
      query.setID(0, project.getID());
      Iterator activities = broker.iterate(query);
      OpActivity activity;
      while (activities.hasNext()) {
         activity = (OpActivity) (activities.next());
         if (activity.getType() == OpActivity.MILESTONE) {
            continue;
         }
         if (activity.getOutlineLevel() <= max_outline_level) {
            _addActivity(data_set, activity, personnel, travel, material, external, miscellaneous);
         }
      }
   }

   protected void _addActivity(XComponent data_set, OpActivity activity, String personnel, String travel,
         String material, String external, String miscellaneous) {
      // NL: Remaining is estimation of resource (effortToComplete); predicted = actual + remaining
      // (Therefore, deviation = predicted - base)
      double base = activity.getBasePersonnelCosts();
      base += activity.getBaseTravelCosts();
      base += activity.getBaseMaterialCosts();
      base += activity.getBaseExternalCosts();
      base += activity.getBaseMiscellaneousCosts();
      double actual = activity.getActualPersonnelCosts();
      actual += activity.getActualTravelCosts();
      actual += activity.getActualMaterialCosts();
      actual += activity.getActualExternalCosts();
      actual += activity.getActualMiscellaneousCosts();
      // TODO: NL actually means effort-to-complete w/remaining
      // (Add as separate, additional column?)
      double remaining = base - actual;
      double predicted;
      // TODO: Check algorithm for calculated predicted effort
      if (actual > 0) {
         if (activity.getComplete() > 0) {
            predicted = actual * 100 / activity.getComplete();
         }
         else {
            predicted = Math.max(actual, base);
         }
      }
      else {
         predicted = base - (base * activity.getComplete() / 100);
      }
      double deviation = predicted - base;
      XComponent data_row = new XComponent(XComponent.DATA_ROW);
      byte outline_level = activity.getOutlineLevel();
      data_row.setOutlineLevel(outline_level);
      data_row.setExpanded(true);
      XComponent data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setStringValue(activity.getName());
      data_row.addChild(data_cell); //0 - name
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(base);
      data_row.addChild(data_cell); //1 - base
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(actual);
      data_row.addChild(data_cell); //2 - actual
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(remaining);
      data_row.addChild(data_cell); //3 - remaining
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(predicted);
      data_row.addChild(data_cell); //4 - predicted
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(deviation);
      data_row.addChild(data_cell); //5 deviation
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_row.addChild(data_cell);  //6 %deviation

      data_set.addChild(data_row);
      XComponent baseActivityRow = data_row;
      // Add detailed costs
      outline_level++;
      String text = null;
      // TODO: Use dynamic language resources (stored in this form's language resource file)
      double activityPredicted = 0;
      double activityBase = base;
      for (int i = 0; i <= 4; i++) {
         switch (i) {
            case 0:
               text = personnel;
               base = activity.getBasePersonnelCosts();
               actual = activity.getActualPersonnelCosts();
               break;
            case 1:
               text = material;
               base = activity.getBaseMaterialCosts();
               actual = activity.getActualMaterialCosts();
               break;
            case 2:
               text = travel;
               base = activity.getBaseTravelCosts();
               actual = activity.getActualTravelCosts();
               break;
            case 3:
               text = external;
               base = activity.getBaseExternalCosts();
               actual = activity.getActualExternalCosts();
               break;
            case 4:
               text = miscellaneous;
               base = activity.getBaseMiscellaneousCosts();
               actual = activity.getActualMiscellaneousCosts();
               break;
         }
         // TODO: NL actually means cost-to-complete w/remaining
         // (Add as separate, additional column?)
         remaining = base - actual; // resource_summary.getRemainingEffort();
         // TODO: Actually we probably need a form of cost-to-complete

         //calculate predicted based on the children activities
         predicted = childrenSumPredicted(activity, i);
         activityPredicted += predicted;

         deviation = predicted - base;
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setOutlineLevel(outline_level);
         data_row.setExpanded(true);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setStringValue(text);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(base);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(actual);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(remaining);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(predicted);
         data_row.addChild(data_cell);
         data_set.addChild(data_row);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(deviation);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         if (base != 0) {
            data_cell.setDoubleValue(deviation * 100 / base);
         }
         else {
            if (deviation != 0){
               data_cell.setDoubleValue(Double.MAX_VALUE);
            }
            else {
               data_cell.setDoubleValue(0);
            }
         }
         data_row.addChild(data_cell);
      }


      //predicted
      data_cell = (XComponent) baseActivityRow.getChild(4);
      data_cell.setDoubleValue(activityPredicted);
      //deviation
      data_cell = (XComponent) baseActivityRow.getChild(5);
      double activityDeviation = activityPredicted - activityBase;
      data_cell.setDoubleValue(activityDeviation);
      //% deviation
      data_cell = (XComponent) baseActivityRow.getChild(6);
      if (activityBase != 0) {
         data_cell.setDoubleValue(activityDeviation * 100 / activityBase);
      }
      else {
         if (deviation != 0){
            data_cell.setDoubleValue(Double.MAX_VALUE);
         }
         else {
            data_cell.setDoubleValue(0);
         }
      }

   }

   /**
    * Calculates the predicted cost for an activity recursively by adding the predicted for its children
    *
    * @param activity activity to calculate the predicted for
    * @param type     type of cost
    * @return value of the predicted
    */
   private double childrenSumPredicted(OpActivity activity, int type) {
      Set activities = activity.getSubActivities();
      double predicted = 0;
      if (activities.isEmpty()) {
         double base = 0, actual = 0;
         switch (type) {
            case 0:
               base = activity.getBasePersonnelCosts();
               actual = activity.getActualPersonnelCosts();
               break;
            case 1:
               base = activity.getBaseMaterialCosts();
               actual = activity.getActualMaterialCosts();
               break;
            case 2:
               base = activity.getBaseTravelCosts();
               actual = activity.getActualTravelCosts();
               break;
            case 3:
               base = activity.getBaseExternalCosts();
               actual = activity.getActualExternalCosts();
               break;
            case 4:
               base = activity.getBaseMiscellaneousCosts();
               actual = activity.getActualMiscellaneousCosts();
               break;
         }
         if (actual > 0) {
            if (activity.getComplete() > 0) {
               predicted = actual * 100 / activity.getComplete();
            }
            else {
               predicted = Math.max(actual, base);
            }
         }
         else {
            predicted = base - (base * activity.getComplete() / 100);
         }
      }
      else {
         for (Iterator iterator = activities.iterator(); iterator.hasNext();) {
            OpActivity child = (OpActivity) iterator.next();
            predicted += childrenSumPredicted(child, type);
         }
      }
      return predicted;
   }


   protected int getMaxOutlineLevel(XComponent form, OpProjectSession session) {
      return 0;
   }
}
