/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityIfc;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpWorkPeriodIfc;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.project.util.Pair;
import onepoint.util.XCalendar;

/**
 * Dataset Factory for resource utilization module.
 *
 * @author mihai.costin
 */
public class OpResourceUtilizationDataSetFactory {

   private static final String SESSION_UTILIZATION_MAP = "UTILIZATION_MAP";

   private static OpResourceUtilizationDataSetFactory instance = new OpResourceUtilizationDataSetFactory();

   
   public static OpResourceUtilizationDataSetFactory getInstance() {
      return instance;
   }
   
   public static void register(OpResourceUtilizationDataSetFactory factory) {
      instance = factory;
   }
   
   /**
    * Class that contains the utilization info (for a resource/pool).
    */
   public static class Utilization {
      private Map<Date, Double> utilizationMap;
      private Date start;
      private Date end;
      private List<Double> values;
      private double available;
      private boolean dayMapFormat;

      public Utilization() {
         utilizationMap = new TreeMap();
         dayMapFormat = false;
      }

      public Utilization(Date start, Date end, List<Double> values) {
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

      protected void toDayMapFormat() {
         dayMapFormat = true;
         Date currentDay = new Date(start.getTime());
         int index = 0;
         while (index < values.size()) {
            utilizationMap.put(currentDay, values.get(index));
            index++;
            currentDay = new Date(currentDay.getTime() + OpProjectCalendar.MILLIS_PER_DAY);
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
    * @param session            Session used to acces the db.
    * @param dataSet            data set to calculate the utilization values for
    * @param poolLocator        ID for the pool that was expanded
    * @param projectProbability Project are filtered by probability.
    *                           Only values from project with probability >= projectsProbability will be taken into account
    */
   public int fillUtilizationValues(OpProjectSession session, XComponent dataSet, String poolLocator, 
         int projectProbability, Set<Long> projectPlanVersionIds) {

      invalidateUtilizations(session, dataSet, poolLocator);

      Map<String, Utilization> utilizations = getUtilizationMap(session, dataSet, projectProbability, projectPlanVersionIds);
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         if (!OpProjectConstants.DUMMY_ROW_ID.equals(dataRow.getStringValue())) {
            String locator = dataRow.getStringValue();
            Utilization utilization = utilizations.get(locator);
            if (utilization != null) {
               Date start = utilization.getStart();
               Date end = utilization.getEnd();
               ArrayList utilizationValues = (ArrayList) utilization.getListValues();

               //set the values on the data row
               ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_START_COLUMN_INDEX)).setDateValue(start);
               ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_END_COLUMN_INDEX)).setDateValue(end);
               ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_VALUES_COLUMN_INDEX)).setListValue(utilizationValues);

               XComponent descriptorCell = ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_DESCRIPTOR_COLUMN_INDEX));
               String rowDescriptor = descriptorCell.getStringValue();
               if (rowDescriptor.equals(OpProjectComponent.UTILIZATION_POOL_DESCRIPTOR)) {
                  XComponent availableCell = ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_AVAILABLE_COLUMN_INDEX));
                  availableCell.setDoubleValue(utilization.getAvailable());
               }
               
               addAdvancedUtilizationValues(utilization, dataRow, rowDescriptor);
            }
         }
      }
      return utilizations.size();
   }

   protected void addAdvancedUtilizationValues(Utilization utilization,
         XComponent dataRow, String rowDescriptor) {
   }

   /**
    * Calculates the utilizationMap for pools and sets the values in the given map (cache).
    *
    * @param session      Session to use for db access.
    * @param poolIds      The pools that should be included when calculating the utilization values.
    *                     If null, all pools in the db are included.
    * @param utilizations Map where the results will be added. key:locator - value:Map of utilization values.
    * @see #getUtilizationMap(onepoint.project.OpProjectSession,onepoint.express.XComponent,int)
    */
   private void fillPoolsUtilizationValues(OpProjectSession session, List poolIds, Map<String, Utilization> utilizations) {
      String queryString = "select pool from OpResourcePool as pool ";
      if (poolIds != null && !poolIds.isEmpty()) {
         queryString += "where pool.id in (:poolIds)";
      }

      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery(queryString);
         if (poolIds != null && !poolIds.isEmpty()) {
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
      }
      finally {
         broker.close();
      }
   }

   private void calculatePoolUtilizationValues(OpResourcePool pool, Map<String, Utilization> utilizations) {

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
      List<Double> utilizationValues = new ArrayList();
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
         date = new Date(date.getTime() + OpProjectCalendar.MILLIS_PER_DAY);
      }
      Utilization utilization = createUtilization(minStart, maxEnd, utilizationValues);
      
      adjustPoolUtilization(utilization, utilizations);
      
      utilization.setAvailable(available);
      utilizations.put(pool.locator(), utilization);
   }

   private void adjustPoolUtilization(Utilization utilization, Map<String, Utilization> utilizations) {
   }

   protected Utilization createUtilization(Date start, Date end, List<Double> values) {
      if (start == null && end == null && values == null) {
         return new Utilization();
      }
      return new Utilization(start, end, values);
   }

   private void fillResourcesUtilizationValues(OpProjectSession session,
         List<Long> resourceIds, Map<String, Utilization> utilizations, int projectProbability,
         Set<Long> projectPlanVersionIds) {
      if (projectPlanVersionIds == null || projectPlanVersionIds.isEmpty()) {
         return;
      }
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

      Set<Byte> actTypes = new HashSet<Byte>();
      actTypes.add(new Byte(OpActivity.STANDARD));
      actTypes.add(new Byte(OpActivity.TASK));

      OpBroker broker = session.newBroker();
      try {
         Map<Long, Date> minStartDates = new HashMap<Long, Date>();
         Map<Long, Date> maxFinishDates = new HashMap<Long, Date>();
         Date startBoundary = null;
         Date finishBoundary = null;
         
         StringBuffer queryBuffer = new StringBuffer("" +
         		"select assignment.Resource.id, min(workPeriod.Start), max(workPeriod.Start) ");
         queryBuffer.append("" +
         		"from OpAssignmentVersion as assignment " +
         		"inner join assignment.ActivityVersion as activity " +
               "inner join activity.PlanVersion planVersion " +
               "inner join activity.WorkPeriodVersions workPeriod " +
         		"inner join planVersion.ProjectPlan as plan " +
         		"inner join plan.ProjectNode as projectNode ");
         queryBuffer.append("" +
         		"where projectNode.Archived=false " +
         		"and projectNode.Probability >= :probability ");
         if (resourceIds != null && !resourceIds.isEmpty()) {
            queryBuffer.append("and assignment.Resource.id in (:resourceIds) ");
         }
         if (projectPlanVersionIds != null && !projectPlanVersionIds.isEmpty()) {
            queryBuffer.append("and planVersion.id in (:projectPlanVersionIds) ");            
         }
         queryBuffer.append("group by assignment.Resource.id");
         OpQuery query = broker.newQuery(queryBuffer.toString());
         if (resourceIds != null && !resourceIds.isEmpty()) {
            query.setCollection("resourceIds", resourceIds);
         }
         if (projectPlanVersionIds != null && !projectPlanVersionIds.isEmpty()) {
            query.setCollection("projectPlanVersionIds", projectPlanVersionIds);
         }
         query.setInteger("probability", projectProbability);
         Iterator result = broker.iterate(query);

         while (result.hasNext()) {
            record = (Object[]) result.next();
            resourceId = (Long) record[0];
            minStart = (Date) record[1];
            maxFinish = new Date(((Date) record[2]).getTime() + 31 * XCalendar.MILLIS_PER_DAY);
            minStartDates.put(resourceId, minStart);
            maxFinishDates.put(resourceId, maxFinish);
            
            startBoundary = startBoundary == null || startBoundary.after(minStart) ? minStart : startBoundary;
            finishBoundary = finishBoundary == null || finishBoundary.before(maxFinish) ? maxFinish : finishBoundary;
         }
         
         Pair<Date, Date> newBoundaries = adjustMinMaxDates(session, broker,
               resourceIds, startBoundary, finishBoundary, minStartDates,
               maxFinishDates);
         startBoundary = newBoundaries.getFirst();
         finishBoundary = newBoundaries.getSecond();

         queryBuffer = new StringBuffer("" +
         		"select assignment.Resource.id, assignment.Assigned, activity, workPeriod ");
         queryBuffer.append("" +
         		"from OpAssignmentVersion as assignment " +
         		"inner join assignment.ActivityVersion as activity " +
         		"inner join activity.WorkPeriodVersions as workPeriod " +
               "inner join activity.PlanVersion planVersion " +
               "inner join planVersion.ProjectPlan as plan " +
               "inner join plan.ProjectNode as projectNode ");
         queryBuffer.append("" +
         		"where activity.Type in (:activityTypes) " +
         		"and activity.Template = false " +
         		"and projectNode.Archived=false " +
         		"and projectNode.Probability >= :probability ");
         if (resourceIds != null && !resourceIds.isEmpty()) {
            queryBuffer.append("and assignment.Resource.id in (:projectPlanVersionIds) ");
         }
         if (projectPlanVersionIds != null && !projectPlanVersionIds.isEmpty()) {
            queryBuffer.append("and planVersion.id in (:projectPlanVersionIds) ");
         }
         queryBuffer.append("order by assignment.Resource.id");
         query = broker.newQuery(queryBuffer.toString());
         if (resourceIds != null && !resourceIds.isEmpty()) {
            query.setCollection("resourceIds", resourceIds);
         }
         if (projectPlanVersionIds != null && !projectPlanVersionIds.isEmpty()) {
            query.setCollection("projectPlanVersionIds", projectPlanVersionIds);
         }
         query.setInteger("probability", projectProbability);
         query.setCollection("activityTypes", actTypes);
         //<FIXME author="Horia Chiorean" description="Changed to broker.iterate when this works">
         result = broker.list(query).iterator();
         //<FIXME>

         Calendar cal = OpProjectCalendarFactory.getInstance().getDefaultCalendar(session).cloneCalendarInstance();
         
         resourceId = null;
         Utilization utilization = null;
         while (result.hasNext()) {
            record = (Object[]) result.next();
            // Work phases are grouped by resource: Check for next resource ID
            if ((resourceId == null) || !resourceId.equals(record[0])) {
               resourceId = (Long) record[0];
               minStart = minStartDates.get(resourceId);
               maxFinish = maxFinishDates.get(resourceId);
               // Initialize utilization values
               valueCount = (int) ((maxFinish.getTime() + OpProjectCalendar.MILLIS_PER_DAY - minStart.getTime()) / OpProjectCalendar.MILLIS_PER_DAY);
               values = new ArrayList<Double>(valueCount);
               for (i = 0; i < valueCount; i++) {
                  values.add(zero);
               }
               utilization = createUtilization(minStart, maxFinish, values);
               OpResource resource = broker.getObject(OpResource.class, resourceId.longValue());
               
               utilization.setAvailable(resource.getAvailable());
               utilizations.put(resource.locator(), utilization);
            }

            OpActivityIfc activity = (OpActivityIfc) record[2];
            OpWorkPeriodIfc workPeriod = (OpWorkPeriodIfc) record[3];
            
            // Set utilization values
            Date activityStart = activity.getStart();
            Date activityFinish = activity.getFinish();
            Date workPeriodStart = workPeriod.getStart();
            cal.setTimeInMillis(workPeriodStart.getTime());
            cal.add(Calendar.MONTH, 1);
            Date workPeriodFinish = new Date(cal.getTimeInMillis() - XCalendar.MILLIS_PER_DAY);

            //start = maxFinishDates (activityStart, workPeriodStart)
            start = activityStart.before(workPeriodStart) ? workPeriodStart : activityStart;
            startIndex = (int) ((start.getTime() - minStart.getTime()) / OpProjectCalendar.MILLIS_PER_DAY);

            //finish = minStartDates (activityFinish, workPeriodFinish)
            finish = activityFinish != null && activityFinish.before(workPeriodFinish) ? activityFinish : workPeriodFinish;
            // finish = finish.after(utilization.getEnd()) ? utilization.getEnd() : finish;
            finishIndex = (int) ((finish.getTime() - minStart.getTime()) / OpProjectCalendar.MILLIS_PER_DAY);

            double assignmentValue = ((Double) record[1]);
            double utilizationValue = getUtilizationValueAccordingToProject(assignmentValue, activity.getProjectPlan().getProjectNode());

            for (i = startIndex; i <= finishIndex; i++) {
               if (isWorkDay(workPeriod, minStart, i)) {
                  utilization.addUtilization(i, utilizationValue);
               }
            }
         }
         
         adjustUtilizationValues(session, broker, utilizations, resourceIds, startBoundary, finishBoundary, minStartDates, maxFinishDates);
      }
      finally {
         broker.close();
      }
   }

   protected void adjustUtilizationValues(OpProjectSession session,
         OpBroker broker, Map<String, Utilization> utilizations, List<Long> resourceIds,
         Date startBoundary, Date finishBoundary, Map<Long, Date> minStartDates,
         Map<Long, Date> maxFinishDates) {
   }

   protected Pair<Date, Date> adjustMinMaxDates(OpProjectSession session, OpBroker broker,
         List<Long> resourceIds, Date startBoundary, Date finishBoundary, Map<Long, Date> minStartDates,
         Map<Long, Date> maxFinishDates) {
      return new Pair<Date, Date>(startBoundary, finishBoundary);
   }

   /**
    * Returns the utilization value for a resource assignment, taking into account some business
    * rules from the project on which the assignment exists.
    * Rule1: Archived projects are not taken into account
    * Rule2: The value of the assignment is multiplied (%-wise) with the probability of the project.
    *
    * @param assignmentValue a <code>double</code> the value of an <code>OpAssignment</code>.
    * @param projectNode     a <code>OpProjectNode</code> the project on which the assignment it
    * @return a <code>double</code> the value for the assignment from the point-of-view of
    *         the resource utilization chart.
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
   private static boolean isWorkDay(OpWorkPeriodIfc workPeriod, Date minStart, int dateIndex) {
      Date periodStart = workPeriod.getStart();
      Date checkedDate = new Date(minStart.getTime() + dateIndex * OpProjectCalendar.MILLIS_PER_DAY);
      int workPeriodDateIndex = (int) ((checkedDate.getTime() - periodStart.getTime()) / OpProjectCalendar.MILLIS_PER_DAY);
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
            try {
               invalidatePool(broker, utilizations, poolLocator);
            }
            finally {
               broker.close();
            }
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
    * @param session            Session to be used for bd access and for SESSION_UTILIZATION_MAP cache.
    * @param dataSet
    * @param projectProbability
    * @param projectPlanVersionIds 
    * @return Map of utilization values. key:locator -> value:ValuesMap.
    *         The ValuesMap contains for each resource the start, end and utilization value as a List.
    *         the keys used are UTILIZATION_START_KEY, UTILIZATION_END_KEY and UTILIZATION_VALUES_KEY.
    */
   public Map<String, Utilization> getUtilizationMap(OpProjectSession session, XComponent dataSet, int projectProbability, Set<Long> projectPlanVersionIds) {
      Map<String, Utilization> utilizations = (Map<String, Utilization>) session.getVariable(SESSION_UTILIZATION_MAP);
      if (utilizations == null) {
         //calculate utilization for all resources
         utilizations = new HashMap<String, Utilization>();
         fillResourcesUtilizationValues(session, null, utilizations, projectProbability, projectPlanVersionIds);

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
         fillResourcesUtilizationValues(session, resourceIds, utilizations, projectProbability, projectPlanVersionIds);

         //calculate utilization for all invalidated resource pools.
         fillPoolsUtilizationValues(session, pollIds, utilizations);
      }
      return utilizations;
   }

   public static void setUtilizationMap(OpProjectSession session, Map utilizationMap) {
      session.setVariable(SESSION_UTILIZATION_MAP, utilizationMap);
   }

   public void enhanceResourceDetailsMap(OpProjectSession session, OpBroker broker,
         OpResource resource, Map details, long startTime, long finishTime) {
   }

   public XComponent createUtilizationDetailsDataRow(String name1,
         String name2, int sequence, Date start, Date finish, Double effort,
         Double assigned, Integer probability) {
      XComponent dataRow;
      XComponent dataCell;
      dataRow = new XComponent(XComponent.DATA_ROW);

      // project name 0
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(name1 != null && name1.length() > 0 ? name1 : " - ");
      dataRow.addChild(dataCell);

      // activity name 1
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(name2 != null && name2.length() > 0 ? name2 : " - ");
      dataRow.addChild(dataCell);

      // activity sequence 2
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(sequence);
      dataRow.addChild(dataCell);

      // activity start date 3
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDateValue(start);
      dataRow.addChild(dataCell);

      // activity finish date 4
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDateValue(finish);
      dataRow.addChild(dataCell);

      // assignment base effort 5
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(effort);
      dataRow.addChild(dataCell);

      // assignment % assigned 6
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(assigned);
      dataRow.addChild(dataCell);

      // project probability % 7
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(probability != null ? new Double(probability.intValue()) : null);
      dataRow.addChild(dataCell);
      return dataRow;
   }

   public void enhancePoolDetailsMap(OpProjectSession session, OpBroker broker,
         Set<OpResource> resources, SortedMap<String, XComponent> detailsRows, Date startDate, Date finishDate) {
   }
   
   public static final int POOL_UTILIZATION_RESOURCE_NAME_COLUMN = 0;
   public static final int POOL_UTILIZATION_EFFORT_COLUMN = 1;
   public static final int POOL_UTILIZATION_ASSIGNED_COLUMN = 2;

   public XComponent newPoolUtilizationRow(OpResource resource, double effort, double assigned) {
      XComponent row = new XComponent(XComponent.DATA_ROW);
      row.setStringValue(XValidator.choice(resource.locator(), resource.getName()));

      XComponent cell;
      //name
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setStringValue(resource.getName());
      row.addChild(cell);

      //effort
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setDoubleValue(effort);
      row.addChild(cell);

      //sum(% assigned)
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setDoubleValue(assigned);
      row.addChild(cell);
      return row;
   }

}
