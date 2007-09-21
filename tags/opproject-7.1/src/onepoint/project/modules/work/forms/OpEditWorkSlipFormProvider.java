/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.work.*;
import onepoint.project.modules.work.validators.OpWorkCostValidator;
import onepoint.project.modules.work.validators.OpWorkEffortValidator;
import onepoint.project.modules.work.validators.OpWorkTimeValidator;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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

   //parameter map keys
   private final static String WORK_SLIP_ID = "WorkSlipID";
   private final static String EDIT_MODE = "edit_mode";

   private static final String TIME_TAB = "TimeTab";
   private static final String TAB_BOX = "WorkSlipsTabBox";

   private final static String TIME_TRACKING = "TimeTrackingOn";
   private final static String PULSING = "Pulsing";
   private final static String ADD_HOURS_BUTTON = "AddHoursButton";
   private final static String REMOVE_HOURS_BUTTON = "RemoveHoursButton";
   private final static String CANCEL_BUTTON = "Cancel";
   private final static String WORK_INFO_MAP = "work.Info";
   private final static String WORK_INFO = "WorkInfo";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      boolean editMode = (Boolean) parameters.get("edit_mode");
      form.findComponent("EditMode").setBooleanValue(editMode);
      String workSlipLocator = (String) parameters.get(WORK_SLIP_ID);
      // TODO: Error handling (not set)

      OpWorkSlip workSlip = (OpWorkSlip) broker.getObject(workSlipLocator);
      // TODO: Error handling (not found, access denied)

      form.findComponent("WorkSlipIDField").setStringValue(workSlipLocator);
      XComponent dateField = form.findComponent(DATE_FIELD);
      dateField.setDateValue(workSlip.getDate());

      //Get the effort, time and cost data sets
      XComponent workEffortDataSet = form.findComponent(WORK_EFFORT_RECORD_SET);
      XComponent workTimeDataSet = form.findComponent(WORK_TIME_RECORD_SET);
      XComponent workCostDataSet = form.findComponent(WORK_COST_RECORD_SET);

      Iterator workRecords = workSlip.getRecords().iterator();
      List<OpWorkRecord> workRecordList = new ArrayList<OpWorkRecord>();
      OpWorkRecord workRecord;
      while (workRecords.hasNext()) {
         workRecord = (OpWorkRecord) (workRecords.next());
         workRecordList.add(workRecord);
      }

      //fill the three data sets
      List<XComponent> dataSetList = OpWorkSlipDataSetFactory.formDataSetsFromWorkRecords(workRecordList, session);
      workEffortDataSet.copyAllChildren(dataSetList.get(OpWorkSlipDataSetFactory.WORK_RECORD_SET_INDEX));
      workTimeDataSet.copyAllChildren(dataSetList.get(OpWorkSlipDataSetFactory.TIME_RECORD_SET_INDEX));
      workCostDataSet.copyAllChildren(dataSetList.get(OpWorkSlipDataSetFactory.COST_RECORD_SET_INDEX));

      //fill the list of resource ids
      List resourceIds = OpWorkSlipDataSetFactory.getListOfSubordinateResourceIds(session, broker);
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

      //check time tracking
      boolean timeTrackingEnabled = setTimeTracking(form, workEffortDataSet);

      //pulsing
      String pulsingSetting = OpSettings.get(OpSettings.PULSING);
      if (pulsingSetting != null) {
         Integer pulsing = Integer.valueOf(pulsingSetting);
         form.findComponent(PULSING).setValue(pulsing);
      }

      fillChoiceSets(broker, form, assignmentList, timeTrackingEnabled);

      //fill the assignmentMapDataField
      form.findComponent(ASSIGNMENT_MAP).setValue(OpWorkSlipDataSetFactory.createAssignmentMap(assignmentList));

      //fill the cost types for the costs tab
      XComponent costTypesDataSet = form.findComponent(COST_TYPES_SET);
      OpCostRecordDataSetFactory.fillCostTypesDataSet(session, costTypesDataSet);

      broker.close();

      Boolean paramEditMode = (Boolean) (parameters.get(EDIT_MODE));
      updateFormComponents(session, form, paramEditMode);

   }

   private void updateFormComponents(OpProjectSession session, XComponent form, Boolean paramEditMode) {
      form.findComponent(EFFORT_TABLE).setEditMode(paramEditMode);
      form.findComponent(TIME_TABLE).setEditMode(paramEditMode);
      form.findComponent(COST_TABLE).setEditMode(paramEditMode);
      form.findComponent(EFFORT_TABLE).setEnabled(paramEditMode);
      form.findComponent(TIME_TABLE).setEnabled(paramEditMode);
      form.findComponent(COST_TABLE).setEnabled(paramEditMode);
      form.findComponent(DATE_FIELD).setEnabled(paramEditMode);
      form.findComponent(CANCEL_BUTTON).setVisible(paramEditMode);
      String title = session.getLocale().getResourceMap(WORK_INFO_MAP).getResource(WORK_INFO).getText();
      form.setText(title);
   }

   private boolean setTimeTracking(XComponent form, XComponent workEffortDataSet) {
      boolean timeTrackingEnabled = false;
      String timeTracking = OpSettings.get(OpSettings.ENABLE_TIME_TRACKING);
      if (timeTracking != null) {
         timeTrackingEnabled = Boolean.valueOf(timeTracking);
      }
      if (!timeTrackingEnabled) {
         //if time tracking is off hide the time tab and select hours tab
         form.findComponent(TIME_TAB).setHidden(true);
         form.findComponent(TAB_BOX).selectDifferentTab(1);
      }
      else {
         //disable non-milestone activities on data set
         for (int i = 0; i < workEffortDataSet.getChildCount(); i++) {
            XComponent row = (XComponent) workEffortDataSet.getChild(i);
            if (!row.getChild(OpWorkEffortValidator.ACTIVITY_TYPE_INDEX).equals(OpGanttValidator.MILESTONE)) {
               row.getChild(OpWorkEffortValidator.PROJECT_NAME_INDEX).setEnabled(false);
               row.getChild(OpWorkEffortValidator.ACTIVITY_NAME_INDEX).setEnabled(false);
               row.getChild(OpWorkEffortValidator.RESOURCE_NAME_INDEX).setEnabled(false);
               row.getChild(OpWorkEffortValidator.ACTUAL_EFFORT_INDEX).setEnabled(false);
            }
         }
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

      //set the "maps" between the projects -> activity, resources, activities -> resources, resouces -> activities
      //for all choice data sets
      OpWorkSlipDataSetFactory.configureProjectChoiceMap(broker, choiceTimeProjectSet, choiceTimeActivitySet, choiceTimeResourceSet);
      OpWorkSlipDataSetFactory.configureResourceChoiceMap(broker, choiceTimeResourceSet, choiceTimeActivitySet);
      OpWorkSlipDataSetFactory.configureActivityChoiceMap(broker, choiceTimeActivitySet, choiceTimeResourceSet);
      OpWorkSlipDataSetFactory.configureProjectChoiceMap(broker, choiceEffortProjectSet, choiceEffortActivitySet, choiceEffortResourceSet);
      OpWorkSlipDataSetFactory.configureResourceChoiceMap(broker, choiceEffortResourceSet, choiceEffortActivitySet);
      OpWorkSlipDataSetFactory.configureActivityChoiceMap(broker, choiceEffortActivitySet, choiceEffortResourceSet);
      OpWorkSlipDataSetFactory.configureProjectChoiceMap(broker, choiceCostProjectSet, choiceCostActivitySet, choiceCostResourceSet);
      OpWorkSlipDataSetFactory.configureResourceChoiceMap(broker, choiceCostResourceSet, choiceCostActivitySet);
      OpWorkSlipDataSetFactory.configureActivityChoiceMap(broker, choiceCostActivitySet, choiceCostResourceSet);

      boolean hasChildren = choiceEffortActivitySet.getChildCount() > 0;
      form.findComponent(ADD_HOURS_BUTTON).setEnabled(hasChildren);
      form.findComponent(REMOVE_HOURS_BUTTON).setEnabled(hasChildren);
      if (hasChildren) {
         ((XExtendedComponent) form.findComponent(EFFORT_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);
      }
      else {
        ((XExtendedComponent) form.findComponent(EFFORT_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_NONE); 
      }

   }
}