package onepoint.project.modules.project_controlling;

import java.sql.Date;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.user.OpUser;

public class OpControllingSheet extends OpObject {

   public final static String CONTROLLING_SHEET = "OpControllingSheet";
   
   public final static String DATE = "Date";
   public final static String TOTAL_EFFORT_CONTROLLED = "TotalEffortControlled";
   public final static String STATE = "State";
   
   public final static String PLAN_VERSION = "PlanVersion";
   public final static String CREATOR = "Creator";
   public final static String ROWS = "Rows";
   
   private Date date;
   private Double totalEffortControlled;
   private int state;
   private OpProjectPlanVersion planVersion;
   private OpUser creator;
   private Set<OpControllingRecord> records;

   public Date getDate() {
      return date;
   }
   public void setDate(Date date) {
      this.date = date;
   }
   public Double getTotalEffortControlled() {
      return totalEffortControlled;
   }
   public void setTotalEffortControlled(Double totalEffortControlled) {
      this.totalEffortControlled = totalEffortControlled;
   }
   public int getState() {
      return state;
   }
   public void setState(int state) {
      this.state = state;
   }
   public OpProjectPlanVersion getPlanVersion() {
      return planVersion;
   }
   public void setPlanVersion(OpProjectPlanVersion planVersion) {
      this.planVersion = planVersion;
   }
   public OpUser getCreator() {
      return creator;
   }
   public void setCreator(OpUser creator) {
      this.creator = creator;
   }
   public Set<OpControllingRecord> getRecords() {
      return records;
   }
   public void setRecords(Set<OpControllingRecord> records) {
      if (this.records != null)
         for (OpControllingRecord cr: this.records) {
            cr.setControllingSheet(null);
         }
      this.records = records;
      this.totalEffortControlled = 0.0;
      if (records != null)
         for (OpControllingRecord cr : records) {
            cr.setControllingSheet(this);
            totalEffortControlled += cr.getRecordEffortSubTotal() + cr.getRowEffortDifference();
         }
   }
}
