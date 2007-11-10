/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_status.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.project_status.OpProjectStatusError;
import onepoint.project.modules.project_status.OpProjectStatusService;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class test project status service methods and form providers.
 *
 * @author lucian.furtos
 */
public class OpProjectStatusServiceTest extends OpBaseOpenTestCase {

   private static final String NAME = "prj_status";
   private static final String NEW_NAME = "new_prj_status";
   private static final String DESCRIPTION = "A test project status";
   private static final String NEW_DESCRIPTION = "The new test project status";

   private static final String DEFAULT_USER = "tester";
   private static final String DEFAULT_PASSWORD = "pass";

   private OpProjectStatusService service;
   private OpProjectStatusTestDataFactory dataFactory;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();
      service = OpTestDataFactory.getProjectStatusService();
      dataFactory = new OpProjectStatusTestDataFactory(session);
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
    * Test the creation of a project status
    *
    * @throws Exception if the test fails
    */
   public void testCreateProjectStatus()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, NAME);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_data", args);
      XMessage response = service.insertProjectStatus(session, request);

      assertNoError(response);

      OpProjectStatus status = dataFactory.getProjectStatusByName(NAME);
      assertNotNull(status);
      assertEquals(DESCRIPTION, status.getDescription());
      assertEquals(7, status.getColor());
   }

   /**
    * Test the invalid name
    *
    * @throws Exception if the test fails
    */
   public void testInvalidName()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, null);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_data", new HashMap(args));
      XMessage response = service.insertProjectStatus(session, request);
      assertError(response, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_SPECIFIED);

      args.put(OpProjectStatus.NAME, "");

      request = new XMessage();
      request.setArgument("project_status_data", new HashMap(args));
      response = service.insertProjectStatus(session, request);
      assertError(response, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_SPECIFIED);
   }

   /**
    * Test duplicate name
    *
    * @throws Exception if the test fails
    */
   public void testDuplicateName()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, NAME);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_data", new HashMap(args));
      XMessage response = service.insertProjectStatus(session, request);
      assertNoError(response);

      request = new XMessage();
      request.setArgument("project_status_data", new HashMap(args));
      response = service.insertProjectStatus(session, request);
      assertError(response, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_UNIQUE);
   }

   /**
    * Test update project status
    *
    * @throws Exception if the test fails
    */
   public void testUpdateProjectStatus()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, NAME);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_data", new HashMap(args));
      XMessage response = service.insertProjectStatus(session, request);
      assertNoError(response);

      OpProjectStatus status = dataFactory.getProjectStatusByName(NAME);
      String id = status.locator();

      args.put(OpProjectStatus.NAME, NEW_NAME);
      args.put(OpProjectStatus.DESCRIPTION, NEW_DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(3));

      request = new XMessage();
      request.setArgument("project_status_id", id);
      request.setArgument("project_status_data", new HashMap(args));
      response = service.updateProjectStatus(session, request);
      assertNoError(response);

      status = dataFactory.getProjectStatusById(id);

      assertEquals(NEW_NAME, status.getName());
      assertEquals(NEW_DESCRIPTION, status.getDescription());
      assertEquals(3, status.getColor());
   }

   /**
    * Test update project status that not exist
    *
    * @throws Exception if the test fails
    */
   public void testUpdateNotExist()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, NAME);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_id", OpLocator.locatorString(OpProjectStatus.PROJECT_STATUS, 100));
      request.setArgument("project_status_data", new HashMap(args));
      XMessage response = service.updateProjectStatus(session, request);
      assertError(response, OpProjectStatusError.PROJECT_STATUS_NOT_FOUND);
   }

   /**
    * Test update project status
    *
    * @throws Exception if the test fails
    */
   public void testUpdateNoName()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, NAME);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_data", new HashMap(args));
      XMessage response = service.insertProjectStatus(session, request);
      assertNoError(response);

      OpProjectStatus status = dataFactory.getProjectStatusByName(NAME);
      String id = status.locator();

      args.put(OpProjectStatus.NAME, null);
      args.put(OpProjectStatus.DESCRIPTION, NEW_DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(3));

      request = new XMessage();
      request.setArgument("project_status_id", id);
      request.setArgument("project_status_data", new HashMap(args));
      response = service.updateProjectStatus(session, request);
      assertError(response, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_SPECIFIED);

      args.put(OpProjectStatus.NAME, "");

      request = new XMessage();
      request.setArgument("project_status_id", id);
      request.setArgument("project_status_data", new HashMap(args));
      response = service.updateProjectStatus(session, request);
      assertError(response, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_SPECIFIED);
   }

   /**
    * Test update project status duplicate name error
    *
    * @throws Exception if the test fails
    */
   public void testUpdateDuplicate()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, NAME + 1);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_data", args);
      XMessage response = service.insertProjectStatus(session, request);
      assertNoError(response);

      args.put(OpProjectStatus.NAME, NAME + 2);
      response = service.insertProjectStatus(session, request);
      assertNoError(response);

      OpProjectStatus status1 = dataFactory.getProjectStatusByName(NAME + 1);

      args.put(OpProjectStatus.NAME, NAME + 2);
      args.put(OpProjectStatus.DESCRIPTION, NEW_DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(3));

      request = new XMessage();
      request.setArgument("project_status_id", status1.locator());
      request.setArgument("project_status_data", new HashMap(args));
      response = service.updateProjectStatus(session, request);
      assertError(response, OpProjectStatusError.PROJECT_STATUS_NAME_NOT_UNIQUE);
   }

   /**
    * Test the delete of a project status
    *
    * @throws Exception if the test fails
    */
   public void testDeleteProjectStatus()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, NAME + 1);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_data", args);
      XMessage response = service.insertProjectStatus(session, request);
      assertNoError(response);

      args.put(OpProjectStatus.NAME, NAME + 2);
      response = service.insertProjectStatus(session, request);
      assertNoError(response);

      args.put(OpProjectStatus.NAME, NAME + 3);
      response = service.insertProjectStatus(session, request);
      assertNoError(response);

      List statusList = dataFactory.getAllProjectsStatus();
      assertEquals(3, statusList.size());
      assertEquals(0, ((OpProjectStatus) statusList.get(0)).getSequence());
      assertEquals(1, ((OpProjectStatus) statusList.get(1)).getSequence());
      assertEquals(2, ((OpProjectStatus) statusList.get(2)).getSequence());

      ArrayList ids = new ArrayList(1);
      ids.add(((OpProjectStatus) statusList.get(1)).locator());
      request = new XMessage();
      request.setArgument("project_status_ids", ids);
      response = service.deleteProjectStatus(session, request);
      assertNoError(response);

      statusList = dataFactory.getAllProjectsStatus();
      assertEquals(2, statusList.size());
      assertEquals(0, ((OpProjectStatus) statusList.get(0)).getSequence());
      assertEquals(1, ((OpProjectStatus) statusList.get(1)).getSequence());
   }

   /**
    * Test the delete of a project status which has dependent ptojects
    *
    * @throws Exception if the test fails
    */
   public void testDeleteStatusWithProjects()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, NAME + 1);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_data", args);
      XMessage response = service.insertProjectStatus(session, request);
      assertNoError(response);

      args.put(OpProjectStatus.NAME, NAME + 2);
      response = service.insertProjectStatus(session, request);
      assertNoError(response);

      OpProjectStatus status1 = dataFactory.getProjectStatusByName(NAME + 1);
      OpProjectStatus status2 = dataFactory.getProjectStatusByName(NAME + 2);

      args.clear();
      args.put(OpProjectNode.TYPE, OpProjectNode.PROJECT);
      args.put(OpProjectNode.NAME, "Project");
      args.put(OpProjectNode.START, new Date(System.currentTimeMillis()));
      args.put(OpProjectNode.BUDGET, new Double(0d));
      args.put(OpProjectNode.STATUS, status2.locator());
      args.put(OpProjectNode.PROBABILITY, OpProjectNode.DEFAULT_PROBABILITY);
      args.put(OpProjectNode.PRIORITY, OpProjectNode.DEFAULT_PRIORITY);
      args.put(OpProjectNode.ARCHIVED, OpProjectNode.DEFAULT_ARCHIVED);
      args.put(OpProjectNode.STATUS, status2.locator());
      args.put("PortfolioID", "OpProjectNode.0.xid");
      args.put(OpPermissionDataSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));

      request = new XMessage();
      request.setArgument(OpProjectAdministrationService.PROJECT_DATA, args);
      request.setArgument(OpProjectAdministrationService.GOALS_SET, new XComponent(XComponent.DATA_SET));
      request.setArgument(OpProjectAdministrationService.TO_DOS_SET, new XComponent(XComponent.DATA_SET));
      
      XComponent attachmentDataSet = OpProjectTestDataFactory.createEmptyAttachmentDataSet();
      request.setArgument(OpProjectAdministrationService.ATTACHMENTS_LIST_SET, attachmentDataSet);
      response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);

      ArrayList ids = new ArrayList(1);
      ids.add(status1.locator());
      ids.add(status2.locator());
      request = new XMessage();
      request.setArgument("project_status_ids", ids);
      response = service.deleteProjectStatus(session, request);
      assertNoError(response);

      status1 = dataFactory.getProjectStatusByName(NAME + 1);
      assertNull(status1);
      status2 = dataFactory.getProjectStatusByName(NAME + 2);
      assertEquals(false, status2.getActive());
   }

   /**
    * Test move up the project statuses
    *
    * @throws Exception if the test fails
    */
   public void testMoveUpProjectStatus()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, NAME + 1);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_data", args);
      XMessage response = service.insertProjectStatus(session, request);
      assertNoError(response);

      args.put(OpProjectStatus.NAME, NAME + 2);
      response = service.insertProjectStatus(session, request);
      assertNoError(response);

      args.put(OpProjectStatus.NAME, NAME + 3);
      response = service.insertProjectStatus(session, request);
      assertNoError(response);

      OpProjectStatus status1 = dataFactory.getProjectStatusByName(NAME + 1);
      assertEquals(0, status1.getSequence());
      OpProjectStatus status2 = dataFactory.getProjectStatusByName(NAME + 2);
      assertEquals(1, status2.getSequence());
      OpProjectStatus status3 = dataFactory.getProjectStatusByName(NAME + 3);
      assertEquals(2, status3.getSequence());

      ArrayList ids = new ArrayList(2);
      ids.add(status2.locator());
      ids.add(status3.locator());
      request = new XMessage();
      request.setArgument("project_locators", ids);
      request.setArgument("direction", new Integer(-1));
      response = service.move(session, request);
      assertNoError(response);

      status1 = dataFactory.getProjectStatusByName(NAME + 1);
      assertEquals(2, status1.getSequence());
      status2 = dataFactory.getProjectStatusByName(NAME + 2);
      assertEquals(0, status2.getSequence());
      status3 = dataFactory.getProjectStatusByName(NAME + 3);
      assertEquals(1, status3.getSequence());
   }

   /**
    * Test move down the project statuses
    *
    * @throws Exception if the test fails
    */
   public void testMoveDownProjectStatus()
        throws Exception {
      HashMap args = new HashMap();
      args.put(OpProjectStatus.NAME, NAME + 1);
      args.put(OpProjectStatus.DESCRIPTION, DESCRIPTION);
      args.put(OpProjectStatus.COLOR, new Integer(7));

      XMessage request = new XMessage();
      request.setArgument("project_status_data", args);
      XMessage response = service.insertProjectStatus(session, request);
      assertNoError(response);

      args.put(OpProjectStatus.NAME, NAME + 2);
      response = service.insertProjectStatus(session, request);
      assertNoError(response);

      args.put(OpProjectStatus.NAME, NAME + 3);
      response = service.insertProjectStatus(session, request);
      assertNoError(response);

      OpProjectStatus status1 = dataFactory.getProjectStatusByName(NAME + 1);
      assertEquals(0, status1.getSequence());
      OpProjectStatus status2 = dataFactory.getProjectStatusByName(NAME + 2);
      assertEquals(1, status2.getSequence());
      OpProjectStatus status3 = dataFactory.getProjectStatusByName(NAME + 3);
      assertEquals(2, status3.getSequence());

      ArrayList ids = new ArrayList(2);
      ids.add(status1.locator());
      ids.add(status2.locator());
      request = new XMessage();
      request.setArgument("project_locators", ids);
      request.setArgument("direction", new Integer(1));
      response = service.move(session, request);
      assertNoError(response);

      status1 = dataFactory.getProjectStatusByName(NAME + 1);
      assertEquals(1, status1.getSequence());
      status2 = dataFactory.getProjectStatusByName(NAME + 2);
      assertEquals(2, status2.getSequence());
      status3 = dataFactory.getProjectStatusByName(NAME + 3);
      assertEquals(0, status3.getSequence());
   }

   /**
    * Test the security mechanism
    *
    * @throws Exception if test fails
    */
   public void testRights()
        throws Exception {
      Map userData = OpUserTestDataFactory.createUserData(DEFAULT_USER, DEFAULT_PASSWORD, OpUser.CONTRIBUTOR_USER_LEVEL);
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = OpTestDataFactory.getUserService().insertUser(session, request);
      assertNoError(response);
      logIn(DEFAULT_USER, DEFAULT_PASSWORD);

      response = service.insertProjectStatus(session, new XMessage());
      assertError(response, OpProjectStatusError.INSUFFICIENT_PRIVILEGES);

      response = service.move(session, new XMessage());
      assertError(response, OpProjectStatusError.INSUFFICIENT_PRIVILEGES);

      response = service.deleteProjectStatus(session, new XMessage());
      assertError(response, OpProjectStatusError.INSUFFICIENT_PRIVILEGES);

      response = service.updateProjectStatus(session, new XMessage());
      assertError(response, OpProjectStatusError.INSUFFICIENT_PRIVILEGES);
   }

//                             ***** Helper Methods *****

   private void clean()
        throws Exception {
      logOut();
      logIn();
      OpUserTestDataFactory usrData = new OpUserTestDataFactory(session);
      OpUser user = usrData.getUserByName(DEFAULT_USER);
      if (user != null) {
         List ids = new ArrayList();
         ids.add(user.locator());
         XMessage request = new XMessage();
         request.setArgument(OpUserService.SUBJECT_IDS, ids);
         OpTestDataFactory.getUserService().deleteSubjects(session, request);
      }

      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();

      OpProjectTestDataFactory projectDataFactory = new OpProjectTestDataFactory(session);
      List projectList = projectDataFactory.getAllProjects(broker);
      for (Object aProjectList : projectList) {
         OpProjectNode project = (OpProjectNode) aProjectList;
         broker.deleteObject(project);
      }

      List statusList = dataFactory.getAllProjectsStatus();
      for (Object aStatusList : statusList) {
         OpProjectStatus status = (OpProjectStatus) aStatusList;
         broker.deleteObject(status);
      }

      transaction.commit();
      broker.close();

   }
}
