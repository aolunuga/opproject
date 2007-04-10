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

   private OpWorkServiceImpl serviceImpl_ =  new OpWorkServiceImpl();

   public XMessage insertWorkSlip(OpProjectSession session, XMessage request) {
      logger.info("OpWorkService.insertWorkSlip()");
      Date start = (Date) (request.getArgument(START));

      OpWorkSlip work_slip = new OpWorkSlip();
      //      work_slip.setNumber(max.intValue() + 1)
//      work_slip.setCreator(user);
      work_slip.setCreated(XCalendar.today());
      work_slip.setDate(start);

      XComponent workRecordSet = (XComponent) (request.getArgument(WORK_RECORD_SET));
      OpBroker broker = session.newBroker();

      OpWorkRecord work_record = null;
      XComponent data_row = null;
      XComponent data_cell = null;
      OpAssignment assignment = null;
      // hashset should be ArraySet
      Set<OpWorkRecord>  workRecordsToAdd = new HashSet<OpWorkRecord>(workRecordSet.getChildCount());
 
      for (int i = 0; i < workRecordSet.getChildCount(); i++) {
        data_row = (XComponent) workRecordSet.getChild(i);

        //check if the work-record has changed
//      XComponent activityInsertMode = (XComponent) data_row.getChild(ACTIVITY_INSERT_MODE);
//      boolean insert = activityInsertMode.getBooleanValue();
//      if (!hasWorkRecordChanged(data_row, insert)) {
//      continue;
//      }

        //work slip's resource record
        work_record = new OpWorkRecord();
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
          try
          {
            work_record.setActualEffort(data_cell.getDoubleValue());
          } catch (IllegalArgumentException exc)
          {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ACTUAL_EFFORT));
            return(reply);
          }
        }

        // Remaining effort is more complicated: Store estimation value and value change (for rollback and editing)
        data_cell = (XComponent) data_row.getChild(REMAINING_EFFORT_COLOMN_INDEX);
        if (data_cell.getValue() != null) {
          try
          {
            work_record.setRemainingEffort(data_cell.getDoubleValue());
            // Must be negative if remaining effort is lower than original remaining effort
            work_record.setRemainingEffortChange(data_cell.getDoubleValue() - assignment.getRemainingEffort());
          } catch (IllegalArgumentException exc)
          {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_REMAINING_EFFORT));
            return(reply);
          }
        }

        // Material Costs data cell
        data_cell = (XComponent) data_row.getChild(MATERIAL_COSTS_COLOMN_INDEX);
        if (data_cell.getValue() != null) {
          try {
            work_record.setMaterialCosts(data_cell.getDoubleValue());
          } catch (IllegalArgumentException exc)
          {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_MATERIAL_COSTS));
            return(reply);
          }
        }

        // Travel costs
        data_cell = (XComponent) data_row.getChild(TRAVEL_COSTS_COLOMN_INDEX);
        if (data_cell.getValue() != null) {
          try {
            work_record.setTravelCosts(data_cell.getDoubleValue());
          } catch (IllegalArgumentException exc)
          {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_TRAVEL_COSTS));
            return(reply);
          }
        }

        // External costs
        data_cell = (XComponent) data_row.getChild(EXTERNAL_COSTS_COLOMN_INDEX);
        if (data_cell.getValue() != null) {
          try {
            work_record.setExternalCosts(data_cell.getDoubleValue());
          } catch (IllegalArgumentException exc)
          {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_EXTERNAL_COSTS));
            return(reply);
          }
        }

        // Miscellaneous Costs
        data_cell = (XComponent) data_row.getChild(MISCELLANEOUS_COSTS_COLOMN_INDEX);
        if (data_cell.getValue() != null) {
          try {
            work_record.setMiscellaneousCosts(data_cell.getDoubleValue());
          } catch (IllegalArgumentException exc)
          {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_MISCELLANEOUS_COSTS));
            return(reply);
          }
        }

        // Optional comment
        data_cell = (XComponent) data_row.getChild(COMMENT_COLOMN_INDEX);
        if (data_cell.getStringValue() != null) {
          work_record.setComment(data_cell.getStringValue());
        }

        //Set the personnel costs for the work record
        work_record.setPersonnelCosts(assignment.getResource().getHourlyRate() * work_record.getActualEffort());
        //Because addWorkRecord() performs hibernate queries, we only persist the work records at the end
        workRecordsToAdd.add(work_record);
      }

      //register work records with broker (done here because hibernate queries flush the session)
//    Iterator it = workRecordsToAdd.iterator();
//    while (it.hasNext()) {
//    broker.makePersistent((OpWorkRecord) it.next());
//    }
      
      OpTransaction t = broker.newTransaction();      
      work_slip.setRecords(workRecordsToAdd);

      //validate work record set
      if (!work_slip.isValid()) {
        XMessage reply = new XMessage();
        reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ACTUAL_EFFORT));
        return(reply);
      }
      
      try
      {
        // note: second one is required in order to correct OpProgressCalculator only! (would be better to notify OpProgressCalculator on any changes - PropertyChangeEvent?)
        serviceImpl_.insertMyWorkSlip(session, broker, work_slip);
        t.commit();
      } catch (XServiceException exc) {
        t.rollback();
        XMessage reply = new XMessage();
        reply.setError(exc.getError());
        return(reply);
      } finally {
        broker.close();
      }
      
      logger.info("/OpWorkService.insertWorkSlip()");
      return null;
   }

   public XMessage editWorkSlip(OpProjectSession session, XMessage request) {
      logger.info("OpWorkService.editWorkSlip()");

      String work_slip_id = (String) (request.getArgument(WORK_SLIP_ID));
      XComponent work_record_set = (XComponent) (request.getArgument(WORK_RECORD_SET));

//      //validate work record set
//      XMessage reply = validateWorkRecordEfforts(session, work_record_set, false);
//      if (reply.getError() != null || reply.getArgument(WARNING) != null) {
//         return reply;
//      }
      // TODO: Error handling (parameters set)?
      OpBroker broker = session.newBroker();
      OpTransaction t = null;

      try
      {
        
        OpWorkSlip work_slip = serviceImpl_.getMyWorkSlipByIdString(session, broker, work_slip_id);

        if (work_slip == null) {
          XMessage reply = new XMessage();
          reply.setError(session.newError(ERROR_MAP, OpWorkError.WORK_SLIP_NOT_FOUND));
          broker.close();
          return reply;        
        }
        
        XComponent data_row;
        XComponent data_cell;
        OpAssignment assignment;
        OpWorkRecord work_record;
        // HashSet should be ArraySet
        Set<OpWorkRecord>  workRecordsToAdd = new HashSet<OpWorkRecord>(work_record_set.getChildCount());

        // strategy: create new records and replace existing ones within OpWorkSlip
        //           call updateMyWorkSlip which updates all records accordingly.
        
        for (int i = 0; i < work_record_set.getChildCount(); i++) {
          data_row = (XComponent) work_record_set.getChild(i);

          //work slip's resource record
          work_record = new OpWorkRecord();
          work_record.setWorkSlip(work_slip);
          assignment = (OpAssignment) (broker.getObject(XValidator.choiceID(data_row.getStringValue())));           
          work_record.setAssignment(assignment);

          //complete
          data_cell = (XComponent) data_row.getChild(COMPLETED_COLUMN_INDEX);
          if (data_cell.getValue() != null) {
             work_record.setCompleted(data_cell.getBooleanValue());
          }

          // actual effort data cell
          data_cell = (XComponent) data_row.getChild(ACTUAL_EFFORT_COLOMN_INDEX);
          if (data_cell.getValue() != null) {
            try
            {
              work_record.setActualEffort(data_cell.getDoubleValue());
            } catch (IllegalArgumentException exc)
            {
              XMessage reply = new XMessage();
              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ACTUAL_EFFORT));
              return(reply);
            }
          }

          // Remaining effort is more complicated: Store estimation value and value change (for rollback and editing)
          data_cell = (XComponent) data_row.getChild(REMAINING_EFFORT_COLOMN_INDEX);
          if (data_cell.getValue() != null) {
            try {
              work_record.setRemainingEffort(data_cell.getDoubleValue());

              // Must be negative if remaining effort is lower than original remaining effort
              work_record.setRemainingEffortChange(data_cell.getDoubleValue() - assignment.getRemainingEffort());
            } catch (IllegalArgumentException exc)
            {
              XMessage reply = new XMessage();
              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_REMAINING_EFFORT));
              return(reply);
            }   
          }

          // Material Costs data cell
          data_cell = (XComponent) data_row.getChild(MATERIAL_COSTS_COLOMN_INDEX);
          if (data_cell.getValue() != null) {
            try {
              work_record.setMaterialCosts(data_cell.getDoubleValue());
            } catch (IllegalArgumentException exc)
            {
              XMessage reply = new XMessage();
              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_MATERIAL_COSTS));
              return(reply);
            }
          }

          // Travel costs
          data_cell = (XComponent) data_row.getChild(TRAVEL_COSTS_COLOMN_INDEX);
          if (data_cell.getValue() != null) {
            try {
              work_record.setTravelCosts(data_cell.getDoubleValue());
            } catch (IllegalArgumentException exc)
            {
              XMessage reply = new XMessage();
              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_TRAVEL_COSTS));
              return(reply);
            }
          }

          // External costs
          data_cell = (XComponent) data_row.getChild(EXTERNAL_COSTS_COLOMN_INDEX);
          if (data_cell.getValue() != null) {
            try {
              work_record.setExternalCosts(data_cell.getDoubleValue());
            } catch (IllegalArgumentException exc)
            {
              XMessage reply = new XMessage();
              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_EXTERNAL_COSTS));
              return(reply);
            }
          }

          // Miscellaneous Costs
          data_cell = (XComponent) data_row.getChild(MISCELLANEOUS_COSTS_COLOMN_INDEX);
          if (data_cell.getValue() != null) {
            try {
              work_record.setMiscellaneousCosts(data_cell.getDoubleValue());
            } catch (IllegalArgumentException exc)
            {
              XMessage reply = new XMessage();
              reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_MISCELLANEOUS_COSTS));
              return(reply);
            }
          }

          // Optional comment
          data_cell = (XComponent) data_row.getChild(COMMENT_COLOMN_INDEX);
          if (data_cell.getStringValue() != null) {
             work_record.setComment(data_cell.getStringValue());
          }

          //Set the personnel costs for the work record
          work_record.setPersonnelCosts(assignment.getResource().getHourlyRate() * work_record.getActualEffort());

          //Because addWorkRecord() performs hibernate queries, we only persist the work records at the end
          workRecordsToAdd.add(work_record);
       }
       t = broker.newTransaction();      
       work_slip.setRecords(workRecordsToAdd);
       //validate work record set
       if (!work_slip.isValid()) {
         XMessage reply = new XMessage();
         reply.setError(session.newError(ERROR_MAP, OpWorkError.INCORRECT_ACTUAL_EFFORT));
         return(reply);
       }
       
       serviceImpl_.updateMyWorkSlip(session, broker, work_slip);
       t.commit();
       
       logger.info("/OpWorkService.editWorkSlip()");
       return null;

      } catch (XServiceException exc)
      {
        if (t != null)
          t.rollback(); 
        XMessage reply = new XMessage();
        exc.append(reply);
        return (reply);
      } finally {
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
      for (int i = 0; i < id_strings.size(); i++) {
        to_delete.add(serviceImpl_.getMyWorkSlipByIdString(session, broker, (String) id_strings.get(i)));
      }
      
      try
      {
        serviceImpl_.deleteMyWorkSlips(session, broker, to_delete.iterator());
        t.commit();
      } catch (XServiceException exc)
      {
        t.rollback();
        XMessage reply = new XMessage();
        exc.append(reply);
        return (reply);
      } finally
      {
        broker.close();
      }

      logger.info("/OpWorkService.deleteWorkSlip()");
      return null;
   }
}
