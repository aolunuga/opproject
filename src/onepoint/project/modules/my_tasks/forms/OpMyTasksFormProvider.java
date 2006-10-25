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
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityFilter;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpPreference;
import onepoint.project.modules.user.OpUser;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

public class OpMyTasksFormProvider implements XFormProvider {

   public final static String ACTIVITY_SET = "ActivitySet";
   public final static String CATEGORY_COLOR_DATA_SET = "CategoryColorDataSet";
   public final static String PRINT_TITLE = "PrintTitle";
   public final static String PROJECT_CHOICE_FIELD = "ProjectChooser";
   public final static String START_TIME_CHOICE_FIELD = "StartTimeChooser";
   public final static String START_BEFORE_SET = "StartBeforeSet";
   public final static String PROJECT_SET = "ProjectSet";

   /* filters*/
   public final static String START_BEFORE_ID = "start_before_id";
   public final static String PROJECT_CHOICE_ID = "project_choice_id";
   /*start from filter choices */
   private final static String ALL = "all";
   private final static String NEXT_WEEK = "nw";
   private final static String NEXT_2_WEEKS = "n2w";
   private final static String NEXT_MONTH = "nm";
   private final static String NEXT_2_MONTHS = "n2m";


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Note: Intentionally no permission check, because activities are *assigned* to resource
      // (Therefore, responsible user is always allowed to view these activities)

      OpBroker broker = session.newBroker();

      // Note: OpUser instance in session is detached, we therefore have to refetch it
      OpUser user = (OpUser) (broker.getObject(OpUser.class, session.getUserID()));
      if (user.getResources().size() == 0) {
         form.findComponent("NewCommentButton").setEnabled(false);
         form.findComponent("InfoButton").setEnabled(false);
         form.findComponent("PrintButton").setEnabled(false);
         return; // TODO: UI-level error -- no resource associated with this user
      }
      form.findComponent(PRINT_TITLE).setStringValue(user.getName());

      String showHoursPref = user.getPreferenceValue(OpPreference.SHOW_ASSIGNMENT_IN_HOURS);
      if (showHoursPref == null) {
         showHoursPref = OpSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS);
      }
      Boolean showHours = Boolean.valueOf(showHoursPref);

      OpQuery query = broker.newQuery("select resource.ID from OpResource as resource where resource.User.ID = ?");
      query.setLong(0, session.getUserID());
      List resourceIds = broker.list(query);
      // Configure activity filter
      OpActivityFilter filter = new OpActivityFilter();
      for (int i = 0; i < resourceIds.size(); i++) {
         filter.addResourceID(((Long) resourceIds.get(i)).longValue());
      }
      filter.setDependencies(true);
      /*activities which are not completed yet*/
      filter.setCompleted(Boolean.FALSE);
      /*activity types filter */
      filter.addType(OpActivity.STANDARD);
      filter.addType(OpActivity.TASK);
      filter.addType(OpActivity.MILESTONE);

      /*fill project set*/
      XComponent projectDataSet = form.findComponent(PROJECT_SET);
      fillProjectDataSet(broker, resourceIds, filter.getTypes(), projectDataSet);

      /*get project from choice field or reset session state */
      String filteredProjectChoiceId = getFilteredProjectChoiceId(session, parameters, form);

      boolean isAll = filteredProjectChoiceId == null || filteredProjectChoiceId.equals(ALL);
      if (!isAll) {
         filter.addProjectNodeID(OpLocator.parseLocator(filteredProjectChoiceId).getID());
      }

      /*get start from choice field or session state*/
      String filteredStartFromId = getFilteredStartBeforeId(session,parameters,form);

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
         /*skip to next month */
         now.set(Calendar.MONTH, now.get(Calendar.MONTH) + 1);
         Date start = new Date(now.getTime().getTime());
         filter.setStartTo(start);
      }
      else if (isFilterNext2Months) {
         Calendar now = Calendar.getInstance();
         /*skip to next 2 months */
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
      XComponent dataSet = form.findComponent(ACTIVITY_SET);
      dataSet.setValue(showHours);
      OpActivityDataSetFactory.retrieveFilteredActivityDataSet(broker, filter, orderCriteria, dataSet);

      //if dataset is empty, disable the all buttons
      if (dataSet.getChildCount() == 0) {
         form.findComponent("NewCommentButton").setEnabled(false);
         form.findComponent("InfoButton").setEnabled(false);
         form.findComponent("PrintButton").setEnabled(false);
      }

      // fill category color data set
      XComponent categoryColorDataSet = form.findComponent(CATEGORY_COLOR_DATA_SET);
      OpActivityDataSetFactory.fillCategoryColorDataSet(broker, categoryColorDataSet);

      broker.close();

   }


   /**
    * Fills the project data set with the necessary data
    *
    * @param resources <code>List</code> of resources for which the session user is responsible
    * @param activityTypes <code>ArrayList</code> of activity types
    * @param dataSet  <code>XComponent.DATA_SET</code>
    */
   public static void fillProjectDataSet(OpBroker broker, List resources, List activityTypes, XComponent dataSet) {
      StringBuffer queryBuffer = new StringBuffer("select distinct assignment.ProjectPlan.ProjectNode from OpAssignment assignment ");
      queryBuffer.append(" where assignment.Resource.ID in (:resourceIds) and assignment.Activity.Type in (:types) and assignment.Activity.Complete < 100");
      queryBuffer.append(" order by assignment.ProjectPlan.ProjectNode.Name");

      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setCollection("resourceIds",resources);
      query.setCollection("types",activityTypes);

      List projectNodes = broker.list(query);
      for (Iterator it = projectNodes.iterator();it.hasNext();){
         OpProjectNode projectNode = (OpProjectNode)it.next();
         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(XValidator.choice(projectNode.locator(), projectNode.getName()));
         dataSet.addDataRow(row);
      }
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
      /*get start from choice field */
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
    * Returns the value of the <code>PROJECT_CHOICE_ID</code>(choice field selection) from the <code>parameters</code> list.
    * If it's not found there,default selected index is set in <code>form</code>'s state map.
    *
    * @param session    <code>OpProjectSession</code> the session
    * @param parameters <code>Map<code> of parameters
    * @param form       <code>XComponent.FORM</code> for which this class is provider
    * @return <code>String</code> representing the value of the choice field selection.
    */
   private String getFilteredProjectChoiceId(OpProjectSession session, Map parameters ,XComponent form){
      /*get project id from choice field */
      String filteredProjectChoiceId = (String) parameters.get(PROJECT_CHOICE_ID);
      if (filteredProjectChoiceId == null) {
         //set the default selected index for the project chooser becouse it is populate within this form provider
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null){
            Integer defaultSelectedIndex = new Integer(0);
            stateMap.put(PROJECT_CHOICE_FIELD, defaultSelectedIndex);
            return ALL;
         }
      }
      return filteredProjectChoiceId;
   }

}
