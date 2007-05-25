/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
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

   private int number;
   private Date date;
   private OpUser creator;
   private Set records;

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

   public void setRecords(Set records) {
      this.records = records;
   }

   public Set getRecords() {
      return records;
   }

}
