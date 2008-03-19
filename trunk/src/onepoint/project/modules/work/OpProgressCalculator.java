/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/*
 * Created on 13.03.2005
 *
 * (c) 2004 by OnePoint Solutions Gerald Mesaric
 */
package onepoint.project.modules.work;

import java.util.Iterator;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransactionLock;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.util.XCalendar;

/**
 * Class responsible for updating efforts, costs and completeness of activities, based on workrecords.
 *
 * @author gmesaric
 * @author horia.chiorean
 */
public class OpProgressCalculator {

   /**
    * Utility class.
    */
   private OpProgressCalculator() {
   }

   /**
    * Adds a new work record.
    *
    * @param broker      a <code>OpBroker</code> used for performing business operations.
    * @param work_record a <code>OpWorkRecord</code> representing the current work record.
    * @param calendar    Calendar used for updating costs
    */
   public static void addWorkRecord(OpBroker broker, OpWorkRecord work_record, XCalendar calendar) {
      applyWorkRecord(broker, work_record, true, calendar);
   }

   /**
    * Removes one or more work records.
    *
    * @param broker       a <code>OpBroker</code> used for performing business operations.
    * @param work_records an <code>Iterator</code> over a collection of <code>OpWorkRecord</code>.
    * @param calendar     Calendar used for updating costs
    */
   public static void removeWorkRecords(OpBroker broker, Iterator work_records, XCalendar calendar) {
      OpWorkRecord work_record;
      while (work_records.hasNext()) {
         work_record = (OpWorkRecord) (work_records.next());
         applyWorkRecord(broker, work_record, false, calendar);
      }
   }

   /**
    * Removes one or more work records.
    *
    * @param broker      a <code>OpBroker</code> used for performing business operations.
    * @param work_record the {@link onepoint.project.modules.work.OpWorkRecord} to be removed.
    * @param calendar    Calendar used for updating costs
    */
   public static void removeWorkRecord(OpBroker broker, OpWorkRecord work_record, XCalendar calendar) {
      applyWorkRecord(broker, work_record, false, calendar);
   }

   /**
    * Updates the values for activities and assignments, based on a given work record.
    *
    * @param broker      a <code>OpBroker</code> used for performing business operations.
    * @param work_record a <code>OpWorkRecord</code> representing a workrecord that should be applied.
    * @param insert_mode a <code>boolean</code> indicating whether an insert of delete operation should be performed.
    * @param calendar    Calendar used for updating costs
    */
   private static void applyWorkRecord(OpBroker broker, OpWorkRecord work_record, boolean insert_mode, XCalendar calendar) {
      // find the project-node for this workrecord because we need an anker for our transaction lock
      // which is high enough in the hierarchy to also lock checkin, checkout, (and maybe backup, restore)...
      OpProjectNode n = work_record.getAssignment().getActivity().getProjectPlan().getProjectNode();
      OpTransactionLock.getInstance().writeLock(n.locator());
      try {
         work_record.getAssignment().handleWorkProgress(work_record, insert_mode);
         
         // double remainingEffortChange = updateAssignment(broker, work_record, insert_mode);
         // updateActivity(broker, work_record, insert_mode, remainingEffortChange);
         //update cost values on work months
   
         //<FIXME author="Mihai Costin" description="Only the remaining values should be updated here... removed because of OPP-218">
         //OpActivityDataSetFactory.updateRemainingValues(broker, calendar, work_record.getAssignment());
         OpActivityDataSetFactory.updateWorkMonths(broker, work_record.getAssignment(), calendar);
         //</FIXME>
      }
      finally {
         OpTransactionLock.getInstance().unlock(n.locator());
      }
   }

   /**
    * Updates a non adhoc task assignment based on its respective work record and the tracking mode.
    *
    * @param assignment          a <code>OpAssignment</code> representing the assignment which is to be updated.
    * @param workRecordCompleted a <code>boolean</code> indicating whether the work record has been completed or not.
    * @param isTrackingOn        a <code>boolean</code> indicating whether tracking is on or off.
    */
   public static void updateAssignmentBasedOnTracking(OpAssignment assignment, boolean workRecordCompleted, boolean isTrackingOn) {
      byte activityType = assignment.getActivity().getType();
      //tracking on - %Complete is determined based on [Actual, Remaining]
      if (isTrackingOn || activityType == OpActivity.ADHOC_TASK) {
         if (workRecordCompleted) {
            assignment.setComplete(100);
         }
         else if (activityType == OpActivity.MILESTONE ||
              activityType == OpActivity.ADHOC_TASK) {
            assignment.setComplete(0);
         }
         else {
            assignment.setComplete(OpGanttValidator.calculateCompleteValue(assignment.getActualEffort(), assignment.getBaseEffort(), assignment.getRemainingEffort()));
         }
      }
      //tracking off - Remaining is determined based on [%Complete, Actual]
      else {
         double complete = assignment.getComplete();
         double actual = assignment.getActualEffort();
         double base = assignment.getBaseEffort();
         double remainingEffort = OpGanttValidator.calculateRemainingEffort(base, actual, complete);
         assignment.setRemainingEffort(remainingEffort);
      }
   }

}
