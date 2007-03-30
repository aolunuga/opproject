/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserError;
import onepoint.project.modules.user.OpUserServiceImpl;
import onepoint.service.server.XServiceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * @author dfreis
 */
public class OpWorkServiceImpl {

   private static final XLog logger_ = XLogFactory.getLogger(OpWorkServiceImpl.class, true);
   public final static OpWorkErrorMap ERROR_MAP = new OpWorkErrorMap();

   /**
    * Puts the given <code>work_slip</code> to the users list of work sets.
    * If a work slip identified by <code>work_slip.getCreator()</code> and
    * <code>work_slip.getNumber()</code> already exists an XServiceException will be thrown.
    *
    * @param work_slip the Map of attributes representing the work slip
    * @throws XServiceException        the given {@link OpWorkSlip} is of an invalid state, or the given {@link OpWorkSlip} already exists.
    * @throws IllegalArgumentException if a required attribute is missing. In this case the work slip will not be added.
    */

   public void insertMyWorkSlip(OpProjectSession session, OpBroker broker, OpWorkSlip work_slip)
        throws XServiceException {
      if (work_slip.getCreator() == null) {
         work_slip.setCreator(session.user(broker));
      }
      if (!session.isUser(work_slip.getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only delete work records from his/her work slips
      }

      //check for mandatory start field
      if (work_slip.getDate() == null) {
         throw new XServiceException(session.newError(ERROR_MAP, OpWorkError.DATE_MISSING));
      }

      //validate work record set
      if (!work_slip.isValid()) {
         throw new XServiceException(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ACTUAL_EFFORT));
      }

      OpUser user = session.user(broker);
      // check for duplicate work slip date
      // must do this using a new broker! otherwise we get a 'Duplicate entry' SQLException!
      Integer max = new Integer(0);
      OpQuery query = broker.newQuery("select max(work_slip.Number) from OpWorkSlip as work_slip where work_slip.Creator.ID = ?");
      query.setLong(0, user.getID());
      max = (Integer) (broker.iterate(query).next());
      if (max == null) {
         max = new Integer(0);
      }

      // check assignment
      Set records = work_slip.getRecords();
      if (records != null) {
         OpWorkRecord record;
         Iterator iter = records.iterator();
         while (iter.hasNext()) {
            record = (OpWorkRecord) iter.next();
            if (record.getAssignment() == null) {
               throw (new XServiceException(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ASSIGNMENT)));
            }
         }
      }

      //create the work slip for the current user
      work_slip.setNumber(max.intValue() + 1);
      work_slip.setCreator(user);

      // Inserts work-records into database and add to progress calculator
      broker.makePersistent(work_slip);

   }

   /**
    * Puts the given wss[] to my list of work sets.
    * If a work slip identified by <code>wss[pos].getCreator()</code> and
    * <code>wss[pos].getNumber()</code> already exists an XServiceException will be thrown. In this case no {@link OpWorkSlip} will be added.
    * The behavior of this operation is unspecified if the specified map is modified while
    * the operation is in progress.
    *
    * @param wss the {@link OpWorkSlip} that is to be added.
    * @throws XServiceException        if any {@link OpWorkSlip} is of an invalid state. In this case no {@link OpWorkSlip} will be added.
    * @throws IllegalArgumentException if a required attribute is missing or if an attribute is of an invalid type. In this case NO work slip will be added.
    */

   public void insertMyWorkSlips(OpProjectSession session, OpBroker broker, Iterator<OpWorkSlip> wss)
        throws XServiceException {
      while (wss.hasNext()) {
         insertMyWorkSlip(session, broker, wss.next());
      }
   }

   /**
    * Removed the given work slip. Does nothing if the given work slip was not registered.
    *
    * @param work_slip the {@link OpWorkSlip} to delete.
    * @throws XServiceException        if the given {@link OpWorkSlip} is of an invalid state. In this the {@link OpWorkSlip} will not be removed.
    * @throws IllegalArgumentException if a required attribute is missing or if an attribute is of an invalid type. In this case NO work slip will be added.
    */

   public void deleteMyWorkSlip(OpProjectSession session, OpBroker broker, OpWorkSlip work_slip)
        throws XServiceException {
      if (!session.isUser(work_slip.getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only delete work records from his/her work slips
      }

      if (work_slip == null) {
         logger_.warn("ERROR: Could not find object with ID " + work_slip.getID());
         throw new XServiceException(session.newError(ERROR_MAP, OpWorkError.WORK_SLIP_NOT_FOUND));
      }
      // Use progress calculator to update progress information in associated project plan
      OpProgressCalculator.removeWorkRecords(broker, work_slip.getRecords().iterator());
      broker.deleteObject(work_slip);
   }

   /**
    * Removed the given work slips. If a given {@link OpWorkSlip} was not registered, it will be ignored and therefor not removed.
    *
    * @param wss the {@link OpWorkSlip}s to delete.
    * @throws XServiceException        if any given {@link OpWorkSlip} is of an invalid state. In this case all previous {@link OpWorkSlip} will be removed.
    * @throws IllegalArgumentException if a required attribute is missing. In this case NO work slip will be added.
    */

   public void deleteMyWorkSlips(OpProjectSession session, OpBroker broker, Iterator<OpWorkSlip> wss)
        throws XServiceException {
      // *** More error handling needed (check mandatory fields)
      while (wss.hasNext()) {
         deleteMyWorkSlip(session, broker, wss.next());
      }
   }

   public void updateMyWorkSlip(OpProjectSession session, OpBroker broker, OpWorkSlip work_slip)
        throws XServiceException {
      if (work_slip == null) {
         throw new IllegalArgumentException("work_slip must not be null!");
      }
      //String work_slip_id = (String) (request.getArgument(WORK_SLIP_ID));
      //XComponent work_record_set = (XComponent) (request.getArgument(WORK_RECORD_SET));

      // validate work record set
      if (!work_slip.isValid()) {
         throw new XServiceException(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ACTUAL_EFFORT), true);
      }
      // TODO: Error handling (parameters set)?

      // FIXME(dfreis Mar 22, 2007 8:12:37 AM)
      // should avoid usage of a new Broker (= new Hibernate Session), this works if OpProgressCalculator
      // is triggerd within Hibernate Interceptor/EventHandler (Or if we check for validity within OpWorkSlip/OpWorkRecord).
      // In this case we could (possible) skip the following whole block!
      // {
      OpWorkSlip old_work_slip = null;

      ArrayList<OpWorkRecord> records_to_delete = new ArrayList<OpWorkRecord>();

      OpBroker old_object_broker = session.newBroker();
      try {
         old_work_slip = getMyWorkSlipById(session, old_object_broker, work_slip.getID());
         if (old_work_slip == null) {
            throw new XServiceException(session.newError(ERROR_MAP, OpWorkError.WORK_SLIP_NOT_FOUND));
         }
//    HashSet new_records = new HashSet(work_slip.getRecords());
         HashMap<Long, OpWorkRecord> old_records_map = new HashMap<Long, OpWorkRecord>(old_work_slip.getRecords().size());
         Iterator iter = old_work_slip.getRecords().iterator();
         OpWorkRecord old;
         while (iter.hasNext()) {
            old = (OpWorkRecord) iter.next();
            old_records_map.put(new Long(old.getID()), old);
         }

         // now iterate..
         Iterator new_or_changed_records = work_slip.getRecords().iterator();
         OpWorkRecord new_or_changed_record;
         OpWorkRecord old_record;
         while (new_or_changed_records.hasNext()) {
            new_or_changed_record = (OpWorkRecord) new_or_changed_records.next();

            old_record = old_records_map.remove(new Long(new_or_changed_record.getID()));

            if (old_record == null) { // -> new record
               insertMyWorkRecord(session, broker, new_or_changed_record, work_slip);
            }
            else {
               if (!new_or_changed_record.equals(old_record)) { // -> update record
                  updateMyWorkRecord(session, broker, new_or_changed_record);
               }
            }
         }
         // finally delete all real old records
         // note: must not do this via OpWorkRecords stored within old_records_map
         //       since they use old_object_broker!
         Iterator<Long> delete_iter = old_records_map.keySet().iterator();
         OpWorkRecord to_delete;
         while (delete_iter.hasNext()) {
            to_delete = getMyWorkRecordById(session, broker, delete_iter.next().longValue());
            if (to_delete != null) {
               records_to_delete.add(to_delete);
               //deleteMyWorkRecord(session, broker, to_delete);
            }
            else {
               logger_.error("Inconsistent State: got null work record (not deleted, skipped)");
            }
         }
      }
      finally {
         old_object_broker.close();
      }
      deleteMyWorkRecords(session, broker, records_to_delete.iterator());
      // }
   }

   public void updateMyWorkSlips(OpProjectSession session, OpBroker broker, Iterator<OpWorkSlip> work_slips)
        throws XServiceException {
      while (work_slips.hasNext()) {
         updateMyWorkSlip(session, broker, work_slips.next());
      }
   }

   /**
    * @param id
    * @return
    * @throws XServiceException
    */

   public OpWorkSlip getMyWorkSlipById(OpProjectSession session, OpBroker broker, long id)
        throws XServiceException {
      OpWorkSlip work_slip = (OpWorkSlip) broker.getObject(OpWorkSlip.class, id);
      if (!session.isUser(work_slip.getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only delete work records from his/her work slips
      }

      return (work_slip);
   }

   public OpWorkSlip getMyWorkSlipByIdString(OpProjectSession session, OpBroker broker, String id_string)
        throws XServiceException {
      OpWorkSlip work_slip = (OpWorkSlip) broker.getObject(id_string);
      if (!session.isUser(work_slip.getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only delete work records from his/her work slips
      }

      return (work_slip);
   }

   public void insertMyWorkRecord(OpProjectSession session, OpBroker broker, OpWorkRecord work_record, OpWorkSlip work_slip)
        throws XServiceException {
      // check and complete all fields
      // check work slip
      logger_.info("insertMyWorkRecord(" + work_record + ")");
      if (work_slip == null) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpWorkError.INCORRECT_WORK_SLIP)));
      }
      work_record.setWorkSlip(work_slip);

      if (!session.isUser(work_record.getWorkSlip().getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only insert work records to his/her work slips
      }

      // check assignment
      if (work_record.getAssignment() == null) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ASSIGNMENT)));
      }

      // Use progress calculator to update progress information in associated project plan
      OpProgressCalculator.addWorkRecord(broker, work_record);

      broker.makePersistent(work_record);
   }

   /**
    * @param session
    * @param broker
    * @param work_records the {@link OpWorkRecord} to insert.
    * @throws XServiceException if any given {@link OpWorkRecord} is of an invalid state, or if one is already existing. Prior records will be inserted.
    */

   public void insertMyWorkRecords(OpProjectSession session, OpBroker broker, Iterator<OpWorkRecord> work_records, OpWorkSlip work_slip)
        throws XServiceException {
      while (work_records.hasNext()) {
         insertMyWorkRecord(session, broker, work_records.next(), work_slip);
      }
   }

   public void deleteMyWorkRecord(OpProjectSession session, OpBroker broker, OpWorkRecord work_record)
        throws XServiceException {
      logger_.info("deleteMyWorkRecord(" + work_record + ")");

      if (!session.isUser(work_record.getWorkSlip().getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only remove work records to his/her work slips
      }

      OpProgressCalculator.removeWorkRecord(broker, work_record);
      broker.deleteObject(work_record);
   }

   public void deleteMyWorkRecords(OpProjectSession session, OpBroker broker, Iterator<OpWorkRecord> work_records)
        throws XServiceException {
      while (work_records.hasNext()) {
         deleteMyWorkRecord(session, broker, work_records.next());
      }
   }

   public void updateMyWorkRecord(OpProjectSession session, OpBroker broker, OpWorkRecord work_record)
        throws XServiceException {
      logger_.info("updateMyWorkRecord(" + work_record + ")");

      if (!session.isUser(work_record.getWorkSlip().getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only remove work records to his/her work slips
      }
      // FIXME(dfreis Mar 22, 2007 8:12:37 AM)
      // should avoid usage of a new Broker (= new Hibernate Session), this works if OpProgressCalculator
      // is triggers within Hibernate Interceptor/EventHandler
      OpBroker old_obj_broker = session.newBroker();
      try {
         OpWorkRecord old_work_record = getMyWorkRecordById(session, old_obj_broker, work_record.getID());
         OpProgressCalculator.removeWorkRecord(broker, old_work_record);
         OpProgressCalculator.addWorkRecord(broker, work_record);
      }
      finally {
         old_obj_broker.close();
      }
      broker.updateObject(work_record);
   }

   public void updateMyWorkRecords(OpProjectSession session, OpBroker broker, Iterator<OpWorkRecord> work_records)
        throws XServiceException {
      while (work_records.hasNext()) {
         updateMyWorkRecord(session, broker, work_records.next());
      }
   }

   /**
    * @param id
    * @return
    * @throws XServiceException
    */
   public OpWorkRecord getMyWorkRecordById(OpProjectSession session, OpBroker broker, long id)
        throws XServiceException {
      OpWorkRecord work_record = (OpWorkRecord) broker.getObject(OpWorkRecord.class, id);
      if (!session.isUser(work_record.getWorkSlip().getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only delete work records from his/her work records
      }

      return (work_record);
   }

   public OpWorkRecord getMyWorkRecordByIdString(OpProjectSession session, OpBroker broker, String id_string)
        throws XServiceException {
      OpWorkRecord work_record = (OpWorkRecord) broker.getObject(id_string);
      if (!session.isUser(work_record.getWorkSlip().getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only delete work records from his/her work records
      }

      return (work_record);
   }

//  /**
//   * Performs validation of the <code>work_record_set</code>
//   *
//   * @param session         the <code>OpProjectSession</code>
//   * @param work_record_set the <code>XComponent.DATA_SET</code> of records
//   * @param insertMode      a <code>boolean</code> indicating whether the operation is insert or edit.
//   * @return <code>XMessage</code> containing a not null <code>XError</code> instance field if validation fails.
//   */
//  private void validateWorkRecordEfforts(OpProjectSession session, OpWorkSlip work_slip, boolean insertMode)
//    throws XServiceException
//  {
//     XComponent data_row;
//     XComponent data_cell;
//     boolean validActualEffort = false;
//     boolean validCosts = false;
//
//     for (int i = 0; i < work_record_set.getChildCount(); i++) {
//        data_row = (XComponent) work_record_set.getChild(i);
//
//        //completed
//        Boolean completedValue = (Boolean) ((XComponent) data_row.getChild(COMPLETED_COLUMN_INDEX)).getValue();
//        if (completedValue != null && completedValue.booleanValue()) {
//           validActualEffort = true;
//        }
//
//        // actual effort
//        data_cell = (XComponent) data_row.getChild(ACTUAL_EFFORT_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           double actualEffort = data_cell.getDoubleValue();
//           if (actualEffort < 0) {
//              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ACTUAL_EFFORT));
//              return reply;
//           }
//           if (actualEffort > 0) {
//              //we found a valid effort
//              validActualEffort = true;
//           }
//        }
//        // Remaining effort
//        data_cell = (XComponent) data_row.getChild(REMAINING_EFFORT_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           if (data_cell.getDoubleValue() < 0) {
//              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_REMAINING_EFFORT));
//              return reply;
//           }
//        }
//        // Material Costs
//        data_cell = (XComponent) data_row.getChild(MATERIAL_COSTS_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           if (data_cell.getDoubleValue() < 0) {
//              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_MATERIAL_COSTS));
//              return reply;
//           }
//           if (data_cell.getDoubleValue() > 0) {
//              validCosts = true;
//           }
//        }
//        // Travel costs
//        data_cell = (XComponent) data_row.getChild(TRAVEL_COSTS_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           if (data_cell.getDoubleValue() < 0) {
//              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_TRAVEL_COSTS));
//              return reply;
//           }
//           if (data_cell.getDoubleValue() > 0) {
//              validCosts = true;
//           }
//        }
//        // External costs
//        data_cell = (XComponent) data_row.getChild(EXTERNAL_COSTS_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           if (data_cell.getDoubleValue() < 0) {
//              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_EXTERNAL_COSTS));
//              return reply;
//           }
//           if (data_cell.getDoubleValue() > 0) {
//              validCosts = true;
//           }
//        }
//        // Miscellaneous Costs
//        data_cell = (XComponent) data_row.getChild(MISCELLANEOUS_COSTS_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           if (data_cell.getDoubleValue() < 0) {
//              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_MISCELLANEOUS_COSTS));
//              return reply;
//           }
//           if (data_cell.getDoubleValue() > 0) {
//              validCosts = true;
//           }
//        }
//     }
//     // a valid effort was not found in the work record set
//     if (!validActualEffort && !validCosts && insertMode) {
//        reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ACTUAL_EFFORT));
//     }
//     else if (!validActualEffort && !validCosts) {
//        //for edit, we just ignore the error...
//        reply.setArgument(WARNING, Boolean.TRUE);
//     }
//     return reply;
//  }

//  /**
//   * Checks if a work record's information has changed or not (determining whether it should be inserted or not).
//   *
//   * @param dataRow    a <code>XComponent</code> representing the client-side work record data.
//   * @param insertMode a <code>boolean</code> indicating whether we are in edit or insert mode.
//   * @return true if the workrecord has changed, false otherwise.
//   */
//  private boolean hasWorkRecordChanged(OpWorkRecord work_record, boolean insertMode) {
//     if (insertMode) {
//        int activityType = work_record.((XComponent) dataRow.getChild(ACTIVITY_TYPE_COLUMN_INDEX)).getIntValue();
//        XComponent dataCell = null;
//        dataCell = (XComponent) dataRow.getChild(MATERIAL_COSTS_COLOMN_INDEX);
//        double cost;
//        boolean zeroCosts = true;
//        if (dataCell.getValue() != null) {
//           cost = dataCell.getDoubleValue();
//           if (cost != 0) {
//              zeroCosts = false;
//           }
//        }
//        dataCell = (XComponent) dataRow.getChild(TRAVEL_COSTS_COLOMN_INDEX);
//        if (dataCell.getValue() != null) {
//           cost = dataCell.getDoubleValue();
//           if (cost != 0) {
//              zeroCosts = false;
//           }
//        }
//        dataCell = (XComponent) dataRow.getChild(EXTERNAL_COSTS_COLOMN_INDEX);
//        if (dataCell.getValue() != null) {
//           cost = dataCell.getDoubleValue();
//           if (cost != 0) {
//              zeroCosts = false;
//           }
//        }
//        dataCell = (XComponent) dataRow.getChild(MISCELLANEOUS_COSTS_COLOMN_INDEX);
//        if (dataCell.getValue() != null) {
//           cost = dataCell.getDoubleValue();
//           if (cost != 0) {
//              zeroCosts = false;
//           }
//        }
//
//
//        switch (activityType) {
//           case OpActivity.ADHOC_TASK:
//           case OpActivity.STANDARD:
//           case OpActivity.TASK: {
//              boolean completed;
//              dataCell = (XComponent) dataRow.getChild(COMPLETED_COLUMN_INDEX);
//              completed = (dataCell.getValue() != null && dataCell.getBooleanValue());
//              double actualEffort = -1;
//              dataCell = (XComponent) dataRow.getChild(ACTUAL_EFFORT_COLOMN_INDEX);
//              if (dataCell.getValue() != null) {
//                 actualEffort = dataCell.getDoubleValue();
//              }
//              if (actualEffort == 0.0 && !completed && zeroCosts) {
//                 return false;
//              }
//              break;
//           }
//           case OpActivity.MILESTONE: {
//              dataCell = (XComponent) dataRow.getChild(COMPLETED_COLUMN_INDEX);
//              if (dataCell.getValue() != null) {
//                 boolean complete = dataCell.getBooleanValue();
//                 if (!complete && zeroCosts) {
//                    return false;
//                 }
//              }
//              break;
//           }
//        }
//     }
//     return true;
//  }

//  /**
//   * Persist <code>OpWorkRecord</code>s entities for the given <code>work_slip</code> resource.
//   *
//   * @param broker          <code>OpBroker</code>
//   * @param work_slip       <code>OpWorkSlip</code>
//   * @param work_record_set <code>XComponent.DATA_SET</code> of work records
//   */
//  protected void insertWorkRecords(OpBroker broker, OpWorkSlip work_slip, XComponent work_record_set) {
//     OpWorkRecord work_record = null;
//     XComponent data_row = null;
//     XComponent data_cell = null;
//     OpAssignment assignment = null;
//     List workRecordsToAdd = new ArrayList();
//
//     for (int i = 0; i < work_record_set.getChildCount(); i++) {
//        data_row = (XComponent) work_record_set.getChild(i);
//
//        //check if the work-record has changed
//        XComponent activityInsertMode = (XComponent) data_row.getChild(ACTIVITY_INSERT_MODE);
//        boolean insert = activityInsertMode.getBooleanValue();
//        if (!hasWorkRecordChanged(data_row, insert)) {
//           continue;
//        }
//
//        //work slip's resource record
//        work_record = new OpWorkRecord();
//        work_record.setWorkSlip(work_slip);
//        data_cell = (XComponent) data_row.getChild(TASK_NAME_COLOMN_INDEX);
//        assignment = (OpAssignment) (broker.getObject(XValidator.choiceID(data_cell.getStringValue())));
//        work_record.setAssignment(assignment);
//
//        //complete
//        data_cell = (XComponent) data_row.getChild(COMPLETED_COLUMN_INDEX);
//        if (data_cell.getValue() != null) {
//           work_record.setCompleted(data_cell.getBooleanValue());
//        }
//
//        // actual effort data cell
//        data_cell = (XComponent) data_row.getChild(ACTUAL_EFFORT_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           work_record.setActualEffort(data_cell.getDoubleValue());
//        }
//        else {
//           work_record.setActualEffort(0);
//        }
//
//        // Remaining effort is more complicated: Store estimation value and value change (for rollback and editing)
//        data_cell = (XComponent) data_row.getChild(REMAINING_EFFORT_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           work_record.setRemainingEffort(data_cell.getDoubleValue());
//
//           // Must be negative if remaining effort is lower than original remaining effort
//           work_record.setRemainingEffortChange(data_cell.getDoubleValue() - assignment.getRemainingEffort());
//        }
//        else {
//           work_record.setRemainingEffort(0);
//           work_record.setRemainingEffortChange(0);
//        }
//
//        // Material Costs data cell
//        data_cell = (XComponent) data_row.getChild(MATERIAL_COSTS_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           work_record.setMaterialCosts(data_cell.getDoubleValue());
//        }
//        else {
//           work_record.setMaterialCosts(0);
//        }
//
//        // Travel costs
//        data_cell = (XComponent) data_row.getChild(TRAVEL_COSTS_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           work_record.setTravelCosts(data_cell.getDoubleValue());
//        }
//        else {
//           work_record.setTravelCosts(0);
//        }
//
//        // External costs
//        data_cell = (XComponent) data_row.getChild(EXTERNAL_COSTS_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           work_record.setExternalCosts(data_cell.getDoubleValue());
//        }
//        else {
//           work_record.setExternalCosts(0);
//        }
//
//        // Miscellaneous Costs
//        data_cell = (XComponent) data_row.getChild(MISCELLANEOUS_COSTS_COLOMN_INDEX);
//        if (data_cell.getValue() != null) {
//           work_record.setMiscellaneousCosts(data_cell.getDoubleValue());
//        }
//        else {
//           work_record.setMiscellaneousCosts(0);
//        }
//
//        // Optional comment
//        data_cell = (XComponent) data_row.getChild(COMMENT_COLOMN_INDEX);
//        if (data_cell.getStringValue() != null) {
//           work_record.setComment(data_cell.getStringValue());
//        }
//
//        // Use progress calculator to update progress information in associated project plan
//        OpProgressCalculator.addWorkRecord(broker, work_record);
//
//        //Set the personnel costs for the work record
//        work_record.setPersonnelCosts(assignment.getResource().getHourlyRate() * work_record.getActualEffort());
//
//        //Because addWorkRecord() performs hibernate queries, we only persist the work records at the end
//        workRecordsToAdd.add(work_record);
//     }
//
//     //register work records with broker (done here because hibernate queries flush the session)
//     Iterator it = workRecordsToAdd.iterator();
//     while (it.hasNext()) {
//        broker.makePersistent((OpWorkRecord) it.next());
//     }
//  }

}
