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
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.my_tasks.OpMyTasksServiceImpl;
import onepoint.project.modules.project.*;
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

   private final static String NEXT_WEEK = "nw";
   private final static String NEXT_2_WEEKS = "n2w";
   private final static String NEXT_MONTH = "nm";
   private final static String NEXT_2_MONTHS = "n2m";

   private final static String NEW_COMMENT = "NewCommentButton";
   private final static String INFO_BUTTON = "InfoButton";
   private final static String PRINT_BUTTON = "PrintButton";
   private final static String NEW_ADHOC = "NewAdhocButton";


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      Map<String, List<String>> adhocProjectsMap = OpProjectDataSetFactory.getProjectToResourceMap(session);

      OpUser user = (OpUser) (broker.getObject(OpUser.class, session.getUserID()));
      if (user.getResources().size() == 0 && adhocProjectsMap.isEmpty()) {
         form.findComponent(NEW_COMMENT).setEnabled(false);
         form.findComponent(INFO_BUTTON).setEnabled(false);
         form.findComponent(PRINT_BUTTON).setEnabled(false);
         form.findComponent(NEW_ADHOC).setEnabled(false);
         form.findComponent(PROJECT_CHOICE_FIELD).setEnabled(false);
         form.findComponent(START_TIME_CHOICE_FIELD).setEnabled(false);
         form.findComponent(RESOURCE_CHOICE_FIELD).setEnabled(false);
         return; // TODO: UI-level error -- no resource associated with this user
      }

      form.findComponent(PRINT_TITLE).setStringValue(user.getName());

      String showHoursPref = user.getPreferenceValue(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showHoursPref == null) {
         showHoursPref = OpSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS);
      }
      Boolean showHours = Boolean.valueOf(showHoursPref);

      // Configure activity activityFilter
      XComponent dataSet = form.findComponent(ACTIVITY_SET);
      List<String> projectChoices = new ArrayList<String>();
      XComponent projectDataSet = form.findComponent(PROJECT_SET);

      fillResourceFilter(form, adhocProjectsMap);

      String filteredResourcesId = getFilteredResourcesId(session, parameters, form);
      boolean all = ALL.equals(filteredResourcesId);
      boolean managed = MANAGED.equals(filteredResourcesId);
      boolean responsible = RESPONSIBLE.equals(filteredResourcesId);
      boolean individual = !(responsible || managed || all);

      if (user.getResources().size() != 0) {

         List<Long> resourceIds = new ArrayList<Long>();
         if (!individual) {
            OpQuery query = broker.newQuery("select resource.ID from OpResource as resource where resource.User.ID = ?");
            query.setLong(0, session.getUserID());
            resourceIds = broker.list(query);
         }
         else {
            long resId = OpLocator.parseLocator(filteredResourcesId).getID();
            resourceIds.add(resId);
         }

         List<Byte> types = new ArrayList<Byte>();
         types.add(OpActivity.STANDARD);
         types.add(OpActivity.TASK);
         types.add(OpActivity.MILESTONE);

         //fill project set
         List projectNodes = getProjects(broker, resourceIds, types);
         for (Object projectNode1 : projectNodes) {
            OpProjectNode projectNode = (OpProjectNode) projectNode1;
            XComponent row = new XComponent(XComponent.DATA_ROW);
            String choice = XValidator.choice(projectNode.locator(), projectNode.getName());
            row.setStringValue(choice);
            projectChoices.add(choice);
            projectDataSet.addDataRow(row);
         }
         fillProjectAdhocFilter(adhocProjectsMap, projectChoices, projectDataSet);

         OpActivityFilter filter = createActivityFilter(session, parameters, form);
         for (Long resourceId : resourceIds) {
            filter.addResourceID(resourceId);
         }
         filter.setDependencies(true);
         //activities which are not completed yet
         filter.setCompleted(Boolean.FALSE);
         filter.setAssignmentCompleted(Boolean.FALSE);
         //activity types activityFilter
         for (Byte type : types) {
            filter.addType(type);
         }

         setFilterStart(session, parameters, form, filter);
         // Configure activity sort order
         Map<String, String> sortOrders = new HashMap<String, String>(2);
         sortOrders.put(OpActivity.START, OpObjectOrderCriteria.ASCENDING);
         sortOrders.put(OpActivity.PRIORITY, OpObjectOrderCriteria.ASCENDING);
         OpObjectOrderCriteria orderCriteria = new OpObjectOrderCriteria(OpActivity.ACTIVITY, sortOrders);

         // Retrieve filtered and ordered activity data-set
         dataSet.setValue(showHours);
         OpActivityDataSetFactory.retrieveFilteredActivityDataSet(broker, filter, orderCriteria, dataSet);
      }
      else {
         fillProjectAdhocFilter(adhocProjectsMap, projectChoices, projectDataSet);
      }

      OpActivityFilter activityFilter = createActivityFilter(session, parameters, form);
      if (all) {
         addAllResources(session, broker, activityFilter);
      }
      else if (managed) {
         addManagedResources(session, broker, activityFilter);
      }
      else if (responsible) {
         addResponsibleResources(session, broker, activityFilter);
      }
      else if (individual) {
         OpLocator locator = OpLocator.parseLocator(filteredResourcesId);
         activityFilter.addResourceID(locator.getID());
      }

      activityFilter.getTypes().clear();
      activityFilter.addType(OpActivity.ADHOC_TASK);
      // Configure activity sort order
      Map<String, String> sortOrders = new HashMap<String, String>(2);
      sortOrders.put(OpActivity.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
      sortOrders.put(OpActivity.PRIORITY, OpObjectOrderCriteria.ASCENDING);
      OpObjectOrderCriteria orderCriteria = new OpObjectOrderCriteria(OpActivity.ACTIVITY, sortOrders);

      // Retrieve filtered and ordered adhoc tasks
      XComponent adHocSet = new XComponent(XComponent.DATA_SET);
      // only if the filter has any resources
      if (!activityFilter.getResourceIds().isEmpty()) {
         OpActivityDataSetFactory.retrieveFilteredActivityDataSet(broker, activityFilter, orderCriteria, adHocSet);
      }
      for (int i = 0; i < adHocSet.getChildCount(); i++) {
         XComponent row = (XComponent) adHocSet.getChild(i);
         XComponent effortCell = (XComponent) row.getChild(7); // base effort index
         effortCell.setValue(null);
         effortCell.setEnabled(false);
         dataSet.addChild(row);
      }
      disableButtons(dataSet, form, adhocProjectsMap);

      // fill category color data set
      XComponent categoryColorDataSet = form.findComponent(CATEGORY_COLOR_DATA_SET);
      OpActivityDataSetFactory.fillCategoryColorDataSet(broker, categoryColorDataSet);

      // fill delete permission data set
      XComponent deletePermissionSet = form.findComponent(DELETE_PERMISSION_SET);
      fillDeletePermissionDataSet((OpProjectSession) s, broker, dataSet, deletePermissionSet);

      broker.close();
   }

   /**
    * Add all the resources accesible for the current user
    *
    * @param session        the project session
    * @param broker         the broker used to access data
    * @param activityFilter the activity filter where the filtered resources are added.
    */
   private void addAllResources(OpProjectSession session, OpBroker broker, OpActivityFilter activityFilter) {
      List<Byte> types = new ArrayList<Byte>();
      types.add(OpPermission.ADMINISTRATOR);
      types.add(OpPermission.MANAGER);
      Set<String> resIds = OpProjectDataSetFactory.getProjectResources(session, broker, types, false);
      for (String resId : resIds) {
         OpLocator locator = OpLocator.parseLocator(resId);
         activityFilter.addResourceID(locator.getID());
      }
      addManagedResources(session, broker, activityFilter);
   }

   /**
    * Add managed and responsible resources for the current user
    *
    * @param session        the project session
    * @param broker         the broker used to access data
    * @param activityFilter the activity filter where the filtered resources are added.
    */
   private void addManagedResources(OpProjectSession session, OpBroker broker, OpActivityFilter activityFilter) {
      List<Byte> types = new ArrayList<Byte>();
      types.add(OpPermission.ADMINISTRATOR);
      types.add(OpPermission.MANAGER);
      types.add(OpPermission.CONTRIBUTOR);
      types.add(OpPermission.OBSERVER);
      Set<String> resIds = OpProjectDataSetFactory.getProjectResources(session, broker, types, false);
      for (String resId : resIds) {
         OpLocator locator = OpLocator.parseLocator(resId);
         long id = locator.getID();
         if (session.effectiveAccessLevel(broker, id) >= OpPermission.MANAGER) {
            activityFilter.addResourceID(id);
         }
      }
      addResponsibleResources(session, broker, activityFilter);
   }

   /**
    * Add responsible resources for the current user
    *
    * @param session        the project session
    * @param broker         the broker used to access data
    * @param activityFilter the activity filter where the filtered resources are added.
    */
   private void addResponsibleResources(OpProjectSession session, OpBroker broker, OpActivityFilter activityFilter) {
      List<Byte> types = new ArrayList<Byte>();
      types.add(OpPermission.ADMINISTRATOR);
      types.add(OpPermission.MANAGER);
      types.add(OpPermission.CONTRIBUTOR);
      types.add(OpPermission.OBSERVER);
      Set<String> resIds = OpProjectDataSetFactory.getProjectResources(session, broker, types, true);
      for (String resId : resIds) {
         OpLocator locator = OpLocator.parseLocator(resId);
         activityFilter.addResourceID(locator.getID());
      }
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
    * @param adhocProjectsMap project->list of resources map
    * @param projectChoices   List with project filter choices already added to the data set.
    * @param projectDataSet   Project filter data set.
    */
   private void fillProjectAdhocFilter(Map adhocProjectsMap, List projectChoices, XComponent projectDataSet) {
      //add projects that are only for adhoc tasks
      for (Object o : adhocProjectsMap.keySet()) {
         String choice = (String) o;
         if (!projectChoices.contains(choice)) {
            XComponent row = new XComponent(XComponent.DATA_ROW);
            row.setStringValue(choice);
            projectDataSet.addDataRow(row);
         }
      }
   }

   /**
    * Disables the tool buttons if no actions are possible.
    *
    * @param dataSet          tasks data set
    * @param form             current form
    * @param adhocProjectsMap project->(list of resources) map.
    */
   private void disableButtons(XComponent dataSet, XComponent form, Map adhocProjectsMap) {
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
    * Sets the start form the period filter
    *
    * @param session    the project session
    * @param parameters parameters
    * @param form       the form
    * @param filter     the activity filter
    */
   private void setFilterStart(OpProjectSession session, HashMap parameters, XComponent form, OpActivityFilter filter) {
      //get start from choice field or session state
      String filteredStartFromId = getFilteredStartBeforeId(session, parameters, form);

      boolean isFilterNextWeek = filteredStartFromId != null && filteredStartFromId.equals(NEXT_WEEK);
      boolean isFilterNext2Weeks = filteredStartFromId != null && filteredStartFromId.equals(NEXT_2_WEEKS);
      boolean isFilterNextMonth = filteredStartFromId != null && filteredStartFromId.equals(NEXT_MONTH);
      boolean isFilterNext2Months = filteredStartFromId != null && filteredStartFromId.equals(NEXT_2_MONTHS);

      //<FIXME author="Mihai Costin" description="XCalendar should be used here instead of Calendar. Hour/min should be set to 0"
      if (isFilterNextWeek) {
         Date start = new Date(System.currentTimeMillis() + XCalendar.MILLIS_PER_WEEK * 1);
         filter.setStartTo(start);
      }
      else if (isFilterNext2Weeks) {
         Date start = new Date(System.currentTimeMillis() + XCalendar.MILLIS_PER_WEEK * 2);
         filter.setStartTo(start);
      }
      else if (isFilterNextMonth) {
         Calendar now = Calendar.getInstance();
         //skip to next month
         now.set(Calendar.MONTH, now.get(Calendar.MONTH) + 1);
         Date start = new Date(now.getTime().getTime());
         filter.setStartTo(start);
      }
      else if (isFilterNext2Months) {
         Calendar now = Calendar.getInstance();
         //skip to next 2 months
         now.set(Calendar.MONTH, now.get(Calendar.MONTH) + 2);
         Date start = new Date(now.getTime().getTime());
         filter.setStartTo(start);
      }
      //</FIXME>
   }

   /**
    * Fills up the resource filter data set.
    *
    * @param form             Current form (the filter data set is in)
    * @param adhocProjectsMap Map of (project) -> (list of resources)
    */
   private void fillResourceFilter(XComponent form, Map adhocProjectsMap) {

      XComponent resourceDataSet = form.findComponent(RESOURCES_SET);
      List<String> allResources = new ArrayList<String>();
      Map<String, String> captionToResourceMap = new HashMap<String, String>();
      for (Object o : adhocProjectsMap.values()) {
         List resourcesList = (List) o;
         for (Object aResourcesList : resourcesList) {
            String choice = (String) aResourcesList;
            String caption = XValidator.choiceCaption(choice);
            if (!allResources.contains(caption)) {
               allResources.add(caption);
               captionToResourceMap.put(caption, choice);
            }
         }
      }
      //sort the resources
      String[] resourceArray = allResources.toArray(new String[0]);
      Arrays.sort(resourceArray);
      allResources = Arrays.asList(resourceArray);

      for (Object allResource : allResources) {
         String caption = (String) allResource;
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
    * @param parameters Form parameters.
    * @param form       Current form
    * @return An activity filter.
    */
   private OpActivityFilter createActivityFilter(OpProjectSession session, HashMap parameters, XComponent form) {
      OpActivityFilter filter = new OpActivityFilter();
      //get project from choice field or reset session state
      String filteredProjectChoiceId = getFilteredProjectChoiceId(session, parameters, form);
      boolean isAll = filteredProjectChoiceId == null || filteredProjectChoiceId.equals(ALL);
      if (!isAll) {
         filter.addProjectNodeID(OpLocator.parseLocator(filteredProjectChoiceId).getID());
      }
      return filter;
   }

   /**
    * Fills the project data set with the necessary data
    *
    * @param resources     <code>List</code> of resources for which the session user is responsible
    * @param activityTypes <code>ArrayList</code> of activity types
    * @param broker        Broker object to use for db access.
    * @return List of projects nodes
    */
   public static List getProjects(OpBroker broker, List resources, List activityTypes) {
      StringBuffer queryBuffer = new StringBuffer("select distinct assignment.ProjectPlan.ProjectNode from OpAssignment assignment ");
      queryBuffer.append(" where assignment.Resource.ID in (:resourceIds) and assignment.Activity.Type in (:types) and assignment.Activity.Complete < 100");
      queryBuffer.append(" order by assignment.ProjectPlan.ProjectNode.Name");

      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setCollection("resourceIds", resources);
      query.setCollection("types", activityTypes);

      return broker.list(query);

   }

   /**
    * Returns the value of the <code>START_FROM_ID</code>(choice field selection) from the <code>parameters</code> list.
    * If it's not found there, the search is performed in <code>form</code>'s state map kept on the <code>session<code>.
    *
    * @param session    <code>OpProjectSession</code> the session
    * @param parameters <code>Map<code> of parameters
    * @param form       <code>XComponent.FORM</code> for which this class is provider
    * @return <code>String</code> representing the value of the choice field selection.
    */
   private String getFilteredStartBeforeId(OpProjectSession session, Map parameters, XComponent form) {
      //get start from choice field
      String filteredStartFromId = (String) parameters.get(START_BEFORE_ID);
      if (filteredStartFromId == null) { //get start id from form's state
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null) {
            Integer selectedIndex = (Integer) stateMap.get(START_TIME_CHOICE_FIELD);
            if (selectedIndex != null) {
               XComponent startTimeDataSet = form.findComponent(START_BEFORE_SET);
               if (selectedIndex < startTimeDataSet.getChildCount()) {
                  XComponent dataRow = (XComponent) startTimeDataSet.getChild(selectedIndex.intValue());
                  filteredStartFromId = XValidator.choiceID(dataRow.getStringValue());
               }
            }
         }
      }
      if (filteredStartFromId == null) {
         filteredStartFromId = NEXT_MONTH; //default value for period filter
      }
      return filteredStartFromId;
   }

   /**
    * Returns the value of the resources choice field selection from the <code>parameters</code> list.
    * If it's not found there, the search is performed in <code>form</code>'s state map kept on the <code>session<code>.
    *
    * @param session    <code>OpProjectSession</code> the session
    * @param parameters <code>Map<code> of parameters
    * @param form       <code>XComponent.FORM</code> for which this class is provider
    * @return <code>String</code> representing the value of the choice field selection.
    */
   private String getFilteredResourcesId(OpProjectSession session, Map parameters, XComponent form) {
      String filteredResourceChoiceId = (String) parameters.get(RESOURCE_CHOICE_ID);
      if (filteredResourceChoiceId == null) {
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null) {
            Integer selectedIndex = (Integer) stateMap.get(RESOURCE_CHOICE_FIELD);
            if (selectedIndex != null) {
               XComponent resourceDataSet = form.findComponent(RESOURCES_SET);
               if (selectedIndex < resourceDataSet.getChildCount()) {
                  XComponent dataRow = (XComponent) resourceDataSet.getChild(selectedIndex.intValue());
                  filteredResourceChoiceId = XValidator.choiceID(dataRow.getStringValue());
               }
            }
         }
      }
      if (filteredResourceChoiceId == null) {
         filteredResourceChoiceId = ALL;
      }
      return filteredResourceChoiceId;
   }


   /**
    * Returns the value of the <code>PROJECT_CHOICE_ID</code>(choice field selection) from the <code>parameters</code> list.
    * If it's not found there,default selected index is set in <code>form</code>'s state map.
    *
    * @param session    <code>OpProjectSession</code> the session
    * @param parameters <code>Map<code> of parameters
    * @param form       <code>XComponent.FORM</code> for which this class is provider
    * @return <code>String</code> representing the value of the choice field selection.
    */
   private String getFilteredProjectChoiceId(OpProjectSession session, Map parameters, XComponent form) {
      //get project id from choice field
      String filteredProjectChoiceId = (String) parameters.get(PROJECT_CHOICE_ID);
      if (filteredProjectChoiceId == null) {
         //set the default selected index for the project chooser becouse it is populate within this form provider
         Map<String, Integer> stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null) {
            Integer selectedIndex = stateMap.get(PROJECT_CHOICE_FIELD);
            if (selectedIndex != null) {
               XComponent projectDataSet = form.findComponent(PROJECT_SET);
               if (selectedIndex < projectDataSet.getChildCount()) {
                  XComponent dataRow = (XComponent) projectDataSet.getChild(selectedIndex.intValue());
                  filteredProjectChoiceId = XValidator.choiceID(dataRow.getStringValue());
               }
            }
            else {
               Integer defaultSelectedIndex = 0;
               stateMap.put(PROJECT_CHOICE_FIELD, defaultSelectedIndex);
            }
         }
      }
      if (filteredProjectChoiceId == null) {
         filteredProjectChoiceId = ALL;
      }
      return filteredProjectChoiceId;
   }

}
