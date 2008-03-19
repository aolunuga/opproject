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
   
   public final static int STATE_EDITABLE = 0;
   public final static int STATE_LOCKED = 1;
   public final static int STATE_APPROVED = 2;
   
   private Date date;
   private Date locked;
   private Date approved;
   private Double totalEffortControlled;
   private int state;
   private String comment;
   private int rating;
   private String notableIssues;
   private int customerSatisfaction;
   private OpProjectPlanVersion planVersion;
   private OpUser creator;
   private Set<OpControllingRecord> records;

   public Date getApproved() {
      return approved;
   }
   public void setApproved(Date approved) {
      this.approved = approved;
   }
   public Date getDate() {
      return date;
   }
   public void setDate(Date date) {
      this.date = date;
   }
   public Date getLocked() {
      return locked;
   }
   public void setLocked(Date approved) {
      this.locked = approved;
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
   public String getComment() {
      return comment;
   }
   public void setComment(String comment) {
      this.comment = comment;
   }
   public int getRating() {
      return rating;
   }
   public void setRating(int rating) {
      this.rating = rating;
   }
   public String getNotableIssues() {
      return notableIssues;
   }
   public void setNotableIssues(String notableIssues) {
      this.notableIssues = notableIssues;
   }
   public int getCustomerSatisfaction() {
      return customerSatisfaction;
   }
   public void setCustomerSatisfaction(int customerSatisfaction) {
      this.customerSatisfaction = customerSatisfaction;
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
   private void setRecords(Set<OpControllingRecord> records) {
      this.records = records;
   }
   public void setRecordsAndCalculate(Set<OpControllingRecord> records) {
      this.records = records;
      this.totalEffortControlled = 0.0;
      if (records != null)
         for (OpControllingRecord cr : records) {
            cr.setControllingSheet(this);
            totalEffortControlled += cr.getRowEffortBilled();
         }
   }
}
