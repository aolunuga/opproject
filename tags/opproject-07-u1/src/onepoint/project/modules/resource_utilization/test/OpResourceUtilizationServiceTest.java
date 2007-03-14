/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource_utilization.test;

import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.test.ProjectTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.resource.test.ResourceTestDataFactory;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationService;
import onepoint.project.test.OpBaseTestCase;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;

import java.util.Iterator;
import java.util.List;

/**
 * This class test resource service methods.
 *
 * @author lucian.furtos
 */
public class OpResourceUtilizationServiceTest extends OpBaseTestCase {

   private static final String NAME = "resource";
   private static final String DESCRIPTION = "The Resource Description";
   private static final String POOL_NAME = "pool";
   private static final String POOL_DESCRIPTION = "The resource pool description";

   private OpResourceUtilizationService service;
   private OpResourceService resourceService;
   private ResourceTestDataFactory dataFactory;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      service = getResourceUtilizationService();
      resourceService = getResourceService();
      dataFactory = new ResourceTestDataFactory(session);

      clean();
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
      XMessage request = dataFactory.createPoolMsg(POOL_NAME + 1, POOL_DESCRIPTION, 72d, rootId);
      XMessage response = resourceService.insertPool(session, request);
      assertNoError(response);
      String son1Id = dataFactory.getResourcePoolId(POOL_NAME + 1);
      // pool son of 1
      request = dataFactory.createPoolMsg(POOL_NAME + 11, POOL_DESCRIPTION, 33d, son1Id);
      response = resourceService.insertPool(session, request);
      assertNoError(response);
      dataFactory.getResourcePoolId(POOL_NAME + 11);

      // pool son of root
      request = dataFactory.createPoolMsg(POOL_NAME + 2, POOL_DESCRIPTION, 23d, rootId);
      response = resourceService.insertPool(session, request);
      assertNoError(response);
      String son2Id = dataFactory.getResourcePoolId(POOL_NAME + 2);
      // resource son of 2
      request = dataFactory.createResourceMsg(NAME, DESCRIPTION, 50d, 535d, false, son2Id);
      response = resourceService.insertResource(session, request);
      assertNoError(response);

      request = new XMessage();
      request.setArgument("source_pool_locator", rootId);
      request.setArgument("outlineLevel", new Integer(2));
      request.setArgument("poolColumnsSelector", null);
      request.setArgument("resourceColumnsSelector", null);
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
      response = service.expandResourcePool(session, request);
      assertNoError(response);
      children = (List) response.getArgument(OpProjectConstants.CHILDREN);
      assertNotNull(children);
      assertEquals(1, children.size());
   }

   //                           ***** Helper Methods *****

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {
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
         if (pool.locator().equals(rootId)) {
            continue;
         }
         dataFactory.deleteObject(pool);
      }
   }
}
