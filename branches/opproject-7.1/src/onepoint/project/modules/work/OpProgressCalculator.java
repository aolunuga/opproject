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
import onepoint.persistence.OpTransactionLock;
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
         double remainingEffortChange = updateAssignment(broker, work_record, insert_mode);
         updateActivity(broker, work_record, insert_mode, remainingEffortChange);
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
    * Updates the values of this activity (and its super activities) based on the given workrecord : effort and costs.
    *
    * @param broker                broker to access db
    * @param work_record           work record
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
      double remainingMaterialCostsChange = 0;
      double remainingTravelCostsChange = 0;
      double remainingExternalCostsChange = 0;
      double remainingMiscellaneousCostsChange = 0;
      boolean hasSubActivities = true;
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

            if (OpActivityDataSetFactory.getSubactivitiesCount(broker, activity) == 0) {
               remainingMaterialCostsChange = activity.getRemainingMaterialCosts() - work_record.getRemMaterialCosts();
               remainingTravelCostsChange = activity.getRemainingTravelCosts() - work_record.getRemTravelCosts();
               remainingExternalCostsChange = activity.getRemainingExternalCosts() - work_record.getRemExternalCosts();
               remainingMiscellaneousCostsChange = activity.getRemainingMiscellaneousCosts() - work_record.getRemMiscCosts();
               hasSubActivities = false;
            }
            else {
               hasSubActivities = true;
            }
            setActivityRemainingCosts(activity, latestWorkRecord, hasSubActivities, remainingMaterialCostsChange,
                 remainingTravelCostsChange, remainingExternalCostsChange, remainingMiscellaneousCostsChange);

            //set the completed attribute for the adhoc tasks
            if (work_record == latestWorkRecord && activity.getType() == OpActivity.ADHOC_TASK) {
               if (latestWorkRecord.getCompleted()) {
                  activity.setComplete(100);
               }
               else {
                  activity.setComplete(0);
               }
            }
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
               if (OpActivityDataSetFactory.getSubactivitiesCount(broker, activity) == 0) {
                  if (secondLatest != null) {
                     remainingMaterialCostsChange = activity.getRemainingMaterialCosts() - secondLatest.getRemMaterialCosts();
                     remainingTravelCostsChange = activity.getRemainingTravelCosts() - secondLatest.getRemTravelCosts();
                     remainingExternalCostsChange = activity.getRemainingExternalCosts() - secondLatest.getRemExternalCosts();
                     remainingMiscellaneousCostsChange = activity.getRemainingMiscellaneousCosts() - secondLatest.getRemMiscCosts();
                     hasSubActivities = false;
                  }
                  else {
                     remainingMaterialCostsChange = activity.getRemainingMaterialCosts() - activity.getBaseMaterialCosts();
                     remainingTravelCostsChange = activity.getRemainingTravelCosts() - activity.getBaseTravelCosts();
                     remainingExternalCostsChange = activity.getRemainingExternalCosts() - activity.getBaseExternalCosts();
                     remainingMiscellaneousCostsChange = activity.getRemainingMiscellaneousCosts() - activity.getBaseMiscellaneousCosts();
                     hasSubActivities = true;
                  }
               }
               setActivityRemainingCosts(activity, secondLatest, hasSubActivities, remainingMaterialCostsChange,
                    remainingTravelCostsChange, remainingExternalCostsChange, remainingMiscellaneousCostsChange);

               //set the completed attribute for the adhoc tasks
               if (activity.getType() == OpActivity.ADHOC_TASK) {
                  if (secondLatest != null && secondLatest.getCompleted()) {
                     activity.setComplete(100);
                  }
                  else {
                     activity.setComplete(0);
                  }
               }
            }
         }

         //update the completed attribute only for non adhoc tasks (the adhoc task already had their completed calculated)
         if (activity.getType() != OpActivity.ADHOC_TASK) {
            updateActivityBasedOnTracking(activity, progressTracked);
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
    * @param broker              broker for db access
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
    * @param broker      broker to access db
    * @param workRecord  work record
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

      boolean workRecordCompleted = false;
      //update the non adhoc assignments
      if (assignment.getActivity().getType() != OpActivity.ADHOC_TASK) {
         workRecordCompleted = insert_mode && latestWorkRecord.getCompleted();
      }
      else {
         //update the adhoc assignments
         if (workRecord == latestWorkRecord) {
            OpWorkRecord secondLatest = null;
            if (workRecords.size() > 1) {
               secondLatest = workRecords.get(1);
            }
            workRecordCompleted = completeAdhocAssignment(assignment, insert_mode, workRecord, secondLatest);
         }
         else {
            if (latestWorkRecord.getCompleted()) {
               workRecordCompleted = true;
            }
         }
      }
      updateAssignmentBasedOnTracking(assignment, workRecordCompleted, progressTracked);

      broker.updateObject(assignment);
      updateAssignmentForWorkingVersion(assignment, broker);
      return remainingEffortChange;
   }

   /**
    * Sets the remaining costs (material/travel/external/miscellaneous) on the activity.
    *
    * @param activity                     - the <code>OpActivity</code> object which has its remaining costs set.
    * @param work_record                  - the <code>OpWorkRecord</code> from which the remaining costs are updated.
    * @param hasSubActivities             - a <code>boolean</code> value indicating whether or not the activity is a collection.
    * @param remainingMaterialCostsChange - the value with which the material costs have changed.
    * @param remainingTravelCostsChange   - the value with which the travel costs have changed.
    * @param remainingExternalCostsChange - the value with which the external costs have changed.
    * @param remainingMiscellaneiousCostsChange
    *                                     - the value with which the miscellaneous costs have changed.
    */
   private static void setActivityRemainingCosts(OpActivity activity, OpWorkRecord work_record, boolean hasSubActivities,
        double remainingMaterialCostsChange, double remainingTravelCostsChange, double remainingExternalCostsChange,
        double remainingMiscellaneiousCostsChange) {
      if (!hasSubActivities) {
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
      else {
         activity.setRemainingMaterialCosts(activity.getRemainingMaterialCosts() - remainingMaterialCostsChange);
         activity.setRemainingTravelCosts(activity.getRemainingTravelCosts() - remainingTravelCostsChange);
         activity.setRemainingExternalCosts(activity.getRemainingExternalCosts() - remainingExternalCostsChange);
         activity.setRemainingMiscellaneousCosts(activity.getRemainingMiscellaneousCosts() - remainingMiscellaneiousCostsChange);
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
    * Returns a <code>boolean</code> value indicating wether the assignment should be set 100% complete or 0% complete
    * (<code>true</code> indicates 100% completion and <code>false</code> indicates 0% completion).
    *
    * @param assignment           a <code>OpAssignment</code> representing the assignment which is to be updated.
    * @param insertMode           a <code>boolean</code> indicating whether the work record is being added or deleted
    *                             (<code>true</code> for adding and <code>false</code> for deleting).
    * @param workRecord           the <code>OpWorkRecord</code> being added or deleted.
    * @param nextLatestWorkRecord the most recent <code>OpWorkRecord</code> after the work record that's being added
    *                             or deleted.
    * @return a <code>boolean</code> value indicating wether the assignment should be set 100% complete or 0% complete
    *         (<code>true</code> indicates 100% completion and <code>false</code> indicates 0% completion).
    */
   private static boolean completeAdhocAssignment(OpAssignment assignment, boolean insertMode,
        OpWorkRecord workRecord, OpWorkRecord nextLatestWorkRecord) {

      //in case of an insert the completion of the assignment should reflect the completion of the work record
      if (insertMode) {
         if (workRecord.getCompleted()) {
            return true;
         }
         else {
            return false;
         }
      }
      // in case of a delete the completion of the assignment should reflect the completion of the most recent
      // work record, if it exists
      else {
         if (nextLatestWorkRecord != null && nextLatestWorkRecord.getCompleted()) {
            return true;
         }
         else {
            return false;
         }
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
            double complete = OpGanttValidator.calculateCompleteValue(activity.getActualEffort(),
                 activity.getBaseEffort(), activity.getRemainingEffort());
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
    * Gets the %complete value for a task or a milestone.
    *
    * @param activity a <code>OpActivity</code> that is either a task or a milestone
    * @return a <code>double</code> value representing the %complete value.
    */
   private static double calculateCompleteForTaksOrMilestone(OpActivity activity) {
      boolean allComplete = true;
      Set assignments = activity.getAssignments();
      for (Object assignment1 : assignments) {
         OpAssignment assignment = (OpAssignment) assignment1;
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
      for (Object subActivity : subActivities) {
         OpActivity child = (OpActivity) subActivity;
         if (!child.getDeleted()) {
            int type = child.getType();
            if (type == OpActivity.MILESTONE) {
               //decision 25.04.06 - exclude milestones from %Complete calculations
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
