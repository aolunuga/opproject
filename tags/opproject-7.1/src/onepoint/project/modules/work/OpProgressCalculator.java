/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/*
 * Created on 13.03.2005
 *
 * (c) 2004 by OnePoint Solutions Gerald Mesaric
 */
package onepoint.project.modules.work;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.util.XCalendar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
      OpWorkRecord work_record = null;
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
      double remainingEffortChange = updateAssignment(broker, work_record, insert_mode);
      updateActivity(broker, work_record, insert_mode, remainingEffortChange);
      //update cost values on work months

      //<FIXME author="Mihai Costin" description="Only the remaining values should be updated here... removed because of OPP-218">
      //OpActivityDataSetFactory.updateRemainingValues(broker, calendar, work_record.getAssignment());
      OpActivityDataSetFactory.updateWorkMonths(broker, work_record.getAssignment(), calendar);
      //</FIXME>
   }

   /**
    * Updates the values of this activity (and its super activities) based on the given workrecord : effort and costs.
    *
    * @param broker
    * @param work_record
    * @param insert_mode           true if work record was added, fals if it was removed
    * @param remainingEffortChange amount of remaining effort that was changed by the last operation
    */
   private static void updateActivity(OpBroker broker, OpWorkRecord work_record, boolean insert_mode, double remainingEffortChange) {

      //find the latest work record for this activity
      OpAssignment assignment = work_record.getAssignment();

      OpQuery query = broker.newQuery("select workRecord.ID from OpWorkRecord workRecord where workRecord.Assignment.Activity.ID = ? order by workRecord.WorkSlip.Date desc, workRecord.Modified desc");
      query.setLong(0, assignment.getActivity().getID());

      List<OpWorkRecord> workRecords = getWorkRecordsList(broker, query, true);

      OpWorkRecord latestWorkRecord;
      latestWorkRecord = work_record;
      if (workRecords.size() > 0) {
         latestWorkRecord = workRecords.get(0);
      }

      boolean progressTracked = assignment.getProjectPlan().getProgressTracked();
      // Update activity path: Add new actual effort and remaining effort change to stored values, update complete
      OpActivity activity = assignment.getActivity();
      while (activity != null) {
         // Add new actual effort and remaining effort change to stored actual and remaining efforts
         if (insert_mode) {
            activity.setActualEffort(activity.getActualEffort() + work_record.getActualEffort());
            if (progressTracked) {
               activity.setRemainingEffort(activity.getRemainingEffort() + remainingEffortChange);
            }
            activity.setActualPersonnelCosts(activity.getActualPersonnelCosts() + work_record.getPersonnelCosts());
            activity.setActualProceeds(activity.getActualProceeds() + work_record.getActualProceeds());

            // Add to manually managed costs
            activity.setActualMaterialCosts(activity.getActualMaterialCosts() + work_record.getMaterialCosts());
            activity.setActualTravelCosts(activity.getActualTravelCosts() + work_record.getTravelCosts());
            activity.setActualExternalCosts(activity.getActualExternalCosts() + work_record.getExternalCosts());
            activity.setActualMiscellaneousCosts(activity.getActualMiscellaneousCosts() + work_record.getMiscellaneousCosts());

            setActivityRemainingCosts(activity, latestWorkRecord);
         }
         else {
            activity.setActualEffort(activity.getActualEffort() - work_record.getActualEffort());
            if (progressTracked) {
               activity.setRemainingEffort(activity.getRemainingEffort() + remainingEffortChange);
            }
            activity.setActualPersonnelCosts(activity.getActualPersonnelCosts() - work_record.getPersonnelCosts());
            activity.setActualProceeds(activity.getActualProceeds() - work_record.getActualProceeds());

            // Subtract from manually managed costs
            activity.setActualMaterialCosts(activity.getActualMaterialCosts() - work_record.getMaterialCosts());
            activity.setActualTravelCosts(activity.getActualTravelCosts() - work_record.getTravelCosts());
            activity.setActualExternalCosts(activity.getActualExternalCosts() - work_record.getExternalCosts());
            activity.setActualMiscellaneousCosts(activity.getActualMiscellaneousCosts() - work_record.getMiscellaneousCosts());

            if (work_record == latestWorkRecord) {
               OpWorkRecord secondLatest = null;
               if (workRecords.size() > 1) {
                  secondLatest = workRecords.get(1);
               }
               setActivityRemainingCosts(activity, secondLatest);
            }
         }

         if (activity.getType() != OpActivity.ADHOC_TASK) {
            updateActivityBasedOnTracking(activity, progressTracked);
         }
         else {
            if (work_record.getCompleted()) {
               activity.setComplete(100);
            }
            else {
               activity.setComplete(0);
            }
         }
         broker.updateObject(activity);
         if (activity.getType() != OpActivity.ADHOC_TASK) {
            updateActivityForWorkingVersion(activity, broker);
         }
         activity = activity.getSuperActivity();
      }
   }


   /**
    * Returns the first two results of the given query.
    *
    * @param broker
    * @param query               Query over workRecord. Result must be work record ids.
    * @param includeEmptyRecords true if cons-only records should be included or not in the result
    * @return a list of work records (keeps the same order as the query result)
    */
   private static List<OpWorkRecord> getWorkRecordsList(OpBroker broker, OpQuery query, boolean includeEmptyRecords) {
      List<OpWorkRecord> workRecords = new ArrayList<OpWorkRecord>();
      Iterator iterator = broker.iterate(query);
      //the latest work record
      while (iterator.hasNext()) {
         long id = (Long) iterator.next();
         OpWorkRecord workRecord = (OpWorkRecord) broker.getObject(OpWorkRecord.class, id);
         if (includeEmptyRecords) {
            workRecords.add(workRecord);
            break;
         }
         else {
            if (!workRecord.isEmpty()) {
               workRecords.add(workRecord);
               break;
            }
         }
      }

      //the "second latest" work record
      while (iterator.hasNext()) {
         long id = (Long) iterator.next();
         OpWorkRecord workRecord = (OpWorkRecord) broker.getObject(OpWorkRecord.class, id);
         if (includeEmptyRecords) {
            workRecords.add(workRecord);
            break;
         }
         else {
            if (!workRecord.isEmpty()) {
               workRecords.add(workRecord);
               break;
            }
         }
      }
      return workRecords;

   }

   /**
    * Updates the values of the workRecord's assignment.
    *
    * @param broker
    * @param workRecord
    * @param insert_mode insert or remove work record
    * @return The remaining effort change amount.
    */
   private static double updateAssignment(OpBroker broker, OpWorkRecord workRecord, boolean insert_mode) {

      double remainingEffortChange = 0;
      //nothing to be done for an empty record (cost only)
      if (workRecord.isEmpty()) {
         return remainingEffortChange;
      }

      OpAssignment assignment = workRecord.getAssignment();

      boolean progressTracked = assignment.getProjectPlan().getProgressTracked();

      OpQuery query = broker.newQuery("select workRecord.ID from OpWorkRecord workRecord where workRecord.Assignment.ID = ? order by workRecord.WorkSlip.Date desc, workRecord.Modified desc");
      query.setLong(0, assignment.getID());

      List<OpWorkRecord> workRecords = getWorkRecordsList(broker, query, false);

      OpWorkRecord latestWorkRecord = workRecord;
      if (workRecords.size() > 0) {
         latestWorkRecord = workRecords.get(0);
      }

      //update assignment
      if (insert_mode) {
         assignment.setActualEffort(assignment.getActualEffort() + workRecord.getActualEffort());
         assignment.setActualCosts(assignment.getActualCosts() + workRecord.getPersonnelCosts());
         assignment.setActualProceeds(assignment.getActualProceeds() + workRecord.getActualProceeds());
         if (progressTracked) {
            remainingEffortChange = -assignment.getRemainingEffort() + latestWorkRecord.getRemainingEffort();
            assignment.setRemainingEffort(latestWorkRecord.getRemainingEffort());
         }
      }
      else {
         assignment.setActualEffort(assignment.getActualEffort() - workRecord.getActualEffort());
         assignment.setActualCosts(assignment.getActualCosts() - workRecord.getPersonnelCosts());
         assignment.setActualProceeds(assignment.getActualProceeds() - workRecord.getActualProceeds());
         if (progressTracked && latestWorkRecord == workRecord) {
            remainingEffortChange = -assignment.getRemainingEffort();
            if (workRecords.size() > 1) {
               OpWorkRecord newLatest = workRecords.get(1);
               assignment.setRemainingEffort(newLatest.getRemainingEffort());
            }
            else {
               assignment.setRemainingEffort(assignment.getBaseEffort());
            }
            remainingEffortChange += assignment.getRemainingEffort();
         }
      }

      boolean workRecordCompleted = insert_mode && workRecord.getCompleted();
      updateAssignmentBasedOnTracking(assignment, workRecordCompleted, progressTracked);
      broker.updateObject(assignment);
      updateAssignmentForWorkingVersion(assignment, broker);
      return remainingEffortChange;
   }

   private static void setActivityRemainingCosts(OpActivity activity, OpWorkRecord work_record) {
      if (work_record == null) {
         activity.setRemainingMaterialCosts(activity.getBaseMaterialCosts());
         activity.setRemainingTravelCosts(activity.getBaseTravelCosts());
         activity.setRemainingExternalCosts(activity.getBaseExternalCosts());
         activity.setRemainingMiscellaneousCosts(activity.getBaseMiscellaneousCosts());
      }
      else {
         activity.setRemainingMaterialCosts(work_record.getRemMaterialCosts());
         activity.setRemainingTravelCosts(work_record.getRemTravelCosts());
         activity.setRemainingExternalCosts(work_record.getRemExternalCosts());
         activity.setRemainingMiscellaneousCosts(work_record.getRemMiscCosts());
      }
   }

   /**
    * Updates the complete value for an activity assignment of the working version of the project, if that exists.
    *
    * @param assignment a <code>OpAssignment</code> representing a "real" assignment which has just been modified due to
    *                   work records.
    * @param broker     a <code>OpBroker</code> used for performing db operations.
    */
   private static void updateAssignmentForWorkingVersion(OpAssignment assignment, OpBroker broker) {
      if (assignment.getProjectPlan().hasWorkingVersion()) {
         OpActivity activity = assignment.getActivity();
         StringBuffer activityVersionQueryString = new StringBuffer();
         activityVersionQueryString.append("select assignmentVer from OpAssignmentVersion assignmentVer ");
         activityVersionQueryString.append(" inner join assignmentVer.ActivityVersion actVersion inner join actVersion.PlanVersion planVer ");
         activityVersionQueryString.append(" where assignmentVer.Resource.ID = ? and actVersion.Activity.ID = ? and planVer.VersionNumber = ?");

         OpQuery query = broker.newQuery(activityVersionQueryString.toString());
         query.setLong(0, assignment.getResource().getID());
         query.setLong(1, activity.getID());
         query.setInteger(2, OpProjectPlan.WORKING_VERSION_NUMBER);

         Iterator it = broker.iterate(query);
         if (it.hasNext()) {
            OpAssignmentVersion assignmentVersion = (OpAssignmentVersion) it.next();
            assignmentVersion.setComplete(assignment.getComplete());
            broker.updateObject(assignmentVersion);
         }
      }
   }

   /**
    * Updates the complete value for an activity  of the working version of the project, if that exists.
    *
    * @param activity a <code>XAactivity</code> representing a "real" activity which has just been modified due to
    *                 work records.
    * @param broker   a <code>OpBroker</code> used for performing db operations.
    */
   private static void updateActivityForWorkingVersion(OpActivity activity, OpBroker broker) {
      if (activity.getProjectPlan().hasWorkingVersion()) {
         StringBuffer activityVersionQueryString = new StringBuffer();
         activityVersionQueryString.append("select actVersion from OpActivityVersion actVersion ");
         activityVersionQueryString.append(" inner join actVersion.PlanVersion planVer ");
         activityVersionQueryString.append(" where actVersion.Activity.ID = ? and planVer.VersionNumber = ?");

         OpQuery query = broker.newQuery(activityVersionQueryString.toString());
         query.setLong(0, activity.getID());
         query.setInteger(1, OpProjectPlan.WORKING_VERSION_NUMBER);

         Iterator it = broker.iterate(query);
         if (it.hasNext()) {
            OpActivityVersion activityVersion = (OpActivityVersion) it.next();
            activityVersion.setComplete(activity.getComplete());
            broker.updateObject(activityVersion);
         }
      }
   }

   /**
    * Updates an assignment based on its respective work record and the tracking mode.
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
         else if (activityType == OpActivity.TASK ||
              activityType == OpActivity.MILESTONE ||
              activityType == OpActivity.ADHOC_TASK) {
            assignment.setComplete(0);
         }
         else {
            double predicted = assignment.getActualEffort() + assignment.getRemainingEffort();
            if (predicted > 0) {
               assignment.setComplete(assignment.getActualEffort() * 100 / predicted);
            }
            else {
               assignment.setComplete(0);
            }
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

   /**
    * Updates the activity based on the value of the project tracking.
    *
    * @param activity     a <code>OpActivity</code> representing the current activity to update.
    * @param isTrackingOn a <code>boolean</code> indicating whether project tracking is on or off.
    */
   public static void updateActivityBasedOnTracking(OpActivity activity, boolean isTrackingOn) {
      byte activityType = activity.getType();
      boolean taskOrMilestone = (activityType == OpActivity.TASK) || (activityType == OpActivity.MILESTONE);
      boolean collection = (activityType == OpActivity.COLLECTION) || (activityType == OpActivity.COLLECTION_TASK);

      //always calculate %complete for collections
      if (collection) {
         double complete = calculateCompleteForCollection(activity);
         activity.setComplete(complete);
      }

      if (isTrackingOn) {
         if (taskOrMilestone) {
            double complete = calculateCompleteForTaksOrMilestone(activity);
            activity.setComplete(complete);
         }
         else if (!collection) {
            double complete = calculateCompleteForStandard(activity);
            activity.setComplete(complete);
         }
      }
      else {
         double remaining = OpGanttValidator.calculateRemainingEffort(activity.getBaseEffort(), activity.getActualEffort()
              , activity.getComplete());
         activity.setRemainingEffort(remaining);
      }
   }


   /**
    * Determines the %complete for a standard activity.
    *
    * @param activity a <code>OpActivity</code> representing a standard activity.
    * @return a <code>double</code> value represeting the %complete of the activity.
    */
   private static double calculateCompleteForStandard(OpActivity activity) {
      Set assignments = activity.getAssignments();
      if (assignments.size() == 0) {
         return 0;
      }
      double baseSum = 0;
      double actualSum = 0;
      double remainingSum = 0;
      for (Iterator it = assignments.iterator(); it.hasNext();) {
         OpAssignment assignment = (OpAssignment) it.next();
         actualSum += assignment.getActualEffort();
         remainingSum += assignment.getRemainingEffort();
         baseSum += assignment.getBaseEffort();
      }
      return OpGanttValidator.calculateCompleteValue(actualSum, baseSum, remainingSum);
   }

   /**
    * Gets the %complete value for a task or a milestone.
    *
    * @param activity a <code>OpActivity</code> that is either a task or a milestone
    * @return a <code>double</code> value representing the %complete value.
    */
   private static double calculateCompleteForTaksOrMilestone(OpActivity activity) {
      boolean allComplete = true;
      Set assignments = activity.getAssignments();
      Iterator it = assignments.iterator();
      while (it.hasNext()) {
         OpAssignment assignment = (OpAssignment) it.next();
         allComplete &= (assignment.getComplete() == 100);
      }
      if (allComplete) {
         return 100;
      }
      else {
         return 0;
      }
   }

   /**
    * Gets the %complete value for a collection or collection of tasks.
    *
    * @param collection a <code>OpActivity</code> that is either a collection or collection of tasks.
    * @return a <code>double</code> value representing the %complete value.
    */
   private static double calculateCompleteForCollection(OpActivity collection) {
      double baseSum = 0;
      double actualSum = 0;
      double remainingSum = 0;
      int standardCount = 0;

      double taskSum = 0;
      int taskCount = 0;

      Set subActivities = collection.getSubActivities();
      Iterator it = subActivities.iterator();
      while (it.hasNext()) {
         OpActivity child = (OpActivity) it.next();
         int type = child.getType();
         //decision 25.04.06 - exclude milestones from %Complete calculations
         if (type == OpActivity.MILESTONE) {
            continue;
         }
         else if (type == OpActivity.TASK || type == OpActivity.COLLECTION_TASK) {
            taskCount++;
            taskSum += child.getComplete();
         }
         else {
            double actualEffort = child.getActualEffort();
            double baseEffort = child.getBaseEffort();
            double remainingEffort = child.getRemainingEffort();

            baseSum += baseEffort;
            actualSum += actualEffort;
            remainingSum += remainingEffort;
            standardCount++;
         }
      }

      double avgStandard = OpGanttValidator.calculateCompleteValue(actualSum, baseSum, remainingSum);
      double avgTask = (taskCount == 0) ? 0 : (taskSum / taskCount);
      if (collection.getType() == OpActivity.COLLECTION && standardCount > 0 && taskCount > 0) {
         return (avgStandard + avgTask) / 2;
      }
      else {
         return avgStandard + avgTask;
      }
   }
}
