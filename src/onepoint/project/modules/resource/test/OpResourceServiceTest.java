/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource.test;

import onepoint.express.XComponent;
import onepoint.persistence.*;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.test.ActivityTestDataFactory;
import onepoint.project.modules.project.test.ProjectTestDataFactory;
import onepoint.project.modules.resource.*;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.user.test.UserTestDataFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.test.OpBaseTestCase;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocaleManager;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

/**
 * This class test resource service methods.
 *
 * @author lucian.furtos
 */
public class OpResourceServiceTest extends OpBaseTestCase {

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
   private ResourceTestDataFactory dataFactory;
   private ActivityTestDataFactory activityFactory;
   private static final String NEW_NAME = "new_resource";
   private XCalendar xCalendar;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      service = getResourceService();
      dataFactory = new ResourceTestDataFactory(session);
      activityFactory = new ActivityTestDataFactory(session);

      clean();

      Map userData = UserTestDataFactory.createUserData(DEFAULT_USER, DEFAULT_PASSWORD, OpUser.STANDARD_USER_LEVEL);
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = getUserService().insertUser(session, request);
      assertNoError(response);

      XCalendar.getDefaultCalendar().configure(null, XLocaleManager.getDefaultLocale(), null, XCalendar.GMT_TIMEZONE);
      xCalendar = XCalendar.getDefaultCalendar();
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
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

      ArrayList ids = new ArrayList(2);
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
    * Test happy-flow creation of resources with OpHourlyRatesPeriods
    *
    * @throws Exception if the test fails
    */
   public void testCreateResourceWithHourlyRatesPeriods()
        throws Exception {
      String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 1d, false, poolid);

      Calendar calendar = Calendar.getInstance();
      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
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

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      HashMap args = (HashMap)request.getArgument(OpResourceService.RESOURCE_DATA);
      args.put(OpResourceService.HOURLY_RATES_SET, dataSet);
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
    * Test mapping of HourlyRatesPeriods errors at creation of a resource
    *
    * @throws Exception if the test fails
    */
   public void testMapHourlyRatesPeriodsErrors()
        throws Exception {
       String poolid = OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, 0); // fake id
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 1d, false, poolid);

      Calendar calendar = Calendar.getInstance();
      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
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

      ((XComponent)dataRow1.getChild(2)).setDoubleValue(-1d);
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      HashMap args = (HashMap)request.getArgument(OpResourceService.RESOURCE_DATA);
      args.put(OpResourceService.HOURLY_RATES_SET, dataSet);
      XMessage response = service.insertResource(session, request);
      assertError(response, OpResourceError.HOURLY_RATE_NOT_VALID);

      ((XComponent)dataRow1.getChild(2)).setDoubleValue(3d);
      ((XComponent)dataRow1.getChild(3)).setDoubleValue(-1d);
      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      args = (HashMap)request.getArgument(OpResourceService.RESOURCE_DATA);
      args.put(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.EXTERNAL_RATE_NOT_VALID);

      ((XComponent)dataRow1.getChild(3)).setDoubleValue(6d);
      ((XComponent)dataRow1.getChild(0)).setDateValue(null);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      args = (HashMap)request.getArgument(OpResourceService.RESOURCE_DATA);
      args.put(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.PERIOD_START_DATE_NOT_VALID);

      calendar.set(2006, 4, 13,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      ((XComponent)dataRow1.getChild(0)).setDateValue(new Date(calendar.getTimeInMillis()));
      ((XComponent)dataRow1.getChild(1)).setDateValue(null);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      args = (HashMap)request.getArgument(OpResourceService.RESOURCE_DATA);
      args.put(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.PERIOD_END_DATE_NOT_VALID);

      calendar.set(2006, 4, 11,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      ((XComponent)dataRow1.getChild(1)).setDateValue(new Date(calendar.getTimeInMillis()));
      
      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      args = (HashMap)request.getArgument(OpResourceService.RESOURCE_DATA);
      args.put(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.PERIOD_INTERVAL_NOT_VALID);

      calendar.set(2006, 4, 20,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      ((XComponent)dataRow1.getChild(1)).setDateValue(new Date(calendar.getTimeInMillis()));

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      args = (HashMap)request.getArgument(OpResourceService.RESOURCE_DATA);
      args.put(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.insertResource(session, request);
      assertError(response, OpResourceError.DATE_INTERVAL_OVERLAP);
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

      double maxAvailability = Double.parseDouble(OpSettings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
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

      double maxAvailability = Double.parseDouble(OpSettings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
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
      XMessage request = ProjectTestDataFactory.createProjectMsg("prj1", new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = getProjectService().insertProject(session, request);
      assertNoError(response);

      request = ProjectTestDataFactory.createProjectMsg("prj2", new Date(System.currentTimeMillis()), 4000d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);

      ProjectTestDataFactory projectDataFactory = new ProjectTestDataFactory(session);
      ArrayList projects = new ArrayList();
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
    * Test happy-flow update of resources
    *
    * @throws Exception if the test fails
    */
   public void testUpdateResource()
        throws Exception {
      UserTestDataFactory userDataFactory = new UserTestDataFactory(session);
      OpUser user = userDataFactory.getUserByName(DEFAULT_USER);
      Calendar calendar = Calendar.getInstance();

      XMessage request = ProjectTestDataFactory.createProjectMsg("prj1", new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = getProjectService().insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg("prj2", new Date(System.currentTimeMillis()), 4000d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg("prj3", new Date(System.currentTimeMillis()), 2000d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);

      ProjectTestDataFactory projectDataFactory = new ProjectTestDataFactory(session);
      ArrayList projectIds = new ArrayList();
      projectIds.add(projectDataFactory.getProjectId("prj1"));
      projectIds.add(projectDataFactory.getProjectId("prj3"));

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
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

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, null, projectIds);
      response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpProjectPlanVersion planVersion = new OpProjectPlanVersion();
      planVersion.setVersionNumber(OpProjectAdministrationService.WORKING_VERSION_NUMBER);
      planVersion.setStart(new Date(System.currentTimeMillis()));
      planVersion.setFinish(new Date(System.currentTimeMillis() + 1000));
      planVersion.setProjectPlan(projectDataFactory.getProjectByName("prj1").getPlan());
      broker.makePersistent(planVersion);

      OpActivityVersion activity = new OpActivityVersion();
      activity.setPlanVersion(planVersion);
      broker.makePersistent(activity);

      OpAssignmentVersion assignment = new OpAssignmentVersion();
      assignment.setActivityVersion(activity);
      assignment.setResource(resource);
      assignment.setPlanVersion(planVersion);
      broker.makePersistent(assignment);

      t.commit();
      broker.close();

      projectIds = new ArrayList();
      projectIds.add(projectDataFactory.getProjectId("prj1"));
      projectIds.add(projectDataFactory.getProjectId("prj2"));

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 80d, 7d, 2d, true, user.locator(), projectIds, dataSet);
      response = service.updateResource(session, request);
      assertNoError(response);

      resource = dataFactory.getResourceById(id);
      assertEquals(NEW_NAME, resource.getName());
      assertEquals(NEW_DESCRIPTION, resource.getDescription());
      assertEquals(80d, resource.getAvailable(), 0d);
      broker = session.newBroker();
      OpResourcePool pool = OpResourceService.findRootPool(broker);
      assertEquals(pool.getHourlyRate(), resource.getHourlyRate(), 0d);
      assertTrue(resource.getInheritPoolRate());
      broker.close();
   }

   /**
    * Test update of resources with error
    *
    * @throws Exception if the test fails
    */
   public void testUpdateResourceWithErrors()
        throws Exception {
      XMessage request = dataFactory.updateResourceMsg(OpLocator.locatorString(OpResource.RESOURCE, 0), "", "", 0d, 0d, 0d, false, null, null, null);
      XMessage response = service.updateResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NOT_FOUND);

      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 3d, false, null);
      response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);

      request = dataFactory.updateResourceMsg(id, null, NEW_DESCRIPTION, 80d, 7d, 2d, true, null, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED);

      request = dataFactory.updateResourceMsg(id, "", NEW_DESCRIPTION, 80d, 7d, 2d, true, null, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED);

      request = dataFactory.updateResourceMsg(id, "name%", NEW_DESCRIPTION, 80d, 7d, 2d, true, null, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_VALID);

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 104d, 7d, 2d, false, null, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.AVAILABILITY_NOT_VALID);

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, -4d, 7d, 2d, false, null, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.AVAILABILITY_NOT_VALID);

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 66d, -3d, 3d, false, null, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.HOURLY_RATE_NOT_VALID);

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 66d, 3d, -3d, false, null, null, null);
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

      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 80d, 7d, 3d, true, null, null, null);
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
    * Test the updating of base costs
    *
    * @throws Exception if the test fails
    */
   public void testUpdateBaseCosts()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 100d, 4d, 5d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);

      Calendar calendar = Calendar.getInstance();

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivityVersion activityVersion1 = new OpActivityVersion();
      activityVersion1.setName("ActivityOne");
      calendar.set(2007, 6, 16, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activityVersion1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 18, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activityVersion1.setFinish(new Date(calendar.getTimeInMillis()));

      OpAssignmentVersion assignmentVersion1 = new OpAssignmentVersion();
      assignmentVersion1.setActivityVersion(activityVersion1);
      assignmentVersion1.setResource(resource);

      OpProjectPlanVersion planVersion = new OpProjectPlanVersion();
      planVersion.setVersionNumber(OpProjectAdministrationService.WORKING_VERSION_NUMBER);
      calendar.set(2007, 6, 5, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      planVersion.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      planVersion.setFinish(new Date(calendar.getTimeInMillis()));
      activityVersion1.setPlanVersion(planVersion);
      broker.makePersistent(activityVersion1);
      broker.makePersistent(assignmentVersion1);
      broker.makePersistent(planVersion);

      OpActivityVersion activityVersion2 = new OpActivityVersion();
      activityVersion2.setName("ActivityTwo");
      calendar.set(2007, 6, 22, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activityVersion2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 28, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activityVersion2.setFinish(new Date(calendar.getTimeInMillis()));
      activityVersion2.setPlanVersion(planVersion);
      OpAssignmentVersion assignmentVersion2 = new OpAssignmentVersion();
      assignmentVersion2.setActivityVersion(activityVersion2);
      assignmentVersion2.setResource(resource);
      broker.makePersistent(activityVersion2);
      broker.makePersistent(assignmentVersion2);
      t.commit();
      broker.close();

      // modified only the resource's rates
      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 100d, 2d, 3d, false,null,null, null);
      response = service.updateResource(session, request);
      assertNoError(response);

      String idActivity1 = activityFactory.getActivityVersionId(activityVersion1.getName());
      activityVersion1 = activityFactory.getActivityVersionById(idActivity1);
      String idActivity2 = activityFactory.getActivityVersionId(activityVersion2.getName());
      activityVersion2 = activityFactory.getActivityVersionById(idActivity2);

      double activity1Base = 0d;
      double activity1Proceeds = 0d;
      double activity2Base = 0d;
      double activity2Proceeds = 0d;

      Calendar calendarStart = Calendar.getInstance();
      calendarStart.set(2007, 6, 16, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      Calendar calendarEnd = Calendar.getInstance();
      calendarEnd.set(2007, 6, 18, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);
      while(!calendarStart.after(calendarEnd)){
         if(xCalendar.isWorkDay(new Date(calendarStart.getTimeInMillis()))){
            activity1Base += 2 * xCalendar.getWorkHoursPerDay();
            activity1Proceeds += 3 * xCalendar.getWorkHoursPerDay();
         }
         calendarStart.add(Calendar.DATE,1);
      }

      calendarStart = Calendar.getInstance();
      calendarStart.set(2007, 6, 22, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      calendarEnd = Calendar.getInstance();
      calendarEnd.set(2007, 6, 28, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);
      while(!calendarStart.after(calendarEnd)){
         if(xCalendar.isWorkDay(new Date(calendarStart.getTimeInMillis()))){
            activity2Base += 2 * xCalendar.getWorkHoursPerDay();
            activity2Proceeds += 3 * xCalendar.getWorkHoursPerDay();
         }
         calendarStart.add(Calendar.DATE,1);
      }

      assertEquals(activity1Base, activityVersion1.getBasePersonnelCosts(), 0d);
      assertEquals(activity1Proceeds, activityVersion1.getBaseProceeds(), 0d);
      assertEquals(activity2Base, activityVersion2.getBasePersonnelCosts(), 0d);
      assertEquals(activity2Proceeds, activityVersion2.getBaseProceeds(), 0d);

      // modified only the resource's rates
      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 100d, 1d, 2d, false,null,null, null);
      response = service.updateResource(session, request);
      assertNoError(response);

      idActivity1 = activityFactory.getActivityVersionId(activityVersion1.getName());
      activityVersion1 = activityFactory.getActivityVersionById(idActivity1);
      idActivity2 = activityFactory.getActivityVersionId(activityVersion2.getName());
      activityVersion2 = activityFactory.getActivityVersionById(idActivity2);

      activity1Base = 0d;
      activity1Proceeds = 0d;
      activity2Base = 0d;
      activity2Proceeds = 0d;

      calendarStart = Calendar.getInstance();
      calendarStart.set(2007, 6, 16, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      calendarEnd = Calendar.getInstance();
      calendarEnd.set(2007, 6, 18, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);
      while(!calendarStart.after(calendarEnd)){
         if(xCalendar.isWorkDay(new Date(calendarStart.getTimeInMillis()))){
            activity1Base += 1 * xCalendar.getWorkHoursPerDay();
            activity1Proceeds += 2 * xCalendar.getWorkHoursPerDay();
         }
         calendarStart.add(Calendar.DATE,1);
      }

      calendarStart = Calendar.getInstance();
      calendarStart.set(2007, 6, 22, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      calendarEnd = Calendar.getInstance();
      calendarEnd.set(2007, 6, 28, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);
      while(!calendarStart.after(calendarEnd)){
         if(xCalendar.isWorkDay(new Date(calendarStart.getTimeInMillis()))){
            activity2Base += 1 * xCalendar.getWorkHoursPerDay();
            activity2Proceeds += 2 * xCalendar.getWorkHoursPerDay();
         }
         calendarStart.add(Calendar.DATE,1);
      }

      assertEquals(activity1Base, activityVersion1.getBasePersonnelCosts(), 0d);
      assertEquals(activity1Proceeds, activityVersion1.getBaseProceeds(), 0d);
      assertEquals(activity2Base, activityVersion2.getBasePersonnelCosts(), 0d);
      assertEquals(activity2Proceeds, activityVersion2.getBaseProceeds(), 0d);

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,14,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,17,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(5d);
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(2d);
      dataRow1.addChild(dataCell);

      XComponent dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,18,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,28,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(4d);
      dataRow2.addChild(dataCell);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);

      //added an hourly rates period that covers two days from the first activity
      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 100d, 1d, 2d, false,null,null,dataSet);
      response = service.updateResource(session, request);
      assertNoError(response);

      idActivity1 = activityFactory.getActivityVersionId(activityVersion1.getName());
      activityVersion1 = activityFactory.getActivityVersionById(idActivity1);
      idActivity2 = activityFactory.getActivityVersionId(activityVersion2.getName());
      activityVersion2 = activityFactory.getActivityVersionById(idActivity2);

      activity1Base = 0d;
      activity1Proceeds = 0d;

      calendar.set(2007, 6, 16, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      if (xCalendar.isWorkDay(new Date(calendar.getTimeInMillis()))) {
         activity1Base += 5 * xCalendar.getWorkHoursPerDay();
         activity1Proceeds += 2 * xCalendar.getWorkHoursPerDay();
      }
      calendar.add(Calendar.DATE, 1);
      if (xCalendar.isWorkDay(new Date(calendar.getTimeInMillis()))) {
         activity1Base += 5 * xCalendar.getWorkHoursPerDay();
         activity1Proceeds += 2 * xCalendar.getWorkHoursPerDay();
      }
      calendar.add(Calendar.DATE, 1);
      if (xCalendar.isWorkDay(new Date(calendar.getTimeInMillis()))) {
         activity1Base += 1 * xCalendar.getWorkHoursPerDay();
         activity1Proceeds += 2 * xCalendar.getWorkHoursPerDay();
      }

      assertEquals(activity1Base, activityVersion1.getBasePersonnelCosts(), 0d);
      assertEquals(activity1Proceeds, activityVersion1.getBaseProceeds(), 0d);
      assertEquals(activity2Base, activityVersion2.getBasePersonnelCosts(), 0d);
      assertEquals(activity2Proceeds, activityVersion2.getBaseProceeds(), 0d);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      //added two hourly rates periods: one that covers two days from the first activity
      //                              : one that covers the last day of the first activity and
      //                                  all the days from the second activity
      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 100d, 1d, 2d, false,null,null,dataSet);
      response = service.updateResource(session, request);
      assertNoError(response);

      idActivity1 = activityFactory.getActivityVersionId(activityVersion1.getName());
      activityVersion1 = activityFactory.getActivityVersionById(idActivity1);
      idActivity2 = activityFactory.getActivityVersionId(activityVersion2.getName());
      activityVersion2 = activityFactory.getActivityVersionById(idActivity2);

      activity1Base = 0d;
      activity1Proceeds = 0d;
      activity2Base = 0d;
      activity2Proceeds = 0d;

      calendar.set(2007, 6, 16, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      if (xCalendar.isWorkDay(new Date(calendar.getTimeInMillis()))) {
         activity1Base += 5 * xCalendar.getWorkHoursPerDay();
         activity1Proceeds += 2 * xCalendar.getWorkHoursPerDay();
      }
      calendar.add(Calendar.DATE, 1);
      if (xCalendar.isWorkDay(new Date(calendar.getTimeInMillis()))) {
         activity1Base += 5 * xCalendar.getWorkHoursPerDay();
         activity1Proceeds += 2 * xCalendar.getWorkHoursPerDay();
      }
      calendar.add(Calendar.DATE, 1);
      if (xCalendar.isWorkDay(new Date(calendar.getTimeInMillis()))) {
         activity1Base += 3 * xCalendar.getWorkHoursPerDay();
         activity1Proceeds += 4 * xCalendar.getWorkHoursPerDay();
      }

      calendarStart = Calendar.getInstance();
      calendarStart.set(2007, 6, 22, 0, 0, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      calendarEnd = Calendar.getInstance();
      calendarEnd.set(2007, 6, 28, 0, 0, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);
      while(!calendarStart.after(calendarEnd)){
         if(xCalendar.isWorkDay(new Date(calendarStart.getTimeInMillis()))){
            activity2Base += 3 * xCalendar.getWorkHoursPerDay();
            activity2Proceeds += 4 * xCalendar.getWorkHoursPerDay();
         }
         calendarStart.add(Calendar.DATE,1);
      }

      assertEquals(activity1Base, activityVersion1.getBasePersonnelCosts(), 0d);
      assertEquals(activity1Proceeds, activityVersion1.getBaseProceeds(), 0d);
      assertEquals(activity2Base, activityVersion2.getBasePersonnelCosts(), 0d);
      assertEquals(activity2Proceeds, activityVersion2.getBaseProceeds(), 0d);
   }

   /**
    * Test the updating of actual costs
    *
    * @throws Exception if the test fails
    */
   public void testUpdateActualCosts()
        throws Exception {

      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 100d, 4d, 5d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);

      Calendar calendar = Calendar.getInstance();

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity1 = new OpActivity();
      activity1.setName("ActivityOne");
      calendar.set(2007, 6, 16, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 18, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setFinish(new Date(calendar.getTimeInMillis()));

      OpAssignment assignment1 = new OpAssignment();
      assignment1.setActivity(activity1);
      assignment1.setResource(resource);

      broker.makePersistent(activity1);
      broker.makePersistent(assignment1);

      OpActivity activity2 = new OpActivity();
      activity2.setName("ActivityTwo");
      calendar.set(2007, 6, 22, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 28, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setFinish(new Date(calendar.getTimeInMillis()));
      OpAssignment assignment2 = new OpAssignment();
      assignment2.setActivity(activity2);
      assignment2.setResource(resource);

      broker.makePersistent(activity2);
      broker.makePersistent(assignment2);

      OpWorkRecord workRecord1 = new OpWorkRecord();
      workRecord1.setAssignment(assignment1);
      workRecord1.setActualEffort(3d);
      OpWorkRecord workRecord2 = new OpWorkRecord();
      workRecord2.setAssignment(assignment1);
      workRecord2.setActualEffort(2d);
      OpWorkRecord workRecord3 = new OpWorkRecord();
      workRecord3.setAssignment(assignment2);
      workRecord3.setActualEffort(4d);
      OpWorkRecord workRecord4 = new OpWorkRecord();
      workRecord4.setAssignment(assignment2);
      workRecord4.setActualEffort(2d);

      OpWorkSlip workSlip1 = new OpWorkSlip();
      calendar.set(2007, 6, 16, 0, 0, 0);
      workSlip1.setDate(new Date(calendar.getTimeInMillis()));
      workRecord1.setWorkSlip(workSlip1);
      workRecord3.setWorkSlip(workSlip1);
      OpWorkSlip workSlip2 = new OpWorkSlip();
      calendar.set(2007, 6, 22, 0, 0, 0);
      workSlip2.setDate(new Date(calendar.getTimeInMillis()));
      workRecord2.setWorkSlip(workSlip2);
      workRecord4.setWorkSlip(workSlip2);

      broker.makePersistent(workRecord1);
      broker.makePersistent(workRecord2);
      broker.makePersistent(workRecord3);
      broker.makePersistent(workRecord4);
      broker.makePersistent(workSlip1);
      broker.makePersistent(workSlip2);

      t.commit();
      broker.close();

      // modified only the resource's rates
      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 100d, 2d, 3d, false, null, null, null);
      response = service.updateResource(session, request);
      assertNoError(response);

      String idActivity1 = activityFactory.getActivityId(activity1.getName());
      activity1 = activityFactory.getActivityById(idActivity1);
      String idActivity2 = activityFactory.getActivityId(activity2.getName());
      activity2 = activityFactory.getActivityById(idActivity2);

      assertEquals(10d, activity1.getActualPersonnelCosts(), 0d);
      assertEquals(15d, activity1.getActualProceeds(), 0d);
      assertEquals(12d, activity2.getActualPersonnelCosts(), 0d);
      assertEquals(18d, activity2.getActualProceeds(), 0d);

      // modified only the resource's rates
      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 100d, 1d, 2d, false,null,null, null);
      response = service.updateResource(session, request);
      assertNoError(response);

      idActivity1 = activityFactory.getActivityId(activity1.getName());
      activity1 = activityFactory.getActivityById(idActivity1);
      idActivity2 = activityFactory.getActivityId(activity2.getName());
      activity2 = activityFactory.getActivityById(idActivity2);

      assertEquals(5d, activity1.getActualPersonnelCosts(), 0d);
      assertEquals(10d, activity1.getActualProceeds(), 0d);
      assertEquals(6d, activity2.getActualPersonnelCosts(), 0d);
      assertEquals(12d, activity2.getActualProceeds(), 0d);

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,14,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,17,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(5d);
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(2d);
      dataRow1.addChild(dataCell);

      XComponent dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,18,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,28,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(4d);
      dataRow2.addChild(dataCell);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);

      //added an hourly rates period which includes the first work slip
      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 100d, 1d, 2d, false,null,null,dataSet);
      response = service.updateResource(session, request);
      assertNoError(response);

      idActivity1 = activityFactory.getActivityId(activity1.getName());
      activity1 = activityFactory.getActivityById(idActivity1);
      idActivity2 = activityFactory.getActivityId(activity2.getName());
      activity2 = activityFactory.getActivityById(idActivity2);

      assertEquals(17d, activity1.getActualPersonnelCosts(), 0d);
      assertEquals(10d, activity1.getActualProceeds(), 0d);
      assertEquals(22d, activity2.getActualPersonnelCosts(), 0d);
      assertEquals(12d, activity2.getActualProceeds(), 0d);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      //added two hourly rates periods: one which includes the first work slip
      //                              : one which includes the second work slip
      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 100d, 1d, 2d, false,null,null,dataSet);
      response = service.updateResource(session, request);
      assertNoError(response);

      idActivity1 = activityFactory.getActivityId(activity1.getName());
      activity1 = activityFactory.getActivityById(idActivity1);
      idActivity2 = activityFactory.getActivityId(activity2.getName());
      activity2 = activityFactory.getActivityById(idActivity2);

      assertEquals(21d, activity1.getActualPersonnelCosts(), 0d);
      assertEquals(14d, activity1.getActualProceeds(), 0d);
      assertEquals(26d, activity2.getActualPersonnelCosts(), 0d);
      assertEquals(16d, activity2.getActualProceeds(), 0d);
   }

   /**
    * Test the user import
    *
    * @throws Exception if the test fails
    */
   public void testImportUser()
        throws Exception {
      UserTestDataFactory userDataFactory = new UserTestDataFactory(session);
      OpUser user = userDataFactory.getUserByName(DEFAULT_USER);

      XMessage request = ProjectTestDataFactory.createProjectMsg("prj1", new Date(System.currentTimeMillis()), 1000d, null, null);
      XMessage response = getProjectService().insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg("prj2", new Date(System.currentTimeMillis()), 4000d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);

      ProjectTestDataFactory projectDataFactory = new ProjectTestDataFactory(session);
      ArrayList projectIds = new ArrayList();
      projectIds.add(projectDataFactory.getProjectId("prj1"));
      projectIds.add(projectDataFactory.getProjectId("prj2"));

      request = dataFactory.importUserMsg(user.locator(), 34d, 8d, false, null, projectIds);
      response = service.importUser(session, request);
      assertNoError(response);

      OpResource res = dataFactory.getResourceByName(DEFAULT_USER);
      assertNotNull(res);
      assertEquals(res.getDescription(), null);
      assertEquals(res.getAvailable(), 34d, 0d);
      assertEquals(res.getHourlyRate(), 8d, 0d);
      assertFalse(res.getInheritPoolRate());
   }

   /**
    * Test unknown user import
    *
    * @throws Exception if the test fails
    */
   public void testImportUnknownUser()
        throws Exception {
      XMessage request = dataFactory.importUserMsg(null, 34d, 8d, false, null, null);
      XMessage response = service.importUser(session, request);
      assertError(response, OpResourceError.USER_ID_NOT_SPECIFIED);

      request = dataFactory.importUserMsg("", 34d, 8d, false, null, null);
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
      UserTestDataFactory userDataFactory = new UserTestDataFactory(session);
      OpUser user = userDataFactory.getUserByName(DEFAULT_USER);

      XMessage request = dataFactory.importUserMsg(user.locator(), -4d, 8d, false, null, null);
      XMessage response = service.importUser(session, request);
      assertError(response, OpResourceError.AVAILABILITY_NOT_VALID);

      request = dataFactory.importUserMsg(user.locator(), 101d, 8d, false, null, null);
      response = service.importUser(session, request);
      assertError(response, OpResourceError.AVAILABILITY_NOT_VALID);

      request = dataFactory.importUserMsg(user.locator(), 50d, -4d, false, null, null);
      response = service.importUser(session, request);
      assertError(response, OpResourceError.HOURLY_RATE_NOT_VALID);
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

      UserTestDataFactory userDataFactory = new UserTestDataFactory(session);
      OpUser user = userDataFactory.getUserByName(DEFAULT_USER);

      request = dataFactory.importUserMsg(user.locator(), 34d, 8d, false, null, null);
      response = service.importUser(session, request);
      assertError(response, OpResourceError.RESOURCE_NAME_NOT_UNIQUE);
   }

   /**
    * Test if the resource has assignments
    *
    * @throws Exception if the test fails
    */
   public void testHasAssignments()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      response = service.hasAssignments(session, request);
      assertNotNull(response);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS)).booleanValue());

      Calendar calendar = Calendar.getInstance();

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpActivity activity1 = new OpActivity();
      calendar.set(2007, 6, 16, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 18, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setFinish(new Date(calendar.getTimeInMillis()));
      OpAssignment assignment1 = new OpAssignment();
      assignment1.setActivity(activity1);
      assignment1.setResource(resource);
      broker.makePersistent(activity1);
      broker.makePersistent(assignment1);

      OpActivity activity2 = new OpActivity();
      calendar.set(2007, 6, 22, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2007, 6, 28, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setFinish(new Date(calendar.getTimeInMillis()));
      OpAssignment assignment2 = new OpAssignment();
      assignment2.setActivity(activity2);
      assignment2.setResource(resource);
      broker.makePersistent(activity2);
      broker.makePersistent(assignment2);
      t.commit();
      broker.close();

      //activities are uncovered - return true
      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      response = service.hasAssignments(session, request);
      assertNotNull(response);
      assertTrue(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS)).booleanValue());

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,14,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,17,0,0,0);
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
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,18,0,0,0);;
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2007,6,28,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(12d);
      dataRow2.addChild(dataCell);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      //the activities are covered by hourly rates periods - return false
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignments(session, request);
      assertNotNull(response);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS)).booleanValue());

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow2);
      
      //the activities are not covered by hourly rates periods - return true
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignments(session, request);
      assertNotNull(response);
      assertTrue(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS)).booleanValue());

   }

   /**
    * Test if the resource has activity assignments in the time periods of it's OpHourlyRatesPeriods
    *
    * @throws Exception if the test fails
    */
   public void testHasAssignmentsWithActivityInTimePeriod()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);
      Calendar calendar = Calendar.getInstance();

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpActivity activity = new OpActivity();
      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resource);
      calendar.set(2006,4,26,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      activity.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006,4,27,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      activity.setFinish(new Date(calendar.getTimeInMillis()));
      OpActivityVersion version = new OpActivityVersion();
      version.setActivity(activity);
      broker.makePersistent(activity);
      broker.makePersistent(assignment);
      broker.makePersistent(version);

      t.commit();
      broker.close();

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,20,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,25,0,0,0);
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
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,26,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,28,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(12d);
      dataRow2.addChild(dataCell);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertTrue(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());
   }

   /**
    * Test if the resource has assignment version in the time periods of it's OpHourlyRatesPeriods
    *
    * @throws Exception if the test fails
    */
   public void testHasAssignmentsWithVersionInTimePeriod()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);
      Calendar calendar = Calendar.getInstance();

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpAssignmentVersion assignment = new OpAssignmentVersion();
      OpActivityVersion version = new OpActivityVersion();
      assignment.setActivityVersion(version);
      assignment.setResource(resource);
      calendar.set(2006,4,26,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      version.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006,4,27,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      version.setFinish(new Date(calendar.getTimeInMillis()));
      broker.makePersistent(assignment);
      broker.makePersistent(version);

      t.commit();
      broker.close();

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,20,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,25,0,0,0);
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
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,26,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,28,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(12d);
      dataRow2.addChild(dataCell);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertTrue(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());
   }

   /**
    * Test if the resource has assignment version in the time periods of it's OpHourlyRatesPeriods
    * with false rsults
    *
    * @throws Exception if the test fails
    */
   public void testDoesntHaveAssignmentsWithVersionInTimePeriod()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);
      Calendar calendar = Calendar.getInstance();

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpAssignmentVersion assignment = new OpAssignmentVersion();
      OpActivityVersion version = new OpActivityVersion();
      assignment.setActivityVersion(version);
      assignment.setResource(resource);
      calendar.set(2006,4,26,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      version.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006,4,27,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      version.setFinish(new Date(calendar.getTimeInMillis()));
      broker.makePersistent(assignment);
      broker.makePersistent(version);

      t.commit();
      broker.close();

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,20,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,25,0,0,0);
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
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,18,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006,4,20,0,0,0);
      calendar.set(Calendar.MILLISECOND,0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(12d);
      dataRow2.addChild(dataCell);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());
   }

   /**
    * Test if the resource has activity assignments in the time periods of it's OpHourlyRatesPeriods
    *
    * @throws Exception if the test fails
    */
   public void testHasAssignmentsInTimePeriod()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);
      Calendar calendar = Calendar.getInstance();

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpActivity activity1 = new OpActivity();
      OpAssignment assignment1 = new OpAssignment();
      assignment1.setActivity(activity1);
      assignment1.setResource(resource);
      calendar.set(2006, 3, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setFinish(new Date(calendar.getTimeInMillis()));
      OpActivityVersion version1 = new OpActivityVersion();
      version1.setActivity(activity1);
      broker.makePersistent(activity1);
      broker.makePersistent(assignment1);
      broker.makePersistent(version1);

      OpActivity activity2 = new OpActivity();
      OpAssignment assignment2 = new OpAssignment();
      assignment2.setActivity(activity2);
      assignment2.setResource(resource);
      calendar.set(2006, 4, 7, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setFinish(new Date(calendar.getTimeInMillis()));
      OpActivityVersion version2 = new OpActivityVersion();
      version2.setActivity(activity2);
      broker.makePersistent(activity2);
      broker.makePersistent(assignment2);
      broker.makePersistent(version2);

      OpActivity activity3 = new OpActivity();
      OpAssignment assignment3 = new OpAssignment();
      assignment3.setActivity(activity3);
      assignment3.setResource(resource);
      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity3.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity3.setFinish(new Date(calendar.getTimeInMillis()));
      OpActivityVersion version3 = new OpActivityVersion();
      version3.setActivity(activity3);
      broker.makePersistent(activity3);
      broker.makePersistent(assignment3);
      broker.makePersistent(version3);

      OpHourlyRatesPeriod hourlyRatesPeriod1 = new OpHourlyRatesPeriod();
      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 24, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod1.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod1.setInternalRate(9d);
      hourlyRatesPeriod1.setExternalRate(3d);
      hourlyRatesPeriod1.setResource(resource);

      OpHourlyRatesPeriod hourlyRatesPeriod2 = new OpHourlyRatesPeriod();
      calendar.set(2006, 3, 29, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod2.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod2.setInternalRate(6d);
      hourlyRatesPeriod2.setExternalRate(0d);
      hourlyRatesPeriod2.setResource(resource);

      OpHourlyRatesPeriod hourlyRatesPeriod3 = new OpHourlyRatesPeriod();
      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod3.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod3.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod3.setInternalRate(2d);
      hourlyRatesPeriod3.setExternalRate(2d);
      hourlyRatesPeriod3.setResource(resource);

      broker.makePersistent(hourlyRatesPeriod1);
      broker.makePersistent(hourlyRatesPeriod2);
      broker.makePersistent(hourlyRatesPeriod3);

      t.commit();
      broker.close();

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 24, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      dataRow1.addChild(dataCell);


      XComponent dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 29, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0d);
      dataRow2.addChild(dataCell);

      XComponent dataRow3 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(2d);
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(2d);
      dataRow3.addChild(dataCell);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      dataSet.addChild(dataRow3);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //expanded the second interval to include the second activity interval
      dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0d);
      dataRow2.addChild(dataCell);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      dataSet.addChild(dataRow3);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertTrue(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //deleted the third interval but the default value for the missing period is the same with the value from the
      //deleted interval
      dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 29, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0d);
      dataRow2.addChild(dataCell);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //deleted the second interval which intersects no activity interval
      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow3);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //insert a new interval which covers part of the third activity period but with different rates
      XComponent dataRow4 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 1, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow4.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 10, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow4.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      dataRow4.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(5d);
      dataRow4.addChild(dataCell);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      dataSet.addChild(dataRow3);
      dataSet.addChild(dataRow4);
      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertTrue(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //deleted the first interval which causes the first activity to change rates
      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow2);
      dataSet.addChild(dataRow3);
      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertTrue(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());
   }

   /**
    * Test if the resource has activity assignments in the time periods of it's OpHourlyRatesPeriods
    *
    * @throws Exception if the test fails
    */
   public void testHasAssignmentsInTimePeriodReduceIntervalDontAffectActivity()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);
      Calendar calendar = Calendar.getInstance();

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpActivity activity1 = new OpActivity();
      OpAssignment assignment1 = new OpAssignment();
      assignment1.setActivity(activity1);
      assignment1.setResource(resource);
      calendar.set(2006, 3, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setFinish(new Date(calendar.getTimeInMillis()));
      OpActivityVersion version1 = new OpActivityVersion();
      version1.setActivity(activity1);
      broker.makePersistent(activity1);
      broker.makePersistent(assignment1);
      broker.makePersistent(version1);

      OpActivity activity2 = new OpActivity();
      OpAssignment assignment2 = new OpAssignment();
      assignment2.setActivity(activity2);
      assignment2.setResource(resource);
      calendar.set(2006, 4, 7, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setFinish(new Date(calendar.getTimeInMillis()));
      OpActivityVersion version2 = new OpActivityVersion();
      version2.setActivity(activity2);
      broker.makePersistent(activity2);
      broker.makePersistent(assignment2);
      broker.makePersistent(version2);

      OpActivity activity3 = new OpActivity();
      OpAssignment assignment3 = new OpAssignment();
      assignment3.setActivity(activity3);
      assignment3.setResource(resource);
      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity3.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity3.setFinish(new Date(calendar.getTimeInMillis()));
      OpActivityVersion version3 = new OpActivityVersion();
      version3.setActivity(activity3);
      broker.makePersistent(activity3);
      broker.makePersistent(assignment3);
      broker.makePersistent(version3);

      OpHourlyRatesPeriod hourlyRatesPeriod1 = new OpHourlyRatesPeriod();
      calendar.set(2006, 3, 16, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 24, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod1.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod1.setInternalRate(9d);
      hourlyRatesPeriod1.setExternalRate(3d);
      hourlyRatesPeriod1.setResource(resource);

      OpHourlyRatesPeriod hourlyRatesPeriod2 = new OpHourlyRatesPeriod();
      calendar.set(2006, 3, 29, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod2.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod2.setInternalRate(6d);
      hourlyRatesPeriod2.setExternalRate(0d);
      hourlyRatesPeriod2.setResource(resource);

      OpHourlyRatesPeriod hourlyRatesPeriod3 = new OpHourlyRatesPeriod();
      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod3.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod3.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod3.setInternalRate(2d);
      hourlyRatesPeriod3.setExternalRate(2d);
      hourlyRatesPeriod3.setResource(resource);

      broker.makePersistent(hourlyRatesPeriod1);
      broker.makePersistent(hourlyRatesPeriod2);
      broker.makePersistent(hourlyRatesPeriod3);

      t.commit();
      broker.close();

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 16, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 24, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      dataRow1.addChild(dataCell);


      XComponent dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 29, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0d);
      dataRow2.addChild(dataCell);

      XComponent dataRow3 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(2d);
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(2d);
      dataRow3.addChild(dataCell);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      dataSet.addChild(dataRow3);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //reduce the first interval but do not affect the first activity
      dataRow1 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 24, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      dataRow1.addChild(dataCell);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      dataSet.addChild(dataRow3);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());
   }

   /**
    * Test if the resource has activity assignments in the time periods of it's OpHourlyRatesPeriods
    *
    * @throws Exception if the test fails
    */
   public void testHasAssignmentsInTimePeriodReduceIntervalAffectActivity()
        throws Exception {
      XMessage request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, null);
      XMessage response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);
      Calendar calendar = Calendar.getInstance();

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpActivity activity1 = new OpActivity();
      OpAssignment assignment1 = new OpAssignment();
      assignment1.setActivity(activity1);
      assignment1.setResource(resource);
      calendar.set(2006, 3, 25, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 27, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity1.setFinish(new Date(calendar.getTimeInMillis()));
      OpActivityVersion version1 = new OpActivityVersion();
      version1.setActivity(activity1);
      broker.makePersistent(activity1);
      broker.makePersistent(assignment1);
      broker.makePersistent(version1);

      OpActivity activity2 = new OpActivity();
      OpAssignment assignment2 = new OpAssignment();
      assignment2.setActivity(activity2);
      assignment2.setResource(resource);
      calendar.set(2006, 4, 7, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity2.setFinish(new Date(calendar.getTimeInMillis()));
      OpActivityVersion version2 = new OpActivityVersion();
      version2.setActivity(activity2);
      broker.makePersistent(activity2);
      broker.makePersistent(assignment2);
      broker.makePersistent(version2);

      OpActivity activity3 = new OpActivity();
      OpAssignment assignment3 = new OpAssignment();
      assignment3.setActivity(activity3);
      assignment3.setResource(resource);
      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity3.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 21, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      activity3.setFinish(new Date(calendar.getTimeInMillis()));
      OpActivityVersion version3 = new OpActivityVersion();
      version3.setActivity(activity3);
      broker.makePersistent(activity3);
      broker.makePersistent(assignment3);
      broker.makePersistent(version3);

      OpHourlyRatesPeriod hourlyRatesPeriod1 = new OpHourlyRatesPeriod();
      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod1.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 24, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod1.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod1.setInternalRate(9d);
      hourlyRatesPeriod1.setExternalRate(3d);
      hourlyRatesPeriod1.setResource(resource);

      OpHourlyRatesPeriod hourlyRatesPeriod2 = new OpHourlyRatesPeriod();
      calendar.set(2006, 3, 26, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod2.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod2.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod2.setInternalRate(6d);
      hourlyRatesPeriod2.setExternalRate(0d);
      hourlyRatesPeriod2.setResource(resource);

      OpHourlyRatesPeriod hourlyRatesPeriod3 = new OpHourlyRatesPeriod();
      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod3.setStart(new Date(calendar.getTimeInMillis()));
      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      hourlyRatesPeriod3.setFinish(new Date(calendar.getTimeInMillis()));
      hourlyRatesPeriod3.setInternalRate(2d);
      hourlyRatesPeriod3.setExternalRate(2d);
      hourlyRatesPeriod3.setResource(resource);

      broker.makePersistent(hourlyRatesPeriod1);
      broker.makePersistent(hourlyRatesPeriod2);
      broker.makePersistent(hourlyRatesPeriod3);

      t.commit();
      broker.close();

      XComponent dataRow1 = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 20, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 24, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(9d);
      dataRow1.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(3d);
      dataRow1.addChild(dataCell);


      XComponent dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 26, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0d);
      dataRow2.addChild(dataCell);

      XComponent dataRow3 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 13, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 4, 19, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(2d);
      dataRow3.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(2d);
      dataRow3.addChild(dataCell);

      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      dataSet.addChild(dataRow3);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());

      //reduce the second interval so that it affects the second activity
      dataRow2 = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 29, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      calendar.set(2006, 3, 30, 0, 0, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      dataCell.setDateValue(new Date(calendar.getTimeInMillis()));
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(6d);
      dataRow2.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0d);
      dataRow2.addChild(dataCell);

      dataSet = new XComponent(XComponent.DATA_SET);
      dataSet.addChild(dataRow1);
      dataSet.addChild(dataRow2);
      dataSet.addChild(dataRow3);

      request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.HOURLY_RATES_SET, dataSet);
      response = service.hasAssignmentsInTimePeriod(session, request);
      assertTrue(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS_IN_TIME_PERIOD)).booleanValue());
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

      String poolid = dataFactory.getResourcePoolId(POOL_NAME);

      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 2d, 2d, false, poolid);
      response = service.insertResource(session, request);
      assertNoError(response);

      String id = dataFactory.getResourceId(NAME);

      request = new XMessage();
      request.setArgument(OpResourceService.POOL_ID, poolid);
      response = service.hasResourceAssignments(session, request);
      assertFalse(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS)).booleanValue());

      OpResource resource = dataFactory.getResourceById(id);
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      OpActivity activity = new OpActivity();
      OpAssignment assignment = new OpAssignment();
      assignment.setActivity(activity);
      assignment.setResource(resource);
      OpActivityVersion version = new OpActivityVersion();
      version.setActivity(activity);
      broker.makePersistent(activity);
      broker.makePersistent(assignment);
      broker.makePersistent(version);
      t.commit();
      broker.close();

      request = new XMessage();
      request.setArgument(OpResourceService.POOL_ID, poolid);
      response = service.hasResourceAssignments(session, request);
      assertTrue(((Boolean) response.getArgument(OpResourceService.HAS_ASSIGNMENTS)).booleanValue());
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
      String poolid = dataFactory.getResourcePoolId(POOL_NAME);
      request = dataFactory.createResourceMsg(NAME + 1, DESCRIPTION, 50d, 2d, 2d, true, poolid);
      response = service.insertResource(session, request);
      assertNoError(response);

      request = dataFactory.createResourceMsg(NAME + 2, DESCRIPTION, 99d, 0d, 0d, false, null);
      response = service.insertResource(session, request);
      assertNoError(response);
      String resourceid = dataFactory.getResourceId(NAME + 2);

      ArrayList resouceIds = new ArrayList();
      resouceIds.add(poolid);
      resouceIds.add(resourceid);

      request = ProjectTestDataFactory.createProjectMsg("prj1", new Date(System.currentTimeMillis()), 1000d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);
      request = ProjectTestDataFactory.createProjectMsg("prj2", new Date(System.currentTimeMillis()), 4000d, null, null);
      response = getProjectService().insertProject(session, request);
      assertNoError(response);

      ProjectTestDataFactory projectDataFactory = new ProjectTestDataFactory(session);
      ArrayList projectIds = new ArrayList();
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

      ArrayList ids = new ArrayList();
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

      ArrayList ids = new ArrayList();
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
      OpBroker broker = session.newBroker();
      String superPoolId = OpResourceService.findRootPool(broker).locator();
      broker.close();

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
      UserTestDataFactory userDataFactory = new UserTestDataFactory(session);
      OpUser user = userDataFactory.getUserByName(DEFAULT_USER);
      request = dataFactory.importUserMsg(user.locator(), 34d, 8d, false, superPoolId, null);
      response = service.importUser(session, request);
      assertError(response, OpResourceError.UPDATE_ACCESS_DENIED);

      //update pool
      request = dataFactory.updatePoolMsg(superPoolId, NEW_POOL_NAME, NEW_POOL_DESCRIPTION, new Double(5d), new Double(5d));
      response = service.updatePool(session, request);
      assertError(response, OpResourceError.UPDATE_ACCESS_DENIED);

      //update resource
      request = dataFactory.updateResourceMsg(id, NEW_NAME, NEW_DESCRIPTION, 80d, 7d, 3d, true, null, null, null);
      response = service.updateResource(session, request);
      assertError(response, OpResourceError.UPDATE_ACCESS_DENIED);

      //delete pool
      ArrayList ids = new ArrayList();
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

   //                           ***** Helper Methods *****

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
//
//      dataFactory.deleteAllActivities();

      deleteAllObjects(OpAssignmentVersion.ASSIGNMENT_VERSION);
      deleteAllObjects(OpProjectPlanVersion.PROJECT_PLAN_VERSION);
      deleteAllObjects(OpActivityVersion.ACTIVITY_VERSION);
      deleteAllObjects(OpWorkRecord.WORK_RECORD);
      deleteAllObjects(OpWorkSlip.WORK_SLIP);
      deleteAllObjects(OpAssignment.ASSIGNMENT);
      deleteAllObjects(OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(OpActivity.ACTIVITY);

      ProjectTestDataFactory projectDataFactory = new ProjectTestDataFactory(session);
      List projectList = projectDataFactory.getAllProjects();
      for (Iterator iterator = projectList.iterator(); iterator.hasNext();) {
         OpProjectNode project = (OpProjectNode) iterator.next();
         projectDataFactory.deleteObject(project);
      }

      List resoucesList = dataFactory.getAllResources();
      for (Iterator iterator = resoucesList.iterator(); iterator.hasNext();) {
         OpResource resource = (OpResource) iterator.next();
         dataFactory.deleteObject(resource);
      }

      List poolList = dataFactory.getAllResourcePools();
      for (Iterator iterator = poolList.iterator(); iterator.hasNext();) {
         OpResourcePool pool = (OpResourcePool) iterator.next();
         if (pool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            continue;
         }
         dataFactory.deleteObject(pool);
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
