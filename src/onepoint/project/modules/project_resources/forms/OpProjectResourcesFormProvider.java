/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_resources.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project_resources.OpProjectResourceSummary;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpProjectResourcesFormProvider implements XFormProvider {

   private final static String RESOURCE_SET = "ResourceSet";

   protected final static String PROJECT_ID = "project_id";

   public final static int ACTIVITY_RESOURCE_COLUMN_INDEX = 0;
   public final static int BASE_COLUMN_INDEX = 1;
   public final static int ACTUAL_COLUMN_INDEX = 2;
   public final static int REMAINING_COLUMN_INDEX = 3;
   public final static int PREDICTED_COLUMN_INDEX = 4;
   public final static int DEVIATION_COLUMN_INDEX = 5;
   public final static int DEVIATION100_COLUMN_INDEX = 6;
   private final static String PRINT_TITLE = "PrintTitle";
   private final static String PRINT_BUTTON = "PrintButton";
   // NL: Remaining is estimation of resource (effortToComplete); predicted = actual + remaining
   // (Therefore, deviation = predicted - base)

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {


      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();

      // Decide on project-ID and retrieve project
      String project_locator = (String) (parameters.get(PROJECT_ID));
      if (project_locator != null) {
         // Get open project-ID from parameters and set project-ID session variable
         session.setVariable(PROJECT_ID, project_locator);
      }
      else
         project_locator = (String) (session.getVariable(PROJECT_ID));

      if (project_locator != null) {

         OpProjectNode project = (OpProjectNode) (broker.getObject(project_locator));
         //print title
         form.findComponent(PRINT_TITLE).setStringValue(project.getName());
         form.findComponent(PRINT_BUTTON).setEnabled(true);

         // Locate data set in form
         XComponent data_set = form.findComponent(RESOURCE_SET);
         int max_outline_level = getMaxOutlineLevel(form, session);
         // Create dynamic resource summaries for collection-activities
         // (Note: Value of collection-activities have been set on check-in/work-calculator)
         createViewDataSet(broker, project, max_outline_level, data_set);
      }
      broker.close();
   }

   protected int getMaxOutlineLevel(XComponent form, OpProjectSession session) {
      return 0;
   }

   protected void createViewDataSet(OpBroker broker, OpProjectNode project, int max_outline_level, XComponent data_set) {
      OpQuery query = broker.newQuery("from OpActivity as activity where activity.ProjectPlan.ProjectNode.ID = ? and activity.Deleted = false order by activity.Sequence");
      query.setID(0, project.getID());
      Iterator activities = broker.iterate(query);
      OpActivity activity = null;
      double predicted = 0.0f;
      Iterator assignments = null;
      OpAssignment assignment = null;
      OpActivity previous_visible_activity = null;
      HashMap resource_summaries = new HashMap();
      OpProjectResourceSummary resource_summary = null;
      long resource_id = 0;
      while (activities.hasNext()) {
         activity = (OpActivity) (activities.next());
         // Filter out milestones
         if (activity.getType() == OpActivity.MILESTONE){
            continue;
         }
         if (activity.getOutlineLevel() <= max_outline_level) {
            // Add previous visible activity with summarized values and reset both
            if (previous_visible_activity != null)
               _addActivity(data_set, previous_visible_activity, resource_summaries);
            resource_summaries.clear();
            previous_visible_activity = activity;
         }
         // Add values of assignments to resource summaries of previous visible row
         assignments = activity.getAssignments().iterator();
         while (assignments.hasNext()) {
            assignment = (OpAssignment) (assignments.next());
            resource_id = assignment.getResource().getID();
            resource_summary = (OpProjectResourceSummary) (resource_summaries.get(new Long(resource_id)));
            if (resource_summary == null) {
               resource_summary = new OpProjectResourceSummary(resource_id, assignment.getResource().getName());
               resource_summaries.put(new Long(resource_id), resource_summary);
            }
            /*
         // TODO: Update algorithm to work w/OpAssignment.EFFORT_TO_COMPLETE (via tracking tool)
         if (assignment.getComplete() > 0)
            predicted = assignment.getActualEffort() * 100 / assignment.getComplete();
         else
            predicted = assignment.getBaseEffort();
            */
            predicted = assignment.getActualEffort() + assignment.getRemainingEffort();
            resource_summary.addBaseEffort(assignment.getBaseEffort());
            resource_summary.addActualEffort(assignment.getActualEffort());
            resource_summary.addEffortToComplete(assignment.getRemainingEffort());
            resource_summary.addPredictedEffort(predicted);
         }
      }
      // Add last visible activity
      if (previous_visible_activity != null)
         _addActivity(data_set, previous_visible_activity, resource_summaries);
   }

   private void _addActivity(XComponent data_set, OpActivity activity, HashMap resource_summaries) {
      // NL: Remaining is estimation of resource (effortToComplete); predicted = actual + remaining
      // (Therefore, deviation = predicted - base)
      double base = activity.getBaseEffort();
      double actual = activity.getActualEffort();
      // TODO: NL actually means effort-to-complete w/remaining
      // (Add as separate, additional column?)
      double remaining = base - actual;
      double predicted = actual + activity.getRemainingEffort();
      // TODO: Check algorithm for calculated predicted effort
      /*
      if (activity.getComplete() > 0)
         predicted = actual * 100 / activity.getComplete();
      else
         predicted = base;
      */
      double deviation = predicted - base;
      XComponent data_row = new XComponent(XComponent.DATA_ROW);
      byte outline_level = activity.getOutlineLevel();
      data_row.setOutlineLevel(outline_level);
      data_row.setExpanded(true);
      XComponent data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setStringValue(activity.getName());
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(base);
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(actual);
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(remaining);
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(predicted);
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(deviation);
      data_row.addChild(data_cell);
      data_cell = new XComponent(XComponent.DATA_CELL);
      if (base != 0){
         data_cell.setDoubleValue(deviation * 100 / base);
      }
      else {
         if (deviation != 0){
            data_cell.setDoubleValue(Double.MAX_VALUE);
         }
         else {
            data_cell.setDoubleValue(0);
         }
      }
      data_row.addChild(data_cell);
      data_set.addChild(data_row);
      // Add resource summaries
      outline_level++;
      OpProjectResourceSummary resource_summary = null;
      Iterator i = resource_summaries.values().iterator();
      while (i.hasNext()) {
         resource_summary = (OpProjectResourceSummary) (i.next());
         base = resource_summary.getBaseEffort();
         actual = resource_summary.getActualEffort();
         /*
          * OLD: remaining = base - actual; if (activity.getComplete() > 0) predicted = actual * 100 / resource_summary.getComplete(); else
          * predicted = base;
          */
         // TODO: NL actually means effort-to-complete w/remaining
         // (Add as separate, additional column?)
         remaining = base - actual;
         predicted = resource_summary.getPredictedEffort();

         deviation = predicted - base;
         data_row = new XComponent(XComponent.DATA_ROW);
         data_row.setOutlineLevel(outline_level);
         data_row.setExpanded(true);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setStringValue(resource_summary.getResourceName());
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(base);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(actual);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(remaining);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(predicted);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(deviation);
         data_row.addChild(data_cell);
         data_cell = new XComponent(XComponent.DATA_CELL);
         if (base != 0){
            data_cell.setDoubleValue(deviation * 100 / base);
         }
         else {
            if (deviation != 0){
               data_cell.setDoubleValue(Double.MAX_VALUE);
            }
            else {
               data_cell.setDoubleValue(0);
            }
         }
         data_row.addChild(data_cell);
         data_set.addChild(data_row);
      }
   }

}