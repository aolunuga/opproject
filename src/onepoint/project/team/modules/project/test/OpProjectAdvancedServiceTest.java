/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.team.modules.project.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectError;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.test.ProjectTestDataFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.UserTestDataFactory;
import onepoint.project.team.modules.project.OpProjectAdministrationAdvancedService;
import onepoint.project.team.test.OpBaseTeamTestCase;
import onepoint.project.test.TestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class test project service methods.
 *
 * @author lucian.furtos
 */
public class OpProjectAdvancedServiceTest extends OpBaseTeamTestCase {

   private static final String TEMPLATE_NAME = "template";

   private OpProjectAdministrationAdvancedService service;
   private ProjectAdvancedTestDataFactory dataFactory;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      service = getProjectAdvancedService();
      dataFactory = new ProjectAdvancedTestDataFactory(session);
      clean();
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
    * Test template creation, happy flow.
    *
    * @throws Exception if the test fails
    */
   public void testInsertTemplate()
        throws Exception {
      String portfolioId = OpLocator.locatorString(OpProjectNode.PROJECT_NODE, -1);
      XMessage request = ProjectAdvancedTestDataFactory.createTemplateMsg(TEMPLATE_NAME, "Some description", portfolioId, Boolean.FALSE, Boolean.TRUE);
      XMessage response = service.insertTemplate(session, request);
      assertNoError(response);
   }

   /**
    * Test template creation, errors.
    *
    * @throws Exception if the test fails
    */
   public void testInsertTemplateErrors()
        throws Exception {
      XMessage request = ProjectAdvancedTestDataFactory.createTemplateMsg(null);
      XMessage response = service.insertTemplate(session, request);
      assertError(response, OpProjectError.TEMPLATE_NAME_MISSING);

      request = ProjectAdvancedTestDataFactory.createTemplateMsg("");
      response = service.insertTemplate(session, request);
      assertError(response, OpProjectError.TEMPLATE_NAME_MISSING);

      request = ProjectAdvancedTestDataFactory.createTemplateMsg(TEMPLATE_NAME);
      response = service.insertTemplate(session, request);
      assertNoError(response);

      request = ProjectAdvancedTestDataFactory.createTemplateMsg(TEMPLATE_NAME);
      response = service.insertTemplate(session, request);
      assertError(response, OpProjectError.TEMPLATE_NAME_ALREADY_USED);
   }

   /**
    * Test the save as template method for a project
    *
    * @throws Exception if test fails
    */
   public void testSaveAsTemplate()
        throws Exception {
      UserTestDataFactory userDataFactory = new UserTestDataFactory(session);
      OpUser admin = userDataFactory.getUserById(OpLocator.locatorString(OpUser.USER, session.getAdministratorID()));
      XComponent dataSet = TestDataFactory.createPermissionSet(OpPermission.ADMINISTRATOR, admin.locator(), admin.getName());
      XMessage request = ProjectTestDataFactory.createProjectMsg("project", new Date(1), null, 1000d, null, null, Boolean.FALSE,
           Boolean.FALSE, null, null, null, dataSet);
      XMessage response = service.insertProject(session, request);
      assertNoError(response);

      assertTrue(dataFactory.getAllTemplates().isEmpty());

      String projectId = dataFactory.getProjectId("project");
      String portofolio = OpProjectAdministrationService.findRootPortfolio(session.newBroker()).locator();
      request = ProjectAdvancedTestDataFactory.saveAsTemplateMsg(projectId, TEMPLATE_NAME, portofolio);
      response = service.saveAsTemplate(session, request);
      assertNoError(response);

      OpProjectNode template = dataFactory.getTemplateByName(TEMPLATE_NAME);
      assertNotNull(template);
      OpProjectPlan plan = template.getPlan();
      assertFalse(plan.getProgressTracked());
      assertEquals(OpProjectPlan.INDEPENDENT, plan.getCalculationMode());
      assertTrue(plan.getTemplate());
   }

   /**
    * Test the errors for save as template method
    *
    * @throws Exception if test fails
    */
   public void testSaveAsTemplateError()
        throws Exception {
      XMessage request = ProjectAdvancedTestDataFactory.saveAsTemplateMsg(null, null, null);
      XMessage response = service.saveAsTemplate(session, request);
      assertError(response, OpProjectError.PROJECT_NOT_SPECIFIED);

      request = ProjectAdvancedTestDataFactory.saveAsTemplateMsg("id", null, null);
      response = service.saveAsTemplate(session, request);
      assertError(response, OpProjectError.TEMPLATE_NAME_MISSING);

      request = ProjectAdvancedTestDataFactory.saveAsTemplateMsg("id", "", null);
      response = service.saveAsTemplate(session, request);
      assertError(response, OpProjectError.TEMPLATE_NAME_MISSING);

      request = ProjectAdvancedTestDataFactory.createTemplateMsg(TEMPLATE_NAME);
      response = service.insertTemplate(session, request);
      assertNoError(response);

      request = ProjectAdvancedTestDataFactory.saveAsTemplateMsg("id", TEMPLATE_NAME, null);
      response = service.saveAsTemplate(session, request);
      assertError(response, OpProjectError.TEMPLATE_NAME_ALREADY_USED);

      request = ProjectAdvancedTestDataFactory.saveAsTemplateMsg(OpLocator.locatorString(OpProjectNode.PROJECT_NODE, -1), TEMPLATE_NAME + 1, null);
      response = service.saveAsTemplate(session, request);
      assertError(response, OpProjectError.PROJECT_NOT_FOUND);

      request = ProjectTestDataFactory.createProjectMsg("project", new Date(1), 1d, null, null);
      response = service.insertProject(session, request);
      assertNoError(response);
      String id = dataFactory.getProjectId("project");
      request = ProjectAdvancedTestDataFactory.saveAsTemplateMsg(id, TEMPLATE_NAME + 1, null);
      response = service.saveAsTemplate(session, request);
      assertError(response, OpProjectError.TARGET_PORTFOLIO_NOT_SELECTED);
   }

   /**
    * Test template update, happy flow.
    *
    * @throws Exception if the test fails
    */
   public void testUpdateTemplate()
        throws Exception {
      XMessage request = ProjectAdvancedTestDataFactory.createTemplateMsg(TEMPLATE_NAME, "Some description", null, Boolean.FALSE, Boolean.TRUE);
      XMessage response = service.insertTemplate(session, request);
      assertNoError(response);

      String id = dataFactory.getTemplateId(TEMPLATE_NAME);
      request = ProjectAdvancedTestDataFactory.updateTemplateMsg(id, "New" + TEMPLATE_NAME, "New description", Boolean.TRUE, Boolean.FALSE);
      response = service.updateTemplate(session, request);
      assertNoError(response);

      OpProjectNode template = dataFactory.getTemplateById(id);
      assertEquals("New" + TEMPLATE_NAME, template.getName());
      assertEquals("New description", template.getDescription());
      OpProjectPlan plan = template.getPlan();
      assertFalse(plan.getProgressTracked());
      assertEquals(OpProjectPlan.EFFORT_BASED, plan.getCalculationMode());
   }

   /**
    * Test template update with errors
    *
    * @throws Exception if the test fails
    */
   public void testUpdateTemplateError()
        throws Exception {
      XMessage request = ProjectAdvancedTestDataFactory.updateTemplateMsg(null, null);
      XMessage response = service.insertTemplate(session, request);
      assertError(response, OpProjectError.TEMPLATE_NAME_MISSING);

      request = ProjectAdvancedTestDataFactory.updateTemplateMsg(null, "");
      response = service.insertTemplate(session, request);
      assertError(response, OpProjectError.TEMPLATE_NAME_MISSING);

      request = ProjectAdvancedTestDataFactory.createTemplateMsg(TEMPLATE_NAME);
      response = service.insertTemplate(session, request);
      assertNoError(response);
      request = ProjectAdvancedTestDataFactory.createTemplateMsg(TEMPLATE_NAME + 1);
      response = service.insertTemplate(session, request);
      assertNoError(response);

      String templateId = dataFactory.getTemplateId(TEMPLATE_NAME + 1);
      request = ProjectAdvancedTestDataFactory.updateTemplateMsg(templateId, TEMPLATE_NAME);
      response = service.insertTemplate(session, request);
      assertError(response, OpProjectError.TEMPLATE_NAME_ALREADY_USED);
   }

   /**
    * Test template delete method
    *
    * @throws Exception if the test fails
    */
   public void testDeleteTemplates()
        throws Exception {
      XMessage request = ProjectAdvancedTestDataFactory.createTemplateMsg(TEMPLATE_NAME);
      XMessage response = service.insertTemplate(session, request);
      assertNoError(response);
      request = ProjectAdvancedTestDataFactory.createTemplateMsg(TEMPLATE_NAME + 1);
      response = service.insertTemplate(session, request);
      assertNoError(response);
      request = ProjectAdvancedTestDataFactory.createTemplateMsg(TEMPLATE_NAME + 2);
      response = service.insertTemplate(session, request);
      assertNoError(response);

      List list = dataFactory.getAllTemplates();
      assertEquals(3, list.size());
      ArrayList ids = new ArrayList();
      ids.add(dataFactory.getTemplateId(TEMPLATE_NAME));
      ids.add(dataFactory.getTemplateId(TEMPLATE_NAME + 2));
      request = new XMessage();
      request.setArgument("template_ids", ids);
      response = service.deleteTemplates(session, request);
      assertNoError(response);

      list = dataFactory.getAllTemplates();
      assertEquals(1, list.size());
      OpProjectNode template = (OpProjectNode) list.get(0);
      assertEquals(TEMPLATE_NAME + 1, template.getName());
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

      deleteAllObjects(OpProjectPlan.PROJECT_PLAN);

      List templateList = dataFactory.getAllTemplates();
      for (Iterator iterator = templateList.iterator(); iterator.hasNext();) {
         OpProjectNode template = (OpProjectNode) iterator.next();
         dataFactory.deleteObject(template);
      }

      List projectList = dataFactory.getAllProjects();
      for (Iterator iterator = projectList.iterator(); iterator.hasNext();) {
         OpProjectNode project = (OpProjectNode) iterator.next();
         dataFactory.deleteObject(project);
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
