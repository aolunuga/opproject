/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.*;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.server.XSession;

import java.util.*;

public class OpActivitiesFormProvider implements XFormProvider {

   public final static String VALIDATE_PLAN = "validatePlan";

   protected final static String ACTIVITY_SET = "ActivitySet";
   protected final static String EDIT_MODE_FIELD = "EditModeField";
   protected final static String ACTIVITY_GANTT_CHART = "ActivityGanttChart";
   protected final static String TOTAL_STRING = "Total";

   private static final XLog logger = XLogFactory.getServerLogger(OpActivitiesFormProvider.class);

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
   private final static String PROJECT_TYPE_FIELD= "ProjectType";

   private final static String PROJECT_ID_FIELD = "ProjectIDField";
   private final static String PROJECT_NAME_FIELD = "ProjectName";
   private final static String WORKING_PLAN_VERSION_ID_FIELD = "WorkingPlanVersionIDField";

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
   private final static String ACTIVITY_COST_FOOTER_DATA_SET = "ActivityCostsFooter";
   private final static int FOOTER_BASE_EFFORT_INDEX = 7;
   private final static int FOOTER_PERSONNEL_INDEX = 2;
   private final static int FOOTER_TRAVEL_INDEX = 3;
   private final static int FOOTER_MATERIAL_INDEX = 4;
   private final static int FOOTER_EXTERNAL_INDEX = 5;
   private final static int FOOTER_MISC_INDEX = 6;
   private final static int FOOTER_PROCEEDS_INDEX = 7;
   private final static int ACTIVITY_TABLE_COLUMNS = 11;
   private final static int COST_TABLE_COLUMNS = 8;

   private final static String COSTS_TAB = "CostsProjectionTab";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      logger.debug("OpActivitiesFormProvider.prepareForm()");
      long begin = new Date().getTime();
      long  activityTime = 0;

      OpProjectSession session = (OpProjectSession) s;

      String project_id_string = (String) (parameters.get(OpProjectConstants.PROJECT_ID));
      Boolean validateProjectPlan = (Boolean) (parameters.get(VALIDATE_PLAN));
      if (validateProjectPlan == null) {
         validateProjectPlan = Boolean.FALSE;
      }
      if (project_id_string != null) {
         // Get open project-ID from parameters and se project-ID session variable
         session.setVariable(OpProjectConstants.PROJECT_ID, project_id_string);
      }
      else {
         project_id_string = (String) (session.getVariable(OpProjectConstants.PROJECT_ID));
      }
      // *** TODO: Store open project-ID in database (user preferences?)

      XComponent activityDataSet = form.findComponent(ACTIVITY_SET);

      // TODO: Calendar should be stored as session-variable (locale-specific)
      // ==> XCalendar calendar =((OpProjectSession)session).getCalendar();
      OpBroker broker = session.newBroker();
      // OpPath path = new OpPath().child("XProject");
      XComponent project_name_set = form.findComponent(PROJECT_NAME_SET);
      logger.debug("*** PIDS " + project_id_string);
      boolean edit_mode = false;
      OpUser currentUser = session.user(broker);
      if (project_id_string != null) {

         OpProjectNode project = (OpProjectNode) (broker.getObject(project_id_string)); // two db-trips opproject and attachment
         form.findComponent(PROJECT_TYPE_FIELD).setByteValue(project.getType());
         logger.debug("after get-project: " + project.getID());

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
            showHoursPref = OpSettingsService.getService().get(OpSettings.SHOW_RESOURCES_IN_HOURS);
         }
         Boolean showHours = Boolean.valueOf(showHoursPref);
         form.findComponent(SHOW_RESOURCE_HOURS).setBooleanValue(showHours.booleanValue());

         setProjectRelatedSettings(form, project);

         addCategories(form, broker);

         // enable all buttons now beside the edit-mode related buttons
         if (session.checkAccessLevel(broker, project.getID(), OpPermission.MANAGER)) {
            form.findComponent(EDIT_BUTTON).setEnabled(true);
            form.findComponent(IMPORT_BUTTON).setEnabled(project.getType() != OpProjectNode.TEMPLATE);
         }
         else {
            form.findComponent(EDIT_BUTTON).setEnabled(false);
            form.findComponent(IMPORT_BUTTON).setEnabled(false);
         }
         form.findComponent(SAVE_BUTTON).setEnabled(false);
         form.findComponent(CHECK_IN_BUTTON).setEnabled(false);
         form.findComponent(EXPORT_BUTTON).setEnabled(project.getType() != OpProjectNode.TEMPLATE);
         form.findComponent(REVERT_BUTTON).setEnabled(false);
         form.findComponent(PRINT_BUTTON).setEnabled(true);

         // TODO: Try to write cleaner code when completing permission checks

         logger.debug("project-locked?");
         Set locks = project.getLocks();
         if (locks.size() > 0) {
            logger.debug("   *** project is locked");
            // Currently only a single lock is allowed to exist
            OpLock lock = (OpLock) locks.iterator().next();
            if (lock.lockedByMe(session, broker)) {
               edit_mode = true;
               registerEventHandlersForButtons(form, activityDataSet);
               enableComponentsWhenUserOwner(form);
            }
            else {
               // project locked, but the owner is not the user, user won't be able to edit, save or checkIn project
               form.findComponent(EDIT_BUTTON).setEnabled(false);
               form.findComponent(IMPORT_BUTTON).setEnabled(false);
               form.findComponent(SAVE_BUTTON).setEnabled(false);
               form.findComponent(CHECK_IN_BUTTON).setEnabled(false);
            }
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
         OpActivityDataSetFactory.retrieveResourceDataSet(broker, project, resourceDataSet);  //one DB-query only!
         logger.debug("after-project-resources");

         //fill the availability map
         XComponent resourceAvailability = form.findComponent(RESOURCE_AVAILABILITY);
         Map<String, Double> availabilityMap = OpResourceDataSetFactory.createResourceAvailabilityMap(broker); //one DB-query only!
         resourceAvailability.setValue(availabilityMap);

         // Check if there is already a project plan
         OpProjectPlan projectPlan = project.getPlan();
         if (projectPlan != null) {

            OpProjectPlanVersion workingPlanVersion = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker,
                 project.getPlan(), OpProjectPlan.WORKING_VERSION_NUMBER);  //one DB-query only
            OpActivityDataSetFactory.fillHourlyRatesDataSet(project, form.findComponent(RESOURCES_HOURLY_RATES_DATA_SET)); //one DB-query only

            if (edit_mode) {
               // Show working plan version (if one exists already)
               if (workingPlanVersion != null) {
                  // Set working plan version ID
                  form.findComponent(WORKING_PLAN_VERSION_ID_FIELD).setStringValue(workingPlanVersion.locator());
                  activityDataSet.setValue(showHours);
                  OpActivityVersionDataSetFactory.retrieveActivityVersionDataSet(broker, workingPlanVersion,
                       activityDataSet, true);
               }
               else {
                  // form.findComponent(EDIT_MODE_FIELD).setBooleanValue(false);
                  activityDataSet.setValue(showHours);
                  OpActivityDataSetFactory.retrieveActivityDataSet(broker, project.getPlan(), activityDataSet, true);
               }
            }
            else {
               form.findComponent(EDIT_MODE_FIELD).setBooleanValue(false);
               activityDataSet.setValue(showHours);
               activityTime = new Date().getTime();
               OpActivityDataSetFactory.retrieveActivityDataSet(broker, project.getPlan(), activityDataSet, false); //bad guy...
               activityTime = new Date().getTime() - activityTime;
            }
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
      if(currentUser.getLevel() == OpUser.OBSERVER_CUSTOMER_USER_LEVEL){
         costsTab.setHidden(true);
      }

      //if the app. is multiuser and hide manager features is set to true and the user is not manager
      if (OpSubjectDataSetFactory.shouldHideFromUser(currentUser)) {
         //hide costs tab
         costsTab.setHidden(true);
         ((OpProjectComponent) form.findComponent(ACTIVITY_GANTT_CHART)).setShowCosts(false);
      }

      broker.close();

      XValidator validator = activityDataSet.validator();
      if (validator != null) {
         if (validateProjectPlan) {
            validator.validateEntireDataSet();
         }
      }

      setFooterData(form, activityDataSet);
      logger.debug("/OpActivitiesFormProvider.prepareForm(). Time : " + (new Date().getTime() - begin) + ". Activity Time: " + activityTime);
   }

   /**
    * Fills the footer data sets with the initial info.
    *
    * @param form
    * @param activityDataSet
    */
   private void setFooterData(XComponent form, XComponent activityDataSet) {
      XComponent listFooterDataSet = form.findComponent(ACTIVITY_LIST_FOOTER_DATA_SET);
      XComponent costsFooterDataSet = form.findComponent(ACTIVITY_COST_FOOTER_DATA_SET);
      //add the right nr of cells for each data set
      XComponent row = new XComponent(XComponent.DATA_ROW);
      for (int i = 0; i < ACTIVITY_TABLE_COLUMNS; i++) {
         row.addChild(new XComponent(XComponent.DATA_CELL));
      }
      ((XComponent) row.getChild(2)).setStringValue(form.findComponent(TOTAL_STRING).getText());
      listFooterDataSet.addChild(row);

      row = new XComponent(XComponent.DATA_ROW);
      for (int i = 0; i < COST_TABLE_COLUMNS; i++) {
         row.addChild(new XComponent(XComponent.DATA_CELL));
      }
      ((XComponent) row.getChild(1)).setStringValue(form.findComponent(TOTAL_STRING).getText());
      costsFooterDataSet.addChild(row);

      //update effort sum
      double sum = activityDataSet.calculateDoubleSum(OpGanttValidator.BASE_EFFORT_COLUMN_INDEX, 0);
      row = (XComponent) listFooterDataSet.getChild(0);
      XComponent footerCell = (XComponent) row.getChild(FOOTER_BASE_EFFORT_INDEX);
      footerCell.setValue(sum);

      //update cost sums
      //personnel costs
      sum = activityDataSet.calculateDoubleSum(OpGanttValidator.BASE_PERSONNEL_COSTS_COLUMN_INDEX, 0);
      row = (XComponent) costsFooterDataSet.getChild(0);
      footerCell = (XComponent) row.getChild(FOOTER_PERSONNEL_INDEX);
      footerCell.setValue(sum);

      //travel costs
      sum = activityDataSet.calculateDoubleSum(OpGanttValidator.BASE_TRAVEL_COSTS_COLUMN_INDEX, 0);
      row = (XComponent) costsFooterDataSet.getChild(0);
      footerCell = (XComponent) row.getChild(FOOTER_TRAVEL_INDEX);
      footerCell.setValue(sum);

      //material costs
      sum = activityDataSet.calculateDoubleSum(OpGanttValidator.BASE_MATERIAL_COSTS_COLUMN_INDEX, 0);
      row = (XComponent) costsFooterDataSet.getChild(0);
      footerCell = (XComponent) row.getChild(FOOTER_MATERIAL_INDEX);
      footerCell.setValue(sum);

      //external costs
      sum = activityDataSet.calculateDoubleSum(OpGanttValidator.BASE_EXTERNAL_COSTS_COLUMN_INDEX, 0);
      row = (XComponent) costsFooterDataSet.getChild(0);
      footerCell = (XComponent) row.getChild(FOOTER_EXTERNAL_INDEX);
      footerCell.setValue(sum);

      //misc costs
      sum = activityDataSet.calculateDoubleSum(OpGanttValidator.BASE_MISCELLANEOUS_COSTS_COLUMN_INDEX, 0);
      row = (XComponent) costsFooterDataSet.getChild(0);
      footerCell = (XComponent) row.getChild(FOOTER_MISC_INDEX);
      footerCell.setValue(sum);

      //proceeds costs
      sum = activityDataSet.calculateDoubleSum(OpGanttValidator.BASE_PROCEEDS_COLUMN_INDEX, 0);
      row = (XComponent) costsFooterDataSet.getChild(0);
      footerCell = (XComponent) row.getChild(FOOTER_PROCEEDS_INDEX);
      footerCell.setValue(sum);
   }

   protected void addCategories(XComponent form, OpBroker broker) {
      // No categories in open version
   }

   protected void setProjectRelatedSettings(XComponent form, OpProjectNode project) {

      //set start and end for project
      form.findComponent(PROJECT_START).setDateValue(project.getStart());
      form.findComponent(PROJECT_FINISH).setDateValue(project.getFinish());

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
      table_box.setAutoGrow(XExtendedComponent.AUTO_GROW_ALTERNATE);
      table_box.setEditMode(true);
      table_box.setSelectionModel(XComponent.CELL_SELECTION);

      table_box = (XExtendedComponent) form.findComponent(COSTS_TABLE);
      table_box.setAutoGrow(XExtendedComponent.AUTO_GROW_ALTERNATE);
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
