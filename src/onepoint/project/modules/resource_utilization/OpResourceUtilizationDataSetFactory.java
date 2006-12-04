/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpWorkPeriod;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.util.OpProjectConstants;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Dataset Factory for resource utilization module.
 *
 * @author mihai.costin
 */
public final class OpResourceUtilizationDataSetFactory {

   /**
    * Calculates the utilization values for the rows in the given data set and sets the values on the
    * corresponding data cells.
    *
    * @param dataSet data set to calculate the utilization values for
    * @param session Session used to acces the db.
    */
   public static void calculateUtilizationValues(XComponent dataSet, OpProjectSession session) {

      XComponent dataRow;
      HashSet resourceIds = new HashSet();
      HashMap resourceRowMap = new HashMap();
      dataRow = null;
      Long resourceId = null;
      int i = 0;
      for (i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if (dataRow.getStringValue() != OpProjectConstants.DUMMY_ROW_ID) {
            dataRow.setVisible((dataRow.getOutlineLevel() < 2));
            resourceId = (Long) ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_COLUMNS)).getValue();
            if (resourceId != null) {
               resourceIds.add(resourceId);
               resourceRowMap.put(resourceId, dataRow);
            }
         }
      }

      // If there are no resources return w/o calculating utilization
      if (resourceIds.size() == 0) {
         return;
      }

      Date minStart = null;
      Date maxFinish = null;
      Object[] record = null;
      resourceId = null;
      dataRow = null;
      int valueCount = 0;
      ArrayList values = null;
      Double zero = new Double(0);
      Date start = null;
      Date finish = null;
      int startIndex = 0;
      int finishIndex = 0;
      Double value = null;

      OpBroker broker = session.newBroker();
      HashMap min = new HashMap();
      HashMap max = new HashMap();
      StringBuffer queryBuffer = new StringBuffer("select assignment.Resource.ID, min(activity.Start), max(activity.Finish) ");
      queryBuffer.append("from OpAssignment as assignment inner join assignment.Activity as activity ");
      queryBuffer.append("where activity.Deleted = false and assignment.Resource.ID in (:resourceIds) group by assignment.Resource.ID");
      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setCollection("resourceIds", resourceIds);
      Iterator result = broker.iterate(query);

      while (result.hasNext()) {
         record = (Object[]) result.next();
         resourceId = (Long) record[0];
         minStart = (Date) record[1];
         maxFinish = (Date) record[2];
         min.put(resourceId, minStart);
         max.put(resourceId, maxFinish);
      }

      queryBuffer = new StringBuffer("select assignment.Resource.ID, assignment.Assigned, activity, workPeriod ");
      queryBuffer.append("from OpAssignment as assignment inner join assignment.Activity as activity inner join activity.WorkPeriods as workPeriod ");
      queryBuffer.append("where activity.Deleted = false and activity.Type = :activityType and activity.Template = false and assignment.Resource.ID in (:resourceIds) order by assignment.Resource.ID");
      query = broker.newQuery(queryBuffer.toString());
      query.setCollection("resourceIds", resourceIds);
      query.setByte("activityType", OpActivity.STANDARD);
      result = broker.iterate(query);

      resourceId = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         // Work phases are grouped by resource: Check for next resource ID
         if ((resourceId == null) || !resourceId.equals(record[0])) {
            resourceId = (Long) record[0];
            dataRow = (XComponent) resourceRowMap.get(resourceId);
            minStart = (Date) min.get(resourceId);
            ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_START_COLUMN_INDEX)).setDateValue(minStart);
            maxFinish = (Date) max.get(resourceId);
            ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_END_COLUMN_INDEX)).setDateValue(maxFinish);
            // Initialize utilization values
            valueCount = (int) ((maxFinish.getTime() + XCalendar.MILLIS_PER_DAY - minStart.getTime()) / XCalendar.MILLIS_PER_DAY);
            values = new ArrayList(valueCount);
            for (i = 0; i < valueCount; i++) {
               values.add(zero);
            }
            ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_VALUES_COLUMN_INDEX)).setListValue(values);
         }

         OpActivity activity = (OpActivity) record[2];
         OpWorkPeriod workPeriod = (OpWorkPeriod) record[3];

         // Set utilization values
         Date activityStart = activity.getStart();
         Date activityFinish = activity.getFinish();
         Date workPeriodStart = workPeriod.getStart();
         Date workPeriodFinish = new Date(workPeriodStart.getTime() + (OpWorkPeriod.PERIOD_LENGTH - 1) * XCalendar.MILLIS_PER_DAY);

         //start = max (activityStart, workPeriodStart)
         start = activityStart.before(workPeriodStart) ? workPeriodStart : activityStart;
         startIndex = (int) ((start.getTime() - minStart.getTime()) / XCalendar.MILLIS_PER_DAY);

         //finish = min (activityFinish, workPeriodFinish)
         finish = activityFinish.before(workPeriodFinish) ? activityFinish : workPeriodFinish;
         finishIndex = (int) ((finish.getTime() - minStart.getTime()) / XCalendar.MILLIS_PER_DAY);

         values = ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_VALUES_COLUMN_INDEX)).getListValue();
         for (i = startIndex; i <= finishIndex; i++) {
            value = (Double) (values.get(i));
            if (isWorkDay(workPeriod, minStart, i)) {
               values.set(i, new Double((value.doubleValue() + ((Double) record[1]).doubleValue())));
            }
         }
         ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_VALUES_COLUMN_INDEX)).setListValue(values);
      }

      broker.close();
   }

   /**
    * Checks if a given day is a working day.
    *
    * @param workPeriod
    * @param minStart
    * @param dateIndex
    * @return
    */
   private static boolean isWorkDay(OpWorkPeriod workPeriod, Date minStart, int dateIndex) {
      Date periodStart = workPeriod.getStart();
      Date checkedDate = new Date(minStart.getTime() + dateIndex * XCalendar.MILLIS_PER_DAY);
      int workPeriodDateIndex = (int) ((checkedDate.getTime() - periodStart.getTime()) / XCalendar.MILLIS_PER_DAY);
      long mask = (1L << workPeriodDateIndex);
      return (workPeriod.getWorkingDays() & mask) == mask;
   }


}
