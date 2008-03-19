package onepoint.project.modules.work.validators;

import onepoint.express.XComponent;
import onepoint.express.XValidator;

import java.util.List;
import java.util.Map;

/**
 * Base class for work validators.
 *
 * @author mihai.costin
 */
public abstract class OpWorkValidator extends XValidator {

   //indexes in the choice data sets
   public static int PROJECT_CHOICE_SET_ACTIVITY_INDEX = 0;
   public static int PROJECT_CHOICE_SET_RESOURCE_INDEX = 1;
   public static int PROJECT_PROGRESS_TRACKED_INDEX = 2;

   public static int RESOURCE_CHOICE_SET_ACTIVITY_INDEX = 0;

   public static int ACTIVITY_CHOICE_SET_ACTIVITY_TYPE_INDEX = 0;
   public static int ACTIVITY_CHOICE_SET_ACTIVITY_COSTS_INDEX = 1;
   public static int ACTIVITY_CHOICE_SET_RESOURCE_INDEX = 2;

   //validation exceptions
   protected static final String PROJECT_NAME_EXCEPTION = "ProjectNameException";
   protected static final String ACTIVITY_NAME_EXCEPTION = "ActivityNameException";
   protected static final String RESOURCE_NAME_EXCEPTION = "ResourceNameException";

   //choice data sets
   protected XComponent resourceSet;
   protected XComponent projectSet;
   protected XComponent activitySet;

   //indexes in the assignment map
   protected static final int ASSIGNMENT_BASE_EFFORT_INDEX = 0;
   protected static final int ASSIGNMENT_LOCATOR_INDEX = 1;
   protected static final int ASSIGNMENT_REMAINING_EFFORT_INDEX = 2;
   protected static final int ASSIGNMENT_REMAINING_MODIFIED_INDEX = 3;

   //assignment map
   private static final String ASSIGNMENT_MAP = "AssignmentMap";
   private XComponent assignmentMap;

   //pulsing
   private static final String PULSING = "Pulsing";
   private Integer NO_PULSING = new Integer(-1);
   private Integer pulsing;


   /**
    * Gets the value of the data cell at the given index.
    *
    * @param row   Row from the data set to get the value from.
    * @param index index of the desired value.
    * @return the values stored at the given index in the data row.
    */
   public Object getValue(XComponent row, int index) {
      XComponent dataCell = (XComponent) row.getChild(index);
      return dataCell.getValue();
   }

   /**
    * Sets the given value on the data cell at <code>index</code> in the data row <code>row</row>
    *
    * @param row   data row to set the value on
    * @param index index of the value (of the data cell that will store the value)
    * @param value value to be set
    */
   public void setValue(XComponent row, int index, Object value) {
      XComponent dataCell = (XComponent) row.getChild(index);
      dataCell.setValue(value);
   }

   /**
    * Adds a new data row to this validator's data set.
    *
    * @param data_row a <code>XComponent</code> representing a data row.
    */
   public void addDataRow(XComponent data_row) {
      data_set.addChild(data_row);
   }

   /**
    * Adds a new data row at the specified position in the validator's dataset.
    *
    * @param index    a <code>int</code> representing the index in the data set where the row should be added.
    * @param data_row a <code>XComponent</code> representing the data row that will be added.
    */
   public void addDataRow(int index, XComponent data_row) {
      data_set.addChild(index, data_row);
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
         data_set.removeChild(dataRow);
      }
      return true;
   }


   /**
    * Gets the assignment map.
    *
    * @return assignment map.
    */
   protected XComponent getAssignmentMap() {
      if (assignmentMap == null) {
         assignmentMap = new XComponent(XComponent.DATA_FIELD);
         XComponent form = data_set.getForm();
         if (form != null) {
            assignmentMap = form.findComponent(ASSIGNMENT_MAP);
         }
      }
      return assignmentMap;
   }

   /**
    * Gets the pulsing property value.
    *
    * @return pulsing pulsing value. Null if it wasn't set.
    */
   protected Integer getPulsing() {

      if (pulsing == null) {
         XComponent form = data_set.getForm();
         if (form != null) {
            pulsing = (Integer) form.findComponent(PULSING).getValue();
         }
         if (pulsing == null) {
            pulsing = NO_PULSING;
         }
      }

      if (NO_PULSING.equals(pulsing)) {
         return null;
      }
      else {
         return pulsing;
      }

   }

   /**
    * Sets the locator of the assignment determined by the activity choice & the resource choice on the data row.
    *
    * @param activityChoice - the choice of the assignment's activity
    * @param resourceChoice - the choice of the assignment's resource
    * @param dataRow        - the <code>XComponent</code> data row that will be updated.
    */
   protected void setAssignmentLocatorOnDataRow(String activityChoice, String resourceChoice, XComponent dataRow) {
      Map assignmentMap = (Map) getAssignmentMap().getValue();
      List assignmentInfoList = (List) assignmentMap.get(activityChoice + "-" + resourceChoice);
      dataRow.setValue(assignmentInfoList.get(ASSIGNMENT_LOCATOR_INDEX));
   }

   /**
    * Gets the project choice set.
    *
    * @return project set.
    */
   protected abstract XComponent getProjectSet();

   /**
    * Gets the activity choice set.
    *
    * @return activity set.
    */
   protected abstract XComponent getActivitySet();

   /**
    * Gets the resource choice set.
    *
    * @return resource set.
    */
   protected abstract XComponent getResourceSet();

   public abstract void setProject(XComponent dataRow, String choice);

   public abstract String getProject(XComponent dataRow);

   public abstract String getActivity(XComponent dataRow);

   public abstract void setActivity(XComponent dataRow, String choice);

   public abstract String getResource(XComponent dataRow);

   public abstract void setResource(XComponent dataRow, String choice);

   /**
    * Sets the resource choice value on the given data row and filters the activity choice set accordingly.
    *
    * @param cell   Data cell that will have the new resource value set.
    * @param choice Resource chocie
    */
   protected void setResourceChoiceValue(XComponent cell, String choice) {

      cell.setValue(choice);
      XComponent dataRow = (XComponent) cell.getParent();

      filterActivities(dataRow, choice);

      //if the activity name was selected set the locator of the assignment on the data row
      if (getActivity(dataRow) != null) {
         setAssignmentLocatorOnDataRow(getActivity(dataRow), cell.getStringValue(), dataRow);
      }
   }

   public void filterActivities(XComponent dataRow, String resourceChoice) {
      showAll(getActivitySet());

      filterByProject(getProject(dataRow), getActivitySet(), PROJECT_CHOICE_SET_ACTIVITY_INDEX);

      //filter the activities by resource
      if (resourceChoice != null) {
         XComponent resourceRow = null;
         for (int i = 0; i < getResourceSet().getChildCount(); i++) {
            XComponent row = (XComponent) getResourceSet().getChild(i);
            if (resourceChoice.equals(row.getStringValue())) {
               resourceRow = row;
               break;
            }
         }

         if (resourceRow != null) {
            List activityList = (List) ((XComponent) resourceRow.getChild(RESOURCE_CHOICE_SET_ACTIVITY_INDEX)).getValue();
            filterDataSet(getActivitySet(), activityList);
         }
      }

      //add any advanced filtering that is necessary at this point
      advancedFilteringForActivity(getActivity(dataRow), resourceChoice);

      if (getActivity(dataRow) != null && !getActivitySet().contains(-1, getActivity(dataRow))) {
         resetRow(dataRow, getActivity(dataRow), getResource(dataRow));
         setActivity(dataRow, null);
      }
   }

   /**
    * Sets the activity choice value on the given data row and filters the resource choice set accordingly.
    *
    * @param cell   Data cell that will have the new activity value set.
    * @param choice Activity chocie
    */
   protected void setActivityChoiceValue(XComponent cell, String choice) {

      cell.setValue(choice);
      XComponent dataRow = (XComponent) cell.getParent();
      filterResources(dataRow, choice);

      //if the resource name was selected set the locator of the assignment on the data row
      if (getResource(dataRow) != null) {
         setAssignmentLocatorOnDataRow(cell.getStringValue(), getResource(dataRow), dataRow);
      }

      //set the project choice on the project name cell
      setProject(dataRow, getProjectChoiceByActivity(choice));
      filterActivities(dataRow, getResource(dataRow));
   }

   public void filterResources(XComponent dataRow, String activityChoice) {
      //filter resources by project
      showAll(getResourceSet());
      filterByProject(getProject(dataRow), getResourceSet(), PROJECT_CHOICE_SET_RESOURCE_INDEX);

      //filter the resources by activity
      if (activityChoice != null) {
         XComponent activityRow = null;
         for (int i = 0; i < getActivitySet().getChildCount(); i++) {
            XComponent row = (XComponent) getActivitySet().getChild(i);
            if (activityChoice.equals(row.getStringValue())) {
               activityRow = row;
               break;
            }
         }

         if (activityRow != null) {
            List resourceList = (List) ((XComponent) activityRow.getChild(ACTIVITY_CHOICE_SET_RESOURCE_INDEX)).getValue();
            filterDataSet(getResourceSet(), resourceList);

            //if the activity has only one resource assigned to it and that resurce is not selected set it on the data row
            if (resourceList.size() == 1) {
               setResource(dataRow, (String) resourceList.get(0));
            }
         }
      }

      //add any advanced filtering that is necessary at this point
      advancedFilteringForResource(getResource(dataRow), activityChoice);

      if (getResource(dataRow) != null && !getResourceSet().contains(-1, getResource(dataRow))) {
         setResource(dataRow, null);
         resetRow(dataRow, getActivity(dataRow), getResource(dataRow));
      }
   }

   /**
    * Sets the project choice value on the given data row and filters the resource and activity choice sets accordingly.
    *
    * @param cell   Data cell that will have the new project choice value set.
    * @param choice Project choice
    */
   protected void setProjectChoiceValue(XComponent cell, String choice) {

      cell.setValue(choice);
      XComponent dataRow = (XComponent) cell.getParent();

      showAll(getActivitySet());
      filterByProject(choice, getActivitySet(), PROJECT_CHOICE_SET_ACTIVITY_INDEX);

      showAll(getResourceSet());
      filterByProject(choice, getResourceSet(), PROJECT_CHOICE_SET_RESOURCE_INDEX);

      //reset the activity and resource values
      String oldActivity = getActivity(dataRow);
      String oldResource = getResource(dataRow);
      setActivity(dataRow, null);
      setResource(dataRow, null);
      resetRow(dataRow, oldActivity, oldResource);
   }

   /**
    * Resets the values on the row
    *
    * @param dataRow          - the <code>XComponent</code> data row that will be reset
    * @param oldActivityValue - the value that was set on the activity cell before the reset
    * @param oldResourceValue - the value that was set on the resource cell before the reset
    */
   protected void resetRow(XComponent dataRow, String oldActivityValue, String oldResourceValue) {
      String resourceChoice = getResource(dataRow);
      String activityChoice = getActivity(dataRow);

      if (activityChoice == null || resourceChoice == null) {
         dataRow.setValue(null);
      }
   }

   /**
    * Filter the given data set by project locator.
    *
    * @param choice          Project Choice
    * @param dataSetToFilter data set to be filtered
    * @param index           index of the filtering information kept in the project row.
    */
   private void filterByProject(String choice, XComponent dataSetToFilter, int index) {
      if (choice != null) {
         //find the project row
         XComponent projectRow = null;
         for (int i = 0; i < getProjectSet().getChildCount(); i++) {
            XComponent row = (XComponent) getProjectSet().getChild(i);
            if (row.getStringValue().equals(choice)) {
               projectRow = row;
               break;
            }
         }
         if (projectRow != null) {
            List valuesList = (List) ((XComponent) projectRow.getChild(index)).getValue();
            filterDataSet(dataSetToFilter, valuesList);
         }
      }
   }

   /**
    * Removes all the filters from the data set.
    *
    * @param dataSet Data Set
    */
   private void showAll(XComponent dataSet) {
      //make visible all rows
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         dataRow.setFiltered(false);
      }
   }

   /**
    * Filters the data set. Only the given entries in the list will be visible.
    *
    * @param dataSet    Data set to be filtered.
    * @param valuesList List of visible rows (values)
    */
   private void filterDataSet(XComponent dataSet, List valuesList) {
      for (int j = 0; j < dataSet.getChildCount(); j++) {
         XComponent row = (XComponent) dataSet.getChild(j);
         String choice = row.getStringValue();
         if (!valuesList.contains(choice)) {
            row.setFiltered(true);
         }
      }
   }

   /**
    * Returns the choice of the project which has the activity whose choice is passed as parameter.
    *
    * @param activityChoice - the choice of the activity for which the project choice is returned
    * @return the choice of the project which has the activity whose choice is passed as parameter.
    */
   protected String getProjectChoiceByActivity(String activityChoice) {
      String projectChoice = null;
      XComponent projectRow;
      List activityChoiceList;

      for (int i = 0; i < getProjectSet().getChildCount(); i++) {
         projectRow = (XComponent) getProjectSet().getChild(i);
         activityChoiceList = ((XComponent) projectRow.getChild(0)).getListValue();
         for (int j = 0; j < activityChoiceList.size(); j++) {
            if (activityChoiceList.get(j).equals(activityChoice)) {
               return projectRow.getStringValue();
            }
         }
      }

      return projectChoice;
   }

   /**
    * Overriden in OpWorkEffortValidator
    */
   protected void advancedFilteringForResource(String oldResourceChoice, String activityChoice) {
   }

   /**
    * Overriden in OpWorkEffortValidator
    */
   protected void advancedFilteringForActivity(String oldActivityChoice, String resourceChoice) {
   }

   /**
    * Returns the project row, from the validator's project set, which corresponds to the project choice on the data row
    *    passed as parameter.
    *
    * @param dataRow - the <code>XComponent</code> data row containing the project choice which will be used to find the
    *                project row.
    * @return the project row, from the validator's project set, which corresponds to the project choice on the data row
    *         passed as parameter.
    */
   protected XComponent getProjectForRow(XComponent dataRow) {
      String projectChoice = getProject(dataRow);
      XComponent projectRow = null;
      for (int i = 0; i < getProjectSet().getChildCount(); i++) {
         XComponent row = (XComponent) getProjectSet().getChild(i);
         if (row.getStringValue().equals(projectChoice)) {
            projectRow = row;
            break;
         }
      }

      return projectRow;
   }

   /**
    * Gets the progress tracked flag for the project on the row passed as parameter. The project cell must be set on
    *    this row.
    *
    * @param dataRow - the <code>XComponent</code> data row containing the project for which the tracking will be returned.
    * @return the progress tracked flag for the project on the row passed as parameter.
    */
   protected boolean isProgressTracked(XComponent dataRow) {
      XComponent projectRow = getProjectForRow(dataRow);

      if (projectRow != null) {
         XComponent dataCell = (XComponent) projectRow.getChild(PROJECT_PROGRESS_TRACKED_INDEX);
         return dataCell.getBooleanValue();
      }
      else {
         return getProgressTrackedFromTimeSet(dataRow);
      }
   }

   /**
    * Overriden in OpWorkEffortValidator.
    *
    * @param dataRow
    * @return
    */
   protected boolean getProgressTrackedFromTimeSet(XComponent dataRow) {
      return true;
   }

   /**
    * Gets the activity type for the given activity caption by searching in the activity choice data set.
    * @param value
    * @return
    */
   public Byte getActivityTypeForChoice(Object value) {
      Byte activityType = null;
      //set the activity type cell value
      XComponent activityChoiceDataSet = getActivitySet();
      //find the activity in the activity choice set
      for (int i = 0; i < activityChoiceDataSet.getChildCount(); i++) {
         XComponent choiceActivityRow = (XComponent) activityChoiceDataSet.getChild(i);
         if (choiceActivityRow.getStringValue().equals(value)) {
            //set the activity type
            activityType = (Byte) ((XComponent) choiceActivityRow.getChild(ACTIVITY_CHOICE_SET_ACTIVITY_TYPE_INDEX)).getValue();
         }
      }
      return activityType;
   }
}
