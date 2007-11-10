/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project_costs.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.project_costs.OpProjectCostsDataSetFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.project_planning.test.OpProjectPlanningTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * This class test project cost methods.
 *
 * @author lucian.furtos
 */
public class OpProjectCostTest extends OpBaseOpenTestCase {

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

      planningService = OpTestDataFactory.getProjectPlanningService();
      planningDataFactory = new OpProjectPlanningTestDataFactory(session);
      projectDataFactory = new OpProjectTestDataFactory(session);
      resourceDataFactory = new OpResourceTestDataFactory(session);

      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = resourceDataFactory.createResourceMsg("resource", "description", 50d, 2d, 2d, false, poolid);
      XMessage response = OpTestDataFactory.getResourceService().insertResource(session, request);
      assertNoError(response);
      resId = resourceDataFactory.getResourceByName("resource").locator();

      request = OpProjectTestDataFactory.createProjectMsg("project", new Date(1), 1d, null, null);
      response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);
      projId = projectDataFactory.getProjectId("project");

      planId = projectDataFactory.getProjectById(projId).getPlan().locator();
   }

   /**
    * Test the functionality of OpProjectCostsDataSetFactory.fillCostsDataSet
    *
    * @throws Exception
    */
   public void testFillCosts()
        throws Exception {
      long time = System.currentTimeMillis();
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpProjectPlan plan = (OpProjectPlan) broker.getObject(planId);
      OpProjectNode project = (OpProjectNode) broker.getObject(projId);

      OpActivity activity = new OpActivity();
      activity.setType(OpActivity.SCHEDULED_TASK);
      activity.setStart(new Date(time));
      activity.setProjectPlan(plan);
      activity.setSubActivities(new HashSet());
      activity.setOutlineLevel((byte) 10);
      activity.setComplete(60d);
      activity.setActualEffort(1);
      activity.setActualExternalCosts(2);
      activity.setActualMaterialCosts(3);
      activity.setActualMiscellaneousCosts(4);
      activity.setActualPersonnelCosts(5);
      activity.setActualTravelCosts(6);
      activity.setActualProceeds(7d);
      activity.setBaseEffort(8);
      activity.setBaseExternalCosts(9);
      activity.setBaseMiscellaneousCosts(10);
      activity.setBaseMaterialCosts(11);
      activity.setBasePersonnelCosts(12);
      activity.setBaseTravelCosts(13);
      activity.setBaseProceeds(14d);
      activity.setAssignments(new HashSet<OpAssignment>());
      broker.makePersistent(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.MILESTONE);
      activity.setStart(new Date(time + 1000));
      activity.setProjectPlan(plan);
      activity.setOutlineLevel((byte) 1);
      activity.setAssignments(new HashSet<OpAssignment>());
      broker.makePersistent(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.ADHOC_TASK);
      activity.setStart(new Date(time + 2000));
      activity.setProjectPlan(plan);
      activity.setOutlineLevel((byte) 5);
      activity.setSubActivities(new HashSet());
      activity.setComplete(20d);
      activity.setActualEffort(1);
      activity.setActualExternalCosts(2);
      activity.setActualMaterialCosts(3);
      activity.setActualMiscellaneousCosts(4);
      activity.setActualPersonnelCosts(5);
      activity.setActualTravelCosts(6);
      activity.setActualProceeds(7d);
      activity.setBaseEffort(8);
      activity.setBaseExternalCosts(9);
      activity.setBaseMiscellaneousCosts(10);
      activity.setBaseMaterialCosts(11);
      activity.setBasePersonnelCosts(12);
      activity.setBaseTravelCosts(13);
      activity.setBaseProceeds(14d);
      activity.setAssignments(new HashSet<OpAssignment>());
      broker.makePersistent(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.COLLECTION_TASK);
      activity.setStart(new Date(time + 3000));
      activity.setProjectPlan(plan);
      activity.setOutlineLevel((byte) 11);
      activity.setAssignments(new HashSet<OpAssignment>());
      broker.makePersistent(activity);

      t.commit();

      XComponent dataSet = new XComponent(XComponent.DATA_SET);

      Map costs = new HashMap();
      costs.put(new Integer(OpProjectCostsDataSetFactory.PERSONNEL_COST_INDEX), "Personal Cost");
      costs.put(new Integer(OpProjectCostsDataSetFactory.MATERIAL_COST_INDEX), "Material Cost");
      costs.put(new Integer(OpProjectCostsDataSetFactory.TRAVEL_COST_INDEX), "Travel Cost");
      costs.put(new Integer(OpProjectCostsDataSetFactory.EXTERNAL_COST_INDEX), "External Cost");
      costs.put(new Integer(OpProjectCostsDataSetFactory.MISC_COST_INDEX), "Misc Cost");
      costs.put(new Integer(OpProjectCostsDataSetFactory.PROCEEDS_COST_INDEX), "Proceeds Cost");

      OpProjectCostsDataSetFactory.fillCostsDataSet(broker, project, 10, dataSet, costs);

      //check the number of rows in the costs data set
      assertEquals(14, dataSet.getChildCount());

      //check the assignment of costs between the data set rows
      assertEquals(12d, ((XComponent) dataSet.getChild(1).getChild(1)).getValue());
      assertEquals(5d, ((XComponent) dataSet.getChild(1).getChild(2)).getValue());
      assertEquals(13d, ((XComponent) dataSet.getChild(2).getChild(1)).getValue());
      assertEquals(6d, ((XComponent) dataSet.getChild(2).getChild(2)).getValue());
      assertEquals(11d, ((XComponent) dataSet.getChild(3).getChild(1)).getValue());
      assertEquals(3d, ((XComponent) dataSet.getChild(3).getChild(2)).getValue());
      assertEquals(9d, ((XComponent) dataSet.getChild(4).getChild(1)).getValue());
      assertEquals(2d, ((XComponent) dataSet.getChild(4).getChild(2)).getValue());
      assertEquals(10d, ((XComponent) dataSet.getChild(5).getChild(1)).getValue());
      assertEquals(4d, ((XComponent) dataSet.getChild(5).getChild(2)).getValue());
      assertEquals(14d, ((XComponent) dataSet.getChild(6).getChild(1)).getValue());
      assertEquals(7d, ((XComponent) dataSet.getChild(6).getChild(2)).getValue());

      assertEquals(12d, ((XComponent) dataSet.getChild(8).getChild(1)).getValue());
      assertEquals(5d, ((XComponent) dataSet.getChild(8).getChild(2)).getValue());
      assertEquals(13d, ((XComponent) dataSet.getChild(9).getChild(1)).getValue());
      assertEquals(6d, ((XComponent) dataSet.getChild(9).getChild(2)).getValue());
      assertEquals(11d, ((XComponent) dataSet.getChild(10).getChild(1)).getValue());
      assertEquals(3d, ((XComponent) dataSet.getChild(10).getChild(2)).getValue());
      assertEquals(9d, ((XComponent) dataSet.getChild(11).getChild(1)).getValue());
      assertEquals(2d, ((XComponent) dataSet.getChild(11).getChild(2)).getValue());
      assertEquals(10d, ((XComponent) dataSet.getChild(12).getChild(1)).getValue());
      assertEquals(4d, ((XComponent) dataSet.getChild(12).getChild(2)).getValue());
      assertEquals(14d, ((XComponent) dataSet.getChild(13).getChild(1)).getValue());
      assertEquals(7d, ((XComponent) dataSet.getChild(13).getChild(2)).getValue());

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
      deleteAllObjects(broker, OpWorkSlip.WORK_SLIP);
      deleteAllObjects(broker, OpAssignment.ASSIGNMENT);
      deleteAllObjects(broker, OpActivityComment.ACTIVITY_COMMENT);
      deleteAllObjects(broker, OpActivity.ACTIVITY);
      deleteAllObjects(broker, OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(broker, OpAssignmentVersion.ASSIGNMENT_VERSION);
      deleteAllObjects(broker, OpProjectPlanVersion.PROJECT_PLAN_VERSION);
      deleteAllObjects(broker, OpActivityVersion.ACTIVITY_VERSION);

      List projectList = projectDataFactory.getAllProjects(broker);
      for (Object aProjectList : projectList) {
         OpProjectNode project = (OpProjectNode) aProjectList;
         broker.deleteObject(project);
      }

      List resoucesList = resourceDataFactory.getAllResources(broker);
      for (Object aResoucesList : resoucesList) {
         OpResource resource = (OpResource) aResoucesList;
         broker.deleteObject(resource);
      }

      List poolList = resourceDataFactory.getAllResourcePools(broker);
      for (Object aPoolList : poolList) {
         OpResourcePool pool = (OpResourcePool) aPoolList;
         if (pool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            continue;
         }
         broker.deleteObject(pool);
      }

      transaction.commit();
      broker.close();
   }

}
