/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project_costs.test;

import onepoint.express.XComponent;
import onepoint.persistence.*;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.ProjectTestDataFactory;
import onepoint.project.modules.project_costs.OpProjectCostsDataSetFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.project_planning.test.ProjectPlanningTestDataFactory;
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
public class OpProjectCostTest extends OpBaseTestCase {

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

      activity = new OpActivity();
      activity.setType(OpActivity.COLLECTION_TASK);
      activity.setStart(new Date(1000));
      activity.setProjectPlan(plan);
      activity.setOutlineLevel((byte) 11);
      broker.makePersistent(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.COLLECTION_TASK);
      activity.setStart(new Date(1000));
      activity.setOutlineLevel((byte) 10);
      broker.makePersistent(activity);

      t.commit();

      XComponent dataSet = new XComponent(XComponent.DATA_SET);

      Map costs = new HashMap();
      costs.put(new Integer(OpProjectCostsDataSetFactory.PERSONNEL_COST_INDEX), "Personal Cost");
      costs.put(new Integer(OpProjectCostsDataSetFactory.MATERIAL_COST_INDEX), "Material Cost");
      costs.put(new Integer(OpProjectCostsDataSetFactory.TRAVEL_COST_INDEX), "Travel Cost");
      costs.put(new Integer(OpProjectCostsDataSetFactory.EXTERNAL_COST_INDEX), "External Cost");
      costs.put(new Integer(OpProjectCostsDataSetFactory.MISC_COST_INDEX), "Misc Cost");

      OpProjectCostsDataSetFactory.fillCostsDataSet(broker, project, 10, dataSet, costs);

      assertEquals(2 * (1 + 5), dataSet.getChildCount());
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
