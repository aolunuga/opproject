/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpAssignment;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceException;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class OpWorkService extends OpProjectService {

   private static final XLog logger = XLogFactory.getServerLogger(OpWorkService.class);

   public final static String START = "start";
   public final static String WORK_RECORD_SET = "work_record_set";
   public final static String WORK_SLIP_ID = "work_slip_id";
   public final static String WORK_SLIP_IDS = "work_slip_ids";

   //work record data column indexes
   private final int ACTUAL_EFFORT_COLUMN_INDEX = 1;
   private final int REMAINING_EFFORT_COLUMN_INDEX = 2;
   private final int MATERIAL_COSTS_COLUMN_INDEX = 3;
   private final int TRAVEL_COSTS_COLUMN_INDEX = 4;
   private final int EXTERNAL_COSTS_COLUMN_INDEX = 5;
   private final int MISCELLANEOUS_COSTS_COLUMN_INDEX = 6;
   private final int COMMENT_COLUMN_INDEX = 7;
   private final int COMPLETED_COLUMN_INDEX = 10;

   public final static OpWorkErrorMap ERROR_MAP = new OpWorkErrorMap();

   private OpWorkServiceImpl serviceImpl = new OpWorkServiceImpl();

   public XMessage insertWorkSlip(OpProjectSession session, XMessage request) {
      logger.info("OpWorkService.insertWorkSlip()");
      Date start = (Date) (request.getArgument(START));

      OpWorkSlip workSlip = new OpWorkSlip();
      workSlip.setCreated(XCalendar.today());
      workSlip.setDate(start);

      XComponent workRecordSet = (XComponent) (request.getArgument(WORK_RECORD_SET));
      OpBroker broker = session.newBroker();

      // hashset should be ArraySet
      Set<OpWorkRecord> workRecordsToAdd = new HashSet<OpWorkRecord>(workRecordSet.getChildCount());

      int errorCode = createWorkRecords(workRecordSet, broker, workRecordsToAdd);

      // if no works records are valid throw an exception
      if (workRecordsToAdd.isEmpty() && errorCode != 0) {
         XMessage reply = new XMessage();
         reply.setError(session.newError(ERROR_MAP, errorCode));
         return reply;
      }

      OpTransaction t = broker.newTransaction();
      workSlip.setRecords(workRecordsToAdd);

      try {
         // note: second one is required in order to correct OpProgressCalculator only! (would be better to notify OpProgressCalculator on any changes - PropertyChangeEvent?)
         serviceImpl.insertMyWorkSlip(session, broker, workSlip);
         t.commit();
      }
      catch (XServiceException exc) {
         t.rollback();
         XMessage reply = new XMessage();
         reply.setError(exc.getError());
         return reply;
      }
      finally {
         broker.close();
      }

      logger.info("/OpWorkService.insertWorkSlip()");
      return null;
   }

   /**
    * Creates a set of work records using the information given in the workRecord data set.
    * Will collect the work records in the given workRecords set.
    *
    * @param workRecordSet data set component with the work records information
    * @param broker        Broker used for db access
    * @param workRecords   set that will contain the created works records
    * @return the first error message returned by the work record validation method
    */
   private int createWorkRecords(XComponent workRecordSet, OpBroker broker, Set<OpWorkRecord> workRecords) {
      XComponent dataRow;
      OpWorkRecord workRecord;
      XComponent dataCell;
      OpAssignment assignment;
      int errorCode = 0;
      for (int i = 0; i < workRecordSet.getChildCount(); i++) {
         dataRow = (XComponent) workRecordSet.getChild(i);
         //work slip's resource record
         workRecord = new OpWorkRecord();
         assignment = (OpAssignment) (broker.getObject(XValidator.choiceID(dataRow.getStringValue())));
         workRecord.setAssignment(assignment);

         //complete
         dataCell = (XComponent) dataRow.getChild(COMPLETED_COLUMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setCompleted(dataCell.getBooleanValue());
         }

         // actual effort data cell
         dataCell = (XComponent) dataRow.getChild(ACTUAL_EFFORT_COLUMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setActualEffort(dataCell.getDoubleValue());
         }

         // Remaining effort is more complicated: Store estimation value and value change (for rollback and editing)
         dataCell = (XComponent) dataRow.getChild(REMAINING_EFFORT_COLUMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setRemainingEffort(dataCell.getDoubleValue());
            // Must be negative if remaining effort is lower than original remaining effort
            workRecord.setRemainingEffortChange(dataCell.getDoubleValue() - assignment.getRemainingEffort());
         }

         // Material Costs data cell
         dataCell = (XComponent) dataRow.getChild(MATERIAL_COSTS_COLUMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setMaterialCosts(dataCell.getDoubleValue());
         }

         // Travel costs
         dataCell = (XComponent) dataRow.getChild(TRAVEL_COSTS_COLUMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setTravelCosts(dataCell.getDoubleValue());
         }

         // External costs
         dataCell = (XComponent) dataRow.getChild(EXTERNAL_COSTS_COLUMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setExternalCosts(dataCell.getDoubleValue());
         }

         // Miscellaneous Costs
         dataCell = (XComponent) dataRow.getChild(MISCELLANEOUS_COSTS_COLUMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setMiscellaneousCosts(dataCell.getDoubleValue());
         }

         // Optional comment
         dataCell = (XComponent) dataRow.getChild(COMMENT_COLUMN_INDEX);
         if (dataCell.getStringValue() != null) {
            workRecord.setComment(dataCell.getStringValue());
         }

         //Set the personnel costs for the work record
         workRecord.setPersonnelCosts(assignment.getResource().getHourlyRate() * workRecord.getActualEffort());

         // Add only if valid
         if (workRecord.isValid() == 0) {
            //Because addWorkRecord() performs hibernate queries, we only persist the work records at the end
            workRecords.add(workRecord);
         }
         else {
            // Keep the first error code
            if (errorCode == 0) {
               errorCode = workRecord.isValid();
            }
         }
      }
      return errorCode;
   }

   public XMessage editWorkSlip(OpProjectSession session, XMessage request) {
      logger.info("OpWorkService.editWorkSlip()");

      String workSlipId = (String) (request.getArgument(WORK_SLIP_ID));
      XComponent workRecordSet = (XComponent) (request.getArgument(WORK_RECORD_SET));

      // TODO: Error handling (parameters set)?
      OpBroker broker = session.newBroker();
      OpTransaction t = null;

      try {

         OpWorkSlip workSlip = serviceImpl.getMyWorkSlipByIdString(session, broker, workSlipId);

         if (workSlip == null) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.WORK_SLIP_NOT_FOUND));
            broker.close();
            return reply;
         }

         // HashSet should be ArraySet
         Set<OpWorkRecord> workRecordsToAdd = new HashSet<OpWorkRecord>(workRecordSet.getChildCount());

         t = broker.newTransaction();

         //remove all work records from the work slip
         serviceImpl.deleteWorkRecords(broker, workSlip);

         //insert all the new work records
         int errorCode = createWorkRecords(workRecordSet, broker, workRecordsToAdd);
         if (workRecordsToAdd.isEmpty() && errorCode != 0) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, errorCode));
            finalizeSession(t, broker);
            return reply;
         }

         workSlip.setRecords(workRecordsToAdd);
         serviceImpl.insertWorkRecords(session, broker, workRecordsToAdd.iterator(), workSlip);

         //validate work record set
         int error = workSlip.isValid();
         if (error != 0) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, error));
            finalizeSession(t, broker);
            return reply;
         }

         t.commit();

         logger.info("/OpWorkService.editWorkSlip()");
         return null;
      }
      catch (XServiceException exc) {
         finalizeSession(t, broker);
         XMessage reply = new XMessage();
         return exc.append(reply);
      }
   }

   public XMessage deleteWorkSlips(OpProjectSession session, XMessage request)
        throws XServiceException {
      ArrayList id_strings = (ArrayList) (request.getArgument(WORK_SLIP_IDS));
      logger.info("OpWorkService.deleteWorkSlip(): workslip_ids = " + id_strings);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      LinkedList<OpWorkSlip> to_delete = new LinkedList<OpWorkSlip>();
      for (Object id_string : id_strings) {
         to_delete.add(serviceImpl.getMyWorkSlipByIdString(session, broker, (String) id_string));
      }

      try {
         serviceImpl.deleteMyWorkSlips(session, broker, to_delete.iterator());
         t.commit();
      }
      catch (XServiceException exc) {
         t.rollback();
         XMessage reply = new XMessage();
         exc.append(reply);
         return (reply);
      }
      finally {
         broker.close();
      }

      logger.info("/OpWorkService.deleteWorkSlip()");
      return null;
   }

   /* (non-Javadoc)
    * @see onepoint.project.OpProjectService#getServiceImpl()
    */
   @Override
   public Object getServiceImpl() {
      return serviceImpl;
   }

}
