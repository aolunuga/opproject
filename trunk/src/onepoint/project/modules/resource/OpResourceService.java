/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAssignmentVersion;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanValidator;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpContact;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpSubject;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpResourceService extends onepoint.project.OpProjectService {

   public static final String SERVICE_NAME = "ResourceService";
   protected static final XLog logger = XLogFactory.getServerLogger(OpResourceService.class);

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

   private static final String INTERNAL_RATE = "InternalRate";
   private static final String EXTERNAL_RATE = "ExternalRate";
   private static final String RESOURCES_DATA_SET = "ResourcesDataSet";
   public final static String CHECK_INHERIT = "checkInherit";

   private final static String NEW_HOURLY_RATE = "newHourlyRate";
   private final static String NEW_EXTERNAL_RATE = "newExternalRate";
   private final static String ORIGINAL_HOURLY_RATE = "originalHourlyRate";
   private final static String ORIGINAL_EXTERNAL_RATE = "originalExternalRate";

   public XMessage insertResource(OpProjectSession session, XMessage request) {
      logger.debug("OpResourceService.insertResource()");
      HashMap resource_data = (HashMap) (request.getArgument(RESOURCE_DATA));
      // *** More error handling needed (check mandatory fields)

      XMessage reply = new XMessage();

      OpResource resource = new OpResource();
      resource.setName((String) (resource_data.get(OpResource.NAME)));
      resource.setDescription((String) (resource_data.get(OpResource.DESCRIPTION)));
      resource.setAvailable((Double) resource_data.get(OpResource.AVAILABLE));
      resource.setHourlyRate((Double) (resource_data.get(OpResource.HOURLY_RATE)));
      resource.setExternalRate((Double) (resource_data.get(OpResource.EXTERNAL_RATE)));
      resource.setInheritPoolRate((Boolean) (resource_data.get(OpResource.INHERIT_POOL_RATE)));

      OpBroker broker = session.newBroker();

      // Set pool for this resource and set hourly rate of pool if inherit is true
      String pool_id_string = (String) (resource_data.get("PoolID"));
      logger.debug("***INTO-POOL: " + pool_id_string);
      OpResourcePool pool;
      if (pool_id_string != null) {
         pool = (OpResourcePool) (broker.getObject(pool_id_string));
         if (pool == null) {
            logger.warn("Superpool is null. Resource will be added to root pool.");
            pool = findRootPool(broker);
         }
      }
      else {
         logger.warn("Given superpool locator is null. Resource will be added to root pool.");
         pool = findRootPool(broker);
      }
      resource.setPool(pool);

      if (resource.getInheritPoolRate()) {
         resource.setHourlyRate(resource.getPool().getHourlyRate());
         resource.setExternalRate(resource.getPool().getExternalRate());
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
      // check valid external rate
      if (resource.getExternalRate() < 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.EXTERNAL_RATE_NOT_VALID));
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
      if (OpEnvironmentManager.isMultiUser()) {
         String user_id_string = (String) (resource_data.get("UserID"));
         if ((user_id_string != null) && (user_id_string.length() > 0)) {
            OpUser user = (OpUser) (broker.getObject(user_id_string));
            resource.setUser(user);
         }
         else { // set user to login user
            resource.setUser(session.user(broker));
         }
      }
      else {
         OpUser user = (OpUser) (broker.getObject(OpUser.class, session.getAdministratorID()));
         resource.setUser(user);
      }

      OpTransaction t = broker.newTransaction();

      broker.makePersistent(resource);

      // Add projects assignments
      ArrayList assigned_projects = (ArrayList) (resource_data.get(PROJECTS));
      if ((assigned_projects != null) && (assigned_projects.size() > 0)) {
         logger.debug("ASSIGNED " + assigned_projects);
         OpProjectNode projectNode;
         OpProjectNodeAssignment projectNodeAssignment;
         // TODO: Check for read-access to project node (important if we add Web Services access)
         for (Object assigned_project : assigned_projects) {
            projectNode = (OpProjectNode) (broker.getObject((String) assigned_project));
            projectNodeAssignment = new OpProjectNodeAssignment();
            projectNodeAssignment.setResource(resource);
            projectNodeAssignment.setProjectNode(projectNode);
            broker.makePersistent(projectNodeAssignment);
            OpProjectAdministrationService.insertContributorPermission(broker, projectNode, resource);
         }
      }

      reply = insertExtendedHourlyRatesForResource(session, broker, resource_data, resource);
      if (reply.getError() != null) {
         broker.close();
         return reply;
      }

      XComponent permission_set = (XComponent) resource_data.get(OpPermissionSetFactory.PERMISSION_SET);
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, resource, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }

      t.commit();

      broker.close();
      logger.debug("/OpResourceService.insertResource()");
      return reply;
   }

   //this method is used in order to add hourly rates periods functionality in the closed module
   protected XMessage insertExtendedHourlyRatesForResource(OpProjectSession session, OpBroker broker,
        HashMap resource_data, OpResource resource) {
      return new XMessage();
   }

   public XMessage importUser(OpProjectSession session, XMessage request) {
      logger.debug("OpResourceService.importUser()");
      Map<String, Object> resourceData = (Map<String, Object>) request.getArgument(RESOURCE_DATA);

      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();

      OpResource resource = new OpResource();
      // get associated user
      OpUser user = null;
      String userId = (String) resourceData.get("UserID");
      if ((userId != null) && (userId.length() > 0)) {
         user = (OpUser) (broker.getObject(userId));
         resource.setUser(user);
      }
      else {
         logger.warn("UserID not set, could not import user as resource");
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.USER_ID_NOT_SPECIFIED));
         return reply;
      }
      //set resource name
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

      //set resource description (from user contact)
      OpContact contact = user.getContact();
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
      double availability = (Double) resourceData.get(OpResource.AVAILABLE);
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
      if ((Double) (resourceData.get(OpResource.HOURLY_RATE)) < 0) {
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
         return reply;
      }
      // check valid external rate
      if ((Double) (resourceData.get(OpResource.EXTERNAL_RATE)) < 0) {
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.EXTERNAL_RATE_NOT_VALID));
         return reply;
      }

      resource.setHourlyRate((Double) (resourceData.get(OpResource.HOURLY_RATE)));
      resource.setExternalRate((Double) (resourceData.get(OpResource.EXTERNAL_RATE)));
      resource.setInheritPoolRate((Boolean) (resourceData.get(OpResource.INHERIT_POOL_RATE)));

      // Check if resource is to be created inside a pool
      OpResourcePool pool = null;
      String poolId = (String) resourceData.get("PoolID");
      if (poolId != null) {
         logger.debug("***INTO-POOL: " + poolId);
         pool = (OpResourcePool) broker.getObject(poolId);
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

      OpTransaction t = broker.newTransaction();
      //persist entity
      broker.makePersistent(resource);
      //persist permissions
      OpPermissionSetFactory.addSystemObjectPermissions(session, broker, resource);

      // Add projects assignments
      List<String> assignedProjects = (List<String>) resourceData.get(PROJECTS);
      if ((assignedProjects != null) && (assignedProjects.size() > 0)) {
         logger.debug("ASSIGNED " + assignedProjects);
         for (String assignedProjectId : assignedProjects) {
            OpProjectNode projectNode = (OpProjectNode) (broker.getObject(assignedProjectId));
            OpProjectNodeAssignment projectNodeAssignment = new OpProjectNodeAssignment();
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

   public XMessage updateResource(OpProjectSession session, XMessage request) {
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
      double availableValue = (Double) (resource_data.get(OpResource.AVAILABLE));
      boolean availibityChanged = (oldAvailableValue != availableValue);
      resource.setAvailable(availableValue);

      double hourlyRate = (Double) (resource_data.get(OpResource.HOURLY_RATE));
      resource.setHourlyRate(hourlyRate);
      double externalRate = (Double) (resource_data.get(OpResource.EXTERNAL_RATE));
      resource.setExternalRate(externalRate);
      resource.setInheritPoolRate((Boolean) (resource_data.get(OpResource.INHERIT_POOL_RATE)));
      // Overwrite hourly rate w/pool rate if inherit is false
      if (resource.getInheritPoolRate()) {
         hourlyRate = resource.getPool().getHourlyRate();
         resource.setHourlyRate(resource.getPool().getHourlyRate());
         externalRate = resource.getPool().getExternalRate();
         resource.setExternalRate(resource.getPool().getExternalRate());
      }

      double maxAvailability = Double.parseDouble(OpSettings.get(OpSettings.RESOURCE_MAX_AVAILABYLITY));
      // check valid availability range [0..maxAvailability]
      double availability = (Double) resource_data.get(OpResource.AVAILABLE);
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
      // check valid external rate
      if (resource.getExternalRate() < 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.EXTERNAL_RATE_NOT_VALID));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      // *** TODO: Currently a work-around (should use choice instead of ID)
      String user_locator = (String) (resource_data.get("UserID"));
      if ((user_locator != null) && (user_locator.length() > 0)) {
         OpUser user = (OpUser) (broker.getObject(user_locator));
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
                  Number counter = (Number) broker.iterate(query).next();
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
                     for (Object permission : permissions) {
                        broker.deleteObject((OpPermission) permission);
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

      //update resource hourlyRatesPeriods
      reply = updateHourlyRatesPeriods(session, broker, resource_data, resource);
      if (reply.getError() != null) {
         broker.close();
         return reply;
      }

      //update permissions
      XComponent permission_set = (XComponent) resource_data.get(OpPermissionSetFactory.PERMISSION_SET);
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, resource, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }

      //update availability
      List projectPlans = new ArrayList();
      if (availibityChanged) {
         projectPlans = updateAvailability(broker, resource);
      }

      //update the working versions for the project plans
      for (Object projectPlan : projectPlans) {
         OpProjectPlan plan = (OpProjectPlan) projectPlan;
         new OpProjectPlanValidator(plan).validateProjectPlanWorkingVersion(broker, null, false);
      }

      XCalendar xCalendar = session.getCalendar();
      //update personnel & proceeds costs
      updatePersonnelCostsForWorkingVersion(broker, xCalendar, resource);
      updateActualCosts(broker, resource);

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

      return broker.iterate(query);
   }

   /**
    * Return a not null message if the resource has any assignments.
    *
    * @param s
    * @param request
    * @return null if resource has no assignments
    */
   public XMessage hasAssignments(OpProjectSession s, XMessage request) {
      String id_string = (String) (request.getArgument(RESOURCE_ID));
      OpBroker broker = s.newBroker();
      OpResource resource = (OpResource) (broker.getObject(id_string));
      boolean hasAssignments = false;
      if (resource.getAssignmentVersions().size() != 0) {
         hasAssignments = true;
      }
      if (resource.getActivityAssignments().size() != 0) {
         hasAssignments = true;
      }
      XMessage xMessage = new XMessage();
      //if inherit mode was changed check if the rates changes
      if (request.getArgument(CHECK_INHERIT) != null) {
         Double newHourlyRate = (Double) request.getArgument(NEW_HOURLY_RATE);
         Double newExternalRate = (Double) request.getArgument(NEW_EXTERNAL_RATE);
         Double originalHourlyRate = (Double) request.getArgument(ORIGINAL_HOURLY_RATE);
         Double originalExternalRate = (Double) request.getArgument(ORIGINAL_EXTERNAL_RATE);
         hasAssignments = hasAssignments && (newHourlyRate.doubleValue() != originalHourlyRate.doubleValue() ||
              newExternalRate.doubleValue() != originalExternalRate.doubleValue());
      }

      xMessage.setArgument(HAS_ASSIGNMENTS, hasAssignments);
      return xMessage;
   }

   /**
    * Return a not null message if the pool has any resources with any assignments.
    *
    * @param s
    * @param request
    * @return null if pool has no resources with assignments
    */
   public XMessage hasResourceAssignments(OpProjectSession s, XMessage request) {
      String id_string = (String) (request.getArgument(POOL_ID));
      boolean hasAssignments = false;
      OpBroker broker = s.newBroker();
      OpResourcePool pool = (OpResourcePool) broker.getObject(id_string);
      for (Object o : pool.getResources()) {
         OpResource resource = (OpResource) o;
         if (resource.getAssignmentVersions().size() != 0) {
            hasAssignments = true;
            break;
         }
         if (resource.getActivityAssignments().size() != 0) {
            hasAssignments = true;
         }
      }
      XMessage xMessage = new XMessage();
      xMessage.setArgument(HAS_ASSIGNMENTS, hasAssignments);
      return xMessage;
   }

   /**
    * Updates the resource availibility for the checked out projects.
    *
    * @param broker   a <code>OpBroker</code> used for performing business operations.
    * @param resource a <code>OpResource</code> representing the resource being edited.
    * @return List of project plans that had their assignments updated
    */
   private List updateAvailability(OpBroker broker, OpResource resource) {
      Iterator it = getAssignmentsForWorkingVersions(broker, resource.getID());
      List projectPlans = new ArrayList();
      while (it.hasNext()) {
         OpAssignmentVersion assignmentVersion = (OpAssignmentVersion) it.next();
         if (assignmentVersion.getAssigned() > resource.getAvailable()) {
            assignmentVersion.setAssigned(resource.getAvailable());
            broker.updateObject(assignmentVersion);
            OpProjectPlan plan = assignmentVersion.getPlanVersion().getProjectPlan();
            projectPlans.add(plan);
         }
      }
      return projectPlans;
   }

   /**
    * Updates the base personnel costs of the assignments of the activities that are checked out at the moment when the
    * resource is updated.
    *
    * @param broker   a <code>OpBroker</code> used for performing business operations.
    * @param calendar - the <code>XCalendar</code> needed to get the working days out of an interval of time
    * @param resource a <code>OpResource</code> representing the resource that has been updated.
    */
   private void updatePersonnelCostsForWorkingVersion(OpBroker broker, XCalendar calendar, OpResource resource) {

      Iterator it = getAssignmentsForWorkingVersions(broker, resource.getID());

      OpActivityVersion activityVersion;
      List<List> ratesList;
      List<Double> internalRatesList;
      List<Double> externalRatesList;
      Double internalSum = 0d;
      Double externalSum = 0d;
      List<OpAssignmentVersion> updatedAssignments = new ArrayList<OpAssignmentVersion>();
      OpAssignmentVersion workingAssignmentVersion;
      List<Date> startEndList;
      double workHoursPerDay;
      List<Date> workingDays;

      while (it.hasNext()) {
         workingAssignmentVersion = (OpAssignmentVersion) it.next();
         activityVersion = workingAssignmentVersion.getActivityVersion();
         workHoursPerDay = calendar.getWorkHoursPerDay();
         //for task activities calculation of start end dates is different
         if (activityVersion.getType() == OpActivityVersion.TASK) {
            startEndList = activityVersion.getStartEndDateByType();
         }
         else {
            startEndList = activityVersion.getStartEndDateByType();
         }
         if (startEndList != null) {
            workingDays = calendar.getWorkingDaysFromInterval(startEndList.get(OpActivityVersion.START_DATE_LIST_INDEX),
                 startEndList.get(OpActivityVersion.END_DATE_LIST_INDEX));
            ratesList = resource.getRatesForListOfDays(workingDays);
            internalRatesList = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
            externalRatesList = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);

            //for task activities workHoursPerDay = activity effort/activity working days
            if (activityVersion.getType() == OpActivityVersion.TASK) {
               if (workingDays.size() > 0) {
                  workHoursPerDay = activityVersion.getBaseEffort() / (double) workingDays.size();
               }
               else {
                  workHoursPerDay = 0d;
               }
            }

            for (Double internalRate : internalRatesList) {
               internalSum += internalRate * workHoursPerDay * workingAssignmentVersion.getAssigned() / 100;
            }
            for (Double externalRate : externalRatesList) {
               externalSum += externalRate * workHoursPerDay * workingAssignmentVersion.getAssigned() / 100;
            }
         }

         if (workingAssignmentVersion.getBaseCosts() != internalSum || workingAssignmentVersion.getBaseProceeds() != externalSum) {
            workingAssignmentVersion.setBaseCosts(internalSum);
            workingAssignmentVersion.setBaseProceeds(externalSum);
            broker.updateObject(workingAssignmentVersion);
            updatedAssignments.add(workingAssignmentVersion);
         }
         internalSum = 0d;
         externalSum = 0d;
      }

      OpActivityDataSetFactory.updateActivityVersionPersonnelCosts(broker, updatedAssignments);
   }

   /**
    * Updates the actual costs of the assignments and the activities that are checked in at the moment when the
    * resource is updated.
    *
    * @param broker   a <code>OpBroker</code> used for performing business operations.
    * @param resource a <code>OpResource</code> representing the resource that has been updated.
    */
   private void updateActualCosts(OpBroker broker, OpResource resource) {
      List<OpAssignment> updatedAssignments = new ArrayList<OpAssignment>();
      List<Double> ratesList = new ArrayList<Double>();

      for (OpAssignment assignment : resource.getActivityAssignments()) {
         Double internalSum = 0d;
         Double externalSum = 0d;
         Double newActualCosts = 0d;
         Double newActualProceeds = 0d;

         for (OpWorkRecord workRecord : assignment.getWorkRecords()) {
            //get the new rate of the resource for the work record's day
            ratesList = resource.getRatesForDay(workRecord.getWorkSlip().getDate());
            newActualCosts = workRecord.getActualEffort() * ratesList.get(OpResource.INTERNAL_RATE_INDEX);
            newActualProceeds = workRecord.getActualEffort() * ratesList.get(OpResource.EXTERNAL_RATE_INDEX);

            //if the costs are different - update the workslip
            if (workRecord.getPersonnelCosts() != newActualCosts || workRecord.getActualProceeds() != newActualProceeds) {
               workRecord.setPersonnelCosts(newActualCosts);
               workRecord.setActualProceeds(newActualProceeds);
               broker.updateObject(assignment);
            }
            internalSum += workRecord.getPersonnelCosts();
            externalSum += workRecord.getActualProceeds();
         }
         if (assignment.getActualCosts() != internalSum || assignment.getActualProceeds() != externalSum) {
            assignment.setActualCosts(internalSum);
            assignment.setActualProceeds(externalSum);
            broker.updateObject(assignment);
            updatedAssignments.add(assignment);
         }
      }

      OpActivityDataSetFactory.updateActivityActualCosts(broker, updatedAssignments);
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
      OpQuery query;
      //the reply message
      XMessage reply = new XMessage();
      logger.debug("ASSIGNED " + assigned_projects);
      // Query stored project node assignments
      query = broker.newQuery("select assignment.ProjectNode.ID from OpProjectNodeAssignment as assignment where assignment.Resource.ID = ?");
      query.setLong(0, resource.getID());

      Iterator result = broker.iterate(query);
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
         result = broker.iterate(query);
         while (result.hasNext()) {
            OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) result.next();
            OpProjectPlan projectPlan = assignment.getProjectNode().getPlan();
            XMessage checkUsageReply = checkResourceUsageOnProjectPlan(session, broker, resource, projectPlan);
            if (checkUsageReply.getError() != null) {
               reply.setError(checkUsageReply.getError());
               return reply;
            }
            broker.deleteObject(assignment);
         }
      }
      return reply;
   }

   //method which allows insertion of advanced hourly rate periods functionality
   protected XMessage updateHourlyRatesPeriods(OpProjectSession session, OpBroker broker, Map resource_data, OpResource resource) {
      //the reply message
      XMessage reply = new XMessage();
      return reply;
   }

   public XMessage deleteResources(OpProjectSession session, XMessage request) {

      XMessage reply = new XMessage();

      List<String> ids = (List<String>) request.getArgument(RESOURCE_IDS);
      if (ids.size() == 0) {
         logger.debug("No resources to delete");
         return reply;
      }

      logger.debug("OpResourceService.deleteResources(): resource_ids = " + ids);

      OpBroker broker = session.newBroker();
      List<Long> resourceIds = new ArrayList<Long>();
      for (String resourceId : ids) {
         resourceIds.add(OpLocator.parseLocator(resourceId).getID());
      }

      OpQuery query = broker.newQuery("select resource.Pool.ID from OpResource as resource where resource.ID in (:resourceIds)");
      query.setCollection("resourceIds", resourceIds);
      List<Long> poolIds = broker.list(query);
      Set accessiblePoolIds = session.accessibleIds(broker, poolIds, OpPermission.MANAGER);
      if (accessiblePoolIds.size() == 0) {
         logger.warn("Manager access to pool " + poolIds + " denied");
         reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
         broker.close();
         return reply;
      }

      //check if any of the resource are assigned on any activities
      List<OpResource> resources = new ArrayList<OpResource>();
      for (long resourceId : resourceIds) {
         OpResource resource = (OpResource) broker.getObject(OpResource.class, resourceId);
         XMessage checkUsageReply = OpResourceService.checkResourceUsageOnProjectPlan(session, broker, resource, null);
         if (checkUsageReply.getError() != null) {
            broker.close();
            reply.setError(checkUsageReply.getError());
            return reply;
         }
         resources.add(resource);
      }

      OpTransaction t = broker.newTransaction();
      for (OpResource resource : resources) {
         // get all project node assignment ids for the resource
         query = broker.newQuery("select assignment.ProjectNode.ID from OpProjectNodeAssignment as assignment where assignment.Resource.ID = ?");
         query.setLong(0, resource.getID());
         Iterator result = broker.iterate(query);
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
      broker.close();
      logger.debug("/OpResourceService.deleteResources()");
      return null;
   }

   public XMessage insertPool(OpProjectSession session, XMessage request) {
      logger.debug("OpResourceService.insertPool()");
      HashMap pool_data = (HashMap) (request.getArgument(POOL_DATA));

      OpResourcePool pool = new OpResourcePool();
      pool.setName((String) (pool_data.get(OpResourcePool.NAME)));
      pool.setDescription((String) (pool_data.get(OpResourcePool.DESCRIPTION)));
      pool.setHourlyRate((Double) (pool_data.get(OpResourcePool.HOURLY_RATE)));
      pool.setExternalRate((Double) (pool_data.get(OpResourcePool.EXTERNAL_RATE)));

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
      // check valid external rate
      if (pool.getExternalRate() < 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.EXTERNAL_RATE_NOT_VALID));
         return reply;
      }

      OpBroker broker = session.newBroker();

      // Set pool for this resource and set hourly rate of pool if inherit is true
      String superPoolLocator = (String) (pool_data.get("SuperPoolID"));
      logger.debug("***INTO-SUPER-POOL: " + superPoolLocator);
      OpResourcePool superPool;
      if (superPoolLocator != null) {
         superPool = (OpResourcePool) (broker.getObject(superPoolLocator));
         if (superPool == null) {
            logger.warn("Given SuperPool is null. Pool will be inserted into root pool.");
            superPool = findRootPool(broker);
         }
      }
      else {
         superPool = findRootPool(broker);
         logger.warn("SuperPool locator is null. Pool will be inserted into root pool.");
      }
      pool.setSuperPool(superPool);

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
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, pool, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }


      t.commit();
      logger.debug("/OpResourceService.insertPool()");
      broker.close();
      return reply;
   }

   public XMessage updatePool(OpProjectSession session, XMessage request) {
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

      double hourlyRate = (Double) (pool_data.get(OpResourcePool.HOURLY_RATE));
      boolean poolRateChanged = false;
      if (hourlyRate != pool.getHourlyRate()) {
         poolRateChanged = true;
         pool.setHourlyRate(hourlyRate);
      }

      double externalRate = (Double) (pool_data.get(OpResourcePool.EXTERNAL_RATE));
      boolean poolExternalRateChanged = false;
      if (externalRate != pool.getExternalRate()) {
         poolExternalRateChanged = true;
         pool.setExternalRate(externalRate);
      }

      // check valid hourly rate
      if (pool.getHourlyRate() < 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
         broker.close();
         return reply;
      }
      // check valid external rate
      if (pool.getExternalRate() < 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.EXTERNAL_RATE_NOT_VALID));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      broker.updateObject(pool);

      XComponent permission_set = (XComponent) pool_data.get(OpPermissionSetFactory.PERMISSION_SET);
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, pool, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }

      // Update all inherited hourly-rate fields & external-rate fields of pool resources if pool rates have changed
      // TODO: Probably optimize by using a query for "InheritPoolRate = true"
      if (poolRateChanged || poolExternalRateChanged) {
         Iterator resources = pool.getResources().iterator();
         OpResource resource = null;
         XCalendar xCalendar = session.getCalendar();
         while (resources.hasNext()) {
            resource = (OpResource) resources.next();
            if (resource.getInheritPoolRate()) {
               resource.setHourlyRate(pool.getHourlyRate());
               resource.setExternalRate(pool.getExternalRate());
               updatePersonnelCostsForWorkingVersion(broker, xCalendar, resource);
               updateActualCosts(broker, resource);
               broker.updateObject(resource);
            }
         }
      }

      t.commit();
      logger.debug("/OpResourceService.updateResource()");
      broker.close();
      return reply;
   }

   public XMessage deletePools(OpProjectSession session, XMessage request) {

      // TODO: Maybe add force-flag (like there was before falsely for delete-group)
      // (Deny deletion of not-empty pools if force flag deleteIfNotEmpty is not set)
      ArrayList id_strings = (ArrayList) (request.getArgument(POOL_IDS));
      logger.debug("OpResourceService.deletePools(): pool_ids = " + id_strings);

      OpBroker broker = session.newBroker();

      List poolIds = new ArrayList();
      XMessage reply = new XMessage();

      for (Object id_string : id_strings) {
         poolIds.add(OpLocator.parseLocator((String) id_string).getID());
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
         for (Object resource1 : resources) {
            OpResource resource = (OpResource) resource1;
            if (!resource.getActivityAssignments().isEmpty() || !resource.getAssignmentVersions().isEmpty() ||
                 !resource.getResponsibleActivities().isEmpty() || !resource.getResponsibleActivityVersions().isEmpty()) {
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

   public XMessage assignToProject(OpProjectSession session, XMessage request) {
      // TODO: Check read-access to project and manage-permissions of resources (bulk-check IDs)

      OpBroker broker = session.newBroker();
      XMessage reply = new XMessage();

      logger.debug("OpResourceService.assignToProject()");

      ArrayList resource_id_strings = (ArrayList) (request.getArgument(RESOURCE_IDS));
      List projectIds = (List) (request.getArgument(PROJECT_IDS));

      // TODO: Error handling for assign-to-project is missing

      // *** Retrieve target project
      OpTransaction t = broker.newTransaction();
      for (Object projectId1 : projectIds) {
         String projectId = (String) projectId1;
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
         Set<Long> resourceIds = new HashSet<Long>();
         for (Object resource_id_string : resource_id_strings) {
            String resourceID = (String) resource_id_string;
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

   private void collectResources(OpObject object, Set<Long> resourceIds) {
      if (OpTypeManager.getPrototypeForObject(object).getName().equals(OpResourcePool.RESOURCE_POOL)) {
         //add all its sub-resources
         OpResourcePool pool = (OpResourcePool) object;
         Set resources = pool.getResources();
         for (Object resource : resources) {
            OpObject entity = (OpObject) resource;
            collectResources(entity, resourceIds);
         }
      }
      else {
         resourceIds.add(object.getID());
      }
   }

   public XMessage moveResourceNode(OpProjectSession session, XMessage request) {
      //get needed args from request
      List resourceIds = (List) request.getArgument(RESOURCE_IDS);
      String poolId = (String) request.getArgument(POOL_ID);
      XCalendar xCalendar = session.getCalendar();

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
         for (Object resourceId1 : resourceIds) {
            String resourceId = (String) resourceId1;
            OpResource resource = (OpResource) broker.getObject(resourceId);

            // Check manager access for resource's pool
            if (!session.checkAccessLevel(broker, resource.getPool().getID(), OpPermission.MANAGER)) {
               reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
               continue;
            }

            //update the resource's pool
            resource.setPool(pool);

            //update personnel costs if inherit pool rate is true and hourly rate changes or external rate changes
            if (resource.getInheritPoolRate() &&
                 (resource.getHourlyRate() != pool.getHourlyRate() || resource.getExternalRate() != pool.getExternalRate())) {
               resource.setHourlyRate(pool.getHourlyRate());
               resource.setExternalRate(pool.getExternalRate());
               updatePersonnelCostsForWorkingVersion(broker, xCalendar, resource);
               updateActualCosts(broker, resource);
            }
            broker.updateObject(resource);
         }
      }

      tx.commit();
      broker.close();
      return reply;
   }

   public XMessage movePoolNode(OpProjectSession session, XMessage request) {
      //get needed args from request
      List poolIds = (List) request.getArgument(POOL_IDS);
      String superPoolId = (String) request.getArgument(SUPER_POOL_ID);

      XMessage reply = new XMessage();
      if (poolIds == null || poolIds.isEmpty() || superPoolId == null) {
         return reply; //NOP
      }

      OpBroker broker = session.newBroker();
      OpTransaction tx = broker.newTransaction();

      for (Object poolId1 : poolIds) {
         String poolId = (String) poolId1;

         OpResourcePool pool = (OpResourcePool) broker.getObject(poolId);
         OpResourcePool superPool = (OpResourcePool) broker.getObject(superPoolId);

         if (checkPoolAssignmentsForLoops(pool, superPool)) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.LOOP_ASSIGNMENT_ERROR));
            continue;
         }

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
    * Checks the group assignments for loops including the newSuperGroups
    *
    * @param pool      the <code>OpResourcePool</code> for which the check is performed
    * @param superPool <code>OpResourcePool</code> representing the assigned super pool the <code>pool</code>
    * @return true if a loop was found, false otherwise
    */
   private boolean checkPoolAssignmentsForLoops(OpResourcePool pool, OpResourcePool superPool) {
      if (pool.getID() == superPool.getID()) {
         return true;
      }
      if (superPool.getSuperPool() != null) {
         if (checkPoolAssignmentsForLoops(pool, superPool.getSuperPool())) {
            return true;
         }
      }
      return false;
   }

   /**
    * Retrieves the children for the given pool id and returns them as a list argument on the reply.
    * It will also filter and enable/disable the rows if the required request params are present.
    *
    * @param session
    * @param request
    * @return
    */
   public XMessage expandResourcePool(OpProjectSession session, XMessage request) {
      XMessage reply = new XMessage();

      String targetPoolLocator = (String) request.getArgument(POOL_LOCATOR);
      Integer outline = (Integer) (request.getArgument(OUTLINE_LEVEL));
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      Map poolSelector = (Map) request.getArgument(POOL_SELECTOR);
      Map resourceSelector = (Map) request.getArgument(RESOURCE_SELECTOR);
      List<XComponent> resultList;
      if (targetPoolLocator != null && outline != null) {
         OpLocator locator = OpLocator.parseLocator(targetPoolLocator);
         //get filter
         List filteredIds = (List) request.getArgument(FILTERED_OUT_IDS);
         OpResourceDataSetFactory.retrieveResourceDataSet(session, dataSet, poolSelector, resourceSelector, locator.getID(), outline.intValue() + 1, filteredIds);
         //enable/disable rows
         enableRows(request, dataSet);
         //set result
         resultList = new ArrayList<XComponent>();
         for (int i = 0; i < dataSet.getChildCount(); i++) {
            resultList.add((XComponent) dataSet.getChild(i));
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
         OpResourceDataSetFactory.enableResourcesSet(dataSet, enableResources, enablePools);
      }
   }

   // Helper methods

   public static OpResourcePool findRootPool(OpBroker broker) {
      return findPool(broker, OpResourcePool.ROOT_RESOURCE_POOL_NAME);
   }

   /**
    * Find a pool resource by name
    *
    * @param broker a <code>OpBroker</code> instance
    * @param name   the name of the resource pool
    * @return an instance of <code>OpResourcePool</code> or <code>null</code> if not found
    */
   public static OpResourcePool findPool(OpBroker broker, String name) {
      OpQuery query = broker.newQuery("select pool from OpResourcePool as pool where pool.Name = ?");
      query.setString(0, name);
      Iterator result = broker.iterate(query);
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
    * Checks if given resource has any usages on the given project plan.
    * Usages mean: activity assignments, activity version assignments, responsible for activity or responsible for activity versions.
    * If the project plan is <code>null</code>, then all the project plans are searched.
    *
    * @param session     a <code>OpProjectSession</code> representing a server session.
    * @param broker      a <code>OpBroker</code> used for persistence operations.
    * @param resource    a <code>OpResource</code> representing a project resource.
    * @param projectPlan a <code>OpProjectPlan</code> representing a project plan. Can be <code>null</code>.
    * @return a <code>XMessage</code> which contains an error if the resource is used
    *         in the project plan, or is empty if not.
    */
   public static XMessage checkResourceUsageOnProjectPlan(OpProjectSession session, OpBroker broker, OpResource resource, OpProjectPlan projectPlan) {
      XMessage reply = new XMessage();
      if (countResourceActivityAssignments(broker, resource, projectPlan) > 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.ACTIVITY_ASSIGNMENTS_EXIST_ERROR));
         return reply;
      }
      if (countResourceActivityAssignmentVersions(broker, resource, projectPlan) > 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.ACTIVITY_ASSIGNMENT_VERSIONS_EXIST_ERROR));
         return reply;
      }
      if (countResourceResponsibleActivities(broker, resource, projectPlan) > 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.RESPONSIBLE_ACTIVITIES_EXIST_ERROR));
         return reply;
      }
      if (countResourceResponsibleActivityVersions(broker, resource, projectPlan) > 0) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.RESPONSIBLE_ACTIVITY_VERSIONS_EXIST_ERROR));
         return reply;
      }
      return reply;
   }

   /**
    * Returns the number of responsible activity versions for the given <code>resource</code> and </code>projectPlan</code>
    *
    * @param broker      <code>OpBroker</code> used for performing business operations.
    * @param resource    <code>OpResource</code> entity
    * @param projectPlan <code>OpProjectPlan</code> entity. If <code>null</code> all project plans are taken into account.
    * @return <code>int</code> representing the number of responsible activity versions of the resource into the given project
    */
   private static long countResourceResponsibleActivityVersions(OpBroker broker, OpResource resource, OpProjectPlan projectPlan) {
      StringBuffer countQuery = new StringBuffer("select count(activityVersion) from OpResource resource inner join resource.ResponsibleActivityVersions activityVersion where resource.ID = ? ");
      if (projectPlan != null) {
         countQuery.append(" and activityVersion.PlanVersion.ProjectPlan.ID = ");
         countQuery.append(projectPlan.getID());
      }
      OpQuery query = broker.newQuery(countQuery.toString());
      query.setLong(0, resource.getID());
      Number counterResponsibleVersions = (Number) broker.iterate(query).next();
      return counterResponsibleVersions.longValue();
   }

   /**
    * Returns the number of responsible activities for the given <code>resource</code> and </code>projectPlan</code>
    *
    * @param broker      <code>OpBroker</code> used for performing business operations.
    * @param resource    <code>OpResource</code> entity
    * @param projectPlan <code>OpProjectPlan</code> entity. If <code>null</code> all project plans are taken into account.
    * @return <code>int</code> representing the number of responsible activities of the resource into the given project
    */
   private static long countResourceResponsibleActivities(OpBroker broker, OpResource resource, OpProjectPlan projectPlan) {
      StringBuffer countQuery = new StringBuffer("select count(activity) from OpResource resource inner join resource.ResponsibleActivities activity where resource.ID = ? ");
      if (projectPlan != null) {
         countQuery.append(" and activity.ProjectPlan.ID = ");
         countQuery.append(projectPlan.getID());
      }
      OpQuery query = broker.newQuery(countQuery.toString());
      query.setLong(0, resource.getID());
      Number counterResponsible = (Number) broker.iterate(query).next();
      return counterResponsible.longValue();
   }

   /**
    * Returns the number of activity assignment versions for the given <code>resource</code> and </code>projectPlan</code>
    *
    * @param broker      <code>OpBroker</code> used for performing business operations.
    * @param resource    <code>OpResource</code> entity
    * @param projectPlan <code>OpProjectPlan</code> entity. If <code>null</code> all project plans are taken into account.
    * @return <code>int</code> representing the number of activity assignment versions of the resource into the given project
    */
   private static long countResourceActivityAssignmentVersions(OpBroker broker, OpResource resource, OpProjectPlan projectPlan) {
      StringBuffer countQuery = new StringBuffer("select count(assignmentVersion) from OpResource resource inner join resource.AssignmentVersions assignmentVersion where resource.ID = ? ");
      if (projectPlan != null) {
         countQuery.append(" and assignmentVersion.PlanVersion.ProjectPlan.ID =");
         countQuery.append(projectPlan.getID());
      }
      OpQuery query = broker.newQuery(countQuery.toString());
      query.setLong(0, resource.getID());
      Number counterAssignmentVersions = (Number) broker.iterate(query).next();
      return counterAssignmentVersions.longValue();
   }

   /**
    * Returns the number of activity assignments for the given <code>resource</code> and </code>projectPlan</code>
    *
    * @param broker      <code>OpBroker</code> used for performing business operations.
    * @param resource    <code>OpResource</code> entity
    * @param projectPlan <code>OpProjectPlan</code> entity. If <code>null</code> all project plans are taken into account.
    * @return <code>int</code> representing the number of activity assignments of the resource into the given project
    */
   private static long countResourceActivityAssignments(OpBroker broker, OpResource resource, OpProjectPlan projectPlan) {
      StringBuffer countQuery = new StringBuffer("select count(assignment) from OpResource resource inner join resource.ActivityAssignments assignment where resource.ID = ? ");
      if (projectPlan != null) {
         countQuery.append(" and assignment.ProjectPlan.ID = ");
         countQuery.append(projectPlan.getID());
      }
      OpQuery query = broker.newQuery(countQuery.toString());
      query.setLong(0, resource.getID());
      Number counterAssignments = (Number) broker.iterate(query).next();
      return counterAssignments.longValue();
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
      Iterator permissionsToDelete = broker.iterate(query);

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
            Number counter = (Number) broker.iterate(query).next();
            if (counter.intValue() == 1) {
               broker.deleteObject(permission);
            }
         }
      }
   }

   /**
    * Adds new rows to the project resource table. This method add only rows representing resources,
    * NOT hourly rates periods.
    *
    * @param s       -
    * @param request - contains the data set of actual resources
    * @return - the new data set with the added rows
    */
   public XMessage addDescriptionToResources(OpProjectSession s, XMessage request) {
      XComponent resourcesDataSet = (XComponent) (request.getArgument(RESOURCES_DATA_SET));
      OpBroker broker = s.newBroker();
      OpResource resource;
      XMessage xMessage = new XMessage();

      for (int i = 0; i < resourcesDataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) resourcesDataSet.getChild(i);
         if (dataRow.getChildCount() == 0) {
            resource = (OpResource) (broker.getObject(dataRow.getStringValue()));

            //0 - resource name
            XComponent dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setStringValue(resource.getName());
            dataCell.setEnabled(true);
            dataRow.addChild(dataCell);

            //1 - resource description
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setStringValue(resource.getDescription());
            dataCell.setEnabled(true);
            dataRow.addChild(dataCell);

            //2 - adjust rates - false
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setBooleanValue(false);
            dataCell.setEnabled(true);
            dataRow.addChild(dataCell);

            //3 - hourly rate/project/resource - empty
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setEnabled(false);
            dataRow.addChild(dataCell);

            //4 - external rate/project/resource - empty
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setEnabled(false);
            dataRow.addChild(dataCell);

            //5 - start date - empty
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataRow.addChild(dataCell);

            //6 - end date - empty
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataRow.addChild(dataCell);

            //7 - internal rate/interval - empty
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataRow.addChild(dataCell);

            //8 - external rate/interval - empty
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataRow.addChild(dataCell);
         }
      }

      xMessage.setArgument(RESOURCES_DATA_SET, resourcesDataSet);
      return xMessage;
   }

   public XMessage getResourceRates(OpProjectSession s, XMessage request) {
      String id_string = (String) (request.getArgument(RESOURCE_ID));
      OpBroker broker = s.newBroker();
      OpResource resource = (OpResource) (broker.getObject(id_string));
      XMessage xMessage = new XMessage();

      List<Double> ratesList = resource.getRatesForDay(new java.sql.Date(System.currentTimeMillis()));

      xMessage.setArgument(INTERNAL_RATE, ratesList.get(OpResource.INTERNAL_RATE_INDEX));
      xMessage.setArgument(EXTERNAL_RATE, ratesList.get(OpResource.EXTERNAL_RATE_INDEX));
      return xMessage;
   }
}
