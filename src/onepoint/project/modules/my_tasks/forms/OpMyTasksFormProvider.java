/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks.forms;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.my_tasks.OpMyTasksServiceImpl;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityFilter;
import onepoint.project.modules.project.OpActivityIfc;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpSubjectDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.validators.OpProjectValidator;
import onepoint.service.server.XSession;

public class OpMyTasksFormProvider implements XFormProvider {

   protected final static String ACTIVITY_SET = "ActivitySet";
   private final static String CATEGORY_COLOR_DATA_SET = "CategoryColorDataSet";
   private final static String PRINT_TITLE = "PrintTitle";
   private final static String PROJECT_CHOICE_FIELD = "ProjectChooser";
   private final static String START_TIME_CHOICE_FIELD = "StartTimeChooser";
   private final static String RESOURCE_CHOICE_FIELD = "ResourcesChooser";
   private final static String START_BEFORE_SET = "StartBeforeSet";
   private final static String PROJECT_SET = "ProjectSet";
   private final static String RESOURCES_SET = "FilterResourcesSet";
   private final static String EXISTING_WORK_SLIP = "ExistingWorkSlip";
   private final static String EDITABLE_WORK_SLIP = "EditableWorkSlip";
   private final static String DELETE_PERMISSION_SET = "DeletePermissionSet";
   private final static String RESOURCE_AVAILABILITY = "ResourceAvailability";
   private final static String ACTIVITY_GANTT_CHART = "ActivityGanttChart";
   private final static String PROJECT_ID_FIELD = "ProjectIDField";

   // filters
   private final static String START_BEFORE_ID = "start_before_id";
   private final static String PROJECT_CHOICE_ID = "project_choice_id";
   private final static String RESOURCE_CHOICE_ID = "resources_choice_id";

   // start from filter choices
   private final static String ALL = "all";
   private final static String RESPONSIBLE = "res";
   private final static String MANAGED = "man";
   private final static List<String> RESOURCE_FILTER_OPTIONS = Arrays.asList(ALL, RESPONSIBLE, MANAGED);

   private final static String NEXT_WEEK = "nw";
   private final static String THIS_WEEK = "tw";
   private final static String NEXT_2_WEEKS = "n2w";
   private final static String NEXT_MONTH = "nm";
   private final static String NEXT_2_MONTHS = "n2m";

   private final static String NEW_COMMENT_BUTTON = "NewCommentButton";
   private final static String INFO_BUTTON = "InfoButton";
   private final static String PRINT_BUTTON = "PrintButton";
   private final static String NEW_ADHOC_BUTTON = "NewAdhocButton";

   private final static String NO_RESOURCES_FOR_USER_ID = "NoResourcesForUser";
   private final static String MESSAGE_LABEL_ID = "MessageLabel";

   // ---
   public final static String MYTASKSFORM_ID = "MyTasksForm";
   public final static String ACTIVITIESTABLEBASEROWMAP_ID = "ActivitiesTableBaseRowMap";
   public final static String ACTIVITIESTABLEROWLOCATORMAP_ID = "ActivitiesTableRowLocatorMap";
   public final static String ACTIVITYSET_ID = "ActivitySet";
   public final static String CATEGORYCOLORDATASET_ID = "CategoryColorDataSet";
   public final static String EDITMODEFIELD_ID = "EditModeField";
   public final static String PRINTTITLE_ID = "PrintTitle";
   public final static String DELETEPERMISSIONSET_ID = "DeletePermissionSet";
   public final static String NORESOURCESFORUSER_ID = "NoResourcesForUser";
   public final static String RESOURCEAVAILABILITY_ID = "ResourceAvailability";
   public final static String EXISTINGWORKSLIP_ID = "ExistingWorkSlip";
   public final static String EDITABLEWORKSLIP_ID = "EditableWorkSlip";
   public final static String ACTIVITYTABLEPOPUPMENU_ID = "ActivityTablePopupMenu";
   public final static String ACTIVITYCHARTPOPUPMENU_ID = "ActivityChartPopupMenu";
   public final static String INDICATORICONSET_ID = "IndicatorIconSet";
   public final static String TIMEUNITSET_ID = "TimeUnitSet";
   public final static String STARTBEFORESET_ID = "StartBeforeSet";
   public final static String FILTERRESOURCESSET_ID = "FilterResourcesSet";
   public final static String PROJECTSET_ID = "ProjectSet";
   public final static String MYTASKSTOOLBAR_ID = "myTasksToolBar";
   public final static String NEWCOMMENTBUTTON_ID = "NewCommentButton";
   public final static String NEWADHOCBUTTON_ID = "NewAdhocButton";
   public final static String INFOBUTTON_ID = "InfoButton";
   public final static String DELETEADHOCBUTTON_ID = "DeleteAdhocButton";
   public final static String PRINTBUTTON_ID = "PrintButton";
   public final static String PROJECTCHOOSER_ID = "ProjectChooser";
   public final static String STARTTIMECHOOSER_ID = "StartTimeChooser";
   public final static String RESOURCESCHOOSER_ID = "ResourcesChooser";
   public final static String ERRORLABEL_ID = "ErrorLabel";
   public final static String MESSAGELABEL_ID = "MessageLabel";
   public final static String MYTABBOX_ID = "MyTabBox";
   public final static String TASKLISTTAB_ID = "TasklistTab";
   public final static String ACTIVITYTABLE_ID = "ActivityTable";
   public final static String TIMEUNITCHOOSER_ID = "TimeUnitChooser";
   public final static String ACTIVITYGANTTCHART_ID = "ActivityGanttChart";
   public static final String MY_RESOURCES = "MyResources";

   /**
    * A list of activity types to filter after
    */
   private final static List<Byte> TYPES_FILTER_LIST = Arrays.asList(OpActivity.STANDARD, OpActivity.MILESTONE, OpActivity.TASK, OpActivity.ADHOC_TASK);

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      OpBroker broker = session.newBroker();
      try {
         OpProjectValidator.transformRowMapToDataSet(
               OpActivityDataSetFactory.ACTIVITY_ROW_BASE_DESCRIPTION, form
                     .findComponent(ACTIVITIESTABLEBASEROWMAP_ID));
         OpProjectValidator.transformRowMapToDataSet(
               OpActivityDataSetFactory.ACTIVITY_ROW_LOCATOR_DESCRIPTION, form
                     .findComponent(ACTIVITIESTABLEROWLOCATORMAP_ID));

         Map<String, List<String>> projectsResourcesMap = OpProjectDataSetFactory.getProjectToResourceMap(session);

         //check the case when the current user doesn't  see any resources
         if (projectsResourcesMap.isEmpty()) {
            handleNoResources(form);
            return;
         }

         OpUser user = session.user(broker);
         
         Set<OpResource> resources = user.getResources();
         Set<String> resourceName = new HashSet<String>();
         if (resources != null) {
        	 for (OpResource resource : resources) {
        		 resourceName.add(resource.getName());
        	 }
         }
    	 XComponent resourcesSet = form.findComponent(MY_RESOURCES);
    	 resourcesSet.setValue(resourceName);
    	 //check the manager rights
         if (OpSubjectDataSetFactory.shouldHideFromUser(session, user)) {
            ((OpProjectComponent) form.findComponent(ACTIVITY_GANTT_CHART)).setShowCosts(false);
         }

         fillWorkslipData(form, broker, user);

         form.findComponent(PRINT_TITLE).setStringValue(user.getName());

         //fill this form's filters
         this.fillResourceFilter(form, projectsResourcesMap);
         this.fillProjectFilter(form, projectsResourcesMap);

         XComponent dataSet = form.findComponent(ACTIVITY_SET);

         if (dataSet.selectedRows().size() == 0) {
            form.findComponent(NEW_COMMENT_BUTTON).setEnabled(false);
         }

         //create the activity filter
         OpActivityFilter activityFilter = createActivityFilter(session, broker, parameters, form, user.getResources(), projectsResourcesMap);
         //retrieve the activities
         this.fillActivityDataSet(activityFilter, user, session, broker, dataSet);

         // check buttons that need enabling or disabling
         this.checkButtons(dataSet, form, projectsResourcesMap);

         // fill category color data set
         XComponent categoryColorDataSet = form.findComponent(CATEGORY_COLOR_DATA_SET);
         OpActivityDataSetFactory.fillCategoryColorDataSet(broker, categoryColorDataSet);

         // fill delete permission data set
         XComponent deletePermissionSet = form.findComponent(DELETE_PERMISSION_SET);
         fillDeletePermissionDataSet((OpProjectSession) s, broker, dataSet, deletePermissionSet);

         //fill the availability map
         XComponent resourceAvailability = form.findComponent(RESOURCE_AVAILABILITY);
         Map<String, Double> availabilityMap = OpResourceDataSetFactory.createResourceAvailabilityMap(broker);
         resourceAvailability.setValue(availabilityMap);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Fills the work slip form data fields with existing work slip information.
    *
    * @param form my task form
    * @param broker current  broker
    * @param user session user
    */
   private void fillWorkslipData(XComponent form, OpBroker broker, OpUser user) {
      Date today = OpProjectCalendar.today();
      OpQuery query = broker.newQuery("select workslip.id, workslip.State from OpWorkSlip workslip where workslip.Date = :wsDate and workslip.Creator.id = :user");
      query.setDate("wsDate", today);
      query.setLong("user", user.getId());
      Iterator iterator = broker.iterate(query);
      if (iterator.hasNext()) {
         Object[] results = (Object[]) iterator.next();
         long id = (Long) results[0];
         String locator = OpLocator.locatorString(OpTypeManager.getPrototypeByClassName(OpWorkSlip.class.getName()), id);
         form.findComponent(EXISTING_WORK_SLIP).setValue(locator);
         int state = (Integer) results[1];
         form.findComponent(EDITABLE_WORK_SLIP).setBooleanValue(state == OpWorkSlip.STATE_EDITABLE);
      }
   }

   /**
    * Fills the activity data-set with data.
    *
    * @param activityFilter a <code>OpActivityFilter</code> used to retrieve activities.
    * @param user           a <code>OpUser</code> representing the current user.
    * @param broker         a <code>OpBroker</code> used for db operations.
    * @param dataSet        a <code>XComponent(DATA_SET)</code> representing the activity data-set.
    */
   private void fillActivityDataSet(OpActivityFilter activityFilter, OpUser user, OpProjectSession session, OpBroker broker, XComponent dataSet) {
      //only makes sense to retrieve any activities if we have projects and resources set
      if (activityFilter.getProjectNodeIds().isEmpty() || activityFilter.getResourceIds().isEmpty()) {
         return;
      }

      // Configure activity sort order
      SortedMap<String, Integer> sortOrders = new TreeMap<String, Integer>();
      sortOrders.put(OpActivity.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
      sortOrders.put(OpActivity.PRIORITY, OpObjectOrderCriteria.ASCENDING);
      OpObjectOrderCriteria orderCriteria = new OpObjectOrderCriteria(OpActivity.class, sortOrders);

      // Retrieve filtered and ordered activity data-set
      XComponent unsortedDataSet = new XComponent(XComponent.DATA_SET);
      Boolean showHours = getShowHoursPreference(broker, user);
      unsortedDataSet.setValue(showHours);
      OpActivityDataSetFactory.getInstance().retrieveFilteredActivityDataSet(session, broker, activityFilter, orderCriteria, unsortedDataSet);
      Map<Integer, String> indexIdMap = createIndexIdMap(unsortedDataSet);
      this.sortActivityDataSet(unsortedDataSet, dataSet);
      Map<String, Integer> idIndexMap = createIdIndexMap(dataSet);

      //rebuild the successors and predecessors indexes in the dataset
      OpActivityDataSetFactory.rebuildPredecessorsSuccessorsIndexes(dataSet, indexIdMap, idIndexMap);

      //set the status value for the my task activities.
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent row = (XComponent) dataSet.getChild(i);
         Date endDate = OpGanttValidator.getEnd(row);
         if (endDate != null && endDate.before(OpProjectCalendar.today()) && OpGanttValidator.getComplete(row) < 100) {
            OpGanttValidator.setStatus(row, OpGanttValidator.OVERDUE_ACTIVITY);
         }
         else {
            OpGanttValidator.setStatus(row, OpGanttValidator.NOT_OVERDUE_ACTIVITY);
         }
      }
   }

   /**
    * Sorts an actviity data-set after the start date of non-ad-hoc task activities, and copies the result into another dataset.
    *
    * @param unsortedDataSet a <code>XComponent(DATA_SET)</code> representing an unsorted data-set.
    * @param sortedDataSet   a <code>XComponent(DATA_SET)</code> representing a data-set where to place the sorted rows.
    */
   private void sortActivityDataSet(XComponent unsortedDataSet, XComponent sortedDataSet) {
      List<XComponent> adHocTasks = new ArrayList<XComponent>();
      List<XComponent> rows = unsortedDataSet.asList();
      for (XComponent row : rows) {
         unsortedDataSet.removeChild(row);
         if (OpGanttValidator.getType(row) == OpGanttValidator.ADHOC_TASK) {
            adHocTasks.add(row);
         }
         else {
            sortedDataSet.addChild(row);
         }
      }
      unsortedDataSet.removeAllChildren();
      sortedDataSet.sort(OpGanttValidator.START_COLUMN_INDEX);
      sortedDataSet.addAllChildren(adHocTasks.toArray(new XComponent[]{}));
   }

   /**
    * Gets the user preferences to show the assignment values in % or in hours.
    *
    * @param user a <code>OpUser</code> representing the current user.
    * @return a <code>Boolean</code> indicating whether to show assignments in hours or not.
    */
   private Boolean getShowHoursPreference(OpBroker broker, OpUser user) {
      String showHoursPref = user.getPreferenceValue(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showHoursPref == null) {
         showHoursPref = OpSettingsService.getService().getStringValue(broker, OpSettings.SHOW_RESOURCES_IN_HOURS);
      }
      return Boolean.valueOf(showHoursPref);
   }

   /**
    * Handles the case when there are no resources for the current user.
    *
    * @param form a <code>XComponent(FORM)</code> representing the my tasks form.
    */
   protected void handleNoResources(XComponent form) {
      //show the error
      XComponent errorLabel = form.findComponent(MESSAGE_LABEL_ID);
      errorLabel.setText(form.findComponent(NO_RESOURCES_FOR_USER_ID).getText());
      errorLabel.setVisible(true);

      form.findComponent(NEW_COMMENT_BUTTON).setEnabled(false);
      form.findComponent(INFO_BUTTON).setEnabled(false);
      form.findComponent(PRINT_BUTTON).setEnabled(false);
      form.findComponent(NEW_ADHOC_BUTTON).setEnabled(false);
      form.findComponent(PROJECT_CHOICE_FIELD).setEnabled(false);
      form.findComponent(START_TIME_CHOICE_FIELD).setEnabled(false);
      form.findComponent(RESOURCE_CHOICE_FIELD).setEnabled(false);
   }


   /**
    * Fills the delete permission set. For each activity we determine if we have the rights to delete it and we add a propper row in the set.
    *
    * @param s                   the session
    * @param broker              the broker
    * @param dataSet             the activity dataset
    * @param deletePermissionSet he delete permission dataset
    */
   private void fillDeletePermissionDataSet(OpProjectSession s, OpBroker broker, XComponent dataSet, XComponent deletePermissionSet) {
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent row = (XComponent) dataSet.getChild(i);
         String locator = row.getStringValue();
         OpActivityIfc activity = (OpActivityIfc) broker.getObject(locator);
         boolean delete = OpMyTasksServiceImpl.deleteGranted(s, broker, activity);
         XComponent delRow = new XComponent(XComponent.DATA_ROW);
         delRow.setBooleanValue(delete);
         deletePermissionSet.addChild(delRow);
      }
   }

   /**
    * Fills up the project filter data set.
    *
    * @param projectsResourcesMap a <code>Map</code> containing all the project where the current user is at least observer and the list of resources on these projects.
    * @param form                 a <code>XComponent(FORM)</code> representing the current form.
    */
   private void fillProjectFilter(XComponent form, Map<String, List<String>> projectsResourcesMap) {
      XComponent projectDataSet = form.findComponent(PROJECT_SET);

      //add projects that are only for adhoc tasks
      for (String projectChoice : projectsResourcesMap.keySet()) {
         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(projectChoice);
         projectDataSet.addChild(row);
      }
   }

   /**
    * Disables the tool buttons if no actions are possible.
    *
    * @param dataSet          tasks data set
    * @param form             current form
    * @param adhocProjectsMap project->(list of resources) map.
    */
   protected void checkButtons(XComponent dataSet, XComponent form, Map adhocProjectsMap) {
      //if dataset is empty, disable all the buttons
      if (dataSet.getChildCount() == 0) {
         form.findComponent(NEW_COMMENT_BUTTON).setEnabled(false);
         form.findComponent(INFO_BUTTON).setEnabled(false);
         form.findComponent(PRINT_BUTTON).setEnabled(false);
      }
      if (adhocProjectsMap.isEmpty()) {
         form.findComponent(NEW_ADHOC_BUTTON).setEnabled(false);
      }
   }

   /**
    * Gets the start form the period filter
    *
    * @param session                       the project session
    * @param filteredStartFromRequestParam a <code>String<code> representing the value of the "start from" request parameter
    * @param form                          the form
    * @return a <code>Date</code> representing the start date for the filter
    */
   private Date getFilterStart(OpProjectSession session, String filteredStartFromRequestParam, XComponent form) {
      //get start from choice field or session state
      String filteredStartFromId = getFilteredStartBeforeId(session, filteredStartFromRequestParam, form);

      //retrieve the server calendar and initialize it
      OpProjectCalendar serverCalendar = session.getCalendar();
      Calendar calendarGMT = OpProjectCalendarFactory.getInstance().getDefaultCalendar(session).cloneCalendarInstance();
      calendarGMT.setTimeInMillis(System.currentTimeMillis());
      calendarGMT.set(Calendar.HOUR_OF_DAY, 0);
      calendarGMT.set(Calendar.MINUTE, 0);
      calendarGMT.set(Calendar.SECOND, 0);

      if (filteredStartFromId.equals(THIS_WEEK)) {
         // in order to end the time interval at the last weekend day(and not reach the first day of work of the next week)
         // substract one second from the calculated time
         calendarGMT.setTimeInMillis(calendarGMT.getTimeInMillis() + OpProjectCalendar.MILLIS_PER_DAY *
              daysLeftInWeek(serverCalendar, calendarGMT) - 1000);
      }
      else if (filteredStartFromId.equals(NEXT_WEEK)) {
         calendarGMT.setTimeInMillis(calendarGMT.getTimeInMillis() + OpProjectCalendar.MILLIS_PER_WEEK);
      }
      else if (filteredStartFromId.equals(NEXT_2_WEEKS)) {
         calendarGMT.setTimeInMillis(calendarGMT.getTimeInMillis() + OpProjectCalendar.MILLIS_PER_WEEK * 2);
      }
      else if (filteredStartFromId.equals(NEXT_MONTH)) {
         calendarGMT.set(Calendar.MONTH, calendarGMT.get(Calendar.MONTH) + 1);
      }
      else if (filteredStartFromId.equals(NEXT_2_MONTHS)) {
         calendarGMT.set(Calendar.MONTH, calendarGMT.get(Calendar.MONTH) + 2);
      }
      else {
         return null;
      }
      return new Date(calendarGMT.getTimeInMillis());
   }

   /**
    * Fills up the resource filter data set.
    *
    * @param form                 Current form (the filter data set is in)
    * @param projectsResourcesMap Map of (project) -> (list of resources)
    */
   private void fillResourceFilter(XComponent form, Map<String, List<String>> projectsResourcesMap) {
      XComponent resourceFilterDataSet = form.findComponent(RESOURCES_SET);

      //if standalone, remove the responsible and managed selection
      if (!OpEnvironmentManager.isMultiUser()) {
         prepareFilterForStandalone(resourceFilterDataSet);
      }

      Set<String> allResources = new TreeSet<String>();
      Map<String, String> captionToResourceMap = new HashMap<String, String>();
      for (List<String> resourcesList : projectsResourcesMap.values()) {
         for (String resourceChoice : resourcesList) {
            String caption = XValidator.choiceCaption(resourceChoice);
            allResources.add(caption);
            captionToResourceMap.put(caption, resourceChoice);
         }
      }

      for (String caption : allResources) {
         String choice = captionToResourceMap.get(caption);
         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(choice);
         resourceFilterDataSet.addChild(row);
      }
   }

   /**
    * Prepares the resource filter for the standalone distribution of the application.
    *
    * @param resourceFilterDataSet a <code>XComponent(DATA_SET)</code> representing
    *                              the filter data-set.
    */
   private void prepareFilterForStandalone(XComponent resourceFilterDataSet) {
      List<XComponent> rowsToRemove = new ArrayList<XComponent>();
      for (int i = 0; i < resourceFilterDataSet.getChildCount(); i++) {
         XComponent resFilterRow = (XComponent) resourceFilterDataSet.getChild(i);
         String resFilterChoiceId = XValidator.choiceID(resFilterRow.getStringValue());
         if (resFilterChoiceId.equalsIgnoreCase(MANAGED) || resFilterChoiceId.equalsIgnoreCase(RESPONSIBLE)) {
            rowsToRemove.add(resFilterRow);
         }
      }
      resourceFilterDataSet.removeChildren(rowsToRemove);
   }

   /**
    * Creates an activity filter and sets the projects on it using the filter project choice.
    *
    * @param session              Current session. used to obtain the project filter
    * @param broker               a <code>OpBroker</code> used for db operations.
    * @param parameters           Form parameters.
    * @param form                 Current form
    * @param responsibleResources a <code>Set(OpResource)</code> representing the responsible resources for the current user.
    * @param projectResourcesMap  a <code>Map</code> of project ids and a list of resources for each project.
    * @return an <code>OpActivityFilter</code> instance.
    */
   private OpActivityFilter createActivityFilter(OpProjectSession session, OpBroker broker, HashMap parameters,
        XComponent form, Set<OpResource> responsibleResources, Map<String, List<String>> projectResourcesMap) {
      OpActivityFilter filter = new OpActivityFilter();
      filter.setDependencies(true);
      filter.setCompleted(Boolean.FALSE);
      filter.setAssignmentCompleted(Boolean.FALSE);

      //add the type filters
      for (byte type : TYPES_FILTER_LIST) {
         filter.addType(type);
      }

      //set the start of the filter according the filter from the UI
      String filteredStartRequestParam = (String) parameters.get(START_BEFORE_ID);
      Date startDate = getFilterStart(session, filteredStartRequestParam, form);
      if (startDate != null) {
         filter.setStartTo(startDate);
      }

      //add project ids to the activity filter, according to the UI
      String projectChoiceId = (String) parameters.get(PROJECT_CHOICE_ID);
      String projectOptionId = activateProjectFilterOptionId(projectChoiceId, session, form);
      List<Long> projectIds = getFilteredProjectIds(projectOptionId, projectResourcesMap);
      for (long projectId : projectIds) {
         filter.addProjectNodeID(projectId);
      }

      //add the resource ids to the filter, according to the UI
      String resourceChoiceId = (String) parameters.get(RESOURCE_CHOICE_ID);
      String resourceOptionId = activateResourceFilterOptionId(session, resourceChoiceId, form);
      List<Long> resourceIds = getFilteredResourceIds(session, broker, resourceOptionId, responsibleResources, projectResourcesMap);
      for (long resourceId : resourceIds) {
         filter.addResourceID(resourceId);
      }
      
      filter.setWorkPhases(true);
      return filter;
   }

   /**
    * Returns the value of the <code>START_FROM_ID</code>(choice field selection) from the <code>parameters</code> list.
    * If it's not found there, the search is performed in <code>form</code>'s state map kept on the <code>session<code>.
    *
    * @param session                       <code>OpProjectSession</code> the session
    * @param filteredStartFromRequestParam a <code>String<code> representing the value of the "start from" request parameter
    * @param form                          <code>XComponent.FORM</code> for which this class is provider
    * @return <code>String</code> representing the value of the choice field selection.
    */
   private String getFilteredStartBeforeId(OpProjectSession session, String filteredStartFromRequestParam, XComponent form) {
      if (filteredStartFromRequestParam == null) { //get start id from form's state
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null) {
            Integer selectedIndex = (Integer) stateMap.get(START_TIME_CHOICE_FIELD);
            if (selectedIndex != null) {
               XComponent startTimeDataSet = form.findComponent(START_BEFORE_SET);
               if (selectedIndex < startTimeDataSet.getChildCount()) {
                  XComponent dataRow = (XComponent) startTimeDataSet.getChild(selectedIndex.intValue());
                  filteredStartFromRequestParam = XValidator.choiceID(dataRow.getStringValue());
               }
            }
         }
      }
      if (filteredStartFromRequestParam == null) {
         filteredStartFromRequestParam = NEXT_MONTH; //default value for period filter
      }
      return filteredStartFromRequestParam;
   }

   /**
    * Returns the value of the resources choice field selection from the <code>parameters</code> list.
    * If it's not found there, the search is performed in <code>form</code>'s state map kept on the <code>session<code>.
    *
    * @param session          <code>OpProjectSession</code> the session
    * @param resourceChoiceId <code>String<code> representing the user selected option from the resources filter
    * @param form             <code>XComponent.FORM</code> for which this class is provider
    * @return <code>String</code> representing the value of the choice field selection.
    */
   private String activateResourceFilterOptionId(OpProjectSession session, String resourceChoiceId, XComponent form) {
      if (resourceChoiceId == null) {
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null) {
            Integer selectedIndex = (Integer) stateMap.get(RESOURCE_CHOICE_FIELD);
            if (selectedIndex != null) {
               XComponent resourceDataSet = form.findComponent(RESOURCES_SET);
               if (selectedIndex < resourceDataSet.getChildCount()) {
                  XComponent dataRow = (XComponent) resourceDataSet.getChild(selectedIndex.intValue());
                  resourceChoiceId = XValidator.choiceID(dataRow.getStringValue());
               }
            }
         }
      }
      if (resourceChoiceId != null) {
         return resourceChoiceId;
      }
      else {
         return OpEnvironmentManager.isMultiUser() ? RESPONSIBLE : ALL;
      }
   }

   /**
    * Returns a list of resource ids, representing those ids which should be used when filtering the activities.
    *
    * @param session              a <code>OpProjectSession</code> instance representing the server session.
    * @param broker               a <code>OpBroker</code> used for db operations.
    * @param resourceOptionId     a <code>String</code> representing the option of the resource filter (from the UI).
    * @param responsibleResources a <code>Set(OpResource)</code> representing the responsible resources for the current user.
    * @param projectResourcesMap  a <code>Map</code> of project ids and a list of resources for each project.
    * @return a <code>List(Long)</code> representing a list of resource ids.
    */
   private List<Long> getFilteredResourceIds(OpProjectSession session, OpBroker broker,
        String resourceOptionId, Set<OpResource> responsibleResources, Map<String, List<String>> projectResourcesMap) {
      List<Long> result = new ArrayList<Long>();

      //only if the option id is a <real> resource locator
      if (!RESOURCE_FILTER_OPTIONS.contains(resourceOptionId)) {
         long resourceId = OpLocator.parseLocator(resourceOptionId).getID();
         result.add(resourceId);
         return result;
      }

      for (List<String> resourcesList : projectResourcesMap.values()) {
         for (String resourceChoice : resourcesList) {
            String resourceLocator = XValidator.choiceID(resourceChoice);
            OpResource resource = (OpResource) broker.getObject(resourceLocator);
            boolean acceptResource = true;

            if (resourceOptionId.equals(MANAGED) && session.effectiveAccessLevel(broker, resource.getId()) < OpPermission.MANAGER) {
               acceptResource = false;
            }
            else if (resourceOptionId.equals(RESPONSIBLE) && !responsibleResources.contains(resource)) {
               acceptResource = false;
            }

            if (acceptResource) {
               result.add(resource.getId());
            }
         }
      }
      return result;
   }

   /**
    * Returns the value of the <code>PROJECT_CHOICE_ID</code>(choice field selection) from the <code>parameters</code> list.
    * If it's not found there,default selected index is set in <code>form</code>'s state map.
    *
    * @param projectOptionId     <code>String<code> representing the locator of a project (may be null)
    * @param projectResourcesMap a <code>Map</code> of project ids and a list of resources for each project.
    * @return <code>String</code> representing the value of the choice field selection.
    */
   private List<Long> getFilteredProjectIds(String projectOptionId, Map<String, List<String>> projectResourcesMap) {
      List<Long> result = new ArrayList<Long>();
      if (projectOptionId.equals(ALL)) {
         for (String projectChoice : projectResourcesMap.keySet()) {
            String projectLocator = XValidator.choiceID(projectChoice);
            result.add(OpLocator.parseLocator(projectLocator).getID());
         }
      }
      else {
         result.add(OpLocator.parseLocator(projectOptionId).getID());
      }
      return result;
   }

   /**
    * Activates an entry from the project filter, according to the request parameter.
    *
    * @param projectChoiceId a <code>String</code>  representing the request parameter.
    * @param session         a <code>OpProjectSession</code> representing the server session.
    * @param form            a <code>XComponent(FORM)</code> representing the my_tasks form.
    * @return a <code>String</code> representing the option value from the projects filter.
    */
   private String activateProjectFilterOptionId(String projectChoiceId, OpProjectSession session, XComponent form) {
      if (projectChoiceId == null) {
         //set the default selected index for the project chooser becouse it is populate within this form provider
         Map<String, Integer> stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null) {
            Integer selectedIndex = stateMap.get(PROJECT_CHOICE_FIELD);
            if (selectedIndex != null) {
               XComponent projectDataSet = form.findComponent(PROJECT_SET);
               if (selectedIndex < projectDataSet.getChildCount()) {
                  XComponent dataRow = (XComponent) projectDataSet.getChild(selectedIndex.intValue());
                  projectChoiceId = XValidator.choiceID(dataRow.getStringValue());
               }
            }
            else {
               Integer defaultSelectedIndex = 0;
               stateMap.put(PROJECT_CHOICE_FIELD, defaultSelectedIndex);
            }
         }
      }
      return (projectChoiceId != null) ? projectChoiceId : ALL;
   }

   /**
    * Creates a <code>Map<Integer, String></code> where the keys are the indexes of the data rows in the
    * <code>XComponent</code> data set passed as parameter and the values are the <code>String</code>
    * values from those data rows.
    * Map structure: Key -  the index of each data row in the data set passed as parameter.
    * Value - the String value on that row.
    *
    * @param dataSet - the <code>XComponent</code> data set on which the <code>Map</code> is created.
    * @return a <code>Map<Integer, String></code> where the keys are the indexes of the data rows in the
    *         <code>XComponent</code> data set passed as parameter and the values are the <code>String</code>
    *         values from those data rows.
    */
   private Map<Integer, String> createIndexIdMap(XComponent dataSet) {
      Map<Integer, String> indexIdMap = new HashMap<Integer, String>();
      XComponent dataRow;

      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         indexIdMap.put(new Integer(dataRow.getIndex()), (String) dataRow.getValue());
      }
      return indexIdMap;
   }

   /**
    * Creates a <code>Map<String, Integer></code> where the keys are the <code>String</code> values of
    * the data rows in the <code>XComponent</code> data set passed as parameter and the values are the
    * indexes of those data rows.
    * Map structure: Key -  the String value of each data row in the data set passed as parameter.
    * Value - the index of that row.
    *
    * @param dataSet - the <code>XComponent</code> data set on which the <code>Map</code> is created.
    * @return a <code>Map<String, Integer></code> where the keys are the <code>String</code> values of
    *         the data rows in the <code>XComponent</code> data set passed as parameter and the values are the
    *         indexes of those data rows.
    */
   private Map<String, Integer> createIdIndexMap(XComponent dataSet) {
      Map<String, Integer> indexIdMap = new HashMap<String, Integer>();
      XComponent dataRow;

      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         indexIdMap.put((String) dataRow.getValue(), new Integer(dataRow.getIndex()));
      }
      return indexIdMap;
   }

   /**
    * Returns the number of days left in the current week (represented by the date set on the calendarGMT).
    *
    * @param serverCalendar the server calendar instance
    * @param calendarGMT the <code>Calendar</code> instance of the server calendar. This calendar should have the current
    *    date set.
    * @return the number of days left in the current week.
    */
   private int daysLeftInWeek(OpProjectCalendar serverCalendar, Calendar calendarGMT) {
      int currentDayInWeek = calendarGMT.get(Calendar.DAY_OF_WEEK);
      int firstWorkDay = serverCalendar.getFirstWorkday();

      return currentDayInWeek < firstWorkDay ? firstWorkDay - currentDayInWeek : firstWorkDay + OpProjectCalendar.DAYS_PER_WEEK - currentDayInWeek;
   }
}