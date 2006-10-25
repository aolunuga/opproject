/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
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
   private final static String WORK_SLIP_ID = "WorkSlipIDField";
   private final static String NEW_ACTIVITIES = "NewAddedActivities";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      String workSlipLocator = (String) parameters.get(WORK_SLIP_ID);
      XComponent dataSet = form.findComponent(DATA_SET);

      OpBroker broker = session.newBroker();
      OpWorkSlip workSlip = (OpWorkSlip) broker.getObject(workSlipLocator);

      int weeks = 8; // TODO: Make #weeks configurable
      Date start = new Date(System.currentTimeMillis() + XCalendar.MILLIS_PER_WEEK * weeks);      

      XComponent data_row;
      OpAssignment assignment;
      OpActivity activity;
      OpWorkRecord workRecord;

      //the existing activities associated with the work slip in DB
      Iterator workRecords = workSlip.getRecords().iterator();
      List existingActivities = new ArrayList();
      while (workRecords.hasNext()) {
         workRecord = (OpWorkRecord) (workRecords.next());
         activity = workRecord.getAssignment().getActivity();
         existingActivities.add(new Long(activity.getID()));
      }

      //the activities newly added in the data set
      XComponent newActivitiesDataSet = (XComponent) parameters.get(NEW_ACTIVITIES);
      if (newActivitiesDataSet!= null) {
         for (int i = 0; i < newActivitiesDataSet.getChildCount(); i++) {
            XComponent dataRow = (XComponent) newActivitiesDataSet.getChild(i);
            long activityId = dataRow.getLongValue();
            existingActivities.add(new Long(activityId));
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

      List activityTypes = new ArrayList();
      activityTypes.add(new Byte(OpActivity.STANDARD));
      activityTypes.add(new Byte(OpActivity.MILESTONE));
      activityTypes.add(new Byte(OpActivity.TASK));

      result = OpNewWorkSlipFormProvider.getAssignments(broker, resourceIds, activityTypes, start, OpNewWorkSlipFormProvider.ALL_PROJECTS_SELECTION);

      while (result.hasNext()) {
         record = (Object[]) result.next();
         assignment = (OpAssignment) record[0];
         activity = (OpActivity) record[1];

         boolean progressTracked = activity.getProjectPlan().getProgressTracked();
         //filter out milestones when progress tracking is off
         if (!progressTracked && activity.getType() == OpActivity.MILESTONE) {
            continue;
         }

         if (existingActivities.contains(new Long(activity.getID()))) {
            continue;
         }

         //activity name to be displayed
         data_row = OpNewWorkSlipFormProvider.createWorkSlipDataRow(activity, assignment, progressTracked, resourceMap);
         String caption = ((XComponent) data_row.getChild(0)).getStringValue();
         data_row.setStringValue(caption);

         //add activity id
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setLongValue(activity.getID());
         data_row.addChild(dataCell);

         dataSet.addChild(data_row);
      }
   }

}
