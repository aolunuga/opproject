/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpActivity.ProgressDelta;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.OpWorkRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class OpAssignment extends OpObject {

   public final static String ASSIGNMENT = "OpAssignment";

   public final static String ASSIGNED = "Assigned";
   public final static String COMPLETE = "Complete";
   public final static String BASE_EFFORT = "BaseEffort";
   public final static String ACTUAL_EFFORT = "ActualEffort";
   public final static String BASE_PROCEEDS = "BaseProceeds";
   public final static String ACTUAL_PROCEEDS = "ActualProceeds";
   public final static String REMAINING_EFFORT = "RemainingEffort";
   public final static String BASE_COSTS = "BaseCosts";
   public final static String ACTUAL_COSTS = "ActualCosts";
   public final static String PROJECT_PLAN = "ProjectPlan";
   public final static String RESOURCE = "Resource";
   public final static String ACTIVITY = "Activity";
   public final static String WORK_RECORDS = "WorkRecords";

   private double assigned = 100.0; // Default is 100%
   private double complete;
   private double baseEffort; // Person-hours
   private double actualEffort; // Person-hours
   private double remainingEffort; // Person-hours

   private double baseCosts; // Personnel costs
   private double actualCosts; // Personnel costs

   private double baseProceeds; // Base External costs
   private double actualProceeds; // Base Actual costs
   private OpProjectPlan projectPlan;
   private OpResource resource;
   private OpActivity activity;
   private Set<OpWorkRecord> workRecords;
   private Set<OpWorkMonth> workMonths;
   private Double remainingProceeds;
   private Double remainingPersonnelCosts;
   
   public void setAssigned(double assigned) {
      this.assigned = assigned;
   }

   public double getAssigned() {
      return assigned;
   }

   public void setComplete(double complete) {
      this.complete = complete;
   }

   public double getComplete() {
      return complete;
   }

   public void setBaseEffort(double baseEffort) {
      this.baseEffort = baseEffort;
   }

   public double getBaseEffort() {
      if (getActivity().isUsingBaselineValues() && getActivity().getBaselineVersion() != null) {
         OpAssignmentVersion assignmentVersion = this.getBaselineVersion();
         if (assignmentVersion == null) {
            return 0;
         }
         else {
            return assignmentVersion.getBaseEffort();
         }
      }
      return baseEffort;
   }

   /**
    * Gets the coresponding assignment version from the baseline version plan, if such an assignment exists.
    *
    * @return baseline assignment version.
    */
   public OpAssignmentVersion getBaselineVersion() {
      OpAssignmentVersion assignmentVersion = null;
      OpActivityVersion baselineVersion = getActivity().getBaselineVersion();
      //<FIXME author="Haizea Florin" description="data loading problem: the getAssignmentVersions().isEmpty() statement
      // will load all the assignment versions of this activity version">
      if (baselineVersion != null && baselineVersion.getAssignmentVersions() != null && !baselineVersion.getAssignmentVersions().isEmpty()) {
      //<FIXME>
         for (OpAssignmentVersion version : baselineVersion.getAssignmentVersions()) {
            if (version.getResource().getID() == this.getResource().getID()) {
               //assignment version found
               assignmentVersion = version;
               break;
            }
         }
      }
      return assignmentVersion;
   }

   public void setActualEffort(double actualEffort) {
      this.actualEffort = actualEffort;
   }

   public double getActualEffort() {
      return actualEffort;
   }

   public void setRemainingEffort(double remainingEffort) {
      this.remainingEffort = remainingEffort;
   }

   public double getRemainingEffort() {
      return remainingEffort;
   }

   public void setBaseCosts(double baseCosts) {
      this.baseCosts = baseCosts;
   }

   /**
    * Gets the base personnel costs for this assignment.
    *
    * @return base personnel costs
    */
   public double getBaseCosts() {
      if (getActivity().isUsingBaselineValues() && getActivity().getBaselineVersion() != null) {
         OpAssignmentVersion assignmentVersion = this.getBaselineVersion();
         if (assignmentVersion == null) {
            return 0;
         }
         else {
            return assignmentVersion.getBaseCosts();
         }
      }
      return baseCosts;
   }

   public void setActualCosts(double actualCosts) {
      this.actualCosts = actualCosts;
   }

   public double getActualCosts() {
      return actualCosts;
   }

   /**
    * @return base proceeds for this assignment.
    */
   public double getBaseProceeds() {
      if (getActivity().isUsingBaselineValues() && getActivity().getBaselineVersion() != null) {
         OpAssignmentVersion assignmentVersion = this.getBaselineVersion();
         if (assignmentVersion == null) {
            return 0;
         }
         else {
            return assignmentVersion.getBaseProceeds();
         }
      }
      return baseProceeds;
   }

   public void setBaseProceeds(Double baseProceeds) {
      this.baseProceeds = (baseProceeds != null) ? baseProceeds : 0;
   }

   public double getActualProceeds() {
      return actualProceeds;
   }

   public void setActualProceeds(Double actualProceeds) {
      this.actualProceeds = (actualProceeds != null) ? actualProceeds : 0;
   }

   public void setProjectPlan(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }

   public OpProjectPlan getProjectPlan() {
      return projectPlan;
   }

   public void setResource(OpResource resource) {
      this.resource = resource;
   }

   public OpResource getResource() {
      return resource;
   }

   public void setActivity(OpActivity activity) {
      this.activity = activity;
   }

   public OpActivity getActivity() {
      return activity;
   }

   public void setWorkRecords(Set<OpWorkRecord> workRecords) {
      this.workRecords = workRecords;
   }

   public Set<OpWorkRecord> getWorkRecords() {
      return workRecords;
   }
      
   public Set<OpWorkMonth> getWorkMonths() {
      return workMonths;
   }

   public void setWorkMonths(Set<OpWorkMonth> workMonths) {
      this.workMonths = workMonths;
   }

   public OpWorkMonth getWorkMonth(int year, byte month) {
      Set<OpWorkMonth> workMonths = getWorkMonths();
      for (OpWorkMonth workMonth : workMonths) {
         if (workMonth.getYear() == year && workMonth.getMonth() == month) {
            return workMonth;
         }
      }
      return null;
   }

   /**
    * Gets the project node assignment for this assignment's resource.
    *
    * @return project nod assignment
    */
   public OpProjectNodeAssignment getProjectNodeAssignment() {
      OpActivity activity = this.getActivity();
      OpResource resource = this.getResource();
      OpProjectNode project = activity.getProjectPlan().getProjectNode();
      for (OpProjectNodeAssignment nodeAssignment : resource.getProjectNodeAssignments()) {
         if (nodeAssignment.getProjectNode().getID() == project.getID()) {
            return nodeAssignment;
         }
      }
      return null;
   }

   public void updateRemainingProceeds() {
      remainingProceeds = 0d;
      for (OpWorkMonth workMonth : getWorkMonths()) {
         remainingProceeds += workMonth.getRemainingProceeds();
      }
   }

   public void setRemainingProceeds(Double remainingProceeds) {
      this.remainingProceeds = remainingProceeds;
   }

   public double getRemainingProceeds() {
      return remainingProceeds == null ? 0 : remainingProceeds;
   }

   public void updateRemainingPersonnelCosts() {
      remainingPersonnelCosts = 0d;
      for (OpWorkMonth workMonth : getWorkMonths()) {
         remainingPersonnelCosts += workMonth.getRemainingPersonnel();
      }
   }

   public void setRemainingPersonnelCosts(Double remainingPersonnelCosts) {
      this.remainingPersonnelCosts = remainingPersonnelCosts;
   }

   public double getRemainingPersonnelCosts() {
      return remainingPersonnelCosts == null ? 0 : remainingPersonnelCosts;
   }

   public void removeWorkMonths(List<OpWorkMonth> reusableWorkMonths) {
      for (OpWorkMonth reusableWorkMonth : reusableWorkMonths) {
         reusableWorkMonth.setAssignment(null);
         workMonths.remove(reusableWorkMonth);
      }
   }
   
   
   /**
    * TODO: optimize for performance, maybe even query?
    * @param number       number of records in the past
    * @param acceptEmpty  Do we accept empty records
    * @return             The list of maxmum number records.
    */
   public List<OpWorkRecord> getLatestWorkRecords(OpWorkRecord current, int number, boolean acceptEmpty) {
      // sort this stuff...
      SortedSet<OpWorkRecord> wrSet= new TreeSet<OpWorkRecord>(new Comparator<OpWorkRecord>() {
         public int compare(OpWorkRecord o1, OpWorkRecord o2) {
            // reverse order:
            if (o2.getWorkSlip() == null || o1.getWorkSlip() == null) {
               return -1; // FIXME: only used for junit tests. Otherwise workrecords without a workslip should not exist!
            }
            int c = o2.getWorkSlip().getDate().compareTo(o1.getWorkSlip().getDate());
            c = c != 0 ? c : Long.signum(o2.getID() - o1.getID());
            return o2.getWorkSlip().getDate().compareTo(o1.getWorkSlip().getDate());
         }});

      wrSet.addAll(this.getWorkRecords());
      if (current != null) {
         wrSet.add(current);
      }
      
      List<OpWorkRecord> result = new ArrayList<OpWorkRecord>();
      Iterator<OpWorkRecord> i = wrSet.iterator();
      while (result.size() < number && i.hasNext()) {
         OpWorkRecord wr = i.next();
         if (!wr.isEmpty() || acceptEmpty) {
            result.add(wr);
         }
      }
      return result;
   }
   
   /**
    * Handle those work progress information supplied by project members
    * @param workRecord  the work record expressing the progress
    * @param insert      whether it is inserted or removed
    * @param baseWeighting 
    */
   public void handleWorkProgress(OpWorkRecord workRecord, boolean insert, boolean baseWeighting) {
      // check, because this stll is somewhat weird:
      double sign = insert ? 1.0 : -1.0;
      if (workRecord.getAssignment() == null) {
         if (insert) {
            workRecord.setAssignment(this);
         }
      }
      else if (workRecord.getAssignment().getID() != getID()) {
         return; // TODO: check, what went wrong?!?
      }
      
      // prerequisites:
      boolean progressTracked = getProjectPlan().getProgressTracked()
            || getActivity().getType() == OpActivity.ADHOC_TASK;

      List<OpWorkRecord> latestWRs = getLatestWorkRecords(workRecord, insert ? 1 : 2, false);
      // predetermined breaking point:
      boolean latest = (insert && latestWRs.size() == 0)
            || (!insert && latestWRs.get(0).getID() == workRecord.getID())
            || latestWRs.get(0).getWorkSlip() == null || workRecord.getWorkSlip() == null // again, junit test problem...
            || (insert && latestWRs.get(0).getWorkSlip().getDate().compareTo(
                  workRecord.getWorkSlip().getDate()) <= 0);
      // local calculations:
      setActualEffort(getActualEffort() + sign * workRecord.getActualEffort());
      setActualCosts(getActualCosts() + sign * workRecord.getPersonnelCosts());
      setActualProceeds(getActualProceeds() + sign * workRecord.getActualProceeds());
      
      double remainingEffortDelta = 0;
      double oldCompleteValue = getComplete();
      boolean completed = false;
      if (progressTracked) {
         if (insert && latest) {
            remainingEffortDelta = workRecord.getRemainingEffort() - getRemainingEffort();
            setRemainingEffort(workRecord.getRemainingEffort());
            completed = workRecord.getCompleted();
         }
         else if (!insert && latest) {
            // latestWRs.get(0) should not be null here!
            if (latestWRs.size() > 1) {
               remainingEffortDelta = latestWRs.get(1).getRemainingEffort() - getRemainingEffort();
               setRemainingEffort(latestWRs.get(1).getRemainingEffort());
               completed = latestWRs.get(1).getCompleted(); // might this be true?!?
            }
            else {
               // no more workrecords for this assignment!
               remainingEffortDelta = getBaseEffort() - getRemainingEffort();
               setRemainingEffort(getBaseEffort());
               completed = false;
            }
         }
         switch (getActivity().getType()) {
         case OpActivity.ADHOC_TASK:
         case OpActivity.MILESTONE:
         case OpActivity.TASK:
            setComplete(completed ? 100.0 : 0);
            break;
         default:
            setComplete(completed ? 100.0 : OpGanttValidator
                  .calculateCompleteValue(getActualEffort(), getBaseEffort(),
                        getRemainingEffort()));
            break;
         }
      }
      else {
         setRemainingEffort(OpGanttValidator.calculateRemainingEffort(
               getBaseEffort(), getActualEffort(), getComplete()));
      }
      
      // the magic number ;-)
      double weightedCompleteDelta = getBaseEffort() * (getComplete() - oldCompleteValue);
      
      // in theory, we are through with this work-record and this assignment.
      // we need to update the activity:
      OpActivity.ProgressDelta delta = new OpActivity.ProgressDelta(
            remainingEffortDelta, insert, weightedCompleteDelta, latest);

      getActivity().handleAssigmentProgress(this, workRecord, delta,
            baseWeighting);
      updateWorkingVersion(delta, baseWeighting);
   }
   
   
   /**
    * This is for completeness for now, because I cannot see whether this 
    * really helps anything. Who sees assigmnetversions from the %complete point of view???
    * @param delta
    * @param baseWeighting
    */
   private void updateWorkingVersion(OpActivity.ProgressDelta delta, boolean baseWeighting) {
      // this time, it's not as easy:
      // 1. get the activity
      // 2. get the working activity version
      // 3. get the assignmentVersions
      // 4. look for the same resource!?!
      Iterator<OpActivityVersion> i = getActivity().getVersions().iterator();
      while (i.hasNext()) {
         OpActivityVersion av = i.next();
         if (av.getPlanVersion().getVersionNumber() == OpProjectPlan.WORKING_VERSION_NUMBER) {
            Iterator<OpAssignmentVersion> j = av.getAssignmentVersions().iterator();
            while (j.hasNext()) {
               OpAssignmentVersion assV = j.next();
               if (assV.getResource().getID() == getResource().getID()) {
                  assV.updateComplete(getActualEffort(), getRemainingEffort(), delta, baseWeighting);
                  break;
               }
            }
            break;
         }
      }
      
   }
}
