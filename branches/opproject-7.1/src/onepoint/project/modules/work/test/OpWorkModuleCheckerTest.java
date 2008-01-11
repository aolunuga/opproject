/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.OpActivityTestDataFactory;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.work.OpWorkModuleChecker;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.Calendar;
import java.util.List;

/**
 * Test class for the work module checker.
 *
 * @author florin.haizea
 */
public class OpWorkModuleCheckerTest extends OpBaseOpenTestCase {

   private OpProjectTestDataFactory projectFactory;
   private OpResourceTestDataFactory resourceFactory;
   private OpActivityTestDataFactory activityFactory;
   private OpWorkModuleChecker workChecker;

   private static final String PROJECT_NAME = "project";
   private static final String RESOURCE_NAME = "resource";
   private static final String ACTIVITY_NAME = "activity";

   private String resource1Locator;
   private String resource2Locator;
   private String planLocator;
   private String activity1Locator;
   private String activity2Locator;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      resourceFactory = new OpResourceTestDataFactory(session);
      projectFactory = new OpProjectTestDataFactory(session);
      activityFactory = new OpActivityTestDataFactory(session);
      workChecker = new OpWorkModuleChecker();

      //insert two resources
      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = resourceFactory.createResourceMsg(RESOURCE_NAME + 1, "description", 100d, 2d, 1d, false, poolid);
      XMessage response = OpTestDataFactory.getResourceService().insertResource(session, request);
      assertNoError(response);
      resource1Locator = resourceFactory.getResourceByName(RESOURCE_NAME + 1).locator();

      request = resourceFactory.createResourceMsg(RESOURCE_NAME + 2, "description", 100d, 4d, 3d, false, poolid);
      response = OpTestDataFactory.getResourceService().insertResource(session, request);
      assertNoError(response);
      resource2Locator = resourceFactory.getResourceByName(RESOURCE_NAME + 2).locator();

      //insert a project
      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());
      request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME, startDate, 1d, null, null);
      response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);
      String projectLocator = projectFactory.getProjectId(PROJECT_NAME);

      planLocator = projectFactory.getProjectById(projectLocator).getPlan().locator();

      //insert two activities for the project
      Date activity1Start = new Date(getCalendarWithExactDaySet(2007, 10, 12).getTimeInMillis());
      Date activity1Finish = new Date(getCalendarWithExactDaySet(2007, 10, 14).getTimeInMillis());
      activity1Locator = insertActivity(PROJECT_NAME, ACTIVITY_NAME + 1, OpActivity.STANDARD, activity1Start, activity1Finish);
      activity2Locator = insertActivity(PROJECT_NAME, ACTIVITY_NAME + 2, OpActivity.STANDARD, activity1Start, activity1Finish);
   }

   /**
    * Test the resetting of actual and remaining values on the activities, assignments and work records when the
    * project tracking is on.
    *
    * @throws Exception if the test fails
    */
   public void testResetWorkValuesTrackingOn()
        throws Exception {

      //insert two assignments, one for each activity
      String assignment1Locator = insertAssignment(planLocator, resource1Locator, activity1Locator);
      String assignment2Locator = insertAssignment(planLocator, resource2Locator, activity2Locator);

      //add a work record for each assignment
      insertWorkRecord(assignment1Locator, 10d);
      insertWorkRecord(assignment2Locator, 5d);

      //insert a work month for each assignment
      insertWorkMonth(assignment1Locator);
      insertWorkMonth(assignment2Locator);

      //set the progress tracking of the project plan to true
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //set the values on the assignments
      OpAssignment assignment1 = (OpAssignment) broker.getObject(assignment1Locator);
      assignment1.setActualCosts(5);
      assignment1.setActualEffort(10);
      assignment1.setActualProceeds(20d);
      assignment1.setBaseEffort(24);
      assignment1.setBaseCosts(48);
      assignment1.setBaseProceeds(24d);
      //wrong remaining effort and costs
      assignment1.setRemainingEffort(0);
      assignment1.setRemainingPersonnelCosts(0d);
      assignment1.setRemainingProceeds(0d);
      broker.updateObject(assignment1);

      OpAssignment assignment2 = (OpAssignment) broker.getObject(assignment2Locator);
      assignment2.setActualCosts(3);
      assignment2.setActualEffort(7);
      assignment2.setActualProceeds(12d);
      assignment2.setBaseEffort(24);
      assignment2.setBaseCosts(12);
      assignment2.setBaseProceeds(15d);
      //wrong remaining effort and costs
      assignment2.setRemainingEffort(0);
      assignment2.setRemainingPersonnelCosts(0d);
      assignment2.setRemainingProceeds(0d);
      broker.updateObject(assignment2);

      //set the values on the activities
      OpActivity activity1 = (OpActivity) broker.getObject(activity1Locator);
      activity1.setSequence(0);
      activity1.setActualEffort(10);
      activity1.setActualExternalCosts(5);
      activity1.setActualMaterialCosts(6);
      activity1.setActualMiscellaneousCosts(7);
      activity1.setActualTravelCosts(8);
      activity1.setActualPersonnelCosts(10);
      activity1.setActualProceeds(20d);
      activity1.setBaseEffort(24);
      activity1.setBaseExternalCosts(30d);
      activity1.setBaseMaterialCosts(31d);
      activity1.setBaseMiscellaneousCosts(32d);
      activity1.setBaseTravelCosts(33d);
      //wrong remaining effort and costs
      activity1.setRemainingEffort(0);
      activity1.setRemainingExternalCosts(0d);
      activity1.setRemainingMaterialCosts(0d);
      activity1.setRemainingMiscellaneousCosts(0d);
      activity1.setRemainingTravelCosts(0d);
      broker.updateObject(activity1);

      OpActivity activity2 = (OpActivity) broker.getObject(activity2Locator);
      activity2.setSequence(1);
      activity2.setActualEffort(5);
      activity2.setActualExternalCosts(9);
      activity2.setActualMaterialCosts(10);
      activity2.setActualMiscellaneousCosts(11);
      activity2.setActualPersonnelCosts(12);
      activity2.setActualProceeds(24d);
      activity2.setActualTravelCosts(13);
      activity2.setBaseEffort(24);
      //wrong remaining effort and costs
      activity2.setRemainingEffort(0);
      activity2.setRemainingExternalCosts(40d);
      activity2.setRemainingMaterialCosts(41d);
      activity2.setRemainingMiscellaneousCosts(42d);
      activity2.setRemainingTravelCosts(43d);
      broker.updateObject(activity2);

      //set the values on the work records
      for (OpWorkRecord workRecord : assignment1.getWorkRecords()) {
         workRecord.setRemainingEffort(14d);
         //<FIXME author="Mihai Costin" description="This will have no effect if no cost records are created!">
         workRecord.setExternalCosts(5d);
         workRecord.setMaterialCosts(8d);
         workRecord.setMiscellaneousCosts(10d);
         workRecord.setTravelCosts(12d);
         workRecord.setRemExternalCosts(25d);
         workRecord.setRemMaterialCosts(23d);
         workRecord.setRemMiscCosts(22d);
         workRecord.setRemTravelCosts(21d);
         //</FIXME>
         broker.updateObject(workRecord);
      }

      for (OpWorkRecord workRecord : assignment2.getWorkRecords()) {
         workRecord.setRemainingEffort(19d);
         workRecord.setRemExternalCosts(-2d);
         workRecord.setRemMaterialCosts(-3d);
         workRecord.setRemMiscCosts(-4d);
         workRecord.setRemTravelCosts(0d);
         broker.updateObject(workRecord);
      }

      OpProjectPlan projectPlan = (OpProjectPlan) broker.getObject(planLocator);
      projectPlan.setProgressTracked(true);
      broker.updateObject(projectPlan);

      t.commit();
      broker.close();

      workChecker.check(session);

      //the new values on the assignments, activities and work records
      broker = session.newBroker();
      assignment1 = (OpAssignment) broker.getObject(assignment1Locator);
      assertEquals(20d, assignment1.getActualCosts());
      assertEquals(10d, assignment1.getActualEffort());
      assertEquals(10d, assignment1.getActualProceeds());
      assertEquals(14d, assignment1.getRemainingEffort());
      assertEquals(28d, assignment1.getRemainingPersonnelCosts());
      assertEquals(14d, assignment1.getRemainingProceeds());

      assignment2 = (OpAssignment) broker.getObject(assignment2Locator);
      assertEquals(20d, assignment2.getActualCosts());
      assertEquals(5d, assignment2.getActualEffort());
      assertEquals(15d, assignment2.getActualProceeds());
      assertEquals(19d, assignment2.getRemainingEffort());
      assertEquals(76d, assignment2.getRemainingPersonnelCosts());
      assertEquals(57d, assignment2.getRemainingProceeds());

      activity1 = (OpActivity) broker.getObject(activity1Locator);
      assertEquals(14d, activity1.getRemainingEffort());
      assertEquals(30d, activity1.getRemainingExternalCosts());
      assertEquals(31d, activity1.getRemainingMaterialCosts());
      assertEquals(32d, activity1.getRemainingMiscellaneousCosts());
      assertEquals(33d, activity1.getRemainingTravelCosts());

      activity2 = (OpActivity) broker.getObject(activity2Locator);
      assertEquals(19d, activity2.getRemainingEffort());
      //the remaining costs are 0 because the work record's remaining costs were negative
      assertEquals(0d, activity2.getRemainingExternalCosts());
      assertEquals(0d, activity2.getRemainingMaterialCosts());
      assertEquals(0d, activity2.getRemainingMiscellaneousCosts());
      assertEquals(0d, activity2.getRemainingTravelCosts());

      //the values on work record 1 were not changed
      for (OpWorkRecord workRecord : assignment1.getWorkRecords()) {
         assertEquals(20d, workRecord.getPersonnelCosts());
         assertEquals(30d, workRecord.getRemExternalCosts());
         assertEquals(31d, workRecord.getRemMaterialCosts());
         assertEquals(32d, workRecord.getRemMiscCosts());
         assertEquals(33d, workRecord.getRemTravelCosts());
      }

      //the remaining costs on work record 2 were changed because they were negative
      for (OpWorkRecord workRecord : assignment2.getWorkRecords()) {
         assertEquals(20d, workRecord.getPersonnelCosts());
         assertEquals(0d, workRecord.getRemExternalCosts());
         assertEquals(0d, workRecord.getRemMaterialCosts());
         assertEquals(0d, workRecord.getRemMiscCosts());
         assertEquals(0d, workRecord.getRemTravelCosts());
      }

      broker.close();
   }

   /**
    * Test the resetting of actual and remaining values on the activities, assignments and work records when the
    * project tracking is off.
    *
    * @throws Exception if the test fails
    */
   public void testResetWorkValuesTrackingOff()
        throws Exception {

      //insert two assignments, one for each activity
      String assignment1Locator = insertAssignment(planLocator, resource1Locator, activity1Locator);
      String assignment2Locator = insertAssignment(planLocator, resource2Locator, activity2Locator);

      //add a work record for each assignment
      insertWorkRecord(assignment1Locator, 6d);
      insertWorkRecord(assignment2Locator, 5d);

      //insert a work month for each assignment
      insertWorkMonth(assignment1Locator);
      insertWorkMonth(assignment2Locator);

      //set the progress tracking of the project plan to true
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //set the values on the assignment
      OpAssignment assignment1 = (OpAssignment) broker.getObject(assignment1Locator);
      assignment1.setActualEffort(6);
      assignment1.setBaseEffort(24);
      assignment1.setBaseCosts(48);
      assignment1.setBaseProceeds(24d);
      //wrong remaining effort and costs
      assignment1.setRemainingEffort(0);
      assignment1.setRemainingPersonnelCosts(0d);
      assignment1.setRemainingProceeds(0d);
      broker.updateObject(assignment1);

      //set the values on activity one
      OpActivity activity1 = (OpActivity) broker.getObject(activity1Locator);
      activity1.setSequence(0);
      activity1.setActualEffort(6);
      activity1.setComplete(25);
      activity1.setActualExternalCosts(5);
      activity1.setActualMaterialCosts(6);
      activity1.setActualMiscellaneousCosts(7);
      activity1.setActualTravelCosts(8);
      activity1.setBaseEffort(24);
      activity1.setDuration(24);
      activity1.setBaseExternalCosts(30d);
      activity1.setBaseMaterialCosts(31d);
      activity1.setBaseMiscellaneousCosts(32d);
      activity1.setBaseTravelCosts(33d);
      //wrong remaining effort and costs
      activity1.setRemainingEffort(0);
      activity1.setRemainingExternalCosts(0d);
      activity1.setRemainingMaterialCosts(0d);
      activity1.setRemainingMiscellaneousCosts(0d);
      activity1.setRemainingTravelCosts(0d);
      broker.updateObject(activity1);

      //delete activity two
      OpActivity activity2 = (OpActivity) broker.getObject(activity2Locator);
      activity2.setDeleted(true);
      broker.updateObject(activity2);

      //set the values on the work record belonging to assignment1
      for (OpWorkRecord workRecord : assignment1.getWorkRecords()) {
         workRecord.setRemainingEffort(18d);
         //<FIXME author="Mihai Costin" description="This will have no effect if no cost records are created!">
         workRecord.setExternalCosts(5d);
         workRecord.setMaterialCosts(8d);
         workRecord.setMiscellaneousCosts(10d);
         workRecord.setTravelCosts(12d);
         workRecord.setRemExternalCosts(25d);
         workRecord.setRemMaterialCosts(23d);
         workRecord.setRemMiscCosts(22d);
         workRecord.setRemTravelCosts(21d);
         //</FIXME>
         broker.updateObject(workRecord);
      }

      OpProjectPlan projectPlan = (OpProjectPlan) broker.getObject(planLocator);
      projectPlan.setProgressTracked(false);
      broker.updateObject(projectPlan);

      t.commit();
      broker.close();

      workChecker.check(session);

      //the new values on the assignments, activities and work records
      broker = session.newBroker();
      assignment1 = (OpAssignment) broker.getObject(assignment1Locator);
      assertEquals(12d, assignment1.getActualCosts());
      assertEquals(6d, assignment1.getActualEffort());
      assertEquals(6d, assignment1.getActualProceeds());
      assertEquals(18d, assignment1.getRemainingEffort());
      assertEquals(36d, assignment1.getRemainingPersonnelCosts());
      assertEquals(18d, assignment1.getRemainingProceeds());
      assertEquals(25d, assignment1.getComplete());

      activity1 = (OpActivity) broker.getObject(activity1Locator);
      assertEquals(18d, activity1.getRemainingEffort());
      assertEquals(30d, activity1.getRemainingExternalCosts());
      assertEquals(31d, activity1.getRemainingMaterialCosts());
      assertEquals(32d, activity1.getRemainingMiscellaneousCosts());
      assertEquals(33d, activity1.getRemainingTravelCosts());

      //the values on work record 1 were not changed
      for (OpWorkRecord workRecord : assignment1.getWorkRecords()) {
         assertEquals(12d, workRecord.getPersonnelCosts());
         assertEquals(30d, workRecord.getRemExternalCosts());
         assertEquals(31d, workRecord.getRemMaterialCosts());
         assertEquals(32d, workRecord.getRemMiscCosts());
         assertEquals(33d, workRecord.getRemTravelCosts());
      }

      broker.close();
   }

   /**
    * Test the resetting of the values on the work months.
    *
    * @throws Exception if the test fails
    */
   public void testResetWorkMonths()
        throws Exception {

      //insert one assignment for activity one
      String assignment1Locator = insertAssignment(planLocator, resource1Locator, activity1Locator);

      //add a work record for the assignment
      insertWorkRecord(assignment1Locator, 6d);

      //insert a work month for the assignment
      insertWorkMonth(assignment1Locator);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //set the values on the assignment
      OpAssignment assignment1 = (OpAssignment) broker.getObject(assignment1Locator);
      assignment1.setActualEffort(6);
      assignment1.setBaseEffort(24);
      assignment1.setBaseCosts(48);
      assignment1.setBaseProceeds(24d);
      //wrong remaining effort and costs
      assignment1.setRemainingEffort(0);
      assignment1.setRemainingPersonnelCosts(0d);
      assignment1.setRemainingProceeds(0d);
      broker.updateObject(assignment1);

      //set the values on activity one
      OpActivity activity1 = (OpActivity) broker.getObject(activity1Locator);
      activity1.setSequence(0);
      activity1.setActualEffort(6);
      activity1.setComplete(25);
      activity1.setActualExternalCosts(5);
      activity1.setActualMaterialCosts(6);
      activity1.setActualMiscellaneousCosts(7);
      activity1.setActualTravelCosts(8);
      activity1.setBaseEffort(24);
      activity1.setDuration(24);
      activity1.setBaseExternalCosts(30d);
      activity1.setBaseMaterialCosts(31d);
      activity1.setBaseMiscellaneousCosts(32d);
      activity1.setBaseTravelCosts(33d);
      //wrong remaining effort and costs
      activity1.setRemainingEffort(0);
      activity1.setRemainingExternalCosts(0d);
      activity1.setRemainingMaterialCosts(0d);
      activity1.setRemainingMiscellaneousCosts(0d);
      activity1.setRemainingTravelCosts(0d);
      broker.updateObject(activity1);

      //delete activity two
      OpActivity activity2 = (OpActivity) broker.getObject(activity2Locator);
      activity2.setDeleted(true);
      broker.updateObject(activity2);

      //set the values on the work record belonging to assignment1
      for (OpWorkRecord workRecord : assignment1.getWorkRecords()) {
         workRecord.setRemainingEffort(18d);
         workRecord.setExternalCosts(5d);
         workRecord.setMaterialCosts(8d);
         workRecord.setMiscellaneousCosts(10d);
         workRecord.setTravelCosts(12d);
         workRecord.setRemExternalCosts(25d);
         workRecord.setRemMaterialCosts(23d);
         workRecord.setRemMiscCosts(22d);
         workRecord.setRemTravelCosts(21d);
         broker.updateObject(workRecord);
      }

      OpProjectPlan projectPlan = (OpProjectPlan) broker.getObject(planLocator);
      projectPlan.setProgressTracked(false);
      broker.updateObject(projectPlan);

      t.commit();
      broker.close();

      workChecker.check(session);

      //the values on the work month belonging to assignment1
      XCalendar xCalendar = XCalendar.getDefaultCalendar();
      Calendar calendar = xCalendar.getCalendar();

      broker = session.newBroker();
      assignment1 = (OpAssignment) broker.getObject(assignment1Locator);
      activity1 = assignment1.getActivity();
      for (OpWorkMonth workMonth : assignment1.getWorkMonths()) {
         calendar.setTime(activity1.getStart());
         assertEquals(workMonth.getYear(), calendar.get(Calendar.YEAR));
         assertEquals(workMonth.getMonth(), calendar.get(Calendar.MONTH));
         List activityWorkingDays = xCalendar.getWorkingDaysFromInterval(activity1.getStart(), activity1.getFinish());
         assertEquals(activityWorkingDays.size(), workMonth.getWorkingDays());
         assertEquals(100d, workMonth.getLatestAssigned());
         assertEquals(24d, workMonth.getBaseEffort());
         assertEquals(24d, workMonth.getLatestEffort());
         assertEquals(48d, workMonth.getBasePersonnelCosts());
         assertEquals(48d, workMonth.getLatestPersonnelCosts());
         assertEquals(24d, workMonth.getBaseProceeds());
         assertEquals(24d, workMonth.getLatestProceeds());
         assertEquals(18d, workMonth.getRemainingEffort());
         assertEquals(36d, workMonth.getRemainingPersonnel());
         assertEquals(18d, workMonth.getRemainingProceeds());
      }

      broker.close();
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
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();

      deleteAllObjects(broker, OpWorkRecord.WORK_RECORD);
      deleteAllObjects(broker, OpWorkMonth.WORK_MONTH);
      deleteAllObjects(broker, OpAssignment.ASSIGNMENT);
      deleteAllObjects(broker, OpActivity.ACTIVITY);
      deleteAllObjects(broker, OpProjectPlan.PROJECT_PLAN);

      for (Object aProjectList : projectFactory.getAllProjects(broker)) {
         OpProjectNode project = (OpProjectNode) aProjectList;
         broker.deleteObject(project);
      }

      for (OpResource resource : resourceFactory.getAllResources(broker)) {
         broker.deleteObject(resource);
      }

      transaction.commit();
      broker.close();
   }

   /**
    * Inserts one <code>OpActivity</code> object for a project which has the name passed as the "project name"
    * parameter. The activity will have its name, type, start and finish dates passed as parameters.
    *
    * @param projectName  - the name of the project to which the activity belongs.
    * @param activityName - the name of the activity.
    * @param type         - the type of the activity.
    * @param start        - the start date of the activity.
    * @param finish       - the finish date of the activity.
    * @return - the locator of the newly inserted activity.
    */
   private String insertActivity(String projectName, String activityName, byte type, Date start, Date finish) {
      OpProjectNode project = projectFactory.getProjectByName(projectName);

      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();

         //insert the activity
         OpActivity activity = new OpActivity();
         activity.setName(activityName);
         activity.setType(type);
         activity.setStart(start);
         activity.setFinish(finish);

         activity.setProjectPlan(project.getPlan());
         broker.makePersistent(activity);

         t.commit();
         return activity.locator();
      }
      finally {
         broker.close();
      }
   }

   /**
    * Inserts an <code>OpAssignment</code> object in the DB. The assignment's activity, resource and project plan are
    * specified by their locators.
    *
    * @param projectPlanLocator - the locator of the project plan for which the assignment is inserted.
    * @param resourceLocator    - the locator of the resource for which the assignment is inserted.
    * @param activityLocator    - the locator of the activity for which the assignment is inserted.
    * @return - the locator of the newly inserted assignment.
    */
   private String insertAssignment(String projectPlanLocator, String resourceLocator, String activityLocator) {
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      try {
         OpAssignment assignment = new OpAssignment();
         OpProjectPlan projectPlan = projectFactory.getProjectPlanById(projectPlanLocator);
         OpResource resource = resourceFactory.getResourceById(resourceLocator);

         assignment.setActivity(activityFactory.getActivityById(activityLocator));
         assignment.setResource(resource);
         assignment.setProjectPlan(projectPlan);

         //insert the project node assignment wich ties the resource to the project node
         OpProjectNodeAssignment projectAssignment = new OpProjectNodeAssignment();
         projectAssignment.setResource(resource);
         projectAssignment.setProjectNode(projectPlan.getProjectNode());

         broker.makePersistent(assignment);
         broker.makePersistent(projectAssignment);

         t.commit();
         return assignment.locator();
      }
      finally {
         broker.close();
      }
   }

   /**
    * Inserts an <code>OpWorkRecord</code> object in the DB for the assignment specified by the assignmentLocator
    * parameter.
    *
    * @param assignmentLocator - the locator of the assignment for which the work record is inserted.
    * @param actualEffort      - the value of the actual effort on the assignment.
    */
   private void insertWorkRecord(String assignmentLocator, Double actualEffort) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();

         OpWorkRecord workRecord = new OpWorkRecord();
         workRecord.setActualEffort(actualEffort);
         workRecord.setAssignment(projectFactory.getAssignmentById(assignmentLocator));
         broker.makePersistent(workRecord);

         t.commit();
      }
      finally {
         broker.close();
      }
   }

   /**
    * Inserts an <code>OpWorkMonth</code> object in the DB for the assignment specified by the assignmentLocator
    * parameter.
    *
    * @param assignmentLocator - the locator of the assignment for which the work record is inserted.
    */
   private void insertWorkMonth(String assignmentLocator) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = broker.newTransaction();

         XCalendar xCalendar = XCalendar.getDefaultCalendar();
         OpAssignment assignment = projectFactory.getAssignmentById(assignmentLocator);
         OpActivity activity = assignment.getActivity();
         Calendar calendar = XCalendar.setCalendarTimeToZero(activity.getStart());
         OpWorkMonth workMonth = new OpWorkMonth();
         workMonth.setYear(calendar.get(Calendar.YEAR));
         workMonth.setMonth((byte) calendar.get(Calendar.MONTH));
         List activityWorkingDays = xCalendar.getWorkingDaysFromInterval(activity.getStart(), activity.getFinish());
         workMonth.setWorkingDays((byte) activityWorkingDays.size());
         workMonth.setAssignment(assignment);
         broker.makePersistent(workMonth);

         t.commit();
      }

      finally {
         broker.close();
      }
   }
}
