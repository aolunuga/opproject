/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_dates.forms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityFilter;
import onepoint.project.modules.project.OpActivityVersionDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpSubjectDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.service.server.XSession;

public class OpProjectDatesFormProvider implements XFormProvider {

   public final static String PROJECT_ID = "project_id";

   /*form components */
   private final static String PROJECT_START = "ProjectStartField";
   private final static String PROJECT_FINISH = "ProjectFinishField";
   private final static String HISTORY_SET = "HistorySet";
   private final static String ACTIVITY_SET = "ActivitySet";
   private final static String RESOURCE_AVAILABILITY = "ResourceAvailability";
   private final static String CATEGORY_COLOR_DATA_SET = "CategoryColorDataSet";
   private final static String PRINT_BUTTON = "PrintButton";
   private final static String RESOURCE_SET = "ResourceSet";
   private final static String RESOURCE_CHOICE_FIELD = "ResourceChooser";
   private final static String VERSION_CHOICE_FIELD = "VersionChooser";
   private final static String TIME_CHOICE_FIELD = "TimeUnitChooser";
   private final static String ACTIVITY_GANTT_CHART = "ActivityGanttChart";
   private final static String PRINT_TITLE = "PrintTitle";
   private final static String FORM_ID = "ProjectDatesForm";
   /*filters */
   private final static String RESOURCE_ID = "resource_id";
   private final static String VERSION_ID = "version_id";
   /*resource filter choices */
   private final static String ALL_RESOURCES = "all";
   /*version filter choices */
   private final static String CURRENT_PLAN = "cp";
   private final static String PREVIOUS_VERSION = "pv";
   private final static String ALL_VERSIONS = "av";
   private final static String LAST_5_VERSIONS = "l5v";
   private final static String BASELINE = "bl";
   private final static String BASELINE_CHOICE = "BaselineChoice";


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      try {
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
            form.findComponent(RESOURCE_CHOICE_FIELD).setEnabled(true);
            form.findComponent(VERSION_CHOICE_FIELD).setEnabled(true);
            form.findComponent(TIME_CHOICE_FIELD).setEnabled(true);

            //check manager rights for costs view
            OpUser user = session.user(broker);
            if (OpSubjectDataSetFactory.shouldHideFromUser(session, user)) {
               ((OpProjectComponent) form.findComponent(ACTIVITY_GANTT_CHART)).setShowCosts(false);
            }

            //fill resource set for selected project's assignments
            XComponent resourceDataSet = form.findComponent(RESOURCE_SET);
            fillResourcesDataSet(project, resourceDataSet);

            //selected resource from choice field
            String filterResourceId = (String) parameters.get(RESOURCE_ID);
            //selected version from choice field
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

               if (projectPlan.getBaseVersion() == null) {
                  XComponent choice = form.findComponent(BASELINE_CHOICE);
                  choice.setEnabled(false);
               }

               //fill the project name
               XComponent project_name_set = form.findComponent("ProjectNameSet");
               fillProjectName(project, project_name_set);

               boolean showWorkingVersion = false;
               if (project.getLocks() != null && project.getLocks().iterator().hasNext()) {
                  showWorkingVersion = project.getLocks().iterator().next().getOwner().getId() == user.getId();
               }
               //locate and fill history data set (use project plan history BLOBs)
               XComponent historyDataSet = form.findComponent(HISTORY_SET);
               boolean showAllResources = (filterResourceId == null) || (filterResourceId != null && filterResourceId.equals(ALL_RESOURCES));
               fillHistoryDataSet(showAllResources, filterResourceId, broker, projectPlan, filterVersionId, historyDataSet, showWorkingVersion);

               //fill the activity set
               XComponent activityDataSet = form.findComponent(ACTIVITY_SET);
               OpProjectPlanVersion pv = projectPlan.getLatestVersion();
               if (session.checkAccessLevel(broker, projectPlan.getProjectNode().getId(), OpPermission.MANAGER)) {
                  pv = projectPlan.getWorkingVersion() != null ? projectPlan.getWorkingVersion() : pv;
               }
               if (pv != null) {
                  fillActivitySet(session, broker, activityDataSet,
                        showAllResources, pv, filterResourceId, project.getId());
               }

               // fill category color data set
               XComponent categoryColorDataSet = form.findComponent(CATEGORY_COLOR_DATA_SET);
               OpActivityDataSetFactory.fillCategoryColorDataSet(broker, categoryColorDataSet);
            }

            //fill the availability map
            XComponent resourceAvailability = form.findComponent(RESOURCE_AVAILABILITY);
            Map<String, Double> availabilityMap = OpResourceDataSetFactory.createResourceAvailabilityMap(broker);
            resourceAvailability.setValue(availabilityMap);
         }
      }
      finally {
         broker.close();
      }
   }

   private void fillActivitySet(OpProjectSession session, OpBroker broker, XComponent activityDataSet, boolean showAllResources,
        OpProjectPlanVersion projectPlanVersion, String filterResourceId, long projectId) {
      OpUser currentUser = session.user(broker);
      String showHoursPref = currentUser.getPreferenceValue(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showHoursPref == null) {
         showHoursPref = OpSettingsService.getService().getStringValue(session, OpSettings.SHOW_RESOURCES_IN_HOURS);
      }
      activityDataSet.setValue(Boolean.valueOf(showHoursPref));

      // Locate and fill activity data set
      if (showAllResources) {
         OpActivityVersionDataSetFactory.getInstance().retrieveActivityVersionDataSet(session, broker, projectPlanVersion, activityDataSet, false);
      }
      else {
         long resourceID = OpLocator.parseLocator(filterResourceId).getID();
         /*retrive activity data set using a specific filter */
         OpActivityFilter filter = new OpActivityFilter();
         filter.addProjectNodeID(projectId);
         filter.addResourceID(resourceID);
         filter.setDependencies(true);
         // configure activity sort order
         SortedMap sortOrders = new TreeMap();
         sortOrders.put(OpActivity.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
         OpObjectOrderCriteria orderCriteria = new OpObjectOrderCriteria(OpActivity.class, sortOrders);
         OpActivityDataSetFactory.getInstance().retrieveFilteredActivityDataSet(session, broker, filter, orderCriteria, activityDataSet);
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
   private void fillHistoryDataSet(boolean showAllResources, String filterResourceId, OpBroker broker, OpProjectPlan plan,
        String filterVersionId, XComponent historyDataSet, boolean showWorkingVersion) {
      boolean filterCurrentPlan = filterVersionId == null || (filterVersionId != null && filterVersionId.equals(CURRENT_PLAN));
      if (filterCurrentPlan) {
         return;
      }
      if (plan.getVersions() == null) {
         return;
      }
      boolean filterPreviousVersion = filterVersionId != null && filterVersionId.equals(PREVIOUS_VERSION);
      boolean filterLast5Versions = filterVersionId != null && filterVersionId.equals(LAST_5_VERSIONS);
      boolean filterAllVersions = filterVersionId != null && filterVersionId.equals(ALL_VERSIONS);
      boolean filterBaselineVersion = filterVersionId != null && filterVersionId.equals(BASELINE);

      Set<Integer> versionsToShow = null;
      if (filterPreviousVersion) {
         versionsToShow = getNVersionsToShow(plan, 1, showWorkingVersion);
      }
      else if (filterLast5Versions) {
         versionsToShow = getNVersionsToShow(plan, 5, showWorkingVersion);
      }
      else if (filterAllVersions) {
         versionsToShow = getNVersionsToShow(plan, Integer.MAX_VALUE, showWorkingVersion);
      }
      else if (filterBaselineVersion) {
         versionsToShow = new HashSet<Integer>();
         if (plan.getBaseVersion() != null) {
            versionsToShow.add(new Integer(plan.getBaseVersion().getVersionNumber()));
         }
      }
      
      if (versionsToShow == null || versionsToShow.isEmpty()) {
         return;
      }
      
      XComponent dataRow;
      OpQuery query = null;
      StringBuffer queryBuffer = null;
      if (showAllResources) {
         queryBuffer = new StringBuffer("select activity.Sequence, planVersion.VersionNumber, activityVersion.Start, activityVersion.Finish, planVersion.Baseline ");
         queryBuffer
              .append("from OpProjectPlanVersion as planVersion inner join planVersion.ActivityVersions as activityVersion inner join activityVersion.Activity as activity ");
         queryBuffer
              .append("where planVersion.ProjectPlan.id = :planId and planVersion.VersionNumber in (:versionIds) order by activity.Sequence, planVersion.VersionNumber desc");
         query = broker.newQuery(queryBuffer.toString());
         query.setLong("planId", plan.getId());
         query.setCollection("versionIds", versionsToShow);
      }
      else {
         long resourceID = OpLocator.parseLocator(filterResourceId).getID();
         queryBuffer = new StringBuffer("select activity.Sequence, planVersion.VersionNumber, activityVersion.Start, activityVersion.Finish, planVersion.Baseline ");
         queryBuffer
              .append("from OpProjectPlanVersion planVersion inner join planVersion.ActivityVersions activityVersion inner join activityVersion.Activity activity inner join activity.Assignments assignment ");
         queryBuffer
              .append("where planVersion.ProjectPlan.id = :planId and planVersion.VersionNumber in (:versionIds) and assignment.Resource.id = :resourceId order by activity.Sequence, planVersion.VersionNumber desc");
         query = broker.newQuery(queryBuffer.toString());
         query.setLong("planId", plan.getId());
         query.setCollection("versionIds", versionsToShow);
         query.setLong("resourceId", resourceID);
      }

      Iterator rit = broker.iterate(query);

      int activitySequence = 0;
      int previousActivitySequence = -1;
      ArrayList starts = null;
      ArrayList finishes = null;
      Object[] record = null;
      XComponent dataCell = null;
      while (rit.hasNext()) {
         record = (Object[]) rit.next();
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
         else if (filterBaselineVersion && ((Boolean)record[4]).booleanValue()) {
            starts.add(record[2]);
            finishes.add(record[3]);
         }
         else if (filterAllVersions) {
            starts.add(record[2]);
            finishes.add(record[3]);
         }
      }
   }

   private Set<Integer> getNVersionsToShow(OpProjectPlan plan, int numPlans, boolean includeLatest) {
      SortedMap<Integer, OpProjectPlanVersion> viewedPlanVersionIds = new TreeMap<Integer, OpProjectPlanVersion>(new Comparator<Integer>() {
         public int compare(Integer o1, Integer o2) {
            return o2.compareTo(o1);
         }});
      int highestVersionNumber = -1;
      for (OpProjectPlanVersion pv : plan.getVersions()) {
         int vn = pv.getVersionNumber();
         highestVersionNumber = vn > highestVersionNumber ? vn : highestVersionNumber;
         viewedPlanVersionIds.put(new Integer(vn), pv);
      }
      Set<Integer> versionsToShow = new HashSet<Integer>();
      Iterator<Map.Entry<Integer, OpProjectPlanVersion>> pvit = viewedPlanVersionIds.entrySet().iterator();
      while (versionsToShow.size() < numPlans && pvit.hasNext()) {
         Map.Entry<Integer, OpProjectPlanVersion> e = pvit.next();
         if ((e.getValue().getVersionNumber() == highestVersionNumber && !includeLatest)
               || e.getValue().getVersionNumber() == OpProjectPlan.WORKING_VERSION_NUMBER) {
            continue;
         }
         versionsToShow.add(new Integer(e.getValue().getVersionNumber())); 
      }
      return versionsToShow;
   }

   /**
    * Fills the resources data set for the given project node.
    *
    * @param project         a <code>OpProjectNode</code> representing a selected project.
    * @param resourceDataSet a <code>XComponent(DATA_SET)</code> representing the
    */
   private void fillResourcesDataSet(OpProjectNode project, XComponent resourceDataSet) {
      Map<String, String> sortedResources = new TreeMap<String, String>();
      Iterator assignments = project.getAssignments().iterator();
      while (assignments.hasNext()) {
         OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) assignments.next();
         OpResource resource = assignment.getResource();
         sortedResources.put(resource.getName(), resource.locator());
      }
      for (String resourceName : sortedResources.keySet()) {
         XComponent dataRow = resourceDataSet.newDataRow();
         dataRow.setStringValue(XValidator.choice(sortedResources.get(resourceName), resourceName));
         resourceDataSet.addChild(dataRow);
      }
   }

}
