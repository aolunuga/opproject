/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.*;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;

import java.util.*;

public class OpResourceService extends onepoint.project.OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpResourceService.class, true);

   public final static String EDIT_MODE = "edit_mode";

   public final static String RESOURCE_DATA = "resource_data";
   public final static String RESOURCE_ID = "resource_id";
   public final static String RESOURCE_IDS = "resource_ids";
   public final static String PROJECTS = "Projects";

   public final static String POOL_DATA = "pool_data";
   public final static String POOL_ID = "pool_id";
   public final static String SUPER_POOL_ID = "super_pool_id";
   public final static String POOL_IDS = "pool_ids";

   public final static String PROJECT_IDS = "project_ids";

   public final static OpResourceErrorMap ERROR_MAP = new OpResourceErrorMap();
   public final static String HAS_ASSIGNMENTS = "Assignments";

   private final static String OUTLINE_LEVEL = "outlineLevel";
   private final static String POOL_LOCATOR = "source_pool_locator";
   private final static String POOL_SELECTOR = "poolColumnsSelector";
   private final static String RESOURCE_SELECTOR = "resourceColumnsSelector";
   private final static String FILTERED_OUT_IDS = "FilteredOutIds";
   private static final String ENABLE_POOLS = "EnablePools";
   private static final String ENABLE_RESOURCES = "EnableResources";


   public XMessage insertResource(XSession s, XMessage request) {
      logger.debug("OpResourceService.insertResource()");
      OpProjectSession session = (OpProjectSession) s;

      HashMap resource_data = (HashMap) (request.getArgument(RESOURCE_DATA));
      // *** More error handling needed (check mandatory fields)

      XMessage reply = new XMessage();

      OpResource resource = new OpResource();
      resource.setName((String) (resource_data.get(OpResource.NAME)));
      resource.setDescription((String) (resource_data.get(OpResource.DESCRIPTION)));
      resource.setAvailable(((Double) resource_data.get(OpResource.AVAILABLE)).doubleValue());
      resource.setHourlyRate(((Double) (resource_data.get(OpResource.HOURLY_RATE))).doubleValue());
      resource.setInheritPoolRate(((Boolean) (resource_data.get(OpResource.INHERIT_POOL_RATE))).booleanValue());

      OpBroker broker = session.newBroker();

      // Set pool for this resource and set hourly rate of pool if inherit is true
      String pool_id_string = (String) (resource_data.get("PoolID"));
      logger.debug("***INTO-POOL: " + pool_id_string);
      if (pool_id_string != null) {
         OpResourcePool pool = (OpResourcePool) (broker.getObject(pool_id_string));
         if (pool != null) {
            resource.setPool(pool);
         }
         else {
            resource.setPool(findRootPool(broker));
         }
      }
      if (resource.getInheritPoolRate()) {
         resource.setHourlyRate(resource.getPool().getHourlyRate());
      }

      // Check manager access for pool
      if (!session.checkAccessLevel(broker, resource.getPool().getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Insert access to pool denied; ID = " + resource.getPool().getID());
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      // check mandatory input fields
      if (resource.getName() == null || resource.getName().length() == 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED));
         broker.close();
         return reply;
      }
      //check if resource name contains invalid char %
      if (resource.getName().indexOf("%") != -1) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_VALID));
         broker.close();
         return reply;
      }

      double maxAvailability = Double.parseDouble(OpSettings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
      // check valid availability range [0..maxAvailability]
      int availability = ((Double) resource_data.get(OpResource.AVAILABLE)).intValue();
      if (availability < 0 || availability > maxAvailability) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.AVAILABILITY_NOT_VALID));
         broker.close();
         return reply;
      }

      // check valid hourly rate
      if (resource.getHourlyRate() < 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
         broker.close();
         return reply;
      }

      // check if resource name is already used
      OpQuery query = broker.newQuery("select resource.ID from OpResource as resource where resource.Name = :resourceName");
      query.setString("resourceName", resource.getName());
      Iterator resourceIds = broker.iterate(query);
      if (resourceIds.hasNext()) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_UNIQUE));
         broker.close();
         return reply;
      }

      // *** TODO: Currently a work-around (should use choice instead of ID)
      String user_id_string = (String) (resource_data.get("UserID"));
      if ((user_id_string != null) && (user_id_string.length() > 0)) {
         OpUser user = (OpUser) (broker.getObject(user_id_string));
         resource.setUser(user);
      }

      OpTransaction t = broker.newTransaction();

      broker.makePersistent(resource);

      // Add projects assignments
      ArrayList assigned_projects = (ArrayList) (resource_data.get(PROJECTS));
      if ((assigned_projects != null) && (assigned_projects.size() > 0)) {
         logger.debug("ASSIGNED " + assigned_projects);
         OpProjectNode projectNode = null;
         OpProjectNodeAssignment projectNodeAssignment = null;
         // TODO: Check for read-access to project node (important if we add Web Services access)
         for (int i = 0; i < assigned_projects.size(); i++) {
            projectNode = (OpProjectNode) (broker.getObject((String) assigned_projects.get(i)));
            projectNodeAssignment = new OpProjectNodeAssignment();
            projectNodeAssignment.setResource(resource);
            projectNodeAssignment.setProjectNode(projectNode);
            broker.makePersistent(projectNodeAssignment);
            OpProjectAdministrationService.insertContributorPermission(broker, projectNode, resource);
         }
      }

      XComponent permission_set = (XComponent) resource_data.get(OpPermissionSetFactory.PERMISSION_SET);
      boolean result = OpPermissionSetFactory.storePermissionSet(broker, resource, permission_set);
      if (!result) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.PERMISSIONS_LEVEL_ERROR));
         broker.close();
         return reply;
      }

      t.commit();

      broker.close();
      logger.debug("/OpResourceService.insertResource()");
      return reply;
   }

   public XMessage importUser(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

      logger.debug("OpResourceService.importUser()");
      HashMap resource_data = (HashMap) (request.getArgument(RESOURCE_DATA));
      // *** More error handling needed (check mandatory fields)

      XMessage reply = new XMessage();

      OpBroker broker = session.newBroker();
      OpResource resource = new OpResource();

      // get associated user
      // *** TODO: Currently a work-around (should use choice instead of ID)
      String user_id_string = (String) (resource_data.get("UserID"));
      OpUser user = null;
      if ((user_id_string != null) && (user_id_string.length() > 0)) {
         user = (OpUser) (broker.getObject(user_id_string));
         resource.setUser(user);
      }
      else {
         logger.warn("UserID not set, could not import user as resource");
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.USER_ID_NOT_SPECIFIED));
         return reply;
      }
      OpContact contact = user.getContact();
      String name = user.getName();

      // check if resource name is already used
      OpQuery query = broker.newQuery("select resource.ID from OpResource as resource where resource.Name = :resourceName");
      query.setString("resourceName", name);
      Iterator resourceIds = broker.iterate(query);
      if (resourceIds.hasNext()) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_UNIQUE));
         broker.close();
         return reply;
      }

      resource.setName(name);
      StringBuffer description = new StringBuffer();
      if ((contact.getFirstName() != null) && (contact.getFirstName().length() > 0)) {
         description.append(contact.getFirstName());
      }
      if ((contact.getLastName() != null) && (contact.getLastName().length() > 0)) {
         if (description.length() > 0) {
            description.append(' ');
         }
         description.append(contact.getLastName());
      }
      if (description.length() > 0) {
         resource.setDescription(description.toString());
      }

      // check valid availability range [0..system.maxAvailable]
      double availability = ((Double) resource_data.get(OpResource.AVAILABLE)).doubleValue();
      double maxAvailable = Double.parseDouble(OpSettings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
      if (availability < 0 || availability > maxAvailable) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.AVAILABILITY_NOT_VALID));
         broker.close();
         return reply;
      }
      else {
         resource.setAvailable(availability);
      }

      // check valid hourly rate
      if (((Double) (resource_data.get(OpResource.HOURLY_RATE))).doubleValue() < 0) {
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
         return reply;
      }
      else {
         resource.setHourlyRate(((Double) (resource_data.get(OpResource.HOURLY_RATE))).doubleValue());
      }

      resource.setInheritPoolRate(((Boolean) (resource_data.get(OpResource.INHERIT_POOL_RATE))).booleanValue());

      // Check if resource is to be created inside a pool
      String pool_id_string = (String) (resource_data.get("PoolID"));
      logger.debug("***INTO-POOL: " + pool_id_string);
      if ((pool_id_string != null) && (pool_id_string.length() > 0)) {
         OpResourcePool pool;
         if (!pool_id_string.equals("0")) {
            pool = (OpResourcePool) (broker.getObject(pool_id_string));
         }
         else {
            pool = OpResourceService.findRootPool(broker);
         }

         // Check manager access for pool
         if (!session.checkAccessLevel(broker, pool.getID(), OpPermission.MANAGER)) {
            logger.warn("ERROR: Import access to pool denied; ID = " + pool.getID());
            broker.close();
            reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
            return reply;
         }
         //set up the pool for the resource
         resource.setPool(pool);
      }

      OpTransaction t = broker.newTransaction();

      broker.makePersistent(resource);

      // Add projects assignments
      ArrayList assigned_projects = (ArrayList) (resource_data.get(PROJECTS));
      if ((assigned_projects != null) && (assigned_projects.size() > 0)) {
         logger.debug("ASSIGNED " + assigned_projects);
         OpProjectNode projectNode = null;
         OpProjectNodeAssignment projectNodeAssignment = null;
         for (int i = 0; i < assigned_projects.size(); i++) {
            projectNode = (OpProjectNode) (broker.getObject((String) assigned_projects.get(i)));
            projectNodeAssignment = new OpProjectNodeAssignment();
            projectNodeAssignment.setResource(resource);
            projectNodeAssignment.setProjectNode(projectNode);
            broker.makePersistent(projectNodeAssignment);
            OpProjectAdministrationService.insertContributorPermission(broker, projectNode, resource);
         }
      }

      t.commit();
      logger.debug("/OpResourceService.importUser()");
      broker.close();
      return reply;
   }

   public XMessage updateResource(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

      String id_string = (String) (request.getArgument(RESOURCE_ID));
      logger.debug("OpResourceService.updateResource(): id = " + id_string);
      HashMap resource_data = (HashMap) (request.getArgument(RESOURCE_DATA));

      XMessage reply = new XMessage();

      OpBroker broker = session.newBroker();

      OpResource resource = (OpResource) (broker.getObject(id_string));
      if (resource == null) {
         logger.warn("ERROR: Could not find object with ID " + id_string);
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NOT_FOUND));
         return reply;
      }

      // Check manager access
      if (!session.checkAccessLevel(broker, resource.getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Udpate access to resource denied; ID = " + id_string);
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      String resourceName = (String) (resource_data.get(OpResource.NAME));

      // check mandatory input fields
      if (resourceName == null || resourceName.length() == 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED));
         broker.close();
         return reply;
      }
      //check if resource name contains invalid char %
      if (resourceName.indexOf("%") != -1) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_VALID));
         broker.close();
         return reply;
      }
      // check if resource name is already used
      OpQuery query = broker.newQuery("select resource from OpResource as resource where resource.Name = :resourceName");
      query.setString("resourceName", resourceName);
      Iterator resources = broker.iterate(query);
      while (resources.hasNext()) {
         OpResource other = (OpResource) resources.next();
         if (other.getID() != resource.getID()) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_UNIQUE));
            broker.close();
            return reply;
         }
      }

      resource.setName(resourceName);
      resource.setDescription((String) (resource_data.get(OpResource.DESCRIPTION)));

      double oldAvailableValue = resource.getAvailable();
      double availableValue = ((Double) (resource_data.get(OpResource.AVAILABLE))).doubleValue();
      boolean availibityChanged = (oldAvailableValue != availableValue);
      resource.setAvailable(availableValue);

      double oldHourlyRate = resource.getHourlyRate();
      double hourlyRate = ((Double) (resource_data.get(OpResource.HOURLY_RATE))).doubleValue();
      resource.setHourlyRate(hourlyRate);
      resource.setInheritPoolRate(((Boolean) (resource_data.get(OpResource.INHERIT_POOL_RATE))).booleanValue());
      // Overwrite hourly rate w/pool rate if inherit is true
      if (resource.getInheritPoolRate()) {
         hourlyRate = resource.getPool().getHourlyRate();
         resource.setHourlyRate(resource.getPool().getHourlyRate());
      }

      boolean hourlyRateChanged = (oldHourlyRate != hourlyRate);

      double maxAvailability = Double.parseDouble(OpSettings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
      // check valid availability range [0..maxAvailability]
      double availability = ((Double) resource_data.get(OpResource.AVAILABLE)).doubleValue();
      if (availability < 0 || availability > maxAvailability) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.AVAILABILITY_NOT_VALID));
         broker.close();
         return reply;
      }

      // check valid hourly rate
      if (resource.getHourlyRate() < 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      // *** TODO: Currently a work-around (should use choice instead of ID)
      String user_id_string = (String) (resource_data.get("UserID"));
      if ((user_id_string != null) && (user_id_string.length() > 0)) {
         OpUser user = (OpUser) (broker.getObject(user_id_string));
         Iterator assignments = resource.getProjectNodeAssignments().iterator();
         //user has no responsible user -> for all the resource's project assignments add a contributor permission entry
         if (resource.getUser() == null) {
            //set up the new responsible user for the resource
            resource.setUser(user);
            while (assignments.hasNext()) {
               OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) assignments.next();
               OpProjectAdministrationService.insertContributorPermission(broker, assignment.getProjectNode(), resource);
            }
         }
         else {
            OpUser oldResponsibleUser = resource.getUser();
            if (oldResponsibleUser.getID() != user.getID()) { //responsible user has been changed
               //list of resource ids for which the resource's old user is responsible
               query = broker.newQuery("select resource.ID from OpUser user inner join user.Resources resource where user.ID = :userId ");
               query.setLong("userId", oldResponsibleUser.getID());
               List resourceIds = broker.list(query);

               //set up the new responsible user for the resource
               resource.setUser(user);

               while (assignments.hasNext()) {
                  OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) assignments.next();
                  query = broker.newQuery("select count(assignment) from OpProjectNodeAssignment as assignment where assignment.Resource.ID in (:resourceIds) and assignment.ProjectNode.ID  = :projectId");
                  query.setLong("projectId", assignment.getProjectNode().getID());
                  query.setCollection("resourceIds", resourceIds);
                  Integer counter = (Integer) broker.iterate(query).next();
                  // at least one project assignment exist for the resources the old user was responsible
                  if (counter.intValue() > 1) {
                     OpProjectAdministrationService.insertContributorPermission(broker, assignment.getProjectNode(), resource);
                  }
                  else {//update the permision subject for the persisted assignment projectNode
                     query = broker.newQuery("select permission from OpPermission permission where permission.Object.ID = :projectId " +
                          "and permission.Subject.ID = :subjectId and permission.AccessLevel = :accessLevel and permission.SystemManaged = :systemManaged");
                     query.setLong("projectId", assignment.getProjectNode().getID());
                     query.setLong("subjectId", oldResponsibleUser.getID());
                     query.setByte("accessLevel", OpPermission.CONTRIBUTOR);
                     query.setBoolean("systemManaged", true);
                     List permissions = broker.list(query);
                     for (int i = 0; i < permissions.size(); i++) {
                        broker.deleteObject((OpPermission) permissions.get(i));
                        OpProjectAdministrationService.insertContributorPermission(broker, assignment.getProjectNode(), resource);
                     }
                  }
               }
            }
         }
      }
      else {
         resource.setUser(null);
      }

      //update resource
      broker.updateObject(resource);

      //update resource assignments
      ArrayList assigned_projects = (ArrayList) (resource_data.get(PROJECTS));
      reply = updateProjectAssignments(session, assigned_projects, broker, resource);
      if (reply.getError() != null) {
         broker.close();
         return reply;
      }

      //update personnel costs
      if (hourlyRateChanged) {
         updatePersonnelCosts(broker, resource);
      }

      //update availability
      if (availibityChanged) {
         updateAvailability(broker, resource);
      }

      //update permissions
      XComponent permission_set = (XComponent) resource_data.get(OpPermissionSetFactory.PERMISSION_SET);
      boolean result = OpPermissionSetFactory.storePermissionSet(broker, resource, permission_set);
      if (!result) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.PERMISSIONS_LEVEL_ERROR));
         broker.close();
         return reply;
      }

      t.commit();
      logger.debug("/OpResourceService.updateResource()");
      broker.close();
      return reply;
   }

   /**
    * Gets all the assignments for a given resource from the project plans which are checked-out.
    *
    * @param broker     a <code>OpBroker</code> used for performing business operations.
    * @param resourceId a <code>long</code> representing the id of a resource.
    * @return a <code>Iterator</code> over the assignments of the resource.
    */
   private Iterator getAssignmentsForWorkingVersions(OpBroker broker, long resourceId) {
      StringBuffer queryString = new StringBuffer();
      queryString.append("select assignment from OpResource resource inner join resource.AssignmentVersions assignment ");
      queryString.append("where assignment.ActivityVersion.PlanVersion.VersionNumber = ? and resource.ID = ?");

      OpQuery query = broker.newQuery(queryString.toString());
      query.setInteger(0, OpProjectAdministrationService.WORKING_VERSION_NUMBER);
      query.setLong(1, resourceId);

      return broker.list(query).iterator();
   }

   /**
    * Return a not null message if the resource has any assignments.
    *
    * @param s
    * @param request
    * @return null if resource has no assignments
    */
   public XMessage hasAssignments(XSession s, XMessage request) {
      String id_string = (String) (request.getArgument(RESOURCE_ID));
      OpBroker broker = ((OpProjectSession) s).newBroker();
      OpResource resource = (OpResource) (broker.getObject(id_string));
      boolean hasAssignments = false;
      if (resource.getAssignmentVersions().size() != 0) {
         hasAssignments = true;
      }
      if (resource.getActivityAssignments().size() != 0) {
         hasAssignments = true;
      }
      XMessage xMessage = new XMessage();
      xMessage.setArgument(HAS_ASSIGNMENTS, Boolean.valueOf(hasAssignments));
      return xMessage;
   }

   /**
    * Return a not null message if the pool has any resources with any assignments.
    *
    * @param s
    * @param request
    * @return null if pool has no resources with assignments
    */
   public XMessage hasResourceAssignments(XSession s, XMessage request) {
      String id_string = (String) (request.getArgument(POOL_ID));
      boolean hasAssignments = false;
      OpBroker broker = ((OpProjectSession) s).newBroker();
      OpResourcePool pool = (OpResourcePool) broker.getObject(id_string);
      for (Iterator iterator = pool.getResources().iterator(); iterator.hasNext();) {
         OpResource resource = (OpResource) iterator.next();
         if (resource.getAssignmentVersions().size() != 0) {
            hasAssignments = true;
            break;
         }
         if (resource.getActivityAssignments().size() != 0) {
            hasAssignments = true;
         }
      }
      XMessage xMessage = new XMessage();
      xMessage.setArgument(HAS_ASSIGNMENTS, Boolean.valueOf(hasAssignments));
      return xMessage;
   }

   /**
    * Updates the resource availibility for the checked out projects.
    *
    * @param broker   a <code>OpBroker</code> used for performing business operations.
    * @param resource a <code>OpResource</code> representing the resource being edited.
    */
   private void updateAvailability(OpBroker broker, OpResource resource) {
      Iterator it = getAssignmentsForWorkingVersions(broker, resource.getID());
      while (it.hasNext()) {
         OpAssignmentVersion assignmentVersion = (OpAssignmentVersion) it.next();
         if (assignmentVersion.getAssigned() > resource.getAvailable()) {
            assignmentVersion.setAssigned(resource.getAvailable());
            broker.updateObject(assignmentVersion);
         }
      }
   }

   /**
    * Updates the base personnel costs of the assignemnts of the activities that are checked out at the moment when the
    * resource is updated.
    *
    * @param broker   a <code>OpBroker</code> used for performing business operations.
    * @param resource a <code>OpResource</code> representing the resource that has been updated.
    */
   private void updatePersonnelCosts(OpBroker broker, OpResource resource) {
      Iterator it = getAssignmentsForWorkingVersions(broker, resource.getID());
      while (it.hasNext()) {
         OpAssignmentVersion assignmentVersion = (OpAssignmentVersion) it.next();
         assignmentVersion.setBaseCosts(assignmentVersion.getBaseEffort() * resource.getHourlyRate());
         broker.updateObject(assignmentVersion);

         OpActivityVersion activityVersion = assignmentVersion.getActivityVersion();
         double oldPersonnelCosts = activityVersion.getBasePersonnelCosts();
         double baseEffort = activityVersion.getBaseEffort();

         double sumAssigned = 0;
         OpQuery query = broker.newQuery("select sum(assignment.Assigned) from OpAssignmentVersion assignment where assignment.ActivityVersion.ID=?");
         query.setLong(0, activityVersion.getID());
         sumAssigned = ((Double) broker.list(query).iterator().next()).doubleValue();

         double personnelCosts = 0;
         Iterator it1 = activityVersion.getAssignmentVersions().iterator();
         while (it1.hasNext()) {
            OpAssignmentVersion assignment = ((OpAssignmentVersion) it1.next());
            OpResource assignmentResource = assignment.getResource();
            double assignmentProportion = assignment.getAssigned() / sumAssigned;
            personnelCosts += assignmentProportion * baseEffort * assignmentResource.getHourlyRate();
         }
         //update activity
         activityVersion.setBasePersonnelCosts(personnelCosts);

         //update all super activities
         while (activityVersion.getSuperActivityVersion() != null) {
            OpActivityVersion superActivityVersion = activityVersion.getSuperActivityVersion();
            double costsDifference = activityVersion.getBasePersonnelCosts() - oldPersonnelCosts;
            oldPersonnelCosts = superActivityVersion.getBasePersonnelCosts();
            superActivityVersion.setBasePersonnelCosts(superActivityVersion.getBasePersonnelCosts() + costsDifference);
            broker.updateObject(activityVersion);
            activityVersion = superActivityVersion;
         }
         broker.updateObject(activityVersion);
      }
   }

   /**
    * Updates the already existent project assignments for the modified resource.
    *
    * @param session           a <code>OpProjectSession</code> representing the current session
    * @param assigned_projects a <code>List</code > of <code>String</code> representing assignment locators.
    * @param broker            a <code>OpBroker</code> used for performing business operations.
    * @param resource          a <code>OpResource</code> representing the resource which was edited.
    * @return <code>XMessage</code>
    */
   private XMessage updateProjectAssignments(OpProjectSession session, List assigned_projects, OpBroker broker, OpResource resource) {
      OpQuery query = null;
      //the reply message
      XMessage reply = new XMessage();
      logger.debug("ASSIGNED " + assigned_projects);
      // Query stored project node assignments
      query = broker.newQuery("select assignment.ProjectNode.ID from OpProjectNodeAssignment as assignment where assignment.Resource.ID = ?");
      query.setLong(0, resource.getID());

      Iterator result = broker.list(query).iterator();
      HashSet storedAssignedIds = new HashSet();
      while (result.hasNext()) {
         storedAssignedIds.add(result.next());
      }

      // *** Iterate client-side assigned-project-ids (from length *down* to 0) and remove ids found in set
      // ==> If not found: Insert new assignment right away
      OpLocator locator = null;
      OpProjectNode projectNode = null;
      OpProjectNodeAssignment projectNodeAssignment = null;
      if ((assigned_projects != null) && (assigned_projects.size() > 0)) {
         for (int i = assigned_projects.size() - 1; i >= 0; i--) {
            locator = OpLocator.parseLocator((String) assigned_projects.get(i));
            if (!storedAssignedIds.remove(new Long(locator.getID()))) {
               projectNode = (OpProjectNode) broker.getObject(locator.getPrototype().getInstanceClass(), locator.getID());
               projectNodeAssignment = new OpProjectNodeAssignment();
               projectNodeAssignment.setResource(resource);
               projectNodeAssignment.setProjectNode(projectNode);
               broker.makePersistent(projectNodeAssignment);
               OpProjectAdministrationService.insertContributorPermission(broker, projectNode, resource);
            }
         }
      }
      // Delete outdated project node assignments if no activity assignments exist for the resource
      if (storedAssignedIds.size() > 0) {

         // Remove auto-added (i.e., system-managed) contributor permissions for project nodes and resources
         deleteContributorPermission(broker, resource.getUser(), storedAssignedIds);

         query = broker.newQuery("select assignment from OpProjectNodeAssignment as assignment where assignment.Resource.ID = :resourceId and assignment.ProjectNode.ID in (:projectNodeIds)");
         query.setLong("resourceId", resource.getID());
         query.setCollection("projectNodeIds", storedAssignedIds);
         result = broker.list(query).iterator();
         while (result.hasNext()) {
            OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) result.next();
            int activityAssignmentsCounter = getActivityAssignmentsCount(broker, assignment.getResource(), assignment.getProjectNode().getPlan());
            if (activityAssignmentsCounter > 0) {
               reply.setError(session.newError(ERROR_MAP, OpResourceError.ACTIVITY_ASSIGNMENTS_EXIST_ERROR));
               return reply;
            }
            else {
               broker.deleteObject(assignment);
            }
         }
      }
      return reply;
   }

   public XMessage deleteResources(XSession s, XMessage request) {

      // Check manager access to pools of resources; delete all accessible resources; return error if not all deleted

      OpProjectSession session = (OpProjectSession) s;

      ArrayList id_strings = (ArrayList) (request.getArgument(RESOURCE_IDS));
      logger.debug("OpResourceService.deleteResources(): resource_ids = " + id_strings);

      OpBroker broker = session.newBroker();

      List resourceIds = new ArrayList();
      XMessage reply = new XMessage();

      if (id_strings.size() == 0) {
         return new XMessage();
         // TODO: Return error (nothing to delete)
      }
      for (int i = 0; i < id_strings.size(); i++) {
         resourceIds.add(new Long(OpLocator.parseLocator((String) id_strings.get(i)).getID()));
      }
      OpQuery query = broker.newQuery("select resource.Pool.ID from OpResource as resource where resource.ID in (:resourceIds)");
      query.setCollection("resourceIds", resourceIds);
      List poolIds = broker.list(query);
      Set accessiblePoolIds = session.accessibleIds(broker, poolIds, OpPermission.MANAGER);

      if (accessiblePoolIds.size() == 0) {
         logger.warn("Manager access to pool " + poolIds + " denied");
         reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();
      /* --- Not yet support by Hibernate (delete query against joined-subclass)
      query = broker.newQuery("delete from OpResource where OpResource.ID in (:resourceIds) and OpResource.Pool.ID in :(accessiblePoolIds)");
      broker.execute(query);
      */
      query = broker.newQuery("select resource from OpResource as resource where resource.ID in (:resourceIds) and resource.Pool.ID in (:accessiblePoolIds) " +
           "and size(resource.ActivityAssignments) = 0 and size(resource.AssignmentVersions) = 0");
      query.setCollection("resourceIds", resourceIds);
      query.setCollection("accessiblePoolIds", accessiblePoolIds);
      List resources = broker.list(query);
      /*size of persistent resources list without activityAssignments and assignmentVersions should be equal with selected resources size*/
      if (resources.size() != resourceIds.size()) {
         logger.warn("Resource from " + resources + " are used in project assignments");
         reply.setError(session.newError(ERROR_MAP, OpResourceError.DELETE_RESOURCE_ASSIGNMENTS_DENIED));
         broker.close();
         return reply;
      }
      for (Iterator it = resources.iterator(); it.hasNext();) {
         OpResource resource = (OpResource) it.next();
         // get all project node assignment ids for the resource
         query = broker.newQuery("select assignment.ProjectNode.ID from OpProjectNodeAssignment as assignment where assignment.Resource.ID = ?");
         query.setLong(0, resource.getID());
         Iterator result = broker.list(query).iterator();

         Set assignedProjectIds = new HashSet();
         while (result.hasNext()) {
            assignedProjectIds.add(result.next());
         }
         //remove contributor permissions
         deleteContributorPermission(broker, resource.getUser(), assignedProjectIds);
         //remove resource entity
         broker.deleteObject(resource);
      }
      t.commit();

      if (accessiblePoolIds.size() < poolIds.size()) {
         ; // TODO: Return ("informative") error if notAllAccessible
      }

      logger.debug("/OpResourceService.deleteResources()");
      broker.close();
      return null;
   }

   public XMessage insertPool(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

      logger.debug("OpResourceService.insertPool()");
      HashMap pool_data = (HashMap) (request.getArgument(POOL_DATA));

      OpResourcePool pool = new OpResourcePool();
      pool.setName((String) (pool_data.get(OpResourcePool.NAME)));
      pool.setDescription((String) (pool_data.get(OpResourcePool.DESCRIPTION)));
      pool.setHourlyRate(((Double) (pool_data.get(OpResourcePool.HOURLY_RATE))).doubleValue());

      XMessage reply = new XMessage();

      // check mandatory input fields
      if (pool.getName() == null || pool.getName().length() == 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.POOL_NAME_NOT_SPECIFIED));
         return reply;
      }
      // check valid hourly rate
      if (pool.getHourlyRate() < 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
         return reply;
      }

      OpBroker broker = session.newBroker();

      // Set pool for this resource and set hourly rate of pool if inherit is true
      String superPoolLocator = (String) (pool_data.get("SuperPoolID"));
      logger.debug("***INTO-SUPER-POOL: " + superPoolLocator);
      if (superPoolLocator != null) {
         OpResourcePool superPool = (OpResourcePool) (broker.getObject(superPoolLocator));
         if (pool != null) {
            pool.setSuperPool(superPool);
         }
         else {
            pool.setSuperPool(findRootPool(broker));
         }
      }

      // Check manager access for super pool
      if (!session.checkAccessLevel(broker, pool.getSuperPool().getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Insert access to pool denied; ID = " + pool.getSuperPool().getID());
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      // check if pool name is already used
      OpQuery query = broker.newQuery("select pool.ID from OpResourcePool as pool where pool.Name = :poolName");
      query.setString("poolName", pool.getName());
      Iterator poolIds = broker.iterate(query);
      if (poolIds.hasNext()) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.POOL_NAME_NOT_UNIQUE));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();
      broker.makePersistent(pool);

      XComponent permission_set = (XComponent) pool_data.get(OpPermissionSetFactory.PERMISSION_SET);
      boolean result = OpPermissionSetFactory.storePermissionSet(broker, pool, permission_set);
      if (!result) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.PERMISSIONS_LEVEL_ERROR));
         broker.close();
         return reply;
      }


      t.commit();
      logger.debug("/OpResourceService.insertPool()");
      broker.close();
      return reply;
   }

   public XMessage updatePool(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

      String id_string = (String) (request.getArgument(POOL_ID));
      logger.debug("OpResourceService.updatePool(): id = " + id_string);
      HashMap pool_data = (HashMap) (request.getArgument(POOL_DATA));

      OpBroker broker = session.newBroker();
      XMessage reply = new XMessage();

      OpResourcePool pool = (OpResourcePool) (broker.getObject(id_string));
      if (pool == null) {
         logger.warn("ERROR: Could not find object with ID " + id_string);
         reply.setError(session.newError(ERROR_MAP, OpResourceError.POOL_NOT_FOUND));
         broker.close();
         return reply;
      }

      // Check manager access
      if (!session.checkAccessLevel(broker, pool.getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Udpate access to pool denied; ID = " + id_string);
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      String poolName = (String) (pool_data.get(OpResourcePool.NAME));

      // check mandatory input fields
      if (poolName == null || poolName.length() == 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.POOL_NAME_NOT_SPECIFIED));
         broker.close();
         return reply;
      }

      // check if pool name is already used
      OpQuery query = broker.newQuery("select pool from OpResourcePool as pool where pool.Name = :poolName");
      query.setString("poolName", poolName);
      Iterator pools = broker.iterate(query);
      while (pools.hasNext()) {
         OpResourcePool other = (OpResourcePool) pools.next();
         if (other.getID() != pool.getID()) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.POOL_NAME_NOT_UNIQUE));
            broker.close();
            return reply;
         }
      }

      //name and description for root pool should not be editable
      if (findRootPool(broker).getID() != pool.getID()) {
         pool.setName(poolName);
         pool.setDescription((String) (pool_data.get(OpResourcePool.DESCRIPTION)));
      }

      double hourlyRate = ((Double) (pool_data.get(OpResourcePool.HOURLY_RATE))).doubleValue();
      boolean poolRateChanged = false;
      if (hourlyRate != pool.getHourlyRate()) {
         poolRateChanged = true;
         pool.setHourlyRate(hourlyRate);
      }

      // check valid hourly rate
      if (pool.getHourlyRate() < 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
         broker.close();
         return reply;
      }


      OpTransaction t = broker.newTransaction();

      broker.updateObject(pool);

      XComponent permission_set = (XComponent) pool_data.get(OpPermissionSetFactory.PERMISSION_SET);
      boolean result = OpPermissionSetFactory.storePermissionSet(broker, pool, permission_set);
      if (!result) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.PERMISSIONS_LEVEL_ERROR));
         broker.close();
         return reply;
      }

      // Update all inherited hourly-rate fields of pool resources if pool rate has changed
      // TODO: Probably optimize by using a query for "InheritPoolRate = true"
      if (poolRateChanged) {
         Iterator resources = pool.getResources().iterator();
         OpResource resource = null;
         while (resources.hasNext()) {
            resource = (OpResource) resources.next();
            if (resource.getInheritPoolRate()) {
               resource.setHourlyRate(pool.getHourlyRate());
               updatePersonnelCosts(broker, resource);
               broker.updateObject(resource);
            }
         }
      }

      t.commit();
      logger.debug("/OpResourceService.updateResource()");
      broker.close();
      return reply;
   }

   public XMessage deletePools(XSession s, XMessage request) {

      // TODO: Maybe add force-flag (like there was before falsely for delete-group)
      // (Deny deletion of not-empty pools if force flag deleteIfNotEmpty is not set)

      OpProjectSession session = (OpProjectSession) s;

      ArrayList id_strings = (ArrayList) (request.getArgument(POOL_IDS));
      logger.debug("OpResourceService.deletePools(): pool_ids = " + id_strings);

      OpBroker broker = session.newBroker();

      List poolIds = new ArrayList();
      XMessage reply = new XMessage();

      for (int i = 0; i < id_strings.size(); i++) {
         poolIds.add(new Long(OpLocator.parseLocator((String) id_strings.get(i)).getID()));
      }
      OpQuery query = broker.newQuery("select pool.SuperPool.ID from OpResourcePool as pool where pool.ID in (:poolIds)");
      query.setCollection("poolIds", poolIds);
      List superPoolIds = broker.list(query);
      Set accessibleSuperPoolIds = session.accessibleIds(broker, superPoolIds, OpPermission.MANAGER);

      if (accessibleSuperPoolIds.size() == 0) {
         logger.warn("Manager access to super pools " + superPoolIds + " denied");
         reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
         broker.close();
         return reply;

      }

      OpTransaction t = broker.newTransaction();
      /* --- Not yet support by Hibernate (delete query against joined-subclass)
      query = broker.newQuery("delete from OpResourcePool where OpResourcePool.ID in (:poolIds) and OpResourcePool.SuperPool.ID in (:accessibleSuperPoolIds)");
      broker.execute(query);
      */
      query = broker.newQuery("select pool from OpResourcePool as pool where pool.ID in (:poolIds) and pool.SuperPool.ID in (:accessibleSuperPoolIds)");
      query.setCollection("poolIds", poolIds);
      query.setCollection("accessibleSuperPoolIds", accessibleSuperPoolIds);
      Iterator result = broker.iterate(query);
      while (result.hasNext()) {
         OpResourcePool pool = (OpResourcePool) result.next();
         Set resources = pool.getResources();
         for (Iterator it = resources.iterator(); it.hasNext();) {
            OpResource resource = (OpResource) it.next();
            if (!resource.getActivityAssignments().isEmpty() || !resource.getAssignmentVersions().isEmpty()) {
               logger.warn("Resource " + resource.getName() + " is used in project assignments");
               reply.setError(session.newError(ERROR_MAP, OpResourceError.DELETE_POOL_RESOURCE_ASSIGNMENTS_DENIED));
               t.rollback();
               broker.close();
               return reply;//fail fast
            }
         }
         //...finally delete pool
         broker.deleteObject(pool);
      }
      t.commit();

      if (accessibleSuperPoolIds.size() < superPoolIds.size()) {
         ; // TODO: Return ("informative") error if notAllAccessible
      }

      logger.debug("/OpResourceService.deletePools()");
      broker.close();
      return null;
   }

   public XMessage assignToProject(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

      // TODO: Check read-access to project and manage-permissions of resources (bulk-check IDs)

      OpBroker broker = session.newBroker();
      XMessage reply = new XMessage();

      logger.debug("OpResourceService.assignToProject()");

      ArrayList resource_id_strings = (ArrayList) (request.getArgument(RESOURCE_IDS));
      List projectIds = (List) (request.getArgument(PROJECT_IDS));

      // TODO: Error handling for assign-to-project is missing

      // *** Retrieve target project
      OpTransaction t = broker.newTransaction();
      for (Iterator it = projectIds.iterator(); it.hasNext();) {
         String projectId = (String) it.next();
         OpProjectNode targetProjectNode = (OpProjectNode) (broker.getObject(projectId));
         if (targetProjectNode == null) {
            logger.warn("ERROR: Could not find object with ID " + projectIds);
            continue;
         }

         if (!session.checkAccessLevel(broker, targetProjectNode.getID(), OpPermission.OBSERVER)) {
            logger.warn("ERROR: Could not access object with ID " + projectIds + " as observer");
            continue;
         }

         // Check manager access to resources
         Set resourceIds = new HashSet();
         for (int i = 0; i < resource_id_strings.size(); i++) {
            String resourceID = (String) resource_id_strings.get(i);
            OpObject object = broker.getObject(resourceID);
            collectResources(object, resourceIds);
         }

         Iterator accessibleResources = session.accessibleObjects(broker, resourceIds, OpPermission.MANAGER, OpObjectOrderCriteria.EMPTY_ORDER);
         /*no accesible resources entities  */
         if (!accessibleResources.hasNext()) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.ASSIGN_ACCESS_DENIED));
            broker.close();
            return reply;
         }

         //project resource ids assignments
         String assignmentQuery = "select assignment.Resource.ID from OpProjectNodeAssignment as assignment where assignment.ProjectNode.ID = ?";
         OpQuery query = broker.newQuery(assignmentQuery);
         query.setLong(0, targetProjectNode.getID());
         List resourceAssignments = broker.list(query);

         OpResource resource = null;
         OpProjectNodeAssignment projectNodeAssignment = null;
         int accesibleResourcesSize = 0;
         while (accessibleResources.hasNext()) {
            resource = (OpResource) accessibleResources.next();
            if (!resourceAssignments.contains(new Long(resource.getID()))) {
               projectNodeAssignment = new OpProjectNodeAssignment();
               projectNodeAssignment.setResource(resource);
               projectNodeAssignment.setProjectNode(targetProjectNode);
               broker.makePersistent(projectNodeAssignment);
               OpProjectAdministrationService.insertContributorPermission(broker, targetProjectNode, resource);
            }
            accesibleResourcesSize++;
         }

         // check if we should return a warning
         if (accesibleResourcesSize < resourceIds.size()) {
            logger.warn("Not all the selected resources could be assigned");
            reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_ASSIGNMENT_WARNING));
            reply.setArgument("warning", Boolean.TRUE);
         }
      }

      t.commit();
      broker.close();

      return reply;

   }

   private void collectResources(OpObject object, Set resourceIds) {
      if (object.getPrototype().getName().equals(OpResourcePool.RESOURCE_POOL)) {
         //add all its sub-resources
         OpResourcePool pool = (OpResourcePool) object;
         Set resources = pool.getResources();
         for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
            OpObject entity = (OpObject) iterator.next();
            collectResources(entity, resourceIds);
         }
      }
      else {
         resourceIds.add(new Long(object.getID()));
      }
   }

   public XMessage moveResourceNode(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;
      /*get needed args from request */
      List resourceIds = (List) request.getArgument(RESOURCE_IDS);
      String poolId = (String) request.getArgument(POOL_ID);

      XMessage reply = new XMessage();

      if (resourceIds == null || resourceIds.isEmpty() || poolId == null) {
         return reply; //NOP
      }

      OpBroker broker = session.newBroker();
      OpTransaction tx = broker.newTransaction();

      OpResourcePool pool = (OpResourcePool) broker.getObject(poolId);

      //check manager access for selected pool
      if (!session.checkAccessLevel(broker, pool.getID(), OpPermission.MANAGER)) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
      }
      else {
         for (Iterator it = resourceIds.iterator(); it.hasNext();) {
            String resourceId = (String) it.next();
            OpResource resource = (OpResource) broker.getObject(resourceId);

            // Check manager access for resource's pool
            if (!session.checkAccessLevel(broker, resource.getPool().getID(), OpPermission.MANAGER)) {
               reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
               continue;
            }

            //update the resource's pool
            resource.setPool(pool);

            //update personnel costs if inherit pool rate is true and hourly rate changes
            if (resource.getInheritPoolRate() && resource.getHourlyRate() != pool.getHourlyRate()) {
               resource.setHourlyRate(pool.getHourlyRate());
               updatePersonnelCosts(broker, resource);
            }
            broker.updateObject(resource);
         }
      }

      tx.commit();
      broker.close();
      return reply;
   }

   public XMessage movePoolNode(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;
      /*get needed args from request */
      List poolIds = (List) request.getArgument(POOL_IDS);
      String superPoolId = (String) request.getArgument(SUPER_POOL_ID);

      XMessage reply = new XMessage();
      if (poolIds == null || poolIds.isEmpty() || superPoolId == null) {
         return reply; //NOP
      }

      OpBroker broker = session.newBroker();
      OpTransaction tx = broker.newTransaction();

      for (Iterator it = poolIds.iterator(); it.hasNext();) {
         String poolId = (String) it.next();

         OpResourcePool pool = (OpResourcePool) broker.getObject(poolId);
         OpResourcePool superPool = (OpResourcePool) broker.getObject(superPoolId);

         // Check manager access for selected pool's super pool and selected super pool
         if (!session.checkAccessLevel(broker, pool.getSuperPool().getID(), OpPermission.MANAGER) ||
              !session.checkAccessLevel(broker, superPool.getID(), OpPermission.MANAGER)) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
            continue;
         }

         //update the pool's superpool
         pool.setSuperPool(superPool);
         broker.updateObject(pool);
      }

      tx.commit();
      broker.close();
      return reply;
   }


   /**
    * Retrieves the children for the given pool id and returns them as a list argument on the reply.
    * It will also filter and enable/disable the rows if the required request params are present.
    *
    * @param s
    * @param request
    * @return
    */
   public XMessage expandResourcePool(XSession s, XMessage request) {
      XMessage reply = new XMessage();
      OpProjectSession session = (OpProjectSession) s;

      String targetPoolLocator = (String) request.getArgument(POOL_LOCATOR);
      Integer outline = (Integer) (request.getArgument(OUTLINE_LEVEL));
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      List poolSelector = (List) request.getArgument(POOL_SELECTOR);
      List resourceSelector = (List) request.getArgument(RESOURCE_SELECTOR);
      List resultList = null;
      if (targetPoolLocator != null && outline != null) {
         OpLocator locator = OpLocator.parseLocator((String) (targetPoolLocator));
         //get filter
         List filteredIds = (List) request.getArgument(FILTERED_OUT_IDS);
         OpResourceDataSetFactory.retrieveResourceDataSet(session, dataSet, poolSelector, resourceSelector, locator.getID(), outline.intValue() + 1, filteredIds);
         //enable/disable rows
         enableRows(request, dataSet);
         //set result
         resultList = new ArrayList();
         for (int i = 0; i < dataSet.getChildCount(); i++) {
            resultList.add(dataSet.getChild(i));
         }
         reply.setArgument(OpProjectConstants.CHILDREN, resultList);
      }

      return reply;
   }

   private void enableRows(XMessage request, XComponent dataSet) {

      //disable pools/resources
      Boolean enablePools = (Boolean) request.getArgument(ENABLE_POOLS);
      Boolean enableResources = (Boolean) request.getArgument(ENABLE_RESOURCES);
      if (enablePools != null && enableResources != null) {
         boolean showPools = enablePools.booleanValue();
         boolean showResources = enableResources.booleanValue();
         OpResourceDataSetFactory.enableResourcesSet(dataSet, showResources, showPools);
      }
   }

   // Helper methods

   public static OpResourcePool findRootPool(OpBroker broker) {
      OpQuery query = broker.newQuery("select pool from OpResourcePool as pool where pool.Name = ?");
      query.setString(0, OpResourcePool.ROOT_RESOURCE_POOL_NAME);
      Iterator result = broker.list(query).iterator();
      if (result.hasNext()) {
         return (OpResourcePool) result.next();
      }
      else {
         return null;
      }
   }

   public static OpResourcePool createRootPool(OpProjectSession session, OpBroker broker) {
      // Insert root resource pool
      OpTransaction t = broker.newTransaction();
      OpResourcePool rootPool = new OpResourcePool();
      rootPool.setName(OpResourcePool.ROOT_RESOURCE_POOL_NAME);
      rootPool.setDescription(OpResourcePool.ROOT_RESOURCE_POOL_DESCRIPTION);
      broker.makePersistent(rootPool);
      OpPermissionSetFactory.addSystemObjectPermissions(session, broker, rootPool);
      t.commit();
      return rootPool;
   }

   /**
    * Returns the number of activity assignments for the given <code>resource</code> and </code>projectPlan</code>
    *
    * @param broker      <code>OpBroker</code> used for performing business operations.
    * @param resource    <code>OpResource</code> entity
    * @param projectPlan <code>OpProjectPlan</code> entity
    * @return <code>int</code> representing the number of activity assignments of the resource into the given project
    */
   public static int getActivityAssignmentsCount(OpBroker broker, OpResource resource, OpProjectPlan projectPlan) {
      String countQuery = "select count(assignment) from OpResource resource inner join resource.ActivityAssignments assignment where resource.ID = ? and assignment.ProjectPlan.ID = ?";
      OpQuery query = broker.newQuery(countQuery);
      query.setLong(0, resource.getID());
      query.setLong(1, projectPlan.getID());
      Integer counter = (Integer) broker.iterate(query).next();
      return counter.intValue();
   }

   /**
    * Removes contributor permission for each project in <code>projectNodeIds</code> and given <code>user</code>
    *
    * @param broker         <code>OpBroker</code> used for performing business operations.
    * @param user           <code>OpSubject</code> user entity
    * @param projectNodeIds <code>Set(String)</code> of project node ids
    */
   private void deleteContributorPermission(OpBroker broker, OpSubject user, Set projectNodeIds) {
      if (user == null || projectNodeIds.isEmpty()) {
         return;
      }
      OpQuery query = broker.newQuery("select permission from OpPermission as permission where permission.Subject.ID = :userId and permission.Object.ID in (:projectNodeIds) and permission.SystemManaged = :systemManaged");
      query.setLong("userId", user.getID());
      query.setCollection("projectNodeIds", projectNodeIds);
      query.setBoolean("systemManaged", true);
      Iterator permissionsToDelete = broker.list(query).iterator();

      //list of resource ids for which the user is responsible
      query = broker.newQuery("select resource.ID from OpUser user inner join user.Resources resource where user.ID = :userId ");
      query.setLong("userId", user.getID());
      List resourceIds = broker.list(query);

      //remove permission if there are no project node assignments for resources with the same responsible user
      while (permissionsToDelete.hasNext()) {
         OpPermission permission = (OpPermission) permissionsToDelete.next();
         if (!resourceIds.isEmpty()) {
            query = broker.newQuery("select count(assignment) from OpProjectNodeAssignment as assignment where assignment.Resource.ID in (:resourceIds) and assignment.ProjectNode.ID  = :projectId");
            query.setLong("projectId", permission.getObject().getID());
            query.setCollection("resourceIds", resourceIds);
            Integer counter = (Integer) broker.iterate(query).next();
            if (counter.intValue() == 1) {
               broker.deleteObject(permission);
            }
         }
      }
   }

}
