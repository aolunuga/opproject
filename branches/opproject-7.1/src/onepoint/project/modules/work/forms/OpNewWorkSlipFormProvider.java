/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import onepoint.express.XComponent;
import onepoint.express.XExtendedComponent;
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
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

public class OpNewWorkSlipFormProvider implements XFormProvider {

   private final static String WORK_EFFORT_RECORD_SET = "WorkEffortRecordSet";
   private final static String WORK_TIME_RECORD_SET = "WorkTimeRecordSet";
   private final static String WORK_COST_RECORD_SET = "WorkCostRecordSet";

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

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // Locate effort record data set in form
      XComponent effortRecordSet = form.findComponent(WORK_EFFORT_RECORD_SET);

      //fill the list of resource ids
      List resourceIds = OpWorkSlipDataSetFactory.getListOfSubordinateResourceIds(session, broker);
       if (resourceIds.isEmpty()) {
         return; //Maybe display a message that no resources are available?
      }

      List activityTypes = new ArrayList();
      activityTypes.add(new Byte(OpActivity.STANDARD));
      activityTypes.add(new Byte(OpActivity.MILESTONE));
      activityTypes.add(new Byte(OpActivity.TASK));
      activityTypes.add(new Byte(OpActivity.ADHOC_TASK));
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
      String timeTracking = OpSettingsService.getService().get(OpSettings.ENABLE_TIME_TRACKING);
      if(timeTracking != null){
         timeTrackingEnabled = Boolean.valueOf(timeTracking);
      }

      //pulsing
      String pulsingSetting = OpSettingsService.getService().get(OpSettings.PULSING);
      if (pulsingSetting != null) {
         Integer pulsing = Integer.valueOf(pulsingSetting);
         form.findComponent(PULSING).setValue(pulsing);
      }


      long projectNodeId = getFilteredProjectNodeId(session, parameters, form);
      assignmentList = getAssignmentList(broker, resourceIds, activityTypes, startBefore, projectNodeId);


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

      //fill the assignmentMapDataField
      form.findComponent(ASSIGNMENT_MAP).setValue(OpWorkSlipDataSetFactory.createAssignmentMap(assignmentList));

      //filter effort, time & cost data sets
      XComponent timeRecordSet = form.findComponent(WORK_TIME_RECORD_SET);
      XComponent costRecordSet = form.findComponent(WORK_COST_RECORD_SET);
      OpWorkSlipDataSetFactory.filterDataSetForAssignments(effortRecordSet, assignmentList);
      OpWorkSlipDataSetFactory.filterDataSetForAssignments(timeRecordSet, assignmentList);
      OpWorkSlipDataSetFactory.filterDataSetForAssignments(costRecordSet, assignmentList);

      broker.close();

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
      form.findComponent(ADD_HOURS_BUTTON).setVisible(effortHasChildren);
      form.findComponent(REMOVE_HOURS_BUTTON).setVisible(effortHasChildren);
      if (effortHasChildren) {
         ((XExtendedComponent) form.findComponent(EFFORT_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);
      }
      else {
         ((XExtendedComponent) form.findComponent(EFFORT_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_NONE);
      }
      boolean timeHasChildren = choiceTimeActivitySet.getChildCount() > 0;
      form.findComponent(ADD_TIME_BUTTON).setVisible(timeHasChildren);
      form.findComponent(REMOVE_TIME_BUTTON).setVisible(timeHasChildren);
      if (timeHasChildren) {
         ((XExtendedComponent) form.findComponent(TIME_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);
      }
      else {
        ((XExtendedComponent) form.findComponent(TIME_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_NONE);
      }
      boolean costHasChildren = choiceCostActivitySet.getChildCount() > 0;
      form.findComponent(ADD_COST_BUTTON).setVisible(costHasChildren);
      form.findComponent(REMOVE_COST_BUTTON).setVisible(costHasChildren);
      form.findComponent(ATTACHMENT_BUTTON).setVisible(costHasChildren);
      if (costHasChildren) {
         ((XExtendedComponent) form.findComponent(COST_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_CONSECUTIVE);
      }
      else {
        ((XExtendedComponent) form.findComponent(COST_TABLE)).setAutoGrow(XExtendedComponent.AUTO_GROW_NONE);
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
   private List<OpAssignment> getAssignmentList(OpBroker broker, List resourceIds, List activityTypes, Date startBefore, long projectNodeId) {
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
      /*get start from choice field or session state*/
      String filteredStartFromId = (String) parameters.get(START_BEFORE_ID);

      if (filteredStartFromId == null) { //set the default selected index for the time chooser
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null) {
            Integer defaultSelectedIndex = new Integer(0);
            stateMap.put(START_TIME_CHOICE_FIELD, defaultSelectedIndex);

         }
         return null;
      }

      boolean isFilterNextWeek = filteredStartFromId.equals(NEXT_WEEK);
      boolean isFilterNext2Weeks = filteredStartFromId.equals(NEXT_2_WEEKS);
      boolean isFilterNextMonth = filteredStartFromId.equals(NEXT_MONTH);
      boolean isFilterNext2Months = filteredStartFromId.equals(NEXT_2_MONTHS);

      Date start = null; //all selection

      if (isFilterNextWeek) {
         start = new Date(System.currentTimeMillis() + XCalendar.MILLIS_PER_WEEK * 1);
      }
      else if (isFilterNext2Weeks) {
         start = new Date(System.currentTimeMillis() + XCalendar.MILLIS_PER_WEEK * 2);
      }
      else if (isFilterNextMonth) {
         Calendar now = Calendar.getInstance();
         /*skip to next month */
         now.set(Calendar.MONTH, now.get(Calendar.MONTH) + 1);
         start = new Date(now.getTime().getTime());

      }
      else if (isFilterNext2Months) {
         Calendar now = Calendar.getInstance();
         /*skip to next 2 months */
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
            Integer defaultSelectedIndex = new Integer(0);
            stateMap.put(PROJECT_CHOICE_FIELD, defaultSelectedIndex);
         }
         return OpWorkSlipDataSetFactory.ALL_PROJECTS_ID;
      }
      return filteredProjectChoiceId.equals(ALL) ? OpWorkSlipDataSetFactory.ALL_PROJECTS_ID : OpLocator.parseLocator(filteredProjectChoiceId).getID();
   }
}