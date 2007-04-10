/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAssignment;

public class OpWorkRecord extends OpObject {

   public final static String WORK_RECORD = "OpWorkRecord";

   // Attention: All fields in work record represent differences/changes to assignment values
   public final static String ACTUAL_EFFORT = "ActualEffort";
   public final static String REMAINING_EFFORT = "RemainingEffort";
   public final static String REMAINING_EFFORT_CHANGE = "RemainingEffortChange";
   /*
	public final static String TRAVEL_TIME = "TravelTime";
	public final static String PRIVATE_CAR = "PrivateCar";
   */
   public final static String TRAVEL_COSTS = "TravelCosts";
   public final static String MATERIAL_COSTS = "MaterialCosts";
   public final static String EXTERNAL_COSTS = "ExternalCosts";
   public final static String MISCELLANEOUS_COSTS = "MiscellaneousCosts";
   public final static String COMMENT = "Comment";
   public final static String ASSIGNMENT = "Assignment";
   public final static String WORK_SLIP = "WorkSlip";

   private double actualEffort; // Additional actual effort in hours
   private double remainingEffort; // Estimated remaining effort in hours
   private double remainingEffortChange; // Change of remaining effort in hours
   private double personnelCosts;
   private double travelCosts;
   private double materialCosts;
   private double externalCosts;
   private double miscellaneousCosts;
   private boolean completed;
   private String comment;
   private OpAssignment assignment;
   private OpWorkSlip workSlip;

   public void setActualEffort(double actualEffort) {
      this.actualEffort = actualEffort;
   }

   public double getActualEffort() {
      return this.actualEffort;
   }

   public void setRemainingEffort(double remainingEffort) {
      this.remainingEffort = remainingEffort;
   }

   public double getRemainingEffort() {
      return this.remainingEffort;
   }

   public void setRemainingEffortChange(double remainingEffortChange) {
      this.remainingEffortChange = remainingEffortChange;
   }

   public double getRemainingEffortChange() {
      return this.remainingEffortChange;
   }

   public void setPersonnelCosts(double actualCosts) {
      this.personnelCosts = actualCosts;
   }

   public double getPersonnelCosts() {
      return personnelCosts;
   }

   public void setTravelCosts(double travelCosts) {
      this.travelCosts = travelCosts;
   }

   public double getTravelCosts() {
      return this.travelCosts;
   }

   public void setMaterialCosts(double materialCosts) {
      this.materialCosts = materialCosts;
   }

   public double getMaterialCosts() {
      return this.materialCosts;
   }

   public void setExternalCosts(double externalCosts) {
      this.externalCosts = externalCosts;
   }

   public double getExternalCosts() {
      return this.externalCosts;
   }

   public void setMiscellaneousCosts(double miscellaneousCosts) {
      this.miscellaneousCosts = miscellaneousCosts;
   }

   public double getMiscellaneousCosts() {
      return this.miscellaneousCosts;
   }

   public void setComment(String comment) {
      this.comment = comment;
   }

   public String getComment() {
      return this.comment;
   }

   public void setAssignment(OpAssignment assignment) {
      this.assignment = assignment;
   }

   public OpAssignment getAssignment() {
      return this.assignment;
   }

   public void setWorkSlip(OpWorkSlip workSlip) {
      this.workSlip = workSlip;
   }

   public OpWorkSlip getWorkSlip() {
      return this.workSlip;
   }

   /**
    * Indicates whether the record has been completed or not.
    * @return a <code>boolean</code> indicating whether the record has been completed.
    */
   public boolean getCompleted() {
      return completed;
   }

   /**
    * Sets the value of the completed flag.
    * @param completed a <code>boolean</code> indicating whether the record has been completed.
    */
   public void setCompleted(boolean completed) {
      this.completed = completed;
   }
}
