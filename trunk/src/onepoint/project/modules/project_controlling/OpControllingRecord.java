package onepoint.project.modules.project_controlling;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkRecord;

public class OpControllingRecord extends OpObject {

   public final static String CONTROLLING_SHEET_ROW = "OpControllingSheetRow";
   
   public final static String DATE = "Date";
   public final static String TOTAL_EFFORT_CONTROLLED = "TotalEffortControlled";
   
   public final static String ControllingSheet = "ControllingSheet";
   public final static String RECORDS = "WorkRecords";
   
   private double recordEffortSubTotal = 0;
   private double rowEffortBilled = 0;
   private boolean closeAssignment = false;
   private OpControllingSheet controllingSheet;
   private Set<OpWorkRecord> workRecords;
   
   private OpAssignment assignment; 

   public Double getRecordEffortSubTotal() {
      return recordEffortSubTotal;
   }
   public void setRecordEffortSubTotal(Double totalEffortControlled) {
      this.recordEffortSubTotal = totalEffortControlled;
   }
   public Double getRowEffortBilled() {
      return rowEffortBilled;
   }
   public void setRowEffortBilled(Double rowEffortBilled) {
      this.rowEffortBilled = rowEffortBilled;
   }
   public boolean isCloseAssignment() {
      return closeAssignment;
   }
   public void setCloseAssignment(boolean closeAssignment) {
      this.closeAssignment = closeAssignment;
   }
   public OpControllingSheet getControllingSheet() {
      return controllingSheet;
   }
   public void setControllingSheet(OpControllingSheet controllingSheet) {
      this.controllingSheet = controllingSheet;
   }
   public Set<OpWorkRecord> getWorkRecords() {
      return workRecords;
   }
   
   public void setWorkRecords(Set<OpWorkRecord> workRecords) {
      if (this.workRecords != null)
         for (OpWorkRecord wr : this.workRecords) {
            wr.setControllingRecord(null);
         }
         
      recordEffortSubTotal = 0;
      this.workRecords = workRecords;
      if (workRecords != null) {
         for (OpWorkRecord wr : workRecords) {
            recordEffortSubTotal += wr.getActualEffort();
            wr.setControllingRecord(this);
         }
      }
   }
   
   public void setAssignment(OpAssignment assignment) {
      this.assignment = assignment;
   }
   
   public OpAssignment getAssignment() {
      return assignment;
   }
   
   public void addWorkRecord(OpWorkRecord workRecord) {
      if (getAssignment() == null)
         setAssignment(workRecord.getAssignment());
      else if (getAssignment() != workRecord.getAssignment())
         throw new RuntimeException("Assignments messed up for controlling record...");
         
      if (workRecords == null)
         workRecords = new HashSet<OpWorkRecord>();
      workRecords.add(workRecord);
      workRecord.setControllingRecord(this);
      // update subtotals:
      recordEffortSubTotal += workRecord.getActualEffort();
   }
}
