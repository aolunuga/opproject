/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityVersionDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpLock;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpSubjectDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.project.validators.OpProjectValidator;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

public class OpActivitiesFormProvider implements XFormProvider {

   public final static String VALIDATE_PLAN = "validatePlan";

   protected final static String ACTIVITY_SET = "ActivitySet";
   protected final static String EDIT_MODE_FIELD = "EditModeField";
   protected final static String EDIT_ACTIONS_MODE_FIELD = "EditActionsModeField";
   protected final static String ACTIVITY_GANTT_CHART = "ActivityGanttChart";
   protected final static String TOTAL_STRING = "Total";

   private static final XLog logger = XLogFactory.getLogger(OpActivitiesFormProvider.class);

   private final static String ASSIGNMENT_SET = OpGanttValidator.ASSIGNMENT_SET;
   private final static String CALCULATION_MODE = OpGanttValidator.CALCULATION_MODE;
   private final static String PROGRESS_TRACKED = OpGanttValidator.PROGRESS_TRACKED;
   private final static String PROJECT_TEMPLATE = OpGanttValidator.PROJECT_TEMPLATE;

   private final static String RESOURCE_AVAILABILITY = "ResourceAvailability";

   private final static String PROJECT_NAME_SET = "ProjectNameSet";
   private final static String PRINT_TITLE = "PrintTitle";
   private final static String SHOW_RESOURCE_HOURS = "ShowResourceHours";

   private final static String ACTIVITY_TABLE = "ActivityTable";
   private final static String COSTS_TABLE = "CostTable";

   private final static String PROJECT_START = "ProjectStartField";
   private final static String PROJECT_FINISH = "ProjectFinishField";
   private final static String PROJECT_SETTINGS_DATA_SET = "ProjectSettingsDataSet";
   private final static String PROJECT_TYPE_FIELD = "ProjectType";

   private final static String PROJECT_ID_FIELD = "ProjectIDField";
   private final static String PROJECT_NAME_FIELD = "ProjectName";
   final static String SOURCE_PLAN_VERSION_ID_FIELD = "SourcePlanVersionIDField";

   private final static String GANTT_CHART_TOGGLE_BAR = "GanttToggleBar";
   private final static String EDIT_BUTTON = "EditButton";
   private final static String SAVE_BUTTON = "SaveButton";
   private final static String IMPORT_BUTTON = "ImportButton";
   private final static String EXPORT_BUTTON = "ExportButton";
   private final static String CHECK_IN_BUTTON = "CheckInButton";
   private final static String REVERT_BUTTON = "RevertButton";
   private final static String PRINT_BUTTON = "PrintButton";

   private final static String RESOURCES_COLUMN = "ResourcesColumn";
   private final static String COMPLETE_COLUMN = "PercentCompleteColumn";

   private final static String ACTIVITY_UNDO_BUTTON = "activityUndoButton";
   private final static String GANTT_UNDO_BUTTON = "ganttUndoButton";
   private final static String COST_UNDO_BUTTON = "costUndoButton";
   private final static String ACTIVITY_REDO_BUTTON = "activityRedoButton";
   private final static String GANTT_REDO_BUTTON = "ganttRedoButton";
   private final static String COST_REDO_BUTTON = "costRedoButton";

   private final static String COST_TABLE_TOOL_BAR = "CostTableToolBar";
   private final static String GANTT_TOOL_BAR = "GanttToolBar";
   private final static String ACTIVITY_TABLE_TOOL_BAR = "ActivityTableToolBar";
   private final static String TIME_CHOOSER = "TimeUnitChooser";
   private final static String RESOURCES_HOURLY_RATES_DATA_SET = "ResourcesHourlyRates";

   private final static String ACTIVITY_LIST_FOOTER_DATA_SET = "ActivityListFooter";

   private final static String EFFORT_BASED_PLANNING_FIELD = "EffortBasedPlanning";
   
   private final static int FOOTER_PERSONNEL_INDEX = 2;
   private final static int FOOTER_TRAVEL_INDEX = 3;
   private final static int FOOTER_MATERIAL_INDEX = 4;
   private final static int FOOTER_EXTERNAL_INDEX = 5;
   private final static int FOOTER_MISC_INDEX = 6;
   private final static int FOOTER_PROCEEDS_INDEX = 7;
   
   private final static int FOOTER_BASE_EFFORT_INDEX = 7;

   private final static int COST_TABLE_COLUMNS = 9;
   private final static int ACTIVITY_TABLE_COLUMNS = 12;

   private final static String ACTIVITY_COST_FOOTER_DATA_SET = "ActivityCostsFooter";

   // private final static int FOOTER_BILLABLE_INDEX = 3;
   
   private final static String COSTS_TAB = "CostsProjectionTab";
   
   private final static String STATUS_BAR = "StatusBar";
   private final static String ACTIVITIES_RESOURCE_MAP = "project_planning.activities";
   private final static String PROJECT_LOCKED_RESOURCE = "${ProjectLocked}";

   private static final String CONTROLLING_SHEETS_EXIST_RESOURCE = "${ControllingSheetsExist}";
   
   // ----
   public final static String ACTIVITIESFORM_ID = "ActivitiesForm";
   public final static String ACTIVITIESTABLEBASEROWMAP_ID = "ActivitiesTableBaseRowMap";
   public final static String ACTIVITIESTABLEROWLOCATORMAP_ID = "ActivitiesTableRowLocatorMap";
   public final static String PROJECTIDFIELD_ID = "ProjectIDField";
   public final static String PROJECTNAME_ID = "ProjectName";
   public final static String SOURCEPLANVERSIONIDFIELD_ID = "SourcePlanVersionIDField";
   public final static String EDITMODEFIELD_ID = "EditModeField";
   public final static String EDITACTIONSMODEFIELD_ID = "EditActionsModeField";
   public final static String EDITACTIONSINVERSEMODEFIELD_ID = "EditActionsInverseModeField";
   public final static String LOOPEXCEPTION_ID = "LoopException";
   public final static String MANDATORYEXCEPTION_ID = "MandatoryException";
   public final static String RANGEEXCEPTION_ID = "RangeException";
   public final static String INVALIDCOSTEXCEPTION_ID = "InvalidCostException";
   public final static String MILESTONECOLLECTIONEXCEPTION_ID = "MilestoneCollectionException";
   public final static String SCHEDULEDMIXEDEXCEPTION_ID = "ScheduledMixedException";
   public final static String ASSIGNMENTEXCEPTION_ID = "AssignmentException";
   public final static String RESOURCENAMEEXCEPTION_ID = "ResourceNameException";
   public final static String NOTEQUALEFFORTSEXCEPTION_ID = "NotEqualEffortsException";
   public final static String WORKRECORDSEXISTEXCEPTION_ID = "WorkRecordsExistException";
   public final static String TASKEXTRARESOURCEEXCEPTION_ID = "TaskExtraResourceException";
   public final static String INVALIDPRIORITYEXCEPTION_ID = "InvalidPriorityException";
   public final static String CANNOTMOVEROOTACTIVITYEXCEPTION_ID = "CannotMoveRootActivityException";
   public final static String OUTLINELEVELINVALIDEXCEPTION_ID = "OutlineLevelInvalidException";
   public final static String INVALIDPAYMENTEXCEPTION_ID = "InvalidPaymentException";
   public final static String PROGRAMELEMENTMOVEEXCEPTION_ID = "ProgramElementMoveException";
   public final static String PROGRAMELEMENTDELETEEXCEPTION_ID = "ProgramElementDeleteException";
   public final static String IMPORTPROJECTTITLE_ID = "ImportProjectTitle";
   public final static String EXPORTPROJECTTITLE_ID = "ExportProjectTitle";
   public final static String FILEWRITEERROR_ID = "FileWriteError";
   public final static String PROJECTSTARTFIELD_ID = "ProjectStartField";
   public final static String PROJECTFINISHFIELD_ID = "ProjectFinishField";
   public final static String PRINTTITLE_ID = "PrintTitle";
   public final static String TOTAL_ID = "Total";
   public final static String SHOWRESOURCEHOURS_ID = "ShowResourceHours";
   public final static String RESOURCEAVAILABILITY_ID = "ResourceAvailability";
   public final static String PROJECTNAMESET_ID = "ProjectNameSet";
   public final static String ASSIGNMENTSET_ID = "AssignmentSet";
   public final static String PROJECTSETTINGSDATASET_ID = "ProjectSettingsDataSet";
   public final static String ACTIVITYSET_ID = "ActivitySet";
   public final static String RESOURCESHOURLYRATES_ID = "ResourcesHourlyRates";
   public final static String PROJECTTYPE_ID = "ProjectType";
   public final static String INCLUDEDPROJECTS_ID = "IncludedProjects";
   public final static String EFFORTBASEDPLANNING_ID = "EffortBasedPlanning";
   public final static String INDICATORICONSET_ID = "IndicatorIconSet";
   public final static String ERRORICONSET_ID = "ErrorIconSet";
   public final static String ACTIONSICONSET_ID = "ActionsIconSet";
   public final static String ACTIVITIESPOPUPMENU_ID = "activitiesPopupMenu";
   public final static String COSTSPOPUPMENU_ID = "costsPopupMenu";
   public final static String GANTTCHARTPOPUPMENU_ID = "ganttChartPopupMenu";
   public final static String PROJECTNAMEFIELD_ID = "ProjectNameField";
   public final static String EDITBUTTON_ID = "EditButton";
   public final static String SAVEBUTTON_ID = "SaveButton";
   public final static String CHECKINBUTTON_ID = "CheckInButton";
   public final static String REVERTBUTTON_ID = "RevertButton";
   public final static String IMPORTBUTTON_ID = "ImportButton";
   public final static String EXPORTBUTTON_ID = "ExportButton";
   public final static String PRINTBUTTON_ID = "PrintButton";
   public final static String TIMEUNITSET_ID = "TimeUnitSet";
   public final static String VALIDATIONERRORLABEL_ID = "ValidationErrorLabel";
   public final static String MYTABBOX_ID = "MyTabBox";
   public final static String TASKLISTTAB_ID = "TasklistTab";
   public final static String ACTIVITYLISTFOOTER_ID = "ActivityListFooter";
   public final static String ACTIVITYTABLE_ID = "ActivityTable";
   public final static String PERCENTCOMPLETECOLUMN_ID = "PercentCompleteColumn";
   public final static String DURATION_ID = "Duration";
   public final static String RESOURCESCOLUMN_ID = "ResourcesColumn";
   public final static String ACTIVITYTABLETOOLBAR_ID = "ActivityTableToolBar";
   public final static String ACTIVITYUNDOBUTTON_ID = "activityUndoButton";
   public final static String ACTIVITYREDOBUTTON_ID = "activityRedoButton";
   public final static String ADDSUBPROJECTBUTTONTABLE_ID = "AddSubProjectButtonTable";
   public final static String ASSIGNRESOURCEBUTTONTABLE_ID = "AssignResourceButtonTable";
   public final static String GANTTDIAGRAMTAB_ID = "GanttDiagramTab";
   public final static String TIMEUNITCHOOSER_ID = "TimeUnitChooser";
   public final static String ACTIVITYGANTTCHART_ID = "ActivityGanttChart";
   public final static String GANTTTOOLBAR_ID = "GanttToolBar";
   public final static String GANTTTOGGLEBAR_ID = "GanttToggleBar";
   public final static String NORMALCURSOR_ID = "NormalCursor";
   public final static String ACTIVITYDRAWITEM_ID = "ActivityDrawItem";
   public final static String MILESTONEDRAWITEM_ID = "MilestoneDrawItem";
   public final static String DEPENDENCYDRAWITEM_ID = "DependencyDrawItem";
   public final static String GANTTUNDOBUTTON_ID = "ganttUndoButton";
   public final static String GANTTREDOBUTTON_ID = "ganttRedoButton";
   public final static String ADDSUBPROJECTBUTTONGANTT_ID = "AddSubProjectButtonGantt";
   public final static String ASSIGNRESOURCEBUTTONGANTT_ID = "AssignResourceButtonGantt";
   public final static String COSTSPROJECTIONTAB_ID = "CostsProjectionTab";
   public final static String ACTIVITYCOSTSFOOTER_ID = "ActivityCostsFooter";
   public final static String COSTTABLE_ID = "CostTable";
   public final static String COSTTABLETOOLBAR_ID = "CostTableToolBar";
   public final static String COSTUNDOBUTTON_ID = "costUndoButton";
   public final static String COSTREDOBUTTON_ID = "costRedoButton";
   public final static String STATUSBAR_ID = "StatusBar";


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      long start = System.currentTimeMillis();
      logger.debug("OpActivitiesFormProvider.prepareForm()");

      OpProjectSession session = (OpProjectSession) s;

      String project_id_string = (String) (parameters.get(OpProjectConstants.PROJECT_ID));
      if (project_id_string == null) {
         String selectLocator = (String)parameters.get(XFormProvider.SELECT);
         if (selectLocator != null) {
            project_id_string = selectLocator;
         }
      }
      Boolean validateProjectPlan = (Boolean) (parameters.get(VALIDATE_PLAN));
      if (validateProjectPlan == null) {
         validateProjectPlan = Boolean.FALSE;
      }
      if (project_id_string != null) {
         // Get open project-ID from parameters and set project-ID session variable
         session.setVariable(OpProjectConstants.PROJECT_ID, project_id_string);
      }
      else {
         project_id_string = (String) (session.getVariable(OpProjectConstants.PROJECT_ID));
      }
      // *** TODO: Store open project-ID in database (user preferences?)

      XComponent activityDataSet = form.findComponent(ACTIVITY_SET);
      
      OpProjectValidator.transformRowMapToDataSet(
            OpActivityDataSetFactory.ACTIVITY_ROW_BASE_DESCRIPTION, form
                  .findComponent(ACTIVITIESTABLEBASEROWMAP_ID));
      OpProjectValidator.transformRowMapToDataSet(
            OpActivityDataSetFactory.ACTIVITY_ROW_LOCATOR_DESCRIPTION, form
                  .findComponent(ACTIVITIESTABLEROWLOCATORMAP_ID));

      // TODO: Calendar should be stored as session-variable (locale-specific)
      // ==> OpProjectCalendar calendar =((OpProjectSession)session).getCalendar();
      OpBroker broker = session.newBroker();
      try {
         // OpPath path = new OpPath().child("XProject");
         XComponent project_name_set = form.findComponent(PROJECT_NAME_SET);
         logger.debug("*** PIDS " + project_id_string);
         boolean edit_mode = false;
         OpUser currentUser = session.user(broker);
         if (project_id_string != null) {

            OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string)); // two db-trips opproject and attachment
            form.findComponent(PROJECT_TYPE_FIELD).setByteValue(project.getType());
            
            if (project.getType() != OpProjectNode.PROJECT) {
               form.findComponent(ASSIGNRESOURCEBUTTONGANTT_ID).setVisible(false);
               form.findComponent(ASSIGNRESOURCEBUTTONTABLE_ID).setVisible(false);
            }
            
            logger.debug("after get-project: " + project.getId());

            //print title
            form.findComponent(PRINT_TITLE).setStringValue(project.getName());
            // Hide column "Resources" if this is a template plan
            if (project.getType() == OpProjectNode.TEMPLATE) {
               form.findComponent(RESOURCES_COLUMN).setHidden(true);
               form.findComponent(COMPLETE_COLUMN).setHidden(true);
            }

            //set show resource hours
            String showHoursPref = currentUser.getPreferenceValue(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
            if (showHoursPref == null) {
               showHoursPref = OpSettingsService.getService().getStringValue(session, OpSettings.SHOW_RESOURCES_IN_HOURS);
            }
            Boolean showHours = Boolean.valueOf(showHoursPref);
            form.findComponent(SHOW_RESOURCE_HOURS).setBooleanValue(showHours.booleanValue());

            setProjectRelatedSettings(form, project);

            addCategories(form, broker);

            // enable all buttons now beside the edit-mode related buttons
            if (session.checkAccessLevel(broker, project.getId(), OpPermission.MANAGER)) {
               form.findComponent(EDIT_BUTTON).setEnabled(true);
               form.findComponent(IMPORT_BUTTON).setEnabled(project.getType() != OpProjectNode.TEMPLATE);
            }
            else {
               form.findComponent(EDIT_BUTTON).setEnabled(false);
               form.findComponent(IMPORT_BUTTON).setEnabled(false);
            }
            form.findComponent(SAVE_BUTTON).setEnabled(false);
            form.findComponent(CHECK_IN_BUTTON).setEnabled(false);
            form.findComponent(REVERT_BUTTON).setEnabled(false);
            form.findComponent(EXPORT_BUTTON).setEnabled(project.getType() != OpProjectNode.TEMPLATE);
            form.findComponent(PRINT_BUTTON).setEnabled(true);
            form.findComponent(EFFORT_BASED_PLANNING_FIELD).setBooleanValue(project.getPlan().getCalculationMode() == OpProjectPlan.EFFORT_BASED);

            // TODO: Try to write cleaner code when completing permission checks

            logger.debug("project-locked?");
            Set<OpLock> locks = project.getLocks();
            // Currently only a single lock is allowed to exist
            OpLock lock = locks.iterator().hasNext() ? locks.iterator().next() : null;
            if (lock != null && lock.lockedByMe(session, broker)) {
               edit_mode = true;
               registerEventHandlersForButtons(form, activityDataSet);
               enableComponentsWhenUserOwner(form);
               
               // check for existing controlling sheets:
               OpProjectPlanVersion workingVersion = project.getPlan().getWorkingVersionOld();
               if (workingVersion != null
                     && workingVersion.getControllingSheets() != null
                     && !workingVersion.getControllingSheets().isEmpty()) {
                  form.findComponent(CHECK_IN_BUTTON).setEnabled(false);
                  form.findComponent(REVERT_BUTTON).setEnabled(false);
                  XLocalizer localizer = new XLocalizer();
                  localizer.setResourceMap(session.getLocale().getResourceMap(ACTIVITIES_RESOURCE_MAP));
                  StringBuffer statusMessage = new StringBuffer(localizer.localize(CONTROLLING_SHEETS_EXIST_RESOURCE));
                  form.findComponent(STATUS_BAR).setText(statusMessage.toString());
               }
            }
            else if (lock != null) {
               if (session.userIsAdministrator()) {
                  enableComponentsWhenUserAdministrator(form);
               }
               // project locked, but the owner is not the user, user won't be able to edit, save or checkIn project
               form.findComponent(EDIT_BUTTON).setEnabled(false);
               form.findComponent(IMPORT_BUTTON).setEnabled(false);
               form.findComponent(SAVE_BUTTON).setEnabled(false);
               form.findComponent(CHECK_IN_BUTTON).setEnabled(false);
               // Show who locked the project in the status bar
               XLocalizer localizer = new XLocalizer();
               localizer.setResourceMap(session.getLocale().getResourceMap(ACTIVITIES_RESOURCE_MAP));
               StringBuffer statusMessage = new StringBuffer(localizer.localize(PROJECT_LOCKED_RESOURCE));
               localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionDataSetFactory.USER_OBJECTS));
               statusMessage.append(": ");
               statusMessage.append(localizer.localize(lock.getOwner().getDisplayName()));
               form.findComponent(STATUS_BAR).setText(statusMessage.toString());
            }
            //set up the project edit mode
            form.findComponent(EDIT_MODE_FIELD).setBooleanValue(edit_mode);

            // if project is locked and user is not the owner || project is not locked
            if (((locks.size() > 0) && !edit_mode) || (locks.size() == 0)) {
               enableComponentsForProjectLocked(form);
            }

            //set default state to GanttToggleBar and Gant Chart
            Map componentStateMap = session.getComponentStateMap(form.getID());

            //set state for toogle bar
            if (componentStateMap != null) {
               componentStateMap.put(GANTT_CHART_TOGGLE_BAR, new Integer(0));
               //set drawing tool state for gantt chart
               List ganttChartState = (List) componentStateMap.get(ACTIVITY_GANTT_CHART);
               if (ganttChartState == null) {
                  ganttChartState = new ArrayList();
                  componentStateMap.put(ACTIVITY_GANTT_CHART, ganttChartState);
               }
               ganttChartState.set(0, OpProjectComponent.DEFAULT_CURSOR);
               ganttChartState.set(1, java.awt.Cursor.getDefaultCursor());
            }

            logger.debug("after-project-locked");
            XComponent project_id_field = form.findComponent(PROJECT_ID_FIELD);
            project_id_field.setStringValue(project.locator());

            XComponent project_name_field = form.findComponent(PROJECT_NAME_FIELD);
            project_name_field.setStringValue(project.getName());

            String info = project.getName();
            if (locks.size() > 0) {
               info += " (wird bearbeitet)";
            }
            XComponent data_row = new XComponent(XComponent.DATA_ROW);
            data_row.setStringValue(info);
            project_name_set.addChild(data_row);
            // *** TODO: Use string-constant
            XComponent resourceDataSet = form.findComponent(ASSIGNMENT_SET);

            // Retrieve data-set of resources assigned to the project node
            logger.debug("before-project-resources");
            OpActivityDataSetFactory.getInstance().retrieveResourceDataSet(broker, project, resourceDataSet);  //one DB-query only!
            logger.debug("after-project-resources");

            //fill the availability map
            XComponent resourceAvailability = form.findComponent(RESOURCE_AVAILABILITY);
            Map<String, Double> availabilityMap = OpResourceDataSetFactory.createResourceAvailabilityMap(broker); //one DB-query only!
            logger.debug("after createResourceAvailabilityMap");
            resourceAvailability.setValue(availabilityMap);

            // Check if there is already a project plan
            OpProjectPlan projectPlan = project.getPlan();
            if (projectPlan != null) {

               OpProjectPlanVersion planVersion = project.getPlan().getWorkingVersion();
               logger.debug("after findProjectPlanVersion");
               OpActivityDataSetFactory.fillHourlyRatesDataSet(project, form.findComponent(RESOURCES_HOURLY_RATES_DATA_SET)); //one DB-query only
               logger.debug("after fillHourlyRatesDataSet");

               form.findComponent(EDIT_MODE_FIELD).setBooleanValue(edit_mode);
               if (edit_mode) {
                  activityDataSet.setValue(showHours);
                  // Show working plan version (if one exists already)
                  logger.debug("before fillActivityDataSet");
                  fillActivityDataSet(form, activityDataSet, session, broker, project,
                        planVersion, SOURCE_PLAN_VERSION_ID_FIELD, true);
                  logger.debug("after fillActivityDataSet");
                  try {
                     // FIXME: validate after load, if in edit-mode. This enables highlighting of invalid dependencies...
                     activityDataSet.validateAll();
                  }
                  catch (IllegalStateException e) {
                     // FIXME: currently, most likely a loop...
                     form.findComponent(VALIDATIONERRORLABEL_ID).setText(form.findComponent(LOOPEXCEPTION_ID).getText());
                  }
               }
               else {
                  activityDataSet.setValue(showHours);
                  logger.debug("before fillActivityDataSet-2");
                  fillActivityDataSet(form, activityDataSet, session, broker, project,
                        project.getPlan().getLatestVersion(), null, false);
                  logger.debug("after fillActivityDataSet-1");
               }
               Set includedProjects = new HashSet();
               for (int i = 0; i < activityDataSet.getChildCount(); i++) {
                  XComponent row = activityDataSet.getDataRow(i);
                  if (OpGanttValidator.importedHeadRow(row)) {
                     includedProjects.add(OpGanttValidator.getSubProject(row));
                  }
               }
               form.findComponent(INCLUDEDPROJECTS_ID).setValue(includedProjects);
            }
         }
         else {
            // No open project
            logger.debug("NO OPEN PROJECT");
            XComponent data_row = new XComponent(XComponent.DATA_ROW);
            // TODO: I18n
            data_row.setStringValue("(Keine offenen Projekte)");
            project_name_set.addChild(data_row);
            enableComponentsForNoOpenProject(form);
         }

         XComponent costsTab = form.findComponent(COSTS_TAB);
         //hide costs tab and costs column for users that have only the customer level
         if (currentUser.getLevel() == OpUser.OBSERVER_CUSTOMER_USER_LEVEL) {
            costsTab.setHidden(true);
         }

         //if the app. is multiuser and hide manager features is set to true and the user is not manager
         if (OpSubjectDataSetFactory.shouldHideFromUser(session, currentUser)) {
            //hide costs tab
            costsTab.setHidden(true);
            ((OpProjectComponent) form.findComponent(ACTIVITY_GANTT_CHART)).setShowCosts(false);
         }
      }
      finally {
         broker.close();
      }

      OpGanttValidator validator = (OpGanttValidator) activityDataSet.validator();
      if (validator != null) {
         if (validateProjectPlan) {
            validator.validateEntireDataSet();
         }
      }

      logger.debug("/OpActivitiesFormProvider.prepareForm(), lasted: "+(System.currentTimeMillis()-start));

   }

   private void enableComponentsWhenUserAdministrator(XComponent form) {
      form.findComponent(REVERT_BUTTON).setEnabled(true);
   }

   /**
    * @param form
    * @param activityDataSet
    * @param broker
    * @param project
    * @param planVersion
    */
   public void fillActivityDataSet(XComponent form, XComponent activityDataSet,
         OpProjectSession session, OpBroker broker, OpProjectNode project,
         OpProjectPlanVersion planVersion, String sourcePlanVersionFieldId, boolean editMode) {
      if (planVersion == null) {
         planVersion = project.getPlan().getLatestVersion();
      }
      // set calendar on form:
      OpProjectCalendar pCal = null;
      if (editMode && project.getPlan().getWorkingVersion() == null) {
         pCal = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, project.getPlan());
      }
      else {
         pCal = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, planVersion);
      }
      form.setComponentCalendar(pCal);

      // TODO check, if this harms anything:
      // Set working plan version ID
      if (sourcePlanVersionFieldId != null && planVersion != null) {
         form.findComponent(sourcePlanVersionFieldId).setStringValue(planVersion.locator());
      }
      if (planVersion != null) {
         OpActivityVersionDataSetFactory.getInstance().retrieveActivityVersionDataSet(session, broker, planVersion,
               activityDataSet, editMode);
      }
   }

   protected void addCategories(XComponent form, OpBroker broker) {
      // No categories in open version
   }

   protected void setProjectRelatedSettings(XComponent form, OpProjectNode project) {

      //set start and end for project
      form.findComponent(PROJECT_START).setDateValue(project.getStart() != null ? project.getStart() : project.getPlan().getStart());
      form.findComponent(PROJECT_FINISH).setDateValue(project.getFinish());

      fillProjectSettings(form, project);
   }

   public static void fillProjectSettings(XComponent form, OpProjectNode project) {
      //set settings
      XComponent settingsDataSet = form.findComponent(PROJECT_SETTINGS_DATA_SET);
      XComponent dataRow;
      XComponent dataCell;
      //calculation mode
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(CALCULATION_MODE);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setByteValue(project.getPlan().getCalculationMode());
      dataRow.addChild(dataCell);
      settingsDataSet.addChild(dataRow);
      //progress tracking
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(PROGRESS_TRACKED);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(project.getPlan().getProgressTracked());
      dataRow.addChild(dataCell);
      settingsDataSet.addChild(dataRow);
      //project type
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(PROJECT_TEMPLATE);
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setBooleanValue(project.getPlan().getTemplate());
      dataRow.addChild(dataCell);
      settingsDataSet.addChild(dataRow);
   }

   protected void enableComponentsForNoOpenProject(XComponent form) {
      form.findComponent(ACTIVITY_GANTT_CHART).setEditMode(false);
      form.findComponent(TIME_CHOOSER).setEnabled(false);
      form.findComponent(ACTIVITY_GANTT_CHART).setPopUpMenuRef(null);
      form.findComponent(ACTIVITY_TABLE).setPopUpMenuRef(null);
      form.findComponent(COSTS_TABLE).setPopUpMenuRef(null);
   }

   protected void enableComponentsForProjectLocked(XComponent form) {
      form.findComponent(ACTIVITY_GANTT_CHART).setEditMode(false);
   }

   protected void enableComponentsWhenUserOwner(XComponent form) {
      form.findComponent(ACTIVITY_TABLE_TOOL_BAR).setVisible(true);
      form.findComponent(GANTT_TOOL_BAR).setVisible(true);
      form.findComponent(COST_TABLE_TOOL_BAR).setVisible(true);

      // Enabled buttons according to edit-mode
      form.findComponent(EDIT_BUTTON).setEnabled(false);
      form.findComponent(SAVE_BUTTON).setEnabled(true);
      form.findComponent(CHECK_IN_BUTTON).setEnabled(true);
      form.findComponent(REVERT_BUTTON).setEnabled(true);
      //enable auto-grow feature for tables
      // *** Set table selection model to cell-based
      XExtendedComponent table_box = (XExtendedComponent) form.findComponent(ACTIVITY_TABLE);
      table_box.setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);
      table_box.setEditMode(true);
      table_box.setSelectionModel(XComponent.CELL_SELECTION);

      table_box = (XExtendedComponent) form.findComponent(COSTS_TABLE);
      table_box.setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);
      table_box.setEditMode(true);
      table_box.setSelectionModel(XComponent.CELL_SELECTION);
      XComponent ganttChart = form.findComponent(ACTIVITY_GANTT_CHART);
      ganttChart.setEditMode(true);
   }

   protected void registerEventHandlersForButtons(XComponent form, XComponent activityDataSet) {
      XComponent button;
      button = form.findComponent(ACTIVITY_UNDO_BUTTON);
      activityDataSet.registerEventHandler(button, XComponent.COMPONENT_EVENT);
      button = form.findComponent(GANTT_UNDO_BUTTON);
      activityDataSet.registerEventHandler(button, XComponent.COMPONENT_EVENT);
      button = form.findComponent(COST_UNDO_BUTTON);
      activityDataSet.registerEventHandler(button, XComponent.COMPONENT_EVENT);
      button = form.findComponent(ACTIVITY_REDO_BUTTON);
      activityDataSet.registerEventHandler(button, XComponent.COMPONENT_EVENT);
      button = form.findComponent(GANTT_REDO_BUTTON);
      activityDataSet.registerEventHandler(button, XComponent.COMPONENT_EVENT);
      button = form.findComponent(COST_REDO_BUTTON);
      activityDataSet.registerEventHandler(button, XComponent.COMPONENT_EVENT);
   }
}
