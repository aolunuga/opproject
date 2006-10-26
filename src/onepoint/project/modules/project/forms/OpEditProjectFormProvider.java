/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.XView;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.*;

public class OpEditProjectFormProvider implements XFormProvider {

   private static XLog logger = XLogFactory.getLogger(OpEditProjectFormProvider.class,true);

   /**
    * Form field ids and parameter ids.
    */
   public final static String PROJECT_ID = "ProjectID";
   public final static String EDIT_MODE = "EditMode";
   public final static String GOALS_SET = "GoalsSet";
   public final static String TO_DOS_SET = "ToDosSet";
   public final static String PERMISSION_SET = "PermissionSet";
   public final static String ASSIGNED_RESOURCE_DATA_SET = "AssignedResourceDataSet";
   public final static String TEMPLATE_FIELD = "TemplateField";

   private final String PROJECT_INFO = "project.Info";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Find project in database
      String id_string = (String) (parameters.get(OpProjectAdministrationService.PROJECT_ID));
      Boolean edit_mode = (Boolean) parameters.get(OpProjectAdministrationService.EDIT_MODE);

      logger.debug("OpEditProjectFormProvider.prepareForm(): " + id_string);

      OpBroker broker = session.newBroker();
      OpProjectNode project = (OpProjectNode) (broker.getObject(id_string));

      //disable templates related stuff
      form.findComponent(TEMPLATE_FIELD).setEnabled(false);

      XComponent calculationModeComponent = form.findComponent(OpProjectPlan.CALCULATION_MODE);
      if (project.getPlan().getCalculationMode() == OpProjectPlan.EFFORT_BASED) {
         calculationModeComponent.setBooleanValue(true);
      }
      else {
         calculationModeComponent.setBooleanValue(false);
      }

      XComponent progressTrackedComponent = form.findComponent(OpProjectPlan.PROGRESS_TRACKED);
      progressTrackedComponent.setBooleanValue(project.getPlan().getProgressTracked());
      progressTrackedComponent.setEnabled(false);

      // Downgrade edit mode to view mode if no manager access
      byte accessLevel = session.effectiveAccessLevel(broker, project.getID());
      if (edit_mode.booleanValue() && (accessLevel < OpPermission.MANAGER))
         edit_mode = Boolean.FALSE;

      // Fill edit-user form with user data
      form.findComponent(PROJECT_ID).setStringValue(id_string);
      form.findComponent(EDIT_MODE).setBooleanValue(edit_mode.booleanValue());

      XComponent name = form.findComponent(OpProjectNode.NAME);
      name.setStringValue(project.getName());
      XComponent desc = form.findComponent(OpProjectNode.DESCRIPTION);
      desc.setStringValue(project.getDescription());
      XComponent start = form.findComponent(OpProjectNode.START);
      start.setDateValue(project.getStart());
      XComponent end = form.findComponent(OpProjectNode.FINISH);
      end.setDateValue(project.getFinish());
      XComponent budget = form.findComponent(OpProjectNode.BUDGET);
      budget.setDoubleValue(project.getBudget());

      // Fill in goals
      XComponent data_set = form.findComponent(GOALS_SET);
      XComponent data_row = null;
      XComponent data_cell = null;
      Iterator goals = project.getGoals().iterator();
      OpGoal goal = null;
      while (goals.hasNext()) {
         goal = (OpGoal) (goals.next());
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setStringValue(goal.locator());
         data_set.addChild(data_row);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setBooleanValue(goal.getCompleted());
         data_cell.setEnabled(true);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setStringValue(goal.getName());
         data_cell.setEnabled(true);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setIntValue(goal.getPriority());
         data_cell.setEnabled(true);
         data_row.addChild(data_cell);
      }
      //sort goals data set based on goal's name (data cell with index 1)
      data_set.sort(1);

      // Fill in to dos
      data_set = form.findComponent(TO_DOS_SET);
      Iterator to_dos = project.getToDos().iterator();
      OpToDo to_do = null;
      while (to_dos.hasNext()) {
         to_do = (OpToDo) (to_dos.next());
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setStringValue(to_do.locator());
         data_set.addChild(data_row);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setBooleanValue(to_do.getCompleted());
         data_cell.setEnabled(true);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setStringValue(to_do.getName());
         data_cell.setEnabled(true);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setIntValue(to_do.getPriority());
         data_cell.setEnabled(true);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDateValue(to_do.getDue());
         data_cell.setEnabled(true);
         data_row.addChild(data_cell);
      }
      //sort to dos data set based on to do's name (data cell with index 1)
      data_set.sort(1);

      if (!edit_mode.booleanValue()) {
         name.setEnabled(false);
         desc.setEnabled(false);
         start.setEnabled(false);
         end.setEnabled(false);
         budget.setEnabled(false);
         form.findComponent("PermissionToolPanel").setVisible(false);
         form.findComponent("ResourcesToolPanel").setVisible(false);
         form.findComponent("GoalsToolPanel").setVisible(false);
         form.findComponent("TasksToolPanel").setVisible(false);
         form.findComponent("GoalsTableBox").setEnabled(false);
         form.findComponent("ToDosTableBox").setEnabled(false);
         form.findComponent("Cancel").setVisible(false);
         form.findComponent("ProgressTracked").setEnabled(false);
         form.findComponent("CalculationMode").setEnabled(false);

         String title = session.getLocale().getResourceMap(PROJECT_INFO).getResource("InfoProject").getText();
         form.setText(title);
      }

      //fill the version of the project
      boolean isAdministrator = (session.getAdministratorID() == session.getUserID()) ||
           session.checkAccessLevel(broker, project.getID(), OpPermission.ADMINISTRATOR);
      boolean isButtonVisible = edit_mode.booleanValue() && isAdministrator && (project.getPlan() != null)
           && (project.getPlan().getVersions().size() > 0);
      if (project.getPlan() != null) {
         XLocalizer userObjectsLocalizer = new XLocalizer();
         userObjectsLocalizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));
         fillVersionsDataSet(form, project.getPlan(), userObjectsLocalizer);
      }
      form.findComponent("RemoveVersionButton").setVisible(isButtonVisible);

      // Locate permission data set in form
      XComponent permissionSet = form.findComponent(PERMISSION_SET);

      OpPermissionSetFactory.retrievePermissionSet(session, broker, project.getPermissions(), permissionSet,
            OpProjectModule.PROJECT_ACCESS_LEVELS, session.getLocale());
      OpPermissionSetFactory.administratePermissionTab(form, edit_mode.booleanValue(), accessLevel);

      Iterator assignments = project.getAssignments().iterator();
      data_set = form.findComponent(ASSIGNED_RESOURCE_DATA_SET);
      //fill assigned resources set
      while(assignments.hasNext()){
         OpProjectNodeAssignment assignment = (OpProjectNodeAssignment)assignments.next();
         OpResource resource = assignment.getResource();
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
         data_set.addChild(data_row);
      }
      //sort assigned resources
      data_set.sort();

      broker.close();
   }


   /**
    * Fills a data-set with all the versions of a project.
    * @param form a <code>XComponent(FORM)</code> representing the current form.
    * @param projectPlan a <code>OpProjectPlan</code> representing a project's plan.
    * @param userObjectsLocalizer a <code>XLocalizer</code> representing a localizer that is used to get the i18n display names.
    */
   private void fillVersionsDataSet(XComponent form, OpProjectPlan projectPlan, XLocalizer userObjectsLocalizer) {
      XComponent versionsDataSet = form.findComponent("VersionsSet");
      Map rowsMap = new TreeMap();

      //add the version nrs in ascending order
      Set planVersions = projectPlan.getVersions();
      Iterator it = planVersions.iterator();
      while (it.hasNext()) {
         OpProjectPlanVersion version = (OpProjectPlanVersion) it.next();

         //filter out working version
         if (version.getVersionNumber() == OpProjectAdministrationService.WORKING_VERSION_NUMBER) {
            continue;
         }

         XComponent dataRow = new XComponent(XComponent.DATA_ROW);

         //version id - 0
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(OpLocator.locatorString(version));
         dataRow.addChild(dataCell);

         //version number - 1
         int versionNr = version.getVersionNumber();
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setIntValue(versionNr);
         dataRow.addChild(dataCell);

         //created by - 2
         OpUser creator = version.getCreator();
         String createdBy = userObjectsLocalizer.localize(creator.getDisplayName());
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(createdBy);
         dataRow.addChild(dataCell);

         //created on - 3
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDateValue(version.getCreated());
         dataRow.addChild(dataCell);

         rowsMap.put(new Integer(versionNr), dataRow);
      }

      Integer[] versionNumbers = (Integer[]) rowsMap.keySet().toArray(new Integer[0]);
      for (int i = versionNumbers.length - 1; i >= 0; i--) {
         Integer versionNumber = versionNumbers[i];
         versionsDataSet.addChild((XView) rowsMap.get(versionNumber));
      }
   }
}
