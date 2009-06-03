/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
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
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityIfc;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAssignmentIfc;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.work.validators.OpWorkEffortValidator;
import onepoint.project.util.OpBulkFetchIterator;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.validators.OpProjectValidator;

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

//   public final static Map<Integer, String> workSlipStates = new HashMap<Integer, String>();
//   public final static Map<String, Integer> workSlipStatesReversed = new HashMap<String, Integer>();

//   private final static String STATE_EDITABLE = "editable";
//   private final static String STATE_LOCKED = "locked";
//   private final static String STATE_APPROVED = "approved";

   private static OpWorkSlipDataSetFactory instance = new OpWorkSlipDataSetFactory();

//   static {
//      workSlipStates.put(0, STATE_EDITABLE);
//      workSlipStates.put(1, STATE_LOCKED);
//      workSlipStates.put(2, STATE_APPROVED);
//
//      workSlipStatesReversed.put(STATE_EDITABLE, 0);
//      workSlipStatesReversed.put(STATE_LOCKED, 1);
//      workSlipStatesReversed.put(STATE_APPROVED, 2);
//   }


   /**
    * Utility class.
    */
   public OpWorkSlipDataSetFactory() {
   }

   /**
    * Returns an instance of the OpProjectPlanningService
    * 
    * @return an instance of the OpProjectPlanningService
    */
   public static void register(OpWorkSlipDataSetFactory dataSetFactory) {
      instance = dataSetFactory;
   }

   /**
    * Returns an instance of the data set factory
    * 
    * @return an instance of the data set factory
    */
   public static OpWorkSlipDataSetFactory getInstance() {
      return instance;
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
   public static Iterator<Object[]> getAssignments(OpBroker broker, Set resourceIds, List activityTypes,
        Date start, OpObjectOrderCriteria activityOrderCriteria, long projectNodeId, boolean allowArchivedProjects) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("select assignment.id from OpAssignment as assignment inner join assignment.Activity as activity ");
      buffer.append("where assignment.Resource.id in (:resourceIds) and assignment.Complete < 100 and activity.Type in (:type) and activity.Deleted = false ");
      if (start != null) {
         buffer.append(" and (activity.Start < :startBefore or activity.Start is null) ");
      }
      if (!allowArchivedProjects) {
         buffer.append(" and activity.ProjectPlan.ProjectNode.Archived=false ");
      }
      if (projectNodeId != ALL_PROJECTS_ID) {
         buffer.append(" and assignment.ProjectPlan.ProjectNode.id = :projectNodeId");
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
      Set<Long> ids = new HashSet<Long>();
      Iterator<Long> idit = broker.iterate(query);
      while (idit.hasNext()) {
         ids.add(idit.next());
      }
      
      idit = ids.iterator(); 
      OpBulkFetchIterator<OpAssignment, Long> bit = new OpBulkFetchIterator<OpAssignment, Long>(
            broker, idit,
            broker.newQuery(
                  		"select ass from " +
                  		" OpAssignment as ass " +
                  		" left join fetch ass.Activity as act " +
                  		" left join fetch act.Actions as a " +
                  		"where ass.id in (:bulk_ids)"),
            new OpBulkFetchIterator.LongIdConverter(), "bulk_ids");


      List<Object[]> result = new ArrayList<Object[]>();
      List<Object[]> adHocActivities = new ArrayList<Object[]>();
      while (bit.hasNext()) {
         Object[] record = new Object[2];
         OpAssignment ass = bit.next(); 
         OpActivity act = ass.getActivity();
         record[0] = ass;
         record[1] = act;
         if (act.getType() == OpActivity.ADHOC_TASK) {
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
            if (superActivity.getId() == activity.getSuperActivity().getId()) {
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
   public static List<OpWorkRecord> formWorkRecordsFromDataSets(
         OpBroker broker, XComponent workEffortDataSet,
         XComponent timeRecordDataSet, XComponent costRecordDataSet,
         Map<XComponent, List<OpAttachment>> unmodifiedAttachmentsMap) {
      List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();
      XComponent workDataRow;
      List<XComponent> costRecordRowsList;

      for (int i = 0; i < workEffortDataSet.getChildCount(); i++) {
         //obtain the work records entity from the work effort row
         workDataRow = (XComponent) workEffortDataSet.getChild(i);
         OpWorkRecord workRecord = OpWorkEffortDataSetFactory.createWorkEntity(broker, workDataRow);

         //obtain the sub data set of time records which belong to the work record
         XComponent timeRecordSubset = filterDataSet(timeRecordDataSet, workDataRow);
         workRecord.addTimeRecords(OpTimeRecordDataSetFactory.createTimeRecords(timeRecordSubset));

         //obtain the sub data set of cost records which belong to the work record
         XComponent costRecordSubset = filterDataSet(costRecordDataSet, workDataRow);

         workRecord.addCostRecords(OpCostRecordDataSetFactory.createCostRecords(broker, costRecordSubset, unmodifiedAttachmentsMap));

         workRecords.add(workRecord);
      }

      /*if there are any cost records that do not belong to any work records create "empty" work records for
        these cost records*/
      List<OpWorkRecord> emptyWorkRecords = new ArrayList<OpWorkRecord>();
      boolean existsEmptyWorkRecord;
      List<XComponent> rows = costRecordDataSet.asList();
      int emptyStartindex = workRecords.size();
      for (XComponent costDataRow : rows) {
         existsEmptyWorkRecord = false;
         //form a set with the cost record form this row
         XComponent tempDataSet = new XComponent();
         costRecordDataSet.removeChild(costDataRow);
         tempDataSet.addChild(costDataRow);
         
         String assignmentLoc = costDataRow.getStringValue();
         Set<OpCostRecord> costRecordSet = OpCostRecordDataSetFactory.createCostRecords(broker, tempDataSet, unmodifiedAttachmentsMap);

         int emptyWRPos = 0;
         for (int i = 0; i < emptyWorkRecords.size(); i++) {
            OpWorkRecord emptyWorkRecord = emptyWorkRecords.get(i);
            //if there already is an "empty" work record for the cost row's assignment
            if (assignmentLoc.equals(emptyWorkRecord.getAssignment().locator())) {
               existsEmptyWorkRecord = true;
               emptyWorkRecord.getCostRecords().addAll(costRecordSet);
               //set the work record on each cost record
               for (OpCostRecord costRecord : costRecordSet) {
                  costRecord.setWorkRecord(emptyWorkRecord);
               }
               emptyWRPos = i;
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
            workRecord.addCostRecords(costRecords);
            workRecords.add(workRecord);
            emptyWRPos = emptyWorkRecords.size();
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
    * @param broker      - the <code>OpBroker</code> needed to perform the DB operations
    * @return a <code>List</code> of <code>XComponent</code> data sets formed from a <code>List</code>
    *         of <code>OpWorkRecord</code> entities. The list will contain:
    *         a set containing information about the work records
    *         a set containing information about the time records
    *         a set containing information about the cost records
    */
   public static List<XComponent> formDataSetsFromWorkRecords(List<OpWorkRecord> workRecords, OpProjectSession session,
        OpBroker broker) {
      List<XComponent> dataSetList = new ArrayList<XComponent>();
      XComponent workRecordDataSet = new XComponent(XComponent.DATA_SET);
      XComponent timeRecordDataSet = new XComponent(XComponent.DATA_SET);
      XComponent costRecordDataSet = new XComponent(XComponent.DATA_SET);

      for (OpWorkRecord workRecord : workRecords) {

         //add this entity in the work effort data set only if it is not an "empty" work record
         if (!workRecord.isEmpty()) {
            //form the work record data set
            workRecordDataSet.addChild(OpWorkEffortDataSetFactory.getInstance().createEffortRow(workRecord));
         }

         //add the time record subset of this work record to the final time record data set
         XComponent timeRecordSubset = OpTimeRecordDataSetFactory.getTimeDataSetForWorkRecord(workRecord);
         List<XComponent> rows = timeRecordSubset.asList();
         for (XComponent row : rows) {
            timeRecordSubset.removeChild(row);
            timeRecordDataSet.addChild(row);
         }

         //add the cost record subset of this work record to the final cost record data set
         XComponent costRecordSubset = OpCostRecordDataSetFactory.getCostDataSetForWorkRecord(workRecord, session, broker);
         rows = costRecordSubset.asList();
         for (XComponent row : rows) {
            costRecordSubset.removeChild(row);
            costRecordDataSet.addChild(row);
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
            String rowAssignmentLoc = dataRow.getStringValue();
            if (rowAssignmentLoc == null || XValidator.choiceID(rowAssignmentLoc).equals(assignment.locator())) {
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
        XComponent resourceChoiceSet, boolean filterCompleted) {
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
         for (OpAssignmentIfc assignment : activity.getAssignments()) {
            // opp 761 - do not add completed assignments
            if (filterCompleted && assignment.getComplete() == 100d) { // completed
               continue;
            }

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
   public static Set<Long> getListOfSubordinateResourceIds(OpProjectSession session, OpBroker broker) {
      Set resourceIds = new HashSet();
      OpQuery query = broker.newQuery("select resource from OpResource as resource where resource.User.id = ?");
      query.setLong(0, session.getUserID());
      Iterator result = broker.iterate(query);

      //fill the list of resource ids
      while (result.hasNext()) {
         OpResource resource = (OpResource) result.next();
         resourceIds.add(new Long(resource.getId()));
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

         // #0: sort criteria:
         XComponent sortColumnCell = new XComponent(XComponent.DATA_CELL);
         sortColumnCell.setStringValue(project.getName());
         projectRow.addChild(sortColumnCell);
         
         choiceProjectSet.addChild(projectRow);
      }

      String generatedActvityName = generateActivityName(activity);
      if (!choiceActivitySet.contains(-1, XValidator.choice(activity.locator(), generatedActvityName))) {
         //fill the choice activity set from the assignments
         XComponent activityRow = new XComponent(XComponent.DATA_ROW);
         activityRow.setValue(XValidator.choice(activity.locator(), generatedActvityName));

         // #0 - activity type
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setByteValue(activity.getType());
         activityRow.addChild(dataCell);

         //form the map of costs for this activity
         Map<Byte, List> costsMap = new HashMap<Byte, List>();

         List travelCostInfo = new ArrayList();
         travelCostInfo.add(activity.getBaseTravelCosts());
         travelCostInfo.add(activity.getRemainingTravelCosts());
         travelCostInfo.add(new Boolean(false));
         travelCostInfo.add(activity.getRemainingTravelCosts());
         costsMap.put(OpCostRecord.TRAVEL_COST, travelCostInfo);

         List materialCostInfo = new ArrayList();
         materialCostInfo.add(activity.getBaseMaterialCosts());
         materialCostInfo.add(activity.getRemainingMaterialCosts());
         materialCostInfo.add(new Boolean(false));
         materialCostInfo.add(activity.getRemainingMaterialCosts());
         costsMap.put(OpCostRecord.MATERIAL_COST, materialCostInfo);

         List externalCostInfo = new ArrayList();
         externalCostInfo.add(activity.getBaseExternalCosts());
         externalCostInfo.add(activity.getRemainingExternalCosts());
         externalCostInfo.add(new Boolean(false));
         externalCostInfo.add(activity.getRemainingExternalCosts());
         costsMap.put(OpCostRecord.EXTERNAL_COST, externalCostInfo);

         List miscCostInfo = new ArrayList();
         miscCostInfo.add(activity.getBaseMiscellaneousCosts());
         miscCostInfo.add(activity.getRemainingMiscellaneousCosts());
         miscCostInfo.add(new Boolean(false));
         miscCostInfo.add(activity.getRemainingMiscellaneousCosts());
         costsMap.put(OpCostRecord.MISCELLANEOUS_COST, miscCostInfo);

         // #1 - map of cost types -> list(base costs, remaining costs, remaining cost modified by user)
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setValue(costsMap);
         activityRow.addChild(dataCell);

         // #2: sort criteria:
         XComponent sortColumnCell = new XComponent(XComponent.DATA_CELL);
         sortColumnCell.setIntValue(activity.getSequence());
         activityRow.addChild(sortColumnCell);
         
         choiceActivitySet.addChild(activityRow);
      }

      if (!choiceResourceSet.contains(-1, XValidator.choice(resource.locator(), resource.getName()))) {
         //fill the choice resource set from the assignments
         XComponent resourceRow = new XComponent(XComponent.DATA_ROW);
         resourceRow.setStringValue(XValidator.choice(resource.locator(), resource.getName()));

         // #0: sort criteria:
         XComponent sortColumnCell = new XComponent(XComponent.DATA_CELL);
         sortColumnCell.setStringValue(resource.getName());
         resourceRow.addChild(sortColumnCell);
         
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
    * @param broker 
    * @param session 
    *
    * @param assignmentList - the <code>List</code> of <code>OpAssignment</code> entities from which the map will be created.
    * @return a <code>Map</code>: Key:assignment.activity choice - assignment.resource choice
    *         Value: List - 0 - assignment's base effort
    *         1 - assignment's locator
    *         2 - assignment' remaining effort
    *         3 - boolean value indicating if the remaining effort was modified
    *         manually by the user
    */
   public Map<String, List> createAssignmentMap(OpProjectSession session, OpBroker broker, List<OpAssignment> assignmentList) {
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
         dataList.add(getActionStatus(activity));
         String activityName = generateActivityName(activity);
         assignmentMap.put(XValidator.choice(activity.locator(), activityName) + "-" +
              XValidator.choice(resource.locator(), resource.getName()), dataList);
      }

      return assignmentMap;
   }

   /**
    * @param activity
    * @return
    * @pre
    * @post
    */
   public int getActionStatus(OpActivity activity) {
      return 0;
   }

//   /**
//    * @param assignment
//    * @return
//    * @pre
//    * @post
//    */
//   private static Object getActionsStatus(OpAssignmentIfc assignment) {
//      // TODO Auto-generated method stub
//      return null;
//   }

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

      List rows = dataSet.asList();
      for (int j = 0; j < rows.size(); j++) {
         dataRow = (XComponent) rows.get(j);
         String assignmentLocator = dataRow.getStringValue();
         if (workDataRow.getStringValue().equals(assignmentLocator)) {
            dataSet.removeChild(dataRow);
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
      SortedMap<String, Integer> sortOrders = new TreeMap<String, Integer>();
      sortOrders.put(OpActivity.START, OpObjectOrderCriteria.ASCENDING);
      sortOrders.put(OpActivity.PRIORITY, OpObjectOrderCriteria.ASCENDING);
      return new OpObjectOrderCriteria(OpActivity.class, sortOrders);
   }

   /**
    * @param userID   user to fill the work slips for
    * @param time     start period for workslips
    * @param data_set work slip data set
    * @param broker   current broker
    */
   public static void fillWorkSlipsDataSet(Iterator work_slips, XComponent data_set, XComponent rowMap, XComponent weekdaysDataSet) {
      OpWorkSlip work_slip;

      XComponent data_cell;
      while (work_slips.hasNext()) {
         work_slip = (OpWorkSlip) (work_slips.next());
         XComponent data_row = new XComponent(XComponent.DATA_ROW);
         
         Calendar c = OpProjectCalendar.getDefaultCalendar().cloneCalendarInstance();
         c.setTimeInMillis(work_slip.getDate().getTime());

         Map<String, Object> objects = new HashMap<String, Object>();
         objects.put("workslip", work_slip);
         objects.put("weekday",  OpProjectValidator.initChoiceValue(weekdaysDataSet, Integer.toString(c.get(Calendar.DAY_OF_WEEK))));
         OpProjectValidator.populateDataRowFromMap(data_row, rowMap, objects, false);
         data_set.addChild(data_row);
      }
   }

   /**
    * Adds the work record information that is "pre-filled" by the system (for effort and time records).
    *
    * @param broker current broker
    * @param activityRows activity rows that have to be pre-filled
    * @param workEffortDataSet effort data set
    * @param workTimeDataSet time data set
    * @param resourceIds 
    */
   public static void addPrefilledAssignments(OpBroker broker, List<XComponent> activityRows, XComponent workEffortDataSet, XComponent workTimeDataSet, Set<Long> resourceIds) {

      boolean timeTrackingEnabled = false;
      String timeTracking = OpSettingsService.getService().getStringValue(broker, OpSettings.ENABLE_TIME_TRACKING);
      if (timeTracking != null) {
         timeTrackingEnabled = Boolean.valueOf(timeTracking);
      }

      for (XComponent activityRow : activityRows) {
         String activityLocator = activityRow.getStringValue();
         List<String> resources = OpGanttValidator.getResources(activityRow);
         for (String resourceLocator : resources) {
            if (!resourceIds.contains(new Long(OpLocator.parseLocator(resourceLocator).getID()))) {
               continue;
            }
            OpActivityIfc activity = (OpActivityIfc) broker.getObject(activityLocator);
            if (activity instanceof OpActivityVersion) {
            	activity = activity.getActivity();
            }
            OpQuery query = broker.newQuery("select assignment from OpAssignment assignment where assignment.Activity = :activity and assignment.Resource.id = :resourceID");
            query.setParameter("activity", activity);
            query.setLong("resourceID", OpLocator.parseLocator(resourceLocator).getID());
            Iterator iterator = broker.iterate(query);
            if (iterator.hasNext()) {
               OpAssignment assignment = (OpAssignment) iterator.next();
               if (!workEffortDataSet.contains(-1, assignment.locator()) && !assignment.isCompleted()) {

                  if (timeTrackingEnabled && assignment.getActivity().isTimeTrackable()) {
                     XComponent timeRow = OpTimeRecordDataSetFactory.createTimeRowFromAssignment(assignment);
                     workTimeDataSet.addChild(timeRow);
                  }

                  XComponent effortRow = OpWorkEffortDataSetFactory.getInstance().createEffortRowFromAssignment(assignment);
                  workEffortDataSet.addChild(effortRow);

               }
            }
         }
      }
   }

   public static void sortChoiceDataSetByColumn(
         XComponent choiceDataSet, int column, Integer order) {
      
      final boolean asc = order > 0;
      SortedMap<Object, List<XComponent>> sortedRows = new TreeMap<Object, List<XComponent>>(new Comparator<Object>() {

         public int compare(Object o1, Object o2) {
            int result = 0;
            if (o1 instanceof String) {
               String s1 = (String) o1;
               String s2 = (String) o2;
               result = s1.compareToIgnoreCase(s2);
            }
            else if (o1 instanceof Integer) {
               Integer i1 = (Integer) o1;
               Integer i2 = (Integer) o2;
               result = i1.compareTo(i2);
            }
            else if (o1 instanceof Date) {
               Date d1 = (Date) o1;
               Date d2 = (Date) o2;
               result = d1.compareTo(d2);
            }
            else if (o1 instanceof Double) {
               Double dbl1 = (Double) o1;
               Double dbl2 = (Double) o2;
               result = dbl1.compareTo(dbl2);
            }
            return asc ? result : - result;
         }
      });

      for (int i = 0; i < choiceDataSet.getChildCount(); i++) {
         XComponent row = (XComponent) choiceDataSet.getChild(i);
         XComponent orderColumn = (XComponent) row.getChild(column);
         Object key = orderColumn.getValue();
         List<XComponent> rowsForKey = sortedRows.get(key);
         if (rowsForKey == null) {
            rowsForKey = new ArrayList<XComponent>();
            sortedRows.put(key, rowsForKey);
         }
         rowsForKey.add(row);
      }
      choiceDataSet.removeAllChildren();
      
      Iterator<Object> it = sortedRows.keySet().iterator();
      while (it.hasNext()) {
         Iterator<XComponent> rowIt = sortedRows.get(it.next()).iterator();
         while (rowIt.hasNext()) {
            choiceDataSet.addChild(rowIt.next());
         }
      }
   }
}
