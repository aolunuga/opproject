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

   public final static OpWorkErrorMap ERROR_MAP = new OpWorkErrorMap();

   private OpWorkServiceImpl serviceImpl_ = new OpWorkServiceImpl();

   public XMessage insertWorkSlip(OpProjectSession session, XMessage request) {
      logger.info("OpWorkService.insertWorkSlip()");
      Date start = (Date) (request.getArgument(START));

      OpWorkSlip workSlip = new OpWorkSlip();
      workSlip.setCreated(XCalendar.today());
      workSlip.setDate(start);

      XComponent workRecordSet = (XComponent) (request.getArgument(WORK_RECORD_SET));
      OpBroker broker = session.newBroker();

      OpWorkRecord workRecord;
      XComponent dataRow;
      XComponent dataCell;
      OpAssignment assignment;
      int errorCode = 0;
      // hashset should be ArraySet
      Set<OpWorkRecord> workRecordsToAdd = new HashSet<OpWorkRecord>(workRecordSet.getChildCount());

      for (int i = 0; i < workRecordSet.getChildCount(); i++) {
         dataRow = (XComponent) workRecordSet.getChild(i);
         //work slip's resource record
         workRecord = new OpWorkRecord();
         dataCell = (XComponent) dataRow.getChild(TASK_NAME_COLOMN_INDEX);
         assignment = (OpAssignment) (broker.getObject(XValidator.choiceID(dataCell.getStringValue())));
         workRecord.setAssignment(assignment);

         //complete
         dataCell = (XComponent) dataRow.getChild(COMPLETED_COLUMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setCompleted(dataCell.getBooleanValue());
         }

         // actual effort data cell
         dataCell = (XComponent) dataRow.getChild(ACTUAL_EFFORT_COLOMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setActualEffort(dataCell.getDoubleValue());
         }

         // Remaining effort is more complicated: Store estimation value and value change (for rollback and editing)
         dataCell = (XComponent) dataRow.getChild(REMAINING_EFFORT_COLOMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setRemainingEffort(dataCell.getDoubleValue());
            // Must be negative if remaining effort is lower than original remaining effort
            workRecord.setRemainingEffortChange(dataCell.getDoubleValue() - assignment.getRemainingEffort());
         }

         // Material Costs data cell
         dataCell = (XComponent) dataRow.getChild(MATERIAL_COSTS_COLOMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setMaterialCosts(dataCell.getDoubleValue());
         }

         // Travel costs
         dataCell = (XComponent) dataRow.getChild(TRAVEL_COSTS_COLOMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setTravelCosts(dataCell.getDoubleValue());
         }

         // External costs
         dataCell = (XComponent) dataRow.getChild(EXTERNAL_COSTS_COLOMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setExternalCosts(dataCell.getDoubleValue());
         }

         // Miscellaneous Costs
         dataCell = (XComponent) dataRow.getChild(MISCELLANEOUS_COSTS_COLOMN_INDEX);
         if (dataCell.getValue() != null) {
            workRecord.setMiscellaneousCosts(dataCell.getDoubleValue());
         }

         // Optional comment
         dataCell = (XComponent) dataRow.getChild(COMMENT_COLOMN_INDEX);
         if (dataCell.getStringValue() != null) {
            workRecord.setComment(dataCell.getStringValue());
         }

         //Set the personnel costs for the work record
         workRecord.setPersonnelCosts(assignment.getResource().getHourlyRate() * workRecord.getActualEffort());

         // Add only if valid
         if (workRecord.isValid() == 0) {
            //Because addWorkRecord() performs hibernate queries, we only persist the work records at the end
            workRecordsToAdd.add(workRecord);
         }
         else {
            // Keep the first error code
            if(errorCode == 0) {
               errorCode = workRecord.isValid();
            }
         }
      }

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
         serviceImpl_.insertMyWorkSlip(session, broker, workSlip);
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

   public XMessage editWorkSlip(OpProjectSession session, XMessage request) {
      logger.info("OpWorkService.editWorkSlip()");

      String workSlipId = (String) (request.getArgument(WORK_SLIP_ID));
      XComponent workRecordSet = (XComponent) (request.getArgument(WORK_RECORD_SET));

      // TODO: Error handling (parameters set)?
      OpBroker broker = session.newBroker();
      OpTransaction t = null;

      try {

         OpWorkSlip workSlip = serviceImpl_.getMyWorkSlipByIdString(session, broker, workSlipId);

         if (workSlip == null) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.WORK_SLIP_NOT_FOUND));
            broker.close();
            return reply;
         }

         XComponent dataRow;
         XComponent dataCell;
         OpAssignment assignment;
         OpWorkRecord workRecord;
         // HashSet should be ArraySet
         Set<OpWorkRecord> workRecordsToAdd = new HashSet<OpWorkRecord>(workRecordSet.getChildCount());

         // strategy: create new records and replace existing ones within OpWorkSlip
         //           call updateMyWorkSlip which updates all records accordingly.

         for (int i = 0; i < workRecordSet.getChildCount(); i++) {
            dataRow = (XComponent) workRecordSet.getChild(i);

            //work slip's resource record
            workRecord = new OpWorkRecord();
            workRecord.setWorkSlip(workSlip);
            assignment = (OpAssignment) (broker.getObject(XValidator.choiceID(dataRow.getStringValue())));
            workRecord.setAssignment(assignment);

            //complete
            dataCell = (XComponent) dataRow.getChild(COMPLETED_COLUMN_INDEX);
            if (dataCell.getValue() != null) {
               workRecord.setCompleted(dataCell.getBooleanValue());
            }

            // actual effort data cell
            dataCell = (XComponent) dataRow.getChild(ACTUAL_EFFORT_COLOMN_INDEX);
            if (dataCell.getValue() != null) {
               workRecord.setActualEffort(dataCell.getDoubleValue());
            }

            // Remaining effort is more complicated: Store estimation value and value change (for rollback and editing)
            dataCell = (XComponent) dataRow.getChild(REMAINING_EFFORT_COLOMN_INDEX);
            if (dataCell.getValue() != null) {
               workRecord.setRemainingEffort(dataCell.getDoubleValue());
               // Must be negative if remaining effort is lower than original remaining effort
               workRecord.setRemainingEffortChange(dataCell.getDoubleValue() - assignment.getRemainingEffort());
            }

            // Material Costs data cell
            dataCell = (XComponent) dataRow.getChild(MATERIAL_COSTS_COLOMN_INDEX);
            if (dataCell.getValue() != null) {
               workRecord.setMaterialCosts(dataCell.getDoubleValue());
            }

            // Travel costs
            dataCell = (XComponent) dataRow.getChild(TRAVEL_COSTS_COLOMN_INDEX);
            if (dataCell.getValue() != null) {
               workRecord.setTravelCosts(dataCell.getDoubleValue());
            }

            // External costs
            dataCell = (XComponent) dataRow.getChild(EXTERNAL_COSTS_COLOMN_INDEX);
            if (dataCell.getValue() != null) {
               workRecord.setExternalCosts(dataCell.getDoubleValue());
            }

            // Miscellaneous Costs
            dataCell = (XComponent) dataRow.getChild(MISCELLANEOUS_COSTS_COLOMN_INDEX);
            if (dataCell.getValue() != null) {
               workRecord.setMiscellaneousCosts(dataCell.getDoubleValue());
            }

            // Optional comment
            dataCell = (XComponent) dataRow.getChild(COMMENT_COLOMN_INDEX);
            if (dataCell.getStringValue() != null) {
               workRecord.setComment(dataCell.getStringValue());
            }

            //Set the personnel costs for the work record
            workRecord.setPersonnelCosts(assignment.getResource().getHourlyRate() * workRecord.getActualEffort());

            if (workRecord.isValid() == 0) {
               //Because addWorkRecord() performs hibernate queries, we only persist the work records at the end
               workRecordsToAdd.add(workRecord);
            }
            else {
               XMessage reply = new XMessage();
               reply.setError(session.newError(ERROR_MAP, workRecord.isValid()));
               return reply;
            }
         }
         t = broker.newTransaction();
         workSlip.setRecords(workRecordsToAdd);

         //validate work record set
         int error = workSlip.isValid();
         if (error != 0) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, error));
            return reply;
         }

         serviceImpl_.updateMyWorkSlip(session, broker, workSlip);
         t.commit();

         logger.info("/OpWorkService.editWorkSlip()");
         return null;
      }
      catch (XServiceException exc) {
         if (t != null) {
            t.rollback();
         }
         XMessage reply = new XMessage();
         exc.append(reply);
         return (reply);
      }
      finally {
         broker.close();
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
         to_delete.add(serviceImpl_.getMyWorkSlipByIdString(session, broker, (String) id_string));
      }

      try {
         serviceImpl_.deleteMyWorkSlips(session, broker, to_delete.iterator());
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
}
