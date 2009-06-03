/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.work.OpCostRecordDataSetFactory;
import onepoint.project.modules.work.OpTimeRecordDataSetFactory;
import onepoint.project.modules.work.OpWorkEffortDataSetFactory;
import onepoint.project.modules.work.OpWorkSlipDataSetFactory;
import onepoint.project.modules.work.validators.OpWorkCostValidator;
import onepoint.project.modules.work.validators.OpWorkEffortValidator;
import onepoint.project.modules.work.validators.OpWorkTimeValidator;
import onepoint.project.modules.work.validators.OpWorkValidator;
import onepoint.project.util.OpProjectCalendar;
import onepoint.service.server.XSession;

public class OpNewWorkSlipFormProvider implements XFormProvider {

   private final static String WORK_EFFORT_RECORD_SET = "WorkEffortRecordSet";
   private final static String WORK_TIME_RECORD_SET = "WorkTimeRecordSet";
   private final static String WORK_COST_RECORD_SET = "WorkCostRecordSet";

   private final static String DATE_FIELD = "StartField";
   private final static String PROJECT_CHOICE_FIELD = "ProjectChooser";
   private final static String START_TIME_CHOICE_FIELD = "StartTimeChooser";
   private final static String FILTER_PROJECT_SET = "FilterProjectSet";
   private final static String COST_TYPES_SET = "CostTypesSet";

   // filters
   private final static String START_BEFORE_ID = "start_before_id";
   private final static String PROJECT_CHOICE_ID = "project_choice_id";

   private final static String EFFORT_TABLE = "EffortTable";
   private final static String TIME_TABLE = "TimeTable";
   private final static String COST_TABLE = "CostTable";
   private final static String ERROR_LABEL = "ErrorLabel";

   private final static String ERROR_MSG = "error_msg";

   //start from filter choices
   private final static String ALL = "all";
   private final static String NEXT_WEEK = "nw";
   private final static String NEXT_2_WEEKS = "n2w";
   private final static String NEXT_MONTH = "nm";
   private final static String NEXT_2_MONTHS = "n2m";

   private static final String TIME_TAB = "TimeTab";
   private static final String TAB_BOX = "WorkSlipsTabBox";

   private final static String TIME_TRACKING = "TimeTrackingOn";
   private final static String PULSING = "Pulsing";
   private final static String ADD_HOURS_BUTTON = "AddHoursButton";
   private final static String REMOVE_HOURS_BUTTON = "RemoveHoursButton";
   private final static String ADD_TIME_BUTTON = "AddTimeButton";
   private final static String REMOVE_TIME_BUTTON = "RemoveTimeButton";
   private final static String ADD_COST_BUTTON = "AddCostButton";
   private final static String REMOVE_COST_BUTTON = "RemoveCostButton";
   private final static String ATTACHMENT_BUTTON = "AttachmentButton";

   private static final String ASSIGNMENT_MAP = "AssignmentMap";
   private final static String PRE_FILLED = "activitiesToFill";

   public final static String NEWWORKSLIPFORM_ID = "NewWorkSlipForm";
   public final static String PROJECTNAMEEXCEPTION_ID = "ProjectNameException";
   public final static String ACTIVITYNAMEEXCEPTION_ID = "ActivityNameException";
   public final static String RESOURCENAMEEXCEPTION_ID = "ResourceNameException";
   public final static String STARTVALUEEXCEPTION_ID = "StartValueException";
   public final static String FINISHVALUEEXCEPTION_ID = "FinishValueException";
   public final static String DURATIONVALUEEXCEPTION_ID = "DurationValueException";
   public final static String INTERVALSOVERLAPEXCEPTION_ID = "IntervalsOverlapException";
   public final static String ACTUALEFFORTEXCEPTION_ID = "ActualEffortException";
   public final static String REMAININGEFFORTEXCEPTION_ID = "RemainingEffortException";
   public final static String DUPLICATEEFFORTEXCEPTION_ID = "DuplicateEffortException";
   public final static String COSTTYPEEXCEPTION_ID = "CostTypeException";
   public final static String ACTUALCOSTEXCEPTION_ID = "ActualCostException";
   public final static String REMAININGCOSTEXCEPTION_ID = "RemainingCostException";
   public final static String NOTALLACTIONSDONE_ID = "NotAllActionsDone";
   public final static String VIEWEDNEWCONTENTS_ID = "ViewedNewContents";
   public final static String TIMEACTIVITYSET_ID = "TimeActivitySet";
   public final static String TIMERESOURCESET_ID = "TimeResourceSet";
   public final static String TIMEPROJECTSET_ID = "TimeProjectSet";
   public final static String EFFORTACTIVITYSET_ID = "EffortActivitySet";
   public final static String EFFORTRESOURCESET_ID = "EffortResourceSet";
   public final static String EFFORTPROJECTSET_ID = "EffortProjectSet";
   public final static String COSTACTIVITYSET_ID = "CostActivitySet";
   public final static String COSTRESOURCESET_ID = "CostResourceSet";
   public final static String COSTPROJECTSET_ID = "CostProjectSet";
   public final static String WORKEFFORTRECORDSET_ID = "WorkEffortRecordSet";
   public final static String WORKTIMERECORDSET_ID = "WorkTimeRecordSet";
   public final static String WORKCOSTRECORDSET_ID = "WorkCostRecordSet";
   public final static String INDICATORICONSET_ID = "IndicatorIconSet";
   public final static String COSTTYPESSET_ID = "CostTypesSet";
   public final static String STARTBEFORESET_ID = "StartBeforeSet";
   public final static String FILTERPROJECTSET_ID = "FilterProjectSet";
   public final static String ASSIGNMENTMAP_ID = "AssignmentMap";
   public final static String TIMETRACKINGON_ID = "TimeTrackingOn";
   public final static String PULSING_ID = "Pulsing";
   public final static String ACTIONSICONSET_ID = "ActionsIconSet";
   public final static String ERRORLABEL_ID = "ErrorLabel";
   public final static String STARTFIELD_ID = "StartField";
   public final static String PROJECTCHOOSER_ID = "ProjectChooser";
   public final static String STARTTIMECHOOSER_ID = "StartTimeChooser";
   public final static String WORKSLIPSTABBOX_ID = "WorkSlipsTabBox";
   public final static String TIMETAB_ID = "TimeTab";
   public final static String TIMETABLE_ID = "TimeTable";
   public final static String TIMERESOURCECOLUMN_ID = "TimeResourceColumn";
   public final static String ADDTIMEBUTTON_ID = "AddTimeButton";
   public final static String REMOVETIMEBUTTON_ID = "RemoveTimeButton";
   public final static String HOURSTAB_ID = "HoursTab";
   public final static String WORKEFFORTRECORDFOOTERSET_ID = "WorkEffortRecordFooterSet";
   public final static String EFFORTTABLE_ID = "EffortTable";
   public final static String HOURSPROJECTCOLUMN_ID = "HoursProjectColumn";
   public final static String HOURSTASKCOLUMN_ID = "HoursTaskColumn";
   public final static String HOURSRESOURCECOLUMN_ID = "HoursResourceColumn";
   public final static String HOURSPLANNEDCOLUMN_ID = "HoursPlannedColumn";
   public final static String HOURSEFFORTCOLUMN_ID = "HoursEffortColumn";
   public final static String ADDHOURSBUTTON_ID = "AddHoursButton";
   public final static String REMOVEHOURSBUTTON_ID = "RemoveHoursButton";
   public final static String COSTTAB_ID = "CostTab";
   public final static String WORKCOSTSRECORDFOOTERSET_ID = "WorkCostsRecordFooterSet";
   public final static String COSTTABLE_ID = "CostTable";
   public final static String COSTSRESOURCECOLUMN_ID = "CostsResourceColumn";
   public final static String ADDCOSTBUTTON_ID = "AddCostButton";
   public final static String REMOVECOSTBUTTON_ID = "RemoveCostButton";
   public final static String ATTACHMENTBUTTON_ID = "AttachmentButton";
   public final static String OKBUTTON_ID = "okButton";
   public final static String CANCEL_ID = "Cancel";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      try {
         // Locate effort record data set in form
         XComponent effortRecordSetParameter = (XComponent) parameters.get(WORK_EFFORT_RECORD_SET);
         XComponent timeRecordSetParameter = (XComponent) parameters.get(WORK_TIME_RECORD_SET);
         XComponent costRecordSetParameter = (XComponent) parameters.get(WORK_COST_RECORD_SET);
         
         XComponent effortRecordSet = form.findComponent(WORK_EFFORT_RECORD_SET);
         XComponent timeRecordSet = form.findComponent(WORK_TIME_RECORD_SET);
         XComponent costRecordSet = form.findComponent(WORK_COST_RECORD_SET);
         
         java.sql.Date date = (Date) parameters.get(DATE_FIELD);
         if (date != null) {
            form.findComponent(DATE_FIELD).setDateValue(date);
         }
         
         if (effortRecordSetParameter != null) {
            effortRecordSet.copyData(effortRecordSetParameter);
         }
         if (timeRecordSetParameter != null) {
            timeRecordSet.copyData(timeRecordSetParameter);
         }
         if (costRecordSetParameter != null) {
            costRecordSet.copyData(costRecordSetParameter);
         }

         String errorMsg = (String) parameters.get(ERROR_MSG);
         if (errorMsg != null && errorMsg.length() > 0) {
            XComponent errorLabel = form.findComponent(ERROR_LABEL);
            errorLabel.setText(errorMsg);
            errorLabel.setVisible(true);
         }

         //fill the list of resource ids
         Set resourceIds = OpWorkSlipDataSetFactory.getListOfSubordinateResourceIds(session, broker);
         if (resourceIds.isEmpty()) {
            return; //Maybe display a message that no resources are available?
         }
         else if (resourceIds.size() == 1) {
            // form.findComponent(TIMERESOURCECOLUMN_ID).setHidden(true);
            // form.findComponent(HOURSRESOURCECOLUMN_ID).setHidden(true);
            // form.findComponent(COSTSRESOURCECOLUMN_ID).setHidden(true);
         }

         List<Byte> activityTypes = new ArrayList<Byte>();
         activityTypes.add(OpActivity.STANDARD);
         activityTypes.add(OpActivity.MILESTONE);
         activityTypes.add(OpActivity.TASK);
         activityTypes.add(OpActivity.ADHOC_TASK);
         Date startBefore = getFilteredStartBeforeDate(session, parameters, form);


         List<OpAssignment> assignmentList = getAssignmentList(broker, resourceIds, activityTypes, startBefore, OpWorkSlipDataSetFactory.ALL_PROJECTS_ID);

         //fill project filter set
         XComponent projectFilterDataSet = form.findComponent(FILTER_PROJECT_SET);
         OpWorkSlipDataSetFactory.fillProjectSet(projectFilterDataSet, assignmentList);

         //fill all the choice data sets for all three tabs
         XComponent choiceTimeActivitySet = form.findComponent(OpWorkTimeValidator.ACTIVITY_SET);
         XComponent choiceTimeResourceSet = form.findComponent(OpWorkTimeValidator.RESOURCE_SET);
         XComponent choiceTimeProjectSet = form.findComponent(OpWorkTimeValidator.PROJECT_SET);
         XComponent choiceEffortActivitySet = form.findComponent(OpWorkEffortValidator.ACTIVITY_SET);
         XComponent choiceEffortResourceSet = form.findComponent(OpWorkEffortValidator.RESOURCE_SET);
         XComponent choiceEffortProjectSet = form.findComponent(OpWorkEffortValidator.PROJECT_SET);
         XComponent choiceCostActivitySet = form.findComponent(OpWorkCostValidator.ACTIVITY_SET);
         XComponent choiceCostResourceSet = form.findComponent(OpWorkCostValidator.RESOURCE_SET);
         XComponent choiceCostProjectSet = form.findComponent(OpWorkCostValidator.PROJECT_SET);

         //check time tracking
         boolean timeTrackingEnabled = false;
         String timeTracking = OpSettingsService.getService().getStringValue(session, OpSettings.ENABLE_TIME_TRACKING);
         if(timeTracking != null){
            timeTrackingEnabled = Boolean.valueOf(timeTracking);
         }

         //pulsing
         String pulsingSetting = OpSettingsService.getService().getStringValue(session, OpSettings.PULSING);
         if (pulsingSetting != null) {
            Integer pulsing = Integer.valueOf(pulsingSetting);
            form.findComponent(PULSING).setValue(pulsing);
         }


         long projectNodeId = getFilteredProjectNodeId(session, parameters, form);
         assignmentList = getAssignmentList(broker, resourceIds, activityTypes, startBefore, projectNodeId);


         OpTimeRecordDataSetFactory.fillChoiceDataSets(choiceTimeProjectSet, choiceTimeActivitySet, choiceTimeResourceSet, assignmentList);
         OpWorkEffortDataSetFactory.fillChoiceDataSets(choiceEffortProjectSet, choiceEffortActivitySet, choiceEffortResourceSet, assignmentList, timeTrackingEnabled);
         OpCostRecordDataSetFactory.fillChoiceDataSets(choiceCostProjectSet, choiceCostActivitySet, choiceCostResourceSet, assignmentList);

         OpWorkSlipDataSetFactory.sortChoiceDataSetByColumn(choiceTimeProjectSet, OpWorkValidator.PROJECT_CHOICE_SET_ORDER_INDEX, OpObjectOrderCriteria.ASCENDING);
         OpWorkSlipDataSetFactory.sortChoiceDataSetByColumn(choiceEffortProjectSet, OpWorkValidator.PROJECT_CHOICE_SET_ORDER_INDEX, OpObjectOrderCriteria.ASCENDING);
         OpWorkSlipDataSetFactory.sortChoiceDataSetByColumn(choiceCostProjectSet, OpWorkValidator.PROJECT_CHOICE_SET_ORDER_INDEX, OpObjectOrderCriteria.ASCENDING);
         OpWorkSlipDataSetFactory.sortChoiceDataSetByColumn(choiceTimeActivitySet, OpWorkValidator.ACTIVITY_CHOICE_SET_ORDER_INDEX, OpObjectOrderCriteria.ASCENDING);
         OpWorkSlipDataSetFactory.sortChoiceDataSetByColumn(choiceEffortActivitySet, OpWorkValidator.ACTIVITY_CHOICE_SET_ORDER_INDEX, OpObjectOrderCriteria.ASCENDING);
         OpWorkSlipDataSetFactory.sortChoiceDataSetByColumn(choiceCostActivitySet, OpWorkValidator.ACTIVITY_CHOICE_SET_ORDER_INDEX, OpObjectOrderCriteria.ASCENDING);
         OpWorkSlipDataSetFactory.sortChoiceDataSetByColumn(choiceTimeResourceSet, OpWorkValidator.RESOURCE_CHOICE_SET_ORDER_INDEX, OpObjectOrderCriteria.ASCENDING);
         OpWorkSlipDataSetFactory.sortChoiceDataSetByColumn(choiceEffortResourceSet, OpWorkValidator.RESOURCE_CHOICE_SET_ORDER_INDEX, OpObjectOrderCriteria.ASCENDING);
         OpWorkSlipDataSetFactory.sortChoiceDataSetByColumn(choiceCostResourceSet, OpWorkValidator.RESOURCE_CHOICE_SET_ORDER_INDEX, OpObjectOrderCriteria.ASCENDING);

         //set the "maps" between the projects -> activity, resources, activities -> resources, resouces -> activities
         //for all choice data sets
         OpWorkSlipDataSetFactory.configureProjectChoiceMap(broker, choiceTimeProjectSet, choiceTimeActivitySet, choiceTimeResourceSet);
         OpWorkSlipDataSetFactory.configureResourceChoiceMap(broker, choiceTimeResourceSet, choiceTimeActivitySet);
         OpWorkSlipDataSetFactory.configureActivityChoiceMap(broker, choiceTimeActivitySet, choiceTimeResourceSet, true);
         OpWorkSlipDataSetFactory.configureProjectChoiceMap(broker, choiceEffortProjectSet, choiceEffortActivitySet, choiceEffortResourceSet);
         OpWorkSlipDataSetFactory.configureResourceChoiceMap(broker, choiceEffortResourceSet, choiceEffortActivitySet);
         OpWorkSlipDataSetFactory.configureActivityChoiceMap(broker, choiceEffortActivitySet, choiceEffortResourceSet, true);
         OpWorkSlipDataSetFactory.configureProjectChoiceMap(broker, choiceCostProjectSet, choiceCostActivitySet, choiceCostResourceSet);
         OpWorkSlipDataSetFactory.configureResourceChoiceMap(broker, choiceCostResourceSet, choiceCostActivitySet);
         OpWorkSlipDataSetFactory.configureActivityChoiceMap(broker, choiceCostActivitySet, choiceCostResourceSet, true);

         //fill the assignmentMapDataField
         form.findComponent(ASSIGNMENT_MAP).setValue(OpWorkSlipDataSetFactory.getInstance().createAssignmentMap(session, broker, assignmentList));

         //filter effort, time & cost data sets
         OpWorkSlipDataSetFactory.filterDataSetForAssignments(effortRecordSet, assignmentList);
         OpWorkSlipDataSetFactory.filterDataSetForAssignments(timeRecordSet, assignmentList);
         OpWorkSlipDataSetFactory.filterDataSetForAssignments(costRecordSet, assignmentList);


         //fill the cost types for the costs tab
         XComponent costTypesDataSet = form.findComponent(COST_TYPES_SET);
         OpCostRecordDataSetFactory.fillCostTypesDataSet(session, costTypesDataSet);

         //check time tracking
         form.findComponent(TIME_TRACKING).setValue(timeTrackingEnabled);
         //if time tracking is off hide the time tab and select hours tab
         if(!timeTrackingEnabled) {
            form.findComponent(TIME_TAB).setHidden(true);
            form.findComponent(TAB_BOX).selectDifferentTab(1);
         }

         boolean effortHasChildren = choiceEffortActivitySet.getChildCount() > 0;
         form.findComponent(ADD_HOURS_BUTTON).setVisible(true);
         form.findComponent(REMOVE_HOURS_BUTTON).setVisible(true);
         XExtendedComponent effortTableBox = ((XExtendedComponent) form.findComponent(EFFORT_TABLE));
         effortTableBox.setSelectionModel(XComponent.CELL_SELECTION);
         effortTableBox.setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);

         boolean timeHasChildren = choiceTimeActivitySet.getChildCount() > 0;
         form.findComponent(ADD_TIME_BUTTON).setVisible(timeTrackingEnabled);
         form.findComponent(REMOVE_TIME_BUTTON).setVisible(timeTrackingEnabled);
         XExtendedComponent timeTableBox = ((XExtendedComponent) form.findComponent(TIME_TABLE));
         timeTableBox.setSelectionModel(XComponent.CELL_SELECTION);
         if (timeTrackingEnabled) {
            timeTableBox.setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);
         }
         else {
            ((XExtendedComponent) form.findComponent(TIME_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_NONE);
         }
         boolean costHasChildren = choiceCostActivitySet.getChildCount() > 0;
         form.findComponent(ADD_COST_BUTTON).setVisible(costHasChildren);
         form.findComponent(REMOVE_COST_BUTTON).setVisible(costHasChildren);
         form.findComponent(ATTACHMENT_BUTTON).setVisible(costHasChildren);
         XExtendedComponent costsTableBox = ((XExtendedComponent) form.findComponent(COST_TABLE));
         costsTableBox.setSelectionModel(XComponent.CELL_SELECTION);
         if (costHasChildren) {
            costsTableBox.setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);
         }
         else {
            ((XExtendedComponent) form.findComponent(COST_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_NONE);
         }

         // activitiesToFill
         List<XComponent> activityRows = (List<XComponent>) parameters.get(PRE_FILLED);
         if (activityRows != null) {
            OpWorkSlipDataSetFactory.addPrefilledAssignments(broker, activityRows, effortRecordSet, timeRecordSet, resourceIds);
            if (timeTrackingEnabled) {
               OpWorkEffortDataSetFactory.disableDataSetForTimeTracking(effortTableBox);
            }
         }
      }
      finally {
         broker.close();
      }

   }

   /**
    * Fills up a list of assignments for the given resources, activity types and projects.
    *
    * @param broker
    * @param resourceIds
    * @param activityTypes
    * @param startBefore
    * @param projectNodeId
    * @return a <code>List of OpAssignment <code>
    */
   private List<OpAssignment> getAssignmentList(OpBroker broker, Set resourceIds, List activityTypes, Date startBefore, long projectNodeId) {
      OpObjectOrderCriteria orderCriteria = OpWorkSlipDataSetFactory.createActivityOrderCriteria();
      Iterator result = OpWorkSlipDataSetFactory.getAssignments(broker, resourceIds, activityTypes, startBefore, orderCriteria, projectNodeId, false);

      List<OpAssignment> assignmentList = new ArrayList<OpAssignment>();
      Object[] record;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         assignmentList.add((OpAssignment) record[0]);
      }
      return assignmentList;
   }

   private Date getFilteredStartBeforeDate(OpProjectSession session, Map parameters, XComponent form) {
      //get start from choice field or session state
      String filteredStartFromId = (String) parameters.get(START_BEFORE_ID);
      XValidator.initChoiceField(form.findComponent(START_TIME_CHOICE_FIELD), filteredStartFromId);

      if (filteredStartFromId == null) {
         //set the default selected index for the time chooser
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null) {
            Integer defaultSelectedIndex = 0;
            stateMap.put(START_TIME_CHOICE_FIELD, defaultSelectedIndex);

         }
         return null;
      }

      boolean isFilterNextWeek = filteredStartFromId.equals(NEXT_WEEK);
      boolean isFilterNext2Weeks = filteredStartFromId.equals(NEXT_2_WEEKS);
      boolean isFilterNextMonth = filteredStartFromId.equals(NEXT_MONTH);
      boolean isFilterNext2Months = filteredStartFromId.equals(NEXT_2_MONTHS);

      //all selection
      Date start = null;

      if (isFilterNextWeek) {
         start = new Date(System.currentTimeMillis() + OpProjectCalendar.MILLIS_PER_WEEK * 1);
      }
      else if (isFilterNext2Weeks) {
         start = new Date(System.currentTimeMillis() + OpProjectCalendar.MILLIS_PER_WEEK * 2);
      }
      else if (isFilterNextMonth) {
         Calendar now = Calendar.getInstance();
         //skip to next month
         now.set(Calendar.MONTH, now.get(Calendar.MONTH) + 1);
         start = new Date(now.getTime().getTime());

      }
      else if (isFilterNext2Months) {
         Calendar now = Calendar.getInstance();
         //skip to next 2 months
         now.set(Calendar.MONTH, now.get(Calendar.MONTH) + 2);
         start = new Date(now.getTime().getTime());
      }

      return start;
   }

   private long getFilteredProjectNodeId(OpProjectSession session, Map parameters, XComponent form) {
      //get project from choice field
      String filteredProjectChoiceId = (String) parameters.get(PROJECT_CHOICE_ID);
      if (filteredProjectChoiceId == null) {
         //set the default selected index for the project chooser
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null) {
            Integer defaultSelectedIndex = 0;
            stateMap.put(PROJECT_CHOICE_FIELD, defaultSelectedIndex);
         }
         return OpWorkSlipDataSetFactory.ALL_PROJECTS_ID;
      }
      return filteredProjectChoiceId.equals(ALL) ? OpWorkSlipDataSetFactory.ALL_PROJECTS_ID : OpLocator.parseLocator(filteredProjectChoiceId).getID();
   }
}