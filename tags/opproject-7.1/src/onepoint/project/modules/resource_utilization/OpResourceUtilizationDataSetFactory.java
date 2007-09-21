/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpWorkPeriod;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.util.OpProjectConstants;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

/**
 * Dataset Factory for resource utilization module.
 *
 * @author mihai.costin
 */
public final class OpResourceUtilizationDataSetFactory {

   private static final String SESSION_UTILIZATION_MAP = "UTILIZATION_MAP";

   /**
    * Class that contains the utilization info (for a resource/pool).
    */
   private static class Utilization {
      private Map utilizationMap;
      private Date start;
      private Date end;
      private List values;
      private double available;
      private boolean dayMapFormat;

      public Utilization() {
         utilizationMap = new TreeMap();
         dayMapFormat = false;
      }

      public Utilization(Date start, Date end, List values) {
         this();
         this.start = start;
         this.end = end;
         this.values = values;
      }

      public List getListValues() {
         return values;
      }

      public Date getStart() {
         return start;
      }

      public Date getEnd() {
         return end;
      }

      public void addUtilization(int index, double value) {
         values.set(index, new Double(((Double) values.get(index)).doubleValue() + value));
         dayMapFormat = false;
      }

      private void toDayMapFormat() {
         dayMapFormat = true;
         Date currentDay = new Date(start.getTime());
         int index = 0;
         while (index < values.size()) {
            utilizationMap.put(currentDay, values.get(index));
            index++;
            currentDay = new Date(currentDay.getTime() + XCalendar.MILLIS_PER_DAY);
         }
      }

      public double getUtilizationForDay(Date day) {
         if (!dayMapFormat) {
            toDayMapFormat();
         }
         Double utilization = (Double) utilizationMap.get(day);
         if (utilization == null) {
            return 0;
         }
         else {
            return utilization.doubleValue();
         }
      }


      public double getAvailable() {
         return available;
      }

      public void setAvailable(double available) {
         this.available = available;
      }
   }


   /**
    * Calculates the utilization values for the rows in the given data set (both resources and pools) and sets the
    * values on the corresponding data cells.
    *
    * @param session     Session used to acces the db.
    * @param dataSet     data set to calculate the utilization values for
    * @param poolLocator ID for the pool that was expanded
    */
   public static void fillUtilizationValues(OpProjectSession session, XComponent dataSet, String poolLocator) {

      invalidateUtilizations(session, dataSet, poolLocator);

      Map utilizations = getUtilizationMap(session, dataSet);
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         if (!OpProjectConstants.DUMMY_ROW_ID.equals(dataRow.getStringValue())) {
            String locator = dataRow.getStringValue();
            Utilization utilization = (Utilization) utilizations.get(locator);
            if (utilization != null) {
               Date start = utilization.getStart();
               Date end = utilization.getEnd();
               ArrayList utilizationValues = (ArrayList) utilization.getListValues();

               //set the values on the data row
               ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_START_COLUMN_INDEX)).setDateValue(start);
               ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_END_COLUMN_INDEX)).setDateValue(end);
               ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_VALUES_COLUMN_INDEX)).setListValue(utilizationValues);

               XComponent descriptorCell = ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_DESCRIPTOR_COLUMN_INDEX));
               if (descriptorCell.getValue().equals(OpProjectComponent.UTILIZATION_POOL_DESCRIPTOR)) {
                  XComponent availableCell = ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_AVAILABLE_COLUMN_INDEX));
                  availableCell.setDoubleValue(utilization.getAvailable());
               }
            }
         }
      }
   }

   /**
    * Calculates the utilizationMap for pools and sets the values in the given map (cache).
    *
    * @param session      Session to use for db access.
    * @param poolIds      The pools that should be included when calculating the utilization values.
    *                     If null, all pools in the db are included.
    * @param utilizations Map where the results will be added. key:locator - value:Map of utilization values.
    * @see #getUtilizationMap(onepoint.project.OpProjectSession,onepoint.express.XComponent)
    */
   private static void fillPoolsUtilizationValues(OpProjectSession session, List poolIds, Map utilizations) {
      String queryString = "select pool from OpResourcePool as pool";
      if (poolIds != null && !poolIds.isEmpty()) {
         queryString += "where pool.ID in (:poolIds)";
      }

      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery(queryString);
      if (poolIds != null) {
         query.setCollection("poolIds", poolIds);
      }

      Iterator result = broker.iterate(query);
      while (result.hasNext()) {
         OpResourcePool pool = (OpResourcePool) result.next();
         String poolLocator = pool.locator();
         if (utilizations.get(poolLocator) == null) {
            calculatePoolUtilizationValues(pool, utilizations);
         }
      }
      broker.close();
   }

   private static void calculatePoolUtilizationValues(OpResourcePool pool, Map utilizations) {

      //calculate for all sub-pools
      Set subPools = pool.getSubPools();
      for (Iterator iterator = subPools.iterator(); iterator.hasNext();) {
         OpResourcePool subPool = (OpResourcePool) iterator.next();
         calculatePoolUtilizationValues(subPool, utilizations);
      }

      //start/end
      Date minStart = null;
      Date maxEnd = null;
      List subEntitiesLocators = new ArrayList();

      for (Iterator iterator = subPools.iterator(); iterator.hasNext();) {
         OpResourcePool subPool = (OpResourcePool) iterator.next();
         subEntitiesLocators.add(subPool.locator());
      }
      Set resources = pool.getResources();
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         OpResource resource = (OpResource) iterator.next();
         subEntitiesLocators.add(resource.locator());
      }

      //if pool is empty
      if (subEntitiesLocators.size() == 0) {
         return;
      }

      double available = 0;
      for (Iterator iterator = subEntitiesLocators.iterator(); iterator.hasNext();) {
         String locator = (String) iterator.next();
         Utilization subValue = (Utilization) utilizations.get(locator);
         if (subValue != null) {
            Date start = subValue.getStart();
            if (minStart == null || start.before(minStart)) {
               minStart = start;
            }
            Date end = subValue.getEnd();
            if (maxEnd == null || maxEnd.before(end)) {
               maxEnd = end;
            }
            //add available
            available += subValue.getAvailable();
         }
      }
      if (minStart == null || maxEnd == null) {
         return;
      }

      //calculate for this pool (sum of subPools and resources)
      Date date = new Date(minStart.getTime());
      List utilizationValues = new ArrayList();
      while (!maxEnd.before(date)) {
         double utilizationValue = 0;
         for (Iterator iterator = subEntitiesLocators.iterator(); iterator.hasNext();) {
            String locator = (String) iterator.next();
            Utilization subUtilization = (Utilization) utilizations.get(locator);
            if (subUtilization != null) {
               utilizationValue += subUtilization.getUtilizationForDay(date);
            }
         }
         utilizationValues.add(new Double(utilizationValue));
         date = new Date(date.getTime() + XCalendar.MILLIS_PER_DAY);
      }
      Utilization utilization = new Utilization(minStart, maxEnd, utilizationValues);
      utilization.setAvailable(available);
      utilizations.put(pool.locator(), utilization);
   }


   private static void fillResourcesUtilizationValues(OpProjectSession session, List resourceIds, Map utilizations) {

      Long resourceId;
      int i;
      Date minStart = null;
      Date maxFinish;
      Object[] record;
      int valueCount;
      List<Double> values;
      Double zero = new Double(0);
      Date start;
      Date finish;
      int startIndex;
      int finishIndex;

      OpBroker broker = session.newBroker();
      Map<Long, Date> minStartDates = new HashMap<Long, Date>();
      Map<Long, Date> maxFinishDates = new HashMap<Long, Date>();
      StringBuffer queryBuffer = new StringBuffer("select assignment.Resource.ID, min(activity.Start), max(activity.Finish) ");
      queryBuffer.append("from OpAssignment as assignment inner join assignment.Activity as activity  inner join activity.ProjectPlan projectPlan inner join projectPlan.ProjectNode projectNode ");
      queryBuffer.append("where activity.Deleted = false and projectNode.Archived=false ");
      if (resourceIds != null && !resourceIds.isEmpty()) {
         queryBuffer.append("and assignment.Resource.ID in (:resourceIds) ");
      }
      queryBuffer.append("group by assignment.Resource.ID");
      OpQuery query = broker.newQuery(queryBuffer.toString());
      if (resourceIds != null && !resourceIds.isEmpty()) {
         query.setCollection("resourceIds", resourceIds);
      }
      Iterator result = broker.iterate(query);

      while (result.hasNext()) {
         record = (Object[]) result.next();
         resourceId = (Long) record[0];
         minStart = (Date) record[1];
         maxFinish = (Date) record[2];
         minStartDates.put(resourceId, minStart);
         maxFinishDates.put(resourceId, maxFinish);
      }

      queryBuffer = new StringBuffer("select assignment.Resource.ID, assignment.Assigned, activity, workPeriod ");
      queryBuffer.append("from OpAssignment as assignment inner join assignment.Activity as activity inner join activity.WorkPeriods as workPeriod inner join activity.ProjectPlan projectPlan inner join projectPlan.ProjectNode projectNode ");
      queryBuffer.append("where activity.Deleted = false and activity.Type = :activityType and activity.Template = false and projectNode.Archived=false ");
      if (resourceIds != null && !resourceIds.isEmpty()) {
         queryBuffer.append("and assignment.Resource.ID in (:resourceIds) ");
      }
      queryBuffer.append("order by assignment.Resource.ID");
      query = broker.newQuery(queryBuffer.toString());
      if (resourceIds != null && !resourceIds.isEmpty()) {
         query.setCollection("resourceIds", resourceIds);
      }
      query.setByte("activityType", OpActivity.STANDARD);
      //<FIXME author="Horia Chiorean" description="Changed to broker.iterate when this works">
      result = broker.list(query).iterator();
      //<FIXME>

      resourceId = null;
      Utilization utilization = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         // Work phases are grouped by resource: Check for next resource ID
         if ((resourceId == null) || !resourceId.equals(record[0])) {
            resourceId = (Long) record[0];
            minStart = (Date) minStartDates.get(resourceId);
            maxFinish = (Date) maxFinishDates.get(resourceId);
            // Initialize utilization values
            valueCount = (int) ((maxFinish.getTime() + XCalendar.MILLIS_PER_DAY - minStart.getTime()) / XCalendar.MILLIS_PER_DAY);
            values = new ArrayList<Double>(valueCount);
            for (i = 0; i < valueCount; i++) {
               values.add(zero);
            }
            utilization = new Utilization(minStart, maxFinish, values);
            OpResource resource = (OpResource) broker.getObject(OpResource.class, resourceId.longValue());
            utilization.setAvailable(resource.getAvailable());
            utilizations.put(resource.locator(), utilization);
         }

         OpActivity activity = (OpActivity) record[2];
         OpWorkPeriod workPeriod = (OpWorkPeriod) record[3];

         // Set utilization values
         Date activityStart = activity.getStart();
         Date activityFinish = activity.getFinish();
         Date workPeriodStart = workPeriod.getStart();
         Date workPeriodFinish = new Date(workPeriodStart.getTime() + (OpWorkPeriod.PERIOD_LENGTH - 1) * XCalendar.MILLIS_PER_DAY);

         //start = maxFinishDates (activityStart, workPeriodStart)
         start = activityStart.before(workPeriodStart) ? workPeriodStart : activityStart;
         startIndex = (int) ((start.getTime() - minStart.getTime()) / XCalendar.MILLIS_PER_DAY);

         //finish = minStartDates (activityFinish, workPeriodFinish)
         finish = activityFinish.before(workPeriodFinish) ? activityFinish : workPeriodFinish;
         finishIndex = (int) ((finish.getTime() - minStart.getTime()) / XCalendar.MILLIS_PER_DAY);

         double assignmentValue = ((Double) record[1]);
         double utilizationValue = getUtilizationValueAccordingToProject(assignmentValue, activity.getProjectPlan().getProjectNode());

         for (i = startIndex; i <= finishIndex; i++) {
            if (isWorkDay(workPeriod, minStart, i)) {
               utilization.addUtilization(i,  utilizationValue);
            }
         }
      }

      broker.close();
   }

   /**
    * Returns the utilization value for a resource assignment, taking into account some business
    * rules from the project on which the assignment exists.
    * Rule1: Archived projects are not taken into account
    * Rule2: The value of the assignment is multiplied (%-wise) with the probability of the project.
    * @param assignmentValue a <code>double</code> the value of an <code>OpAssignment</code>.
    * @param projectNode a <code>OpProjectNode</code> the project on which the assignment it
    * @return a <code>double</code> the value for the assignment from the point-of-view of
    * the resource utilization chart.
    */
   private static double getUtilizationValueAccordingToProject(double assignmentValue, OpProjectNode projectNode) {
      int projectProbability = projectNode.getProbability();
      return (projectProbability * assignmentValue) / 100;
   }

   /**
    * Checks if a given day is a working day.
    *
    * @param workPeriod Work period containing the information for the verified day.
    * @param minStart   start date for the indexed day
    * @param dateIndex  day index from minStart date
    * @return true if the checked day is a working day. False otherwise.
    */
   private static boolean isWorkDay(OpWorkPeriod workPeriod, Date minStart, int dateIndex) {
      Date periodStart = workPeriod.getStart();
      Date checkedDate = new Date(minStart.getTime() + dateIndex * XCalendar.MILLIS_PER_DAY);
      int workPeriodDateIndex = (int) ((checkedDate.getTime() - periodStart.getTime()) / XCalendar.MILLIS_PER_DAY);
      long mask = (1L << workPeriodDateIndex);
      return (workPeriod.getWorkingDays() & mask) == mask;
   }

   /**
    * Invalidates (removes them from cache) the utilization for the given pool and also for the parent pools.
    *
    * @param session
    * @param dataSet
    * @param poolLocator
    */
   private static void invalidateUtilizations(OpProjectSession session, XComponent dataSet, String poolLocator) {

      if (poolLocator == null) {
         //invalidate all
         session.setVariable(SESSION_UTILIZATION_MAP, null);
      }
      else {
         Map utilizations = (Map) session.getVariable(SESSION_UTILIZATION_MAP);
         if (utilizations != null) {

            XComponent poolRow = null;
            for (int i = 0; i < dataSet.getChildCount(); i++) {
               XComponent row = (XComponent) dataSet.getChild(i);
               String locatorString = row.getStringValue();
               if (poolLocator.equals(locatorString)) {
                  poolRow = row;
               }
            }

            if (poolRow == null) {
               session.setVariable(SESSION_UTILIZATION_MAP, null);
               return;
            }

            //invalidate all super-rows for the given id
            List parentRows = poolRow.getSuperRows();
            for (int i = 0; i < parentRows.size(); i++) {
               XComponent row = (XComponent) parentRows.get(i);
               String locator = row.getStringValue();
               utilizations.remove(locator);
            }

            //invalidate pool and resources
            OpBroker broker = session.newBroker();
            invalidatePool(broker, utilizations, poolLocator);
            broker.close();
         }
      }
   }

   private static void invalidatePool(OpBroker broker, Map utilizations, String poolLocator) {
      //invalidate the pool
      utilizations.remove(poolLocator);

      //invalidate all the resources belonging to the given pool
      OpResourcePool pool = (OpResourcePool) broker.getObject(poolLocator);
      Set resources = pool.getResources();
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         OpResource resource = (OpResource) iterator.next();
         utilizations.remove(resource.locator());
      }

      //invalidate sub-pools
      Set subPools = pool.getSubPools();
      for (Iterator iterator = subPools.iterator(); iterator.hasNext();) {
         OpResourcePool subPool = (OpResourcePool) iterator.next();
         invalidatePool(broker, utilizations, subPool.locator());
      }
   }

   /**
    * @param session Session to be used for bd access and for SESSION_UTILIZATION_MAP cache.
    * @param dataSet
    * @return Map of utilization values. key:locator -> value:ValuesMap.
    *         The ValuesMap contains for each resource the start, end and utilization value as a List.
    *         the keys used are UTILIZATION_START_KEY, UTILIZATION_END_KEY and UTILIZATION_VALUES_KEY.
    */
   public static Map getUtilizationMap(OpProjectSession session, XComponent dataSet) {
      Map utilizations = (Map) session.getVariable(SESSION_UTILIZATION_MAP);
      if (utilizations == null) {
         //calculate utilization for all resources
         utilizations = new HashMap();
         fillResourcesUtilizationValues(session, null, utilizations);

         //calculate utilization for all resource pools
         fillPoolsUtilizationValues(session, null, utilizations);
      }
      else {
         //check for invalidated resources in the given data set
         List resourceIds = new ArrayList();
         List pollIds = new ArrayList();
         for (int i = 0; i < dataSet.getChildCount(); i++) {
            XComponent row = (XComponent) dataSet.getChild(i);
            String locatorString = row.getStringValue();
            if (utilizations.get(locatorString) == null) {
               OpLocator locator = OpLocator.parseLocator(locatorString);
               if (locator != null) {
                  if (locator.getPrototype().getInstanceClass().equals(OpResource.class)) {
                     resourceIds.add(new Long(locator.getID()));
                  }
                  else {
                     pollIds.add(new Long(locator.getID()));
                  }
               }
            }
         }
         fillResourcesUtilizationValues(session, resourceIds, utilizations);

         //calculate utilization for all invalidated resource pools.
         fillPoolsUtilizationValues(session, pollIds, utilizations);
      }

      return utilizations;
   }

   public static void setUtilizationMap(OpProjectSession session, Map utilizationMap) {
      session.setVariable(SESSION_UTILIZATION_MAP, utilizationMap);
   }


}
