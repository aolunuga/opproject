/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
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
import onepoint.project.modules.project.*;
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

   // filters
   private final static String START_BEFORE_ID = "start_before_id";
   private final static String PROJECT_CHOICE_ID = "project_choice_id";
   private final static String RESOURCE_CHOICE_ID = "resources_choice_id";

   // start from filter choices
   private final static String ALL = "all";
   private final static String NEXT_WEEK = "nw";
   private final static String NEXT_2_WEEKS = "n2w";
   private final static String NEXT_MONTH = "nm";
   private final static String NEXT_2_MONTHS = "n2m";

   private final static String RESPONSIBLE = "res";
   private final static String MANAGED = "man";
   private final static String INDIVIDUAL = "ind";

   private final static String NEW_COMMENT = "NewCommentButton";
   private final static String INFO_BUTTON = "InfoButton";
   private final static String PRINT_BUTTON = "PrintButton";
   private final static String NEW_ADHOC = "NewAdhocButton";


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Note: Intentionally no permission check, because activities are *assigned* to resource
      // (Therefore, responsible user is always allowed to view these activities)

      OpBroker broker = session.newBroker();

      Map adhocProjectsMap = OpProjectDataSetFactory.getProjectToResourceMap(session);

      // Note: OpUser instance in session is detached, we therefore have to refetch it
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

      // Configure activity filter
      XComponent dataSet = form.findComponent(ACTIVITY_SET);
      List projectChoices = new ArrayList();
      XComponent projectDataSet = form.findComponent(PROJECT_SET);

      if (user.getResources().size() != 0) {
         OpQuery query = broker.newQuery("select resource.ID from OpResource as resource where resource.User.ID = ?");
         query.setLong(0, session.getUserID());
         List resourceIds = broker.list(query);

         OpActivityFilter filter = createActivityFilter(session, parameters, form);
         for (int i = 0; i < resourceIds.size(); i++) {
            filter.addResourceID(((Long) resourceIds.get(i)).longValue());
         }
         filter.setDependencies(true);
         //activities which are not completed yet
         filter.setCompleted(Boolean.FALSE);
         filter.setAssignmentCompleted(Boolean.FALSE);
         //activity types filter
         filter.addType(OpActivity.STANDARD);
         filter.addType(OpActivity.TASK);
         filter.addType(OpActivity.MILESTONE);

         //fill project set
         List projectNodes = getProjects(broker, resourceIds, filter.getTypes());
         for (Iterator it = projectNodes.iterator(); it.hasNext();) {
            OpProjectNode projectNode = (OpProjectNode) it.next();
            XComponent row = new XComponent(XComponent.DATA_ROW);
            String choice = XValidator.choice(projectNode.locator(), projectNode.getName());
            row.setStringValue(choice);
            projectChoices.add(choice);
            projectDataSet.addDataRow(row);
         }

         //get start from choice field or session state
         String filteredStartFromId = getFilteredStartBeforeId(session, parameters, form);

         boolean isFilterNextWeek = filteredStartFromId != null && filteredStartFromId.equals(NEXT_WEEK);
         boolean isFilterNext2Weeks = filteredStartFromId != null && filteredStartFromId.equals(NEXT_2_WEEKS);
         boolean isFilterNextMonth = filteredStartFromId != null && filteredStartFromId.equals(NEXT_MONTH);
         boolean isFilterNext2Months = filteredStartFromId != null && filteredStartFromId.equals(NEXT_2_MONTHS);

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

         // Configure activity sort order
         Map sortOrders = new HashMap(2);
         sortOrders.put(OpActivity.START, OpObjectOrderCriteria.ASCENDING);
         sortOrders.put(OpActivity.PRIORITY, OpObjectOrderCriteria.ASCENDING);
         OpObjectOrderCriteria orderCriteria = new OpObjectOrderCriteria(OpActivity.ACTIVITY, sortOrders);

         // Retrieve filtered and ordered activity data-set
         dataSet.setValue(showHours);
         OpActivityDataSetFactory.retrieveFilteredActivityDataSet(broker, filter, orderCriteria, dataSet);
      }

      String filteredResourcesId = getFilteredResourcesId(session, parameters, form);
      boolean responsible = RESPONSIBLE.equals(filteredResourcesId);
      boolean managed = MANAGED.equals(filteredResourcesId);
      boolean individual = INDIVIDUAL.equals(filteredResourcesId);


      OpActivityFilter filter = createActivityFilter(session, parameters, form);
      for (Iterator iterator = adhocProjectsMap.values().iterator(); iterator.hasNext();) {
         List resourcesList = (List) iterator.next();
         for (Iterator resIt = resourcesList.iterator(); resIt.hasNext();) {
            String choice = (String) resIt.next();
            String locatorStr = XValidator.choiceID(choice);
            boolean isResponsible = isResponsible(session.getUserID(), broker, locatorStr);
            if (responsible && !isResponsible) {
               continue;
            }
            boolean isManaged = isManaged(session, broker, locatorStr);
            if (managed && !isManaged) {
               continue;
            }
            if (individual && (!isResponsible || !isManaged)) {
               continue;
            }
            OpLocator locator = OpLocator.parseLocator(locatorStr);
            filter.addResourceID(locator.getID());
         }
      }
      filter.getTypes().clear();
      filter.addType(OpActivity.ADHOC_TASK);
      // Configure activity sort order
      Map sortOrders = new HashMap(2);
      sortOrders.put(OpActivity.SEQUENCE, OpObjectOrderCriteria.ASCENDING);
      sortOrders.put(OpActivity.PRIORITY, OpObjectOrderCriteria.ASCENDING);
      OpObjectOrderCriteria orderCriteria = new OpObjectOrderCriteria(OpActivity.ACTIVITY, sortOrders);

      // Retrieve filtered and ordered adhoc tasks
      XComponent adHocSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveFilteredActivityDataSet(broker, filter, orderCriteria, adHocSet);
      for (int i = 0; i < adHocSet.getChildCount(); i++) {
         XComponent row = (XComponent) adHocSet.getChild(i);
         XComponent effortCell = (XComponent) row.getChild(7); // base effort index
         effortCell.setValue(null);
         effortCell.setEnabled(false);
         dataSet.addChild(row);
      }

      //if dataset is empty, disable all the buttons
      if (dataSet.getChildCount() == 0) {
         form.findComponent(NEW_COMMENT).setEnabled(false);
         form.findComponent(INFO_BUTTON).setEnabled(false);
         form.findComponent(PRINT_BUTTON).setEnabled(false);
      }

      if (adhocProjectsMap.isEmpty()) {
         form.findComponent(NEW_ADHOC).setEnabled(false);
      }
      else {
         //add projects that are only for adhoc tasks
         Iterator projectIt = adhocProjectsMap.keySet().iterator();
         while (projectIt.hasNext()) {
            String choice = (String) projectIt.next();
            if (!projectChoices.contains(choice)) {
               XComponent row = new XComponent(XComponent.DATA_ROW);
               row.setStringValue(choice);
               projectDataSet.addDataRow(row);
            }
         }
      }

      // fill category color data set
      XComponent categoryColorDataSet = form.findComponent(CATEGORY_COLOR_DATA_SET);
      OpActivityDataSetFactory.fillCategoryColorDataSet(broker, categoryColorDataSet);

      broker.close();
   }

   /**
    * @param userID
    * @param broker
    * @param resourceLocator
    * @return True if the given user (user ID) is the responsible user for this resource.
    */
   private boolean isResponsible(long userID, OpBroker broker, String resourceLocator) {
      OpResource resource = (OpResource) broker.getObject(resourceLocator);
      OpUser user = resource.getUser();
      if (user != null) {
         if (user.getID() == userID) {
            return true;
         }
      }
      return false;
   }

   private boolean isManaged(OpProjectSession session, OpBroker broker, String resourceLocator) {
      OpResource resource = (OpResource) broker.getObject(resourceLocator);
      if (session.effectiveAccessLevel(broker, resource.getID()) > OpPermission.MANAGER) {
         return true;
      }
      return false;
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
               if (selectedIndex.intValue() < startTimeDataSet.getChildCount()) {
                  XComponent dataRow = (XComponent) startTimeDataSet.getChild(selectedIndex.intValue());
                  filteredStartFromId = XValidator.choiceID(dataRow.getStringValue());
               }
            }
         }
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
               if (selectedIndex.intValue() < resourceDataSet.getChildCount()) {
                  XComponent dataRow = (XComponent) resourceDataSet.getChild(selectedIndex.intValue());
                  filteredResourceChoiceId = XValidator.choiceID(dataRow.getStringValue());
               }
            }
         }
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
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null) {
            Integer defaultSelectedIndex = new Integer(0);
            stateMap.put(PROJECT_CHOICE_FIELD, defaultSelectedIndex);
            return ALL;
         }
      }
      return filteredProjectChoiceId;
   }

}
