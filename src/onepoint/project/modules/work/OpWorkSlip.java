/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

public class OpWorkSlip extends OpObject {

   public final static String WORK_SLIP = "OpWorkSlip";

   public final static String NUMBER = "Number";
   public final static String DATE = "Date";
   public final static String RESOURCE = "Resource";
   public final static String RECORDS = "Records";
   public final static String CREATOR = "Creator";

   private int number = -1;
   private Date date;
   private OpUser creator;
   private Set<OpWorkRecord> records = new HashSet<OpWorkRecord>();

   public void setNumber(int number) {
      this.number = number;
   }

   public int getNumber() {
      return number;
   }

   public void setDate(Date date) {
      this.date = date;
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

   public void setRecords(Set<OpWorkRecord> records) {
      this.records = records;
      for (OpWorkRecord record : records) {
         record.setWorkSlip(this);
      }
   }

   public Set<OpWorkRecord> getRecords() {
      return records;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("<").append(getClass().getSimpleName()).append(">\n");
      buffer.append("<number=").append(number).append("/>\n");
      buffer.append("<date=").append(number).append("/>\n");
      buffer.append("<creator=").append(creator).append("/>\n");
      buffer.append("<records>");
      if (records != null) {
         for (OpWorkRecord record : records) {
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
         try{
            record.validate();
         }
         catch (OpEntityException e){
            throw e;
         }
      }
   }
}
