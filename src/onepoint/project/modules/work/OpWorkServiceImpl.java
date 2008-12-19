/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpConnection;
import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.OpService;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpAttachmentDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserError;
import onepoint.project.modules.user.OpUserServiceImpl;
import onepoint.service.server.XServiceException;

/**
 * @author dfreis
 */
public class OpWorkServiceImpl implements OpService {

   /**
    * The name of this service.
    */
   public static final String SERVICE_NAME = "WorkService";
   private static final XLog logger = XLogFactory.getLogger(OpWorkServiceImpl.class);

   private final static String SELECT_WORK_SLIP_BY_USER_AND_DATE = "select workslip from OpWorkSlip as workslip where workslip.Creator.id = ? and workslip.Date = ?";

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
      try {
         OpQuery query = broker.newQuery("select work_slip from OpWorkSlip as work_slip where work_slip.Creator.id = ? and work_slip.Date = ?");
         query.setLong(0, user.getId());
         query.setDate(1, work_slip.getDate());
         Iterator it = broker.iterate(query);
         if (it.hasNext()) {
            throw new XServiceException(session.newError(ERROR_MAP, OpWorkError.DUPLICATE_DATE));
         }
      } 
      finally {
         // set flush mode back again
         broker.getConnection().setFlushMode(mode);
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
      OpProgressCalculator.removeWorkRecords(session, broker, work_slip.getRecords().iterator());

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
    * @param session
    * @param broker
    * @param user
    * @param date
    * @return
    * @pre
    * @post
    */
   public OpWorkSlip getWorkSlip(OpProjectSession session, OpBroker broker, OpUser user, Date date) {
      OpQuery query = broker.newQuery(SELECT_WORK_SLIP_BY_USER_AND_DATE);
      
      Calendar c = new GregorianCalendar();
      c.setTime(date);
      c.set(Calendar.HOUR, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      
      java.sql.Date sqlDate = new java.sql.Date(c.getTimeInMillis());
      query.setLong(0, user.getId());
      query.setDate(1, sqlDate);
      
      Iterator iter = broker.iterate(query);
      if (iter.hasNext()) {
         return (OpWorkSlip) iter.next();
      }
      return null;
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

   public void insertMyWorkRecord(OpProjectSession session, OpBroker broker,
         OpWorkRecord work_record, OpWorkSlip work_slip)
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
      OpAssignment assignment = work_record.getAssignment();
      OpProgressCalculator.addWorkRecord(session, broker, work_record, assignment);

      OpProjectNode projectNode = work_record.getAssignment().getActivity().getProjectPlan().getProjectNode();
      Set<OpWorkRecord> records = work_record.getWorkSlip().getRecords();
      if (records != null) {
    	  for (OpWorkRecord record : records) {
    		  Set<OpCostRecord> costRecs = record.getCostRecords();
    		  if (costRecs != null) {
    			  for (OpCostRecord costRec : costRecs) {
    				Set<OpAttachment> atts = costRec.getAttachments();
    				if (atts != null) {
    					for (OpAttachment att : atts) {
							OpPermissionDataSetFactory.updatePermissions(broker, projectNode, att);
    					}
    				}
    			  }
    		  }
    	  }
      }

      broker.makePersistent(work_record);
   }

   /**
    * @param session
    * @param broker
    * @param work_records the {@link OpWorkRecord} to insert.
    * @param work_slip
    * @throws XServiceException if any given {@link OpWorkRecord} is of an invalid state, or if one is already existing. Prior records will be inserted.
    */
   public void insertMyWorkRecords(OpProjectSession session, OpBroker broker,
         Iterator<OpWorkRecord> work_records, OpWorkSlip work_slip)
        throws XServiceException {
      while (work_records.hasNext()) {
         OpWorkRecord opWorkRecord =  work_records.next();
         insertMyWorkRecord(session, broker, opWorkRecord, work_slip);
      }
   }

   /**
    * @param session
    * @param broker
    * @param workRecord   
    * @pre
    * @post
    */
   public void updateMyWorkRecord(OpProjectSession session, OpBroker broker, OpWorkRecord workRecord) {
      // check and complete all fields
      // check work slip
      logger.info("updateMyWorkRecord(" + workRecord + ")");

      if (!session.isUser(workRecord.getWorkSlip().getCreator()) && (!session.userIsAdministrator())) {
         throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP,
              OpUserError.INSUFFICIENT_PRIVILEGES)); // user may only insert work records to his/her work slips
      }

      // check assignment
      if (workRecord.getAssignment() == null) {
         throw (new XServiceException(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ASSIGNMENT)));
      }

      // Use progress calculator to update progress information in associated project plan
      //workRecord.get
      OpAssignment ass = workRecord.getAssignment();
      OpProgressCalculator.addWorkRecord(session, broker, workRecord, ass);

   }


   public void deleteMyWorkRecord(OpProjectSession session, OpBroker broker, OpWorkRecord work_record)
        throws XServiceException {
      logger.info("deleteMyWorkRecord(" + work_record + ")");
      OpProgressCalculator.removeWorkRecord(session, broker, work_record);
      broker.deleteObject(work_record);
   }


   /* (non-Javadoc)
   * @see onepoint.project.OpService#getName()
   */
   public String getName() {
      return SERVICE_NAME;
   }

   public void deleteWorkRecords(OpProjectSession session, OpBroker broker, OpWorkSlip workSlip) {

      for (Iterator<OpWorkRecord> iterator = workSlip.getRecords().iterator(); iterator.hasNext();) {
         OpWorkRecord opWorkRecord = iterator.next();
         iterator.remove();
         deleteMyWorkRecord(session, broker, opWorkRecord);
      }
      broker.updateObject(workSlip);
   }

}
