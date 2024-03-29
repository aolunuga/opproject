/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
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
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpContact;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpSubject;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceException;

public class OpResourceService extends onepoint.project.OpProjectService {

	public static final String SERVICE_NAME = "ResourceService";
   protected static final XLog logger = XLogFactory.getLogger(OpResourceService.class);

   private OpResourceServiceImpl serviceImpl = new OpResourceServiceImpl();

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

   private final static String APPLY_PERMISSIONS_RECURSIVELY = "ApplyPermissionsRecursively";

   private final static String OUTLINE_LEVEL = "outlineLevel";
   private final static String POOL_LOCATOR = "source_pool_locator";
   private final static String POOL_SELECTOR = "poolColumnsSelector";
   private final static String RESOURCE_SELECTOR = "resourceColumnsSelector";
   private final static String FILTERED_OUT_IDS = "FilteredOutIds";
   private static final String ENABLE_POOLS = "EnablePools";
   private static final String ENABLE_RESOURCES = "EnableResources";
   private static final String NOT_SELECTABLE_IDS = "NotSelectableIds";
   private static final String ADD_ARCHIVED_RESOURCES = "addArchivedResources";

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
      String resourceName = (String) (resource_data.get(OpResource.NAME));
      resourceName = resourceName != null ? resourceName.trim() : null;
      resource.setName(resourceName);
      resource.setDescription((String) (resource_data.get(OpResource.DESCRIPTION)));
      resource.updateAvailable(((Double) resource_data.get(OpResource.AVAILABLE)).doubleValue());
      resource.updateInternalHourlyRate(((Double) (resource_data.get(OpResource.HOURLY_RATE))).doubleValue());
      resource.updateExternalHourlyRate(((Double) (resource_data.get(OpResource.EXTERNAL_RATE))).doubleValue());
      resource.setInheritPoolRate((Boolean) (resource_data.get(OpResource.INHERIT_POOL_RATE)));
      resource.setArchived((Boolean) (resource_data.get(OpResource.ARCHIVED)));

      OpBroker broker = session.newBroker();
      OpTransaction transaction = null;

      try {
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
            resource.updateInternalHourlyRate(resource.getPool().getHourlyRate());
            resource.updateExternalHourlyRate(resource.getPool().getExternalRate());
         }

         // Check manager access for pool
         if (!session.checkAccessLevel(broker, resource.getPool().getId(), OpPermission.MANAGER)) {
            logger.warn("ERROR: Insert access to pool denied; ID = " + resource.getPool().getId());
            reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
            return reply;
         }

         // check mandatory input fields
         if (resource.getName() == null || resource.getName().length() == 0) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED));
            return reply;
         }
         //check if resource name contains invalid char %
         if (resource.getName().indexOf("%") != -1) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_VALID));
            return reply;
         }

         double maxAvailability = Double.parseDouble(OpSettingsService.getService().getStringValue(session, OpSettings.RESOURCE_MAX_AVAILABYLITY));
         // check valid availability range [0..maxAvailability]
         int availability = ((Double) resource_data.get(OpResource.AVAILABLE)).intValue();
         if (availability < 0 || availability > maxAvailability) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.AVAILABILITY_NOT_VALID));
            return reply;
         }

         // check valid hourly rate
         if (resource.getHourlyRate() < 0) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
            return reply;
         }
         // check valid external rate
         if (resource.getExternalRate() < 0) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.EXTERNAL_RATE_NOT_VALID));
            return reply;
         }

         // check if resource name is already used
         OpQuery query = broker.newQuery("select resource.id from OpResource as resource where resource.Name = :resourceName");
         query.setString("resourceName", resource.getName());
         Iterator resourceIds = broker.iterate(query);
         if (resourceIds.hasNext()) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_UNIQUE));
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

         //validate resource before persisting
         resource.validate();
         try {
            insertResourceAdditional(session, broker, request, resource_data, resource);
         } 
         catch (XServiceException exc) {
            exc.append(reply);
            return reply;
         }

         transaction = broker.newTransaction();

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
            return reply;
         }

         //update skill rating
         reply = insertSkillRatings(session, broker, resource_data, resource);
         if (reply.getError() != null) {
            return reply;
         }

         XComponent permission_set = (XComponent) resource_data.get(OpPermissionDataSetFactory.PERMISSION_SET);
         XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, resource, permission_set);
         if (result != null) {
            reply.setError(result);
            return reply;
         }

         transaction.commit();
         logger.debug("/OpResourceService.insertResource()");
         return reply;

      }
      catch (OpEntityException e) {
         throw new XServiceException(session.newError(ERROR_MAP, mapEntityError(e)));
      }
      finally {
         finalizeSession(transaction, broker);
      }
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
      OpTransaction transaction = null;
      try {
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
            reply.setError(session.newError(ERROR_MAP, OpResourceError.USER_ID_NOT_SPECIFIED));
            return reply;
         }
         //set resource name
         String resourceName = user.getName();
         resourceName = resourceName != null ? resourceName.trim() : null;
         // check if resource name is already used
         OpQuery query = broker.newQuery("select resource.id from OpResource as resource where resource.Name = :resourceName");
         query.setString("resourceName", resourceName);
         Iterator resourceIds = broker.iterate(query);
         if (resourceIds.hasNext()) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_UNIQUE));
            return reply;
         }
         resource.setName(resourceName);

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
         double maxAvailable = Double.parseDouble(OpSettingsService.getService().getStringValue(session, OpSettings.RESOURCE_MAX_AVAILABYLITY));
         if (availability < 0 || availability > maxAvailable) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.AVAILABILITY_NOT_VALID));
            return reply;
         }
         else {
            resource.updateAvailable(availability);
         }

         // check valid hourly rate
         if ((Double) (resourceData.get(OpResource.HOURLY_RATE)) < 0) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
            return reply;
         }
         // check valid external rate
         if ((Double) (resourceData.get(OpResource.EXTERNAL_RATE)) < 0) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.EXTERNAL_RATE_NOT_VALID));
            return reply;
         }

         resource.updateInternalHourlyRate(((Double) (resourceData.get(OpResource.HOURLY_RATE))).doubleValue());
         resource.updateExternalHourlyRate(((Double) (resourceData.get(OpResource.EXTERNAL_RATE))).doubleValue());
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
         if (!session.checkAccessLevel(broker, pool.getId(), OpPermission.MANAGER)) {
            logger.warn("ERROR: Import access to pool denied; ID = " + pool.getId());
            reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
            return reply;
         }
         //set up the pool for the resource
         resource.setPool(pool);

         //validate resource
         resource.validate();

         transaction = broker.newTransaction();
         //persist entity
         broker.makePersistent(resource);
         //persist permissions
         OpPermissionDataSetFactory.addSystemObjectPermissions(session, broker, resource);

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
         transaction.commit();
         logger.debug("/OpResourceService.importUser()");
         return reply;
      }
      catch (OpEntityException e) {
         throw new XServiceException(session.newError(ERROR_MAP, mapEntityError(e)));
      }
      finally {
         finalizeSession(transaction, broker);
      }
   }

   //<FIXME author="Mihai Costin" description="This should be done by a general mechanism, like for XServiceException">
   private int mapEntityError(OpEntityException e) {
      int entityErrorCode = e.getErrorCode();
      if (entityErrorCode == OpResource.INVALID_USER_LEVEL) {
         return OpResourceError.INVALID_USER_LEVEL_ERROR;
      }
      return -1; //should be a default error
   }
   //</FIXME>


   public XMessage updateResource(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(RESOURCE_ID));
      logger.debug("OpResourceService.updateResource(): id = " + id_string);
      HashMap resource_data = (HashMap) (request.getArgument(RESOURCE_DATA));

      XMessage reply = new XMessage();

      OpBroker broker = session.newBroker();
      OpTransaction transaction = null;
      try {
         OpResource resource = (OpResource) (broker.getObject(id_string));
         if (resource == null) {
            logger.warn("ERROR: Could not find object with ID " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NOT_FOUND));
            return reply;
         }

         // Check manager access
         if (!session.checkAccessLevel(broker, resource.getId(), OpPermission.MANAGER)) {
            logger.warn("ERROR: Udpate access to resource denied; ID = " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
            return reply;
         }

         String resourceName = (String) (resource_data.get(OpResource.NAME));
         resourceName = resourceName != null ? resourceName.trim() : null;
         
         // check mandatory input fields
         if (resourceName == null || resourceName.length() == 0) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED));
            return reply;
         }
         //check if resource name contains invalid char %
         if (resourceName.indexOf("%") != -1) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_VALID));
            return reply;
         }
         // check if resource name is already used
         OpQuery query = broker.newQuery("select resource from OpResource as resource where resource.Name = :resourceName");
         query.setString("resourceName", resourceName);
         Iterator resources = broker.iterate(query);
         while (resources.hasNext()) {
            OpResource other = (OpResource) resources.next();
            if (other.getId() != resource.getId()) {
               reply.setError(session.newError(ERROR_MAP, OpResourceError.RESOURCE_NAME_NOT_UNIQUE));
               return reply;
            }
         }

         resource.setName(resourceName);
         resource.setDescription((String) (resource_data.get(OpResource.DESCRIPTION)));

         double oldAvailableValue = resource.getAvailable();
         double availableValue = (Double) (resource_data.get(OpResource.AVAILABLE));
         boolean availibityChanged = (oldAvailableValue != availableValue);
         resource.updateAvailable(availableValue);

         double hourlyRate = ((Double) (resource_data.get(OpResource.HOURLY_RATE))).doubleValue();
         resource.updateInternalHourlyRate(hourlyRate);
         double externalRate = ((Double) (resource_data.get(OpResource.EXTERNAL_RATE))).doubleValue();
         resource.updateExternalHourlyRate(externalRate);
         resource.setInheritPoolRate((Boolean) (resource_data.get(OpResource.INHERIT_POOL_RATE)));
         resource.setArchived((Boolean) (resource_data.get(OpResource.ARCHIVED)));
         // Overwrite hourly rate w/pool rate if inherit is false
         if (resource.getInheritPoolRate()) {
            hourlyRate = resource.getPool().getHourlyRate();
            resource.updateInternalHourlyRate(resource.getPool().getHourlyRate());
            externalRate = resource.getPool().getExternalRate();
            resource.updateExternalHourlyRate(resource.getPool().getExternalRate());
         }

         double maxAvailability = Double.parseDouble(OpSettingsService.getService().getStringValue(session, OpSettings.RESOURCE_MAX_AVAILABYLITY));
         // check valid availability range [0..maxAvailability]
         double availability = (Double) resource_data.get(OpResource.AVAILABLE);
         if (availability < 0 || availability > maxAvailability) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.AVAILABILITY_NOT_VALID));
            return reply;
         }

         // check valid hourly rate
         if (resource.getHourlyRate() < 0) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
            return reply;
         }
         // check valid external rate
         if (resource.getExternalRate() < 0) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.EXTERNAL_RATE_NOT_VALID));
            return reply;
         }

         try {
            updateResourceAdditional(session, broker, request, resource_data, resource);
         } 
         catch (XServiceException exc) {
            exc.append(reply);
            return reply;
         }

         transaction = broker.newTransaction();

         reply = this.updateResourceResponsibleUser(resource_data, broker, session, resource);
         if (reply != null && reply.getError() != null) {
            return reply;
         }

         resource.validate();

         //update resource
         broker.updateObject(resource);

         //update resource hourlyRatesPeriods
         reply = updateHourlyRatesPeriods(session, broker, resource_data, resource);
         if (reply.getError() != null) {
            return reply;
         }

         //update resource assignments
         ArrayList assigned_projects = (ArrayList) (resource_data.get(PROJECTS));
         reply = updateProjectAssignments(session, assigned_projects, broker, resource);
         if (reply.getError() != null) {
            return reply;
         }

         //update skill rating
         reply = updateSkillRatings(session, broker, resource_data, resource);
         if (reply.getError() != null) {
            return reply;
         }

         //update permissions
         XComponent permission_set = (XComponent) resource_data.get(OpPermissionDataSetFactory.PERMISSION_SET);
         XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, resource, permission_set);
         if (result != null) {
            reply.setError(result);
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
            OpProjectPlanValidator.getInstance().validateProjectPlanWorkingVersion(session, broker, plan, null, false);
         }

         OpProjectCalendar xCalendar = session.getCalendar();
         //update personnel & proceeds costs
         updatePersonnelCostsForWorkingVersion(broker, xCalendar, resource);
         updateActualCosts(broker, resource);

         //update work months (remaining cost values)
         for (OpAssignment assignment : resource.getActivityAssignments()) {
            OpActivityDataSetFactory.updateWorkMonths(session, broker, assignment);
         }

         transaction.commit();

         logger.debug("/OpResourceService.updateResource()");
      }
      catch (OpEntityException e) {
         throw new XServiceException(session.newError(ERROR_MAP, mapEntityError(e)));
      }
      finally {
         finalizeSession(transaction, broker);
      }
      return reply;
   }

   /**
    * Updates the responsible user of an existing resource.
    *
    * @param resourceData a <code>Map</code> of requrest parameters and values.
    * @param broker       a <code>OpBroker</code> used for business operations.
    * @param resource     a <code>OpResource</code> an existing resource.
    */
   private XMessage updateResourceResponsibleUser(Map resourceData, OpBroker broker,
        OpProjectSession session, OpResource resource) {
      String userLocator = (String) (resourceData.get("UserID"));
      //no user parameter on request data
      if (userLocator == null || userLocator.trim().length() == 0) {
         resource.setUser(null);
         broker.updateObject(resource);
         return null;
      }

      if ((userLocator != null) && (userLocator.length() > 0)) {
         OpUser user = (OpUser) (broker.getObject(userLocator));
         Iterator assignments = resource.getProjectNodeAssignments().iterator();
         //user has no responsible user -> for all the resource's project assignments add a contributor permission entry
         if (resource.getUser() == null) {
            //set up the new responsible user for the resource
            resource.setUser(user);
            broker.updateObject(resource);
            while (assignments.hasNext()) {
               OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) assignments.next();
               OpProjectAdministrationService.insertContributorPermission(broker, assignment.getProjectNode(), resource);
            }
            return null;
         }
         OpUser oldResponsibleUser = resource.getUser();
         if (oldResponsibleUser.getId() != user.getId()) { //responsible user has been changed
            //list of resource ids for which the resource's old user is responsible
            OpQuery query = broker.newQuery("select resource.id from OpUser user inner join user.Resources resource where user.id = :userId ");
            query.setLong("userId", oldResponsibleUser.getId());
            List resourceIds = broker.list(query);

            //set up the new responsible user for the resource
            resource.setUser(user);

            while (assignments.hasNext()) {
               OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) assignments.next();
               query = broker.newQuery("select count(assignment) from OpProjectNodeAssignment as assignment where assignment.Resource.id in (:resourceIds) and assignment.ProjectNode.id  = :projectId");
               query.setLong("projectId", assignment.getProjectNode().getId());
               query.setCollection("resourceIds", resourceIds);
               Number counter = (Number) broker.iterate(query).next();
               // at least one project assignment exist for the resources the old user was responsible
               if (counter.intValue() > 1) {
                  OpProjectAdministrationService.insertContributorPermission(broker, assignment.getProjectNode(), resource);
               }
               else {//update the permision subject for the persisted assignment projectNode
                  query = broker.newQuery("select permission from OpPermission permission where permission.Object.id = :projectId " +
                       "and permission.Subject.id = :subjectId and permission.AccessLevel = :accessLevel and permission.SystemManaged = :systemManaged");
                  query.setLong("projectId", assignment.getProjectNode().getId());
                  query.setLong("subjectId", oldResponsibleUser.getId());
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
      return null;
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
      queryString.append("where assignment.ActivityVersion.PlanVersion.VersionNumber = ? and resource.id = ?");

      OpQuery query = broker.newQuery(queryString.toString());
      query.setInteger(0, OpProjectPlan.WORKING_VERSION_NUMBER);
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
   public XMessage hasAssignments(OpProjectSession session,  XMessage request) {
      OpBroker broker = session.newBroker();
      try {
         return hasAssignments(session, broker, request);
      }
      finally {
         broker.close();
      }
   }
   /**
    * Return a not null message if the resource has any assignments.
    *
    * @param s
    * @param request
    * @return null if resource has no assignments
    */
   public XMessage hasAssignments(OpProjectSession s, OpBroker broker, XMessage request) {
      String id_string = (String) (request.getArgument(RESOURCE_ID));
      OpResource resource = (OpResource) (broker.getObject(id_string));
      boolean hasAssignments = false;
      if (OpResourceDataSetFactory.hasAssignmentVersions(broker, resource)) {
         hasAssignments = true;
      }
      if (OpResourceDataSetFactory.hasActivityAssignments(broker, resource)) {
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
      OpBroker broker = s.newBroker();
      try {
         return hasResourceAssignments(s, broker, request);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Return a not null message if the pool has any resources with any assignments.
    *
    * @param s
    * @param request
    * @return null if pool has no resources with assignments
    */
   public XMessage hasResourceAssignments(OpProjectSession s, OpBroker broker, XMessage request) {
      String id_string = (String) (request.getArgument(POOL_ID));
      boolean hasAssignments = false;
      OpResourcePool pool = (OpResourcePool) broker.getObject(id_string);
      for (Object o : pool.getResources()) {
         OpResource resource = (OpResource) o;
         if (OpResourceDataSetFactory.hasAssignmentVersions(broker, resource)) {
            hasAssignments = true;
            break;
         }
         if (OpResourceDataSetFactory.hasActivityAssignments(broker, resource)) {
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
      Iterator it = getAssignmentsForWorkingVersions(broker, resource.getId());
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
    * @param calendar - the <code>OpProjectCalendar</code> needed to get the working days out of an interval of time
    * @param resource a <code>OpResource</code> representing the resource that has been updated.
    */
   private void updatePersonnelCostsForWorkingVersion(OpBroker broker, OpProjectCalendar calendar, OpResource resource) {

      Iterator it = getAssignmentsForWorkingVersions(broker, resource.getId());

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
            // FIXME: honor calendars!
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

      Set<OpAssignment> assignments = resource.getActivityAssignments();
      OpProjectAdministrationService.updateActualValuesForAssignments(broker, assignments);
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
      query = broker.newQuery("select assignment.ProjectNode.id from OpProjectNodeAssignment as assignment where assignment.Resource.id = ?");
      query.setLong(0, resource.getId());

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

         query = broker.newQuery("select assignment from OpProjectNodeAssignment as assignment where assignment.Resource.id = :resourceId and assignment.ProjectNode.id in (:projectNodeIds)");
         query.setLong("resourceId", resource.getId());
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

   protected XMessage updateSkillRatings(OpProjectSession session, OpBroker broker, Map resource_data, OpResource resource) {
      //the reply message
      XMessage reply = new XMessage();
      return reply;
   }

   protected XMessage insertSkillRatings(OpProjectSession session, OpBroker broker, Map resource_data, OpResource resource) {
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
      try {
         OpTransaction t = broker.newTransaction();
         List<Long> resourceIds = new ArrayList<Long>();
         for (String resourceId : ids) {
            resourceIds.add(OpLocator.parseLocator(resourceId).getID());
         }

         OpQuery query = broker.newQuery("select resource.Pool.id from OpResource as resource where resource.id in (:resourceIds)");
         query.setCollection("resourceIds", resourceIds);
         List<Long> poolIds = broker.list(query);
         Set accessiblePoolIds = session.accessibleIds(broker, poolIds, OpPermission.MANAGER);
         if (accessiblePoolIds.size() == 0) {
            logger.warn("Manager access to pool " + poolIds + " denied");
            reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
            return reply;
         }

         //check if any of the resource are assigned on any activities
         List<OpResource> resources = new ArrayList<OpResource>();
         for (long resourceId : resourceIds) {
            OpResource resource = (OpResource) broker.getObject(OpResource.class, resourceId);
            XMessage checkUsageReply = OpResourceService.checkResourceUsageOnProjectPlan(session, broker, resource, null);
            if (checkUsageReply.getError() != null) {
               reply.setError(checkUsageReply.getError());
               return reply;
            }
            resources.add(resource);
         }

         for (OpResource resource : resources) {
            // get all project node assignment ids for the resource
            query = broker.newQuery("select assignment.ProjectNode.id from OpProjectNodeAssignment as assignment where assignment.Resource.id = ?");
            query.setLong(0, resource.getId());
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

      }
      finally {
         broker.close();
      }
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
      try {
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
         if (!session.checkAccessLevel(broker, pool.getSuperPool().getId(), OpPermission.MANAGER)) {
            logger.warn("ERROR: Insert access to pool denied; ID = " + pool.getSuperPool().getId());
            reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
            return reply;
         }

         // check if pool name is already used
         OpQuery query = broker.newQuery("select pool.id from OpResourcePool as pool where pool.Name = :poolName");
         query.setString("poolName", pool.getName());
         Iterator poolIds = broker.iterate(query);
         if (poolIds.hasNext()) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.POOL_NAME_NOT_UNIQUE));
            return reply;
         }

         try {
            insertPoolAdditional(session, broker, request, pool_data, pool);
         } 
         catch (XServiceException exc) {
            exc.append(reply);
            return reply;
         }
         OpTransaction t = broker.newTransaction();
         broker.makePersistent(pool);

         XComponent permission_set = (XComponent) pool_data.get(OpPermissionDataSetFactory.PERMISSION_SET);
         XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, pool, permission_set);
         if (result != null) {
            broker.close();
            return reply;
         }


         t.commit();
         logger.debug("/OpResourceService.insertPool()");
         return reply;
      }
      finally {
         broker.close();
      }
   }

   public XMessage updatePool(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(POOL_ID));
      logger.debug("OpResourceService.updatePool(): id = " + id_string);
      HashMap pool_data = (HashMap) (request.getArgument(POOL_DATA));

      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();
      try {
         OpResourcePool pool = (OpResourcePool) (broker.getObject(id_string));
         if (pool == null) {
            logger.warn("ERROR: Could not find object with ID " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpResourceError.POOL_NOT_FOUND));
            return reply;
         }

         // Check manager access
         if (!session.checkAccessLevel(broker, pool.getId(), OpPermission.MANAGER)) {
            logger.warn("ERROR: Udpate access to pool denied; ID = " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
            return reply;
         }

         String poolName = (String) (pool_data.get(OpResourcePool.NAME));

         // check mandatory input fields
         if (poolName == null || poolName.length() == 0) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.POOL_NAME_NOT_SPECIFIED));
            return reply;
         }

         // check if pool name is already used
         OpQuery query = broker.newQuery("select pool from OpResourcePool as pool where pool.Name = :poolName");
         query.setString("poolName", poolName);
         Iterator pools = broker.iterate(query);
         while (pools.hasNext()) {
            OpResourcePool other = (OpResourcePool) pools.next();
            if (other.getId() != pool.getId()) {
               reply.setError(session.newError(ERROR_MAP, OpResourceError.POOL_NAME_NOT_UNIQUE));
               return reply;
            }
         }

         //name and description for root pool should not be editable
         if (findRootPool(broker).getId() != pool.getId()) {
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
            return reply;
         }
         // check valid external rate
         if (pool.getExternalRate() < 0) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.EXTERNAL_RATE_NOT_VALID));
            return reply;
         }
         try {
            updatePoolAdditional(session, broker, request, pool_data, pool);
         } 
         catch (XServiceException exc) {
            exc.append(reply);
            return reply;
         }

         OpTransaction t = broker.newTransaction();

         broker.updateObject(pool);

         XComponent permission_set = (XComponent) pool_data.get(OpPermissionDataSetFactory.PERMISSION_SET);
         // Store permissions on pool's sub-elements if the ApplyPermissionsRecursively was checked
         if ((Boolean) pool_data.get(APPLY_PERMISSIONS_RECURSIVELY)) {
            reply = storePermissionSetRecursively(broker, session, pool, permission_set);
            if (reply.getError() != null) {
               return reply;
            }
         }
         else {
            XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, pool, permission_set);
            if (result != null) {
               reply.setError(result);
               return reply;
            }
         }

         // Update all inherited hourly-rate fields & external-rate fields of pool resources if pool rates have changed
         // TODO: Probably optimize by using a query for "InheritPoolRate = true"
         if (poolRateChanged || poolExternalRateChanged) {
            Iterator resources = pool.getResources().iterator();
            OpResource resource = null;
            OpProjectCalendar xCalendar = session.getCalendar();
            while (resources.hasNext()) {
               resource = (OpResource) resources.next();
               if (resource.getInheritPoolRate()) {
                  resource.updateInternalHourlyRate(pool.getHourlyRate());
                  resource.updateExternalHourlyRate(pool.getExternalRate());
                  updatePersonnelCostsForWorkingVersion(broker, xCalendar, resource);
                  updateActualCosts(broker, resource);
                  broker.updateObject(resource);
               }
            }
         }

         t.commit();
         logger.debug("/OpResourceService.updateResource()");
         return reply;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Saves the permissions from the permissionSet on the <code>OpResourcePool</code> object passed as parameter and on
    * all it's subpools and resources.
    *
    * @param broker        a <code>OpBroker</code> used for performing business operations.
    * @param session       a <code>OpProjectSession</code> representing the current server session
    * @param pool        the <code>OpResourcePool</code> on which the permissions will be set and whose subpools and resources
    *                       will also receive the same permissions
    * @param permissionSet a <code>XComponent(DATA_SET)</code> the client permissions set
    * @return a <code>XMessage</code> object containing an error if at some point the permissions could no be set on a
    *         pool or resource or an empty <code>XMessage</code> if there were no errors.
    */
   private XMessage storePermissionSetRecursively(OpBroker broker, OpProjectSession session, OpResourcePool pool, XComponent permissionSet) {
      XMessage reply = new XMessage();

      // Check manager access on the current element
      if (!session.checkAccessLevel(broker, pool.getId(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Udpate access to pool denied; ID = " + pool.locator());
         reply.setError(session.newError(ERROR_MAP, OpResourceError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, pool, permissionSet);
      if (result != null) {
         reply.setError(result);
         return reply;
      }

      for (OpResourcePool subPool : pool.getSubPools()) {
         reply = storePermissionSetRecursively(broker, session, subPool, permissionSet);
         if (reply.getError() != null) {
            return reply;
         }
      }

      for (OpResource resource : pool.getResources()) {
         result = OpPermissionDataSetFactory.storePermissionSet(broker, session, resource, permissionSet);
         if (result != null) {
            reply.setError(result);
            return reply;
         }
      }

      return reply;
   }

   public XMessage deletePools(OpProjectSession session, XMessage request) {

      // TODO: Maybe add force-flag (like there was before falsely for delete-group)
      // (Deny deletion of not-empty pools if force flag deleteIfNotEmpty is not set)
      ArrayList id_strings = (ArrayList) (request.getArgument(POOL_IDS));
      logger.debug("OpResourceService.deletePools(): pool_ids = " + id_strings);

      OpBroker broker = session.newBroker();
      try {
         List poolIds = new ArrayList();
         XMessage reply = new XMessage();

         for (Object id_string : id_strings) {
            poolIds.add(OpLocator.parseLocator((String) id_string).getID());
         }
         OpQuery query = broker.newQuery("select pool.SuperPool.id from OpResourcePool as pool where pool.id in (:poolIds)");
         query.setCollection("poolIds", poolIds);
         List superPoolIds = broker.list(query);
         Set accessibleSuperPoolIds = session.accessibleIds(broker, superPoolIds, OpPermission.MANAGER);

         if (accessibleSuperPoolIds.size() == 0) {
            logger.warn("Manager access to super pools " + superPoolIds + " denied");
            reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
            return reply;
         }

         OpTransaction t = broker.newTransaction();
         /* --- Not yet support by Hibernate (delete query against joined-subclass)
      query = broker.newQuery("delete from OpResourcePool where OpResourcePool.id in (:poolIds) and OpResourcePool.SuperPool.id in (:accessibleSuperPoolIds)");
      broker.execute(query);
          */
         query = broker.newQuery("select pool from OpResourcePool as pool where pool.id in (:poolIds) and pool.SuperPool.id in (:accessibleSuperPoolIds)");
         query.setCollection("poolIds", poolIds);
         query.setCollection("accessibleSuperPoolIds", accessibleSuperPoolIds);
         Iterator result = broker.iterate(query);
         while (result.hasNext()) {
            OpResourcePool pool = (OpResourcePool) result.next();
            Set resources = pool.getResources();
            for (Object resource1 : resources) {
               OpResource resource = (OpResource) resource1;
               if (OpResourceDataSetFactory.hasActivityAssignments(broker, resource) ||
                     OpResourceDataSetFactory.hasAssignmentVersions(broker, resource) ||
                     OpResourceDataSetFactory.hasResponsibleActivities(broker, resource) ||
                     OpResourceDataSetFactory.hasResponsibleActivityVersions(broker, resource)) {
                  logger.warn("Resource " + resource.getName() + " is used in project assignments");
                  reply.setError(session.newError(ERROR_MAP, OpResourceError.DELETE_POOL_RESOURCE_ASSIGNMENTS_DENIED));
                  t.rollback();
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
      }
      finally {
         broker.close();
      }
      return null;
   }

   public XMessage assignToProject(OpProjectSession session, XMessage request) {
      // TODO: Check read-access to project and manage-permissions of resources (bulk-check IDs)

      OpBroker broker = session.newBroker();
      try {
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

            if (!session.checkAccessLevel(broker, targetProjectNode.getId(), OpPermission.OBSERVER)) {
               logger.warn("ERROR: Could not access object with ID " + projectIds + " as observer");
               continue;
            }

            // Check manager access to resources
            Set<Long> resourceIds = new HashSet<Long>();
            for (Object resource_id_string : resource_id_strings) {
               String resourceID = (String) resource_id_string;
               OpObjectIfc object = broker.getObject(resourceID);
               collectResources(object, resourceIds);
            }

            Iterator accessibleResources = session.accessibleObjects(broker,
                  resourceIds, OpPermission.MANAGER, new OpObjectOrderCriteria(OpResource.class, new TreeMap()));
            /*no accesible resources entities  */
            if (!accessibleResources.hasNext()) {
               reply.setError(session.newError(ERROR_MAP, OpResourceError.ASSIGN_ACCESS_DENIED));
               return reply;
            }

            //project resource ids assignments
            String assignmentQuery = "select assignment.Resource.id from OpProjectNodeAssignment as assignment where assignment.ProjectNode.id = ?";
            OpQuery query = broker.newQuery(assignmentQuery);
            query.setLong(0, targetProjectNode.getId());
            List resourceAssignments = broker.list(query);

            OpResource resource = null;
            OpProjectNodeAssignment projectNodeAssignment = null;
            int accesibleResourcesSize = 0;
            while (accessibleResources.hasNext()) {
               resource = (OpResource) accessibleResources.next();
               if (!resourceAssignments.contains(new Long(resource.getId()))) {
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
         return reply;
      }
      finally {
         broker.close();
      }
   }

   private void collectResources(OpObjectIfc object, Set<Long> resourceIds) {
      if (OpTypeManager.getPrototypeForObject(object).getName().equals(OpResourcePool.RESOURCE_POOL)) {
         //add all its sub-resources
         OpResourcePool pool = (OpResourcePool) object;
         Set resources = pool.getResources();
         for (Object resource : resources) {
            OpObjectIfc entity = (OpObjectIfc) resource;
            collectResources(entity, resourceIds);
         }
      }
      else {
         resourceIds.add(object.getId());
      }
   }

   public XMessage moveResourceNode(OpProjectSession session, XMessage request) {
      //get needed args from request
      List resourceIds = (List) request.getArgument(RESOURCE_IDS);
      String poolId = (String) request.getArgument(POOL_ID);
      OpProjectCalendar xCalendar = session.getCalendar();

      XMessage reply = new XMessage();

      if (resourceIds == null || resourceIds.isEmpty() || poolId == null) {
         return reply; //NOP
      }

      OpBroker broker = session.newBroker();
      try {
         OpTransaction tx = broker.newTransaction();

         OpResourcePool pool = (OpResourcePool) broker.getObject(poolId);

         //check manager access for selected pool
         if (!session.checkAccessLevel(broker, pool.getId(), OpPermission.MANAGER)) {
            reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
         }
         else {
            for (Object resourceId1 : resourceIds) {
               String resourceId = (String) resourceId1;
               OpResource resource = (OpResource) broker.getObject(resourceId);

               // Check manager access for resource's pool
               if (!session.checkAccessLevel(broker, resource.getPool().getId(), OpPermission.MANAGER)) {
                  reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
                  continue;
               }

               //update the resource's pool
               resource.setPool(pool);

               //update personnel costs if inherit pool rate is true and hourly rate changes or external rate changes
               if (resource.getInheritPoolRate() &&
                     (resource.getHourlyRate() != pool.getHourlyRate() || resource.getExternalRate() != pool.getExternalRate())) {
                  resource.updateInternalHourlyRate(pool.getHourlyRate());
                  resource.updateExternalHourlyRate(pool.getExternalRate());
                  updatePersonnelCostsForWorkingVersion(broker, xCalendar, resource);
                  updateActualCosts(broker, resource);
               }
               if (resource.getInheritPoolWorkCalendar()) {
                  resource.updateWorkCalendar(pool.getWorkCalendar());
                  OpProjectCalendarFactory.getInstance().resetCalendar(resource.locator());
               }
               broker.updateObject(resource);
            }
         }

         tx.commit();
      }
      finally {
         broker.close();
      }
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
      try {
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
            if (!session.checkAccessLevel(broker, pool.getSuperPool().getId(), OpPermission.MANAGER) ||
                  !session.checkAccessLevel(broker, superPool.getId(), OpPermission.MANAGER)) {
               reply.setError(session.newError(ERROR_MAP, OpResourceError.MANAGER_ACCESS_DENIED));
               continue;
            }

            //update the pool's superpool
            pool.setSuperPool(superPool);
            broker.updateObject(pool);
         }

         tx.commit();
      }
      finally {
         broker.close();
      }
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
      if (pool.getId() == superPool.getId()) {
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
      Boolean addArchivedResources = (Boolean) request.getArgument(ADD_ARCHIVED_RESOURCES);
      List<XComponent> resultList;
      if (targetPoolLocator != null && outline != null) {
         OpLocator locator = OpLocator.parseLocator(targetPoolLocator);
         //get filter
         Set<String> filteredLocators = (Set<String>) request.getArgument(FILTERED_OUT_IDS);
         OpResourceDataSetFactory.retrieveResourceDataSet(session, dataSet, poolSelector, resourceSelector, locator.getID(), outline.intValue() + 1, filteredLocators, null, Boolean.TRUE.equals(addArchivedResources));
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
      List notSelectableIds = (List) request.getArgument(NOT_SELECTABLE_IDS);
      if (enablePools != null && enableResources != null) {
         OpResourceDataSetFactory.enableResourcesSet(dataSet, enableResources, enablePools, notSelectableIds);
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
      t.commit();
      t = broker.newTransaction();
      OpPermissionDataSetFactory.addSystemObjectPermissions(session, broker, rootPool);
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
      StringBuffer countQuery = new StringBuffer("select count(activityVersion) from OpResource resource inner join resource.ResponsibleActivityVersions activityVersion where resource.id = ? ");
      if (projectPlan != null) {
         countQuery.append(" and activityVersion.PlanVersion.ProjectPlan.id = ");
         countQuery.append(projectPlan.getId());
      }
      OpQuery query = broker.newQuery(countQuery.toString());
      query.setLong(0, resource.getId());
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
      StringBuffer countQuery = new StringBuffer("select count(activity) from OpResource resource inner join resource.ResponsibleActivities activity where resource.id = ? ");
      if (projectPlan != null) {
         countQuery.append(" and activity.ProjectPlan.id = ");
         countQuery.append(projectPlan.getId());
      }
      OpQuery query = broker.newQuery(countQuery.toString());
      query.setLong(0, resource.getId());
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
      StringBuffer countQuery = new StringBuffer("select count(assignmentVersion) from OpResource resource inner join resource.AssignmentVersions assignmentVersion where resource.id = ? ");
      if (projectPlan != null) {
         countQuery.append(" and assignmentVersion.PlanVersion.ProjectPlan.id =");
         countQuery.append(projectPlan.getId());
      }
      OpQuery query = broker.newQuery(countQuery.toString());
      query.setLong(0, resource.getId());
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
      StringBuffer countQuery = new StringBuffer("select count(assignment) from OpResource resource inner join resource.ActivityAssignments assignment where resource.id = ? ");
      if (projectPlan != null) {
         countQuery.append(" and assignment.ProjectPlan.id = ");
         countQuery.append(projectPlan.getId());
      }
      OpQuery query = broker.newQuery(countQuery.toString());
      query.setLong(0, resource.getId());
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
      OpQuery query = broker.newQuery("select permission from OpPermission as permission where permission.Subject.id = :userId and permission.Object.id in (:projectNodeIds) and permission.SystemManaged = :systemManaged");
      query.setLong("userId", user.getId());
      query.setCollection("projectNodeIds", projectNodeIds);
      query.setBoolean("systemManaged", true);
      Iterator permissionsToDelete = broker.iterate(query);

      //list of resource ids for which the user is responsible
      query = broker.newQuery("select resource.id from OpUser user inner join user.Resources resource where user.id = :userId ");
      query.setLong("userId", user.getId());
      List resourceIds = broker.list(query);

      //remove permission if there are no project node assignments for resources with the same responsible user
      while (permissionsToDelete.hasNext()) {
         OpPermission permission = (OpPermission) permissionsToDelete.next();
         if (!resourceIds.isEmpty()) {
            query = broker.newQuery("select count(assignment) from OpProjectNodeAssignment as assignment where assignment.Resource.id in (:resourceIds) and assignment.ProjectNode.id  = :projectId");
            query.setLong("projectId", permission.getObject().getId());
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
      try {
         OpResource resource;
         XMessage xMessage = new XMessage();

         for (int i = 0; i < resourcesDataSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) resourcesDataSet.getChild(i);
            if (dataRow.getChildCount() == 0) {
               resource = (OpResource) (broker.getObject(dataRow.getStringValue()));

               //0 - resource name
               XComponent dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setStringValue(resource.getName());
               dataCell.setEnabled(false);
               dataRow.addChild(dataCell);

               //1 - resource description
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setStringValue(resource.getDescription());
               dataCell.setEnabled(false);
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
      finally {
         broker.close();
      }
   }

   public XMessage getResourceRates(OpProjectSession s, XMessage request) {
      String id_string = (String) (request.getArgument(RESOURCE_ID));
      OpBroker broker = s.newBroker();
      try {
         OpResource resource = (OpResource) (broker.getObject(id_string));
         XMessage xMessage = new XMessage();

         List<Double> ratesList = resource.getRatesForDay(new java.sql.Date(System.currentTimeMillis()));

         xMessage.setArgument(INTERNAL_RATE, ratesList.get(OpResource.INTERNAL_RATE_INDEX));
         xMessage.setArgument(EXTERNAL_RATE, ratesList.get(OpResource.EXTERNAL_RATE_INDEX));
         return xMessage;
      }
      finally {
         broker.close();
      }
   }

   /**
    * @param session
    * @param broker
    * @param request
    * @param resource_data2
    * @param resource
    * @pre
    * @post
    */
   protected void insertResourceAdditional(OpProjectSession session,
         OpBroker broker, XMessage request, HashMap resourceData,
         OpResource resource) {
   }

   /**
    * @param session
    * @param broker
    * @param request
    * @param resource_data2
    * @param resource
    * @pre
    * @post
    */
   protected void updateResourceAdditional(OpProjectSession session,
         OpBroker broker, XMessage request, HashMap resourceData,
         OpResource resource) {
   }

   /**
    * @param session
    * @param broker
    * @param request
    * @param resource_data2
    * @param resource
    * @pre
    * @post
    */
   protected void insertPoolAdditional(OpProjectSession session,
         OpBroker broker, XMessage request, HashMap poolData,
         OpResourcePool pool) {
   }

   /**
    * @param session
    * @param broker
    * @param request
    * @param resource_data2
    * @param resource
    * @pre
    * @post
    */
   protected void updatePoolAdditional(OpProjectSession session,
         OpBroker broker, XMessage request, HashMap poolData,
         OpResourcePool pool) {
   }

   /* (non-Javadoc)
   * @see onepoint.project.OpProjectService#getServiceImpl()
   */
   @Override
   public Object getServiceImpl() {
      return serviceImpl;
   }

}
