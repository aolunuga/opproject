/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.user.test;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.project.test.OpProjectTestDataFactory;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.test.OpResourceTestDataFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserModuleChecker;
import onepoint.project.test.OpBaseOpenTestCase;

import java.sql.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Test class for the user module checker.
 *
 * @author florin.haizea
 */
public class OpUserModuleCheckerTest extends OpBaseOpenTestCase {

   private static final String RESOURCE_NAME = "resource";
   private static final String RESOURCE_POOL_NAME = "resource_pool";
   private static final String PROJECT_NAME = "project";

   private OpProjectTestDataFactory projectFactory;
   private OpResourceTestDataFactory resourceFactory;
   private OpUserModuleChecker userChecker;

   private static final String GET_PERMISSION_FOR_OBJECT = "from OpPermission permission where permission.Object = :objectID";

   private static final String ALL_NON_DEFAULT_PERMISSIONS_QUERY = "from OpPermission permission where permission.ID not in " +
                                    "(select distinct perm.ID from OpPermission perm, OpResourcePool pool, OpProjectNode project, " +
                                         "OpSubject subject " +
                                             "where (perm.Object = pool.ID and pool.Name = '${RootResourcePoolName}')" +
                                                "or (perm.Object = project.ID and project.Name = '${RootProjectPortfolioName}')" +
                                                "and perm.Subject = subject.ID " +
                                                "and (subject.Name = 'Administrator' or subject.Name = 'Everyone'))";
        
   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      projectFactory = new OpProjectTestDataFactory(session);
      resourceFactory = new OpResourceTestDataFactory(session);
      userChecker = new OpUserModuleChecker();
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
    * Test the user module checker when there are no objects without permissions in the DB.
    *
    * @throws Exception if the test fails
    */
   public void testCheckNoObjects()
        throws Exception {
      userChecker.check(session);
   }

   /**
    * Test the user module checker when there is an <code>OpResource</code> object without a permission in the DB.
    *
    * @throws Exception if the test fails
    */
   public void testCheckResourceNoPermission()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create a resource without permission
      OpResource resource = new OpResource();
      resource.setName(RESOURCE_NAME);

      broker.makePersistent(resource);
      t.commit();
      broker.close();

      //check that there is no permission for this resource
      broker = session.newBroker();
      assertEquals(0, getPermissionForResource(broker, RESOURCE_NAME).size());
      broker.close();

      userChecker.check(session);

      //check that a permission exists for this resource
      broker = session.newBroker();
      List permissions = getPermissionForResource(broker, RESOURCE_NAME);
      checkAdministratorPermission(permissions);
      broker.close();
   }

   /**
    * Test the user module checker when there is an <code>OpResource</code> object with a permission other than the
    *    administrator permisssion.
    *
    * @throws Exception if the test fails
    */
   public void testCheckResourceWithNoAdminPermission()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create a resource and a non admin subject
      OpResource resource = new OpResource();
      resource.setName(RESOURCE_NAME);

      //create a non admin permission for the resource
      OpPermission permission = new OpPermission();
      permission.setObject(resource);
      permission.setAccessLevel(OpPermission.CONTRIBUTOR);

      broker.makePersistent(resource);
      broker.makePersistent(permission);
      t.commit();
      broker.close();

      //check that there is a permission for the resource
      broker = session.newBroker();
      assertEquals(1, getPermissionForResource(broker, RESOURCE_NAME).size());
      broker.close();

      userChecker.check(session);

      //check that two permissions (the "contributor" and the admin permissions) exists for this resource
      broker = session.newBroker();
      List permissions = getPermissionForResource(broker, RESOURCE_NAME);
      assertEquals(2, permissions.size());
      boolean contributor = false;
      boolean administrator = false;
      for (Object o : permissions) {
         OpPermission perm = (OpPermission) o;
         if(perm.getAccessLevel() == OpPermission.CONTRIBUTOR) {
            contributor = true;
         }
         else {
            administrator = true;
         }
      }
      assertTrue(contributor);
      assertTrue(administrator);
      broker.close();
   }

   /**
    * Test the user module checker when there is an <code>OpResource</code> object with an administrator permission.
    *
    * @throws Exception if the test fails
    */
   public void testCheckResourceAdminPermission()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create a resource and the admin permission
      OpResource resource = new OpResource();
      resource.setName(RESOURCE_NAME);
      OpUser user = session.administrator(broker);

      OpPermission permission = new OpPermission();
      permission.setObject(resource);
      permission.setSubject(user);
      permission.setAccessLevel(OpPermission.ADMINISTRATOR);

      broker.makePersistent(resource);
      broker.makePersistent(permission);
      t.commit();
      broker.close();

      //check that there is a permission for the resource
      broker = session.newBroker();
      assertEquals(1, getPermissionForResource(broker, RESOURCE_NAME).size());
      broker.close();

      userChecker.check(session);

      //check that the administrator is the only permission for the resource
      broker = session.newBroker();
      List permissions = getPermissionForResource(broker, RESOURCE_NAME);
      checkAdministratorPermission(permissions);
      broker.close();
   }

   /**
    * Test the user module checker when there is an <code>OpResourcePool</code> object without a permission in the DB.
    *
    * @throws Exception if the test fails
    */
   public void testCheckResourcePoolNoPermission()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create a resource without permission
      OpResourcePool resourcePool = new OpResourcePool();
      resourcePool.setName(RESOURCE_POOL_NAME);

      broker.makePersistent(resourcePool);
      t.commit();
      broker.close();

      //check that there is no permission for this resource pool
      broker = session.newBroker();
      assertEquals(0, getPermissionForResourcePool(broker, RESOURCE_POOL_NAME).size());
      broker.close();

      userChecker.check(session);

      //check that a permission exists for this resource pool
      broker = session.newBroker();
      List permissions = getPermissionForResourcePool(broker, RESOURCE_POOL_NAME);
      checkAdministratorPermission(permissions);
      broker.close();
   }

   /**
    * Test the user module checker when there is an <code>OpProjectNode</code> object without a permission in the DB.
    *
    * @throws Exception if the test fails
    */
   public void testCheckProjectNodeNoPermission()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create a project node without permission
      OpProjectNode project = new OpProjectNode();
      project.setName(PROJECT_NAME);
      project.setType(OpProjectNode.PROJECT);

      broker.makePersistent(project);
      t.commit();
      broker.close();

      //check that there is no permission for the project node
      broker = session.newBroker();
      assertEquals(0, getPermissionForProjectNode(broker, PROJECT_NAME).size());
      broker.close();

      userChecker.check(session);

      //check that a permission exists for this project node
      broker = session.newBroker();
      List permissions = getPermissionForProjectNode(broker, PROJECT_NAME);
      checkAdministratorPermission(permissions);
      broker.close();
   }

   /**
    * Test the user module checker when there is an <code>OpProjectPlan</code> object without a permission in the DB.
    *
    * @throws Exception if the test fails
    */
   public void testCheckProjectPlanNoPermission()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());
      Date finishDate = new Date(getCalendarWithExactDaySet(2007, 10, 20).getTimeInMillis());

      //create a project node and the project plan without permissions
      OpProjectNode project = new OpProjectNode();
      project.setName(PROJECT_NAME);
      project.setType(OpProjectNode.PROJECT);

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setStart(startDate);
      projectPlan.setFinish(finishDate);
      projectPlan.setProjectNode(project);

      broker.makePersistent(project);
      broker.makePersistent(projectPlan);
      t.commit();
      broker.close();

      //check that there is no permission for the project
      broker = session.newBroker();
      assertEquals(0, getPermissionForProjectNode(broker, PROJECT_NAME).size());

      //check that there is no permission for the project plan
      assertEquals(0, getPermissionForProjectPlan(broker, PROJECT_NAME).size());
      broker.close();

      userChecker.check(session);

      //check that a permission exists for the project node
      broker = session.newBroker();
      List permissions = getPermissionForProjectNode(broker, PROJECT_NAME);
      checkAdministratorPermission(permissions);

      //check that a permission exists for the project plan
      permissions = getPermissionForProjectPlan(broker, PROJECT_NAME);
      checkAdministratorPermission(permissions);
      broker.close();
   }

   /**
    * Test the user module checker when there is an <code>OpProjectPlanVersion</code> object without a permission in
    *    the DB.
    *
    * @throws Exception if the test fails
    */
   public void testCheckProjectPlanVersionNoPermission()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      Date startDate = new Date(getCalendarWithExactDaySet(2007, 10, 10).getTimeInMillis());
      Date finishDate = new Date(getCalendarWithExactDaySet(2007, 10, 20).getTimeInMillis());

      //create a project node, a project plan and a project plan version without permissions
      OpProjectNode project = new OpProjectNode();
      project.setName(PROJECT_NAME);
      project.setType(OpProjectNode.PROJECT);

      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setStart(startDate);
      projectPlan.setFinish(finishDate);
      projectPlan.setProjectNode(project);

      OpProjectPlanVersion projectPlanVersion = new OpProjectPlanVersion();
      projectPlanVersion.setStart(startDate);
      projectPlanVersion.setFinish(finishDate);
      projectPlanVersion.setProjectPlan(projectPlan);

      broker.makePersistent(project);
      broker.makePersistent(projectPlan);
      broker.makePersistent(projectPlanVersion);
      t.commit();
      broker.close();

      //check that there is no permission for the project
      broker = session.newBroker();
      assertEquals(0, getPermissionForProjectNode(broker, PROJECT_NAME).size());

      //check that there is no permission for the project plan
      assertEquals(0, getPermissionForProjectPlan(broker, PROJECT_NAME).size());

      //check that there is no permission for the project plan version
      assertEquals(0, getPermissionForProjectPlanVersion(broker, PROJECT_NAME).size());
      broker.close();

      userChecker.check(session);

      //check that a permission exists for the project node
      broker = session.newBroker();
      List permissions = getPermissionForProjectNode(broker, PROJECT_NAME);
      checkAdministratorPermission(permissions);

      //check that a permission exists for the project plan
      permissions = getPermissionForProjectPlan(broker, PROJECT_NAME);
      checkAdministratorPermission(permissions);

       //check that a permission exists for the project plan
      permissions = getPermissionForProjectPlanVersion(broker, PROJECT_NAME);
      checkAdministratorPermission(permissions);
      broker.close();
   }

   /**
    * Test the user module checker when there is an <code>OpAttachment</code> object without a permission in the DB.
    *
    * @throws Exception if the test fails
    */
   public void testCheckAttachmentNoPermission()
        throws Exception {

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      //create a project node and the attachment without permissions
      OpProjectNode project = new OpProjectNode();
      project.setName(PROJECT_NAME);
      project.setType(OpProjectNode.PROJECT);

      OpAttachment attachment = new OpAttachment();
      attachment.setProjectNode(project);

      broker.makePersistent(project);
      broker.makePersistent(attachment);
      t.commit();
      broker.close();

      //check that there is no permission for the project
      broker = session.newBroker();
      assertEquals(0, getPermissionForProjectNode(broker, PROJECT_NAME).size());

      //check that there is no permission for the attachment
      assertEquals(0, getPermissionForAttachment(broker, PROJECT_NAME).size());
      broker.close();

      userChecker.check(session);

      //check that a permission exists for the project node
      broker = session.newBroker();
      List permissions = getPermissionForProjectNode(broker, PROJECT_NAME);
      checkAdministratorPermission(permissions);

      //check that a permission exists for the attachment
      permissions = getPermissionForAttachment(broker, PROJECT_NAME);
      checkAdministratorPermission(permissions);
      broker.close();
   }

   /**
    * Cleans the database
    *
    * @throws Exception if deleting test artifacts fails
    */
   private void clean()
        throws Exception {

      OpUserTestDataFactory usrData = new OpUserTestDataFactory(session);

      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();

      for (OpUser user : usrData.getAllUsers(broker)) {
         if (user.getName().equals(OpUser.ADMINISTRATOR_NAME)) {
            continue;
         }
         broker.deleteObject(user);
      }

      deleteNonDefaultPermissions(broker);
      deleteAllObjects(broker, OpAttachment.ATTACHMENT);
      deleteAllObjects(broker, OpProjectPlan.PROJECT_PLAN);
      deleteAllObjects(broker, OpProjectPlanVersion.PROJECT_PLAN_VERSION);

      for (OpProjectNode project : projectFactory.getAllProjects(broker)) {
         broker.deleteObject(project);
      }

      List resoucesList = resourceFactory.getAllResources(broker);
      for (Object aResoucesList : resoucesList) {
         OpResource resource = (OpResource) aResoucesList;
         broker.deleteObject(resource);
      }

      List poolList = resourceFactory.getAllResourcePools(broker);
      for (Object aPoolList : poolList) {
         OpResourcePool pool = (OpResourcePool) aPoolList;
         if (pool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            continue;
         }
         broker.deleteObject(pool);
      }

      transaction.commit();
      broker.close();
   }

   /**
    * Deletes all non default permissions from the database.
    *
    * @param broker - the <code>OpBroker</code> needed to perform the delete operations.
    */
   private void deleteNonDefaultPermissions(OpBroker broker) {
      OpQuery query = broker.newQuery(ALL_NON_DEFAULT_PERMISSIONS_QUERY);
      Iterator result = broker.iterate(query);
      while (result.hasNext()) {
         OpPermission permission = (OpPermission) result.next();
         broker.deleteObject(permission);
      }
   }

   /**
    * Checks that in the <code>List</code> of <code>OpPermission</code> objects passed as parameter there is only one
    *    permission and that permission has the administrator access level.
    * @param permissions - the <code>List</code> of permissions which is being checked.
    */
   private void checkAdministratorPermission(List permissions) {
      assertEquals(1, permissions.size());
      for (Object o : permissions) {
         OpPermission perm = (OpPermission) o;
         assertEquals(OpPermission.ADMINISTRATOR, perm.getAccessLevel());
      }
   }

   /**
    * Returns a <code>List</code> object over a query that gets the permission for a resource specified by its name.
    *
    * @param broker - the <code>OpBroker</code> object needed to perform the DB operations.
    * @param resourceName - the name of the resource for which the permission will be retreived.
    * @return a <code>List</code> object over a query that gets the permission for a resource specified by its name.
    */
   private List getPermissionForResource(OpBroker broker, String resourceName) {
      String resourceLocator = resourceFactory.getResourceId(resourceName);
      OpResource resource = (OpResource) broker.getObject(resourceLocator);
      OpQuery query = broker.newQuery(GET_PERMISSION_FOR_OBJECT);
      query.setLong("objectID", resource.getID());
      return broker.list(query);
   }

   /**
    * Returns a <code>List</code> object over a query that gets the permission for a resource pool specified by its name.
    *
    * @param broker       - the <code>OpBroker</code> object needed to perform the DB operations.
    * @param resourcePoolName - the name of the resource pool for which the permission will be retreived.
    * @return a <code>List</code> object over a query that gets the permission for a resource pool specified by its name.
    */
   private List getPermissionForResourcePool(OpBroker broker, String resourcePoolName) {
      String poolLocator = resourceFactory.getResourcePoolId(resourcePoolName);
      OpResourcePool resourcePool = (OpResourcePool) broker.getObject(poolLocator);
      OpQuery query = broker.newQuery(GET_PERMISSION_FOR_OBJECT);
      query.setLong("objectID", resourcePool.getID());
      return broker.list(query);
   }

    /**
    * Returns a <code>List</code> object over a query that gets the permission for a project node specified by its name.
    *
    * @param broker       - the <code>OpBroker</code> object needed to perform the DB operations.
    * @param projectName - the name of the project node for which the permission will be retreived.
    * @return a <code>List</code> object over a query that gets the permission for a project node specified by its name.
    */
   private List getPermissionForProjectNode(OpBroker broker, String projectName) {
      String projectLocator = projectFactory.getProjectId(projectName);
      OpProjectNode project = (OpProjectNode) broker.getObject(projectLocator);
      OpQuery query = broker.newQuery(GET_PERMISSION_FOR_OBJECT);
      query.setLong("objectID", project.getID());
      return broker.list(query);
   }

   /**
    * Returns a <code>List</code> object over a query that gets the permission for a project plan specified by its
    * project node name.
    *
    * @param broker      - the <code>OpBroker</code> object needed to perform the DB operations.
    * @param projectName - the name of the project node.
    * @return a <code>List</code> object over a query that gets the permission for a project plan specified by its
    *         project node name.
    */
   private List getPermissionForProjectPlan(OpBroker broker, String projectName) {
      String projectLocator = projectFactory.getProjectId(projectName);
      OpProjectNode project = (OpProjectNode) broker.getObject(projectLocator);
      OpQuery query = broker.newQuery(GET_PERMISSION_FOR_OBJECT);
      query.setLong("objectID", project.getPlan().getID());
      return broker.list(query);
   }

   /**
    * Returns a <code>List</code> object over a query that gets the permission for a project plan version specified by
    *    its project node name.
    *
    * @param broker      - the <code>OpBroker</code> object needed to perform the DB operations.
    * @param projectName - the name of the project node.
    * @return a <code>List</code> object over a query that gets the permission for a project plan version specified by
    *    its project node name.
    */
   private List getPermissionForProjectPlanVersion(OpBroker broker, String projectName) {
      String projectLocator = projectFactory.getProjectId(projectName);
      OpProjectNode project = (OpProjectNode) broker.getObject(projectLocator);
      OpQuery query = broker.newQuery(GET_PERMISSION_FOR_OBJECT);
      for(OpProjectPlanVersion version : project.getPlan().getVersions()) {
         query.setLong("objectID", version.getID());
      }
      //get the permissions of the last project plan version (normally there should be only one version)
      return broker.list(query);
   }

   /**
    * Returns a <code>List</code> object over a query that gets the permission for an attachment specified by the
    *    project node name to which is assigned.
    *
    * @param broker      - the <code>OpBroker</code> object needed to perform the DB operations.
    * @param projectName - the name of the project node.
    * @return a <code>List</code> object over a query that gets the permission for an attachment specified by the
    *    project node name to which is assigned.
    */
   private List getPermissionForAttachment(OpBroker broker, String projectName) {
      String projectLocator = projectFactory.getProjectId(projectName);
      OpProjectNode project = (OpProjectNode) broker.getObject(projectLocator);
      OpQuery query = broker.newQuery(GET_PERMISSION_FOR_OBJECT);
      for(OpAttachment attachment : project.getAttachments()) {
         query.setLong("objectID", attachment.getID());
      }
      //get the permissions of the last attachment (normally there should be only one attachment)
      return broker.list(query);
   }
}