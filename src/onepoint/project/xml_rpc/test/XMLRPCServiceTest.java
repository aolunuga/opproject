/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.xml_rpc.test;

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
import onepoint.project.modules.work.*;
import onepoint.project.test.OpBaseTestCase;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class test project service methods.
 *
 * @author dieter.freismuth
 */
public class XMLRPCServiceTest extends OpBaseTestCase {

   private OpWorkService service;
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
      planningService = getProjectPlanningService();
      planningDataFactory = new ProjectPlanningTestDataFactory(session);
      projectDataFactory = new ProjectTestDataFactory(session);
      resourceDataFactory = new ResourceTestDataFactory(session);

      clean();

//      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
//      XMessage request = resourceDataFactory.createResourceMsg("resource", "description", 50d, 2d, 1d, false, poolid);
//      XMessage response = getResourceService().insertResource(session, request);
//      assertNoError(response);
//      resId = resourceDataFactory.getResourceByName("resource").locator();
//
//      request = ProjectTestDataFactory.createProjectMsg("project", new Date(1), 1d, null, null);
//      response = getProjectService().insertProject(session, request);
//      assertNoError(response);
//      projId = projectDataFactory.getProjectId("project");
//
//      planId = projectDataFactory.getProjectById(projId).getPlan().locator();
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


   public void testInsertWorkSlip() throws Exception {
//      OpBroker broker = session.newBroker();
//      OpTransaction t = broker.newTransaction();
//
//      OpProjectPlan plan = (OpProjectPlan) broker.getObject(planId);
//      OpResource resource = (OpResource) broker.getObject(resId);
//
//      OpActivity activity = new OpActivity(OpActivity.SCHEDULED_TASK);
//      broker.makePersistent(activity);
//
//      OpAssignment assignment = new OpAssignment();
//      assignment.setProjectPlan(plan);
//      assignment.setResource(resource);
//      assignment.setActivity(activity);
//      broker.makePersistent(assignment);
//
//      t.commit();
//      OpQuery query = broker.newQuery("from OpAssignment");
//      assignment = (OpAssignment) broker.list(query).get(0);
//      broker.close();
//
//      //todo: check work slips and records
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
   }
}
