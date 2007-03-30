/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project_resources.test;

import onepoint.express.XComponent;
import onepoint.persistence.*;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.ProjectTestDataFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.project_planning.test.ProjectPlanningTestDataFactory;
import onepoint.project.modules.project_resources.OpProjectResourceDataSetFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.ResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.UserTestDataFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.test.OpBaseTestCase;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.*;

/**
 * This class test project cost methods.
 *
 * @author lucian.furtos
 */
public class OpProjectResourceTest extends OpBaseTestCase {

   private OpProjectPlanningService planningService;
   private ProjectPlanningTestDataFactory planningDataFactory;
   private ProjectTestDataFactory projectDataFactory;
   private ResourceTestDataFactory resourceDataFactory;

   private String res1Id;
   private String res2Id;
   private String res3Id;
   private String projId;
   private String planId;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      planningService = getProjectPlanningService();
      planningDataFactory = new ProjectPlanningTestDataFactory(session);
      projectDataFactory = new ProjectTestDataFactory(session);
      resourceDataFactory = new ResourceTestDataFactory(session);

      clean();

      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = resourceDataFactory.createResourceMsg("resource1", "description", 50d, 1d, false, poolid);
      XMessage response = getResourceService().insertResource(session, request);
      assertNoError(response);
      res1Id = resourceDataFactory.getResourceByName("resource1").locator();
      request = resourceDataFactory.createResourceMsg("resource2", "description", 70d, 7d, false, poolid);
      response = getResourceService().insertResource(session, request);
      assertNoError(response);
      res2Id = resourceDataFactory.getResourceByName("resource2").locator();
      request = resourceDataFactory.createResourceMsg("resource3", "description", 20d, 24d, false, poolid);
      response = getResourceService().insertResource(session, request);
      assertNoError(response);
      res3Id = resourceDataFactory.getResourceByName("resource3").locator();

      request = ProjectTestDataFactory.createProjectMsg("project", new Date(1), 1d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);
      projId = projectDataFactory.getProjectId("project");

      planId = projectDataFactory.getProjectById(projId).getPlan().locator();
   }

   /**
    * Test the <code>fillEffortDataSet</code> from <code>OpProjectResourceDataSetFactory</code>.
    * It will create two <code>OpActivity</code> that match the query. The result data set must contains one entry for
    * each activity and five more for each resource.
    *
    * @throws Exception if the test fail.
    */
   public void testFillEffort()
        throws Exception {
      long time = System.currentTimeMillis();
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpProjectPlan plan = (OpProjectPlan) broker.getObject(planId);
      OpProjectNode project = (OpProjectNode) broker.getObject(projId);
      OpResource resource1 = (OpResource) broker.getObject(res1Id);
      OpResource resource2 = (OpResource) broker.getObject(res2Id);
      OpResource resource3 = (OpResource) broker.getObject(res3Id);

      OpActivity activity = new OpActivity();
      activity.setType(OpActivity.SCHEDULED_TASK);
      activity.setStart(new Date(time));
      activity.setProjectPlan(plan);
      activity.setSubActivities(new HashSet());
      activity.setOutlineLevel((byte) 10);
      activity.setComplete(60d);
      activity.setActualEffort(34242);
      activity.setActualExternalCosts(34);
      activity.setActualExternalCosts(324);
      activity.setActualMaterialCosts(4324);
      activity.setActualMiscellaneousCosts(54);
      activity.setActualPersonnelCosts(254);
      activity.setActualTravelCosts(2554);
      activity.setBaseEffort(3242);
      activity.setBaseExternalCosts(432);
      activity.setBaseMiscellaneousCosts(9432);
      activity.setBaseMaterialCosts(4232);
      activity.setBasePersonnelCosts(45632);
      activity.setBaseTravelCosts(4432);
      broker.makePersistent(activity);

      Set assignments = new HashSet();

      OpAssignment assignment = new OpAssignment();
      assignment.setProjectPlan(plan);
      assignment.setResource(resource1);
      assignment.setActivity(activity);
      assignment.setComplete(60d);
      broker.makePersistent(assignment);
      assignments.add(assignment);

      assignment = new OpAssignment();
      assignment.setProjectPlan(plan);
      assignment.setResource(resource2);
      assignment.setActivity(activity);
      assignment.setComplete(20d);
      broker.makePersistent(assignment);
      assignments.add(assignment);

      activity.setAssignments(assignments);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.MILESTONE);
      activity.setStart(new Date(time + 1000));
      activity.setProjectPlan(plan);
      activity.setOutlineLevel((byte) 1);
      broker.makePersistent(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.ADHOC_TASK);
      activity.setStart(new Date(1));
      activity.setProjectPlan(plan);
      activity.setOutlineLevel((byte) 5);
      activity.setSubActivities(new HashSet());
      activity.setComplete(20d);
      activity.setActualEffort(34242);
      activity.setActualExternalCosts(34);
      activity.setActualExternalCosts(324);
      activity.setActualMaterialCosts(4324);
      activity.setActualMiscellaneousCosts(54);
      activity.setActualPersonnelCosts(254);
      activity.setActualTravelCosts(2554);
      activity.setBaseEffort(3242);
      activity.setBaseExternalCosts(432);
      activity.setBaseMiscellaneousCosts(9432);
      activity.setBaseMaterialCosts(4232);
      activity.setBasePersonnelCosts(45632);
      activity.setBaseTravelCosts(4432);
      broker.makePersistent(activity);

      assignments = new HashSet();

      assignment = new OpAssignment();
      assignment.setProjectPlan(plan);
      assignment.setResource(resource1);
      assignment.setActivity(activity);
      assignment.setComplete(60d);
      broker.makePersistent(assignment);
      assignments.add(assignment);

      assignment = new OpAssignment();
      assignment.setProjectPlan(plan);
      assignment.setResource(resource2);
      assignment.setActivity(activity);
      assignment.setComplete(23d);
      broker.makePersistent(assignment);
      assignments.add(assignment);

      assignment = new OpAssignment();
      assignment.setProjectPlan(plan);
      assignment.setResource(resource3);
      assignment.setActivity(activity);
      assignment.setComplete(89d);
      broker.makePersistent(assignment);
      assignments.add(assignment);

      activity.setAssignments(assignments);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.COLLECTION_TASK);
      activity.setStart(new Date(1000));
      activity.setProjectPlan(plan);
      activity.setOutlineLevel((byte) 11);
      activity.setAssignments(new HashSet());
      broker.makePersistent(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.COLLECTION_TASK);
      activity.setStart(new Date(1000));
      activity.setOutlineLevel((byte) 10);
      broker.makePersistent(activity);

      t.commit();

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpProjectResourceDataSetFactory.fillEffortDataSet(broker, project, 10, dataSet);

      assertEquals(1 + 2 + 1 + 3, dataSet.getChildCount());
      // todo: test other infos
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

      deleteAllObjects(OpWorkRecord.WORK_RECORD);
      deleteAllObjects(OpWorkSlip.WORK_SLIP);
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
         projectDataFactory.deleteObject(project);
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
         projectDataFactory.deleteObject(object);
      }
   }
}
