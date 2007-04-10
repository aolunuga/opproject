/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_dates.forms;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.XRenderer;
import onepoint.express.XStyle;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project_dates.forms.OpProjectDatesFormProvider;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.team.modules.project_planning.components.OpChartComponent;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Timestamp;
import java.util.*;

public class OpProjectDatesAdvancedFormProvider extends OpProjectDatesFormProvider {

   public final static String MILESTONE_SET = "MilestoneSet";
   public final static String MILESTONE_COLORS_SET = "MilestonesColorsSet";
   public final static String MILESTONE_TABLE = "MilestoneTable";
   public final static String MILESTONE_CHART = "MilestoneChart";
   public final static int CURRENT_DATE_COLUMN_INDEX = 1;


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      String project_locator = (String) (parameters.get(PROJECT_ID));
      if (project_locator != null) {
         // Get open project-ID from parameters and set project-ID session variable
         session.setVariable(PROJECT_ID, project_locator);
      }
      else {
         project_locator = (String) (session.getVariable(PROJECT_ID));
      }
      if (project_locator != null) {
         OpProjectNode project = (OpProjectNode) (broker.getObject(project_locator));
         OpProjectPlan projectPlan = project.getPlan();
         if (projectPlan != null) {
            //allow the parent to do its job
            super.prepareForm(s, form, parameters);

            //set controlling interval and points
            long controlling_interval = XCalendar.MILLIS_PER_WEEK * Integer.valueOf(OpSettings.get(OpSettings.MILESTONE_CONTROLLING_INTERVAL)).intValue();
            java.sql.Date last_controlling_date = new java.sql.Date(XCalendar.today().getTime() - controlling_interval);
            int controlling_point_count = 5;
            long controlling_date = last_controlling_date.getTime();

            OpChartComponent milestone_chart = (OpChartComponent) (form.findComponent(MILESTONE_CHART));
            XExtendedComponent milestone_table = (XExtendedComponent) form.findComponent(MILESTONE_TABLE);
            controlling_date = fillMilestoneChart(milestone_table, controlling_point_count, controlling_date, controlling_interval, milestone_chart);

            // Fetch milestones and create milestone rows
            XComponent milestoneDataSet = form.findComponent(MILESTONE_SET);
            fillMilestones(broker, projectPlan, controlling_point_count, milestoneDataSet, controlling_interval, controlling_date, last_controlling_date);
            XComponent milestoneColorsDataSet = form.findComponent(MILESTONE_COLORS_SET);
            fillMilestonesColors(milestoneDataSet, milestoneColorsDataSet);
         }
      }
      broker.close();
   }

   /**
    * Fills the color data set for the chart and color legend components.
    * @param milestoneDataSet chart main data set
    * @param milestoneColorsDataSet color data set
    */
   private void fillMilestonesColors(XComponent milestoneDataSet, XComponent milestoneColorsDataSet) {
      for (int i=0; i<milestoneDataSet.getChildCount(); i++) {
         XComponent milestoneRow = (XComponent) milestoneDataSet.getChild(i);
         String name = ((XComponent)milestoneRow.getChild(0)).getStringValue();
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(name);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setValue(XStyle.colorSchema.get(i % XStyle.colorSchema.size()));
         dataRow.addChild(dataCell);
         milestoneColorsDataSet.addChild(dataRow);
      }
   }

   private void fillMilestones(OpBroker broker, OpProjectPlan projectPlan, int controlling_point_count, XComponent milestoneDataSet,
        long controlling_interval, long controlling_date, java.sql.Date last_controlling_date) {
      OpQuery query = broker.newQuery("select activity from OpActivity as activity where activity.ProjectPlan.ID = ? and activity.Deleted = false and activity.Type = ? order by activity.Sequence");
      query.setLong(0, projectPlan.getID());
      query.setByte(1, OpActivity.MILESTONE);
      Iterator result = broker.iterate(query);

      OpActivity milestone = null;
      HashMap milestoneDataRowMap = new HashMap();
      while (result.hasNext()) {
         milestone = (OpActivity) result.next();
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         String name = milestone.getName();
         //if name is null, replace with "" (for caption)
         if (name == null) {
            name = "";
         }
         dataCell.setStringValue(name);
         dataRow.addChild(dataCell);
         for (int columnIndex = 0; columnIndex <= controlling_point_count; columnIndex++) {
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setDateValue(milestone.getFinish());
            dataRow.addChild(dataCell);
         }
         milestoneDataSet.addChild(dataRow);
         milestoneDataRowMap.put(new Long(milestone.getID()), dataRow);
      }

      // Separate query for milestones since we might want to use a separate, different filter for the milestones
      // If there are zero (!) versions there are no versions
      int versions = projectPlan.getVersions().size();
      StringBuffer queryBuffer = null;
      boolean existVersions = false;
      if (versions == 0) {
         queryBuffer = new StringBuffer(
              "select activity.ID, plan.Created, activity.Finish ");
         queryBuffer
              .append("from OpProjectPlan as plan inner join plan.Activities as activity ");
         queryBuffer
              .append("where plan.ID = ? and activity.Type = ? and activity.Deleted = false");
      }
      else {
         existVersions = true;
         queryBuffer = new StringBuffer(
              "select activity.ID, planVersion.Created, activityVersion.Finish, planVersion.VersionNumber ");
         queryBuffer
              .append("from OpProjectPlanVersion as planVersion inner join planVersion.ActivityVersions as activityVersion inner join activityVersion.Activity as activity ");
         queryBuffer
              .append("where planVersion.ProjectPlan.ID = ? and planVersion.VersionNumber != ? and activity.Type = ? and activity.Deleted = false order by planVersion.VersionNumber desc");
      }

      query = broker.newQuery(queryBuffer.toString());

      //set query parameters
      if (existVersions) {
         query.setLong(0, projectPlan.getID());
         query.setLong(1, OpProjectAdministrationService.WORKING_VERSION_NUMBER);
         query.setByte(2, OpActivity.MILESTONE);
      }
      else {
         query.setLong(0, projectPlan.getID());
         query.setByte(1, OpActivity.MILESTONE);
      }
      
      result = broker.iterate(query);

      Long previousActivityId = null;
      Long activityId = null;
      XComponent dataRow = null;
      int columnIndex = 0;
      Timestamp created = null;
      java.sql.Date finish = null;
      java.sql.Date date = null;
      while (result.hasNext()) {
         Object[] record = (Object[]) result.next();
         activityId = (Long) record[0];

         created = (Timestamp) record[1];
         finish = (java.sql.Date) record[2];

         if ((previousActivityId == null) || (previousActivityId.longValue() != previousActivityId.longValue())) {
            // We moved on to a new milestone/data-row
            if (dataRow != null) {
               while (columnIndex > 0) {
                  ((XComponent) dataRow.getChild(columnIndex)).setDateValue(date);
                  controlling_date -= controlling_interval;
                  columnIndex--;
               }
            }
            // Get milestone data row belonging to this new milestone ID
            dataRow = (XComponent) milestoneDataRowMap.get(activityId);
            date = ((XComponent) dataRow.getChild(1)).getDateValue();
            // ((XComponent) dataRow.getChild(controlling_point_count + 1)).setDateValue(date);
            controlling_date = last_controlling_date.getTime();
            columnIndex = controlling_point_count;
         }

         while ((controlling_date > created.getTime()) && (columnIndex > 0)) {
            ((XComponent) dataRow.getChild(columnIndex)).setDateValue(date);
            controlling_date -= controlling_interval;
            columnIndex--;
         }

         // Move on to previous date
         date = finish;

      }

      if (dataRow != null) {
         // "Fill up" remaining controlling points?
         while (columnIndex > 0) {
            ((XComponent) dataRow.getChild(columnIndex)).setDateValue(date);
            controlling_date -= controlling_interval;
            columnIndex--;
         }
      }
   }

   private long fillMilestoneChart(XExtendedComponent milestone_table, int controlling_point_count, long controlling_date,
        long controlling_interval, OpChartComponent milestone_chart) {
      // Add table columns
      XRenderer renderer = new XRenderer();
      renderer.setValueType(XRenderer.DATE);

      //the value captions for MilestoneChart
      List valueCaptions = new ArrayList();

      XExtendedComponent table_column = (XExtendedComponent) milestone_table.getChild(XExtendedComponent.HEADER_INDEX).getChild(0).getChild(CURRENT_DATE_COLUMN_INDEX);
      table_column.setValueType(XRenderer.DATE);
      table_column.setDataColumnIndex(controlling_point_count + 1);
      //add CURRENT value column
      valueCaptions.add(table_column.getText());
      int columnIndex = 0;
      for (columnIndex = controlling_point_count; columnIndex >= 1; columnIndex--) {
         table_column = new XExtendedComponent(XExtendedComponent.TABLE_COLUMN);
         table_column.setText(renderer.valueToString(new java.sql.Date(controlling_date)));
         table_column.setValueType(XRenderer.DATE);
         table_column.setWidth(70);
         table_column.setDataColumnIndex(columnIndex);
         milestone_table.addChild(table_column);
         controlling_date -= controlling_interval;
         valueCaptions.add(table_column.getText());
      }

      Object[] valueCaptionsArray = valueCaptions.toArray();
      //rotate value captions
      for (int index = 0; index < valueCaptionsArray.length; index++) {
         valueCaptions.set(index, valueCaptionsArray[valueCaptionsArray.length - index - 1]);
      }

      milestone_chart.setDataColumnCount(controlling_point_count + 1);
      //set value captions on the line-chart-box
      milestone_chart.setValueCaptions(valueCaptions);
      return controlling_date;
   }
}
