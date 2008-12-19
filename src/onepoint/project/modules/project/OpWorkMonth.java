package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

/**
 * WokrMonth entity that holds assignment time based information.
 *
 * @author mihai.costin
 */
public class OpWorkMonth extends OpObject {

   public OpWorkMonth() {
      // TODO Auto-generated constructor stub
   }
   
   public final static String WORK_MONTH = "OpWorkMonth";

   private int year;
   private byte month;

   /**
    * Assignment of this work month period.
    */
   private OpAssignment assignment;

   /**
    * % assigned of the resource in the baseline version.
    */
   private double baseAssigned;

   /**
    * % assigned of the resource in the current plan.
    */
   private double latestAssigned;

   /**
    * Baseline effort of the assignment from the baseline project.
    */
   private double baseEffort;

   /**
    * Base effort of the assignment.
    */
   private double latestEffort;

   /**
    * Baseline personnel costs of the assignment from the baseline project.
    */
   private double basePersonnelCosts;

   /**
    * Base personnel costs of the assignment.
    */
   private double latestPersonnelCosts;

   /**
    * Baseline proceeds of the assignment from the baseline project.
    */
   private double baseProceeds;

   /**
    * Baseline proceeds of the assignment.
    */
   private double latestProceeds;

   private Double remainingPersonnel = 0d;
   private Double remainingProceeds = 0d;
   private Double remainingEffort = 0d;

   /**
    * The activity's working days in the month represented by this work month
    */
   private Byte workingDays = 0;

   public int getYear() {
      return year;
   }

   public void setYear(int year) {
      this.year = year;
   }

   public byte getMonth() {
      return month;
   }

   public void setMonth(byte month) {
      this.month = month;
   }

   public OpAssignment getAssignment() {
      return assignment;
   }

   public void setAssignment(OpAssignment assignment) {
      this.assignment = assignment;
   }

   public double getBaseAssigned() {
      return baseAssigned;
   }

   public void setBaseAssigned(double baseAssigned) {
      this.baseAssigned = baseAssigned;
   }

   public double getLatestAssigned() {
      return latestAssigned;
   }

   public void setLatestAssigned(double latestAssigned) {
      this.latestAssigned = latestAssigned;
   }

   public double getBaseEffort() {
      return baseEffort;
   }

   public void setBaseEffort(double baseEffort) {
      this.baseEffort = baseEffort;
   }

   public double getLatestEffort() {
      return latestEffort;
   }

   public void setLatestEffort(double latestEffort) {
      this.latestEffort = latestEffort;
   }

   public double getBasePersonnelCosts() {
      return basePersonnelCosts;
   }

   public void setBasePersonnelCosts(double basePersonnelCosts) {
      this.basePersonnelCosts = basePersonnelCosts;
   }

   public double getLatestPersonnelCosts() {
      return latestPersonnelCosts;
   }

   public void setLatestPersonnelCosts(double latestPersonnelCosts) {
      this.latestPersonnelCosts = latestPersonnelCosts;
   }

   public double getBaseProceeds() {
      return baseProceeds;
   }

   public void setBaseProceeds(double baseProceeds) {
      this.baseProceeds = baseProceeds;
   }

   public double getLatestProceeds() {
      return latestProceeds;
   }

   public void setLatestProceeds(double latestProceeds) {
      this.latestProceeds = latestProceeds;
   }

   public boolean isBaselineOnly() {
      return latestEffort == 0 && latestPersonnelCosts == 0 && latestProceeds == 0;
   }

   public double getRemainingPersonnel() {
      return remainingPersonnel != null ? remainingPersonnel.doubleValue() : 0;
   }

   public void setRemainingPersonnel(Double remainingPersonnel) {
      this.remainingPersonnel = remainingPersonnel;
   }

   public double getRemainingProceeds() {
      return remainingProceeds != null ? remainingProceeds.doubleValue() : 0;
   }

   public void setRemainingProceeds(Double remainingProceeds) {
      this.remainingProceeds = remainingProceeds;
   }

   public byte getWorkingDays() {
      return workingDays != null ? workingDays : 0;
   }

   public void setWorkingDays(Byte workingDays) {
      this.workingDays = workingDays;
   }

   public Double getRemainingEffort() {
      return remainingEffort;
   }

   public void setRemainingEffort(Double remainingEffort) {
      this.remainingEffort = remainingEffort;
   }
}
