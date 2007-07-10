/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.validators.OpWorkEffortValidator;

import java.util.List;

/**
 * Helper class needed to form OpWorkRecord entities from data sets and vice versa
 *
 * @author florin.haizea
 */
public class OpWorkEffortDataSetFactory {

   /**
    * Creates a <code>XComponent</code> data row with the cell's values set from the <code>OpWorkRecord</code> entity
    *
    * @param workRecord - the <code>OpWorkRecord</code> entity whose atributes will be set on the data row
    * @return a data row with the cell's values set from the <code>OpWorkRecord</code> entity
    */
   public static XComponent createEffortRow(OpWorkRecord workRecord) {
      OpWorkEffortValidator effortValidator = new OpWorkEffortValidator();
      XComponent dataRow = effortValidator.newDataRow();
      XComponent dataCell;

      OpActivity activity = workRecord.getAssignment().getActivity();
      OpProjectNode project = activity.getProjectPlan().getProjectNode();
      OpResource resource = workRecord.getAssignment().getResource();

      //0 - set the name of the project
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.PROJECT_NAME_INDEX);
      dataCell.setStringValue(XValidator.choice(project.locator(), project.getName()));

      //1 - set the name of the activity
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTIVITY_NAME_INDEX);
      dataCell.setStringValue(XValidator.choice(activity.locator(), activity.getName()));

      //2 - set the name of the resource
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.RESOURCE_NAME_INDEX);
      dataCell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));

      //3 - set the planned effort
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.PLANNED_EFFORT_INDEX);
      if (activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setValue(null);
      }
      else {
         dataCell.setDoubleValue(workRecord.getAssignment().getBaseEffort());
      }
      dataCell.setEnabled(false);

      //4 - set the actual effort
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX);
      if (activity.getType() == OpActivity.MILESTONE) {
         dataCell.setValue(null);
         dataCell.setEnabled(false);
      }
      else {
         dataCell.setDoubleValue(workRecord.getActualEffort());
      }

      //5 - set the remaining effort
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.REMAINING_EFFORT_INDEX);
      dataCell.setDoubleValue(workRecord.getRemainingEffort());

      boolean progressTracked = activity.getProjectPlan().getProgressTracked();
      if (!progressTracked || activity.getType() == OpActivity.MILESTONE || activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setValue(null);
         dataCell.setEnabled(false);
      }

      //6 - set completed
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.COMPLETED_INDEX);
      if (workRecord.getAssignment().getProjectPlan().getProgressTracked() || activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setBooleanValue(workRecord.getCompleted());
      }
      else {
         dataCell.setValue(null);
         dataCell.setEnabled(false);
      }

      //7 - set comment
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.COMMENTS_INDEX);
      dataCell.setStringValue(workRecord.getComment());

      //8 - set the original remaining
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ORIGINAL_REMAINING_INDEX);
      dataCell.setDoubleValue(workRecord.getAssignment().getRemainingEffort());

      //9 - set the activity type
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTIVITY_TYPE_INDEX);
      dataCell.setByteValue(activity.getType());

      //10 - set the activity created status
      dataCell = (XComponent) dataRow.getChild(OpWorkEffortValidator.ACTIVITY_CREATED_INDEX);
      dataCell.setBooleanValue(true);

      //set the activity's assignment on the dataRow
      dataRow.setValue(workRecord.getAssignment().locator());

      return dataRow;
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
      //set remaining effort change
      workRecord.setRemainingEffortChange(remainingEffort - assignment.getRemainingEffort());
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
      OpActivity activity;

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
   }
}
