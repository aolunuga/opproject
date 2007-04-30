/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.*;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XError;
import onepoint.service.XMessage;

import java.util.*;

public class OpResourceService extends onepoint.project.OpProjectService {

   private static final XLog logger = XLogFactory.getServerLogger(OpResourceService.class);

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
   public final static String HAS_ASSIGNMENTS_IN_TIME_PERIOD = "AssignmentsInPeriod";

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
   public final static String HOURLY_RATES_SET = "HourlyRatesSet";
   public final static String CHECK_INHERIT = "checkInherit";
   //hourly rates data column indexes
   private final int START_DATE_COLUMN_INDEX = 0;
   private final int END_DATE_COLUMN_INDEX = 1;
   private final int HOURLY_RATE_COLUMN_INDEX = 2;
   private final int EXTERNAL_RATE_COLUMN_INDEX = 3;

   private static final int START_DATES_LIST_INDEX = 0;
   private static final int END_DATES_LIST_INDEX = 1;

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
      if (OpInitializer.isMultiUser()) {
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

      //add the OpHourlyRatesPeriod set to the resource
      XComponent hourlyRatesSet = (XComponent) (resource_data.get(HOURLY_RATES_SET));

      OpHourlyRatesPeriod period;
      XComponent dataRow;
      XComponent dataCell;
      Set<OpHourlyRatesPeriod> hourlyRatesPeriodsToAdd = new HashSet<OpHourlyRatesPeriod>(hourlyRatesSet.getChildCount());
      for (int i = 0; i < hourlyRatesSet.getChildCount(); i++) {
         dataRow = (XComponent) hourlyRatesSet.getChild(i);
         period = new OpHourlyRatesPeriod();

         // start date
         dataCell = (XComponent) dataRow.getChild(START_DATE_COLUMN_INDEX);
         period.setStart(dataCell.getDateValue());

         // end date
         dataCell = (XComponent) dataRow.getChild(END_DATE_COLUMN_INDEX);
         period.setFinish(dataCell.getDateValue());

         // hourly rate
         dataCell = (XComponent) dataRow.getChild(HOURLY_RATE_COLUMN_INDEX);
         period.setInternalRate(dataCell.getDoubleValue());

         // external rate
         dataCell = (XComponent) dataRow.getChild(EXTERNAL_RATE_COLUMN_INDEX);
         period.setExternalRate(dataCell.getDoubleValue());

         int validationResult = period.isValid();
         if (validationResult != 0) {
            reply.setError(session.newError(ERROR_MAP, validationResult));
            broker.close();
            return (reply);
         }

         hourlyRatesPeriodsToAdd.add(period);
      }

      resource.setHourlyRatesPeriods(hourlyRatesPeriodsToAdd);

      //if at least two of the period intervals overlap than return error
      if (!resource.checkPeriodDoNotOverlap()) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.DATE_INTERVAL_OVERLAP));
         broker.close();
         return (reply);
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

      // Add hourly rates periods
      for(OpHourlyRatesPeriod hourlyRatesPeriod : hourlyRatesPeriodsToAdd){
         hourlyRatesPeriod.setResource(resource);
         broker.makePersistent(hourlyRatesPeriod);
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

   public XMessage importUser(OpProjectSession session, XMessage request) {
      logger.debug("OpResourceService.importUser()");
      HashMap resource_data = (HashMap) (request.getArgument(RESOURCE_DATA));
      // *** More error handling needed (check mandatory fields)

      XMessage reply = new XMessage();

      OpBroker broker = session.newBroker();
      OpResource resource = new OpResource();

      // get associated user
      // *** TODO: Currently a work-around (should use choice instead of ID)
      String user_id_string = (String) (resource_data.get("UserID"));
      OpUser user;
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
      double availability = (Double) resource_data.get(OpResource.AVAILABLE);
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
      if ((Double) (resource_data.get(OpResource.HOURLY_RATE)) < 0) {
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpResourceError.HOURLY_RATE_NOT_VALID));
         return reply;
      }
      else {
         resource.setHourlyRate((Double) (resource_data.get(OpResource.HOURLY_RATE)));
      }

      resource.setInheritPoolRate((Boolean) (resource_data.get(OpResource.INHERIT_POOL_RATE)));

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
         OpProjectNode projectNode;
         OpProjectNodeAssignment projectNodeAssignment;
         for (Object assigned_project : assigned_projects) {
            projectNode = (OpProjectNode) (broker.getObject((String) assigned_project));
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

      double oldHourlyRate = resource.getHourlyRate();
      double hourlyRate = (Double) (resource_data.get(OpResource.HOURLY_RATE));
      resource.setHourlyRate(hourlyRate);
      double oldExternalRate = resource.getExternalRate();
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

      boolean hourlyRateChanged = (oldHourlyRate != hourlyRate);
      boolean externalRateChanged = (oldExternalRate != externalRate);

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
                  Integer counter = (Integer) broker.iterate(query).next();
                  // at least one project assignment exist for the resources the old user was responsible
                  if (counter > 1) {
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
      XComponent hourlyRatesPeriodSet = (XComponent) (resource_data.get(HOURLY_RATES_SET));
      List<XComponent> periodRows = new ArrayList<XComponent>();
      for (int i = 0; i < hourlyRatesPeriodSet.getChildCount(); i++) {
         periodRows.add((XComponent) hourlyRatesPeriodSet.getChild(i));
      }
      reply = updateHourlyRatesPeriods(session, periodRows, broker, resource);
      if (reply.getError() != null) {
         broker.close();
         return reply;
      }

      //update personnel costs
      if (hourlyRateChanged) {
         updatePersonnelCosts(broker, resource);
      }

      //update personnel external costs
      if (externalRateChanged) {
         //TODO: implement method for updating personnel external costs
      }

      //update availability
      List projectPlans = new ArrayList();
      if (availibityChanged) {
         projectPlans = updateAvailability(broker, resource);
      }

      //update permissions
      XComponent permission_set = (XComponent) resource_data.get(OpPermissionSetFactory.PERMISSION_SET);
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, resource, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }

      t.commit();

      //update the working versions for the project plans
      for (Object projectPlan : projectPlans) {
         OpProjectPlan plan = (OpProjectPlan) projectPlan;
         new OpProjectPlanValidator(plan).validateProjectPlanWorkingVersion(broker, null);
      }

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
      if (request.getArgument(CHECK_INHERIT) != null) {
         hasAssignments = resource.getHourlyRate() != resource.getPool().getHourlyRate() ||
              resource.getExternalRate() != resource.getPool().getExternalRate();
      }
      xMessage.setArgument(HAS_ASSIGNMENTS, hasAssignments);
      return xMessage;
   }

   /**
    * Return a not null message if the resource has any assignments in a given period of time.
    *
    * @param s
    * @param request
    * @return null if resource has no assignments
    */
   public XMessage hasAssignmentsInTimePeriod(OpProjectSession s, XMessage request) {
      String id_string = (String) (request.getArgument(RESOURCE_ID));
      OpBroker broker = s.newBroker();
      OpResource resource = (OpResource) (broker.getObject(id_string));
      boolean hasAssignments = false;
      XMessage xMessage = new XMessage();

      //obtain the list which contains the intervals of the assignments of the resource
      List<List> startFinishOfAssignments = getStartFinishOfAssignments(resource);

      List<java.sql.Date> startList;
      List<java.sql.Date> endList;
      java.sql.Date start = null;
      java.sql.Date end = null;

      startList = startFinishOfAssignments.get(START_DATES_LIST_INDEX);
      endList = startFinishOfAssignments.get(END_DATES_LIST_INDEX);

      //obtain the start date and end date in which the resource has activity versions and activities
      if (!startList.isEmpty() && !endList.isEmpty()) {
         start = startList.get(0);
         end = endList.get(endList.size() - 1);
      }

      Map<Date, OpHourlyRatesPeriod> cutInterval = null;

      if (start != null && end != null) {
         //obtain the map which contains the intervals from the client's data set
         Map intervalsFromHourlyRatesPeriods = getIntervalsFromHourlyRatesPeriods((XComponent) request.getArgument(HOURLY_RATES_SET), resource, start, end);

         //obtain the map which contains the intervals from the client's data set between the start and end dates
         cutInterval = cutIntervalMapToStartFinish(intervalsFromHourlyRatesPeriods, start, end);
      }

      if (resource.getAssignmentVersions().size() != 0 && cutInterval != null) {
         OpActivityVersion activityVersion;
         for(OpAssignmentVersion assignmentVersion : resource.getAssignmentVersions()){
            activityVersion = assignmentVersion.getActivityVersion();
            if(hasDifferentRatesInInterval(activityVersion.getStart(), activityVersion.getFinish(),
                 cutInterval, resource)){
               hasAssignments = true;
               xMessage.setArgument(HAS_ASSIGNMENTS_IN_TIME_PERIOD, hasAssignments);
               return xMessage;
            }
         }
      }

      if (resource.getActivityAssignments().size() != 0 && cutInterval != null) {
         OpActivity activity;
         for(OpAssignment assignment : resource.getActivityAssignments()){
            activity = assignment.getActivity();
            if(hasDifferentRatesInInterval(activity.getStart(), activity.getFinish(), cutInterval, resource)){
               hasAssignments = true;
               xMessage.setArgument(HAS_ASSIGNMENTS_IN_TIME_PERIOD, hasAssignments);
               return xMessage;
            }
         }
      }
      xMessage.setArgument(HAS_ASSIGNMENTS_IN_TIME_PERIOD, hasAssignments);
      return xMessage;
   }

   /**
    * Returns an List containing two lists:
    * 0 - startList: containing the start dates of the resource's activities and activity assignments
    * 1 - endList: containing the end dates of the resource's activities and activity versions
    *
    * @param resource - the resource who's activity/activity versions start and end dates will be returned
    * @return - an <code>List</code> containing two <cod>List</code> objects
    *         0 - startList: containing the start dates of the resource's activities and activity assignments
    *         1 - endList: containing the end dates of the resource's activities and activity versions
    */

   private List<List> getStartFinishOfAssignments(OpResource resource) {
      List<List> result = new ArrayList<List>();
      List<java.sql.Date> startList = new ArrayList<java.sql.Date>();
      List<java.sql.Date> endList = new ArrayList<java.sql.Date>();

      //put all the start/end dates of the activity versions and activities belonging to the resource in a start/end TreeMap
      if (!resource.getAssignmentVersions().isEmpty()) {
         Iterator<OpAssignmentVersion> iterator = resource.getAssignmentVersions().iterator();
         OpAssignmentVersion assignmentVersion;
         OpActivityVersion activityVersion;
         while (iterator.hasNext()) {
            assignmentVersion = iterator.next();
            activityVersion = assignmentVersion.getActivityVersion();
            startList.add(activityVersion.getStart());
            endList.add(activityVersion.getFinish());
         }
      }

      if (!resource.getActivityAssignments().isEmpty()) {
         Iterator<OpAssignment> iterator = resource.getActivityAssignments().iterator();
         OpAssignment assignment;
         OpActivity activity;
         while (iterator.hasNext()) {
            assignment = iterator.next();
            activity = assignment.getActivity();
            startList.add(activity.getStart());
            endList.add(activity.getFinish());
         }
      }
      Collections.sort(startList);
      Collections.sort(endList);
      result.add(START_DATES_LIST_INDEX, startList);
      result.add(END_DATES_LIST_INDEX, endList);
      return result;
   }

   /**
    * Returns a map with all the sorted intervals from the hourlyRatesPeriods data set with the corresponding
    * internal/external rate values and intervals with default internal/external rate values for the
    * intervals which were not specified in the set
    *
    * @param hourlyRatesPeriodsDataSet - the data set containing the OpHourlyRatesPeriods data from the client
    * @param resource                  - the resource which has its rates modified
    * @param startOfTime               - the start date fo the first interval
    * @param endOfTime                 - the end date of the last interval
    * @return - a <code>Map<code> with all the sorted intervals from the hourlyRatesPeriods data set
    */
   private Map<java.sql.Date, OpHourlyRatesPeriod> getIntervalsFromHourlyRatesPeriods(XComponent hourlyRatesPeriodsDataSet,
        OpResource resource, java.sql.Date startOfTime, java.sql.Date endOfTime) {
      Map<java.sql.Date, OpHourlyRatesPeriod> intervalMap = new TreeMap<java.sql.Date, OpHourlyRatesPeriod>();
      OpHourlyRatesPeriod hourlyRatesPeriod;

      if (hourlyRatesPeriodsDataSet != null) {
         //form an OpHourlyRatesPeriod object for each row in the data set and store it in an interval map under it's start date
         for (int i = 0; i < hourlyRatesPeriodsDataSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) hourlyRatesPeriodsDataSet.getChild(i);
            hourlyRatesPeriod = new OpHourlyRatesPeriod();
            hourlyRatesPeriod.setStart(((XComponent) dataRow.getChild(START_DATE_COLUMN_INDEX)).getDateValue());
            hourlyRatesPeriod.setFinish(((XComponent) dataRow.getChild(END_DATE_COLUMN_INDEX)).getDateValue());
            hourlyRatesPeriod.setInternalRate(((XComponent) dataRow.getChild(HOURLY_RATE_COLUMN_INDEX)).getDoubleValue());
            hourlyRatesPeriod.setExternalRate(((XComponent) dataRow.getChild(EXTERNAL_RATE_COLUMN_INDEX)).getDoubleValue());
            if(hourlyRatesPeriod.isValid() == 0){
               intervalMap.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
            }
         }
      }

      //form a list containing all the start dates
      Set<java.sql.Date> startDatesSet = intervalMap.keySet();
      List<java.sql.Date> startDatesList = new ArrayList<java.sql.Date>();
      for(java.sql.Date startDate : startDatesSet){
         startDatesList.add(new java.sql.Date(startDate.getTime()));
      }

      Calendar calendarStart = Calendar.getInstance();
      Calendar calendarFinish = Calendar.getInstance();
      Date finishInterval;
      Date startInterval;

      if (!startDatesList.isEmpty()) {
         //add an interval from "begining of time" to first startDate - 1
         finishInterval = intervalMap.get(startDatesList.get(0)).getStart();
         calendarFinish.setTimeInMillis(finishInterval.getTime());
         calendarFinish.add(Calendar.DATE, -1);
         hourlyRatesPeriod = new OpHourlyRatesPeriod();
         hourlyRatesPeriod.setStart(startOfTime);
         hourlyRatesPeriod.setFinish(new java.sql.Date(calendarFinish.getTimeInMillis()));
         hourlyRatesPeriod.setInternalRate(resource.getHourlyRate());
         hourlyRatesPeriod.setExternalRate(resource.getExternalRate());
         if (!hourlyRatesPeriod.getFinish().before(hourlyRatesPeriod.getStart())) {
            intervalMap.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
         }

         //add an interval from the last finishDate + 1 to the "end of time"
         startInterval = intervalMap.get(startDatesList.get(startDatesList.size() - 1)).getFinish();
         calendarStart.setTimeInMillis(startInterval.getTime());
         calendarStart.add(Calendar.DATE, 1);
         hourlyRatesPeriod = new OpHourlyRatesPeriod();
         hourlyRatesPeriod.setStart(new java.sql.Date(calendarStart.getTimeInMillis()));
         hourlyRatesPeriod.setFinish(endOfTime);
         hourlyRatesPeriod.setInternalRate(resource.getHourlyRate());
         hourlyRatesPeriod.setExternalRate(resource.getExternalRate());
         if (!hourlyRatesPeriod.getFinish().before(hourlyRatesPeriod.getStart())) {
            intervalMap.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
         }
      }
      else {
         hourlyRatesPeriod = new OpHourlyRatesPeriod();
         hourlyRatesPeriod.setStart(startOfTime);
         hourlyRatesPeriod.setFinish(endOfTime);
         hourlyRatesPeriod.setInternalRate(resource.getHourlyRate());
         hourlyRatesPeriod.setExternalRate(resource.getExternalRate());
         intervalMap.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
      }

      //if the intervals have empty periods of time between them form new intervals for those periods with default
      //interval and external rate values
      for (int i = 0; i < startDatesList.size() - 1; i++) {
         finishInterval = intervalMap.get(startDatesList.get(i)).getFinish();
         startInterval = intervalMap.get(startDatesList.get(i + 1)).getStart();
         calendarFinish.setTimeInMillis(finishInterval.getTime());
         calendarStart.setTimeInMillis(startInterval.getTime());
         calendarFinish.add(Calendar.DATE, 1);
         if (!calendarFinish.equals(calendarStart)) {
            calendarStart.add(Calendar.DATE, -1);
            hourlyRatesPeriod = new OpHourlyRatesPeriod();
            hourlyRatesPeriod.setStart(new java.sql.Date(calendarFinish.getTimeInMillis()));
            hourlyRatesPeriod.setFinish(new java.sql.Date(calendarStart.getTimeInMillis()));
            hourlyRatesPeriod.setInternalRate(resource.getHourlyRate());
            hourlyRatesPeriod.setExternalRate(resource.getExternalRate());
            intervalMap.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
         }
      }
      return intervalMap;
   }

   /**
    * Returns a map containing part of the map passed as the parameter and which contains period interval between
    * (or containing) the start and end date
    *
    * @param intervalMap - <code>Map</code> of period intervals which will be filtered
    * @param start       - the start date from which intervals are returned
    * @param end         - the end date up until intervals are returned
    * @return - a <code>Map</code> containing part of the map passed as the parameter and which contains
    *         period interval between (or containing) the start and end date
    */
   private Map<Date, OpHourlyRatesPeriod> cutIntervalMapToStartFinish(Map intervalMap, Date start, Date end) {
      Map<Date, OpHourlyRatesPeriod> cutInterval = new TreeMap<Date, OpHourlyRatesPeriod>();

      Collection<OpHourlyRatesPeriod> hourlyRatesCollection = intervalMap.values();
      boolean containsStart = false;
      boolean containsEnd = false;

      //add an interval from the interval map only if if contains the start date or
      // if it follows an interval which contains the start date and doesn't follow an interval which contains the end date
      for(OpHourlyRatesPeriod hourlyRatesPeriod : hourlyRatesCollection){
         if (!start.before(hourlyRatesPeriod.getStart()) && !start.after(hourlyRatesPeriod.getFinish())) {
            containsStart = true;
            cutInterval.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
         }
         else if (containsStart && !containsEnd) {
            cutInterval.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
         }
         if (!end.before(hourlyRatesPeriod.getStart()) && !end.after(hourlyRatesPeriod.getFinish())) {
            containsEnd = true;
         }
      }
      return cutInterval;
   }

   /**
    * Returns <code>true</code> if the resource has at least one internal/external rate different from the rates
    * in the time interval between start and end, or <code>false</code> otherwise
    * @param start - the start date of the interval for which the rates wil be compared
    * @param end - the end date of the interval for which the rates wil be compared
    * @param cutInterval - a <code>Map</code> cotaining the intervals with different rates
    * @param resource - the <code>OpResource</code> object for which the rates will be compared
    * @return - <code>true</code> if the resource has at least one internal/external rate different from the rates
    * in the time interval between start and end, or <code>false</code> otherwise
    */
   private boolean hasDifferentRatesInInterval(java.sql.Date start, java.sql.Date end,
        Map<Date, OpHourlyRatesPeriod> cutInterval, OpResource resource) {
      java.sql.Date startDate;
      java.sql.Date endDate;
      List<Double> internalRatesForInterval;
      List<Double> externalRatesForInterval;
      Map<Date, OpHourlyRatesPeriod> cutIntervalForActivity;

      //obtain the map of the different rates in the activity's time interval
      cutIntervalForActivity = cutIntervalMapToStartFinish(cutInterval, start, end);
      //determine the start/end dated for which the comparison of rates will be made
      //here we intersect the two time intervals
      for (OpHourlyRatesPeriod hourlyRatesPeriod : cutIntervalForActivity.values()) {
         if (!start.before(hourlyRatesPeriod.getStart())) {
            startDate = start;
         }
         else {
            startDate = hourlyRatesPeriod.getStart();
         }
         if (!end.after(hourlyRatesPeriod.getFinish())) {
            endDate = end;
         }
         else {
            endDate = hourlyRatesPeriod.getFinish();
         }

         List<List> ratesList = resource.getRatesForInterval(startDate, endDate);
         internalRatesForInterval = ratesList.get(OpResource.INTERNAL_RATE_LIST_INDEX);
         for (Double anInternalRatesForInterval : internalRatesForInterval) {
            if (hourlyRatesPeriod.getInternalRate() != anInternalRatesForInterval) {
               return true;
            }
         }
         //compare only in the smallest interval (activityStart - activityFinish OR hourlyRatePeriodStart - hourlyRatePeriodFinish)
         externalRatesForInterval = ratesList.get(OpResource.EXTERNAL_RATE_LIST_INDEX);
         for (Double anExternalRatesForInterval : externalRatesForInterval) {
            if (hourlyRatesPeriod.getExternalRate() != anExternalRatesForInterval) {
               return true;
            }
         }
      }

      return false;
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
         sumAssigned = (Double) broker.list(query).iterator().next();

         double personnelCosts = 0;
         for (Object o : activityVersion.getAssignmentVersions()) {
            OpAssignmentVersion assignment = ((OpAssignmentVersion) o);
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
      OpQuery query;
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
            int activityAssignmentsCounter = getResourcePlanningAssignmentsCount(broker, assignment.getResource(), assignment.getProjectNode().getPlan());
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

   /**
    * Updates the already existent OpHourlyRatesPeriods for the modified resource and inserts the newly added ones.
    *
    * @param session  a <code>OpProjectSession</code> representing the current session
    * @param periods  a <code>Set</code > of <code>XComponent DataRows</code> containing the information about the OpHourlyRatesPeriods.
    * @param broker   a <code>OpBroker</code> used for performing business operations.
    * @param resource a <code>OpResource</code> representing the resource which was edited.
    * @return <code>XMessage</code>
    */
   private XMessage updateHourlyRatesPeriods(OpProjectSession session, List<XComponent> periods, OpBroker broker, OpResource resource) {
      OpQuery query = null;
      //the reply message
      XMessage reply = new XMessage();
      logger.debug("HourlyRatesPeriods " + periods);
      // Query stored hourly rates periods
      query = broker.newQuery("select hourlyratesperiod.ID from OpHourlyRatesPeriod as hourlyratesperiod where hourlyratesperiod.Resource.ID = ?");
      query.setLong(0, resource.getID());

      Iterator result = broker.list(query).iterator();
      HashSet storedHourlyRatesIds = new HashSet();
      while (result.hasNext()) {
         storedHourlyRatesIds.add(result.next());
      }

      // *** Iterate client-side hourlyRatesPeriods-ids and remove ids found in set
      OpLocator locator = null;
      OpHourlyRatesPeriod hourlyRatesPeriod = null;
      ArrayList<OpHourlyRatesPeriod> hourlyRatesPeriodsToAdd = new ArrayList<OpHourlyRatesPeriod>();
      ArrayList<OpHourlyRatesPeriod> hourlyRatesPeriodsModified = new ArrayList<OpHourlyRatesPeriod>();

      if ((periods != null) && !periods.isEmpty()) {
         XComponent dataCell = null;
         for(XComponent dataRow : periods){
            //if this is a new row it doesn't have a StringValue set on it
            boolean contained = true;
            if (dataRow.getStringValue() != null) {
               locator = OpLocator.parseLocator(dataRow.getStringValue());

               //create the new OpHourlyRatesPeriods and store them in an ArrayList
               if (storedHourlyRatesIds.remove(new Long(locator.getID()))) {
                  //store the modified OpHourlyRatesPeriods in an ArrayList
                  hourlyRatesPeriod = (OpHourlyRatesPeriod) broker.getObject(locator.getPrototype().getInstanceClass(), locator.getID());
                  contained = true;
               }
            }
            //new row
            else {
               hourlyRatesPeriod = new OpHourlyRatesPeriod();
               contained = false;
            }

            // start date
            dataCell = (XComponent) dataRow.getChild(START_DATE_COLUMN_INDEX);
            hourlyRatesPeriod.setStart(dataCell.getDateValue());

            // end date
            dataCell = (XComponent) dataRow.getChild(END_DATE_COLUMN_INDEX);
            hourlyRatesPeriod.setFinish(dataCell.getDateValue());

            // hourly rate
            dataCell = (XComponent) dataRow.getChild(HOURLY_RATE_COLUMN_INDEX);
            hourlyRatesPeriod.setInternalRate(dataCell.getDoubleValue());

            // external rate
            dataCell = (XComponent) dataRow.getChild(EXTERNAL_RATE_COLUMN_INDEX);
            hourlyRatesPeriod.setExternalRate(dataCell.getDoubleValue());

            int validationResult = hourlyRatesPeriod.isValid();
            if (validationResult != 0) {
               reply.setError(session.newError(ERROR_MAP, validationResult));
               broker.close();
               return (reply);
            }

            if (!contained) {
               hourlyRatesPeriod.setResource(resource);
               hourlyRatesPeriodsToAdd.add(hourlyRatesPeriod);
            }
            else {
               hourlyRatesPeriodsModified.add(hourlyRatesPeriod);
            }
         }
      }

      //check all added and updated HourlyRatesPeriods for overlaping intervals
      Set<OpHourlyRatesPeriod> allAddedUpdatedPeriods = new HashSet<OpHourlyRatesPeriod>();
      allAddedUpdatedPeriods.addAll(hourlyRatesPeriodsToAdd);
      allAddedUpdatedPeriods.addAll(hourlyRatesPeriodsModified);
      resource.setHourlyRatesPeriods(allAddedUpdatedPeriods);
      if (!resource.checkPeriodDoNotOverlap()) {
         reply.setError(session.newError(ERROR_MAP, OpResourceError.DATE_INTERVAL_OVERLAP));
         broker.close();
         return (reply);
      }

      // insert the newly added hourly rates periods
      for(OpHourlyRatesPeriod hourlyRatesPeriodToAdd : hourlyRatesPeriodsToAdd){
         broker.makePersistent(hourlyRatesPeriodToAdd);
      }

      // update the modified hourly rates periods
      for(OpHourlyRatesPeriod hourlyRatesPeriodToModify : hourlyRatesPeriodsModified){
         broker.updateObject(hourlyRatesPeriodToModify);
      }

      // Delete the deleted OpHourlyRatesPeriod for the resource
      if (!storedHourlyRatesIds.isEmpty()) {
         query = broker.newQuery("select hourlyratesperiod from OpHourlyRatesPeriod as hourlyratesperiod where hourlyratesperiod.Resource.ID = :resourceId and hourlyratesperiod.ID in (:hourlyRatesIds)");
         query.setLong("resourceId", resource.getID());
         query.setCollection("hourlyRatesIds", storedHourlyRatesIds);
         result = broker.list(query).iterator();
         while (result.hasNext()) {
            OpHourlyRatesPeriod hourlyRatePeriod = (OpHourlyRatesPeriod) result.next();
            broker.deleteObject(hourlyRatePeriod);
         }
      }
      return reply;
   }

   public XMessage deleteResources(OpProjectSession session, XMessage request) {

      // Check manager access to pools of resources; delete all accessible resources; return error if not all deleted

      ArrayList id_strings = (ArrayList) (request.getArgument(RESOURCE_IDS));
      logger.debug("OpResourceService.deleteResources(): resource_ids = " + id_strings);

      OpBroker broker = session.newBroker();

      List resourceIds = new ArrayList();
      XMessage reply = new XMessage();

      if (id_strings.size() == 0) {
         return new XMessage();
         // TODO: Return error (nothing to delete)
      }
      for (Object id_string : id_strings) {
         resourceIds.add(OpLocator.parseLocator((String) id_string).getID());
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
           "and size(resource.ActivityAssignments) = 0 and size(resource.AssignmentVersions) = 0" +
           "and size(resource.ResponsibleActivities) = 0 and size(resource.ResponsibleActivityVersions) = 0");
      query.setCollection("resourceIds", resourceIds);
      query.setCollection("accessiblePoolIds", accessiblePoolIds);
      List resources = broker.list(query);
      //size of persistent resources list without activityAssignments and assignmentVersions should be equal with selected resources size
      if (resources.size() != resourceIds.size()) {
         logger.warn("Resource from " + resources + " are used in project assignments");
         reply.setError(session.newError(ERROR_MAP, OpResourceError.DELETE_RESOURCE_ASSIGNMENTS_DENIED));
         broker.close();
         return reply;
      }
      for (Object resource1 : resources) {
         OpResource resource = (OpResource) resource1;
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

      // Update all inherited hourly-rate fields of pool resources if pool rate has changed
      // TODO: Probably optimize by using a query for "InheritPoolRate = true"
      if (poolRateChanged) {
         Iterator resources = pool.getResources().iterator();
         OpResource resource = null;
         while (resources.hasNext()) {
            resource = (OpResource) resources.next();
            if (resource.getInheritPoolRate()) {
               resource.setHourlyRate(pool.getHourlyRate());
               resource.setExternalRate(pool.getExternalRate());
               updatePersonnelCosts(broker, resource);
               broker.updateObject(resource);
            }
         }
      }

      // Update all inherited external-rate fields of pool resources if external pool rate has changed
      // TODO: Probably optimize by using a query for "InheritPoolRate = true"
      if (poolExternalRateChanged) {
         Iterator resources = pool.getResources().iterator();
         OpResource resource = null;
         while (resources.hasNext()) {
            resource = (OpResource) resources.next();
            if (resource.getInheritPoolRate()) {
               //TODO: Implement method for updating personnel external costs
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
      if (object.getPrototype().getName().equals(OpResourcePool.RESOURCE_POOL)) {
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
    * Returns the number of activity assignments/responsible activities for the given <code>resource</code> and </code>projectPlan</code>
    *
    * @param broker      <code>OpBroker</code> used for performing business operations.
    * @param resource    <code>OpResource</code> entity
    * @param projectPlan <code>OpProjectPlan</code> entity
    * @return <code>int</code> representing the number of activity assignments of the resource into the given project
    */
   public static int getResourcePlanningAssignmentsCount(OpBroker broker, OpResource resource, OpProjectPlan projectPlan) {
      String countQuery = "select count(assignment) from OpResource resource inner join resource.ActivityAssignments assignment where resource.ID = ? and assignment.ProjectPlan.ID = ?";
      OpQuery query = broker.newQuery(countQuery);
      query.setLong(0, resource.getID());
      query.setLong(1, projectPlan.getID());
      Integer counterAssignments = (Integer) broker.iterate(query).next();

      countQuery = "select count(assignmentVersion) from OpResource resource inner join resource.AssignmentVersions assignmentVersion where resource.ID = ? and assignmentVersion.PlanVersion.ProjectPlan.ID = ?";
      query = broker.newQuery(countQuery);
      query.setLong(0, resource.getID());
      query.setLong(1, projectPlan.getID());
      Integer counterAssignmentVersions = (Integer) broker.iterate(query).next();

      countQuery = "select count(activity) from OpResource resource inner join resource.ResponsibleActivities activity where resource.ID = ? and activity.ProjectPlan.ID = ?";
      query = broker.newQuery(countQuery);
      query.setLong(0, resource.getID());
      query.setLong(1, projectPlan.getID());
      Integer counterResponsible = (Integer) broker.iterate(query).next();

      countQuery = "select count(activityVersion) from OpResource resource inner join resource.ResponsibleActivityVersions activityVersion where resource.ID = ? and activityVersion.PlanVersion.ProjectPlan.ID = ?";
      query = broker.newQuery(countQuery);
      query.setLong(0, resource.getID());
      query.setLong(1, projectPlan.getID());
      Integer counterResponsibleVersions = (Integer) broker.iterate(query).next();

      return counterAssignments + counterAssignmentVersions +
           counterResponsible + counterResponsibleVersions;
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
            if (counter == 1) {
               broker.deleteObject(permission);
            }
         }
      }
   }

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
            dataRow.addChild(dataCell);

            //1 - resource description
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setStringValue(resource.getDescription());
            dataRow.addChild(dataCell);

            //2 - adjust rates - false
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataCell.setBooleanValue(false);
            dataRow.addChild(dataCell);

            //3 - hourly rate - empty
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataRow.addChild(dataCell);

            //4 - external rate - empty
            dataCell = new XComponent(XComponent.DATA_CELL);
            dataRow.addChild(dataCell);
         }
      }

      xMessage.setArgument(RESOURCES_DATA_SET, resourcesDataSet);
      return xMessage;
   }

   public XMessage getResourceRates(OpProjectSession s, XMessage request){
      String id_string = (String) (request.getArgument(RESOURCE_ID));
      OpBroker broker = s.newBroker();
      OpResource resource = (OpResource) (broker.getObject(id_string));
      XMessage xMessage = new XMessage();

      List<Double> ratesList = resource.getRatesForDay(new java.sql.Date(System.currentTimeMillis()));

      xMessage.setArgument(INTERNAL_RATE,ratesList.get(OpResource.INTERNAL_RATE_INDEX));
      xMessage.setArgument(EXTERNAL_RATE,ratesList.get(OpResource.EXTERNAL_RATE_INDEX));
      return xMessage;
   }
}
