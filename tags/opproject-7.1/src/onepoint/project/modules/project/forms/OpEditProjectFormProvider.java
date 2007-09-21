/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.util.*;

public class OpEditProjectFormProvider implements XFormProvider {

   /**
    * Form field ids and parameter ids.
    */
   protected final static String TEMPLATE_FIELD = "TemplateField";
   protected final static String ASSIGNED_RESOURCE_DATA_SET = "AssignedResourceDataSet";
   protected final static String ORIGINAL_RESOURCE_DATA_SET = "OriginalResourceDataSet";

   protected final static String ADJUST_RATES_COLUMN = "AdjustRatesColumn";
   protected final static String INTERNAL_RATES_COLUMN = "InternalRatesColumn";
   protected final static String EXTENAL_RATES_COLUMN = "ExternalRatesColumn";
   protected final static String BASELINE_COLUMN = "BaselineColumn";

   private final static String PROJECT_EDIT_PROJECT = "project.EditProject";
   private final static String PROJECT_ID = "ProjectID";
   private final static String EDIT_MODE = "EditMode";
   private final static String ORIGINAL_START_DATE = "OriginalStartDate";
   private final static String GOALS_SET = "GoalsSet";
   private final static String TO_DOS_SET = "ToDosSet";
   private final static String PERMISSION_SET = "PermissionSet";
   private final static String ATTACHMENTS_SET = "AttachmentSet";
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
   private final static String TODAY_DATE_FIELD = "Today";
   private final static String END_OF_YEAR_DATE_FIELD = "EndOfYear";
   private final static String GOALS_TOOLS_PANEL = "GoalsToolPanel";
   private final static String CANCEL = "Cancel";
   private final static String TAKS_TOOL_PANEL = "TasksToolPanel";
   private final static String RESOURCES_TABLE = "ResourcesTable";
   private final static String RESOURCE_TOOL_PANEL = "ResourcesToolPanel";
   private final static String PERMISSION_TOOL_PANEL = "PermissionToolPanel";
   private final static String REMOVE_VERSION_BUTTON = "RemoveVersionButton";
   private final static String VERSION_DATA_SET = "VersionsSet";
   private final static String PROJECT_INFO_RESOURCE = "InfoProject";

   private final static String ADD_DOCUMENT_BUTTON = "AddDocumentButton";
   private final static String ADD_URL_BUTTON = "AddURLButton";
   private final static String REMOVE_ATTACHMENT_BUTTON = "RemoveAttachmentButton";
   private final static String ATTACHMENTS_TOOL_PANEL = "AttachmentsToolPanel";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // Find project in database
      String id_string = (String) (parameters.get(OpProjectAdministrationService.PROJECT_ID));
      OpProjectNode project = (OpProjectNode) (broker.getObject(id_string));
      form.findComponent(PROJECT_ID).setStringValue(id_string);

      // Downgrade edit mode to view mode if no manager access
      Boolean editMode = (Boolean) parameters.get(OpProjectAdministrationService.EDIT_MODE);
      byte accessLevel = session.effectiveAccessLevel(broker, project.getID());
      if (editMode && (accessLevel < OpPermission.MANAGER)) {
         editMode = Boolean.FALSE;
      }
      form.findComponent(EDIT_MODE).setBooleanValue(editMode);

      //update components which are not project-related
      this.updateComponentsProjectUnrelated(session, broker, form, editMode);

      //update components which are project related
      this.fillDataFromProject(form, project, broker, session, editMode);

      broker.close();
   }

   /**
    * Sets the data in the form's fields by taking the information out of the requested project.
    *
    * @param form     a <code>XComponent(FORM)</code> representing the edit projec form.
    * @param project  a <code>OpProjectNode</code> representing the project being edited.
    * @param broker   a <code>OpBroker</code> used for business operations.
    * @param session  a <code>OpProjectSession</code> representing the current server session.
    * @param editMode a <code>boolean</code> indicating whether we are editing or view-ing a project.
    */
   private void fillDataFromProject(XComponent form, OpProjectNode project, OpBroker broker, OpProjectSession session, Boolean editMode) {

      boolean isAdministrator = (session.getAdministratorID() == session.getUserID()) ||
           session.checkAccessLevel(broker, project.getID(), OpPermission.ADMINISTRATOR);

      //set the orignal start date of the project
      form.findComponent(ORIGINAL_START_DATE).setDateValue(project.getStart());

      //set the calculation mode
      XComponent calculationModeComponent = form.findComponent(OpProjectPlan.CALCULATION_MODE);
      calculationModeComponent.setBooleanValue(project.getPlan().getCalculationMode() == OpProjectPlan.EFFORT_BASED);
      calculationModeComponent.setEnabled(editMode);

      //progress tracked - can't be changed at edit time (disabled)
      XComponent progressTrackedComponent = form.findComponent(OpProjectPlan.PROGRESS_TRACKED);
      progressTrackedComponent.setBooleanValue(project.getPlan().getProgressTracked());
      progressTrackedComponent.setEnabled(false);

      //name
      XComponent name = form.findComponent(OpProjectNode.NAME);
      name.setStringValue(project.getName());
      name.setEnabled(editMode);

      //description
      XComponent desc = form.findComponent(OpProjectNode.DESCRIPTION);
      desc.setStringValue(project.getDescription());
      desc.setEnabled(editMode);

      //start
      XComponent start = form.findComponent(OpProjectNode.START);
      start.setDateValue(project.getStart());
      start.setEnabled(editMode);

      //finish
      XComponent end = form.findComponent(OpProjectNode.FINISH);
      end.setDateValue(project.getFinish());
      end.setEnabled(editMode);

      //budget
      XComponent budget = form.findComponent(OpProjectNode.BUDGET);
      budget.setDoubleValue(project.getBudget());
      budget.setEnabled(editMode && isAdministrator);

      //priority
      XComponent priority = form.findComponent(OpProjectNode.PRIORITY);
      priority.setIntValue(project.getPriority());
      priority.setEnabled(editMode);

      //probability
      XComponent probability = form.findComponent(OpProjectNode.PROBABILITY);
      probability.setIntValue(project.getProbability());
      probability.setEnabled(editMode);

      //archived
      XComponent archived = form.findComponent(OpProjectNode.ARCHIVED);
      archived.setBooleanValue(project.getArchived());
      archived.setEnabled(editMode);

      //statuses
      String noStatusText = XValidator.choice(NULL_ID, session.getLocale().getResourceMap(PROJECT_EDIT_PROJECT).getResource(NO_STATUS).getText());
      this.fillStatuses(broker, form, noStatusText, project.getStatus(), editMode && isAdministrator);

      //goals
      this.fillGoals(form, project, editMode);

      //to dos
      this.fillToDos(form, project, editMode);

      //fill the version of the project
      this.fillProjectPlanVersions(form, editMode && isAdministrator, project, session);

      //fill the resources & hourly rates periods data set
      this.fillResources(form, project, editMode);

      //fill permissions
      this.fillPermissions(session, broker, form, project, editMode);

      //fill attachments
      this.fillAttachments(form, project, editMode);
   }

   /**
    * Fills the permissions for the project being edited.
    *
    * @param session  a <code>OpProjectSession</code> representing the server session.
    * @param broker   a <code>OpBroker</code> used for business operations.
    * @param form     a <code>XComponent(FORM)</code> representing the edit project form.
    * @param project  a <code>OpProjectNode</code> the project being edited.
    * @param editMode a <code>boolean</code> indicating whether the operation is view or edit.
    */
   private void fillPermissions(OpProjectSession session, OpBroker broker, XComponent form,
        OpProjectNode project, boolean editMode) {
      if (OpEnvironmentManager.isMultiUser()) {
         byte accessLevel = session.effectiveAccessLevel(broker, project.getID());
         XComponent permissionSet = form.findComponent(PERMISSION_SET);
         OpPermissionSetFactory.retrievePermissionSet(session, broker, project.getPermissions(), permissionSet,
              OpProjectModule.PROJECT_ACCESS_LEVELS, session.getLocale());
         OpPermissionSetFactory.administratePermissionTab(form, editMode, accessLevel);
      }
      else {
         form.findComponent(PERMISSIONS_TAB).setHidden(true);
      }
      form.findComponent(PERMISSION_TOOL_PANEL).setVisible(editMode);
   }

   /**
    * Fills the goals for the edited project.
    *
    * @param form     a <code>XComponent(FORM)</code> representing the edit project form.
    * @param project  a <code>OpProjectNode</code> representing the project being edited.
    * @param editMode a <code>boolean</code> indicating whether an edit or view is performed.
    */
   private void fillGoals(XComponent form, OpProjectNode project, boolean editMode) {

      XComponent dataSet = form.findComponent(GOALS_SET);
      XComponent dataRow;
      XComponent dataCell;
      for (OpGoal goal : project.getGoals()) {
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
      //sort goals data set based on goal's name (data cell with index 1)
      dataSet.sort(1);

      form.findComponent(GOALS_TABLE_BOX).setEditMode(editMode);
      form.findComponent(GOALS_TABLE_BOX).setEnabled(editMode);
      form.findComponent(GOALS_TOOLS_PANEL).setVisible(editMode);
   }

   /**
    * Fills the edit form with projec versions.
    *
    * @param form     a <code>XComponent(FORM)</code> representing the edit project form.
    * @param editMode a <code>boolean</code> indicating whether it's and edit or view operation.
    * @param project  a <code>OpProjectNode</code> representing the project being edited.
    * @param session  a <code>OpProjectSession</code> representing the server session.
    */
   private void fillProjectPlanVersions(XComponent form, boolean editMode, OpProjectNode project, OpProjectSession session) {
      boolean isButtonVisible = editMode && (project.getPlan() != null) && (project.getPlan().getVersions().size() > 0);
      if (project.getPlan() != null) {
         this.fillVersionsDataSet(form, editMode, project.getPlan(), session);
      }
      form.findComponent(REMOVE_VERSION_BUTTON).setVisible(isButtonVisible);
   }

   /**
    * Fills the project statuses and modifies the form components accordingly.
    *
    * @param broker        a <code>OpBroker</code> used for persistence operations.
    * @param form          a <code>XComponent(FORM)</code> representing the edit project form.
    * @param noStatusText  a <code>String</code> representing the value "No Status".
    * @param projectStatus a <code>OpProjectStatus</code> object.
    * @param editable      a <code>boolean</code> indicating whether the status choice should
    *                      be editable or not.
    */
   private void fillStatuses(OpBroker broker, XComponent form, String noStatusText, OpProjectStatus projectStatus, boolean editable) {

      XComponent statusDataSet = form.findComponent(PROJECT_STATUS_DATA_SET);
      //add the no status row
      XComponent row = new XComponent(XComponent.DATA_ROW);
      row.setStringValue(noStatusText);
      statusDataSet.addChild(row);

      int selectedIndex = -1;
      Iterator statusIterator = OpProjectDataSetFactory.getProjectStatusIterator(broker);
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
         statusChoice.setSelectedIndex(selectedIndex);
      }
      else {
         if (projectStatus != null) {
            row = new XComponent(XComponent.DATA_ROW);
            row.setStringValue(XValidator.choice(String.valueOf(projectStatus.locator()), projectStatus.getName()));
            statusDataSet.addChild(row);
            statusChoice.setSelectedIndex(row.getIndex());
         }
      }
      statusChoice.setEnabled(editable);
   }

   /**
    * Updates the form's components which are independent of the
    * selected project according to the edit mode.
    *
    * @param session  a <code>OpProjectSession</code> representing the server session
    * @param broker   a <code>OpBroker</code> used for business operations.
    * @param form     a <code>XComponent(FORM)</code> representing the edit project form.
    * @param editMode a <code>boolean</code> indicating whether we are in edit or info mode.
    */
   private void updateComponentsProjectUnrelated(OpProjectSession session, OpBroker broker, XComponent form, boolean editMode) {

      if (editMode) {
         XComponent readOnlyResources = form.findComponent(READ_ONLY_RESOURCES_SET);
         OpResourceDataSetFactory.fillReadOnlyResources(broker, session, readOnlyResources);
      }
      else {
         String title = session.getLocale().getResourceMap(PROJECT_INFO).getResource(PROJECT_INFO_RESOURCE).getText();
         form.setText(title);
      }

      //set the cancel button
      form.findComponent(CANCEL).setVisible(editMode);

      //disable resource rates / project
      form.findComponent(ADJUST_RATES_COLUMN).setHidden(true);
      form.findComponent(INTERNAL_RATES_COLUMN).setHidden(true);
      form.findComponent(EXTENAL_RATES_COLUMN).setHidden(true);
      form.findComponent(BASELINE_COLUMN).setHidden(true);

      //disable templates related stuff
      form.findComponent(TEMPLATE_FIELD).setEnabled(false);

      //set the date of today and end of year on the form
      form.findComponent(TODAY_DATE_FIELD).setDateValue(XCalendar.today());
      form.findComponent(END_OF_YEAR_DATE_FIELD).setDateValue(XCalendar.lastDayOfYear());
   }

   /**
    * Fills a data-set with all the versions of a project.
    *
    * @param form        a <code>XComponent(FORM)</code> representing the current form.
    * @param editMode    a <code>boolean</code> representing the edit mode of the form.
    * @param projectPlan a <code>OpProjectPlan</code> representing a project's plan.
    * @param session     a <code>OpProjectSession</code> the project session
    */
   private void fillVersionsDataSet(XComponent form, boolean editMode, OpProjectPlan projectPlan, OpProjectSession session) {
      XLocalizer userObjectsLocalizer = new XLocalizer();
      userObjectsLocalizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));

      form.findComponent(FORM_WORKING_VERSION_NUMBER).setStringValue(String.valueOf(OpProjectPlan.WORKING_VERSION_NUMBER));

      XComponent versionsDataSet = form.findComponent(VERSION_DATA_SET);
      Map<Integer, XComponent> rowsMap = new TreeMap<Integer, XComponent>();

      //add the version nrs in ascending order
      XComponent workingDataRow = new XComponent(XComponent.DATA_ROW);

      Set planVersions = projectPlan.getVersions();
      Iterator it = planVersions.iterator();
      while (it.hasNext()) {
         OpProjectPlanVersion version = (OpProjectPlanVersion) it.next();
         int versionNr = version.getVersionNumber();
         XComponent dataRow = this.createProjectVersionDataRow(session, version,
              userObjectsLocalizer, editMode);
         if (version.getVersionNumber() == OpProjectPlan.WORKING_VERSION_NUMBER) {
            workingDataRow = dataRow;
         }
         else {
            rowsMap.put(versionNr, dataRow);
         }
      }

      Integer[] versionNumbers = rowsMap.keySet().toArray(new Integer[0]);
      //add the working data row first if exists such a row
      if (workingDataRow.getChildCount() > 0) {
         versionsDataSet.addChild(workingDataRow);
      }
      for (int i = versionNumbers.length - 1; i >= 0; i--) {
         Integer versionNumber = versionNumbers[i];
         versionsDataSet.addChild(rowsMap.get(versionNumber));
      }
   }

   /**
    * Creates a data row containing the information for a project plan version.
    *
    * @param session              a <code>OpProjectSession</code> representing the server session.
    * @param version              a <code>OpProjectPlanVersion</code> the version for which to create the data-row.
    * @param editMode             a <code>boolean</code> indicating whether we are viewing or editing the project.
    * @param userObjectsLocalizer a <code>XLocalizer</code> representing a localizer that is used to get the i18n display names.
    * @return a <code>XComponent(DATA_ROW)</code> the data-row for the plan version.
    */
   protected XComponent createProjectVersionDataRow(OpProjectSession session, OpProjectPlanVersion version,
        XLocalizer userObjectsLocalizer, boolean editMode) {
      int versionNr = version.getVersionNumber();
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);

      //version id - 0
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      if (version.getVersionNumber() == OpProjectPlan.WORKING_VERSION_NUMBER) {
         dataCell.setStringValue(String.valueOf(version.getVersionNumber()));
      }
      else {
         dataCell.setStringValue(OpLocator.locatorString(version));
      }
      dataRow.addChild(dataCell);

      //version number - 1
      dataCell = new XComponent(XComponent.DATA_CELL);
      if (version.getVersionNumber() == OpProjectPlan.WORKING_VERSION_NUMBER) {
         dataCell.setStringValue(session.getLocale().getResourceMap(PROJECT_EDIT_PROJECT).getResource(WORKING_VERSION).getText());
      }
      else {
         dataCell.setStringValue(String.valueOf(versionNr));
      }
      dataRow.addChild(dataCell);

      //created by - 2
      String creator = version.getCreator();
      String createdBy = userObjectsLocalizer.localize(creator);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(createdBy);
      dataRow.addChild(dataCell);

      //created on - 3
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDateValue(version.getCreated());
      dataRow.addChild(dataCell);

      //empty row - 4 (will be overridden in team edition)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataRow.addChild(dataCell);

      return dataRow;
   }

   /**
    * Fills the attachments for the edited project.
    *
    * @param form     a <code>XComponent(FORM)</code> representing the edit project form.
    * @param project  a <code>OpProjectNode</code> representing the project being edited.
    * @param editMode a <code>boolean</code> indicating whether an edit or view is performed.
    */
   private void fillAttachments(XComponent form, OpProjectNode project, boolean editMode) {

      List<List> attachmentList = new ArrayList<List>();
      OpActivityDataSetFactory.retrieveAttachments(project.getAttachments(), attachmentList);

      XComponent attachmentSet = form.findComponent(ATTACHMENTS_SET);
      OpAttachmentDataSetFactory.fillAttachmentsDataSet(attachmentList, attachmentSet);

      form.findComponent(ADD_DOCUMENT_BUTTON).setEnabled(editMode);
      form.findComponent(ADD_URL_BUTTON).setEnabled(editMode);
      form.findComponent(REMOVE_ATTACHMENT_BUTTON).setEnabled(editMode);
      form.findComponent(ATTACHMENTS_TOOL_PANEL).setVisible(editMode);
   }

   /**
    * Fills the goals for the edited project.
    *
    * @param form     a <code>XComponent(FORM)</code> representing the edit project form.
    * @param project  a <code>OpProjectNode</code> representing the project being edited.
    * @param editMode a <code>boolean</code> indicating whether an edit or view is performed.
    */
   private void fillToDos(XComponent form, OpProjectNode project, boolean editMode) {
      XComponent dataSet = form.findComponent(TO_DOS_SET);
      XComponent dataRow;
      XComponent dataCell;

      for (OpToDo toDo : project.getToDos()) {
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(toDo.locator());
         dataSet.addChild(dataRow);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setBooleanValue(toDo.getCompleted());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(toDo.getName());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setIntValue(toDo.getPriority());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDateValue(toDo.getDue());
         dataCell.setEnabled(true);
         dataRow.addChild(dataCell);
      }
      //sort to dos data set based on to do's name (data cell with index 1)
      dataSet.sort(1);

      form.findComponent(TAKS_TOOL_PANEL).setVisible(editMode);
      form.findComponent(TODOS_TABLE_BOX).setEnabled(editMode);
      form.findComponent(TODOS_TABLE_BOX).setEditMode(editMode);
   }

   /**
    * Fills the resources for the edited project.
    *
    * @param form     a <code>XComponent(FORM)</code> representing the edit project form.
    * @param project  a <code>OpProjectNode</code> representing the project being edited.
    * @param editMode a <code>boolean</code> indicating whether an edit or view is performed.
    */
   private void fillResources(XComponent form, OpProjectNode project, boolean editMode) {
      XComponent dataSet = form.findComponent(ASSIGNED_RESOURCE_DATA_SET);
      XComponent originalDataSet = form.findComponent(ORIGINAL_RESOURCE_DATA_SET);
      XComponent dataRow;
      XComponent dataCell;

      for (OpProjectNodeAssignment assignment : project.getAssignments()) {
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
         if (internalRate != null || externalRate != null) {
            dataCell.setBooleanValue(true);
         }
         else {
            dataCell.setBooleanValue(false);
         }
         dataCell.setEnabled(editMode);
         dataRow.addChild(dataCell);

         //3 - internal project rate
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (internalRate != null) {
            dataCell.setDoubleValue(internalRate);
            dataCell.setEnabled(editMode);
         }
         else {
            dataCell.setEnabled(false);
         }
         dataRow.addChild(dataCell);

         //4 - external project rate
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (externalRate != null) {
            dataCell.setDoubleValue(externalRate);
            dataCell.setEnabled(editMode);
         }
         else {
            dataCell.setEnabled(false);
         }
         dataRow.addChild(dataCell);

         //5 - start date - null
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataRow.addChild(dataCell);

         //6 - end date - null
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataRow.addChild(dataCell);

         //7 - internal period rate - null
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataRow.addChild(dataCell);

         //8 - external period rate - null
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataRow.addChild(dataCell);

         //expand the row if the assignment has hourly rates periods
         if (!assignment.getHourlyRatesPeriods().isEmpty()) {
            dataRow.setExpanded(true);
         }
         dataSet.addChild(dataRow);

         for (int i = 0; i < dataSet.getChildCount(); i++) {
            dataRow = (XComponent) dataSet.getChild(i);
            originalDataSet.addChild(dataRow.copyData());
         }
      }
      form.findComponent(RESOURCES_TABLE).setEditMode(editMode);
      form.findComponent(RESOURCE_TOOL_PANEL).setVisible(editMode);
   }
}