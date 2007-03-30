/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

import java.sql.Date;
import java.util.Iterator;
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
   private Set records;

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

   public void setRecords(Set records) {
      this.records = records;
   }

   public Set getRecords() {
      return records;
   }
   
   public boolean isValid()
   {
     Set records = getRecords();
     if (records == null)
       return(true);
     Iterator iter = records.iterator();
     OpWorkRecord work_record;
     boolean validActualEffort = false;
     boolean validCosts = false;

     while (iter.hasNext())
     {
       work_record = (OpWorkRecord) iter.next();
       //completed
       if (!work_record.getCompleted()) {
         validActualEffort = true;
       }
       // actual effort
       if (work_record.getActualEffort() > 0) {
         validActualEffort = true;
       }
       // Material Costs
       if (work_record.getMaterialCosts() > 0) {
         validCosts = true;
       }

       // Travel costs
       if (work_record.getTravelCosts() > 0) {
         validCosts = true;
       }

       // External costs
       if (work_record.getExternalCosts() > 0) {
         validCosts = true;
       }

       // Miscellaneous Costs
       if (work_record.getMiscellaneousCosts() > 0) {
         validCosts = true;
       }
     }
     
     // a valid effort was not found in the work record set
     if (!validActualEffort && !validCosts) // && number == -1)
       return(false);
     return(true);
   }
}
