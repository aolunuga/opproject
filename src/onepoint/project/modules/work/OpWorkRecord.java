/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAssignment;
import onepoint.util.XCalendar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class OpWorkRecord extends OpObject {

   public final static String WORK_RECORD = "OpWorkRecord";

   // Attention: All fields in work record represent differences/changes to assignment values
   public final static String ACTUAL_EFFORT = "ActualEffort";
   public final static String REMAINING_EFFORT = "RemainingEffort";

   public final static String TRAVEL_COSTS = "TravelCosts";
   public final static String MATERIAL_COSTS = "MaterialCosts";
   public final static String EXTERNAL_COSTS = "ExternalCosts";
   public final static String MISCELLANEOUS_COSTS = "MiscellaneousCosts";
   public final static String COMMENT = "Comment";
   public final static String ASSIGNMENT = "Assignment";
   public final static String WORK_SLIP = "WorkSlip";

   private double actualEffort = 0; // Additional actual effort in hours
   private double remainingEffort = 0; // Estimated remaining effort in hours
   private double personnelCosts = 0;
   private double travelCosts = 0;
   private double remTravelCosts = 0d;
   private double materialCosts = 0;
   private double remMaterialCosts = 0d;
   private double externalCosts = 0;
   private double remExternalCosts = 0d;
   private double miscellaneousCosts = 0;
   private double remMiscCosts = 0d;
   private double actualProceeds = 0;
   private boolean completed;
   private String comment;
   private OpAssignment assignment;
   private OpWorkSlip workSlip;
   private Set<OpCostRecord> costRecords = new HashSet<OpCostRecord>();
   private Set<OpTimeRecord> timeRecords = new HashSet<OpTimeRecord>();

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

   public void setPersonnelCosts(double actualCosts) {
      this.personnelCosts = actualCosts;
   }

   public double getPersonnelCosts() {
      return personnelCosts;
   }

   public void setActualProceeds(Double actualProceeds) {
      this.actualProceeds = (actualProceeds != null) ? actualProceeds : 0;
   }

   public double getActualProceeds() {
      return actualProceeds;
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
    *
    * @return a <code>boolean</code> indicating whether the record has been completed.
    */
   public boolean getCompleted() {
      return completed;
   }

   /**
    * Sets the value of the completed flag.
    *
    * @param completed a <code>boolean</code> indicating whether the record has been completed.
    */
   public void setCompleted(boolean completed) {
      this.completed = completed;
   }

   public double getRemTravelCosts() {
      return remTravelCosts;
   }

   public void setRemTravelCosts(Double remTravelCosts) {
      this.remTravelCosts = remTravelCosts != null ? remTravelCosts : 0;
   }

   public double getRemMaterialCosts() {
      return remMaterialCosts;
   }

   public void setRemMaterialCosts(Double remMaterialCosts) {
      this.remMaterialCosts = remMaterialCosts != null ? remMaterialCosts : 0;
   }

   public double getRemExternalCosts() {
      return remExternalCosts;
   }

   public void setRemExternalCosts(Double remExternalCosts) {
      this.remExternalCosts = remExternalCosts != null ? remExternalCosts : 0;
   }

   public double getRemMiscCosts() {
      return remMiscCosts;
   }

   public void setRemMiscCosts(Double remMiscCosts) {
      this.remMiscCosts = remMiscCosts != null ? remMiscCosts : 0;
   }

   public Set<OpCostRecord> getCostRecords() {
      return costRecords;
   }

   public void setCostRecords(Set<OpCostRecord> costRecords) {
      this.costRecords = costRecords;
   }

   /**
    * Sets the cost record set on the <code>OpWorkRecord</code> entity and sets the work record on each
    * <code>OpCostRecord</code> entity in the set.
    *
    * @param costRecords - the <code>Set<OpCostRecord></code> of cost records which will be set on the work record.
    */
   public void addCostRecords(Set<OpCostRecord> costRecords) {
      this.costRecords = costRecords;
      for (OpCostRecord cost : costRecords) {
         cost.setWorkRecord(this);
      }
   }

   public Set<OpTimeRecord> getTimeRecords() {
      return timeRecords;
   }

   public void setTimeRecords(Set<OpTimeRecord> timeRecords) {
      this.timeRecords = timeRecords;
   }

   /**
    * Sets the time record set on the <code>OpWorkRecord</code> entity and sets the work record on each
    * <code>OpTimeRecord</code> entity in the set.
    *
    * @param timeRecords - the <code>Set<OpTimeRecord></code> of time records which will be set on the work record.
    */
   public void addTimeRecords(Set<OpTimeRecord> timeRecords) {
      this.timeRecords = timeRecords;
      for (OpTimeRecord time : timeRecords) {
         time.setWorkRecord(this);
      }
   }

   /**
    * Return the value of the base personnel costs.
    *
    * @return a <code>double</code> representing the value of the personnel costs.
    */
   public double getBasePersonnelCosts() {
      if (this.getAssignment() == null) {
         return 0;
      }
      return this.getAssignment().getBaseCosts();
   }

   /**
    * Return the value of the base material costs.
    *
    * @return a <code>double</code> representing the value of the material costs.
    */
   public double getBaseMaterialCosts() {
      if (this.getAssignment() == null) {
         return 0;
      }
      return this.getAssignment().getActivity().getBaseMaterialCosts();
   }

   /**
    * Return the value of the base travel costs.
    *
    * @return a <code>double</code> representing the value of the travel costs.
    */
   public double getBaseTravelCosts() {
      if (this.getAssignment() == null) {
         return 0;
      }
      return this.getAssignment().getActivity().getBaseTravelCosts();
   }

   /**
    * Return the value of the base external costs.
    *
    * @return a <code>double</code> representing the value of the external costs.
    */
   public double getBaseExternalCosts() {
      if (this.getAssignment() == null) {
         return 0;
      }
      return this.getAssignment().getActivity().getBaseExternalCosts();
   }

   /**
    * Return the value of the base miscellaneous costs.
    *
    * @return a <code>double</code> representing the value of the miscellaneous costs.
    */
   public double getBaseMiscellaneousCosts() {
      if (this.getAssignment() == null) {
         return 0;
      }
      return this.getAssignment().getActivity().getBaseMiscellaneousCosts();
   }

   /**
    * Calculates the value of the actual costs of the given type, based on the cost records.
    * This method also sets the corresponding cost attribute of the work record to the calculated value.
    *
    * @param type a <code>int</code> representing a cost record type.
    * @return a <code>double</code> representing the value of the actual costs.
    * @see onepoint.project.modules.work.OpCostRecord
    */
   public double calculateActualCostsOfType(byte type) {
      double sum = 0;
      Set<OpCostRecord> costRecords = this.getCostRecordByType(type);
      for (OpCostRecord costRecord : costRecords) {
         sum += costRecord.getActualCosts();
      }
      switch (type) {
         case OpCostRecord.MATERIAL_COST: {
            this.materialCosts = sum;
            break;
         }
         case OpCostRecord.EXTERNAL_COST: {
            this.externalCosts = sum;
            break;
         }
         case OpCostRecord.MISCELLANEOUS_COST: {
            this.miscellaneousCosts = sum;
            break;
         }
         case OpCostRecord.TRAVEL_COST: {
            this.travelCosts = sum;
            break;
         }
      }
      return sum;
   }

   /**
    * Calculates the value of the remaining costs of the given type, based on the cost records.
    * This method also sets the corresponding cost attribute of the work record to the calculated value.
    *
    * @param type a <code>int</code> representing a cost record type.
    * @return a <code>double</code> representing the value of the remaining costs.
    * @see onepoint.project.modules.work.OpCostRecord
    */
   public double calculateRemainingCostsOfType(byte type) {
      // FIXME: this could not be implemented more complicated/confusing:
      // and btw, it was OBVIOUSLY WRONG!!!
      double maximum = 0;
      Set<OpCostRecord> costRecords = this.getCostRecordByType(type);
      if (!costRecords.isEmpty()) {
         for (OpCostRecord costRecord : costRecords) {
            maximum = costRecord.getRemainingCosts() > maximum ? costRecord.getRemainingCosts() : maximum;
         }
      }
      switch (type) {
         case OpCostRecord.MATERIAL_COST: {
            if (costRecords.isEmpty()) {
               maximum = this.getAssignment().getActivity().getRemainingMaterialCosts();
            }
            this.remMaterialCosts = maximum;
            break;
         }
         case OpCostRecord.EXTERNAL_COST: {
            if (costRecords.isEmpty()) {
               maximum = this.getAssignment().getActivity().getRemainingExternalCosts();
            }
            this.remExternalCosts = maximum;
            break;
         }
         case OpCostRecord.MISCELLANEOUS_COST: {
            if (costRecords.isEmpty()) {
               maximum = this.getAssignment().getActivity().getRemainingMiscellaneousCosts();
            }
            this.remMiscCosts = maximum;
            break;
         }
         case OpCostRecord.TRAVEL_COST: {
            if (costRecords.isEmpty()) {
               maximum = this.getAssignment().getActivity().getRemainingTravelCosts();
            }
            this.remTravelCosts = maximum;
            break;
         }
      }
      // /FIXME
      return maximum;
   }

   /**
    * Gets a list of all the costs records with the given type.
    *
    * @param type an <code>int</code> representing the type of a cost record.
    * @return a <code>Set(OpCostRecord)</code>.
    */
   private Set<OpCostRecord> getCostRecordByType(byte type) {
      Set<OpCostRecord> result = new HashSet<OpCostRecord>();
      for (OpCostRecord costRecord : costRecords) {
         if (costRecord.getType() == type) {
            result.add(costRecord);
         }
      }
      return result;
   }

   /**
    * Calculates the actual effort of the work record by summing up the duration of each of
    * the time records.
    *
    * @return a <code>double</code> representing the actual effort in hours.
    */
   public double calculateEffortFromTimeRecords() {
      double minutesSum = 0;
      for (OpTimeRecord timeRecord : this.timeRecords) {
         minutesSum += timeRecord.getDuration();
      }
      double effort = minutesSum / XCalendar.MINUTES_PER_HOUR;
      this.setActualEffort(effort);
      return effort;
   }

   /**
    * Checks if the <code>OpWorkRecord</code> entity is an "empty" work record (it was created only because
    * it has cost records)
    *
    * @return <code>true</code> if the work record has only cost records and no other meaningful information
    *         or <code>false</code> otherwise
    */
   public boolean isEmpty() {
      //<FIXME author="Haizea Florin" description="data loading problem: the costRecords.isEmpty() statement will load
      //  all the cost records of this work record">
      return !completed && actualEffort == 0 && !costRecords.isEmpty();
      //<FIXME>
   }

   /**
    * Test if this <code>OpWorkRecord</code> is valid.
    *
    * @throws onepoint.persistence.OpEntityException
    *          if some validation constraints are broken
    */
   public void validate()
        throws OpEntityException {

      // Actual Efort
      // FIXME: cant' be...
      if (getActualEffort() < 0 || (!getCompleted() && getActualEffort() == 0 && getCostRecords().isEmpty())) {
         throw new OpEntityException(OpWorkError.INCORRECT_ACTUAL_EFFORT);
      }
      // Material Costs
      if (getMaterialCosts() < 0) {
         throw new OpEntityException(OpWorkError.INCORRECT_MATERIAL_COSTS);
      }
      // Travel costs
      if (getTravelCosts() < 0) {
         throw new OpEntityException(OpWorkError.INCORRECT_TRAVEL_COSTS);
      }
      // External costs
      if (getExternalCosts() < 0) {
         throw new OpEntityException(OpWorkError.INCORRECT_EXTERNAL_COSTS);
      }
      // Miscellaneous Costs
      if (getMiscellaneousCosts() < 0) {
         throw new OpEntityException(OpWorkError.INCORRECT_MISCELLANEOUS_COSTS);
      }

      //Overlaping of time records
      ArrayList<OpTimeRecord> timeRecordList = new ArrayList<OpTimeRecord>();
      for (OpTimeRecord timeRecord : getTimeRecords()) {
         timeRecordList.add(timeRecord);
      }

      for (int i = 0; i < timeRecordList.size(); i++) {
         OpTimeRecord currentTimeRecord = timeRecordList.get(i);
         int currentStart = currentTimeRecord.getStart();
         int currentEnd = currentTimeRecord.getFinish();

         for (int j = i + 1; j < timeRecordList.size(); j++) {
            OpTimeRecord secondTimeRecord = timeRecordList.get(j);
            int secondStart = secondTimeRecord.getStart();
            int secondEnd = secondTimeRecord.getFinish();
            if (!(currentEnd <= secondStart || secondEnd <= currentStart)) {
               throw new OpEntityException(OpWorkError.TIME_RECORDS_OVERLAP);
            }
         }
      }

      //validate linked entities
      for (OpCostRecord costRecord : getCostRecords()) {
         costRecord.validate();
      }

      for (OpTimeRecord timRecord : getTimeRecords()) {
         timRecord.validate();
      }
   }


   @Override
   public String toString() {
      if (workSlip != null && assignment != null) {
         return "Work slip date " + workSlip.getDate() + " for Activity " + assignment.getActivity().getName() + " and resource " + assignment.getResource().getName();
      }
      else {
         return super.toString();
      }
   }

   public boolean hasCostRecordForType(byte costType) {
      return !getCostRecordByType(costType).isEmpty();
   }

   public Set<Byte> getCostTypes() {
      Set<Byte> types = new HashSet<Byte>();
      for (OpCostRecord cr: getCostRecords()) {
         types.add(new Byte(cr.getType()));
      }
      return types;
   }
}
