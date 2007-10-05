/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.validators.OpWorkEffortValidator;

import java.sql.Date;
import java.util.*;

/**
 * Data Set factory for WorkSlips .
 *
 * @author mihai.costin
 */
public class OpWorkSlipDataSetFactory {

   //indexes used when returning a list of data sets from a list of work records
   public static final int WORK_RECORD_SET_INDEX = 0;
   public static final int TIME_RECORD_SET_INDEX = 1;
   public static final int COST_RECORD_SET_INDEX = 2;

   public final static long ALL_PROJECTS_ID = -1;

   /**
    * Utility class.
    */
   private OpWorkSlipDataSetFactory() {
   }

   /**
    * Gets the assignments satisfing the given constraints.
    *
    * @param broker                Broker used to qury the db.
    * @param resourceIds           Resources to take into account when searching for the assignments.
    * @param activityTypes         Types of activities to include in the search.
    * @param start                 Limit date. Will return only those assignments that have activities starting before the
    *                              given date.
    * @param activityOrderCriteria a <code>OpObjectOrderCriteria</code> used for ordering the activities.
    * @param projectNodeId         Project Id filter. Will return only those assignments that have activities belonging
    *                              to the given project. If all projects are to be taken into account, ALL_PROJECTS_ID
    *                              should be used.
    * @param allowArchivedProjects a <code>boolean</code> whether to filter out or not archived projects. @return Iterator over the found assignments.
    */
   public static Iterator<Object[]> getAssignments(OpBroker broker, List resourceIds, List activityTypes,
        Date start, OpObjectOrderCriteria activityOrderCriteria, long projectNodeId, boolean allowArchivedProjects) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("select assignment, activity from OpAssignment as assignment inner join assignment.Activity as activity ");
      buffer.append("where assignment.Resource.ID in (:resourceIds) and assignment.Complete < 100 and activity.Type in (:type) and activity.Deleted = false");
      if (start != null) {
         buffer.append(" and activity.Start < :startBefore");
      }
      if (!allowArchivedProjects) {
         buffer.append(" and activity.ProjectPlan.ProjectNode.Archived=false ");
      }
      if (projectNodeId != ALL_PROJECTS_ID) {
         buffer.append(" and assignment.ProjectPlan.ProjectNode.ID = :projectNodeId");
      }
      if (activityOrderCriteria != null) {
         buffer.append(activityOrderCriteria.toHibernateQueryString("activity"));
      }
      OpQuery query = broker.newQuery(buffer.toString());
      query.setCollection("resourceIds", resourceIds);
      query.setCollection("type", activityTypes);
      if (start != null) {
         query.setDate("startBefore", start);
      }
      if (projectNodeId != ALL_PROJECTS_ID) {
         query.setLong("projectNodeId", projectNodeId);
      }

      List<Object[]> result = new ArrayList<Object[]>();
      List<Object[]> adHocActivities = new ArrayList<Object[]>();
      Iterator<Object[]> it = broker.iterate(query);
      while (it.hasNext()) {
         Object[] record = it.next();
         OpActivity activity = (OpActivity) record[1];
         if (activity.getType() == OpActivity.ADHOC_TASK) {
            adHocActivities.add(record);
         }
         else {
            result.add(record);
         }
      }
      result.addAll(adHocActivities);
      return result.iterator();
   }

   /**
    * Creates a work slip data row using the given information.
    *
    * @param activity        Workslip activity.
    * @param assignment      Workslip assignment.
    * @param progressTracked Flag indicating if progress tarck is on/off.
    * @param dataSet
    * @return a Data Row representing a work slip.
    */
   public static XComponent addWorkEffortRowToDataSet(OpActivity activity, OpAssignment assignment, boolean progressTracked, XComponent dataSet) {
      String choice;
      XComponent data_cell;
      double remainingEffort;
      // Iterate super-activities and "patch" activity name by adding context
      // (Note: This and the assignments can be optimized using bulk-queries)
      String name = activity.getName();
      StringBuffer activityName = (name != null) ? new StringBuffer(name) : new StringBuffer();
      OpActivity superActivity = activity.getSuperActivity();
      if (superActivity != null) {
         while (superActivity != null) {
            if (superActivity.getID() == activity.getSuperActivity().getID()) {
               activityName.append(" (");
            }
            else {
               activityName.append(" - ");
            }
            name = superActivity.getName();
            activityName.append((name != null) ? name : "");
            superActivity = superActivity.getSuperActivity();
         }
         activityName.append(')');
      }

      choice = XValidator.choice(assignment.locator(), activityName.toString());

      OpWorkEffortValidator validator = (OpWorkEffortValidator) dataSet.validator();
      XComponent dataRow = validator.newDataRow();

      //activity name
      OpWorkEffortValidator.set(dataRow, OpWorkEffortValidator.ACTIVITY_NAME_INDEX, choice);

      // New effort
      data_cell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX);
      if (activity.getType() == OpActivity.MILESTONE) {
         data_cell.setValue(null);
         data_cell.setEnabled(false);
      }
      else {
         data_cell.setDoubleValue(0.0);
         data_cell.setEnabled(true);
      }

      // Remaining effort -- default value is current effort minus already booked effort - 2
      data_cell = (XComponent) dataRow.getChild(OpWorkEffortValidator.REMAINING_EFFORT_INDEX);
      remainingEffort = assignment.getBaseEffort() - assignment.getActualEffort();
      if (remainingEffort < 0.0d) {
         remainingEffort = 0.0d;
      }
      if (progressTracked && activity.getType() != OpActivity.MILESTONE && activity.getType() != OpActivity.ADHOC_TASK) {
         data_cell.setDoubleValue(remainingEffort);
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }

      // Resource
      String resource = XValidator.choice(assignment.getResource().locator(), assignment.getResource().getName());
      OpWorkEffortValidator.set(dataRow, OpWorkEffortValidator.RESOURCE_NAME_INDEX, resource);

      // Original remainig effort (can be changed from the client side) - 5
      OpWorkEffortValidator.set(dataRow, OpWorkEffortValidator.ORIGINAL_REMAINING_INDEX, remainingEffort);

      // Completed
      data_cell = (XComponent) dataRow.getChild(OpWorkEffortValidator.COMPLETED_INDEX);
      if (assignment.getProjectPlan().getProgressTracked() || activity.getType() == OpActivity.ADHOC_TASK) {
         data_cell.setBooleanValue(false);
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setValue(null);
         data_cell.setEnabled(false);
      }

      // Activity type
      OpWorkEffortValidator.set(dataRow, OpWorkEffortValidator.ACTIVITY_TYPE_INDEX, activity.getType());

      // Activity's project
      String project = XValidator.choice(activity.getProjectPlan().getProjectNode().locator(), activity.getProjectPlan().getProjectNode().getName());
      OpWorkEffortValidator.set(dataRow, OpWorkEffortValidator.PROJECT_NAME_INDEX, project);

      // Assignment base effort
      OpWorkEffortValidator.set(dataRow, OpWorkEffortValidator.PLANNED_EFFORT_INDEX, assignment.getBaseEffort());

      //set the value of the dataRow to the id of the assignment
      dataRow.setStringValue(choice);

      dataSet.addChild(dataRow);

      return dataRow;
   }

   /**
    * Creates a set of <code>OpWorkRecord</code> entities from the workEffortDataSet parameters.
    * Each entity will have it's cost records and time records sets obtained from the
    * timeRecordDataSet and costRecordDataSet parameters.
    *
    * @param broker            - the <code>OpBroker</code> needed to persist the attachments and contents
    * @param workEffortDataSet - the <code>XComponent</code> data set which contains information
    *                          about the <code>OpWorkRecord</code> entities
    * @param timeRecordDataSet - the <code>XComponent</code> data set which contains information
    *                          about the <code>OpTimeRecord</code> entities
    * @param costRecordDataSet - the <code>XComponent</code> data set which contains information
    *                          about the <code>OpCostRecord</code> entities
    * @return a <code>List</code> of <code>OpWorkRecord</code> entities
    */
   public static List<OpWorkRecord> formWorkRecordsFromDataSets(OpBroker broker, XComponent workEffortDataSet,
        XComponent timeRecordDataSet, XComponent costRecordDataSet) {
      List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();
      XComponent workDataRow;
      List<XComponent> costRecordRowsList;

      for (int i = 0; i < workEffortDataSet.getChildCount(); i++) {
         //obtain the work records entity from the work effort row
         workDataRow = (XComponent) workEffortDataSet.getChild(i);
         OpWorkRecord workRecord = OpWorkEffortDataSetFactory.createWorkEntity(broker, workDataRow);

         //obtain the sub data set of time records which belong to the work record
         XComponent timeRecordSubset = filterDataSet(timeRecordDataSet, workDataRow);
         workRecord.setTimeRecords(OpTimeRecordDataSetFactory.createTimeRecords(timeRecordSubset));

         //obtain the sub data set of cost records which belong to the work record
         XComponent costRecordSubset = filterDataSet(costRecordDataSet, workDataRow);
         workRecord.setCostRecords(OpCostRecordDataSetFactory.createCostRecords(broker, costRecordSubset));

         //remove the cost subset for this work record from the original CostRecordDataSet
         costRecordRowsList = new ArrayList<XComponent>();
         for (int j = 0; j < costRecordSubset.getChildCount(); j++) {
            costRecordRowsList.add((XComponent) costRecordSubset.getChild(j));
         }
         costRecordDataSet.removeChildren(costRecordRowsList);

         workRecords.add(workRecord);
      }

      /*if there are any cost records that do not belong to any work records create "empty" work records for
        these cost records*/
      List<OpWorkRecord> emptyWorkRecords = new ArrayList<OpWorkRecord>();
      XComponent costDataRow;
      boolean existsEmptyWorkRecord;
      for (int i = 0; i < costRecordDataSet.getChildCount(); i++) {
         existsEmptyWorkRecord = false;
         costDataRow = (XComponent) costRecordDataSet.getChild(i);
         //form a set with the cost record form this row
         XComponent tempDataSet = new XComponent();
         tempDataSet.addChild(costDataRow);
         Set<OpCostRecord> costRecordSet = OpCostRecordDataSetFactory.createCostRecords(broker, tempDataSet);

         for (OpWorkRecord emptyWorkRecord : emptyWorkRecords) {
            //if there already is an "empty" work record for the cost row's assignment
            if (costDataRow.getStringValue().equals(emptyWorkRecord.getAssignment().locator())) {
               existsEmptyWorkRecord = true;
               emptyWorkRecord.getCostRecords().addAll(costRecordSet);
               break;
            }
         }

         //if there was no "empty" record for the cost
         if (!existsEmptyWorkRecord) {
            OpWorkRecord workRecord = new OpWorkRecord();
            workRecord.setCompleted(false);
            workRecord.setAssignment((OpAssignment) broker.getObject(costDataRow.getStringValue()));
            Set<OpCostRecord> costRecords = new HashSet<OpCostRecord>();
            costRecords.addAll(costRecordSet);
            workRecord.setCostRecords(costRecords);
            workRecords.add(workRecord);
            emptyWorkRecords.add(workRecord);
         }
      }

      return workRecords;
   }

   /**
    * Returns a <code>List</code> of <code>XComponent</code> data sets formed from a <code>List</code>
    * of <code>OpWorkRecord</code> entities.
    *
    * @param workRecords - the <code>List</code> of <code>OpWorkRecord</code> entities from which the
    *                    data sets will be formed
    * @param session     - the <code>OpProjectSession</code> needed to get the internationalized cost types
    * @return a <code>List</code> of <code>XComponent</code> data sets formed from a <code>List</code>
    *         of <code>OpWorkRecord</code> entities. The list will contain:
    *         a set containing information about the work records
    *         a set containing information about the time records
    *         a set containing information about the cost records
    */
   public static List<XComponent> formDataSetsFromWorkRecords(List<OpWorkRecord> workRecords, OpProjectSession session) {
      List<XComponent> dataSetList = new ArrayList<XComponent>();
      XComponent workRecordDataSet = new XComponent(XComponent.DATA_SET);
      XComponent timeRecordDataSet = new XComponent(XComponent.DATA_SET);
      XComponent costRecordDataSet = new XComponent(XComponent.DATA_SET);

      for (OpWorkRecord workRecord : workRecords) {

         //add this entity in the work effort data set only if it is not an "empty" work record
         if (!workRecord.isEmpty()) {
            //form the work record data set
            workRecordDataSet.addChild(OpWorkEffortDataSetFactory.createEffortRow(workRecord));
         }

         //add the time record subset of this work record to the final time record data set
         XComponent timeRecordSubset = OpTimeRecordDataSetFactory.getTimeDataSetForWorkRecord(workRecord);
         for (int i = 0; i < timeRecordSubset.getChildCount(); i++) {
            timeRecordDataSet.addChild(timeRecordSubset.getChild(i));
         }

         //add the cost record subset of this work record to the final cost record data set
         XComponent costRecordSubset = OpCostRecordDataSetFactory.getCostDataSetForWorkRecord(workRecord, session);
         for (int i = 0; i < costRecordSubset.getChildCount(); i++) {
            costRecordDataSet.addChild(costRecordSubset.getChild(i));
         }
      }

      dataSetList.add(WORK_RECORD_SET_INDEX, workRecordDataSet);
      dataSetList.add(TIME_RECORD_SET_INDEX, timeRecordDataSet);
      dataSetList.add(COST_RECORD_SET_INDEX, costRecordDataSet);
      return dataSetList;
   }

   /**
    * Filters a <code>XComponent</code> data set on a <code>List</code> of assignments.
    *
    * @param dataSet     - the data set to be filtered.
    * @param assignments - the <code>List</code> of assignments against which the data set is filtered.
    */
   public static void filterDataSetForAssignments(XComponent dataSet, List<OpAssignment> assignments) {
      XComponent dataRow;
      boolean existsInList;
      for (int i = 0; i < dataSet.getChildCount(); i++) {

         existsInList = false;
         dataRow = (XComponent) dataSet.getChild(i);
         //reset previous filters
         dataRow.setFiltered(false);
         for (OpAssignment assignment : assignments) {
            if (XValidator.choiceID(dataRow.getStringValue()).equals(assignment.locator())) {
               existsInList = true;
               break;
            }
         }
         if (!existsInList) {
            dataRow.setFiltered(true);
         }
      }
   }

   /**
    * Adds 2 cells to each row in the <code>XComponent</code> projectChoiceSet.
    * - the first cell will contain a <code>List</code> with the locators of the activities related to that row's project,
    * - the second cell will contain a <code>List</code> with the locators of the resources related to that row's project.
    *
    * @param broker            - the <code>OpBroker</code> needed to get the projects
    * @param projectChoiceSet  - the <code>XComponent</code> data set of projects from which the project locators are taken
    * @param activityChoiceSet - the <code>XComponent</code> data set of activities which must contain the project's
    *                          activity locators
    * @param resourceChoiceSet - the <code>XComponent</code> data set of resources which must contain the project's
    *                          resource locators.
    */
   public static void configureProjectChoiceMap(OpBroker broker, XComponent projectChoiceSet,
        XComponent activityChoiceSet, XComponent resourceChoiceSet) {
      XComponent projectDataRow;
      XComponent dataCell;
      OpProjectNode project;
      ArrayList<String> resourceList;
      ArrayList<String> activityList;
      boolean existsInResourceChoiceSet;
      boolean existsInResourceChoiceList;
      boolean existsInActivityChoiceSet;
      boolean existsInActivityChoiceList;

      for (int i = 0; i < projectChoiceSet.getChildCount(); i++) {
         resourceList = new ArrayList<String>();
         activityList = new ArrayList<String>();

         projectDataRow = (XComponent) projectChoiceSet.getChild(i);
         project = (OpProjectNode) broker.getObject(XValidator.choiceID(projectDataRow.getStringValue()));

         //fill the resource & activity choice lists for this project
         XComponent resourceRow;
         XComponent activityRow;
         for (OpAssignment assignment : project.getPlan().getActivityAssignments()) {
            existsInResourceChoiceSet = false;
            existsInResourceChoiceList = false;
            existsInActivityChoiceSet = false;
            existsInActivityChoiceList = false;

            //form the resource & activity choices from the assignment
            String resourceChoice = XValidator.choice(assignment.getResource().locator(), assignment.getResource().getName());
            String activityName = generateActivityName(assignment.getActivity());
            String activityChoice = XValidator.choice(assignment.getActivity().locator(), activityName);

            //check if the resource choice exists in the resource choice data set
            for (int j = 0; j < resourceChoiceSet.getChildCount(); j++) {
               resourceRow = (XComponent) resourceChoiceSet.getChild(j);
               if (resourceRow.getStringValue().equals(resourceChoice)) {
                  existsInResourceChoiceSet = true;
                  break;
               }
            }

            //check if the resource choice already exists in the resource choice list
            for (String resourceChoiceListValue : resourceList) {
               if (resourceChoiceListValue.equals(resourceChoice)) {
                  existsInResourceChoiceList = true;
                  break;
               }
            }

            //check if the activity choice exists in the activity choice data set
            for (int j = 0; j < activityChoiceSet.getChildCount(); j++) {
               activityRow = (XComponent) activityChoiceSet.getChild(j);
               if (activityRow.getStringValue().equals(activityChoice)) {
                  existsInActivityChoiceSet = true;
                  break;
               }
            }

            //check if the activity choice already exists in the activity choice list
            for (String activityChoiceListValue : activityList) {
               if (activityChoiceListValue.equals(activityChoice)) {
                  existsInActivityChoiceList = true;
                  break;
               }
            }

            //if the resource choice is present in the resourceChoiceSet and
            //it is not present in the resource choice list add it to the list
            if (existsInResourceChoiceSet && !existsInResourceChoiceList) {
               resourceList.add(resourceChoice);
            }
            //if the activity choice is present in the activityChoiceSet and
            //it is not present in the activity choice list add it to the list
            if (existsInActivityChoiceSet && !existsInActivityChoiceList) {
               activityList.add(activityChoice);
            }
         }

         //0 - list of related activity locators
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setListValue(activityList);
         projectDataRow.addChild(dataCell);
         //1 - list of related resource locators
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setListValue(resourceList);
         projectDataRow.addChild(dataCell);
         //2 - progress tracked
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setBooleanValue(project.getPlan().getProgressTracked());
         projectDataRow.addChild(dataCell);
      }
   }

   /**
    * Adds one cell to each row in the <code>XComponent</code> resourceChoiceSet.
    * - the cell will contain a <code>List</code> with the locators of the activities related to that row's resource.
    *
    * @param resourceChoiceSet - the <code>XComponent</code> data set of resources from which the resource locators are taken
    * @param broker            - the <code>OpBroker</code> needed to get the resources
    * @param activityChoiceSet - the <code>XComponent</code> data set of activities which must contain the resource's
    *                          activity locators
    */
   public static void configureResourceChoiceMap(OpBroker broker, XComponent resourceChoiceSet,
        XComponent activityChoiceSet) {
      XComponent resourceDataRow;
      XComponent dataCell;
      OpResource resource;
      ArrayList<String> activityList;
      boolean existsInActivityChoiceSet;
      boolean existsInActivityChoiceList;

      for (int i = 0; i < resourceChoiceSet.getChildCount(); i++) {
         activityList = new ArrayList<String>();

         resourceDataRow = (XComponent) resourceChoiceSet.getChild(i);
         resource = (OpResource) broker.getObject(XValidator.choiceID(resourceDataRow.getStringValue()));

         //fill the activity choice list for this resource
         XComponent activityRow;
         for (OpAssignment assignment : resource.getActivityAssignments()) {
            existsInActivityChoiceSet = false;
            existsInActivityChoiceList = false;

            //form the activity choice from the assignment
            String activityName = generateActivityName(assignment.getActivity());
            String activityChoice = XValidator.choice(assignment.getActivity().locator(), activityName);

            //check if the activity choice exists in the activity choice data set
            for (int j = 0; j < activityChoiceSet.getChildCount(); j++) {
               activityRow = (XComponent) activityChoiceSet.getChild(j);
               if (activityRow.getStringValue().equals(activityChoice)) {
                  existsInActivityChoiceSet = true;
                  break;
               }
            }

            //check if the activity choice already exists in the activity choice list
            for (String activityChoiceListValue : activityList) {
               if (activityChoiceListValue.equals(activityChoice)) {
                  existsInActivityChoiceList = true;
                  break;
               }
            }

            //if the activity choice is present in the activityChoiceSet and
            //it is not present in the activity choice list add it to the list
            if (existsInActivityChoiceSet && !existsInActivityChoiceList) {
               activityList.add(activityChoice);
            }
         }

         //0 - list of related activity locators
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setListValue(activityList);
         resourceDataRow.addChild(dataCell);
      }
   }

   /**
    * Adds one cell to each row in the <code>XComponent</code> activityChoiceSet.
    * - the cell will contain a <code>List</code> with the locators of the resources related to that row's activity,
    *
    * @param broker            - the <code>OpBroker</code> needed to get the activities
    * @param activityChoiceSet - the <code>XComponent</code> data set of activities from which the activity locators are taken
    * @param resourceChoiceSet - the <code>XComponent</code> data set of resources which must contain the activity's
    *                          resource locators.
    */
   public static void configureActivityChoiceMap(OpBroker broker, XComponent activityChoiceSet,
        XComponent resourceChoiceSet) {
      XComponent activityDataRow;
      XComponent dataCell;
      OpActivity activity;
      ArrayList<String> resourceList;
      boolean existsInResourceChoiceSet;
      boolean existsInResourceChoiceList;

      for (int i = 0; i < activityChoiceSet.getChildCount(); i++) {
         resourceList = new ArrayList<String>();

         activityDataRow = (XComponent) activityChoiceSet.getChild(i);
         activity = (OpActivity) broker.getObject(XValidator.choiceID(activityDataRow.getStringValue()));

         //fill the resource choice list for this activity
         XComponent resourceRow;
         for (OpAssignment assignment : activity.getAssignments()) {
            existsInResourceChoiceSet = false;
            existsInResourceChoiceList = false;

            //form the resource choice from the assignment
            String resourceChoice = XValidator.choice(assignment.getResource().locator(), assignment.getResource().getName());

            //check if the resource choice exists in the resource choice data set
            for (int j = 0; j < resourceChoiceSet.getChildCount(); j++) {
               resourceRow = (XComponent) resourceChoiceSet.getChild(j);
               if (resourceRow.getStringValue().equals(resourceChoice)) {
                  existsInResourceChoiceSet = true;
                  break;
               }
            }

            //check if the resource choice already exists in the resource choice list
            for (String resourceChoiceListValue : resourceList) {
               if (resourceChoiceListValue.equals(resourceChoice)) {
                  existsInResourceChoiceList = true;
                  break;
               }
            }

            //if the resource choice is present in the resourceChoiceSet and
            //it is not present in the resource choice list add it to the list
            if (existsInResourceChoiceSet && !existsInResourceChoiceList) {
               resourceList.add(resourceChoice);
            }
         }

         //2 - list of related resource locators
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setListValue(resourceList);
         activityDataRow.addChild(dataCell);
      }
   }

   /**
    * Returns a <code>List</code> which contains the ids of the resources for which the current user is responsible.
    *
    * @param session - the <code>OpProjectSession</code> needed to get the current user id
    * @param broker  - the <code>OpBroker</code> needed to get the resource ids
    * @return - a <code>List</code> which contains the ids of the resources for which the current user is responsible.
    */
   public static List getListOfSubordinateResourceIds(OpProjectSession session, OpBroker broker) {
      List resourceIds = new ArrayList();
      OpQuery query = broker.newQuery("select resource from OpResource as resource where resource.User.ID = ?");
      query.setLong(0, session.getUserID());
      Iterator result = broker.iterate(query);

      //fill the list of resource ids
      while (result.hasNext()) {
         OpResource resource = (OpResource) result.next();
         resourceIds.add(resource.getID());
      }
      return resourceIds;
   }

   /**
    * Fills the <code>XComponent</code> data set passed as parameter with project nodes information
    *
    * @param projectDataSet - the <code>XComponent</code> data set to be filled
    * @param assignmentList - the <code>List</code> of assignments from which the information about the projects is extracted
    */
   public static void fillProjectSet(XComponent projectDataSet, List<OpAssignment> assignmentList) {

      XComponent projectSet = new XComponent(XComponent.DATA_SET);
      for (OpAssignment assignment : assignmentList) {
         OpProjectNode projectNode = assignment.getProjectPlan().getProjectNode();
         String choice = XValidator.choice(projectNode.locator(), projectNode.getName());
         if (!projectSet.contains(-1, choice)) {
            XComponent row = new XComponent(XComponent.DATA_ROW);
            row.setStringValue(choice);
            projectSet.addChild(row);
         }
      }
      //sort the projects set and add it to the projectDataSet
      projectSet.sort();
      projectDataSet.addAllRows(projectSet);
   }

   /**
    * Adds to each choice set: the project/activity/resource choice set, a row with the project/activity/resource
    * entity choice obtained from the assignment parameter.
    * The activity choice set row will contain a data cell with the type of activity and a data cell with
    * the map of costs for that activity.
    * The map will contain the following information:
    * Key - cost type
    * Value - a <code>List</code> with the base cost value, the remaining cost value and a boolean
    * value indicating if fot this type of cost the user entered a remaining cost by hand
    *
    * @param choiceProjectSet  - the <code>XComponent</code> choice project set
    * @param choiceActivitySet - the <code>XComponent</code> choice activity set
    * @param choiceResourceSet - the <code>XComponent</code> choice resource set
    * @param assignment        - the <code>OpAssignment</code> entity from which the entity choices are taken.
    */
   public static void fillChoiceDataSetsFromSingleAssignment(OpAssignment assignment, XComponent choiceProjectSet,
        XComponent choiceActivitySet, XComponent choiceResourceSet) {
      OpActivity activity = assignment.getActivity();
      OpResource resource = assignment.getResource();
      OpProjectNode project = assignment.getProjectPlan().getProjectNode();

      if (!choiceProjectSet.contains(-1, XValidator.choice(project.locator(), project.getName()))) {
         //add the project choice to the project choice set
         XComponent projectRow = new XComponent(XComponent.DATA_ROW);
         projectRow.setStringValue(XValidator.choice(project.locator(), project.getName()));
         choiceProjectSet.addChild(projectRow);
      }

      String generatedActvityName = generateActivityName(activity);
      if (!choiceActivitySet.contains(-1, XValidator.choice(activity.locator(), generatedActvityName))) {
         //fill the choice activity set from the assignments
         XComponent activityRow = new XComponent(XComponent.DATA_ROW);
         activityRow.setValue(XValidator.choice(activity.locator(), generatedActvityName));
         choiceActivitySet.addChild(activityRow);

         //0 - activity type
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setByteValue(activity.getType());
         activityRow.addChild(dataCell);

         //form the map of costs for this activity
         Map<Byte, List> costsMap = new HashMap<Byte, List>();

         List travelCostInfo = new ArrayList();
         travelCostInfo.add(activity.getBaseTravelCosts());
         travelCostInfo.add(activity.getRemainingTravelCosts());
         travelCostInfo.add(new Boolean(false));
         costsMap.put(OpCostRecord.TRAVEL_COST, travelCostInfo);

         List materialCostInfo = new ArrayList();
         materialCostInfo.add(activity.getBaseMaterialCosts());
         materialCostInfo.add(activity.getRemainingMaterialCosts());
         materialCostInfo.add(new Boolean(false));
         costsMap.put(OpCostRecord.MATERIAL_COST, materialCostInfo);

         List externalCostInfo = new ArrayList();
         externalCostInfo.add(activity.getBaseExternalCosts());
         externalCostInfo.add(activity.getRemainingExternalCosts());
         externalCostInfo.add(new Boolean(false));
         costsMap.put(OpCostRecord.EXTERNAL_COST, externalCostInfo);

         List miscCostInfo = new ArrayList();
         miscCostInfo.add(activity.getBaseMiscellaneousCosts());
         miscCostInfo.add(activity.getRemainingMiscellaneousCosts());
         miscCostInfo.add(new Boolean(false));
         costsMap.put(OpCostRecord.MISCELLANEOUS_COST, miscCostInfo);

         //1 - map of cost types -> list(base costs, remaining costs, remaining cost modified by user)
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setValue(costsMap);
         activityRow.addChild(dataCell);
      }

      if (!choiceResourceSet.contains(-1, XValidator.choice(resource.locator(), resource.getName()))) {
         //fill the choice resource set from the assignments
         XComponent resourceRow = new XComponent(XComponent.DATA_ROW);
         resourceRow.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
         choiceResourceSet.addChild(resourceRow);
      }
   }

   /**
    * Generates a string representing the name of an activity, including the name of all
    * its parents.
    *
    * @param activity an <code>OpActivity</code>
    * @return a <code>String</code> the name of the activity, including the name
    *         of all it's parents
    */
   static String generateActivityName(OpActivity activity) {
      String name = activity.getName();
      if (name == null) {
         name = "";
      }
      StringBuffer nameBuffer = new StringBuffer(name);
      boolean hasParent = activity.getSuperActivity() != null;
      if (hasParent) {
         nameBuffer.append(" (");
      }
      while (activity.getSuperActivity() != null) {
         nameBuffer.append(activity.getSuperActivity().getName());
         activity = activity.getSuperActivity();
         if (activity.getSuperActivity() != null) {
            nameBuffer.append(" - ");
         }
      }
      if (hasParent) {
         nameBuffer.append(")");
      }
      return nameBuffer.toString();
   }

   /**
    * Creates a <code>Map</code>: Key:assignment.activity choice - assignment.resource choice
    * Value: List - 0 - assignment's base effort
    * 1 - assignment's locator
    * 2 - assignment' remaining effort
    * 3 - boolean value indicating if the remaining effort was modified
    * manually by the user
    *
    * @param assignmentList - the <code>List</code> of <code>OpAssignment</code> entities from which the map will be created.
    * @return a <code>Map</code>: Key:assignment.activity choice - assignment.resource choice
    *         Value: List - 0 - assignment's base effort
    *         1 - assignment's locator
    *         2 - assignment' remaining effort
    *         3 - boolean value indicating if the remaining effort was modified
    *         manually by the user
    */
   public static Map<String, List> createAssignmentMap(List<OpAssignment> assignmentList) {
      Map<String, List> assignmentMap = new HashMap<String, List>();
      List dataList;
      OpActivity activity;
      OpResource resource;

      for (OpAssignment assignment : assignmentList) {
         activity = assignment.getActivity();
         resource = assignment.getResource();

         dataList = new ArrayList();
         dataList.add(assignment.getBaseEffort());
         dataList.add(assignment.locator());
         dataList.add(assignment.getRemainingEffort());
         dataList.add(false);
         String activityName = generateActivityName(activity);
         assignmentMap.put(XValidator.choice(activity.locator(), activityName) + "-" +
              XValidator.choice(resource.locator(), resource.getName()), dataList);
      }

      return assignmentMap;
   }

   /**
    * Filters each row of the data set parameter using the locator of the work record assignment
    *
    * @param dataSet     - the <code>XComponent</code> data set that's being filtered
    * @param workDataRow - the <code>XComponent</code> data row from which the assignment locator is obtained
    * @return a <code>XComponent</code> data set that is a subset of the data set passed as parameter and
    *         has the <code>OpWorkRecord's</code> assignment locator set on all of it's rows.
    */
   private static XComponent filterDataSet(XComponent dataSet, XComponent workDataRow) {
      XComponent subset = new XComponent(XComponent.DATA_SET);
      XComponent dataRow;

      for (int j = 0; j < dataSet.getChildCount(); j++) {
         dataRow = (XComponent) dataSet.getChild(j);
         String assignmentLocator = dataRow.getStringValue();
         if (workDataRow.getStringValue().equals(assignmentLocator)) {
            subset.addChild(dataRow);
         }
      }

      return subset;
   }

   /**
    * Creates an ordering criteria that is used throughout the workslip code.
    *
    * @return a <code>OpObjectOrderCriteria</code>.
    */
   public static OpObjectOrderCriteria createActivityOrderCriteria() {
      // Configure activity sort order
      Map<String, String> sortOrders = new HashMap<String, String>(2);
      sortOrders.put(OpActivity.START, OpObjectOrderCriteria.ASCENDING);
      sortOrders.put(OpActivity.PRIORITY, OpObjectOrderCriteria.ASCENDING);
      return  new OpObjectOrderCriteria(OpActivity.ACTIVITY, sortOrders);
   }
}
