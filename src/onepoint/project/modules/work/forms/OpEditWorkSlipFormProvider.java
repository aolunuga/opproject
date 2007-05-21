/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpEditWorkSlipFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getServerLogger(OpNewWorkSlipFormProvider.class);

   public final static String WORK_SLIP_ID_FIELD = "WorkSlipIDField";
   public final static String WORK_RECORD_SET = "WorkRecordSet";
   public final static String DATE_FIELD = "DateField";
   public final static String RESOURCE_COLUMN_EFFORT = "ResourceColumnEffort";
   public final static String RESOURCE_COLUMN_COSTS = "ResourceColumnCosts";
   public final static String NEW_ADDED_ACTIVITIES_SET = "NewAddedActivities";
   public final static String ORIGINAL_DATABASE_ACTIVITIES = "OriginalDatabaseActivities";

   // Form parameters
   public final static String WORK_SLIP_ID = "WorkSlipID";
   
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession)s;
      OpBroker broker = session.newBroker();
      
      boolean editMode = (Boolean) parameters.get("edit_mode");
      form.findComponent("EditMode").setBooleanValue(editMode);
      String workSlipLocator = (String) parameters.get(WORK_SLIP_ID);
      // TODO: Error handling (not set)
      
      OpWorkSlip workSlip = (OpWorkSlip) broker.getObject(workSlipLocator);
      // TODO: Error handling (not found, access denied)

      form.findComponent("WorkSlipIDField").setStringValue(workSlipLocator);
      XComponent dateField = form.findComponent(DATE_FIELD);
      dateField.setDateValue(workSlip.getDate());

      OpQuery query = broker.newQuery("select count(resource.ID) from OpResource as resource where resource.User.ID = ?");
      query.setLong(0, session.getUserID());
      Iterator result = broker.list(query).iterator();
      if (result.hasNext() && (((Number) result.next()).intValue() == 1)) {
         form.findComponent(RESOURCE_COLUMN_EFFORT).setHidden(true);
         form.findComponent(RESOURCE_COLUMN_COSTS).setHidden(true);
      }

      // Locate time record data set in form
      XComponent workRecordSet = form.findComponent(WORK_RECORD_SET);
      XComponent originalRecordSet = form.findComponent(ORIGINAL_DATABASE_ACTIVITIES);
      XComponent dataRow;
      XComponent originalDataRow;
      XComponent dataCell;

      Iterator workRecords = workSlip.getRecords().iterator();
      logger.debug("*** after workRecords");
      OpWorkRecord workRecord;
      String choice;
      while (workRecords.hasNext()) {
         workRecord = (OpWorkRecord) (workRecords.next());
         OpActivity activity = workRecord.getAssignment().getActivity();
         logger.debug("   WorkRecord: " + workRecord.getID());
         String activityName = workRecord.getAssignment().getActivity().getName();
         if (activityName == null) {
            activityName = "";
         }
         choice = XValidator.choice(workRecord.getAssignment().getActivity().locator(), activityName);

         boolean progressTracked = activity.getProjectPlan().getProgressTracked();

         //activity name - 0
         dataRow = new XComponent(XComponent.DATA_ROW);
         //set the value of the dataRow to the id of the assignment
         String choiceAssignment = XValidator.choice(workRecord.getAssignment().locator(), activityName);
         dataRow.setStringValue(choiceAssignment);
         workRecordSet.addChild(dataRow);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(choice);
         dataCell.setEnabled(false);
         dataRow.addChild(dataCell);

         // Actual effort - 1
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (activity.getType() == OpActivity.MILESTONE) {
            dataCell.setValue(null);
            dataCell.setEnabled(false);

         }
         else {
            dataCell.setDoubleValue(workRecord.getActualEffort());
            dataCell.setEnabled(editMode);
         }
         dataRow.addChild(dataCell);

         // Remaining effort (remaining effort change is calculated after edit) - 2
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(workRecord.getRemainingEffort());
         if (progressTracked && activity.getType() != OpActivity.MILESTONE && activity.getType() != OpActivity.ADHOC_TASK) {
            dataCell.setEnabled(editMode);
         }
         else {
            dataCell.setEnabled(false);
            dataCell.setValue(null);
         }
         dataRow.addChild(dataCell);

         // Material costs - 3
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(workRecord.getMaterialCosts());
         if (activity.getType() != OpActivity.MILESTONE) {
            dataCell.setEnabled(editMode);
         }
         else {
            dataCell.setEnabled(false);
            dataCell.setValue(null);
         }
         dataRow.addChild(dataCell);

         // Travel costs - 4
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(workRecord.getTravelCosts());
         if (activity.getType() != OpActivity.MILESTONE) {
            dataCell.setEnabled(editMode);
         }
         else {
            dataCell.setEnabled(false);
            dataCell.setValue(null);
         }
         dataRow.addChild(dataCell);

         // External costs - 5
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(workRecord.getExternalCosts());
         if (activity.getType() != OpActivity.MILESTONE) {
            dataCell.setEnabled(editMode);
         }
         else {
            dataCell.setEnabled(false);
            dataCell.setValue(null);
         }
         dataRow.addChild(dataCell);

         // Miscellaneous costs - 6
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(workRecord.getMiscellaneousCosts());
         if (activity.getType() != OpActivity.MILESTONE) {
            dataCell.setEnabled(editMode);
         }
         else {
            dataCell.setEnabled(false);
            dataCell.setValue(null);
         }
         dataRow.addChild(dataCell);

         // Optional comment - 7
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (workRecord.getComment() != null)
            dataCell.setStringValue(workRecord.getComment());
         dataCell.setEnabled(editMode);
         dataRow.addChild(dataCell);

         // Resource name - 8
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         dataCell.setStringValue(workRecord.getAssignment().getResource().getName());
         dataRow.addChild(dataCell);

         //Original remaining effort - 9
         double originalRemainingEffort = workRecord.getRemainingEffort() - workRecord.getRemainingEffortChange();
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         dataCell.setDoubleValue(originalRemainingEffort);
         dataRow.addChild(dataCell);

         //Complete
         boolean complete = (workRecord.getAssignment().getComplete() == 100);
         if (complete) {
            dataRow.getChild(2).setEnabled(false);
         }
         dataCell = new XComponent(XComponent.DATA_CELL);
         if (workRecord.getAssignment().getProjectPlan().getProgressTracked() || activity.getType() == OpActivity.ADHOC_TASK) {
            dataCell.setEnabled(editMode);
            dataCell.setBooleanValue(complete);
         }
         else {
            dataCell.setEnabled(false);
            dataCell.setValue(null);
         }
         dataRow.addChild(dataCell);

         // Activity type - 11
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setIntValue(activity.getType());
         dataRow.addChild(dataCell);

         // Activity created status (newly inerted / edit ) - 12
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setBooleanValue(false);
         dataRow.addChild(dataCell);

         // Activity's project name - 13
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(activity.getProjectPlan().getProjectNode().getName());
         dataRow.addChild(dataCell);

         // Assignment base effort - 14
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(workRecord.getAssignment().getBaseEffort());
         dataRow.addChild(dataCell);

         //add a copy of each row to the original activities dataset
         originalDataRow = dataRow.copyData();
         originalRecordSet.addChild(originalDataRow);
      }
      logger.debug("*** after loop");

      Boolean paramEditMode = (Boolean) (parameters.get("edit_mode"));
      form.findComponent("EffortTable").setEditMode(paramEditMode);
      form.findComponent("CostsTable").setEditMode(paramEditMode);

      if (!paramEditMode) {
         form.findComponent("EffortTable").setEnabled(false);
         form.findComponent("CostsTable").setEnabled(false);
         form.findComponent("DateField").setEnabled(false);
         form.findComponent("Cancel").setVisible(false);
         form.findComponent("WorkSlipAddIcon").setVisible(false);
         form.findComponent("WorkSlipDeleteIcon").setVisible(false);
         String title = session.getLocale().getResourceMap("work.Info").getResource("WorkInfo").getText();
         form.setText(title);
      }
      broker.close();
   }
}
