/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpHourlyRatesPeriod;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpSubject;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceManager;

import java.sql.Date;
import java.util.*;


public class OpProjectAdministrationService extends OpProjectService {

   private static final XLog logger = XLogFactory.getServerLogger(OpProjectAdministrationService.class);

   private OpProjectAdministrationServiceImpl serviceImpl =
        new OpProjectAdministrationServiceImpl();

   public final static int WORKING_VERSION_NUMBER = -1;
   public final static String PROJECT_DATA = "project_data";
   public final static String PROJECT_ID = "project_id";
   public final static String PROJECT_IDS = "project_ids";
   public final static String PORTFOLIO_DATA = "portfolio_data";
   public final static String PORTFOLIO_ID = "portfolio_id";
   public final static String PORTFOLIO_IDS = "portfolio_ids";
   public final static String GOALS_SET = "goals_set";
   public final static String TO_DOS_SET = "to_dos_set";
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

   private static final int START_DATES_LIST_INDEX = 0;
   private static final int END_DATES_LIST_INDEX = 1;

   public XMessage insertProject(OpProjectSession session, XMessage request) {

      // *** TODO: Add start-date of project (maybe also due-date?)
      logger.debug("OpProjectAdministrationService.insertProject()");
      HashMap project_data = (HashMap) (request.getArgument(PROJECT_DATA));

      XMessage reply = new XMessage();
      XError error;

      OpProjectNode project = new OpProjectNode();
      project.setType(OpProjectNode.PROJECT);
      project.setName((String) (project_data.get(OpProjectNode.NAME)));

      // check mandatory input fields
      if (project.getName() == null || project.getName().length() == 0) {
         error = session.newError(ERROR_MAP, OpProjectError.PROJECT_NAME_MISSING);
         reply.setError(error);
         return reply;
      }

      project.setDescription((String) (project_data.get(OpProjectNode.DESCRIPTION)));

      Date start_date = (Date) project_data.get(OpProjectNode.START);
      if (start_date == null) {
         error = session.newError(ERROR_MAP, OpProjectError.START_DATE_MISSING);
         reply.setError(error);
         return reply;
      }

      Date end_date = (Date) project_data.get(OpProjectNode.FINISH);
      if (end_date != null && start_date.after(end_date)) {
         error = session.newError(ERROR_MAP, OpProjectError.END_DATE_INCORRECT);
         reply.setError(error);
         return reply;
      }

      project.setStart(start_date);
      project.setFinish(end_date);

      double budget = ((Double) (project_data.get(OpProjectNode.BUDGET))).doubleValue();
      if (budget < 0) {
         error = session.newError(ERROR_MAP, OpProjectError.BUDGET_INCORRECT);
         reply.setError(error);
         return reply;
      }
      else {
         project.setBudget(budget);
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

      broker.makePersistent(project);

      // Insert project plan including settings
      OpProjectPlan projectPlan = new OpProjectPlan();
      projectPlan.setStart(project.getStart());
      if (project.getFinish() != null) {
         projectPlan.setFinish(project.getFinish());
      }
      else {
         projectPlan.setFinish(projectPlan.getStart());
      }
      projectPlan.setProjectNode(project);

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
      this.applyTemplate(broker, project_data, project, projectPlan);

      // Set template-node relationship and instantiate template plan if template was specified
//      String templateNodeLocator = (String) project_data.get(OpProjectNode.TEMPLATE_NODE);
//      if (templateNodeLocator != null) {
//         templateNodeLocator = XValidator.choiceID(templateNodeLocator);
//      }
//      if ((templateNodeLocator != null) && (!templateNodeLocator.equals("null"))) {
//         OpProjectNode templateNode = (OpProjectNode) broker.getObject(templateNodeLocator);
//         if (templateNode != null) {
//            // QUESTION: Copy also template node assignments, or does this undermine the security system?
//            project.setTemplateNode(templateNode);
//            copyProjectPlan(broker, templateNode.getPlan(), projectPlan);
//         }
//      }

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

      XComponent permission_set = (XComponent) project_data.get(OpPermissionSetFactory.PERMISSION_SET);
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, project, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
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
    * Inserts the assignments related to the project passed as a parameter. In case of an error returns
    * an <code>XMessage</code> object containing the error code
    *
    * @param session
    * @param broker
    * @param project              - the project for which the assignments are inserted
    * @param assignedResourcesSet - the clients data set containing the information about the project assignments
    * @return - an <code>XMessage</code> object containing the error code in case of an error
    */
   private XMessage insertProjectAssignments(OpProjectSession session, OpBroker broker,
        OpProjectNode project, XComponent assignedResourcesSet) {
      //the reply message
      XMessage reply = new XMessage();

      //insert project assignments & hourly rates periods for each assignment
      if (assignedResourcesSet != null && assignedResourcesSet.getChildCount() > 0) {
         OpResource resource;
         OpProjectNodeAssignment projectNodeAssignment;
         Double internalRate;
         Double externalRate;
         for (int i = 0; i < assignedResourcesSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) assignedResourcesSet.getChild(i);

            //if outline level = 0 this is a resource
            if (dataRow.getOutlineLevel() == 0) {
               resource = (OpResource) (broker.getObject(dataRow.getStringValue()));

               //3 - internal rate
               internalRate = null;
               if (((XComponent) dataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX)).getValue() != null) {
                  internalRate = ((XComponent) dataRow.getChild(INTERNAL_PROJECT_RATE_COLUMN_INDEX)).getDoubleValue();
               }
               //4 - external rate
               externalRate = null;
               if (((XComponent) dataRow.getChild(EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).getValue() != null) {
                  externalRate = ((XComponent) dataRow.getChild(EXTERNAL_PROJECT_RATE_COLUMN_INDEX)).getDoubleValue();
               }

               //get the hourly rates corresponding to this project assignment
               Set<OpHourlyRatesPeriod> hourlyRatesPeriodsSet = getHourlyRatesPeriodsForAssignment(dataRow);

               if (!hourlyRatesPeriodsSet.isEmpty()) {
                  //validate the hourly rates period objects
                  for (OpHourlyRatesPeriod hourlyRatesPeriod : hourlyRatesPeriodsSet) {
                     int validationResult = hourlyRatesPeriod.isValid();
                     if (validationResult != 0) {
                        reply.setError(session.newError(ERROR_MAP, mapHourlyRatesPeriodValidation(validationResult)));
                        return reply;
                     }
                  }
               }

               projectNodeAssignment = new OpProjectNodeAssignment();
               projectNodeAssignment.setResource(resource);
               projectNodeAssignment.setProjectNode(project);
               projectNodeAssignment.setHourlyRate(internalRate);
               projectNodeAssignment.setExternalRate(externalRate);
               projectNodeAssignment.setHourlyRatesPeriods(hourlyRatesPeriodsSet);

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

               //if at least two of the period intervals overlap than return error
               if (!projectNodeAssignment.checkPeriodDoNotOverlap()) {
                  reply.setError(session.newError(ERROR_MAP, OpProjectError.DATE_INTERVAL_OVERLAP));
                  return reply;
               }

               broker.makePersistent(projectNodeAssignment);
               insertContributorPermission(broker, project, resource);

               // Add hourly rates periods
               for (OpHourlyRatesPeriod hourlyRatesPeriod : hourlyRatesPeriodsSet) {
                  hourlyRatesPeriod.setProjectNodeAssignment(projectNodeAssignment);
                  broker.makePersistent(hourlyRatesPeriod);
               }
            }
         }
      }
      return reply;
   }

   /**
    * Template method for setting the template of a project. By default, doesn't do anything.
    *
    * @param broker       an <code>OpBroker</code> used for performing business operations.
    * @param project_data a <code>HashMap</code> representing the parameters.
    * @param project      a <code>OpProjectNode</code> entity representing a project node.
    * @param projectPlan  a <code>OpProjectPlan</code> entity representing a project plan.
    */
   protected void applyTemplate(OpBroker broker, HashMap project_data, OpProjectNode project, OpProjectPlan projectPlan) {
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

      String projectName = (String) (project_data.get(OpProjectNode.NAME));

      // check mandatory input fields
      if (projectName == null || projectName.length() == 0) {
         error = session.newError(ERROR_MAP, OpProjectError.PROJECT_NAME_MISSING);
         reply.setError(error);
         broker.close();
         return reply;
      }

      Date start_date = (Date) project_data.get(OpProjectNode.START);
      if (start_date == null) {
         error = session.newError(ERROR_MAP, OpProjectError.START_DATE_MISSING);
         reply.setError(error);
         broker.close();
         return reply;
      }

      Date end_date = (Date) project_data.get(OpProjectNode.FINISH);
      if (end_date != null && start_date.after(end_date)) {
         error = session.newError(ERROR_MAP, OpProjectError.END_DATE_INCORRECT);
         reply.setError(error);
         broker.close();
         return reply;
      }

      double budget = ((Double) (project_data.get(OpProjectNode.BUDGET))).doubleValue();
      if (budget < 0) {
         error = session.newError(ERROR_MAP, OpProjectError.BUDGET_INCORRECT);
         reply.setError(error);
         broker.close();
         return reply;
      }

      OpProjectNode project = (OpProjectNode) (broker.getObject(id_string));

      if (project == null) {
         logger.warn("ERROR: Could not find object with ID " + id_string);
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpProjectError.PROJECT_NOT_FOUND));
         return reply;
      }

      // Check manager access
      if (!session.checkAccessLevel(broker, project.getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Udpate access to project denied; ID = " + id_string);
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
         return reply;
      }

      // check if project name is already used
      OpQuery projectNameQuery = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
      projectNameQuery.setString(0, projectName);
      Iterator projects = broker.iterate(projectNameQuery);
      while (projects.hasNext()) {
         OpProjectNode other = (OpProjectNode) projects.next();
         if (other.getID() != project.getID()) {
            error = session.newError(ERROR_MAP, OpProjectError.PROJECT_NAME_ALREADY_USED);
            reply.setError(error);
            broker.close();
            return reply;
         }
      }

      //check if the start date is in the future
      if (start_date.after(project.getStart())) {
         //if its checked out, throw error
         if (project.getLocks().size() > 0) {
            error = session.newError(ERROR_MAP, OpProjectError.PROJECT_LOCKED_ERROR);
            reply.setError(error);
            broker.close();
            return reply;
         }
         reply = this.shiftPlanDates(session, project, start_date);
         if (reply.getError() != null) {
            broker.close();
            return reply;
         }
         else {
            reply = new XMessage();
         }
      }

      project.setName(projectName);
      project.setDescription((String) (project_data.get(OpProjectNode.DESCRIPTION)));
      project.setStart(start_date);
      project.setFinish(end_date);
      project.setBudget(budget);

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
      Boolean calculationMode = (Boolean) project_data.get(OpProjectPlan.CALCULATION_MODE);
      if (calculationMode != null && !calculationMode.booleanValue()) {
         projectPlan.setCalculationMode(OpProjectPlan.INDEPENDENT);
      }
      else {
         projectPlan.setCalculationMode(OpProjectPlan.EFFORT_BASED);
      }
      // progress tracking can't be changed

      OpTransaction t = broker.newTransaction();

      broker.updateObject(projectPlan);
      broker.updateObject(project);

      // Update current goals
      XComponent goalsDataSet = (XComponent) (request.getArgument(GOALS_SET));
      reply = updateGoals(session, broker, project, goalsDataSet);
      if (reply.getError() != null) {
         finalizeSession(t, broker);
         return reply;
      }

      // Update current to dos
      XComponent toDosDataSet = (XComponent) (request.getArgument(TO_DOS_SET));
      reply = updateToDos(session, broker, project, toDosDataSet);
      if (reply.getError() != null) {
         finalizeSession(t, broker);
         return reply;
      }

      //update project assignments
      XComponent assignedResourcesSet = (XComponent) request.getArgument(RESOURCE_SET);
      reply = updateProjectAssignments(session, broker, project, assignedResourcesSet);
      if (reply.getError() != null) {
         finalizeSession(t, broker);
         return reply;
      }

      // update project plan versions
      Set projectPlanVersions = projectPlan.getVersions();
      if (projectPlanVersions != null && projectPlanVersions.size() > 0) {
         XComponent versionDataSet = (XComponent) request.getArgument(VERSIONS_SET);
         updateProjectPlanVersions(broker, projectPlanVersions, versionDataSet);
      }

      // update permissions
      XComponent permission_set = (XComponent) project_data.get(OpPermissionSetFactory.PERMISSION_SET);
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, project, permission_set);
      if (result != null) {
         reply.setError(result);
         broker.close();
         return reply;
      }

      t.commit();
      logger.debug("/OpProjectAdministrationService.updateProject()");
      broker.close();
      return null;
   }

   private XMessage updateGoals(OpProjectSession session, OpBroker broker,
        OpProjectNode project, XComponent goalsDataSet) {
      //the reply message
      XMessage reply = new XMessage();

      HashMap goal_map = new HashMap();
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
            if (!goal.getName().equals(data_cell.getStringValue())) {
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

      HashMap to_do_map = new HashMap();
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
            if (!to_do.getName().equals(data_cell.getStringValue())) {
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
   private XMessage updateProjectAssignments(OpProjectSession session, OpBroker broker, OpProjectNode project, XComponent assignedResourcesSet) {
      //the reply message
      XMessage reply = new XMessage();
      Iterator projectNodeAssignments = project.getAssignments().iterator();
      // map of project assignments <resource Locator , projectNodeAssignment entity>
      Map assignmentNodeMap = new HashMap();
      //map of responsible user id for the resource
      Map responsibleUsersMap = new HashMap();
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
            if (dataRow.getOutlineLevel() == 0) {
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

               //get the hourly rates corresponding to this project assignment
               Set<OpHourlyRatesPeriod> hourlyRatesPeriodsSet = getHourlyRatesPeriodsForAssignment(dataRow);

               if (!hourlyRatesPeriodsSet.isEmpty()) {
                  //validate the hourly rates period objects
                  for (OpHourlyRatesPeriod hourlyRatesPeriod : hourlyRatesPeriodsSet) {
                     int validationResult = hourlyRatesPeriod.isValid();
                     if (validationResult != 0) {
                        reply.setError(session.newError(ERROR_MAP, mapHourlyRatesPeriodValidation(validationResult)));
                        return reply;
                     }
                  }
               }

               if (!assignmentNodeMap.containsKey(resourceLocator)) {
                  //a new assignment was added
                  OpProjectNodeAssignment assignment = new OpProjectNodeAssignment();
                  assignment.setResource(resource);
                  assignment.setProjectNode(project);
                  assignment.setHourlyRate(internalRate);
                  assignment.setExternalRate(externalRate);
                  assignment.setHourlyRatesPeriods(hourlyRatesPeriodsSet);

                  //if at least two of the period intervals overlap than return error
                  if (!assignment.checkPeriodDoNotOverlap()) {
                     reply.setError(session.newError(ERROR_MAP, OpProjectError.DATE_INTERVAL_OVERLAP));
                     return reply;
                  }

                  broker.makePersistent(assignment);
                  insertContributorPermission(broker, project, resource);

                  // Add hourly rates periods
                  for (OpHourlyRatesPeriod hourlyRatesPeriod : hourlyRatesPeriodsSet) {
                     hourlyRatesPeriod.setProjectNodeAssignment(assignment);
                     broker.makePersistent(hourlyRatesPeriod);
                  }
               }
               else {
                  //update existing assignments
                  OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) assignmentNodeMap.get(resourceLocator);
                  assignment.setResource(resource);
                  assignment.setProjectNode(project);
                  assignment.setHourlyRate(internalRate);
                  assignment.setExternalRate(externalRate);

                  //get the old hourly rates periods corresponding to this assignment and delete them
                  for (OpHourlyRatesPeriod hourlyRatesPeriod : assignment.getHourlyRatesPeriods()) {
                     broker.deleteObject(hourlyRatesPeriod);
                  }


                  assignment.setHourlyRatesPeriods(hourlyRatesPeriodsSet);
                  //if at least two of the period intervals overlap than return error
                  if (!assignment.checkPeriodDoNotOverlap()) {
                     reply.setError(session.newError(ERROR_MAP, OpProjectError.DATE_INTERVAL_OVERLAP));
                     return reply;
                  }

                  //insert the new hourly rates periods fo this assignment
                  for (OpHourlyRatesPeriod hourlyRatesPeriod : hourlyRatesPeriodsSet) {
                     hourlyRatesPeriod.setProjectNodeAssignment(assignment);
                     broker.makePersistent(hourlyRatesPeriod);
                  }
                  broker.updateObject(assignment);

                  assignmentNodeMap.remove(resourceLocator);
                  responsibleUsersMap.remove(resourceLocator);
               }
            }
         }
      }
      // Remove outdated project node assignments if no activity assignments exist for the resource
      Iterator outDatedAssignments = assignmentNodeMap.values().iterator();
      while (outDatedAssignments.hasNext()) {
         OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) outDatedAssignments.next();
         int activityAssignmentsCounter = OpResourceService.getResourcePlanningAssignmentsCount(broker, assignment.getResource(), assignment.getProjectNode().getPlan());
         if (activityAssignmentsCounter > 0) {
            reply.setError(session.newError(ERROR_MAP, OpProjectError.ACTIVITY_ASSIGNMENTS_EXIST_ERROR));
            return reply;
         }
         else {
            //before deleting the assignment delete the hourly rates periods corresponding to it
            for (OpHourlyRatesPeriod hourlyRatesPeriod : assignment.getHourlyRatesPeriods()) {
               broker.deleteObject(hourlyRatesPeriod);
            }

            broker.deleteObject(assignment);
         }
      }

      // Remove auto-added (i.e., system-managed) contributor permissions for project node and resources
      Collection userIds = responsibleUsersMap.values();
      if (!userIds.isEmpty()) {
         OpQuery query = broker.newQuery("select permission from OpPermission as permission where permission.Object.ID = :projectId and permission.Subject.ID in (:userIds) and permission.SystemManaged = :systemManaged");
         query.setLong("projectId", project.getID());
         query.setCollection("userIds", userIds);
         query.setBoolean("systemManaged", true);
         Iterator permissionsToDelete = broker.list(query).iterator();
         while (permissionsToDelete.hasNext()) {
            OpPermission permission = (OpPermission) permissionsToDelete.next();
            //remove permission if there are no project node assignments for resources with the same responsible user
            OpSubject subject = permission.getSubject();
            if (subject.getPrototype().getName().equals(OpUser.USER)) {
               OpUser user = (OpUser) subject;
               //list of resource ids for which the user is responsible
               query = broker.newQuery("select resource.ID from OpUser user inner join user.Resources resource where user.ID = :userId ");
               query.setLong("userId", user.getID());
               List resourceIds = broker.list(query);

               if (!resourceIds.isEmpty()) {
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
      }

      return reply;
   }

   /**
    * Updates the versions of a project plan, by deleting the ones that were deleted by the client.
    *
    * @param broker           a <code>OpBroker</code> used for performing business operations.
    * @param existingVersions a <code>Set</code> of <code>OpProjectPlanVersion</code> representing the existent project plan
    *                         versions.
    * @param versionsDataSet  a <code>XComponent</code> representing the client side project plan versions.
    */
   private void updateProjectPlanVersions(OpBroker broker, Set existingVersions, XComponent versionsDataSet) {
      // create a map of the existing versions
      Map existingVersionMap = new HashMap(existingVersions.size());
      for (Iterator it = existingVersions.iterator(); it.hasNext();) {
         OpProjectPlanVersion version = (OpProjectPlanVersion) it.next();
         if (version.getVersionNumber() != WORKING_VERSION_NUMBER) {
            String versionId = OpLocator.locatorString(version);
            existingVersionMap.put(versionId, version);
         }
      }

      // remove the existent ones from the map
      for (int i = 0; i < versionsDataSet.getChildCount(); i++) {
         XComponent row = (XComponent) versionsDataSet.getChild(i);
         String versionId = ((XComponent) row.getChild(0)).getStringValue();
         existingVersionMap.remove(versionId);
      }

      // remove all the other versions in the map
      Collection values = existingVersionMap.values();
      if (values.size() > 0) {
         for (Iterator it = values.iterator(); it.hasNext();) {
            OpProjectPlanVersion version = (OpProjectPlanVersion) it.next();
            OpActivityVersionDataSetFactory.deleteProjectPlanVersion(broker, version);
         }
      }
   }

   public XMessage deleteProjects(OpProjectSession session, XMessage request) {
      ArrayList id_strings = (ArrayList) (request.getArgument(PROJECT_IDS));
      logger.debug("OpProjectAdministrationService.deleteProjects(): project_ids = " + id_strings);

      OpBroker broker = session.newBroker();
      XMessage reply = new XMessage();

      List projectIds = new ArrayList();
      for (int i = 0; i < id_strings.size(); i++) {
         projectIds.add(new Long(OpLocator.parseLocator((String) id_strings.get(i)).getID()));
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

      OpProjectNode portfolio = new OpProjectNode();
      portfolio.setType(OpProjectNode.PORTFOLIO);
      portfolio.setName((String) (portfolioData.get(OpProjectNode.NAME)));
      portfolio.setDescription((String) (portfolioData.get(OpProjectNode.DESCRIPTION)));

      XMessage reply = new XMessage();
      XError error;

      if (portfolio.getName() == null || portfolio.getName().length() == 0) {
         error = session.newError(ERROR_MAP, OpProjectError.PORTFOLIO_NAME_MISSING);
         reply.setError(error);
         return reply;
      }

      OpBroker broker = session.newBroker();

      String superPortfolioLocator = (String) portfolioData.get("SuperPortfolioID");
      System.err.println("SUPER_PORT " + superPortfolioLocator);
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
         error = session.newError(ERROR_MAP, OpProjectError.PORTFOLIO_NAME_ALREADY_USED);
         reply.setError(error);
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      broker.makePersistent(portfolio);

      XComponent permission_set = (XComponent) portfolioData.get(OpPermissionSetFactory.PERMISSION_SET);
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, portfolio, permission_set);
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
      // *** TODO: Check for other fields that can be updated
      String id_string = (String) (request.getArgument(PORTFOLIO_ID));
      logger.debug("OpProjectAdministrationService.updatePortfolio(): id = " + id_string);
      HashMap portfolioData = (HashMap) (request.getArgument(PORTFOLIO_DATA));

      XMessage reply = new XMessage();
      XError error;

      OpBroker broker = session.newBroker();

      String portfolioName = (String) (portfolioData.get(OpProjectNode.NAME));

      if (portfolioName == null || portfolioName.length() == 0) {
         error = session.newError(ERROR_MAP, OpProjectError.PORTFOLIO_NAME_MISSING);
         reply.setError(error);
         broker.close();
         return reply;
      }

      OpProjectNode portfolio = (OpProjectNode) (broker.getObject(id_string));

      // *** We could check if the fields have been modified (does this help or
      // not)?
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

      // check if portfolio name is already used
      OpQuery query = broker.newQuery(PROJECT_NODE_NAME_QUERY_STRING);
      query.setString(0, portfolioName);
      Iterator portfolios = broker.iterate(query);
      while (portfolios.hasNext()) {
         OpProjectNode other = (OpProjectNode) portfolios.next();
         if (other.getID() != portfolio.getID()) {
            error = session.newError(ERROR_MAP, OpProjectError.PORTFOLIO_NAME_ALREADY_USED);
            reply.setError(error);
            broker.close();
            return reply;
         }
      }

      if (findRootPortfolio(broker).getID() != portfolio.getID()) {
         portfolio.setName(portfolioName);
         portfolio.setDescription((String) (portfolioData.get(OpProjectNode.DESCRIPTION)));
      }

      OpTransaction t = broker.newTransaction();

      broker.updateObject(portfolio);

      XComponent permission_set = (XComponent) portfolioData.get(OpPermissionSetFactory.PERMISSION_SET);
      XError result = OpPermissionSetFactory.storePermissionSet(broker, session, portfolio, permission_set);
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

      List portfolioIds = new ArrayList();
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
      Iterator it = portfolio.getSubNodes().iterator();
      while (it.hasNext()) {
         OpProjectNode child = (OpProjectNode) it.next();
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
      Iterator result = broker.list(query).iterator();
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
      OpPermissionSetFactory.addSystemObjectPermissions(session, broker, rootPortfolio);
      t.commit();
      return rootPortfolio;
   }

   protected static void copyProjectPlan(OpBroker broker, OpProjectPlan projectPlan, OpProjectPlan newProjectPlan) {
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
      // Force new start to be 0001-01-01 for template plans
      // TODO: Use calendar of this template
      Date newStart = newProjectPlan.getStart();
      if (asTemplate) {
         newStart = OpGanttValidator.getDefaultTemplateStart();
      }
      long newStartOffset = newStart.getTime() - start.getTime();
      // Retrieve project plan for copying
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, projectPlan, dataSet, false);
      // Initialize GANTT validator
      OpGanttValidator validator = new OpGanttValidator();
      validator.setCalculationMode(new Byte(projectPlan.getCalculationMode()));
      validator.setProgressTracked(Boolean.valueOf(projectPlan.getProgressTracked()));
      validator.setDataSet(dataSet);
      // Apply new start offset, set finish values to null and remove resource assignments (for scheduled activities)
      XComponent dataRow;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if ((OpGanttValidator.getType(dataRow) != OpGanttValidator.TASK) && (OpGanttValidator.getType(dataRow) != OpGanttValidator.COLLECTION_TASK)) {
            OpGanttValidator.setStart(dataRow, new Date(OpGanttValidator.getStart(dataRow).getTime() + newStartOffset));
            OpGanttValidator.setEnd(dataRow, null);
            OpGanttValidator.setResources(dataRow, new ArrayList());
            OpGanttValidator.setResourceBaseEfforts(dataRow, new ArrayList());
            OpGanttValidator.setWorkPhaseBaseEfforts(dataRow, null);
            OpGanttValidator.setWorkPhaseStarts(dataRow, null);
            OpGanttValidator.setWorkPhaseFinishes(dataRow, null);
         }
         OpGanttValidator.setComplete(dataRow, 0);
         OpGanttValidator.setActualEffort(dataRow, 0);
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

      Integer requestedTypes = (Integer) request.getArgument(TYPES_PARAMETER);
      int types = (requestedTypes != null) ? requestedTypes.intValue() : OpProjectDataSetFactory.ALL_TYPES;

      Boolean requestedTabular = (Boolean) request.getArgument(TABULAR_PARAMETER);
      boolean tabular = (requestedTabular == null) || requestedTabular.booleanValue();

      List children = OpProjectDataSetFactory.retrieveProjectNodeChildren(projectSession, dataRow, types, tabular, null);

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
    * Returns a <code>Set</code> of <code>OpHourlyRatesPeriod</code> objects representing the hourly rates
    * per time periods for an assignment
    *
    * @param parentDataRow - the row containing the assignment.
    * @return - a <code>Set</code> of <code>OpHourlyRatesPeriod</code> objects representing the hourly rates
    *         per time periods for an assignment.
    */
   private Set<OpHourlyRatesPeriod> getHourlyRatesPeriodsForAssignment(XComponent parentDataRow) {
      Set<OpHourlyRatesPeriod> result = new HashSet<OpHourlyRatesPeriod>();
      XComponent dataRow;
      XComponent dataCell;
      OpHourlyRatesPeriod hourlyRatesPeriod;
      List childRows = parentDataRow.getSubRows();

      for (int i = 0; i < childRows.size(); i++) {
         dataRow = (XComponent) childRows.get(i);

         hourlyRatesPeriod = new OpHourlyRatesPeriod();

         // start date
         dataCell = (XComponent) dataRow.getChild(PERIOD_START_DATE);
         hourlyRatesPeriod.setStart(dataCell.getDateValue());

         // end date
         dataCell = (XComponent) dataRow.getChild(PERIOD_END_DATE);
         hourlyRatesPeriod.setFinish(dataCell.getDateValue());

         // internal rate
         dataCell = (XComponent) dataRow.getChild(INTERNAL_PERIOD_RATE_COLUMN_INDEX);
         hourlyRatesPeriod.setInternalRate(dataCell.getDoubleValue());

         // external rate
         dataCell = (XComponent) dataRow.getChild(EXTERNAL_PERIOD_RATE_COLUMN_INDEX);
         hourlyRatesPeriod.setExternalRate(dataCell.getDoubleValue());

         result.add(hourlyRatesPeriod);
      }
      return result;
   }

   /**
    * Maps the result of the validation on an OpHourlyRatesPeriod object to an error in the project error map
    *
    * @param validationResult - the result of the validation (will always represent an error)
    * @return - the project map error code corresponding to the error code of the validation
    */
   private int mapHourlyRatesPeriodValidation(int validationResult) {
      if (validationResult == OpHourlyRatesPeriod.PERIOD_START_DATE_NOT_VALID) {
         return OpProjectError.PERIOD_START_DATE_NOT_VALID;
      }
      else if (validationResult == OpHourlyRatesPeriod.PERIOD_END_DATE_NOT_VALID) {
         return OpProjectError.PERIOD_END_DATE_NOT_VALID;
      }
      else if (validationResult == OpHourlyRatesPeriod.INTERNAL_RATE_NOT_VALID) {
         return OpProjectError.INTERNAL_RATE_NOT_VALID;
      }
      else if (validationResult == OpHourlyRatesPeriod.EXTERNAL_RATE_NOT_VALID) {
         return OpProjectError.EXTERNAL_RATE_NOT_VALID;
      }
      else {
         return OpProjectError.PERIOD_INTERVAL_NOT_VALID;
      }
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
         if (originalDataRow.getOutlineLevel() == 0) {

            modified = false;
            for (int j = 0; j < newResourceSet.getChildCount(); j++) {
               newDataRow = (XComponent) newResourceSet.getChild(j);
               if (newDataRow.getOutlineLevel() == 0
                    && originalDataRow.getStringValue().equals(newDataRow.getStringValue())) {
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

                  if (modified) {

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
                              //check if the resource's activity assignments are covered by hourly rates periods
                              if (!isPeriodCoveredByHourlyRatesPeriod(activityVersion.getStart(), activityVersion.getFinish(), newResourceSet)) {
                                 //check if the rates have changed for this interval
                                 if (isRateDifferentForPeriod(activityVersion.getStart(), activityVersion.getFinish(), resource,
                                      newInternalRate, newExternalRate)) {
                                    hasAssignments = true;
                                    xMessage.setArgument(HAS_ASSIGNMENTS, Boolean.valueOf(hasAssignments));
                                    return xMessage;
                                 }
                              }
                           }
                        }
                     }

                     if (!resource.getActivityAssignments().isEmpty() && projectAssignment != null) {
                        for (OpAssignment assignment : resource.getActivityAssignments()) {
                           if (assignment.getProjectPlan().getProjectNode().getID() == projectAssignment.getProjectNode().getID()) {
                              OpActivity activity = assignment.getActivity();
                              //check if the resource's activities are covered by hourly rates periods
                              if (!isPeriodCoveredByHourlyRatesPeriod(activity.getStart(), activity.getFinish(), newResourceSet)) {
                                 //check if the rates have changed for this interval
                                 if (isRateDifferentForPeriod(activity.getStart(), activity.getFinish(), resource,
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
            }
         }
      }

      xMessage.setArgument(HAS_ASSIGNMENTS, Boolean.valueOf(hasAssignments));
      return xMessage;
   }

   /**
    * Return <code>true</code> if in the data set of assignments there is at least one assignment that has
    * any activity for this project in a given period of time with different internal or external rate.
    *
    * @param s
    * @param request
    * @return <code>false</code> if all the assignments have no activities and <code>true</code> otherwise
    */
   public XMessage haveAssignmentsInTimePeriod(OpProjectSession s, XMessage request) {
      XComponent assignmentsRatesSet = (XComponent) (request.getArgument(RESOURCE_SET));
      String projectID = (String) (request.getArgument(PROJECT_ID));
      OpBroker broker = s.newBroker();
      XMessage xMessage = new XMessage();
      boolean hasAssignments = false;
      XComponent dataRow;
      OpProjectNodeAssignment projectNodeAssignment = null;
      OpResource resource;
      OpProjectNode project = (OpProjectNode) (broker.getObject(projectID));

      for (int i = 0; i < assignmentsRatesSet.getChildCount(); i++) {
         dataRow = (XComponent) assignmentsRatesSet.getChild(i);
         //we have an assignment
         if (dataRow.getOutlineLevel() == 0) {
            resource = (OpResource) (broker.getObject(dataRow.getStringValue()));
            for (OpProjectNodeAssignment resourceAssignment : resource.getProjectNodeAssignments()) {
               for (OpProjectNodeAssignment projectAssignment : project.getAssignments()) {
                  if (resourceAssignment.getID() == projectAssignment.getID()) {
                     projectNodeAssignment = projectAssignment;
                  }
               }
            }

            if (projectNodeAssignment != null) {
               //obtain the list which contains the intervals of the activities of the resource for this project
               List<List> startFinishOfAssignments = getStartFinishOfAssignments(resource, project);

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
                  //include only the periods that belong to the resource
                  XComponent resourcePeriods = new XComponent(XComponent.DATA_SET);
                  XComponent tempRow = new XComponent(XComponent.DATA_ROW);
                  for (int j = 0; j < assignmentsRatesSet.getChildCount(); j++) {
                     tempRow = (XComponent) assignmentsRatesSet.getChild(j);
                     if (tempRow.getOutlineLevel() == 0) {
                        if (resource.locator().equals(XValidator.choiceID(tempRow.getStringValue()))) {
                           XComponent[] children = (XComponent[]) tempRow.getSubRows().toArray(new XComponent[0]);
                           resourcePeriods.addAllChildren(children);
                        }
                     }
                  }

                  Map intervalsFromHourlyRatesPeriods = getIntervalsFromHourlyRatesPeriods(resourcePeriods, projectNodeAssignment, start, end);

                  //obtain the map which contains the intervals from the client's data set between the start and end dates
                  cutInterval = cutIntervalMapToStartFinish(intervalsFromHourlyRatesPeriods, start, end);
               }

               List<Date> activityStartEndList;
               if (resource.getAssignmentVersions().size() != 0 && cutInterval != null) {
                  OpActivityVersion activityVersion;
                  for (OpAssignmentVersion assignmentVersion : resource.getAssignmentVersions()) {
                     if (assignmentVersion.getPlanVersion().getProjectPlan().getProjectNode().getID() ==
                          projectNodeAssignment.getProjectNode().getID()) {
                        activityVersion = assignmentVersion.getActivityVersion();
                        activityStartEndList = activityVersion.getStartEndDateByType();
                        if (activityStartEndList != null) {
                           if (hasDifferentRatesInInterval(activityStartEndList.get(OpActivityVersion.START_DATE_LIST_INDEX),
                                activityStartEndList.get(OpActivityVersion.END_DATE_LIST_INDEX), cutInterval, projectNodeAssignment)) {
                              hasAssignments = true;
                              xMessage.setArgument(HAS_ASSIGNMENTS_IN_TIME_PERIOD, Boolean.valueOf(hasAssignments));
                              return xMessage;
                           }
                        }
                     }
                  }
               }

               if (resource.getActivityAssignments().size() != 0 && cutInterval != null) {
                  OpActivity activity;
                  for (OpAssignment assignment : resource.getActivityAssignments()) {
                     if (assignment.getProjectPlan().getProjectNode().getID() == projectNodeAssignment.getProjectNode().getID()) {
                        activity = assignment.getActivity();
                        activityStartEndList = activity.getStartEndDateByType();
                        if (activityStartEndList != null) {
                           if (hasDifferentRatesInInterval(activityStartEndList.get(OpActivity.START_DATE_LIST_INDEX),
                                activityStartEndList.get(OpActivity.END_DATE_LIST_INDEX), cutInterval, projectNodeAssignment)) {
                              hasAssignments = true;
                              xMessage.setArgument(HAS_ASSIGNMENTS_IN_TIME_PERIOD, Boolean.valueOf(hasAssignments));
                              return xMessage;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      xMessage.setArgument(HAS_ASSIGNMENTS_IN_TIME_PERIOD, Boolean.valueOf(hasAssignments));
      return xMessage;
   }

   /**
    * Returns an List containing two lists:
    * 0 - startList: containing the start dates of the resource's activities and activity assignments for the project
    * 1 - endList: containing the end dates of the resource's activities and activity versions for the project
    *
    * @param resource - the resource who's activity/activity versions start and end dates will be returned
    * @param project  - the project which contains the resource's activity/activity versions
    * @return - an <code>List</code> containing two <cod>List</code> objects
    *         0 - startList: containing the start dates of the resource's activities and activity assignments for the project
    *         1 - endList: containing the end dates of the resource's activities and activity versions for the project
    */

   private List<List> getStartFinishOfAssignments(OpResource resource, OpProjectNode project) {
      List<List> result = new ArrayList<List>();
      List<java.sql.Date> startList = new ArrayList<java.sql.Date>();
      List<java.sql.Date> endList = new ArrayList<java.sql.Date>();
      List<Date> activityStartDateList;

      //put all the start/end dates of the activity versions and activities belonging to the resource in a start/end TreeMap
      if (!resource.getAssignmentVersions().isEmpty()) {
         Iterator<OpAssignmentVersion> iterator = resource.getAssignmentVersions().iterator();
         OpAssignmentVersion assignmentVersion;
         OpActivityVersion activityVersion;
         while (iterator.hasNext()) {
            assignmentVersion = iterator.next();
            //check that the assignment versions are from this project
            if (assignmentVersion.getPlanVersion().getProjectPlan().getProjectNode().getID() == project.getID()) {
               activityVersion = assignmentVersion.getActivityVersion();
               activityStartDateList = activityVersion.getStartEndDateByType();
               if (activityStartDateList != null) {
                  startList.add(activityStartDateList.get(OpActivityVersion.START_DATE_LIST_INDEX));
                  endList.add(activityStartDateList.get(OpActivityVersion.END_DATE_LIST_INDEX));
               }
            }
         }
      }

      if (!resource.getActivityAssignments().isEmpty()) {
         Iterator<OpAssignment> iterator = resource.getActivityAssignments().iterator();
         OpAssignment assignment;
         OpActivity activity;
         while (iterator.hasNext()) {
            assignment = iterator.next();
            //check that the assignments are from this project
            if (assignment.getProjectPlan().getProjectNode().getID() == project.getID()) {
               activity = assignment.getActivity();
               activityStartDateList = activity.getStartEndDateByType();
               if (activityStartDateList != null) {
                  startList.add(activityStartDateList.get(OpActivity.START_DATE_LIST_INDEX));
                  endList.add(activityStartDateList.get(OpActivity.END_DATE_LIST_INDEX));
               }
            }
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
    * @param projectNodeAssignment     - the project assignment which has its rates modified
    * @param startOfTime               - the start date fo the first interval
    * @param endOfTime                 - the end date of the last interval
    * @return - a <code>Map<code> with all the sorted intervals from the hourlyRatesPeriods data set
    */
   private Map<java.sql.Date, OpHourlyRatesPeriod> getIntervalsFromHourlyRatesPeriods(XComponent hourlyRatesPeriodsDataSet,
        OpProjectNodeAssignment projectNodeAssignment, java.sql.Date startOfTime, java.sql.Date endOfTime) {
      Map<java.sql.Date, OpHourlyRatesPeriod> intervalMap = new TreeMap<java.sql.Date, OpHourlyRatesPeriod>();
      OpHourlyRatesPeriod hourlyRatesPeriod;

      if (hourlyRatesPeriodsDataSet != null) {
         //form an OpHourlyRatesPeriod object for each row in the data set and store it in an interval map under it's start date
         for (int i = 0; i < hourlyRatesPeriodsDataSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) hourlyRatesPeriodsDataSet.getChild(i);
            if (dataRow.getOutlineLevel() == 1) {
               hourlyRatesPeriod = new OpHourlyRatesPeriod();
               hourlyRatesPeriod.setStart(((XComponent) dataRow.getChild(PERIOD_START_DATE)).getDateValue());
               hourlyRatesPeriod.setFinish(((XComponent) dataRow.getChild(PERIOD_END_DATE)).getDateValue());
               hourlyRatesPeriod.setInternalRate(((XComponent) dataRow.getChild(INTERNAL_PERIOD_RATE_COLUMN_INDEX)).getDoubleValue());
               hourlyRatesPeriod.setExternalRate(((XComponent) dataRow.getChild(EXTERNAL_PERIOD_RATE_COLUMN_INDEX)).getDoubleValue());
               if (hourlyRatesPeriod.isValid() == 0) {
                  intervalMap.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
               }
            }
         }
      }

      //form a list containing all the start dates
      Set<java.sql.Date> startDatesSet = intervalMap.keySet();
      List<java.sql.Date> startDatesList = new ArrayList<java.sql.Date>();
      for (java.sql.Date startDate : startDatesSet) {
         startDatesList.add(new java.sql.Date(startDate.getTime()));
      }

      Calendar calendarStart = Calendar.getInstance();
      Calendar calendarFinish = Calendar.getInstance();
      Date finishInterval;
      Date startInterval;
      Map<Date, OpHourlyRatesPeriod> tempMap = new HashMap<Date, OpHourlyRatesPeriod>();

      if (!startDatesList.isEmpty()) {
         //add an interval from "begining of time" to first startDate - 1
         finishInterval = intervalMap.get(startDatesList.get(0)).getStart();
         calendarFinish.setTimeInMillis(finishInterval.getTime());
         calendarFinish.add(Calendar.DATE, -1);
         hourlyRatesPeriod = new OpHourlyRatesPeriod();
         hourlyRatesPeriod.setStart(startOfTime);
         hourlyRatesPeriod.setFinish(new java.sql.Date(calendarFinish.getTimeInMillis()));
         //break this interval into smaller one with constant rates
         tempMap = breakIntervalAccordingToRates(projectNodeAssignment, hourlyRatesPeriod.getStart(), hourlyRatesPeriod.getFinish());
         intervalMap.putAll(tempMap);

         //add an interval from the last finishDate + 1 to the "end of time"
         startInterval = intervalMap.get(startDatesList.get(startDatesList.size() - 1)).getFinish();
         calendarStart.setTimeInMillis(startInterval.getTime());
         calendarStart.add(Calendar.DATE, 1);
         hourlyRatesPeriod = new OpHourlyRatesPeriod();
         hourlyRatesPeriod.setStart(new java.sql.Date(calendarStart.getTimeInMillis()));
         hourlyRatesPeriod.setFinish(endOfTime);
         tempMap = breakIntervalAccordingToRates(projectNodeAssignment, hourlyRatesPeriod.getStart(), hourlyRatesPeriod.getFinish());
         intervalMap.putAll(tempMap);
      }
      else {
         hourlyRatesPeriod = new OpHourlyRatesPeriod();
         hourlyRatesPeriod.setStart(startOfTime);
         hourlyRatesPeriod.setFinish(endOfTime);
         tempMap = breakIntervalAccordingToRates(projectNodeAssignment, hourlyRatesPeriod.getStart(), hourlyRatesPeriod.getFinish());
         intervalMap.putAll(tempMap);
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
            tempMap = breakIntervalAccordingToRates(projectNodeAssignment, hourlyRatesPeriod.getStart(), hourlyRatesPeriod.getFinish());
            intervalMap.putAll(tempMap);
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
      for (OpHourlyRatesPeriod hourlyRatesPeriod : hourlyRatesCollection) {
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
    * Returns <code>true</code> if the project assignment has at least one internal/external rate different from
    * the rates in the time interval between start and end, or <code>false</code> otherwise
    *
    * @param start             - the start date of the interval for which the rates wil be compared
    * @param end               - the end date of the interval for which the rates wil be compared
    * @param cutInterval       - a <code>Map</code> cotaining the intervals with different rates
    * @param projectAssignment - the <code>OpProjectNodeAssignment</code> object for which the rates will be compared
    * @return - <code>true</code> if the project assignment has at least one internal/external rate different from
    *         the rates in the time interval between start and end, or <code>false</code> otherwise
    */
   private boolean hasDifferentRatesInInterval(java.sql.Date start, java.sql.Date end,
        Map<Date, OpHourlyRatesPeriod> cutInterval, OpProjectNodeAssignment projectAssignment) {
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

         List<List> ratesList = projectAssignment.getRatesForInterval(startDate, endDate, true);
         internalRatesForInterval = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
         for (int i = 0; i < internalRatesForInterval.size(); i++) {
            if (hourlyRatesPeriod.getInternalRate() != internalRatesForInterval.get(i)) {
               return true;
            }
         }
         //compare only in the smallest interval (activityStart - activityFinish OR hourlyRatePeriodStart - hourlyRatePeriodFinish)
         externalRatesForInterval = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);
         for (int i = 0; i < externalRatesForInterval.size(); i++) {
            if (hourlyRatesPeriod.getExternalRate() != externalRatesForInterval.get(i)) {
               return true;
            }
         }
      }

      return false;
   }

   /**
    * Breaks and interval with variable internal and external rates into smaller intervals with constant internal
    * and external rates
    *
    * @param projectAssignment - the assignment for which the rates are calculated and separated into intervals
    * @param start             - the start date of the interval with variable rates
    * @param end               - the end date of the interval with variable rates
    * @return - a <code>HashMap</code> containg the intervals with constant internal and external rates.
    *         The key of an entity from the map will be the start date of the smller interval and the value will be
    *         the OpHourlyRatesPeriod object that contains the interval and the constant rates.
    */
   private Map<Date, OpHourlyRatesPeriod> breakIntervalAccordingToRates(OpProjectNodeAssignment projectAssignment,
        Date start, Date end) {
      Map<Date, OpHourlyRatesPeriod> result = new HashMap<Date, OpHourlyRatesPeriod>();
      List<List> rates = projectAssignment.getRatesForInterval(start, end, false);
      List<Double> internalRatesList = rates.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
      List<Double> externalRatesList = rates.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);

      Calendar calendarStart = Calendar.getInstance();
      calendarStart.setTime(start);
      calendarStart.set(Calendar.HOUR, 0);
      calendarStart.set(Calendar.MINUTE, 0);
      calendarStart.set(Calendar.SECOND, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      Calendar calendarEnd = Calendar.getInstance();
      calendarEnd.setTime(start);
      calendarEnd.set(Calendar.HOUR, 0);
      calendarEnd.set(Calendar.MINUTE, 0);
      calendarEnd.set(Calendar.SECOND, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);
      Calendar calendarFinish = Calendar.getInstance();
      calendarFinish.setTime(end);
      calendarFinish.set(Calendar.HOUR, 0);
      calendarFinish.set(Calendar.MINUTE, 0);
      calendarFinish.set(Calendar.SECOND, 0);
      calendarFinish.set(Calendar.MILLISECOND, 0);
      OpHourlyRatesPeriod hourlyRatesPeriod;

      if (internalRatesList.size() == 1) {
         hourlyRatesPeriod = new OpHourlyRatesPeriod();
         hourlyRatesPeriod.setStart(start);
         hourlyRatesPeriod.setFinish(end);
         hourlyRatesPeriod.setInternalRate(internalRatesList.get(0));
         hourlyRatesPeriod.setExternalRate(externalRatesList.get(0));
         result.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
      }
      for (int i = 0; i < internalRatesList.size() - 1; i++) {
         if (!internalRatesList.get(i).equals(internalRatesList.get(i + 1)) ||
              !externalRatesList.get(i).equals(externalRatesList.get(i + 1))) {
            hourlyRatesPeriod = new OpHourlyRatesPeriod();
            hourlyRatesPeriod.setStart(new java.sql.Date(calendarStart.getTimeInMillis()));
            hourlyRatesPeriod.setFinish(new java.sql.Date(calendarEnd.getTimeInMillis()));
            hourlyRatesPeriod.setInternalRate(internalRatesList.get(i));
            hourlyRatesPeriod.setExternalRate(externalRatesList.get(i));
            result.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
            calendarEnd.add(Calendar.DATE, 1);
            calendarStart.setTimeInMillis(calendarEnd.getTimeInMillis());
         }
         else {
            calendarEnd.add(Calendar.DATE, 1);
         }
         if (calendarEnd.getTimeInMillis() == calendarFinish.getTimeInMillis()) {
            hourlyRatesPeriod = new OpHourlyRatesPeriod();
            hourlyRatesPeriod.setStart(new java.sql.Date(calendarStart.getTimeInMillis()));
            hourlyRatesPeriod.setFinish(new java.sql.Date(calendarEnd.getTimeInMillis()));
            hourlyRatesPeriod.setInternalRate(internalRatesList.get(i + 1));
            hourlyRatesPeriod.setExternalRate(externalRatesList.get(i + 1));
            result.put(hourlyRatesPeriod.getStart(), hourlyRatesPeriod);
         }
      }
      return result;
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

      Calendar calendarStart = Calendar.getInstance();
      calendarStart.setTime(start);
      calendarStart.set(Calendar.HOUR, 0);
      calendarStart.set(Calendar.MINUTE, 0);
      calendarStart.set(Calendar.SECOND, 0);
      calendarStart.set(Calendar.MILLISECOND, 0);
      Calendar calendarEnd = Calendar.getInstance();
      calendarEnd.setTime(end);
      calendarEnd.set(Calendar.HOUR, 0);
      calendarEnd.set(Calendar.MINUTE, 0);
      calendarEnd.set(Calendar.SECOND, 0);
      calendarEnd.set(Calendar.MILLISECOND, 0);

      //obtain the list of days contained in the interval
      while (!calendarEnd.before(calendarStart)) {
         days.add(new Date(calendarStart.getTimeInMillis()));
         calendarStart.add(Calendar.DATE, 1);
      }

      for (int i = 0; i < clientHourlyRatesPeriodsSet.getChildCount(); i++) {
         dataRow = (XComponent) clientHourlyRatesPeriodsSet.getChild(i);
         //check to see if the start date and end date are contained in this period's interval
         if (dataRow.getOutlineLevel() == 1) {
            calendarStart.setTime(((XComponent) dataRow.getChild(PERIOD_START_DATE)).getDateValue());
            calendarEnd.setTime(((XComponent) dataRow.getChild(PERIOD_END_DATE)).getDateValue());
            calendarStart.set(Calendar.HOUR, 0);
            calendarStart.set(Calendar.MINUTE, 0);
            calendarStart.set(Calendar.SECOND, 0);
            calendarStart.set(Calendar.MILLISECOND, 0);
            calendarEnd.set(Calendar.HOUR, 0);
            calendarEnd.set(Calendar.MINUTE, 0);
            calendarEnd.set(Calendar.SECOND, 0);
            calendarEnd.set(Calendar.MILLISECOND, 0);
            while (!calendarEnd.before(calendarStart)) {
               Date date = new Date(calendarStart.getTimeInMillis());
               if (days.contains(date)) {
                  days.remove(date);
               }
               calendarStart.add(Calendar.DATE, 1);
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
}
