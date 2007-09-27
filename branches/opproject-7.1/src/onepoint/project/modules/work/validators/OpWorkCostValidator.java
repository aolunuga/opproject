package onepoint.project.modules.work.validators;

import onepoint.express.XComponent;
import onepoint.express.XValidationException;
import onepoint.express.XValidator;
import onepoint.project.modules.project.components.OpGanttValidator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Work Costs data set validator class.
 *
 * @author mihai.costin
 */
public class OpWorkCostValidator extends OpWorkValidator {


   public final static int PROJECT_NAME_INDEX = 0; //string (choice: project_locator['project name'])
   public final static int ACTIVITY_NAME_INDEX = 1; //string (choice: activity_locator['activity name'])
   public final static int RESOURCE_NAME_INDEX = 2; //string (choice: resource_locator['resource name'])
   public final static int INDICATOR_INDEX = 3; //int (icon indicator)
   public final static int COST_TYPE_INDEX = 4; //string (choice type_of_cost['cost name']), type of cost - byte
   public final static int BASE_COST_INDEX = 5; // double (planned cost for the activity)
   public final static int ACTUAL_COST_INDEX = 6; //double
   public final static int REMAINING_COST_INDEX = 7; //double
   public final static int COMMENTS_COST_INDEX = 8; //string
   public final static int ATTACHMENT_INDEX = 9; //List<List>
   public final static int ORIGINAL_REMAINING_COST_INDEX = 10; //double
   public final static int ACTIVITY_TYPE_INDEX = 11; //byte
   public final static int ACTUAL_REMAINING_SUM_INDEX = 12; //double

   public static final String PROJECT_SET = "CostProjectSet";
   public static final String ACTIVITY_SET = "CostActivitySet";
   public static final String RESOURCE_SET = "CostResourceSet";

   private static final String ACTUAL_COST_EXCEPTION = "ActualCostException";
   private static final String REMAINING_COST_EXCEPTION = "RemainingCostException";

   // Cost record attributes (bits/flags)
   public final static int HAS_ATTACHMENTS = 2;

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
         if (getValue(row, COST_TYPE_INDEX) == null) {
            throw new XValidationException(RESOURCE_NAME_EXCEPTION);
         }
         if (getValue(row, ACTUAL_COST_INDEX) == null || ((Double) getValue(row, ACTUAL_COST_INDEX)).doubleValue() <= 0) {
            throw new XValidationException(ACTUAL_COST_EXCEPTION);
         }
         if ((getValue(row, REMAINING_COST_INDEX) == null || ((Double) getValue(row, REMAINING_COST_INDEX)).doubleValue() < 0)
              && ((Byte) getValue(row, ACTIVITY_TYPE_INDEX)).byteValue() != OpGanttValidator.ADHOC_TASK) {
            throw new XValidationException(REMAINING_COST_EXCEPTION);
         }
      }

      return true;
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

   public String getProject(XComponent dataRow) {
      return (String) getValue(dataRow, PROJECT_NAME_INDEX);
   }

   public void setProject(XComponent dataRow, String choice) {
      setValue(dataRow, PROJECT_NAME_INDEX, choice);
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

   protected Byte getCostType(XComponent dataRow) {
      String costTypeString = (String) getValue(dataRow, COST_TYPE_INDEX);
      if (costTypeString != null) {
         return new Byte(XValidator.choiceID(costTypeString));
      }
      else {
         return null;
      }
   }

   protected void setCostType(XComponent dataRow, Byte choice) {
      setValue(dataRow, COST_TYPE_INDEX, choice);
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
      dataCell.setEnabled(false);
      dataCell.setIntValue((byte) 0);
      dataRow.addChild(dataCell); // indicator 3

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataRow.addChild(dataCell); // cost type 4

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(false);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell); //base cost 5

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell); //actual cost  6

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell); //remaining cost 7

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(true);
      dataRow.addChild(dataCell); //comments 8

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataRow.addChild(dataCell); //attachment 9

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataRow.addChild(dataCell); //original ramaining 10

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataRow.addChild(dataCell); //activity type 11

      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0d);
      dataRow.addChild(dataCell);//actual + remaining sum

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
               //update remaining costs as a result of the deletion of this activity - cost type combination
               updateRemainingCostsAtDeletion(dataRow);

               setActivityChoiceValue(cell, (String) value);

               //get the type of the activity
               byte activityType = getActivityType(cell);
               setValue(dataRow, ACTIVITY_TYPE_INDEX, new Byte(activityType));

               if (activityType != OpGanttValidator.ADHOC_TASK) {
                  //check if the cost type cell is filled
                  if (getCostType(dataRow) != null) {
                     setValue(dataRow, BASE_COST_INDEX, new Double(0));
                     setValue(dataRow, ACTUAL_COST_INDEX, new Double(0));
                     setValue(dataRow, REMAINING_COST_INDEX, new Double(0));
                     dataRow.getChild(REMAINING_COST_INDEX).setEnabled(true);
                     //set the base cost cell value & the original remaining cost cell value
                     updateCostCells(cell.getStringValue(), dataRow);
                  }
               }
               //if the activity is an adhoc task set base cost and remaining cost to null and disable them
               else {
                  setValue(dataRow, BASE_COST_INDEX, null);
                  setValue(dataRow, REMAINING_COST_INDEX, null);
                  dataRow.getChild(REMAINING_COST_INDEX).setEnabled(false);
               }
               break;

            case RESOURCE_NAME_INDEX:
               setResourceChoiceValue(cell, (String) value);
               break;

            case COST_TYPE_INDEX:
               //update remaining costs as a result of the deletion of this activity - cost type combination
               updateRemainingCostsAtDeletion(dataRow);

               cell.setValue(value);

               //check if the activity cell is filled
               String activityChoice = getActivity(dataRow);
               //get the type of the activity
               activityType = getActivityType(cell);
               if (activityChoice != null && activityType != OpGanttValidator.ADHOC_TASK) {
                  setValue(dataRow, BASE_COST_INDEX, new Double(0));
                  setValue(dataRow, ACTUAL_COST_INDEX, new Double(0));
                  setValue(dataRow, REMAINING_COST_INDEX, new Double(0));
                  dataRow.getChild(REMAINING_COST_INDEX).setEnabled(true);
                  //set the base cost cell value & the original remaining cost cell value
                  updateCostCells(activityChoice, dataRow);
               }
               break;

            case ACTUAL_COST_INDEX:
               if (value == null) {
                  throw new XValidationException(ACTUAL_COST_EXCEPTION);
               }
               double actualCost = ((Double) value).doubleValue();
               if (actualCost < 0) {
                  throw new XValidationException(ACTUAL_COST_EXCEPTION);
               }

               //check if the user already modified the remaining cost for this activity - cost type manually
               activityChoice = getActivity(dataRow);
               //get the type of the activity
               activityType = getActivityType(cell);
               if (getCostType(dataRow) != null && activityType != OpGanttValidator.ADHOC_TASK) {
                  byte costType = getCostType(dataRow).byteValue();
                  if (!getRemainingCostModifiedByUser(activityChoice, costType)) {
                     //update the remaining cost for all rows with the same activity and the same cost type
                     XComponent remainingEffortCell = (XComponent) dataRow.getChild(REMAINING_COST_INDEX);
                     XComponent actualRemainingSumCell = (XComponent) dataRow.getChild(ACTUAL_REMAINING_SUM_INDEX);
                     double oldCellValue = cell.getDoubleValue();
                     if (oldCellValue > actualRemainingSumCell.getDoubleValue()) {
                        oldCellValue = actualRemainingSumCell.getDoubleValue();
                     }
                     if (remainingEffortCell.getDoubleValue() + oldCellValue - actualCost > 0) {
                        updateRemainingCostCells(activityChoice, costType, remainingEffortCell.getDoubleValue() + oldCellValue - actualCost);
                     }
                     else {
                        actualRemainingSumCell.setDoubleValue(oldCellValue + remainingEffortCell.getDoubleValue());
                        updateRemainingCostCells(activityChoice, costType, 0d);
                     }
                  }
               }
               cell.setValue(value);
               break;

            case REMAINING_COST_INDEX:
               if (value == null) {
                  throw new XValidationException(REMAINING_COST_EXCEPTION);
               }
               double remainingCost = ((Double) value).doubleValue();
               if (remainingCost < 0) {
                  throw new XValidationException(REMAINING_COST_EXCEPTION);
               }
               cell.setValue(value);
               //set the flag that indicates that the user modified the remaining cost for this cost type for this activity
               activityChoice = getActivity(dataRow);
               if (activityChoice != null && getCostType(dataRow) != null) {
                  byte costType = getCostType(dataRow).byteValue();
                  setRemainingCostModifiedByUser(activityChoice, costType, true);
                  //update the remaining cost for all cells with the same activity - cost type combination
                  updateRemainingCostCells(activityChoice, costType, ((Double) value).doubleValue());
               }
               break;

            case ATTACHMENT_INDEX:
               List attachmentList = (List) value;
               if (!attachmentList.isEmpty()) {
                  setAttribute(dataRow, HAS_ATTACHMENTS, true);
               }
               else {
                  setAttribute(dataRow, HAS_ATTACHMENTS, false);
               }

               cell.setValue(value);
               break;

            default:
               cell.setValue(value);
               break;
         }
      }
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

         String activityChoice = getActivity(dataRow);
         Byte costType = getCostType(dataRow);
         if (getValue(dataRow, ACTIVITY_TYPE_INDEX) != null) {
            //get the type of the activity
            byte activityType = ((Byte) getValue(dataRow, ACTIVITY_TYPE_INDEX)).byteValue();
            if (activityChoice != null && costType != null && activityType != OpGanttValidator.ADHOC_TASK) {
               //check if the current row is the last one with the activity - cost type combination
               if (countRowsWithSameActivityCostType(activityChoice, costType.byteValue()) == 1) {
                  setRemainingCostModifiedByUser(activityChoice, costType.byteValue(), false);
               }
               /* if the current row is not the last one with the activity - cost type combination
                  and the user did not modify the remaining cost by hand, add the actual effort of the
                  deleted row to the remaining of the other similar rows */
               else {
                  if (!getRemainingCostModifiedByUser(activityChoice, costType.byteValue())) {
                     double newRemaining = ((Double) getValue(dataRow, REMAINING_COST_INDEX)).doubleValue() +
                          ((Double) getValue(dataRow, ACTUAL_COST_INDEX)).doubleValue();
                     updateRemainingCostCells(activityChoice, costType.byteValue(), newRemaining);
                  }
               }
            }
         }
         data_set.removeChild(dataRow);
      }
      return true;
   }

   public void setAttribute(XComponent dataRow, int attribute, boolean value) {
      int attributes = ((Integer) getValue(dataRow, INDICATOR_INDEX)).intValue();
      if (value) {
         attributes |= attribute;
      }
      else {
         attributes -= (attributes & attribute);
      }
      setValue(dataRow, INDICATOR_INDEX, new Integer(attributes));
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
         if (activityChoice == null) {
            //check if the old activity + cost type combination is unique
            //check if the cost type cell is filled
            if (getCostType(dataRow) != null && oldActivityValue != null) {
               byte costType = getCostType(dataRow).byteValue();
               //the activity choice from the reset row was already set to null so we check if there is another row
               //with the same activity - cost type combination
               if (countRowsWithSameActivityCostType(oldActivityValue, costType) == 0) {
                  //reset the flag that indicates that the user manually modified the remaining cost for this combination
                  //of oldActivityValue - cost type
                  setRemainingCostModifiedByUser(oldActivityValue, costType, false);
               }
               //update the remaining cost for the remaining rows with the same activity - cost type combination
               else {
                  double remainingCost = ((XComponent) dataRow.getChild(REMAINING_COST_INDEX)).getDoubleValue();
                  double actualCost = ((XComponent) dataRow.getChild(ACTUAL_COST_INDEX)).getDoubleValue();
                  updateRemainingCostCells(oldActivityValue, costType, remainingCost + actualCost);
               }
            }

            setValue(dataRow, BASE_COST_INDEX, new Double(0));
            setValue(dataRow, ACTUAL_COST_INDEX, new Double(0));
            setValue(dataRow, ORIGINAL_REMAINING_COST_INDEX, new Double(0));
            setValue(dataRow, REMAINING_COST_INDEX, new Double(0));
         }
      }

   }

   /**
    * Sets the base cost cell value and the original remaining cost cell value.
    *
    * @param activityChoice - the choice of the activity for which the costs are set
    * @param dataRow        - the data row that is being updated.
    */
   private void updateCostCells(String activityChoice, XComponent dataRow) {
      List costsList;

      Map costsMap = getCostsMap(activityChoice);
      if (!costsMap.keySet().isEmpty()) {
         byte costType = Byte.parseByte(XValidator.choiceID(((XComponent) dataRow.getChild(COST_TYPE_INDEX)).getStringValue()));
         double sumOfActualCosts = sumOfActualCosts(activityChoice, costType);
         costsList = (List) costsMap.get(new Byte(costType));

         //set the base cost cell value
         if (dataRow.getChild(REMAINING_COST_INDEX).getEnabled()) {
            setValue(dataRow, BASE_COST_INDEX, costsList.get(0));
            //reset the actual cost cell value
            setValue(dataRow, ACTUAL_COST_INDEX, new Double(0));
            //set the original remaining cost cell value
            setValue(dataRow, ORIGINAL_REMAINING_COST_INDEX, costsList.get(1));
            setValue(dataRow, ACTUAL_REMAINING_SUM_INDEX, costsList.get(1));
         }

         //if the remaining cost was not manually modified by the user
         if (dataRow.getChild(REMAINING_COST_INDEX).getEnabled()) {
            if (!getRemainingCostModifiedByUser(activityChoice, costType)) {
               double remainingCost = 0d;
               //if this is the first cost with the activity choice - cost type combination
               if (countRowsWithSameActivityCostType(activityChoice, costType) == 1) {
                  //set the remaining cost cell value to the original remaining - sum of actual costs with the same pair activity - cost type
                  remainingCost = ((Double) costsList.get(1)).doubleValue() - sumOfActualCosts;
               }
               else {
                  //set the remaining cost cell value to one of the remaining costs of a similar cost (a cost with the same
                  //    activity choice - cost type combination)
                  remainingCost = getRemainingCostFromSimilarRows(activityChoice, costType, dataRow.getIndex());
               }

               if (remainingCost < 0) {
                  remainingCost = 0d;
               }
               setValue(dataRow, REMAINING_COST_INDEX, new Double(remainingCost));
            }
            //set the remaining cost from a row with the same activity - cost type combination
            else {
               setValue(dataRow, REMAINING_COST_INDEX, new Double(getRemainingCostFromSimilarRows(activityChoice, costType, dataRow.getIndex())));
            }
         }
      }
   }

   /**
    * Returns a <code>boolean</code> value which is <code>true</code> if the remaining cost for the
    * activity specified by the activityChoice and for the type specified by costType was manually
    * modified by the user and <code>false</code> otherwise.
    *
    * @param activityChoice - the choice of the activity for which the remaining cost modification is interrogated
    * @param costType       - the cost type for which the remaining cost modification is interrogated
    * @return <code>true</code> if the remaining cost for the activity specified by the activityChoice and
    *         for the type specified by costType was manually modified by the user and <code>false</code> otherwise.
    */
   private boolean getRemainingCostModifiedByUser(String activityChoice, byte costType) {
      List costsList;

      Map costsMap = getCostsMap(activityChoice);
      if (!costsMap.keySet().isEmpty()) {
         costsList = (List) costsMap.get(new Byte(costType));
         return ((Boolean) costsList.get(2)).booleanValue();
      }
      return false;
   }

   /**
    * Sets a flag with the given value indicating that the remaining cost for the
    * activity specified by the activityChoice and for the type specified by costType was manually
    * modified by the user.
    *
    * @param activityChoice - the choice of the activity for which the remaining cost modification is updated
    * @param costType       - the cost type for which the remaining cost modification is updated
    * @param value          - the <code>boolean</code> value which will be set on the flag
    */
   private void setRemainingCostModifiedByUser(String activityChoice, byte costType, boolean value) {
      List costsList;

      Map costsMap = getCostsMap(activityChoice);
      if (!costsMap.keySet().isEmpty()) {
         costsList = (List) costsMap.get(new Byte(costType));
         costsList.set(2, new Boolean(value));
      }
   }

   /**
    * Returns the map of costs for the activity which has the choice passed as parameter
    *
    * @param activityChoice - the activity choice for which the choice map is returned
    * @return the map of costs for the activity which has the choice passed as parameter.
    */
   private Map getCostsMap(String activityChoice) {
      Map costsMap = new HashMap();
      XComponent choiceActivityRow;

      XComponent activityChoiceDataSet = this.data_set.getForm().findComponent(ACTIVITY_SET);
      //find the activity in the activity choice set
      for (int i = 0; i < activityChoiceDataSet.getChildCount(); i++) {
         choiceActivityRow = (XComponent) activityChoiceDataSet.getChild(i);
         if (choiceActivityRow.getStringValue().equals(activityChoice)) {
            //get the costs map for this activity
            costsMap = (Map) ((XComponent) choiceActivityRow.getChild(ACTIVITY_CHOICE_SET_ACTIVITY_COSTS_INDEX)).getValue();
         }
      }
      return costsMap;
   }

   /**
    * Returns the sum of the actual cost for all the rows in the cost data set which have the specified activity choice
    * and cost type set on them
    *
    * @param activityChoice - the choice of the activity for which the sum will be calculated
    * @param costType       -  the cost type cof which the sum will be calculated
    * @return the sum of the actual cost for all the rows in the cost data set which have the specified activity choice
    *         and cost type set on them.
    */
   private double sumOfActualCosts(String activityChoice, byte costType) {
      XComponent costRow;
      double sum = 0d;

      XComponent costDataSet = this.data_set;
      //find the activity in the activity choice set
      for (int i = 0; i < costDataSet.getChildCount(); i++) {
         costRow = (XComponent) costDataSet.getChild(i);
         String costRowActivityChoice = ((XComponent) costRow.getChild(ACTIVITY_NAME_INDEX)).getStringValue();
         Byte costRowCostType = getCostType(costRow);
         //we have found a cost for the given activity and the given cost type
         if (activityChoice.equals(costRowActivityChoice) && costRowCostType != null && costType == costRowCostType.byteValue()) {
            sum += ((XComponent) costRow.getChild(ACTUAL_COST_INDEX)).getDoubleValue();
         }
      }

      return sum;
   }

   /**
    * Updates all the remaining cost cells for all the rows in the cost data set which have the specified activity
    * choice and cost type set on them to the newRemaining value
    *
    * @param activityChoice - the choice of the activity for which the remaining cost will be updated
    * @param costType       -  the cost type for which the remaining cost will be updated
    * @param newRemaining   - the new remaining cost value
    */
   private void updateRemainingCostCells(String activityChoice, byte costType, double newRemaining) {
      XComponent costRow;

      XComponent costDataSet = this.data_set;
      for (int i = 0; i < costDataSet.getChildCount(); i++) {
         costRow = (XComponent) costDataSet.getChild(i);
         String costRowActivityChoice = ((XComponent) costRow.getChild(ACTIVITY_NAME_INDEX)).getStringValue();
         Byte costRowCostType = getCostType(costRow);
         //we have found a cost for the given activity and the given cost type
         if (activityChoice.equals(costRowActivityChoice) && costRowCostType != null && costType == costRowCostType.byteValue()) {
            ((XComponent) costRow.getChild(REMAINING_COST_INDEX)).setDoubleValue(newRemaining);
         }
      }
   }

   /**
    * Conts all the rows in the cost data set which have the specified activity choice and cost type set on them
    *
    * @param activityChoice - the choice of the activity for which the rows will be counted
    * @param costType       -  the cost type cof which the rows will be counted
    * @return the number of rows in the cost data set which have the specified activity choice and cost type set on them
    */
   private int countRowsWithSameActivityCostType(String activityChoice, byte costType) {
      int numberOfRows = 0;
      XComponent costRow;

      XComponent costDataSet = this.data_set;
      for (int i = 0; i < costDataSet.getChildCount(); i++) {
         costRow = (XComponent) costDataSet.getChild(i);
         String costRowActivityChoice = ((XComponent) costRow.getChild(ACTIVITY_NAME_INDEX)).getStringValue();
         Byte costRowCostType = getCostType(costRow);
         if (costRowActivityChoice != null && costRowCostType != null) {
            //we have found a row for the given activity and the given cost type
            if (activityChoice.equals(costRowActivityChoice) && costType == costRowCostType.byteValue()) {
               numberOfRows++;
            }
         }
      }
      return numberOfRows;
   }

   /**
    * Updates the remaining costs for the data rows which have the same activity - cost type combination as the
    * deleted row.
    *
    * @param dataRow - the <code>XComponent</code> deleted row.
    */
   private void updateRemainingCostsAtDeletion(XComponent dataRow) {
      String activityChoice = getActivity(dataRow);
      if (getCostType(dataRow) != null && activityChoice != null) {
         byte costType = getCostType(dataRow).byteValue();
         if (countRowsWithSameActivityCostType(activityChoice, costType) == 1) {
            //reset the flag that indicates that the user manually modified the remaining cost for this combination
            //of activity - cost type
            setRemainingCostModifiedByUser(activityChoice, costType, false);
         }
         else {
            //update the remaining cost for the remaining rows with the same activity - cost type combination
            //if the remaining cost was not manually modified by the user
            if (!getRemainingCostModifiedByUser(activityChoice, costType)) {
               double remainingCost = ((XComponent) dataRow.getChild(REMAINING_COST_INDEX)).getDoubleValue();
               double actualCost = ((XComponent) dataRow.getChild(ACTUAL_COST_INDEX)).getDoubleValue();
               updateRemainingCostCells(activityChoice, costType, remainingCost + actualCost);
            }
         }
      }
   }

   /**
    * Returns the remaining cost cell value from a data row which has the activityChoice- costType combination set on it.
    *
    * @param activityChoice - the choice of the activity for which the remaining cost will be returned
    * @param costType -  the cost type for which the remaining cost will be returned
    * @param dataRowIndex - the index of the data row that is being edited
    * @return the remaining cost cell value from a data row which has the activityChoice - costType combination set on it.
    *    If no other row with the same activityChoice - costType combination is found the method returns -1.
    */
   private double getRemainingCostFromSimilarRows(String activityChoice, byte costType, int dataRowIndex){
      double remainingCost = -1;
      XComponent costRow;

      XComponent costDataSet = this.data_set;
      for (int i = 0; i < costDataSet.getChildCount(); i++) {
         costRow = (XComponent) costDataSet.getChild(i);
         String costRowActivityChoice = ((XComponent) costRow.getChild(ACTIVITY_NAME_INDEX)).getStringValue();
         Byte costRowCostType = getCostType(costRow);
         if (costRowActivityChoice != null && costRowCostType != null) {
            //we have found a row for the given activity and the given cost type
            if (activityChoice.equals(costRowActivityChoice) && costType == costRowCostType.byteValue()) {
               //be sure not to return the remaining cost of the row that's being edited
               remainingCost = ((XComponent) costRow.getChild(REMAINING_COST_INDEX)).getDoubleValue();
               if (costRow.getIndex() != dataRowIndex) {
                  return remainingCost;
               }
            }
         }
      }
      return remainingCost;
   }

   /**
    * Returns the activity type of the activity selected in the activity choice data set.
    *
    * @param cell - the <code>XComponent</code> data cell containing the name of the activity whose type will be returned
    * @return the activity type of the activity selected in the activity choice data set.
    */
   private byte getActivityType(XComponent cell) {
      XComponent activityChoiceDataSet = getActivitySet();
      for (int i = 0; i < activityChoiceDataSet.getChildCount(); i++) {
         XComponent choiceActivityRow = (XComponent) activityChoiceDataSet.getChild(i);
         String activityName = ((XComponent) cell.getParent().getChild(ACTIVITY_NAME_INDEX)).getStringValue();
         if (choiceActivityRow.getStringValue().equals(activityName)) {
            //get the activity type
            return ((XComponent) choiceActivityRow.getChild(ACTIVITY_CHOICE_SET_ACTIVITY_TYPE_INDEX)).getByteValue();
         }
      }
      return (byte) -1;
   }
}