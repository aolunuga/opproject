/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

import java.sql.Date;
import java.util.Set;

public class OpWorkSlip extends OpObject {

   public final static String WORK_SLIP = "OpWorkSlip";

   public final static String NUMBER = "Number";
   public final static String DATE = "Date";
   public final static String RESOURCE = "Resource";
   public final static String RECORDS = "Records";

   private int number = -1;
   private Date date;
   private OpUser creator;
   private Set<OpWorkRecord> records;

   protected void setNumber(int number) {
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
    * @return the error code if work-slip invalid or 0 if the workslip is valid
    */
   public int isValid() {
      // check if creator is set
      if (creator == null) {
         return OpWorkError.CREATOR_MISSING;
      }
      // check if date is set
      if (date == null) {
         return OpWorkError.DATE_MISSING;
      }

      Set<OpWorkRecord> records = getRecords();
      // a valid workslip should have >0 records
      if (records == null || records.isEmpty()) {
         return OpWorkError.WORK_RECORDS_MISSING;
      }

      // test all the work records from this work-slip
      for (OpWorkRecord record : records) {
         int errCode = record.isValid();
         if (errCode != 0) return errCode;
      }

      return 0;
   }
}
