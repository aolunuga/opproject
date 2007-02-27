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
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkSlipDataSetFactory;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

public class OpNewWorkSlipFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getLogger(OpNewWorkSlipFormProvider.class, true);

   public final static String WORK_RECORD_SET = "WorkRecordSet";
   public final static String RESOURCE_COLUMN_EFFORT = "ResourceColumnEffort";
   public final static String RESOURCE_COLUMN_COSTS = "ResourceColumnCosts";
   public final static String PROJECT_CHOICE_FIELD = "ProjectChooser";
   public final static String START_TIME_CHOICE_FIELD = "StartTimeChooser";
   public final static String PROJECT_SET = "ProjectSet";

   // filters
   public final static String START_BEFORE_ID = "start_before_id";
   public final static String PROJECT_CHOICE_ID = "project_choice_id";

   //start from filter choices
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
      activityTypes.add(new Byte(OpActivity.ADHOC_TASK));

      //fill project set
      XComponent projectDataSet = form.findComponent(PROJECT_SET);
      List projectNodes = OpMyTasksFormProvider.getProjects(broker, resourceIds, activityTypes);
         for (Iterator it = projectNodes.iterator(); it.hasNext();) {
            OpProjectNode projectNode = (OpProjectNode) it.next();
            XComponent row = new XComponent(XComponent.DATA_ROW);
            String choice = XValidator.choice(projectNode.locator(), projectNode.getName());
            row.setStringValue(choice);
            projectDataSet.addDataRow(row);
         }

      Date startBefore = getFilteredStartBeforeDate(session, parameters, form);
      long projectNodeId = getFilteredProjectNodeId(session, parameters, form);
      result = OpWorkSlipDataSetFactory.getAssignments(broker, resourceIds, activityTypes, startBefore, projectNodeId);

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

         data_row = OpWorkSlipDataSetFactory.createWorkSlipDataRow(activity, assignment, progressTracked, resourceMap);
         work_record_set.addChild(data_row);

      }
      logger.debug("*** after loop");
      broker.close();
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
