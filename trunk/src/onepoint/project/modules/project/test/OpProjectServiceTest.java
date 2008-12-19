/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project.test;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityFilter;
import onepoint.project.modules.project.OpActivityVersionDataSetFactory;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectError;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.project.modules.project.OpWorkPeriod;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_status.OpProjectStatusService;
import onepoint.project.modules.project_status.test.OpProjectStatusTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;

/**
 * This class test project service methods.
 *
 * @author lucian.furtos
 */
public class OpProjectServiceTest extends OpBaseOpenTestCase {

   private static final String PRJ_NAME = "prj";
   private static final String PORTOFOLIO_NAME = "portofolio";
   private static final String STATUS_NAME = "prj_status";
   private static final String DESCRIPTION = "A test project status";
   private static final String PROJECT_STATUS_DATA = "project_status_data";

   private static final String RES_NAME = "res";
   private static final String RES_DESCR = "descr";

   private static final String USER_NAME = "userName";
   private static final String USER_PASSWORD = "userPassword";

   private OpProjectStatusService projectStatusService;
   private OpProjectStatusTestDataFactory projectStatusDataFactory;
   private OpProjectAdministrationService service;
   private OpProjectTestDataFactory dataFactory;
   private OpResourceTestDataFactory resourceDataFactory;
   private String resId1;
   private String resId2;
   private XComponent dataRowRes1;
   private XComponent dataRowRes2;
   private OpUserTestDataFactory userDataFactory;
   private OpUserService userService;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      userService = OpTestDataFactory.getUserService();
      userDataFactory = new OpUserTestDataFactory(session);
      projectStatusService = OpTestDataFactory.getProjectStatusService();
      projectStatusDataFactory = new OpProjectStatusTestDataFactory(session);
      service = OpTestDataFactory.getProjectService();
      dataFactory = new OpProjectTestDataFactory(session);
      resourceDataFactory = new OpResourceTestDataFactory(session);

      // create resources
      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = resourceDataFactory.createResourceMsg(RES_NAME + 1, RES_DESCR, 50d, 2d, 2d, false, poolid);
      XMessage response = OpTestDataFactory.getResourceService().insertResource(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      try {
        OpResource  resource1 = resourceDataFactory.getResourceByName(RES_NAME + 1);
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
         response = OpTestDataFactory.getResourceService().insertResource(session, request);
         assertNoError(response);
         OpResource resource2 = resourceDataFactory.getResourceByName(RES_NAME + 2);
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

   /**
    * Test happy flow for insert portofolio and possible errors
    *
    * @throws Exception if the tst fails
    */
   public void testInsertPortofolio()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createPortofolioMsg(null, null, null, null);
      XMessage response = service.insertPortfolio(session, request);
      assertError(response, OpProjectError.PORTFOLIO_NAME_MISSING);

      request = OpProjectTestDataFactory.createPortofolioMsg("", null, null, null);
      response = service.insertPortfolio(session, request);
      assertError(response, OpProjectError.PORTFOLIO_NAME_MISSING);

      request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      response = service.insertPortfolio(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      try {
         assertNotNull(dataFactory.getPortofolioByName(PORTOFOLIO_NAME));

         request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, null, null, null);
         response = service.insertPortfolio(session, request);
         assertError(response, OpProjectError.PORTFOLIO_NAME_ALREADY_USED);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Test happy flow for update portofolio
    *
    * @throws Exception if the tst fails
    */
   public void testUpdatePortofolio()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      XMessage response = service.insertPortfolio(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      try {
         OpProjectNode portfolio = dataFactory.getPortofolioByName(PORTOFOLIO_NAME);
         assertNotNull(portfolio);
         String id = portfolio.locator();

         request = OpProjectTestDataFactory.updatePortofolioMsg(id, "New" + PORTOFOLIO_NAME, "new description", null);
         response = service.updatePortfolio(session, request);
         assertNoError(response);

         broker.clear();
         portfolio = dataFactory.getPortofolioById(id);
         assertNotNull(portfolio);
         assertEquals("New" + PORTOFOLIO_NAME, portfolio.getName());
         assertEquals("new description", portfolio.getDescription());
      }
      finally {
         broker.close();
      }
   }

   /**
    * Test errors for update portofolio
    *
    * @throws Exception if the tst fails
    */
   public void testUpdatePortofolioErrors()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      XMessage response = service.insertPortfolio(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      try {
         String id = dataFactory.getPortofolioId(PORTOFOLIO_NAME);

         request = OpProjectTestDataFactory.updatePortofolioMsg(id, null, null, null);
         response = service.updatePortfolio(session, request);
         assertError(response, OpProjectError.PORTFOLIO_NAME_MISSING);

         String fakeid = OpLocator.locatorString(OpProjectNode.PROJECT_NODE, 0);
         request = OpProjectTestDataFactory.updatePortofolioMsg(fakeid, PORTOFOLIO_NAME, null, null);
         response = service.updatePortfolio(session, request);
         assertError(response, OpProjectError.PROJECT_NOT_FOUND);

         request = OpProjectTestDataFactory.createPortofolioMsg("PortofolioToUpdate", "portofolio description", null, null);
         response = service.insertPortfolio(session, request);
         assertNoError(response);
         id = dataFactory.getPortofolioId("PortofolioToUpdate");

         request = OpProjectTestDataFactory.updatePortofolioMsg(id, PORTOFOLIO_NAME, null, null);
         response = service.updatePortfolio(session, request);
         assertError(response, OpProjectError.PORTFOLIO_NAME_ALREADY_USED);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Test happy flow for delete portofolio
    *
    * @throws Exception if the tst fails
    */
   public void testDeletePortofolio()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      XMessage response = service.insertPortfolio(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      try {
         OpProjectNode portfolio = dataFactory.getPortofolioByName(PORTOFOLIO_NAME);
         assertNotNull(portfolio);
         String id = portfolio.locator();

         request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME + 1, "portofolio description", id, null);
         response = service.insertPortfolio(session, request);
         assertNoError(response);

         request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME + 2, "portofolio description", id, null);
         response = service.insertPortfolio(session, request);
         assertNoError(response);

         List<String> ids = new ArrayList<String>();
         List portofolios = dataFactory.getAllPortofolios(session);
         assertEquals(4, portofolios.size());

         for (int i = 0; i < portofolios.size(); i++) {
            OpProjectNode p = (OpProjectNode) portofolios.get(i);
            if (!p.getName().equals(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)) {
               ids.add(p.locator());
            }
         }
         broker.close();

         request = OpProjectTestDataFactory.deletePortofolioMsg(ids);
         response = service.deletePortfolios(session, request);
         assertNoError(response);

         assertEquals(1, dataFactory.getAllPortofolios(session).size());
      }
      finally {
         broker.close();
      }
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

      Date date = Date.valueOf("2007-06-06");
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, date, 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      try {
         OpProjectNode project = dataFactory.getProjectByName(PRJ_NAME);
         assertEquals(date, project.getStart());
      }
      finally {
         broker.close();
      }
   }

   public void testInsertProjectWithBudgetSetError()
        throws Exception {
      XMessage request;
      XMessage response;

      // create a user
      createUser(USER_NAME, USER_PASSWORD);
      OpBroker broker = session.newBroker();
      try {
         OpUser user = userDataFactory.getUserByName(USER_NAME);
         assertNotNull(user);

         // create a portfolio
         XComponent set = new XComponent(XComponent.DATA_SET);
         XComponent permissionDataRow = new XComponent(XComponent.DATA_ROW);
         XComponent userDataRow = new XComponent(XComponent.DATA_ROW);
         XComponent permissionDataCell = new XComponent(XComponent.DATA_CELL);

         permissionDataCell.setByteValue(OpPermission.MANAGER);
         permissionDataRow.addChild(permissionDataCell);
         permissionDataRow.setOutlineLevel(0);

         userDataRow.setStringValue(OpLocator.locatorString(user));
         userDataRow.setOutlineLevel(1);

         set.addChild(permissionDataRow);
         set.addChild(userDataRow);

         request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, set);
         response = service.insertPortfolio(session, request);
         assertNoError(response);
         OpProjectNode portfolio = dataFactory.getPortofolioByName(PORTOFOLIO_NAME);
         assertNotNull(portfolio);

         // signOff the administrator
         response = OpTestDataFactory.getUserService().signOff(session, new XMessage());
         assertNoError(response);

         // signOn the user
         request = new XMessage();
         request.setArgument(OpUserService.LOGIN, USER_NAME);
         request.setArgument(OpUserService.PASSWORD, USER_PASSWORD);
         response = OpTestDataFactory.getUserService().signOn(session, request);
         assertNoError(response);

         // creates a project
         Date date = Date.valueOf("2007-06-06");
         request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, date, 1000d, null,
              OpLocator.locatorString(portfolio), Boolean.FALSE, Boolean.TRUE, null, null);

         response = service.insertProject(session, request);
         assertError(response, OpProjectError.NO_RIGHTS_CHANGING_BUDGET_ERROR);
      }
      finally {
         broker.close();
      }
   }

   public void testInsertProjectWithStatusSetError()
        throws Exception {
      XMessage request;
      XMessage response;

      // create a user
      createUser(USER_NAME, USER_PASSWORD);
      OpBroker broker = session.newBroker();
      try {
         OpUser user = userDataFactory.getUserByName(USER_NAME);
         assertNotNull(user);

         // create a portfolio
         XComponent set = new XComponent(XComponent.DATA_SET);
         XComponent permissionDataRow = new XComponent(XComponent.DATA_ROW);
         XComponent userDataRow = new XComponent(XComponent.DATA_ROW);
         XComponent permissionDataCell = new XComponent(XComponent.DATA_CELL);

         permissionDataCell.setByteValue(OpPermission.MANAGER);
         permissionDataRow.addChild(permissionDataCell);
         permissionDataRow.setOutlineLevel(0);

         userDataRow.setStringValue(OpLocator.locatorString(user));
         userDataRow.setOutlineLevel(1);

         set.addChild(permissionDataRow);
         set.addChild(userDataRow);

         request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, set);
         response = service.insertPortfolio(session, request);
         assertNoError(response);
         OpProjectNode portfolio = dataFactory.getPortofolioByName(PORTOFOLIO_NAME);
         assertNotNull(portfolio);

         // creates a project status
         Date date = Date.valueOf("2007-06-06");
         Map<String, Object> statusData = new HashMap<String, Object>();
         statusData.put(OpProjectStatus.NAME, STATUS_NAME);
         statusData.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
         statusData.put(OpProjectStatus.COLOR, new Integer(10));

         request = new XMessage();
         request.setArgument(PROJECT_STATUS_DATA, statusData);
         response = projectStatusService.insertProjectStatus(session, request);

         assertNoError(response);

         // signOff the administrator
         response = OpTestDataFactory.getUserService().signOff(session, new XMessage());
         assertNoError(response);

         // signOn the user
         request = new XMessage();
         request.setArgument(OpUserService.LOGIN, USER_NAME);
         request.setArgument(OpUserService.PASSWORD, USER_PASSWORD);
         response = OpTestDataFactory.getUserService().signOn(session, request);
         assertNoError(response);

         OpProjectStatus status = projectStatusDataFactory.getProjectStatusByName(STATUS_NAME);
         assertNotNull(status);

         request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, date, 0d, OpLocator.locatorString(status),
              OpLocator.locatorString(portfolio), Boolean.FALSE, Boolean.TRUE, null, null);

         response = service.insertProject(session, request);
         assertError(response, OpProjectError.NO_RIGHTS_CHANGING_STATUS_ERROR);
      }
      finally {
         broker.close();
      }

   }

   public void testInsertProjectWithBudgetSet()
        throws Exception {
      XMessage request;
      XMessage response;

      // create a user
      createUser(USER_NAME, USER_PASSWORD);
      OpBroker broker = session.newBroker();
      try {
         OpUser user = userDataFactory.getUserByName(USER_NAME);
         assertNotNull(user);

         // create a portfolio
         XComponent set = new XComponent(XComponent.DATA_SET);
         XComponent permissionDataRow = new XComponent(XComponent.DATA_ROW);
         XComponent userDataRow = new XComponent(XComponent.DATA_ROW);
         XComponent permissionDataCell = new XComponent(XComponent.DATA_CELL);

         permissionDataCell.setByteValue(OpPermission.ADMINISTRATOR);
         permissionDataRow.addChild(permissionDataCell);
         permissionDataRow.setOutlineLevel(0);

         userDataRow.setStringValue(OpLocator.locatorString(user));
         userDataRow.setOutlineLevel(1);

         set.addChild(permissionDataRow);
         set.addChild(userDataRow);

         request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, set);
         response = service.insertPortfolio(session, request);
         assertNoError(response);
         OpProjectNode portfolio = dataFactory.getPortofolioByName(PORTOFOLIO_NAME);
         assertNotNull(portfolio);

         // signOff the administrator
         response = OpTestDataFactory.getUserService().signOff(session, new XMessage());
         assertNoError(response);

         // signOn the user
         request = new XMessage();
         request.setArgument(OpUserService.LOGIN, USER_NAME);
         request.setArgument(OpUserService.PASSWORD, USER_PASSWORD);
         response = OpTestDataFactory.getUserService().signOn(session, request);
         assertNoError(response);

         // creates a project
         Date date = Date.valueOf("2007-06-06");
         request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, date, 1000d, null,
              OpLocator.locatorString(portfolio), Boolean.FALSE, Boolean.FALSE, null, null);

         response = service.insertProject(session, request);
         assertNoError(response);
      }
      finally {
         broker.close();
      }
   }

   public void testInsertProjectWithStatusSet()
        throws Exception {
      XMessage request;
      XMessage response;

      // create a user
      createUser(USER_NAME, USER_PASSWORD);
      OpBroker broker = session.newBroker();
      try {
         OpUser user = userDataFactory.getUserByName(USER_NAME);
         assertNotNull(user);

         // create a portfolio
         XComponent set = new XComponent(XComponent.DATA_SET);
         XComponent permissionDataRow = new XComponent(XComponent.DATA_ROW);
         XComponent userDataRow = new XComponent(XComponent.DATA_ROW);
         XComponent permissionDataCell = new XComponent(XComponent.DATA_CELL);

         permissionDataCell.setByteValue(OpPermission.ADMINISTRATOR);
         permissionDataRow.addChild(permissionDataCell);
         permissionDataRow.setOutlineLevel(0);

         userDataRow.setStringValue(OpLocator.locatorString(user));
         userDataRow.setOutlineLevel(1);

         set.addChild(permissionDataRow);
         set.addChild(userDataRow);

         request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, set);
         response = service.insertPortfolio(session, request);
         assertNoError(response);
         OpProjectNode portfolio = dataFactory.getPortofolioByName(PORTOFOLIO_NAME);
         assertNotNull(portfolio);

         // creates a project status
         Date date = Date.valueOf("2007-06-06");
         Map<String, Object> statusData = new HashMap<String, Object>();
         statusData.put(OpProjectStatus.NAME, STATUS_NAME);
         statusData.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
         statusData.put(OpProjectStatus.COLOR, new Integer(10));

         request = new XMessage();
         request.setArgument(PROJECT_STATUS_DATA, statusData);
         response = projectStatusService.insertProjectStatus(session, request);

         assertNoError(response);

         // signOff the administrator
         response = OpTestDataFactory.getUserService().signOff(session, new XMessage());
         assertNoError(response);

         // signOn the user
         request = new XMessage();
         request.setArgument(OpUserService.LOGIN, USER_NAME);
         request.setArgument(OpUserService.PASSWORD, USER_PASSWORD);
         response = OpTestDataFactory.getUserService().signOn(session, request);
         assertNoError(response);

         OpProjectStatus status = projectStatusDataFactory.getProjectStatusByName(STATUS_NAME);
         assertNotNull(status);

         request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, date, 0d, OpLocator.locatorString(status),
              OpLocator.locatorString(portfolio), Boolean.FALSE, Boolean.TRUE, null, null);

         response = service.insertProject(session, request);
         assertNoError(response);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Test project creation errors
    *
    * @throws Exception if the test fails
    */
   public void testInsertProjectErrors()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createProjectMsg(null, null, 0d, null, null);
      XMessage response = service.insertProject(session, request);
      assertError(response, OpProjectError.PROJECT_NAME_MISSING);

      request = OpProjectTestDataFactory.createProjectMsg("", null, 0d, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.PROJECT_NAME_MISSING);

      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, null, 0d, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.START_DATE_MISSING);

      long time = System.currentTimeMillis();
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(time), new Date(time - 1), 0d, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.END_DATE_INCORRECT);

      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(time), new Date(time + 1000), -1d, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.BUDGET_INCORRECT);

      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(time), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(time), new Date(time + 1000), 1d, null, null);
      response = service.insertProject(session, request);
      assertError(response, OpProjectError.PROJECT_NAME_ALREADY_USED);
   }

   public void testUpdateProjectError()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      try {
         String goodId = dataFactory.getProjectId(PRJ_NAME + 1);

         request = OpProjectTestDataFactory.updateProjectMsg(goodId, null, null, null, 0d, null, null, null, null, null, null, false);
         response = service.updateProject(session, request);
         assertError(response, OpProjectError.PROJECT_NAME_MISSING);

         request = OpProjectTestDataFactory.updateProjectMsg(goodId, "", null, null, 0d, null, null, null, null, null, null, false);
         response = service.updateProject(session, request);
         assertError(response, OpProjectError.PROJECT_NAME_MISSING);

         request = OpProjectTestDataFactory.updateProjectMsg(goodId, PRJ_NAME, null, null, 0d, null, null, null, null, null, null, false);
         response = service.updateProject(session, request);
         assertError(response, OpProjectError.START_DATE_MISSING);

         request = OpProjectTestDataFactory.updateProjectMsg(goodId, PRJ_NAME, new Date(System.currentTimeMillis()),
              new Date(System.currentTimeMillis() - 1000), 0d, null, null, null, null, null, null, false);
         response = service.updateProject(session, request);
         assertError(response, OpProjectError.END_DATE_INCORRECT);

         request = OpProjectTestDataFactory.updateProjectMsg(goodId, PRJ_NAME, new Date(System.currentTimeMillis()),
              new Date(System.currentTimeMillis() + 1000), -1d, null, null, null, null, null, null, false);
         response = service.updateProject(session, request);
         assertError(response, OpProjectError.BUDGET_INCORRECT);

         String id = OpLocator.locatorString(OpProjectNode.PROJECT_NODE, -1);
         request = OpProjectTestDataFactory.updateProjectMsg(id, PRJ_NAME, new Date(System.currentTimeMillis()),
              new Date(System.currentTimeMillis() + 1000), 1d, null, null, null, null, null, null, false);
         response = service.updateProject(session, request);
         assertError(response, OpProjectError.PROJECT_NOT_FOUND);

         request = OpProjectTestDataFactory.updateProjectMsg(goodId, PRJ_NAME, new Date(System.currentTimeMillis()),
              new Date(System.currentTimeMillis() + 1000), 1d, null, null, null, null, null, null, false);
         response = service.updateProject(session, request);
         assertError(response, OpProjectError.PROJECT_NAME_ALREADY_USED);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Test project update
    * When we update a project and we assign it another status, the old status of the project will be remove
    * from the data base if it is no longer refred by any other project
    *
    * @throws Exception if the tst fails
    */
   public void testUpdateProjectChangeStatus()
        throws Exception {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction t = null;

         //create the projectStatus
         Map<String, Object> args = new HashMap<String, Object>();
         args.put(OpProjectStatus.NAME, STATUS_NAME);
         args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
         args.put(OpProjectStatus.COLOR, new Integer(7));
         XMessage request = new XMessage();
         request.setArgument("project_status_data", args);
         XMessage response = projectStatusService.insertProjectStatus(session, request);

         assertNoError(response);

         Long statusId = projectStatusDataFactory.getProjectStatusId(STATUS_NAME);
         String statusLocator = OpLocator.locatorString(OpProjectStatus.PROJECT_STATUS, statusId);
         OpProjectStatus projectStatus = (OpProjectStatus) broker.getObject(statusLocator);

         assertNotNull(projectStatus);
         assertEquals(DESCRIPTION, projectStatus.getDescription());
         assertEquals(7, projectStatus.getColor());

         //create the first project
         Date date = Date.valueOf("2007-06-06");
         request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, date, 100d, null, null);
         response = service.insertProject(session, request);

         assertNoError(response);

         String projectLocator = dataFactory.getProjectId(PRJ_NAME);
         OpProjectNode project = (OpProjectNode) broker.getObject(projectLocator);

         assertEquals(date, project.getStart());

         //create the second project
         request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, date, 100d, null, null);
         response = service.insertProject(session, request);

         assertNoError(response);

         String secondProjectLocator = dataFactory.getProjectId(PRJ_NAME + 1);
         OpProjectNode secondProject = (OpProjectNode) broker.getObject(secondProjectLocator);

         assertEquals(date, secondProject.getStart());

         //assign the projectStatus to projects
         project.setStatus(projectStatus);
         secondProject.setStatus(projectStatus);
         t = broker.newTransaction();
         broker.updateObject(project);
         broker.updateObject(secondProject);
         t.commit();

         //delete the projectStatus
         request = new XMessage();
         request.setArgument("project_status_ids", Arrays.asList(statusLocator));
         response = projectStatusService.deleteProjectStatus(session, request);

         assertNoError(response);

         //update project to another project status
         XComponent emptyDataSet = new XComponent(XComponent.DATA_SET);
         request = OpProjectTestDataFactory.updateProjectMsg(projectLocator, project.getName(), project.getStart(),
              null, project.getBudget(), null, null, null, null, emptyDataSet, emptyDataSet, false);
         response = service.updateProject(session, request);
         assertNoError(response);

         projectStatus = (OpProjectStatus) broker.getObject(statusLocator);
         assertNotNull(projectStatus);

         //update secondProject to another project status
         request = OpProjectTestDataFactory.updateProjectMsg(secondProjectLocator, secondProject.getName(), secondProject.getStart(),
              null, secondProject.getBudget(), null, null, null, null, emptyDataSet, emptyDataSet, false);
         response = service.updateProject(session, request);
         assertNoError(response);

         broker.clear();

         projectStatus = (OpProjectStatus) broker.getObject(statusLocator);
         assertNull(projectStatus);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Test happy flow for project delete
    *
    * @throws Exception if the tst fails
    */
   public void testDeleteProject()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 12d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 23d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 3, new Date(System.currentTimeMillis()), 645d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      List<String> ids = new ArrayList<String>();
      OpBroker broker = session.newBroker();
      try {
         List projects = dataFactory.getAllProjects(session);
         broker.close();
         assertEquals(3, projects.size());
         for (int i = 0; i < projects.size(); i++) {
            OpProjectNode p = (OpProjectNode) projects.get(i);
            ids.add(p.locator());
         }

         request = OpProjectTestDataFactory.deleteProjectMsg(ids);
         response = service.deleteProjects(session, request);
         assertNoError(response);

         broker.clear();
         assertEquals(0, dataFactory.getAllProjects(session).size());
      }
      finally {
         broker.close();
      }
   }

   public void testExpandNode()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      //todo: create portofolio

      OpBroker broker = session.newBroker();
      try {
         String id = OpProjectAdministrationService.findRootPortfolio(broker).locator();
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
      finally {
         broker.close();
      }
   }

   public void testExpandNodeForView()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      try {
         String id = OpProjectAdministrationService.findRootPortfolio(broker).locator();
         List<String> ids = new ArrayList<String>();
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
      finally {
         broker.close();
      }
   }

   public void testGetRootHierarchy()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      //todo: create portofolio
      List<String> ids = new ArrayList<String>();
      ids.add(dataFactory.getProjectId(PRJ_NAME + 1));
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpProjectDataSetFactory.retrieveProjectDataSetRootHierarchy(session, dataSet, OpProjectDataSetFactory.ALL_TYPES, true, ids);
      assertEquals(2, dataSet.getChildCount());
   }

   public void testRetrieveActivityDataSet()
        throws Exception {
      long time = System.currentTimeMillis();
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 1000d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      try {
         String planId = dataFactory.getProjectByName(PRJ_NAME + 1).getPlan().locator();
         OpProjectPlan plan = dataFactory.getProjectPlanById(planId);

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
         broker.makePersistent(activity);

         OpAssignment assignment = new OpAssignment();
         activity.addAssignment(assignment);
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
         broker.makePersistent(activity);

         assignment = new OpAssignment();
         activity.addAssignment(assignment);
         assignment.setResource(resourceDataFactory.getResourceById(resId2));
         broker.makePersistent(assignment);

         t.commit();

         plan = (OpProjectPlan) broker.getObject(plan.locator());

         XComponent dataSet = new XComponent(XComponent.DATA_SET);
         OpActivityVersionDataSetFactory.getInstance().retrieveActivityVersionDataSet(session, broker, plan.getLatestVersion(), dataSet, false);
         assertEquals(2, dataSet.getChildCount());

         OpActivityFilter filter = new OpActivityFilter();
         filter.addProjectNodeID(dataFactory.getProjectByName(PRJ_NAME + 1).getId());
         filter.addResourceID(resourceDataFactory.getResourceById(resId1).getId());
         filter.addResourceID(resourceDataFactory.getResourceById(resId2).getId());
         filter.addType(OpActivity.TASK);
         filter.setStartFrom(new Date(time));
         filter.setStartTo(new Date(time + 10000));
         filter.setCompleted(Boolean.FALSE);
         filter.setTemplates(false);

         OpObjectOrderCriteria order = new OpObjectOrderCriteria(OpProjectNode.class, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);

         dataSet = new XComponent(XComponent.DATA_SET);
         OpActivityDataSetFactory.getInstance().retrieveFilteredActivityDataSet(session, broker, filter, order, dataSet);
         assertEquals(1, dataSet.getChildCount());
      }
      finally {
         broker.close();
      }
   }

   public void testCountProjects()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(1), 1d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(1), 1d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      try {
         assertEquals(1, OpProjectDataSetFactory.countProjectNode(OpProjectNode.PORTFOLIO, broker));
         assertEquals(2, OpProjectDataSetFactory.countProjectNode(OpProjectNode.PROJECT, session.newBroker()));
         assertEquals(0, OpProjectDataSetFactory.countProjectNode(OpProjectNode.TEMPLATE, session.newBroker()));
      }
      finally {
         broker.close();
      }
   }

   public void testProjectAndResourcesMapping()
        throws Exception {
      XComponent resources1 = new XComponent(XComponent.DATA_SET);
      resources1.addChild(dataRowRes1);

      XComponent resources2 = new XComponent(XComponent.DATA_SET);
      resources2.addChild(dataRowRes1.copyData());
      resources2.addChild(dataRowRes2);

      XComponent permissions = OpTestDataFactory.createPermissionSet(OpPermission.ADMINISTRATOR, adminId, OpUser.ADMINISTRATOR_NAME);
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME, new Date(1), null, 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, new XComponent(XComponent.DATA_SET), null, permissions);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      String id = dataFactory.getProjectId(PRJ_NAME);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(1), null, 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources1, null, permissions);
      response = service.insertProject(session, request);
      assertNoError(response);
      String id1 = dataFactory.getProjectId(PRJ_NAME + 1);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(1), null, 1000d, null, null,
           Boolean.FALSE, Boolean.TRUE, resources2, null, permissions);
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
      XMessage request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 12d, null, null);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 23d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 3, new Date(System.currentTimeMillis()), 645d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);

      List<String> ids = new ArrayList<String>();
      OpBroker broker = session.newBroker();
      try {
         List projects = dataFactory.getAllProjects(session);
         assertEquals(3, projects.size());
         for (int i = 0; i < projects.size(); i++) {
            OpProjectNode p = (OpProjectNode) projects.get(i);
            ids.add(p.locator());
         }
         broker.clear();

         request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
         response = service.insertPortfolio(session, request);
         assertNoError(response);
         broker.clear();
         OpProjectNode portofolio = dataFactory.getPortofolioByName(PORTOFOLIO_NAME);
         assertTrue(portofolio.getSubNodes().isEmpty());

         request = OpProjectTestDataFactory.moveProjectsMsg(portofolio.locator(), ids);
         broker.clear();
         response = service.moveProjectNode(session, request);
         assertNoError(response);

         broker.clear();
         portofolio = dataFactory.getPortofolioById(portofolio.locator());
         assertEquals(3, portofolio.getSubNodes().size());
      }
      finally {
         broker.close();
      }
   }

   // ********* Test DataSet Factories **********

   public void testRetrieveProjectDataSet()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createPortofolioMsg(PORTOFOLIO_NAME, "portofolio description", null, null);
      XMessage response = service.insertPortfolio(session, request);
      assertNoError(response);
      OpBroker broker = session.newBroker();
      try {
         String id = dataFactory.getPortofolioId(PORTOFOLIO_NAME);
         request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 1, new Date(System.currentTimeMillis()), 23d, null, id);
         response = service.insertProject(session, request);
         assertNoError(response);
         request = OpProjectTestDataFactory.createProjectMsg(PRJ_NAME + 2, new Date(System.currentTimeMillis()), 645d, null, null);
         response = service.insertProject(session, request);
         assertNoError(response);

         XComponent dataSet = new XComponent(XComponent.DATA_SET);
         OpProjectDataSetFactory.retrieveProjectDataSet(session, broker, dataSet, OpProjectDataSetFactory.ALL_TYPES, true);
         assertEquals(4, dataSet.getChildCount());
      }
      finally {
         broker.close();
      }
   }

   // ******** Helper Methods *********

   private void createUser(String userName, String password) {
      XMessage request = new XMessage();
      XMessage response;

      //create user
      Map userData = OpUserTestDataFactory.createUserData(userName, password, OpUser.MANAGER_USER_LEVEL);

      request.setArgument(OpUserService.USER_DATA, userData);
      response = userService.insertUser(session, request);
      assertNoError(response);
   }

}
