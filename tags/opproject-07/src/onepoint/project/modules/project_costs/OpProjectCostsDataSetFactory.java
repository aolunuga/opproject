/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_costs;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * DataSet Factory for project costs controlling.
 *
 * @author mihai.costin
 */
public final class OpProjectCostsDataSetFactory {

   public final static int ACTIVITY_COST_TYPE_COLUMN_INDEX = 0;
   public final static int BASE_COLUMN_INDEX = 1;
   public final static int ACTUAL_COLUMN_INDEX = 2;
   public final static int REMAINING_COLUMN_INDEX = 3;
   public final static int PREDICTED_COLUMN_INDEX = 4;
   public final static int DEVIATION_COLUMN_INDEX = 5;
   public final static int DEVIATION100_COLUMN_INDEX = 6;


   public static final int PERSONNEL_COST_INDEX = 0;
   public static final int MATERIAL_COST_INDEX = 1;
   public static final int TRAVEL_COST_INDEX = 2;
   public static final int EXTERNAL_COST_INDEX = 3;
   public static final int MISC_COST_INDEX = 4;

   /**
    * Fills the given dataSet with cost related information for
    *
    * @param broker            Broker instance to use for db operations.
    * @param project           Project to get the cost information for.
    * @param max_outline_level Detail level. Activities with outline level > max_outline_level won't be added to the result.
    * @param data_set          Cost data set to be filled up.
    * @param costNames         Map with the costs display titles.
    */
   public static void fillCostsDataSet(OpBroker broker, OpProjectNode project, int max_outline_level, XComponent data_set, Map costNames) {
      OpQuery query = broker.newQuery("from OpActivity as activity where activity.ProjectPlan.ProjectNode.ID = ? and activity.Deleted = false order by activity.Sequence");
      query.setID(0, project.getID());
      Iterator activities = broker.iterate(query);
      OpActivity activity;
      while (activities.hasNext()) {
         activity = (OpActivity) (activities.next());
         if (activity.getType() == OpActivity.MILESTONE) {
            continue;
         }
         if (activity.getOutlineLevel() <= max_outline_level) {
            addActivityToCostDataSet(data_set, activity, costNames);
         }
      }
   }

   /**
    * Adds the cost info for the given activity to the data set.
    *
    * @param data_set  Data set to add the information data row to.
    * @param activity  Activity to get the info for.
    * @param costNames Map with the display text for costs.
    */
   private static void addActivityToCostDataSet(XComponent data_set, OpActivity activity, Map costNames) {
      // NL: Remaining is estimation of resource (effortToComplete); predicted = actual + remaining (Therefore, deviation = predicted - base)
      // TODO: NL actually means effort-to-complete w/remaining (Add as separate, additional column?)

      XComponent baseActivityRow = createActivityRow(activity);
      data_set.addChild(baseActivityRow);

      double activityPredicted = addActivityCostDetails(costNames, activity, data_set);

      //predicted
      XComponent data_cell = (XComponent) baseActivityRow.getChild(PREDICTED_COLUMN_INDEX);
      data_cell.setDoubleValue(activityPredicted);
      //deviation
      double activityBase = activity.calculateBaseCost();
      data_cell = (XComponent) baseActivityRow.getChild(DEVIATION_COLUMN_INDEX);
      double activityDeviation = activityPredicted - activityBase;
      data_cell.setDoubleValue(activityDeviation);
      //% deviation
      data_cell = (XComponent) baseActivityRow.getChild(DEVIATION100_COLUMN_INDEX);
      data_cell.setDoubleValue(OpActivityDataSetFactory.calculatePercentDeviation(activityBase, activityDeviation));
   }

   /**
    * Adds cost details to data set for the given activity (will be added on activity.outlinelevel + 1).
    *
    * @param costNames Name text for the costs.
    * @param activity  Activity for which the details are added.
    * @param data_set  DataSet that will contain the detail rows.
    * @return Predicted cost for given activity.
    */
   private static double addActivityCostDetails(Map costNames, OpActivity activity, XComponent data_set) {

      double remaining;
      double predicted;
      double deviation;
      double activityPredicted = 0;
      String text = null;
      for (int i = 0; i <= 4; i++) {
         double base = 0;
         double actual = 0;
         switch (i) {
            case PERSONNEL_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(new Integer(PERSONNEL_COST_INDEX));
               }
               base = activity.getBasePersonnelCosts();
               actual = activity.getActualPersonnelCosts();
               break;
            case MATERIAL_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(new Integer(MATERIAL_COST_INDEX));
               }
               base = activity.getBaseMaterialCosts();
               actual = activity.getActualMaterialCosts();
               break;
            case TRAVEL_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(new Integer(TRAVEL_COST_INDEX));
               }
               base = activity.getBaseTravelCosts();
               actual = activity.getActualTravelCosts();
               break;
            case EXTERNAL_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(new Integer(EXTERNAL_COST_INDEX));
               }
               base = activity.getBaseExternalCosts();
               actual = activity.getActualExternalCosts();
               break;
            case MISC_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(new Integer(MISC_COST_INDEX));
               }
               base = activity.getBaseMiscellaneousCosts();
               actual = activity.getActualMiscellaneousCosts();
               break;
         }
         // TODO: NL actually means cost-to-complete w/remaining(Add as separate, additional column?)
         // TODO: Actually we probably need a form of cost-to-complete
         remaining = base - actual;

         //calculate predicted based on the children activities
         predicted = childrenSumPredictedCosts(activity, i);
         activityPredicted += predicted;

         deviation = predicted - base;
         XComponent data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setOutlineLevel(activity.getOutlineLevel() + 1);
         data_row.setExpanded(true);
         XComponent data_cell = new XComponent(XComponent.DATA_CELL);
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
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(deviation);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(OpActivityDataSetFactory.calculatePercentDeviation(base, deviation));
         data_row.addChild(data_cell);
         data_set.addChild(data_row);
      }
      return activityPredicted;
   }


   /**
    * Creates a row with activity cost information using the given values.
    *
    * @param activity The activity entity to create the data row for.
    * @return data row containing cost realtef information for the given activity.
    */
   private static XComponent createActivityRow(OpActivity activity) {
      XComponent data_row;

      double base = activity.calculateBaseCost();
      double actual = activity.calculateActualCost();
      double remaining = base - actual;
      double predicted = calculatePredicted(activity.getComplete(), actual, base);
      double deviation = predicted - base;

      data_row = new XComponent(XComponent.DATA_ROW);
      data_row.setOutlineLevel(activity.getOutlineLevel());
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

      return data_row;
   }

   /**
    * Calculates the predicted value for the given actual/base using the complete value.
    *
    * @param activityComplete Activity % complete
    * @param actual           Activity actual cost
    * @param base             Activity base cost
    * @return Predicted value.
    */
   private static double calculatePredicted(double activityComplete, double actual, double base) {
      // TODO: Check algorithm for calculated predicted effort
      double predicted;
      if (actual > 0) {
         if (activityComplete > 0) {
            predicted = actual * 100 / activityComplete;
         }
         else {
            predicted = Math.max(actual, base);
         }
      }
      else {
         predicted = base - (base * activityComplete / 100);
      }
      return predicted;
   }

   /**
    * Calculates the predicted cost for an activity recursively by adding the predicted for its children
    *
    * @param activity activity to calculate the predicted for
    * @param type     type of cost
    * @return value of the predicted
    */
   private static double childrenSumPredictedCosts(OpActivity activity, int type) {
      Set activities = activity.getSubActivities();
      double predicted = 0;
      if (activities.isEmpty()) {
         double base = 0, actual = 0;
         switch (type) {
            case PERSONNEL_COST_INDEX:
               base = activity.getBasePersonnelCosts();
               actual = activity.getActualPersonnelCosts();
               break;
            case MATERIAL_COST_INDEX:
               base = activity.getBaseMaterialCosts();
               actual = activity.getActualMaterialCosts();
               break;
            case TRAVEL_COST_INDEX:
               base = activity.getBaseTravelCosts();
               actual = activity.getActualTravelCosts();
               break;
            case EXTERNAL_COST_INDEX:
               base = activity.getBaseExternalCosts();
               actual = activity.getActualExternalCosts();
               break;
            case MISC_COST_INDEX:
               base = activity.getBaseMiscellaneousCosts();
               actual = activity.getActualMiscellaneousCosts();
               break;
         }
         predicted = calculatePredicted(activity.getComplete(), actual, base);
      }
      else {
         for (Iterator iterator = activities.iterator(); iterator.hasNext();) {
            OpActivity child = (OpActivity) iterator.next();
            predicted += childrenSumPredictedCosts(child, type);
         }
      }
      return predicted;
   }
}
