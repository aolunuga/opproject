/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.work.OpCostRecordDataSetFactory;
import onepoint.project.modules.work.OpTimeRecordDataSetFactory;
import onepoint.project.modules.work.OpWorkEffortDataSetFactory;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.modules.work.OpWorkSlipDataSetFactory;
import onepoint.project.modules.work.validators.OpWorkCostValidator;
import onepoint.project.modules.work.validators.OpWorkEffortValidator;
import onepoint.project.modules.work.validators.OpWorkTimeValidator;
import onepoint.project.modules.work.validators.OpWorkValidator;
import onepoint.service.server.XSession;

public class OpEditWorkSlipFormProvider implements XFormProvider {

   private final static String DATE_FIELD = "DateField";

   private final static String WORK_EFFORT_RECORD_SET = "WorkEffortRecordSet";
   private final static String WORK_TIME_RECORD_SET = "WorkTimeRecordSet";
   private final static String WORK_COST_RECORD_SET = "WorkCostRecordSet";
   public final static String COST_TYPES_SET = "CostTypesSet";

   private static final String ASSIGNMENT_MAP = "AssignmentMap";

   private final static String EFFORT_TABLE = "EffortTable";
   private final static String TIME_TABLE = "TimeTable";
   private final static String COST_TABLE = "CostTable";
   private final static String ERROR_LABEL = "ErrorLabel";

   //parameter map keys
   private final static String WORK_SLIP_ID = "WorkSlipID";
   private final static String EDIT_MODE = "edit_mode";
   private final static String ERROR_MSG = "error_msg";

   private static final String TIME_TAB = "TimeTab";
   private static final String TAB_BOX = "WorkSlipsTabBox";

   private final static String TIME_TRACKING = "TimeTrackingOn";
   private final static String PULSING = "Pulsing";
   private final static String ADD_HOURS_BUTTON = "AddHoursButton";
   private final static String REMOVE_HOURS_BUTTON = "RemoveHoursButton";
   private final static String CANCEL_BUTTON = "Cancel";
   private final static String WORK_INFO_MAP = "work.Info";
   private final static String WORK_INFO = "WorkInfo";
   private final static String ADD_TIME_BUTTON = "AddTimeButton";
   private final static String REMOVE_TIME_BUTTON = "RemoveTimeButton";
   private final static String ADD_COST_BUTTON = "AddCostButton";
   private final static String REMOVE_COST_BUTTON = "RemoveCostButton";
   private final static String ATTACHMENT_BUTTON = "AttachmentButton";
   private final static String PRE_FILLED = "activitiesToFill";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      boolean editMode = (Boolean) parameters.get(EDIT_MODE);
      //check time tracking
      boolean timeTrackingEnabled = false;
      try {
         form.findComponent("EditMode").setBooleanValue(editMode);
         String workSlipLocator = (String) parameters.get(WORK_SLIP_ID);
         // TODO: Error handling (not set)
         String errorMsg = (String) parameters.get(ERROR_MSG);
         if (errorMsg != null && errorMsg.length() > 0) {
            XComponent errorLabel = form.findComponent(ERROR_LABEL);
            errorLabel.setText(errorMsg);
            errorLabel.setVisible(true);
         }
         
         OpWorkSlip workSlip = (OpWorkSlip) broker.getObject(workSlipLocator);
         // TODO: Error handling (not found, access denied)
         
         form.findComponent("WorkSlipIDField").setStringValue(workSlipLocator);
         XComponent dateField = form.findComponent(DATE_FIELD);
         dateField.setDateValue(workSlip.getDate());
         //date field always disabled for edit mode
         dateField.setEnabled(false);

         //Get the effort, time and cost data sets
         XComponent workEffortDataSet = form.findComponent(WORK_EFFORT_RECORD_SET);
         XComponent workTimeDataSet = form.findComponent(WORK_TIME_RECORD_SET);
         XComponent workCostDataSet = form.findComponent(WORK_COST_RECORD_SET);

         Iterator workRecords = workSlip.getRecords().iterator();
         List<OpWorkRecord> workRecordList = new ArrayList<OpWorkRecord>();
         
         //fill the list of resource ids
         Set<Long> resourceIds = OpWorkSlipDataSetFactory.getListOfSubordinateResourceIds(session, broker);
         // if we edit a workslip with resources we no longer own, we should get them from the workrecords and add them here:
         // this is safe because at the time the workslip was created we definitely owned them:
         
         OpWorkRecord workRecord;
         while (workRecords.hasNext()) {
            workRecord = (OpWorkRecord) (workRecords.next());
            workRecordList.add(workRecord);
            resourceIds.add(new Long(workRecord.getAssignment().getResource().getId()));
         }

         //fill the three data sets
         List<XComponent> dataSetList = OpWorkSlipDataSetFactory.formDataSetsFromWorkRecords(workRecordList, session, broker);
         workEffortDataSet.copyAllChildren(dataSetList.get(OpWorkSlipDataSetFactory.WORK_RECORD_SET_INDEX));
         workTimeDataSet.copyAllChildren(dataSetList.get(OpWorkSlipDataSetFactory.TIME_RECORD_SET_INDEX));
         workCostDataSet.copyAllChildren(dataSetList.get(OpWorkSlipDataSetFactory.COST_RECORD_SET_INDEX));

         // activitiesToFill
         List<XComponent> activityRows = (List<XComponent>) parameters.get(PRE_FILLED);
         if (activityRows != null) {
            OpWorkSlipDataSetFactory.addPrefilledAssignments(broker, activityRows, workEffortDataSet, workTimeDataSet, resourceIds);
         }
         
         if (resourceIds.isEmpty()) {
            return; //Maybe display a message that no resources are available?
         }

         List<Byte> activityTypes = new ArrayList<Byte>();
         activityTypes.add(OpActivity.STANDARD);
         activityTypes.add(OpActivity.MILESTONE);
         activityTypes.add(OpActivity.TASK);
         activityTypes.add(OpActivity.ADHOC_TASK);

         OpObjectOrderCriteria orderCriteria = OpWorkSlipDataSetFactory.createActivityOrderCriteria();
         Iterator result = OpWorkSlipDataSetFactory.getAssignments(broker, resourceIds, activityTypes, null, orderCriteria, OpWorkSlipDataSetFactory.ALL_PROJECTS_ID, true);
         List<OpAssignment> assignmentList = new ArrayList<OpAssignment>();
         Object[] record;
         while (result.hasNext()) {
            record = (Object[]) result.next();
            assignmentList.add((OpAssignment) record[0]);
         }

         //add the completed assignments from the work records of this work slip
         for (OpWorkRecord wr : workSlip.getRecords()) {
            if (wr.getAssignment().getComplete() == 100) {
               assignmentList.add(wr.getAssignment());
            }
         }
         
         timeTrackingEnabled = getTimeTracking(form, workEffortDataSet, session);
         //pulsing
         String pulsingSetting = OpSettingsService.getService().getStringValue(session, OpSettings.PULSING);
         if (pulsingSetting != null) {
            Integer pulsing = Integer.valueOf(pulsingSetting);
            form.findComponent(PULSING).setValue(pulsing);
         }

         fillChoiceSets(broker, form, assignmentList, timeTrackingEnabled);

         //fill the assignmentMapDataField
         form.findComponent(ASSIGNMENT_MAP).setValue(OpWorkSlipDataSetFactory.getInstance().createAssignmentMap(session, broker, assignmentList));

         //fill the cost types for the costs tab
         XComponent costTypesDataSet = form.findComponent(COST_TYPES_SET);
         OpCostRecordDataSetFactory.fillCostTypesDataSet(session, costTypesDataSet);

      }
      finally {
         broker.close();
      }

      Boolean paramEditMode = (Boolean) (parameters.get(EDIT_MODE));
      updateFormComponents(session, form, paramEditMode, timeTrackingEnabled);

   }

   private void updateFormComponents(OpProjectSession session, XComponent form, Boolean paramEditMode, boolean timeTrackingEnabled) {
      form.findComponent(EFFORT_TABLE).setEditMode(paramEditMode);
      form.findComponent(TIME_TABLE).setEditMode(paramEditMode && timeTrackingEnabled);
      form.findComponent(COST_TABLE).setEditMode(paramEditMode);
      form.findComponent(EFFORT_TABLE).setEnabled(paramEditMode);
      form.findComponent(TIME_TABLE).setEnabled(paramEditMode && timeTrackingEnabled);
      form.findComponent(COST_TABLE).setEnabled(paramEditMode);
      form.findComponent(CANCEL_BUTTON).setVisible(paramEditMode);
      form.findComponent(ADD_COST_BUTTON).setVisible(paramEditMode);
      form.findComponent(ADD_TIME_BUTTON).setVisible(paramEditMode && timeTrackingEnabled);
      form.findComponent(ADD_HOURS_BUTTON).setVisible(paramEditMode);
      form.findComponent(REMOVE_COST_BUTTON).setVisible(paramEditMode);
      form.findComponent(REMOVE_HOURS_BUTTON).setVisible(paramEditMode);
      form.findComponent(REMOVE_TIME_BUTTON).setVisible(paramEditMode && timeTrackingEnabled);
      form.findComponent(ATTACHMENT_BUTTON).setVisible(paramEditMode);
      
      String title = session.getLocale().getResourceMap(WORK_INFO_MAP).getResource(WORK_INFO).getText();
      form.setText(title);
   }

   private boolean getTimeTracking(XComponent form, XComponent workEffortDataSet, OpProjectSession session) {
      boolean timeTrackingEnabled = false;
      String timeTracking = OpSettingsService.getService().getStringValue(session, OpSettings.ENABLE_TIME_TRACKING);
      if (timeTracking != null) {
         timeTrackingEnabled = Boolean.valueOf(timeTracking);
      }
      if (timeTrackingEnabled) {
         OpWorkEffortDataSetFactory.disableDataSetForTimeTracking(form.findComponent(EFFORT_TABLE));
      }
      else {
         //if time tracking is off hide the time tab and select hours tab
         form.findComponent(TIME_TAB).setHidden(true);
         form.findComponent(TAB_BOX).selectDifferentTab(1);
      }

      form.findComponent(TIME_TRACKING).setValue(timeTrackingEnabled);

      return timeTrackingEnabled;
   }

   private void fillChoiceSets(OpBroker broker, XComponent form, List<OpAssignment> assignmentList, boolean timeTrackingEnabled) {
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


      OpTimeRecordDataSetFactory.fillChoiceDataSets(choiceTimeProjectSet, choiceTimeActivitySet, choiceTimeResourceSet, assignmentList);
      OpWorkEffortDataSetFactory.fillChoiceDataSets(choiceEffortProjectSet, choiceEffortActivitySet, choiceEffortResourceSet, assignmentList, timeTrackingEnabled);
      OpCostRecordDataSetFactory.fillChoiceDataSets(choiceCostProjectSet, choiceCostActivitySet, choiceCostResourceSet, assignmentList);
      
      // Sort those Choice-Datasets...
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

      boolean effortHasChildren = choiceEffortActivitySet.getChildCount() > 0;
      form.findComponent(ADD_HOURS_BUTTON).setVisible(true);
      form.findComponent(REMOVE_HOURS_BUTTON).setVisible(true);
      XExtendedComponent effortTableBox = ((XExtendedComponent) form.findComponent(EFFORT_TABLE));
      effortTableBox.setSelectionModel(XComponent.CELL_SELECTION);
      if (!timeTrackingEnabled) {
          effortTableBox.setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);
      }
      else {
         ((XExtendedComponent) form.findComponent(EFFORT_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_NONE);
      }
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
      XExtendedComponent costsBox = ((XExtendedComponent) form.findComponent(COST_TABLE));
      costsBox.setSelectionModel(XComponent.CELL_SELECTION);
      costsBox.setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);

   }
}