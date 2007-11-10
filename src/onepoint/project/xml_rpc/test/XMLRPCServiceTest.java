/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.xml_rpc.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.project_planning.test.OpProjectPlanningTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkService;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;

import java.util.Iterator;
import java.util.List;

/**
 * This class test project service methods.
 *
 * @author dieter.freismuth
 */
public class XMLRPCServiceTest extends OpBaseOpenTestCase {

   private OpWorkService service;
   private OpProjectPlanningService planningService;
   private OpProjectPlanningTestDataFactory planningDataFactory;
   private OpProjectTestDataFactory projectDataFactory;
   private OpResourceTestDataFactory resourceDataFactory;

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

      service = OpTestDataFactory.getWorkService();
      planningService = OpTestDataFactory.getProjectPlanningService();
      planningDataFactory = new OpProjectPlanningTestDataFactory(session);
      projectDataFactory = new OpProjectTestDataFactory(session);
      resourceDataFactory = new OpResourceTestDataFactory(session);

//      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
//      XMessage request = resourceDataFactory.createResourceMsg("resource", "description", 50d, 2d, 1d, false, poolid);
//      XMessage response = getResourceService().insertResource(session, request);
//      assertNoError(response);
//      resId = resourceDataFactory.getResourceByName("resource").locator();
//
//      request = OpProjectTestDataFactory.createProjectMsg("project", new Date(1), 1d, null, null);
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
      OpUserTestDataFactory usrData = new OpUserTestDataFactory(session);

      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();

      deleteAllObjects(broker, OpWorkRecord.WORK_RECORD);
      deleteAllObjects(broker, OpWorkSlip.WORK_SLIP);
      deleteAllObjects(broker, OpAssignment.ASSIGNMENT);
      deleteAllObjects(broker, OpActivityComment.ACTIVITY_COMMENT);
      deleteAllObjects(broker, OpActivity.ACTIVITY);
      deleteAllObjects(broker, OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(broker, OpAssignmentVersion.ASSIGNMENT_VERSION);
      deleteAllObjects(broker, OpProjectPlanVersion.PROJECT_PLAN_VERSION);
      deleteAllObjects(broker, OpActivityVersion.ACTIVITY_VERSION);

 
      List resoucesList = resourceDataFactory.getAllResources(broker);
      for (Iterator iterator = resoucesList.iterator(); iterator.hasNext();) {
         OpResource resource = (OpResource) iterator.next();
         broker.deleteObject(resource);
      }

      List poolList = resourceDataFactory.getAllResourcePools(broker);
      for (Iterator iterator = poolList.iterator(); iterator.hasNext();) {
         OpResourcePool pool = (OpResourcePool) iterator.next();
         if (pool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            continue;
         }
         broker.deleteObject(pool);
      }

      for (Object user1 : usrData.getAllUsers(broker)) {
         OpUser user = (OpUser) user1;
         if (user.getName().equals(OpUser.ADMINISTRATOR_NAME)) {
            continue;
         }
         broker.deleteObject(user);
      }
      
      transaction.commit();
      broker.close();
   }
}
