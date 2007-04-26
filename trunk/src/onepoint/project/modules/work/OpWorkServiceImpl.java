/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpConnection;
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

   private static final XLog logger_ = XLogFactory.getServerLogger(OpWorkServiceImpl.class);
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

      //validate work record set
      int error = work_slip.isValid();
      if (error != 0) {
         throw new XServiceException(session.newError(ERROR_MAP, error));
      }

      OpUser user = session.user(broker);
      
      // check for duplicate work slip date   

      // allow queries to return stale state
      int mode = broker.getConnection().getFlushMode();
      broker.getConnection().setFlushMode(OpConnection.FLUSH_MODE_COMMIT);
      OpQuery query = broker.newQuery("select work_slip from OpWorkSlip as work_slip where work_slip.Creator.ID = ? and work_slip.Date = ?");
      query.setLong(0, user.getID());
      query.setDate(1, work_slip.getDate());
      Iterator it = broker.iterate(query);
      if (it.hasNext()) {
         throw new XServiceException(session.newError(ERROR_MAP, OpWorkError.DUPLICATE_DATE));
      } 
      // set flush mode back again
      broker.getConnection().setFlushMode(mode);
    
      Integer max = new Integer(0);
      query = broker.newQuery("select max(work_slip.Number) from OpWorkSlip as work_slip where work_slip.Creator.ID = ?");
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

      // make work slip persistent
      broker.makePersistent(work_slip);

      // Inserts work-records into database and add to progress calculator
      insertMyWorkRecords(session, broker, work_slip.getRecords().iterator(), work_slip);
	    broker.getConnection().flush(); // required to ensure ConstraintViolationException!
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
      int error = work_slip.isValid();
      if (error != 0) {
         throw new XServiceException(session.newError(ERROR_MAP, error), true);
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

   private void insertMyWorkRecord(OpProjectSession session, OpBroker broker, OpWorkRecord work_record, OpWorkSlip work_slip)
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

   private void insertMyWorkRecords(OpProjectSession session, OpBroker broker, Iterator<OpWorkRecord> work_records, OpWorkSlip work_slip)
        throws XServiceException {
      while (work_records.hasNext()) {
         insertMyWorkRecord(session, broker, work_records.next(), work_slip);
      }
   }

   private void deleteMyWorkRecord(OpProjectSession session, OpBroker broker, OpWorkRecord work_record)
        throws XServiceException {
      logger_.info("deleteMyWorkRecord(" + work_record + ")");

//      if (!session.isUser(work_record.getWorkSlip().getCreator()) && (!session.userIsAdministrator())) {
//         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
//              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only remove work records to his/her work slips
//      }

      OpProgressCalculator.removeWorkRecord(broker, work_record);
      broker.deleteObject(work_record);
   }

   private void deleteMyWorkRecords(OpProjectSession session, OpBroker broker, Iterator<OpWorkRecord> work_records)
        throws XServiceException {
      while (work_records.hasNext()) {
         deleteMyWorkRecord(session, broker, work_records.next());
      }
   }

   private void updateMyWorkRecord(OpProjectSession session, OpBroker broker, OpWorkRecord work_record)
        throws XServiceException {
      logger_.info("updateMyWorkRecord(" + work_record + ")");

//      if (!session.isUser(work_record.getWorkSlip().getCreator()) && (!session.userIsAdministrator())) {
//         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
//              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only remove work records to his/her work slips
//      }
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

   private void updateMyWorkRecords(OpProjectSession session, OpBroker broker, Iterator<OpWorkRecord> work_records)
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
   private OpWorkRecord getMyWorkRecordById(OpProjectSession session, OpBroker broker, long id)
        throws XServiceException {
      OpWorkRecord work_record = (OpWorkRecord) broker.getObject(OpWorkRecord.class, id);
      if (!session.isUser(work_record.getWorkSlip().getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only delete work records from his/her work records
      }

      return (work_record);
   }

   private OpWorkRecord getMyWorkRecordByIdString(OpProjectSession session, OpBroker broker, String id_string)
        throws XServiceException {
      OpWorkRecord work_record = (OpWorkRecord) broker.getObject(id_string);
      if (!session.isUser(work_record.getWorkSlip().getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only delete work records from his/her work records
      }

      return (work_record);
   }
}
