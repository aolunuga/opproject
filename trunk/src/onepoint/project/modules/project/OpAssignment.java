/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.OpWorkRecord;

import java.util.Set;

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
   private OpAssignmentVersion getBaselineVersion() {
      OpAssignmentVersion assignmentVersion = null;
      OpActivityVersion baselineVersion = getActivity().getBaselineVersion();
      for (OpAssignmentVersion version : baselineVersion.getAssignmentVersions()) {
         if (version.getResource().getID() == this.getResource().getID()) {
            //assignment version found
            assignmentVersion = version;
            break;
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
}
