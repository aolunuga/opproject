/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
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

import java.util.Iterator;
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
    */
   public static void addWorkRecord(OpBroker broker, OpWorkRecord work_record) {
      applyWorkRecord(broker, work_record, true);
   }

   /**
    * Removes one or more work records.
    *
    * @param broker       a <code>OpBroker</code> used for performing business operations.
    * @param work_records an <code>Iterator</code> over a collection of <code>OpWorkRecord</code>.
    */
   public static void removeWorkRecords(OpBroker broker, Iterator work_records) {
      OpWorkRecord work_record = null;
      while (work_records.hasNext()) {
         work_record = (OpWorkRecord) (work_records.next());
         applyWorkRecord(broker, work_record, false);
      }
   }

   /**
    * Updates the values for activities and assignments, based on a given work record.
    *
    * @param broker      a <code>OpBroker</code> used for performing business operations.
    * @param work_record a <code>OpWorkRecord</code> representing a workrecord that should be applied.
    * @param insert_mode a <code>boolean</code> indicating whether an insert of delete operation should be performed.
    */
   private static void applyWorkRecord(OpBroker broker, OpWorkRecord work_record, boolean insert_mode) {
      OpAssignment assignment = work_record.getAssignment();
      boolean progressTracked = assignment.getProjectPlan().getProgressTracked();

      //update assignment
      if (insert_mode) {
         assignment.setActualEffort(assignment.getActualEffort() + work_record.getActualEffort());
         assignment.setActualCosts(assignment.getActualCosts() + assignment.getResource().getHourlyRate() * work_record.getActualEffort());
         if (progressTracked) {
            assignment.setRemainingEffort(assignment.getRemainingEffort() + work_record.getRemainingEffortChange());
         }
      }
      else {
         assignment.setActualEffort(assignment.getActualEffort() - work_record.getActualEffort());
         assignment.setActualCosts(assignment.getActualCosts() - work_record.getPersonnelCosts());
         if (progressTracked) {
            assignment.setRemainingEffort(assignment.getRemainingEffort() - work_record.getRemainingEffortChange());
         }
      }

      boolean workRecordCompleted = insert_mode && work_record.getCompleted();
      updateAssignmentBasedOnTracking(assignment, workRecordCompleted, progressTracked);
      broker.updateObject(assignment);
      updateAssignmentForWorkingVersion(assignment, broker);

      // Update activity path: Add new actual effort and remaining effort change to stored values, update complete
      OpActivity activity = assignment.getActivity();
      while (activity != null) {
         // Add new actual effort and remaining effort change to stored actual and remaining efforts
         if (insert_mode) {
            activity.setActualEffort(activity.getActualEffort() + work_record.getActualEffort());
            if (progressTracked) {
               activity.setRemainingEffort(activity.getRemainingEffort() + work_record.getRemainingEffortChange());
            }
            activity.setActualPersonnelCosts(activity.getActualPersonnelCosts() + assignment.getResource().getHourlyRate() * work_record.getActualEffort());
            // Add to manually managed costs
            activity.setActualMaterialCosts(activity.getActualMaterialCosts() + work_record.getMaterialCosts());
            activity.setActualTravelCosts(activity.getActualTravelCosts() + work_record.getTravelCosts());
            activity.setActualExternalCosts(activity.getActualExternalCosts() + work_record.getExternalCosts());
            activity.setActualMiscellaneousCosts(activity.getActualMiscellaneousCosts() + work_record.getMiscellaneousCosts());
         }
         else {
            activity.setActualEffort(activity.getActualEffort() - work_record.getActualEffort());
            if (progressTracked) {
               activity.setRemainingEffort(activity.getRemainingEffort() - work_record.getRemainingEffortChange());
            }
            activity.setActualPersonnelCosts(activity.getActualPersonnelCosts() - work_record.getPersonnelCosts());
            // Subtract from manually managed costs
            activity.setActualMaterialCosts(activity.getActualMaterialCosts() - work_record.getMaterialCosts());
            activity.setActualTravelCosts(activity.getActualTravelCosts() - work_record.getTravelCosts());
            activity.setActualExternalCosts(activity.getActualExternalCosts() - work_record.getExternalCosts());
            activity.setActualMiscellaneousCosts(activity.getActualMiscellaneousCosts() - work_record.getMiscellaneousCosts());
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
    * Updates the complete value for an activity assignment of the working version of the project, if that exists.
    *
    * @param assignment a <code>OpAssignment</code> representing a "real" assignment which has just been modified due to
    *                   work records.
    * @param broker     a <code>OpBroker</code> used for performing db operations.
    */
   private static void updateAssignmentForWorkingVersion(OpAssignment assignment, OpBroker broker) {
      OpActivity activity = assignment.getActivity();

      StringBuffer activityVersionQueryString = new StringBuffer();
      activityVersionQueryString.append("select assignmentVer from OpAssignmentVersion assignmentVer ");
      activityVersionQueryString.append(" inner join assignmentVer.ActivityVersion actVersion inner join actVersion.PlanVersion planVer ");
      activityVersionQueryString.append(" where assignmentVer.Resource.ID = ? and actVersion.Activity.ID = ? and planVer.VersionNumber = ?");

      OpQuery query = broker.newQuery(activityVersionQueryString.toString());
      query.setLong(0, assignment.getResource().getID());
      query.setLong(1, activity.getID());
      query.setInteger(2, OpProjectAdministrationService.WORKING_VERSION_NUMBER);

      Iterator it = broker.iterate(query);
      if (it.hasNext()) {
         OpAssignmentVersion assignmentVersion = (OpAssignmentVersion) it.next();
         assignmentVersion.setComplete(assignment.getComplete());
         broker.updateObject(assignmentVersion);
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
      StringBuffer activityVersionQueryString = new StringBuffer();
      activityVersionQueryString.append("select actVersion from OpActivityVersion actVersion ");
      activityVersionQueryString.append(" inner join actVersion.PlanVersion planVer ");
      activityVersionQueryString.append(" where actVersion.Activity.ID = ? and planVer.VersionNumber = ?");

      OpQuery query = broker.newQuery(activityVersionQueryString.toString());
      query.setLong(0, activity.getID());
      query.setInteger(1, OpProjectAdministrationService.WORKING_VERSION_NUMBER);

      Iterator it = broker.iterate(query);
      if (it.hasNext()) {
         OpActivityVersion activityVersion = (OpActivityVersion) it.next();
         activityVersion.setComplete(activity.getComplete());
         broker.updateObject(activityVersion);
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
