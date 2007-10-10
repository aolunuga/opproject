/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project.components.OpIncrementalValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.*;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceManager;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;


public class OpProjectAdministrationService extends OpProjectService {

   private static final XLog logger = XLogFactory.getServerLogger(OpProjectAdministrationService.class);

   private OpProjectAdministrationServiceImpl serviceImpl =
        new OpProjectAdministrationServiceImpl();

   public final static String PROJECT_DATA = "project_data";
   public final static String PROJECT_ID = "project_id";
   public final static String PROJECT_IDS = "project_ids";
   public final static String PORTFOLIO_DATA = "portfolio_data";
   public final static String PORTFOLIO_ID = "portfolio_id";
   public final static String PORTFOLIO_IDS = "portfolio_ids";
   public final static String GOALS_SET = "goals_set";
   public final static String TO_DOS_SET = "to_dos_set";
   public final static String ATTACHMENTS_LIST_SET = "attachments_list_set";
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


   public XMessage insertProject(OpProjectSession session, XMessage request) {

      // *** TODO: Add start-date of project (maybe also due-date?)
      logger.debug("OpProjectAdministrationService.insertProject()");
      HashMap project_data = (HashMap) (request.getArgument(PROJECT_DATA));

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

      // check if project name is already used
      OpQuery projectNameQuery = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
      projectNameQuery.setString(0, project.getName());
      Iterator projects = broker.iterate(projectNameQuery);
      if (projects.hasNext()) {
         error = session.newError(ERROR_MAP, OpProjectError.PROJECT_NAME_ALREADY_USED);
         reply.setError(error);
         broker.close();
         return reply;
      }

      String portfolioLocator = (String) project_data.get(PORTFOLIO_LOCATOR);
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
      project.setSuperNode(portfolio);

      // Check manager access for portfolio
      if (!session.checkAccessLevel(broker, project.getSuperNode().getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Insert access to portfolio denied; ID = " + project.getSuperNode().getID());
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      //project status
      String statusLocator = (String) project_data.get(OpProjectNode.STATUS);
      OpProjectStatus status = null;
      if (statusLocator != null && !statusLocator.equals(NULL_ID)) {
         status = (OpProjectStatus) (broker.getObject(statusLocator));
         project.setStatus(status);
      }

      OpTransaction t = broker.newTransaction();

      //insert project attachments
      XComponent attachmentsListSet = (XComponent) request.getArgument(ATTACHMENTS_LIST_SET);
      List<List> attachmentsList = (List) ((XComponent) attachmentsListSet.getChild(0).getChild(0)).getValue();
      insertAttachments(broker, project, attachmentsList);

      broker.makePersistent(project);

      // Insert project plan including settings
      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setHolidayCalendar(session.getCalendar().getHolidayCalendarId());
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

      broker.makePersistent(projectPlan);

      //allow a template to be set
      this.applyTemplate(broker, project_data, project, projectPlan, session.getCalendar());

      // Insert goals
      XComponent goalsDataSet = (XComponent) (request.getArgument(GOALS_SET));
      reply = insertGoals(session, broker, project, goalsDataSet);
      if (reply.getError() != null) {
         finalizeSession(t, broker);
         return reply;
      }

      // Insert to dos
      XComponent toDosDataSet = (XComponent) (request.getArgument(TO_DOS_SET));
      reply = insertToDos(session, broker, project, toDosDataSet);
      if (reply.getError() != null) {
         finalizeSession(t, broker);
         return reply;
      }

      //insert project assignments
      XComponent assignedResourcesSet = (XComponent) request.getArgument(RESOURCE_SET);
      reply = insertProjectAssignments(session, broker, project, assignedResourcesSet);
      if (reply.getError() != null) {
         finalizeSession(t, broker);
         return reply;
      }

      XComponent permission_set = (XComponent) project_data.get(OpPermissionDataSetFactory.PERMISSION_SET);
      XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, project, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }
      //copy the permissions from the projects to the attachments belonging to the project
      for (OpAttachment attachment : project.getAttachments()) {
         result = OpPermissionDataSetFactory.storePermissionSet(broker, session, attachment, permission_set);
         if (result != null) {
            reply.setError(result);
            broker.close();
            return reply;
         }
      }

      t.commit();
      logger.debug("/OpProjectAdministrationService.insertProject()");
      broker.close();
      return reply;
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
         goal.setName(data_cell.getStringValue());
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
    * Inserts the to dos related to the project passed as a parameter. In case of an error returns
    * an <code>XMessage</code> object containing the error code
    *
    * @param session
    * @param broker
    * @param project      - the project for which the to dos are inserted
    * @param toDosDataSet - the clients data set containing the information about the project to dos
    * @return - an <code>XMessage</code> object containing the error code in case of an error
    */
   private XMessage insertToDos(OpProjectSession session, OpBroker broker,
        OpProjectNode project, XComponent toDosDataSet) {
      //the reply message
      XMessage reply = new XMessage();
      OpToDo toDo;
      XComponent data_row;
      XComponent data_cell;

      for (int i = 0; i < toDosDataSet.getChildCount(); i++) {
         data_row = (XComponent) (toDosDataSet.getChild(i));
         toDo = new OpToDo();
         toDo.setProjectNode(project);
         data_cell = (XComponent) (data_row.getChild(0));
         toDo.setCompleted(data_cell.getBooleanValue());
         data_cell = (XComponent) (data_row.getChild(1));
         toDo.setName(data_cell.getStringValue());
         data_cell = (XComponent) (data_row.getChild(2));
         if (data_cell.getIntValue() < 1 || data_cell.getIntValue() > 9) {
            reply.setError(session.newError(ERROR_MAP, OpProjectError.TODO_PRIORITY_ERROR));
            return reply;
         }
         else {
            toDo.setPriority((byte) data_cell.getIntValue());
         }
         data_cell = (XComponent) (data_row.getChild(3));
         toDo.setDue(data_cell.getDateValue());
         broker.makePersistent(toDo);
      }
      return reply;
   }

   /**
    * Inserts the attachments related to the project passed as a parameter.
    *
    * @param broker
    * @param project         - the project for which the attachments are inserted
    * @param attachmentsList - the <code>List</code> containing the information about the project attachments
    */
   private void insertAttachments(OpBroker broker, OpProjectNode project, List<List> attachmentsList) {

      //set the attachments
      if (attachmentsList != null && !attachmentsList.isEmpty()) {
         Set<OpAttachment> attachments = new HashSet<OpAttachment>();
         for (List attachmentElement : attachmentsList) {
            OpAttachment attachment = OpActivityDataSetFactory.createAttachment(broker, null, null, attachmentElement, null, project);
            if (attachment != null) {
               attachments.add(attachment);
            }
         }
         project.setAttachments(attachments);
      }
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

      return reply;
   }

   /**
    * Template method for setting the template of a project. By default, doesn't do anything.
    *
    * @param broker       an <code>OpBroker</code> used for performing business operations.
    * @param project_data a <code>HashMap</code> representing the parameters.
    * @param project      a <code>OpProjectNode</code> entity representing a project node.
    * @param projectPlan  a <code>OpProjectPlan</code> entity representing a project plan.
    * @param calendar     Current calendar to be used for working days calculations
    */
   protected void applyTemplate(OpBroker broker, HashMap project_data, OpProjectNode project, OpProjectPlan projectPlan, XCalendar calendar) {
      //do nothing here
   }

   public XMessage updateProject(OpProjectSession session, XMessage request) {
      // *** TODO: Check for other fields that can be updated
      String id_string = (String) (request.getArgument(PROJECT_ID));
      logger.debug("OpProjectAdministrationService.updateProject(): id = " + id_string);
      HashMap project_data = (HashMap) (request.getArgument(PROJECT_DATA));

      XMessage reply = new XMessage();
      XError error;
      OpBroker broker = session.newBroker();
      OpTransaction transaction = null;

      try {
         OpProjectNode project = (OpProjectNode) (broker.getObject(id_string));

         if (project == null) {
            logger.warn("ERROR: Could not find object with ID " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpProjectError.PROJECT_NOT_FOUND));
            return reply;
         }

         // Check manager access
         if (!session.checkAccessLevel(broker, project.getID(), OpPermission.CONTRIBUTOR)) {
            logger.warn("ERROR: Udpate access to project denied; ID = " + id_string);
            reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
            return reply;
         }

         Date originalStartDate = project.getStart();

         try {
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
            if (other.getID() != project.getID()) {
               error = session.newError(ERROR_MAP, OpProjectError.PROJECT_NAME_ALREADY_USED);
               reply.setError(error);
               return reply;
            }
         }

         //check if the start date is in the future
         if (project.getStart().after(originalStartDate)) {
            //if its checked out, throw error
            if (project.getLocks().size() > 0) {
               error = session.newError(ERROR_MAP, OpProjectError.PROJECT_LOCKED_ERROR);
               reply.setError(error);
               return reply;
            }
            reply = this.shiftPlanDates(session, project, project.getStart());
            if (reply.getError() != null) {
               return reply;
            }
         }

         //project status
         String statusLocator = (String) project_data.get(OpProjectNode.STATUS);
         OpProjectStatus status = null;
         if (statusLocator != null && !statusLocator.equals(NULL_ID)) {
            status = (OpProjectStatus) (broker.getObject(statusLocator));
            project.setStatus(status);
         }
         else {
            project.setStatus(null);
         }

         OpProjectPlan projectPlan = project.getPlan();
         //check if the project plan has any activities and if not, update the start and end
         if (projectPlan.getActivities().size() == 0) {
            projectPlan.copyDatesFromProject();
         }

         //set the calculation mode
         Boolean calculationMode = (Boolean) project_data.get(OpProjectPlan.CALCULATION_MODE);
         if (calculationMode != null && !calculationMode) {
            projectPlan.setCalculationMode(OpProjectPlan.INDEPENDENT);
         }
         else {
            projectPlan.setCalculationMode(OpProjectPlan.EFFORT_BASED);
         }

         transaction = broker.newTransaction();

         broker.updateObject(projectPlan);
         broker.updateObject(project);

         // Update current goals
         XComponent goalsDataSet = (XComponent) (request.getArgument(GOALS_SET));
         reply = updateGoals(session, broker, project, goalsDataSet);
         if (reply.getError() != null) {
            return reply;
         }

         // Update current to dos
         XComponent toDosDataSet = (XComponent) (request.getArgument(TO_DOS_SET));
         reply = updateToDos(session, broker, project, toDosDataSet);
         if (reply.getError() != null) {
            return reply;
         }

         //Update attachments
         XComponent attachmentsListSet = (XComponent) request.getArgument(ATTACHMENTS_LIST_SET);
         List<List> attachmentsList = (List) ((XComponent) attachmentsListSet.getChild(0).getChild(0)).getValue();
         updateAttachments(broker, project, attachmentsList);

         // update project plan versions (must be done before deleting the versions)
         XComponent versionDataSet = (XComponent) request.getArgument(VERSIONS_SET);
         updateProjectPlanVersions(session, broker, projectPlan, versionDataSet);

         //update project assignments
         XComponent assignedResourcesSet = (XComponent) request.getArgument(RESOURCE_SET);
         reply = updateProjectAssignments(session, broker, project, assignedResourcesSet);
         if (reply.getError() != null) {
            return reply;
         }

         // update permissions
         XComponent permission_set = (XComponent) project_data.get(OpPermissionDataSetFactory.PERMISSION_SET);
         XError result = checkChangingPermissionForEditingUser(project.getLocks(), permission_set, session);
         if (result != null) {
            reply.setError(result);
            return reply;
         }
         result = OpPermissionDataSetFactory.storePermissionSet(broker, session, project, permission_set);
         if (result != null) {
            reply.setError(result);
            return reply;
         }

         //update permissions for the attachments belonging to this project
         for (OpAttachment attachment : project.getAttachments()) {
            result = OpPermissionDataSetFactory.storePermissionSet(broker, session, attachment, permission_set);
            if (result != null) {
               reply.setError(result);
               broker.close();
               return reply;
            }
         }

         XCalendar xCalendar = session.getCalendar();
         //update personnel & actual costs
         updatePersonnelCostsForWorkingVersion(broker, xCalendar, project);
         updateActualCosts(broker, project);

         transaction.commit();

         //if project was archived and is currently saved in the session clear it
         if (session.getVariable(OpProjectConstants.PROJECT_ID) != null) {
            String storedProjectLocator = OpLocator.parseLocator((String) session.getVariable(OpProjectConstants.PROJECT_ID)).toString();
            if (project.getArchived() && storedProjectLocator.equalsIgnoreCase(project.locator())) {
               session.setVariable(OpProjectConstants.PROJECT_ID, null);
            }
         }
         logger.debug("/OpProjectAdministrationService.updateProject()");
      }
      finally {
         finalizeSession(transaction, broker);
      }

      return null;
   }

   /**
    * Checks if after an update, the permission for a user which is editing the project has
    * been remvoed for that project.
    *
    * @param projectLocks  a <code>Set(OpLock)</code> the locks on a project.
    * @param permissionSet a <code>XComponent(DATA_SET)</code> the client permissions
    *                      set.
    * @param session       a <code>OpProjectSession</code> the server session.
    * @return a <code>XError</code> if the permissions for an editing user has been removed
    *         or <code>null</code> otherwise.
    */
   private XError checkChangingPermissionForEditingUser(Set<OpLock> projectLocks, XComponent permissionSet,
        OpProjectSession session) {
      if (!OpEnvironmentManager.isMultiUser() || projectLocks.size() == 0) {
         return null;
      }
      Map<String, Byte> permissionsMap = new HashMap<String, Byte>();
      byte accessValue = -1;
      for (int i = 0; i < permissionSet.getChildCount(); i++) {
         XComponent permissionsRow = (XComponent) permissionSet.getChild(i);
         if (permissionsRow.getOutlineLevel() == 0) {
            accessValue = ((XComponent) permissionsRow.getChild(OpPermissionDataSetFactory.ACCESS_LEVEL_COLUMN_INDEX)).getByteValue();
            continue;
         }
         String userLocator = XValidator.choiceID(permissionsRow.getStringValue());
         Byte previousAccessValue = permissionsMap.get(userLocator);
         byte value = (byte) (previousAccessValue != null ? Math.max(previousAccessValue, accessValue) : accessValue);
         permissionsMap.put(userLocator, value);
      }

      for (OpLock lock : projectLocks) {
         String lockOwner = lock.getOwner().locator();
         if (permissionsMap.get(lockOwner) == null || permissionsMap.get(lockOwner) < OpPermission.MANAGER) {
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
               goal.setName(data_cell.getStringValue());
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
            goal.setName(data_cell.getStringValue());
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

   private XMessage updateToDos(OpProjectSession session, OpBroker broker,
        OpProjectNode project, XComponent toDosDataSet) {
      //the reply message
      XMessage reply = new XMessage();

      Map<String, OpToDo> to_do_map = new HashMap<String, OpToDo>();
      Iterator to_dos = project.getToDos().iterator();
      OpToDo to_do;
      while (to_dos.hasNext()) {
         to_do = (OpToDo) (to_dos.next());
         to_do_map.put(to_do.locator(), to_do);
      }

      XComponent data_row;
      XComponent data_cell;
      boolean updated;

      for (int i = 0; i < toDosDataSet.getChildCount(); i++) {
         data_row = (XComponent) (toDosDataSet.getChild(i));
         to_do = (OpToDo) (to_do_map.remove(data_row.getStringValue()));
         if (to_do != null) {
            // Compare values and update to do if values have changed
            updated = false;
            // completed data cell
            data_cell = (XComponent) (data_row.getChild(0));
            if (to_do.getCompleted() != data_cell.getBooleanValue()) {
               to_do.setCompleted(data_cell.getBooleanValue());
               updated = true;
            }
            data_cell = (XComponent) (data_row.getChild(1));
            if ((to_do.getName() != null && !to_do.getName().equals(data_cell.getStringValue())) ||
                 (to_do.getName() == null && data_cell.getStringValue() != null)) {
               to_do.setName(data_cell.getStringValue());
               updated = true;
            }
            data_cell = (XComponent) (data_row.getChild(2));
            if (to_do.getPriority() != data_cell.getIntValue()) {
               if (data_cell.getIntValue() < 1 || data_cell.getIntValue() > 9) {
                  reply.setError(session.newError(ERROR_MAP, OpProjectError.TODO_PRIORITY_ERROR));
                  return reply;
               }
               else {
                  to_do.setPriority((byte) data_cell.getIntValue());
                  updated = true;
               }
            }
            data_cell = (XComponent) (data_row.getChild(3));
            if (to_do.getDue() != data_cell.getDateValue()) {
               if ((to_do.getDue() == null) || (data_cell.getDateValue() == null)
                    || (!to_do.getDue().equals(data_cell.getDateValue()))) {
                  to_do.setDue(data_cell.getDateValue());
                  updated = true;
               }
            }
            if (updated) {
               broker.updateObject(to_do);
            }
         }
         else {
            // Insert new to do
            to_do = new OpToDo();
            to_do.setProjectNode(project);
            data_cell = (XComponent) (data_row.getChild(0));
            to_do.setCompleted(data_cell.getBooleanValue());
            data_cell = (XComponent) (data_row.getChild(1));
            to_do.setName(data_cell.getStringValue());
            data_cell = (XComponent) (data_row.getChild(2));
            if (data_cell.getIntValue() < 1 || data_cell.getIntValue() > 9) {
               reply.setError(session.newError(ERROR_MAP, OpProjectError.TODO_PRIORITY_ERROR));
               return reply;
            }
            else {
               to_do.setPriority((byte) data_cell.getIntValue());

            }
            data_cell = (XComponent) (data_row.getChild(3));
            to_do.setDue(data_cell.getDateValue());
            broker.makePersistent(to_do);
         }
      }
      // Remove outdated to dos from set and delete them
      Iterator locators = to_do_map.keySet().iterator();
      Set to_do_set = project.getToDos();
      while (locators.hasNext()) {
         to_do = (OpToDo) (to_do_map.get((String) (locators.next())));
         to_do_set.remove(to_do);
         broker.deleteObject(to_do);
      }
      return reply;
   }

   /**
    * Update the attachments that belong to this <code>OpProjectNode</code> entity.
    *
    * @param broker          - the <code>OpBroker</code> needed to persist the attachments and contents.
    * @param project         - the <code>OpProjectNode</code> for which the attachments are updated.
    * @param attachmentsList - the <code>List</code> which contains the information about the attachments
    *                        received from the client.
    */
   private void updateAttachments(OpBroker broker, OpProjectNode project, List<List> attachmentsList) {
      //delete all attachments and decrement their content reference number
      Iterator it = project.getAttachments().iterator();
      while (it.hasNext()) {
         OpAttachment attachment = (OpAttachment) it.next();
         if (!attachment.getLinked()) {
            OpContentManager.updateContent(attachment.getContent(), broker, false, false);
            attachment.setContent(null);
         }
         it.remove();
         broker.deleteObject(attachment);
      }

      //create new attachments from the client's attachment list
      if (attachmentsList != null && !attachmentsList.isEmpty()) {
         Set<OpAttachment> attachments = new HashSet<OpAttachment>();
         for (List attachmentElement : attachmentsList) {
            OpAttachment attachment = OpActivityDataSetFactory.createAttachment(broker, null, null, attachmentElement, null, project);
            if (attachment != null) {
               attachments.add(attachment);
            }
         }
         project.setAttachments(attachments);
      }

      //delete all contents with reference count = 0
      OpContentManager.deleteZeroRefContents(broker);
   }

   /**
    * Updates the start & end dates of all the activities in a project plan, revalidating the project plan
    * as a result of a project start date being moved into the future.
    *
    * @param session    a <code>OpProjectSession</code> representing a server session.
    * @param project    a <code>OpProjectNode</code> representing the project node being edited.
    * @param start_date a <code>Date</code> representing the
    * @return a <code>XMessage</code> indicating whether the operation was successfull or not.
    */
   private XMessage shiftPlanDates(OpProjectSession session, OpProjectNode project, Date start_date) {
      XService planningService = XServiceManager.getService("PlanningService");
      if (planningService == null) {
         throw new UnsupportedOperationException("Cannot retrieve the registered project planning service !");
      }
      XMessage moveRequest = new XMessage();
      moveRequest.setArgument("projectPlan", project.getPlan());
      moveRequest.setArgument("newDate", start_date);
      return planningService.invokeMethod(session, "moveProjectPlanStartDate", moveRequest);
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
            responsibleUsersMap.put(resource.locator(), new Long(user.getID()));
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
      OpQuery query = broker.newQuery("select permission from OpPermission as permission where permission.Object.ID = :projectId and permission.Subject.ID in (:userIds) and permission.SystemManaged = :systemManaged");
      query.setLong("projectId", project.getID());
      query.setCollection("userIds", userIds);
      query.setBoolean("systemManaged", true);
      for (Object o : broker.list(query)) {
         OpPermission permission = (OpPermission) o;
         //remove permission if there are no project node assignments for resources with the same responsible user
         OpSubject subject = permission.getSubject();
         if (OpTypeManager.getPrototypeForObject(subject).getName().equals(OpUser.USER)) {
            //list of resource ids for which the user is responsible
            query = broker.newQuery("select resource.ID from OpUser user inner join user.Resources resource where user.ID = :userId ");
            query.setLong("userId", subject.getID());
            List resourceIds = broker.list(query);

            if (resourceIds.isEmpty()) {
               return;
            }
            query = broker.newQuery("select count(assignment) from OpProjectNodeAssignment as assignment where assignment.Resource.ID in (:resourceIds) and assignment.ProjectNode.ID  = :projectId");
            query.setLong("projectId", project.getID());
            query.setCollection("resourceIds", resourceIds);
            Number counter = (Number) broker.iterate(query).next();
            if (counter.intValue() == 0) {
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
   private void updateProjectPlanVersions(OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan, XComponent versionsDataSet) {
      Set<OpProjectPlanVersion> existingVersions = projectPlan.getVersions();
      if (existingVersions == null || existingVersions.size() == 0) {
         return;
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
            OpActivityVersionDataSetFactory.deleteProjectPlanVersion(broker, version);
         }
      }
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

      OpBroker broker = session.newBroker();
      XMessage reply = new XMessage();

      List<Long> projectIds = new ArrayList<Long>();
      for (String id_string : id_strings) {
         projectIds.add(OpLocator.parseLocator(id_string).getID());
      }
      OpQuery query = broker.newQuery("select project.SuperNode.ID from OpProjectNode as project where project.ID in (:projectIds) and project.Type = (:projectType)");
      query.setCollection("projectIds", projectIds);
      query.setByte("projectType", OpProjectNode.PROJECT);
      List portfolioIds = broker.list(query);

      Set accessiblePortfolioIds = session.accessibleIds(broker, portfolioIds, OpPermission.MANAGER);
      if (accessiblePortfolioIds.size() == 0) {
         logger.warn("Manager access to portfolio " + portfolioIds + " denied");
         reply.setError(session.newError(ERROR_MAP, OpProjectError.MANAGER_ACCESS_DENIED));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();
      /*
       * --- Not yet support by Hibernate (delete query against joined-subclass) query = broker.newQuery("delete from
       * XProject where XProject.ID in (:projectIds) and XProject.Portfolio.ID in :(accessiblePortfolioIds)");
       * broker.execute(query);
       */
      query = broker
           .newQuery("select project from OpProjectNode as project where project.ID in (:projectIds) and project.SuperNode.ID in (:accessiblePortfolioIds)");
      query.setCollection("projectIds", projectIds);
      query.setCollection("accessiblePortfolioIds", accessiblePortfolioIds);

      //check that there are no work-records for any of the actitivities in the project plan versions
      boolean allInvalid = true;
      boolean warningFound = false;

      Iterator result = broker.iterate(query);
      while (result.hasNext()) {
         boolean canDelete = true;
         OpProjectNode project = (OpProjectNode) result.next();

         if (hasWorkRecords(project, broker)) {
            warningFound = true;
            canDelete = false;
         }
         else {
            allInvalid = false;
         }

         //check if we are deleting an active project
         clearActiveProjectNodeSelection(project, session);

         if (canDelete) {
            //manage the contents of the attachments belonging to this project
            OpAttachmentDataSetFactory.removeContents(broker, project.getAttachments());
            broker.deleteObject(project);
         }
      }
      t.commit();

      if (allInvalid) {
         reply.setError(session.newError(ERROR_MAP, OpProjectError.WORKRECORDS_STILL_EXIST_ERROR));
         broker.close();
         return reply;
      }
      if (warningFound) {
         reply.setArgument("warning", Boolean.TRUE);
         reply.setError(session.newError(ERROR_MAP, OpProjectError.WORKRECORDS_STILL_EXIST_WARNING));
         broker.close();
         return reply;
      }

      //if (accessiblePortfolioIds.size() < portfolioIds.size())
      // TODO: Return ("informative") error if notAllAccessible

      broker.close();
      logger.debug("/OpProjectAdministrationService.deleteProjects()");
      return null;
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
         if (projectNode.getID() == projectId) {
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
      OpQuery query = broker.newQuery("select count(workrecord) from OpProjectPlan projectPlan join projectPlan.Activities activity join activity.Assignments assignment join assignment.WorkRecords workrecord where projectPlan.ID = ?");
      query.setLong(0, project.getPlan().getID());
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
      if (!session.checkAccessLevel(broker, superPortfolio.getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Insert access to super portfolio denied; ID = " + superPortfolio.getID());
         broker.close();
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
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      broker.makePersistent(portfolio);

      XComponent permission_set = (XComponent) portfolioData.get(OpPermissionDataSetFactory.PERMISSION_SET);
      XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, portfolio, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }

      t.commit();

      logger.debug("/OpProjectAdministrationService.insertPortfolio()");
      broker.close();
      return reply;
   }

   public XMessage updatePortfolio(OpProjectSession session, XMessage request) {
      String id_string = (String) (request.getArgument(PORTFOLIO_ID));
      logger.debug("OpProjectAdministrationService.updatePortfolio(): id = " + id_string);
      HashMap portfolioData = (HashMap) (request.getArgument(PORTFOLIO_DATA));

      XMessage reply = new XMessage();

      OpBroker broker = session.newBroker();

      OpProjectNode portfolio = (OpProjectNode) (broker.getObject(id_string));
      //check if the given is valid
      if (portfolio == null) {
         logger.warn("ERROR: Could not find object with ID " + id_string);
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpProjectError.PROJECT_NOT_FOUND));
         return reply;
      }
      // Check manager access
      if (!session.checkAccessLevel(broker, portfolio.getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Udpate access to portfolio denied; ID = " + id_string);
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      boolean isRootPortfolio = findRootPortfolio(broker).getID() == portfolio.getID();

      if (!isRootPortfolio) {
         //set the fields from the request
         try {
            portfolio.fillProjectNode(portfolioData);
         }
         catch (OpEntityException e) {
            reply.setError(session.newError(ERROR_MAP, e.getErrorCode()));
            broker.close();
            return reply;
         }

         // check if portfolio name is already used
         OpQuery query = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
         query.setString(0, portfolio.getName());
         Iterator portfolios = broker.iterate(query);
         while (portfolios.hasNext()) {
            OpProjectNode other = (OpProjectNode) portfolios.next();
            if (other.getID() != portfolio.getID()) {
               XError error = session.newError(ERROR_MAP, OpProjectError.PORTFOLIO_NAME_ALREADY_USED);
               reply.setError(error);
               broker.close();
               return reply;
            }
         }
      }

      OpTransaction t = broker.newTransaction();
      broker.updateObject(portfolio);

      XComponent permission_set = (XComponent) portfolioData.get(OpPermissionDataSetFactory.PERMISSION_SET);
      XError result = OpPermissionDataSetFactory.storePermissionSet(broker, session, portfolio, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }
      t.commit();

      logger.debug("/OpProjectAdministrationService.updatePortfolio()");

      broker.close();
      return reply;
   }

   public XMessage deletePortfolios(OpProjectSession session, XMessage request) {
      ArrayList id_strings = (ArrayList) (request.getArgument(PORTFOLIO_IDS));
      logger.debug("OpProjectAdministrationService.deletePortfolios(): portfolio_ids = " + id_strings);

      OpBroker broker = session.newBroker();
      XMessage reply = new XMessage();

      List<Long> portfolioIds = new ArrayList<Long>();
      for (int i = 0; i < id_strings.size(); i++) {
         portfolioIds.add(new Long(OpLocator.parseLocator((String) id_strings.get(i)).getID()));
      }
      OpQuery query = broker.newQuery("select portfolio.SuperNode.ID from OpProjectNode as portfolio where portfolio.ID in (:portfolioIds) and portfolio.Type = (:projectType)");
      query.setCollection("portfolioIds", portfolioIds);
      query.setByte("projectType", OpProjectNode.PORTFOLIO);
      List superPortfolioIds = broker.list(query);

      Set accessibleSuperPortfolioIds = session.accessibleIds(broker, superPortfolioIds, OpPermission.MANAGER);
      if (accessibleSuperPortfolioIds.size() == 0) {
         logger.warn("Manager access to super portfolio " + superPortfolioIds + " denied");
         reply.setError(session.newError(ERROR_MAP, OpProjectError.MANAGER_ACCESS_DENIED));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();
      /*
       * --- Not yet support by Hibernate (delete query against joined-subclass) query = broker.newQuery("delete from
       * XPool where XPool.ID in (:poolIds) and XPool.SuperPool.ID in (:accessibleSuperPoolIds)");
       * broker.execute(query);
       */
      query = broker
           .newQuery("select portfolio from OpProjectNode as portfolio where portfolio.ID in (:portfolioIds) and portfolio.SuperNode.ID in (:accessibleSuperPortfolioIds)");
      query.setCollection("portfolioIds", portfolioIds);
      query.setCollection("accessibleSuperPortfolioIds", accessibleSuperPortfolioIds);
      Iterator result = broker.iterate(query);
      boolean warningFound = false;
      while (result.hasNext()) {
         OpProjectNode portfolio = (OpProjectNode) result.next();
         boolean canDeletePortfolio = deletePortfolio(portfolio, broker, session);
         if (!canDeletePortfolio) {
            warningFound = true;
         }
      }
      t.commit();

      if (warningFound) {
         reply.setArgument("warning", Boolean.TRUE);
         reply.setError(session.newError(ERROR_MAP, OpProjectError.WORKRECORDS_STILL_EXIST_WARNING));
         broker.close();
         return reply;
      }

      //if (accessibleSuperPortfolioIds.size() < superPortfolioIds.size())
      // TODO: Return ("informative") error if notAllAccessible

      logger.debug("/OpProjectAdministrationService.deletePortfolios()");
      broker.close();
      return reply;
   }

   /**
    * Tries to delete a portfolio and all the projects under that portfolio which have no workrecords.
    *
    * @param portfolio a <code>OpProjectNode</code> representing a portfolio.
    * @param broker    a <code>OpBroker</code> used for performing business operations.
    * @param session   a <code>OpProjectSession</code> representing the current server session.
    * @return a <code>boolean</code> indicating whether any project with work records was found or not.
    */
   private boolean deletePortfolio(OpProjectNode portfolio, OpBroker broker, OpProjectSession session) {
      boolean canDeleteAllProjects = true;
      for (Object o : portfolio.getSubNodes()) {
         OpProjectNode child = (OpProjectNode) o;
         if (child.getType() == OpProjectNode.PORTFOLIO) {
            canDeleteAllProjects &= deletePortfolio(child, broker, session);
         }
         else {
            boolean hasWorkRecords = hasWorkRecords(child, broker);
            if (hasWorkRecords) {
               canDeleteAllProjects = false;
            }
            else {
               clearActiveProjectNodeSelection(child, session);
               broker.deleteObject(child);
            }
         }
      }

      if (canDeleteAllProjects) {
         broker.deleteObject(portfolio);
      }
      return canDeleteAllProjects;
   }

   public XMessage moveProjectNode(OpProjectSession session, XMessage request) {
      List projectIds = (List) request.getArgument(PROJECT_IDS);
      String portfolioId = (String) request.getArgument(PORTFOLIO_ID);

      XMessage reply = new XMessage();

      if (projectIds == null || projectIds.isEmpty() || portfolioId == null) {
         return reply; //NOP
      }

      OpBroker broker = session.newBroker();
      OpTransaction tx = broker.newTransaction();

      //get the portfolio
      OpProjectNode portfolio = (OpProjectNode) broker.getObject(portfolioId);
      // check manager access for new selected portfolio
      if (!session.checkAccessLevel(broker, portfolio.getID(), OpPermission.MANAGER)) {
         logger.warn("Move access to portfolio denied; ID = " + portfolio.getID());
         reply.setError(session.newError(ERROR_MAP, OpProjectError.MANAGER_ACCESS_DENIED));
      }
      else {
         for (Iterator it = projectIds.iterator(); it.hasNext();) {
            String projectNodeId = (String) it.next();
            OpProjectNode projectNode = (OpProjectNode) broker.getObject(projectNodeId);

            // check manager access for project node portfolio
            if (!session.checkAccessLevel(broker, projectNode.getSuperNode().getID(), OpPermission.MANAGER)) {
               logger.warn("Move access to portfolio denied; ID = " + projectNode.getSuperNode().getID());
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
      broker.close();
      return reply;
   }

   private boolean checkPortfolioAssignmentsForLoops(OpProjectNode projectNode, OpProjectNode portfolio) {
      if (projectNode.getID() == portfolio.getID()) {
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

   protected static void copyProjectPlan(XCalendar calendar, OpBroker broker, OpProjectPlan projectPlan, OpProjectPlan newProjectPlan) {
      boolean asTemplate = newProjectPlan.getTemplate();
      // Get minimum activity start date from database (just to be sure)
      OpQuery query = broker
           .newQuery("select min(activity.Start) from OpActivity as activity where activity.ProjectPlan.ID = ? and activity.Deleted = false and activity.Type != ?");
      query.setLong(0, projectPlan.getID());
      query.setByte(1, OpActivity.ADHOC_TASK);
      Iterator result = broker.iterate(query);
      if (!result.hasNext()) {
         return;
      }
      Date start = (Date) result.next();

      // Check for empty project plan
      if (start == null) {
         return;
      }

      Date newStart = newProjectPlan.getStart();
      if (asTemplate) {
         newStart = OpGanttValidator.getDefaultTemplateStart();
      }

      // Retrieve project plan for copying
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, projectPlan, dataSet, false);

      List<Integer> gaps = new ArrayList<Integer>();
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         Date activityStart = OpGanttValidator.getStart(dataRow);
         int gap = calendar.getWorkingDaysFromInterval(start, activityStart).size() - 1;
         gaps.add(gap);
      }

      // Initialize GANTT validator
      OpGanttValidator validator = new OpIncrementalValidator();
      validator.setCalculationMode(projectPlan.getCalculationMode());
      validator.setProgressTracked(projectPlan.getProgressTracked());
      validator.setDataSet(dataSet);

      // Apply new start offset and remove resource assignments (for scheduled activities)
      XComponent dataRow;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if ((OpGanttValidator.getType(dataRow) != OpGanttValidator.TASK) && (OpGanttValidator.getType(dataRow) != OpGanttValidator.COLLECTION_TASK)) {

            //keep the same working days gap
            Date date = new Date(newStart.getTime());
            int gap = gaps.get(i);
            while (gap > 0) {
               date = calendar.nextWorkDay(date);
               gap--;
            }
            Date newActivityStart = new Date(date.getTime());
            OpGanttValidator.setStart(dataRow, newActivityStart);
            OpGanttValidator.setResources(dataRow, new ArrayList());
            OpGanttValidator.setResourceBaseEfforts(dataRow, new ArrayList());
            OpGanttValidator.setWorkPhaseBaseEfforts(dataRow, new ArrayList());
            OpGanttValidator.setWorkPhaseStarts(dataRow, new ArrayList());
            OpGanttValidator.setWorkPhaseFinishes(dataRow, new ArrayList());
         }
         OpGanttValidator.setComplete(dataRow, 0);
         OpGanttValidator.setActualEffort(dataRow, 0);

         //duration must stay the same
         validator.updateDuration(dataRow, OpGanttValidator.getDuration(dataRow));
      }

      // Validate copied and adjusted project plan
      validator.validateDataSet();

      // Store activity data-set helper updates plan start/finish values and activity template flags
      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, new HashMap(), newProjectPlan, null);
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
         String countQuery = "select count(permission) from OpPermission permission where permission.Object.ID = :projectId " +
              "and permission.Subject.ID = :subjectId and permission.AccessLevel = :accessLevel and permission.SystemManaged = :systemManaged";
         OpQuery query = broker.newQuery(countQuery);
         query.setLong("projectId", projectNode.getID());
         query.setLong("subjectId", user.getID());
         query.setByte("accessLevel", OpPermission.CONTRIBUTOR);
         query.setBoolean("systemManaged", true);
         Number counter = (Number) broker.iterate(query).next();
         if (counter.intValue() == 0) {
            OpPermission permission = new OpPermission();
            permission.setAccessLevel(OpPermission.CONTRIBUTOR);
            permission.setObject(projectNode);
            permission.setSubject(user);
            permission.setSystemManaged(true);
            broker.makePersistent(permission);
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

      List filteredOutIds = (List) request.getArgument(OpProjectDataSetFactory.FILTERED_OUT_IDS);

      Integer requestedTypes = (Integer) request.getArgument(TYPES_PARAMETER);
      int types = (requestedTypes != null) ? requestedTypes.intValue() : OpProjectDataSetFactory.ALL_TYPES;

      Boolean requestedTabular = (Boolean) request.getArgument(TABULAR_PARAMETER);
      boolean tabular = (requestedTabular == null) || requestedTabular.booleanValue();

      List children = OpProjectDataSetFactory.retrieveProjectNodeChildren(projectSession, dataRow, types, tabular, filteredOutIds);

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
      List filteredOutIds = (List) request.getArgument(OpProjectDataSetFactory.FILTERED_OUT_IDS);

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
      boolean modified;

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

         modified = false;
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
                  if (resourceAssignment.getID() == projectNodeAssignment.getID()) {
                     projectAssignment = projectNodeAssignment;
                     break;
                  }
               }
            }

            if (!resource.getAssignmentVersions().isEmpty() && projectAssignment != null) {
               for (OpAssignmentVersion assignmentVersion : resource.getAssignmentVersions()) {
                  if (assignmentVersion.getPlanVersion().getProjectPlan().getProjectNode().getID() ==
                       projectAssignment.getProjectNode().getID()) {
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
                  if (assignment.getProjectPlan().getProjectNode().getID() == projectAssignment.getProjectNode().getID()) {
                     OpActivity activity = assignment.getActivity();
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

      Calendar calendar = XCalendar.setCalendarTimeToZero(start);
      Date dateStart = new Date(calendar.getTimeInMillis());
      calendar = XCalendar.setCalendarTimeToZero(end);
      Date dateEnd = new Date(calendar.getTimeInMillis());

      //obtain the list of days contained in the interval
      while (!dateEnd.before(dateStart)) {
         days.add(new Date(dateStart.getTime()));
         calendar = XCalendar.setCalendarTimeToZero(dateStart);
         calendar.add(Calendar.DATE, 1);
         dateStart = new Date(calendar.getTimeInMillis());
      }

      for (int i = 0; i < clientHourlyRatesPeriodsSet.getChildCount(); i++) {
         dataRow = (XComponent) clientHourlyRatesPeriodsSet.getChild(i);
         //check to see if the start date and end date are contained in this period's interval
         if (dataRow.getOutlineLevel() == 1) {
            calendar = XCalendar.setCalendarTimeToZero(((XComponent) dataRow.getChild(PERIOD_START_DATE)).getDateValue());
            dateStart = new Date(calendar.getTimeInMillis());
            calendar = XCalendar.setCalendarTimeToZero(((XComponent) dataRow.getChild(PERIOD_END_DATE)).getDateValue());
            dateEnd = new Date(calendar.getTimeInMillis());
            while (!dateEnd.before(dateStart)) {
               Date date = new Date(dateStart.getTime());
               if (days.contains(date)) {
                  days.remove(date);
               }
               calendar = XCalendar.setCalendarTimeToZero(dateStart);
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
      queryString.append("where assignment.ActivityVersion.PlanVersion.VersionNumber = ? and planVersion.ProjectPlan.ID = ?");

      OpQuery query = broker.newQuery(queryString.toString());
      query.setInteger(0, OpProjectPlan.WORKING_VERSION_NUMBER);
      query.setLong(1, projectPlanId);

      return broker.iterate(query);
   }

   /**
    * Updates the base personnel costs of the checked out version of the project
    *
    * @param broker   - a <code>OpBroker</code> used for performing business operations.
    * @param calendar - the <code>XCalendar</code> needed to get the working days out of an interval of time
    * @param project  - the <code>OpProjectNode</code> representing the project that has been updated.
    */
   private void updatePersonnelCostsForWorkingVersion(OpBroker broker, XCalendar calendar, OpProjectNode project) {

      Iterator it = getAssignmentsForWorkingVersion(broker, project.getPlan().getID());

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
            workingDays = calendar.getWorkingDaysFromInterval(startEndList.get(OpActivityVersion.START_DATE_LIST_INDEX),
                 startEndList.get(OpActivityVersion.END_DATE_LIST_INDEX));

            //get the project node assignment for this assignment's resource
            for (OpProjectNodeAssignment resourceAssignment : workingAssignmentVersion.getResource().getProjectNodeAssignments()) {
               for (OpProjectNodeAssignment projectAssignment : project.getAssignments()) {
                  if (resourceAssignment.getID() == projectAssignment.getID()) {
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
         }
         internalSum = 0d;
         externalSum = 0d;
      }

      OpActivityDataSetFactory.updateActivityVersionPersonnelCosts(broker, updatedAssignments);
   }

   /**
    * Updates the actual costs of the assignments and the activities that are checked in at the moment when the
    * project is updated.
    *
    * @param broker  a <code>OpBroker</code> used for performing business operations.
    * @param project a <code>OpProjectNode</code> representing the project that has been updated.
    */
   private void updateActualCosts(OpBroker broker, OpProjectNode project) {
      List<OpAssignment> updatedAssignments = new ArrayList<OpAssignment>();
      List<Double> ratesList;
      OpProjectNodeAssignment projectNodeAssignment = new OpProjectNodeAssignment();

      for (OpAssignment assignment : project.getPlan().getActivityAssignments()) {
         Double internalSum = 0d;
         Double externalSum = 0d;
         Double newActualCosts = 0d;
         Double newActualProceeds = 0d;

         for (OpWorkRecord workRecord : assignment.getWorkRecords()) {

            //get the project node assignment for this assignment's resource
            for (OpProjectNodeAssignment resourceAssignment : assignment.getResource().getProjectNodeAssignments()) {
               for (OpProjectNodeAssignment projectAssignment : project.getAssignments()) {
                  if (resourceAssignment.getID() == projectAssignment.getID()) {
                     projectNodeAssignment = projectAssignment;
                     break;
                  }
               }
            }

            //get the new rate of the project node assignment for the work record's day
            ratesList = projectNodeAssignment.getRatesForDay(workRecord.getWorkSlip().getDate(), true);
            newActualCosts = workRecord.getActualEffort() * ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
            newActualProceeds = workRecord.getActualEffort() * ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);

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
}
