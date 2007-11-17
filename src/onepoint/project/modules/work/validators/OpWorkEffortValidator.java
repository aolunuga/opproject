package onepoint.project.modules.work.validators;

import onepoint.express.XComponent;
import onepoint.express.XValidationException;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.util.XCalendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mihai.costin
 */
public class OpWorkEffortValidator extends OpWorkValidator {

   public final static int PROJECT_NAME_INDEX = 0;
   public final static int ACTIVITY_NAME_INDEX = 1;
   public final static int RESOURCE_NAME_INDEX = 2;
   public final static int PLANNED_EFFORT_INDEX = 3;
   public final static int ACTUAL_EFFORT_INDEX = 4;
   public final static int REMAINING_EFFORT_INDEX = 5;
   public final static int COMPLETED_INDEX = 6;
   public final static int COMMENTS_INDEX = 7;
   public final static int ORIGINAL_REMAINING_INDEX = 8;
   public final static int ACTIVITY_TYPE_INDEX = 9;
   public final static int ACTIVITY_CREATED_INDEX = 10;
   public final static int ACTUAL_REMAINING_SUM_INDEX = 11;
   public final static int ENABLED_INDEX = 12;

   public static final String PROJECT_SET = "EffortProjectSet";
   public static final String ACTIVITY_SET = "EffortActivitySet";
   public static final String RESOURCE_SET = "EffortResourceSet";

   private static final String ACTUAL_EFFORT_EXCEPTION = "ActualEffortException";
   private static final String REMAINING_EFFORT_EXCEPTION = "RemainingEffortException";
   private static final String DUPLICATE_EFFORT_EXCEPTION = "DuplicateEffortException";

   //indexes used for the list of completed assignments
   private static final int LIST_ACTIVITY_INDEX = 0;
   private static final int LIST_RESOURCE_INDEX = 1;

   private static final String TIME_DATA_SET = "WorkTimeRecordSet";
   private OpWorkTimeValidator timeValidator;

   private static final XLog logger = XLogFactory.getClientLogger(OpWorkEffortValidator.class);

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

         if (getValue(row, ACTIVITY_NAME_INDEX) == null) {
            throw new XValidationException(ACTIVITY_NAME_EXCEPTION);
         }

         if (getValue(row, RESOURCE_NAME_INDEX) == null) {
            throw new XValidationException(RESOURCE_NAME_EXCEPTION);
         }

         if (getActualEffort(row) == null) {
            if (getActivityType(row).byteValue() != OpGanttValidator.MILESTONE) {
               throw new XValidationException(ACTUAL_EFFORT_EXCEPTION);
            }
         }
         else {
            if (getActualEffort(row).doubleValue() < 0) {
               throw new XValidationException(ACTUAL_EFFORT_EXCEPTION);
            }
         }

         if (getRemainingEffort(row) == null) {
            if (isProgressTracked(row)) {
               if (getActivityType(row).byteValue() != OpGanttValidator.MILESTONE &&
                    getActivityType(row).byteValue() != OpGanttValidator.ADHOC_TASK) {
                  throw new XValidationException(REMAINING_EFFORT_EXCEPTION);
               }
            }
         }
         else {
            if (getRemainingEffort(row).doubleValue() < 0) {
               throw new XValidationException(REMAINING_EFFORT_EXCEPTION);
            }
            else {
               if (getRemainingEffort(row).doubleValue() == 0) {
                  setValue(row, COMPLETED_INDEX, Boolean.TRUE);
               }
            }
         }

         for (int j = i + 1; j < data_set.getChildCount(); j++) {
            XComponent followingRow = (XComponent) data_set.getChild(j);
            if (row.getStringValue().equals(followingRow.getStringValue())) {
               throw new XValidationException(DUPLICATE_EFFORT_EXCEPTION);
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

      // Activity's project name - 0
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataRow.addChild(dataCell);

      //activity name - 1
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataRow.addChild(dataCell);

      //resource - 2
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataRow.addChild(dataCell);

      // Assignment base effort (planned effort) - 3
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(false);
      dataCell.setDoubleValue(0d);
      dataRow.addChild(dataCell);

      //actual effort - 4
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setDoubleValue(0d);
      dataRow.addChild(dataCell);

      //remaining effort - 5
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setDoubleValue(0d);
      dataRow.addChild(dataCell);

      //completed - 6
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setBooleanValue(false);
      dataRow.addChild(dataCell);

      //comments - 7
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataRow.addChild(dataCell);

      //original remaining effort - 8
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setDoubleValue(0d);
      dataRow.addChild(dataCell);

      //activity type - 9
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataRow.addChild(dataCell);

      //Activity created status (newly inserted / edit ) - 10
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setBooleanValue(true);
      dataRow.addChild(dataCell);

      //actual + remaining sum - 11
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setDoubleValue(0d);
      dataRow.addChild(dataCell);

      //row enabled - 12
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setBooleanValue(true);
      dataRow.addChild(dataCell);


      return dataRow;
   }


   public static void set(XComponent dataRow, int column_index, Object value) {
      XComponent cell = (XComponent) dataRow.getChild(column_index);
      cell.setValue(value);
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
               String activityChoice = getActivity(dataRow);
               String resourceChoice = getResource(dataRow);
               if (activityChoice != null && resourceChoice != null) {
                  setRemainingEffortModifiedByUser(activityChoice, resourceChoice, false);
               }

               setActivityChoiceValue(cell, (String) value);

               Byte type = getActivityTypeForChoice(value);

               setActivityType(dataRow, type);
               enableRowForActivityType(dataRow, type.byteValue(), isProgressTracked(dataRow));

               //check if the resource name cell is filled in order to set the planned effort cell value
               if (getResource(dataRow) != null) {
                  //set the base effort, remaining & original remaining effort cell values
                  updateEffortCells(cell.getStringValue(), getResource(dataRow), dataRow, isProgressTracked(dataRow), getAssignmentMap());
               }
               break;

            case RESOURCE_NAME_INDEX:
               //reset the flag that indicates that the user manually modified the remaining cost for this assignment
               activityChoice = getActivity(dataRow);
               resourceChoice = getResource(dataRow);
               if (activityChoice != null && resourceChoice != null) {
                  setRemainingEffortModifiedByUser(activityChoice, resourceChoice, false);
               }
               setResourceChoiceValue(cell, (String) value);
               //check if the activity name cell is filled
               if (getActivity(dataRow) != null) {
                  //set the base effort cell value & the original remaining effort cell value
                  updateEffortCells(getActivity(dataRow), cell.getStringValue(), dataRow, isProgressTracked(dataRow), getAssignmentMap());
                  //set the project name cell value
                  setValue(dataRow, PROJECT_NAME_INDEX, getProjectChoiceByActivity(activityChoice));
               }
               break;

            case ACTUAL_EFFORT_INDEX:
               if (value == null) {
                  throw new XValidationException(ACTUAL_EFFORT_EXCEPTION);
               }
               double actualEffort = ((Double) value).doubleValue();
               if (actualEffort < 0) {
                  throw new XValidationException(ACTUAL_EFFORT_EXCEPTION);
               }

               if (getPulsing() != null) {
                  int minutes = (int) (actualEffort * XCalendar.MINUTES_PER_HOUR);
                  int pulsing = getPulsing().intValue();
                  if (pulsing != 0 && minutes % pulsing != 0) {
                     minutes = (minutes / pulsing + 1) * pulsing;
                  }
                  actualEffort = (double) minutes / XCalendar.MINUTES_PER_HOUR;
               }

               //make a copy of the old cell value before changing it
               double oldCellValue = cell.getDoubleValue();

               cell.setDoubleValue(actualEffort);

               activityChoice = getActivity(dataRow);
               resourceChoice = getResource(dataRow);
               if (activityChoice != null && resourceChoice != null &&
                    !getRemainingEffortModifiedByUser(activityChoice, resourceChoice)) {

                  //update remaining effort cell value
                  XComponent remainingEffortCell = (XComponent) dataRow.getChild(REMAINING_EFFORT_INDEX);
                  XComponent actualRemainingSumCell = (XComponent) dataRow.getChild(ACTUAL_REMAINING_SUM_INDEX);
                  if (remainingEffortCell.getEnabled()) {
                     if (oldCellValue > actualRemainingSumCell.getDoubleValue()) {
                        oldCellValue = actualRemainingSumCell.getDoubleValue();
                     }
                     if (remainingEffortCell.getDoubleValue() + oldCellValue - cell.getDoubleValue() > 0) {
                        remainingEffortCell.setDoubleValue(remainingEffortCell.getDoubleValue() + oldCellValue - cell.getDoubleValue());
                        setValue(dataRow, COMPLETED_INDEX, new Boolean(false));
                     }
                     else {
                        actualRemainingSumCell.setDoubleValue(oldCellValue + remainingEffortCell.getDoubleValue());
                        remainingEffortCell.setDoubleValue(0d);
                        setValue(dataRow, COMPLETED_INDEX, new Boolean(true));
                     }
                  }
               }

               break;

            case REMAINING_EFFORT_INDEX:
               if (value == null) {
                  throw new XValidationException(REMAINING_EFFORT_EXCEPTION);
               }
               double remainingEffort = ((Double) value).doubleValue();
               if (remainingEffort < 0) {
                  throw new XValidationException(REMAINING_EFFORT_EXCEPTION);
               }
               //if remaining is set to 0 set completed to true and disable remaining
               if (remainingEffort == 0) {
                  setValue(dataRow, COMPLETED_INDEX, new Boolean(true));
                  cell.setEnabled(false);
               }
               //else set completed to false
               else {
                  setValue(dataRow, COMPLETED_INDEX, new Boolean(false));
               }
               cell.setValue(value);
               //set the flag that indicates that the user modified the remaining effort for this assignment
               activityChoice = getActivity(dataRow);
               resourceChoice = getResource(dataRow);
               if (activityChoice != null && resourceChoice != null) {
                  setRemainingEffortModifiedByUser(activityChoice, resourceChoice, true);
               }
               break;

            case COMPLETED_INDEX:
               XComponent remainingEffortCell = (XComponent) dataRow.getChild(REMAINING_EFFORT_INDEX);
               if (getActivityType(dataRow) != null) {
                  byte activityType = ((XComponent) dataRow.getChild(ACTIVITY_TYPE_INDEX)).getByteValue();
                  if (activityType != OpGanttValidator.MILESTONE && activityType != OpGanttValidator.ADHOC_TASK) {
                     if (((Boolean) value).booleanValue()) {
                        //set the remaining effort cell to 0 and disable it
                        remainingEffortCell.setDoubleValue(0d);
                     }
                     remainingEffortCell.setEnabled(!((Boolean) value).booleanValue());
                  }
               }
               cell.setValue(value);
               break;

            default:
               cell.setValue(value);
               break;
         }
      }
   }

   public void setActivityType(XComponent dataRow, Byte type) {
      setValue(dataRow, ACTIVITY_TYPE_INDEX, type);
   }

   /**
    * Enables/diables the data cells of this data row for the given activity type.
    *
    * @param dataRow
    * @param activityType
    * @param progressTracked
    */
   public void enableRowForActivityType(XComponent dataRow, byte activityType, boolean progressTracked) {

      if (activityType == OpGanttValidator.MILESTONE) {
         //if the new activity is a milestone set disable actual and remaining effort
         setValue(dataRow, ACTUAL_EFFORT_INDEX, null);
         setValue(dataRow, REMAINING_EFFORT_INDEX, null);
         dataRow.getChild(ACTUAL_EFFORT_INDEX).setEnabled(false);
         dataRow.getChild(REMAINING_EFFORT_INDEX).setEnabled(false);
         //reset the completed check box
         setValue(dataRow, COMPLETED_INDEX, new Boolean(false));
         dataRow.getChild(COMPLETED_INDEX).setEnabled(true);
      }
      else if (activityType == OpGanttValidator.ADHOC_TASK) {
         //if the new activity is an adhoc task disable base cost and remaining effort and set them to null,
         setValue(dataRow, PLANNED_EFFORT_INDEX, null);
         setValue(dataRow, REMAINING_EFFORT_INDEX, null);
         dataRow.getChild(REMAINING_EFFORT_INDEX).setEnabled(false);
         //reset the completed check box
         setValue(dataRow, COMPLETED_INDEX, new Boolean(false));
         dataRow.getChild(COMPLETED_INDEX).setEnabled(true);
      }
      else {
         //other activity types
         dataRow.getChild(ACTUAL_EFFORT_INDEX).setEnabled(true);
         if (progressTracked) {
            dataRow.getChild(REMAINING_EFFORT_INDEX).setEnabled(true);
            setValue(dataRow, REMAINING_EFFORT_INDEX, new Double(0));
            //reset the completed check box
            setValue(dataRow, COMPLETED_INDEX, new Boolean(false));
            dataRow.getChild(COMPLETED_INDEX).setEnabled(true);
         }
         else {
            dataRow.getChild(REMAINING_EFFORT_INDEX).setEnabled(false);
            setValue(dataRow, REMAINING_EFFORT_INDEX, null);
            dataRow.getChild(COMPLETED_INDEX).setEnabled(false);
            setValue(dataRow, COMPLETED_INDEX, null);
         }
         setValue(dataRow, PLANNED_EFFORT_INDEX, new Double(0));
         setActualEffort(dataRow, new Double(0));
      }
   }

   public void setActualEffort(XComponent dataRow, Double value) {
      setValue(dataRow, ACTUAL_EFFORT_INDEX, value);
   }

   public Double getActualEffort(XComponent dataRow) {
      return (Double) getValue(dataRow, ACTUAL_EFFORT_INDEX);
   }

   public void setRemainingEffort(XComponent dataRow, Double value) {
      setValue(dataRow, REMAINING_EFFORT_INDEX, value);
   }

   public Double getRemainingEffort(XComponent dataRow) {
      return (Double) getValue(dataRow, REMAINING_EFFORT_INDEX);
   }

   public void setOriginalRemainingEffort(XComponent dataRow, Double value) {
      setValue(dataRow, ORIGINAL_REMAINING_INDEX, value);
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

   public void setProject(XComponent dataRow, String choice) {
      setValue(dataRow, PROJECT_NAME_INDEX, choice);
   }

   public String getProject(XComponent dataRow) {
      return (String) getValue(dataRow, PROJECT_NAME_INDEX);
   }

   public String getActivity(XComponent dataRow) {
      return (String) getValue(dataRow, ACTIVITY_NAME_INDEX);
   }

   public void setActivity(XComponent dataRow, String choice) {
      setValue(dataRow, ACTIVITY_NAME_INDEX, choice);
   }

   public String getResource(XComponent dataRow) {
      return (String) getValue(dataRow, RESOURCE_NAME_INDEX);
   }

   public void setResource(XComponent dataRow, String choice) {
      setValue(dataRow, RESOURCE_NAME_INDEX, choice);
   }

   public Byte getActivityType(XComponent row) {
      return (Byte) getValue(row, ACTIVITY_TYPE_INDEX);
   }

   public void setEnabled(XComponent row, boolean enabled) {
      setValue(row, ENABLED_INDEX, Boolean.valueOf(enabled));
   }

   public boolean getEnabled(XComponent row) {
      return ((Boolean) getValue(row, ENABLED_INDEX)).booleanValue();
   }

   public OpWorkTimeValidator getTimeValidator() {
      if(timeValidator != null) {
         return timeValidator;
      }
      else {
         XComponent form = data_set.getForm();
         XComponent timeDataSet = null;
         if (form != null) {
            timeDataSet = form.findComponent(TIME_DATA_SET);
         }
         if (timeDataSet != null) {
            timeValidator = (OpWorkTimeValidator) timeDataSet.validator();
         }
        return timeValidator;
      }
   }

   public void setTimeValidator(OpWorkTimeValidator timeValidator) {
      this.timeValidator = timeValidator;
   }

   /**
    * Removes an array of data rows from the underlying data set.
    *
    * @param dataRows a <code>List</code> of <code>XComponent</code> representing data rows.
    * @return <code>true</code> or <code>false</code> whether the removal was sucessfull.
    */
   public boolean removeDataRows(List dataRows) {
      for (int i = 0; i < dataRows.size(); i++) {
         XComponent dataRow = (XComponent) dataRows.get(i);

         if (getEnabled(dataRow)) {
            //reset the flag indicating that the assignment was manually mofified by the user
            String activityChoice = getActivity(dataRow);
            String resourceChoice = getResource(dataRow);
            if (activityChoice != null && resourceChoice != null) {
               setRemainingEffortModifiedByUser(activityChoice, resourceChoice, false);
            }
            data_set.removeChild(dataRow);
         }
      }
      return true;
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

   /**
    * Resets the values on the row
    *
    * @param dataRow          - the <code>XComponent</code> data row that will be reset
    * @param oldActivityValue - the value that was set on the activity cell before the reset
    * @param oldResourceValue - the value that was set on the resource cell before the reset
    */
   protected void resetRow(XComponent dataRow, String oldActivityValue, String oldResourceValue) {
      super.resetRow(dataRow, oldActivityValue, oldResourceValue);

      String resourceChoice = getResource(dataRow);
      String activityChoice = getActivity(dataRow);
      if (activityChoice == null || resourceChoice == null) {
         setValue(dataRow, PLANNED_EFFORT_INDEX, new Double(0));
         setActualEffort(dataRow, new Double(0));
         setValue(dataRow, ORIGINAL_REMAINING_INDEX, new Double(0));
         setValue(dataRow, REMAINING_EFFORT_INDEX, new Double(0));
         setValue(dataRow, COMPLETED_INDEX, new Boolean(false));
         if (activityChoice == null && ((XComponent) dataRow.getChild(ACTIVITY_TYPE_INDEX)).getValue() != null) {
            byte activityType = ((XComponent) dataRow.getChild(ACTIVITY_TYPE_INDEX)).getByteValue();

            //if the activity was a milestone or an adhoc task enable completed
            if (activityType == OpGanttValidator.MILESTONE || activityType == OpGanttValidator.ADHOC_TASK) {
               dataRow.getChild(COMPLETED_INDEX).setEnabled(true);
               //if the activity was a milestone enable actual effort and remaining effort cells
               if (activityType == OpGanttValidator.MILESTONE) {
                  dataRow.getChild(ACTUAL_EFFORT_INDEX).setEnabled(true);
                  dataRow.getChild(REMAINING_EFFORT_INDEX).setEnabled(true);
               }
            }
            setActivityType(dataRow, null);
         }
      }

      //reset the flag indicating that the assignment's remaining effort was manually modified by the user
      if (oldActivityValue != null && oldResourceValue != null) {
         setRemainingEffortModifiedByUser(oldActivityValue, oldResourceValue, false);
      }
   }

   /**
    * Sets the planned effort cell value and the original remaining effort cell value.
    *
    * @param activityChoice  - the choice of the activity, which together with the choice of the resource,
    *                        determines the assignment from which the efforts are takken
    * @param resourceChoice  - the choice of the resource, which together with the choice of the resource,
    *                        determines the assignment from which the efforts are takken
    * @param dataRow         - the data row that is being updated.
    * @param progressTracked
    * @param mapField
    */
   public void updateEffortCells(String activityChoice, String resourceChoice, XComponent dataRow, boolean progressTracked, XComponent mapField) {
      //get the list of assignment data from the assignment map
      HashMap assignmentMap = (HashMap) mapField.getValue();
      List assignmentDataList = (List) assignmentMap.get(activityChoice + "-" + resourceChoice);

      //get the base effort for this assignment & set it on the base effort cell
      if (((XComponent) dataRow.getChild(PLANNED_EFFORT_INDEX)).getValue() != null) {
         Double baseEffort = (Double) assignmentDataList.get(ASSIGNMENT_BASE_EFFORT_INDEX);
         setValue(dataRow, PLANNED_EFFORT_INDEX, baseEffort);
      }

      if (progressTracked) {
         //get the original remaining effort from the assignment and set it on the original remaining effort cell
         //and on the remaining effort cell
         Double originalRemainingEffort = (Double) assignmentDataList.get(ASSIGNMENT_REMAINING_EFFORT_INDEX);
         setValue(dataRow, ORIGINAL_REMAINING_INDEX, originalRemainingEffort);
         setValue(dataRow, ACTUAL_REMAINING_SUM_INDEX, originalRemainingEffort);
         if (dataRow.getChild(REMAINING_EFFORT_INDEX).getEnabled()) {
            setValue(dataRow, REMAINING_EFFORT_INDEX, originalRemainingEffort);
         }
      }
   }

   /**
    * Returns a <code>boolean</code> value which is <code>true</code> if the remaining effort for the
    * activity specified by the activityChoice and for the resource specified by resourceChoice was manually
    * modified by the user and <code>false</code> otherwise.
    *
    * @param activityChoice - the choice of the activity for which the remaining effort modification is interrogated
    * @param resourceChoice - the choice of the resource for which the remaining effort modification is interrogated
    * @return <code>true</code> if the remaining effort for the activity specified by the activityChoice and
    *         for the resource specified by resourceChoice was manually modified by the user and <code>false</code> otherwise.
    */
   private boolean getRemainingEffortModifiedByUser(String activityChoice, String resourceChoice) {
      List assignmentDataList;

      Map assignmentMap = (Map) getAssignmentMap().getValue();
      if (assignmentMap != null && !assignmentMap.keySet().isEmpty()) {
         assignmentDataList = (List) assignmentMap.get(activityChoice + "-" + resourceChoice);
         return ((Boolean) assignmentDataList.get(ASSIGNMENT_REMAINING_MODIFIED_INDEX)).booleanValue();
      }
      return false;
   }

   /**
    * Sets a flag with the given value indicating that the remaining effort for the
    * activity specified by the activityChoice and for the resource specified by resourceChoice was manually
    * modified by the user.
    *
    * @param activityChoice - the choice of the activity for which the remaining effort modification is updated
    * @param resourceChoice - the choice of the resource for which the remaining effort modification is updated
    * @param value          - the <code>boolean</code> value which will be set on the flag
    */
   private void setRemainingEffortModifiedByUser(String activityChoice, String resourceChoice, boolean value) {
      List assignmentDataList;

      Map assignmentMap = (Map) getAssignmentMap().getValue();
      if (assignmentMap != null && !assignmentMap.keySet().isEmpty()) {
         assignmentDataList = (List) assignmentMap.get(activityChoice + "-" + resourceChoice);
         assignmentDataList.set(ASSIGNMENT_REMAINING_MODIFIED_INDEX, new Boolean(value));
      }
   }

   /**
    * Returns a list which contains an activity -> resource map and an resource -> activity map.
    * First map: Key - the locator of the activity which has completed assignments
    * Value - a list of resources, one resource for each completed assignment
    * Second map: Key - the locator of the resource which has completed assignments
    * Value - a list of activities, one activity for each completed assignment
    *
    * @return - the list of maps.
    */
   private List getAllCompletedAssignments() {
      List assignmentMaps = new ArrayList();
      Map activityResourceMap = new HashMap();
      Map resourceActivityMap = new HashMap();

      for (int j = 0; j < data_set.getChildCount(); j++) {
         XComponent row = (XComponent) data_set.getChild(j);
         boolean completed = false;
         if (((XComponent) row.getChild(COMPLETED_INDEX)).getValue() != null) {
            completed = ((XComponent) row.getChild(COMPLETED_INDEX)).getBooleanValue();
         }
         if (completed) {
            //add the row's resource to the list of resources that belong to the row's activity
            List resourceList;
            if (!activityResourceMap.keySet().contains(getActivity(row))) {
               resourceList = new ArrayList();
               resourceList.add(getResource(row));
               activityResourceMap.put(getActivity(row), resourceList);
            }
            else {
               resourceList = (ArrayList) activityResourceMap.get(getActivity(row));
               resourceList.add(getResource(row));
            }

            //add the row's activity to the list of activities that belong to the row's resource
            List activityList;
            if (!resourceActivityMap.keySet().contains(getResource(row))) {
               activityList = new ArrayList();
               activityList.add(getActivity(row));
               resourceActivityMap.put(getResource(row), activityList);
            }
            else {
               activityList = (ArrayList) resourceActivityMap.get(getResource(row));
               activityList.add(getActivity(row));
            }
         }
      }

      assignmentMaps.add(LIST_ACTIVITY_INDEX, activityResourceMap);
      assignmentMaps.add(LIST_RESOURCE_INDEX, resourceActivityMap);

      return assignmentMaps;
   }

   /**
    * Checks if all the assignments of the activity specified by the activityChoice parameter are completed.
    *
    * @param activityChoice     - the choice of the activity whose assignments are being checked.
    * @param activityResouceMap - the map which contains all the resources from the completed assignments
    *                           for each activity in the activity set.
    * @return -  <code>true</code> if the activity has no uncompleted assignments or <code>false</code> otherwise.
    */
   private boolean areAllActivityAssignmentsCompleted(String activityChoice, Map activityResouceMap) {
      //get the list of resources (one for each assignment) for the activity
      List resourceList = new ArrayList();
      for (int i = 0; i < getActivitySet().getChildCount(); i++) {
         XComponent activityRow = (XComponent) getActivitySet().getChild(i);
         if (activityRow.getStringValue().equals(activityChoice)) {
            resourceList = ((XComponent) activityRow.getChild(ACTIVITY_CHOICE_SET_RESOURCE_INDEX)).getListValue();
         }
      }

      //get the list of resources from the completed assignments for the activity
      List assignmentResourceList = new ArrayList();
      if (activityResouceMap.keySet().contains(activityChoice)) {
         assignmentResourceList = (List) activityResouceMap.get(activityChoice);
      }

      if (assignmentResourceList.isEmpty() || assignmentResourceList.size() != resourceList.size()) {
         return false;
      }

      for (int i = 0; i < resourceList.size(); i++) {
         if (!assignmentResourceList.contains(resourceList.get(i))) {
            return false;
         }
      }
      return true;
   }

   /**
    * Checks if all the assignments of the resource specified by the resourceChoice parameter are completed.
    *
    * @param resourceChoice      - the choice of the resource whose assignments are being checked.
    * @param resourceActivityMap - the map which contains all the activities from the completed assignments
    *                            for each resource in the resource set.
    * @return -  <code>true</code> if the resource has no uncompleted assignments or <code>false</code> otherwise.
    */
   private boolean areAllResourceAssignmentsCompleted(String resourceChoice, Map resourceActivityMap) {
      //get the list of activities (one for each assignment) for the resource
      List activityList = new ArrayList();
      for (int i = 0; i < getResourceSet().getChildCount(); i++) {
         XComponent resourceRow = (XComponent) getResourceSet().getChild(i);
         if (resourceRow.getStringValue().equals(resourceChoice)) {
            activityList = ((XComponent) resourceRow.getChild(RESOURCE_CHOICE_SET_ACTIVITY_INDEX)).getListValue();
         }
      }

      //get the list of activities from the completed assignments for the resource
      List assignmentActivityList = new ArrayList();
      if (resourceActivityMap.keySet().contains(resourceChoice)) {
         assignmentActivityList = (List) resourceActivityMap.get(resourceChoice);
      }

      if (assignmentActivityList.isEmpty() || assignmentActivityList.size() != activityList.size()) {
         return false;
      }

      for (int i = 0; i < activityList.size(); i++) {
         if (!assignmentActivityList.contains(activityList.get(i))) {
            return false;
         }
      }
      return true;
   }

   /**
    * Filters out the activities that have all their assignments completed.
    *
    * @param resourceChoice - the locator of the resource ()
    */
   protected void advancedFilteringForActivity(String oldActivityChoice, String resourceChoice) {
      List completedAssignmentMaps = getAllCompletedAssignments();
      Map activityResourceMap = (Map) completedAssignmentMaps.get(LIST_ACTIVITY_INDEX);

      XComponent row;
      for (int i = 0; i < getActivitySet().getChildCount(); i++) {
         row = (XComponent) getActivitySet().getChild(i);
         if ((!row.getFiltered() && areAllActivityAssignmentsCompleted(row.getStringValue(), activityResourceMap))
              || (!row.getFiltered() && isCompletedAssignment(resourceChoice, row.getStringValue()))) {
            //if the filtered activity is already set on the data row do not filter it
            if (oldActivityChoice == null || !oldActivityChoice.equals(row.getValue())) {
               row.setFiltered(true);
            }
         }
      }
   }

   /**
    * Filters out the resources that have all their assignments completed.
    */
   protected void advancedFilteringForResource(String oldResourceChoice, String activityChoice) {
      List completedAssignmentMaps = getAllCompletedAssignments();
      Map resourceActivityMap = (Map) completedAssignmentMaps.get(LIST_RESOURCE_INDEX);

      XComponent row;
      for (int i = 0; i < getResourceSet().getChildCount(); i++) {
         row = (XComponent) getResourceSet().getChild(i);
         if ((!row.getFiltered() && areAllResourceAssignmentsCompleted(row.getStringValue(), resourceActivityMap))
              || (!row.getFiltered() && isCompletedAssignment(row.getStringValue(), activityChoice))) {
            //if the filtered resource is already set on the data row do not filter it
            if (oldResourceChoice == null || !oldResourceChoice.equals(row.getValue())) {
               row.setFiltered(true);
            }
         }
      }
   }

   /**
    * Checks if the assignment determined by the activity choice and resource choice passed as parameters is completed
    * in the current work slip or not.
    *
    * @param resourceChoice - the choice of the resource for which the assignment is being checked.
    * @param activityChoice - the choice of the activity for which the assignment is being checked.
    * @return <code>true</code> if the assignment formed by the resource choice and activity choice passed as parameters
    *         is completed in the current work slip or <code>false</code> otherwise.
    */
   private boolean isCompletedAssignment(String resourceChoice, String activityChoice) {
      if (resourceChoice != null && activityChoice != null) {
         for (int i = 0; i < data_set.getChildCount(); i++) {
            XComponent row = (XComponent) data_set.getChild(i);
            if (((XComponent) row.getChild(ACTIVITY_NAME_INDEX)).getValue() != null &&
                 ((XComponent) row.getChild(RESOURCE_NAME_INDEX)).getValue() != null &&
                 ((XComponent) row.getChild(ACTIVITY_NAME_INDEX)).getStringValue().equals(activityChoice) &&
                 ((XComponent) row.getChild(RESOURCE_NAME_INDEX)).getStringValue().equals(resourceChoice)) {
               return ((XComponent) row.getChild(COMPLETED_INDEX)).getBooleanValue();
            }
         }
      }
      return false;
   }


   public void addEmptyRow(XComponent row) {
      row.getChild(PROJECT_NAME_INDEX).setEnabled(true);
      row.getChild(ACTIVITY_NAME_INDEX).setEnabled(true);
      row.getChild(RESOURCE_NAME_INDEX).setEnabled(true);
      super.addEmptyRow(row);
   }

   /**
    * Gets the progress tracked flag for the project on the row passed as parameter. The search will be done in the
    *    project set from the time tab. The project cell must be set on this row.
    *
    * @param dataRow - the <code>XComponent</code> data row containing the project for which the tracking will be returned.
    * @return the progress tracked flag for the project on the row passed as parameter. The search will be done in the
    *    project set from the time tab.
    *
    */
   protected boolean getProgressTrackedFromTimeSet(XComponent dataRow) {
      if(getTimeValidator() != null) {
         return getTimeValidator().isProgressTracked(dataRow);
      }
      logger.warn("Illegal state between the time dataset, hours dataset and their validators in WorkSlips");
      return true;
   }
}