/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.*;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.ResourceTestDataFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.UserTestDataFactory;
import onepoint.project.test.OpBaseTestCase;
import onepoint.project.test.TestDataFactory;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.*;

/**
 * This class test project service methods.
 *
 * @author lucian.furtos
 */
public class OpProjectServiceTest extends OpBaseTestCase {

   private static final String PRJ_NAME = "prj";
   private static final String PORTOFOLIO_NAME = "portofolio";

   private static final String RES_NAME = "res";
   private static final String RES_DESCR = "descr";

   private OpProjectAdministrationService service;
   private ProjectTestDataFactory dataFactory;
   private ResourceTestDataFactory resourceDataFactory;
   private String resId1;
   private String resId2;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      service = getProjectService();
      dataFactory = new ProjectTestDataFactory(session);
      resourceDataFactory = new ResourceTestDataFactory(session);

      clean();
      // create resources
      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = resourceDataFactory.createResourceMsg(RES_NAME + 1, RES_DESCR, 50d, 2d, false, poolid);
      XMessage response = getResourceService().insertResource(session, request);
      assertNoError(response);
      resId1 = resourceDataFactory.getResourceByName(RES_NAME + 1).locator();

      request = resourceDataFactory.createResourceMsg(RES_NAME + 2, RES_DESCR, 80d, 5d, false, poolid);
      response = getResourceService().insertResource(session, request);
      assertNoError(response);
      resId2 = resourceDataFactory.getResourceByName(RES_NAME + 2).locator();
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
    * Test happy flow for insert portofolio and possible errors
    *
    * @throws Exception if the tst fails
    */
   public void testInsertPortofolio()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createPortofolioMsg(null, null, null, null);
      XMessage response = service.insertPortfolio(session, request);
      assertError(response, OpProjectError.PORTFOLIO_NAME_MISSING);

      request = ProjectTestDataFactory.createPortofolioMsg("", null, null, null);
      response = service.insertPortfolio(session, request);
      assertError(response, OpProjectError.PORTFOLIO_NAME_MISSING);

      request = ProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      response = service.insertPortfolio(session, request);
      assertNoError(response);

      assertNotNull(dataFactory.getPortofolioByName(PORTOFOLIO_NAME));

      request = ProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, null, null, null);
      response = service.insertPortfolio(session, request);
      assertError(response, OpProjectError.PORTFOLIO_NAME_ALREADY_USED);
   }

   /**
    * Test happy flow for update portofolio
    *
    * @throws Exception if the tst fails
    */
   public void testUpdatePortofolio()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      XMessage response = service.insertPortfolio(session, request);
      assertNoError(response);

      OpProjectNode portfolio = dataFactory.getPortofolioByName(PORTOFOLIO_NAME);
      assertNotNull(portfolio);
      String id = portfolio.locator();

      request = ProjectTestDataFactory.updatePortofolioMsg(id, "New" + PORTOFOLIO_NAME, "new description", null);
      response = service.updatePortfolio(session, request);
      assertNoError(response);

      portfolio = dataFactory.getPortofolioById(id);
      assertNotNull(portfolio);
      assertEquals("New" + PORTOFOLIO_NAME, portfolio.getName());
      assertEquals("new description", portfolio.getDescription());
   }

   /**
    * Test errors for update portofolio
    *
    * @throws Exception if the tst fails
    */
   public void testUpdatePortofolioErrors()
        throws Exception {
      XMessage request = ProjectTestDataFactory.updatePortofolioMsg(null, null, null, null);
      XMessage response = service.updatePortfolio(session, request);
      assertError(response, OpProjectError.PORTFOLIO_NAME_MISSING);

      request = ProjectTestDataFactory.updatePortofolioMsg(null, "", null, null);
      response = service.updatePortfolio(session, request);
      assertError(response, OpProjectError.PORTFOLIO_NAME_MISSING);

      String fakeid = OpLocator.locatorString(OpProjectNode.PROJECT_NODE, 0);
      request = ProjectTestDataFactory.updatePortofolioMsg(fakeid, PORTOFOLIO_NAME, null, null);
      response = service.updatePortfolio(session, request);
      assertError(response, OpProjectError.PROJECT_NOT_FOUND);

      request = ProjectTestDataFactory.updatePortofolioMsg(fakeid, PORTOFOLIO_NAME, null, null);
      response = service.updatePortfolio(session, request);
      assertError(response, OpProjectError.PROJECT_NOT_FOUND);

      request = ProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      response = service.insertPortfolio(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createPortofolioMsg("PortofolioToUpdate", "portofolio description", null, null);
      response = service.insertPortfolio(session, request);
      assertNoError(response);
      String id = dataFactory.getPortofolioId("PortofolioToUpdate");

      request = ProjectTestDataFactory.updatePortofolioMsg(id, PORTOFOLIO_NAME, null, null);
      response = service.updatePortfolio(session, request);
      assertError(response, OpProjectError.PORTFOLIO_NAME_ALREADY_USED);
   }

   /**
    * Test happy flow for delete portofolio
    *
    * @throws Exception if the tst fails
    */
   public void testDeletePortofolio()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      XMessage response = service.insertPortfolio(session, request);
      assertNoError(response);

      OpProjectNode portfolio = dataFactory.getPortofolioByName(PORTOFOLIO_NAME);
      assertNotNull(portfolio);
      String id = portfolio.locator();

      request = ProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME + 1, "portofolio description", id, null);
      response = service.insertPortfolio(session, request);
      assertNoError(response);

      request = ProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME + 2, "portofolio description", id, null);
      response = service.insertPortfolio(session, request);
      assertNoError(response);

      List ids = new ArrayList();
      List portofolios = dataFactory.getAllPortofolios();
      assertEquals(4, portofolios.size());
      for (int i = 0; i < portofolios.size(); i++) {
         OpProjectNode p = (OpProjectNode) portofolios.get(i);
         if (!p.getName().equals(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)) {
            ids.add(p.locator());
         }
      }

      request = ProjectTestDataFactory.deletePortofolioMsg(ids);
      response = service.deletePortfolios(session, request);
      assertNoError(response);
      assertEquals(1, dataFactory.getAllPortofolios().size());
   }

   /**
    * Test project creation, happy flow.
    *
    * @throws Exception if the test fails
    */
   public void testInsertProject()
        throws Exception {
      ArrayList resources = new ArrayList();
      resources.add(resId1);
      resources.add(resId2);

      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
   }

   /**
    * Test project creation errors
    *
    * @throws Exception if the test fails
    */
   public void testInsertProjectErrors()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createProjectMsg(null, null, 0d, null, null);
      XMessage response = service.insertProject(session, request);
      assertError(response, OpProjectError.PROJECT_NAME_MISSING);

      request = ProjectTestDataFactory.createProjectMsg("", null, 0d, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.PROJECT_NAME_MISSING);

      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, null, 0d, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.START_DATE_MISSING);

      long time = System.currentTimeMillis();
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(time), new Date(time - 1), 0d, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.END_DATE_INCORRECT);

      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(time), new Date(time + 1000), -1d, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.BUDGET_INCORRECT);

      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(time), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(time), new Date(time + 1000), 1d, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.PROJECT_NAME_ALREADY_USED);
   }

   /**
    * Test project update, happy flow.
    *
    * @throws Exception if the test fails
    */
   public void testUpdateProject()
        throws Exception {
      ArrayList resources = new ArrayList();
      resources.add(resId1);
      Object[][] goals = {{Boolean.FALSE, "subject", new Integer(5)},
           {Boolean.FALSE, "subject_remove", new Integer(7)}};
      Object[][] todos = {{Boolean.FALSE, "todo", new Integer(1), new Date(System.currentTimeMillis())},
           {Boolean.FALSE, "todo_remove", new Integer(6), new Date(System.currentTimeMillis())}};
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.TRUE, Boolean.TRUE, resources, goals, todos);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      OpProjectNode project = dataFactory.getProjectByName(PRJ_NAME);
      Set goals_set = project.getGoals();
      List goal_ids = new ArrayList();
      for (Iterator iterator = goals_set.iterator(); iterator.hasNext();) {
         OpGoal goal = (OpGoal) iterator.next();
         if (goal.getName().equals("subject_remove")) {
            continue;
         }
         goal_ids.add(goal.locator());
      }
      Set todos_set = project.getToDos();
      List todo_ids = new ArrayList();
      for (Iterator iterator = todos_set.iterator(); iterator.hasNext();) {
         OpToDo todo = (OpToDo) iterator.next();
         if (todo.getName().equals("todo_remove")) {
            continue;
         }
         todo_ids.add(todo.locator());
      }

      String id = dataFactory.getProjectId(PRJ_NAME);
      resources.clear();
      resources.add(resId1);
      resources.add(resId2);
      Object[][] goals1 = {{Boolean.TRUE, "subject_new", new Integer(1)},
           {Boolean.FALSE, "subject3", new Integer(3)}};
      Object[][] todos1 = {{Boolean.TRUE, "todo_new", new Integer(1), new Date(System.currentTimeMillis())},
           {Boolean.FALSE, "todo2", new Integer(8), new Date(System.currentTimeMillis())}};
      request = ProjectTestDataFactory.updateProjectMsg(id, PRJ_NAME + 1, new Date(System.currentTimeMillis()), null, 100d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, ProjectTestDataFactory.createDataSet(goal_ids, goals1), ProjectTestDataFactory.createDataSet(todo_ids, todos1));
      response = service.updateProject(session, request);
      assertNoError(response);
   }

   public void testUpdateProjectError()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      String goodId = dataFactory.getProjectId(PRJ_NAME + 1);

      request = ProjectTestDataFactory.updateProjectMsg(null, null, null, null, 0d, null, null, null, null, null, null, null);
      response = service.updateProject(session, request);
      assertError(response, OpProjectError.PROJECT_NAME_MISSING);

      request = ProjectTestDataFactory.updateProjectMsg(null, "", null, null, 0d, null, null, null, null, null, null, null);
      response = service.updateProject(session, request);
      assertError(response, OpProjectError.PROJECT_NAME_MISSING);

      request = ProjectTestDataFactory.updateProjectMsg(null, PRJ_NAME, null, null, 0d, null, null, null, null, null, null, null);
      response = service.updateProject(session, request);
      assertError(response, OpProjectError.START_DATE_MISSING);

      request = ProjectTestDataFactory.updateProjectMsg(null, PRJ_NAME, new Date(System.currentTimeMillis()),
           new Date(System.currentTimeMillis() - 1000), 0d, null, null, null, null, null, null, null);
      response = service.updateProject(session, request);
      assertError(response, OpProjectError.END_DATE_INCORRECT);

      request = ProjectTestDataFactory.updateProjectMsg(null, PRJ_NAME, new Date(System.currentTimeMillis()),
           new Date(System.currentTimeMillis() + 1000), -1d, null, null, null, null, null, null, null);
      response = service.updateProject(session, request);
      assertError(response, OpProjectError.BUDGET_INCORRECT);

      String id = OpLocator.locatorString(OpProjectNode.PROJECT_NODE, -1);
      request = ProjectTestDataFactory.updateProjectMsg(id, PRJ_NAME, new Date(System.currentTimeMillis()),
           new Date(System.currentTimeMillis() + 1000), 1d, null, null, null, null, null, null, null);
      response = service.updateProject(session, request);
      assertError(response, OpProjectError.PROJECT_NOT_FOUND);

      request = ProjectTestDataFactory.updateProjectMsg(goodId, PRJ_NAME, new Date(System.currentTimeMillis()),
           new Date(System.currentTimeMillis() + 1000), 1d, null, null, null, null, null, null, null);
      response = service.updateProject(session, request);
      assertError(response, OpProjectError.PROJECT_NAME_ALREADY_USED);
   }

   /**
    * Test happy flow for project delete
    *
    * @throws Exception if the tst fails
    */
   public void testDeleteProject()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 12d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 23d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 3, new Date(System.currentTimeMillis()), 645d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      List ids = new ArrayList();
      List projects = dataFactory.getAllProjects();
      assertEquals(3, projects.size());
      for (int i = 0; i < projects.size(); i++) {
         OpProjectNode p = (OpProjectNode) projects.get(i);
         ids.add(p.locator());
      }

      request = ProjectTestDataFactory.deleteProjectMsg(ids);
      response = service.deleteProjects(session, request);
      assertNoError(response);
      assertEquals(0, dataFactory.getAllProjects().size());
   }

   public void testExpandNode()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      //todo: create portofolio

      OpBroker broker = session.newBroker();
      String id = OpProjectAdministrationService.findRootPortfolio(broker).locator();
      broker.close();
      request = new XMessage();
      XComponent row = new XComponent();
      row.setValue(id);
      row.setOutlineLevel(0);
      request.setArgument("project_row", row);
      request.setArgument("project_types", new Integer(OpProjectDataSetFactory.PROJECTS));
      response = service.expandProjectNode(session, request);
      assertNoError(response);
      List children = (List) response.getArgument(OpProjectConstants.CHILDREN);
      assertEquals(2, children.size());
   }

   public void testExpandNodeForView()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      String id = OpProjectAdministrationService.findRootPortfolio(broker).locator();
      broker.close();
      List ids = new ArrayList();
      ids.add(dataFactory.getProjectId(PRJ_NAME + 1));
      request = new XMessage();
      XComponent row = new XComponent();
      row.setValue(id);
      row.setOutlineLevel(0);
      request.setArgument("project_row", row);
      request.setArgument(OpProjectDataSetFactory.FILTERED_OUT_IDS, ids);
      request.setArgument(OpProjectDataSetFactory.ENABLE_PORTFOLIOS, Boolean.FALSE);
      request.setArgument(OpProjectDataSetFactory.ENABLE_PROJECTS, Boolean.TRUE);
      request.setArgument(OpProjectDataSetFactory.ENABLE_TEMPLATES, Boolean.FALSE);
      response = service.expandProjectChooserNode(session, request);
      assertNoError(response);
      List children = (List) response.getArgument(OpProjectConstants.CHILDREN);
      assertEquals(1, children.size());
   }

   public void testGetRootHierarchy()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      //todo: create portofolio

      List ids = new ArrayList();
      ids.add(dataFactory.getProjectId(PRJ_NAME + 1));
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpProjectDataSetFactory.retrieveProjectDataSetRootHierarchy(session, dataSet, OpProjectDataSetFactory.ALL_TYPES, true, ids);
      assertEquals(2, dataSet.getChildCount());
   }

   public void testRetrieveActivityDataSet()
        throws Exception {
      long time = System.currentTimeMillis();
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      String planId = dataFactory.getProjectByName(PRJ_NAME + 1).getPlan().locator();
      OpProjectPlan plan = dataFactory.getProjectPlanById(planId);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      plan.setCalculationMode(OpGanttValidator.INDEPENDENT);
      broker.updateObject(plan);

      OpActivity activity = new OpActivity(OpActivity.STANDARD);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(time + 1000));
      activity.setFinish(new Date(time + 61000));
      activity.setComplete(67d);
      activity.setTemplate(false);
      activity.setAttachments(new HashSet());
      activity.setAssignments(new HashSet());
      broker.makePersistent(activity);

      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId1));
      assignment.setProjectPlan(plan);
      broker.makePersistent(assignment);

      OpWorkPeriod workPeriod = new OpWorkPeriod();
      workPeriod.setActivity(activity);
      workPeriod.setProjectPlan(plan);
      workPeriod.setStart(new Date(time));
      workPeriod.setBaseEffort(7.5);
      workPeriod.setWorkingDays(5);
      broker.makePersistent(workPeriod);

      activity = new OpActivity(OpActivity.TASK);
      activity.setProjectPlan(plan);
      activity.setStart(new Date(time + 1500));
      activity.setFinish(new Date(time + 11500));
      activity.setComplete(80d);
      activity.setTemplate(false);
      activity.setAttachments(new HashSet());
      activity.setAssignments(new HashSet());
      broker.makePersistent(activity);

      assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId2));
      broker.makePersistent(assignment);

      workPeriod = new OpWorkPeriod();
      workPeriod.setActivity(activity);
      workPeriod.setProjectPlan(plan);
      workPeriod.setStart(new Date(time));
      workPeriod.setBaseEffort(7.5);
      workPeriod.setWorkingDays(5);
      broker.makePersistent(workPeriod);

      t.commit();
      broker.close();

      broker = session.newBroker();

      plan = (OpProjectPlan) broker.getObject(plan.locator());

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, plan, dataSet, false);
      assertEquals(2, dataSet.getChildCount());

      OpActivityFilter filter = new OpActivityFilter();
      filter.addProjectNodeID(dataFactory.getProjectByName(PRJ_NAME + 1).getID());
      filter.addResourceID(resourceDataFactory.getResourceById(resId1).getID());
      filter.addResourceID(resourceDataFactory.getResourceById(resId2).getID());
      filter.addType(OpActivity.TASK);
      filter.setStartFrom(new Date(time));
      filter.setStartTo(new Date(time + 10000));
      filter.setCompleted(Boolean.FALSE);
      filter.setTemplates(false);

      OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.PROJECT_NODE, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);

      dataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveFilteredActivityDataSet(broker, filter, order, dataSet);
      assertEquals(1, dataSet.getChildCount());
      broker.close();
   }

   public void testCountProjects()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(1), 1d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(1), 1d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      assertEquals(1, OpProjectDataSetFactory.countProjectNode(OpProjectNode.PORTFOLIO, broker));
      broker.close();

      broker = session.newBroker();
      assertEquals(2, OpProjectDataSetFactory.countProjectNode(OpProjectNode.PROJECT, session.newBroker()));
      broker.close();

      broker = session.newBroker();
      assertEquals(0, OpProjectDataSetFactory.countProjectNode(OpProjectNode.TEMPLATE, session.newBroker()));
      broker.close();
   }

   public void testProjectAndResourcesMapping()
        throws Exception {
      ArrayList resources1 = new ArrayList();
      resources1.add(resId1);
      ArrayList resources2 = new ArrayList();
      resources2.add(resId1);
      resources2.add(resId2);

      XComponent permissions = TestDataFactory.createPermissionSet(OpPermission.ADMINISTRATOR, adminId, OpUser.ADMINISTRATOR_NAME);
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(1), null, 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, new ArrayList(), null, null, permissions);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      String id = dataFactory.getProjectId(PRJ_NAME);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(1), null, 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources1, null, null, permissions);
      response = service.insertProject(session, request);
      assertNoError(response);
      String id1 = dataFactory.getProjectId(PRJ_NAME + 1);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(1), null, 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources2, null, null, permissions);
      response = service.insertProject(session, request);
      assertNoError(response);
      String id2 = dataFactory.getProjectId(PRJ_NAME + 2);

      Map map = OpProjectDataSetFactory.getProjectToResourceMap(session);
      List res = (List) map.get(XValidator.choice(id, PRJ_NAME));
      assertNull(res);
      res = (List) map.get(XValidator.choice(id1, PRJ_NAME + 1));
      assertNotNull(res);
      assertEquals(1, res.size());
      assertTrue(res.contains(XValidator.choice(resId1, RES_NAME + 1)));
      res = (List) map.get(XValidator.choice(id2, PRJ_NAME + 2));
      assertEquals(2, res.size());
      assertTrue(res.contains(XValidator.choice(resId1, RES_NAME + 1)) && res.contains(XValidator.choice(resId2, RES_NAME + 2)));
   }

   public void testMoveProjects()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 12d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 23d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 3, new Date(System.currentTimeMillis()), 645d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      List ids = new ArrayList();
      List projects = dataFactory.getAllProjects();
      assertEquals(3, projects.size());
      for (int i = 0; i < projects.size(); i++) {
         OpProjectNode p = (OpProjectNode) projects.get(i);
         ids.add(p.locator());
      }

      request = ProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      response = service.insertPortfolio(session, request);
      assertNoError(response);
      OpProjectNode portofolio = dataFactory.getPortofolioByName(PORTOFOLIO_NAME);
      assertTrue(portofolio.getSubNodes().isEmpty());

      request = ProjectTestDataFactory.moveProjectsMsg(portofolio.locator(), ids);
      response = service.moveProjectNode(session, request);
      assertNoError(response);

      portofolio = dataFactory.getPortofolioById(portofolio.locator());
      assertEquals(3, portofolio.getSubNodes().size());
   }

   // ********* Test DataSet Factories **********

   public void testRetrieveProjectDataSet()
        throws Exception {
      XMessage request = ProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      XMessage response = service.insertPortfolio(session, request);
      assertNoError(response);
      String id = dataFactory.getPortofolioId(PORTOFOLIO_NAME);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 23d, null, id);
      response = service.insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 645d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpBroker broker = session.newBroker();
      OpProjectDataSetFactory.retrieveProjectDataSet(session, broker, dataSet, OpProjectDataSetFactory.ALL_TYPES, true);
      broker.close();

      assertEquals(4, dataSet.getChildCount());
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

      deleteAllObjects(OpWorkPeriod.WORK_PERIOD);
      deleteAllObjects(OpAssignment.ASSIGNMENT);
      deleteAllObjects(OpActivity.ACTIVITY);
      deleteAllObjects(OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(OpAssignmentVersion.ASSIGNMENT_VERSION);
      deleteAllObjects(OpProjectPlanVersion.PROJECT_PLAN_VERSION);
      deleteAllObjects(OpActivityVersion.ACTIVITY_VERSION);

      List projectList = dataFactory.getAllProjects();
      for (Iterator iterator = projectList.iterator(); iterator.hasNext();) {
         OpProjectNode project = (OpProjectNode) iterator.next();
         dataFactory.deleteObject(project);
      }

      List portofolioList = dataFactory.getAllPortofolios();
      for (Iterator iterator = portofolioList.iterator(); iterator.hasNext();) {
         OpProjectNode portofolio = (OpProjectNode) iterator.next();
         if (portofolio.getName().equals(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)) {
            continue;
         }
         dataFactory.deleteObject(portofolio);
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
