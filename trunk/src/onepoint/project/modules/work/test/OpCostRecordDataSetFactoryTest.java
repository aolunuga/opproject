/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.modules.work.OpCostRecord;
import onepoint.project.modules.work.OpCostRecordDataSetFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.validators.OpWorkCostValidator;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.util.OpProjectConstants;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Test class for testing the methods in OpCostRecordDataSetFactory
 *
 * @author florin.haizea
 */

public class OpCostRecordDataSetFactoryTest extends OpBaseOpenTestCase {

   private static final String PROJECT_NAME = "project";
   private static final String ACTIVITY_NAME = "activity";
   private static final String RESOURCE_NAME = "resource";

   private OpWorkTestDataFactory dataFactory;
   private OpProjectTestDataFactory projectFactory;

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

   //test method for creating a XComponent data set out of a set of OpCostRecord entities
   public void testGetCostDataSetForWorkRecord()
        throws Exception {
      prepareTest();

      String workRecordId = dataFactory.getWorkRecordId(RESOURCE_NAME);
      OpWorkRecord workRecord = dataFactory.getWorkRecordById(workRecordId);

      OpBroker broker = session.newBroker();
      XComponent dataSet = OpCostRecordDataSetFactory.getCostDataSetForWorkRecord(workRecord, session, broker);
      assertEquals(workRecord.getCostRecords().size(), dataSet.getChildCount());
      broker.close();

      OpAttachment attachment = new OpAttachment();

      for (OpCostRecord costRecord : workRecord.getCostRecords()) {
         assertTrue(isEntityInDataSet(costRecord, dataSet));

         if (costRecord.getType() == OpCostRecord.TRAVEL_COST) {
            String attachmentId = dataFactory.getAttachmentId(costRecord.getID());
            attachment = dataFactory.getAttachmentById(attachmentId);
         }
      }

      //test the existence of the attachment in the data set
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         if (((XComponent) dataRow.getChild(OpWorkCostValidator.COMMENTS_COST_INDEX)).getStringValue()
              .equals("Comment cost record 1")) {
            XComponent attachmentCell = (XComponent) dataRow.getChild(OpWorkCostValidator.ATTACHMENT_INDEX);
            List attachmentList = attachmentCell.getListValue();
            List atachmentElement = (ArrayList) attachmentList.get(0);
            assertEquals(OpProjectConstants.LINKED_ATTACHMENT_DESCRIPTOR, atachmentElement.get(0));
            assertEquals(attachment.locator(), atachmentElement.get(1));
            assertEquals("Attachment 1", atachmentElement.get(2));
            assertEquals("http://www.google.com/", atachmentElement.get(3));
         }
      }
   }

   //test method for creating a list of OpCostRecord entities out of a dataset
   public void testCreateCostRecords()
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

      OpAttachment attachment = new OpAttachment();

      for (OpCostRecord costRecord : workRecord.getCostRecords()) {
         if (costRecord.getType() == OpCostRecord.TRAVEL_COST) {
            String attachmentId = dataFactory.getAttachmentId(costRecord.getID());
            attachment = dataFactory.getAttachmentById(attachmentId);
         }
      }

      //add two data rows to the data set
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
      dataCell.setIntValue(0 | OpWorkCostValidator.HAS_ATTACHMENTS);
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
      //9 - set the attachments
      dataCell = new XComponent(XComponent.DATA_CELL);
      ArrayList attachmentList = new ArrayList();
      List attachmentElement = new ArrayList();
      attachmentList.add(attachmentElement);
      attachmentElement.add(OpProjectConstants.LINKED_ATTACHMENT_DESCRIPTOR);
      attachmentElement.add(attachment.locator());
      attachmentElement.add("Attachment 1");
      attachmentElement.add("http://www.google.com/");
      dataCell.setListValue(attachmentList);
      dataRow.addChild(OpWorkCostValidator.ATTACHMENT_INDEX, dataCell);
      //assignment locator as data row value
      dataRow.setStringValue(assignment.locator());
      dataSet.addChild(dataRow);

      dataRow = new XComponent(XComponent.DATA_ROW);
      //0 - project name
      dataCell = new XComponent(XComponent.DATA_CELL);
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
      dataCell.setIntValue(0 - 0 & OpWorkCostValidator.HAS_ATTACHMENTS);
      dataRow.addChild(OpWorkCostValidator.INDICATOR_INDEX, dataCell);
      //4 - cost type
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(XValidator.choice(String.valueOf(OpCostRecord.MATERIAL_COST), "Material Cost"));
      dataRow.addChild(OpWorkCostValidator.COST_TYPE_INDEX, dataCell);
      //5 - base cost
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(50d);
      dataRow.addChild(OpWorkCostValidator.BASE_COST_INDEX, dataCell);
      //6 - actual cost
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(25d);
      dataRow.addChild(OpWorkCostValidator.ACTUAL_COST_INDEX, dataCell);
      //7 - remaining cost
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(25d);
      dataRow.addChild(OpWorkCostValidator.REMAINING_COST_INDEX, dataCell);
      //8 - comment
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue("Comment cost record 2");
      dataRow.addChild(OpWorkCostValidator.COMMENTS_COST_INDEX, dataCell);
      //9 - set the attachments
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setValue(new ArrayList());
      dataRow.addChild(OpWorkCostValidator.ATTACHMENT_INDEX, dataCell);
      //assignment locator as data row value
      dataRow.setStringValue(assignment.locator());
      dataSet.addChild(dataRow);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      Set<OpCostRecord> costRecords = OpCostRecordDataSetFactory.createCostRecords(broker, dataSet);
      assertEquals(dataSet.getChildCount(), costRecords.size());
      for(OpCostRecord costRecord : costRecords) {
         broker.makePersistent(costRecord);
      }

      t.commit();
      broker.close();

      for (OpCostRecord costRecord : costRecords) {
         costRecord.setWorkRecord(workRecord);
         assertTrue(isEntityInDataSet(costRecord, dataSet));
         if (costRecord.getType() == OpCostRecord.TRAVEL_COST) {
            assertEquals(1, costRecord.getAttachments().size());

            for (OpAttachment attachmentWR : costRecord.getAttachments()) {
               assertTrue(attachmentWR.getLinked());
               assertEquals("Attachment 1", attachmentWR.getName());
               assertEquals("http://www.google.com/", attachmentWR.getLocation());
            }
         }
      }
   }

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();

      OpUserTestDataFactory usrData = new OpUserTestDataFactory(session);
      List<OpUser> users = usrData.getAllUsers(broker);
      for (OpUser user : users) {
         if (user.getName().equals(OpUser.ADMINISTRATOR_NAME)) {
            continue;
         }
         broker.deleteObject(user);
      }

      deleteAllObjects(broker, OpAttachment.ATTACHMENT);
      deleteAllObjects(broker, OpWorkRecord.WORK_RECORD);
      deleteAllObjects(broker, OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(broker, OpAssignment.ASSIGNMENT);
      deleteAllObjects(broker, OpActivity.ACTIVITY);
      deleteAllObjects(broker, OpResource.RESOURCE);
      deleteAllObjects(broker, OpCostRecord.COST_RECORD);

      for (OpProjectNode project : projectFactory.getAllProjects(broker)) {
         broker.deleteObject(project);
      }

      transaction.commit();
      broker.close();
   }

   /**
    * Returns <code>true</code> if the data set has one row in which the cells contain the values of the <code>OpCostRecord</code> entity attributes
    * or <code>false</code> otherwise
    *
    * @param costRecord - the <code>OpCostRecord</code> entity whose values are checked against the data cells
    * @param dataSet    - the <code>XComponent</code> data set
    * @return <code>true</code> if the data set has one row in which the cells contain the values of the <code>OpCostRecord</code> entity attributes
    *         or <code>false</code> otherwise
    */
   public static boolean isEntityInDataSet(OpCostRecord costRecord, XComponent dataSet) {
      XComponent dataRow;
      OpActivity activity = costRecord.getActivity();
      OpProjectNode project = activity.getProjectPlan().getProjectNode();
      OpAssignment assignment = costRecord.getWorkRecord().getAssignment();
      OpResource resource = assignment.getResource();

      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         //boolean value indicating if the indicator is correct
         boolean indicator = false;
         int indicatorValue = ((XComponent) dataRow.getChild(OpWorkCostValidator.INDICATOR_INDEX)).getIntValue();
         if((indicatorValue == (0 | OpWorkCostValidator.HAS_ATTACHMENTS) &&
              !costRecord.getAttachments().isEmpty())
              || (( indicatorValue == 0 - (0 & OpWorkCostValidator.HAS_ATTACHMENTS)
              && costRecord.getAttachments().isEmpty()))){
            indicator = true;
         }
         if (((XComponent) dataRow.getChild(OpWorkCostValidator.PROJECT_NAME_INDEX)).getStringValue().
              equals(XValidator.choice(project.locator(), project.getName())) &&
              ((XComponent) dataRow.getChild(OpWorkCostValidator.ACTIVITY_NAME_INDEX)).getStringValue().
                   equals(XValidator.choice(activity.locator(), activity.getName())) &&
              ((XComponent) dataRow.getChild(OpWorkCostValidator.RESOURCE_NAME_INDEX)).getStringValue().
                   equals(XValidator.choice(resource.locator(), resource.getName())) &&
              indicator &&
              XValidator.choiceID(((XComponent) dataRow.getChild(OpWorkCostValidator.COST_TYPE_INDEX)).getStringValue()).
                   equals(String.valueOf(costRecord.getType())) &&
              ((XComponent) dataRow.getChild(OpWorkCostValidator.BASE_COST_INDEX)).getDoubleValue() ==
                   costRecord.getBaseCost() &&
              ((XComponent) dataRow.getChild(OpWorkCostValidator.ACTUAL_COST_INDEX)).getDoubleValue() ==
                   costRecord.getActualCosts() &&
              ((XComponent) dataRow.getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getDoubleValue() ==
                   costRecord.getRemainingCosts() &&
              ((XComponent) dataRow.getChild(OpWorkCostValidator.COMMENTS_COST_INDEX)).getStringValue().
                   equals(costRecord.getComment()) &&
              (dataRow.getStringValue().equals(assignment.locator()))) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns <code>true</code> if the data set has one row in which the cells contain the values of the <code>OpCostRecord</code> entity attributes
    * or <code>false</code> otherwise.  Only the fields that are set by OpCostRecordDataSetFactory.createCostEntity() method are
    * checked.
    *
    * @param costRecord - the <code>OpCostRecord</code> entity whose values are checked against the data cells
    * @param dataSet    - the <code>XComponent</code> data set
    * @return <code>true</code> if the data set has one row in which the cells contain the values of the <code>OpCostRecord</code> entity attributes
    *         or <code>false</code> otherwise
    */
   public static boolean hasEntityFieldsInDataSet(OpCostRecord costRecord, XComponent dataSet) {
      XComponent dataRow;

      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if (XValidator.choiceID(((XComponent) dataRow.getChild(OpWorkCostValidator.COST_TYPE_INDEX)).getStringValue()).
              equals(String.valueOf(costRecord.getType())) &&
              ((XComponent) dataRow.getChild(OpWorkCostValidator.ACTUAL_COST_INDEX)).getDoubleValue() ==
                   costRecord.getActualCosts() &&
              ((XComponent) dataRow.getChild(OpWorkCostValidator.REMAINING_COST_INDEX)).getDoubleValue() ==
                   costRecord.getRemainingCosts() &&
              ((XComponent) dataRow.getChild(OpWorkCostValidator.COMMENTS_COST_INDEX)).getStringValue().
                   equals(costRecord.getComment())) {
            return true;
         }
      }
      return false;
   }

   /**
    * Inserts in the database a project plan with an associated project
    * an activity on the project plan and a resource
    * an assignment for the activity and the resource
    * a work record for the assignment and two cost records associated with the work record
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
      projectPlan.setProjectNode(project);

      //create the activity - assignment - resource
      OpActivity activity = new OpActivity();
      activity.setName(ACTIVITY_NAME);
      activity.setProjectPlan(projectPlan);
      activity.setBaseTravelCosts(30d);
      activity.setBaseMaterialCosts(50d);
      OpResource resource = new OpResource();
      resource.setName(RESOURCE_NAME);
      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resource);

      //create the workRecord
      OpWorkRecord workRecord = new OpWorkRecord();
      workRecord.setAssignment(assignment);

      //create the OpCostRecords
      OpCostRecord costRecord1 = new OpCostRecord();
      costRecord1.setType(OpCostRecord.TRAVEL_COST);
      costRecord1.setActualCosts(20d);
      costRecord1.setRemainingCosts(10d);
      costRecord1.setComment("Comment cost record 1");
      costRecord1.setWorkRecord(workRecord);
      OpCostRecord costRecord2 = new OpCostRecord();
      costRecord2.setType(OpCostRecord.MATERIAL_COST);
      costRecord2.setActualCosts(25d);
      costRecord2.setRemainingCosts(25d);
      costRecord2.setComment("Comment cost record 2");
      costRecord2.setWorkRecord(workRecord);

      //create an attachment for the first cost record
      OpAttachment attachment1 = new OpAttachment();
      attachment1.setLinked(true);
      attachment1.setName("Attachment 1");
      attachment1.setLocation("http://www.google.com/");
      attachment1.setCostRecord(costRecord1);

      broker.makePersistent(project);
      broker.makePersistent(projectPlan);
      broker.makePersistent(activity);
      broker.makePersistent(resource);
      broker.makePersistent(assignment);
      broker.makePersistent(workRecord);
      broker.makePersistent(costRecord1);
      broker.makePersistent(costRecord2);
      broker.makePersistent(attachment1);

      t.commit();
      broker.close();
   }
}