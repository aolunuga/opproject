/**
 * Copyright(c) OnePoint Software GmbH 2008. All Rights Reserved.
 */
package onepoint.project.modules.project_checklist.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpToDo;
import onepoint.project.modules.project_checklist.OpProjectChecklistError;
import onepoint.project.modules.project_checklist.OpProjectChecklistService;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;

/**
 * Test class for OpProjectChecklistService
 *
 * @author mihai.costin
 */
public class OpProjectChecklistServiceTest extends OpBaseOpenTestCase {

   private static final String PROJECT_NAME = "project";
   private static final String TO_DO_1_NAME = "FirstToDo";
   private static final String TO_DO_2_NAME = "SecondToDo";
   private static final int TO_DO_1_PRIORITY = 3;
   private static final int TO_DO_2_PRIORITY = 1;
   private static final Date TO_DO_1_DUE_DATE = new Date(getCalendarWithExactDaySet(2008, 2, 12).getTimeInMillis());
   private static final Date TO_DO_2_DUE_DATE = new Date(getCalendarWithExactDaySet(2008, 3, 3).getTimeInMillis());

   private static final String PROJECT_ID = "project_id";
   private static final String TO_DOS_SET = "to_dos_set";

   private OpProjectChecklistService service;
   private OpProjectNode project;
   private XComponent dataRowToDo1;
   private XComponent dataRowToDo2;

   /**
    * Base set-up.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   @Override
   protected void setUp()
        throws Exception {
      super.setUp();

      service = OpTestDataFactory.getProjectChecklistService();

      // insert an empty project (which contains no todos)
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      project = new OpProjectNode();
      project.setName(PROJECT_NAME);
      project.setType(OpProjectNode.PROJECT);
      broker.makePersistent(project);
      t.commit();
      broker.close();

      dataRowToDo1 = new XComponent(XComponent.DATA_ROW);

      //0 - completed
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(false);
      dataRowToDo1.addChild(dataCell);

      //1 - name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(TO_DO_1_NAME);
      dataRowToDo1.addChild(dataCell);

      //2 - priority
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(TO_DO_1_PRIORITY);
      dataRowToDo1.addChild(dataCell);

      //3 - due date
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDateValue(TO_DO_1_DUE_DATE);
      dataRowToDo1.addChild(dataCell);

      dataRowToDo2 = new XComponent(XComponent.DATA_ROW);

      //0 - completed
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(true);
      dataRowToDo2.addChild(dataCell);

      //1 - name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(TO_DO_2_NAME);
      dataRowToDo2.addChild(dataCell);

      //2 - priority
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(TO_DO_2_PRIORITY);
      dataRowToDo2.addChild(dataCell);

      //3 - due date
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDateValue(TO_DO_2_DUE_DATE);
      dataRowToDo2.addChild(dataCell);      
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   @Override
   protected void tearDown()
        throws Exception {
      clean();
      super.tearDown();
   }

   /**
    * Test the happy flow of adding one to do
    *
    * @throws Exception if the test fails
    */
    public void testAddOneToDo()
        throws Exception {

      //create the request
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRowToDo1);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(1, project.getToDos().size());
      for(OpToDo todo : project.getToDos()) {
         assertFalse(todo.getCompleted());   
         assertEquals(TO_DO_1_NAME, todo.getName());
         assertEquals(TO_DO_1_PRIORITY, todo.getPriority());
         assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
      }
      broker.close();
   }

   /**
    * Test the error flow of adding one to do
    *
    * @throws Exception if the test fails
    */
    public void testAddOneToDoError()
        throws Exception {

      //create the request
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      //set the priority of the to do object to -1
      ((XComponent)dataRowToDo1.getChild(2)).setIntValue(-1);
      dataSet.addChild(dataRowToDo1);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertError(response, OpProjectChecklistError.TODO_PRIORITY_ERROR);
   }

   /**
    * Test the happy flow of adding multiple to dos
    *
    * @throws Exception if the test fails
    */
   public void testAddMultipleToDos()
        throws Exception {

      //create the request
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRowToDo1);
      dataSet.addChild(dataRowToDo2);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(2, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertTrue(todo.getName().equals(TO_DO_1_NAME) || todo.getName().equals(TO_DO_2_NAME));
         if (todo.getName().equals(TO_DO_1_NAME)) {
            assertFalse(todo.getCompleted());
            assertEquals(TO_DO_1_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
         }
         else {
            assertTrue(todo.getCompleted());
            assertEquals(TO_DO_2_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_2_DUE_DATE, todo.getDue());
         }
      }
      broker.close();
   }

   /**
    * Test the error flow of adding multiple to dos
    *
    * @throws Exception if the test fails
    */
   public void testAddMultipleToDosError()
        throws Exception {

      //create the request
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRowToDo1);
      //change the priority of the second to do object to 11
      ((XComponent)dataRowToDo2.getChild(2)).setIntValue(11);
      dataSet.addChild(dataRowToDo2);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertError(response, OpProjectChecklistError.TODO_PRIORITY_ERROR);
   }

   /**
    * Test the deletion of one to do from a project which contains multiple to dos
    *
    * @throws Exception if the test fails
    */
   public void testDeleteOneToDo()
        throws Exception {

      //create the request in order to add two to dos for the project
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRowToDo1);
      dataSet.addChild(dataRowToDo2);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertNoError(response);
      
      //create the request in order to delete one to do
      request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      for (OpToDo todo : project.getToDos()) {
         if(todo.getName().equals(TO_DO_2_NAME)) {
            dataRowToDo2.setStringValue(todo.locator());
         }
      }
      broker.close();
      dataSet.removeChild(0);
      request.setArgument(TO_DOS_SET, dataSet);

      response = service.updateProjectToDos(session, request);
      assertNoError(response);

      broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(1, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertTrue(todo.getCompleted());
         assertEquals(TO_DO_2_NAME, todo.getName());
         assertEquals(TO_DO_2_PRIORITY, todo.getPriority());
         assertEquals(TO_DO_2_DUE_DATE, todo.getDue());
      }
      broker.close();
   }

   /**
    * Test the deletion of all to dos from a project which contains multiple to dos
    *
    * @throws Exception if the test fails
    */
   public void testDeleteAllToDos()
        throws Exception {

      //create the request in order to add two to dos for the project
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRowToDo1);
      dataSet.addChild(dataRowToDo2);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertNoError(response);

      //create the request in order to delete one to do
      request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      dataSet = new XComponent(XComponent.DATA_SET);
      request.setArgument(TO_DOS_SET, dataSet);

      response = service.updateProjectToDos(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(0, project.getToDos().size());
      broker.close();
   }

   /**
    * Test the adding of one to do to a project which already contains other to dos
    *
    * @throws Exception if the test fails
    */
   public void testAddToDoToExistingOnes()
        throws Exception {

      //create the request in order to add one to do to the project
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRowToDo1);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(1, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertFalse(todo.getCompleted());
         assertEquals(TO_DO_1_NAME, todo.getName());
         assertEquals(TO_DO_1_PRIORITY, todo.getPriority());
         assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
         dataRowToDo1.setStringValue(todo.locator());
      }
      broker.close();

      //create the request in order to add another to do to the project
      request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      dataSet.addChild(dataRowToDo2);
      request.setArgument(TO_DOS_SET, dataSet);

      response = service.updateProjectToDos(session, request);
      assertNoError(response);

      broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(2, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertTrue(todo.getName().equals(TO_DO_1_NAME) || todo.getName().equals(TO_DO_2_NAME));
         if (todo.getName().equals(TO_DO_1_NAME)) {
            assertFalse(todo.getCompleted());
            assertEquals(TO_DO_1_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
         }
         else {
            assertTrue(todo.getCompleted());
            assertEquals(TO_DO_2_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_2_DUE_DATE, todo.getDue());
         }
      }
      broker.close();
   }

   /**
    * Test the updating of one to do from a project which contains multiple to dos
    *
    * @throws Exception if the test fails
    */
   public void testModifyOneExistingToDo()
        throws Exception {

      //create the request in order to add two to dos to the project
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRowToDo1);
      dataSet.addChild(dataRowToDo2);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(2, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertTrue(todo.getName().equals(TO_DO_1_NAME) || todo.getName().equals(TO_DO_2_NAME));
         if (todo.getName().equals(TO_DO_1_NAME)) {
            assertFalse(todo.getCompleted());
            assertEquals(TO_DO_1_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
         }
         else {
            assertTrue(todo.getCompleted());
            assertEquals(TO_DO_2_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_2_DUE_DATE, todo.getDue());
         }
      }
      for (OpToDo todo : project.getToDos()) {
         if(todo.getName().equals(TO_DO_1_NAME)) {
            dataRowToDo1.setStringValue(todo.locator());
         }
         else {
            dataRowToDo2.setStringValue(todo.locator());
         }
      }
      broker.close();

      //create the request in order to modify the first to do
      request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      ((XComponent) dataRowToDo1.getChild(0)).setBooleanValue(true);
      ((XComponent) dataRowToDo1.getChild(2)).setIntValue(9);
      request.setArgument(TO_DOS_SET, dataSet);

      response = service.updateProjectToDos(session, request);
      assertNoError(response);

      broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(2, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertTrue(todo.getName().equals(TO_DO_1_NAME) || todo.getName().equals(TO_DO_2_NAME));
         if (todo.getName().equals(TO_DO_1_NAME)) {
            assertTrue(todo.getCompleted());
            assertEquals(9, todo.getPriority());
            assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
         }
         else {
            assertTrue(todo.getCompleted());
            assertEquals(TO_DO_2_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_2_DUE_DATE, todo.getDue());
         }
      }
      broker.close();
   }

   /**
    * Test the updating of all to dos from a project
    *
    * @throws Exception if the test fails
    */
   public void testModifyAllExistingToDos()
        throws Exception {

      //create the request in order to add two to dos to the project
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRowToDo1);
      dataSet.addChild(dataRowToDo2);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(2, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertTrue(todo.getName().equals(TO_DO_1_NAME) || todo.getName().equals(TO_DO_2_NAME));
         if (todo.getName().equals(TO_DO_1_NAME)) {
            assertFalse(todo.getCompleted());
            assertEquals(TO_DO_1_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
         }
         else {
            assertTrue(todo.getCompleted());
            assertEquals(TO_DO_2_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_2_DUE_DATE, todo.getDue());
         }
      }
      for (OpToDo todo : project.getToDos()) {
         if (todo.getName().equals(TO_DO_1_NAME)) {
            dataRowToDo1.setStringValue(todo.locator());
         }
         else {
            dataRowToDo2.setStringValue(todo.locator());
         }
      }
      broker.close();

      //create the request in order to modify the first to do
      request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      ((XComponent) dataRowToDo1.getChild(0)).setBooleanValue(true);
      ((XComponent) dataRowToDo1.getChild(2)).setIntValue(9);
      ((XComponent) dataRowToDo2.getChild(0)).setBooleanValue(false);
      ((XComponent) dataRowToDo2.getChild(1)).setStringValue("ModifiedToDo");
      ((XComponent) dataRowToDo2.getChild(2)).setIntValue(7);
      request.setArgument(TO_DOS_SET, dataSet);

      response = service.updateProjectToDos(session, request);
      assertNoError(response);

      broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(2, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertTrue(todo.getName().equals(TO_DO_1_NAME) || todo.getName().equals("ModifiedToDo"));
         if (todo.getName().equals(TO_DO_1_NAME)) {
            assertTrue(todo.getCompleted());
            assertEquals(9, todo.getPriority());
            assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
         }
         else {
            assertFalse(todo.getCompleted());
            assertEquals(7, todo.getPriority());
            assertEquals(TO_DO_2_DUE_DATE, todo.getDue());
         }
      }
      broker.close();
   }

   /**
    * Test the error flow of updating one to do from a project which contains multiple to dos.
    *
    * @throws Exception if the test fails
    */
   public void testModifyOneExistingToDoError()
        throws Exception {

      //create the request in order to add two to dos to the project
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRowToDo1);
      dataSet.addChild(dataRowToDo2);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(2, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertTrue(todo.getName().equals(TO_DO_1_NAME) || todo.getName().equals(TO_DO_2_NAME));
         if (todo.getName().equals(TO_DO_1_NAME)) {
            assertFalse(todo.getCompleted());
            assertEquals(TO_DO_1_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
         }
         else {
            assertTrue(todo.getCompleted());
            assertEquals(TO_DO_2_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_2_DUE_DATE, todo.getDue());
         }
      }
      for (OpToDo todo : project.getToDos()) {
         if (todo.getName().equals(TO_DO_1_NAME)) {
            dataRowToDo1.setStringValue(todo.locator());
         }
         else {
            dataRowToDo2.setStringValue(todo.locator());
         }
      }
      broker.close();

      //create the request in order to modify the first to do
      request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      ((XComponent) dataRowToDo1.getChild(0)).setBooleanValue(true);
      ((XComponent) dataRowToDo1.getChild(2)).setIntValue(12);
      request.setArgument(TO_DOS_SET, dataSet);

      response = service.updateProjectToDos(session, request);
      assertError(response, OpProjectChecklistError.TODO_PRIORITY_ERROR);
   }

   /**
    * Test the no op scenario.
    *
    * @throws Exception if the test fails
    */
   public void testNoOp()
        throws Exception {

      //create the request in order to add two to dos to the project
      XMessage request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRowToDo1);
      dataSet.addChild(dataRowToDo2);
      request.setArgument(TO_DOS_SET, dataSet);

      XMessage response = service.updateProjectToDos(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(2, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertTrue(todo.getName().equals(TO_DO_1_NAME) || todo.getName().equals(TO_DO_2_NAME));
         if (todo.getName().equals(TO_DO_1_NAME)) {
            assertFalse(todo.getCompleted());
            assertEquals(TO_DO_1_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
         }
         else {
            assertTrue(todo.getCompleted());
            assertEquals(TO_DO_2_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_2_DUE_DATE, todo.getDue());
         }
      }
      broker.close();

      //create the request in order to modify the first to do
      request = new XMessage();
      request.setArgument(PROJECT_ID, project.locator());
      request.setArgument(TO_DOS_SET, dataSet);

      response = service.updateProjectToDos(session, request);
      assertNoError(response);

      broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(project.locator());
      assertEquals(2, project.getToDos().size());
      for (OpToDo todo : project.getToDos()) {
         assertTrue(todo.getName().equals(TO_DO_1_NAME) || todo.getName().equals(TO_DO_2_NAME));
         if (todo.getName().equals(TO_DO_1_NAME)) {
            assertFalse(todo.getCompleted());
            assertEquals(TO_DO_1_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_1_DUE_DATE, todo.getDue());
         }
         else {
            assertTrue(todo.getCompleted());
            assertEquals(TO_DO_2_PRIORITY, todo.getPriority());
            assertEquals(TO_DO_2_DUE_DATE, todo.getDue());
         }
      }
      broker.close();
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

      broker.deleteObject(project);

      transaction.commit();
      broker.close();
   }
}
