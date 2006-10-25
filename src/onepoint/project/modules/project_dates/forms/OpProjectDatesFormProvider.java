/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_dates.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpUser;
import onepoint.service.server.XSession;

import java.util.*;

public class OpProjectDatesFormProvider implements XFormProvider {

   /*form components */
   public final static String PROJECT_START = "ProjectStartField";
   public final static String PROJECT_FINISH = "ProjectFinishField";
   public final static String HISTORY_SET = "HistorySet";
   public final static String ACTIVITY_SET = "ActivitySet";
   public final static String CATEGORY_COLOR_DATA_SET = "CategoryColorDataSet";
   public final static String MILESTONE_SET = "MilestoneSet";
   public final static String MILESTONE_TABLE = "MilestoneTable";
   public final static String MILESTONE_CHART = "MilestoneChart";
   public final static String PRINT_BUTTON = "PrintButton";
   public final static String RESOURCE_SET = "ResourceSet";
   public final static String RESOURCE_CHOICE_FIELD = "ResourceChooser";
   public final static String VERSION_CHOICE_FIELD = "VersionChooser";

   public final static String PROJECT_ID = "project_id";
   public final static String PRINT_TITLE = "PrintTitle";
   public final static int CURRENT_DATE_COLUMN_INDEX = 1;


   private final static String FORM_ID = "ProjectDatesForm";
   /*filters */
   public final static String RESOURCE_ID = "resource_id";
   public final static String VERSION_ID = "version_id";
   /*resource filter choices */
   private final static String ALL_RESOURCES = "all";
   /*version filter choices */
   private final static String CURRENT_PLAN = "cp";
   private final static String PREVIOUS_VERSION = "pv";
   private final static String ALL_VERSIONS = "av";
   private final static String LAST_5_VERSIONS = "l5v";


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

         //set start and end for project
         form.findComponent(PROJECT_START).setDateValue(project.getStart());
         form.findComponent(PROJECT_FINISH).setDateValue(project.getFinish());

         //print title
         form.findComponent(PRINT_TITLE).setStringValue(project.getName());
         form.findComponent(PRINT_BUTTON).setEnabled(true);

         //fill resource set for selected project's assignments
         XComponent resourceDataSet = form.findComponent(RESOURCE_SET);
         fillResourcesDataSet(project, resourceDataSet);

         /*selected resource from choice field */
         String filterResourceId = (String) parameters.get(RESOURCE_ID);
         /*selected version from choice field */
         String filterVersionId = (String) parameters.get(VERSION_ID);
         //set up the default selected index
         if (filterResourceId == null && filterVersionId == null) {
            Integer defaultSelectedIndex = new Integer(0);
            Map stateMap = session.getComponentStateMap(FORM_ID);
            if (stateMap != null) {
               stateMap.put(VERSION_CHOICE_FIELD, defaultSelectedIndex);
               stateMap.put(RESOURCE_CHOICE_FIELD, defaultSelectedIndex);
            }
         }

         // Check it there is already a project plan
         OpProjectPlan projectPlan = project.getPlan();
         if (projectPlan != null) {
            //fill the project name
            XComponent project_name_set = form.findComponent("ProjectNameSet");
            fillProjectName(project, project_name_set);

            //locate and fill history data set (use project plan history BLOBs)
            XComponent historyDataSet = form.findComponent(HISTORY_SET);
            boolean showAllResources = (filterResourceId == null) || (filterResourceId != null && filterResourceId.equals(ALL_RESOURCES));
            fillHistoryDataSet(showAllResources, filterResourceId, broker, projectPlan.getID(), filterVersionId, historyDataSet);

            //fill the activity set
            XComponent activityDataSet = form.findComponent(ACTIVITY_SET);
            fillActivitySet(session, broker, activityDataSet, showAllResources, projectPlan, filterResourceId, project.getID());

            // fill category color data set
            XComponent categoryColorDataSet = form.findComponent(CATEGORY_COLOR_DATA_SET);
            OpActivityDataSetFactory.fillCategoryColorDataSet(broker, categoryColorDataSet);
         }
      }
      broker.close();
   }

   private void fillActivitySet(OpProjectSession session, OpBroker broker, XComponent activityDataSet, boolean showAllResources,
        OpProjectPlan projectPlan, String filterResourceId, long projectId) {
      OpUser currentUser = session.user(broker);
      String showHoursPref = currentUser.getPreferenceValue(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showHoursPref == null) {
         showHoursPref = OpSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS);
      }
      activityDataSet.setValue(Boolean.valueOf(showHoursPref));

      // Locate and fill activity data set
      if (showAllResources) {
         OpActivityDataSetFactory.retrieveActivityDataSet(broker, projectPlan, activityDataSet, false);
      }
      else {
         long resourceID = OpLocator.parseLocator(filterResourceId).getID();
         /*retrive activity data set using a specific filter */
         OpActivityFilter filter = new OpActivityFilter();
         filter.addProjectNodeID(projectId);
         filter.addResourceID(resourceID);
         filter.setDependencies(true);
         // configure activity sort order
         Map sortOrders = new HashMap();
         sortOrders.put(OpActivity.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
         OpObjectOrderCriteria orderCriteria = new OpObjectOrderCriteria(OpActivity.ACTIVITY, sortOrders);
         OpActivityDataSetFactory.retrieveFilteredActivityDataSet(broker, filter, orderCriteria, activityDataSet);
      }
   }

   /**
    * Fills the dateset with the project name.
    *
    * @param project          a <code>OpProjectNode</code> representing the project whose name will be taken.
    * @param project_name_set a <code>XComponent</code> representing a data set.
    */
   private void fillProjectName(OpProjectNode project, XComponent project_name_set) {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(project.getName());
      project_name_set.addChild(dataRow);
   }

   /**
    * Fills the history data set with data.
    *
    * @param showAllResources a <code>boolean</code> indicating whether to show all the resources or not.
    * @param filterResourceId a <code>String</code> representing the id of a resource, in the case when the above flag
    *                         is false.
    * @param broker           a <code>OpBroker</code> used for performing business operations.
    * @param planId           a <code>long</code> representing the id of a project plan.
    * @param filterVersionId  a <code>String</code> representing the id of ther version to filter. May be null.
    * @param historyDataSet   a <code>XComponent representing the history data set.
    */
   private void fillHistoryDataSet(boolean showAllResources, String filterResourceId, OpBroker broker, long planId,
        String filterVersionId, XComponent historyDataSet) {
      XComponent dataRow;
      OpQuery query = null;
      StringBuffer queryBuffer = null;
      if (showAllResources) {
         queryBuffer = new StringBuffer("select activity.Sequence, planVersion.VersionNumber, activityVersion.Start, activityVersion.Finish ");
         queryBuffer
              .append("from OpProjectPlanVersion as planVersion inner join planVersion.ActivityVersions as activityVersion inner join activityVersion.Activity as activity ");
         queryBuffer
              .append("where planVersion.ProjectPlan.ID = ? and planVersion.VersionNumber != ? and activity.Deleted = false order by activity.Sequence, planVersion.VersionNumber desc");
         query = broker.newQuery(queryBuffer.toString());
         query.setLong(0, planId);
         query.setLong(1, OpProjectAdministrationService.WORKING_VERSION_NUMBER);
      }
      else {
         long resourceID = OpLocator.parseLocator(filterResourceId).getID();
         queryBuffer = new StringBuffer("select activity.Sequence, planVersion.VersionNumber, activityVersion.Start, activityVersion.Finish ");
         queryBuffer
              .append("from OpProjectPlanVersion planVersion inner join planVersion.ActivityVersions activityVersion inner join activityVersion.Activity activity inner join activity.Assignments assignment ");
         queryBuffer
              .append("where planVersion.ProjectPlan.ID = ? and planVersion.VersionNumber != ? and activity.Deleted = false and assignment.Resource.ID = ? order by activity.Sequence, planVersion.VersionNumber desc");
         query = broker.newQuery(queryBuffer.toString());
         query.setLong(0, planId);
         query.setLong(1, OpProjectAdministrationService.WORKING_VERSION_NUMBER);
         query.setLong(2, resourceID);
      }

      List results = broker.list(query);

      int activitySequence = 0;
      int previousActivitySequence = -1;
      ArrayList starts = null;
      ArrayList finishes = null;
      Object[] record = null;
      XComponent dataCell = null;
      boolean filterCurrentPlan = filterVersionId == null || (filterVersionId != null && filterVersionId.equals(CURRENT_PLAN));
      boolean filterPreviousVersion = filterVersionId != null && filterVersionId.equals(PREVIOUS_VERSION);
      boolean filterLast5Versions = filterVersionId != null && filterVersionId.equals(LAST_5_VERSIONS);
      boolean filterAllVersions = filterVersionId != null && filterVersionId.equals(ALL_VERSIONS);

      if (!filterCurrentPlan) { //no history for current plan selection
         for (int index = 0; index < results.size(); index++) {
            record = (Object[]) results.get(index);
            if (record[2] == null || record[3] == null) {
               continue;
            }
            activitySequence = ((Integer) record[0]).intValue();

            if (activitySequence > previousActivitySequence) {
               // We moved on to a new activity/data-row
               dataRow = new XComponent(XComponent.DATA_ROW);
               dataCell = new XComponent(XComponent.DATA_CELL);
               starts = new ArrayList();
               dataCell.setListValue(starts);
               dataRow.addChild(dataCell);
               dataCell = new XComponent(XComponent.DATA_CELL);
               finishes = new ArrayList();
               dataCell.setListValue(finishes);
               dataRow.addChild(dataCell);
               historyDataSet.addChild(dataRow);
               previousActivitySequence = activitySequence;
            }

            // History data is already sorted correctly
            // (Important: Must be from newer to older -- activities could habe been added later on)
            if (filterPreviousVersion && starts.size() == 0) {
               starts.add(record[2]);
               finishes.add(record[3]);
            }
            else if (filterLast5Versions && starts.size() < 5) {
               starts.add(record[2]);
               finishes.add(record[3]);
            }
            else if (filterAllVersions) {
               starts.add(record[2]);
               finishes.add(record[3]);
            }
         }
      }
   }

   /**
    * Fills the resources data set for the given project node.
    *
    * @param project         a <code>OpProjectNode</code> representing a selected project.
    * @param resourceDataSet a <code>XComponent(DATA_SET)</code> representing the
    */
   private void fillResourcesDataSet(OpProjectNode project, XComponent resourceDataSet) {
      Iterator assignments = project.getAssignments().iterator();
      while (assignments.hasNext()) {
         OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) assignments.next();
         OpResource resource = assignment.getResource();
         XComponent dataRow = resourceDataSet.newDataRow();
         dataRow.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
         resourceDataSet.addDataRow(dataRow);
         resourceDataSet.sort();
      }
   }

}
