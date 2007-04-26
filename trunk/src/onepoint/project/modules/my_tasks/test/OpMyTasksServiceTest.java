/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.my_tasks.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.my_tasks.OpMyTasksError;
import onepoint.project.modules.my_tasks.OpMyTasksService;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.ProjectTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.ResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.UserTestDataFactory;
import onepoint.project.test.OpBaseTestCase;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class test project service methods.
 *
 * @author lucian.furtos
 */
public class OpMyTasksServiceTest extends OpBaseTestCase {

   private static final String RESOURCE_NAME = "resource";
   private static final String PROJECT_NAME = "project";
   private static final String ACTIVITY_NAME = "activity";
   private static final String ATTACH_NAME = "Attachment";

   private OpMyTasksService service;
   private MyTasksTestDataFactory dataFactory;
   private ProjectTestDataFactory projectDataFactory;
   private ResourceTestDataFactory resourceDataFactory;

   private String resId;
   private String res2Id;
   private String projId;
   private String proj2Id;
   private String planId;
   private String plan2Id;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      service = getMyTasksService();
      dataFactory = new MyTasksTestDataFactory(session);
      projectDataFactory = new ProjectTestDataFactory(session);
      resourceDataFactory = new ResourceTestDataFactory(session);

      clean();

      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = resourceDataFactory.createResourceMsg(RESOURCE_NAME, "description", 50d, 2d, 1d, false, poolid);
      XMessage response = getResourceService().insertResource(session, request);
      assertNoError(response);
      resId = resourceDataFactory.getResourceByName(RESOURCE_NAME).locator();
      request = resourceDataFactory.createResourceMsg(RESOURCE_NAME + 2, "description", 10d, 9d, 1d, false, poolid);
      response = getResourceService().insertResource(session, request);
      assertNoError(response);
      res2Id = resourceDataFactory.getResourceByName(RESOURCE_NAME + 2).locator();

      request = ProjectTestDataFactory.createProjectMsg(PROJECT_NAME, new Date(1), 1d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);
      projId = projectDataFactory.getProjectId(PROJECT_NAME);
      request = ProjectTestDataFactory.createProjectMsg(PROJECT_NAME + 2, new Date(1000), 6d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);
      proj2Id = projectDataFactory.getProjectId(PROJECT_NAME + 2);

      planId = projectDataFactory.getProjectById(projId).getPlan().locator();
      plan2Id = projectDataFactory.getProjectById(proj2Id).getPlan().locator();
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


   public void testAddAdhocTasks()
        throws Exception {
      assertTrue(dataFactory.getAllActivities().isEmpty());

      Date duedate = Date.valueOf("2007-05-24");
      String prjChoice = XValidator.choice(projId, PROJECT_NAME);
      String resChoice = XValidator.choice(resId, RESOURCE_NAME);
      XMessage request = MyTasksTestDataFactory.addAdhocMsg(ACTIVITY_NAME, "descr", 5, duedate, prjChoice, resChoice);
      XMessage response = service.addAdhocTask(session, request);
      assertNoError(response);

      OpActivity activity = dataFactory.getActivityByName(ACTIVITY_NAME);
      assertNotNull(activity);
      assertEquals(5, activity.getPriority());
      assertEquals(OpActivity.ADHOC_TASK, activity.getType());
      assertEquals(1, activity.getAssignments().size());
      assertEquals(duedate, activity.getFinish());
      assertEquals(planId, activity.getProjectPlan().locator());
   }

   public void testErrorAddAdhocTasks()
        throws Exception {
      assertTrue(dataFactory.getAllActivities().isEmpty());

      String prjChoice = XValidator.choice(projId, PROJECT_NAME);
      String resChoice = XValidator.choice(resId, RESOURCE_NAME);
      XMessage request = MyTasksTestDataFactory.addAdhocMsg(null, null, 1, null, null, null);
      XMessage response = service.addAdhocTask(session, request);
      assertError(response, OpMyTasksError.EMPTY_NAME_ERROR_CODE);

      request = MyTasksTestDataFactory.addAdhocMsg(ACTIVITY_NAME, null, 1, null, null, null);
      response = service.addAdhocTask(session, request);
      assertError(response, OpMyTasksError.NO_PROJECT_ERROR_CODE);

      request = MyTasksTestDataFactory.addAdhocMsg(ACTIVITY_NAME, null, 1, null, prjChoice, null);
      response = service.addAdhocTask(session, request);
      assertError(response, OpMyTasksError.NO_RESOURCE_ERROR_CODE);

      request = MyTasksTestDataFactory.addAdhocMsg(ACTIVITY_NAME, null, 0, null, prjChoice, resChoice);
      response = service.addAdhocTask(session, request);
      assertError(response, OpMyTasksError.INVALID_PRIORITY_ERROR_CODE);

      request = MyTasksTestDataFactory.addAdhocMsg(ACTIVITY_NAME, null, 10, null, prjChoice, resChoice);
      response = service.addAdhocTask(session, request);
      assertError(response, OpMyTasksError.INVALID_PRIORITY_ERROR_CODE);
   }

   public void testUpdateAdhocTasks()
        throws Exception {
      assertTrue(dataFactory.getAllActivities().isEmpty());

      Date duedate = Date.valueOf("2007-05-24");
      String prjChoice = XValidator.choice(projId, PROJECT_NAME);
      String resChoice = XValidator.choice(resId, RESOURCE_NAME);
      XMessage request = MyTasksTestDataFactory.addAdhocMsg(ACTIVITY_NAME, "descr", 5, duedate, prjChoice, resChoice);
      XMessage response = service.addAdhocTask(session, request);
      assertNoError(response);

      assertEquals(1, dataFactory.getAllActivities().size());
      String locator = dataFactory.getActivityId(ACTIVITY_NAME);
      duedate = Date.valueOf("2007-06-15");
      prjChoice = XValidator.choice(proj2Id, PROJECT_NAME + 2);
      resChoice = XValidator.choice(res2Id, RESOURCE_NAME + 2);
      request = MyTasksTestDataFactory.updateAdhocMsg(locator, "New" + ACTIVITY_NAME, "new descr", 9, duedate, prjChoice, resChoice);
      response = service.updateAdhocTask(session, request);
      assertNoError(response);

      OpActivity activity = dataFactory.getActivityById(locator);
      assertNotNull(activity);
      assertEquals(9, activity.getPriority());
      assertEquals("New" + ACTIVITY_NAME, activity.getName());
      assertEquals(duedate, activity.getFinish());
      assertEquals(plan2Id, activity.getProjectPlan().locator());
   }

   public void testAdhocTasksAttachments()
        throws Exception {
      assertTrue(dataFactory.getAllActivities().isEmpty());
      assertTrue(dataFactory.getAllAttachments().isEmpty());

      Date duedate = Date.valueOf("2007-05-24");
      String prjChoice = XValidator.choice(projId, PROJECT_NAME);
      String resChoice = XValidator.choice(resId, RESOURCE_NAME);
      List choices = new ArrayList();
      choices.add(XValidator.choice(OpActivityDataSetFactory.NO_CONTENT_ID, ATTACH_NAME + 0));
      choices.add(XValidator.choice(OpActivityDataSetFactory.NO_CONTENT_ID, ATTACH_NAME + 1));
      choices.add(XValidator.choice(OpActivityDataSetFactory.NO_CONTENT_ID, ATTACH_NAME + 2));
      XComponent attachSet = MyTasksTestDataFactory.createDataSet(choices);
      XMessage request = MyTasksTestDataFactory.addAdhocMsg(ACTIVITY_NAME, "descr", 5, duedate, prjChoice, resChoice, attachSet);
      XMessage response = service.addAdhocTask(session, request);
      assertNoError(response);

      assertEquals(1, dataFactory.getAllActivities().size());
      List attachs = dataFactory.getAllAttachments();
      assertEquals(3, attachs.size());
      for (int i = 0; i < 3; i++) {
         OpAttachment attachment = (OpAttachment) attachs.get(i);
         assertEquals(ATTACH_NAME + i, attachment.getName());
      }

      choices.remove(0);
      String locator = dataFactory.getActivityId(ACTIVITY_NAME);
      duedate = Date.valueOf("2007-06-15");
      prjChoice = XValidator.choice(proj2Id, PROJECT_NAME + 2);
      resChoice = XValidator.choice(res2Id, RESOURCE_NAME + 2);
      attachSet = MyTasksTestDataFactory.createDataSet(choices);
      request = MyTasksTestDataFactory.updateAdhocMsg(locator, "New" + ACTIVITY_NAME, "new descr", 9, duedate, prjChoice, resChoice, attachSet);
      response = service.updateAdhocTask(session, request);
      assertNoError(response);

      OpActivity activity = dataFactory.getActivityById(locator);
      assertNotNull(activity);
      assertEquals(9, activity.getPriority());
      assertEquals("New" + ACTIVITY_NAME, activity.getName());
      assertEquals(duedate, activity.getFinish());
      assertEquals(plan2Id, activity.getProjectPlan().locator());
      attachs = dataFactory.getAllAttachments();
      assertEquals(2, attachs.size());
   }

   public void testDeleteAdhocTasks()
        throws Exception {
      assertTrue(dataFactory.getAllActivities().isEmpty());

      String prjChoice = XValidator.choice(projId, PROJECT_NAME);
      String resChoice = XValidator.choice(resId, RESOURCE_NAME);
      List ids = new ArrayList();

      for (int i = 0; i < 3; i++) {
         XMessage request = MyTasksTestDataFactory.addAdhocMsg(ACTIVITY_NAME + i, "descr " + i, i + 1, null, prjChoice, resChoice);
         XMessage response = service.addAdhocTask(session, request);
         assertNoError(response);
         ids.add(dataFactory.getActivityId(ACTIVITY_NAME + i));
      }
      assertEquals(3, dataFactory.getAllActivities().size());

      ids.remove(1);

      XMessage request = MyTasksTestDataFactory.deleteAdhocMsg(ids);
      XMessage response = service.deleteAdhocTask(session, request);
      assertNoError(response);

      assertEquals(1, dataFactory.getAllActivities().size());
      OpActivity activity = dataFactory.getActivityByName(ACTIVITY_NAME + 1);
      assertNotNull(activity);
   }

   // ******** Helper Methods *********

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {
      UserTestDataFactory usrData = new UserTestDataFactory(session);
      ArrayList ids = new ArrayList();
      List users = usrData.getAllUsers();
      for (Iterator iterator = users.iterator(); iterator.hasNext();) {
         OpUser user = (OpUser) iterator.next();
         if (user.getName().equals(OpUser.ADMINISTRATOR_NAME)) {
            continue;
         }
         ids.add(user.locator());
      }
      XMessage request = new XMessage();
      request.setArgument(OpUserService.SUBJECT_IDS, ids);
      getUserService().deleteSubjects(session, request);

      deleteAllObjects(OpAssignment.ASSIGNMENT);
      deleteAllObjects(OpActivityComment.ACTIVITY_COMMENT);
      deleteAllObjects(OpActivity.ACTIVITY);
      deleteAllObjects(OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(OpAssignmentVersion.ASSIGNMENT_VERSION);
      deleteAllObjects(OpProjectPlanVersion.PROJECT_PLAN_VERSION);
      deleteAllObjects(OpActivityVersion.ACTIVITY_VERSION);

      List projectList = projectDataFactory.getAllProjects();
      for (Iterator iterator = projectList.iterator(); iterator.hasNext();) {
         OpProjectNode project = (OpProjectNode) iterator.next();
         dataFactory.deleteObject(project);
      }

      List resoucesList = resourceDataFactory.getAllResources();
      for (Iterator iterator = resoucesList.iterator(); iterator.hasNext();) {
         OpResource resource = (OpResource) iterator.next();
         resourceDataFactory.deleteObject(resource);
      }

      List poolList = resourceDataFactory.getAllResourcePools();
      for (Iterator iterator = poolList.iterator(); iterator.hasNext();) {
         OpResourcePool pool = (OpResourcePool) iterator.next();
         if (pool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            continue;
         }
         resourceDataFactory.deleteObject(pool);
      }
   }

   private void deleteAllObjects(String prototypeName) {
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery("from " + prototypeName);
      Iterator it = broker.list(query).iterator();
      broker.close();
      while (it.hasNext()) {
         OpObject object = (OpObject) it.next();
         dataFactory.deleteObject(object);
      }
   }
}
