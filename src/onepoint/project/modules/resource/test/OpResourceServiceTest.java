/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.configuration.OpDatabaseConfiguration;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceError;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.OpUserTestDataFactory;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.util.OpProjectConstants;
import onepoint.project.util.OpProjectCalendar;
import onepoint.resource.XLocaleManager;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class test resource service methods.
 *
 * @author lucian.furtos
 */
public class OpResourceServiceTest extends OpBaseOpenTestCase {

   private static final String NAME = "resource_one";
   private static final String DESCRIPTION = "The Resource Description";
   private static final String POOL_NAME = "pool_one";
   private static final String POOL_DESCRIPTION = "The resource pool description";
   private static final String NEW_POOL_NAME = "new_pool_name";
   private static final String NEW_POOL_DESCRIPTION = "New pool description";
   private static final String NEW_DESCRIPTION = "New Resource description";

   private static final String DEFAULT_USER = "tester";
   private static final String DEFAULT_PASSWORD = "pass";

   private OpResourceService service;
   private OpResourceTestDataFactory dataFactory;
   private static final String NEW_NAME = "new_resource";

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   @Override
   protected void setUp()
        throws Exception {
      super.setUp();

      service = OpTestDataFactory.getResourceService();
      dataFactory = new OpResourceTestDataFactory(session);

      Map userData = OpUserTestDataFactory.createUserData(DEFAULT_USER, DEFAULT_PASSWORD, OpUser.CONTRIBUTOR_USER_LEVEL);
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = OpTestDataFactory.getUserService().insertUser(session, request);
      assertNoError(response);

      OpProjectCalendar.getDefaultProjectCalendar().configure(null, XLocaleManager.getDefaultLocale(), null, OpProjectCalendar.GMT_TIMEZONE);
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   @Override
   protected void tearDown()
        throws Exception {
      logOut();
      logIn();

      clean();

      super.tearDown();
   }

   /**
    * Test happy-flow creation of resource pools
    *
    * @throws Exception if the test fails
    */
   public void testCreateResourcePool()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 2d, 2d, null);
      XMessage response = service.insertPool(session, request);
      assertNoError(response);
      OpResourcePool pool = dataFactory.getResourcePoolByName(POOL_NAME);
      assertEquals(POOL_DESCRIPTION, pool.getDescription());
      assertEquals(2d, pool.getHourlyRate(), 0d);
   }

   /**
    * Test creation of resource pools with wrong name
    *
    * @throws Exception if the test fails
    */
   public void testCreateResourcePoolWrongName()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(null, POOL_DESCRIPTION, 2d, 2d, null);
      XMessage response = service.insertPool(session, request);
      assertError(response, OpResourceError.POOL_NAME_NOT_SPECIFIED);

      request = dataFactory.createPoolMsg("", POOL_DESCRIPTION, 2d, 2d, null);
      response = service.insertPool(session, request);
      assertError(response, OpResourceError.POOL_NAME_NOT_SPECIFIED);

      request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, -2d, 2d, null);
      response = service.insertPool(session, request);
      assertError(response, OpResourceError.HOURLY_RATE_NOT_VALID);

      request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 2d, -2d, null);
      response = service.insertPool(session, request);
      assertError(response, OpResourceError.EXTERNAL_RATE_NOT_VALID);

      request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 2d, 2d, null);
      response = service.insertPool(session, request);
      assertNoError(response);

      request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 1d, 1d, null);
      response = service.insertPool(session, request);
      assertError(response, OpResourceError.POOL_NAME_NOT_UNIQUE);
   }

   /**
    * Test update of resource pool
    *
    * @throws Exception if the test fails
    */
   public void testUpdateResourcePool()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 2d, 2d, null);
      XMessage response = service.insertPool(session, request);
      assertNoError(response);
      String id = dataFactory.getResourcePoolId(POOL_NAME);

      request = dataFactory.updatePoolMsg(id, NEW_POOL_NAME, NEW_POOL_DESCRIPTION, new Double(5d), new Double(7d));
      response = service.updatePool(session, request);
      assertNoError(response);

      OpResourcePool pool = dataFactory.getResourcePoolById(id);
      assertEquals(NEW_POOL_NAME, pool.getName());
      assertEquals(NEW_POOL_DESCRIPTION, pool.getDescription());
      assertEquals(5d, pool.getHourlyRate(), 0d);
   }

   /**
    * Test update of resource pool errors
    *
    * @throws Exception if the test fails
    */
   public void testUpdateResourcePoolErrors()
        throws Exception {
      String fakceid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0);
      XMessage request = dataFactory.updatePoolMsg(fakceid, NEW_POOL_NAME, NEW_POOL_DESCRIPTION, new Double(5d), new Double(5d));
      XMessage response = service.updatePool(session, request);
      assertError(response, OpResourceError.POOL_NOT_FOUND);

      request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 2d, 2d, null);
      response = service.insertPool(session, request);
      assertNoError(response);

      String id = dataFactory.getResourcePoolId(POOL_NAME);

      request = dataFactory.updatePoolMsg(id, null, NEW_POOL_DESCRIPTION, new Double(5d), new Double(5d));
      response = service.updatePool(session, request);
      assertError(response, OpResourceError.POOL_NAME_NOT_SPECIFIED);

      request = dataFactory.updatePoolMsg(id, "", NEW_POOL_DESCRIPTION, new Double(5d), new Double(5d));
      response = service.updatePool(session, request);
      assertError(response, OpResourceError.POOL_NAME_NOT_SPECIFIED);

      request = dataFactory.createPoolMsg(NEW_POOL_NAME, NEW_POOL_DESCRIPTION, 3d, 2d, null);
      response = service.insertPool(session, request);
      assertNoError(response);

      request = dataFactory.updatePoolMsg(id, NEW_POOL_NAME, NEW_POOL_DESCRIPTION, new Double(5d), new Double(5d));
      response = service.updatePool(session, request);
      assertError(response, OpResourceError.POOL_NAME_NOT_UNIQUE);

      request = dataFactory.updatePoolMsg(id, POOL_NAME, POOL_DESCRIPTION, new Double(-5d), new Double(5d));
      response = service.updatePool(session, request);
      assertError(response, OpResourceError.HOURLY_RATE_NOT_VALID);

      request = dataFactory.updatePoolMsg(id, POOL_NAME, POOL_DESCRIPTION, new Double(5d), new Double(-5d));
      response = service.updatePool(session, request);
      assertError(response, OpResourceError.EXTERNAL_RATE_NOT_VALID);
   }

   /**
    * Test happy-flow deletion of resource pools
    *
    * @throws Exception if the test fails
    */
   public void testDeleteResourcePool()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(POOL_NAME + 1, POOL_DESCRIPTION, 2d, 2d, null);
      XMessage response = service.insertPool(session, request);
      assertNoError(response);

      request = dataFactory.createPoolMsg(POOL_NAME + 2, POOL_DESCRIPTION, 3d, 2d, null);
      response = service.insertPool(session, request);
      assertNoError(response);

      List<String> ids = new ArrayList<String>(2);
      ids.add(dataFactory.getResourcePoolId(POOL_NAME + 1));
      ids.add(dataFactory.getResourcePoolId(POOL_NAME + 2));
      request = new XMessage();
      request.setArgument(OpResourceService.POOL_IDS, ids);
      response = service.deletePools(session, request);
      assertNoError(response);
   }

   /**
    * Test happy-flow creation of resources
    *
    * @throws Exception if the test fails
    */
   public void testCreateResource()
        throws Exception {
      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 1d, false, poolid);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      OpResource resource = dataFactory.getResourceByName(NAME);
      assertEquals(DESCRIPTION, resource.getDescription());
      assertEquals(50d, resource.getAvailable(), 0d);
      assertEquals(2d, resource.getHourlyRate(), 0d);
      assertEquals(1d, resource.getExternalRate(), 0d);
      assertFalse(resource.getInheritPoolRate());
   }

   /**
    * Test creation of resources with error
    *
    * @throws Exception if the test fails
    */
   public void testCreateResourceError()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(null, DESCRIPTION, 50d, 2d, 1d, false, null);
      XMessage response = service.insertResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED);

      request = dataFactory.createResourceMsg("", DESCRIPTION, 50d, 2d, 1d, false, null);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED);

      request = dataFactory.createResourceMsg(NAME + '%', DESCRIPTION, 50d, 2d, 1d, false, null);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_VALID);

      double maxAvailability = Double.parseDouble(OpSettingsService.getService().getStringValue(session, OpSettings.RESOURCE_MAX_AVAILABYLITY));
      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, maxAvailability + 1, 2d, 1d, false, null);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.AVAILABILITY_NOT_VALID);

      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 99d, -1d, 1d, false, null);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.HOURLY_RATE_NOT_VALID);

      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 99d, 1d, -1d, false, null);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.EXTERNAL_RATE_NOT_VALID);
   }

   /**
    * Test creation of resources with duplicate name
    *
    * @throws Exception if the test fails
    */
   public void testCreateResourceDuplicateName()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 1d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      request = dataFactory.createResourceMsg(NAME, NEW_DESCRIPTION, 30d, 6d, 3d, false, null);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_UNIQUE);

      request = dataFactory.createResourceMsg(NAME + '%', DESCRIPTION, 50d, 2d, 1d, false, null);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_VALID);

      double maxAvailability = Double.parseDouble(OpSettingsService.getService().getStringValue(session, OpSettings.RESOURCE_MAX_AVAILABYLITY));
      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, maxAvailability + 1, 2d, 3d, false, null);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.AVAILABILITY_NOT_VALID);

      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 99d, -1d, 2d, false, null);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.HOURLY_RATE_NOT_VALID);

      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 99d, 1d, -2d, false, null);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.EXTERNAL_RATE_NOT_VALID);
   }

   /**
    * Test creation of resources with projects
    *
    * @throws Exception if the test fails
    */
   public void testCreateResourceWithProjects()
        throws Exception {
      XMessage request = OpProjectTestDataFactory.createProjectMsg("prj1", new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);

      request = OpProjectTestDataFactory.createProjectMsg("prj2", new Date(System.currentTimeMillis()), 4000d, null, null);
      response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);

      OpProjectTestDataFactory projectDataFactory = new OpProjectTestDataFactory(session);
      ArrayList<String> projects = new ArrayList<String>();
      projects.add(projectDataFactory.getProjectId("prj1"));
      projects.add(projectDataFactory.getProjectId("prj2"));

      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 1d, false, null, projects);
      response = service.insertResource(session, request);
      assertNoError(response);

      OpResource resource = dataFactory.getResourceByName(NAME);
      assertEquals(DESCRIPTION, resource.getDescription());
      assertEquals(50d, resource.getAvailable(), 0d);
      assertEquals(2d, resource.getHourlyRate(), 0d);
      assertFalse(resource.getInheritPoolRate());
   }

   /**
    * Test update of resources with error
    *
    * @throws Exception if the test fails
    */
   public void testUpdateResourceWithErrors()
        throws Exception {
      XMessage request = dataFactory.updateResourceMsg(OpLocator.locatorString(OpResource.RESOURCE, 0), "", "", 0d, 0d, 0d, false, null, null);
      XMessage response = service.updateResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NOT_FOUND);

      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 3d, false, null);
      response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);

      request = dataFactory.updateResourceMsg(id, null, NEW_DESCRIPTION, 80d, 7d, 2d, true, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED);

      request = dataFactory.updateResourceMsg(id, "", NEW_DESCRIPTION, 80d, 7d, 2d, true, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED);

      request = dataFactory.updateResourceMsg(id, "name%", NEW_DESCRIPTION, 80d, 7d, 2d, true, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_VALID);

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 104d, 7d, 2d, false, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.AVAILABILITY_NOT_VALID);

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, -4d, 7d, 2d, false, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.AVAILABILITY_NOT_VALID);

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 66d, -3d, 3d, false, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.HOURLY_RATE_NOT_VALID);

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 66d, 3d, -3d, false, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.EXTERNAL_RATE_NOT_VALID);
   }

   /**
    * Test update of resources with duplicate name
    *
    * @throws Exception if the test fails
    */
   public void testUpdateResourceDuplicateName()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 1d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      request = dataFactory.createResourceMsg(NEW_NAME, NEW_DESCRIPTION, 90d, 3d, 1d, true, null);
      response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 80d, 7d, 3d, true, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_UNIQUE);
   }

   /**
    * Test the inheritance of Hourly rate from pools to resources
    *
    * @throws Exception if the test fails
    */
   public void testHourlyRateUpdate()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 0d, 0d, null);
      XMessage response = service.insertPool(session, request);
      assertNoError(response);
      String poolid = dataFactory.getResourcePoolId(POOL_NAME);

      request = dataFactory.createResourceMsg(NAME + 1, DESCRIPTION, 50d, 0d, 0d, true, poolid);
      response = service.insertResource(session, request);
      assertNoError(response);
      request = dataFactory.createResourceMsg(NAME + 2, DESCRIPTION, 50d, 0d, 0d, false, poolid);
      response = service.insertResource(session, request);
      assertNoError(response);

      request = dataFactory.updatePoolMsg(poolid, POOL_NAME, POOL_DESCRIPTION, new Double(5d), new Double(5d));
      response = service.updatePool(session, request);
      assertNoError(response);

      OpResource resource = dataFactory.getResourceByName(NAME + 1);
      assertTrue(resource.getInheritPoolRate());
      assertEquals(5d, resource.getHourlyRate(), 0d);
      resource = dataFactory.getResourceByName(NAME + 2);
      assertFalse(resource.getInheritPoolRate());
      assertEquals(0d, resource.getHourlyRate(), 0d);
   }

   /**
    * Test the inheritance of External rate from pools to resources
    *
    * @throws Exception if the test fails
    */
   public void testExternalRateUpdate()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 0d, 0d, null);
      XMessage response = service.insertPool(session, request);
      assertNoError(response);
      String poolid = dataFactory.getResourcePoolId(POOL_NAME);

      request = dataFactory.createResourceMsg(NAME + 1, DESCRIPTION, 50d, 0d, 0d, true, poolid);
      response = service.insertResource(session, request);
      assertNoError(response);
      request = dataFactory.createResourceMsg(NAME + 2, DESCRIPTION, 50d, 0d, 0d, false, poolid);
      response = service.insertResource(session, request);
      assertNoError(response);

      request = dataFactory.updatePoolMsg(poolid, POOL_NAME, POOL_DESCRIPTION, new Double(5d), new Double(5d));
      response = service.updatePool(session, request);
      assertNoError(response);

      OpResource resource = dataFactory.getResourceByName(NAME + 1);
      assertTrue(resource.getInheritPoolRate());
      assertEquals(5d, resource.getExternalRate(), 0d);
      resource = dataFactory.getResourceByName(NAME + 2);
      assertFalse(resource.getInheritPoolRate());
      assertEquals(0d, resource.getExternalRate(), 0d);
   }

   /**
    * Test the user import
    *
    * @throws Exception if the test fails
    */
   public void testImportUser()
        throws Exception {
      OpUserTestDataFactory userDataFactory = new OpUserTestDataFactory(session);
      OpUser user = userDataFactory.getUserByName(DEFAULT_USER);

      XMessage request = OpProjectTestDataFactory.createProjectMsg("prj1", new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg("prj2", new Date(System.currentTimeMillis()), 4000d, null, null);
      response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);

      OpProjectTestDataFactory projectDataFactory = new OpProjectTestDataFactory(session);
      ArrayList<String> projectIds = new ArrayList<String>();
      projectIds.add(projectDataFactory.getProjectId("prj1"));
      projectIds.add(projectDataFactory.getProjectId("prj2"));

      request = dataFactory.importUserMsg(user.locator(), 34d, 8d, 10d, false, null, projectIds);
      response = service.importUser(session, request);
      assertNoError(response);

      OpResource res = dataFactory.getResourceByName(DEFAULT_USER);
      assertNotNull(res);
      assertEquals(res.getDescription(), null);
      assertEquals(res.getAvailable(), 34d, 0d);
      assertEquals(res.getHourlyRate(), 8d, 0d);
      assertEquals(res.getExternalRate(), 10d, 0d);
      assertFalse(res.getInheritPoolRate());
   }

   /**
    * Test unknown user import
    *
    * @throws Exception if the test fails
    */
   public void testImportUnknownUser()
        throws Exception {
      XMessage request = dataFactory.importUserMsg(null, 34d, 8d, 10d, false, null, null);
      XMessage response = service.importUser(session, request);
      assertError(response, OpResourceError.USER_ID_NOT_SPECIFIED);

      request = dataFactory.importUserMsg("", 34d, 8d, 10d, false, null, null);
      response = service.importUser(session, request);
      assertError(response, OpResourceError.USER_ID_NOT_SPECIFIED);
   }

   /**
    * Test user import with erors
    *
    * @throws Exception if the test fails
    */
   public void testImportUserWithErrors()
        throws Exception {
      OpUserTestDataFactory userDataFactory = new OpUserTestDataFactory(session);
      OpUser user = userDataFactory.getUserByName(DEFAULT_USER);

      XMessage request = dataFactory.importUserMsg(user.locator(), -4d, 8d, 10d, false, null, null);
      XMessage response = service.importUser(session, request);
      assertError(response, OpResourceError.AVAILABILITY_NOT_VALID);

      request = dataFactory.importUserMsg(user.locator(), 101d, 8d, 10d, false, null, null);
      response = service.importUser(session, request);
      assertError(response, OpResourceError.AVAILABILITY_NOT_VALID);

      request = dataFactory.importUserMsg(user.locator(), 50d, -4d, 10d, false, null, null);
      response = service.importUser(session, request);
      assertError(response, OpResourceError.HOURLY_RATE_NOT_VALID);

      request = dataFactory.importUserMsg(user.locator(), 50d, 4d, -10d, false, null, null);
      response = service.importUser(session, request);
      assertError(response, OpResourceError.EXTERNAL_RATE_NOT_VALID);
   }

   /**
    * Test the user import with duplicate resource name
    *
    * @throws Exception if the test fails
    */
   public void testImportUserDuplicate()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(DEFAULT_USER, DESCRIPTION, 50d, 2d, 2d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      OpUserTestDataFactory userDataFactory = new OpUserTestDataFactory(session);
      OpUser user = userDataFactory.getUserByName(DEFAULT_USER);

      request = dataFactory.importUserMsg(user.locator(), 34d, 8d, 10d, false, null, null);
      response = service.importUser(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_UNIQUE);
   }


   /**
    * Test if the pool has resources with assignments
    *
    * @throws Exception if the test fails
    */
   public void testHasResourceAssignments()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 2d, 2d, null);
      XMessage response = service.insertPool(session, request);
      assertNoError(response);

      OpBroker broker = session.newBroker();
      try {
         String poolid = dataFactory.getResourcePoolId(POOL_NAME);

         request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, poolid);
         response = service.insertResource(session, request);
         assertNoError(response);

         String id = dataFactory.getResourceId(NAME);

         request = new XMessage();
         request.setArgument(OpResourceService.POOL_ID, poolid);
         response = service.hasResourceAssignments(session, broker, request);
         assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS)).booleanValue());

         OpResource resource = dataFactory.getResourceById(id);
         OpTransaction t = broker.newTransaction();
         OpActivity activity = new OpActivity();
         OpAssignment assignment = new OpAssignment();
         activity.addAssignment(assignment);
         assignment.setResource(resource);
         OpActivityVersion version = new OpActivityVersion();
         version.setActivity(activity);
         broker.makePersistent(activity);
         broker.makePersistent(assignment);
         broker.makePersistent(version);
         t.commit();

         request = new XMessage();
         request.setArgument(OpResourceService.POOL_ID, poolid);
         response = service.hasResourceAssignments(session, broker, request);
         assertTrue(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS)).booleanValue());
      }
      finally {
         broker.close();
      }
   }

   /**
    * Test assignment of pools and resources to projects
    *
    * @throws Exception if the test fails
    */
   public void testAssignToProjects()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 2d, 2d, null);
      XMessage response = service.insertPool(session, request);
      assertNoError(response);
      OpProjectTestDataFactory projectDataFactory = new OpProjectTestDataFactory(session);
      String poolid = dataFactory.getResourcePoolId(POOL_NAME);
      request = dataFactory.createResourceMsg(NAME + 1, DESCRIPTION, 50d, 2d, 2d, true, poolid);
      response = service.insertResource(session, request);
      assertNoError(response);

      request = dataFactory.createResourceMsg(NAME + 2, DESCRIPTION, 99d, 0d, 0d, false, null);
      response = service.insertResource(session, request);
      assertNoError(response);
      String resourceid = dataFactory.getResourceId(NAME + 2);

      List<String> resouceIds = new ArrayList<String>();
      resouceIds.add(poolid);
      resouceIds.add(resourceid);

      request = OpProjectTestDataFactory.createProjectMsg("prj1", new Date(System.currentTimeMillis()), 1000d, null, null);
      response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);
      request = OpProjectTestDataFactory.createProjectMsg("prj2", new Date(System.currentTimeMillis()), 4000d, null, null);
      response = OpTestDataFactory.getProjectService().insertProject(session, request);
      assertNoError(response);

      List<String> projectIds = new ArrayList<String>();
      projectIds.add(projectDataFactory.getProjectId("prj1"));
      projectIds.add(projectDataFactory.getProjectId("prj2"));

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_IDS, resouceIds);
      request.setArgument(OpResourceService.PROJECT_IDS, projectIds);
      response = service.assignToProject(session, request);
      assertNoError(response);

      OpResource resource = dataFactory.getResourceByName(NAME + 1);
      assertNotNull(resource);
      assertEquals(2, resource.getProjectNodeAssignments().size());
      resource = dataFactory.getResourceByName(NAME + 2);
      assertNotNull(resource);
      assertEquals(2, resource.getProjectNodeAssignments().size());

      OpProjectNode project = projectDataFactory.getProjectByName("prj1");
      assertNotNull(project);
      assertEquals(2, project.getAssignments().size());
      project = projectDataFactory.getProjectByName("prj2");
      assertNotNull(project);
      assertEquals(2, project.getAssignments().size());
   }

   /**
    * Test the move of resources
    *
    * @throws Exception if the test fails
    */
   public void testMoveResourceNode()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 5d, 5d, null);
      XMessage response = service.insertPool(session, request);
      assertNoError(response);

      String poolid = dataFactory.getResourcePoolId(POOL_NAME);

      request = dataFactory.createResourceMsg(NAME + 1, DESCRIPTION, 50d, 2d, 2d, true, null);
      response = service.insertResource(session, request);
      assertNoError(response);
      request = dataFactory.createResourceMsg(NAME + 2, DESCRIPTION, 50d, 2d, 2d, false, null);
      response = service.insertResource(session, request);
      assertNoError(response);

      List<String> ids = new ArrayList<String>();
      ids.add(dataFactory.getResourceId(NAME + 1));
      ids.add(dataFactory.getResourceId(NAME + 2));
      request = new XMessage();
      request.setArgument(OpResourceService.POOL_ID, poolid);
      request.setArgument(OpResourceService.RESOURCE_IDS, ids);
      response = service.moveResourceNode(session, request);
      assertNoError(response);

      OpResource resource = dataFactory.getResourceByName(NAME + 1);
      assertEquals(5d, resource.getHourlyRate(), 0d);
      assertEquals(poolid, resource.getPool().locator());
      resource = dataFactory.getResourceByName(NAME + 2);
      assertEquals(2d, resource.getHourlyRate(), 0d);
      assertEquals(poolid, resource.getPool().locator());
   }

   /**
    * Test the move of pools
    *
    * @throws Exception if the test fails
    */
   public void testMovePoolNode()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 5d, 5d, null);
      XMessage response = service.insertPool(session, request);
      assertNoError(response);

      String poolid = dataFactory.getResourcePoolId(POOL_NAME);

      request = dataFactory.createPoolMsg(POOL_NAME + 1, POOL_DESCRIPTION, 2d, 2d, null);
      response = service.insertPool(session, request);
      assertNoError(response);
      request = dataFactory.createPoolMsg(POOL_NAME + 2, POOL_DESCRIPTION, 2d, 2d, null);
      response = service.insertPool(session, request);
      assertNoError(response);

      List<String> ids = new ArrayList<String>();
      ids.add(dataFactory.getResourcePoolId(POOL_NAME + 1));
      ids.add(dataFactory.getResourcePoolId(POOL_NAME + 2));

      request = new XMessage();
      request.setArgument(OpResourceService.SUPER_POOL_ID, poolid);
      request.setArgument(OpResourceService.POOL_IDS, ids);
      response = service.movePoolNode(session, request);
      assertNoError(response);

      OpResourcePool pool = dataFactory.getResourcePoolByName(POOL_NAME + 1);
      assertEquals(poolid, pool.getSuperPool().locator());
      pool = dataFactory.getResourcePoolByName(POOL_NAME + 2);
      assertEquals(poolid, pool.getSuperPool().locator());
      pool = dataFactory.getResourcePoolByName(POOL_NAME);
      assertEquals(2, pool.getSubPools().size());
   }

   /**
    * Test the expand of a resource pool
    *
    * @throws Exception if the test fails
    */
   public void testExpandResourcePool()
        throws Exception {
      XMessage request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 5d, 5d, null);
      XMessage response = service.insertPool(session, request);
      assertNoError(response);
      String poolid = dataFactory.getResourcePoolId(POOL_NAME);

      request = dataFactory.createPoolMsg(POOL_NAME + 1, POOL_DESCRIPTION, 2d, 2d, poolid);
      response = service.insertPool(session, request);
      assertNoError(response);
      request = dataFactory.createPoolMsg(POOL_NAME + 2, POOL_DESCRIPTION, 2d, 2d, poolid);
      response = service.insertPool(session, request);
      assertNoError(response);

      request = new XMessage();
      request.setArgument("source_pool_locator", poolid);
      request.setArgument("outlineLevel", new Integer(1));
      request.setArgument("poolColumnsSelector", null);
      request.setArgument("resourceColumnsSelector", null);
      request.setArgument("FilteredOutIds", new ArrayList());
      response = service.expandResourcePool(session, request);
      assertNoError(response);

      List children = (List) response.getArgument(OpProjectConstants.CHILDREN);
      assertNotNull(children);
   }

   /**
    * Test permissions errors
    *
    * @throws Exception if the test fails
    */
   public void testNoPermisions()
        throws Exception {

      //on derby we can't have a different user other than the administrator, so this test will not run
      // read testing configuration.
      OpConfiguration configuration = OpTestDataFactory.getTestingConfiguration();
      OpDatabaseConfiguration dataBaseConfiguration = configuration.getDatabaseConfigurations().iterator().next();
      if (dataBaseConfiguration.getDatabaseType() != OpHibernateSource.DERBY) {

         String superPoolId = null;
         OpBroker broker = session.newBroker();
         try {
            superPoolId = OpResourceService.findRootPool(broker).locator();
         }
         finally {
            broker.close();
         }

         XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, superPoolId);
         XMessage response = service.insertResource(session, request);
         assertNoError(response);
         String id = dataFactory.getResourceId(NAME);

         request = dataFactory.createPoolMsg(POOL_NAME, POOL_DESCRIPTION, 2d, 2d, superPoolId);
         response = service.insertPool(session, request);
         assertNoError(response);
         String poolid = dataFactory.getResourcePoolId(POOL_NAME);

         logIn(DEFAULT_USER, DEFAULT_PASSWORD);

         //create pool
         request = dataFactory.createPoolMsg(POOL_NAME + 1, POOL_DESCRIPTION, 2d, 2d, superPoolId);
         response = service.insertPool(session, request);
         assertError(response, OpResourceError.UPDATE_ACCESS_DENIED);

         //create resource
         request = dataFactory.createResourceMsg(NAME + 1, DESCRIPTION, 50d, 2d, 2d, false, superPoolId);
         response = service.insertResource(session, request);
         assertError(response, OpResourceError.UPDATE_ACCESS_DENIED);

         //import users
         OpUserTestDataFactory userDataFactory = new OpUserTestDataFactory(session);
         OpUser user = userDataFactory.getUserByName(DEFAULT_USER);
         request = dataFactory.importUserMsg(user.locator(), 34d, 8d, 10d, false, superPoolId, null);
         response = service.importUser(session, request);
         assertError(response, OpResourceError.UPDATE_ACCESS_DENIED);

         //update pool
         request = dataFactory.updatePoolMsg(superPoolId, NEW_POOL_NAME, NEW_POOL_DESCRIPTION, new Double(5d), new Double(5d));
         response = service.updatePool(session, request);
         assertError(response, OpResourceError.UPDATE_ACCESS_DENIED);

         //update resource
         request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 80d, 7d, 3d, true, null, null);
         response = service.updateResource(session, request);
         assertError(response, OpResourceError.UPDATE_ACCESS_DENIED);

         //delete pool
         List<String> ids = new ArrayList<String>();
         ids.add(poolid);
         request = new XMessage();
         request.setArgument(OpResourceService.POOL_IDS, ids);
         response = service.deletePools(session, request);
         assertError(response, OpResourceError.MANAGER_ACCESS_DENIED);

         //delete resources
         ids.clear();
         ids.add(id);
         request = new XMessage();
         request.setArgument(OpResourceService.RESOURCE_IDS, ids);
         response = service.deleteResources(session, request);
         assertError(response, OpResourceError.MANAGER_ACCESS_DENIED);
      }
   }
}