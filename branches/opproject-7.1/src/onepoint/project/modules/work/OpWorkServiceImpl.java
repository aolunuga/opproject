/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpConnection;
import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.OpService;
import onepoint.project.modules.project.OpAttachmentDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserError;
import onepoint.project.modules.user.OpUserServiceImpl;
import onepoint.service.server.XServiceException;
import onepoint.util.XCalendar;

import java.util.Iterator;
import java.util.Set;

/**
 * @author dfreis
 */
public class OpWorkServiceImpl implements OpService {

   /**
    * The name of this service.
    */
   public static final String SERVICE_NAME = "UserService";
   private static final XLog logger = XLogFactory.getServerLogger(OpWorkServiceImpl.class);
   public final static OpWorkErrorMap ERROR_MAP = new OpWorkErrorMap();

   /**
    * Puts the given <code>work_slip</code> to the users list of work sets.
    * If a work slip identified by <code>work_slip.getCreator()</code> and
    * <code>work_slip.getNumber()</code> already exists an XServiceException will be thrown.
    *
    * @param work_slip the Map of attributes representing the work slip
    * @param session
    * @param broker
    * @throws XServiceException        the given {@link OpWorkSlip} is of an invalid state, or the given {@link OpWorkSlip} already exists.
    * @throws IllegalArgumentException if a required attribute is missing. In this case the work slip will not be added.
    */

   public void insertMyWorkSlip(OpProjectSession session, OpBroker broker, OpWorkSlip work_slip)
        throws XServiceException {
      if (work_slip.getCreator() == null) {
         work_slip.setCreator(session.user(broker));
      }

      //validate work record set
      try {
         work_slip.validate();
      }
      catch (OpEntityException e) {
         throw new XServiceException(session.newError(ERROR_MAP, e.getErrorCode()));
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

      Integer max;
      query = broker.newQuery("select max(work_slip.Number) from OpWorkSlip as work_slip where work_slip.Creator.ID = ?");
      query.setLong(0, user.getID());
      max = (Integer) (broker.iterate(query).next());
      if (max == null) {
         max = 0;
      }

      // set flush mode back again
      broker.getConnection().setFlushMode(mode);

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
      work_slip.setNumber(max + 1);
      work_slip.setCreator(user);

      // make work slip persistent
      broker.makePersistent(work_slip);

      // Inserts work-records into database and add to progress calculator
      insertWorkRecords(session, broker, work_slip.getRecords().iterator(), work_slip);
      broker.getConnection().flush(); // required to ensure ConstraintViolationException!
   }

   /**
    * Puts the given wss[] to my list of work sets.
    * If a work slip identified by <code>wss[pos].getCreator()</code> and
    * <code>wss[pos].getNumber()</code> already exists an XServiceException will be thrown. In this case no {@link OpWorkSlip} will be added.
    * The behavior of this operation is unspecified if the specified map is modified while
    * the operation is in progress.
    *
    * @param wss     the {@link OpWorkSlip} that is to be added.
    * @param session
    * @param broker
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
    * @param session
    * @param broker
    * @throws XServiceException        if the given {@link OpWorkSlip} is of an invalid state. In this the {@link OpWorkSlip} will not be removed.
    * @throws IllegalArgumentException if a required attribute is missing or if an attribute is of an invalid type. In this case NO work slip will be added.
    */

   public void deleteMyWorkSlip(OpProjectSession session, OpBroker broker, OpWorkSlip work_slip)
        throws XServiceException {

      if (!session.isUser(work_slip.getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only delete work records from his/her work slips
      }

      // Use progress calculator to update progress information in associated project plan
      OpProgressCalculator.removeWorkRecords(broker, work_slip.getRecords().iterator(), session.getCalendar());

      //manage the contentes of all the attachments that belong (idirectly) to the work slip
      for (OpWorkRecord workRecord : work_slip.getRecords()) {
         for (OpCostRecord costRecord : workRecord.getCostRecords()) {
            OpAttachmentDataSetFactory.removeContents(broker, costRecord.getAttachments());
         }
      }
      broker.deleteObject(work_slip);
   }

   /**
    * Removed the given work slips. If a given {@link OpWorkSlip} was not registered, it will be ignored
    * and therefor not removed.
    *
    * @param wss     the {@link OpWorkSlip}s to delete.
    * @param broker
    * @param session
    * @throws XServiceException        if any given {@link OpWorkSlip} is of an invalid state. In this case all
    *                                  previous {@link OpWorkSlip} will be removed.
    * @throws IllegalArgumentException if a required attribute is missing. In this case NO work slip will be added.
    */

   public void deleteMyWorkSlips(OpProjectSession session, OpBroker broker, Iterator<OpWorkSlip> wss)
        throws XServiceException {
      // *** More error handling needed (check mandatory fields)
      while (wss.hasNext()) {
         deleteMyWorkSlip(session, broker, wss.next());
      }
   }

   /**
    * @param id
    * @param session
    * @param broker
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
      logger.info("insertMyWorkRecord(" + work_record + ")");
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
      OpProgressCalculator.addWorkRecord(broker, work_record, session.getCalendar());

      broker.makePersistent(work_record);
   }

   /**
    * @param session
    * @param broker
    * @param work_records the {@link OpWorkRecord} to insert.
    * @param work_slip
    * @throws XServiceException if any given {@link OpWorkRecord} is of an invalid state, or if one is already existing. Prior records will be inserted.
    */
   public void insertWorkRecords(OpProjectSession session, OpBroker broker, Iterator<OpWorkRecord> work_records, OpWorkSlip work_slip)
        throws XServiceException {
      while (work_records.hasNext()) {
         insertMyWorkRecord(session, broker, work_records.next(), work_slip);
      }
   }

   private void deleteMyWorkRecord(OpBroker broker, OpWorkRecord work_record, XCalendar calendar)
        throws XServiceException {
      logger.info("deleteMyWorkRecord(" + work_record + ")");
      OpProgressCalculator.removeWorkRecord(broker, work_record, calendar);
      broker.deleteObject(work_record);
   }


   /* (non-Javadoc)
   * @see onepoint.project.OpService#getName()
   */
   public String getName() {
      return SERVICE_NAME;
   }

   public void deleteWorkRecords(OpBroker broker, OpWorkSlip workSlip, XCalendar calendar) {

      for (Iterator<OpWorkRecord> iterator = workSlip.getRecords().iterator(); iterator.hasNext();) {
         OpWorkRecord opWorkRecord = iterator.next();
         iterator.remove();
         deleteMyWorkRecord(broker, opWorkRecord, calendar);
      }
      broker.updateObject(workSlip);
   }

}
