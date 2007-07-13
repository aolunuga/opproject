package onepoint.project.modules.work.validators;

import onepoint.express.XComponent;
import onepoint.express.XValidationException;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.util.XCalendar;

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

   public static final String PROJECT_SET = "EffortProjectSet";
   public static final String ACTIVITY_SET = "EffortActivitySet";
   public static final String RESOURCE_SET = "EffortResourceSet";

   private static final String ACTUAL_EFFORT_EXCEPTION = "ActualEffortException";
   private static final String REMAINING_EFFORT_EXCEPTION = "RemainingEffortException";
   private static final String DUPLICATE_EFFORT_EXCEPTION = "DuplicateEffortException";

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
            if (getActivityType(row).byteValue() != OpGanttValidator.MILESTONE &&
                 getActivityType(row).byteValue() != OpGanttValidator.ADHOC_TASK) {
               throw new XValidationException(REMAINING_EFFORT_EXCEPTION);
            }
         }
         else {
            if (getRemainingEffort(row).doubleValue() < 0) {
               throw new XValidationException(REMAINING_EFFORT_EXCEPTION);
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

               //set the activity type cell value
               XComponent activityChoiceDataSet = getActivitySet();

               //find the activity in the activity choice set
               for (int i = 0; i < activityChoiceDataSet.getChildCount(); i++) {
                  XComponent choiceActivityRow = (XComponent) activityChoiceDataSet.getChild(i);
                  if (choiceActivityRow.getStringValue().equals(cell.getStringValue())) {
                     //set the activity type
                     byte activityType = ((XComponent) choiceActivityRow.getChild(ACTIVITY_CHOICE_SET_ACTIVITY_TYPE_INDEX)).getByteValue();
                     setValue(dataRow, ACTIVITY_TYPE_INDEX, new Byte(activityType));

                     //if the new activity is a milestone set disable actual and remaining effort, else enable them
                     if (activityType == OpGanttValidator.MILESTONE) {
                        setValue(dataRow, ACTUAL_EFFORT_INDEX, null);
                        setValue(dataRow, REMAINING_EFFORT_INDEX, null);
                        dataRow.getChild(ACTUAL_EFFORT_INDEX).setEnabled(false);
                        dataRow.getChild(REMAINING_EFFORT_INDEX).setEnabled(false);
                     }
                     else {
                        setActualEffort(dataRow, new Double(0));
                        setValue(dataRow, REMAINING_EFFORT_INDEX, new Double(0));
                        dataRow.getChild(ACTUAL_EFFORT_INDEX).setEnabled(true);
                        dataRow.getChild(REMAINING_EFFORT_INDEX).setEnabled(true);
                     }

                     //if the new activity is an adhoc task disable base cost and remaining effost and set them to null,
                     //else enable remaining and set both of them to 0
                     if (activityType == OpGanttValidator.ADHOC_TASK) {
                        setValue(dataRow, PLANNED_EFFORT_INDEX, null);
                        setValue(dataRow, REMAINING_EFFORT_INDEX, null);
                        dataRow.getChild(REMAINING_EFFORT_INDEX).setEnabled(false);
                     }
                     else {
                        setValue(dataRow, PLANNED_EFFORT_INDEX, new Double(0));
                        setValue(dataRow, REMAINING_EFFORT_INDEX, new Double(0));
                        dataRow.getChild(REMAINING_EFFORT_INDEX).setEnabled(true);
                     }
                  }
               }

               //reset the completed check box
               setValue(dataRow, COMPLETED_INDEX, new Boolean(false));

               //check if the resource name cell is filled in order to set the planned effort cell value
               if (getResource(dataRow) != null) {
                  //set the base effort, remaining & original remaining effort cell values
                  updateEffortCells(cell.getStringValue(), getResource(dataRow), dataRow);
                  //set the project name cell value
                  setValue(dataRow, PROJECT_NAME_INDEX, getProjectChoiceByActivity(activityChoice));
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
                  updateEffortCells(getActivity(dataRow), cell.getStringValue(), dataRow);
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

               cell.setDoubleValue(actualEffort);

               activityChoice = getActivity(dataRow);
               resourceChoice = getResource(dataRow);
               if (activityChoice != null && resourceChoice != null &&
                    !getRemainingEffortModifiedByUser(activityChoice, resourceChoice)) {

                  //update remaining effort cell value
                  XComponent remainingEffortCell = (XComponent) dataRow.getChild(REMAINING_EFFORT_INDEX);
                  XComponent originalRemEffortCell = (XComponent) dataRow.getChild(ORIGINAL_REMAINING_INDEX);
                  if (remainingEffortCell.getEnabled()) {
                     if (originalRemEffortCell.getDoubleValue() - ((Double) value).doubleValue() > 0) {
                        remainingEffortCell.setDoubleValue(originalRemEffortCell.getDoubleValue() - ((Double) value).doubleValue());
                        setValue(dataRow, COMPLETED_INDEX, new Boolean(false));
                     }
                     else {
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

   /**
    * Removes an array of data rows from the underlying data set.
    *
    * @param dataRows a <code>XArray</code> of <code>XComponent</code> representing data rows.
    * @return <code>true</code> or <code>false</code> whether the removal was sucessfull.
    */
   public boolean removeDataRows(List dataRows) {
      for (int i = 0; i < dataRows.size(); i++) {
         XComponent dataRow = (XComponent) dataRows.get(i);

         //reset the flag indicating that the assignment was manually mofified by the user
         String activityChoice = getActivity(dataRow);
         String resourceChoice = getResource(dataRow);
         if (activityChoice != null && resourceChoice != null) {
            setRemainingEffortModifiedByUser(activityChoice, resourceChoice, false);
         }
         data_set.removeChild(dataRow);
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
            setValue(dataRow, ACTIVITY_TYPE_INDEX, null);
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
    * @param activityChoice - the choice of the activity, which together with the choice of the resource,
    *                       determines the assignment from which the efforts are takken
    * @param resourceChoice - the choice of the resource, which together with the choice of the resource,
    *                       determines the assignment from which the efforts are takken
    * @param dataRow        - the data row that is being updated.
    */
   public void updateEffortCells(String activityChoice, String resourceChoice, XComponent dataRow) {
      //get the list of assignment data from the assignment map
      XComponent mapField = getAssignmentMap();
      HashMap assignmentMap = (HashMap) mapField.getValue();
      List assignmentDataList = (List) assignmentMap.get(activityChoice + "-" + resourceChoice);

      //get the base effort for this assignment & set it on the base effort cell
      if (((XComponent)dataRow.getChild(PLANNED_EFFORT_INDEX)).getValue() != null) {
         Double baseEffort = (Double) assignmentDataList.get(ASSIGNMENT_BASE_EFFORT_INDEX);
         setValue(dataRow, PLANNED_EFFORT_INDEX, baseEffort);
      }

      //get the original remaining effort from the assignment and set it on the original remaining effort cell
      //and on the remaining effort cell
      Double originalRemainingEffort = (Double) assignmentDataList.get(ASSIGNMENT_REMAINING_EFFORT_INDEX);
      setValue(dataRow, ORIGINAL_REMAINING_INDEX, originalRemainingEffort);
      if (dataRow.getChild(REMAINING_EFFORT_INDEX).getEnabled()) {
         setValue(dataRow, REMAINING_EFFORT_INDEX, originalRemainingEffort);
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

   public Byte getActivityType(XComponent row) {
      return (Byte) getValue(row, ACTIVITY_TYPE_INDEX);
   }
}