/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_costs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityIfc;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.util.OpProjectCalendar;

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
   public static final int TRAVEL_COST_INDEX = 1;
   public static final int MATERIAL_COST_INDEX = 2;
   public static final int EXTERNAL_COST_INDEX = 3;
   public static final int MISC_COST_INDEX = 4;
   public static final int PROCEEDS_COST_INDEX = 5;

   /**
    * Fills the given dataSet with cost related information for
    *
    * @param broker            Broker instance to use for db operations.
    * @param project           Project to get the cost information for.
    * @param max_outline_level Detail level. Activities with outline level > max_outline_level won't be added to the
    *                          result. If the value is <code>Integer.MAX_VALUE</code>, this search criteria will be ignored.
    * @param data_set          Cost data set to be filled up.
    * @param costNames         Map with the costs display titles.
    */
   public static void fillCostsDataSet(OpProjectSession session, OpBroker broker, OpProjectNode project, int max_outline_level, XComponent data_set, Map costNames) {
      OpProjectPlanVersion version = project.getPlan().getBaseVersion();
      if (version == null) {
         return;
      }
      fillCostsDataSet(session, broker, version, max_outline_level, data_set, costNames);
   }

   /**
    * Fills the given dataSet with cost related information for
    *
    * @param broker            Broker instance to use for db operations
    * @param planVersion the <code>OpProjectPlanVersion</code> which will be used to retrieve the (effort) related information
    * @param max_outline_level Detail level. Activities with outline level > max_outline_level won't be added to the
    *                          result. If the value is <code>Integer.MAX_VALUE</code>, this search criteria will be ignored
    * @param data_set          Cost data set to be filled up.
    * @param costNames         Map with the costs display titles.
    */
   public static void fillCostsDataSet(OpProjectSession session,
         OpBroker broker, OpProjectPlanVersion planVersion,
         int max_outline_level, XComponent data_set, Map costNames) {
      if(planVersion == null) {
         return;
      }

      //this query could be improved by filtering out milestones and deleted activities when the plan doesn't have a baseline
      StringBuffer queryString = new StringBuffer("from OpActivityVersion as av where av.PlanVersion.id = :planVersionId");
      if (max_outline_level != Integer.MAX_VALUE) {
         queryString.append(" and av.OutlineLevel <= :maxLevel");
      }
      queryString.append(" order by av.Sequence");

      OpQuery query = broker.newQuery(queryString.toString());
      query.setLong("planVersionId", planVersion.getId());
      if (max_outline_level != Integer.MAX_VALUE) {
         query.setLong("maxLevel", max_outline_level);
      }

      Map<String, OpActivityVersion> importedProjectsMap = new HashMap<String, OpActivityVersion>();
      Iterator<OpActivityVersion> iActs = broker.iterate(query);
      while (iActs.hasNext()) {
         OpActivityVersion activity = iActs.next();
         if (activity.getSubProject() != null) {
            importedProjectsMap.put(activity.getSubProject().locator(), OpActivityDataSetFactory.getInstance().prepareProjectHeadActivityVersion(activity.getSubProject()));
         }
      }

      Iterator<Entry<String, OpActivityVersion>> piait = importedProjectsMap.entrySet().iterator();
      while (piait.hasNext()) {
         Entry<String, OpActivityVersion> iAct = piait.next();
         OpActivityVersion av = iAct.getValue();
         long projectNodeId = OpLocator.parseLocator(iAct.getKey()).getID();

         XComponent tmpDataSet = new XComponent(XComponent.DATA_SET);
         OpProjectCalendar calendar = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, planVersion.getProjectPlan());
         OpActivityDataSetFactory.getInstance().prepareSubProjectActualData(session, broker, calendar, av, iAct.getKey(), tmpDataSet);
      }
      
      // reset:
      Iterator<OpActivityVersion> activities = broker.iterate(query);
      while (activities.hasNext()) {
         OpActivityVersion activity = activities.next();
         if (activity.getType() == OpActivity.MILESTONE) {
            continue;
         }
         if (activity.getMasterActivityVersion() != null) {
            addActivityToCostDataSet(activity.getMasterActivityVersion(), costNames, data_set);
         }
         else if (activity.getSubProject() != null) {
            OpActivityVersion iAct = importedProjectsMap.get(activity.getSubProject().locator());
            addActivityToCostDataSet(iAct, costNames, data_set);
         }
         else {
            addActivityToCostDataSet(activity, costNames, data_set);
         }
      }
      
      OpQuery adhocTasksQuery = broker.newQuery("from OpActivity as a where a.ProjectPlan.id = :planId and a.Type = :adhocType order by a.Sequence");
      adhocTasksQuery.setLong("planId", planVersion.getProjectPlan().getId());
      adhocTasksQuery.setByte("adhocType", OpActivity.ADHOC_TASK);
      Iterator<OpActivity> adhocTasks = broker.iterate(adhocTasksQuery);
      while (adhocTasks.hasNext()) {
         OpActivity activity = adhocTasks.next();
         if (activity.getType() == OpActivity.MILESTONE) {
            continue;
         }
         addActivityToCostDataSet(activity, costNames, data_set);
      }
   }

   /**
    * Adds the cost info for the given activity to the data set.
    *
    * @param data_set  Data set to add the information data row to.
    * @param activity  Activity to get the info for.
    * @param costNames Map with the display text for costs.
    */
   private static void addActivityToCostDataSet(OpActivityIfc activity, Map costNames, XComponent data_set) {
      // NL: Remaining is estimation of resource (effortToComplete); predicted = actual + remaining (Therefore, deviation = predicted - base)

      XComponent baseActivityRow = createActivityRow(activity);
      data_set.addChild(baseActivityRow);
      
      //predicted
      double activityPredicted = addActivityCostDetails(costNames, activity, data_set);
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
   private static double addActivityCostDetails(Map costNames, OpActivityIfc activity, XComponent data_set) {
      double remaining;
      double predicted;
      double deviation;
      double activityPredicted = 0;
      String text = null;
      OpActivityIfc act = activity;
      for (int type = 0; type <= 5; type++) {
         double base = 0;
         double actual = 0;
         switch (type) {
            case PERSONNEL_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(PERSONNEL_COST_INDEX);
               }
               base = activity.getBasePersonnelCosts();
               actual = act == null ? 0d : act.getActualPersonnelCosts();
               break;
            case MATERIAL_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(MATERIAL_COST_INDEX);
               }
               base = activity.getBaseMaterialCosts();
               actual = act == null ? 0d : act.getActualMaterialCosts();
               break;
            case TRAVEL_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(TRAVEL_COST_INDEX);
               }
               base = activity.getBaseTravelCosts();
               actual = act == null ? 0d : act.getActualTravelCosts();
               break;
            case EXTERNAL_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(EXTERNAL_COST_INDEX);
               }
               base = activity.getBaseExternalCosts();
               actual = act == null ? 0d : act.getActualExternalCosts();
               break;
            case MISC_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(MISC_COST_INDEX);
               }
               base = activity.getBaseMiscellaneousCosts();
               actual = act == null ? 0d : act.getActualMiscellaneousCosts();
               break;
            case PROCEEDS_COST_INDEX:
               if (costNames != null) {
                  text = (String) costNames.get(PROCEEDS_COST_INDEX);
               }
               base = activity.getBaseProceeds();
               actual = act == null ? 0d : act.getActualProceeds();
               break;
         }
         remaining = base - actual;

         //calculate predicted based on the children activities
         predicted = childrenSumPredictedCosts(act, type);

         //do not add the proceeds to the expected costs
         if (type != PROCEEDS_COST_INDEX) {
            activityPredicted += predicted;
         }

         deviation = predicted - base;

         XComponent data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setOutlineLevel((activity.getOutlineLevel() < 0 ? 0 : activity.getOutlineLevel()) + 1);
         data_row.setExpanded(true);
         XComponent data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setStringValue(text); //0 - name
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(base);  //1 - base value
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(actual); //2 - actual value
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(remaining); //3 - remaining value
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(predicted); //4- predicted value
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(deviation); //5 - deviation value
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         double percentDeviation = OpActivityDataSetFactory.calculatePercentDeviation(base, deviation);
         data_cell.setDoubleValue(percentDeviation); //6 - percent deviation
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL); //7 - type
         //in the case of cost rows the type cell looses it's meaning of "activity type" and it is used only
         //to determine the style used to draw the row
         if (type != PROCEEDS_COST_INDEX) {
            data_cell.setByteValue((byte) 0);
         }
         else {
            data_cell.setByteValue((byte) 3);
         }
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
   private static XComponent createActivityRow(OpActivityIfc activity) {
      double base = activity.calculateBaseCost();
      double actual = activity.calculateActualCost();
      double remaining = base - actual;

      XComponent data_row = new XComponent(XComponent.DATA_ROW);
      data_row.setOutlineLevel(activity.getOutlineLevel() < 0 ? 0 : activity.getOutlineLevel());
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

      //predicted, deviation and %deviation are only added without value (values are set after calculating the children)
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_row.addChild(data_cell); //4 - predicted

      data_cell = new XComponent(XComponent.DATA_CELL);
      data_row.addChild(data_cell); //5 deviation

      data_cell = new XComponent(XComponent.DATA_CELL);
      data_row.addChild(data_cell);  //6 %deviation

      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setByteValue(activity.getType());
      data_row.addChild(data_cell); //7 - type

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
   private static double childrenSumPredictedCosts(OpActivityIfc  activity, int type) {
      double predicted;
      switch (type) {
         case PERSONNEL_COST_INDEX:
            predicted = (activity == null ? 0d : activity.getActualPersonnelCosts()) + (activity == null ? 0d : activity.getRemainingPersonnelCosts());
            break;
         case MATERIAL_COST_INDEX:
            predicted = (activity == null ? 0d : activity.getActualMaterialCosts()) + (activity == null ? 0d : activity.getRemainingMaterialCosts());
            break;
         case TRAVEL_COST_INDEX:
            predicted = (activity == null ? 0d : activity.getActualTravelCosts()) + (activity == null ? 0d : activity.getRemainingTravelCosts());
            break;
         case EXTERNAL_COST_INDEX:
            predicted = (activity == null ? 0d : activity.getActualExternalCosts()) + (activity == null ? 0d : activity.getRemainingExternalCosts());
            break;
         case MISC_COST_INDEX:
            predicted = (activity == null ? 0d : activity.getActualMiscellaneousCosts()) + (activity == null ? 0d : activity.getRemainingMiscellaneousCosts());
            break;
         case PROCEEDS_COST_INDEX:
            predicted = (activity == null ? 0d : activity.getActualProceeds()) + (activity == null ? 0d : activity.getRemainingProceeds());
            break;
         default: {
            throw new IllegalArgumentException("Invalid cost type parameter");
         }
      }
      return predicted;
   }
}
