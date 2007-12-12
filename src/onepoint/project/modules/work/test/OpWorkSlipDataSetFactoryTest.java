/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.test.OpActivityTestDataFactory;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.modules.work.OpCostRecord;
import onepoint.project.modules.work.OpTimeRecord;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlipDataSetFactory;
import onepoint.project.modules.work.validators.OpWorkCostValidator;
import onepoint.project.modules.work.validators.OpWorkEffortValidator;
import onepoint.project.modules.work.validators.OpWorkTimeValidator;
import onepoint.project.test.OpBaseOpenTestCase;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test class for testing the methods in OpWorkSlipDataSetFactory
 *
 * @author florin.haizea
 */
public class OpWorkSlipDataSetFactoryTest extends OpBaseOpenTestCase {

   private OpWorkTestDataFactory dataFactory;
   private OpProjectTestDataFactory projectFactory;
   private OpActivityTestDataFactory activityFactory;
   private OpResourceTestDataFactory resourceFactory;

   private static final String PROJECT_NAME = "project";
   private static final String ACTIVITY_NAME = "activity";
   private static final String RESOURCE_NAME = "resource";

   private final double DOUBLE_ERROR_MARGIN = Math.pow(10, -4);

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      dataFactory = new OpWorkTestDataFactory(session);
      projectFactory = new OpProjectTestDataFactory(session);
      activityFactory = new OpActivityTestDataFactory(session);
      resourceFactory = new OpResourceTestDataFactory(session);
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      clean();
      super.tearDown();
   }

   /**
    * test method for creating a list of XComponent data sets out of a List of OpWorkRecord entities
    * - the list contains no work records
    *
    * @throws Exception
    */
   public void testFormDataSetsFromWorkRecordsNoWR()
        throws Exception {
      List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();

      OpBroker broker = session.newBroker();
      List<XComponent> dataSets = OpWorkSlipDataSetFactory.formDataSetsFromWorkRecords(workRecords, session, broker);
      assertEquals(3, dataSets.size());
      broker.close();

      //the work record set should have ono rows because the are no work records
      XComponent workRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.WORK_RECORD_SET_INDEX);
      assertEquals(0, workRecordDataSet.getChildCount());

      //the time record set should have no rows because the work records has no time records attached
      XComponent timeRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.TIME_RECORD_SET_INDEX);
      assertEquals(0, timeRecordDataSet.getChildCount());

      //the cost record set should have no rows because the work records has no cost records attached
      XComponent costRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.COST_RECORD_SET_INDEX);
      assertEquals(0, costRecordDataSet.getChildCount());
   }

   /**
    * test method for creating a list of XComponent data sets out of a List of OpWorkRecord entities
    * - the list contains one work record with no time records and no cost records attached
    *
    * @throws Exception
    */
   public void testFormDataSetsFromWorkRecordsOneWR()
        throws Exception {
      prepareTest();

      String workRecordId = dataFactory.getWorkRecordId(RESOURCE_NAME);
      OpWorkRecord workRecord = dataFactory.getWorkRecordById(workRecordId);
      List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();
      workRecords.add(workRecord);

      OpBroker broker = session.newBroker();
      List<XComponent> dataSets = OpWorkSlipDataSetFactory.formDataSetsFromWorkRecords(workRecords, session, broker);
      assertEquals(3, dataSets.size());
      broker.close();

      //the work record set should have only one row corresponding to the work record
      XComponent workRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.WORK_RECORD_SET_INDEX);
      assertEquals(1, workRecordDataSet.getChildCount());

      XComponent dataRow = (XComponent) workRecordDataSet.getChild(0);
      assertTrue(OpWorkEffortDataSetFactoryTest.isEntityInDataRow(workRecord, dataRow));

      //the time record set should have no rows because the work records has no time records attached
      XComponent timeRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.TIME_RECORD_SET_INDEX);
      assertEquals(0, timeRecordDataSet.getChildCount());

      //the cost record set should have no rows because the work records has no cost records attached
      XComponent costRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.COST_RECORD_SET_INDEX);
      assertEquals(0, costRecordDataSet.getChildCount());
   }

   /**
    * test method for creating a list of XComponent data sets out of a List of OpWorkRecord entities
    * - the list contains one work record with one time record and one cost record attached
    */

   public void testFormDataSetsFromWorkRecordsOneWROneTROneCR()
        throws Exception {
      prepareTest();

      String workRecordId = dataFactory.getWorkRecordId(RESOURCE_NAME);
      OpWorkRecord workRecord = dataFactory.getWorkRecordById(workRecordId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create the OpCostRecord
      OpCostRecord costRecord1 = new OpCostRecord();
      costRecord1.setType(OpCostRecord.TRAVEL_COST);
      costRecord1.setActualCosts(20d);
      costRecord1.setRemainingCosts(10d);
      costRecord1.setComment("Comment cost record 1");
      costRecord1.setWorkRecord(workRecord);

      //create the OpTimeRecord
      OpTimeRecord timeRecord1 = new OpTimeRecord();
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord1, 50, 100, 50);
      timeRecord1.setWorkRecord(workRecord);

      broker.makePersistent(costRecord1);
      broker.makePersistent(timeRecord1);

      t.commit();
      broker.close();

      workRecord = dataFactory.getWorkRecordById(workRecordId);
      List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();
      workRecords.add(workRecord);

      broker = session.newBroker();
      List<XComponent> dataSets = OpWorkSlipDataSetFactory.formDataSetsFromWorkRecords(workRecords, session, broker);
      assertEquals(3, dataSets.size());
      broker.close();

      //the work record set should have only one row corresponding to the work record
      XComponent workRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.WORK_RECORD_SET_INDEX);
      assertEquals(1, workRecordDataSet.getChildCount());

      XComponent dataRow = (XComponent) workRecordDataSet.getChild(0);
      assertTrue(OpWorkEffortDataSetFactoryTest.isEntityInDataRow(workRecord, dataRow));

      //the time record set should have one row corresponding to the OpTimeRecord in the work record's time set
      XComponent timeRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.TIME_RECORD_SET_INDEX);
      assertEquals(1, timeRecordDataSet.getChildCount());
      assertTrue(OpTimeRecordDataSetFactoryTest.isEntityInDataSet(timeRecord1, timeRecordDataSet));

      //the cost record set should have one row corresponding to the OpCostRecord in the work record's cost set
      XComponent costRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.COST_RECORD_SET_INDEX);
      assertEquals(1, costRecordDataSet.getChildCount());
      assertTrue(OpCostRecordDataSetFactoryTest.isEntityInDataSet(costRecord1, costRecordDataSet));
   }

   /**
    * test method for creating a list of XComponent data sets out of a List of OpWorkRecord entities
    * - the list contains two work records with no time records and no cost records attached
    */
   public void testFormDataSetsFromWorkRecordsTwoWR()
        throws Exception {
      prepareTest();

      String workRecordId = dataFactory.getWorkRecordId(RESOURCE_NAME);
      OpWorkRecord workRecord1 = dataFactory.getWorkRecordById(workRecordId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create another assignment - resource
      OpResource resource = new OpResource();
      resource.setName(RESOURCE_NAME + 1);
      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(workRecord1.getAssignment().getActivity());
      assignment.setResource(resource);
      assignment.setProjectPlan(workRecord1.getAssignment().getProjectPlan());

      //create another workRecord
      OpWorkRecord workRecord2 = new OpWorkRecord();
      workRecord2.setActualEffort(10d);
      workRecord2.setAssignment(assignment);
      workRecord2.setComment("Comment work record 2");

      broker.makePersistent(resource);
      broker.makePersistent(assignment);
      broker.makePersistent(workRecord2);

      t.commit();
      broker.close();

      workRecord1 = dataFactory.getWorkRecordById(workRecordId);
      String workRecordId2 = dataFactory.getWorkRecordId(RESOURCE_NAME + 1);
      workRecord2 = dataFactory.getWorkRecordById(workRecordId2);
      List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();
      workRecords.add(workRecord1);
      workRecords.add(workRecord2);

      broker = session.newBroker();
      List<XComponent> dataSets = OpWorkSlipDataSetFactory.formDataSetsFromWorkRecords(workRecords, session, broker);
      assertEquals(3, dataSets.size());
      broker.close();

      //the work record set should have two rows corresponding to the work records
      XComponent workRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.WORK_RECORD_SET_INDEX);
      assertEquals(2, workRecordDataSet.getChildCount());

      XComponent dataRow1 = (XComponent) workRecordDataSet.getChild(0);
      assertTrue(OpWorkEffortDataSetFactoryTest.isEntityInDataRow(workRecord1, dataRow1));
      XComponent dataRow2 = (XComponent) workRecordDataSet.getChild(1);
      assertTrue(OpWorkEffortDataSetFactoryTest.isEntityInDataRow(workRecord2, dataRow2));

      //the time record set should have no rows because the work records has no time records attached
      XComponent timeRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.TIME_RECORD_SET_INDEX);
      assertEquals(0, timeRecordDataSet.getChildCount());

      //the cost record set should have no rows because the work records has no cost records attached
      XComponent costRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.COST_RECORD_SET_INDEX);
      assertEquals(0, costRecordDataSet.getChildCount());
   }

   /* test method for creating a list of XComponent data sets out of a List of OpWorkRecord entities
      - the list contains two work records:
         - the first work record has two time records and one cost record
         - the second work record has one time record and one cost record
    */

   public void testFormDataSetsFromWorkRecordsTwoWRThreeTRTwoCR()
        throws Exception {
      prepareTest();

      String workRecordId = dataFactory.getWorkRecordId(RESOURCE_NAME);
      OpWorkRecord workRecord1 = dataFactory.getWorkRecordById(workRecordId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create another assignment - resource
      OpResource resource = new OpResource();
      resource.setName(RESOURCE_NAME + 1);
      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(workRecord1.getAssignment().getActivity());
      assignment.setResource(resource);
      assignment.setProjectPlan(workRecord1.getAssignment().getProjectPlan());

      //create another workRecord
      OpWorkRecord workRecord2 = new OpWorkRecord();
      workRecord2.setAssignment(assignment);
      workRecord2.setActualEffort(10d);
      workRecord2.setComment("Comment work record 2");

      //create the OpCostRecords
      OpCostRecord costRecord1 = new OpCostRecord();
      costRecord1.setType(OpCostRecord.TRAVEL_COST);
      costRecord1.setActualCosts(20d);
      costRecord1.setRemainingCosts(10d);
      costRecord1.setComment("Comment cost record 1");
      costRecord1.setWorkRecord(workRecord1);
      OpCostRecord costRecord2 = new OpCostRecord();
      costRecord2.setType(OpCostRecord.MATERIAL_COST);
      costRecord2.setActualCosts(25d);
      costRecord2.setRemainingCosts(25d);
      costRecord2.setComment("Comment cost record 2");
      costRecord2.setWorkRecord(workRecord2);

      //create the OpTimeRecord
      OpTimeRecord timeRecord1 = new OpTimeRecord();
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord1, 50, 100, 50);
      timeRecord1.setWorkRecord(workRecord1);
      OpTimeRecord timeRecord2 = new OpTimeRecord();
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord2, 150, 170, 20);
      timeRecord2.setWorkRecord(workRecord1);
      OpTimeRecord timeRecord3 = new OpTimeRecord();
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord3, 30, 35, 5);
      timeRecord3.setWorkRecord(workRecord2);

      broker.makePersistent(resource);
      broker.makePersistent(assignment);
      broker.makePersistent(workRecord2);

      broker.makePersistent(costRecord1);
      broker.makePersistent(costRecord2);
      broker.makePersistent(timeRecord1);
      broker.makePersistent(timeRecord2);
      broker.makePersistent(timeRecord3);

      t.commit();
      broker.close();

      workRecord1 = dataFactory.getWorkRecordById(workRecordId);
      String workRecordId2 = dataFactory.getWorkRecordId(RESOURCE_NAME + 1);
      workRecord2 = dataFactory.getWorkRecordById(workRecordId2);
      List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();
      workRecords.add(workRecord1);
      workRecords.add(workRecord2);

      broker = session.newBroker();
      List<XComponent> dataSets = OpWorkSlipDataSetFactory.formDataSetsFromWorkRecords(workRecords, session, broker);
      assertEquals(3, dataSets.size());
      broker.close();

      //the work record set should have two rows corresponding to the work records
      XComponent workRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.WORK_RECORD_SET_INDEX);
      assertEquals(2, workRecordDataSet.getChildCount());

      XComponent dataRow1 = (XComponent) workRecordDataSet.getChild(0);
      assertTrue(OpWorkEffortDataSetFactoryTest.isEntityInDataRow(workRecord1, dataRow1));
      XComponent dataRow2 = (XComponent) workRecordDataSet.getChild(1);
      assertTrue(OpWorkEffortDataSetFactoryTest.isEntityInDataRow(workRecord2, dataRow2));

      //the time record set should have three rows corresponding to the OpTimeRecords in the work records time sets
      XComponent timeRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.TIME_RECORD_SET_INDEX);
      assertEquals(3, timeRecordDataSet.getChildCount());
      assertTrue(OpTimeRecordDataSetFactoryTest.isEntityInDataSet(timeRecord1, timeRecordDataSet));
      assertTrue(OpTimeRecordDataSetFactoryTest.isEntityInDataSet(timeRecord2, timeRecordDataSet));
      assertTrue(OpTimeRecordDataSetFactoryTest.isEntityInDataSet(timeRecord3, timeRecordDataSet));

      //the cost record set should have two rows corresponding to the OpCostRecords in the work records cost sets
      XComponent costRecordDataSet = dataSets.get(OpWorkSlipDataSetFactory.COST_RECORD_SET_INDEX);
      assertEquals(2, costRecordDataSet.getChildCount());
      assertTrue(OpCostRecordDataSetFactoryTest.isEntityInDataSet(costRecord1, costRecordDataSet));
      assertTrue(OpCostRecordDataSetFactoryTest.isEntityInDataSet(costRecord2, costRecordDataSet));
   }

   /* test method for creating a list of OpWorkRecords from three data sets which contain information about the work
      records, time records and cost records
      - the data sets contain no rows
   */
   public void testFormWorkRecordsFromDataSetsNoWR()
        throws Exception {
      XComponent workRecordDataSet = new XComponent(XComponent.DATA_SET);
      XComponent timeRecordDataSet = new XComponent(XComponent.DATA_SET);
      XComponent costRecordDataSet = new XComponent(XComponent.DATA_SET);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      List<OpWorkRecord> workRecords = OpWorkSlipDataSetFactory.formWorkRecordsFromDataSets(broker, workRecordDataSet, timeRecordDataSet, costRecordDataSet);
      assertTrue(workRecords.isEmpty());

      t.commit();
      broker.close();
   }

   /* test method for creating a list of OpWorkRecords from three data sets which contain information about the work
      records, time records and cost records
      - the work record data set contains one row
   */
   public void testFormWorkRecordsFromDataSetsOneWR()
        throws Exception {
      XComponent workRecordDataSet = new XComponent(XComponent.DATA_SET);
      XComponent timeRecordDataSet = new XComponent(XComponent.DATA_SET);
      XComponent costRecordDataSet = new XComponent(XComponent.DATA_SET);
      prepareTest();

      String workRecordId = dataFactory.getWorkRecordId(RESOURCE_NAME);
      OpWorkRecord workRecord = dataFactory.getWorkRecordById(workRecordId);

      OpAssignment assignment = workRecord.getAssignment();
      OpActivity activity = assignment.getActivity();
      OpResource resource = assignment.getResource();
      OpProjectNode project = activity.getProjectPlan().getProjectNode();

      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);

      //0 - project name
      dataCell.setStringValue(XValidator.choice(project.locator(), project.getName()));
      dataRow.addChild(OpWorkEffortValidator.PROJECT_NAME_INDEX, dataCell);
      //1 - activity name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(activity.locator(), activity.getName()));
      dataRow.addChild(OpWorkEffortValidator.ACTIVITY_NAME_INDEX, dataCell);
      //2 - resource name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
      dataRow.addChild(OpWorkEffortValidator.RESOURCE_NAME_INDEX, dataCell);
      //3 - planned effort
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(workRecord.getAssignment().getBaseEffort());
      dataRow.addChild(OpWorkEffortValidator.PLANNED_EFFORT_INDEX, dataCell);
      //4 - actual effort
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(workRecord.getActualEffort());
      dataRow.addChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX, dataCell);
      //5 - remaining effort
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(workRecord.getRemainingEffort());
      dataRow.addChild(OpWorkEffortValidator.REMAINING_EFFORT_INDEX, dataCell);
      //6 - completed
      dataCell = new XComponent(XComponent.DATA_CELL);
      if (workRecord.getAssignment().getProjectPlan().getProgressTracked() || activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setBooleanValue(workRecord.getCompleted());
      }
      else {
         dataCell.setValue(null);
      }
      dataRow.addChild(OpWorkEffortValidator.COMPLETED_INDEX, dataCell);
      //7 - comment
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("Comment work record");
      dataRow.addChild(OpWorkEffortValidator.COMMENTS_INDEX, dataCell);
      //8 - original remaining
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(workRecord.getRemainingEffort());
      dataRow.addChild(OpWorkEffortValidator.ORIGINAL_REMAINING_INDEX, dataCell);
      //9 - activity type
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(activity.getType());
      dataRow.addChild(OpWorkEffortValidator.ACTIVITY_TYPE_INDEX, dataCell);
      //10 - activity created
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(true);
      dataRow.addChild(OpWorkEffortValidator.ACTIVITY_CREATED_INDEX, dataCell);
      //assignment locator as data row value
      dataRow.setStringValue(assignment.locator());
      workRecordDataSet.addChild(dataRow);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      List<OpWorkRecord> workRecords = OpWorkSlipDataSetFactory.formWorkRecordsFromDataSets(broker, workRecordDataSet, timeRecordDataSet, costRecordDataSet);

      t.commit();
      broker.close();

      assertEquals(1, workRecords.size());
      assertTrue(OpWorkEffortDataSetFactoryTest.hasEntityFieldsInDataRow(workRecords.get(0), (XComponent) workRecordDataSet.getChild(0)));
      assertTrue(workRecords.get(0).getTimeRecords().isEmpty());
      assertTrue(workRecords.get(0).getCostRecords().isEmpty());
   }

   /* test method for creating a list of OpWorkRecords from three data sets which contain information about the work
      records, time records and cost records
      - the work record data set contains one row
      - the time record data set contains one row
      - the cost record data set contains one row
   */
   public void testFormWorkRecordsFromDataSetsOneWROneTRTwoCR()
        throws Exception {
      XComponent workRecordDataSet = new XComponent(XComponent.DATA_SET);
      XComponent timeRecordDataSet = new XComponent(XComponent.DATA_SET);
      XComponent costRecordDataSet = new XComponent(XComponent.DATA_SET);
      prepareTest();

      String workRecordId = dataFactory.getWorkRecordId(RESOURCE_NAME);
      OpWorkRecord workRecord = dataFactory.getWorkRecordById(workRecordId);

      String idActivity = activityFactory.getActivityId(ACTIVITY_NAME);
      OpActivity activity = activityFactory.getActivityById(idActivity);

      String projectId = projectFactory.getProjectId(PROJECT_NAME);
      OpBroker broker = session.newBroker();
      OpProjectNode project = projectFactory.getProjectById(broker, projectId);

      OpTransaction t = broker.newTransaction();

      //create a new assignment for another resource
      OpResource resource = new OpResource();
      resource.setName(RESOURCE_NAME + 1);
      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resource);
      assignment.setProjectPlan(project.getPlan());

      //create the OpCostRecord which belong to a work record
      OpCostRecord costRecord1 = new OpCostRecord();
      costRecord1.setType(OpCostRecord.TRAVEL_COST);
      costRecord1.setActualCosts(20d);
      costRecord1.setRemainingCosts(10d);
      costRecord1.setComment("Comment cost record 1");
      costRecord1.setWorkRecord(workRecord);

      //create the OpTimeRecord
      OpTimeRecord timeRecord1 = new OpTimeRecord();
      OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord1, 50, 100, 50);
      timeRecord1.setWorkRecord(workRecord);

      broker.makePersistent(resource);
      broker.makePersistent(assignment);
      broker.makePersistent(costRecord1);
      broker.makePersistent(timeRecord1);

      t.commit();
      broker.close();

      workRecord = dataFactory.getWorkRecordById(workRecordId);

      assignment = workRecord.getAssignment();
      activity = assignment.getActivity();
      resource = assignment.getResource();
      project = activity.getProjectPlan().getProjectNode();

      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);

      //fill the work record data set
      //0 - project name
      dataCell.setStringValue(XValidator.choice(project.locator(), project.getName()));
      dataRow.addChild(OpWorkEffortValidator.PROJECT_NAME_INDEX, dataCell);
      //1 - activity name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(activity.locator(), activity.getName()));
      dataRow.addChild(OpWorkEffortValidator.ACTIVITY_NAME_INDEX, dataCell);
      //2 - resource name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
      dataRow.addChild(OpWorkEffortValidator.RESOURCE_NAME_INDEX, dataCell);
      //3 - planned effort
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(workRecord.getAssignment().getBaseEffort());
      dataRow.addChild(OpWorkEffortValidator.PLANNED_EFFORT_INDEX, dataCell);
      //4 - actual effort
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(workRecord.getActualEffort());
      dataRow.addChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX, dataCell);
      //5 - remaining effort
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(workRecord.getRemainingEffort());
      dataRow.addChild(OpWorkEffortValidator.REMAINING_EFFORT_INDEX, dataCell);
      //6 - completed
      dataCell = new XComponent(XComponent.DATA_CELL);
      if (workRecord.getAssignment().getProjectPlan().getProgressTracked() || activity.getType() == OpActivity.ADHOC_TASK) {
         dataCell.setBooleanValue(workRecord.getCompleted());
      }
      else {
         dataCell.setValue(null);
      }
      dataRow.addChild(OpWorkEffortValidator.COMPLETED_INDEX, dataCell);
      //7 - comment
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("Comment work record");
      dataRow.addChild(OpWorkEffortValidator.COMMENTS_INDEX, dataCell);
      //8 - original remaining
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(workRecord.getRemainingEffort());
      dataRow.addChild(OpWorkEffortValidator.ORIGINAL_REMAINING_INDEX, dataCell);
      //9 - activity type
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(activity.getType());
      dataRow.addChild(OpWorkEffortValidator.ACTIVITY_TYPE_INDEX, dataCell);
      //10 - activity created
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(true);
      dataRow.addChild(OpWorkEffortValidator.ACTIVITY_CREATED_INDEX, dataCell);
      //assignment locator as data row value
      dataRow.setStringValue(assignment.locator());
      workRecordDataSet.addChild(dataRow);

      //fill the time record data set
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      //0 - project name
      dataCell.setStringValue(XValidator.choice(project.locator(), project.getName()));
      dataRow.addChild(OpWorkTimeValidator.PROJECT_NAME_INDEX, dataCell);
      //1 - activity name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(activity.locator(), activity.getName()));
      dataRow.addChild(OpWorkTimeValidator.ACTIVITY_NAME_INDEX, dataCell);
      //2 - resource name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
      dataRow.addChild(OpWorkTimeValidator.RESOURCE_NAME_INDEX, dataCell);
      //3 - start time
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(50);
      dataRow.addChild(OpWorkTimeValidator.START_INDEX, dataCell);
      //4 - finish time
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(100);
      dataRow.addChild(OpWorkTimeValidator.FINISH_INDEX, dataCell);
      //5 - duration
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(50);
      dataRow.addChild(OpWorkTimeValidator.DURATION_INDEX, dataCell);
      //assignment locator as data row value
      dataRow.setStringValue(assignment.locator());
      timeRecordDataSet.addChild(dataRow);

      //fill the cost record data set
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      //0 - project name
      dataCell.setStringValue(XValidator.choice(project.locator(), project.getName()));
      dataRow.addChild(OpWorkCostValidator.PROJECT_NAME_INDEX, dataCell);
      //1 - activity name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(activity.locator(), activity.getName()));
      dataRow.addChild(OpWorkCostValidator.ACTIVITY_NAME_INDEX, dataCell);
      //2 - resource name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
      dataRow.addChild(OpWorkCostValidator.RESOURCE_NAME_INDEX, dataCell);
      //3 - indicator
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(0);
      dataRow.addChild(OpWorkCostValidator.INDICATOR_INDEX, dataCell);
      //4 - cost type
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(String.valueOf(OpCostRecord.TRAVEL_COST), "Travel Cost"));
      dataRow.addChild(OpWorkCostValidator.COST_TYPE_INDEX, dataCell);
      //5 - base cost
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(30d);
      dataRow.addChild(OpWorkCostValidator.BASE_COST_INDEX, dataCell);
      //6 - actual cost
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(20d);
      dataRow.addChild(OpWorkCostValidator.ACTUAL_COST_INDEX, dataCell);
      //7 - remaining cost
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(10d);
      dataRow.addChild(OpWorkCostValidator.REMAINING_COST_INDEX, dataCell);
      //8 - comment
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("Comment cost record 1");
      dataRow.addChild(OpWorkCostValidator.COMMENTS_COST_INDEX, dataCell);
      //9 - set the attachements
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(new ArrayList());
      dataRow.addChild(OpWorkCostValidator.ATTACHMENT_INDEX, dataCell);
      //assignment locator as data row value
      dataRow.setStringValue(assignment.locator());
      costRecordDataSet.addChild(dataRow);

      //add a cost record which does not belong to any work records
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      //0 - project name
      dataCell.setStringValue(XValidator.choice(project.locator(), project.getName()));
      dataRow.addChild(OpWorkCostValidator.PROJECT_NAME_INDEX, dataCell);
      //1 - activity name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(activity.locator(), activity.getName()));
      dataRow.addChild(OpWorkCostValidator.ACTIVITY_NAME_INDEX, dataCell);
      //2 - resource name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
      dataRow.addChild(OpWorkCostValidator.RESOURCE_NAME_INDEX, dataCell);
      //3 - indicator
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(0);
      dataRow.addChild(OpWorkCostValidator.INDICATOR_INDEX, dataCell);
      //4 - cost type
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(String.valueOf(OpCostRecord.MATERIAL_COST), "Material Cost"));
      dataRow.addChild(OpWorkCostValidator.COST_TYPE_INDEX, dataCell);
      //5 - base cost
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(10d);
      dataRow.addChild(OpWorkCostValidator.BASE_COST_INDEX, dataCell);
      //6 - actual cost
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(5d);
      dataRow.addChild(OpWorkCostValidator.ACTUAL_COST_INDEX, dataCell);
      //7 - remaining cost
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(5d);
      dataRow.addChild(OpWorkCostValidator.REMAINING_COST_INDEX, dataCell);
      //8 - comment
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("Comment cost record 2");
      dataRow.addChild(OpWorkCostValidator.COMMENTS_COST_INDEX, dataCell);
      //9 - set the attachments
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(new ArrayList());
      dataRow.addChild(OpWorkCostValidator.ATTACHMENT_INDEX, dataCell);

      String resourceId = resourceFactory.getResourceId(RESOURCE_NAME + 1);
      resource = resourceFactory.getResourceById(resourceId);
      for (OpAssignment resourceAssignment : resource.getActivityAssignments()) {
         assignment = resourceAssignment;
      }

      //assignment locator as data row value
      dataRow.setStringValue(assignment.locator());
      costRecordDataSet.addChild(dataRow);

      //make a copy of the cost data set
      XComponent costRecordDataSetCopy = new XComponent(XComponent.DATA_SET);
      for (int i = 0; i < costRecordDataSet.getChildCount(); i++) {
         costRecordDataSetCopy.addChild(((XComponent) costRecordDataSet.getChild(i)).copyData());
      }

      broker = session.newBroker();
      t = broker.newTransaction();

      XComponent recordDataSet = timeRecordDataSet.copyData();
      List<OpWorkRecord> workRecords = OpWorkSlipDataSetFactory.formWorkRecordsFromDataSets(broker, workRecordDataSet, timeRecordDataSet, costRecordDataSet);

      assertEquals(2, workRecords.size());
      for (OpWorkRecord resultWorkRecord : workRecords) {
         //check validity of the work record which had the cost record attached
         if (resultWorkRecord.getAssignment().getResource().getName().equals(RESOURCE_NAME)) {
            assertTrue(OpWorkEffortDataSetFactoryTest.hasEntityFieldsInDataRow(resultWorkRecord, (XComponent) workRecordDataSet.getChild(0)));
            assertEquals(1, resultWorkRecord.getTimeRecords().size());
            for (OpTimeRecord timeRecord : resultWorkRecord.getTimeRecords()) {
               assertTrue(OpTimeRecordDataSetFactoryTest.hasEntityFieldsInDataSet(timeRecord, recordDataSet));
            }
         }
         //check the validity of the "empty" work record
         else {
            assertFalse(resultWorkRecord.getCompleted());
            assertEquals(0, resultWorkRecord.getActualEffort(), DOUBLE_ERROR_MARGIN);
            assertEquals(((XComponent) costRecordDataSetCopy.getChild(1)).getStringValue(), resultWorkRecord.getAssignment().locator());
         }
         assertEquals(1, resultWorkRecord.getCostRecords().size());
         for (OpCostRecord costRecord : resultWorkRecord.getCostRecords()) {
            assertTrue(OpCostRecordDataSetFactoryTest.hasEntityFieldsInDataSet(costRecord, costRecordDataSetCopy));
         }
      }

      t.commit();
      broker.close();
   }

   /**
    * Test for getListOfSubordinateResourceIds() in OpWorkSlipDataSetFactory.
    *
    * @throws Exception if the test fails
    */
   public void testGetListOfSubordinateResourceIds()
        throws Exception {
      //there are no resources which are subordinate to the current user
      OpBroker broker = session.newBroker();
      OpUser user = (OpUser) (broker.getObject(OpUser.class, session.getUserID()));
      List resourceIds = OpWorkSlipDataSetFactory.getListOfSubordinateResourceIds(session, broker);
      broker.close();

      assertEquals(0, resourceIds.size());

      //insert two resources which are subordinate to the user
      broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpResource resource = new OpResource();
      resource.setName(RESOURCE_NAME);
      resource.setUser(user);
      OpResource resource1 = new OpResource();
      resource1.setName(RESOURCE_NAME + 1);
      resource1.setUser(user);
      broker.makePersistent(resource);
      broker.makePersistent(resource1);

      t.commit();
      resourceIds = OpWorkSlipDataSetFactory.getListOfSubordinateResourceIds(session, broker);
      broker.close();

      assertEquals(2, resourceIds.size());
      String resourceLocator = resourceFactory.getResourceId(RESOURCE_NAME);
      String resource1Locator = resourceFactory.getResourceId(RESOURCE_NAME + 1);
      OpLocator locator = OpLocator.parseLocator(resourceLocator);
      Long resourceId = locator.getID();
      locator = OpLocator.parseLocator(resource1Locator);
      Long resource1Id = locator.getID();
      assertTrue(resourceIds.contains(resourceId));
      assertTrue(resourceIds.contains(resource1Id));

      broker = session.newBroker();
      t = broker.newTransaction();

      OpUser user1 = new OpUser();
      user1.setName("User1");
      OpResource resource2 = new OpResource();
      resource2.setName(RESOURCE_NAME + 2);
      resource2.setUser(user1);
      broker.makePersistent(user1);
      broker.makePersistent(resource2);
      broker.deleteObject(resource1);

      t.commit();
      resourceIds = OpWorkSlipDataSetFactory.getListOfSubordinateResourceIds(session, broker);
      broker.close();

      assertEquals(1, resourceIds.size());
      String resource2Locator = resourceFactory.getResourceId(RESOURCE_NAME + 2);
      locator = OpLocator.parseLocator(resource2Locator);
      Long resource2Id = locator.getID();
      assertTrue(resourceIds.contains(resourceId));
      assertFalse(resourceIds.contains(resource2Id));
   }

   /**
    * Test for fillProjectSet() in OpWorkSlipDataSetFactory.
    *
    * @throws Exception
    */
   public void testFillProjectSet()
        throws Exception {

      createAssignments();
      XComponent projectsSet = new XComponent(XComponent.DATA_SET);

      String projectId = projectFactory.getProjectId(PROJECT_NAME);
      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project = projectFactory.getProjectById(broker, projectId);
         String project1Id = projectFactory.getProjectId(PROJECT_NAME + 1);
         OpProjectNode project1 = projectFactory.getProjectById(broker, project1Id);

         List<OpAssignment> assignmentList = new ArrayList<OpAssignment>();
         assertEquals(0, projectsSet.getChildCount());

         for (OpAssignment assignment : project.getPlan().getActivityAssignments()) {
            assignmentList.add(assignment);
         }
         for (OpAssignment assignment : project1.getPlan().getActivityAssignments()) {
            assignmentList.add(assignment);
         }

         OpWorkSlipDataSetFactory.fillProjectSet(projectsSet, assignmentList);
         assertEquals(2, projectsSet.getChildCount());

         String choice = XValidator.choice(project.locator(), project.getName());
         String choice1 = XValidator.choice(project1.locator(), project1.getName());
         for (int i = 0; i < projectsSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) projectsSet.getChild(i);
            assertTrue(dataRow.getValue().equals(choice) || dataRow.getValue().equals(choice1));
         }
      }
      finally {
         broker.close();
      }

   }

   /**
    * Test for fillChoiceDataSetsFromSingleAssignment() in OpWorkSlipDataSetFactory.
    *
    * @throws Exception
    */
   public void testFillChoiceDataSetsFromSingleAssignment()
        throws Exception {

      createAssignments();
      XComponent choiceProjectSet = new XComponent(XComponent.DATA_SET);
      XComponent choiceActivitySet = new XComponent(XComponent.DATA_SET);
      XComponent choiceResourceSet = new XComponent(XComponent.DATA_SET);

      String project1Id = projectFactory.getProjectId(PROJECT_NAME + 1);
      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project1 = projectFactory.getProjectById(broker, project1Id);

         OpAssignment assignment = new OpAssignment();
         for (OpAssignment activityAssignment : project1.getPlan().getActivityAssignments()) {
            activityAssignment = projectFactory.getAssignmentById(activityAssignment.locator());
            if (activityAssignment.getActivity().getName().equals(ACTIVITY_NAME + 1)) {
               assignment = activityAssignment;
               break;
            }
         }

         OpWorkSlipDataSetFactory.fillChoiceDataSetsFromSingleAssignment(assignment, choiceProjectSet,
              choiceActivitySet, choiceResourceSet);

         //check if the project choice data set was filled correctly
         assertEquals(1, choiceProjectSet.getChildCount());
         String projectChoice = XValidator.choice(project1.locator(), project1.getName());
         assertEquals(projectChoice, ((XComponent) choiceProjectSet.getChild(0)).getValue());

         //check if the activity choice data set was filled correctly
         assertEquals(1, choiceActivitySet.getChildCount());
         assertEquals(2, choiceActivitySet.getChild(0).getChildCount());
         OpActivity activity = assignment.getActivity();
         String activityChoice = XValidator.choice(activity.locator(), activity.getName());
         assertEquals(activityChoice, ((XComponent) choiceActivitySet.getChild(0)).getValue());
         assertEquals(activity.getType(), ((XComponent) ((XComponent) choiceActivitySet.getChild(0)).getChild(0)).getValue());

         //check the activity costs map
         Map<Byte, List> costsMap = (Map) ((XComponent) ((XComponent) choiceActivitySet.getChild(0)).getChild(1)).getValue();
         assertEquals(4, costsMap.keySet().size());

         List travelCosts = costsMap.get(OpCostRecord.TRAVEL_COST);
         assertEquals(activity.getBaseTravelCosts(), (Double) travelCosts.get(0), DOUBLE_ERROR_MARGIN);
         assertEquals(activity.getRemainingTravelCosts(), (Double) travelCosts.get(1), DOUBLE_ERROR_MARGIN);
         assertFalse((Boolean) travelCosts.get(2));

         List materialCosts = costsMap.get(OpCostRecord.MATERIAL_COST);
         assertEquals(activity.getBaseMaterialCosts(), (Double) materialCosts.get(0), DOUBLE_ERROR_MARGIN);
         assertEquals(activity.getRemainingMaterialCosts(), (Double) materialCosts.get(1), DOUBLE_ERROR_MARGIN);
         assertFalse((Boolean) materialCosts.get(2));

         List externalCosts = costsMap.get(OpCostRecord.EXTERNAL_COST);
         assertEquals(activity.getBaseExternalCosts(), (Double) externalCosts.get(0), DOUBLE_ERROR_MARGIN);
         assertEquals(activity.getRemainingExternalCosts(), (Double) externalCosts.get(1), DOUBLE_ERROR_MARGIN);
         assertFalse((Boolean) externalCosts.get(2));

         List miscCosts = costsMap.get(OpCostRecord.MISCELLANEOUS_COST);
         assertEquals(activity.getBaseMiscellaneousCosts(), (Double) miscCosts.get(0), DOUBLE_ERROR_MARGIN);
         assertEquals(activity.getRemainingMiscellaneousCosts(), (Double) miscCosts.get(1), DOUBLE_ERROR_MARGIN);
         assertFalse((Boolean) miscCosts.get(2));

         //check if the resource choice data set was filled correctly
         assertEquals(1, choiceResourceSet.getChildCount());
         String resourceChoice = XValidator.choice(assignment.getResource().locator(), assignment.getResource().getName());
         assertEquals(resourceChoice, ((XComponent) choiceResourceSet.getChild(0)).getValue());
      }
      finally {
         broker.close();
      }
   }

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {

      OpUserTestDataFactory usrData = new OpUserTestDataFactory(session);

      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();

      deleteAllObjects(broker, OpWorkRecord.WORK_RECORD);
      deleteAllObjects(broker, OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(broker, OpAssignment.ASSIGNMENT);
      deleteAllObjects(broker, OpActivity.ACTIVITY);
      deleteAllObjects(broker, OpResource.RESOURCE);
      deleteAllObjects(broker, OpCostRecord.COST_RECORD);

      for (OpProjectNode project : projectFactory.getAllProjects(broker)) {
         broker.deleteObject(project);
      }

      for (OpUser user : usrData.getAllUsers(broker)) {
         if (user.getName().equals(OpUser.ADMINISTRATOR_NAME)) {
            continue;
         }
         broker.deleteObject(user);
      }

      transaction.commit();
      broker.close();
   }

   /**
    * Inserts in the database a project plan with an associated project
    * an activity on the project plan and a resource
    * an assignment for the activity and the resource
    * and a work record for the assignment
    */
   private void prepareTest() {
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create the project node and the project plan
      OpProjectNode project = new OpProjectNode();
      project.setName(PROJECT_NAME);
      project.setType(OpProjectNode.PROJECT);
      project.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()));
      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()));
      projectPlan.setFinish(new Date(getCalendarWithExactDaySet(2007, 6, 30).getTimeInMillis()));
      projectPlan.setProgressTracked(true);
      projectPlan.setProjectNode(project);

      //create the activity - assignment - resource
      OpActivity activity = new OpActivity();
      activity.setName(ACTIVITY_NAME);
      activity.setProjectPlan(projectPlan);
      activity.setBaseTravelCosts(30d);
      activity.setBaseMaterialCosts(50d);
      activity.setType(OpActivity.STANDARD);
      OpResource resource = new OpResource();
      resource.setName(RESOURCE_NAME);
      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resource);
      assignment.setProjectPlan(projectPlan);

      //create the workRecord
      OpWorkRecord workRecord = new OpWorkRecord();
      workRecord.setAssignment(assignment);
      workRecord.setActualEffort(10d);
      workRecord.setComment("Comment work record");

      broker.makePersistent(project);
      broker.makePersistent(projectPlan);
      broker.makePersistent(activity);
      broker.makePersistent(resource);
      broker.makePersistent(assignment);
      broker.makePersistent(workRecord);

      t.commit();
      broker.close();
   }

   /**
    * Inserts in the database 2 projects, 3 activities, 2 resources and 3 assignments.
    * The first project one activity and the second one has two activities.
    * Each activity has one associated resource and the first resource is assigned to the activity from the first project
    * and to the second activity from the second project.
    */
   private void createAssignments() {
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create 2 project nodes and the project plans
      OpProjectNode project = new OpProjectNode();
      project.setName(PROJECT_NAME);
      project.setType(OpProjectNode.PROJECT);
      project.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()));
      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()));
      projectPlan.setFinish(new Date(getCalendarWithExactDaySet(2007, 6, 30).getTimeInMillis()));
      projectPlan.setProgressTracked(true);
      projectPlan.setProjectNode(project);

      OpProjectNode project1 = new OpProjectNode();
      project1.setName(PROJECT_NAME + 1);
      project1.setType(OpProjectNode.PROJECT);
      project1.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()));
      OpProjectPlan projectPlan1 = new OpProjectPlan();
      projectPlan1.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 12).getTimeInMillis()));
      projectPlan1.setFinish(new Date(getCalendarWithExactDaySet(2007, 6, 25).getTimeInMillis()));
      projectPlan1.setProgressTracked(true);
      projectPlan1.setProjectNode(project1);

      //create the 3 activities, 2 resources and the assignments that correspond to them
      OpActivity activity = new OpActivity();
      activity.setName(ACTIVITY_NAME);
      activity.setType(OpActivity.STANDARD);
      activity.setBaseTravelCosts(10d);
      activity.setBaseMaterialCosts(20d);
      activity.setBaseExternalCosts(30d);
      activity.setBaseMiscellaneousCosts(40d);
      activity.setRemainingTravelCosts(50d);
      activity.setRemainingMaterialCosts(60d);
      activity.setRemainingExternalCosts(70d);
      activity.setRemainingMiscellaneousCosts(80d);
      activity.setProjectPlan(projectPlan);

      OpActivity activity1 = new OpActivity();
      activity1.setName(ACTIVITY_NAME + 1);
      activity1.setType(OpActivity.MILESTONE);
      activity1.setBaseTravelCosts(1d);
      activity1.setBaseMaterialCosts(2d);
      activity1.setBaseExternalCosts(3d);
      activity1.setBaseMiscellaneousCosts(4d);
      activity1.setRemainingTravelCosts(5d);
      activity1.setRemainingMaterialCosts(6d);
      activity1.setRemainingExternalCosts(7d);
      activity1.setRemainingMiscellaneousCosts(8d);
      activity1.setProjectPlan(projectPlan1);

      OpActivity activity2 = new OpActivity();
      activity2.setName(ACTIVITY_NAME + 2);
      activity2.setType(OpActivity.STANDARD);
      activity2.setBaseTravelCosts(21d);
      activity2.setBaseMaterialCosts(22d);
      activity2.setBaseExternalCosts(23d);
      activity2.setBaseMiscellaneousCosts(24d);
      activity2.setRemainingTravelCosts(25d);
      activity2.setRemainingMaterialCosts(26d);
      activity2.setRemainingExternalCosts(27d);
      activity2.setRemainingMiscellaneousCosts(28d);
      activity2.setProjectPlan(projectPlan1);

      OpResource resource = new OpResource();
      resource.setName(RESOURCE_NAME);
      OpResource resource1 = new OpResource();
      resource1.setName(RESOURCE_NAME + 1);

      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resource);
      assignment.setProjectPlan(projectPlan);

      OpAssignment assignment1 = new OpAssignment();
      assignment1.setActivity(activity1);
      assignment1.setResource(resource1);
      assignment1.setProjectPlan(projectPlan1);

      OpAssignment assignment2 = new OpAssignment();
      assignment2.setActivity(activity2);
      assignment2.setResource(resource);
      assignment2.setProjectPlan(projectPlan1);

      broker.makePersistent(project);
      broker.makePersistent(project1);
      broker.makePersistent(projectPlan);
      broker.makePersistent(projectPlan1);
      broker.makePersistent(activity);
      broker.makePersistent(activity1);
      broker.makePersistent(activity2);
      broker.makePersistent(resource);
      broker.makePersistent(resource1);
      broker.makePersistent(assignment);
      broker.makePersistent(assignment1);
      broker.makePersistent(assignment2);

      t.commit();
      broker.close();
   }
}
