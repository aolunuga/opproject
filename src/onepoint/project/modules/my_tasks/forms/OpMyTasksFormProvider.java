/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.my_tasks.OpMyTasksServiceImpl;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpUser;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

public class OpMyTasksFormProvider implements XFormProvider {

   private final static String ACTIVITY_SET = "ActivitySet";
   private final static String CATEGORY_COLOR_DATA_SET = "CategoryColorDataSet";
   private final static String PRINT_TITLE = "PrintTitle";
   private final static String PROJECT_CHOICE_FIELD = "ProjectChooser";
   private final static String START_TIME_CHOICE_FIELD = "StartTimeChooser";
   private final static String RESOURCE_CHOICE_FIELD = "ResourcesChooser";
   private final static String START_BEFORE_SET = "StartBeforeSet";
   private final static String PROJECT_SET = "ProjectSet";
   private final static String RESOURCES_SET = "FilterResourcesSet";
   private final static String DELETE_PERMISSION_SET = "DeletePermissionSet";

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
   private final static String NEXT_2_WEEKS = "n2w";
   private final static String NEXT_MONTH = "nm";
   private final static String NEXT_2_MONTHS = "n2m";

   private final static String NEW_COMMENT = "NewCommentButton";
   private final static String INFO_BUTTON = "InfoButton";
   private final static String PRINT_BUTTON = "PrintButton";
   private final static String NEW_ADHOC = "NewAdhocButton";

   private final static String NO_RESOURCES_FOR_USER_ID = "NoResourcesForUser";
   private final static String ERROR_LABEL_ID = "ErrorLabel";

   /**
    * A list of activity types to filter after
    */
   private final static List<Byte> TYPES_FILTER_LIST = Arrays.asList(OpActivity.STANDARD, OpActivity.MILESTONE,  OpActivity.TASK,  OpActivity.ADHOC_TASK);

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)  
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      Map<String, List<String>> projectsResourcesMap = this.getProjectToResourcesViewMap(session);

      //check the case when the current user doesn't  see any resources
      if (projectsResourcesMap.isEmpty()) {
         handleNoResources(form);
         broker.close();
         return;
      }

      OpUser user = (OpUser) (broker.getObject(OpUser.class, session.getUserID()));
      form.findComponent(PRINT_TITLE).setStringValue(user.getName());

      //fill this form's filters
      this.fillResourceFilter(form, projectsResourcesMap);
      this.fillProjectFilter(form, projectsResourcesMap);

      XComponent dataSet = form.findComponent(ACTIVITY_SET);

      //create the activity filter
      OpActivityFilter activityFilter = createActivityFilter(session, broker, parameters, form, user.getResources(), projectsResourcesMap);
      //retrieve the activities
      this.fillActivityDataSet(activityFilter, user, broker, dataSet);

      // check buttons that need enabling or disabling
      this.checkButtons(dataSet, form, projectsResourcesMap);

      // fill category color data set
      XComponent categoryColorDataSet = form.findComponent(CATEGORY_COLOR_DATA_SET);
      OpActivityDataSetFactory.fillCategoryColorDataSet(broker, categoryColorDataSet);

      // fill delete permission data set
      XComponent deletePermissionSet = form.findComponent(DELETE_PERMISSION_SET);
      fillDeletePermissionDataSet((OpProjectSession) s, broker, dataSet, deletePermissionSet);

      broker.close();
   }

   /**
    * Fills the activity data-set with data.
    * @param activityFilter a <code>OpActivityFilter</code> used to retrieve activities.
    * @param user a <code>OpUser</code> representing the current user.
    * @param broker a <code>OpBroker</code> used for db operations.
    * @param dataSet  a <code>XComponent(DATA_SET)</code> representing the activity data-set.
    */
   private void fillActivityDataSet(OpActivityFilter activityFilter, OpUser user, OpBroker broker, XComponent dataSet) {
      //only makes sense to retrieve any activities if we have projects and resources set
      if (activityFilter.getProjectNodeIds().isEmpty() || activityFilter.getResourceIds().isEmpty()) {
         return;
      }

      // Configure activity sort order
      Map<String, String> sortOrders = new HashMap<String, String>(2);
      sortOrders.put(OpActivity.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
      sortOrders.put(OpActivity.PRIORITY, OpObjectOrderCriteria.ASCENDING);
      OpObjectOrderCriteria orderCriteria = new OpObjectOrderCriteria(OpActivity.ACTIVITY, sortOrders);

      // Retrieve filtered and ordered activity data-set
      XComponent unsortedDataSet = new XComponent(XComponent.DATA_SET);
      Boolean showHours = getShowHoursPreference(user);
      unsortedDataSet.setValue(showHours);
      OpActivityDataSetFactory.retrieveFilteredActivityDataSet(broker, activityFilter, orderCriteria, unsortedDataSet);
      this.sortActivityDataSet(unsortedDataSet, dataSet);
   }

   /**
    * Returns a map of projects and list of resources for each project, where the current user is
    * at least observer on the project.
    *
    * @param session Current project session (used for db access and current user)
    * @return Map of key: project_locator/project_name choice -> value: List of resource_locator/resource_name choices
    */
   private  Map<String, List<String>> getProjectToResourcesViewMap(OpProjectSession session) {
      Map<String, List<String>> projectsMap = new HashMap<String, List<String>>();
      OpBroker broker = session.newBroker();
      long userId = session.getUserID();

      // add all the resources for which is responsible from project where the user has contributer access
      List<Byte> levels = new ArrayList<Byte>();

      //add only the responsible resources for the projects where the user is either OBSERVER or CONTRIBUTOR
      levels.add(OpPermission.OBSERVER);
      levels.add(OpPermission.CONTRIBUTOR);
      List<Long> projectIds = OpProjectDataSetFactory.getProjectsByPermissions(session, broker, levels);
      for (Long id : projectIds) {
         OpProjectNode project = (OpProjectNode) broker.getObject(OpProjectNode.class, id);
         List<String> resources = OpProjectDataSetFactory.getProjectResources(project, userId, true);
         if (!resources.isEmpty()) {
            projectsMap.put(XValidator.choice(project.locator(), project.getName()), resources);
         }
      }

      //add all the resources for the projects where the user is either ADMINISTRATOR or MANAGER
      levels.clear();
      levels.add(OpPermission.ADMINISTRATOR);
      levels.add(OpPermission.MANAGER);
      projectIds = OpProjectDataSetFactory.getProjectsByPermissions(session, broker, levels);
      for (Long id : projectIds) {
         OpProjectNode project = (OpProjectNode) broker.getObject(OpProjectNode.class, id);
         //the list of project resources includes resources for which the user is responsible
         List<String> resources = OpProjectDataSetFactory.getProjectResources(project, userId, false);
         if (!resources.isEmpty()) {
            projectsMap.put(XValidator.choice(project.locator(), project.getName()), resources);
         }
      }
      broker.close();
      return projectsMap;
   }

   /**
    * Sorts an actviity data-set after the start date of non-ad-hoc task activities, and copies the result into another dataset.
    * @param unsortedDataSet a <code>XComponent(DATA_SET)</code> representing an unsorted data-set.
    * @param sortedDataSet a <code>XComponent(DATA_SET)</code> representing a data-set where to place the sorted rows.
    */
   private void sortActivityDataSet(XComponent unsortedDataSet, XComponent sortedDataSet) {
      List<XComponent> adHocTasks= new ArrayList<XComponent>();
      for (int i = 0; i < unsortedDataSet.getChildCount(); i++) {
         XComponent activityRow = (XComponent) unsortedDataSet.getChild(i);
         if (OpGanttValidator.getType(activityRow) == OpGanttValidator.ADHOC_TASK) {
            adHocTasks.add(activityRow);
         }
         else {
            sortedDataSet.addChild(activityRow);
         }
      }
      unsortedDataSet.removeAllChildren();
      sortedDataSet.sort(OpGanttValidator.START_COLUMN_INDEX);
      sortedDataSet.addAllChildren(adHocTasks.toArray(new XComponent[]{}));
   }

   /**
    * Gets the user preferences to show the assignment values in % or in hours.
    * @param user a <code>OpUser</code> representing the current user.
    * @return a <code>Boolean</code> indicating whether to show assignments in hours or not.
    */
   private Boolean getShowHoursPreference(OpUser user) {
      String showHoursPref = user.getPreferenceValue(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showHoursPref == null) {
         showHoursPref = OpSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS);
      }
      return  Boolean.valueOf(showHoursPref);
   }

   /**
    * Handles the case when there are no resources for the current user.
    * @param form  a <code>XComponent(FORM)</code> representing the my tasks form.
    */
   private void handleNoResources(XComponent form) {
      form.findComponent(NEW_COMMENT).setEnabled(false);
      form.findComponent(INFO_BUTTON).setEnabled(false);
      form.findComponent(PRINT_BUTTON).setEnabled(false);
      form.findComponent(NEW_ADHOC).setEnabled(false);
      form.findComponent(PROJECT_CHOICE_FIELD).setEnabled(false);
      form.findComponent(START_TIME_CHOICE_FIELD).setEnabled(false);
      form.findComponent(RESOURCE_CHOICE_FIELD).setEnabled(false);

      //show the error
      XComponent errorLabel = form.findComponent(ERROR_LABEL_ID);
      errorLabel.setText(form.findComponent(NO_RESOURCES_FOR_USER_ID).getText());
      errorLabel.setVisible(true);
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
         OpActivity activity = (OpActivity) broker.getObject(locator);
         boolean delete = OpMyTasksServiceImpl.deleteGranted(s, activity);
         XComponent delRow = new XComponent(XComponent.DATA_ROW);
         delRow.setBooleanValue(delete);
         deletePermissionSet.addChild(delRow);
      }
   }

   /**
    * Fills up the project filter data set.
    *
    * @param projectsResourcesMap a <code>Map</code> containing all the project where the current user is at least observer and the list of resources on these projects.
    * @param form a <code>XComponent(FORM)</code> representing the current form.
    */
   private void fillProjectFilter(XComponent form, Map<String, List<String>> projectsResourcesMap) {
      XComponent projectDataSet = form.findComponent(PROJECT_SET);

      //add projects that are only for adhoc tasks
      for (String projectChoice : projectsResourcesMap.keySet()) {
         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(projectChoice);
         projectDataSet.addDataRow(row);
      }
   }

   /**
    * Disables the tool buttons if no actions are possible.
    *
    * @param dataSet          tasks data set
    * @param form             current form
    * @param adhocProjectsMap project->(list of resources) map.
    */
   private void checkButtons(XComponent dataSet, XComponent form, Map adhocProjectsMap) {
      //if dataset is empty, disable all the buttons
      if (dataSet.getChildCount() == 0) {
         form.findComponent(NEW_COMMENT).setEnabled(false);
         form.findComponent(INFO_BUTTON).setEnabled(false);
         form.findComponent(PRINT_BUTTON).setEnabled(false);
      }
      if (adhocProjectsMap.isEmpty()) {
         form.findComponent(NEW_ADHOC).setEnabled(false);
      }
   }

   /**
    * Gets the start form the period filter
    *
    * @param session    the project session
    * @param filteredStartFromRequestParam a <code>String<code> representing the value of the "start from" request parameter
    * @param form       the form
    *
    * @return a <code>Date</code> representing the start date for the filter
    */
   private Date getFilterStart(OpProjectSession session, String filteredStartFromRequestParam , XComponent form) {
      //get start from choice field or session state
      String filteredStartFromId = getFilteredStartBeforeId(session, filteredStartFromRequestParam, form);

      //retrieve the server calendar and initialize it
      XCalendar serverCalendar = session.getCalendar();
      Calendar calendarGMT = serverCalendar.getCalendar();
      calendarGMT.setTimeInMillis(System.currentTimeMillis());
      calendarGMT.set(Calendar.HOUR, 0);
      calendarGMT.set(Calendar.MINUTE, 0);
      calendarGMT.set(Calendar.SECOND, 0);

      if (filteredStartFromId.equals(NEXT_WEEK)) {
        calendarGMT.setTimeInMillis(calendarGMT.getTimeInMillis() + XCalendar.MILLIS_PER_WEEK);
      }
      else if (filteredStartFromId.equals(NEXT_2_WEEKS)) {
         calendarGMT.setTimeInMillis(calendarGMT.getTimeInMillis() + XCalendar.MILLIS_PER_WEEK * 2);
      }
      else if (filteredStartFromId.equals(NEXT_MONTH)) {
         calendarGMT.set(Calendar.MONTH, calendarGMT.get(Calendar.MONTH) + 1);
      }
      else if  (filteredStartFromId.equals(NEXT_2_MONTHS)) {
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
    * @param form             Current form (the filter data set is in)
    * @param projectsResourcesMap Map of (project) -> (list of resources)
    */
   private void fillResourceFilter(XComponent form, Map<String, List<String>> projectsResourcesMap) {
      XComponent resourceDataSet = form.findComponent(RESOURCES_SET);
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
         resourceDataSet.addChild(row);
      }
   }

   /**
    * Creates an activity filter and sets the projects on it using the filter project choice.
    *
    * @param session    Current session. used to obtain the project filter
    * @param broker a <code>OpBroker</code> used for db operations.
    * @param parameters Form parameters.
    * @param form       Current form
    * @param responsibleResources a <code>Set(OpResource)</code> representing the responsible resources for the current user.
    * @param projectResourcesMap a <code>Map</code> of project ids and a list of resources for each project.
    * @return an <code>OpActivityFilter</code> instance.
    */
   private OpActivityFilter createActivityFilter(OpProjectSession session, OpBroker broker, HashMap parameters,
        XComponent form,  Set<OpResource> responsibleResources, Map<String, List<String>> projectResourcesMap) {
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
      return filter;
   }

   /**
    * Returns the value of the <code>START_FROM_ID</code>(choice field selection) from the <code>parameters</code> list.
    * If it's not found there, the search is performed in <code>form</code>'s state map kept on the <code>session<code>.
    *
    * @param session    <code>OpProjectSession</code> the session
    * @param filteredStartFromRequestParam a <code>String<code> representing the value of the "start from" request parameter
    * @param form       <code>XComponent.FORM</code> for which this class is provider
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
    * @param session    <code>OpProjectSession</code> the session
    * @param resourceChoiceId <code>String<code> representing the user selected option from the resources filter
    * @param form       <code>XComponent.FORM</code> for which this class is provider
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
      return (resourceChoiceId != null) ? resourceChoiceId : ALL;
   }

   /**
    * Returns a list of resource ids, representing those ids which should be used when filtering the activities.
    * @param session  a <code>OpProjectSession</code> instance representing the server session.
    * @param broker  a <code>OpBroker</code> used for db operations.
    * @param resourceOptionId  a <code>String</code> representing the option of the resource filter (from the UI).
    * @param responsibleResources a <code>Set(OpResource)</code> representing the responsible resources for the current user.
    * @param projectResourcesMap a <code>Map</code> of project ids and a list of resources for each project.
    * @return  a <code>List(Long)</code> representing a list of resource ids.
    */
   private List<Long> getFilteredResourceIds(OpProjectSession session, OpBroker broker,
        String resourceOptionId, Set<OpResource> responsibleResources, Map<String, List<String>> projectResourcesMap) {
      List<Long> result = new ArrayList<Long>();

      //only if the option id is a <real> resource locator
      if (! RESOURCE_FILTER_OPTIONS.contains(resourceOptionId)) {
         long resourceId = OpLocator.parseLocator(resourceOptionId).getID();
         result.add(resourceId);
         return result;
      }

      for (List<String> resourcesList : projectResourcesMap.values()) {
         for (String resourceChoice : resourcesList) {
            String resourceLocator = XValidator.choiceID(resourceChoice);
            OpResource resource = (OpResource) broker.getObject(resourceLocator);
            boolean acceptResource = true;

            if (resourceOptionId.equals(MANAGED) && session.effectiveAccessLevel(broker, resource.getID()) <  OpPermission.MANAGER) {
               acceptResource = false;
             }
             else if (resourceOptionId.equals(RESPONSIBLE) && !responsibleResources.contains(resource)) {
               acceptResource = false;
             }

            if (acceptResource) {
                result.add(resource.getID());
            }
         }
      }
      return result;
   }

   /**
    * Returns the value of the <code>PROJECT_CHOICE_ID</code>(choice field selection) from the <code>parameters</code> list.
    * If it's not found there,default selected index is set in <code>form</code>'s state map.
    *
    * @param projectOptionId <code>String<code> representing the locator of a project (may be null)
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
    * @param projectChoiceId a <code>String</code>  representing the request parameter.
    * @param session a <code>OpProjectSession</code> representing the server session.
    * @param form a <code>XComponent(FORM)</code> representing the my_tasks form.
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
      return (projectChoiceId != null) ? projectChoiceId : ALL ;
   }

}
