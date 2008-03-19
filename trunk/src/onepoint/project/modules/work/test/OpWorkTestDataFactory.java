/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.work.*;
import onepoint.project.modules.work.validators.OpWorkCostValidator;
import onepoint.project.modules.work.validators.OpWorkEffortValidator;
import onepoint.project.test.OpTestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class contains helper methods for managing projects data
 *
 * @author lucian.furtos
 */
public class OpWorkTestDataFactory extends OpTestDataFactory {

   private final static String SELECT_WORK_SLIP_ID_BY_NUMBER_QUERY = "select workSlip.ID from OpWorkSlip as workSlip where workSlip.Number = ?";
   private final static String SELECT_ALL_WORK_SLIPS = "select workSlip from OpWorkSlip as workSlip";
   private final static String SELECT_WORK_RECORD_ID_BY_RESOURCE_NAME_QUERY = "select workRecord.ID from OpWorkRecord as workRecord where workRecord.Assignment.Resource.Name = ?";
   private final static String SELECT_ATTACHMENT_ID_BY_COST_RECORD_ID_QUERY = "select attachment.ID from OpAttachment as attachment where attachment.CostRecord.ID = ?";

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public OpWorkTestDataFactory(OpProjectSession session) {
      super(session);
   }

   public static XMessage createInsertWorkSlipRequest(OpAssignment assignment, String name, Date date, boolean completed, boolean insert, int type, double actual, double remaining, double material,
        double travel, double external, double misc) {


      XComponent workSet = new XComponent(XComponent.DATA_SET);
      OpWorkEffortValidator workValidator = new OpWorkEffortValidator();
      addEffortRow(workValidator, workSet, assignment, completed, insert, type, actual, remaining);

      XComponent costSet = new XComponent(XComponent.DATA_SET);
      OpWorkCostValidator costValidator = new OpWorkCostValidator();

      addCostRow(costSet, costValidator, assignment, OpCostRecord.MATERIAL_COST, material);
      addCostRow(costSet, costValidator, assignment, OpCostRecord.TRAVEL_COST, travel);
      addCostRow(costSet, costValidator, assignment, OpCostRecord.EXTERNAL_COST, external);
      addCostRow(costSet, costValidator, assignment, OpCostRecord.MISCELLANEOUS_COST, misc);

      XComponent timeSet = new XComponent(XComponent.DATA_SET);

      XMessage request = new XMessage();
      request.setArgument(OpWorkService.START, date);
      request.setArgument(OpWorkService.EFFORT_RECORD_SET_ARGUMENT, workSet);
      request.setArgument(OpWorkService.COSTS_RECORD_SET_ARGUMENT, costSet);
      request.setArgument(OpWorkService.TIME_RECORD_SET_ARGUMENT, timeSet);

      return request;
   }

   private static void addEffortRow(OpWorkEffortValidator workValidator, XComponent workSet, OpAssignment assignment, boolean completed, boolean insert, int type, double actual, double remaining) {
      XComponent row = workValidator.newDataRow();
      workSet.addChild(row);
      row.setStringValue(assignment.locator());

      workValidator.setValue(row, OpWorkEffortValidator.ACTIVITY_NAME_INDEX, assignment.getActivity().locator());
      workValidator.setValue(row, OpWorkEffortValidator.RESOURCE_NAME_INDEX, assignment.getResource().locator());
      workValidator.setValue(row, OpWorkEffortValidator.PROJECT_NAME_INDEX, assignment.getProjectPlan().locator());
      workValidator.setValue(row, OpWorkEffortValidator.COMPLETED_INDEX, completed);
      workValidator.setValue(row, OpWorkEffortValidator.ACTIVITY_CREATED_INDEX, insert);
      workValidator.setValue(row, OpWorkEffortValidator.ACTIVITY_TYPE_INDEX, type);
      workValidator.setValue(row, OpWorkEffortValidator.ACTUAL_EFFORT_INDEX, actual);
      workValidator.setValue(row, OpWorkEffortValidator.REMAINING_EFFORT_INDEX, remaining);
   }

   private static void addCostRow(XComponent costSet, OpWorkCostValidator costValidator, OpAssignment assignment, byte type, double actual) {
      XComponent row;
      row = costValidator.newDataRow();
      row.setStringValue(assignment.locator());
      costValidator.setValue(row, OpWorkCostValidator.ACTIVITY_NAME_INDEX, assignment.getActivity().locator());
      costValidator.setValue(row, OpWorkCostValidator.ACTUAL_COST_INDEX, actual);
      costValidator.setValue(row, OpWorkCostValidator.COST_TYPE_INDEX, XValidator.choice(String.valueOf(type), ""));
      costValidator.setValue(row, OpWorkCostValidator.RESOURCE_NAME_INDEX, assignment.getResource().locator());
      costValidator.setValue(row, OpWorkCostValidator.PROJECT_NAME_INDEX, assignment.getProjectPlan().locator());

      costSet.addChild(row);
   }


   public static XMessage insertMoreWSMsg(List<OpAssignment> assignments, Date date, boolean completed, boolean insert, int type, List<Double> actuals, List<Double> remainings,
        List<Double> materials, List<Double> travels, List<Double> externals, List<Double> miscs) {

      XComponent workSet = new XComponent(XComponent.DATA_SET);
      OpWorkEffortValidator workValidator = new OpWorkEffortValidator();

      XComponent costSet = new XComponent(XComponent.DATA_SET);
      OpWorkCostValidator costValidator = new OpWorkCostValidator();      

      for (int i = 0; i < assignments.size(); i++) {
         addEffortRow(workValidator, workSet, assignments.get(i), completed, insert, type, actuals.get(i), remainings.get(i));
         addCostRow(costSet, costValidator, assignments.get(i), OpCostRecord.MATERIAL_COST, materials.get(i));
         addCostRow(costSet, costValidator, assignments.get(i), OpCostRecord.TRAVEL_COST, travels.get(i));
         addCostRow(costSet, costValidator, assignments.get(i), OpCostRecord.EXTERNAL_COST, externals.get(i));
         addCostRow(costSet, costValidator, assignments.get(i), OpCostRecord.MISCELLANEOUS_COST, miscs.get(i));
      }

      XComponent timeSet = new XComponent(XComponent.DATA_SET);

      XMessage request = new XMessage();
      request.setArgument(OpWorkService.START, date);
      request.setArgument(OpWorkService.EFFORT_RECORD_SET_ARGUMENT, workSet);
      request.setArgument(OpWorkService.TIME_RECORD_SET_ARGUMENT, timeSet);
      request.setArgument(OpWorkService.COSTS_RECORD_SET_ARGUMENT, costSet);

      return request;
   }

   /**
    * Get the DB identifier of a work slip by number
    *
    * @param workSlipNumber the work slip number
    * @return the unique identifier of an entity (the locator)
    */
   public String getWorkSlipId(String workSlipNumber) {
      OpBroker broker = session.newBroker();
      try {
         Long workSlipId = null;

         OpQuery query = broker.newQuery(SELECT_WORK_SLIP_ID_BY_NUMBER_QUERY);
         query.setString(0, workSlipNumber);
         Iterator workSlipIt = broker.iterate(query);
         if (workSlipIt.hasNext()) {
            workSlipId = (Long) workSlipIt.next();
         }

         if (workSlipId != null) {
            return OpLocator.locatorString(OpWorkSlip.WORK_SLIP, Long.parseLong(workSlipId.toString()));
         }

         return null;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get a work slip by the locator
    *
    * @param locator the unique identifier (locator) of an entity
    * @return an instance of <code>OpWorkSlip</code>
    */
   public OpWorkSlip getResourceById(String locator) {
      OpBroker broker = session.newBroker();
      try {
         return (OpWorkSlip) broker.getObject(locator);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Gets all workslips from the database
    *
    * @return - a <code>List</code> ontaining all the workslips in the database
    */
   public List<OpWorkSlip> getAllWorkSlips() {
      OpBroker broker = session.newBroker();
      try {
         List<OpWorkSlip> workSlips = new ArrayList<OpWorkSlip>();
         OpQuery query = broker.newQuery(SELECT_ALL_WORK_SLIPS);

         Iterator it = broker.iterate(query);
         while (it.hasNext()) {
            workSlips.add((OpWorkSlip) it.next());
         }
         return workSlips;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get the DB identifier of a work record by the name of the resource that is assigned to it
    *
    * @param resourceName the resource name
    * @return the unique identifier of an entity (the locator)
    */
   public String getWorkRecordId(String resourceName) {
      OpBroker broker = session.newBroker();
      try {
         Long workRecordId = null;

         OpQuery query = broker.newQuery(SELECT_WORK_RECORD_ID_BY_RESOURCE_NAME_QUERY);
         query.setString(0, resourceName);
         Iterator workRecordIt = broker.iterate(query);
         if (workRecordIt.hasNext()) {
            workRecordId = (Long) workRecordIt.next();
         }

         if (workRecordId != null) {
            return OpLocator.locatorString(OpWorkRecord.WORK_RECORD, Long.parseLong(workRecordId.toString()));
         }

         return null;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get a work record by the locator
    *
    * @param locator the unique identifier (locator) of an entity
    * @return an instance of <code>OpWorkRecord</code>
    */
   public OpWorkRecord getWorkRecordById(String locator) {
      OpBroker broker = session.newBroker();
      try {
         OpWorkRecord workRecord = (OpWorkRecord) broker.getObject(locator);
         if (workRecord != null) {
            // just to inialize the collection
            workRecord.getCostRecords().size();
            workRecord.getTimeRecords().size();
            workRecord.getAssignment().getActivity().getProjectPlan().getProjectNode().getName();
            workRecord.getAssignment().getResource().getName();
            if (!workRecord.getCostRecords().isEmpty()) {
               for (OpCostRecord costRecord : workRecord.getCostRecords()) {
                  costRecord.getAttachments().size();
               }
            }
         }
         return workRecord;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get the DB identifier of an attachment by the id of the cost record
    *
    * @param costRecordId the id of the cost record
    * @return the unique identifier of an entity (the locator)
    */
   public String getAttachmentId(Long costRecordId) {
      OpBroker broker = session.newBroker();
      try {
         Long attachmentId = null;

         OpQuery query = broker.newQuery(SELECT_ATTACHMENT_ID_BY_COST_RECORD_ID_QUERY);
         query.setLong(0, costRecordId);
         Iterator attachmentIt = broker.iterate(query);
         if (attachmentIt.hasNext()) {
            attachmentId = (Long) attachmentIt.next();
         }
         if (attachmentId != null) {
            return OpLocator.locatorString(OpAttachment.ATTACHMENT, Long.parseLong(attachmentId.toString()));
         }

         return null;
      }
      finally {
         broker.close();
      }
   }

   /**
    * Get an attachment by the locator
    *
    * @param locator the unique identifier (locator) of an entity
    * @return an instance of <code>OpAttachment</code>
    */
   public OpAttachment getAttachmentById(String locator) {
      OpBroker broker = session.newBroker();
      try {
         return (OpAttachment) broker.getObject(locator);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Sets the start time, finish time and duration values on an <code>OpTimeRecord</code> entity
    *
    * @param timeRecord - the <code>OpTimeRecord</code> entity
    * @param startTime  - the start time value
    * @param finishTime - the finish time value
    * @param duration   - the duration value
    */
   public static void setFieldsOnTimeRecord(OpTimeRecord timeRecord, int startTime, int finishTime, int duration) {
      timeRecord.setStart(startTime);
      timeRecord.setFinish(finishTime);
      timeRecord.setDuration(duration);
   }

}
