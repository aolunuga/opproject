/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.my_tasks.forms.OpMyTasksFormProvider;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.user.OpUser;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

public class OpNewWorkSlipFormProvider implements XFormProvider {

   private static XLog logger = XLogFactory.getLogger(OpNewWorkSlipFormProvider.class, true);

   public final static String WORK_RECORD_SET = "WorkRecordSet";
   public final static String RESOURCE_COLUMN_EFFORT = "ResourceColumnEffort";
   public final static String RESOURCE_COLUMN_COSTS = "ResourceColumnCosts";
   public final static String PROJECT_CHOICE_FIELD = "ProjectChooser";
   public final static String START_TIME_CHOICE_FIELD = "StartTimeChooser";
   public final static long  ALL_PROJECTS_SELECTION = -1;
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
      OpBroker broker = session.newBroker();
      // Locate time record data set in form
      XComponent work_record_set = form.findComponent(WORK_RECORD_SET);
      XComponent data_row;

      // Note: OpUser instance in session is detached, we therefore have to refetch it
      OpUser user = (OpUser) (broker.getObject(OpUser.class, session.getUserID()));
      if (user.getResources().size() == 0) {
         return; // TODO: UI-level error -- no resource associated with this user
      }

      OpQuery query = broker.newQuery("select resource.ID, resource.Name from OpResource as resource where resource.User.ID = ?");
      query.setLong(0, session.getUserID());
      Iterator result = broker.list(query).iterator();
      if (!result.hasNext()) {
         return; // Nothing to do (TODO: Maybe display a message that no resources are available?)
      }

      List resourceIds = new ArrayList();
      HashMap resourceMap = new HashMap();
      Object[] record = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         resourceIds.add(record[0]);
         resourceMap.put(record[0], record[1]);
      }

      // Hide "Resource" column if user only manages a single resource (keep it simple)
      if (resourceIds.size() == 1) {
         form.findComponent(RESOURCE_COLUMN_EFFORT).setHidden(true);
         form.findComponent(RESOURCE_COLUMN_COSTS).setHidden(true);
      }

      List activityTypes = new ArrayList();
      activityTypes.add(new Byte(OpActivity.STANDARD));
      activityTypes.add(new Byte(OpActivity.MILESTONE));
      activityTypes.add(new Byte(OpActivity.TASK));

      /*fill project set*/
      XComponent projectDataSet = form.findComponent(PROJECT_SET);
      OpMyTasksFormProvider.fillProjectDataSet(broker, resourceIds, activityTypes, projectDataSet);

      Date startBefore = getFilteredStartBeforeDate(session, parameters, form);
      long projectNodeId = getFilteredProjectNodeId(session, parameters, form);
      result = getAssignments(broker, resourceIds, activityTypes, startBefore, projectNodeId);

      OpAssignment assignment;
      OpActivity activity;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         assignment = (OpAssignment) record[0];
         activity = (OpActivity) record[1];
         logger.debug("   Assignment: " + assignment.getID());
         logger.debug("   Assignment.Activity: " + assignment.getActivity());
         logger.debug("   Assignment.Activity.ID: " + assignment.getActivity().getID());

         boolean progressTracked = activity.getProjectPlan().getProgressTracked();
         //filter out milestones when progress tracking is off
         if (!progressTracked && activity.getType() == OpActivity.MILESTONE) {
            continue;
         }

         data_row = createWorkSlipDataRow(activity, assignment, progressTracked, resourceMap);
         work_record_set.addChild(data_row);

      }
      logger.debug("*** after loop");

      broker.close();

   }

   public static Iterator getAssignments(OpBroker broker, List resourceIds,List activityTypes, Date start, long projectNodeId) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("select assignment, activity from OpAssignment as assignment inner join assignment.Activity as activity ");
      buffer.append("where assignment.Resource.ID in (:resourceIds) and assignment.Complete < 100 and activity.Type in (:type)");
      if (start != null){
         buffer.append(" and activity.Start < :startBefore");
      }
      if (projectNodeId != ALL_PROJECTS_SELECTION){
         buffer.append(" and assignment.ProjectPlan.ProjectNode.ID = :projectNodeId");
      }
      OpQuery query = broker.newQuery(buffer.toString());
      query.setCollection("resourceIds", resourceIds);
      query.setCollection("type", activityTypes);
      if (start != null) {
         query.setDate("startBefore", start);
      }
      if (projectNodeId != ALL_PROJECTS_SELECTION) {
         query.setLong("projectNodeId", projectNodeId);
      }
      return broker.iterate(query);
   }

   public static XComponent createWorkSlipDataRow(OpActivity activity, OpAssignment assignment, boolean progressTracked, HashMap resourceMap) {
      XComponent data_row;
      String choice;
      XComponent data_cell;
      double remainingEffort;
      data_row = new XComponent(XComponent.DATA_ROW);
      // Iterate super-activities and "patch" activity name by adding context
      // (Note: This and the assignments can be optimized using bulk-queries)
         String name = activity.getName();
         StringBuffer activityName = (name != null) ? new StringBuffer(name) : new StringBuffer();
      OpActivity superActivity = activity.getSuperActivity();
      if (superActivity != null) {
         while (superActivity != null) {
            if (superActivity.getID() == activity.getSuperActivity().getID()) {
               activityName.append(" (");
            }
            else {
               activityName.append(" - ");
            }
               name = superActivity.getName();
               activityName.append((name != null) ? name : "");
            superActivity = superActivity.getSuperActivity();
         }
         activityName.append(')');
      }

      choice = XValidator.choice(assignment.locator(), activityName.toString());

      //activity name - 0
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setStringValue(choice);
      data_cell.setEnabled(false);
      data_row.addChild(data_cell);

      // New effort - 1
      data_cell = new XComponent(XComponent.DATA_CELL);
      if (activity.getType() == OpActivity.MILESTONE) {
         data_cell.setValue(null);
         data_cell.setEnabled(false);
      }
      else {
         data_cell.setDoubleValue(0.0);
         data_cell.setEnabled(true);
      }
      data_row.addChild(data_cell);

      // Remaining effort -- default value is current effort minus already booked effort - 2
      data_cell = new XComponent(XComponent.DATA_CELL);
      remainingEffort = assignment.getBaseEffort() - assignment.getActualEffort();
      if (remainingEffort < 0.0d) {
         remainingEffort = 0.0d;
      }
      if (progressTracked && activity.getType() != OpActivity.TASK && activity.getType() != OpActivity.MILESTONE) {
         data_cell.setDoubleValue(remainingEffort);
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }
      data_row.addChild(data_cell);

      // Material costs - 3
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(0.0);
      if (activity.getType() != OpActivity.MILESTONE) {
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }
      data_row.addChild(data_cell);

      // Travel costs - 4
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(0.0);
      if (activity.getType() != OpActivity.MILESTONE) {
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }
      data_row.addChild(data_cell);

      // External costs - 5
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(0.0);
      if (activity.getType() != OpActivity.MILESTONE) {
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }
      data_row.addChild(data_cell);

      // Miscellaneous costs - 6
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(0.0);
      if (activity.getType() != OpActivity.MILESTONE) {
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }
      data_row.addChild(data_cell);

      // Optional comment - 7
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_row.addChild(data_cell);

      // Resource id - 8
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setStringValue((String) resourceMap.get(new Long(assignment.getResource().getID())));
      data_row.addChild(data_cell);

      // Original remainig effort (can be changed from the client side) - 9
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(remainingEffort);
      data_cell.setEnabled(false);
      data_row.addChild(data_cell);

      // Completed - 10
      data_cell = new XComponent(XComponent.DATA_CELL);
      if (assignment.getProjectPlan().getProgressTracked()) {
         data_cell.setBooleanValue(false);
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setValue(null);
         data_cell.setEnabled(false);
      }
      data_row.addChild(data_cell);

      // Activity type - 11
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setIntValue(activity.getType());
      data_row.addChild(data_cell);

      // Activity created status (newly inerted / edit ) - 12
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setBooleanValue(true);
      data_row.addChild(data_cell);

      // Activity's project name - 13
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setStringValue(activity.getProjectPlan().getProjectNode().getName());
      data_row.addChild(data_cell);

      return data_row;
   }

   private Date getFilteredStartBeforeDate(OpProjectSession session, Map parameters ,XComponent form) {
     /*get start from choice field or session state*/
      String filteredStartFromId = (String)parameters.get(START_BEFORE_ID);

      if (filteredStartFromId == null) { //set the default selected index for the time chooser
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null){
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

   private long getFilteredProjectNodeId(OpProjectSession session, Map parameters ,XComponent form) {
      /*get project from choice field */
      String filteredProjectChoiceId = (String) parameters.get(PROJECT_CHOICE_ID);
      if (filteredProjectChoiceId == null) {
         //set the default selected index for the project chooser
         Map stateMap = session.getComponentStateMap(form.getID());
         if (stateMap != null){
            Integer defaultSelectedIndex = new Integer(0);
            stateMap.put(PROJECT_CHOICE_FIELD, defaultSelectedIndex);
         }
         return ALL_PROJECTS_SELECTION;
      }
      return filteredProjectChoiceId.equals(ALL) ? ALL_PROJECTS_SELECTION : OpLocator.parseLocator(filteredProjectChoiceId).getID();
   }
}
