/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization.forms;

import onepoint.express.XComponent;
import onepoint.express.XStyle;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpWorkPeriod;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


public class OpResourceUtilizationFormProvider implements XFormProvider {

   // TODO: Check if it can be 100% correct w/start and end-based data, or if time-phased approach is necessary
   // TODO: Try to optimize via database queries and joins (assignments and activities)

   public final static String UTILIZATION_DATA_SET = "UtilizationDataSet";
   public final static String UTILIZATION_LEGEND_DATA_SET = "UtilizationResourceColorSet";

   public final static String ALL_POOLS = "from " + OpResourcePool.RESOURCE_POOL;
   public final static String RESOURCES_OUTSIDE_POOL = "select resource from OpResource as resource where resource.Pool.ID = null";
   public final static String ALL_ASSIGNMENTS = "from " + OpAssignment.ASSIGNMENT;

   public final static int POOL_ICON_INDEX = 0;
   public final static int RESOURCE_ICON_INDEX = 1;

   public final static String POOL_DESCRIPTOR = OpProjectComponent.UTILIZATION_POOL_DESCRIPTOR;
   public final static String RESOURCE_DESCRIPTOR = OpProjectComponent.UTILIZATION_RESOURCE_DESCRIPTOR;
   private final static String RESOURCE_MAP = "resource_utilization.overview";
   private final static String HIGHLY_UNDERUSED = "HighlyUnderused";
   private final static String UNDERUSED = "Underused";
   private final static String NORMALUSE = "Normalused";
   private final static String OVERUSED = "Overused";
   private final static String HIGHLY_OVERUSED = "HighlyOverused";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      
      XComponent dataSet = form.findComponent(UTILIZATION_DATA_SET);
      XComponent dataRow;
      XLanguageResourceMap map = session.getLocale().getResourceMap(RESOURCE_MAP);

      //prepare the utilization legend data set
      XComponent legendDataSet = form.findComponent(UTILIZATION_LEGEND_DATA_SET);
      XComponent dataCell;
      
      //HIGHLY_UNDERUSED -> BACKGROUND
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(HIGHLY_UNDERUSED).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(OpProjectComponent.DEFAULT_UTILIZATION_ROW_STYLE_ATTRIBUTES.alternate_background);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);

      //UNDERUSED -> BLUE
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(UNDERUSED).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.DEFAULT_BLUE);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);

      //NORMALUSE -> GREEN
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(NORMALUSE).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);      
      dataCell.setValue(XStyle.DEFAULT_GREEN);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);

      //OVERUSED -> ORANGE
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(OVERUSED).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.DEFAULT_ORANGE);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);
      
      //HIGHLY_OVERUSED -> RED
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(map.getResource(HIGHLY_OVERUSED).getText());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(XStyle.DEFAULT_RED);
      dataRow.addChild(dataCell);
      legendDataSet.addChild(dataRow);

      OpBroker broker = session.newBroker();
      int[] poolColumnsSelector = new int[OpProjectComponent.UTILIZATION_COLUMNS + 1];
      poolColumnsSelector[OpProjectComponent.UTILIZATION_DESCRIPTOR_COLUMN_INDEX] = OpResourceDataSetFactory.DESCRIPTOR;
      poolColumnsSelector[OpProjectComponent.UTILIZATION_NAME_COLUMN_INDEX] = OpResourceDataSetFactory.NAME;
      poolColumnsSelector[OpProjectComponent.UTILIZATION_AVAILABLE_COLUMN_INDEX] = OpResourceDataSetFactory.NULL;
      poolColumnsSelector[OpProjectComponent.UTILIZATION_START_COLUMN_INDEX] = OpResourceDataSetFactory.NULL;
      poolColumnsSelector[OpProjectComponent.UTILIZATION_END_COLUMN_INDEX] = OpResourceDataSetFactory.NULL;
      poolColumnsSelector[OpProjectComponent.UTILIZATION_VALUES_COLUMN_INDEX] = OpResourceDataSetFactory.NULL;
      poolColumnsSelector[OpProjectComponent.UTILIZATION_COLUMNS] = OpResourceDataSetFactory.NULL;
      int[] resourceColumnsSelector = new int[OpProjectComponent.UTILIZATION_COLUMNS + 1];
      resourceColumnsSelector[OpProjectComponent.UTILIZATION_DESCRIPTOR_COLUMN_INDEX] = OpResourceDataSetFactory.DESCRIPTOR;
      resourceColumnsSelector[OpProjectComponent.UTILIZATION_NAME_COLUMN_INDEX] = OpResourceDataSetFactory.NAME;
      resourceColumnsSelector[OpProjectComponent.UTILIZATION_AVAILABLE_COLUMN_INDEX] = OpResourceDataSetFactory.AVAILABLE;
      resourceColumnsSelector[OpProjectComponent.UTILIZATION_START_COLUMN_INDEX] = OpResourceDataSetFactory.NULL;
      resourceColumnsSelector[OpProjectComponent.UTILIZATION_END_COLUMN_INDEX] = OpResourceDataSetFactory.NULL;
      resourceColumnsSelector[OpProjectComponent.UTILIZATION_VALUES_COLUMN_INDEX] = OpResourceDataSetFactory.NULL;
      resourceColumnsSelector[OpProjectComponent.UTILIZATION_COLUMNS] = OpResourceDataSetFactory.ID;
      OpResourceDataSetFactory.retrieveResourceDataSet(session, broker, dataSet, poolColumnsSelector, resourceColumnsSelector);

      // *** Phase 2: Query all work phases of all resource IDs we have access to (grouped or ordered by resource IDs)
      // ==> Calculate utilization based on work phases (one after the other -- iterating via grouping/sorting)
      // "select assignment.Resource.ID, assignment.Assigned, workPhase from OpAssignment as assignment inner join assignment.Activity as activity inner join activity.workPhases as workPhase"
      // "group/order by assignment.Resource.ID"
      // *** Important: Use resource.Available as "max%" rather than hard-coded 100%
      // ==> Quite simple since we anyway do it resource per resource (because of grouping/sorting -- see above)
      
      // TODO: Question -- is there a limit of number of values in an input set for a Hibernate query?
      HashSet resourceIds = new HashSet();
      HashMap resourceRowMap = new HashMap();
      dataRow = null;
      Long resourceId = null;
      int i = 0;
      for (i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         dataRow.setVisible((dataRow.getOutlineLevel() < 2));
         resourceId = (Long) ((XComponent) dataRow.getChild(OpProjectComponent.UTILIZATION_COLUMNS)).getValue();
         if (resourceId != null) {
            resourceIds.add(resourceId);
            resourceRowMap.put(resourceId, dataRow);
         }
      }
      
      // If there are no resources return w/o calculating utilization
      if (resourceIds.size() == 0)
         return;

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


      HashMap min = new HashMap();
      HashMap max = new HashMap();
      StringBuffer queryBuffer = new StringBuffer("select assignment.Resource.ID, min(activity.Start), max(activity.Finish) ");
      queryBuffer.append("from OpAssignment as assignment inner join assignment.Activity as activity ");
      queryBuffer.append("where activity.Deleted = false and assignment.Resource.ID in (:resourceIds) group by assignment.Resource.ID");
      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setCollection("resourceIds", resourceIds);
      Iterator result = broker.iterate(query);

      while (result.hasNext()){
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

   private boolean isWorkDay(OpWorkPeriod workPeriod, Date minStart, int dateIndex) {
      Date periodStart = workPeriod.getStart();
      Date checkedDate = new Date(minStart.getTime() + dateIndex * XCalendar.MILLIS_PER_DAY);
      int workPeriodDateIndex = (int) ((checkedDate.getTime() - periodStart.getTime()) / XCalendar.MILLIS_PER_DAY);
      long mask = (1L << workPeriodDateIndex);
      return (workPeriod.getWorkingDays() & mask) == mask;
   }

}
