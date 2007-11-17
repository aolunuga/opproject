package onepoint.project.modules.work.validators;

import onepoint.express.XComponent;
import onepoint.express.XValidationException;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.util.XCalendar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Work Time data set validator class.
 *
 * @author mihai.costin
 */
public class OpWorkTimeValidator extends OpWorkValidator {

   //data set indexes
   public static final int PROJECT_NAME_INDEX = 0; //string (choice: project_locator['project name'])
   public static final int ACTIVITY_NAME_INDEX = 1; //string (choice: activity_locator['activity name'])
   public static final int RESOURCE_NAME_INDEX = 2; //string (choice: resource_locator['resource name'])
   public static final int START_INDEX = 3; // int (nr of minutes form 00:00)
   public static final int FINISH_INDEX = 4;// int (nr of minutes from 00:00)
   public static final int DURATION_INDEX = 5; // int (nr of minutes from start to finish)

   public static final String PROJECT_SET = "TimeProjectSet";
   public static final String ACTIVITY_SET = "TimeActivitySet";
   public static final String RESOURCE_SET = "TimeResourceSet";

   private static final String START_VALUE_EXCEPTION = "StartValueException";
   private static final String FINISH_VALUE_EXCEPTION = "FinishValueException";
   private static final String DURATION_VALUE_EXCEPTION = "DurationValueException";
   private static final String INTERVALS_OVERLAP_EXCEPTION = "IntervalsOverlapException";
   private static final String EFFORT_DATA_SET = "WorkEffortRecordSet";

   private OpWorkEffortValidator hoursValidator;

   /**
    * Validates the data set.
    *
    * @return true if the data set is valid.
    * @throws XValidationException if a validation constraint is broken.
    */
   public boolean validateDataSet() {

      //checks if all the values on the data set are valid
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent row = (XComponent) data_set.getChild(i);
         if (getValue(row, PROJECT_NAME_INDEX) == null) {
            throw new XValidationException(PROJECT_NAME_EXCEPTION);
         }
         if (getActivity(row) == null) {
            throw new XValidationException(ACTIVITY_NAME_EXCEPTION);
         }
         if (getResource(row) == null) {
            throw new XValidationException(RESOURCE_NAME_EXCEPTION);
         }
         if (getValue(row, START_INDEX) == null) {
            throw new XValidationException(START_VALUE_EXCEPTION);
         }
         int startMinutes = ((Integer) getValue(row, START_INDEX)).intValue();
         if (startMinutes < 0 || startMinutes > XCalendar.MINUTES_PER_DAY) {
            throw new XValidationException(START_VALUE_EXCEPTION);
         }
         if (getValue(row, FINISH_INDEX) == null) {
            throw new XValidationException(FINISH_VALUE_EXCEPTION);
         }
         int finishMinutes = ((Integer) getValue(row, FINISH_INDEX)).intValue();
         if (finishMinutes <= 0 || finishMinutes > XCalendar.MINUTES_PER_DAY || finishMinutes < startMinutes) {
            throw new XValidationException(FINISH_VALUE_EXCEPTION);
         }
         if (getValue(row, DURATION_INDEX) == null) {
            throw new XValidationException(DURATION_VALUE_EXCEPTION);
         }
         int durationMinutes = getDuration(row);
         if (durationMinutes <= 0 || durationMinutes > XCalendar.MINUTES_PER_DAY) {
            throw new XValidationException(DURATION_VALUE_EXCEPTION);
         }
         if (finishMinutes - startMinutes != durationMinutes) {
            throw new XValidationException(DURATION_VALUE_EXCEPTION);
         }
      }

      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent row = (XComponent) data_set.getChild(i);
         int start1 = ((Integer) getValue(row, START_INDEX)).intValue();
         int end1 = ((Integer) getValue(row, FINISH_INDEX)).intValue();
         for (int j = i + 1; j < data_set.getChildCount(); j++) {
            XComponent otherRow = (XComponent) data_set.getChild(j);
            int start2 = ((Integer) getValue(otherRow, START_INDEX)).intValue();
            int end2 = ((Integer) getValue(otherRow, FINISH_INDEX)).intValue();
            if (!(start2 > end1 || start1 > end2)) {
               //intervals overlap for the same assignment
               if (row.getValue().equals(otherRow.getValue())) {
                  throw new XValidationException(INTERVALS_OVERLAP_EXCEPTION);
               }
            }
         }
      }

      return true;
   }

   /**
    * Creates a new data row component for this validator's underlying dataset.
    *
    * @return a <code>XComponent</code> that represents a data row.
    */
   public XComponent newDataRow() {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell;

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataRow.addChild(dataCell); // project name 0

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataRow.addChild(dataCell); // activity name 1

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataRow.addChild(dataCell); // resource name 2

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setIntValue(0);
      dataRow.addChild(dataCell); // start 3

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setIntValue(0);
      dataRow.addChild(dataCell); // finish 4

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setIntValue(0);
      dataRow.addChild(dataCell); // duration 5

      return dataRow;
   }

   /**
    * Modifies the underlying data-cell for a given data-row.
    *
    * @param dataRow      a <code>XComponent</code> representing a row of data.
    * @param column_index a <code>int</code> representing the position of the data-cell inside the data-row.
    * @param value        a <code>Object</code> representing the new value for the data-cell.
    * @throws onepoint.express.XValidationException
    *          if the underlying data-cell could not be modified because of a validation error.
    */
   public void setDataCellValue(XComponent dataRow, int column_index, Object value) {
      XComponent cell = (XComponent) dataRow.getChild(column_index);

      if (cell.getValue() == null || !cell.getValue().equals(value)) {
         switch (column_index) {
            case PROJECT_NAME_INDEX:
               setProjectChoiceValue(cell, (String) value);
               break;

            case ACTIVITY_NAME_INDEX:
               setActivityChoiceValue(cell, (String) value);
               break;

            case RESOURCE_NAME_INDEX:
               setResourceChoiceValue(cell, (String) value);
               break;

            case START_INDEX:
               if (value == null) {
                  throw new XValidationException(START_VALUE_EXCEPTION);
               }
               int startMinutes = ((Integer) value).intValue();
               if (startMinutes < 0) {
                  throw new XValidationException(START_VALUE_EXCEPTION);
               }
               cell.setValue(value);
               //update duration
               updateDuration(dataRow);
               adjustDurationToPulse(dataRow);
               break;

            case FINISH_INDEX:
               if (value == null) {
                  throw new XValidationException(FINISH_VALUE_EXCEPTION);
               }
               int finishMinutes = ((Integer) value).intValue();
               if (finishMinutes < 0) {
                  throw new XValidationException(FINISH_VALUE_EXCEPTION);
               }
               cell.setValue(value);
               //update duration
               updateDuration(dataRow);
               adjustDurationToPulse(dataRow);
               break;

            case DURATION_INDEX:
               if (value == null) {
                  throw new XValidationException(DURATION_VALUE_EXCEPTION);
               }
               int durationMinutes = ((Integer) value).intValue();
               if (durationMinutes < 0) {
                  throw new XValidationException(DURATION_VALUE_EXCEPTION);
               }
               cell.setValue(value);
               //update finish
               if (!adjustDurationToPulse(dataRow)) {
                  updateFinish(dataRow);
               }
               break;

            default:
               cell.setValue(value);
               break;
         }
         updateHoursDataSet();
      }
   }


   /**
    * Removes an array of data rows from the underlying data set.
    *
    * @param dataRows a <code>List</code> of <code>XComponent</code> representing data rows.
    * @return <code>true</code> or <code>false</code> whether the removal was sucessfull.
    */
   public boolean removeDataRows(List dataRows) {
      boolean success = super.removeDataRows(dataRows);
      if (success) {
         updateHoursDataSet();
      }
      return success;
   }

   /**
    * Adjusts the value of the duration to the pulsing.
    *
    * @param dataRow Data row to adjust the duration
    * @return true if duration was changed.
    */
   private boolean adjustDurationToPulse(XComponent dataRow) {
      if (getPulsing() != null) {
         int pulsing = getPulsing().intValue();
         if (pulsing != 0) {
            int durationMinutes = getDuration(dataRow);
            if (durationMinutes % pulsing != 0) {
               durationMinutes = (durationMinutes / pulsing + 1) * pulsing;
               setValue(dataRow, DURATION_INDEX, new Integer(durationMinutes));
               updateFinish(dataRow);
               return true;
            }
         }
      }
      return false;
   }

   private int getDuration(XComponent dataRow) {
      return ((Integer) getValue(dataRow, DURATION_INDEX)).intValue();
   }

   /**
    * Updates the finish (and start if necessary) based on the duration value.
    * It assumes the duration in not null.
    *
    * @param dataRow data row to be updated
    */
   private void updateFinish(XComponent dataRow) {
      int durationMinutes = getDuration(dataRow);
      Integer start = (Integer) getValue(dataRow, START_INDEX);
      if (start == null) {
         setValue(dataRow, FINISH_INDEX, new Integer(0));
         return;
      }
      int startMinutes = start.intValue();
      if (startMinutes + durationMinutes < XCalendar.MINUTES_PER_DAY) {
         setValue(dataRow, FINISH_INDEX, new Integer(startMinutes + durationMinutes));
      }
      else {
         startMinutes = XCalendar.MINUTES_PER_DAY - 1 - durationMinutes;
         setValue(dataRow, START_INDEX, new Integer(startMinutes));
         setValue(dataRow, FINISH_INDEX, new Integer(startMinutes + durationMinutes));
      }
   }

   /**
    * Updates the duration based on the start/finish values. If any of the required values is missing duration will
    * be set to 0.
    *
    * @param dataRow data row to be updated
    */
   private void updateDuration(XComponent dataRow) {
      Integer start = (Integer) getValue(dataRow, START_INDEX);
      if (start == null) {
         setValue(dataRow, DURATION_INDEX, new Integer(0));
         return;
      }
      int startMinutes = start.intValue();
      Integer finish = (Integer) getValue(dataRow, FINISH_INDEX);
      if (finish == null) {
         setValue(dataRow, DURATION_INDEX, new Integer(0));
         return;
      }
      int finishMinutes = finish.intValue();

      if (finishMinutes > startMinutes) {
         setValue(dataRow, DURATION_INDEX, new Integer(finishMinutes - startMinutes));
      }
      else {
         setValue(dataRow, DURATION_INDEX, new Integer(0));
      }
   }


   /**
    * Gets the resource choice set.
    *
    * @return resource set.
    */
   protected XComponent getResourceSet() {
      if (resourceSet == null) {
         resourceSet = new XComponent(XComponent.DATA_SET);
         XComponent form = data_set.getForm();
         if (form != null) {
            resourceSet = form.findComponent(RESOURCE_SET);
         }
      }
      return resourceSet;
   }

   /**
    * Gets the activity choice set.
    *
    * @return activity set.
    */
   protected XComponent getActivitySet() {
      if (activitySet == null) {
         activitySet = new XComponent(XComponent.DATA_SET);
         XComponent form = data_set.getForm();
         if (form != null) {
            activitySet = form.findComponent(ACTIVITY_SET);
         }
      }
      return activitySet;
   }

   /**
    * Gets the project choice set.
    *
    * @return project set.
    */
   protected XComponent getProjectSet() {
      if (projectSet == null) {
         projectSet = new XComponent(XComponent.DATA_SET);
         XComponent form = data_set.getForm();
         if (form != null) {
            projectSet = form.findComponent(PROJECT_SET);
         }
      }
      return projectSet;
   }

   public void setProject(XComponent dataRow, String choice) {
      setValue(dataRow, PROJECT_NAME_INDEX, choice);
   }

   public String getResource(XComponent dataRow) {
      return (String) getValue(dataRow, RESOURCE_NAME_INDEX);
   }

   public String getActivity(XComponent dataRow) {
      return (String) getValue(dataRow, ACTIVITY_NAME_INDEX);
   }

   public String getProject(XComponent dataRow) {
      return (String) getValue(dataRow, PROJECT_NAME_INDEX);
   }

   public void setResource(XComponent dataRow, String value) {
      setValue(dataRow, RESOURCE_NAME_INDEX, value);
   }

   public void setActivity(XComponent dataRow, String value) {
      setValue(dataRow, ACTIVITY_NAME_INDEX, value);
   }

   public OpWorkEffortValidator getHoursValidator() {
      //if the hours validator was previously set just return it
      if (hoursValidator != null) {
         return hoursValidator;
      }
      //if it was not set get it from the form's effort dataset
      else {
         XComponent form = data_set.getForm();
         XComponent hoursDataSet = null;
         if (form != null) {
            hoursDataSet = form.findComponent(EFFORT_DATA_SET);
         }
         if (hoursDataSet != null) {
            hoursValidator = (OpWorkEffortValidator) hoursDataSet.validator();
         }

         if(hoursValidator == null) {
            throw new IllegalStateException("Illegal state between the time dataset, hours dataset and their validators in WorkSlips");
         }
         return hoursValidator;
      }
   }

   public void setHoursValidator(OpWorkEffortValidator hoursValidator) {
      this.hoursValidator = hoursValidator;
   }

   /**
    * Iterates the time data set and for each row with assignment value set it calculates the actual effort.
    * It then uses the new values to insert/update rows in hours data set.
    */
   public void updateHoursDataSet() {

      //collect actual efforts
      Map efforts = new HashMap();
      for (int i = 0; i < data_set.getChildCount(); i++) {
         XComponent row = (XComponent) data_set.getChild(i);
         Object locator = row.getValue();
         if (locator != null) {
            int duration = getDuration(row);
            if (efforts.get(locator) != null) {
               duration += ((Integer) efforts.get(locator)).intValue();
            }
            efforts.put(locator, new Integer(duration));
         }
      }

      //update values from the hours data set
      XComponent hours = getHoursValidator().getDataSet();      

      for (int i = 0; i < hours.getChildCount(); i++) {
         XComponent hourRow = (XComponent) hours.getChild(i);
         Object locator = hourRow.getValue();
         if (locator != null) {
            Integer newEffort = (Integer) efforts.get(locator);
            if (newEffort != null) {
               //update with the new activity
               Double actualEffort = new Double(newEffort.doubleValue() / XCalendar.MINUTES_PER_HOUR);
               getHoursValidator().setDataCellValue(hourRow, OpWorkEffortValidator.ACTUAL_EFFORT_INDEX, actualEffort);
               //remove entry from map
               efforts.remove(locator);
            }
            else {
               //remove it if it's not a milestone
               Byte type = getHoursValidator().getActivityType(hourRow);
               if (type != null && type.byteValue() != OpGanttValidator.MILESTONE) {
                  hours.removeChild(hourRow);
               }
            }
         }
      }

      //add all remaining values from map into hours
      for (Iterator iterator = efforts.entrySet().iterator(); iterator.hasNext();) {
         Map.Entry entry = (Map.Entry) iterator.next();

         String locator = (String) entry.getKey();
         XComponent timeRow = null;
         for (int i = 0; i < data_set.getChildCount(); i++) {
            timeRow = (XComponent) data_set.getChild(i);
            if (timeRow.getValue() != null && timeRow.getValue().equals(locator)) {
               break;
            }
         }

         if (timeRow != null) {
            Integer effort = (Integer) entry.getValue();
            XComponent hourRow = getHoursValidator().newDataRow();
            hourRow.setValue(entry.getKey());

            getHoursValidator().setProject(hourRow, this.getProject(timeRow));
            String resourceChoice = this.getResource(timeRow);
            getHoursValidator().setResource(hourRow, resourceChoice);
            String activityChoice = this.getActivity(timeRow);
            getHoursValidator().setActivity(hourRow, activityChoice);
            Byte type = getActivityTypeForChoice(activityChoice);
            getHoursValidator().setActivityType(hourRow, type);
            getHoursValidator().enableRowForActivityType(hourRow, type.byteValue(), isProgressTracked(timeRow));
            getHoursValidator().updateEffortCells(activityChoice, resourceChoice, hourRow, isProgressTracked(hourRow), getAssignmentMap());

            Double actualEffort = new Double(effort.doubleValue() / XCalendar.MINUTES_PER_HOUR);
            getHoursValidator().setActualEffort(hourRow, actualEffort);
            if (isProgressTracked(hourRow)) {
               Double originalRemaining = (Double) ((XComponent) hourRow.getChild(OpWorkEffortValidator.ORIGINAL_REMAINING_INDEX)).getValue();
               double remainingValue = originalRemaining.doubleValue() - actualEffort.doubleValue();
               if (remainingValue < 0) {
                  remainingValue = 0;
               }
               getHoursValidator().setRemainingEffort(hourRow, new Double(remainingValue));
            }

            hourRow.getChild(OpWorkEffortValidator.PROJECT_NAME_INDEX).setEnabled(false);
            hourRow.getChild(OpWorkEffortValidator.ACTIVITY_NAME_INDEX).setEnabled(false);
            hourRow.getChild(OpWorkEffortValidator.RESOURCE_NAME_INDEX).setEnabled(false);
            hourRow.getChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX).setEnabled(false);

            getHoursValidator().setEnabled(hourRow, false);
            hours.addDataRow(hourRow);
         }
      }
   }
}