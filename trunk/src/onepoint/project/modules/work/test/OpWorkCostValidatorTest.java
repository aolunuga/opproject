/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.express.XComponent;
import onepoint.express.server.XFormSchema;
import onepoint.project.modules.work.OpCostRecord;
import onepoint.project.modules.work.validators.OpWorkCostValidator;
import onepoint.project.test.OpTestCase;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test case for OpWorkCostValidatorClass
 *
 * @author florin.haizea
 */
public class OpWorkCostValidatorTest extends OpTestCase {

   /**
    * The name of the xml file that contains the test data.
    */
   private static final String TEST_DATA_FILENAME = "workCostValidatorTestData.xml";

   /**
    * The cost validator that is tested.
    */
   private OpWorkCostValidator validator = null;

   private static final String PROJECT_CHOICE = "OpProjectNode.xid.10['Project']";
   private static final String ACTIVITY_ONE_CHOICE = "OpActivity.xid.1['ActivityOne']";
   private static final String ACTIVITY_TWO_CHOICE = "OpActivity.xid.2['ActivityTwo']";
   private static final String RESOURCE_ONE_CHOICE = "OpResource.xid.1['ResourceOne']";
   private static final String RESOURCE_TWO_CHOICE = "OpResource.xid.2['ResourceTwo']";

   private final double DOUBLE_ERROR_MARGIN = Math.pow(10, -4);

   /**
    * @see junit.framework.TestCase#setUp()
    */
   public void setUp()
        throws Exception {
      super.setUp();

      XComponent testDataSet = getTestDataSet(TEST_DATA_FILENAME);

      validator = getValidator();
      validator.setDataSet(testDataSet);
   }

   /**
    * Loads a test data set from the given file.
    *
    * @param testDataFile a <code>xml</code> file containing a data set that will be used
    *                     for testing.
    * @return a <code>XComponent(DATA_SET)</code> containing test data for the validator.
    */
   private XComponent getTestDataSet(String testDataFile) {
      XLoader xmlLoader = new XLoader(new XDocumentHandler(new XFormSchema()));
      InputStream testDataInputStream = this.getClass().getResourceAsStream(testDataFile);
      XComponent testForm = (XComponent) xmlLoader.loadObject(testDataInputStream, null);

      //set the project/activity/resource choice sets on the form
      fillProjectChoiceDataSet(testForm.findComponent(OpWorkCostValidator.PROJECT_SET));
      fillActivityChoiceDataSet(testForm.findComponent(OpWorkCostValidator.ACTIVITY_SET));
      fillResourceChoiceDataSet(testForm.findComponent(OpWorkCostValidator.RESOURCE_SET));

      //set the assignment map field
      testForm.findComponent("AssignmentMap").setValue(createAssignmentMap());

      //work cost data set.
      return (XComponent) testForm.getChild(4);
   }

   /**
    * Fills the <code>XComponent</code> data set passed as parameter with the project choice values.
    *
    * @param - the <code>XComponent</code> data set which is going to be filled.
    * @return a <code>XComponent(DATA_SET)</code>.
    */
   private XComponent fillProjectChoiceDataSet(XComponent projectSet) {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(PROJECT_CHOICE);

      //add the project - activity map
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      List<String> activityList = new ArrayList<String>();
      activityList.add(ACTIVITY_ONE_CHOICE);
      activityList.add(ACTIVITY_TWO_CHOICE);
      dataCell.setListValue(activityList);
      dataRow.addChild(dataCell);

      //add the project - resource map
      dataCell = new XComponent(XComponent.DATA_CELL);
      List<String> resourceList = new ArrayList<String>();
      resourceList.add(RESOURCE_ONE_CHOICE);
      resourceList.add(RESOURCE_TWO_CHOICE);
      dataCell.setListValue(resourceList);
      dataRow.addChild(dataCell);

      projectSet.addChild(dataRow);
      return projectSet;
   }

   /**
    * Fills the <code>XComponent</code> data set passed as parameter with the activity choice values.
    *
    * @param - the <code>XComponent</code> data set which is going to be filled.
    * @return a <code>XComponent(DATA_SET)</code>.
    */
   private XComponent fillActivityChoiceDataSet(XComponent activitySet) {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(ACTIVITY_ONE_CHOICE);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setByteValue((byte)0);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(createActivityOneCostMap());
      dataRow.addChild(dataCell);

      //add the activity - resource map
      dataCell = new XComponent(XComponent.DATA_CELL);
      List<String> resourceList = new ArrayList<String>();
      resourceList.add(RESOURCE_ONE_CHOICE);
      resourceList.add(RESOURCE_TWO_CHOICE);
      dataCell.setListValue(resourceList);
      dataRow.addChild(dataCell);

      activitySet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(ACTIVITY_TWO_CHOICE);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setByteValue((byte)0);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(createActivityTwoCostMap());
      dataRow.addChild(dataCell);

      //add the activity - resource map
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setListValue(resourceList);
      dataRow.addChild(dataCell);
      activitySet.addChild(dataRow);
      return activitySet;
   }

   /**
    * Creates the <code>Map<Byte, List></code> of costs for activity one. The map will have as keys the type of costs,
    *    and as values lists containing the base cost, remaining cost and "modified by user" boolean value for each
    *    cost type.
    * @return the <code>Map<Byte, List></code> of costs for activity one.
    */
   private Map<Byte, List> createActivityOneCostMap() {
      //form the map of costs for this activity
      Map<Byte, List> costsMap = new HashMap<Byte, List>();

      List travelCostInfo = new ArrayList();
      //add base travel cost to list
      travelCostInfo.add(new Double (50));
      //add remaining travel cost to list
      travelCostInfo.add(new Double(5));
      //add "modified by user" info to list
      travelCostInfo.add(false);
      costsMap.put(OpCostRecord.TRAVEL_COST, travelCostInfo);

      List materialCostInfo = new ArrayList();
      materialCostInfo.add(new Double(30));
      materialCostInfo.add(new Double(10));
      materialCostInfo.add(new Boolean(false));
      costsMap.put(OpCostRecord.MATERIAL_COST, materialCostInfo);

      List externalCostInfo = new ArrayList();
      externalCostInfo.add(new Double(100));
      externalCostInfo.add(new Double(50));
      externalCostInfo.add(new Boolean(true));
      costsMap.put(OpCostRecord.EXTERNAL_COST, externalCostInfo);

      List miscCostInfo = new ArrayList();
      miscCostInfo.add(new Double(70));
      miscCostInfo.add(new Double(70));
      miscCostInfo.add(new Boolean(false));
      costsMap.put(OpCostRecord.MISCELLANEOUS_COST, miscCostInfo);

      return costsMap;
   }

    /**
    * Creates the <code>Map<Byte, List></code> of costs for activity two. The map will have as keys the type of costs,
    *    and as values lists containing the base cost, remaining cost and "modified by user" boolean value for each
    *    cost type.
    * @return the <code>Map<Byte, List></code> of costs for activity two.
    */
   private Map<Byte, List> createActivityTwoCostMap() {
      //form the map of costs for this activity
      Map<Byte, List> costsMap = new HashMap<Byte, List>();

      List travelCostInfo = new ArrayList();
      //add base travel cost to list
      travelCostInfo.add(new Double (50));
      //add remaining travel cost to list
      travelCostInfo.add(new Double(50));
      //add "modified by user" info to list
      travelCostInfo.add(false);
      costsMap.put(OpCostRecord.TRAVEL_COST, travelCostInfo);

      List materialCostInfo = new ArrayList();
      materialCostInfo.add(new Double(75));
      materialCostInfo.add(new Double(55));
      materialCostInfo.add(new Boolean(false));
      costsMap.put(OpCostRecord.MATERIAL_COST, materialCostInfo);

      List externalCostInfo = new ArrayList();
      externalCostInfo.add(new Double(60));
      externalCostInfo.add(new Double(45));
      externalCostInfo.add(new Boolean(false));
      costsMap.put(OpCostRecord.EXTERNAL_COST, externalCostInfo);

      List miscCostInfo = new ArrayList();
      miscCostInfo.add(new Double(20));
      miscCostInfo.add(new Double(15));
      miscCostInfo.add(new Boolean(true));
      costsMap.put(OpCostRecord.MISCELLANEOUS_COST, miscCostInfo);

      return costsMap;
   }

   /**
    * Fills the <code>XComponent</code> data set passed as parameter with the resource choice values.
    *
    * @param - the <code>XComponent</code> data set which is going to be filled.
    * @return a <code>XComponent(DATA_SET)</code>.
    */
   private XComponent fillResourceChoiceDataSet(XComponent resourceSet) {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(RESOURCE_ONE_CHOICE);

      //add the resource - activity map
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      List<String> activityList = new ArrayList<String>();
      activityList.add(ACTIVITY_ONE_CHOICE);
      activityList.add(ACTIVITY_TWO_CHOICE);
      dataCell.setListValue(activityList);
      dataRow.addChild(dataCell);
      resourceSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(RESOURCE_TWO_CHOICE);

      //add the resource - activity map
      dataCell = new XComponent(XComponent.DATA_CELL);     
      dataCell.setListValue(activityList);
      dataRow.addChild(dataCell);
      resourceSet.addChild(dataRow);
      return resourceSet;
   }

   /**
    * Creates a <code>Map</code>: Key:assignment.activity choice - assignment.resource choice
    *                             Value: List - 0 - assignment's base effort
    *                                           1 - assignment's locator
    *                                           2 - assignment' remaining effort
    *                                           3 - boolean value indicating if the remaining effort was modified
    *                                                 manually by the user
    *
    * @return a <code>Map</code>: Key:assignment.activity choice - assignment.resource choice
    *                             Value: List - 0 - assignment's base effort
    *                                           1 - assignment's locator
    *                                           2 - assignment' remaining effort
    *                                           3 - boolean value indicating if the remaining effort was modified
    *                                                 manually by the user
    */
   private Map<String, List> createAssignmentMap() {
      Map<String, List> assignmentMap = new HashMap<String, List>();
      List dataList = new ArrayList();
      dataList.add(new Double(40d));
      dataList.add("OpAssignment.xid.1['']");
      dataList.add(new Double(40d));
      dataList.add(false);
      assignmentMap.put(ACTIVITY_ONE_CHOICE + "-" + RESOURCE_ONE_CHOICE, dataList);

      dataList = new ArrayList();
      dataList.add(new Double(40d));
      dataList.add("OpAssignment.xid.2['']");
      dataList.add(new Double(40d));
      dataList.add(false);
      assignmentMap.put(ACTIVITY_ONE_CHOICE + "-" + RESOURCE_TWO_CHOICE, dataList);

      dataList = new ArrayList();
      dataList.add(new Double(40d));
      dataList.add("OpAssignment.xid.3['']");
      dataList.add(new Double(40d));
      dataList.add(false);
      assignmentMap.put(ACTIVITY_TWO_CHOICE + "-" + RESOURCE_ONE_CHOICE, dataList);

      dataList = new ArrayList();
      dataList.add(new Double(40d));
      dataList.add("OpAssignment.xid.4['']");
      dataList.add(new Double(40d));
      dataList.add(false);
      assignmentMap.put(ACTIVITY_TWO_CHOICE + "-" + RESOURCE_TWO_CHOICE, dataList);

      return assignmentMap;
   }

   protected OpWorkCostValidator getValidator() {
      return new OpWorkCostValidator();
   }

   /**
    * Tests the removal of data rows from the data set.
    *
    * @throws Exception if anything fails.
    */
   public void testRemoveDataRows()
        throws Exception {

      //get the "modified by user" value for the activity two - miscellaneous cost combination
      assertTrue(getRemainingCostModifiedByUser(ACTIVITY_TWO_CHOICE, OpCostRecord.MISCELLANEOUS_COST));

      //remove the last row: Project   ActivityTwo    ResourceTwo    MiscellaneousCost    20    5     15
      List removedRows = new ArrayList();
      removedRows.add(validator.getDataSet().getChild(8));
      validator.removeDataRows(removedRows);

      //now, the "modified by user" value for the activity two - miscellaneous cost combination must be set to false
      assertFalse(getRemainingCostModifiedByUser(ACTIVITY_TWO_CHOICE, OpCostRecord.MISCELLANEOUS_COST));

      //get the remaining cost for the activity one - travel cost
      double remainingTravelCost = ((XComponent)((XComponent)validator.getDataSet().getChild(0)).getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getDoubleValue();
      assertEquals(remainingTravelCost, 5d, DOUBLE_ERROR_MARGIN);

      //remove the first row: Project   ActivityOne    ResourceOne    TravelCost           50    25    5
      removedRows = new ArrayList();
      removedRows.add(validator.getDataSet().getChild(0));
      validator.removeDataRows(removedRows);

      //now, the remaining cost value for the activity two - miscellaneous cost combination must 30
      remainingTravelCost = ((XComponent)((XComponent)validator.getDataSet().getChild(1)).getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getDoubleValue();
      assertEquals(remainingTravelCost, 30d, DOUBLE_ERROR_MARGIN);

      //get the remaining cost for the activity one - external cost
      double remainingExternalCost = ((XComponent)((XComponent)validator.getDataSet().getChild(6)).getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getDoubleValue();
      assertEquals(remainingExternalCost, 7d, DOUBLE_ERROR_MARGIN);

      //remove the row: Project   ActivityOne    ResourceTwo    ExternalCost         100   55    7
      removedRows = new ArrayList();
      removedRows.add(validator.getDataSet().getChild(6));
      validator.removeDataRows(removedRows);

      //now, the remaining cost value for the activity one - external cost combination must 7 because the remaining cost was manually
      //modified by the user
      remainingExternalCost = ((XComponent)((XComponent)validator.getDataSet().getChild(5)).getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getDoubleValue();
      assertEquals(remainingExternalCost, 7d, DOUBLE_ERROR_MARGIN);
   }

   /**
    * Tests the setting of a value on the activity cell in the work data set.
    *
    * @throws Exception if anything fails.
    */
   public void testSetActivityCellValue()
      throws Exception {

      //first, we set the second activity value on a new row that has no other values set on it
      XComponent newDataRow = validator.newDataRow();
      validator.getDataSet().addChild(newDataRow);
      validator.setDataCellValue(newDataRow, OpWorkCostValidator.ACTIVITY_NAME_INDEX, ACTIVITY_TWO_CHOICE);

      //since the cost type was not set on the data row, the base cost cell value and remaining cost cell value will
      //be the default ones
      assertEquals(0d, ((XComponent)newDataRow.getChild(OpWorkCostValidator.BASE_COST_INDEX)).getDoubleValue(), DOUBLE_ERROR_MARGIN);
      assertEquals(0d, ((XComponent)newDataRow.getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getDoubleValue(), DOUBLE_ERROR_MARGIN);

      //second, we set the second activity on a new data row that has the project choice and the cost type set on it
      newDataRow = validator.newDataRow();
      validator.getDataSet().addChild(newDataRow);
      validator.setDataCellValue(newDataRow, OpWorkCostValidator.PROJECT_NAME_INDEX, PROJECT_CHOICE);
      validator.setDataCellValue(newDataRow, OpWorkCostValidator.COST_TYPE_INDEX, "2['TravelCost']");
      validator.setDataCellValue(newDataRow, OpWorkCostValidator.ACTIVITY_NAME_INDEX, ACTIVITY_TWO_CHOICE);
      
      assertEquals(50d, ((XComponent)newDataRow.getChild(OpWorkCostValidator.BASE_COST_INDEX)).getDoubleValue(), DOUBLE_ERROR_MARGIN);
      assertEquals(50d, ((XComponent)newDataRow.getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getDoubleValue(), DOUBLE_ERROR_MARGIN);

      //third, we set the second activity on a data row that had all the cells values previously set
      //we set the second activity on the row: Project   ActivityOne    ResourceOne    TravelCost           50    25    5
      validator.setDataCellValue((XComponent)validator.getDataSet().getChild(0), OpWorkCostValidator.ACTIVITY_NAME_INDEX, ACTIVITY_TWO_CHOICE);

      //the remaining cost on the third row was modified
      Double remainingCost = ((XComponent) validator.getDataSet().getChild(2).getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getDoubleValue();
      assertEquals(30d, remainingCost, DOUBLE_ERROR_MARGIN);
      //the base cost and remaining cost on the first row was modified
      Double baseCost = ((XComponent) validator.getDataSet().getChild(0).getChild(OpWorkCostValidator.BASE_COST_INDEX)).getDoubleValue();
      remainingCost = ((XComponent) validator.getDataSet().getChild(0).getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getDoubleValue();
      assertEquals(50d, baseCost, DOUBLE_ERROR_MARGIN);
      assertEquals(50d, remainingCost, DOUBLE_ERROR_MARGIN);
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

      XComponent activityChoiceDataSet = validator.getDataSet().getForm().findComponent(OpWorkCostValidator.ACTIVITY_SET);
      //find the activity in the activity choice set
      for (int i = 0; i < activityChoiceDataSet.getChildCount(); i++) {
         choiceActivityRow = (XComponent) activityChoiceDataSet.getChild(i);
         if (choiceActivityRow.getStringValue().equals(activityChoice)) {
            //get the costs map for this activity
            costsMap = (Map) ((XComponent) choiceActivityRow.getChild(OpWorkCostValidator.ACTIVITY_CHOICE_SET_ACTIVITY_COSTS_INDEX)).getValue();
         }
      }
      return costsMap;
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

}
