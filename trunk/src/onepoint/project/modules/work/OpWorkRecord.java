/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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

   private double actualEffort = 0; // Additional actual effort in hours
   private double remainingEffort = 0; // Estimated remaining effort in hours
   private double remainingEffortChange = 0; // Change of remaining effort in hours
   private double personnelCosts = 0;
   private double travelCosts = 0;
   private double materialCosts = 0;
   private double externalCosts = 0;
   private double miscellaneousCosts = 0;
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

   /**
    * Test if this <code>OpWorkRecord</code> is valid.
    *
    * @return the error code if work-record invalid or 0 if the work-record is valid
    */
   public int isValid() {
      // Actual Efort
      if (getActualEffort() < 0 || (!getCompleted() && getActualEffort() == 0)) {
         return OpWorkError.INCORRECT_ACTUAL_EFFORT;
      }
      // Material Costs
      if (getMaterialCosts() < 0) {
         return OpWorkError.INCORRECT_MATERIAL_COSTS;
      }
      // Travel costs
      if (getTravelCosts() < 0) {
         return OpWorkError.INCORRECT_TRAVEL_COSTS;
      }
      // External costs
      if (getExternalCosts() < 0) {
         return OpWorkError.INCORRECT_EXTERNAL_COSTS;
      }
      // Miscellaneous Costs
      if (getMiscellaneousCosts() < 0) {
         return OpWorkError.INCORRECT_MATERIAL_COSTS;
      }
      return 0;
   }

//  /* (non-Javadoc)
//   * @see onepoint.persistence.OpObject#equals(java.lang.Object)
//   */
//  @Override
//  public boolean equals(Object other) {
//    try {
//      OpWorkRecord other_record = (OpWorkRecord)other;
//      if (!super.equals(other_record))
//        return(false);
//      if (this == other)
//        return(true);
//      return((actualEffort == other_record.actualEffort) &&
//             (remainingEffort == other_record.remainingEffort) &&
//             (remainingEffortChange == other_record.remainingEffortChange) &&
//             (personnelCosts == other_record.personnelCosts) &&
//             (travelCosts == other_record.travelCosts) &&
//             (materialCosts == other_record.materialCosts) &&
//             (externalCosts == other_record.externalCosts) &&
//             (miscellaneousCosts == other_record.miscellaneousCosts) &&
//             (completed == other_record.completed) &&
//             (comment == null ? other_record.comment == null : comment.equals(other_record.comment)) &&
//             (assignment == null ? other_record.assignment == null : assignment.equals(other_record.assignment)) &&
//             (workSlip == null ? other_record.workSlip == null : workSlip.equals(other_record.workSlip)));
//    }
//    catch (ClassCastException exc) {
//      return(false);
//    }
//  } 
  
//  /* (non-Javadoc)
//   * @see onepoint.persistence.OpObject#hashCode()
//   */
//  @Override
//  public int hashCode() {
//    int hash = super.hashCode();    
//    long bits = Double.doubleToLongBits(actualEffort);
//    hash = hash * 31 + (int)(bits ^ (bits >>> 32));
//    bits = Double.doubleToLongBits(remainingEffort);
//    hash = hash * 31 + (int)(bits ^ (bits >>> 32));
//    bits = Double.doubleToLongBits(remainingEffortChange);
//    hash = hash * 31 + (int)(bits ^ (bits >>> 32));
//    bits = Double.doubleToLongBits(personnelCosts);
//    hash = hash * 31 + (int)(bits ^ (bits >>> 32));
//    bits = Double.doubleToLongBits(travelCosts);
//    hash = hash * 31 + (int)(bits ^ (bits >>> 32));
//    bits = Double.doubleToLongBits(materialCosts);
//    hash = hash * 31 + (int)(bits ^ (bits >>> 32));
//    bits = Double.doubleToLongBits(externalCosts);
//    hash = hash * 31 + (int)(bits ^ (bits >>> 32));
//    bits = Double.doubleToLongBits(miscellaneousCosts);
//    hash = hash * 31 + (int)(bits ^ (bits >>> 32));
//    hash = hash * 31 + (completed ? 1231 : 1237);
//    hash = hash * 31 + (assignment == null ? 0 : assignment.hashCode());
//    hash = hash * 31 + (workSlip == null ? 0 : workSlip.hashCode());
//    return(hash);
//   }
//    return super.hashCode();
//  }
}
