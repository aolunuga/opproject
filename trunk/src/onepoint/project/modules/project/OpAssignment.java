/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.OpWorkRecord;

public class OpAssignment extends OpObject implements OpAssignmentIfc {

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
   private Set<OpAssignmentVersion> assignmentVersions;
   private Set<OpWorkRecord> workRecords;
   private Set<OpWorkMonth> workMonths;
   private Double remainingProceeds;
   private Double remainingPersonnelCosts;
   
   /**
    * 
    */
   public OpAssignment() {
   }
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setAssigned(double)
    */
   public void setAssigned(double assigned) {
      this.assigned = assigned;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getAssigned()
    */
   public double getAssigned() {
      return assigned;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setComplete(double)
    */
   public void setComplete(double complete) {
      this.complete = complete;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getComplete()
    */
   public double getComplete() {
      return complete;
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#isCompleted()
    */
   public boolean isCompleted() {
      return complete == 100d;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setBaseEffort(double)
    */
   public void setBaseEffort(double baseEffort) {
      this.baseEffort = baseEffort;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getBaseEffort()
    */
   public double getBaseEffort() {
      return baseEffort;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setActualEffort(double)
    */
   public void setActualEffort(double actualEffort) {
      this.actualEffort = actualEffort;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getActualEffort()
    */
   public double getActualEffort() {
      return actualEffort;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setRemainingEffort(double)
    */
   public void setRemainingEffort(double remainingEffort) {
      this.remainingEffort = remainingEffort;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getRemainingEffort()
    */
   public double getRemainingEffort() {
      return remainingEffort;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setBaseCosts(double)
    */
   public void setBaseCosts(double baseCosts) {
      this.baseCosts = baseCosts;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getBaseCosts()
    */
   public double getBaseCosts() {
      return baseCosts;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setActualCosts(double)
    */
   public void setActualCosts(double actualCosts) {
      this.actualCosts = actualCosts;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getActualCosts()
    */
   public double getActualCosts() {
      return actualCosts;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getBaseProceeds()
    */
   public double getBaseProceeds() {
      return baseProceeds;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setBaseProceeds(java.lang.Double)
    */
   public void setBaseProceeds(Double baseProceeds) {
      this.baseProceeds = (baseProceeds != null) ? baseProceeds : 0;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getActualProceeds()
    */
   public double getActualProceeds() {
      return actualProceeds;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setActualProceeds(java.lang.Double)
    */
   public void setActualProceeds(Double actualProceeds) {
      this.actualProceeds = (actualProceeds != null) ? actualProceeds : 0;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setProjectPlan(onepoint.project.modules.project.OpProjectPlan)
    */
   public void setProjectPlan(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getProjectPlan()
    */
   public OpProjectPlan getProjectPlan() {
      return projectPlan;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setResource(onepoint.project.modules.resource.OpResource)
    */
   public void setResource(OpResource resource) {
      this.resource = resource;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getResource()
    */
   public OpResource getResource() {
      return resource;
   }

   void setActivity(OpActivity activity) {
      this.activity = activity;
   }

//   public void linkActivity(OpActivity activity) {
//      this.activity = activity;
//   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getActivity()
    */
   public OpActivity getActivity() {
      return activity;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setWorkRecords(java.util.Set)
    */
   public void setWorkRecords(Set<OpWorkRecord> workRecords) {
      this.workRecords = workRecords;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getWorkRecords()
    */
   public Set<OpWorkRecord> getWorkRecords() {
      return workRecords;
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getWorkMonths()
    */
   public Set<OpWorkMonth> getWorkMonths() {
      return workMonths;
   }

   public void addWorkMonth(OpWorkMonth wm) {
      if (getWorkMonths() == null) {
         setWorkMonths(new HashSet<OpWorkMonth>());
      }
      if (getWorkMonths().add(wm)) {
         wm.setAssignment(this);
      }
   }
   public void removeWorkMonth(OpWorkMonth wm) {
      if (getWorkMonths() == null) {
         return;
      }
      if (getWorkMonths().remove(wm)) {
         wm.setAssignment(null);
      }
   }
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setWorkMonths(java.util.Set)
    */
   public void setWorkMonths(Set<OpWorkMonth> workMonths) {
      this.workMonths = workMonths;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getWorkMonth(int, byte)
    */
   public OpWorkMonth getWorkMonth(int year, byte month) {
      Set<OpWorkMonth> workMonths = getWorkMonths();
      for (OpWorkMonth workMonth : workMonths) {
         if (workMonth.getYear() == year && workMonth.getMonth() == month) {
            return workMonth;
         }
      }
      return null;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getProjectNodeAssignment()
    */
   public OpProjectNodeAssignment getProjectNodeAssignment() {
      OpActivityIfc activity = this.getActivity();
      OpResource resource = this.getResource();
      OpProjectNode project = activity.getProjectPlan().getProjectNode();
      return getProjectNodeAssignment(resource, project);
   }
   
   public OpProjectNodeAssignment getProjectNodeAssignment(OpResource resource,
         OpProjectNode project) {
      for (OpProjectNodeAssignment nodeAssignment : resource.getProjectNodeAssignments()) {
         if (nodeAssignment.getProjectNode().getId() == project.getId()) {
            return nodeAssignment;
         }
      }
      return null;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#updateRemainingProceeds()
    */
   public void updateRemainingProceeds() {
      remainingProceeds = 0d;
      for (OpWorkMonth workMonth : getWorkMonths()) {
         remainingProceeds += workMonth.getRemainingProceeds();
      }
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setRemainingProceeds(java.lang.Double)
    */
   public void setRemainingProceeds(Double remainingProceeds) {
      this.remainingProceeds = remainingProceeds;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getRemainingProceeds()
    */
   public double getRemainingProceeds() {
      return remainingProceeds == null ? 0 : remainingProceeds;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#updateRemainingPersonnelCosts()
    */
   public void updateRemainingPersonnelCosts() {
      remainingPersonnelCosts = 0d;
      for (OpWorkMonth workMonth : getWorkMonths()) {
         remainingPersonnelCosts += workMonth.getRemainingPersonnel();
      }
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#setRemainingPersonnelCosts(java.lang.Double)
    */
   public void setRemainingPersonnelCosts(Double remainingPersonnelCosts) {
      this.remainingPersonnelCosts = remainingPersonnelCosts;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getRemainingPersonnelCosts()
    */
   public double getRemainingPersonnelCosts() {
      return remainingPersonnelCosts == null ? 0 : remainingPersonnelCosts;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#removeWorkMonths(java.util.List)
    */
   public void removeWorkMonths(List<OpWorkMonth> reusableWorkMonths) {
      for (OpWorkMonth reusableWorkMonth : reusableWorkMonths) {
         reusableWorkMonth.setAssignment(null);
         workMonths.remove(reusableWorkMonth);
      }
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getLatestWorkRecords(onepoint.project.modules.work.OpWorkRecord, int, java.util.Set)
    */
   public Map<Byte, List<OpWorkRecord>> getLatestWorkRecords(
         OpWorkRecord current, int number, Set<Byte> costTypes) {
      // sort this stuff...
      SortedSet<OpWorkRecord> wrSet= new TreeSet<OpWorkRecord>(new Comparator<OpWorkRecord>() {
         public int compare(OpWorkRecord o1, OpWorkRecord o2) {
            // reverse order:
            if (o2.getWorkSlip() == null || o1.getWorkSlip() == null) {
               return -1; // FIXME: only used for junit tests. Otherwise workrecords without a workslip should not exist!
            }
            int c = o2.getWorkSlip().getDate().compareTo(o1.getWorkSlip().getDate());
            return c;
         }});

      // first insert the current, so that the new one is in the set here (see javadoc for Set.add() for details)!
      if (current != null) {
         wrSet.add(current);
      }
      if (getWorkRecords() != null) {
         wrSet.addAll(getWorkRecords());
      }
      
      Map<Byte, List<OpWorkRecord>> result = new HashMap<Byte, List<OpWorkRecord>>();
      Set<Byte> completed = new HashSet<Byte>();
      Iterator<OpWorkRecord> i = wrSet.iterator();
      while (costTypes != null && costTypes.size() > completed.size() && i.hasNext()) {
         OpWorkRecord wr = i.next();
         for (Byte ct: costTypes) {
            if ((ct.compareTo(OpAssignmentIfc.COST_TYPE_UNDEFINED) == 0 && !wr.isEmpty())
                  || wr.hasCostRecordForType(ct.byteValue())) {
               List<OpWorkRecord> r = result.get(ct);
               if (r == null) {
                  r = new ArrayList<OpWorkRecord>();
                  result.put(ct, r);
               }
               r.add(wr);
               if (r.size() == number) {
                  completed.add(ct);
               }
            }
         }
      }
      return result;
   }
   
   private List<OpWorkRecord> getLatestWRsWithEffort(OpWorkRecord currentWR, int number) {
      Set<Byte> EFFORT_WORK_RECORD_TYPES = new HashSet<Byte>();
      EFFORT_WORK_RECORD_TYPES.add(OpAssignmentIfc.COST_TYPE_UNDEFINED);
      return getLatestWorkRecords(currentWR, number, EFFORT_WORK_RECORD_TYPES)
            .get(OpAssignmentIfc.COST_TYPE_UNDEFINED);
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getLatestWorkRecord()
    */
   public OpWorkRecord getLatestWorkRecord() {
      List<OpWorkRecord> wrs = getLatestWRsWithEffort(null, 1);
      if (wrs != null && wrs.size() > 0)
         return wrs.get(0);
      return null;
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#handleWorkProgress(onepoint.project.modules.work.OpWorkRecord, boolean)
    */
   public void handleWorkProgress(OpWorkRecord workRecord, boolean insert) {
      // check, because this stll is somewhat weird:
      double sign = insert ? 1.0 : -1.0;
      // prerequisites:
      boolean progressTracked = getActivity().isProgressTracked();

      // predetermined breaking point:
      List<OpWorkRecord> latestWRs = getLatestWRsWithEffort(workRecord,
            insert ? 1 : 2);

      boolean latest = latestWRs != null && latestWRs.size() > 0
            && latestWRs.get(0).getId() == workRecord.getId();

      // first, check if there are no more workrecords left for this
      // assignment:
      double wrRemainingEffort = workRecord.getRemainingEffort();

      double wrActualEffort = 0d;
      double wrPersonellCosts = 0d;
      double wrActualProceeds = 0d;
      if (insert || (latestWRs != null && latestWRs.size() > 1)) {
         wrActualEffort = workRecord.getActualEffort();
         wrPersonellCosts = workRecord.getPersonnelCosts();
         wrActualProceeds = workRecord.getActualProceeds();
      }
      else {
         // we do delete the latest WR
         wrActualEffort = getActualEffort();
         wrPersonellCosts = getActualCosts();
         wrActualProceeds = getActualProceeds();
      }
      // local calculations:
      setActualEffort(getActualEffort() + sign * wrActualEffort);
      setActualCosts(getActualCosts() + sign * wrPersonellCosts);
      setActualProceeds(getActualProceeds() + sign * wrActualProceeds);

      double remainingEffortDelta = 0;
      double remainingPersonellCostsDelta = 0;
      double remainingProceedsDelta = 0;
      double oldCompleteValue = getComplete();
      boolean completed = false;
      boolean completedOld = getComplete() == 100; // it was completed when we had 100% completed ;-)
      // the magic number ;-)
      if (progressTracked) {
         // FIXME: again, those ugly empty work record shows up and
         // asks for special treatment:
         if (latest) {
            if (insert) {
               remainingEffortDelta = wrRemainingEffort - getRemainingEffort();
               setRemainingEffort(wrRemainingEffort);
               completed = workRecord.getCompleted();
            }
            else {
               if (latestWRs.size() > 1) {
                  remainingEffortDelta = latestWRs.get(1).getRemainingEffort() - getRemainingEffort();
                  setRemainingEffort(latestWRs.get(1).getRemainingEffort());
                  completed = latestWRs.get(1).getCompleted(); // might this be true?!?
               }
               else {
                  // no more workrecords for this assignment!
                  remainingEffortDelta = getBaseEffort() - getRemainingEffort();
                  remainingPersonellCostsDelta = getBaseCosts() - getRemainingPersonnelCosts();
                  remainingProceedsDelta = getBaseProceeds() - getRemainingProceeds();
                  setRemainingEffort(getBaseEffort());
                  completed = false;
               }
            }
         }
         if (getActivity().getType() == OpActivity.ADHOC_TASK
               || getActivity().getType() == OpActivity.MILESTONE || isZero()) {
            setComplete(completed ? 100.0 : 0);
         } else {
            setComplete(completed ? 100.0 : OpGanttValidator
                  .calculateCompleteValue(getActualEffort(), getBaseEffort(),
                        getOpenEffort()));
         }
      } else {
         // FIXME: move this into OpGanttValidator??? -> calculations-class ???
         setComplete(getActivity().getComplete());
         double remainingEffort = OpGanttValidator.calculateRemainingEffort(getBaseEffort(), getActualEffort(), getComplete());
         remainingEffortDelta = remainingEffort - getRemainingEffort();
         setRemainingEffort(remainingEffort);
      }
      
      // in theory, we are through with this work-record and this assignment.
      // we need to update the activity:
      OpActivity.OpProgressDelta delta = new OpActivity.OpProgressDelta(insert,
            0d, 0d, sign * wrActualEffort, 0d, sign * wrPersonellCosts, 0d, sign
                  * wrActualProceeds, remainingEffortDelta, 0d, 0d);

      delta.setActualCosts(OpAssignment.COST_TYPE_EXTERNAL, sign * workRecord.getExternalCosts());
      delta.setActualCosts(OpAssignment.COST_TYPE_MATERIAL, sign * workRecord.getMaterialCosts());
      delta.setActualCosts(OpAssignment.COST_TYPE_MISC, sign * workRecord.getMiscellaneousCosts());
      delta.setActualCosts(OpAssignment.COST_TYPE_TRAVEL, sign * workRecord.getTravelCosts());
      
      getActivity().handleAssigmentProgress(workRecord, delta, true);
      updateWorkingVersion(delta);
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#addWorkRecord(onepoint.project.modules.work.OpWorkRecord)
    */
   public void addWorkRecord(OpWorkRecord workRecord) {
      // TODO Auto-generated method stub
      if (getWorkRecords() == null) {
         workRecords = new HashSet<OpWorkRecord>();
      }
      getWorkRecords().add(workRecord);
      workRecord.setAssignment(this);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#getCompleteFromTracking()
    */
   public double getCompleteFromTracking() {
      if (getActivity().getProjectPlan().getProgressTracked()) {
         if (getActivity().isIndivisible()) {
            List<OpWorkRecord> latestWRs = getLatestWRsWithEffort(null, 1);
            if (latestWRs != null && latestWRs.size() > 0) {
               return latestWRs.get(0).getCompleted() ? 100 : 0;
            } else {
               return 0;
            }
         } else {
            return OpGanttValidator.calculateCompleteValue(getActualEffort(),
                  getBaseEffort(), getOpenEffort());
         }
      }
      else {
         // none-Progress tracked assignments inherit their completeness from the activity!
         return getComplete();
      }
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#attachToActivity(onepoint.project.modules.project.OpActivity, boolean)
    */
   public void attachToActivity(OpActivity activity) {
      if (activity == null) {
         return;
      }
      
      if (!activity.hasSubActivities()) {
         // goal: build a delta-object to get the right thing done here:
         // TODO, FIXME: clarify behavior of remaining values!
         OpActivity.OpProgressDelta delta = new OpActivity.OpProgressDelta(
               true, 0d, -getBaseEffort(), getActualEffort(), getBaseCosts(),
               getActualCosts(), getBaseProceeds(), getActualProceeds(),
               getRemainingEffort(), getRemainingPersonnelCosts(),
               getRemainingProceeds());
   
         activity.handleAssigmentProgress(null, delta, false);
      }
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#detachFromActivity(onepoint.project.modules.project.OpActivity)
    */
   public void detachFromActivity(OpActivity activity) {
      if (activity == null) {
         return;
      }
      
      if (!activity.hasSubActivities()) {
         // goal: build a delta-object to get the right thing done here:
         OpActivity.OpProgressDelta delta = new OpActivity.OpProgressDelta(
               true, 0d, getBaseEffort(), -getActualEffort(), -getBaseCosts(),
               -getActualCosts(), -getBaseProceeds(), -getActualProceeds(),
               -getRemainingEffort(), -getRemainingPersonnelCosts(),
               -getRemainingProceeds());
         
         // remaining costs: find those latest WR for this assignment 
         activity.handleAssigmentProgress(null, delta, false);
      }
   }
   
   
   /**
    * This is for completeness for now, because I cannot see whether this 
    * really helps anything. Who sees assigmnetversions from the %complete point of view???
    * @param delta
    * @param baseWeighting
    */
   private void updateWorkingVersion(OpActivity.OpProgressDelta delta) {
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
               if (assV.getResource().getId() == getResource().getId()) {
                  assV.updateComplete(getComplete(), getActualEffort(), getOpenEffort(), delta);
                  break;
               }
            }
            break;
         }
      }
      
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#isZero()
    */
   public boolean isZero() {
      return getActualEffort() == 0d && getRemainingEffort() == 0d;
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#hasWorkRecords()
    */
   public boolean hasWorkRecords() {
      return getWorkRecords() != null && !getWorkRecords().isEmpty();
   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#toString()
    */
   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("OpAssignment:{");
      b.append(super.toString());
      b.append(" ACT:");
      b.append(getActivity() != null ? getActivity().getName() : "null");
      b.append(" (id:");
      b.append(getActivity() != null ? getActivity().getId() : "null");
      b.append(") RSC:");
      b.append(getResource() != null ? getResource().getName() : "null");
      b.append(" (id:");
      b.append(getResource() != null ? getResource().getId() : "null");
      b.append(") B:");
      b.append(getBaseEffort());
      b.append(" A:");
      b.append(getActualEffort());
      b.append(" R:");
      b.append(getRemainingEffort());
      b.append(" C:");
      b.append(getComplete());
      b.append("}");
      return b.toString();
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpAssignmentIfc#removeWorkRecord(onepoint.project.modules.work.OpWorkRecord)
    */
   public void removeWorkRecord(OpWorkRecord wr) {
      if (getWorkRecords() == null) {
         return;
      }
      if (workRecords.remove(wr)) {
         wr.setAssignment(null);
      }
      
   }
   public Set<OpAssignmentVersion> getAssignmentVersions() {
      return assignmentVersions;
   }
   private void setAssignmentVersions(Set<OpAssignmentVersion> assignmentVersions) {
      this.assignmentVersions = assignmentVersions;
   }

   public void addAssignmentVersion(OpAssignmentVersion version) {
      if (getAssignmentVersions() == null) {
         setAssignmentVersions(new HashSet<OpAssignmentVersion>());
      }
      if (getAssignmentVersions().add(version)) {
         version.setAssignment(this);
      }
   }
   
   public void removeAssignmentVersion(OpAssignmentVersion version) {
      if (getAssignmentVersions() == null) {
         return;
      }
      if (getAssignmentVersions().remove(version)) {
         version.setAssignment(null);
      }
   }
   public double getOpenEffort() {
      return getRemainingEffort();
   }
   public Set getTrackedSubElements() {
      return new HashSet();
   }
   public boolean isTrackingLeaf() {
      return true;
   }
   public boolean isIndivisible() {
      return OpGanttValidator.isIndivisibleElement(this);
   }

}
