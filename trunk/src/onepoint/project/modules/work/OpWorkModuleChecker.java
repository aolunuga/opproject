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
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Checker class associated to the work module.
 *
 * @author mihai.costin
 * @author horia.chiorean
 */
public class OpWorkModuleChecker extends OpProjectModuleChecker {

   /**
    * This class's logger
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpWorkModule.class);

   @Override
   public void check(OpProjectSession session) {
      logger.info("Checking module Work...");
      for (Iterator it = super.getProjectsOfType(session, OpProjectNode.PROJECT).iterator(); it.hasNext();) {
         Long projectId = (Long) it.next();
         resetWorkValues(session, projectId);
         // resetWorkMonths(session, projectId);
      }
   }


   /**
    * Takes care of the values on activities and assignments given by work records.
    *
    * @param session   a <code>OpProjectSession</code> used during the upgrade procedure.
    * @param projectId a <code>long</code> the id of a project.
    */
   private void resetWorkValues(OpProjectSession session, long projectId) {
      resetAssignments(session, projectId);
      resetActivities(session, projectId);
      applyWorkRecordsForProject(session, projectId);
   }

   private final static int TRANSACTION_SIZE = 100;

   /**
    * Applies all the found work records in the db on the associated assignments.
    * This method must be called only after resetting the values on assignments and activities.
    *
    * @param session    project session
    * @param projectId a <code>long</code> the id of an projectId
    */
   private void applyWorkRecordsForProject(OpProjectSession session, long projectId) {
      OpBroker broker;
      OpTransaction transaction;
      OpQuery query;

      broker = session.newBroker();
      try {
         transaction = broker.newTransaction();
         query = broker.newQuery("select wr.ID from OpWorkRecord as wr where wr.Assignment.Activity.ProjectPlan.ProjectNode.ID = :projectId");
         query.setLong("projectId", projectId);
         List<Long> workRecordsId = broker.list(query);
         List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();
         logger.info("Found " + workRecordsId.size() + " work records to upgrade for project id: " + projectId);
   
   
         int opCount = 0;
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
   
            //update work record costs based on cost records values!
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
   
            workRecords.add(workRecord);
            broker.updateObject(workRecord);
            opCount++;
            if (opCount % TRANSACTION_SIZE == 0) {
               logger.info("Commiting WRs after " + opCount + " updates");
               transaction.commit();
               transaction = broker.newTransaction();
            }
         }
   
         Set<OpAssignment> assignmentsTouched = new HashSet<OpAssignment>();
         for (OpWorkRecord workRecord : workRecords) {
            logger.info("Upgrading work record " + workRecord);
            workRecord.getAssignment().handleWorkProgress(workRecord, true);
            assignmentsTouched.add(workRecord.getAssignment());
            opCount++;
            if (opCount % TRANSACTION_SIZE == 0) {
               logger.info("Commiting Progress after " + opCount + " updates");
               transaction.commit();
               transaction = broker.newTransaction();
            }
         }
         logger.info("#Assignments to upgrade: " + assignmentsTouched.size());
         Iterator<OpAssignment> i = assignmentsTouched.iterator();
         while (i.hasNext()) {
            OpActivityDataSetFactory.updateWorkMonths(broker, i.next(), session
                  .getCalendar());
            opCount++;
            if (opCount % TRANSACTION_SIZE == 0) {
               logger.info("Commiting Work Months after " + opCount + " updates");
               transaction.commit();
               transaction = broker.newTransaction();
            }
         }
   
         transaction.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }

   /**
    * Reset the values on all the activities (actual and remaining).
    *
    * @param session   project session.
    * @param projectId a <code>long</code> the id of a project.
    */
   private void resetActivities(OpProjectSession session, long projectId) {
      OpBroker broker;
      OpTransaction transaction;
      OpQuery query;
      Iterator result;

      //reset activities
      broker = session.newBroker();
      try {
         transaction = broker.newTransaction();
         query = broker.newQuery("select activity.ID from OpActivity activity where activity.Deleted = false and activity.ProjectPlan.ProjectNode.ID=?");
         query.setLong(0, projectId);
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
               activity.setComplete(0.0);
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
      }
      finally {
         broker.closeAndEvict();
      }
   }

   /**
    * Resets the values on all the assignments (actual and remaining).
    *
    * @param session   project session
    * @param projectId a <code>long</code> the id of a project.
    */
   private void resetAssignments(OpProjectSession session, long projectId) {
      OpBroker broker = session.newBroker();
      try {
         OpTransaction transaction = broker.newTransaction();
         //reset assignments
         OpQuery query = broker.newQuery("select assignment.ID from OpAssignment assignment where assignment.Activity.ProjectPlan.ProjectNode.ID=?");
         query.setLong(0, projectId);
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
               assignment.setComplete(0d);
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
            assignment.setComplete(0d);
         }
         transaction.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }
}
