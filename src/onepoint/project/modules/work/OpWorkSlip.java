/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAssignmentIfc;
import onepoint.project.modules.user.OpUser;

public class OpWorkSlip extends OpObject {

   public final static String WORK_SLIP = "OpWorkSlip";

   public final static String DATE = "Date";
   public final static String RESOURCE = "Resource";
   public final static String RECORDS = "Records";
   public final static String TOTAL_ACTUAL_EFFORT = "TotalActualEffort";
   public final static String CREATOR = "Creator";
   public final static String STATE = "State";

   public final static int STATE_EDITABLE = 0;
   public final static int STATE_LOCKED = 1;
   public final static int STATE_APPROVED = 2;
   
   
   private Date date;
   private OpUser creator;
   private Double totalActualEffort;
   private Double totalActualOtherCosts0;
   private Double totalActualOtherCosts1;
   private Double totalActualOtherCosts2;
   private Double totalActualOtherCosts3;
   private Double totalActualOtherCosts4;
   private Set<OpWorkRecord> records = new HashSet<OpWorkRecord>();
   private int state = STATE_EDITABLE;

   /**
    * 
    */
   public OpWorkSlip() {
      super();
   }

   public void setDate(Date date) {
      this.date = date;
   }

   public void setDate(java.util.Date date) {
      this.date = new Date(date.getTime());
   }

   public Date getDate() {
      return date;
   }

   public void setCreator(OpUser creator) {
      this.creator = creator;
   }

   public OpUser getCreator() {
      return creator;
   }

   public void setTotalActualEffort(Double totalActualEffort) {
      this.totalActualEffort = totalActualEffort;
   }

   public Double getTotalActualEffort() {
      return totalActualEffort;
   }

   public void setRecords(Set<OpWorkRecord> records) {
      this.records = records;
   }

   /**
    * Sets the work record set on the <code>OpWorkSlip</code> entity and sets the work slip on each
    * <code>OpWorkRecord</code> entity in the set.
    *
    * @param records - the <code>Set<OpWorkRecord></code> of work records which will be set on the work slip.
    */
   public void addRecords(Set<OpWorkRecord> records) {
      this.records = records;
      for (OpWorkRecord record : records) {
         record.setWorkSlip(this);
      }
   }

   public void addRecord(OpWorkRecord workRecord) {
      if (getRecords() == null) {
         records = new HashSet<OpWorkRecord>();
      }
      records.add(workRecord);
      workRecord.setWorkSlip(this);
   }
   
   public void removeRecord(OpWorkRecord workRecord) {
      if (getRecords() == null) {
         return;
      }
      if (records.remove(workRecord)) {
         workRecord.setWorkSlip(null);
      }
   }
   
   public Set<OpWorkRecord> getRecords() {
      return records;
   }

   public int getState() {
      return state;
   }

   public void setState(int state) {
      this.state = state;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("<").append(getClass().getSimpleName()).append(">\n");
      buffer.append("<date=").append(date).append("/>\n");
      buffer.append("<creator=").append(creator).append("/>\n");
      buffer.append("<effort=").append(totalActualEffort).append("/>\n");
      buffer.append("<records>");
      if (records != null) {
         boolean first = true;
         for (OpWorkRecord record : records) {
            if (!first) {
               buffer.append("; ");
            }
            first = false;
            buffer.append(record);
         }
      }
      buffer.append("</records>");
      buffer.append("</").append(getClass().getSimpleName()).append(">");
      return (buffer.toString());
   }

   /**
    * Test if this <code>OpWorkSlip</code> is valid.
    *
    * @throws onepoint.persistence.OpEntityException
    *          if some validation constraints are broken
    */
   public void validate()
        throws OpEntityException {
      // check if creator is set
      if (creator == null) {
         throw new OpEntityException(OpWorkError.CREATOR_MISSING);
      }
      // check if date is set
      if (date == null) {
         throw new OpEntityException(OpWorkError.DATE_MISSING);
      }

      Set<OpWorkRecord> records = getRecords();
      // a valid workslip should have >0 records
      if (records == null || records.isEmpty()) {
         throw new OpEntityException(OpWorkError.WORK_RECORDS_MISSING);
      }

      // test all the work records from this work-slip
      for (OpWorkRecord record : records) {
         record.validate();
      }
   }

   /**
    * Calculates total values from work records
    */
   public void updateTotals() {
      double totalEffort = 0d;

      double ct0s = 0d;
      double ct1s = 0d;
      double ct2s = 0d;
      double ct3s = 0d;
      double ct4s = 0d;

      for(OpWorkRecord workRecord : records){
         totalEffort += workRecord.getActualEffort();
         ct0s += workRecord.getTravelCosts();
         ct1s += workRecord.getMaterialCosts();
         ct2s += workRecord.getExternalCosts();
         ct3s += workRecord.getMiscellaneousCosts();
      }

      setTotalActualEffort(totalEffort);

      setTotalActualOtherCosts0(ct0s);
      setTotalActualOtherCosts1(ct1s);
      setTotalActualOtherCosts2(ct2s);
      setTotalActualOtherCosts3(ct3s);
      setTotalActualOtherCosts4(ct4s);
   }

   /**
    * @param assignment
    * @return
    * @pre
    * @post
    */
   public OpWorkRecord getRecord(OpAssignmentIfc assignment) {
      for (OpWorkRecord record : records) {
         if (record.getAssignment().equals(assignment))
            return(record);
      }
      return null;
   }

   public Double getTotalActualOtherCosts0() {
      return totalActualOtherCosts0;
   }

   public void setTotalActualOtherCosts0(Double totalActualOtherCosts) {
      this.totalActualOtherCosts0 = totalActualOtherCosts;
   }

   public Double getTotalActualOtherCosts1() {
      return totalActualOtherCosts1;
   }

   public void setTotalActualOtherCosts1(Double totalActualOtherCosts1) {
      this.totalActualOtherCosts1 = totalActualOtherCosts1;
   }

   public Double getTotalActualOtherCosts2() {
      return totalActualOtherCosts2;
   }

   public void setTotalActualOtherCosts2(Double totalActualOtherCosts2) {
      this.totalActualOtherCosts2 = totalActualOtherCosts2;
   }

   public Double getTotalActualOtherCosts3() {
      return totalActualOtherCosts3;
   }

   public void setTotalActualOtherCosts3(Double totalActualOtherCosts3) {
      this.totalActualOtherCosts3 = totalActualOtherCosts3;
   }

   public Double getTotalActualOtherCosts4() {
      return totalActualOtherCosts4;
   }

   public void setTotalActualOtherCosts4(Double totalActualOtherCosts4) {
      this.totalActualOtherCosts4 = totalActualOtherCosts4;
   }
   
}