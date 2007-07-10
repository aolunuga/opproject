/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceException;

import java.sql.Date;
import java.util.*;

public class OpWorkService extends OpProjectService {

   private static final XLog logger = XLogFactory.getServerLogger(OpWorkService.class);

   public final static String START = "start";
   public final static String WORK_SLIP_ID = "work_slip_id";
   public final static String WORK_SLIP_IDS = "work_slip_ids";

   public final static String EFFORT_RECORD_SET_ARGUMENT = "effort_record_set";
   public final static String TIME_RECORD_SET_ARGUMENT = "time_record_set";
   public final static String COSTS_RECORD_SET_ARGUMENT = "costs_record_set";

   public final static String ATTACHMENT_LIST = "attachmentsList";
   public final static String COST_ROW_INDEX = "costRowIndex";

   //indexes of resouce rates in the rates list
   private final int INTERNAL_RESOURCE_RATE_INDEX = 0;
   private final int EXTERNAL_RESOURCE_RATE_INDEX = 1;

   public final static OpWorkErrorMap ERROR_MAP = new OpWorkErrorMap();

   private OpWorkServiceImpl serviceImpl = new OpWorkServiceImpl();

   public XMessage insertWorkSlip(OpProjectSession session, XMessage request) {

      logger.info("OpWorkService.insertWorkSlip()");
      Date start = (Date) (request.getArgument(START));

      XComponent effortSet = (XComponent) (request.getArgument(EFFORT_RECORD_SET_ARGUMENT));
      XComponent timeSet = (XComponent) (request.getArgument(TIME_RECORD_SET_ARGUMENT));
      XComponent costSet = (XComponent) (request.getArgument(COSTS_RECORD_SET_ARGUMENT));

      // hashset should be ArraySet
      Set<OpWorkRecord> workRecordsToAdd = new HashSet<OpWorkRecord>();

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      int errorCode = createWorkRecords(broker, effortSet, timeSet, costSet, workRecordsToAdd, start);

      // if no works records are valid throw an exception
      if (workRecordsToAdd.isEmpty() && errorCode != 0) {
         XMessage reply = new XMessage();
         reply.setError(session.newError(ERROR_MAP, errorCode));
         return reply;
      }

      try {
         OpWorkSlip workSlip = new OpWorkSlip();
         workSlip.setDate(start);
         workSlip.setRecords(workRecordsToAdd);

         // note: second one is required in order to correct OpProgressCalculator only! (would be better to notify OpProgressCalculator on any changes - PropertyChangeEvent?)
         serviceImpl.insertMyWorkSlip(session, broker, workSlip);
         t.commit();
      }
      catch (XServiceException exc) {
         XMessage reply = new XMessage();
         reply.setError(exc.getError());
         return reply;
      }
      finally {
         finalizeSession(t, broker);
      }

      logger.info("/OpWorkService.insertWorkSlip()");
      return null;
   }

   /**
    * Creates a set of work records using the information given in the workRecord data set.
    * Will collect the work records in the given workRecords set.
    *
    * @param broker          Broker used for db access
    * @param effortRecordSet
    * @param timeRecordSet
    * @param costRecordSet
    * @param workRecords     set that will contain the created works records
    * @param workSlipDate    the date needed in the calculation of the actual costs @return the first error message returned by the work record validation method
    * @return error code
    */
   private int createWorkRecords(OpBroker broker, XComponent effortRecordSet, XComponent timeRecordSet, XComponent costRecordSet, Set<OpWorkRecord> workRecords, Date workSlipDate) {
      OpAssignment assignment;
      int errorCode = 0;

      List<OpWorkRecord> workRecordList = OpWorkSlipDataSetFactory.formWorkRecordsFromDataSets(broker, effortRecordSet, timeRecordSet, costRecordSet);
      for (OpWorkRecord workRecord : workRecordList) {
         assignment = workRecord.getAssignment();

         //get the project node assignment for this assignment's resource
         OpProjectNodeAssignment projectNodeAssignment = null;
         for (OpProjectNodeAssignment resourceAssignment : assignment.getResource().getProjectNodeAssignments()) {
            for (OpProjectNodeAssignment projectAssignment : assignment.getProjectPlan().getProjectNode().getAssignments()) {
               if (resourceAssignment.getID() == projectAssignment.getID()) {
                  projectNodeAssignment = projectAssignment;
                  break;
               }
            }
         }

         //Set the personnel costs for the work record
         if (projectNodeAssignment != null) {
            List<Double> ratesList = projectNodeAssignment.getRatesForDay(workSlipDate, true);
            double internalResourceRate = ratesList.get(INTERNAL_RESOURCE_RATE_INDEX);
            double externalResourceRate = ratesList.get(EXTERNAL_RESOURCE_RATE_INDEX);
            workRecord.setPersonnelCosts(internalResourceRate * workRecord.getActualEffort());
            workRecord.setActualProceeds(externalResourceRate * workRecord.getActualEffort());
         }

         //update remaining costs
         workRecord.calculateActualCostsOfType(OpCostRecord.TRAVEL_COST);
         workRecord.calculateActualCostsOfType(OpCostRecord.MATERIAL_COST);
         workRecord.calculateActualCostsOfType(OpCostRecord.EXTERNAL_COST);
         workRecord.calculateActualCostsOfType(OpCostRecord.MISCELLANEOUS_COST);

         workRecord.calculateRemainingCostsOfType(OpCostRecord.TRAVEL_COST);
         workRecord.calculateRemainingCostsOfType(OpCostRecord.MATERIAL_COST);
         workRecord.calculateRemainingCostsOfType(OpCostRecord.EXTERNAL_COST);
         workRecord.calculateRemainingCostsOfType(OpCostRecord.MISCELLANEOUS_COST);

         OpActivity activity = assignment.getActivity();
         workRecord.setRemTravelCostsChange(workRecord.getRemTravelCosts() - activity.getRemainingTravelCosts());
         workRecord.setRemMaterialCostsChange(workRecord.getRemMaterialCosts() - activity.getRemainingMaterialCosts());
         workRecord.setRemExternalCostsChange(workRecord.getRemExternalCosts()- activity.getRemainingExternalCosts());
         workRecord.setRemMiscCostsChange(workRecord.getRemMiscCosts() - activity.getRemainingMiscellaneousCosts());

         try {
            workRecord.validate();
         }
         catch (OpEntityException e) {
            // Keep the first error code
            if (errorCode == 0) {
               errorCode = e.getErrorCode();
            }
            logger.error("Invalid work record found " + e.getMessage());
            logger.debug("Invalid work record found ",e);
         }

         workRecords.add(workRecord);
      }

      return errorCode;
   }

   public XMessage editWorkSlip(OpProjectSession session, XMessage request) {
      logger.info("OpWorkService.editWorkSlip()");

      String workSlipId = (String) (request.getArgument(WORK_SLIP_ID));
      XComponent effortRecordSet = (XComponent) (request.getArgument(EFFORT_RECORD_SET_ARGUMENT));
      XComponent timeRecordSet = (XComponent) (request.getArgument(TIME_RECORD_SET_ARGUMENT));
      XComponent costsRecordSet = (XComponent) (request.getArgument(COSTS_RECORD_SET_ARGUMENT));

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
         Set<OpWorkRecord> workRecordsToAdd = new HashSet<OpWorkRecord>();

         t = broker.newTransaction();

         //<FIXME author="Haizea Florin" description="find a better method to update the contents of the attachments">
         for (OpWorkRecord workRecord : workSlip.getRecords()) {
            for (OpCostRecord costRecord : workRecord.getCostRecords()) {
               for (OpAttachment attachment : costRecord.getAttachments()) {
                  if (!attachment.getLinked()) {
                     OpContentManager.updateContent(attachment.getContent(), broker, false, attachment);
                     attachment.setContent(null);
                  }
               }
            }
         }
         //<FIXME>         //update the contents for all the attachments that belong to the work record

         //remove all work records from the work slip
         serviceImpl.deleteWorkRecords(broker, workSlip);

         //insert all the new work records
         int errorCode = createWorkRecords(broker, effortRecordSet, timeRecordSet, costsRecordSet, workRecordsToAdd, workSlip.getDate());
         if (workRecordsToAdd.isEmpty() && errorCode != 0) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, errorCode));
            finalizeSession(t, broker);
            return reply;
         }

         workSlip.setRecords(workRecordsToAdd);
         serviceImpl.insertWorkRecords(session, broker, workRecordsToAdd.iterator(), workSlip);

         //validate work record set
         try {
            workSlip.validate();
         }
         catch (OpEntityException e) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, e.getErrorCode()));
            finalizeSession(t, broker);
            return reply;
         }

         t.commit();

         logger.info("/OpWorkService.editWorkSlip()");
         return null;
      }
      catch (XServiceException exc) {
         XMessage reply = new XMessage();
         return exc.append(reply);
      }
      finally{
        finalizeSession(t, broker);
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
         XMessage reply = new XMessage();
         exc.append(reply);
         return (reply);
      }
      finally {
         finalizeSession(t, broker);
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