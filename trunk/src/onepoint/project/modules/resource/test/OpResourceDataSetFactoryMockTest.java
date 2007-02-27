/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.test.OpBaseMockTestCase;
import onepoint.resource.XLocalizer;
import org.jmock.core.Constraint;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Test case class for OpResourceDataSetFactory. Will test the functionality of the helper class using mock objects
 *
 * @author ovidiu.lupas
 */

//<FIXME author="Mihai Costin" description="This test needs re-writing!!"
public class OpResourceDataSetFactoryMockTest extends OpBaseMockTestCase {
//</FIXME>

   //the root resource pool
   private OpResourcePool rootResourcePool;
   //subresource pool of the root pool
   private OpResourcePool resourcePool;
   // resource of the resource pool
   private OpResource resource1;
   // resource of the root resource pool
   private OpResource resource2;
   private XLocalizer localizer = new XLocalizer();

   // ids of the entities
   private long ROOT_RESOURCE_POOL_ID = 1;
   private long RESOURCE_POOL_ID = 10;
   private long RESOURCE1_ID = 11;
   private long RESOURCE2_ID = 12;

   //queries
   private static final String SELECT_POOL_ID = "select pool.ID from OpResourcePool as pool where pool.SuperPool.ID is null";
   private static final String SELECT_POOL_ID_BY_SUPERPOOL_ID = "select pool.ID from OpResourcePool as pool where pool.SuperPool.ID = ?";
   private static final String SELECT_RESOURCE_BY_POOL_ID = "select resource.ID from OpResource as resource where resource.Pool.ID = ?";

   /**
    * @see onepoint.project.test.OpBaseMockTestCase#invocationMatch(org.jmock.core.Invocation)
    */
   public Object invocationMatch(Invocation invocation)
        throws IllegalArgumentException {
      String methodName = invocation.invokedMethod.getName();

      if (methodName.equals(ACCESSIBLE_OBJECTS_METHOD)) {
         return queryResults.iterator();
      }
      //no such method was found
      throw new IllegalArgumentException("Invalid method name:" + methodName + " for this stub");
   }

   /**
    * @see onepoint.project.test.OpBaseMockTestCase#setUp()
    */
   public void setUp() {
      super.setUp();
      //create the root resource pool
      rootResourcePool = new OpResourcePool();
      rootResourcePool.setID(ROOT_RESOURCE_POOL_ID);
      rootResourcePool.setName("RootResourcePool");
      rootResourcePool.setDescription("RootResourcePoolDescription");

      //create the resource pool
      resourcePool = new OpResourcePool();
      resourcePool.setID(RESOURCE_POOL_ID);
      resourcePool.setName("ResourcePool");
      resourcePool.setDescription("ResourcePoolDescription");

      //create the inner resource of the resource pool
      resource1 = new OpResource();
      resource1.setID(RESOURCE1_ID);
      resource1.setName("Resource1");
      resource1.setDescription("Resource1_Description");

      //create the resouce of the root pool
      resource2 = new OpResource();
      resource2.setID(RESOURCE2_ID);
      resource2.setName("Resource2");
      resource2.setDescription("Resource2_Description");

      //query results
      queryResults = new ArrayList();

   }

   /**
    * Tests the behaviour of <code>OpResourceDataSetFactory#retrieveResourceDataSet</code>.
    * The expected resource data set is presented below.
    * ->RootResourcePool
    * ---->ResourcePool
    * ------>Resource1
    * ---->Resource2
    */
   public void testRetriveResourceDataSet() {
      //get locale method
      mockSession.expects(once()).method(GET_LOCALE_METHOD).will(methodStub);

      //accessible object will return a queryResults iterator
      mockSession.expects(atLeastOnce()).method(ACCESSIBLE_OBJECTS_METHOD).will(methodStub);

      //list result sets
      mockBroker.expects(atLeastOnce()).method(LIST_METHOD).with(same(query)).will(methodStub);

      //the rootResourcePool is searched for
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_POOL_ID)).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            //add rootResourcePool
            queryResults.clear();
            queryResults.add(rootResourcePool);
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_POOL_ID);
         }
      });

      //the pool is searched for
      mockBroker.expects(atLeastOnce()).method(NEW_QUERY_METHOD).with(eq(SELECT_POOL_ID_BY_SUPERPOOL_ID)).
           will(methodStub).id(SELECT_POOL_ID_BY_SUPERPOOL_ID);

      //the pool resources are searched for
      mockBroker.expects(atLeastOnce()).method(NEW_QUERY_METHOD).with(eq(SELECT_RESOURCE_BY_POOL_ID)).
           will(methodStub).id(SELECT_RESOURCE_BY_POOL_ID);

      //search for the pools of root pool
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(ROOT_RESOURCE_POOL_ID)}).
           after(mockBroker, SELECT_POOL_ID_BY_SUPERPOOL_ID).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            //add resourcePool
            queryResults.clear();
            queryResults.add(resourcePool);
            return null;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SET_LONG_METHOD);
         }
      });

      //search for the inner pools of resourcePool
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(RESOURCE_POOL_ID)}).
           after(mockBroker, SELECT_POOL_ID_BY_SUPERPOOL_ID).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            //clear results
            queryResults.clear();
            return null;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SET_LONG_METHOD);
         }
      });

      //search for the resources of rootResourcePool
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(ROOT_RESOURCE_POOL_ID)}).
           after(mockBroker, SELECT_RESOURCE_BY_POOL_ID).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            //add resource2
            queryResults.clear();
            queryResults.add(resource2);
            return null;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SET_LONG_METHOD);
         }
      });

      //search for the resources of resourcePool
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(RESOURCE_POOL_ID)}).
           after(mockBroker, SELECT_RESOURCE_BY_POOL_ID).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            //add resource1
            queryResults.clear();
            queryResults.add(resource1);
            return null;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SET_LONG_METHOD);
         }
      });

      Map columnsSelector = new HashMap();
      columnsSelector.put(new Integer(0), new Integer(OpResourceDataSetFactory.DESCRIPTOR));
      columnsSelector.put(new Integer(1), new Integer(OpResourceDataSetFactory.NAME));
      columnsSelector.put(new Integer(2), new Integer(OpResourceDataSetFactory.DESCRIPTION));

      /*the expected data set */
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpResourceDataSetFactory.retrieveResourceDataSet(((OpProjectSession) mockSession.proxy()), dataSet, columnsSelector, columnsSelector, -1, 0, null);

      assertEquals("Wrong number of children in the data set", 4, dataSet.getChildCount());
      //assert equality for resource pools
      assertEqualResourcePools(rootResourcePool, (XComponent) dataSet.getChild(0), 0);
      assertEqualResourcePools(resourcePool, (XComponent) dataSet.getChild(1), 1);
      //assert equality for resources
      assertEqualResources(resource1, (XComponent) dataSet.getChild(2), 2);
      assertEqualResources(resource2, (XComponent) dataSet.getChild(3), 1);
   }

   /**
    * Asserts that the given <code>poolDataRow</code> has the <code>expectedPool<code> field values.
    *
    * @param expectedPool the expected resource pool instance of <code>OpResourcePool</code>
    * @param poolDataRow  the tested resource pool <code>XComponent.DATA_ROW</code>
    * @param outlineLevel <code>int</code> representing the expected outline level of the <code>poolDataRow</code>
    */

   public void assertEqualResourcePools(OpResourcePool expectedPool, XComponent poolDataRow, int outlineLevel) {
      //resource pool locator
      assertEquals("Resource Pools locator do not match", expectedPool.locator(), poolDataRow.getStringValue());
      //outline level property
      assertEquals("Tested Resource Pool outline level is wrong", poolDataRow.getOutlineLevel(), outlineLevel);

      XComponent dataCell = (XComponent) poolDataRow.getChild(0);
      //resource pool descriptor
      assertEquals("Tested Resource Pool descriptor do not match ", OpResourceDataSetFactory.POOL_DESCRIPTOR, dataCell.getStringValue());

      //resource pool name
      dataCell = (XComponent) poolDataRow.getChild(1);
      String expectedName = XValidator.choice(expectedPool.locator(), localizer.localize(expectedPool.getName()), OpResourceDataSetFactory.POOL_ICON_INDEX);
      assertEquals("Resource Pools name do not match ", expectedName, dataCell.getStringValue());

      //resource pool description
      dataCell = (XComponent) poolDataRow.getChild(2);
      String expectedDescription = localizer.localize(expectedPool.getDescription());
      assertEquals("Resource Pools description do not match ", expectedDescription, dataCell.getStringValue());


   }

   /**
    * Asserts that the given <code>resourceDataRow</code> has the <code>expectedResource<code> field values.
    *
    * @param expectedResource the expected resource instance of <code>OpResource</code>
    * @param resourceDataRow  the tested resource <code>XComponent.DATA_ROW</code>
    * @param outlineLevel     <code>int</code> representing the expected outline level of the <code>resourceDataRow</code>
    */

   public void assertEqualResources(OpResource expectedResource, XComponent resourceDataRow, int outlineLevel) {
      //outline level property
      assertEquals("Tested Resource outline Level is wrong", resourceDataRow.getOutlineLevel(), outlineLevel);
      //resource locator
      assertEquals("Resources locator do not match", expectedResource.locator(), resourceDataRow.getStringValue());

      XComponent dataCell = (XComponent) resourceDataRow.getChild(0);
      //resource descriptor
      assertEquals("Tested Resource descriptor do not match ", OpResourceDataSetFactory.RESOURCE_DESCRIPTOR, dataCell.getStringValue());

      //resource name
      dataCell = (XComponent) resourceDataRow.getChild(1);
      String expectedName = XValidator.choice(expectedResource.locator(), localizer.localize(expectedResource.getName()), OpResourceDataSetFactory.RESOURCE_ICON_INDEX);
      assertEquals("Resources name do not match ", expectedName, dataCell.getStringValue());

      //resource description
      dataCell = (XComponent) resourceDataRow.getChild(2);
      String expectedDescription = localizer.localize(expectedResource.getDescription());
      assertEquals("Resources description do not match ", expectedDescription, dataCell.getStringValue());


   }
}
