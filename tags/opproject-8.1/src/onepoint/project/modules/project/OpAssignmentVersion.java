/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpActivity.OpProgressDelta;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;

public class OpAssignmentVersion extends OpObject implements OpAssignmentIfc {

   public final static String ASSIGNMENT_VERSION = "OpAssignmentVersion";

   public final static String ASSIGNED = "Assigned";
   public final static String COMPLETE = "Complete";
   public final static String BASE_EFFORT = "BaseEffort";
   public final static String BASE_COSTS = "BaseCosts";
   public final static String BASE_PROCEEDS = "BaseProceeds";
   public final static String PLAN_VERSION = "PlanVersion";
   public final static String RESOURCE = "Resource";
   public final static String ACTIVITY_VERSION = "ActivityVersion";

   private double assigned = 100; // Default is 100%
   private double complete;
   private double baseEffort; // Person-hours
   private double baseCosts; // Personnel costs
   private double baseProceeds; //External costs
   private OpProjectPlanVersion planVersion;
   private OpResource resource;
   private OpActivityVersion activityVersion;
   private OpAssignment assignment;
   private Set<OpWorkMonthVersion> workMonthVersions = new HashSet<OpWorkMonthVersion>();
   private Object setxx;
   

   public OpAssignmentVersion() {
   }
   
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
      return baseEffort;
   }

   public void setBaseCosts(double baseCosts) {
      this.baseCosts = baseCosts;
   }

   public double getBaseCosts() {
      return baseCosts;
   }

   public double getBaseProceeds() {
      return baseProceeds;
   }

   public void setBaseProceeds(Double baseProceeds) {
      this.baseProceeds = (baseProceeds != null) ? baseProceeds : 0;
   }

   public void setPlanVersion(OpProjectPlanVersion planVersion) {
      this.planVersion = planVersion;
   }

   public OpProjectPlanVersion getPlanVersion() {
      return planVersion;
   }

   public void setResource(OpResource resource) {
      this.resource = resource;
   }

   public OpResource getResource() {
      return resource;
   }

   public void setActivityVersion(OpActivityVersion activityVersion) {
      this.activityVersion = activityVersion;
   }

   // deprecated
   public OpActivityVersion getActivityVersion() {
      return activityVersion;
   }
   
   public OpActivityVersion getActivity() {
      return activityVersion;
   }

   public Set<OpWorkMonthVersion> getWorkMonthVersions() {
      return workMonthVersions;
   }

   public void setWorkMonthVersions(Set<OpWorkMonthVersion> workMonthVersions) {
      this.workMonthVersions = workMonthVersions;
   }
   
   public void addWorkMonthVersion(OpWorkMonthVersion workMonthVersion) {
      if (getWorkMonthVersions() == null) {
         setWorkMonthVersions(new HashSet<OpWorkMonthVersion>());
      }
      if (getWorkMonthVersions().add(workMonthVersion)) {
         workMonthVersion.setAssignmentVersion(this);
      }
   }

   public void removeWorkMonthVersion(OpWorkMonthVersion workMonthVersion) {
      if (getWorkMonthVersions() == null) {
         return;
      }
      if (getWorkMonthVersions().remove(workMonthVersion)) {
         workMonthVersion.setAssignmentVersion(null);
      }
   }

   public void updateComplete(double complete, double actualEffort, double remainingEffort, OpProgressDelta delta) {
      if (getActivityVersion().isProgressTracked())
         setComplete(OpGanttValidator.calculateCompleteValue(actualEffort, getBaseEffort(), remainingEffort));
      else {
         setComplete(complete);
      }
   }

   public double getRemainingEffort() {
      return getAssignment() != null ? getAssignment().getRemainingEffort() : getBaseEffort();
   }
   
   public void attachToActivity(OpActivityVersion activity) {
      if (activity == null) {
         return;
      }
      
      if (!activity.hasSubActivities()) {
         double weightedCompleteDelta = getBaseEffort() * getComplete();
         // goal: build a delta-object to get the right thing done here:
         // TODO, FIXME: clarify behavior of remaining values!
         OpActivity.OpProgressDelta delta = new OpActivity.OpProgressDelta(
               true, -getBaseEffort(), 0d, getBaseCosts(), 0d,
               getBaseProceeds(), 0d, getRemainingEffort(), 0d, 0d, weightedCompleteDelta);
   
         activity.handleAssigmentProgress(delta);
      }
   }
   
   public void detachFromActivity(OpActivityVersion activity) {
      if (activity == null) {
         return;
      }
      
      if (!activity.hasSubActivities()) {
         double weightedCompleteDelta = getBaseEffort() * getComplete();
         // goal: build a delta-object to get the right thing done here:
         OpActivity.OpProgressDelta delta = new OpActivity.OpProgressDelta(
               true, getBaseEffort(), 0d, -getBaseCosts(), 0d,
               -getBaseProceeds(), 0d, -getRemainingEffort(), 0d, 0d, weightedCompleteDelta);
         
         // remaining costs: find those latest WR for this assignment 
         activity.handleAssigmentProgress(delta);
      }
   }

   public OpAssignment getAssignment() {
      return assignment;
   }

   public void setAssignment(OpAssignment assignment) {
      this.assignment = assignment;
   }

   public double getCompleteFromTracking() {
      if (getPlanVersion() == null) {
         return 0d;
      }
      boolean tracked = getPlanVersion().getProjectPlan().getProgressTracked();
      if (tracked) {
         return getAssignment() != null ? getAssignment().getComplete() : 0d;
      }
      else {
         return getActivityVersion() != null ? getActivityVersion().getComplete() : 0d;
      }
   }

   public double getActualEffort() {
      return getAssignment() != null ? getAssignment().getActualEffort() : 0d;
   }

   public double getOpenEffort() {
      return getAssignment() != null ? getAssignment().getOpenEffort() : getBaseEffort();
   }

   public Set getTrackedSubElements() {
      return new HashSet();
   }

   public boolean isTrackingLeaf() {
      return true;
   }
   
   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("OpAssignment:{");
      b.append(super.toString());
      b.append(" ACT:");
      b.append(getActivity() != null ? getActivityVersion().getName() : "null");
      b.append(" (id:");
      b.append(getActivity() != null ? getActivityVersion().getActivity().getId() : "null");
      b.append(") RSC:");
      b.append(getResource() != null ? getResource().getName() : "null");
      b.append(" (id:");
      b.append(getResource() != null ? getResource().getId() : "null");
      b.append(") B:");
      b.append(getBaseEffort());
      b.append(" C:");
      b.append(getComplete());
      b.append("}");
      return b.toString();
   }

   public boolean isIndivisible() {
      return OpGanttValidator.isIndivisibleElemen(this);
   }

}
