/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project_planning.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.OpActivityTestDataFactory;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningModuleChecker;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.List;

/**
 * Test class for the project planing module checker.
 *
 * @author florin.haizea
 */
public class OpProjectPlanningModuleCheckerTest extends OpBaseOpenTestCase {

   private static final String PROJECT_NAME1 = "project1";
   private static final String PROJECT_NAME2 = "project2";
   private static final String ACTIVITY_NAME1 = "activity1";
   private static final String ACTIVITY_NAME2 = "activity2";
   private static final String ACTIVITY_NAME3 = "activity3";
   private static final String ACTIVITY_NAME4 = "activity4";

   private OpProjectAdministrationService projectService;
   private OpProjectTestDataFactory projectFactory;
   private OpResourceTestDataFactory resourceFactory;
   private OpActivityTestDataFactory activityFactory;
   private OpProjectPlanningModuleChecker projectPlanningChecker;

   private String resource1Locator;
   private String resource2Locator;
   private String planLocator;
   private String activity1Locator;
   private String activity2Locator;

   private static final String PROJECT_NAME = "project";
   private static final String RESOURCE_NAME = "resource";
   private static final String ACTIVITY_NAME = "activity";

   private static final String ALL_WORK_RECORDS_QUERY = "from OpWorkRecord";
   private static final String ALL_ACTIVITY_ASSIGNMENTS_QUERY = "from OpAssignment";

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      projectService = OpTestDataFactory.getProjectService();
      projectFactory = new OpProjectTestDataFactory(session);
      resourceFactory = new OpResourceTestDataFactory(session);
      activityFactory = new OpActivityTestDataFactory(session);
      projectPlanningChecker = new OpProjectPlanningModuleChecker();

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
    * Test the project planning module checker when no project and no activities are present in the DB.
    *
    * @throws Exception if the test fails
    */
   public void testCheckNoProject()
        throws Exception {
      projectPlanningChecker.check(session);
   }

   /**
    * Test the project planning module checker when there is only one project with no activities. The start and
    * finish dates of the project plan are set.
    *
    * @throws Exception if the test fails
    */
   public void testCheckNoActivitiesWithDatesSet()
        throws Exception {
      //insert the project
      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());
      Date finishDate = new Date(getCalendarWithExactDaySet(2007, 10, 15).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME1, startDate, finishDate, 100d, null, null);
      XMessage response = projectService.insertProject(session, request);
      assertNoError(response);

      String projectLocator = projectFactory.getProjectId(PROJECT_NAME1);
      OpProjectNode project = projectFactory.getProjectById(projectLocator);

      //by this time the project plan should have its start and finish dates set
      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(finishDate, project.getPlan().getFinish());

      projectPlanningChecker.check(session);

      //the project planning checker shouldn't have changed the start and finish dates of the project plan
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(OpProjectNode.class, project.getID());
      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(finishDate, project.getPlan().getFinish());
      broker.close();
   }

   /**
    * Test the project planning module checker when there is only one project with no activities. The finish date
    *    of the project is not set.
    *
    * @throws Exception if the test fails
    */
   public void testCheckNoActivitiesFinishDateNotSet()
        throws Exception {
      //insert the project
      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME1, startDate, 100d, null, null);
      XMessage response = projectService.insertProject(session, request);
      assertNoError(response);

      String projectLocator = projectFactory.getProjectId(PROJECT_NAME1);
      OpProjectNode project = projectFactory.getProjectById(projectLocator);

      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(startDate, project.getPlan().getFinish());

      projectPlanningChecker.check(session);

      //the project plan should have the finish date the same as the start date
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(OpProjectNode.class, project.getID());
      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(startDate, project.getPlan().getFinish());
      broker.close();
   }

   /**
    * Test the project planning module checker when there is only one project with one standard activity. The finish
    *    date of the project is not set.
    *
    * @throws Exception if the test fails
    */
   public void testCheckOneActivityFinishDateNotSet()
        throws Exception {
      //insert the project
      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME1, startDate, 100d, null, null);
      XMessage response = projectService.insertProject(session, request);
      assertNoError(response);

      String projectLocator = projectFactory.getProjectId(PROJECT_NAME1);
      OpProjectNode project = projectFactory.getProjectById(projectLocator);

      //insert the activity for the given project
      Date activityStart = new Date(getCalendarWithExactDaySet(2007, 10, 11).getTimeInMillis());
      Date activityFinish = new Date(getCalendarWithExactDaySet(2007, 10, 15).getTimeInMillis());
      insertActivity(PROJECT_NAME1, ACTIVITY_NAME1, OpActivity.STANDARD, activityStart, activityFinish);

      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(startDate, project.getPlan().getFinish());

      projectPlanningChecker.check(session);

      //the project plan should have the same finish date as the activity's finish date
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(OpProjectNode.class, project.getID());
      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(activityFinish, project.getPlan().getFinish());
      broker.close();
   }

   /**
    * Test the project planning module checker when there is only one project with one standard activity. The finish
    * date of the project is set.
    *
    * @throws Exception if the test fails
    */
   public void testCheckOneActivityFinishDateSet()
        throws Exception {
      //insert the project
      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());
      Date finishDate = new Date(getCalendarWithExactDaySet(2007, 10, 20).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME1, startDate, finishDate, 100d, null, null);
      XMessage response = projectService.insertProject(session, request);
      assertNoError(response);

      String projectLocator = projectFactory.getProjectId(PROJECT_NAME1);
      OpProjectNode project = projectFactory.getProjectById(projectLocator);

      //insert the activity for the given project
      Date activityStart = new Date(getCalendarWithExactDaySet(2007, 10, 11).getTimeInMillis());
      Date activityFinish = new Date(getCalendarWithExactDaySet(2007, 10, 15).getTimeInMillis());
      insertActivity(PROJECT_NAME1, ACTIVITY_NAME1, OpActivity.STANDARD, activityStart, activityFinish);

      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(finishDate, project.getPlan().getFinish());

      projectPlanningChecker.check(session);

      //the project plan should have the same finish date as the project's finish date
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(OpProjectNode.class, project.getID());
      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(finishDate, project.getPlan().getFinish());
      broker.close();
   }

   /**
    * Test the project planning module checker when there is only one project with one milestone activity. The finish
    *    date of the project is not set.
    *
    * @throws Exception if the test fails
    */
   public void testCheckOneMilestoneFinishDateNotSet()
        throws Exception {
      //insert the project
      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME1, startDate, 100d, null, null);
      XMessage response = projectService.insertProject(session, request);
      assertNoError(response);

      String projectLocator = projectFactory.getProjectId(PROJECT_NAME1);
      OpProjectNode project = projectFactory.getProjectById(projectLocator);

      //insert the activity for the given project
      Date activityStart = new Date(getCalendarWithExactDaySet(2007, 10, 12).getTimeInMillis());
      insertActivity(PROJECT_NAME1, ACTIVITY_NAME1, OpActivity.MILESTONE, activityStart, activityStart);

      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(startDate, project.getPlan().getFinish());

      projectPlanningChecker.check(session);

      //the project plan should have the same finish date as the milestone's finish date
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(OpProjectNode.class, project.getID());
      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(activityStart, project.getPlan().getFinish());
      broker.close();
   }

   /**
    * Test the project planning module checker when there is only one project with one adhoc task activity. The finish
    * date of the project is not set.
    *
    * @throws Exception if the test fails
    */
   public void testCheckOneAdhocTaskFinishDateNotSet()
        throws Exception {
      //insert the project
      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME1, startDate, 100d, null, null);
      XMessage response = projectService.insertProject(session, request);
      assertNoError(response);

      String projectLocator = projectFactory.getProjectId(PROJECT_NAME1);
      OpProjectNode project = projectFactory.getProjectById(projectLocator);

      //insert the activity for the given project
      insertActivity(PROJECT_NAME1, ACTIVITY_NAME1, OpActivity.ADHOC_TASK, null, null);

      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(startDate, project.getPlan().getFinish());

      projectPlanningChecker.check(session);

      //the project plan should have the same finish date as the project's start date
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(OpProjectNode.class, project.getID());
      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(startDate, project.getPlan().getFinish());
      broker.close();
   }

   /**
    * Test the project planning module checker when there is only one project with two activities. The finish
    *    date of the project is not set.
    *
    * @throws Exception if the test fails
    */
   public void testCheckTwoActivitiesFinishDateNotSet()
        throws Exception {
      //insert the project
      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME1, startDate, 100d, null, null);
      XMessage response = projectService.insertProject(session, request);
      assertNoError(response);

      String projectLocator = projectFactory.getProjectId(PROJECT_NAME1);
      OpProjectNode project = projectFactory.getProjectById(projectLocator);

      //insert the two activities for the given project
      Date activityStart = new Date(getCalendarWithExactDaySet(2007, 10, 12).getTimeInMillis());
      insertActivity(PROJECT_NAME1, ACTIVITY_NAME1, OpActivity.MILESTONE, activityStart, activityStart);
      insertActivity(PROJECT_NAME1, ACTIVITY_NAME2, OpActivity.ADHOC_TASK, null, null);

      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(startDate, project.getPlan().getFinish());

      projectPlanningChecker.check(session);

      //the project plan should have the same finish date as the milestone's start date
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(OpProjectNode.class, project.getID());
      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(activityStart, project.getPlan().getFinish());
      broker.close();
   }

   /**
    * Test the project planning module checker when there is only one project with two activities. The finish
    *    date of the project is set.
    *
    * @throws Exception if the test fails
    */
   public void testCheckTwoActivitiesFinishDateSet()
        throws Exception {
      //insert the project
      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());
      Date finishDate = new Date(getCalendarWithExactDaySet(2007, 10, 20).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME1, startDate, finishDate, 100d, null, null);
      XMessage response = projectService.insertProject(session, request);
      assertNoError(response);

      String projectLocator = projectFactory.getProjectId(PROJECT_NAME1);
      OpProjectNode project = projectFactory.getProjectById(projectLocator);

      //insert the two activities for the given project
      Date milestoneStart = new Date(getCalendarWithExactDaySet(2007, 10, 12).getTimeInMillis());
      Date activityStart = new Date(getCalendarWithExactDaySet(2007, 10, 13).getTimeInMillis());
      Date activityFinish = new Date(getCalendarWithExactDaySet(2007, 10, 27).getTimeInMillis());
      insertActivity(PROJECT_NAME1, ACTIVITY_NAME1, OpActivity.MILESTONE, milestoneStart, milestoneStart);
      insertActivity(PROJECT_NAME1, ACTIVITY_NAME2, OpActivity.STANDARD, activityStart, activityFinish);

      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(finishDate, project.getPlan().getFinish());

      projectPlanningChecker.check(session);

      //the project plan should have the same finish date as the activity's start date
      OpBroker broker = session.newBroker();
      project = (OpProjectNode) broker.getObject(OpProjectNode.class, project.getID());
      assertEquals(startDate, project.getPlan().getStart());
      assertEquals(activityFinish, project.getPlan().getFinish());
      broker.close();
   }

   /**
    * Test the project planning module checker when there are two projects with two activities each.
    *
    * @throws Exception if the test fails
    */
   public void testCheckTwoProjects()
        throws Exception {
      //insert the projects
      Date proj1Start = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());
      Date proj1Finish = new Date(getCalendarWithExactDaySet(2007, 10, 20).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME1, proj1Start, proj1Finish, 100d, null, null);
      XMessage response = projectService.insertProject(session, request);
      assertNoError(response);
      
      Date proj2Start = new Date(getCalendarWithExactDaySet(2007, 10, 13).getTimeInMillis());
      Date proj2Finish = new Date(getCalendarWithExactDaySet(2007, 10, 18).getTimeInMillis());

      request = OpProjectTestDataFactory.createProjectMsg(PROJECT_NAME2, proj2Start, proj2Finish, 100d, null, null);
      response = projectService.insertProject(session, request);
      assertNoError(response);

      String projectLocator = projectFactory.getProjectId(PROJECT_NAME1);
      OpProjectNode project1 = projectFactory.getProjectById(projectLocator);
      projectLocator = projectFactory.getProjectId(PROJECT_NAME2);
      OpProjectNode project2 = projectFactory.getProjectById(projectLocator);

      //insert two activities for each project
      Date milestoneStart = new Date(getCalendarWithExactDaySet(2007, 10, 12).getTimeInMillis());
      Date activity1Start = new Date(getCalendarWithExactDaySet(2007, 10, 13).getTimeInMillis());
      Date activity1Finish = new Date(getCalendarWithExactDaySet(2007, 10, 27).getTimeInMillis());
      insertActivity(PROJECT_NAME1, ACTIVITY_NAME1, OpActivity.MILESTONE, milestoneStart, milestoneStart);
      insertActivity(PROJECT_NAME1, ACTIVITY_NAME2, OpActivity.STANDARD, activity1Start, activity1Finish);

      Date activity2Start = new Date(getCalendarWithExactDaySet(2007, 10, 13).getTimeInMillis());
      Date activity2Finish = new Date(getCalendarWithExactDaySet(2007, 10, 15).getTimeInMillis());
      insertActivity(PROJECT_NAME2, ACTIVITY_NAME3, OpActivity.ADHOC_TASK, null, null);
      insertActivity(PROJECT_NAME2, ACTIVITY_NAME4, OpActivity.STANDARD, activity2Start, activity2Finish);

      assertEquals(proj1Start, project1.getPlan().getStart());
      assertEquals(proj1Finish, project1.getPlan().getFinish());
      assertEquals(proj2Start, project2.getPlan().getStart());
      assertEquals(proj2Finish, project2.getPlan().getFinish());

      projectPlanningChecker.check(session);

      //the project plan should have the same finish date as the activity's start date
      OpBroker broker = session.newBroker();
      project1 = (OpProjectNode) broker.getObject(OpProjectNode.class, project1.getID());
      project2 = (OpProjectNode) broker.getObject(OpProjectNode.class, project2.getID());
      assertEquals(proj1Start, project1.getPlan().getStart());
      assertEquals(activity1Finish, project1.getPlan().getFinish());
      assertEquals(proj2Start, project2.getPlan().getStart());
      assertEquals(proj2Finish, project2.getPlan().getFinish());
      broker.close();
   }

   /**
    * Test the work module checker when there is one deleted activity in the DB.
    *
    * @throws Exception if the test fails
    */
   public void testCheckDeletedActivity()
        throws Exception {

      //insert two assignments, one for each activity
      String assignment1Locator = insertAssignment(planLocator, resource1Locator, activity1Locator);
      String assignment2Locator = insertAssignment(planLocator, resource2Locator, activity2Locator);

      //add a work record for each assignment
      insertWorkRecord(assignment1Locator, 10d);
      insertWorkRecord(assignment2Locator, 5d);

      //mark the first activity as deleted and set the order of the activities in the dataset
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity1 = (OpActivity) broker.getObject(activity1Locator);
      activity1.setDeleted(true);
      OpActivity activity2 = (OpActivity) broker.getObject(activity2Locator);
      broker.updateObject(activity1);
      broker.updateObject(activity2);

      t.commit();
      broker.close();

      //each assignment has a work record associated to it
      assertEquals(1, projectFactory.getAssignmentById(assignment1Locator).getWorkRecords().size());
      assertEquals(1, projectFactory.getAssignmentById(assignment2Locator).getWorkRecords().size());

      projectPlanningChecker.check(session);

      //since activity1 was deleted assignment1 and all work records associated to it were deleted
      broker = session.newBroker();
      activity1 = (OpActivity) broker.getObject(activity1Locator);
      assertFalse(activity1.getAssignments().iterator().hasNext());
      assertEquals(1, projectFactory.getAssignmentById(assignment2Locator).getWorkRecords().size());
      broker.close();
   }

   /**
    * Test the work module checker when all the activities are deleted.
    *
    * @throws Exception if the test fails
    */
   public void testCheckDeletedActivities()
        throws Exception {

      //insert two assignments, one for each activity
      String assignment1Locator = insertAssignment(planLocator, resource1Locator, activity1Locator);
      String assignment2Locator = insertAssignment(planLocator, resource2Locator, activity2Locator);

      //add a work record for each assignment
      insertWorkRecord(assignment1Locator, 10d);
      insertWorkRecord(assignment2Locator, 5d);

      //mark the first activity as deleted and set the order of the activities
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity1 = (OpActivity) broker.getObject(activity1Locator);
      OpActivity activity2 = (OpActivity) broker.getObject(activity2Locator);
      activity1.setDeleted(true);
      activity1.setSequence(0);
      activity2.setDeleted(true);
      activity2.setSequence(1);
      broker.updateObject(activity1);
      broker.updateObject(activity2);

      t.commit();
      broker.close();

      //each assignment has a work record associated to it
      assertEquals(1, projectFactory.getAssignmentById(assignment1Locator).getWorkRecords().size());
      assertEquals(1, projectFactory.getAssignmentById(assignment2Locator).getWorkRecords().size());

      projectPlanningChecker.check(session);

      //since the activities were deleted the assignments and the work records should have been deleted
      broker = session.newBroker();
      OpQuery workRecordsQuery = broker.newQuery(ALL_WORK_RECORDS_QUERY);
      OpQuery assignmentsQuery = broker.newQuery(ALL_ACTIVITY_ASSIGNMENTS_QUERY);
      assertFalse(broker.iterate(workRecordsQuery).hasNext());
      assertFalse(broker.iterate(assignmentsQuery).hasNext());
      broker.close();
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

      for (OpUser user : usrData.getAllUsers(broker)) {
         if (user.getName().equals(OpUser.ADMINISTRATOR_NAME)) {
            continue;
         }
         broker.deleteObject(user);
      }

      deleteAllObjects(broker, OpWorkRecord.WORK_RECORD);      
      deleteAllObjects(broker, OpAssignment.ASSIGNMENT);
      deleteAllObjects(broker, OpActivity.ACTIVITY);
      deleteAllObjects(broker, OpProjectPlan.PROJECT_PLAN);

      for (OpProjectNode project : projectFactory.getAllProjects(broker)) {
         broker.deleteObject(project);
      }

      for (OpResource resource : resourceFactory.getAllResources(broker)) {
         broker.deleteObject(resource);
      }

      List portofolioList = projectFactory.getAllPortofolios(broker);
      for (Object aPortofolioList : portofolioList) {
         OpProjectNode portofolio = (OpProjectNode) aPortofolioList;
         if (portofolio.getName().equals(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)) {
            continue;
         }
         broker.deleteObject(portofolio);
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
      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project = projectFactory.getProjectByName(projectName);
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
      try {
         OpTransaction t = broker.newTransaction();

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
}