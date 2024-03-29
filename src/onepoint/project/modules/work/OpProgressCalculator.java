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
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectNode;

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
   public static void addWorkRecord(OpProjectSession session, OpBroker broker, OpWorkRecord work_record, OpAssignment assignment) {
      applyWorkRecord(session, broker, work_record, true, assignment);
   }

   /**
    * Removes one or more work records.
    *
    * @param broker       a <code>OpBroker</code> used for performing business operations.
    * @param work_records an <code>Iterator</code> over a collection of <code>OpWorkRecord</code>.
    * @param calendar     Calendar used for updating costs
    */
   public static void removeWorkRecords(OpProjectSession session, OpBroker broker, Iterator work_records) {
      OpWorkRecord work_record;
      
      while (work_records.hasNext()) {
         work_record = (OpWorkRecord) (work_records.next());
         OpAssignment assignment = work_record.getAssignment();
         applyWorkRecord(session, broker, work_record, false, assignment);
      }
   }

   /**
    * Removes one or more work records.
    *
    * @param broker      a <code>OpBroker</code> used for performing business operations.
    * @param work_record the {@link onepoint.project.modules.work.OpWorkRecord} to be removed.
    * @param calendar    Calendar used for updating costs
    */
   public static void removeWorkRecord(OpProjectSession session, OpBroker broker, OpWorkRecord work_record) {
      OpAssignment ass = work_record.getAssignment();
      applyWorkRecord(session, broker, work_record, false, ass);
   }

   /**
    * Updates the values for activities and assignments, based on a given work record.
    *
    * @param broker      a <code>OpBroker</code> used for performing business operations.
    * @param work_record a <code>OpWorkRecord</code> representing a workrecord that should be applied.
    * @param insert_mode a <code>boolean</code> indicating whether an insert of delete operation should be performed.
    * @param calendar    Calendar used for updating costs
    */
   private static void applyWorkRecord(OpProjectSession session,
         OpBroker broker, OpWorkRecord work_record, boolean insert_mode,
         OpAssignment assignment) {
      // find the project-node for this workrecord because we need an anker for our transaction lock
      // which is high enough in the hierarchy to also lock checkin, checkout, (and maybe backup, restore)...
      OpProjectNode n = assignment.getActivity().getProjectPlan().getProjectNode();
      OpTransactionLock.getInstance().writeLock(n.locator());
      try {
         if (insert_mode) {
            assignment.addWorkRecord(work_record);
         }
         assignment.handleWorkProgress(work_record, insert_mode);
         
         // double remainingEffortChange = updateAssignment(broker, work_record, insert_mode);
         // updateActivity(broker, work_record, insert_mode, remainingEffortChange);
         //update cost values on work months
   
         //<FIXME author="Mihai Costin" description="Only the remaining values should be updated here... removed because of OPP-218">
         //OpActivityDataSetFactory.updateRemainingValues(broker, calendar, work_record.getAssignment());
         OpActivityDataSetFactory.updateWorkMonths(session, broker, assignment);
         //</FIXME>
         if (!insert_mode) {
            work_record.getAssignment().removeWorkRecord(work_record);
         }
      }
      finally {
         OpTransactionLock.getInstance().unlock(n.locator());
      }
   }

}
