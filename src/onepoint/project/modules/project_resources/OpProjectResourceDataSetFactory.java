/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_resources;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.project.*;

import java.util.*;

/**
 * DataSet Factory for resource controlling.
 *
 * @author mihai.costin
 */
public final class OpProjectResourceDataSetFactory {

   public final static int ACTIVITY_RESOURCE_COLUMN_INDEX = 0;
   public final static int BASE_COLUMN_INDEX = 1;
   public final static int ACTUAL_COLUMN_INDEX = 2;
   public final static int REMAINING_COLUMN_INDEX = 3;
   public final static int PREDICTED_COLUMN_INDEX = 4;
   public final static int DEVIATION_COLUMN_INDEX = 5;
   public final static int DEVIATION100_COLUMN_INDEX = 6;


   /**
    * Fills the given dataSet with resource (effort) related information.
    *
    * @param broker            Instance to use for db operations.
    * @param project           ProjectNode to get the effort information for.
    * @param max_outline_level Detail level. In the resulting data set will be added only those activities that have
    *                          outline level < max_outline_level
    * @param data_set          DataSet to be filled up with the info.
    */
   public static void fillEffortDataSet(OpBroker broker, OpProjectNode project, int max_outline_level, XComponent data_set) {

      StringBuffer queryString = new StringBuffer("from OpActivity as activity where activity.ProjectPlan.ProjectNode.ID = ?");
      queryString.append(" and activity.OutlineLevel <= ? order by activity.Sequence");
      OpQuery query = broker.newQuery(queryString.toString());
      query.setID(0, project.getID());
      query.setInteger(1, max_outline_level);
      List<OpActivity> activities = broker.list(query);

      double predicted;
      Iterator assignments;
      OpActivity previous_visible_activity = null;
      HashMap<Long, OpProjectResourceSummary> resource_summaries = new HashMap<Long, OpProjectResourceSummary>();
      OpProjectResourceSummary resource_summary;
      long resource_id;

      boolean useBaseline = false;
      if (project.getPlan().getBaselineVersion() != null) {
         useBaseline = true;
         for (OpActivity activity : activities) {
            activity.setIsUsingBaselineValues(true);
         }
      }

      Set<Long> baselineResource = new HashSet<Long>();
      for (OpActivity activity : activities) {

         if (useBaseline) {
            if (!activity.isInBaselineVersion()) {
               continue;
            }
            // Filter out milestones
            if (activity.getBaselineVersion().getType() == OpActivity.MILESTONE && activity.getType() == OpActivity.MILESTONE) {
               continue;
            }
         }
         else {
            // Filter out milestones
            if (activity.getType() == OpActivity.MILESTONE) {
               continue;
            }
            if (activity.getDeleted()) {
               continue;
            }
         }

         // Add previous visible activity with summarized values and reset both
         if (previous_visible_activity != null) {
            addActivityToEffortDataSet(data_set, previous_visible_activity, resource_summaries);
         }
         resource_summaries.clear();
         previous_visible_activity = activity;

         baselineResource = new HashSet<Long>();
         if (useBaseline) {
            //add version assignments
            OpActivityVersion version = activity.getBaselineVersion();
            Set<OpAssignmentVersion> assignmentVersions = version.getAssignmentVersions();
            for (OpAssignmentVersion assignmentVersion : assignmentVersions) {
               resource_id = assignmentVersion.getResource().getID();
               resource_summary = resource_summaries.get(new Long(resource_id));
               if (resource_summary == null) {
                  resource_summary = new OpProjectResourceSummary(resource_id, assignmentVersion.getResource().getName());
                  resource_summaries.put(resource_id, resource_summary);
               }
               resource_summary.addBaseEffort(assignmentVersion.getBaseEffort());
               baselineResource.add(resource_id);
            }
         }

         // Add values of assignments to resource summaries of previous visible row
         assignments = activity.getAssignments().iterator();
         for (OpAssignment assignment : activity.getAssignments()) {
            assignment = (OpAssignment) (assignments.next());
            resource_id = assignment.getResource().getID();
            resource_summary = resource_summaries.get(new Long(resource_id));
            if (resource_summary == null) {
               resource_summary = new OpProjectResourceSummary(resource_id, assignment.getResource().getName());
               resource_summaries.put(resource_id, resource_summary);
            }

            if (!baselineResource.contains(resource_id)) {
               resource_summary.addBaseEffort(assignment.getBaseEffort());
            }

            predicted = assignment.getActualEffort() + assignment.getRemainingEffort();
            resource_summary.addActualEffort(assignment.getActualEffort());
            resource_summary.addEffortToComplete(assignment.getRemainingEffort());
            resource_summary.addPredictedEffort(predicted);
         }
      }

      // Add last visible activity
      if (previous_visible_activity != null) {
         addActivityToEffortDataSet(data_set, previous_visible_activity, resource_summaries);
      }

      if (useBaseline) {
         for (OpActivity activity : activities) {
            activity.setIsUsingBaselineValues(false);
         }
      }


   }

   /**
    * Adds info about the given activity to the effort data set (data_set).
    *
    * @param data_set           Effort data set
    * @param activity           Activity entity
    * @param resource_summaries Instance containing the resource info.
    */
   private static void addActivityToEffortDataSet(XComponent data_set, OpActivity activity, HashMap resource_summaries) {
      // NL: Remaining is estimation of resource (effortToComplete); predicted = actual + remaining
      // (Therefore, deviation = predicted - base)
      double base = activity.getBaseEffort();
      double actual = activity.getActualEffort();
      // TODO: NL actually means effort-to-complete w/remaining (Add as separate, additional column?)
      double remaining = base - actual;
      double predicted = actual + activity.getRemainingEffort();
      // TODO: Check algorithm for calculated predicted effort

      double deviation = predicted - base;
      XComponent data_row = new XComponent(XComponent.DATA_ROW);

      data_row.setOutlineLevel(activity.getOutlineLevel());
      data_row.setExpanded(true);
      XComponent data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setStringValue(activity.getName());
      data_row.addChild(data_cell); // 0 Name
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(base);
      data_row.addChild(data_cell);// 1 Base effort
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(actual);
      data_row.addChild(data_cell);// 2 Actual effort
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(remaining);
      data_row.addChild(data_cell);// 3 Remaining effort
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(predicted);
      data_row.addChild(data_cell);// 4 Predicted effort
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(deviation);
      data_row.addChild(data_cell);// 5 Deviation
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(OpActivityDataSetFactory.calculatePercentDeviation(base, deviation));
      data_row.addChild(data_cell);// 6 %deviation
      data_set.addChild(data_row);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(activity.getType());
      data_row.addChild(data_cell);// 7 Type/Style

      // Add resource summaries
      addResourceSummary(activity, resource_summaries.values(), data_set);
   }

   /**
    * Adds detailed information about the activity's resources from the map.
    *
    * @param activity           activity entity for which the resource info is added.
    * @param resource_summaries collection of resourceSummary
    * @param data_set           dataSet where the info will be added as dataRows
    */
   private static void addResourceSummary(OpActivity activity, Collection resource_summaries, XComponent data_set) {
      double deviation;
      XComponent data_row;
      XComponent data_cell;
      byte outline_level = (byte) (activity.getOutlineLevel() + 1);
      OpProjectResourceSummary resource_summary;
      Iterator i = resource_summaries.iterator();
      while (i.hasNext()) {
         resource_summary = (OpProjectResourceSummary) (i.next());
         double resourceBase = resource_summary.getBaseEffort();
         double resourceActual = resource_summary.getActualEffort();
         double resourceRemaining = resourceBase - resourceActual;
         double resourcePredicted = resource_summary.getPredictedEffort();

         deviation = resourcePredicted - resourceBase;
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setOutlineLevel(outline_level);
         data_row.setExpanded(true);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setStringValue(resource_summary.getResourceName()); // 0 resource name
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(resourceBase);// 1 resource base
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(resourceActual);// 2 resource actual
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(resourceRemaining);// 3 resource remaining
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(resourcePredicted);// 4 resource predicted
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(deviation);// 5 resource deviation
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         double percentDeviation = OpActivityDataSetFactory.calculatePercentDeviation(resourceBase, deviation);
         data_cell.setDoubleValue(percentDeviation);// 6 resource deviation
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setByteValue((byte) 0);// 7 type/style
         data_row.addChild(data_cell);
         data_set.addChild(data_row);
      }
   }


}
