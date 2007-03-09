/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.express.XComponent;
import onepoint.persistence.*;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.ProjectTestDataFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.project_planning.test.ProjectPlanningTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.ResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.UserTestDataFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkService;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.modules.work.OpWorkSlipDataSetFactory;
import onepoint.project.test.OpBaseTestCase;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class test project service methods.
 *
 * @author lucian.furtos
 */
public class OpWorkServiceTest extends OpBaseTestCase {

   private OpWorkService service;
   private WorkTestDataFactory dataFactory;
   private OpProjectPlanningService planningService;
   private ProjectPlanningTestDataFactory planningDataFactory;
   private ProjectTestDataFactory projectDataFactory;
   private ResourceTestDataFactory resourceDataFactory;

   private String resId;
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

      service = getWorkService();
      dataFactory = new WorkTestDataFactory(session);
      planningService = getProjectPlanningService();
      planningDataFactory = new ProjectPlanningTestDataFactory(session);
      projectDataFactory = new ProjectTestDataFactory(session);
      resourceDataFactory = new ResourceTestDataFactory(session);

      clean();

      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = resourceDataFactory.createResourceMsg("resource", "description", 50d, 2d, false, poolid);
      XMessage response = getResourceService().insertResource(session, request);
      assertNoError(response);
      resId = resourceDataFactory.getResourceByName("resource").locator();

      request = ProjectTestDataFactory.createProjectMsg("project", new Date(1), 1d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);
      projId = projectDataFactory.getProjectId("project");

      planId = projectDataFactory.getProjectById(projId).getPlan().locator();
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


   public void testInsertWorkSlip()
        throws Exception {
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpProjectPlan plan = (OpProjectPlan) broker.getObject(planId);
      OpResource resource = (OpResource) broker.getObject(resId);

      OpActivity activity = new OpActivity();
      activity.setType(OpActivity.SCHEDULED_TASK);
      broker.makePersistent(activity);

      OpAssignment assignment = new OpAssignment();
      assignment.setProjectPlan(plan);
      assignment.setResource(resource);
      assignment.setActivity(activity);
      broker.makePersistent(assignment);

      t.commit();
      OpQuery query = broker.newQuery("from OpAssignment");
      assignment = (OpAssignment) broker.list(query).get(0);
      broker.close();

      XMessage request = WorkTestDataFactory.insertWSMsg(assignment.locator(), "", new Date(System.currentTimeMillis()),
           false, true, OpActivity.TASK, 32.43d, 321d, 545.432d, 654.32d, 423.31d, 543.32d);
      XMessage response = service.insertWorkSlip(session, request);
      assertNoError(response);

      //todo: check work slips and records
   }

   public void testCreateWSRow()
        throws Exception {
      OpActivity sa = new OpActivity();
      sa.setName("Super");

      OpProjectNode project = new OpProjectNode();
      project.setName("Project One");
      OpProjectPlan plan = new OpProjectPlan();
      plan.setProjectNode(project);

      OpActivity activity = new OpActivity();
      activity.setProjectPlan(plan);
      activity.setSuperActivity(sa);

      OpResource resource = new OpResource();
      resource.setID(1);

      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resource);
      assignment.setProjectPlan(plan);
      assignment.setBaseEffort(0.2);

      boolean prgtrk = false;
      HashMap resources = new HashMap();
      resources.put(new Long(1), "Resource One");
      XComponent row = OpWorkSlipDataSetFactory.createWorkSlipDataRow(activity, assignment, prgtrk, resources);
   }

   public void testGetAssignments()
        throws Exception {
      long time = System.currentTimeMillis();
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpProjectPlan plan = (OpProjectPlan) broker.getObject(planId);
      OpResource resource = (OpResource) broker.getObject(resId);

      OpActivity activity = new OpActivity();
      activity.setType(OpActivity.SCHEDULED_TASK);
      activity.setStart(new Date(1));
      broker.makePersistent(activity);

      OpAssignment assignment = new OpAssignment();
      assignment.setProjectPlan(plan);
      assignment.setResource(resource);
      assignment.setActivity(activity);
      assignment.setComplete(60d);
      broker.makePersistent(assignment);

      t.commit();

      List resources = new ArrayList();
      resources.add(new Long(resourceDataFactory.getResourceById(resId).getID()));
      List types = new ArrayList();
      types.add(new Byte(OpActivity.SCHEDULED_TASK));
      types.add(new Byte(OpActivity.COLLECTION_TASK));
      Date date = new Date(time);
      long id = projectDataFactory.getProjectById(projId).getID();
      Iterator it = OpWorkSlipDataSetFactory.getAssignments(broker, resources, types, date, id);
      assertTrue(it.hasNext());
      broker.close();
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
