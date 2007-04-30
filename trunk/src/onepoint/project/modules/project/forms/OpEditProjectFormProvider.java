/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.*;

public class OpEditProjectFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getServerLogger(OpEditProjectFormProvider.class);

   /**
    * Form field ids and parameter ids.
    */
   protected final static String TEMPLATE_FIELD = "TemplateField";

   private final static String PROJECT_EDIT_PROJECT = "project.EditProject";

   private final static String PROJECT_ID = "ProjectID";
   private final static String EDIT_MODE = "EditMode";
   private final static String ORIGINAL_START_DATE = "OriginalStartDate";
   private final static String GOALS_SET = "GoalsSet";
   private final static String TO_DOS_SET = "ToDosSet";
   private final static String PERMISSION_SET = "PermissionSet";
   private final static String ASSIGNED_RESOURCE_DATA_SET = "AssignedResourceDataSet";
   private final static String PROJECT_STATUS_DATA_SET = "ProjectStatusDataSet";
   private final static String PROJECT_STATUS_CHOICE = "StatusChoice";
   private final static String PROJECT_INFO = "project.Info";
   private final static String NO_STATUS = "NoStatus";
   private final static String WORKING_VERSION = "WorkingVersion";
   private final static String NULL_ID = "null";
   private final static String PERMISSIONS_TAB = "PermissionsTab";
   private final static String READ_ONLY_RESOURCES_SET = "ReadOnlyResourceDataSet";
   private final static String FORM_WORKING_VERSION_NUMBER = "WorkingVersionNumber";
   private final static String GOALS_TABLE_BOX = "GoalsTableBox";
   private final static String TODOS_TABLE_BOX = "ToDosTableBox";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Find project in database
      String id_string = (String) (parameters.get(OpProjectAdministrationService.PROJECT_ID));
      Boolean editMode = (Boolean) parameters.get(OpProjectAdministrationService.EDIT_MODE);

      logger.debug("OpEditProjectFormProvider.prepareForm(): " + id_string);

      OpBroker broker = ((OpProjectSession) session).newBroker();
      OpProjectNode project = (OpProjectNode) (broker.getObject(id_string));

      //set the orignal start date of the project
      form.findComponent(ORIGINAL_START_DATE).setDateValue(project.getStart());

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
      if (editMode.booleanValue() && (accessLevel < OpPermission.MANAGER)) {
         editMode = Boolean.FALSE;
      }

      if (editMode.booleanValue()) {
         XComponent readOnlyResources = form.findComponent(READ_ONLY_RESOURCES_SET);
         OpResourceDataSetFactory.fillReadOnlyResources(broker, session, readOnlyResources);
      }

      // Fill edit-user form with user data
      form.findComponent(PROJECT_ID).setStringValue(id_string);
      form.findComponent(EDIT_MODE).setBooleanValue(editMode.booleanValue());

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

      //Fill status data set
      Iterator statusIterator = OpProjectDataSetFactory.getProjectStatusIterator(broker);
      XComponent statusDataSet = form.findComponent(PROJECT_STATUS_DATA_SET);
      OpProjectStatus projectStatus = project.getStatus();
      String nullChoice = XValidator.choice(NULL_ID, session.getLocale().getResourceMap(PROJECT_EDIT_PROJECT).getResource(NO_STATUS).getText());
      XComponent row = new XComponent(XComponent.DATA_ROW);
      row.setStringValue(nullChoice);
      statusDataSet.addChild(row);
      int selectedIndex = -1;
      while (statusIterator.hasNext()) {
         OpProjectStatus status = (OpProjectStatus) statusIterator.next();
         row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(XValidator.choice(String.valueOf(status.locator()), status.getName()));
         statusDataSet.addChild(row);
         if (projectStatus != null && (projectStatus.getID() == status.getID())) {
            selectedIndex = row.getIndex();
         }
      }

      XComponent statusChoice = form.findComponent(PROJECT_STATUS_CHOICE);
      if (selectedIndex != -1) {
         statusChoice.setSelectedIndex(new Integer(selectedIndex));
      }
      else {
         if (projectStatus != null) {
            row = new XComponent(XComponent.DATA_ROW);
            row.setStringValue(XValidator.choice(String.valueOf(projectStatus.locator()), projectStatus.getName()));
            statusDataSet.addChild(row);
            statusChoice.setSelectedIndex(new Integer(row.getIndex()));
         }
      }
      statusChoice.setEnabled(editMode.booleanValue());

      // Fill in goals
      XComponent dataSet = form.findComponent(GOALS_SET);
      XComponent dataRow = null;
      XComponent dataCell = null;
      Iterator goals = project.getGoals().iterator();
      OpGoal goal = null;
      while (goals.hasNext()) {
         goal = (OpGoal) (goals.next());
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(goal.locator());
         dataSet.addChild(dataRow);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setBooleanValue(goal.getCompleted());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(goal.getName());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setIntValue(goal.getPriority());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
      }
      form.findComponent(GOALS_TABLE_BOX).setEditMode(editMode);
      //sort goals data set based on goal's name (data cell with index 1)
      dataSet.sort(1);

      // Fill in to dos
      dataSet = form.findComponent(TO_DOS_SET);
      Iterator to_dos = project.getToDos().iterator();
      OpToDo to_do = null;
      while (to_dos.hasNext()) {
         to_do = (OpToDo) (to_dos.next());
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(to_do.locator());
         dataSet.addChild(dataRow);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setBooleanValue(to_do.getCompleted());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(to_do.getName());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setIntValue(to_do.getPriority());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDateValue(to_do.getDue());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
      }
      form.findComponent(TODOS_TABLE_BOX).setEditMode(editMode);
      //sort to dos data set based on to do's name (data cell with index 1)
      dataSet.sort(1);

      if (!editMode.booleanValue()) {
         name.setEnabled(false);
         desc.setEnabled(false);
         start.setEnabled(false);
         end.setEnabled(false);
         budget.setEnabled(false);
         form.findComponent("ResourcesTable").setEditMode(false);
         form.findComponent("PermissionToolPanel").setVisible(false);
         form.findComponent("ResourcesToolPanel").setVisible(false);
         form.findComponent("GoalsToolPanel").setVisible(false);
         form.findComponent("TasksToolPanel").setVisible(false);
         form.findComponent(GOALS_TABLE_BOX).setEnabled(false);
         form.findComponent(TODOS_TABLE_BOX).setEnabled(false);
         form.findComponent("Cancel").setVisible(false);
         form.findComponent("ProgressTracked").setEnabled(false);
         form.findComponent("CalculationMode").setEnabled(false);

         String title = session.getLocale().getResourceMap(PROJECT_INFO).getResource("InfoProject").getText();
         form.setText(title);
      }

      //fill the version of the project
      boolean isAdministrator = (session.getAdministratorID() == session.getUserID()) ||
           session.checkAccessLevel(broker, project.getID(), OpPermission.ADMINISTRATOR);
      boolean isButtonVisible = editMode.booleanValue() && isAdministrator && (project.getPlan() != null)
           && (project.getPlan().getVersions().size() > 0);
      if (project.getPlan() != null) {
         XLocalizer userObjectsLocalizer = new XLocalizer();
         userObjectsLocalizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));
         fillVersionsDataSet(form, project.getPlan(), userObjectsLocalizer, session);
      }
      form.findComponent("RemoveVersionButton").setVisible(isButtonVisible);

      if (OpInitializer.isMultiUser()) {
         // Locate permission data set in form
         XComponent permissionSet = form.findComponent(PERMISSION_SET);
         OpPermissionSetFactory.retrievePermissionSet(session, broker, project.getPermissions(), permissionSet,
              OpProjectModule.PROJECT_ACCESS_LEVELS, session.getLocale());
         OpPermissionSetFactory.administratePermissionTab(form, editMode.booleanValue(), accessLevel);
      }
      else {
         form.findComponent(PERMISSIONS_TAB).setHidden(true);
      }

      Iterator assignments = project.getAssignments().iterator();
      dataSet = form.findComponent(ASSIGNED_RESOURCE_DATA_SET);

      //fill assigned resources set
      while (assignments.hasNext()) {
         OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) assignments.next();
         OpResource resource = assignment.getResource();
         Double internalRate = assignment.getHourlyRate();
         Double externalRate = assignment.getExternalRate();
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(resource.locator(), resource.getName()));

         //0 - resource name
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(resource.getName());
         dataCell.setEnabled(editMode);
         dataRow.addChild(dataCell);

         //1 - resource description
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(resource.getDescription());
         dataCell.setEnabled(editMode);
         dataRow.addChild(dataCell);

         //2 - adjust rates
         dataCell = new XComponent(XComponent.DATA_CELL);
         if(internalRate != null || externalRate != null){
            dataCell.setBooleanValue(true);
         }
         else{
            dataCell.setBooleanValue(false);
         }
         dataCell.setEnabled(editMode);
         dataRow.addChild(dataCell);

         //3 - internal rate
         dataCell = new XComponent(XComponent.DATA_CELL);
         if(internalRate != null){
            dataCell.setDoubleValue(internalRate);
            dataCell.setEnabled(editMode);
         }
         else{
            dataCell.setEnabled(false);
         }
         dataRow.addChild(dataCell);

         //4 - external rate
         dataCell = new XComponent(XComponent.DATA_CELL);
         if(externalRate != null){
            dataCell.setDoubleValue(externalRate);
            dataCell.setEnabled(editMode);
         }
         else{
            dataCell.setEnabled(false);
         }
         dataRow.addChild(dataCell);

         dataSet.addChild(dataRow);
      }
      //sort assigned resources
      dataSet.sort();

      broker.close();
   }

   /**
    * Fills a data-set with all the versions of a project.
    *
    * @param form                 a <code>XComponent(FORM)</code> representing the current form.
    * @param projectPlan          a <code>OpProjectPlan</code> representing a project's plan.
    * @param userObjectsLocalizer a <code>XLocalizer</code> representing a localizer that is used to get the i18n display names.
    */
   private void fillVersionsDataSet(XComponent form, OpProjectPlan projectPlan, XLocalizer userObjectsLocalizer, OpProjectSession session) {
      form.findComponent(FORM_WORKING_VERSION_NUMBER).setStringValue(String.valueOf(OpProjectAdministrationService.WORKING_VERSION_NUMBER));

      XComponent versionsDataSet = form.findComponent("VersionsSet");
      Map rowsMap = new TreeMap();

      //add the version nrs in ascending order
      Set planVersions = projectPlan.getVersions();
      Iterator it = planVersions.iterator();
      XComponent workingDataRow = new XComponent(XComponent.DATA_ROW);

      while (it.hasNext()) {
         OpProjectPlanVersion version = (OpProjectPlanVersion) it.next();
         
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);

         //version id - 0
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         if (version.getVersionNumber() == OpProjectAdministrationService.WORKING_VERSION_NUMBER) {
            dataCell.setStringValue(String.valueOf(version.getVersionNumber()));
         }
         else{
            dataCell.setStringValue(OpLocator.locatorString(version));
         }
         dataRow.addChild(dataCell);

         //version number - 1
         int versionNr = version.getVersionNumber();
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (version.getVersionNumber() == OpProjectAdministrationService.WORKING_VERSION_NUMBER) {
            dataCell.setStringValue(session.getLocale().getResourceMap(PROJECT_EDIT_PROJECT).getResource(WORKING_VERSION).getText());
         }
         else{
            dataCell.setStringValue(String.valueOf(versionNr));
         }
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

         if (version.getVersionNumber() == OpProjectAdministrationService.WORKING_VERSION_NUMBER) {
            workingDataRow = dataRow;
         }
         else{
            rowsMap.put(new Integer(versionNr), dataRow);
         }
      }

      Integer[] versionNumbers = (Integer[]) rowsMap.keySet().toArray(new Integer[0]);
      //add the working data row first if exists such a row
      if(workingDataRow.getChildCount() > 0){
         versionsDataSet.addChild((XView) workingDataRow);
      }
      for (int i = versionNumbers.length - 1; i >= 0; i--) {
         Integer versionNumber = versionNumbers[i];
         versionsDataSet.addChild((XView) rowsMap.get(versionNumber));
      }
   }
}