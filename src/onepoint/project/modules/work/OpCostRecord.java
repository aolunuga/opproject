/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAttachment;

import java.util.HashSet;
import java.util.Set;

/**
 * Cost record entity - used to track costs.
 *
 * @author horia.chiorean
 */
public class OpCostRecord extends OpObject {

   public final static String COST_RECORD = "OpCostRecord";

   public static final byte TRAVEL_COST = 2;
   public static final byte MATERIAL_COST = 3;
   public static final byte EXTERNAL_COST = 4;
   public static final byte MISCELLANEOUS_COST = 5;

    //Error codes
   public static int ACTUAL_COSTS_NOT_VALID = OpWorkError.ACTUAL_COSTS_NOT_VALID;
   public static int REMAINING_COSTS_NOT_VALID = OpWorkError.REMAINING_COSTS_NOT_VALID;
   public static int COST_TYPE_NOT_VALID = OpWorkError.COST_TYPE_NOT_VALID;

   /**
    * The parent work record.
    */
   private OpWorkRecord workRecord = null;

   /**
    * The type of the costs.
    */
   private Byte type = null;

   /**
    * The actual value of the costs for the cost record
    */
   private double actualCosts = 0;

   /**
    * The remaining value of the costs for the cost record
    */
   private double remainingCosts = 0;

   /**
    * The comment for the cost record
    */
   private String comment = null;

   /**
    *  The set of attachments.
    */
   private Set<OpAttachment> attachments = new HashSet<OpAttachment>();

   /**
    * Gets this record's work record.
    *
    * @return a <code>OpWorkRecord</code> instance.
    */
   public OpWorkRecord getWorkRecord() {
      return workRecord;
   }

   /**
    * Sets this record's work record.
    *
    * @param workRecord a <code>OpWorkRecord</code> .
    */
   public void setWorkRecord(OpWorkRecord workRecord) {
      this.workRecord = workRecord;
   }

   /**
    * Gets the type of this cost record.
    *
    * @return an <code>Integer</code> representing the type of the costs record.
    */
   public Byte getType() {
      return type;
   }

   /**
    * Sets the type of this cost record.
    *
    * @param type an <code>Integer</code> representing the type of the costs record.
    */
   public void setType(Byte type) {
      this.type = type;
   }

   /**
    * Gets the actual costs for this cost record.
    *
    * @return a <code>double</code> representing the value of the actual costs.
    */
   public double getActualCosts() {
      return actualCosts;
   }

   /**
    * Sets the actual costs for this cost record.
    *
    * @param actualCosts a <code>double</code> representing the value of the actual costs.
    */
   public void setActualCosts(double actualCosts) {
      this.actualCosts = actualCosts;
   }

   /**
    * Gets the remaining costs for this cost record.
    *
    * @return a <code>double</code> representing the value of the remaining costs.
    */
   public double getRemainingCosts() {
      return remainingCosts;
   }

   /**
    * Sets the remaining costs for this cost record.
    *
    * @param remainingCosts a <code>double</code> representing the value of the remaining costs.
    */
   public void setRemainingCosts(double remainingCosts) {
      this.remainingCosts = remainingCosts;
   }

   /**
    * Gets the records's comment.
    *
    * @return a <code>String</code> representing the record's comment.
    */
   public String getComment() {
      return comment;
   }

   /**
    * Sets the records's comment.
    *
    * @param comment a <code>String</code> representing the record's comment.
    */
   public void setComment(String comment) {
      this.comment = comment;
   }


   /**
    * Gets the set of attachments.
    * @return a <code>Set(OpAttachment)</code>.
    */
   public Set<OpAttachment> getAttachments() {
      return attachments;
   }

   /**
    * Sets the set of attachments.
    * @param attachments a <code>Set(OpAttachment)</code>.
    */
   public void setAttachments(Set<OpAttachment> attachments) {
      this.attachments = attachments;
   }

   /**
    * Sets the attachments on the cost record entity and sets the <code>OpCostRecord</code> on each
    *    <code>OpAttachment</code> from the set.
    *
    * @param attachments - the <code>Set<OpAttachment></code> of attachments that will be set on the cost record.
    */
   public void addAttachments(Set<OpAttachment> attachments) {
      this.attachments = attachments;
      for (OpAttachment attachment : attachments) {
         attachment.setCostRecord(this);
      }
   }

   /**
    * Returns the record's activity if it has one, <code>null</code> otherwise.
    *
    * @return a <code>OpActivity</code> instance of <code>null</code>.
    */
   public OpActivity getActivity() {
      if (this.getWorkRecord() == null) {
         return null;
      }
      return this.getWorkRecord().getAssignment().getActivity();
   }

   /**
    * Returns the value of the base costs for the cost record, according to the type of the record.
    *
    * @return a <code>double</code> value representing the value of the base costs.
    */
   public double getBaseCost() {
      switch (type) {
         case TRAVEL_COST: {
            return this.getWorkRecord().getBaseTravelCosts();
         }
         case MATERIAL_COST: {
            return this.getWorkRecord().getBaseMaterialCosts();
         }
         case EXTERNAL_COST: {
            return this.getWorkRecord().getBaseExternalCosts();
         }
         case MISCELLANEOUS_COST: {
            return this.getWorkRecord().getBaseMiscellaneousCosts();
         }
      }
      throw new IllegalArgumentException("Trying to determine base costs for invalid OpCostRecord type:" + type);
   }

   /**
    * Checks if the fields of the cost record are valid
    *
    * @throws onepoint.persistence.OpEntityException
    *          if some validation constraints are broken
    */
   public void validate()
        throws OpEntityException {

      //actual costs must be positive
      if (actualCosts < 0) {
         throw new OpEntityException(ACTUAL_COSTS_NOT_VALID);
      }
      //remaining costs must be positive
      else if (remainingCosts < 0) {
         throw new OpEntityException(REMAINING_COSTS_NOT_VALID);
      }
      //type must be set
      else if (type == null) {
         throw new OpEntityException(COST_TYPE_NOT_VALID);
      }
   }
}
