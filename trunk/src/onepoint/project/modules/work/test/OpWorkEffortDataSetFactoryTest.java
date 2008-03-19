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
import onepoint.project.modules.work.OpWorkEffortDataSetFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.validators.OpWorkEffortValidator;
import onepoint.project.test.OpBaseOpenTestCase;

import java.sql.Date;

/**
 * Test class for testing the methods in OpWorkEffortDataSetFactory
 *
 * @author florin.haizea
 */
public class OpWorkEffortDataSetFactoryTest extends OpBaseOpenTestCase {

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

   //test method for creating a XComponent data row out of a set of an OpWorkRecord entity
   public void testGetCostDataSetForWorkRecord()
        throws Exception {
      prepareTest();

      String workRecordId = dataFactory.getWorkRecordId(RESOURCE_NAME);
      OpWorkRecord workRecord = dataFactory.getWorkRecordById(workRecordId);

      XComponent dataRow = OpWorkEffortDataSetFactory.createEffortRow(workRecord);
      assertTrue(isEntityInDataRow(workRecord, dataRow));
   }

   //test method for creating an OpWorkRecord entity out of a datarow
   public void testCreateWorkRecord()
        throws Exception {
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

      assertTrue(isEntityInDataRow(workRecord, dataRow));
   }

   /**
    * Returns <code>true</code> if the data row's cells contain the values of the <code>OpWorkRecord</code> entity attributes
    * or <code>false</code> otherwise
    *
    * @param workRecord - the <code>OpWorkRecord</code> entity whose values are checked against the data cells
    * @param dataRow    - the <code>XComponent</code> data row
    * @return <code>true</code> if the data row's cells contain the values of the <code>OpWorkRecord</code> entity attributes
    *         or <code>false</code> otherwise
    */
   public static boolean isEntityInDataRow(OpWorkRecord workRecord, XComponent dataRow) {
      OpAssignment assignment = workRecord.getAssignment();
      OpActivity activity = assignment.getActivity();
      OpResource resource = assignment.getResource();
      OpProjectNode project = activity.getProjectPlan().getProjectNode();
      boolean completed = false;
      if (workRecord.getAssignment().getProjectPlan().getProgressTracked() || activity.getType() == OpActivity.ADHOC_TASK) {
         if (((XComponent) dataRow.getChild(OpWorkEffortValidator.COMPLETED_INDEX)).getBooleanValue() ==
              workRecord.getCompleted()) {
            completed = true;
         }
      }
      else {
         completed = true;
      }

      if (((XComponent) dataRow.getChild(OpWorkEffortValidator.PROJECT_NAME_INDEX)).getStringValue().
           equals(XValidator.choice(project.locator(), project.getName())) &&
           ((XComponent) dataRow.getChild(OpWorkEffortValidator.ACTIVITY_NAME_INDEX)).getStringValue().
                equals(XValidator.choice(activity.locator(), activity.getName())) &&
           ((XComponent) dataRow.getChild(OpWorkEffortValidator.RESOURCE_NAME_INDEX)).getStringValue().
                equals(XValidator.choice(resource.locator(), resource.getName())) &&
           ((XComponent) dataRow.getChild(OpWorkEffortValidator.PLANNED_EFFORT_INDEX)).getDoubleValue() ==
                workRecord.getAssignment().getBaseEffort() &&
           ((XComponent) dataRow.getChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX)).getDoubleValue() ==
                workRecord.getActualEffort() &&
           ((XComponent) dataRow.getChild(OpWorkEffortValidator.REMAINING_EFFORT_INDEX)).getDoubleValue() ==
                workRecord.getRemainingEffort() &&
           completed &&
           ((XComponent) dataRow.getChild(OpWorkEffortValidator.COMMENTS_INDEX)).getStringValue().
                equals(workRecord.getComment()) &&
           (dataRow.getStringValue().equals(assignment.locator()))) {
         return true;
      }

      return false;
   }

   /**
    * Returns <code>true</code> if the data row's cells contain the values of the <code>OpWorkRecord</code> entity attributes
    * or <code>false</code> otherwise. Only the fields that are set by OpWorkEffortDataSetFactory.createWorkEntity() method are
    * checked
    *
    * @param workRecord - the <code>OpWorkRecord</code> entity whose values are checked against the data cells
    * @param dataRow    - the <code>XComponent</code> data row
    * @return <code>true</code> if the data row's cells contain the values of the <code>OpWorkRecord</code> entity attributes
    *         or <code>false</code> otherwise
    */
   public static boolean hasEntityFieldsInDataRow(OpWorkRecord workRecord, XComponent dataRow) {
      boolean completed = false;
      if (((XComponent) dataRow.getChild(OpWorkEffortValidator.COMPLETED_INDEX)).getValue() != null) {
         if (((XComponent) dataRow.getChild(OpWorkEffortValidator.COMPLETED_INDEX)).getBooleanValue() ==
              workRecord.getCompleted()) {
            completed = true;
         }
      }
      else {
         completed = true;
      }

      if (((XComponent) dataRow.getChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX)).getDoubleValue() ==
           workRecord.getActualEffort() &&
           ((XComponent) dataRow.getChild(OpWorkEffortValidator.REMAINING_EFFORT_INDEX)).getDoubleValue() ==
                workRecord.getRemainingEffort() &&
           completed &&
           ((XComponent) dataRow.getChild(OpWorkEffortValidator.COMMENTS_INDEX)).getStringValue().
                equals(workRecord.getComment()) &&
           dataRow.getStringValue().equals(workRecord.getAssignment().locator())) {
         return true;
      }

      return false;
   }

   /**
    * Inserts in the database a project plan with an associated project
    * an activity on the project plan and a resource
    * an assignment for the activity and the resource
    * and a work record for the assignment
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
         workRecord.setComment("Comment work record");

         broker.makePersistent(project);
         broker.makePersistent(projectPlan);
         broker.makePersistent(activity);
         broker.makePersistent(resource);
         broker.makePersistent(assignment);
         broker.makePersistent(workRecord);

         t.commit();
      }
      finally {
         broker.close();
      }
   }
}
