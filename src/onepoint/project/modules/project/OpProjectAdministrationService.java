/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpSubject;
import onepoint.project.modules.user.OpUser;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;

import java.sql.Date;
import java.util.*;


public class OpProjectAdministrationService extends OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpProjectAdministrationService.class, true);

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

   protected final static OpProjectErrorMap ERROR_MAP = new OpProjectErrorMap();

   /**
    * Query used to retrieve projects that have a certain name.
    */
   protected static final String PROJECT_NODE_NAME_QUERY_STRING = "select project from OpProjectNode project where project.Name = ?";

   private final static String RESOURCE_LIST = "resource_list";
   private final static String VERSIONS_SET = "versions_set";
   private final static String PORTFOLIO_LOCATOR = "PortfolioID";

   public XMessage insertProject(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

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
      if (portfolioLocator != null) {
         OpProjectNode portfolio = (OpProjectNode) broker.getObject(portfolioLocator);
         if (portfolio != null) {
            project.setSuperNode(portfolio);
         }
         else {
            project.setSuperNode(OpProjectAdministrationService.findRootPortfolio(broker));
         }
      }

      // Check manager access for portfolio
      if (!session.checkAccessLevel(broker, project.getSuperNode().getID(), OpPermission.MANAGER)) {
         logger.warn("ERROR: Insert access to portfolio denied; ID = " + project.getSuperNode().getID());
         broker.close();
         reply.setError(session.newError(ERROR_MAP, OpProjectError.UPDATE_ACCESS_DENIED));
         return reply;
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
      XComponent data_set = (XComponent) (request.getArgument(GOALS_SET));
      XComponent data_row;
      XComponent data_cell;
      int i;
      OpGoal goal;
      for (i = 0; i < data_set.getChildCount(); i++) {
         data_row = (XComponent) (data_set.getChild(i));
         goal = new OpGoal();
         goal.setProjectNode(project);
         data_cell = (XComponent) (data_row.getChild(0));
         goal.setCompleted(data_cell.getBooleanValue());
         data_cell = (XComponent) (data_row.getChild(1));
         goal.setName(data_cell.getStringValue());
         data_cell = (XComponent) (data_row.getChild(2));
         if (data_cell.getIntValue() < 0) {
            error = session.newError(ERROR_MAP, OpProjectError.PRIORITY_INCORRECT);
            reply.setError(error);
            finalizeSession(t, broker);
            return reply;
         }
         else {
            goal.setPriority((byte) data_cell.getIntValue());
         }

         broker.makePersistent(goal);
      }

      // Insert to dos
      data_set = (XComponent) (request.getArgument(TO_DOS_SET));
      OpToDo to_do;
      for (i = 0; i < data_set.getChildCount(); i++) {
         data_row = (XComponent) (data_set.getChild(i));
         to_do = new OpToDo();
         to_do.setProjectNode(project);
         data_cell = (XComponent) (data_row.getChild(0));
         to_do.setCompleted(data_cell.getBooleanValue());
         data_cell = (XComponent) (data_row.getChild(1));
         to_do.setName(data_cell.getStringValue());
         data_cell = (XComponent) (data_row.getChild(2));
         if (data_cell.getIntValue() < 0) {
            error = session.newError(ERROR_MAP, OpProjectError.PRIORITY_INCORRECT);
            reply.setError(error);
            finalizeSession(t, broker);
            return reply;
         }
         else {
            to_do.setPriority((byte) data_cell.getIntValue());
         }
         data_cell = (XComponent) (data_row.getChild(3));
         to_do.setDue(data_cell.getDateValue());
         broker.makePersistent(to_do);
      }

      //insert project assignments
      List assignedResources = (ArrayList) request.getArgument(RESOURCE_LIST);
      if (assignedResources != null && !assignedResources.isEmpty()) {
         OpResource resource;
         OpProjectNodeAssignment projectNodeAssignment;
         for (i = 0; i < assignedResources.size(); i++) {
            resource = (OpResource) (broker.getObject((String) assignedResources.get(i)));
            projectNodeAssignment = new OpProjectNodeAssignment();
            projectNodeAssignment.setResource(resource);
            projectNodeAssignment.setProjectNode(project);
            broker.makePersistent(projectNodeAssignment);
            insertContributorPermission(broker, project, resource);
         }
      }

      XComponent permission_set = (XComponent) project_data.get(OpPermissionSetFactory.PERMISSION_SET);
      boolean result = OpPermissionSetFactory.storePermissionSet(broker, project, permission_set);
      if (!result) {
         error = session.newError(ERROR_MAP, OpProjectError.PERMISSIONS_LEVEL_ERROR);
         reply.setError(error);
         broker.close();
         return reply;
      }


      t.commit();
      logger.debug("/OpProjectAdministrationService.insertProject()");
      broker.close();
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

   public XMessage updateProject(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

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

      // *** We could check if the fields have been modified (does this help or
      // not)?
      if (project == null) {
         logger.warn("ERROR: Could not find object with ID " + id_string);
         broker.close();
         return null;
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

      project.setName(projectName);
      project.setDescription((String) (project_data.get(OpProjectNode.DESCRIPTION)));
      project.setStart(start_date);
      project.setFinish(end_date);
      project.setBudget(budget);

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
      HashMap goal_map = new HashMap();
      Iterator goals = project.getGoals().iterator();
      OpGoal goal;
      while (goals.hasNext()) {
         goal = (OpGoal) (goals.next());
         goal_map.put(goal.locator(), goal);
      }
      XComponent data_set = (XComponent) (request.getArgument(GOALS_SET));
      XComponent data_row;
      XComponent data_cell;
      int i;
      boolean updated;
      for (i = 0; i < data_set.getChildCount(); i++) {
         data_row = (XComponent) (data_set.getChild(i));
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
               if (data_cell.getIntValue() < 0) {
                  error = session.newError(ERROR_MAP, OpProjectError.PRIORITY_INCORRECT);
                  reply.setError(error);
                  finalizeSession(t, broker);
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
            if (data_cell.getIntValue() < 0) {
               error = session.newError(ERROR_MAP, OpProjectError.PRIORITY_INCORRECT);
               reply.setError(error);
               finalizeSession(t, broker);
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

      // Update current to dos
      HashMap to_do_map = new HashMap();
      Iterator to_dos = project.getToDos().iterator();
      OpToDo to_do;
      while (to_dos.hasNext()) {
         to_do = (OpToDo) (to_dos.next());
         to_do_map.put(to_do.locator(), to_do);
      }
      data_set = (XComponent) (request.getArgument(TO_DOS_SET));
      for (i = 0; i < data_set.getChildCount(); i++) {
         data_row = (XComponent) (data_set.getChild(i));
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
               if (data_cell.getIntValue() < 0) {
                  error = session.newError(ERROR_MAP, OpProjectError.PRIORITY_INCORRECT);
                  reply.setError(error);
                  finalizeSession(t, broker);
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
            if (data_cell.getIntValue() < 0) {
               error = session.newError(ERROR_MAP, OpProjectError.PRIORITY_INCORRECT);
               reply.setError(error);
               finalizeSession(t, broker);
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
      locators = to_do_map.keySet().iterator();
      Set to_do_set = project.getToDos();
      while (locators.hasNext()) {
         to_do = (OpToDo) (to_do_map.get((String) (locators.next())));
         to_do_set.remove(to_do);
         broker.deleteObject(to_do);
      }

      //update project assignments
      List assignedResources = (ArrayList) request.getArgument(RESOURCE_LIST);
      reply = updateProjectAssignments(session, broker, project, assignedResources);
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
      boolean result = OpPermissionSetFactory.storePermissionSet(broker, project, permission_set);
      if (!result) {
         error = session.newError(ERROR_MAP, OpProjectError.PERMISSIONS_LEVEL_ERROR);
         reply.setError(error);
         broker.close();
         return reply;
      }

      t.commit();
      logger.debug("/OpProjectAdministrationService.updateProject()");
      broker.close();
      return null;
   }

   /**
    * Updates the already existent project assignments for the modified project.
    *
    * @param session           <code>OpProjectSession</code> the current session
    * @param broker            <code>OpBroker</code> used for performing business operations.
    * @param project           <code>OpProjectNode</code> representing the project which was edited.
    * @param assignedResources <code>List</code > of <code>String</code> representing assignment locators.
    * @return <code>XMessage</code>
    */
   private XMessage updateProjectAssignments(OpProjectSession session, OpBroker broker, OpProjectNode project, List assignedResources) {
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

      if (assignedResources != null && assignedResources.size() > 0) {
         for (int i = 0; i < assignedResources.size(); i++) {
            String resourceChoice = (String) assignedResources.get(i);
            String resourceLocator = OpLocator.parseLocator(resourceChoice).toString();
            if (!assignmentNodeMap.containsKey(resourceLocator)) { //a new assignment was added
               OpResource resource = (OpResource) broker.getObject(resourceLocator);
               OpProjectNodeAssignment assignment = new OpProjectNodeAssignment();
               assignment.setResource(resource);
               assignment.setProjectNode(project);
               broker.makePersistent(assignment);
               insertContributorPermission(broker, project, resource);
            }
            else {
               assignmentNodeMap.remove(resourceLocator);
               responsibleUsersMap.remove(resourceLocator);
            }
         }
      }
      // Remove outdated project node assignments if no activity assignments exist for the resource
      Iterator outDatedAssignments = assignmentNodeMap.values().iterator();
      while (outDatedAssignments.hasNext()) {
         OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) outDatedAssignments.next();
         int activityAssignmentsCounter = OpResourceService.getActivityAssignmentsCount(broker, assignment.getResource(), assignment.getProjectNode().getPlan());
         if (activityAssignmentsCounter > 0) {
            reply.setError(session.newError(ERROR_MAP, OpProjectError.ACTIVITY_ASSIGNMENTS_EXIST_ERROR));
            return reply;
         }
         else {
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
                  Integer counter = (Integer) broker.iterate(query).next();
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

   public XMessage deleteProjects(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

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
   private boolean hasWorkRecords(OpProjectNode project, OpBroker broker) {
      OpQuery query = broker.newQuery("select count(workrecord) from OpProjectPlan projectPlan join projectPlan.Activities activity join activity.Assignments assignment join assignment.WorkRecords workrecord where projectPlan.ID = ?");
      query.setLong(0, project.getPlan().getID());
      Integer workRecordNr = (Integer) broker.iterate(query).next();
      return (workRecordNr != null) && (workRecordNr.intValue() > 0);
   }

   public XMessage insertPortfolio(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

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
      boolean result = OpPermissionSetFactory.storePermissionSet(broker, portfolio, permission_set);
      if (!result) {
         error = session.newError(ERROR_MAP, OpProjectError.PERMISSIONS_LEVEL_ERROR);
         reply.setError(error);
         broker.close();
         return reply;
      }

      t.commit();

      logger.debug("/OpProjectAdministrationService.insertPortfolio()");
      broker.close();
      return reply;
   }

   public XMessage updatePortfolio(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

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
      boolean result = OpPermissionSetFactory.storePermissionSet(broker, portfolio, permission_set);
      if (!result) {
         error = session.newError(ERROR_MAP, OpProjectError.PERMISSIONS_LEVEL_ERROR);
         reply.setError(error);
         broker.close();
         return reply;
      }

      t.commit();

      logger.debug("/OpProjectAdministrationService.updatePortfolio()");

      broker.close();
      return reply;
   }

   public XMessage deletePortfolios(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;

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

   public XMessage moveProjectNode(XSession s, XMessage request) {
      OpProjectSession session = (OpProjectSession) s;
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
            projectNode.setSuperNode(portfolio);
            broker.updateObject(projectNode);
         }
      }

      tx.commit();
      broker.close();
      return reply;
   }


   public static OpProjectNode findRootPortfolio(OpBroker broker) {
      OpQuery query = broker
           .newQuery("select portfolio from OpProjectNode as portfolio where portfolio.Name = ? and portfolio.Type = ?");
      query.setString(0, OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME);
      query.setByte(1, OpProjectNode.PORTFOLIO);
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
           .newQuery("select min(activity.Start) from OpActivity as activity where activity.ProjectPlan.ID = ?");
      query.setLong(0, projectPlan.getID());
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
         Integer counter = (Integer) broker.iterate(query).next();
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
}
