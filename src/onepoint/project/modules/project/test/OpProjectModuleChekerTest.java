/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.*;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.List;

/**
 * Test class for the project module checker.
 *
 * @author florin.haizea
 */
public class OpProjectModuleChekerTest extends OpBaseOpenTestCase {

   private static final String PROJECT_NAME1 = "project1";
   private static final String PROJECT_NAME2 = "project2";
   private static final String ACTIVITY_VERSION_NAME1 = "activityVersion1";
   private static final String ACTIVITY_VERSION_NAME2 = "activityVersion2";
   private static final String ACTIVITY_VERSION_NAME3 = "activityVersion3";
   private static final String ACTIVITY_VERSION_NAME4 = "activityVersion4";

   private OpProjectAdministrationService projectService;
   private OpActivityTestDataFactory activityFactory;
   private OpProjectTestDataFactory dataFactory;
   private OpProjectModuleChecker projectChecker;

   private static final String COUNT_ALL_OBJECTS_QUERY = "select count(*) from OpObject";

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      projectService = OpTestDataFactory.getProjectService();
      dataFactory = new OpProjectTestDataFactory(session);
      activityFactory = new OpActivityTestDataFactory(session);
      projectChecker = new OpProjectModuleChecker();
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
    * Test the project module checker when no project and no activities are present in the DB.
    *
    * @throws Exception if the test fails
    */
   public void testCheckNoProject()
        throws Exception {

      int beforeCount = countAllDBObjects();
      projectChecker.check(session);
      int afterCount = countAllDBObjects();

      assertEquals("OpProjectModuleCheker error", beforeCount, afterCount);
   }

   /**
    * Test the project module checker when there are projects and activity versions but no assignment versions
    * in the database.
    *
    * @throws Exception if the test fails
    */
   public void testCheckNoAssignmentVersions()
        throws Exception {

      insertProjectWithTwoActivityVersions(PROJECT_NAME1, ACTIVITY_VERSION_NAME1, ACTIVITY_VERSION_NAME2);
      int beforeCount = countAllDBObjects();
      projectChecker.check(session);
      int afterCount = countAllDBObjects();

      assertEquals("OpProjectModuleCheker error", beforeCount, afterCount);
   }

   /**
    * Test the project module checker when there are projects and activity versions which have assignment versions
    * but each assignment version is linked to an activity version.
    *
    * @throws Exception if the test fails
    */
   public void testCheckLinkedAssignmentVersions()
        throws Exception {
      insertProjectWithTwoActivityVersions(PROJECT_NAME1, ACTIVITY_VERSION_NAME1, ACTIVITY_VERSION_NAME2);
      insertLinkedAssignmentVersions(PROJECT_NAME1, ACTIVITY_VERSION_NAME1, ACTIVITY_VERSION_NAME2);

      int beforeCount = countAllDBObjects();
      projectChecker.check(session);
      int afterCount = countAllDBObjects();
      assertEquals("OpProjectModuleCheker error", beforeCount, afterCount);

      //check that the assignment versions were not deleted
      String idActivity1 = activityFactory.getActivityVersionId(ACTIVITY_VERSION_NAME1);
      OpActivityVersion activityVersion1 = activityFactory.getActivityVersionById(idActivity1);
      String idActivity2 = activityFactory.getActivityVersionId(ACTIVITY_VERSION_NAME2);
      OpActivityVersion activityVersion2 = activityFactory.getActivityVersionById(idActivity2);

      assertEquals(1, activityVersion1.getAssignmentVersions().size());
      assertEquals(1, activityVersion2.getAssignmentVersions().size());
   }

   /**
    * Test the project module checker when there are projects and activity versions which have assignment versions
    * and some assignment versions are not linked to any activity versions.
    *
    * @throws Exception if the test fails
    */
   public void testCheckUnlinkedAssignmentVersions()
        throws Exception {
      insertProjectWithTwoActivityVersions(PROJECT_NAME1, ACTIVITY_VERSION_NAME1, ACTIVITY_VERSION_NAME2);
      insertLinkedAssignmentVersions(PROJECT_NAME1, ACTIVITY_VERSION_NAME1, ACTIVITY_VERSION_NAME2);
      long assignmentVersionID = insertUnlinkedAssignmentVersion(PROJECT_NAME1);

      //check the deletion of the unlinked assignment version object
      OpBroker broker = session.newBroker();
      assertNotNull(broker.getObject(OpAssignmentVersion.class, assignmentVersionID));
      broker.close();
      projectChecker.check(session);
      broker = session.newBroker();
      assertNull(broker.getObject(OpAssignmentVersion.class, assignmentVersionID));
      broker.close();

      //check that the linked assignment versions were not deleted
      String idActivity1 = activityFactory.getActivityVersionId(ACTIVITY_VERSION_NAME1);
      OpActivityVersion activityVersion1 = activityFactory.getActivityVersionById(idActivity1);
      String idActivity2 = activityFactory.getActivityVersionId(ACTIVITY_VERSION_NAME2);
      OpActivityVersion activityVersion2 = activityFactory.getActivityVersionById(idActivity2);

      assertEquals(1, activityVersion1.getAssignmentVersions().size());
      assertEquals(1, activityVersion2.getAssignmentVersions().size());
   }

   /**
    * Test the project module checker when there are more assignment versions linked to the activities of 2 projects
    * and 2 unlinked assignment versions belonging to two different projects.
    *
    * @throws Exception if the test fails
    */
   public void testCheckUnlinkedAssignmentVersionsMultipleProjects()
        throws Exception {
      insertProjectWithTwoActivityVersions(PROJECT_NAME1, ACTIVITY_VERSION_NAME1, ACTIVITY_VERSION_NAME2);
      insertLinkedAssignmentVersions(PROJECT_NAME1, ACTIVITY_VERSION_NAME1, ACTIVITY_VERSION_NAME2);
      long assignmentVersion1ID = insertUnlinkedAssignmentVersion(PROJECT_NAME1);
      insertProjectWithTwoActivityVersions(PROJECT_NAME2, ACTIVITY_VERSION_NAME3, ACTIVITY_VERSION_NAME4);
      insertLinkedAssignmentVersions(PROJECT_NAME2, ACTIVITY_VERSION_NAME3, ACTIVITY_VERSION_NAME4);
      long assignmentVersion2ID = insertUnlinkedAssignmentVersion(PROJECT_NAME2);

      //check the deletion of the unlinked assignment version objects
      OpBroker broker = session.newBroker();
      assertNotNull(broker.getObject(OpAssignmentVersion.class, assignmentVersion1ID));
      assertNotNull(broker.getObject(OpAssignmentVersion.class, assignmentVersion2ID));
      broker.close();
      projectChecker.check(session);
      broker = session.newBroker();
      assertNull(broker.getObject(OpAssignmentVersion.class, assignmentVersion1ID));
      assertNull(broker.getObject(OpAssignmentVersion.class, assignmentVersion2ID));
      broker.close();

      //check that the linked assignment versions were not deleted
      String idActivity1 = activityFactory.getActivityVersionId(ACTIVITY_VERSION_NAME1);
      OpActivityVersion activityVersion1 = activityFactory.getActivityVersionById(idActivity1);
      String idActivity2 = activityFactory.getActivityVersionId(ACTIVITY_VERSION_NAME2);
      OpActivityVersion activityVersion2 = activityFactory.getActivityVersionById(idActivity2);
      String idActivity3 = activityFactory.getActivityVersionId(ACTIVITY_VERSION_NAME3);
      OpActivityVersion activityVersion3 = activityFactory.getActivityVersionById(idActivity3);
      String idActivity4 = activityFactory.getActivityVersionId(ACTIVITY_VERSION_NAME4);
      OpActivityVersion activityVersion4 = activityFactory.getActivityVersionById(idActivity4);

      assertEquals(1, activityVersion1.getAssignmentVersions().size());
      assertEquals(1, activityVersion2.getAssignmentVersions().size());
      assertEquals(1, activityVersion3.getAssignmentVersions().size());
      assertEquals(1, activityVersion4.getAssignmentVersions().size());
   }

   /**
    * Test the project module checker when there are only assignment versions which have no links to any
    *    activity versions.
    *
    * @throws Exception if the test fails
    */
   public void testCheckOnlyUnlinkedAssignmentVersions()
        throws Exception {
      insertProjectWithTwoActivityVersions(PROJECT_NAME1, ACTIVITY_VERSION_NAME1, ACTIVITY_VERSION_NAME2);
      long assignmentVersion1ID = insertUnlinkedAssignmentVersion(PROJECT_NAME1);
      long assignmentVersion2ID = insertUnlinkedAssignmentVersion(PROJECT_NAME1);
      insertProjectWithTwoActivityVersions(PROJECT_NAME2, ACTIVITY_VERSION_NAME3, ACTIVITY_VERSION_NAME4);
      long assignmentVersion3ID = insertUnlinkedAssignmentVersion(PROJECT_NAME2);

      //check the deletion of the unlinked assignment version objects
      OpBroker broker = session.newBroker();
      assertNotNull(broker.getObject(OpAssignmentVersion.class, assignmentVersion1ID));
      assertNotNull(broker.getObject(OpAssignmentVersion.class, assignmentVersion2ID));
      assertNotNull(broker.getObject(OpAssignmentVersion.class, assignmentVersion3ID));
      broker.close();
      projectChecker.check(session);
      broker = session.newBroker();
      assertNull(broker.getObject(OpAssignmentVersion.class, assignmentVersion1ID));
      assertNull(broker.getObject(OpAssignmentVersion.class, assignmentVersion2ID));
      assertNull(broker.getObject(OpAssignmentVersion.class, assignmentVersion3ID));
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

      deleteAllObjects(broker, OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(broker, OpAssignmentVersion.ASSIGNMENT_VERSION);
      deleteAllObjects(broker, OpProjectPlanVersion.PROJECT_PLAN_VERSION);
      deleteAllObjects(broker, OpActivityVersion.ACTIVITY_VERSION);

      List<OpProjectNode> projectList = dataFactory.getAllProjects(broker);
      for (OpProjectNode project : projectList) {
         broker.deleteObject(project);
      }

      List<OpProjectNode> portofolioList = dataFactory.getAllPortofolios(broker);
      for (OpProjectNode portofolio : portofolioList) {
         if (portofolio.getName().equals(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)) {
            continue;
         }
         broker.deleteObject(portofolio);
      }

      transaction.commit();
      broker.close();
   }

   /**
    * Counts all objects in the database.
    *
    * @return - the number of objects in the database.
    */
   private int countAllDBObjects() {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery(COUNT_ALL_OBJECTS_QUERY);
      Number result = 0;
      for (Object o : broker.list(query)) {
         result = (Number) o;
      }
      broker.close();

      return result.intValue();
   }

   /**
    * Inserts one project and two activity versions belonging to it.
    *
    * @param projectName               - the name of the project.
    * @param firstActivityVersionName  - the name of an activity version belonging to the project.
    * @param secondActivityVersionName - the name of an activity version belonging to the project.
    */
   private void insertProjectWithTwoActivityVersions(String projectName, String firstActivityVersionName,
        String secondActivityVersionName) {
      //insert the project
      Date startDate = new Date(getCalendarWithExactDaySet(2007, 6, 5).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(projectName, startDate, 100d, null, null);
      XMessage response = projectService.insertProject(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project = dataFactory.getProjectByName(broker, projectName);

         OpTransaction t = broker.newTransaction();

         //insert the two activity versions
         OpActivityVersion activityVersion1 = new OpActivityVersion();
         activityVersion1.setName(firstActivityVersionName);
         activityVersion1.setType(OpActivityVersion.TASK);
         activityVersion1.setBaseEffort(10);
         activityVersion1.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 5).getTimeInMillis()));
         activityVersion1.setFinish(new Date(getCalendarWithExactDaySet(2007, 6, 30).getTimeInMillis()));

         OpActivityVersion activityVersion2 = new OpActivityVersion();
         activityVersion2.setName(secondActivityVersionName);
         activityVersion2.setType(OpActivityVersion.STANDARD);
         activityVersion2.setBaseEffort(15);
         activityVersion2.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 5).getTimeInMillis()));
         activityVersion2.setFinish(new Date(getCalendarWithExactDaySet(2007, 6, 15).getTimeInMillis()));

         OpProjectPlanVersion planVersion = new OpProjectPlanVersion();
         planVersion.setVersionNumber(OpProjectPlan.WORKING_VERSION_NUMBER);
         planVersion.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 5).getTimeInMillis()));
         planVersion.setFinish(new Date(getCalendarWithExactDaySet(2007, 6, 30).getTimeInMillis()));
         planVersion.setProjectPlan(project.getPlan());
         activityVersion1.setPlanVersion(planVersion);
         activityVersion2.setPlanVersion(planVersion);
         broker.makePersistent(activityVersion1);
         broker.makePersistent(activityVersion2);
         broker.makePersistent(planVersion);

         t.commit();
      }
      finally {
         broker.close();
      }
   }

   /**
    * Inserts two <code>OpAssignmentVersion</code> objects which are linked to two <code>OpActivityVersion</code>
    * objects belonging to the same project. (It is supposed that the activities and the project already exist).
    *
    * @param projectName               - the name of the project to which the <code>OpAssignmentVersion</code> objects are belonging.
    * @param firstActivityVersionName  - the name of an activity version to which an <code>OpAssignmentVersion</code> object
    *                                  is linked.
    * @param secondActivityVersionName - the name of an activity version to which an <code>OpAssignmentVersion</code> object
    *                                  is linked.
    */
   private void insertLinkedAssignmentVersions(String projectName, String firstActivityVersionName,
        String secondActivityVersionName) {

      String idActivity1 = activityFactory.getActivityVersionId(firstActivityVersionName);
      OpActivityVersion activityVersion1 = activityFactory.getActivityVersionById(idActivity1);

      String idActivity2 = activityFactory.getActivityVersionId(secondActivityVersionName);
      OpActivityVersion activityVersion2 = activityFactory.getActivityVersionById(idActivity2);

      OpProjectPlanVersion planVersion = new OpProjectPlanVersion();
      String projectLocator = dataFactory.getProjectId(projectName);

      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project = dataFactory.getProjectById(broker, projectLocator);
         for (OpProjectPlanVersion projectPlanVersion : project.getPlan().getVersions()) {
            planVersion = projectPlanVersion;
         }

//       OpBroker broker = session.newBroker();
         OpTransaction t = broker.newTransaction();

         OpAssignmentVersion assignmentVersion1 = new OpAssignmentVersion();
         assignmentVersion1.setActivityVersion(activityVersion1);
         assignmentVersion1.setPlanVersion(planVersion);

         OpAssignmentVersion assignmentVersion2 = new OpAssignmentVersion();
         assignmentVersion2.setActivityVersion(activityVersion2);
         assignmentVersion2.setPlanVersion(planVersion);

         broker.makePersistent(assignmentVersion1);
         broker.makePersistent(assignmentVersion2);

         t.commit();
      }
      finally {
         broker.close();
      }
   }

   /**
    * Inserts an <code>OpAssignmentVersion</code> object which has no linke to an <code>OpActivityVersion</code>
    * objectand returns it's ID. (It is supposed that the <code>OpProjectPlanVersion</code> to which the assignment version is
    * attached alkready exists).
    *
    * @param projectName - the name of the project to which the <code>OpAssignmentVersion</code> will be linked.
    * @return - the ID of the newly inserted <code>OpAssignmentVersion</code> object.
    */
   private long insertUnlinkedAssignmentVersion(String projectName) {
      OpProjectPlanVersion planVersion = getSinglePlanVersionOfProject(projectName);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpAssignmentVersion assignmentVersion1 = new OpAssignmentVersion();
      assignmentVersion1.setPlanVersion(planVersion);
      broker.makePersistent(assignmentVersion1);

      t.commit();
      broker.close();
      return assignmentVersion1.getID();
   }

   /**
    * Returns the <code>OpProjectPlanVersion</code> belonging to a <code>OpProjectNode</code> specified by a name.
    * It is supposed that the project node has only one project plan version.
    *
    * @param projectName - the name of the <code>OpProjectNode</code> for which the plan version is returned.
    * @return the <code>OpProjectPlanVersion</code> belonging to a <code>OpProjectNode</code> specified by a name.
    */
   private OpProjectPlanVersion getSinglePlanVersionOfProject(String projectName) {
      OpProjectPlanVersion planVersion = new OpProjectPlanVersion();
      String projectLocator = dataFactory.getProjectId(projectName);
      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project = dataFactory.getProjectById(broker, projectLocator);
         for (OpProjectPlanVersion projectPlanVersion : project.getPlan().getVersions()) {
            planVersion = projectPlanVersion;
         }
      }
      finally {
         broker.close();
      }

      return planVersion;
   }
}
