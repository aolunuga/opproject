/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.OpTimeRecord;
import onepoint.project.modules.work.OpTimeRecordDataSetFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.validators.OpWorkTimeValidator;
import onepoint.project.test.OpBaseOpenTestCase;

import java.sql.Date;
import java.util.Set;

/**
 * Test class for testing the methods in OpTimeRecordDataSetFactory
 *
 * @author florin.haizea
 */
public class OpTimeRecordDataSetFactoryTest extends OpBaseOpenTestCase {

   private static final String PROJECT_NAME = "project";
   private static final String ACTIVITY_NAME = "activity";
   private static final String RESOURCE_NAME = "resource";

   private OpWorkTestDataFactory dataFactory;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      dataFactory = new OpWorkTestDataFactory(session);
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

   //test method for creating a XComponent data set out of a set of OpTimeRecord entities
   public void testGetTimeDataSetForWorkRecord()
        throws Exception {
      prepareTest();

      String workRecordId = dataFactory.getWorkRecordId(RESOURCE_NAME);
      OpWorkRecord workRecord = dataFactory.getWorkRecordById(workRecordId);

      XComponent dataSet = OpTimeRecordDataSetFactory.getTimeDataSetForWorkRecord(workRecord);
      assertEquals(workRecord.getTimeRecords().size(), dataSet.getChildCount());

      for (OpTimeRecord timeRecord : workRecord.getTimeRecords()) {
         assertTrue(isEntityInDataSet(timeRecord, dataSet));
      }
   }

   //test method for creating a list of OpTimeRecord entities out of a dataset
   public void testCreateTimeRecords()
        throws Exception {
      prepareTest();

      String workRecordId = dataFactory.getWorkRecordId(RESOURCE_NAME);
      OpWorkRecord workRecord = dataFactory.getWorkRecordById(workRecordId);

      OpAssignment assignment = workRecord.getAssignment();
      OpActivity activity = assignment.getActivity();
      OpResource resource = assignment.getResource();
      OpProjectNode project = activity.getProjectPlan().getProjectNode();

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);

      //add two data rows to the data set
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
      dataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      //0 - project name
      dataCell = new XComponent(XComponent.DATA_CELL);
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
      dataCell.setIntValue(30);
      dataRow.addChild(OpWorkTimeValidator.START_INDEX, dataCell);
      //4 - finish time
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(35);
      dataRow.addChild(OpWorkTimeValidator.FINISH_INDEX, dataCell);
      //5 - duration
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(5);
      dataRow.addChild(OpWorkTimeValidator.DURATION_INDEX, dataCell);
      //assignment locator as data row value
      dataRow.setStringValue(assignment.locator());
      dataSet.addChild(dataRow);

      Set<OpTimeRecord> timeRecords = OpTimeRecordDataSetFactory.createTimeRecords(dataSet);
      assertEquals(dataSet.getChildCount(), timeRecords.size());

      for (OpTimeRecord timeRecord : timeRecords) {
         timeRecord.setWorkRecord(workRecord);
         assertTrue(isEntityInDataSet(timeRecord, dataSet));
      }
   }

   /**
    * Returns <code>true</code> if the data set has one row in which the cells contain the values of the <code>OpTimeRecord</code> entity attributes
    * or <code>false</code> otherwise
    *
    * @param timeRecord - the <code>OpTimeRecord</code> entity whose values are checked against the data cells
    * @param dataSet    - the <code>XComponent</code> data set
    * @return <code>true</code> if the data set has one row in which the cells contain the values of the <code>OpTimeRecord</code> entity attributes
    *         or <code>false</code> otherwise
    */
   public static boolean isEntityInDataSet(OpTimeRecord timeRecord, XComponent dataSet) {
      XComponent dataRow;
      OpActivity activity = timeRecord.getActivity();
      OpProjectNode project = activity.getProjectPlan().getProjectNode();
      OpAssignment assignment = timeRecord.getWorkRecord().getAssignment();
      OpResource resource = assignment.getResource();

      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if (((XComponent) dataRow.getChild(OpWorkTimeValidator.PROJECT_NAME_INDEX)).getStringValue().
              equals(XValidator.choice(project.locator(), project.getName())) &&
              ((XComponent) dataRow.getChild(OpWorkTimeValidator.ACTIVITY_NAME_INDEX)).getStringValue().
                   equals(XValidator.choice(activity.locator(), activity.getName())) &&
              ((XComponent) dataRow.getChild(OpWorkTimeValidator.RESOURCE_NAME_INDEX)).getStringValue().
                   equals(XValidator.choice(resource.locator(), resource.getName())) &&
              ((XComponent) dataRow.getChild(OpWorkTimeValidator.START_INDEX)).getIntValue() ==
                   timeRecord.getStart() &&
              ((XComponent) dataRow.getChild(OpWorkTimeValidator.FINISH_INDEX)).getIntValue() ==
                   timeRecord.getFinish() &&
              ((XComponent) dataRow.getChild(OpWorkTimeValidator.DURATION_INDEX)).getIntValue() ==
                   timeRecord.getDuration() &&
              (dataRow.getStringValue().equals(assignment.locator()))) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns <code>true</code> if the data set has one row in which the cells contain the values of the <code>OpTimeRecord</code> entity attributes
    * or <code>false</code> otherwise. Only the fields that are set by OpTimeRecordDataSetFactory.createTimeEntity() method are
    * checked.
    *
    * @param timeRecord - the <code>OpTimeRecord</code> entity whose values are checked against the data cells
    * @param dataSet    - the <code>XComponent</code> data set
    * @return <code>true</code> if the data set has one row in which the cells contain the values of the <code>OpTimeRecord</code> entity attributes
    *         or <code>false</code> otherwise
    */
   public static boolean hasEntityFieldsInDataSet(OpTimeRecord timeRecord, XComponent dataSet) {
      XComponent dataRow;

      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if (((XComponent) dataRow.getChild(OpWorkTimeValidator.START_INDEX)).getIntValue() ==
              timeRecord.getStart() &&
              ((XComponent) dataRow.getChild(OpWorkTimeValidator.FINISH_INDEX)).getIntValue() ==
                   timeRecord.getFinish() &&
              ((XComponent) dataRow.getChild(OpWorkTimeValidator.DURATION_INDEX)).getIntValue() ==
                   timeRecord.getDuration()) {
            return true;
         }
      }
      return false;
   }

   /**
    * Inserts in the database a project plan with an associated project
    * an activity on the project plan and a resource
    * an assignment for the activity and the resource
    * a work record for the assignment and two time records associated with the work record
    */
   private void prepareTest() {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();

         //create the project node and the project plan
         OpProjectNode project = new OpProjectNode();
         project.setName(PROJECT_NAME);
         project.setType(OpProjectNode.PROJECT);
         project.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()));
         OpProjectPlan projectPlan = new OpProjectPlan();
         projectPlan.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 10).getTimeInMillis()));
         projectPlan.setFinish(new Date(getCalendarWithExactDaySet(2007, 6, 30).getTimeInMillis()));
         projectPlan.setProjectNode(project);

         //create the activity - assignment - resource
         OpActivity activity = new OpActivity();
         activity.setName(ACTIVITY_NAME);
         activity.setProjectPlan(projectPlan);
         OpResource resource = new OpResource();
         resource.setName(RESOURCE_NAME);
         OpAssignment assignment = new OpAssignment();
         activity.addAssignment(assignment);
         assignment.setResource(resource);

         //create the workRecord
         OpWorkRecord workRecord = new OpWorkRecord();
         workRecord.setAssignment(assignment);

         //create the OpTimeRecords
         OpTimeRecord timeRecord1 = new OpTimeRecord();
         OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord1, 50, 100, 50);
         timeRecord1.setWorkRecord(workRecord);
         OpTimeRecord timeRecord2 = new OpTimeRecord();
         OpWorkTestDataFactory.setFieldsOnTimeRecord(timeRecord2, 30, 35, 5);
         timeRecord2.setWorkRecord(workRecord);

         broker.makePersistent(project);
         broker.makePersistent(projectPlan);
         broker.makePersistent(activity);
         broker.makePersistent(resource);
         broker.makePersistent(assignment);
         broker.makePersistent(workRecord);
         broker.makePersistent(timeRecord1);
         broker.makePersistent(timeRecord2);

         t.commit();
      }
      finally {
         broker.close();
      }
   }
}
