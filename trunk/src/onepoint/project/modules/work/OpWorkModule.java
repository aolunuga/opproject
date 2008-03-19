/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpEntityEventListener;
import onepoint.persistence.OpEvent;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpSource;
import onepoint.persistence.OpSourceManager;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.module.OpModuleChecker;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;

import java.util.Iterator;
import java.util.List;

/**
 * Class representing the work module.
 */
public class OpWorkModule extends OpModule implements OpEntityEventListener {

   private static final XLog logger = XLogFactory.getServerLogger(OpWorkModule.class);

   @Override
   public void start(OpProjectSession session) {
      // register listeners for OpProjectNode and OpProjectPlan
      for (OpSource source : OpSourceManager.getAllSources()) {
         source.addEntityEventListener(OpAssignment.class, this);
      }
   }

   @Override
   public void stop(OpProjectSession session) {
      // register listeners for OpProjectNode and OpProjectPlan
      for (OpSource source : OpSourceManager.getAllSources()) {
         source.removeEntityEventListener(OpAssignment.class, this);
      }
   }

   public void entityChangedEvent(OpEvent opevent) {
      // TODO Auto-generated method stub
      logger.debug ("Event received for class: " + opevent.getSource().getClass().getName() + " " + opevent.getAction()); 
   }
   
   /**
    * Upgrades the module to version 5 (internal schema version) via reflection.
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion5(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction tx = broker.newTransaction();
         this.upgradeWorkRecordsCosts(broker);
         this.upgradeActivityRemainingCosts(broker);
         tx.commit();
      }
      finally {
         broker.closeAndEvict();         
      }
   }

   /**
    * Upgrades the module to version 13 (internal schema version) via reflection.
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion13(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction tx = broker.newTransaction();
         this.upgradeWorkSlipTotalActualEffort(broker);
         tx.commit();
      }
      finally {
         broker.closeAndEvict();         
      }
   }

   /**
    * Upgrades the module to version 50 (internal schema version) via reflection.
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion50(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction tx = broker.newTransaction();
         this.upgradeWorkSlipState(broker);
         tx.commit();
      }
      finally {
         broker.close();         
      }
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
    * Upgrades the states for all the existent work records.
    *
    * @param broker a <code>OpBroker</code> used for persistence operations.
    */
   private void upgradeWorkSlipState(OpBroker broker) {
      String workSlipQuery = "select workSlip from OpWorkSlip workSlip where State is null";
      OpQuery query = broker.newQuery(workSlipQuery);
      Iterator wsIt = broker.iterate(query);
      while (wsIt.hasNext()) {
         OpWorkSlip workRecord = (OpWorkSlip) wsIt.next();
         workRecord.setState(OpWorkSlip.STATE_EDITABLE);
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


   public List<OpModuleChecker> getCheckerList() {
      List<OpModuleChecker> checkers = super.getCheckerList();
      checkers.add(new OpWorkModuleChecker());
      return checkers;
   }

}
