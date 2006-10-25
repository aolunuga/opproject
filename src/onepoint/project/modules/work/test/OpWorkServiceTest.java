/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.work.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkService;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test case class for OpWorkService. Will test the functionality of the service using mock objects.
 *
 * @author ovidiu.lupas
 */
public class OpWorkServiceTest extends onepoint.project.test.OpServiceAbstractTest {
   /*the instance of OpWorkService*/
   protected OpWorkService workService;
   /*the user session */
   private OpUser sessionUser;
   /*the assignment project plan*/
   private OpProjectPlan projectPlan;

   private List workSlipNumbers;
   private OpActivity activity;
   private OpAssignment assignment;
   private OpWorkSlip workSlip;
   private final String ASSIGNMENT_ID = "Assignment.id 980";
   private final String WORK_SLIP_ID = "WorkSlip.id 1686";
   private final String NON_EXISTENT_WORK_SLIP_ID = "NonExistent_WorkSlip.id 1890";

   private static String SELECT_MAX_WORK_SLIP_NUMBER_BY_RESOURCE_ID = "select max(work_slip.Number) from OpWorkSlip as work_slip where work_slip.Creator.ID = ?";
   private static String SELECT_EXISTENT_WORK_SLIP_BY_DATE = "select work_slip from OpWorkSlip as work_slip where work_slip.Creator.ID = ? and work_slip.Date = ?";
   private static String SELECT_WORKING_VERSION_ASSIGNMENT = "select assignmentVer from OpAssignmentVersion assignmentVer  inner join assignmentVer.ActivityVersion actVersion inner join actVersion.PlanVersion planVer  where assignmentVer.Resource.ID = ? and actVersion.Activity.ID = ? and planVer.VersionNumber = ?";
   private static String SELECT_WORKING_VERSION_ACTIVITY = "select actVersion from OpActivityVersion actVersion  inner join actVersion.PlanVersion planVer  where actVersion.Activity.ID = ? and planVer.VersionNumber = ?";
   /**
    * Set up the test case
    */
   public void setUp() {

      super.setUp();

      workService = new OpWorkService();
      //a empty list of results
      queryResults = new ArrayList();

      //a integer list of work slip number
      workSlipNumbers = new ArrayList();
      workSlipNumbers.add(new Integer(0));

      //session user
      sessionUser = new OpUser();
      sessionUser.setID(1);
      sessionUser.setName("user");
      sessionUser.setPassword("password");

      Set prefereceSet = new HashSet();
      OpPreference preference = new OpPreference();
      preference.setName(OpPreference.LOCALE);
      preference.setValue("en");
      //add the preference to the set
      prefereceSet.add(preference);
      sessionUser.setPreferences(prefereceSet);

      Set resourcesSet = new HashSet();
      OpResource resource = new OpResource();
      resource.setID(1);
      resource.setName(sessionUser.getName());
      resource.setHourlyRate((byte) 200);
      resource.setInheritPoolRate(true);
      resource.setUser(sessionUser);
      //add a resource to set
      resourcesSet.add(resource);
      sessionUser.setResources(resourcesSet);

      projectPlan = new OpProjectPlan();
      projectPlan.setStart(XCalendar.today());
      projectPlan.setFinish(XCalendar.today());
      projectPlan.setActivityAssignments(new HashSet());
      projectPlan.setActivityAttachments(new HashSet());
      projectPlan.setDependencies(new HashSet());
      projectPlan.setWorkPeriods(new HashSet());
      projectPlan.setProgressTracked(true);

      //create an activity
      activity = new OpActivity();
      activity = new OpActivity();
      activity.setSequence(0);
      activity.setType(OpActivity.STANDARD);
      activity.setStart(XCalendar.today());
      activity.setFinish(XCalendar.today());
      activity.setAttachments(new HashSet());
      activity.setAssignments(new HashSet());
      activity.setWorkPeriods(new HashSet());
      activity.setProjectPlan(projectPlan);

      //create the activity asignment
      assignment = new OpAssignment();
      assignment.setActualEffort(0.0);
      assignment.setRemainingEffort(0.0);
      assignment.setResource(resource);
      assignment.setActualCosts(0.0);
      assignment.setProjectPlan(projectPlan);
      assignment.setActivity(activity);

      //create the work slip
      workSlip = new OpWorkSlip();
      workSlip.setCreated(XCalendar.today());
      workSlip.setDate(XCalendar.today());
      workSlip.setNumber(1);
      workSlip.setRecords(new HashSet());

   }

   /**
    * Specifies the behaviour of the mocked methods.
    *
    * @param invocation contains the object and the invoked method
    * @return depends on the invoked method
    * @throws IllegalArgumentException if no such method was defined in this mapping
    */
   public Object invocationMatch(Invocation invocation)
        throws IllegalArgumentException {
      //the invoked method parameter types
      Class[] parameterTypes = invocation.invokedMethod.getParameterTypes();
      //the invoked method parameter values
      List parameterValues = invocation.parameterValues;

      //find
      if (invocation.invokedMethod.getName().equals(FIND_METHOD)) {
         return workSlipNumbers.iterator();
      }
      //getUser
      else if (invocation.invokedMethod.getName().equals(USER_METHOD)) {
         return sessionUser;
      }
      //getObject(Class,long)
      else if ((invocation.invokedMethod.getName().equals(GET_OBJECT_METHOD)) && (parameterTypes[0].equals(Class.class)) &&
           (parameterTypes[1].equals(long.class))) {
         return sessionUser;
      }
      //getObject(String)
      else if ((invocation.invokedMethod.getName().equals(GET_OBJECT_METHOD)) && (parameterTypes[0].equals(String.class))) {
         if (parameterValues.get(0).equals(ASSIGNMENT_ID)) {
            return assignment;
         }
         if (parameterValues.get(0).equals(WORK_SLIP_ID)) {
            return workSlip;
         }
         //default
         return null;
      }
      else {
         throw new IllegalArgumentException("No such object.method "+invocation.invokedMethod.getName()+" defind in this stub");
      }

   }

   /**
    * Tests that a new work slip will be correctly inserted.
    *     */
   public void testInsertWorkSlipOk() {
      XMessage request = new XMessage();
      //create the start date
      Date startDate = XCalendar.today();
      //set up the request
      request.setArgument(OpWorkService.START, startDate);
      request.setArgument(OpWorkService.WORK_RECORD_SET, createWorkRecordSet(ASSIGNMENT_ID, 10, 80, 0, 0, 0, 0,"ResourceId",false, OpActivity.STANDARD));

      //mock session expectations
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //get session user
      mockSession.expects(once()).method(USER_METHOD).will(methodStub);

      //mock transaction expectations
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //mock broker expectations
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //check for duplicate work slip date
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_EXISTENT_WORK_SLIP_BY_DATE)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
            queryResults.clear();
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_EXISTENT_WORK_SLIP_BY_DATE);
         }
      });
      //set start date
      mockQuery.expects(once()).method(SET_DATE_METHOD);

      //new query on borker
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_MAX_WORK_SLIP_NUMBER_BY_RESOURCE_ID)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
           //will find a work slip Number
           queryResults.add(new Integer(workSlip.getNumber()));
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_MAX_WORK_SLIP_NUMBER_BY_RESOURCE_ID);
         }
      });

      //new query on borker
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_WORKING_VERSION_ASSIGNMENT)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
           //will find no working version
           queryResults.clear();
           return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_WORKING_VERSION_ASSIGNMENT);
         }
      });

      //new query on borker
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_WORKING_VERSION_ACTIVITY)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
           //will find no working version
           queryResults.clear();
           return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_WORKING_VERSION_ACTIVITY);
         }
      });

      //set resource id and activity id
      mockQuery.expects(atLeastOnce()).method(SET_LONG_METHOD);
      //set project plan version
      mockQuery.expects(atLeastOnce()).method(SET_INTEGER_METHOD);

      //iterate over query results
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(isA(OpObject.class));
      //make work record persistent
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(isA(OpWorkRecord.class));
      //make work slip persistent
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(isA(OpWorkSlip.class));

      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(isA(String.class)).will(methodStub);

      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(isA(OpActivity.class));
      //broker close
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(workService.insertWorkSlip((XSession) mockSession.proxy(), request));

   }
   // TODO: Aditional tests for insert work slips taking into consideration: completed, activity type

   /**
    * Tests Tests that the persistance of a new inserted work slip will fail.
    *
    * @throws Exception if anything fails.
    */
   public void testInsertResourceWrongData() throws Exception {
      checkInsertWorkSlipWrongData(-100, 5.0,0,0,0,10);
      checkInsertWorkSlipWrongData(100, -5.0,0,0,0,10);
      checkInsertWorkSlipWrongData(100, 5.0,-10,0,0,10);
      checkInsertWorkSlipWrongData(100, 5.0,10,-10,0,10);
      checkInsertWorkSlipWrongData(100, 5.0,10,10,-10,10);
      checkInsertWorkSlipWrongData(100, 5.0,10,10,10,-10);
   }

   /**
    * Tests that the persistance of a new inserted work slip will fail.
    * @param actualEffort       a <code>double</code> representing the actualEffort of the work record
    * @param remainingEffort    a <code>double</code> representing the remainingEffort of the work record
    * @param materialCosts      a <code>double</code> representing the materialCosts of the work record
    * @param travelCosts        a <code>double</code> representing the travelCosts of the work record
    * @param externalCosts      a <code>double</code> representing the externalCosts of the work record
    * @param miscellaneousCosts a <code>double</code> representing the miscellaneousCosts of the work record
    */
   public void checkInsertWorkSlipWrongData(double actualEffort, double remainingEffort,double materialCosts,
        double travelCosts, double externalCosts, double miscellaneousCosts) {
      XMessage request = new XMessage();
      //create the start date
      Date startDate = XCalendar.today();
      //set up the request
      request.setArgument(OpWorkService.START, startDate);
      request.setArgument(OpWorkService.WORK_RECORD_SET, createWorkRecordSet(ASSIGNMENT_ID, actualEffort, remainingEffort,
                          materialCosts, travelCosts, externalCosts, miscellaneousCosts,"ResourceId",false, OpActivity.STANDARD));

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //mock commit transaction expectations
      mockTransaction.expects(never()).method(COMMIT_METHOD);
      //make work record /work slip  persistent
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      XMessage result = workService.insertWorkSlip((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests that the work slip is correctly deleted .
    */
   public void testDeleteWorkSlip() {
      /*create the request */
      XMessage request = new XMessage();
      //create the work slips ids array
      ArrayList workSlipIds = new ArrayList();
      workSlipIds.add(WORK_SLIP_ID);
      //set up the request
      request.setArgument(OpWorkService.WORK_SLIP_IDS, workSlipIds);

      //mock session expectations
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //mock broker expectations
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);
      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(isA(String.class)).will(methodStub);
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(isA(OpObject.class));
      mockBroker.expects(once()).method(CLOSE_METHOD);

      //mock transaction expectations
      mockTransaction.expects(once()).method(COMMIT_METHOD);


      assertNoError(workService.deleteWorkSlips((XSession) mockSession.proxy(), request));
   }

   /**
    * Tests that the no work slips are deleted (empty work slip id array).
    */
   public void testDeleteEmptyWorkSlipArray() {
      /*create the request */
      XMessage request = new XMessage();
      //create the work slips ids empty array
      ArrayList workSlipIds = new ArrayList();
      //set up the request
      request.setArgument(OpWorkService.WORK_SLIP_IDS, workSlipIds);

      //mock session expectations
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //mock broker expectations
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);
      mockBroker.expects(once()).method(CLOSE_METHOD);

      //mock transaction expectations
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      assertNoError(workService.deleteWorkSlips((XSession) mockSession.proxy(), request));
   }

   /**
    * Tests that a OpWorkService correctely deletes a non existing work slip.
    */
   public void testDeleteNonExistentWorkSlip() {
      /*create the request */
      XMessage request = new XMessage();
      //create the work slips ids array
      ArrayList workSlipIds = new ArrayList();
      workSlipIds.add(NON_EXISTENT_WORK_SLIP_ID);
      //set up the request
      request.setArgument(OpWorkService.WORK_SLIP_IDS, workSlipIds);

      //mock session expectations
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //mock broker expectations
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //get work slip object
      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(eq(NON_EXISTENT_WORK_SLIP_ID)).will(methodStub);
      //broker close
      mockBroker.expects(once()).method(CLOSE_METHOD);

      //mock transaction expectations
      mockTransaction.expects(never()).method(COMMIT_METHOD);


      XMessage result = workService.deleteWorkSlips((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result);
   }

   /**
    * Tests that a OpWorkService works correctely for editing a existing work slip.
    */
   public void testEditWorkSlipOk() {
      XMessage request = new XMessage();
      //set up the request
      request.setArgument(OpWorkService.WORK_SLIP_ID, WORK_SLIP_ID);
      request.setArgument(OpWorkService.WORK_RECORD_SET, createWorkRecordSet(ASSIGNMENT_ID, 10, 80, 0, 0, 0, 0,"ResourceId",false, OpActivity.STANDARD));

      //mock session expectations
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //mock broker expectations
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //new query on borker
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_WORKING_VERSION_ASSIGNMENT)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
           //will find no working version
           queryResults.clear();
           return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_WORKING_VERSION_ASSIGNMENT);
         }
      });

      //new query on borker
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_WORKING_VERSION_ACTIVITY)).will(new Stub() {
         public Object invoke(Invocation invocation) throws Throwable {
           //will find no working version
           queryResults.clear();
           return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_WORKING_VERSION_ACTIVITY);
         }
      });

      //set resource id and activity id
      mockQuery.expects(atLeastOnce()).method(SET_LONG_METHOD);
      //set project plan version
      mockQuery.expects(atLeastOnce()).method(SET_INTEGER_METHOD);

      //iterate over query results
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //make work record persistent
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(isA(OpWorkRecord.class));
      //get work slip object /assignment
      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(isA(String.class)).will(methodStub);

      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(isA(OpObject.class));

      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(isA(OpActivity.class));

      //commit transaction expectations
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //broker close
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(workService.editWorkSlip((XSession) mockSession.proxy(), request));

   }
   // TODO: Aditional tests for edit work slips taking into consideration: completed, activity type
    /**
    * Tests that the update of an work slip will fail.
    *
    * @throws Exception if anything fails.
    */
   public void testEditWorkSlipWrongData() throws Exception {
      checkEditWorkSlipWrongData(-100, 5.0,0,0,0,10);
      checkEditWorkSlipWrongData(100, -5.0,0,0,0,10);
      checkEditWorkSlipWrongData(100, 5.0,-10,0,0,10);
      checkEditWorkSlipWrongData(100, 5.0,10,-10,0,10);
      checkEditWorkSlipWrongData(100, 5.0,10,10,-10,10);
      checkEditWorkSlipWrongData(100, 5.0,10,10,10,-10);
   }
   /**
    * Tests that a OpWorkService works correctely for editing an work record wrong data slip.
    * @param actualEffort       a <code>double</code> representing the actualEffort of the work record
    * @param remainingEffort    a <code>double</code> representing the remainingEffort of the work record
    * @param materialCosts      a <code>double</code> representing the materialCosts of the work record
    * @param travelCosts        a <code>double</code> representing the travelCosts of the work record
    * @param externalCosts      a <code>double</code> representing the externalCosts of the work record
    * @param miscellaneousCosts a <code>double</code> representing the miscellaneousCosts of the work record
    */
   public void checkEditWorkSlipWrongData(double actualEffort, double remainingEffort,double materialCosts,
        double travelCosts, double externalCosts, double miscellaneousCosts) {
      XMessage request = new XMessage();
      //set up the request
      request.setArgument(OpWorkService.WORK_SLIP_ID, WORK_SLIP_ID);
      request.setArgument(OpWorkService.WORK_RECORD_SET, createWorkRecordSet(ASSIGNMENT_ID, actualEffort, remainingEffort,
                          materialCosts, travelCosts, externalCosts, miscellaneousCosts,"ResourceId",false, OpActivity.STANDARD));

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

     //mock transaction expectations
      mockTransaction.expects(never()).method(COMMIT_METHOD);
      //never update the object
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD).with(isA(OpObject.class));

      XMessage result = workService.editWorkSlip((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests that a OpWorkService works correctely for editing a non existent work slip.
    */
   public void testEditNonExistentWorkSlip() {
      XMessage request = new XMessage();
      //set up the request argument
      request.setArgument(OpWorkService.WORK_SLIP_ID,NON_EXISTENT_WORK_SLIP_ID );
      request.setArgument(OpWorkService.WORK_RECORD_SET, createWorkRecordSet(ASSIGNMENT_ID, 10, 80, 0, 0, 0, 0,"ResourceId",false, OpActivity.STANDARD));

      //mock session expectations
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //mock broker expectations
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);
      mockBroker.expects(atLeastOnce()).method(GET_OBJECT_METHOD).with(isA(String.class)).will(methodStub);
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD).with(isA(OpObject.class));
      mockBroker.expects(once()).method(CLOSE_METHOD);

      //mock transaction expectations
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      XMessage result = workService.editWorkSlip((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Creates and returns a new <code>XComponent.DATA_SET</code> representing the work record set.
    *
    * @param assignmentId       a <code>String</code> representing the assignmentId
    * @param actualEffort       a <code>double</code> representing the actualEffort
    * @param remainingEffort    a <code>double</code> representing the remainingEffort
    * @param materialCosts      a <code>double</code> representing the materialCosts
    * @param travelCosts        a <code>double</code> representing the travelCosts
    * @param externalCosts      a <code>double</code> representing the externalCosts
    * @param miscellaneousCosts a <code>double</code> representing the miscellaneousCosts
    * @param resourceId         a <code>String</code> representing the id of the resource
    * @param completed          a <code>boolean</code> representing completed check box
    * @param activityType       a <code>int</cde> representing the activity yype
    * @return a new <code>XComponent.DATA_SET</code> component representing the work record set
    */
   public XComponent createWorkRecordSet(String assignmentId, double actualEffort, double remainingEffort,
        double materialCosts, double travelCosts, double externalCosts, double miscellaneousCosts,
        String resourceId, boolean completed, byte activityType) {
      //create the work record set
      XComponent workRecordSet = new XComponent(XComponent.DATA_SET);

      //create the work record set
      XComponent workDataRow = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell;
      //assignment name
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(assignmentId);
      workDataRow.addChild(dataCell);

      //actuall effort
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(actualEffort);
      workDataRow.addChild(dataCell);

      //remaining effort
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(remainingEffort);
      workDataRow.addChild(dataCell);

      //material costs
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(materialCosts);
      workDataRow.addChild(dataCell);

      //travel costs
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(travelCosts);
      workDataRow.addChild(dataCell);

      //external costs
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(externalCosts);
      workDataRow.addChild(dataCell);

      //Miscellaneous Costs
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(miscellaneousCosts);
      workDataRow.addChild(dataCell);

      //Optional comment
      dataCell = new XComponent(XComponent.DATA_CELL);
      workDataRow.addChild(dataCell);

      //Resource id
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(resourceId);
      workDataRow.addChild(dataCell);

      //Original remaing effort
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(remainingEffort);
      workDataRow.addChild(dataCell);

      //Completed
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(completed);
      workDataRow.addChild(dataCell);

      //Activity type
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(activityType);
      workDataRow.addChild(dataCell);

      //Activity insert mode
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(false);
      workDataRow.addChild(dataCell);

      //add data to work record set
      workRecordSet.addChild(workDataRow);

      return workRecordSet;
   }

}
