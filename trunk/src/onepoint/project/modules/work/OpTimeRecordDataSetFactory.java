/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.validators.OpWorkTimeValidator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class needed to form OpTimeRecord entities from data sets and vice versa
 *
 * @author florin.haizea
 */
public class OpTimeRecordDataSetFactory {

    /**
    * Creates a <code>XComponent</code> data row with the cell's values set from the <code>OpTimeRecord</code> entity
    *
    * @param timeRecord - the <code>OpTimeRecord</code> entity whose atributes will be set on the data row
    * @return a data row with the cell's values set from the <code>OpTimeRecord</code> entity
    */
    private static XComponent createTimeRow(OpTimeRecord timeRecord) {
       OpWorkTimeValidator timeValidator = new OpWorkTimeValidator();
       XComponent dataRow = timeValidator.newDataRow();
       XComponent dataCell;

       OpActivity activity = timeRecord.getActivity();
       OpProjectNode project = activity.getProjectPlan().getProjectNode();
       OpResource resource = timeRecord.getWorkRecord().getAssignment().getResource();

       //0 - set the name of the project
       dataCell = (XComponent) dataRow.getChild(OpWorkTimeValidator.PROJECT_NAME_INDEX);
       dataCell.setStringValue(XValidator.choice(project.locator(), project.getName()));

       //1 - set the name of the activity
       dataCell = (XComponent) dataRow.getChild(OpWorkTimeValidator.ACTIVITY_NAME_INDEX);
       dataCell.setStringValue(XValidator.choice(activity.locator(), activity.getName()));

       //2 - set the name of the resource
       dataCell = (XComponent) dataRow.getChild(OpWorkTimeValidator.RESOURCE_NAME_INDEX);
       dataCell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));

       //3 - set the start time
       dataCell = (XComponent) dataRow.getChild(OpWorkTimeValidator.START_INDEX);
       dataCell.setIntValue(timeRecord.getStart());

       //4 - set the finish time
       dataCell = (XComponent) dataRow.getChild(OpWorkTimeValidator.FINISH_INDEX);
       dataCell.setIntValue(timeRecord.getFinish());

       //5 - set the duration
       dataCell = (XComponent) dataRow.getChild(OpWorkTimeValidator.DURATION_INDEX);
       dataCell.setIntValue(timeRecord.getDuration());

       //set the activity's assignment on the dataRow
       dataRow.setValue(timeRecord.getWorkRecord().getAssignment().locator());

       return dataRow;
    }

   /**
    * Creates an <code>OpTimeRecord</code> entity from the data row's cell's values
    *
    * @param dataRow - the <code>XComponent</code> data row whose cell's values will be set on the entity
    * @return an <code>OpTimeRecord</code> entity created from the data row's cell's values
    */
   private static OpTimeRecord createTimeEntity(XComponent dataRow) {
      OpTimeRecord timeRecord = new OpTimeRecord();

      //the work record will not be set on the timeRecord
      timeRecord.setStart(((XComponent)dataRow.getChild(OpWorkTimeValidator.START_INDEX)).getIntValue());
      timeRecord.setFinish(((XComponent)dataRow.getChild(OpWorkTimeValidator.FINISH_INDEX)).getIntValue());
      timeRecord.setDuration(((XComponent)dataRow.getChild(OpWorkTimeValidator.DURATION_INDEX)).getIntValue());
      
      return timeRecord;
   }

   /**
    * Creates a <code>XComponent</code> data set from a <code>OpWorkRecord</code> entity. Each row in the
    * data set will represent an <code>OpTimeRecord</code> entity from the work record's time recods set.
    *
    * @param workRecord - the <code>OpWorkRecord</code> entity whose time records will be in the data set
    * @return a <code>XComponent</code> data set created from the <code>OpWorkRecord</code> entity. Each row in the
    *         data set will represent an <code>OpTimeRecord</code> entity from the work record's time recods set.
    */
   public static XComponent getTimeDataSetForWorkRecord(OpWorkRecord workRecord) {
      XComponent dataSet = new XComponent(XComponent.DATA_SET);

      for (OpTimeRecord timeRecord : workRecord.getTimeRecords()) {
         XComponent dataRow = createTimeRow(timeRecord);
         dataSet.addChild(dataRow);
      }
      dataSet.sort(OpWorkTimeValidator.START_INDEX);

      return dataSet;
   }

   /**
    * Creates a <code>List</code> of <code>OpTimePeriod</code> entities, each entity corresponding to a row in the
    * data set.
    *
    * @param timeDataSet - the <code>XComponent</code> data set whose rows will form the list of time periods
    * @return a <code>List</code> of <code>OpTimePeriod</code> entities, each entity corresponding to a row in the
    * data set.
    */
   public static Set<OpTimeRecord> createTimeRecords(XComponent timeDataSet) {
      Set<OpTimeRecord> timeRecords = new HashSet<OpTimeRecord>();
      OpTimeRecord timeRecord;

      for(int i = 0; i < timeDataSet.getChildCount(); i++)  {
         timeRecord = createTimeEntity((XComponent) timeDataSet.getChild(i));
         timeRecords.add(timeRecord);
      }

      return timeRecords;
   }

   /**
    * Fills a project choice set, an activity choice set and a resource choice set with information from the
    * assignmentList. Each row in the data sets will have it's value set to the choice of the entity.
    * The activity choice set rows will contain a data cell with the type of activity and a data cell with
    * the list of costs for that activity.
    *
    * @param choiceProjectSet    - the <code>XComponent</code> choice project set
    * @param choiceActivitySet   - the <code>XComponent</code> choice activity set
    * @param choiceResourceSet   - the <code>XComponent</code> choice resource set
    * @param assignmentList - the <code>List</code> of assignments.
    */
   public static void fillChoiceDataSets(XComponent choiceProjectSet, XComponent choiceActivitySet, XComponent choiceResourceSet,
        List<OpAssignment> assignmentList) {
      OpActivity activity;

      for(OpAssignment assignment : assignmentList) {
         activity = assignment.getActivity();

         boolean progressTracked = activity.getProjectPlan().getProgressTracked();
         //filter out milestones
         if (activity.getType() == OpActivity.MILESTONE) {
            continue;
         }

         OpWorkSlipDataSetFactory.fillChoiceDataSetsFromSingleAssignment(assignment, choiceProjectSet,
              choiceActivitySet, choiceResourceSet);
      }
   }
}