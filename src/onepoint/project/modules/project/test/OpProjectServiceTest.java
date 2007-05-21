/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.*;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpHourlyRatesPeriod;
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
   private OpResource  resource1;
   private OpResource  resource2;
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

      service = getProjectService();
      dataFactory = new ProjectTestDataFactory(session);
      resourceDataFactory = new ResourceTestDataFactory(session);

      clean();
      // create resources
      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = resourceDataFactory.createResourceMsg(RES_NAME + 1, RES_DESCR, 50d, 2d, 2d, false, poolid);
      XMessage response = getResourceService().insertResource(session, request);
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

      request = resourceDataFactory.createResourceMsg(RES_NAME + 2, RES_DESCR, 80d, 5d, 3d, false, poolid);
      response = getResourceService().insertResource(session, request);
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
      XComponent resources = new XComponent(XComponent.DATA_SET);

      resources.addChild(dataRowRes1);
      resources.addChild(dataRowRes2);

      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
   }

   /**
    * Test happy-flow creation of a project with OpHourlyRatesPeriods
    *
    * @throws Exception if the test fails
    */
   public void testInsertProjectWithHourlyRatesPeriods()
        throws Exception {
      XComponent resources = new XComponent(XComponent.DATA_SET);

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataRow1.setOutlineLevel(1);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      Calendar calendar = Calendar.getInstance();
      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 18, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow1.addChild(dataCell);

      XComponent dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataRow2.setOutlineLevel(1);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 22, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(12d);
      dataRow2.addChild(dataCell);

      resources.addChild(dataRowRes1);
      resources.addChild(dataRow1);
      resources.addChild(dataRowRes2);
      resources.addChild(dataRow2);

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
    * Test mapping of HourlyRatesPeriods errors at creation of a project
    *
    * @throws Exception if the test fails
    */
   public void testMapHourlyRatesPeriodsErrors()
        throws Exception {
       XComponent resources = new XComponent(XComponent.DATA_SET);

      Calendar calendar = Calendar.getInstance();
      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataRow1.setOutlineLevel(1);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      calendar.set(2006, 4, 13,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 18,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow1.addChild(dataCell);

      XComponent dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataRow2.setOutlineLevel(1);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 19,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 22,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(12d);
      dataRow2.addChild(dataCell);

      ((XComponent) dataRow1.getChild(OpProjectAdministrationService.INTERNAL_PERIOD_RATE_COLUMN_INDEX)).setDoubleValue(-1d);
      resources.addChild(dataRowRes1);
      resources.addChild(dataRow1);
      resources.addChild(dataRow2);
      resources.addChild(dataRowRes2);

      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, null, null);
      XMessage response = service.insertProject(session, request);
      assertError(response, OpProjectError.INTERNAL_RATE_NOT_VALID);

      ((XComponent)dataRow1.getChild(OpProjectAdministrationService.INTERNAL_PERIOD_RATE_COLUMN_INDEX)).setDoubleValue(3d);
      ((XComponent)dataRow1.getChild(OpProjectAdministrationService.EXTERNAL_PERIOD_RATE_COLUMN_INDEX)).setDoubleValue(-1d);
      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.EXTERNAL_RATE_NOT_VALID);

      ((XComponent)dataRow1.getChild(OpProjectAdministrationService.EXTERNAL_PERIOD_RATE_COLUMN_INDEX)).setDoubleValue(6d);
      ((XComponent)dataRow1.getChild(OpProjectAdministrationService.PERIOD_START_DATE)).setDateValue(null);

      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.PERIOD_START_DATE_NOT_VALID);

      calendar.set(2006, 4, 13,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      ((XComponent)dataRow1.getChild(OpProjectAdministrationService.PERIOD_START_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));
      ((XComponent)dataRow1.getChild(OpProjectAdministrationService.PERIOD_END_DATE)).setDateValue(null);

      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.PERIOD_END_DATE_NOT_VALID);

      calendar.set(2006, 4, 11,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      ((XComponent)dataRow1.getChild(OpProjectAdministrationService.PERIOD_END_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));

      request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.PERIOD_INTERVAL_NOT_VALID);

      calendar.set(2006, 4, 20,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      ((XComponent)dataRow1.getChild(OpProjectAdministrationService.PERIOD_END_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));

       request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.DATE_INTERVAL_OVERLAP);
   }

   /**
    * Test project update, happy flow.
    *
    * @throws Exception if the test fails
    */
   public void testUpdateProject()
        throws Exception {
      XComponent resources = new XComponent(XComponent.DATA_SET);
      resources.addChild(dataRowRes1);
      resources.addChild(dataRowRes2);
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
      resources.removeAllChildren();

      Calendar calendar = Calendar.getInstance();
      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataRow1.setOutlineLevel(1);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      dataRow1.addChild(dataCell);
      calendar.set(2006, 4, 13,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 18,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow1.addChild(dataCell);

      XComponent dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataRow2.setOutlineLevel(1);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 19,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 22,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(12d);
      dataRow2.addChild(dataCell);

      resources.addChild(dataRowRes1);
      resources.addChild(dataRow1);
      resources.addChild(dataRow2);
      resources.addChild(dataRowRes2);

      Object[][] goals1 = {{Boolean.TRUE, "subject_new", new Integer(1)},
           {Boolean.FALSE, "subject3", new Integer(3)}};
      Object[][] todos1 = {{Boolean.TRUE, "todo_new", new Integer(1), new Date(System.currentTimeMillis())},
           {Boolean.FALSE, "todo2", new Integer(8), new Date(System.currentTimeMillis())}};
      request = ProjectTestDataFactory.updateProjectMsg(id, PRJ_NAME + 1, new Date(System.currentTimeMillis()), null, 100d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, ProjectTestDataFactory.createDataSet(goal_ids, goals1), ProjectTestDataFactory.createDataSet(todo_ids, todos1));
      response = service.updateProject(session, request);
      assertNoError(response);

      project = dataFactory.getProjectByName(PRJ_NAME + 1);
      assertEquals(2,project.getAssignments().size());
      for(OpProjectNodeAssignment assignment : project.getAssignments()){
         if(assignment.getResource().getID() == resource1.getID()){
            assertEquals(2,assignment.getHourlyRatesPeriods().size());
         }
         else{
            assertEquals(0,assignment.getHourlyRatesPeriods().size());
         }
      }

      dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataRow2.setOutlineLevel(1);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 19,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 25,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(16d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(12d);
      dataRow2.addChild(dataCell);

      XComponent dataRow3 = new XComponent(XComponent.DATA_ROW);
      dataRow3.setOutlineLevel(1);
      dataRow3.addChild(dataCell);
      dataRow3.addChild(dataCell);
      dataRow3.addChild(dataCell);
      dataRow3.addChild(dataCell);
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 5, 10,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,5, 20,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow3.addChild(dataCell);

      resources.removeAllChildren();
      resources.addChild(dataRowRes1);
      resources.addChild(dataRow2);
      resources.addChild(dataRowRes2);
      resources.addChild(dataRow3);

      request = ProjectTestDataFactory.updateProjectMsg(id, PRJ_NAME + 1, new Date(System.currentTimeMillis()), null, 100d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, ProjectTestDataFactory.createDataSet(goal_ids, goals1), ProjectTestDataFactory.createDataSet(todo_ids, todos1));
      response = service.updateProject(session, request);
      assertNoError(response);

      project = dataFactory.getProjectByName(PRJ_NAME + 1);
      assertEquals(2,project.getAssignments().size());
      for(OpProjectNodeAssignment assignment : project.getAssignments()){
         if(assignment.getResource().getID() == resource1.getID()){
            assertEquals(1,assignment.getHourlyRatesPeriods().size());
         }
         else{
            assertEquals(1,assignment.getHourlyRatesPeriods().size());
         }
      }
   }

   public void testModifiedRatesForResourcesTab()
      throws Exception {

      ((XComponent) dataRowRes1.getChild(OpProjectAdministrationService.ADJUST_RATES_COLUMN_INDEX)).setBooleanValue(true);
      ((XComponent) dataRowRes1.getChild(OpProjectAdministrationService.INTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(0);
      ((XComponent) dataRowRes1.getChild(OpProjectAdministrationService.EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(7);
      ((XComponent) dataRowRes2.getChild(OpProjectAdministrationService.ADJUST_RATES_COLUMN_INDEX)).setBooleanValue(true);
      ((XComponent) dataRowRes2.getChild(OpProjectAdministrationService.INTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(5);
      ((XComponent) dataRowRes2.getChild(OpProjectAdministrationService.EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(3);

      XComponent resources = new XComponent(XComponent.DATA_SET);
      resources.addChild(dataRowRes1);
      resources.addChild(dataRowRes2);
      Object[][] goals = {{Boolean.FALSE, "subject", new Integer(5)},
           {Boolean.FALSE, "subject_remove", new Integer(7)}};
      Object[][] todos = {{Boolean.FALSE, "todo", new Integer(1), new Date(System.currentTimeMillis())},
           {Boolean.FALSE, "todo_remove", new Integer(6), new Date(System.currentTimeMillis())}};
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.TRUE, Boolean.TRUE, resources, goals, todos);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);

      OpProjectNode project = dataFactory.getProjectByName(PRJ_NAME);

      Calendar calendarStart = Calendar.getInstance();
      calendarStart.set(2006, 4, 15,0,0,0);
      calendarStart.set(Calendar.MILLISECOND,0);
      Calendar calendarEnd = Calendar.getInstance();
      calendarEnd.set(2006, 4, 20,0,0,0);
      calendarEnd.set(Calendar.MILLISECOND,0);

      request = new XMessage();
      request.setArgument(OpActivity.NAME, "Activity1");
      request.setArgument(OpActivity.START, new Date(calendarStart.getTimeInMillis()));
      request.setArgument(OpActivity.FINISH, new Date(calendarEnd.getTimeInMillis()));
      request.setArgument(OpActivity.PROJECT_PLAN, project.getPlan());

      OpActivity activity1 = new OpActivity();
      activity1.setStart(new Date(calendarStart.getTimeInMillis()));
      activity1.setFinish(new Date(calendarEnd.getTimeInMillis()));
      activity1.setName("Activity 1");
      activity1.setProjectPlan(project.getPlan());

      calendarStart.set(2006, 4, 10,0,0,0);
      calendarStart.set(Calendar.MILLISECOND,0);
      calendarEnd.set(2006, 4, 12,0,0,0);
      calendarEnd.set(Calendar.MILLISECOND,0);

      OpActivity activity2 = new OpActivity();

      activity2.setStart(new Date(calendarStart.getTimeInMillis()));
      activity2.setFinish(new Date(calendarEnd.getTimeInMillis()));
      activity2.setName("Activity 2");
      activity2.setProjectPlan(project.getPlan());

      OpAssignment activityAssignment1 = new OpAssignment();
      activityAssignment1.setResource(resource1);
      activityAssignment1.setProjectPlan(project.getPlan());
      activityAssignment1.setActivity(activity1);

      OpAssignment activityAssignment2 = new OpAssignment();
      activityAssignment2.setResource(resource1);
      activityAssignment2.setProjectPlan(project.getPlan());
      activityAssignment2.setActivity(activity2);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      broker.makePersistent(activity1);
      broker.makePersistent(activity2);
      broker.makePersistent(activityAssignment1);
      broker.makePersistent(activityAssignment2);

      t.commit();
      broker.close();

      //nothing changed - return false
      XComponent newDataSet = new XComponent(XComponent.DATA_SET);
      newDataSet.addChild(dataRowRes1);
      newDataSet.addChild(dataRowRes2);
      XComponent originalDataSet = new XComponent(XComponent.DATA_SET);
      originalDataSet.addChild(dataRowRes1);
      originalDataSet.addChild(dataRowRes2);

      request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PROJECT_ID, project.locator());
      request.setArgument(OpProjectAdministrationService.RESOURCE_SET, newDataSet);
      request.setArgument(OpProjectAdministrationService.ORIGINAL_RESOURCE_SET, originalDataSet);

      response = service.checkModifiedRates(session, request);
      assertFalse(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS)).booleanValue());

      //change the internal rate of resource one to 5 - this will affect resource's the activity - return true
      XComponent newDataRow = dataRowRes1.copyData();
      ((XComponent)newDataRow.getChild(OpProjectAdministrationService.INTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(5d);
      newDataSet = new XComponent(XComponent.DATA_SET);
      newDataSet.addChild(newDataRow);
      newDataSet.addChild(dataRowRes2);

      request.setArgument(OpProjectAdministrationService.RESOURCE_SET, newDataSet);
      response = service.checkModifiedRates(session, request);
      assertTrue(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS)).booleanValue());

      //insert two periods for resource one so that they do not entirely cover the activitie's periods
      //but do not change any of the rates in the resource tab - return false
      Calendar calendar = Calendar.getInstance();
      XComponent periodRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      periodRow1.setOutlineLevel(1);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      calendar.set(2006, 4, 13,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 19,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(21d);
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(21d);
      periodRow1.addChild(dataCell);

      XComponent periodRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      periodRow2.setOutlineLevel(1);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      calendar.set(2006, 4, 25,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 27,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(22d);
      periodRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(22d);
      periodRow2.addChild(dataCell);

      newDataSet = new XComponent(XComponent.DATA_SET);
      newDataSet.addChild(dataRowRes1);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      originalDataSet = new XComponent(XComponent.DATA_SET);
      originalDataSet.addChild(dataRowRes1);
      originalDataSet.addChild(dataRowRes2);

      request.setArgument(OpProjectAdministrationService.PROJECT_ID, project.locator());
      request.setArgument(OpProjectAdministrationService.RESOURCE_SET, newDataSet);
      request.setArgument(OpProjectAdministrationService.ORIGINAL_RESOURCE_SET, originalDataSet);
      response = service.checkModifiedRates(session, request);
      assertFalse(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS)).booleanValue());

      //change the rate of resource one to 5d - this will affect it's activities - return true
      newDataSet = new XComponent(XComponent.DATA_SET);
      newDataSet.addChild(newDataRow);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      originalDataSet = new XComponent(XComponent.DATA_SET);
      originalDataSet.addChild(dataRowRes1);
      originalDataSet.addChild(dataRowRes2);

      request.setArgument(OpProjectAdministrationService.PROJECT_ID, project.locator());
      request.setArgument(OpProjectAdministrationService.RESOURCE_SET, newDataSet);
      request.setArgument(OpProjectAdministrationService.ORIGINAL_RESOURCE_SET, originalDataSet);
      response = service.checkModifiedRates(session, request);
      assertTrue(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS)).booleanValue());

      //insert two periods for resource one so that they cover the activitie's periods
      //but do not change any of the rates in the resource tab - return false
      periodRow1 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      periodRow1.setOutlineLevel(1);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      calendar.set(2006, 4, 10,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 20,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(21d);
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(21d);
      periodRow1.addChild(dataCell);

      periodRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      periodRow2.setOutlineLevel(1);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      calendar.set(2006, 4, 5,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 9,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(22d);
      periodRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(22d);
      periodRow2.addChild(dataCell);

      newDataSet = new XComponent(XComponent.DATA_SET);
      newDataSet.addChild(dataRowRes1);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      originalDataSet = new XComponent(XComponent.DATA_SET);
      originalDataSet.addChild(dataRowRes1);
      originalDataSet.addChild(dataRowRes2);

      request.setArgument(OpProjectAdministrationService.PROJECT_ID, project.locator());
      request.setArgument(OpProjectAdministrationService.RESOURCE_SET, newDataSet);
      request.setArgument(OpProjectAdministrationService.ORIGINAL_RESOURCE_SET, originalDataSet);
      response = service.checkModifiedRates(session, request);
      assertFalse(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS)).booleanValue());

      //change the rate of the first resource to 5d - this resource's activities are covered by periods - return false
      newDataSet = new XComponent(XComponent.DATA_SET);
      newDataSet.addChild(newDataRow);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      originalDataSet = new XComponent(XComponent.DATA_SET);
      originalDataSet.addChild(dataRowRes1);
      originalDataSet.addChild(dataRowRes2);

      request.setArgument(OpProjectAdministrationService.PROJECT_ID, project.locator());
      request.setArgument(OpProjectAdministrationService.RESOURCE_SET, newDataSet);
      request.setArgument(OpProjectAdministrationService.ORIGINAL_RESOURCE_SET, originalDataSet);
      response = service.checkModifiedRates(session, request);
      assertFalse(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS)).booleanValue());

      ((XComponent)dataRowRes1.getChild(OpProjectAdministrationService.INTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(2);
      ((XComponent)dataRowRes1.getChild(OpProjectAdministrationService.EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(2);
      ((XComponent)newDataRow.getChild(OpProjectAdministrationService.ADJUST_RATES_COLUMN_INDEX)).setBooleanValue(false);
      ((XComponent)newDataRow.getChild(OpProjectAdministrationService.INTERNAL_PROJECT_RATE_COLUMN_INDEX)).setValue(null);
      ((XComponent)newDataRow.getChild(OpProjectAdministrationService.EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).setValue(null);

      periodRow1 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      periodRow1.setOutlineLevel(1);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      calendar.set(2006, 4, 10,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 19,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(21d);
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(21d);
      periodRow1.addChild(dataCell);

      newDataSet = new XComponent(XComponent.DATA_SET);
      newDataSet.addChild(newDataRow);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      originalDataSet = new XComponent(XComponent.DATA_SET);
      originalDataSet.addChild(dataRowRes1);
      originalDataSet.addChild(dataRowRes2);

      request.setArgument(OpProjectAdministrationService.PROJECT_ID, project.locator());
      request.setArgument(OpProjectAdministrationService.RESOURCE_SET, newDataSet);
      request.setArgument(OpProjectAdministrationService.ORIGINAL_RESOURCE_SET, originalDataSet);
      response = service.checkModifiedRates(session, request);
      assertFalse(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS)).booleanValue());

      ((XComponent)newDataRow.getChild(OpProjectAdministrationService.ADJUST_RATES_COLUMN_INDEX)).setBooleanValue(true);
      ((XComponent)newDataRow.getChild(OpProjectAdministrationService.INTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(15d);
      ((XComponent)newDataRow.getChild(OpProjectAdministrationService.EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(15d);

      newDataSet = new XComponent(XComponent.DATA_SET);
      newDataSet.addChild(newDataRow);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      originalDataSet = new XComponent(XComponent.DATA_SET);
      originalDataSet.addChild(dataRowRes1);
      originalDataSet.addChild(dataRowRes2);

      request.setArgument(OpProjectAdministrationService.PROJECT_ID, project.locator());
      request.setArgument(OpProjectAdministrationService.RESOURCE_SET, newDataSet);
      request.setArgument(OpProjectAdministrationService.ORIGINAL_RESOURCE_SET, originalDataSet);
      response = service.checkModifiedRates(session, request);
      assertTrue(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS)).booleanValue());
   }

   /**
    * Test if the project's assignments have activities in the time periods of the OpHourlyRatesPeriods set
    *
    * @throws Exception if the test fails
    */
   public void testHaveAssignmentsInTimePeriod()
        throws Exception {

      XComponent resources = new XComponent(XComponent.DATA_SET);
      ((XComponent)dataRowRes1.getChild(OpProjectAdministrationService.ADJUST_RATES_COLUMN_INDEX)).setBooleanValue(true);
      ((XComponent)dataRowRes1.getChild(OpProjectAdministrationService.INTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(15d);
      ((XComponent)dataRowRes1.getChild(OpProjectAdministrationService.EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(16d);
      ((XComponent)dataRowRes2.getChild(OpProjectAdministrationService.ADJUST_RATES_COLUMN_INDEX)).setBooleanValue(true);
      ((XComponent)dataRowRes2.getChild(OpProjectAdministrationService.INTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(12d);
      ((XComponent)dataRowRes2.getChild(OpProjectAdministrationService.EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).setDoubleValue(13d);

      resources.addChild(dataRowRes1);
      resources.addChild(dataRowRes2);
      Object[][] goals = {{Boolean.FALSE, "subject", new Integer(5)},
           {Boolean.FALSE, "subject_remove", new Integer(7)}};
      Object[][] todos = {{Boolean.FALSE, "todo", new Integer(1), new Date(System.currentTimeMillis())},
           {Boolean.FALSE, "todo_remove", new Integer(6), new Date(System.currentTimeMillis())}};
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null,
           Boolean.TRUE, Boolean.TRUE, resources, goals, todos);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);

      OpProjectNode project = dataFactory.getProjectByName(PRJ_NAME);

      resource1 = resourceDataFactory.getResourceByName(RES_NAME + 1);
      resource2 = resourceDataFactory.getResourceByName(RES_NAME + 2);

      OpProjectNodeAssignment projectNodeAssignment1 = new OpProjectNodeAssignment();
      OpProjectNodeAssignment projectNodeAssignment2 = new OpProjectNodeAssignment();
      for (OpProjectNodeAssignment resourceAssignment : resource1.getProjectNodeAssignments()) {
         for (OpProjectNodeAssignment projectAssignment : project.getAssignments()) {
            if (resourceAssignment.getID() == projectAssignment.getID()) {
               projectNodeAssignment1 = projectAssignment;
            }
         }
      }
      for (OpProjectNodeAssignment resourceAssignment : resource2.getProjectNodeAssignments()) {
         for (OpProjectNodeAssignment projectAssignment : project.getAssignments()) {
            if (resourceAssignment.getID() == projectAssignment.getID()) {
               projectNodeAssignment2 = projectAssignment;
            }
         }
      }

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      Calendar calendar = Calendar.getInstance();
      OpActivity activity1 = new OpActivity();
      OpAssignment activityAssignment1 = new OpAssignment();
      activityAssignment1.setActivity(activity1);
      activityAssignment1.setResource(resource1);
      activityAssignment1.setProjectPlan(project.getPlan());

      calendar.set(2006, 3, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setFinish(new Date(calendar.getTimeInMillis()));
      activity1.setName("activity1");
      activity1.setProjectPlan(project.getPlan());

      broker.makePersistent(activity1);
      broker.makePersistent(activityAssignment1);

      OpActivity activity2 = new OpActivity();
      OpAssignment activityAssignment2 = new OpAssignment();
      activityAssignment2.setActivity(activity2);
      activityAssignment2.setResource(resource1);
      activityAssignment2.setProjectPlan(project.getPlan());

      calendar.set(2006, 4, 7, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setFinish(new Date(calendar.getTimeInMillis()));
      activity2.setName("activity2");
      activity2.setProjectPlan(project.getPlan());

      broker.makePersistent(activity2);
      broker.makePersistent(activityAssignment2);

      OpActivity activity3 = new OpActivity();
      OpAssignment activityAssignment3 = new OpAssignment();
      activityAssignment3.setActivity(activity3);
      activityAssignment3.setResource(resource2);
      activityAssignment3.setProjectPlan(project.getPlan());

      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity3.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity3.setFinish(new Date(calendar.getTimeInMillis()));
      activity3.setName("activity3");
      activity3.setProjectPlan(project.getPlan());

      broker.makePersistent(activity3);
      broker.makePersistent(activityAssignment3);

      OpHourlyRatesPeriod hourlyRatesPeriod1 = new OpHourlyRatesPeriod();
      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 26, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod1.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod1.setInternalRate(9d);
      hourlyRatesPeriod1.setExternalRate(3d);
      hourlyRatesPeriod1.setProjectNodeAssignment(projectNodeAssignment1);

      OpHourlyRatesPeriod hourlyRatesPeriod2 = new OpHourlyRatesPeriod();
      calendar.set(2006, 3, 29, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod2.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod2.setInternalRate(6d);
      hourlyRatesPeriod2.setExternalRate(0d);
      hourlyRatesPeriod2.setProjectNodeAssignment(projectNodeAssignment1);

      OpHourlyRatesPeriod hourlyRatesPeriod3 = new OpHourlyRatesPeriod();
      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod3.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod3.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod3.setInternalRate(2d);
      hourlyRatesPeriod3.setExternalRate(2d);
      hourlyRatesPeriod3.setProjectNodeAssignment(projectNodeAssignment2);

      broker.makePersistent(hourlyRatesPeriod1);
      broker.makePersistent(hourlyRatesPeriod2);
      broker.makePersistent(hourlyRatesPeriod3);

      t.commit();
      broker.close();

      XComponent periodRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      periodRow1.setOutlineLevel(1);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      periodRow1.addChild(dataCell);
      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 26, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      periodRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      periodRow1.addChild(dataCell);


      XComponent periodRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      periodRow2.setOutlineLevel(1);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      periodRow2.addChild(dataCell);
      calendar.set(2006, 3, 29, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      periodRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0d);
      periodRow2.addChild(dataCell);

      XComponent periodRow3 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      periodRow3.setOutlineLevel(1);
      periodRow3.addChild(dataCell);
      periodRow3.addChild(dataCell);
      periodRow3.addChild(dataCell);
      periodRow3.addChild(dataCell);
      periodRow3.addChild(dataCell);
      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(2d);
      periodRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(2d);
      periodRow3.addChild(dataCell);

      XComponent newDataSet = new XComponent(XComponent.DATA_SET);
      newDataSet.addChild(dataRowRes1);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      newDataSet.addChild(periodRow3);

      //nothing changed - return false
      request.setArgument(OpProjectAdministrationService.PROJECT_ID, project.locator());
      request.setArgument(OpProjectAdministrationService.RESOURCE_SET, newDataSet);
      response = service.haveAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //extend the period of resource two to 3/25/2006 - 4/30/206 - this won't affect it's activity - return false
      calendar.set(2006, 3, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ((XComponent)periodRow3.getChild(OpProjectAdministrationService.PERIOD_START_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));     
      calendar.set(2006, 4, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ((XComponent)periodRow3.getChild(OpProjectAdministrationService.PERIOD_END_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));

      response = service.haveAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //extend the period of resource two to 3/21/2006 - 4/30/206 - this will affect it's activity - return true
      calendar.set(2006, 3, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ((XComponent)periodRow3.getChild(OpProjectAdministrationService.PERIOD_START_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ((XComponent)periodRow3.getChild(OpProjectAdministrationService.PERIOD_END_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));

      response = service.haveAssignmentsInTimePeriod(session, request);
      assertTrue(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //delete the second period of resource one this will not affect any of it's activities - return false
      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ((XComponent)periodRow3.getChild(OpProjectAdministrationService.PERIOD_START_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ((XComponent)periodRow3.getChild(OpProjectAdministrationService.PERIOD_END_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));

      newDataSet.removeAllChildren();
      newDataSet.addChild(dataRowRes1);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(dataRowRes2);
      newDataSet.addChild(periodRow3);

      response = service.haveAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //insert a new period for resource two from 3/22/2006 - 3/26/2006 this will not affect it's activitie - return false
      XComponent periodRow4 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      periodRow4.setOutlineLevel(1);
      periodRow4.addChild(dataCell);
      periodRow4.addChild(dataCell);
      periodRow4.addChild(dataCell);
      periodRow4.addChild(dataCell);
      periodRow4.addChild(dataCell);
      calendar.set(2006, 3, 22, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow4.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 26, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      periodRow4.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(19d);
      periodRow4.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(19d);
      periodRow4.addChild(dataCell);

      newDataSet.removeAllChildren();
      newDataSet.addChild(dataRowRes1);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      newDataSet.addChild(periodRow3);
      newDataSet.addChild(periodRow4);
      
      response = service.haveAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //extend second period of resource one to 3/24/2006 - 3/30/206 - this will affect it's activities - return true
      calendar.set(2006, 3, 24, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ((XComponent)periodRow2.getChild(OpProjectAdministrationService.PERIOD_START_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ((XComponent)periodRow2.getChild(OpProjectAdministrationService.PERIOD_END_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));

      newDataSet.removeAllChildren();
      newDataSet.addChild(dataRowRes1);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      newDataSet.addChild(periodRow3);

      response = service.haveAssignmentsInTimePeriod(session, request);
      assertTrue(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //delete the first period of resource one - this will affect it's activities - return true
      calendar.set(2006, 3, 29, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ((XComponent)periodRow2.getChild(OpProjectAdministrationService.PERIOD_START_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      ((XComponent)periodRow2.getChild(OpProjectAdministrationService.PERIOD_END_DATE)).setDateValue(new Date(calendar.getTimeInMillis()));

      newDataSet.removeAllChildren();
      newDataSet.addChild(dataRowRes1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      newDataSet.addChild(periodRow3);

      response = service.haveAssignmentsInTimePeriod(session, request);
      assertTrue(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //change the rate of the first period of resource one - this will affect it's activities - return true
      ((XComponent)periodRow1.getChild(OpProjectAdministrationService.INTERNAL_PERIOD_RATE_COLUMN_INDEX)).setDoubleValue(10d);
      ((XComponent)periodRow1.getChild(OpProjectAdministrationService.EXTERNAL_PERIOD_RATE_COLUMN_INDEX)).setDoubleValue(4d);

      newDataSet.removeAllChildren();
      newDataSet.addChild(dataRowRes1);
      newDataSet.addChild(periodRow1);
      newDataSet.addChild(periodRow2);
      newDataSet.addChild(dataRowRes2);
      newDataSet.addChild(periodRow3);

      response = service.haveAssignmentsInTimePeriod(session, request);
      assertTrue(((Boolean) response.getArgument(OpProjectAdministrationService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());
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
      activity.setName("Standard Activity");
      activity.setSequence(0);
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
      activity.setName("Task Activity");
      activity.setSequence(1);
      activity.setProjectPlan(plan);
      activity.setComplete(80d);
      activity.setTemplate(false);
      activity.setAttachments(new HashSet());
      activity.setAssignments(new HashSet());
      broker.makePersistent(activity);

      assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resourceDataFactory.getResourceById(resId2));
      broker.makePersistent(assignment);

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
      XComponent resources1 = new XComponent(XComponent.DATA_SET);
      resources1.addChild(dataRowRes1);

      XComponent resources2 = new XComponent(XComponent.DATA_SET);
      resources2.addChild(dataRowRes1);
      resources2.addChild(dataRowRes2);

      XComponent permissions = TestDataFactory.createPermissionSet(OpPermission.ADMINISTRATOR, adminId, OpUser.ADMINISTRATOR_NAME);
      XMessage request = ProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(1), null, 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, new XComponent(XComponent.DATA_SET), null, null, permissions);
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
