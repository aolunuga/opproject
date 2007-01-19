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
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpEditWorkSlipFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getLogger(OpNewWorkSlipFormProvider.class,true);

   public final static String WORK_SLIP_ID_FIELD = "WorkSlipIDField";
   public final static String WORK_RECORD_SET = "WorkRecordSet";
   public final static String DATE_FIELD = "DateField";
   public final static String RESOURCE_COLUMN_EFFORT = "ResourceColumnEffort";
   public final static String RESOURCE_COLUMN_COSTS = "ResourceColumnCosts";

   // Form parameters
   public final static String WORK_SLIP_ID = "WorkSlipID";
   
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession)s;
      OpBroker broker = session.newBroker();
      
      boolean editMode = ((Boolean) parameters.get("edit_mode")).booleanValue();
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
      if (result.hasNext() && (((Integer) result.next()).intValue() == 1)) {
         form.findComponent(RESOURCE_COLUMN_EFFORT).setHidden(true);
         form.findComponent(RESOURCE_COLUMN_COSTS).setHidden(true);
      }

      // Locate time record data set in form
      XComponent work_record_set = form.findComponent(WORK_RECORD_SET);
      XComponent data_row;
      XComponent data_cell;

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
         choice = XValidator.choice(workRecord.getAssignment().locator(), activityName);

         boolean progressTracked = activity.getProjectPlan().getProgressTracked();

         //activity name - 0
         data_row = new XComponent(XComponent.DATA_ROW);
         work_record_set.addChild(data_row);
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setStringValue(choice);
         data_cell.setEnabled(false);
         data_row.addChild(data_cell);

         // Actual effort - 1
         data_cell = new XComponent(XComponent.DATA_CELL);
         if (activity.getType() == OpActivity.MILESTONE) {
            data_cell.setValue(null);
            data_cell.setEnabled(false);

         }
         else {
            data_cell.setDoubleValue(workRecord.getActualEffort());
            data_cell.setEnabled(editMode);
         }
         data_row.addChild(data_cell);

         // Remaining effort (remaining effort change is calculated after edit) - 2
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(workRecord.getRemainingEffort());
         if (progressTracked && activity.getType() != OpActivity.MILESTONE && activity.getType() != OpActivity.ADHOC_TASK) {
            data_cell.setEnabled(editMode);
         }
         else {
            data_cell.setEnabled(false);
            data_cell.setValue(null);
         }
         data_row.addChild(data_cell);

         // Material costs - 3
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(workRecord.getMaterialCosts());
         if (activity.getType() != OpActivity.MILESTONE) {
            data_cell.setEnabled(editMode);
         }
         else {
            data_cell.setEnabled(false);
            data_cell.setValue(null);
         }
         data_row.addChild(data_cell);

         // Travel costs - 4
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(workRecord.getTravelCosts());
         if (activity.getType() != OpActivity.MILESTONE) {
            data_cell.setEnabled(editMode);
         }
         else {
            data_cell.setEnabled(false);
            data_cell.setValue(null);
         }
         data_row.addChild(data_cell);

         // External costs - 5
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(workRecord.getExternalCosts());
         if (activity.getType() != OpActivity.MILESTONE) {
            data_cell.setEnabled(editMode);
         }
         else {
            data_cell.setEnabled(false);
            data_cell.setValue(null);
         }
         data_row.addChild(data_cell);

         // Miscellaneous costs - 6
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setDoubleValue(workRecord.getMiscellaneousCosts());
         if (activity.getType() != OpActivity.MILESTONE) {
            data_cell.setEnabled(editMode);
         }
         else {
            data_cell.setEnabled(false);
            data_cell.setValue(null);
         }
         data_row.addChild(data_cell);

         // Optional comment - 7
         data_cell = new XComponent(XComponent.DATA_CELL);
         if (workRecord.getComment() != null)
            data_cell.setStringValue(workRecord.getComment());
         data_cell.setEnabled(editMode);
         data_row.addChild(data_cell);

         // Resource name - 8
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setEnabled(false);
         data_cell.setStringValue(workRecord.getAssignment().getResource().getName());
         data_row.addChild(data_cell);

         //Original remaining effort - 9
         double originalRemainingEffort = workRecord.getRemainingEffort() - workRecord.getRemainingEffortChange();
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setEnabled(false);
         data_cell.setDoubleValue(originalRemainingEffort);
         data_row.addChild(data_cell);

         //Complete
         boolean complete = (workRecord.getAssignment().getComplete() == 100);
         if (complete) {
            data_row.getChild(2).setEnabled(false);
         }
         data_cell = new XComponent(XComponent.DATA_CELL);
         if (workRecord.getAssignment().getProjectPlan().getProgressTracked() || activity.getType() == OpActivity.ADHOC_TASK) {
            data_cell.setEnabled(editMode);
            data_cell.setBooleanValue(complete);
         }
         else {
            data_cell.setEnabled(false);
            data_cell.setValue(null);
         }
         data_row.addChild(data_cell);

         // Activity type - 11
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setIntValue(activity.getType());
         data_row.addChild(data_cell);

         // Activity created status (newly inerted / edit ) - 12
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setBooleanValue(false);
         data_row.addChild(data_cell);

         // Activity's project name - 13
         data_cell = new XComponent(XComponent.DATA_CELL);
         data_cell.setStringValue(activity.getProjectPlan().getProjectNode().getName());
         data_row.addChild(data_cell);

      }
      logger.debug("*** after loop");


     Boolean edit_mode = (Boolean)(parameters.get("edit_mode"));
     if (!edit_mode.booleanValue()){
       form.findComponent("EffortTable").setEnabled(false);
       form.findComponent("CostsTable").setEnabled(false);
       form.findComponent("DateField").setEnabled(false);
       form.findComponent("Cancel").setVisible(false);
       form.findComponent("WorkSlipAddIcon").setVisible(false);
       String title = session.getLocale().getResourceMap("work.Info").getResource("WorkInfo").getText();
       form.setText(title);
     }
      broker.close();

   }

}
