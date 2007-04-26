/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.work.OpWorkSlipDataSetFactory;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Form provider for add activity to work slip dialog.
 *
 * @author mihai.costin
 */
public class OpAddWorkSlipActivityFormProvider implements XFormProvider {
   private final static String DATA_SET = "ActivityDataSet";
   private final static String WORK_DATA_SET = "WorkDataSet";
   private final static String NEW_ACTIVITIES = "NewAddedActivities";
   private final static String ORIGINAL_ACTIVITIES = "OriginalActivities";
   private final static int ORIGINAL_REMAINING_EFFORT_INDEX = 9;
   private final static int REMAINING_EFFORT_INDEX = 2;
   private final static int ACTUAL_EFFORT_INDEX = 1;

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      XComponent dataSet = form.findComponent(DATA_SET);

      OpBroker broker = session.newBroker();

      XComponent activityDataRow;
      OpAssignment assignment;
      OpActivity activity;

      //the existing activities associated with the work slip
      List<Long> existingActivities = new ArrayList<Long>();

      //the activities previously added in the data set
      XComponent workDataSet = (XComponent) parameters.get(WORK_DATA_SET);
      if (workDataSet != null) {
         for (int i = 0; i < workDataSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) workDataSet.getChild(i);
            OpLocator dataRowLocator =  OpLocator.parseLocator(XValidator.choiceID(dataRow.getStringValue()));
            long activityId = dataRowLocator.getID();
            existingActivities.add(activityId);
         }
      }

      //the activities newly added in the data set
      XComponent newActivitiesDataSet = (XComponent) parameters.get(NEW_ACTIVITIES);
      if (newActivitiesDataSet != null) {
         for (int i = 0; i < newActivitiesDataSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) newActivitiesDataSet.getChild(i);
            OpLocator dataRowLocator =  OpLocator.parseLocator(XValidator.choiceID(dataRow.getStringValue()));
            long activityId = dataRowLocator.getID();
            existingActivities.add(activityId);
         }
      }

      //all the possible activities for this
      OpQuery query = broker.newQuery("select resource.ID, resource.Name from OpResource as resource where resource.User.ID = ?");
      query.setLong(0, session.getUserID());
      Iterator result = broker.list(query).iterator();
      if (!result.hasNext()) {
         return;
      }
      List resourceIds = new ArrayList();
      HashMap resourceMap = new HashMap();
      Object[] record;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         resourceIds.add(record[0]);
         resourceMap.put(record[0], record[1]);
      }

      List<Byte> activityTypes = new ArrayList<Byte>();
      activityTypes.add(OpActivity.STANDARD);
      activityTypes.add(OpActivity.MILESTONE);
      activityTypes.add(OpActivity.TASK);
      activityTypes.add(OpActivity.ADHOC_TASK);

      XComponent originalActivitiesDataSet = (XComponent) parameters.get(ORIGINAL_ACTIVITIES);     
      result = OpWorkSlipDataSetFactory.getAssignments(broker, resourceIds, activityTypes, null, OpWorkSlipDataSetFactory.ALL_PROJECTS_ID);

      while (result.hasNext()) {
         record = (Object[]) result.next();
         assignment = (OpAssignment) record[0];
         activity = (OpActivity) record[1];

         boolean progressTracked = activity.getProjectPlan().getProgressTracked();
         //filter out milestones when progress tracking is off
         if (!progressTracked && activity.getType() == OpActivity.MILESTONE) {
            continue;
         }

         if (existingActivities.contains(new Long(assignment.getID()))) {
            continue;
         }

         //activity name to be displayed
         activityDataRow = OpWorkSlipDataSetFactory.createWorkSlipDataRow(activity, assignment, progressTracked, resourceMap);
         String caption = activity.getName();
         //if an activity has more than one resource show the name of the resource and the name of the activity
         if(activity.getAssignments().size() > 1){
            caption = assignment.getResource().getName() + ": " + caption;
         }
         String choice = XValidator.choice(assignment.locator(), caption);
         activityDataRow.setStringValue(choice);

         //add a last cell with the assignment id
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(assignment.locator());
         activityDataRow.addChild(dataCell);

         if (originalActivitiesDataSet != null) {
            for (int i = 0; i < originalActivitiesDataSet.getChildCount(); i++) {
               XComponent originalDataRow = (XComponent) originalActivitiesDataSet.getChild(i);
               if(originalDataRow.getStringValue().equals(activityDataRow.getStringValue())){
                  double originalRemainingEffort = ((XComponent)originalDataRow.getChild(REMAINING_EFFORT_INDEX)).getDoubleValue();
                  double originalActualEffort = ((XComponent)originalDataRow.getChild(ACTUAL_EFFORT_INDEX)).getDoubleValue();
                  ((XComponent)activityDataRow.getChild(REMAINING_EFFORT_INDEX)).setDoubleValue(originalActualEffort + originalRemainingEffort);
                  ((XComponent)activityDataRow.getChild(ORIGINAL_REMAINING_EFFORT_INDEX)).setDoubleValue(originalActualEffort + originalRemainingEffort);
               }
            }
         }

         dataSet.addChild(activityDataRow);
      }
   }

}
