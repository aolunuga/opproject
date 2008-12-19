/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 *
 */

package onepoint.project.modules.work;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectModuleChecker;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.project.OpWorkPeriod;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.util.OpProjectCalendar;

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
   private static final XLog logger = XLogFactory.getLogger(OpWorkModule.class);

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
      resetActivities(session, projectId);
      resetAssignments(session, projectId);
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
         query = broker.newQuery("select wr.id from OpWorkRecord as wr where wr.Assignment.Activity.ProjectPlan.ProjectNode.id = :projectId");
         query.setLong("projectId", projectId);
         List<Long> workRecordsId = broker.list(query);
         List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();
         logger.info("Found " + workRecordsId.size() + " work records to upgrade for project id: " + projectId);
         
         OpProjectNode pn = broker.getObject(OpProjectNode.class, projectId);
         
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
            OpProjectNodeAssignment pna = workRecord.getAssignment().getProjectNodeAssignment();
            List<Double> ratesList = null;
            if (pna != null) {
               ratesList = pna.getRatesForDay(workRecord.getWorkSlip().getDate(), true);
            }
            else {
               ratesList = workRecord.getAssignment().getResource().getRatesForDay(workRecord.getWorkSlip().getDate());
            }

            workRecord.setPersonnelCosts(workRecord.getActualEffort() * ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX));
            workRecord.setActualProceeds(workRecord.getActualEffort() * ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX));

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
            OpAssignment ass = i.next();
            OpActivityDataSetFactory.updateWorkMonths(session, broker, ass);
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
         query = broker.newQuery("select activity.id from OpActivity activity where activity.Deleted = false and activity.ProjectPlan.ProjectNode.id=?");
         query.setLong(0, projectId);
         result = broker.iterate(query);
         while (result.hasNext()) {
            long id = (Long) result.next();
            OpActivity activity = (OpActivity) broker.getObject(OpActivity.class, id);
            activity.setUnassignedEffort(activity.getBaseEffort());
            
            //actual set to 0 (simulate the no workslip state)
            activity.setActualEffort(0);
            activity.setActualExternalCosts(0);
            activity.setActualMaterialCosts(0);
            activity.setActualMiscellaneousCosts(0);
            activity.setActualPersonnelCosts(0);
            activity.setActualProceeds(0.0);
            activity.setActualTravelCosts(0);

            activity.setBasePersonnelCosts(0d);
            activity.setBaseProceeds(0d);
            
            activity.setRemainingEffort(0d);
            if (activity.getProjectPlan().getProgressTracked()) {
               activity.setComplete(0d);
            }
            activity.setRemainingPersonnelCosts(0d);
            activity.setRemainingProceeds(0d);

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
         OpQuery query = broker.newQuery("select assignment.id from OpAssignment assignment where assignment.Activity.ProjectPlan.ProjectNode.id=?");
         query.setLong(0, projectId);
         Iterator result = broker.iterate(query);
         OpProjectCalendar pCal = null;
         OpProjectNode pn = null;
         while (result.hasNext()) {
            long id = (Long) result.next();
            OpAssignment assignment = (OpAssignment) broker.getObject(OpAssignment.class, id);

            if (pCal == null) {
               pCal = OpProjectCalendarFactory.getInstance()
                     .getCalendar(session, broker,
                           assignment.getProjectPlan().getLatestVersion());
               pn = assignment.getProjectPlan().getProjectNode();
            }
            //actual set to 0 (simulate the no workslip state)
            assignment.setActualCosts(0);
            assignment.setActualEffort(0);
            assignment.setActualProceeds(0.0);

            // fix assignment regarding duration/effort ration:
            OpProjectCalendar cal = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, assignment.getResource(), assignment.getProjectPlan().getLatestVersion());
            double durationDays = 0d;
            if (assignment.getActivity().getDuration() < pCal.getWorkHoursPerDay()) {
               Set<OpWorkPeriod> wps = assignment.getActivity().getWorkPeriods();
               if (wps != null) {
                  for (OpWorkPeriod wp : wps) {
                     durationDays += OpActivityDataSetFactory.countWorkDaysInPeriod(wp);
                  }
               }
            }
            else {
               durationDays = OpGanttValidator.getDurationDays(assignment.getActivity().getDuration(), pCal);
            }
            boolean zeroAct = (durationDays == 0d);
            double hoursAssigned = zeroAct ? 0d : (cal.getWorkHoursPerDay() * durationDays * assignment.getAssigned() / 100d);
            if (hoursAssigned > assignment.getActivity().getUnassignedEffort()) {
               hoursAssigned = assignment.getActivity().getUnassignedEffort();
               double percentAssigned = zeroAct ? 100d : (hoursAssigned / durationDays / cal.getWorkHoursPerDay() * 100d);
               if (Math.abs(assignment.getAssigned() - percentAssigned) > OpGanttValidator.ERROR_MARGIN) {
                  logger.error("Corrected Assignment (%) for " + pn.getName() + " " + assignment);
               }
               assignment.setAssigned(percentAssigned);
            }
            if (assignment.getBaseEffort() != hoursAssigned) {
               if (Math.abs(assignment.getBaseEffort() - hoursAssigned) > OpGanttValidator.ERROR_MARGIN) {
                  logger.error("Corrected Assignment (h) for " + pn.getName() + " " + assignment);
               }
               assignment.setBaseEffort(hoursAssigned);
            }

            //reset remaining values based on tracking
            if (assignment.getProjectPlan().getProgressTracked()) {
               assignment.setRemainingEffort(assignment.getBaseEffort());
               assignment.setComplete(0d);
               assignment.setRemainingPersonnelCosts(assignment.getBaseCosts());
               assignment.setRemainingProceeds(assignment.getBaseProceeds());
            }
            else {
               double complete = assignment.getActivity().getComplete();
               assignment.setComplete(complete);
               OpProgressCalculator.updateAssignmentBasedOnTracking(assignment, false, false);
            }
            
            // WARNING: will add remaining costs/effort and the like to activity! 
            assignment.attachToActivity(assignment.getActivity());
         }
         transaction.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }
}
