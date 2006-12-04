/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.user.OpUser;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OpWorkService extends OpProjectService {

   private static final XLog logger = XLogFactory.getLogger(OpWorkService.class, true);

   public final static String START = "start";
   public final static String WORK_RECORD_SET = "work_record_set";
   public final static String WORK_SLIP_ID = "work_slip_id";
   public final static String WORK_SLIP_IDS = "work_slip_ids";
   /*work record data column indexes */
   private final int TASK_NAME_COLOMN_INDEX = 0;
   private final int ACTUAL_EFFORT_COLOMN_INDEX = 1;
   private final int REMAINING_EFFORT_COLOMN_INDEX = 2;
   private final int MATERIAL_COSTS_COLOMN_INDEX = 3;
   private final int TRAVEL_COSTS_COLOMN_INDEX = 4;
   private final int EXTERNAL_COSTS_COLOMN_INDEX = 5;
   private final int MISCELLANEOUS_COSTS_COLOMN_INDEX = 6;
   private final int COMMENT_COLOMN_INDEX = 7;
   private final int COMPLETED_COLUMN_INDEX = 10;
   private final int ACTIVITY_TYPE_COLUMN_INDEX = 11;
   private final int ACTIVITY_INSERT_MODE = 12;

   public final static OpWorkErrorMap ERROR_MAP = new OpWorkErrorMap();
   private static final String WARNING = "warning";

   public XMessage insertWorkSlip(XSession s, XMessage request) {
      logger.info("OpWorkService.insertWorkSlip()");
      OpProjectSession session = (OpProjectSession) s;
      Date start = (Date) (request.getArgument(START));
      XComponent workRecordSet = (XComponent) (request.getArgument(WORK_RECORD_SET));

      //check for mandatory start field
      if (start == null) {
         XMessage reply = new XMessage();
         reply.setError(session.newError(ERROR_MAP, OpWorkError.DATE_MISSING));
         return reply;
      }
      //validate work record set
      XMessage reply = validateWorkRecordEfforts(session, workRecordSet, true);
      if (reply.getError() != null) {
         return reply;
      }

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      OpUser user = session.user(broker);

      //check for duplicate work slip date
      OpQuery query = broker.newQuery("select work_slip from OpWorkSlip as work_slip where work_slip.Creator.ID = ? and work_slip.Date = ?");
      query.setLong(0, user.getID());
      query.setDate(1, start);
      Iterator it = broker.iterate(query);
      if (it.hasNext()) {
         reply = new XMessage();
         reply.setError(session.newError(ERROR_MAP, OpWorkError.DUPLICATE_DATE));
         broker.close();
         return reply;
      }

      // Get max work-slip number for the current user
      query = broker.newQuery("select max(work_slip.Number) from OpWorkSlip as work_slip where work_slip.Creator.ID = ?");
      query.setLong(0, user.getID());
      Integer max = (Integer) (broker.iterate(query).next());
      if (max == null) {
         max = new Integer(0);
      }

      //create the work slip for the current user
      OpWorkSlip work_slip = new OpWorkSlip();
      work_slip.setNumber(max.intValue() + 1);
      work_slip.setCreator(user);
      work_slip.setCreated(XCalendar.today());
      work_slip.setDate(start);

      // Inserts work-records into database and add to progress calculcator
      insertWorkRecords(broker, work_slip, workRecordSet);
      broker.makePersistent(work_slip);

      logger.info("/OpWorkService.insertWorkSlip()");
      t.commit();
      broker.close();
      return null;
   }

   public XMessage editWorkSlip(XSession s, XMessage request) {
      logger.info("OpWorkService.editWorkSlip()");
      OpProjectSession session = (OpProjectSession) s;

      String work_slip_id = (String) (request.getArgument(WORK_SLIP_ID));
      XComponent work_record_set = (XComponent) (request.getArgument(WORK_RECORD_SET));

      //validate work record set
      XMessage reply = validateWorkRecordEfforts(session, work_record_set, false);
      if (reply.getError() != null || reply.getArgument(WARNING) != null) {
         return reply;
      }
      // TODO: Error handling (parameters set)?
      OpBroker broker = session.newBroker();

      OpWorkSlip work_slip = (OpWorkSlip) broker.getObject(work_slip_id);

      if (work_slip == null) {
         reply = new XMessage();
         reply.setError(session.newError(ERROR_MAP, OpWorkError.WORK_SLIP_NOT_FOUND));
         broker.close();
         return reply;
      }

      OpTransaction t = broker.newTransaction();

      // Delete outdated work-records from database and remove from progress calculcator
      deleteWorkRecords(broker, work_slip);

      // Inserts new work-records into database and add to progress calculcator
      insertWorkRecords(broker, work_slip, work_record_set);

      t.commit();

      logger.info("/OpWorkService.editWorkSlip()");

      broker.close();

      return null;
   }

   public XMessage deleteWorkSlips(XSession s, XMessage request) {
      ArrayList id_strings = (ArrayList) (request.getArgument(WORK_SLIP_IDS));
      logger.info("OpWorkService.deleteWorkSlip(): workslip_ids = " + id_strings);
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      for (int i = 0; i < id_strings.size(); i++) {
         String id_string = (String) (id_strings.get(i));

         // *** More error handling needed (check mandatory fields)
         OpWorkSlip work_slip = (OpWorkSlip) broker.getObject(id_string);
         if (work_slip == null) {
            logger.warn("ERROR: Could not find object with ID " + id_string);
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.WORK_SLIP_NOT_FOUND));
            broker.close();
            return reply;
         }
         // Use progress calculator to update progress information in associated project plan
         OpProgressCalculator.removeWorkRecords(broker, work_slip.getRecords().iterator());
         broker.deleteObject(work_slip);
      }

      t.commit();
      logger.info("/OpWorkService.deleteWorkSlip()");
      broker.close();
      return null;
   }

   protected void deleteWorkRecords(OpBroker broker, OpWorkSlip work_slip) {
      // Use progress calculator to update progress information in associated project plan
      OpProgressCalculator.removeWorkRecords(broker, work_slip.getRecords().iterator());
      Iterator work_records = work_slip.getRecords().iterator();
      OpWorkRecord work_record = null;
      while (work_records.hasNext()) {
         work_record = (OpWorkRecord) (work_records.next());
         broker.deleteObject(work_record);
      }
   }

   /**
    * Persist <code>OpWorkRecord</code>s entities for the given <code>work_slip</code> resource.
    *
    * @param broker          <code>OpBroker</code>
    * @param work_slip       <code>OpWorkSlip</code>
    * @param work_record_set <code>XComponent.DATA_SET</code> of work records
    */
   protected void insertWorkRecords(OpBroker broker, OpWorkSlip work_slip, XComponent work_record_set) {
      OpWorkRecord work_record = null;
      XComponent data_row = null;
      XComponent data_cell = null;
      OpAssignment assignment = null;
      List workRecordsToAdd = new ArrayList();

      for (int i = 0; i < work_record_set.getChildCount(); i++) {
         data_row = (XComponent) work_record_set.getChild(i);

         //check if the work-record has changed
         XComponent activityInsertMode = (XComponent) data_row.getChild(ACTIVITY_INSERT_MODE);
         boolean insert = activityInsertMode.getBooleanValue();
         if (!hasWorkRecordChanged(data_row, insert)) {
            continue;
         }

         //work slip's resource record
         work_record = new OpWorkRecord();
         work_record.setWorkSlip(work_slip);
         data_cell = (XComponent) data_row.getChild(TASK_NAME_COLOMN_INDEX);
         assignment = (OpAssignment) (broker.getObject(XValidator.choiceID(data_cell.getStringValue())));
         work_record.setAssignment(assignment);

         //complete
         data_cell = (XComponent) data_row.getChild(COMPLETED_COLUMN_INDEX);
         if (data_cell.getValue() != null) {
            work_record.setCompleted(data_cell.getBooleanValue());
         }

         // actual effort data cell
         data_cell = (XComponent) data_row.getChild(ACTUAL_EFFORT_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            work_record.setActualEffort(data_cell.getDoubleValue());
         }
         else {
            work_record.setActualEffort(0);
         }

         // Remaining effort is more complicated: Store estimation value and value change (for rollback and editing)
         data_cell = (XComponent) data_row.getChild(REMAINING_EFFORT_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            work_record.setRemainingEffort(data_cell.getDoubleValue());

            // Must be negative if remaining effort is lower than original remaining effort
            work_record.setRemainingEffortChange(data_cell.getDoubleValue() - assignment.getRemainingEffort());
         }
         else {
            work_record.setRemainingEffort(0);
            work_record.setRemainingEffortChange(0);
         }

         // Material Costs data cell
         data_cell = (XComponent) data_row.getChild(MATERIAL_COSTS_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            work_record.setMaterialCosts(data_cell.getDoubleValue());
         }
         else {
            work_record.setMaterialCosts(0);
         }

         // Travel costs
         data_cell = (XComponent) data_row.getChild(TRAVEL_COSTS_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            work_record.setTravelCosts(data_cell.getDoubleValue());
         }
         else {
            work_record.setTravelCosts(0);
         }

         // External costs
         data_cell = (XComponent) data_row.getChild(EXTERNAL_COSTS_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            work_record.setExternalCosts(data_cell.getDoubleValue());
         }
         else {
            work_record.setExternalCosts(0);
         }

         // Miscellaneous Costs
         data_cell = (XComponent) data_row.getChild(MISCELLANEOUS_COSTS_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            work_record.setMiscellaneousCosts(data_cell.getDoubleValue());
         }
         else {
            work_record.setMiscellaneousCosts(0);
         }

         // Optional comment
         data_cell = (XComponent) data_row.getChild(COMMENT_COLOMN_INDEX);
         if (data_cell.getStringValue() != null) {
            work_record.setComment(data_cell.getStringValue());
         }

         // Use progress calculator to update progress information in associated project plan
         OpProgressCalculator.addWorkRecord(broker, work_record);

         //Set the personnel costs for the work record
         work_record.setPersonnelCosts(assignment.getResource().getHourlyRate() * work_record.getActualEffort());

         //Because addWorkRecord() performs hibernate queries, we only persist the work records at the end
         workRecordsToAdd.add(work_record);
      }

      //register work records with broker (done here because hibernate queries flush the session)
      Iterator it = workRecordsToAdd.iterator();
      while (it.hasNext()) {
         broker.makePersistent((OpWorkRecord) it.next());
      }
   }

   /**
    * Checks if a work record's information has changed or not (determining whether it should be inserted or not).
    *
    * @param dataRow    a <code>XComponent</code> representing the client-side work record data.
    * @param insertMode a <code>boolean</code> indicating whether we are in edit or insert mode.
    * @return true if the workrecord has changed, false otherwise.
    */
   private boolean hasWorkRecordChanged(XComponent dataRow, boolean insertMode) {
      if (insertMode) {
         int activityType = ((XComponent) dataRow.getChild(ACTIVITY_TYPE_COLUMN_INDEX)).getIntValue();
         XComponent dataCell = null;
         dataCell = (XComponent) dataRow.getChild(MATERIAL_COSTS_COLOMN_INDEX);
         double cost;
         boolean zeroCosts = true;
         if (dataCell.getValue() != null) {
            cost = dataCell.getDoubleValue();
            if (cost != 0){
               zeroCosts = false;
            }
         }
         dataCell = (XComponent) dataRow.getChild(TRAVEL_COSTS_COLOMN_INDEX);
         if (dataCell.getValue() != null) {
            cost = dataCell.getDoubleValue();
            if (cost != 0){
               zeroCosts = false;
            }
         }
         dataCell = (XComponent) dataRow.getChild(EXTERNAL_COSTS_COLOMN_INDEX);
         if (dataCell.getValue() != null) {
            cost = dataCell.getDoubleValue();
            if (cost != 0){
               zeroCosts = false;
            }
         }
         dataCell = (XComponent) dataRow.getChild(MISCELLANEOUS_COSTS_COLOMN_INDEX);
         if (dataCell.getValue() != null) {
            cost = dataCell.getDoubleValue();
            if (cost != 0){
               zeroCosts = false;
            }
         }


         switch (activityType) {
            case OpActivity.STANDARD: {
               boolean completed = true;
               dataCell = (XComponent) dataRow.getChild(COMPLETED_COLUMN_INDEX);
               if (dataCell.getValue() != null) {
                  completed = dataCell.getBooleanValue();
               }
               else {
                  completed = false;
               }

               double actualEffort = -1;
               dataCell = (XComponent) dataRow.getChild(ACTUAL_EFFORT_COLOMN_INDEX);
               if (dataCell.getValue() != null) {
                  actualEffort = dataCell.getDoubleValue();
               }
               if (actualEffort == 0.0 && !completed && zeroCosts) {
                  return false;
               }
               break;
            }
            case OpActivity.MILESTONE: {
               dataCell = (XComponent) dataRow.getChild(COMPLETED_COLUMN_INDEX);
               if (dataCell.getValue() != null) {
                  boolean complete = dataCell.getBooleanValue();
                  if (!complete && zeroCosts) {
                     return false;
                  }
               }
               break;
            }
            case OpActivity.TASK: {
               boolean completed = true;
               dataCell = (XComponent) dataRow.getChild(COMPLETED_COLUMN_INDEX);
               if (dataCell.getValue() != null) {
                  completed = dataCell.getBooleanValue();
               }
               double actualEffort = -1;
               dataCell = (XComponent) dataRow.getChild(ACTUAL_EFFORT_COLOMN_INDEX);
               if (dataCell.getValue() != null) {
                  actualEffort = dataCell.getDoubleValue();
               }

               if (!completed && actualEffort == 0 && zeroCosts) {
                  return false;
               }
               break;
            }
         }
      }
      return true;
   }


   /**
    * Performs validation of the <code>work_record_set</code>
    *
    * @param session         the <code>OpProjectSession</code>
    * @param work_record_set the <code>XComponent.DATA_SET</code> of records
    * @param insertMode      a <code>boolean</code> indicating whether the operation is insert or edit.
    * @return <code>XMessage</code> containing a not null <code>XError</code> instance field if validation fails.
    */
   private XMessage validateWorkRecordEfforts(OpProjectSession session, XComponent work_record_set, boolean insertMode) {

      XMessage reply = new XMessage();
      XComponent data_row;
      XComponent data_cell;
      boolean validActualEffort = false;
      boolean validCosts = false;

      for (int i = 0; i < work_record_set.getChildCount(); i++) {
         data_row = (XComponent) work_record_set.getChild(i);

         //completed
         Boolean completedValue = (Boolean) ((XComponent) data_row.getChild(COMPLETED_COLUMN_INDEX)).getValue();
         if (completedValue != null && completedValue.booleanValue()) {
            validActualEffort = true;
         }

         // actual effort
         data_cell = (XComponent) data_row.getChild(ACTUAL_EFFORT_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            double actualEffort = data_cell.getDoubleValue();
            if (actualEffort < 0) {
               reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ACTUAL_EFFORT));
               return reply;
            }
            if (actualEffort > 0) {
               //we found a valid effort
               validActualEffort = true;
            }
         }
         // Remaining effort
         data_cell = (XComponent) data_row.getChild(REMAINING_EFFORT_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            if (data_cell.getDoubleValue() < 0) {
               reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_REMAINING_EFFORT));
               return reply;
            }
         }
         // Material Costs
         data_cell = (XComponent) data_row.getChild(MATERIAL_COSTS_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            if (data_cell.getDoubleValue() < 0) {
               reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_MATERIAL_COSTS));
               return reply;
            }
            if (data_cell.getDoubleValue() > 0) {
               validCosts = true;
            }
         }
         // Travel costs
         data_cell = (XComponent) data_row.getChild(TRAVEL_COSTS_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            if (data_cell.getDoubleValue() < 0) {
               reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_TRAVEL_COSTS));
               return reply;
            }
            if (data_cell.getDoubleValue() > 0) {
               validCosts = true;
            }
         }
         // External costs
         data_cell = (XComponent) data_row.getChild(EXTERNAL_COSTS_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            if (data_cell.getDoubleValue() < 0) {
               reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_EXTERNAL_COSTS));
               return reply;
            }
            if (data_cell.getDoubleValue() > 0) {
               validCosts = true;
            }
         }
         // Miscellaneous Costs
         data_cell = (XComponent) data_row.getChild(MISCELLANEOUS_COSTS_COLOMN_INDEX);
         if (data_cell.getValue() != null) {
            if (data_cell.getDoubleValue() < 0) {
               reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_MISCELLANEOUS_COSTS));
               return reply;
            }
            if (data_cell.getDoubleValue() > 0) {
               validCosts = true;
            }
         }
      }
      // a valid effort was not found in the work record set
      if (!validActualEffort && !validCosts && insertMode) {
         reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ACTUAL_EFFORT));
      }
      else if (!validActualEffort && !validCosts) {
         //for edit, we just ignore the error...
         reply.setArgument(WARNING, Boolean.TRUE);
      }
      return reply;
   }

}
