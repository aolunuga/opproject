/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
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
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.OpTransactionLock;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.project_status.OpProjectStatusService;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpLock;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpSubject;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceException;
import onepoint.service.server.XServiceManager;


public class OpProjectAdministrationService extends OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpProjectAdministrationService.class);

   private OpProjectAdministrationServiceImpl serviceImpl =
        new OpProjectAdministrationServiceImpl();

   public final static String PROJECT_DATA = "project_data";
   public final static String PROJECT_ID = "project_id";
   public final static String PROJECT_IDS = "project_ids";
   public final static String PORTFOLIO_DATA = "portfolio_data";
   public final static String PORTFOLIO_ID = "portfolio_id";
   public final static String PORTFOLIO_IDS = "portfolio_ids";
   public final static String GOALS_SET = "goals_set";
   public final static String EDIT_MODE = "edit_mode";
   public final static String NULL_ID = "null";

   public final static OpProjectErrorMap ERROR_MAP = new OpProjectErrorMap();

   /**
    * Query used to retrieve projects that have a certain name.
    */
   protected static final String PROJECT_NODE_NAME_QUERY_STRING = "select project from OpProjectNode project where project.Name = ?";

   public final static String RESOURCE_SET = "resource_set";
   public final static String ORIGINAL_RESOURCE_SET = "original_resource_set";
   public final static String VERSIONS_SET = "versions_set";
   private final static String PORTFOLIO_LOCATOR = "PortfolioID";
   private final static String PROJECT_ROW_PARAMETER = "project_row";
   private final static String TYPES_PARAMETER = "project_types";
   private final static String TABULAR_PARAMETER = "tabular";
   private final static String MODIFIED_RATES = "modified_rates";

   public final static int ADJUST_RATES_COLUMN_INDEX = 2;
   public final static int INTERNAL_PROJECT_RATE_COLUMN_INDEX = 3;
   public final static int EXTERNAL_PROJECT_RATE_COLUMN_INDEX = 4;
   public final static int PERIOD_START_DATE = 5;
   public final static int PERIOD_END_DATE = 6;
   public final static int INTERNAL_PERIOD_RATE_COLUMN_INDEX = 7;
   public final static int EXTERNAL_PERIOD_RATE_COLUMN_INDEX = 8;

   public final static String HAS_ASSIGNMENTS = "Assignments";
   public final static String HAS_ASSIGNMENTS_IN_TIME_PERIOD = "AssignmentsInPeriod";
   /**
    * The name of this service.
    */
   public static final String SERVICE_NAME = "ProjectService";

   private static final String UNEXPANDED_NODES = "unexpandedNodes";
   private final static String APPLY_PERMISSIONS_RECURSIVELY = "ApplyPermissionsRecursively";

   public XMessage insertProject(OpProjectSession session, XMessage request) {

      // *** TODO: Add start-date of project (maybe also due-date?)
      logger.debug("OpProjectAdministrationService.insertProject()");
      HashMap project_data = (HashMap) (request.getArgument(PROJECT_DATA));
      String portfolioLocator = (String) project_data.get(PORTFOLIO_LOCATOR);

      XMessage reply = new XMessage();
      XError error = null;

      OpProjectNode project = new OpProjectNode();
      try {
         project.fillProjectNode(project_data);
      }
      catch (OpEntityException e) {
         error = session.newError(ERROR_MAP, e.getErrorCode());
         reply.setError(error);
         return reply;
      }

      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      try {
         // check if project name is already used
         OpQuery projectNameQuery = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
         projectNameQuery.setString(0, project.getName());
         Iterator projects = broker.iterate(projectNameQuery);
         if (projects.hasNext()) {
            error = session.newError(ERROR_MAP, OpProjectError.PROJECT_NAME_ALREADY_USED);
            reply.setError(error);
            return reply;
         }

         logger.debug("PortfolioID='" + portfolioLocator + "'");
         OpProjectNode portfolio;
         if (portfolioLocator != null) {
            portfolio = (OpProjectNode) broker.getObject(portfolioLocator);
            if (portfolio == null) {
               logger.warn("Portfolio is null. Project will be added to root portfolio");
               portfolio = OpProjectAdministrationService.findRootPortfolio(broker);
            }
         }
         else {
            logger.warn("Given portfolio locator is null. Project will be added to root portfolio");
            portfolio = OpProjectAdministrationService.findRootPortfolio(broker);
         }

         byte userAccessLevel = session.effectiveAccessLevel(broker, portfolio.getId());

         if (userAccessLevel != OpPermission.ADMINISTRATOR) {
            double budget = ((Double) project_data.get(OpProjectNode.BUDGET)).doubleValue();
            String statusLocator = (String) project_data.get(OpProjectNode.STATUS);

            if (budget != 0) {
               error = session.newError(ERROR_MAP, OpProjectError.NO_RIGHTS_CHANGING_BUDGET_ERROR);
               reply.setError(error);
               return reply;
            }

            if (!statusLocator.equals(NULL_ID)) {
               error = session.newError(ERROR_MAP, OpProjectError.NO_RIGHTS_CHANGING_STATUS_ERROR);
               reply.setError(error);
               return reply;
            }
         }
         project.setSuperNode(portfolio);

         // Check manager access for portfolio
         if (!session.checkAccessLevel(broker, project.getSuperNode().getId(), OpPermission.MANAGER)) {
            logger.warn("ERROR: Insert access to portfolio denied; ID = " + project.getSuperNode().getId());
            reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
            return reply;
         }

         //project status
         String statusLocator = (String) project_data.get(OpProjectNode.STATUS);
         OpProjectStatus status = null;
         if (statusLocator != null && !statusLocator.equals(NULL_ID)) {
            status = (OpProjectStatus) (broker.getObject(statusLocator));
         }
         else {
            status = null;
         }
         if ((status == null && project.getStatus() != null) ||
              (status != null && project.getStatus() == null) ||
              (status != null && project.getStatus() != null && project.getStatus().getId() != status.getId())) {

            //check access level
            if ((userAccessLevel != OpPermission.ADMINISTRATOR) && !(status.equals(project.getStatus()))) {
               error = session.newError(ERROR_MAP, OpProjectError.NO_RIGHTS_CHANGING_STATUS_ERROR);
               reply.setError(error);
               return reply;
            }
            project.setStatus(status);
         }

         t = broker.newTransaction();

         broker.makePersistent(project);

         // Insert project plan including settings
         OpProjectPlan projectPlan = new OpProjectPlan();
         projectPlan.setProjectNode(project);
         projectPlan.copyDatesFromProject();

         // calculation Mode
         Boolean calculationMode = (Boolean) project_data.get(OpProjectPlan.CALCULATION_MODE);
         if (calculationMode != null && !calculationMode.booleanValue()) {
            projectPlan.setCalculationMode(OpProjectPlan.INDEPENDENT);
         }
         else {
            projectPlan.setCalculationMode(OpProjectPlan.EFFORT_BASED);
         }

         Boolean progressTracked = (Boolean) project_data.get(OpProjectPlan.PROGRESS_TRACKED);
         if (progressTracked != null) {
            projectPlan.setProgressTracked(progressTracked.booleanValue());
         }
         else {
            projectPlan.setProgressTracked(true);
         }
         projectPlan.setActivities(new HashSet<OpActivity>());
         project.setPlan(projectPlan);
         broker.makePersistent(projectPlan);

         //allow a template to be set
         this.insertProjectAdditional(session, broker, request, project_data, project);

         // Insert goals
         XComponent goalsDataSet = (XComponent) (request.getArgument(GOALS_SET));
         reply = insertGoals(session, broker, project, goalsDataSet);
         if (reply.getError() != null) {
            return reply;
         }
         
         //insert project assignments
         XComponent assignedResourcesSet = (XComponent) request.getArgument(RESOURCE_SET);
         reply = insertProjectAssignments(session, broker, project, assignedResourcesSet);
         if (reply.getError() != null) {
            return reply;
         }

         XComponent permission_set = (XComponent) project_data.get(OpPermissionDataSetFactory.PERMISSION_SET);
         XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, project, permission_set);
         if (result != null) {
            reply.setError(result);
            return reply;
         }

         t.commit();
         return reply;
      }
      finally {
         finalizeSession(t, broker);
      }
   }

   /**
    * Inserts the goals related to the project passed as a parameter. In case of an error returns
    * an <code>XMessage</code> object containing the error code
    *
    * @param session
    * @param broker
    * @param project      - the project for which the assignments are inserted
    * @param goalsDataSet - the clients data set containing the information about the project goals
    * @return - an <code>XMessage</code> object containing the error code in case of an error
    */
   private XMessage insertGoals(OpProjectSession session, OpBroker broker,
        OpProjectNode project, XComponent goalsDataSet) {
      //the reply message
      XMessage reply = new XMessage();

      XComponent data_row;
      XComponent data_cell;
      int i;
      OpGoal goal;
      for (i = 0; i < goalsDataSet.getChildCount(); i++) {
         data_row = (XComponent) (goalsDataSet.getChild(i));
         goal = new OpGoal();
         goal.setProjectNode(project);
         data_cell = (XComponent) (data_row.getChild(0));
         goal.setCompleted(data_cell.getBooleanValue());
         data_cell = (XComponent) (data_row.getChild(1));
         goal.setName(data_cell.getStringValue() == null ? "" : data_cell.getStringValue());
         data_cell = (XComponent) (data_row.getChild(2));
         if (data_cell.getIntValue() < 1 || data_cell.getIntValue() > 9) {
            reply.setError(session.newError(ERROR_MAP, OpProjectError.GOAL_PRIORITY_ERROR));
            return reply;
         }
         else {
            goal.setPriority((byte) data_cell.getIntValue());
         }

         broker.makePersistent(goal);
      }
      return reply;
   }

   /**
    * Inserts the assignments related to the project passed as a parameter. In case of an error returns
    * an <code>XMessage</code> object containing the error code
    *
    * @param session
    * @param broker
    * @param project              - the project for which the assignments are inserted
    * @param assignedResourcesSet - the clients data set containing the information about the project assignments
    * @return - an <code>XMessage</code> object containing the error code in case of an error
    */
   protected XMessage insertProjectAssignments(OpProjectSession session, OpBroker broker,
        OpProjectNode project, XComponent assignedResourcesSet) {
      //the reply message
      XMessage reply = new XMessage();

      //insert project assignments
      if (assignedResourcesSet != null && assignedResourcesSet.getChildCount() > 0) {
         for (int i = 0; i < assignedResourcesSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) assignedResourcesSet.getChild(i);
            reply = insertProjectAssignment(session, broker, project, dataRow);
            if (reply.getError() != null) {
               return reply;
            }
         }
      }
      return reply;
   }

   /**
    * Inserts an OpProjectNodeAssignment obtained from the data row passed as parameter. In case of an error returns
    * an <code>XMessage</code> object containing the error code
    *
    * @param session
    * @param broker
    * @param project - the project for which the assignments are inserted
    * @param dataRow - the clients data row containing the information about the project assignments
    * @return an <code>XMessage</code> object containing the error code in case of an error
    */
   protected XMessage insertProjectAssignment(OpProjectSession session, OpBroker broker,
        OpProjectNode project, XComponent dataRow) {
      XMessage reply = new XMessage();

      OpResource resource = (OpResource) (broker.getObject(dataRow.getStringValue()));

      //3 - internal rate
      Double internalRate = null;
      if (((XComponent) dataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX)).getValue() != null) {
         internalRate = ((XComponent) dataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX)).getDoubleValue();
      }
      //4 - external rate
      Double externalRate = null;
      if (((XComponent) dataRow.getChild(EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).getValue() != null) {
         externalRate = ((XComponent) dataRow.getChild(EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).getDoubleValue();
      }

      OpProjectNodeAssignment projectNodeAssignment = new OpProjectNodeAssignment();
      projectNodeAssignment.setResource(resource);
      projectNodeAssignment.setProjectNode(project);
      projectNodeAssignment.setHourlyRate(internalRate);
      projectNodeAssignment.setExternalRate(externalRate);

      if (projectNodeAssignment.getHourlyRate() != null) {
         // check valid internal rate/resource/project
         if (projectNodeAssignment.getHourlyRate() < 0) {
            reply.setError(session.newError(ERROR_MAP, OpProjectError.INTERNAL_RATE_NOT_VALID));
            return reply;
         }
      }
      if (projectNodeAssignment.getExternalRate() != null) {
         // check valid external rate/resource/project
         if (projectNodeAssignment.getExternalRate() < 0) {
            reply.setError(session.newError(ERROR_MAP, OpProjectError.EXTERNAL_RATE_NOT_VALID));
            return reply;
         }
      }

      broker.makePersistent(projectNodeAssignment);
      insertContributorPermission(broker, project, resource);
      broker.getConnection().flush();

      return reply;
   }

   /**
    * Template method for setting the template of a project. By default, doesn't do anything.
    *
    * @param session      - the <code>OpProjectSession</code> object.
    * @param broker       - the <code>OpBroker</code> used for performing business operations.
    * @param request      - the <code>XMessage</code> object representing the request.
    * @param project_data - the <code>HashMap</code> representing the parameters.
    * @param project      - the <code>OpProjectNode</code> entity representing a project node.
    */
   protected void insertProjectAdditional(OpProjectSession session, OpBroker broker,
        XMessage request, Map project_data, OpProjectNode project) {
      //do nothing here
   }

   public XMessage updateProject(OpProjectSession session, XMessage request) {
      // *** TODO: Check for other fields that can be updated
      String id_string = (String) (request.getArgument(PROJECT_ID));
      logger.debug("OpProjectAdministrationService.updateProject(): id = " + id_string);
      HashMap project_data = (HashMap) (request.getArgument(PROJECT_DATA));

      XComponent goalsDataSet = (XComponent) (request.getArgument(GOALS_SET));
      XComponent versionDataSet = (XComponent) request.getArgument(VERSIONS_SET);
      XComponent assignedResourcesSet = (XComponent) request.getArgument(RESOURCE_SET);
      
      XMessage reply = new XMessage();
      XError error;
      OpBroker broker = session.newBroker();
      OpTransaction transaction = null;

      OpTransactionLock.getInstance().writeLock(id_string);
      try {
         // transaction = broker.newTransaction();
         OpProjectNode project = (OpProjectNode) (broker.getObject(id_string));

         if (project == null) {
            logger.warn("ERROR: Could not find object with ID " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpProjectError.PROJECT_NOT_FOUND));
            return reply;
         }

         byte projectAccesssLevel = session.effectiveAccessLevel(broker, project.getId());
         byte projectPermission = session.effectivePermissions(broker, project.getId());

         if (projectPermission < OpPermission.MANAGER) {
            logger.warn("ERROR: Udpate access to project denied; ID = " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpProjectError.PROJECT_LOCKED_ERROR));
            return reply;
         }
         Date originalStartDate = project.getStart();

         try {
            OpProjectNode dummy = new OpProjectNode();
            dummy.fillProjectNode(project_data);

            if (!checkPermissionsForChange(session, reply, projectPermission, OpPermission.ADMINISTRATOR, project.getBudget() != dummy.getBudget(), OpProjectError.NO_RIGHTS_CHANGING_BUDGET_ERROR) ||
                  !checkPermissionsForChange(session, reply, projectPermission, OpPermission.ADMINISTRATOR, project.getArchived() != dummy.getArchived(), OpProjectError.NO_PERMISSION_CHANGING_ARCHIVED_STATE_ERROR)) {
               return reply;
            }
            
            project.fillProjectNode(project_data);
         }
         catch (OpEntityException e) {
            reply.setError(session.newError(ERROR_MAP, e.getErrorCode()));
            return reply;
         }

         // check if project name is already used
         String projectName = (String) (project_data.get(OpProjectNode.NAME));
         OpQuery projectNameQuery = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
         projectNameQuery.setString(0, projectName);
         Iterator projects = broker.iterate(projectNameQuery);
         while (projects.hasNext()) {
            OpProjectNode other = (OpProjectNode) projects.next();
            if (other.getId() != project.getId()) {
               error = session.newError(ERROR_MAP, OpProjectError.PROJECT_NAME_ALREADY_USED);
               reply.setError(error);
               return reply;
            }
         }

         //check if the start date is in the future
         if (project.getStart().after(originalStartDate)) {
            XMessage tmpReply = this.shiftPlanDates(session, broker, project, project.getStart(), originalStartDate);
            if (tmpReply != null && tmpReply.getError() != null) {
               return tmpReply;
            }
         }
         // moved from above to avoid deadlock in reply = this.shiftPlanDates(session, project, project.getStart());
         transaction = broker.newTransaction();
         //project status
         String statusLocator = (String) project_data.get(OpProjectNode.STATUS);
         OpProjectStatus status = null;
         OpProjectStatus oldStatus = project.getStatus();
         if (statusLocator != null && !statusLocator.equals(NULL_ID)) {
            status = (OpProjectStatus) (broker.getObject(statusLocator));
            if ((projectPermission < OpPermission.ADMINISTRATOR) && status.getId() != oldStatus.getId()) {
               error = session.newError(ERROR_MAP, OpProjectError.NO_RIGHTS_CHANGING_STATUS_ERROR);
               reply.setError(error);
               return reply;
            }
            project.setStatus(status);
         }
         else {
            if ((projectPermission != OpPermission.ADMINISTRATOR) && (project.getStatus() != null)) {
               error = session.newError(ERROR_MAP, OpProjectError.NO_RIGHTS_CHANGING_STATUS_ERROR);
               reply.setError(error);
               return reply;
            }

            project.setStatus(null);
         }

         OpProjectPlan projectPlan = project.getPlan();
         //check if the project plan has any activities and if not, update the start and end
         if (!OpProjectDataSetFactory.hasActivities(broker, projectPlan)) {
            projectPlan.copyDatesFromProject();
         }

         if (projectPlan.getLatestVersion() == null) {
            Boolean pt = (Boolean) project_data.get(OpProjectNode.PROGRESS_TRACKED);
            projectPlan.setProgressTracked(pt.booleanValue());
         }
         
         //set the calculation mode
         Boolean calculationMode = (Boolean) project_data.get(OpProjectPlan.CALCULATION_MODE);
         if (calculationMode != null && !calculationMode) {
            projectPlan.setCalculationMode(OpProjectPlan.INDEPENDENT);
         }
         else {
            projectPlan.setCalculationMode(OpProjectPlan.EFFORT_BASED);
         }

         // Update current goals
         reply = updateGoals(session, broker, project, goalsDataSet);
         if (reply.getError() != null) {
            return reply;
         }

         //update project assignments
//         transaction.commit();
//         OpTransaction t = broker.newTransaction();
         reply = updateProjectAssignments(session, broker, project, assignedResourcesSet);
         if (reply.getError() != null) {
            return reply;
         }
//         t.commit();
//         transaction = broker.newTransaction();
         
         
         // update permissions
         XComponent permission_set = (XComponent) project_data.get(OpPermissionDataSetFactory.PERMISSION_SET);
         XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, project, permission_set);
         if (result != null) {
            reply.setError(result);
            return reply;
         }
         result = checkChangingPermissionForEditingUser(session, broker, project);
         if (result != null) {
            reply.setError(result);
            return reply;
         }

         try {
            updateProjectAdditional(session, broker, request, project_data, project, permission_set);
         }
         catch (XServiceException exc) {
            exc.append(reply);
            return reply;
         }

         OpProjectCalendar xCalendar = session.getCalendar();
         //update personnel & actual costs
         updatePersonnelCostsForWorkingVersion(broker, xCalendar, project);

         //update the actual costs only if there were some modifications on the resource rates or hourly rates
         if ((Boolean) request.getArgument(MODIFIED_RATES)) {
            updateActualCosts(broker, project);
         }

         //delete unused status if it is inactive and it is no longer refered by another project
         if (oldStatus != null && !oldStatus.getActive()) {
            oldStatus = (OpProjectStatus) broker.getObject(OpProjectStatus.class, oldStatus.getId());
            if (oldStatus.getProjects().size() == 0) {

               ArrayList project_status_ids = new ArrayList();
               project_status_ids.add(oldStatus.locator());
               OpProjectStatusService.doDeleteProjectStatus(project_status_ids, broker);
            }
         }

         List<OpProjectPlanVersion> versionsToDelete = updateProjectPlanVersions(session, broker, projectPlan, versionDataSet);

//         transaction.commit();
         
         for (OpProjectPlanVersion version : versionsToDelete) {
//            transaction = broker.newTransaction();
            version = (OpProjectPlanVersion) broker.getObject(OpProjectPlanVersion.class, version.getId());
            // rather weird: why do I need this check? A foreign key constraint should trigger an error anyway ?!?
            OpActivityVersionDataSetFactory.deleteProjectPlanVersion(broker, version);
            version.setProjectPlan(null);
            projectPlan.getVersions().remove(version);

//            transaction.commit();
         }

         //if project was archived and is currently saved in the session clear it
         if (session.getVariable(OpProjectConstants.PROJECT_ID) != null) {
            String storedProjectLocator = OpLocator.parseLocator((String) session.getVariable(OpProjectConstants.PROJECT_ID)).toString();
            if (project.getArchived() && storedProjectLocator.equalsIgnoreCase(project.locator())) {
               session.setVariable(OpProjectConstants.PROJECT_ID, null);
            }
         }
         logger.debug("/OpProjectAdministrationService.updateProject()");
         transaction.commit();
      }
      finally {
         OpTransactionLock.getInstance().unlock(id_string);
         finalizeSession(transaction, broker);
      }

      return null;
   }

   private boolean checkPermissionsForChange(OpProjectSession session,
         XMessage reply, byte projectPermission, byte permissions, boolean changed,
         int errorCode) {
      XError error;
      if ((projectPermission < permissions) && changed) {
         error = session.newError(ERROR_MAP, errorCode);
         reply.setError(error);
         return false;
      }
      return true;
   }

   /**
    * Updates additional parts of the project
    *
    * @param broker        - the <code>OpBroker</code> needed to perform the DB operations.
    * @param session       - the <code>OpProjectSession</code> object.
    * @param request       - the <code>XMessage</code> object representing the request.
    * @param project_data  - the <code>Map</code> containing project information.
    * @param project       - the <code>OpProjectNode</code> objectt representing the project.
    * @param permissionSet - an <code>XComponent</code> representing the permission set.
    * @return an <code>XMessage</code> which contains the error if an error occured or an empty XMessage otherwise.
    */
   protected XMessage updateProjectAdditional(OpProjectSession session, OpBroker broker,
        XMessage request, Map project_data, OpProjectNode project, XComponent permissionSet) {
      return new XMessage();
   }

   /**
    * Checks if after an update, the permission for a user which is editing the project has
    * been remvoed for that project.
    *
    * @param session  a <code>Set(OpLock)</code> the locks on a project.
    * @param broker a <code>XComponent(DATA_SET)</code> the client permissions
    *                      set.
    * @param project       a <code>OpProjectSession</code> the server session.
    * @return a <code>XError</code> if the permissions for an editing user has been removed
    *         or <code>null</code> otherwise.
    */
   private XError checkChangingPermissionForEditingUser(OpProjectSession session, OpBroker broker,
        OpProjectNode project) {
      Set<OpLock> locks = project.getLocks();
      if (!OpEnvironmentManager.isMultiUser() || locks.size() == 0) {
         return null;
      }
      for (OpLock lock : locks) {
         if (!session.checkAccessLevel(broker, project, lock.getOwner(), OpPermission.MANAGER)) {
            return session.newError(ERROR_MAP, OpProjectError.CANNOT_REMOVE_PERMISSION_ERROR);
         }
      }
      return null;
   }

   private XMessage updateGoals(OpProjectSession session, OpBroker broker,
        OpProjectNode project, XComponent goalsDataSet) {
      //the reply message
      XMessage reply = new XMessage();

      Map<String, OpGoal> goal_map = new HashMap<String, OpGoal>();
      Iterator goals = project.getGoals().iterator();
      OpGoal goal;
      while (goals.hasNext()) {
         goal = (OpGoal) (goals.next());
         goal_map.put(goal.locator(), goal);
      }

      XComponent data_row;
      XComponent data_cell;
      int i;
      boolean updated;
      for (i = 0; i < goalsDataSet.getChildCount(); i++) {
         data_row = (XComponent) (goalsDataSet.getChild(i));
         goal = (OpGoal) (goal_map.remove(data_row.getStringValue()));
         if (goal != null) {
            // Compare values and update goal if values have changed
            updated = false;
            // completed data cell
            data_cell = (XComponent) (data_row.getChild(0));
            if (goal.getCompleted() != data_cell.getBooleanValue()) {
               goal.setCompleted(data_cell.getBooleanValue());
               updated = true;
            }
            // subject data cell
            data_cell = (XComponent) (data_row.getChild(1));
            if ((goal.getName() != null && !goal.getName().equals(data_cell.getStringValue())) ||
                 (goal.getName() == null && data_cell.getStringValue() != null)) {
               goal.setName(data_cell.getStringValue() == null ? "" : data_cell.getStringValue());
               updated = true;
            }
            // priority data cell
            data_cell = (XComponent) (data_row.getChild(2));
            if (goal.getPriority() != data_cell.getIntValue()) {
               if (data_cell.getIntValue() < 1 || data_cell.getIntValue() > 9) {
                  reply.setError(session.newError(ERROR_MAP, OpProjectError.GOAL_PRIORITY_ERROR));
                  return reply;
               }
               else {
                  goal.setPriority((byte) data_cell.getIntValue());
                  updated = true;
               }
            }
            // if anything is updated persist it
            if (updated) {
               broker.updateObject(goal);
            }
         }
         else {
            // Insert new goal
            goal = new OpGoal();
            goal.setProjectNode(project);
            // completed data cell
            data_cell = (XComponent) (data_row.getChild(0));
            goal.setCompleted(data_cell.getBooleanValue());
            // subject data cell
            data_cell = (XComponent) (data_row.getChild(1));
            goal.setName(data_cell.getStringValue() == null ? "" : data_cell.getStringValue());
            // priority data cell
            data_cell = (XComponent) (data_row.getChild(2));
            if (data_cell.getIntValue() < 1 || data_cell.getIntValue() > 9) {
               reply.setError(session.newError(ERROR_MAP, OpProjectError.GOAL_PRIORITY_ERROR));
               return reply;
            }
            else {
               goal.setPriority((byte) data_cell.getIntValue());
            }
            broker.makePersistent(goal);
         }
      }
      // Remove outdated goals from set and delete them
      Iterator locators = goal_map.keySet().iterator();
      Set goal_set = project.getGoals();
      while (locators.hasNext()) {
         goal = (OpGoal) (goal_map.get((String) (locators.next())));
         goal_set.remove(goal);
         broker.deleteObject(goal);
      }
      return reply;
   }

   /**
    * Updates the start & end dates of all the activities in a project plan, revalidating the project plan
    * as a result of a project start date being moved into the future.
    *
    * @param session    a <code>OpProjectSession</code> representing a server session.
    * @param project    a <code>OpProjectNode</code> representing the project node being edited.
    * @param start_date a <code>Date</code> representing the
    * @param originalStartDate 
    * @return a <code>XMessage</code> indicating whether the operation was successfull or not.
    */
   private XMessage shiftPlanDates(OpProjectSession session, OpBroker broker, OpProjectNode project, Date start_date, Date originalStartDate) {
      OpProjectPlanningService planningService = (OpProjectPlanningService) XServiceManager.getService("PlanningService");
      if (planningService == null) {
         throw new UnsupportedOperationException("Cannot retrieve the registered project planning service !");
      }
      return planningService.moveProjectPlanStartDate(session, broker, project.getPlan(), start_date, originalStartDate);
   }

   /**
    * Updates the already existent project assignments for the modified project.
    *
    * @param session              <code>OpProjectSession</code> the current session
    * @param broker               <code>OpBroker</code> used for performing business operations.
    * @param project              <code>OpProjectNode</code> representing the project which was edited.
    * @param assignedResourcesSet <code>XComponent</code > data set representing assignment locators and the assignment internal and external rates.
    * @return <code>XMessage</code>
    */
   protected XMessage updateProjectAssignments(OpProjectSession session, OpBroker broker, OpProjectNode project, XComponent assignedResourcesSet) {
      //the reply message
      XMessage reply = new XMessage();
      Iterator projectNodeAssignments = project.getAssignments().iterator();
      // map of project assignments <resource Locator , projectNodeAssignment entity>
      Map<String, OpProjectNodeAssignment> assignmentNodeMap = new HashMap<String, OpProjectNodeAssignment>();
      //map of responsible user id for the resource
      Map<String, Long> responsibleUsersMap = new HashMap<String, Long>();
      while (projectNodeAssignments.hasNext()) {
         OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) projectNodeAssignments.next();
         OpResource resource = assignment.getResource();
         assignmentNodeMap.put(resource.locator(), assignment);
         OpUser user = resource.getUser();
         if (user != null) {
            responsibleUsersMap.put(resource.locator(), new Long(user.getId()));
         }
      }

      if (assignedResourcesSet != null && assignedResourcesSet.getChildCount() > 0) {
         Double internalRate;
         Double externalRate;
         for (int i = 0; i < assignedResourcesSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) assignedResourcesSet.getChild(i);
            String resourceChoice = dataRow.getStringValue();
            String resourceLocator = OpLocator.parseLocator(resourceChoice).toString();
            internalRate = null;
            externalRate = null;

            //3 - internal rate
            if (((XComponent) dataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX)).getValue() != null) {
               internalRate = ((XComponent) dataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX)).getDoubleValue();

               // check valid internal rate/resource/project
               if (internalRate < 0) {
                  reply.setError(session.newError(ERROR_MAP, OpProjectError.INTERNAL_RATE_NOT_VALID));
                  return reply;
               }
            }
            //4 - external rate
            if (((XComponent) dataRow.getChild(EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).getValue() != null) {
               externalRate = ((XComponent) dataRow.getChild(EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).getDoubleValue();

               // check valid external rate/resource/project
               if (externalRate < 0) {
                  reply.setError(session.newError(ERROR_MAP, OpProjectError.EXTERNAL_RATE_NOT_VALID));
                  return reply;
               }
            }

            OpResource resource = (OpResource) broker.getObject(resourceLocator);

            if (!assignmentNodeMap.containsKey(resourceLocator)) {
               //a new assignment was added
               OpProjectNodeAssignment assignment = new OpProjectNodeAssignment();
               assignment.setResource(resource);
               assignment.setProjectNode(project);
               assignment.setHourlyRate(internalRate);
               assignment.setExternalRate(externalRate);

               broker.makePersistent(assignment);
               insertContributorPermission(broker, project, resource);
            }
            else {
               //update existing assignments
               OpProjectNodeAssignment assignment = assignmentNodeMap.get(resourceLocator);
               assignment.setResource(resource);
               assignment.setProjectNode(project);
               assignment.setHourlyRate(internalRate);
               assignment.setExternalRate(externalRate);

               broker.updateObject(assignment);

               assignmentNodeMap.remove(resourceLocator);
               responsibleUsersMap.remove(resourceLocator);
            }
         }
      }

      // Remove outdated project node assignments if no activity assignments exist for the resource
      reply = deleteOutdatedProjectAssignments(session, broker, assignmentNodeMap);
      if (reply.getError() != null) {
         return reply;
      }

      // Remove auto-added (i.e., system-managed) contributor permissions for project node and resources
      deleteContributorPermissions(broker, project, responsibleUsersMap);

      return reply;
   }

   /**
    * Deletes the project node assignments if no activity assignments exist for the resource
    *
    * @param session
    * @param broker
    * @param assignmentNodeMap - map of project assignments <resource Locator , projectNodeAssignment entity>
    * @return an <code>XMessage</code> object containing the error code in case of an error
    */
   protected XMessage deleteOutdatedProjectAssignments(OpProjectSession session, OpBroker broker, Map<String, OpProjectNodeAssignment> assignmentNodeMap) {
      //the reply message
      XMessage reply = new XMessage();

      for (OpProjectNodeAssignment assignment : assignmentNodeMap.values()) {
         OpResource resource = assignment.getResource();
         OpProjectPlan projectPlan = assignment.getProjectNode().getPlan();
         XMessage checkUsageReply = OpResourceService.checkResourceUsageOnProjectPlan(session, broker, resource, projectPlan);
         if (checkUsageReply.getError() != null) {
            reply.setError(checkUsageReply.getError());
            return reply;
         }
         broker.deleteObject(assignment);
      }

      return reply;
   }

   /**
    * Deletes the auto-added (i.e., system-managed) contributor permissions for project node and resources
    *
    * @param broker
    * @param project
    * @param responsibleUsersMap - map of responsible user id for the resource
    */
   protected void deleteContributorPermissions(OpBroker broker, OpProjectNode project, Map<String, Long> responsibleUsersMap) {
      Collection userIds = responsibleUsersMap.values();
      if (userIds.isEmpty()) {
         return;
      }
      OpQuery query = broker.newQuery("select permission from OpPermission as permission where permission.Object.id = :projectId and permission.Subject.id in (:userIds) and permission.SystemManaged = :systemManaged");
      query.setLong("projectId", project.getId());
      query.setCollection("userIds", userIds);
      query.setBoolean("systemManaged", true);
      for (Object o : broker.list(query)) {
         OpPermission permission = (OpPermission) o;
         //remove permission if there are no project node assignments for resources with the same responsible user
         OpSubject subject = permission.getSubject();
         if (OpTypeManager.getPrototypeForObject(subject).getName().equals(OpUser.USER)) {
            //list of resource ids for which the user is responsible
            query = broker.newQuery("select resource.id from OpUser user inner join user.Resources resource where user.id = :userId ");
            query.setLong("userId", subject.getId());
            List resourceIds = broker.list(query);

            if (resourceIds.isEmpty()) {
               return;
            }
            query = broker.newQuery("select count(assignment) from OpProjectNodeAssignment as assignment where assignment.Resource.id in (:resourceIds) and assignment.ProjectNode.id  = :projectId");
            query.setLong("projectId", project.getId());
            query.setCollection("resourceIds", resourceIds);
            Number counter = (Number) broker.iterate(query).next();
            if (counter.intValue() == 0) {
               if (permission.getObject() != null) {
                  permission.getObject().removePermission(permission);
               }
               if (permission.getSubject() != null) {
                  permission.getSubject().removeOwnedPermission(permission);
               }
               broker.deleteObject(permission);
            }
         }
      }
   }

   /**
    * Updates the versions of a project plan, by deleting the ones that were deleted by the client.
    *
    * @param session         a <code>OpProjectSession</code> the server session.
    * @param broker          a <code>OpBroker</code> used for performing business operations.
    * @param projectPlan     the project plan
    * @param versionsDataSet a <code>XComponent</code> representing the client side project plan versions.
    */
   private List<OpProjectPlanVersion> updateProjectPlanVersions(OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan, XComponent versionsDataSet) {
      Set<OpProjectPlanVersion> existingVersions = projectPlan.getVersions();
      List<OpProjectPlanVersion> versionsToDelete = new ArrayList<OpProjectPlanVersion>();
      if (existingVersions == null || existingVersions.size() == 0) {
         return versionsToDelete;
      }

      // create a map of the existing versions
      Map<String, OpProjectPlanVersion> existingVersionMap = new HashMap<String, OpProjectPlanVersion>(existingVersions.size());
      for (OpProjectPlanVersion existingVersion : existingVersions) {
         if (existingVersion.getVersionNumber() != OpProjectPlan.WORKING_VERSION_NUMBER) {
            String versionId = OpLocator.locatorString(existingVersion);
            existingVersionMap.put(versionId, existingVersion);
         }
      }

      this.updateExistingProjectVersionsMap(projectPlan, versionsDataSet, existingVersionMap, broker, session);

      // remove all the other versions in the map
      Collection<OpProjectPlanVersion> values = existingVersionMap.values();
      if (values.size() > 0) {
         for (OpProjectPlanVersion version : values) {
            if (version.getControllingSheets() == null || version.getControllingSheets().isEmpty()) {
//               version.setProjectPlan(null);
//               projectPlan.getVersions().remove(version);
               versionsToDelete.add(version);
            }
         }
      }
      return versionsToDelete;
   }


   /**
    * Updates the map of existing project versions.
    *
    * @param projectPlan        a <code>OpProjectPlan</code> the project plan.
    * @param versionsDataSet    a <code>XComponent(DATA_SET)</code> the client versions data-set.
    * @param existingVersionMap a <code>Map(String, OpProjectPlanVersion)</code> the already existent project plan versions.
    * @param broker             a <code>OpBroker</code> used for persistence operations.
    * @param session            a <code>OpProjectSession</code> the server session.
    */
   protected void updateExistingProjectVersionsMap(OpProjectPlan projectPlan, XComponent versionsDataSet, Map<String, OpProjectPlanVersion> existingVersionMap, OpBroker broker, OpProjectSession session) {
      // remove the existent ones from the map
      for (int i = 0; i < versionsDataSet.getChildCount(); i++) {
         XComponent row = (XComponent) versionsDataSet.getChild(i);
         String versionId = ((XComponent) row.getChild(0)).getStringValue();
         if (versionId.equals(String.valueOf(OpProjectPlan.WORKING_VERSION_NUMBER))) {
            continue;
         }
         existingVersionMap.remove(versionId);
      }
   }

   public XMessage deleteProjects(OpProjectSession session, XMessage request) {
      List<String> id_strings = (ArrayList) (request.getArgument(PROJECT_IDS));
      logger.debug("OpProjectAdministrationService.deleteProjects(): project_ids = " + id_strings);

      return deleteProjectNodes(session, id_strings);
   }

   protected XMessage deleteProjectNodes(OpProjectSession session,
         List<String> id_strings) {
      OpBroker broker = session.newBroker();
      try {
         XMessage reply = new XMessage();

         List<Long> projectIds = new ArrayList<Long>();
         for (String id_string : id_strings) {
            projectIds.add(OpLocator.parseLocator(id_string).getID());
         }
         OpQuery query = broker.newQuery("" +
         		"select " +
         		"  project.SuperNode.id " +
         		"from " +
         		"  OpProjectNode as project " +
         		"where " +
         		"  project.id in (:projectIds)");
         query.setCollection("projectIds", projectIds);
         List projectNodeIDs = broker.list(query);

         Set accessibleProjectNodeIDs = session.accessibleIds(broker, projectNodeIDs, OpPermission.MANAGER);
         if (accessibleProjectNodeIDs.size() == 0) {
            logger.warn("Manager access to portfolio " + projectNodeIDs + " denied");
            reply.setError(session.newError(ERROR_MAP, OpProjectError.MANAGER_ACCESS_DENIED));
            return reply;
         }

         OpTransaction t = broker.newTransaction();
         query = broker
              .newQuery("" +
              		"select " +
              		"  project " +
              		"from " +
              		"  OpProjectNode as project " +
              		"where " +
              		"  project.id in (:projectIds) and " +
              		"  project.SuperNode.id in (:accessiblePortfolioIds)");
         query.setCollection("projectIds", projectIds);
         query.setCollection("accessiblePortfolioIds", accessibleProjectNodeIDs);

         //check that there are no work-records for any of the actitivities in the project plan versions
         boolean allInvalid = true;
         boolean warningFound = false;

         List<OpProjectNode> projectsToDelete = new ArrayList<OpProjectNode>();
         List<Long> deletableProjectIds = new ArrayList<Long>();
         Iterator result = broker.iterate(query);
         while (result.hasNext()) {
            OpProjectNode project = (OpProjectNode) result.next();

            if (!deleteProjectNode(project, broker, session)) {
               warningFound = true;
            }
            else {
               allInvalid = false;
            }
         }

         t.commit();

         if (allInvalid) {
            reply.setError(session.newError(ERROR_MAP, OpProjectError.PROJECTS_STILL_REFERNCED_ERROR));
            return reply;
         }
         if (warningFound) {
            reply.setArgument("warning", Boolean.TRUE);
            reply.setError(session.newError(ERROR_MAP, OpProjectError.PROJECTS_STILL_REFERNCED_WARNING));
            return reply;
         }
      }
      finally {
         broker.close();
      }
      logger.debug("/OpProjectAdministrationService.deleteProjects()");
      return null;
   }

   /**
    * Advanced functionality done when projects are deleted. Default behavior is NoOp.
    *
    * @param broker              - the <code>OpBroker</code> object needed to perform the DB operations.
    * @param deletableProjectIds - the <code>List<Long></code> containing the ids of the projects that can be deleted.
    */
   protected void deleteProjectAditional(OpBroker broker, List<Long> deletableProjectIds) {
   }

   /**
    * Clears the project or portfolio selection from the project navigation tree.
    *
    * @param projectNode a <code>OpProjectNode</code> representing the project to delete.
    * @param session     a <code>OpProjectSession</code> representing the current server session.
    */
   protected void clearActiveProjectNodeSelection(OpProjectNode projectNode, OpProjectSession session) {
      String currentProjectId = (String) session.getVariable(PROJECT_ID);
      if (currentProjectId != null) {
         long projectId = OpLocator.parseLocator(currentProjectId).getID();
         if (projectNode.getId() == projectId) {
            session.setVariable(PROJECT_ID, null);
            Map projectNavigatorFormStateMap = session.getComponentStateMap("ProjectNavigatorForm");
            if (projectNavigatorFormStateMap != null) {
               projectNavigatorFormStateMap.put("ProjectNavigatorTree", null);
            }
         }
      }
   }

   /**
    * Checks whether a project has assignments or not.
    *
    * @param project a <code>OpProjectNode</code> representing a project.
    * @param broker  a <code>OpBroker</code> used for performing business operations.
    * @return a <code>boolean</code> indicating whether the project has work records.
    */
   public static boolean hasWorkRecords(OpProjectNode project, OpBroker broker) {
      OpQuery query = broker.newQuery("select count(workrecord) from OpProjectPlan projectPlan join projectPlan.Activities activity join activity.Assignments assignment join assignment.WorkRecords workrecord where projectPlan.id = ?");
      query.setLong(0, project.getPlan().getId());
      Number workRecordNr = (Number) broker.iterate(query).next();
      return (workRecordNr != null) && (workRecordNr.intValue() > 0);
   }

   public XMessage insertPortfolio(OpProjectSession session, XMessage request) {
      logger.debug("OpProjectAdministrationService.insertPortfolio()");
      HashMap portfolioData = (HashMap) (request.getArgument(PORTFOLIO_DATA));
      XMessage reply = new XMessage();

      OpProjectNode portfolio = new OpProjectNode();
      try {
         portfolio.fillProjectNode(portfolioData);
      }
      catch (OpEntityException e) {
         reply.setError(session.newError(ERROR_MAP, e.getErrorCode()));
         return reply;
      }

      OpBroker broker = session.newBroker();
      try {
         String superPortfolioLocator = (String) portfolioData.get("SuperPortfolioID");
         logger.debug("SuperPortfolioID='" + superPortfolioLocator + "'");
         OpProjectNode superPortfolio = null;
         if (superPortfolioLocator != null) {
            superPortfolio = (OpProjectNode) broker.getObject(superPortfolioLocator);
         }
         if (superPortfolio == null) {
            superPortfolio = OpProjectAdministrationService.findRootPortfolio(broker);
         }

         // Check manager access for super portfolio
         if (!session.checkAccessLevel(broker, superPortfolio.getId(), OpPermission.MANAGER)) {
            logger.warn("ERROR: Insert access to super portfolio denied; ID = " + superPortfolio.getId());
            reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
            return reply;
         }

         portfolio.setSuperNode(superPortfolio);

         // check if portfolio name is already used
         OpQuery query = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
         query.setString(0, portfolio.getName());
         Iterator groups = broker.iterate(query);
         if (groups.hasNext()) {
            XError error = session.newError(ERROR_MAP, OpProjectError.PORTFOLIO_NAME_ALREADY_USED);
            reply.setError(error);
            return reply;
         }
         try {
            insertProjectAdditional(session, broker, request, portfolioData, portfolio);
         }
         catch (XServiceException exc) {
            exc.append(reply);
            return reply;
         }

         OpTransaction t = broker.newTransaction();

         broker.makePersistent(portfolio);

         XComponent permission_set = (XComponent) portfolioData.get(OpPermissionDataSetFactory.PERMISSION_SET);
         XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, portfolio, permission_set);
         if (result != null) {
            reply.setError(result);
            return reply;
         }

         t.commit();

         logger.debug("/OpProjectAdministrationService.insertPortfolio()");
         return reply;
      }
      finally {
         broker.close();
      }
   }

   public XMessage updatePortfolio(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(PORTFOLIO_ID));
      logger.debug("OpProjectAdministrationService.updatePortfolio(): id = " + id_string);
      HashMap portfolioData = (HashMap) (request.getArgument(PORTFOLIO_DATA));

      XMessage reply = new XMessage();

      OpBroker broker = session.newBroker();
      try {
         OpProjectNode portfolio = (OpProjectNode) (broker.getObject(id_string));
         //check if the given is valid
         if (portfolio == null) {
            logger.warn("ERROR: Could not find object with ID " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpProjectError.PROJECT_NOT_FOUND));
            return reply;
         }
         // Check manager access
         if (!session.checkAccessLevel(broker, portfolio.getId(), OpPermission.MANAGER)) {
            logger.warn("ERROR: Udpate access to portfolio denied; ID = " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
            return reply;
         }

         boolean isRootPortfolio = findRootPortfolio(broker).getId() == portfolio.getId();

         if (!isRootPortfolio) {
            //set the fields from the request
            try {
               portfolio.fillProjectNode(portfolioData);
            }
            catch (OpEntityException e) {
               reply.setError(session.newError(ERROR_MAP, e.getErrorCode()));
               return reply;
            }

            // check if portfolio name is already used
            OpQuery query = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
            query.setString(0, portfolio.getName());
            Iterator portfolios = broker.iterate(query);
            while (portfolios.hasNext()) {
               OpProjectNode other = (OpProjectNode) portfolios.next();
               if (other.getId() != portfolio.getId()) {
                  XError error = session.newError(ERROR_MAP, OpProjectError.PORTFOLIO_NAME_ALREADY_USED);
                  reply.setError(error);
                  return reply;
               }
            }
         }

         try {
            updateProjectAdditional(session, broker, request, portfolioData, portfolio, null);
         }
         catch (XServiceException exc) {
            exc.append(reply);
            return reply;
         }

         OpTransaction t = broker.newTransaction();
         broker.updateObject(portfolio);

         XComponent permission_set = (XComponent) portfolioData.get(OpPermissionDataSetFactory.PERMISSION_SET);
         // Store permissions on portfolio's sub-elements if the ApplyPermissionsRecursively was checked
         if((Boolean) portfolioData.get(APPLY_PERMISSIONS_RECURSIVELY)) {
            reply = storePermissionSetRecursively(broker, session, portfolio, permission_set);
            if (reply.getError() != null) {
               return reply;
            }
         }
         else {
            XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, portfolio, permission_set);
            if (result != null) {
               reply.setError(result);
               return reply;
            }
         }
         
         t.commit();

         logger.debug("/OpProjectAdministrationService.updatePortfolio()");

         return reply;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Saves the permissions from the permissionSet on the <code>OpProjectNode</code> object passed as parameter and on
    *    all it's subnodes if the object is a portfolio.
    *
    * @param broker  a <code>OpBroker</code> used for performing business operations.
    * @param session     a <code>OpProjectSession</code> representing the current server session
    * @param object the <code>OpProjectNode</code> on which the permissions will be set and whose subnodes will also receive
    *    the same permissions
    * @param permissionSet a <code>XComponent(DATA_SET)</code> the client permissions set
    * @return a <code>XMessage</code> object containing an error if at some point the permissions could no be set on a
    *    project node or an empty <code>XMessage</code> if there were no errors.
    */
   private XMessage storePermissionSetRecursively(OpBroker broker, OpProjectSession session, OpProjectNode object, XComponent permissionSet) {
      XMessage reply = new XMessage();

      // Check manager access on the current element
      if (!session.checkAccessLevel(broker, object.getId(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Udpate access to OpProjectNode denied; ID = " + object.locator());
         reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, object, permissionSet);
      if (result != null) {
         reply.setError(result);
         return reply;
      }

      for (OpProjectNode subElement : object.getSubNodes()) {
         reply = storePermissionSetRecursively(broker, session, subElement, permissionSet);
         if (reply.getError() != null) {
            return reply;
         }
      }

      return reply;
   }

   public XMessage deletePortfolios(OpProjectSession session, XMessage request) {
      ArrayList id_strings = (ArrayList) (request.getArgument(PORTFOLIO_IDS));
      logger.debug("OpProjectAdministrationService.deletePortfolios(): portfolio_ids = " + id_strings);

      return deleteProjectNodes(session, id_strings);
   }

   public static final int REMOVABLE = 0;
   public static final int WORKSLIPS_EXIST = 1;
   public static final int TEMPLATE_IN_USE = 2;
   
   /**
    * Tries to delete a portfolio and all the projects under that portfolio which have no workrecords.
    *
    * @param portfolio a <code>OpProjectNode</code> representing a portfolio.
    * @param broker    a <code>OpBroker</code> used for performing business operations.
    * @param session   a <code>OpProjectSession</code> representing the current server session.
    * @return a <code>boolean</code> indicating whether any project with work records was found or not.
    */
   protected boolean deleteProjectNode(OpProjectNode node, OpBroker broker, OpProjectSession session) {

      boolean removable = true;
      if (node.getSubNodes() != null) {
         Iterator<OpProjectNode> snit = node.getSubNodes().iterator();
         while (snit.hasNext()) {
            OpProjectNode p = snit.next();
            if (deleteProjectNode(p, broker, session)) {
               snit.remove();
            }
            else {
               removable = false;
            }
         }
      }
      if (removable && isRemovable(session, broker, node)) {
         // one by one, otherwise it will not work...
         List<Long> deletableProjectIds = new ArrayList<Long>();
         deletableProjectIds.add(new Long(node.getId()));
         deleteProjectAditional(broker, deletableProjectIds);
         
         clearActiveProjectNodeSelection(node, session);

         for (OpReport report : node.getReports()) {
            report.setProject(null);
         }
         broker.deleteObject(node);
         return true;
      }
      return false;
   }
   
   public boolean isRemovable(OpProjectSession session, OpBroker broker, OpProjectNode node) {
      boolean removable = false;
      switch (node.getType()) {
      case OpProjectNode.PORTFOLIO:
         removable = node.getSubNodes() == null || node.getSubNodes().isEmpty();
         break;
      case OpProjectNode.PROJECT:
         removable = !hasWorkRecords(node, broker);
         break;
      default:
         break;
      }
      return removable;
   }

   public XMessage moveProjectNode(OpProjectSession session, XMessage request) {
      List projectIds = (List) request.getArgument(PROJECT_IDS);
      String portfolioId = (String) request.getArgument(PORTFOLIO_ID);

      XMessage reply = new XMessage();

      if (projectIds == null || projectIds.isEmpty() || portfolioId == null) {
         return reply; //NOP
      }

      OpBroker broker = session.newBroker();
      try {
         OpTransaction tx = broker.newTransaction();
         //get the portfolio
         OpProjectNode portfolio = (OpProjectNode) broker.getObject(portfolioId);
         // check manager access for new selected portfolio
         if (!session.checkAccessLevel(broker, portfolio.getId(), OpPermission.MANAGER)) {
            logger.warn("Move access to portfolio denied; ID = " + portfolio.getId());
            reply.setError(session.newError(ERROR_MAP, OpProjectError.MANAGER_ACCESS_DENIED));
         }
         else {
            for (Iterator it = projectIds.iterator(); it.hasNext();) {
               String projectNodeId = (String) it.next();
               OpProjectNode projectNode = (OpProjectNode) broker.getObject(projectNodeId);

               // check manager access for project node portfolio
               if (!session.checkAccessLevel(broker, projectNode.getSuperNode().getId(), OpPermission.MANAGER)) {
                  logger.warn("Move access to portfolio denied; ID = " + projectNode.getSuperNode().getId());
                  reply.setError(session.newError(ERROR_MAP, OpProjectError.MANAGER_ACCESS_DENIED));
                  continue;
               }

               if (checkPortfolioAssignmentsForLoops(projectNode, portfolio)) {
                  reply.setError(session.newError(ERROR_MAP, OpProjectError.LOOP_ASSIGNMENT_ERROR));
                  continue;
               }

               projectNode.setSuperNode(portfolio);
               broker.updateObject(projectNode);
            }
         }

         tx.commit();
      }
      finally {
         broker.close();
      }
      return reply;
   }

   private boolean checkPortfolioAssignmentsForLoops(OpProjectNode projectNode, OpProjectNode portfolio) {
      if (projectNode.getId() == portfolio.getId()) {
         return true;
      }
      if (portfolio.getSuperNode() != null) {
         if (checkPortfolioAssignmentsForLoops(projectNode, portfolio.getSuperNode())) {
            return true;
         }
      }
      return false;
   }


   public static OpProjectNode findRootPortfolio(OpBroker broker) {
      return findProjectNode(broker, OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME, OpProjectNode.PORTFOLIO);
   }

   /**
    * Finds a project node witha given name and type.
    *
    * @param broker a <code>OpBroker</code> instance
    * @param name   the name of the project node
    * @param type   the type of the project node
    * @return an <code>OpProjectNode</code> instance or <code>null</code> if node not found.
    */
   public static OpProjectNode findProjectNode(OpBroker broker, String name, byte type) {
      OpQuery query = broker
           .newQuery("select node from OpProjectNode as node where node.Name = ? and node.Type = ?");
      query.setString(0, name);
      query.setByte(1, type);
      Iterator result = broker.iterate(query);
      if (result.hasNext()) {
         return (OpProjectNode) result.next();
      }
      else {
         return null;
      }
   }

   public static OpProjectNode createRootPortfolio(OpProjectSession session, OpBroker broker) {
      // Insert root project portfolio
      OpTransaction t = broker.newTransaction();
      OpProjectNode rootPortfolio = new OpProjectNode();
      rootPortfolio.setType(OpProjectNode.PORTFOLIO);
      rootPortfolio.setName(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME);
      rootPortfolio.setDescription(OpProjectNode.ROOT_PROJECT_PORTFOLIO_DESCRIPTION);
      broker.makePersistent(rootPortfolio);
      OpPermissionDataSetFactory.addSystemObjectPermissions(session, broker, rootPortfolio);
      t.commit();
      return rootPortfolio;
   }

   /**
    * 
    * @param session
    * @param broker
    * @param calendar
    * @param projectPlan
    * @param newProjectPlan
    * @return
    */
   protected XError copyProjectPlan(OpProjectSession session, OpBroker broker,
         OpProjectCalendar calendar, OpProjectPlan projectPlan,
         OpProjectPlan newProjectPlan, int attributesToSet, int attributesToRemove) {
      boolean independentCopy = newProjectPlan.getTemplate();
      // Get minimum activity start date from database (just to be sure)
      OpQuery query = broker
           .newQuery("select min(activity.Start) from OpActivity as activity where activity.ProjectPlan.id = ? and activity.Deleted = false and activity.Type != ?");
      query.setLong(0, projectPlan.getId());
      query.setByte(1, OpActivity.ADHOC_TASK);
      Iterator result = broker.iterate(query);
      if (!result.hasNext()) {
         return null;
      }
      Date start = (Date) result.next();

      Date newStart = newProjectPlan.getStart();
      if (independentCopy) {
         newStart = OpGanttValidator.getDefaultTemplateStart();
      }

      // Retrieve project plan for copying
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpActivityVersionDataSetFactory.getInstance()
            .retrieveActivityVersionDataSet(session, broker,
                  projectPlan.getLatestVersion(), dataSet, false,
                  attributesToSet, attributesToRemove);

      List<Integer> gaps = new ArrayList<Integer>();
      boolean isProgram = false;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         Integer gap = null;
         if (!isTaskActivity(dataRow)) {
            Date activityStart = OpGanttValidator.getStart(dataRow);
            gap = new Integer(calendar.countWorkDaysBetween(start, activityStart));
         }
         gaps.add(gap);
         if (OpGanttValidator.importedActivity(dataRow)) {
            isProgram = true;
         }
      }
      // TODO: check somewhere else!
      if (isProgram) {
         return session.newError(ERROR_MAP, OpProjectError.PROJECT_IS_PROGRAM_ERROR);
      }
      
      // Initialize GANTT validator
      OpGanttValidator validator = createValidator(session, broker, newProjectPlan);
      validator.setDataSet(dataSet);

      // Apply new start offset and remove resource assignments (for scheduled activities)
      XComponent dataRow;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if (!isTaskActivity(dataRow)) {
            //keep the same working days gap
            Date date = new Date(newStart.getTime());
            int gap = gaps.get(i) == null ? 0 : gaps.get(i).intValue();
            while (gap > 0) {
               date = calendar.nextWorkDay(date);
               gap--;
            }
            Date newActivityStart = new Date(date.getTime());
            OpGanttValidator.setStart(dataRow, newActivityStart);
            OpGanttValidator.setWorkPhases(dataRow, new TreeMap());
         }

         OpGanttValidator.setResources(dataRow, new ArrayList());
         OpGanttValidator.setResponsibleResource(dataRow, null);
         OpGanttValidator.setResourceBaseEfforts(dataRow, new HashMap());
         OpGanttValidator.setComplete(dataRow, 0);
         OpGanttValidator.setActualEffort(dataRow, 0);

         //duration must stay the same
         if (!isTaskActivity(dataRow)) {
            validator.updateDuration(dataRow, OpGanttValidator.getDuration(dataRow), true);
         }
      }

      // Validate copied and adjusted project plan
      validator.validateEntireDataSet();

      // Store activity data-set helper updates plan start/finish values and activity template flags
      try {
         // TODO: find a replacement... 
         // create new working version and check it in here:
         OpProjectPlanVersion tmpWorkingVersion = OpActivityVersionDataSetFactory
               .getInstance()
               .createProjectPlanVersionObject(session, broker, newProjectPlan,
                     null, OpProjectPlan.INITIAL_VERSION_NUMBER);
         
        OpProjectNode pn = projectPlan.getProjectNode();
		  byte accessLevel = session.effectiveAccessLevel(broker, pn.getId());
        OpProjectPlanVersion srcPv = accessLevel < OpPermission.MANAGER
               || pn.getPlan().getWorkingVersion() == null ? pn.getPlan()
               .getLatestVersion() : pn.getPlan().getWorkingVersion();

         OpActivityVersionDataSetFactory.getInstance().storeActivityVersionDataSet(session, broker, dataSet, tmpWorkingVersion, new HashMap(), srcPv, true);
         OpActivityDataSetFactory.getInstance().checkInProjectPlan(session, broker, tmpWorkingVersion);
         newProjectPlan.addProjectPlanVersion(tmpWorkingVersion);
         newProjectPlan.setBaseVersion(tmpWorkingVersion);
         newProjectPlan.setLatestVersion(tmpWorkingVersion);
      }
      catch (XServiceException exc) {
         return exc.getError();
      }

      return null;
   }

   protected OpGanttValidator createValidator(OpProjectSession session,
         OpBroker broker, OpProjectPlan projectPlan) {
      OpGanttValidator validator = OpProjectPlanValidator.getInstance().createValidator(session, broker, projectPlan);
      return validator;
   }

   private static boolean isTaskActivity(XComponent dataRow) {
      return (OpGanttValidator.getType(dataRow) == OpGanttValidator.TASK) || (OpGanttValidator.getType(dataRow) == OpGanttValidator.COLLECTION_TASK);
   }

   /**
    * Inserts a <code>OpPermission.CONTRIBUTOR</code> system managed permission for the <code>projectNode</code>
    * if the <code>resource</code> has a repsonsible user.
    *
    * @param broker      <code>OpBroker</code> used for performing business operations.
    * @param resource    <code>OpResource</code> entity
    * @param projectNode <code>OpProjectNode</code> entity
    */
   public static void insertContributorPermission(OpBroker broker, OpProjectNode projectNode, OpResource resource) {

      OpUser user = resource.getUser();
      if (user != null) {
         //check for duplicate contributor permission
         String countQuery = "select count(permission) from OpPermission permission where permission.Object.id = :projectId " +
              "and permission.Subject.id = :subjectId and permission.AccessLevel = :accessLevel and permission.SystemManaged = :systemManaged";
         OpQuery query = broker.newQuery(countQuery);
         query.setLong("projectId", projectNode.getId());
         query.setLong("subjectId", user.getId());
         query.setByte("accessLevel", OpPermission.CONTRIBUTOR);
         query.setBoolean("systemManaged", true);
         Number counter = (Number) broker.iterate(query).next();
         if (counter.intValue() == 0) {
            OpPermission permission = new OpPermission(projectNode, user, OpPermission.CONTRIBUTOR);
            permission.setSystemManaged(true);
            broker.makePersistent(permission);
            user.addOwnedPermission(permission);
            projectNode.addPermission(permission);
         }
      }
   }

   /**
    * Expands a project node, for the project administration view.
    *
    * @param projectSession a <code>OpProjectSession</code> representing the application session.
    * @param request        a <code>XMessage</code> representing the client request.
    * @return a <code>XMessage</code> representing the server response.
    */
   public XMessage expandProjectNode(OpProjectSession projectSession, XMessage request) {
      XComponent dataRow = (XComponent) request.getArgument(PROJECT_ROW_PARAMETER);

      Collection filteredOutIds = (Collection) request.getArgument(OpProjectDataSetFactory.FILTERED_OUT_IDS);

      Integer requestedTypes = (Integer) request.getArgument(TYPES_PARAMETER);
      int types = (requestedTypes != null) ? requestedTypes.intValue() : OpProjectDataSetFactory.ALL_TYPES;

      Boolean requestedTabular = (Boolean) request.getArgument(TABULAR_PARAMETER);
      boolean tabular = (requestedTabular == null) || requestedTabular.booleanValue();

      List<XComponent> children = OpProjectDataSetFactory.retrieveProjectNodeChildren(projectSession, dataRow, types, tabular, filteredOutIds);

      addAdvancedProjectProperties(projectSession, children, tabular);

      XMessage reply = new XMessage();
      reply.setArgument(OpProjectConstants.CHILDREN, children);
      return reply;
   }

   /**
    * Expands a project node, for the project chooser view.
    *
    * @param projectSession a <code>OpProjectSession</code> representing the application session.
    * @param request        a <code>XMessage</code> representing the client request.
    * @return a <code>XMessage</code> representing the server response.
    */
   public XMessage expandProjectChooserNode(OpProjectSession projectSession, XMessage request) {
      XComponent dataRow = (XComponent) request.getArgument(PROJECT_ROW_PARAMETER);
      Collection filteredOutIds = (Collection) request.getArgument(OpProjectDataSetFactory.FILTERED_OUT_IDS);

      List children = OpProjectDataSetFactory.retrieveProjectNodeChildren(projectSession, dataRow, OpProjectDataSetFactory.ALL_TYPES, false, filteredOutIds);

      OpProjectDataSetFactory.enableNodes(request.getArgumentsMap(), children);

      XMessage reply = new XMessage();
      reply.setArgument(OpProjectConstants.CHILDREN, children);
      return reply;
   }

   /* (non-Javadoc)
   * @see onepoint.project.OpProjectService#getServiceImpl()
   */
   @Override
   public Object getServiceImpl() {
      return serviceImpl;
   }

   /**
    * Returns true if at least one project assignment had it's internal or external rate modified,
    * or false otherwise
    *
    * @param s
    * @param request
    * @return true if at least one project assignment had it's internal or external rate modified,
    *         or false otherwise
    */
   public XMessage checkModifiedRates(OpProjectSession s, XMessage request) {
      boolean hasAssignments = false;
      XMessage xMessage = new XMessage();
      XComponent newResourceSet = (XComponent) (request.getArgument(RESOURCE_SET));
      XComponent originalResourceSet = (XComponent) (request.getArgument(ORIGINAL_RESOURCE_SET));
      String projectID = (String) (request.getArgument(PROJECT_ID));
      XComponent originalDataRow;
      XComponent newDataRow;
      XComponent originalDataCell;
      XComponent newDataCell;
      OpBroker broker = s.newBroker();
      try {

         OpProjectNode project = (OpProjectNode) (broker.getObject(projectID));
         OpProjectNodeAssignment projectAssignment = null;

         Double newInternalRate = -1d;
         Double newExternalRate = -1d;

         //obtain all the modified assignments
         for (int i = 0; i < originalResourceSet.getChildCount(); i++) {
            originalDataRow = (XComponent) originalResourceSet.getChild(i);
            if (originalDataRow.getOutlineLevel() != 0) {
               continue;
            }

            boolean modified = false;
            for (int j = 0; j < newResourceSet.getChildCount(); j++) {
               newDataRow = (XComponent) newResourceSet.getChild(j);
               if (!(newDataRow.getOutlineLevel() == 0
                    && originalDataRow.getStringValue().equals(newDataRow.getStringValue()))) {
                  continue;
               }
               //check if any rates were changed
               originalDataCell = (XComponent) originalDataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX);
               newDataCell = (XComponent) newDataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX);
               if ((originalDataCell.getValue() != null && newDataCell.getValue() == null)
                    || (originalDataCell.getValue() == null && newDataCell.getValue() != null)
                    || (originalDataCell.getValue() != null && newDataCell.getValue() != null
                    && !originalDataCell.getValue().equals(newDataCell.getValue()))) {
                  modified = true;
               }
               originalDataCell = (XComponent) originalDataRow.getChild(EXTERNAL_PROJECT_RATE_COLUMN_INDEX);
               newDataCell = (XComponent) newDataRow.getChild(EXTERNAL_PROJECT_RATE_COLUMN_INDEX);
               if ((originalDataCell.getValue() != null && newDataCell.getValue() == null)
                    || (originalDataCell.getValue() == null && newDataCell.getValue() != null)
                    || (originalDataCell.getValue() != null && newDataCell.getValue() != null
                    && !originalDataCell.getValue().equals(newDataCell.getValue()))) {
                  modified = true;
               }

               if (!modified) {
                  continue;
               }

               //if we introduced new rates/resource/project
               if (((XComponent) newDataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX)).getValue() != null) {
                  newInternalRate = ((XComponent) newDataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX)).getDoubleValue();
                  newExternalRate = ((XComponent) newDataRow.getChild(EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).getDoubleValue();
               }
               //if we reseted the rates/resource/project
               else {
                  newInternalRate = ((XComponent) originalDataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX)).getDoubleValue();
                  newExternalRate = ((XComponent) originalDataRow.getChild(EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).getDoubleValue();
               }
               OpResource resource = (OpResource) (broker.getObject(originalDataRow.getStringValue()));
               for (OpProjectNodeAssignment resourceAssignment : resource.getProjectNodeAssignments()) {
                  for (OpProjectNodeAssignment projectNodeAssignment : project.getAssignments()) {
                     if (resourceAssignment.getId() == projectNodeAssignment.getId()) {
                        projectAssignment = projectNodeAssignment;
                        break;
                     }
                  }
               }

               if (!resource.getAssignmentVersions().isEmpty() && projectAssignment != null) {
                  for (OpAssignmentVersion assignmentVersion : resource.getAssignmentVersions()) {
                     if (assignmentVersion.getPlanVersion().getProjectPlan().getProjectNode().getId() ==
                          projectAssignment.getProjectNode().getId()) {
                        OpActivityVersion activityVersion = assignmentVersion.getActivityVersion();
                        //TODO: I think this should not happen, but got activityVersion == null (maybe a good hook, to try healing?)
                        //check if the resource's activity assignments are covered by hourly rates periods
                        if (!isPeriodCoveredByHourlyRatesPeriod(activityVersion.getStart(), activityVersion.getFinish(), newResourceSet)
                             //check if the rates have changed for this interval
                             && isRateDifferentForPeriod(activityVersion.getStart(), activityVersion.getFinish(), resource,
                             newInternalRate, newExternalRate)) {
                           hasAssignments = true;
                           xMessage.setArgument(HAS_ASSIGNMENTS, Boolean.valueOf(hasAssignments));
                           return xMessage;
                        }
                     }
                  }
               }

               if (!resource.getActivityAssignments().isEmpty() && projectAssignment != null) {
                  for (OpAssignment assignment : resource.getActivityAssignments()) {
                     if (assignment.getProjectPlan().getProjectNode().getId() == projectAssignment.getProjectNode().getId()) {
                        OpActivityIfc activity = assignment.getActivity();
                        //check if the resource's activities are covered by hourly rates periods
                        if (!isPeriodCoveredByHourlyRatesPeriod(activity.getStart(), activity.getFinish(), newResourceSet)
                             //check if the rates have changed for this interval
                             && isRateDifferentForPeriod(activity.getStart(), activity.getFinish(), resource,
                             newInternalRate, newExternalRate)) {
                           hasAssignments = true;
                           xMessage.setArgument(HAS_ASSIGNMENTS, Boolean.valueOf(hasAssignments));
                           return xMessage;
                        }
                     }
                  }
               }
            }
         }
      }
      finally {
         broker.close();
      }
      xMessage.setArgument(HAS_ASSIGNMENTS, Boolean.valueOf(hasAssignments));
      return xMessage;
   }

   /**
    * Returns <code>true</code> if the period of time between start and end is completely covered by hourly
    * rates periods from the client's hourly rates periods data set
    *
    * @param start                       - the start of the checked interval
    * @param end                         - the end of the checked interval
    * @param clientHourlyRatesPeriodsSet - the set of hourly rates periods
    * @return - <code>true</code> if the period of time between start and end is completely covered by hourly
    *         rates periods from the client's hourly rates periods data set, and <code>false</code> otherwise
    */
   private boolean isPeriodCoveredByHourlyRatesPeriod(Date start, Date end, XComponent clientHourlyRatesPeriodsSet) {
      boolean covered = false;
      XComponent dataRow;
      List<Date> days = new ArrayList<Date>();

      Calendar calendar = OpProjectCalendar.setCalendarTimeToZero(start);
      Date dateStart = new Date(calendar.getTimeInMillis());
      calendar = OpProjectCalendar.setCalendarTimeToZero(end);
      Date dateEnd = new Date(calendar.getTimeInMillis());

      //obtain the list of days contained in the interval
      while (!dateEnd.before(dateStart)) {
         days.add(new Date(dateStart.getTime()));
         calendar = OpProjectCalendar.setCalendarTimeToZero(dateStart);
         calendar.add(Calendar.DATE, 1);
         dateStart = new Date(calendar.getTimeInMillis());
      }

      for (int i = 0; i < clientHourlyRatesPeriodsSet.getChildCount(); i++) {
         dataRow = (XComponent) clientHourlyRatesPeriodsSet.getChild(i);
         //check to see if the start date and end date are contained in this period's interval
         if (dataRow.getOutlineLevel() == 1) {
            calendar = OpProjectCalendar.setCalendarTimeToZero(((XComponent) dataRow.getChild(PERIOD_START_DATE)).getDateValue());
            dateStart = new Date(calendar.getTimeInMillis());
            calendar = OpProjectCalendar.setCalendarTimeToZero(((XComponent) dataRow.getChild(PERIOD_END_DATE)).getDateValue());
            dateEnd = new Date(calendar.getTimeInMillis());
            while (!dateEnd.before(dateStart)) {
               Date date = new Date(dateStart.getTime());
               if (days.contains(date)) {
                  days.remove(date);
               }
               calendar = OpProjectCalendar.setCalendarTimeToZero(dateStart);
               calendar.add(Calendar.DATE, 1);
               dateStart = new Date(calendar.getTimeInMillis());
            }
         }
      }
      if (days.isEmpty()) {
         covered = true;
      }
      return covered;
   }

   /**
    * Return <code>true</code> if in the specified period of time there is one day in which the resource has
    * a different rate then the ones passed as parameters, and <code>false</code> otherwise
    *
    * @param start           - the begining of the specified interval
    * @param end             - the end of the specified interval
    * @param resource        - the resource which has its rates compared to the new rates
    * @param newInternalRate - the new internal rate which is checked against the resource's internal rates form
    *                        the interval
    * @param newExternalRate - the new external rate which is checked against the resource's external rates form
    *                        the interval
    * @return - <code>true</code> if in the specified period of time there is one day in which the resource has
    *         a different rate then the ones passed as parameters, and <code>false</code> otherwise
    */
   private boolean isRateDifferentForPeriod(Date start, Date end, OpResource resource,
        Double newInternalRate, Double newExternalRate) {

      List<List> rates = resource.getRatesForInterval(start, end);
      List<Double> internalRates = rates.get(OpResource.INTERNAL_RATE_LIST_INDEX);
      List<Double> externalRates = rates.get(OpResource.EXTERNAL_RATE_LIST_INDEX);

      for (Double internalRate : internalRates) {
         if (!internalRate.equals(newInternalRate)) {
            return true;
         }
      }

      for (Double externalRate : externalRates) {
         if (!externalRate.equals(newExternalRate)) {
            return true;
         }
      }

      return false;
   }

   /**
    * Gets all the assignments for a given project from its project plan that is checked-out.
    *
    * @param broker        a <code>OpBroker</code> used for performing business operations.
    * @param projectPlanId a <code>long</code> representing the id of the project plan.
    * @return a <code>Iterator</code> over the working version assignments of the project plan.
    */
   private Iterator getAssignmentsForWorkingVersion(OpBroker broker, long projectPlanId) {
      StringBuffer queryString = new StringBuffer();
      queryString.append("select assignment from OpProjectPlanVersion planVersion inner join planVersion.AssignmentVersions assignment ");
      queryString.append("where assignment.ActivityVersion.PlanVersion.VersionNumber = ? and planVersion.ProjectPlan.id = ?");

      OpQuery query = broker.newQuery(queryString.toString());
      query.setInteger(0, OpProjectPlan.WORKING_VERSION_NUMBER);
      query.setLong(1, projectPlanId);

      return broker.iterate(query);
   }

   /**
    * Updates the base personnel costs of the checked out version of the project
    *
    * @param broker   - a <code>OpBroker</code> used for performing business operations.
    * @param calendar - the <code>OpProjectCalendar</code> needed to get the working days out of an interval of time
    * @param project  - the <code>OpProjectNode</code> representing the project that has been updated.
    */
   private boolean updatePersonnelCostsForWorkingVersion(OpBroker broker, OpProjectCalendar calendar, OpProjectNode project) {

      boolean updated = false;
      
      Iterator it = getAssignmentsForWorkingVersion(broker, project.getPlan().getId());

      OpActivityVersion activityVersion;
      List<List<Double>> ratesList;
      List<Double> internalRatesList;
      List<Double> externalRatesList;
      Double internalSum = 0d;
      Double externalSum = 0d;
      List<OpAssignmentVersion> updatedAssignments = new ArrayList<OpAssignmentVersion>();
      OpAssignmentVersion workingAssignmentVersion;
      List<Date> startEndList;
      double workHoursPerDay;
      List<Date> workingDays;
      OpProjectNodeAssignment projectNodeAssignment = new OpProjectNodeAssignment();

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

            //get the project node assignment for this assignment's resource
            for (OpProjectNodeAssignment resourceAssignment : workingAssignmentVersion.getResource().getProjectNodeAssignments()) {
               for (OpProjectNodeAssignment projectAssignment : project.getAssignments()) {
                  if (resourceAssignment.getId() == projectAssignment.getId()) {
                     projectNodeAssignment = projectAssignment;
                     break;
                  }
               }
            }

            ratesList = projectNodeAssignment.getRatesForListOfDays(workingDays);
            internalRatesList = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
            externalRatesList = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);

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
            updated = true;
         }
         internalSum = 0d;
         externalSum = 0d;
      }

      updated = OpActivityDataSetFactory.updateActivityVersionPersonnelCosts(broker, updatedAssignments) || updated;
      return updated;
   }

   /**
    * Updates the actual costs of the assignments and the activities that are checked in at the moment when the
    * project is updated.
    *
    * @param broker  a <code>OpBroker</code> used for performing business operations.
    * @param project a <code>OpProjectNode</code> representing the project that has been updated.
    */
   public static boolean updateActualCosts(OpBroker broker, OpProjectNode project) {
      
      Set<OpAssignment> assignments = project.getPlan().getActivityAssignments();
      return updateActualValuesForAssignments(broker, assignments);
   }

   public static boolean updateActualValuesForAssignments(OpBroker broker,
         Set<OpAssignment> assignments) {
      boolean updated = false;
      List<Double> ratesList;
      List<OpAssignment> updatedAssignments = new ArrayList<OpAssignment>();
      
      for (OpAssignment assignment : assignments) {
         Double internalSum = 0d;
         Double externalSum = 0d;
         Double newActualCosts = 0d;
         Double newActualProceeds = 0d;
         Double newLatestRemainingPersonellCosts = assignment.getRemainingPersonnelCosts();
         Double newLatestRemainingProceeds = assignment.getRemainingProceeds();
         Date currentLatestWRDate = OpGanttValidator.BEGINNING_OF_TIME;
         
         for (OpWorkRecord workRecord : assignment.getWorkRecords()) {

            //get the new rate of the project node assignment for the work record's day
            if (assignment.getProjectNodeAssignment() != null) {
               ratesList = assignment.getProjectNodeAssignment().getRatesForDay(workRecord.getWorkSlip().getDate(), true);
            }
            else {
               ratesList = assignment.getResource().getRatesForDay(workRecord.getWorkSlip().getDate());
            }
            newActualCosts = workRecord.getActualEffort() * ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
            newActualProceeds = workRecord.getActualEffort() * ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);

            //if the costs are different - update the workslip
            if (workRecord.getPersonnelCosts() != newActualCosts || workRecord.getActualProceeds() != newActualProceeds) {
               workRecord.setPersonnelCosts(newActualCosts);
               workRecord.setActualProceeds(newActualProceeds);
               if (currentLatestWRDate.before(workRecord.getWorkSlip().getDate()) && workRecord.getActualEffort() != 0d) {
                  currentLatestWRDate = workRecord.getWorkSlip().getDate();
                  newLatestRemainingPersonellCosts = workRecord.getRemainingEffort() * ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
                  newLatestRemainingProceeds = workRecord.getRemainingEffort() * ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
               }
               updated = true;
               broker.updateObject(assignment);
            }
            internalSum += workRecord.getPersonnelCosts();
            externalSum += workRecord.getActualProceeds();
         }
         if (assignment.getActualCosts() != internalSum
               || assignment.getActualProceeds() != externalSum
               || assignment.getRemainingPersonnelCosts() != newLatestRemainingPersonellCosts
               || assignment.getRemainingProceeds() != newLatestRemainingProceeds) {
            assignment.setActualCosts(internalSum);
            assignment.setActualProceeds(externalSum);
            assignment.setRemainingPersonnelCosts(newLatestRemainingPersonellCosts);
            assignment.setRemainingProceeds(newLatestRemainingProceeds);

            updated = true;
            broker.updateObject(assignment);
            updatedAssignments.add(assignment);
         }
      }

      OpActivityDataSetFactory.updateActivityActualCosts(broker, updatedAssignments);
      return updated;
   }

   /**
    * Fills the form data set with the resource hourly rates.
    * Each row has the resource locator as value set on it and a data cell with a map containing
    * the interval start date as key and a list with internal and external rates as value.
    *
    * @param project The current project.
    * @param dataSet Hourly rates data set.
    */
   public void fillHourlyRatesDataSet(OpProjectNode project, XComponent dataSet) {
      Set<OpProjectNodeAssignment> assignments = project.getAssignments();
      for (OpProjectNodeAssignment assignment : assignments) {
         OpResource resource = assignment.getResource();

         //fill the data set with the information from the map
         XComponent resourceRow = new XComponent(XComponent.DATA_ROW);
         resourceRow.setStringValue(resource.locator());
         Map<Date, List> sortedIntervals = new TreeMap<Date, List>();

         List<Double> rates = getDefaultHourlyRates(resource, assignment);
         sortedIntervals.put(project.getStart(), rates);

         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setValue(sortedIntervals);
         resourceRow.addChild(dataCell);
         dataSet.addChild(resourceRow);
      }
   }

   /**
    * Gets the default hourly rates for a resource, by choosing either from the hourly rates on the project assignment,
    * or  the hourly rates globally defined for the resource.
    *
    * @param resource   a <code>OpResource</code>.
    * @param assignment a <code>OpProjectNodeAssignment</code>
    * @return a <code>List</code> of [internal, external] rates.
    */
   protected List<Double> getDefaultHourlyRates(OpResource resource, OpProjectNodeAssignment assignment) {
      List<Double> rates = new ArrayList<Double>();
      if (assignment.getHourlyRate() != null) {
         rates.add(OpGanttValidator.INTERNAL_HOURLY_RATE_INDEX, assignment.getHourlyRate());
         rates.add(OpGanttValidator.EXTERNAL_HOURLY_RATE_INDEX, assignment.getExternalRate());
      }
      else {
         rates.add(OpGanttValidator.INTERNAL_HOURLY_RATE_INDEX, assignment.getResource().getHourlyRate());
         rates.add(OpGanttValidator.EXTERNAL_HOURLY_RATE_INDEX, assignment.getResource().getExternalRate());
      }
      return rates;
   }

   /**
    * Returns a <code>XMessage</code> containing a <code>Map<String, List<XComponent>></code>. The map contains for each
    * entry (which is a project locator) a list of data rows. The data rows represent all the projects located in the
    * subtree (from the whole project hierarchy) for which the project is the root.
    *
    * @param projectSession - the <code>OpProjectSession</code> object.
    * @param request        - the <code>XMessage</code> containing the project locators for which the subprojects are retreived.
    * @return a <code>XMessage</code> containing a <code>Map<String, List<XComponent>></code>. The map contains for each
    *         entry (which is a project locator) a list of data rows. The data rows represent all the projects located in the
    *         subtree (from the whole project hierarchy) for which the project is the root.
    */
   public XMessage loadAllRows(OpProjectSession projectSession, XMessage request) {
      Map<String, Integer> unexpandedProjectNodes = (Map<String, Integer>) request.getArgument(UNEXPANDED_NODES);
      Map<String, List<XComponent>> nodesMap = new HashMap<String, List<XComponent>>();
      OpLocator locator;
      if (unexpandedProjectNodes != null && !unexpandedProjectNodes.keySet().isEmpty()) {
         for (String projectLocator : unexpandedProjectNodes.keySet()) {
            locator = OpLocator.parseLocator(projectLocator);
            List<XComponent> rowsList = OpProjectDataSetFactory.getAllSubprojects(projectSession, locator.getID(),
                 unexpandedProjectNodes.get(projectLocator));
            addAdvancedProjectProperties(projectSession, rowsList, true);
            nodesMap.put(projectLocator, rowsList);
         }
      }

      XMessage reply = new XMessage();
      reply.setArgument(UNEXPANDED_NODES, nodesMap);
      return reply;
   }

   protected void addAdvancedProjectProperties(OpProjectSession projectSession, List<XComponent> children, boolean tabular) {
      // do nothing here, override this as required...
   }

}
