/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.work.validators.OpWorkCostValidator;
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

   private static final String WORK_SLIPS_ARGUMENT = "work_slip_set";
   private static final String WORK_SLIP_STATE_ARGUMENT = "work_slip_state";

   public final static String ATTACHMENT_LIST = "attachmentsList";
   public final static String COST_ROW_INDEX = "costRowIndex";

   //indexes of resouce rates in the rates list
   private final int INTERNAL_RESOURCE_RATE_INDEX = 0;
   private final int EXTERNAL_RESOURCE_RATE_INDEX = 1;

   public final static OpWorkErrorMap ERROR_MAP = new OpWorkErrorMap();

   private OpWorkServiceImpl serviceImpl = new OpWorkServiceImpl();
   private static final String GET_ATTACHMENTS_FROM_WORK_SLIP = "select attachment from OpWorkSlip workSlip inner join workSlip.Records workRecord inner join workRecord.CostRecords costRecord inner join costRecord.Attachments attachment where workSlip.ID = ?";


   public XMessage insertWorkSlip(OpProjectSession session, XMessage request) {

      logger.info("OpWorkService.insertWorkSlip()");
      Date start = (Date) (request.getArgument(START));

      XComponent effortSet = (XComponent) (request.getArgument(EFFORT_RECORD_SET_ARGUMENT));
      XComponent timeSet = (XComponent) (request.getArgument(TIME_RECORD_SET_ARGUMENT));
      //create a copy of the costSet because the original one (the one from the request) is needed to keep the
      //content ids in case of an error
      XComponent costSet = ((XComponent) (request.getArgument(COSTS_RECORD_SET_ARGUMENT))).copyData();

      // hashset should be ArraySet
      Set<OpWorkRecord> workRecordsToAdd = new HashSet<OpWorkRecord>();

      OpBroker broker = session.newBroker();
      OpTransaction t = null;
      try {
         t = broker.newTransaction();
         int errorCode = createWorkRecords(broker, effortSet, timeSet, costSet, workRecordsToAdd, start, null);

         // if no works records are valid throw an exception
         if (workRecordsToAdd.isEmpty() && errorCode != 0) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, errorCode));
            return reply;
         }

         OpWorkSlip workSlip = new OpWorkSlip();
         workSlip.setDate(start);
         workSlip.addRecords(workRecordsToAdd);
         workSlip.updateTotalActualEffort();

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
   private int createWorkRecords(OpBroker broker, XComponent effortRecordSet, XComponent timeRecordSet,
        XComponent costRecordSet, Set<OpWorkRecord> workRecords, Date workSlipDate, Map<XComponent,
        List<OpAttachment>> unmodifiedAttachmentsMap) {
      OpAssignment assignment;
      int errorCode = 0;

      List<OpWorkRecord> workRecordList = OpWorkSlipDataSetFactory
            .formWorkRecordsFromDataSets(broker, effortRecordSet,
           timeRecordSet, costRecordSet, unmodifiedAttachmentsMap);
      for (OpWorkRecord workRecord : workRecordList) {
         assignment = workRecord.getAssignment();

         OpProjectNodeAssignment projectNodeAssignment = assignment.getProjectNodeAssignment();

         //Set the personnel costs for the work record
         if (projectNodeAssignment != null) {
            List<Double> ratesList = projectNodeAssignment.getRatesForDay(workSlipDate, true);
            double internalResourceRate = ratesList.get(INTERNAL_RESOURCE_RATE_INDEX);
            double externalResourceRate = ratesList.get(EXTERNAL_RESOURCE_RATE_INDEX);
            workRecord.setPersonnelCosts(internalResourceRate * workRecord.getActualEffort());
            workRecord.setActualProceeds(externalResourceRate * workRecord.getActualEffort());
         }


         workRecord.calculateActualCostsOfType(OpCostRecord.TRAVEL_COST);
         workRecord.calculateActualCostsOfType(OpCostRecord.MATERIAL_COST);
         workRecord.calculateActualCostsOfType(OpCostRecord.EXTERNAL_COST);
         workRecord.calculateActualCostsOfType(OpCostRecord.MISCELLANEOUS_COST);

         //update remaining costs -- this will set on the work record the remaining costs provided by the user
         // or the ones from the activity if none are provided           
         workRecord.calculateRemainingCostsOfType(OpCostRecord.TRAVEL_COST);
         workRecord.calculateRemainingCostsOfType(OpCostRecord.MATERIAL_COST);
         workRecord.calculateRemainingCostsOfType(OpCostRecord.EXTERNAL_COST);
         workRecord.calculateRemainingCostsOfType(OpCostRecord.MISCELLANEOUS_COST);

         try {
            workRecord.validate();
         }
         catch (OpEntityException e) {
            // Keep the first error code
            if (errorCode == 0) {
               errorCode = e.getErrorCode();
            }
            logger.error("Invalid work record found " + e.getMessage());
            logger.debug("Invalid work record found ", e);
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
      //create a copy of the costSet because the original one (the one from the request) is needed to keep the
      //content ids in case of an error
      XComponent costsRecordSet = ((XComponent) (request.getArgument(COSTS_RECORD_SET_ARGUMENT))).copyData();

      // TODO: Error handling (parameters set)?
      OpBroker broker = session.newBroker();
      OpTransaction t = null;

      try {

         OpWorkSlip workSlip = serviceImpl.getMyWorkSlipByIdString(session, broker, workSlipId);

         if (workSlip == null) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.WORK_SLIP_NOT_FOUND));
            return reply;
         }

         // Check, whether our state changed...
         if (workSlip.getState() != OpWorkSlip.STATE_EDITABLE) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, OpWorkError.WORK_SLIP_NOT_EDITABLE));
            return reply;
         }

         // HashSet should be ArraySet
         Set<OpWorkRecord> workRecordsToAdd = new HashSet<OpWorkRecord>();

         t = broker.newTransaction();

         //obtain the attachments that were not modified during the edit operation
         List<List> attachmentList;
         //the list containing the locators of the unmodified attachments
         List<String> unmodifiedAttachmentLocators;
         //map Key: cost record data row; Value: a list of Strings containing the locators of the unmodified attachments
         //for the cost record represented by the data row.
         Map<XComponent, List<String>> unmodifiedLocatorsMap = new HashMap<XComponent, List<String>>();
         for(int i = 0; i < costsRecordSet.getChildCount(); i++) {
            XComponent costRow = (XComponent) costsRecordSet.getChild(i);
            attachmentList = (List<List>) ((XComponent)costRow.getChild(OpWorkCostValidator.ATTACHMENT_INDEX)).getValue();
            unmodifiedAttachmentLocators = new ArrayList<String>();

            Iterator<List> iterator = attachmentList.iterator();
            while (iterator.hasNext()) {
               List attachmentElement = iterator.next();
               String attachmentChoice = (String) attachmentElement.get(1);
               String attachmentLocator = XValidator.choiceID(attachmentChoice);
               //if the attachment locator is 0 then the attachment is a newly inserted one, else it is an unmodified attachment
               if (!attachmentLocator.equals("0")) {
                  unmodifiedAttachmentLocators.add(attachmentLocator);
                  //if the attachment was unmodified remove the it's corresponding attachment element from the list so that
                  // it is not inserted again
                  iterator.remove();
               }
            }
            if (!unmodifiedAttachmentLocators.isEmpty()) {
               //add a new entry in the map for this cost row, namely all its unmodified attachment locators
               unmodifiedLocatorsMap.put(costRow, unmodifiedAttachmentLocators);
            }
         }

         //update the contents for all the attachments that belong to the work record
         //map Key: cost record data row; Value: a list of OpAttachment containing the unmodified attachments
         //for the cost record represented by the data row.
         Map<XComponent, List<OpAttachment>> unmodifiedAttachmentsMap = new HashMap<XComponent, List<OpAttachment>>();
         List<OpContent> contents = new ArrayList<OpContent>();
         OpQuery query = broker.newQuery(GET_ATTACHMENTS_FROM_WORK_SLIP);
         query.setLong(0, workSlip.getID());
         List<OpAttachment> result = broker.list(query);
         for (OpAttachment attachment : result) {
            //if the attachment was not modified by the edit operation, break it's link to the cost record and add it
            //to the list of unmodified attachments for the cost row to which it belongs
            if(checkUnmodifiedAttachment(attachment, unmodifiedLocatorsMap, unmodifiedAttachmentsMap)) {
               OpCostRecord costRecord = attachment.getCostRecord();
               costRecord.getAttachments().remove(attachment);
               attachment.setCostRecord(null);
            }
            else {
               if (!attachment.getLinked()) {
                  contents.add(attachment.getContent());
                  attachment.setContent(null);
               }
            }
         }

         //remove all work records from the work slip
         serviceImpl.deleteWorkRecords(broker, workSlip, session.getCalendar());

         //insert all the new work records
         int errorCode = createWorkRecords(broker, effortRecordSet, timeRecordSet, costsRecordSet, workRecordsToAdd,
              workSlip.getDate(), unmodifiedAttachmentsMap);
         if (workRecordsToAdd.isEmpty() && errorCode != 0) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, errorCode));
            return reply;
         }

         workSlip.addRecords(workRecordsToAdd);
         workSlip.updateTotalActualEffort();
         serviceImpl.insertMyWorkRecords(session, broker, workRecordsToAdd.iterator(), workSlip);

         //validate work record set
         try {
            workSlip.validate();
         }
         catch (OpEntityException e) {
            XMessage reply = new XMessage();
            reply.setError(session.newError(ERROR_MAP, e.getErrorCode()));
            return reply;
         }

         //delete all contents with reference count = 0
         for(OpContent content : contents) {
            OpContentManager.updateContent(content, broker, false, true);
         }
         t.commit();

         logger.info("/OpWorkService.editWorkSlip()");
         return null;
      }
      catch (XServiceException exc) {
         XMessage reply = new XMessage();
         return exc.append(reply);
      }
      finally {
         finalizeSession(t, broker);
      }
   }

   public XMessage deleteWorkSlips(OpProjectSession session, XMessage request)
        throws XServiceException {
      ArrayList id_strings = (ArrayList) (request.getArgument(WORK_SLIP_IDS));
      logger.info("OpWorkService.deleteWorkSlip(): workslip_ids = " + id_strings);

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      try {
         LinkedList<OpWorkSlip> to_delete = new LinkedList<OpWorkSlip>();
         for (Object id_string : id_strings) {
            OpWorkSlip ws = serviceImpl.getMyWorkSlipByIdString(session, broker, (String) id_string);
            // TODO: change this to return an error???
            if (ws != null && ws.getState() == OpWorkSlip.STATE_EDITABLE) {
               to_delete.add(serviceImpl.getMyWorkSlipByIdString(session, broker, (String) id_string));
            }
         }

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

   /**
    * Checks if the <code>OpAttachment</code> passed as parameter has its locator in the
    *    <code>Map<XComponent, List<String>></code> unmodifiedLocatorsMap and if this is the case it adds the locator in
    *    the <code>Map<XComponent, List<OpAttachment>></code> unmodifiedAttachmentsMap at the same key as in
    *    the unmodifiedLocatorsMap.
    *
    * @param attachment - the <code>OpAttachment</code> who is checked.
    * @param unmodifiedLocatorsMap - the <code>Map<XComponent, List<String>></code> containing the locators of the
    *    unmodified attachments.
    * @param unmodifiedAttachmentsMap - the <code>Map<XComponent, List<OpAttachment>></code> containing the unmodified
    *    attachments.
    * @return <code>true</code> if the <code>OpAttachment</code> passed as parameter has its locator in the
    *    <code>Map<XComponent, List<String>></code> unmodifiedLocatorsMap and <code>false</code> otherwise.
    */
   private boolean checkUnmodifiedAttachment(OpAttachment attachment, Map<XComponent, List<String>> unmodifiedLocatorsMap,
        Map<XComponent, List<OpAttachment>> unmodifiedAttachmentsMap) {
      for(XComponent dataRow : unmodifiedLocatorsMap.keySet()) {
         //if the attachment locator is in the list of unmodified attachment locator of a certain key
         if(unmodifiedLocatorsMap.get(dataRow).contains(attachment.locator())) {
            //add the attachment to the list of attachment for the key
            if(!unmodifiedAttachmentsMap.keySet().contains(dataRow)) {
               List<OpAttachment> attachmentList = new ArrayList<OpAttachment>();
               attachmentList.add(attachment);
               unmodifiedAttachmentsMap.put(dataRow, attachmentList);
            }
            else {
               unmodifiedAttachmentsMap.get(dataRow).add(attachment);
            }
            return true;
         }
      }
      return false;
   }
}