/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityIfc;
import onepoint.project.modules.project.OpAssignmentIfc;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlanVersion;

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
    * @param maxOutlineLevel Detail level. In the resulting data set will be added only those activities that have
*                          outline level < maxOutlineLevel
    * @param dataSet          DataSet to be filled up with the info.
    * @param retrieveAllActivities a <code>boolean</code> whether to retrieve all the
    * activities or use the maxOutlineLevel parameter as a filter.
    * <FIXME author="Horia Chiorean" description="This method should be refactored. It violates SRP amongst other things">
    */
   public static void fillEffortDataSet(OpProjectSession session, OpBroker broker, OpProjectNode project,
        int maxOutlineLevel, XComponent dataSet, boolean retrieveAllActivities) {
      fillEffortDataSet(session, broker, project.getPlan().getBaseVersion(), maxOutlineLevel, dataSet, retrieveAllActivities);      
   }

   /**
    * Fills the given dataSet with resource (effort) related information.
    *
    * @param broker            Instance to use for db operations.
    * @param planVersion the <code>OpProjectPlanVersion</code> which will be used to retrieve the (effort) related information
    * @param maxOutlineLevel Detail level. In the resulting data set will be added only those activities that have
*                          outline level < maxOutlineLevel
    * @param dataSet          DataSet to be filled up with the info.
    * @param retrieveAllActivities a <code>boolean</code> whether to retrieve all the
    * activities or use the maxOutlineLevel parameter as a filter.
    * <FIXME author="Horia Chiorean" description="This method should be refactored. It violates SRP amongst other things">
    */
   public static void fillEffortDataSet(OpProjectSession session, OpBroker broker, OpProjectPlanVersion planVersion,
        int maxOutlineLevel, XComponent dataSet, boolean retrieveAllActivities) {

      if (planVersion == null) {
         return;
      }
      Iterator<OpActivityIfc> activities = retrieveActivities(broker, planVersion, maxOutlineLevel, retrieveAllActivities);

      addActivitiesToResourceDataSet(session, broker, planVersion,
            maxOutlineLevel, dataSet, retrieveAllActivities, activities);
      
      OpQuery adhocTasksQuery = broker.newQuery("from OpActivity as a where a.ProjectPlan.id = :planId and a.Type = :adhocType order by a.Sequence");
      adhocTasksQuery.setLong("planId", planVersion.getProjectPlan().getId());
      adhocTasksQuery.setByte("adhocType", OpActivity.ADHOC_TASK);
      Iterator<OpActivityIfc> adhocTasks = broker.iterate(adhocTasksQuery);

      addActivitiesToResourceDataSet(session, broker, planVersion,
            maxOutlineLevel, dataSet, retrieveAllActivities, adhocTasks);
      
   }

   private static void addActivitiesToResourceDataSet(OpProjectSession session,
         OpBroker broker, OpProjectPlanVersion planVersion,
         int maxOutlineLevel, XComponent dataSet,
         boolean retrieveAllActivities, Iterator<OpActivityIfc> activities) {
      OpActivityIfc previous_visible_activity = null;
      HashMap<Long, OpProjectResourceSummary> resource_summaries = new HashMap<Long, OpProjectResourceSummary>();
      while (activities.hasNext()) {
         
         OpActivityIfc activity = activities.next();

         // Filter out milestones
         if (activity.getType() == OpActivity.MILESTONE) {
            continue;
         }

         // Add previous visible activity with summarized values and reset both
         if (retrieveAllActivities) {
            if (activity.getOutlineLevel() <= maxOutlineLevel) {
               if (previous_visible_activity != null) {
                  addActivityToEffortDataSet(dataSet, previous_visible_activity, resource_summaries);
               }
               resource_summaries.clear();
               previous_visible_activity = activity;
            }
         }
         else {
            if (previous_visible_activity != null) {
               addActivityToEffortDataSet(dataSet, previous_visible_activity, resource_summaries);
            }
            resource_summaries.clear();
            previous_visible_activity = activity;
         }

         Set<Long> baselineResource = new HashSet<Long>();
            //add version assignments
         Set<? extends OpAssignmentIfc> assignments = activity.getAssignments();
         if (assignments != null) {
            for (OpAssignmentIfc assignmentVersion : assignments) {
               long resource_id = assignmentVersion.getResource().getId();
               OpProjectResourceSummary resource_summary = resource_summaries.get(new Long(resource_id));
               if (resource_summary == null) {
                  resource_summary = new OpProjectResourceSummary(resource_id, assignmentVersion.getResource().getName());
                  resource_summaries.put(resource_id, resource_summary);
               }
               resource_summary.addBaseEffort(assignmentVersion.getBaseEffort());
               baselineResource.add(resource_id);
            }
         }
         // Add values of assignments to resource summaries of previous visible row
         if (activity.getAssignments() != null) {
            for (OpAssignmentIfc assignment : activity.getAssignments()) {
               long resource_id = assignment.getResource().getId();
               OpProjectResourceSummary resource_summary = resource_summaries.get(new Long(resource_id));
               if (resource_summary == null) {
                  resource_summary = new OpProjectResourceSummary(resource_id, assignment.getResource().getName());
                  resource_summaries.put(resource_id, resource_summary);
               }
   
               if (!baselineResource.contains(resource_id)) {
                  resource_summary.addBaseEffort(assignment.getBaseEffort());
               }
   
               double predicted = assignment.getActualEffort() + assignment.getRemainingEffort();
               resource_summary.addActualEffort(assignment.getActualEffort());
               resource_summary.addEffortToComplete(assignment.getRemainingEffort());
               resource_summary.addPredictedEffort(predicted);
            }
         }
      }

      // Add last visible activity
      if (previous_visible_activity != null) {
         addActivityToEffortDataSet(dataSet, previous_visible_activity, resource_summaries);
      }
   }

   /**
    * Retrives activities which are to be used for building the effort data-set.
    * @param broker a <code>OpBroker</code> used for persistence operations.
    * @param projectId a <code>long</code> the id of a project
    * @param maxOutlineLevel an <code>int</code> the outline level of the activities which
    * should be retrived.
    * @param retrieveAllActivities  a <code>boolean</code> whether to retrieve all the
    * activities or not from the db. If not, the maxOutlineLevel parameter is used.
    * @return a <code>List</code> of <code>OpActivity</code>.
    */
   private static Iterator<OpActivityIfc> retrieveActivities(OpBroker broker, OpProjectPlanVersion planVersion,
        int maxOutlineLevel, boolean retrieveAllActivities) {
      OpQuery query = null;
      List<OpActivityIfc> result = new ArrayList<OpActivityIfc>();
      if (planVersion != null) {
         if (!retrieveAllActivities) {
            StringBuffer queryString = new StringBuffer("from OpActivityVersion as av left join fetch av.Activity as act where av.PlanVersion.id = :planVersionId ");
            queryString.append(" and av.OutlineLevel <= :maxLevel order by av.Sequence");
            query = broker.newQuery(queryString.toString());
            query.setLong("planVersionId", planVersion.getId());
            query.setInteger("maxLevel", maxOutlineLevel);
         }
         else {
            StringBuffer queryString = new StringBuffer("from OpActivityVersion as av left join fetch av.Activity as act where av.PlanVersion.id = :planVersionId  order by av.Sequence");
            query = broker.newQuery(queryString.toString());
            query.setLong("planVersionId", planVersion.getId());
         }
         result = broker.list(query);
      }
      return result.iterator();
   }

   /**
    * Adds info about the given activity to the effort data set (data_set).
    *
    * @param data_set           Effort data set
    * @param activity           Activity entity
    * @param resource_summaries Instance containing the resource info.
    */
   private static void addActivityToEffortDataSet(XComponent data_set, OpActivityIfc activity, HashMap resource_summaries) {
      // NL: Remaining is estimation of resource (effortToComplete); predicted = actual + remaining
      // (Therefore, deviation = predicted - base)
      double base = activity.getBaseEffort();
      double actual = activity.getActualEffort();
      // TODO: NL actually means effort-to-complete w/remaining (Add as separate, additional column?)
      double remaining = base - actual;
      double predicted = actual + activity.getOpenEffort();
      // TODO: Check algorithm for calculated predicted effort

      double deviation = predicted - base;
      XComponent data_row = new XComponent(XComponent.DATA_ROW);

      data_row.setOutlineLevel(activity.getOutlineLevel() < 0 ? 0 : activity.getOutlineLevel());
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
   private static void addResourceSummary(OpActivityIfc activity, Collection resource_summaries, XComponent data_set) {
      double deviation;
      XComponent data_row;
      XComponent data_cell;
      byte outline_level = (byte) ((activity.getOutlineLevel() < 0 ? 0 : activity.getOutlineLevel()) + 1);
      OpProjectResourceSummary resource_summary;
      SortedSet sortedResourceSummaries = new TreeSet(new Comparator() {
         public int compare(Object o1, Object o2) {
            OpProjectResourceSummary s1 = (OpProjectResourceSummary) o1;
            OpProjectResourceSummary s2 = (OpProjectResourceSummary) o2;
            int ret = s1.getResourceName().toLowerCase().compareTo(s2.getResourceName().toLowerCase());
            if (ret == 0) {
               ret = s1.getResourceName().compareTo(s2.getResourceName());
            }
            return ret;
         }
      });
      sortedResourceSummaries.addAll(resource_summaries);
      
      Iterator i = sortedResourceSummaries.iterator();
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
