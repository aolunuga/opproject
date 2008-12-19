/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work;

import java.util.List;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityIfc;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.validators.OpWorkEffortValidator;

/**
 * Helper class needed to form OpWorkRecord entities from data sets and vice versa
 *
 * @author florin.haizea
 */
public class OpWorkEffortDataSetFactory {
   
   private static OpWorkEffortDataSetFactory instance = new OpWorkEffortDataSetFactory();

   /**
    * Returns an instance of the OpProjectPlanningService
    * 
    * @return an instance of the OpProjectPlanningService
    */
   public static void register(OpWorkEffortDataSetFactory dataSetFactory) {
      instance = dataSetFactory;
   }

   public static OpWorkEffortDataSetFactory getInstance() {
      return instance;
   }

   /**
    * Creates a <code>XComponent</code> data row with the cell's values set from the <code>OpWorkRecord</code> entity
    *
    * @param workRecord - the <code>OpWorkRecord</code> entity whose atributes will be set on the data row
    * @return a data row with the cell's values set from the <code>OpWorkRecord</code> entity
    */
   public XComponent createEffortRow(OpWorkRecord workRecord) {
      OpAssignment assignment = workRecord.getAssignment();
      return createRow(workRecord, assignment);
   }

   private XComponent createRow(OpWorkRecord workRecord, OpAssignment assignment) {

      OpActivity activity = assignment.getActivity();
      OpProjectPlan plan = activity.getProjectPlan();
      OpProjectNode project = plan.getProjectNode();
      OpResource resource = assignment.getResource();

      OpWorkEffortValidator effortValidator = new OpWorkEffortValidator();
      XComponent dataRow = effortValidator.newDataRow();
      XComponent dataCell;

      //0 - set the name of the project
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.PROJECT_NAME_INDEX);
      dataCell.setStringValue(XValidator.choice(project.locator(), project.getName()));

      //1 - set the name of the activity
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTIVITY_NAME_INDEX);
      String activityName = OpWorkSlipDataSetFactory.generateActivityName(activity);
      dataCell.setStringValue(XValidator.choice(activity.locator(), activityName));

      //2 - set the name of the resource
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.RESOURCE_NAME_INDEX);
      dataCell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));

      //3 - set the planned effort
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.PLANNED_EFFORT_INDEX);
      if (activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setValue(null);
      }
      else {
         dataCell.setDoubleValue(assignment.getBaseEffort());
      }
      dataCell.setEnabled(false);

      //4 - set the actual effort
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX);
      if (activity.getType() == OpActivity.MILESTONE) {
         dataCell.setValue(null);
         dataCell.setEnabled(false);
      }
      else {
         if (workRecord != null) {
            dataCell.setDoubleValue(workRecord.getActualEffort());
         }
         else {
            dataCell.setDoubleValue(0);
         }
      }

      //5 - set the remaining effort
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.REMAINING_EFFORT_INDEX);
      if (workRecord != null) {
         dataCell.setDoubleValue(workRecord.getRemainingEffort());
      }
      else {
         dataCell.setDoubleValue(assignment.getRemainingEffort());
      }

      boolean progressTracked = plan.getProgressTracked();
      if (!progressTracked || activity.getType() == OpActivity.MILESTONE || activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setValue(null);
         dataCell.setEnabled(false);
      }

      //6 - set completed
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.COMPLETED_INDEX);
      if (assignment.getProjectPlan().getProgressTracked() || activity.getType() == OpActivity.ADHOC_TASK) {
         if (workRecord != null) {
            dataCell.setBooleanValue(workRecord.getCompleted());
         }
         else {
            dataCell.setBooleanValue(false);
         }
         int actionsStatus = getActionStatus(activity);
         dataCell.setEnabled(actionsStatus <= OpGanttValidator.NO_ACTIONS || actionsStatus >= OpGanttValidator.DONE);
      }
      else {
         dataCell.setValue(null);
         dataCell.setEnabled(false);
      }

      //7 - set comment
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.COMMENTS_INDEX);
      if (workRecord != null) {
         dataCell.setStringValue(workRecord.getComment());
      }

      //8 - set the original remaining
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ORIGINAL_REMAINING_INDEX);
      dataCell.setDoubleValue(assignment.getRemainingEffort());

      //9 - set the activity type
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTIVITY_TYPE_INDEX);
      dataCell.setByteValue(activity.getType());

      //10 - set the activity created status
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTIVITY_CREATED_INDEX);
      dataCell.setBooleanValue(true);

      //11 - set the actual + remaining sum
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTUAL_REMAINING_SUM_INDEX);
      if (workRecord != null) {
         dataCell.setDoubleValue(workRecord.getActualEffort() + workRecord.getRemainingEffort());
      }
      else {
         dataCell.setDoubleValue(assignment.getBaseEffort());
      }
      if (!progressTracked || activity.getType() == OpActivity.MILESTONE || activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setValue(null);
      }

      //13
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTIONS_STATUS);
      dataCell.setIntValue(getActionStatus(activity));
      
      //set the activity's assignment on the dataRow
      dataRow.setValue(assignment.locator());

      return dataRow;
   }

   /**
    * @param activity 
    * @return
    * @pre
    * @post
    */
   protected int getActionStatus(OpActivity activity) {
      return 0;
   }

   /**
    * Creates an <code>OpWorkRecord</code> entity from the data row's cell's values
    *
    * @param dataRow - the <code>XComponent</code> data row whose cell's values will be set on the entity
    * @param broker  - the broker needed to get the assignment
    * @return an <code>OpWorkRecord</code> entity created from the data row's cell's values
    */
   public static OpWorkRecord createWorkEntity(OpBroker broker, XComponent dataRow) {
      OpWorkRecord workRecord = new OpWorkRecord();
      OpAssignment assignment = (OpAssignment) broker.getObject(dataRow.getStringValue());

      //set the assignment
      workRecord.setAssignment(assignment);

      //set the actual effort
      Double actualValue = (Double) ((XComponent) dataRow.getChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX)).getValue();
      double actualEffort;
      if (actualValue == null) {
         actualEffort = 0;
      }
      else {
         actualEffort = actualValue;
      }
      workRecord.setActualEffort(actualEffort);
      double remainingEffort;

      Double remainingValue = (Double) ((XComponent) dataRow.getChild(OpWorkEffortValidator.REMAINING_EFFORT_INDEX)).getValue();
      if (remainingValue == null) {
         remainingEffort = 0;
      }
      else {
         remainingEffort = remainingValue;
      }

      //set the remaining effort
      workRecord.setRemainingEffort(remainingEffort);

      //set completed
      if (((XComponent) dataRow.getChild(OpWorkEffortValidator.COMPLETED_INDEX)).getValue() != null) {
         workRecord.setCompleted(((XComponent) dataRow.getChild(OpWorkEffortValidator.COMPLETED_INDEX)).getBooleanValue());
      }
      //set comments
      workRecord.setComment(((XComponent) dataRow.getChild(OpWorkEffortValidator.COMMENTS_INDEX)).getStringValue());

      return workRecord;
   }

   /**
    * Fills a project choice set, an activity choice set and a resource choice set with information from the
    * assignmentList. Each row in the data sets will have it's value set to the choice of the entity.
    * The activity choice set rows will contain a data cell with the type of activity and a data cell with
    * the list of costs for that activity.
    *
    * @param choiceProjectSet  - the <code>XComponent</code> choice project set
    * @param choiceActivitySet - the <code>XComponent</code> choice activity set
    * @param choiceResourceSet - the <code>XComponent</code> choice resource set
    * @param assignmentList    - the <code>List</code> of assignments
    * @param timeTrackingOn    - a <code>boolean</code> value indicating the type of activities that should be filtered out
    *                          If time tracking is on: take into consideration only activities of type milestone;
    *                          If time tracking is off: take into consideration only activities which are not of type milestone;
    */
   public static void fillChoiceDataSets(XComponent choiceProjectSet, XComponent choiceActivitySet, XComponent choiceResourceSet,
        List<OpAssignment> assignmentList, boolean timeTrackingOn) {
      OpActivityIfc activity;

      for (OpAssignment assignment : assignmentList) {
         activity = assignment.getActivity();

         boolean progressTracked = activity.getProjectPlan().getProgressTracked();
         //filter out milestones when progress tracking is off
         if (!progressTracked && activity.getType() == OpActivity.MILESTONE) {
            continue;
         }
         else {
            //filter out milestones when time tracking is off && filter out other activity types when time tracking is on
            if (timeTrackingOn && (activity.getType() != OpActivity.MILESTONE)) {
               continue;
            }
         }

         OpWorkSlipDataSetFactory.fillChoiceDataSetsFromSingleAssignment(assignment, choiceProjectSet,
              choiceActivitySet, choiceResourceSet);
      }
      //sort the project & resources data-sets ascending after name
      choiceProjectSet.sort();
      choiceResourceSet.sort();
   }

   /**
    * Creates a <code>XComponent</code> effort data row with the cell's values set from the given assignment.
    *
    * @param assignment - the <code>OpAssignment</code> entity whose atributes will be set on the data row
    * @return a data row with the work effort information related to the given assignment
    */
   public XComponent createEffortRowFromAssignment(OpAssignment assignment) {
      return createRow(null, assignment);
   }

   /**
    * Disable the work data set when time tracking is on.
    *
    * @param workEffortDataSet Effort data set.
    */
   public static void disableDataSetForTimeTracking(XComponent workEffortTable) {
      //disable non-milestone activities on data set
      XComponent workEffortDataSet = workEffortTable.getDataSetComponent();
      for (int i = 0; i < workEffortDataSet.getChildCount(); i++) {
         XComponent row = (XComponent) workEffortDataSet.getChild(i);
         if (!((XComponent) row.getChild(OpWorkEffortValidator.ACTIVITY_TYPE_INDEX)).getValue().equals(OpGanttValidator.MILESTONE)) {
            ((XComponent) row.getChild(OpWorkEffortValidator.ENABLED_INDEX)).setValue(Boolean.FALSE);
         }

         //<FIXME author="Mihai Costin" description="This is hiding a milestone related bug! This code should be inside the previous if">
         row.getChild(OpWorkEffortValidator.PROJECT_NAME_INDEX).setEnabled(false);
         row.getChild(OpWorkEffortValidator.ACTIVITY_NAME_INDEX).setEnabled(false);
         row.getChild(OpWorkEffortValidator.RESOURCE_NAME_INDEX).setEnabled(false);
         row.getChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX).setEnabled(false);
         //</FIXME>

      }
   }
}
