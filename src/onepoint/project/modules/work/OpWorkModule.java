/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class representing the work module.
 */
public class OpWorkModule extends OpModule {

   private static final XLog logger = XLogFactory.getServerLogger(OpWorkModule.class);

   /**
    * Upgrades the module to version 5 (internal schema version) via reflection.
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion5(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpTransaction tx = broker.newTransaction();
      this.upgradeWorkRecordsCosts(broker);
      this.upgradeActivityRemainingCosts(broker);
      tx.commit();
      broker.close();
   }

   /**
    * Upgrades the module to version 13 (internal schema version) via reflection.
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion13(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpTransaction tx = broker.newTransaction();
      this.upgradeWorkSlipTotalActualEffort(broker);
      tx.commit();
      broker.close();
   }

   /**
    * Upgrade to version 26.
    * Takes care of the values on activities and assignments given by work records.
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion30(OpProjectSession session) {

      resetAssignments(session);

      resetActivities(session);

      applyWorkRecords(session);
   }

   private void applyWorkRecords(OpProjectSession session) {
      OpBroker broker;
      OpTransaction transaction;
      OpQuery query;

      broker = session.newBroker();
      transaction = broker.newTransaction();
      query = broker.newQuery("select workRecord.ID from OpWorkRecord workRecord");
      List<Long> workRecordsId = broker.list(query);
      List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();
      logger.info("Found " + workRecordsId.size() + " work records to upgrade.");
      
      for (Long workRecordId : workRecordsId) {
         OpWorkRecord workRecord = (OpWorkRecord) broker.getObject(OpWorkRecord.class, workRecordId);
         if (workRecord.getRemainingEffort() < 0.0) {
            workRecord.setRemainingEffort(0.0);
         }
         if (workRecord.getRemExternalCosts() < 0.0) {
            workRecord.setRemExternalCosts(0.0);
         }
         if (workRecord.getRemMaterialCosts() < 0.0) {
            workRecord.setRemMaterialCosts(0.0);
         }
         if (workRecord.getRemMiscCosts() < 0.0) {
            workRecord.setRemMiscCosts(0.0);
         }
         if (workRecord.getRemTravelCosts() < 0.0) {
            workRecord.setRemTravelCosts(0.0);
         }

         //make sure the costs are calculated ok
         OpAssignment assignment = workRecord.getAssignment();
         OpResource resource = assignment.getResource();
         workRecord.setPersonnelCosts(workRecord.getActualEffort() * resource.getHourlyRate());
         workRecord.setActualProceeds(workRecord.getActualEffort() * resource.getExternalRate());         

         workRecords.add(workRecord);
         broker.updateObject(workRecord);
      }

      for (OpWorkRecord workRecord : workRecords) {
         logger.info("Upgrading work record " + workRecord);
         OpProgressCalculator.addWorkRecord(broker, workRecord, session.getCalendar());
      }

      transaction.commit();
      broker.close();
   }

   private void resetActivities(OpProjectSession session) {
      OpBroker broker;
      OpTransaction transaction;
      OpQuery query;
      Iterator result;

      //reset activities
      broker = session.newBroker();
      transaction = broker.newTransaction();
      query = broker.newQuery("select activity.ID from OpActivity activity where activity.Deleted = false");
      result = broker.iterate(query);
      while (result.hasNext()) {
         long id = (Long) result.next();
         OpActivity activity = (OpActivity) broker.getObject(OpActivity.class, id);

         //actual set to 0 (simulate the no workslip state)
         activity.setActualEffort(0);
         activity.setActualExternalCosts(0);
         activity.setActualMaterialCosts(0);
         activity.setActualMiscellaneousCosts(0);
         activity.setActualPersonnelCosts(0);
         activity.setActualProceeds(0.0);
         activity.setActualTravelCosts(0);

         //remaining values
         if (activity.getProjectPlan().getProgressTracked()) {
            activity.setRemainingEffort(activity.getBaseEffort());
         }
         else {
            double remainingEffort = OpGanttValidator.calculateRemainingEffort(activity.getBaseEffort(), activity.getActualEffort(), activity.getComplete());
            activity.setRemainingEffort(remainingEffort);
         }
         //this will be set by progress calculator
         activity.setRemainingExternalCosts(activity.getBaseExternalCosts());
         activity.setRemainingMaterialCosts(activity.getBaseMaterialCosts());
         activity.setRemainingMiscellaneousCosts(activity.getBaseMiscellaneousCosts());
         activity.setRemainingTravelCosts(activity.getBaseTravelCosts());
      }
      transaction.commit();
      broker.close();
   }

   private void resetAssignments(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpTransaction transaction = broker.newTransaction();
      //reset assignments
      OpQuery query = broker.newQuery("select assignment.ID from OpAssignment assignment");
      Iterator result = broker.iterate(query);
      while (result.hasNext()) {
         long id = (Long) result.next();
         OpAssignment assignment = (OpAssignment) broker.getObject(OpAssignment.class, id);

         //actual set to 0 (simulate the no workslip state)
         assignment.setActualCosts(0);
         assignment.setActualEffort(0);
         assignment.setActualProceeds(0.0);

         //reset remaining values based on tracking
         if (assignment.getProjectPlan().getProgressTracked()) {
            assignment.setRemainingEffort(assignment.getBaseEffort());
            assignment.setRemainingPersonnelCosts(assignment.getBaseCosts());
            assignment.setRemainingProceeds(assignment.getBaseProceeds());
         }
         else {
            double complete = assignment.getActivity().getComplete();
            assignment.setComplete(complete);
            OpProgressCalculator.updateAssignmentBasedOnTracking(assignment, false, false);
            //will be set by progress calculator
            assignment.setRemainingPersonnelCosts(assignment.getBaseCosts());
            assignment.setRemainingProceeds(assignment.getBaseProceeds());
         }
         broker.updateObject(assignment);
      }
      transaction.commit();
      broker.close();
   }


   /**
    * Updates the remaining costs for all un-deleted activities.
    *
    * @param broker a <code>OpBroker</code> used for persistence operations.
    */
   private void upgradeActivityRemainingCosts(OpBroker broker) {
      String activitiesQuery = "select activity from OpActivity activity where activity.Deleted=false";
      OpQuery activityQuery = broker.newQuery(activitiesQuery);
      Iterator activitiesIt = broker.iterate(activityQuery);
      while (activitiesIt.hasNext()) {
         OpActivity activity = (OpActivity) activitiesIt.next();
         activity.setRemainingExternalCosts(activity.getBaseExternalCosts() - activity.getActualExternalCosts());
         activity.setRemainingMaterialCosts(activity.getBaseMaterialCosts() - activity.getActualMaterialCosts());
         activity.setRemainingTravelCosts(activity.getBaseTravelCosts() - activity.getActualTravelCosts());
         activity.setRemainingMiscellaneousCosts(activity.getBaseMiscellaneousCosts() - activity.getActualMiscellaneousCosts());
         broker.updateObject(activity);
      }
   }

   /**
    * Upgrades the costs for all the existent work records.
    *
    * @param broker a <code>OpBroker</code> used for persistence operations.
    */
   private void upgradeWorkRecordsCosts(OpBroker broker) {
      String workRecordQuery = "select workRecord from OpWorkRecord workRecord " +
           "inner join workRecord.Assignment assignment inner join assignment.Activity activity" +
           " where activity.Deleted=false";
      OpQuery query = broker.newQuery(workRecordQuery);
      Iterator workRecordsIt = broker.iterate(query);
      while (workRecordsIt.hasNext()) {
         OpWorkRecord workRecord = (OpWorkRecord) workRecordsIt.next();
         this.updateTravelCosts(workRecord, broker);
         this.updateMaterialCosts(workRecord, broker);
         this.updateExternalCosts(workRecord, broker);
         this.updateMiscellaneousCosts(workRecord, broker);
      }
   }

   /**
    * Updates the travel costs for the given work record.
    *
    * @param workRecord a <code>OpWorkRecord</code> object for which to update
    *                   the travel costs.
    * @param broker     a <code>OpBroker</code> used for persistence operations.
    */
   private void updateTravelCosts(OpWorkRecord workRecord, OpBroker broker) {
      double baseTravelCosts = workRecord.getAssignment().getActivity().getBaseTravelCosts();
      double actualTravelCosts = workRecord.getTravelCosts();
      double remainingTravelCosts = baseTravelCosts - actualTravelCosts;
      workRecord.setRemTravelCosts(remainingTravelCosts);
      broker.updateObject(workRecord);

      if (actualTravelCosts > 0) {
         OpCostRecord costRecord = new OpCostRecord();
         costRecord.setType(OpCostRecord.TRAVEL_COST);
         costRecord.setActualCosts(actualTravelCosts);
         costRecord.setRemainingCosts(remainingTravelCosts);
         costRecord.setWorkRecord(workRecord);
         broker.makePersistent(costRecord);
      }
   }

   /**
    * Updates the material costs for the given work record.
    *
    * @param workRecord a <code>OpWorkRecord</code> object for which to update
    *                   the material costs.
    * @param broker     a <code>OpBroker</code> used for persistence operations.
    */
   private void updateMaterialCosts(OpWorkRecord workRecord, OpBroker broker) {
      double baseMaterialCosts = workRecord.getAssignment().getActivity().getBaseMaterialCosts();
      double actualMaterialCosts = workRecord.getMaterialCosts();
      double remainingMaterialCosts = baseMaterialCosts - actualMaterialCosts;
      workRecord.setRemMaterialCosts(remainingMaterialCosts);
      broker.updateObject(workRecord);

      if (actualMaterialCosts > 0) {
         OpCostRecord costRecord = new OpCostRecord();
         costRecord.setType(OpCostRecord.MATERIAL_COST);
         costRecord.setActualCosts(actualMaterialCosts);
         costRecord.setRemainingCosts(remainingMaterialCosts);
         costRecord.setWorkRecord(workRecord);
         broker.makePersistent(costRecord);
      }
   }

   /**
    * Updates the external costs for the given work record.
    *
    * @param workRecord a <code>OpWorkRecord</code> object for which to update
    *                   the external costs.
    * @param broker     a <code>OpBroker</code> used for persistence operations.
    */
   private void updateExternalCosts(OpWorkRecord workRecord, OpBroker broker) {
      double baseExternallCosts = workRecord.getAssignment().getActivity().getBaseExternalCosts();
      double actualExternallCosts = workRecord.getExternalCosts();
      double remainingExternallCosts = baseExternallCosts - actualExternallCosts;
      workRecord.setRemExternalCosts(remainingExternallCosts);
      broker.updateObject(workRecord);

      if (actualExternallCosts > 0) {
         OpCostRecord costRecord = new OpCostRecord();
         costRecord.setType(OpCostRecord.EXTERNAL_COST);
         costRecord.setActualCosts(actualExternallCosts);
         costRecord.setRemainingCosts(remainingExternallCosts);
         costRecord.setWorkRecord(workRecord);
         broker.makePersistent(costRecord);
      }
   }

   /**
    * Updates the miscellaneous costs for the given work record.
    *
    * @param workRecord a <code>OpWorkRecord</code> object for which to update
    *                   the miscellaneous costs.
    * @param broker     a <code>OpBroker</code> used for persistence operations.
    */
   private void updateMiscellaneousCosts(OpWorkRecord workRecord, OpBroker broker) {
      double baseMiscCosts = workRecord.getAssignment().getActivity().getBaseMiscellaneousCosts();
      double actualMiscCosts = workRecord.getMiscellaneousCosts();
      double remainingMiscCosts = baseMiscCosts - actualMiscCosts;
      workRecord.setRemMiscCosts(remainingMiscCosts);
      broker.updateObject(workRecord);

      if (actualMiscCosts > 0) {
         OpCostRecord costRecord = new OpCostRecord();
         costRecord.setType(OpCostRecord.MISCELLANEOUS_COST);
         costRecord.setActualCosts(actualMiscCosts);
         costRecord.setRemainingCosts(remainingMiscCosts);
         costRecord.setWorkRecord(workRecord);
         broker.makePersistent(costRecord);
      }
   }

   /**
    * Updates the sum of actual efforts for workslips.
    *
    * @param broker a <code>OpBroker</code> used for persistence operations.
    */
   private void upgradeWorkSlipTotalActualEffort(OpBroker broker) {
      String queryString = "select workSlip from OpWorkSlip workSlip";
      OpQuery query = broker.newQuery(queryString);
      Iterator workSlipIt = broker.iterate(query);
      while (workSlipIt.hasNext()) {
         OpWorkSlip workSlip = (OpWorkSlip) workSlipIt.next();
         workSlip.updateTotalActualEffort();
         broker.updateObject(workSlip);
      }
   }
}
