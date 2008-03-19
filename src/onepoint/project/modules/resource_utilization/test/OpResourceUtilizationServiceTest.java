/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource_utilization.test;

import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationService;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;

import java.util.List;

/**
 * This class test resource service methods.
 *
 * @author lucian.furtos
 */
public class OpResourceUtilizationServiceTest extends OpBaseOpenTestCase {

   private static final String NAME = "resource";
   private static final String DESCRIPTION = "The Resource Description";
   private static final String POOL_NAME = "pool";
   private static final String POOL_DESCRIPTION = "The resource pool description";

   private OpResourceUtilizationService service;
   private OpResourceService resourceService;
   private OpResourceTestDataFactory dataFactory;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      service = OpTestDataFactory.getResourceUtilizationService();
      resourceService = OpTestDataFactory.getResourceService();
      dataFactory = new OpResourceTestDataFactory(session);
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
    * Test
    *
    * @throws Exception if the test fails
    */
   public void testExpandResourcePool()
        throws Exception {
      // pool son of root
      XMessage request = dataFactory.createPoolMsg(POOL_NAME + 1, POOL_DESCRIPTION, 72d, 36d, rootId);
      XMessage response = resourceService.insertPool(session, request);
      assertNoError(response);

      String son1Id = dataFactory.getResourcePoolId(POOL_NAME + 1);
      // pool son of 1
      request = dataFactory.createPoolMsg(POOL_NAME + 11, POOL_DESCRIPTION, 33d, 50d, son1Id);
      response = resourceService.insertPool(session, request);
      assertNoError(response);
      dataFactory.getResourcePoolId(POOL_NAME + 11);

      // pool son of root
      request = dataFactory.createPoolMsg(POOL_NAME + 2, POOL_DESCRIPTION, 23d, 25d, rootId);
      response = resourceService.insertPool(session, request);
      assertNoError(response);
      String son2Id = dataFactory.getResourcePoolId(POOL_NAME + 2);
      // resource son of 2
      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 535d, 31d, false, son2Id);
      response = resourceService.insertResource(session, request);
      assertNoError(response);

      request = new XMessage();
      request.setArgument("source_pool_locator", rootId);
      request.setArgument("outlineLevel", new Integer(2));
      request.setArgument("poolColumnsSelector", null);
      request.setArgument("resourceColumnsSelector", null);
      request.setArgument(OpResourceUtilizationService.PROBABILITY_CHOICE_ID, "0");
      response = service.expandResourcePool(session, request);
      assertNoError(response);
      List children = (List) response.getArgument(OpProjectConstants.CHILDREN);
      assertNotNull(children);
      assertEquals(4, children.size());

      request = new XMessage();
      request.setArgument("source_pool_locator", son1Id);
      request.setArgument("outlineLevel", new Integer(2));
      request.setArgument("poolColumnsSelector", null);
      request.setArgument("resourceColumnsSelector", null);
      request.setArgument(OpResourceUtilizationService.PROBABILITY_CHOICE_ID, "0");
      response = service.expandResourcePool(session, request);
      assertNoError(response);
      children = (List) response.getArgument(OpProjectConstants.CHILDREN);
      assertNotNull(children);
      assertEquals(1, children.size());
   }
}
