/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.OpActivityTestDataFactory;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.project_planning.test.OpProjectPlanningTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.modules.work.*;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class test project service methods.
 *
 * @author lucian.furtos
 */
public class OpWorkServiceTest extends OpBaseOpenTestCase {

   private static final String RES_NAME = "res";
   private static final String RES_DESCR = "descr";
   private static final String PRJ_NAME = "prj";

   private static final String ACTIVITY1_NAME = "ActivityOne";

   private OpWorkService service;
   private OpWorkTestDataFactory dataFactory;
   private OpProjectPlanningService planningService;
   private OpProjectPlanningTestDataFactory planningDataFactory;
   private OpProjectTestDataFactory projectDataFactory;
   private OpResourceTestDataFactory resourceDataFactory;
   private OpActivityTestDataFactory activityDataFactory;

   private String resId;
   private String projId;
   private String planId;

   private OpResource resource1;
   private OpResource resource2;
   private String resId1;
   private String resId2;
   private XComponent dataRowRes1;
   private XComponent dataRowRes2;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      service = OpTestDataFactory.getWorkService();
      dataFactory = new OpWorkTestDataFactory(session);
      planningService = OpTestDataFactory.getProjectPlanningService();
      planningDataFactory = new OpProjectPlanningTestDataFactory(session);
      projectDataFactory = new OpProjectTestDataFactory(session);
      resourceDataFactory = new OpResourceTestDataFactory(session);
      activityDataFactory = new OpActivityTestDataFactory(session);

      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = resourceDataFactory.createResourceMsg("resource", "description", 50d, 2d, 1d, false, poolid);
      XMessage response = OpTestDataFactory.getResourceService().insertResource(session, request);
      assertNoError(response);
      resId = resourceDataFactory.getResourceByName("resource").locator();

      request = OpProjectTestDataFactory.createProjectMsg("project", new Date(1), 1d, null, null);
      response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);
      projId = projectDataFactory.getProjectId("project");

      OpBroker broker = session.newBroker();
      try {
         planId = projectDataFactory.getProjectById(broker, projId).getPlan().locator();

         request = resourceDataFactory.createResourceMsg(RES_NAME + 1, RES_DESCR, 100d, 2d, 1d, false, poolid);
         response = OpTestDataFactory.getResourceService().insertResource(session, request);
         assertNoError(response);
         resource1 = resourceDataFactory.getResourceByName(RES_NAME + 1);
         resId1 = resourceDataFactory.getResourceByName(RES_NAME + 1).locator();

         dataRowRes1 = new XComponent(XComponent.DATA_ROW);
         dataRowRes1.setStringValue(XValidator.choice(resource1.locator(), resource1.getName()));

         //0 - resource name
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(resource1.getName());
         dataRowRes1.addChild(dataCell);

         //1 - resource description
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(resource1.getDescription());
         dataRowRes1.addChild(dataCell);

         //2 - adjust rates
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setBooleanValue(false);
         dataRowRes1.addChild(dataCell);

         //3 - internal rate
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataRowRes1.addChild(dataCell);

         //4 - external rate
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataRowRes1.addChild(dataCell);

         request = resourceDataFactory.createResourceMsg(RES_NAME + 2, RES_DESCR, 100d, 5d, 3d, false, poolid);
         response = OpTestDataFactory.getResourceService().insertResource(session, request);
         assertNoError(response);
         resource2 = resourceDataFactory.getResourceByName(RES_NAME + 2);
         resId2 = resourceDataFactory.getResourceByName(RES_NAME + 2).locator();

         dataRowRes2 = new XComponent(XComponent.DATA_ROW);
         dataRowRes2.setStringValue(XValidator.choice(resource2.locator(), resource2.getName()));

         //0 - resource name
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(resource2.getName());
         dataRowRes2.addChild(dataCell);

         //1 - resource description
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(resource2.getDescription());
         dataRowRes2.addChild(dataCell);

         //2 - adjust rates
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setBooleanValue(false);
         dataRowRes2.addChild(dataCell);

         //3 - internal rate
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataRowRes2.addChild(dataCell);

         //4 - external rate
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataRowRes2.addChild(dataCell);
      }
      finally {
         broker.close();
      }
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

      OpActivity activity = new OpActivity(OpActivity.SCHEDULED_TASK);
      activity.setProjectPlan(plan);
      broker.makePersistent(activity);

      OpAssignment assignment = new OpAssignment();
      assignment.setProjectPlan(plan);
      assignment.setResource(resource);
      assignment.setActivity(activity);
      broker.makePersistent(assignment);

      t.commit();
      OpQuery query = broker.newQuery("from OpAssignment");
      assignment = (OpAssignment) broker.iterate(query).next();
      broker.close();

      XMessage request = OpWorkTestDataFactory.createInsertWorkSlipRequest(assignment, "", new Date(System.currentTimeMillis()),
           false, true, OpActivity.TASK, 32.43d, 321d, 545.432d, 654.32d, 423.31d, 543.32d);
      XMessage response = service.insertWorkSlip(session, request);
      assertNoError(response);

      //todo: check work slips and records
   }

   public void testInsertWorkSlipsSameDate()
        throws Exception {
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpProjectPlan plan = (OpProjectPlan) broker.getObject(planId);
      OpResource resource = (OpResource) broker.getObject(resId);

      OpActivity activity = new OpActivity(OpActivity.SCHEDULED_TASK);
      activity.setProjectPlan(plan);
      broker.makePersistent(activity);

      OpAssignment assignment = new OpAssignment();
      assignment.setProjectPlan(plan);
      assignment.setResource(resource);
      assignment.setActivity(activity);
      broker.makePersistent(assignment);

      t.commit();
      OpQuery query = broker.newQuery("from OpAssignment");
      assignment = (OpAssignment) broker.iterate(query).next();
      broker.close();

      Date wd_date = new Date(System.currentTimeMillis());
      XMessage request = OpWorkTestDataFactory.createInsertWorkSlipRequest(assignment, "", wd_date,
           false, true, OpActivity.TASK, 32.43d, 321d, 545.432d, 654.32d, 423.31d, 543.32d);
      XMessage response = service.insertWorkSlip(session, request);
      assertNoError(response);

      request = OpWorkTestDataFactory.createInsertWorkSlipRequest(assignment, "", wd_date,
           false, true, OpActivity.TASK, 32.43d, 321d, 545.432d, 654.32d, 423.31d, 543.32d);
      response = service.insertWorkSlip(session, request);
      assertError(response, OpWorkError.DUPLICATE_DATE);
   }

   /**
    * Test the update of activity actual cost when inserting workslips and the activity has two resources assigned to it
    *
    * @throws Exception
    */
   public void testUpdateActualCostsWithTwoResources()
        throws Exception {

      XComponent resources = new XComponent(XComponent.DATA_SET);
      resources.addChild(dataRowRes1);
      resources.addChild(dataRowRes2);

      Date startDate = new Date(getCalendarWithExactDaySet(2007, 6, 1).getTimeInMillis());

      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, startDate, 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, null, null);
      XMessage response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);

      String projectLocator = projectDataFactory.getProjectId(PRJ_NAME);
      String resource1Locator = resourceDataFactory.getResourceId(resource1.getName());
      String resource2Locator = resourceDataFactory.getResourceId(resource2.getName());

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpProjectNode project = (OpProjectNode) broker.getObject(projectLocator);
      resource1 = (OpResource) broker.getObject(resource1Locator);
      resource2 = (OpResource) broker.getObject(resource2Locator);

      OpActivity activity = new OpActivity();
      activity.setName(ACTIVITY1_NAME);
      activity.setStart(new Date(getCalendarWithExactDaySet(2007, 6, 5).getTimeInMillis()));
      activity.setFinish(new Date(getCalendarWithExactDaySet(2007, 6, 25).getTimeInMillis()));
      activity.setProjectPlan(project.getPlan());
      broker.makePersistent(activity);

      OpAssignment assignment1 = new OpAssignment();
      assignment1.setProjectPlan(project.getPlan());
      assignment1.setResource(resource1);
      assignment1.setActivity(activity);
      broker.makePersistent(assignment1);

      OpAssignment assignment2 = new OpAssignment();
      assignment2.setProjectPlan(project.getPlan());
      assignment2.setResource(resource2);
      assignment2.setActivity(activity);
      broker.makePersistent(assignment2);


      OpActivityDataSetFactory.updateWorkMonths(broker, assignment1, XCalendar.getDefaultCalendar());
      OpActivityDataSetFactory.updateWorkMonths(broker, assignment2, XCalendar.getDefaultCalendar());

      t.commit();
      broker.close();

      List<String> names = new ArrayList<String>();
      names.add("");
      names.add("");
      List<Double> actuals = new ArrayList<Double>();
      actuals.add(5d);
      actuals.add(3d);
      List<Double> remainings = new ArrayList<Double>();
      remainings.add(100d);
      remainings.add(150d);
      List<Double> materials = new ArrayList<Double>();
      materials.add(100d);
      materials.add(150d);
      List<Double> travels = new ArrayList<Double>();
      travels.add(100d);
      travels.add(150d);
      List<Double> externals = new ArrayList<Double>();
      externals.add(100d);
      externals.add(150d);
      List<Double> miscs = new ArrayList<Double>();
      miscs.add(100d);
      miscs.add(150d);

      List<OpAssignment> assignments = new ArrayList<OpAssignment>(2);
      String idAssignment1 = projectDataFactory.getActivityAssignmentId(resource1.getName());

      broker = session.newBroker();

      assignment1 = (OpAssignment) broker.getObject(idAssignment1);
      assignments.add(assignment1);

      String idAssignment2 = projectDataFactory.getActivityAssignmentId(resource2.getName());
      assignment2 = (OpAssignment) broker.getObject(idAssignment2);
      assignments.add(assignment2);

      //insert a workslip on 6/7/2007 with 5h of actual effort for the first assignment
      //                                   3h of actual effort for the second assignment
      request = OpWorkTestDataFactory.insertMoreWSMsg(assignments, new Date(getCalendarWithExactDaySet(2007, 6, 7).getTimeInMillis()),
           false, true, OpActivity.TASK, actuals, remainings, materials, travels, externals, miscs);
      broker.close();
      response = service.insertWorkSlip(session, request);
      assertNoError(response);

      String idActivity = activityDataFactory.getActivityId(ACTIVITY1_NAME);
      activity = activityDataFactory.getActivityById(idActivity);

      assertEquals(25d, activity.getActualPersonnelCosts(), 0d);
      assertEquals(14d, activity.getActualProceeds(), 0d);

      //insert a second workslip on 6/9/2007 with 2h of actual effort for the first assignment
      request = OpWorkTestDataFactory.createInsertWorkSlipRequest(assignments.get(0), "", new Date(getCalendarWithExactDaySet(2007, 6, 9).getTimeInMillis()),
           false, true, OpActivity.TASK, 2d, 321d, 545.432d, 654.32d, 423.31d, 543.32d);
      response = service.insertWorkSlip(session, request);
      assertNoError(response);

      activity = activityDataFactory.getActivityById(idActivity);

      assertEquals(29d, activity.getActualPersonnelCosts(), 0d);
      assertEquals(16d, activity.getActualProceeds(), 0d);
   }

   public void testGetAssignments()
        throws Exception {
      long time = System.currentTimeMillis();
      OpBroker broker = session.newBroker();
      try {
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
         long id = projectDataFactory.getProjectById(broker, projId).getID();
         Iterator it = OpWorkSlipDataSetFactory.getAssignments(broker, resources, types, date, null, id, true);
         assertTrue(it.hasNext());
      }
      finally {
         broker.close();
      }
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

      for (Object aProjectList : projectDataFactory.getAllProjects(broker)) {
         OpProjectNode project = (OpProjectNode) aProjectList;
         broker.deleteObject(project);
      }

      for (OpResource resource : resourceDataFactory.getAllResources(broker)) {
         broker.deleteObject(resource);
      }

      for (OpResourcePool pool : resourceDataFactory.getAllResourcePools(broker)) {
         if (pool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            continue;
         }
         broker.deleteObject(pool);
      }

      transaction.commit();
      broker.close();
   }

}
