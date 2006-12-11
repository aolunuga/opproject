/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.test;

import onepoint.express.XComponent;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpContact;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;
import org.jmock.core.Constraint;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;

import java.util.*;

/**
 * Test case class for the <code>OpResourceService</code> service.
 *
 * @author ovidiu.lupas
 */
public class OpResourceServiceTest extends onepoint.project.test.OpServiceAbstractTest {

   /* the resource service object that is being tested*/
   private OpResourceService resourceService;

   /*the resource that is being tested*/
   private String resourceName;
   private String resourceDescription;
   private OpResource resource;

   /*the pool that is being tested*/
   private String poolName;
   private String poolDescription;
   private OpResourcePool pool;
   /* the super pool */
   private OpResourcePool superPool;

   /*user assigned to a resource */
   private OpUser user;

   /**
    * Resource ,Pool and Project String id
    */
   private final String RESOURCE_ID = "OpResource.16.xid";
   private final String RESOURCE_POOL_ID = "OpResourcePool.3.xid";
   private final String PROJECT_ID = "XProject.5.xid";

   private static final String NONEXISTENT_RESOURCE_ID = "nonExistentResourceId";
   private static final String NONEXISTENT_POOL_ID = "nonExistentPoolId";
   private static final String NONEXISTENT_PROJECT_ID = "nonExistentProjectId";

   private static final long USER_ID_LONG = 1987;
   private static final String USER_ID = "OpUser." + USER_ID_LONG + ".xid";
   private static final String USER_LOGIN_NAME = "userLoginName";

   /* HQLs */
   private static final String SELECT_POOL_BY_NAME_PARAMETER = "select pool from OpResourcePool as pool where pool.Name = :poolName";
   private static final String SELECT_POOL_BY_NAME = "select pool from OpResourcePool as pool where pool.Name = ?";
   private static final String SELECT_POOL_ID_BY_NAME = "select pool.ID from OpResourcePool as pool where pool.Name = :poolName";
   private static final String SELECT_RESOURCE_BY_NAME = "select resource from OpResource as resource where resource.Name = :resourceName";
   private static final String SELECT_RESOURCE_ID_BY_NAME = "select resource.ID from OpResource as resource " +
        "where resource.Name = :resourceName";
   private static final String SELECT_RESOURCE_POOL_ID = "select resource.Pool.ID from OpResource as resource " +
        "where resource.ID in (:resourceIds)";
   private static final String SELECT_POOL_SUPER_POOL_ID = "select pool.SuperPool.ID from OpResourcePool as pool where pool.ID in (:poolIds)";
   private static final String SELECT_ACCESSIBLE_RESOURCES = "select resource from OpResource as resource " +
        "where resource.ID in (:resourceIds) and resource.Pool.ID in (:accessiblePoolIds) and size(resource.ActivityAssignments) = 0 and size(resource.AssignmentVersions) = 0";
   private static final String SELECT_ACCESSIBLE_POOLS = "select pool from OpResourcePool as pool " +
        "where pool.ID in (:poolIds) and pool.SuperPool.ID in (:accessibleSuperPoolIds)";
   private static final String SELECT_EXISTENT_PROJECT_ASSIGNMENT = "select assignment.Resource.ID from OpProjectNodeAssignment as assignment where assignment.ProjectNode.ID = ?";
   private static final String SELECT_PROJECT_ASSIGNMENTS_FOR_RESOURCE = "select assignment.ProjectNode.ID from OpProjectNodeAssignment as assignment where assignment.Resource.ID = ?";
   private static final String SELECT_WORKING_VERSION_ASSIGNMENT = "select assignment from OpResource resource inner join resource.AssignmentVersions assignment where assignment.ActivityVersion.PlanVersion.VersionNumber = ? and resource.ID = ?";
   private final String SUPER_POOL_ID_PARAM = "SuperPoolID";

   /**
    * @see onepoint.project.test.OpServiceAbstractTest#setUp()
    */
   public void setUp() {

      super.setUp();
      //create the resource service object
      resourceService = new OpResourceService();

      //create the super pool object
      superPool = new OpResourcePool();
      superPool.setID(15);
      superPool.setName("SuperPoolName");
      superPool.setDescription("SuperPoolDescription");
      superPool.setHourlyRate(5.0);
      superPool.setResources(new HashSet());

      //create the pool object
      pool = new OpResourcePool();
      pool.setID(10);
      poolName = "PoolName";
      poolDescription = "PoolDescription";
      pool.setName(poolName);
      pool.setDescription(poolDescription);
      pool.setHourlyRate(5.0);
      pool.setResources(new HashSet());

      //create the resource
      resource = new OpResource();
      resourceName = "ResourceName";
      resourceDescription = "ResourceDescription";
      resource.setPool(pool);
      resource.setName(resourceName);
      resource.setDescription(resourceDescription);
      resource.setAvailable((byte) 100);
      resource.setHourlyRate(5.0);
      resource.setInheritPoolRate(true);
      resource.setProjectNodeAssignments(new HashSet());

      //prepare a user from the "db" that will be assigned to a resource
      String userName = "userFirstName";
      String userLastName = "userLastName";
      String userPassword = "userPassword";

      user = new OpUser();
      OpContact userContact = new OpContact();
      userContact.setFirstName(userName);
      userContact.setLastName(userLastName);
      user.setID(USER_ID_LONG);
      user.setPreferences(new HashSet());
      user.setPassword(userPassword);

      user.setName(USER_LOGIN_NAME);
      user.setAssignments(new HashSet());
      user.setResources(new HashSet());
      user.setContact(userContact);

      //am empty list of query results
      queryResults = new ArrayList();
   }

   /**
    * @see onepoint.project.test.OpServiceAbstractTest#invocationMatch(org.jmock.core.Invocation)
    */
   public Object invocationMatch(Invocation invocation)
        throws IllegalArgumentException {
      String methodName = invocation.invokedMethod.getName();

      if (methodName.equals(GET_OBJECT_METHOD)) {
         String entityId = (String) invocation.parameterValues.get(0);
         //get the pool
         if (entityId.equals(RESOURCE_POOL_ID)) {
            Set resourcesSet = new HashSet();
            resourcesSet.add(resource);
            pool.setResources(resourcesSet);
            return pool;
         }
         //get the resource
         if (entityId.equals(RESOURCE_ID)) {
            return resource;
         }
         //get the project
         if (entityId.equals(PROJECT_ID)) {
            //create a default project
            OpProjectNode project = new OpProjectNode();
            project.setType(OpProjectNode.PROJECT);
            project.setName("ProjectName");
            project.setStart(XCalendar.today());
            project.setFinish(XCalendar.today());
            project.setBudget(1000.0);
            return project;
         }
         //get the user
         if (entityId.equals(USER_ID)) {
            return user;
         }
         //not found in the DB
         if (entityId.equals(NONEXISTENT_POOL_ID) || (entityId.equals(NONEXISTENT_RESOURCE_ID)) ||
              entityId.equals(NONEXISTENT_PROJECT_ID)) {
            return null;
         }
      }
      //check acces level
      else if (methodName.equals(CHECK_ACCESS_LEVEL_METHOD)) {
         return Boolean.valueOf(true);
      }
      //list query results
      else if (methodName.equals(LIST_METHOD)) {
         return queryResults;
      }
      //accesible pool and super pool ids
      else if (methodName.equals(ACCESSIBLE_IDS_METHOD)) {
         Set accessiblePoolIds = new HashSet();
         accessiblePoolIds.add(RESOURCE_POOL_ID);
         return accessiblePoolIds;
      }
      else if (methodName.equals(ACCESSIBLE_OBJECTS_METHOD)) {
         return queryResults.iterator();
      }
      //no such method was found
      throw new IllegalArgumentException("Invalid method name for this stub");
   }

   /**
    * Tests that a new resource is will be corectly inserted provided that the resource data is correct
    *
    * @throws Exception if anything fails.
    */
   public void testInsertNewResource()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      Map resourceData = createResourceData("ResourceName", "ResourceDescription", 100, 5.0, true, RESOURCE_POOL_ID, null,
           permissionSet);
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);

      Constraint testResource = createResourceConstraint(resourceData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //the resource is searched for (a resource can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_RESOURCE_ID_BY_NAME)).will(methodStub);
      //set resource name
      mockQuery.expects(once()).method(SET_STRING_METHOD);
      //will not find a resource
      queryResults.clear();

      //iterate over query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(atLeastOnce()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the resource must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD).with(testResource);

      //transaction must be commited
      mockTransaction.expects(atLeastOnce()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(resourceService.insertResource((XSession) mockSession.proxy(), request));
   }

   /**
    * Tests the behavior of insertResource for an invalid resource data.
    *
    * @throws Exception if anything fails.
    */
   public void testInsertResourceWrongData()
        throws Exception {
      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      checkInsertResourceWrongData("", "ResourceDescription", 100, 5.0, true, null, permissionSet);
      checkInsertResourceWrongData("ResourceName", "ResourceDescription", -19, 5.0, true, null, permissionSet);
      checkInsertResourceWrongData("ResourceName", "ResourceDescription", 185, 5.0, true, null, permissionSet);
      checkInsertResourceWrongData("ResourceName", "ResourceDescription", 100, -15.0, false, null, permissionSet);
   }

   /**
    * Tests the behavior of insertResource for an invalid resource data.
    *
    * @throws Exception if anything fails.
    */
   private void checkInsertResourceWrongData(String resourceName, String resourceDescription, int resourceAvailability,
        double resourceHourlyRate, boolean resourceInheritPoolRate,
        ArrayList resourceProjects, XComponent permissionSet) {

      //create the request
      XMessage request = new XMessage();
      Map resourceData = createResourceData(resourceName, resourceDescription, resourceAvailability,
           resourceHourlyRate, resourceInheritPoolRate, RESOURCE_POOL_ID, resourceProjects, permissionSet);
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the resource must not be persisted
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must not be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = resourceService.insertResource((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }


   /**
    * Tests the behavior of insertResource for an already existent resource data.
    *
    * @throws Exception if anything fails.
    */
   public void testInsertAlreadyExistentResource()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      //the resource permission set
      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      Map resourceData = createResourceData(resource.getName(), "ResourceDescription", 100, 5.0, true, RESOURCE_POOL_ID, null, permissionSet);
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get pool object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //the resource is searched for (a resource can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_RESOURCE_ID_BY_NAME)).will(methodStub);
      //set resource name
      mockQuery.expects(once()).method(SET_STRING_METHOD);
      //will find a resource
      queryResults.add(RESOURCE_ID);

      //iterate over query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      ///a new transaction will never be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the resource must not be persisted
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);


      XMessage result = resourceService.insertResource((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of updateResource for an existent resource and accurate data.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateResource()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      //set up the resource id
      request.setArgument(OpResourceService.RESOURCE_ID, RESOURCE_ID);
      //the resource permission set
      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      Map resourceData = createResourceData("NewResourceName", "NewResourceDescription", 100, 5.0, true, RESOURCE_POOL_ID, null, permissionSet);
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);

      Constraint testResource = createResourceConstraint(resourceData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get resource object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_ID)).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //the resource is searched for (a resource can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_RESOURCE_BY_NAME)).will(methodStub);
      //set resource name
      mockQuery.expects(once()).method(SET_STRING_METHOD);
      //will not find a resource
      queryResults.clear();

      //iterate over query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //resource project assignments
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_ASSIGNMENTS_FOR_RESOURCE)).will(methodStub).id(SELECT_PROJECT_ASSIGNMENTS_FOR_RESOURCE);
      //set resource id
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(resource.getID())});
      //iterate over query results
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).after(SELECT_PROJECT_ASSIGNMENTS_FOR_RESOURCE).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            return new ArrayList();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of iterating the resource assignments ");
         }
      });

      //a new transaction will be created
      mockBroker.expects(atLeastOnce()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the resource must be updated
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(testResource);

      //transaction must be commited
      mockTransaction.expects(atLeastOnce()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(resourceService.updateResource((XSession) mockSession.proxy(), request));

   }

   /**
    * Tests the behavior of updatePool for a non existent pool name and accurata data.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateNonExistentResource()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      //set up the request
      request.setArgument(OpResourceService.RESOURCE_ID, NONEXISTENT_RESOURCE_ID);
      //the resource permission set
      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      Map resourceData = createResourceData("NewResourceName", "NewResourceDescription", 100, 5.0, true, RESOURCE_POOL_ID, null, permissionSet);
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(NONEXISTENT_RESOURCE_ID)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the resource must not be updated
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = resourceService.updateResource((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());

   }


   /**
    * Tests the behavior of updateResource for an invalid resource data.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateResourceWrongData()
        throws Exception {
      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
//     <FIXME author="Ovidiu Lupas" description="check mandatory input fields is not made consecutively">
//     checkUpdateResourceWrongData("", "ResourceDescription", 100, 5.0, true, null, permissionSet);
//     </FIXME>
      checkUpdateResourceWrongData("ResourceName", "ResourceDescription", -19, 5.0, true, null, permissionSet);
      checkUpdateResourceWrongData("ResourceName", "ResourceDescription", 100, -5.0, false, null, permissionSet);
   }

   /**
    * Tests the behavior of updateResource for an invalid resource data.
    *
    * @throws Exception if anything fails.
    */
   private void checkUpdateResourceWrongData(String resourceName, String resourceDescription, int resourceAvailability,
        double resourceHourlyRate, boolean resourceInheritPoolRate,
        ArrayList resourceProjects, XComponent permissionSet) {
      //create the request
      XMessage request = new XMessage();
      //set up the request
      request.setArgument(OpResourceService.RESOURCE_ID, RESOURCE_ID);
      Map resourceData = createResourceData(resourceName, resourceDescription, resourceAvailability,
           resourceHourlyRate, resourceInheritPoolRate, RESOURCE_POOL_ID, resourceProjects, permissionSet);
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get resource object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_ID)).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //the resource is searched for (a resource can't be inserted if it's already there)
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_RESOURCE_BY_NAME)).will(methodStub);
      //set resource name
      mockQuery.expects(once()).method(SET_STRING_METHOD);
      //will not find a resource
      queryResults.clear();

      //iterate over query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the resource must not be updated
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = resourceService.updateResource((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   public void testMoveResource() {
      XMessage request = new XMessage();
      ArrayList resourcesIds = new ArrayList();
      resourcesIds.add(RESOURCE_ID);
      //set up the request
      request.setArgument(OpResourceService.RESOURCE_IDS, resourcesIds);
      request.setArgument(OpResourceService.POOL_ID, RESOURCE_POOL_ID);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //get the selected destination pool
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      //check manager access rights
      mockSession.expects(atLeastOnce()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);
      
      //get the selected resource
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_ID)).will(methodStub);

      //update object
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(new Constraint() {
         public boolean eval(Object object) {
            if (!(object instanceof OpResource)) {
               return false;
            }
            OpResource paramPool = (OpResource) object;
            if (paramPool != resource) {
               return false;
            }
            if (paramPool.getPool() != pool) {
               return false;
            }
            return true;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return null;
         }
      });

      //commit & close broker
      mockTransaction.expects(once()).method(COMMIT_METHOD);
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = resourceService.moveResourceNode((XSession) mockSession.proxy(), request);
      assertNull("No Error message should have been returned", result.getError());
   }

   public void testMoveResourceNoRights() {
      XMessage request = new XMessage();
      ArrayList resourcesIds = new ArrayList();
      resourcesIds.add(RESOURCE_ID);

      //set up the request
      request.setArgument(OpResourceService.RESOURCE_IDS, resourcesIds);
      request.setArgument(OpResourceService.POOL_ID, RESOURCE_POOL_ID);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //get the selected destination pool
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      //check manager access rights
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(returnValue(false));

      //commit & close broker
      mockTransaction.expects(once()).method(COMMIT_METHOD);
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = resourceService.moveResourceNode((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
   }

   public void testMoveResourceChangeHR() {
      XMessage request = new XMessage();
      ArrayList resourcesIds = new ArrayList();
      resourcesIds.add(RESOURCE_ID);
      //set up the request
      request.setArgument(OpResourceService.RESOURCE_IDS, resourcesIds);
      request.setArgument(OpResourceService.POOL_ID, RESOURCE_POOL_ID);
      final int hRate = 10;
      pool.setHourlyRate(hRate);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);
      //a new transaction will be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //get the selected destination pool
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      //check manager access rights
      mockSession.expects(atLeastOnce()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //get the selected resource
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_ID)).will(methodStub);

      //update hr and costs
      //get assignments
      StringBuffer queryString = new StringBuffer();
      queryString.append("select assignment from OpResource resource inner join resource.AssignmentVersions assignment ");
      queryString.append("where assignment.ActivityVersion.PlanVersion.VersionNumber = ? and resource.ID = ?");
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(queryString.toString())).will(methodStub);
      mockQuery.expects(once()).method(SET_INTEGER_METHOD).with(new Constraint[]{eq(0), eq(OpProjectAdministrationService.WORKING_VERSION_NUMBER)});
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(1), eq(resource.getID())});
      mockBroker.expects(once()).method(LIST_METHOD).will(methodStub);
      //no assignments on the resource...

      //update object
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(new Constraint() {
         public boolean eval(Object object) {
            if (!(object instanceof OpResource)) {
               return false;
            }
            OpResource paramPool = (OpResource) object;
            if (paramPool != resource) {
               return false;
            }
            if (paramPool.getPool() != pool) {
               return false;
            }
            if (paramPool.getHourlyRate() != hRate) {
               return false;
            }
            return true;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return null;
         }
      });


      //commit & close broker
      mockTransaction.expects(once()).method(COMMIT_METHOD);
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = resourceService.moveResourceNode((XSession) mockSession.proxy(), request);
      assertNull("No Error message should have been returned", result.getError());
   }


   /**
    * Tests the behavior of deleteResources for an existing resource.
    *
    * @throws Exception if anything fails.
    */
   public void testDeleteResource()
        throws Exception {
      XMessage request = new XMessage();
      ArrayList resourcesIds = new ArrayList();
      resourcesIds.add(RESOURCE_ID);
      //set up the request
      request.setArgument(OpResourceService.RESOURCE_IDS, resourcesIds);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the resource's pool id is searched
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_RESOURCE_POOL_ID)).will(methodStub);
      //select resource's pool id
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(methodStub);

      //accessible pool id must be called
      mockSession.expects(once()).method(ACCESSIBLE_IDS_METHOD).will(methodStub);

      //resources that belong to accessible pools
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ACCESSIBLE_RESOURCES)).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            queryResults.clear();
            //will find a resource
            queryResults.add(resource);
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_ACCESSIBLE_RESOURCES);
         }
      });

      //set resources ids
      mockQuery.expects(atLeastOnce()).method(SET_COLLECTION_METHOD);

      //select resource's pool id
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(methodStub);

      //resource project assignments
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_PROJECT_ASSIGNMENTS_FOR_RESOURCE)).will(methodStub).id(SELECT_PROJECT_ASSIGNMENTS_FOR_RESOURCE);
      //set resource id
      mockQuery.expects(once()).method(SET_LONG_METHOD).with(new Constraint[]{eq(0), eq(resource.getID())});
      //iterate over query results
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).after(SELECT_PROJECT_ASSIGNMENTS_FOR_RESOURCE).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            return new ArrayList();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of iterating the resource assignments ");
         }
      });

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //delete object method
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(same(resource));

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(resourceService.deleteResources((XSession) mockSession.proxy(), request));
   }


   /**
    * Tests that a new pool is will be corectly inserted provided that the pool data is correct
    *
    * @throws Exception if anything fails.
    */
   public void testInsertNewPool()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      XComponent permisionSet = new XComponent(XComponent.DATA_SET);
      Map poolData = creatPoolData("PoolName", "Pool Description", 5.0, permisionSet);
      request.setArgument(OpResourceService.POOL_DATA, poolData);

      Constraint testPool = createPoolConstraint(poolData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //the root pool must be found
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      //an already existent pool must be not found
      queryResults.clear();
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_POOL_ID_BY_NAME)).will(methodStub);

      //set pool name
      mockQuery.expects(atLeastOnce()).method(SET_STRING_METHOD);

      //iterate over query results
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //a new transaction will be created
      mockBroker.expects(atLeastOnce()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the pool must be persisted
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD).with(testPool);

      //transaction must be commited
      mockTransaction.expects(atLeastOnce()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(resourceService.insertPool((XSession) mockSession.proxy(), request));

   }

   /**
    * Tests the behavior of insertPool for an invalid pool data.
    *
    * @throws Exception if anything fails.
    */
   public void testInsertPoolWrongData()
        throws Exception {
      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      checkInsertPoolWrongData("", "PoolDescription", 5.0, permissionSet);
      checkInsertPoolWrongData("PoolName", "PoolDescription", -5.0, permissionSet);
   }

   /**
    * Tests the behavior of insertPool for an invalid pool data.
    *
    * @throws Exception if anything fails.
    */
   private void checkInsertPoolWrongData(String poolName, String poolDescription, double poolHourlyRate,
        XComponent permissionSet) {
      //create the request
      XMessage request = new XMessage();
      Map poolData = creatPoolData(poolName, poolDescription, poolHourlyRate, permissionSet);
      request.setArgument(OpResourceService.POOL_DATA, poolData);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      // a new broker must  not be created
      mockSession.expects(never()).method(NEW_BROKER_METHOD);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the pool must not be persisted
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must not be closed
      mockBroker.expects(never()).method(CLOSE_METHOD);

      XMessage result = resourceService.insertPool((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of insertPool for an already existent pool data.
    *
    * @throws Exception if anything fails.
    */
   public void testInsertAlreadyExistentPool()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      Map poolData = creatPoolData(pool.getName(), "Pool Description", 5.0, permissionSet);
      request.setArgument(OpResourceService.POOL_DATA, poolData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //the root pool must be found
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      //an already existent pool must be found
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_POOL_ID_BY_NAME)).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            queryResults.clear();
            queryResults.add(new Long(pool.getID()));
            return query;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of new query for ").append(SELECT_POOL_ID_BY_NAME);
         }
      });

      //set root pool name /pool name
      mockQuery.expects(atLeastOnce()).method(SET_STRING_METHOD);

      //iterate over results
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      ///a new transaction will never be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the pool must not be persisted
      mockBroker.expects(never()).method(MAKE_PERSISTENT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);


      XMessage result = resourceService.insertPool((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of updatePool for an existent pool and accurate data.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdatePool()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      //set up the pool id
      request.setArgument(OpResourceService.POOL_ID, RESOURCE_POOL_ID);

      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      //set up the updated pool data
      Map poolData = creatPoolData(pool.getName(), "NewPoolDescription", 15.0, permissionSet);
      request.setArgument(OpResourceService.POOL_DATA, poolData);

      Constraint testPool = createPoolConstraint(poolData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //get pool object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      //an already existent pool with the same name must not be found
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_POOL_BY_NAME_PARAMETER)).will(methodStub);
      //set pool name
      mockQuery.expects(atLeastOnce()).method(SET_STRING_METHOD);
      //no results found
      queryResults.clear();
      //iterate over results
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //a root resource pool should be found
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_POOL_BY_NAME)).will(methodStub);
      mockQuery.expects(once()).method(SET_STRING_METHOD).with(new Constraint[]{eq(0), eq(OpResourcePool.ROOT_RESOURCE_POOL_NAME)});

      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            queryResults.clear();
            queryResults.add(superPool);
            return queryResults;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour list resurs of query ").append(SELECT_POOL_BY_NAME);
         }
      });

      //new query on broker
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_WORKING_VERSION_ASSIGNMENT)).will(methodStub).id(SELECT_WORKING_VERSION_ASSIGNMENT);
      //set resource id
      mockQuery.expects(once()).method(SET_LONG_METHOD);
      //set project plan version
      mockQuery.expects(once()).method(SET_INTEGER_METHOD);
      //iterate over query results
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).after(SELECT_WORKING_VERSION_ASSIGNMENT).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            queryResults.clear();
            return queryResults;
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of listing the resource assignments");
         }
      });

      //a new transaction will be created
      mockBroker.expects(atLeastOnce()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //the pool must be updated
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(testPool);

      //the resource must be updated
      mockBroker.expects(once()).method(UPDATE_OBJECT_METHOD).with(same(resource));

      //transaction must be commited
      mockTransaction.expects(atLeastOnce()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = resourceService.updatePool((XSession) mockSession.proxy(), request);
      assertNull("No error message should have been returned", result.getError());

   }

   /**
    * Tests the behavior of updatePool for a non existent pool name and accurata data.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdateNonExistentPool()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      //set up the request
      request.setArgument(OpResourceService.POOL_ID, NONEXISTENT_POOL_ID);

      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      //set up the updated pool data
      Map poolData = creatPoolData(pool.getName(), "Pool Description", 15.0, permissionSet);
      request.setArgument(OpResourceService.POOL_DATA, poolData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(NONEXISTENT_POOL_ID)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the pool must not be updated
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = resourceService.updatePool((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());

   }

   /**
    * Tests the behavior of updatePool for an invalid pool data.
    *
    * @throws Exception if anything fails.
    */
   public void testUpdatePoolWrongData()
        throws Exception {
      XComponent permissionSet = new XComponent(XComponent.DATA_SET);
      checkUpdatePoolWrongData("", "PoolDescription", 5.0, permissionSet);
      //<FIXME author="Ovidiu Lupas" description="check mandatory input fields is not made consecutively">
      //checkUpdatePoolWrongData("PoolName", "PoolDescription", -5.0, permissionSet);
      //</FIXME>
   }

   /**
    * Tests the behavior of updatePool for an invalid pool data.
    *
    * @throws Exception if anything fails.
    */
   private void checkUpdatePoolWrongData(String poolName, String poolDescription, double poolHourlyRate,
        XComponent permissionSet) {
      //create the request
      XMessage request = new XMessage();
      //set up the request
      request.setArgument(OpResourceService.POOL_ID, RESOURCE_POOL_ID);

      Map poolData = creatPoolData(poolName, poolDescription, poolHourlyRate, permissionSet);
      request.setArgument(OpResourceService.POOL_DATA, poolData);

      // a new broker must be created
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //get object
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //a new transaction will not be created
      mockBroker.expects(never()).method(NEW_TRANSACTION_METHOD);

      //the pool must not be updated
      mockBroker.expects(never()).method(UPDATE_OBJECT_METHOD);

      //transaction must not be commited
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      //broker must be closed
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = resourceService.updatePool((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of deletePools for an existing pool.
    *
    * @throws Exception if anything fails.
    */
   public void testDeletePool()
        throws Exception {
      XMessage request = new XMessage();
      ArrayList poolIds = new ArrayList();
      poolIds.add(RESOURCE_POOL_ID);
      //set up the request
      request.setArgument(OpResourceService.POOL_IDS, poolIds);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //accessible pool id must be called
      mockSession.expects(once()).method(ACCESSIBLE_IDS_METHOD).will(methodStub);

      //the pool's super pool id is searched
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_POOL_SUPER_POOL_ID)).will(methodStub);

      //pools that belong to accessible super pools
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_ACCESSIBLE_POOLS)).will(methodStub);

      //set resources ids
      mockQuery.expects(atLeastOnce()).method(SET_COLLECTION_METHOD);

      //select pool's superpool id
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(methodStub);

      //will find a resource
      queryResults.add(pool);

      //iterate over query results
      mockBroker.expects(once()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      //delete object method -delete pool
      mockBroker.expects(once()).method(DELETE_OBJECT_METHOD).with(same(pool));

      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(resourceService.deletePools((XSession) mockSession.proxy(), request));

   }


   /**
    * Tests the behavior of assignToProject for an existing resource and project.
    *
    * @throws Exception if anything fails.
    */
   public void testAssignResourceToProject()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      ArrayList resourcesIds = new ArrayList();
      resourcesIds.add(RESOURCE_ID);
      //set up the request
      request.setArgument(OpResourceService.RESOURCE_IDS, resourcesIds);
      request.setArgument(OpResourceService.PROJECT_IDS, Arrays.asList(new String[]{PROJECT_ID}));

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);

      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_ID)).will(methodStub);
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(PROJECT_ID)).will(methodStub);
      //becouse the method's complexity the result represents the all resources
      mockSession.expects(once()).method(ACCESSIBLE_OBJECTS_METHOD).will(methodStub);
      //query result for accesible objects
      queryResults.add(resource);
      // check already existence of the project assignment
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_EXISTENT_PROJECT_ASSIGNMENT)).will(methodStub);
      //set project node id
      mockQuery.expects(once()).method(SET_LONG_METHOD);
      //iterate over results
      mockBroker.expects(once()).method(LIST_METHOD).with(same(query)).will(new Stub() {
         public Object invoke(Invocation invocation)
              throws Throwable {
            return new ArrayList(0);
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Mocks the behaviour of iterator for results of the query ")
                 .append(SELECT_EXISTENT_PROJECT_ASSIGNMENT);
         }
      });

      //the project node assignment must be persisted
      mockBroker.expects(atLeastOnce()).method(MAKE_PERSISTENT_METHOD);
      //commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      assertNoError(resourceService.assignToProject((XSession) mockSession.proxy(), request));

   }

   /**
    * Tests the behavior of importUser as resource.
    *
    * @throws Exception if anything fails.
    */
   public void testImportUserAsResource()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      Map resourceData = createResourceData(USER_ID, "%f[1]%l[1]", 100, 10, false, RESOURCE_POOL_ID);
      //set up the request
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //transaction must be created
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);
      /*get the user */
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(USER_ID)).will(methodStub);

      /*a new query is made to find if the resource name already exists in the db*/
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_RESOURCE_ID_BY_NAME)).will(methodStub);
      //set resource name according to name format
      mockQuery.expects(once()).method(SET_STRING_METHOD);
      //no results found
      queryResults.clear();
      //iterate over results
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //get pool and check for user constaint (2 different resources which have assigned the same user)
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);

      //the resource must be persisted must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      //do commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);

      assertNoError(resourceService.importUser((XSession) mockSession.proxy(), request));
   }


   /**
    * Tests the behavior of importUser as resource when the user does not exists anymore .
    *
    * @throws Exception if anything fails.
    */
   public void testImportNonExistentUserAsResource()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      Map resourceData = createResourceData(null, "%f[1]%l[1]", 100, 10, false, RESOURCE_POOL_ID);
      //set up the request
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);
      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);

      //never do commit
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      XMessage result = resourceService.importUser((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());

   }

   /**
    * Tests the behavior of importUser when the resource name according to name format already exists in the db .
    *
    * @throws Exception if anything fails.
    */
   public void testImportUserAsExistentResourceName()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      Map resourceData = createResourceData(USER_ID, "%f[1]%l[1]", 100, 10, false, RESOURCE_POOL_ID);
      //set up the request
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      /*get the user */
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(USER_ID)).will(methodStub);

      /*a new query is made to find if the resource name already exists in the db*/
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_RESOURCE_ID_BY_NAME)).will(methodStub);
      //set resource name according to name format
      mockQuery.expects(once()).method(SET_STRING_METHOD);
      //one result found return error
      queryResults.add(resource);
      //iterate over results
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);
      //error must be generated
      mockSession.expects(once()).method(NEW_ERROR_METHOD).will(methodStub);
      //never do commit
      mockTransaction.expects(never()).method(COMMIT_METHOD);

      XMessage result = resourceService.importUser((XSession) mockSession.proxy(), request);
      assertNotNull("Error message should have been returned", result.getError());
      assertEquals("Error should be the one that was set on new error call", error, result.getError());
   }

   /**
    * Tests the behavior of importUser when the user is already assigned to a resource in the pool
    *
    * @throws Exception if anything fails.
    */
   public void testImportExistentUserInResourcePool()
        throws Exception {
      //create the request
      XMessage request = new XMessage();
      Map resourceData = createResourceData(USER_ID, "%f[1]%l[1]", 100, 10, false, RESOURCE_POOL_ID);
      //set up the request
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);

      //broker must be called
      mockSession.expects(once()).method(NEW_BROKER_METHOD).will(methodStub);

      /*get the user */
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(USER_ID)).will(methodStub);

      /*a new query is made to find if the resource name already exists in the db*/
      mockBroker.expects(once()).method(NEW_QUERY_METHOD).with(eq(SELECT_RESOURCE_ID_BY_NAME)).will(methodStub);

      //set resource name according to name format
      mockQuery.expects(once()).method(SET_STRING_METHOD);

      //no results found in the db (resources with the same name
      queryResults.clear();

      //iterate over results
      mockBroker.expects(atLeastOnce()).method(ITERATE_METHOD).with(same(query)).will(methodStub);

      // the check acces level must be performed
      mockSession.expects(once()).method(CHECK_ACCESS_LEVEL_METHOD).will(methodStub);

      //get pool and check for user constaint (2 different resources which have assigned the same user)
      mockBroker.expects(once()).method(GET_OBJECT_METHOD).with(eq(RESOURCE_POOL_ID)).will(methodStub);
      /*resource already exists for a pool and has assigned the same user */
      resource.setUser(user);
      //being transaction
      mockBroker.expects(once()).method(NEW_TRANSACTION_METHOD).will(methodStub);
      //the resource must be persisted must be persisted
      mockBroker.expects(once()).method(MAKE_PERSISTENT_METHOD);
      //do commit
      mockTransaction.expects(once()).method(COMMIT_METHOD);
      //close broker
      mockBroker.expects(once()).method(CLOSE_METHOD);

      XMessage result = resourceService.importUser((XSession) mockSession.proxy(), request);
      assertNoError(result);
   }

   /**
    * Creates a resource data used for import user as resource unit tests.
    *
    * @param userId                  <code>String</code> representing user locator
    * @param nameFormat              <code>String</code> represeting name format %f[1]%l[2]
    * @param resourceAvailability    a <code>int</code> representing the availability of the resource
    * @param resourceHourlyRate      a <code>int</code> representing the hourly rate of the resource
    * @param resourceInheritPoolRate a <code>boolean</code> representing the inherit pool rate property
    * @param resourcePoolId          a <code>String</code> representing the resource's pool id
    * @return a new <code>Map</code> with the resource data
    */
   private Map createResourceData(String userId, String nameFormat, int resourceAvailability,
        int resourceHourlyRate, boolean resourceInheritPoolRate, String resourcePoolId) {
      Map resourceValues = new HashMap();
      resourceValues.put("UserID", userId);
      resourceValues.put("PoolID", resourcePoolId);
      resourceValues.put("NameFormat", nameFormat);
      resourceValues.put(OpResource.AVAILABLE, new Double(resourceAvailability));
      resourceValues.put(OpResource.HOURLY_RATE, new Double(resourceHourlyRate));
      resourceValues.put(OpResource.INHERIT_POOL_RATE, Boolean.valueOf(resourceInheritPoolRate));

      return resourceValues;
   }

   /**
    * Creates a pool data given the properties of the new pool
    *
    * @param poolName        a <code>String </code> representing the name for the pool
    * @param poolDescription a <code>String</code> representing the description of the pool
    * @param poolHourlyRate  a <code>double</code> representing the hourly rate of the pool
    * @param permissionSet   a <code>XComponent.DATA_SET</code> representing the permission set of the pool
    * @return a new <code>XStruct</code> with the pool data
    */
   private Map creatPoolData(String poolName, String poolDescription, double poolHourlyRate,
        XComponent permissionSet) {
      Map poolValues = new HashMap();
      poolValues.put(OpResourcePool.NAME, poolName);
      poolValues.put(SUPER_POOL_ID_PARAM, RESOURCE_POOL_ID);
      poolValues.put(OpResourcePool.DESCRIPTION, poolDescription);
      poolValues.put(OpResourcePool.HOURLY_RATE, new Double(poolHourlyRate));
      poolValues.put(OpPermissionSetFactory.PERMISSION_SET, permissionSet);
      return poolValues;
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if a pool is the expected one.
    *
    * @return a new pool constraint
    */
   private Constraint createPoolConstraint(final Map poolData) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpResourcePool)) {
               return false;
            }
            OpResourcePool pool = (OpResourcePool) object;
            if (!poolData.get(OpResourcePool.NAME).equals(pool.getName())) {
               return false;
            }
            if (!poolData.get(OpResourcePool.DESCRIPTION).equals(pool.getDescription())) {
               return false;
            }
            return ((Double) poolData.get(OpResourcePool.HOURLY_RATE)).doubleValue() == pool.getHourlyRate();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if a pool is the expected one");
         }
      };
   }

   /**
    * Creates a resource data given the properties of the new resource
    *
    * @param resourceName            a <code>String </code> representing the name for the resource
    * @param resourceDescription     a <code>String</code> representing the description of the resource
    * @param resourceAvailability    a <code>int</code> representing the availability of the resource
    * @param resourceHourlyRate      a <code>double</code> representing the hourly rate of the resource
    * @param resourceInheritPoolRate a <code>boolean</code> representing the inherit pool rate property
    * @param resourcePoolId          a <code>String</code> representing the resource's pool id
    * @param projectAssignments      a <code>XArray<code> representing the resource project assignments
    * @param projectPermissionSet    a <code>XComponent.DATA_SET</code> representing project's permission set
    * @return a new <code>Map</code> with the resource data
    */
   private Map createResourceData(String resourceName, String resourceDescription, int resourceAvailability,
        double resourceHourlyRate, boolean resourceInheritPoolRate, String resourcePoolId,
        ArrayList projectAssignments, XComponent projectPermissionSet) {
      Map resourceValues = new HashMap();
      resourceValues.put(OpResource.NAME, resourceName);
      resourceValues.put(OpResource.DESCRIPTION, resourceDescription);
      resourceValues.put(OpResource.AVAILABLE, new Double(resourceAvailability));
      resourceValues.put(OpResource.HOURLY_RATE, new Double(resourceHourlyRate));
      resourceValues.put(OpResource.INHERIT_POOL_RATE, Boolean.valueOf(resourceInheritPoolRate));
      resourceValues.put("PoolID", resourcePoolId);
      resourceValues.put(OpResourceService.PROJECTS, projectAssignments);
      resourceValues.put(OpPermissionSetFactory.PERMISSION_SET, projectPermissionSet);
      return resourceValues;
   }

   /**
    * Creates a new constraint (anonymous inner class) that can be used to check if a resource is the expected one.
    *
    * @return a new resource constraint
    */
   private Constraint createResourceConstraint(final Map resourceData) {

      return new Constraint() {

         public boolean eval(Object object) {
            if (!(object instanceof OpResource)) {
               return false;
            }
            OpResource resource = (OpResource) object;
            if (!resourceData.get(OpResource.NAME).equals(resource.getName())) {
               return false;
            }
            if (!resourceData.get(OpResource.DESCRIPTION).equals(resource.getDescription())) {
               return false;
            }
            if (((Double) resourceData.get(OpResource.AVAILABLE)).doubleValue() != resource.getAvailable()) {
               return false;
            }
            if (((Boolean) resourceData.get(
                 OpResource.INHERIT_POOL_RATE)).booleanValue() != resource.getInheritPoolRate()) {
               return false;
            }
            return ((Double) resourceData.get(OpResourcePool.HOURLY_RATE)).doubleValue() == resource.getHourlyRate();
         }

         public StringBuffer describeTo(StringBuffer stringBuffer) {
            return stringBuffer.append("Will check if a resource is the expected one");
         }
      };
   }
}
