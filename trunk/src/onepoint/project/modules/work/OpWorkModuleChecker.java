/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 *
 */

package onepoint.project.modules.work;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleChecker;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Checker class associated to the work module.
 *
 * @author mihai.costin
 */
public class OpWorkModuleChecker implements OpModuleChecker {

   private static final XLog logger = XLogFactory.getServerLogger(OpWorkModule.class);

   public void check(OpProjectSession session) {
      logger.info("Checking module Work...");
      resetWorkValues(session);
      resetWorkMonths(session);
   }


   /**
    * Takes care of the values on activities and assignments given by work records.
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   private void resetWorkValues(OpProjectSession session) {
      resetAssignments(session);
      resetActivities(session);
      applyWorkRecords(session);
   }

   /**
    * Recalculates the values for all the work months.
    *
    * @param session project session
    */
   private void resetWorkMonths(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpQuery allProjectsQuery = broker.newQuery("from OpProjectNode projectNode where projectNode.Type = :type");
      allProjectsQuery.setParameter("type", OpProjectNode.PROJECT);
      OpTransaction tx = broker.newTransaction();
      Iterator<OpProjectNode> projectsIt = broker.iterate(allProjectsQuery);
      while (projectsIt.hasNext()) {
         OpProjectNode project = projectsIt.next();
         for (OpAssignment assignment : project.getPlan().getActivityAssignments()) {
            OpActivityDataSetFactory.updateWorkMonths(broker, assignment, session.getCalendar());
         }
      }
      tx.commit();
      broker.closeAndEvict();
   }

   /**
    * Applies all the found work records in the db on the associated assignments.
    * This method must be called only after resetting the values on assignments and activities.
    *
    * @param session project session
    */
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
      broker.closeAndEvict();
   }

   /**
    * Reset the values on all the activities (actual and remaining).
    *
    * @param session project session.
    */
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
         activity.setRemainingPersonnelCosts(activity.getBasePersonnelCosts());
         activity.setRemainingProceeds(activity.getBaseProceeds());

         //this will be set by progress calculator
         activity.setRemainingExternalCosts(activity.getBaseExternalCosts());
         activity.setRemainingMaterialCosts(activity.getBaseMaterialCosts());
         activity.setRemainingMiscellaneousCosts(activity.getBaseMiscellaneousCosts());
         activity.setRemainingTravelCosts(activity.getBaseTravelCosts());
      }
      transaction.commit();
      broker.closeAndEvict();
   }

   /**
    * Resets the values on all the assignments (actual and remaining).
    *
    * @param session project session
    */
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
      broker.closeAndEvict();
   }


}
