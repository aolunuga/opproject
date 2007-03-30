/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.resource.OpResource;

public class OpAssignmentVersion extends OpObject {

   public final static String ASSIGNMENT_VERSION = "OpAssignmentVersion";

   public final static String ASSIGNED = "Assigned";
   public final static String COMPLETE = "Complete";
   public final static String BASE_EFFORT = "BaseEffort";
   public final static String BASE_COSTS = "BaseCosts";
   public final static String PLAN_VERSION = "PlanVersion";
   public final static String RESOURCE = "Resource";
   public final static String ACTIVITY_VERSION = "ActivityVersion";

   private double assigned = 100; // Default is 100%
   private double complete;
   private double baseEffort; // Person-hours
   private double baseCosts; // Personnel costs
   private OpProjectPlanVersion planVersion;
   private OpResource resource;
   private OpActivityVersion activityVersion;

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

   public OpActivityVersion getActivityVersion() {
      return activityVersion;
   }

}
